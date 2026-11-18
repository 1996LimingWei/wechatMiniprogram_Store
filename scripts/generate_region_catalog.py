"""从固定版本MIT数据生成离线地区资源，不执行下载包中的代码。"""
import base64
import hashlib
import io
import json
from pathlib import Path
import re
import tarfile
from urllib.request import urlopen

ROOT = Path(__file__).resolve().parent.parent
URL = 'https://registry.npmjs.org/@vant/area-data/-/area-data-2.2.0.tgz'
INTEGRITY = 'H1payEoXLnUqTNPdt2zftv0yoZBP8AZF69XntMqqGOn5LGtuaZ/VeRXQnJLiBPgGuHs2sXPgZ4gUVGp4Cg154w=='


def main():
    with urlopen(URL, timeout=30) as response:
        content = response.read()
    if base64.b64encode(hashlib.sha512(content).digest()).decode() != INTEGRITY:
        raise ValueError('数据源完整性不匹配，停止生成')
    with tarfile.open(fileobj=io.BytesIO(content)) as archive:
        source = archive.extractfile('package/dist/index.cjs').read().decode()
        license_text = archive.extractfile('package/LICENSE').read().decode()
    groups = []
    for name in ['province_list', 'city_list', 'county_list']:
        block = re.search(name + r':\s*\{([^}]+)\}', source).group(1)
        entries = re.findall(r"(\d{6}):\s*'([^']+)'", block)
        groups.append({int(code): label for code, label in entries if int(code[:2]) <= 65})
    provinces, cities, counties = groups
    if len(provinces) != 31 or len(counties) < 2800:
        raise ValueError('地区覆盖数量不符合预期')
    nodes = []
    for level, group in enumerate(groups, 1):
        for code, label in sorted(group.items()):
            parent = 1 if level == 1 else code // (10000 if level == 2 else 100) * (10000 if level == 2 else 100)
            if level > 1 and parent not in groups[level - 2]:
                raise ValueError('地区缺少上级：' + str(code))
            nodes.append({'id': code, 'name': label, 'parentId': parent, 'type': level})
    if len({node['id'] for node in nodes}) != len(nodes):
        raise ValueError('不同层级存在重复编码，请显式处理，不能静默覆盖')
    target = ROOT / 'shop-backend/shop-module-trade/src/main/resources/regions'
    target.mkdir(parents=True, exist_ok=True)
    (target / 'mainland.json').write_text(json.dumps(nodes, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    (target / 'LICENSE-vant-area-data.txt').write_text(license_text, encoding='utf-8')
    print('已生成离线地区：', len(provinces), '省级，', len(cities), '市级，', len(counties), '区县级')


if __name__ == '__main__':
    main()
