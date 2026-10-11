/*
 * VocabularyPublicController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、VocabularyStudyConstants、VocabularyDto、VocabularyContentInvalidException、VocabularyPronunciationService、VocabularyService。
 * 阅读入口：themes（themes处理入口）；words（words处理入口）；wordsByIds（按标识集合读取words）；word（单词处理入口）。
 */

package com.starrainnotes.english.vocabulary.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.vocabulary.constant.VocabularyStudyConstants;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto;
import com.starrainnotes.english.vocabulary.exception.VocabularyContentInvalidException;
import com.starrainnotes.english.vocabulary.service.VocabularyPronunciationService;
import com.starrainnotes.english.vocabulary.service.VocabularyService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * 词汇公开读取：主题、单词、批量取词与发音代理。
 *
 * 这里只有「内容」和「发音」，没有任何个人数据；记忆进度一律走 /api/account/english/vocabulary。
 */
/**
 * 词汇公开控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/english/vocabulary")
public class VocabularyPublicController {

    /**
     * 大小。
     * 初始值为 <code>50</code>。
     */
    private static final int DEFAULT_PAGE_SIZE = 50;

    /**
     * 业务服务。
     * 协作类型：<code>VocabularyService</code>。
     */
    private final VocabularyService service;
    /**
     * pronunciation业务服务。
     * 协作类型：<code>VocabularyPronunciationService</code>。
     */
    private final VocabularyPronunciationService pronunciationService;

    /* 空串与全空白都算「没传」，避免 ?search=&q=physical 时把 q 丢掉 */
    /**
     * 首次NonBlank处理入口。
     * 关键处理步骤调用 <code>value.isBlank</code>。
     *
     * @param values 值集合；类型为 <code>String[]</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /* size 优先于 pageSize；都没传时用默认值 */
    /**
     * 首次Non空值处理入口。
     *
     * @param fallback fallback；类型为 <code>int</code>
     * @param values 值集合；类型为 <code>Integer[]</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    private static int firstNonNull(int fallback, Integer... values) {
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }
        return fallback;
    }

    /**
     * themes处理入口。
     * HTTP 映射：<code>@GetMapping("/themes")</code>。
     * 主要协作调用：<code>service.themes</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;VocabularyDto.Theme&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/themes")
    public ApiResponse<List<VocabularyDto.Theme>> themes() {
        return ApiResponse.ok(service.themes());
    }

    /*
     * 分页取词。
     *
     * 分页参数同时接受 size / pageSize 两种写法，搜索词同时接受 search / q：
     * V1 的公开接口用的是 pageSize 与 q，V2 早先只认 size 与 search。
     * 只认一种会静默回落到默认值（50 条），调用方会以为自己传的分页没生效——
     * 这正是之前踩到的坑，因此两种写法都收，并在验收文档里写明优先级。
     *
     * 注意：这里没有 planned 过滤。公开接口是匿名内容接口，服务端拿不到浏览器的游客计划，
     * 强行支持只会返回「空」这种与事实相反的结果。「已加入计划」筛选在主题页按
     * 账号记忆集合（或游客 IndexedDB 集合）在客户端完成。
     */
    /**
     * words处理入口。
     * HTTP 映射：<code>@GetMapping("/words")</code>。
     * 主要协作调用：<code>service.words</code>。
     *
     * @param themeId 主题标识；类型为 <code>String</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param q q；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>Integer</code>
     * @param pageSize 单页返回条目数；类型为 <code>Integer</code>
     * @return 类型为 <code>ApiResponse&lt;VocabularyDto.Page&lt;VocabularyDto.Word&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/words")
    public ApiResponse<VocabularyDto.Page<VocabularyDto.Word>> words(
            @RequestParam(required = false) String themeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(service.words(themeId, firstNonBlank(search, q), page,
                firstNonNull(DEFAULT_PAGE_SIZE, size, pageSize)));
    }

    /*
     * 按编号批量取词。必须声明在 /words/{wordId} 之前读起来才不迷惑；
     * Spring 本身会优先匹配字面量段，所以两条并存不会互相吃掉。
     */
    /**
     * 按标识集合读取words。
     * HTTP 映射：<code>@GetMapping("/words/batch")</code>。
     * 主要协作调用：<code>service.wordsByIds</code>。
     * 实现中的显式失败类型包括 <code>VocabularyContentInvalidException</code>；具体触发条件见对应分支。
     *
     * @param ids 标识集合；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;VocabularyDto.Word&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/words/batch")
    public ApiResponse<List<VocabularyDto.Word>> wordsByIds(@RequestParam String ids) {
        List<Long> parsed = new ArrayList<>();
        for (String token : Arrays.asList(ids.split(","))) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                parsed.add(Long.valueOf(trimmed));
            } catch (NumberFormatException exception) {
                throw new VocabularyContentInvalidException("ids 只能是逗号分隔的数字编号");
            }
            if (parsed.size() >= VocabularyStudyConstants.MAX_BATCH_WORDS) {
                break;
            }
        }
        return ApiResponse.ok(service.wordsByIds(parsed, VocabularyStudyConstants.MAX_BATCH_WORDS));
    }

    /**
     * 单词处理入口。
     * HTTP 映射：<code>@GetMapping("/words/{wordId}")</code>。
     * 主要协作调用：<code>service.word</code>。
     *
     * @param wordId 单词标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;VocabularyDto.Word&gt;</code> 的处理结果
     */
    @GetMapping("/words/{wordId}")
    public ApiResponse<VocabularyDto.Word> word(@PathVariable String wordId) {
        return ApiResponse.ok(service.word(wordId));
    }

    /*
     * 单词发音代理。
     *
     * 浏览器不直连第三方词典：服务端校验单词、取音频、按 sha256(word|accent) 落盘缓存后回传。
     * 返回音频字节因此不套 ApiResponse；失败仍由全局异常处理器输出统一 JSON，
     * 前端收到错误即回退浏览器语音合成。音频内容对同一个词不变，可以长缓存。
     */
    /**
     * pronunciation处理入口。
     * HTTP 映射：<code>@GetMapping("/pronunciation")</code>。
     * 主要协作调用：<code>pronunciationService.audio</code>。
     *
     * @param word 单词；类型为 <code>String</code>
     * @param accent accent；类型为 <code>String</code>
     * @return 类型为 <code>ResponseEntity&lt;byte[]&gt;</code> 的处理结果
     */
    @GetMapping("/pronunciation")
    public ResponseEntity<byte[]> pronunciation(@RequestParam String word,
                                               @RequestParam(required = false) String accent) {
        byte[] bytes = pronunciationService.audio(word, accent);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CACHE_CONTROL,
                        CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic().immutable().getHeaderValue())
                .body(bytes);
    }
}
