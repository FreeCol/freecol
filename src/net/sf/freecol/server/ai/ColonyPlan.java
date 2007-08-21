package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Objects of this class describes the plan the AI has for a <code>Colony</code>.
 * 
 * <br>
 * <br>
 * 
 * A <code>ColonyPlan</code> contains {@link WorkLocationPlan}s which defines
 * the production of each {@link Building} and {@link ColonyTile}.
 * 
 * @see Colony
 */
public class ColonyPlan {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ColonyPlan.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    /**
     * The FreeColGameObject this AIObject contains AI-information for.
     */
    private Colony colony;

    private AIMain aiMain;

    private ArrayList<WorkLocationPlan> workLocationPlans = new ArrayList<WorkLocationPlan>();


    /**
     * Creates a new <code>ColonyPlan</code>.
     * 
     * @param aiMain The main AI-object.
     * @param colony The colony to make a <code>ColonyPlan</code> for.
     */
    public ColonyPlan(AIMain aiMain, Colony colony) {
        this.aiMain = aiMain;
        this.colony = colony;

        if (colony == null) {
            throw new NullPointerException("colony == null");
        }
    }

    /**
     * Creates a new <code>ColonyPlan</code>.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public ColonyPlan(AIMain aiMain, Element element) {
        this.aiMain = aiMain;
        readFromXMLElement(element);
    }

    /**
     * Returns the <code>WorkLocationPlan</code>s associated with this
     * <code>ColonyPlan</code>.
     * 
     * @return The list of <code>WorkLocationPlan</code>s .
     */
    @SuppressWarnings("unchecked")
    public List<WorkLocationPlan> getWorkLocationPlans() {
        return (List<WorkLocationPlan>) workLocationPlans.clone();
    }

    /**
     * Returns the <code>WorkLocationPlan</code>s associated with this
     * <code>ColonyPlan</code> sorted by production in a decreasing order.
     * 
     * @return The list of <code>WorkLocationPlan</code>s .
     */
    public List<WorkLocationPlan> getSortedWorkLocationPlans() {
        List<WorkLocationPlan> workLocationPlans = getWorkLocationPlans();
        Collections.sort(workLocationPlans, new Comparator<WorkLocationPlan>() {
            public int compare(WorkLocationPlan o, WorkLocationPlan p) {
                return p.getProductionOf(p.getGoodsType()) - o.getProductionOf(o.getGoodsType());
            }
        });

        return workLocationPlans;
    }

    /**
     * Gets an <code>Iterator</code> for everything to be built in the
     * <code>Colony</code>.
     * 
     * @return An iterator containing all the <code>Buildable</code> sorted by
     *         priority (highest priority first).
     */
    public Iterator<Integer> getBuildable() {
        ArrayList<Integer> buildList = new ArrayList<Integer>();

        if (!colony.getBuilding(Building.DOCK).isBuilt() && !colony.isLandLocked()
                && colony.getBuilding(Building.DOCK).canBuildNext()) {
            buildList.add(Building.DOCK);
        }

        Iterator<WorkLocationPlan> wlpIt = getSortedWorkLocationPlans().iterator();
        while (wlpIt.hasNext()) {
            WorkLocationPlan wlp = wlpIt.next();
            if (wlp.getWorkLocation() instanceof Building) {
                Building b = (Building) wlp.getWorkLocation();
                if (b.getType() == Building.TOWN_HALL) {
                    if (colony.getBuilding(Building.PRINTING_PRESS).canBuildNext()) {
                        buildList.add(new Integer(Building.PRINTING_PRESS));
                    }
                } else {
                    if (b.canBuildNext()) {
                        buildList.add(new Integer(b.getType()));
                    }
                }
            }
        }

        if (colony.getBuilding(Building.CUSTOM_HOUSE).canBuildNext()) {
            if (colony.getGoodsContainer().hasReachedCapacity(colony.getWarehouseCapacity())) {
                buildList.add(0, new Integer(Building.CUSTOM_HOUSE));
            }
        }

        // Check if we should improve the warehouse:
        if (colony.getBuilding(Building.WAREHOUSE).canBuildNext()) {
            if (colony.getGoodsContainer().hasReachedCapacity(colony.getWarehouseCapacity())) {
                buildList.add(0, new Integer(Building.WAREHOUSE));
            } else {
                buildList.add(new Integer(Building.WAREHOUSE));
            }
        }

        if (buildList.size() > 3 && colony.getBuilding(Building.CARPENTER).canBuildNext()) {
            buildList.add(0, new Integer(Building.CARPENTER));
        }

        if (colony.getHorseProduction() > 2 && colony.getBuilding(Building.STABLES).canBuildNext()) {
            buildList.add(new Integer(Building.STABLES));
        }

        if (colony.getBuilding(Building.STOCKADE).canBuildNext()) {
            buildList.add(new Integer(Building.STOCKADE));
        }

        if (colony.getBuilding(Building.ARMORY).canBuildNext() && !colony.getBuilding(Building.ARMORY).isBuilt()) {
            buildList.add(new Integer(Building.ARMORY));
        }
        buildList.add(new Integer(Colony.BUILDING_UNIT_ADDITION + Unit.ARTILLERY));

        // buildList.add(new Integer(Building.SCHOOLHOUSE));

        if (colony.getBuilding(Building.CUSTOM_HOUSE).canBuildNext()) {
            buildList.add(new Integer(Building.CUSTOM_HOUSE));
        }

        buildList.add(new Integer(-1));

        return buildList.iterator();
    }

    /**
     * Gets the main AI-object.
     * 
     * @return The main AI-object.
     */
    public AIMain getAIMain() {
        return aiMain;
    }

    /**
     * Get the <code>Game</code> this object is associated to.
     * 
     * @return The <code>Game</code>.
     */
    public Game getGame() {
        return aiMain.getGame();
    }

    /**
     * Creates a plan for this colony. That is; determines what type of goods
     * each tile should produce and what type of goods that should be
     * manufactored.
     */
    public void create() {
        
        // TODO: Erik - adapt plan to colony profile
        // Colonies should be able to specialize, determine role by colony
        // resources, buildings and specialists
        
        workLocationPlans.clear();

        // Choose the best production for each tile:
        Iterator<ColonyTile> colonyTileIterator = getColony().getColonyTileIterator();
        while (colonyTileIterator.hasNext()) {
            ColonyTile ct = colonyTileIterator.next();

            if (ct.getWorkTile().getOwner() != null && ct.getWorkTile().getOwner() != colony || ct.isColonyCenterTile()) {
                continue;
            }

            int goodsType = getBestGoodsToProduce(ct.getWorkTile());
            WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), ct, goodsType);
            workLocationPlans.add(wlp);
        }

        // Ensure that we produce lumber:
        if (getProductionOf(Goods.LUMBER) <= 0) {
            WorkLocationPlan bestChoice = null;
            int highestPotential = 0;

            Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
            while (wlpIterator.hasNext()) {
                WorkLocationPlan wlp = wlpIterator.next();
                if (wlp.getWorkLocation() instanceof ColonyTile
                        && ((ColonyTile) wlp.getWorkLocation()).getWorkTile().potential(Goods.LUMBER) > highestPotential) {
                    highestPotential = ((ColonyTile) wlp.getWorkLocation()).getWorkTile().potential(Goods.LUMBER);
                    bestChoice = wlp;
                }
            }
            if (highestPotential > 0) {
                bestChoice.setGoodsType(Goods.LUMBER);
            }
        }

        // Determine the primary and secondary types of goods:
        GoodsType primaryRawMaterial;
        int primaryRawMaterialProduction = 0;
        GoodsType secondaryRawMaterial;
        int secondaryRawMaterialProduction = 0;
        List<GoodsType> goodsTypeList = FreeCol.getSpecification().getGoodsTypeList();
        for (GoodsType goodsType : goodsTypeList) {
            if (goodsType != Goods.SUGAR && goodsType != Goods.TOBACCO && goodsType != Goods.COTTON
                    && goodsType != Goods.FURS && goodsType != Goods.ORE) {
                continue;
            }
            if (getProductionOf(goodsType) > primaryRawMaterialProduction) {
                secondaryRawMaterial = primaryRawMaterial;
                secondaryRawMaterialProduction = primaryRawMaterialProduction;
                primaryRawMaterial = goodsType;
                primaryRawMaterialProduction = getProductionOf(goodsType);
            } else if (getProductionOf(goodsType) > secondaryRawMaterialProduction) {
                secondaryRawMaterial = goodsType;
                secondaryRawMaterialProduction = getProductionOf(goodsType);
            }
        }

        // Produce food instead of goods not being primary, secondary, lumber,
        // ore or silver:
        // Stop producing if the amount of goods being produced is too low:
        Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
        while (wlpIterator.hasNext()) {
            WorkLocationPlan wlp = wlpIterator.next();
            if (!(wlp.getWorkLocation() instanceof ColonyTile)) {
                continue;
            }
            if (wlp.getGoodsType() == primaryRawMaterial || wlp.getGoodsType() == secondaryRawMaterial
                    || wlp.getGoodsType() == Goods.LUMBER || wlp.getGoodsType() == Goods.ORE
                    || wlp.getGoodsType() == Goods.SILVER) {
                continue;
            }
            if (((ColonyTile) wlp.getWorkLocation()).getWorkTile().potential(Goods.FOOD) <= 2) {
                if (wlp.getProductionOf(wlp.getGoodsType()) <= 2) {
                    wlpIterator.remove();
                }
                continue;
            }

            wlp.setGoodsType(Goods.FOOD);
        }

        // Place a carpenter:
        if (getProductionOf(Goods.LUMBER) > 0) {
            WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), colony.getBuilding(Building.CARPENTER),
                    Goods.HAMMERS);
            workLocationPlans.add(wlp);
        }

        // Place a statesman:
        WorkLocationPlan townHallWlp = new WorkLocationPlan(getAIMain(), colony.getBuilding(Building.TOWN_HALL),
                Goods.BELLS);
        workLocationPlans.add(townHallWlp);

        // Place a colonist to manufacture the primary goods:
        if (primaryRawMaterial != null) {
            Building b = colony.getBuildingForProducing(Goods.getManufactoredGoods(primaryRawMaterial));
            if (b != null) {
                WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), b, Goods
                        .getManufactoredGoods(primaryRawMaterial));
                workLocationPlans.add(wlp);
            }
        }

        // Remove the secondary goods if we need food:
        if (getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2
                && (secondaryRawMaterial == Goods.SUGAR || secondaryRawMaterial == Goods.TOBACCO
                        || secondaryRawMaterial == Goods.COTTON || secondaryRawMaterial == Goods.FURS)) {

            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext()) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof ColonyTile && wlp.getGoodsType() == secondaryRawMaterial) {
                    Tile t = ((ColonyTile) wlp.getWorkLocation()).getWorkTile();
                    if (t.getMaximumPotential(Goods.FOOD) > 2) {
                        wlp.setGoodsType(Goods.FOOD);
                    } else {
                        wlpIterator2.remove();
                    }
                }
            }
        }

        // Remove the workers on the primary goods one-by-one if we need food:
        if (getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof ColonyTile && wlp.getGoodsType() == primaryRawMaterial) {
                    Tile t = ((ColonyTile) wlp.getWorkLocation()).getWorkTile();
                    if (t.getMaximumPotential(Goods.FOOD) > 2) {
                        wlp.setGoodsType(Goods.FOOD);
                    } else {
                        wlpIterator2.remove();
                    }
                }
            }
        }

        // Remove the manufacturer if we still lack food:
        if (getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof Building) {
                    Building b = (Building) wlp.getWorkLocation();
                    if (b.getType() != Building.CARPENTER && b.getType() != Building.TOWN_HALL) {
                        wlpIterator2.remove();
                    }
                }
            }
        }

        // Remove the lumberjacks if we still lack food:
        if (getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof ColonyTile && wlp.getGoodsType() == Goods.LUMBER) {
                    wlpIterator2.remove();
                }
            }
        }

        // Remove the carpenter if we have no lumber or lack food:
        // TODO: Erik - run short on lumber as long as there is a stockpile!
        if (getProductionOf(Goods.LUMBER) < 1 || getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof Building) {
                    Building b = (Building) wlp.getWorkLocation();
                    if (b.getType() == Building.CARPENTER) {
                        wlpIterator2.remove();
                    }
                }
            }
        }

        // Remove all other colonists in buildings if we still are lacking food:
        if (getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof Building) {
                    wlpIterator2.remove();
                }
            }
        }

        int primaryWorkers = 1;
        int secondaryWorkers = 0;
        int carpenters = 1;
        int gunsmiths = 0;
        boolean colonistAdded = true;
        while (colonistAdded) {
            boolean blacksmithAdded = false;

            // Add a manufacturer for the secondary type of goods:
            if (getProductionOf(Goods.FOOD) >= workLocationPlans.size() * 2 + 2 && secondaryRawMaterial > -1
                    && 12 * secondaryWorkers + 6 <= getProductionOf(secondaryRawMaterial)
                    && secondaryWorkers <= Building.MAX_LEVEL) {
                Building b = colony.getBuildingForProducing(Goods.getManufactoredGoods(secondaryRawMaterial));
                if (b != null) {
                    WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), b, Goods
                            .getManufactoredGoods(secondaryRawMaterial));
                    workLocationPlans.add(wlp);
                    colonistAdded = true;
                    secondaryWorkers++;
                    if (secondaryRawMaterial == Goods.ORE) {
                        blacksmithAdded = true;
                    }
                }
            }

            // Add a manufacturer for the primary type of goods:
            if (getProductionOf(Goods.FOOD) >= workLocationPlans.size() * 2 + 2 && primaryRawMaterial > -1
                    && 12 * primaryWorkers + 6 <= getProductionOf(primaryRawMaterial)
                    && primaryWorkers <= Building.MAX_LEVEL) {
                Building b = colony.getBuildingForProducing(Goods.getManufactoredGoods(primaryRawMaterial));
                if (b != null) {
                    WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), b, Goods
                            .getManufactoredGoods(primaryRawMaterial));
                    workLocationPlans.add(wlp);
                    colonistAdded = true;
                    primaryWorkers++;
                    if (primaryRawMaterial == Goods.ORE) {
                        blacksmithAdded = true;
                    }
                }
            }

            // Add a gunsmith:
            if (blacksmithAdded && getProductionOf(Goods.FOOD) >= workLocationPlans.size() * 2 + 2
                    && gunsmiths < Building.MAX_LEVEL) {
                Building b = colony.getBuildingForProducing(Goods.MUSKETS);
                if (b != null) {
                    WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), b, Goods.MUSKETS);
                    workLocationPlans.add(wlp);
                    colonistAdded = true;
                    gunsmiths++;
                }
            }

            // Add carpenters:
            if (getProductionOf(Goods.FOOD) >= workLocationPlans.size() * 2 + 2
                    && 12 * carpenters + 6 <= getProductionOf(Goods.LUMBER) && carpenters <= Building.MAX_LEVEL) {
                Building b = colony.getBuilding(Building.CARPENTER);
                if (b != null) {
                    WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), b, Goods.HAMMERS);
                    workLocationPlans.add(wlp);
                    colonistAdded = true;
                    carpenters++;
                }
            }

            // TODO: Add worker to armory.

            colonistAdded = false;
        }

        // TODO: Add statesman
        // TODO: Add teacher
        // TODO: Add preacher
    }

    /**
     * Returns the production of the given type of goods accoring to this plan.
     * 
     * @param goodsType The type of goods to check the production for.
     * @return The maximum possible production of the given type of goods
     *         according to this <code>ColonyPlan</code>.
     */
    public int getProductionOf(GoodsType goodsType) {
        int amount = 0;

        Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
        while (wlpIterator.hasNext()) {
            WorkLocationPlan wlp = wlpIterator.next();
            amount += wlp.getProductionOf(goodsType);
        }

        // Add values for the center tile:
        if (goodsType == Goods.FOOD) {
            amount += colony.getTile().getMaximumPotential(Goods.FOOD);
        } else if (goodsType == colony.getTile().secondaryGoods()) {
            amount += colony.getTile().getMaximumPotential(goodsType);
        }

        return amount;
    }

    /**
     * Determines the best goods to produce on a given <code>Tile</code>
     * within this colony.
     * 
     * @param t The <code>Tile</code>.
     * @return The type of goods.
     */
    private GoodsType getBestGoodsToProduce(Tile t) {
        if (t.hasResource()) {
            return t.getTileItemContainer().getResource().getBestGoodsType();
        }
        GoodsType[] top = getSortedGoodsTop(t.getType(), t.getTileItemContainer(), t.getFishBonus());
        return top[0];
    }
/*
        if (t.isForested() && t.hasBonus()) {
            if (t.getType() == Tile.GRASSLANDS || t.getType() == Tile.SAVANNAH) {
                return Goods.LUMBER;
            } else {
                return Goods.FURS;
            }
        }
        if (t.getAddition() == Tile.ADD_HILLS) {
            return Goods.ORE;
        }
        if (t.getAddition() == Tile.ADD_MOUNTAINS) {
            if (t.hasBonus()) {
                return Goods.SILVER;
            } else {
                return Goods.ORE;
            }
        }
        if (!t.isLand()) {
            return Goods.FOOD;
        }
        if (t.getType() == Tile.DESERT) {
            if (t.hasBonus()) {
                return Goods.FOOD;
            } else {
                return Goods.ORE;
            }
        }
        switch (t.getType()) {
        case Tile.SWAMP:
        case Tile.PLAINS:
        case Tile.TUNDRA:
        case Tile.MARSH:
            return Goods.FOOD;
        case Tile.PRAIRIE:
            return Goods.COTTON;
        case Tile.GRASSLANDS:
            return Goods.TOBACCO;
        case Tile.SAVANNAH:
            return Goods.SUGAR;
        case Tile.ARCTIC:
        default:
            return Goods.ORE;
        }
    }
*/

    /**
     * Gets the <code>Colony</code> this <code>ColonyPlan</code> controls.
     * 
     * @return The <code>Colony</code>.
     */
    public Colony getColony() {
        return colony;
    }

    /**
     * Creates an XML-representation of this object.
     * 
     * @param document The <code>Document</code> in which the
     *            XML-representation should be created.
     * @return The XML-representation.
     */
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", colony.getID());

        return element;
    }

    /**
     * Updates this object from an XML-representation of a
     * <code>ColonyPlan</code>.
     * 
     * @param element The XML-representation.
     */
    public void readFromXMLElement(Element element) {
        colony = (Colony) getAIMain().getFreeColGameObject(element.getAttribute("ID"));
    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return "colonyPlan"
     */
    public static String getXMLElementTagName() {
        return "colonyPlan";
    }
}
