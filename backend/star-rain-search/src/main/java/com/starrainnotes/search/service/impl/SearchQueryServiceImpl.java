/*
 * SearchQueryServiceImpl：搜索模块的业务实现文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：PageResult、EnglishSearchTypes、SearchPageInvalidException、SearchQueryRequiredException、SearchQueryTooLongException、SearchQueryTooShortException、SearchTypeInvalidException。
 * 阅读入口：search（搜索处理入口）；quick（quick处理入口）；suggestions（suggestions处理入口）。
 */

package com.starrainnotes.search.service.impl;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.english.api.EnglishSearchTypes;
import com.starrainnotes.search.exception.SearchPageInvalidException;
import com.starrainnotes.search.exception.SearchQueryRequiredException;
import com.starrainnotes.search.exception.SearchQueryTooLongException;
import com.starrainnotes.search.exception.SearchQueryTooShortException;
import com.starrainnotes.search.exception.SearchTypeInvalidException;
import com.starrainnotes.search.service.SearchIndexService;
import com.starrainnotes.search.service.SearchQueryService;
import com.starrainnotes.search.mapper.SearchDocumentMapper;
import com.starrainnotes.search.vo.SearchHitVO;
import com.starrainnotes.search.vo.SearchSuggestionVO;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 搜索查询参数业务服务实现，属于搜索模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class SearchQueryServiceImpl implements SearchQueryService {
    /**
     * 数据映射。
     * 协作类型：<code>SearchDocumentMapper</code>。
     */
    private final SearchDocumentMapper mapper;

    /**
     * 搜索处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.count</code>、<code>mapper.search</code>。
     * 实现中的显式失败类型包括 <code>SearchPageInvalidException</code>；具体触发条件见对应分支。
     *
     * @param rawQuery 原始查询参数；类型为 <code>String</code>
     * @param rawTypes 原始Types；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;SearchHitVO&gt;</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<SearchHitVO> search(String rawQuery, String rawTypes, int page, int pageSize) {
        String query = query(rawQuery);
        if (page < 1 || pageSize < 1 || pageSize > 100 || (long) (page - 1) * pageSize > Integer.MAX_VALUE) {
            throw new SearchPageInvalidException();
        }
        List<String> types = types(rawTypes);
        long total = mapper.count(query, types);
        List<SearchHitVO> items = total == 0 ? List.of()
            : mapper.search(query, types, (long) (page - 1) * pageSize, pageSize);
        return PageResult.<SearchHitVO>builder().items(items).total(total).page(page).pageSize(pageSize).build();
    }

    /**
     * quick处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.search</code>。
     * 实现中的显式失败类型包括 <code>SearchPageInvalidException</code>；具体触发条件见对应分支。
     *
     * @param rawQuery 原始查询参数；类型为 <code>String</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    @Transactional(readOnly = true)
    public List<SearchHitVO> quick(String rawQuery, int limit) {
        if (limit < 1 || limit > 10) throw new SearchPageInvalidException();
        return mapper.search(query(rawQuery), List.of(), 0, limit);
    }

    /**
     * suggestions处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.suggestions</code>。
     * 实现中的显式失败类型包括 <code>SearchPageInvalidException</code>；具体触发条件见对应分支。
     *
     * @param rawQuery 原始查询参数；类型为 <code>String</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    @Transactional(readOnly = true)
    public List<SearchSuggestionVO> suggestions(String rawQuery, int limit) {
        if (limit < 1 || limit > 10) throw new SearchPageInvalidException();
        return mapper.suggestions(query(rawQuery), limit);
    }

    /**
     * 查询参数处理入口。
     * 关键处理步骤调用 <code>value.isBlank</code>、<code>value.strip</code>、<code>query.codePointCount</code>、<code>query.length</code>。
     * 实现中的显式失败类型包括 <code>SearchQueryRequiredException</code>、<code>SearchQueryTooShortException</code>、<code>SearchQueryTooLongException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String query(String value) {
        if (value == null || value.isBlank()) throw new SearchQueryRequiredException();
        String query = value.strip();
        int length = query.codePointCount(0, query.length());
        if (length < 2) throw new SearchQueryTooShortException();
        if (length > 100) throw new SearchQueryTooLongException();
        return query;
    }

    /**
     * types处理入口。
     * 关键处理步骤调用 <code>raw.isBlank</code>、<code>List.of</code>、<code>Arrays.stream</code>、<code>raw.split</code>。
     * 实现中的显式失败类型包括 <code>SearchTypeInvalidException</code>；具体触发条件见对应分支。
     *
     * @param raw 原始；类型为 <code>String</code>
     * @return 本次处理得到的条目集合
     */
    private List<String> types(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> types = Arrays.stream(raw.split(",", -1)).map(String::strip).toList();
        if (types.stream().anyMatch(type -> !"ENGLISH".equals(type) && !SearchIndexService.TYPES.contains(type))) {
            throw new SearchTypeInvalidException();
        }
        return types.stream().flatMap(type -> "ENGLISH".equals(type)
            ? EnglishSearchTypes.ALL.stream() : java.util.stream.Stream.of(type)).distinct().toList();
    }

}
