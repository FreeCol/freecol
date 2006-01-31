
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
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.WorkInsideColonyMission;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
* Objects of this class contains AI-information for a single {@link Colony}.
*/
public class AIColony extends AIObject {
    private static final Logger logger = Logger.getLogger(AIColony.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
    * The FreeColGameObject this AIObject contains AI-information for.
    */
    private Colony colony;

    private ColonyPlan colonyPlan;
    private List aiGoods = new ArrayList();
    private List wishes = new ArrayList();
    private List tileImprovements = new ArrayList();


    /**
     * Creates a new <code>AIColony</code>.
     * 
     * @param aiMain The main AI-object.
     * @param colony The colony to make an {@link AIObject} for.
     */
    public AIColony(AIMain aiMain, Colony colony) {
        super(aiMain);

        this.colony = colony;
        colonyPlan = new ColonyPlan(aiMain, colony);
    }


    /**
     * Creates a new <code>AIColony</code>.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public AIColony(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }


    /**
    * Gets the <code>Colony</code> this <code>AIColony</code> controls.
    * @return The <code>Colony</code>.
    */
    public Colony getColony() {
        return colony;
    }

    
    /**
     * Disposes this <code>AIColony</code>.
     */
    public void dispose() {
        Iterator it1 = aiGoods.iterator();
        while (it1.hasNext()) {
            AIGoods ag = (AIGoods) it1.next();
            if (ag.getGoods().getLocation() == colony) {
                ag.dispose();
            }
        }
        Iterator it2 = wishes.iterator();
        while (it2.hasNext()) {
            ((Wish) it2.next()).dispose();
        }   
        Iterator it3 = tileImprovements.iterator();
        while (it3.hasNext()) {
            ((TileImprovement) it3.next()).dispose();
        } 
        super.dispose();
    }

    /**
    * Returns an <code>Iterator</code> of the goods to be
    * shipped from this colony. The item with the highest
    * {@link Transportable#getTransportPriority transport priority}
    * gets returned first by this <code>Iterator</code>.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getAIGoodsIterator() {
        Iterator agi = aiGoods.iterator();
        while (agi.hasNext()) {
            AIGoods ag = (AIGoods) agi.next();
            if (ag.getGoods().getLocation() != colony) {
                agi.remove();
            }
        }
        return aiGoods.iterator();
    }


    /**
    * Gets an <code>Iterator</code> for every <code>Wish</code>
    * the <code>Colony</code> has.
    *
    * @return The <code>Iterator</code>. The items with the 
    *       {@link Wish#getValue highest value} appears first
    *       in the <code>Iterator</code>
    * @see Wish
    */
    public Iterator getWishIterator() {
        return wishes.iterator();
    }


    /**
     * Creates a list of the <code>Tile</code>-improvements which
     * will increase the production by this <code>Colony</code>.
     * 
     * @see TileImprovement
     */
    public void createTileImprovements() {
        /*
         * TODO: This method has to be implemented properly.
         *       For instance, tiles we are currently using
         *       should be improved before the ones which
         *       will only be used later.
         */
                        
        List workLocationPlans = colonyPlan.getSortedWorkLocationPlans();
                    
        Iterator wlpIterator = workLocationPlans.iterator();
        while (wlpIterator.hasNext()) {
            WorkLocationPlan wlp = (WorkLocationPlan) wlpIterator.next();
            if (!(wlp.getWorkLocation() instanceof ColonyTile)) {
                continue;
            }
            Tile target = ((ColonyTile) wlp.getWorkLocation()).getWorkTile();
            
            // Update the TileImprovement if it already exist:
            boolean tileImprovementUpdated = false;
            Iterator tiIterator = tileImprovements.iterator();
            while (tiIterator.hasNext()) {
                TileImprovement ti = (TileImprovement) tiIterator.next();               
                if (ti.getTarget() == target) {
                    if (wlp.updateTileImprovement(ti) == null) {
                        ti.dispose();
                        tiIterator.remove();
                    }
                    tileImprovementUpdated = true;
                    break;
                }
            }
            
            // Create a new TileImprovement if it did not exist already:
            if (!tileImprovementUpdated) {          
                TileImprovement ti = wlp.createTileImprovement();
                if (ti != null) {
                    tileImprovements.add(ti);
                }
            }
        }
        
        // Create a TileImprovement for the center tile:
        Iterator tiIterator = tileImprovements.iterator();
        boolean centerTileFound = false;
        while (tiIterator.hasNext()) {
            TileImprovement ti = (TileImprovement) tiIterator.next();               
            if (ti.getTarget() == colony.getTile()) {
                if (!colony.getTile().canBePlowed()) {
                    ti.dispose();
                    tiIterator.remove();
                }
                centerTileFound = true;
                break;
            }
        }
        if (!centerTileFound && colony.getTile().canBePlowed()) {
            tileImprovements.add(new TileImprovement(getAIMain(), colony.getTile(), TileImprovement.PLOW, 15));
        }
            
        Collections.sort(tileImprovements, new Comparator() {
            public int compare(Object o, Object p) {
                Integer i = new Integer(((TileImprovement) o).getValue());
                Integer j = new Integer(((TileImprovement) p).getValue());
                
                return j.compareTo(i);
            }
        });
    }
    
    
    /**
     * Returns an <code>Iterator</code> over all the 
     * <code>TileImprovement</code>s needed by this
     * colony.
     * 
     * @return The <code>Iterator</code>.
     * @see TileImprovement
     */
    public Iterator getTileImprovementIterator() {
        return tileImprovements.iterator();
    }
    
    
    /**
    * Creates the wishes for the <code>Colony</code>.
    */
    private void createWishes() {
        List workLocationPlans = colonyPlan.getSortedWorkLocationPlans();

        int[] production = new int[Goods.NUMBER_OF_TYPES];
        ArrayList nonExpertUnits = new ArrayList();
        Iterator unitIterator = colony.getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit u = (Unit) unitIterator.next();
            int workType = u.getExpertWorkType();
            if (workType >= 0) {
                Iterator wlpIterator = workLocationPlans.iterator();
                while (wlpIterator.hasNext()) {
                    WorkLocationPlan wlp = (WorkLocationPlan) wlpIterator.next();
                    if (wlp.getGoodsType() == workType) {
                        if (workType < production.length) {
                            production[workType] += wlp.getProductionOf(workType);
                        }
                        production[Goods.FOOD] -= 2;
                        wlpIterator.remove();
                        break;
                    }
                }
            } else {
                nonExpertUnits.add(u);
            }
        }

        List newWishes = new ArrayList();
        int value = 120;    // TODO: Better method for determining the value of the wish.
        while (workLocationPlans.size() > 0) {
            // Farmer/fisherman wishes:
            if (production[Goods.FOOD] < 2) {
                Iterator wlpIterator = workLocationPlans.iterator();
                while (wlpIterator.hasNext()) {
                    WorkLocationPlan wlp = (WorkLocationPlan) wlpIterator.next();
                    if (wlp.getGoodsType() == Goods.FOOD) {
                        production[Goods.FOOD] += wlp.getProductionOf(Goods.FOOD);
                        production[Goods.FOOD] -= 2;
                        wlpIterator.remove();

                        int unitType = ((ColonyTile) wlp.getWorkLocation()).getWorkTile().isLand()
                                        ? Unit.EXPERT_FARMER
                                        : Unit.EXPERT_FISHERMAN;
                        if (nonExpertUnits.size() > 0) {
                            newWishes.add(new WorkerWish(getAIMain(), colony, value, unitType, false));
                            nonExpertUnits.remove(0);
                        } else {
                            newWishes.add(new WorkerWish(getAIMain(), colony, value, unitType, true));
                        }
                        value -= 5;
                        if (value < 50) {
                            value = 50;
                        }
                        break;
                    }
                }
            }

            Iterator wlpIterator = workLocationPlans.iterator();
            while (wlpIterator.hasNext()) {
                WorkLocationPlan wlp = (WorkLocationPlan) wlpIterator.next();
                // TODO: Check if the production of the raw material is sufficient.
                if (true) {
                    if (wlp.getGoodsType() < production.length) {
                        production[wlp.getGoodsType()] += wlp.getProductionOf(wlp.getGoodsType());
                    }
                    production[Goods.FOOD] -= 2;
                    wlpIterator.remove();

                    int unitType;
                    if (wlp.getWorkLocation() instanceof ColonyTile) {
                        unitType = ((ColonyTile) wlp.getWorkLocation()).getExpertForProducing(wlp.getGoodsType());
                    } else {
                        unitType = ((Building) wlp.getWorkLocation()).getExpertUnitType();
                    }
                    if (unitType < 0) {
                        logger.warning("unitType < 0");
                    }
                    if (nonExpertUnits.size() > 0) {
                        newWishes.add(new WorkerWish(getAIMain(), colony, value, unitType, false));
                        nonExpertUnits.remove(0);
                    } else {
                        newWishes.add(new WorkerWish(getAIMain(), colony, value, unitType, true));
                    }
                    value -= 5;
                    if (value < 50) {
                        value = 50;
                    }
                    break;
                }
            }
        }

        Iterator wishIterator = wishes.iterator();
        while (wishIterator.hasNext()) {
            Wish w = (Wish) wishIterator.next();
            if (w instanceof WorkerWish) {
                WorkerWish ww = (WorkerWish) w;
                Iterator newWishIterator = newWishes.iterator();
                boolean wishFound = false;
                while (!wishFound && newWishIterator.hasNext()) {
                    WorkerWish ww2 = (WorkerWish) newWishIterator.next();
                    if (ww2.getTransportable() == null && ww2.getUnitType() == ww.getUnitType()) {
                        ww2.setTransportable(ww.getTransportable());
                        wishFound = true;
                    }
                }
                if (!wishFound && ww.getTransportable() != null) {
                    ((AIUnit) ww.getTransportable()).setMission(null);
                }
            } else if (w instanceof GoodsWish) {
                GoodsWish gw = (GoodsWish) w;
                //TODO: check for a certain required amount?
                if (getColony().getGoodsCount(gw.getGoodsType()) == 0) {
                    newWishes.add(gw);
                }
            } else {
                logger.warning("Unknown type of Wish.");
            }
        }

        wishes = newWishes;
    }


    /**
    * Creates a list of the goods which should be shipped to and from this colony.
    */
    public void createAIGoods() {
        createExportAIGoodsList();
        // TODO: createGoodsWishes()
    }

    /**
    * Add a <code>GoodsWish</code> to the wish list.
    * @param gw The <code>GoodsWish</code> to be added. 
    */
    public void addGoodsWish(GoodsWish gw) {
        wishes.add(gw);
    }


    /**
    * Creates a list of the goods which should be shipped out of this colony.
    * This is the list {@link #getAIGoodsIterator} returns the <code>Iterator</code>
    * for.
    */
    private void createExportAIGoodsList() {
        List newAIGoods = new ArrayList();

        // TODO: Do not sell raw material we are lacking.

        for (int goodsType=0; goodsType<Goods.NUMBER_OF_TYPES; goodsType++) {
            if (goodsType == Goods.FOOD || goodsType == Goods.LUMBER || goodsType == Goods.HORSES) {
                continue;
            }
            if (goodsType == Goods.MUSKETS 
                    && colony.getProductionOf(Goods.MUSKETS) > 0
                    && colony.getGoodsCount(Goods.MUSKETS) > colony.getWarehouseCapacity() - 50) {
                continue;
            }
            if (colony.getGoodsCount(goodsType) > 0) {
                List alreadyAdded = new ArrayList();
                for (int j=0; j<aiGoods.size(); j++) {
                    AIGoods ag = ((AIGoods) aiGoods.get(j));
                    if (ag == null) {
                        logger.warning("aiGoods == null");
                    }
                    if (ag.getGoods() == null) {
                        logger.warning("aiGoods.getGoods() == null");
                    }
                    if (ag != null && ag.getGoods().getType() == goodsType
                            && ag.getGoods().getLocation() == colony) {
                        alreadyAdded.add(ag);
                    }
                }

                int amountRemaining = colony.getGoodsCount(goodsType);
                for (int i=0; i<alreadyAdded.size(); i++) {
                    AIGoods oldGoods = (AIGoods) alreadyAdded.get(i);
                    if (oldGoods.getGoods().getLocation() != colony) {
                        continue;
                    }
                    if (oldGoods.getGoods().getAmount() < 100 && oldGoods.getGoods().getAmount() < amountRemaining) {
                        int goodsAmount = Math.min(100, amountRemaining);
                        oldGoods.getGoods().setAmount(goodsAmount);
                        if (amountRemaining >= colony.getWarehouseCapacity()
                                && oldGoods.getTransportPriority() < AIGoods.IMPORTANT_DELIVERY) {
                            oldGoods.setTransportPriority(AIGoods.IMPORTANT_DELIVERY);
                        } else if (goodsAmount == 100 && oldGoods.getTransportPriority() < AIGoods.FULL_DELIVERY) {
                            oldGoods.setTransportPriority(AIGoods.FULL_DELIVERY);
                        }
                        amountRemaining -= goodsAmount;
                        newAIGoods.add(oldGoods);
                    } else if (oldGoods.getGoods().getAmount() > amountRemaining) {
                        if (amountRemaining == 0) {
                            if (oldGoods.getTransport() != null
                                    && oldGoods.getTransport().getMission() instanceof TransportMission) {
                                ((TransportMission) oldGoods.getTransport().getMission()).removeFromTransportList(oldGoods);
                            }
                            oldGoods.dispose();
                        } else {
                            oldGoods.getGoods().setAmount(amountRemaining);
                            newAIGoods.add(oldGoods);
                            amountRemaining = 0;
                        }
                    } else {
                        newAIGoods.add(oldGoods);
                        amountRemaining -= oldGoods.getGoods().getAmount();
                    }
                }
                while (amountRemaining > 0) {
                    if (amountRemaining >= 100) {
                        AIGoods newGoods = new AIGoods(getAIMain(), colony, goodsType, 100, getColony().getOwner().getEurope());
                        if (amountRemaining >= colony.getWarehouseCapacity()) {
                            newGoods.setTransportPriority(AIGoods.IMPORTANT_DELIVERY);
                        } else {
                            newGoods.setTransportPriority(AIGoods.FULL_DELIVERY);
                        }
                        newAIGoods.add(newGoods);
                        amountRemaining -= 100;
                    } else {
                        AIGoods newGoods = new AIGoods(getAIMain(), colony, goodsType, amountRemaining, getColony().getOwner().getEurope());
                        newAIGoods.add(newGoods);
                        amountRemaining = 0;
                    }
                }
            }
        }

        aiGoods.clear();
        Iterator nai = newAIGoods.iterator();
        while (nai.hasNext()) {
            AIGoods ag = (AIGoods) nai.next();
            int i;
            for (i=0; i<aiGoods.size() && ((AIGoods) aiGoods.get(i)).getTransportPriority() > ag.getTransportPriority(); i++);
            aiGoods.add(i, ag);
        }
    }


    /**
    * Rearranges the workers within this colony.
    * This is done according to the {@link ColonyPlan}, although minor
    * adjustments can be done to increase production.
    */
    public void rearrangeWorkers(Connection connection) {
        colonyPlan.create();

        // TODO: Detect a siege and move the workers temporarily around.

        List units = new ArrayList();
        List workLocationPlans = new ArrayList(colonyPlan.getWorkLocationPlans());
        Collections.sort(workLocationPlans, new Comparator() {
            public int compare(Object o, Object p) {
                Integer i = new Integer(((WorkLocationPlan) o).getProductionOf(((WorkLocationPlan) o).getGoodsType()));
                Integer j = new Integer(((WorkLocationPlan) p).getProductionOf(((WorkLocationPlan) p).getGoodsType()));

                return j.compareTo(i);
            }
        });
        
        // Remove all colonists from the colony:
        Iterator ui = colony.getUnitIterator();
        while (ui.hasNext()) {
            Unit unit = (Unit) ui.next();
            units.add(unit);
            unit.setLocation(null);
        }

        // Place all the experts:
        Iterator uit = units.iterator();
        while (uit.hasNext()) {
            Unit unit = (Unit) uit.next();
            if (unit.getExpertWorkType() >= 0) {
                Iterator wlpIterator = workLocationPlans.iterator();
                while (wlpIterator.hasNext()) {
                    WorkLocationPlan wlp = (WorkLocationPlan) wlpIterator.next();
                    WorkLocation wl = wlp.getWorkLocation();
                    if (unit.getExpertWorkType() == wlp.getGoodsType() && wlp.getWorkLocation().canAdd(unit)
                            && (wlp.getGoodsType() != Goods.FOOD
                            || !((ColonyTile) wl).getWorkTile().isLand() && unit.getType() == Unit.EXPERT_FISHERMAN && colony.getBuilding(Building.DOCK).isBuilt()
                            || ((ColonyTile) wl).getWorkTile().isLand() && unit.getType() != Unit.EXPERT_FISHERMAN)) {
                        unit.setLocation(wlp.getWorkLocation());
                        unit.setWorkType(wlp.getGoodsType());
                        wlpIterator.remove();
                        uit.remove();
                        break;
                    }
                }
            }
        }

        boolean workerAdded = true;
        while (workerAdded) {
            workerAdded = false;
            // Use a food production plan if necessary:
            int food = colony.getFoodProduction() - colony.getFoodConsumption();
            for (int i=0; i<workLocationPlans.size() && food < 2; i++) {
                WorkLocationPlan wlp = (WorkLocationPlan) workLocationPlans.get(i);
                WorkLocation wl = wlp.getWorkLocation();
                if (wlp.getGoodsType() == Goods.FOOD &&
                        (((ColonyTile) wl).getWorkTile().isLand() || colony.getBuilding(Building.DOCK).isBuilt())) {
                    Unit bestUnit = null;
                    int bestProduction = 0;
                    Iterator unitIterator = units.iterator();
                    while (unitIterator.hasNext()) {
                        Unit unit = (Unit) unitIterator.next();
                        int production = unit.getFarmedPotential(Goods.FOOD, ((ColonyTile) wlp.getWorkLocation()).getWorkTile());
                        if (production > 1 && (bestUnit == null
                                || production > bestProduction
                                || production == bestProduction && unit.getSkillLevel() < bestUnit.getSkillLevel())) {
                            bestUnit = unit;
                            bestProduction = production;
                        }
                    }
                    if (bestUnit != null && wlp.getWorkLocation().canAdd(bestUnit)) {
                        bestUnit.setLocation(wlp.getWorkLocation());
                        bestUnit.setWorkType(wlp.getGoodsType());
                        units.remove(bestUnit);
                        workLocationPlans.remove(wlp);
                        workerAdded = true;
                        food = colony.getFoodProduction() - colony.getFoodConsumption();
                    }
                }
            }
            // Use the next non-food plan:
            if (food >= 2) {
                for (int i=0; i<workLocationPlans.size(); i++) {
                    WorkLocationPlan wlp = (WorkLocationPlan) workLocationPlans.get(i);
                    if (wlp.getGoodsType() != Goods.FOOD) {
                        Unit bestUnit = null;
                        int bestProduction = 0;
                        Iterator unitIterator = units.iterator();
                        while (unitIterator.hasNext()) {
                            Unit unit = (Unit) unitIterator.next();
                            int production;
                            if (wlp.getWorkLocation() instanceof ColonyTile) {
                                production = unit.getFarmedPotential(wlp.getGoodsType(), ((ColonyTile) wlp.getWorkLocation()).getWorkTile());
                            } else { // Building
                                production = unit.getProducedAmount(wlp.getGoodsType());
                            }
                            if (bestUnit == null
                                    || production > bestProduction
                                    || production == bestProduction && unit.getSkillLevel() < bestUnit.getSkillLevel()) {
                                bestUnit = unit;
                                bestProduction = production;
                            }
                        }
                        if (bestUnit != null && wlp.getWorkLocation().canAdd(bestUnit)) {
                            bestUnit.setLocation(wlp.getWorkLocation());
                            bestUnit.setWorkType(wlp.getGoodsType());
                            units.remove(bestUnit);
                            workLocationPlans.remove(wlp);
                            workerAdded = true;
                            food = colony.getFoodProduction() - colony.getFoodConsumption();
                        }
                    }
                }
            }
        }

        // Ensure that we have enough food:
        int food = colony.getFoodProduction() - colony.getFoodConsumption();
        while (food < 0 && colony.getGoodsCount(Goods.FOOD) + food * 3 < 0) {
            Iterator wlIterator = colony.getWorkLocationIterator();
            WorkLocation bestPick = null;
            while (wlIterator.hasNext()) {
                WorkLocation wl = (WorkLocation) wlIterator.next();
                if (wl.getUnitCount() > 0) {
                    if (wl instanceof ColonyTile) {
                        ColonyTile ct = (ColonyTile) wl;
                        Unit u = ct.getUnit();
                        if (ct.getUnit().getWorkType() != Goods.FOOD) {
                            int uProduction = u.getFarmedPotential(Goods.FOOD, ct.getWorkTile());
                            if (uProduction > 1) {
                                if (bestPick == null || bestPick instanceof Building) {
                                    bestPick = wl;
                                } else {
                                    ColonyTile bpct = (ColonyTile) bestPick;
                                    int bestPickProduction = bpct.getUnit().getFarmedPotential(Goods.FOOD, bpct.getWorkTile());
                                    if (uProduction > bestPickProduction
                                            || (uProduction == bestPickProduction && u.getSkillLevel() < bpct.getUnit().getSkillLevel())) {
                                        bestPick = wl;
                                    }
                                }
                            } else {
                                if (bestPick == null) {
                                    bestPick = wl;
                                }  // else - TODO: This might be the best pick sometimes:
                            }
                        }
                    } else { // wl instanceof Building
                        if (bestPick == null || (bestPick instanceof Building
                                && ((Building) wl).getProduction() < ((Building) bestPick).getProduction())) {
                            bestPick = wl;
                        }
                    }
                }
            }
            if (bestPick == null) {
                break;
            }
            if (bestPick instanceof ColonyTile) {
                ColonyTile ct = (ColonyTile) bestPick;
                Unit u = ct.getUnit();
                if (u.getFarmedPotential(Goods.FOOD, ct.getWorkTile()) > 1) {
                    u.setWorkType(Goods.FOOD);
                } else {
                    u.setLocation(colony.getTile());
                    AIUnit au = (AIUnit) getAIMain().getAIObject(u);
                    if (au.getMission() instanceof WorkInsideColonyMission) {
                        au.setMission(null);
                    }
                }
            } else { // bestPick instanceof Building
                Building b = (Building) bestPick;
                Iterator unitIterator = b.getUnitIterator();
                Unit bestUnit = (Unit) unitIterator.next();
                while (unitIterator.hasNext()) {
                    Unit u = (Unit) unitIterator.next();
                    if (u.getExpertWorkType() != u.getWorkType()) {
                        bestUnit = u;
                        break;
                    }
                }
                bestUnit.setLocation(colony.getTile());
                AIUnit au = (AIUnit) getAIMain().getAIObject(bestUnit);
                if (au.getMission() instanceof WorkInsideColonyMission) {
                    au.setMission(null);
                }
            }

            food = colony.getFoodProduction() - colony.getFoodConsumption();
        }

        // Move any workers not producing anything to a temporary location.
        Iterator wlIterator = colony.getWorkLocationIterator();
        while (wlIterator.hasNext()) {
            WorkLocation wl = (WorkLocation) wlIterator.next();
            while (wl.getUnitCount() > 0 && wl instanceof Building
                    && ((Building) wl).getProductionNextTurn() <= 0) {
                Iterator unitIterator = wl.getUnitIterator();
                Unit bestPick = (Unit) unitIterator.next();
                while (unitIterator.hasNext()) {
                    Unit u = (Unit) unitIterator.next();
                    if (u.getExpertWorkType() != u.getWorkType()) {
                        bestPick = u;
                        break;
                    }
                }
                int rawMaterial = Goods.getRawMaterial(bestPick.getWorkType());
                ColonyTile ct = (rawMaterial >= 0) ? colony.getVacantColonyTileFor(bestPick, rawMaterial) : null;
                if (ct != null) {
                    bestPick.setLocation(ct);
                    bestPick.setWorkType(rawMaterial);
                } else {
                    Building th = colony.getBuilding(Building.TOWN_HALL);
                    if (th.canAdd(bestPick)) {
                        bestPick.setLocation(th);
                    } else {
                        ct = colony.getVacantColonyTileFor(bestPick, Goods.FOOD);
                        if (ct != null) {
                            bestPick.setLocation(ct);
                            bestPick.setWorkType(Goods.FOOD);
                        } else {
                            bestPick.setLocation(colony);
                            if (bestPick.getLocation() == wl) {
                                break;
                            }
                        }
                    }
                }
            }
        }

        // TODO: Move workers to temporarily improve the production.
        // TODO: Change the production type of workers producing a cargo there is no room for.

        // Use any remaining food plans:
        for (int i=0; i<workLocationPlans.size(); i++) {
            WorkLocationPlan wlp = (WorkLocationPlan) workLocationPlans.get(i);
            WorkLocation wl = wlp.getWorkLocation();
            if (wlp.getGoodsType() == Goods.FOOD &&
                    (((ColonyTile) wl).getWorkTile().isLand() || colony.getBuilding(Building.DOCK).isBuilt())) {
                Unit bestUnit = null;
                int bestProduction = 0;
                Iterator unitIterator = units.iterator();
                while (unitIterator.hasNext()) {
                    Unit unit = (Unit) unitIterator.next();
                    int production = unit.getFarmedPotential(Goods.FOOD, ((ColonyTile) wlp.getWorkLocation()).getWorkTile());
                    if (production > 1 && (bestUnit == null
                            || production > bestProduction
                            || production == bestProduction && unit.getSkillLevel() < bestUnit.getSkillLevel())) {
                        bestUnit = unit;
                        bestProduction = production;
                    }
                }
                if (bestUnit != null && wlp.getWorkLocation().canAdd(bestUnit)) {
                    bestUnit.setLocation(wlp.getWorkLocation());
                    bestUnit.setWorkType(wlp.getGoodsType());
                    units.remove(bestUnit);
                    workLocationPlans.remove(wlp);
                }
            }
        }

        // Put any remaining workers outside the colony:
        Iterator ui6 = units.iterator();
        while (ui6.hasNext()) {
            Unit u = (Unit) ui6.next();
            u.setLocation(colony.getTile());
            AIUnit au = (AIUnit) getAIMain().getAIObject(u);
            if (au.getMission() instanceof WorkInsideColonyMission) {
                au.setMission(null);
            }
        }

        createWishes();
        createTileImprovements();
    }


    /**
    * Determines the best goods to produce on a given <code>Tile</code>
    * within this colony.
    *
    * @param The <code>Tile</code>.
    * @return The type of goods.
    */
    private int getBestGoodsToProduce(Tile t) {
        if (t.isForested()) {
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
        switch(t.getType()) {
            case Tile.SWAMP:
            case Tile.PLAINS:
                return Goods.FOOD;
            case Tile.PRAIRIE:
                return Goods.COTTON;
            case Tile.GRASSLANDS:
                return Goods.TOBACCO;
            case Tile.SAVANNAH:
                return Goods.SUGAR;
            case Tile.MARSH:
            case Tile.TUNDRA:
            case Tile.ARCTIC:
            default:
                return Goods.ORE;
        }
    }


    /**
     * Gets the ID of this object.
     * @return The same ID as the <code>Colony</code>
     *      this <code>AIColony</code> stores AI-specific
     *      information for.
     */
    public String getID() {
        return colony.getID();
    }


    /**
     * Creates an XML-representation of this object.
     * @param document The <code>Document</code> in which
     *      the XML-representation should be created.
     * @return The XML-representation.
     */
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", getID());

        Iterator aiGoodsIterator = aiGoods.iterator();
        while (aiGoodsIterator.hasNext()) {
            AIGoods ag = (AIGoods) aiGoodsIterator.next();
            Element agElement = document.createElement(AIGoods.getXMLElementTagName() + "ListElement");
            agElement.setAttribute("ID", ag.getID());
            element.appendChild(agElement);
        }

        Iterator wishesIterator = wishes.iterator();
        while (wishesIterator.hasNext()) {
            Wish w = (Wish) wishesIterator.next();
            Element wElement;
            if (w instanceof WorkerWish) {
                wElement = document.createElement(WorkerWish.getXMLElementTagName() + "WishListElement");
            } else {
                logger.warning("Unknown type of wish.");
                continue;
            }
            wElement.setAttribute("ID", w.getID());
            element.appendChild(wElement);
        }
        
        Iterator tileImprovementIterator = tileImprovements.iterator();
        while (tileImprovementIterator.hasNext()) {
            TileImprovement ti = (TileImprovement) tileImprovementIterator.next();
            Element tiElement = document.createElement(TileImprovement.getXMLElementTagName() + "ListElement");
            tiElement.setAttribute("ID", ti.getID());
            element.appendChild(tiElement);
        }        

        return element;
    }


    /**
     * Updates this object from an XML-representation of
     * a <code>AIColony</code>.
     * 
     * @param element The XML-representation.
     */
    public void readFromXMLElement(Element element) {
        colony = (Colony) getAIMain().getFreeColGameObject(element.getAttribute("ID"));       
        if (colony == null) {
            throw new NullPointerException("Could not find Colony with ID: " + element.getAttribute("ID"));
        }

        aiGoods.clear();
        wishes.clear();

        colonyPlan = new ColonyPlan(getAIMain(), colony);
        colonyPlan.create();

        NodeList nl = element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) {
            if (!(nl.item(i) instanceof Element)) {
                continue;
            }
            Element e = (Element) nl.item(i);
            if (e.getTagName().equals(AIGoods.getXMLElementTagName() + "ListElement")) {
                AIGoods ag = (AIGoods) getAIMain().getAIObject(e.getAttribute("ID"));
                aiGoods.add(ag);
            } else if (e.getTagName().equals(WorkerWish.getXMLElementTagName() + "WishListElement")) {
                Wish w = (Wish) getAIMain().getAIObject(e.getAttribute("ID"));
                if (w != null) {
                    wishes.add(w);
                } else {
                    logger.warning("Wish with ID: " + e.getAttribute("ID") + " could not be found.");
                }
            } else if (e.getTagName().equals(TileImprovement.getXMLElementTagName() + "ListElement")) {
                TileImprovement ti = (TileImprovement) getAIMain().getAIObject(e.getAttribute("ID"));
                if (ti != null) {
                    tileImprovements.add(ti);
                } else {
                    logger.warning("TileImprovement with ID: " + e.getAttribute("ID") + " could not be found.");
                }               
            } else {
                logger.warning("Unknown tag name: " + e.getTagName());
            }
        }
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return "aiColony"
    */
    public static String getXMLElementTagName() {
        return "aiColony";
    }
}
