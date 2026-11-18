"""从实际 Spring Boot JAR 提取 Maven 坐标，查询 OSV，失败或有报告均不放行。"""
import argparse
from io import BytesIO
import json
import re
from pathlib import Path
from urllib.request import Request, urlopen
import zipfile


def coordinates(jar, repository=None):
    result = set()
    missing = []
    with zipfile.ZipFile(jar) as app:
        for name in app.namelist():
            if not name.startswith('BOOT-INF/lib/') or not name.endswith('.jar'):
                continue
            found = False
            with zipfile.ZipFile(BytesIO(app.read(name))) as library:
                for prop in library.namelist():
                    if prop.startswith('META-INF/maven/') and prop.endswith('/pom.properties'):
                        data = {}
                        for line in library.read(prop).decode('utf-8').splitlines():
                            if line and not line.startswith('#') and '=' in line:
                                key, value = line.split('=', 1)
                                data[key.strip()] = value.strip()
                        if all(key in data for key in ['groupId', 'artifactId', 'version']):
                            result.add((data['groupId'] + ':' + data['artifactId'], data['version']))
                            found = True
            if not found:
                missing.append(name.rsplit('/', 1)[-1])
    if repository:
        unresolved = set(missing)
        candidates = {}
        for file in Path(repository).rglob('*.jar'):
            if file.name not in unresolved:
                continue
            relative = file.relative_to(repository).parts
            if len(relative) >= 4:
                coordinate = ('.'.join(relative[:-3]) + ':' + relative[-3], relative[-2])
                candidates.setdefault(file.name, set()).add(coordinate)
        for name, choices in candidates.items():
            if len(choices) == 1:
                result.update(choices)
                unresolved.remove(name)
        missing = sorted(unresolved)
    # Spring Boot 重打包时自动加入的辅助 JAR 不带 pom.properties。
    for name in list(missing):
        match = re.fullmatch(r'spring-boot-jarmode-tools-([0-9A-Za-z.\-]+)\.jar', name)
        if match:
            result.add(('org.springframework.boot:spring-boot-jarmode-tools', match.group(1)))
            missing.remove(name)
    return sorted(result), missing


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--maven-repo', type=Path)
    args = parser.parse_args()
    packages, missing = coordinates(args.jar, args.maven_repo)
    if not packages:
        raise ValueError('没有提取到运行依赖，禁止当作零漏洞')
    query = {'queries': [{'package': {'ecosystem': 'Maven', 'name': name}, 'version': version}
                         for name, version in packages]}
    request = Request('https://api.osv.dev/v1/querybatch', json.dumps(query).encode(),
                      headers={'Content-Type': 'application/json'})
    with urlopen(request, timeout=45) as response:
        result = json.load(response)
    if len(result.get('results', [])) != len(packages):
        raise ValueError('漏洞库响应不完整')
    findings = []
    for package, entry in zip(packages, result['results']):
        if entry.get('vulns'):
            findings.append({'package': package[0], 'version': package[1],
                             'ids': [v['id'] for v in entry['vulns']]})
    report = {'source': 'https://api.osv.dev', 'checked': len(packages),
              'unidentified': missing, 'findings': findings,
              'passed': not findings and not missing}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print('运行依赖扫描：%d 个坐标，%d 组报告，%d 个未识别 JAR。' % (len(packages), len(findings), len(missing)))
    if findings or missing:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
