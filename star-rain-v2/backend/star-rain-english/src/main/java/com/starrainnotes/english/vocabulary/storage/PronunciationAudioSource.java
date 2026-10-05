package com.starrainnotes.english.vocabulary.storage;

import java.util.Optional;

/*
 * 单词发音音频来源（基础设施抽象，可替换的后端实现）。
 *
 * 只负责「这个词用哪个上游地址」，不负责下载与缓存——真正取字节的是发音服务，
 * 这样换提供方（有道 / 未来的其它词典）时不需要动缓存与校验逻辑。
 */
public interface PronunciationAudioSource {

    /*
     * 返回该词该口音的上游音频地址；来源被关闭或不支持时返回空，
     * 由调用方转成 404 让前端回退浏览器合成。
     */
    Optional<String> audioUrl(String word, String accent);

    /* 当前来源是否可用；false 时不必再做网络请求 */
    boolean enabled();

    /* 来源标识，用于日志与排障 */
    String providerName();
}
