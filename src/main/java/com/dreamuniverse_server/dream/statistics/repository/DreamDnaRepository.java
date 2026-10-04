package com.dreamuniverse_server.dream.statistics.repository;

import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamDnaAiScoreProjection;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamDnaElementProjection;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamDnaEmotionProjection;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamDnaExperienceProjection;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamDnaRecurrenceProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface DreamDnaRepository
        extends Repository<Dream, Long> {

    @Query(
            value = """
                    select
                        count(*) as dreamCount,
                        coalesce(
                            sum(
                                case
                                    when d.nightmare = true then 1
                                    else 0
                                end
                            ),
                            0
                        ) as nightmareCount,
                        coalesce(
                            sum(
                                case
                                    when d.sleep_paralysis = true then 1
                                    else 0
                                end
                            ),
                            0
                        ) as sleepParalysisCount,
                        coalesce(
                            sum(
                                case
                                    when d.lucid_dream = true then 1
                                    else 0
                                end
                            ),
                            0
                        ) as lucidDreamCount
                    from dreams d
                    where d.user_id = :userId
                      and d.dream_date between :startDate and :endDate
                    """,
            nativeQuery = true
    )
    DreamDnaExperienceProjection findExperienceScores(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(
            value = """
                    select
                        avg(emotion_data.max_score)
                            as averageEmotionPeak,

                        avg(emotion_data.negative_score)
                            as averageNegativeEmotion
                    from (
                        select
                            da.id as analysis_id,

                            max(de.score)
                                as max_score,

                            sum(
                                case
                                    when de.emotion_type in (
                                        'ANXIETY',
                                        'FEAR',
                                        'SADNESS',
                                        'ANGER',
                                        'CONFUSION'
                                    )
                                    then de.score
                                    else 0
                                end
                            ) as negative_score

                        from dream_analyses da

                        join dreams d
                            on d.id = da.dream_id

                        join dream_emotions de
                            on de.analysis_id = da.id

                        where d.user_id = :userId
                          and d.dream_date between :startDate and :endDate

                        group by da.id
                    ) emotion_data
                    """,
            nativeQuery = true
    )
    DreamDnaEmotionProjection findEmotionScores(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(
            value = """
                    select
                        avg(da.symbolism_score)
                            as averageSymbolismScore,

                        avg(da.dynamism_score)
                            as averageDynamismScore,

                        avg(da.fantasy_score)
                            as averageFantasyScore

                    from dream_analyses da

                    join dreams d
                        on d.id = da.dream_id

                    where d.user_id = :userId
                      and d.dream_date between :startDate and :endDate
                    """,
            nativeQuery = true
    )
    DreamDnaAiScoreProjection findAiScores(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(
            value = """
                    select
                        coalesce(
                            sum(element_data.dream_count),
                            0
                        ) as totalAppearanceCount,

                        coalesce(
                            sum(
                                case
                                    when element_data.dream_count > 1
                                    then element_data.dream_count - 1
                                    else 0
                                end
                            ),
                            0
                        ) as repeatedAppearanceCount

                    from (
                        select
                            de.element_type,
                            de.element_value,
                            count(distinct d.id) as dream_count

                        from dream_elements de

                        join dream_analyses da
                            on da.id = de.analysis_id

                        join dreams d
                            on d.id = da.dream_id

                        where d.user_id = :userId
                          and d.dream_date between :startDate and :endDate
                          and de.element_type in (
                              'PERSON',
                              'PLACE',
                              'OBJECT'
                          )

                        group by
                            de.element_type,
                            de.element_value
                    ) element_data
                    """,
            nativeQuery = true
    )
    DreamDnaRecurrenceProjection findRecurrenceScores(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(
            value = """
                    select
                        count(*) as totalElementCount,

                        coalesce(
                            sum(
                                case
                                    when de.element_type = 'PERSON'
                                    then 1
                                    else 0
                                end
                            ),
                            0
                        ) as personElementCount

                    from dream_elements de

                    join dream_analyses da
                        on da.id = de.analysis_id

                    join dreams d
                        on d.id = da.dream_id

                    where d.user_id = :userId
                      and d.dream_date between :startDate and :endDate
                      and de.element_type in (
                          'PERSON',
                          'PLACE',
                          'OBJECT'
                      )
                    """,
            nativeQuery = true
    )
    DreamDnaElementProjection findElementScores(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}