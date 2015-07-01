#! /bin/sh
# Upload what has changed in the web directory to the web site.
#
if test "x$USERNAME" = "x" ; then
    echo "set USERNAME" >&2
    exit 1
fi
WEBDIR=www.freecol.org

PREV=`git log --pretty=oneline "$WEBDIR" | sed -n -e '2s/^\([^ ]*\).*$/\1/p'`
{
    echo "cd /home/project-web/freecol/htdocs"
    git diff --name-only "$PREV" HEAD -- "$WEBDIR" | sed -e 's|^[^/]*/\(.*\)$|put \1 \1|p'
} | (cd "$WEBDIR" ; sftp $USERNAME,freecol@web.sf.net)
