/*
 * WorkType：作品模块的枚举文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */

package com.starrainnotes.portfolio.enumeration;

/**
 * 作品类型，属于作品模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum WorkType {
    /**
     * SOFTWARE。
     */
    SOFTWARE,
    /**
     * 视频。
     */
    VIDEO,
    /**
     * MUSIC。
     */
    MUSIC,
    /**
     * 写作。
     */
    WRITING,
    /**
     * OTHER。
     */
    OTHER
}
