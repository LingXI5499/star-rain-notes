/*
 * VocabularyPronunciationService：英语学习模块的业务契约文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 阅读入口：audio（音频处理入口）。
 */

package com.starrainnotes.english.vocabulary.service;

/*
 * 单词发音代理。
 *
 * 浏览器不直连第三方词典：由服务端校验单词、取音频、按内容落盘缓存后回传字节。
 * 未启用（或上游不可用）时抛业务异常，前端据此回退浏览器语音合成。
 */
/**
 * 词汇Pronunciation业务服务，属于英语学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface VocabularyPronunciationService {

    /*
     * 取该词该口音的 MP3 字节。word 必须是单个英文单词，accent 只接受 US / UK（缺省用配置默认值）。
     */
    /**
     * 音频处理入口。
     *
     * @param word 单词；类型为 <code>String</code>
     * @param accent accent；类型为 <code>String</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    byte[] audio(String word, String accent);
}
