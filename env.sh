#!/usr/bin/env bash

# Resolve JDK 17+
if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
    if [ -x "$HOME/.jdks/temurin-17.0.12/bin/java" ]; then
        export JAVA_HOME="$HOME/.jdks/temurin-17.0.12"
    elif [ -d "$HOME/.jdks" ] && compgen -G "$HOME/.jdks/*" > /dev/null; then
        export JAVA_HOME="$(ls -d $HOME/.jdks/* | head -n 1)"
    elif [ -x "/usr/lib/jvm/java-17-openjdk-amd64/bin/java" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    elif command -v java >/dev/null 2>&1; then
        _java_bin="$(readlink -f "$(command -v java)")"
        export JAVA_HOME="$(dirname "$(dirname "$_java_bin")")"
    fi
fi

# Resolve ANDROID_HOME / ANDROID_SDK_ROOT
if [ -z "$ANDROID_HOME" ] || [ ! -d "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    elif [ -d "/usr/lib/android-sdk" ]; then
        export ANDROID_HOME="/usr/lib/android-sdk"
    fi
fi
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"

# Export to PATH
if [ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME/bin" ]; then
    case ":$PATH:" in
        *":$JAVA_HOME/bin:"*) ;;
        *) export PATH="$JAVA_HOME/bin:$PATH" ;;
    esac
fi

if [ -n "$ANDROID_HOME" ]; then
    [ -d "$ANDROID_HOME/cmdline-tools/latest/bin" ] && export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
    [ -d "$ANDROID_HOME/platform-tools" ] && export PATH="$ANDROID_HOME/platform-tools:$PATH"
fi
