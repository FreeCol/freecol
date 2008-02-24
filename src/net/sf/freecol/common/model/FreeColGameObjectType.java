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
    };

    protected abstract void readFromXML(XMLStreamReader in, Specification specification) throws XMLStreamException;
    
    /**
     * Use only for debugging purposes! A human-readable and localized name is
     * returned by getName().
     */
    public String toString() {
        return getId();
    }
}
