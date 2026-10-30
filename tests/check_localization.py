#!/usr/bin/env python3
"""Validate UI locale coverage, format arguments and resource references without Android."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
res = root / 'app/src/main/res'
def strings(folder):
    values = {}
    for file in (res / folder).glob('*.xml'):
        for entry in ET.parse(file).getroot():
            key = entry.attrib['name']
            if key.startswith('ui_'):
                assert key not in values, f'Duplicate resource: {key}'
                if entry.tag == 'plurals':
                    for item in entry:
                        values[key + '/' + item.attrib['quantity']] = ''.join(item.itertext())
                else:
                    values[key] = ''.join(entry.itertext())
    return values
english, italian = strings('values'), strings('values-it')
assert english and {k.split('/')[0] for k in english} == {k.split('/')[0] for k in italian}, 'English/Italian resource coverage differs'
formats = re.compile(r'%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z]')
for key, text in italian.items():
    default = english.get(key, english.get(key.split('/')[0] + '/other'))
    assert default and default.strip() and text.strip(), f'Empty translation: {key}'
    assert sorted(formats.findall(default)) == sorted(formats.findall(text)), f'Format mismatch: {key}'
references = 0
for file in (root / 'app/src/main/java').rglob('*.kt'):
    if file.name.endswith(' 2.kt'):
        continue
    for key in re.findall(r'R\.(?:string|plurals)\.(ui_\w+)', file.read_text()):
        references += 1
        assert key in english or key + '/other' in english, f'Missing default translation: {file.name}: {key}'
assert references > 0, 'No source resource references checked'
print(f'{len(english)} English/Italian resource pairs: coverage, format arguments and references verified')
