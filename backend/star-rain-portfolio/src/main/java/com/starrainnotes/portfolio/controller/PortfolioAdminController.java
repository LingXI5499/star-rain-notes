/*
 * PortfolioAdminController：作品模块的HTTP 接入文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、PageResult、PortfolioPermissions、IdOrderDTO、WorkBodyDTO、WorkCreateDTO、WorkLinkDTO。
 * 阅读入口：list（列出作品后台）；create（创建作品后台）；detail（读取详情作品后台）；preview（预览作品后台）。
 */

package com.starrainnotes.portfolio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.constant.PortfolioPermissions;
import com.starrainnotes.portfolio.dto.IdOrderDTO;
import com.starrainnotes.portfolio.dto.WorkBodyDTO;
import com.starrainnotes.portfolio.dto.WorkCreateDTO;
import com.starrainnotes.portfolio.dto.WorkLinkDTO;
import com.starrainnotes.portfolio.dto.WorkMediaDTO;
import com.starrainnotes.portfolio.dto.WorkPatchDTO;
import com.starrainnotes.portfolio.enumeration.WorkType;
import com.starrainnotes.portfolio.service.PortfolioWorkService;
import com.starrainnotes.portfolio.vo.WorkVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作品后台控制器，属于作品模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/portfolio")
public class PortfolioAdminController {
    /**
     * 作品列表。
     * 协作类型：<code>PortfolioWorkService</code>。
     */
    private final PortfolioWorkService works;

    /**
     * 列出作品后台。
     * HTTP 映射：<code>@GetMapping("/works")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>works.filteredWorks</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param type 类型；类型为 <code>WorkType</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @param q q；类型为 <code>String</code>
     * @param filter 筛选；类型为 <code>com.starrainnotes.portfolio.dto.WorkFilterDTO</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;WorkVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/works")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.READ_ADMIN + "')")
    public ApiResponse<PageResult<WorkVO>> list(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                 @RequestParam(required = false) WorkType type,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String q,
            @org.springframework.web.bind.annotation.ModelAttribute com.starrainnotes.portfolio.dto.WorkFilterDTO filter) {
        return ApiResponse.ok(works.filteredWorks(page, pageSize, type, status, q, filter, false));
    }

    /**
     * 创建作品后台。
     * HTTP 映射：<code>@PostMapping("/works")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>works.create</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>WorkCreateDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @PostMapping("/works")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> create(@Valid @RequestBody WorkCreateDTO request) {
        return ApiResponse.ok(works.create(request));
    }

    /**
     * 读取详情作品后台。
     * HTTP 映射：<code>@GetMapping("/works/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>works.adminWork</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @GetMapping("/works/{id}")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.READ_ADMIN + "')")
    public ApiResponse<WorkVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(works.adminWork(id));
    }

    /**
     * 预览作品后台。
     * HTTP 映射：<code>@GetMapping("/works/{id}/preview")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>works.adminWork</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @GetMapping("/works/{id}/preview")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.READ_ADMIN + "')")
    public ApiResponse<WorkVO> preview(@PathVariable Long id) {
        return ApiResponse.ok(works.adminWork(id));
    }

    /**
     * 更新作品后台。
     * HTTP 映射：<code>@PatchMapping("/works/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>works.update</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkPatchDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @PatchMapping("/works/{id}")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> update(@PathVariable Long id, @Valid @RequestBody WorkPatchDTO request) {
        return ApiResponse.ok(works.update(id, request));
    }

    /**
     * 正文处理入口。
     * HTTP 映射：<code>@PutMapping("/works/{id}/body")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>works.updateBody</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkBodyDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @PutMapping("/works/{id}/body")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> body(@PathVariable Long id, @Valid @RequestBody WorkBodyDTO request) {
        return ApiResponse.ok(works.updateBody(id, request.getBodyMarkdown()));
    }

    /**
     * 类型详情处理入口。
     * HTTP 映射：<code>@PutMapping("/works/{id}/detail")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>works.updateDetail</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>JsonNode</code>
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @PutMapping("/works/{id}/detail")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> typeDetail(@PathVariable Long id, @RequestBody JsonNode request) {
        return ApiResponse.ok(works.updateDetail(id, request));
    }

    /**
     * 添加媒体。
     * HTTP 映射：<code>@PostMapping("/works/{id}/media")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>works.addMedia</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkMediaDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @PostMapping("/works/{id}/media")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> addMedia(@PathVariable Long id, @Valid @RequestBody WorkMediaDTO request) {
        return ApiResponse.ok(works.addMedia(id, request));
    }

    /**
     * 更新媒体。
     * HTTP 映射：<code>@PatchMapping("/media/{mediaId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>works.updateMedia</code>。
     *
     * @param mediaId 媒体标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkMediaDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @PatchMapping("/media/{mediaId}")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> updateMedia(@PathVariable Long mediaId, @Valid @RequestBody WorkMediaDTO request) {
        return ApiResponse.ok(works.updateMedia(mediaId, request));
    }

    /**
     * 移除媒体。
     * HTTP 映射：<code>@DeleteMapping("/media/{mediaId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>works.removeMedia</code>。
     *
     * @param mediaId 媒体标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @DeleteMapping("/media/{mediaId}")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<Void> removeMedia(@PathVariable Long mediaId) {
        works.removeMedia(mediaId);
        return ApiResponse.ok(null);
    }

    /**
     * 顺序媒体处理入口。
     * HTTP 映射：<code>@PutMapping("/works/{id}/media/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>works.orderMedia</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>IdOrderDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @PutMapping("/works/{id}/media/order")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> orderMedia(@PathVariable Long id, @Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(works.orderMedia(id, request));
    }

    /**
     * 添加链接。
     * HTTP 映射：<code>@PostMapping("/works/{id}/links")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>works.addLink</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkLinkDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @PostMapping("/works/{id}/links")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> addLink(@PathVariable Long id, @Valid @RequestBody WorkLinkDTO request) {
        return ApiResponse.ok(works.addLink(id, request));
    }

    /**
     * 更新链接。
     * HTTP 映射：<code>@PatchMapping("/links/{linkId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>works.updateLink</code>。
     *
     * @param linkId 链接标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkLinkDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @PatchMapping("/links/{linkId}")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> updateLink(@PathVariable Long linkId, @Valid @RequestBody WorkLinkDTO request) {
        return ApiResponse.ok(works.updateLink(linkId, request));
    }

    /**
     * 移除链接。
     * HTTP 映射：<code>@DeleteMapping("/links/{linkId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>works.removeLink</code>。
     *
     * @param linkId 链接标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @DeleteMapping("/links/{linkId}")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<Void> removeLink(@PathVariable Long linkId) {
        works.removeLink(linkId);
        return ApiResponse.ok(null);
    }

    /**
     * 顺序Links处理入口。
     * HTTP 映射：<code>@PutMapping("/works/{id}/links/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>works.orderLinks</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>IdOrderDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @PutMapping("/works/{id}/links/order")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> orderLinks(@PathVariable Long id, @Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(works.orderLinks(id, request));
    }

    /**
     * 发布作品后台。
     * HTTP 映射：<code>@PostMapping("/works/{id}/publish")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.PUBLISH + "\')")</code>。
     * 主要协作调用：<code>works.publish</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @PostMapping("/works/{id}/publish")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.PUBLISH + "')")
    public ApiResponse<WorkVO> publish(@PathVariable Long id) {
        return ApiResponse.ok(works.publish(id));
    }

    /**
     * 撤回作品后台。
     * HTTP 映射：<code>@PostMapping("/works/{id}/withdraw")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.WITHDRAW + "\')")</code>。
     * 主要协作调用：<code>works.withdraw</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @PostMapping("/works/{id}/withdraw")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.WITHDRAW + "')")
    public ApiResponse<WorkVO> withdraw(@PathVariable Long id) {
        return ApiResponse.ok(works.withdraw(id));
    }

    /**
     * 恢复作品后台。
     * HTTP 映射：<code>@PostMapping("/works/{id}/restore")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.PUBLISH + "\')")</code>。
     * 主要协作调用：<code>works.restore</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @PostMapping("/works/{id}/restore")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.PUBLISH + "')")
    public ApiResponse<WorkVO> restore(@PathVariable Long id) {
        return ApiResponse.ok(works.restore(id));
    }

    /**
     * 删除作品后台。
     * HTTP 映射：<code>@DeleteMapping("/works/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + PortfolioPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>works.delete</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @DeleteMapping("/works/{id}")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        works.delete(id);
        return ApiResponse.ok(null);
    }
}
