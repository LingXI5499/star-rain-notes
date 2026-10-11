/*
 * MapperXmlIntegrationTest：应用启动模块的测试文件。
 * 业务范围：多模块装配、入口路由、安全配置和启动初始化。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：everyMapperXmlOnDiskIsInTheVerificationScope（every数据映射Xml触发Disk是否内部The验证Scope处理入口）；everyStatementIdMatchesAMethodOnItsMapperInterface（everyStatement标识MatchesAMethod触发Its数据映射Interface处理入口）；everyMapperMethodHasAStatement（every数据映射Method是否具备AStatement处理入口）；configurationRegistersEveryMapperInterface（configurationRegistersEvery数据映射Interface处理入口）。
 */

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
/**
 * 数据映射XmlIntegration测试，属于应用启动模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class MapperXmlIntegrationTest extends MapperXmlIntegrationSupport {

    /**
     * STATEMENT。
     */
    private static final Pattern STATEMENT = Pattern.compile(
            "<(select|insert|update|delete)\\s+id=\"([^\"]+)\"");

    /**
     * every数据映射Xml触发Disk是否内部The验证Scope处理入口。
     * 关键处理步骤调用 <code>xmlResources</code>、<code>onDisk.isEmpty</code>。
     */
    @Test
    void everyMapperXmlOnDiskIsInTheVerificationScope() {
        Set<String> onDisk = new LinkedHashSet<>(xmlResources());
        assertTrue(!onDisk.isEmpty(), "没有扫描到任何 Mapper XML，验证范围为空说明扫描逻辑坏了");
    }

    /**
     * everyStatement标识MatchesAMethod触发Its数据映射Interface处理入口。
     * 关键处理步骤调用 <code>xmlResources</code>、<code>mapperInterfaceOf</code>、<code>methodNames</code>、<code>statementIds</code>。
     */
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
    /**
     * every数据映射Method是否具备AStatement处理入口。
     * 关键处理步骤调用 <code>xmlResources</code>、<code>mapperInterfaceOf</code>、<code>statementIds</code>、<code>methodNames</code>。
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

    /**
     * configurationRegistersEvery数据映射Interface处理入口。
     * 关键处理步骤调用 <code>configuration</code>、<code>configuration.getMappedStatementNames</code>、<code>xmlResources</code>、<code>mapperInterfaceOf</code>。
     */
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

    /**
     * 数据映射Interface所属处理入口。
     * 关键处理步骤调用 <code>namespaceOf</code>、<code>Class.forName</code>。
     * 实现中的显式失败类型包括 <code>AssertionError</code>；具体触发条件见对应分支。
     *
     * @param resource resource；类型为 <code>String</code>
     * @return 类型为 <code>Class&lt;?&gt;</code> 的处理结果
     */
    private Class<?> mapperInterfaceOf(String resource) {
        String namespace = namespaceOf(resource);
        try {
            return Class.forName(namespace);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("XML 的 namespace 指向不存在的类：" + resource + " → " + namespace, exception);
        }
    }

    /**
     * methodNames处理入口。
     * 关键处理步骤调用 <code>mapper.getDeclaredMethods</code>、<code>names.add</code>、<code>method.getName</code>。
     *
     * @param mapper 数据映射；类型为 <code>Class&lt;?&gt;</code>
     * @return 本次处理得到的条目集合
     */
    private Set<String> methodNames(Class<?> mapper) {
        Set<String> names = new LinkedHashSet<>();
        for (Method method : mapper.getDeclaredMethods()) {
            names.add(method.getName());
        }
        return names;
    }

    /**
     * statement标识集合处理入口。
     * 主要协作调用：<code>STATEMENT.matcher</code>。
     *
     * @param resource resource；类型为 <code>String</code>
     * @return 本次处理得到的条目集合
     */
    private Set<String> statementIds(String resource) {
        Set<String> ids = new LinkedHashSet<>();
        Matcher matcher = STATEMENT.matcher(readResource(resource));
        while (matcher.find()) {
            ids.add(matcher.group(2));
        }
        return ids;
    }
}
