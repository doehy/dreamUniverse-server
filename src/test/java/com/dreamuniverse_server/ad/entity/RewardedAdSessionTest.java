package com.dreamuniverse_server.ad.entity;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RewardedAdSessionTest {

    @Test
    void 생성된_세션은_30분간_검증을_기다린다() {
        Instant now = Instant.parse("2026-10-16T10:00:00Z");

        RewardedAdSession session =
                RewardedAdSession.create(1L, now);

        assertEquals(
                RewardedAdStatus.PENDING,
                session.getStatus()
        );

        assertEquals(
                now.plus(Duration.ofMinutes(30)),
                session.getVerificationExpiresAt()
        );

        assertNull(session.getRewardExpiresAt());
    }

    @Test
    void 광고_검증_성공시_보상은_10분간_유효하다() {
        Instant createdAt =
                Instant.parse("2026-10-16T10:00:00Z");

        Instant verifiedAt = createdAt.plusSeconds(60);

        RewardedAdSession session =
                RewardedAdSession.create(1L, createdAt);

        session.verify("transaction-1", verifiedAt);

        assertEquals(
                RewardedAdStatus.VERIFIED,
                session.getStatus()
        );

        assertEquals(
                verifiedAt.plus(Duration.ofMinutes(10)),
                session.getRewardExpiresAt()
        );
    }

    @Test
    void 검증_만료된_세션은_보상을_받지_못한다() {
        Instant now =
                Instant.parse("2026-10-16T10:00:00Z");

        RewardedAdSession session =
                RewardedAdSession.create(1L, now);

        assertThrows(
                IllegalStateException.class,
                () -> session.verify(
                        "transaction-1",
                        now.plus(Duration.ofMinutes(30))
                )
        );

        assertEquals(
                RewardedAdStatus.PENDING,
                session.getStatus()
        );
    }

    @Test
    void 보상은_정확히_한_번만_소비할_수_있다() {
        Instant now =
                Instant.parse("2026-10-16T10:00:00Z");

        RewardedAdSession session =
                RewardedAdSession.create(1L, now);

        session.verify(
                "transaction-1",
                now.plusSeconds(60)
        );

        Instant consumedAt = now.plusSeconds(120);

        session.consume(consumedAt);

        assertEquals(
                RewardedAdStatus.CONSUMED,
                session.getStatus()
        );

        assertEquals(
                consumedAt,
                session.getConsumedAt()
        );

        assertThrows(
                IllegalStateException.class,
                () -> session.consume(consumedAt.plusSeconds(1))
        );
    }

    @Test
    void 보상_사용기간이_끝나면_소비할_수_없다() {
        Instant now =
                Instant.parse("2026-10-16T10:00:00Z");

        RewardedAdSession session =
                RewardedAdSession.create(1L, now);

        Instant verifiedAt = now.plusSeconds(60);

        session.verify("transaction-1", verifiedAt);

        assertThrows(
                IllegalStateException.class,
                () -> session.consume(
                        verifiedAt.plus(Duration.ofMinutes(10))
                )
        );

        assertEquals(
                RewardedAdStatus.VERIFIED,
                session.getStatus()
        );
    }
}
