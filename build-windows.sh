#!/usr/bin/env bash
# Windows-adapted copy of build.sh. Differences from the original, all environmental:
#   1. d8 / apksigner / sdkmanager are .bat on Windows; bash will not resolve them extensionless.
#   2. python3 is named python on this machine.
#   3. JAVA_HOME is pinned so javac/jar/keytool resolve.
set -euo pipefail
cd "$(dirname "$0")"
# Point JAVA_HOME at your JDK 17 if it is not already on PATH.
: "${JAVA_HOME:=/c/Program Files/Microsoft/jdk-17.0.20.101-hotspot}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
# Windows-style path with forward slashes: javac cannot read MSYS /c/... paths.
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT, e.g. export ANDROID_SDK_ROOT=\"C:/Users/you/AppData/Local/Android/Sdk\"}"
case "$ANDROID_SDK_ROOT" in
 /*) ANDROID_SDK_ROOT="$(cygpath -m "$ANDROID_SDK_ROOT" 2>/dev/null || echo "$ANDROID_SDK_ROOT")" ;;
esac
bt="$ANDROID_SDK_ROOT/build-tools/35.0.0"
platform="$ANDROID_SDK_ROOT/platforms/android-35/android.jar"
mkdir -p build/classes build/dex build/tests build/keys dist

echo "=== JVM logic tests ==="
javac -encoding UTF-8 -d build/tests app/src/main/java/in/nxprototype/app/PaymentCore.java tests/PaymentCoreTest.java
java -cp build/tests in.nxprototype.app.PaymentCoreTest
javac -encoding UTF-8 -cp build/tests -d build/tests app/src/main/java/in/nxprototype/app/QueueCore.java tests/QueueCoreTest.java
java -cp build/tests in.nxprototype.app.QueueCoreTest
javac -encoding UTF-8 -cp build/tests -d build/tests app/src/main/java/in/nxprototype/app/SplitCore.java app/src/main/java/in/nxprototype/app/AssistSession.java tests/SplitCoreTest.java
java -cp build/tests in.nxprototype.app.SplitCoreTest
javac -encoding UTF-8 -d build/tests app/src/main/java/in/nxprototype/app/ObserverSession.java tests/ObserverSessionTest.java
java -cp build/tests in.nxprototype.app.ObserverSessionTest
javac -encoding UTF-8 -d build/tests app/src/main/java/in/nxprototype/app/AppChoices.java tests/AppChoicesTest.java
java -cp build/tests in.nxprototype.app.AppChoicesTest
javac -encoding UTF-8 -cp libs/zxing-core-3.5.3.jar -d build/tests app/src/main/java/in/nxprototype/app/QrDecoder.java tests/QrDecoderTest.java
java -cp "build/tests;libs/zxing-core-3.5.3.jar" in.nxprototype.app.QrDecoderTest

echo "=== resources ==="
"$bt/aapt2.exe" compile --dir app/src/main/res -o build/resources.zip
"$bt/aapt2.exe" link -o build/base.apk -I "$platform" --manifest app/src/main/AndroidManifest.xml -A app/src/main/assets build/resources.zip

echo "=== compile app ==="
find app/src/main/java -name '*.java' -print > build/sources.txt
javac -encoding UTF-8 -source 8 -target 8 -classpath "$platform;libs/zxing-core-3.5.3.jar" -d build/classes @build/sources.txt
jar cf build/classes.jar -C build/classes .

echo "=== dex ==="
"$bt/d8.bat" --min-api 26 --lib "$platform" --output build/dex build/classes.jar libs/zxing-core-3.5.3.jar

echo "=== package ==="
cp build/base.apk build/unsigned.apk
python - <<'PY'
import zipfile
from pathlib import Path
with zipfile.ZipFile('build/unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
    for f in Path('build/dex').glob('*.dex'): z.write(f,f.name)
PY
"$bt/zipalign.exe" -f -p 4 build/unsigned.apk build/aligned.apk

echo "=== sign ==="
if [ ! -f build/keys/prototype.jks ]; then
 keytool -genkeypair -keystore build/keys/prototype.jks -storepass android -keypass android -alias prototype -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=NX Lab Prototype,O=Development,C=IN'
fi
"$bt/apksigner.bat" sign --ks build/keys/prototype.jks --ks-key-alias prototype --ks-pass pass:android --key-pass pass:android --out dist/ranna-0.1.12.apk build/aligned.apk
"$bt/apksigner.bat" verify --verbose dist/ranna-0.1.12.apk
sha256sum dist/ranna-0.1.12.apk > dist/SHA256SUMS.txt
echo "=== done ==="
