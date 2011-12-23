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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitWas;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.server.ai.mission.BuildColonyMission;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.IdleAtColonyMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.WorkInsideColonyMission;

import org.w3c.dom.Element;


/**
 * Objects of this class contains AI-information for a single {@link Colony}.
 */
public class AIColony extends AIObject implements PropertyChangeListener {

    private static final Logger logger = Logger.getLogger(AIColony.class.getName());

    private static final String LIST_ELEMENT = "ListElement";

    // Do not bother trying to ship out less than this amount of goods.
    private static final int EXPORT_MINIMUM = 10;

    // The colony this AIColony is managing.
    private Colony colony;

    // The current production plan for the colony.
    private ColonyPlan colonyPlan;

    // Goods to export from the colony.
    private final List<AIGoods> aiGoods = new ArrayList<AIGoods>();

    // Useful things for the colony.
    private ArrayList<Wish> wishes = new ArrayList<Wish>();

    // Plans to improve neighbouring tiles.
    private ArrayList<TileImprovementPlan> tileImprovementPlans
        = new ArrayList<TileImprovementPlan>();

    // When should the workers in this Colony be rearranged?
    private Turn rearrangeWorkers = new Turn(0);

    // Goods that should be completely exported and only exported to
    // prevent the warehouse filling.
    private static final Set<GoodsType> fullExport = new HashSet<GoodsType>();
    private static final Set<GoodsType> partExport = new HashSet<GoodsType>();


    /**
     * Creates a new <code>AIColony</code>.
     *
     * @param aiMain The main AI-object.
     * @param colony The colony to make an {@link AIObject} for.
     */
    public AIColony(AIMain aiMain, Colony colony) {
        super(aiMain, colony.getId());

        this.colony = colony;
        colonyPlan = new ColonyPlan(aiMain, colony);
        colony.addPropertyChangeListener(Colony.REARRANGE_WORKERS, this);
    }

    /**
     * Creates a new <code>AIColony</code>.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public AIColony(AIMain aiMain, Element element) {
        super(aiMain, element.getAttribute(ID_ATTRIBUTE));
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>AIColony</code>.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public AIColony(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain, in.getAttributeValue(null, ID_ATTRIBUTE));
        readFromXML(in);
    }

    /**
     * Creates a new <code>AIColony</code>.
     *
     * @param aiMain The main AI-object.
     * @param id
     */
    public AIColony(AIMain aiMain, String id) {
        this(aiMain, (Colony) aiMain.getGame().getFreeColGameObject(id));
    }

    /**
     * Gets the <code>Colony</code> this <code>AIColony</code> controls.
     *
     * @return The <code>Colony</code>.
     */
    public Colony getColony() {
        return colony;
    }

    /**
     * Gets the current plan for this colony.
     *
     * @return The current <code>ColonyPlan</code>.
     */
    public ColonyPlan getColonyPlan() {
        return colonyPlan;
    }


    protected AIUnit getAIUnit(Unit unit) {
        return getAIMain().getAIUnit(unit);
    }

    protected AIPlayer getAIOwner() {
        return getAIMain().getAIPlayer(colony.getOwner());
    }

    protected Connection getConnection() {
        return getAIOwner().getConnection();
    }


    /**
     * Disposes this <code>AIColony</code>.
     */
    public void dispose() {
        List<AIObject> disposeList = new ArrayList<AIObject>();
        for (AIGoods ag : aiGoods) {
            if (ag.getGoods().getLocation() == colony) {
                disposeList.add(ag);
            }
        }
        for (Wish w : wishes) {
            disposeList.add(w);
        }
        for (TileImprovementPlan ti : tileImprovementPlans) {
            disposeList.add(ti);
        }
        for (AIObject o : disposeList) {
            o.dispose();
        }
        super.dispose();
    }

    /**
     * Is the colony badly defended?
     * Deliberately does not waste defenders on small colonies.
     *
     * @param colony The <code>Colony</code> to consider.
     * @return True if the colony needs more defenders.
     */
    public static boolean isBadlyDefended(Colony colony) {
        return colony.getTotalDefencePower()
            < 1.25f * colony.getUnitCount() - 2.5f;
    }

    /**
     * Rearranges the workers within this colony using the {@link ColonyPlan}.
     * TODO: Detect military threats and boost defence.
     *
     * @return True if the workers were rearranged.
     */
    public boolean rearrangeWorkers() {
        int turn = getGame().getTurn().getNumber();
        if (colony.getCurrentlyBuilding() == null
            && colonyPlan.getBestBuildableType() != null
            && rearrangeWorkers.getNumber() > turn) {
            logger.warning(colony.getName() + " could be building but"
                + " is asleep until turn: " + rearrangeWorkers.getNumber()
                + "( > " + turn + ")");
        }
        if (rearrangeWorkers.getNumber() > turn) return false;
        final AIMain aiMain = getAIMain();
        final Tile tile = colony.getTile();
        final Player player = colony.getOwner();
        final Specification spec = colony.getSpecification();

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
                AIMessage.askClaimLand(getConnection(), t, colony, 0);
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
        for (Unit u : workers) was.add(new UnitWas(u));
        for (Unit u : tile.getUnitList()) {
            if (!u.isPerson()) continue;
            Mission mission = getAIUnit(u).getMission();
            if (mission == null
                || mission instanceof IdleAtColonyMission
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
        // ATM we just accept this assignment.
        Colony scratch = colonyPlan.assignWorkers(workers);

        // Apply the arrangement, and give suitable missions to all units.
        // For now, do a soft rearrange (that is, no c-s messaging).
        // Also change the goods counts as we may have changed equipment.
        // TODO: Better would be to restore the initial state and use
        // a special c-s message to execute the rearrangement--- code to
        // untangle the movement dependencies is non-trivial.
        for (Unit u : scratch.getUnitList()) {
            AIUnit aiU = getAIUnit(u);
            WorkLocation wl = (WorkLocation)u.getLocation();
            wl = colony.getCorrespondingWorkLocation(wl);
            u.setLocation(wl);
        }
        for (Unit u : scratch.getTile().getUnitList()) {
            u.setLocation(tile);
        }            
        for (GoodsType g : spec.getGoodsTypeList()) {
            if (!g.isStorable()) continue;
            int oldCount = colony.getGoodsCount(g);
            int newCount = scratch.getGoodsCount(g);
            if (newCount != oldCount) {
                colony.getGoodsContainer().addGoods(g, newCount - oldCount);
            }
        }
        scratch.dispose();

        // Emergency recovery if something broke and the colony is empty.
        if (colony.getUnitCount() <= 0) {
            String destruct = "Autodestruct at " + colony.getName()
                + " in " + turn + "\n";
            for (UnitWas uw : was) destruct += uw.toString() + "\n";
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
            logger.warning(colony.getName() + " reneged building " + build);
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
            + " + " + nextRearrange + "\n";
        for (UnitWas uw : was) report += uw.toString() + "\n";
        logger.finest(report);

        // Give suitable missions to all units.
        for (Unit u : colony.getUnitList()) {
            AIUnit aiU = getAIUnit(u);
            aiU.setMission(new WorkInsideColonyMission(aiMain, aiU, this));
        }
        for (Unit u : tile.getUnitList()) {
            if (u.isArmed()) {
                AIUnit aiU = getAIUnit(u);
                aiU.setMission(new DefendSettlementMission(aiMain, aiU,
                        colony));
            }
        }

        // Change the export settings when required.
        resetExports();

        // TODO: these look rational but need review.
        createTileImprovementPlans();
        createWishes();

        // Set the next rearrangement turn.
        rearrangeWorkers = new Turn(turn + nextRearrange);
        return true;
    }

    /**
     * Reset the export settings.
     * This is always needed even when there is no customs house, because
     * createAIGoods needs to know what to export by transport.
     * TODO: consider market prices?
     */
    private void resetExports() {
        final Specification spec = colony.getSpecification();
        final List<GoodsType> produce = colonyPlan.getPreferredProduction();
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
                    if (g.isRawMaterial() && produce.contains(g)) {
                        partExport.add(g);
                    } else {
                        fullExport.add(g);
                    }
                }
            }
            for (EquipmentType e : spec.getEquipmentTypeList()) {
                for (AbstractGoods ag : e.getGoodsRequired()) {
                    fullExport.remove(ag.getType());
                    partExport.add(ag.getType());
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
                    || u.hasAbility("model.ability.expertScout"))) {
                explorers.add(u);
            }
        }
        for (Tile t : tile.getSurroundingTiles(1)) {
            if (t.hasLostCityRumour()) {
                Direction direction = tile.getDirection(t);
                for (;;) {
                    if (explorers.isEmpty()) return;
                    Unit u = explorers.remove(0);
                    if (!u.getMoveType(t).isProgress()) continue;
                    if (AIMessage.askMove(getAIUnit(u), direction)
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
        final Specification spec = colony.getSpecification();
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
            if (colony.getProductionOf(g) <= 0) needed.add(g);
        }

        // If a tile can be stolen, do so if already at war with the
        // owner or if it is the best one available.
        UnitType unitType = spec.getDefaultUnitType();
        Tile steal = null;
        float score = 1.0f;
        for (Tile t : tile.getSurroundingTiles(1)) {
            Player owner = t.getOwner();
            if (owner == null || owner == player
                || owner.isEuropean()) continue;
            if (owner.atWarWith(player)) {
                if (AIMessage.askClaimLand(getConnection(), t, colony,
                        NetworkConstants.STEAL_LAND)
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
            if (AIMessage.askClaimLand(getConnection(), steal, colony,
                    NetworkConstants.STEAL_LAND)
                && player.owns(steal)) {
                logger.info(player.getName() + " stole tile " + steal
                    + " (score = " + score
                    + ") from " + owner.getName());
            }
        }
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
        while (aiGoods.remove(ag)) { /* Do nothing here */
        }
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
                .removeFromTransportList(ag);
        }
        aiGoods.remove(ag);
        ag.dispose();
    }

    /**
     * Creates a list of the goods which should be shipped out of this colony.
     */
    public void createAIGoods() {
        if (colony.hasAbility(Ability.EXPORT)) {
            while (!aiGoods.isEmpty()) {
                AIGoods ag = aiGoods.remove(0);
                dropGoods(ag);
            }
            return;
        }

        final Europe europe = colony.getOwner().getEurope();
        final int capacity = colony.getWarehouseCapacity();
        List<AIGoods> newAIGoods = new ArrayList<AIGoods>();
        List<AIGoods> oldAIGoods = new ArrayList<AIGoods>();
        for (GoodsType g : colony.getSpecification().getGoodsTypeList()) {
            if (colony.getAdjustedNetProductionOf(g) < 0) continue;
            int count = colony.getGoodsCount(g);
            int exportAmount = (fullExport.contains(g))
                ? count
                : (partExport.contains(g))
                ? count - colony.getExportData(g).getExportLevel()
                : -1;
            int priority = (exportAmount >= capacity)
                ? AIGoods.IMPORTANT_DELIVERY
                : (exportAmount > GoodsContainer.CARGO_SIZE)
                ? AIGoods.FULL_DELIVERY
                : 0;
            //if (exportAmount > 0) {
            //    logger.finest(String.format("%-20s %-8s AIGoods: %d %s\n",
            //            colony.getName(), "finds", 
            //            exportAmount, g.toString().substring(12)));
            //}

            int i = 0;
            while (i < aiGoods.size()) {
                AIGoods ag = aiGoods.get(i);
                if (ag == null) {
                    aiGoods.remove(i);
                    continue;
                }
                if (ag.getGoods() == null
                    || ag.getGoods().getType() == null
                    || ag.getGoods().getAmount() <= 0) {
                    dropGoods(ag);
                    continue;
                }
                Goods oldGoods = ag.getGoods();
                if (oldGoods.getLocation() != colony) {
                    // Its on its way.  No longer of interest to the colony.
                    aiGoods.remove(ag);
                    continue;
                }
                if (oldGoods.getType() != g) {
                    i++;
                    continue;
                }
                
                int oldAmount = oldGoods.getAmount();
                String msg = null;
                if (oldAmount < exportAmount) {
                    int goodsAmount = oldAmount;
                    if (oldAmount < GoodsContainer.CARGO_SIZE) {
                        goodsAmount = Math.min(exportAmount,
                            GoodsContainer.CARGO_SIZE);
                        oldGoods.setAmount(goodsAmount);
                        //msg = String.format("%-20s %-8s AIGoods: %s %d %s\n",
                        //    colony.getName(), "grows", ag.getId(),
                        //    oldGoods.getAmount(), g.toString().substring(12));
                        ag.setTransportPriority(priority);
                    } else {
                        //msg = String.format("%-20s %-8s AIGoods: %s full %s\n",
                        //    colony.getName(), "keeps", ag.getId(),
                        //    g.toString().substring(12));
                    }
                    exportAmount -= goodsAmount;
                    oldAIGoods.add(ag);
                } else if (oldAmount == exportAmount) {
                    //msg = String.format("%-20s %-8s AIGoods: %s %d %s\n",
                    //    colony.getName(), "keeps", ag.getId(),
                    //    oldGoods.getAmount(), g.toString().substring(12));
                    oldAIGoods.add(ag);
                    exportAmount = 0;
                } else { // oldAmount > exportAmount
                    if (exportAmount <= 0) {
                        msg = String.format("%-20s %-8s AIGoods: %s %d %s\n",
                            colony.getName(), "drops", ag.getId(),
                            oldGoods.getAmount(), g.toString().substring(12));
                        dropGoods(ag);
                        continue;
                    }
                    oldGoods.setAmount(exportAmount);
                    //msg = String.format("%-20s %-8s AIGoods: %s %d %s\n",
                    //    colony.getName(), "shrinks", ag.getId(),
                    //    oldGoods.getAmount(), g.toString().substring(12));
                    oldAIGoods.add(ag);
                    exportAmount = 0;
                }
                if (msg != null) logger.finest(msg);
                i++;
            }

            while (exportAmount >= GoodsContainer.CARGO_SIZE) {
                AIGoods newGoods = new AIGoods(getAIMain(), colony, g,
                    GoodsContainer.CARGO_SIZE, europe);
                logger.finest(String.format("%-20s %-8s AIGoods: %s full %s\n",
                        colony.getName(), "makes", newGoods.getId(),
                        g.toString().substring(12)));
                newGoods.setTransportPriority(priority);
                newAIGoods.add(newGoods);
                exportAmount -= GoodsContainer.CARGO_SIZE;
            }
            if (exportAmount >= EXPORT_MINIMUM) {
                AIGoods newGoods = new AIGoods(getAIMain(), colony, g,
                    exportAmount, europe);
                logger.finest(String.format("%-20s %-8s AIGoods: %s %d %s\n",
                        colony.getName(), "makes", newGoods.getId(),
                        exportAmount, g.toString().substring(12)));
                newAIGoods.add(newGoods);
            }
        }
        aiGoods.clear();
        aiGoods.addAll(oldAIGoods);
        aiGoods.addAll(newAIGoods);
        Collections.sort(aiGoods, AIGoods.getAIGoodsPriorityComparator());
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
     * Removes a wish from the wishes list.
     *
     * @param wish The <code>Wish</code> to remove.
     */
    public void removeWish(Wish wish) {
        wishes.remove(wish);
    }

    /**
     * Tries to complete any wishes for some goods that have just arrived.
     *
     * @param goods Some <code>Goods</code> that are arriving in this colony.
     */
    public void completeWish(Goods goods) {
        int i = 0;
        while (i < wishes.size()) {
            if (wishes.get(i) instanceof GoodsWish) {
                GoodsWish gw = (GoodsWish)wishes.get(i);
                if (gw.getGoodsType() == goods.getType()
                    && gw.getGoodsAmount() <= goods.getAmount()) {
                    logger.finest(colony.getName()
                        + " completes goods wish: " + gw);
                    wishes.remove(gw);
                    gw.dispose();
                    continue;
                }
            }
            i++;
        }
    }

    /**
     * Tries to complete any wishes for a unit that has just arrived.
     *
     * @param unit A <code>Unit</code> that is arriving in this colony.
     */
    public void completeWish(Unit unit) {
        int i = 0;
        while (i < wishes.size()) {
            if (wishes.get(i) instanceof WorkerWish) {
                WorkerWish ww = (WorkerWish)wishes.get(i);
                if (ww.getUnitType() == unit.getType()) {
                    logger.finest(colony.getName()
                        + " completes worker wish: " + ww);
                    wishes.remove(ww);
                    ww.dispose();
                    continue;
                }
            }
            i++;
        }
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
     * Gets an <code>Iterator</code> for every <code>Wish</code> the
     * <code>Colony</code> has.
     *
     * @return The <code>Iterator</code>. The items with the
     *         {@link Wish#getValue highest value} appears first in the
     *         <code>Iterator</code>
     * @see Wish
     */
    public Iterator<Wish> getWishIterator() {
        return wishes.iterator();
    }

    /**
     * Creates the wishes for the <code>Colony</code>.
     */
    private void createWishes() {
        wishes.clear();
        createWorkerWishes();
        createGoodsWishes();
    }

    /**
     * Creates the worker wishes.
     */
    private void createWorkerWishes() {
        final Specification spec = colony.getSpecification();
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
            WorkerWish ww = new WorkerWish(getAIMain(), colony, value, expert,
                true);
            wishes.add(ww);
            logger.finest("New WorkerWish at " + colony.getName()
                + ": " + ww.getId() + " " + ww);
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
            WorkerWish ww = new WorkerWish(getAIMain(), colony, 50, expert,
                false);
            wishes.add(ww);
            logger.finest("New WorkerWish at " + colony.getName()
                + ": " + ww.getId() + " " + ww);
        }

        // TODO: check for students
        // TODO: add missionaries

        // Improve defence.
        if (isBadlyDefended(colony)) {
            UnitType bestDefender = colony.getBestDefenderType();
            if (bestDefender != null) {
                WorkerWish ww = new WorkerWish(getAIMain(), colony, 100,
                                               bestDefender, true);
                wishes.add(ww);
                logger.finest("New WorkerWish at " + colony.getName()
                    + ": " + ww.getId() + " " + ww);
            }
        }
    }

    /**
     * Creates the goods wishes.
     */
    private void createGoodsWishes() {
        final Specification spec = colony.getSpecification();
        int goodsWishValue = 50;

        // request goods
        // TODO: improve heuristics
        TypeCountMap<GoodsType> required = new TypeCountMap<GoodsType>();

        // add building materials
        if (colony.getCurrentlyBuilding() != null) {
            for (AbstractGoods ag : colony.getCurrentlyBuilding()
                     .getGoodsRequired()) {
                if (colony.getAdjustedNetProductionOf(ag.getType()) <= 0) {
                    required.incrementCount(ag.getType(), ag.getAmount());
                }
            }
        }

        // add materials required to improve tiles
        for (TileImprovementPlan plan : tileImprovementPlans) {
            for (AbstractGoods ag : plan.getType().getExpendedEquipmentType()
                     .getGoodsRequired()) {
                required.incrementCount(ag.getType(), ag.getAmount());
            }
        }

        // add raw materials for buildings
        for (WorkLocation workLocation : colony.getCurrentWorkLocations()) {
            if (workLocation instanceof Building) {
                Building building = (Building) workLocation;
                GoodsType inputType = building.getGoodsInputType();
                ProductionInfo info = colony.getProductionInfo(building);
                if (inputType != null
                    && info != null
                    && !info.hasMaximumProduction()) {
                    // TODO: find better heuristics
                    required.incrementCount(inputType, 100);
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
        if (isBadlyDefended(colony)) {
            for (EquipmentType type : spec.getEquipmentTypeList()) {
                if (!type.isMilitaryEquipment()) continue;
                for (Unit unit : colony.getTile().getUnitList()) {
                    if (!unit.canBeEquippedWith(type)) continue;
                    for (AbstractGoods ag : type.getGoodsRequired()) {
                        required.incrementCount(ag.getType(), ag.getAmount());
                    }
                    break;
                }
            }
        }

        for (GoodsType type : required.keySet()) {
            GoodsType requiredType = type;
            while (requiredType != null) {
                if (requiredType.isStorable()) break;
                requiredType = requiredType.getRawMaterial();
            }
            if (requiredType != null) {
                int amount = Math.min(colony.getWarehouseCapacity(),
                    (required.getCount(requiredType)
                        - colony.getGoodsCount(requiredType)));
                if (amount > 0) {
                    int value = goodsWishValue;
                    if (colonyCouldProduce(requiredType)) value /= 10;
                    GoodsWish gw = new GoodsWish(getAIMain(), colony, value,
                        amount, requiredType);
                    wishes.add(gw);
                    logger.finest("New GoodsWish at " + colony.getName()
                        + ": " + gw.getId() + " " + gw);
                }
            }
        }
        Collections.sort(wishes);
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
                return (goodsType.getRawMaterial() == null) ? true
                    : colonyCouldProduce(goodsType.getRawMaterial());
            }
        }
        return false;
    }

    /**
     * Returns an <code>Iterator</code> over all the
     * <code>TileImprovementPlan</code>s needed by this colony.
     *
     * @return The <code>Iterator</code>.
     * @see TileImprovementPlan
     */
    public Iterator<TileImprovementPlan> getTileImprovementPlanIterator() {
        return tileImprovementPlans.iterator();
    }

    /**
     * Adds a tile improvement plan to this AI colony.
     *
     * @param tip The <code>TileImprovementPlan</code> to add.
     */
    public void addTileImprovementPlan(TileImprovementPlan tip) {
        tileImprovementPlans.add(tip);
    }

    /**
     * Removes a <code>TileImprovementPlan</code> from the list
     * @return True if it was successfully deleted, false otherwise
     */
    public boolean removeTileImprovementPlan(TileImprovementPlan plan){
        return tileImprovementPlans.remove(plan);
    }

    /**
     * Creates a list of the <code>Tile</code>-improvements which will
     * increase the production by this <code>Colony</code>.
     *
     * @see TileImprovementPlan
     */
    public void createTileImprovementPlans() {
        Map<Tile, TileImprovementPlan> plans
            = new HashMap<Tile, TileImprovementPlan>();
        for (TileImprovementPlan plan : tileImprovementPlans) {
            plans.put(plan.getTarget(), plan);
        }
        for (WorkLocationPlan wlp : colonyPlan.getTilePlans()) {
            ColonyTile colonyTile = (ColonyTile) wlp.getWorkLocation();
            Tile target = colonyTile.getWorkTile();
            boolean others = target.getOwningSettlement() != colony
                && target.getOwner() == colony.getOwner();
            TileImprovementPlan plan = plans.get(target);
            if (plan == null) {
                if (others) continue; // owned by another of our colonies
                plan = wlp.createTileImprovementPlan();
                if (plan != null) {
                    int value = plan.getValue();
                    if (!colonyTile.isEmpty()) value *= 2;
                    value -= colony.getOwner().getLandPrice(target);
                    plan.setValue(value);
                    tileImprovementPlans.add(plan);
                    plans.put(target, plan);
                }
            } else if (wlp.updateTileImprovementPlan(plan) == null
                || others) {
                tileImprovementPlans.remove(plan);
                plan.dispose();
            }
        }

        Tile centerTile = colony.getTile();
        TileImprovementPlan centerPlan = plans.get(centerTile);
        TileImprovementType type = WorkLocationPlan
            .findBestTileImprovementType(centerTile, colony.getSpecification()
                                         .getGoodsType("model.goods.grain"));
        if (type == null) {
            if (centerPlan != null) {
                tileImprovementPlans.remove(centerPlan);
            }
        } else {
            if (centerPlan == null) {
                centerPlan = new TileImprovementPlan(getAIMain(), colony.getTile(), type, 30);
                tileImprovementPlans.add(0, centerPlan);
            } else {
                centerPlan.setType(type);
            }
        }

        Collections.sort(tileImprovementPlans);
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
        List<GoodsType> libertyGoods = colony.getSpecification()
            .getLibertyGoodsTypeList();
        for (Unit u : colony.getTile().getUnitList()) {
            if (!u.isPerson()) continue;
            for (WorkLocation wl : colony.getAvailableWorkLocations()) {
                if (!wl.canAdd(u)) continue;
                for (GoodsType type : libertyGoods) {
                    if (wl.getPotentialProduction(u.getType(), type) > 0
                        && AIMessage.askWork(getAIUnit(u), wl)
                        && u.getLocation() == wl) {
                        AIMessage.askChangeWorkType(getAIUnit(u), type);
                        logger.warning("Colony " + colony.getName()
                            + " autodestruct averted.");
                        break;
                    }
                }
            }
        }
        // No good, no choice but to fail.
        if (colony.getUnitCount() <= 0) {
            throw new IllegalStateException("Colony " + colony.getName()
                + " rearrangement leaves no units!");
        }
    }

    public void propertyChange(PropertyChangeEvent event) {
        logger.finest("Property change REARRANGE_WORKERS fired.");
        rearrangeWorkers = new Turn(0);
    }

    // Serialization

    /**
     * Writes this object to an XML stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute(ID_ATTRIBUTE, getId());

        for (AIGoods ag : aiGoods) {
            if (ag.getId() == null) {
                logger.warning("ag.getId() == null");
                continue;
            }
            out.writeStartElement(ag.getXMLElementTagName() + LIST_ELEMENT);
            out.writeAttribute(ID_ATTRIBUTE, ag.getId());
            out.writeEndElement();
        }

        for (Wish w : wishes) {
            if (!w.shouldBeStored()) continue;
            String tag = (w instanceof GoodsWish) ? GoodsWish.getXMLElementTagName()
                : (w instanceof WorkerWish) ? WorkerWish.getXMLElementTagName()
                : null;
            if (tag == null) continue;
            out.writeStartElement(tag + LIST_ELEMENT);
            out.writeAttribute(ID_ATTRIBUTE, w.getId());
            out.writeEndElement();
        }

        for (TileImprovementPlan tip : tileImprovementPlans) {
            out.writeStartElement(tip.getXMLElementTagName() + LIST_ELEMENT);
            out.writeAttribute(ID_ATTRIBUTE, tip.getId());
            out.writeEndElement();
        }

        out.writeEndElement();
    }

    /**
     * Reads information for this object from an XML stream.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading from the
     *             stream.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        colony = (Colony) getAIMain().getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE));
        if (colony == null) {
            throw new NullPointerException("Could not find Colony with ID: "
                + in.getAttributeValue(null, ID_ATTRIBUTE));
        }

        aiGoods.clear();
        wishes.clear();
        colonyPlan = new ColonyPlan(getAIMain(), colony);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(AIGoods.getXMLElementTagName() + LIST_ELEMENT)) {
                AIGoods ag = (AIGoods) getAIMain().getAIObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (ag == null) {
                    ag = new AIGoods(getAIMain(), in.getAttributeValue(null, ID_ATTRIBUTE));
                }
                aiGoods.add(ag);
                in.nextTag();
            } else if (in.getLocalName().equals(WorkerWish.getXMLElementTagName() + LIST_ELEMENT)
                // @compat 0.10.3
                || in.getLocalName().equals(WorkerWish.getXMLElementTagName() + "Wish" + LIST_ELEMENT)                       
                // end compatibility code
                       ) {
                Wish w = (Wish) getAIMain().getAIObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (w == null) {
                    w = new WorkerWish(getAIMain(), in.getAttributeValue(null, ID_ATTRIBUTE));
                }
                wishes.add(w);
                in.nextTag();
            } else if (in.getLocalName().equals(GoodsWish.getXMLElementTagName() + LIST_ELEMENT)
                // @compat 0.10.3
                || in.getLocalName().equals("GoodsWishWish" + LIST_ELEMENT)                       
                // end compatibility code
                       ) {
                Wish w = (Wish) getAIMain().getAIObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (w == null) {
                    w = new GoodsWish(getAIMain(), in.getAttributeValue(null, ID_ATTRIBUTE));
                }
                wishes.add(w);
                in.nextTag();
            } else if (in.getLocalName().equals(TileImprovementPlan.getXMLElementTagName() + LIST_ELEMENT)
                // @compat 0.10.3
                || in.getLocalName().equals("tileimprovementplan" + LIST_ELEMENT)
                // end compatibility code
                       ) {
                TileImprovementPlan ti = (TileImprovementPlan) getAIMain().getAIObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (ti == null) {
                    ti = new TileImprovementPlan(getAIMain(), in.getAttributeValue(null, ID_ATTRIBUTE));
                }
                tileImprovementPlans.add(ti);
                in.nextTag();
            } else {
                logger.warning("Unknown tag name: " + in.getLocalName());
            }
        }

        if (!in.getLocalName().equals(getXMLElementTagName())) {
            logger.warning("Expected end tag, received: " + in.getLocalName());
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "aiColony"
     */
    public static String getXMLElementTagName() {
        return "aiColony";
    }
}
