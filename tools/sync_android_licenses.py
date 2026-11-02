#!/usr/bin/env python3
"""Synchronize checked-in Android legal assets from their authoritative project copies."""
from pathlib import Path
import argparse

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--check', action='store_true', help='Read-only verification; fail on stale assets')
args = parser.parse_args()
sources = {root/'LICENSE': 'LICENSE.txt', root/'THIRD_PARTY_NOTICES.md': 'THIRD_PARTY_NOTICES.md'}
sources.update({p: p.name for p in (root/'licenses').glob('*.txt')})
folder = root/'app/src/main/assets/licenses'
for source, filename in sources.items():
    target = folder/filename
    if args.check:
        assert target.is_file() and target.read_bytes() == source.read_bytes(), f'Stale legal asset: {filename}'
    else:
        folder.mkdir(parents=True, exist_ok=True)
        target.write_bytes(source.read_bytes())
print(f'{len(sources)} Android legal assets ' + ('verified' if args.check else 'synchronized'))
