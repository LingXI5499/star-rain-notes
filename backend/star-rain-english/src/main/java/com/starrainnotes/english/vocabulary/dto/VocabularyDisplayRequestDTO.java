package com.starrainnotes.english.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/*
 * 单卡显示模式写入请求。跟随全局用 DELETE /display 清除，不在这里传 FOLLOW_GLOBAL。
 */
@Data
public class VocabularyDisplayRequestDTO {

    @NotBlank
    @Pattern(regexp = "BILINGUAL|ENGLISH_ONLY|CHINESE_ONLY", message = "显示模式只能是双语、只看英文或只看中文")
    private String displayMode;
}
