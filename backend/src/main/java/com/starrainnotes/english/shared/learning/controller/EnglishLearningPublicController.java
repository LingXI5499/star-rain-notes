package com.starrainnotes.english.shared.learning.controller;

import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public learning endpoints (X-Learner-Key).
 *
 * 阶段四：游客进度只写浏览器 localStorage，登录管理员走 /account/english。
 * 因此这里按"游客进度改为本地存储"拦截为 410 GONE，防止匿名访客再向服务器
 * 写入英语学习数据。前端已迁移到本地/账号分流，不再调用本组端点。
 */
@RestController
@RequestMapping("/api/v1/public/english/learning")
public class EnglishLearningPublicController {

    /** Guest progress is now stored locally; these server endpoints are gone. */
    private ApiException gone() {
        return new ApiException(HttpStatus.GONE, "GUEST_PROGRESS_LOCAL_ONLY",
                "Guest progress is stored locally",
                "English learning progress for anonymous visitors is now kept in the browser and is no longer "
                        + "persisted on the server. Sign in to store it in your account.");
    }

    @GetMapping("/records/{contentType}/{contentId}")
    public Object get(@RequestHeader(value = "X-Learner-Key", required = false) String key,
                      @PathVariable String contentType,
                      @PathVariable Long contentId) {
        throw gone();
    }

    @GetMapping("/records/batch")
    public Object batch(@RequestHeader(value = "X-Learner-Key", required = false) String key,
                        @RequestParam(value = "ref", required = false) java.util.List<String> refs) {
        throw gone();
    }

    @PutMapping("/records/{contentType}/{contentId}")
    public Object save(@RequestHeader(value = "X-Learner-Key", required = false) String key,
                       @PathVariable String contentType,
                       @PathVariable Long contentId,
                       @RequestBody(required = false) Object request) {
        throw gone();
    }

    @GetMapping("/summary")
    public Object summary(@RequestHeader(value = "X-Learner-Key", required = false) String key) {
        throw gone();
    }

    @GetMapping("/insights")
    public Object insights(@RequestHeader(value = "X-Learner-Key", required = false) String key) {
        throw gone();
    }

    @GetMapping("/writing-submissions/{promptId}")
    public Object submission(@RequestHeader(value = "X-Learner-Key", required = false) String key,
                             @PathVariable Long promptId) {
        throw gone();
    }

    @PutMapping("/writing-submissions/{promptId}")
    public Object saveSubmission(@RequestHeader(value = "X-Learner-Key", required = false) String key,
                                 @PathVariable Long promptId,
                                 @RequestBody(required = false) Object request) {
        throw gone();
    }
}
