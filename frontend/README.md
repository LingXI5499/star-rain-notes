# 前端

## 源码阅读与注释

每个 Vue、JavaScript 和 CSS 源文件（包含测试）都有职责说明。Vue 文件标明输入属性、输出事件、主要状态和阅读入口；函数注释说明请求路径、状态变化、参数、结果及异步边界。样式说明对应选择器、布局、交互状态和断点，既有行为由测试与实现共同约束。

可从 `src/main.js`、`src/router/index.js` 和 `src/shared/shells` 开始，再按业务进入 `src/modules`。接口文件复用 `src/shared/http.js`；Markdown 编辑、媒体选择与保存正文是不同步骤，上传完成不等于已建立业务引用。公开阅读共用 Markdown 组件与目录跟踪能力，博客和作品采用左栏目录、右侧正文布局。

`package.json` 的 `type: module` 使用 ES 模块；`private: true` 避免误发布 npm 包。`scripts` 分别提供本地开发、生产构建、构建预览和自动化测试；`dependencies` 是应用所需库，`devDependencies` 是构建及测试工具，`overrides` 统一指定传递依赖。生成的 `package-lock.json` 记录精确依赖树，保持原样；JSON 不支持注释，因此在此说明。`public/brand/site.webmanifest` 的名称、主题色和图标描述可安装应用的公开品牌资源，也保持原样。

HTML 入口、静态 404、品牌 SVG、构建配置和 `.env.example` 也有对应注释。`VITE_*` 值会进入浏览器产物，只能保存公开信息；依赖目录、生成产物和二进制图片不属于项目源码注释范围。

Vue 3、Vite、Vue Router、Pinia。公开页面与 `/useradmin` 共用单域名，首页直接加载，管理及其他页面按需加载。Markdown 经净化后渲染，支持代码高亮、公式、表格和目录。

教程阅读使用章节目录、正文和页内导航三栏。博客与作品详情使用左侧导航、右侧正文两栏，手机端可展开目录。作品详情仅在配置有效代码库地址时展示代码库按钮，详情页不展示封面与在线访问入口。

具备媒体上传权限的账号可在 Markdown 编辑器中选图、粘贴或拖入图片，单张上限为 20 MB。图片上传后立即进入媒体库并插入正文，保存正文后建立内容引用；未保存正文时媒体仍保留在库中。也可通过带缩略图和分页的媒体选择器插入已有图片。编辑器图片默认公开可读，请勿上传私人文件。

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
