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
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/english/vocabulary")
public class VocabularyPublicController {

    private static final int DEFAULT_PAGE_SIZE = 50;

    private final VocabularyService service;
    private final VocabularyPronunciationService pronunciationService;

    /* 空串与全空白都算「没传」，避免 ?search=&q=physical 时把 q 丢掉 */
    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /* size 优先于 pageSize；都没传时用默认值 */
    private static int firstNonNull(int fallback, Integer... values) {
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }
        return fallback;
    }

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
