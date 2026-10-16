package com.dreamuniverse_server.ad.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdMobSsvVerifierTest {

    private static final long KEY_ID = 12345L;

    private AdMobSsvVerifier verifier;
    private PrivateKey privateKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("EC");

        generator.initialize(
                new ECGenParameterSpec("secp256r1")
        );

        KeyPair keyPair = generator.generateKeyPair();

        privateKey = keyPair.getPrivate();

        verifier = new AdMobSsvVerifier();

        // 실제 Google 네트워크 호출 없이
        // 테스트용 공개키를 캐시에 주입한다.
        ReflectionTestUtils.setField(
                verifier,
                "cachedKeys",
                Map.of(KEY_ID, keyPair.getPublic())
        );

        ReflectionTestUtils.setField(
                verifier,
                "cacheExpiresAt",
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void 정상_서명은_검증에_성공한다() throws Exception {
        String query = sign(validPayload());

        AdMobSsvVerifier.VerifiedCallback result =
                verifier.verify(query);

        assertEquals(
                "550e8400-e29b-41d4-a716-446655440000",
                result.sessionId()
        );
        assertEquals("transaction-123", result.transactionId());
        assertEquals("9405922508", result.adUnit());
        assertEquals("ai_completion", result.rewardItem());
        assertEquals(1L, result.rewardAmount());
        assertEquals(1760000000000L, result.timestampMillis());
    }

    @Test
    void 서명_이후_광고_단위를_변조하면_거부한다()
            throws Exception {

        String signedQuery = sign(validPayload());

        String tamperedQuery = signedQuery.replace(
                "ad_unit=9405922508",
                "ad_unit=9999999999"
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> verifier.verify(tamperedQuery)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );
    }

    @Test
    void 서명_이후_보상_수량을_변조하면_거부한다()
            throws Exception {

        String signedQuery = sign(validPayload());

        String tamperedQuery = signedQuery.replace(
                "reward_amount=1",
                "reward_amount=100"
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> verifier.verify(tamperedQuery)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );
    }

    @Test
    void 서명이_없는_요청은_거부한다() {
        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> verifier.verify(validPayload())
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );
    }

    @Test
    void 필수_파라미터가_없으면_서명이_정상이어도_거부한다()
            throws Exception {

        String payload = validPayload()
                .replace("&transaction_id=transaction-123", "");

        String query = sign(payload);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> verifier.verify(query)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );
    }

    @Test
    void 중복_파라미터가_있으면_거부한다()
            throws Exception {

        String query = sign(
                validPayload() + "&reward_amount=1"
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> verifier.verify(query)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );
    }

    @Test
    void 잘못된_DER_서명은_거부한다() {
        String invalidSignature =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(new byte[]{1, 2, 3});

        String query = validPayload()
                + "&signature=" + invalidSignature
                + "&key_id=" + KEY_ID;

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> verifier.verify(query)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );
    }

    private String validPayload() {
        return "ad_network=1234"
                + "&ad_unit=9405922508"
                + "&custom_data=550e8400-e29b-41d4-a716-446655440000"
                + "&reward_amount=1"
                + "&reward_item=ai_completion"
                + "&timestamp=1760000000000"
                + "&transaction_id=transaction-123";
    }

    private String sign(String payload) throws Exception {
        Signature signature =
                Signature.getInstance("SHA256withECDSA");

        signature.initSign(privateKey);
        signature.update(
                payload.getBytes(StandardCharsets.UTF_8)
        );

        String encoded =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(signature.sign());

        return payload
                + "&signature=" + encoded
                + "&key_id=" + KEY_ID;
    }
}
