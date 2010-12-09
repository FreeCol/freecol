#!/bin/sh
# simple script used to run FreeCol in Debian
# copied from the Debian package argouml

# The first existing directory is used for JAVA_HOME (if JAVA_HOME is not
# defined in $DEFAULT)
JDK_DIRS="$JAVA_HOME /usr/lib/j2se/1.4 /usr/lib/j2sdk1.4 /usr/local/lib/jdk /usr/local/lib/j2sdk1.4 /usr/local/lib/j2se/1.4"

# Look for the right JVM to use
for jdir in $JDK_DIRS; do
	if [ -r "$jdir/bin/java" -a -z "${JAVA_HOME}" ]; then
		JAVA_HOME="$jdir"
	fi
done
export JAVA_HOME

if [ "$JAVA_HOME" ] ; then
  if [ -z "$JAVACMD" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
  fi

  FOUND="false"
  for X in $@; do
      if [ "$X" == "--freecol-data" ]; then
          FOUND="true"
      fi
  done

  if [ "$FOUND" == "true" ]; then
      $JAVACMD -cp /usr/share/java/freecol.jar net.sf.freecol.FreeCol "$@"
  else
      $JAVACMD -cp /usr/share/java/freecol.jar net.sf.freecol.FreeCol "$@" --freecol-data /usr/share/freecol/data
  fi

else
  echo "No JVM found to run FreeCol."
  echo "Please install a JVM (>= 1.4) to run FreeCol or "
  echo "set JAVA_HOME if it's not a JVM from a Debian Package."
  exit 1
fi
