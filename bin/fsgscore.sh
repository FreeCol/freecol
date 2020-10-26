#! /bin/sh
# fsgscore.sh
# Score fsg files
#
# Usage: fsgscore <fsgfile>..."
#
FSGUNPACK=${FSGUNPACK:-fsgunpack.sh}
STATS=${STATS:-stats.awk}
#
tmp=`mktemp -d fsgscore.XXXXXX`
finish () {
    rm -rf "$tmp"
}
trap finish EXIT
#
now=`pwd`
for f in ${1+"$@"} ; do
    case "$f" in
    *.fsg)
        cp "$f" "$tmp"
        cd "$tmp"
        if $FSGUNPACK "$f" ; then
            b=`basename "$f" .fsg`
            colonies=`xml_grep 'colony' "$b/savegame.xml" \
                | sed -n -e 's|^.*<colony .*name="\([^"]*\)".*$|<|p' \
                    -e 's|^.*</colony>.*$|>|p' -e 's|^.*<unit.*$|U|p' \
                | awk '{ if ($1 == "<") n = 0; else if ($1 == "U") n++; else if ($1 == ">") print n; }' \
                | $STATS`
            buildings=`xml_grep 'building' "$b/savegame.xml" | grep -c '<building'`
            echo -n "colonies: $colonies, buildings: $buildings"
            xml_grep 'marketData' "$b/savegame.xml" \
                | sed -e 's/^.*incomeAfterTaxes="\([^"]*\)".*$/\1/' \
                | awk '{ if ($1 < 0) l += $1; if ($1 > 0) u += $1; } END { printf(", euroExpense: %d, euroIncome: %d\n", -l, u); }'
            xml_grep 'player' "$b/savegame.xml" \
            | sed -n -e '/playerType="NATIVE"/d' -e '/playerType="REF"/d' \
                -e '/playerType="native"/d' -e '/playerType="ref"/d' \
                -e '/"model.nation.unknownEnemy"/d' \
                -e 's/^.*<player.*nationI[dD]="model.nation.\([^"]*\)".*score="\([^"]*\)".*$/\1 \2/p'
        fi
        cd "$now"
        ;;
    *)
        echo "not an fsg file: $f" >&2
        exit 1
        ;;
    esac
done

