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


package net.sf.freecol.server.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighSeas;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.TradeRouteStop;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.ChangePriority;
import net.sf.freecol.server.control.ChangeSet.See;


/**
 * Server version of a unit.
 */
public class ServerUnit extends Unit implements ServerModelObject {

    private static final Logger logger = Logger.getLogger(ServerUnit.class.getName());

    // The bogus end of the world year.
    private static final int MAYAN_PROPHESY_YEAR = 2012;

    // How many `nothing' rumours are there.
    private static int rumourNothing = -1;


    /**
     * Trivial constructor required for all ServerModelObjects.
     */
    public ServerUnit(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new ServerUnit.
     *
     * -vis: Visibility issues depending on location.
     * -til: Changes appearance if unit goes into a colony.
     *
     * @param game The <code>Game</code> in which this unit belongs.
     * @param location The <code>Location</code> to place this at.
     * @param owner The <code>Player</code> owning this unit.
     * @param type The type of the unit.
     */
    public ServerUnit(Game game, Location location, Player owner,
                      UnitType type) {
        this(game, location, owner, type, type.getDefaultRole());
    }

    /**
     * Create a new ServerUnit from a template.
     *
     * -vis: Visibility issues depending on location.
     * -til: Changes appearance if unit goes into a colony.
     *
     * @param game The <code>Game</code> in which this unit belongs.
     * @param location The <code>Location</code> to place this at.
     * @param template A <code>Unit</code> to copy from.
     */
    public ServerUnit(Game game, Location location, Unit template) {
        this(game, location, game.getPlayer(template.getOwner().getNationId()),
            game.getSpecification().getUnitType(template.getType().getId()));

        final Specification spec = getSpecification();
        setNationality(template.getNationality());
        setEthnicity(template.getEthnicity());
        visibleGoodsCount = -1;
        if (getType().canCarryGoods()) {
            setGoodsContainer(new GoodsContainer(game, this));
        }
        workLeft = template.getWorkLeft();
        workType = spec.getGoodsType(template.getWorkType().getId());
        movesLeft = template.getMovesLeft();
        hitPoints = template.getType().getHitPoints();
        for (EquipmentType equipmentType : template.getEquipment().keySet()) {
            EquipmentType et = spec.getEquipmentType(equipmentType.getId());
            int amount = template.getEquipment().getCount(equipmentType);
            equipment.incrementCount(et, amount);
        }
        setRole();
        setStateUnchecked(template.getState());

        getOwner().addUnit(this);
    }

    /**
     * Creates a new ServerUnit.
     *
     * -vis: Visibility issues depending on location.
     * -til: Changes appearance if unit goes into a colony.
     *
     * @param game The <code>Game</code> in which this unit belongs.
     * @param location The <code>Location</code> to place this at.
     * @param owner The <code>Player</code> owning this unit.
     * @param type The type of the unit.
     * @param role The role of the unit.
     */
    public ServerUnit(Game game, Location location, Player owner,
                      UnitType type, Role role) {
        super(game);


        if (type.canCarryGoods()) {
            setGoodsContainer(new GoodsContainer(game, this));
        }

        UnitType newType = type.getTargetType(ChangeType.CREATION, owner);
        this.unitType = (newType == null) ? type : newType;
        this.owner = owner;
        if (unitType.hasAbility(Ability.PERSON)) {
            this.nationality = owner.getNationId();
            this.ethnicity = nationality;
        }
        this.location = null;

        this.workLeft = -1;
        this.workType = null;

        this.movesLeft = getInitialMovesLeft();
        this.hitPoints = unitType.getHitPoints();

        if (role != null) {
            List<EquipmentType> initialEquipment = getSpecification()
                .getRoleEquipment(role.getId(), true);
            for (EquipmentType equipmentType : initialEquipment) {
                if (EquipmentType.NO_EQUIPMENT.equals(equipmentType)) {
                    this.equipment.clear();
                    break;
                }
                this.equipment.incrementCount(equipmentType, 1);
            }
        }
        setRole();
        setStateUnchecked(state);
        this.visibleGoodsCount = -1;

        setLocation(location);//-vis(owner),-til
        owner.addUnit(this);
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
        boolean locDirty = false;
        boolean unitDirty = false;

        // Attrition.  Do it first as the unit might die.
        if (loc instanceof Tile && !((Tile)loc).hasSettlement()) {
            int attrition = getAttrition() + 1;
            setAttrition(attrition);
            if (attrition > getType().getMaximumAttrition()) {
                cs.addMessage(See.only(owner),
                    new ModelMessage(ModelMessage.MessageType.UNIT_LOST,
                        "model.unit.attrition", this)
                    .addStringTemplate("%unit%", getLabel())
                    .addStringTemplate("%location%",
                                       loc.getLocationNameFor(owner)));
                cs.add(See.perhaps(), (Tile)loc);
                cs.addDispose(See.perhaps().always(owner), loc,
                              this);//-vis(owner)
                owner.invalidateCanSeeTiles();//+vis(owner)
                return;
            }
        } else {
            setAttrition(0);
        }

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
                changeType(learn);//-vis: safe within colony
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

        // Update moves left.
        if (isInMission()) {
            getTile().updateIndianSettlement(owner);
            setMovesLeft(0);
        } else if (isDamaged()) {
            setMovesLeft(0);
        } else {
            setMovesLeft(getInitialMovesLeft());
        }

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
                    int amount = (getType().hasAbility(Ability.EXPERT_PIONEER))
                        ? 2 : 1;
                    int turns = ti.getTurnsToComplete();
                    if ((turns -= amount) < 0) turns = 0;
                    ti.setTurnsToComplete(turns);
                    setWorkLeft(turns);
                    if (ti.isRoad() && ti.isComplete()) {
                        ti.updateRoadConnections(true);
                        for (Tile t : loc.getTile().getSurroundingTiles(1)) {
                            if (t.hasRoad()) cs.add(See.perhaps(), t);
                        }
                        locDirty = true;
                    }
                }
                break;
            default:
                setWorkLeft(getWorkLeft() - 1);
                break;
            }

            if (loc instanceof HighSeas && getOwner().isREF()) {
                // Swift travel to America for the REF
                setWorkLeft(0);
            }
        }

        if (getWorkLeft() == 0) locDirty |= csCompleteWork(random, cs);

        if (getState() == UnitState.SKIPPED) {
            setState(UnitState.ACTIVE);
            unitDirty = true;
        }

        if (locDirty) {
            cs.add(See.perhaps(), (FreeColGameObject)getLocation());
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
     * @return True if the unit location needs an update.
     */
    private boolean csCompleteWork(Random random, ChangeSet cs) {
        setWorkLeft(-1);

        if (getLocation() instanceof HighSeas) {
            ServerPlayer owner = (ServerPlayer)getOwner();
            Europe europe = owner.getEurope();
            Map map = getGame().getMap();
            Location dst = getDestination();
            Location result = resolveDestination();
            if (result == europe) {
                logger.info(this + " arrives in Europe");
                if (getTradeRoute() == null) {
                    setDestination(null);
                    cs.addMessage(See.only(owner),
                        new ModelMessage(ModelMessage.MessageType.DEFAULT,
                                         "model.unit.arriveInEurope",
                                         europe, this)
                        .add("%europe%", europe.getNameKey()));
                }
                setState(UnitState.ACTIVE);
                setLocation(europe);//-vis: safe/Europe
                cs.add(See.only(owner), owner.getHighSeas());
                return true;
            } else if (result instanceof Tile) {
                Tile tile = ((Tile)result).getSafeTile(owner, random);
                logger.info(this + " arrives in America at " + tile
                    + ((dst == null) ? "" : ", sailing for " + dst));
                if (dst instanceof Map) setDestination(null);
                csMove(tile, random, cs);
            } else {
                logger.warning(this + " has unsupported destination "
                               + getDestination());
            }
        } else {
            switch (getState()) {
            case FORTIFYING:
                setState(UnitState.FORTIFIED);
                break;
            case IMPROVING:
                csImproveTile(random, cs);
                setWorkImprovement(null);
                return true;
            default:
                logger.warning("Unknown work completed, state=" + getState());
                setState(UnitState.ACTIVE);
                break;
            }
        }
        return false;
    }

    /**
     * Completes a tile improvement.
     *
     * +til: Resolves the change of appearance.
     *
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csImproveTile(Random random, ChangeSet cs) {
        Tile tile = getTile();
        tile.cacheUnseen();//+til
        AbstractGoods deliver = getWorkImprovement().getType().getProduction(tile.getType());
        if (deliver != null) { // Deliver goods if any
            int amount = deliver.getAmount();
            if (getType().hasAbility(Ability.EXPERT_PIONEER)) {
                amount *= 2;
            }
            Settlement settlement = tile.getSettlement();
            if (settlement != null && owner.owns(settlement)) {
                amount = (int)settlement.applyModifier(amount,
                    Modifier.TILE_TYPE_CHANGE_PRODUCTION, deliver.getType());
                settlement.addGoods(deliver.getType(), amount);
            } else {
                List<Settlement> adjacent = new ArrayList<Settlement>();
                int newAmount = amount;
                for (Tile t : tile.getSurroundingTiles(2)) {
                    Settlement ts = t.getSettlement();
                    if (ts != null && owner.owns(ts)) {
                        adjacent.add(ts);
                        int modAmount = (int)ts.applyModifier((float)amount,
                            Modifier.TILE_TYPE_CHANGE_PRODUCTION,
                            deliver.getType());
                        if (modAmount > newAmount) {
                            newAmount = modAmount;
                        }
                    }
                }
                if (adjacent.size() > 0) {
                    int deliverPerCity = newAmount / adjacent.size();
                    for (Settlement s : adjacent) {
                        s.addGoods(deliver.getType(), deliverPerCity);
                    }
                    // Add residue to first adjacent settlement.
                    adjacent.get(0).addGoods(deliver.getType(),
                                             newAmount % adjacent.size());
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
            tile.changeType(changeType);//-til
        }

        // Does a resource get exposed?
        TileImprovementType tileImprovementType = ti.getType();
        int exposeResource = tileImprovementType.getExposeResourcePercent();
        if (exposeResource > 0 && !tile.hasResource()) {
            if (Utils.randomInt(logger, "Expose resource", random, 100)
                < exposeResource) {
                ResourceType resType = RandomChoice
                    .getWeightedRandom(logger, "Resource type",
                                       tile.getType().getWeightedResources(),
                                       random);
                int minValue = resType.getMinValue();
                int maxValue = resType.getMaxValue();
                int value = minValue + ((minValue == maxValue) ? 0
                    : Utils.randomInt(logger, "Resource quantity",
                                      random, maxValue - minValue + 1));
                tile.addResource(new Resource(getGame(), tile,
                                              resType, value));//-til
            }
        }

        // Expend equipment
        EquipmentType type = ti.getType().getExpendedEquipmentType();
        changeEquipment(type, -ti.getType().getExpendedAmount());
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
            String messageId = getType() + ".noMoreTools";
            if (!Messages.containsKey(messageId)) {
                messageId = "model.unit.noMoreTools";
            }
            cs.addMessage(See.only(owner),
                new ModelMessage(ModelMessage.MessageType.WARNING,
                    messageId, this)
                .addStringTemplate("%unit%", getLabel())
                .addStringTemplate("%location%", locName));
        }
    }

    /**
     * Embark a unit.
     *
     * @param carrier The <code>Unit</code> to embark on.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csEmbark(Unit carrier, ChangeSet cs) {
        final ServerPlayer owner = (ServerPlayer)getOwner();

        Location oldLocation = getLocation();
        if (oldLocation instanceof WorkLocation) {
            oldLocation.getTile().cacheUnseen();//+til
        }
        setLocation(carrier);//-vis: only if on a different tile
                                  //-til if moving from colony
        setMovesLeft(0);
        cs.add(See.only(owner), (FreeColGameObject)oldLocation);
        if (carrier.getLocation() != oldLocation) {
            cs.add(See.only(owner), carrier);
        }
        if (oldLocation instanceof Tile) {
            if (carrier.getTile() != (Tile)oldLocation) {
                cs.addMove(See.only(owner), this, (Tile)oldLocation,
                           carrier.getTile());
                owner.invalidateCanSeeTiles();//+vis(serverPlayer)
            }
            cs.addDisappear(owner, (Tile)oldLocation, this);
        }
    }

    /**
     * Repair a unit.
     *
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csRepairUnit(ChangeSet cs) {
        ServerPlayer owner = (ServerPlayer) getOwner();
        setHitPoints(getHitPoints() + 1);
        if (!isDamaged()) {
            Location loc = getLocation();
            cs.addMessage(See.only(owner),
                new ModelMessage("model.unit.unitRepaired",
                    this, (FreeColGameObject)loc)
                .addStringTemplate("%unit%", getLabel())
                .addStringTemplate("%repairLocation%",
                    loc.getLocationNameFor(owner)));
            setState(UnitState.ACTIVE);
        }
        cs.addPartial(See.only(owner), this, "hitPoints");
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
        boolean pirate = hasAbility(Ability.PIRACY);
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
                if ((pirate || enemyUnit.hasAbility(Ability.PIRACY)
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
                    Math.round(totalProbability) + 1) < totalAttackPower) {
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
        ServerPlayer serverPlayer = (ServerPlayer)getOwner();
        Tile tile = getTile();
        ServerPlayer indianPlayer = (ServerPlayer)tile.getOwner();
        serverPlayer.csContact(indianPlayer, cs);
        indianPlayer.csModifyTension(serverPlayer, 
            Tension.Level.HATEFUL.getLimit(), cs);//+til
        serverPlayer.csChangeStance(Stance.WAR, indianPlayer, true, cs);
        cs.addMessage(See.only(serverPlayer),
            new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                "lostCityRumour.burialGround", serverPlayer, this)
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
        int difficulty = spec.getInteger(GameOptions.RUMOUR_DIFFICULTY);
        int dx = 10 - difficulty;
        UnitType unitType;
        Unit newUnit = null;
        List<UnitType> treasureUnitTypes
            = spec.getUnitTypesWithAbility(Ability.CARRY_TREASURE);

        RumourType rumour = lostCity.getType();
        if (rumour == null) {
            rumour = lostCity.chooseType(this, random);
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
            boolean nothing = false;
            while (!done) {
                rumour = lostCity.chooseType(this, random);
                switch (rumour) {
                case NOTHING: // Do not accept nothing-result the first time.
                    if (nothing) {
                        done = true;
                    } else {
                        nothing = true;
                    }
                    break;
                case EXPEDITION_VANISHES: case TRIBAL_CHIEF:
                    done = true;
                    break;
                case RUINS:
                    done = true;
                    // Misiulo confirms that in Col1 deSoto does *not*
                    // protect against a burial ground at the same
                    // time as a ruins find!
                    if (Utils.randomInt(logger, "Ruins+Burial", random, 100)
                        >= spec.getInteger(GameOptions.BAD_RUMOUR)) break;
                    // Fall through
                case BURIAL_GROUND:
                    if (tile.getOwner() != null
                        && tile.getOwner().isIndian()) {
                        csNativeBurialGround(cs);
                    }
                    done = true;
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
            cs.addDispose(See.perhaps().always(serverPlayer), tile,
                          this);//-vis(serverPlayer)
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                    "lostCityRumour.expeditionVanishes", serverPlayer));
            break;
        case NOTHING:
            if (game.getTurn().getYear() % 100 == 12
                && Utils.randomInt(logger, "Mayans?", random, 4) == 0) {
                int years = MAYAN_PROPHESY_YEAR - game.getTurn().getYear();
                cs.addMessage(See.only(serverPlayer),
                    new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                        "lostCityRumour.mayans", serverPlayer, this)
                    .add("%years%", Integer.toString(years)));
                break;
            } else if (mounds) {
                cs.addMessage(See.only(serverPlayer),
                    new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                        "lostCityRumour.moundsNothing", serverPlayer, this));
                break;
            }
            if (rumourNothing < 0) {
                int i;
                for (i = 0; Messages.containsKey("lostCityRumour.nothing."
                                                 + Integer.toString(i)); i++);
                rumourNothing = i;
            }
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                    "lostCityRumour.nothing." + Utils.randomInt(logger,
                        "Nothing rumour", random, rumourNothing),
                    serverPlayer, this));
            break;
        case LEARN:
            StringTemplate oldName = getLabel();
            List<UnitType> learnTypes = getType().getUnitTypesLearntInLostCity();
            unitType = Utils.getRandomMember(logger, "Choose learn",
                                             learnTypes, random);
            changeType(unitType);//-vis(serverPlayer)
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                    "lostCityRumour.learn", serverPlayer, this)
                .addStringTemplate("%unit%", oldName)
                .add("%type%", getType().getNameKey()));
            break;
        case TRIBAL_CHIEF:
            int chiefAmount = Utils.randomInt(logger, "Chief base amount",
                                              random, dx * 10) + dx * 5;
            serverPlayer.modifyGold(chiefAmount);
            cs.addPartial(See.only(serverPlayer), serverPlayer, "gold", "score");
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                    ((mounds) ? "lostCityRumour.moundsTrinkets"
                        : "lostCityRumour.tribalChief"),
                    serverPlayer, this)
                .addAmount("%money%", chiefAmount));
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
            break;
        case COLONIST:
            List<UnitType> foundTypes = spec.getUnitTypesWithAbility(Ability.FOUND_IN_LOST_CITY);
            unitType = Utils.getRandomMember(logger, "Choose found",
                                             foundTypes, random);
            newUnit = new ServerUnit(game, tile, serverPlayer,
                                     unitType);//-vis: safe, scout on tile
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                    "lostCityRumour.colonist", serverPlayer, newUnit));
            break;
        case CIBOLA:
            String cityKey = game.nextCityOfCibola();
            if (cityKey != null) {
                int treasureAmount = Utils.randomInt(logger,
                    "Base treasure amount", random, dx * 600) + dx * 300;
                unitType = Utils.getRandomMember(logger, "Choose train",
                                                 treasureUnitTypes, random);
                newUnit = new ServerUnit(game, tile, serverPlayer,
                                         unitType);//-vis: safe, scout on tile
                newUnit.setTreasureAmount(treasureAmount);
                cs.addMessage(See.only(serverPlayer),
                    new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                        "lostCityRumour.cibola", serverPlayer, newUnit)
                    .add("%city%", cityKey)
                    .addAmount("%money%", treasureAmount));
                cs.addGlobalHistory(game,
                    new HistoryEvent(game.getTurn(),
                        HistoryEvent.EventType.CITY_OF_GOLD, serverPlayer)
                    .addStringTemplate("%nation%", serverPlayer.getNationName())
                    .add("%city%", cityKey)
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
                newUnit = new ServerUnit(game, tile, serverPlayer,
                                         unitType);//-vis: safe, scout on tile
                newUnit.setTreasureAmount(ruinsAmount);
            }
            cs.addMessage(See.only(serverPlayer),
                 new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                  ((mounds) ? "lostCityRumour.moundsTreasure"
                                   : "lostCityRumour.ruins"),
                                  serverPlayer, ((newUnit != null) ? newUnit
                                                 : this))
                     .addAmount("%money%", ruinsAmount));
            break;
        case FOUNTAIN_OF_YOUTH:
            Europe europe = serverPlayer.getEurope();
            if (europe == null) {
                // FoY should now be disabled for non-colonial
                // players, but leave this in for now as it is harmless.
                cs.addMessage(See.only(serverPlayer),
                     new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                         "lostCityRumour.fountainOfYouthWithoutEurope",
                         serverPlayer, this));
            } else {
                if (!serverPlayer.isAI()) { // TODO: let the AI select
                    // Remember, and ask player to select
                    serverPlayer.setRemainingEmigrants(dx);
                    cs.addTrivial(See.only(serverPlayer), "fountainOfYouth",
                                  ChangeSet.ChangePriority.CHANGE_LATE,
                                  "migrants", Integer.toString(dx));
                } else {
                    List<RandomChoice<UnitType>> recruitables
                        = serverPlayer.generateRecruitablesList();
                    for (int k = 0; k < dx; k++) {
                        UnitType type = RandomChoice.getWeightedRandom(logger,
                            "Choose FoY", recruitables, random);
                        new ServerUnit(game, europe, serverPlayer,
                                       type);//-vis: safe, Europe
                    }
                    cs.add(See.only(serverPlayer), europe);
                }
                cs.addMessage(See.only(serverPlayer),
                     new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                         "lostCityRumour.fountainOfYouth",
                         serverPlayer, this));
                cs.addAttribute(See.only(serverPlayer),
                                "sound", "sound.event.fountainOfYouth");
            }
            break;
        case NO_SUCH_RUMOUR: case MOUNDS:
        default:
            logger.warning("Bogus rumour type: " + rumour);
            break;
        }
        tile.cacheUnseen();//+til
        tile.removeLostCityRumour();//-til
    }

    /**
     * Activate sentried units on a tile.
     *
     * @param tile The <code>Tile</code> to activate sentries on.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csActivateSentries(Tile tile, ChangeSet cs) {
        for (Unit u : tile.getUnitList()) {
            if (u.getState() == UnitState.SENTRY) {
                u.setState(UnitState.ACTIVE);
                cs.add(See.perhaps(), u);
            }
        }
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
        for (Tile t : tile.getSurroundingTiles(0, los)) {
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

        // Plan to update tiles that could not be seen before but will
        // now be within the line-of-sight.
        List<Tile> newTiles = collectNewTiles(newTile);

        // Update unit state.
        Location oldLocation = getLocation();
        setState(UnitState.ACTIVE);
        setStateToAllChildren(UnitState.SENTRY);
        if (oldLocation instanceof HighSeas) {
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
        if (oldLocation instanceof WorkLocation) {
            oldLocation.getTile().cacheUnseen();//+til
        }
        setLocation(newTile);//-vis(serverPlayer),-til if in colony
        if (newTile.hasLostCityRumour() && serverPlayer.isEuropean()) {
            csExploreLostCityRumour(random, cs);//+vis(serverPlayer)
        } else {
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
        }

        // Unless moving in from off-map, update the old location and
        // make sure the move is always visible even if the unit
        // dies (including the animation).  However, dead units
        // make no discoveries.  Always update the new tile.
        if (oldLocation instanceof Tile) {
            cs.addMove(See.perhaps().always(serverPlayer), this,
                       oldLocation, newTile);
            cs.add(See.perhaps().always(serverPlayer),
                   (FreeColGameObject)oldLocation);
        } else {
            cs.add(See.only(serverPlayer), (FreeColGameObject)oldLocation);
        }
        cs.add(See.perhaps().always(serverPlayer), newTile);
        if (isDisposed()) return;
        serverPlayer.csSeeNewTiles(newTiles, cs);

        if (newTile.isLand()) {
            Settlement settlement;
            Unit unit = null;
            int d;
            // Claim land for tribe?
            if ((newTile.getOwner() == null
                    || (newTile.getOwner().isEuropean()
                        && newTile.getOwningSettlement() == null))
                && serverPlayer.isIndian()
                && (settlement = getHomeIndianSettlement()) != null
                && ((d = newTile.getDistanceTo(settlement.getTile()))
                    < (settlement.getRadius()
                        + settlement.getType().getExtraClaimableRadius()))
                && Utils.randomInt(logger, "Claim tribal land", random,
                                   d + 1) == 0) {
                newTile.cacheUnseen();//+til
                newTile.changeOwnership(serverPlayer, settlement);//-til
            }

            // Check for first landing
            String newLand = null;
            boolean firstLanding = !serverPlayer.isNewLandNamed();
            if (serverPlayer.isEuropean() && firstLanding) {
                newLand = Messages.getNewLandName(serverPlayer);
                // Set the default value now to prevent multiple attempts.
                // The user setNewLandName can override.
                serverPlayer.setNewLandName(newLand);
                cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                    new NewLandNameMessage(this, newLand));
                logger.finest("First landing for " + serverPlayer
                    + " at " + newTile + " with " + this);
            }

            // Check for new contacts.
            for (Tile t : newTile.getSurroundingTiles(1, 1)) {
                if (t == null || !t.isLand()) {
                    continue; // Invalid tile for contact
                }

                settlement = t.getSettlement();
                ServerPlayer other = (settlement != null)
                    ? (ServerPlayer)settlement.getOwner()
                    : ((unit = t.getFirstUnit()) != null)
                    ? (ServerPlayer)unit.getOwner()
                    : null;
                if (other == null
                    || other == serverPlayer) continue; // No contact
                if (serverPlayer.csContact(other, cs)) {
                    // First contact
                    if (serverPlayer.isEuropean()) {
                        if (other.isIndian()) {
                            Tile offer = (firstLanding && other.owns(newTile))
                                ? newTile
                                : null;
                            serverPlayer.csNativeFirstContact(other, offer, cs);
                        } else {
                            serverPlayer.csEuropeanFirstContact(this, settlement, unit, cs);
                        }
                    } else {
                        if (other.isIndian()) {
                            ; // Do nothing
                        } else {
                            other.csNativeFirstContact(serverPlayer, null, cs);
                        }
                    }
                }

                // Initialize alarm for native settlements or units and
                // notify of contact.
                ServerPlayer contactPlayer = serverPlayer;
                IndianSettlement is = (settlement instanceof IndianSettlement)
                    ? (IndianSettlement)settlement
                    : null;
                if (is != null
                    || (unit != null
                        && (is = unit.getHomeIndianSettlement()) != null)
                    || (unit != null
                        && (contactPlayer = (ServerPlayer)unit.getOwner())
                            .isEuropean()
                        && (is = getHomeIndianSettlement()) != null)) {
                    Tile copied = is.getTile().getTileToCache();
                    if (contactPlayer.hasExplored(is.getTile())
                        && is.setContacted(contactPlayer)) {//-til
                        is.getTile().cacheUnseen(copied);//+til
                        cs.add(See.only(contactPlayer), is);
                        // First European contact with native settlement.
                        StringTemplate nation = is.getOwner().getNationName();
                        cs.addMessage(See.only(contactPlayer),
                            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                             "model.unit.nativeSettlementContact",
                                             this, is)
                                .addStringTemplate("%nation%", nation)
                                .addName("%settlement%", is.getName()));
                        logger.finest("First contact between " + contactPlayer
                            + " and " + is + " at " + newTile);
                    }                   
                }
                csActivateSentries(t, cs);
            }
        } else { // water
            for (Tile t : newTile.getSurroundingTiles(1, 1)) {
                if (t == null || t.isLand() || t.getFirstUnit() == null) {
                    continue;
                }
                if ((ServerPlayer) t.getFirstUnit().getOwner()
                    != serverPlayer) csActivateSentries(t, cs);
            }
        }

        // Check for slowing units.
        Unit slowedBy = getSlowedBy(newTile, random);
        if (slowedBy != null) {
            StringTemplate enemy = slowedBy.getApparentOwnerName();
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                    "model.unit.slowed", this, slowedBy)
                .addStringTemplate("%unit%", this.getFullLabel())
                .addStringTemplate("%enemyUnit%", slowedBy.getFullLabel())
                .addStringTemplate("%enemyNation%", enemy));
        }

        // Check for region discovery
        Region region = newTile.getDiscoverableRegion();
        if (serverPlayer.isEuropean() && region != null
            && region.getName() == null) {
            String defaultName = (region.isPacific())
                ? Messages.message("model.region.pacific")
                : Messages.getDefaultRegionName(serverPlayer,
                                                region.getType());
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new NewRegionNameMessage(region, newTile, this,
                                         defaultName));
            // Set a placeholder name on the server for now, so that
            // we do not ask again if another unit moves in before we
            // asynchronously answer the new region name message.
            region.setName(defaultName);
        }
    }

    /**
     * Is there work for a unit to do at a stop?
     *
     * @param stop The <code>TradeRouteStop</code> to test.
     * @return True if the unit should load or unload cargo at the stop.
     */
    public boolean hasWorkAtStop(TradeRouteStop stop) {
        Location loc = stop.getLocation();

        // Look for goods to load.
        List<GoodsType> stopGoods = stop.getCargo();
        for (GoodsType type : stopGoods) {
            if (getLoadableAmount(type) <= 0) continue;
            // There is space on the unit to load some more
            // of this goods type, so return true if there is
            // some available at the stop.
            if (loc instanceof Colony) {
                if (((Colony)loc).getExportAmount(type) > 0) return true;
            } else if (loc instanceof Europe) {
                return true;
            }
        }

        // Look for goods to unload.
        for (Goods goods : getCompactGoodsList()) {
            GoodsType type = goods.getType();
            if (stopGoods.contains(type)) continue;
            // There are goods on board this unit that need to be unloaded.
            if (loc instanceof Colony) {
                if (((Colony)loc).getImportAmount(type) > 0) return true;
            } else if (loc instanceof Europe) {
                return true;
            }
        }
            
        return false;
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
