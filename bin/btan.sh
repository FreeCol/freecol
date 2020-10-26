#! /bin/bash
# btan.sh
# Analyze bigtest run
# Has to be bash now we are using arrays
#
# Usage:
#   btan.sh <tag> < <fc-log-score-output>
#
nations="dutch english french spanish danish portuguese swedish russian"

if test "x$1" = "x" ; then name=Unknown ; else name="$1" ; fi
name=`date +%Y%m%d`-"$name"
if test "x$STATS" = "x" ; then
    dpath=`dirname "$0"`
    if test -x "${dpath}/stats.awk" ; then
        STATS="${dpath}/stats.awk"
    fi
fi
STATS=${STATS:-stats.awk} # Where is the stats awk script?
blockitstate=y


blockit () {
    if test "$blockitstate" = "y" ; then
        echo -n "     "
        blockitstate=n
    else
        echo
        blockitstate=y
    fi
}

section () {
    printf "%-16s" "$1"
}

statit () {
    statitMEAN=0 ; statitSD=0
    eval `$STATS | sed -n -e 's/^n=[^ ]* *mean=\([^ ]*\) *sd=\(.*\)$/statitMEAN="\1";statitSD="\2"/p' -`
    printf "%9.3f ~ %8.3f" "$statitMEAN" "$statitSD"
}

tmp=`mktemp btan.XXXXXXXX`
trap "rm -f '$tmp'" 0
cat - > "$tmp"
runs=`sed -n -e 's/^run: .*colonies: n=\([0-9]*\) mean=\([\.0-9]*\) sd=\([\.0-9]*\), buildings: \([0-9]*\), euroExpense: \([0-9]*\), euroIncome: \([0-9]*\)$/NC="\1";MEAN="\2";SD="\3";NB="\4";EE="\5";EI="\6"/p' "$tmp"`

N=0
for r in $runs ; do N=`expr $N + 1`; done
echo "$name x $N"


section "Builds"
sed -n -e 's/^Count builds: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "Building#"
(for r in $runs ; do eval $r ; echo $NB ;   done) | statit
blockit

section "Colony#"
(for r in $runs ; do eval $r ; echo $NC ;   done) | statit
blockit

section "ColonySize"
(for r in $runs ; do eval $r ; echo $MEAN ; done) | statit
blockit

section "ColonyFalls"
sed -n -e 's/^Count colony-fall: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "SettlementFalls"
sed -n -e 's/^Count native-fall: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "Cashins"
sed -n -e 's/^Count cashins: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "CibolaFinds"
sed -n -e 's/^Count Cibola: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "EuroExpense"
(for r in $runs ; do eval $r ; echo $EE ; done) | statit
blockit

section "EuroIncome"
(for r in $runs ; do eval $r ; echo $EI ; done) | statit
blockit

section "FountainFinds"
sed -n -e 's/^Count fountain: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "Defences"
sed -n -e 's/^Count defences: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "FoundingFathers"
sed -n -e 's/^Count FF: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "Missions"
sed -n -e 's/^Count missions: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "NativeDemands"
sed -n -e 's/^Count demands: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "NativeGifts"
sed -n -e 's/^Count gifts: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "Pioneerings"
sed -n -e 's/^Count pioneerings: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "Piracies"
sed -n -e 's/^Count piracies: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "Scoutings"
sed -n -e 's/^Count scoutings: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "Seek+Destroys"
sed -n -e 's/^Count seek+dests: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "Transports"
sed -n -e 's/^Count transports: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "TurnSpeed"
sed -n -e 's/^Average turn: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section "Wishes"
sed -n -e 's/^Count wishes: *\(.*\)$/\1/p' "$tmp" | statit
blockit

section ""
blockit

for n in $nations ; do
    section "$n"
    sed -n -e 's/^'"$n"' \([0-9]*\)$/\1/p' "$tmp" | statit
    blockit
done

test "$blockitstate" = "y" || echo
