#!/bin/bash
# Medición de rendimiento de Lumina en un dispositivo real. Uso: tools/perf-phone.sh <serial> <etiqueta>
# Requiere la app instalada con acceso completo y la pantalla desbloqueada. Escribe docs/perf-<etiqueta>.txt
S=${1:?serial}; L=${2:?etiqueta}; OUT=$(dirname "$0")/../docs/perf-$L.txt
A="adb -s $S"
W=$($A shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1); W=${W%x*}; H=$($A shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1); H=${H#*x}
X=$((W/2)); Y1=$((H*85/100)); Y2=$((H*15/100))
{
echo "== $L ($(date +%F\ %T)) =="; $A shell getprop ro.product.model
$A shell input keyevent KEYCODE_WAKEUP; $A shell am force-stop com.adrian.lumina; sleep 1
$A logcat -c
$A shell am start -W -n com.adrian.lumina/.app.MainActivity | grep -E "TotalTime|WaitTime"
sleep 6
$A logcat -d -s LuminaPerf LuminaMedia | grep -E "desde el inicio|MediaStore:" | sed 's/^.*I Lumina/Lumina/'
$A shell dumpsys gfxinfo com.adrian.lumina reset >/dev/null
echo "-- 50 deslizamientos rápidos de punta a punta --"
for i in $(seq 1 50); do $A shell input swipe $X $Y1 $X $Y2 120; sleep 0.25; done
$A shell dumpsys gfxinfo com.adrian.lumina | grep -E "Total frames|Janky frames:|50th percentile|90th percentile|95th percentile|99th percentile|Number Missed|Number Slow UI|Number Slow bitmap|Number Slow issue"
$A shell dumpsys meminfo com.adrian.lumina | grep -E "TOTAL PSS|Java Heap:|Native Heap:|Graphics:" | head -4
echo "-- arrastre del scrubber (carril derecho) --"
$A shell dumpsys gfxinfo com.adrian.lumina reset >/dev/null
for i in 1 2 3; do $A shell input swipe $((W-30)) $((H*20/100)) $((W-30)) $((H*80/100)) 900; $A shell input swipe $((W-30)) $((H*80/100)) $((W-30)) $((H*20/100)) 900; done
$A shell dumpsys gfxinfo com.adrian.lumina | grep -E "Total frames|Janky frames:|50th percentile|90th percentile|99th percentile"
} | tee "$OUT"
