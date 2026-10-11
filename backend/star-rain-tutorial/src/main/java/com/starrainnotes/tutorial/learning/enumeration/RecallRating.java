/*
 * RecallRating：教程与学习模块的枚举文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */

package com.starrainnotes.tutorial.learning.enumeration;

/**
 * Recall评分，属于教程与学习模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum RecallRating {
    /**
     * FORGOT。
     */
    FORGOT,
    /**
     * HARD。
     */
    HARD,
    /**
     * NORMAL。
     */
    NORMAL,
    /**
     * EASY。
     */
    EASY
}
