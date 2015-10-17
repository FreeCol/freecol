/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.i18n.NameCache;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighSeas;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
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
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.RandomChoice;
import static net.sf.freecol.common.util.RandomUtils.*;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.ChangePriority;
import net.sf.freecol.server.control.ChangeSet.See;


/**
 * Server version of a unit.
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
     * Note all FCGOTs are looked up in the specification by id,
     * allowing the template to derive from a different specification
     * as might happen when loading a scenario map.
     *
     * -vis: Visibility issues depending on location.
     * -til: Changes appearance if unit goes into a colony.
     *
     * @param game The <code>Game</code> in which this unit belongs.
     * @param location The <code>Location</code> to place this at.
     * @param template A <code>Unit</code> to copy from.
     */
    public ServerUnit(Game game, Location location, Unit template) {
        this(game, location,
            game.getPlayerByNationId(template.getOwner().getNationId()),
            game.getSpecification().getUnitType(template.getType().getId()),
            game.getSpecification().getDefaultRole());

        final Specification spec = getSpecification();
        setNationality(template.getNationality());
        setEthnicity(template.getEthnicity());
        workLeft = template.getWorkLeft();
        workType = spec.getGoodsType(template.getWorkType().getId());
        movesLeft = template.getMovesLeft();
        hitPoints = template.getType().getHitPoints();
        changeRole(spec.getRole(template.getRole().getId()),
                   template.getRoleCount());
        setStateUnchecked(template.getState());
        if (getType().canCarryGoods()) {
            setGoodsContainer(new GoodsContainer(game, this));
        }
        this.visibleGoodsCount = -1;
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

        UnitType newType = type.getTargetType(ChangeType.CREATION, owner);
        this.unitType = (newType == null) ? type : newType;
        this.owner = owner;
        this.state = UnitState.ACTIVE; // placeholder
        this.role = getSpecification().getDefaultRole(); // placeholder
        this.location = null;
        this.entryLocation = null;
        if (unitType.hasAbility(Ability.PERSON)) {
            this.nationality = owner.getNationId();
            this.ethnicity = nationality;
        } else {
            this.nationality = null;
            this.ethnicity = null;
        }

        this.workLeft = -1;
        this.workType = null;
        this.movesLeft = getInitialMovesLeft();
        this.hitPoints = unitType.getHitPoints();
        this.experienceType = null;
        this.experience = 0;
        this.workImprovement = null;
        this.student = this.teacher = null;
        this.turnsOfTraining = 0;
        this.indianSettlement = null;
        this.hitPoints = unitType.getHitPoints();
        this.destination = null;
        this.tradeRoute = null;
        this.currentStop = -1;
        this.treasureAmount = 0;
        this.attrition = 0;
        this.visibleGoodsCount = -1;

        // Fix up role, state and location now other values are present.
        changeRole(role, role.getMaximumCount());
        setStateUnchecked(state);
        setLocation(location);//-vis(owner),-til
        if (getType().canCarryGoods()) {
            setGoodsContainer(new GoodsContainer(game, this));
        }

        owner.addUnit(this);
    }


    /**
     * New turn for this unit.
     *
     * @param random A <code>Random</code> number source.
     * @param lb A <code>LogBuilder</code> to log to.
     * @param cs A <code>ChangeSet</code> to update.
     */
    @Override
    public void csNewTurn(Random random, LogBuilder lb, ChangeSet cs) {
        lb.add(this);
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
                        loc.getLocationLabelFor(owner)));
                cs.add(See.perhaps(), (Tile)loc);
                cs.addRemove(See.perhaps().always(owner), loc, 
                             this);//-vis(owner)
                this.dispose();
                owner.invalidateCanSeeTiles();//+vis(owner)
                lb.add(", ");
                return;
            }
        } else {
            setAttrition(0);
        }

        // Check for experience-promotion.
        GoodsType produce;
        UnitType learn;
        if (isInColony()
            && (produce = getWorkType()) != null
            && (learn = spec.getExpertForProducing(produce)) != null
            && learn != getType()
            && getType().canBeUpgraded(learn, ChangeType.EXPERIENCE)) {
            int maximumExperience = getType().getMaximumExperience();
            int maxValue = (100 * maximumExperience) /
                getType().getUnitTypeChange(learn).getProbability(ChangeType.EXPERIENCE);
            if (maxValue > 0
                && randomInt(logger, "Experience", random, maxValue)
                < Math.min(getExperience(), maximumExperience)) {
                StringTemplate oldName = getLabel();
                changeType(learn);//-vis: safe within colony
                cs.addMessage(See.only(owner),
                    new ModelMessage(ModelMessage.MessageType.UNIT_IMPROVED,
                        "model.unit.experience", getColony(), this)
                    .addStringTemplate("%oldName%", oldName)
                    .addStringTemplate("%unit%", getLabel())
                    .addName("%colony%", getColony().getName()));
                lb.add(" experience upgrade to ", getType());
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
                if (ti == null // Another unit on the tile completed it first
                    || ti.isComplete()) {
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

        if (getState() == UnitState.SKIPPED) {
            setState(UnitState.ACTIVE);
            unitDirty = true;
        }

        if (getWorkLeft() <= 0) {
            if (getLocation() instanceof HighSeas) {
                final Europe europe = owner.getEurope();
                final Location dst = getDestination();
                Location result = resolveDestination();
                if (result == europe) {
                    lb.add(" arrives in Europe");
                    if (getTradeRoute() == null) {
                        setDestination(null);
                        cs.addMessage(See.only(owner),
                            new ModelMessage(ModelMessage.MessageType.DEFAULT,
                                "model.unit.arriveInEurope",
                                europe, this)
                            .addNamed("%europe%", europe));
                    }
                    setState(UnitState.ACTIVE);
                    setLocation(europe);//-vis: safe/Europe
                    cs.add(See.only(owner), owner.getHighSeas());
                    locDirty = true;
                } else {
                    if (!(result instanceof Tile)) {
                        logger.warning("Unit has unsupported destination: "
                            + dst + " -> " + result);
                        result = getEntryLocation().getTile();
                    }
                    Tile tile = result.getTile().getSafeTile(owner, random);
                    lb.add(" arrives in America at ", tile);
                    if (dst != null) {
                        lb.add(" sailing for ", dst);
                        if (dst instanceof Map) setDestination(null);
                    }
                    csMove(tile, random, cs);
                    locDirty = unitDirty = false; // loc update present
                }
            } else {
                switch (getState()) {
                case ACTIVE: case FORTIFIED: case SENTRY: case IN_COLONY:
                    break; // These states are stable
                case IMPROVING:
                    csImproveTile(random, cs);
                    setWorkImprovement(null);
                    locDirty = true;
                    break;
                case FORTIFYING:
                    setState(UnitState.FORTIFIED);
                    unitDirty = true;
                    break;
                case SKIPPED: default:
                    lb.add(" work completed, bad state: ", getState());
                    setState(UnitState.ACTIVE);
                    unitDirty = true;
                    break;
                }
            }
        }

        if (locDirty) {
            cs.add(See.perhaps(), (FreeColGameObject)getLocation());
        } else if (unitDirty) {
            cs.add(See.perhaps(), this);
        } else {
            cs.addPartial(See.only(owner), this, "movesLeft");
        }
        lb.add(", ");
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
        AbstractGoods deliver = getWorkImprovement().getType()
            .getProduction(tile.getType());
        if (deliver != null) { // Deliver goods if any
            final Turn turn = getGame().getTurn();
            int amount = deliver.getAmount();
            amount = (int)this.applyModifiers(amount, turn,
                Modifier.TILE_TYPE_CHANGE_PRODUCTION, deliver.getType());
            Settlement settlement = tile.getOwningSettlement();
            if (settlement != null && owner.owns(settlement)) {
                amount = (int)settlement.applyModifiers(amount, turn,
                    Modifier.TILE_TYPE_CHANGE_PRODUCTION, deliver.getType());
                settlement.addGoods(deliver.getType(), amount);
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
            if (randomInt(logger, "Expose resource", random, 100)
                < exposeResource) {
                ResourceType resType = RandomChoice
                    .getWeightedRandom(logger, "Resource type",
                                       tile.getType().getWeightedResources(),
                                       random);
                int minValue = resType.getMinValue();
                int maxValue = resType.getMaxValue();
                int value = minValue + ((minValue == maxValue) ? 0
                    : randomInt(logger, "Resource quantity",
                                random, maxValue - minValue + 1));
                tile.addResource(new Resource(getGame(), tile,
                                              resType, value));//-til
            }
        }

        // Expend equipment.
        if (changeRoleCount(-ti.getType().getExpendedAmount())) {
            // FIXME: assumes tools, make more generic, use
            // ti.getType().getRequiredRole().getRequiredGoods()
            ServerPlayer owner = (ServerPlayer)getOwner();
            StringTemplate locName
                = getLocation().getLocationLabelFor(owner);
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

        // Cancel other co-located improvements of the same type
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
        Colony colony = (oldLocation instanceof WorkLocation) ? getColony()
            : null;
        if (colony != null) oldLocation.getTile().cacheUnseen();//+til
        setLocation(carrier);//-vis: only if on a different tile
                             //-til if moving from colony
        setMovesLeft(0);
        cs.add(See.only(owner), (colony != null) ? colony
            : (FreeColGameObject)oldLocation);
        if (carrier.getLocation() != oldLocation) {
            cs.add(See.only(owner), carrier);
        }
        if (oldLocation instanceof Tile) {
            if (carrier.getTile() != oldLocation) {
                cs.addMove(See.only(owner), this, oldLocation,
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
                    loc.getLocationLabelFor(owner)));
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
        double attackPower = 0, totalAttackPower = 0;

        if (!isNaval() || getMovesLeft() <= 0) return null;
        for (Tile tile : newTile.getSurroundingTiles(1)) {
            // Ships in settlements do not slow enemy ships, but:
            // FIXME: should a fortress slow a ship?
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
            double defencePower = combatModel.getDefencePower(attacker, this);
            double totalProbability = totalAttackPower + defencePower;
            if (randomInt(logger, "Slowed", random,
                    (int)Math.round(totalProbability + 1)) < totalAttackPower) {
                int diff = Math.max(0,
                    (int)Math.round(totalAttackPower - defencePower));
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
                RumourType.BURIAL_GROUND.getDescriptionKey(), serverPlayer, this)
            .addStringTemplate("%nation%", indianPlayer.getNationLabel()));
    }

    /**
     * Explore a lost city.
     *
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to add changes to.
     * @return True if the unit survives.
     */
    private boolean csExploreLostCityRumour(Random random, ChangeSet cs) {
        ServerPlayer serverPlayer = (ServerPlayer) getOwner();
        Tile tile = getTile();
        LostCityRumour lostCity = tile.getLostCityRumour();
        if (lostCity == null) return true;

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
                    if (randomInt(logger, "Ruins+Burial", random, 100)
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
        boolean result = true;
        String key = rumour.getDescriptionKey();
        switch (rumour) {
        case BURIAL_GROUND:
            csNativeBurialGround(cs);
            break;
        case EXPEDITION_VANISHES:
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 key, serverPlayer));
            result = false;
            break;
        case NOTHING:
            cs.addMessage(See.only(serverPlayer),
                lostCity.getNothingMessage(serverPlayer, mounds, random));
            break;
        case LEARN:
            StringTemplate oldName = getLabel();
            List<UnitType> learnTypes = getType().getUnitTypesLearntInLostCity();
            unitType = getRandomMember(logger, "Choose learn",
                                       learnTypes, random);
            changeType(unitType);//-vis(serverPlayer)
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 key, serverPlayer, this)
                    .addStringTemplate("%unit%", oldName)
                    .addNamed("%type%", getType()));
            break;
        case TRIBAL_CHIEF:
            int chiefAmount = randomInt(logger, "Chief base amount",
                                        random, dx * 10) + dx * 5;
            serverPlayer.modifyGold(chiefAmount);
            cs.addPartial(See.only(serverPlayer), serverPlayer, "gold", "score");
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                    ((mounds) ? rumour.getAlternateDescriptionKey("mounds")
                        : key),
                    serverPlayer, this)
                .addAmount("%money%", chiefAmount));
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
            break;
        case COLONIST:
            List<UnitType> foundTypes
                = spec.getUnitTypesWithAbility(Ability.FOUND_IN_LOST_CITY);
            unitType = getRandomMember(logger, "Choose found",
                                       foundTypes, random);
            newUnit = new ServerUnit(game, tile, serverPlayer,
                                     unitType);//-vis: safe, scout on tile
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 key, serverPlayer, newUnit));
            break;
        case CIBOLA:
            String cityName = NameCache.getNextCityOfCibola();
            if (cityName != null) {
                int treasureAmount = randomInt(logger,
                    "Base treasure amount", random, dx * 600) + dx * 300;
                unitType = getRandomMember(logger, "Choose train",
                                           treasureUnitTypes, random);
                newUnit = new ServerUnit(game, tile, serverPlayer,
                                         unitType);//-vis: safe, scout on tile
                newUnit.setTreasureAmount(treasureAmount);
                cs.addMessage(See.only(serverPlayer),
                    new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                     key, serverPlayer, newUnit)
                    .addName("%city%", cityName)
                    .addAmount("%money%", treasureAmount));
                cs.addGlobalHistory(game,
                    new HistoryEvent(game.getTurn(),
                        HistoryEvent.HistoryEventType.CITY_OF_GOLD, serverPlayer)
                    .addStringTemplate("%nation%", serverPlayer.getNationLabel())
                    .addName("%city%", cityName)
                    .addAmount("%treasure%", treasureAmount));
                break;
            }
            // Fall through, found all the cities of gold.
        case RUINS:
            int ruinsAmount = randomInt(logger, "Base ruins amount", random,
                                        dx * 2) * 300 + 50;
            if (ruinsAmount < 500) { // FIXME: remove magic number
                serverPlayer.modifyGold(ruinsAmount);
                cs.addPartial(See.only(serverPlayer), serverPlayer,
                              "gold", "score");
            } else {
                unitType = getRandomMember(logger, "Choose train",
                                           treasureUnitTypes, random);
                newUnit = new ServerUnit(game, tile, serverPlayer,
                                         unitType);//-vis: safe, scout on tile
                newUnit.setTreasureAmount(ruinsAmount);
            }
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                    ((mounds) ? rumour.getAlternateDescriptionKey("mounds")
                        : key),
                    serverPlayer, ((newUnit != null) ? newUnit : this))
                    .addAmount("%money%", ruinsAmount));
            break;
        case FOUNTAIN_OF_YOUTH:
            ServerEurope europe = (ServerEurope)serverPlayer.getEurope();
            if (europe == null) {
                // FoY should now be disabled for non-colonial
                // players, but leave this in for now as it is harmless.
                cs.addMessage(See.only(serverPlayer),
                    new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                        rumour.getAlternateDescriptionKey("noEurope"),
                        serverPlayer, this));
            } else {
                if (serverPlayer.isAI()) { // FIXME: let the AI select
                    europe.generateFountainRecruits(dx, random);
                    cs.add(See.only(serverPlayer), europe);
                } else {
                    // Remember, and ask player to select
                    serverPlayer.setRemainingEmigrants(dx);
                    cs.addTrivial(See.only(serverPlayer), "fountainOfYouth",
                                  ChangeSet.ChangePriority.CHANGE_LATE,
                                  "migrants", Integer.toString(dx));
                }
                cs.addMessage(See.only(serverPlayer),
                     new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                      key, serverPlayer, this));
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
        return result;
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
        final int los = getLineOfSight();
        return tile.getSurroundingTiles(0, los).stream()
            .filter(t -> !getOwner().canSee(t)).collect(Collectors.toList());
    }

    /**
     * Move a unit.
     *
     * @param newTile The <code>Tile</code> to move to.
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csMove(Tile newTile, Random random, ChangeSet cs) {
        final ServerPlayer serverPlayer = (ServerPlayer)getOwner();

        // Plan to update tiles that could not be seen before but will
        // now be within the line-of-sight.
        final Location oldLocation = getLocation();
        List<Tile> oldTiles = (oldLocation.getTile() == null)
            ? Collections.<Tile>emptyList()
            : oldLocation.getTile().getSurroundingTiles(1, getLineOfSight());
        List<Tile> newTiles = collectNewTiles(newTile);

        // Update unit state.
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
        if (newTile.hasLostCityRumour() && serverPlayer.isEuropean()
            && !csExploreLostCityRumour(random, cs)) {
            cs.addRemove(See.perhaps().always(serverPlayer), oldLocation,
                         this);//-vis(serverPlayer)
            this.dispose();
        }
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)

        // Update tiles that are now invisible.
        Iterator<Tile> it = oldTiles.iterator();
        while (it.hasNext()) {
            if (serverPlayer.canSee(it.next())) it.remove();
        }
        if (!oldTiles.isEmpty()) cs.add(See.only(serverPlayer), oldTiles);
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
                && randomInt(logger, "Claim tribal land", random, d + 1) == 0) {
                newTile.cacheUnseen();//+til
                newTile.changeOwnership(serverPlayer, settlement);//-til
            }

            // Check for first landing
            String newLand = null;
            boolean firstLanding = !serverPlayer.isNewLandNamed();
            if (serverPlayer.isEuropean() && firstLanding) {
                newLand = serverPlayer.getNameForNewLand();
                // Set the default value now to prevent multiple attempts.
                // The user setNewLandName can override.
                serverPlayer.setNewLandName(newLand);
                cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                    new NewLandNameMessage(this, newLand));
                logger.finest("First landing for " + serverPlayer
                    + " at " + newTile + " with " + this);
            }

            // Check for new contacts.
            List<ServerPlayer> pending = new ArrayList<>();
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
                    || other == serverPlayer
                    || pending.contains(other)) continue; // No contact
                if (serverPlayer.csContact(other, cs)) {
                    // First contact.  Note contact pending because
                    // European first contact now requires a diplomacy
                    // interaction to complete before leaving UNCONTACTED
                    // state.
                    pending.add(other);
                    if (serverPlayer.isEuropean()) {
                        if (other.isIndian()) {
                            Tile offer = (firstLanding && other.owns(newTile))
                                ? newTile
                                : null;
                            serverPlayer.csNativeFirstContact(other, offer, cs);
                        } else {
                            serverPlayer.csEuropeanFirstContact(this,
                                settlement, unit, cs);
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
                        && (is = getHomeIndianSettlement()) != null
                        && is.getTile() != null)) {
                    Tile copied = is.getTile().getTileToCache();
                    if (contactPlayer.hasExplored(is.getTile())
                        && is.setContacted(contactPlayer)) {//-til
                        is.getTile().cacheUnseen(copied);//+til
                        cs.add(See.only(contactPlayer), is);
                        // First European contact with native settlement.
                        StringTemplate nation = is.getOwner().getNationLabel();
                        cs.addMessage(See.only(contactPlayer),
                            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                             "model.unit.nativeSettlementContact",
                                             this, is)
                                .addStringTemplate("%nation%", nation)
                                .addName("%settlement%", is.getName()));
                        logger.finest("First contact between "
                            + contactPlayer.getId()
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
                if (t.getFirstUnit().getOwner()
                    != serverPlayer) csActivateSentries(t, cs);
            }
        }

        // Disembark in colony.
        if (isCarrier() && !isEmpty() && newTile.getColony() != null
            && getSpecification().getBoolean(GameOptions.DISEMBARK_IN_COLONY)) {
            for (Unit u : getUnitList()) {
                ((ServerUnit)u).csMove(newTile, random, cs);
            }
            setMovesLeft(0);
        }
                
        // Check for slowing units.
        Unit slowedBy = getSlowedBy(newTile, random);
        if (slowedBy != null) {
            StringTemplate enemy = slowedBy.getApparentOwnerName();
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                    "model.unit.slowed", this, slowedBy)
                .addStringTemplate("%unit%", getLabel(UnitLabelType.NATIONAL))
                .addStringTemplate("%enemyUnit%", slowedBy.getLabel(UnitLabelType.NATIONAL))
                .addStringTemplate("%enemyNation%", enemy));
        }

        // Check for region discovery
        Region region = newTile.getDiscoverableRegion();
        if (serverPlayer.isEuropean() && region != null
            && region.getDiscoverer() == null) {
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new NewRegionNameMessage(region, newTile, this,
                    serverPlayer.getNameForRegion(region)));
            region.setDiscoverer(getId());
        }
    }


    // Serialization

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "serverUnit"
     */
    @Override
    public String getServerXMLElementTagName() {
        return "serverUnit";
    }
}
