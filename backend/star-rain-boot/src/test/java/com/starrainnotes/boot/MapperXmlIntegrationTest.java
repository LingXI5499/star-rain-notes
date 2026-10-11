package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

/*
 * Mapper XML 的静态绑定验证。**不需要数据库**，因此在任何机器与 CI 上都会执行。
 *
 * 它拦的是这一类真实故障：
 *   - 接口方法改了名 / 加了新方法，XML 没跟上 → 运行期 `Invalid bound statement (not found)`
 *   - XML 里语句 id 打错字，没人调用那条语句 → 直到某个分支被走到才炸
 *   - namespace 写错 → 整个 Mapper 的所有方法都找不到语句
 *   - resultType / parameterType 指向不存在的类 → 解析期就报错
 *
 * 从 `BaseMapper` 迁移到显式方法的整个过程最容易犯的就是第一条：
 * 删掉继承、加一个方法、忘了写 XML，编译器完全不会提醒。
 */
class MapperXmlIntegrationTest extends MapperXmlIntegrationSupport {

    private static final Pattern STATEMENT = Pattern.compile(
            "<(select|insert|update|delete)\\s+id=\"([^\"]+)\"");

    @Test
    void everyMapperXmlOnDiskIsInTheVerificationScope() {
        Set<String> onDisk = new LinkedHashSet<>(xmlResources());
        assertTrue(!onDisk.isEmpty(), "没有扫描到任何 Mapper XML，验证范围为空说明扫描逻辑坏了");
    }

    @Test
    void everyStatementIdMatchesAMethodOnItsMapperInterface() {
        List<String> problems = new ArrayList<>();
        for (String resource : xmlResources()) {
            Class<?> mapper = mapperInterfaceOf(resource);
            Set<String> methods = methodNames(mapper);
            for (String statementId : statementIds(resource)) {
                if (!methods.contains(statementId)) {
                    problems.add(resource + " 的语句 id `" + statementId + "` 在 " + mapper.getSimpleName()
                            + " 里没有同名方法");
                }
            }
        }
        assertTrue(problems.isEmpty(), "XML 语句与 Mapper 接口对不上：\n  " + String.join("\n  ", problems));
    }

    /*
     * 反向检查：接口上每个方法都必须有语句。
     * 这是 BaseMapper 迁移过程中最关键的护栏 —— 少写一条 XML 在编译期毫无痕迹，
     * 只会在第一次调用时抛 Invalid bound statement。
     */
    @Test
    void everyMapperMethodHasAStatement() {
        List<String> problems = new ArrayList<>();
        for (String resource : xmlResources()) {
            Class<?> mapper = mapperInterfaceOf(resource);
            Set<String> statements = statementIds(resource);
            for (String method : methodNames(mapper)) {
                if (!statements.contains(method)) {
                    problems.add(mapper.getSimpleName() + "." + method + "() 没有对应语句（" + resource + "）");
                }
            }
        }
        assertTrue(problems.isEmpty(), "Mapper 接口方法缺少 XML 语句：\n  " + String.join("\n  ", problems));
    }

    @Test
    void configurationRegistersEveryMapperInterface() {
        Configuration configuration = configuration();
        assertTrue(!configuration.getMappedStatementNames().isEmpty(), "没有解析出任何语句");
        for (String resource : xmlResources()) {
            Class<?> mapper = mapperInterfaceOf(resource);
            assertTrue(configuration.hasMapper(mapper),
                    "XML 的 namespace 未注册成 Mapper 接口：" + resource + " → " + mapper.getName());
        }
    }

    private Class<?> mapperInterfaceOf(String resource) {
        String namespace = namespaceOf(resource);
        try {
            return Class.forName(namespace);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("XML 的 namespace 指向不存在的类：" + resource + " → " + namespace, exception);
        }
    }

    private Set<String> methodNames(Class<?> mapper) {
        Set<String> names = new LinkedHashSet<>();
        for (Method method : mapper.getDeclaredMethods()) {
            names.add(method.getName());
        }
        return names;
    }

    private Set<String> statementIds(String resource) {
        Set<String> ids = new LinkedHashSet<>();
        Matcher matcher = STATEMENT.matcher(readResource(resource));
        while (matcher.find()) {
            ids.add(matcher.group(2));
        }
        return ids;
    }
}
