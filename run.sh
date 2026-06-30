#!/usr/bin/env bash
# Run the PDS portal. Starts from LoginFrame (which forwards into PDSApp dashboards).
set -e
cd "$(dirname "$0")"

CP="lib/flatlaf-3.5.4.jar:lib/flatlaf-extras-3.5.4.jar:lib/jsvg-1.6.1.jar:mysql-connector-j-8.3.0.jar:.:resources"

ENTRY="${1:-LoginFrame}"
echo "▶ Launching $ENTRY ..."
java -cp "$CP" "$ENTRY"


