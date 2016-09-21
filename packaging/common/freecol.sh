#! /bin/sh
# Generic freecol start script.
# Please customize this for your distribution.
# Look at the debian scripts for other ideas, they are clueful.
#
# Things this script needs to do:
# - Find the freecol data directory.
#     It is usually called "data".
#     Try to allow the --freecol-data command line override to work.
#     This script allows a FREECOL_DATA environment variable override,
#     tries the obvious directories, and puts the result in FCDAT.
# - Find the freecol .jar.
#     It is usually called "FreeCol.jar".
#     This script allows a FREECOL_JAR environment variable override,
#     tries the obvious directories, and puts the result in FCJAR.
# - Find the java binary.
#     This script assumes it is "java".
#     If not, fix the last line.
#
BINDIR=`dirname "$0"`
FREECOLDATA="data"
FREECOLJAR="FreeCol.jar"

# Find the data directory, put it in FCDAT.
FCDAT=""
# Is the data directory supplied in the command line args?
for x in $@ ; do
    if test "x$x" = "x--freecol-data" ; then
        FCDAT="DONTSET!"
        break
    fi
done
# - Is there a credible FREECOL_DATA environment variable?
if test "x${FCDAT}" = "x" ; then
    if test "x${FREECOL_DATA}" != "x" -a -d "${FREECOL_DATA}" ; then
        FCDAT="${FREECOL_DATA}"
# - Is it in the current directory?
    elif test -d "${FREECOLDATA}" ; then
        FCDAT="DONTSET!"
# - Is it where this script is?
    elif test -d "${BINDIR}/${FREECOLDATA}" ; then
        FCDAT="${BINDIR}/${FREECOLDATA}"
# - Is it in a likely linux FHS place?
    elif test -d "/usr/share/freecol/data" ; then
        FCDAT="/usr/share/freecol/data"
# - It it in $HOME/freecol?
    elif test "x${HOME}" != "x" -a -d "${HOME}/freecol" \
           -a -d "${HOME}/freecol/${FREECOLDATA}" ; then
        FCDAT="${HOME}/freecol/${FREECOLDATA}"
# - Give up.
    else
        echo "can not find freecol data directory" >&2
        exit 1
    fi
fi

# Find the freecol jar, put it in FCJAR.
FCJAR=""
# - Is there a credible FREECOL_JAR environment variable?
if test "x${FREECOL_JAR}" != "x" -a -r "${FREECOL_JAR}" ; then
    FCJAR="${FREECOL_JAR}"
# - Is it in the current directory?
elif test -r "${FREECOLJAR}" ; then
    FCJAR="${FREECOLJAR}"
# - Is it where this script is?
elif test -r "${BINDIR}/${FREECOLJAR}" ; then
    FCJAR="${BINDIR}/${FREECOLJAR}"
# - Is it in a likely linux FHS place?
elif test -d "/usr/share/java/${FREECOLJAR}" ; then
    FCJAR="/usr/share/java/${FREECOLJAR}"
# Give up.
else
    echo "can not find ${FREECOLJAR}" >&2
    exit 1
fi

# Clean up the data argument and run.
if test "x${FCDAT}" = "xDONTSET!" ; then
    exec java -Xmx512M -cp "${FCJAR}" net.sf.freecol.FreeCol ${1+"$@"}
else
    exec java -Xmx512M -cp "${FCJAR}" net.sf.freecol.FreeCol --freecol-data "${FCDAT}" ${1+"$@"}
fi
