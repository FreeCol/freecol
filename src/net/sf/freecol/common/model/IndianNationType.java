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

/**
 * Represents one of the Indian nations present in the game.
 */
public class IndianNationType extends NationType {


    public static final int TEEPEE = 0, LONGHOUSE = 1, CITY = 2;
    public static final int LOW = 0, AVERAGE = 1, HIGH = 2;

    /**
     * The number of settlements this Nation has.
     */
    private int numberOfSettlements;

    /**
     * The aggression of this Nation.
     */
    private int aggression;

    /**
     * The type of settlement this Nation has.
     */
    private int typeOfSettlement;

    /**
     * Stores the ids of the skills taught by this Nation.
     */
    private List<String> skills = new ArrayList<String>();

    /**
     * Sole constructor.
     */
    public IndianNationType(int index) {
        super(index);
    }


    /**
     * Returns false.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isEuropean() {
        return false;
    }

    /**
     * Returns false.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isREF() {
        return false;
    }
    /**
     * Get the <code>NumberOfSettlements</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getNumberOfSettlements() {
        return numberOfSettlements;
    }

    /**
     * Set the <code>NumberOfSettlements</code> value.
     *
     * @param newNumberOfSettlements The new NumberOfSettlements value.
     */
    public final void setNumberOfSettlements(final int newNumberOfSettlements) {
        this.numberOfSettlements = newNumberOfSettlements;
    }

    /**
     * Get the <code>Aggression</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getAggression() {
        return aggression;
    }

    /**
     * Set the <code>Aggression</code> value.
     *
     * @param newAggression The new Aggression value.
     */
    public final void setAggression(final int newAggression) {
        this.aggression = newAggression;
    }

    /**
     * Get the <code>TypeOfSettlement</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getTypeOfSettlement() {
        return typeOfSettlement;
    }

    /**
     * Set the <code>TypeOfSettlement</code> value.
     *
     * @param newTypeOfSettlement The new TypeOfSettlement value.
     */
    public final void setTypeOfSettlement(final int newTypeOfSettlement) {
        this.typeOfSettlement = newTypeOfSettlement;
    }

    /**
     * Returns a list of this Nation's skills.
     *
     * @return a list of this Nation's skills.
     */
    public List<String> getSkills() {
        return skills;
    }

    public void readFromXML(XMLStreamReader in, final Map<String, UnitType> unitTypeByRef)
            throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));

        String valueString = in.getAttributeValue(null, "number-of-settlements");
        if ("low".equals(valueString)) {
            numberOfSettlements = LOW;
        } else if ("average".equals(valueString)) {
            numberOfSettlements = AVERAGE;
        } else if ("high".equals(valueString)) {
            numberOfSettlements = HIGH;
        } else {
            throw new IllegalArgumentException("Unknown value for attribute number-of-settlements: " +
                                               valueString);
        }

        valueString = in.getAttributeValue(null, "aggression");
        if ("low".equals(valueString)) {
            aggression = LOW;
        } else if ("average".equals(valueString)) {
            aggression = AVERAGE;
        } else if ("high".equals(valueString)) {
            aggression = HIGH;
        } else {
            throw new IllegalArgumentException("Unknown value for attribute aggression: " +
                                               valueString);
        }

        valueString = in.getAttributeValue(null, "type-of-settlement");
        if ("teepee".equals(valueString)) {
            typeOfSettlement = TEEPEE;
        } else if ("longhouse".equals(valueString)) {
            typeOfSettlement = LONGHOUSE;
        } else if ("city".equals(valueString)) {
            typeOfSettlement = CITY;
        } else {
            throw new IllegalArgumentException("Unknown value for attribute type-of-settlement: " +
                                               valueString);
        }

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("ability".equals(childName)) {
                String abilityId = in.getAttributeValue(null, "id");
                boolean value = getAttribute(in, "value", true);
                setAbility(abilityId, value);
                in.nextTag(); // close this element
            } else if (Modifier.getXMLElementTagName().equals(childName)) {
                Modifier modifier = new Modifier(in); // Modifier close the element
                if (modifier.getSource() == null) {
                    modifier.setSource(this.getId());
                }
               setModifier(modifier.getId(), modifier);
            } else if ("skill".equals(childName)) {
                skills.add(in.getAttributeValue(null, "id"));
                in.nextTag(); // close this element
            }
        }
    }

}
