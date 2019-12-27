#! /bin/sh
# Upload the website
#
# Default is to upload what changed recently, with -g from a specific commit.
# -a just uploads all of it.
#
set -xv
MODE=prev
while test "x$1" != "x" ; do
    case "x$1" in
    "x-a")
        MODE=all
        ;;
    "x-g")
        shift
        MODE="$1"
        ;;
    *)
        USERNAME="$1"
        ;;
    esac
    shift
done
if test "x$USERNAME" = "x" ; then
    echo "usage: $0 [-a|-g <commit>] <USERNAME>" >&2
    exit 1
fi
WEBDIR=www.freecol.org

if test "x$MODE" = "xall" ; then
    {
        echo "cd /home/project-web/freecol/htdocs"
        find www.freecol.org -type f -print \
            | sed -e 's|^www.freecol.org/\(.*\)$|put \1 \1|'
    } | (cd "$WEBDIR" ; sftp $USERNAME,freecol@web.sf.net)
else
    if test "x$MODE" = "xprev" ; then
        PREV=`git log --pretty=oneline "$WEBDIR" | sed -n -e '2s/^\([^ ]*\).*$/\1/p'`
    else
        PREV="$MODE"
    fi
    {
        echo "cd /home/project-web/freecol/htdocs"
        git diff --name-only "$PREV" HEAD -- "$WEBDIR" | sed -e 's|^[^/]*/\(.*\)$|put \1 \1|p'
    } | (cd "$WEBDIR" ; sftp $USERNAME,freecol@web.sf.net)
fi
