
package net.sf.freecol.common.model;


import java.util.HashSet;
import java.util.Map;

import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;


public final class UnitType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION  = "$Revision$";

    public static final  int  UNDEFINED = Integer.MIN_VALUE;

    public  String     id;
    public  String     name;
    public  int        offence;
    public  int        defence;
    public  int        hammersRequired;
    public  int        toolsRequired;
    public  int        skill;
    public  int        price;
    public  GoodsType  expertProduction;
    public HashSet<String> abilityArray = new HashSet<String>();    


    public void readFromXmlElement( Node xml, Map<String, GoodsType> goodsTypeByRef ) {

        id = Xml.attribute( xml, "name" );
        name = Xml.attribute( xml, "name" );
        offence = Xml.intAttribute( xml, "offence" );
        defence = Xml.intAttribute( xml, "defence" );

        if ( Xml.hasAttribute(xml, "skill") ) {

            skill = Xml.intAttribute( xml, "skill" );
        }
        else {
            skill = UNDEFINED;
        }

        if ( Xml.hasAttribute(xml, "hammers") ) {

            hammersRequired = Xml.intAttribute( xml, "hammers" );
            toolsRequired = Xml.intAttribute( xml, "tools" );
        }
        else {
            hammersRequired = UNDEFINED;
            toolsRequired = UNDEFINED;
        }

        if ( Xml.hasAttribute(xml, "price") ) {

            price = Xml.intAttribute( xml, "price" );
        }
        else {
            price = UNDEFINED;
        }

        if ( Xml.hasAttribute(xml, "expert-production") ) {

            String  goodsTypeRef = Xml.attribute( xml, "expert-production" );
            expertProduction = goodsTypeByRef.get( goodsTypeRef );
        }
        else {
            expertProduction = null;
        }

        String[] array = Xml.attribute(xml, "abilities").split( "," );
        if (array != null)
        {
            for (int i = 0; i < array.length; i++) {
                abilityArray.add(array[i]);
            }
        }

    }


    public boolean hasSkill() {

        return skill != UNDEFINED;
    }


    public boolean canBeBuilt() {

        return hammersRequired != UNDEFINED;
    }


    public boolean hasPrice() {

        return price != UNDEFINED;
    }


    public boolean hasAbility( String abilityName ) {
        return abilityArray.contains(abilityName);
    }

}
