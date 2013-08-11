/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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


/**
 * Contains information on buildable types.
 */
public abstract class BuildableType extends FreeColGameObjectType {

    /** The required population for an ordinary buildable. */
    private static final int DEFAULT_REQUIRED_POPULATION = 1;

    /**
     * The minimum population that a Colony needs in order to build
     * this type.
     */
    private int requiredPopulation = DEFAULT_REQUIRED_POPULATION;

    /** Stores the abilities required by this Type. */
    private Map<String, Boolean> requiredAbilities = null;

    /** A list of AbstractGoods required to build this type. */
    private List<AbstractGoods> requiredGoods = null;

    /** Limits on the production of this type. */
    private List<Limit> limits = null;


    /**
     * Creates a new buildable type.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public BuildableType(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Get the population required to build this buildable type.
     *
     * @return The population required.
     */
    public int getRequiredPopulation() {
        return requiredPopulation;
    }

    /**
     * Set the population required to build this buildable type.
     *
     * @param newPopulation The new population required.
     */
    public void setRequiredPopulation(final int newPopulation) {
        this.requiredPopulation = newPopulation;
    }

    /**
     * Gets the abilities required by this type.
     *
     * @return The required abilities.
     */
    public Map<String, Boolean> getRequiredAbilities() {
        return (requiredAbilities != null) ? requiredAbilities
            : new HashMap<String, Boolean>();
    }

    public boolean requiresAbility(String key) {
        return (requiredAbilities == null)
            ? false
            : (requiredAbilities.get(key) == Boolean.TRUE);
    }

    /**
     * Sets the abilities required by this type.
     *
     * @param abilities The new required abilities.
     */
    public void setRequiredAbilities(Map<String, Boolean> abilities) {
        requiredAbilities = abilities;
    }

    /**
     * Add a new required ability.
     *
     * @param tag The ablilty name.
     * @param value The ability value.
     */
    private void addRequiredAbility(String tag, boolean value) {
        if (requiredAbilities == null) {
            requiredAbilities = new HashMap<String, Boolean>();
        }
        requiredAbilities.put(tag, value);
    }

    /**
     * Is this buildable available to a given Player?
     *
     * @param player The <code>Player</code> to check.
     * @return True if the buildable is available.
     */
    public boolean isAvailableTo(Player player) {
        if (requiredAbilities != null) {
            for (Entry<String, Boolean> entry : requiredAbilities.entrySet()) {
                if (player.hasAbility(entry.getKey()) != entry.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Get the goods required to build an instance of this buildable.
     *
     * @return A list of required goods.
     */
    public List<AbstractGoods> getRequiredGoods() {
        if (requiredGoods == null) return Collections.emptyList();
        return requiredGoods;
    }

    /**
     * Get the amount required of a given <code>GoodsType</code> to build
     * an instance of this buildable.
     *
     * @param type The <code>GoodsType</code> to check.
     * @return The amount of goods required.
     */
    public int getRequiredAmountOf(GoodsType type) {
        for (AbstractGoods goods : getRequiredGoods()) {
            if (goods.getType() == type) return goods.getAmount();
        }
        return 0;
    }

    /**
     * Set the required goods.
     *
     * @param newGoods The new required goods.
     */
    public void setRequiredGoods(List<AbstractGoods> newGoods) {
        this.requiredGoods = newGoods;
    }

    /**
     * Add a new goods requirement.
     *
     * @param ag The required <code>AbstractGoods</code> to add.
     */
    private void addRequiredGoods(AbstractGoods ag) {
        if (requiredGoods == null) {
            requiredGoods = new ArrayList<AbstractGoods>();
        }
        requiredGoods.add(ag);
    }

    /**
     * Does this buildable need goods to build?
     *
     * @return True if goods are required to build this buildable.
     */
    public boolean needsGoodsToBuild() {
        return requiredGoods != null && !requiredGoods.isEmpty();
    }

    /**
     * Get the limits on this buildable.
     *
     * @return A <code>List<Limit></code> of limits.
     */
    public List<Limit> getLimits() {
        if (limits == null) return Collections.emptyList();
        return limits;
    }

    /**
     * Set the limits on this buildable.
     *
     * @param newLimits The new <code>Limits</code> value.
     */
    public void setLimits(List<Limit> newLimits) {
        limits = newLimits;
    }

    /**
     * Add a new limit.
     *
     * @param limit The <code>Limit</code> to add.
     */
    private void addLimit(Limit limit) {
        if (limits == null) limits = new ArrayList<Limit>();
        limits.add(limit);
    }


    // Serialization

    private static final String REQUIRED_ABILITY_TAG = "required-ability";
    private static final String REQUIRED_GOODS_TAG = "required-goods";
    // Subclasses need to check this.
    public static final String REQUIRED_POPULATION_TAG = "required-population";


   /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (requiredPopulation > 1) {
            xw.writeAttribute(REQUIRED_POPULATION_TAG, requiredPopulation);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (requiredAbilities != null) {
            for (Map.Entry<String, Boolean> entry
                     : requiredAbilities.entrySet()) {
                xw.writeStartElement(REQUIRED_ABILITY_TAG);

                xw.writeAttribute(ID_ATTRIBUTE_TAG, entry.getKey());

                xw.writeAttribute(VALUE_TAG, entry.getValue());

                xw.writeEndElement();
            }
        }

        for (AbstractGoods goods : getRequiredGoods()) {
            xw.writeStartElement(REQUIRED_GOODS_TAG);

            xw.writeAttribute(ID_ATTRIBUTE_TAG, goods.getType());

            xw.writeAttribute(VALUE_TAG, goods.getAmount());

            xw.writeEndElement();
        }

        for (Limit limit : getLimits()) limit.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        requiredPopulation = xr.getAttribute(REQUIRED_POPULATION_TAG,
                                             DEFAULT_REQUIRED_POPULATION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        if (xr.shouldClearContainers()) {
            requiredAbilities = null;
            requiredGoods = null;
            limits = null;
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

        if (REQUIRED_ABILITY_TAG.equals(tag)) {
            String id = xr.readId();
            addRequiredAbility(id, xr.getAttribute(VALUE_TAG, true));
            spec.addAbility(id);
            xr.closeTag(REQUIRED_ABILITY_TAG);

        } else if (REQUIRED_GOODS_TAG.equals(tag)) {
            GoodsType type = xr.getType(spec, ID_ATTRIBUTE_TAG,
                                        GoodsType.class, (GoodsType)null);
            type.setBuildingMaterial(true);
            int amount = xr.getAttribute(VALUE_TAG, 0);
            addRequiredGoods(new AbstractGoods(type, amount));
            xr.closeTag(REQUIRED_GOODS_TAG);

        } else if (Limit.getXMLElementTagName().equals(tag)) {
            Limit limit = new Limit(xr, spec);
            if (limit.getLeftHandSide().getType() == null) {
                limit.getLeftHandSide().setType(getId());
            }
            addLimit(limit);

        } else {
            super.readChild(xr);
        }
    }
}
