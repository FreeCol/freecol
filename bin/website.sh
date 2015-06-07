#! /bin/sh
# Upload what has changed in the web directory to the web site.
#
if test "x$USERNAME" = "x" ; then
    echo "set USERNAME" >&2
    exit 1
fi
WEBDIR=www.freecol.org

PREV=`git log --pretty=oneline "$WEBDIR" | sed -n -e '2s/^\([^ ]*\).*$/\1/p'`
cd "$WEBDIR"
{
    echo "cd /home/project-web/freecol/htdocs"
    for f in `git diff --name-only "$PREV" HEAD "$WEBDIR" | sed -e "s|^$WEBDIR/||"` ; do
        echo "put $f $f"
    done
} | sftp $USERNAME,freecol@web.sf.net
