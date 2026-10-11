/*
 * OneTimeTokens：账号模块的工具文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 阅读入口：create（创建单次时间令牌集合）；hash（摘要处理入口）。
 */

package com.starrainnotes.account.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 单次时间令牌集合，属于账号模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class OneTimeTokens {

    /**
     * RANDOM。
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 构造OneTimeTokens，接收本类使用的依赖或初始化参数。
     */
    private OneTimeTokens() {
    }

    /**
     * 创建单次时间令牌集合。
     * 主要协作调用：<code>RANDOM.nextBytes</code>。
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String create() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 摘要处理入口。
     * 关键处理步骤调用 <code>MessageDigest.getInstance</code>、<code>token.getBytes</code>、<code>HexFormat.of</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param token 令牌；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
