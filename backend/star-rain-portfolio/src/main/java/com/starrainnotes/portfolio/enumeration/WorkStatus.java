/*
 * WorkStatus：作品模块的枚举文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */

package com.starrainnotes.portfolio.enumeration;

/**
 * 作品状态，属于作品模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum WorkStatus {
    /**
     * 草稿。
     */
    DRAFT,
    /**
     * 已发布。
     */
    PUBLISHED,
    /**
     * 已撤回。
     */
    WITHDRAWN
}
