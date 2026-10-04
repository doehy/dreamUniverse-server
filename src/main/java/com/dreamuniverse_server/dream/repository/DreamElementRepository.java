package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.DreamElement;
import com.dreamuniverse_server.dream.entity.DreamElementType;
import com.dreamuniverse_server.dream.repository.projection.DreamWorldElementProjection;
import com.dreamuniverse_server.dream.repository.projection.DreamWorldRandomDreamProjection;
import com.dreamuniverse_server.dream.repository.projection.DreamWorldRandomElementProjection;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamStatisticsElementProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DreamElementRepository
        extends JpaRepository<DreamElement, Long> {

    List<DreamElement> findAllByAnalysisId(Long analysisId);

    void deleteAllByAnalysisId(Long analysisId);

    @Query("""
            select
                element.elementValue as elementValue,
                count(distinct element.analysis.dream.id) as appearanceCount
            from DreamElement element
            where element.analysis.dream.user.id = :userId
              and element.elementType = :elementType
            group by element.elementValue
            order by
                count(distinct element.analysis.dream.id) desc,
                max(element.analysis.dream.dreamDate) desc
            """)
    List<DreamWorldElementProjection> findTopElements(
            @Param("userId") Long userId,
            @Param("elementType") DreamElementType elementType,
            Pageable pageable
    );

    @Query(
            value = """
                    select
                        de.element_type as elementType,
                        de.element_value as elementValue,
                        count(distinct da.dream_id) as appearanceCount
                    from dream_elements de
                    join dream_analyses da
                        on da.id = de.analysis_id
                    join dreams d
                        on d.id = da.dream_id
                    where d.user_id = :userId
                      and de.element_type in ('PERSON', 'PLACE', 'OBJECT')
                    group by
                        de.element_type,
                        de.element_value
                    order by rand()
                    """,
            nativeQuery = true
    )
    List<DreamWorldRandomElementProjection> findRandomElements(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query(
            value = """
                    select
                        d.id as dreamId,
                        d.dream_date as dreamDate,
                        d.title as title
                    from dream_elements de
                    join dream_analyses da
                        on da.id = de.analysis_id
                    join dreams d
                        on d.id = da.dream_id
                    where d.user_id = :userId
                      and de.element_type = :elementType
                      and de.element_value = :elementValue
                      and (
                            :excludeDreamId is null
                            or d.id <> :excludeDreamId
                      )
                    group by
                        d.id,
                        d.dream_date,
                        d.title
                    order by rand()
                    limit 1
                    """,
            nativeQuery = true
    )
    Optional<DreamWorldRandomDreamProjection> findRandomDreamByElement(
            @Param("userId") Long userId,
            @Param("elementType") String elementType,
            @Param("elementValue") String elementValue,
            @Param("excludeDreamId") Long excludeDreamId
    );

    @Query(
            value = """
                select
                    de.element_type as elementType,
                    de.element_value as elementValue,
                    count(distinct d.id) as appearanceCount

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

                order by
                    count(distinct d.id) desc
                """,
            nativeQuery = true
    )
    List<DreamStatisticsElementProjection> findTopStatisticsElements(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}