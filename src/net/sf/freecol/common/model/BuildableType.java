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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Contains information on buildable types.
 */
public abstract class BuildableType extends FreeColGameObjectType {

    /**
     * The required population for an ordinary buildable.
     */
    private static final int DEFAULT_REQUIRED_POPULATION = 1;

    /**
     * The minimum population that a Colony needs in order to build
     * this type.
     */
    private int requiredPopulation = DEFAULT_REQUIRED_POPULATION;

    /**
     * Stores the abilities required by this Type.
     */
    private Map<String, Boolean> requiredAbilities = null;

    /**
     * A list of AbstractGoods required to build this type.
     */
    private List<AbstractGoods> requiredGoods = null;

    /**
     * Limits on the production of this type.
     */
    private List<Limit> limits = null;


    /**
     * Creates a new buildable type.
     *
     * @param id The id of the buildable.
     * @param specification A <code>Specification</code> to operate within.
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

    /**
     * Sets the abilities required by this type.
     *
     * @param abilities The new required abilities.
     */
    public void setRequiredAbilities(Map<String, Boolean> abilities) {
        requiredAbilities = abilities;
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
        return (requiredGoods != null) ? requiredGoods
            : new ArrayList<AbstractGoods>();
    }

    /**
     * Get the amount required of a given <code>GoodsType</code> to build
     * an instance of this buildable.
     *
     * @param The <code>GoodsType</code> to check.
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
        return (limits != null) ? limits
            : new ArrayList<Limit>();
    }

    /**
     * Set the limits on this buildable.
     *
     * @param newLimits The new <code>Limits</code> value.
     */
    public void setLimits(List<Limit> newLimits) {
        limits = newLimits;
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
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        if (requiredPopulation > 1) {
            writeAttribute(out, REQUIRED_POPULATION_TAG, requiredPopulation);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        if (requiredAbilities != null) {
            for (Map.Entry<String, Boolean> entry
                     : requiredAbilities.entrySet()) {
                out.writeStartElement(REQUIRED_ABILITY_TAG);

                writeAttribute(out, ID_ATTRIBUTE_TAG, entry.getKey());

                writeAttribute(out, VALUE_TAG, entry.getValue());

                out.writeEndElement();
            }
        }

        for (AbstractGoods goods : getRequiredGoods()) {
            out.writeStartElement(REQUIRED_GOODS_TAG);
            
            writeAttribute(out, ID_ATTRIBUTE_TAG, goods.getType());

            writeAttribute(out, VALUE_TAG, goods.getAmount());

            out.writeEndElement();
        }

        for (Limit limit : getLimits()) {
            limit.toXMLImpl(out);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        requiredPopulation = getAttribute(in, REQUIRED_POPULATION_TAG,
                                          DEFAULT_REQUIRED_POPULATION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        if (readShouldClearContainers(in)) {
            requiredAbilities = null;
            requiredGoods = null;
            limits = null;
        }
        
        super.readChildren(in);
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = in.getLocalName();

        if (REQUIRED_ABILITY_TAG.equals(tag)) {
            String str = getAttribute(in, ID_ATTRIBUTE_TAG, (String)null);
            if (str != null) {
                if (requiredAbilities == null) {
                    requiredAbilities = new HashMap<String, Boolean>();
                }
                requiredAbilities.put(str, getAttribute(in, VALUE_TAG, true));
                spec.addAbility(str);
            }
            in.nextTag(); // close this element

        } else if (REQUIRED_GOODS_TAG.equals(tag)) {
            GoodsType type = spec.getType(in, ID_ATTRIBUTE_TAG,
                                          GoodsType.class, (GoodsType)null);
            int amount = getAttribute(in, VALUE_TAG, 0);
            if (type != null && amount > 0) {
                AbstractGoods ag = new AbstractGoods(type, amount);
                type.setBuildingMaterial(true);
                if (requiredGoods == null) {
                    requiredGoods = new ArrayList<AbstractGoods>();
                }
                requiredGoods.add(ag);
            }
            in.nextTag(); // close this element

        } else if (Limit.getXMLElementTagName().equals(tag)) {
            Limit limit = new Limit(spec);
            limit.readFromXML(in);
            if (limit.getLeftHandSide().getType() == null) {
                limit.getLeftHandSide().setType(getId());
            }
            if (limits == null) {
                limits = new ArrayList<Limit>();
            }
            limits.add(limit);

        } else {
            super.readChild(in);
        }
    }
}
