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

package net.sf.freecol.server.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildQueue;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.See;


/**
 * The server version of a colony.
 */
public class ServerColony extends Colony implements ServerModelObject {

    private static final Logger logger = Logger.getLogger(ServerColony.class.getName());

    // Temporary variable:
    private int lastVisited;


    /**
     * Trivial constructor required for all ServerModelObjects.
     */
    public ServerColony(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new ServerColony.
     *
     * @param game The <code>Game</code> in which this object belongs.
     * @param owner The <code>Player</code> owning this <code>Colony</code>.
     * @param name The name of the new <code>Colony</code>.
     * @param tile The location of the <code>Colony</code>.
     */
    public ServerColony(Game game, Player owner, String name, Tile tile) {
        super(game, owner, name, tile);
        Specification spec = getSpecification();

        goodsContainer = new GoodsContainer(game, this);
        sonsOfLiberty = 0;
        oldSonsOfLiberty = 0;
        established = game.getTurn();
        tile.setOwner(owner);
        if (!tile.hasRoad()) {
            TileImprovement road
                = new TileImprovement(game, tile,
                    spec.getTileImprovementType("model.improvement.road"));
            road.setTurnsToComplete(0);
            road.setVirtual(true);
            tile.add(road);
        }

        ColonyTile colonyTile = new ServerColonyTile(game, this, tile);
        colonyTiles.add(colonyTile);
        for (Tile t : tile.getSurroundingTiles(getRadius())) {
            colonyTiles.add(new ServerColonyTile(game, this, t));
            if (t.getType().isWater()) {
                landLocked = false;
            }
        }
        // set up default production queues
        if (landLocked) {
            buildQueue.add(spec.getBuildingType("model.building.warehouse"));
        } else {
            buildQueue.add(spec.getBuildingType("model.building.docks"));
            getFeatureContainer().addAbility(HAS_PORT);
        }
        for (UnitType unitType : spec.getUnitTypesWithAbility("model.ability.bornInColony")) {
            if (!unitType.getGoodsRequired().isEmpty()) {
                populationQueue.add(unitType);
            }
        }

        Building building;
        List<BuildingType> buildingTypes = spec.getBuildingTypeList();
        for (BuildingType buildingType : buildingTypes) {
            if (buildingType.isAutomaticBuild()
                || isAutomaticBuild(buildingType)) {
                building = new ServerBuilding(getGame(), this, buildingType);
                addBuilding(building);
            }
        }

        lastVisited = -1;
    }


    /**
     * New turn for this colony.
     * Try to find out if the colony is going to survive (last colonist does
     * not starve) before generating lots of production-related messages.
     * TODO: use the warehouse to store things?
     *
     * @param random A <code>Random</code> number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csNewTurn(Random random, ChangeSet cs) {
        logger.finest("ServerColony.csNewTurn, for " + toString());
        ServerPlayer owner = (ServerPlayer) getOwner();
        Specification spec = getSpecification();

        boolean tileDirty = false;
        boolean colonyDirty = false;
        boolean newUnitBorn = false;
        List<FreeColGameObject> updates = new ArrayList<FreeColGameObject>();
        GoodsContainer container = getGoodsContainer();
        container.saveState();

        java.util.Map<Object, ProductionInfo> info
            = getProductionAndConsumption();
        TypeCountMap<GoodsType> netProduction = new TypeCountMap<GoodsType>();
        for (Entry<Object, ProductionInfo> entry : info.entrySet()) {
            ProductionInfo productionInfo = entry.getValue();
            if (entry.getKey() instanceof WorkLocation) {
                WorkLocation workLocation = (WorkLocation) entry.getKey();
                ((ServerModelObject) workLocation).csNewTurn(random, cs);
                if (workLocation.getUnitCount() > 0) {
                    for (AbstractGoods goods : productionInfo.getProduction()) {
                        UnitType expert = spec.getExpertForProducing(goods.getType());
                        int experience = goods.getAmount() / workLocation.getUnitCount();
                        for (Unit unit : workLocation.getUnitList()) {
                            if (goods.getType() == unit.getExperienceType()
                                && unit.getType().canBeUpgraded(expert, ChangeType.EDUCATION)) {
                                unit.setExperience(unit.getExperience() + experience);
                                cs.addPartial(See.only(owner), unit, "experience");
                            }
                        }
                    }
                }
                if (entry.getKey() instanceof Building) {
                    // TODO: generalize to other WorkLocations?
                    csCheckMissingInput((Building) entry.getKey(),
                                        productionInfo, cs);
                }
            } else if (entry.getKey() instanceof BuildQueue
                && !productionInfo.getConsumption().isEmpty()) {
                // this means we are actually building something
                BuildQueue queue = (BuildQueue) entry.getKey();
                BuildableType buildable = queue.getCurrentlyBuilding();
                if (buildable instanceof UnitType) {
                    Unit newUnit = csBuildUnit(queue, random, cs);
                    if (newUnit.hasAbility("model.ability.bornInColony")) {
                        newUnitBorn = true;
                    }
                    tileDirty = true;
                } else if (buildable instanceof BuildingType) {
                    colonyDirty = buildBuilding(queue, cs, updates);
                } else {
                    throw new IllegalStateException("Bogus buildable: " + buildable);
                }
                // Having removed something from the build queue, nudge it again
                // to see if there is a problem with the next item if any.
                buildable = csGetBuildable(cs);
            }

            for (AbstractGoods goods : productionInfo.getProduction()) {
                netProduction.incrementCount(goods.getType().getStoredAs(), goods.getAmount());
            }
            for (AbstractGoods goods : productionInfo.getStorage()) {
                netProduction.incrementCount(goods.getType().getStoredAs(), goods.getAmount());
            }
            for (AbstractGoods goods : productionInfo.getConsumption()) {
                netProduction.incrementCount(goods.getType().getStoredAs(), -goods.getAmount());
            }
        }

        // Apply the changes accumulated in the netProduction map.
        // Check for famine when total primary food goes negative.
        boolean famine = false;
        for (Entry<GoodsType, Integer> entry
                 : netProduction.getValues().entrySet()) {
            GoodsType goodsType = entry.getKey();
            int net = entry.getValue();
            int stored = getGoodsCount(goodsType);
            if (net + stored < 0) {
                if (goodsType == spec.getPrimaryFoodType()) famine = true;
                removeGoods(goodsType, stored);
            } else {
                addGoods(goodsType, net);
            }
        }

        // Handle the food situation
        if (famine) {
            if (getUnitCount() > 1) {
                Unit victim = Utils.getRandomMember(logger, "Choose starver",
                                                    getUnitList(), random);
                updates.add((FreeColGameObject) victim.getLocation());
                cs.addDispose(owner, this, victim);
                cs.addMessage(See.only(owner),
                              new ModelMessage(ModelMessage.MessageType.UNIT_LOST,
                                               "model.colony.colonistStarved",
                                               this)
                              .addName("%colony%", getName()));
            } else { // Its dead, Jim.
                cs.addMessage(See.only(owner),
                              new ModelMessage(ModelMessage.MessageType.UNIT_LOST,
                                               "model.colony.colonyStarved",
                                               this)
                              .addName("%colony%", getName()));
                cs.addDispose(owner, getTile(), this);
                return;
            }
        } else {
            int storedFood = getGoodsCount(spec.getPrimaryFoodType());
            int netFood = netProduction.getCount(spec.getPrimaryFoodType());
            int turns;
            if (!newUnitBorn && netFood < 0
                && (turns = storedFood / -netFood) <= 3) {
                cs.addMessage(See.only(owner),
                              new ModelMessage(ModelMessage.MessageType.WARNING,
                                               "model.colony.famineFeared",
                                               this)
                              .addName("%colony%", getName())
                              .addName("%number%", String.valueOf(turns)));
                logger.finest("Famine feared in " + getName()
                              + " food=" + storedFood
                              + " production=" + netFood
                              + " turns=" + turns);
            }
        }

        // Export goods if custom house is built.
        // Do not flush price changes yet, as any price change may change
        // yet again in csYearlyGoodsRemoval.
        if (hasAbility("model.ability.export")) {
            boolean gold = false;
            for (Goods goods : container.getCompactGoods()) {
                GoodsType type = goods.getType();
                ExportData data = getExportData(type);
                if (data.isExported()
                    && (owner.canTrade(goods, Market.Access.CUSTOM_HOUSE))) {
                    int amount = goods.getAmount() - data.getExportLevel();
                    if (amount > 0) {
                        owner.sell(container, type, amount, random);
                        gold = true;
                        cs.addMessage(See.only(owner),
                            new ModelMessage(ModelMessage.MessageType.GOODS_MOVEMENT,
                                             "customs.sale", this)
                            .addName("%colony%", getName())
                            .addName("%amount%", Integer.toString(amount))
                            .add("%goods%", type.getNameKey()));
                    }
                }
            }
            if (gold) {
                cs.addPartial(See.only(owner), owner, "gold");
            }
        }

        // Throw away goods there is no room for, and warn about
        // levels that will be exceeded next turn
        int limit = getWarehouseCapacity();
        int adjustment = limit / GoodsContainer.CARGO_SIZE;
        for (Goods goods : container.getCompactGoods()) {
            GoodsType type = goods.getType();
            if (!type.isStorable()) continue;
            ExportData exportData = getExportData(type);
            int low = exportData.getLowLevel() * adjustment;
            int high = exportData.getHighLevel() * adjustment;
            int amount = goods.getAmount();
            int oldAmount = container.getOldGoodsCount(type);

            if (amount < low && oldAmount >= low) {
                cs.addMessage(See.only(owner),
                    new ModelMessage(ModelMessage.MessageType.WAREHOUSE_CAPACITY,
                                     "model.building.warehouseEmpty",
                                     this, type)
                              .add("%goods%", type.getNameKey())
                              .addAmount("%level%", low)
                              .addName("%colony%", getName()));
                continue;
            }
            if (type.limitIgnored()) continue;

            String messageId = null;
            int waste = 0;
            if (amount > limit) {
                // limit has been exceeded
                waste = amount - limit;
                container.removeGoods(type, waste);
                messageId = "model.building.warehouseWaste";
            } else if (amount == limit && oldAmount < limit) {
                // limit has been reached during this turn
                messageId = "model.building.warehouseOverfull";
            } else if (amount > high && oldAmount <= high) {
                // high-water-mark has been reached this turn
                messageId = "model.building.warehouseFull";
            }
            if (messageId != null) {
                cs.addMessage(See.only(owner),
                              new ModelMessage(ModelMessage.MessageType.WAREHOUSE_CAPACITY,
                                               messageId, this, type)
                              .add("%goods%", type.getNameKey())
                              .addAmount("%waste%", waste)
                              .addAmount("%level%", high)
                              .addName("%colony%", getName()));
            }

            // No problem this turn, but what about the next?
            if (!(exportData.isExported()
                  && hasAbility("model.ability.export")
                  && owner.canTrade(type, Market.Access.CUSTOM_HOUSE))
                && amount <= limit) {
                int loss = amount + netProduction.getCount(type) - limit;
                if (loss > 0) {
                    cs.addMessage(See.only(owner),
                                  new ModelMessage(ModelMessage.MessageType.WAREHOUSE_CAPACITY,
                                                   "model.building.warehouseSoonFull",
                                                   this, type)
                                  .add("%goods%", goods.getNameKey())
                                  .addName("%colony%", getName())
                                  .addAmount("%amount%", loss));
                }
            }
        }

        // Check for free buildings
        for (BuildingType buildingType : spec.getBuildingTypeList()) {
            if (isAutomaticBuild(buildingType)) {
                addBuilding(new ServerBuilding(getGame(), this, buildingType));
            }
        }

        // Update SoL.
        updateSoL();
        if (sonsOfLiberty / 10 != oldSonsOfLiberty / 10) {
            cs.addMessage(See.only(owner),
                new ModelMessage(ModelMessage.MessageType.SONS_OF_LIBERTY,
                                 (sonsOfLiberty > oldSonsOfLiberty)
                                 ? "model.colony.SoLIncrease"
                                 : "model.colony.SoLDecrease",
                                 this, spec.getGoodsType("model.goods.bells"))
                          .addAmount("%oldSoL%", oldSonsOfLiberty)
                          .addAmount("%newSoL%", sonsOfLiberty)
                          .addName("%colony%", getName()));

            ModelMessage govMgtMessage = checkForGovMgtChangeMessage();
            if (govMgtMessage != null) {
                cs.addMessage(See.only(owner), govMgtMessage);
            }
        }
        updateProductionBonus();

        // Try to update minimally.
        if (tileDirty) {
            cs.add(See.perhaps(), getTile());
        } else {
            cs.add(See.only(owner), this);
        }
    }

    /**
     * Build a unit from a build queue.
     *
     * @param buildQueue The <code>BuildQueue</code> to find the unit in.
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     * @return The unit that was built.
     */
    private Unit csBuildUnit(BuildQueue buildQueue, Random random,
                             ChangeSet cs) {
        Unit unit = new ServerUnit(getGame(), getTile(), owner,
                                   (UnitType) buildQueue.getCurrentlyBuilding(),
                                   UnitState.ACTIVE);
        if (unit == null) return null;
        if (unit.hasAbility("model.ability.bornInColony")) {
            cs.addMessage(See.only((ServerPlayer) owner),
                          new ModelMessage(ModelMessage.MessageType.UNIT_ADDED,
                                           "model.colony.newColonist",
                                           this, unit)
                          .addName("%colony%", getName()));
            if (buildQueue.size() > 1) {
                Collections.shuffle(buildQueue.getValues(), random);
            }
        } else {
            cs.addMessage(See.only((ServerPlayer) owner),
                          new ModelMessage(ModelMessage.MessageType.UNIT_ADDED,
                                           "model.colony.unitReady",
                                           this, unit)
                          .addName("%colony%", getName())
                          .addStringTemplate("%unit%", unit.getLabel()));
            // Remove the unit-to-build unless it is the last entry.
            if (buildQueue.size() > 1) buildQueue.remove(0);
        }

        logger.info("New unit created in " + getName() + ": " + unit);
        return unit;
    }

    /**
     * Check a building to see if it is missing input.
     *
     * @param building The <code>Building</code> to check.
     * @param pi The <code>ProductionInfo</code> for the building.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csCheckMissingInput(Building building, ProductionInfo pi,
                                     ChangeSet cs) {
        // Can not happen if the building needs no input or no one is
        // working there.
        if (building.canAutoProduce() || building.getUnitCount() <= 0) return;

        // Check all goods types produced by this building.  If the
        // output for a type is zero and the maximum production is
        // non-zero then the building input type must be missing, Emit
        // a message and return avoiding possible multiple messages
        // per building.
        for (AbstractGoods goods : pi.getProduction()) {
            if (goods.getAmount() > 0) continue;
            GoodsType type = goods.getType();
            for (AbstractGoods g : pi.getMaximumProduction()) {
                if (g.getType() != type) continue;
                if (goods.getAmount() >= 0) {
                    type = building.getGoodsInputType();
                    cs.addMessage(See.only((ServerPlayer) owner),
                        new ModelMessage(ModelMessage.MessageType.MISSING_GOODS,
                                         "model.building.notEnoughInput",
                                         this, type)
                                  .add("%inputGoods%", type.getNameKey())
                                  .add("%building%", building.getNameKey())
                                  .addName("%colony%", getName()));
                    return;
                }
                break;
            }
        }
    }


    private boolean buildBuilding(BuildQueue buildQueue, ChangeSet cs, List<FreeColGameObject> updates) {
        BuildingType type = (BuildingType) buildQueue.getCurrentlyBuilding();
        BuildingType from = type.getUpgradesFrom();
        boolean success;
        boolean colonyDirty = false;
        if (from == null) {
            addBuilding(new ServerBuilding(getGame(), this, type));
            colonyDirty = true;
            success = true;
        } else {
            Building building = getBuilding(from);
            if (building.upgrade()) {
                updates.add(building);
                success = true;
            } else {
                cs.addMessage(See.only((ServerPlayer) owner),
                              new ModelMessage(ModelMessage.MessageType.BUILDING_COMPLETED,
                                               "colonyPanel.unbuildable",
                                               this)
                              .addName("%colony%", getName())
                              .add("%object%", type.getNameKey()));
                success = false;
            }
        }
        if (success) {
            tile.updatePlayerExploredTiles(); // See stockade changes
            cs.addMessage(See.only((ServerPlayer) owner),
                          new ModelMessage(ModelMessage.MessageType.BUILDING_COMPLETED,
                                           "model.colony.buildingReady",
                                           this)
                          .addName("%colony%", getName())
                          .add("%building%", type.getNameKey()));
            if (buildQueue.size() == 1) {
                cs.addMessage(See.only((ServerPlayer) owner),
                              new ModelMessage(ModelMessage.MessageType.BUILDING_COMPLETED,
                                               "model.colony.notBuildingAnything",
                                               this)
                              .addName("%colony%", getName())
                              .add("%building%", type.getNameKey()));
            }
        }
        buildQueue.remove(0);
        return colonyDirty;
    }


    /**
     * Gets what this colony really is building, removing anything that
     * is currently impossible.
     *
     * @param cs A <code>ChangeSet</code> to update.
     * @return A buildable that can be built, or null if nothing.
     */
    private BuildableType csGetBuildable(ChangeSet cs) {
        ServerPlayer owner = (ServerPlayer) getOwner();
        Specification spec = getSpecification();

        while (!buildQueue.isEmpty()) {
            BuildableType buildable = buildQueue.getCurrentlyBuilding();
            switch (getNoBuildReason(buildable)) {
            case NONE:
                return buildable;
            case NOT_BUILDING:
                for (GoodsType goodsType : spec.getGoodsTypeList()) {
                    if (goodsType.isBuildingMaterial()
                        && !goodsType.isStorable()
                        && getProductionOf(goodsType) > 0) {
                        // Production is idle
                        cs.addMessage(See.only(owner),
                                      new ModelMessage(ModelMessage.MessageType.WARNING,
                                                       "model.colony.cannotBuild",
                                                       this)
                                      .addName("%colony%", getName()));
                    }
                }
                return null;
            case POPULATION_TOO_SMALL:
                cs.addMessage(See.only(owner),
                              new ModelMessage(ModelMessage.MessageType.WARNING,
                                               "model.colony.buildNeedPop",
                                               this)
                              .addName("%colony%", getName())
                              .add("%building%", buildable.getNameKey()));
                break;
            default: // Are there other warnings to send?
                cs.addMessage(See.only(owner),
                              new ModelMessage(ModelMessage.MessageType.WARNING,
                                               "colonyPanel.unbuildable",
                                               this, buildable)
                              .addName("%colony%", getName())
                              .add("%object%", buildable.getNameKey()));
                break;
            }
            buildQueue.remove(0);
        }
        return null;
    }

    /**
     * Are all the requirements to complete a buildable satisfied?
     *
     * @param buildable The <code>Buildable</code> to check.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private boolean csHasAllRequirements(BuildableType buildable,
                                         ChangeSet cs) {
        ServerPlayer owner = (ServerPlayer) getOwner();
        GoodsContainer container = getGoodsContainer();
        // Check availability of goods required for construction
        ArrayList<ModelMessage> messages = new ArrayList<ModelMessage>();
        for (AbstractGoods required : buildable.getGoodsRequired()) {
            GoodsType type = required.getType();
            int available = container.getGoodsCount(type);
            if (available < required.getAmount()) {
                if (type.isStorable()) {
                    int need = required.getAmount() - available;
                    messages.add(new ModelMessage(ModelMessage.MessageType.MISSING_GOODS,
                                                  "model.colony.buildableNeedsGoods",
                                                  this, type)
                                 .addName("%colony%", getName())
                                 .add("%buildable%", buildable.getNameKey())
                                 .addName("%amount%", String.valueOf(need))
                                 .add("%goodsType%", type.getNameKey()));
                } else {
                    // Not complete due to missing unstorable goods
                    // (probably hammers) so there is no point griping.
                    return false;
                }
            }
        }
        if (!messages.isEmpty()) {
            // Not complete due to missing storable goods.
            // Gripe away.
            for (ModelMessage message : messages) {
                cs.addMessage(See.only(owner), message);
            }
            return false;
        }
        return true;
    }

    /**
     * Evict the users from a tile used by this colony, due to military
     * action from another unit.
     *
     * @param enemyUnit The <code>Unit</code> that has moved in.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csEvictUser(Unit enemyUnit, ChangeSet cs) {
        ServerPlayer serverPlayer = (ServerPlayer) getOwner();
        Tile tile = enemyUnit.getTile();
        ServerColonyTile ct = (ServerColonyTile) getColonyTile(tile);
        Unit unit;
        if (ct == null || (unit = ct.getUnit()) == null) return;
        Tile centerTile = getTile();

        unit.setState(UnitState.ACTIVE);
        unit.setLocation(centerTile);
        cs.add(See.only(serverPlayer), ct);
        cs.add(See.perhaps(), centerTile);
        cs.addMessage(See.only(serverPlayer),
                      new ModelMessage(ModelMessage.MessageType.WARNING,
                                       "model.colony.workerEvicted",
                                       this, this)
                      .addName("%colony%", getName())
                      .addStringTemplate("%unit%", unit.getLabel())
                      .addStringTemplate("%location%", tile.getLocationName())
                      .addStringTemplate("%enemyUnit%", enemyUnit.getLabel()));
    }


    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "serverColony"
     */
    public String getServerXMLElementTagName() {
        return "serverColony";
    }
}
