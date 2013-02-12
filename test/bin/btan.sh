#! /bin/sh
# btan.sh
# Analyze bigtest run
#
# Usage:
#   btan.sh <tag> < <fc-log-score-output>
#
if test "x$1" = "x" ; then name=Unknown ; else name="$1" ; fi
name=`date +%Y%m%d`-"$name"
STATS=stats # Where is the stats awk script?
blockitstate=y


blockit () {
    if test "$blockitstate" = "y" ; then
        echo -n "         "
        blockitstate=n
    else
        echo
        blockitstate=y
    fi
}

statit () {
    statitMEAN=0 ; statitSD=0
    eval `$STATS | sed -n -e 's/^n=[^ ]* *mean=\([^ ]*\) *sd=\(.*\)$/statitMEAN="\1";statitSD="\2"/p' -`
    printf "%7.3f ~ %7.3f" "$statitMEAN" "$statitSD"
}

tmp=`mktemp btan.XXXXXXXX`
trap "rm -f '$tmp'" 0
cat - > "$tmp"
runs=`sed -n -e 's/^run: .*colonies: n=\([0-9]*\) mean=\([\.0-9]*\) sd=\([\.0-9]*\)$/NC="\1";MEAN="\2";SD="\3"/p' "$tmp"`

N=0
for r in $runs ; do N=`expr $N + 1`; done
echo "$name x $N"


echo -n "Builds        "
sed -n -e 's/^Count builds: *\(.*\)$/\1/p' "$tmp" | statit
blockit

echo -n "Cashins       "
sed -n -e 's/^Count cashins: *\(.*\)$/\1/p' "$tmp" | statit
blockit

echo -n "Colony#       "
(for r in $runs ; do eval $r ; echo $NC ;   done) | statit
blockit

echo -n "ColonySize    "
(for r in $runs ; do eval $r ; echo $MEAN ; done) | statit
blockit

echo -n "Defences      "
sed -n -e 's/^Count defences: *\(.*\)$/\1/p' "$tmp" | statit
blockit

echo -n "Missions      "
sed -n -e 's/^Count missions: *\(.*\)$/\1/p' "$tmp" | statit
blockit

echo -n "Native Demands"
sed -n -e 's/^Count demands: *\(.*\)$/\1/p' "$tmp" | statit
blockit

echo -n "Native Gifts  "
sed -n -e 's/^Count gifts: *\(.*\)$/\1/p' "$tmp" | statit
blockit

echo -n "Pioneerings   "
sed -n -e 's/^Count pioneerings: *\(.*\)$/\1/p' "$tmp" | statit
blockit

echo -n "Piracies      "
sed -n -e 's/^Count piracies: *\(.*\)$/\1/p' "$tmp" | statit
blockit

echo -n "Scoutings     "
sed -n -e 's/^Count scoutings: *\(.*\)$/\1/p' "$tmp" | statit
blockit

echo -n "Seek+Destroys "
sed -n -e 's/^Count seek+dests: *\(.*\)$/\1/p' "$tmp" | statit
blockit

echo -n "Transports    "
sed -n -e 's/^Count transports: *\(.*\)$/\1/p' "$tmp" | statit
blockit

echo -n "Turn Speed    "
sed -n -e 's/^Average turn: *\(.*\)$/\1/p' "$tmp" | statit
blockit

echo -n "Wishes        "
sed -n -e 's/^Count wishes: *\(.*\)$/\1/p' "$tmp" | statit
blockit

test "$blockitstate" = "y" || echo
