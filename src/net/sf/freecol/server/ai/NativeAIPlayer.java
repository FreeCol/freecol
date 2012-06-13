/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTradeItem;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.GoldTradeItem;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsTradeItem;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StanceTradeItem;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitTradeItem;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.IdleAtSettlementMission;
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

    /**
     * The modifier to apply when a ship is trading.
     */
    private static final String SHIP_TRADE_PENALTY
        = "model.modifier.shipTradePenalty";

    /**
     * The modifier to apply when to trade with a settlement with a
     * missionary if the enhancedMissionaries option is enabled.
     */
    private static final String MISSIONARY_TRADE_BONUS
        = "model.modifier.missionaryTradeBonus";

    public static final int MAX_DISTANCE_TO_BRING_GIFTS = 5;

    public static final int MAX_NUMBER_OF_GIFTS_BEING_DELIVERED = 1;

    public static final int MAX_DISTANCE_TO_MAKE_DEMANDS = 5;

    public static final int MAX_NUMBER_OF_DEMANDS = 1;

    /**
     * Stores temporary information for sessions (trading with another
     * player etc).
     */
    private HashMap<String, Integer> sessionRegister
        = new HashMap<String, Integer>();


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
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public NativeAIPlayer(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain, in);

        uninitialized = getPlayer() == null;
    }


    /**
     * Tells this <code>AIPlayer</code> to make decisions.
     * The <code>AIPlayer</code> is done doing work this turn when
     * this method returns.
     */
    public void startWorking() {
        Turn turn = getGame().getTurn();
        logger.finest(getClass().getName() + " in " + turn
            + ": " + getPlayer().getNationID());
        sessionRegister.clear();
        clearAIUnits();
        determineStances();
        if (turn.isFirstTurn()) {
            initializeMissions();
        } else {
            abortInvalidAndOneTimeMissions();
            secureSettlements();
            bringGifts();
            demandTribute();
            giveNormalMissions();
            doMissions();
            abortInvalidMissions();
            giveNormalMissions();
        }
        doMissions();
        abortInvalidMissions();
        clearAIUnits();
    }

    /**
     * Simple initialization of AI missions given that we know the starting
     * conditions.
     */
    private void initializeMissions() {
        AIMain aiMain = getAIMain();
        Player player = getPlayer();
        
        // Give defensive missions up to the minimum expected defence,
        // leave the rest with the default wander-hostile mission.
        List<Unit> units = new ArrayList<Unit>();
        for (IndianSettlement is : player.getIndianSettlements()) {
            int defence = is.getType().getMinimumSize() - 1;
            units.clear();
            units.addAll(is.getTile().getUnitList());
            units.addAll(is.getUnitList());
            while (units.size() > defence) units.remove(0);
            for (Unit u : units) {
                AIUnit aiu = getAIUnit(u);
                aiu.setMission(new DefendSettlementMission(aiMain, aiu, is));
            }
        }
    }
    
    /**
     * Takes the necessary actions to secure the settlements.
     * This is done by making new military units or to give existing
     * units new missions.
     */
    private void secureSettlements() {
        List<IndianSettlement> settlements
            = getPlayer().getIndianSettlements();
        for (IndianSettlement is : settlements) {
            // Spread arms and horses between camps
            // TODO: maybe make this dependent on difficulty level?
            int n = Utils.randomInt(logger, "Secure",
                                    getAIRandom(), settlements.size());
            IndianSettlement settlement = settlements.get(n);
            if (settlement != is) {
                is.tradeGoodsWithSettlement(settlement);
            }
        }
        for (IndianSettlement is : settlements) {
            equipBraves(is);
            secureIndianSettlement(is);
        }
    }

    /**
     * Greedily equips braves with horses and muskets.
     * Public for the test suite.
     *
     * @param is The <code>IndianSettlement</code> where the equipping occurs.
     */
    public void equipBraves(IndianSettlement is) {
        final Specification spec = getSpecification();
        List<Unit> units = is.getUnitList();
        units.addAll(is.getTile().getUnitList());
        roles: for (Role r : new Role[] { Role.DRAGOON, Role.SOLDIER,
                                          Role.SCOUT }) {
            List<EquipmentType> e = r.getRoleEquipment(spec);
            while (!units.isEmpty()) {
                Unit u = units.get(0);
                for (EquipmentType et : e) {
                    if (u.canBeEquippedWith(et)
                        && !is.canProvideEquipment(et)) {
                        continue roles;
                    }
                }
                if (u.getRole() != Role.DRAGOON && u.getRole() != r) {
                    getAIUnit(u).equipForRole(r, false);
                }
                units.remove(0);
            }
        }
    }

    /**
     * Takes the necessary actions to secure an indian settlement
     * Public for the test suite.
     *
     * @param is The <code>IndianSettlement</code> to secure.
     */
    public void secureIndianSettlement(final IndianSettlement is) {
        final AIMain aiMain = getAIMain();
        final Player player = getPlayer();
        final CombatModel cm = getGame().getCombatModel();
        final int minimumDefence = is.getType().getMinimumSize() - 1;

        // Collect native units and defenders
        List<Unit> units = new ArrayList<Unit>();
        List<Unit> defenders = new ArrayList<Unit>();
        units.addAll(is.getUnitList());
        units.addAll(is.getTile().getUnitList());
        for (Unit u : is.getOwnedUnits()) {
            if (!units.contains(u)) units.add(u);
        }

        // Collect the current defenders
        String logMe = "Defending settlement " + is.getName() + " with:";
        for (Unit u : new ArrayList<Unit>(units)) {
            AIUnit aiu = aiMain.getAIUnit(u);
            if (aiu == null) {
                units.remove(u);
            } else if (aiu.getMission() instanceof DefendSettlementMission
                && (((DefendSettlementMission)aiu.getMission())
                    .getTarget() == is)) {
                logMe += " " + u.getId();
                defenders.add(u);
                units.remove(u);
            } else if (Mission.invalidNewMissionReason(aiu) != null) {
                units.remove(u);
            }
        }

        // Collect threats and other potential defenders
        final HashMap<Tile, Float> threats = new HashMap<Tile, Float>();
        Player enemy;
        Tension tension;
        for (Tile t : is.getTile().getSurroundingTiles(2)) {
            if (!t.isLand() || t.getUnitCount() == 0) {
                ; // Do nothing
            } else if ((enemy = t.getFirstUnit().getOwner()) == player) {
                // Its one of ours!
                for (Unit u : t.getUnitList()) {
                    AIUnit aiu;
                    if (defenders.contains(u) || units.contains(u)
                        || (aiu = aiMain.getAIUnit(u)) == null) {
                        ; // Do nothing
                    } else if (aiu.getMission() instanceof DefendSettlementMission
                        && ((DefendSettlementMission)aiu.getMission())
                        .getTarget() == is) {
                        logMe += " " + u.getId();
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
                float threshold, bonus, value = 0.0f;
                if (tension.getLevel().compareTo(Tension.Level.DISPLEASED) <= 0) {
                    threshold = 1.0f;
                    bonus = 0.0f;
                } else {
                    threshold = 0.0f;
                    bonus = (float)tension.getLevel().ordinal()
                        - Tension.Level.CONTENT.ordinal();
                }
                for (Unit u : t.getUnitList()) {
                    float offence = cm.getOffencePower(u, is);
                    if (offence > threshold) value += offence + bonus;
                }
                if (value > 0.0f) threats.put(t, new Float(value));
            }
        }
        
        // Sort the available units by proximity to the settlement.
        // Simulates favouring the first warriors found by outgoing messengers.
        // Also favour units native to the settlement.
        final int homeBonus = 3;
        Collections.sort(units, new Comparator<Unit>() {
                public int compare(Unit u1, Unit u2) {
                    int s1 = u1.getTile().getDistanceTo(is.getTile());
                    int s2 = u2.getTile().getDistanceTo(is.getTile());
                    if (u1.getIndianSettlement() == is) s1 -= homeBonus;
                    if (u2.getIndianSettlement() == is) s2 -= homeBonus;
                    return s1 - s2;
                }
            });

        // Do we need more defenders?  If so, call some in.
        if (defenders.size() < minimumDefence + threats.size()) {
            int needed = minimumDefence + threats.size();
            logger.finest(is.getName() + " has " + defenders.size()
                + " initial defenders but needs " + minimumDefence + "(base)"
                + " + " + threats.size() + "(threats)" + " = " + needed);
            while (!units.isEmpty()) {
                Unit u = units.remove(0);
                AIUnit aiu = aiMain.getAIUnit(u);
                aiu.setMission(new DefendSettlementMission(aiMain, aiu, is));
                logMe += " [" + u.getId() + "]";
                defenders.add(u);
                if (defenders.size() >= needed) break;
            }
        }
        logger.finest(logMe);
        if (units.isEmpty()) return;

        // Sort threat tiles by threat value.
        List<Tile> threatTiles = new ArrayList<Tile>(threats.keySet());
        Collections.sort(threatTiles, new Comparator<Tile>() {
                public int compare(Tile t1, Tile t2) {
                    return Float.compare(threats.get(t2).floatValue(),
                        threats.get(t1).floatValue());
                }
            });

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
            logger.finest(is.getName() + " sends unit to attack " + target
                + " at " + tile + ": " + aiUnit);
            aiUnit.setMission(new UnitSeekAndDestroyMission(aiMain, aiUnit,
                                                            target));
        }
    }

    /**
     * Gives a mission to all units.
     */
    private void giveNormalMissions() {
        final AIMain aiMain = getAIMain();
        final Specification spec = aiMain.getGame().getSpecification();
        final List<EquipmentType> scoutEq
            = Unit.Role.SCOUT.getRoleEquipment(spec);
        final List<EquipmentType> soldierEq
            = Unit.Role.SOLDIER.getRoleEquipment(spec);
        for (AIUnit aiUnit : getAIUnits()) {
            final Unit unit = aiUnit.getUnit();

            String reason = null;
            if (unit.isUninitialized()) {
                reason = "(unit uninitialized)";
            } else if (unit.isDisposed()) {
                reason = "(unit disposed)";
            }
            if (reason != null) {
                logger.finest("Mission-Ignored " + reason + ": " + unit);
                continue;
            }

            // Check the current mission, and assign a new one to m if needed.
            Settlement settlement = unit.getSettlement();
            IndianSettlement is;
            Mission m = aiUnit.getMission();
            if (m != null && m.isValid() && !m.isOneTime()) {
                m = null;
            } else if (settlement != null && (settlement.getUnitCount()
                    + unit.getTile().getUnitCount() <= 1)) {
                m = new DefendSettlementMission(aiMain, aiUnit, settlement);
                
            } else if ((is = unit.getIndianSettlement()) != null
                && ((!unit.isMounted() && is.canProvideEquipment(scoutEq))
                    || (!unit.isArmed() && is.canProvideEquipment(soldierEq)))) {
                // Go home for new equipment if the home settlement has them.
                m = new DefendSettlementMission(aiMain, aiUnit, is);

            } else if (UnitWanderHostileMission.invalidReason(aiUnit) == null) {
                if (m instanceof UnitWanderHostileMission) {
                    m = null;
                } else {
                    m = new UnitWanderHostileMission(aiMain, aiUnit);
                }
            } else {
                if (m instanceof IdleAtSettlementMission) {
                    m = null;
                } else {
                    m = new IdleAtSettlementMission(aiMain, aiUnit);
                }
            }
            if (m != null) {
                if (!m.isOneTime()) {
                    logger.fine("Mission-New " + m + ": " + unit);
                }
                aiUnit.setMission(m);
            } else {
                logger.finest("Mission-Continues " + aiUnit.getMission()
                    + ": " + unit);
            }
        }
    }

    /**
     * Brings gifts to nice players with nearby colonies.
     */
    private void bringGifts() {
        final Player player = getPlayer();
        final Map map = getGame().getMap();
        for (IndianSettlement is : player.getIndianSettlements()) {
            // Do not bring gifts all the time.
            if (Utils.randomInt(logger, is.getName() + " bring gifts",
                    getAIRandom(), 10) != 0) continue;

            // Check if there are available units, and if there are already
            // enough missions in operation.
            List<Unit> availableUnits = new ArrayList<Unit>();
            int alreadyAssignedUnits = 0;
            for (Unit ou : is.getOwnedUnits()) {
                AIUnit aiu = getAIUnit(ou);
                if (aiu == null) {
                    continue;
                } else if (aiu.getMission() instanceof IndianBringGiftMission) {
                    alreadyAssignedUnits++;
                } else if (Mission.invalidNewMissionReason(aiu) == null) {
                    availableUnits.add(ou);
                }
            }
            if (alreadyAssignedUnits > MAX_NUMBER_OF_GIFTS_BEING_DELIVERED) {
                logger.finest(is.getName() + " has " + alreadyAssignedUnits
                    + " already.");
                continue;
            } else if (availableUnits.isEmpty()) {
                logger.finest(is.getName() + " has no gift units.");
                continue;
            }
            // Pick a random available capable unit.
            Unit unit = null;
            AIUnit aiUnit = null;
            while (unit == null && !availableUnits.isEmpty()) {
                Unit u = availableUnits.get(Utils.randomInt(logger,
                        "Choose gift unit", getAIRandom(),
                        availableUnits.size()));
                availableUnits.remove(u);
                aiUnit = getAIUnit(u);
                if (IndianBringGiftMission.invalidReason(aiUnit) == null
                    && map.findFullPath(u, u.getTile(), is.getTile(),
                        null, CostDeciders.numberOfLegalTiles()) != null) {
                    unit = u;
                }
            }
            if (unit == null) {
                logger.finest(is.getName() + " has no suitable gift units.");
                continue;
            }

            // Collect nearby colonies.  Filter out ones which are unreachable
            // or with which the settlement is on bad terms.
            List<Colony> nearbyColonies = new ArrayList<Colony>();
            for (Tile t : is.getTile()
                     .getSurroundingTiles(MAX_DISTANCE_TO_BRING_GIFTS)) {
                Colony c = t.getColony();
                if (c != null
                    && is.getAlarm(c.getOwner()) != null
                    && IndianBringGiftMission.invalidReason(aiUnit, c) == null
                    && map.findFullPath(unit, is.getTile(), c.getTile(),
                        null, CostDeciders.numberOfLegalTiles()) != null) {
                    nearbyColonies.add(c);
                }
            }
            // If there are any suitable colonies, pick a random one
            // to send a gift to.
            if (nearbyColonies.isEmpty()) {
                logger.finest(is.getName() + " has no nearby gift colonies.");
                continue;
            }
            Colony target = nearbyColonies.get(Utils.randomInt(logger,
                    "Choose gift colony", getAIRandom(),
                    nearbyColonies.size()));

            // Send the unit.
            logger.finest("Assigning gift from " + is.getName()
                + " to " + target.getName() + ": " + unit);
            aiUnit.setMission(new IndianBringGiftMission(getAIMain(),
                              aiUnit, target));
        }
    }

    /**
     * Demands tribute from nasty players with nearby colonies.
     */
    private void demandTribute() {
        final Map map = getGame().getMap();
        final Player player = getPlayer();
        for (IndianSettlement is : player.getIndianSettlements()) {
            // Do not demand tribute all of the time.
            if (Utils.randomInt(logger, is.getName() + " demand tribute",
                    getAIRandom(), 10) != 0) continue;

            // Check if there are available units, and if there are already
            // enough missions in operation.
            List<Unit> availableUnits = new ArrayList<Unit>();
            int alreadyAssignedUnits = 0;
            for (Unit ou : is.getOwnedUnits()) {
                AIUnit aiu = getAIUnit(ou);
                if (Mission.invalidNewMissionReason(aiu) == null) {
                    if (aiu.getMission() instanceof IndianDemandMission) {
                        alreadyAssignedUnits++;
                    } else {
                        availableUnits.add(ou);
                    }
                }
            }
            if (alreadyAssignedUnits > MAX_NUMBER_OF_DEMANDS) {
                logger.finest(is.getName() + " has " + alreadyAssignedUnits
                    + " already.");
                continue;
            } else if (availableUnits.isEmpty()) {
                logger.finest(is.getName() + " has no demand units.");
                continue;
            }
            // Pick a random available capable unit.
            Unit unit = null;
            AIUnit aiUnit = null;
            while (unit == null && !availableUnits.isEmpty()) {
                Unit u = availableUnits.get(Utils.randomInt(logger,
                        "Choose demand unit", getAIRandom(),
                        availableUnits.size()));
                availableUnits.remove(u);
                aiUnit = getAIUnit(u);
                if (IndianDemandMission.invalidReason(aiUnit) == null
                    && map.findFullPath(u, u.getTile(), is.getTile(),
                        null, CostDeciders.numberOfLegalTiles()) != null) {
                    unit = u;
                }
            }
            if (unit == null) {
                logger.finest(is.getName() + " has no suitable demand units.");
                continue;
            }

            // Collect nearby colonies.  Filter out ones which are unreachable
            // or with which the settlement is on adequate terms.
            List<Colony> nearbyColonies = new ArrayList<Colony>();
            for (Tile t : is.getTile()
                     .getSurroundingTiles(MAX_DISTANCE_TO_MAKE_DEMANDS)) {
                Colony c = t.getColony();
                if (c != null
                    && is.getAlarm(c.getOwner()) != null
                    && IndianDemandMission.invalidReason(aiUnit, c) == null
                    && map.findFullPath(unit, is.getTile(), c.getTile(),
                        null, CostDeciders.numberOfLegalTiles()) != null) {
                    nearbyColonies.add(c);
                }
            }
            // If there are any suitable colonies, pick one to demand from.
            // Sometimes a random one, sometimes the weakest, sometimes the
            // most annoying.
            if (nearbyColonies.isEmpty()) {
                logger.finest(is.getName() + " has no nearby demand colonies.");
                continue;
            }
            int rnd = Utils.randomInt(logger, "Choose demand colony",
                                      getAIRandom(), 3 * nearbyColonies.size());
            Colony target = null;
            if (rnd < nearbyColonies.size()) {
                target = nearbyColonies.get(rnd);
            } else if (rnd < 2 * nearbyColonies.size()) {
                int bestValue = Integer.MAX_VALUE;
                for (Colony c : nearbyColonies) {
                    int value = ((c.getStockade() == null) ? 0
                        : (c.getStockade().getLevel() * 10))
                        + c.getUnitCount();
                    if (value < bestValue) {
                        value = bestValue;
                        target = c;
                    }
                }
            } else {
                int bestValue = -1;
                for (Colony c : nearbyColonies) {
                    if (is.getAlarm(c.getOwner()).getValue() > bestValue) {
                        bestValue = is.getAlarm(c.getOwner()).getValue();
                        target = c;
                    }
                }
            }
            if (target == null) {
                throw new IllegalStateException("No target!?!");
            }

            // Send the unit.
            logger.finest("Assigning demand from " + is.getName()
                + " to " + target.getName() + ": " + unit);
            aiUnit.setMission(new IndianDemandMission(getAIMain(),
                              aiUnit, target));
        }
    }


    /**
     * Evaluates a proposed mission type for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to perform the mission.
     * @param path A <code>PathNode</code> to the target of this mission.
     * @param type The mission type.
     * @return A score representing the desirability of this mission.
     */
    public int scoreMission(AIUnit aiUnit, PathNode path, Class type) {
        int value = super.scoreMission(aiUnit, path, type);
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
            IndianSettlement is = aiUnit.getUnit().getIndianSettlement();
            if (targetPlayer != null
                && is != null && is.getAlarm(targetPlayer) != null) {
                value += is.getAlarm(targetPlayer).getValue()
                    - Tension.Level.DISPLEASED.getLimit();
            }
        }

        return value;
    }


    /**
     * Resolves a diplomatic trade offer.
     *
     * @param agreement The proposed <code>DiplomaticTrade</code>.
     * @return True if the agreement is accepted.
     */
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
                        // Invalid, never accept.
                        validOffer = false;
                        break;
                    case WAR: // Always accept war without cost.
                        break;
                    case CEASE_FIRE:
                        value -= 500;
                        break;
                    case PEACE:
                        if (!agreement.getSender()
                            .hasAbility("model.ability.alwaysOfferedPeace")) {
                            // TODO: introduce some kind of counter in
                            // order to avoid Benjamin Franklin exploit.
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
                    value -= getPlayer().getMarket()
                        .getBidPrice(goods.getType(), goods.getAmount());
                } else {
                    value += getPlayer().getMarket()
                        .getSalePrice(goods.getType(), goods.getAmount());
                }
            }
        }
        if (validOffer) {
            logger.info("Trade value is " + value + ", accept if >=0");
        } else {
            logger.info("Trade offer is considered invalid!");
        }
        return (value >= 0) && validOffer;
    }

    /**
     * Called after another <code>Player</code> sends a
     * <code>trade</code> message.
     *
     * @param goods The goods which we are going to offer
     */
    public void registerSellGoods(Goods goods) {
        String goldKey = "tradeGold#" + goods.getType().getId()
            + "#" + goods.getAmount() + "#" + goods.getLocation().getId();
        sessionRegister.put(goldKey, null);
    }

    /**
     * Gets the appropriate missionary trade bonuses.
     *
     * @param missionary The missionary <code>Unit</code>.
     * @param sense The sense to apply the modifiers.
     * @return The missionary trade bonuses.
     */
    private Set<Modifier> getMissionaryTradeBonuses(Unit missionary,
                                                    boolean sense) {
        Set<Modifier> missionaryBonuses = missionary
            .getModifierSet(MISSIONARY_TRADE_BONUS);
        Set<Modifier> result;
        if (sense) {
            result = missionaryBonuses;
        } else {
            result = new HashSet<Modifier>();
            for (Modifier m : missionaryBonuses) {
                result.add(new Modifier(m.getId(), m.getSource(),
                        -m.getValue(), m.getType()));
            }
        }
        return result;
    }

    /**
     * Gets the appropriate ship trade penalties.
     *
     * @param sense The sense to apply the modifiers.
     * @return The ship trade penalties.
     */
    private Set<Modifier> getShipTradePenalties(boolean sense) {
        Specification spec = getSpecification();
        List<Modifier> shipPenalties = spec.getModifiers(SHIP_TRADE_PENALTY);
        Set<Modifier> result = new HashSet<Modifier>();
        int penalty = spec.getInteger(GameOptions.SHIP_TRADE_PENALTY);
        for (Modifier m : shipPenalties) {
            result.add(new Modifier(m.getId(), m.getSource(),
                    ((sense) ? penalty : -penalty), m.getType()));
        }
        return result;
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
            Set<Modifier> modifiers = new HashSet<Modifier>();
            Unit missionary = is.getMissionary(buyer);
            if (missionary != null
                && spec.getBoolean(GameOptions.ENHANCED_MISSIONARIES)) {
                modifiers.addAll(getMissionaryTradeBonuses(missionary, false));
            }
            if (unit.isNaval()) {
                modifiers.addAll(getShipTradePenalties(false));
            }
            price = (int) FeatureContainer.applyModifierSet((float)price,
                getGame().getTurn(), modifiers);
            sessionRegister.put(goldKey, new Integer(price));
            return price;
        }
        price = registered.intValue();
        if (price < 0 || price == gold) return price;
        if (gold < (price * 9) / 10) {
            logger.warning("Cheating attempt: sending offer too low");
            sessionRegister.put(goldKey, new Integer(-1));
            return NetworkConstants.NO_TRADE;
        }

        int haggling = 1;
        if (sessionRegister.containsKey(hagglingKey)) {
            haggling = sessionRegister.get(hagglingKey).intValue();
        }
        if (Utils.randomInt(logger, "Haggle-buy",
                getAIRandom(), 3 + haggling) >= 3) {
            sessionRegister.put(goldKey, new Integer(-1));
            return NetworkConstants.NO_TRADE_HAGGLE;
        }
        sessionRegister.put(goldKey, new Integer(gold));
        sessionRegister.put(hagglingKey, new Integer(haggling + 1));
        return gold;
    }

    /**
     * Called when another <code>Player</code> proposes a sale.
     *
     * @param unit The foreign <code>Unit</code> trying to trade.
     * @param settlement The <code>Settlement</code> this player owns and
     *     which the given <code>Unit</code> if trying to sell goods.
     * @param goods The goods the given <code>Unit</code> is trying to sell.
     * @param gold The suggested price.
     * @return The price this <code>AIPlayer</code> suggests or
     *     {@link NetworkConstants#NO_TRADE}.
     */
    public int sellProposition(Unit unit, Settlement settlement,
                               Goods goods, int gold) {
        logger.finest("Entering method sellProposition");
        Specification spec = getSpecification();
        IndianSettlement is = (IndianSettlement) settlement;
        Player seller = unit.getOwner();
        String goldKey = "tradeGold#" + goods.getType().getId()
            + "#" + goods.getAmount() + "#" + settlement.getId();
        String hagglingKey = "tradeHaggling#" + unit.getId();
        int price;
        if (sessionRegister.containsKey(goldKey)) {
            price = sessionRegister.get(goldKey).intValue();
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
            Set<Modifier> modifiers = new HashSet<Modifier>();
            Unit missionary = is.getMissionary(seller);
            if (missionary != null
                && spec.getBoolean(GameOptions.ENHANCED_MISSIONARIES)) {
                modifiers.addAll(getMissionaryTradeBonuses(missionary, true));
            }
            if (unit.isNaval()) {
                modifiers.addAll(getShipTradePenalties(true));
            }
            price = (int) FeatureContainer.applyModifierSet((float)price,
                getGame().getTurn(), modifiers);
            if (price <= 0) return 0;
            sessionRegister.put(goldKey, new Integer(price));
        }
        if (gold < 0 || price == gold) return price;
        if (gold > (price * 11) / 10) {
            logger.warning("Cheating attempt: haggling request too high");
            sessionRegister.put(goldKey, new Integer(-1));
            return NetworkConstants.NO_TRADE;
        }
        int haggling = 1;
        if (sessionRegister.containsKey(hagglingKey)) {
            haggling = sessionRegister.get(hagglingKey).intValue();
        }
        if (Utils.randomInt(logger, "Haggle-sell",
                getAIRandom(), 3 + haggling) >= 3) {
            sessionRegister.put(goldKey, new Integer(-1));
            return NetworkConstants.NO_TRADE_HAGGLE;
        }
        sessionRegister.put(goldKey, new Integer(gold));
        sessionRegister.put(hagglingKey, new Integer(haggling + 1));
        return gold;
    }

    // Use default acceptTax, acceptMercenaries, determineStances,
    // selectFoundingFather.
}
