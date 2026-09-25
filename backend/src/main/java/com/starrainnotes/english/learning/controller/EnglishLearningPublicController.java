package com.starrainnotes.english.learning.controller;

import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public Object get() {
        throw gone();
    }

    @GetMapping("/records/batch")
    public Object batch() {
        throw gone();
    }

    @PutMapping("/records/{contentType}/{contentId}")
    public Object save() {
        throw gone();
    }

    @GetMapping("/summary")
    public Object summary() {
        throw gone();
    }

    @GetMapping("/insights")
    public Object insights() {
        throw gone();
    }

    @GetMapping("/writing-submissions/{promptId}")
    public Object submission() {
        throw gone();
    }

    @PutMapping("/writing-submissions/{promptId}")
    public Object saveSubmission() {
        throw gone();
    }
}
