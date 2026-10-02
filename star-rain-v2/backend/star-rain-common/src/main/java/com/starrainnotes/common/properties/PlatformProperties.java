package com.starrainnotes.common.properties;

import com.starrainnotes.common.enumeration.SiteEntry;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*
 * 三入口的域名配置。前缀 star-rain.platform，见 star-rain-boot 的 application.yml。
 *
 * 这里用 @Component 让组件扫描直接注册，而不是另建一个 @Configuration 类：
 * common 的分层里没有 config 类别，为绑定一个配置类新增一层类别不划算。
 *
 * 默认值与 application.yml 逐条对齐，并且都指向本地可用的地址。
 * 线上必须由环境变量覆盖，否则会退回 127.0.0.1 / localhost 这一组本地值。
 *
 * 列表元素允许写成通配后缀（"*.yulanlin.cn"），匹配规则由 HostEntryResolver 负责。
 */
@Data
@Component
@ConfigurationProperties(prefix = "star-rain.platform")
public class PlatformProperties {

    // 公开站域名：匿名只读，不暴露账号相关能力
    private List<String> publicHosts = new ArrayList<>(List.of(
            "yulanlin.cn", "www.yulanlin.cn", "127.0.0.1", "localhost"));

    // 用户站域名：USER 与 ADMIN 共用
    private List<String> userHosts = new ArrayList<>(List.of(
            "user.yulanlin.cn", "user.localhost"));

    // 管理站域名：SUPER_ADMIN
    private List<String> adminHosts = new ArrayList<>(List.of(
            "admin.yulanlin.cn", "admin.localhost"));

    /*
     * 取某个入口配置的域名列表。
     * 解析器只依赖这个方法，因此新增入口时只需要在这里补一条分支。
     */
    public List<String> hostsOf(SiteEntry entry) {
        return switch (entry) {
            case PUBLIC -> getPublicHosts();
            case USER -> getUserHosts();
            case ADMIN -> getAdminHosts();
        };
    }
}
