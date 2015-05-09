#! /bin/sh
# Test integrity of freecol save files
#
if test $# -lt 1 ; then
    echo "usage: $0 <freecol-save-file|directory>..." >&2
    exit 1
fi
FCBIN=${FCBIN-freecol}
TMPDIR=${TMPDIR-/tmp}

check () {
    case "$1" in
    *.fsg)
        log="$TMPDIR/fclog-"`basename "$1" .fsg`
        $FCBIN --log-file "$log" --no-sound --splash /dev/null \
               --check-savegame "$1" 1> /dev/null 2>&1
        case $? in
        0) echo "${1}: ok" >&2 ; rm -f "$log" ;;
        1) echo "${1}: game unreadable" >&2 ;;
        2) echo "${1}: game integrity failure" >&2 ;;
        *) echo "${1}: unexpected status $?" >&2 ;;
        esac
        ;;
    *)
        ;;
    esac
}

for d in ${1+"$@"} ; do
    if test -d "$d" ; then
        for f in `find "$d" -name \*.fsg -print` ; do
            check "$f"
        done
    elif test -f "$d" ; then
        check "$d"
    else
        echo "What is this?: $d" >&2
    fi
done
