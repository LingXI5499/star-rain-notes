package com.starrainnotes.english.knowledge.dto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.Data;
public final class RevisionDto {
    private RevisionDto() { }
    @Data public static class Revision {
        private long revisionNo;
        private String createdAt;
        private String changeNote;
        @JsonIgnore private String snapshotJson;
        private Object snapshot;
    }
    @Data public static class Request { private Long rowVersion; private String changeNote; }
    public record Page(List<Revision> items,long total,int page,int size) { }
}
