from pathlib import Path
path = Path(r'C:/Users/sakip/.gradle/caches/fabric-loom/1.21.9/net.fabricmc.yarn.1_21_9.1.21.9+build.1-v2/mappings.tiny')
current = False
with path.open(encoding='utf-8') as f:
    for raw in f:
        stripped = raw.lstrip('\t').rstrip('\n')
        if not stripped or stripped.startswith('#'):
            continue
        parts = stripped.split('\t')
        kind = parts[0]
        if kind == 'c':
            current = len(parts) > 3 and parts[3] == 'net/minecraft/block/IceBlock'
        elif current and kind == 'm':
            print(parts)
