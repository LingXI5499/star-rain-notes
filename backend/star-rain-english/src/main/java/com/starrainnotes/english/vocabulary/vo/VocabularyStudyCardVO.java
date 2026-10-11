/*
 * VocabularyStudyCardVO：英语学习模块的响应数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 * 主要协作类型：VocabularyDto。
 */

package com.starrainnotes.english.vocabulary.vo;

import com.starrainnotes.english.vocabulary.dto.VocabularyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 学习队列中的一个卡片：词条 + 个人记忆 + 本次复习方向 + 是否新词。
 */
/**
 * 词汇学习卡片响应对象，属于英语学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyStudyCardVO {

    /**
     * 单词。
     */
    private VocabularyDto.Word word;

    /**
     * 记忆。
     */
    private VocabularyMemoryVO memory;

    /* EN_TO_ZH / ZH_TO_EN，MIXED 已在服务端解析成确定方向 */
    /**
     * direction。
     */
    private String direction;

    /**
     * new单词。
     */
    private boolean newWord;
}
