/*
 * PortfolioWorkService：作品模块的业务契约文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：PageResult、IdOrderDTO、WorkCreateDTO、WorkLinkDTO、WorkMediaDTO、WorkPatchDTO、WorkType。
 * 阅读入口：publicWorks（公开作品列表处理入口）；filteredWorks（filtered作品列表处理入口）；publicWork（公开作品处理入口）；publicWorkById（按标识读取公开作品）。
 */

package com.starrainnotes.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.dto.IdOrderDTO;
import com.starrainnotes.portfolio.dto.WorkCreateDTO;
import com.starrainnotes.portfolio.dto.WorkLinkDTO;
import com.starrainnotes.portfolio.dto.WorkMediaDTO;
import com.starrainnotes.portfolio.dto.WorkPatchDTO;
import com.starrainnotes.portfolio.enumeration.WorkType;
import com.starrainnotes.portfolio.vo.WorkVO;

/**
 * 作品作品业务服务，属于作品模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface PortfolioWorkService {
    /**
     * 公开作品列表处理入口。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param type 类型；类型为 <code>WorkType</code>
     * @return 类型为 <code>PageResult&lt;WorkVO&gt;</code> 的处理结果
     */
    PageResult<WorkVO> publicWorks(int page, int pageSize, WorkType type);
    /**
     * filtered作品列表处理入口。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param type 类型；类型为 <code>WorkType</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @param keyword 关键词；类型为 <code>String</code>
     * @param filter 筛选；类型为 <code>com.starrainnotes.portfolio.dto.WorkFilterDTO</code>
     * @param publicOnly 公开Only；类型为 <code>boolean</code>
     * @return 类型为 <code>PageResult&lt;WorkVO&gt;</code> 的处理结果
     */
    PageResult<WorkVO> filteredWorks(int page, int pageSize, WorkType type, String status, String keyword,
        com.starrainnotes.portfolio.dto.WorkFilterDTO filter, boolean publicOnly);
    /**
     * 公开作品处理入口。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO publicWork(String slug);
    /**
     * 按标识读取公开作品。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO publicWorkById(Long id);
    /**
     * 后台作品列表处理入口。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param type 类型；类型为 <code>WorkType</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @param keyword 关键词；类型为 <code>String</code>
     * @return 类型为 <code>PageResult&lt;WorkVO&gt;</code> 的处理结果
     */
    PageResult<WorkVO> adminWorks(int page, int pageSize, WorkType type, String status, String keyword);
    /**
     * 后台作品处理入口。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO adminWork(Long id);
    /**
     * 创建作品作品。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>WorkCreateDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO create(WorkCreateDTO request);
    /**
     * 更新作品作品。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkPatchDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO update(Long id, WorkPatchDTO request);
    /**
     * 更新正文。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param markdown Markdown；类型为 <code>String</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO updateBody(Long id, String markdown);
    /**
     * 更新详情。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param detail 详情；类型为 <code>JsonNode</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO updateDetail(Long id, JsonNode detail);
    /**
     * 添加媒体。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkMediaDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO addMedia(Long id, WorkMediaDTO request);
    /**
     * 更新媒体。
     *
     * @param mediaId 媒体标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkMediaDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO updateMedia(Long mediaId, WorkMediaDTO request);
    /**
     * 移除媒体。
     *
     * @param mediaId 媒体标识；类型为 <code>Long</code>
     */
    void removeMedia(Long mediaId);
    /**
     * 顺序媒体处理入口。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>IdOrderDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO orderMedia(Long id, IdOrderDTO request);
    /**
     * 添加链接。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkLinkDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO addLink(Long id, WorkLinkDTO request);
    /**
     * 更新链接。
     *
     * @param linkId 链接标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkLinkDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO updateLink(Long linkId, WorkLinkDTO request);
    /**
     * 移除链接。
     *
     * @param linkId 链接标识；类型为 <code>Long</code>
     */
    void removeLink(Long linkId);
    /**
     * 顺序Links处理入口。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>IdOrderDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO orderLinks(Long id, IdOrderDTO request);
    /**
     * 发布作品作品。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO publish(Long id);
    /**
     * 撤回作品作品。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO withdraw(Long id);
    /**
     * 恢复作品作品。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    WorkVO restore(Long id);
    /**
     * 删除作品作品。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    void delete(Long id);
}
