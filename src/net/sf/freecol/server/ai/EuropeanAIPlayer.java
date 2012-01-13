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
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTradeItem;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.GoldTradeItem;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsTradeItem;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Modifier;
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
import net.sf.freecol.common.util.Utils;
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
public class EuropeanAIPlayer extends AIPlayer {

    private static final Logger logger = Logger.getLogger(EuropeanAIPlayer.class.getName());

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
    public EuropeanAIPlayer(AIMain aiMain, ServerPlayer player) {
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
    public EuropeanAIPlayer(AIMain aiMain, Element element) {
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
    public EuropeanAIPlayer(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
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
        cheat();
        determineStances();
        rearrangeWorkersInColonies();
        abortInvalidAndOneTimeMissions();
        ensureColonyMissions();
        giveNormalMissions();
        bringGifts();
        demandTribute();
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
        ensureColonyMissions();
        clearAIUnits();
    }

    /**
     * Gets a list of all the player's tile improvement plans required by
     * the colonies.
     *
     * @return A list of tile improvements.
     * @see net.sf.freecol.common.model.TileImprovement
     */
    public List<TileImprovementPlan> getTileImprovementPlans() {
        List<TileImprovementPlan> tileImprovements
            = new ArrayList<TileImprovementPlan>();
        for (AIColony aic : getAIColonies()) {
            tileImprovements.addAll(aic.getTileImprovementPlans());
        }
        return tileImprovements;
    }
        
    /**
     * Remove a <code>TileImprovementPlan</code> from the relevant colony.
     */
    public void removeTileImprovementPlan(TileImprovementPlan plan) {
        for (AIColony aic : getAIColonies()) {
            if (aic.removeTileImprovementPlan(plan)) return;
        }
        logger.warning("Not found given TileImprovementPlan to remove");
    }

    /**
     * Asks the server to recruit a unit in Europe on behalf of the AIPlayer.
     *
     * TODO: Move this to a specialized Handler class (AIEurope?)
     * TODO: Give protected access?
     *
     * @param index The index of the unit to recruit in the recruitables list,
     *     (if not a valid index, recruit a random unit).
     * @return The new AIUnit created by this action or null on failure.
     */
    public AIUnit recruitAIUnitInEurope(int index) {
        AIUnit aiUnit = null;
        Europe europe = getPlayer().getEurope();
        int n = europe.getUnitCount();
        final String selectAbility = "model.ability.selectRecruit";
        int slot = (index >= 0 && index < Europe.RECRUIT_COUNT
            && getPlayer().hasAbility(selectAbility)) ? (index + 1) : 0;
        if (AIMessage.askEmigrate(getConnection(), slot)
            && europe.getUnitCount() == n+1) {
            aiUnit = getAIUnit(europe.getUnitList().get(n));
        }
        return aiUnit;
    }

    /**
     * Helper function for server communication - Ask the server
     * to train a unit in Europe on behalf of the AIGetPlayer().
     *
     * TODO: Move this to a specialized Handler class (AIEurope?)
     * TODO: Give protected access?
     *
     * @return the new AIUnit created by this action. May be null.
     */
    public AIUnit trainAIUnitInEurope(UnitType unitType) {
        if (unitType==null) {
            throw new IllegalArgumentException("Invalid UnitType.");
        }

        AIUnit aiUnit = null;
        Europe europe = getPlayer().getEurope();
        int n = europe.getUnitCount();

        if (AIMessage.askTrainUnitInEurope(getConnection(), unitType)
            && europe.getUnitCount() == n+1) {
            aiUnit = getAIUnit(europe.getUnitList().get(n));
        }
        return aiUnit;
    }

    /**
     * This is a temporary method which are used for forcing the
     * computer players into building more colonies. The method will
     * be removed after the proper code for deciding whether a colony
     * should be built or not has been implemented.
     *
     * @return <code>true</code> if the AI should build more colonies.
     */
    public boolean hasFewColonies() {
        if (!getPlayer().canBuildColonies()) return false;
        int numberOfColonies = 0;
        int numberOfWorkers = 0;
        for (Settlement settlement : getPlayer().getSettlements()) {
            numberOfColonies++;
            numberOfWorkers += settlement.getUnitCount();
        }

        boolean result = numberOfColonies <= 2
            || (numberOfColonies >= 3
                && numberOfWorkers / numberOfColonies > numberOfColonies - 2);
        logger.finest("hasFewColonies (" + numberOfColonies
            + " : " + numberOfWorkers + "): " + result);
        return result;
    }

    /**
     * Gets the wishes for all this player's colonies, sorted by the
     * {@link Wish#getValue value}.
     *
     * @return A list of wishes.
     */
    public List<Wish> getWishes() {
        List<Wish> wishes = new ArrayList<Wish>();
        for (AIColony aic : getAIColonies()) {
            wishes.addAll(aic.getWishes());
        }
        Collections.sort(wishes);
        return wishes;
    }

    /**
     * Selects the most useful founding father offered.
     *
     * @param foundingFathers The founding fathers on offer.
     * @return The founding father selected.
     */
    public FoundingFather selectFoundingFather(List<FoundingFather> foundingFathers) {
        // TODO: improve choice
        int age = getGame().getTurn().getAge();
        FoundingFather bestFather = null;
        int bestWeight = Integer.MIN_VALUE;
        for (FoundingFather father : foundingFathers) {
            if (father == null) continue;

            // For the moment, arbitrarily: always choose the one
            // offering custom houses.  Allowing the AI to build CH
            // early alleviates the complexity problem of handling all
            // TransportMissions correctly somewhat.
            if (father.hasAbility("model.ability.buildCustomHouse")) {
                bestFather = father;
                break;
            }

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
            // TODO: check whether we already have horses!
            return false;
        } else if (goodsType.isMilitaryGoods() ||
                   goodsType.isTradeGoods() ||
                   goodsType.isBuildingMaterial()) {
            if (getGame().getTurn().getAge() == 3) {
                // by this time, we should be able to produce
                // enough ourselves
                // TODO: check whether we have an armory, at least
                return false;
            } else {
                return true;
            }
        } else {
            int averageIncome = 0;
            int numberOfGoods = 0;
            // TODO: consider the amount of goods produced. If we
            // depend on shipping huge amounts of cheap goods, we
            // don't want these goods to be boycotted.
            List<GoodsType> goodsTypes = getAIMain().getGame().getSpecification().getGoodsTypeList();
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
     * @param unit The <code>Unit</code> making demands.
     * @param colony The <code>Colony</code> where demands are being made.
     * @param goods The <code>Goods</code> demanded.
     * @param gold The amount of gold demanded.
     * @return True if this player accepts the demand.
     */
    public boolean indianDemand(Unit unit, Colony colony,
                                Goods goods, int gold) {
        // TODO: make a better choice, check whether the colony is
        // well defended
        return !"conquest".equals(getAIAdvantage());
    }

    /**
     * Decides to accept an offer of mercenaries or not.
     * TODO: make a better choice.
     *
     * @return True if the mercenaries are accepted.
     */
    public boolean acceptMercenaries() {
        return getPlayer().isAtWar() || "conquest".equals(getAIAdvantage());
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
    // TODO: this obviously applies only to native players. Is there
    // an European equivalent?
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
                int bonus = (missionary.hasAbility(Ability.EXPERT_MISSIONARY)) ? 8
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
                if (Utils.randomInt(logger, "Buy gold", getAIRandom(),
                        3 + haggling) <= 3) {
                    sessionRegister.put(goldKey, new Integer(gold));
                    sessionRegister.put(hagglingKey, new Integer(haggling + 1));
                    return gold;
                } else {
                    sessionRegister.put(goldKey, new Integer(-1));
                    return NetworkConstants.NO_TRADE_HAGGLE;
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
        Colony colony = (Colony) settlement;
        Player otherPlayer = unit.getOwner();
        // don't pay for more than fits in the warehouse
        int amount = colony.getWarehouseCapacity() - colony.getGoodsCount(goods.getType());
        amount = Math.min(amount, goods.getAmount());
        // get a good price
        Tension.Level tensionLevel = getPlayer().getTension(otherPlayer).getLevel();
        int percentage = (9 - tensionLevel.ordinal()) * 10;
        // what we could get for the goods in Europe (minus taxes)
        int netProfits = ((100 - getPlayer().getTax())
                          * getPlayer().getMarket().getSalePrice(goods.getType(), amount)) / 100;
        int price = (netProfits * percentage) / 100;
        return price;

    }

/* Internal methods ***********************************************************/


    /**
     * Cheats for the AI.  Please try to centralize cheats here.
     *
     * TODO: Remove when the AI is good enough.
     */
    private void cheat() {
        logger.finest("Entering method cheat");
        Specification spec = getAIMain().getGame().getSpecification();
        Market market = getPlayer().getMarket();
        for (GoodsType goodsType : spec.getGoodsTypeList()) {
            if (market.getArrears(goodsType) > 0
                && Utils.randomInt(logger, "Cheat boycott",
                    getAIRandom(), 5) == 0) {
                market.setArrears(goodsType, 0);
                // Just remove one goods party modifier (we can not
                // currently identify which modifier applies to which
                // goods type, but that is not worth fixing for the
                // benefit of `temporary' cheat code).  If we do not
                // do this, AI colonies accumulate heaps of party
                // modifiers because of the cheat boycott removal.
                findOne: for (Colony c : getPlayer().getColonies()) {
                    FeatureContainer fc = c.getFeatureContainer();
                    for (Modifier m : fc.getModifiers()) {
                        if ("model.modifier.colonyGoodsParty".equals(m.getSource())) {
                            fc.removeModifier(m);
                            break findOne;
                        }
                    }
                }
            }
        }

        //TODO: This seems to buy units the AIPlayer can't possibly use (see BR#2566180)
        if (getAIMain().getFreeColServer().isSingleplayer()
            && getPlayer().getPlayerType() == PlayerType.COLONIAL) {
            Europe europe = getPlayer().getEurope();
            List<UnitType> unitTypes = spec.getUnitTypeList();

            if (Utils.randomInt(logger, "Cheat buy unit",
                    getAIRandom(), 10) == 1) {
                List<WorkerWish> workerWishes = new ArrayList<WorkerWish>();
                for (AIColony aic : getAIColonies()) {
                    workerWishes.addAll(aic.getWorkerWishes());
                }
                if (!workerWishes.isEmpty()) {
                    Collections.sort(workerWishes);
                    UnitType unitToTrain = workerWishes.get(0).getUnitType();
                    int unitPrice = europe.getUnitPrice(unitToTrain);
                    if (unitPrice >= 0) {
                        // add the necessary amount of money
                        getPlayer().modifyGold(unitPrice);
                        AIUnit aiUnit = trainAIUnitInEurope(unitToTrain);
                        if (aiUnit != null) {
                            Unit unit = aiUnit.getUnit();
                            if (unit != null && unit.isColonist()) {
                                // no need to equip artillery units with muskets or horses
                                // TODO: cleanup magic numbers 50 and 1
                                GoodsType muskets = spec.getGoodsType("model.goods.muskets");
                                GoodsType horses = spec.getGoodsType("model.goods.horses");
                                getPlayer().modifyGold(getPlayer().getMarket().getBidPrice(muskets, 50));
                                getPlayer().modifyGold(getPlayer().getMarket().getBidPrice(horses, 50));
                                
                                EquipmentType horsesEq = spec.getEquipmentType("model.equipment.horses");
                                EquipmentType musketsEq = spec.getEquipmentType("model.equipment.muskets");
                                AIMessage.askEquipUnit(getAIUnit(unit), horsesEq, 1);
                                AIMessage.askEquipUnit(getAIUnit(unit), musketsEq, 1);
                            }
                        }
                    }
                }
            }
            // TODO: better heuristics to determine which ship to buy
            if (Utils.randomInt(logger, "Cheat buy ship",
                    getAIRandom(), 40) == 21) {
                int total = 0;
                ArrayList<UnitType> navalUnits = new ArrayList<UnitType>();
                for (UnitType unitType : unitTypes) {
                    if (unitType.hasAbility(Ability.NAVAL_UNIT) && unitType.hasPrice()) {
                        navalUnits.add(unitType);
                        total += europe.getUnitPrice(unitType);
                    }
                }

                UnitType unitToPurchase = null;
                int r = Utils.randomInt(logger, "Cheat which ship",
                    getAIRandom(), total);
                total = 0;
                for (UnitType unitType : navalUnits) {
                    total += unitType.getPrice();
                    if (r < total) {
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
    private void ensureColonyMissions() {
        logger.finest("Entering method ensureColonyMissions");

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
     * Calls {@link AIColony#rearrangeWorkers} for every colony this player
     * owns.
     */
    private void rearrangeWorkersInColonies() {
        logger.finest("Entering method rearrangeWorkersInColonies");
        for (AIColony aic : getAIColonies()) aic.rearrangeWorkers();
    }

    /**
     * Takes the necessary actions to secure a european colony
     */
    /*
    private void secureColony(Colony colony) {
        GoodsType musketType = getAIMain().getGame().getSpecification().getGoodsType("model.goods.muskets");
        GoodsType horsesType = getAIMain().getGame().getSpecification().getGoodsType("model.goods.horses");
        final EquipmentType muskets = getAIMain().getGame().getSpecification().getEquipmentType("model.equipment.muskets");
        final EquipmentType horses = getAIMain().getGame().getSpecification().getEquipmentType("model.equipment.horses");

        Map map = getPlayer().getGame().getMap();
        int olddefenders = 0;
        int defenders = 0;
        int threat = 0;
        int worstThreat = 0;
        Location bestTarget = null;
        Iterator<Unit> ui = colony.getTile().getUnitIterator();
        while (ui.hasNext()) {
            if ((ui.next()).isDefensiveUnit()) {
                defenders++;
            }
        }
        Iterator<Position> positionIterator = map.getCircleIterator(colony.getTile().getPosition(), true, 5);
        while (positionIterator.hasNext()) {
            Tile t = map.getTile(positionIterator.next());
            if (t.getFirstUnit() != null) {
                if (t.getFirstUnit().getOwner() == getPlayer()) {
                    Iterator<Unit> uit = t.getUnitIterator();
                    while (uit.hasNext()) {
                        if (uit.next().isOffensiveUnit()) {
                            defenders++;
                        }
                    }
                } else {
                    int thisThreat = 0;
                    if (getPlayer().getTension(t.getFirstUnit().getOwner()).getValue() >= Tension.TENSION_ADD_MAJOR) {
                        Iterator<Unit> uit = t.getUnitIterator();
                        while (uit.hasNext()) {
                            if (uit.next().isOffensiveUnit()) {
                                thisThreat += 2;
                            }
                        }
                    } else if (getPlayer().getTension(t.getFirstUnit().getOwner()).getValue() >= Tension.TENSION_ADD_MINOR) {
                        Iterator<Unit> uit = t.getUnitIterator();
                        while (uit.hasNext()) {
                            if (uit.next().isOffensiveUnit()) {
                                thisThreat++;
                            }
                        }
                    }
                    threat += thisThreat;
                    if (thisThreat > worstThreat) {
                        if (t.getSettlement() != null) {
                            bestTarget = t.getSettlement();
                        } else {
                            bestTarget = t.getFirstUnit();
                        }
                        worstThreat = thisThreat;
                    }
                }
            }
        }
        olddefenders = defenders;
        if (colony.hasStockade()) {
            defenders += (defenders * (colony.getStockade().getLevel()) / 2);
        }
        if (threat > defenders) {
            // We're under attack! Man the stockade!
            ArrayList<Unit> recruits = new ArrayList<Unit>();
            ArrayList<Unit> others = new ArrayList<Unit>();
            int inColonyCount = 0;
            // Let's make some more soldiers, if we can.
            // First, find some people we can recruit.
            ui = colony.getUnitIterator();
            while (ui.hasNext()) {
                Unit u = (ui.next());
                if (u.isOffensiveUnit()) {
                    continue; // don't bother dealing with current
                    // soldiers at the moment
                }
                if (u.getLocation() != colony.getTile()) {
                    // If we are not on the tile we are in the colony.
                    inColonyCount++;
                }
                if (u.hasAbility(Ability.EXPERT_SOLDIER)) {
                    recruits.add(u);
                } else if (u.hasAbility(Ability.CAN_BE_EQUIPPED)) {
                    others.add(u);
                }
            }
            // ATTENTION: skill may be Integer.MIN_VALUE!
            Collections.sort(others, Unit.getSkillLevelComparator());
            recruits.addAll(others);
            // Don't overdo it - leave at least one person behind.
            int recruitCount = threat - defenders;
            if (recruitCount > recruits.size() - 1) {
                recruitCount = recruits.size() - 1;
            }
            if (recruitCount > inColonyCount - 1) {
                recruitCount = inColonyCount - 1;
            }
            // Actually go through and arm our people.
            boolean needMuskets = false;
            boolean needHorses = false;
            ui = recruits.iterator();
            while (ui.hasNext() && recruitCount > 0) {
                Unit u = (ui.next());
                if (!u.isArmed() && u.canBeEquippedWith(muskets)) {
                    recruitCount--;
                    Element equipUnitElement = Message.createNewRootElement("equipUnit");
                    equipUnitElement.setAttribute("unit", u.getId());
                    equipUnitElement.setAttribute("type", muskets.getId());
                    equipUnitElement.setAttribute("amount", "1");
                    u.equipWith(muskets);
                    sendAndWaitSafely(equipUnitElement);
                    Element putOutsideColonyElement = Message.createNewRootElement("putOutsideColony");
                    putOutsideColonyElement.setAttribute("unit", u.getId());
                    u.putOutsideColony();
                    sendAndWaitSafely(putOutsideColonyElement);
                    // Check if the unit can fortify before sending the order
                    if (u.checkSetState(UnitState.FORTIFYING)) {
                        Element changeStateElement = Message.createNewRootElement("changeState");
                        changeStateElement.setAttribute("unit", u.getId());
                        changeStateElement.setAttribute("state", UnitState.FORTIFYING.toString());
                        sendAndWaitSafely(changeStateElement);
                    }
                    olddefenders++;
                    if (!u.isMounted() && u.canBeEquippedWith(horses)) {
                        equipUnitElement = Message.createNewRootElement("equipUnit");
                        equipUnitElement.setAttribute("unit", u.getId());
                        equipUnitElement.setAttribute("type", horses.getId());
                        equipUnitElement.setAttribute("amount", "1");
                        sendAndWaitSafely(equipUnitElement);
                    } else {
                        needHorses = true;
                    }
                } else {
                    needMuskets = true;
                    break;
                }
            }
            AIColony ac = null;
            if (needMuskets || needHorses) {
                Iterator<AIColony> aIterator = getAIColonyIterator();
                while (aIterator.hasNext()) {
                    AIColony temp = aIterator.next();
                    if (temp != null && temp.getColony() == colony) {
                        ac = temp;
                        break;
                    }
                }
            }
            if (needMuskets && ac != null) {
                // Check and see if we have already made a GoodsWish for
                // here.
                Iterator<Wish> wishes = ac.getWishIterator();
                boolean made = false;
                while (wishes.hasNext()) {
                    Wish w = wishes.next();
                    if (!(w instanceof GoodsWish)) {
                        continue;
                    }
                    GoodsWish gw = (GoodsWish) w;
                    if (gw.getGoodsType() == musketType) {
                        made = true;
                    }
                }
                if (made == false) {
                    // Add a new GoodsWish onto the stack.
                    ac
                            .addGoodsWish(new GoodsWish(getAIMain(), colony, (threat - olddefenders) * 50,
                                    musketType));
                }
            }
            if (needHorses && ac != null) {
                // Check and see if we have already made a GoodsWish for
                // here.
                Iterator<Wish> wishes = ac.getWishIterator();
                boolean made = false;
                while (wishes.hasNext()) {
                    Wish w = wishes.next();
                    if (!(w instanceof GoodsWish)) {
                        continue;
                    }
                    GoodsWish gw = (GoodsWish) w;
                    if (gw.getGoodsType() == horsesType) {
                        made = true;
                    }
                }
                if (made == false) {
                    // Add a new GoodsWish onto the stack.
                    ac.addGoodsWish(new GoodsWish(getAIMain(), colony, (threat - defenders) * 50, horsesType));
                }
            }
            defenders = olddefenders;
            if (colony.hasStockade()) {
                defenders += (defenders * (colony.getStockade().getLevel()) / 2);
            }
        }
        if (defenders > (threat * 2)) {
            // We're so big and tough, we can go wipe out this threat.
            // Pick someone to go make it happen.
            Unit u = null;
            Iterator<Unit> uit = colony.getUnitIterator();
            while (uit.hasNext()) {
                Unit candidate = uit.next();
                if (candidate.isOffensiveUnit() && candidate.getState() == UnitState.FORTIFIED) {
                    u = candidate;
                    break;
                }
            }
            if (u != null) {
                u.setState(UnitState.ACTIVE);
                u.setLocation(colony.getTile());
                AIUnit newDefenderAI = (AIUnit) getAIMain().getAIObject(u);
                if (bestTarget != null) {
                    newDefenderAI.setMission(new UnitSeekAndDestroyMission(getAIMain(), newDefenderAI, bestTarget));
                } else {
                    newDefenderAI.setMission(new UnitWanderHostileMission(getAIMain(), newDefenderAI));
                }
            }
        }
    }
    */

    /**
     * Gives a mission to non-naval units.
     */
    protected void giveNormalMissions() {
        logger.finest("Entering method giveNormalMissions");

        // Create a datastructure for the worker wishes:
        java.util.Map<UnitType, ArrayList<Wish>> workerWishes =
            new HashMap<UnitType, ArrayList<Wish>>();
        for (UnitType unitType : getAIMain().getGame().getSpecification().getUnitTypeList()) {
            workerWishes.put(unitType, new ArrayList<Wish>());
        }
        for (Wish w : getWishes()) {
            if (w instanceof WorkerWish && w.getTransportable() == null) {
                workerWishes.get(((WorkerWish) w).getUnitType()).add(w);
            }
        }

        final boolean fewColonies = hasFewColonies();
        boolean isPioneerReq = PioneeringMission.getPlayerPioneers(this).size() == 0;
        Iterator<AIUnit> aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = aiUnitsIterator.next();
            if (aiUnit.hasMission()) continue;

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

            if (PrivateerMission.isValid(aiUnit)) {
                aiUnit.setMission(new PrivateerMission(getAIMain(), aiUnit));
                continue;
            }
            /* TODO: we need a mission for frigates and similar
             * units to patrol enemy shipping lanes
             */

            if (unit.isCarrier()) {
                aiUnit.setMission(new TransportMission(getAIMain(), aiUnit));
                continue;
            }

            if (unit.canCarryTreasure()) {
                aiUnit.setMission(new CashInTreasureTrainMission(getAIMain(), aiUnit));
                continue;
            }

            if (ScoutingMission.isValid(aiUnit)) {
                aiUnit.setMission(new ScoutingMission(getAIMain(), aiUnit));
                continue;
            }

            if (unit.isOffensiveUnit() || unit.isDefensiveUnit()){
                Player owner = unit.getOwner();
                boolean isPastStart = getGame().getTurn().getNumber() > 5
                    && !owner.getSettlements().isEmpty();

                if (!unit.isColonist() || isPastStart) {
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
            if ((isPioneer || (isPioneerReq && !isExpert)) && PioneeringMission.isValid(aiUnit)) {
                aiUnit.setMission(new PioneeringMission(getAIMain(), aiUnit));
                isPioneerReq = false;
                continue;
            }

            if (unit.isColonist()) {
                giveColonistMission(aiUnit, fewColonies, workerWishes);
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

    private void giveColonistMission(AIUnit aiUnit, boolean fewColonies,
                                     java.util.Map<UnitType, ArrayList<Wish>> workerWishes) {
        Unit unit = aiUnit.getUnit();
        /*
         * Motivated by (speed) performance: This map stores the
         * distance between the unit and the destination of a Wish:
         */
        HashMap<Location, Integer> distances = new HashMap<Location, Integer>(121);
        for (ArrayList<Wish> al : workerWishes.values()) {
            for (Wish w : al) {
                if (w.getDestination() != null
                    && !distances.containsKey(w.getDestination())) {
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
            // TODO: is this necessary? If so, use Iterator?
            WorkerWish ww = (WorkerWish) wishList.get(i);
            if (ww.getTransportable() != null) {
                wishList.remove(i);
                i--;
                continue;
            }
            int turns = getScaledTurns(distances, ww.getDestination());
            if (bestWish == null
                || ww.getValue() - (turns * 2) > bestWish.getValue() - (bestTurns * 2)) {
                bestWish = ww;
                bestTurns = turns;
            }
        }
        if (bestWish != null) {
            bestWish.setTransportable(aiUnit);
            aiUnit.setMission(new WishRealizationMission(getAIMain(), aiUnit, bestWish));
            return;
        }
        // Find a site for a new colony:
        Tile colonyTile = null;
        if (getPlayer().canBuildColonies() && unit.canBuildColony()) {
            colonyTile = BuildColonyMission.findColonyLocation(aiUnit.getUnit());
            if (colonyTile != null) {
                bestTurns = unit.getTurnsToReach(colonyTile);
            }
        }

        // Check if we can find a better site to work than a new colony:
        if (!fewColonies || colonyTile == null || bestTurns > 10) {
            for (List<Wish> wishes : workerWishes.values()) {
                for (int j = 0; j < wishes.size(); j++) {
                    WorkerWish ww = (WorkerWish) wishes.get(j);
                    if (ww.getTransportable() != null) {
                        wishes.remove(j);
                        j--;
                        continue;
                    }
                    int turns = getScaledTurns(distances, ww.getDestination());
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
            return;
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
                // Verify carrier mission
                AIUnit carrier = getAIUnit((Unit) aiUnit.getUnit()
                    .getLocation());
                Mission carrierMission = carrier.getMission();
                if (carrierMission == null) {
                    carrierMission = new TransportMission(getAIMain(), carrier);
                    carrier.setMission(carrierMission);
                } else if (!(carrierMission instanceof TransportMission)) {
                    throw new IllegalStateException("Carrier carrying unit not on a transport mission");
                }
                // Transport unit to carrier destination (is this what
                // is truly wanted?)
                ((TransportMission) carrierMission).addToTransportList(aiUnit);
            }
            return;
        }
    }

    private int getScaledTurns(java.util.Map<Location, Integer> distances, Location destination) {
        int turns = distances.get(destination);
        // TODO: what do these calcuations mean?
        if (turns == Integer.MAX_VALUE) {
            return (destination.getTile() == null) ? 5 : 10;
        } else {
            return Math.min(5, turns);
        }
    }

    /**
     * Brings gifts to nice players with nearby colonies.
     *
     * TODO: European players can also bring gifts! However,
     * this might be folded into a trade mission, since
     * European gifts are just a special case of trading.
     */
    private void bringGifts() {
        return;
    }

    /**
     * Demands goods from players with nearby colonies.
     *
     * TODO: European players can also demand tribute!
     */
    private void demandTribute() {
        return;
    }

    /**
     * Calls {@link AIColony#createAIGoods()} for every colony this player owns.
     */
    private void createAIGoodsInColonies() {
        logger.finest("Entering method createAIGoodsInColonies");
        for (AIColony aic : getAIColonies()) aic.createAIGoods();
    }

    /**
     * Counts the number of defenders in a colony.
     *
     * @param colony The <code>Colony</code> to examine.
     * @return The number of defenders.
     */
    protected int getColonyDefenders(Colony colony) {
        int defenders = 0;
        for (AIUnit au : getAIUnits()) {
            Mission m = au.getMission();
            if (m != null && m instanceof DefendSettlementMission
                && ((DefendSettlementMission) m).getSettlement() == colony
                && au.getUnit().getColony() == colony) {
                defenders++;
            }
        }
        return defenders;
    }

    /**
     * Evaluate allocating a unit to the defence of a colony.
     * Temporary helper method for giveMilitaryMission.
     *
     * @param unit The <code>Unit</code> that is to defend.
     * @param colony The <code>Colony</code> to defend.
     * @param turns The turns for the unit to reach the colony.
     * @return A value for such a mission.
     */
    public int getDefendColonyMissionValue(Unit unit, Colony colony,
                                           int turns) {
        // Sanitation
        if (unit == null || colony == null) return Integer.MIN_VALUE;

        int value = 10025 - 10 * turns;
        int defenders = getColonyDefenders(colony);

        // Reduce value in proportion to the number of defenders.
        value -= 500 * defenders;

        // Reduce value if defenders are protected.
        if (colony.hasStockade()) {
            if (defenders > colony.getStockade().getLevel() + 1) {
                value = 20 - defenders;
            } else {
                value -= 1000 * colony.getStockade().getLevel();
            }
        }

        // Do not return negative except for error cases.
        return Math.max(0, value);
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
    public int getUnitSeekAndDestroyMissionValue(Unit unit, Tile newTile, int turns) {
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

            Unit train = getBestTreasureTrain(newTile);
            if (train != null) {
                value += Math.min(train.getTreasureAmount() / 10, 50);
            }

            if (defender.getType().getOffence() > 0) {
                value += 200 - def * 2 - turns * 50;
            }

            value += combatModel.getOffencePower(defender, unit)
                - combatModel.getDefencePower(defender, unit);

            if (!defender.isNaval()
                && defender.hasAbility(Ability.EXPERT_SOLDIER)
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
     * Chooses the best target for an AI unit to attack.
     *
     * @param aiUnit The <code>AIUnit</code> that will attack.
     * @return The best target for a military mission.
     */
    public Ownable chooseMilitaryTarget(AIUnit aiUnit) {
        final AIMain aiMain = getAIMain();

        final Unit unit = aiUnit.getUnit();
        final Unit carrier = (!unit.isOnCarrier()) ? null
            : (Unit) unit.getLocation();
        Ownable bestTarget = null;         // The best target for a mission.
        int bestValue = Integer.MIN_VALUE; // The value of the target above.

        // Determine starting tile.
        Tile startTile = ((carrier != null) ? carrier : unit).getTile();
        if (startTile == null) {
            startTile = ((carrier != null) ? carrier : unit)
                .getFullEntryLocation();
        }
        if (startTile == null) {
            logger.warning("chooseMilitaryTarget failed, no tile for: "
                           + unit);
            return null;
        }

        // Check if the unit is located on a Tile with a
        // Settlement which requires defenders.
        if (unit.getColony() != null) {
            bestTarget = unit.getColony();
            bestValue = getDefendColonyMissionValue(unit,
                                                    (Colony) bestTarget, 0);
        }

        // Checks if a nearby colony requires additional defence:
        GoalDecider gd = new GoalDecider() {
                private PathNode best = null;

                private int bestValue = 0;

                public PathNode getGoal() {
                    return best;
                }

                public boolean hasSubGoals() {
                    return true;
                }

                private int getValue(PathNode pathNode) {
                    // Note: check *unit*, not *u*, which could be
                    // the carrier unit is on.
                    Colony colony = pathNode.getTile().getColony();
                    return (colony == null
                            || colony.getOwner() != unit.getOwner())
                        ? Integer.MIN_VALUE
                        : getDefendColonyMissionValue(unit, colony,
                                                      pathNode.getTurns());
                }

                public boolean check(Unit u, PathNode pathNode) {
                    int value = getValue(pathNode);
                    if (value > bestValue) {
                        bestValue = value;
                        best = pathNode;
                        return true;
                    }
                    return false;
                }
            };
        final int MAXIMUM_TURNS_TO_SETTLEMENT = 10;
        PathNode bestPath = unit.search(startTile, gd,
                                        CostDeciders.avoidIllegal(),
                                        MAXIMUM_TURNS_TO_SETTLEMENT,
                                        carrier);
        if (bestPath != null) {
            PathNode last = bestPath.getLastNode();
            Colony colony = last.getTile().getColony();
            int value = getDefendColonyMissionValue(unit, colony,
                                                    last.getTurns());
            if (value > bestValue) {
                bestTarget = colony;
                bestValue = value;
            }
        }

        // Search for the closest target of a pre-existing
        // UnitSeekAndDestroyMission.
        Location bestExistingTarget = null;
        int bestDistance = Integer.MAX_VALUE;
        for (AIUnit au : getAIUnits()) {
            Unit u = au.getUnit();
            if (u.getTile() == null
                || !(au.getMission() instanceof UnitSeekAndDestroyMission))
                continue;
            UnitSeekAndDestroyMission usdm
                = (UnitSeekAndDestroyMission) au.getMission();
            int distance = unit.getTurnsToReach(startTile,
                                                usdm.getTarget().getTile());
            if (distance < bestDistance) {
                bestExistingTarget = usdm.getTarget();
                bestDistance = distance;
            }
        }
        if (bestExistingTarget != null) {
            int value = getUnitSeekAndDestroyMissionValue(unit,
                                                          bestExistingTarget.getTile(), bestDistance);
            if (value > bestValue) {
                bestValue = value;
                bestTarget = (Ownable) bestExistingTarget;
            }
        }

        // General check for available targets for seek-and-destroy.
        GoalDecider targetDecider = new GoalDecider() {
                private PathNode bestTarget = null;

                private int bestNewTargetValue = Integer.MIN_VALUE;

                public PathNode getGoal() {
                    return bestTarget;
                }

                public boolean hasSubGoals() {
                    return true;
                }

                private int getValue(PathNode pathNode) {
                    // Note: check *unit*, not *u*, which could be
                    // the carrier unit is on.
                    Tile tile = pathNode.getTile();
                    return (isTargetValidForSeekAndDestroy(unit, tile))
                        ? getUnitSeekAndDestroyMissionValue(unit, tile,
                                                            pathNode.getTurns())
                        : Integer.MIN_VALUE;
                }

                public boolean check(Unit u, PathNode pathNode) {
                    int value = getValue(pathNode);
                    if (value > bestNewTargetValue) {
                        bestTarget = pathNode;
                        bestNewTargetValue = value;
                        return true;
                    }
                    return false;
                }
            };
        PathNode newTarget = unit.search(startTile, targetDecider,
                                         CostDeciders.avoidIllegal(),
                                         INFINITY, carrier);
        if (newTarget != null) {
            Tile targetTile = newTarget.getLastNode().getTile();
            int value = getUnitSeekAndDestroyMissionValue(unit,
                                                          targetTile, newTarget.getTotalTurns());
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
        return (bestValue > 0) ? bestTarget : null;
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
        final Player player = getPlayer();
        Mission mission;
        Ownable bestTarget = chooseMilitaryTarget(aiUnit);
        if (bestTarget == null) {
            // Just wander around if there is nothing better to do.
            mission = new UnitWanderHostileMission(aiMain, aiUnit);
        } else if (bestTarget instanceof Settlement
            && ((Settlement) bestTarget).getOwner() == player) {
            Settlement settlement = (Settlement) bestTarget;
            mission = new DefendSettlementMission(aiMain, aiUnit, settlement);
        } else {
            mission = new UnitSeekAndDestroyMission(aiMain, aiUnit,
                (Location) bestTarget);
        }

        aiUnit.setMission(mission);
        logger.finest("giveMilitaryMission found: " + mission
                      + " for unit: " + aiUnit);
    }

    private int getTurns(Transportable t, TransportMission tm) {
        if (t.getTransportDestination() != null
            && t.getTransportDestination().getTile()
            == tm.getUnit().getTile()) {
            return 0;
        }
        PathNode path = tm.getTransportPath(t);
        return (path == null) ? -1 : path.getTotalTurns();
    }

    /**
     * Assign transportable units and goods to available carriers
     * transport lists.
     */
    private void createTransportLists() {
        logger.finest("Entering method createTransportLists");
        if (!getPlayer().isEuropean()) return;
        List<Transportable> transportables = new ArrayList<Transportable>();

        // Collect non-naval units needing transport.
        for (AIUnit au : getAIUnits()) {
            if (!au.getUnit().isNaval()
                && au.getTransport() == null
                && au.getTransportDestination() != null) {
                transportables.add(au);
            }
        }

        // Add goods to transport
        for (AIColony aic : getAIColonies()) {
            for (AIGoods aig : aic.getAIGoods()) {
                if (aig.getTransportDestination() != null
                    && aig.getTransport() == null) {
                    transportables.add(aig);
                }
            }
        }

        // Update the priority.
        for (Transportable t : transportables) {
            t.increaseTransportPriority();
        }

        // Order the transportables by priority.
        Collections.sort(transportables, new Comparator<Transportable>() {
            public int compare(Transportable o1, Transportable o2) {
                if (o1 == o2) return 0;
                int result = o2.getTransportPriority() - o1.getTransportPriority();
                return (result == 0) ? o1.getId().compareTo(o2.getId())
                    : result;
            }
        });

        // Collect current transport missions with space available.
        ArrayList<TransportMission> availableMissions
            = new ArrayList<TransportMission>();
        for (AIUnit au : getAIUnits()) {
            if (au.hasMission() && au.getMission() instanceof TransportMission
                && au.getUnit().getSpaceLeft() > 0) {
                availableMissions.add((TransportMission) au.getMission());
            }
        }

        // If a transportable is already on a carrier, just add it to
        // its transport list.  Note however that it may not be
        // possible to complete such transport, in which case, the
        // carrier should dump the transportable in the nearest
        // colony.
        for (Transportable t : new ArrayList<Transportable>(transportables)) {
            Location transportableLoc = t.getTransportLocatable().getLocation();
            boolean onCarrier = transportableLoc instanceof Unit;
            if (onCarrier) {
                AIUnit carrierAI = getAIUnit((Unit) transportableLoc);
                Mission m = carrierAI.getMission();
                if (m instanceof TransportMission) {
                    ((TransportMission) m).addToTransportList(t);
                }
                transportables.remove(t);
            }
        }

        // For all remaining transportables, find the best carrier.
        // That is the one with space available that is closest.
        // TODO: this concentrates on packing, but ignores destinations
        // which is definitely going to be inefficient
        // TODO: be smarter about removing bestTransport from
        // availableMissions when it is full.
        while (!transportables.isEmpty() && !availableMissions.isEmpty()) {
            Transportable t = transportables.remove(0);
            TransportMission bestTransport = null;
            int bestTransportSpace = 0;
            int bestTransportTurns = Integer.MAX_VALUE;
            for (TransportMission tm : availableMissions) {
                int transportSpace = tm.getAvailableSpace(t);
                if (transportSpace <= 0) continue;
                if (t instanceof AIUnit && !tm.getUnit().canCarryUnits()) {
                    continue;
                }
                if (t.getTransportSource() != null
                    && (t.getTransportSource().getTile()
                        == tm.getUnit().getTile())) {
                    if (bestTransportTurns > 0
                        || (bestTransportTurns == 0
                            && transportSpace > bestTransportSpace)) {
                        bestTransport = tm;
                        bestTransportSpace = transportSpace;
                        bestTransportTurns = 0;
                    }
                    continue;
                }
                int totalTurns = getTurns(t, tm);
                if (totalTurns <= 0) continue;
                if (totalTurns < bestTransportTurns
                    || (totalTurns == bestTransportTurns
                        && transportSpace > bestTransportSpace)) {
                    bestTransport = tm;
                    bestTransportSpace = transportSpace;
                    bestTransportTurns = totalTurns;
                }
            }
            if (bestTransport == null) {
                logger.finest("Transport failed for: " + t);
                continue;
            }
            bestTransport.addToTransportList(t);
            logger.finest("Transport found for: " + t
                          + " using: " + bestTransport
                          + "  unit: " + bestTransport.getUnit());

            // See if any other transportables are present at the same
            // location and can fit.
            for (int i = 0; i < transportables.size(); i++) {
                Transportable t2 = transportables.get(i);
                if (t2.getTransportLocatable().getLocation()
                    == t.getTransportLocatable().getLocation()
                    && bestTransport.getAvailableSpace(t2) > 0) {
                    transportables.remove(t2);
                    bestTransport.addToTransportList(t2);
                    logger.finest("Transport piggyback for: " + t2
                                  + " using: " + t);
                }
            }
        }
    }

    /**
     * Finds the treasure train carrying the largest treasure located
     * on the given <code>Tile</code>.
     *
     * @param tile The <code>Tile</code> to check.
     * @return The best treasure train found or null if none present.
     */
    private Unit getBestTreasureTrain(Tile tile) {
        Unit best = null;
        for (Unit unit : tile.getUnitList()) {
            if (unit.canCarryTreasure()
                && (best == null
                    || best.getTreasureAmount() < unit.getTreasureAmount())) {
                best = unit;
            }
        }
        return best;
    }

}
