//package com.dreamuniverse_server.dream.service;
//
//import com.dreamuniverse_server.dream.dto.request.DreamCreateRequest;
//import com.dreamuniverse_server.dream.dto.request.DreamUpdateRequest;
//import com.dreamuniverse_server.dream.entity.Dream;
//import com.dreamuniverse_server.dream.repository.DreamAnalysisRepository;
//import com.dreamuniverse_server.dream.repository.DreamElementRepository;
//import com.dreamuniverse_server.dream.repository.DreamEmotionRepository;
//import com.dreamuniverse_server.dream.repository.DreamRepository;
//import com.dreamuniverse_server.user.entity.User;
//import com.dreamuniverse_server.user.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDate;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class DreamServiceTest {
//
//    @Mock
//    private DreamRepository dreamRepository;
//
//    @Mock
//    private DreamAnalysisRepository dreamAnalysisRepository;
//
//    @Mock
//    private DreamEmotionRepository dreamEmotionRepository;
//
//    @Mock
//    private DreamElementRepository dreamElementRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private DreamCommandService dreamCommandService;
//
//    @Mock
//    private DreamAiUsageService dreamAiUsageService;
//
//    @Mock
//    private DreamAiStatusService dreamAiStatusService;
//
//    @Mock
//    private DreamAiAsyncService dreamAiAsyncService;
//
//    @Mock
//    private User user;
//
//    @Mock
//    private Dream dream;
//
//    private DreamService dreamService;
//
//
//    @BeforeEach
//    void setUp() {
//        dreamService =
//                new DreamService(
//                        dreamRepository,
//                        dreamAnalysisRepository,
//                        dreamEmotionRepository,
//                        dreamElementRepository,
//                        userRepository,
//                        dreamCommandService,
//                        dreamAiUsageService,
//                        dreamAiStatusService,
//                        dreamAiAsyncService
//                );
//    }
//
//
//    @Test
//    void 꿈_생성시_AI를_요청하면_PROCESSING으로_변경하고_비동기작업을_시작한다() {
//
//        // given
//        Long userId = 1L;
//        Long dreamId = 10L;
//
//        LocalDate dreamDate =
//                LocalDate.of(
//                        2026,
//                        9,
//                        23
//                );
//
//        LocalDate usageDate =
//                LocalDate.of(
//                        2026,
//                        9,
//                        23
//                );
//
//        String title =
//                "시험 꿈";
//
//        String content =
//                "학교에서 시험을 보다가 하늘을 날아다니는 꿈";
//
//
//        DreamCreateRequest request =
//                new DreamCreateRequest(
//                        title,
//                        content,
//                        dreamDate,
//                        true
//                );
//
//
//        given(
//                userRepository.findById(
//                        userId
//                )
//        ).willReturn(
//                Optional.of(user)
//        );
//
//        given(
//                user.isSignupCompleted()
//        ).willReturn(true);
//
//        given(
//                user.getId()
//        ).willReturn(userId);
//
//
//        given(
//                dreamRepository.existsByUserIdAndDreamDate(
//                        userId,
//                        dreamDate
//                )
//        ).willReturn(false);
//
//
//        given(
//                dreamCommandService.create(
//                        user,
//                        title,
//                        content,
//                        dreamDate
//                )
//        ).willReturn(
//                dream
//        );
//
//
//        given(
//                dream.getId()
//        ).willReturn(
//                dreamId
//        );
//
//
//        given(
//                dreamAiUsageService.acquire(
//                        user
//                )
//        ).willReturn(
//                usageDate
//        );
//
//
//        // when
//        dreamService.createDream(
//                userId,
//                request
//        );
//
//
//        // then
//        verify(dreamAiUsageService)
//                .acquire(
//                        user
//                );
//
//        verify(dreamAiStatusService)
//                .markProcessing(
//                        dreamId
//                );
//
//        verify(dream)
//                .startAiCompletion();
//
//        verify(dreamAiAsyncService)
//                .complete(
//                        userId,
//                        dreamId,
//                        content,
//                        usageDate
//                );
//
//
//        verify(
//                dreamAiStatusService,
//                never()
//        ).markFailed(
//                anyLong()
//        );
//
//        verify(
//                dreamAiUsageService,
//                never()
//        ).release(
//                anyLong(),
//                any(LocalDate.class)
//        );
//    }
//
//
//    @Test
//    void 꿈_생성시_AI를_요청하지_않으면_AI관련_서비스를_호출하지_않는다() {
//
//        // given
//        Long userId = 1L;
//
//        LocalDate dreamDate =
//                LocalDate.of(
//                        2026,
//                        9,
//                        23
//                );
//
//        String title =
//                "시험 꿈";
//
//        String content =
//                "학교에서 시험을 보는 꿈";
//
//
//        DreamCreateRequest request =
//                new DreamCreateRequest(
//                        title,
//                        content,
//                        dreamDate,
//                        false
//                );
//
//
//        given(
//                userRepository.findById(
//                        userId
//                )
//        ).willReturn(
//                Optional.of(user)
//        );
//
//        given(
//                user.isSignupCompleted()
//        ).willReturn(true);
//
//        given(
//                user.getId()
//        ).willReturn(userId);
//
//
//        given(
//                dreamRepository.existsByUserIdAndDreamDate(
//                        userId,
//                        dreamDate
//                )
//        ).willReturn(false);
//
//
//        given(
//                dreamCommandService.create(
//                        user,
//                        title,
//                        content,
//                        dreamDate
//                )
//        ).willReturn(
//                dream
//        );
//
//
//        // when
//        dreamService.createDream(
//                userId,
//                request
//        );
//
//
//        // then
//        verifyNoInteractions(
//                dreamAiUsageService,
//                dreamAiStatusService,
//                dreamAiAsyncService
//        );
//    }
//
//
////    @Test
////    void 꿈_수정시_AI를_요청하면_PROCESSING으로_변경하고_비동기작업을_시작한다() {
////
////        // given
////        Long userId = 1L;
////        Long dreamId = 10L;
////
////        LocalDate usageDate =
////                LocalDate.of(
////                        2026,
////                        9,
////                        23
////                );
////
////        String title =
////                "수정된 꿈";
////
////        String content =
////                "바다 위를 걸어가는 꿈";
////
////
////        DreamUpdateRequest request =
////                new DreamUpdateRequest(
////                        title,
////                        content,
////                        true
////                );
////
////
////        given(
////                userRepository.findById(
////                        userId
////                )
////        ).willReturn(
////                Optional.of(user)
////        );
////
////        given(
////                user.isSignupCompleted()
////        ).willReturn(true);
////
////        given(
////                user.getId()
////        ).willReturn(userId);
////
////
////        given(
////                dreamCommandService.update(
////                        dreamId,
////                        userId,
////                        title,
////                        content
////                )
////        ).willReturn(
////                dream
////        );
////
////
////        given(
////                dream.getId()
////        ).willReturn(
////                dreamId
////        );
////
////
////        given(
////                dreamAiUsageService.acquire(
////                        user
////                )
////        ).willReturn(
////                usageDate
////        );
////
////
////        // when
////        dreamService.updateDream(
////                userId,
////                dreamId,
////                request
////        );
////
////
////        // then
////        verify(dreamAiStatusService)
////                .markProcessing(
////                        dreamId
////                );
////
////        verify(dream)
////                .startAiCompletion();
////
////        verify(dreamAiAsyncService)
////                .complete(
////                        userId,
////                        dreamId,
////                        content,
////                        usageDate
////                );
////    }
//
//
//    @Test
//    void 비동기작업_시작에_실패하면_FAILED로_변경하고_AI사용량을_복구한다() {
//
//        // given
//        Long userId = 1L;
//        Long dreamId = 10L;
//
//        LocalDate dreamDate =
//                LocalDate.of(
//                        2026,
//                        9,
//                        23
//                );
//
//        LocalDate usageDate =
//                LocalDate.of(
//                        2026,
//                        9,
//                        23
//                );
//
//        String title =
//                "시험 꿈";
//
//        String content =
//                "학교에서 시험을 보는 꿈";
//
//
//        DreamCreateRequest request =
//                new DreamCreateRequest(
//                        title,
//                        content,
//                        dreamDate,
//                        true
//                );
//
//
//        given(
//                userRepository.findById(
//                        userId
//                )
//        ).willReturn(
//                Optional.of(user)
//        );
//
//        given(
//                user.isSignupCompleted()
//        ).willReturn(true);
//
//        given(
//                user.getId()
//        ).willReturn(userId);
//
//
//        given(
//                dreamRepository.existsByUserIdAndDreamDate(
//                        userId,
//                        dreamDate
//                )
//        ).willReturn(false);
//
//
//        given(
//                dreamCommandService.create(
//                        user,
//                        title,
//                        content,
//                        dreamDate
//                )
//        ).willReturn(
//                dream
//        );
//
//
//        given(
//                dream.getId()
//        ).willReturn(
//                dreamId
//        );
//
//
//        given(
//                dreamAiUsageService.acquire(
//                        user
//                )
//        ).willReturn(
//                usageDate
//        );
//
//
//        doThrow(
//                new RuntimeException(
//                        "async start failure"
//                )
//        )
//                .when(dreamAiAsyncService)
//                .complete(
//                        userId,
//                        dreamId,
//                        content,
//                        usageDate
//                );
//
//
//        // when & then
//        assertThatThrownBy(
//                () -> dreamService.createDream(
//                        userId,
//                        request
//                )
//        )
//                .isInstanceOf(RuntimeException.class)
//                .hasMessage(
//                        "async start failure"
//                );
//
//
//        verify(dreamAiStatusService)
//                .markProcessing(
//                        dreamId
//                );
//
//        verify(dream)
//                .startAiCompletion();
//
//
//        verify(dreamAiStatusService)
//                .markFailed(
//                        dreamId
//                );
//
//        verify(dream)
//                .failAiCompletion();
//
//
//        verify(dreamAiUsageService)
//                .release(
//                        userId,
//                        usageDate
//                );
//    }
//}