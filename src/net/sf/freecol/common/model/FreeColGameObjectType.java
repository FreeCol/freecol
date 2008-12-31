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

import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.Specification;
import net.sf.freecol.client.gui.i18n.Messages;

public abstract class FreeColGameObjectType extends FreeColObject {

    private int index;

    /**
     * Describe featureContainer here.
     */
    protected FeatureContainer featureContainer = new FeatureContainer();

    /**
     * Get the <code>FeatureContainer</code> value.
     *
     * @return a <code>FeatureContainer</code> value
     */
    public final FeatureContainer getFeatureContainer() {
        return featureContainer;
    }

    /**
     * Set the <code>FeatureContainer</code> value.
     *
     * @param newFeatureContainer The new FeatureContainer value.
     */
    public final void setFeatureContainer(final FeatureContainer newFeatureContainer) {
        this.featureContainer = newFeatureContainer;
    }

    protected final void setIndex(final int index) {
        this.index = index;
    }

    public final int getIndex() {
        return index;
    }

    public final String getNameKey() {
        return getId() + ".name";
    }

    public final String getName() {
        return Messages.message(getNameKey());
    }

    public final String getDescription() {
        return Messages.message(getId() + ".description");
    }

    public boolean hasAbility(String id) {
        return featureContainer.hasAbility(id);
    }

    public boolean hasAbility(String id, FreeColGameObjectType type) {
        return featureContainer.hasAbility(id, type);
    }

    public void addAbility(Ability ability) {
        featureContainer.addAbility(ability);
    }

    public void addModifier(Modifier modifier) {
        featureContainer.addModifier(modifier);
    }

    public Set<Modifier> getModifierSet(String id) {
        return featureContainer.getModifierSet(id);
    }

    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // currently, FreeColGameObjectTypes are not serialized
    }

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        throw new UnsupportedOperationException("Call 'readFromXML' instead.");
    }

    public void readFromXML(XMLStreamReader in, Specification specification) throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE_TAG));
        readAttributes(in, specification);
        readChildren(in, specification);
    }

    // TODO: make this abstract
    protected void readAttributes(XMLStreamReader in, Specification specification) throws XMLStreamException {}

    public void readChildren(XMLStreamReader in, Specification specification) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            readChild(in, specification);
        }
    }
    
    protected FreeColObject readChild(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        String childName = in.getLocalName();
        if (Ability.getXMLElementTagName().equals(childName)) {
            Ability ability = new Ability(in);
            if (ability.getSource() == null) {
                ability.setSource(this);
            }
            addAbility(ability); // Ability close the element
            specification.addAbility(ability);
            return ability;
        } else if (Modifier.getXMLElementTagName().equals(childName)) {
            Modifier modifier = new Modifier(in);
            if (modifier.getSource() == null) {
                modifier.setSource(this);
            }
            addModifier(modifier); // Modifier close the element
            specification.addModifier(modifier);
            return modifier;
        } else {
            logger.warning("Parsing of " + childName + " is not implemented yet");
            while (in.nextTag() != XMLStreamConstants.END_ELEMENT ||
                   !in.getLocalName().equals(childName)) {
                in.nextTag();
            }
            return null;
        }
    }
    
    /**
     * Use only for debugging purposes! A human-readable and localized name is
     * returned by getName().
     */
    public String toString() {
        return getId();
    }
}
