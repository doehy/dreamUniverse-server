package com.dreamuniverse_server.dream.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "dream_elements",
        indexes = {
                @Index(
                        name = "idx_dream_elements_value",
                        columnList = "element_value"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DreamElement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private DreamAnalysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "element_type",
            nullable = false,
            length = 20,
            columnDefinition = "varchar(20)"
    )
    private DreamElementType elementType;

    @Column(
            name = "element_value",
            nullable = false,
            length = 100
    )
    private String elementValue;

    public static DreamElement create(
            DreamAnalysis analysis,
            DreamElementType elementType,
            String elementValue
    ) {
        DreamElement element = new DreamElement();
        element.analysis = analysis;
        element.elementType = elementType;
        element.elementValue = elementValue;

        return element;
    }
}