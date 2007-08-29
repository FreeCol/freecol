
package net.sf.freecol.common.model;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.util.Xml;

/**
* Represents one founding father to be contained in a Player object.
* Stateful information is in the Player object.
*/
public class FoundingFather extends FreeColGameObjectType implements Abilities {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    public static final int NONE = -1,
                            ADAM_SMITH = 0,
                            JACOB_FUGGER = 1,
                            PETER_MINUIT = 2,
                            PETER_STUYVESANT = 3, 
                            JAN_DE_WITT = 4,
                            FERDINAND_MAGELLAN = 5,
                            FRANCISCO_DE_CORONADO = 6,
                            HERNANDO_DE_SOTO = 7,
                            HENRY_HUDSON = 8,
                            LA_SALLE = 9,
                            HERNAN_CORTES = 10,
                            GEORGE_WASHINGTON = 11,
                            PAUL_REVERE = 12,
                            FRANCIS_DRAKE = 13,
                            JOHN_PAUL_JONES = 14,
                            THOMAS_JEFFERSON = 15,
                            POCAHONTAS = 16,
                            THOMAS_PAINE = 17,
                            SIMON_BOLIVAR = 18,
                            BENJAMIN_FRANKLIN = 19,//TODO
                            WILLIAM_BREWSTER = 20,
                            WILLIAM_PENN = 21,
                            FATHER_JEAN_DE_BREBEUF = 22,
                            JUAN_DE_SEPULVEDA = 23,
                            BARTOLOME_DE_LAS_CASAS = 24,

                            FATHER_COUNT = 25;

    private int[] weight = new int[4];

    private int type;

    /**
     * Stores the abilities of this Type.
     */
    private HashMap<String, Boolean> abilities = new HashMap<String, Boolean>();    

    public static final int TRADE = 0,
                            EXPLORATION = 1,
                            MILITARY = 2,
                            POLITICAL = 3,
                            RELIGIOUS = 4,
                            TYPE_COUNT = 5;

    /**
     * Creates a new <code>FoundingFather</code> instance.
     *
     * @param newIndex an <code>int</code> value
     */
    public FoundingFather(int newIndex) {
        setIndex(newIndex);
    }

    public String getText() {
        return Messages.message(getID() + ".text");
    }

    public String getBirthAndDeath() {
        return Messages.message(getID() + ".birthAndDeath");
    }

    public int getType() {
        return type;
    }
    
    public String getTypeAsString() {
        return getTypeAsString(type);
    }

    public static String getTypeAsString(int type) {
        switch (type) {
            case TRADE: return Messages.message("foundingFather.trade");
            case EXPLORATION: return Messages.message("foundingFather.exploration");
            case MILITARY: return Messages.message("foundingFather.military");
            case POLITICAL: return Messages.message("foundingFather.political");
            case RELIGIOUS: return Messages.message("foundingFather.religious");
        }
        
        return "";
    }

    /**
     * Get the weight of this FoundingFather. This is used to select a
     * random FoundingFather.
     *
     * @param age an <code>int</code> value
     * @return an <code>int</code> value
     */
    public int getWeight(int age) {
        switch(age) {
        case 1:
            return weight[1];
        case 2:
            return weight[2];
        case 3:
        default:
            return weight[3];
        }
    }
    

    /**
     * Returns true if this FoundingFather has the ability with the given ID.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        return abilities.containsKey(id) && abilities.get(id);
    }

    /**
     * Returns a copy of this FoundingFather's abilities.
     *
     * @return a <code>Map</code> value
     */
    public Map<String, Boolean> getAbilities() {
        return new HashMap<String, Boolean>(abilities);
    }

    /**
     * Sets the ability to newValue;
     *
     * @param id a <code>String</code> value
     * @param newValue a <code>boolean</code> value
     */
    public void setAbility(String id, boolean newValue) {
        abilities.put(id, newValue);
    }


    public void readFromXmlElement(Node xml, Map<String, GoodsType> goodsTypeByRef) {

        setID(Xml.attribute(xml, "id"));
        String typeString = Xml.attribute(xml, "type");
        if ("trade".equals(typeString)) {
            type = TRADE;
        } else if ("exploration".equals(typeString)) {
            type = EXPLORATION;
        } else if ("military".equals(typeString)) {
            type = MILITARY;
        } else if ("political".equals(typeString)) {
            type = POLITICAL;
        } else if ("religious".equals(typeString)) {
            type = RELIGIOUS;
        } else {
            throw new IllegalArgumentException("FoundingFather has unknown type " + typeString);
        }                           

        weight[1] = Xml.intAttribute(xml, "weight1");
        weight[2] = Xml.intAttribute(xml, "weight2");
        weight[3] = Xml.intAttribute(xml, "weight3");

        Xml.Method method = new Xml.Method() {
                public void invokeOn(Node node) {
                    if ("ability".equals(node.getNodeName())) {
                        String abilityId = Xml.attribute(node, "id");
                        boolean value = Xml.booleanAttribute(node, "value");
                        setAbility(abilityId, value);
                    }
                }
            };
        Xml.forEachChild(xml, method);

    }

}
