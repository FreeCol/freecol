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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.Specification;

/**
 * Represents one of the European nations present in the game, i.e. both REFs
 * and possible human players.
 */
public class EuropeanNationType extends NationType {


    /**
     * Whether this is an REF Nation.
     */
    private boolean ref;

    /**
     * Stores the starting units of this Nation.
     */
    private List<AbstractUnit> startingUnits = new ArrayList<AbstractUnit>();

    /**
     * Constructor.
     */
    public EuropeanNationType(int index) {
        super(index);
    }

    /**
     * Returns the name of this Nation's Home Port.
     *
     * @return a <code>String</code> value
     */
    public String getEuropeName() {
        return Messages.message(getId() + ".europe");
    }

    /**
     * Returns the name of this Nation's REF.
     *
     * @return a <code>String</code> value
     */
    public String getREFName() {
        return Messages.message(getId() + ".ref");
    }

    /**
     * Get the <code>REF</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isREF() {
        return ref;
    }

    /**
     * Set the <code>REF</code> value.
     *
     * @param newREF The new REF value.
     */
    public final void setREF(final boolean newREF) {
        this.ref = newREF;
    }

    /**
     * Returns true.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isEuropean() {
        return true;
    }

    /**
     * Returns a list of this Nation's starting units.
     *
     * @return a list of this Nation's starting units.
     */
    public List<AbstractUnit> getStartingUnits() {
        return startingUnits;
    }

    public void readFromXML(XMLStreamReader in, Specification specification)
            throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        ref = getAttribute(in, "ref", false);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if (Ability.getXMLElementTagName().equals(childName)) {
                Ability ability = new Ability(in);
                if (ability.getSource() == null) {
                    ability.setSource(getNameKey());
                }
                addAbility(ability);
            } else if (Modifier.getXMLElementTagName().equals(childName)) {
                Modifier modifier = new Modifier(in); // Modifier close the element
                if (modifier.getSource() == null) {
                    modifier.setSource(this.getId());
                }
                addModifier(modifier);
            } else if ("unit".equals(childName)) {
                AbstractUnit unit = new AbstractUnit(in); // AbstractUnit closes element
                startingUnits.add(unit);
            }
        }
    }

}
