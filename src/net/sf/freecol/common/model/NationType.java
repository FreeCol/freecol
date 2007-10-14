/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.common.model;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Represents one of the nations present in the game.
 */
public abstract class NationType extends FreeColGameObjectType implements Abilities, Modifiers {


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
     * Whether this is a EuropeanNation, i.e. a player or a REF.
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


    public String toString() {
        return getName();
    }

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in, null);
    }

    public abstract void readFromXML(XMLStreamReader in, final Map<String, UnitType> unitTypeByRef)
        throws XMLStreamException;

}
