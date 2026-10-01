package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.DreamElement;
import com.dreamuniverse_server.dream.entity.DreamElementType;
import com.dreamuniverse_server.dream.repository.projection.DreamWorldElementProjection;
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
}