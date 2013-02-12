#! /bin/sh
# fsgunpack.sh
# Unpack fsg file.
#
# Usage:
#   fsgunpack <fsgfile>...

# Debug infrastructure
name="fsgunpack"
case "$DEBUGFSGUNPACK" in *l*) exec 2> "$HOME/tmp/debug/$name" ;; esac
case "$DEBUGFSGUNPACK" in *v*) set -xv ;; esac

while test "x$1" != "x" ; do
    case "x$1" in
    x*.fsg) ;;
    *) echo "not an fsg file: $1" >&2 ; exit 1 ;;
    esac
    n=`basename "$1" .fsg`
    mkdir "$n" || exit $?
    cp "$1" "$n"
    (cd "$n" ;\
        unzip "$n.fsg" 1> /dev/null ;\
        rm -f "$n.fsg" ;\
        for s in *.xml ; do xml_pp -i "$s" ; done) || exit $?
    shift
done
exit 0
