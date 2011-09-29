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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * Objects of this class contains AI-information for a single REF player.
 *
 * For now, mostly just the EuropeanAIPlayer, with a few tweaks.
 */
public class REFAIPlayer extends EuropeanAIPlayer {

    private static final Logger logger = Logger.getLogger(REFAIPlayer.class.getName());


    /**
     * Creates a new <code>REFAIPlayer</code>.
     *
     * @param aiMain The main AI-class.
     * @param player The player that should be associated with this
     *            <code>REFAIPlayer</code>.
     */
    public REFAIPlayer(AIMain aiMain, ServerPlayer player) {
        super(aiMain, player);
    }

    /**
     * Creates a new <code>REFAIPlayer</code> and reads the
     * information from the given <code>Element</code>.
     *
     * @param aiMain The main AI-class.
     * @param element The XML-element containing information.
     */
    public REFAIPlayer(AIMain aiMain, Element element) {
        super(aiMain, element);
    }

    /**
     * Creates a new <code>REFAIPlayer</code>.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public REFAIPlayer(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain, in);
    }


    /**
     * Tells this <code>REFAIPlayer</code> to make decisions.
     */
    public void startWorking() {
        final Player player = getPlayer();
        logger.finest("Entering method startWorking: "
            + player + ", year " + getGame().getTurn());
        if (!player.isWorkForREF()) {
            logger.warning("No work for REF: " + player);
            return;
        }
        super.startWorking();
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
     * @return The preferred entry location, or null if no useful preference.
     */
    public Tile initialize(boolean teleport) {
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
            logger.warning("New REF has no army?!?");
            return null;
        }
        Unit unit = aiUnit.getUnit();

        // Find the best coastal colony.
        Colony target = null;
        int bestScore = Integer.MIN_VALUE;
        Player player = getPlayer();
        for (Player p : player.getRebels()) {
            for (Colony c : p.getColonies()) {
                if (c.isConnected()) {
                    int score = getUnitSeekAndDestroyMissionValue(unit,
                        c.getTile(), INFINITY);
                    if (score > bestScore) {
                        bestScore = score;
                        target = c;
                    }
                }
            }
        }
        if (target == null) {
            logger.warning("Rebels have no connected colonies?!?");
            return null;
        }
        Tile tile = target.getTile();

        // Give the army seek-and-destroy missions for the target,
        // then once the army has missions it is possible to give the
        // navy valid transport missions where needed.
        final AIMain aiMain = getAIMain();
        for (AIUnit aiu : getAIUnits()) {
            if (!aiu.getUnit().isNaval()) {
                aiu.setMission(new UnitSeekAndDestroyMission(aiMain, aiu,
                        target));
            }
        }
        for (AIUnit aiu : getAIUnits()) {
            if (aiu.getUnit().isNaval()) {
                Unit ship = aiu.getUnit();
                if (ship.getUnitCount() > 0) {
                    TransportMission tm = new TransportMission(aiMain, aiu);
                    for (Unit u : ship.getUnitList()) {
                        tm.addToTransportList(aiMain.getAIUnit(u));
                    }
                    aiu.setMission(tm);
                }
            }
        }

        // Search from the target position to find a Tile to disembark
        // to, which must be:
        // - Unoccupied
        // - Have an unoccupied connected neighbour
        //
        // TODO: pick the tile with the best defence and try to avoid
        // hostile fortifications.
        Map map = getGame().getMap();
        GoalDecider gd = new GoalDecider() {
                private PathNode goal = null;

                public PathNode getGoal() {
                    return goal;
                }

                public boolean hasSubGoals() {
                    return false;
                }

                public boolean check(Unit u, PathNode pathNode) {
                    if (!pathNode.getTile().isEmpty()) return false;
                    for (Tile t : pathNode.getTile().getSurroundingTiles(1)) {
                        if (t.isConnected() && t.isEmpty()) {
                            goal = pathNode;
                            return true;
                        }
                    }
                    return false;
                }
            };
        PathNode path = map.search(unit, tile, gd, 10, null);
        if (path == null) {
            logger.warning("Can not find suitable REF landing site for: "
                + target);
            return null;
        }
        tile = path.getTile();

        // If teleporting in, the connected tile is an acceptable target.
        for (Tile t : tile.getSurroundingTiles(1)) {
            if (t.isConnected() && t.isEmpty()) {
                tile = t;
                break;
            }
        }
        if (teleport) return tile;

        // Unit should be aboard a man-o-war which we can use to find a
        // path to Europe.  Use the end of that path.
        if (unit.getLocation() instanceof Unit) {
            path = map.findPathToEurope((Unit) unit.getLocation(), tile);
            if (path == null) {
                logger.warning("Can not find path to Europe from: " + tile);
                return null;
            }
            return path.getLastNode().getTile();
        }
        logger.warning("REF land unit not aboard a ship: " + unit);
        return null;
    }

    /**
     * Evaluate allocating a unit to the defence of a colony.
     * Temporary helper method for giveMilitaryMission.
     *
     * Only public for testAssignDefendSettlementMission.
     *
     * @param unit The <code>Unit</code> that is to defend.
     * @param colony The <code>Colony</code> to defend.
     * @param turns The turns for the unit to reach the colony.
     * @return A value for such a mission.
     */
    @Override
    public int getDefendColonyMissionValue(Unit unit, Colony colony,
                                           int turns) {
        int value = super.getDefendColonyMissionValue(unit, colony, turns);

        // The REF garrisons thinly.
        return (getColonyDefenders(colony) > 0) ? 0 : value;
    }

    /**
     * Evaluate a potential seek and destroy mission for a given unit
     * to a given tile.
     *
     * @param unit The <code>Unit</code> to do the mission.
     * @param newTile The <code>Tile</code> to go to.
     * @param turns How long to travel to the tile.
     * @return A score for the proposed mission.
     */
    @Override
    public int getUnitSeekAndDestroyMissionValue(Unit unit, Tile newTile,
                                                 int turns) {
        int value = super.getUnitSeekAndDestroyMissionValue(unit, newTile,
                                                            turns);
        if (value <= 0) return value;

        Settlement settlement = newTile.getSettlement();
        Unit defender = newTile.getDefendingUnit(unit);
        if (settlement == null) {
            if (unit.getOwner().getNumberOfSettlements() <= 0) {
                // Do not chase units until at least one colony is captured.
                return Integer.MIN_VALUE;
            }
            // Do not all chase the one unit!
            if (alreadySeeking(defender)) return Integer.MIN_VALUE;
            // The REF is more interested in colonies.
            value /= 2;
        } else {
            if (settlement.isConnected()) value += 1000;
        }
        return value;
    }

    /**
     * Checks if there is already a seek and destroy mission active on
     * for a target unit.
     *
     * @param unit The <code>Unit</code> to check if there is a mission for.
     * @return True if there is a mission for the unit.
     */
    private boolean alreadySeeking(Unit unit) {
        for (AIUnit au : getAIUnits()) {
            Mission m = au.getMission();
            Location target;
            if (m != null
                && m instanceof UnitSeekAndDestroyMission
                && (target = ((UnitSeekAndDestroyMission) m).getTarget()) != null
                && target instanceof Unit
                && ((Unit)target) == unit) return true;
        }
        return false;
    }

    /**
     * Gives a mission to non-naval units.
     */
    @Override
    public void giveNormalMissions() {
        // Give military missions to all offensive units.
        for (AIUnit aiu : getAIUnits()) {
            Unit u = aiu.getUnit();
            if (u.isNaval() || aiu.hasMission()) continue;
            if (u.isOffensiveUnit()) giveMilitaryMission(aiu);
        }

        // Fall back to the normal EuropeanAI behaviour for non-army.
        super.giveNormalMissions();
    }
}
