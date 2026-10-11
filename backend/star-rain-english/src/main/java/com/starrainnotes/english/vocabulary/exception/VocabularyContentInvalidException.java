/*
 * VocabularyContentInvalidException：英语学习模块的业务异常文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.english.vocabulary.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 词汇内容（主题 / 单词）写入参数不合法，或路径上的编号无法解析。
 */
/**
 * 词汇内容无效异常，属于英语学习模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class VocabularyContentInvalidException extends ApiException {
    /**
     * 构造VocabularyContentInvalidException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param message 留言；类型为 <code>String</code>
     */
    public VocabularyContentInvalidException(String message) {
        super("ENGLISH_VOCABULARY_CONTENT_INVALID", message, 400);
    }
}
