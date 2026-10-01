package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.DreamElement;
import com.dreamuniverse_server.dream.entity.DreamElementType;
import com.dreamuniverse_server.dream.repository.projection.DreamWorldElementProjection;
import com.dreamuniverse_server.dream.repository.projection.DreamWorldRandomElementProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}