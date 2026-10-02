package com.starrainnotes.common.enumeration;

/*
 * 站点入口。
 *
 * 三个入口按域名隔离：公开站匿名只读，用户站给 USER 与 ADMIN 共用，管理站只给 SUPER_ADMIN。
 * 认证 API 只有一套（/api/auth/**），三个入口共用同一份实现，
 * 差别只在「这个入口暴露哪些入口能力」，由 AccountSecurityConfig 的规则决定。
 *
 * 枚举的声明顺序就是权限从小到大的顺序：HostEntryResolver 在配置冲突时取序号靠前的一个，
 * 即落到权限最小的一侧，所以新增入口必须按权限顺位插入，不能随手追加在末尾。
 */
public enum SiteEntry {

    // 公开站：匿名只读，不暴露账号相关能力
    PUBLIC("public"),

    // 用户站：USER 与 ADMIN 共用，含个人中心与内容协作（媒体库、审核中心）
    USER("user"),

    // 管理站：SUPER_ADMIN 的账户治理与博客后台
    ADMIN("admin");

    // 与前端 shared/entry.js 的字符串值一一对应，改动必须两边同步
    private final String code;

    SiteEntry(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
