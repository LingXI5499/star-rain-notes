/*
 * PronunciationAudioSource：英语学习模块的存储适配文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 实现文件或存储访问边界，向业务服务提供读写能力，并将底层失败转换为本模块约定的异常。
 * 阅读入口：audioUrl（音频地址处理入口）；enabled（已启用处理入口）；providerName（提供方名称处理入口）。
 */

package com.starrainnotes.english.vocabulary.storage;

import java.util.Optional;

/*
 * 单词发音音频来源（基础设施抽象，可替换的后端实现）。
 *
 * 只负责「这个词用哪个上游地址」，不负责下载与缓存——真正取字节的是发音服务，
 * 这样换提供方（有道 / 未来的其它词典）时不需要动缓存与校验逻辑。
 */
/**
 * Pronunciation音频来源，属于英语学习模块。
 * 实现文件或存储访问边界，向业务服务提供读写能力，并将底层失败转换为本模块约定的异常。
 */
public interface PronunciationAudioSource {

    /*
     * 返回该词该口音的上游音频地址；来源被关闭或不支持时返回空，
     * 由调用方转成 404 让前端回退浏览器合成。
     */
    /**
     * 音频地址处理入口。
     *
     * @param word 单词；类型为 <code>String</code>
     * @param accent accent；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    Optional<String> audioUrl(String word, String accent);

    /* 当前来源是否可用；false 时不必再做网络请求 */
    /**
     * 已启用处理入口。
     * @return 本方法条件是否成立的判断结果
     */
    boolean enabled();

    /* 来源标识，用于日志与排障 */
    /**
     * 提供方名称处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    String providerName();
}
