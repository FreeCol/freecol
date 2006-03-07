
package net.sf.freecol.common.model;


import java.util.Map;

import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;


public final class GoodsType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision$";

    private static  int  nextIndex;

    public final  int        index;
    public        String     name;
    public        boolean    isFarmed;
    public boolean improvedByPlowing = false;
    public boolean improvedByRiver = false;
    public boolean improvedByRoad = false;
    public        GoodsType  madeFrom;
    public        GoodsType  makes;



    // ----------------------------------------------------------- constructors

    public GoodsType() {

        index = nextIndex ++;
    }


    // ------------------------------------------------------------ API methods

    public void readFromXmlElement( Node xml, Map goodsTypeByRef ) {

        name = Xml.messageAttribute( xml, "name" );
        isFarmed = Xml.booleanAttribute( xml, "is-farmed" );

        if ( Xml.hasAttribute(xml, "made-from") ) {

            String  madeFromRef = Xml.attribute( xml, "made-from" );
            GoodsType  rawMaterial = (GoodsType) goodsTypeByRef.get( madeFromRef );
            madeFrom = rawMaterial;
            rawMaterial.makes = this;
        }

        if (Xml.hasAttribute(xml, "improved-by-plowing") &&
            Xml.booleanAttribute(xml, "improved-by-plowing")) {
            improvedByPlowing = true;
        }
        if (Xml.hasAttribute(xml, "improved-by-river") &&
            Xml.booleanAttribute(xml, "improved-by-river")) {
            improvedByRiver = true;
        }
        if (Xml.hasAttribute(xml, "improved-by-road") &&
            Xml.booleanAttribute(xml, "improved-by-road")) {
            improvedByRoad = true;
        }
    }


    public boolean isRawMaterial() {

        return makes != null;
    }


    public boolean isRefined() {

        return madeFrom != null;
    }

}
