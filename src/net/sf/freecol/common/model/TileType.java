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

    /**
     * Use these tile types only for "land maps", i.e. maps that only
     * distinguish water and land.
     */
    public static final TileType WATER = new TileType("WATER", true);
    public static final TileType LAND  = new TileType("LAND", false);

    private boolean forest;
    private boolean water;
    private boolean canSettle;
    private boolean elevation;

    private int basicMoveCost;
    private int basicWorkTurns;

    public static enum RangeType { HUMIDITY, TEMPERATURE, ALTITUDE }

    private int[] humidity = new int[2];
    private int[] temperature = new int[2];
    private int[] altitude = new int[2];

    private List<RandomChoice<ResourceType>> resourceType =
        new ArrayList<RandomChoice<ResourceType>>();


    /**
     * Whether this TileType is connected to Europe.
     */
    private boolean connected;

    /**
     * The disasters that may strike this type of tile.
     */
    private List<RandomChoice<Disaster>> disasters
        = new ArrayList<RandomChoice<Disaster>>();

    /**
     * The possible production types of this tile type. This includes
     * the production types available if a tile of this type is a
     * colony center tile.
     */
    private final List<ProductionType> productionTypes
        = new ArrayList<ProductionType>();

    // TODO: make this hack go away!
    private String productionLevel = null;


    // ------------------------------------------------------------ constructor

    public TileType(String id, Specification specification) {
        super(id, specification);
    }

    private TileType(String id, boolean water) {
        super(id, null);
        this.water = water;
    }

    // ------------------------------------------------------------ retrieval methods

    /**
     * Returns the index of this FreeColGameObjectType. The index
     * imposes a total ordering consistent with equals on each class
     * extending FreeColGameObjectType, but this ordering is nothing
     * but the order in which the objects of the respective class were
     * defined. It is guaranteed to remain stable only for a
     * particular revision of a particular specification.
     *
     * @return an <code>int</code> value
     */
    @Override
    public int getIndex() {
        return super.getIndex();
    }

    public boolean isForested() {
        return forest;
    }

    public boolean isWater() {
        return water;
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
     * Is this tile an elevation.
     *
     * @return <tt>true</tt> if and only if the tile is an elevation, <tt>false</tt> otherwise.
     */
    public boolean isElevation() {
    	return elevation;
    }

    public boolean canSettle() {
        return canSettle;
    }

    /**
     * Returns true if this TileType supports the given TileImprovementType.
     *
     * @param improvement a <code>TileImprovementType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean canHaveImprovement(TileImprovementType improvement) {
        return (improvement != null && improvement.isTileTypeAllowed(this));
    }

    public int getBasicMoveCost() {
        return basicMoveCost;
    }

    public int getBasicWorkTurns() {
        return basicWorkTurns;
    }

    public Set<Modifier> getDefenceBonus() {
        return getModifierSet(Modifier.DEFENCE);
    }

    /**
     * Returns the amount of goods of given GoodsType this TileType
     * can produce. This method applies the production bonus to
     * <code>0f</code>. Thus, it will always return <code>0</code>
     * unless an additive modifier is present. This is intentional.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @param unitType an <code>UnitType</code> value
     * @return an <code>int</code> value
     * @see #getProductionBonus(GoodsType)
     */
    public int getProductionOf(GoodsType goodsType, UnitType unitType) {
        return (int)applyModifier(0f, goodsType.getId(), unitType);
        /*
        Set<Modifier> result = featureContainer.getModifierSet(goodsType.getId(), unitType);
        if (unitType != null && !result.isEmpty()) {
            result.addAll(unitType.getFeatureContainer().getModifierSet(goodsType.getId()));
        }
        return (int) featureContainer.applyModifierSet(0f, null, result);
        */
    }

    /**
     * Returns the production bonus for the given GoodsType.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return a <code>Modifier</code> value
     */
    public Set<Modifier> getProductionBonus(GoodsType goodsType) {
        return getModifierSet(goodsType.getId());
    }

    /**
     * Returns the production types available for the given combination
     * of colony center tile and production level. If the production
     * level is null, all production levels will be returned.
     *
     * @param center whether the tile is a colony center tile
     * @param level the production level
     * @return a <code>List<ProductionType></code> value
     */
    public List<ProductionType> getProductionTypes(boolean center, String level) {
        List<ProductionType> result = new ArrayList<ProductionType>();
        for (ProductionType productionType : productionTypes) {
            if (productionType.isColonyCenterTile() == center
                && productionType.appliesTo(level)) {
                result.add(productionType);
            }
        }
        return result;
    }

    public List<ProductionType> getProductionTypes() {
        return productionTypes;
    }

    /**
     * Get the <code>PrimaryGoods</code> value.
     *
     * @return an <code>AbstractGoods</code> value
     */
    public AbstractGoods getPrimaryGoods() {
        List<ProductionType> production = getProductionTypes(true, productionLevel);
        if (production == null || production.isEmpty()) {
            return null;
        } else {
            List<AbstractGoods> outputs = production.get(0).getOutputs();
            if (outputs == null || outputs.isEmpty()) {
                return null;
            } else {
                return outputs.get(0);
            }
        }
    }

    /**
     * Returns true if the given <code>GoodsType</code> is the primary
     * goods type of this TileType.
     *
     * @param type a <code>GoodsType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isPrimaryGoodsType(GoodsType type) {
        AbstractGoods primaryGoods = getPrimaryGoods();
        return (primaryGoods != null && primaryGoods.getType() == type);
    }

    /**
     * Get the <code>SecondaryGoods</code> value.
     *
     * @return an <code>AbstractGoods</code> value
     */
    public AbstractGoods getSecondaryGoods() {
        List<ProductionType> production = getProductionTypes(true, productionLevel);
        if (production == null || production.isEmpty()) {
            return null;
        } else {
            List<AbstractGoods> outputs = production.get(0).getOutputs();
            if (outputs == null || outputs.size() < 2) {
                return null;
            } else {
                return outputs.get(1);
            }
        }
    }

    /**
     * Returns true if the given <code>GoodsType</code> is the secondary
     * goods type of this TileType.
     *
     * @param type a <code>GoodsType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isSecondaryGoodsType(GoodsType type) {
        AbstractGoods secondaryGoods = getSecondaryGoods();
        return (secondaryGoods != null && secondaryGoods.getType() == type);
    }


    /**
     * Returns a list of all types of AbstractGoods produced by this
     * TileType when it is not the colony center tile.
     *
     * @return a <code>List<AbstractGoods></code> value
     */
    public List<AbstractGoods> getProduction() {
        List<AbstractGoods> production = new ArrayList<AbstractGoods>();
        for (ProductionType productionType : getProductionTypes(false, productionLevel)) {
            List<AbstractGoods> outputs = productionType.getOutputs();
            if (!(outputs == null || outputs.isEmpty())) {
                production.addAll(outputs);
            }
        }
        return production;
    }


    public List<RandomChoice<ResourceType>> getWeightedResources() {
        return resourceType;
    }

    public List<ResourceType> getResourceTypeList() {
        List<ResourceType> result = new ArrayList<ResourceType>();
        for (RandomChoice<ResourceType> resource : resourceType) {
            result.add(resource.getObject());
        }
        return result;
    }

    /**
     * Return a weighted list of natural disasters than can strike
     * this tile type.
     *
     * @return a <code>List<RandomChoice<Disaster>></code> value
     */
    public List<RandomChoice<Disaster>> getDisasters() {
        return disasters;
    }

    /**
     * Set the <code>Disasters</code> value.
     *
     * @param newDisasters The new Disasters value.
     */
    public void setDisasters(final List<RandomChoice<Disaster>> newDisasters) {
        this.disasters = newDisasters;
    }

    /**
     * Can this <code>TileType</code> contain a specified <code>ResourceType</code>?
     *
     * @param resourceType a <code>ResourceType</code> to test
     * @return Whether this <code>TileType</code> contains the specified <code>ResourceType</code>
     */
    public boolean canHaveResourceType(ResourceType resourceType) {
        return getResourceTypeList().contains(resourceType);
    }

    public boolean withinRange(RangeType rangeType, int value) {
        switch (rangeType) {
        case HUMIDITY:
            return (humidity[0] <= value && value <= humidity[1]);
        case TEMPERATURE:
            return (temperature[0] <= value && value <= temperature[1]);
        case ALTITUDE:
            return (altitude[0] <= value && value <= altitude[1]);
        default:
            return false;
        }
    }

    /**
     * Applies the difficulty level to this TileType.
     *
     * @param difficultyLevel difficulty level to apply
     */
    @Override
    public void applyDifficultyLevel(OptionGroup difficultyLevel) {
        productionLevel = ((StringOption) difficultyLevel.getOption("model.option.tileProduction"))
            .getValue();
        // remove old modifiers
        for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
            removeModifiers(goodsType.getId());
        }
        // add new modifiers
        for (ProductionType productionType : productionTypes) {
            if (productionType.appliesTo(productionLevel)) {
                List<AbstractGoods> outputs = productionType.getOutputs();
                if (!(outputs == null || outputs.isEmpty())) {
                    for (AbstractGoods goods : outputs) {
                        addModifier(new Modifier(goods.getType().getId(), this,
                                                 goods.getAmount(),
                                                 Modifier.Type.ADDITIVE));
                    }
                }
            }
        }
    }

   /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("basic-move-cost", Integer.toString(basicMoveCost));
        out.writeAttribute("basic-work-turns", Integer.toString(basicWorkTurns));
        out.writeAttribute("is-forest", Boolean.toString(forest));
        out.writeAttribute("is-water", Boolean.toString(water));
        out.writeAttribute("is-elevation", Boolean.toString(elevation));
        out.writeAttribute("is-connected", Boolean.toString(connected));
        out.writeAttribute("can-settle", Boolean.toString(canSettle));
        out.writeAttribute("productionLevel", productionLevel);
    }

    /**
     * Write the children of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    @Override
    protected void writeChildren(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeChildren(out);

        out.writeStartElement("gen");
        out.writeAttribute("humidityMin", Integer.toString(humidity[0]));
        out.writeAttribute("humidityMax", Integer.toString(humidity[1]));
        out.writeAttribute("temperatureMin", Integer.toString(temperature[0]));
        out.writeAttribute("temperatureMax", Integer.toString(temperature[1]));
        out.writeAttribute("altitudeMin", Integer.toString(altitude[0]));
        out.writeAttribute("altitudeMax", Integer.toString(altitude[1]));
        out.writeEndElement();

        for (ProductionType productionType : productionTypes) {
            productionType.toXML(out);
        }

        for (RandomChoice<ResourceType> choice : resourceType) {
            out.writeStartElement("resource");
            out.writeAttribute("type", choice.getObject().getId());
            out.writeAttribute("probability",
                Integer.toString(choice.getProbability()));
            out.writeEndElement();
        }

        for (RandomChoice<Disaster> choice : disasters) {
            out.writeStartElement("disaster");
            out.writeAttribute("id", choice.getObject().getId());
            out.writeAttribute("probability",
                Integer.toString(choice.getProbability()));
            out.writeEndElement();
        }
    }

    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        basicMoveCost = Integer.parseInt(in.getAttributeValue(null,
                "basic-move-cost"));
        basicWorkTurns = Integer.parseInt(in.getAttributeValue(null,
                "basic-work-turns"));
        forest = getAttribute(in, "is-forest", false);
        water = getAttribute(in, "is-water", false);
        elevation = getAttribute(in, "is-elevation", false);
        canSettle = getAttribute(in, "can-settle", !water);
        connected = getAttribute(in, "is-connected", false);
        productionLevel = in.getAttributeValue(null, "productionLevel");
    }

    /**
     * Reads a child object.
     *
     * @param in The XML stream to read.
     * @exception XMLStreamException if an error occurs
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        String childName = in.getLocalName();
        if ("gen".equals(childName)) {
            humidity[0] = getAttribute(in, "humidityMin", 0);
            humidity[1] = getAttribute(in, "humidityMax", 100);
            temperature[0] = getAttribute(in, "temperatureMin", -20);
            temperature[1] = getAttribute(in, "temperatureMax", 40);
            altitude[0] = getAttribute(in, "altitudeMin", 0);
            altitude[1] = getAttribute(in, "altitudeMax", 0);
            in.nextTag(); // close this element
        } else if ("production".equals(childName)
                   && in.getAttributeValue(null, "goods-type") == null) {
            // new production style
            ProductionType productionType = new ProductionType(getSpecification());
            productionType.readFromXML(in);
            productionTypes.add(productionType);
        } else if ("production".equals(childName)
                   || "primary-production".equals(childName)
                   || "secondary-production".equals(childName)) {
            // @compat 0.10.6
            GoodsType type = getSpecification().getGoodsType(in.getAttributeValue(null, "goods-type"));
            int amount = Integer.parseInt(in.getAttributeValue(null, VALUE_TAG));
            AbstractGoods goods = new AbstractGoods(type, amount);
            String tileProduction = in.getAttributeValue(null, "tile-production");
            // CAUTION: this only works if the primary production is
            // defined before the secondary production
            if ("primary-production".equals(childName)) {
                productionTypes.add(new ProductionType(goods, true, tileProduction));
            } else if ("secondary-production".equals(childName)) {
                for (ProductionType productionType : productionTypes) {
                    if (productionType.isColonyCenterTile()
                        && (tileProduction == null
                            || tileProduction.equals(productionType.getProductionLevel()))) {
                        productionType.getOutputs().add(goods);
                    }
                }
            } else {
                productionTypes.add(new ProductionType(goods, false, tileProduction));
            }
            in.nextTag(); // close this element
            // end compat
        } else if ("resource".equals(childName)) {
            ResourceType type = getSpecification().getResourceType(in.getAttributeValue(null, "type"));
            int probability = getAttribute(in, "probability", 100);
            resourceType.add(new RandomChoice<ResourceType>(type, probability));
            in.nextTag(); // close this element
        } else if ("disaster".equals(childName)) {
            Disaster disaster = getSpecification().getDisaster(in.getAttributeValue(null, "id"));
            int probability = getAttribute(in, "probability", 100);
            disasters.add(new RandomChoice<Disaster>(disaster, probability));
            in.nextTag(); // close this element
        } else {
            super.readChild(in);
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "tile-type".
     */
    public static String getXMLElementTagName() {
        return "tile-type";
    }
}
