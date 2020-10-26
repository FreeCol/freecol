#! /bin/sh
# Update the copyright year in the boilerplate at the top of the files.
#
test -r "src/net/sf/freecol/FreeCol.java" \
    || (echo "Run from a FreeCol git directory" ; exit 1)

Y=`date +%Y`
sed -i -e "s/\(copyright 2...--\)2.../\1$Y/" doc/FreeCol.tex
find ./data ./schema ./src ./packaging ./test \
    -type f \
    -a \( -name \*.java -o -name \*.xml -o -name \*.xsd -o -name copyright \
        -o -name README -o -name FreeColMessages.properties \) \
    -exec sed -i -e "s/\(Copyright (C) 2...-\)2...\( *The FreeCol Team\)/\1$Y\2/" {} \;
exec find ./www.freecol.org -type f -a -name \*.html \
    -exec sed -i -e "s/\(&copy; 2...-\)2...\( *FreeCol\)/\1$Y\2/" {} \;
