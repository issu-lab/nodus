#!/usr/bin/env python3
"""Run pure Kotlin command tests using the project's already cached Kotlin compiler.

No downloads, Android runtime, Gradle daemon or project build artifacts required.
"""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--gradle-cache', type=Path,
                    default=Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle')) / 'caches')
args = parser.parse_args()
cache = args.gradle_cache / 'modules-2/files-2.1'

def jar(group, module, version=None):
    choices = sorted((cache / group / module).glob(f'{version or "*"}/*/{module}-*.jar'))
    choices = [p for p in choices if not p.name.endswith(('-sources.jar', '-javadoc.jar'))]
    if not choices:
        raise SystemExit(f'Missing cached {module}; compile the Android project to populate its Kotlin cache first.')
    return choices[-1]

stdlib = jar('org.jetbrains.kotlin', 'kotlin-stdlib', '1.9.0')
annotations = jar('org.jetbrains', 'annotations')
compiler = [jar('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', '1.9.0'), stdlib,
            jar('org.jetbrains.kotlin', 'kotlin-reflect'),
            jar('org.jetbrains.kotlin', 'kotlin-script-runtime'),
            jar('org.jetbrains.intellij.deps', 'trove4j'), annotations]
java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if 'JAVA_HOME' in os.environ else shutil.which('java')
if not java:
    raise SystemExit('A JDK is required.')
root = Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory(prefix='nodus-command-tests-') as output:
    subprocess.run([java, '-cp', os.pathsep.join(map(str, compiler)),
                    'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect',
                    '-classpath', os.pathsep.join(map(str, [stdlib, annotations])), '-d', output,
                    str(root / 'app/src/main/java/com/nodus/launcher/notifications/NotificationActions.kt'),
                    str(root / 'tests/NotificationActionsTest.kt'),
                    str(root / 'app/src/main/java/com/nodus/launcher/home/HomePolicy.kt'),
                    str(root / 'tests/HomePolicyTest.kt'),
                    str(root / 'app/src/main/java/com/nodus/launcher/home/HomeInteractionPolicy.kt'),
                    str(root / 'tests/HomeInteractionTest.kt'),
                    str(root / 'app/src/main/java/com/nodus/launcher/home/AppDrawerPolicy.kt'),
                    str(root / 'tests/AppDrawerPolicyTest.kt'),
                    str(root / 'app/src/main/java/com/nodus/launcher/widgets/BatteryReading.kt'),
                    str(root / 'tests/BatteryReadingTest.kt'),
                    str(root / 'app/src/main/java/com/nodus/launcher/home/HeaderSizes.kt'),
                    str(root / 'tests/HeaderSizesTest.kt')], check=True)
    subprocess.run([java, '-cp', os.pathsep.join([output, str(stdlib)]),
                    'NotificationActionsTestKt'], check=True)
    subprocess.run([java, '-cp', os.pathsep.join([output, str(stdlib)]),
                    'HomePolicyTestKt'], check=True)

    subprocess.run([java, '-cp', os.pathsep.join([output, str(stdlib)]),
                    'HomeInteractionTestKt'], check=True)

    subprocess.run([java, '-cp', os.pathsep.join([output, str(stdlib)]),
                    'AppDrawerPolicyTestKt'], check=True)

    subprocess.run([java, '-cp', os.pathsep.join([output, str(stdlib)]),
                    'BatteryReadingTestKt'], check=True)

    subprocess.run([java, '-cp', os.pathsep.join([output, str(stdlib)]),
                    'HeaderSizesTestKt'], check=True)
