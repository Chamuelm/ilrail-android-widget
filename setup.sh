#!/bin/bash

# Check if Android Studio is installed
if ! command -v "/Applications/Android Studio.app/Contents/MacOS/studio" &> /dev/null; then
    echo "Installing Android Studio..."
    brew install --cask android-studio
    echo "Please launch Android Studio and complete the initial setup, then run this script again."
    exit 1
fi

# Set up ANDROID_HOME if not set
if [ -z "$ANDROID_HOME" ]; then
    echo "export ANDROID_HOME=~/Library/Android/sdk" >> ~/.zshrc
    echo "export PATH=\$PATH:\$ANDROID_HOME/tools:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/emulator:\$ANDROID_HOME/cmdline-tools/latest/bin" >> ~/.zshrc
    source ~/.zshrc
fi

# Install command line tools if needed
if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
    echo "Installing Android command line tools..."
    CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-mac-9477386_latest.zip"
    TEMP_DIR=$(mktemp -d)
    curl -o "$TEMP_DIR/cmdline-tools.zip" "$CMDLINE_TOOLS_URL"
    unzip -q "$TEMP_DIR/cmdline-tools.zip" -d "$TEMP_DIR"
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    mv "$TEMP_DIR/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    rm -rf "$TEMP_DIR"
fi

# Check if Gradle is installed, if not install it
if ! command -v gradle &> /dev/null; then
    echo "Installing Gradle..."
    brew install gradle
fi

# Create Gradle wrapper with specific version
echo "Creating Gradle wrapper..."
gradle wrapper --gradle-version 8.1.0 --distribution-type bin --warning-mode all

# Make gradlew executable
chmod +x gradlew

# Accept Android licenses
echo "Accepting Android SDK licenses..."
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses

# Install required SDK components
echo "Installing required SDK components..."
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "system-images;android-34;google_apis;arm64-v8a"

# Create Pixel 6 emulator if it doesn't exist
if ! $ANDROID_HOME/emulator/emulator -list-avds | grep -q "Pixel6_API34"; then
    echo "Creating Pixel 6 emulator..."
    echo "no" | $ANDROID_HOME/cmdline-tools/latest/bin/avdmanager create avd \
        -n Pixel6_API34 \
        -k "system-images;android-34;google_apis;arm64-v8a" \
        -d "pixel_6"
fi

echo "Setup complete! You can now run './run.sh' to build and deploy the app."
echo "Note: If you encounter any Gradle-related issues, try running: './gradlew wrapper --gradle-version 8.1.0 --distribution-type bin'"
