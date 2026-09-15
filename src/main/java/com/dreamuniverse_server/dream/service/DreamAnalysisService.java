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

    public DreamAnalysis saveAnalysis(Dream dream, DreamAnalysisResult result) {
        DreamAnalysis analysis = dreamAnalysisRepository.findByDreamId(dream.getId())
                .map(existing -> {
                    existing.update(
                            result.getSummary(),
                            result.getInterpretation()
                    );
                    return existing;
                })
                .orElseGet(() ->
                        dreamAnalysisRepository.save(
                                DreamAnalysis.create(
                                        dream,
                                        result.getSummary(),
                                        result.getInterpretation()
                                )
                        )
                );

        dreamEmotionRepository.deleteAllByAnalysisId(analysis.getId());
        dreamElementRepository.deleteAllByAnalysisId(analysis.getId());

        List<DreamEmotion> emotions = result.getEmotions()
                .stream()
                .map(emotion ->
                        DreamEmotion.create(
                                analysis,
                                emotion.getType(),
                                emotion.getScore()
                        )
                )
                .toList();

        dreamEmotionRepository.saveAll(emotions);

        List<DreamElement> elements = result.getElements()
                .stream()
                .map(element ->
                        DreamElement.create(
                                analysis,
                                element.getType(),
                                element.getValue()
                        )
                )
                .toList();

        dreamElementRepository.saveAll(elements);

        return analysis;
    }
}