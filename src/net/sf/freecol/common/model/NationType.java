
package net.sf.freecol.common.model;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.gui.i18n.Messages;

/**
 * Represents one of the nations present in the game.
 */
public abstract class NationType extends FreeColGameObjectType implements Abilities, Modifiers {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
     * Stores the abilities of this Nation.
     */
    private HashMap<String, Boolean> abilities = new HashMap<String, Boolean>();

    /**
     * Stores the Modifiers of this Nation.
     */
    private HashMap<String, Modifier> modifiers = new HashMap<String, Modifier>();

    /**
     * Sole constructor.
     */
    public NationType(int index) {
        setIndex(index);
    }

    /**
     * Get the <code>RulerName</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getRulerName() {
        return Messages.message(getID() + ".ruler");
    }

    /**
     * Whether this is a EuropeanNation.
     *
     */
    public abstract boolean isEuropean();

    /**
     * Whether this is a EuropeanREFNation.
     *
     */
    public abstract boolean isREF();

    /**
     * Returns true if this Nation has the ability with the given ID.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        return abilities.containsKey(id) && abilities.get(id);
    }

    /**
     * Returns a copy of this Nation's abilities.
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
     * Returns a copy of this Nation's modifiers.
     *
     * @return a <code>Map</code> value
     */
    public Map<String, Modifier> getModifiers() {
        return new HashMap<String, Modifier>(modifiers);
    }


    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in, null);
    }

    public abstract void readFromXML(XMLStreamReader in, final Map<String, UnitType> unitTypeByRef)
        throws XMLStreamException;

}