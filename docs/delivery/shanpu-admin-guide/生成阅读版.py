"""将图文 Markdown 生成为无需联网的 HTML 教程。"""

from html import escape
from pathlib import Path
import re

from markdown_it import MarkdownIt


GUIDE_DIR = Path(__file__).resolve().parent
source = (GUIDE_DIR / "操作教程.md").read_text(encoding="utf-8")
parser = MarkdownIt("commonmark", {"html": False}).enable("table")
tokens = parser.parse(source)
contents = []

for index, token in enumerate(tokens):
    if token.type == "heading_open" and token.tag == "h2":
        title = tokens[index + 1].content
        anchor = f"section-{len(contents) + 1:02d}"
        token.attrSet("id", anchor)
        contents.append((anchor, title))

body = parser.renderer.render(tokens, parser.options, {})
# 图片保持原图文件，点击即可在浏览器中按原始分辨率查看。
body = re.sub(
    r'<img src="([^"]+)" alt="([^"]*)"\s*/?>',
    lambda match: (
        f'<a class="screenshot" href="{match[1]}" target="_blank" '
        f'rel="noopener" aria-label="查看原图：{match[2]}">'
        f'<img src="{match[1]}" alt="{match[2]}" loading="lazy"></a>'
        f'<span class="image-caption">{match[2]} · 点击图片放大查看</span>'
    ),
    body,
)
toc = "\n".join(
    f'<li><a href="#{anchor}">{escape(title)}</a></li>'
    for anchor, title in contents
)

document = """<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>陕铺后台操作教程</title>
<style>
*{box-sizing:border-box}html{scroll-behavior:smooth;scroll-padding-top:28px}
body{margin:0;background:#f5f6f4;color:#242a26;font:16px/1.85 "Microsoft YaHei","PingFang SC",sans-serif}
a{color:#306849;text-underline-offset:3px}a:hover{color:#174d2b}
.layout{max-width:1440px;margin:auto;display:grid;grid-template-columns:290px minmax(0,1fr);gap:36px;padding:36px 28px}
aside{position:sticky;top:28px;align-self:start;max-height:calc(100vh - 56px);overflow:auto;padding:0 8px 24px 0}
.brand{font-size:28px;font-weight:700;color:#000;letter-spacing:.1em;margin:0}
.aside-label{margin:0 0 20px;font-size:14px;color:#59665b}
summary{font-weight:700;cursor:pointer}nav ol{list-style:none;padding:0;margin:12px 0}
nav li{margin:0;border-bottom:1px solid #e4e8e3}nav a{display:block;padding:7px 5px;font-size:14px;line-height:1.65;text-decoration:none}
.tools{display:flex;gap:8px;flex-wrap:wrap;margin-top:16px}button,.source-link{border:1px solid #b6c5b9;border-radius:5px;padding:7px 12px;background:#fff;color:#2b563b;font:inherit;font-size:13px;cursor:pointer;text-decoration:none}
.tools-note{font-size:12px;line-height:1.7;color:#637266;margin-top:12px}
main{min-width:0;background:#fff;padding:38px 48px 70px;border:1px solid #e0e6df;border-radius:8px}
h1,h2,h3{color:#000;line-height:1.5}h1{font-size:32px;margin:0 0 16px}h2{font-size:24px;margin:58px 0 20px;border-top:2px solid #dbe6da;padding-top:24px}h3{font-size:19px;margin:30px 0 12px}
p{margin:14px 0}li{margin:8px 0}strong{color:#172c1d}code{font:15px/1.6 Consolas,monospace;background:#edf3ed;padding:2px 5px;border-radius:3px}
table{border-collapse:collapse;width:100%;margin:20px 0 26px;font-size:14px;line-height:1.8}th,td{border:1px solid #d5ded3;padding:11px 13px;text-align:left;vertical-align:top;overflow-wrap:anywhere}th{background:#edf3ec;color:#192e20;font-weight:700}tbody tr:nth-child(even){background:#fafcf9}
.screenshot{display:block;margin-top:22px;line-height:0;border:1px solid #d5ded3;border-radius:5px;overflow:hidden;cursor:zoom-in}
.screenshot img{width:100%;height:auto;display:block}.image-caption{display:block;text-align:center;font-size:13px;line-height:1.6;color:#5d6d60;margin:9px 0 24px}
footer{margin-top:45px;border-top:1px solid #dce5d9;padding-top:16px;color:#607164;font-size:13px}
@media(max-width:1000px){.layout{display:block;padding:18px}aside{position:static;max-height:none;padding:10px 4px 24px}nav ol{columns:2;column-gap:20px}nav li{break-inside:avoid}main{padding:28px}h1{font-size:28px}}
@media(max-width:600px){.layout{padding:8px}main{padding:22px 16px}nav ol{columns:1}h2{font-size:22px}table{font-size:12px}th,td{padding:8px 6px}body{font-size:15px}ol,ul{padding-left:24px}}
@media print{@page{size:A4;margin:16mm}body{background:white;font-size:11pt;line-height:1.65}.layout{display:block;padding:0}aside{display:none}main{padding:0;border:0;border-radius:0}h1{font-size:24pt}h2{font-size:17pt;break-after:avoid;margin-top:25px}h3{break-after:avoid;font-size:13pt}.screenshot{break-inside:avoid;border-color:#ccc}.screenshot img{max-height:155mm;object-fit:contain}.image-caption{font-size:9pt}table{font-size:9pt}tr{break-inside:avoid}thead{display:table-header-group}a{color:inherit}.source-link{display:none}}
</style>
</head>
<body>
<div class="layout">
<aside aria-label="教程目录">
<p class="brand">陕铺</p><p class="aside-label">后台操作教程 · 2026年11月18日</p>
<details open><summary>按任务查找操作步骤</summary><nav><ol>__TOC__</ol></nav></details>
<div class="tools"><button type="button" onclick="window.print()">打印 / 保存为 PDF</button><a class="source-link" href="操作教程.md">可编辑正文</a></div>
<p class="tools-note">本页可离线阅读。按 Ctrl+F 查找文字；点击截图打开原图，查看小字和操作编号。</p>
</aside>
<main><article>__BODY__</article><footer>教程正文保存在同目录“操作教程.md”。转发时请保留 images 文件夹。截图为演示数据，不包含正式业务账号。</footer></main>
</div>
<script>
const compactLayout = window.matchMedia('(max-width: 600px)');
const directory = document.querySelector('aside details');
function adaptDirectory() { directory.open = !compactLayout.matches; }
compactLayout.addEventListener('change', adaptDirectory);
adaptDirectory();
</script>
</body>
</html>
""".replace("__TOC__", toc).replace("__BODY__", body)

target = GUIDE_DIR / "操作教程.html"
target.write_text(document, encoding="utf-8", newline="\n")
print(f"已生成：{target}，共 {len(contents)} 个章节")

# 阅读版与压缩包由同一入口生成，并逐文件核对，避免转发旧正文或缺图。
import zipfile
package = GUIDE_DIR.parent / "陕铺后台操作教程-2026-11-18.zip"
files = [GUIDE_DIR / "操作教程.md", target, *sorted((GUIDE_DIR / "images").glob("*.png"))]
entries = {
    source.relative_to(GUIDE_DIR).as_posix(): (
        source.read_text(encoding="utf-8").encode("utf-8")
        if source.suffix in {".md", ".html"}
        else source.read_bytes()
    )
    for source in files
}
with zipfile.ZipFile(package, "w", zipfile.ZIP_DEFLATED) as archive:
    for name, content in entries.items():
        archive.writestr(name, content)
with zipfile.ZipFile(package) as archive:
    for name, content in entries.items():
        assert archive.read(name) == content
print(f"已生成并核对：{package}，共 {len(files)} 个文件")
