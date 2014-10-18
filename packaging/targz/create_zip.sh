#!/bin/bash

#######################################################################################################
#
# This script generates a .zip file that is fit for distribution to the user.
#
# It will add the following files to the archive:
#   - the jar file
#     (prepare with: ant -Dnodata=true -Dnosound=true -Dnojars=true)
#   - the data files (images, music & sounds)
#   - all files from the freecol/packaging/common directory
#           (these files will be put in the archive under the freecol directory to make them easily
#           accessible by the user)
#
# The only variables that may be configured are FREECOL_CVS_DIR and VERSION.
#
#######################################################################################################

# Location of the freecol CVS directory.
FREECOL_CVS_DIR=/home/`whoami`/freecol
# The current FreeCol version
VERSION="0.11.0"




#######################################################################################################
#
# Start of the script.
#

#
# Step 1: Set some variables and go to the correct directory.
#
OLD_PWD=`pwd`
cd $FREECOL_CVS_DIR/..

#
# Step 2: Copy the common files so that they can be easily added to the correct directory in the archive.
#
commonFiles=`find freecol/packaging/common ! -path '*CVS*' -type f -printf '%f '`
for i in $commonFiles; do
    if [ -f freecol/$i ]; then
        echo "File \"freecol/$i\" already exists. It will be overwritten when making the tar.gz file. Aborting."
        exit 1
    elif [ -d freecol/$i ]; then
        echo "Directory \"freecol/$i\" already exists. It will be overwritten when making the tar.gz file. Aborting."
        exit 1
    fi
done
copyFiles=`find freecol/packaging/common ! -path '*CVS*' -type f -printf '%p '`
cp $copyFiles freecol

#
# Step 3: Find the files that should be added to the archive. These will be passed on to 'zip'.
#
filesToArchive=`\
    find freecol/data ! -path '*CVS*' -type f -printf '%p ';\
    find freecol/FreeCol.jar -type f -printf '%p '`
for i in $commonFiles; do
    filesToArchive="$filesToArchive freecol/$i"
done

#
# Step 4: Create the 'zip' file.
#
zip $OLD_PWD/freecol-$VERSION.zip $filesToArchive

#
# Step 5: Remove the common files that were copied in Step 2.
#
for i in $commonFiles; do
    rm -f freecol/$i
done
