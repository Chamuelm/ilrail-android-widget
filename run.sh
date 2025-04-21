#!/bin/bash

# Check for required environment
if [ -z "$ANDROID_HOME" ]; then
    echo "ANDROID_HOME is not set. Running setup first..."
    ./setup.sh
    source ~/.zshrc
fi

# Verify Gradle wrapper exists
if [ ! -f "./gradlew" ]; then
    echo "Gradle wrapper not found. Running setup first..."
    ./setup.sh
fi

# Verify command line tools exist
if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
    echo "Android command line tools not found. Running setup first..."
    ./setup.sh
fi

# Accept Android SDK licenses if needed
echo "Verifying Android SDK licenses..."
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null 2>&1

# Check if emulator is running
if ! pgrep -f "qemu-system" > /dev/null; then
    # Verify emulator exists
    if ! $ANDROID_HOME/emulator/emulator -list-avds | grep -q "Pixel6_API34"; then
        echo "Emulator not found. Running setup first..."
        ./setup.sh
    fi

    echo "Starting emulator..."
    $ANDROID_HOME/emulator/emulator -avd Pixel6_API34 &
    
    # Wait for emulator to boot
    echo "Waiting for emulator to boot..."
    $ANDROID_HOME/platform-tools/adb wait-for-device
    
    # Additional wait for complete boot
    echo "Waiting for system to complete boot..."
    while [ "$($ANDROID_HOME/platform-tools/adb shell getprop sys.boot_completed 2>/dev/null)" != "1" ]; do
        echo -n "."
        sleep 2
    done
    echo ""
fi

# Build and install the app
echo "Building and installing the app..."
./gradlew installDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "App installed successfully!"
    echo ""
    echo "To add the widget:"
    echo "1. Long press on the home screen"
    echo "2. Select 'Widgets'"
    echo "3. Find 'Israel Rail Widget'"
    echo "4. Drag it to your desired location"
    echo "5. Configure your preferred stations"
    echo ""
else
    echo "Build failed. Please check the error messages above."
fi

# Keep the script running to maintain the emulator
echo "Press Ctrl+C to stop the emulator and exit..."
wait
