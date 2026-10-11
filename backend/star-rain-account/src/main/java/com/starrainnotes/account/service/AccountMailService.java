/*
 * AccountMailService：账号模块的业务契约文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 阅读入口：requireDelivery（检查所需Delivery）；invitationUrl（邀请地址处理入口）；sendReset（发送Reset）；sendEmailVerificationCode（发送邮箱验证代码）。
 */

package com.starrainnotes.account.service;

/**
 * 账号邮件业务服务，属于账号模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface AccountMailService {
    /**
     * 检查所需Delivery。
     */
    void requireDelivery();

    /**
     * 邀请地址处理入口。
     *
     * @param token 令牌；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    String invitationUrl(String token);

    /**
     * 发送Reset。
     *
     * @param email 邮箱；类型为 <code>String</code>
     * @param token 令牌；类型为 <code>String</code>
     */
    void sendReset(String email, String token);

    /**
     * 发送邮箱验证代码。
     *
     * @param email 邮箱；类型为 <code>String</code>
     * @param code 代码；类型为 <code>String</code>
     */
    void sendEmailVerificationCode(String email, String code);

    /**
     * 发送邀请。
     *
     * @param email 邮箱；类型为 <code>String</code>
     * @param token 令牌；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    String sendInvitation(String email, String token);
}
