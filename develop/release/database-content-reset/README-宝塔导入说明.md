# 星雨笔录数据库内容重建包

本目录只负责重建教程体系与作品数据。它不会修改博客、英语、用户、媒体、数据库结构或 Flyway 迁移记录。

执行后固定得到：5 个教程分类、8 门公开教程、79 个目录组、223 个公开章节，以及 1 个公开精选作品“星雨笔录”。

> 该操作会删除数据库里现有的全部教程、教程章节和作品。必须先备份数据库。

## 一、在宝塔中备份数据库

1. 登录宝塔面板。
2. 左侧点击“数据库”。
3. 找到数据库 `star_rain_notes`。
4. 点击该行右侧“备份”。
5. 等待备份完成，再点击“备份列表”确认出现刚生成的备份文件。
6. 记录备份时间；没有成功备份时不要继续。

## 二、上传本目录

1. 左侧点击“文件”。
2. 进入 `/opt/star-rain-notes/`。如果目录不存在，先依次创建 `star-rain-notes` 目录。
3. 将整个 `database-content-reset` 文件夹上传到该目录。
4. 上传完成后应看到：

~~~text
/opt/star-rain-notes/database-content-reset/
├── star-rain-content-reset.sql
├── README-宝塔导入说明.md
└── SHA256SUMS.txt
~~~

不要把 SQL 放进网站静态目录，也不要编辑 SQL 文件编码。

## 三、校验上传文件

1. 左侧点击“终端”。
2. 执行：

~~~bash
cd /opt/star-rain-notes/database-content-reset
sha256sum -c SHA256SUMS.txt
~~~

两个文件都必须显示 `OK`。如果显示 `FAILED`，删除服务器上的文件夹后重新上传，不能继续导入。

## 四、确认后端与数据库状态

执行：

~~~bash
curl -fsS http://127.0.0.1:24680/actuator/health
mysql -u star_rain -p -D star_rain_notes -e "SELECT VERSION(); SELECT MAX(version) AS flyway_version FROM flyway_schema_history WHERE success=1;"
~~~

第一条应返回 `{"status":"UP"}`。第二条输入数据库用户 `star_rain` 的密码后，Flyway 版本应为 `25`。

如果健康检查失败或 Flyway 不是 25，停止操作，不要导入。

## 五、记录导入前数量

执行下面命令，输入数据库密码：

~~~bash
mysql -u star_rain -p -D star_rain_notes -e "SELECT COUNT(*) AS blogs_before FROM blog_post; SELECT COUNT(*) AS words_before FROM vocabulary_word; SELECT COUNT(*) AS grammar_before FROM english_grammar_lesson;"
~~~

保存终端输出，导入后用于核对博客和英语数据没有变化。

## 六、执行内容重建

确保当前目录仍是 `/opt/star-rain-notes/database-content-reset`，然后执行：

~~~bash
mysql --default-character-set=utf8mb4 --show-warnings -u star_rain -p star_rain_notes < star-rain-content-reset.sql
~~~

- 只在密码提示符中输入密码，不要把密码写进命令。
- 不要给命令添加 `--force`。
- 正常结束时，终端最后会显示分类、教程、目录组、章节和作品数量。
- 预期值依次为 `5 / 8 / 79 / 223 / 1`，`invalid_chapters` 必须为 `0`。
- 出现任何 `ERROR` 都视为失败；不要重复尝试，先执行第十节恢复步骤。

数据通过事务一次性提交，正常导入不需要重启 Java 项目。

## 七、再次执行数据库验收

~~~bash
mysql -u star_rain -p -D star_rain_notes -e "SELECT COUNT(*) categories FROM tutorial_category; SELECT COUNT(*) tutorials FROM tutorial; SELECT node_type,COUNT(*) total FROM tutorial_node GROUP BY node_type; SELECT title,slug,publish_status,project_status,featured FROM portfolio_project; SELECT COUNT(*) blogs_after FROM blog_post; SELECT COUNT(*) words_after FROM vocabulary_word; SELECT COUNT(*) grammar_after FROM english_grammar_lesson;"
~~~

确认：

- 分类 5、教程 8、GROUP 79、CHAPTER 223、作品 1。
- “星雨笔录”为 `PUBLISHED / ONLINE / featured=1`。
- 博客、词汇和语法数量与第五节完全一致。

## 八、验证公开接口

依次执行：

~~~bash
curl -fsS -o /dev/null -w "categories: %{http_code}\n" http://127.0.0.1:24680/api/v1/public/tutorial-categories/tree
curl -fsS -o /dev/null -w "tutorials: %{http_code}\n" http://127.0.0.1:24680/api/v1/public/tutorials
curl -fsS -o /dev/null -w "javase: %{http_code}\n" http://127.0.0.1:24680/api/v1/public/tutorials/javase
curl -fsS -o /dev/null -w "portfolio: %{http_code}\n" http://127.0.0.1:24680/api/v1/public/portfolio/projects
curl -fsS -o /dev/null -w "star-rain: %{http_code}\n" http://127.0.0.1:24680/api/v1/public/portfolio/projects/star-rain-notes
~~~

五项都应返回 `200`。

## 九、浏览器验收

1. 打开 `https://yulanlin.cn/tutorials`。
2. 按 `Ctrl+F5` 强制刷新。
3. 确认左侧显示五个分类，全部教程合计为 8。
4. 打开“JavaSE 基础”，确认目录组和章节可以展开、章节正文可以打开。
5. 分别检查四门计算机基础教程以及 Git、Agent、软件工程教程。
6. 打开网站“作品”页面，确认仅显示精选作品“星雨笔录”，演示和仓库链接正确。

## 十、失败恢复

如果导入报错或页面验收不通过：

1. 不要手工删除其他表，也不要再次运行 SQL。
2. 回到宝塔“数据库”。
3. 找到 `star_rain_notes`，打开“备份列表”。
4. 选择第一节生成的备份，点击“恢复”。
5. 等待恢复结束后执行健康检查。
6. 保存导入时完整错误输出，再根据第一条 `ERROR` 定位问题。

SQL 导入和数据库恢复都会直接改变后端读取的数据，通常不需要重启 Java；只有健康检查异常时才在宝塔 Java 项目管理中重启。
