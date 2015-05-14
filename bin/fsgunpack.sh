#! /bin/sh
# fsgunpack.sh
# Unpack fsg file.
#
# Usage:
#   fsgunpack <fsgfile>...

for f in ${1+"$@"} ; do
    case "x$f" in
    x*.fsg) ;;
    *) echo "not an fsg file: $f" >&2 ; exit 1 ;;
    esac
    n=`basename "$f" .fsg`
    mkdir "$n" || exit $?
    cp "$f" "$n"
    (cd "$n" ;\
        unzip "$n.fsg" 1> /dev/null ;\
        rm -f "$n.fsg" ;\
        for s in *.xml ; do xml_pp -i "$s" 2> /dev/null ; done) || exit $?
done
exit 0
