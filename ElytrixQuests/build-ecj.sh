#!/usr/bin/env bash
# Сборка ElytrixQuests.jar через ECJ + spigot-api (compile-only).
# Результат: dist/ElytrixQuests.jar
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$(pwd)"

JAVA_BIN="${JAVA_BIN:-java}"
ECJ_JAR="${ECJ_JAR:-/home/user/aia9as9gfAH/.toolchain/ecj/ecj.jar}"
SPIGOT_JAR="${SPIGOT_JAR:-/home/user/aia9as9gfAH/.toolchain/spigot-api-1.16.5.jar}"

if [ ! -f "$ECJ_JAR" ]; then echo "!!! Нет ECJ: $ECJ_JAR"; exit 1; fi
if [ ! -f "$SPIGOT_JAR" ]; then echo "!!! Нет spigot-api: $SPIGOT_JAR"; exit 1; fi

CP="$SPIGOT_JAR"
for j in "$ROOT"/libs/*.jar; do CP="$CP:$j"; done

rm -rf build/classes
mkdir -p build/classes
"$JAVA_BIN" -jar "$ECJ_JAR" \
    -source 8 -target 8 -proc:none -nowarn \
    -encoding UTF-8 \
    -classpath "$CP" \
    -d build/classes \
    $(find src/main/java stubs -name "*.java")

cp -r src/main/resources/* build/classes/

mkdir -p dist
rm -f "dist/ElytrixQuests.jar"
python3 - "dist/ElytrixQuests.jar" build/classes <<'PYEOF'
import sys, zipfile, pathlib
out, src = sys.argv[1], pathlib.Path(sys.argv[2])
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
    for p in sorted(src.rglob('*')):
        if p.is_file():
            z.write(p, p.relative_to(src).as_posix())
PYEOF

echo "OK: $ROOT/dist/ElytrixQuests.jar"
