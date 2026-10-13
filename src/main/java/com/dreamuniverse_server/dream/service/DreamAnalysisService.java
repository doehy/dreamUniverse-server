package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult;
import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.entity.DreamAnalysis;
import com.dreamuniverse_server.dream.entity.DreamElement;
import com.dreamuniverse_server.dream.entity.DreamEmotion;
import com.dreamuniverse_server.dream.repository.DreamAnalysisRepository;
import com.dreamuniverse_server.dream.repository.DreamElementRepository;
import com.dreamuniverse_server.dream.repository.DreamEmotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DreamAnalysisService {

    private final DreamAnalysisRepository dreamAnalysisRepository;
    private final DreamEmotionRepository dreamEmotionRepository;
    private final DreamElementRepository dreamElementRepository;

    public DreamAnalysis saveAnalysis(
            Dream dream,
            DreamAnalysisResult result
    ) {
        // 1. AI 응답에 중복 감정 유형이 존재하는지 확인
        long uniqueEmotionCount = result.getEmotions()
                .stream()
                .map(emotion -> emotion.getType())
                .distinct()
                .count();

        if (uniqueEmotionCount != result.getEmotions().size()) {
            throw new IllegalArgumentException(
                    "AI analysis contains duplicate emotion types"
            );
        }

        // 2. 기존 분석 조회 또는 새로운 분석 생성
        DreamAnalysis analysis =
                dreamAnalysisRepository.findByDreamId(dream.getId())
                        .map(existing -> {
                            existing.update(
                                    result.getInterpretation(),
                                    result.getSymbolismScore(),
                                    result.getDynamismScore(),
                                    result.getFantasyScore()
                            );

                            return existing;
                        })
                        .orElseGet(() ->
                                dreamAnalysisRepository.save(
                                        DreamAnalysis.create(
                                                dream,
                                                result.getInterpretation(),
                                                result.getSymbolismScore(),
                                                result.getDynamismScore(),
                                                result.getFantasyScore()
                                        )
                                )
                        );

        // 3. 기존 감정·요소를 벌크 DELETE로 즉시 삭제
        dreamEmotionRepository.deleteAllByAnalysisId(
                analysis.getId()
        );

        dreamElementRepository.deleteAllByAnalysisId(
                analysis.getId()
        );

        // 4. 새로운 감정 데이터 생성
        List<DreamEmotion> emotions =
                result.getEmotions()
                        .stream()
                        .map(emotion ->
                                DreamEmotion.create(
                                        analysis,
                                        emotion.getType(),
                                        emotion.getScore()
                                )
                        )
                        .toList();

        // 5. 새로운 감정 저장
        dreamEmotionRepository.saveAll(emotions);

        // 6. 새로운 요소 데이터 생성
        List<DreamElement> elements =
                result.getElements()
                        .stream()
                        .map(element ->
                                DreamElement.create(
                                        analysis,
                                        element.getType(),
                                        element.getValue()
                                )
                        )
                        .toList();

        // 7. 새로운 요소 저장
        dreamElementRepository.saveAll(elements);

        return analysis;
    }
}