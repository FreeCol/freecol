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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.FreeCol;

public final class TileImprovementType extends FreeColGameObjectType
{

    private boolean natural;
    private String typeId;
    private int magnitude;
    private int addWorkTurns;
    private String occupationString;

    private int artOverlay;
    private boolean artOverTrees;
    
    private List<TileType> allowedTileTypes;
    private TileImprovementType requiredImprovementType;

    private HashSet<String> allowedWorkers;
    private GoodsType expendedGoodsType;
    private int expendedAmount;   
    private GoodsType deliverGoodsType;
    private int deliverAmount;

    private List<GoodsType> goodsEffect;
    private List<Integer> goodsBonus;
    private List<TileType> tileTypeChangeFrom;
    private List<TileType> tileTypeChangeTo;

    private int movementCost;
    private float movementCostFactor;
    
    // ------------------------------------------------------------ constructors

    public TileImprovementType(int index) {
        setIndex(index);
    }

    // ------------------------------------------------------------ retrieval methods

    public boolean isNatural() {
        return natural;
    }

    // TODO: Why don't we use getId()?
    public String getTypeId() {
        return typeId;
    }

    public int getMagnitude() {
        return magnitude;
    }

    public int getAddWorkTurns() {
        return addWorkTurns;
    }

    public String getOccupationString() {
        return occupationString;
    }

    // TODO: Make this work like the other *types with images using Hashtable
    // Currently only Plowing has any art, the others have special display methods (roads/rivers)
    public int getArtOverlay() {
        return artOverlay;
    }

    public boolean isArtOverTrees() {
        return artOverTrees;
    }

    public TileImprovementType getRequiredImprovementType() {
        return requiredImprovementType;
    }

    public GoodsType getExpendedGoodsType() {
        return expendedGoodsType;
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
        if (!isWorkerTypeAllowed(unit.getUnitType())) {
            return false;
        }
        if (expendedAmount == 0) {
            return true;
        }
        /** TODO: Wait for correct methods from Unit.java, for now return true
            if (!unit.hasGoods(expendedGoodsType) || unit.goods(expendedGoodsType) < expendedAmount) {
            return false;
            }
        */
        // Quick fix, replace later
        if (expendedGoodsType == Goods.TOOLS) {
            return (unit.getNumberOfTools() >= expendedAmount);
        }
            
        return true;
    }

    /**
	 * This will check if in principle this type of improvement can be used on
	 * this kind of tile, disregarding the current state of an actual tile.
	 * 
	 * If you want to find out if an improvement is allowed for a tile, call
	 * {@link #isTileAllowed(Tile)}.
	 * 
	 * @param tileType
	 * @return
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
        if (tile.findTileImprovementType(this) != null) {
            return false;
        }
        return true;
    }

    public int getBonus(GoodsType goodsType) {
        int index = goodsEffect.indexOf(goodsType);
        if (index < 0) {
            return 0;
        } else {
            return goodsBonus.get(index);
        }
    }
    
    public TileType getChange(TileType tileType) {
        int index = tileTypeChangeFrom.indexOf(tileType);
        if (index < 0) {
            return null;
        } else {
            return tileTypeChangeTo.get(index);
        }
    }

    /**
     * Returns a value for use in AI decision making.
     * @param tileType The <code>TileType</code> to be considered. A <code>null</code> entry
     *        denotes no interest in a TileImprovementType that changes TileTypes
     * @param goodsType A preferred <code>GoodsType</code> or <code>null</code>
     * @return Sum of all bonuses with a triple bonus for the preferred GoodsType
     */
    public int getValue(TileType tileType, GoodsType goodsType) {
        List<GoodsType> goodsList = FreeCol.getSpecification().getGoodsTypeList();
        // 2 main types TileImprovementTypes - Changing of TileType and Simple Bonus
        TileType newTileType = getChange(tileType);
        int value = 0;
        if (newTileType != null) {
            // Calculate difference in output
            for (GoodsType g : goodsList) {
                if (!g.isFarmed())
                    continue;
                int change = newTileType.getPotential(g) - tileType.getPotential(g);
                if (goodsType == g) {
                    if (change < 0) {
                        return 0;   // Reject if there is a drop in preferred GoodsType
                    } else {
                        change *= 3;
                    }
                }
                value += change;
            }
        } else {
            // Calculate bonuses from TileImprovementType
            for (int i = 0; i < goodsEffect.size(); i++) {
                int change = goodsBonus.get(i);
                if (goodsType == goodsEffect.get(i)) {
                    if (change < 0) {
                        return 0;   // Reject if there is a drop in preferred GoodsType
                    } else {
                        change *= 3;
                    }
                }
                value += change;
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
            float cost2 = (float)cost * movementCostFactor;
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

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in, null, null, null, null);
    }

    public void readFromXML(XMLStreamReader in, final List<TileType> tileTypeList,
           final Map<String, TileType> tileTypeByRef, final Map<String, GoodsType> goodsTypeByRef,
           final Map<String, TileImprovementType> tileImprovementTypeByRef) throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        natural = getAttribute(in, "natural", false);
        String s = getAttribute(in, "occupation-string", "I");
        occupationString = s.substring(0, 1);
        addWorkTurns = getAttribute(in, "add-work-turns", 0);
        movementCost = -1;
        movementCostFactor = -1;
        
        String req = in.getAttributeValue(null, "required-improvement");
        requiredImprovementType = tileImprovementTypeByRef.get(req);
        artOverlay = getAttribute(in, "overlay", -1);
        artOverTrees = getAttribute(in, "over-trees", false);

        String g = in.getAttributeValue(null, "expended-goods-type");
        expendedGoodsType = goodsTypeByRef.get(g);
        expendedAmount = getAttribute(in, "expended-amount", 0);
        g = in.getAttributeValue(null, "deliver-goods-type");
        deliverGoodsType = goodsTypeByRef.get(g);
        deliverAmount = getAttribute(in, "deliver-amount", 0);

        allowedWorkers = new HashSet<String>();
        allowedTileTypes = new ArrayList<TileType>();
        goodsEffect = new ArrayList<GoodsType>();
        goodsBonus = new ArrayList<Integer>();
        tileTypeChangeFrom = new ArrayList<TileType>();
        tileTypeChangeTo = new ArrayList<TileType>();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("type".equals(childName)) {
                typeId = in.getAttributeValue(null, "id");
                magnitude = getAttribute(in, "magnitude", 1);
                in.nextTag(); // close this element

            } else if ("tiles".equals(childName)) {
                boolean allLand = getAttribute(in, "all-land-tiles", false);
                boolean allForestUndefined = in.getAttributeValue(null, "all-forest-tiles") == null;
                boolean allForest = getAttribute(in, "all-forest-tiles", false);
                boolean allWater = getAttribute(in, "all-water-tiles", false);

                for (TileType t : tileTypeList) {
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
                allowedTileTypes.add(tileTypeByRef.get(tileId));
                in.nextTag(); // close this element

            } else if ("worker".equals(childName)) {
                allowedWorkers.add(in.getAttributeValue(null, "id"));
                in.nextTag(); // close this element

            } else if ("effect".equals(childName)) {
                if (hasAttribute(in, "goods-type")) {
                    g = in.getAttributeValue(null, "goods-type");
                    GoodsType gt = goodsTypeByRef.get(g);
                    if (gt != null) {
                        goodsEffect.add(gt);
                        goodsBonus.add(Integer.parseInt(in.getAttributeValue(null, "value")));
                    }
                }
                if (hasAttribute(in, "movement-cost")) {
                    movementCost = getAttribute(in, "movement-cost", -1);
                }
                if (hasAttribute(in, "movement-cost-factor")) {
                    movementCostFactor = getAttribute(in, "movement-cost-factor", -1);
                }
                in.nextTag(); // close this element
                
            } else if ("change".equals(childName)) {
                tileTypeChangeFrom.add(tileTypeByRef.get(in.getAttributeValue(null, "from")));
                tileTypeChangeTo.add(tileTypeByRef.get(in.getAttributeValue(null, "to")));
                in.nextTag(); // close this element

            } else {
                throw new RuntimeException("unexpected: " + childName);
            }
        }
    }   
}
