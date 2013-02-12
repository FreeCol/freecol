#! /bin/sh
# fsgpack.sh
# Pack fsg files.
#
# Usage:
#   fsgpack <fsg-file-directory>...

for f in ${1+"$@"} ; do
    if test -d "$f" -a -r "${f}/savegame.xml" ; then
        (cd "$f" ; jar -cMf "../${f}.fsg" *) || exit $?
    else
        echo "Not a freecol save directory: $f" >&2
        exit 1
    fi
done
exit 0

