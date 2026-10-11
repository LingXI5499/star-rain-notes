/*
 * ProfileSeoSourceProvider：公开页面与 SEO模块的模块支持文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：SeoSourceProvider、SeoSourceDocument、ProfilePublicApi、ProfilePublishedDocument。
 * 阅读入口：supports（判断是否支持资料SEO来源提供方）；loadByRoute（加载依据路由）；listPublished（列出已发布）。
 */

package com.starrainnotes.seo.provider.impl;

import com.starrainnotes.seo.provider.SeoSourceProvider;

import com.starrainnotes.seo.dto.SeoSourceDocument;

import com.starrainnotes.profile.api.ProfilePublicApi;
import com.starrainnotes.profile.api.dto.ProfilePublishedDocument;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 资料SEO来源提供方，属于公开页面与 SEO模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class ProfileSeoSourceProvider implements SeoSourceProvider {
    /**
     * profiles。
     * 协作类型：<code>ProfilePublicApi</code>。
     */
    private final ProfilePublicApi profiles;

    /**
     * 判断是否支持资料SEO来源提供方。
     *
     * @param path 路径；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean supports(String path) { return "/about".equals(path); }

    /**
     * 加载依据路由。
     * 主要协作调用：<code>profiles.publishedDocument</code>。
     *
     * @param path 路径；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    public Optional<SeoSourceDocument> loadByRoute(String path) {
        if (!supports(path)) return Optional.empty();
        ProfilePublishedDocument profile = profiles.publishedDocument();
        if (profile == null) return Optional.empty();
        return Optional.of(SeoSourceDocument.builder().routePath("/about").contentType("PROFILE")
            .contentId(profile.getId()).title(profile.getTitle())
            .summary(profile.getSummary()).bodyMarkdown(profile.getBodyMarkdown()).build());
    }

    /**
     * 列出已发布。
     * 关键处理步骤调用 <code>loadByRoute</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<SeoSourceDocument> listPublished() { return loadByRoute("/about").stream().toList(); }
}
