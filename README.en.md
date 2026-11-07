# Inkwell

> Paste a clipboard or a txt / Markdown file straight into a Minecraft **Book and Quill**.

Minecraft **26.3** · Fabric **client-side only** · by **KujouShiori**

*[中文说明](README.md)*

The vanilla book editor gives you a 1024-character limit per page, but the reading screen can only
**draw** about 14 lines. Inkwell adds buttons to the book screen and paginates your text by real
pixel measurements, so every page actually fits on screen.

## Features

| Feature | Description |
|---|---|
| **Text library** | Lists `.txt` / `.md` files under `.minecraft/inkwell/`, live filename search, sort by name / modified time / size, first-page preview |
| **Folder browsing** | Subfolders work: `inkwell/novel-a/ch01.md`, `inkwell/novel-b/ch01.md`. Click a folder to enter, breadcrumb shows where you are |
| **Recursive scan (optional)** | Off by default — browse folder by folder. Turn it on to see everything at once, **grouped by folder** with a header per group |
| **Markdown** | `.md` is converted to plain text — headings, bold, italics, inline code, links, lists, quotes, fenced code and tables are stripped; a lone `---` becomes a **page break** |
| **Display-aware pagination** | Measures with the game font: 114 px wide, 14 lines per page |
| **Overwrite / append** | Write from the current page, or append at the end; a confirmation dialog explains exactly what gets replaced |
| **Page jump** | Type a page number, press Enter |
| **Sign shortcut** | Opens the vanilla signing screen with one click |
| **In-game settings** | Folder, page-break marker, lines/characters per page, page cap, Markdown, recursion, debug logging — saved immediately |
| **Clipboard paste** | Long clipboard content is imported as a whole book; short text still pastes inline like vanilla |

## Install

1. Install **Fabric Loader 0.19.5+** for Minecraft 26.3 (Java 25)
2. Drop `inkwell-1.3.0.jar` into `.minecraft/mods/`
3. Launch — the log shows `Inkwell 已加载 | ...` (Inkwell loaded)

No Fabric API needed. Nothing goes on the server.

## Usage

Open a Book and Quill — five buttons appear to the right of the page:

- **文本库 / 预览** (Text library / preview) — browse files, preview, import
- **粘贴剪贴板** (Paste clipboard) — import clipboard as pages
- **跳转页码…** (Jump to page) — type a page number
- **设置** (Settings) — in-game configuration
- **签名…** (Sign) — open the signing screen

Shortcuts inside the book screen:

| Key | Action |
|---|---|
| `Ctrl + V` | Import clipboard as a whole book if it is longer than one page; short text falls through to vanilla |
| `Ctrl + Shift + V` | Always import the whole clipboard |
| `Ctrl + Alt + V` | Import `inkwell/book.txt` |
| **Middle click** | Paste the clipboard (fallback for IMEs that swallow `Ctrl+V`) |

Text files live in the `inkwell/` folder of your game directory (created on first use):

```
inkwell/
├── novel-a/
│   ├── chapter-01.md
│   └── chapter-02.md
└── shorts/
    └── extra.txt
```

## Configuration

`config/inkwell.properties` — or just click **Settings** in game:

```properties
folder=inkwell            # text folder, relative to the game directory
file=book.txt             # file used by Ctrl+Alt+V
recursive=false           # browse folder by folder; true = flat list grouped by folder
displayPaging=true        # paginate by real pixel width (recommended)
charsPerPage=1024         # hard character cap per page (vanilla editor limit)
linesPerPage=14           # lines per page (vanilla: 126 / 9 = 14)
maxPages=100              # pages per book (vanilla cap)
pageBreakLine=---         # a line containing this forces a page break
markdown=true             # strip Markdown when reading .md
longClipboardOnly=true    # Ctrl+V only takes over for long clipboards
maxFileBytes=8388608      # per-file size limit
debug=false               # log every key event
```

## How many characters fit on a page?

| Value | Where it comes from |
|---|---|
| Text area width | 114 px | `BookViewScreen.TEXT_WIDTH` |
| Text area height | 128 px (reading) / 126 px (editing) | `TEXT_HEIGHT` |
| Line height | 9 px | `font.lineHeight` |
| Lines per page | **14** | 128 ÷ 9 |

Roughly **170 CJK characters** or **270 Latin characters** per page. With `displayPaging=true`
Inkwell wraps text with the actual game font, so nothing gets cut off.

## Format notes

See [docs/format.md](docs/format.md) for the full format reference (encoding, page breaks,
Markdown conversion table, capacity, FAQ).

## Building from source

```bash
./gradlew build        # Windows: gradlew.bat build
```

The jar lands in `build/libs/inkwell-1.3.0.jar`.

> **About mappings:** Minecraft 26.3 ships **unobfuscated** — Mojang's version JSON no longer
> includes `client_mappings`, and Fabric's intermediary table is empty (header only). So no
> remapping is involved: `build.gradle` generates an empty tiny mapping at configure time and the
> jar keeps the original `net.minecraft....` class names.

## Compatibility

- Minecraft **26.3** only (the 26.x GUI API differs a lot from earlier versions)
- Client-side; the server needs nothing
- Same limits as vanilla: 100 pages per book, excess pages are dropped and logged

## License

[MIT](LICENSE) © 2026 KujouShiori
