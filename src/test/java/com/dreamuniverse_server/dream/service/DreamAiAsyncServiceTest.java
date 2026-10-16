//package com.dreamuniverse_server.dream.service;
//
//import com.dreamuniverse_server.dream.client.DreamAiClient;
//import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult;
//import com.dreamuniverse_server.user.entity.User;
//import com.dreamuniverse_server.user.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Optional;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class DreamAiAsyncServiceTest {
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private DreamAiClient dreamAiClient;
//
//    @Mock
//    private DreamImageService dreamImageService;
//
//    @Mock
//    private DreamAiCompletionService dreamAiCompletionService;
//
//    @Mock
//    private DreamAiStatusService dreamAiStatusService;
//
//    @Mock
//    private DreamAiUsageService dreamAiUsageService;
//
//    @Mock
//    private User user;
//
//    @Mock
//    private DreamAnalysisResult analysisResult;
//
//    private DreamAiAsyncService dreamAiAsyncService;
//
//
//    @BeforeEach
//    void setUp() {
//        dreamAiAsyncService =
//                new DreamAiAsyncService(
//                        userRepository,
//                        dreamAiClient,
//                        dreamImageService,
//                        dreamAiCompletionService,
//                        dreamAiStatusService,
//                        dreamAiUsageService
//                );
//    }
//
//
//    @Test
//    void AI_작업이_성공하면_분석_이미지생성_DB저장_기존이미지정리까지_수행한다() {
//
//        // given
//        Long userId = 1L;
//        Long dreamId = 10L;
//
//        String content =
//                "학교에서 시험을 보다가 하늘을 날아다니는 꿈";
//
//        LocalDate usageDate =
//                LocalDate.of(
//                        2026,
//                        9,
//                        23
//                );
//
//        String newImageKey =
//                "dreams/10/new.png";
//
//        List<String> oldImageKeys =
//                List.of(
//                        "dreams/10/old.png"
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
//
//        given(
//                dreamAiClient.analyze(
//                        content
//                )
//        ).willReturn(
//                analysisResult
//        );
//
//
//        given(
//                dreamImageService.generateAndUpload(
//                        user,
//                        dreamId,
//                        content
//                )
//        ).willReturn(
//                newImageKey
//        );
//
//
//        given(
//                dreamAiCompletionService.complete(
//                        dreamId,
//                        analysisResult,
//                        newImageKey
//                )
//        ).willReturn(
//                oldImageKeys
//        );
//
//
//        // when
//        dreamAiAsyncService.complete(
//                userId,
//                dreamId,
//                content,
//                usageDate
//        );
//
//
//        // then
//        verify(userRepository)
//                .findById(
//                        userId
//                );
//
//
//        verify(dreamAiClient)
//                .analyze(
//                        content
//                );
//
//
//        verify(dreamImageService)
//                .generateAndUpload(
//                        user,
//                        dreamId,
//                        content
//                );
//
//
//        verify(dreamAiCompletionService)
//                .complete(
//                        dreamId,
//                        analysisResult,
//                        newImageKey
//                );
//
//
//        verify(dreamImageService)
//                .deleteOldImages(
//                        oldImageKeys
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
//
//        verify(
//                dreamAiUsageService,
//                never()
//        ).release(
//                anyLong(),
//                any(LocalDate.class)
//        );
//
//
//        verify(
//                dreamImageService,
//                never()
//        ).deleteUploadedImage(
//                anyString()
//        );
//    }
//
//
//    @Test
//    void OpenAI_텍스트분석에_실패하면_FAILED처리하고_사용량을_반환한다() {
//
//        // given
//        Long userId = 1L;
//        Long dreamId = 10L;
//
//        String content =
//                "꿈 내용";
//
//        LocalDate usageDate =
//                LocalDate.of(
//                        2026,
//                        9,
//                        23
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
//
//        given(
//                dreamAiClient.analyze(
//                        content
//                )
//        ).willThrow(
//                new RuntimeException(
//                        "OpenAI failure"
//                )
//        );
//
//
//        // when
//        dreamAiAsyncService.complete(
//                userId,
//                dreamId,
//                content,
//                usageDate
//        );
//
//
//        // then
//        verify(dreamAiStatusService)
//                .markFailed(
//                        dreamId
//                );
//
//
//        verify(dreamAiUsageService)
//                .release(
//                        userId,
//                        usageDate
//                );
//
//
//        verify(
//                dreamImageService,
//                never()
//        ).generateAndUpload(
//                any(),
//                anyLong(),
//                anyString()
//        );
//
//
//        verify(
//                dreamAiCompletionService,
//                never()
//        ).complete(
//                anyLong(),
//                any(),
//                anyString()
//        );
//
//
//        verify(
//                dreamImageService,
//                never()
//        ).deleteUploadedImage(
//                anyString()
//        );
//    }
//
//
//    @Test
//    void DB_최종저장에_실패하면_새_R2이미지를_삭제하고_FAILED처리하고_사용량을_반환한다() {
//
//        // given
//        Long userId = 1L;
//        Long dreamId = 10L;
//
//        String content =
//                "꿈 내용";
//
//        LocalDate usageDate =
//                LocalDate.of(
//                        2026,
//                        9,
//                        23
//                );
//
//        String newImageKey =
//                "dreams/10/new.png";
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
//
//        given(
//                dreamAiClient.analyze(
//                        content
//                )
//        ).willReturn(
//                analysisResult
//        );
//
//
//        given(
//                dreamImageService.generateAndUpload(
//                        user,
//                        dreamId,
//                        content
//                )
//        ).willReturn(
//                newImageKey
//        );
//
//
//        given(
//                dreamAiCompletionService.complete(
//                        dreamId,
//                        analysisResult,
//                        newImageKey
//                )
//        ).willThrow(
//                new RuntimeException(
//                        "DB failure"
//                )
//        );
//
//
//        // when
//        dreamAiAsyncService.complete(
//                userId,
//                dreamId,
//                content,
//                usageDate
//        );
//
//
//        // then
//        verify(dreamImageService)
//                .deleteUploadedImage(
//                        newImageKey
//                );
//
//
//        verify(dreamAiStatusService)
//                .markFailed(
//                        dreamId
//                );
//
//
//        verify(dreamAiUsageService)
//                .release(
//                        userId,
//                        usageDate
//                );
//
//
//        verify(
//                dreamImageService,
//                never()
//        ).deleteOldImages(
//                anyList()
//        );
//    }
//}