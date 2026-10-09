package com.starrainnotes.english.taxonomy.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
public final class TaxonomyDto {
    private TaxonomyDto() { }
    @Data public static class Node {
        @JsonSerialize(using=ToStringSerializer.class) private Long id;
        @JsonSerialize(using=ToStringSerializer.class) private Long parentId;
        private String dimension;
        private String name;
        private String nameEn;
        private String slug;
        private String description;
        private int sortOrder;
        private boolean enabled;
        private boolean worldBaseline;
        private List<Node> children = new ArrayList<>();
    }
}
