/*
 * TutorialPermissions：教程与学习模块的常量文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 集中维护本模块使用的固定代码或边界值，避免业务实现各自定义不同的字符串和数值。
 */

package com.starrainnotes.tutorial.content.constant;

/**
 * 教程权限集合，属于教程与学习模块。
 * 集中维护本模块使用的固定代码或边界值，避免业务实现各自定义不同的字符串和数值。
 */
public final class TutorialPermissions {
    /**
     * 后台。
     */
    public static final String READ_ADMIN = "tutorial:read-admin";
    /**
     * EDIT。
     */
    public static final String EDIT = "tutorial:edit";
    /**
     * SUBMIT。
     */
    public static final String SUBMIT = "tutorial:submit";
    /**
     * 发布。
     */
    public static final String PUBLISH = "tutorial:publish";
    /**
     * 撤回。
     */
    public static final String WITHDRAW = "tutorial:withdraw";

    /**
     * 构造TutorialPermissions，接收本类使用的依赖或初始化参数。
     */
    private TutorialPermissions() {
    }
}
