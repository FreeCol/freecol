/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * Objects of this class contains AI-information for a single REF player.
 *
 * For now, mostly just the EuropeanAIPlayer, with a few tweaks.
 */
public class REFAIPlayer extends EuropeanAIPlayer {

    private static final Logger logger = Logger.getLogger(REFAIPlayer.class.getName());

    /** Container class for REF target colony information. */
    private static class TargetTuple implements Comparable<TargetTuple> {

        public Colony colony;
        public PathNode path;
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
     * @return A list of <code>TargetTuple</code> target choices.
     */
    private List<TargetTuple> findColonyTargets(AIUnit aiu, boolean port) {
        final Player player = getPlayer();
        final Unit unit = aiu.getUnit();
        final Unit carrier = unit.getCarrier();
        final List<TargetTuple> targets = new ArrayList<TargetTuple>();
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
        // TODO: use Modifiers?
        final int percentTwiddle = 20; // Perturb score by +/-20%
        int[] twiddle = Utils.randomInts(logger, "REF target twiddle",
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
        AIUnit aiUnit = null;
        for (AIUnit aiu : getAIUnits()) {
            if (!aiu.getUnit().isNaval() && aiu.getUnit().isOffensiveUnit()) {
                aiUnit = aiu;
                break;
            }
        }
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

        List<TargetTuple> targets = findColonyTargets(aiUnit, true);
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
        List<AIUnit> navy = new ArrayList<AIUnit>();
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
        final List<Unit> rebelNavy = new ArrayList<Unit>();
        final GoalDecider navyGD = new GoalDecider() {
                public PathNode getGoal() { return null; }
                public boolean hasSubGoals() { return true; }
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
        Comparator<Unit> militaryStrength
            = Unit.getMilitaryStrengthComparator(getGame().getCombatModel());
        Collections.sort(rebelNavy, militaryStrength);
        Iterator<Unit> ui = rebelNavy.iterator();
        List<Tile> entries = new ArrayList<Tile>();
        entries.add(rebel.getEntryLocation().getTile());
        while (!navy.isEmpty()) {
            final AIUnit aiu = navy.remove(0);
            final Unit u = aiu.getUnit();
            final Unit enemy = (ui.hasNext()) ? ui.next() : null;
            Tile start;
            if (enemy == null) {
                if ((m = getWanderHostileMission(aiu)) != null) {
                    start = Utils.getRandomMember(logger, "REF patrol entry",
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
     * {@inheritDoc}
     */
    @Override
    protected Stance determineStance(Player other) {
        Player player = getPlayer();
        // The REF is always at war with rebels.
        return (other.getREFPlayer() == player
            && other.getPlayerType() == Player.PlayerType.REBEL) ? Stance.WAR
            : super.determineStance(other);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void giveNormalMissions(LogBuilder lb) {
        // Give military missions to all REF units.
        lb.add("\n  Military mission changes:");
        for (AIUnit aiu : getAIUnits()) {
            Unit u = aiu.getUnit();
            if (u.isDisposed() || u.isNaval() || aiu.hasMission()) continue;
            if (u.hasAbility(Ability.REF_UNIT)) {
                Location target = UnitSeekAndDestroyMission.findTarget(aiu, 
                    seekAndDestroyRange, false);
                Mission m = (target == null)
                    ? getWanderHostileMission(aiu)
                    : getSeekAndDestroyMission(aiu, target);
                lb.add(" ", m);
            }
        }

        // Fall back to the normal EuropeanAI behaviour for non-army.
        super.giveNormalMissions(lb);
    }


    // AI Player interface
    // Inherit everything from EuropeanAIPlayer except the following overrides.

    /**
     * {@inheritDoc}
     */
    public void startWorking() {
        final Player player = getPlayer();
        if (!player.isWorkForREF()) {
            logger.warning("No work for REF: " + player);
            return;
        }
        super.startWorking();
    }

    /**
     * {@inheritDoc}
     */
    public int adjustMission(AIUnit aiUnit, PathNode path, Class type,
                             int value) {
        if (value > 0) {
            if (type == DefendSettlementMission.class) {
                // REF garrisons thinly.
                Location loc = DefendSettlementMission.extractTarget(aiUnit, path);
                if (loc instanceof Settlement
                    && getSettlementDefenders((Settlement)loc) > 0) value = 0;
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
                    for (AIUnit au : getAIUnits()) {
                        Mission m = au.getMission(UnitSeekAndDestroyMission.class);
                        Location loc;
                        if (m != null
                            && (loc = m.getTarget()) != null
                            && loc instanceof Unit
                            && loc == target) return Integer.MIN_VALUE;
                    }
                    // The REF is more interested in colonies.
                    value /= 2;
                }
            }
        }
        return value;
    }
}
