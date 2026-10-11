/*
 * TutorialSlugDeriverTest：教程与学习模块的测试文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：derivesReadableAsciiAndStableChineseFallback（derivesReadableAscii及StableChineseFallback处理入口）；normalizesFullWidthAndLimitsLength（normalizes完整宽度及LimitsLength处理入口）。
 */

package com.starrainnotes.tutorial.content.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 教程固定链接标识Deriver测试，属于教程与学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class TutorialSlugDeriverTest {
    /**
     * derivesReadableAscii及StableChineseFallback处理入口。
     * 关键处理步骤调用 <code>TutorialSlugDeriver.fromTitle</code>、<code>chinese.matches</code>。
     */
    @Test
    void derivesReadableAsciiAndStableChineseFallback() {
        assertEquals("java-learning-path", TutorialSlugDeriver.fromTitle("Java Learning Path", "tutorial"));
        assertEquals("java-21", TutorialSlugDeriver.fromTitle("Java 21 教程", "tutorial"));
        String chinese = TutorialSlugDeriver.fromTitle("并发编程基础", "tutorial");
        assertTrue(chinese.matches("tutorial-[0-9a-f]{16}"));
        assertEquals(chinese, TutorialSlugDeriver.fromTitle("并发编程基础", "tutorial"));
    }

    /**
     * normalizes完整宽度及LimitsLength处理入口。
     * 关键处理步骤调用 <code>TutorialSlugDeriver.fromTitle</code>、<code>longSlug.length</code>。
     */
    @Test
    void normalizesFullWidthAndLimitsLength() {
        assertEquals("java", TutorialSlugDeriver.fromTitle("Ｊａｖａ", "tutorial"));
        String longSlug = TutorialSlugDeriver.fromTitle("a".repeat(200), "tutorial");
        assertEquals(140, longSlug.length());
    }
}
