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
#
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
            echo "colonies: $colonies"
        fi
        cd "$now"
        ;;
    *)
        echo "not an fsg file: $f" >&2
        exit 1
        ;;
    esac
done

