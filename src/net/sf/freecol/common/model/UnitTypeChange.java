/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class UnitTypeChange extends FreeColObject {

    /**
     * Describe newUnitType here.
     */
    private UnitType newUnitType;

    public static enum ChangeType {
        EDUCATION,
        NATIVES,
        EXPERIENCE,
        LOST_CITY,
        PROMOTION,
        CREATION,
        ENTER_COLONY,
        INDEPENDENCE,
        CLEAR_SKILL,
        DEMOTION,
        CAPTURE,
        UNDEAD
    }

    public static final Map<ChangeType, String> tags =
        new EnumMap<ChangeType, String>(ChangeType.class);

    static {
        tags.put(ChangeType.EDUCATION, "learnInSchool");
        tags.put(ChangeType.NATIVES, "learnFromNatives");
        tags.put(ChangeType.EXPERIENCE, "learnFromExperience");
        tags.put(ChangeType.LOST_CITY, "learnInLostCity");
        tags.put(ChangeType.PROMOTION, "promotion");
        tags.put(ChangeType.CLEAR_SKILL, "clearSkill");
        tags.put(ChangeType.DEMOTION, "demotion");
        tags.put(ChangeType.CAPTURE, "capture");
        tags.put(ChangeType.CREATION, "creation");
        tags.put(ChangeType.ENTER_COLONY, "enterColony");
        tags.put(ChangeType.INDEPENDENCE, "independence");
        tags.put(ChangeType.UNDEAD, "undead");
    }

    protected int turnsToLearn = 0;

    protected Map<ChangeType, Integer> changeTypes =
        new EnumMap<ChangeType, Integer>(ChangeType.class);

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
     * @param specification a <code>Specification</code> value
     * @exception XMLStreamException if an error occurs
     */
    public UnitTypeChange(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE_TAG));
        setSpecification(specification);
        readFromXML(in);
    }

    /**
     * Returns the probability of a change taking place (defaults to
     * zero). At the moment, this probability only applies to the
     * ChangeTypes EXPERIENCE and PROMOTION.
     *
     * @param type a <code>ChangeType</code> value
     * @return an <code>int</code> value
     */
    public final int getProbability(ChangeType type) {
        Integer result = changeTypes.get(type);
        return (result == null) ? 0 : result;
    }

    public List<Scope> getScopes() {
        return scopes;
    }

    public Map<ChangeType, Integer> getChangeTypes() {
        return changeTypes;
    }

    /**
     * Describe <code>asResultOf</code> method here.
     *
     * @param type a <code>ChangeType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean asResultOf(ChangeType type) {
        return changeTypes.containsKey(type)
            && changeTypes.get(type) > 0;
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

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        if (newUnitType != null) {
            out.writeAttribute("unit", newUnitType.getId());
        }
        if (turnsToLearn != UNDEFINED) {
            out.writeAttribute("turnsToLearn", Integer.toString(turnsToLearn));
        }
        for (Map.Entry<ChangeType, Integer> entry : changeTypes.entrySet()) {
            out.writeAttribute(tags.get(entry.getKey()),
                entry.getValue().toString());
        }
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The XML input stream.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        String newTypeId = in.getAttributeValue(null, "unit");
        if (newTypeId == null) {
            newUnitType = null;
        } else {
            newUnitType = getSpecification().getType(newTypeId, UnitType.class);
            turnsToLearn = getAttribute(in, "turnsToLearn", UNDEFINED);
            if (turnsToLearn > 0) {
                changeTypes.put(ChangeType.EDUCATION, 100);
            }
            // @compat 0.9.x
            for (ChangeType type : ChangeType.values()) {
                String value = in.getAttributeValue(null, tags.get(type));
                if (value != null) {
                    if(value.equalsIgnoreCase("false")) {
                        changeTypes.put(type, 0);
                    } else if (value.equalsIgnoreCase("true")) {
                        changeTypes.put(type, 100);
                    } else {
                        changeTypes.put(type, Math.max(0,
                                Math.min(100, new Integer(value))));
                    }
                }
            }
            // end compatibility code
        }
    }

    /**
     * Reads the children of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String nodeName = in.getLocalName();
            if ("scope".equals(nodeName)) {
                scopes.add(new Scope(in));
            }
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "upgrade".
     */
    public static final String getXMLElementTagName() {
        return "upgrade";
    }

}