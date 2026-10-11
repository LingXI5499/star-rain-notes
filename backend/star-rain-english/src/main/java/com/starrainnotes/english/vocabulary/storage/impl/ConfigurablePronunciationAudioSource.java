/*
 * ConfigurablePronunciationAudioSource：英语学习模块的存储适配文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 实现文件或存储访问边界，向业务服务提供读写能力，并将底层失败转换为本模块约定的异常。
 * 主要协作类型：VocabularyPronunciationProperties、PronunciationAudioSource。
 * 阅读入口：audioUrl（音频地址处理入口）；enabled（已启用处理入口）；providerName（提供方名称处理入口）。
 */

package com.starrainnotes.english.vocabulary.storage.impl;

import com.starrainnotes.english.vocabulary.properties.VocabularyPronunciationProperties;
import com.starrainnotes.english.vocabulary.storage.PronunciationAudioSource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/*
 * 配置驱动的发音来源。
 *
 * 当前只实现有道 dictvoice：/dictvoice?type=<0 美式|1 英式>&audio=<word>。
 * 之所以不做成「有多个 @Component 实现 + @ConditionalOnProperty 二选一」，是因为
 * 条件装配在遇到未识别的 provider 值时会一个都不生效，导致注入失败而不是「自动降级为关闭」。
 * 这里把开关收敛在一个实现里：不认识的取值一律按 disabled 处理，行为可预期。
 */
/**
 * ConfigurablePronunciation音频来源，属于英语学习模块。
 * 实现文件或存储访问边界，向业务服务提供读写能力，并将底层失败转换为本模块约定的异常。
 */
@Component
@RequiredArgsConstructor
public class ConfigurablePronunciationAudioSource implements PronunciationAudioSource {

    /**
     * YOUDAO。
     */
    private static final String YOUDAO = "youdao";

    /**
     * 本模块的外部配置属性。
     * 协作类型：<code>VocabularyPronunciationProperties</code>。
     */
    private final VocabularyPronunciationProperties properties;

    /**
     * 音频地址处理入口。
     * 主要协作调用：<code>properties.getUkType</code>、<code>properties.getUsType</code>、<code>properties.getDictVoiceBaseUrl</code>。
     * 边界判断包含 <code>(!enabled())</code>；<code>(clean.isEmpty())</code>。
     *
     * @param word 单词；类型为 <code>String</code>
     * @param accent accent；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    public Optional<String> audioUrl(String word, String accent) {
        if (!enabled()) {
            return Optional.empty();
        }
        String clean = word == null ? "" : word.trim();
        if (clean.isEmpty()) {
            return Optional.empty();
        }
        boolean uk = "UK".equals(accent == null ? "" : accent.toUpperCase(Locale.ROOT));
        String type = uk ? properties.getUkType() : properties.getUsType();
        String base = properties.getDictVoiceBaseUrl();
        String encoded = URLEncoder.encode(clean, StandardCharsets.UTF_8);
        String separator = base.contains("?") ? "&" : "?";
        return Optional.of(base + separator + "type=" + type + "&audio=" + encoded);
    }

    /**
     * 已启用处理入口。
     * 主要协作调用：<code>properties.isEnabled</code>、<code>YOUDAO.equalsIgnoreCase</code>。
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean enabled() {
        return properties.isEnabled() && YOUDAO.equalsIgnoreCase(providerOrDefault());
    }

    /**
     * 提供方名称处理入口。
     * 关键处理步骤调用 <code>enabled</code>。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String providerName() {
        return enabled() ? YOUDAO : "disabled";
    }

    /**
     * 提供方或默认处理入口。
     * 主要协作调用：<code>properties.getProvider</code>。
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String providerOrDefault() {
        String provider = properties.getProvider();
        return provider == null || provider.isBlank() ? "disabled" : provider.trim();
    }
}
