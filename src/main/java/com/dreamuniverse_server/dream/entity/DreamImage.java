package com.dreamuniverse_server.dream.entity;

import com.dreamuniverse_server.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "dream_images",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dream_images_dream_order",
                        columnNames = {
                                "dream_id",
                                "image_order"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DreamImage extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "dream_id",
            nullable = false
    )
    private Dream dream;

    @Column(
            name = "image_key",
            nullable = false,
            length = 255
    )
    private String imageKey;

    @Column(
            name = "image_order",
            nullable = false
    )
    private int imageOrder;

    private DreamImage(
            Dream dream,
            String imageKey,
            int imageOrder
    ) {
        this.dream = dream;
        this.imageKey = imageKey;
        this.imageOrder = imageOrder;
    }

    public static DreamImage create(
            Dream dream,
            String imageKey,
            int imageOrder
    ) {
        return new DreamImage(
                dream,
                imageKey,
                imageOrder
        );
    }
}