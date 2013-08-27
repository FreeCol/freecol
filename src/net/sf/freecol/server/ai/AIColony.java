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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitWas;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIGoods;
import net.sf.freecol.server.ai.GoodsWish;
import net.sf.freecol.server.ai.TileImprovementPlan;
import net.sf.freecol.server.ai.WorkerWish;
import net.sf.freecol.server.ai.mission.BuildColonyMission;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.IdleAtSettlementMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.PioneeringMission;
import net.sf.freecol.server.ai.mission.ScoutingMission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.WorkInsideColonyMission;

import org.w3c.dom.Element;


/**
 * Objects of this class contains AI-information for a single {@link Colony}.
 */
public class AIColony extends AIObject implements PropertyChangeListener {

    private static final Logger logger = Logger.getLogger(AIColony.class.getName());

    private static final String LIST_ELEMENT = "ListElement";

    /**
     * Do not perform tile improvements that would leave less than
     * this amount of forested work locations available to the colony.
     */
    private static final int FOREST_MINIMUM = 1;

    /** Do not bother trying to ship out less than this amount of goods. */
    private static final int EXPORT_MINIMUM = 10;

    /** The colony this AIColony is managing. */
    private Colony colony;

    /** The current plan for the colony.  Does not need to be serialized. */
    private ColonyPlan colonyPlan = null;

    /** Goods to export from the colony. */
    private final List<AIGoods> aiGoods;

    /** Useful things for the colony. */
    private final List<Wish> wishes;

    /** Plans to improve neighbouring tiles. */
    private final List<TileImprovementPlan> tileImprovementPlans;

    /** When should the workers in this Colony be rearranged? */
    private Turn rearrangeTurn = new Turn(0);

    /**
     * Goods that should be completely exported and only exported to
     * prevent the warehouse filling.
     */
    private static final Set<GoodsType> fullExport = new HashSet<GoodsType>();
    private static final Set<GoodsType> partExport = new HashSet<GoodsType>();

    /**
     * Comparator to favour expert scouts, then units in that role,
     * then least skillful.
     */
    private static final Comparator<Unit> scoutComparator
        = new Comparator<Unit>() {
            public int compare(Unit u1, Unit u2) {
                boolean a1 = u1.hasAbility(Ability.EXPERT_SCOUT);
                boolean a2 = u2.hasAbility(Ability.EXPERT_SCOUT);
                if (a1 != a2) return (a1) ? -1 : 1;
                a1 = u1.hasAbility(Ability.SCOUT_INDIAN_SETTLEMENT);
                a2 = u2.hasAbility(Ability.SCOUT_INDIAN_SETTLEMENT);
                if (a1 != a2) return (a1) ? -1 : 1;
                return u1.getType().getSkill() - u2.getType().getSkill();
            }
        };


    /**
     * Creates a new uninitialized <code>AIColony</code>.
     *
     * @param aiMain The main AI-object.
     * @param id The object identifier.
     */
    public AIColony(AIMain aiMain, String id) {
        super(aiMain, id);

        this.colony = null;
        this.colonyPlan = null;
        this.aiGoods = new ArrayList<AIGoods>();
        this.wishes = new ArrayList<Wish>();
        this.tileImprovementPlans = new ArrayList<TileImprovementPlan>();
    }

    /**
     * Creates a new <code>AIColony</code>.
     *
     * @param aiMain The main AI-object.
     * @param colony The colony to make an {@link AIObject} for.
     */
    public AIColony(AIMain aiMain, Colony colony) {
        this(aiMain, colony.getId());

        this.colony = colony;
        colony.addPropertyChangeListener(Colony.REARRANGE_WORKERS, this);

        uninitialized = false;
    }

    /**
     * Creates a new <code>AIColony</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param element The root element for the XML-representation
     *       of a <code>Wish</code>.
     */
    public AIColony(AIMain aiMain, Element element) {
        this(aiMain, (String)null);

        readFromXMLElement(element);
        addAIObjectWithId();

        uninitialized = getColony() == null;
    }

    /**
     * Creates a new <code>AIColony</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public AIColony(AIMain aiMain, FreeColXMLReader xr) throws XMLStreamException {
        this(aiMain, (String)null);

        readFromXML(xr);
        addAIObjectWithId();

        uninitialized = getColony() == null;
    }


    /**
     * Gets the <code>Colony</code> this <code>AIColony</code> controls.
     *
     * @return The <code>Colony</code>.
     */
    public Colony getColony() {
        return colony;
    }

    protected AIUnit getAIUnit(Unit unit) {
        return getAIMain().getAIUnit(unit);
    }

    protected AIPlayer getAIOwner() {
        return getAIMain().getAIPlayer(colony.getOwner());
    }

    /**
     * Is a colony badly defended?
     * Deliberately does not require defenders for small colonies.
     *
     * @param colony The <code>Colony</code> to consider.
     * @return True if the colony needs more defenders.
     */
    public static boolean isBadlyDefended(Colony colony) {
        return colony.getTotalDefencePower()
            < 1.25f * colony.getUnitCount() - 2.5f;
    }

    /**
     * Is this colony badly defended?
     *
     * @return True if this colony needs more defenders.
     */
    public boolean isBadlyDefended() {
        return isBadlyDefended(colony);
    }

    /**
     * Rearranges the workers within this colony using the {@link ColonyPlan}.
     * TODO: Detect military threats and boost defence.
     *
     * @return True if the workers were rearranged.
     */
    public boolean rearrangeWorkers() {
        final AIMain aiMain = getAIMain();
        if (colonyPlan == null) colonyPlan = new ColonyPlan(aiMain, colony);

        // Skip this colony if it does not yet need rearranging, but
        // first check if it is collapsing.
        final int turn = getGame().getTurn().getNumber();
        if (colony.getUnitCount() <= 0) {
            avertAutoDestruction();
        } else if (rearrangeTurn.getNumber() > turn) {
            if (colony.getCurrentlyBuilding() == null
                && colonyPlan.getBestBuildableType() != null) {
                logger.warning(colony.getName() + " could be building but"
                    + " is asleep until turn: " + rearrangeTurn.getNumber()
                    + "( > " + turn + ")");
            }
            return false;
        }

        final Tile tile = colony.getTile();
        final Player player = colony.getOwner();
        final Specification spec = getSpecification();

        // For now, cap the rearrangement horizon, because confidence
        // that we are triggering on all relevant changes is low.
        int nextRearrange = 15;

        // See if there are neighbouring LCRs to explore, or tiles
        // to steal, or just unclaimed tiles (a neighbouring settlement
        // might have disappeared or relinquished a tile).
        // This needs to be done early so that new tiles can be
        // included in any new colony plan.
        exploreLCRs();
        stealTiles();
        for (Tile t : tile.getSurroundingTiles(1)) {
            if (!player.owns(t) && player.canClaimForSettlement(t)) {
                AIMessage.askClaimLand(t, this, 0);
            }
        }

        // Update the colony plan.
        colonyPlan.update();

        // Now that we know what raw materials are available in the
        // colony plan, set the current buildable, first backing out
        // of anything currently being built that is now impossible.
        // If a buildable is chosen, refine the worker allocation in
        // the colony plan in case the required building materials
        // have changed.
        BuildableType oldBuild = colony.getCurrentlyBuilding();
        BuildableType build = colonyPlan.getBestBuildableType();
        if (build != oldBuild) {
            List<BuildableType> queue = new ArrayList<BuildableType>();
            if (build != null) queue.add(build);
            AIMessage.askSetBuildQueue(this, queue);
            build = colony.getCurrentlyBuilding();
        }
        colonyPlan.refine(build);

        // Collect all potential workers from the colony and from the tile,
        // being careful not to disturb existing non-colony missions.
        // Note the special case of a unit aiming to build a colony on this
        // tile, which happens regularly with the initial AI colony.
        // Remember where the units came from.
        List<Unit> workers = colony.getUnitList();
        List<UnitWas> was = new ArrayList<UnitWas>();
        for (Unit u : workers) {
            Location loc = u.getLocation();
            was.add(new UnitWas(u));
        }
        for (Unit u : tile.getUnitList()) {
            if (!u.isPerson() || getAIUnit(u) == null) continue;
            Mission mission = getAIUnit(u).getMission();
            if (mission == null
                || mission instanceof IdleAtSettlementMission
                || mission instanceof WorkInsideColonyMission
                || (mission instanceof BuildColonyMission
                    && ((BuildColonyMission)mission).getTarget() == tile)
                // TODO: drop this when the AI stops building excessive armies
                || mission instanceof DefendSettlementMission) {
                workers.add(u);
                was.add(new UnitWas(u));
            }
        }
        // Assign the workers according to the colony plan.
        // ATM we just accept this assignment unless it failed, in
        // which case restore original state.
        AIPlayer aiPlayer = getAIOwner();
        boolean preferScouts = ((EuropeanAIPlayer)aiPlayer).scoutsNeeded() > 0;
        Colony scratch = colonyPlan.assignWorkers(new ArrayList<Unit>(workers),
                                                  preferScouts);
        if (scratch == null) {
            rearrangeTurn = new Turn(turn + 1);
            return false;
        }

        // Apply the arrangement, and give suitable missions to all units.
        AIMessage.askRearrangeColony(this, workers, scratch);

        // Emergency recovery if something broke and the colony is empty.
        if (colony.getUnitCount() <= 0) {
            String destruct = "Autodestruct at " + colony.getName()
                + " in " + turn + ":";
            for (UnitWas uw : was) destruct += "\n" + uw.toString();
            logger.warning(destruct);
            avertAutoDestruction();
        }

        // Argh.  We may have chosen to build something we can no
        // longer build due to some limitation.  Try to find a
        // replacement, but do not re-refine/assign as that process is
        // sufficiently complex that we can not be confident that this
        // will not loop indefinitely.  The compromise is to just
        // rearrange next turn until we get out of this state.
        if (build != null && !colony.canBuild(build)) {
            logger.warning(colony.getName() + " reneged building "
                + build.getSuffix() + ": " + colony.getNoBuildReason(build));
            List<BuildableType> queue = new ArrayList<BuildableType>();
            build = colonyPlan.getBestBuildableType();
            if (build != null) queue.add(build);
            AIMessage.askSetBuildQueue(this, queue);
            nextRearrange = 1;
        }

        // Now that all production has been stabilized, plan to
        // rearrange when the warehouse hits a limit.
        if (colony.getNetProductionOf(spec.getPrimaryFoodType()) < 0) {
            int net = colony.getNetProductionOf(spec.getPrimaryFoodType());
            int when = colony.getGoodsCount(spec.getPrimaryFoodType()) / -net;
            nextRearrange = Math.max(0, Math.min(nextRearrange, when-1));
        }
        int warehouse = colony.getWarehouseCapacity();
        for (GoodsType g : spec.getGoodsTypeList()) {
            if (!g.isStorable() || g.isFoodType()) continue;
            int have = colony.getGoodsCount(g);
            int net = colony.getAdjustedNetProductionOf(g);
            if (net >= 0 && (have >= warehouse || g.limitIgnored())) continue;
            int when = (net < 0) ? (have / -net - 1)
                : (net > 0) ? ((warehouse - have) / net - 1)
                : Integer.MAX_VALUE;
            nextRearrange = Math.max(1, Math.min(nextRearrange, when));
        }

        // Log the changes.
        build = colony.getCurrentlyBuilding();
        String buildStr = (build != null) ? build.toString()
            : ((build = colonyPlan.getBestBuildableType()) != null)
            ? "unexpected-null(" + build.toString() + ")"
            : "expected-null";
        String report = "Rearrange " + colony.getName()
            + " (" + colony.getUnitCount() + ")"
            + " build=" + buildStr
            + " " + getGame().getTurn()
            + " + " + nextRearrange;
        for (UnitWas uw : was) report += "\n" + uw.toString();
        logger.finest(report);

        // Give suitable missions to all units.
        for (Unit u : colony.getUnitList()) {
            AIUnit aiU = getAIUnit(u);
            if (aiU.getMission() instanceof WorkInsideColonyMission
                && ((WorkInsideColonyMission)aiU.getMission()).getAIColony()
                == this) {
                ;// Do nothing
            } else {
                aiU.setMission(new WorkInsideColonyMission(aiMain, aiU, this));
            }
        }
        EuropeanAIPlayer aip = (EuropeanAIPlayer)aiMain.getAIPlayer(player);
        boolean pioneersWanted = aip.pioneersNeeded() > 0;
        Tile pioneerTile;
        for (Unit u : tile.getUnitList()) {
            AIUnit aiU = getAIUnit(u);
            if (aiU == null || aiU.getMission() != null) continue;
            Mission m = null;
            if (u.isArmed()) {
                m = new DefendSettlementMission(aiMain, aiU, colony);
            } else if (u.hasAbility(Ability.SCOUT_INDIAN_SETTLEMENT)) {
                if (preferScouts) m = aip.getScoutingMission(aiU);
            } else if (u.hasAbility(Ability.IMPROVE_TERRAIN)) {
                if (pioneersWanted) m = aip.getPioneeringMission(aiU);
            } else if (u.hasAbility(Ability.ESTABLISH_MISSION)) {
                m = aip.getMissionaryMission(aiU);
            }
            if (m != null) aiU.setMission(m);
        }

        // Change the export settings when required.
        resetExports();

        createTileImprovementPlans();
        updateWishes();

        // Set the next rearrangement turn.
        rearrangeTurn = new Turn(turn + nextRearrange);
        return true;
    }

    /**
     * Reset the export settings.
     * This is always needed even when there is no customs house, because
     * updateAIGoods needs to know what to export by transport.
     * TODO: consider market prices?
     */
    private void resetExports() {
        final Specification spec = getSpecification();
        if (fullExport.isEmpty()) {
            // Initialize the exportable sets.
            // Luxury goods, non-raw materials (silver), and raw
            // materials we do not produce (might have been gifted)
            // should always be fully exported.  Other raw and
            // manufactured goods should be exported only to the
            // extent of not filling the warehouse.
            for (GoodsType g : spec.getGoodsTypeList()) {
                if (g.isStorable()
                    && !g.isFoodType()
                    && !g.isBuildingMaterial()
                    && !g.isMilitaryGoods()
                    && !g.isTradeGoods()) {
                    if (g.isRawMaterial()) {
                        partExport.add(g);
                    } else {
                        fullExport.add(g);
                    }
                }
            }
            for (EquipmentType e : spec.getEquipmentTypeList()) {
                for (AbstractGoods ag : e.getRequiredGoods()) {
                    if (fullExport.contains(ag.getType())) {
                        fullExport.remove(ag.getType());
                        partExport.add(ag.getType());
                    }
                }
            }
        }

        if (colony.getOwner().getMarket() == null) {
            // Do not export when there is no market!
            for (GoodsType g : spec.getGoodsTypeList()) {
                colony.getExportData(g).setExported(false);
            }
        } else {
            int exportLevel = 4 * colony.getWarehouseCapacity() / 5;
            for (GoodsType g : spec.getGoodsTypeList()) {
                if (fullExport.contains(g)) {
                    colony.getExportData(g).setExportLevel(0);
                    colony.getExportData(g).setExported(true);
                } else if (partExport.contains(g)) {
                    colony.getExportData(g).setExportLevel(exportLevel);
                    colony.getExportData(g).setExported(true);
                } else {
                    colony.getExportData(g).setExported(false);
                }
            }
        }
    }

    /**
     * Explores any neighbouring LCRs.
     * Choose non-expert persons for the exploration.
     */
    private void exploreLCRs() {
        final Tile tile = colony.getTile();
        List<Unit> explorers = new ArrayList<Unit>();
        for (Unit u : tile.getUnitList()) {
            if (u.isPerson()
                && (u.getType().getSkill() <= 0
                    || u.hasAbility(Ability.EXPERT_SCOUT))) {
                explorers.add(u);
            }
        }
        Collections.sort(explorers, scoutComparator);
        for (Tile t : tile.getSurroundingTiles(1)) {
            if (t.hasLostCityRumour()) {
                Direction direction = tile.getDirection(t);
                for (;;) {
                    if (explorers.isEmpty()) return;
                    Unit u = explorers.remove(0);
                    if (!u.getMoveType(t).isProgress()) continue;
                    if (getAIUnit(u).move(direction)
                        && !t.hasLostCityRumour()) {
                        u.setDestination(tile);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Steals neighbouring tiles but only if the colony has some
     * defence.  Grab everything if at war with the owner, otherwise
     * just take the tile that best helps with the currently required
     * raw building materials, with a lesser interest in food.
     */
    private void stealTiles() {
        final Specification spec = getSpecification();
        final Tile tile = colony.getTile();
        final Player player = colony.getOwner();
        boolean hasDefender = false;
        for (Unit u : tile.getUnitList()) {
            if (u.isDefensiveUnit()
                && getAIUnit(u).getMission() instanceof DefendSettlementMission) {
                // TODO: be smarter
                hasDefender = true;
                break;
            }
        }
        if (!hasDefender) return;

        // What goods are really needed?
        List<GoodsType> needed = new ArrayList<GoodsType>();
        for (GoodsType g : spec.getRawBuildingGoodsTypeList()) {
            if (colony.getTotalProductionOf(g) <= 0) needed.add(g);
        }

        // If a tile can be stolen, do so if already at war with the
        // owner or if it is the best one available.
        UnitType unitType = spec.getDefaultUnitType();
        Tile steal = null;
        float score = 1.0f;
        for (Tile t : tile.getSurroundingTiles(1)) {
            Player owner = t.getOwner();
            if (owner == null || owner == player
                || owner.isEuropean()
                || !player.canClaimForSettlement(t)) continue;
            if (owner.atWarWith(player)) {
                if (AIMessage.askClaimLand(t, this, NetworkConstants.STEAL_LAND)
                    && player.owns(t)) {
                    logger.info(player.getName() + " stole tile " + t
                        + " from hostile " + owner.getName());
                }
            } else {
                // Pick the best tile to steal, considering mainly the
                // building goods needed, but including food at a lower
                // weight.
                float s = 0.0f;
                for (GoodsType g : needed) {
                    s += t.potential(g, unitType);
                }
                for (GoodsType g : spec.getFoodGoodsTypeList()) {
                    s += 0.1 * t.potential(g, unitType);
                }
                if (s > score) {
                    score = s;
                    steal = t;
                }
            }
        }
        if (steal != null) {
            Player owner = steal.getOwner();
            if (AIMessage.askClaimLand(steal, this, NetworkConstants.STEAL_LAND)
                && player.owns(steal)) {
                logger.info(player.getName() + " stole tile " + steal
                    + " (score = " + score
                    + ") from " + owner.getName());
            }
        }
    }

    /**
     * Something bad happened, there is no remaining unit working in
     * the colony.
     *
     * Throwing an exception stalls the AI and wrecks the colony in a
     * weird way.  Try to recover by hopefully finding a unit outside
     * the colony and stuffing it into the town hall.
     */
    private void avertAutoDestruction() {
        String msg = "Colony " + colony.getName()
            + " rearrangement leaves no units, "
            + colony.getTile().getUnitCount() + " available";
        for (Unit u : colony.getTile().getUnitList()) {
            msg += ", " + u.toString();
        }

        List<GoodsType> libertyGoods = getSpecification()
            .getLibertyGoodsTypeList();
        for (Unit u : colony.getTile().getUnitList()) {
            if (!u.isPerson()) continue;
            for (WorkLocation wl : colony.getAvailableWorkLocations()) {
                if (!wl.canAdd(u)) continue;
                for (GoodsType type : libertyGoods) {
                    if (wl.getPotentialProduction(type, u.getType()) > 0
                        && AIMessage.askWork(getAIUnit(u), wl)
                        && u.getLocation() == wl) {
                        AIMessage.askChangeWorkType(getAIUnit(u), type);
                        msg += ".  Autodestruct averted with " + u + ".";
                        logger.warning(msg);
                        break;
                    }
                }
            }
        }
        // No good, no choice but to fail.
        if (colony.getUnitCount() <= 0) {
            throw new IllegalStateException(msg);
        }
    }

    /**
     * Stop using a work location.
     *
     * Called from BuildColonyMission to clear a colony tile that is about
     * to have a colony built on it.
     *
     * @param wl The <code>WorkLocation</code> to stop using.
     */
    public void stopUsing(WorkLocation wl) {
        for (Unit u : wl.getUnitList()) {
            AIMessage.askPutOutsideColony(getAIUnit(u));
        }
        if (colony.getUnitCount() <= 0) avertAutoDestruction();
        rearrangeTurn = new Turn(getGame().getTurn().getNumber());
    }

    /**
     * Gets the goods to be exported from this AI colony.
     *
     * @return A copy of the aiGoods list.
     */
    public List<AIGoods> getAIGoods() {
        return new ArrayList<AIGoods>(aiGoods);
    }

    /**
     * Removes the given <code>AIGoods</code> from this colony's list. The
     * <code>AIGoods</code>-object is not disposed as part of this operation.
     * Use that method instead to remove the object completely (this method
     * would then be called indirectly).
     *
     * @param ag The <code>AIGoods</code> to be removed.
     * @see AIGoods#dispose()
     */
    public void removeAIGoods(AIGoods ag) {
        while (aiGoods.remove(ag)) {} /* Do nothing here */
    }

    /**
     * Drops some goods from the goods list, and cancels any transport.
     *
     * @param ag The <code>AIGoods</code> to drop.
     */
    private void dropGoods(AIGoods ag) {
        if (ag.getTransport() != null
            && ag.getTransport().getMission() instanceof TransportMission) {
            ((TransportMission)ag.getTransport().getMission())
                .removeTransportable((Transportable)ag);
        }
        removeAIGoods(ag);
        ag.dispose();
    }

    /**
     * Emits a standard message regarding the state of AIGoods.
     *
     * @param ag The <code>AIGoods</code> to log.
     * @param action The state of the goods.
     */
    private void goodsLog(AIGoods ag, String action) {
        if (logger.isLoggable(Level.FINEST)) {
            Goods goods = (ag == null) ? null : ag.getGoods();
            int amount = (goods == null) ? -1 : goods.getAmount();
            String type = (goods == null) ? "(null)"
                : ag.getGoods().getType().getSuffix();
            logger.finest(String.format("%-20s %-10s %s %s %s",
                    colony.getName(), action,
                    ((ag == null) ? "(null)" : ag.getId()),
                    ((amount >= GoodsContainer.CARGO_SIZE) ? "full"
                        : Integer.toString(amount)), type));
        }
    }

    /**
     * Creates a list of the goods which should be shipped out of this colony.
     */
    public void updateAIGoods() {
        if (colony.hasAbility(Ability.EXPORT)) {
            while (!aiGoods.isEmpty()) {
                AIGoods ag = aiGoods.remove(0);
                goodsLog(ag, "customizes");
                dropGoods(ag);
            }
            return;
        }

        int i = 0;
        while (i < aiGoods.size()) {
            AIGoods ag = aiGoods.get(i);
            if (ag == null) {
                aiGoods.remove(i);
            } else if (ag.checkIntegrity(false) < 0) {
                goodsLog(ag, "reaps");
                dropGoods(ag);
            } else if (ag.getGoods().getLocation() != colony) {
                // On its way, no longer of interest here, but do not dispose
                // as that will happen when delivered.
                goodsLog(ag, "sends");
                aiGoods.remove(i);
            } else if (colony.getAdjustedNetProductionOf(ag.getGoods()
                    .getType()) < 0) {
                goodsLog(ag, "needs");
                dropGoods(ag);
            } else {
                i++;
            }
        }

        final Europe europe = colony.getOwner().getEurope();
        final int capacity = colony.getWarehouseCapacity();
        List<AIGoods> newAIGoods = new ArrayList<AIGoods>();
        for (GoodsType g : getSpecification().getGoodsTypeList()) {
            if (colony.getAdjustedNetProductionOf(g) < 0) continue;
            int count = colony.getGoodsCount(g);
            int exportAmount = (fullExport.contains(g))
                ? count
                : (partExport.contains(g))
                ? count - colony.getExportData(g).getExportLevel()
                : -1;
            int priority = (exportAmount >= capacity)
                ? Transportable.IMPORTANT_DELIVERY
                : (exportAmount >= GoodsContainer.CARGO_SIZE)
                ? Transportable.FULL_DELIVERY
                : 0;

            // Find all existing AI goods of type g
            //   update amount of goods to export
            //   reduce exportAmount at each step, dropping excess exports
            i = 0;
            while (i < aiGoods.size()) {
                AIGoods ag = aiGoods.get(i);
                Goods goods = ag.getGoods();
                if (goods.getType() != g) {
                    i++;
                    continue;
                }
                int amount = goods.getAmount();
                if (amount <= exportAmount) {
                    if (amount < GoodsContainer.CARGO_SIZE) {
                        amount = Math.min(exportAmount,
                            GoodsContainer.CARGO_SIZE);
                        goods.setAmount(amount);
                        ag.setTransportPriority(priority);
                    }
                    goodsLog(ag, "exports");
                } else if (exportAmount >= EXPORT_MINIMUM) {
                    goods.setAmount(exportAmount);
                    goodsLog(ag, "clamps");
                } else {
                    goodsLog(ag, "unexports");
                    dropGoods(ag);
                    continue;
                }
                exportAmount -= amount;
                i++;
            }

            // Export new goods, to Europe if possible.
            Location destination = (colony.getOwner().canTrade(g)) ? europe
                : null;
            while (exportAmount >= EXPORT_MINIMUM) {
                int amount = Math.min(exportAmount, GoodsContainer.CARGO_SIZE);
                AIGoods newGoods = new AIGoods(getAIMain(), colony, g,
                                               amount, destination);
                newGoods.setTransportPriority(priority);
                newAIGoods.add(newGoods);
                goodsLog(newGoods, "makes");
                exportAmount -= amount;
            }
        }
        aiGoods.addAll(newAIGoods);
        Collections.sort(aiGoods, Transportable.transportableComparator);
    }


    /**
     * Adds a <code>Wish</code> to the wishes list.
     *
     * @param wish The <code>Wish</code> to be added.
     */
    public void addWish(Wish wish) {
        wishes.add(wish);
    }

    /**
     * Tries to complete a supplied wish.
     *
     * @param wish The <code>Wish</code> to complete.
     * @return True if this wish was successfully completed.
     */
    public boolean completeWish(Wish wish, String reason) {
        if (!wishes.remove(wish)) return false;
        ((EuropeanAIPlayer)getAIOwner()).completeWish(wish);
        logger.finest(colony.getName() + " completes " + reason
            + " wish: " + wish);
        wish.dispose();
        return true;
    }

    /**
     * Tries to complete any wishes for some goods that have just arrived.
     *
     * @param goods Some <code>Goods</code> that are arriving in this colony.
     * @return True if a wish was successfully completed.
     */
    public boolean completeWish(Goods goods) {
        boolean ret = false;
        int i = 0;
        while (i < wishes.size()) {
            if (wishes.get(i) instanceof GoodsWish) {
                GoodsWish gw = (GoodsWish)wishes.get(i);
                if (gw.satisfiedBy(goods)
                    && completeWish(gw, "satisfied(" + goods + ")")) {
                    ret = true;
                    continue;
                }
            }
            i++;
        }
        return ret;
    }

    /**
     * Tries to complete any wishes for a unit that has just arrived.
     *
     * @param unit A <code>Unit</code> that is arriving in this colony.
     * @return True if a wish was successfully completed.
     */
    public boolean completeWish(Unit unit) {
        boolean ret = false;
        int i = 0;
        while (i < wishes.size()) {
            if (wishes.get(i) instanceof WorkerWish) {
                WorkerWish ww = (WorkerWish)wishes.get(i);
                if (ww.satisfiedBy(unit)
                    && completeWish(ww, "satisfied (" + unit.getId() + ")")) {
                    ret = true;
                    continue;
                }
            }
            i++;
        }
        return ret;
    }

    /**
     * Gets the wishes this colony has.
     *
     * @return A copy of the wishes list.
     */
    public List<Wish> getWishes() {
        return new ArrayList<Wish>(wishes);
    }

    /**
     * Gets the goods wishes this colony has.
     *
     * @return A copy of the wishes list with non-goods wishes removed.
     */
    public List<GoodsWish> getGoodsWishes() {
        List<GoodsWish> result = new ArrayList<GoodsWish>();
        for (Wish wish : wishes) {
            if (wish instanceof GoodsWish) {
                result.add((GoodsWish) wish);
            }
        }
        return result;
    }

    /**
     * Gets the worker wishes this colony has.
     *
     * @return A copy of the wishes list with non-worker wishes removed.
     */
    public List<WorkerWish> getWorkerWishes() {
        List<WorkerWish> result = new ArrayList<WorkerWish>();
        for (Wish wish : wishes) {
            if (wish instanceof WorkerWish) {
                result.add((WorkerWish) wish);
            }
        }
        return result;
    }

    /**
     * Requires a goods wish for a specified goods type be present at this
     * AI colony.  If one is already present, the amount specified here
     * takes precedence as it is more likely to be up to date.  The value
     * is treated as a minimum requirement.
     *
     * @param type The <code>GoodsType</code> to wish for.
     * @param amount The amount of goods wished for.
     * @param value The urgency of the wish.
     */
    public void requireGoodsWish(GoodsType type, int amount, int value) {
        GoodsWish gw = null;
        for (Wish w : wishes) {
            if (w instanceof GoodsWish
                && ((GoodsWish)w).getGoodsType() == type) {
                gw = (GoodsWish)w;
                break;
            }
        }
        if (gw != null) {
            gw.setGoodsAmount(amount);
            gw.setValue(value);
        } else {
            gw = new GoodsWish(getAIMain(), colony, value, amount, type);
            wishes.add(gw);
            logger.finest(colony.getName() + " makes new goods wish: " + gw);
        }
    }

    /**
     * Requires a worker wish for a unit type to be provided to this AIColony.
     * If a suitable wish is already present, the expert and value parameters
     * take precedence as they are more likely to be up to date.
     *
     * @param type The <code>UnitType</code> to wish for.
     * @param expertNeeded Is an expert unit required?
     * @param value The urgency of the wish.
     */
    public void requireWorkerWish(UnitType type, boolean expertNeeded,
                                  int value) {
        WorkerWish ww = null;
        for (Wish w : wishes) {
            if (w instanceof WorkerWish
                && ((WorkerWish)w).getUnitType() == type) {
                ww = (WorkerWish)w;
                break;
            }
        }
        if (ww != null) {
            ww.update(type, expertNeeded, value);
        } else {
            ww = new WorkerWish(getAIMain(), colony, value, type, expertNeeded);
            wishes.add(ww);
            logger.finest(colony.getName() + " makes new worker wish: "
                + ww);
        }
    }

    /**
     * Updates the wishes for the <code>Colony</code>.
     */
    private void updateWishes() {
        updateWorkerWishes();
        updateGoodsWishes();
        Collections.sort(wishes);
    }

    /**
     * Updates the worker wishes.
     */
    private void updateWorkerWishes() {
        final Specification spec = getSpecification();
        final int baseValue = 25;
        final int priorityMax = 50;
        final int priorityDecay = 5;
        final int multipleBonus = 5;
        final int multipleMax = 5;

        // For every non-expert, request expert replacement.
        // Prioritize by lowest net production among the goods that are
        // being produced by units (note that we have to traverse the work
        // locations/unit-lists, rather than just check for non-zero
        // production because it could be in balance).
        // Add some weight when multiple cases of the same expert are
        // needed, rather than generating heaps of wishes.
        List<GoodsType> producing = new ArrayList<GoodsType>();
        for (WorkLocation wl : colony.getAvailableWorkLocations()) {
            for (Unit u : wl.getUnitList()) {
                GoodsType work = u.getWorkType();
                if (work != null) {
                    work = work.getStoredAs();
                    if (!producing.contains(work)) producing.add(work);
                }
            }
        }
        Collections.sort(producing, new Comparator<GoodsType>() {
                public int compare(GoodsType g1, GoodsType g2) {
                    return colony.getAdjustedNetProductionOf(g1)
                        - colony.getAdjustedNetProductionOf(g2);
                }
            });
        TypeCountMap<UnitType> experts = new TypeCountMap<UnitType>();
        for (Unit unit : colony.getUnitList()) {
            GoodsType goods = unit.getWorkType();
            UnitType expert = (goods == null
                || goods == unit.getType().getExpertProduction()) ? null
                : spec.getExpertForProducing(goods);
            if (expert != null) {
                experts.incrementCount(expert, 1);
            }
        }
        for (UnitType expert : experts.keySet()) {
            GoodsType goods = expert.getExpertProduction();
            int value = baseValue
                + Math.max(0, priorityMax
                    - priorityDecay * producing.indexOf(goods))
                + (Math.min(multipleMax, experts.getCount(expert) - 1)
                    * multipleBonus);
            requireWorkerWish(expert, true, value);
        }

        // Request population increase if no worker wishes and the bonus
        // can take it.
        if (experts.isEmpty()
            && colony.governmentChange(colony.getUnitCount() + 1) >= 0) {
            boolean needFood = colony.getFoodProduction()
                <= colony.getFoodConsumption()
                + colony.getOwner().getMaximumFoodConsumption();
            // Choose expert for best work location plan
            UnitType expert = spec.getDefaultUnitType();
            for (WorkLocationPlan plan : (needFood) ? colonyPlan.getFoodPlans()
                     : colonyPlan.getWorkPlans()) {
                WorkLocation location = plan.getWorkLocation();
                if (!location.canBeWorked()) continue;
                expert = spec.getExpertForProducing(plan.getGoodsType());
                break;
            }
            requireWorkerWish(expert, false, 50);
        }

        // TODO: check for students
        // TODO: add missionaries

        // Improve defence.
        if (isBadlyDefended()) {
            UnitType bestDefender = colony.getBestDefenderType();
            if (bestDefender != null) {
                requireWorkerWish(bestDefender, true, 100);
            }
        }
    }

    /**
     * Updates the goods wishes.
     */
    private void updateGoodsWishes() {
        final Specification spec = getSpecification();
        int goodsWishValue = 50;

        // Request goods.
        // TODO: improve heuristics
        TypeCountMap<GoodsType> required = new TypeCountMap<GoodsType>();

        // Add building materials.
        if (colony.getCurrentlyBuilding() != null) {
            for (AbstractGoods ag : colony.getCurrentlyBuilding()
                     .getRequiredGoods()) {
                if (colony.getAdjustedNetProductionOf(ag.getType()) <= 0) {
                    required.incrementCount(ag.getType(), ag.getAmount());
                }
            }
        }

        // Add materials required to improve tiles.
        for (TileImprovementPlan plan : tileImprovementPlans) {
            for (AbstractGoods ag : plan.getType()
                     .getExpendedEquipmentType().getRequiredGoods()) {
                required.incrementCount(ag.getType(), ag.getAmount());
            }
        }

        // Add raw materials for buildings.
        for (WorkLocation workLocation : colony.getCurrentWorkLocations()) {
            if (workLocation instanceof Building) {
                Building building = (Building) workLocation;
                List<AbstractGoods> inputs = building.getInputs();
                if (!(inputs == null || inputs.isEmpty())) {
                    ProductionInfo info = colony.getProductionInfo(building);
                    if (!info.hasMaximumProduction()) {
                        for (AbstractGoods goods : inputs) {
                            // TODO: find better heuristics
                            required.incrementCount(goods.getType(), 100);
                        }
                    }
                }
            }
        }

        // Add breedable goods
        for (GoodsType g : spec.getGoodsTypeList()) {
            if (g.isBreedable()
                && colony.getGoodsCount(g) < g.getBreedingNumber()) {
                required.incrementCount(g, g.getBreedingNumber());
            }
        }

        // Add materials required to build military equipment,
        // but make sure there is a unit present that can use it.
        if (isBadlyDefended()) {
            for (EquipmentType type : spec.getEquipmentTypeList()) {
                if (!type.isMilitaryEquipment()) continue;
                for (Unit unit : colony.getTile().getUnitList()) {
                    if (!unit.canBeEquippedWith(type)) continue;
                    for (AbstractGoods ag : type.getRequiredGoods()) {
                        required.incrementCount(ag.getType(), ag.getAmount());
                    }
                    break;
                }
            }
        }

        // Drop wishes that are no longer needed.
        int i = 0;
        while (i < wishes.size()) {
            if (wishes.get(i) instanceof GoodsWish) {
                GoodsWish g = (GoodsWish)wishes.get(i);
                GoodsType t = g.getGoodsType();
                if (required.getCount(t) < colony.getGoodsCount(t)) {
                    completeWish(g, "redundant");
                    continue;
                }
            }
            i++;
        }

        // Require wishes for what is missing.
        for (GoodsType type : required.keySet()) {
            GoodsType requiredType = type;
            while (requiredType != null) {
                if (requiredType.isStorable()) break;
                requiredType = requiredType.getInputType();
            }
            if (requiredType == null) continue;
            int amount = Math.min(colony.getWarehouseCapacity(),
                (required.getCount(type)
                    - colony.getGoodsCount(requiredType)));
            if (amount > 0) {
                int value = goodsWishValue;
                if (colonyCouldProduce(requiredType)) value /= 10;
                requireGoodsWish(requiredType, amount, value);
            }
        }

    }

    /**
     * Can a colony produce certain goods?
     *
     * @param goodsType The <code>GoodsType</code> to check production of.
     * @return True if the colony can produce such goods.
     */
    private boolean colonyCouldProduce(GoodsType goodsType) {
        if (goodsType.isBreedable()) {
            return colony.getGoodsCount(goodsType)
                >= goodsType.getBreedingNumber();
        }
        if (goodsType.isFarmed()) {
            for (ColonyTile colonyTile : colony.getColonyTiles()) {
                if (colonyTile.getWorkTile().potential(goodsType, null) > 0) {
                    return true;
                }
            }
        } else {
            if (!colony.getBuildingsForProducing(goodsType).isEmpty()) {
                return (goodsType.getInputType() == null) ? true
                    : colonyCouldProduce(goodsType.getInputType());
            }
        }
        return false;
    }


    /**
     * Gets the tile improvements planned for this colony.
     *
     * @return A copy of the tile improvement plan list.
     */
    public List<TileImprovementPlan> getTileImprovementPlans() {
        return new ArrayList<TileImprovementPlan>(tileImprovementPlans);
    }

    /**
     * Removes a <code>TileImprovementPlan</code> from the list
     *
     * @return True if it was successfully deleted, false otherwise
     */
    public boolean removeTileImprovementPlan(TileImprovementPlan plan){
        return tileImprovementPlans.remove(plan);
    }

    /**
     * Gets the first plan for a specified tile from a list of tile
     * improvement plans.
     *
     * @param tile The <code>Tile</code> to look for.
     * @param plans A list of <code>TileImprovementPlan</code>s to search.
     * @return A matching plan, or null if not found.
     */
    private TileImprovementPlan getPlanFor(Tile tile,
                                           List<TileImprovementPlan> plans) {
        for (TileImprovementPlan tip : plans) {
            if (tip.getTarget() == tile) return tip;
        }
        return null;
    }

    /**
     * Creates a list of the <code>Tile</code>-improvements which will
     * increase the production by this <code>Colony</code>.
     *
     * @see TileImprovementPlan
     */
    public void createTileImprovementPlans() {
        List<TileImprovementPlan> newPlans
            = new ArrayList<TileImprovementPlan>();
        for (WorkLocation wl : colony.getAvailableWorkLocations()) {
            if (!(wl instanceof ColonyTile)) continue;
            ColonyTile colonyTile = (ColonyTile) wl;
            Tile workTile = colonyTile.getWorkTile();
            if (workTile.getOwningSettlement() != colony
                || getPlanFor(workTile, newPlans) != null) continue;

            // Require food for the center tile, but otherwise insist
            // the tile is being used, and try to improve the
            // production that is underway.
            GoodsType goodsType = null;
            if (colonyTile.isColonyCenterTile()) {
                for (AbstractGoods ag : colonyTile.getProduction()) {
                    if (ag.getType().isFoodType()) {
                        goodsType = ag.getType();
                        break;
                    }
                }
            } else {
                if (colonyTile.isEmpty()) continue;
                goodsType = colonyTile.getUnitList().get(0).getWorkType();
            }
            if (goodsType == null) continue;

            TileImprovementPlan plan = getPlanFor(workTile,
                                                  tileImprovementPlans);
            if (plan == null) {
                TileImprovementType type = TileImprovementPlan
                    .getBestTileImprovementType(workTile, goodsType);
                if (type != null) {
                    plan = new TileImprovementPlan(getAIMain(), workTile,
                        type, type.getImprovementValue(workTile, goodsType));
                }
            } else {
                if (!plan.update(goodsType)) plan = null;
            }
            if (plan != null) {
                // Defend against clearing the last forested tile, but
                // otherwise add the plan.
                TileType change = plan.getType().getChange(workTile.getType());
                if (change != null && !change.isForested()) {
                    int forest = 0;
                    for (WorkLocation f : colony.getAvailableWorkLocations()) {
                        if (f instanceof ColonyTile
                            && ((ColonyTile)f).getWorkTile().isForested())
                            forest++;
                    }
                    if (forest <= FOREST_MINIMUM) continue;
                }
                newPlans.add(plan);
                logger.info(colony.getName()
                    + " new tile improvement plan: " + plan);
            }
        }
        tileImprovementPlans.clear();
        tileImprovementPlans.addAll(newPlans);
        Collections.sort(tileImprovementPlans);
    }

    /**
     * Get the list of buildables in the colony plan.
     *
     * Public for the test suite.
     *
     * @return A list of planned <code>BuildableType</code>.
     */
    public List<BuildableType> getPlannedBuildableTypes() {
        if (colonyPlan == null) return Collections.emptyList();
        return colonyPlan.getBuildableTypes();
    }

    /**
     * Summarize the colony plan as a string.
     *
     * @return A summary of the colony plan.
     */
    public String planToString() {
        if (colonyPlan == null) return "No plan.";

        StringBuilder sb = new StringBuilder();
        sb.append(colonyPlan.toString()).append("\n\nTILE IMPROVEMENTS:\n");
        for (TileImprovementPlan tip : getTileImprovementPlans()) {
            sb.append(tip.toString()).append("\n");
        }
        sb.append("\n\nWISHES:\n");
        for (Wish w : getWishes()) sb.append(w.toString()).append("\n");
        sb.append("\n\nEXPORT GOODS:\n");
        for (AIGoods aig : getAIGoods()) sb.append(aig.toString()).append("\n");
        return sb.toString();
    }

    /**
     * Handle REARRANGE_WORKERS property change events.
     *
     * @param event The <code>PropertyChangeEvent</code>.
     */
    public void propertyChange(PropertyChangeEvent event) {
        logger.finest("Property change REARRANGE_WORKERS fired.");
        requestRearrange();
    }

    /**
     * Sets the rearrangeTurn variable such that rearrangeWorkers will
     * run fully next time it is invoked.
     */
    public void requestRearrange() {
        rearrangeTurn = new Turn(0);
    }


    // Override AIObject

    /**
     * Disposes this <code>AIColony</code>.
     */
    @Override
    public void dispose() {
        List<AIObject> disposeList = new ArrayList<AIObject>();
        for (AIGoods ag : aiGoods) {
            if (ag.getGoods().getLocation() == colony) disposeList.add(ag);
        }
        for (Wish w : wishes) {
            disposeList.add(w);
        }
        for (TileImprovementPlan ti : tileImprovementPlans) {
            disposeList.add(ti);
        }
        for (AIObject o : disposeList) o.dispose();
        colonyPlan = null;
        // Do not clear this.colony, the identifier is still required.
        super.dispose();
    }

    /**
     * Checks the integrity of a this AIColony
     *
     * @param fix Fix problems if possible.
     * @return Negative if there are problems remaining, zero if
     *     problems were fixed, positive if no problems found at all.
     */
    @Override
    public int checkIntegrity(boolean fix) {
        int result = super.checkIntegrity(fix);
        if (colony == null || colony.isDisposed()) result = -1;
        return result;
    }


    // Serialization

    private static final String AI_GOODS_LIST_TAG
        = AIGoods.getXMLElementTagName() + LIST_ELEMENT;
    private static final String GOODS_WISH_LIST_TAG
        = GoodsWish.getXMLElementTagName() + LIST_ELEMENT;
    private static final String TILE_IMPROVEMENT_PLAN_LIST_TAG
        = TileImprovementPlan.getXMLElementTagName() + LIST_ELEMENT;
    private static final String WORKER_WISH_LIST_TAG
        = WorkerWish.getXMLElementTagName() + LIST_ELEMENT;
    // @compat 0.10.3
    private static final String OLD_GOODS_WISH_TAG
        = GoodsWish.getXMLElementTagName() + "Wish" + LIST_ELEMENT;
    private static final String OLD_TILE_IMPROVEMENT_PLAN_TAG
        = "tileimprovementplan" + LIST_ELEMENT;
    private static final String OLD_WORKER_WISH_TAG
        = WorkerWish.getXMLElementTagName() + "Wish" + LIST_ELEMENT;
    // end @compat


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (AIGoods ag : aiGoods) {
            if (ag.checkIntegrity(true) < 0) continue;
            xw.writeStartElement(AI_GOODS_LIST_TAG);

            xw.writeAttribute(ID_ATTRIBUTE_TAG, ag);

            xw.writeEndElement();
        }

        for (TileImprovementPlan tip : tileImprovementPlans) {
            if (tip.checkIntegrity(true) < 0) continue;

            xw.writeStartElement(TILE_IMPROVEMENT_PLAN_LIST_TAG);

            xw.writeAttribute(ID_ATTRIBUTE_TAG, tip);

            xw.writeEndElement();
        }

        for (Wish w : wishes) {
            String tag = (w instanceof GoodsWish) ? GOODS_WISH_LIST_TAG
                : (w instanceof WorkerWish) ? WORKER_WISH_LIST_TAG
                : null;
            if (w.checkIntegrity(true) < 0 || !w.shouldBeStored()
                || tag == null) continue;

            xw.writeStartElement(tag);

            xw.writeAttribute(ID_ATTRIBUTE_TAG, w);

            xw.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final AIMain aiMain = getAIMain();

        colony = xr.getAttribute(aiMain.getGame(), ID_ATTRIBUTE_TAG,
                                 Colony.class, (Colony)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        aiGoods.clear();
        tileImprovementPlans.clear();
        wishes.clear();

        super.readChildren(xr);

        if (getColony() != null) uninitialized = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final AIMain aiMain = getAIMain();
        final String tag = xr.getLocalName();

        if (AI_GOODS_LIST_TAG.equals(tag)) {
            aiGoods.add(xr.makeAIObject(aiMain, ID_ATTRIBUTE_TAG,
                                        AIGoods.class, (AIGoods)null, true));
            xr.closeTag(AI_GOODS_LIST_TAG);

        } else if (GOODS_WISH_LIST_TAG.equals(tag)
            // @compat 0.10.3
            || OLD_GOODS_WISH_TAG.equals(tag)
            // end @compat
                   ) {
            wishes.add(xr.makeAIObject(aiMain, ID_ATTRIBUTE_TAG,
                                       GoodsWish.class, (GoodsWish)null, true));
            xr.closeTag(tag);// FIXME: tag -> GOODS_WISH_LIST_TAG

        } else if (TILE_IMPROVEMENT_PLAN_LIST_TAG.equals(tag)
            // @compat 0.10.3
            || OLD_TILE_IMPROVEMENT_PLAN_TAG.equals(tag)
            // end @compat
                   ) {
            tileImprovementPlans.add(xr.makeAIObject(aiMain, ID_ATTRIBUTE_TAG,
                    TileImprovementPlan.class, (TileImprovementPlan)null, true));
            xr.closeTag(tag);// FIXME: tag -> TILE_IMPROVEMENT_PLAN_LIST_TAG

        } else if (WORKER_WISH_LIST_TAG.equals(tag)
            // @compat 0.10.3
            || OLD_WORKER_WISH_TAG.equals(tag)
            // end @compat
                   ) {
            wishes.add(xr.makeAIObject(aiMain, ID_ATTRIBUTE_TAG,
                                       WorkerWish.class, (WorkerWish)null, true));
            xr.closeTag(tag);// FIXME: tag -> WORKER_WISH_LIST_TAG

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "aiColony"
     */
    public static String getXMLElementTagName() {
        return "aiColony";
    }
}
