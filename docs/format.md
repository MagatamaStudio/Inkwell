# Text format

A condensed English version of [格式说明.md](格式说明.md).

## Where files go

```
.minecraft/inkwell/          # configurable via `folder`
├── novel-a/chapter-01.md    # subfolders are supported
└── book.txt                 # used by Ctrl+Alt+V
```

## Encoding

- Extensions: `.txt`, `.md`, `.markdown` (case-insensitive)
- Encoding: **UTF-8**, with or without BOM (the BOM is stripped automatically)
- Line endings: `LF`, `CRLF`, `CR` all work
- Per-file size limit: 8 MB (`maxFileBytes`)

## Page breaks

Pages are filled by real pixel measurement: **114 px wide, 14 lines per page**
(text area 128 px tall, line height 9 px). Roughly 170 CJK or 270 Latin characters per page.

A line containing only `---` forces a page break (configurable via `pageBreakLine`).

## Markdown conversion

| Markdown | Result |
|---|---|
| `# Heading` | Heading |
| `**bold**` `*italic*` `~~strike~~` | bold / italic / strike |
| `` `code` `` | code |
| `[text](url)` | text |
| `![alt](img)` | [图片] |
| `- item` | · item |
| `> quote` | quote |
| fenced code block | content kept, fences removed |
| table rows | pipes replaced with spaces |
| `---` | kept — becomes a page break |

## Writing rules

- **Overwrite**: starts at the current page, everything from there is replaced
- **Append**: appended after the last page
- Book cap: **100 pages** (vanilla), excess is dropped and logged
- Empty pages are never written; leading/trailing whitespace is stripped
