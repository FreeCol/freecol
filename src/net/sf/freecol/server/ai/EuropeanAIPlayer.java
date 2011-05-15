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
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.ai.mission.WorkInsideColonyMission;

/**
 *
 * Objects of this class contains AI-information for a single {@link Player} and
 * is used for controlling this player.
 *
 * @deprecated Currently unused, outdated copy of {@link AIPlayer}. Eventual
 * specialization of AI should extend AIPlayer, see {@link ColonialAIPlayer}.
 *
 * The method {@link #startWorking} gets called by the
 * {@link AIInGameInputHandler} when it is this player's turn.
 */
public abstract class EuropeanAIPlayer extends NewAIPlayer {

    private static final Logger logger = Logger.getLogger(EuropeanAIPlayer.class.getName());

    /**
     * Ask the server to train a unit in Europe on behalf of the AIPlayer
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
     * Ask the server to recruit a unit in Europe on behalf of the AIPlayer
     */
    public AIUnit recruitAIUnitInEurope(int slot) {
        AIUnit aiUnit = null;
        Europe europe = getPlayer().getEurope();
        int n = europe.getUnitCount();

        if (AIMessage.askEmigrate(getConnection(), slot)
            && europe.getUnitCount() == n+1) {
            aiUnit = getAIUnit(europe.getUnitList().get(n));
        }
        return aiUnit;
    }

    /**
     * Ensures that all workers inside a colony gets a
     * {@link WorkInsideColonyMission}.
     */
    protected void ensureCorrectMissions() {
        logger.finest("Entering method ensureCorrectMissions");
        Iterator<AIUnit> it = getAIUnitIterator();
        while (it.hasNext()) {
            AIUnit au = it.next();
            if (!au.hasMission()
                && (au.getUnit().getLocation() instanceof ColonyTile || au.getUnit().getLocation() instanceof Building)) {
                AIColony ac = (AIColony) getAIMain().getAIObject(au.getUnit().getColony());
                au.setMission(new WorkInsideColonyMission(getAIMain(), au, ac));
            }
        }
    }


    /**
     * Gives missions to all the naval units this player owns.
     */
    protected void giveNavalMissions() {
        logger.finest("Entering method giveNavalMissions");
        if (!getPlayer().isEuropean()) {
            return;
        }
        Iterator<AIUnit> aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = aiUnitsIterator.next();
            if (aiUnit.getUnit().isNaval() && !aiUnit.hasMission()) {
                // TODO: privateers should have military naval mission?
                aiUnit.setMission(new TransportMission(getAIMain(), aiUnit));
            }
        }
    }

    /**
     * Calls {@link AIColony#rearrangeWorkers} for every colony this player
     * owns.
     */
    protected void rearrangeWorkersInColonies() {
        logger.finest("Entering method rearrangeWorkersInColonies");
        if (!getPlayer().isEuropean()) {
            return;
        }
        Iterator<AIColony> ci = getAIColonyIterator();
        while (ci.hasNext()) {
            AIColony c = ci.next();
            ArrayList<Tile> oldWorkTiles = new ArrayList<Tile>();
            for (ColonyTile colonyTile : c.getColony().getColonyTiles()) {
                if (colonyTile.getUnit() != null) {
                    oldWorkTiles.add(colonyTile.getWorkTile());
                }
            }

            c.rearrangeWorkers(getConnection());
        }
    }

    /**
     * Takes the necessary actions to secure the settlements. This is done by
     * making new military units or to give existing units new missions.
     */
    protected void secureSettlements() {
        logger.finest("Entering method secureSettlements");
        // Temporarily deactive this feature:
        // Ok, we are a European getPlayer(). Things are about to get fun.
        /*
          for (Colony colony : getPlayer().getColonies()) {
          secureColony(colony);
          }
        */
    }


    /**
     * Takes the necessary actions to secure a european colony
     */
    protected void secureColony(Colony colony) {
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

        for (Tile t: colony.getTile().getSurroundingTiles(5)) {
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
                if (u.hasAbility("model.ability.expertSoldier")) {
                    recruits.add(u);
                } else if (u.hasAbility("model.ability.canBeEquipped")) {
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
            EquipmentType muskets = getGame().getSpecification().getEquipmentType("model.equipment.muskets");
            EquipmentType horses = getGame().getSpecification().getEquipmentType("model.equipment.horses");
            boolean needMuskets = false;
            boolean needHorses = false;
            ui = recruits.iterator();
            while (ui.hasNext() && recruitCount > 0) {
                Unit u = (ui.next());
                if (!u.isArmed() && u.canBeEquippedWith(muskets)) {
                    recruitCount--;
                    AIMessage.askEquipUnit(getAIUnit(u), muskets, 1);
                    AIMessage.askPutOutsideColony(getAIUnit(u));
                    // Check if the unit can fortify before sending the order
                    if (u.checkSetState(UnitState.FORTIFYING)) {
                        AIMessage.askChangeState(getAIUnit(u), UnitState.FORTIFYING);
                    }
                    olddefenders++;
                    if (!u.isMounted() && u.canBeEquippedWith(horses)) {
                        AIMessage.askEquipUnit(getAIUnit(u), horses, 1);
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
            GoodsType musketType = getAIMain().getGame().getSpecification().getGoodsType("model.goods.muskets");
            GoodsType horsesType = getAIMain().getGame().getSpecification().getGoodsType("model.goods.horses");
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
                    ac.addGoodsWish(new GoodsWish(getAIMain(), colony, (threat - olddefenders) * 50,
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

    /**
     * Calls {@link AIColony#createAIGoods()} for every colony this player owns.
     */
    protected void createAIGoodsInColonies() {
        logger.finest("Entering method createAIGoodsInColonies");
        Iterator<AIColony> ci = getAIColonyIterator();
        while (ci.hasNext()) {
            AIColony c = ci.next();
            c.createAIGoods();
        }
    }

    int getDefendColonyMissionValue(Unit u, Colony colony, int turns) {
        logger.finest("Entering method getDefendColonyMissionValue");

        // Sanitation
        if(colony == null)
            return Integer.MIN_VALUE;

        // Temporary helper method for: giveMilitaryMission
        int value = 10025 - turns;
        int numberOfDefendingUnits = 0;
        /*
         * Iterator ui = colony.getTile().getUnitIterator(); while
         * (ui.hasNext()) { Unit tu = (Unit) ui.next(); if
         * (tu.isDefensiveUnit()) { value -= 6; numberOfDefendingUnits++; } }
         */
        Iterator<AIUnit> aui = getAIUnitIterator();
        while (aui.hasNext()) {
            Mission m = aui.next().getMission();
            if (m != null && m instanceof DefendSettlementMission) {
                if (((DefendSettlementMission) m).getSettlement() == colony) {
                    //TODO: this decrease seems too little
                    value -= 6;
                    numberOfDefendingUnits++;
                }
            }
        }
        if (u.getOwner().isREF()) {
            value -= 19;
            if (numberOfDefendingUnits > 0) {
                return 0;
            }
        }
        //TODO: Does not take into consideration the various levels of
        //fortification, only if has one or not
        if (colony.getStockade() != null &&
            numberOfDefendingUnits > colony.getStockade().getLevel() + 1) {
            return Math.max(0, value - 9000);
        }
        return value;
    }

    /**
     * Gives a military <code>Mission</code> to the given unit. <br>
     * <br>
     * <b>This method should only be used on units owned by european players.</b>
     *
     * @param aiUnit The unit.
     */
    void giveMilitaryMission(AIUnit aiUnit) {
        logger.finest("Entering method giveMilitaryMission");
        /*
         *
         * Temporary method for giving a military mission.
         *
         * This method will be removed when "MilitaryStrategy" and
         *
         * the "Tactic"-classes has been implemented.
         *
         */
        if (getPlayer().isIndian()) {
            aiUnit.setMission(new UnitWanderHostileMission(getAIMain(), aiUnit));
            return;
        }
        Unit unit = aiUnit.getUnit();
        Unit carrier = (unit.isOnCarrier()) ? (Unit) unit.getLocation() : null;
        Map map = unit.getGame().getMap();
        // Initialize variables:
        Ownable bestTarget = null; // The best target for a mission.
        int bestValue = Integer.MIN_VALUE; // The value of the target above.
        // Determine starting tile:
        Tile startTile = unit.getTile();
        if (startTile == null) {
            startTile = ((unit.isOnCarrier()) ? ((Unit) unit.getLocation())
                         : unit).getFullEntryLocation();
            if (startTile == null) {
                logger.warning("Unable to determine entry location for: "
                               + unit.toString());
                return;
            }
        }
        /*
         *
         * Checks if we are currently located on a Tile with a Settlement
         *
         * which requires defenders:
         *
         */
        if (unit.getColony() != null) {
            bestTarget = unit.getColony();
            bestValue = getDefendColonyMissionValue(unit, (Colony) bestTarget, 0);
        }
        // Checks if a nearby colony requires additional defence:
        GoalDecider gd = new GoalDecider() {
                private PathNode best = null;

                private int bestValue = Integer.MIN_VALUE;


                public PathNode getGoal() {
                    return best;
                }

                public boolean hasSubGoals() {
                    return true;
                }

                public boolean check(Unit u, PathNode pathNode) {
                    Tile t = pathNode.getTile();
                    if (t.getColony() != null && t.getColony().getOwner() == u.getOwner()) {
                        int value = getDefendColonyMissionValue(u, t.getColony(), pathNode.getTurns());
                        if (value > 0 && value > bestValue) {
                            bestValue = value;
                            best = pathNode;
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
            };
        final int MAXIMUM_DISTANCE_TO_SETTLEMENT = 10; // Given in number of
        // turns.
        PathNode bestPath = map.search(unit, startTile, gd,
                                       MAXIMUM_DISTANCE_TO_SETTLEMENT, carrier);
        if (bestPath != null) {
            PathNode ln = bestPath.getLastNode();
            int value = getDefendColonyMissionValue(unit, ln.getTile().getColony(), ln.getTurns());
            if (value > bestValue) {
                bestTarget = ln.getTile().getColony();
                bestValue = value;
            }
        }
        // Searches for the closest target for an existing
        // "UnitSeekAndDestroyMission":
        Location bestExistingTarget = null;
        int smallestDifference = Integer.MAX_VALUE;
        Iterator<AIUnit> aui = getAIUnitIterator();
        while (aui.hasNext() && smallestDifference>0) {
            AIUnit coAIUnit = aui.next();
            Unit coUnit = coAIUnit.getUnit();
            if (coUnit.getTile() != null && coAIUnit.getMission() instanceof UnitSeekAndDestroyMission) {
                Location target = ((UnitSeekAndDestroyMission) coAIUnit.getMission()).getTarget();
                int ourDistance = unit.getTurnsToReach(startTile, target.getTile());
                int coUnitDistance = coUnit.getTurnsToReach(target.getTile());
                if (ourDistance != Integer.MAX_VALUE) {
                    int difference = Math.abs(ourDistance - coUnitDistance);
                    if (difference < smallestDifference) {
                        smallestDifference = difference;
                        bestExistingTarget = target;
                    }
                }
            }
        }
        if (bestExistingTarget != null) {
            int value = getUnitSeekAndDestroyMissionValue(unit, bestExistingTarget.getTile(), smallestDifference);
            if (value > bestValue) {
                bestValue = value;
                bestTarget = (Ownable) bestExistingTarget;
            }
        }
        // Checks if there is a better target than the existing one:
        GoalDecider targetDecider = new GoalDecider() {
                private PathNode bestTarget = null;

                private int bestNewTargetValue = Integer.MIN_VALUE;


                public PathNode getGoal() {
                    return bestTarget;
                }

                public boolean hasSubGoals() {
                    return true;
                }

                public boolean check(Unit unit, PathNode pathNode) {
                    Tile newTile = pathNode.getTile();
                    Unit defender = newTile.getDefendingUnit(unit);
                    if (isTargetValidForSeekAndDestroy(unit,defender)) {
                        int value = getUnitSeekAndDestroyMissionValue(unit, pathNode.getTile(), pathNode.getTurns());
                        if (value > bestNewTargetValue) {
                            bestTarget = pathNode;
                            bestNewTargetValue = value;
                            return true;
                        }
                    }
                    return false;
                }
            };
        PathNode newTarget = map.search(unit, startTile, targetDecider, Integer.MAX_VALUE,
                                        carrier);
        if (newTarget != null) {
            Tile targetTile = newTarget.getLastNode().getTile();
            int value = getUnitSeekAndDestroyMissionValue(unit, targetTile, newTarget.getTotalTurns());
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
        // Use the best target:
        if (bestTarget != null && bestValue > 0) {
            if (bestTarget.getOwner() == unit.getOwner()) {
                aiUnit.setMission(new DefendSettlementMission(getAIMain(), aiUnit, (Colony) bestTarget));
            } else {
                aiUnit.setMission(new UnitSeekAndDestroyMission(getAIMain(), aiUnit, (Location) bestTarget));
            }
        } else {
            // Just give a simple mission if we could not find a better one:
            aiUnit.setMission(new UnitWanderHostileMission(getAIMain(), aiUnit));
        }
    }

    /**
     * Returns an <code>Iterator</code> over all the
     * <code>TileImprovement</code>s needed by all of this player's colonies.
     *
     * @return The <code>Iterator</code>.
     * @see TileImprovement
     */
    public Iterator<TileImprovementPlan> getTileImprovementPlanIterator() {
        ArrayList<TileImprovementPlan> tileImprovements = new ArrayList<TileImprovementPlan>();
        Iterator<AIColony> acIterator = getAIColonyIterator();
        while (acIterator.hasNext()) {
            AIColony ac = acIterator.next();
            Iterator<TileImprovementPlan> it = ac.getTileImprovementPlanIterator();
            while (it.hasNext()) {
                tileImprovements.add(it.next());
            }
        }
        return tileImprovements.iterator();
    }

    /**
     * This is a temporary method which are used for forcing the computer
     * players into building more colonies. The method will be removed after the
     * proper code for deciding whether a colony should be built or not has been
     * implemented.
     *
     * @return <code>true</code> if the AI should build more colonies.
     */
    public boolean hasFewColonies() {
        if (!getPlayer().canBuildColonies()) {
            return false;
        }
        int numberOfColonies = 0;
        int numberOfWorkers = 0;
        for (Colony colony : getPlayer().getColonies()) {
            numberOfColonies++;
            numberOfWorkers += colony.getUnitCount();
        }

        boolean result = numberOfColonies <= 2
            || (numberOfColonies >= 3
                && numberOfWorkers / numberOfColonies > numberOfColonies - 2);
        logger.finest("hasFewColonies = " + result);
        return result;
    }

    /**
     * Maps <code>Transportable</code>s to carrier's using a
     * <code>TransportMission</code>.
     */
    protected void createTransportLists() {
        logger.finest("Entering method createTransportLists");
        if (!getPlayer().isEuropean()) {
            return;
        }
        ArrayList<Transportable> transportables = new ArrayList<Transportable>();

        // Add units
        Iterator<AIUnit> aui = getAIUnitIterator();
        while (aui.hasNext()) {
            AIUnit au = aui.next();
            // naval units do not need transports
            if(au.getUnit().isNaval()){
                continue;
            }
            if (au.getTransportDestination() != null && au.getTransport() == null) {
                transportables.add(au);
            }
        }

        // Add goods
        Iterator<AIColony> aci = getAIColonyIterator();
        while (aci.hasNext()) {
            AIColony ac = aci.next();
            Iterator<AIGoods> agi = ac.getAIGoodsIterator();
            while (agi.hasNext()) {
                AIGoods ag = agi.next();
                if (ag.getTransportDestination() != null && ag.getTransport() == null) {
                    transportables.add(ag);
                }
            }
        }

        Collections.sort(transportables, new Comparator<Transportable>() {
                public int compare(Transportable o1, Transportable o2) {
                    if (o1 == o2) {
                        return 0;
                    }
                    int result = o2.getTransportPriority() - o1.getTransportPriority();
                    if (result == 0) {
                        result = o1.getId().compareTo(o2.getId());
                    }
                    return result;
                }
            });

        ArrayList<Mission> vacantTransports = new ArrayList<Mission>();
        Iterator<AIUnit> iter = getAIUnitIterator();
        while (iter.hasNext()) {
            AIUnit au = iter.next();
            if (au.hasMission() && au.getMission() instanceof TransportMission
                && !(au.getUnit().getLocation() instanceof Europe)) {
                vacantTransports.add(au.getMission());
            }
        }

        Iterator<Transportable> ti = transportables.iterator();
        while (ti.hasNext()) {
            Transportable t = ti.next();
            t.increaseTransportPriority();
            if (t.getTransportLocatable().getLocation() instanceof Unit) {
                Mission m = ((AIUnit) getAIMain().getAIObject(
                                                              (FreeColGameObject) t.getTransportLocatable().getLocation())).getMission();
                if (m instanceof TransportMission) {
                    ((TransportMission) m).addToTransportList(t);
                }
                ti.remove();
            }
        }

        while (transportables.size() > 0) {
            Transportable t = transportables.get(0);
            TransportMission bestTransport = null;
            int bestTransportSpace = 0;
            int bestTransportTurns = Integer.MAX_VALUE;
            for (int i = 0; i < vacantTransports.size(); i++) {
                TransportMission tm = (TransportMission) vacantTransports.get(i);
                if (t.getTransportSource().getTile() == tm.getUnit().getLocation().getTile()) {
                    int transportSpace = tm.getAvailableSpace(t);
                    if (transportSpace > 0) {
                        bestTransport = tm;
                        bestTransportSpace = transportSpace;
                        bestTransportTurns = 0;
                        break;
                    } else {
                        continue;
                    }
                }
                PathNode path = tm.getPath(t);
                if (path != null && path.getTotalTurns() <= bestTransportTurns) {
                    int transportSpace = tm.getAvailableSpace(t);
                    if (transportSpace > 0
                        && (path.getTotalTurns() < bestTransportTurns || transportSpace > bestTransportSpace)) {
                        bestTransport = tm;
                        bestTransportSpace = transportSpace;
                        bestTransportTurns = path.getTotalTurns();
                    }
                }
            }
            if (bestTransport == null) {
                // No more transports available:
                break;
            }
            bestTransport.addToTransportList(t);
            transportables.remove(t);
            vacantTransports.remove(bestTransport);
            bestTransportSpace--;
            for (int i = 0; i < transportables.size() && bestTransportSpace > 0; i++) {
                Transportable t2 = transportables.get(0);
                if (t2.getTransportLocatable().getLocation() == t.getTransportLocatable().getLocation()) {
                    bestTransport.addToTransportList(t2);
                    transportables.remove(t2);
                    bestTransportSpace--;
                }
            }
        }
    }

    /**
     * Called when another <code>Player</code> proposes a sale.
     *
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
    }

    /**
     * Decides whether to accept an Indian demand, or not.
     *
     * @param unit The unit making demands.
     * @param colony The colony where demands are being made.
     * @param goods The goods demanded.
     * @param gold The amount of gold demanded.
     * @return <code>true</code> if this <code>AIPlayer</code> accepts the
     *         indian demand and <code>false</code> otherwise.
     */
    public abstract boolean acceptIndianDemand(Unit unit, Colony colony, Goods goods, int gold);

    /**
     * Returns an iterator over all the <code>AIColony</code>s owned by this
     * getPlayer().
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<AIColony> getAIColonyIterator() {
        ArrayList<AIColony> ac = new ArrayList<AIColony>();
        for (Colony colony : getPlayer().getColonies()) {
            AIColony a = (AIColony) getAIMain().getAIObject(colony.getId());
            if (a != null) {
                ac.add(a);
            } else {
                logger.warning("Could not find the AIColony for: " + colony);
            }
        }
        return ac.iterator();
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




}
