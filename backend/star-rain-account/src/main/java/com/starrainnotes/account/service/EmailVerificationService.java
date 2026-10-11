/*
 * EmailVerificationService：账号模块的业务契约文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：EmailVerificationCodeVO。
 * 阅读入口：send（发送邮箱验证）；sendForCurrentAccount（发送面向当前账号）；verify（验证邮箱验证）；consume（消费邮箱验证）。
 */

package com.starrainnotes.account.service;

import com.starrainnotes.account.vo.EmailVerificationCodeVO;

// 邮箱验证码的发送、校验与消费入口。
/**
 * 邮箱验证业务服务，属于账号模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface EmailVerificationService {

    /**
     * 发送邮箱验证。
     *
     * @param rawEmail 原始邮箱；类型为 <code>String</code>
     * @param ip IP 地址；类型为 <code>String</code>
     * @return 类型为 <code>EmailVerificationCodeVO</code> 的处理结果
     */
    EmailVerificationCodeVO send(String rawEmail, String ip);

    /**
     * 发送面向当前账号。
     *
     * @param ip IP 地址；类型为 <code>String</code>
     * @return 类型为 <code>EmailVerificationCodeVO</code> 的处理结果
     */
    EmailVerificationCodeVO sendForCurrentAccount(String ip);

    /**
     * 验证邮箱验证。
     *
     * @param email 邮箱；类型为 <code>String</code>
     * @param code 代码；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    String verify(String email, String code);

    /**
     * 消费邮箱验证。
     *
     * @param email 邮箱；类型为 <code>String</code>
     * @param codeHash 代码摘要；类型为 <code>String</code>
     */
    void consume(String email, String codeHash);
}
