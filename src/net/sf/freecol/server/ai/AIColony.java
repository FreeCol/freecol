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
import net.sf.freecol.common.model.ExportData;
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

    // The colony this AIColony is managing.
    private Colony colony;

    // The current production plan for the colony.
    private ColonyPlan colonyPlan;

    // Goods to export from the colony.
    private ArrayList<AIGoods> aiGoods = new ArrayList<AIGoods>();

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
     * Rearranges the workers within this colony using the {@link ColonyPlan}.
     * TODO: Detect military threats and boost defence.
     *
     * @return True if the workers were rearranged.
     */
    public boolean rearrangeWorkers() {
        int turn = getGame().getTurn().getNumber();
        if (rearrangeWorkers.getNumber() > turn) return false;
        int nextRearrange = Integer.MAX_VALUE;
        final AIMain aiMain = getAIMain();
        final Tile tile = colony.getTile();
        final Player player = colony.getOwner();
        final Specification spec = colony.getSpecification();

        // See if there are neighbouring LCRs to explore, or tiles
        // to steal, or just unclaimed tiles (a neighbouring settlement
        // might have disappeared or relinquished a tile).
        // This needs to be done early so that new tiles can be
        // included in any new colony plan.
        exploreLCRs();
        stealTiles();
        for (Tile t : tile.getSurroundingTiles(1)) {
            if (player.canClaimForSettlement(t)) {
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
        BuildableType build = colony.getCurrentlyBuilding();
        if (!colony.canBuild(build)) build = null;
        for (BuildableType b : colonyPlan.getBuildableTypes()) {
            if (colony.canBuild(b)) {
                if (b != build) {
                    List<BuildableType> queue = new ArrayList<BuildableType>();
                    queue.add(b);
                    AIMessage.askSetBuildQueue(this, queue);
                }
                break;
            }
        }
        build = colony.getCurrentlyBuilding();
        if (build != null) {
            colonyPlan.refine(build);
            nextRearrange = Math.min(nextRearrange,
                Math.max(1, colony.getTurnsToComplete(build, null)));
        }

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
        Colony scratch = colonyPlan.assignWorkers(workers);
        // ATM we just accept this assignment.
        // Plan to rearrange when the warehouse hits a limit.
        int warehouse = scratch.getWarehouseCapacity();
        for (GoodsType g : spec.getGoodsTypeList()) {
            if (!g.isStorable() || g.limitIgnored()) continue;
            int have = scratch.getGoodsCount(g);
            int net = scratch.getNetProductionOf(g);
            if (net >= 0 && have >= warehouse) continue;
            nextRearrange = Math.max(1, Math.min(nextRearrange,
                    (net < 0) ? (have / -net)
                    : (net > 0) ? ((warehouse - have) / net)
                    : Integer.MAX_VALUE));
        }

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
            aiU.setMission(new WorkInsideColonyMission(aiMain, aiU, this));
        }
        for (Unit u : scratch.getTile().getUnitList()) {
            u.setLocation(tile);
            if (u.isArmed()) {
                AIUnit aiU = getAIUnit(u);
                aiU.setMission(new DefendSettlementMission(aiMain, aiU,
                        colony));
            }
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
            String destruct = "Autodestruct at " + colony.getName();
            for (UnitWas uw : was) destruct += uw.toString();
            logger.warning(destruct);
            avertAutoDestruction();
        }

        // Argh.  We may have chosen to build something we can no
        // longer build due to a colony size limitation.  Try to find
        // something, but do not re-refine/assign as we may get caught
        // in an infinite loop.  Just rearrange next turn.
        build = colony.getCurrentlyBuilding();
        if (!colony.canBuild(build)) {
            build = null;
            for (BuildableType b : colonyPlan.getBuildableTypes()) {
                if (colony.canBuild(b)) {
                    build = b;
                    break;
                }
            }
            List<BuildableType> queue = new ArrayList<BuildableType>();
            if (build != null) queue.add(build);
            AIMessage.askSetBuildQueue(this, queue);
            nextRearrange = 1;
        }

        // For now, cap the rearrangement horizon, because confidence
        // that we are triggering on all relevant changes is low.
        nextRearrange = Math.min(nextRearrange, 15);

        // Log the changes.
        StringBuilder sb = new StringBuilder();
        sb.append("Rearrange " + colony.getName()
            + " (" + colony.getUnitCount() + ")"
            + " build=" + colony.getCurrentlyBuilding()
            + " " + getGame().getTurn()
            + " + " + nextRearrange + "\n");
        for (UnitWas uw : was) sb.append(uw.toString() + "\n");
        logger.finest(sb.toString());

        // Change the export settings when required.
        resetExports();

        // TODO: these look rational but need review.
        createTileImprovementPlans();
        createWishes();
        checkConditionsForHorseBreed();

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
        if (fullExport.isEmpty()) {
            // Initialize the exportable sets.
            // Luxury goods and non-raw materials (silver) should always
            // be fully exported.
            // Other raw and manufactured goods should be exported only
            // to the extent of not filling the warehouse.
            for (GoodsType g : spec.getGoodsTypeList()) {
                if (g.isStorable()
                    && !g.isFoodType()
                    && !g.isTradeGoods()) {
                    if (g.isRawMaterial()) {
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
                    Unit u = explorers.get(0);
                    explorers.remove(0);
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
     * raw building materials with a lesser interest in food.
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


    private UnitType getNextExpert(boolean onlyFood) {
        // some type should be returned, not null
        UnitType bestType = colony.getSpecification().getDefaultUnitType();
        List<WorkLocationPlan> plans = colonyPlan.getFoodPlans();
        if (!onlyFood) plans.addAll(colonyPlan.getWorkPlans());
        for (WorkLocationPlan plan : plans) {
            WorkLocation location = plan.getWorkLocation();
            if (location instanceof ColonyTile) {
                ColonyTile colonyTile = (ColonyTile) location;
                if (colonyTile.canBeWorked()) {
                    bestType = colony.getSpecification()
                        .getExpertForProducing(plan.getGoodsType());
                    break;
                }
            } else if (location instanceof Building) {
                Building building = (Building) location;
                if (building.canBeWorked()) {
                    bestType = building.getExpertUnitType();
                    break;
                }
            }
        }
        return bestType;
    }

    private int getToolsRequired(BuildableType buildableType) {
        int toolsRequiredForBuilding = 0;
        if (buildableType != null) {
            for (AbstractGoods goodsRequired : buildableType.getGoodsRequired()) {
                if (goodsRequired.getType() == colony.getSpecification().getGoodsType("model.goods.tools")) {
                    toolsRequiredForBuilding = goodsRequired.getAmount();
                    break;
                }
            }
        }
        return toolsRequiredForBuilding;
    }


    private int getHammersRequired(BuildableType buildableType) {
        int hammersRequiredForBuilding = 0;
        if (buildableType != null) {
            for (AbstractGoods goodsRequired : buildableType.getGoodsRequired()) {
                if (goodsRequired.getType() == colony.getSpecification().getGoodsType("model.goods.hammers")) {
                    hammersRequiredForBuilding = goodsRequired.getAmount();
                    break;
                }
            }
        }
        return hammersRequiredForBuilding;
    }

    /**
     * Is the colony badly defended?
     * TODO: check if this heuristic makes sense.
     *
     * @return True if the colony needs more defenders.
     */
    public boolean isBadlyDefended() {
        return colony.getTotalDefencePower() < 1.5f * colony.getUnitCount();
    }


    public void removeWish(Wish w) {
        wishes.remove(w);
    }

    /**
     * Add a <code>GoodsWish</code> to the wish list.
     *
     * @param gw The <code>GoodsWish</code> to be added.
     */
    public void addGoodsWish(GoodsWish gw) {
        wishes.add(gw);
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
     * Creates a list of the goods which should be shipped out of this colony.
     * This is the list {@link #getAIGoodsIterator} returns the
     * <code>Iterator</code> for.
     */
    public void createAIGoods() {
        int capacity = colony.getWarehouseCapacity();
        if (colony.hasAbility(Ability.EXPORT)) {
            aiGoods.clear();
        } else {
            ArrayList<AIGoods> newAIGoods = new ArrayList<AIGoods>();
            for (GoodsType g : colony.getSpecification().getGoodsTypeList()) {
                if (!fullExport.contains(g)) continue;
                if (colony.getGoodsCount(g) > 0) {
                    List<AIGoods> alreadyAdded = new ArrayList<AIGoods>();
                    for (int j = 0; j < aiGoods.size(); j++) {
                        AIGoods ag = aiGoods.get(j);
                        if (ag == null) {
                            logger.warning("aiGoods == null");
                        } else if (ag.getGoods() == null) {
                            logger.warning("aiGoods.getGoods() == null");
                            if (ag.isUninitialized()) {
                                logger.warning("AIGoods uninitialized: "
                                    + ag.getId());
                            }
                        }
                        if (ag != null
                            && ag.getGoods() != null
                            && ag.getGoods().getType() == g
                            && ag.getGoods().getLocation() == colony) {
                            alreadyAdded.add(ag);
                        }
                    }

                    int amountRemaining = colony.getGoodsCount(g);
                    for (int i = 0; i < alreadyAdded.size(); i++) {
                        AIGoods oldGoods = alreadyAdded.get(i);
                        if (oldGoods.getGoods().getLocation() != colony) {
                            continue;
                        }
                        if (oldGoods.getGoods().getAmount() < GoodsContainer.CARGO_SIZE
                            && oldGoods.getGoods().getAmount() < amountRemaining) {
                            int goodsAmount = Math.min(GoodsContainer.CARGO_SIZE, amountRemaining);
                            oldGoods.getGoods().setAmount(goodsAmount);
                            if (amountRemaining >= colony.getWarehouseCapacity()
                                && oldGoods.getTransportPriority() < AIGoods.IMPORTANT_DELIVERY) {
                                oldGoods.setTransportPriority(AIGoods.IMPORTANT_DELIVERY);
                            } else if (goodsAmount == GoodsContainer.CARGO_SIZE
                                && oldGoods.getTransportPriority() < AIGoods.FULL_DELIVERY) {
                                oldGoods.setTransportPriority(AIGoods.FULL_DELIVERY);
                            }
                            amountRemaining -= goodsAmount;
                            newAIGoods.add(oldGoods);
                        } else if (oldGoods.getGoods().getAmount() > amountRemaining) {
                            if (amountRemaining == 0) {
                                if (oldGoods.getTransport() != null
                                    && oldGoods.getTransport().getMission() instanceof TransportMission) {
                                    ((TransportMission) oldGoods.getTransport().getMission())
                                    .removeFromTransportList(oldGoods);
                                }
                                oldGoods.dispose();
                            } else {
                                oldGoods.getGoods().setAmount(amountRemaining);
                                newAIGoods.add(oldGoods);
                                amountRemaining = 0;
                            }
                        } else {
                            newAIGoods.add(oldGoods);
                            amountRemaining -= oldGoods.getGoods().getAmount();
                        }
                    }
                    while (amountRemaining > 0) {
                        if (amountRemaining >= GoodsContainer.CARGO_SIZE) {
                            AIGoods newGoods = new AIGoods(getAIMain(), colony, g, GoodsContainer.CARGO_SIZE, getColony().getOwner()
                                                           .getEurope());
                            if (amountRemaining >= colony.getWarehouseCapacity()) {
                                newGoods.setTransportPriority(AIGoods.IMPORTANT_DELIVERY);
                            } else {
                                newGoods.setTransportPriority(AIGoods.FULL_DELIVERY);
                            }
                            newAIGoods.add(newGoods);
                            amountRemaining -= GoodsContainer.CARGO_SIZE;
                        } else {
                            AIGoods newGoods = new AIGoods(getAIMain(), colony, g, amountRemaining, getColony()
                                                           .getOwner().getEurope());
                            newAIGoods.add(newGoods);
                            amountRemaining = 0;
                        }
                    }
                }
            }

            aiGoods.clear();
            Iterator<AIGoods> nai = newAIGoods.iterator();
            while (nai.hasNext()) {
                AIGoods ag = nai.next();
                int i;
                for (i = 0; i < aiGoods.size() && aiGoods.get(i).getTransportPriority() > ag.getTransportPriority(); i++)
                    ;
                aiGoods.add(i, ag);
            }
        }
    }

    /**
     * Returns an <code>Iterator</code> of the goods to be shipped from this
     * colony. The item with the highest
     * {@link Transportable#getTransportPriority transport priority} gets
     * returned first by this <code>Iterator</code>.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<AIGoods> getAIGoodsIterator() {
        Iterator<AIGoods> agi = aiGoods.iterator();
        // TODO: Remove the following code and replace by throw RuntimeException
        while (agi.hasNext()) {
            AIGoods ag = agi.next();
            if (ag.getGoods().getLocation() != colony) {
                agi.remove();
            }
        }
        return aiGoods.iterator();
    }

    /**
     * Returns the available amount of the GoodsType given.
     *
     * @return The amount of tools not needed for the next thing we are
     *         building.
     */
    public int getAvailableGoods(GoodsType goodsType) {
        int materialsRequiredForBuilding = 0;
        if (colony.getCurrentlyBuilding() != null) {
            for (AbstractGoods materials : colony.getCurrentlyBuilding().getGoodsRequired()) {
                if (materials.getType() == goodsType) {
                    materialsRequiredForBuilding = materials.getAmount();
                    break;
                }
            }
        }

        return Math.max(0, colony.getGoodsCount(goodsType) - materialsRequiredForBuilding);
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
     * Creates the wishes for the <code>Colony</code>.
     */
    private void createWishes() {
        wishes.clear();
        createWorkerWishes();
        createGoodsWishes();
    }

    private void createWorkerWishes() {

        int expertValue = 100;

        // For every non-expert, request expert replacement. TODO:
        // value should depend on how urgently the unit is needed, and
        // possibly on skill, too.
        for (Unit unit : colony.getUnitList()) {
            if (unit.getWorkType() != null
                && unit.getWorkType() != unit.getType().getExpertProduction()) {
                UnitType expert = colony.getSpecification().getExpertForProducing(unit.getWorkType());
                wishes.add(new WorkerWish(getAIMain(), colony, expertValue, expert, true));
            }
        }

        // request population increase
        if (wishes.isEmpty()) {
            int newPopulation = colony.getUnitCount() + 1;
            if (colony.governmentChange(newPopulation) >= 0) {
                // population increase incurs no penalty
                boolean needFood = colony.getFoodProduction()
                    <= colony.getFoodConsumption() + colony.getOwner().getMaximumFoodConsumption();
                // choose expert for best work location plan
                UnitType expert = getNextExpert(needFood);
                wishes.add(new WorkerWish(getAIMain(), colony, expertValue / 5, expert, false));
            }
        }

        // TODO: check for students
        // TODO: add missionaries

        // increase defense value
        boolean badlyDefended = isBadlyDefended();
        if (badlyDefended) {
            UnitType bestDefender = colony.getBestDefenderType();
            if (bestDefender != null) {
                wishes.add(new WorkerWish(getAIMain(), colony, expertValue, bestDefender, true));
            }
        }
    }

    private void createGoodsWishes() {
        int goodsWishValue = 50;

        // request goods
        // TODO: improve heuristics
        TypeCountMap<GoodsType> requiredGoods = new TypeCountMap<GoodsType>();

        // add building materials
        if (colony.getCurrentlyBuilding() != null) {
            for (AbstractGoods goods : colony.getCurrentlyBuilding().getGoodsRequired()) {
                if (colony.getAdjustedNetProductionOf(goods.getType()) == 0) {
                    requiredGoods.incrementCount(goods.getType(), goods.getAmount());
                }
            }
        }

        // add materials required to improve tiles
        for (TileImprovementPlan plan : tileImprovementPlans) {
            for (AbstractGoods goods : plan.getType().getExpendedEquipmentType()
                     .getGoodsRequired()) {
                requiredGoods.incrementCount(goods.getType(), goods.getAmount());
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
                    requiredGoods.incrementCount(inputType, 100);
                }
            }
        }

        // add breedable goods
        for (GoodsType goodsType : colony.getSpecification().getGoodsTypeList()) {
            if (goodsType.isBreedable()) {
                requiredGoods.incrementCount(goodsType, goodsType.getBreedingNumber());
            }
        }

        // add materials required to build military equipment
        if (isBadlyDefended()) {
            for (EquipmentType type : colony.getSpecification().getEquipmentTypeList()) {
                if (type.isMilitaryEquipment()) {
                    for (Unit unit : colony.getUnitList()) {
                        if (unit.canBeEquippedWith(type)) {
                            for (AbstractGoods goods : type.getGoodsRequired()) {
                                requiredGoods.incrementCount(goods.getType(), goods.getAmount());
                            }
                            break;
                        }
                    }
                }
            }
        }

        for (GoodsType type : requiredGoods.keySet()) {
            GoodsType requiredType = type;
            while (requiredType != null && !requiredType.isStorable()) {
                requiredType = requiredType.getRawMaterial();
            }
            if (requiredType != null) {
                int amount = Math.min((requiredGoods.getCount(requiredType)
                                       - colony.getGoodsCount(requiredType)),
                                      colony.getWarehouseCapacity());
                if (amount > 0) {
                    int value = colonyCouldProduce(requiredType) ?
                        goodsWishValue / 10 : goodsWishValue;
                    wishes.add(new GoodsWish(getAIMain(), colony, value, amount, requiredType));
                }
            }
        }
        Collections.sort(wishes);
    }

    private boolean colonyCouldProduce(GoodsType goodsType) {
        if (goodsType.isBreedable()) {
            return colony.getGoodsCount(goodsType) >= goodsType.getBreedingNumber();
        } else if (goodsType.isFarmed()) {
            for (ColonyTile colonyTile : colony.getColonyTiles()) {
                if (colonyTile.getWorkTile().potential(goodsType, null) > 0) {
                    return true;
                }
            }
        } else {
            if (!colony.getBuildingsForProducing(goodsType).isEmpty()) {
                if (goodsType.getRawMaterial() == null) {
                    return true;
                } else {
                    return colonyCouldProduce(goodsType.getRawMaterial());
                }
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
     * Verifies if the <code>Colony</code> has conditions for breeding
     * horses, and un-mounts a mounted <code>Unit</code> if available,
     * to have horses to breed.
     *
     * Method is public so the test suite can call it.
     */
    public void checkConditionsForHorseBreed() {
        GoodsType horsesType = colony.getSpecification().getGoodsType("model.goods.horses");
        EquipmentType horsesEqType = colony.getSpecification().getEquipmentType("model.equipment.horses");
        GoodsType reqGoodsType = horsesType.getRawMaterial();

        // Colony already is breeding horses
        if(colony.getGoodsCount(horsesType) >= horsesType.getBreedingNumber()){
            return;
        }

        //int foodProdAvail = colony.getProductionOf(reqGoodsType) - colony.getConsumptionOf(reqGoodsType);
        int foodProdAvail = colony.getFoodProduction() - colony.getConsumptionOf(reqGoodsType);
        // no food production available for breeding anyway
        if(foodProdAvail <= 0){
            return;
        }

        // we will now look for any mounted unit that can be temporarily dismounted
        for(Unit u : colony.getTile().getUnitList()){
            int amount = u.getEquipmentCount(horsesEqType);
            if (amount > 0
                && AIMessage.askEquipUnit(getAIUnit(u), horsesEqType,
                                          -amount)) {
                if (colony.getGoodsCount(horsesType) >= horsesType.getBreedingNumber()) {
                    return;
                }
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

        Iterator<AIGoods> aiGoodsIterator = aiGoods.iterator();
        while (aiGoodsIterator.hasNext()) {
            AIGoods ag = aiGoodsIterator.next();
            if (ag == null) {
                logger.warning("ag == null");
                continue;
            }
            if (ag.getId() == null) {
                logger.warning("ag.getId() == null");
                continue;
            }
            out.writeStartElement(AIGoods.getXMLElementTagName() + "ListElement");
            out.writeAttribute(ID_ATTRIBUTE, ag.getId());
            out.writeEndElement();
        }

        Iterator<Wish> wishesIterator = wishes.iterator();
        while (wishesIterator.hasNext()) {
            Wish w = wishesIterator.next();
            if (!w.shouldBeStored()) {
                continue;
            }
            if (w instanceof WorkerWish) {
                out.writeStartElement(WorkerWish.getXMLElementTagName() + "WishListElement");
            } else if (w instanceof GoodsWish) {
                out.writeStartElement(GoodsWish.getXMLElementTagName() + "WishListElement");
            } else {
                logger.warning("Unknown type of wish.");
                continue;
            }
            out.writeAttribute(ID_ATTRIBUTE, w.getId());
            out.writeEndElement();
        }

        Iterator<TileImprovementPlan> TileImprovementPlanIterator = tileImprovementPlans.iterator();
        while (TileImprovementPlanIterator.hasNext()) {
            TileImprovementPlan ti = TileImprovementPlanIterator.next();
            out.writeStartElement(TileImprovementPlan.getXMLElementTagName() + "ListElement");
            out.writeAttribute(ID_ATTRIBUTE, ti.getId());
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
            throw new NullPointerException("Could not find Colony with ID: " + in.getAttributeValue(null, ID_ATTRIBUTE));
        }

        aiGoods.clear();
        wishes.clear();

        colonyPlan = new ColonyPlan(getAIMain(), colony);
        colonyPlan.update();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(AIGoods.getXMLElementTagName() + "ListElement")) {
                AIGoods ag = (AIGoods) getAIMain().getAIObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (ag == null) {
                    ag = new AIGoods(getAIMain(), in.getAttributeValue(null, ID_ATTRIBUTE));
                }
                aiGoods.add(ag);
                in.nextTag();
            } else if (in.getLocalName().equals(WorkerWish.getXMLElementTagName() + "WishListElement")) {
                Wish w = (Wish) getAIMain().getAIObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (w == null) {
                    w = new WorkerWish(getAIMain(), in.getAttributeValue(null, ID_ATTRIBUTE));
                }
                wishes.add(w);
                in.nextTag();
            } else if (in.getLocalName().equals(GoodsWish.getXMLElementTagName() + "WishListElement")) {
                Wish w = (Wish) getAIMain().getAIObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (w == null) {
                    w = new GoodsWish(getAIMain(), in.getAttributeValue(null, ID_ATTRIBUTE));
                }
                wishes.add(w);
                in.nextTag();
            } else if (in.getLocalName().equals(TileImprovementPlan.getXMLElementTagName() + "ListElement")) {
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
