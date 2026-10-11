/*
 * PortfolioPublicController：作品模块的HTTP 接入文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、PageResult、WorkType、PortfolioWorkService、PortfolioPublicReadService、WorkVO。
 * 阅读入口：works（作品列表处理入口）；work（作品处理入口）。
 */

package com.starrainnotes.portfolio.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.enumeration.WorkType;
import com.starrainnotes.portfolio.service.PortfolioWorkService;
import com.starrainnotes.portfolio.service.PortfolioPublicReadService;
import com.starrainnotes.portfolio.vo.WorkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作品公开控制器，属于作品模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/portfolio/works")
public class PortfolioPublicController {
    /**
     * 作品列表。
     * 协作类型：<code>PortfolioWorkService</code>。
     */
    private final PortfolioWorkService works;
    /**
     * 阅读。
     * 协作类型：<code>PortfolioPublicReadService</code>。
     */
    private final PortfolioPublicReadService reading;

    /**
     * 作品列表处理入口。
     * HTTP 映射：<code>@GetMapping</code>。
     * 主要协作调用：<code>works.filteredWorks</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param type 类型；类型为 <code>WorkType</code>
     * @param filter 筛选；类型为 <code>com.starrainnotes.portfolio.dto.WorkFilterDTO</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;WorkVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping
    public ApiResponse<PageResult<WorkVO>> works(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(required = false) WorkType type,
            @org.springframework.web.bind.annotation.ModelAttribute com.starrainnotes.portfolio.dto.WorkFilterDTO filter) {
        return ApiResponse.ok(works.filteredWorks(page, pageSize, type, null, null, filter, true));
    }

    /**
     * 作品处理入口。
     * HTTP 映射：<code>@GetMapping("/{slug}")</code>。
     * 主要协作调用：<code>reading.read</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;WorkVO&gt;</code> 的处理结果
     */
    @GetMapping("/{slug}")
    public ApiResponse<WorkVO> work(@PathVariable String slug) {
        return ApiResponse.ok(reading.read(slug));
    }
}
