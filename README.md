# Inkwell · 墨水瓶

> 把剪切板或 txt / Markdown 里的文字，一键灌进 Minecraft 的《书与笔》。

Minecraft **26.3** · Fabric **纯客户端** · 作者 **KujouShiori**

*[English](README.en.md)*

原版《书与笔》只能一个字一个字敲，而且编辑框写着 1024 字/页——可阅读界面一页**根本画不下**这么多。Inkwell 在书页旁边加了按钮，把整篇文章按"实际能看见的排版"切好页写进去。

## 特性

| 功能 | 说明 |
|---|---|
| **文本库** | 列出 `.minecraft/inkwell/` 下的 txt / md，实时搜索文件名、按名称/时间/大小排序，右侧预览首屏 |
| **文件夹浏览** | 支持子文件夹：`inkwell/小说A/ch01.md`、`inkwell/小说B/ch01.md` 各自成组，点文件夹进入，面包屑显示当前位置 |
| **递归扫描** | 一键把子文件夹里的文件全平铺出来（按相对路径排序），找文件不用一层层点 |
| **游戏内设置** | 书页右侧「设置」按钮，目录、翻页标记、每页行数/字数、页数上限、Markdown、递归等都能直接改，保存即生效 |
| **Markdown** | `.md` 自动转纯文本：标题、加粗、斜体、行内代码、链接、列表、引用、代码块、表格全部清掉，单独一行的 `---` 保留为**翻页标记** |
| **按显示分页** | 用游戏字体实测每行宽度（114px）和行高，一页 14 行，保证书里每一页都完整可见 |
| **两种写入方式** | 「从当前页覆盖」或「末尾追加」，导入前弹确认框写清楚会覆盖到第几页 |
| **跳转页码** | 输入页码回车即达，不用狂点上一页/下一页 |
| **粘贴剪切板** | 长文本一键整本导入，短文本仍按原版在光标处粘贴 |

## 安装

1. 装好 **Fabric Loader 0.19.5+**（Minecraft 26.3，Java 25）
2. 把 `inkwell-1.2.1.jar` 丢进 `.minecraft/mods/`
3. 启动游戏，日志里会出现 `Inkwell 已加载 | …`

不需要装 Fabric API，也不用装服务端。

## 文档

- [格式说明](docs/格式说明.md) — 放哪里、什么编码、`---` 翻页、Markdown 怎么转、一页能放多少字
- [English README](README.en.md)

## 使用

打开《书与笔》，书页右侧会出现五个按钮：

- **文本库 / 预览** — 选文件、看预览、确认导入
- **粘贴剪贴板** — 直接把剪贴板内容分页写进书里
- **跳转页码…** — 输入页码跳转
- **设置** — 游戏内改配置，点保存立即生效（同时写回 `config/inkwell.properties`）
- **签名…** — 直接打开原版签名界面，不用去点那个小按钮

快捷键（在书与笔界面里按）：

| 按键 | 作用 |
|---|---|
| `Ctrl + V` | 剪贴板超过一页才整本导入；短文本交给原版 |
| `Ctrl + Shift + V` | 无条件把剪贴板导入成整页 |
| `Ctrl + Alt + V` | 导入 `inkwell/book.txt` |
| **鼠标中键** | 粘贴剪贴板（输入法吞掉 `Ctrl+V` 时的备用入口，只在书与笔界面生效） |

文本文件放在游戏目录下的 `inkwell/` 里（进游戏后第一次导入会自动创建）。可以建子文件夹按作品分类：

```
inkwell/
├── 长篇一/
│   ├── 第一章.md
│   └── 第二章.md
└── 短篇集/
    └── 番外.txt
```

文本库里点文件夹进入，或者打开「递归」直接看全部。

## 配置

`config/inkwell.properties`：

```properties
folder=inkwell            # txt/md 所在文件夹（相对游戏目录）
file=book.txt             # Ctrl+Alt+V 导入的文件名

recursive=true            # 文本库默认递归扫描子文件夹
displayPaging=true        # 按实际显示宽度分页（推荐）；false 只按字符数
charsPerPage=1024         # 每页字符硬上限（原版编辑框上限）
linesPerPage=14           # 每页行数（原版 126 / 9 = 14）
maxPages=100              # 整本页数上限（原版 WritableBookContent.MAX_PAGES）
pageBreakLine=---         # 单独一行等于强制翻页
markdown=true             # 读 .md 时清掉 Markdown 语法
longClipboardOnly=true    # Ctrl+V 时短文本不接管
maxFileBytes=8388608      # 单个文件大小上限
debug=false               # 打印每个按键事件
```

## 一页到底能显示多少字

| 参数 | 值 | 来源 |
|---|---|---|
| 文本区宽度 | 114 px | `BookViewScreen.TEXT_WIDTH` |
| 文本区高度 | 128 px（阅读）/ 126 px（编辑） | `TEXT_HEIGHT` |
| 行高 | 9 px | `font.lineHeight` |
| → 每页行数 | 14 行 | 128 ÷ 9 |

中文约 12 字/行 → **一页约 170 字**；英文约 19 字/行 → 一页约 270 字。
`displayPaging=true` 时按这个真实排版切页，所以书里不会出现"点开只有半页字"的情况。

## 从源码构建

```bash
./gradlew build          # Windows: gradlew.bat build
```

产物在 `build/libs/inkwell-1.2.1.jar`。

> **关于映射**：Minecraft 26.3 的客户端**本身没有混淆**——Mojang 的 version json 里已经没有
> `client_mappings`，Fabric 的 intermediary 也是只有表头的空表。所以本项目不需要 Remap：
> `build.gradle` 在构建时生成一份空的 tiny 映射交给 Loom，射出的 jar 里类名就是
> `net.minecraft.client.gui.screens.inventory.BookEditScreen` 这种原名。

## 兼容性

- 只支持 **26.3**（26.x 的界面 API 和之前的版本差异很大）
- 纯客户端模组，服务端不用装；导入后仍走原版"编辑书"数据包
- 与原版一致：一本最多 100 页，超出部分会被丢弃并在日志提示

## 许可

[MIT](LICENSE) © 2026 KujouShiori
