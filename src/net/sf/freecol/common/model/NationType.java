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
public abstract class NationType extends FreeColGameObjectType implements Features {

    /**
     * Contains the abilities and modifiers of this type.
     */
    private FeatureContainer featureContainer = new FeatureContainer();

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
     * Returns true if the Object has the ability identified by
     * <code>id</code>.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        return featureContainer.hasAbility(id);
    }

    /**
     * Returns the Modifier identified by <code>id</code>.
     *
     * @param id a <code>String</code> value
     * @return a <code>Modifier</code> value
     */
    public Modifier getModifier(String id) {
        return featureContainer.getModifier(id);
    }

    /**
     * Add the given Feature to the Features Map. If the Feature given
     * can not be combined with a Feature with the same ID already
     * present, the old Feature will be replaced.
     *
     * @param feature a <code>Feature</code> value
     */
    public void addFeature(Feature feature) {
        featureContainer.addFeature(feature);
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
