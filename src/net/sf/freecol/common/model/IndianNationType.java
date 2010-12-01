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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.RandomChoice;


/**
 * Represents one of the Indian nations present in the game.
 */
public class IndianNationType extends NationType {

    public static enum SettlementNumber { LOW, AVERAGE, HIGH }
    public static enum AggressionLevel { LOW, AVERAGE, HIGH }


    /**
     * The number of settlements this Nation has.
     */
    private SettlementNumber numberOfSettlements = SettlementNumber.AVERAGE;

    /**
     * The aggression of this Nation.
     */
    private AggressionLevel aggression = AggressionLevel.AVERAGE;

    /**
     * Stores the ids of the skills taught by this Nation.
     */
    private List<RandomChoice<UnitType>> skills =
        new ArrayList<RandomChoice<UnitType>>();

    /**
     * The regions that can be settled by this Nation.
     */
    private List<String> regions = new ArrayList<String>();



    public IndianNationType(String id, Specification specification) {
        super(id, specification);
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
     * Describe <code>getSettlementTypeAsString</code> method here.
     *
     * @return a <code>String</code> value
     */
    /*
    public final String getSettlementTypeKey() {
        switch (getTypeOfSettlement()) {
        case INCA_CITY:
        case AZTEC_CITY:
            return "settlementType.city";
        case INDIAN_VILLAGE:
            return "settlementType.village";
        case INDIAN_CAMP:
        default:
            return "settlementType.camp";
        }
    }
    */

    /**
     * Returns the list of regions in which this tribe my settle.
     *
     * @return the list of regions in which this tribe my settle.
     */
    public List<String> getRegionNames() {
        return regions;
    }

    /**
     * Returns true if this Nation can settle the given Tile.
     *
     * @param tile a <code>Tile</code> value
     * @return a <code>boolean</code> value
     */
    /*
    public boolean canSettleTile(Tile tile) {
        if (tile.getType().canSettle()) {
            return canSettleRegion(tile.getRegion());
        } else {
            return false;
        }
    }
    */
    /**
     * Returns true if this Nation can settle the given Region.
     *
     * @param region a <code>Region</code> value
     * @return a <code>boolean</code> value
     */
    /*
    public boolean canSettleRegion(Region region) {
        if (regions.isEmpty()) {
            return true;
        } else if (regions.contains(region.getId())) {
            return true;
        } else if (region.getParent() == null) {
            return false;
        } else {
            return canSettleRegion(region.getParent());
        }
    }
    */

    /**
     * Generates choices for skill that could be taught from a settlement on
     * a given Tile.
     *
     * @param tile The <code>Tile</code> where the settlement will be located.
     * @return A random choice set of skills.
     */
    public List<RandomChoice<UnitType>> generateSkillsForTile(Tile tile) {
        List<RandomChoice<UnitType>> skills = getSkills();
        Map<GoodsType, Integer> scale = new HashMap<GoodsType, Integer>();

        for (RandomChoice<UnitType> skill : skills) {
            scale.put(skill.getObject().getExpertProduction(), 1);
        }

        for (Tile t: tile.getSurroundingTiles(1)) {
            for (GoodsType goodsType : scale.keySet()) {
                scale.put(goodsType, scale.get(goodsType).intValue()
                          + t.potential(goodsType, null));
            }
        }

        List<RandomChoice<UnitType>> scaledSkills
            = new ArrayList<RandomChoice<UnitType>>();
        for (RandomChoice<UnitType> skill : skills) {
            UnitType unitType = skill.getObject();
            int scaleValue = scale.get(unitType.getExpertProduction()).intValue();
            scaledSkills.add(new RandomChoice<UnitType>(unitType,
                    skill.getProbability() * scaleValue));
        }

        return scaledSkills;
    }

    /**
     * Returns a list of this Nation's skills.
     *
     * @return a list of this Nation's skills.
     */
    public List<RandomChoice<UnitType>> getSkills() {
        return skills;
    }

    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        String extendString = in.getAttributeValue(null, "extends");
        IndianNationType parent = (extendString == null) ? this :
            getSpecification().getType(extendString, IndianNationType.class);
        String valueString = in.getAttributeValue(null, "number-of-settlements");
        if (valueString == null) {
            numberOfSettlements = parent.numberOfSettlements;
        } else {
            numberOfSettlements = Enum.valueOf(SettlementNumber.class, valueString.toUpperCase(Locale.US));
        }

        valueString = in.getAttributeValue(null, "aggression");
        if (valueString == null) {
            aggression = parent.aggression;
        } else {
            aggression = Enum.valueOf(AggressionLevel.class, valueString.toUpperCase(Locale.US));
        }

        if (parent != this) {
            skills.addAll(parent.skills);
            getSettlementTypes().addAll(parent.getSettlementTypes());
            getFeatureContainer().add(parent.getFeatureContainer());
            if (parent.isAbstractType()) {
                getFeatureContainer().replaceSource(parent, this);
            }
        }

    }

    public void readChildren(XMLStreamReader in) throws XMLStreamException {
        super.readChildren(in);
        // sort skill according to probability
        Collections.sort(skills, new Comparator<RandomChoice<UnitType>>() {
                public int compare(RandomChoice<UnitType> choice1, RandomChoice<UnitType> choice2) {
                    return choice2.getProbability() - choice1.getProbability();
                }
            });
    }

    public void readChild(XMLStreamReader in) throws XMLStreamException {
        String childName = in.getLocalName();
        if ("skill".equals(childName)) {
            UnitType unitType = getSpecification().getUnitType(in.getAttributeValue(null, ID_ATTRIBUTE_TAG));
            int probability = getAttribute(in, "probability", 0);
            skills.add(new RandomChoice<UnitType>(unitType, probability));
            in.nextTag(); // close this element
        } else if (Region.getXMLElementTagName().equals(childName)) {
            regions.add(in.getAttributeValue(null, ID_ATTRIBUTE_TAG));
            in.nextTag(); // close this element
        } else {
            super.readChild(in);
        }
    }

    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("number-of-settlements", numberOfSettlements.toString().toLowerCase(Locale.US));
        out.writeAttribute("aggression", aggression.toString().toLowerCase(Locale.US));
    }

    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        for (RandomChoice<UnitType> choice : skills) {
            out.writeStartElement("skill");
            out.writeAttribute(ID_ATTRIBUTE_TAG, choice.getObject().getId());
            out.writeAttribute("probability", Integer.toString(choice.getProbability()));
            out.writeEndElement();
        }

        for (String region : regions) {
            out.writeStartElement(Region.getXMLElementTagName());
            out.writeAttribute(ID_ATTRIBUTE_TAG, region);
            out.writeEndElement();
        }

    }

    public static String getXMLElementTagName() {
        return "indian-nation-type";
    }


}
