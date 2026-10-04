package com.starrainnotes.portfolio.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.enumeration.WorkType;
import com.starrainnotes.portfolio.service.PortfolioWorkService;
import com.starrainnotes.portfolio.vo.WorkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/portfolio/works")
public class PortfolioPublicController {
    private final PortfolioWorkService works;

    @GetMapping
    public ApiResponse<PageResult<WorkVO>> works(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(required = false) WorkType type) {
        return ApiResponse.ok(works.publicWorks(page, pageSize, type));
    }

    @GetMapping("/{slug}")
    public ApiResponse<WorkVO> work(@PathVariable String slug) {
        return ApiResponse.ok(works.publicWork(slug));
    }
}
