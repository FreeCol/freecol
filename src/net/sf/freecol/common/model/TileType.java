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

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.option.StringOption;
import net.sf.freecol.common.util.RandomChoice;

public final class TileType extends FreeColGameObjectType {

    private static int nextIndex = 0;

    private String artBasic;
    private String artOverlay;
    private String artForest;

    private boolean forest;
    private boolean water;
    private boolean canSettle;

    private int basicMoveCost;
    private int basicWorkTurns;
    
    public static enum RangeType { HUMIDITY, TEMPERATURE, ALTITUDE }
    
    private int[] humidity = new int[2];
    private int[] temperature = new int[2];
    private int[] altitude = new int[2];

    private List<RandomChoice<ResourceType>> resourceType;


    /**
     * Whether this TileType is connected to Europe.
     */
    private boolean connected;

    /**
     * The primary goods produced by this tile type. In the original
     * game, this is always food or null (in the case of the arctic).
     */
    private AbstractGoods primaryGoods = null;

    /**
     * The secondary goods produced by this tile type. In the original
     * game, this is never food, but may be null (in the case of the
     * arctic).
     */
    private AbstractGoods secondaryGoods = null;

    /**
     * A list of AbstractGoods produced by this TileType when it is
     * not the colony center tile.
     */
    private List<AbstractGoods> production;

    private Map<String, AbstractGoods> primaryGoodsMap =
        new HashMap<String, AbstractGoods>();

    private Map<String, AbstractGoods> secondaryGoodsMap =
        new HashMap<String, AbstractGoods>();

    private Map<String, Map<GoodsType, AbstractGoods>> productionMap =
        new HashMap<String, Map<GoodsType, AbstractGoods>>();


    // ------------------------------------------------------------ constructor

    public TileType() {
        setIndex(nextIndex++);
    }

    // ------------------------------------------------------------ retrieval methods

    public String getArtBasic() {
        return artBasic;
    }

    public String getArtOverlay() {
        return artOverlay;
    }

    public String getArtForest() {
        return artForest;
    }

    public boolean isForested() {
        return forest;
    }

    public boolean isWater() {
        return water;
    }

    /**
     * Get the <code>Connected</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isConnected() {
        return connected;
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
        return getModifierSet("model.modifier.defence");
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
        return (int) featureContainer.applyModifier(0f, goodsType.getId(), unitType);
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
        return featureContainer.getModifierSet(goodsType.getId());
    }

    /**
     * Get the <code>PrimaryGoods</code> value.
     *
     * @return an <code>AbstractGoods</code> value
     */
    public AbstractGoods getPrimaryGoods() {
        return primaryGoods;
    }

    /**
     * Get the <code>PrimaryGoods</code> value at the tileProduction level
     * with the ID given.
     *
     * @return an <code>AbstractGoods</code> value
     */
    public AbstractGoods getPrimaryGoods(String tileProduction) {
        AbstractGoods result = primaryGoodsMap.get(tileProduction);
        if (result == null) {
            result = primaryGoodsMap.get(null);
        }
        return result;
    }

    /**
     * Returns true if the given <code>GoodsType</code> is the primary
     * goods type of this TileType.
     *
     * @param type a <code>GoodsType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isPrimaryGoodsType(GoodsType type) {
        return (primaryGoods != null && primaryGoods.getType() == type);
    }

    /**
     * Set the <code>PrimaryGoods</code> value.
     *
     * @param newPrimaryGoods The new PrimaryGoods value.
     */
    public void setPrimaryGoods(final AbstractGoods newPrimaryGoods) {
        this.primaryGoods = newPrimaryGoods;
    }

    /**
     * Get the <code>SecondaryGoods</code> value.
     *
     * @return an <code>AbstractGoods</code> value
     */
    public AbstractGoods getSecondaryGoods() {
        return secondaryGoods;
    }

    /**
     * Get the <code>SecondaryGoods</code> value at the tileProduction level
     * with the ID given.
     *
     * @return an <code>AbstractGoods</code> value
     */
    public AbstractGoods getSecondaryGoods(String tileProduction) {
        AbstractGoods result = secondaryGoodsMap.get(tileProduction);
        if (result == null) {
            result = secondaryGoodsMap.get(null);
        }
        return result;
    }

    /**
     * Set the <code>SecondaryGoods</code> value.
     *
     * @param newSecondaryGoods The new SecondaryGoods value.
     */
    public void setSecondaryGoods(final AbstractGoods newSecondaryGoods) {
        this.secondaryGoods = newSecondaryGoods;
    }

    /**
     * Returns true if the given <code>GoodsType</code> is the secondary
     * goods type of this TileType.
     *
     * @param type a <code>GoodsType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isSecondaryGoodsType(GoodsType type) {
        return (secondaryGoods != null && secondaryGoods.getType() == type);
    }

    /**
     * Returns a list of all types of AbstractGoods produced by this
     * TileType when it is not the colony center tile.
     *
     * @return a <code>List<AbstractGoods></code> value
     */
    public List<AbstractGoods> getProduction() {
        return production;
    }

    /**
     * Returns a list of all types of AbstractGoods produced by this
     * TileType when it is not the colony center tile.
     *
     * @param tileProduction
     * @return a <code>List<AbstractGoods></code> value
     */
    public List<AbstractGoods> getProduction(String tileProduction) {
        Map<GoodsType, AbstractGoods> result = new HashMap<GoodsType, AbstractGoods>();
        Map<GoodsType, AbstractGoods> defaultMap = productionMap.get(null);
        Map<GoodsType, AbstractGoods> difficultyMap = productionMap.get(tileProduction);
        if (defaultMap != null) {
            result.putAll(defaultMap);
        }
        if (difficultyMap != null) {
            result.putAll(difficultyMap);
        }
        return new ArrayList<AbstractGoods>(result.values());
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
     * @param difficulty difficulty level to apply
     */
    @Override
    public void applyDifficultyLevel(DifficultyLevel difficultyLevel) {
        String tileProduction = ((StringOption) difficultyLevel.getOption("model.option.tileProduction"))
            .getValue();
        primaryGoods = getPrimaryGoods(tileProduction);
        secondaryGoods = getSecondaryGoods(tileProduction);
        // remove old modifiers
        if (production != null) {
            for (AbstractGoods goods : production) {
                Modifier oldModifier = new Modifier(goods.getType().getId(), this, goods.getAmount(),
                                                    Modifier.Type.ADDITIVE);
                getFeatureContainer().removeModifier(oldModifier);
            }
        }
        production = getProduction(tileProduction);
        // add new modifiers
        for (AbstractGoods goods : production) {
            addModifier(new Modifier(goods.getType().getId(), this, goods.getAmount(),
                                     Modifier.Type.ADDITIVE));
        }
    }


    // ------------------------------------------------------------ API methods

    public void readAttributes(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        basicMoveCost = Integer.parseInt(in.getAttributeValue(null, "basic-move-cost"));
        basicWorkTurns = Integer.parseInt(in.getAttributeValue(null, "basic-work-turns"));
        forest = getAttribute(in, "is-forest", false);
        water = getAttribute(in, "is-water", false);
        canSettle = getAttribute(in, "can-settle", !water);
        connected = getAttribute(in, "is-connected", false);
    }
        
    public void readChildren(XMLStreamReader in, Specification specification)
        throws XMLStreamException {

        artBasic = null;
        production = new ArrayList<AbstractGoods>();
        resourceType = new ArrayList<RandomChoice<ResourceType>>();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("art".equals(childName)) {
                artBasic = in.getAttributeValue(null, "basic");
                artOverlay = in.getAttributeValue(null, "overlay");
                artForest = in.getAttributeValue(null, "forest");
                in.nextTag(); // close this element
            } else if ("gen".equals(childName)) {
                humidity[0] = getAttribute(in, "humidityMin", 0);
                humidity[1] = getAttribute(in, "humidityMax", 100);
                temperature[0] = getAttribute(in, "temperatureMin", -20);
                temperature[1] = getAttribute(in, "temperatureMax", 40);
                altitude[0] = getAttribute(in, "altitudeMin", 0);
                altitude[1] = getAttribute(in, "altitudeMax", 0);
                in.nextTag(); // close this element
            } else if ("production".equals(childName)
                       || "primary-production".equals(childName)
                       || "secondary-production".equals(childName)) {
                GoodsType type = specification.getGoodsType(in.getAttributeValue(null, "goods-type"));
                int amount = Integer.parseInt(in.getAttributeValue(null, "value"));
                AbstractGoods goods = new AbstractGoods(type, amount);
                String tileProduction = in.getAttributeValue(null, "tile-production");
                if ("primary-production".equals(childName)) {
                    primaryGoodsMap.put(tileProduction, goods);
                } else if ("secondary-production".equals(childName)) {
                    secondaryGoodsMap.put(tileProduction, goods);
                } else {
                    Map<GoodsType, AbstractGoods> oldValue = productionMap.get(tileProduction);
                    if (oldValue == null) {
                        oldValue = new HashMap<GoodsType, AbstractGoods>();
                        productionMap.put(tileProduction, oldValue);
                    }
                    oldValue.put(type, goods);
                }
                in.nextTag(); // close this element
            } else if ("resource".equals(childName)) {
                ResourceType type = specification.getResourceType(in.getAttributeValue(null, "type"));
                int probability = getAttribute(in, "probability", 100);
                resourceType.add(new RandomChoice<ResourceType>(type, probability));
                in.nextTag(); // close this element
            } else {
                super.readChild(in, specification);
            }
        }
        
        if (artBasic == null) {
            throw new RuntimeException("TileType " + getId() + " has no art defined!");
        }
    }

}
