/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.RandomChoice;


/**
 * Represents one of the native nations present in the game.
 */
public class IndianNationType extends NationType {

    /** Stores the ids of the skills taught by this Nation. */
    private List<RandomChoice<UnitType>> skills = null;

    /** Identifiers for the regions that can be settled by this Nation. */
    private List<String> regions = null;


    /**
     * Create a new native nation type.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public IndianNationType(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Is this a European nation type?
     *
     * @return False.
     */
    @Override
    public boolean isEuropean() {
        return false;
    }

    /**
     * Is this a native nation type?
     *
     * @return True.
     */
    @Override
    public boolean isIndian() {
        return true;
    }

    /**
     * Is this a REF nation type?
     *
     * @return False.
     */
    @Override
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
        return (regions == null) ? Collections.<String>emptyList()
            : regions;
    }

    /**
     * Add a region identifier.
     *
     * @param id The object identifier.
     */
    private void addRegion(String id) {
        if (regions == null) regions = new ArrayList<>();
        regions.add(id);
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
        return (skills == null)
            ? Collections.<RandomChoice<UnitType>>emptyList()
            : skills;
    }

    /**
     * Add a skill.
     *
     * @param unitType The <code>UnitType</code> skill taught.
     * @param probability The probability of the skill.
     */
    private void addSkill(UnitType unitType, int probability) {
        if (skills == null) skills = new ArrayList<>();
        skills.add(new RandomChoice<>(unitType, probability));
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
        Map<GoodsType, Integer> scale = new HashMap<>();

        for (RandomChoice<UnitType> skill : skills) {
            scale.put(skill.getObject().getExpertProduction(), 1);
        }

        for (Tile t: tile.getSurroundingTiles(1)) {
            for (Entry<GoodsType, Integer> entry : scale.entrySet()) {
                GoodsType goodsType = entry.getKey();
                scale.put(goodsType, entry.getValue()
                          + t.getPotentialProduction(goodsType, null));
            }
        }

        List<RandomChoice<UnitType>> scaledSkills = new ArrayList<>();
        for (RandomChoice<UnitType> skill : skills) {
            UnitType unitType = skill.getObject();
            int scaleValue = scale.get(unitType.getExpertProduction());
            scaledSkills.add(new RandomChoice<>(unitType,
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
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (RandomChoice<UnitType> choice : getSkills()) {
            xw.writeStartElement(SKILL_TAG);

            xw.writeAttribute(ID_ATTRIBUTE_TAG, choice.getObject());

            xw.writeAttribute(PROBABILITY_TAG, choice.getProbability());

            xw.writeEndElement();
        }

        for (String region : getRegionNames()) {
            xw.writeStartElement(Region.getXMLElementTagName());

            xw.writeAttribute(ID_ATTRIBUTE_TAG, region);

            xw.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (xr.shouldClearContainers()) {
            skills = null;
            regions = null;
        }

        final Specification spec = getSpecification();
        IndianNationType parent = xr.getType(spec, EXTENDS_TAG,
                                             IndianNationType.class, this);
        if (parent != this) {
            if (parent.skills != null && !parent.skills.isEmpty()) {
                if (skills == null) skills = new ArrayList<>();
                skills.addAll(parent.skills);
            }

            if (parent.regions != null && !parent.regions.isEmpty()) {
                if (regions == null) regions = new ArrayList<>();
                regions.addAll(parent.regions);
            }
        }

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (SKILL_TAG.equals(tag)) {
            addSkill(xr.getType(spec, ID_ATTRIBUTE_TAG,
                                UnitType.class, (UnitType)null),
                     xr.getAttribute(PROBABILITY_TAG, 0));
            xr.closeTag(SKILL_TAG);

        } else if (Region.getXMLElementTagName().equals(tag)) {
            addRegion(xr.readId());
            xr.closeTag(Region.getXMLElementTagName());

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "indian-nation-type".
     */
    public static String getXMLElementTagName() {
        return "indian-nation-type";
    }
}
