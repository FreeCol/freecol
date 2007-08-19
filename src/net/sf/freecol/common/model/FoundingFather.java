
package net.sf.freecol.common.model;

import java.util.Hashtable;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.util.Xml;

/**
* Represents one founding father to be contained in a Player object.
* Stateful information is in the Player object.
*/
public class FoundingFather implements Abilities {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    /**
     * Describe id here.
     */
    private String id;

    private int[] weight = new int[4];

    private int type;

    /**
     * Stores the abilities of this Type.
     */
    private Hashtable<String, Boolean> abilities = new Hashtable<String, Boolean>();    


    // Remember to update the list in "getWeight" when you add an effect to a founding father:
    public static final int NONE = -1,
                            ADAM_SMITH = 0,
                            JACOB_FUGGER = 1,
                            PETER_MINUIT = 2,
                            PETER_STUYVESANT = 3, 
                            JAN_DE_WITT = 4, //TODO
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
                            JUAN_DE_SEPULVEDA = 23,//TODO
                            BARTOLOME_DE_LAS_CASAS = 24,

                            FATHER_COUNT = 25;

    public static final int TRADE = 0,
                            EXPLORATION = 1,
                            MILITARY = 2,
                            POLITICAL = 3,
                            RELIGIOUS = 4,
                            TYPE_COUNT = 5;

    /**
     * Get the <code>Id</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getId() {
        return id;
    }

    /**
     * Set the <code>Id</code> value.
     *
     * @param newId The new Id value.
     */
    public final void setId(final String newId) {
        this.id = newId;
    }

    public static String getName(int foundingFather) {
        return getPrefix(foundingFather) + ".name";
    }


    public static String getDescription(int foundingFather) {
        return getPrefix(foundingFather) + ".description";
    }


    public static String getText(int foundingFather) {
        return getPrefix(foundingFather) + ".text";
    }


    public static String getBirthAndDeath(int foundingFather) {
        return getPrefix(foundingFather) + ".birthAndDeath";
    }


    private static String getPrefix(int foundingFather) {
        switch (foundingFather) {
            case ADAM_SMITH: return "foundingFather.adamSmith";
            case JACOB_FUGGER: return "foundingFather.jacobFugger";
            case PETER_MINUIT: return "foundingFather.peterMinuit";
            case PETER_STUYVESANT: return "foundingFather.peterStuyvesant";
            case JAN_DE_WITT: return "foundingFather.janDeWitt";
            case FERDINAND_MAGELLAN: return "foundingFather.ferdinandMagellan";
            case FRANCISCO_DE_CORONADO: return "foundingFather.franciscoDeCoronado";
            case HERNANDO_DE_SOTO: return "foundingFather.hernandoDeSoto";
            case HENRY_HUDSON: return "foundingFather.henryHudson";
            case LA_SALLE: return "foundingFather.laSalle";
            case HERNAN_CORTES: return "foundingFather.hernanCortes";
            case GEORGE_WASHINGTON: return "foundingFather.georgeWashington";
            case PAUL_REVERE: return "foundingFather.paulRevere";
            case FRANCIS_DRAKE: return "foundingFather.francisDrake";
            case JOHN_PAUL_JONES: return "foundingFather.johnPaulJones";
            case THOMAS_JEFFERSON: return "foundingFather.thomasJefferson";
            case POCAHONTAS: return "foundingFather.pocahontas";
            case THOMAS_PAINE: return "foundingFather.thomasPaine";
            case SIMON_BOLIVAR: return "foundingFather.simonBolivar";
            case BENJAMIN_FRANKLIN: return "foundingFather.benjaminFranklin";
            case WILLIAM_BREWSTER: return "foundingFather.williamBrewster";
            case WILLIAM_PENN: return "foundingFather.williamPenn";
            case FATHER_JEAN_DE_BREBEUF: return "foundingFather.fatherJeanDeBrebeuf";
            case JUAN_DE_SEPULVEDA: return "foundingFather.juanDeSepulveda";
            case BARTOLOME_DE_LAS_CASAS: return "foundingFather.bartolomeDeLasCasas";
            default:
                throw new IllegalArgumentException("FoundingFather has invalid type.");
        }
    }


    public int getType() {
        return type;
    }

    public static int getType(int foundingFather) {
        switch (foundingFather) {
            case ADAM_SMITH: return TRADE;
            case JACOB_FUGGER: return TRADE;
            case PETER_MINUIT: return TRADE;
            case PETER_STUYVESANT: return TRADE;
            case JAN_DE_WITT: return TRADE;
            case FERDINAND_MAGELLAN: return EXPLORATION;
            case FRANCISCO_DE_CORONADO: return EXPLORATION;
            case HERNANDO_DE_SOTO: return EXPLORATION;
            case HENRY_HUDSON: return EXPLORATION;
            case LA_SALLE: return EXPLORATION;
            case HERNAN_CORTES: return MILITARY;
            case GEORGE_WASHINGTON: return MILITARY;
            case PAUL_REVERE: return MILITARY;
            case FRANCIS_DRAKE: return MILITARY;
            case JOHN_PAUL_JONES: return MILITARY;
            case THOMAS_JEFFERSON: return POLITICAL;
            case POCAHONTAS: return POLITICAL;
            case THOMAS_PAINE: return POLITICAL;
            case SIMON_BOLIVAR: return POLITICAL;
            case BENJAMIN_FRANKLIN: return POLITICAL;
            case WILLIAM_BREWSTER: return RELIGIOUS;
            case WILLIAM_PENN: return RELIGIOUS;
            case FATHER_JEAN_DE_BREBEUF: return RELIGIOUS;
            case JUAN_DE_SEPULVEDA: return RELIGIOUS;
            case BARTOLOME_DE_LAS_CASAS: return RELIGIOUS;
            default:
                throw new IllegalArgumentException("FoundingFather has invalid type.");
        }
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


    public static int getWeight(int foundingFather, int age) {
        // This is the list of the founding fathers without effects:
        if (foundingFather == BENJAMIN_FRANKLIN ||
            foundingFather == JUAN_DE_SEPULVEDA) {
            return 0;
        }
        if (age == 1) {
            return getWeight1(foundingFather);
        } else if (age == 2) {
            return getWeight2(foundingFather);
        } else {
            return getWeight3(foundingFather);
        }
    }

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
    
    private static int getWeight1(int foundingFather) {
        switch (foundingFather) {
            case ADAM_SMITH: return 2;
            case JACOB_FUGGER: return 0;
            case PETER_MINUIT: return 9;
            case PETER_STUYVESANT: return 2;
            case JAN_DE_WITT: return 2;
            case FERDINAND_MAGELLAN: return 2;
            case FRANCISCO_DE_CORONADO: return 3;
            case HERNANDO_DE_SOTO: return 5;
            case HENRY_HUDSON: return 10;
            case LA_SALLE: return 7;
            case HERNAN_CORTES: return 6;
            case GEORGE_WASHINGTON: return 0;
            case PAUL_REVERE: return 10;
            case FRANCIS_DRAKE: return 4;
            case JOHN_PAUL_JONES: return 0;
            case THOMAS_JEFFERSON: return 4;
            case POCAHONTAS: return 7;
            case THOMAS_PAINE: return 1;
            case SIMON_BOLIVAR: return 0;
            case BENJAMIN_FRANKLIN: return 5;
            case WILLIAM_BREWSTER: return 7;
            case WILLIAM_PENN: return 8;
            case FATHER_JEAN_DE_BREBEUF: return 6;
            case JUAN_DE_SEPULVEDA: return 3;
            case BARTOLOME_DE_LAS_CASAS: return 0;
            default:
                throw new IllegalArgumentException("FoundingFather has invalid type.");
        }
    }
    
    
    private static int getWeight2(int foundingFather) {
        switch (foundingFather) {
            case ADAM_SMITH: return 8;
            case JACOB_FUGGER: return 5;
            case PETER_MINUIT: return 1;
            case PETER_STUYVESANT: return 4;
            case JAN_DE_WITT: return 6;
            case FERDINAND_MAGELLAN: return 10;
            case FRANCISCO_DE_CORONADO: return 5;
            case HERNANDO_DE_SOTO: return 10;
            case HENRY_HUDSON: return 1;
            case LA_SALLE: return 5;
            case HERNAN_CORTES: return 5;
            case GEORGE_WASHINGTON: return 4;
            case PAUL_REVERE: return 2;
            case FRANCIS_DRAKE: return 8;
            case JOHN_PAUL_JONES: return 6;
            case THOMAS_JEFFERSON: return 5;
            case POCAHONTAS: return 5;
            case THOMAS_PAINE: return 2;
            case SIMON_BOLIVAR: return 4;
            case BENJAMIN_FRANKLIN: return 5;
            case WILLIAM_BREWSTER: return 4;
            case WILLIAM_PENN: return 5;
            case FATHER_JEAN_DE_BREBEUF: return 6;
            case JUAN_DE_SEPULVEDA: return 8;
            case BARTOLOME_DE_LAS_CASAS: return 5;
            default:
                throw new IllegalArgumentException("FoundingFather has invalid type.");
        }
    }
    
    
    private static int getWeight3(int foundingFather) {
        switch (foundingFather) {
            case ADAM_SMITH: return 6;
            case JACOB_FUGGER: return 8;
            case PETER_MINUIT: return 1; // 0
            case PETER_STUYVESANT: return 8;
            case JAN_DE_WITT: return 10;
            case FERDINAND_MAGELLAN: return 10;
            case FRANCISCO_DE_CORONADO: return 7;
            case HERNANDO_DE_SOTO: return 5;
            case HENRY_HUDSON: return 1; // 0
            case LA_SALLE: return 3;
            case HERNAN_CORTES: return 1;
            case GEORGE_WASHINGTON: return 10;
            case PAUL_REVERE: return 1;
            case FRANCIS_DRAKE: return 6;
            case JOHN_PAUL_JONES: return 7;
            case THOMAS_JEFFERSON: return 6;
            case POCAHONTAS: return 3;
            case THOMAS_PAINE: return 8;
            case SIMON_BOLIVAR: return 6;
            case BENJAMIN_FRANKLIN: return 5;
            case WILLIAM_BREWSTER: return 1;
            case WILLIAM_PENN: return 2;
            case FATHER_JEAN_DE_BREBEUF: return 1;
            case JUAN_DE_SEPULVEDA: return 3;
            case BARTOLOME_DE_LAS_CASAS: return 10;
            default:
                throw new IllegalArgumentException("FoundingFather has invalid type.");
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
     * Sets the ability to newValue;
     *
     * @param id a <code>String</code> value
     * @param newValue a <code>boolean</code> value
     */
    public void setAbility(String id, boolean newValue) {
        abilities.put(id, newValue);
    }


    public void readFromXmlElement(Node xml, Map<String, GoodsType> goodsTypeByRef) {

        id = Xml.attribute(xml, "id");
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
