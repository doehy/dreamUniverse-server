package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.ad.service.RewardedAdSessionService;
import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.entity.DreamAiStatus;
import com.dreamuniverse_server.dream.policy.DreamUsagePolicy;
import com.dreamuniverse_server.dream.repository.DreamRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DreamAiRequestServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long DREAM_ID = 10L;

    private static final String SESSION_ID =
            "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private DreamRepository dreamRepository;

    @Mock
    private DreamAiUsageService dreamAiUsageService;

    @Mock
    private DreamEntitlementService entitlementService;

    @Mock
    private RewardedAdSessionService rewardedAdSessionService;

    @Mock
    private User user;

    @Mock
    private Dream dream;

    @InjectMocks
    private DreamAiRequestService requestService;

    @BeforeEach
    void setUp() {
        when(dreamRepository.findByIdAndUserIdForUpdate(
                DREAM_ID,
                USER_ID
        )).thenReturn(Optional.of(dream));
    }

    @Test
    void 무료_사용자가_광고_보상을_제출하면_분석을_시작한다() {
        LocalDate usageDate = LocalDate.of(2026, 10, 16);

        when(dream.getAiStatus())
                .thenReturn(DreamAiStatus.NOT_REQUESTED);

        when(entitlementService.getPolicy(USER_ID))
                .thenReturn(new DreamUsagePolicy(3, 4, true));

        when(dreamAiUsageService.acquire(user))
                .thenReturn(usageDate);

        LocalDate result = requestService.prepare(
                USER_ID,
                user,
                DREAM_ID,
                SESSION_ID
        );

        assertEquals(usageDate, result);

        InOrder order = inOrder(
                dreamAiUsageService,
                rewardedAdSessionService,
                dream
        );

        order.verify(dreamAiUsageService).acquire(user);

        order.verify(rewardedAdSessionService)
                .consumeVerifiedReward(
                        USER_ID,
                        SESSION_ID
                );

        order.verify(dream).startAiCompletion();
    }

    @Test
    void 무료_사용자가_광고_없이_요청하면_거부한다() {
        when(dream.getAiStatus())
                .thenReturn(DreamAiStatus.NOT_REQUESTED);

        when(entitlementService.getPolicy(USER_ID))
                .thenReturn(new DreamUsagePolicy(3, 4, true));

        assertThrows(
                CustomException.class,
                () -> requestService.prepare(
                        USER_ID,
                        user,
                        DREAM_ID,
                        null
                )
        );

        verifyNoInteractions(
                dreamAiUsageService,
                rewardedAdSessionService
        );

        verify(dream, never()).startAiCompletion();
    }

    @Test
    void 이미_분석_중이면_광고와_사용량을_소비하지_않는다() {
        when(dream.getAiStatus())
                .thenReturn(DreamAiStatus.PROCESSING);

        assertThrows(
                CustomException.class,
                () -> requestService.prepare(
                        USER_ID,
                        user,
                        DREAM_ID,
                        SESSION_ID
                )
        );

        verifyNoInteractions(
                dreamAiUsageService,
                rewardedAdSessionService,
                entitlementService
        );

        verify(dream, never()).startAiCompletion();
    }

    @Test
    void 하루_한도를_초과하면_광고_보상을_소비하지_않는다() {
        when(dream.getAiStatus())
                .thenReturn(DreamAiStatus.COMPLETED);

        when(entitlementService.getPolicy(USER_ID))
                .thenReturn(new DreamUsagePolicy(3, 4, true));

        when(dreamAiUsageService.acquire(user))
                .thenThrow(new CustomException(
                        com.dreamuniverse_server.global.exception
                                .ErrorCode.DREAM_AI_DAILY_LIMIT_EXCEEDED
                ));

        assertThrows(
                CustomException.class,
                () -> requestService.prepare(
                        USER_ID,
                        user,
                        DREAM_ID,
                        SESSION_ID
                )
        );

        verifyNoInteractions(rewardedAdSessionService);
        verify(dream, never()).startAiCompletion();
    }

    @Test
    void 광고_보상_소비에_실패하면_PROCESSING으로_변경하지_않는다() {
        when(dream.getAiStatus())
                .thenReturn(DreamAiStatus.NOT_REQUESTED);

        when(entitlementService.getPolicy(USER_ID))
                .thenReturn(new DreamUsagePolicy(3, 4, true));

        when(dreamAiUsageService.acquire(user))
                .thenReturn(LocalDate.now());

        doThrow(new CustomException(
                com.dreamuniverse_server.global.exception
                        .ErrorCode.AD_REWARD_NOT_AVAILABLE
        )).when(rewardedAdSessionService)
                .consumeVerifiedReward(USER_ID, SESSION_ID);

        assertThrows(
                CustomException.class,
                () -> requestService.prepare(
                        USER_ID,
                        user,
                        DREAM_ID,
                        SESSION_ID
                )
        );

        verify(dream, never()).startAiCompletion();
    }

    @Test
    void 광고_면제_정책이면_광고_없이_분석을_시작한다() {
        when(dream.getAiStatus())
                .thenReturn(DreamAiStatus.NOT_REQUESTED);

        when(entitlementService.getPolicy(USER_ID))
                .thenReturn(new DreamUsagePolicy(3, 8, false));

        when(dreamAiUsageService.acquire(user))
                .thenReturn(LocalDate.now());

        requestService.prepare(
                USER_ID,
                user,
                DREAM_ID,
                null
        );

        verifyNoInteractions(rewardedAdSessionService);
        verify(dream).startAiCompletion();
    }
}
