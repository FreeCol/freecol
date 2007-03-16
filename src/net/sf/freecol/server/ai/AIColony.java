
package net.sf.freecol.server.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
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

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
    * The FreeColGameObject this AIObject contains AI-information for.
    */
    private Colony colony;

    private ColonyPlan colonyPlan;
    private ArrayList<AIGoods> aiGoods = new ArrayList<AIGoods>();
    private ArrayList<Wish> wishes = new ArrayList<Wish>();
    private ArrayList<TileImprovement> tileImprovements = new ArrayList<TileImprovement>();


    /**
     * Creates a new <code>AIColony</code>.
     * 
     * @param aiMain The main AI-object.
     * @param colony The colony to make an {@link AIObject} for.
     */
    public AIColony(AIMain aiMain, Colony colony) {
        super(aiMain, colony.getID());

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
        super(aiMain, element.getAttribute("ID"));
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>AIColony</code>.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
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
        Iterator it2 = ((ArrayList) ((ArrayList) wishes).clone()).iterator();
        while (it2.hasNext()) {
            ((Wish) it2.next()).dispose();
        }   
        Iterator it3 = ((ArrayList) ((ArrayList) tileImprovements).clone()).iterator();
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
    public Iterator<Wish> getWishIterator() {
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
            
        Collections.sort(tileImprovements, new Comparator<TileImprovement>() {
            public int compare(TileImprovement o,TileImprovement p) {
                Integer i = o.getValue();
                Integer j = p.getValue();
                
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
    public Iterator<TileImprovement> getTileImprovementIterator() {
        return tileImprovements.iterator();
    }
    
    
    /**
    * Creates the wishes for the <code>Colony</code>.
    */
    private void createWishes() {
        List<WorkLocationPlan> workLocationPlans = colonyPlan.getSortedWorkLocationPlans();

        int[] production = new int[Goods.NUMBER_OF_TYPES];
        ArrayList<Unit> nonExpertUnits = new ArrayList<Unit>();
        Iterator<Unit> unitIterator = colony.getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit u = unitIterator.next();
            int workType = u.getExpertWorkType();
            if (workType >= 0) {
                Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
                while (wlpIterator.hasNext()) {
                    WorkLocationPlan wlp = wlpIterator.next();
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

        List<Wish> newWishes = new ArrayList<Wish>();
        int value = 120;    // TODO: Better method for determining the value of the wish.
        while (workLocationPlans.size() > 0) {
            int unitType = -1;
            
            // Farmer/fisherman wishes:
            if (production[Goods.FOOD] < 2) {
                Iterator wlpIterator = workLocationPlans.iterator();
                while (wlpIterator.hasNext()) {
                    WorkLocationPlan wlp = (WorkLocationPlan) wlpIterator.next();
                    if (wlp.getGoodsType() == Goods.FOOD) {
                        production[Goods.FOOD] += wlp.getProductionOf(Goods.FOOD);
                        production[Goods.FOOD] -= 2;
                        wlpIterator.remove();

                        unitType = ((ColonyTile) wlp.getWorkLocation()).getWorkTile().isLand()
                                        ? Unit.EXPERT_FARMER
                                        : Unit.EXPERT_FISHERMAN;
                        break;
                    }
                }
            }
            if (unitType == -1) {
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
                        
                        if (wlp.getWorkLocation() instanceof ColonyTile) {
                            unitType = ((ColonyTile) wlp.getWorkLocation()).getExpertForProducing(wlp.getGoodsType());
                        } else {
                            unitType = ((Building) wlp.getWorkLocation()).getExpertUnitType();
                        }
                        break;
                    }
                }
            }
            if (unitType >= 0) {
                boolean expert = (nonExpertUnits.size() <= 0);
                
                boolean wishFound = false;
                Iterator wishIterator = wishes.iterator();
                while (wishIterator.hasNext()) {
                    Wish w = (Wish) wishIterator.next();
                    if (w instanceof WorkerWish) {
                        WorkerWish ww = (WorkerWish) w;
                        if (ww.getUnitType() == unitType
                                && !newWishes.contains(ww)) {
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
        final boolean needsPioneer = (tileImprovements.size() > 0 
                || unequippedHardyPioneer != null  && PioneeringMission.isValid(unequippedHardyPioneer));
        int toolsRequiredForBuilding = 0;
        if (colony.getCurrentlyBuilding() >= 0) {
            toolsRequiredForBuilding = (colony.getCurrentlyBuilding() >= Colony.BUILDING_UNIT_ADDITION) 
                    ? Unit.getNextTools(colony.getCurrentlyBuilding()-Colony.BUILDING_UNIT_ADDITION)
                    : colony.getBuilding(colony.getCurrentlyBuilding()).getNextTools();
        }
        if (colony.getProductionNetOf(Goods.TOOLS) == 0
                && (colony.getGoodsCount(Goods.TOOLS) < 20 && needsPioneer)
                    || toolsRequiredForBuilding > colony.getGoodsCount(Goods.TOOLS)) {
            int goodsWishValue = AIGoods.TOOLS_FOR_COLONY_PRIORITY 
                    + AIGoods.TOOLS_FOR_IMPROVEMENT * tileImprovements.size()
                    + ((unequippedHardyPioneer != null) ? AIGoods.TOOLS_FOR_PIONEER : 0)
                    + ((toolsRequiredForBuilding > colony.getGoodsCount(Goods.TOOLS)) 
                        ? AIGoods.TOOLS_FOR_BUILDING + (toolsRequiredForBuilding - colony.getGoodsCount(Goods.TOOLS)): 0);
            boolean goodsOrdered = false;
            Iterator wishIterator = wishes.iterator();
            while (wishIterator.hasNext()) {
                Wish w = (Wish) wishIterator.next();
                if (w instanceof GoodsWish) {
                    GoodsWish gw = (GoodsWish) w;
                    //TODO: check for a certain required amount?
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
            Iterator wishIterator = ((List) ((ArrayList) wishes).clone()).iterator();
            while (wishIterator.hasNext()) {
                Wish w = (Wish) wishIterator.next();
                if (w instanceof GoodsWish) {
                    GoodsWish gw = (GoodsWish) w;
                    //TODO: check for a certain required amount?
                    if (gw.getGoodsType() == Goods.TOOLS) {
                        gw.dispose();                      
                    }
                }
            }
        }
        
        Iterator wishIterator = ((ArrayList) ((ArrayList) wishes).clone()).iterator();
        while (wishIterator.hasNext()) {
            Wish w = (Wish) wishIterator.next();
            if (w instanceof WorkerWish) {
                if (!newWishes.contains(w)) {
                    w.dispose();
                }
            } else if (w instanceof GoodsWish) {
                GoodsWish gw = (GoodsWish) w;
                //TODO: check for a certain required amount?
                if (getColony().getGoodsCount(gw.getGoodsType()) >= 20) {
                    gw.dispose();
                }
            } else {
                logger.warning("Unknown type of Wish.");
            }
        }
        
        Collections.sort(wishes, new Comparator<Wish>() {
            public int compare(Wish o, Wish p) {
                Integer i = o.getValue();
                Integer j = p.getValue();
                return j.compareTo(i);
            }
        });

    }
    
    /**
     * Returns an unequipped pioneer that is either
     * inside this colony or standing on the same <code>Tile</code>.
     * 
     * @return A unit with a {@link PioneeringMission} or a unit being
     *      a {@link Unit#HARDY_PIONEER hardy pioneer} - and with
     *      no tools. Returns <code>null</code> if no such unit was found. 
     */
    private AIUnit getUnequippedHardyPioneer() {
        Iterator ui = colony.getTile().getUnitIterator();
        while (ui.hasNext()) {
            Unit u = (Unit) ui.next();
            AIUnit au = (AIUnit) getAIMain().getAIObject(u);
            if (au.getMission() != null
                    && au.getMission() instanceof PioneeringMission
                    && u.getNumberOfTools() == 0) {
                return au;
            }
        }
        ui = colony.getUnitIterator();
        while (ui.hasNext()) {
            Unit u = (Unit) ui.next();
            AIUnit au = (AIUnit) getAIMain().getAIObject(u);
            if (u.getType() == Unit.HARDY_PIONEER) {
                return au;
            }
        }
        return null;
    }
    
    public void removeWish(Wish w) {
        wishes.remove(w);
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
     * Removes the given <code>AIGoods</code> from this colony's list.
     * The <code>AIGoods</code>-object is not disposed as part of this
     * operation. Use that method instead to remove the object completely
     * (this method would then be called indirectly).
     * 
     * @param ag The <code>AIGoods</code> to be removed.
     * @see AIGoods#dispose()
     */
    public void removeAIGoods(AIGoods ag) {
        Iterator it = aiGoods.iterator();
        while (it.hasNext()) {
                if (it.next() == ag) {
                        it.remove();
                }
        }
    }

    /**
    * Creates a list of the goods which should be shipped out of this colony.
    * This is the list {@link #getAIGoodsIterator} returns the <code>Iterator</code>
    * for.
    */
    private void createExportAIGoodsList() {
        ArrayList<AIGoods> newAIGoods = new ArrayList<AIGoods>();

        // TODO: Do not sell raw material we are lacking.

        for (int goodsType=0; goodsType<Goods.NUMBER_OF_TYPES; goodsType++) {
            // Never export food and lumber
            if (goodsType == Goods.FOOD || goodsType == Goods.LUMBER) {
                continue;
            }
            // Only export muskets if we do not have room for them:
            if (goodsType == Goods.MUSKETS && (colony.getProductionOf(Goods.MUSKETS) == 0
                    || colony.getGoodsCount(Goods.MUSKETS) < colony.getWarehouseCapacity() - colony.getProductionOf(Goods.MUSKETS))) {
                continue;
            }
            // Only export horses if we do not have room for them:
            if (goodsType == Goods.HORSES && (colony.getProductionOf(Goods.HORSES) == 0
                    || colony.getGoodsCount(Goods.HORSES) < colony.getWarehouseCapacity() - colony.getProductionOf(Goods.HORSES))) {
                continue;
            }
            
            /*
             * Only export tools if we are producing it in this colony
             * and have sufficient amounts in warehouse:
             */
            if (goodsType == Goods.TOOLS && colony.getGoodsCount(Goods.TOOLS) > 0) {                
                if (colony.getProductionNetOf(Goods.TOOLS) > 0) {                    
                    int requiredTools = 0;
                    int buildTurns = 0;
                    final int currentlyBuilding = colony.getCurrentlyBuilding();                
                    if (currentlyBuilding >= 0) {
                        if (colony.getCurrentlyBuilding() >= Colony.BUILDING_UNIT_ADDITION) {
                            final int nextHammers = Unit.getNextHammers(colony.getCurrentlyBuilding()-Colony.BUILDING_UNIT_ADDITION);
                            requiredTools += Unit.getNextTools(colony.getCurrentlyBuilding()-Colony.BUILDING_UNIT_ADDITION);
                            buildTurns += (nextHammers - colony.getHammers()) / (colony.getProductionOf(Goods.HAMMERS)+1);
                        } else {
                            Building b = colony.getBuilding(currentlyBuilding);
                            requiredTools += b.getNextTools();
                            buildTurns += (b.getNextHammers() - colony.getHammers()) / (colony.getProductionOf(Goods.HAMMERS)+1);
                        }
                    }
                                        
                    if (requiredTools > 0) {
                        if (colony.getWarehouseCapacity() > 100) {
                            requiredTools += 100;
                        }
                        int toolsProductionTurns = requiredTools / colony.getProductionNetOf(Goods.TOOLS);
                        if (buildTurns <= toolsProductionTurns + 1) {
                            continue;
                        }
                    } else if (colony.getWarehouseCapacity() > 100
                            && colony.getGoodsCount(Goods.TOOLS) <= 100) {
                        continue;
                    }
                } else {
                    continue;
                }
            }
            
            if (colony.getGoodsCount(goodsType) > 0) {
                List<AIGoods> alreadyAdded = new ArrayList<AIGoods>();
                for (int j=0; j<aiGoods.size(); j++) {
                    AIGoods ag = aiGoods.get(j);
                    if (ag == null) {
                        logger.warning("aiGoods == null");
                    } else if (ag.getGoods() == null) {
                        logger.warning("aiGoods.getGoods() == null");
                        if (ag.isUninitialized()) {
                            logger.warning("AIGoods uninitialized: " + ag.getID());
                        }                        
                    }
                    if (ag != null
                            && ag.getGoods() != null
                            && ag.getGoods().getType() == goodsType
                            && ag.getGoods().getLocation() == colony) {
                        alreadyAdded.add(ag);
                    }
                }

                int amountRemaining = colony.getGoodsCount(goodsType);
                for (int i=0; i<alreadyAdded.size(); i++) {
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
            for (i=0; i<aiGoods.size() && aiGoods.get(i).getTransportPriority() > ag.getTransportPriority(); i++);
            aiGoods.add(i, ag);
        }
    }

    /**
     * Returns the available amount of tools.
     * @return The amount of tools not needed for the next
     *     thing we are building.
     */
    public int getAvailableTools() {
        int toolsRequiredForBuilding = 0;
        if (colony.getCurrentlyBuilding() >= 0) {
            toolsRequiredForBuilding = (colony.getCurrentlyBuilding() >= Colony.BUILDING_UNIT_ADDITION) 
                    ? Unit.getNextTools(colony.getCurrentlyBuilding()-Colony.BUILDING_UNIT_ADDITION)
                    : colony.getBuilding(colony.getCurrentlyBuilding()).getNextTools();
        }
        
        return Math.max(0, colony.getGoodsCount(Goods.TOOLS) - toolsRequiredForBuilding);
    }

    /**
    * Rearranges the workers within this colony.
    * This is done according to the {@link ColonyPlan}, although minor
    * adjustments can be done to increase production.
    * 
    * @param connection The <code>Connection</code> to be used when
    *       communicating with the server.
    */
    public void rearrangeWorkers(Connection connection) {
        colonyPlan.create();

        // TODO: Detect a siege and move the workers temporarily around.
        
        // Move a pioneer outside the colony if we have a sufficient amount of tools:
        if (colony.getUnitCount() > 1
                && getAvailableTools() >= 20) {
            AIUnit unequippedPioneer = getUnequippedHardyPioneer();
            if (unequippedPioneer != null
                    && (unequippedPioneer.getMission() == null
                            || !(unequippedPioneer.getMission() instanceof PioneeringMission))
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
                WorkLocationPlan wlp = workLocationPlans.get(i);
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
                    WorkLocationPlan wlp = workLocationPlans.get(i);
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
            WorkLocationPlan wlp = workLocationPlans.get(i);
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

        decideBuildable(connection);
        createTileImprovements();
        createWishes();
    }


    /**
     * Decides what to build in the <code>Colony</code>.
     * @param connection The connection to use when communicating
     *      with the server.
     */
    private void decideBuildable(Connection connection) {
        // TODO: Request tools if needed.
        Iterator bi = colonyPlan.getBuildable();
        while (bi.hasNext()) {
            int buildable = ((Integer) bi.next()).intValue();
            
            if (buildable == colony.getCurrentlyBuilding()) {                
                // We are building the right item already:
                break;
            }
            
            int hammersNew = (buildable >= Colony.BUILDING_UNIT_ADDITION) 
                    ? Unit.getNextHammers(buildable-Colony.BUILDING_UNIT_ADDITION) 
                    : colony.getBuilding(buildable).getNextHammers();
            int hammersOld = 0;
            if (colony.getCurrentlyBuilding() >= Colony.BUILDING_UNIT_ADDITION) {
                hammersOld = Unit.getNextHammers(colony.getCurrentlyBuilding() - Colony.BUILDING_UNIT_ADDITION);
            } else if (colony.getCurrentlyBuilding() > -1) {
                hammersOld = colony.getBuilding(colony.getCurrentlyBuilding()).getNextHammers();
            }
                    
            boolean isOldValid = true;
            if (colony.getCurrentlyBuilding() < 0) {
                isOldValid = false;
            } else if (colony.getCurrentlyBuilding() < Colony.BUILDING_UNIT_ADDITION) {
                isOldValid = colony.getBuilding(colony.getCurrentlyBuilding()).canBuildNext();
            }

            if (hammersNew > colony.getHammers()
                    || hammersNew > hammersOld
                    || !isOldValid) {
                Element setCurrentlyBuildingElement = Message.createNewRootElement("setCurrentlyBuilding");
                setCurrentlyBuildingElement.setAttribute("colony", colony.getID());
                setCurrentlyBuildingElement.setAttribute("type", Integer.toString(buildable));
                
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
    */


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
     * Writes this object to an XML stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getID());

        Iterator aiGoodsIterator = aiGoods.iterator();
        while (aiGoodsIterator.hasNext()) {
            AIGoods ag = (AIGoods) aiGoodsIterator.next();
            if (ag == null) {
                logger.warning("ag == null");
                continue;
            }
            if (ag.getID() == null) {
                logger.warning("ag.getID() == null");
                continue;
            }
            out.writeStartElement(AIGoods.getXMLElementTagName() + "ListElement");
            out.writeAttribute("ID", ag.getID());
            out.writeEndElement();
        }

        Iterator wishesIterator = wishes.iterator();
        while (wishesIterator.hasNext()) {
            Wish w = (Wish) wishesIterator.next();
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
            out.writeAttribute("ID", w.getID());
            out.writeEndElement();
        }
        
        Iterator tileImprovementIterator = tileImprovements.iterator();
        while (tileImprovementIterator.hasNext()) {
            TileImprovement ti = (TileImprovement) tileImprovementIterator.next();
            out.writeStartElement(TileImprovement.getXMLElementTagName() + "ListElement");
            out.writeAttribute("ID", ti.getID());
            out.writeEndElement();
        }        

        out.writeEndElement();
    }

    /**
     * Reads information for this object from an XML stream.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading
     *      from the stream.
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
            } else if (in.getLocalName().equals(TileImprovement.getXMLElementTagName() + "ListElement")) {
                TileImprovement ti = (TileImprovement) getAIMain().getAIObject(in.getAttributeValue(null, "ID"));
                if (ti == null) {
                    ti = new TileImprovement(getAIMain(), in.getAttributeValue(null, "ID"));
                }
                tileImprovements.add(ti);
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
    * @return "aiColony"
    */
    public static String getXMLElementTagName() {
        return "aiColony";
    }
}
