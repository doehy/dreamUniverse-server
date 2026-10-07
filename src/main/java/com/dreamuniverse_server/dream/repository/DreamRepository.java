package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamExperienceStatisticsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DreamRepository extends JpaRepository<Dream, Long> {

    long countByUserIdAndDreamDate(
            Long userId,
            LocalDate dreamDate
    );

    Optional<Dream> findByIdAndUserId(
            Long dreamId,
            Long userId
    );

    List<Dream> findAllByUserIdOrderByDreamDateDesc(
            Long userId
    );

    @Query(
            value = """
                select
                    count(*) as dreamCount,

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
                                when d.nightmare = true then 1
                                else 0
                            end
                        ),
                        0
                    ) as nightmareCount,

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
    DreamExperienceStatisticsProjection findExperienceStatistics(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}