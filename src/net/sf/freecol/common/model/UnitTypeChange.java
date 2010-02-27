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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class UnitTypeChange extends FreeColObject {

    /**
     * Describe newUnitType here.
     */
    private UnitType newUnitType;

    public static enum ChangeType { EDUCATION, NATIVES, EXPERIENCE,
            LOST_CITY, PROMOTION, CREATION, ENTER_COLONY, INDEPENDENCE,
            CLEAR_SKILL, DEMOTION, CAPTURE }

    protected int turnsToLearn = 0;

    protected Set<ChangeType> changeTypes = new HashSet<ChangeType>();

    /**
     * A list of Scopes limiting the applicability of this Feature.
     */
    private List<Scope> scopes = new ArrayList<Scope>();


    public UnitTypeChange() {
        // empty constructor
    }

    /**
     * Creates a new <code>UnitTypeChange</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public UnitTypeChange(XMLStreamReader in) throws XMLStreamException {
        this(in, Specification.getSpecification());
    }

    public UnitTypeChange(XMLStreamReader in, Specification specification) throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE_TAG));
        readAttributes(in, specification);
        readChildren(in, specification);
    }

    public List<Scope> getScopes() {
        return scopes;
    }

    public Set<ChangeType> getChangeTypes() {
        return changeTypes;
    }

    /**
     * Describe <code>asResultOf</code> method here.
     *
     * @param type a <code>ChangeType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean asResultOf(ChangeType type) {
        return changeTypes.contains(type);
    }

    /**
     * Describe <code>appliesTo</code> method here.
     *
     * @param player a <code>Player</code> value
     * @return a <code>boolean</code> value
     */
    public boolean appliesTo(Player player) {
        if (scopes.isEmpty()) {
            return true;
        } else {
            for (Scope scope : scopes) {
                if (scope.appliesTo(player)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Get the <code>TurnsToLearn</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getTurnsToLearn() {
        return turnsToLearn;
    }

    /**
     * Set the <code>TurnsToLearn</code> value.
     *
     * @param newTurnsToLearn The new TurnsToLearn value.
     */
    public final void setTurnsToLearn(final int newTurnsToLearn) {
        this.turnsToLearn = newTurnsToLearn;
    }

    public boolean canBeTaught() {
        return asResultOf(ChangeType.EDUCATION) && turnsToLearn > 0;
    }

    /**
     * Get the <code>NewUnitType</code> value.
     *
     * @return an <code>UnitType</code> value
     */
    public final UnitType getNewUnitType() {
        return newUnitType;
    }

    /**
     * Set the <code>NewUnitType</code> value.
     *
     * @param newNewUnitType The new NewUnitType value.
     */
    public final void setNewUnitType(final UnitType newNewUnitType) {
        this.newUnitType = newNewUnitType;
    }

    protected void readAttributes(XMLStreamReader in, Specification specification) throws XMLStreamException {
        newUnitType = specification.getType(in.getAttributeValue(null, "unit"), UnitType.class);
        turnsToLearn = getAttribute(in, "turnsToLearn", UnitType.UNDEFINED);
        if (getAttribute(in, "learnInSchool", false) || turnsToLearn > 0) {
            changeTypes.add(ChangeType.EDUCATION);
        }
        if (getAttribute(in, "learnFromNatives", false)) {
            changeTypes.add(ChangeType.NATIVES);
        }
        if (getAttribute(in, "learnFromExperience", false)) {
            changeTypes.add(ChangeType.EXPERIENCE);
        }
        if (getAttribute(in, "learnInLostCity", false)) {
            changeTypes.add(ChangeType.LOST_CITY);
        }
        if (getAttribute(in, "promotion", false)) {
            changeTypes.add(ChangeType.PROMOTION);
        }
        if (getAttribute(in, "clearSkill", false)) {
            changeTypes.add(ChangeType.CLEAR_SKILL);
        }
        if (getAttribute(in, "demotion", false)) {
            changeTypes.add(ChangeType.DEMOTION);
        }
        if (getAttribute(in, "capture", false)) {
            changeTypes.add(ChangeType.CAPTURE);
        }
        if (getAttribute(in, "creation", false)) {
            changeTypes.add(ChangeType.CREATION);
        }
        if (getAttribute(in, "enterColony", false)) {
            changeTypes.add(ChangeType.ENTER_COLONY);
        }
        if (getAttribute(in, "independence", false)) {
            changeTypes.add(ChangeType.INDEPENDENCE);
        }
    }

    public void readChildren(XMLStreamReader in, Specification specification) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String nodeName = in.getLocalName();
            if ("scope".equals(nodeName)) {
                scopes.add(new Scope(in));
            }
        }
    }

    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // currently not serialized
    }


}