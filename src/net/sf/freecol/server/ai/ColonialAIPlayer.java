/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

/* "COLONIAL" AIPLAYER *********************************************************
 *
 * This class is specialized to control the "Colonial" player type,
 * using it for any other type will fail.
 * 
 * It is currently not used in any standard game setting,
 * but can be activated by starting a game using the "--experimentalAI" arg. 
 * 
 ******************************************************************************/
   
package net.sf.freecol.server.ai;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.ColonyTradeItem;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.GoldTradeItem;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsTradeItem;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StanceTradeItem;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitTradeItem;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.server.ai.goal.ManageMissionariesGoal;
import net.sf.freecol.server.ai.mission.BuildColonyMission;
import net.sf.freecol.server.ai.mission.CashInTreasureTrainMission;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.IdleAtColonyMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.PioneeringMission;
import net.sf.freecol.server.ai.mission.PrivateerMission;
import net.sf.freecol.server.ai.mission.ScoutingMission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.ai.mission.UnitWanderMission;
import net.sf.freecol.server.ai.mission.WishRealizationMission;
import net.sf.freecol.server.ai.mission.WorkInsideColonyMission;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;

/**
 * 
 * Objects of this class contains AI-information for a single {@link Player} and
 * is used for controlling this player.
 * 
 * <br />
 * <br />
 * 
 * The method {@link #startWorking} gets called by the
 * {@link AIInGameInputHandler} when it is this player's turn.
 */
public class ColonialAIPlayer extends AIPlayer {
    private static final Logger logger = Logger.getLogger(ColonialAIPlayer.class.getName());

    private static enum AIStrategy { NONE, TRADE, IMMIGRATION, COOPERATION, CONQUEST }

    /** The strategy of this player. */
    private AIStrategy strategy = AIStrategy.NONE;

    /** Used in temporary override of AIUnit iterator methods */
    private ArrayList<AIUnit> myAIUnits = new ArrayList<AIUnit>();

    /**
     * Goal to manage missionary units of this player.
     */         
    private ManageMissionariesGoal mGoal = new ManageMissionariesGoal((AIPlayer)this, null, 1.0f);

    /**
     * Creates a new <code>AIPlayer</code>.
     * 
     * @param aiMain The main AI-class.
     * @param player The player that should be associated with this
     *            <code>AIPlayer</code>.
     */
    public ColonialAIPlayer(AIMain aiMain, ServerPlayer player) {
        super(aiMain, player.getId());
        setPlayer(player);
    }

    /**
     * Creates a new <code>AIPlayer</code> and reads the information from the
     * given <code>Element</code>.
     * 
     * @param aiMain The main AI-class.
     * @param element The XML-element containing information.
     */
    public ColonialAIPlayer(AIMain aiMain, Element element) {
        super(aiMain, element.getAttribute("ID"));
        readFromXMLElement(element);
        //TODO: setPlayer()?
    }

    /**
     * Creates a new <code>AIPlayer</code>.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public ColonialAIPlayer(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain, in.getAttributeValue(null, "ID"));
        readFromXML(in);
        //TODO: setPlayer()?
    }


/* IMPLEMENTATION (AIPlayer interface) ****************************************/


    /**
     * Tells this <code>AIPlayer</code> to make decisions. The
     * <code>AIPlayer</code> is done doing work this turn when this method
     * returns.
     */
    public void startWorking() {
        logger.fine("Entering AI code for: " + getPlayer().toString()
                    + ", year " + getGame().getTurn());

        //this AI assumes that player is _not_ REF, _not_ Indian, but European!
        if (getPlayer().isREF() ||
            getPlayer().isIndian() ||
            !getPlayer().isEuropean()) {
            throw new IllegalStateException("Player type not supported by this AI!");
        }
        
        // TODO: find some intelligent solution to set a proper strategy!
        this.strategy = AIStrategy.TRADE;

        //reset unit iterator to all units without current goal         
        clearAIUnits();

        //add goals for missionaries (this will remove them from the iterator)
        manageMissionaries();
        mGoal.setNeedsPlanningRecursive(true);

        boolean furtherPlanning = true;
        while (furtherPlanning) {
            mGoal.doPlanning();
            furtherPlanning = mGoal.needsPlanning();            
        }
        
        //manage the remaining units as before
        cheat();
        determineStances();
        rearrangeWorkersInColonies();
        abortInvalidAndOneTimeMissions();
        ensureCorrectMissions();
        giveNavalMissions();
        giveNormalMissions();
        createAIGoodsInColonies();
        createTransportLists();
        doMissions();
        rearrangeWorkersInColonies();
        abortInvalidMissions();
        // Some of the mission might have been invalidated by a another mission.
        giveNormalMissions();
        doMissions();
        rearrangeWorkersInColonies();
        abortInvalidMissions();
        ensureCorrectMissions();
        
        clearAIUnits();
    }

    /**
     * Returns an <code>Iterator</code> over all the
     * <code>TileImprovement</code>s needed by all of this player's colonies.
     * 
     * @return The <code>Iterator</code>.
     * @see TileImprovement
     */
    public Iterator<TileImprovementPlan> getTileImprovementPlanIterator() {
        ArrayList<TileImprovementPlan> tileImprovements = new ArrayList<TileImprovementPlan>();
        Iterator<AIColony> acIterator = getAIColonyIterator();
        while (acIterator.hasNext()) {
            AIColony ac = acIterator.next();
            Iterator<TileImprovementPlan> it = ac.getTileImprovementPlanIterator();
            while (it.hasNext()) {
                tileImprovements.add(it.next());
            }
        }
        return tileImprovements.iterator();
    }
    
    /**
     * Remove a <code>TileImprovementPlan</code> from the list
     */
    public void removeTileImprovementPlan(TileImprovementPlan plan){
    	Iterator<AIColony> colonyIter = getAIColonyIterator();
        while (colonyIter.hasNext()) {
            AIColony colony = colonyIter.next();
            if(colony.removeTileImprovementPlan(plan)){
            	return;
            }
        }
        logger.warning("Not found given TileImprovementPlan to remove");
    }

    /**
     * This is a temporary method which are used for forcing the computer
     * players into building more colonies. The method will be removed after the
     * proper code for deciding whether a colony should be built or not has been
     * implemented.
     * 
     * @return <code>true</code> if the AI should build more colonies.
     */
    public boolean hasFewColonies() {        
        if (!getPlayer().canBuildColonies()) {
            return false;
        }
        int numberOfColonies = 0;
        int numberOfWorkers = 0;
        for (Colony colony : getPlayer().getColonies()) {
            numberOfColonies++;
            numberOfWorkers += colony.getUnitCount();
        }
        
        boolean result = numberOfColonies <= 2
            || (numberOfColonies >= 3
                && numberOfWorkers / numberOfColonies > numberOfColonies - 2);
        logger.finest("hasFewColonies = " + result);
        return result;
    }

    /**
     * Returns an <code>Iterator</code> for all the wishes. The items are
     * sorted by the {@link Wish#getValue value}, with the item having the
     * highest value appearing first in the <code>Iterator</code>.
     * 
     * @return The <code>Iterator</code>.
     * @see Wish
     */
    public Iterator<Wish> getWishIterator() {
        ArrayList<Wish> wishList = new ArrayList<Wish>();
        Iterator<AIColony> ai = getAIColonyIterator();
        while (ai.hasNext()) {
            AIColony ac = ai.next();
            Iterator<Wish> wishIterator = ac.getWishIterator();
            while (wishIterator.hasNext()) {
                Wish w = wishIterator.next();
                wishList.add(w);
            }
        }
        Collections.sort(wishList);
        return wishList.iterator();
    }

    /**
     * Selects the most useful founding father offered.
     * 
     * @param foundingFathers The founding fathers on offer.
     * @return The founding father selected.
     */
    public FoundingFather selectFoundingFather(List<FoundingFather> foundingFathers) {
        //TODO: improve choice
        int age = getGame().getTurn().getAge();
        FoundingFather bestFather = null;
        int bestWeight = -1;
        for (FoundingFather father : foundingFathers) {
            if (father == null) continue;
            int weight = father.getWeight(age);
            if (weight > bestWeight) {
                bestWeight = weight;
                bestFather = father;
            }
        }
        return bestFather;
    }

    /**
     * Decides whether to accept the monarch's tax raise or not.
     * 
     * @param tax The new tax rate to be considered.
     * @return <code>true</code> if the tax raise should be accepted.
     */
    public boolean acceptTax(int tax) {
        Goods toBeDestroyed = getPlayer().getMostValuableGoods();
        if (toBeDestroyed == null) {
            return false;
        }
        
        GoodsType goodsType = toBeDestroyed.getType();
        if (goodsType.isFoodType() || goodsType.isBreedable()) {
            // we should be able to produce food and horses ourselves
            return false;
        } else if (goodsType.isMilitaryGoods() || 
                   goodsType.isTradeGoods() ||
                   goodsType.isBuildingMaterial()) {
            if (getGame().getTurn().getAge() == 3) {
                // by this time, we should be able to produce
                // enough ourselves
                return false;
            } else {
                return true;
            }
        } else {
            int averageIncome = 0;
            int numberOfGoods = 0;
            List<GoodsType> goodsTypes = getGame().getSpecification().getGoodsTypeList();
            for (GoodsType type : goodsTypes) {
                if (type.isStorable()) {
                    averageIncome += getPlayer().getIncomeAfterTaxes(type);
                    numberOfGoods++;
                }
            }
            averageIncome = averageIncome / numberOfGoods;
            if (getPlayer().getIncomeAfterTaxes(toBeDestroyed.getType()) > averageIncome) {
                // this is a more valuable type of goods
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Decides whether to accept an Indian demand, or not.
     * 
     * @param unit The unit making demands.
     * @param colony The colony where demands are being made.
     * @param goods The goods demanded.
     * @param gold The amount of gold demanded.
     * @return <code>true</code> if this <code>AIPlayer</code> accepts the
     *         indian demand and <code>false</code> otherwise.
     */
    public boolean acceptIndianDemand(Unit unit, Colony colony, Goods goods, int gold) {
        // TODO: make a better choice
        if (strategy == AIStrategy.CONQUEST) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Decides whether to accept a mercenary offer, or not.
     * 
     * @return <code>true</code> if this <code>AIPlayer</code> accepts the
     *         offer and <code>false</code> otherwise.
     */
    public boolean acceptMercenaryOffer() {
        // TODO: make a better choice
        if (strategy == AIStrategy.CONQUEST || getPlayer().isAtWar()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean acceptDiplomaticTrade(DiplomaticTrade agreement) {
        Stance stance = null;
        int value = 0;
        Iterator<TradeItem> itemIterator = agreement.iterator();
        while (itemIterator.hasNext()) {
            TradeItem item = itemIterator.next();
            if (item instanceof GoldTradeItem) {
                int gold = ((GoldTradeItem) item).getGold();
                if (item.getSource() == getPlayer()) {
                    value -= gold;
                } else {
                    value += gold;
                }
            } else if (item instanceof StanceTradeItem) {
                // TODO: evaluate whether we want this stance change
                stance = ((StanceTradeItem) item).getStance();
            } else if (item instanceof ColonyTradeItem) {
                // TODO: evaluate whether we might wish to give up a colony
                if (item.getSource() == getPlayer()) {
                    value = Integer.MIN_VALUE;
                    break;
                } else {
                    value += 1000;
                }
            } else if (item instanceof UnitTradeItem) {
                // TODO: evaluate whether we might wish to give up a unit
                if (item.getSource() == getPlayer()) {
                    value = Integer.MIN_VALUE;
                    break;
                } else {
                    value += 100;
                }
            } else if (item instanceof GoodsTradeItem) {
                Goods goods = ((GoodsTradeItem) item).getGoods();
                if (item.getSource() == getPlayer()) {
                    value -= getPlayer().getMarket().getBidPrice(goods.getType(), goods.getAmount());
                } else {
                    value += getPlayer().getMarket().getSalePrice(goods.getType(), goods.getAmount());
                }
            }
        }

        boolean accept = false;
        switch (stance) {
        case UNCONTACTED:
            accept = false;
            break;
        case WAR: // always accept war without cost
            accept = value >= 0;
            break;
        case CEASE_FIRE:
            accept = value >= 500;
            break;
        case PEACE:
            if (agreement.getSender().hasAbility("model.ability.alwaysOfferedPeace")
                && value >= 0) {
                // TODO: introduce some kind of counter in order to avoid
                // Benjamin Franklin exploit
                accept = true;
            } else if (value >= 1000) {
                accept = true;
            }
            break;
        case ALLIANCE:
            accept = value >= 2000;
            break;
        }

        logger.info("Trade value is " + value + ", accept is " + accept);
        return accept;
    }

    
    /**
     * Called after another <code>Player</code> sends a <code>trade</code> message
     * 
     * @param goods The goods which we are going to offer
     */
    public void registerSellGoods(Goods goods) {
        //method currently only called for owners of indian settlements
        logger.warning("registerSellGoods() not implemented for ColonialAIPlayer!");
    }

    /**
     * Called when another <code>Player</code> proposes a trade.
     * 
     * 
     * @param unit The foreign <code>Unit</code> trying to trade.
     * @param settlement The <code>Settlement</code> this player owns and
     *            which the given <code>Unit</code> is trying to trade.
     * @param goods The goods the given <code>Unit</code> is trying to sell.
     * @param gold The suggested price.
     * @return The price this <code>AIPlayer</code> suggests or
     *         {@link NetworkConstants#NO_TRADE}.
     */
    public int buyProposition(Unit unit, Settlement settlement, Goods goods, int gold) {
        //method currently only called for owners of indian settlements
        logger.warning("buyProposition() not implemented for ColonialAIPlayer!");
        return NetworkConstants.NO_TRADE;
    }

    /**
     * Called when another <code>Player</code> proposes a sale.
     * 
     * @param unit The foreign <code>Unit</code> trying to trade.
     * @param settlement The <code>Settlement</code> this player owns and
     *            which the given <code>Unit</code> if trying to sell goods.
     * @param goods The goods the given <code>Unit</code> is trying to sell.
     * @param gold The suggested price.
     * @return The price this <code>AIPlayer</code> suggests or
     *         {@link NetworkConstants#NO_TRADE}.
     */
    public int sellProposition(Unit unit, Settlement settlement, Goods goods, int gold) {
        logger.finest("Entering method sellProposition");
        if (settlement instanceof Colony) {
            Colony colony = (Colony) settlement;
            Player otherPlayer = unit.getOwner();
            // the client should have prevented this
            if (getPlayer().atWarWith(otherPlayer)) {
                return NetworkConstants.NO_TRADE;
            }
            // don't pay for more than fits in the warehouse
            int amount = colony.getWarehouseCapacity() - colony.getGoodsCount(goods.getType());
            amount = Math.min(amount, goods.getAmount());
            // get a good price
            Tension.Level tensionLevel = getPlayer().getTension(otherPlayer).getLevel();
            int percentage = (9 - tensionLevel.ordinal()) * 10;
            // what we could get for the goods in Europe (minus taxes)
            int netProfits = ((100 - getPlayer().getTax()) * getPlayer().getMarket().getSalePrice(goods.getType(), amount)) / 100;
            int price = (netProfits * percentage) / 100;
            return price;
        } else {
            logger.warning("ColonialAIPlayer shouldn't have indian settlement!");
            return NetworkConstants.NO_TRADE;
        }
    }

    /**
     * Writes this object to an XML stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute("ID", getId());
        out.writeEndElement();
    }

    /**
     * Reads information for this object from an XML stream.
     * 
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading from the
     *             stream.
     */
    @Override
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setPlayer((ServerPlayer) getAIMain().getFreeColGameObject(in.getAttributeValue(null, "ID")));
        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "colonialAIPlayer";
    }


/* Internal methods ***********************************************************/


    /**
     * Cheats for the AI :-)
     */
    private void cheat() {
        logger.finest("Entering method cheat");
        // TODO-AI-CHEATING: REMOVE WHEN THE AI IS GOOD ENOUGH:
        for (GoodsType goodsType : getGame().getSpecification().getGoodsTypeList()) {
            getPlayer().getMarket().setArrears(goodsType, 0);
        }
        
        //TODO: This seems to buy units the AIPlayer can't possibly use (see BR#2566180)
        if (getAIMain().getFreeColServer().isSingleplayer() && getPlayer().isAI()
                && getPlayer().getPlayerType() == PlayerType.COLONIAL) {
            Europe europe = getPlayer().getEurope();
            List<UnitType> unitTypes = getGame().getSpecification().getUnitTypeList();
            
            if (getAIRandom().nextInt(10) == 1) {
                int price = 0;
                UnitType unitToTrain = null;
                for (UnitType unitType : unitTypes) {
                    if (unitType.hasPrice()) {
                        int unitPrice = europe.getUnitPrice(unitType);
                        if (unitToTrain == null || unitPrice < price) {
                            unitToTrain = unitType;
                            price = unitPrice;
                        }
                    }
                }
                Unit unit = null;
                if (unitToTrain != null) {
                    getPlayer().modifyGold(price);
                    unit = this.trainAIUnitInEurope(unitToTrain).getUnit();
                }
                if (unit != null && unit.isColonist()) {
                    // no need to equip artillery units with muskets or horses
                    Specification spec = getGame().getSpecification();
                    GoodsType muskets = spec.getGoodsType("model.goods.muskets");
                    GoodsType horses = spec.getGoodsType("model.goods.horses");
                    getPlayer().modifyGold(getPlayer().getMarket().getBidPrice(muskets, 50));
                    getPlayer().modifyGold(getPlayer().getMarket().getBidPrice(horses, 50));
                    
                    AIMessage.askClearSpeciality(getAIUnit(unit));
                    AIMessage.askEquipUnit(getAIUnit(unit), spec.getEquipmentType("model.equipment.horses"), 1);
                    AIMessage.askEquipUnit(getAIUnit(unit), spec.getEquipmentType("model.equipment.muskets"), 1);
                }
            }
            if (getAIRandom().nextInt(40) == 21) {
                int total = 0;
                ArrayList<UnitType> navalUnits = new ArrayList<UnitType>();
                for (UnitType unitType : unitTypes) {
                    if (unitType.hasAbility("model.ability.navalUnit") && unitType.hasPrice()) {
                        navalUnits.add(unitType);
                        total += europe.getUnitPrice(unitType);
                    }
                }
                
                UnitType unitToPurchase = null;
                int random = getAIRandom().nextInt(total);
                total = 0;
                for (UnitType unitType : navalUnits) {
                    total += unitType.getPrice();
                    if (random < total) {
                        unitToPurchase = unitType;
                        break;
                    }
                }
                getPlayer().modifyGold(europe.getUnitPrice(unitToPurchase));
                this.trainAIUnitInEurope(unitToPurchase);
            }
        }
    }

    /**
     * Ensures that all workers inside a colony gets a
     * {@link WorkInsideColonyMission}.
     */
    private void ensureCorrectMissions() {
        logger.finest("Entering method ensureCorrectMissions");
        Iterator<AIUnit> it = getAIUnitIterator();
        while (it.hasNext()) {
            AIUnit au = it.next();
            if (!au.hasMission()
                    && (au.getUnit().getLocation() instanceof ColonyTile || au.getUnit().getLocation() instanceof Building)) {
                AIColony ac = (AIColony) getAIMain().getAIObject(au.getUnit().getColony());
                au.setMission(new WorkInsideColonyMission(getAIMain(), au, ac));
            }
        }
    }

    /**
     * Determines the stances towards each player.
     * 
     * That is: should we declare war?
     */
    private void determineStances() {
        logger.finest("Entering method determineStances");
        Player player = getPlayer();
        for (Player p : getGame().getPlayers()) {
            if (p != player) determineStance(p);
        }
    }

    /**
     * Aborts all the missions which are no longer valid.
     */
    private void abortInvalidMissions() {
        logger.finest("Entering method abortInvalidMissions");
        Iterator<AIUnit> aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = aiUnitsIterator.next();
            if (aiUnit.getMission() == null) {
                continue;
            }
            if (!aiUnit.getMission().isValid()) {
                aiUnit.setMission(null);
            }
        }
    }

    /**
     * Aborts all the missions which are no longer valid.
     */
    private void abortInvalidAndOneTimeMissions() {
        logger.finest("Entering method abortInvalidAndOneTimeMissions");
        Iterator<AIUnit> aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = aiUnitsIterator.next();
            if (aiUnit.getMission() == null) {
                continue;
            }
            if (!aiUnit.getMission().isValid() || aiUnit.getMission() instanceof UnitWanderHostileMission
                    || aiUnit.getMission() instanceof UnitWanderMission
                    || aiUnit.getMission() instanceof IdleAtColonyMission
            // || aiUnit.getMission() instanceof DefendSettlementMission
            // || aiUnit.getMission() instanceof UnitSeekAndDestroyMission
            ) {
                aiUnit.setMission(null);
            }
        }
    }

    /**
     * Gives missions to all the naval units this player owns.
     */
    private void giveNavalMissions() {
        logger.finest("Entering method giveNavalMissions");
        Iterator<AIUnit> aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = aiUnitsIterator.next();
            Unit unit = aiUnit.getUnit();
            if (aiUnit.hasMission() || !unit.isNaval()){
                continue;
            }

            if (PrivateerMission.isValid(aiUnit)) {
                aiUnit.setMission(new PrivateerMission(getAIMain(), aiUnit));
            }
            else{
                aiUnit.setMission(new TransportMission(getAIMain(), aiUnit));
            }
        }
    }

    /**
     * Calls {@link AIColony#rearrangeWorkers} for every colony this player
     * owns.
     */
    private void rearrangeWorkersInColonies() {
        logger.finest("Entering method rearrangeWorkersInColonies");
        Iterator<AIColony> ci = getAIColonyIterator();
        while (ci.hasNext()) {
            AIColony c = ci.next();
            ArrayList<Tile> oldWorkTiles = new ArrayList<Tile>();
            for (ColonyTile colonyTile : c.getColony().getColonyTiles()) {
                if (colonyTile.getUnit() != null) {
                    oldWorkTiles.add(colonyTile.getWorkTile());
                }
            }

            c.rearrangeWorkers(getConnection());
        }
    }

    /**
     * Gives a mission to non-naval units.
     */
    private void giveNormalMissions() {
        logger.finest("Entering method giveNormalMissions");
        
        // Create a datastructure for the worker wishes:
        java.util.Map<UnitType, ArrayList<Wish>> workerWishes = new HashMap<UnitType, ArrayList<Wish>>();

        Iterator<AIColony> aIterator = getAIColonyIterator();
        while (aIterator.hasNext()) {
            Iterator<Wish> wIterator = aIterator.next().getWishIterator();
            while (wIterator.hasNext()) {
                Wish w = wIterator.next();
                if (w instanceof WorkerWish && w.getTransportable() == null) {
                    UnitType unitType = ((WorkerWish) w).getUnitType();
                    ArrayList<Wish> wishes = workerWishes.get(unitType);
                    if (wishes == null) {
                        wishes = new ArrayList<Wish>();
                        workerWishes.put(unitType, wishes);
                    }
                    wishes.add(w);
                }
            }
        }

        
        final boolean fewColonies = hasFewColonies();
        boolean isPioneerReq = PioneeringMission.getPlayerPioneers(this).size() == 0;
        Iterator<AIUnit> aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = aiUnitsIterator.next();
            
            if (aiUnit.hasMission()) {
                continue;
            }
            
            Unit unit = aiUnit.getUnit();
            
            if (unit.isUninitialized()) {
                logger.warning("Trying to assign a mission to an uninitialized object: " + unit.getId());
                continue;
            }
            
            // Setup as a pioneer if unit is:
            //      - already with tools, or
            //      - an expert pioneer, or
            //      - a non-expert unit and there are no other units assigned as pioneers
            boolean isPioneer = unit.hasAbility("model.ability.improveTerrain")
                                || unit.hasAbility("model.ability.expertPioneer");
            boolean isExpert = unit.getSkillLevel() > 0;
            if ((isPioneer || (isPioneerReq && !isExpert)) && PioneeringMission.isValid(aiUnit)) {
                aiUnit.setMission(new PioneeringMission(getAIMain(), aiUnit));
                isPioneerReq = false;
                continue;
            }
            
            if (unit.canCarryTreasure()) {
                aiUnit.setMission(new CashInTreasureTrainMission(getAIMain(), aiUnit));
            } else if (unit.hasAbility("model.ability.scoutIndianSettlement") &&
                       ScoutingMission.isValid(aiUnit)) {
                aiUnit.setMission(new ScoutingMission(getAIMain(), aiUnit));
            } else if ((unit.isOffensiveUnit() || unit.isDefensiveUnit())
                       && (!unit.isColonist() || unit.hasAbility("model.ability.expertSoldier") || 
                        getGame().getTurn().getNumber() > 5)) {
                giveMilitaryMission(aiUnit);
            } else if (unit.isColonist()) {                
                /*
                 * Motivated by (speed) performance: This map stores the
                 * distance between the unit and the destination of a Wish:
                 */
                HashMap<Location, Integer> distances = new HashMap<Location, Integer>(121);
                for (ArrayList<Wish> al : workerWishes.values()) {
                    for (Wish w : al) {
                        if (!distances.containsKey(w.getDestination())) {
                            distances.put(w.getDestination(), unit.getTurnsToReach(w.getDestination()));
                        }
                    }
                }

                // Check if this unit is needed as an expert (using:
                // "WorkerWish"):
                ArrayList<Wish> wishList = workerWishes.get(unit.getType());
                WorkerWish bestWish = null;
                int bestTurns = Integer.MAX_VALUE;
                for (int i = 0; i < wishList.size(); i++) {
                    WorkerWish ww = (WorkerWish) wishList.get(i);
                    if (ww.getTransportable() != null) {
                        wishList.remove(i);
                        i--;
                        continue;
                    }
                    int turns = distances.get(ww.getDestination());
                    if (turns == Integer.MAX_VALUE) {
                        if (ww.getDestination().getTile() == null) {
                            turns = 5;
                        } else {
                            turns = 10;
                        }
                    } else if (turns > 5) {
                        turns = 5;
                    }
                    if (bestWish == null
                            || ww.getValue() - (turns * 2) > bestWish.getValue() - (bestTurns * 2)) {
                        bestWish = ww;
                        bestTurns = turns;
                    }
                }
                if (bestWish != null) {
                    bestWish.setTransportable(aiUnit);
                    aiUnit.setMission(new WishRealizationMission(getAIMain(), aiUnit, bestWish));
                    continue;
                }
                // Find a site for a new colony:
                Tile colonyTile = null;
                if (getPlayer().canBuildColonies()) {
                    colonyTile = BuildColonyMission.findColonyLocation(aiUnit.getUnit());
                }
                if (colonyTile != null) {
                    bestTurns = unit.getTurnsToReach(colonyTile);
                }
                
                // Check if we can find a better site to work than a new colony:
                if (!fewColonies || colonyTile == null || bestTurns > 10) {
                    for (int i = 0; i < workerWishes.size(); i++) {
                        wishList = workerWishes.get(i);
                        for (int j = 0; j < wishList.size(); j++) {
                            WorkerWish ww = (WorkerWish) wishList.get(j);
                            if (ww.getTransportable() != null) {
                                wishList.remove(j);
                                j--;
                                continue;
                            }
                            int turns = distances.get(ww.getDestination());
                            if (turns == Integer.MAX_VALUE) {
                                if (ww.getDestination().getTile() == null) {
                                    turns = 5;
                                } else {
                                    turns = 10;
                                }
                            } else if (turns > 5) {
                                turns = 5;
                            }
                            // TODO: Choose to build colony if the value of the
                            // wish is low.
                            if (bestWish == null
                                    || ww.getValue() - (turns * 2) > bestWish.getValue() - (bestTurns * 2)) {
                                bestWish = ww;
                                bestTurns = turns;
                            }
                        }
                    }
                }
                if (bestWish != null) {
                    bestWish.setTransportable(aiUnit);
                    aiUnit.setMission(new WishRealizationMission(getAIMain(), aiUnit, bestWish));
                    continue;
                }
                // Choose to build a new colony:
                if (colonyTile != null) {
                	Mission mission = new BuildColonyMission(getAIMain(), 
                							aiUnit, 
                							colonyTile, 
                							getPlayer().getColonyValue(colonyTile));
                    aiUnit.setMission(mission);
                    
                    boolean isUnitOnCarrier = aiUnit.getUnit().isOnCarrier(); 
                    if (isUnitOnCarrier) {
                        AIUnit carrier = (AIUnit) getAIMain().getAIObject(
                                (FreeColGameObject) aiUnit.getUnit().getLocation());
                        
                        //make verification of carrier mission
                        Mission carrierMission = carrier.getMission();
                        
                        boolean isCarrierMissionToTransport = carrierMission instanceof TransportMission; 
                        if(!isCarrierMissionToTransport){
                        	throw new IllegalStateException("Carrier carrying unit not on a transport mission");
                        }
                        //transport unit to carrier destination (is this what is truly wanted?)
                        ((TransportMission) carrierMission).addToTransportList(aiUnit);
                    }
                    continue;
                }
            }
            if (!aiUnit.hasMission()) {
                if (aiUnit.getUnit().isOffensiveUnit()) {
                    aiUnit.setMission(new UnitWanderHostileMission(getAIMain(), aiUnit));
                } else {
                    //non-offensive units should take shelter in a nearby colony,
                    //not try to be hostile
                    aiUnit.setMission(new IdleAtColonyMission(getAIMain(), aiUnit));
                }
            }
        }
    }

    /**
     * Calls {@link AIColony#createAIGoods()} for every colony this player owns.
     */
    private void createAIGoodsInColonies() {
        logger.finest("Entering method createAIGoodsInColonies");

        Iterator<AIColony> ci = getAIColonyIterator();
        while (ci.hasNext()) {
            AIColony c = ci.next();
            c.createAIGoods();
        }
    }

    /**
     * 
     * Makes every unit perform their mission.
     * 
     */
    private void doMissions() {
        logger.finest("Entering method doMissions");
        Iterator<AIUnit> aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = aiUnitsIterator.next();
            if (aiUnit.hasMission() && aiUnit.getMission().isValid()
                    && !(aiUnit.getUnit().isOnCarrier())) {
                try {
                    aiUnit.doMission(getConnection());
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    logger.warning(sw.toString());
                }
            }
        }
    }

    int getDefendColonyMissionValue(Unit u, Colony colony, int turns) {
        logger.finest("Entering method getDefendColonyMissionValue");
        
        // Sanitation
        if(colony == null)
        	return Integer.MIN_VALUE;
        
        // Temporary helper method for: giveMilitaryMission
        int value = 10025 - turns;
        int numberOfDefendingUnits = 0;

        Iterator<AIUnit> aui = getAIUnitIterator();
        while (aui.hasNext()) {
            Mission m = aui.next().getMission();
            if (m != null && m instanceof DefendSettlementMission) {
                if (((DefendSettlementMission) m).getSettlement() == colony) {
                	//TODO: this decrease seems too little
                	value -= 6; 
                    numberOfDefendingUnits++;
                }
            }
        }
        if (u.getOwner().isREF()) {
            value -= 19;
            if (numberOfDefendingUnits > 0) {
                return 0;
            }
        }
        //TODO: Does not take into consideration the various levels of
        //fortification, only if has one or not
        if (colony.getStockade() != null &&
            numberOfDefendingUnits > colony.getStockade().getLevel() + 1) {
            return Math.max(0, value - 9000);
        }
        return value;
    }

    int getUnitSeekAndDestroyMissionValue(Unit unit, Tile newTile, int turns) {
        logger.finest("Entering method getUnitSeekAndDestroyMissionValue");
        
        Unit defender = newTile.getDefendingUnit(unit);
        
        if(!isTargetValidForSeekAndDestroy(unit, defender)){
        	return Integer.MIN_VALUE;
        }
        
        int value = 10020;
        CombatModel combatModel = unit.getGame().getCombatModel();
        
        if (getBestTreasureTrain(newTile) != null) {
        	value += Math.min(getBestTreasureTrain(newTile).getTreasureAmount() / 10, 50);
        }
        if (defender.getType().getOffence() > 0 &&
        		newTile.getSettlement() == null) {
        	value += 200 - combatModel.getDefencePower(unit, defender) * 2 - turns * 50;
        }
            
        value += combatModel.getOffencePower(defender, unit) -
              combatModel.getDefencePower(defender, unit);
        value -= turns * 10;
 
        if (!defender.isNaval()) {
        	if (defender.hasAbility("model.ability.expertSoldier")
                    && !defender.isArmed()) {
                value += 10 - combatModel.getDefencePower(unit, defender) * 2 - turns * 25;
            }
            if (newTile.getSettlement() != null) {
                value += 300;
                Iterator<Unit> dp = newTile.getUnitIterator();
                while (dp.hasNext()) {
                    Unit u = dp.next();
                    if (u.isDefensiveUnit()) {
                        if (combatModel.getDefencePower(unit, u) > combatModel.getOffencePower(unit, u)) {
                            value -= 100 * (combatModel.getDefencePower(unit, u) - combatModel.getOffencePower(unit, u));
                        } else {
                            value -= combatModel.getDefencePower(unit, u);
                        }
                    }
                }
            }
        }   
        return Math.max(0, value);
    }
    
    /**
     * TODO: Package for access by a test only - necessary?
     */     
    boolean isTargetValidForSeekAndDestroy(Unit attacker, Unit defender) {
        if (attacker.getOwner()!=getPlayer()) {
            logger.warning("isTargetValidForSeekAndDestroy() called for other players unit!");
            return false;
        }
    
        if (defender == null) { // Sanitation
            return false;
        }
    	
        // A naval unit cannot attack a land unit and vice-versa
        if (attacker.isNaval() != defender.isNaval()) {
            return false;
        }

        if (attacker.isNaval()) { // Naval units can only fight at sea
            if (attacker.getTile() == null || attacker.getTile().isLand()
                || defender.getTile() == null || defender.getTile().isLand()) {
                return false;
            }
        }

        Player attackerPlayer = attacker.getOwner();
        Player defenderPlayer = defender.getOwner();
        if (attackerPlayer == defenderPlayer) { // Cannot attack own units
            return false;
        }

        // If european, do not attack if not at war
        return attackerPlayer.atWarWith(defenderPlayer);
    }
        
    /**
     * Gives a military <code>Mission</code> to the given unit. <br>
     * <br>
     * <b>This method should only be used on units owned by european players.</b>
     *
     * TODO: Package for access by a test only - necessary?
     * 
     * @param aiUnit The unit.
     */
    void giveMilitaryMission(AIUnit aiUnit) {
        logger.finest("Entering method giveMilitaryMission");
        /*
         * 
         * Temporary method for giving a military mission.
         * 
         * This method will be removed when "MilitaryStrategy" and
         * 
         * the "Tactic"-classes has been implemented.
         * 
         */
        final Unit unit = aiUnit.getUnit();
        Unit carrier = (unit.isOnCarrier()) ? (Unit) unit.getLocation() : null;
        Map map = unit.getGame().getMap();
        // Initialize variables:
        Ownable bestTarget = null; // The best target for a mission.
        int bestValue = Integer.MIN_VALUE; // The value of the target above.
        // Determine starting tile:
        Tile startTile = unit.getTile();
        if (startTile == null) {
            startTile = ((unit.isOnCarrier()) ? ((Unit) unit.getLocation())
                         : unit).getFullEntryLocation();
            if (startTile == null) {
                logger.warning("Unable to determine entry location for: "
                               + unit.toString());
                return;
            }
        }
        /*
         * 
         * Checks if we are currently located on a Tile with a Settlement
         * 
         * which requires defenders:
         * 
         */
        if (unit.getColony() != null) {
            bestTarget = unit.getColony();
            bestValue = getDefendColonyMissionValue(unit, (Colony) bestTarget, 0);
        }
        // Checks if a nearby colony requires additional defence:
        GoalDecider gd = new GoalDecider() {
            private PathNode best = null;

            private int bestValue = Integer.MIN_VALUE;


            public PathNode getGoal() {
                return best;
            }

            public boolean hasSubGoals() {
                return true;
            }

            public boolean check(Unit u, PathNode pathNode) {
                Tile t = pathNode.getTile();
                if (t.getColony() != null && t.getColony().getOwner() == u.getOwner()) {
                    int value = getDefendColonyMissionValue(unit, t.getColony(), pathNode.getTurns());
                    if (value > 0 && value > bestValue) {
                        bestValue = value;
                        best = pathNode;
                    }
                    return true;
                } else {
                    return false;
                }
            }
        };
        final int MAXIMUM_DISTANCE_TO_SETTLEMENT = 10; // Given in number of
        // turns.
        PathNode bestPath = map.search(unit, startTile, gd,
                MAXIMUM_DISTANCE_TO_SETTLEMENT, carrier);
        if (bestPath != null) {
            PathNode ln = bestPath.getLastNode();
            int value = getDefendColonyMissionValue(unit, ln.getTile().getColony(), ln.getTurns());
            if (value > bestValue) {
                bestTarget = ln.getTile().getColony();
                bestValue = value;
            }
        }
        // Searches for the closest target for an existing
        // "UnitSeekAndDestroyMission":
        Location bestExistingTarget = null;
        int smallestDifference = Integer.MAX_VALUE;
        Iterator<AIUnit> aui = getAIUnitIterator();
        while (aui.hasNext() && smallestDifference>0) {
            AIUnit coAIUnit = aui.next();
            Unit coUnit = coAIUnit.getUnit();
            if (coUnit.getTile() != null && coAIUnit.getMission() instanceof UnitSeekAndDestroyMission) {
                Location target = ((UnitSeekAndDestroyMission) coAIUnit.getMission()).getTarget();
                int ourDistance = unit.getTurnsToReach(startTile, target.getTile());
                int coUnitDistance = coUnit.getTurnsToReach(target.getTile());
                if (ourDistance != Integer.MAX_VALUE) {
                    int difference = Math.abs(ourDistance - coUnitDistance);
                    if (difference < smallestDifference) {
                        smallestDifference = difference;
                        bestExistingTarget = target;
                    }
                }
            }
        }
        if (bestExistingTarget != null) {
            int value = getUnitSeekAndDestroyMissionValue(unit, bestExistingTarget.getTile(), smallestDifference);
            if (value > bestValue) {
                bestValue = value;
                bestTarget = (Ownable) bestExistingTarget;
            }
        }
        // Checks if there is a better target than the existing one:
        GoalDecider targetDecider = new GoalDecider() {
            private PathNode bestTarget = null;

            private int bestNewTargetValue = Integer.MIN_VALUE;


            public PathNode getGoal() {
                return bestTarget;
            }

            public boolean hasSubGoals() {
                return true;
            }

            public boolean check(Unit u, PathNode pathNode) {
                Tile newTile = pathNode.getTile();
                Unit defender = newTile.getDefendingUnit(unit);
                if (isTargetValidForSeekAndDestroy(unit, defender)) {
                    int value = getUnitSeekAndDestroyMissionValue(unit, pathNode.getTile(), pathNode.getTurns());
                    if (value > bestNewTargetValue) {
                        bestTarget = pathNode;
                        bestNewTargetValue = value;
                        return true;
                    }
                }
                return false;
            }
        };
        PathNode newTarget = map.search(unit, startTile, targetDecider, Integer.MAX_VALUE,
                carrier);
        if (newTarget != null) {
            Tile targetTile = newTarget.getLastNode().getTile();
            int value = getUnitSeekAndDestroyMissionValue(unit, targetTile, newTarget.getTotalTurns());
            if (value > bestValue) {
                bestValue = value;
                if (targetTile.getSettlement() != null) {
                    bestTarget = targetTile.getSettlement();
                } else if (getBestTreasureTrain(targetTile) != null) {
                    bestTarget = getBestTreasureTrain(targetTile);
                } else {
                    bestTarget = targetTile.getDefendingUnit(unit);
                }
            }
        }
        // Use the best target:
        if (bestTarget != null && bestValue > 0) {
            if (bestTarget.getOwner() == unit.getOwner()) {
                aiUnit.setMission(new DefendSettlementMission(getAIMain(), aiUnit, (Colony) bestTarget));
            } else {
                aiUnit.setMission(new UnitSeekAndDestroyMission(getAIMain(), aiUnit, (Location) bestTarget));
            }
        } else {
            // Just give a simple mission if we could not find a better one:
            aiUnit.setMission(new UnitWanderHostileMission(getAIMain(), aiUnit));
        }
    }

    /**
     * Maps <code>Transportable</code>s to carrier's using a
     * <code>TransportMission</code>.
     */
    private void createTransportLists() {
        logger.finest("Entering method createTransportLists");

        ArrayList<Transportable> transportables = new ArrayList<Transportable>();

        // Add units to transport
        Iterator<AIUnit> aui = getAIUnitIterator();
        while (aui.hasNext()) {
            AIUnit au = aui.next();
            // naval units do not need transports
            if(au.getUnit().isNaval()){
                continue;
            }
            if (au.getTransportDestination() != null && au.getTransport() == null) {
                transportables.add(au);
            }
        }

        // Add goods to transport
        Iterator<AIColony> aci = getAIColonyIterator();
        while (aci.hasNext()) {
            AIColony ac = aci.next();
            Iterator<AIGoods> agi = ac.getAIGoodsIterator();
            while (agi.hasNext()) {
                AIGoods ag = agi.next();
                if (ag.getTransportDestination() != null && ag.getTransport() == null) {
                    transportables.add(ag);
                }
            }
        }

        // save further processing
        if(transportables.isEmpty()){
            return;
        }
        
        // Update the priority
        for (Transportable t : transportables){
            t.increaseTransportPriority();
        }

        // get available transports
        ArrayList<Mission> vacantTransports = new ArrayList<Mission>();
        Iterator<AIUnit> iter = getAIUnitIterator();
        while (iter.hasNext()) {
            AIUnit au = iter.next();
            if (au.hasMission() && au.getMission() instanceof TransportMission
                    && !(au.getUnit().getLocation() instanceof Europe)) {
                vacantTransports.add(au.getMission());
            }
        }
        
        // save further processing
        // we must only do this verification after the priority update
        if(vacantTransports.isEmpty()){
            return;
        }
        
        // order the list by priority
        Collections.sort(transportables, new Comparator<Transportable>() {
            public int compare(Transportable o1, Transportable o2) {
                if (o1 == o2) {
                    return 0;
                }
                int result = o2.getTransportPriority() - o1.getTransportPriority();
                if (result == 0) {
                    result = o1.getId().compareTo(o2.getId());
                }
                return result;
            }
        });

        // Since we are manipulating the contents of the list, we need to have a clone list
        //to iterate through
        // If a good is already in a carrier, just add it the transport list
        // Note however that it may not be possible to complete such transport
        //in which case, the carrier should dump the transportable in the nearest colony
        ArrayList<Transportable> iteratingList = new ArrayList<Transportable>(transportables);
        for(Transportable t : iteratingList){
            Location transportableLoc = t.getTransportLocatable().getLocation();
            boolean isTransportableAlreadyOnCarrier = transportableLoc instanceof Unit;
            if (isTransportableAlreadyOnCarrier) {
                AIUnit carrierAI = (AIUnit) getAIMain().getAIObject((Unit) transportableLoc);
                Mission m = carrierAI.getMission();
                if (m instanceof TransportMission) {
                    ((TransportMission) m).addToTransportList(t);
                }
                transportables.remove(t);
            }
        }

        while (transportables.size() > 0) {
            Transportable t = transportables.get(0);
            TransportMission bestTransport = null;
            int bestTransportSpace = 0;
            int bestTransportTurns = Integer.MAX_VALUE;
            for (int i = 0; i < vacantTransports.size(); i++) {
                TransportMission tm = (TransportMission) vacantTransports.get(i);
                if (t.getTransportSource().getTile() == tm.getUnit().getLocation().getTile()) {
                    int transportSpace = tm.getAvailableSpace(t);
                    if (transportSpace > 0) {
                        bestTransport = tm;
                        bestTransportSpace = transportSpace;
                        bestTransportTurns = 0;
                        break;
                    } else {
                        continue;
                    }
                }
                PathNode path = tm.getPath(t);
                if (path != null && path.getTotalTurns() <= bestTransportTurns) {
                    int transportSpace = tm.getAvailableSpace(t);
                    if (transportSpace > 0
                            && (path.getTotalTurns() < bestTransportTurns || transportSpace > bestTransportSpace)) {
                        bestTransport = tm;
                        bestTransportSpace = transportSpace;
                        bestTransportTurns = path.getTotalTurns();
                    }
                }
            }
            if (bestTransport == null) {
                // No more transports available:
                break;
            }
            bestTransport.addToTransportList(t);
            transportables.remove(t);
            vacantTransports.remove(bestTransport);
            bestTransportSpace--;
            for (int i = 0; i < transportables.size() && bestTransportSpace > 0; i++) {
                Transportable t2 = transportables.get(0);
                if (t2.getTransportLocatable().getLocation() == t.getTransportLocatable().getLocation()) {
                    bestTransport.addToTransportList(t2);
                    transportables.remove(t2);
                    bestTransportSpace--;
                }
            }
        }
    }

    /**
     * Returns the treasure train carrying the largest treasure
     * located on the given <code>Tile</code>.
     * 
     * @param tile a <code>Tile</code> value
     * @return The best treasure train or <code>null</code> if no treasure
     *         train is located on this <code>Tile</code>.
     */
    private Unit getBestTreasureTrain(Tile tile) {
        Unit bestTreasureTrain = null;
        for (Unit unit : tile.getUnitList()) {
            if (unit.canCarryTreasure() &&
                (bestTreasureTrain == null || 
                 bestTreasureTrain.getTreasureAmount() < unit.getTreasureAmount())) {
                bestTreasureTrain = unit;
            }
        }

        return bestTreasureTrain;
    }

    private void manageMissionaries() {
        Iterator<AIUnit> it = getAIUnitIterator();
        while (it.hasNext()) {
            AIUnit au = it.next();
            Unit u = au.getUnit();
            
            if (u.getRole()==Role.MISSIONARY) {
                logger.info("Found missionary unit:"+u.getId());
                mGoal.addUnit(au);
                //NOTE: Removes units being assigned a goal from the units list
                it.remove(); 
            }
        }
    }

    /**
     * Returns an iterator over all <code>AIUnit</code>s owned by this
     * player, which currently do _not_ have a goal.
     * 
     * This is a temporary override to allow the old AI code to keep working
     * with some units, while exempting those already managed by new code.
     * 
     * @return The <code>Iterator</code>.
     */
    protected Iterator<AIUnit> getAIUnitIterator() {
        logger.info("Override: getAIUnitIterator()!");
        if (myAIUnits.size() == 0) {
            ArrayList<AIUnit> au = new ArrayList<AIUnit>();
            Iterator<Unit> unitsIterator = getPlayer().getUnitIterator();
            while (unitsIterator.hasNext()) {
                Unit theUnit = unitsIterator.next();
                AIUnit a = (AIUnit) getAIMain().getAIObject(theUnit.getId());
                if (a != null) {
                    if (a.getGoal() == null) au.add(a);
                } else {
                    logger.warning("Could not find the AIUnit for: "
                                   + theUnit + " (" + theUnit.getId() + ") - "
                                   + (getGame().getFreeColGameObject(theUnit.getId()) != null));
                }
            }
            myAIUnits = au;
        }
        return myAIUnits.iterator();
    }

    /**
     * Helper method to let implementing subclasses clear aiUnits.
     * Override, see above!     
     */         
    protected void clearAIUnits() {
        logger.info("Override: clearAIUnits()!");
        myAIUnits.clear();    
    }

}
