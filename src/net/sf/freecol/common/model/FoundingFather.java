
package net.sf.freecol.common.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.gui.i18n.Messages;

/**
 * Represents one FoundingFather to be contained in a Player object.
 * The FoundingFather is able to grant new abilities or bonuses to the
 * player, or to cause certain events.
 */
public class FoundingFather extends FreeColGameObjectType implements Abilities, Modifiers {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    /**
     * The probability of this FoundingFather being offered for selection.
     */
    private int[] weight = new int[4];

    /**
     * The type of this FoundingFather. One of the following constants.
     */
    private int type;

    public static final int TRADE = 0,
                            EXPLORATION = 1,
                            MILITARY = 2,
                            POLITICAL = 3,
                            RELIGIOUS = 4,
                            TYPE_COUNT = 5;

    /**
     * Stores the Modifiers of this Type.
     */
    private HashMap<String, Modifier> modifiers = new HashMap<String, Modifier>();

    /**
     * Stores the Events of this Type.
     */
    private HashMap<String, String> events = new HashMap<String, String>();

    /**
     * Stores the IDs of the Nations and NationTypes this
     * FoundingFather is available to.
     */
    private HashSet<String> availableTo = new HashSet<String>();

    /**
     * Creates a new <code>FoundingFather</code> instance.
     *
     * @param newIndex an <code>int</code> value
     */
    public FoundingFather(int newIndex) {
        setIndex(newIndex);
    }

    /**
     * Return the localized text of this FoundingFather.
     *
     * @return a <code>String</code> value
     */
    public String getText() {
        return Messages.message(getID() + ".text");
    }

    /**
     * Return the localized birth and death dates of this FoundingFather.
     *
     * @return a <code>String</code> value
     */
    public String getBirthAndDeath() {
        return Messages.message(getID() + ".birthAndDeath");
    }

    /**
     * Return the type of this FoundingFather.
     *
     * @return an <code>int</code> value
     */
    public int getType() {
        return type;
    }
    
    /**
     * Return the localized type of this FoundingFather.
     *
     * @return a <code>String</code> value
     */
    public String getTypeAsString() {
        return getTypeAsString(type);
    }

    /**
     * Return the localized type of the given FoundingFather.
     *
     * @param type an <code>int</code> value
     * @return a <code>String</code> value
     */
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
     * Returns true if this <code>FoundingFather</code> is available
     * to the Player given.
     *
     * @param player a <code>Player</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isAvailableTo(Player player) {
        return (availableTo.isEmpty() || availableTo.contains(player.getNationID()) ||
                availableTo.contains(player.getNationType().getID()));
    }



    /**
     * Returns true if this FoundingFather has the ability with the given ID.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        return modifiers.containsKey(id) && modifiers.get(id).getBooleanValue();
    }

    /**
     * Sets the ability to newValue;
     *
     * @param id a <code>String</code> value
     * @param newValue a <code>boolean</code> value
     */
    public void setAbility(String id, boolean newValue) {
        if (modifiers.containsKey(id)) {
            modifiers.get(id).setBooleanValue(newValue);
        } else {
            modifiers.put(id, new Modifier(id, newValue, Modifier.BOOLEAN));
        }
    }

    /**
     * Get the <code>Modifier</code> value.
     *
     * @param id a <code>String</code> value
     * @return a <code>Modifier</code> value
     */
    public final Modifier getModifier(String id) {
        return modifiers.get(id);
    }

    /**
     * Set the <code>Modifier</code> value.
     *
     * @param id a <code>String</code> value
     * @param newModifier a <code>Modifier</code> value
     */
    public final void setModifier(String id, final Modifier newModifier) {
        modifiers.put(id, newModifier);
    }

    /**
     * Returns a copy of this FoundingFather's modifiers.
     *
     * @return a <code>Map</code> value
     */
    public Map<String, Modifier> getModifiers() {
        return new HashMap<String, Modifier>(modifiers);
    }

    // TODO: make this unnecessary
    public Map<String, Boolean> getAbilities() {
        return null;
    }

    /**
     * Returns all events.
     *
     * @return a <code>List</code> of Events.
     */
    public Map<String, String> getEvents() {
        return events;
    }

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in, null);
    }

    public void readFromXML(XMLStreamReader in, final Map<String, GoodsType> goodsTypeByRef)
            throws XMLStreamException {
        setID(in.getAttributeValue(null, "id"));
        String typeString = in.getAttributeValue(null, "type");
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
            throw new IllegalArgumentException("FoundingFather " + getID() + " has unknown type " + typeString);
        }                           

        weight[1] = Integer.parseInt(in.getAttributeValue(null, "weight1"));
        weight[2] = Integer.parseInt(in.getAttributeValue(null, "weight2"));
        weight[3] = Integer.parseInt(in.getAttributeValue(null, "weight3"));

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("ability".equals(childName)) {
                Modifier modifier = new Modifier(in);
                setModifier(modifier.getId(), modifier);
                // close this element
            } else if (Modifier.getXMLElementTagName().equals(childName)) {
                Modifier modifier = new Modifier(in);
                setModifier(modifier.getId(), modifier); // close this element
            } else if ("event".equals(childName)) {
                String eventId = in.getAttributeValue(null, "id");
                String value = in.getAttributeValue(null, "value");
                events.put(eventId, value);
                in.nextTag(); // close this element
            } else if ("nation".equals(childName) ||
                       "nation-type".equals(childName)) {
                availableTo.add(in.getAttributeValue(null, "id"));
                in.nextTag();
            } else {
                logger.finest("Parsing of " + childName + " is not implemented yet");
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT ||
                        !in.getLocalName().equals(childName)) {
                    in.nextTag();
                }
            }
        }

    }

}
