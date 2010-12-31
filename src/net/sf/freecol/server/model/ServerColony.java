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

package net.sf.freecol.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.ChangePriority;
import net.sf.freecol.server.control.ChangeSet.See;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerColonyTile;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerModelObject;


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
        goodsContainer.addPropertyChangeListener(this);
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
        if (landLocked) {
            buildQueue.add(spec.getBuildingType("model.building.warehouse"));
        } else {
            buildQueue.add(spec.getBuildingType("model.building.docks"));
            getFeatureContainer().addAbility(HAS_PORT);
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
        List<FreeColGameObject> updates = new ArrayList<FreeColGameObject>();
        GoodsContainer container = getGoodsContainer();
        container.saveState();

        // Update the colony tiles (hopefully producing food).
        for (ColonyTile colonyTile : colonyTiles) {
            ((ServerModelObject) colonyTile).csNewTurn(random, cs);
        }

        // Categorize buildings as {food, materials, other}-producers
        // To determine materials, examine the requirements for the
        // current building if any.
        List<GoodsType> forBuilding = new ArrayList<GoodsType>();
        BuildableType buildable = csGetBuildable(cs);
        if (buildable != null) {
            for (AbstractGoods ag : buildable.getGoodsRequired()) {
                forBuilding.add(ag.getType());
            }
        }
        List<Building> forFood = new ArrayList<Building>();
        List<Building> forMaterials = new ArrayList<Building>();
        List<Building> foodConsumers = new ArrayList<Building>();
        List<Building> forOther = new ArrayList<Building>();
        for (Building building : getBuildings()) {
            GoodsType outputType = building.getGoodsOutputType();
            GoodsType inputType = building.getGoodsInputType();
            if (outputType == null) {
                forOther.add(building);
            } else if (outputType.isFoodType()) {
                forFood.add(building);
            } else if (inputType != null && inputType.isFoodType()) {
                foodConsumers.add(building);
            } else if (forBuilding.contains(outputType)) {
                forMaterials.add(building);
            } else {
                int index = -1;
                for (int i = 0; i < forOther.size(); i++) {
                    GoodsType input = forOther.get(i).getGoodsInputType();
                    if (outputType.equals(input)) {
                        index = i;
                        break;
                    }
                }
                if (index < 0) {
                    forOther.add(building);
                } else { // insert before consumer
                    forOther.add(index, building);
                }
            }
        }

        // Update the food producers (none in the standard rule set).
        for (Building building : forFood) {
            ((ServerModelObject) building).csNewTurn(random, cs);
        }

        int foodRequired = getFoodConsumption();
        if (!foodConsumers.isEmpty()) {
            // check for surplus available to produce goods from food
            // types (e.g. horses)
            int surplus = 0;
            for (Goods goods : container.getCompactGoods()) {
                if (goods.getType().isFoodType()) {
                    surplus += (goods.getAmount() - container.getOldGoodsCount(goods.getType()));
                }
            }
            surplus -= foodRequired;
            System.out.println("----------------------------------------");
            System.out.println("Food surplus in " + getName() + " is " + surplus);

            if (surplus > 0) {
                // TODO: make more generic
                surplus = Math.max(0, surplus - storedSurplus(spec.getPrimaryFoodType(), surplus));
                System.out.println("Surplus after storage is " + surplus);
            }

            if (surplus > 0) {
                for (Building building : foodConsumers) {
                    GoodsType inputType = building.getGoodsInputType();
                    GoodsType outputType = building.getGoodsOutputType();
                    System.out.println("Food consumer is " + building.getType().getId());
                    int inputCount = getGoodsCount(inputType);
                    int outputCount = getGoodsCount(outputType);
                    System.out.println("Goods input is " + Math.min(inputCount, surplus)
                                       + " " + inputType.getId());
                    ((ServerBuilding) building).csNewTurn(random, cs, Math.min(inputCount, surplus));
                    surplus -= inputCount - getGoodsCount(inputType);
                    System.out.println("Produced " + (getGoodsCount(outputType) - outputCount)
                                       + " " + outputType);
                    System.out.println("Surplus is now " + surplus);
                    if (surplus < 0) {
                        logger.warning("Building " + building.getId() + " consumed more than allowed!");
                        break;
                    } else if (surplus == 0) {
                        break;
                    }
                }
            }
        }

        // convert all food types to food (or whatever)
        for (Goods goods : container.getCompactGoods()) {
            GoodsType goodsType = goods.getType();
            if (goodsType.isFoodType() && goodsType.isStoredAs()) {
                container.addGoods(goodsType.getStoredAs(), goods.getAmount());
                container.removeGoods(goods);
            }
        }

        // All food should be produced, so now check for starvation,
        // and hence whether the colony will survive.
        int foodAvailable = getFoodCount();
        if (foodRequired > foodAvailable) { // Someone starves.
            removeFood(foodAvailable);
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
            removeFood(foodRequired);
            int production = getFoodProduction();
            if (foodRequired > production){
                int turns = (foodAvailable - foodRequired)
                    / (foodRequired - production);
                if (turns <= 3) {
                    cs.addMessage(See.only(owner),
                        new ModelMessage(ModelMessage.MessageType.WARNING,
                                         "model.colony.famineFeared",
                                         this)
                                  .addName("%colony%", getName())
                                  .addName("%number%", String.valueOf(turns)));
                }
            }
        }

        // Now produce the materials.
        for (Building building : forMaterials) {
            ((ServerModelObject) building).csNewTurn(random, cs);
        }

        // Now that materials are present, check if the buildable is
        // complete or blocked by absence of a storable good.  If
        // complete, then complete it.
        if (buildable != null
            && csHasAllRequirements(buildable, cs)) {
            // TODO: why do we need this? how can this be called twice?
            if (lastVisited == getGame().getTurn().getNumber()) {
                throw new IllegalStateException("Double call!");
            }
            lastVisited = getGame().getTurn().getNumber();

            // Consume the goods.
            // Waste excess goods if not storable or overflow allowed.
            boolean overflow
                = spec.getBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW);
            for (AbstractGoods required : buildable.getGoodsRequired()) {
                if (overflow || required.getType().isStorable()) {
                    removeGoods(required);
                } else {
                    removeGoods(required.getType());
                }
            }

            // Create the buildable.
            if (buildable instanceof UnitType) {
                Unit unit = new ServerUnit(getGame(), getTile(), owner,
                                           (UnitType) buildable,
                                           UnitState.ACTIVE);
                cs.addMessage(See.only(owner),
                    new ModelMessage(ModelMessage.MessageType.UNIT_ADDED,
                                     "model.colony.unitReady",
                                     this, unit)
                              .addName("%colony%", getName())
                              .addStringTemplate("%unit%", unit.getLabel()));
                // Remove the unit-to-build unless it is the last entry.
                if (buildQueue.size() > 1) buildQueue.remove(0);
                tileDirty = true;

            } else if (buildable instanceof BuildingType) {
                BuildingType type = (BuildingType) buildable;
                BuildingType from = type.getUpgradesFrom();
                boolean success;
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
                        cs.addMessage(See.only(owner),
                            new ModelMessage(ModelMessage.MessageType.BUILDING_COMPLETED,
                                     "colonyPanel.unbuildable",
                                     this)
                              .addName("%colony%", getName())
                              .add("%object%", buildable.getNameKey()));
                        success = false;
                    }
                }
                if (success) {
                    tile.updatePlayerExploredTiles(); // See stockade changes
                    cs.addMessage(See.only(owner),
                        new ModelMessage(ModelMessage.MessageType.BUILDING_COMPLETED,
                                         "model.colony.buildingReady",
                                         this)
                                  .addName("%colony%", getName())
                                  .add("%building%", buildable.getNameKey()));
                    if (buildQueue.size() == 1) {
                        cs.addMessage(See.only(owner),
                            new ModelMessage(ModelMessage.MessageType.BUILDING_COMPLETED,
                                             "model.colony.notBuildingAnything",
                                             this)
                                      .addName("%colony%", getName())
                                      .add("%building%", buildable.getNameKey()));
                    }
                }
                buildQueue.remove(0);
            } else {
                throw new IllegalStateException("Bogus buildable: "
                                                + buildable);
            }

            // Having removed something from the build queue, nudge it again
            // to see if there is a problem with the next item if any.
            buildable = csGetBuildable(cs);
        } else {
            if (buildQueue.size() == 0) {
                cs.addMessage(See.only(owner),
                    new ModelMessage(ModelMessage.MessageType.BUILDING_COMPLETED,
                                     "model.colony.notBuildingAnything",
                                     this)
                              .addName("%colony%", getName()));
            }
        }

        // The other buildings that do not produce building materials can
        // run now.
        for (Building building : forOther) {
            ((ServerModelObject) building).csNewTurn(random, cs);
        }

        // Now that other production which might consume food (e.g. horses)
        // has been done, check for new colonists.
        List<UnitType> colonyTypes
            = spec.getUnitTypesWithAbility("model.ability.bornInColony");
        if (getFoodCount() >= FOOD_PER_COLONIST && !colonyTypes.isEmpty()) {
            UnitType type = Utils.getRandomMember(logger, "Choose birth",
                                                  colonyTypes, random);
            Unit unit = new ServerUnit(getGame(), getTile(), owner, type,
                                       UnitState.ACTIVE);
            removeFood(FOOD_PER_COLONIST);
            cs.addMessage(See.only(owner),
                new ModelMessage(ModelMessage.MessageType.UNIT_ADDED,
                                 "model.colony.newColonist",
                                 this, unit)
                          .addName("%colony%", getName()));
            logger.info("New colonist created in " + getName()
                        + " with ID=" + unit.getId());
            tileDirty = true;
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
            String messageId = null;
            int level = 0;
            int waste = 0;
            if (!type.limitIgnored()) {
                if (amount > limit) { // limit has been exceeded
                    waste = amount - limit;
                    container.removeGoods(type, waste);
                    messageId = "model.building.warehouseWaste";
                } else if (amount == limit && oldAmount < limit) {
                    // limit has been reached during this turn
                    messageId = "model.building.warehouseOverfull";
                } else if (amount > high && oldAmount <= high) {
                    messageId = "model.building.warehouseFull";
                    level = high;
                }
            }
            if (amount < low && oldAmount >= low) {
                messageId = "model.building.warehouseEmpty";
                level = low;
            }
            if (messageId != null) {
                cs.addMessage(See.only(owner),
                    new ModelMessage(ModelMessage.MessageType.WAREHOUSE_CAPACITY,
                                     messageId, this, type)
                              .add("%goods%", type.getNameKey())
                              .addAmount("%waste%", waste)
                              .addAmount("%level%", level)
                              .addName("%colony%", getName()));
                continue;
            }

            // No problem this turn, but what about the next?
            if (!type.limitIgnored()
                && !(exportData.isExported()
                     && owner.canTrade(type, Market.Access.CUSTOM_HOUSE))
                && amount <= limit
                && amount + getProductionNetOf(type) > limit) {
                int lose = amount + getProductionNetOf(type) - limit;
                cs.addMessage(See.only(owner),
                    new ModelMessage(ModelMessage.MessageType.WAREHOUSE_CAPACITY,
                                     "model.building.warehouseSoonFull",
                                     this, type)
                              .add("%goods%", goods.getNameKey())
                              .addName("%colony%", getName())
                              .addName("%amount%", String.valueOf(lose)));
            }
        }

        // Remove goods consumed by the colonists (except food, which
        // has already been handled). In the future, colonists might
        // consume luxury goods, for example
        for (GoodsType goodsType : spec.getGoodsTypeList()) {
            if (!goodsType.isFoodType()) {
                removeGoods(goodsType, getConsumptionOf(goodsType));
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
     * Returns the tag name of the root element representing this object.
     *
     * @return "serverColony"
     */
    public String getServerXMLElementTagName() {
        return "serverColony";
    }
}
