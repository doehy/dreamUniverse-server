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
                            result.summary(),
                            result.interpretation()
                    );
                    return existing;
                })
                .orElseGet(() ->
                        dreamAnalysisRepository.save(
                                DreamAnalysis.create(
                                        dream,
                                        result.summary(),
                                        result.interpretation()
                                )
                        )
                );

        dreamEmotionRepository.deleteAllByAnalysisId(analysis.getId());
        dreamElementRepository.deleteAllByAnalysisId(analysis.getId());

        List<DreamEmotion> emotions = result.emotions()
                .stream()
                .map(emotion ->
                        DreamEmotion.create(
                                analysis,
                                emotion.type(),
                                emotion.score()
                        )
                )
                .toList();

        dreamEmotionRepository.saveAll(emotions);

        List<DreamElement> elements = result.elements()
                .stream()
                .map(element ->
                        DreamElement.create(
                                analysis,
                                element.type(),
                                element.value()
                        )
                )
                .toList();

        dreamElementRepository.saveAll(elements);

        return analysis;
    }
}