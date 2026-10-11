/*
 * VocabularyPronunciationProperties：英语学习模块的外部配置文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 承接配置文件和环境变量映射。字段初始值是默认值，实际部署可以通过配置绑定覆盖。
 */

package com.starrainnotes.english.vocabulary.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/*
 * 单词发音代理配置（star-rain.english.vocabulary.pronunciation.*）。
 *
 * 默认开启 youdao：不写任何配置时，服务端就会代理有道 dictvoice 并在磁盘缓存音频。
 * 把 provider 设为 disabled（或 enabled=false）时接口返回 404，前端自动回退浏览器合成。
 * 这里不需要任何密钥，dictvoice 是公开接口。
 */
/**
 * 词汇Pronunciation配置属性，属于英语学习模块。
 * 承接配置文件和环境变量映射。字段初始值是默认值，实际部署可以通过配置绑定覆盖。
 */
@Data
@ConfigurationProperties(prefix = "star-rain.english.vocabulary.pronunciation")
public class VocabularyPronunciationProperties {

    /* 发音代理总开关；关闭时接口返回 404 */
    /**
     * 已启用。
     * 初始值为 <code>true</code>。
     */
    private boolean enabled = true;

    /* 发音提供方：youdao 或 disabled（其它值一律按 disabled 处理） */
    /**
     * 提供方。
     */
    private String provider = "youdao";

    /* 有道词典发音接口地址 */
    /**
     * dictVoice基础地址。
     */
    private String dictVoiceBaseUrl = "https://dict.youdao.com/dictvoice";

    /* 美式发音的 type 参数 */
    /**
     * us类型。
     */
    private String usType = "0";

    /* 英式发音的 type 参数 */
    /**
     * uk类型。
     */
    private String ukType = "1";

    /* 未显式指定口音时的默认口音 */
    /**
     * 默认Accent。
     */
    private String defaultAccent = "US";

    /*
     * 音频缓存目录。相对路径按后端工作目录（star-rain-v2/backend）解析。
     * 刻意放在媒体根目录之外：媒体目录由媒体库按库内记录扫描，混入旁路缓存会干扰孤儿清理。
     */
    /**
     * 缓存Dir。
     */
    private String cacheDir = "../.local/pronunciation-cache";

    /* 建立连接的超时秒数 */
    /**
     * connect超时秒。
     * 初始值为 <code>5</code>。
     */
    private int connectTimeoutSeconds = 5;

    /* 单次上游请求的超时秒数 */
    /**
     * 请求超时秒。
     * 初始值为 <code>8</code>。
     */
    private int requestTimeoutSeconds = 8;
}
