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
@Data
@ConfigurationProperties(prefix = "star-rain.english.vocabulary.pronunciation")
public class VocabularyPronunciationProperties {

    /* 发音代理总开关；关闭时接口返回 404 */
    private boolean enabled = true;

    /* 发音提供方：youdao 或 disabled（其它值一律按 disabled 处理） */
    private String provider = "youdao";

    /* 有道词典发音接口地址 */
    private String dictVoiceBaseUrl = "https://dict.youdao.com/dictvoice";

    /* 美式发音的 type 参数 */
    private String usType = "0";

    /* 英式发音的 type 参数 */
    private String ukType = "1";

    /* 未显式指定口音时的默认口音 */
    private String defaultAccent = "US";

    /*
     * 音频缓存目录。相对路径按后端工作目录（star-rain-v2/backend）解析。
     * 刻意放在媒体根目录之外：媒体目录由媒体库按库内记录扫描，混入旁路缓存会干扰孤儿清理。
     */
    private String cacheDir = "../.local/pronunciation-cache";

    /* 建立连接的超时秒数 */
    private int connectTimeoutSeconds = 5;

    /* 单次上游请求的超时秒数 */
    private int requestTimeoutSeconds = 8;
}
