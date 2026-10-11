# 前端

Vue 3、Vite、Vue Router、Pinia。公开页面与 `/useradmin` 共用单域名，首页直接加载，管理及其他页面按需加载。Markdown 经净化后渲染，支持代码高亮、公式、表格和目录。

## 开发与构建

需要 Node.js 22.12+ 或 24 LTS；在本目录运行：

```bash
npm ci
npm run dev
```

开发地址 `http://127.0.0.1:5174`，`/api` 默认代理到 `http://127.0.0.1:8088`，可通过进程变量 `STAR_RAIN_API_TARGET` 更改后端地址。

```bash
npm test
npm run build
```

产物为 `dist/`。部署应提供全部构建 chunk，`/assets` 缺失返回 404；加载旧 chunk 失败时页面提供显式刷新入口，不自动丢弃未保存输入。详细同源部署与 SEO 文档路由见 [后端 README](../backend/README.md)。

## 配置与约定

站点标题、标语、首页介绍、模块展示和作者资料由后台配置管理。可复制 `.env.example` 为被 Git 忽略的 `.env.local`，配置页脚域名及备案；变更后重新构建。所有 `VITE_*` 值会进入浏览器，不能放密码、密钥或私人邮箱。

业务代码位于 `src/modules/`，跨模块 UI、HTTP、分页、导航位于 `src/shared/`，路由与权限入口在 `src/router/`。所有业务请求使用 `src/shared/http.js`；非幂等请求共享 CSRF 获取，登录状态变更时清理缓存，写操作不自动重放。

教程后台分页和筛选在服务端执行，排序模式使用完整的轻量目录，不能只提交当前页 ID。删除确认要求文章标题，允许复制时附带一层常见书名号或方括号，以及等价空格；输入不同标题仍不可删除。已发布文章应先撤回再删除。

## 第三方许可

圆形主题切换和图标过渡改编自 [clay-blog](https://github.com/laogou717/clay-blog)，提交 `65d2fa5`。以下保留原始 MIT 版权和许可：

```text
MIT License

Copyright (c) 2026 laogou717

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
