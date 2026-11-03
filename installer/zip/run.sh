#!/bin/sh
# ======================================================================
#   run.sh
# ======================================================================
#   Purpose:
#   Starts Golf Analyst from the unzipped folder on a Mac or on Linux,
#   with the Java already installed (17 or newer). It tells the program
#   which folder it is in (golfanalyst.home), so your own data goes in
#   Documents/Golf Analyst and never inside this folder.
#
#   Passed in:  nothing (run it with:  sh run.sh)
#   Passed out: nothing (the program's window opens)
# ======================================================================
here=$(cd "$(dirname "$0")" && pwd)
if ! command -v java >/dev/null 2>&1
then
    echo "Java was not found. Golf Analyst needs Java 17 or newer."
    echo "Install it from https://adoptium.net and then run this again."
    exit 1
fi
major=$(java -version 2>&1 | head -n 1 | sed -E 's/[^"]*"([0-9]+).*/\1/')
case "$major" in
    ''|*[!0-9]*)
        ;;
    *)
        if [ "$major" -lt 17 ]
        then
            echo "This Java is version $major. Golf Analyst needs Java 17 or newer (https://adoptium.net)."
            exit 1
        fi
        ;;
esac
exec java "-Dgolfanalyst.home=$here" -Dapple.awt.application.name="Golf Analyst" -jar "$here/golfanalyst.jar"
