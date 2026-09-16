# 星雨笔录 Windows 图形化部署

发布包解压后，双击 `启动图形化部署.cmd`。向导会复制前后端产物、生成生产配置、Nginx 配置以及启动/停止脚本。

部署前需要：

- Windows Server 2019/2022 或 Windows 10/11；
- Java 21；
- MySQL 8.0，并提前创建空数据库；
- Nginx for Windows，或由运维将生成的配置转换到现有反向代理。

已有站点升级前必须备份数据库、安装目录下的 `data` 与 `config`。部署包不包含数据库、用户媒体、SMTP 密钥或生产证书。

向导完成后：

1. 检查安装目录中的 `config/application-deploy.yml`；
2. 将 `config/nginx-star-rain-notes.conf` 引入 Nginx，按实际域名和 HTTPS 证书修改 `listen` 与 `server_name`；
3. 双击 `启动服务.cmd`；
4. 重载 Nginx，访问站点与 `/actuator/health`；
5. 全新部署访问 `/admin/activate` 激活超级管理员。

配置文件含数据库密码和 SMTP 授权码，应限制读取权限。静态原型必须保留 Nginx 配置中的 CSP、`nosniff`、禁止目录浏览及独立长缓存规则。
