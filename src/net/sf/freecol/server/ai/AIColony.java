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

package net.sf.freecol.server.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.mission.PioneeringMission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.WorkInsideColonyMission;

import org.w3c.dom.Element;

/**
 * Objects of this class contains AI-information for a single {@link Colony}.
 */
public class AIColony extends AIObject {
    private static final Logger logger = Logger.getLogger(AIColony.class.getName());




    /**
     * The FreeColGameObject this AIObject contains AI-information for.
     */
    private Colony colony;

    private ColonyPlan colonyPlan;

    private ArrayList<AIGoods> aiGoods = new ArrayList<AIGoods>();

    private ArrayList<Wish> wishes = new ArrayList<Wish>();

    private ArrayList<TileImprovementPlan> TileImprovementPlans = new ArrayList<TileImprovementPlan>();


    /**
     * Creates a new <code>AIColony</code>.
     * 
     * @param aiMain The main AI-object.
     * @param colony The colony to make an {@link AIObject} for.
     */
    public AIColony(AIMain aiMain, Colony colony) {
        super(aiMain, colony.getId());

        this.colony = colony;
        colonyPlan = new ColonyPlan(aiMain, colony);
    }

    /**
     * Creates a new <code>AIColony</code>.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public AIColony(AIMain aiMain, Element element) {
        super(aiMain, element.getAttribute("ID"));
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>AIColony</code>.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public AIColony(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain, in.getAttributeValue(null, "ID"));
        readFromXML(in);
    }

    /**
     * Creates a new <code>AIColony</code>.
     * 
     * @param aiMain The main AI-object.
     * @param id
     */
    public AIColony(AIMain aiMain, String id) {
        this(aiMain, (Colony) aiMain.getGame().getFreeColGameObject(id));
    }

    /**
     * Gets the <code>Colony</code> this <code>AIColony</code> controls.
     * 
     * @return The <code>Colony</code>.
     */
    public Colony getColony() {
        return colony;
    }

    /**
     * Disposes this <code>AIColony</code>.
     */
    public void dispose() {
        List<AIObject> disposeList = new ArrayList<AIObject>();
        for (AIGoods ag : aiGoods) {
            if (ag.getGoods().getLocation() == colony) {
                disposeList.add(ag);
            }
        }
        for(Wish w : wishes) {
            disposeList.add(w);
        }
        for(TileImprovementPlan ti : TileImprovementPlans) {
            disposeList.add(ti);
        }
        for(AIObject o : disposeList) {
            o.dispose();
        }
        super.dispose();
    }

    /**
     * Returns an <code>Iterator</code> of the goods to be shipped from this
     * colony. The item with the highest
     * {@link Transportable#getTransportPriority transport priority} gets
     * returned first by this <code>Iterator</code>.
     * 
     * @return The <code>Iterator</code>.
     */
    public Iterator<AIGoods> getAIGoodsIterator() {
        Iterator<AIGoods> agi = aiGoods.iterator();
        // TODO: Remove the following code and replace by throw RuntimeException
        while (agi.hasNext()) {
            AIGoods ag = agi.next();
            if (ag.getGoods().getLocation() != colony) {
                agi.remove();
            }
        }
        return aiGoods.iterator();
    }

    /**
     * Gets an <code>Iterator</code> for every <code>Wish</code> the
     * <code>Colony</code> has.
     * 
     * @return The <code>Iterator</code>. The items with the
     *         {@link Wish#getValue highest value} appears first in the
     *         <code>Iterator</code>
     * @see Wish
     */
    public Iterator<Wish> getWishIterator() {
        return wishes.iterator();
    }

    /**
     * Creates a list of the <code>Tile</code>-improvements which will
     * increase the production by this <code>Colony</code>.
     * 
     * @see TileImprovementPlan
     */
    public void createTileImprovementPlans() {
        /*
         * TODO: This method has to be implemented properly. For instance, tiles
         * we are currently using should be improved before the ones which will
         * only be used later.
         */

        List<WorkLocationPlan> workLocationPlans = colonyPlan.getSortedWorkLocationPlans();

        Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
        while (wlpIterator.hasNext()) {
            WorkLocationPlan wlp = wlpIterator.next();
            if (!(wlp.getWorkLocation() instanceof ColonyTile)) {
                continue;
            }
            Tile target = ((ColonyTile) wlp.getWorkLocation()).getWorkTile();

            // Update the TileImprovementPlan if it already exist:
            boolean TileImprovementPlanUpdated = false;
            Iterator<TileImprovementPlan> tiIterator = TileImprovementPlans.iterator();
            while (tiIterator.hasNext()) {
                TileImprovementPlan ti = tiIterator.next();
                if (ti.getTarget() == target) {
                    if (wlp.updateTileImprovementPlan(ti) == null) {
                        ti.dispose();
                        tiIterator.remove();
                    }
                    TileImprovementPlanUpdated = true;
                    break;
                }
            }

            // Create a new TileImprovementPlan if it did not exist already:
            if (!TileImprovementPlanUpdated) {
                TileImprovementPlan ti = wlp.createTileImprovementPlan();
                if (ti != null) {
                    TileImprovementPlans.add(ti);
                }
            }
        }

        // Create a TileImprovementPlan for the center tile: - not sure if it's allowed - ryan
/*
        Iterator<TileImprovementPlan> tiIterator = TileImprovementPlans.iterator();
        boolean centerTileFound = false;
        while (tiIterator.hasNext()) {
            TileImprovementPlan ti = tiIterator.next();
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
            TileImprovementPlans.add(new TileImprovementPlan(getAIMain(), colony.getTile(), TileImprovementPlan.PLOW, 15));
        }
*/
        Collections.sort(TileImprovementPlans, new Comparator<TileImprovementPlan>() {
            public int compare(TileImprovementPlan o, TileImprovementPlan p) {
                Integer i = o.getValue();
                Integer j = p.getValue();

                return j.compareTo(i);
            }
        });
    }

    /**
     * Returns an <code>Iterator</code> over all the
     * <code>TileImprovementPlan</code>s needed by this colony.
     * 
     * @return The <code>Iterator</code>.
     * @see TileImprovementPlan
     */
    public Iterator<TileImprovementPlan> getTileImprovementPlanIterator() {
        return TileImprovementPlans.iterator();
    }

    /**
     * Creates the wishes for the <code>Colony</code>.
     */
    private void createWishes() {
        logger.finest("Entering method createWishes");
        List<WorkLocationPlan> workLocationPlans = colonyPlan.getSortedWorkLocationPlans();
        Iterator<WorkLocationPlan> rit = workLocationPlans.iterator();
        while (rit.hasNext()) {
            WorkLocationPlan wlp = rit.next();
            // Do not use tiles taken by other colonies:
            if (wlp.getWorkLocation() instanceof ColonyTile
                    && ((ColonyTile) wlp.getWorkLocation()).getWorkTile().getSettlement() != null) {
                rit.remove();
            }
            // Do not request fishermen unless Docks have been completed:
            if (wlp.getWorkLocation() instanceof ColonyTile
                    && !((ColonyTile) wlp.getWorkLocation()).getWorkTile().isLand()
                    && !colony.hasAbility("model.ability.produceInWater")) {
                // TODO: Check if docks are currently being built (and a carpenter with lumber is available)
                rit.remove();
            }
        }

        int[] production = new int[Goods.NUMBER_OF_TYPES];
        ArrayList<Unit> nonExpertUnits = new ArrayList<Unit>();
        Iterator<Unit> unitIterator = colony.getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit u = unitIterator.next();
            GoodsType workType = u.getExpertWorkType();
            if (workType != null) {
                Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
                while (wlpIterator.hasNext()) {
                    WorkLocationPlan wlp = wlpIterator.next();
                    if (wlp.getGoodsType() == workType) {
                        if (workType.getIndex() < production.length) {
                            production[workType.getIndex()] += wlp.getProductionOf(workType);
                        }
                        production[(Goods.FOOD).getIndex()] -= 2;
                        wlpIterator.remove();
                        break;
                    }
                }
            } else {
                nonExpertUnits.add(u);
            }
        }

        List<Wish> newWishes = new ArrayList<Wish>();
        int value = 120; // TODO: Better method for determining the value of
        // the wish.
        while (workLocationPlans.size() > 0) {
            UnitType unitType = null;

            // Farmer/fisherman wishes:
            if (production[(Goods.FOOD).getIndex()] < 2) {
                Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
                while (wlpIterator.hasNext()) {
                    WorkLocationPlan wlp = wlpIterator.next();
                    if (wlp.getGoodsType() == Goods.FOOD) {
                        production[(Goods.FOOD).getIndex()] += wlp.getProductionOf(Goods.FOOD);
                        production[(Goods.FOOD).getIndex()] -= 2;
                        wlpIterator.remove();

                        if (((ColonyTile) wlp.getWorkLocation()).getWorkTile().isLand()) {
                            unitType = FreeCol.getSpecification().getExpertForProducing(Goods.FOOD);
                        } else {
                            unitType = FreeCol.getSpecification().getExpertForProducing(Goods.FISH);
                        }
                        break;
                    }
                }
            }
            if (unitType == null) {
                if (production[(Goods.FOOD).getIndex()] < 2) {
                    // Not enough food.
                    break;
                }
                Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
                while (wlpIterator.hasNext()) {
                    WorkLocationPlan wlp = wlpIterator.next();
                    // TODO: Check if the production of the raw material is
                    // sufficient.
                    if (wlp.getGoodsType().getIndex() < production.length) {
                        production[wlp.getGoodsType().getIndex()] += wlp.getProductionOf(wlp.getGoodsType());
                    }
                    production[(Goods.FOOD).getIndex()] -= 2;
                    wlpIterator.remove();

                    if (wlp.getWorkLocation() instanceof ColonyTile) {
                        unitType = FreeCol.getSpecification().getExpertForProducing(wlp.getGoodsType());
                    } else {
                        unitType = ((Building) wlp.getWorkLocation()).getExpertUnitType();
                    }
                    break;
                }
            }
            if (unitType != null) {
                boolean expert = (nonExpertUnits.size() <= 0);

                boolean wishFound = false;
                Iterator<Wish> wishIterator = wishes.iterator();
                while (wishIterator.hasNext()) {
                    Wish w = wishIterator.next();
                    if (w instanceof WorkerWish) {
                        WorkerWish ww = (WorkerWish) w;
                        if (ww.getUnitType() == unitType && !newWishes.contains(ww)) {
                            ww.update(value, unitType, expert);
                            newWishes.add(ww);
                            wishFound = true;
                            break;
                        }
                    }
                }

                if (!wishFound) {
                    WorkerWish ww = new WorkerWish(getAIMain(), colony, value, unitType, expert);
                    wishes.add(ww);
                    newWishes.add(ww);
                }
                if (!expert) {
                    nonExpertUnits.remove(0);
                }
                value -= 5;
                if (value < 50) {
                    value = 50;
                }
            }
        }

        // We might need more tools for a building or a pioneer:
        AIUnit unequippedHardyPioneer = getUnequippedHardyPioneer();
        final boolean needsPioneer = (TileImprovementPlans.size() > 0 || unequippedHardyPioneer != null
                && PioneeringMission.isValid(unequippedHardyPioneer));
        int toolsRequiredForBuilding = 0;
        if (colony.getCurrentlyBuilding() != BuildableType.NOTHING) {
            toolsRequiredForBuilding = colony.getCurrentlyBuilding().getToolsRequired();
        }
        if (colony.getProductionNetOf(Goods.TOOLS) == 0 && (colony.getGoodsCount(Goods.TOOLS) < 20 && needsPioneer)
                || toolsRequiredForBuilding > colony.getGoodsCount(Goods.TOOLS)) {
            int goodsWishValue = AIGoods.TOOLS_FOR_COLONY_PRIORITY
                    + AIGoods.TOOLS_FOR_IMPROVEMENT
                    * TileImprovementPlans.size()
                    + ((unequippedHardyPioneer != null) ? AIGoods.TOOLS_FOR_PIONEER : 0)
                    + ((toolsRequiredForBuilding > colony.getGoodsCount(Goods.TOOLS)) ? AIGoods.TOOLS_FOR_BUILDING
                            + (toolsRequiredForBuilding - colony.getGoodsCount(Goods.TOOLS)) : 0);
            boolean goodsOrdered = false;
            for (Wish w : wishes) {
                if (w instanceof GoodsWish) {
                    GoodsWish gw = (GoodsWish) w;
                    // TODO: check for a certain required amount?
                    if (gw.getGoodsType() == Goods.TOOLS) {
                        gw.value = goodsWishValue;
                        goodsOrdered = true;
                        break;
                    }
                }
            }
            if (!goodsOrdered) {
                GoodsWish gw = new GoodsWish(getAIMain(), colony, goodsWishValue, Goods.TOOLS);
                wishes.add(gw);
            }
        } else {
            disposeAllToolsGoodsWishes();
        }

        disposeUnwantedWishes(newWishes);        

        Collections.sort(wishes, new Comparator<Wish>() {
            public int compare(Wish o, Wish p) {
                Integer i = o.getValue();
                Integer j = p.getValue();
                return j.compareTo(i);
            }
        });

    }

    /**
     * Dispose all goods wishes for tools. Note the two-pass approach
     * in order to avoid concurrent modification exceptions!
     */
    private void disposeAllToolsGoodsWishes() {
        List<GoodsWish> toolsWishes = new ArrayList<GoodsWish>();
        for (Wish w : wishes) {
            if (w instanceof GoodsWish) {
                GoodsWish gw = (GoodsWish) w;
                // TODO: check for a certain required amount?
                if (gw.getGoodsType() == Goods.TOOLS) {
                    toolsWishes.add(gw);
                }
            }
        }
        for(GoodsWish gw : toolsWishes) {
            gw.dispose();
        }
    }

    /**
     * Dispose wishes no longer relevant. For worker wishes this is all
     * wishes not present among the new ones. For goods wishes the current
     * criteria (TO BE CHANGED) is at least 20 of the given goods. Other
     * types of wishes are not supported.
     * <p>
     * Note that the wishes are disposed in a separate pass. This is necessary
     * or there will be a {@link ConcurrentModificationException}.
     * 
     * @param newWishes The new wishes.
     */
    private void disposeUnwantedWishes(List<Wish> newWishes) {
        List<Wish> wishesToDispose = new ArrayList<Wish>();
        for (Wish w : wishes) {
            if (w instanceof WorkerWish) {
                if (!newWishes.contains(w)) {
                    wishesToDispose.add(w);
                }
            } else if (w instanceof GoodsWish) {
                GoodsWish gw = (GoodsWish) w;
                // TODO: check for a certain required amount?
                if (getColony().getGoodsCount(gw.getGoodsType()) >= 20) {
                    wishesToDispose.add(gw);
                }
            } else {
                logger.warning("Unknown type of Wish: " + w + " for " + this);
            }
        }
        for (Wish w : wishesToDispose) {
            w.dispose();
        }
    }

    /**
     * Returns an unequipped pioneer that is either inside this colony or
     * standing on the same <code>Tile</code>.
     * 
     * @return A unit with a {@link PioneeringMission} or a unit being a
     *         {@link Unit#HARDY_PIONEER hardy pioneer} - and with no tools.
     *         Returns <code>null</code> if no such unit was found.
     */
    private AIUnit getUnequippedHardyPioneer() {
        Iterator<Unit> ui = colony.getTile().getUnitIterator();
        while (ui.hasNext()) {
            Unit u = ui.next();
            AIUnit au = (AIUnit) getAIMain().getAIObject(u);
            if (au.getMission() != null && au.getMission() instanceof PioneeringMission && u.getNumberOfTools() == 0) {
                return au;
            }
        }
        ui = colony.getUnitIterator();
        while (ui.hasNext()) {
            Unit u = ui.next();
            AIUnit au = (AIUnit) getAIMain().getAIObject(u);
            if (u.hasAbility("model.ability.expertPioneer")) {
                return au;
            }
        }
        return null;
    }

    public void removeWish(Wish w) {
        wishes.remove(w);
    }

    /**
     * Creates a list of the goods which should be shipped to and from this
     * colony.
     */
    public void createAIGoods() {
        createExportAIGoodsList();
        // TODO: createGoodsWishes()
    }

    /**
     * Add a <code>GoodsWish</code> to the wish list.
     * 
     * @param gw The <code>GoodsWish</code> to be added.
     */
    public void addGoodsWish(GoodsWish gw) {
        wishes.add(gw);
    }

    /**
     * Removes the given <code>AIGoods</code> from this colony's list. The
     * <code>AIGoods</code>-object is not disposed as part of this operation.
     * Use that method instead to remove the object completely (this method
     * would then be called indirectly).
     * 
     * @param ag The <code>AIGoods</code> to be removed.
     * @see AIGoods#dispose()
     */
    public void removeAIGoods(AIGoods ag) {
        while (aiGoods.remove(ag)) { /* Do nothing here */
        }
    }

    /**
     * Creates a list of the goods which should be shipped out of this colony.
     * This is the list {@link #getAIGoodsIterator} returns the
     * <code>Iterator</code> for.
     */
    private void createExportAIGoodsList() {
        ArrayList<AIGoods> newAIGoods = new ArrayList<AIGoods>();

        // TODO: Do not sell raw material we are lacking.

        List<GoodsType> goodsList = FreeCol.getSpecification().getGoodsTypeList();
        for (GoodsType goodsType : goodsList) {
            // Never export food and lumber
            if (goodsType == Goods.FOOD || goodsType == Goods.LUMBER) {
                continue;
            }
            // Never export unstorable goods
            if (!goodsType.isStorable()) {
                continue;
            }
            // Only export muskets if we do not have room for them:
            if (goodsType == Goods.MUSKETS
                    && (colony.getProductionOf(Goods.MUSKETS) == 0 || colony.getGoodsCount(Goods.MUSKETS) < colony
                            .getWarehouseCapacity()
                            - colony.getProductionOf(Goods.MUSKETS))) {
                continue;
            }
            // Only export horses if we do not have room for them:
            if (goodsType == Goods.HORSES
                    && (colony.getProductionOf(Goods.HORSES) == 0 || colony.getGoodsCount(Goods.HORSES) < colony
                            .getWarehouseCapacity()
                            - colony.getProductionOf(Goods.HORSES))) {
                continue;
            }

            /*
             * Only export tools if we are producing it in this colony and have
             * sufficient amounts in warehouse:
             */
            if (goodsType == Goods.TOOLS && colony.getGoodsCount(Goods.TOOLS) > 0) {
                if (colony.getProductionNetOf(Goods.TOOLS) > 0) {
                    final BuildableType currentlyBuilding = colony.getCurrentlyBuilding();
                    int requiredTools = currentlyBuilding.getToolsRequired();
                    int requiredHammers = currentlyBuilding.getHammersRequired();
                    int buildTurns = (requiredHammers - colony.getHammers()) /
                        (colony.getProductionOf(Goods.HAMMERS) + 1);
                    if (requiredTools > 0) {
                        if (colony.getWarehouseCapacity() > 100) {
                            requiredTools += 100;
                        }
                        int toolsProductionTurns = requiredTools / colony.getProductionNetOf(Goods.TOOLS);
                        if (buildTurns <= toolsProductionTurns + 1) {
                            continue;
                        }
                    } else if (colony.getWarehouseCapacity() > 100 && colony.getGoodsCount(Goods.TOOLS) <= 100) {
                        continue;
                    }
                } else {
                    continue;
                }
            }

            if (colony.getGoodsCount(goodsType) > 0) {
                List<AIGoods> alreadyAdded = new ArrayList<AIGoods>();
                for (int j = 0; j < aiGoods.size(); j++) {
                    AIGoods ag = aiGoods.get(j);
                    if (ag == null) {
                        logger.warning("aiGoods == null");
                    } else if (ag.getGoods() == null) {
                        logger.warning("aiGoods.getGoods() == null");
                        if (ag.isUninitialized()) {
                            logger.warning("AIGoods uninitialized: " + ag.getId());
                        }
                    }
                    if (ag != null && ag.getGoods() != null && ag.getGoods().getType() == goodsType
                            && ag.getGoods().getLocation() == colony) {
                        alreadyAdded.add(ag);
                    }
                }

                int amountRemaining = colony.getGoodsCount(goodsType);
                for (int i = 0; i < alreadyAdded.size(); i++) {
                    AIGoods oldGoods = alreadyAdded.get(i);
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
                                ((TransportMission) oldGoods.getTransport().getMission())
                                        .removeFromTransportList(oldGoods);
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
                        AIGoods newGoods = new AIGoods(getAIMain(), colony, goodsType, 100, getColony().getOwner()
                                .getEurope());
                        if (amountRemaining >= colony.getWarehouseCapacity()) {
                            newGoods.setTransportPriority(AIGoods.IMPORTANT_DELIVERY);
                        } else {
                            newGoods.setTransportPriority(AIGoods.FULL_DELIVERY);
                        }
                        newAIGoods.add(newGoods);
                        amountRemaining -= 100;
                    } else {
                        AIGoods newGoods = new AIGoods(getAIMain(), colony, goodsType, amountRemaining, getColony()
                                .getOwner().getEurope());
                        newAIGoods.add(newGoods);
                        amountRemaining = 0;
                    }
                }
            }
        }

        aiGoods.clear();
        Iterator<AIGoods> nai = newAIGoods.iterator();
        while (nai.hasNext()) {
            AIGoods ag = nai.next();
            int i;
            for (i = 0; i < aiGoods.size() && aiGoods.get(i).getTransportPriority() > ag.getTransportPriority(); i++)
                ;
            aiGoods.add(i, ag);
        }
    }

    /**
     * Returns the available amount of tools.
     * 
     * @return The amount of tools not needed for the next thing we are
     *         building.
     */
    public int getAvailableTools() {
        int toolsRequiredForBuilding = 0;
        if (colony.getCurrentlyBuilding() != BuildableType.NOTHING) {
            toolsRequiredForBuilding = colony.getCurrentlyBuilding().getToolsRequired();
        }

        return Math.max(0, colony.getGoodsCount(Goods.TOOLS) - toolsRequiredForBuilding);
    }

    /**
     * Rearranges the workers within this colony. This is done according to the
     * {@link ColonyPlan}, although minor adjustments can be done to increase
     * production.
     * 
     * @param connection The <code>Connection</code> to be used when
     *            communicating with the server.
     */
    public void rearrangeWorkers(Connection connection) {
        colonyPlan.create();

        // TODO: Detect a siege and move the workers temporarily around.

        // Move a pioneer outside the colony if we have a sufficient amount of
        // tools:
        if (colony.getUnitCount() > 1 && getAvailableTools() >= 20) {
            AIUnit unequippedPioneer = getUnequippedHardyPioneer();
            if (unequippedPioneer != null
                    && (unequippedPioneer.getMission() == null || !(unequippedPioneer.getMission() instanceof PioneeringMission))
                    && PioneeringMission.isValid(unequippedPioneer)) {
                unequippedPioneer.getUnit().setLocation(colony.getTile());
                unequippedPioneer.setMission(new PioneeringMission(getAIMain(), unequippedPioneer));
            }
        }

        List<Unit> units = new ArrayList<Unit>();
        List<WorkLocationPlan> workLocationPlans = colonyPlan.getWorkLocationPlans();
        Collections.sort(workLocationPlans, new Comparator<WorkLocationPlan>() {
            public int compare(WorkLocationPlan o, WorkLocationPlan p) {
                Integer i = o.getProductionOf(o.getGoodsType());
                Integer j = p.getProductionOf(p.getGoodsType());

                return j.compareTo(i);
            }
        });

        // Remove all colonists from the colony:
        Iterator<Unit> ui = colony.getUnitIterator();
        while (ui.hasNext()) {
            Unit unit = ui.next();
            units.add(unit);
            unit.setLocation(null);
        }

        // Place all the experts:
        Iterator<Unit> uit = units.iterator();
        while (uit.hasNext()) {
            Unit unit = uit.next();
            if (unit.getExpertWorkType() != null) {
                Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
                while (wlpIterator.hasNext()) {
                    WorkLocationPlan wlp = wlpIterator.next();
                    WorkLocation wl = wlp.getWorkLocation();
                    if (unit.getExpertWorkType() == wlp.getGoodsType()
                            && wlp.getWorkLocation().canAdd(unit)
                            && (wlp.getGoodsType() != Goods.FOOD || !((ColonyTile) wl).getWorkTile().isLand()
                                    && unit.getType() == Unit.EXPERT_FISHERMAN
                                    && colony.hasAbility("model.ability.produceInWater")
                                    || ((ColonyTile) wl).getWorkTile().isLand()
                                    && unit.getType() != Unit.EXPERT_FISHERMAN)) {
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
            for (int i = 0; i < workLocationPlans.size() && food < 2; i++) {
                WorkLocationPlan wlp = workLocationPlans.get(i);
                WorkLocation wl = wlp.getWorkLocation();
                if (wlp.getGoodsType() == Goods.FOOD
                        && (((ColonyTile) wl).getWorkTile().isLand()
                        || colony.hasAbility("model.ability.produceInWater"))) {
                    Unit bestUnit = null;
                    int bestProduction = 0;
                    Iterator<Unit> unitIterator = units.iterator();
                    while (unitIterator.hasNext()) {
                        Unit unit = unitIterator.next();
                        int production = ((ColonyTile) wlp.getWorkLocation()).getProductionOf(unit,
                                Goods.FOOD);
                        if (production > 1
                                && (bestUnit == null || production > bestProduction || production == bestProduction
                                        && unit.getSkillLevel() < bestUnit.getSkillLevel())) {
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
                for (int i = 0; i < workLocationPlans.size(); i++) {
                    WorkLocationPlan wlp = workLocationPlans.get(i);
                    if (wlp.getGoodsType() != Goods.FOOD) {
                        Unit bestUnit = null;
                        int bestProduction = 0;
                        Iterator<Unit> unitIterator = units.iterator();
                        while (unitIterator.hasNext()) {
                            Unit unit = unitIterator.next();
                            int production;
                            WorkLocation location = wlp.getWorkLocation();
                            if (location instanceof ColonyTile) {
                                production = ((ColonyTile) wlp.getWorkLocation()).getProductionOf(unit,
                                        wlp.getGoodsType());
                            } else { // Building
                                production = ((Building) location).getProductivity(unit);
                            }
                            if (bestUnit == null || production > bestProduction || production == bestProduction
                                    && unit.getSkillLevel() < bestUnit.getSkillLevel()) {
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
            Iterator<WorkLocation> wlIterator = colony.getWorkLocationIterator();
            WorkLocation bestPick = null;
            while (wlIterator.hasNext()) {
                WorkLocation wl = wlIterator.next();
                if (wl.getUnitCount() > 0) {
                    if (wl instanceof ColonyTile) {
                        ColonyTile ct = (ColonyTile) wl;
                        Unit u = ct.getUnit();
                        if (ct.getUnit().getWorkType() != Goods.FOOD) {
                            int uProduction = ct.getProductionOf(u, Goods.FOOD);
                            if (uProduction > 1) {
                                if (bestPick == null || bestPick instanceof Building) {
                                    bestPick = wl;
                                } else {
                                    ColonyTile bpct = (ColonyTile) bestPick;
                                    int bestPickProduction = bpct.getProductionOf(bpct.getUnit(), Goods.FOOD);
                                    if (uProduction > bestPickProduction
                                            || (uProduction == bestPickProduction && u.getSkillLevel() < bpct.getUnit()
                                                    .getSkillLevel())) {
                                        bestPick = wl;
                                    }
                                }
                            } else {
                                if (bestPick == null) {
                                    bestPick = wl;
                                } // else - TODO: This might be the best pick
                                // sometimes:
                            }
                        }
                    } else { // wl instanceof Building
                        if (bestPick == null
                                || (bestPick instanceof Building && ((Building) wl).getProduction() < ((Building) bestPick)
                                        .getProduction())) {
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
                if (ct.getProductionOf(u, Goods.FOOD) > 1) {
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
                Iterator<Unit> unitIterator = b.getUnitIterator();
                Unit bestUnit = unitIterator.next();
                while (unitIterator.hasNext()) {
                    Unit u = unitIterator.next();
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
        Iterator<WorkLocation> wlIterator = colony.getWorkLocationIterator();
        while (wlIterator.hasNext()) {
            WorkLocation wl = wlIterator.next();
            while (wl.getUnitCount() > 0 && wl instanceof Building && ((Building) wl).getProductionNextTurn() <= 0) {
                Iterator<Unit> unitIterator = wl.getUnitIterator();
                Unit bestPick = unitIterator.next();
                while (unitIterator.hasNext()) {
                    Unit u = unitIterator.next();
                    if (u.getExpertWorkType() != u.getWorkType()) {
                        bestPick = u;
                        break;
                    }
                }
                GoodsType rawMaterial = bestPick.getWorkType().getRawMaterial();
                ColonyTile ct = (rawMaterial != null) ? colony.getVacantColonyTileFor(bestPick, rawMaterial) : null;
                if (ct != null) {
                    bestPick.setLocation(ct);
                    bestPick.setWorkType(rawMaterial);
                } else {
                    Building th = colony.getBuildingForProducing(Goods.BELLS);
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

        // Changes the production type of workers producing a cargo there
        // is no room for.
        List<GoodsType> goodsList = FreeCol.getSpecification().getGoodsTypeList();
        for (GoodsType goodsType : goodsList) {
        	int production = colony.getProductionNetOf(goodsType);
        	int in_stock = colony.getGoodsCount(goodsType);
        	if (Goods.FOOD != goodsType && production + in_stock > colony.getWarehouseCapacity()) {
        		Iterator<Unit> unitIterator = colony.getUnitIterator();
        		int waste = production + in_stock - colony.getWarehouseCapacity();
        		while (unitIterator.hasNext() && waste > 0){
        			Unit unit = unitIterator.next();
        			if (unit.getWorkType() == goodsType) {
                        final Location oldLocation = unit.getLocation();
        				unit.setLocation(null);
        				boolean working = false;
        				waste = colony.getGoodsCount(goodsType) + colony.getProductionNetOf(goodsType)
        					    - colony.getWarehouseCapacity();
        				int best = 0;
                        for (GoodsType goodsType2 : goodsList) {
                            if (!goodsType2.isFarmed())
                                continue;
        					int production2 = colony.getVacantColonyTileProductionFor(unit, goodsType2);
        					if (production2 > best && production2 + colony.getGoodsCount(goodsType2)
        							+ colony.getProductionNetOf(goodsType2) < colony.getWarehouseCapacity()){
        						if (working){
        							unit.setLocation(null);
        						}
        						unit.setLocation(colony.getVacantColonyTileFor(unit, goodsType2));
                                unit.setWorkType(goodsType2);
        						best = production2;
        						working = true;
        					}
        				}
        				if (!working){
        					//units.add(unit);
                            /*
                             * Keep the unit inside the colony. Units outside
                             * colonies are assigned Missions.
                             */
                            // TODO: Create a Mission for units temporarily moved outside colonies.
                            unit.setLocation(oldLocation);
        				}
        			}
        		}
        	}
        }


        // Use any remaining food plans:
        for (int i = 0; i < workLocationPlans.size(); i++) {
            WorkLocationPlan wlp = workLocationPlans.get(i);
            WorkLocation wl = wlp.getWorkLocation();
            if (wlp.getGoodsType() == Goods.FOOD
                    && (((ColonyTile) wl).getWorkTile().isLand()
                    || colony.hasAbility("model.ability.produceInWater"))) {
                Unit bestUnit = null;
                int bestProduction = 0;
                Iterator<Unit> unitIterator = units.iterator();
                while (unitIterator.hasNext()) {
                    Unit unit = unitIterator.next();
                    int production = ((ColonyTile) wlp.getWorkLocation()).getProductionOf(unit,
                            Goods.FOOD);
                    if (production > 1
                            && (bestUnit == null || production > bestProduction || production == bestProduction
                                    && unit.getSkillLevel() < bestUnit.getSkillLevel())) {
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
        Iterator<Unit> ui6 = units.iterator();
        while (ui6.hasNext()) {
            Unit u = ui6.next();
            u.setLocation(colony.getTile());
            AIUnit au = (AIUnit) getAIMain().getAIObject(u);
            if (au.getMission() instanceof WorkInsideColonyMission) {
                au.setMission(null);
            }
        }

        // FIXME: should be executed just once, when the custom house is built
        if (colony.hasAbility("model.ability.export")) {
            colony.setExports(Goods.SILVER, true);
            colony.setExports(Goods.RUM, true);
            colony.setExports(Goods.CIGARS, true);
            colony.setExports(Goods.CLOTH, true);
            colony.setExports(Goods.COATS, true);
        }

        // temporarily move units from carpenter to blacksmith if the current building needs no lumber but more tools
        // TODO: mine ore if needed
        BuildableType nowbuilding = colony.getCurrentlyBuilding();
        if (nowbuilding != BuildableType.NOTHING &&
            nowbuilding.getHammersRequired() <= colony.getHammers() &&
            nowbuilding.getToolsRequired() > colony.getGoodsCount(Goods.TOOLS)) {
            Building carpenter = colony.getBuildingForProducing(Goods.HAMMERS);
            Building blacksmith = colony.getBuildingForProducing(Goods.TOOLS);
            for (Unit unit : carpenter.getUnitList()) {
                if (carpenter.getUnitCount() > 0 && blacksmith.canAdd(carpenter.getFirstUnit())) {
                    carpenter.getFirstUnit().setLocation(blacksmith);
                }
            }
        }
        decideBuildable(connection);
        createTileImprovementPlans();
        createWishes();
    }

    /**
     * Decides what to build in the <code>Colony</code>.
     * 
     * @param connection The connection to use when communicating with the
     *            server.
     */
    private void decideBuildable(Connection connection) {
        // TODO: Request tools if needed.
        int hammersOld = colony.getCurrentlyBuilding().getHammersRequired();

        boolean isOldValid = colony.canBuild();
        
        Iterator<BuildableType> bi = colonyPlan.getBuildable();
        while (bi.hasNext()) {
            BuildableType buildable = bi.next();

            if (buildable == colony.getCurrentlyBuilding()) {
                // We are building the right item already:
                break;
            }

            int hammersNew = buildable.getHammersRequired();
            if (hammersNew > colony.getHammers() || hammersNew > hammersOld || !isOldValid) {
                Element setCurrentlyBuildingElement = Message.createNewRootElement("setCurrentlyBuilding");
                setCurrentlyBuildingElement.setAttribute("colony", colony.getId());
                setCurrentlyBuildingElement.setAttribute("type", buildable.getId());

                try {
                    connection.sendAndWait(setCurrentlyBuildingElement);
                } catch (IOException e) {
                    logger.warning("Could not send \"setCurrentlyBuilding\"-message.");
                }

                // We have found something to build:
                break;
            }
        }
    }

    /**
     * Determines the best goods to produce on a given <code>Tile</code>
     * within this colony.
     * 
     * @param t The <code>Tile</code>.
     * @return The type of goods.
     * 
     * private int getBestGoodsToProduce(Tile t) { if (t.isForested()) { if
     * (t.getType() == Tile.GRASSLANDS || t.getType() == Tile.SAVANNAH) { return
     * Goods.LUMBER; } else { return Goods.FURS; } } if (t.getAddition() ==
     * Tile.ADD_HILLS) { return Goods.ORE; } if (t.getAddition() ==
     * Tile.ADD_MOUNTAINS) { if (t.hasBonus()) { return Goods.SILVER; } else {
     * return Goods.ORE; } } if (!t.isLand()) { return Goods.FOOD; } if
     * (t.getType() == Tile.DESERT) { if (t.hasBonus()) { return Goods.FOOD; }
     * else { return Goods.ORE; } } switch(t.getType()) { case Tile.SWAMP: case
     * Tile.PLAINS: return Goods.FOOD; case Tile.PRAIRIE: return Goods.COTTON;
     * case Tile.GRASSLANDS: return Goods.TOBACCO; case Tile.SAVANNAH: return
     * Goods.SUGAR; case Tile.MARSH: case Tile.TUNDRA: case Tile.ARCTIC:
     * default: return Goods.ORE; } }
     */

    /**
     * Writes this object to an XML stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getId());

        Iterator<AIGoods> aiGoodsIterator = aiGoods.iterator();
        while (aiGoodsIterator.hasNext()) {
            AIGoods ag = aiGoodsIterator.next();
            if (ag == null) {
                logger.warning("ag == null");
                continue;
            }
            if (ag.getId() == null) {
                logger.warning("ag.getId() == null");
                continue;
            }
            out.writeStartElement(AIGoods.getXMLElementTagName() + "ListElement");
            out.writeAttribute("ID", ag.getId());
            out.writeEndElement();
        }

        Iterator<Wish> wishesIterator = wishes.iterator();
        while (wishesIterator.hasNext()) {
            Wish w = wishesIterator.next();
            if (!w.shouldBeStored()) {
                continue;
            }
            if (w instanceof WorkerWish) {
                out.writeStartElement(WorkerWish.getXMLElementTagName() + "WishListElement");
            } else if (w instanceof GoodsWish) {
                out.writeStartElement(GoodsWish.getXMLElementTagName() + "WishListElement");
            } else {
                logger.warning("Unknown type of wish.");
                continue;
            }
            out.writeAttribute("ID", w.getId());
            out.writeEndElement();
        }

        Iterator<TileImprovementPlan> TileImprovementPlanIterator = TileImprovementPlans.iterator();
        while (TileImprovementPlanIterator.hasNext()) {
            TileImprovementPlan ti = TileImprovementPlanIterator.next();
            out.writeStartElement(TileImprovementPlan.getXMLElementTagName() + "ListElement");
            out.writeAttribute("ID", ti.getId());
            out.writeEndElement();
        }

        out.writeEndElement();
    }

    /**
     * Reads information for this object from an XML stream.
     * 
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading from the
     *             stream.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        colony = (Colony) getAIMain().getFreeColGameObject(in.getAttributeValue(null, "ID"));
        if (colony == null) {
            throw new NullPointerException("Could not find Colony with ID: " + in.getAttributeValue(null, "ID"));
        }

        aiGoods.clear();
        wishes.clear();

        colonyPlan = new ColonyPlan(getAIMain(), colony);
        colonyPlan.create();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(AIGoods.getXMLElementTagName() + "ListElement")) {
                AIGoods ag = (AIGoods) getAIMain().getAIObject(in.getAttributeValue(null, "ID"));
                if (ag == null) {
                    ag = new AIGoods(getAIMain(), in.getAttributeValue(null, "ID"));
                }
                aiGoods.add(ag);
                in.nextTag();
            } else if (in.getLocalName().equals(WorkerWish.getXMLElementTagName() + "WishListElement")) {
                Wish w = (Wish) getAIMain().getAIObject(in.getAttributeValue(null, "ID"));
                if (w == null) {
                    w = new WorkerWish(getAIMain(), in.getAttributeValue(null, "ID"));
                }
                wishes.add(w);
                in.nextTag();
            } else if (in.getLocalName().equals(GoodsWish.getXMLElementTagName() + "WishListElement")) {
                Wish w = (Wish) getAIMain().getAIObject(in.getAttributeValue(null, "ID"));
                if (w == null) {
                    w = new GoodsWish(getAIMain(), in.getAttributeValue(null, "ID"));
                }
                wishes.add(w);
                in.nextTag();
            } else if (in.getLocalName().equals(TileImprovementPlan.getXMLElementTagName() + "ListElement")) {
                TileImprovementPlan ti = (TileImprovementPlan) getAIMain().getAIObject(in.getAttributeValue(null, "ID"));
                if (ti == null) {
                    ti = new TileImprovementPlan(getAIMain(), in.getAttributeValue(null, "ID"));
                }
                TileImprovementPlans.add(ti);
                in.nextTag();
            } else {
                logger.warning("Unknown tag name: " + in.getLocalName());
            }
        }

        if (!in.getLocalName().equals(getXMLElementTagName())) {
            logger.warning("Expected end tag, received: " + in.getLocalName());
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return "aiColony"
     */
    public static String getXMLElementTagName() {
        return "aiColony";
    }
}
