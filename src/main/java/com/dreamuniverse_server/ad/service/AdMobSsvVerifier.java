
package com.dreamuniverse_server.ad.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AdMobSsvVerifier {

    private static final String KEYS_URL =
            "https://www.gstatic.com/admob/reward/verifier-keys.json";

    private static final Pattern SIGNED_QUERY =
            Pattern.compile(
                    "^(.+)&signature=([^&]+)&key_id=([0-9]+)$"
            );

    private static final long CACHE_SECONDS = 3600;

    private final RestClient restClient;

    private volatile Map<Long, PublicKey> cachedKeys = Map.of();
    private volatile Instant cacheExpiresAt = Instant.EPOCH;

    public AdMobSsvVerifier() {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * Google AdMob SSV 콜백 서명 검증
     */
    public VerifiedCallback verify(String rawQuery) {

        if (rawQuery == null
                || rawQuery.length() > 4096) {
            throw invalid();
        }

        Matcher matcher = SIGNED_QUERY.matcher(rawQuery);

        if (!matcher.matches()) {
            throw invalid();
        }

        // Google이 서명한 원본 문자열
        String signedContent = matcher.group(1);

        // Base64 URL-safe 인코딩된 서명
        String encodedSignature = matcher.group(2);

        final long keyId;
        final byte[] signatureBytes;

        try {
            keyId = Long.parseLong(matcher.group(3));

            signatureBytes = Base64.getUrlDecoder()
                    .decode(encodedSignature);

        } catch (IllegalArgumentException e) {
            throw invalid();
        }

        // Google 공개키 조회
        PublicKey publicKey = getPublicKey(keyId);

        // 전자서명 검증
        try {
            Signature verifier =
                    Signature.getInstance("SHA256withECDSA");

            verifier.initVerify(publicKey);

            // 디코딩이나 순서 변경 없이 원본 사용
            verifier.update(
                    signedContent.getBytes(StandardCharsets.UTF_8)
            );

            if (!verifier.verify(signatureBytes)) {
                throw invalid();
            }

        } catch (ResponseStatusException e) {
            throw e;

        } catch (SignatureException e) {
            // 서명 데이터 형식이 잘못된 경우
            throw invalid();

        } catch (java.security.GeneralSecurityException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AdMob signature verification unavailable",
                    e
            );
        }

        // 서명 검증을 통과한 데이터만 파싱
        Map<String, String> params =
                parseParameters(signedContent);

        String sessionId =
                required(params, "custom_data");

        String transactionId =
                required(params, "transaction_id");

        String adUnit =
                required(params, "ad_unit");

        String timestamp =
                required(params, "timestamp");

        final long timestampMillis;

        try {
            timestampMillis = Long.parseLong(timestamp);

            if (timestampMillis < 0) {
                throw invalid();
            }

        } catch (NumberFormatException e) {
            throw invalid();
        }

        return new VerifiedCallback(
                sessionId,
                transactionId,
                adUnit,
                timestampMillis
        );
    }

    /**
     * key_id에 해당하는 Google 공개키 조회
     */
    private PublicKey getPublicKey(long keyId) {

        Map<Long, PublicKey> keys = loadKeys();

        PublicKey key = keys.get(keyId);

        if (key == null) {
            // Google 키 교체 가능성 고려
            keys = refreshKeys();
            key = keys.get(keyId);
        }

        if (key == null) {
            throw invalid();
        }

        return key;
    }

    /**
     * 캐시된 공개키 우선 사용
     */
    private Map<Long, PublicKey> loadKeys() {

        if (Instant.now().isBefore(cacheExpiresAt)
                && !cachedKeys.isEmpty()) {
            return cachedKeys;
        }

        return refreshKeys();
    }

    /**
     * Google 공개키 목록 새로 조회
     */
    private synchronized Map<Long, PublicKey> refreshKeys() {

        try {
            KeyResponse response = restClient.get()
                    .uri(KEYS_URL)
                    .retrieve()
                    .body(KeyResponse.class);

            if (response == null
                    || response.keys() == null
                    || response.keys().isEmpty()) {
                throw new IllegalStateException(
                        "No AdMob public keys available"
                );
            }

            Map<Long, PublicKey> keys = new HashMap<>();

            for (KeyData data : response.keys()) {

                if (data.base64() == null
                        || data.base64().isBlank()) {
                    throw new IllegalStateException(
                            "Missing AdMob public key"
                    );
                }

                byte[] encoded =
                        Base64.getDecoder()
                                .decode(data.base64());

                PublicKey key = KeyFactory
                        .getInstance("EC")
                        .generatePublic(
                                new X509EncodedKeySpec(encoded)
                        );

                keys.put(data.keyId(), key);
            }

            cachedKeys = Map.copyOf(keys);

            cacheExpiresAt = Instant.now()
                    .plusSeconds(CACHE_SECONDS);

            return cachedKeys;

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AdMob public keys temporarily unavailable",
                    e
            );
        }
    }

    /**
     * 서명 검증 완료 후 쿼리 파라미터 파싱
     */
    private Map<String, String> parseParameters(
            String signedContent
    ) {

        Map<String, String> result = new HashMap<>();

        for (String part : signedContent.split("&", -1)) {

            int separator = part.indexOf('=');

            if (separator <= 0) {
                throw invalid();
            }

            String key = part.substring(0, separator);

            final String value;

            try {
                value = URLDecoder.decode(
                        part.substring(separator + 1),
                        StandardCharsets.UTF_8
                );
            } catch (IllegalArgumentException e) {
                throw invalid();
            }

            if (result.putIfAbsent(key, value) != null) {
                throw invalid();
            }
        }

        return result;
    }

    /**
     * 필수 파라미터 확인
     */
    private String required(
            Map<String, String> params,
            String key
    ) {
        String value = params.get(key);

        if (value == null || value.isBlank()) {
            throw invalid();
        }

        return value;
    }

    private ResponseStatusException invalid() {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid AdMob SSV callback"
        );
    }

    /**
     * Google 공개키 응답
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KeyResponse(
            List<KeyData> keys
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KeyData(
            long keyId,
            String base64
    ) {
    }

    /**
     * 서명 검증을 통과한 광고 콜백
     */
    public record VerifiedCallback(
            String sessionId,
            String transactionId,
            String adUnit,
            long timestampMillis
    ) {
    }
}
