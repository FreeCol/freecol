
package net.sf.freecol.common.model;

import java.awt.Color;
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

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final int TEEPEE = 1, LONGHOUSE = 2, CITY = 3;
    public static final int LOW = 1, AVERAGE = 2, HIGH = 3;

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
        setID(in.getAttributeValue(null, "id"));
        setColor(new Color(Integer.parseInt(in.getAttributeValue(null, "color"), 16)));

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
                setModifier(modifier.getId(), modifier);
            } else if ("skill".equals(childName)) {
                skills.add(in.getAttributeValue(null, "id"));
                in.nextTag(); // close this element
            }
        }
    }

}