package cn.kong.kb.domain;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 知识库分类实体。
 */
@Data
public class KnowledgeBase {

    private UUID id;
    private String name;
    private String description;
    private String color;
    private int documentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public KnowledgeBase() {}

    public KnowledgeBase(String name, String description, String color) {
        this.name = name;
        this.description = description;
        this.color = color;
    }
}
