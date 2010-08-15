/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.server.ai.mission.IndianBringGiftMission;
import net.sf.freecol.server.ai.mission.IndianDemandMission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;


/**
 *
 * Objects of this class contains AI-information for a single {@link Player} and
 * is used for controlling this getPlayer().
 *
 * @deprecated Currently unused, outdated copy of {@link AIPlayer}. Eventual
 * specialization of AI should extend AIPlayer, see {@link ColonialAIPlayer}.
 *
 * The method {@link #startWorking} gets called by the
 * {@link AIInGameInputHandler} when it is this player's turn.
 */
public class IndianAIPlayer extends NewAIPlayer {

    private static final Logger logger = Logger.getLogger(IndianAIPlayer.class.getName());

    private static final int MAX_DISTANCE_TO_BRING_GIFT = 5;

    private static final int MAX_NUMBER_OF_GIFTS_BEING_DELIVERED = 1;

    private static final int MAX_DISTANCE_TO_MAKE_DEMANDS = 5;

    private static final int MAX_NUMBER_OF_DEMANDS = 1;


    /**
     *
     * Tells this <code>AIPlayer</code> to make decisions. The
     * <code>AIPlayer</code> is done doing work this turn when this method
     * returns.
     */
    public void startWorking() {
        logger.fine("Entering AI code for: " + getPlayer());
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
     * Gives a mission to non-naval units.
     */
    private void giveNormalMissions() {
        logger.finest("Entering method giveNormalMissions");

        /*
        int numberOfUnits = Specification.getSpecification().numberOfUnitTypes();
        // Create a datastructure for the worker wishes:
        ArrayList<ArrayList<Wish>> workerWishes = new ArrayList<ArrayList<Wish>>(numberOfUnits);
        for (int i = 0; i < numberOfUnits; i++) {
            workerWishes.add(new ArrayList<Wish>());
        }
        */

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

            if (!aiUnit.hasMission()) {
                aiUnit.setMission(new UnitWanderHostileMission(getAIMain(), aiUnit));
            }
        }
    }

    /**
     * Takes the necessary actions to secure the settlements. This is done by
     * making new military units or to give existing units new missions.
     */
    private void secureSettlements() {
        logger.finest("Entering method secureSettlements");
        // Determines if we need to move a brave out of the settlement.
        for (IndianSettlement is : getPlayer().getIndianSettlements()) {
            secureIndianSettlement(is);
        }
    }

    /**
     * Takes the necessary actions to secure an indian settlement
     */
    public void secureIndianSettlement(IndianSettlement is) {
        if (is.getOwner().isAtWar()) {
            if (is.getUnitCount() > 2) {
                int defenders = is.getTile().getUnitCount();
                int threat = 0;
                int worstThreat = 0;
                Location bestTarget = null;
                for (Tile t: is.getTile().getSurroundingTiles(2)) {
                    // ignore sea tiles
                    // Indians dont have sea power
                    if(!t.isLand()){
                        continue;
                    }
                    if (t.getFirstUnit() != null) {
                        Player enemy = t.getFirstUnit().getOwner();
                        if (enemy == getPlayer()) {
                            defenders++;
                        } else {
                            int value = getPlayer().getTension(enemy).getValue();
                            if (value >= Tension.TENSION_ADD_MAJOR) {
                                threat += 2;
                                if (t.getUnitCount() * 2 > worstThreat) {
                                    if (t.getSettlement() != null) {
                                        bestTarget = t.getSettlement();
                                    } else {
                                        bestTarget = t.getFirstUnit();
                                    }
                                    worstThreat = t.getUnitCount() * 2;
                                }
                            } else if (value >= Tension.TENSION_ADD_MINOR) {
                                threat += 1;
                                if (t.getUnitCount() > worstThreat) {
                                    if (t.getSettlement() != null) {
                                        bestTarget = t.getSettlement();
                                    } else {
                                        bestTarget = t.getFirstUnit();
                                    }
                                    worstThreat = t.getUnitCount();
                                }
                            }
                        }
                    }
                }
                if (threat > defenders) {
                    Unit newDefender = is.getFirstUnit();
                    newDefender.setState(UnitState.ACTIVE);
                    newDefender.setLocation(is.getTile());
                    AIUnit newDefenderAI = (AIUnit) getAIMain().getAIObject(newDefender);
                    if (bestTarget != null) {
                        newDefenderAI.setMission(new UnitSeekAndDestroyMission(getAIMain(), newDefenderAI,
                                                                               bestTarget));
                    } else {
                        newDefenderAI.setMission(new UnitWanderHostileMission(getAIMain(), newDefenderAI));
                    }
                }
            }
        }
    }

    /**
     * Brings gifts to nice players with nearby colonies.
     */
    private void bringGifts() {
        logger.finest("Entering method bringGifts");
        if (!getPlayer().isIndian()) {
            return;
        }
        for (IndianSettlement indianSettlement : getPlayer().getIndianSettlements()) {
            // Do not bring gifts all the time:
            if (getAIRandom().nextInt(10) != 1) {
                continue;
            }
            int alreadyAssignedUnits = 0;
            Iterator<Unit> ownedUnits = indianSettlement.getOwnedUnitsIterator();
            while (ownedUnits.hasNext()) {
                if (((AIUnit) getAIMain().getAIObject(ownedUnits.next())).getMission() instanceof IndianBringGiftMission) {
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
                Colony target = nearbyColonies.get(getAIRandom().nextInt(nearbyColonies.size()));
                Iterator<Unit> it2 = indianSettlement.getOwnedUnitsIterator();
                AIUnit chosenOne = null;
                while (it2.hasNext()) {
                    chosenOne = (AIUnit) getAIMain().getAIObject(it2.next());
                    if (!(chosenOne.getUnit().getLocation() instanceof Tile)) {
                        chosenOne = null;
                    } else if (chosenOne.getMission() == null
                               || chosenOne.getMission() instanceof UnitWanderHostileMission) {
                        break;
                    }
                }
                if (chosenOne != null) {
                    // Check that the colony can be reached:
                    PathNode pn = chosenOne.getUnit().findPath(indianSettlement.getTile(), target.getTile());
                    if (pn != null && pn.getTotalTurns() <= MAX_DISTANCE_TO_BRING_GIFT) {
                        chosenOne.setMission(new IndianBringGiftMission(getAIMain(), chosenOne, target));
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
        if (!getPlayer().isIndian()) {
            return;
        }
        for (IndianSettlement indianSettlement : getPlayer().getIndianSettlements()) {
            // Do not demand goods all the time:
            if (getAIRandom().nextInt(10) != 1) {
                continue;
            }
            int alreadyAssignedUnits = 0;
            Iterator<Unit> ownedUnits = indianSettlement.getOwnedUnitsIterator();
            while (ownedUnits.hasNext()) {
                if (((AIUnit) getAIMain().getAIObject(ownedUnits.next())).getMission() instanceof IndianDemandMission) {
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
                    int tension = 1 + getPlayer().getTension(to).getValue() + indianSettlement.getAlarm(to).getValue();
                    tension = getAIRandom().nextInt(tension);
                    if (tension > targetTension) {
                        targetTension = tension;
                        target = t;
                    }
                }
                Iterator<Unit> it2 = indianSettlement.getOwnedUnitsIterator();
                AIUnit chosenOne = null;
                while (it2.hasNext()) {
                    chosenOne = (AIUnit) getAIMain().getAIObject(it2.next());
                    if (!(chosenOne.getUnit().getLocation() instanceof Tile)) {
                        chosenOne = null;
                    } else if (chosenOne.getMission() == null
                               || chosenOne.getMission() instanceof UnitWanderHostileMission) {
                        break;
                    }
                }
                if (chosenOne != null && target != null) {
                    // Check that the colony can be reached:
                    PathNode pn = chosenOne.getUnit().findPath(indianSettlement.getTile(), target.getTile());
                    if (pn != null && pn.getTotalTurns() <= MAX_DISTANCE_TO_MAKE_DEMANDS) {
                        // Make it less probable that nice players get targeted
                        // for a demand mission:
                        Player tp = target.getOwner();
                        int tension = 1 + getPlayer().getTension(tp).getValue()
                            + indianSettlement.getAlarm(tp).getValue();
                        if (getAIRandom().nextInt(tension) > Tension.Level.HAPPY.getLimit()) {
                            chosenOne.setMission(new IndianDemandMission(getAIMain(), chosenOne, target));
                        }
                    }
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
        String goldKey = "tradeGold#" + goods.getType().getIndex() + "#" + goods.getAmount() + "#" + unit.getId();
        String hagglingKey = "tradeHaggling#" + unit.getId();
        int price;
        if (sessionRegister.containsKey(goldKey)) {
            price = sessionRegister.get(goldKey).intValue();
            if (price <= 0) {
                return price;
            }
        } else {
            price = ((IndianSettlement) settlement).getPrice(goods) - getPlayer().getTension(unit.getOwner()).getValue();
            price = Math.min(price, getPlayer().getGold() / 2);
            if (price <= 0) {
                return 0;
            }
            sessionRegister.put(goldKey, new Integer(price));
        }
        if (gold < 0 || price == gold) {
            return price;
        } else if (gold > (getPlayer().getGold() * 3) / 4) {
            sessionRegister.put(goldKey, new Integer(-1));
            return NetworkConstants.NO_TRADE;
        } else if (gold > (price * 11) / 10) {
            logger.warning("Cheating attempt: haggling with a request too high");
            sessionRegister.put(goldKey, new Integer(-1));
            return NetworkConstants.NO_TRADE;
        } else {
            int haggling = 1;
            if (sessionRegister.containsKey(hagglingKey)) {
                haggling = sessionRegister.get(hagglingKey).intValue();
            }
            if (getAIRandom().nextInt(3 + haggling) <= 3) {
                sessionRegister.put(goldKey, new Integer(gold));
                sessionRegister.put(hagglingKey, new Integer(haggling + 1));
                return gold;
            } else {
                sessionRegister.put(goldKey, new Integer(-1));
                return NetworkConstants.NO_TRADE;
            }
        }
    }


}