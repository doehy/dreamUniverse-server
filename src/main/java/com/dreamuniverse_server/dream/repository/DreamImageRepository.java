package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.DreamImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DreamImageRepository
        extends JpaRepository<DreamImage, Long> {

    List<DreamImage> findAllByDreamIdOrderByImageOrderAsc(
            Long dreamId
    );

    void deleteAllByDreamId(
            Long dreamId
    );
}