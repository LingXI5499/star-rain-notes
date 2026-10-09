package com.starrainnotes.blog.dto;

import java.util.List;
import lombok.Data;

@Data
public class BlogTopicNavigationOrderDTO {
    private List<Long> topicIds;
}
