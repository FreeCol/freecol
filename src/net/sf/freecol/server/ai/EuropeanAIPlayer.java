/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTradeItem;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeContext;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoldTradeItem;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.NativeTrade;
import net.sf.freecol.common.model.NativeTrade.NativeTradeAction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StanceTradeItem;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.util.CachingFunction;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.RandomChoice;
import static net.sf.freecol.common.util.RandomUtils.*;

import net.sf.freecol.server.ai.GoodsWish;
import net.sf.freecol.server.ai.mission.BuildColonyMission;
import net.sf.freecol.server.ai.mission.CashInTreasureTrainMission;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.IdleAtSettlementMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.MissionaryMission;
import net.sf.freecol.server.ai.mission.PioneeringMission;
import net.sf.freecol.server.ai.mission.PrivateerMission;
import net.sf.freecol.server.ai.mission.ScoutingMission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.ai.mission.WishRealizationMission;
import net.sf.freecol.server.ai.mission.WorkInsideColonyMission;
import net.sf.freecol.server.ai.ValuedAIObject;
import net.sf.freecol.server.ai.WorkerWish;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * Objects of this class contains AI-information for a single European
 * {@link Player} and is used for controlling this player.
 *
 * The method {@link #startWorking} gets called by the
 * {@link AIInGameInputHandler} when it is this player's turn.
 */
public class EuropeanAIPlayer extends MissionAIPlayer {

    private static final Logger logger = Logger.getLogger(EuropeanAIPlayer.class.getName());

    /** Predicate to select units that can be equipped. */
    private static final Predicate<Unit> equipPred = u ->
        u.hasDefaultRole() && u.hasAbility(Ability.CAN_BE_EQUIPPED);

    /** Predicate to select party modifiers. */
    private static final Predicate<Modifier> partyPred
        = matchKey(Specification.COLONY_GOODS_PARTY_SOURCE,
                   Modifier::getSource);

    /** Maximum number of turns to travel to a building site. */
    private static final int buildingRange = 5;

    /** Maximum number of turns to travel to a cash in location. */
    private static final int cashInRange = 20;

    /** Maximum number of turns to travel to a missionary target. */
    private static final int missionaryRange = 20;

    /**
     * Maximum number of turns to travel to make progress on
     * pioneering.  This is low-ish because it is usually more
     * efficient to ship the tools where they are needed and either
     * create a new pioneer on site or send a hardy pioneer on
     * horseback.  The AI is probably smart enough to do the former
     * already, and one day the latter.
     */
    private static final int pioneeringRange = 10;

    /**
     * Maximum number of turns to travel to a privateering target.
     * Low number because of large naval moves.
     */
    private static final int privateerRange = 1;

    /** Maximum number of turns to travel to a scouting target. */
    private static final int scoutingRange = 20;

    /** A comparator to sort units by decreasing builder score. */
    private static final Comparator<AIUnit> builderComparator
        = Comparator.comparingInt(AIUnit::getBuilderScore).reversed();

    /**
     * A comparator to sort units by suitability for a PioneeringMission.
     *
     * We do not check if a unit is near to a colony that can provide tools,
     * as that is likely to be too expensive.  FIXME: perhaps we should.
     */
    public static final Comparator<AIUnit> pioneerComparator
        = Comparator.comparingInt(AIUnit::getPioneerScore).reversed();

    /**
     * A comparator to sort units by suitability for a ScoutingMission.
     *
     * We do not check if a unit is near to a colony that can provide horses,
     * as that is likely to be too expensive.  FIXME: perhaps we should.
     */
    public static final Comparator<AIUnit> scoutComparator
        = Comparator.comparingInt(AIUnit::getScoutScore).reversed();


    // These should be final, but need the spec.

    /** Cheat chances. */
    private static int liftBoycottCheatPercent;
    private static int equipScoutCheatPercent;
    private static int equipPioneerCheatPercent;
    private static int landUnitCheatPercent;
    private static int offensiveLandUnitCheatPercent;
    private static int offensiveNavalUnitCheatPercent;
    private static int transportNavalUnitCheatPercent;
    /** The pioneer role. */
    private static Role pioneerRole = null;
    /** The scouting role. */
    private static Role scoutRole = null;

    // Caches/internals.  Do not serialize.

    /**
     * A cached map of Tile to best TileImprovementPlan.
     * Used to choose a tile improvement for a pioneer to work on.
     */
    private final java.util.Map<Tile, TileImprovementPlan> tipMap
        = new HashMap<>();

    /**
     * A cached map of destination Location to Wishes awaiting transport.
     */
    private final java.util.Map<Location, List<Wish>> transportDemand
        = new HashMap<>();

    /** A cached list of transportables awaiting transport. */
    private final List<TransportableAIObject> transportSupply
        = new ArrayList<>();

    /**
     * A mapping of goods type to the goods wishes where a colony has
     * requested that goods type.  Used to retarget goods that have
     * gone astray.
     */
    private final java.util.Map<GoodsType, List<GoodsWish>> goodsWishes
        = new HashMap<>();

    /**
     * A mapping of unit type to the worker wishes for that type.
     * Used to allocate WishRealizationMissions for units.
     */
    private final java.util.Map<UnitType, List<WorkerWish>> workerWishes
        = new HashMap<>();

    /**
     * A mapping of contiguity number to number of wagons needed in
     * that landmass.
     */
    private final java.util.Map<Integer, Integer> wagonsNeeded
        = new HashMap<>();

    /** The colonies that start the turn badly defended. */
    private final List<AIColony> badlyDefended = new ArrayList<>();

    /**
     * Current estimate of the number of new
     * {@code BuildColonyMission}s to create.
     */
    private int nBuilders = 0;

    /**
     * Current estimate of the number of new
     * {@code PioneeringMission}s to create.
     */
    private int nPioneers = 0;

    /**
     * Current estimate of the number of new
     * {@code ScoutingMission}s to create.
     */
    private int nScouts = 0;

    /** Count of the number of transports needing a naval unit. */
    private int nNavalCarrier = 0;


    /**
     * Creates a new {@code EuropeanAIPlayer}.
     *
     * @param aiMain The main AI-class.
     * @param player The player that should be associated with this
     *            {@code AIPlayer}.
     */
    public EuropeanAIPlayer(AIMain aiMain, Player player) {
        super(aiMain, player);
    }

    /**
     * Creates a new {@code AIPlayer}.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public EuropeanAIPlayer(AIMain aiMain,
                            FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, xr);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAIObject(AIObject ao) {
        if (ao instanceof AIColony) {
            removeAIColony((AIColony)ao);
        } else {
            super.removeAIObject(ao);
        }
    }

    /**
     * Remove one of our colonies.
     *
     * @param aic The {@code AIColony} to remove.
     */
    private void removeAIColony(AIColony aic) {
        final Colony colony = aic.getColony();
        
        Set<TileImprovementPlan> tips = new HashSet<>();
        for (Tile t : colony.getOwnedTiles()) {
            TileImprovementPlan tip = tipMap.remove(t);
            if (tip != null) tips.add(tip);
        }

        for (AIGoods aig : aic.getExportGoods()) {
            if (Map.isSameLocation(aig.getLocation(), colony)) {
                aig.changeTransport(null);
                aig.dispose();
            }
        }

        transportDemand.remove(colony);

        Set<Wish> wishes = new HashSet<>(aic.getWishes());
        for (AIUnit aiu : getAIUnits()) {
            PioneeringMission pm = aiu.getMission(PioneeringMission.class);
            if (pm != null) {
                if (tips.contains(pm.getTileImprovementPlan())) {
                    logger.info(pm + " collapses with loss of " + colony);
                    aiu.changeMission(null);
                }
                continue;
            }
            WishRealizationMission
                wm = aiu.getMission(WishRealizationMission.class);
            if (wm != null) {
                if (wishes.contains(wm.getWish())) {
                    logger.info(wm + " collapses with loss of " + colony);
                    aiu.changeMission(null);
                }
                continue;
            }
        }
    }

    /**
     * Initialize the static fields that would be final but for
     * needing the specification.
     *
     * @param spec The {@code Specification} to initialize from.
     */
    private static synchronized void initializeFromSpecification(Specification spec) {
        if (pioneerRole != null) return;
        pioneerRole = spec.getRoleWithAbility(Ability.IMPROVE_TERRAIN, null);
        scoutRole = spec.getRoleWithAbility(Ability.SPEAK_WITH_CHIEF, null);
        liftBoycottCheatPercent
            = spec.getInteger(GameOptions.LIFT_BOYCOTT_CHEAT);
        equipScoutCheatPercent
            = spec.getInteger(GameOptions.EQUIP_SCOUT_CHEAT);
        equipPioneerCheatPercent
            = spec.getInteger(GameOptions.EQUIP_PIONEER_CHEAT);
        landUnitCheatPercent
            = spec.getInteger(GameOptions.LAND_UNIT_CHEAT);
        offensiveLandUnitCheatPercent
            = spec.getInteger(GameOptions.OFFENSIVE_LAND_UNIT_CHEAT);
        offensiveNavalUnitCheatPercent
            = spec.getInteger(GameOptions.OFFENSIVE_NAVAL_UNIT_CHEAT);
        transportNavalUnitCheatPercent
            = spec.getInteger(GameOptions.TRANSPORT_NAVAL_UNIT_CHEAT);
    }

    /**
     * Get the list of badly defended colonies.
     *
     * @return A list of {@code AIColony}s that were badly
     *     defended at the start of this turn.
     */
    protected List<AIColony> getBadlyDefended() {
        return badlyDefended;
    }

    /**
     * Simple initialization of AI missions given that we know the starting
     * conditions.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    private void initializeMissions(LogBuilder lb) {
        final AIMain aiMain = getAIMain();
        List<AIUnit> aiUnits = getAIUnits();
        lb.add("\n  Initialize ");
        
        // Find all the carriers with potential colony builders on board,
        // give them missions.
        final Map map = getGame().getMap();
        final int maxRange = map.getWidth() + map.getHeight();
        Location target;
        Mission m;
        TransportMission tm;
        for (AIUnit aiCarrier : aiUnits) {
            if (aiCarrier.hasMission()) continue;
            Unit carrier = aiCarrier.getUnit();
            if (!carrier.isNaval()) continue;
            target = null;
            for (Unit u : carrier.getUnitList()) {
                AIUnit aiu = aiMain.getAIUnit(u);
                for (int range = buildingRange; range < maxRange;
                     range += buildingRange) {
                    target = BuildColonyMission.findMissionTarget(aiu, range, false);
                    if (target != null) break;
                }
                if (target == null) {
                    throw new RuntimeException("Initial colony fail: " + u);
                }
                if ((m = getBuildColonyMission(aiu, target)) != null) {
                    lb.add(m, ", ");
                }
            }
            // Initialize the carrier mission after the cargo units
            // have a valid mission so that the transport list and
            // mission target do not break.
            tm = (TransportMission)getTransportMission(aiCarrier);
            if (tm != null) {
                lb.add(tm);
                for (Unit u : carrier.getUnitList()) {
                    AIUnit aiu = getAIMain().getAIUnit(u);
                    if (aiu == null) continue;
                    tm.queueTransportable(aiu, false, lb);
                }
            }
        }

        // Put in some backup missions.
        lb.mark();
        for (AIUnit aiu : aiUnits) {
            if (aiu.hasMission()) continue;
            if ((m = getSimpleMission(aiu)) != null) lb.add(m, ", ");
        }
        if (lb.grew("\n  Backup: ")) lb.shrink(", ");
    }

    /**
     * Cheat by adding gold to guarantee the player has a minimum amount.
     *
     * @param amount The minimum amount of gold required.
     * @param lb A {@code LogBuilder} to log to.
     */
    public void cheatGold(int amount, LogBuilder lb) {
        final Player player = getPlayer();
        int gold = player.getGold();
        if (gold < amount) {
            amount -= gold;
            player.modifyGold(amount);
            lb.add("added ", amount, " gold");
        }
        player.logCheat(amount + " gold");
    }

    /**
     * Cheats for the AI.  Please try to centralize cheats here.
     *
     * FIXME: Remove when the AI is good enough.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    private void cheat(LogBuilder lb) {
        final AIMain aiMain = getAIMain();
        if (!aiMain.getFreeColServer().getSinglePlayer()) return;

        final Player player = getPlayer();
        if (player.getPlayerType() != PlayerType.COLONIAL) return;
        lb.mark();

        final Specification spec = getSpecification();
        final Game game = getGame();
        final Market market = player.getMarket();
        final Europe europe = player.getEurope();
        final Random air = getAIRandom();
        final List<GoodsType> arrears = new ArrayList<>();
        if (market != null) {
            arrears.addAll(transform(spec.getGoodsTypeList(),
                                     gt -> market.getArrears(gt) > 0));
        }
        final int nCheats = arrears.size() + 6; // 6 cheats + arrears
        int[] randoms = randomInts(logger, "cheats", air, 100, nCheats);
        int cheatIndex = 0;

        for (GoodsType goodsType : arrears) {
            if (randoms[cheatIndex++] < liftBoycottCheatPercent) {
                market.setArrears(goodsType, 0);
                // Just remove one goods party modifier (we can not
                // currently identify which modifier applies to which
                // goods type, but that is not worth fixing for the
                // benefit of `temporary' cheat code).  If we do not
                // do this, AI colonies accumulate heaps of party
                // modifiers because of the cheat boycott removal.
                final CachingFunction<Colony, Modifier> partyModifierMapper
                    = new CachingFunction<>(c ->
                        first(transform(c.getModifiers(), partyPred)));
                Colony party = getRandomMember(logger, "end boycott",
                    transform(player.getColonies(),
                              isNotNull(c -> partyModifierMapper.apply(c))),
                    air);
                if (party != null) {
                    party.removeModifier(partyModifierMapper.apply(party));
                    lb.add("lift-boycott at ", party, ", ");
                    player.logCheat("lift boycott at " + party.getName());
                }
            }
        }
    
        if (!europe.isEmpty()
            && scoutsNeeded() > 0
            && randoms[cheatIndex++] < equipScoutCheatPercent) {
            for (Unit u : transform(europe.getUnits(), equipPred)) {
                try {
                    int g = europe.priceGoods(u.getGoodsDifference(scoutRole, 1));
                    cheatGold(g, lb);
                } catch (FreeColException fce) {
                    continue;
                }
                if (getAIUnit(u).equipForRole(spec.getRoleWithAbility(Ability.SPEAK_WITH_CHIEF, null))) {
                    lb.add(" to equip scout ", u, ", ");
                    player.logCheat("Equip scout " + u.toShortString());
                    break;
                }
            }
        }

        if (!europe.isEmpty()
            && pioneersNeeded() > 0
            && randoms[cheatIndex++] < equipPioneerCheatPercent) {
            for (Unit u : transform(europe.getUnits(), equipPred)) {
                try {
                    int g = europe.priceGoods(u.getGoodsDifference(pioneerRole, 1)); 
                    cheatGold(g, lb);
                } catch (FreeColException fce) {
                    continue;
                }
                if (getAIUnit(u).equipForRole(spec.getRoleWithAbility(Ability.IMPROVE_TERRAIN, null))) {
                    lb.add(" to equip pioneer ", u, ", ");
                    player.logCheat("Equip pioneer " + u.toShortString());
                    break;
                }
            }
        }

        if (randoms[cheatIndex++] < landUnitCheatPercent) {
            final Predicate<Entry<UnitType, List<WorkerWish>>> bestWishPred = e -> {
                UnitType ut = e.getKey();
                return ut != null && ut.isAvailableTo(player)
                    && europe.getUnitPrice(ut) != UNDEFINED
                    && any(e.getValue());
            };
            WorkerWish bestWish = maximize(transform(workerWishes.entrySet(),
                                                     bestWishPred,
                                                     e -> first(e.getValue()),
                                                     Collectors.toSet()),
                ValuedAIObject.ascendingValueComparator);
            int cost = (bestWish != null)
                ? europe.getUnitPrice(bestWish.getUnitType())
                : (player.getImmigration() < player.getImmigrationRequired() / 2)
                ? player.getEuropeanRecruitPrice()
                : INFINITY;
            if (cost != INFINITY) {
                cheatGold(cost, lb);
                AIUnit aiu;
                if (bestWish == null) {
                    if ((aiu = recruitAIUnitInEurope(-1)) != null) {
                        // let giveNormalMissions look after the mission
                        lb.add(" to recruit ", aiu.getUnit(), ", ");
                    }
                } else {
                    if ((aiu = trainAIUnitInEurope(bestWish.getUnitType())) != null) {
                        Mission m = getWishRealizationMission(aiu, bestWish);
                        if (m != null) {
                            lb.add(" to train for ", m, ", ");
                        } else {
                            lb.add(" to train ", aiu.getUnit(), ", ");
                        }
                    }
                }
                if (aiu != null) player.logCheat("Make " + aiu.getUnit());
            }
        }

        if (game.getTurn().getNumber() > 300
            && player.isAtWar()
            && randoms[cheatIndex++] < offensiveLandUnitCheatPercent) {
            // - collect enemies, prefer not to antagonize the strong or
            //   crush the weak
            List<Player> wars = transform(game.getLivePlayers(player),
                                          x -> player.atWarWith(x));
            List<Player> preferred = new ArrayList<>(wars.size());
            List<Player> enemies = new ArrayList<>(wars.size());
            for (Player p : wars) {
                enemies.add(p);
                double strength = getStrengthRatio(p);
                if (strength < 3.0/2.0 && strength > 2.0/3.0) {
                    preferred.add(p);
                }
            }
            if (!preferred.isEmpty()) {
                enemies.clear();
                enemies.addAll(preferred);
            }
            List<Colony> colonies = player.getColonyList();
            // Find a target to attack.
            Location target = null;
            // Few colonies?  Attack the weakest European port
            if (colonies.size() < 3) {
                final Comparator<Colony> targetScore
                    = cachingDoubleComparator(c -> {
                            double score = 100000.0 / c.getUnitCount();
                            Building stockade = c.getStockade();
                            return (stockade == null) ? 1.0
                                : score / (stockade.getLevel() + 1.5);
                        });
                target = maximize(flatten(enemies, Player::isEuropean,
                                          Player::getConnectedPorts),
                                  targetScore);
            }
            // Otherwise attack something near a weak colony
            if (target == null && !colonies.isEmpty()) {
                List<AIColony> bad = new ArrayList<>(getBadlyDefended());
                if (bad.isEmpty()) bad.addAll(getAIColonies());
                AIColony defend = getRandomMember(logger,
                    "AIColony to defend", bad, air);
                Tile center = defend.getColony().getTile();
                Tile t = game.getMap().searchCircle(center,
                    GoalDeciders.getEnemySettlementGoalDecider(enemies),
                    30);
                if (t != null) target = t.getSettlement();
            }
            if (target != null) {
                List<AbstractUnit> aMercs = new ArrayList<>();
                int aPrice = player.getMonarch().loadMercenaries(air, aMercs);
                if (aPrice > 0) {
                    List<Unit> mercs = ((ServerPlayer)player)
                        .createUnits(aMercs, europe, air);
                    for (Unit u : mercs) {
                        AIUnit aiu = getAIUnit(u);
                        if (aiu == null) continue; // Can not happen
                        player.logCheat("Enlist " + aiu.getUnit());
                        Mission m = getSeekAndDestroyMission(aiu, target);
                        if (m != null) {
                            lb.add("enlisted ", m, ", ");
                        } else {
                            lb.add("enlisted ", aiu.getUnit(), ", ");
                        }
                    }
                }
            }
        }
            
        // Always cheat a new armed ship if the navy is destroyed,
        // otherwise if the navy is below average the chance to cheat
        // is proportional to how badly below average.
        double naval = getNavalStrengthRatio();
        int nNaval = (player.getUnitCount(true) == 0) ? 100
            : (0.0f < naval && naval < 0.5f)
            ? (int)(naval * offensiveNavalUnitCheatPercent)
            : -1;
        final Function<UnitType, RandomChoice<UnitType>> mapper = ut ->
            new RandomChoice<>(ut, 100000 / europe.getUnitPrice(ut));
        if (randoms[cheatIndex++] < nNaval) {
            cheatUnit(transform(spec.getUnitTypeList(),
                                ut -> ut.hasAbility(Ability.NAVAL_UNIT)
                                    && ut.isAvailableTo(player)
                                    && ut.hasPrice()
                                    && ut.isOffensive(),
                                mapper), "offensive-naval", lb);
        }
        // Only cheat carriers if they have work to do.
        int nCarrier = (nNavalCarrier > 0) ? transportNavalUnitCheatPercent
            : -1;
        if (randoms[cheatIndex++] < nCarrier) {
            cheatUnit(transform(spec.getUnitTypeList(),
                                ut -> ut.hasAbility(Ability.NAVAL_UNIT)
                                    && ut.isAvailableTo(player)
                                    && ut.hasPrice()
                                    && ut.getSpace() > 0,
                                mapper), "transport-naval", lb);
        }

        if (lb.grew("\n  Cheats: ")) lb.shrink(", ");
    }

    /**
     * Cheat-build a unit in Europe.
     *
     * @param rc A list of random choices to choose from.
     * @param what A description of the unit.
     * @param lb A {@code LogBuilder} to log to.
     * @return The {@code AIUnit} built.
     */
    private AIUnit cheatUnit(List<RandomChoice<UnitType>> rc, String what,
                             LogBuilder lb) {
        UnitType unitToPurchase
            = RandomChoice.getWeightedRandom(logger, "Cheat which unit",
                                             rc, getAIRandom());
        return (unitToPurchase == null) ? null
            : cheatUnit(unitToPurchase, what, lb);
    }

    /**
     * Cheat-build a unit in Europe.
     *
     * @param unitType The {@code UnitType} to build.
     * @param what A description of the unit.
     * @param lb A {@code LogBuilder} to log to.
     * @return The {@code AIUnit} built.
     */
    private AIUnit cheatUnit(UnitType unitType, String what, LogBuilder lb) {
        final Player player = getPlayer();
        final Europe europe = player.getEurope();
        int cost = europe.getUnitPrice(unitType);
        cheatGold(cost, lb);
        AIUnit result = trainAIUnitInEurope(unitType);
        lb.add(" to build ", what, " ", unitType.getSuffix(),
            ((result != null) ? "" : "(failed)"), ", ");
        if (result == null) return null;
        player.logCheat("Build " + result.getUnit());
        return result;
    }

    /**
     * Assign transportable units and goods to available carriers.
     *
     * These supply driven assignments supplement the demand driven
     * calls inside TransportMission.
     *
     * @param transportables A list of {@code TransportableAIObject}s to
     *     allocated transport for.
     * @param missions A list of {@code TransportMission}s to potentially
     *     assign more transportables to.
     * @param lb A {@code LogBuilder} to log to.
     */
    public void allocateTransportables(List<TransportableAIObject> transportables,
                                       List<TransportMission> missions,
                                       LogBuilder lb) {
        if (transportables.isEmpty()) return;
        if (missions.isEmpty()) return;

        lb.add("\n  Allocate Transport cargo=", transportables.size(),
               " carriers=", missions.size());
        //for (TransportableAIObject t : urgent) lb.add(" ", t);
        //lb.add(" ->");
        //for (Mission m : missions) lb.add(" ", m);

        LogBuilder lb2 = new LogBuilder(0);
        TransportMission best;
        float bestValue;
        boolean present;
        int i = 0;
        outer: while (i < transportables.size()) {
            if (missions.isEmpty()) break;
            TransportableAIObject t = transportables.get(i);
            lb.add(" for ", t);
            best = null;
            bestValue = 0.0f;
            present = false;
            for (TransportMission tm : missions) {
                if (!tm.spaceAvailable(t)) continue;
                Cargo cargo = tm.makeCargo(t, lb2);
                if (cargo == null) { // Serious problem with this cargo
                    transportables.remove(i);
                    continue outer;
                }
                int turns = cargo.getTurns();
                float value;
                if (turns == 0) {
                    value = tm.destinationCapacity();
                    if (!present) bestValue = 0.0f; // reset
                    present = true;
                } else {
                    value = (present) ? -1.0f
                        : (float)t.getTransportPriority() / turns;
                }
                if (bestValue < value) {
                    bestValue = value;
                    best = tm;
                }
            }
            if (best == null) {
                lb.add(" nothing found");
            } else {
                lb.add(" ", best.getUnit(), " chosen");
                if (best.queueTransportable(t, false, lb)) {
                    claimTransportable(t);
                    if (best.destinationCapacity() <= 0) {
                        missions.remove(best);
                    }
                } else {
                    missions.remove(best);
                }
            }
            i++;
        }
    }

    /**
     * Brings gifts to nice players with nearby colonies.
     *
     * FIXME: European players can also bring gifts!  However, this
     * might be folded into a trade mission, since European gifts are
     * just a special case of trading.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    private void bringGifts(@SuppressWarnings("unused") LogBuilder lb) {
        return;
    }

    /**
     * Demands goods from players with nearby colonies.
     *
     * FIXME: European players can also demand tribute!
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    private void demandTribute(@SuppressWarnings("unused") LogBuilder lb) {
        return;
    }


    // Tile Improvement handling

    /**
     * Rebuilds a map of locations to TileImprovementPlans.
     *
     * Called by startWorking at the start of every turn.
     *
     * Public for the test suite.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    public void buildTipMap(LogBuilder lb) {
        tipMap.clear();
        for (AIColony aic : getAIColonies()) {
            for (TileImprovementPlan tip : aic.getTileImprovementPlans()) {
                if (tip == null || tip.isComplete()) {
                    aic.removeTileImprovementPlan(tip);
                } else if (tip.getPioneer() != null) {
                    // Do nothing, remove when complete
                } else if (!tip.validate()) {
                    aic.removeTileImprovementPlan(tip);
                    tip.dispose();
                } else if (tip.getTarget() == null) {
                    logger.warning("No target for tip: " + tip);
                } else {
                    TileImprovementPlan other = tipMap.get(tip.getTarget());
                    if (other == null || other.getValue() < tip.getValue()) {
                        tipMap.put(tip.getTarget(), tip);
                    }
                }
            }
        }
        if (!tipMap.isEmpty()) {
            lb.add("\n  Improvements:");
            forEachMapEntry(tipMap, e -> {
                    Tile t = e.getKey();
                    TileImprovementPlan tip = e.getValue();
                    AIUnit pioneer = tip.getPioneer();
                    lb.add(" ", t, "=", tip.getType().getSuffix());
                    if (pioneer != null) lb.add("/", pioneer.getUnit());
                });
        }                
    }

    /**
     * Update the tip map with tips from a new colony.
     *
     * @param aic The new {@code AIColony}.
     */
    private void updateTipMap(AIColony aic) {
        for (TileImprovementPlan tip : aic.getTileImprovementPlans()) {
            tipMap.put(tip.getTarget(), tip);
        }
    }

    /**
     * Gets the best plan for a tile from the tipMap.
     *
     * @param tile The {@code Tile} to lookup.
     * @return The best plan for a tile.
     */
    public TileImprovementPlan getBestPlan(Tile tile) {
        return (tipMap == null) ? null : tipMap.get(tile);
    }

    /**
     * Gets the best plan for a colony from the tipMap.
     *
     * @param colony The {@code Colony} to check.
     * @return The tile with the best plan for a colony, or null if none found.
     */
    public Tile getBestPlanTile(Colony colony) {
        final Comparator<TileImprovementPlan> valueComp
            = Comparator.comparingInt(TileImprovementPlan::getValue);
        final Function<Tile, TileImprovementPlan> tileMapper = t ->
            tipMap.get(t);
        TileImprovementPlan best
            = maximize(map(colony.getOwnedTiles(), tileMapper),
                       isNotNull(), valueComp);
        return (best == null) ? null : best.getTarget();
    }

    /**
     * Remove a {@code TileImprovementPlan} from the relevant colony.
     *
     * @param plan The {@code TileImprovementPlan} to remove.
     */
    public void removeTileImprovementPlan(TileImprovementPlan plan) {
        if (plan == null) return;
        if (plan.getTarget() != null) tipMap.remove(plan.getTarget());
        for (AIColony aic : getAIColonies()) {
            if (aic.removeTileImprovementPlan(plan)) break;
        }
    }


    // Transport handling

    /**
     * Update the transport of a unit following a target change.
     *
     * If the target has changed
     * - drop all non-boarded transport unless the target is the same
     * - dump boarded transport with no target
     * - requeue all boarded transport unless the target is the same
     *
     * @param aiu The {@code AIUnit} to check.
     * @param oldTarget The old target {@code Location}.
     * @param lb A {@code LogBuilder} to log to.
     */
    public void updateTransport(AIUnit aiu, Location oldTarget, LogBuilder lb) {
        final AIUnit aiCarrier = aiu.getTransport();
        final Mission newMission = aiu.getMission();
        final Location newTarget = (newMission == null) ? null
            : newMission.getTarget();
        TransportMission tm;
        if (aiCarrier != null
            && (tm = aiCarrier.getMission(TransportMission.class)) != null
            && !Map.isSameLocation(oldTarget, newTarget)) {
            if (aiu.getUnit().getLocation() != aiCarrier.getUnit()) {
                lb.add(", drop transport ", aiCarrier.getUnit());
                aiu.dropTransport();
            } else if (newTarget == null) {
                tm.dumpTransportable(aiu, lb);
            } else {
                tm.requeueTransportable(aiu, lb);
            }
        }
    }

    /**
     * Checks if a transportable needs transport.
     *
     * @param t The {@code TransportableAIObject} to check.
     * @return True if no transport is already present or the
     *     transportable is already aboard a carrier, and there is a
     *     well defined source and destination location.
     */
    private boolean requestsTransport(TransportableAIObject t) {
        return t.getTransport() == null
            && t.getTransportDestination() != null
            && t.getTransportSource() != null
            && !(t.getLocation() instanceof Unit);
    }

    /**
     * Checks that the carrier assigned to a transportable is has a
     * transport mission and the transport is queued thereon.
     *
     * @param t The {@code TransportableAIObject} to check.
     * @return True if all is well.
     */
    private boolean checkTransport(TransportableAIObject t) {
        AIUnit aiCarrier = t.getTransport();
        if (aiCarrier == null) return false;
        TransportMission tm = aiCarrier.getMission(TransportMission.class);
        if (tm != null && tm.isTransporting(t)) return true;
        t.changeTransport(null);
        return false;
    }

    /**
     * Changes the needed wagons map for a specified tile/contiguity.
     * If the change is zero, that is a special flag that a connected
     * port is available, and thus that the map should be initialized
     * for that contiguity.
     *
     * @param tile The {@code Tile} to derive the contiguity from.
     * @param amount The change to make.
     */
    private void changeNeedWagon(Tile tile, int amount) {
        if (tile == null) return;
        int contig = tile.getContiguity();
        if (contig > 0) {
            Integer i = wagonsNeeded.get(contig);
            if (i == null) {
                if (amount == 0) wagonsNeeded.put(contig, 0);
            } else {
                wagonsNeeded.put(contig, i + amount);
            }
        }
    }

    /**
     * Rebuild the transport maps.
     * Count the number of transports requiring naval/land carriers.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    private void buildTransportMaps(LogBuilder lb) {
        transportDemand.clear();
        transportSupply.clear();
        wagonsNeeded.clear();
        nNavalCarrier = 0;

        // Prime the wagonsNeeded map with contiguities with a connected port
        for (AIColony aic : getAIColonies()) {
            Colony colony = aic.getColony();
            if (colony.isConnectedPort()) changeNeedWagon(colony.getTile(), 0);
        }

        for (AIUnit aiu : getAIUnits()) {
            if (aiu.hasMission() && !aiu.getMission().isValid()) continue;
            Unit u = aiu.getUnit();
            if (u.isCarrier()) {
                if (u.isNaval()) {
                    nNavalCarrier--;
                } else {
                    changeNeedWagon(u.getTile(), -1);
                }
            } else {
                checkTransport(aiu);
                if (requestsTransport(aiu)) {
                    transportSupply.add(aiu);
                    aiu.incrementTransportPriority();
                    nNavalCarrier++;
                }
            }
        }

        for (AIColony aic : getAIColonies()) {
            for (AIGoods aig : aic.getExportGoods()) {
                checkTransport(aig);
                if (requestsTransport(aig)) {
                    transportSupply.add(aig);
                    aig.incrementTransportPriority();
                    Location src = aig.getTransportSource();
                    Location dst = aig.getTransportDestination();
                    if (!Map.isSameContiguity(src, dst)) {
                        nNavalCarrier++;
                    }
                }
            }
            Colony colony = aic.getColony();
            if (!colony.isConnectedPort()) {
                changeNeedWagon(colony.getTile(), 1);
            }
        }

        for (Wish w : getWishes()) {
            TransportableAIObject t = w.getTransportable();
            if (t != null && t.getTransport() == null
                && t.getTransportDestination() != null) {
                Location loc = Location.upLoc(t.getTransportDestination());
                appendToMapList(transportDemand, loc, w);
            }
        }

        if (!transportSupply.isEmpty()) {
            lb.add("\n  Transport Supply:");
            for (TransportableAIObject t : transportSupply) {
                lb.add(" ", t.getTransportPriority(), "+", t);
            }
        }
        if (!transportDemand.isEmpty()) {
            lb.add("\n  Transport Demand:");
            forEachMapEntry(transportDemand, e -> {
                    Location ld = e.getKey();
                    lb.add("\n    ", ld, "[");
                    for (Wish w : e.getValue()) lb.add(" ", w);
                    lb.add(" ]");
                });
        }
    }

    /**
     * Gets the most urgent transportables.
     *
     * @return The most urgent 10% of the available transportables.
     */
    public List<TransportableAIObject> getUrgentTransportables() {
        List<TransportableAIObject> urgent
            = sort(transportSupply, ValuedAIObject.descendingValueComparator);
        // Do not let the list exceed 10% of all transports
        int urge = urgent.size();
        urge = Math.max(2, (urge + 5) / 10);
        while (urgent.size() > urge) urgent.remove(urge);
        return urgent;
    }

    /**
     * Allows a TransportMission to signal that it has taken responsibility
     * for a TransportableAIObject.
     *
     * @param t The {@code TransportableAIObject} being claimed.
     * @return True if the transportable was claimed from the supply map.
     */
    public boolean claimTransportable(TransportableAIObject t) {
        return transportSupply.remove(t);
    }

    /**
     * Rearrange colonies.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    private void rearrangeColonies(LogBuilder lb) {
        for (AIColony aic : getAIColonies()) aic.rearrangeColony(lb);
    }


    // Wish handling

    /**
     * Suppress European trade in a goods type.  A goods party and
     * boycott is incoming.
     *
     * @param type The {@code GoodsType} to suppress.
     * @param lb A {@code LogBuilder} to log to.
     */
    private void suppressEuropeanTrade(GoodsType type, LogBuilder lb) {
        final Player player = getPlayer();
        final Europe europe = player.getEurope();

        lb.add("  Suppressing trade in ", type.getSuffix());
        List<Unit> units = new ArrayList<>(europe.getUnitList());
        units.addAll(player.getHighSeas().getUnitList());
        for (Unit u : units) {
            int amount;
            AIUnit aiu;
            if (u.isCarrier() && (amount = u.getGoodsCount(type)) > 0
                && (aiu = getAIUnit(u)) != null
                && AIMessage.askUnloadGoods(type, amount, aiu)) {
                lb.add(", ", u, " sold ", amount);
            }
        }
        for (AIUnit aiu : getAIUnits()) {
            TransportMission tm = aiu.getMission(TransportMission.class);
            if (tm != null) tm.suppressEuropeanTrade(type, lb);
        }           

        int n = 0;
        List<GoodsWish> wishes = goodsWishes.get(type);
        if (wishes != null) {
            for (GoodsWish gw : wishes) {
                if (gw.getGoodsType() == type
                    && gw.getDestination() == europe) {
                    if (gw.getTransportable() instanceof AIGoods) {
                        AIGoods aig = (AIGoods)gw.getTransportable();
                        consumeGoodsWish(aig, gw);
                        aig.setTransportDestination(null);
                    }
                    gw.dispose();
                    n++;
                }
            }
            if (n > 0) lb.add(", dropped ", n, " goods wishes");
        }
        lb.add(".");
    }
                
    /**
     * Gets a list of the wishes at a given location for a unit type.
     *
     * @param loc The {@code Location} to look for wishes at.
     * @param type The {@code UnitType} to look for.
     * @return A list of {@code WorkerWish}es.
     */
    public List<WorkerWish> getWorkerWishesAt(Location loc, UnitType type) {
        List<Wish> demand = transportDemand.get(Location.upLoc(loc));
        return (demand == null) ? Collections.<WorkerWish>emptyList()
            : transform(demand,
                        w -> w instanceof WorkerWish
                            && ((WorkerWish)w).getUnitType() == type,
                        w -> (WorkerWish)w);
    }

    /**
     * Gets a list of the wishes at a given location for a goods type.
     *
     * @param loc The {@code Location} to look for wishes at.
     * @param type The {@code GoodsType} to look for.
     * @return A list of {@code GoodsWish}es.
     */
    public List<GoodsWish> getGoodsWishesAt(Location loc, GoodsType type) {
        List<Wish> demand = transportDemand.get(Location.upLoc(loc));
        return (demand == null) ? Collections.<GoodsWish>emptyList()
            : transform(demand,
                        w -> w instanceof GoodsWish
                            && ((GoodsWish)w).getGoodsType() == type,
                        w -> (GoodsWish)w);
    }

    /**
     * Gets the best worker wish for a carrier unit.
     *
     * @param aiUnit The carrier {@code AIUnit}.
     * @param unitType The {@code UnitType} to find a wish for.
     * @return The best worker wish for the unit.
     */
    private WorkerWish getBestWorkerWish(AIUnit aiUnit, UnitType unitType) {
        List<WorkerWish> wishes = workerWishes.get(unitType);
        if (wishes == null) return null;

        final Unit carrier = aiUnit.getUnit();
        WorkerWish carried = null;
        WorkerWish other = null;
        double bestCarriedValue = -1.0, bestOtherValue = -1.0;
        for (WorkerWish w : wishes) {
            Location dest = w.getDestination();
            if (dest == null) continue; // Defend against crash
            int turns = carrier.getTurnsToReach(dest);
            if (turns < Unit.MANY_TURNS) {
                if (bestCarriedValue < (double)w.getValue() / turns) {
                    bestCarriedValue = (double)w.getValue() / turns;
                    carried = w;
                }
            } else {
                if (bestOtherValue < w.getValue()) {
                    bestOtherValue = w.getValue();
                    other = w;
                }
            }
        }
        return (carried != null) ? carried : (other != null) ? other : null;
    }

    /**
     * Gets the best goods wish for a carrier unit.
     *
     * @param aiUnit The carrier {@code AIUnit}.
     * @param goodsType The {@code GoodsType} to wish for.
     * @return The best {@code GoodsWish} for the unit.
     */
    public GoodsWish getBestGoodsWish(AIUnit aiUnit, GoodsType goodsType) {
        final Unit carrier = aiUnit.getUnit();
        final ToDoubleFunction<GoodsWish> wishValue
            = cacheDouble(gw -> {
                    int turns = carrier.getTurnsToReach(carrier.getLocation(),
                                                        gw.getDestination());
                    return (turns >= Unit.MANY_TURNS) ? -1.0
                        : (double)gw.getValue() / turns;
                });
        final Comparator<GoodsWish> comp
            = Comparator.comparingDouble(wishValue);

        List<GoodsWish> wishes = goodsWishes.get(goodsType);
        return (wishes == null) ? null
            : maximize(wishes, gw -> wishValue.applyAsDouble(gw) > 0.0, comp);
    }

    /**
     * Rebuilds the goods and worker wishes maps.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    private void buildWishMaps(LogBuilder lb) {
        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            List<WorkerWish> wl = workerWishes.get(unitType);
            if (wl == null) {
                workerWishes.put(unitType, new ArrayList<WorkerWish>());
            } else {
                wl.clear();
            }
        }
        for (GoodsType goodsType : getSpecification().getStorableGoodsTypeList()) {
            List<GoodsWish> gl = goodsWishes.get(goodsType);
            if (gl == null) {
                goodsWishes.put(goodsType, new ArrayList<GoodsWish>());
            } else {
                gl.clear();
            }
        }

        for (Wish w : getWishes()) {
            if (w instanceof WorkerWish) {
                WorkerWish ww = (WorkerWish)w;
                if (ww.getTransportable() == null) {
                    appendToMapList(workerWishes, ww.getUnitType(), ww);
                }
            } else if (w instanceof GoodsWish) {
                GoodsWish gw = (GoodsWish)w;
                if (gw.getDestination() instanceof Colony) {
                    appendToMapList(goodsWishes, gw.getGoodsType(), gw);
                }
            }
        }

        if (!workerWishes.isEmpty()) {
            lb.add("\n  Wishes (workers):");
            forEachMapEntry(workerWishes, e -> {
                    UnitType ut = e.getKey();
                    List<WorkerWish> wl = e.getValue();
                    if (!wl.isEmpty()) {
                        lb.add("\n    ", ut.getSuffix(), ":");
                        for (WorkerWish ww : wl) {
                            lb.add(" ", ww.getDestination(),
                                "(", ww.getValue(), ")");
                        }
                    }
                });
        }
        if (!goodsWishes.isEmpty()) {
            lb.add("\n  Wishes (goods):");
            forEachMapEntry(goodsWishes, e -> {
                    GoodsType gt = e.getKey();
                    List<GoodsWish> gl = e.getValue();
                    if (!gl.isEmpty()) {
                        lb.add("\n    ", gt.getSuffix(), ":");
                        for (GoodsWish gw : gl) {
                            lb.add(" ", gw.getDestination(),
                                "(", gw.getValue(), ")");
                        }
                    }
                });
        }
    }

    /**
     * Consume a WorkerWish, yielding a WishRealizationMission for a unit.
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @param ww The {@code WorkerWish} to consume.
     */
    private void consumeWorkerWish(AIUnit aiUnit, WorkerWish ww) {
        final Unit unit = aiUnit.getUnit();
        List<WorkerWish> wwL = workerWishes.get(unit.getType());
        wwL.remove(ww);
        List<Wish> wl = transportDemand.get(ww.getDestination());
        if (wl != null) wl.remove(ww);
        ww.setTransportable(aiUnit);
    }

    /**
     * Consume a GoodsWish.
     *
     * @param aig The {@code AIGoods} to use.
     * @param gw The {@code GoodsWish} to consume.
     */
    private void consumeGoodsWish(AIGoods aig, GoodsWish gw) {
        final Goods goods = aig.getGoods();
        List<GoodsWish> gwL = goodsWishes.get(goods.getType());
        gwL.remove(gw);
        List<Wish> wl = transportDemand.get(gw.getDestination());
        if (wl != null) wl.remove(gw);
        gw.setTransportable(aig);
    }


    // Useful public routines

    /**
     * Gets the number of units that should build a colony.
     *
     * This is the desired total number, not the actual number which would
     * take into account the number of existing BuildColonyMissions.
     *
     * @return The desired number of colony builders for this player.
     */
    private int buildersNeeded() {
        Player player = getPlayer();
        if (!player.canBuildColonies()) return 0;

        int nColonies = 0, nPorts = 0, nWorkers = 0, nEuropean = 0;
        for (Settlement settlement : player.getSettlementList()) {
            nColonies++;
            if (settlement.isConnectedPort()) nPorts++;
            nWorkers += count(settlement.getAllUnitsList(), Unit::isPerson);
        }
        Europe europe = player.getEurope();
        nEuropean = (europe == null) ? 0
            : count(europe.getUnits(), Unit::isPerson);
            
        // If would be good to have at least two colonies, and at least
        // one port.  After that, determine the ratio of workers to colonies
        // (which should be the average colony size), and if that is above
        // a threshold, send out another colonist.
        // The threshold probably should be configurable.  2 is too
        // low IMHO as it makes a lot of brittle colonies, 3 is too
        // high at least initially as it makes it hard for the initial
        // colonies to become substantial.  For now, arbitrarily choose e.
        return (nColonies == 0 || nPorts == 0) ? 2
            : ((nPorts <= 1) && (nWorkers + nEuropean) >= 3) ? 1
            : ((double)(nWorkers + nEuropean) / nColonies > Math.E) ? 1
            : 0;
    }


    /**
     * Asks the server to recruit a unit in Europe on behalf of the AIPlayer.
     *
     * FIXME: Move this to a specialized Handler class (AIEurope?)
     * FIXME: Give protected access?
     *
     * @param slot The migration slot to recruit from.
     * @return The new AIUnit created by this action or null on failure.
     */
    private AIUnit recruitAIUnitInEurope(int slot) {
        AIUnit aiUnit = null;
        Europe europe = getPlayer().getEurope();
        if (europe == null) return null;
        int n = europe.getUnitCount();
        final String selectAbility = Ability.SELECT_RECRUIT;
        if (!Europe.MigrationType.validMigrantSlot(slot)) {
            slot = (getPlayer().hasAbility(selectAbility))
                ? Europe.MigrationType.getDefaultSlot()
                : Europe.MigrationType.getUnspecificSlot();
        }
        if (AIMessage.askEmigrate(this, slot)
            && europe.getUnitCount() == n+1) {
            aiUnit = getAIUnit(europe.getUnitList().get(n));
            if (aiUnit != null) addAIUnit(aiUnit);
        }
        return aiUnit;
    }

    /**
     * Helper function for server communication - Ask the server
     * to train a unit in Europe on behalf of the AIGetPlayer().
     *
     * FIXME: Move this to a specialized Handler class (AIEurope?)
     * FIXME: Give protected access?
     *
     * @param unitType The {@code UnitType} to train.
     * @return the new AIUnit created by this action. May be null.
     */
    private AIUnit trainAIUnitInEurope(UnitType unitType) {
        if (unitType == null) {
            throw new RuntimeException("Invalid UnitType: " + this);
        }

        AIUnit aiUnit = null;
        Europe europe = getPlayer().getEurope();
        if (europe == null) return null;
        int n = europe.getUnitCount();

        if (AIMessage.askTrainUnitInEurope(this, unitType)
            && europe.getUnitCount() == n+1) {
            aiUnit = getAIUnit(europe.getUnitList().get(n));
            if (aiUnit != null) addAIUnit(aiUnit);
        }
        return aiUnit;
    }

    /**
     * Gets the wishes for all this player's colonies, sorted by the
     * {@link Wish#getValue value}.
     *
     * @return A list of wishes.
     */
    public List<Wish> getWishes() {
        return sort(flatten(getAIColonies(), aic -> aic.getWishes().stream()),
                    ValuedAIObject.descendingValueComparator);
    }


    // Diplomacy support

    /**
     * Determines the stances towards each player.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    private void determineStances(LogBuilder lb) {
        final Player player = getPlayer();
        lb.mark();

        for (Player p : getGame().getLivePlayerList(player)) {
            Stance newStance = determineStance(p);
            if (newStance != player.getStance(p)) {
                if (newStance == Stance.WAR && peaceHolds(p)) {
                    ; // Peace treaty holds for now
                } else {
                    getAIMain().getFreeColServer().getInGameController()
                        .changeStance(player, newStance, p, true);
                    lb.add(" ", p.getDebugName(), "->", newStance, ", ");
                }
            }
        }
        if (lb.grew("\n  Stance changes:")) lb.shrink(", ");
    }

    /**
     * See if a recent peace treaty still has force.
     *
     * @param p The {@code Player} to check for a peace treaty with.
     * @return True if peace gets another chance.
     */
    private boolean peaceHolds(Player p) {
        final Player player = getPlayer();
        final Turn turn = getGame().getTurn();
        final double peaceProb = getSpecification()
            .getPercentageMultiplier(GameOptions.PEACE_PROBABILITY);

        int peaceTurn = -1;
        for (HistoryEvent h : player.getHistory()) {
            if (p.getId().equals(h.getPlayerId())
                && h.getTurn().getNumber() > peaceTurn) {
                switch (h.getEventType()) {
                case MAKE_PEACE: case FORM_ALLIANCE:
                    peaceTurn = h.getTurn().getNumber();
                    break;
                case DECLARE_WAR:
                    peaceTurn = -1;
                    break;
                default:
                    break;
                }
            }
        }
        if (peaceTurn < 0) return false;

        int n = turn.getNumber() - peaceTurn;
        float prob = (float)Math.pow(peaceProb, n);
        // Apply Franklin's modifier
        prob = p.apply(prob, turn, Modifier.PEACE_TREATY);
        return prob > 0.0f
            && (randomInt(logger, "Peace holds?",  getAIRandom(), 100)
                < (int)(100.0f * prob));
    }


    /**
     * Get a nation summary for another player.
     *
     * @param other The other {@code Player} to get the summary for.
     * @return The current {@code NationSummary} for a player.
     */
    protected NationSummary getNationSummary(Player other) {
        final Player player = getPlayer();
        NationSummary ns = player.getNationSummary(other);
        if (ns != null) return ns;
        AIMessage.askNationSummary(this, other);
        return player.getNationSummary(other);
    }

    /**
     * Get the land force strength ratio of this player with respect
     * to another.
     *
     * @param other The other {@code Player}.
     * @return The strength ratio (strength/sum(strengths)).
     */
    protected double getStrengthRatio(Player other) {
        return getPlayer().getStrengthRatio(other, false);
    }

    /**
     * Is this player lagging in naval strength?  Calculate the ratio
     * of its naval strength to the average strength of other European
     * colonial powers.
     *
     * @return The naval strength ratio, or negative if there are no other
     *     European colonial nations.
     */
    protected double getNavalStrengthRatio() {
        final Player player = getPlayer();
        double navalAverage = 0.0;
        double navalStrength = 0.0;
        int nPlayers = 0;
        for (Player p : transform(getGame().getLiveEuropeanPlayers(player),
                                  x -> !x.isREF())) {
            NationSummary ns = getNationSummary(p);
            if (ns == null) continue;
            if (p == player) {
                navalStrength = ns.getNavalStrength();
            } else {
                int st = ns.getNavalStrength();
                if (st >= 0) navalAverage += st;
                nPlayers++;
            }
        }
        if (nPlayers <= 0 || navalStrength < 0) return -1.0;
        navalAverage /= nPlayers;
        return (navalAverage == 0.0) ? -1.0 : navalStrength / navalAverage;
    }

    /**
     * Reject a trade agreement, except if a Franklin-derived stance
     * is supplied.
     *
     * @param stance A stance {@code TradeItem}.
     * @param agreement The {@code DiplomaticTrade} to reset.
     * @return The {@code TradeStatus} for the agreement.
     */
    private TradeStatus rejectAgreement(TradeItem stance,
                                        DiplomaticTrade agreement) {
        if (stance == null) return TradeStatus.REJECT_TRADE;
        
        agreement.clear();
        agreement.add(stance);
        return TradeStatus.PROPOSE_TRADE;
    }


    // Mission handling

    /**
     * Ensures all units have a mission.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    protected void giveNormalMissions(LogBuilder lb) {
        final AIMain aiMain = getAIMain();
        final Player player = getPlayer();
        BuildColonyMission bcm = null;
        Mission m;

        nBuilders = buildersNeeded();
        nPioneers = pioneersNeeded();
        nScouts = scoutsNeeded();

        List<AIUnit> aiUnits = getAIUnits();
        List<AIUnit> navalUnits = new ArrayList<>(aiUnits.size()/2);
        List<AIUnit> done = new ArrayList<>(aiUnits.size());
        List<TransportMission> transportMissions = new ArrayList<>(aiUnits.size()/2);
        java.util.Map<Unit, String> reasons = new HashMap<>(aiUnits.size());

        // For all units, check if it is a candidate for a new
        // mission.  If it is not a candidate remove it from the
        // aiUnits list (reporting why not).  Adjust the
        // Build/Pioneer/Scout counts according to the existing valid
        // missions.  Accumulate potentially usable transport missions.
        lb.mark();
        for (AIUnit aiUnit : aiUnits) {
            final Unit unit = aiUnit.getUnit();
            final Colony colony = unit.getColony();
            m = aiUnit.getMission();
            final Location oldTarget = (m == null) ? null : m.getTarget();

            if (!unit.isInitialized() || unit.isDisposed()) {
                reasons.put(unit, "Invalid");

            } else if (unit.isDamaged()) { // Damaged units must wait
                if (!(m instanceof IdleAtSettlementMission)) {
                    if ((m = getIdleAtSettlementMission(aiUnit)) != null) {
                        lb.add(", ", m);
                    }
                }
                reasons.put(unit, "Damaged");
                    
            } else if (unit.getState() == UnitState.IN_COLONY
                && colony.getUnitCount() <= 1) {
                // The unit has its hand full keeping the colony alive.
                if (!(m instanceof WorkInsideColonyMission)
                    && (m = getWorkInsideColonyMission(aiUnit,
                            aiMain.getAIColony(colony))) != null) {
                    logger.warning(aiUnit + " should WorkInsideColony at "
                        + colony.getName());
                    lb.add(", ", m);
                    updateTransport(aiUnit, oldTarget, lb);
                }
                reasons.put(unit, "Vital");

            } else if (unit.isInMission()) {
                reasons.put(unit, "Mission");

            } else if (m != null && m.isValid() && !m.isOneTime()) {
                if (m instanceof BuildColonyMission) {
                    bcm = (BuildColonyMission)m;
                    nBuilders--;
                } else if (m instanceof PioneeringMission) {
                    nPioneers--;
                } else if (m instanceof ScoutingMission) {
                    nScouts--;
                } else if (m instanceof TransportMission) {
                    TransportMission tm = (TransportMission)m;
                    // Consider reassigning quiescent transport
                    // missions to privateer missions
                    if (tm.isEmpty() && unit.isNaval()
                        && unit.isOffensiveUnit()) {
                        navalUnits.add(aiUnit);
                        done.add(aiUnit);
                        continue;
                    }
                    // If there is capacity in this mission, consider adding
                    // more cargoes
                    if (tm.destinationCapacity() > 0) {
                        transportMissions.add(tm);
                    }
                } else if (m instanceof PrivateerMission) {
                    if (!(m.getTarget() instanceof Unit)) {
                        // Privateering but not chasing a unit, consider
                        // reassigning to transport.
                        navalUnits.add(aiUnit);
                        done.add(aiUnit);
                        continue;
                    }
                }
                reasons.put(unit, "Valid");

            } else if (unit.isNaval()) {
                navalUnits.add(aiUnit);

            } else if (unit.isAtSea()) { // Wait for it to emerge
                reasons.put(unit, "At-Sea");

            } else { // Needs mission
                continue;
            }                
            done.add(aiUnit);
        }
        aiUnits.removeAll(done);
        done.clear();

        // First try to satisfy the demand for missions with a defined
        // quota.  Builders first to keep weak players in the game,
        // scouts next as they are profitable.  Pile onto any existing
        // building mission if there are no colonies.
        if (bcm != null && !player.hasSettlements()) {
            final Location bcmTarget = bcm.getTarget();
            for (AIUnit aiUnit : sort(aiUnits, builderComparator)) {
                final Location oldTarget = ((m = aiUnit.getMission()) == null)
                    ? null : m.getTarget();
                if ((m = getBuildColonyMission(aiUnit, bcmTarget)) == null)
                    continue;
                lb.add(", ", m);
                updateTransport(aiUnit, oldTarget, lb);
                done.add(aiUnit);
                if (requestsTransport(aiUnit)) transportSupply.add(aiUnit);
                reasons.put(aiUnit.getUnit(), "0Builder");
            }
            aiUnits.removeAll(done);
            done.clear();
        }
        if (nBuilders > 0) {
            for (AIUnit aiUnit : sort(aiUnits, builderComparator)) {
                final Location oldTarget = ((m = aiUnit.getMission()) == null)
                    ? null : m.getTarget();
                if ((m = getBuildColonyMission(aiUnit, null)) == null)
                    continue;
                lb.add(", ", m);
                updateTransport(aiUnit, oldTarget, lb);
                done.add(aiUnit);
                if (requestsTransport(aiUnit)) transportSupply.add(aiUnit);
                reasons.put(aiUnit.getUnit(), "Builder" + nBuilders);
                if (--nBuilders <= 0) break;
            }
            aiUnits.removeAll(done);
            done.clear();
        }
        if (nScouts > 0) {
            for (AIUnit aiUnit : sort(aiUnits, scoutComparator)) {
                final Location oldTarget = ((m = aiUnit.getMission()) == null)
                    ? null : m.getTarget();
                final Unit unit = aiUnit.getUnit();
                if ((m = getScoutingMission(aiUnit)) == null) continue;
                lb.add(", ", m);
                updateTransport(aiUnit, oldTarget, lb);
                done.add(aiUnit);
                if (requestsTransport(aiUnit)) transportSupply.add(aiUnit);
                reasons.put(unit, "Scout" + nScouts);
                if (--nScouts <= 0) break;
            }
            aiUnits.removeAll(done);
            done.clear();
        }
        if (nPioneers > 0) {
            for (AIUnit aiUnit : sort(aiUnits, pioneerComparator)) {
                final Unit unit = aiUnit.getUnit();
                final Location oldTarget = ((m = aiUnit.getMission()) == null)
                    ? null : m.getTarget();
                if ((m = getPioneeringMission(aiUnit, null)) == null) continue;
                lb.add(", ", m);
                updateTransport(aiUnit, oldTarget, lb);
                done.add(aiUnit);
                if (requestsTransport(aiUnit)) transportSupply.add(aiUnit);
                reasons.put(unit, "Pioneer" + nPioneers);
                if (--nPioneers <= 0) break;
            }
            aiUnits.removeAll(done);
            done.clear();
        }

        // Give the remaining land units a valid mission.
        for (AIUnit aiUnit : aiUnits) {
            final Unit unit = aiUnit.getUnit();
            final Location oldTarget = ((m = aiUnit.getMission()) == null)
                ? null : m.getTarget();
            if ((m = getSimpleMission(aiUnit)) == null) continue;
            lb.add(", ", m);
            updateTransport(aiUnit, oldTarget, lb);
            reasons.put(unit, "New-Land");
            done.add(aiUnit);
            if (requestsTransport(aiUnit)) transportSupply.add(aiUnit);
        }
        aiUnits.removeAll(done);
        done.clear();

        // Process the free naval units, possibly adding to the usable
        // transport missions.
        for (AIUnit aiUnit : navalUnits) {
            final Unit unit = aiUnit.getUnit();
            Mission old = ((m = aiUnit.getMission()) != null && m.isValid())
                ? m : null;
            if ((m = getSimpleMission(aiUnit)) == null) continue;
            lb.add(", ", m, ((m == old) ? " (preserved)" : " (new)"));
            reasons.put(unit, "New-Naval");
            done.add(aiUnit);
            if (m instanceof TransportMission) {
                TransportMission tm = (TransportMission)m;
                if (tm.destinationCapacity() > 0) {
                    transportMissions.add(tm);
                }
                // A new transport mission might have retargeted
                // its passengers into new valid missions.
                for (Unit u : aiUnit.getUnit().getUnitList()) {
                    AIUnit aiu = getAIUnit(u);
                    Mission um = aiu.getMission();
                    if (um != null && um.isValid()
                        && aiUnits.contains(aiu)) {
                        aiUnits.remove(aiu);
                        reasons.put(aiu.getUnit(), "TNew");
                    }
                }
            }
        }
        navalUnits.removeAll(done);
        done.clear();

        // Give remaining units the fallback mission.
        aiUnits.addAll(navalUnits);
        List<Colony> ports = null;
        int nPorts = player.getNumberOfPorts();
        for (AIUnit aiUnit : aiUnits) {
            final Unit unit = aiUnit.getUnit();
            m = aiUnit.getMission();
            final Location oldTarget = (m == null) ? null : m.getTarget();
            if (m != null && m.isValid() && !m.isOneTime()) {
                logger.warning("Trying fallback mission for unit " + unit
                    + " with valid mission " + m
                    + " reason " + reasons.get(unit));
                continue;
            }

            if (nPorts > 0 && unit.isInEurope() && unit.isPerson()) {
                // Choose a port to add to
                if (ports == null) ports = player.getConnectedPortList();
                Colony c = ports.remove(0);
                AIColony aic = aiMain.getAIColony(c);
                if ((m = getWorkInsideColonyMission(aiUnit, aic)) != null) {
                    lb.add(", ", m);
                    updateTransport(aiUnit, oldTarget, lb);
                    reasons.put(unit, "To-work");
                    ports.add(c);
                }

            } else if (m instanceof IdleAtSettlementMission) {
                reasons.put(unit, "Idle"); // already idle
            } else {
                if ((m = getIdleAtSettlementMission(aiUnit)) != null) {
                    lb.add(", ", m);
                    updateTransport(aiUnit, oldTarget, lb);
                    reasons.put(unit, "Idle");
                }
            }
        }
        lb.grew("\n  Mission changes");

        // Now see if transport can be found
        allocateTransportables(getUrgentTransportables(),
                               transportMissions, lb);

        // Log
        if (!aiUnits.isEmpty()) {
            lb.add("\n  Free Land Units:");
            for (AIUnit aiu : aiUnits) {
                lb.add(" ", aiu.getUnit());
            }
        }
        if (!navalUnits.isEmpty()) {
            lb.add("\n  Free Naval Units:");
            for (AIUnit aiu : navalUnits) {
                lb.add(" ", aiu.getUnit());
            }
        }
        lb.add("\n  Missions(colonies=", player.getSettlementCount(),
            " builders=", nBuilders,
            " pioneers=", nPioneers,
            " scouts=", nScouts,
            " naval-carriers=", nNavalCarrier,
            ")");
        logMissions(reasons, lb);
    }

    /**
     * Choose a mission for an AIUnit.
     *
     * @param aiUnit The {@code AIUnit} to choose for.
     * @return A suitable {@code Mission}, or null if none found.
     */
    private Mission getSimpleMission(AIUnit aiUnit) {
        final Unit unit = aiUnit.getUnit();
        Mission m, ret;
        final Mission old = ((m = aiUnit.getMission()) != null && m.isValid())
            ? m : null;

        if (unit.isNaval()) {
            ret = (old instanceof PrivateerMission) ? old
                : ((m = getPrivateerMission(aiUnit, null)) != null) ? m
                : (old instanceof TransportMission) ? old
                : ((m = getTransportMission(aiUnit)) != null) ? m
                : (old instanceof UnitSeekAndDestroyMission) ? old
                : ((m = getSeekAndDestroyMission(aiUnit, 8)) != null) ? m
                : (old instanceof UnitWanderHostileMission) ? old
                : getWanderHostileMission(aiUnit);

        } else if (unit.isCarrier()) {
            ret = getTransportMission(aiUnit);

        } else {
            // CashIn missions are obvious
            ret = (old instanceof CashInTreasureTrainMission) ? old
                : ((m = getCashInTreasureTrainMission(aiUnit)) != null) ? m

                // Working in colony is obvious
                : (unit.isInColony()
                    && old instanceof WorkInsideColonyMission) ? old
                : (unit.isInColony()
                    && (m = getWorkInsideColonyMission(aiUnit, null)) != null) ? m

                // Try to maintain local defence
                : (old instanceof DefendSettlementMission) ? old
                : ((m = getDefendCurrentSettlementMission(aiUnit)) != null) ? m

                // REF override
                : (unit.hasAbility(Ability.REF_UNIT))
                ? ((old instanceof UnitSeekAndDestroyMission) ? old
                    : ((m = getSeekAndDestroyMission(aiUnit, 12)) != null) ? m
                    : (m = getWanderHostileMission(aiUnit)))

                // Favour wish realization for expert units
                : (unit.isColonist() && unit.getSkillLevel() > 0
                    && old instanceof WishRealizationMission) ? old
                : (unit.isColonist() && unit.getSkillLevel() > 0
                    && (m = getWishRealizationMission(aiUnit, null)) != null) ? m

                // Ordinary defence
                : ((m = getDefendSettlementMission(aiUnit, false)) != null) ? m

                // Try nearby offence
                : (old instanceof UnitSeekAndDestroyMission) ? old
                : ((m = getSeekAndDestroyMission(aiUnit, 8)) != null) ? m

                // Missionary missions are only available to some units
                : (old instanceof MissionaryMission) ? old
                : ((m = getMissionaryMission(aiUnit)) != null) ? m

                // Try to satisfy any remaining wishes, such as population
                : (old instanceof WishRealizationMission) ? old
                : ((m = getWishRealizationMission(aiUnit, null)) != null) ? m

                // Another try to defend, with relaxed cost decider
                : ((m = getDefendSettlementMission(aiUnit, true)) != null) ? m

                // Another try to attack, at longer range
                : ((m = getSeekAndDestroyMission(aiUnit, 16)) != null) ? m

                // Leftover offensive units should go out looking for trouble
                : (old instanceof UnitWanderHostileMission) ? old
                : ((m = getWanderHostileMission(aiUnit)) != null) ? m

                : null;
        }
        return ret;
    }

    // Mission creation convenience routines.
    // Aggregated here for uniformity.  Might have been more logical
    // to disperse them to the individual classes.

    /**
     * Gets a new BuildColonyMission for a unit.
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @param target An optional target {@code Location}.
     * @return A new mission, or null if impossible.
     */
    private Mission getBuildColonyMission(AIUnit aiUnit, Location target) {
        String reason = BuildColonyMission.invalidMissionReason(aiUnit);
        if (reason != null) return null;
        final Unit unit = aiUnit.getUnit();
        if (target == null) {
            target = BuildColonyMission.findMissionTarget(aiUnit,
                buildingRange, unit.isInEurope());
        }
        return (target == null) ? null
            : new BuildColonyMission(getAIMain(), aiUnit, target);
    }

    /**
     * Gets a new CashInTreasureTrainMission for a unit.
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @return A new mission, or null if impossible.
     */
    private Mission getCashInTreasureTrainMission(AIUnit aiUnit) {
        String reason = CashInTreasureTrainMission.invalidMissionReason(aiUnit);
        if (reason != null) return null;
        final Unit unit = aiUnit.getUnit();
        Location loc = CashInTreasureTrainMission.findMissionTarget(aiUnit,
            cashInRange, unit.isInEurope());
        return (loc == null) ? null
            : new CashInTreasureTrainMission(getAIMain(), aiUnit, loc);
    }

    /**
     * Gets a new DefendSettlementMission for a unit.
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @param relaxed Use a relaxed cost decider to choose the target.
     * @return A new mission, or null if impossible.
     */
    private Mission getDefendSettlementMission(AIUnit aiUnit, boolean relaxed) {
        if (DefendSettlementMission.invalidMissionReason(aiUnit) != null) return null;
        final Unit unit = aiUnit.getUnit();
        final Location loc = unit.getLocation();
        double worstValue = 1000000.0;
        Colony worstColony = null;
        for (AIColony aic : getAIColonies()) {
            Colony colony = aic.getColony();
            if (aic.isBadlyDefended()) {
                if (unit.isAtLocation(colony.getTile())) {
                    worstColony = colony;
                    break;
                }
                int ttr = 1 + unit.getTurnsToReach(loc, colony.getTile(),
                    unit.getCarrier(),
                    ((relaxed) ? CostDeciders.numberOfTiles() : null));
                if (ttr >= Unit.MANY_TURNS) continue;
                double value = colony.getDefenceRatio() * 100.0 / ttr;
                if (worstValue > value) {
                    worstValue = value;
                    worstColony = colony;
                }
            }
        }
        return (worstColony == null) ? null
            : getDefendSettlementMission(aiUnit, worstColony);
    }

    /**
     * Gets a new MissionaryMission for a unit.
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @return A new mission, or null if impossible.
     */
    public Mission getMissionaryMission(AIUnit aiUnit) {
        if (MissionaryMission.prepare(aiUnit) != null) return null;
        Location loc = MissionaryMission.findMissionTarget(aiUnit,
            missionaryRange, true);
        if (loc == null) {
            aiUnit.equipForRole(getSpecification().getDefaultRole());
            return null;
        }
        return new MissionaryMission(getAIMain(), aiUnit, loc);
    }

    /**
     * Gets a new PioneeringMission for a unit.
     *
     * FIXME: pioneers to make roads between colonies
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @param target An optional target {@code Location}.
     * @return A new mission, or null if impossible.
     */
    public Mission getPioneeringMission(AIUnit aiUnit, Location target) {
        if (PioneeringMission.prepare(aiUnit) != null) return null;
        if (target == null) {
            target = PioneeringMission.findMissionTarget(aiUnit,
                pioneeringRange, true);
        }
        if (target == null) {
            Unit unit = aiUnit.getUnit();
            if (unit.isInEurope() || unit.getSettlement() != null) {
                aiUnit.equipForRole(getSpecification().getDefaultRole());
            }
            return null;
        }
        return new PioneeringMission(getAIMain(), aiUnit, target);
    }

    /**
     * Gets a new PrivateerMission for a unit.
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @param target An optional target {@code Location}.
     * @return A new mission, or null if impossible.
     */
    public Mission getPrivateerMission(AIUnit aiUnit, Location target) {
        if (PrivateerMission.invalidMissionReason(aiUnit) != null) return null;
        if (target == null) {
            target = PrivateerMission.findMissionTarget(aiUnit, privateerRange, true);
        }
        return (target == null) ? null
            : new PrivateerMission(getAIMain(), aiUnit, target);
    }

    /**
     * Gets a new ScoutingMission for a unit.
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @return A new mission, or null if impossible.
     */
    public Mission getScoutingMission(AIUnit aiUnit) {
        if (ScoutingMission.prepare(aiUnit) != null) return null;
        Location loc = ScoutingMission.findMissionTarget(aiUnit,
            scoutingRange, true);
        if (loc == null) {
            Unit unit = aiUnit.getUnit();
            if (unit.isInEurope() || unit.getSettlement() != null) {
                aiUnit.equipForRole(getSpecification().getDefaultRole());
            }
            return null;
        }            
        return new ScoutingMission(getAIMain(), aiUnit, loc);
    }

    /**
     * Gets a new TransportMission for a unit.
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @return A new mission, or null if impossible.
     */
    public Mission getTransportMission(AIUnit aiUnit) {
        if (TransportMission.invalidMissionReason(aiUnit) != null) return null;
        return new TransportMission(getAIMain(), aiUnit);
    }

    /**
     * Gets a new WishRealizationMission for a unit.
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @param wish An optional {@code WorkerWish} to realize.
     * @return A new mission, or null if impossible.
     */
    private Mission getWishRealizationMission(AIUnit aiUnit, WorkerWish wish) {
        if (WishRealizationMission.invalidMissionReason(aiUnit) != null) return null;
        final Unit unit = aiUnit.getUnit();
        if (wish == null) {
            wish = getBestWorkerWish(aiUnit, unit.getType());
        }
        if (wish == null) return null;
        consumeWorkerWish(aiUnit, wish);
        return new WishRealizationMission(getAIMain(), aiUnit, wish);
    }

    /**
     * Gets a WorkInsideColonyMission for a unit.
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @param aiColony An optional {@code AIColony} to work at.
     * @return A new mission, or null if impossible.
     */
    public Mission getWorkInsideColonyMission(AIUnit aiUnit,
                                              AIColony aiColony) {
        if (WorkInsideColonyMission.invalidMissionReason(aiUnit) != null) return null;
        if (aiColony == null) {
            aiColony = getAIColony(aiUnit.getUnit().getColony());
        }
        return (aiColony == null) ? null
            : new WorkInsideColonyMission(getAIMain(), aiUnit, aiColony);
    }


    // AIPlayer interface

    /**
     * {@inheritDoc}
     */
    @Override
    protected Stance determineStance(Player other) {
        final Player player = getPlayer();
        return (other.isREF())
            ? ((player.getREFPlayer() == other) 
                // At war with our REF if rebel, otherwise at peace.
                ? ((player.isRebel()) ? Stance.WAR : Stance.PEACE)
                // Do not mess with other player's REF unless they conquer
                // their rebellious colonies.
                : ((!other.getRebels().isEmpty()) ? Stance.PEACE
                    : super.determineStance(other)))
            // Use normal stance determination for non-REF nations.
            : super.determineStance(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startWorking() {
        final Player player = getPlayer();
        final Turn turn = getGame().getTurn();
        final Specification spec = getSpecification();
        initializeFromSpecification(spec);

        // This is happening, very rarely.  Hopefully now fixed by
        // synchronizing access to AIMain.aiObjects.
        if (getAIMain().getAIPlayer(player) != this) {
            throw new RuntimeException("EuropeanAIPlayer integrity fail: " + player);
        }
        clearAIUnits();
        player.clearNationCache();
        badlyDefended.clear();

        // Note call to getAIUnits().  This triggers
        // AIPlayer.createAIUnits which we want to do early, certainly
        // before cheat() or other operations that might make new units
        // happen.
        LogBuilder lb = new LogBuilder(1024);
        int colonyCount = getAIColonies().size();
        lb.add(player.getDebugName(),
               " in ", turn, "/", turn.getNumber(),
               " units=", getAIUnits().size(),
               " colonies=", colonyCount,
               " declare=", (player.checkDeclareIndependence() == null),
               " v-land-REF=", player.getRebelStrengthRatio(false),
               " v-naval-REF=", player.getRebelStrengthRatio(true));
        if (turn.isFirstTurn()) initializeMissions(lb);
        determineStances(lb);

        if (colonyCount > 0) {
            lb.add("\n  Badly defended:"); // FIXME: prioritize defence
            for (AIColony aic : getAIColonies()) {
                if (aic.isBadlyDefended()) {
                    badlyDefended.add(aic);
                    lb.add(" ", aic.getColony());
                }
            }

            lb.add("\n  Update colonies:");
            for (AIColony aic : getAIColonies()) aic.update(lb);

            buildTipMap(lb);
            buildWishMaps(lb);
        }
        cheat(lb);
        buildTransportMaps(lb);

        // Note order of operations below.  We allow rearrange et al to run
        // even when there are no movable units left because this expedites
        // mission assignment.
        List<AIUnit> aiUnits = getAIUnits();
        for (int i = 0; i < 3; i++) {
            rearrangeColonies(lb);
            giveNormalMissions(lb);
            bringGifts(lb);
            demandTribute(lb);
            if (aiUnits.isEmpty()) break;
            aiUnits = doMissions(aiUnits, lb);
        }
        lb.log(logger, Level.FINE);

        clearAIUnits();
        tipMap.clear();
        transportDemand.clear();
        transportSupply.clear();
        wagonsNeeded.clear();
        goodsWishes.clear();
        workerWishes.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<AIUnit> doMissions(List<AIUnit> aiUnits, LogBuilder lb) {
        lb.add("\n  Do missions:");
        List<AIUnit> result = new ArrayList<>();

        // For all units, do their mission and collect the ones that need
        // to be revisited.
        for (AIUnit aiu : aiUnits) {
            final Unit unit = aiu.getUnit();
            if (unit == null || unit.isDisposed()) continue;

            // giveNormalMissions should have given all units a
            // mission, but TransportMissions may have delivered a
            // unit and completed its WishRealizationMission, so it is
            // possible for a null mission to happen here.  Refer such
            // units back to giveNormalMissions.
            final Mission oldMission = aiu.getMission();
            if (oldMission == null) {
                result.add(aiu);
                continue;
            }
            final Location oldTarget = oldMission.getTarget();
            final Location oldLocation = unit.getLocation();
            final Colony oldColony = oldLocation.getColony();

            // Do the mission.  Clean up dead units.
            lb.add("\n  ", unit, " ");
            try {
                aiu.doMission(lb);
            } catch (Exception e) {
                lb.add(", EXCEPTION: ", e.getMessage());
                logger.log(Level.WARNING, "doMissions failed for: " + aiu, e);
            }
            if (unit.isDisposed() || unit.getLocation() == null) {
                aiu.dropTransport();
                lb.add(", DIED.");
                continue;
            }
            
            updateTransport(aiu, oldTarget, lb);
            // Check again that the unit is alive, updateTransport() can
            // cause unit to disembark onto a fatal LCR!
            if (unit.isDisposed() || unit.getLocation() == null) {
                lb.add(", DIED.");
                continue;
            }
            
            // Units with moves left should be requeued.  If they are on a
            // carrier the carrier needs to have moves left.
            if (unit.getMovesLeft() > 0 && (!unit.isOnCarrier()
                    || unit.getCarrier().getMovesLeft() > 0)) {
                lb.add("+");
                result.add(aiu);
            } else {
                lb.add(".");
            }

            // Immediately update a newly built colony so that other
            // units that are about to wake up can see its tile
            // improvement plans.
            Colony newColony = unit.getLocation().getColony();
            if (oldColony == null && newColony != null
                && Map.isSameLocation(oldLocation, newColony)) {
                AIColony aiColony = getAIColony(newColony);
                aiColony.update(lb);
                updateTipMap(aiColony);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int adjustMission(AIUnit aiUnit, PathNode path, Class type,
                             int value) {
        if (value > 0) {
            if (type == DefendSettlementMission.class) {
                // Reduce value in proportion to the number of defenders.
                Location loc = DefendSettlementMission.extractTarget(aiUnit, path);
                if (!(loc instanceof Colony)) {
                    throw new IllegalStateException("European players defend colonies: " + loc);
                }
                Colony colony = (Colony)loc;
                int defenders = getSettlementDefenders(colony);
                value -= 25 * defenders;
                // Reduce value according to the stockade level.
                if (colony.hasStockade()) {
                    if (defenders > colony.getStockade().getLevel() + 1) {
                        value -= 100 * colony.getStockade().getLevel();
                    } else {
                        value -= 20 * colony.getStockade().getLevel();
                    }
                }
            }
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndianDemandAction indianDemand(Unit unit, Colony colony,
                                           GoodsType goods, int gold,
                                           IndianDemandAction accept) {
        // FIXME: make a better choice, check whether the colony is
        // well defended
        return ("conquest".equals(getAIAdvantage()))
            ? IndianDemandAction.INDIAN_DEMAND_ACCEPT
            : IndianDemandAction.INDIAN_DEMAND_REJECT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TradeStatus acceptDiplomaticTrade(DiplomaticTrade agreement) {
        final Player player = getPlayer();
        final Player other = agreement.getOtherPlayer(player);
        final boolean franklin
            = other.hasAbility(Ability.ALWAYS_OFFERED_PEACE);
        final java.util.Map<TradeItem, Integer> scores = new HashMap<>();
        TradeItem peace = null;
        TradeStatus result = null;
        LogBuilder lb = new LogBuilder(64);
        lb.add("Evaluate trade offer to ", player.getName(),
            " from ", other.getName());
        if (agreement.getVersion() == 0) {
            // Synthetic event
            result = TradeStatus.PROPOSE_TRADE;
        } else {
            int unacceptable = 0, value = 0, colonies = 0;
            for (TradeItem item : agreement.getItems()) {
                if (item instanceof StanceTradeItem) {
                    getNationSummary(other); // Freshen the name summary cache
                }                    
                int score = item.evaluateFor(player);
                if (item instanceof ColonyTradeItem) {
                    if (item.getSource() == player) {
                        colonies++;
                    } else {
                        colonies--;
                    }
                } else if (item instanceof StanceTradeItem) {
                    // Handle some special cases
                    switch (item.getStance()) {
                    case ALLIANCE: case CEASE_FIRE:
                        if (franklin) {
                            peace = item;
                            score = 0;
                        }
                        break;
                    case UNCONTACTED: case PEACE:
                        if (agreement.getContext() == TradeContext.CONTACT) {
                            peace = item;
                            score = 0;
                        }
                        break;
                    default:
                        break;
                    }
                }
                if (score == TradeItem.INVALID_TRADE_ITEM) {
                    unacceptable++;
                } else {
                    value += score;
                }
                scores.put(item, score);
                lb.add(", ", Messages.message(item.getLabel()), " = ", score);
            }
            lb.add(".");

            if (colonies > 0
                && colonies > player.getSettlementCount() - Colony.TRADE_MARGIN) {
                result = rejectAgreement(peace, agreement);
                lb.add("  Too many (", colonies, ") colonies lost.");
            } else if (unacceptable == 0 && value >= 0) { // Accept if all good
                result = TradeStatus.ACCEPT_TRADE;
                lb.add("  All accepted at ", value, ".");
            } else { // If too many items are unacceptable, reject
                double ratio = (double)unacceptable
                    / (unacceptable + agreement.getItems().size());
                if (ratio > 0.5 - 0.5 * agreement.getVersion()) {
                    result = rejectAgreement(peace, agreement);
                    lb.add("  Too many (", unacceptable, ") unacceptable.");
                }
            }
       
            if (result == null) {
                // Dump the unacceptable offers, sum the rest
                value = 0;
                for (Entry<TradeItem, Integer> entry : scores.entrySet()) {
                    if (entry.getValue() == TradeItem.INVALID_TRADE_ITEM) {
                        agreement.remove(entry.getKey());
                        lb.add("  Dropped invalid ", entry.getKey(), ".");
                    } else {
                        value += entry.getValue();
                        lb.add("  Added valid ", entry.getKey(),
                            ", total = ", value, ".");
                    }
                }
                // If nothing is left then fail, 
                if (agreement.isEmpty()) {
                    result = rejectAgreement(peace, agreement);
                }
            }

            // Give up?
            if (randomInt(logger, "Enough diplomacy?", getAIRandom(),
                    1 + agreement.getVersion()) > 5) {
                result = rejectAgreement(peace, agreement);
                lb.add("  Ran out of patience at ", agreement.getVersion(), ".");
            }

            if (result == null) {
                // Dump the negative offers until the sum is non-negative.
                // Return a proposal with items we like/can accept, or reject
                // if none are left.
                for (Entry<TradeItem, Integer> e
                         : mapEntriesByValue(scores, ascendingIntegerComparator)) {
                    if (value >= 0) break;
                    TradeItem item = e.getKey();
                    value -= e.getValue();
                    if (value >= 50 && item instanceof GoldTradeItem) {
                        // Counter offer smaller amount of gold, FIXME: magic#
                        GoldTradeItem gti = (GoldTradeItem)item;
                        gti.setGold(gti.getGold() - value / 2);
                        value /= 2;
                        lb.add("  Reducing gold item to ", gti.getGold(), ".");
                    } else {
                        agreement.remove(item);
                        lb.add("  Dropped ", item, ", value now = ", value, ".");
                    }
                }
                if (value >= 0 && !agreement.isEmpty()) {
                    result = TradeStatus.PROPOSE_TRADE;
                    lb.add("  Pruned until acceptable at ", value, ".");
                } else {
                    result = rejectAgreement(peace, agreement);
                    lb.add("  Agreement unsalvageable at ", value, ".");
                }
            }
        }

        lb.add(" => ", result);
        lb.log(logger, Level.INFO);
        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public NativeTradeAction handleTrade(NativeTradeAction action,
                                         NativeTrade nt) {
        return NativeTradeAction.NAK_INVALID;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean acceptTax(int tax) {
        boolean ret = true;
        LogBuilder lb = new LogBuilder(64);
        Goods toBeDestroyed = getPlayer().getMostValuableGoods();
        lb.add("Tax demand to ", getPlayer().getName(), " of ", tax, "% with ",
            getPlayer().getMostValuableGoods(), " ");
        GoodsType goodsType = (toBeDestroyed == null) ? null
            : toBeDestroyed.getType();

        if (tax <= 2) {
            // Accept small increase.
            ret = true;
            lb.add("accepted: small rise.");
        } else if (toBeDestroyed == null) {
            // Is this cheating to look at what the crown will destroy?
            ret = false;
            lb.add("rejected: no-goods-under-threat.");
        } else if (goodsType.isFoodType()) {
            ret = false;
            lb.add("rejected: food-type.");
        } else if (goodsType.isBreedable()) {
            // Refuse if we already have this type under production in
            // multiple places.
            int n = count(getPlayer().getSettlements(),
                          s -> s.getGoodsCount(goodsType) > 0);
            ret = n < 2;
            if (ret) {
                lb.add("accepted: breedable-type-", goodsType.getSuffix(), 
                       "-missing.");
            } else {
                lb.add("rejected: breedable-type-", goodsType.getSuffix(),
                       "-present-in-", n, "-settlements.");
            }
        } else if (goodsType.getMilitary()
            || goodsType.isTradeGoods()
            || goodsType.isBuildingMaterial()) {
            // By age 3 we should be able to produce enough ourselves.
            // FIXME: check whether we have an armory, at least
            int turn = getGame().getTurn().getNumber();
            ret = turn < 300;
            lb.add(((ret) ? "accepted" : "rejected"),
                   ": special-goods-in-turn-", turn, ".");
        } else {
            // FIXME: consider the amount of goods produced. If we
            // depend on shipping huge amounts of cheap goods, we
            // don't want these goods to be boycotted.
            final List<GoodsType> goodsTypes = getSpecification()
                .getStorableGoodsTypeList();
            int averageIncome = sum(goodsTypes,
                gt -> getPlayer().getIncomeAfterTaxes(gt)) / goodsTypes.size();
            int income = getPlayer().getIncomeAfterTaxes(toBeDestroyed.getType());
            ret = income <= 0 || income > averageIncome;
            lb.add(((ret) ? "accepted" : "rejected"),
                ": goods(", goodsType.getSuffix(), ")-with-income(", income,
                ((ret) ? ")non-positive-or-more-than(" : ")less-than-average("),
                averageIncome, ").");
        }
        if (!ret) suppressEuropeanTrade(goodsType, lb);
        lb.log(logger, Level.INFO);
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean acceptMercenaries() {
        return getPlayer().isAtWar() || "conquest".equals(getAIAdvantage());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FoundingFather selectFoundingFather(List<FoundingFather> ffs) {
        final int age = getGame().getAge();
        FoundingFather bestFather = null;
        int bestWeight = Integer.MIN_VALUE;
        for (FoundingFather father : ffs) {
            if (father == null) continue;

            // For the moment, arbitrarily: always choose the one
            // offering custom houses.  Allowing the AI to build CH
            // early alleviates the complexity problem of handling all
            // TransportMissions correctly somewhat.
            if (father.hasAbility(Ability.BUILD_CUSTOM_HOUSE)) {
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
     * Gets the needed wagons for a tile/contiguity.
     *
     * @param tile The {@code Tile} to derive the contiguity from.
     * @return The number of wagons needed.
     */
    public int getNeededWagons(Tile tile) {
        if (tile != null) {
            int contig = tile.getContiguity();
            if (contig > 0) {
                Integer i = wagonsNeeded.get(contig);
                if (i != null) return i;
            }
        }
        return 0;
    }

    /**
     * How many pioneers should we have?
     *
     * This is the desired total number, not the actual number which would
     * take into account the number of existing PioneeringMissions.
     *
     * @return The desired number of pioneers for this player.
     */
    public int pioneersNeeded() {
        return (tipMap.size() + 1) / 2;
    }

    /**
     * How many scouts should we have?
     *
     * This is the desired total number, not the actual number which would
     * take into account the number of existing ScoutingMissions.
     *
     * Current scheme for European AIs is to use up to three scouts in
     * the early part of the game, then one.
     *
     * @return The desired number of scouts for this player.
     */
    public int scoutsNeeded() {
        return 3 - (getGame().getTurn().getNumber() / 100);
    }

    /**
     * Notify that a wish has been completed.  Called from AIColony.
     *
     * @param w The {@code Wish} to complete.
     */
    public void completeWish(Wish w) {
        if (w instanceof WorkerWish) {
            WorkerWish ww = (WorkerWish)w;
            List<WorkerWish> wl = workerWishes.get(ww.getUnitType());
            if (wl != null) wl.remove(ww);
        } else if (w instanceof GoodsWish) {
            GoodsWish gw = (GoodsWish)w;
            List<GoodsWish> gl = goodsWishes.get(gw.getGoodsType());
            if (gl != null) gl.remove(gw);
        } else {
            throw new IllegalStateException("Bogus wish: " + w);
        }
    }
    

    // Serialization

    // getXMLTagName not needed, uses parent
}
