package net.sf.freecol.common.model;

import java.lang.String;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;


public final class TileImprovementType extends FreeColGameObjectType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision: 1.00 $";

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
        return allowedWorkers.contains(unitType.getName());
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

    public boolean isTileTypeAllowed(TileType tileType) {
        return (allowedTileTypes.indexOf(tileType) >= 0);
    }

    /**
     * Check if a given <code>Tile</code> is valid for this TileImprovement.
     * @return true if Tile TileType is valid and required Improvement (if any) is present.
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

    public void readFromXmlElement(Node xml, final List<TileType> tileTypeList,
                                   final Map<String, TileType> tileTypeByRef,
                                   final Map<String, GoodsType> goodsTypeByRef,
                                   final Map<String, TileImprovementType> tileImprovementTypeByRef) {

        setID(Xml.attribute(xml, "id"));
        natural = Xml.booleanAttribute(xml, "natural", false);
        String s = Xml.attribute(xml, "occupation-string", "I");
        occupationString = s.substring(0, 1);
        addWorkTurns = Xml.intAttribute(xml, "add-works-turns", 0);
        movementCost = -1;
        movementCostFactor = -1;
        
        String req = Xml.attribute(xml, "required-improvement", "");
        requiredImprovementType = tileImprovementTypeByRef.get(req);
        artOverlay = Xml.intAttribute(xml, "overlay", -1);
        artOverTrees = Xml.booleanAttribute(xml, "over-trees", false);

        allowedWorkers = new HashSet<String>();
        String[] workers = Xml.arrayAttribute(xml, "workers", new String[] {});
        for(int i = 0; i < workers.length; i++) {
            allowedWorkers.add(workers[i]);
        }

        String g = Xml.attribute(xml, "expended-goods-type", "");
        expendedGoodsType = goodsTypeByRef.get(g);
        expendedAmount = Xml.intAttribute(xml, "expended-amount", 0);
        g = Xml.attribute(xml, "deliver-goods-type", "");
        deliverGoodsType = goodsTypeByRef.get(g);
        deliverAmount = Xml.intAttribute(xml, "deliver-amount", 0);

        allowedTileTypes = new ArrayList<TileType>();
        goodsEffect = new ArrayList<GoodsType>();
        goodsBonus = new ArrayList<Integer>();
        tileTypeChangeFrom = new ArrayList<TileType>();
        tileTypeChangeTo = new ArrayList<TileType>();

        Xml.Method method = new Xml.Method() {
            public void invokeOn(Node xml) {
                String childName = xml.getNodeName();

                if ("type".equals(childName)) {
                    typeId = Xml.attribute(xml, "id", null);
                    magnitude = Xml.intAttribute(xml, "magnitude", 1);

                } else if ("tiles".equals(childName)) {
                    boolean allLand = Xml.booleanAttribute(xml, "all-land-tiles", false);
                    boolean allForest = Xml.booleanAttribute(xml, "all-forest-tiles", false);
                    boolean allWater = Xml.booleanAttribute(xml, "all-water-tiles", false);

                    for (TileType t : tileTypeList) {
                        if (!allLand && !t.isWater() || !allForest && t.isForested()
                            || !allWater && t.isWater()) {
                            continue;
                        }
                        allowedTileTypes.add(t);
                    }

                    if (Xml.hasAttribute(xml, "tiles")) {
                        String[] tiles = Xml.arrayAttribute(xml, "tiles");
                        for (int j = 0; j < tiles.length; j++) {
                            TileType t = tileTypeByRef.get(tiles[j]);
                            if (t != null && allowedTileTypes.indexOf(t) < 0) {
                                allowedTileTypes.add(t);
                            }
                        }
                    }
                } else if ("effect".equals(childName)) {
                    if (Xml.hasAttribute(xml, "goods-type")) {
                        String g = Xml.attribute(xml, "goods-type");
                        GoodsType gt = goodsTypeByRef.get(g);
                        if (gt != null) {
                            goodsEffect.add(gt);
                            goodsBonus.add(Xml.intAttribute(xml, "value"));
                        }
                    }
                    if (Xml.hasAttribute(xml, "goods-types")) {
                        String[] goods = Xml.arrayAttribute(xml, "goods-types");
                        int[] bonus = Xml.intArrayAttribute(xml, "values", new int[] {});
                        for (int i = 0; i < goods.length; i++) {
                            GoodsType gt = goodsTypeByRef.get(goods[i]);
                            if (gt != null && !goodsEffect.contains(gt)) {
                                goodsEffect.add(gt);
                                if (i < bonus.length) {
                                    goodsBonus.add(bonus[i]);
                                }
                            }
                        }
                    }
                    movementCost = Xml.intAttribute(xml, "movement-cost", -1);
                    movementCostFactor = Xml.intAttribute(xml, "movement-cost-factor", -1);
                } else if ("change".equals(childName)) {
                    tileTypeChangeFrom.add(tileTypeByRef.get(Xml.attribute(xml, "from")));
                    tileTypeChangeTo.add(tileTypeByRef.get(Xml.attribute(xml, "to")));
                } else {
                    throw new RuntimeException("unexpected: " + xml);
                }
            }
        };
        Xml.forEachChild(xml, method);
    }   
}
