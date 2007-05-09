
package net.sf.freecol.common.model;


import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;


public final class TileType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision$";

    public  String    id;
    public  String    name;
    public  int       basicMoveCost;
    public  int       defenceBonus;
    public  TileType  whenForested;


    public void readFromXmlElement( Node tileTypeXml ) {

        id = Xml.attribute( tileTypeXml, "name" );
        name = Xml.attribute( tileTypeXml, "name" );
        basicMoveCost = Xml.intAttribute(tileTypeXml, "basic-move-cost");
        defenceBonus = Xml.intAttribute(tileTypeXml, "defence-bonus");

        /* there may be zero or one children of a "tile-type" element.  a child
         * element (if present) defines the forested version of the tile
         */
        Xml.Method method = new Xml.Method() {
            public void invokeOn( Node xml ) {

                whenForested = new TileType();
                whenForested.readFromXmlElement( xml );
            }
        };

        Xml.forEachChild( tileTypeXml, method );
    }

}
