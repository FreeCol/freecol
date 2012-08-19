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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTradeItem;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.GoldTradeItem;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsTradeItem;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StanceTradeItem;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitTradeItem;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.util.Utils;
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
import net.sf.freecol.server.model.ServerPlayer;


/**
 * Objects of this class contains AI-information for a single {@link
 * Player} and is used for controlling this player.
 *
 * The method {@link #startWorking} gets called by the
 * {@link AIInGameInputHandler} when it is this player's turn.
 */
public class EuropeanAIPlayer extends AIPlayer {

    private static final Logger logger = Logger.getLogger(EuropeanAIPlayer.class.getName());

    /**
     * A comparator to sort units by suitability for a BuildColonyMission.
     *
     * Favours unequipped freeColonists, and other unskilled over experts.
     * Also favour units on the map.
     */
    private static final Comparator<AIUnit> builderComparator
        = new Comparator<AIUnit>() {
            private int score(AIUnit a) {
                Unit unit = a.getUnit();
                if (BuildColonyMission.invalidReason(a) != null) return -1000;
                int base = (!unit.getEquipment().isEmpty()) ? 0
                    : (unit.getSkillLevel() > 0) ? 100
                    : 500 + 100 * unit.getSkillLevel();
                if (unit.getTile() != null) base += 50;
                return base;
            }

            public int compare(AIUnit a1, AIUnit a2) {
                return score(a2) - score(a1);
            }
        };

    /**
     * A comparator to sort units by suitability for a ScoutingMission.
     *
     * Favours existing scouts (especially if on the map), then dismounted
     * experts, then units that can become scouts.
     *
     * We do not check if a unit is near to a colony that can provide horses,
     * as that is likely to be too expensive.  TODO: revise
     */
    private static final Comparator<AIUnit> scoutComparator
        = new Comparator<AIUnit>() {
            private int score(AIUnit a) {
                Unit unit = a.getUnit();
                if (!unit.isColonist()) {
                    return -1000;
                } else if (unit.hasAbility("model.ability.speakWithChief")) {
                    return 900 + ((unit.getTile() != null) ? 100 : 0);
                } else if (unit.hasAbility("model.ability.expertScout")) {
                    return 600;
                }
                int base = (unit.isInEurope()) ? 500
                    : (unit.getLocation().getColony() != null
                        && unit.getLocation().getColony()
                        .canProvideEquipment(Unit.Role.SCOUT.getRoleEquipment(unit.getSpecification()))) ? 400
                    : -1000;
                if (!unit.getEquipment().isEmpty()) {
                    base -= 400;
                } else if (unit.getSkillLevel() > 0) {
                    base -= 200;
                }
                // Do not penalize criminals or servants.
                return base;
            }

            public int compare(AIUnit a1, AIUnit a2) {
                return score(a2) - score(a1);
            }
        };

    /**
     * A comparator to sort units by suitability for a PioneeringMission.
     *
     * Favours existing pioneers (especially if on the map), then experts
     * missing tools, then units that can become pioneers.
     *
     * We do not check if a unit is near to a colony that can provide tools,
     * as that is likely to be too expensive.  TODO: revise
     */
    private static final Comparator<AIUnit> pioneerComparator
        = new Comparator<AIUnit>() {
            private int score(AIUnit a) {
                Unit unit = a.getUnit();
                if (!unit.isColonist()) {
                    return -1000;
                } else if (unit.hasAbility("model.ability.improveTerrain")) {
                    return 900 + ((unit.getTile() != null) ? 100 : 0);
                } else if (unit.hasAbility("model.ability.expertPioneer")) {
                    return 600;
                }
                int base = (unit.isInEurope()) ? 500
                    : (unit.getLocation().getColony() != null
                        && unit.getLocation().getColony()
                        .canProvideEquipment(Unit.Role.PIONEER.getRoleEquipment(unit.getSpecification()))) ? 400
                    : -1000;
                if (!unit.getEquipment().isEmpty()) {
                    base -= 400;
                } else if (unit.getSkillLevel() > 0) {
                    base -= 200;
                } else {
                    base += unit.getSkillLevel() * 150;
                }
                return base;
            }

            public int compare(AIUnit a1, AIUnit a2) {
                return score(a2) - score(a1);
            }
        };

    /** A cached map of Tile to best TileImprovementPlan.  Do not serialize. */
    private final java.util.Map<Tile, TileImprovementPlan> tipMap
        = new HashMap<Tile, TileImprovementPlan>();

    /**
     * Stores temporary information for sessions (trading with another player
     * etc).
     */
    private final java.util.Map<String, Integer> sessionRegister
        = new HashMap<String, Integer>();


    /**
     * Creates a new <code>EuropeanAIPlayer</code>.
     *
     * @param aiMain The main AI-class.
     * @param player The player that should be associated with this
     *            <code>AIPlayer</code>.
     */
    public EuropeanAIPlayer(AIMain aiMain, ServerPlayer player) {
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
    public EuropeanAIPlayer(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain, in);

        uninitialized = getPlayer() == null;
    }


    /**
     * Weeds out a broken or obsolete tile improvement plan.
     *
     * @param tip The <code>TileImprovementPlan</code> to test.
     * @return True if the plan survives this check.
     */
    public boolean validateTileImprovementPlan(TileImprovementPlan tip) {
        if (tip == null) return false;
        Tile target = tip.getTarget();
        if (target == null) {
            logger.warning("Removing targetless TileImprovementPlan");
            tip.dispose();
            return false;
        }
        if (target.hasImprovement(tip.getType())) {
            logger.finest("Removing obsolete TileImprovementPlan");
            tip.dispose();
            return false;
        }
        if (tip.getPioneer() != null
            && (tip.getPioneer().getUnit() == null
                || tip.getPioneer().getUnit().isDisposed())) {
            logger.warning("Clearing broken pioneer for TileImprovementPlan");
            tip.setPioneer(null);
        }
        return true;
    }

    /**
     * Builds a map of locations to TileImprovementPlans.
     * Called by startWorking at the start of every turn.
     * Public for the test suite.
     */
    public void buildTipMap() {
        tipMap.clear();
        for (AIColony aic : getAIColonies()) {
            for (TileImprovementPlan tip : aic.getTileImprovementPlans()) {
                if (!validateTileImprovementPlan(tip)) {
                    aic.removeTileImprovementPlan(tip);
                    continue;
                }
                if (tip.getPioneer() != null) continue;
                TileImprovementPlan other = tipMap.get(tip.getTarget());
                if (other == null || other.getValue() < tip.getValue()) {
                    tipMap.put(tip.getTarget(), tip);
                }
            }
        }
    }

    /**
     * Gets the best plan for a tile from the tipMap.
     *
     * @param tile The <code>Tile</code> to lookup.
     * @return The best plan for a tile.
     */
    public TileImprovementPlan getBestPlan(Tile tile) {
        return (tipMap == null) ? null : tipMap.get(tile);
    }

    /**
     * Gets the best plan for a colony from the tipMap.
     *
     * @param colony The <code>Colony</code> to check.
     * @return The tile with the best plan for a colony, or null if none found.
     */
    public Tile getBestPlanTile(Colony colony) {
        TileImprovementPlan best = null;
        int bestValue = Integer.MIN_VALUE;
        for (Tile t : colony.getOwnedTiles()) {
            TileImprovementPlan tip = tipMap.get(t);
            if (tip != null && tip.getValue() > bestValue) {
                bestValue = tip.getValue();
                best = tip;
            }
        }
        return (best == null) ? null : best.getTarget();
    }

    /**
     * Remove a <code>TileImprovementPlan</code> from the relevant colony.
     */
    public void removeTileImprovementPlan(TileImprovementPlan plan) {
        for (AIColony aic : getAIColonies()) {
            if (aic.removeTileImprovementPlan(plan)) break;
        }
    }

    /**
     * Gets the number of units that should build a colony.
     *
     * @return The desired number of colony builders for this player.
     */
    private int buildersNeeded() {
        Player player = getPlayer();
        if (!player.canBuildColonies()) return 0;

        int nColonies = 0, nPorts = 0, nWorkers = 0;
        for (Settlement settlement : player.getSettlements()) {
            nColonies++;
            if (settlement.isConnectedPort()) nPorts++;
            for (Unit u : settlement.getUnitList()) {
                if (u.isPerson()) nWorkers++;
            }
        }

        // If would be good to have at least two colonies, and at least
        // one port.  After that, determine the ratio of workers to colonies
        // (which should be the average colony size), and if that is above
        // a threshold, send out another colonist.
        // The threshold probably should be configurable.  2 is too
        // low IMHO as it makes a lot of brittle colonies, 3 is too
        // high at least initially as it slows expansion.  For now,
        // arbitrarily choose e.
        int result = (nColonies == 0 || nPorts == 0) ? 2
            : ((nPorts == 1) && nWorkers >= 3) ? 1
            : ((double)nWorkers / nColonies > Math.E) ? 1
            : 0;
        return result;
    }

    /**
     * How many pioneers should we have?
     *
     * @return The desired number of pioneers for this player.
     */
    public int pioneersNeeded() {
        return tipMap.size() / 2;
    }

    /**
     * How many scouts should we have?
     *
     * Current scheme for European AIs is to use up to three scouts in
     * the early part of the game, then one.
     *
     * @return The desired number of scouts for this player.
     */
    public int scoutsNeeded() {
        return (getGame().getTurn().getAge() <= 1) ? 3 : 1;
    }

    /**
     * Asks the server to recruit a unit in Europe on behalf of the AIPlayer.
     *
     * TODO: Move this to a specialized Handler class (AIEurope?)
     * TODO: Give protected access?
     *
     * @param index The index of the unit to recruit in the recruitables list,
     *     (if not a valid index, recruit a random unit).
     * @return The new AIUnit created by this action or null on failure.
     */
    public AIUnit recruitAIUnitInEurope(int index) {
        AIUnit aiUnit = null;
        Europe europe = getPlayer().getEurope();
        int n = europe.getUnitCount();
        final String selectAbility = "model.ability.selectRecruit";
        int slot = (index >= 0 && index < Europe.RECRUIT_COUNT
            && getPlayer().hasAbility(selectAbility)) ? (index + 1) : 0;
        if (AIMessage.askEmigrate(this, slot)
            && europe.getUnitCount() == n+1) {
            aiUnit = getAIUnit(europe.getUnitList().get(n));
        }
        return aiUnit;
    }

    /**
     * Helper function for server communication - Ask the server
     * to train a unit in Europe on behalf of the AIGetPlayer().
     *
     * TODO: Move this to a specialized Handler class (AIEurope?)
     * TODO: Give protected access?
     *
     * @return the new AIUnit created by this action. May be null.
     */
    public AIUnit trainAIUnitInEurope(UnitType unitType) {
        if (unitType==null) {
            throw new IllegalArgumentException("Invalid UnitType.");
        }

        AIUnit aiUnit = null;
        Europe europe = getPlayer().getEurope();
        int n = europe.getUnitCount();

        if (AIMessage.askTrainUnitInEurope(this, unitType)
            && europe.getUnitCount() == n+1) {
            aiUnit = getAIUnit(europe.getUnitList().get(n));
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
        List<Wish> wishes = new ArrayList<Wish>();
        for (AIColony aic : getAIColonies()) {
            wishes.addAll(aic.getWishes());
        }
        Collections.sort(wishes);
        return wishes;
    }

/* Internal methods ***********************************************************/


    /**
     * Cheats for the AI.  Please try to centralize cheats here.
     *
     * TODO: Remove when the AI is good enough.
     */
    private void cheat() {
        logger.finest("Entering method cheat");
        Specification spec = getSpecification();
        Market market = getPlayer().getMarket();
        for (GoodsType goodsType : spec.getGoodsTypeList()) {
            if (market.getArrears(goodsType) > 0
                && Utils.randomInt(logger, "Cheat boycott",
                    getAIRandom(), 5) == 0) {
                market.setArrears(goodsType, 0);
                // Just remove one goods party modifier (we can not
                // currently identify which modifier applies to which
                // goods type, but that is not worth fixing for the
                // benefit of `temporary' cheat code).  If we do not
                // do this, AI colonies accumulate heaps of party
                // modifiers because of the cheat boycott removal.
                findOne: for (Colony c : getPlayer().getColonies()) {
                    for (Modifier m : c.getModifiers()) {
                        if ("model.modifier.colonyGoodsParty".equals(m.getSource())) {
                            c.removeModifier(m);
                            break findOne;
                        }
                    }
                }
            }
        }

        // TODO: This seems to buy units the AIPlayer can't possibly
        // use (see BR#2566180)
        if (getAIMain().getFreeColServer().isSinglePlayer()
            && getPlayer().getPlayerType() == PlayerType.COLONIAL) {
            Europe europe = getPlayer().getEurope();
            List<UnitType> unitTypes = spec.getUnitTypeList();

            if (!europe.isEmpty()
                && scoutsNeeded() > 0
                && Utils.randomInt(logger, "Cheat equip scout", 
                                   getAIRandom(), 4) == 1) {
                for (Unit u : europe.getUnitList()) {
                    if (u.getRole() == Unit.Role.DEFAULT
                        && getAIUnit(u).equipForRole(Unit.Role.SCOUT, true)) {
                        break;
                    }
                }
            }

            if (Utils.randomInt(logger, "Cheat buy unit",
                    getAIRandom(), 10) == 1) {
                WorkerWish bestWish = null;
                int bestValue = Integer.MIN_VALUE;
                for (AIColony aic : getAIColonies()) {
                    for (WorkerWish ww : aic.getWorkerWishes()) {
                        if (ww.getValue() > bestValue) {
                            bestValue = ww.getValue();
                            bestWish = ww;
                        }
                    }
                }
                UnitType unitType;
                int unitPrice;
                if (bestWish != null
                    && (unitType = bestWish.getUnitType()) != null
                    && (unitPrice = europe.getUnitPrice(unitType)) >= 0) {
                    // cheat add the necessary amount of money
                    getPlayer().modifyGold(unitPrice);
                    AIUnit aiUnit = trainAIUnitInEurope(unitType);
                    if (aiUnit != null) {
                        Unit unit = aiUnit.getUnit();
                        if (unit != null && unit.isColonist()) {
                            aiUnit.equipForRole(Unit.Role.DRAGOON, true);
                        }
                        aiUnit.setMission(new WishRealizationMission(getAIMain(), aiUnit, bestWish));
                    }
                }
            }
            // TODO: better heuristics to determine which ship to buy
            if (Utils.randomInt(logger, "Cheat buy ship",
                    getAIRandom(), 40) == 21) {
                int total = 0;
                ArrayList<UnitType> navalUnits = new ArrayList<UnitType>();
                for (UnitType unitType : unitTypes) {
                    if (unitType.hasAbility(Ability.NAVAL_UNIT) && unitType.hasPrice()) {
                        navalUnits.add(unitType);
                        total += europe.getUnitPrice(unitType);
                    }
                }

                UnitType unitToPurchase = null;
                int r = Utils.randomInt(logger, "Cheat which ship",
                    getAIRandom(), total);
                total = 0;
                for (UnitType unitType : navalUnits) {
                    total += unitType.getPrice();
                    if (r < total) {
                        unitToPurchase = unitType;
                        break;
                    }
                }
                getPlayer().modifyGold(europe.getUnitPrice(unitToPurchase));
                this.trainAIUnitInEurope(unitToPurchase);
            }
        }
    }

    /**
     * Calls {@link AIColony#rearrangeWorkers} for every colony this player
     * owns.
     */
    private void rearrangeWorkersInColonies() {
        for (AIColony aic : getAIColonies()) aic.rearrangeWorkers();
    }

    /**
     * Ensures all units have a mission.
     */
    protected void giveNormalMissions() {
        final AIMain aiMain = getAIMain();
        final Player player = getPlayer();
        final int turnNumber = getGame().getTurn().getNumber();
        int nBuilders = buildersNeeded();
        int nPioneers = pioneersNeeded();
        int nScouts = scoutsNeeded();

        // Create a mapping of unit type to worker wishes.
        java.util.Map<UnitType, List<Wish>> workerWishes
            = new HashMap<UnitType, List<Wish>>();
        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            workerWishes.put(unitType, new ArrayList<Wish>());
        }
        for (Wish w : getWishes()) {
            if (w instanceof WorkerWish && w.getTransportable() == null) {
                workerWishes.get(((WorkerWish) w).getUnitType()).add(w);
            }
        }

        // For all units, check if it is a candidate for a new
        // mission.  If it is not a candidate remove it from the
        // aiUnits list (reporting why not).  Adjust the
        // Build/Pioneer/Scout counts according to the existing valid
        // missions.
        List<AIUnit> aiUnits = getAIUnits();
        List<AIUnit> navalUnits = new ArrayList<AIUnit>();
        String report = "";
        int allUnits = aiUnits.size(), i = 0;
        while (i < aiUnits.size()) {
            final AIUnit aiUnit = aiUnits.get(i);
            final Unit unit = aiUnit.getUnit();
            Mission m = aiUnit.getMission();
            String reason = null;

            if (unit.isUninitialized() || unit.isDisposed()) {
                reason = "Invalid-" + aiUnit.toString();

            } else if (unit.getState() == UnitState.IN_COLONY
                && unit.getColony().getUnitCount() <= 1) {
                // The unit has its hand full keeping the colony alive.
                if (!(aiUnit.getMission() instanceof WorkInsideColonyMission)){
                    logger.warning(aiUnit + " should WorkInsideColony at "
                        + unit.getColony().getName());
                    m = new WorkInsideColonyMission(aiMain, aiUnit,
                        aiMain.getAIColony(unit.getColony()));
                }
                reason = "Vital-to-" + unit.getSettlement().getName()
                    + "-" + m.toString();

            } else if (unit.isInMission()) {
                reason = "In-Mission-" + aiUnit.toString();

            } else if (m != null && m.isValid() && !m.isOneTime()) {
                if (m instanceof BuildColonyMission) {
                    nBuilders--;
                } else if (m instanceof PioneeringMission) {
                    nPioneers--;
                } else if (m instanceof ScoutingMission) {
                    nScouts--;
                }
                reason = "Valid-" + m.toString();

            } else if (unit.isNaval()) {
                aiUnits.remove(i);
                navalUnits.add(aiUnit);
                continue;

            } else if (unit.isAtSea()) { // Wait for it to emerge
                reason = "At-Sea-" + aiUnit.toString();
            }

            if (reason == null) {
                i++;
            } else {
                report += "\n  " + reason;
                aiUnits.remove(i);
            }
        }
        report = Utils.lastPart(getPlayer().getNationID(), ".")
            + ".giveNormalMissions(turn=" + turnNumber
            + " all-units=" + allUnits + " free-land-units=" + aiUnits.size()
            + " free-naval-units=" + navalUnits.size()
            + " builders=" + nBuilders + " pioneers=" + nPioneers
            + " nScouts=" + nScouts + ")"
            + report;

        // First try to satisfy the demand for missions with a defined quota.
        if (nBuilders > 0) {
            Collections.sort(aiUnits, builderComparator);
            while (!aiUnits.isEmpty()) {
                AIUnit aiUnit = aiUnits.get(0);
                Mission m = getBuildColonyMission(aiUnit);
                if (m == null) break; // Reached the unsuitable units
                aiUnits.remove(0);
                aiUnit.setMission(m);
                report += "\n  New-" + m.toString();
                if (--nBuilders <= 0) break;
            }
        }
        if (nScouts > 0) {
            Collections.sort(aiUnits, scoutComparator);
            while (!aiUnits.isEmpty()) {
                AIUnit aiUnit = aiUnits.get(0);
                Mission m = getScoutingMission(aiUnit);
                if (m == null) break; // Reached the unsuitable units
                aiUnits.remove(0);
                aiUnit.setMission(m);
                report += "\n  New-" + m.toString();
                if (--nScouts <= 0) break;
            }
        }
        if (nPioneers > 0) {
            Collections.sort(aiUnits, pioneerComparator);
            while (!aiUnits.isEmpty()) {
                AIUnit aiUnit = aiUnits.get(0);
                Mission m = getPioneeringMission(aiUnit);
                if (m == null) break; // Reached the unsuitable units
                aiUnits.remove(0);
                aiUnit.setMission(m);
                report += "\n  New-" + m.toString();
                if (--nPioneers <= 0) break;
            }
        }

        // Process the free naval units.
        i = 0;
        while (i < navalUnits.size()) {
            final AIUnit aiUnit = navalUnits.get(i);
            Mission m;
            if ((m = getPrivateerMission(aiUnit)) != null
                || (m = getTransportMission(aiUnit)) != null
                || (m = getSeekAndDestroyMission(aiUnit, 8)) != null
                || (m = getWanderHostileMission(aiUnit)) != null
                ) {
                aiUnit.setMission(m);
                report += "\n  New-" + m.toString();
                navalUnits.remove(i);
            } else {
                i++;
            }
        }
            
        // Sort the remaining units, putting naval/transport units at
        // the front so that we can rely on valid transport missions
        // for them when handling their passengers.  Then loop through
        // them making sure every unit gets an appropriate mission.
        i = 0;
        while (i < aiUnits.size()) {
            final AIUnit aiUnit = aiUnits.get(i);
            final Unit unit = aiUnit.getUnit();
            Mission m;

            // CashIn missions are obvious
            if ((m = getCashInTreasureTrainMission(aiUnit)) != null

                // Try to maintain defence
                || (unit.isDefensiveUnit()
                    && (m = getDefendSettlementMission(aiUnit, false)) != null)

                // Favour wish realization for expert units
                || (unit.isColonist() && unit.getSkillLevel() > 0
                    && (m = getWishRealizationMission(aiUnit,
                                                      workerWishes)) != null)

                // Try nearby offence
                || (unit.isOffensiveUnit()
                    && (m = getSeekAndDestroyMission(aiUnit, 8)) != null)

                // Missionary missions are only available to some units
                || (m = getMissionaryMission(aiUnit)) != null

                // Try to satisfy any remaining wishes, such as population
                || ((m = getWishRealizationMission(aiUnit,
                                                   workerWishes)) != null)

                // Another try to defend, with relaxed cost decider
                || (unit.isDefensiveUnit()
                    && (m = getDefendSettlementMission(aiUnit, true)) != null)

                // Another try to attack, at longer range
                || (unit.isOffensiveUnit()
                    && (m = getSeekAndDestroyMission(aiUnit, 16)) != null)

                // Leftover offensive units should go out looking for trouble
                || (unit.isOffensiveUnit()
                    && (m = getWanderHostileMission(aiUnit)) != null)
                ) {
                aiUnit.setMission(m);
                report += "\n  New-" + m.toString();
                aiUnits.remove(i);
            } else {
                i++;
            }
        }
        
        for (AIUnit aiUnit : aiUnits) {
            Mission m;
            if (aiUnit.getMission() instanceof IdleAtSettlementMission) {
                m = aiUnit.getMission();
            } else {
                m = new IdleAtSettlementMission(aiMain, aiUnit);
                aiUnit.setMission(m);
            }
            report += "\n  UNUSED-" + m
                + " at " + aiUnit.getUnit().getLocation();
        }
        for (AIUnit aiUnit : navalUnits) {
            Mission m;
            if (aiUnit.getMission() instanceof IdleAtSettlementMission) {
                m = aiUnit.getMission();
            } else {
                m = new IdleAtSettlementMission(aiMain, aiUnit);
                aiUnit.setMission(m);
            }
            report += "\n  UNUSED-" + m
                + " at " + aiUnit.getUnit().getLocation();
        }
        logger.fine(report);
    }

    /**
     * Gets a new BuildColonyMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A new mission, or null if impossible.
     */
    private Mission getBuildColonyMission(AIUnit aiUnit) {
        final Unit unit = aiUnit.getUnit();
        Location loc = (unit.getOwner().canBuildColonies()
            && unit.isColonist()
            && BuildColonyMission.invalidReason(aiUnit) == null)
            ? BuildColonyMission.findTarget(aiUnit, unit.isInEurope())
            : null;
        return (loc != null)
            ? new BuildColonyMission(getAIMain(), aiUnit, loc)
            : null;
    }

    /**
     * Gets a new CashInTreasureTrainMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A new mission, or null if impossible.
     */
    private Mission getCashInTreasureTrainMission(AIUnit aiUnit) {
        return (CashInTreasureTrainMission.invalidReason(aiUnit) == null)
            ? new CashInTreasureTrainMission(getAIMain(), aiUnit)
            : null;
    }

    /**
     * Gets a new DefendSettlementMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param relaxed Use a relaxed cost decider to choose the target.
     * @return A new mission, or null if impossible.
     */
    private Mission getDefendSettlementMission(AIUnit aiUnit, boolean relaxed) {
        if (DefendSettlementMission.invalidReason(aiUnit) != null) return null;
        final Unit unit = aiUnit.getUnit();
        final Location loc = unit.getLocation();
        double worstValue = 1000000.0;
        Colony worstColony = null;
        for (AIColony aic : getAIColonies()) {
            Colony colony = aic.getColony();
            if (aic.isBadlyDefended()) {
                if (Map.isSameLocation(colony.getTile(), loc)) {
                    worstColony = colony;
                    break;
                }
                double value = colony.getDefenceRatio() * 100.0
                    / unit.getTurnsToReach(loc, colony.getTile(),
                        unit.getCarrier(),
                        ((relaxed) ? CostDeciders.numberOfTiles() : null));
                if (worstValue > value) {
                    worstValue = value;
                    worstColony = colony;
                }
            }
        }
        return (worstColony == null) ? null
            : new DefendSettlementMission(getAIMain(), aiUnit, worstColony);
    }
                        
    /**
     * Gets a new MissionaryMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A new mission, or null if impossible.
     */
    private Mission getMissionaryMission(AIUnit aiUnit) {
        return (MissionaryMission.prepare(aiUnit) == null)
            ? new MissionaryMission(getAIMain(), aiUnit)
            : null;
    }

    /**
     * Gets a new PioneeringMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A new mission, or null if impossible.
     */
    private Mission getPioneeringMission(AIUnit aiUnit) {
        Location loc;
        return (PioneeringMission.prepare(aiUnit) == null
            && (loc = PioneeringMission.findTarget(aiUnit, true)) != null)
            // TODO: pioneers to make roads between colonies
            ? new PioneeringMission(getAIMain(), aiUnit, loc)
            : null;
    }

    /**
     * Gets a new PrivateerMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A new mission, or null if impossible.
     */
    private Mission getPrivateerMission(AIUnit aiUnit) {
        return (PrivateerMission.invalidReason(aiUnit) == null)
            ? new PrivateerMission(getAIMain(), aiUnit)
            : null;
    }

    /**
     * Gets a new ScoutingMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A new mission, or null if impossible.
     */
    private Mission getScoutingMission(AIUnit aiUnit) {
        return (ScoutingMission.invalidReason(aiUnit) == null)
            ? new ScoutingMission(getAIMain(), aiUnit)
            : null;
    }

    /**
     * Gets a UnitSeekAndDestroyMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     *
     * @return A new mission, or null if impossible.
     */
    public Mission getSeekAndDestroyMission(AIUnit aiUnit, int range) {
        if (UnitSeekAndDestroyMission.invalidReason(aiUnit)
            != null) return null;
        Location target = UnitSeekAndDestroyMission.findTarget(aiUnit, range);
        return (target == null) ? null
            : new UnitSeekAndDestroyMission(getAIMain(), aiUnit, target);
    }

    /**
     * Gets a new TransportMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A new mission, or null if impossible.
     */
    private Mission getTransportMission(AIUnit aiUnit) {
        return (TransportMission.invalidReason(aiUnit) == null)
            ? new TransportMission(getAIMain(), aiUnit)
            : null;
    }

    /**
     * Gets the best worker wish for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param workerWishes A map of unit type to wishes.
     * @return The best worker wish for the unit.
     */
    private WorkerWish getBestWorkerWish(AIUnit aiUnit, 
        java.util.Map<UnitType, List<Wish>> workerWishes) {
        final Unit unit = aiUnit.getUnit();
        List<Wish> wishList = workerWishes.get(unit.getType());
        WorkerWish nonTransported = null;
        WorkerWish transported = null;
        float bestNonTransportedValue = -1.0f;
        float bestTransportedValue = -1.0f;
        for (Wish w : wishList) {
            WorkerWish ww = (WorkerWish)w;
            int turns = unit.getTurnsToReach(ww.getDestination());
            if (turns == INFINITY) {
                if (bestTransportedValue < ww.getValue()) {
                    bestTransportedValue = ww.getValue();
                    transported = ww;
                }
            } else {
                if (bestNonTransportedValue < (float)ww.getValue() / turns) {
                    bestNonTransportedValue = (float)ww.getValue() / turns;
                    nonTransported = ww;
                }
            }
        }
        return (nonTransported != null) ? nonTransported
            : (transported != null) ? transported
            : null;
    }

    /**
     * Gets a new UnitWanderHostileMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A new mission, or null if impossible.
     */
    private Mission getWanderHostileMission(AIUnit aiUnit) {
        return (UnitWanderHostileMission.invalidReason(aiUnit) == null)
            ? new UnitWanderHostileMission(getAIMain(), aiUnit)
            : null;
    }

    /**
     * Gets a new WishRealizationMission for a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param workerWishes A map of unit type to wishes.
     * @param best Optionally override the <code>WorkerWish</code>.
     * @return A new mission, or null if impossible.
     */
    private Mission getWishRealizationMission(AIUnit aiUnit,
        java.util.Map<UnitType, List<Wish>> workerWishes) {
        WorkerWish best = getBestWorkerWish(aiUnit, workerWishes);
        if (best == null) return null;
        final Unit unit = aiUnit.getUnit();
        best.setTransportable(aiUnit);
        List<Wish> wishList = workerWishes.get(unit.getType());
        wishList.remove(best);
        workerWishes.put(unit.getType(), wishList);
        return new WishRealizationMission(getAIMain(), aiUnit, best);
    }

    /**
     * Brings gifts to nice players with nearby colonies.
     *
     * TODO: European players can also bring gifts! However,
     * this might be folded into a trade mission, since
     * European gifts are just a special case of trading.
     */
    private void bringGifts() {
        return;
    }

    /**
     * Demands goods from players with nearby colonies.
     *
     * TODO: European players can also demand tribute!
     */
    private void demandTribute() {
        return;
    }

    /**
     * Calls {@link AIColony#updateAIGoods()} for every colony this
     * player owns.
     */
    private void createAIGoodsInColonies() {
        logger.finest("Entering method createAIGoodsInColonies");
        for (AIColony aic : getAIColonies()) aic.updateAIGoods();
    }

    /**
     * Assign transportable units and goods to available carriers
     * transport lists.
     */
    private void createTransportLists() {
        if (!getPlayer().isEuropean()) return;
        List<Transportable> transportables = new ArrayList<Transportable>();

        // Collect non-naval units needing transport.
        for (AIUnit au : getAIUnits()) {
            if (!au.getUnit().isNaval()
                && au.getTransport() == null
                && au.getTransportDestination() != null) {
                transportables.add(au);
            }
        }

        // Add goods to transport
        for (AIColony aic : getAIColonies()) {
            for (AIGoods aig : aic.getAIGoods()) {
                if (aig.getTransportDestination() != null
                    && aig.getTransport() == null) {
                    transportables.add(aig);
                }
            }
        }

        // Update the priority.
        for (Transportable t : transportables) {
            t.increaseTransportPriority();
        }

        // Order the transportables by priority.
        Collections.sort(transportables,
            Transportable.transportableComparator);

        // Collect current transport missions.
        ArrayList<TransportMission> availableMissions
            = new ArrayList<TransportMission>();
        for (AIUnit au : getAIUnits()) {
            if (au.getMission() instanceof TransportMission) {
                availableMissions.add((TransportMission)au.getMission());
            }
        }

        // For all transportables, find the best carrier.
        // 
        // That is the one with space available that is closest.
        // TODO: this concentrates on packing, but ignores destinations
        // which is definitely going to be inefficient
        // TODO: be smarter about removing bestTransport from
        // availableMissions when it is full.
        while (!transportables.isEmpty() && !availableMissions.isEmpty()) {
            Transportable t = transportables.remove(0);
            Location loc = t.getTransportLocatable().getLocation();

            // Leave existing transport arrangements intact.
            if (loc instanceof Unit) {
                AIUnit aiCarrier = getAIUnit((Unit)loc);
                Mission m = aiCarrier.getMission();
                if (m instanceof TransportMission) {
                    TransportMission tm = (TransportMission)m;
                    if (tm.isOnTransportList(t)
                        || tm.addToTransportList(t)) {
                        logger.finest("Transport continuing: " + t);
                        continue;
                    }
                }
            }
        
            TransportMission bestTransport = null;
            int bestTransportSpace = 0;
            int bestTransportTurns = Integer.MAX_VALUE;
            for (TransportMission tm : availableMissions) {
                int transportSpace = tm.getAvailableSpace(t);
                if (transportSpace <= 0) continue;
                if (t instanceof AIUnit) {
                    if (!tm.getUnit().canCarryUnits()) continue;
                } else if (t instanceof AIGoods) {
                    if (!tm.getUnit().canCarryGoods()) continue;
                }                    
                if (t.getTransportSource() != null
                    && (t.getTransportSource().getTile()
                        == tm.getUnit().getTile())) {
                    if (bestTransportTurns > 0
                        || (bestTransportTurns == 0
                            && transportSpace > bestTransportSpace)) {
                        bestTransport = tm;
                        bestTransportSpace = transportSpace;
                        bestTransportTurns = 0;
                    }
                    continue;
                }
                PathNode p;
                int totalTurns = (t.getTransportDestination() != null
                    && t.getTransportDestination().getTile()
                    == tm.getUnit().getTile()) ? 0
                    : ((p = tm.getTransportPath(t)) == null) ? -1
                    : p.getTotalTurns();
                if (totalTurns <= 0) continue;
                if (totalTurns < bestTransportTurns
                    || (totalTurns == bestTransportTurns
                        && transportSpace > bestTransportSpace)) {
                    bestTransport = tm;
                    bestTransportSpace = transportSpace;
                    bestTransportTurns = totalTurns;
                }
            }
            if (bestTransport == null) {
                logger.finest("Transport unavailable: " + t);
                continue;
            } else if (bestTransport.addToTransportList(t)) {
                logger.finest("Transport found for: " + t
                    + " using: " + bestTransport);
            } else {
                logger.finest("Transport failed for: " + t
                    + " using: " + bestTransport);
            }
        }
    }


    // AIPlayer interface

    /**
     * Tells this <code>AIPlayer</code> to make decisions. The
     * <code>AIPlayer</code> is done doing work this turn when this method
     * returns.
     */
    public void startWorking() {
        Turn turn = getGame().getTurn();
        logger.finest(getClass().getName() + " in " + turn
            + ": " + Utils.lastPart(getPlayer().getNationID(), "."));
        sessionRegister.clear();
        clearAIUnits();
        cheat();
        determineStances();
        if (turn.isFirstTurn()) initializeMissions();
        buildTipMap();
        rearrangeWorkersInColonies();
        abortInvalidAndOneTimeMissions();
        giveNormalMissions();
        bringGifts();
        demandTribute();
        createAIGoodsInColonies();
        createTransportLists();
        doMissions();
        rearrangeWorkersInColonies();
        abortInvalidMissions();
        giveNormalMissions();
        doMissions();
        rearrangeWorkersInColonies();
        abortInvalidMissions();
        clearAIUnits();
    }

    /**
     * Simple initialization of AI missions given that we know the starting
     * conditions.
     */
    private void initializeMissions() {
        // Full debug setup is very different.
        if (FreeColDebugger.getDebugLevel() >= FreeColDebugger.DEBUG_FULL)
            return;

        AIMain aiMain = getAIMain();

        // Give the ship a transport mission.
        TransportMission tm = null;
        for (AIUnit aiu : getAIUnits()) {
            Unit u = aiu.getUnit();
            if (u.isNaval() && !aiu.hasMission()) {
                aiu.setMission(tm = new TransportMission(aiMain, aiu));
            }
        }

        // Find a colony site, give the land units build colony missions,
        // and add them to the ship's transport mission.
        Location target = null;
        for (AIUnit aiu : getAIUnits()) {
            Unit u = aiu.getUnit();
            if (!u.isNaval() && !aiu.hasMission()) {
                if (target == null) {
                    target = BuildColonyMission.findTarget(aiu, false);
                }
                aiu.setMission(new BuildColonyMission(aiMain, aiu, target));
                tm.addToTransportList(aiu);                
            }
        }
    }

    /**
     * Evaluates a proposed mission type for a unit, specialized for
     * European players.
     *
     * @param aiUnit The <code>AIUnit</code> to perform the mission.
     * @param path A <code>PathNode</code> to the target of this mission.
     * @param type The mission type.
     * @return A score representing the desirability of this mission.
     */
    public int scoreMission(AIUnit aiUnit, PathNode path, Class type) {
        int value = super.scoreMission(aiUnit, path, type);
        if (value > 0) {
            if (type == DefendSettlementMission.class) {
                // Reduce value in proportion to the number of defenders.
                Colony colony = (Colony)DefendSettlementMission
                    .extractTarget(aiUnit, path);
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
     * Decides whether to accept an Indian demand, or not.
     *
     * @param unit The <code>Unit</code> making demands.
     * @param colony The <code>Colony</code> where demands are being made.
     * @param goods The <code>Goods</code> demanded.
     * @param gold The amount of gold demanded.
     * @return True if this player accepts the demand.
     */
    public boolean indianDemand(Unit unit, Colony colony,
                                Goods goods, int gold) {
        // TODO: make a better choice, check whether the colony is
        // well defended
        return !"conquest".equals(getAIAdvantage());
    }

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
                        validOffer = false; //never accept invalid stance change
                        break;
                    case WAR: // always accept war without cost
                        break;
                    case CEASE_FIRE:
                        value -= 500;
                        break;
                    case PEACE:
                        if (!agreement.getSender().hasAbility("model.ability.alwaysOfferedPeace")) {
                            // TODO: introduce some kind of counter in order to avoid
                            // Benjamin Franklin exploit
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
                    value -= getPlayer().getMarket().getBidPrice(goods.getType(), goods.getAmount());
                } else {
                    value += getPlayer().getMarket().getSalePrice(goods.getType(), goods.getAmount());
                }
            }
        }
        if (validOffer) {
            logger.info("Trade value is " + value + ", accept if >=0");
        } else {
            logger.info("Trade offer is considered invalid!");
        }
        return (value>=0)&&validOffer;
    }


    /**
     * Called after another <code>Player</code> sends a
     * <code>trade</code> message
     *
     * @param goods The <code>Goods</code> to offer.
     */
    public void registerSellGoods(Goods goods) {
        String goldKey = "tradeGold#" + goods.getType().getId()
            + "#" + goods.getAmount() + "#" + goods.getLocation().getId();
        sessionRegister.put(goldKey, null);
    }

    /**
     * Called when another <code>Player</code> proposes to buy.
     *
     * TODO: this obviously applies only to native players. Is there
     * an European equivalent?
     *
     * @param unit The foreign <code>Unit</code> trying to trade.
     * @param settlement The <code>Settlement</code> this player owns and
     *            which the given <code>Unit</code> is trading.
     * @param goods The goods the given <code>Unit</code> is trying to sell.
     * @param gold The suggested price.
     * @return The price this <code>AIPlayer</code> suggests or
     *         {@link NetworkConstants#NO_TRADE}.
     */
    public int buyProposition(Unit unit, Settlement settlement, Goods goods, int gold) {
        logger.finest("Entering method buyProposition");
        Player buyer = unit.getOwner();
        String goldKey = "tradeGold#" + goods.getType().getId()
            + "#" + goods.getAmount() + "#" + settlement.getId();
        String hagglingKey = "tradeHaggling#" + unit.getId();
        Integer registered = sessionRegister.get(goldKey);
        if (registered == null) {
            int price = ((IndianSettlement) settlement).getPriceToSell(goods)
                + getPlayer().getTension(buyer).getValue();
            Unit missionary = ((IndianSettlement) settlement).getMissionary(buyer);
            if (missionary != null && getSpecification()
                .getBoolean("model.option.enhancedMissionaries")) {
                // 10% bonus for missionary, 20% if expert
                int bonus = (missionary.hasAbility(Ability.EXPERT_MISSIONARY)) ? 8
                    : 9;
                price = (price * bonus) / 10;
            }
            sessionRegister.put(goldKey, new Integer(price));
            return price;
        } else {
            int price = registered.intValue();
            if (price < 0 || price == gold) {
                return price;
            } else if (gold < (price * 9) / 10) {
                logger.warning("Cheating attempt: sending a offer too low");
                sessionRegister.put(goldKey, new Integer(-1));
                return NetworkConstants.NO_TRADE;
            } else {
                int haggling = 1;
                if (sessionRegister.containsKey(hagglingKey)) {
                    haggling = sessionRegister.get(hagglingKey).intValue();
                }
                if (Utils.randomInt(logger, "Buy gold", getAIRandom(),
                        3 + haggling) <= 3) {
                    sessionRegister.put(goldKey, new Integer(gold));
                    sessionRegister.put(hagglingKey, new Integer(haggling + 1));
                    return gold;
                } else {
                    sessionRegister.put(goldKey, new Integer(-1));
                    return NetworkConstants.NO_TRADE_HAGGLE;
                }
            }
        }
    }

    /**
     * Called when another <code>Player</code> proposes a sale.
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
        // don't pay for more than fits in the warehouse
        int amount = colony.getWarehouseCapacity() - colony.getGoodsCount(goods.getType());
        amount = Math.min(amount, goods.getAmount());
        // get a good price
        Tension.Level tensionLevel = getPlayer().getTension(otherPlayer).getLevel();
        int percentage = (9 - tensionLevel.ordinal()) * 10;
        // what we could get for the goods in Europe (minus taxes)
        int netProfits = ((100 - getPlayer().getTax())
                          * getPlayer().getMarket().getSalePrice(goods.getType(), amount)) / 100;
        int price = (netProfits * percentage) / 100;
        return price;

    }

    /**
     * Decides whether to accept the monarch's tax raise or not.
     *
     * @param tax The new tax rate to be considered.
     * @return <code>true</code> if the tax raise should be accepted.
     */
    public boolean acceptTax(int tax) {
        Goods toBeDestroyed = getPlayer().getMostValuableGoods();
        if (toBeDestroyed == null) {
            return false;
        }

        GoodsType goodsType = toBeDestroyed.getType();
        if (goodsType.isFoodType() || goodsType.isBreedable()) {
            // we should be able to produce food and horses ourselves
            // TODO: check whether we already have horses!
            return false;
        } else if (goodsType.isMilitaryGoods() ||
                   goodsType.isTradeGoods() ||
                   goodsType.isBuildingMaterial()) {
            if (getGame().getTurn().getAge() == 3) {
                // by this time, we should be able to produce
                // enough ourselves
                // TODO: check whether we have an armory, at least
                return false;
            } else {
                return true;
            }
        } else {
            int averageIncome = 0;
            int numberOfGoods = 0;
            // TODO: consider the amount of goods produced. If we
            // depend on shipping huge amounts of cheap goods, we
            // don't want these goods to be boycotted.
            List<GoodsType> goodsTypes = getSpecification().getGoodsTypeList();
            for (GoodsType type : goodsTypes) {
                if (type.isStorable()) {
                    averageIncome += getPlayer().getIncomeAfterTaxes(type);
                    numberOfGoods++;
                }
            }
            averageIncome = averageIncome / numberOfGoods;
            if (getPlayer().getIncomeAfterTaxes(toBeDestroyed.getType()) > averageIncome) {
                // this is a more valuable type of goods
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Decides to accept an offer of mercenaries or not.
     * TODO: make a better choice.
     *
     * @return True if the mercenaries are accepted.
     */
    public boolean acceptMercenaries() {
        return getPlayer().isAtWar() || "conquest".equals(getAIAdvantage());
    }

    /**
     * Selects the most useful founding father offered.
     * TODO: improve choice
     *
     * @param foundingFathers The founding fathers on offer.
     * @return The founding father selected.
     */
    public FoundingFather selectFoundingFather(List<FoundingFather> foundingFathers) {
        int age = getGame().getTurn().getAge();
        FoundingFather bestFather = null;
        int bestWeight = Integer.MIN_VALUE;
        for (FoundingFather father : foundingFathers) {
            if (father == null) continue;

            // For the moment, arbitrarily: always choose the one
            // offering custom houses.  Allowing the AI to build CH
            // early alleviates the complexity problem of handling all
            // TransportMissions correctly somewhat.
            if (father.hasAbility("model.ability.buildCustomHouse")) {
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
}
