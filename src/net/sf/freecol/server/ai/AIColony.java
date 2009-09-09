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
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.ClaimLandMessage;
import net.sf.freecol.server.ai.mission.PioneeringMission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.WorkInsideColonyMission;

import org.w3c.dom.Element;

/**
 * Objects of this class contains AI-information for a single {@link Colony}.
 */
public class AIColony extends AIObject {

    private static final Logger logger = Logger.getLogger(AIColony.class.getName());

    private static final EquipmentType toolsType = FreeCol.getSpecification().getEquipmentType("model.equipment.tools");

    /**
     * The FreeColGameObject this AIObject contains AI-information for.
     */
    private Colony colony;

    private ColonyPlan colonyPlan;

    private ArrayList<AIGoods> aiGoods = new ArrayList<AIGoods>();

    private ArrayList<Wish> wishes = new ArrayList<Wish>();

    private ArrayList<TileImprovementPlan> tileImprovementPlans = new ArrayList<TileImprovementPlan>();


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
        for(TileImprovementPlan ti : tileImprovementPlans) {
            disposeList.add(ti);
        }
        for(AIObject o : disposeList) {
            o.dispose();
        }
        super.dispose();
    }
    
    /**
     * Updates custom house export settings.
     */
    private void updateCustomHouse() {
        int capacity = colony.getWarehouseCapacity();
        for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
            if (goodsType.isTradeGoods()) {
                // can only be produced in Europe
                colony.setExportData(new ExportData(goodsType, false, 0));
            } else if (!goodsType.isStorable()) {
                // abstract goods such as hammers
                colony.setExportData(new ExportData(goodsType, false, 0));
            } else if (goodsType.isBreedable()) {
                colony.setExportData(new ExportData(goodsType, true, capacity - 20));
            } else if (goodsType.isMilitaryGoods()) {
                colony.setExportData(new ExportData(goodsType, true, capacity - 50));
            } else if (goodsType.isBuildingMaterial()) {
                colony.setExportData(new ExportData(goodsType, true, Math.min(capacity, 250)));
            } else if (goodsType.isFoodType()) {
                colony.setExportData(new ExportData(goodsType, false, 0));
            } else if (goodsType.isNewWorldGoodsType() || goodsType.isRefined()) {
                colony.setExportData(new ExportData(goodsType, true, 0));
            } else {
                colony.setExportData(new ExportData(goodsType, false, 0));
            }
        }
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

        Map<Tile, TileImprovementPlan> plans =
            new HashMap<Tile, TileImprovementPlan>();
        for (TileImprovementPlan plan : tileImprovementPlans) {
            plans.put(plan.getTarget(), plan);
        }
        for (WorkLocationPlan wlp : colonyPlan.getWorkLocationPlans()) {
            if (wlp.getWorkLocation() instanceof ColonyTile) {
                ColonyTile colonyTile = (ColonyTile) wlp.getWorkLocation();
                Tile target = colonyTile.getWorkTile();
                TileImprovementPlan plan = plans.get(target);
                if (plan == null) {
                    plan = wlp.createTileImprovementPlan();
                    if (plan != null) {
                        if (colonyTile.getUnit() != null) {
                            plan.setValue(2 * plan.getValue());
                        }
                        tileImprovementPlans.add(plan);
                        plans.put(target, plan);
                    }
                } else if (wlp.updateTileImprovementPlan(plan) == null) {
                    tileImprovementPlans.remove(plan);
                    plan.dispose();
                } else if (colonyTile.getUnit() != null) {
                    plan.setValue(2 * plan.getValue());
                }
            }
        }

        Tile centerTile = colony.getTile();
        TileImprovementPlan centerPlan = plans.get(centerTile);
        TileImprovementType type = TileImprovement
            .findBestTileImprovementType(centerTile, FreeCol.getSpecification()
                                         .getGoodsType("model.goods.food"));
        if (type == null) {
            if (centerPlan != null) {
                tileImprovementPlans.remove(centerPlan);
            }
        } else {
            if (centerPlan == null) {
                centerPlan = new TileImprovementPlan(getAIMain(), colony.getTile(), type, 30);
                tileImprovementPlans.add(0, centerPlan);
            } else {
                centerPlan.setType(type);
            }
        }

        Collections.sort(tileImprovementPlans);
    }

    /**
     * Returns an <code>Iterator</code> over all the
     * <code>TileImprovementPlan</code>s needed by this colony.
     * 
     * @return The <code>Iterator</code>.
     * @see TileImprovementPlan
     */
    public Iterator<TileImprovementPlan> getTileImprovementPlanIterator() {
        return tileImprovementPlans.iterator();
    }
    
    /**
     * Removes a <code>TileImprovementPlan</code> from the list
     * @return True if it was successfully deleted, false otherwise 
     */
    public boolean removeTileImprovementPlan(TileImprovementPlan plan){
        return tileImprovementPlans.remove(plan);
    }
    
    /**
     * Creates the wishes for the <code>Colony</code>.
     */
    private void createWishes() {
        wishes.clear();
        int expertValue = 100;
        int goodsWishValue = 50;

        // for every non-expert, request expert replacement
        for (Unit unit : colony.getUnitList()) {
            if (unit.getWorkType() != null
                && unit.getWorkType() != unit.getType().getExpertProduction()) {
                UnitType expert = FreeCol.getSpecification().getExpertForProducing(unit.getWorkType());
                wishes.add(new WorkerWish(getAIMain(), colony, expertValue, expert, true));
            }
        }

        // request population increase
        if (wishes.isEmpty()) {
            int newPopulation = colony.getUnitCount() + 1;
            if (colony.governmentChange(newPopulation) >= 0) {
                // population increase incurs no penalty
                boolean needFood = colony.getFoodProduction()
                    <= newPopulation * Colony.FOOD_CONSUMPTION;
                // choose expert for best work location plan
                UnitType expert = getNextExpert(needFood);
                wishes.add(new WorkerWish(getAIMain(), colony, expertValue / 5, expert, false));
            }
        }

        // TODO: check for students

        // increase defense value
        int defence = 0;
        for (Unit unit : colony.getTile().getUnitList()) {
            // TODO: better algorithm to determine defence
            // should be located in combat model?
            defence += unit.getType().getDefence();
            if (unit.isArmed()) {
                defence += 1;
            }
            if (unit.isMounted()) {
                defence += 1;
            }
        }

        // TODO: is this heuristic suitable?
        boolean badlyDefended = defence < 3 * colony.getUnitCount();
        if (badlyDefended) {
            UnitType bestDefender = null;
            for (UnitType unitType : FreeCol.getSpecification().getUnitTypeList()) {
                if ((bestDefender == null
                     || bestDefender.getDefence() < unitType.getDefence())
                    && !unitType.hasAbility("model.ability.navalUnit")
                    && unitType.isAvailableTo(colony.getOwner())) {
                    bestDefender = unitType;
                }
            }
            if (bestDefender != null) {
                wishes.add(new WorkerWish(getAIMain(), colony, expertValue, bestDefender, true));
            }
        }

        // request goods
        // TODO: improve heuristics
        TypeCountMap<GoodsType> requiredGoods = new TypeCountMap<GoodsType>();

        // add building materials
        if (colony.getCurrentlyBuilding() != null) {
            for (AbstractGoods goods : colony.getCurrentlyBuilding().getGoodsRequired()) {
                if (colony.getProductionNetOf(goods.getType()) == 0) {
                    requiredGoods.incrementCount(goods.getType(), goods.getAmount());
                }
            }
        }

        // add materials required to improve tiles
        for (TileImprovementPlan plan : tileImprovementPlans) {
            for (AbstractGoods goods : plan.getType().getExpendedEquipmentType()
                     .getGoodsRequired()) {
                requiredGoods.incrementCount(goods.getType(), goods.getAmount());
            }
        }

        // add raw materials for buildings
        for (WorkLocation workLocation : colony.getWorkLocations()) {
            if (workLocation instanceof Building) {
                Building building = (Building) workLocation;
                GoodsType inputType = building.getGoodsInputType();
                if (inputType != null
                    && colony.getProductionNetOf(inputType) < building.getMaximumGoodsInput()) {
                    requiredGoods.incrementCount(inputType, 100);
                }
            }
        }

        // add breedable goods
        for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
            if (goodsType.isBreedable()) {
                requiredGoods.incrementCount(goodsType, goodsType.getBreedingNumber());
            }
        }

        // add materials required to build military equipment
        if (badlyDefended) {
            for (EquipmentType type : FreeCol.getSpecification().getEquipmentTypeList()) {
                if (type.isMilitaryEquipment()) {
                    for (Unit unit : colony.getUnitList()) {
                        if (unit.canBeEquippedWith(type)) {
                            for (AbstractGoods goods : type.getGoodsRequired()) {
                                requiredGoods.incrementCount(goods.getType(), goods.getAmount());
                            }
                            break;
                        }
                    }
                }
            }
        }

        for (GoodsType type : requiredGoods.keySet()) {
            GoodsType requiredType = type;
            while (requiredType != null && !requiredType.isStorable()) {
                requiredType = requiredType.getRawMaterial();
            }
            if (requiredType != null) {
                int amount = Math.min((requiredGoods.getCount(requiredType)
                                       - colony.getGoodsCount(requiredType)),
                                      colony.getWarehouseCapacity());
                if (amount > 0) {
                    int value = colonyCouldProduce(requiredType) ?
                        goodsWishValue / 10 : goodsWishValue;
                    wishes.add(new GoodsWish(getAIMain(), colony, value, amount, requiredType));
                }
            }
        }
        Collections.sort(wishes);
    }

    private boolean colonyCouldProduce(GoodsType goodsType) {
        if (goodsType.isBreedable()) {
            return colony.getGoodsCount(goodsType) >= goodsType.getBreedingNumber();
        } else if (goodsType.isFarmed()) {
            for (ColonyTile colonyTile : colony.getColonyTiles()) {
                if (colonyTile.getWorkTile().potential(goodsType, null) > 0) {
                    return true;
                }
            }
        } else {
            if (!colony.getBuildingsForProducing(goodsType).isEmpty()) {
                if (goodsType.getRawMaterial() == null) {
                    return true;
                } else {
                    return colonyCouldProduce(goodsType.getRawMaterial());
                }
            }
        }
        return false;
    }


    private UnitType getNextExpert(boolean onlyFood) {
        UnitType bestType = null;
        for (WorkLocationPlan plan : colonyPlan.getSortedWorkLocationPlans()) {
            if (plan.getGoodsType().isFoodType() || !onlyFood) {
                WorkLocation location = plan.getWorkLocation();
                if (location instanceof ColonyTile) {
                    ColonyTile colonyTile = (ColonyTile) location;
                    if (colonyTile.getUnit() == null
                        && (colonyTile.getWorkTile().isLand()
                            || colony.hasAbility("model.ability.produceInWater"))) {
                        bestType = FreeCol.getSpecification()
                            .getExpertForProducing(plan.getGoodsType());
                        break;
                    }
                } else if (location instanceof Building) {
                    Building building = (Building) location;
                    if (building.getUnitCount() < building.getMaxUnits()) {
                        bestType = building.getExpertUnitType();
                        break;
                    }
                }
            }
        }
        return bestType;
    }

    private int getToolsRequired(BuildableType buildableType) {
        int toolsRequiredForBuilding = 0;
        if (buildableType != null) {
            for (AbstractGoods goodsRequired : buildableType.getGoodsRequired()) {
                if (goodsRequired.getType() == Goods.TOOLS) {
                    toolsRequiredForBuilding = goodsRequired.getAmount();
                    break;
                }
            }
        }
        return toolsRequiredForBuilding;
    }


    private int getHammersRequired(BuildableType buildableType) {
        int hammersRequiredForBuilding = 0;
        if (buildableType != null) {
            for (AbstractGoods goodsRequired : buildableType.getGoodsRequired()) {
                if (goodsRequired.getType() == Goods.HAMMERS) {
                    hammersRequiredForBuilding = goodsRequired.getAmount();
                    break;
                }
            }
        }
        return hammersRequiredForBuilding;
    }


    /**
     * Returns an unequipped pioneer that is either inside this colony or
     * standing on the same <code>Tile</code>.
     * 
     * @return A suitable unit with no tools.
     *         Returns <code>null</code> if no such unit was found.
     */
    private AIUnit getUnequippedPioneer() {
        AIUnit bestPioneer = null;
        for (Unit unit : colony.getTile().getUnitList()) {
            if (unit.canBeEquippedWith(toolsType)) {
                AIUnit aiUnit = (AIUnit) getAIMain().getAIObject(unit);
                if (aiUnit.getMission() == null
                    || (aiUnit.getMission() instanceof PioneeringMission
                        && !unit.getEquipment().containsKey(toolsType))) {
                    if (unit.hasAbility("model.ability.expertPioneer")) {
                        bestPioneer = aiUnit;
                        // can't get better than that
                        break;
                    } else if (bestPioneer == null
                               || (unit.getType().getSkill()
                                   < bestPioneer.getUnit().getType().getSkill())) {
                        bestPioneer = aiUnit;
                    }
                }
            }
        }
        return bestPioneer;
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
        
        updateCustomHouse();
        if (colony.hasAbility("model.ability.export")) {
            // No need to export.
            aiGoods.clear();
            return;
        }

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
                    int requiredTools = getToolsRequired(currentlyBuilding);
                    int requiredHammers = getHammersRequired(currentlyBuilding);
                    int buildTurns = (requiredHammers - colony.getGoodsCount(Goods.HAMMERS)) /
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
     * Returns the available amount of the GoodsType given.
     * 
     * @return The amount of tools not needed for the next thing we are
     *         building.
     */
    public int getAvailableGoods(GoodsType goodsType) {
        int materialsRequiredForBuilding = 0;
        if (colony.getCurrentlyBuilding() != null) {
            for (AbstractGoods materials : colony.getCurrentlyBuilding().getGoodsRequired()) {
                if (materials.getType() == goodsType) {
                    materialsRequiredForBuilding = materials.getAmount();
                    break;
                }
            }
        }

        return Math.max(0, colony.getGoodsCount(goodsType) - materialsRequiredForBuilding);
    }


    /**
     * Returns <code>true</code> if this AIColony can build the given
     * type of equipment. Unlike the method of the Colony, this takes
     * goods "reserved" for building or breeding purposes into account.
     *
     * @param equipmentType an <code>EquipmentType</code> value
     * @return a <code>boolean</code> value
     * @see Colony#canBuildEquipment(EquipmentType equipmentType)
     */
    public boolean canBuildEquipment(EquipmentType equipmentType) {
        if (getColony().canBuildEquipment(equipmentType)) {
            for (AbstractGoods goods : equipmentType.getGoodsRequired()) {
                int breedingNumber = goods.getType().getBreedingNumber();
                if (breedingNumber != GoodsType.NO_BREEDING &&
                    getColony().getGoodsCount(goods.getType()) < goods.getAmount() + breedingNumber) {
                    return false;
                }
                if (getAvailableGoods(goods.getType()) < goods.getAmount()) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }


    /**
     * Find a colony's best tile to put a unit to produce a type of
     * goods.  Steals land from other settlements only when it is
     * free.
     *
     * @param connection The <code>Connection</code> to be used when
     *            communicating with the server.
     * @param unit The <code>Unit</code> to work the tile.
     * @param goodsType The type of goods to produce.
     * @return The best choice of available vacant colony tiles, or
     *         null if nothing suitable.
     */
    private ColonyTile getBestVacantTile(Connection connection,
                                         Unit unit, GoodsType goodsType) {
        ColonyTile colonyTile = colony.getVacantColonyTileFor(unit, goodsType);
        if (colonyTile == null) return null;

        // Check if the tile needs to be claimed from another settlement.
        Tile tile = colonyTile.getWorkTile();
        if (tile.getOwningSettlement() != colony) {
            ClaimLandMessage message = new ClaimLandMessage(tile, colony, 0);
            try {
                connection.sendAndWait(message.toXMLElement());
            } catch (IOException e) {
                logger.warning("Could not send \""
                               + message.getXMLElementTagName()
                               + "\"-message:" + e.getMessage());
            }
            if (tile.getOwningSettlement() != colony) {
                return null; // Claim failed.
            }
        }

        return colonyTile;
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
        if (colony.getUnitCount() > 1) {
            boolean canBuildTools = true;
            for (AbstractGoods materials : toolsType.getGoodsRequired()) {
                if (getAvailableGoods(materials.getType()) < materials.getAmount()) {
                    canBuildTools = false;
                    break;
                }
            }
            if (canBuildTools) {
                AIUnit unequippedPioneer = getUnequippedPioneer();
                if (unequippedPioneer != null
                    && (unequippedPioneer.getMission() == null ||
                        !(unequippedPioneer.getMission() instanceof PioneeringMission))
                    && PioneeringMission.isValid(unequippedPioneer)) {
                    unequippedPioneer.getUnit().putOutsideColony();
                    unequippedPioneer.setMission(new PioneeringMission(getAIMain(), unequippedPioneer));
                }
            }
        }

        List<Unit> units = new ArrayList<Unit>();
        List<WorkLocationPlan> workLocationPlans = colonyPlan.getWorkLocationPlans();
        Collections.sort(workLocationPlans);

        // Remove all colonists from the colony:
        Iterator<Unit> ui = colony.getUnitIterator();
        while (ui.hasNext()) {
            Unit unit = ui.next();
            units.add(unit);
            //don't set location to null, but to the tile of the colony this
            //unit is being removed from!
            unit.putOutsideColony();
        }

        // Place the experts:
        placeExpertsInWorkPlaces(units, workLocationPlans);

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
                        //use work() instead of setLocation()
                        //to make sure that unitState is properly updated!
                        bestUnit.work(wlp.getWorkLocation());
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
                            int production = 0;
                            WorkLocation location = wlp.getWorkLocation();
                            if (location instanceof ColonyTile) {
                                production = ((ColonyTile) wlp.getWorkLocation()).getProductionOf(unit,
                                                                                                  wlp.getGoodsType());
                            } else if (location instanceof Building) {
                                production = ((Building) location).getUnitProductivity(unit);
                            }
                            if (bestUnit == null || production > bestProduction || production == bestProduction
                                && unit.getSkillLevel() < bestUnit.getSkillLevel()) {
                                bestUnit = unit;
                                bestProduction = production;
                            }
                        }
                        if (bestUnit != null && wlp.getWorkLocation().canAdd(bestUnit)) {
                            //use work() instead of setLocation()
                            //to make sure that unitState is properly updated!
                            bestUnit.work(wlp.getWorkLocation());
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
            WorkLocation bestPick = null;
            for (WorkLocation wl : colony.getWorkLocations()) {
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
                    u.putOutsideColony();
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
                    if (u.getType().getExpertProduction() != u.getWorkType()) {
                        bestUnit = u;
                        break;
                    }
                }
                bestUnit.putOutsideColony();
                AIUnit au = (AIUnit) getAIMain().getAIObject(bestUnit);
                if (au.getMission() instanceof WorkInsideColonyMission) {
                    au.setMission(null);
                }
            }

            food = colony.getFoodProduction() - colony.getFoodConsumption();
        }

        // Move any workers not producing anything to a temporary location.
        for (WorkLocation wl : colony.getWorkLocations()) {
            while (wl.getUnitCount() > 0 && wl instanceof Building && ((Building) wl).getProductionNextTurn() <= 0) {
                Iterator<Unit> unitIterator = wl.getUnitIterator();
                Unit bestPick = unitIterator.next();
                while (unitIterator.hasNext()) {
                    Unit u = unitIterator.next();
                    if (u.getType().getExpertProduction() != u.getWorkType()) {
                        bestPick = u;
                        break;
                    }
                }
                GoodsType rawMaterial = bestPick.getWorkType().getRawMaterial();
                ColonyTile ct = (rawMaterial == null) ? null
                    : getBestVacantTile(connection, bestPick, rawMaterial);
                if (ct != null) {
                    bestPick.work(ct);
                    bestPick.setWorkType(rawMaterial);
                } else {
                    Building th = colony.getBuildingForProducing(Goods.BELLS);
                    if (th.canAdd(bestPick)) {
                        bestPick.work(th);
                    } else {
                        ct = getBestVacantTile(connection, bestPick, Goods.FOOD);
                        if (ct != null) {
                            bestPick.work(ct);
                            bestPick.setWorkType(Goods.FOOD);
                        } else {
                            bestPick.putOutsideColony();
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
            if (Goods.FOOD != goodsType
                && goodsType.isStorable()
                && production + in_stock > colony.getWarehouseCapacity()) {
                Iterator<Unit> unitIterator = colony.getUnitIterator();
                int waste = production + in_stock - colony.getWarehouseCapacity();
                while (unitIterator.hasNext() && waste > 0){
                    Unit unit = unitIterator.next();
                    if (unit.getWorkType() == goodsType) {
                        final Location oldLocation = unit.getLocation();
                        unit.putOutsideColony();
                        boolean working = false;
                        waste = colony.getGoodsCount(goodsType) + colony.getProductionNetOf(goodsType)
                            - colony.getWarehouseCapacity();
                        int best = 0;
                        for (GoodsType goodsType2 : goodsList) {
                            if (!goodsType2.isFarmed())
                                continue;
                            ColonyTile bestTile = getBestVacantTile(connection, unit, goodsType2);
                            int production2 = (bestTile == null ? 0 :
                                               bestTile.getProductionOf(unit, goodsType2));
                            if (production2 > best && production2 + colony.getGoodsCount(goodsType2)
                                + colony.getProductionNetOf(goodsType2) < colony.getWarehouseCapacity()){
                                if (working){
                                    unit.putOutsideColony();
                                }
                                unit.work(bestTile);
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
                            //Assuming that unit already has the correct UnitState here.
                            //If not, this will be fixed by setLocation(),
                            //resulting in a logger warning.
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
                    
                    bestUnit.work(wlp.getWorkLocation());
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
            u.putOutsideColony();
            AIUnit au = (AIUnit) getAIMain().getAIObject(u);
            if (au.getMission() instanceof WorkInsideColonyMission) {
                au.setMission(null);
            }
        }

        // FIXME: should be executed just once, when the custom house is built
        /*if (colony.hasAbility("model.ability.export")) {
          colony.setExports(Goods.SILVER, true);
          colony.setExports(Goods.RUM, true);
          colony.setExports(Goods.CIGARS, true);
          colony.setExports(Goods.CLOTH, true);
          colony.setExports(Goods.COATS, true);
          }*/
        
        decideBuildable(connection);
        createTileImprovementPlans();
        createWishes();
        colonyPlan.adjustProductionAndManufacture();
        checkConditionsForHorseBreed();
        
        if (this.colony.getUnitCount()<=0) {
            // something bad happened, there is no remaining unit working in the colony
            throw new IllegalStateException("Colony " + colony.getName() + " contains no units!");
        }
    }

    /**
     * Verifies if the <code>Colony</code> has conditions for breeding horses,
     *and un-mounts a mounted <code>Unit</code> if available, to have horses to breed.
     */
    void checkConditionsForHorseBreed() {
        GoodsType horsesType = FreeCol.getSpecification().getGoodsType("model.goods.horses");
        EquipmentType horsesEqType = FreeCol.getSpecification().getEquipmentType("model.equipment.horses");
        GoodsType reqGoodsType = horsesType.getRawMaterial();
        
        // Colony already is breeding horses
        if(colony.getGoodsCount(horsesType) >= horsesType.getBreedingNumber()){
            return;
        }
        
        int foodProdAvail = colony.getProductionOf(reqGoodsType) - colony.getFoodConsumptionByType(reqGoodsType);
        // no food production available for breeding anyway
        if(foodProdAvail <= 0){
            return;
        }
        
        // we will now look for any mounted unit that can be temporarily dismounted  
        for(Unit u : colony.getTile().getUnitList()){
            if(!u.isMounted()){
                continue;
            }
            u.removeEquipment(horsesEqType);
            return;
        }
    }

    private void placeExpertsInWorkPlaces(List<Unit> units, List<WorkLocationPlan> workLocationPlans) {
        boolean canProduceInWater = colony.hasAbility("model.ability.produceInWater");
        
        // Since we will change the original list, we need to make a copy to iterate from
        Iterator<Unit> uit = new ArrayList<Unit>(units).iterator();
        while (uit.hasNext()) {
            Unit unit = uit.next();
            
            GoodsType expertProd = unit.getType().getExpertProduction();
            
            // not an expert
            if(expertProd == null){
                continue;
            }
            
            WorkLocationPlan bestWorkPlan = null;
            int bestProduction = 0;
                        
            Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
            while (wlpIterator.hasNext()) {
                WorkLocationPlan wlp = wlpIterator.next();
                WorkLocation wl = wlp.getWorkLocation();
                
                GoodsType locGoods = wlp.getGoodsType();
                
                boolean isColonyTile = wl instanceof ColonyTile;
                boolean isLand = true;
                if(isColonyTile){
                    isLand = ((ColonyTile) wl).getWorkTile().isLand();
                }
                
                //Colony cannot get fish yet
                if(isColonyTile && !isLand && !canProduceInWater){
                    continue;
                }
                
                // not a fit
                if(expertProd != locGoods){
                    continue;
                }
                
                // no need to look any further, only one place to work in
                if(!isColonyTile){
                    bestWorkPlan = wlp;
                    break;
                }
                
                int planProd = wlp.getProductionOf(expertProd);
                if(bestWorkPlan == null || bestProduction < planProd){
                    bestWorkPlan = wlp;
                    bestProduction = planProd;
                    
                }
            }

            if(bestWorkPlan != null){
                //use work() instead of setLocation()
                //to make sure that unitState is properly updated!
                unit.work(bestWorkPlan.getWorkLocation());
                unit.setWorkType(bestWorkPlan.getGoodsType());
                workLocationPlans.remove(bestWorkPlan);
                units.remove(unit);
            }
        }
    }

    /**
     * Decides what to build in the <code>Colony</code>.
     * 
     * @param connection The connection to use when communicating with the
     *            server.
     */
    private void decideBuildable(Connection connection) {
        Iterator<BuildableType> bi = colonyPlan.getBuildable();
        BuildableType buildable = (bi.hasNext()) ? bi.next() : null;
        if (buildable != null && colony.canBuild(buildable)
            && buildable != colony.getCurrentlyBuilding()) {
            Element element = Message.createNewRootElement("setBuildQueue");
            element.setAttribute("colony", colony.getId());
            element.setAttribute("size", "1");
            element.setAttribute("x0", buildable.getId());
            try {
                connection.sendAndWait(element);
            } catch (IOException e) {
                logger.warning("Could not send \"setBuildQueue\"-message.");
            }
            logger.fine("Colony " + colony.getId()
                        + " will build " + buildable.getId());
        }
    }

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

        Iterator<TileImprovementPlan> TileImprovementPlanIterator = tileImprovementPlans.iterator();
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
                tileImprovementPlans.add(ti);
                in.nextTag();
            } else {
                logger.warning("Unknown tag name: " + in.getLocalName());
            }
        }

        if (!in.getLocalName().equals(getXMLElementTagName())) {
            logger.warning("Expected end tag, received: " + in.getLocalName());
        }
    }
    
    public ColonyPlan getColonyPlan() {
        return colonyPlan;
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
