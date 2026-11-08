"""Copy each completed built-in generation into the project, keeping original outputs."""
from pathlib import Path
import json,shutil
root=Path(__file__).resolve().parents[1]
rows=json.loads((root/'revisions/v11/generation-manifest.json').read_text(encoding='utf8'))
for row in rows:
    target=root/row['source'];target.parent.mkdir(parents=True,exist_ok=True)
    if not target.exists():shutil.copy2(row['source_path'],target)
print(f'{len(rows)} generated originals saved')
