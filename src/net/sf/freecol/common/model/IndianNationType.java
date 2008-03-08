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
import net.sf.freecol.common.model.Settlement.SettlementType;


/**
 * Represents one of the Indian nations present in the game.
 */
public class IndianNationType extends NationType {

    public static enum SettlementNumber { LOW, AVERAGE, HIGH };
    public static enum AggressionLevel { LOW, AVERAGE, HIGH };

    /**
     * The number of settlements this Nation has.
     */
    private SettlementNumber numberOfSettlements;

    /**
     * The aggression of this Nation.
     */
    private AggressionLevel aggression;

    /**
     * The type of settlement this Nation has.
     */
    private SettlementType typeOfSettlement;

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
     * @return a <code>SettlementNumber</code> value
     */
    public final SettlementNumber getNumberOfSettlements() {
        return numberOfSettlements;
    }

    /**
     * Set the <code>NumberOfSettlements</code> value.
     *
     * @param newNumberOfSettlements The new NumberOfSettlements value.
     */
    public final void setNumberOfSettlements(final SettlementNumber newNumberOfSettlements) {
        this.numberOfSettlements = newNumberOfSettlements;
    }

    /**
     * Get the <code>Aggression</code> value.
     *
     * @return an <code>AggressionLevel</code> value
     */
    public final AggressionLevel getAggression() {
        return aggression;
    }

    /**
     * Set the <code>Aggression</code> value.
     *
     * @param newAggression The new Aggression value.
     */
    public final void setAggression(final AggressionLevel newAggression) {
        this.aggression = newAggression;
    }

    /**
     * Get the <code>TypeOfSettlement</code> value.
     *
     * @return an <code>SettlementType</code> value
     */
    public final SettlementType getTypeOfSettlement() {
        return typeOfSettlement;
    }

    /**
     * Describe <code>getSettlementTypeAsString</code> method here.
     *
     * @return a <code>String</code> value
     */
    public final String getSettlementTypeAsString() {
        switch (typeOfSettlement) {
        case INCA_CITY:
        case AZTEC_CITY:
            return Messages.message("settlementType.city");
        case INDIAN_VILLAGE:
            return Messages.message("settlementType.village");
        case INDIAN_CAMP:
        default:
            return Messages.message("settlementType.camp");
        }
    }


    /**
     * Set the <code>TypeOfSettlement</code> value.
     *
     * @param newTypeOfSettlement The new TypeOfSettlement value.
     */
    public final void setTypeOfSettlement(final SettlementType newTypeOfSettlement) {
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

    public void readFromXML(XMLStreamReader in, Specification specification)
            throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));

        String valueString = in.getAttributeValue(null, "number-of-settlements").toUpperCase();
        numberOfSettlements = Enum.valueOf(SettlementNumber.class, valueString);

        valueString = in.getAttributeValue(null, "aggression").toUpperCase();
        aggression = Enum.valueOf(AggressionLevel.class, valueString);

        valueString = in.getAttributeValue(null, "type-of-settlement").toUpperCase();
        typeOfSettlement = Enum.valueOf(SettlementType.class, valueString);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if (Ability.getXMLElementTagName().equals(childName)) {
                Ability ability = new Ability(in);
                if (ability.getSource() == null) {
                    ability.setSource(getNameKey());
                }
                addAbility(ability); // Ability close the element
                specification.getAbilityKeys().add(ability.getId());
            } else if (Modifier.getXMLElementTagName().equals(childName)) {
                Modifier modifier = new Modifier(in); // Modifier close the element
                if (modifier.getSource() == null) {
                    modifier.setSource(getNameKey());
                }
               addModifier(modifier);
                specification.getModifierKeys().add(modifier.getId());
            } else if ("skill".equals(childName)) {
                skills.add(in.getAttributeValue(null, "id"));
                in.nextTag(); // close this element
            }
        }
    }

}
