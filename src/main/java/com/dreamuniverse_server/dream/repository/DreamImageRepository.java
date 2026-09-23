package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.DreamAiStatus;
import com.dreamuniverse_server.dream.entity.DreamImage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DreamImageRepository
        extends JpaRepository<DreamImage, Long> {

    List<DreamImage> findAllByDreamIdOrderByImageOrderAsc(
            Long dreamId
    );

    @Modifying
    @Query("""
            delete from DreamImage image
            where image.dream.id = :dreamId
            """)
    int deleteByDreamId(
            Long dreamId
    );

    @Query("""
            select di
            from DreamImage di
            join fetch di.dream d
            where d.user.id = :userId
              and d.aiStatus = :aiStatus
              and di.imageOrder = 1
            order by di.id desc
            """)
    List<DreamImage> findFirstGalleryImages(
            @Param("userId")
            Long userId,
            @Param("aiStatus")
            DreamAiStatus aiStatus,
            Pageable pageable
    );

    @Query("""
            select di
            from DreamImage di
            join fetch di.dream d
            where d.user.id = :userId
              and d.aiStatus = :aiStatus
              and di.imageOrder = 1
              and di.id < :cursor
            order by di.id desc
            """)
    List<DreamImage> findNextGalleryImages(
            @Param("userId")
            Long userId,
            @Param("aiStatus")
            DreamAiStatus aiStatus,
            @Param("cursor")
            Long cursor,
            Pageable pageable
    );
}