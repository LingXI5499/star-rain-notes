package com.starrainnotes.english.vocabulary.service;

/*
 * 单词发音代理。
 *
 * 浏览器不直连第三方词典：由服务端校验单词、取音频、按内容落盘缓存后回传字节。
 * 未启用（或上游不可用）时抛业务异常，前端据此回退浏览器语音合成。
 */
public interface VocabularyPronunciationService {

    /*
     * 取该词该口音的 MP3 字节。word 必须是单个英文单词，accent 只接受 US / UK（缺省用配置默认值）。
     */
    byte[] audio(String word, String accent);
}
