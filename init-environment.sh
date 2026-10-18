#!/usr/bin/env bash
set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

# Text Styling
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${CYAN}${BOLD}======================================================${NC}"
echo -e "${CYAN}${BOLD}  Inisialisasi Lingkungan Android GG Agent (CLI Mode) ${NC}"
echo -e "${CYAN}${BOLD}======================================================${NC}"

# 1. Konfigurasi Direktori
JDK_DIR="$HOME/.jdks/temurin-17.0.12"
SDK_DIR="$HOME/Android/Sdk"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
JDK_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.12%2B7/OpenJDK17U-jdk_x64_linux_hotspot_17.0.12_7.tar.gz"

mkdir -p "$HOME/.jdks" "$SDK_DIR"

# 2. Setup JDK 17
echo -e "\n${CYAN}[1/6] Memeriksa instalasi Java / JDK 17...${NC}"
NEED_JDK=false
if command -v java >/dev/null 2>&1; then
    JAVA_VER=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d. -f1)
    if [ "$JAVA_VER" -lt 17 ] 2>/dev/null; then
        echo -e "${YELLOW}Java yang terpasang versi $JAVA_VER (< 17). Membutuhkan JDK 17+.${NC}"
        NEED_JDK=true
    else
        echo -e "${GREEN}[✔] Java yang terpasang memenuhi syarat: $(java -version 2>&1 | head -n 1)${NC}"
        export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
    fi
else
    NEED_JDK=true
fi

if [ "$NEED_JDK" = true ]; then
    if [ -x "$JDK_DIR/bin/java" ]; then
        echo -e "${GREEN}[✔] Temurin JDK 17 ditemukan di $JDK_DIR${NC}"
        export JAVA_HOME="$JDK_DIR"
    else
        echo -e "${YELLOW}Mengunduh Eclipse Temurin OpenJDK 17...${NC}"
        TMP_JDK="/tmp/temurin-17-$$.tar.gz"
        curl -L -o "$TMP_JDK" "$JDK_URL"
        mkdir -p "$JDK_DIR"
        tar -xzf "$TMP_JDK" -C "$JDK_DIR" --strip-components=1
        rm -f "$TMP_JDK"
        export JAVA_HOME="$JDK_DIR"
        echo -e "${GREEN}[✔] JDK 17 berhasil dipasang di $JDK_DIR${NC}"
    fi
fi
export PATH="$JAVA_HOME/bin:$PATH"

# 3. Setup Android Commandline Tools
echo -e "\n${CYAN}[2/6] Memeriksa Android SDK Commandline Tools...${NC}"
SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
    echo -e "${YELLOW}Mengunduh Android Commandline Tools...${NC}"
    TMP_CMDLINE="/tmp/cmdline-tools-$$.zip"
    TMP_EXTRACT="/tmp/cmdline-tools-extracted-$$"
    curl -L -o "$TMP_CMDLINE" "$CMDLINE_TOOLS_URL"
    mkdir -p "$TMP_EXTRACT"
    unzip -q "$TMP_CMDLINE" -d "$TMP_EXTRACT"
    mkdir -p "$SDK_DIR/cmdline-tools"
    rm -rf "$SDK_DIR/cmdline-tools/latest"
    mv "$TMP_EXTRACT/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
    rm -rf "$TMP_CMDLINE" "$TMP_EXTRACT"
    echo -e "${GREEN}[✔] Android Commandline Tools terpasang.${NC}"
else
    echo -e "${GREEN}[✔] Android Commandline Tools sudah tersedia.${NC}"
fi

export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_ROOT="$SDK_DIR"
export PATH="$SDK_DIR/cmdline-tools/latest/bin:$SDK_DIR/platform-tools:$PATH"

# 4. Accept Licenses & Install SDK Components
echo -e "\n${CYAN}[3/6] Memeriksa Android SDK Licenses & Komponen (API 35)...${NC}"
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true

echo -e "Memverifikasi paket SDK: platforms;android-35 & build-tools;35.0.0..."
"$SDKMANAGER" "platform-tools" "platforms;android-35" "build-tools;35.0.0" >/dev/null 2>&1
echo -e "${GREEN}[✔] Komponen Android SDK 35 lengkap.${NC}"

# 5. Konfigurasi local.properties
echo -e "\n${CYAN}[4/6] Memperbarui local.properties...${NC}"
LOCAL_PROP="$PROJECT_ROOT/local.properties"
if [ -f "$LOCAL_PROP" ]; then
    # Hapus baris sdk.dir lama lalu tambahkan yang baru
    sed -i '/^sdk\.dir=/d' "$LOCAL_PROP"
    echo -e "sdk.dir=$SDK_DIR\n$(cat "$LOCAL_PROP")" > "$LOCAL_PROP"
else
    cat <<EOF > "$LOCAL_PROP"
sdk.dir=$SDK_DIR

AGNES_BASE_URL=https://apihub.agnes-ai.com/v1
AGNES_API_KEY=
TAVILY_API_KEY=
EOF
fi
echo -e "${GREEN}[✔] local.properties diperbarui: sdk.dir=$SDK_DIR${NC}"

# Bersihkan override aapt2 Termux lama dari gradle.properties jika ada
if [ -f "$PROJECT_ROOT/gradle.properties" ]; then
    sed -i '/^android\.aapt2FromMavenOverride=/d' "$PROJECT_ROOT/gradle.properties"
fi

# 6. Setup Release Keystore
echo -e "\n${CYAN}[5/6] Memeriksa Release Keystore...${NC}"
KEYSTORE_DIR="$PROJECT_ROOT/app/keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/release.jks"
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo -e "Membuat release keystore baru di app/keystore/release.jks..."
    mkdir -p "$KEYSTORE_DIR"
    keytool -genkeypair -v \
        -keystore "$KEYSTORE_FILE" \
        -alias "ggagent" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass "ggagent123" \
        -keypass "ggagent123" \
        -dname "CN=GG Agent, OU=Mobile, O=GustyxPower, L=Jakarta, ST=DKI, C=ID"
    echo -e "${GREEN}[✔] Release keystore berhasil dibuat.${NC}"
else
    echo -e "${GREEN}[✔] Release keystore sudah ada.${NC}"
fi

# 7. Set executable permissions & Verifikasi Gradle
echo -e "\n${CYAN}[6/6] Mempersiapkan Gradle Wrapper & Perizinan Shell...${NC}"
chmod +x "$PROJECT_ROOT/gradlew"
chmod +x "$PROJECT_ROOT/env.sh"
chmod +x "$PROJECT_ROOT/build-debug.sh"
chmod +x "$PROJECT_ROOT/build-release.sh"
chmod +x "$PROJECT_ROOT/init-environment.sh"

echo -e "Menguji Gradle wrapper..."
./gradlew --version

echo -e "\n${GREEN}${BOLD}======================================================${NC}"
echo -e "${GREEN}${BOLD}  Inisialisasi Projek Berhasil & Siap Digunakan!      ${NC}"
echo -e "${GREEN}${BOLD}======================================================${NC}"
echo -e "Perintah yang dapat digunakan:"
echo -e "  - Build Debug & Install:   ${BOLD}./build-debug.sh${NC}"
echo -e "  - Build Release & Install: ${BOLD}./build-release.sh${NC}"
echo -e "  - Muat Environment CLI:    ${BOLD}source env.sh${NC}"
