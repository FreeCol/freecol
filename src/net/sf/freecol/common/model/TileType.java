/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.List;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.option.GameOptions;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.RandomChoice;


/**
 * The types of tiles.
 */
public final class TileType extends FreeColSpecObjectType
                            implements BaseProduction {

    public static final String TAG = "tile-type";

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
    private final int[] humidity = new int[2];
    /** The temperature range for this tile type. */
    private final int[] temperature = new int[2];
    /** The altitude range for this tile type. */
    private final int[] altitude = new int[2];

    /** The resource types that are valid for this tile type. */
    private List<RandomChoice<ResourceType>> resourceTypes = null;

    /** The disasters that may strike this type of tile. */
    private List<RandomChoice<Disaster>> disasters = null;

    /**
     * The possible production types of this tile type.  This includes
     * the production types available if a tile of this type is a
     * colony center tile.
     */
    private final List<ProductionType> productionTypes = new ArrayList<>();


    /**
     * Create a new tile type.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
     */
    public TileType(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Creates a new {@code TileType} instance. This constructor
     * is used to create the "virtual" tile types {@code LAND}
     * and {@code WATER}, which are intended to simplify map
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
     * @return True if the tile type is inherently connected to the
     *     high seas.
     */
    public boolean isHighSeasConnected() {
        return connected;
    }

    /**
     * Is this tile type directly connected to the high seas, that is,
     * a unit on a tile of this type can move immediately to the high
     * seas.
     *
     * @return True if the tile type is directly connected.
     */
    public boolean isDirectlyHighSeasConnected() {
        return hasAbility(Ability.MOVE_TO_EUROPE);
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

    protected int getHumidity(int i) {
        return this.humidity[i];
    }
    protected void setHumidity(int i, int value) {
        this.humidity[i] = value;
    }
    protected int getTemperature(int i) {
        return this.temperature[i];
    }
    protected void setTemperature(int i, int value) {
        this.temperature[i] = value;
    }
    protected int getAltitude(int i) {
        return this.altitude[i];
    }
    protected void setAltitude(int i, int value) {
        this.altitude[i] = value;
    }
    
    /**
     * Is this tile type suitable for a given range type value.
     *
     * @param rangeType The {@code RangeType} to test.
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
     * Gets the resources that can be placed on this tile type.
     *
     * @return A weighted list of resource types.
     */
    public List<RandomChoice<ResourceType>> getResourceTypes() {
        if (this.resourceTypes == null) this.resourceTypes = new ArrayList<>();
        return this.resourceTypes;
    }

    /**
     * Set the resource types list.
     *
     * @param resourceTypes The list of resource type choices.
     */
    protected void setResourceTypes(List<RandomChoice<ResourceType>> resourceTypes) {
        if (this.resourceTypes == null) {
            this.resourceTypes = new ArrayList<>();
        } else {
            this.resourceTypes.clear();
        }
        this.resourceTypes.addAll(resourceTypes);
    }

    /**
     * Gets the resource types that can be found on this tile type.
     *
     * @return A list of {@code ResourceType}s.
     */
    public List<ResourceType> getResourceTypeValues() {
        return transform(getResourceTypes(), alwaysTrue(),
                         RandomChoice::getObject);
    }

    /**
     * Add a resource type.
     *
     * @param type The {@code ResourceType} to add.
     * @param prob The percentage probability of the resource being
     *     present.
     */
    private void addResourceType(ResourceType type, int prob) {
        if (this.resourceTypes == null) this.resourceTypes = new ArrayList<>();
        this.resourceTypes.add(new RandomChoice<>(type, prob));
    }

    /**
     * Can this tile type contain a specified resource type?
     *
     * @param resourceType The {@code ResourceType} to test.
     * @return True if the {@code ResourceType} is compatible.
     */
    public boolean canHaveResourceType(ResourceType resourceType) {
        return getResourceTypeValues().contains(resourceType);
    }

    /**
     * Get the disaster choices.
     *
     * @return A list of {@code Disaster} choices.
     */
    protected List<RandomChoice<Disaster>> getDisasters() {
        return this.disasters;
    }

    /**
     * Set the disaster choices.
     *
     * @param disasters The new {@code Disaster} choice list.
     */
    protected void setDisasters(List<RandomChoice<Disaster>> disasters) {
        if (this.disasters == null) {
            this.disasters = new ArrayList<>();
        } else {
            this.disasters.clear();
        }
        this.disasters.addAll(disasters);
    }

    /**
     * Gets the natural disasters than can strike this tile type.
     *
     * @return A stream of {@code Disaster} choices.
     */
    public Stream<RandomChoice<Disaster>> getDisasterChoices() {
        return (this.disasters == null)
            ? Stream.<RandomChoice<Disaster>>empty()
            : disasters.stream();
    }

    /**
     * Add a disaster.
     *
     * @param disaster The {@code Disaster} to add.
     * @param probability The probability of the disaster.
     */
    private void addDisaster(Disaster disaster, int probability) {
        if (disasters == null) disasters = new ArrayList<>();
        disasters.add(new RandomChoice<>(disaster, probability));
    }

    /**
     * Get the production type list.
     *
     * @return The {@code ProductionType} list.
     */
    protected List<ProductionType> getProductionTypes() {
        return this.productionTypes;
    }
    
    /**
     * Set the production type list.
     *
     * @param productionTypes The new {@code ProductionType} list.
     */
    protected void setProductionTypes(List<ProductionType> productionTypes) {
        this.productionTypes.clear();
        this.productionTypes.addAll(productionTypes);
    }
    
    /**
     * Gets the production types available at the current difficulty
     * level.
     *
     * @param unattended Whether the production is unattended.
     * @return A list of {@code ProductionType}s.
     */
    public List<ProductionType> getAvailableProductionTypes(boolean unattended) {
        return getAvailableProductionTypes(unattended,
            getSpecification().getString(GameOptions.TILE_PRODUCTION));
    }

    /**
     * Gets the production types available for the given combination
     * of colony center tile and production level.  If the production
     * level is null, all production levels will be returned.
     *
     * Public for the test suite.
     *
     * @param unattended Whether the production is unattended.
     * @param level The production level.
     * @return A list of {@code ProductionType}s.
     */
    public List<ProductionType> getAvailableProductionTypes(boolean unattended,
                                                            String level) {
        List<ProductionType> good = new ArrayList<>(),
            better = new ArrayList<>();
        for (ProductionType productionType : transform(productionTypes,
                matchKey(unattended, ProductionType::getUnattended))) {
            if (productionType.appliesExactly(level)) {
                better.add(productionType);
            } else if (productionType.appliesTo(level)) {
                good.add(productionType);
            }
        }
        return (!better.isEmpty()) ? better : good;
    }


    // Utilities

    /**
     * Can a tile of this type produce a given goods type?
     *
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType An optional {@code UnitType} that is to do
     *     the work, if null the unattended production is considered.
     * @return True if this tile type produces the goods.
     */
    public boolean canProduce(GoodsType goodsType, UnitType unitType) {
        return goodsType != null
            && ProductionType.canProduce(goodsType,
                getAvailableProductionTypes(unitType == null));
    }

    /**
     * Get the amount of goods of given goods type the given unit type
     * could produce on a tile of this tile type.
     *
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType An optional {@code UnitType} that is to do
     *     the work, if null the unattended production is considered.
     * @return The amount of goods produced.
     */
    public int getPotentialProduction(GoodsType goodsType,
                                      UnitType unitType) {
        if (goodsType == null) return 0;
        int amount = getBaseProduction(null, goodsType, unitType);
        amount = (int)apply(amount, null, goodsType.getId(), unitType);
        return (amount < 0) ? 0 : amount;
    }

    /**
     * Get all possible goods produced at a tile of this type.
     *
     * Used by static tile type displays that just list unattended
     * production values.  Planning and production routines should use
     * {@link #getPotentialProduction(GoodsType, UnitType)}
     *
     * @param unattended Select unattended production.
     * @return A stream of produced {@code AbstractGoods}.
     */
    public Stream<AbstractGoods> getPossibleProduction(boolean unattended) {
        return flatten(getAvailableProductionTypes(unattended),
                       ProductionType::getOutputs);
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        TileType o = copyInCast(other, TileType.class);
        if (o == null || !super.copyIn(o)) return false;
        this.forest = o.isForested();
        this.water = o.isWater();
        this.canSettle = o.canSettle();
        this.connected = o.isHighSeasConnected();
        this.elevation = o.isElevation();
        this.basicMoveCost = o.getBasicMoveCost();
        this.basicWorkTurns = o.getBasicWorkTurns();
        for (int i = 0; i < this.humidity.length; i++) {
            this.setHumidity(i, o.getHumidity(i));
        }
        for (int i = 0; i < this.temperature.length; i++) {
            this.setTemperature(i, o.getTemperature(i));
        }
        for (int i = 0; i < this.altitude.length; i++) {
            this.setAltitude(i, o.getAltitude(i));
        }
        this.setResourceTypes(o.getResourceTypes());
        this.setDisasters(o.getDisasters());
        this.setProductionTypes(o.getProductionTypes());
        return true;
    }


    // Serialization

    private static final String ALTITUDE_MIN_TAG = "altitude-minimum";
    private static final String ALTITUDE_MAX_TAG = "altitude-maximum";
    private static final String BASIC_MOVE_COST_TAG = "basic-move-cost";
    private static final String BASIC_WORK_TURNS_TAG = "basic-work-turns";
    private static final String CAN_SETTLE_TAG = "can-settle";
    private static final String DISASTER_TAG = "disaster";
    private static final String GEN_TAG = "gen";
    private static final String GOODS_TYPE_TAG = "goods-type";
    private static final String HUMIDITY_MIN_TAG = "humidity-minimum";
    private static final String HUMIDITY_MAX_TAG = "humidity-maximum";
    private static final String IS_CONNECTED_TAG = "is-connected";
    private static final String IS_ELEVATION_TAG = "is-elevation";
    private static final String IS_FOREST_TAG = "is-forest";
    private static final String IS_WATER_TAG = "is-water";
    private static final String PROBABILITY_TAG = "probability";
    private static final String PRODUCTION_TAG = "production";
    private static final String RESOURCE_TAG = "resource";
    private static final String TEMPERATURE_MIN_TAG = "temperature-minimum";
    private static final String TEMPERATURE_MAX_TAG = "temperature-maximum";
    private static final String TYPE_TAG = "type";
    // @compat 0.11.x
    private static final String PRIMARY_PRODUCTION_TAG = "primary-production";
    private static final String SECONDARY_PRODUCTION_TAG = "secondary-production";
    // end @compat 0.11.x
    // @compat 0.11.3
    private static final String OLD_ALTITUDE_MIN_TAG = "altitudeMin";
    private static final String OLD_ALTITUDE_MAX_TAG = "altitudeMax";
    private static final String OLD_HUMIDITY_MIN_TAG = "humidityMin";
    private static final String OLD_HUMIDITY_MAX_TAG = "humidityMax";
    private static final String OLD_TEMPERATURE_MIN_TAG = "temperatureMin";
    private static final String OLD_TEMPERATURE_MAX_TAG = "temperatureMax";
    // end @compat 0.11.3


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(BASIC_MOVE_COST_TAG, this.basicMoveCost);

        xw.writeAttribute(BASIC_WORK_TURNS_TAG, this.basicWorkTurns);

        xw.writeAttribute(IS_FOREST_TAG, this.forest);

        xw.writeAttribute(IS_WATER_TAG, this.water);

        xw.writeAttribute(IS_ELEVATION_TAG, this.elevation);

        xw.writeAttribute(IS_CONNECTED_TAG, this.connected);

        xw.writeAttribute(CAN_SETTLE_TAG, this.canSettle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        xw.writeStartElement(GEN_TAG);

        xw.writeAttribute(HUMIDITY_MIN_TAG, this.humidity[0]);

        xw.writeAttribute(HUMIDITY_MAX_TAG, this.humidity[1]);

        xw.writeAttribute(TEMPERATURE_MIN_TAG, this.temperature[0]);

        xw.writeAttribute(TEMPERATURE_MAX_TAG, this.temperature[1]);

        xw.writeAttribute(ALTITUDE_MIN_TAG, this.altitude[0]);

        xw.writeAttribute(ALTITUDE_MAX_TAG, this.altitude[1]);

        xw.writeEndElement();

        for (ProductionType productionType : this.productionTypes) {
            productionType.toXML(xw);
        }

        if (this.resourceTypes != null) {
            for (RandomChoice<ResourceType> choice : this.resourceTypes) {
                xw.writeStartElement(RESOURCE_TAG);

                xw.writeAttribute(TYPE_TAG, choice.getObject());

                xw.writeAttribute(PROBABILITY_TAG, choice.getProbability());

                xw.writeEndElement();
            }
        }

        for (RandomChoice<Disaster> choice : iterable(getDisasterChoices())) {
            xw.writeStartElement(DISASTER_TAG);

            xw.writeAttribute(ID_ATTRIBUTE_TAG, choice.getObject());

            xw.writeAttribute(PROBABILITY_TAG, choice.getProbability());

            xw.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        this.basicMoveCost = xr.getAttribute(BASIC_MOVE_COST_TAG, 1);

        this.basicWorkTurns = xr.getAttribute(BASIC_WORK_TURNS_TAG, 1);

        this.forest = xr.getAttribute(IS_FOREST_TAG, false);

        this.water = xr.getAttribute(IS_WATER_TAG, false);

        this.elevation = xr.getAttribute(IS_ELEVATION_TAG, false);

        this.canSettle = xr.getAttribute(CAN_SETTLE_TAG, !water);

        this.connected = xr.getAttribute(IS_CONNECTED_TAG, false);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (xr.shouldClearContainers()) {
            this.disasters = null;
            this.resourceTypes = null;
            this.productionTypes.clear();
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

        if (DISASTER_TAG.equals(tag)) {
            Disaster d = xr.getType(spec, ID_ATTRIBUTE_TAG,
                                    Disaster.class, (Disaster)null);
            if (d != null) {
                addDisaster(d, xr.getAttribute(PROBABILITY_TAG, 100));
            }
            xr.closeTag(DISASTER_TAG);

        } else if (GEN_TAG.equals(tag)) {
            this.humidity[0] = xr.getAttribute(HUMIDITY_MIN_TAG, 0);
            this.humidity[1] = xr.getAttribute(HUMIDITY_MAX_TAG, 100);
            this.temperature[0] = xr.getAttribute(TEMPERATURE_MIN_TAG, -20);
            this.temperature[1] = xr.getAttribute(TEMPERATURE_MAX_TAG, 40);
            this.altitude[0] = xr.getAttribute(ALTITUDE_MIN_TAG, 0);
            this.altitude[1] = xr.getAttribute(ALTITUDE_MAX_TAG, 0);
            // @compat 0.11.3
            if (xr.hasAttribute(OLD_HUMIDITY_MIN_TAG)) {
                this.humidity[0] = xr.getAttribute(OLD_HUMIDITY_MIN_TAG, 0);
            }
            if (xr.hasAttribute(OLD_HUMIDITY_MAX_TAG)) {
                this.humidity[1] = xr.getAttribute(OLD_HUMIDITY_MAX_TAG, 100);
            }
            if (xr.hasAttribute(OLD_TEMPERATURE_MIN_TAG)) {
                this.temperature[0] = xr.getAttribute(OLD_TEMPERATURE_MIN_TAG, -20);
            }
            if (xr.hasAttribute(OLD_TEMPERATURE_MAX_TAG)) {
                this.temperature[1] = xr.getAttribute(OLD_TEMPERATURE_MAX_TAG, 40);
            }
            if (xr.hasAttribute(OLD_ALTITUDE_MIN_TAG)) {
                this.altitude[0] = xr.getAttribute(OLD_ALTITUDE_MIN_TAG, 0);
            }
            if (xr.hasAttribute(OLD_ALTITUDE_MAX_TAG)) {
                this.altitude[1] = xr.getAttribute(OLD_ALTITUDE_MAX_TAG, 0);
            }
            // end @compat 0.11.3
            xr.closeTag(GEN_TAG);

        } else if (PRODUCTION_TAG.equals(tag)
            && xr.getAttribute(DELETE_TAG, false)) {
            this.productionTypes.clear();
            xr.closeTag(PRODUCTION_TAG);

        } else if (PRODUCTION_TAG.equals(tag)) {
            this.productionTypes.add(new ProductionType(xr, spec));

        } else if (RESOURCE_TAG.equals(tag)) {
            addResourceType(xr.getType(spec, TYPE_TAG, ResourceType.class,
                                       (ResourceType)null),
                            xr.getAttribute(PROBABILITY_TAG, 100));
            xr.closeTag(RESOURCE_TAG);

        // @compat 0.11.x
        // Primary and secondary production was dropped at 0.11.0, but
        // some saved games slipped through.
        } else if (PRIMARY_PRODUCTION_TAG.equals(tag)
            || SECONDARY_PRODUCTION_TAG.equals(tag)) {
            GoodsType type = xr.getType(spec, GOODS_TYPE_TAG,
                                        GoodsType.class, (GoodsType)null);
            int amount = xr.getAttribute(VALUE_TAG, 0);
            ProductionType pt = new ProductionType(null, type, amount);
            pt.setUnattended(true);
            this.productionTypes.add(pt);
            xr.closeTag(tag);
        // @end compat 0.11.x

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
