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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.RandomUtils.*;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.PrivateerMission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * Objects of this class contains AI-information for a single REF player.
 *
 * For now, mostly just the EuropeanAIPlayer, with a few tweaks.
 */
public class REFAIPlayer extends EuropeanAIPlayer {

    private static final Logger logger = Logger.getLogger(REFAIPlayer.class.getName());

    /** Limit on the number of REF units chasing a single hostile unit. */
    private static final int UNIT_USAD_THRESHOLD = 5;

    /** Container class for REF target colony information. */
    private static class TargetTuple implements Comparable<TargetTuple> {

        public final Colony colony;
        public final PathNode path;
        public double score;
        public Tile disembarkTile;
        public Tile entry;


        public TargetTuple(Colony colony, PathNode path, double score) {
            this.colony = colony;
            this.path = path;
            this.score = score;
            this.disembarkTile = null;
            this.entry = null;
            if (path != null) {
                for (PathNode p = path; p != null; p = p.next) {
                    Tile t = p.getTile();
                    if (t != null) {
                        this.entry = t;
                        break;
                    }
                }
            }
        }

        // Implement Comparable<TargetTuple>

        @Override
        public int compareTo(TargetTuple other) {
            double cmp = other.score - score;
            return (cmp < 0.0) ? -1 : (cmp > 0.0) ? 1 : 0;
        }

        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object other) {
            if (other instanceof TargetTuple) {
                return this.compareTo((TargetTuple)other) == 0;
            }
            return super.equals(other);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int hash = super.hashCode();
            hash = 37 * hash + Utils.hashCode(colony);
            hash = 37 * hash + Utils.hashCode(path);
            hash = 37 * hash + Utils.hashCode(score);
            hash = 37 * hash + Utils.hashCode(disembarkTile);
            return 37 * hash + Utils.hashCode(entry);
        }
    }

    private static final int seekAndDestroyRange = 12;

    /** Map of target to count. */
    private final Map<Location, Integer> targetMap = new HashMap<>();


    /**
     * Creates a new <code>REFAIPlayer</code>.
     *
     * @param aiMain The main AI-class.
     * @param player The player that should be associated with this
     *            <code>REFAIPlayer</code>.
     */
    public REFAIPlayer(AIMain aiMain, ServerPlayer player) {
        super(aiMain, player);

        uninitialized = getPlayer() == null;
    }

    /**
     * Creates a new <code>REFAIPlayer</code>.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public REFAIPlayer(AIMain aiMain,
                       FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, xr);

        uninitialized = getPlayer() == null;
    }


    /**
     * Find suitable colony targets.
     *
     * @param aiu The <code>AIUnit</code> to search with.
     * @param port If true, insist on the colonies being ports.
     * @param aiCarrier The <code>AIUnit</code> to use as a carrier.
     * @return A list of <code>TargetTuple</code> target choices.
     */
    private List<TargetTuple> findColonyTargets(AIUnit aiu, boolean port,
                                                AIUnit aiCarrier) {
        final Player player = getPlayer();
        final Unit unit = aiu.getUnit();
        final Unit carrier = aiCarrier.getUnit();
        final List<TargetTuple> targets = new ArrayList<>();
        for (Player p : player.getRebels()) {
            for (Colony c : p.getColonies()) {
                if (port && !c.isConnectedPort()) continue;
                PathNode path = unit.findPath(carrier, c, carrier, null);
                if (path == null) continue;
                int score = UnitSeekAndDestroyMission.scorePath(aiu, path);
                targets.add(new TargetTuple(c, path, score));
            }
        }

        // Increase score for drydock/s, musket and tools suppliers,
        // but decrease for fortifications.
        // FIXME: use Modifiers?
        final int percentTwiddle = 20; // Perturb score by +/-20%
        int[] twiddle = randomInts(logger, "REF target twiddle",
                                   getAIRandom(), 2*percentTwiddle+1,
                                   targets.size());
        int twidx = 0;
        for (TargetTuple t : targets) {
            t.score *= 0.01 * (101 - Math.min(100, t.colony.getSoL()));
            for (Building b : t.colony.getBuildings()) {
                if (b.getLevel() > 1) {
                    if (b.hasAbility(Ability.REPAIR_UNITS)) t.score *= 1.5;
                    for (AbstractGoods ag : b.getOutputs()) {
                        if (ag.getType().isMilitaryGoods()) {
                            t.score *= 2.0;
                        } else if (ag.getType().isBuildingMaterial()
                            && ag.getType().isRefined()) {
                            t.score *= 1.5;
                        }
                    }
                }
            }
            int stockade = (!t.colony.hasStockade()) ? 0
                : t.colony.getStockade().getLevel();
            t.score *= (6 - stockade) / 6.0;
            t.score *= 1.0 + 0.01 * (twiddle[twidx++] - percentTwiddle);
        }
        Collections.sort(targets);

        LogBuilder lb = new LogBuilder(64);
        lb.add("REF found colony targets:");
        for (TargetTuple t : targets) lb.add(" ", t.colony, "(", t.score, ")");
        lb.log(logger, Level.FINE);
        return targets;
    }

    /**
     * Initialize the REF.
     * - Find the initial target
     * - Give valid missions to all the units
     *
     * Note that we can not rely on normal AI processing as the
     * "teleporting" Col1-style REF needs to be placed on the map
     * before its first turn starts, so the server should ask this
     * AI where it should arrive on the map.
     *
     * Note also that to find a target we can not just call
     * getMilitaryMission and aim for it as the getMilitaryMission
     * scoring includes distance from starting point, which is what we
     * are trying to determine.
     * So, just choose the best coastal colony.
     *
     * FIXME: Mission assignment is done here because ATM the European
     * AI is prone to send ships full of troops off to attack the
     * rebel navy.  If this is fixed check if the normal mission
     * assignment works and drop it from here.
     *
     * @param teleport "Teleporting" in is allowed.
     * @return True if the initialization succeeds.
     */
    public boolean initialize(boolean teleport) {
        final AIMain aiMain = getAIMain();
        final Random aiRandom = getAIRandom();
        // Find a representative offensive land unit to use to search
        // for the initial target.
        AIUnit aiUnit = find(getAIUnits(), aiu -> !aiu.getUnit().isNaval()
            && aiu.getUnit().isOffensiveUnit());
        if (aiUnit == null) {
            logger.warning("REF has no army?!?");
            return false;
        }
        final Unit unit = aiUnit.getUnit();
        final Unit carrier = unit.getCarrier();
        if (carrier == null) {
            logger.warning("REF land unit not on a carrier: " + unit);
            return false;
        }
        final AIUnit aiCarrier = aiMain.getAIUnit(carrier);
        if (aiCarrier == null) {
            logger.warning("REF naval unit missing: " + carrier);
            return false;
        }

        List<TargetTuple> targets = findColonyTargets(aiUnit, true, aiCarrier);
        if (targets.isEmpty()) {
            logger.warning("REF found no targets.");
            return false;
        }

        final Player rebel = targets.get(0).colony.getOwner();
        double ratio = getStrengthRatio(rebel);
        int n = targets.size();
        LogBuilder lb = new LogBuilder(64);
        lb.add("REF attacking ", rebel.getName(), " ratio=", ratio);

        // For each target search from the target position to find a
        // Tile to disembark to.  If teleporting in, the navy will
        // appear at this location, otherwise at the best entry
        // location for it.
        int fail = 0;
        for (int i = 0; i < n; i++) {
            final TargetTuple t = targets.get(i);
            final GoalDecider gd = GoalDeciders
                .getDisembarkGoalDecider(t.colony.getTile());
            PathNode path = unit.search(t.entry, gd, null, 10, carrier);
            if (path == null) {
                t.disembarkTile = null;
                fail++;
            } else {
                // Step forward to the point the unit is about to
                // disembark.  This is where the carrier should teleport to.
                t.disembarkTile = path.getTransportDropNode()
                    .previous.getTile();
            }
        }
        if (fail > 0) {
            if (fail < n) { // Drop targets without a decent disembark tile
                lb.add(" (");
                int i = 0;
                while (i < targets.size()) {
                    final TargetTuple t = targets.get(i);
                    if (t.disembarkTile == null) {
                        lb.add(" ", t.colony);
                        targets.remove(i);
                        n--;
                    } else {
                        i++;
                    }
                }
                lb.add(")");
            } else { // They were all bad, just use the existing simple path
                for (int i = 0; i < n; i++) {
                    final TargetTuple t = targets.get(i);
                    t.disembarkTile = t.path.getTransportDropNode()
                        .previous.getTile();
                }
            }
        }
        // Reset N, now we have eliminated bad landing sites.
        n = (ratio < 1.0) ? 1 // Just go for one place
            : (ratio < 2.0) ? Math.min(2, targets.size())
            : Math.min(3, targets.size());
        lb.add(" => #targets=", n);

        if (!teleport) {
            // Try to arrive on the map without being seen, while retaining
            // a path to the disembark tile that is at worst one turn
            // slower than the existing one.
            GoalDecider stealthGD = GoalDeciders.getComposedGoalDecider(true,
                GoalDeciders.getHighSeasGoalDecider(),
                GoalDeciders.getStealthyGoalDecider(rebel));
            for (int i = 0; i < n; i++) {
                final TargetTuple t = targets.get(i);
                if (!rebel.canSee(t.entry)) continue;
                PathNode path = carrier.search(t.disembarkTile, stealthGD,
                    CostDeciders.avoidSettlementsAndBlockingUnits(),
                    t.path.getTotalTurns() + 1, null);
                if (path != null) {
                    t.entry = path.getLastNode().getTile();
                    t.score *= 1.5; // Prefer invisible paths
                }
            }
            Collections.sort(targets); // Re-sort with new scores
        }

        // Give the land units seek-and-destroy missions for the
        // target.  A valid target is needed before giving the carrier
        // a valid transport missions.  Send roughly 2/3 of the force
        // at the best target, decreasing from there.
        List<AIUnit> navy = new ArrayList<>();
        Iterator<AIUnit> auIterator = getAIUnits().iterator();
        int land = getPlayer().getNumberOfKingLandUnits();
        int used;
        Mission m;
        for (int i = 0; i < n; i++) {
            if (!auIterator.hasNext()) break;
            final TargetTuple t = targets.get(i);
            lb.add("\n  Attack ", t.colony, " from ", t.entry,
                   " via ", t.disembarkTile, " with ");
            while (auIterator.hasNext()) {
                AIUnit aiu = auIterator.next();
                if (!aiu.getUnit().isNaval()) continue;
                Unit ship = aiu.getUnit();
                if (ship.isEmpty()) {
                    navy.add(aiu);
                    continue;
                }
                if (teleport) {
                    ship.setEntryLocation(t.disembarkTile);
                } else {
                    ship.setEntryLocation(t.entry);
                }
                lb.add("[", ship);
                lb.mark();
                used = 0;
                for (Unit u : aiu.getUnit().getUnitList()) {
                    AIUnit laiu = aiMain.getAIUnit(u);
                    m = getSeekAndDestroyMission(laiu, t.colony);
                    if (m != null) lb.add(" ", m);
                    used++;
                }
                m = getTransportMission(aiu);
                lb.grew(" ", m);
                lb.add(" ]");
                if (i < n-1 && used >= (int)Math.floor(land * 0.66)) {
                    land -= used;
                    break;
                }
            }
        }

        // Try to find some rebel naval units near the entry locations
        // for the targets.
        final List<Unit> rebelNavy = new ArrayList<>();
        final GoalDecider navyGD = new GoalDecider() {
                @Override
                public PathNode getGoal() { return null; }
                @Override
                public boolean hasSubGoals() { return true; }
                @Override
                public boolean check(Unit unit, PathNode pathNode) {
                    Tile tile = pathNode.getTile();
                    if (tile != null && !tile.isEmpty()
                        && !tile.isLand()
                        && rebel.owns(tile.getFirstUnit())) {
                        for (Unit u : tile.getUnitList()) {
                            if (u.isOffensiveUnit() && u.isNaval()
                                && !rebelNavy.contains(u)) rebelNavy.add(u);
                        }
                    }
                    return false;
                }
            };
        for (int i = 0; i < n; i++) {
            carrier.search(targets.get(i).entry, navyGD, null,
                           carrier.getInitialMovesLeft() * 2, null);
        }

        // Attack naval targets.
        final Comparator<Unit> militaryStrength
            = getGame().getCombatModel().getMilitaryStrengthComparator();
        Collections.sort(rebelNavy, militaryStrength);
        Iterator<Unit> ui = rebelNavy.iterator();
        List<Tile> entries = new ArrayList<>();
        entries.add(rebel.getEntryLocation().getTile());
        while (!navy.isEmpty()) {
            final AIUnit aiu = navy.remove(0);
            final Unit u = aiu.getUnit();
            final Unit enemy = (ui.hasNext()) ? ui.next() : null;
            Tile start;
            if (enemy == null) {
                if ((m = getWanderHostileMission(aiu)) != null) {
                    start = getRandomMember(logger, "REF patrol entry",
                                            entries, aiRandom);
                    u.setEntryLocation(start);
                    lb.add("\n  Patrol from ", start, " with ", m);
                }
            } else {
                if ((m = getSeekAndDestroyMission(aiu, enemy)) != null) {
                    start = u.getBestEntryTile(enemy.getTile());
                    u.setEntryLocation(start);
                    entries.add(start);
                    lb.add("\n  Suppress ", enemy, " from ", start,
                        " with ", m);
                }
            }
        }
        lb.log(logger, Level.FINE);
        return true;
    }

    /**
     * Require more transport missions, recruiting from the privateering
     * missions.
     *
     * @param nt The number of transport missions required.
     * @param transports The list of <code>AIUnit</code>s with a transport
     *     mission.
     * @param privateers The list of <code>AIUnit</code>s with a privateer
     *     mission.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return A list of new <code>AIUnit</code>s with transport missions.
     */
    private List<AIUnit> requireTransports(int nt, List<AIUnit> transports,
                                           List<AIUnit> privateers,
                                           LogBuilder lb) {
        Mission m;
        List<AIUnit> naval = new ArrayList<>();
        List<AIUnit> result = new ArrayList<>();
        if (transports.size() < nt) {
            // Recruit privateers not currently chasing a unit.
            // Collect privateers that are on the map.
            for (AIUnit aiu : privateers) {
                Location target = aiu.getMission().getTarget();
                if (target instanceof Unit && aiu.getUnit().hasTile()) {
                    naval.add(aiu);
                } else if ((m = getTransportMission(aiu)) != null) {
                    lb.add(" notarget ", m);
                    result.add(aiu);
                }
            }
        }
        if (transports.size() < nt) {
            // Sort by longest distance to target
            Collections.sort(naval, new Comparator<AIUnit>() {
                    @Override
                    public int compare(AIUnit a1, AIUnit a2) {
                        int d1 = a1.getMission(PrivateerMission.class)
                            .getDistanceToTarget();
                        int d2 = a2.getMission(PrivateerMission.class)
                            .getDistanceToTarget();
                        return d1 - d2;
                    }
                });
            while (!naval.isEmpty()) {
                AIUnit aiu = naval.remove(0);
                int distance = aiu.getMission(PrivateerMission.class)
                    .getDistanceToTarget();
                if ((m = getTransportMission(aiu)) != null) {
                    lb.add(" REQUIRED ", distance, " ", m);
                    result.add(aiu);
                    if (result.size() + transports.size() >= nt) break;
                }
            }
        }
        privateers.removeAll(result);
        transports.addAll(result);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Stance determineStance(Player other) {
        final Player player = getPlayer();
        // The REF is always at war with its own rebels.
        return (other.getREFPlayer() == player)
            ? ((other.isRebel()) ? Stance.WAR : Stance.PEACE)
            : (other.atWarWith(player)) ? Stance.WAR
            : (!player.getRebels().isEmpty()) ? Stance.PEACE // Focus!
            : super.determineStance(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void giveNormalMissions(LogBuilder lb) {
        final Player player = getPlayer();
        final Map<Location, List<AIUnit>> idlers = new HashMap<>();
        List<AIUnit> privateers = new ArrayList<>();
        List<AIUnit> transports = new ArrayList<>();
        List<AIUnit> todo = new ArrayList<>();
        List<AIUnit> land = new ArrayList<>();
        Mission m;
        Colony colony;
        lb.add("\n  REF mission changes:");

        // Collect the REF units, the privateers, the transports, the
        // unemployed navy, and the unemployed land units.
        targetMap.clear();
        for (AIUnit aiu : getAIUnits()) {
            Unit u = aiu.getUnit();
            if (u.isDisposed() || !u.hasAbility(Ability.REF_UNIT)) continue;
            Mission mission = aiu.getMission();
            if (u.isNaval()) {
                if (mission == null || !mission.isValid()) {
                    todo.add(aiu);
                } else if (mission instanceof TransportMission) {
                    transports.add(aiu);
                } else if (mission instanceof PrivateerMission) {
                    privateers.add(aiu);
                    Location loc = mission.getTarget();
                    if (loc != null) incrementMapCount(targetMap, loc);
                } else {
                    todo.add(aiu);
                }
            } else {
                if (mission == null) {
                    land.add(aiu);
                } else if (mission instanceof DefendSettlementMission) {
                    if (mission.isValid()) {
                        colony = (Colony)mission.getTarget();
                        // Bleed off excessive defenders.
                        if (u.isAtLocation(colony)
                            && !colony.isBadlyDefended()
                            && randomInt(logger, "REF defend " + colony.getName(), 
                                         getAIRandom(), 3) == 0) {
                            land.add(aiu);
                        } else {
                            incrementMapCount(targetMap, mission.getTarget());
                        }                          
                    } else {
                        land.add(aiu);
                    }                    
                } else if (mission instanceof UnitSeekAndDestroyMission) {
                    if (mission.isValid()) {
                        incrementMapCount(targetMap, mission.getTarget());
                        continue;
                    }
                    land.add(aiu);
                } else {
                    land.add(aiu);
                }
            }
        }

        // Use free naval units as transports.
        for (AIUnit aiu : todo) {
            if ((m = getTransportMission(aiu)) != null) {
                lb.add(" ", m);
                transports.add(aiu);
            }
        }
        todo.clear();

        // Insist on a minimum number of transports.
        int nt = Math.max(3, privateers.size() / 10);
        requireTransports(nt, transports, privateers, lb);

        // Use up the free land units:
        // - mop up nearby hostile targets (but do not all rush after
        //   one loose wagon!)
        // - if idle at a well defended port, consider further attacks below
        // - defend the closest settlement needing defence
        // - defend the closest port
        // - go idle in a port
        for (AIUnit aiu : land) {
            Location target = UnitSeekAndDestroyMission.findTarget(aiu, 
                seekAndDestroyRange, false);
            if (target != null) {
                int count = (targetMap.containsKey(target))
                    ? targetMap.get(target) : 0;
                if (target instanceof Unit
                    && count < UNIT_USAD_THRESHOLD
                    && (m = getSeekAndDestroyMission(aiu, target)) != null) {
                    lb.add(" NEW-SEEK-", count, " ", m);
                    incrementMapCount(targetMap, target);
                    continue;
                } else if (target instanceof Settlement
                    && (m = getSeekAndDestroyMission(aiu, target)) != null) {
                    lb.add(" NEW-SEEK ", m);
                    incrementMapCount(targetMap, target);
                    continue;
                } else {
                    throw new RuntimeException("Bogus target: " + target);
                }
            }

            // Find units idle at a port
            final Unit u = aiu.getUnit();
            if (u.isInEurope()) {
                appendToMapList(idlers, player.getEurope(), aiu);
                continue;
            } else if ((colony = u.getColony()) != null
                && colony.isConnectedPort()) {
                appendToMapList(idlers, colony, aiu);
                continue;
            }

            // Go defend the nearest colony needing defence
            Colony best = u.getClosestColony(getBadlyDefended().stream()
                .map(AIColony::getColony));
            if (best != null
                && (m = getDefendSettlementMission(aiu, best)) != null) {
                lb.add(" GO-DEFEND-", best.getName(), " " , m);
                incrementMapCount(targetMap, best);
                continue;
            }

            // Just go defend the nearest port.  Once there and enough
            // defenders are clearly allocated, some will be made available
            // to launch new attacks.
            PathNode path = u.findOurNearestPort();
            colony = (path == null) ? null
                : path.getLastNode().getTile().getColony();
            if (colony != null
                && (m = getDefendSettlementMission(aiu, colony)) != null) {
                lb.add(" GOTO-", colony.getName(), " " , m);
                incrementMapCount(targetMap, colony);
                continue;
            }

            // Just go somewhere and idle.
            m = getIdleAtSettlementMission(aiu);
            lb.add(" ", m);
        }

        // Try to find new attacks for units left idling at ports.
        if (!idlers.isEmpty()) {
            // See what transport is present at a colony already.
            requireTransports(0, transports, privateers, lb);
            todo.clear();
            Map<Location, List<AIUnit>> ready = new HashMap<>();
            for (AIUnit aiu : transports) {
                TransportMission tm = aiu.getMission(TransportMission.class);
                if (!tm.isEmpty()) continue;
                Unit u = aiu.getUnit();
                Location key;
                if (u.isInEurope()
                    && idlers.containsKey(key = player.getEurope())) {
                    appendToMapList(ready, key, aiu);
                } else if ((key = u.getColony()) != null
                    && idlers.containsKey(key)) {
                    appendToMapList(ready, key, aiu);
                } else {
                    todo.add(aiu);
                }
            }

            // If there are idle units and carriers present at the
            // same colony, load the carriers and launch new USAD
            // missions with them.  Collect the ports that still
            // contain idle units, and accumulate the amount of space
            // needed to move the units.
            List<Location> idlePorts = new ArrayList<>();
            List<AIUnit> aiCarriers = new ArrayList<>();
            int space = 0;
            for (Entry<Location, List<AIUnit>> e : idlers.entrySet()) {
                if (e.getValue() == null) continue;
                aiCarriers.clear();
                Location key = e.getKey();
                if (!ready.containsKey(key)
                    || ready.get(key).isEmpty()) continue; // No carrier here
                landUnit: for (AIUnit aiu : e.getValue()) {
                    for (AIUnit aiCarrier : ready.get(key)) {
                        Unit carrier = aiCarrier.getUnit();
                        if (carrier.canAdd(aiu.getUnit())
                            && aiu.joinTransport(carrier, null)) {
                            if (!aiCarriers.contains(aiCarrier)) {
                                aiCarriers.add(aiCarrier);
                            }
                            continue landUnit;
                        }
                    }
                }
                if (aiCarriers.isEmpty()) continue; // Did not load

                // Choose a target colony.  Pick a representative unit
                // to plan with, but if it happens to have a target already
                // keep that.
                AIUnit found = null;
                Colony target = null;
                for (AIUnit aiCarrier : aiCarriers) {
                    for (Unit u : aiCarrier.getUnit().getUnitList()) {
                        if (u.hasAbility(Ability.REF_UNIT)
                            && (found = getAIUnit(u)) != null) break;
                    }
                    if (found != null
                        && (m = found.getMission()) != null
                        && m.isValid()
                        && m instanceof UnitSeekAndDestroyMission
                        && m.getTarget() instanceof Colony) {
                        target = (Colony)m.getTarget();
                        break;
                    }
                }
                if (target == null) {
                    AIUnit aiCarrier = getAIUnit(found.getUnit().getCarrier());
                    List<TargetTuple> ct = findColonyTargets(found, true,
                                                             aiCarrier);
                    if (ct.isEmpty()) {
                        ct = findColonyTargets(found, false, aiCarrier);
                    }
                    if (!ct.isEmpty()) {
                        target = ct.get(0).colony;
                    }
                }
                if (target == null) continue; // No target for these idlers

                // Send them to destroy the target
                for (AIUnit aiCarrier : aiCarriers) {
                    TransportMission tm
                        = aiCarrier.getMission(TransportMission.class);
                    AIUnit aiu;
                    for (Unit u : aiCarrier.getUnit().getUnitList()) {
                        if (u.hasAbility(Ability.REF_UNIT)
                            && (aiu = getAIUnit(u)) != null
                            && (m = getSeekAndDestroyMission(aiu, target)) != null) {
                            lb.add(" IDLER->", target, " ", m);
                            tm.queueTransportable(aiu, false, lb);
                            e.getValue().remove(aiu);
                        }
                    }
                }

                // Are there more idle units waiting here?
                if (!e.getValue().isEmpty()) {
                    idlePorts.add(key);
                    space += e.getValue().stream()
                        .mapToInt(aiu -> aiu.getUnit().getSpaceTaken()).sum();
                }
            }

            if (!idlePorts.isEmpty()) {
                // Do we need to switch more units from privateering
                // to transport?
                for (AIUnit aiu : todo) {
                    space -= aiu.getUnit().getCargoCapacity()
                        - aiu.getUnit().getCargoSpaceTaken();
                }
                nt = todo.size();
                if (space < 0) {
                    nt += -space / 5 + 1; // Quick and dirty hack
                    requireTransports(nt, todo, privateers, lb);
                }

                // Send transports to the idle ports, preferring the ones
                // with the most units.
                Collections.sort(idlePorts, new Comparator<Location>() {
                        @Override
                        public int compare(Location l1, Location l2) {
                            return idlers.get(l1).size() - idlers.get(l2).size();
                        }
                    });
                boolean bad = false;
                while (!bad && !todo.isEmpty()) {
                    for (Location l : idlePorts) {
                        int bestValue = Unit.MANY_TURNS;
                        AIUnit best = null;
                        for (AIUnit aiu : todo) {
                            int value = aiu.getUnit().getTurnsToReach(l);
                            if (bestValue > value) {
                                bestValue = value;
                                best = aiu;
                            }
                        }
                        if (best == null) {
                            bad = true;
                            continue;
                        }
                        todo.remove(best);
                        best.getMission().setTarget(l);
                        lb.add(" retarget ", best, " to ", l,
                            "(", idlers.get(l).size(), ")");
                    }
                }
            }
        }

        // Fall back to the normal EuropeanAI behaviour for remaining units.
        super.giveNormalMissions(lb);
    }


    // AI Player interface
    // Inherit everything from EuropeanAIPlayer except the following overrides.

    /**
     * {@inheritDoc}
     */
    @Override
    public void startWorking() {
        final Player player = getPlayer();
        if (!player.isWorkForREF()) {
            logger.warning("No work for REF: " + player);
            return;
        }

        super.startWorking();

        // Always allocate transport for all land units in Europe.
        List<TransportMission> transport = new ArrayList<>();
        List<TransportableAIObject> land = new ArrayList<>();
        for (AIUnit aiu : getAIUnits()) {
            final Unit u = aiu.getUnit();
            if (u.isNaval()) {
                if (aiu.hasMission(TransportMission.class)) {
                    transport.add(aiu.getMission(TransportMission.class));
                }
            } else {
                if (u.isInEurope()) land.add(aiu);
            }
        }
        if (!land.isEmpty() && !transport.isEmpty()) {
            LogBuilder lb = new LogBuilder(256);
            allocateTransportables(land, transport, lb);
            lb.log(logger, Level.FINE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int adjustMission(AIUnit aiUnit, PathNode path, Class type,
                             int value) {
        if (value > 0) {
            if (type == DefendSettlementMission.class) {
                // REF garrisons thinly.
                Location loc = DefendSettlementMission.extractTarget(aiUnit, path);
                if (loc instanceof Colony && !((Colony)loc).isBadlyDefended()) {
                    return Integer.MIN_VALUE;
                }
            } else if (type == UnitSeekAndDestroyMission.class) {
                Location target = UnitSeekAndDestroyMission
                    .extractTarget(aiUnit, path);
                if (target instanceof Settlement) {
                    // Value connected settlements highly.
                    // Initially, accept no others.
                    if (((Settlement)target).isConnectedPort()) {
                        value += 500;
                    } else {
                        if (getPlayer().getNumberOfSettlements() <= 0) {
                            return Integer.MIN_VALUE;
                        }
                    }
                } else if (target instanceof Unit) {
                    // Do not chase units until at least one colony is captured.
                    if (getPlayer().getNumberOfSettlements() <= 0) {
                        return Integer.MIN_VALUE;
                    }
                    // Do not chase the same unit!
                    if (any(getAIUnits().stream()
                            .filter(aiu -> aiu != aiUnit)
                            .map(aiu -> aiu.getMission(UnitSeekAndDestroyMission.class)),
                            m -> m != null
                                && m.getTarget() instanceof Unit
                                && (Unit)m.getTarget() == target))
                        return Integer.MIN_VALUE;
                    // The REF is more interested in colonies.
                    value /= 2;
                }
            }
        }
        return value;
    }
}
