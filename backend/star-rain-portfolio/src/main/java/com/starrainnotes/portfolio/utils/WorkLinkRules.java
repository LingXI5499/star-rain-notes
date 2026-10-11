/*
 * WorkLinkRules：作品模块的工具文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：WorkInvalidException。
 * 阅读入口：requireSafeUrl（检查所需安全地址）。
 */

package com.starrainnotes.portfolio.utils;

import com.starrainnotes.portfolio.exception.WorkInvalidException;
import java.net.URI;

/**
 * 作品链接规则，属于作品模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class WorkLinkRules {
    /**
     * 构造WorkLinkRules，接收本类使用的依赖或初始化参数。
     */
    private WorkLinkRules() {
    }

    /**
     * 检查所需安全地址。
     * 关键处理步骤调用 <code>input.length</code>、<code>URI.create</code>、<code>input.trim</code>、<code>uri.getScheme</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(input == null || input.length() &gt; 1000)</code>；<code>(!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null || uri.getUserInfo() != null)</code>。
     *
     * @param input input；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String requireSafeUrl(String input) {
        if (input == null || input.length() > 1000) {
            throw new WorkInvalidException("外部链接长度无效");
        }
        try {
            URI uri = URI.create(input.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new WorkInvalidException("外部链接只接受 http/https 地址");
            }
            return uri.toString();
        } catch (IllegalArgumentException exception) {
            throw new WorkInvalidException("外部链接地址无效");
        }
    }
}
