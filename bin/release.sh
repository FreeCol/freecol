#! /bin/sh
if test "x$FREECOL_VERSION" = "x" ; then
    echo "set FREECOL_VERSION" >&2
    exit 1
fi
if test "x$USERNAME" = "x" ; then
    echo "set USERNAME" >&2
    exit 1
fi
if test ! -d dist ; then
    echo "ant dist did not succeed" >&2
    exit 1
fi

cd dist
PREFIX=freecol-$FREECOL_VERSION
sftp $USERNAME,freecol@frs.sourceforge.net <<EOF
cd /home/frs/project/f/fr/freecol/freecol
mkdir $PREFIX
cd $PREFIX
put $PREFIX-installer.exe
put $PREFIX-installer.jar
put $PREFIX-mac.tar.bz2
put $PREFIX-src.zip
put $PREFIX.zip
exit
EOF
cd ..

if ant manual devmanual javadoc ; then
    cd doc
    {
        echo "cd /home/project-web/freecol/htdocs"
        echo "put FreeCol.pdf docs/"
        echo "put FreeCol.html docs/"
        echo "put developer.pdf docs/"
        echo "put developer.html docs/"
        echo "put images/* docs/images"
        find javadoc/ -printf "put %p %p\n"
    } | sftp $USERNAME,freecol@web.sf.net
    cd ..
else
    echo "failed to build manual/javadoc" >&2
    exit 1
fi

exit 0
