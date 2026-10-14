package com.starrainnotes.boot.reliability;

import com.starrainnotes.common.web.ContentVersion;
import java.util.Set;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.stereotype.Component;

/** Track only successful aggregate-version SQL increments, never infer a version from a later GET. */
@Component
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class ContentVersionInterceptor implements Interceptor {
    private static final Set<String> BLOG = Set.of("updatePostMeta", "updatePostBody", "publishPost", "withdrawPost");
    private static final Set<String> WORK = Set.of("updateContent", "updateBody", "touch", "publish", "withdraw", "restore", "updatePrototype");
    @Override public Object intercept(Invocation invocation) throws Throwable {
        Object result = invocation.proceed();
        String statement = ((MappedStatement) invocation.getArgs()[0]).getId();
        String method = statement.substring(statement.lastIndexOf('.') + 1);
        if (result instanceof Integer rows && rows > 0
                && (statement.startsWith("com.starrainnotes.blog.mapper.BlogPostMapper.") && BLOG.contains(method)
                || statement.startsWith("com.starrainnotes.portfolio.mapper.WorkMapper.") && WORK.contains(method))) ContentVersion.advanced();
        return result;
    }
}
