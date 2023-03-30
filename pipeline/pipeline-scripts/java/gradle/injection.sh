#!/bin/sh

FILE="build.gradle"

if ! grep -q "id ['\"]org.sonarqube['\"] version ['\"].*['\"]" "$FILE"; then
    echo "Applying sonarqube plugin..."
    sed -i.bak '/plugins {/a id '\''org.sonarqube'\'' version '\''4.0.0.2929'\''' "$FILE"
    echo "Plugin applied."
else
    echo "Sonarqube plugin already present."
fi

# Remove backup files
rm -f "$FILE.bak"

echo "Finished updating build.gradle"