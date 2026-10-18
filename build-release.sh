#!/usr/bin/env bash
set -e

# Change directory to project root
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

# Source environment
if [ -f "$PROJECT_ROOT/env.sh" ]; then
    source "$PROJECT_ROOT/env.sh"
fi

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${CYAN}${BOLD}=== GG Agent: Build Release & Install ===${NC}"

# Check Java
if ! command -v java >/dev/null 2>&1; then
    echo -e "${RED}[Error] Java tidak ditemukan. Jalankan ./init-environment.sh terlebih dahulu.${NC}"
    exit 1
fi

# Check Android SDK
if [ -z "$ANDROID_HOME" ] || [ ! -d "$ANDROID_HOME" ]; then
    echo -e "${RED}[Error] ANDROID_HOME tidak disetel atau direktori tidak ditemukan. Jalankan ./init-environment.sh.${NC}"
    exit 1
fi

# Ensure release keystore exists
KEYSTORE_DIR="$PROJECT_ROOT/app/keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/release.jks"

if [ ! -f "$KEYSTORE_FILE" ]; then
    echo -e "${YELLOW}[Info] Release keystore belum ditemukan. Membuat default keystore di: app/keystore/release.jks...${NC}"
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
    echo -e "${GREEN}[✔] Keystore rilis berhasil dibuat.${NC}"
fi

# Check gradlew
if [ ! -x "./gradlew" ]; then
    chmod +x ./gradlew
fi

echo -e "${CYAN}[1/3] Menjalankan build Gradle Release...${NC}"
./gradlew assembleRelease

APK_PATH="$PROJECT_ROOT/app/build/outputs/apk/release/app-release.apk"
if [ ! -f "$APK_PATH" ]; then
    # In case AGP generates unsigned or universal apk
    ALT_APK=$(find "$PROJECT_ROOT/app/build/outputs/apk/release" -name "*.apk" | head -n 1)
    if [ -n "$ALT_APK" ]; then
        APK_PATH="$ALT_APK"
    else
        echo -e "${RED}[Error] File APK rilis tidak ditemukan di $PROJECT_ROOT/app/build/outputs/apk/release${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}[✔] Build Release sukses!${NC}"
echo -e "APK Path: ${BOLD}$APK_PATH${NC} ($(du -h "$APK_PATH" | cut -f1))"

echo -e "\n${CYAN}[2/3] Memeriksa koneksi ADB...${NC}"
if ! command -v adb >/dev/null 2>&1; then
    echo -e "${YELLOW}[Peringatan] adb tidak ditemukan dalam PATH.${NC}"
    echo -e "File APK dapat diinstal manual: $APK_PATH"
    exit 0
fi

DEVICE_ARG=""
if [ "$1" == "-s" ] && [ -n "$2" ]; then
    DEVICE_ARG="-s $2"
elif [ -n "$1" ] && [[ "$1" != -* ]]; then
    DEVICE_ARG="-s $1"
fi

DEVICES=($(adb devices | grep -v "List of devices" | grep "device$" | cut -f1))
DEVICE_COUNT=${#DEVICES[@]}

if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo -e "${YELLOW}[Info] Tidak ada perangkat Android terhubung via ADB.${NC}"
    echo -e "Pastikan:"
    echo -e "  1. USB Debugging sudah aktif di perangkat Android Anda."
    echo -e "  2. Kabel USB tersambung atau ADB WiFi aktif."
    echo -e "  3. Cek perintah: adb devices"
    echo -e "\nFile APK siap diinstal manual kapan saja:"
    echo -e "  ${BOLD}adb install -r -d $APK_PATH${NC}"
    exit 0
fi

SELECTED_DEVICE=""
if [ -n "$DEVICE_ARG" ]; then
    SELECTED_DEVICE="$DEVICE_ARG"
    echo -e "Target perangkat: ${BOLD}$SELECTED_DEVICE${NC}"
elif [ "$DEVICE_COUNT" -eq 1 ]; then
    SELECTED_DEVICE="-s ${DEVICES[0]}"
    echo -e "Ditemukan 1 perangkat: ${BOLD}${DEVICES[0]}${NC}"
else
    echo -e "Ditemukan $DEVICE_COUNT perangkat terhubung:"
    for i in "${!DEVICES[@]}"; do
        echo -e "  [$i] ${DEVICES[$i]}"
    done
    SELECTED_DEVICE="-s ${DEVICES[0]}"
    echo -e "Menggunakan perangkat default: ${BOLD}${DEVICES[0]}${NC}"
    echo -e "Gunakan argumen untuk memilih: ./build-release.sh -s <device_id>"
fi

echo -e "\n${CYAN}[3/3] Menginstal APK rilis ke perangkat...${NC}"
adb $SELECTED_DEVICE install -r -d "$APK_PATH"

echo -e "\n${CYAN}Membuka aplikasi GG Agent...${NC}"
adb $SELECTED_DEVICE shell am start -n id.gustyxganteng.agent/.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER || true

echo -e "\n${GREEN}${BOLD}=== Instalasi & Peluncuran Rilis Selesai ===${NC}"
echo -e "Monitor log dengan:"
echo -e "  ${BOLD}adb $SELECTED_DEVICE logcat -s GG-Agent:V AndroidRuntime:E *:S${NC}"
