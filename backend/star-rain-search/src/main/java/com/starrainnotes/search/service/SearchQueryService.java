/*
 * SearchQueryService：搜索模块的业务契约文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：PageResult、SearchHitVO、SearchSuggestionVO。
 * 阅读入口：search（搜索处理入口）；quick（quick处理入口）；suggestions（suggestions处理入口）。
 */

package com.starrainnotes.search.service;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.search.vo.SearchHitVO;
import com.starrainnotes.search.vo.SearchSuggestionVO;
import java.util.List;

// 搜索查询业务入口：全文检索、快捷检索与搜索建议。
/**
 * 搜索查询参数业务服务，属于搜索模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface SearchQueryService {

    /**
     * 搜索处理入口。
     *
     * @param rawQuery 原始查询参数；类型为 <code>String</code>
     * @param rawTypes 原始Types；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;SearchHitVO&gt;</code> 的处理结果
     */
    PageResult<SearchHitVO> search(String rawQuery, String rawTypes, int page, int pageSize);

    /**
     * quick处理入口。
     *
     * @param rawQuery 原始查询参数；类型为 <code>String</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    List<SearchHitVO> quick(String rawQuery, int limit);

    /**
     * suggestions处理入口。
     *
     * @param rawQuery 原始查询参数；类型为 <code>String</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    List<SearchSuggestionVO> suggestions(String rawQuery, int limit);
}
