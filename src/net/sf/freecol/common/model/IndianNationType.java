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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.util.RandomChoice;


/**
 * Represents one of the Indian nations present in the game.
 */
public class IndianNationType extends NationType {

    /**
     * Stores the ids of the skills taught by this Nation.
     */
    private List<RandomChoice<UnitType>> skills = null;

    /**
     * Identifiers for the regions that can be settled by this Nation.
     */
    private List<String> regions = null;


    /**
     * Create a new native nation type.
     *
     * @param id The nation type identifier.
     * @param specification The containing <code>Specification</code>.
     */
    public IndianNationType(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Is this a European nation type?
     *
     * @return False.
     */
    public boolean isEuropean() {
        return false;
    }

    /**
     * Is this a native nation type?
     *
     * @return True.
     */
    public boolean isIndian() {
        return true;
    }

    /**
     * Is this a REF nation type?
     *
     * @return False.
     */
    public boolean isREF() {
        return false;
    }

    /**
     * Get a message id for the general type of settlements of this nation.
     *
     * @param plural Choose the plural form or not.
     * @return A suitable message id.
     */
    public final String getSettlementTypeKey(boolean plural) {
        return getSettlementType(false).getId() + ((plural) ? ".plural" : "");
    }

    /**
     * Gets the list of regions in which this tribe may settle.
     *
     * @return A list of regions identifiers.
     */
    public List<String> getRegionNames() {
        return (regions == null) ? new ArrayList<String>()
            : regions;
    }

    /**
     * Can this Nation can settle the given Tile?
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
     * Can this Nation can settle the given Region?
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
     * Gets a list of this Nation's skills.
     *
     * @return A list of national skills.
     */
    public List<RandomChoice<UnitType>> getSkills() {
        if (skills == null) return Collections.emptyList();
        return skills;
    }

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


    // Serialization

    private static final String PROBABILITY_TAG = "probability";
    private static final String SKILL_TAG = "skill";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        for (RandomChoice<UnitType> choice : getSkills()) {
            out.writeStartElement(SKILL_TAG);

            writeAttribute(out, ID_ATTRIBUTE_TAG, choice.getObject());

            writeAttribute(out, PROBABILITY_TAG, choice.getProbability());

            out.writeEndElement();
        }

        for (String region : getRegionNames()) {
            out.writeStartElement(Region.getXMLElementTagName());

            writeAttribute(out, ID_ATTRIBUTE_TAG, region);

            out.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        if (readShouldClearContainers(in)) {
            // Clear containers.
            skills = null;
            regions = null;
        }

        final Specification spec = getSpecification();
        IndianNationType parent = spec.getType(in, EXTENDS_TAG,
                                               IndianNationType.class, this);

        super.readChildren(in);

        if (parent != this) {
            if (parent.skills != null && !parent.skills.isEmpty()) {
                if (skills == null) {
                    skills = new ArrayList<RandomChoice<UnitType>>();
                }
                skills.addAll(parent.skills);
            }

            if (parent.regions != null && !parent.regions.isEmpty()) {
                if (regions == null) {
                    regions = new ArrayList<String>();
                }
                regions.addAll(parent.regions);
            }
        }

        if (skills != null) {
            // sort skill according to probability
            Collections.sort(skills, new Comparator<RandomChoice<UnitType>>() {
                    public int compare(RandomChoice<UnitType> c1,
                                       RandomChoice<UnitType> c2) {
                        return c2.getProbability() - c1.getProbability();
                    }
                });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = in.getLocalName();

        if (SKILL_TAG.equals(tag)) {
            UnitType unitType = spec.getType(in, ID_ATTRIBUTE_TAG,
                                             UnitType.class, (UnitType)null);

            int probability = getAttribute(in, PROBABILITY_TAG, 0);

            if (unitType != null && probability > 0) {
                if (skills == null) {
                    skills = new ArrayList<RandomChoice<UnitType>>();
                }
                skills.add(new RandomChoice<UnitType>(unitType, probability));
            }
            closeTag(in, SKILL_TAG);

        } else if (Region.getXMLElementTagName().equals(tag)) {
            String id = getAttribute(in, ID_ATTRIBUTE_TAG, (String)null);
            if (id != null) {
                if (regions == null) {
                    regions = new ArrayList<String>();
                }
                regions.add(id);
            }
            closeTag(in, Region.getXMLElementTagName());

        } else {
            super.readChild(in);
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "indian-nation-type".
     */
    public static String getXMLElementTagName() {
        return "indian-nation-type";
    }
}
