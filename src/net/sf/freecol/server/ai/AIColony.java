/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
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

import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;


/**
 * Objects of this class contains AI-information for a single {@link Colony}.
 */
public final class AIColony extends AIObject implements PropertyChangeListener {

    private static final Logger logger = Logger.getLogger(AIColony.class.getName());

    public static final String TAG = "aiColony";

    private static final String LIST_ELEMENT = "ListElement";
    
    /**
     * Do not perform tile improvements that would leave less than
     * this amount of forested work locations available to the colony.
     */
    private static final int FOREST_MINIMUM = 1;

    /** Do not bother trying to ship out less than this amount of goods. */
    private static final int EXPORT_MINIMUM = 10;

    /** Comparator to choose the best pioneers. */
    private static final Comparator<Unit> pioneerComparator
        = Comparator.comparingInt(Unit::getPioneerScore).reversed();

    /** Comparator to choose the best scouts. */
    private static final Comparator<Unit> scoutComparator
        = Comparator.comparingInt(Unit::getScoutScore).reversed();

    /**
     * Goods that should be completely exported and only exported to
     * prevent the warehouse filling.
     */
    private static final Set<GoodsType> fullExport = new HashSet<>();
    private static final Set<GoodsType> partExport = new HashSet<>();

    /** The colony this AIColony is managing. */
    private Colony colony;

    /** The current plan for the colony.  Does not need to be serialized. */
    private ColonyPlan colonyPlan;

    /** Goods to export from the colony. */
    private List<AIGoods> exportGoods;

    /** Useful things for the colony. */
    private List<Wish> wishes;

    /** Plans to improve neighbouring tiles. */
    private List<TileImprovementPlan> tileImprovementPlans;

    /** When should the workers in this Colony be rearranged? */
    private Turn rearrangeTurn = new Turn(0);


    /**
     * Creates a new uninitialized {@code AIColony}.
     *
     * @param aiMain The main AI-object.
     * @param id The object identifier.
     */
    public AIColony(AIMain aiMain, String id) {
        super(aiMain, id);

        baseInitialize();
        this.initialized = false;
    }

    /**
     * Creates a new {@code AIColony}.
     *
     * @param aiMain The main AI-object.
     * @param colony The colony to make an {@link AIObject} for.
     */
    public AIColony(AIMain aiMain, Colony colony) {
        this(aiMain, colony.getId());

        this.colony = colony;
        colony.addPropertyChangeListener(Colony.REARRANGE_COLONY, this);

        setInitialized();
    }

    /**
     * Creates a new {@code AIColony} from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public AIColony(AIMain aiMain, FreeColXMLReader xr)
        throws XMLStreamException {
        super(aiMain, xr);

        setInitialized();
    }


    /**
     * Initialize the basic fields.
     */
    private void baseInitialize() {
        if (this.exportGoods != null) return;
        this.colonyPlan = null;
        this.exportGoods = new ArrayList<>();
        this.wishes = new ArrayList<>();
        this.tileImprovementPlans = new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     */
    public void setInitialized() {
        this.initialized = getColony() != null;
    }

    /**
     * Gets the {@code Colony} this {@code AIColony} controls.
     *
     * @return The {@code Colony}.
     */
    public final Colony getColony() {
        return colony;
    }

    protected AIUnit getAIUnit(Unit unit) {
        return getAIMain().getAIUnit(unit);
    }

    protected AIPlayer getAIOwner() {
        return getAIMain().getAIPlayer(colony.getOwner());
    }

    /**
     * Is this AI colony badly defended?
     *
     * @return True if this colony needs more defenders.
     */
    public boolean isBadlyDefended() {
        return colony.isBadlyDefended();
    }

    /**
     * Update any relatively static properties of the colony:
     *   - export state
     *   - disposition of export goods in this colony
     *   - tile improvements (might ignore freshly grabbed tiles)
     *   - wishes
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    public void update(LogBuilder lb) {
        lb.add("\n  ", colony.getName());
        resetExports();
        updateExportGoods(lb);
        updateTileImprovementPlans(lb);
        updateWishes(lb);
    }

    /**
     * Rearranges the workers within this colony using the {@link ColonyPlan}.
     *
     * FIXME: Detect military threats and boost defence.
     *
     * @param lb A {@code LogBuilder} to log to.
     * @return A set of {@code AIUnit}s that changed their work state.
     */
    public Set<AIUnit> rearrangeColony(LogBuilder lb) {
        final AIMain aiMain = getAIMain();
        Set<AIUnit> result = new HashSet<>();

        // First check if it is collapsing.
        if (colony.getUnitCount() <= 0) {
            if (!avertAutoDestruction()) return result;
        }

        // Skip this colony if it does not yet need rearranging.
        final int turn = getGame().getTurn().getNumber();
        if (rearrangeTurn.getNumber() > turn) {
            if (colony.getCurrentlyBuilding() == null
                && colonyPlan != null
                && colonyPlan.getBestBuildableType() != null) {
                logger.warning(colony.getName() + " could be building but"
                    + " is asleep until turn: " + rearrangeTurn.getNumber()
                    + "( > " + turn + ")");
            } else {
                return result;
            }
        }

        final Tile tile = colony.getTile();
        final Player player = colony.getOwner();
        final Specification spec = getSpecification();
        lb.add("\n  ", colony.getName());

        // For now, cap the rearrangement horizon, because confidence
        // that we are triggering on all relevant changes is low.
        int nextRearrange = 15;

        // See if there are neighbouring LCRs to explore, or tiles
        // to steal, or just unclaimed tiles (a neighbouring settlement
        // might have disappeared or relinquished a tile).
        // This needs to be done early so that new tiles can be
        // included in any new colony plan.
        exploreLCRs();
        stealTiles(lb);
        for (Tile t : transform(tile.getSurroundingTiles(1,1),
                t2 -> !player.owns(t2) && player.canClaimForSettlement(t2))) {
            AIMessage.askClaimLand(t, this, 0);
            if (player.owns(t)) lb.add(", claimed tile ", t);
        }

        // Update the colony plan.
        if (colonyPlan == null) colonyPlan = new ColonyPlan(aiMain, colony);
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
            List<BuildableType> queue = new ArrayList<>(1);
            if (build != null) queue.add(build);
            AIMessage.askSetBuildQueue(this, queue);
            build = colony.getCurrentlyBuilding();
        }
        colonyPlan.refine(build, lb);

        // Collect all potential workers from the colony and from the tile,
        // being careful not to disturb existing non-colony missions.
        // Note the special case of a unit aiming to build a colony on this
        // tile, which happens regularly with the initial AI colony.
        // Remember where the units came from.
        List<Unit> workers = colony.getUnitList();
        List<UnitWas> was = transform(workers, alwaysTrue(),
                                      u -> new UnitWas(u));
        final Predicate<Unit> workerPred = u -> {
            AIUnit validAIU;
            return u.isPerson() && !u.hasAbility(Ability.REF_UNIT)
                && (validAIU = getAIUnit(u)) != null
                && validAIU.isAvailableForWork(colony);
        };                
        for (Unit u : transform(tile.getUnits(), workerPred)) {
            workers.add(u);
            was.add(new UnitWas(u));
        }
        // Assign the workers according to the colony plan.
        // ATM we just accept this assignment unless it failed, in
        // which case restore original state.
        AIPlayer aiPlayer = getAIOwner();
        LogBuilder aw = new LogBuilder(256);
        boolean preferScouts = aiPlayer.scoutsNeeded() > 0;
        Colony scratch = colonyPlan.assignWorkers(new ArrayList<>(workers),
                                                  preferScouts, aw);
        if (scratch == null) {
            lb.add(", failed to assign workers.");
            rearrangeTurn = new Turn(turn + 1);
            return result;
        }
        lb.add(", assigned ", workers.size(), " workers");

        // Apply the arrangement, and give suitable missions to all units.
        AIMessage.askRearrangeColony(this, workers, scratch);

        // Emergency recovery if something broke and the colony is empty.
        if (colony.getUnitCount() <= 0) {
            lb.add(", autodestruct detected");
            StringBuilder sb = new StringBuilder(64);
            sb.append("Autodestruct at ").append(colony.getName())
                .append(" in ").append(turn).append(':');
            for (UnitWas uw : was) sb.append('\n').append(uw);
            logger.warning(sb.toString());
            if (!avertAutoDestruction()) return result;
        }

        // Argh.  We may have chosen to build something we can no
        // longer build due to some limitation.  Try to find a
        // replacement, but do not re-refine as that process is
        // sufficiently complex that we can not be confident that this
        // will not loop indefinitely.  The compromise is to just
        // rearrange next turn until we get out of this state.
        if (build != null && !colony.canBuild(build)) {
            BuildableType newBuild = colonyPlan.getBestBuildableType();
            lb.add(", reneged building ", build.getSuffix(),
                   " (", colony.getNoBuildReason(build, null), ")");
            List<BuildableType> queue = new ArrayList<>();
            if (newBuild != null) queue.add(newBuild);
            AIMessage.askSetBuildQueue(this, queue);
            nextRearrange = 1;
        }

        // Now that all production has been stabilized, plan to
        // rearrange when the warehouse hits a limit.
        if (colony.getNetProductionOf(spec.getPrimaryFoodType()) < 0) {
            GoodsType food = spec.getPrimaryFoodType();
            int net = colony.getNetProductionOf(food);
            int when = colony.getGoodsCount(food) / -net;
            nextRearrange = Math.max(0, Math.min(nextRearrange, when-1));
        }
        int warehouse = colony.getWarehouseCapacity();
        for (GoodsType g : transform(spec.getStorableGoodsTypeList(),
                                     gt -> !gt.isFoodType())) {
            int have = colony.getGoodsCount(g);
            int net = colony.getAdjustedNetProductionOf(g);
            if (net >= 0 && (have >= warehouse || g.limitIgnored())) continue;
            int when = (net < 0) ? (have / -net - 1)
                : (net > 0) ? ((warehouse - have) / net - 1)
                : Integer.MAX_VALUE;
            nextRearrange = Math.max(1, Math.min(nextRearrange, when));
        }

        // Return any units with the wrong mission
        for (Unit u : colony.getUnitList()) {
            final AIUnit aiu = getAIUnit(u);
            if (!aiu.tryWorkInsideColonyMission(this, lb)) result.add(aiu); 
        }

        // Allocate pioneers if possible.
        int tipSize = tileImprovementPlans.size();
        if (tipSize > 0) {
            List<Unit> pioneers
                = transform(tile.getUnits(), u -> u.getPioneerScore() >= 0,
                            Function.<Unit>identity(), pioneerComparator);
            for (Unit u : pioneers) {
                final AIUnit aiu = getAIUnit(u);
                if (aiu.tryPioneeringMission(lb)) {
                    if (--tipSize <= 0) break;
                }
            }
        }

        for (Unit u : tile.getUnitList()) {
            final AIUnit aiu = getAIUnit(u);
            if (!aiu.trySomeUsefulMission(colony, lb)) result.add(aiu);
        }

        // Log the changes.
        build = colony.getCurrentlyBuilding();
        String buildStr = (build != null) ? build.toString()
            : ((build = colonyPlan.getBestBuildableType()) != null)
            ? "unexpected-null(" + build + ")"
            : "expected-null";
        lb.add(", building ", buildStr, ", population ", colony.getUnitCount(),
            ", rearrange ", nextRearrange, ".\n");
        lb.add(aw.toString());
        lb.shrink("\n");
        for (UnitWas uw : was) lb.add("\n  ", uw);

        // Set the next rearrangement turn.
        rearrangeTurn = new Turn(turn + nextRearrange);

        return result;
    }

    /**
     * Reset the export settings.
     * This is always needed even when there is no customs house, because
     * updateExportGoods needs to know what to export by transport.
     *
     * FIXME: consider market prices?
     */
    private void resetExports() {
        final Specification spec = getSpecification();
        final Player player = colony.getOwner();

        fullExport.clear();
        partExport.clear();
        // Initialize the exportable sets.
        // Luxury goods, non-raw materials (silver), and raw materials
        // we do not produce (might have been gifted) should always be
        // fully exported.  Other raw and manufactured goods should be
        // exported only to the extent of not filling the warehouse.
        for (GoodsType g : spec.getStorableGoodsTypeList()) {
            if (!g.isFoodType()
                && !g.isBuildingMaterial()
                && !g.getMilitary()
                && !g.isTradeGoods()) {
                if (g.isRawMaterial()) {
                    partExport.add(g);
                } else {
                    fullExport.add(g);
                }
            }
        }

        // Goods required to equip for any available role should only be
        // partially exported.
        final UnitType dUT = spec.getDefaultUnitType(player);
        for (AbstractGoods ag : transform(flatten(spec.getRoles(),
                                                  r -> r.isAvailableTo(player, dUT),
                                                  Role::getRequiredGoods),
                                          g -> fullExport.contains(g.getType()))) {
            fullExport.remove(ag.getType());
            partExport.add(ag.getType());
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
        final Predicate<Unit> explorerPred = u -> u.isPerson()
            && (u.getType().getSkill() <= 0
                || u.hasAbility(Ability.EXPERT_SCOUT));
        List<Unit> scouts = transform(tile.getUnits(), explorerPred,
                                      Function.<Unit>identity(),
                                      scoutComparator);
        for (Tile t : transform(tile.getSurroundingTiles(1,1),
                                Tile::hasLostCityRumour)) {
            Direction direction = tile.getDirection(t);
            for (;;) {
                if (scouts.isEmpty()) return;
                Unit u = scouts.remove(0);
                if (!u.getMoveType(t).isProgress()) continue;
                if (getAIUnit(u).move(direction)
                    && !t.hasLostCityRumour()) {
                    u.setDestination(tile);
                    break;
                }
            }
        }
    }

    /**
     * Steals neighbouring tiles but only if the colony has some
     * defence.  Grab everything if at war with the owner, otherwise
     * just take the tile that best helps with the currently required
     * raw building materials, with a lesser interest in food.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    private void stealTiles(LogBuilder lb) {
        final Specification spec = getSpecification();
        final Tile tile = colony.getTile();
        final Player player = colony.getOwner();
        boolean hasDefender = any(tile.getUnits(),
            u -> u.isDefensiveUnit()
                && getAIUnit(u).hasDefendSettlementMission());
        if (!hasDefender) return;

        // What goods are really needed?
        List<GoodsType> needed
            = transform(spec.getRawBuildingGoodsTypeList(),
                        gt -> colony.getTotalProductionOf(gt) <= 0);

        // If a tile can be stolen, do so if already at war with the
        // owner or if it is the best one available.
        UnitType unitType = spec.getDefaultUnitType(player);
        Tile steal = null;
        double score = 1.0;
        for (Tile t : tile.getSurroundingTiles(1)) {
            Player owner = t.getOwner();
            if (owner == null || owner == player
                || owner.isEuropean()
                || !player.canClaimForSettlement(t)) continue;
            if (owner.atWarWith(player)) {
                if (AIMessage.askClaimLand(t, this, STEAL_LAND)
                    && player.owns(t)) {
                    lb.add(", stole tile ", t,
                          " from hostile ", owner.getName());
                }
            } else {
                // Pick the best tile to steal, considering mainly the
                // building goods needed, but including food at a lower
                // weight.
                double s = sumDouble(needed,
                        gt -> t.getPotentialProduction(gt, unitType))
                    + sumDouble(spec.getFoodGoodsTypeList(),
                        ft -> 0.1 * t.getPotentialProduction(ft, unitType));
                if (s > score) {
                    score = s;
                    steal = t;
                }
            }
        }
        if (steal != null) {
            Player owner = steal.getOwner();
            if (AIMessage.askClaimLand(steal, this, STEAL_LAND)
                && player.owns(steal)) {
                lb.add(", stole tile ", steal, " (score = ", score,
                      ") from ", owner.getName());
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
     *
     * @return True if autodestruction has been averted.
     */
    private boolean avertAutoDestruction() {
        LogBuilder lb = new LogBuilder(64);
        lb.add("Colony ", colony.getName(), " rearrangement leaves no units, ",
            colony.getTile().getUnitCount(), " available:");
        for (Unit u : colony.getTile().getUnitList()) lb.add(" ", u);
        List<GoodsType> libertyGoods = getSpecification()
            .getLibertyGoodsTypeList();
        out: for (Unit u : transform(colony.getTile().getUnits(),
                                     Unit::isPerson)) {
            for (WorkLocation wl : transform(colony.getAvailableWorkLocations(),
                                             w -> w.canAdd(u))) {
                for (GoodsType type : libertyGoods) {
                    if (wl.getPotentialProduction(type, u.getType()) > 0
                        && AIMessage.askWork(getAIUnit(u), wl)
                        && u.getLocation() == wl) {
                        AIMessage.askChangeWorkType(getAIUnit(u), type);
                        lb.add(", averts destruction with ", u);
                        break out;
                    }
                }
            }
        }
        lb.log(logger, Level.WARNING);
        return colony.getUnitCount() > 0;
    }

    /**
     * Stop using a work location.
     *
     * Called from BuildColonyMission to clear a colony tile that is about
     * to have a colony built on it.
     *
     * @param wl The {@code WorkLocation} to stop using.
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
     * @return A copy of the exportGoods list.
     */
    public List<AIGoods> getExportGoods() {
        synchronized (exportGoods) {
            // firePropertyChanges can hit exportGoods
            return new ArrayList<>(exportGoods);
        }
    }

    /**
     * Clear the export goods.
     */
    private void clearExportGoods() {
        synchronized (exportGoods) {
            exportGoods.clear();
        }
    }

    /**
     * Add to the export goods list, and resort.
     *
     * @param aiGoods The {@code AIGoods} to add.
     */
    private void addExportGoods(AIGoods aiGoods) {
        synchronized (exportGoods) {
            exportGoods.add(aiGoods);
        }
    }

    /**
     * Set the export goods list.
     *
     * @param aiGoods The new list of {@code AIGoods}.
     */
    private void setExportGoods(List<AIGoods> aiGoods) {            
        clearExportGoods();
        synchronized (exportGoods) {
            exportGoods.addAll(aiGoods);
        }
        sortExportGoods();
    }

    /**
     * Sort the export goods.
     */
    private void sortExportGoods() {
        if (this.exportGoods == null) return;
        synchronized (exportGoods) {
            exportGoods.sort(ValuedAIObject.descendingValueComparator);
        }
    }

    /**
     * Removes the given {@code AIGoods} from the export goods
     * for this colony.  The {@code AIGoods}-object is not
     * disposed as part of this operation.  Use dropExportGoods
     * instead to remove the object completely (this method would then
     * be called indirectly).
     *
     * @param ag The {@code AIGoods} to be removed.
     * @see AIGoods#dispose()
     */
    public void removeExportGoods(AIGoods ag) {
        synchronized (exportGoods) {
            while (exportGoods.remove(ag)) {} /* Do nothing here */
        }
    }

    /**
     * Drops some goods from the goods list, and cancels any transport.
     *
     * @param ag The {@code AIGoods} to drop.
     */
    private void dropExportGoods(AIGoods ag) {
        AIUnit transport = ag.getTransport();
        if (transport != null)
            transport.removeTransportable(ag);
        removeExportGoods(ag);
        ag.dispose();
    }

    /**
     * Emits a standard message regarding the state of AIGoods.
     *
     * @param ag The {@code AIGoods} to log.
     * @param action The state of the goods.
     * @param lb A {@code LogBuilder} to log to.
     */
    private void goodsLog(AIGoods ag, String action, LogBuilder lb) {
        Goods goods = (ag == null) ? null : ag.getGoods();
        int amount = (goods == null) ? -1 : goods.getAmount();
        String type = (goods == null) ? "(null)"
            : ag.getGoods().getType().getSuffix();
        lb.add(", ", action, " ", ((ag == null) ? "(null)" : ag.getId()),
            " ", ((amount >= GoodsContainer.CARGO_SIZE) ? "full "
                : Integer.toString(amount) + " "), type);
    }

    /**
     * Creates a list of the goods which should be shipped out of this colony.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    private void updateExportGoods(LogBuilder lb) {
        if (colony.hasAbility(Ability.EXPORT)) {
            for (AIGoods aig : getExportGoods()) {
                goodsLog(aig, "customizes", lb);
                dropExportGoods(aig);
            }

        } else { // does not have a customs house
            for (AIGoods aig : getExportGoods()) {
                if (aig == null) {
                    ; // removeExportGoods(aig);
                } else if (!aig.checkIntegrity(false).safe()) {
                    goodsLog(aig, "reaps", lb);
                    dropExportGoods(aig);
                } else if (aig.getGoods().getLocation() != colony) {
                    // On its way, no longer of interest here, but do
                    // not dispose as that will happen when delivered.
                    goodsLog(aig, "sends", lb);
                    removeExportGoods(aig);
                } else if (colony.getAdjustedNetProductionOf(aig.getGoods()
                        .getType()) < 0) {
                    goodsLog(aig, "needs", lb);
                    dropExportGoods(aig);
                }
            }

            // Create a new batch of exported goods.
            final Europe europe = colony.getOwner().getEurope();
            final int capacity = colony.getWarehouseCapacity();
            List<AIGoods> newAIGoods = new ArrayList<>();
            outer: for (GoodsType gt : getSpecification().getGoodsTypeList()) {
                if (colony.getAdjustedNetProductionOf(gt) < 0) continue;
                int count = colony.getGoodsCount(gt);
                int exportAmount = (fullExport.contains(gt))
                    ? count
                    : (partExport.contains(gt))
                    ? count - colony.getExportData(gt).getExportLevel()
                    : -1;
                if (exportAmount <= 0) continue;
                int priority = (exportAmount >= capacity)
                    ? TransportableAIObject.IMPORTANT_DELIVERY
                    : (exportAmount >= GoodsContainer.CARGO_SIZE)
                    ? TransportableAIObject.FULL_DELIVERY
                    : 0;

                // Find all existing AI goods of type gt, update
                // amount of goods to export to that present in the
                // colony, and drop exports of trivial amounts.  If
                // existing goods are found we do not continue with
                // this goods type but add the updated goods to the
                // new goods batch.
                for (AIGoods aig : getExportGoods()) {
                    Goods goods = aig.getGoods();
                    if (goods.getType() == gt) {
                        int amount = goods.getAmount();
                        if (amount <= exportAmount) {
                            goods.setAmount(exportAmount);
                            goodsLog(aig, "updates", lb);
                            newAIGoods.add(aig);
                        } else {
                            if (exportAmount >= EXPORT_MINIMUM) {
                                goods.setAmount(exportAmount);
                                goodsLog(aig, "clamps", lb);
                                newAIGoods.add(aig);
                            } else {
                                goodsLog(aig, "unexports", lb);
                                dropExportGoods(aig);
                            }
                        }
                        continue outer;
                    }
                }
                // Export new goods, to Europe if possible.
                Location destination = (colony.getOwner().canTrade(gt))
                    ? europe : null;
                if (exportAmount >= EXPORT_MINIMUM) {
                    AIGoods newGoods = new AIGoods(getAIMain(), colony, gt,
                                                   exportAmount, destination);
                    newGoods.setTransportPriority(priority);
                    newAIGoods.add(newGoods);
                    goodsLog(newGoods, "makes", lb);
                }
            }
            setExportGoods(newAIGoods);
        }
    }


    /**
     * Clear the wishes.
     */
    private void clearWishes() {
        this.wishes.clear();
    }

    /**
     * Adds a {@code Wish} to the wishes list.
     *
     * @param wish The {@code Wish} to be added.
     */
    public void addWish(Wish wish) {
        wishes.add(wish);
    }

    /**
     * Tries to complete a supplied wish.
     *
     * @param wish The {@code Wish} to complete.
     * @param reason A reason for wish completion.
     * @param lb A {@code LogBuilder} to log to.
     * @return True if this wish was successfully completed.
     */
    public boolean completeWish(Wish wish, String reason, LogBuilder lb) {
        if (!wishes.remove(wish)) {
            lb.add(", ", reason, " not wished for at ", colony.getName());
            return false;
        }
        getAIOwner().completeWish(wish);
        lb.add(", ", reason, " fulfills at ", colony.getName());
        wish.dispose();
        requestRearrange();
        return true;
    }

    /**
     * Tries to complete any wishes for some goods that have just arrived.
     *
     * @param goods Some {@code Goods} that are arriving in this colony.
     * @param lb A {@code LogBuilder} to log to.
     * @return True if a wish was successfully completed.
     */
    public boolean completeWish(Goods goods, LogBuilder lb) {
        boolean ret = false;
        for (Wish wish : transform(wishes, w -> w.satisfiedBy(goods))) {
            ret |= completeWish(wish, goods.toString(), lb);
        }
        return ret;
    }

    /**
     * Tries to complete any wishes for a unit that has just arrived.
     *
     * @param unit A {@code Unit} that is arriving in this colony.
     * @param lb A {@code LogBuilder} to log to.
     * @return True if a wish was successfully completed.
     */
    public boolean completeWish(Unit unit, LogBuilder lb) {
        boolean ret = false;
        for (Wish wish : transform(wishes, w -> w.satisfiedBy(unit))) {
            ret |= completeWish(wish, unit.toShortString(), lb);
        }
        return ret;
    }

    /**
     * Tries to complete any wishes for a transportable that has just arrived.
     *
     * @param t The arriving {@code TransportableAIObject}.
     * @param lb A {@code LogBuilder} to log to.
     * @return True if a wish was successfully completed.
     */
    public boolean completeWish(TransportableAIObject t, LogBuilder lb) {
        if (t instanceof AIGoods) {
            return completeWish((Goods)t.getTransportLocatable(), lb);
        } else if (t instanceof AIUnit) {
            AIUnit aiUnit = (AIUnit)t;
            if (aiUnit.isCompleteWishRealizationMission(colony)) {
                lb.add(", at wish-target");
                completeWish(aiUnit.getUnit(), lb);
                aiUnit.removeMission();
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the wishes this colony has.
     *
     * @return A copy of the wishes list.
     */
    public List<Wish> getWishes() {
        return new ArrayList<>(wishes);
    }

    /**
     * Gets the goods wishes this colony has.
     *
     * @return A copy of the wishes list with non-goods wishes removed.
     */
    public List<GoodsWish> getGoodsWishes() {
        return transform(wishes, w -> w instanceof GoodsWish,
                         w -> (GoodsWish)w);
    }

    /**
     * Gets the worker wishes this colony has.
     *
     * @return A copy of the wishes list with non-worker wishes removed.
     */
    public List<WorkerWish> getWorkerWishes() {
        return transform(wishes, w -> w instanceof WorkerWish,
                         w -> (WorkerWish)w);
    }

    /**
     * Requires a goods wish for a specified goods type be present at this
     * AI colony.  If one is already present, the amount specified here
     * takes precedence as it is more likely to be up to date.  The value
     * is treated as a minimum requirement.
     *
     * @param type The {@code GoodsType} to wish for.
     * @param amount The amount of goods wished for.
     * @param value The urgency of the wish.
     * @param lb A {@code LogBuilder} to log to.
     */
    private void requireGoodsWish(GoodsType type, int amount, int value,
                                  LogBuilder lb) {
        GoodsWish gw = (GoodsWish)find(wishes, w -> w instanceof GoodsWish
            && ((GoodsWish)w).getGoodsType() == type);
        if (gw != null) {
            gw.update(type, amount, gw.getValue() + 1);
            lb.add(", update ", gw);
        } else {
            gw = new GoodsWish(getAIMain(), colony, value, amount, type);
            wishes.add(gw);
            lb.add(", add ", gw);
        }
    }

    /**
     * Requires a worker wish for a unit type to be provided to this AIColony.
     * If a suitable wish is already present, the expert and value parameters
     * take precedence as they are more likely to be up to date.
     *
     * @param type The {@code UnitType} to wish for.
     * @param expertNeeded Is an expert unit required?
     * @param value The urgency of the wish.
     * @param lb A {@code LogBuilder} to log to.
     */
    private void requireWorkerWish(UnitType type, boolean expertNeeded,
                                   int value, LogBuilder lb) {
        WorkerWish ww = (WorkerWish)find(wishes, w -> w instanceof WorkerWish
            && ((WorkerWish)w).getUnitType() == type);
        if (ww != null) {
            ww.update(type, expertNeeded, ww.getValue() + 1);
            lb.add(", update ", ww);
        } else {
            ww = new WorkerWish(getAIMain(), colony, value, type, expertNeeded);
            wishes.add(ww);
            lb.add(", add ", ww);
        }
    }

    /**
     * Updates the wishes for the {@code Colony}.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    private void updateWishes(LogBuilder lb) {
        updateWorkerWishes(lb);
        updateGoodsWishes(lb);
        wishes.sort(ValuedAIObject.descendingValueComparator);
    }

    /**
     * Updates the worker wishes.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    private void updateWorkerWishes(LogBuilder lb) {
        final Specification spec = getSpecification();
        final int baseValue = 25;
        final int priorityMax = 50;
        final int priorityDecay = 5;
        final int multipleBonus = 5;
        final int multipleMax = 5;

        // Build a list of goods types the colony is producing,
        // in order of amount.
        final Comparator<GoodsType> comp = cachingIntComparator(gt ->
            colony.getAdjustedNetProductionOf(gt));
        List<GoodsType> producing
            = sort(transform(flatten(colony.getAvailableWorkLocations(),
                                     WorkLocation::getUnits),
                             isNotNull(Unit::getWorkType),
                             u -> u.getWorkType().getStoredAs(),
                             Collectors.toSet()),
                   comp);
        
        // For every non-expert, request expert replacement.
        // Prioritize by lowest net production among the goods that are
        // being produced by units (note that we have to traverse the work
        // locations/unit-lists, rather than just check for non-zero
        // production because it could be in balance).
        // Add some weight when multiple cases of the same expert are
        // needed, rather than generating heaps of wishes.
        TypeCountMap<UnitType> experts = new TypeCountMap<>();
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
            requireWorkerWish(expert, true, value, lb);
        }

        // Request population increase if no worker wishes and the bonus
        // can take it.
        if (colonyPlan != null
            && experts.isEmpty()
            && colony.governmentChange(colony.getUnitCount() + 1) >= 0) {
            boolean needFood = colony.getFoodProduction()
                <= colony.getFoodConsumption()
                + colony.getOwner().getMaximumFoodConsumption();
            // Choose expert for best work location plan
            final Player owner = colony.getOwner();
            UnitType expert = spec.getDefaultUnitType(owner);
            for (WorkLocationPlan plan : (needFood) ? colonyPlan.getFoodPlans()
                     : colonyPlan.getWorkPlans()) {
                WorkLocation location = plan.getWorkLocation();
                if (!location.canBeWorked()) continue;
                expert = spec.getExpertForProducing(plan.getGoodsType());
                break;
            }
            requireWorkerWish(expert, false, 50, lb);
        }

        // FIXME: check for students
        // FIXME: add missionaries

        // Improve defence.
        if (isBadlyDefended()) {
            UnitType bestDefender = colony.getBestDefenderType();
            if (bestDefender != null) {
                requireWorkerWish(bestDefender, true, 100, lb);
            }
        }
    }

    /**
     * Updates the goods wishes.
     *
     * @param lb A {@code LogBuilder} to log to.
     */
    private void updateGoodsWishes(LogBuilder lb) {
        final Specification spec = getSpecification();
        int goodsWishValue = 50;

        // Request goods.
        // FIXME: improve heuristics
        TypeCountMap<GoodsType> required = new TypeCountMap<>();

        // Add building materials.
        BuildableType build = colony.getCurrentlyBuilding();
        if (build != null) {
            for (AbstractGoods ag : build.getRequiredGoodsList()) {
                if (colony.getAdjustedNetProductionOf(ag.getType()) <= 0) {
                    required.incrementCount(ag.getType(), ag.getAmount());
                }
            }
        }

        // Add materials required to improve tiles.
        for (TileImprovementPlan plan : tileImprovementPlans) {
            Role role = plan.getType().getRequiredRole();
            if (role == null) continue;
            for (AbstractGoods ag : role.getRequiredGoodsList()) {
                required.incrementCount(ag.getType(), ag.getAmount());
            }
        }

        // Add raw materials for buildings.
        forEach(map(flatten(colony.getCurrentWorkLocations(),
                            wl -> !wl.getProductionInfo().atMaximumProduction(),
                            WorkLocation::getInputs),
                    AbstractGoods::getType),
            // FIXME: find better heuristic
            gt -> required.incrementCount(gt, 100));

        // Add breedable goods
        for (GoodsType gt : transform(spec.getGoodsTypeList(),
                g -> (g.isBreedable()
                    && colony.getGoodsCount(g) < g.getBreedingNumber()))) {
            required.incrementCount(gt, gt.getBreedingNumber());
        }

        // Add materials required to build military equipment,
        // but make sure there is a unit present that can use it.
        if (isBadlyDefended()) {
            Role role = first(spec.getMilitaryRoles());
            Player owner = colony.getOwner();
            final Predicate<Unit> rolePred = u ->
                (u.roleIsAvailable(role)
                    && (u.hasDefaultRole()
                        || Role.rolesCompatible(role, u.getRole())));
            if (any(colony.getTile().getUnits(), rolePred)) {
                for (AbstractGoods ag : role.getRequiredGoodsList()) {
                    required.incrementCount(ag.getType(), ag.getAmount());
                }
            }
        }

        // Drop wishes that are no longer needed.
        for (Wish wish : transform(wishes, w -> {
                    if (w instanceof GoodsWish) {
                        GoodsType t = ((GoodsWish)w).getGoodsType();
                        return required.getCount(t) < colony.getGoodsCount(t);
                    } else {
                        return false;
                    }
                })) {
            completeWish(wish, "redundant", lb);
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
                if (colony.canProduce(requiredType)) value /= 10;
                requireGoodsWish(requiredType, amount, value, lb);
            }
        }

    }

    /**
     * Clear the tile improvement plans.
     */
    private void clearTileImprovementPlans() {
        this.tileImprovementPlans.clear();
    }

    /**
     * Add a tile improvement plan.
     *
     * @param plan The new {@code TileImprovementPlan} to add.
     */
    private void addTileImprovementPlan(TileImprovementPlan plan) {
        this.tileImprovementPlans.add(plan);
    }

    /**
     * Gets the tile improvements planned for this colony.
     *
     * @return A copy of the tile improvement plan list.
     */
    public List<TileImprovementPlan> getTileImprovementPlans() {
        return new ArrayList<>(tileImprovementPlans);
    }

    /**
     * Removes a {@code TileImprovementPlan} from the list
     *
     * @param plan The {@code TileImprovementPlan} to remove.
     * @return True if it was successfully deleted, false otherwise
     */
    public boolean removeTileImprovementPlan(TileImprovementPlan plan) {
        return tileImprovementPlans.remove(plan);
    }

    /**
     * Gets the first plan for a specified tile from a list of tile
     * improvement plans.
     *
     * @param tile The {@code Tile} to look for.
     * @param plans A list of {@code TileImprovementPlan}s to search.
     * @return A matching plan, or null if not found.
     */
    private TileImprovementPlan getPlanFor(Tile tile,
                                           List<TileImprovementPlan> plans) {
        return find(plans, tip -> tip.getTarget() == tile);
    }

    /**
     * Creates a list of the {@code Tile}-improvements which will
     * increase the production by this {@code Colony}.
     *
     * Public for the test suite.
     *
     * @see TileImprovementPlan
     * @param lb A {@code LogBuilder} to log to.
     */
    public void updateTileImprovementPlans(LogBuilder lb) {
        List<WorkLocation> wls = transform(colony.getAvailableWorkLocations(),
                                           w -> w instanceof ColonyTile);
        List<TileImprovementPlan> newPlans = new ArrayList<>(wls.size());
        for (WorkLocation wl : wls) {
            Tile workTile = wl.getWorkTile();
            ColonyTile colonyTile = (ColonyTile)wl;
            if (workTile.getOwningSettlement() != colony
                || getPlanFor(workTile, newPlans) != null) continue;

            // Require food for the center tile, but otherwise insist
            // the tile is being used, and try to improve the
            // production that is underway.
            GoodsType goodsType;
            if (colonyTile.isColonyCenterTile()) {
                AbstractGoods food = find(wl.getProduction(),
                                          AbstractGoods::isFoodType);
                goodsType = (food == null) ? null : food.getType();
            } else {
                goodsType = (wl.isEmpty()) ? null : wl.getCurrentWorkType();
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
            if (plan == null) continue;

            // Defend against clearing the last forested tile.
            TileType change = plan.getType().getChange(workTile.getType());
            final Predicate<WorkLocation> forestPred = cwl ->
                cwl instanceof ColonyTile
                    && !((ColonyTile)cwl).isColonyCenterTile()
                    && cwl.getWorkTile().isForested();
            if (change != null
                && !change.isForested()
                && !colonyTile.isColonyCenterTile()
                && count(colony.getAvailableWorkLocations(), forestPred)
                    <= FOREST_MINIMUM) continue;

            newPlans.add(plan); // Otherwise add the plan.
        }
        clearTileImprovementPlans();
        tileImprovementPlans.addAll(newPlans);
        tileImprovementPlans.sort(ValuedAIObject.descendingValueComparator);
        if (!tileImprovementPlans.isEmpty()) {
            lb.add(", improve:");
            for (TileImprovementPlan tip : tileImprovementPlans) {
                lb.add(" ", tip.getTarget(), "-", tip.getType().getSuffix());
            }
        }
    }

    /**
     * Get the list of buildables in the colony plan.
     *
     * Public for the test suite.
     *
     * @return A list of planned {@code BuildableType}.
     */
    public List<BuildableType> getPlannedBuildableTypes() {
        return (colonyPlan == null) ? Collections.<BuildableType>emptyList()
            : colonyPlan.getBuildableTypes();
    }

    /**
     * Summarize the colony plan as a string.
     *
     * @return A summary of the colony plan.
     */
    public String planToString() {
        if (colonyPlan == null) return "No plan.";

        LogBuilder lb = new LogBuilder(256);
        lb.add(colonyPlan, "\n\nTILE IMPROVEMENTS:\n");
        for (TileImprovementPlan tip : getTileImprovementPlans()) {
            lb.add(tip, "\n");
        }
        lb.add("\n\nWISHES:\n");
        for (Wish w : getWishes()) lb.add(w, "\n");
        lb.add("\n\nEXPORT GOODS:\n");
        for (AIGoods aig : getExportGoods()) lb.add(aig, "\n");
        return lb.toString();
    }

    /**
     * Handle REARRANGE_WORKERS property change events.
     *
     * @param event The {@code PropertyChangeEvent}.
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        logger.finest("Property change REARRANGE_WORKERS fired.");
        requestRearrange();

        // Check for goods party!
        if (event != null 
            && event.getOldValue() instanceof GoodsType) {
            GoodsType goodsType = (GoodsType)event.getOldValue();
            int left = colony.getGoodsCount(goodsType);
            for (AIGoods aig : getExportGoods()) {
                boolean remove = false;
                if (aig.isDisposed()) {
                    remove = true;
                } else if (aig.getGoods() == null) {
                    aig.changeTransport(null);
                    remove = true;
                } else if (aig.getGoodsType() == goodsType) {
                    if (left > 0) {
                        aig.getGoods().setAmount(left);
                    } else {
                        aig.changeTransport(null);
                        remove = true;
                    }
                }
                if (remove) {
                    removeExportGoods(aig);
                    break;
                }
            }
        }
    }

    /**
     * Sets the rearrangeTurn variable such that rearrangeColony will
     * run fully next time it is invoked.
     */
    public void requestRearrange() {
        rearrangeTurn = new Turn(0);
    }


    // Override AIObject

    /**
     * Disposes this {@code AIColony}.
     */
    @Override
    public void dispose() {
        final Predicate<AIGoods> ourGoodsPred = aig ->
            !aig.isDisposed() && aig.getGoods() != null
                && aig.getGoods().getLocation() == colony;
        List<AIObject> objects = transform(getExportGoods(), ourGoodsPred,
                                           aig -> (AIObject)aig);
        objects.addAll(wishes);
        clearWishes();
        objects.addAll(tileImprovementPlans);
        clearTileImprovementPlans();
        for (AIObject o : objects) o.dispose();
        colonyPlan = null;
        // Do not clear this.colony, the identifier is still required.
        super.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        IntegrityType result = super.checkIntegrity(fix, lb);
        if (colony == null || colony.isDisposed()) {
            lb.add("\n  Null colony: ", getId());
            result = result.fail();
        }
        return result;
    }


    // Serialization

    private static final String AI_GOODS_LIST_TAG
        = AIGoods.TAG + LIST_ELEMENT;
    private static final String GOODS_WISH_LIST_TAG
        = GoodsWish.TAG + LIST_ELEMENT;
    private static final String TILE_IMPROVEMENT_PLAN_LIST_TAG
        = TileImprovementPlan.TAG + LIST_ELEMENT;
    private static final String WORKER_WISH_LIST_TAG
        = WorkerWish.TAG + LIST_ELEMENT;
    // @compat 0.11.3
    private static final String OLD_TILE_IMPROVEMENT_PLAN_LIST_TAG
        = "tileimprovementplan" + LIST_ELEMENT;
    // end @compat 0.11.3


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (AIGoods ag : getExportGoods()) {
            if (!ag.checkIntegrity(true).safe()) continue;
            xw.writeStartElement(AI_GOODS_LIST_TAG);

            xw.writeAttribute(ID_ATTRIBUTE_TAG, ag);

            xw.writeEndElement();
        }

        for (TileImprovementPlan tip : tileImprovementPlans) {
            if (!tip.checkIntegrity(true).safe()) continue;

            xw.writeStartElement(TILE_IMPROVEMENT_PLAN_LIST_TAG);

            xw.writeAttribute(ID_ATTRIBUTE_TAG, tip);

            xw.writeEndElement();
        }

        for (Wish w : wishes) {
            String tag = (w instanceof GoodsWish) ? GOODS_WISH_LIST_TAG
                : (w instanceof WorkerWish) ? WORKER_WISH_LIST_TAG
                : null;
            if (!w.checkIntegrity(true).safe() || !w.shouldBeStored()
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
        baseInitialize(); // Make sure the containers exist
        // Clear containers.
        clearExportGoods();
        clearTileImprovementPlans();
        clearWishes();

        super.readChildren(xr);
        sortExportGoods();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final AIMain aiMain = getAIMain();
        final String tag = xr.getLocalName();

        // Note: the list elements are just references by identifier,
        // and rely on their being a full definition somewhere in the
        // <AIMain> structure.
        if (AI_GOODS_LIST_TAG.equals(tag)) {
            addExportGoods(xr.makeAIObject(aiMain, ID_ATTRIBUTE_TAG,
                                           AIGoods.class, (AIGoods)null, true));
            xr.closeTag(AI_GOODS_LIST_TAG);

        } else if (GOODS_WISH_LIST_TAG.equals(tag)) {
            addWish(xr.makeAIObject(aiMain, ID_ATTRIBUTE_TAG,
                                    GoodsWish.class, (GoodsWish)null, true));
            xr.closeTag(GOODS_WISH_LIST_TAG);

        } else if (TILE_IMPROVEMENT_PLAN_LIST_TAG.equals(tag)
            // @compat 0.11.3
            || OLD_TILE_IMPROVEMENT_PLAN_LIST_TAG.equals(tag)
            // end @compat 0.11.3
                   ) {
            addTileImprovementPlan(xr.makeAIObject(aiMain, ID_ATTRIBUTE_TAG,
                    TileImprovementPlan.class, (TileImprovementPlan)null, true));
            xr.closeTag(tag);// FIXME: tag -> TILE_IMPROVEMENT_PLAN_LIST_TAG

        } else if (WORKER_WISH_LIST_TAG.equals(tag)) {
            addWish(xr.makeAIObject(aiMain, ID_ATTRIBUTE_TAG,
                                    WorkerWish.class, (WorkerWish)null, true));
            xr.closeTag(WORKER_WISH_LIST_TAG);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return TAG; }
}
