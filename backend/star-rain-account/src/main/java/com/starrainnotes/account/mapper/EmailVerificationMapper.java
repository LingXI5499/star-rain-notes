/*
 * EmailVerificationMapper：账号模块的数据访问文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：EmailVerificationEntity。
 * 阅读入口：ensureEmail（确保邮箱）；findForUpdate（查找面向更新）；saveCode（保存代码）；incrementFailures（incrementFailures处理入口）。
 */

package com.starrainnotes.account.mapper;

import com.starrainnotes.account.entity.EmailVerificationEntity;
import org.apache.ibatis.annotations.Param;

/**
 * 邮箱验证数据映射，属于账号模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface EmailVerificationMapper {
    /**
     * 确保邮箱。
     *
     * 持久化操作：新增，涉及 <code>sr_email_verification</code>。
     *
     * @param email 邮箱；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("email")</code>
     */
    void ensureEmail(@Param("email") String email);

    /**
     * 查找面向更新。
     *
     * 持久化操作：读取，涉及 <code>sr_email_verification</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param email 邮箱；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("email")</code>
     * @return 类型为 <code>EmailVerificationEntity</code> 的处理结果
     */
    EmailVerificationEntity findForUpdate(@Param("email") String email);

    /**
     * 保存代码。
     *
     * 持久化操作：更新，涉及 <code>sr_email_verification</code>。
     *
     * @param verification 验证；类型为 <code>EmailVerificationEntity</code>
     */
    void saveCode(EmailVerificationEntity verification);

    /**
     * incrementFailures处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_email_verification</code>。
     *
     * @param email 邮箱；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("email")</code>
     */
    void incrementFailures(@Param("email") String email);

    /**
     * 消费邮箱验证。
     *
     * 持久化操作：更新，涉及 <code>sr_email_verification</code>。
     *
     * @param email 邮箱；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("email")</code>
     * @param codeHash 代码摘要；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("codeHash")</code>
     * @return SQL 写入影响的记录行数
     */
    int consume(@Param("email") String email, @Param("codeHash") String codeHash);
}
