/*
 * ModuleBoundaryTest：应用启动模块的测试文件。
 * 业务范围：多模块装配、入口路由、安全配置和启动初始化。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：businessModulesOnlyImportOtherModulesPublicContracts（businessModulesOnlyImportOtherModules公开Contracts处理入口）；controllersDoNotDependOnPersistenceTypes（controllersDo不Depend触发持久化Types处理入口）；reviewedServicePackagesKeepInterfacesAndImplementationsSeparate（复习业务服务PackagesKeepInterfaces及ImplementationsSeparate处理入口）；backendRoot（backendRoot处理入口）。
 */

package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Module边界测试，属于应用启动模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class ModuleBoundaryTest {
    /**
     * IMPORT。
     */
    private static final Pattern IMPORT = Pattern.compile("^import com\\.starrainnotes\\.([a-z]+)\\.(.+);$");
    /**
     * MODULES。
     */
    private static final Set<String> MODULES = Set.of("account", "analytics", "blog", "english", "media",
        "message", "portfolio", "profile", "review", "search", "seo", "site", "tutorial");

    /**
     * businessModulesOnlyImportOtherModules公开Contracts处理入口。
     * 主要协作调用：<code>IMPORT.matcher</code>、<code>MODULES.contains</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    void businessModulesOnlyImportOtherModulesPublicContracts() throws IOException {
        Path backend = backendRoot();
        List<String> violations = new ArrayList<>();
        for (String source : MODULES) {
            Path javaRoot = backend.resolve("star-rain-" + source).resolve("src/main/java");
            try (var files = Files.walk(javaRoot)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    for (String line : Files.readAllLines(file)) {
                        Matcher match = IMPORT.matcher(line);
                        if (!match.matches()) continue;
                        String target = match.group(1);
                        String name = match.group(2);
                        if (!MODULES.contains(target) || source.equals(target)) continue;
                        if ((name.startsWith("api.") || name.contains(".api."))
                            && !name.startsWith("api.impl.") && !name.contains(".api.impl.")) continue;
                        violations.add(backend.relativize(file) + " imports " + line);
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }

    /**
     * controllersDo不Depend触发持久化Types处理入口。
     * 关键处理步骤调用 <code>backendRoot</code>、<code>backend.resolve</code>、<code>Files.walk</code>、<code>files.filter</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    void controllersDoNotDependOnPersistenceTypes() throws IOException {
        Path backend = backendRoot();
        List<String> violations = new ArrayList<>();
        for (String source : MODULES) {
            Path javaRoot = backend.resolve("star-rain-" + source).resolve("src/main/java");
            try (var files = Files.walk(javaRoot)) {
                for (Path file : files.filter(path -> path.toString().endsWith("Controller.java")).toList()) {
                    for (String line : Files.readAllLines(file)) {
                        if (line.matches("^import com\\.starrainnotes\\.[^.]+\\.(mapper|entity)\\..+;$")) {
                            violations.add(backend.relativize(file) + " imports " + line);
                        }
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }

    /**
     * 复习业务服务PackagesKeepInterfaces及ImplementationsSeparate处理入口。
     * 关键处理步骤调用 <code>backendRoot</code>、<code>List.of</code>、<code>backend.resolve</code>、<code>Files.list</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    void reviewedServicePackagesKeepInterfacesAndImplementationsSeparate() throws IOException {
        Path backend = backendRoot();
        List<String> violations = new ArrayList<>();
        for (String module : List.of("account", "analytics", "media", "seo")) {
            Path service = backend.resolve("star-rain-" + module)
                .resolve("src/main/java/com/starrainnotes/" + module + "/service");
            try (var files = Files.list(service)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    if (!Files.readString(file).contains("public interface ")) {
                        violations.add(backend.relativize(file) + " is not a service interface");
                    }
                }
            }
            Path implementations = service.resolve("impl");
            try (var files = Files.list(implementations)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    if (!file.getFileName().toString().endsWith("Impl.java")) {
                        violations.add(backend.relativize(file) + " lacks Impl suffix");
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }

    /**
     * backendRoot处理入口。
     * 关键处理步骤调用 <code>Path.of</code>、<code>System.getProperty</code>、<code>Files.isDirectory</code>、<code>current.resolve</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     * @return 类型为 <code>Path</code> 的处理结果
     */
    private Path backendRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("star-rain-blog/src/main/java"))) return current;
            if (Files.isDirectory(current.resolve("backend/star-rain-blog/src/main/java"))) return current.resolve("backend");
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate the V2 backend reactor");
    }
}
