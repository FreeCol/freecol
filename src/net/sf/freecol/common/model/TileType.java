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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.StringOption;
import net.sf.freecol.common.util.RandomChoice;


public final class TileType extends FreeColGameObjectType {

    public static enum RangeType { HUMIDITY, TEMPERATURE, ALTITUDE };

    /**
     * Use these tile types only for "land maps", i.e. maps that only
     * distinguish water and land.
     */
    public static final TileType WATER = new TileType("WATER", true);
    public static final TileType LAND  = new TileType("LAND", false);

    /** Is this a forested tile? */
    private boolean forest;

    /** Is this a water tile? */
    private boolean water;

    /** Can this tile be settled? */
    private boolean canSettle;

    /** Whether this TileType is connected to Europe. */
    private boolean connected;

    /** Is this elevated terrain? */
    private boolean elevation;

    /** The base movement cost for this tile type. */
    private int basicMoveCost;

    /** The base work turns for this tile type. */
    private int basicWorkTurns;

    /** The humidity range for this tile type. */
    private int[] humidity = new int[2];
    /** The temperature range for this tile type. */
    private int[] temperature = new int[2];
    /** The altitude range for this tile type. */
    private int[] altitude = new int[2];

    /** The resource types that are valid for this tile type. */
    private List<RandomChoice<ResourceType>> resourceTypes = null;

    /** The disasters that may strike this type of tile. */
    private List<RandomChoice<Disaster>> disasters = null;

    /**
     * The possible production types of this tile type.  This includes
     * the production types available if a tile of this type is a
     * colony center tile.
     */
    private List<ProductionType> productionTypes = null;

    private static final String TILE_PRODUCTION = "model.option.tileProduction";

    // TODO: make this hack go away!
    private String productionLevel = null;


    /**
     * Create a new tile type.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public TileType(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Creates a new <code>TileType</code> instance. This constructor
     * is used to create the "virtual" tile types <code>LAND</code>
     * and <code>WATER</code>, which are intended to simplify map
     * loading.
     *
     * @param id The object identifier.
     * @param water True if this is a water tile.
     */
    private TileType(String id, boolean water) {
        super(id, null);
        this.water = water;
    }


    /**
     * Is this tile type forested?
     *
     * @return True if this is a forested tile type.
     */
    public boolean isForested() {
        return forest;
    }

    /**
     * Is this a water tile type?
     *
     * @return True if this is a water tile type.
     */
    public boolean isWater() {
        return water;
    }

    /**
     * Can this tile type be settled?
     *
     * @return True if this is a settleable tile type.
     */
    public boolean canSettle() {
        return canSettle;
    }

    /**
     * Is this tile type connected to the high seas, by definition.
     *
     * @return True if the tile type is inherently connected to the high seas.
     */
    public boolean isHighSeasConnected() {
        return connected;
    }

    /**
     * Is this tile type directly connected to the high seas, that is, a
     * unit on a tile of this type can move immediately to the high seas.
     *
     * @return True if the tile type is directly connected.
     */
    public boolean isDirectlyHighSeasConnected() {
        return hasAbility("model.ability.moveToEurope");
    }

    /**
     * Is this an elevated tile type?
     *
     * @return True if this is an elevated tile type.
     */
    public boolean isElevation() {
        return elevation;
    }

    /**
     * Gets the basic movement cost through this tile type.
     *
     * @return The basic movement cost.
     */
    public int getBasicMoveCost() {
        return basicMoveCost;
    }

    /**
     * Gets the basic work turns to build an improvement on this tile type.
     *
     * @return The basic work turns.
     */
    public int getBasicWorkTurns() {
        return basicWorkTurns;
    }

    /**
     * Gets the resources that can be placed on this tile type.
     *
     * @return A weighted list of resource types.
     */
    public List<RandomChoice<ResourceType>> getWeightedResources() {
        if (resourceTypes == null) return Collections.emptyList();
        return resourceTypes;
    }

    /**
     * Gets the natural disasters than can strike this tile type.
     *
     * @return a <code>List<RandomChoice<Disaster>></code> value
     */
    public List<RandomChoice<Disaster>> getDisasters() {
        if (disasters == null) return Collections.emptyList();
        return disasters;
    }

    /**
     * Add a disaster.
     *
     * @param disaster The <code>Disaster</code> to add.
     * @param probability The probability of the disaster.
     */
    private void addDisaster(Disaster disaster, int probability) {
        if (disasters == null) {
            disasters = new ArrayList<RandomChoice<Disaster>>();
        }
        disasters.add(new RandomChoice<Disaster>(disaster, probability));
    }

    /**
     * Gets the resource types that can be found on this tile type.
     *
     * @return A list of <code>ResourceType</code>s.
     */
    public List<ResourceType> getResourceTypes() {
        List<ResourceType> result = new ArrayList<ResourceType>();
        if (resourceTypes != null) { 
            for (RandomChoice<ResourceType> resource : resourceTypes) {
                result.add(resource.getObject());
            }
        }
        return result;
    }

    /**
     * Add a resource type.
     *
     * @param type The <code>ResourceType</code> to add.
     * @param probability The percentage probability of the resource
     *     being present.
     */
    private void addResourceType(ResourceType type, int probability) {
        if (resourceTypes == null) {
            resourceTypes = new ArrayList<RandomChoice<ResourceType>>();
        }
        resourceTypes.add(new RandomChoice<ResourceType>(type, probability));
    }

    /**
     * Can this tile type contain a specified resource type?
     *
     * @param resourceType The <code>ResourceType</code> to test.
     * @return True if the <code>ResourceType</code> is compatible.
     */
    public boolean canHaveResourceType(ResourceType resourceType) {
        return getResourceTypes().contains(resourceType);
    }

    /**
     * Gets the amount of goods of given goods type this tile type can
     * produce.
     *
     * This method applies the production bonus to
     * <code>0f</code>.  Thus, it will always return <code>0</code>
     * unless an additive modifier is present.  This is intentional.
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param unitType A <code>UnitType</code> that is to do the work.
     * @return The amount of goods production.
     * @see #getProductionBonus(GoodsType)
     */
    public int getProductionOf(GoodsType goodsType, UnitType unitType) {
        return (int)applyModifier(0f, goodsType.getId(), unitType);
    }

    /**
     * Gets the production bonuses for the given goods type.
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @return A set of applicable production modifiers.
     */
    public Set<Modifier> getProductionBonus(GoodsType goodsType) {
        return getModifierSet(goodsType.getId());
    }

    /**
     * Gets the production types applicable to this tile type.
     *
     * @return A list of <code>ProductionType</code>s.
     */
    public List<ProductionType> getProductionTypes() {
        if (productionTypes == null) return Collections.emptyList();
        return productionTypes;
    }

    /**
     * Evaluate the TILE_PRODUCTION option.
     *
     * @return The TILE_PRODUCTION option value.
     */
    public String getTileProduction() {
        return getSpecification().getString(TILE_PRODUCTION);
    }

    /**
     * Gets the production types available at the current difficulty
     * level.
     *
     * @param center Whether the tile is a colony center tile.
     * @return A list of <code>ProductionType</code>s.
     */
    public List<ProductionType> getProductionTypes(boolean center) {
        return getProductionTypes(center, getTileProduction());
    }

    /**
     * Gets the production types available for the given combination
     * of colony center tile and production level.  If the production
     * level is null, all production levels will be returned.
     *
     * @param center Whether the tile is a colony center tile.
     * @param level The production level.
     * @return A list of <code>ProductionType</code>s.
     */
    public List<ProductionType> getProductionTypes(boolean center,
                                                   String level) {
        List<ProductionType> result = new ArrayList<ProductionType>();
        if (productionTypes != null) {
            for (ProductionType productionType : productionTypes) {
                if (productionType.isColonyCenterTile() == center
                    && productionType.appliesTo(level)) {
                    result.add(productionType);
                }
            }
        }
        return result;
    }

    /**
     * Gets a list of the AbstractGoods produced by this tile type
     * when it is not the colony center tile.
     *
     * @return A list of produced <code>AbstractGoods</code>.
     */
    public List<AbstractGoods> getProduction() {
        List<AbstractGoods> production = new ArrayList<AbstractGoods>();
        for (ProductionType productionType
                 : getProductionTypes(false, productionLevel)) {
            List<AbstractGoods> outputs = productionType.getOutputs();
            if (outputs != null && !outputs.isEmpty()) {
                production.addAll(outputs);
            }
        }
        return production;
    }

    /**
     * Add a production type.
     *
     * @param productionType The <code>ProductionType</code> to add.
     */
    private void addProductionType(ProductionType productionType) {
        if (productionTypes == null) {
            productionTypes = new ArrayList<ProductionType>();
        }
        productionTypes.add(productionType);
    }

    /**
     * Gets the defence bonuses applicable to this tile type.
     *
     * @return A set of defensive modifiers.
     */
    public Set<Modifier> getDefenceBonus() {
        return getModifierSet(Modifier.DEFENCE);
    }

    /**
     * Can this tile type support the a given TileImprovementType.
     *
     * @param improvement The <code>TileImprovementType</code> to check.
     * @return True if the improvement is compatible.
     */
    public boolean canHaveImprovement(TileImprovementType improvement) {
        return improvement != null && improvement.isTileTypeAllowed(this);
    }

    /**
     * Is this tile type suitable for a given range type value.
     *
     * @param rangeType The <code>RangeType</code> to test.
     * @param value The value to check.
     * @return True if the tile type meets the range limits.
     */
    public boolean withinRange(RangeType rangeType, int value) {
        switch (rangeType) {
        case HUMIDITY:
            return humidity[0] <= value && value <= humidity[1];
        case TEMPERATURE:
            return temperature[0] <= value && value <= temperature[1];
        case ALTITUDE:
            return altitude[0] <= value && value <= altitude[1];
        default:
            break;
        }
        return false;
    }

    /**
     * Apply a difficulty level to this tile type.
     *
     * @param difficultyLevel difficulty level to apply
     */
    @Override
    public void applyDifficultyLevel(OptionGroup difficultyLevel) {
        final Specification spec = getSpecification();

        productionLevel = ((StringOption) difficultyLevel.getOption("model.option.tileProduction"))
            .getValue();
        // remove old modifiers
        for (GoodsType goodsType : spec.getGoodsTypeList()) {
            removeModifiers(goodsType.getId());
        }
        // add new modifiers
        for (ProductionType productionType : getProductionTypes()) {
            if (productionType.appliesTo(productionLevel)) {
                List<AbstractGoods> outputs = productionType.getOutputs();
                if (outputs == null) continue;
                for (AbstractGoods goods : outputs) {
                    addModifier(new Modifier(goods.getType().getId(), this,
                                             goods.getAmount(),
                                             Modifier.Type.ADDITIVE));
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Kludge to make this public so that MapViewer can see it.
     */
    @Override
    public int getIndex() {
        return super.getIndex();
    }


    // Serialization

    private static final String ALTITUDE_MIN_TAG = "altitudeMin";
    private static final String ALTITUDE_MAX_TAG = "altitudeMax";
    private static final String BASIC_MOVE_COST_TAG = "basic-move-cost";
    private static final String BASIC_WORK_TURNS_TAG = "basic-work-turns";
    private static final String CAN_SETTLE_TAG = "can-settle";
    private static final String DISASTER_TAG = "disaster";
    private static final String GEN_TAG = "gen";
    private static final String GOODS_TYPE_TAG = "goods-type";
    private static final String HUMIDITY_MIN_TAG = "humidityMin";
    private static final String HUMIDITY_MAX_TAG = "humidityMax";
    private static final String IS_CONNECTED_TAG = "is-connected";
    private static final String IS_ELEVATION_TAG = "is-elevation";
    private static final String IS_FOREST_TAG = "is-forest";
    private static final String IS_WATER_TAG = "is-water";
    private static final String PRIMARY_PRODUCTION_TAG = "primary-production";
    private static final String PROBABILITY_TAG = "probability";
    private static final String PRODUCTION_TAG = "production";
    private static final String PRODUCTION_LEVEL_TAG = "productionLevel";
    private static final String RESOURCE_TAG = "resource";
    private static final String SECONDARY_PRODUCTION_TAG = "secondary-production";
    private static final String TEMPERATURE_MIN_TAG = "temperatureMin";
    private static final String TEMPERATURE_MAX_TAG = "temperatureMax";
    private static final String TILE_PRODUCTION_TAG = "tile-production";
    private static final String TYPE_TAG = "type";


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
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, BASIC_MOVE_COST_TAG, basicMoveCost);

        writeAttribute(out, BASIC_WORK_TURNS_TAG, basicWorkTurns);

        writeAttribute(out, IS_FOREST_TAG, forest);

        writeAttribute(out, IS_WATER_TAG, water);

        writeAttribute(out, IS_ELEVATION_TAG, elevation);

        writeAttribute(out, IS_CONNECTED_TAG, connected);

        writeAttribute(out, CAN_SETTLE_TAG, canSettle);

        writeAttribute(out, PRODUCTION_LEVEL_TAG, productionLevel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        out.writeStartElement(GEN_TAG);

        writeAttribute(out, HUMIDITY_MIN_TAG, humidity[0]);

        writeAttribute(out, HUMIDITY_MAX_TAG, humidity[1]);

        writeAttribute(out, TEMPERATURE_MIN_TAG, temperature[0]);

        writeAttribute(out, TEMPERATURE_MAX_TAG, temperature[1]);

        writeAttribute(out, ALTITUDE_MIN_TAG, altitude[0]);

        writeAttribute(out, ALTITUDE_MAX_TAG, altitude[1]);

        out.writeEndElement();

        for (ProductionType productionType : getProductionTypes()) {
            productionType.toXML(out);
        }

        for (RandomChoice<ResourceType> choice : getWeightedResources()) {
            out.writeStartElement(RESOURCE_TAG);

            writeAttribute(out, TYPE_TAG, choice.getObject());

            writeAttribute(out, PROBABILITY_TAG, choice.getProbability());

            out.writeEndElement();
        }

        for (RandomChoice<Disaster> choice : getDisasters()) {
            out.writeStartElement(DISASTER_TAG);

            writeAttribute(out, ID_ATTRIBUTE_TAG, choice.getObject());

            writeAttribute(out, PROBABILITY_TAG, choice.getProbability());

            out.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        basicMoveCost = getAttribute(in, BASIC_MOVE_COST_TAG, 1);

        basicWorkTurns = getAttribute(in, BASIC_WORK_TURNS_TAG, 1);

        forest = getAttribute(in, IS_FOREST_TAG, false);

        water = getAttribute(in, IS_WATER_TAG, false);

        elevation = getAttribute(in, IS_ELEVATION_TAG, false);

        canSettle = getAttribute(in, CAN_SETTLE_TAG, !water);

        connected = getAttribute(in, IS_CONNECTED_TAG, false);

        productionLevel = getAttribute(in, PRODUCTION_LEVEL_TAG, (String)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        if (readShouldClearContainers(in)) {
            disasters = null;
            resourceTypes = null;
            productionTypes = null;
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

        if (DISASTER_TAG.equals(tag)) {
            addDisaster(spec.getType(in, ID_ATTRIBUTE_TAG,
                                     Disaster.class, (Disaster)null),
                        getAttribute(in, PROBABILITY_TAG, 100));
            closeTag(in, DISASTER_TAG);

        } else if (GEN_TAG.equals(tag)) {
            humidity[0] = getAttribute(in, HUMIDITY_MIN_TAG, 0);
            humidity[1] = getAttribute(in, HUMIDITY_MAX_TAG, 100);
            temperature[0] = getAttribute(in, TEMPERATURE_MIN_TAG, -20);
            temperature[1] = getAttribute(in, TEMPERATURE_MAX_TAG, 40);
            altitude[0] = getAttribute(in, ALTITUDE_MIN_TAG, 0);
            altitude[1] = getAttribute(in, ALTITUDE_MAX_TAG, 0);
            closeTag(in, GEN_TAG);

        } else if (PRODUCTION_TAG.equals(tag)
            && getAttribute(in, GOODS_TYPE_TAG, (String)null) == null) {
            // new production style
            addProductionType(new ProductionType(in, spec));

        } else if (PRODUCTION_TAG.equals(tag)
            || PRIMARY_PRODUCTION_TAG.equals(tag)
            || SECONDARY_PRODUCTION_TAG.equals(tag)) {
            // @compat 0.10.6
            GoodsType type = spec.getType(in, GOODS_TYPE_TAG,
                                          GoodsType.class, (GoodsType)null);
            int amount = getAttribute(in, VALUE_TAG, 0);
            AbstractGoods goods = new AbstractGoods(type, amount);
            String tileProduction = getAttribute(in, TILE_PRODUCTION_TAG,
                                                 (String)null);
            // CAUTION: this only works if the primary production is
            // defined before the secondary production
            if (PRIMARY_PRODUCTION_TAG.equals(tag)) {
                addProductionType(new ProductionType(goods, true, tileProduction));
            } else if (SECONDARY_PRODUCTION_TAG.equals(tag)) {
                for (ProductionType productionType : getProductionTypes()) {
                    if (productionType.isColonyCenterTile()
                        && (tileProduction == null
                            || tileProduction.equals(productionType.getProductionLevel()))) {
                        productionType.getOutputs().add(goods);
                    }
                }
            } else {
                addProductionType(new ProductionType(goods, false,
                                                     tileProduction));
            }
            closeTag(in, tag);
            // end @compat

        } else if (RESOURCE_TAG.equals(tag)) {
            addResourceType(spec.getType(in, TYPE_TAG, ResourceType.class,
                                         (ResourceType)null),
                            getAttribute(in, PROBABILITY_TAG, 100));
            closeTag(in, RESOURCE_TAG);

        } else {
            super.readChild(in);
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "tile-type".
     */
    public static String getXMLElementTagName() {
        return "tile-type";
    }
}
