#!/usr/bin/env bash
# Compile all .java files with FlatLaf + MySQL connector on the classpath.
set -e
cd "$(dirname "$0")"

CP="lib/flatlaf-3.5.4.jar:lib/flatlaf-extras-3.5.4.jar:lib/jsvg-1.6.1.jar:mysql-connector-j-8.3.0.jar:."

echo "▶ Compiling Java sources (JDK 21)..."
javac -cp "$CP" -d . *.java
echo "✓ Build complete."
