#! /bin/sh
# fclogscore
# Analyze FC log
#
# Usage:
#   fclogscore.sh <freecol-log-file>...
#
STATS=${STATS:-stats.awk} # Where is the stats awk script?

statit () {
    statitMEAN=0 ; statitSD=0
    eval `$STATS | sed -n -e 's/^n=[^ ]* *mean=\([^ ]*\) *sd=\(.*\)$/statitMEAN="\1";statitSD="\2"/p' -`
    printf "%7.3f" "$statitMEAN"
}

countinlog () {
    w0=`sed -n -e 's/^.*AI transport.*\(COMPLETED, at wish-target.*fulfills at\)[^]]*, collecting.*$/\1/p' "$1" | sed -e 's/fulfills/#/g' -e 's/[^#]//g' | tr -d '\010' | wc -c`
    w1=`grep -c 'AI wisher.*, COMPLETED' "$1"`
    t0=`sed -n -e 's/^.*AI transport.*, delivering,\(.*COMPLETED.*\), collecting.*$/\1/p' "$1" | sed -e 's/COMPLETED/#/g' -e 's/[^#]//g' | tr -d '\010' | wc -c`

    echo -n 'Count builds: '      ; grep -c 'AI colony builder.*, COMPLETED' "$1"
    echo -n 'Count cashins: '     ; grep -c 'AI treasureTrain.*, COMPLETED' "$1"
    echo -n 'Count defences: '    ; grep -c 'AI defender.*, attacking' "$1"
    echo -n 'Count missions: '    ; grep -c 'AI missionary.*, COMPLETED' "$1"
    echo -n 'Count demands:  '    ; grep -c 'AI native demander.*, COMPLETED' "$1"
    echo -n 'Count gifts:    '    ; grep -c 'AI native gifter.*, COMPLETED' "$1"
    echo -n 'Count pioneerings: ' ; grep -c 'AI pioneer.*, COMPLETED' "$1"
    echo -n 'Count piracies: '    ; grep -c 'AI privateer.*, attacking' "$1"
    echo -n 'Count scoutings: '   ; grep -c 'AI scout.*, COMPLETED' "$1"
    echo -n 'Count seek+dests: '  ; grep -c 'AI seek+destroyer.*, attacking' "$1"
    echo -n 'Count transports: '  ; echo $t0
    echo -n 'Count wishes: '      ; expr $w0 + $w1
    echo -n 'Count Cibola: '      ; grep -c 'is exploring rumour CIBOLA' "$1"
    echo -n 'Count fountain: '    ; grep -c 'is exploring rumour FOUNTAIN' "$1"
    echo -n 'Count colony-fall: ' ; grep -c 'DESTROY_COLONY' "$1"
    echo -n 'Count native-fall: ' ; grep -c 'DESTROY_SETTLEMENT' "$1"
    echo -n 'Count FF: '          ; grep -c 'chose founding father:' "$1"
    echo -n 'Average turn: '  
    avg=`sed -n -e 's/^.*duration = \(.*\)ms$/\1/p' "$1" | statit`
    echo "scale=3; $avg / 1000.0" | bc
#    echo "########## Spanish Succession"
#    sed -n -e '{ s/^\t\(INFO: Spanish succession.*\)/\1/p ; T ; n ; p }' "$1"
}

for f in ${1+"$@"} ; do
    if test -r "$f" ; then
        countinlog "$f"
    else
        echo "can not read: $f" >&2
        exit 1
    fi
done
exit 0
