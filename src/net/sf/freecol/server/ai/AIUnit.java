/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Colony;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitLocation;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.mission.BuildColonyMission;
import net.sf.freecol.server.ai.mission.CashInTreasureTrainMission;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.IdleAtSettlementMission;
import net.sf.freecol.server.ai.mission.IndianBringGiftMission;
import net.sf.freecol.server.ai.mission.IndianDemandMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.MissionaryMission;
import net.sf.freecol.server.ai.mission.PioneeringMission;
import net.sf.freecol.server.ai.mission.PrivateerMission;
import net.sf.freecol.server.ai.mission.ScoutingMission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.ai.mission.UnitWanderMission;
import net.sf.freecol.server.ai.mission.WishRealizationMission;
import net.sf.freecol.server.ai.mission.WorkInsideColonyMission;


/**
 * Objects of this class contains AI-information for a single {@link Unit}.
 *
 * AIUnits are successfully initialized by a call to
 * {@link #setUnit(Unit)} with a non-null actual {@link Unit}.

 * The method {@link #doMission(LogBuilder)} is called once each turn,
 * by {@link AIPlayer#startWorking()}, to perform the assigned
 * {@code Mission}.  Most of the methods in this class just
 * delegates the call to that mission.
 *
 * @see Mission
 */
public final class AIUnit extends TransportableAIObject {

    private static final Logger logger = Logger.getLogger(AIUnit.class.getName());

    public static final String TAG = "aiUnit";

    /** The Unit this AIObject contains AI-information for. */
    private Unit unit;

    /** The mission to which this AI unit has been assigned. */
    private Mission mission;


    /**
     * Creates a new uninitialized {@code AIUnit}.
     *
     * @param aiMain The main AI-object.
     * @param id The object identifier.
     */
    public AIUnit(AIMain aiMain, String id) {
        super(aiMain, id);

        this.unit = null;
        this.mission = null;
        this.initialized = false;
    }

    /**
     * Creates a new {@code AIUnit}.
     *
     * @param aiMain The main AI-object.
     * @param unit The unit to make an {@link AIObject} for.
     */
    public AIUnit(AIMain aiMain, Unit unit) {
        this(aiMain, unit.getId());

        this.unit = unit;
        this.mission = null;
        setInitialized();
    }

    /**
     * Creates a new {@code AIUnit} from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public AIUnit(AIMain aiMain,
                  FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, xr);

        setInitialized();
    }


    /**
     * {@inheritDoc}
     */
    public void setInitialized() {
        this.initialized = getUnit() != null;
    }

    /**
     * Gets the {@code Unit} this {@code AIUnit} controls.
     *
     * @return The {@code Unit}.
     */
    public final Unit getUnit() {
        return this.unit;
    }

    /**
     * Set the {@code Unit} this {@code AIUnit} controls.
     *
     * Note: it is an error to change the unit once it is set.
     *
     * @param unit The new {@code Unit}.
     */
    private final void setUnit(Unit unit) {
        if (this.unit != null && unit != this.unit) {
            throw new RuntimeException("Attempt to change AI Unit " + getId()
                + " to " + unit
                + "\n" + net.sf.freecol.common.debug.FreeColDebugger.stackTraceToString());
        }
        this.unit = unit;
    }

    /**
     * Checks if this unit has been assigned a mission.
     *
     * @return True if this unit has a mission.
     */
    public final boolean hasMission() {
        return this.mission != null;
    }

    /**
     * Gets the mission this unit has been assigned.
     *
     * @return The {@code Mission}.
     */
    public final Mission getMission() {
        return this.mission;
    }

    /**
     * Assigns a mission to unit. 
     *
     * @param mission The new {@code Mission}.
     */
    public final void setMission(Mission mission) {
        this.mission = mission;
    }


    // Internal

    /**
     * Request a rearrangement of any colony at the current location.
     */
    private void requestLocalRearrange() {
        Location loc;
        Colony colony;
        AIColony aiColony;
        final Unit unit = getUnit();
        if (unit != null
            && (loc = unit.getLocation()) != null
            && (colony = loc.getColony()) != null
            && (aiColony = getAIMain().getAIColony(colony)) != null) {
            aiColony.requestRearrange();
        }
    }

    /**
     * Take the current carrier as transport, keeping the transport mission/s
     * consistent.
     */
    private void takeTransport() {
        Unit carrier = getUnit().getCarrier();
        AIUnit aiCarrier = (carrier == null) ? null
            : getAIMain().getAIUnit(carrier);
        AIUnit transport = getTransport();
        if (transport != aiCarrier) {
            if (transport != null) {
                logger.warning("Taking different transport: " + aiCarrier);
                dropTransport();
            }
            setTransport(aiCarrier);
        }
    }


    // Public routines

    /**
     * Get the {@code Player} that owns this AIUnit.
     *
     * @return The owning {@code Player}.
     */
    public Player getOwner() {
        final Unit unit = getUnit();
        return (unit == null) ? null : unit.getOwner();
    }

    /**
     * Gets the AIPlayer that owns this AIUnit.
     *
     * @return The owning AIPlayer.
     */
    public AIPlayer getAIOwner() {
        final Player owner = getOwner();
        return (owner == null) ? null : getAIMain().getAIPlayer(owner);
    }

    /**
     * Gets the PRNG to use with this unit.
     *
     * @return A {@code Random} instance.
     */
    public Random getAIRandom() {
        AIPlayer aiOwner = getAIOwner();
        return (aiOwner == null) ? null : aiOwner.getAIRandom();
    }

    /**
     * Get a trivial target, usually a safe nearby settlement or Europe.
     *
     * @return A trivial target, or null if none found.
     */
    public Location getTrivialTarget() {
        final Unit unit = getUnit();
        PathNode path;
        return (unit == null
            || (path = unit.getTrivialPath()) == null) ? null
            : Location.upLoc(path.getLastNode().getLocation());
    }

    /**
     * Is this AI unit carrying any cargo (units or goods).
     *
     * @return True if the unit has cargo aboard.
     */
    public final boolean hasCargo() {
        final Unit unit = getUnit();
        return (unit == null) ? false : unit.hasCargo();
    }

    /**
     * Does this unit have a particular class of mission?
     *
     * @param <T> The type of the mission.
     * @param returnClass The {@code Class} of mission to check.
     * @return True if the mission is of the given class.
     */
    public <T extends Mission> boolean hasMission(Class<T> returnClass) {
        return getMission(returnClass) != null;
    }

    /**
     * Get the unit mission if it is of a given class.
     *
     * @param <T> The type of the mission.
     * @param returnClass The {@code Class} of the mission.
     * @return The {@code Mission}, or null if it is not of the
     *     given class.
     */
    public <T extends Mission> T getMission(Class<T> returnClass) {
        try {
            return returnClass.cast(this.mission);
        } catch (ClassCastException cce) {
            return null;
        }
    }

    /**
     * Performs the mission this unit has been assigned.
     *
     * Do *not* check mission validity.  The mission itself does that,
     * and has special case error handling.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    public void doMission(LogBuilder lb) {
        if (this.mission != null) this.mission.doMission(lb);
    }

    /**
     * Change the mission of a unit.
     *
     * @param mission The new {@code Mission}.
     * @return The new current {@code Mission}.
     */
    public Mission changeMission(Mission mission) {
        if (this.mission == mission) return this.mission;

        removeMission();
        setMission(mission);
        if (mission != null) {
            setTransportPriority(mission.getBaseTransportPriority());
        }
        return this.mission;
    }

    public void removeMission() {
        if (this.mission != null) {
            this.mission.dispose();
            this.mission = null;
        }
    }


    // Helper methods for AIColony

    public boolean hasDefendSettlementMission() {
        return hasMission(DefendSettlementMission.class);
    }

    public boolean isCompleteWishRealizationMission(Colony colony) {
        WishRealizationMission wm
            = getMission(WishRealizationMission.class);
        return (wm != null && Map.isSameLocation(wm.getTarget(), colony));
    }

    public boolean isAvailableForWork(Colony colony) {
        Mission m = getMission();
        return (m == null
            || (m instanceof BuildColonyMission
                && (Map.isSameLocation(m.getTarget(), colony.getTile())
                    || colony.getUnitCount() <= 1))
            // FIXME: drop this when the AI stops building excessive armies
            || m instanceof DefendSettlementMission
            || m instanceof IdleAtSettlementMission
            || m instanceof WorkInsideColonyMission
            );
    }

    public boolean tryWorkInsideColonyMission(AIColony aiColony, LogBuilder lb) {
        WorkInsideColonyMission wic
            = getMission(WorkInsideColonyMission.class);
        if (wic == null) {
            AIPlayer aiPlayer = getAIOwner();
            if (!(aiPlayer instanceof EuropeanAIPlayer) ||
                    ((EuropeanAIPlayer)aiPlayer)
                    .getWorkInsideColonyMission(this, aiColony) == null) {
                return false; 
            }
            lb.add(", ", getMission());
            dropTransport();
        }
        return true;
    }

    public boolean tryPioneeringMission(LogBuilder lb) {
        Mission m = getMission();
        Location oldTarget = (m == null) ? null : m.getTarget();
        AIPlayer aiPlayer = getAIOwner();

        if (aiPlayer instanceof EuropeanAIPlayer) {
            EuropeanAIPlayer euaiPlayer = (EuropeanAIPlayer)aiPlayer;
            if (euaiPlayer.getPioneeringMission(this, null) != null) {
                lb.add(", ", getMission());
                euaiPlayer.updateTransport(this, oldTarget, lb);
                return true;
            }
        }
        return false;
    }

    public boolean trySomeUsefulMission(Colony colony, LogBuilder lb) {
        Mission m = getMission();
        if (m instanceof BuildColonyMission
            || m instanceof DefendSettlementMission
            || m instanceof MissionaryMission
            || m instanceof PioneeringMission
            || m instanceof ScoutingMission
            || m instanceof UnitSeekAndDestroyMission)
            return true;
        Location oldTarget = (m == null) ? null : m.getTarget();
        AIPlayer aiPlayer = getAIOwner();

        final Unit unit = getUnit();
        if(aiPlayer instanceof EuropeanAIPlayer) {
            EuropeanAIPlayer euaiPlayer = (EuropeanAIPlayer)getAIOwner();

            if (unit.hasAbility(Ability.SPEAK_WITH_CHIEF)
                && (m = euaiPlayer.getScoutingMission(this)) != null) {
                lb.add(", ", m);
                euaiPlayer.updateTransport(this, oldTarget, lb);
                return true;
            } else if (unit.isDefensiveUnit()
                && (m = euaiPlayer.getDefendSettlementMission(this, colony)) != null) {
                lb.add(", ", m);
                euaiPlayer.updateTransport(this, oldTarget, lb);
                return true;
            } else if (unit.hasAbility(Ability.ESTABLISH_MISSION)
                && (m = euaiPlayer.getMissionaryMission(this)) != null) {
                lb.add(", ", m);
                euaiPlayer.updateTransport(this, oldTarget, lb);
                return true;
            }
        }
        return false;
    }

    public void removeTransportable(AIGoods ag) {
        TransportMission tm = getMission(TransportMission.class);
        if (tm != null) {
            tm.removeTransportable(ag);
        }
    }

    /**
     * Moves a unit to the new world.
     *
     * @return True if there was no c-s problem.
     */
    public boolean moveToAmerica() {
        return AIMessage.askMoveTo(this, getOwner().getGame().getMap());
    }

    /**
     * Moves a unit to Europe.
     *
     * @return True if there was no c-s problem.
     */
    public boolean moveToEurope() {
        return AIMessage.askMoveTo(this, getOwner().getEurope());
    }

    /**
     * Moves this AI unit.
     *
     * @param direction The {@code Direction} to move.
     * @return True if the move succeeded.
     */
    public boolean move(Direction direction) {
        final Unit unit = getUnit();
        Tile start = unit.getTile();
        return unit.getMoveType(direction).isProgress()
            && AIMessage.askMove(this, direction)
            && unit.getTile() != start;
    }

    /**
     * Equips this AI unit for a particular role.
     *
     * The unit must be at a location where the required goods are available
     * (possibly requiring a purchase, which may fail due to lack of gold
     * or boycotts in effect).
     *
     * @param role The {@code Role} to equip for identifier.
     * @return True if the role change was successful.
     */
    public boolean equipForRole(Role role) {
        final Player player = getOwner();
        final Unit unit = getUnit();
        Location loc = Location.upLoc(unit.getLocation());
        if (!(loc instanceof UnitLocation)) return false;
        int count = role.getMaximumCount();
        if (count > 0) {
            for (; count > 0; count--) {
                List<AbstractGoods> req = unit.getGoodsDifference(role, count);
                try {
                    int price = ((UnitLocation)loc).priceGoods(req);
                    if (player.checkGold(price)) break;
                } catch (FreeColException fce) {
                    continue;
                }
            }
            if (count <= 0) return false;
        }
        return AIMessage.askEquipForRole(this, role, count)
            && unit.getRole() == role
            && unit.getRoleCount() == count;
    }

    /**
     * Score this AI unit with its suitability for building.
     *
     * Favour unequipped freeColonists, and other unskilled over experts.
     * Also slightly favour units on the map.
     *
     * @return An integer score.
     */
    public int getBuilderScore() {
        final Unit unit = getUnit();
        if (unit == null || BuildColonyMission.invalidMissionReason(this) != null)
            return -1000;
        int ret = (!unit.hasDefaultRole()) ? 0
            : (unit.getSkillLevel() > 0) ? 100
            : 500 + 100 * unit.getSkillLevel();
        if (unit.hasTile()) ret += 50;
        return ret;
    }        

    /**
     * Score this AI unit with its suitability for pioneering.
     *
     * @return An integer score.
     */
    public int getPioneerScore() {
        final Unit unit = getUnit();
        return (unit == null) ? -1000 : unit.getPioneerScore();
    }

    /**
     * Score this AI unit with its suitability for scouting.
     *
     * @return An integer score.
     */
    public int getScoutScore() {
        final Unit unit = getUnit();
        return (unit == null) ? -1000 : unit.getScoutScore();
    }

    
    // Implement TransportableAIObject

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTransportPriority() {
        return (hasMission()) ? super.getTransportPriority() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Locatable getTransportLocatable() {
        return getUnit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportSource() {
        final Unit unit = getUnit();
        return (unit == null || unit.isDisposed()) ? null
            : unit.getLocation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportDestination() {
        final Unit unit = getUnit();
        return (unit == null || unit.isDisposed() || !hasMission())
            ? null
            : this.mission.getTransportDestination();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PathNode getDeliveryPath(Unit carrier, Location dst) {
        if (dst == null) {
            dst = getTransportDestination();
            if (dst == null) return null;
        }
        dst = Location.upLoc(dst);

        PathNode path;
        final Unit unit = getUnit();
        if (unit.getLocation() == carrier) {
            path = unit.findPath(carrier.getLocation(), dst, carrier);
            if (path == null && dst.getTile() != null) {
                path = unit.findPathToNeighbour(carrier.getLocation(),
                    dst.getTile(), carrier, null);
            }
        } else if (unit.getLocation() instanceof Unit) {
            return null;
        } else {
            path = unit.findPath(unit.getLocation(), dst, carrier);
            if (path == null && dst.getTile() != null) {
                path = unit.findPathToNeighbour(unit.getLocation(),
                    dst.getTile(), carrier, null);
            }
        }
        if (path != null) path.ensureDisembark();
        return path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PathNode getIntermediatePath(Unit carrier, Location dst) {
        return null; // NYI
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTransportDestination(Location destination) {
        throw new RuntimeException("AI unit transport destination set by mission:" + destination);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean carriableBy(Unit carrier) {
        return carrier.couldCarry(getUnit());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canMove() {
        return getUnit().getMovesLeft() > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean leaveTransport() {
        final Unit unit = getUnit();
        if (!unit.isOnCarrier()) return true; // Harmless error

        // Just leave at once if in Europe
        if (unit.isInEurope()) return leaveTransport(null);

        // Otherwise if not on the map, do nothing.
        final Tile tile = unit.getTile();
        if (tile == null) return false;

        // Try to go to the target location.
        final Mission mission = getMission();
        final Location target = (mission == null || !mission.isValid()) ? null
            : mission.getTarget();
        Direction direction;
        if (target != null) {
            if (Map.isSameLocation(target, tile)) return leaveTransport(null);
            if (target.getTile() != null
                && (direction = tile.getDirection(target.getTile())) != null) {
                return leaveTransport(direction);
            }
            PathNode path = unit.findPath(target); // Not using carrier!
            if (path != null
                && (direction = tile.getDirection(path.next.getTile())) != null) {
                try {
                    return leaveTransport(direction);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Leave transport crash for "
                        + this + "/" + unit.getMovesLeft(), e);
                }
            }
        }

        // Just get off here if possible.
        if (tile.isLand()) return leaveTransport(null);

        // Collect neighbouring land tiles, get off if one has our settlement
        List<Tile> tiles = new ArrayList<>();
        for (Tile t : tile.getSurroundingTiles(1)) {
            if (!t.isBlocked(unit)) {
                if (t.getSettlement() != null) {
                    return leaveTransport(tile.getDirection(t));
                }
                tiles.add(t);
            }
        }

        // No adjacent unblocked tile, fail.
        if (tiles.isEmpty()) return false;

        // Pick the available tile with the shortest path to one of our
        // settlements, or the tile with the highest defence value.
        final Player player = getOwner();
        Tile safe = tiles.get(0);
        Tile best = null;
        int bestTurns = Unit.MANY_TURNS;
        Settlement settlement = null;
        for (Tile t : tiles) {
            if (settlement == null || t.isConnectedTo(settlement.getTile())) {
                settlement = t.getNearestSettlement(player, 10, true);
            }
            if (settlement != null) {
                int turns = unit.getTurnsToReach(t, settlement);
                if (bestTurns > turns) {
                    bestTurns = turns;
                    best = t;
                }
            }
            if (safe.getDefenceValue() < t.getDefenceValue()) {
                safe = t;
            }
        }
        return leaveTransport(tile.getDirection((best != null) ? best : safe));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean leaveTransport(Direction direction) {
        final Unit unit = getUnit();
        if (!unit.isOnCarrier()) return false;
        final Unit carrier = unit.getCarrier();
        boolean result = (direction == null)
            ? (AIMessage.askDisembark(this)
                && unit.getLocation() == carrier.getLocation())
            : move(direction);
        if (result) {
            requestLocalRearrange();
            dropTransport();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean joinTransport(Unit carrier, Direction direction) {
        AIUnit aiCarrier = getAIMain().getAIUnit(carrier);
        if (aiCarrier == null) return false;
        final Unit unit = getUnit();
        boolean result = AIMessage.askEmbark(aiCarrier, unit, direction)
            && unit.getLocation() == carrier;

        if (result) {
            requestLocalRearrange();
            takeTransport();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String invalidReason() {
        String reason = Mission.invalidTransportableReason(this);
        return (reason != null) ? reason
            : (hasMission()) ? getMission().invalidReason()
            : null;
    }


    // Override AIObject

    /**
     * Disposes this object and any attached mission.
     */
    @Override
    public void dispose() {
        dropTransport();
        AIPlayer aiOwner = getAIOwner();
        // Might not be owned by an AI
        if (aiOwner != null) aiOwner.removeAIObject(this);

        if (this.mission != null) {
            this.mission.dispose();
            this.mission = null;
        }
        super.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        IntegrityType result = super.checkIntegrity(fix, lb);
        final Unit unit = getUnit();
        if (unit == null) {
            lb.add("\n  AIUnit with null unit: ", getId());
            result = result.fail();
        } else if (unit.isDisposed()) {
            lb.add("\n  AIUnit with disposed unit: ", getId());
            result = result.fail();
        }
        return result;
    }


    // Serialization


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (mission != null && !mission.isOneTime() && mission.isValid()) {
            mission.toXML(xw);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final AIMain aiMain = getAIMain();

        Unit u = xr.findFreeColGameObject(aiMain.getGame(), ID_ATTRIBUTE_TAG,
                                          Unit.class, (Unit)null, true);
        if (u.isInitialized()) {
            setUnit(u);
        } else {
            xr.nextTag(); // Move off the opening <AIUnit> tag
            throw new XMLStreamException("AIUnit for uninitialized Unit: "
                + u.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final AIMain aiMain = getAIMain();
        final String tag = xr.getLocalName();

        mission = null;
        if (BuildColonyMission.TAG.equals(tag)) {
            mission = new BuildColonyMission(aiMain, this, xr);

        } else if (CashInTreasureTrainMission.TAG.equals(tag)) {
            mission = new CashInTreasureTrainMission(aiMain, this, xr);

        } else if (DefendSettlementMission.TAG.equals(tag)) {
            mission = new DefendSettlementMission(aiMain, this, xr);

        } else if (IdleAtSettlementMission.TAG.equals(tag)) {
            mission = new IdleAtSettlementMission(aiMain, this, xr);

        } else if (IndianBringGiftMission.TAG.equals(tag)) {
            mission = new IndianBringGiftMission(aiMain, this, xr);

        } else if (IndianDemandMission.TAG.equals(tag)) {
            mission = new IndianDemandMission(aiMain, this, xr);

        } else if (MissionaryMission.TAG.equals(tag)) {
            mission = new MissionaryMission(aiMain, this, xr);

        } else if (PioneeringMission.TAG.equals(tag)) {
            mission = new PioneeringMission(aiMain, this, xr);

        } else if (PrivateerMission.TAG.equals(tag)) {
            mission = new PrivateerMission(aiMain, this, xr);

        } else if (ScoutingMission.TAG.equals(tag)) {
            mission = new ScoutingMission(aiMain, this, xr);

        } else if (TransportMission.TAG.equals(tag)) {
            mission = new TransportMission(aiMain, this, xr);

        } else if (UnitSeekAndDestroyMission.TAG.equals(tag)) {
            mission = new UnitSeekAndDestroyMission(aiMain, this, xr);

        } else if (UnitWanderHostileMission.TAG.equals(tag)) {
            mission = new UnitWanderHostileMission(aiMain, this, xr);

        } else if (UnitWanderMission.TAG.equals(tag)) {
            mission = new UnitWanderMission(aiMain, this, xr);

        } else if (WishRealizationMission.TAG.equals(tag)) {
            mission = new WishRealizationMission(aiMain, this, xr);

        } else if (WorkInsideColonyMission.TAG.equals(tag)) {
            mission = new WorkInsideColonyMission(aiMain, this, xr);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AIUnit)) return false;
        AIUnit other = (AIUnit)o;
        return Utils.equals(this.unit, other.unit)
            && Utils.equals(this.mission, other.mission)
            && super.equals(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 37 * hash + Utils.hashCode(this.unit);
        return 37 * hash + Utils.hashCode(this.mission);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return (this.unit == null) ? "AIUnit-null"
            : this.unit.toString("AIUnit ");
    }
}
