#!/usr/bin/env python3
"""JVM ZIP codec tests; supply JSON-java JAR and Android SDK stubs. No downloads.
Font validation and DataStore persistence need Android and are not invoked here.
"""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--json-jar', required=True, type=Path)
parser.add_argument('--android-jar', required=True, type=Path)
args = parser.parse_args()
cache = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle')) / 'caches/modules-2/files-2.1'
def jar(group, name, version='*'):
    files = sorted(p for p in (cache / group / name).glob(f'{version}/*/{name}-*.jar') if not p.name.endswith(('-sources.jar', '-javadoc.jar')))
    if not files: raise SystemExit(f'Missing cached {name}')
    return files[-1]
stdlib = jar('org.jetbrains.kotlin', 'kotlin-stdlib', '1.9.0')
annotations = jar('org.jetbrains', 'annotations')
compiler = [jar('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', '1.9.0'), stdlib,
            jar('org.jetbrains.kotlin', 'kotlin-reflect'), jar('org.jetbrains.kotlin', 'kotlin-script-runtime'),
            jar('org.jetbrains.intellij.deps', 'trove4j'), annotations]
java = shutil.which('java')
root = Path(__file__).resolve().parents[1]
classpath = [args.json_jar.resolve(), args.android_jar.resolve(), stdlib, annotations]
with tempfile.TemporaryDirectory(prefix='nodus-backup-tests-') as output:
    subprocess.run([java, '-cp', os.pathsep.join(map(str, compiler)), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-no-stdlib', '-no-reflect', '-classpath', os.pathsep.join(map(str, classpath)), '-d', output,
        str(root / 'app/src/main/java/com/nodus/launcher/util/ConfigSchema.kt'),
        str(root / 'app/src/main/java/com/nodus/launcher/util/ConfigBackup.kt'),
        str(root / 'tests/ConfigBackupTest.kt')], check=True)
    subprocess.run([java, '-cp', os.pathsep.join(map(str, [output, *classpath])), 'ConfigBackupTestKt'], check=True)
