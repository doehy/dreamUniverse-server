package com.dreamuniverse_server.dream.statistics.service;

import com.dreamuniverse_server.dream.statistics.dto.response.DreamDnaResponse;
import com.dreamuniverse_server.dream.statistics.type.DreamType;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;

@Service
public class DreamTypeService {

    public DreamType classify(
            DreamDnaResponse dna
    ) {
        return Arrays.stream(
                        DreamType.values()
                )
                .min(
                        Comparator.comparingLong(
                                dreamType ->
                                        calculateDistance(
                                                dna,
                                                dreamType
                                        )
                        )
                )
                .orElseThrow();
    }

    private long calculateDistance(
            DreamDnaResponse dna,
            DreamType dreamType
    ) {
        return square(
                dna.emotionIntensity()
                        - dreamType.getEmotionIntensity()
        )
                + square(
                dna.tension()
                        - dreamType.getTension()
        )
                + square(
                dna.lucidity()
                        - dreamType.getLucidity()
        )
                + square(
                dna.recurrence()
                        - dreamType.getRecurrence()
        )
                + square(
                dna.relationship()
                        - dreamType.getRelationship()
        )
                + square(
                dna.symbolism()
                        - dreamType.getSymbolism()
        )
                + square(
                dna.dynamism()
                        - dreamType.getDynamism()
        )
                + square(
                dna.fantasy()
                        - dreamType.getFantasy()
        );
    }

    private long square(
            int value
    ) {
        return (long) value * value;
    }
}