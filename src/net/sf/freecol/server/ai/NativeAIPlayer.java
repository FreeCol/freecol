/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.RandomChoice;
import static net.sf.freecol.common.util.RandomUtils.*;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.IndianBringGiftMission;
import net.sf.freecol.server.ai.mission.IndianDemandMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * Objects of this class contains AI-information for a single {@link
 * Player} and is used for controlling this player.
 *
 * The method {@link #startWorking} gets called by the
 * {@link AIInGameInputHandler} when it is this player's turn.
 */
public class NativeAIPlayer extends AIPlayer {

    private static final Logger logger = Logger.getLogger(NativeAIPlayer.class.getName());

    public static final int MAX_DISTANCE_TO_BRING_GIFTS = 5;

    public static final int MAX_NUMBER_OF_GIFTS_BEING_DELIVERED = 1;

    public static final int MAX_DISTANCE_TO_MAKE_DEMANDS = 5;

    public static final int MAX_NUMBER_OF_DEMANDS = 1;

    /**
     * Stores temporary information for sessions (trading with another
     * player etc).
     */
    private final HashMap<String, Integer> sessionRegister = new HashMap<>();

    /**
     * Debug helper to keep track of why/what the units are doing.
     * Do not serialize.
     */
    private final java.util.Map<Unit, String> reasons = new HashMap<>();


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
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public NativeAIPlayer(AIMain aiMain,
                          FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, xr);

        uninitialized = getPlayer() == null;
    }


    /**
     * Simple initialization of AI missions given that we know the starting
     * conditions.
     *
     * @param lb A <code>LogBuilder</code>  to log to.
     */
    private void initializeMissions(LogBuilder lb) {
        final AIMain aiMain = getAIMain();
        final Player player = getPlayer();
        lb.add("\n  Initialize");

        // Give defensive missions up to the minimum expected defence,
        // leave the rest with the default wander-hostile mission.
        List<Unit> units = new ArrayList<>();
        for (IndianSettlement is : player.getIndianSettlements()) {
            units.clear();
            units.addAll(is.getTile().getUnitList());
            units.addAll(is.getUnitList());
            while (units.size() > is.getRequiredDefenders()) {
                Unit u = units.remove(0);
                AIUnit aiu = getAIUnit(u);
                Mission m = getWanderHostileMission(aiu);
                if (m != null) lb.add(" ", m);
            }
            for (Unit u : units) {
                AIUnit aiu = getAIUnit(u);
                Mission m = getDefendSettlementMission(aiu, is);
                if (m != null) lb.add(" ", m);
            }
        }
    }

    /**
     * Determines the stances towards each player.
     * That is: should we declare war?
     *
     * @param lb A <code>LogBuilder</code> to log to.
     */
    private void determineStances(LogBuilder lb) {
        final ServerPlayer serverPlayer = (ServerPlayer)getPlayer();
        lb.mark();

        for (Player p : getGame().getLivePlayers(serverPlayer)) {
            Stance newStance = determineStance(p);
            if (newStance != serverPlayer.getStance(p)) {
                getAIMain().getFreeColServer().getInGameController()
                    .changeStance(serverPlayer, newStance, 
                                  (ServerPlayer)p, true);
                lb.add(" ", p.getDebugName(), "->", newStance, ", ");
            }
        }
        if (lb.grew("\n  Stance changes:")) lb.shrink(", ");
    }

    /**
     * Takes the necessary actions to secure the settlements.
     * This is done by making new military units or to give existing
     * units new missions.
     *
     * @param randoms An array of random settlement indexes.
     * @param lb A <code>LogBuilder</code> to log to.
     */
    private void secureSettlements(int[] randoms, LogBuilder lb) {
        int randomIdx = 0;
        List<IndianSettlement> settlements
            = getPlayer().getIndianSettlements();
        for (IndianSettlement is : settlements) {
            // Spread arms and horses between camps
            // FIXME: maybe make this dependent on difficulty level?
            int n = randoms[randomIdx++];
            IndianSettlement settlement = settlements.get(n);
            if (settlement != is) {
                is.tradeGoodsWithSettlement(settlement);
            }
        }
        for (IndianSettlement is : settlements) {
            lb.mark();
            equipBraves(is, lb);
            secureIndianSettlement(is, lb);
            if (lb.grew("\n  At ", is.getName())) lb.shrink(", ");
        }
    }

    /**
     * Greedily equips braves with horses and muskets.
     * Public for the test suite.
     *
     * @param is The <code>IndianSettlement</code> where the equipping occurs.
     * @param lb A <code>LogBuilder</code> to log to.
     */
    public void equipBraves(IndianSettlement is, LogBuilder lb) {
        final Specification spec = getSpecification();

        // Find all the units
        List<Unit> units = is.getUnitList();
        units.addAll(is.getTile().getUnitList());

        // Prioritize promoting partially equipped units to full dragoon
        Collections.sort(units,
            getGame().getCombatModel().getMilitaryStrengthComparator());

        boolean moreHorses = true, moreMuskets = true;
        for (Unit u : units) {
            Role r = is.canImproveUnitMilitaryRole(u);
            if (r != null) {
                Role old = u.getRole();
                if (getAIUnit(u).equipForRole(r) && u.getRole() != old) {
                    lb.add(u, " upgraded from ", old.getSuffix(), ", ");
                }
            }
        }
    }

    /**
     * Takes the necessary actions to secure an indian settlement
     * Public for the test suite.
     *
     * @param is The <code>IndianSettlement</code> to secure.
     * @param lb A <code>LogBuilder</code> to log to.
     */
    public void secureIndianSettlement(final IndianSettlement is,
                                       LogBuilder lb) {
        final AIMain aiMain = getAIMain();
        final Player player = getPlayer();
        final CombatModel cm = getGame().getCombatModel();
        final int minimumDefence = is.getType().getMinimumSize() - 1;
        DefendSettlementMission dm;

        // Collect native units and defenders
        List<Unit> units = new ArrayList<>();
        List<Unit> defenders = new ArrayList<>();
        units.addAll(is.getUnitList());
        units.addAll(is.getTile().getUnitList());
        for (Unit u : is.getOwnedUnits()) {
            if (!units.contains(u)) units.add(u);
        }

        // Collect the current defenders
        for (Unit u : new ArrayList<>(units)) {
            AIUnit aiu = aiMain.getAIUnit(u);
            if (aiu == null) {
                units.remove(u);
            } else if ((dm = aiu.getMission(DefendSettlementMission.class)) != null
                && dm.getTarget() == is) {
                defenders.add(u);
                units.remove(u);
            } else if (Mission.invalidNewMissionReason(aiu) != null) {
                units.remove(u);
            }
        }

        // Collect threats and other potential defenders
        final HashMap<Tile, Double> threats = new HashMap<>();
        Player enemy;
        Tension tension;
        for (Tile t : is.getTile().getSurroundingTiles(is.getRadius() + 1)) {
            if (!t.isLand() || t.getUnitCount() == 0) {
                ; // Do nothing
            } else if ((enemy = t.getFirstUnit().getOwner()) == player) {
                // Its one of ours!
                for (Unit u : t.getUnitList()) {
                    AIUnit aiu;
                    if (defenders.contains(u) || units.contains(u)
                        || (aiu = aiMain.getAIUnit(u)) == null) {
                        ; // Do nothing
                    } else if ((dm = aiu.getMission(DefendSettlementMission.class)) != null
                        && dm.getTarget() == is) {
                        defenders.add(u);
                    } else if (Mission.invalidNewMissionReason(aiu) == null) {
                        units.add(u);
                    }
                }
            } else if ((tension = is.getAlarm(enemy)) == null
                || tension.getLevel().compareTo(Tension.Level.CONTENT) <= 0) {
                ; // Not regarded as a threat
            } else {
                // Evaluate the threat
                double threshold, bonus, value = 0.0;
                if (tension.getLevel().compareTo(Tension.Level.DISPLEASED) <= 0) {
                    threshold = 1.0;
                    bonus = 0.0f;
                } else {
                    threshold = 0.0;
                    bonus = (float)tension.getLevel().ordinal()
                        - Tension.Level.CONTENT.ordinal();
                }
                value += t.getUnitList().stream()
                    .filter(u -> cm.getOffencePower(u, is) > threshold)
                    .mapToDouble(u -> cm.getOffencePower(u, is) + bonus).sum();
                if (value > 0.0) threats.put(t, value);
            }
        }

        // Sort the available units by proximity to the settlement.
        // Simulates favouring the first warriors found by outgoing messengers.
        // Also favour units native to the settlement.
        final int homeBonus = 3;
        final Tile isTile = is.getTile();
        final Comparator<Unit> isComparator
            = new Comparator<Unit>() {
                @Override
                public int compare(Unit u1, Unit u2) {
                    Tile t1 = u1.getTile();
                    int s1 = t1.getDistanceTo(isTile);
                    Tile t2 = u2.getTile();
                    int s2 = t2.getDistanceTo(isTile);
                    if (u1.getHomeIndianSettlement() == is) s1 -= homeBonus;
                    if (u2.getHomeIndianSettlement() == is) s2 -= homeBonus;
                    return s1 - s2;
                }
           };

        // Do we need more or less defenders?
        int needed = minimumDefence + threats.size();
        if (defenders.size() < needed) { // More needed, call some in.
            Collections.sort(units, isComparator);
            while (!units.isEmpty()) {
                Unit u = units.remove(0);
                AIUnit aiu = aiMain.getAIUnit(u);
                Mission m = getDefendSettlementMission(aiu, is);
                if (m != null) {
                    lb.add(m, ", ");
                    defenders.add(u);
                    if (defenders.size() >= needed) break;
                }
            }
        } else if (defenders.size() > needed) { // Less needed, release them
            Collections.sort(defenders, isComparator);
            Collections.reverse(defenders);
            while (defenders.size() > needed) {
                units.add(defenders.remove(0));
            }
        }

        // Sort threat tiles by threat value.
        List<Tile> threatTiles = new ArrayList<>(threats.keySet());
        Collections.sort(threatTiles, new Comparator<Tile>() {
                @Override
                public int compare(Tile t1, Tile t2) {
                    return Double.compare(threats.get(t2),
                            threats.get(t1));
                }
            });

        if (!defenders.isEmpty()) {
            lb.add(" defend with:");
            for (Unit u : defenders) lb.add(" ", u);
            lb.add(" minimum=", minimumDefence,
                   " threats=", threats.size(), ", ");
        }

        // Assign units to attack the threats, greedily chosing closest unit.
        while (!threatTiles.isEmpty() && !units.isEmpty()) {
            Tile tile = threatTiles.remove(0);
            int bestDistance = Integer.MAX_VALUE;
            Unit unit = null;
            for (Unit u : units) {
                AIUnit aiu = aiMain.getAIUnit(u);
                if (UnitSeekAndDestroyMission.invalidReason(aiu,
                        tile.getDefendingUnit(u)) != null) continue;
                int distance = u.getTile().getDistanceTo(tile);
                if (bestDistance > distance) {
                    bestDistance = distance;
                    unit = u;
                }
            }
            if (unit == null) continue; // Declined to attack.
            units.remove(unit);
            AIUnit aiUnit = aiMain.getAIUnit(unit);
            Unit target = tile.getDefendingUnit(unit);
            Mission m = getSeekAndDestroyMission(aiUnit, target);
            if (m != null) lb.add(m, ", ");
        }
    }

    /**
     * Gives a mission to all units.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     */
    private void giveNormalMissions(LogBuilder lb) {
        final AIMain aiMain = getAIMain();
        final Player player = getPlayer();
        final Specification spec = getSpecification();
        final int turnNumber = getGame().getTurn().getNumber();
        List<AIUnit> aiUnits = getAIUnits();

        lb.mark();
        List<AIUnit> done = new ArrayList<>();
        reasons.clear();
        for (AIUnit aiUnit : aiUnits) {
            final Unit unit = aiUnit.getUnit();
            Mission m = aiUnit.getMission();
            String reason = null;

            if (unit.isUninitialized() || unit.isDisposed()) {
                reasons.put(unit, "Invalid");

            } else if (m != null && m.isValid() && !m.isOneTime()) {
                reasons.put(unit, "Valid");

            } else { // Unit needs a mission
                continue;
            }
            done.add(aiUnit);
        }
        aiUnits.removeAll(done);
        done.clear();

        for (AIUnit aiUnit : aiUnits) {
            final Unit unit = aiUnit.getUnit();
            final Settlement settlement = unit.getSettlement();
            final IndianSettlement is = unit.getHomeIndianSettlement();
            Mission m = aiUnit.getMission();
            
            if (settlement != null && settlement.getUnitCount()
                + settlement.getTile().getUnitCount() <= 1) {
                // First see to local settlement defence
                if (!(m instanceof DefendSettlementMission)
                    || m.getTarget() != settlement) {
                    m = getDefendSettlementMission(aiUnit, settlement);
                    if (m == null) continue;
                    lb.add(m, ", ");
                }
                reasons.put(unit, "Defend-" + settlement.getName());

            } else if (is != null
                && is.canImproveUnitMilitaryRole(unit) != null) {
                // Go home for new equipment if the home settlement has it
                if (!(m instanceof DefendSettlementMission)
                    || m.getTarget() != is) {
                    m = getDefendSettlementMission(aiUnit, is);
                    if (m == null) continue;
                    lb.add(m, ", ");
                }
                reasons.put(unit, "Equip-" + is.getName());

            } else {
                // Go out looking for trouble
                if (!(m instanceof UnitWanderHostileMission)) {
                    m = getWanderHostileMission(aiUnit);
                    if (m == null) continue;
                    lb.add(m, ", ");
                }
                reasons.put(unit, "Patrol");
            }
            done.add(aiUnit);
        }
        aiUnits.removeAll(done);
        done.clear();

        // Log
        if (lb.grew("\n  Mission changes: ")) lb.shrink(", ");
        if (!aiUnits.isEmpty()) {
            lb.add("\n  Free Land Units:");
            for (AIUnit aiu : aiUnits) lb.add(" ", aiu.getUnit());
        }
        lb.add("\n  Missions(settlements=",
            player.getNumberOfSettlements(), ")");
        logMissions(reasons, lb);
    }

    /**
     * Brings gifts to nice players with nearby colonies.
     *
     * @param randoms An array of random percentages.
     * @param lb A <code>LogBuilder</code> to log to.
     */
    private void bringGifts(int[] randoms, LogBuilder lb) {
        final Player player = getPlayer();
        final CostDecider cd = CostDeciders.numberOfLegalTiles();
        final int giftProbability = getSpecification()
            .getInteger(GameOptions.GIFT_PROBABILITY);
        int randomIdx = 0;
        lb.mark();

        for (IndianSettlement is : player.getIndianSettlements()) {
            // Do not bring gifts all the time.
            if (randoms[randomIdx++] >= giftProbability) continue;

            // Check if the settlement has anything to give.
            Goods gift = is.getRandomGift(getAIRandom());
            if (gift == null) continue;

            // Check if there are available units, and if there are already
            // enough missions in operation.
            List<Unit> availableUnits = new ArrayList<>();
            int alreadyAssignedUnits = 0;
            for (Unit ou : is.getOwnedUnits()) {
                AIUnit aiu = getAIUnit(ou);
                if (aiu == null) {
                    continue;
                } else if (aiu.hasMission(IndianBringGiftMission.class)) {
                    alreadyAssignedUnits++;
                } else if (Mission.invalidNewMissionReason(aiu) == null) {
                    availableUnits.add(ou);
                }
            }
            if (alreadyAssignedUnits > MAX_NUMBER_OF_GIFTS_BEING_DELIVERED) {
                lb.add(is.getName(), " has ", alreadyAssignedUnits, 
                       " already, ");
                continue;
            } else if (availableUnits.isEmpty()) {
                lb.add(is.getName(), " has no gift units, ");
                continue;
            }
            // Pick a random available capable unit.
            Unit unit = null;
            AIUnit aiUnit = null;
            Tile home = is.getTile();
            while (unit == null && !availableUnits.isEmpty()) {
                Unit u = availableUnits.get(randomInt(logger, "Gift unit",
                        getAIRandom(), availableUnits.size()));
                availableUnits.remove(u);
                aiUnit = getAIUnit(u);
                if (IndianBringGiftMission.invalidReason(aiUnit) == null
                    && u.findPath(u.getTile(), home, null, cd) != null) {
                    unit = u;
                }
            }
            if (unit == null) {
                lb.add(is.getName(), " found no gift unit, ");
                continue;
            }

            // Collect nearby colonies.  Filter out ones which are uncontacted,
            // unreachable or otherwise unsuitable.  Score the rest on alarm
            // and distance.
            List<RandomChoice<Colony>> nearbyColonies = new ArrayList<>();
            for (Tile t : home.getSurroundingTiles(MAX_DISTANCE_TO_BRING_GIFTS)) {
                Colony c = t.getColony();
                PathNode path;
                if (c == null
                    || !is.hasContacted(c.getOwner())
                    || IndianBringGiftMission.invalidReason(aiUnit, c) != null
                    || (path = unit.findPath(home, c.getTile(),
                                             null, cd)) == null) continue;
                int alarm = Math.max(1, is.getAlarm(c.getOwner()).getValue());
                nearbyColonies.add(new RandomChoice<>(c,
                        1000000 / alarm / path.getTotalTurns()));
            }

            // If there are any suitable colonies, pick a random one
            // to send a gift to.
            if (nearbyColonies.isEmpty()) {
                lb.add(is.getName(), " found no gift colonies, ");
                continue;
            }
            Colony target = RandomChoice.getWeightedRandom(logger,
                "Choose gift colony", nearbyColonies, getAIRandom());
            if (target == null) {
                throw new IllegalStateException("No gift target!?!");
            }

            // Send the unit.
            Mission m = new IndianBringGiftMission(getAIMain(), aiUnit, target);
            lb.add(m, " gift from ", is.getName(),
                   " to ", target.getName(), ", ");
        }
        if (lb.grew("\n  Gifts: ")) lb.shrink(", ");
    }

    /**
     * Demands tribute from nasty players with nearby colonies.
     *
     * @param randoms An array of random percentages.
     * @param lb A <code>LogBuilder</code> to log to.
     */
    private void demandTribute(int[] randoms, LogBuilder lb) {
        final Player player = getPlayer();
        final CostDecider cd = CostDeciders.numberOfLegalTiles();
        final int demandProbability = getSpecification()
            .getInteger(GameOptions.DEMAND_PROBABILITY);
        int randomIdx = 0;
        lb.mark();

        for (IndianSettlement is : player.getIndianSettlements()) {
            // Do not demand tribute all of the time.
            if (randoms[randomIdx++] >= demandProbability) continue;

            // Check if there are available units, and if there are already
            // enough missions in operation.
            List<Unit> availableUnits = new ArrayList<>();
            int alreadyAssignedUnits = 0;
            for (Unit ou : is.getOwnedUnits()) {
                AIUnit aiu = getAIUnit(ou);
                if (Mission.invalidNewMissionReason(aiu) == null) {
                    if (aiu.hasMission(IndianDemandMission.class)) {
                        alreadyAssignedUnits++;
                    } else {
                        availableUnits.add(ou);
                    }
                }
            }
            if (alreadyAssignedUnits > MAX_NUMBER_OF_DEMANDS) {
                lb.add(is.getName(), " has ", alreadyAssignedUnits,
                       " already, ");
                continue;
            } else if (availableUnits.isEmpty()) {
                lb.add(is.getName(), " has no demand units, ");
                continue;
            }
            // Pick a random available capable unit.
            Tile home = is.getTile();
            Unit unit = null;
            AIUnit aiUnit = null;
            while (unit == null && !availableUnits.isEmpty()) {
                Unit u = availableUnits.get(randomInt(logger, "Demand unit",
                        getAIRandom(), availableUnits.size()));
                availableUnits.remove(u);
                aiUnit = getAIUnit(u);
                if (IndianDemandMission.invalidReason(aiUnit) == null
                    && u.findPath(u.getTile(), home, null, cd) != null) {
                    unit = u;
                }
            }
            if (unit == null) {
                lb.add(is.getName(), " found no demand unit, ");
                continue;
            }

            // Collect nearby colonies.  Filter out ones which are unreachable
            // or with which the settlement is on adequate terms.
            List<RandomChoice<Colony>> nearbyColonies = new ArrayList<>();
            for (Tile t : home.getSurroundingTiles(MAX_DISTANCE_TO_MAKE_DEMANDS)) {
                Colony c = t.getColony();
                PathNode path;
                if (c == null
                    || !is.hasContacted(c.getOwner())
                    || IndianDemandMission.invalidReason(aiUnit, c) != null
                    || (path = unit.findPath(home, c.getTile(),
                                             null, cd)) == null) continue;
                int alarm = is.getAlarm(c.getOwner()).getValue();
                int defence = c.getUnitCount() + ((c.getStockade() == null) ? 1
                    : (c.getStockade().getLevel() * 10));
                int weight = 1 + alarm * (1000000 / defence
                                                  / path.getTotalTurns());
                nearbyColonies.add(new RandomChoice<>(c, weight));
            }
            // If there are any suitable colonies, pick one to demand from.
            // Sometimes a random one, sometimes the weakest, sometimes the
            // most annoying.
            if (nearbyColonies.isEmpty()) {
                lb.add(is.getName(), " found no demand colonies, ");
                continue;
            }
            Colony target = RandomChoice.getWeightedRandom(logger,
                "Choose demand colony", nearbyColonies, getAIRandom());
            if (target == null) {
                lb.add(is.getName(), " found no demand target, ");
                continue;
            }

            // Send the unit.
            Mission m = new IndianDemandMission(getAIMain(), aiUnit, target);
            lb.add("At ", is.getName(), " ", m,
                   " will demand of ", target, ", ");
        }
        if (lb.grew("\n  Tribute: ")) lb.shrink(", ");
    }

    /**
     * Gets the appropriate ship trade penalties.
     *
     * @param sense The sense to apply the modifiers.
     * @return The ship trade penalties.
     */
    private Set<Modifier> getShipTradePenalties(boolean sense) {
        final Specification spec = getSpecification();
        int penalty = spec.getInteger(GameOptions.SHIP_TRADE_PENALTY);
        Set<Modifier> result = new HashSet<>();
        for (Modifier m : spec.getModifiers(Modifier.SHIP_TRADE_PENALTY)) {
            Modifier n = new Modifier(m);
            n.setValue((sense) ? penalty : -penalty);
            result.add(n);
        }
        return result;
    }

    /**
     * Aborts all the missions which are no longer valid.
     *
     * Public for the test suite.
     */
    public void abortInvalidMissions() {
        for (AIUnit au : getAIUnits()) {
            Mission mission = au.getMission();
            String reason = (mission == null) ? null : mission.invalidReason();
            if (reason != null) au.setMission(null);
        }
    }


    // AIPlayer interface
    // Inherit:
    //   indianDemand
    //   acceptDiplomaticTrade
    //   acceptTax
    //   acceptMercenaries
    //   selectFoundingFather

    /**
     * {@inheritDoc}
     */
    @Override
    public void startWorking() {
        final Player player = getPlayer();
        final Turn turn = getGame().getTurn();
        final int nSettlements = player.getNumberOfSettlements();
        final Random air = getAIRandom();

        LogBuilder lb = new LogBuilder(1024);
        lb.add(player.getDebugName(), " in ", turn, "/", turn.getNumber());

        sessionRegister.clear();
        clearAIUnits();

        determineStances(lb);
        List<AIUnit> more;
        if (turn.isFirstTurn()) {
            initializeMissions(lb);
            more = getAIUnits();
        } else {
            int[] randoms;
            abortInvalidMissions();
            randoms = randomInts(logger, "Trades", air,
                                 nSettlements, nSettlements);
            secureSettlements(randoms, lb);
            randoms = randomInts(logger, "Gifts", air, 100, nSettlements);
            bringGifts(randoms, lb);
            randoms = randomInts(logger, "Tribute", air, 100, nSettlements);
            demandTribute(randoms, lb);
            giveNormalMissions(lb);
            more = doMissions(getAIUnits(), lb);
        }

        if (!more.isEmpty()) {
            abortInvalidMissions();
            giveNormalMissions(lb);
            doMissions(more, lb);
        }
        clearAIUnits();
        lb.log(logger, Level.FINEST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int adjustMission(AIUnit aiUnit, PathNode path, Class type,
                             int value) {
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
            IndianSettlement is = aiUnit.getUnit().getHomeIndianSettlement();
            if (targetPlayer != null
                && is != null && is.getAlarm(targetPlayer) != null) {
                value += is.getAlarm(targetPlayer).getValue()
                    - Tension.Level.DISPLEASED.getLimit();
            }
        }

        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerSellGoods(Goods goods) {
        String goldKey = "tradeGold#" + goods.getType().getId()
            + "#" + goods.getAmount() + "#" + goods.getLocation().getId();
        sessionRegister.put(goldKey, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int buyProposition(Unit unit, Settlement settlement,
                              Goods goods, int gold) {
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
            Set<Modifier> modifiers = new HashSet<>();
            if (is.hasMissionary(buyer)
                && spec.getBoolean(GameOptions.ENHANCED_MISSIONARIES)) {
                Unit u = is.getMissionary();
                modifiers.addAll(u.getMissionaryTradeModifiers(false));
            }
            if (unit.isNaval()) {
                modifiers.addAll(getShipTradePenalties(false));
            }
            price = (int)FeatureContainer.applyModifiers((float)price,
                getGame().getTurn(), modifiers);
            sessionRegister.put(goldKey, price);
            return price;
        }
        price = registered;
        if (price < 0 || price == gold) return price;
        if (gold < (price * 9) / 10) {
            logger.warning("Cheating attempt: sending offer too low");
            sessionRegister.put(goldKey, -1);
            return NetworkConstants.NO_TRADE;
        }

        int haggling = 1;
        if (sessionRegister.containsKey(hagglingKey)) {
            haggling = sessionRegister.get(hagglingKey);
        }
        if (randomInt(logger, "Haggle-buy", getAIRandom(), 3 + haggling) >= 3) {
            sessionRegister.put(goldKey, -1);
            return NetworkConstants.NO_TRADE_HAGGLE;
        }
        sessionRegister.put(goldKey, gold);
        sessionRegister.put(hagglingKey, haggling + 1);
        return gold;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sellProposition(Unit unit, Settlement settlement,
                               Goods goods, int gold) {
        logger.finest("Entering method sellProposition");
        Specification spec = getSpecification();
        IndianSettlement is = (IndianSettlement) settlement;
        Player seller = unit.getOwner();
        String goldKey = "tradeGold#" + goods.getType().getId()
            + "#" + goods.getAmount() + "#" + unit.getId()
            + "#" + settlement.getId();
        String hagglingKey = "tradeHaggling#" + unit.getId();
        int price;
        if (sessionRegister.containsKey(goldKey)) {
            price = sessionRegister.get(goldKey);
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
            Set<Modifier> modifiers = new HashSet<>();
            if (is.hasMissionary(seller)
                && spec.getBoolean(GameOptions.ENHANCED_MISSIONARIES)) {
                Unit u = is.getMissionary();
                modifiers.addAll(u.getMissionaryTradeModifiers(true));
            }
            if (unit.isNaval()) {
                modifiers.addAll(getShipTradePenalties(true));
            }
            price = (int)FeatureContainer.applyModifiers((float)price,
                getGame().getTurn(), modifiers);
            if (price <= 0) return 0;
            sessionRegister.put(goldKey, price);
        }
        if (gold < 0 || price == gold) return price;
        if (gold > (price * 11) / 10) {
            logger.warning("Cheating attempt: haggling request too high");
            sessionRegister.put(goldKey, -1);
            return NetworkConstants.NO_TRADE;
        }
        int haggling = 1;
        if (sessionRegister.containsKey(hagglingKey)) {
            haggling = sessionRegister.get(hagglingKey);
        }
        if (randomInt(logger, "Haggle-sell", getAIRandom(), 3 + haggling) >= 3) {
            sessionRegister.put(goldKey, -1);
            return NetworkConstants.NO_TRADE_HAGGLE;
        }
        sessionRegister.put(goldKey, gold);
        sessionRegister.put(hagglingKey, haggling + 1);
        return gold;
    }


    // Serialization

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }
}
