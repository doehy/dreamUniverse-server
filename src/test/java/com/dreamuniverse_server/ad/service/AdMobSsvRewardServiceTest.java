package com.dreamuniverse_server.ad.service;

import com.dreamuniverse_server.ad.entity.RewardedAdSession;
import com.dreamuniverse_server.ad.entity.RewardedAdStatus;
import com.dreamuniverse_server.ad.repository.RewardedAdSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdMobSsvRewardServiceTest {

    @Mock
    private RewardedAdSessionRepository sessionRepository;

    @InjectMocks
    private AdMobSsvRewardService rewardService;

    private RewardedAdSession session;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                rewardService,
                "rewardedAdUnitId",
                "ca-app-pub-6868205551979078/9405922508"
        );

        ReflectionTestUtils.setField(
                rewardService,
                "expectedRewardItem",
                "ai_completion"
        );

        ReflectionTestUtils.setField(
                rewardService,
                "expectedRewardAmount",
                1L
        );

        session = RewardedAdSession.create(
                1L,
                Instant.now().minusSeconds(10)
        );
    }

    @Test
    void 정상_콜백이면_광고_보상을_지급한다() {
        AdMobSsvVerifier.VerifiedCallback callback =
                callback(session.getId());

        when(sessionRepository.findByIdForUpdate(session.getId()))
                .thenReturn(Optional.of(session));

        when(sessionRepository.existsByAdmobTransactionId(
                "transaction-123"
        )).thenReturn(false);

        rewardService.grant(callback);

        assertEquals(
                RewardedAdStatus.VERIFIED,
                session.getStatus()
        );

        assertEquals(
                "transaction-123",
                session.getAdmobTransactionId()
        );

        assertNotNull(session.getVerifiedAt());
        assertNotNull(session.getRewardExpiresAt());

        long seconds = java.time.Duration.between(
                session.getVerifiedAt(),
                session.getRewardExpiresAt()
        ).toSeconds();

        assertEquals(600L, seconds);
    }

    @Test
    void 다른_광고_단위이면_보상을_지급하지_않는다() {
        var callback = new AdMobSsvVerifier.VerifiedCallback(
                session.getId(),
                "transaction-123",
                "9999999999",
                "ai_completion",
                1L,
                Instant.now().toEpochMilli()
        );

        rewardService.grant(callback);

        assertEquals(
                RewardedAdStatus.PENDING,
                session.getStatus()
        );

        verifyNoInteractions(sessionRepository);
    }

    @Test
    void 잘못된_보상_상품이면_지급하지_않는다() {
        var callback = new AdMobSsvVerifier.VerifiedCallback(
                session.getId(),
                "transaction-123",
                "9405922508",
                "wrong_reward",
                1L,
                Instant.now().toEpochMilli()
        );

        rewardService.grant(callback);

        assertEquals(
                RewardedAdStatus.PENDING,
                session.getStatus()
        );

        verifyNoInteractions(sessionRepository);
    }

    @Test
    void 보상_수량이_다르면_지급하지_않는다() {
        var callback = new AdMobSsvVerifier.VerifiedCallback(
                session.getId(),
                "transaction-123",
                "9405922508",
                "ai_completion",
                2L,
                Instant.now().toEpochMilli()
        );

        rewardService.grant(callback);

        assertEquals(
                RewardedAdStatus.PENDING,
                session.getStatus()
        );

        verifyNoInteractions(sessionRepository);
    }

    @Test
    void 만료된_광고_세션은_보상을_받을_수_없다() {
        RewardedAdSession expired =
                RewardedAdSession.create(
                        1L,
                        Instant.now().minusSeconds(31 * 60)
                );

        when(sessionRepository.findByIdForUpdate(expired.getId()))
                .thenReturn(Optional.of(expired));

        var callback = callback(expired.getId());

        rewardService.grant(callback);

        assertEquals(
                RewardedAdStatus.PENDING,
                expired.getStatus()
        );

        verify(sessionRepository, never())
                .existsByAdmobTransactionId(anyString());
    }

    @Test
    void 이미_검증된_세션에는_다른_거래로_재지급하지_않는다() {
        session.verify(
                "previous-transaction",
                Instant.now()
        );

        when(sessionRepository.findByIdForUpdate(session.getId()))
                .thenReturn(Optional.of(session));

        rewardService.grant(callback(session.getId()));

        assertEquals(
                "previous-transaction",
                session.getAdmobTransactionId()
        );

        verify(sessionRepository, never())
                .existsByAdmobTransactionId(anyString());
    }

    @Test
    void 다른_세션에서_사용된_거래_ID는_거부한다() {
        when(sessionRepository.findByIdForUpdate(session.getId()))
                .thenReturn(Optional.of(session));

        when(sessionRepository.existsByAdmobTransactionId(
                "transaction-123"
        )).thenReturn(true);

        rewardService.grant(callback(session.getId()));

        assertEquals(
                RewardedAdStatus.PENDING,
                session.getStatus()
        );
    }

    @Test
    void 동일_콜백이_재전송되어도_상태를_변경하지_않는다() {
        session.verify(
                "transaction-123",
                Instant.now()
        );

        Instant firstVerifiedAt = session.getVerifiedAt();

        when(sessionRepository.findByIdForUpdate(session.getId()))
                .thenReturn(Optional.of(session));

        rewardService.grant(callback(session.getId()));

        assertEquals(
                RewardedAdStatus.VERIFIED,
                session.getStatus()
        );

        assertEquals(
                firstVerifiedAt,
                session.getVerifiedAt()
        );

        verify(sessionRepository, never())
                .existsByAdmobTransactionId(anyString());
    }

    private AdMobSsvVerifier.VerifiedCallback callback(
            String sessionId
    ) {
        return new AdMobSsvVerifier.VerifiedCallback(
                sessionId,
                "transaction-123",
                "9405922508",
                "ai_completion",
                1L,
                Instant.now().toEpochMilli()
        );
    }
}
