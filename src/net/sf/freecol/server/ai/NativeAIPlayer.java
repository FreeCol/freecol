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

package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.ColonyTradeItem;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.GoldTradeItem;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsTradeItem;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
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
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitTradeItem;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.server.ai.mission.BuildColonyMission;
import net.sf.freecol.server.ai.mission.CashInTreasureTrainMission;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.IdleAtColonyMission;
import net.sf.freecol.server.ai.mission.IndianBringGiftMission;
import net.sf.freecol.server.ai.mission.IndianDemandMission;
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
 * Objects of this class contains AI-information for a single {@link
 * Player} and is used for controlling this player.
 *
 * The method {@link #startWorking} gets called by the
 * {@link AIInGameInputHandler} when it is this player's turn.
 */
public class NativeAIPlayer extends AIPlayer {

    private static final Logger logger = Logger.getLogger(NativeAIPlayer.class.getName());

    /**
     * Stores temporary information for sessions (trading with another player
     * etc).
     */
    private HashMap<String, Integer> sessionRegister = new HashMap<String, Integer>();

    /**
     * Creates a new <code>AIPlayer</code>.
     *
     * @param aiMain The main AI-class.
     * @param player The player that should be associated with this
     *            <code>AIPlayer</code>.
     */
    public NativeAIPlayer(AIMain aiMain, ServerPlayer player) {
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
    public NativeAIPlayer(AIMain aiMain, Element element) {
        super(aiMain, element.getAttribute(ID_ATTRIBUTE));
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>AIPlayer</code>.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public NativeAIPlayer(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain, in.getAttributeValue(null, ID_ATTRIBUTE));
        readFromXML(in);
    }


/* IMPLEMENTATION (AIPlayer interface) ****************************************/


    /**
     * Tells this <code>AIPlayer</code> to make decisions. The
     * <code>AIPlayer</code> is done doing work this turn when this method
     * returns.
     */
    public void startWorking() {
        final Player player = getPlayer();
        logger.finest("Entering method startWorking: "
                      + player + ", year " + getGame().getTurn());
        sessionRegister.clear();
        clearAIUnits();
        determineStances();
        abortInvalidAndOneTimeMissions();
        ensureCorrectMissions();
        secureSettlements();
        giveNormalMissions();
        bringGifts();
        demandTribute();
        doMissions();
        abortInvalidMissions();
        // Some of the mission might have been invalidated by a another mission.
        giveNormalMissions();
        doMissions();
        abortInvalidMissions();
        ensureCorrectMissions();
        clearAIUnits();
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

    public boolean acceptDiplomaticTrade(DiplomaticTrade agreement) {
        boolean validOffer = true;
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
                switch (stance) {
                    case UNCONTACTED:
                        validOffer = false; //never accept invalid stance change
                        break;
                    case WAR: // always accept war without cost
                        break;
                    case CEASE_FIRE:
                        value -= 500;
                        break;
                    case PEACE:
                        if (!agreement.getSender().hasAbility("model.ability.alwaysOfferedPeace")) {
                            // TODO: introduce some kind of counter in order to avoid
                            // Benjamin Franklin exploit
                            value -= 1000;
                        }
                        break;
                    case ALLIANCE:
                        value -= 2000;
                        break;
                    }

            } else if (item instanceof ColonyTradeItem) {
                // TODO: evaluate whether we might wish to give up a colony
                if (item.getSource() == getPlayer()) {
                    validOffer = false;
                    break;
                } else {
                    value += 1000;
                }
            } else if (item instanceof UnitTradeItem) {
                // TODO: evaluate whether we might wish to give up a unit
                if (item.getSource() == getPlayer()) {
                    validOffer = false;
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
        if (validOffer) {
            logger.info("Trade value is " + value + ", accept if >=0");
        } else {
            logger.info("Trade offer is considered invalid!");
        }
        return (value>=0)&&validOffer;
    }


    /**
     * Called after another <code>Player</code> sends a <code>trade</code> message
     *
     *
     * @param goods The goods which we are going to offer
     */
    public void registerSellGoods(Goods goods) {
        String goldKey = "tradeGold#" + goods.getType().getId() + "#" + goods.getAmount()
            + "#" + goods.getLocation().getId();
        sessionRegister.put(goldKey, null);
    }

    /**
     * Called when another <code>Player</code> proposes to buy.
     *
     *
     * @param unit The foreign <code>Unit</code> trying to trade.
     * @param settlement The <code>Settlement</code> this player owns and
     *            which the given <code>Unit</code> is trading.
     * @param goods The goods the given <code>Unit</code> is trying to sell.
     * @param gold The suggested price.
     * @return The price this <code>AIPlayer</code> suggests or
     *         {@link NetworkConstants#NO_TRADE}.
     */
    public int buyProposition(Unit unit, Settlement settlement, Goods goods, int gold) {
        logger.finest("Entering method buyProposition");
        Player buyer = unit.getOwner();
        String goldKey = "tradeGold#" + goods.getType().getId() + "#" + goods.getAmount()
            + "#" + settlement.getId();
        String hagglingKey = "tradeHaggling#" + unit.getId();

        Integer registered = sessionRegister.get(goldKey);
        if (registered == null) {
            int price = ((IndianSettlement) settlement).getPriceToSell(goods)
                + getPlayer().getTension(buyer).getValue();
            Unit missionary = ((IndianSettlement) settlement).getMissionary(buyer);
            if (missionary != null && getSpecification()
                .getBoolean("model.option.enhancedMissionaries")) {
                // 10% bonus for missionary, 20% if expert
                int bonus = (missionary.hasAbility("model.ability.expertMissionary")) ? 8
                    : 9;
                price = (price * bonus) / 10;
            }
            sessionRegister.put(goldKey, new Integer(price));
            return price;
        } else {
            int price = registered.intValue();
            if (price < 0 || price == gold) {
                return price;
            } else if (gold < (price * 9) / 10) {
                logger.warning("Cheating attempt: sending a offer too low");
                sessionRegister.put(goldKey, new Integer(-1));
                return NetworkConstants.NO_TRADE;
            } else {
                int haggling = 1;
                if (sessionRegister.containsKey(hagglingKey)) {
                    haggling = sessionRegister.get(hagglingKey).intValue();
                }
                if (getAIRandom().nextInt(3 + haggling) <= 3) {
                    sessionRegister.put(goldKey, new Integer(gold));
                    sessionRegister.put(hagglingKey, new Integer(haggling + 1));
                    return gold;
                } else {
                    sessionRegister.put(goldKey, new Integer(-1));
                    return NetworkConstants.NO_TRADE;
                }
            }
        }
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
        Player seller = unit.getOwner();
        if (settlement instanceof IndianSettlement) {
            String goldKey = "tradeGold#" + goods.getType().getId() + "#" + goods.getAmount() + "#" + unit.getId();
            String hagglingKey = "tradeHaggling#" + unit.getId();
            int price;
            if (sessionRegister.containsKey(goldKey)) {
                price = sessionRegister.get(goldKey).intValue();
                if (price <= 0) {
                    return price;
                }
            } else {
                price = ((IndianSettlement) settlement).getPriceToBuy(goods) - getPlayer().getTension(seller).getValue();
                Unit missionary = ((IndianSettlement) settlement).getMissionary(seller);
                if (missionary != null && getSpecification()
                    .getBoolean("model.option.enhancedMissionaries")) {
                    // 10% bonus for missionary, 20% if expert
                    int bonus = (missionary.hasAbility("model.ability.expertMissionary")) ? 12
                        : 11;
                    price = (price * bonus) / 10;
                }
                if (price <= 0) return 0;
                sessionRegister.put(goldKey, new Integer(price));
            }
            if (gold < 0 || price == gold) {
                return price;
            } else if (gold > (price * 11) / 10) {
                logger.warning("Cheating attempt: haggling with a request too high");
                sessionRegister.put(goldKey, new Integer(-1));
                return NetworkConstants.NO_TRADE;
            } else {
                int haggling = 1;
                if (sessionRegister.containsKey(hagglingKey)) {
                    haggling = sessionRegister.get(hagglingKey).intValue();
                }
                if (getAIRandom().nextInt(3 + haggling) <= 3) {
                    sessionRegister.put(goldKey, new Integer(gold));
                    sessionRegister.put(hagglingKey, new Integer(haggling + 1));
                    return gold;
                } else {
                    sessionRegister.put(goldKey, new Integer(-1));
                    return NetworkConstants.NO_TRADE;
                }
            }
        } else if (settlement instanceof Colony) {
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
            throw new IllegalArgumentException("Unknown type of settlement.");
        }
    }

/* Internal methods ***********************************************************/


    /**
     * Ensures that all workers inside a colony gets a
     * {@link WorkInsideColonyMission}.
     */
    private void ensureCorrectMissions() {
        logger.finest("Entering method ensureCorrectMissions");
        if (getPlayer().isIndian()) return;

        for (AIUnit au : getAIUnits()) {
            if (au.hasMission()) continue;

            // TODO: Find out why this happens, or even if it still does.
            Unit u = au.getUnit();
            if (u.getLocation() instanceof WorkLocation) {
                AIColony ac = getAIColony(u.getColony());
                if (ac == null) {
                    logger.warning("No AIColony for unit: " + u
                                   + " at: " + u.getLocation());
                    continue;
                }
                au.setMission(new WorkInsideColonyMission(getAIMain(), au, ac));
            }
        }
    }

    /**
     * Determines the stances towards each player.
     * That is: should we declare war?
     * TODO: something better, that includes peacemaking.
     */
    private void determineStances() {
        logger.finest("Entering method determineStances");
        Player player = getPlayer();
        for (Player p : getGame().getPlayers()) {
            if (p != player && !p.isDead()) determineStance(p);
        }
    }

    /**
     * Aborts all the missions which are no longer valid.
     */
    private void abortInvalidMissions() {
        for (AIUnit au : getAIUnits()) {
            if (au.getMission() == null) continue;
            if (!au.getMission().isValid()) {
                logger.finest("Abort invalid mission for: " + au.getUnit());
                au.setMission(null);
            }
        }
    }

    /**
     * Aborts all the missions which are no longer valid.
     */
    private void abortInvalidAndOneTimeMissions() {
        for (AIUnit au : getAIUnits()) {
            Mission mission = au.getMission();
            if (mission == null) continue;
            if (!mission.isValid()) {
                logger.finest("Abort invalid mission: " + mission
                              + " for: " + au.getUnit());
                au.setMission(null);
            } else if (mission instanceof UnitWanderHostileMission
                       || mission instanceof UnitWanderMission
                       || mission instanceof IdleAtColonyMission
                       // TODO: Mission.isOneTime()
                       ) {
                logger.finest("Abort one-time mission: " + mission
                              + " for: " + au.getUnit());
                au.setMission(null);
            }
        }
    }

    /**
     * Takes the necessary actions to secure the settlements. This is done by
     * making new military units or to give existing units new missions.
     */
    private void secureSettlements() {
        List<IndianSettlement> settlements
            = getPlayer().getIndianSettlements();
        for (IndianSettlement is : settlements) {
            // Spread arms and horses between camps
            // TODO: maybe make this dependent on difficulty level?
            int n = getAIRandom().nextInt(settlements.size());
            IndianSettlement settlement = settlements.get(n);
            if (settlement != is) {
                is.tradeGoodsWithSetlement(settlement);
            }
        }
        for (IndianSettlement is : settlements) {
            equipBraves(is);
            secureIndianSettlement(is);
        }
    }

    /**
     * Equips braves with horses and muskets.
     * Keeps some for the settlement defence.
     *
     * @param is The <code>IndianSettlement</code> where the equipping occurs.
     */
    public void equipBraves(IndianSettlement is) {
        Specification spec = getGame().getSpecification();
        GoodsType armsType = spec.getGoodsType("model.goods.muskets");
        GoodsType horsesType = spec.getGoodsType("model.goods.horses");
        EquipmentType armsEqType = spec.getEquipmentType("model.equipment.indian.muskets");
        EquipmentType horsesEqType = spec.getEquipmentType("model.equipment.indian.horses");

        int musketsToArmIndian = armsEqType.getAmountRequiredOf(armsType);
        int horsesToMountIndian = horsesEqType.getAmountRequiredOf(horsesType);
        int armsAvail = is.getGoodsCount(armsType);
        int horsesAvail = is.getGoodsCount(horsesType);

        for (Unit brave : is.getUnitList()) {
            if (armsAvail < musketsToArmIndian) {
                break;
            }
            if (brave.isArmed()) {
                continue;
            }
            logger.info("Equipping brave with muskets");
            AIMessage.askEquipUnit(getAIUnit(brave), armsEqType, 1);
            if (!brave.isArmed()) {
                logger.warning("Brave has not been armed");
            }
            armsAvail = is.getGoodsCount(armsType);
        }

        for (Unit brave : is.getUnitList()) {
            if (horsesAvail < horsesToMountIndian) {
                break;
            }
            if (brave.isMounted()) {
                continue;
            }
            logger.info("Equipping brave with horses");
            AIMessage.askEquipUnit(getAIUnit(brave), horsesEqType, 1);
            if (!brave.isMounted()) {
                logger.warning("Brave has not mounted");
            }
            horsesAvail = is.getGoodsCount(horsesType);
        }
    }

    /**
     * Remove all equipment on a unit.
     *
     * @param unit The <code>Unit</code> to remove equipment from.
     */
    public void removeAllEquipment(Unit unit) {
        AIUnit aiUnit = getAIUnit(unit);
        Set<EquipmentType> kit = unit.getEquipment().keySet();
        for (EquipmentType type : new ArrayList<EquipmentType>(kit)) {
            AIMessage.askEquipUnit(aiUnit, type, -unit.getEquipmentCount(type));
        }
    }

    /**
     * Switches equipment between colonists
     */
    public void switchEquipmentWith(Unit unit1, Unit unit2){
        if(!unit1.isColonist() || !unit2.isColonist()){
            throw new IllegalArgumentException("Both units need to be colonists to switch equipment");
        }

        if(unit1.getTile() != unit2.getTile()){
            throw new IllegalStateException("Units can only switch equipment in the same location");
        }

        if(unit1.getSettlement() == null){
            throw new IllegalStateException("Units can only switch equipment in a settlement");
        }

        // TODO: use the TypeCountMap that getEquipment() returns and
        // swap the counts as well.
        List<EquipmentType> equipList1 = new ArrayList<EquipmentType>(unit1.getEquipment().keySet());
        List<EquipmentType> equipList2 = new ArrayList<EquipmentType>(unit2.getEquipment().keySet());
        removeAllEquipment(unit1);
        removeAllEquipment(unit2);
        for(EquipmentType equip : equipList2){
            AIMessage.askEquipUnit(getAIUnit(unit1), equip, 1);
        }
        for(EquipmentType equip : equipList1){
            AIMessage.askEquipUnit(getAIUnit(unit2), equip, 1);
        }
    }


    /**
     * Takes the necessary actions to secure an indian settlement
     *
     * TODO: Package for access by a test only - necessary?
     */
    void secureIndianSettlement(IndianSettlement is) {
        // if not at war, no need to secure settlement
        if (!is.getOwner().isAtWar()) {
            return;
        }

        int defenders = is.getTile().getUnitCount();
        int threat = 0;
        int worstThreat = 0;
        Location bestTarget = null;

        for (Tile t: is.getTile().getSurroundingTiles(2)) {
            // Do not check ocean tiles
            // Indians do not have naval power
            if(!t.isLand()){
                continue;
            }

            // No units on tile
            if (t.getUnitCount() == 0) {
                continue;
            }

            Player enemy = t.getFirstUnit().getOwner();

            // Own units on tile
            if (enemy == getPlayer()) {
                defenders++;
                continue;
            }

            if (!getPlayer().hasContacted(enemy)) continue;
            int value = getPlayer().getTension(enemy).getValue();
            int threatModifier = 0;
            int unitThreat = 0;
            if (value >= Tension.TENSION_ADD_MAJOR) {
                threatModifier = 2;
                unitThreat = t.getUnitCount() * 2;
            } else if (value >= Tension.TENSION_ADD_MINOR) {
                threatModifier = 1;
                unitThreat = t.getUnitCount();
            }

            threat += threatModifier;
            if (unitThreat > worstThreat) {
                if (t.getSettlement() != null) {
                    bestTarget = t.getSettlement();
                } else {
                    bestTarget = t.getFirstUnit();
                }
                worstThreat = unitThreat;
            }
        }
        //Note: this is totally arbitrary
        if (threat > defenders && bestTarget != null) {
            AIUnit newDefenderAI = getBraveForSeekAndDestroy(is);
            if (newDefenderAI != null) {
                Tile targetTile = bestTarget.getTile();
                if (isTargetValidForSeekAndDestroy(newDefenderAI.getUnit(), targetTile)) {
                    newDefenderAI.setMission(new UnitSeekAndDestroyMission(getAIMain(), newDefenderAI, bestTarget));
                }
            }
        }
    }

    private AIUnit getBraveForSeekAndDestroy(final IndianSettlement indianSettlement) {
        final Iterator<Unit> it = indianSettlement.getOwnedUnitsIterator();
        while (it.hasNext()) {
            final AIUnit chosenOne = getAIUnit(it.next());
            if (chosenOne.getUnit().getLocation() instanceof Tile
                && (chosenOne.getMission() == null
                    || chosenOne.getMission() instanceof UnitWanderHostileMission)) {
                return chosenOne;
            }
        }
        return null;
    }

    /**
     * Gives a mission to non-naval units.
     */
    private void giveNormalMissions() {
        logger.finest("Entering method giveNormalMissions");

        // Create a datastructure for the worker wishes:
        java.util.Map<UnitType, ArrayList<Wish>> workerWishes =
            new HashMap<UnitType, ArrayList<Wish>>();
        for (UnitType unitType : getAIMain().getGame().getSpecification().getUnitTypeList()) {
            workerWishes.put(unitType, new ArrayList<Wish>());
        }

        final boolean fewColonies = false;
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

            if (unit.getState() == UnitState.IN_COLONY
                && unit.getSettlement().getUnitCount() <= 1) {
                // The unit has its hand full keeping the colony alive.
                continue;
            }

            if (unit.isOffensiveUnit() || unit.isDefensiveUnit()){
            	Player owner = unit.getOwner();
            	boolean isPastStart = getGame().getTurn().getNumber() > 5
            			&& !owner.getSettlements().isEmpty();

            	if(!unit.isColonist() 
            			|| isPastStart
            			|| owner.isIndian()
            			|| owner.isREF()){
            		giveMilitaryMission(aiUnit);
            		continue;
            	}
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
     * Brings gifts to nice players with nearby colonies.
     */
    private void bringGifts() {
        logger.finest("Entering method bringGifts");
        for (IndianSettlement indianSettlement : getPlayer().getIndianSettlements()) {
            // Do not bring gifts all the time:
            if (getAIRandom().nextInt(10) != 1) {
                continue;
            }
            int alreadyAssignedUnits = 0;
            Iterator<Unit> ownedUnits = indianSettlement.getOwnedUnitsIterator();
            while (ownedUnits.hasNext()) {
                if (getAIUnit(ownedUnits.next()).getMission() instanceof IndianBringGiftMission) {
                    alreadyAssignedUnits++;
                }
            }
            if (alreadyAssignedUnits > MAX_NUMBER_OF_GIFTS_BEING_DELIVERED) {
                continue;
            }
            // Creates a list of nearby colonies:
            ArrayList<Colony> nearbyColonies = new ArrayList<Colony>();
            for (Tile t: indianSettlement.getTile().getSurroundingTiles(MAX_DISTANCE_TO_BRING_GIFT)) {
                if (t.getColony() != null
                        && IndianBringGiftMission.isValidMission(getPlayer(), t.getColony().getOwner())) {
                    nearbyColonies.add(t.getColony());
                }
            }
            if (nearbyColonies.size() > 0) {
                Colony target = nearbyColonies.get(getAIRandom().nextInt(nearbyColonies.size()));
                Iterator<Unit> it2 = indianSettlement.getOwnedUnitsIterator();
                AIUnit chosenOne = null;
                while (it2.hasNext()) {
                    chosenOne = getAIUnit(it2.next());
                    if (chosenOne.getUnit().getLocation() instanceof Tile
                        && chosenOne.getUnit().canCarryGoods()
                        && (chosenOne.getMission() == null
                            || chosenOne.getMission() instanceof UnitWanderHostileMission)) {
                        // Check that the colony can be reached:
                        PathNode pn = chosenOne.getUnit().findPath(indianSettlement.getTile(),
                                                                   target.getTile());
                        if (pn != null && pn.getTotalTurns() <= MAX_DISTANCE_TO_BRING_GIFT) {
                            chosenOne.setMission(new IndianBringGiftMission(getAIMain(), chosenOne, target));
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Demands goods from players with nearby colonies.
     */
    private void demandTribute() {
        logger.finest("Entering method demandTribute");
        for (IndianSettlement indianSettlement : getPlayer().getIndianSettlements()) {
            // Do not demand goods all the time:
            if (getAIRandom().nextInt(10) != 1) {
                continue;
            }
            int alreadyAssignedUnits = 0;
            Iterator<Unit> ownedUnits = indianSettlement.getOwnedUnitsIterator();
            while (ownedUnits.hasNext()) {
                if (getAIUnit(ownedUnits.next()).getMission() instanceof IndianDemandMission) {
                    alreadyAssignedUnits++;
                }
            }
            if (alreadyAssignedUnits > MAX_NUMBER_OF_DEMANDS) {
                continue;
            }
            // Creates a list of nearby colonies:
            ArrayList<Colony> nearbyColonies = new ArrayList<Colony>();

            for (Tile t: indianSettlement.getTile().getSurroundingTiles(MAX_DISTANCE_TO_MAKE_DEMANDS)) {
                if (t.getColony() != null) {
                    nearbyColonies.add(t. getColony());
                }
            }
            if (nearbyColonies.size() > 0) {
                int targetTension = Integer.MIN_VALUE;
                Colony target = null;
                for (int i = 0; i < nearbyColonies.size(); i++) {
                    Colony t = nearbyColonies.get(i);
                    Player to = t.getOwner();
                    if (!getPlayer().hasContacted(to)
                        || !indianSettlement.hasContactedSettlement(to)) {
                        continue;
                    }
                    int tension = 1 + getPlayer().getTension(to).getValue()
                        + indianSettlement.getAlarm(to).getValue();
                    tension = getAIRandom().nextInt(tension);
                    if (tension > targetTension) {
                        targetTension = tension;
                        target = t;
                    }
                }
                if (target != null) {
                    Iterator<Unit> it2 = indianSettlement.getOwnedUnitsIterator();
                    AIUnit chosenOne = null;
                    while (it2.hasNext()) {
                        chosenOne = getAIUnit(it2.next());
                        if (chosenOne.getUnit().getLocation() instanceof Tile
                            && chosenOne.getUnit().canCarryGoods()
                            && (chosenOne.getMission() == null
                                || chosenOne.getMission() instanceof UnitWanderHostileMission)) {
                            // Check that the colony can be reached:
                            PathNode pn = chosenOne.getUnit().findPath(indianSettlement.getTile(),
                                                                       target.getTile());
                            if (pn != null && pn.getTotalTurns() <= MAX_DISTANCE_TO_MAKE_DEMANDS) {
                                // Make it less probable that nice players get targeted
                                // for a demand mission:
                                Player tp = target.getOwner();
                                int tension = 1 + getPlayer().getTension(tp).getValue()
                                    + indianSettlement.getAlarm(tp).getValue();
                                if (getAIRandom().nextInt(tension) > Tension.Level.HAPPY.getLimit()) {
                                    chosenOne.setMission(new IndianDemandMission(getAIMain(), chosenOne,
                                                                                 target));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Makes every unit perform their mission.
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
                    logger.log(Level.WARNING, "doMissions failed", e);
                }
            }
        }
    }

    /**
     * Evaluate a potential seek and destroy mission for a given unit
     * to a given tile.
     * TODO: revisit and rebalance the mass of magic numbers.
     *
     * @param unit The <code>Unit</code> to do the mission.
     * @param newTile The <code>Tile</code> to go to.
     * @param turns How long to travel to the tile.
     * @return A score for the proposed mission.
     */
    int getUnitSeekAndDestroyMissionValue(Unit unit, Tile newTile, int turns) {
        if (!isTargetValidForSeekAndDestroy(unit, newTile)) {
            return Integer.MIN_VALUE;
        }
        Settlement settlement = newTile.getSettlement();
        Unit defender = newTile.getDefendingUnit(unit);
        // Take distance to target into account
        int value = 10020 - turns * 100;

        if (settlement != null) {
            // Do not cheat and look inside the settlement.
            // Just use visible facts about it.
            // TODO: if we are the REF and there is a significant Tory
            // population inside, assume traitors have briefed us.
            if (settlement instanceof Colony) {
                // Favour high population and weak fortifications.
                Colony colony = (Colony) settlement;
                value += 50 * colony.getUnitCount();
                if (colony.hasStockade()) {
                    value -= 1000 * colony.getStockade().getLevel();
                }
            } else if (settlement instanceof IndianSettlement) {
                // Favour the most hostile settlements
                IndianSettlement is = (IndianSettlement) settlement;
                Tension tension = is.getAlarm(unit.getOwner());
                if (tension != null) value += tension.getValue();
            }
        } else if (defender != null) {
            CombatModel combatModel = unit.getGame().getCombatModel();
            float off = combatModel.getOffencePower(unit, defender);
            float def = combatModel.getDefencePower(unit, defender);

            if (defender.getType().getOffence() > 0) {
                value += 200 - def * 2 - turns * 50;
            }

            value += combatModel.getOffencePower(defender, unit)
                - combatModel.getDefencePower(defender, unit);

            if (!defender.isNaval()
                && defender.hasAbility("model.ability.expertSoldier")
                && !defender.isArmed()) {
                value += 10 - def * 2 - turns * 25;
            }
            if (value < 0) value = 0;
        }
        logger.finest("getUnitSeekAndDestroyMissionValue " + unit.getId()
                      + " v " + ((settlement != null) ? settlement.getId()
                                 : (defender != null) ? defender.getId()
                                 : "none")
                      + " = " + value);
        return value;
    }

    /**
     * Find out if a tile contains a suitable target for seek-and-destroy.
     * TODO: Package for access by a test only - necessary?
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param tile The <code>Tile</code> to attack into.
     * @return True if an attack can be launched.
     */
    public boolean isTargetValidForSeekAndDestroy(Unit attacker, Tile tile) {
        Player attackerPlayer = attacker.getOwner();

        // Insist the attacker exists.
        if (attacker == null) return false;

        // Determine the defending player.
        Settlement settlement = tile.getSettlement();
        Unit defender = tile.getDefendingUnit(attacker);
        Player defenderPlayer = (settlement != null) ? settlement.getOwner()
            : (defender != null) ? defender.getOwner()
            : null;

        // Insist there be a defending player.
        if (defenderPlayer == null) return false;

        // Can not attack our own units.
        if (attackerPlayer == defenderPlayer) return false;

        // If European, do not attack if not at war.
        // If native, do not attack if not at war and at least content.
        // Otherwise some attacks are allowed even if not at war.
        boolean atWar = attackerPlayer.atWarWith(defenderPlayer);
        if (attackerPlayer.isEuropean()) {
            if (!atWar) return false;
        } else if (attackerPlayer.isIndian()) {
            if (!atWar && attackerPlayer.getTension(defenderPlayer)
                .getLevel().compareTo(Tension.Level.CONTENT) <= 0) {
                return false;
            }
        }

        // A naval unit can never attack a land unit or settlement,
        // but a land unit *can* attack a naval unit if it is on land.
        // Otherwise naval units can only fight at sea, land units
        // only on land.
        if (attacker.isNaval()) {
            if (settlement != null
                || !defender.isNaval() || defender.getTile().isLand()) {
                return false;
            }
        } else {
            if (defender != null && !defender.getTile().isLand()) {
                return false;
            }
        }

        // Otherwise, attack.
        return true;
    }

    /**
     * Gives a military mission to the given unit.
     *
     * Old comment: Temporary method for giving a military mission.
     * This method will be removed when "MilitaryStrategy" and the
     * "Tactic"-classes has been implemented.
     *
     * @param aiUnit The <code>AIUnit</code> to give a mission to.
     */
    public void giveMilitaryMission(AIUnit aiUnit) {
        logger.finest("Entering giveMilitaryMission for: " + aiUnit.getUnit());
        final AIMain aiMain = getAIMain();
        Mission mission = new UnitWanderHostileMission(aiMain, aiUnit);
        aiUnit.setMission(mission);
        logger.finest("giveMilitaryMission found: " + mission
                      + " for unit: " + aiUnit);
    }

}
