package com.starrainnotes.profile.api.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileVO {
    private String displayName;
    private String headline;
    private String bioMarkdown;
    private String locationText;
    private String avatarUrl;
    private String resumeUrl;
    private String avatarMediaAssetId;
    private String resumeMediaAssetId;
    private List<Experience> experiences;
    private List<Skill> skills;
    private List<SocialLink> socialLinks;
    private List<Featured> featuredContents;

    @Data @AllArgsConstructor
    public static class Experience {
        private String id;
        private String experienceType;
        private String title;
        private String organization;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isCurrent;
        private String descriptionMd;
        private Integer sortOrder;
    }

    @Data @AllArgsConstructor
    public static class Skill {
        private String id;
        private String category;
        private String name;
        private String description;
        private String proficiency;
        private Integer sortOrder;
    }

    @Data @AllArgsConstructor
    public static class SocialLink {
        private String id;
        private String platformCode;
        private String label;
        private String url;
        private Integer sortOrder;
    }

    @Data @AllArgsConstructor
    public static class Featured {
        private String id;
        private String contentType;
        private String contentId;
        private String title;
        private String summary;
        private String url;
        private String coverUrl;
        private Boolean available;
        private Integer sortOrder;
    }
}
