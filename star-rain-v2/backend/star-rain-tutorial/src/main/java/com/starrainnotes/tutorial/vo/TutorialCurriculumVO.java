package com.starrainnotes.tutorial.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialCurriculumVO {
    private TutorialAdminVO tutorial;
    private List<TutorialGroupVO> groups;
}
