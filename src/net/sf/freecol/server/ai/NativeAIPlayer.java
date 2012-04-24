/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTradeItem;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.GoldTradeItem;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsTradeItem;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StanceTradeItem;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitTradeItem;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.IdleAtSettlementMission;
import net.sf.freecol.server.ai.mission.IndianBringGiftMission;
import net.sf.freecol.server.ai.mission.IndianDemandMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.PioneeringMission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
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
     * The modifier to apply when a ship is trading.
     */
    private static final String SHIP_TRADE_PENALTY
        = "model.modifier.shipTradePenalty";

    /**
     * The modifier to apply when to trade with a settlement with a missionary
     * if the enhancedMissionaries option is enabled.
     */
    private static final String MISSIONARY_TRADE_BONUS
        = "model.modifier.missionaryTradeBonus";

    /**
     * Stores temporary information for sessions (trading with another player
     * etc).
     */
    private HashMap<String, Integer> sessionRegister
        = new HashMap<String, Integer>();


    /**
     * Creates a new <code>AIPlayer</code>.
     *
     * @param aiMain The main AI-class.
     * @param player The player that should be associated with this
     *            <code>AIPlayer</code>.
     */
    public NativeAIPlayer(AIMain aiMain, ServerPlayer player) {
        super(aiMain, player);

        uninitialized = getPlayer() == null;
    }

    /**
     * Creates a new <code>AIPlayer</code>.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public NativeAIPlayer(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain, in);

        uninitialized = getPlayer() == null;
    }


    // AIPlayer interface

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
        clearAIUnits();
    }

    /**
     * Evaluates a proposed mission type for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to perform the mission.
     * @param path A <code>PathNode</code> to the target of this mission.
     * @param type The mission type.
     * @return A score representing the desirability of this mission.
     */
    public int scoreMission(AIUnit aiUnit, PathNode path, Class type) {
        int value = super.scoreMission(aiUnit, path, type);
        if (type == DefendSettlementMission.class) {
            // Reduce value in proportion to the number of active defenders.
            Settlement settlement = (Settlement)DefendSettlementMission
                .extractTarget(aiUnit, path);
            value -= 75 * getSettlementDefenders(settlement);

        } else if (type == UnitSeekAndDestroyMission.class) {
            // Natives prefer to attack when DISPLEASED.
            Location target = UnitSeekAndDestroyMission
                .extractTarget(aiUnit, path);
            Player targetPlayer = (target instanceof Ownable)
                ? ((Ownable)target).getOwner()
                : null;
            IndianSettlement is = aiUnit.getUnit().getIndianSettlement();
            if (targetPlayer != null
                && is != null && is.getAlarm(targetPlayer) != null) {
                value += is.getAlarm(targetPlayer).getValue()
                    - Tension.Level.DISPLEASED.getLimit();
            }
        }

        return value;
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
     * Called after another <code>Player</code> sends a
     * <code>trade</code> message.
     *
     * @param goods The goods which we are going to offer
     */
    public void registerSellGoods(Goods goods) {
        String goldKey = "tradeGold#" + goods.getType().getId() + "#" + goods.getAmount()
            + "#" + goods.getLocation().getId();
        sessionRegister.put(goldKey, null);
    }

    /**
     * Gets the appropriate missionary trade bonuses.
     *
     * @param missionary The missionary <code>Unit</code>.
     * @param sense The sense to apply the modifiers.
     * @return The missionary trade bonuses.
     */
    private Set<Modifier> getMissionaryTradeBonuses(Unit missionary,
                                                    boolean sense) {
        Set<Modifier> missionaryBonuses = missionary
            .getModifierSet(MISSIONARY_TRADE_BONUS);
        Set<Modifier> result;
        if (sense) {
            result = missionaryBonuses;
        } else {
            result = new HashSet<Modifier>();
            for (Modifier m : missionaryBonuses) {
                result.add(new Modifier(m.getId(), m.getSource(),
                        -m.getValue(), m.getType()));
            }
        }
        return result;
    }

    /**
     * Gets the appropriate ship trade penalties.
     *
     * @param sense The sense to apply the modifiers.
     * @return The ship trade penalties.
     */
    private Set<Modifier> getShipTradePenalties(boolean sense) {
        Specification spec = getSpecification();
        List<Modifier> shipPenalties = spec.getModifiers(SHIP_TRADE_PENALTY);
        Set<Modifier> result = new HashSet<Modifier>();
        int penalty = spec.getInteger(GameOptions.SHIP_TRADE_PENALTY);
        for (Modifier m : shipPenalties) {
            result.add(new Modifier(m.getId(), m.getSource(),
                    ((sense) ? penalty : -penalty), m.getType()));
        }
        return result;
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
        Specification spec = getSpecification();
        IndianSettlement is = (IndianSettlement) settlement;
        Player buyer = unit.getOwner();
        String goldKey = "tradeGold#" + goods.getType().getId()
            + "#" + goods.getAmount() + "#" + settlement.getId();
        String hagglingKey = "tradeHaggling#" + unit.getId();
        int price;
        Integer registered = sessionRegister.get(goldKey);
        if (registered == null) {
            price = is.getPriceToSell(goods);
            switch (is.getAlarm(buyer).getLevel()) {
            case HAPPY: case CONTENT:
                break;
            case DISPLEASED:
                price *= 2;
                break;
            default:
                return NetworkConstants.NO_TRADE_HOSTILE;
            }
            Set<Modifier> modifiers = new HashSet<Modifier>();
            Unit missionary = is.getMissionary(buyer);
            if (missionary != null
                && spec.getBoolean(GameOptions.ENHANCED_MISSIONARIES)) {
                modifiers.addAll(getMissionaryTradeBonuses(missionary, false));
            }
            if (unit.isNaval()) {
                modifiers.addAll(getShipTradePenalties(false));
            }
            price = (int) FeatureContainer.applyModifierSet((float)price,
                getGame().getTurn(), modifiers);
            sessionRegister.put(goldKey, new Integer(price));
            return price;
        }
        price = registered.intValue();
        if (price < 0 || price == gold) return price;
        if (gold < (price * 9) / 10) {
            logger.warning("Cheating attempt: sending a offer too low");
            sessionRegister.put(goldKey, new Integer(-1));
            return NetworkConstants.NO_TRADE;
        }

        int haggling = 1;
        if (sessionRegister.containsKey(hagglingKey)) {
            haggling = sessionRegister.get(hagglingKey).intValue();
        }
        if (Utils.randomInt(logger, "Haggle-buy",
                getAIRandom(), 3 + haggling) >= 3) {
            sessionRegister.put(goldKey, new Integer(-1));
            return NetworkConstants.NO_TRADE_HAGGLE;
        }
        sessionRegister.put(goldKey, new Integer(gold));
        sessionRegister.put(hagglingKey, new Integer(haggling + 1));
        return gold;
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
     *         {@link NetworkConstants#NO_TRADE*}.
     */
    public int sellProposition(Unit unit, Settlement settlement, Goods goods, int gold) {
        logger.finest("Entering method sellProposition");
        Specification spec = getSpecification();
        IndianSettlement is = (IndianSettlement) settlement;
        Player seller = unit.getOwner();
        String goldKey = "tradeGold#" + goods.getType().getId()
            + "#" + goods.getAmount() + "#" + settlement.getId();
        String hagglingKey = "tradeHaggling#" + unit.getId();
        int price;
        if (sessionRegister.containsKey(goldKey)) {
            price = sessionRegister.get(goldKey).intValue();
        } else {
            price = is.getPriceToBuy(goods);
            switch (is.getAlarm(seller).getLevel()) {
            case HAPPY: case CONTENT:
                break;
            case DISPLEASED:
                price /= 2;
                break;
            case ANGRY:
                if (!goods.getType().isMilitaryGoods())
                    return NetworkConstants.NO_TRADE_HOSTILE;
                price /= 2;
                break;
            default:
                return NetworkConstants.NO_TRADE_HOSTILE;
            }
            Set<Modifier> modifiers = new HashSet<Modifier>();
            Unit missionary = is.getMissionary(seller);
            if (missionary != null
                && spec.getBoolean(GameOptions.ENHANCED_MISSIONARIES)) {
                modifiers.addAll(getMissionaryTradeBonuses(missionary, true));
            }
            if (unit.isNaval()) {
                modifiers.addAll(getShipTradePenalties(true));
            }
            price = (int) FeatureContainer.applyModifierSet((float)price,
                getGame().getTurn(), modifiers);
            if (price <= 0) return 0;
            sessionRegister.put(goldKey, new Integer(price));
        }
        if (gold < 0 || price == gold) return price;
        if (gold > (price * 11) / 10) {
            logger.warning("Cheating attempt: haggling with a request too high");
            sessionRegister.put(goldKey, new Integer(-1));
            return NetworkConstants.NO_TRADE;
        }
        int haggling = 1;
        if (sessionRegister.containsKey(hagglingKey)) {
            haggling = sessionRegister.get(hagglingKey).intValue();
        }
        if (Utils.randomInt(logger, "Haggle-sell",
                getAIRandom(), 3 + haggling) >= 3) {
            sessionRegister.put(goldKey, new Integer(-1));
            return NetworkConstants.NO_TRADE_HAGGLE;
        }
        sessionRegister.put(goldKey, new Integer(gold));
        sessionRegister.put(hagglingKey, new Integer(haggling + 1));
        return gold;
    }

/* Internal methods ***********************************************************/


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
            int n = Utils.randomInt(logger, "Secure",
                getAIRandom(), settlements.size());
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
     * Greedily equips braves with horses and muskets.
     *
     * @param is The <code>IndianSettlement</code> where the equipping occurs.
     */
    public void equipBraves(IndianSettlement is) {
        final Specification spec = getSpecification();
        List<Unit> units = is.getUnitList();
        roles: for (Role r : new Role[] { Role.DRAGOON, Role.SOLDIER,
                                          Role.SCOUT }) {
            List<EquipmentType> e = r.getRoleEquipment(spec);
            while (!units.isEmpty()) {
                Unit u = units.get(0);
                for (EquipmentType et : e) {
                    if (u.canBeEquippedWith(et)
                        && !is.canProvideEquipment(et)) {
                        continue roles;
                    }
                }
                if (u.getRole() != Role.DRAGOON && u.getRole() != r) {
                    getAIUnit(u).equipForRole(r, false);
                }
                units.remove(0);
            }
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
        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            workerWishes.put(unitType, new ArrayList<Wish>());
        }

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
                                || unit.hasAbility(Ability.EXPERT_PIONEER);
            boolean isExpert = unit.getSkillLevel() > 0;
            if (isPioneer && PioneeringMission.isValid(aiUnit)) {
                aiUnit.setMission(new PioneeringMission(getAIMain(), aiUnit));
                continue;
            }

            if (!aiUnit.hasMission()) {
                if (aiUnit.getUnit().isOffensiveUnit()) {
                    aiUnit.setMission(new UnitWanderHostileMission(getAIMain(), aiUnit));
                } else {
                    //non-offensive units should take shelter in a nearby colony,
                    //not try to be hostile
                    aiUnit.setMission(new IdleAtSettlementMission(getAIMain(), aiUnit));
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
            if (Utils.randomInt(logger, "BringGifts", getAIRandom(), 10) != 1) {
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
                Colony target = nearbyColonies.get(Utils.randomInt(logger,
                        "Choose colony", getAIRandom(), nearbyColonies.size()));
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
            if (Utils.randomInt(logger, "DemandTribute",
                    getAIRandom(), 10) != 1) {
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
                    tension = Utils.randomInt(logger, "Tension",
                        getAIRandom(), tension);
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
                                if (Utils.randomInt(logger, "Unhappy?",
                                        getAIRandom(), tension) > Tension.Level.HAPPY.getLimit()) {
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
