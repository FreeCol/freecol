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
    echo -n 'Count builds: '      ; grep -c 'AI colony builder completed' "$1"
    echo -n 'Count cashins: '     ; grep -c 'AI treasureTrain completed' "$1"
    echo -n 'Count defences: '    ; grep -c 'AI defender completed' "$1"
    echo -n 'Count missions: '    ; grep -c 'AI missionary completed' "$1"
    echo -n 'Count demands:  '    ; grep -c 'AI native demander completed' "$1"
    echo -n 'Count gifts:    '    ; grep -c 'AI native gifter completed' "$1"
    echo -n 'Count pioneerings: ' ; grep -c 'AI pioneer completed' "$1"
    echo -n 'Count piracies: '    ; grep -c 'AI privateer completed' "$1"
    echo -n 'Count scoutings: '   ; grep -c 'AI scout completed' "$1"
    echo -n 'Count seek+dests: '  ; grep -c 'AI seek+destroyer attacking' "$1"
    echo -n 'Count transports: '  ; grep -c 'AI transport completed' "$1"
    echo -n 'Count wishes: '      ; grep -c 'AI wisher completed' "$1"
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
