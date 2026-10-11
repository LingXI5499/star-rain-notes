/*
 * PortfolioPermissions：作品模块的常量文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 集中维护本模块使用的固定代码或边界值，避免业务实现各自定义不同的字符串和数值。
 */

package com.starrainnotes.portfolio.constant;

/**
 * 作品权限集合，属于作品模块。
 * 集中维护本模块使用的固定代码或边界值，避免业务实现各自定义不同的字符串和数值。
 */
public final class PortfolioPermissions {
    /**
     * 后台。
     */
    public static final String READ_ADMIN = "portfolio:read-admin";
    /**
     * EDIT。
     */
    public static final String EDIT = "portfolio:edit";
    /**
     * 发布。
     */
    public static final String PUBLISH = "portfolio:publish";
    /**
     * 撤回。
     */
    public static final String WITHDRAW = "portfolio:withdraw";

    /**
     * 构造PortfolioPermissions，接收本类使用的依赖或初始化参数。
     */
    private PortfolioPermissions() {
    }
}
