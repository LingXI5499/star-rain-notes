package com.starrainnotes.english.knowledge.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Data;
@Data
public class DocumentMetadata {
    @JsonSerialize(using=ToStringSerializer.class) private Long primaryTopicId;
    @JsonSerialize(contentUsing=ToStringSerializer.class) private List<Long> otherTopicIds=List.of();
    @JsonSerialize(contentUsing=ToStringSerializer.class) private List<Long> genreIds=List.of();
    @JsonSerialize(contentUsing=ToStringSerializer.class) private List<Long> purposeIds=List.of();
    private List<String> keywords=List.of();
    private Long rowVersion;
}
