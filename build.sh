#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to an Android SDK with platform 35 and build-tools 35.0.0}"
bt="$ANDROID_SDK_ROOT/build-tools/35.0.0"
platform="$ANDROID_SDK_ROOT/platforms/android-35/android.jar"
mkdir -p build/classes build/dex build/tests build/keys dist
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
java -cp build/tests:libs/zxing-core-3.5.3.jar in.nxprototype.app.QrDecoderTest
"$bt/aapt2" compile --dir app/src/main/res -o build/resources.zip
"$bt/aapt2" link -o build/base.apk -I "$platform" --manifest app/src/main/AndroidManifest.xml -A app/src/main/assets build/resources.zip
find app/src/main/java -name '*.java' -print > build/sources.txt
javac -encoding UTF-8 -source 8 -target 8 -classpath "$platform:libs/zxing-core-3.5.3.jar" -d build/classes @build/sources.txt
jar cf build/classes.jar -C build/classes .
"$bt/d8" --min-api 26 --lib "$platform" --output build/dex build/classes.jar libs/zxing-core-3.5.3.jar
cp build/base.apk build/unsigned.apk
python3 - <<'PY'
import zipfile
from pathlib import Path
with zipfile.ZipFile('build/unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
    for f in Path('build/dex').glob('*.dex'): z.write(f,f.name)
PY
"$bt/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
# A public development key, for this prototype only. Keep it for update compatibility.
if [ ! -f build/keys/prototype.jks ]; then
 keytool -genkeypair -keystore build/keys/prototype.jks -storepass android -keypass android -alias prototype -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=NX Lab Prototype,O=Development,C=IN'
fi
"$bt/apksigner" sign --ks build/keys/prototype.jks --ks-key-alias prototype --ks-pass pass:android --key-pass pass:android --out dist/ranna-0.1.12.apk build/aligned.apk
"$bt/apksigner" verify --verbose dist/ranna-0.1.12.apk
sha256sum dist/ranna-0.1.12.apk > dist/SHA256SUMS.txt
