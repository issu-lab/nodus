#!/usr/bin/env python3
"""Prepare a clean GitHub source tree and an unchanged APK beta bundle. No network writes."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ROOT_FILES = {'.gitignore', 'LICENSE', 'README.md', 'THIRD_PARTY_NOTICES.md',
              'build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew'}
DOCS = {'BEHAVIOR.md', 'CHANGELOG.md', 'DEVELOPMENT.md', 'ROADMAP.md', 'BETA_TESTING.md'}
FOLDERS = {'app', 'assets', 'source', 'licenses', 'tests', 'tools', '.github', 'gradle'}
DENIED = {'.git', '.gradle', 'build', 'node_modules', '__pycache__', 'dist', '.idea'}


def source_paths():
    paths = subprocess.check_output(['git', 'ls-files', '-z', '--', '.'], cwd=ROOT).decode().split('\0')
    selected = []
    for name in paths:
        if not name:
            continue
        p = Path(name)
        if any(part in DENIED for part in p.parts):
            continue
        if p.suffix.lower() in {'.apk', '.aab', '.jks', '.keystore', '.pem', '.key', '.pyc'}:
            raise ValueError(f'Private/binary distribution file in source selection: {name}')
        if len(p.parts) == 1:
            allowed = name in ROOT_FILES
        elif p.parts[0] == 'docs':
            allowed = len(p.parts) == 2 and p.name in DOCS
        else:
            allowed = p.parts[0] in FOLDERS
        if not allowed:
            continue
        full = ROOT / p
        if full.is_symlink() or not full.is_file() or not full.resolve().is_relative_to(ROOT):
            raise ValueError(f'Not a regular project source file: {name}')
        selected.append(p)
    required = {Path(x) for x in ROOT_FILES} | {Path('docs/BETA_TESTING.md'), Path('tools/prepare_github.py')}
    if not required.issubset(selected):
        raise ValueError('Commit or stage all publication metadata before exporting.')
    return sorted(selected)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repository', required=True, help='Proposed owner/name; never created by this script.')
    parser.add_argument('--output', required=True, type=Path, help='New output directory; existing paths are refused.')
    args = parser.parse_args()
    if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', args.repository):
        parser.error('Repository must be owner/name.')
    if args.output.exists():
        parser.error('Output already exists; choose a new directory to preserve the previous package.')
    version = re.search(r'versionName "([^"]+)"', (ROOT/'app/build.gradle').read_text()).group(1)
    apk = ROOT / f'dist/nodus-{version}-debug.apk'
    expected = (ROOT/f'dist/nodus-{version}-debug.sha256').read_text().split()[0]
    if sha(apk) != expected:
        raise ValueError('Existing APK hash does not match its companion checksum.')
    code = ['app/src', 'app/build.gradle', 'app/proguard-rules.pro', 'build.gradle',
            'settings.gradle', 'gradle.properties', 'gradle', 'gradlew']
    if subprocess.run(['git', 'diff', '--quiet', 'HEAD', '--', *code], cwd=ROOT).returncode:
        raise ValueError('Commit the application/build sources before preparing a publication candidate.')
    selected = source_paths()
    for p in selected:
        if p.suffix in {'.md', '.py', '.kt', '.gradle', '.yml', '.properties', '.xml'}:
            text = (ROOT/p).read_text()
            if re.search(r'(?<![A-Za-z0-9_./])/(?:home|Users)/[^/\s]+|gh[pousr]_[A-Za-z0-9]{20,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----', text):
                raise ValueError(f'Private path or credential-like content requires review: {p}')
    output = args.output.resolve()
    source = output/'repository'
    source.mkdir(parents=True)
    for p in selected:
        target = source/p
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(ROOT/p, target)
    # Raw footer URL is a candidate until this proposed repository is published.
    readme = source/'README.md'
    readme.write_text(readme.read_text().replace('src="assets/issu-open-homelab-badge.png"',
        f'src="https://raw.githubusercontent.com/{args.repository}/main/assets/issu-open-homelab-badge.png"'))
    notes = (f'noduS {version} — experimental beta\n\n'
             'Extract this ZIP and open the included APK on Android 8.0 or newer.\n'
             'This is the existing debug-signed test APK, unchanged. Keep your previous launcher installed.\n'
             'Select noduS in Android default Home settings; enable notification access for the hub.\n'
             'See docs/BETA_TESTING.md in the source repository for permissions and feedback.\n'
             'Experimental beta: phone validation is still in progress.\n')
    archive = output/f'nodus-{version}-beta.zip'
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
        z.write(apk, apk.name)
        z.writestr('INSTALL.txt', notes)
        z.write(ROOT/'LICENSE', 'LICENSE')
        z.write(ROOT/'THIRD_PARTY_NOTICES.md', 'THIRD_PARTY_NOTICES.md')
        for p in sorted((ROOT/'licenses').glob('*.txt')):
            z.write(p, 'licenses/'+p.name)
    (output/'SHA256SUMS.txt').write_text(f'{sha(archive)}  {archive.name}\n{sha(apk)}  {apk.name}\n')
    manifest = {'repository_proposal': args.repository, 'version': version,
                'workspace_commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
                'source_file_count': len(selected), 'apk_sha256': sha(apk),
                'source_files': {str(p): sha(source/p) for p in selected},
                'published': False}
    (output/'manifest.json').write_text(json.dumps(manifest, indent=2)+'\n')
    print(json.dumps({'source': str(source), 'beta_zip': str(archive), 'source_files': len(selected),
                      'apk_unchanged': True, 'published': False}, indent=2))


if __name__ == '__main__':
    main()
