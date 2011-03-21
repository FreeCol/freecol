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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.TradeRoute.Stop;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.See;


/**
 * Server version of a unit.
 *
 */
public class ServerUnit extends Unit implements ServerModelObject {

    private static final Logger logger = Logger.getLogger(ServerUnit.class.getName());


    /**
     * Trivial constructor required for all ServerModelObjects.
     */
    public ServerUnit(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new ServerUnit.
     *
     * @param game The <code>Game</code> in which this unit belongs.
     * @param location The <code>Location</code> to place this at.
     * @param owner The <code>Player</code> owning this unit.
     * @param type The type of the unit.
     * @param state The initial state for this unit.
     * @param initialEquipment The list of initial EquimentTypes
     */
    public ServerUnit(Game game, Location location, Player owner,
                      UnitType type, UnitState state) {
        this(game, location, owner, type, state, type.getDefaultEquipment());
    }

    /**
     * Creates a new ServerUnit.
     *
     * @param game The <code>Game</code> in which this unit belongs.
     * @param location The <code>Location</code> to place this at.
     * @param owner The <code>Player</code> owning this unit.
     * @param type The type of the unit.
     * @param state The initial state for this unit.
     * @param initialEquipment The list of initial EquimentTypes
     */
    public ServerUnit(Game game, Location location, Player owner,
                      UnitType type, UnitState state,
                      EquipmentType... initialEquipment) {
        super(game);

        visibleGoodsCount = -1;

        if (type.canCarryGoods()) {
            goodsContainer = new GoodsContainer(game, this);
        }

        UnitType newType = type.getTargetType(ChangeType.CREATION, owner);
        unitType = (newType == null) ? type : newType;
        this.owner = owner;
        owner.getNationID();
        naval = unitType.hasAbility("model.ability.navalUnit");
        setLocation(location);

        workLeft = -1;
        workType = getSpecification().getPrimaryFoodType();

        this.movesLeft = getInitialMovesLeft();
        hitpoints = unitType.getHitPoints();

        for (EquipmentType equipmentType : initialEquipment) {
            if (EquipmentType.NO_EQUIPMENT.equals(equipmentType)) {
                equipment.clear();
                break;
            }
            equipment.incrementCount(equipmentType, 1);
        }
        setRole();
        setStateUnchecked(state);

        owner.setUnit(this);
        owner.invalidateCanSeeTiles();
        owner.modifyScore(unitType.getScoreValue());
    }


    /**
     * New turn for this unit.
     *
     * @param random A <code>Random</code> number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csNewTurn(Random random, ChangeSet cs) {
        logger.finest("ServerUnit.csNewTurn, for " + toString());
        ServerPlayer owner = (ServerPlayer) getOwner();
        Specification spec = getSpecification();
        Location loc = getLocation();
        boolean tileDirty = false;
        boolean unitDirty = false;

        // Check for experience-promotion.
        GoodsType produce;
        UnitType learn;
        if (loc instanceof WorkLocation
            && (produce = getWorkType()) != null
            && (learn = spec.getExpertForProducing(produce)) != null
            && learn != getType()
            && getType().canBeUpgraded(learn, ChangeType.EXPERIENCE)) {
            int maximumExperience = getType().getMaximumExperience();
            int maxValue = (100 * maximumExperience) /
                getType().getUnitTypeChange(learn).getProbability(ChangeType.EXPERIENCE);
            if (maxValue > 0
                && Utils.randomInt(logger, "Experience", random, maxValue)
                < Math.min(getExperience(), maximumExperience)) {
                StringTemplate oldName = getLabel();
                setType(learn);
                cs.addMessage(See.only(owner),
                              new ModelMessage(ModelMessage.MessageType.UNIT_IMPROVED,
                                               "model.unit.experience",
                                               getColony(), this)
                              .addStringTemplate("%oldName%", oldName)
                              .addStringTemplate("%unit%", getLabel())
                              .addName("%colony%", getColony().getName()));
                logger.finest("Experience upgrade for unit " + getId()
                              + " to " + getType());
                unitDirty = true;
            }
        }

        // Attrition
        if (loc instanceof Tile && ((Tile) loc).getSettlement() == null) {
            int attrition = getAttrition() + 1;
            setAttrition(attrition);
            if (attrition > getType().getMaximumAttrition()) {
                cs.addMessage(See.only(owner),
                    new ModelMessage(ModelMessage.MessageType.UNIT_LOST,
                                     "model.unit.attrition", this)
                              .addStringTemplate("%unit%", getLabel()));
                cs.addDispose(owner, loc, this);
            }
        } else {
            setAttrition(0);
        }

        setMovesLeft((isUnderRepair() || isInMission()) ? 0
                     : getInitialMovesLeft());

        if (getWorkLeft() > 0) {
            unitDirty = true;
            switch (getState()) {
            case IMPROVING:
                // Has the improvement been completed already? Do nothing.
                TileImprovement ti = getWorkImprovement();
                if (ti.isComplete()) {
                    setState(UnitState.ACTIVE);
                    setWorkLeft(-1);
                } else {
                    // Otherwise do work
                    int amount = (getType().hasAbility("model.ability.expertPioneer"))
                        ? 2 : 1;
                    int turns = ti.getTurnsToComplete();
                    if ((turns -= amount) < 0) turns = 0;
                    ti.setTurnsToComplete(turns);
                    setWorkLeft(turns);
                }
                break;
            case TO_AMERICA:
                if (getOwner().isREF()) { // Swift travel to America for the REF
                    setWorkLeft(0);
                    break;
                }
                // Fall through
            default:
                setWorkLeft(getWorkLeft() - 1);
                break;
            }
        }

        if (getWorkLeft() == 0) tileDirty |= csCompleteWork(random, cs);

        if (getState() == UnitState.SKIPPED) {
            setState(UnitState.ACTIVE);
            unitDirty = true;
        }

        if (tileDirty) {
            cs.add(See.perhaps(), getTile());
        } else if (unitDirty) {
            cs.add(See.perhaps(), this);
        } else {
            cs.addPartial(See.only(owner), this, "movesLeft");
        }
    }

    /**
     * Complete the work a unit is doing.
     *
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     * @return True if the tile under the unit needs an update.
     */
    private boolean csCompleteWork(Random random, ChangeSet cs) {
        setWorkLeft(-1);

        switch (getState()) {
        case TO_EUROPE:
            logger.info(toString() + " arrives in Europe");
            if (getTradeRoute() != null) {
                setMovesLeft(0);
                setState(UnitState.ACTIVE);
                return false;
            }
            ServerPlayer owner = (ServerPlayer) getOwner();
            Europe europe = owner.getEurope();
            if (getDestination() == europe) setDestination(null);
            cs.addMessage(See.only(owner),
                          new ModelMessage(ModelMessage.MessageType.DEFAULT,
                                           "model.unit.arriveInEurope",
                                           europe, this)
                          .add("%europe%", europe.getNameKey()));
            setState(UnitState.ACTIVE);
            break;
        case TO_AMERICA:
            logger.info(toString() + " arrives in America");
            csMove(getVacantEntryLocation(random), random, cs);
            break;
        case FORTIFYING:
            setState(UnitState.FORTIFIED);
            break;
        case IMPROVING:
            csImproveTile(random, cs);
            return true;
        default:
            logger.warning("Unknown work completed, state=" + getState());
            setState(UnitState.ACTIVE);
            break;
        }
        return false;
    }

    /**
     * Completes a tile improvement.
     *
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csImproveTile(Random random, ChangeSet cs) {
        Tile tile = getTile();
        GoodsType deliver = getWorkImprovement().getDeliverGoodsType();
        if (deliver != null) { // Deliver goods if any
            int amount = tile.potential(deliver, getType())
                * getWorkImprovement().getDeliverAmount();
            if (getType().hasAbility("model.ability.expertPioneer")) {
                amount *= 2;
            }
            Settlement settlement = tile.getSettlement();
            if (settlement != null
                && (ServerPlayer) settlement.getOwner() == owner) {
                settlement.addGoods(deliver, amount);
            } else {
                List<Settlement> adjacent = new ArrayList<Settlement>();
                for (Tile t : tile.getSurroundingTiles(1)) {
                    if (t.getSettlement() != null
                        && (ServerPlayer) t.getSettlement().getOwner()
                        == owner) {
                        adjacent.add(t.getSettlement());
                    }
                }
                if (adjacent.size() > 0) {
                    int deliverPerCity = amount / adjacent.size();
                    for (Settlement s : adjacent) {
                        s.addGoods(deliver, deliverPerCity);
                    }
                    // Add residue to first adjacent settlement.
                    adjacent.get(0).addGoods(deliver,
                                             amount % adjacent.size());
                }
            }
        }

        // Finish up
        TileImprovement ti = getWorkImprovement();
        TileType changeType = ti.getChange(tile.getType());
        if (changeType != null) {
            // Changes like clearing a forest need to be completed,
            // whereas for changes like road building the improvement
            // is already added and now complete.
            tile.setType(changeType);
        }

        // Does a resource get exposed?
        TileImprovementType tileImprovementType = ti.getType();
        int exposeResource = tileImprovementType.getExposeResourcePercent();
        if (exposeResource > 0 && !tile.hasResource()) {
            if (Utils.randomInt(logger, "Expose resource", random, 100)
                < exposeResource) {
                ResourceType resType = RandomChoice.getWeightedRandom(logger,
                        "Resource type", random,
                        tile.getType().getWeightedResources());
                int minValue = resType.getMinValue();
                int maxValue = resType.getMaxValue();
                int value = minValue + ((minValue == maxValue) ? 0
                        : Utils.randomInt(logger, "Resource quantity",
                                          random,
                                          maxValue - minValue + 1));
                tile.addResource(new Resource(getGame(), tile, resType, value));
            }
        }

        // Expend equipment
        EquipmentType type = ti.getExpendedEquipmentType();
        changeEquipment(type, -ti.getExpendedAmount());
        for (Unit unit : tile.getUnitList()) {
            if (unit.getWorkImprovement() != null
                && unit.getWorkImprovement().getType() == ti.getType()
                && unit.getState() == UnitState.IMPROVING) {
                unit.setWorkLeft(-1);
                unit.setWorkImprovement(null);
                unit.setState(UnitState.ACTIVE);
                unit.setMovesLeft(0);
            }
        }
        // TODO: make this more generic, currently assumes tools used
        EquipmentType tools = getSpecification()
            .getEquipmentType("model.equipment.tools");
        if (type == tools && getEquipmentCount(tools) == 0) {
            ServerPlayer owner = (ServerPlayer) getOwner();
            StringTemplate locName
                = getLocation().getLocationNameFor(owner);
            String messageId = (getType().getDefaultEquipmentType() == type)
                ? getType() + ".noMoreTools"
                : "model.unit.noMoreTools";
            cs.addMessage(See.only(owner),
                          new ModelMessage(ModelMessage.MessageType.WARNING,
                                           messageId, this)
                          .addStringTemplate("%unit%", getLabel())
                          .addStringTemplate("%location%", locName));
        }
    }

    /**
     * Repair a unit.
     *
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csRepairUnit(ChangeSet cs) {
        ServerPlayer owner = (ServerPlayer) getOwner();
        setHitpoints(getHitpoints() + 1);
        if (!isUnderRepair()) {
            Location loc = getLocation();
            cs.addMessage(See.only(owner),
                new ModelMessage("model.unit.unitRepaired",
                                 this, (FreeColGameObject) loc)
                          .addStringTemplate("%unit%", getLabel())
                          .addStringTemplate("%repairLocation%",
                                             loc.getLocationNameFor(owner)));
        }
        cs.addPartial(See.only(owner), this, "hitpoints");
    }

    /**
     * Finds a suitable tile to put this unit on return from Europe.
     * If this unit has not not been outside Europe before, it will
     * return the default value from the owner.
     *
     * @param random A pseudo-random number source.
     * @return A suitable entry location for this unit.
     * @see #getEntryLocation
     */
    private Tile getVacantEntryLocation(Random random) {
        Tile tile = getFullEntryLocation();
        if (tile.getFirstUnit() == null
            || tile.getFirstUnit().getOwner() == getOwner()) return tile;

        Map map = getGame().getMap();
        for (int r = 1; true; r++) {
            List<Tile> tiles = tile.getSurroundingTiles(r, r);
            Collections.shuffle(tiles, random);
            for (Tile t : tiles) {
                if (t.getFirstUnit() == null
                    || t.getFirstUnit().getOwner() == getOwner()) {
                    return t;
                }
            }
        }
    }

    /**
     * If a unit moves, check if an opposing naval unit slows it down.
     * Note that the unit moves are reduced here.
     *
     * @param newTile The <code>Tile</code> the unit is moving to.
     * @param random A pseudo-random number source.
     * @return Either an enemy unit that causes a slowdown, or null if none.
     */
    private Unit getSlowedBy(Tile newTile, Random random) {
        Player player = getOwner();
        Game game = getGame();
        CombatModel combatModel = game.getCombatModel();
        boolean pirate = hasAbility("model.ability.piracy");
        Unit attacker = null;
        float attackPower = 0, totalAttackPower = 0;

        if (!isNaval() || getMovesLeft() <= 0) return null;
        for (Tile tile : newTile.getSurroundingTiles(1)) {
            // Ships in settlements do not slow enemy ships, but:
            // TODO should a fortress slow a ship?
            Player enemy;
            if (tile.isLand()
                || tile.getColony() != null
                || tile.getFirstUnit() == null
                || (enemy = tile.getFirstUnit().getOwner()) == player) continue;
            for (Unit enemyUnit : tile.getUnitList()) {
                if ((pirate || enemyUnit.hasAbility("model.ability.piracy")
                     || (enemyUnit.isOffensiveUnit() && player.atWarWith(enemy)))
                    && enemyUnit.isNaval()
                    && combatModel.getOffencePower(enemyUnit, this) > attackPower) {
                    attackPower = combatModel.getOffencePower(enemyUnit, this);
                    totalAttackPower += attackPower;
                    attacker = enemyUnit;
                }
            }
        }
        if (attacker != null) {
            float defencePower = combatModel.getDefencePower(attacker, this);
            float totalProbability = totalAttackPower + defencePower;
            if (Utils.randomInt(logger, "Slowed", random,
                                Math.round(totalProbability) + 1)
                < totalAttackPower) {
                int diff = Math.max(0, Math.round(totalAttackPower - defencePower));
                int moves = Math.min(9, 3 + diff / 3);
                setMovesLeft(getMovesLeft() - moves);
                logger.info(getId() + " slowed by " + attacker.getId()
                            + " by " + Integer.toString(moves) + " moves.");
            } else {
                attacker = null;
            }
        }
        return attacker;
    }

    /**
     * Explores a lost city, finding a native burial ground.
     *
     * @param cs A <code>ChangeSet</code> to add changes to.
     */
    private void csNativeBurialGround(ChangeSet cs) {
        ServerPlayer serverPlayer = (ServerPlayer) getOwner();
        Tile tile = getTile();
        Player indianPlayer = tile.getOwner();
        cs.add(See.only(serverPlayer),
               indianPlayer.modifyTension(serverPlayer, Tension.Level.HATEFUL.getLimit()));
        cs.add(See.only(serverPlayer), indianPlayer);
        cs.addMessage(See.only(serverPlayer),
            new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                             "lostCityRumour.BurialGround",
                             serverPlayer, this)
                .addStringTemplate("%nation%", indianPlayer.getNationName()));
    }

    /**
     * Explore a lost city.
     *
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to add changes to.
     */
    private void csExploreLostCityRumour(Random random, ChangeSet cs) {
        ServerPlayer serverPlayer = (ServerPlayer) getOwner();
        Tile tile = getTile();
        LostCityRumour lostCity = tile.getLostCityRumour();
        if (lostCity == null) return;

        Game game = getGame();
        Specification spec = game.getSpecification();
        int difficulty = spec.getInteger("model.option.rumourDifficulty");
        int dx = 10 - difficulty;
        UnitType unitType;
        Unit newUnit = null;
        List<UnitType> treasureUnitTypes
            = spec.getUnitTypesWithAbility("model.ability.carryTreasure");

        RumourType rumour = lostCity.getType();
        if (rumour == null) {
            rumour = lostCity.chooseType(this, difficulty, random);
        }
        // Filter out failing cases that could only occur if the
        // type was explicitly set in debug mode.
        switch (rumour) {
        case BURIAL_GROUND: case MOUNDS:
            if (tile.getOwner() == null || !tile.getOwner().isIndian()) {
                rumour = RumourType.NOTHING;
            }
            break;
        case LEARN:
            if (getType().getUnitTypesLearntInLostCity().isEmpty()) {
                rumour = RumourType.NOTHING;
            }
            break;
        default:
            break;
        }

        // Mounds are a special case that degrade to other cases.
        boolean mounds = rumour == RumourType.MOUNDS;
        if (mounds) {
            boolean done = false;
            boolean burial = false;
            while (!done) {
                rumour = lostCity.chooseType(this, difficulty, random);
                switch (rumour) {
                case EXPEDITION_VANISHES: case NOTHING: case TRIBAL_CHIEF:
                case RUINS:
                    done = true;
                    break;
                case BURIAL_GROUND:
                    if (tile.getOwner() != null && tile.getOwner().isIndian()
                        && !burial) {
                        csNativeBurialGround(cs);
                        burial = true;
                    }
                    break;
                default:
                    ; // unacceptable result for mounds
                }
            }
        }

        logger.info("Unit " + getId() + " is exploring rumour " + rumour);
        switch (rumour) {
        case BURIAL_GROUND:
            csNativeBurialGround(cs);
            break;
        case EXPEDITION_VANISHES:
            cs.addDispose(serverPlayer, tile, this);
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 "lostCityRumour.ExpeditionVanishes",
                                 serverPlayer));
            break;
        case NOTHING:
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 ((mounds) ? "lostCityRumour.moundsNothing"
                                  : "lostCityRumour.Nothing"),
                                 serverPlayer, this));
            break;
        case LEARN:
            StringTemplate oldName = getLabel();
            List<UnitType> learnTypes = getType().getUnitTypesLearntInLostCity();
            unitType = Utils.getRandomMember(logger, "Choose learn",
                                             learnTypes, random);
            setType(unitType);
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 "lostCityRumour.Learn",
                                 serverPlayer, this)
                    .addStringTemplate("%unit%", oldName)
                    .add("%type%", getType().getNameKey()));
            break;
        case TRIBAL_CHIEF:
            int chiefAmount = Utils.randomInt(logger, "Chief base amount",
                                              random, dx * 10) + dx * 5;
            serverPlayer.modifyGold(chiefAmount);
            cs.addPartial(See.only(serverPlayer), serverPlayer,
                          "gold", "score");
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 ((mounds) ? "lostCityRumour.moundsTrinkets"
                                  : "lostCityRumour.TribalChief"),
                                 serverPlayer, this)
                    .addAmount("%money%", chiefAmount));
            break;
        case COLONIST:
            List<UnitType> foundTypes = spec.getUnitTypesWithAbility("model.ability.foundInLostCity");
            unitType = Utils.getRandomMember(logger, "Choose found",
                                             foundTypes, random);
            newUnit = new ServerUnit(game, tile, serverPlayer, unitType,
                                     UnitState.ACTIVE);
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 "lostCityRumour.Colonist",
                                 serverPlayer, newUnit));
            break;
        case CIBOLA:
            String cityName = game.getCityOfCibola();
            if (cityName != null) {
                int treasureAmount = Utils.randomInt(logger,
                    "Base treasure amount", random, dx * 600) + dx * 300;
                unitType = Utils.getRandomMember(logger, "Choose train",
                                                 treasureUnitTypes, random);
                newUnit = new ServerUnit(game, tile, serverPlayer, unitType,
                                         UnitState.ACTIVE);
                newUnit.setTreasureAmount(treasureAmount);
                cs.addMessage(See.only(serverPlayer),
                    new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                     "lostCityRumour.Cibola",
                                     serverPlayer, newUnit)
                        .add("%city%", cityName)
                        .addAmount("%money%", treasureAmount));
                cs.addGlobalHistory(game,
                    new HistoryEvent(game.getTurn(),
                                     HistoryEvent.EventType.CITY_OF_GOLD)
                        .addStringTemplate("%nation%", serverPlayer.getNationName())
                        .add("%city%", cityName)
                        .addAmount("%treasure%", treasureAmount));
                break;
            }
            // Fall through, found all the cities of gold.
        case RUINS:
            int ruinsAmount = Utils.randomInt(logger,
                "Base ruins amount", random, dx * 2) * 300 + 50;
            if (ruinsAmount < 500) { // TODO remove magic number
                serverPlayer.modifyGold(ruinsAmount);
                cs.addPartial(See.only(serverPlayer), serverPlayer,
                              "gold", "score");
            } else {
                unitType = Utils.getRandomMember(logger, "Choose train",
                                                 treasureUnitTypes, random);
                newUnit = new ServerUnit(game, tile, serverPlayer, unitType,
                                         UnitState.ACTIVE);
                newUnit.setTreasureAmount(ruinsAmount);
            }
            cs.addMessage(See.only(serverPlayer),
                 new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                  ((mounds) ? "lostCityRumour.moundsTreasure"
                                   : "lostCityRumour.Ruins"),
                                  serverPlayer, ((newUnit != null) ? newUnit
                                                 : this))
                     .addAmount("%money%", ruinsAmount));
            break;
        case FOUNTAIN_OF_YOUTH:
            Europe europe = serverPlayer.getEurope();
            if (europe == null) {
                cs.addMessage(See.only(serverPlayer),
                     new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                      "lostCityRumour.FountainOfYouthWithoutEurope",
                                      serverPlayer, this));
            } else {
                if (serverPlayer.hasAbility("model.ability.selectRecruit")
                    && !serverPlayer.isAI()) { // TODO: let the AI select
                    // Remember, and ask player to select
                    serverPlayer.setRemainingEmigrants(dx);
                    cs.addAttribute(See.only(serverPlayer),
                                    "fountainOfYouth", Integer.toString(dx));
                } else {
                    List<RandomChoice<UnitType>> recruitables
                        = serverPlayer.generateRecruitablesList();
                    for (int k = 0; k < dx; k++) {
                        UnitType type = RandomChoice
                            .getWeightedRandom(logger,
                                "Choose FoY", random, recruitables);
                        new ServerUnit(game, europe, serverPlayer, type,
                                       UnitState.ACTIVE);
                    }
                    cs.add(See.only(serverPlayer), europe);
                }
                cs.addMessage(See.only(serverPlayer),
                     new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                      "lostCityRumour.FountainOfYouth",
                                      serverPlayer, this));
            }
            cs.addAttribute(See.only(serverPlayer),
                            "sound", "sound.event.fountainOfYouth");
            break;
        case NO_SUCH_RUMOUR: case MOUNDS:
        default:
            logger.warning("Bogus rumour type: " + rumour);
            break;
        }
        tile.removeLostCityRumour();
    }

    /**
     * Check for a special contact panel for a nation.  If not found,
     * check for a more general one if allowed.
     *
     * @param player A European <code>Player</code> making contact.
     * @param other The <code>Player</code> nation to being contacted.
     * @return An <code>EventPanel</code> key, or null if none appropriate.
     */
    private String getContactKey(Player player, Player other) {
        String key = "EventPanel.MEETING_" + other.getNationNameKey();
        if (!Messages.containsKey(key)) {
            if (other.isEuropean()) {
                key = (player.hasContactedEuropeans()) ? null
                    : "EventPanel.MEETING_EUROPEANS";
            } else {
                key = (player.hasContactedIndians()) ? null
                    : "EventPanel.MEETING_NATIVES";
            }
        }
        return key;
    }

    /**
     * Collects the tiles surrounding this unit that the player
     * can not currently see, but now should as a result of a move.
     *
     * @param tile The center tile to look from.
     * @return A list of new tiles to see.
     */
    public List<Tile> collectNewTiles(Tile tile) {
        List<Tile> newTiles = new ArrayList<Tile>();
        int los = getLineOfSight();
        for (Tile t : tile.getSurroundingTiles(los)) {
            if (!getOwner().canSee(t)) newTiles.add(t);
        }
        return newTiles;
    }

    /**
     * Move a unit.
     *
     * @param newTile The <code>Tile</code> to move to.
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csMove(Tile newTile, Random random, ChangeSet cs) {
        ServerPlayer serverPlayer = (ServerPlayer) getOwner();
        Game game = getGame();
        Specification spec = game.getSpecification();
        Turn turn = game.getTurn();

        // Plan to update tiles that could not be seen before but will
        // now be within the line-of-sight.
        List<Tile> newTiles = collectNewTiles(newTile);

        // Update unit state.
        Location oldLocation = getLocation();
        setState(UnitState.ACTIVE);
        setStateToAllChildren(UnitState.SENTRY);
        if (oldLocation instanceof Europe) {
            ; // Do not try to calculate move cost from Europe!
        } else if (oldLocation instanceof Unit) {
            setMovesLeft(0); // Disembark always consumes all moves.
        } else {
            if (getMoveCost(newTile) <= 0) {
                logger.warning("Move of unit: " + getId()
                               + " from: " + ((oldLocation == null) ? "null"
                                              : oldLocation.getTile().getId())
                               + " to: " + newTile.getId()
                               + " has bogus cost: " + getMoveCost(newTile));
                setMovesLeft(0);
            }
            setMovesLeft(getMovesLeft() - getMoveCost(newTile));
        }

        // Do the move and explore a rumour if needed.
        setLocation(newTile);
        if (newTile.hasLostCityRumour() && serverPlayer.isEuropean()) {
            csExploreLostCityRumour(random, cs);
        }

        // Unless moving in from Europe, update the old location and
        // make sure the move is always visible even if the unit
        // dies, including the animation.  However dead units
        // make no discoveries.  Always update the new tile.
        if (oldLocation instanceof Europe) {
            cs.add(See.only(serverPlayer), (Europe) oldLocation);
        } else {
            cs.addMove(See.perhaps().always(serverPlayer), this,
                       oldLocation, newTile);
            cs.add(See.perhaps().always(serverPlayer),
                   (FreeColGameObject) oldLocation);
        }
        cs.add(See.perhaps().always(serverPlayer), newTile);
        if (isDisposed()) return;
        serverPlayer.csSeeNewTiles(newTiles, cs);

        if (newTile.isLand()) {
            // Claim land for tribe?
            Settlement settlement;
            if (newTile.getOwner() == null
                && serverPlayer.isIndian()
                && (settlement = getIndianSettlement()) != null
                && (newTile.getPosition().getDistance(settlement
                                                      .getTile().getPosition())
                    < settlement.getRadius()
                        + settlement.getType().getExtraClaimableRadius())) {
                newTile.setOwner(serverPlayer);
            }

            // Check for first landing
            if (serverPlayer.isEuropean()
                && !serverPlayer.isNewLandNamed()) {
                String newLand = Messages.getNewLandName(serverPlayer);
                if (serverPlayer.isAI()) {
                    // TODO: Not convinced shortcutting the AI like
                    // this is a good idea, this really should be in
                    // the AI code.
                    serverPlayer.setNewLandName(newLand);
                } else { // Ask player to name the land.
                    cs.addAttribute(See.only(serverPlayer),
                                    "nameNewLand", newLand);
                }
            }

            // Check for new contacts.
            ServerPlayer welcomer = null;
            for (Tile t : newTile.getSurroundingTiles(1, 1)) {
                if (t == null || !t.isLand()) {
                    continue; // Invalid tile for contact
                }

                ServerPlayer other = null;
                settlement = t.getSettlement();
                if (settlement != null) {
                    other = (ServerPlayer) t.getSettlement().getOwner();
                } else if (t.getFirstUnit() != null) {
                    other = (ServerPlayer) t.getFirstUnit().getOwner();
                }
                if (other == null || other == serverPlayer) {
                    continue; // No contact
                }

                // Activate sentries
                for (Unit u : t.getUnitList()) {
                    if (u.getState() == UnitState.SENTRY) {
                        u.setState(UnitState.ACTIVE);
                        cs.add(See.only(serverPlayer), u);
                    }
                }

                // Ignore previously contacted nations.
                if (serverPlayer.hasContacted(other)) continue;

                // Must be a first contact!
                if (serverPlayer.isIndian()) {
                    // Ignore native-to-native contacts.
                    if (!other.isIndian()) {
                        String key = getContactKey(other, serverPlayer);
                        if (key != null) {
                            cs.addMessage(See.only(other),
                                new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                                 key, other, serverPlayer));
                        }
                        cs.addHistory(other, new HistoryEvent(turn,
                                HistoryEvent.EventType.MEET_NATION)
                                .addStringTemplate("%nation%", serverPlayer.getNationName()));
                    }
                } else { // (serverPlayer.isEuropean)
                    // Initialize alarm for native settlements.
                    if (other.isIndian() && settlement != null) {
                        IndianSettlement is = (IndianSettlement) settlement;
                        if (!is.hasContactedSettlement(serverPlayer)) {
                            is.makeContactSettlement(serverPlayer);
                            cs.add(See.only(serverPlayer), is);
                        }
                    }

                    // Add first contact messages.
                    String key = getContactKey(serverPlayer, other);
                    if (key != null) {
                        cs.addMessage(See.only(serverPlayer),
                            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                             key, serverPlayer, other));
                    }

                    // History event for European players.
                    cs.addHistory(serverPlayer, new HistoryEvent(turn,
                            HistoryEvent.EventType.MEET_NATION)
                            .addStringTemplate("%nation%", other.getNationName()));
                    // Extra special meeting on first landing!
                    if (other.isIndian()
                        && !serverPlayer.isNewLandNamed()
                        && (welcomer == null || newTile.getOwner() == other)) {
                        welcomer = other;
                    }
                }

                // Now make the contact properly.
                serverPlayer.csChangeStance(Stance.PEACE, other, true, cs);
                serverPlayer.setTension(other,
                                        new Tension(Tension.TENSION_MIN));
                other.setTension(serverPlayer,
                                 new Tension(Tension.TENSION_MIN));
            }
            if (welcomer != null) {
                cs.addAttribute(See.only(serverPlayer), "welcome",
                    welcomer.getId());
                cs.addAttribute(See.only(serverPlayer), "camps",
                    Integer.toString(welcomer.getNumberOfSettlements()));
            }
        }

        // Check for slowing units.
        Unit slowedBy = getSlowedBy(newTile, random);
        if (slowedBy != null) {
            cs.addAttribute(See.only(serverPlayer), "slowedBy",
                            slowedBy.getId());
        }

        // Check for region discovery
        Region region = newTile.getDiscoverableRegion();
        if (serverPlayer.isEuropean() && region != null) {
            if (region.isPacific()) {
                cs.addAttribute(See.only(serverPlayer),
                                "discoverPacific", "true");
                cs.addRegion(serverPlayer, region,
                             Messages.message("model.region.pacific"));
            } else {
                String regionName = Messages.getDefaultRegionName(serverPlayer,
                                                                  region.getType());
                if (serverPlayer.isAI()) {
                    // TODO: here is another dubious AI shortcut.
                    cs.addRegion(serverPlayer, region, regionName);
                } else { // Ask player to name the region.
                    cs.addAttribute(See.only(serverPlayer),
                                    "discoverRegion", regionName);
                    cs.addAttribute(See.only(serverPlayer),
                                    "regionType",
                                    Messages.message(region.getLabel()));
                }
            }
        }
    }


    /**
     * Remove equipment from a unit.
     *
     * @param settlement The <code>Settlement</code> where the unit is
     *     (may be null if the unit is in Europe).
     * @param remove A collection of <code>EquipmentType</code> to remove.
     * @param amount Override the amount of equipment to remove.
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csRemoveEquipment(Settlement settlement,
                                  Collection<EquipmentType> remove,
                                  int amount, Random random, ChangeSet cs) {
        ServerPlayer serverPlayer = (ServerPlayer) getOwner();
        for (EquipmentType e : remove) {
            int a = (amount > 0) ? amount : getEquipmentCount(e);
            for (AbstractGoods goods : e.getGoodsRequired()) {
                GoodsType goodsType = goods.getType();
                int n = goods.getAmount() * a;
                if (isInEurope()) {
                    if (serverPlayer.canTrade(goodsType,
                                              Market.Access.EUROPE)) {
                        serverPlayer.sell(null, goodsType, n, random);
                        serverPlayer.csFlushMarket(goodsType, cs);
                    }
                } else if (settlement != null) {
                    settlement.addGoods(goodsType, n);
                }
            }
            // Removals can not cause incompatible-equipment trouble
            changeEquipment(e, -a);
        }
    }


    /**
     * Is there work for a unit to do at a stop?
     *
     * @param stop The <code>Stop</code> to test.
     * @return True if the unit should load or unload cargo at the stop.
     */
    public boolean hasWorkAtStop(Stop stop) {
        List<GoodsType> stopGoods = stop.getCargo();
        int cargoSize = stopGoods.size();
        for (Goods goods : getGoodsList()) {
            GoodsType type = goods.getType();
            if (stopGoods.contains(type)) {
                if (getLoadableAmount(type) > 0) {
                    // There is space on the unit to load some more
                    // of this goods type, so return true if there is
                    // some available at the stop.
                    Location loc = stop.getLocation();
                    if (loc instanceof Colony) {
                        if (((Colony) loc).getExportAmount(type) > 0) {
                            return true;
                        }
                    } else if (loc instanceof Europe) {
                        return true;
                    }
                } else {
                    cargoSize--; // No room for more of this type.
                }
            } else {
                return true; // This type should be unloaded here.
            }
        }

        // Return true if there is space left, and something to load.
        return getSpaceLeft() > 0 && cargoSize > 0;
    }


    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "serverUnit"
     */
    public String getServerXMLElementTagName() {
        return "serverUnit";
    }
}
