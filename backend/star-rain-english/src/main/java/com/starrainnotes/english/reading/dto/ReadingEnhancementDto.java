package com.starrainnotes.english.reading.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Data;
public final class ReadingEnhancementDto {
    private ReadingEnhancementDto() { }
    @Data public static class Item {
        @JsonSerialize(using=ToStringSerializer.class) private Long id;
        private String groupKey; private String language; private Integer paragraphIndex;
        private Integer rangeStart; private Integer rangeEnd; private String expectedText;
        private String paragraphHash; private String sourceMarkdownHash; private String status;
        private String analysisMarkdown; private String word; private String partOfSpeech;
        private String meaningZh; private Integer sortOrder; private Long rowVersion;
    }
    @Data public static class Batch { private Long rowVersion; private List<Item> items; }
    public record Result(Long rowVersion,List<Item> items) { }
    public record Snapshot(ReadingDto.Article article,List<Item> alignments,List<Item> annotations,List<Item> vocabulary) { }
}
