#! /bin/sh
# Update the copyright year in the boilerplate at the top of the files.
# Only edit java, xml and xsd files.
# Remember to also do FreeColMessages.properties and FreeCol.tex manually.
Y=`date +%Y`
cd ../git || (echo "Run from the git directory" ; exit 1)
exec find ./data ./schema ./src ./packaging ./test \
    -type f \
    -a \( -name \*.java -o -name \*.xml -o -name \*.xsd -o -name copyright -o -name README \) \
    -exec sed -i -e "s/\(Copyright (C) 2...-\)..../\1$Y/" {} \;
