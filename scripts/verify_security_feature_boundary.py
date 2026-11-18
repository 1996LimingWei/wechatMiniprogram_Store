"""检查已评审的组件报告所涉及功能是否被应用启用；不能替代完整漏洞扫描。"""
from pathlib import Path
import re
from delivery_backup import ROOT

PATTERN = re.compile(r'\b(?:XsltView|XsltViewResolver|SseEmitter|FragmentsRendering|ServerSentEvent|OgnlCache)\b|(?:ognl\.Ognl|Ognl\.(?:getValue|parseExpression))')


def main():
    violations = []
    for file in (ROOT / 'shop-backend').rglob('*.java'):
        path = file.as_posix()
        if '/src/main/java/' not in path:
            continue
        if PATTERN.search(file.read_text(encoding='utf-8-sig')):
            violations.append(str(file.relative_to(ROOT)))
    if violations:
        raise ValueError('新增受影响功能使用，需要重新评审：' + ', '.join(violations))
    print('应用未启用受影响的 XSLT/SSE 片段渲染或直接 OGNL 表达式执行功能；组件报告仍保留在审查记录。')


if __name__ == '__main__':
    main()
