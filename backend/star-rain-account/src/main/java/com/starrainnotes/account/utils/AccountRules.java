/*
 * AccountRules：账号模块的工具文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：ApiException。
 * 阅读入口：id（当前业务对象的数据库标识处理入口）；email（邮箱处理入口）；password（密码参数；使用和保存方式由对应的密码处理流程决定处理入口）。
 */

package com.starrainnotes.account.utils;

import com.starrainnotes.common.exception.ApiException;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

/**
 * 账号规则，属于账号模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class AccountRules {

    /**
     * 构造AccountRules，接收本类使用的依赖或初始化参数。
     */
    private AccountRules() {
    }

    /**
     * 当前业务对象的数据库标识处理入口。
     * 关键处理步骤调用 <code>Long.parseLong</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(id &gt; 0)</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    public static long id(String value) {
        try {
            long id = Long.parseLong(value);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // The same public validation error applies to malformed and nonpositive ids.
        }
        throw new ApiException("INVALID_ID", "无效的账户或权限编号", 400);
    }

    /**
     * 邮箱处理入口。
     * 关键处理步骤调用 <code>value.trim</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String email(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 密码参数；使用和保存方式由对应的密码处理流程决定处理入口。
     * 关键处理步骤调用 <code>password.equals</code>、<code>password.length</code>、<code>password.getBytes</code>、<code>password.isBlank</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param password 密码参数；使用和保存方式由对应的密码处理流程决定；类型为 <code>String</code>
     * @param confirmation 确认；类型为 <code>String</code>
     */
    public static void password(String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new ApiException("PASSWORD_CONFIRM_MISMATCH", "两次输入的密码不一致", 400);
        }
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72 || password.isBlank()) {
            throw new ApiException("PASSWORD_POLICY_VIOLATION", "密码至少 12 个字符，UTF-8 编码不超过 72 字节", 400);
        }
    }
}

