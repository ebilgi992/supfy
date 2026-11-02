#!/usr/bin/env bash
# Disposable test device only. Keep permission changes outside the instrumented process.
set -euo pipefail
v5_adb="${SUPFY_ADB:-adb}"
v5_user="${SUPFY_TEST_USER:-0}"
v5_log="${SUPFY_TEST_LOG:-/tmp/supfy-v5-android-tests.log}"
v5_package=com.supfy.app
v5_old="$("$v5_adb" shell appops get --user "$v5_user" "$v5_package" REQUEST_INSTALL_PACKAGES | tr -d '\r')"
v5_mode="$(sed -n 's/^REQUEST_INSTALL_PACKAGES: \([a-z_]*\).*/\1/p' <<< "$v5_old")"
if [[ "$v5_old" == *"No operations."* ]]; then v5_mode=default; fi
case "$v5_mode" in allow|ignore|deny|default|foreground) ;; *) echo 'Cannot determine prior installer permission' >&2; exit 1;; esac
restore() { "$v5_adb" shell appops set --user "$v5_user" "$v5_package" REQUEST_INSTALL_PACKAGES "$v5_mode" >/dev/null; }
trap restore EXIT
"$v5_adb" shell appops set --user "$v5_user" "$v5_package" REQUEST_INSTALL_PACKAGES allow
"$v5_adb" shell am instrument --user "$v5_user" -w -r "$@" "$v5_package.test/androidx.test.runner.AndroidJUnitRunner" | tee "$v5_log"
python3 "$(dirname "$0")/v5_android_results.py" "$v5_log"
