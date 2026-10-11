/*
 * MessageService：留言模块的业务契约文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：PageResult、MessageSubmitDTO、AdminMessageVO、MessageActionVO、PublicMessageVO。
 * 阅读入口：submit（提交留言）；publicMessages（公开Messages处理入口）；adminMessages（后台Messages处理入口）；actions（actions处理入口）。
 */

package com.starrainnotes.message.service;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.message.dto.MessageSubmitDTO;
import com.starrainnotes.message.vo.AdminMessageVO;
import com.starrainnotes.message.vo.MessageActionVO;
import com.starrainnotes.message.vo.PublicMessageVO;
import java.util.List;

/**
 * 留言业务服务，属于留言模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface MessageService {
    /**
     * 提交留言。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>MessageSubmitDTO</code>
     * @param remoteAddress 远端地址；类型为 <code>String</code>
     * @return 类型为 <code>Long</code> 的处理结果
     */
    Long submit(MessageSubmitDTO request, String remoteAddress);
    /**
     * 公开Messages处理入口。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;PublicMessageVO&gt;</code> 的处理结果
     */
    PageResult<PublicMessageVO> publicMessages(int page, int pageSize);
    /**
     * 后台Messages处理入口。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @return 类型为 <code>PageResult&lt;AdminMessageVO&gt;</code> 的处理结果
     */
    PageResult<AdminMessageVO> adminMessages(int page, int pageSize, String status);
    /**
     * actions处理入口。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 本次处理得到的条目集合
     */
    List<MessageActionVO> actions(long id);
    /**
     * 批准留言。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>AdminMessageVO</code> 的处理结果
     */
    AdminMessageVO approve(long id);
    /**
     * 拒绝留言。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param reason 原因；类型为 <code>String</code>
     * @return 类型为 <code>AdminMessageVO</code> 的处理结果
     */
    AdminMessageVO reject(long id, String reason);
    /**
     * hide处理入口。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>AdminMessageVO</code> 的处理结果
     */
    AdminMessageVO hide(long id);
    /**
     * 恢复留言。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>AdminMessageVO</code> 的处理结果
     */
    AdminMessageVO restore(long id);
    /**
     * 删除留言。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     */
    void delete(long id);
}
