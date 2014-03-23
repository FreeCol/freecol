/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
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

    private static int seekAndDestroyRange = 12;


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
     * @param colonies An array of <code>Colony</code>s to fill in.
     * @param port If true, insist on the colonies being ports.
     * @return The number of colonies filled into the array.
     */
    private int findColonyTargets(AIUnit aiu, Colony[] colonies, boolean port) {
        final Player player = getPlayer();
        final Unit unit = aiu.getUnit();
        final Unit carrier = unit.getCarrier();
        final Map<Colony, Double> scores = new HashMap<Colony, Double>();
        for (Player p : player.getRebels()) {
            for (Colony c : p.getColonies()) {
                if (!port || c.isConnectedPort()) {
                    PathNode path = unit.findPath(carrier, c, carrier, null);
                    int score = UnitSeekAndDestroyMission.scorePath(aiu, path);
                    scores.put(c, (double)score);
                }
            }
        }

        // Increase score for drydock/s, musket and tools suppliers,
        // but decrease for fortifications.
        // TODO: use Modifiers?
        for (Colony c : scores.keySet()) {
            double score = scores.get(c);
            score *= 0.01 * (101 - Math.min(100, c.getSoL()));
            for (Building b : c.getBuildings()) {
                if (b.getLevel() > 1) {
                    if (b.hasAbility(Ability.REPAIR_UNITS)) score *= 1.5;
                    for (AbstractGoods ag : b.getOutputs()) {
                        if (ag.getType().isMilitaryGoods()) {
                            score *= 2.0;
                        } else if (ag.getType().isBuildingMaterial()
                            && ag.getType().isRefined()) {
                            score *= 1.5;
                        }
                    }
                }
            }
            int stockade = (!c.hasStockade()) ? 0
                : c.getStockade().getLevel();
            score *= (6 - stockade) / 6.0;
            scores.put(c, score);
        }

        Colony[] best = scores.keySet().toArray(new Colony[0]);
        Arrays.sort(best, new Comparator<Colony>() {
                public int compare(Colony c1, Colony c2) {
                    double cmp = scores.get(c2) - scores.get(c1);
                    return (cmp < 0) ? -1 : (cmp > 0) ? 1 : 0;
                }
            });
        int n = Math.min(colonies.length, best.length);
        for (int i = 0; i < n; i++) colonies[i] = best[i];

        StringBuffer sb = new StringBuffer(32);
        sb.append("REF found colony targets:");
        for (int i = 0; i < n; i++) {
            sb.append(" ").append(colonies[i].getName())
                .append("(").append(scores.get(colonies[i])).append(")");
        }
        logger.fine(sb.toString());
        return n;
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

        Colony best[] = { null, null };
        int n = findColonyTargets(aiUnit, best, true);
        if (n == 0) {
            logger.warning("REF found no targets.");
            return false;
        }
        final Player rebel = best[0].getOwner();
        double ratio = getStrengthRatio(rebel);
        if (ratio < 1.0) n = 1; // Just go for one place

        // For each target search from the target position to find a
        // Tile to disembark to.  If teleporting in, the navy will appear
        // at this location, otherwise at the best entry location for it.
        Tile[] entry = new Tile[n];
        for (int i = 0; i < n; i++) {
            final Tile target = best[i].getTile();
            entry[i] = carrier.getBestEntryTile(target);
            final GoalDecider gd = GoalDeciders.getDisembarkGoalDecider(target);
            PathNode path = unit.search(entry[i], gd, null, 10, carrier);
            if (path == null) {
                logger.severe("Can not find suitable REF landing site for: "
                    + upLoc(target));
                return false;
            }
            entry[i] = path.getTile();
            if (teleport) {
                // Step forward to the point the unit is about to
                // disembark.  This is where the carrier should
                // teleport to.
                while (path.isOnCarrier()) path = path.next;
                entry[i] = path.previous.getTile();
            }
        }

        // Give the land units seek-and-destroy missions for the
        // target.  A valid target is needed before giving the carrier
        // a valid transport missions.  Send roughly 2/3 of the force
        // at the best target, decreasing from there.
        StringBuffer sb = new StringBuffer(256);
        List<AIUnit> navy = new ArrayList<AIUnit>();
        int land = getPlayer().getNumberOfKingLandUnits();
        int used, bIndex = 0;
        sb.append("REF attacking\n  [").append(upLoc(best[bIndex]))
            .append(" from ").append(entry[bIndex])
            .append(" with ");
        for (AIUnit aiu : getAIUnits()) {
            if (!aiu.getUnit().isNaval()) continue;
            Unit ship = aiu.getUnit();
            if (ship.isEmpty()) navy.add(aiu); else {
                sb.append("[").append(ship);
                used = 0;
                for (Unit u : aiu.getUnit().getUnitList()) {
                    AIUnit laiu = aiMain.getAIUnit(u);
                    laiu.setMission(new UnitSeekAndDestroyMission(aiMain,
                            laiu, best[bIndex]));
                    used++;
                    sb.append(" ").append(u);
                }
                sb.append("]");
                TransportMission tm = new TransportMission(aiMain, aiu);
                aiu.setMission(tm);
                if (bIndex < n-1 && used >= (int)Math.floor(land * 0.66)) {
                    land -= used;
                    used = 0;
                    bIndex++;
                    sb.append("]\n  [").append(upLoc(best[bIndex]))
                        .append(" from " ).append(entry[bIndex])
                        .append(" with ");
                }
                ship.setEntryLocation(entry[bIndex]);
            }
        }
        sb.append("]");

        // Try to find some rebel naval units near the entry locations
        // for the targets.
        final List<Unit> rebelNavy = new ArrayList<Unit>();
        final GoalDecider navyGD = new GoalDecider() {
                public PathNode getGoal() { return null; }
                public boolean hasSubGoals() { return true; }
                public boolean check(Unit unit, PathNode pathNode) {
                    Tile tile = pathNode.getTile();
                    if (tile != null && !tile.isEmpty()
                        && (!tile.isLand() || tile.hasSettlement())
                        && rebel.owns(tile.getFirstUnit())) {
                        for (Unit u : tile.getUnitList()) {
                            if (u.isOffensiveUnit()
                                && !rebelNavy.contains(u)) rebelNavy.add(u);
                        }
                    }
                    return false;
                }
            };
        for (int i = 0; i < n; i++) {
            carrier.search(entry[i], navyGD, null,
                           carrier.getInitialMovesLeft() * 2, null);
        }

        // Attack naval targets.
        Comparator<Unit> militaryStrength
            = Unit.getMilitaryStrengthComparator(getGame().getCombatModel());
        Collections.sort(rebelNavy, militaryStrength);
        Iterator<Unit> ui = (rebelNavy.isEmpty()) ? null
            : rebelNavy.iterator();
        String nonspecific = "\n  [nonspecific-naval with";
        if (ui == null) sb.append(nonspecific);
        while (!navy.isEmpty()) {
            final AIUnit aiu = navy.remove(0);
            final Unit u = aiu.getUnit();
            if (ui != null && !ui.hasNext()) {
                ui = null;
                sb.append(nonspecific);
            }
            if (ui == null) {
                aiu.setMission(new UnitWanderHostileMission(aiMain, aiu));
                u.setEntryLocation(entry[0]);
            } else {
                Unit target = ui.next();
                aiu.setMission(new UnitSeekAndDestroyMission(aiMain, aiu,
                                                             target));
                Tile start = u.getBestEntryTile(target.getTile());
                u.setEntryLocation(start);
                sb.append("\n  [").append(target)
                    .append(" from ").append(start)
                    .append(" with ").append(u).append("]");
            }
        }
        sb.append("]");
        logger.info(sb.toString());
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
     * Gives a mission to non-naval units.
     */
    @Override
    public void giveNormalMissions() {
        // Give military missions to all REF units.
        for (AIUnit aiu : getAIUnits()) {
            Unit u = aiu.getUnit();
            if (u.isNaval() || aiu.hasMission()) continue;
            if (u.isOffensiveUnit()) {
                Location target = UnitSeekAndDestroyMission.findTarget(aiu, 
                    seekAndDestroyRange, false);
                Mission m = (target == null)
                    ? new UnitWanderHostileMission(getAIMain(), aiu)
                    : new UnitSeekAndDestroyMission(getAIMain(), aiu, target);
                aiu.setMission(m);
            }
        }

        // Fall back to the normal EuropeanAI behaviour for non-army.
        super.giveNormalMissions();
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
                        Mission m = au.getMission();
                        Location loc;
                        if (m != null
                            && m instanceof UnitSeekAndDestroyMission
                            && (loc = ((UnitSeekAndDestroyMission)m)
                                .getTarget()) != null
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
