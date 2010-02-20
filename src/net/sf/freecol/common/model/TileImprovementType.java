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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.Specification;

public final class TileImprovementType extends FreeColGameObjectType {

    private static int nextIndex = 0;

    private boolean natural;
    private String typeId;
    private int magnitude;
    private int addWorkTurns;

    private String artOverlay;

    private List<TileType> allowedTileTypes;
    private TileImprovementType requiredImprovementType;

    private Set<String> allowedWorkers;
    private EquipmentType expendedEquipmentType;
    private int expendedAmount;
    private GoodsType deliverGoodsType;
    private int deliverAmount;

    private Map<TileType, TileType> tileTypeChange = new HashMap<TileType, TileType>();

    private int movementCost;
    private float movementCostFactor;

    /**
     * The layer a TileItem belongs to. Items with higher zIndex
     * will be displayed above items with a lower zIndex. E.g. the
     * LostCityRumour will be displayed above the Plow improvement.
     */
    private int zIndex;

    // ------------------------------------------------------------ constructors

    public TileImprovementType() {
        setIndex(nextIndex++);
    }

    // ------------------------------------------------------------ retrieval methods

    public boolean isNatural() {
        return natural;
    }

    public int getMagnitude() {
        return magnitude;
    }

    public int getAddWorkTurns() {
        return addWorkTurns;
    }

    public String getArtOverlay() {
        return artOverlay;
    }

    /**
     * Get the <code>ZIndex</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getZIndex() {
        return zIndex;
    }

    /**
     * Set the <code>ZIndex</code> value.
     *
     * @param newZIndex The new ZIndex value.
     */
    public void setZIndex(final int newZIndex) {
        this.zIndex = newZIndex;
    }

    public TileImprovementType getRequiredImprovementType() {
        return requiredImprovementType;
    }

    public EquipmentType getExpendedEquipmentType() {
        return expendedEquipmentType;
    }

    public int getExpendedAmount() {
        return expendedAmount;
    }

    public GoodsType getDeliverGoodsType() {
        return deliverGoodsType;
    }

    public int getDeliverAmount() {
        return deliverAmount;
    }

    public boolean isWorkerTypeAllowed(UnitType unitType) {
        return allowedWorkers.isEmpty() || allowedWorkers.contains(unitType.getId());
    }

    /**
     * Check if a given <code>Unit</code> can perform this TileImprovement.
     * @return true if Worker UnitType is allowed and expended Goods are available
     */
    public boolean isWorkerAllowed(Unit unit) {
        if (!isWorkerTypeAllowed(unit.getType())) {
            return false;
        }
        return (unit.getEquipment().getCount(expendedEquipmentType) >= expendedAmount);
    }

    /**
     * This will check if in principle this type of improvement can be used on
     * this kind of tile, disregarding the current state of an actual tile.
     *
     * If you want to find out if an improvement is allowed for a tile, call
     * {@link #isTileAllowed(Tile)}.
     *
     * @param tileType The type of terrain
     * @return true if improvement is possible
     */
    public boolean isTileTypeAllowed(TileType tileType) {
        return (allowedTileTypes.indexOf(tileType) >= 0);
    }

    /**
     * Check if a given <code>Tile</code> is valid for this TileImprovement.
     *
     * @return true if Tile TileType is valid and required Improvement (if any)
     *         is present.
     */
    public boolean isTileAllowed(Tile tile) {
        if (!isTileTypeAllowed(tile.getType())) {
            return false;
        }
        if (requiredImprovementType != null && tile.findTileImprovementType(requiredImprovementType) == null) {
            return false;
        }
        TileImprovement ti = tile.findTileImprovementType(this);
        return ti == null || !ti.isComplete();
    }

    public int getBonus(GoodsType goodsType) {
        Modifier result = getProductionModifier(goodsType);
        if (result == null) {
            return 0;
        } else {
            return (int) result.getValue();
        }
    }

    public Modifier getProductionModifier(GoodsType goodsType) {
        Set<Modifier> modifierSet = featureContainer.getModifierSet(goodsType.getId());
        if (modifierSet == null || modifierSet.isEmpty()) {
            return null;
        } else {
            if (modifierSet.size() > 1) {
                logger.warning("Only one Modifier for " + goodsType.getId() + " expected!");
            }
            return modifierSet.iterator().next();
        }
    }

    public TileType getChange(TileType tileType) {
        return tileTypeChange.get(tileType);
    }

    public boolean changeContainsTarget(TileType tileType) {
        return tileTypeChange.containsValue(tileType);
    }

    /**
     * Returns a value for use in AI decision making.
     * @param tileType The <code>TileType</code> to be considered. A <code>null</code> entry
     *        denotes no interest in a TileImprovementType that changes TileTypes
     * @param goodsType A preferred <code>GoodsType</code> or <code>null</code>
     * @return Sum of all bonuses with a triple bonus for the preferred GoodsType
     */
    public int getValue(TileType tileType, GoodsType goodsType) {
        int value = 0;
        if (goodsType.isFarmed()) {
            TileType newTileType = getChange(tileType);
            // 2 main types TileImprovementTypes - Changing of TileType and Simple Bonus
            if (newTileType != null) {
                int change = newTileType.getProductionOf(goodsType, null)
                    - tileType.getProductionOf(goodsType, null);
                if (change > 0) {
                    value += change * 3;
                }
            } else if (tileType.getProductionOf(goodsType, null) > 0) {
                // Calculate bonuses from TileImprovementType
                for (Modifier modifier : featureContainer.getModifiers()) {
                    float change = modifier.applyTo(1);
                    if (modifier.getId().equals(goodsType.getId())) {
                        if (change > 1) {
                            value += change * 3;
                        }
                    }
                }
            }
        }
        return value;
    }

    /**
     * Performs reduction of the movement-cost.
     * @param moveCost Original movement cost
     * @return The movement cost after any change
     */
    public int getMovementCost(int moveCost) {
        int cost = moveCost;
        if (movementCostFactor >= 0) {
            float cost2 = cost * movementCostFactor;
            cost = (int)cost2;
            if (cost < cost2) {
                cost++;
            }
        }
        if (movementCost >= 0) {
            if (movementCost < cost) {
                return movementCost;
            } else {
                return cost;
            }
        }
        return cost;
    }

    // ------------------------------------------------------------ API methods

    public void readAttributes(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        natural = getAttribute(in, "natural", false);
        addWorkTurns = getAttribute(in, "add-work-turns", 0);
        movementCost = getAttribute(in, "movement-cost", -1);
        movementCostFactor = -1;
        magnitude = getAttribute(in, "magnitude", 1);

        requiredImprovementType = specification.getType(in, "required-improvement", 
                                                        TileImprovementType.class, null);

        artOverlay = getAttribute(in, "overlay", null);
        zIndex = getAttribute(in, "zIndex", 0);

        expendedEquipmentType = specification.getType(in, "expended-equipment-type", EquipmentType.class, null);
        expendedAmount = getAttribute(in, "expended-amount", 0);
        deliverGoodsType = specification.getType(in, "deliver-goods-type", GoodsType.class, null);
        deliverAmount = getAttribute(in, "deliver-amount", 0);
    }


    public void readChildren(XMLStreamReader in, Specification specification)
        throws XMLStreamException {

        allowedWorkers = new HashSet<String>();
        allowedTileTypes = new ArrayList<TileType>();
        tileTypeChange = new HashMap<TileType, TileType>();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("tiles".equals(childName)) {
                boolean allLand = getAttribute(in, "all-land-tiles", false);
                boolean allForestUndefined = in.getAttributeValue(null, "all-forest-tiles") == null;
                boolean allForest = getAttribute(in, "all-forest-tiles", false);
                boolean allWater = getAttribute(in, "all-water-tiles", false);

                for (TileType t : specification.getTileTypeList()) {
                    if (t.isWater()){
                        if (allWater)
                            allowedTileTypes.add(t);
                    } else {
                        if (t.isForested()){
                            if ((allLand && allForestUndefined) || allForest){
                                allowedTileTypes.add(t);
                            }
                        } else {
                            if (allLand){
                                allowedTileTypes.add(t);
                            }
                        }
                                
                    }
                }
                in.nextTag(); // close this element
            } else if ("tile".equals(childName)) {
                String tileId = in.getAttributeValue(null, "id");
                if (getAttribute(in, "value", true)) {
                    allowedTileTypes.add(specification.getTileType(tileId));
                } else {
                    allowedTileTypes.remove(specification.getTileType(tileId));
                }
                in.nextTag(); // close this element
            } else if ("worker".equals(childName)) {
                allowedWorkers.add(in.getAttributeValue(null, "id"));
                in.nextTag(); // close this element
            } else if ("change".equals(childName)) {
                tileTypeChange.put(specification.getTileType(in.getAttributeValue(null, "from")),
                                   specification.getTileType(in.getAttributeValue(null, "to")));
                in.nextTag(); // close this element
            } else {
                super.readChild(in, specification);
            }
        }
    }
}
