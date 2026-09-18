package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.client.DreamAiClient;
import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult;
import com.dreamuniverse_server.dream.dto.request.DreamCreateRequest;
import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.dto.response.DreamAiUsageResponse;
import com.dreamuniverse_server.dream.repository.DreamAnalysisRepository;
import com.dreamuniverse_server.dream.repository.DreamElementRepository;
import com.dreamuniverse_server.dream.repository.DreamEmotionRepository;
import com.dreamuniverse_server.dream.dto.response.DreamAnalysisResponse;
import com.dreamuniverse_server.dream.entity.DreamAnalysis;
import com.dreamuniverse_server.dream.entity.DreamElement;
import com.dreamuniverse_server.dream.entity.DreamEmotion;
import com.dreamuniverse_server.dream.entity.DreamElementType;
import com.dreamuniverse_server.dream.entity.DreamEmotionType;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.user.entity.OAuthProvider;
import com.dreamuniverse_server.dream.repository.DreamRepository;
import com.dreamuniverse_server.user.entity.User;
import com.dreamuniverse_server.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DreamServiceTest {

    @Mock
    private DreamRepository dreamRepository;

    @Mock
    private DreamAnalysisRepository dreamAnalysisRepository;

    @Mock
    private DreamEmotionRepository dreamEmotionRepository;

    @Mock
    private DreamElementRepository dreamElementRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DreamCommandService dreamCommandService;

    @Mock
    private DreamAiClient dreamAiClient;

    @Mock
    private DreamAiUsageService dreamAiUsageService;

    @Mock
    private DreamAiCompletionService dreamAiCompletionService;

    @InjectMocks
    private DreamService dreamService;


    @Test
    void OpenAI_호출이_실패하면_선점한_AI_사용량을_반환한다() {

        // given
        Long userId = 1L;

        LocalDate dreamDate =
                LocalDate.of(2026, 9, 17);

        LocalDate usageDate =
                LocalDate.of(2026, 9, 17);

        DreamCreateRequest request =
                new DreamCreateRequest(
                        "바다 꿈",
                        "학교에서 시험을 보는데 교실 바닥이 바다로 변했다.",
                        dreamDate,
                        true
                );

        User user = mock(User.class);
        Dream dream = mock(Dream.class);

        given(user.getId())
                .willReturn(userId);

        given(user.isSignupCompleted())
                .willReturn(true);

        given(
                userRepository.findById(userId)
        )
                .willReturn(
                        Optional.of(user)
                );

        given(
                dreamRepository.existsByUserIdAndDreamDate(
                        userId,
                        dreamDate
                )
        )
                .willReturn(false);

        given(
                dreamCommandService.create(
                        user,
                        request.title(),
                        request.content(),
                        request.dreamDate()
                )
        )
                .willReturn(dream);

        /*
         * AI 사용권 선점 성공
         */
        given(
                dreamAiUsageService.acquire(user)
        )
                .willReturn(usageDate);

        /*
         * OpenAI 호출 실패 상황을 강제로 만든다.
         */
        given(
                dreamAiClient.analyze(
                        request.content()
                )
        )
                .willThrow(
                        new RuntimeException(
                                "OpenAI 호출 실패"
                        )
                );


        // when & then
        assertThatThrownBy(
                () -> dreamService.createDream(
                        userId,
                        request
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("OpenAI 호출 실패");


        /*
         * 가장 중요한 검증.
         *
         * OpenAI가 실패했으므로
         * 앞에서 acquire한 사용권을 돌려줘야 한다.
         */
        verify(
                dreamAiUsageService
        )
                .release(
                        userId,
                        usageDate
                );


        /*
         * OpenAI 자체가 실패했기 때문에
         * AI 결과 저장은 실행되면 안 된다.
         */
        verifyNoInteractions(
                dreamAiCompletionService
        );
    }


    @Test
    void AI_분석결과_저장이_실패해도_선점한_AI_사용량을_반환한다() {

        // given
        Long userId = 1L;

        LocalDate dreamDate =
                LocalDate.of(2026, 9, 17);

        LocalDate usageDate =
                LocalDate.of(2026, 9, 17);

        DreamCreateRequest request =
                new DreamCreateRequest(
                        "바다 꿈",
                        "학교에서 시험을 보는데 교실 바닥이 바다로 변했다.",
                        dreamDate,
                        true
                );

        User user = mock(User.class);
        Dream dream = mock(Dream.class);
        DreamAnalysisResult result =
                mock(DreamAnalysisResult.class);

        given(user.getId())
                .willReturn(userId);

        given(user.isSignupCompleted())
                .willReturn(true);

        given(
                userRepository.findById(userId)
        )
                .willReturn(
                        Optional.of(user)
                );

        given(
                dreamRepository.existsByUserIdAndDreamDate(
                        userId,
                        dreamDate
                )
        )
                .willReturn(false);

        given(
                dreamCommandService.create(
                        user,
                        request.title(),
                        request.content(),
                        request.dreamDate()
                )
        )
                .willReturn(dream);

        given(
                dreamAiUsageService.acquire(user)
        )
                .willReturn(usageDate);

        /*
         * OpenAI 분석 자체는 성공.
         */
        given(
                dreamAiClient.analyze(
                        request.content()
                )
        )
                .willReturn(result);

        /*
         * 하지만 DB에 분석 결과 저장 중 실패했다고 가정.
         */
        doThrow(
                new RuntimeException(
                        "AI 결과 저장 실패"
                )
        )
                .when(
                        dreamAiCompletionService
                )
                .complete(
                        dream,
                        result
                );


        // when & then
        assertThatThrownBy(
                () -> dreamService.createDream(
                        userId,
                        request
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("AI 결과 저장 실패");


        /*
         * 결과 저장이 실패해도
         * 사용 횟수는 복구되어야 한다.
         */
        verify(
                dreamAiUsageService
        )
                .release(
                        userId,
                        usageDate
                );
    }


    @Test
    void AI_처리가_정상적으로_완료되면_사용량을_반환하지_않는다() {

        // given
        Long userId = 1L;

        LocalDate dreamDate =
                LocalDate.of(2026, 9, 17);

        LocalDate usageDate =
                LocalDate.of(2026, 9, 17);

        DreamCreateRequest request =
                new DreamCreateRequest(
                        "바다 꿈",
                        "학교에서 시험을 보는데 교실 바닥이 바다로 변했다.",
                        dreamDate,
                        true
                );

        User user = mock(User.class);
        Dream dream = mock(Dream.class);
        DreamAnalysisResult result =
                mock(DreamAnalysisResult.class);


        given(
                userRepository.findById(userId)
        )
                .willReturn(
                        Optional.of(user)
                );

        given(user.isSignupCompleted())
                .willReturn(true);

        given(
                dreamRepository.existsByUserIdAndDreamDate(
                        userId,
                        dreamDate
                )
        )
                .willReturn(false);


        given(
                dreamCommandService.create(
                        user,
                        request.title(),
                        request.content(),
                        request.dreamDate()
                )
        )
                .willReturn(dream);


        given(
                dreamAiUsageService.acquire(user)
        )
                .willReturn(usageDate);


        given(
                dreamAiClient.analyze(
                        request.content()
                )
        )
                .willReturn(result);


        // when
        dreamService.createDream(
                userId,
                request
        );


        // then
        verify(
                dreamAiCompletionService
        )
                .complete(
                        dream,
                        result
                );

        verify(
                dreamAiUsageService,
                never()
        )
                .release(
                        anyLong(),
                        any(LocalDate.class)
                );
    }

    @Test
    void AI_남은_사용횟수를_조회한다() {

        // given
        Long userId = 1L;

        User user = User.create(
                OAuthProvider.GOOGLE,
                "test-provider-user-id"
        );

        user.completeSignup(
                "test@test.com",
                "Test User",
                "tester",
                LocalDate.of(2000, 1, 1),
                "Asia/Seoul",
                Instant.now()
        );

        given(
                userRepository.findById(userId)
        )
                .willReturn(
                        Optional.of(user)
                );

        given(
                dreamAiUsageService.getRemainingCount(user)
        )
                .willReturn(1);


        // when
        DreamAiUsageResponse response =
                dreamService.getAiUsage(userId);


        // then
        assertThat(response.remainingCount())
                .isEqualTo(1);

        assertThat(response.dailyLimit())
                .isEqualTo(2);

        verify(
                dreamAiUsageService
        )
                .getRemainingCount(user);
    }

    @Test
    void 꿈의_AI_분석결과를_조회한다() {

        // given
        Long userId = 1L;
        Long dreamId = 10L;
        Long analysisId = 100L;

        User user = User.create(
                OAuthProvider.GOOGLE,
                "analysis-test-user"
        );

        user.completeSignup(
                "analysis@test.com",
                "Test User",
                "tester",
                LocalDate.of(2000, 1, 1),
                "Asia/Seoul",
                Instant.now()
        );

        /*
         * DreamAnalysisResponse.from()에서 필요한 값만
         * Entity mock으로 준비한다.
         *
         * Dream, Analysis, Emotion, Element의 정확한 factory 메서드는
         * 현재 테스트 목적과 관계없으므로 여기서는 필요한 getter만 mock한다.
         */
        Dream dream = mock(Dream.class);
        DreamAnalysis analysis = mock(DreamAnalysis.class);

        DreamEmotion anxiety = mock(DreamEmotion.class);
        DreamEmotion confusion = mock(DreamEmotion.class);

        DreamElement place = mock(DreamElement.class);
        DreamElement event = mock(DreamElement.class);


        given(
                userRepository.findById(userId)
        )
                .willReturn(Optional.of(user));

        given(
                dreamRepository.findByIdAndUserId(
                        dreamId,
                        user.getId()
                )
        )
                .willReturn(Optional.of(dream));

        given(dream.getId())
                .willReturn(dreamId);

        given(
                dreamAnalysisRepository.findByDreamId(dreamId)
        )
                .willReturn(Optional.of(analysis));

        given(analysis.getId())
                .willReturn(analysisId);

        given(analysis.getInterpretation())
                .willReturn(
                        "시험에 대한 압박감과 불확실한 감정이 반영된 꿈입니다."
                );


        given(anxiety.getEmotionType())
                .willReturn(DreamEmotionType.ANXIETY);

        given(anxiety.getScore())
                .willReturn(80);

        given(confusion.getEmotionType())
                .willReturn(DreamEmotionType.CONFUSION);

        given(confusion.getScore())
                .willReturn(55);


        given(place.getElementType())
                .willReturn(DreamElementType.PLACE);

        given(place.getElementValue())
                .willReturn("학교");

        given(event.getElementType())
                .willReturn(DreamElementType.EVENT);

        given(event.getElementValue())
                .willReturn("시험");


        given(
                dreamEmotionRepository.findAllByAnalysisId(
                        analysisId
                )
        )
                .willReturn(
                        List.of(
                                anxiety,
                                confusion
                        )
                );

        given(
                dreamElementRepository.findAllByAnalysisId(
                        analysisId
                )
        )
                .willReturn(
                        List.of(
                                place,
                                event
                        )
                );


        // when
        DreamAnalysisResponse response =
                dreamService.getAnalysis(
                        userId,
                        dreamId
                );


        // then
        assertThat(response.interpretation())
                .isEqualTo(
                        "시험에 대한 압박감과 불확실한 감정이 반영된 꿈입니다."
                );

        assertThat(response.emotions())
                .hasSize(2);

        assertThat(response.emotions().get(0).type())
                .isEqualTo("ANXIETY");

        assertThat(response.emotions().get(0).score())
                .isEqualTo(80);

        assertThat(response.emotions().get(1).type())
                .isEqualTo("CONFUSION");

        assertThat(response.emotions().get(1).score())
                .isEqualTo(55);


        assertThat(response.elements())
                .hasSize(2);

        assertThat(response.elements().get(0).type())
                .isEqualTo("PLACE");

        assertThat(response.elements().get(0).value())
                .isEqualTo("학교");

        assertThat(response.elements().get(1).type())
                .isEqualTo("EVENT");

        assertThat(response.elements().get(1).value())
                .isEqualTo("시험");


        verify(
                dreamAnalysisRepository
        )
                .findByDreamId(dreamId);

        verify(
                dreamEmotionRepository
        )
                .findAllByAnalysisId(analysisId);

        verify(
                dreamElementRepository
        )
                .findAllByAnalysisId(analysisId);
    }

    @Test
    void AI_분석결과가_없으면_예외가_발생한다() {

        // given
        Long userId = 1L;
        Long dreamId = 10L;

        User user = User.create(
                OAuthProvider.GOOGLE,
                "analysis-not-found-user"
        );

        user.completeSignup(
                "analysis-not-found@test.com",
                "Test User",
                "tester",
                LocalDate.of(2000, 1, 1),
                "Asia/Seoul",
                Instant.now()
        );

        Dream dream = mock(Dream.class);

        given(
                userRepository.findById(userId)
        )
                .willReturn(
                        Optional.of(user)
                );

        given(
                dreamRepository.findByIdAndUserId(
                        dreamId,
                        user.getId()
                )
        )
                .willReturn(
                        Optional.of(dream)
                );

        given(dream.getId())
                .willReturn(dreamId);

        given(
                dreamAnalysisRepository.findByDreamId(
                        dreamId
                )
        )
                .willReturn(
                        Optional.empty()
                );


        // when & then
        assertThatThrownBy(
                () -> dreamService.getAnalysis(
                        userId,
                        dreamId
                )
        )
                .isInstanceOf(CustomException.class);
    }
}