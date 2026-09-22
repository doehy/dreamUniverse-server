package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.DreamImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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
}