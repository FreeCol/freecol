
package net.sf.freecol.common.model;


import java.util.Map;

import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;


public final class GoodsType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision$";

    public static final  int IMPROVED_BY_NOTHING = 0;
    public static final  int IMPROVED_BY_PLOWING = 1;
    public static final  int IMPROVED_BY_TRANSPORT = 2;

    private static  int  nextIndex;

    public final  int        index;
    public        String     name;
    public        boolean    isFarmed;
    public        GoodsType  madeFrom;
    public        GoodsType  makes;
    public        int        improvedBy = IMPROVED_BY_NOTHING;


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

        if ( Xml.hasAttribute(xml, "improved-by") ) {
            String improved = Xml.attribute(xml, "improved-by");
            if (improved.equals("plowing")) {
                improvedBy = IMPROVED_BY_PLOWING;
            } else if (improved.equals("transport")) {
                improvedBy = IMPROVED_BY_TRANSPORT;
            }
        }

    }


    public boolean isRawMaterial() {

        return makes != null;
    }


    public boolean isRefined() {

        return madeFrom != null;
    }

}
