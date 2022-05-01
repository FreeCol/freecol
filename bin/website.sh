#! /bin/sh
# Upload the website
#
# Former default is to upload what changed recently, with -g from a
# specific commit.  -a just uploads all of it.  But with the jekyll
# stage, we can not reliably tell what has changed, so now defaulting to
# full upload.
#
#set -xv
MODE=all
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
if test ! -d "$WEBDIR" ; then
   echo "run from the top of the source tree, $WEBDIR should be a subdirectory" >&2
   exit 1
fi

# Build the website
(cd "$WEBDIR" ; jekyll build) || exit $?

if test "x$MODE" = "xall" ; then
    {
        echo "cd /home/project-web/freecol/htdocs"
        find "$WEBDIR/_site" -type f -print \
            | sed -n -e "s|^$WEBDIR/_site/"'\(.*\)$|put \1 \1|p'
    } | (cd "$WEBDIR/_site" ; sftp $USERNAME,freecol@web.sf.net)
else
    echo "incremental mode disabled" >&2
    exit 1
    
    if test "x$MODE" = "xprev" ; then
        PREV=`git log --pretty=oneline "$WEBDIR" | sed -n -e '2s/^\([^ ]*\).*$/\1/p'`
    else
        PREV="$MODE"
    fi
    {
        echo "cd /home/project-web/freecol/htdocs"
        git diff --name-only "$PREV" HEAD -- "$WEBDIR" | sed -n -e 's|^[^/]*/\(.*\)$|put \1 \1|p'
    } | cat #(cd "$WEBDIR" ; sftp $USERNAME,freecol@web.sf.net)
fi
