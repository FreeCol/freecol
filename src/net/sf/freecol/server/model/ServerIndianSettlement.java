/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.logging.Logger;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ChangeSet.See;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.RandomUtils.*;



/**
 * The server version of an Indian Settlement.
 */
public class ServerIndianSettlement extends IndianSettlement
    implements TurnTaker {

    private static final Logger logger = Logger.getLogger(ServerIndianSettlement.class.getName());

    /** Alarm added when a new missionary is added. */
    public static final int ALARM_NEW_MISSIONARY = -100;

    /** How far to search for a colony to add an Indian convert to. */
    public static final int MAX_CONVERT_DISTANCE = 10;

    public static final int MAX_HORSES_PER_TURN = 2;


    /**
     * Trivial constructor for Game.newInstance.
     *
     * @param game The {@code Game} in which this object belong.
     * @param id The object identifier.
     */
    public ServerIndianSettlement(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new ServerIndianSettlement.
     *
     * @param game The {@code Game} in which this object belong.
     * @param owner The {@code Player} owning this settlement.
     * @param name The name for this settlement.
     * @param tile The location of the {@code IndianSettlement}.
     * @param isCapital True if settlement is tribe's capital
     * @param learnableSkill The skill that can be learned by
     *     Europeans at this settlement.
     * @param missionary The missionary in this settlement (or null).
     */
    public ServerIndianSettlement(Game game, Player owner, String name,
                                  Tile tile, boolean isCapital,
                                  UnitType learnableSkill,
                                  Unit missionary) {
        super(game, owner, name, tile);

        setGoodsContainer(new GoodsContainer(game, this));
        this.learnableSkill = learnableSkill;
        setCapital(isCapital);
        this.missionary = missionary;

        convertProgress = 0;
        updateWantedGoods();
    }

    /**
     * Creates a new ServerIndianSettlement from a template.
     *
     * @param game The {@code Game} in which this object belong.
     * @param owner The {@code Player} owning this settlement.
     * @param tile The location of the {@code IndianSettlement}.
     * @param template The template {@code IndianSettlement} to copy.
     */
    public ServerIndianSettlement(Game game, Player owner, Tile tile,
                                  IndianSettlement template) {
        super(game, owner, template.getName(), tile);

        setLearnableSkill(template.getLearnableSkill());
        setCapital(template.isCapital());
        // FIXME: the template settlement might have additional owned units
        for (Unit unit: template.getUnitList()) {
            Unit newUnit = new ServerUnit(game, this,
                                          unit);//-vis: safe, not on map yet
            add(newUnit);
            addOwnedUnit(newUnit);
        }
        Unit missionary = template.getMissionary();
        if (missionary != null) {
            this.missionary = new ServerUnit(game, this,
                                             missionary);//-vis: safe not on map
        }
        setConvertProgress(template.getConvertProgress());
        setLastTribute(template.getLastTribute());
        setGoodsContainer(new GoodsContainer(game, this));
        final Specification spec = getSpecification();
        for (Goods goods : template.getCompactGoodsList()) {
            GoodsType type = spec.getGoodsType(goods.getType().getId());
            addGoods(type, goods.getAmount());
        }
        setWantedGoods(template.getWantedGoods());
    }


    /**
     * Starts a new turn for a player.
     *
     * @param random A pseudo-random number source.
     * @param cs A {@code ChangeSet} to update.
     */
    public void csStartTurn(Random random, ChangeSet cs) {
        final Unit missionary = getMissionary();
        if (missionary == null) return;
        final Player other = missionary.getOwner();
        final Tile tile = getTile();
        final Turn turn = getGame().getTurn();

        // Check for braves converted by missionaries
        float convert = getConvertProgress();
        float cMiss = missionary.apply(missionary.getType().getSkill(),
                                       turn, Modifier.CONVERSION_SKILL);
        // The convert rate increases by a percentage of the current alarm.
        int alarm = Math.min(getAlarm(other).getValue(), Tension.TENSION_MAX);
        float cAlarm = missionary.apply(alarm, turn,
            Modifier.CONVERSION_ALARM_RATE);
        convert += cMiss + (cAlarm - alarm);
        logger.finest("Conversion at " + getName() + " alarm=" + alarm
            + " " + convert
            + " = " + getConvertProgress() + " + " + cMiss + " + " + cAlarm);
        ServerColony colony = (ServerColony)tile.getNearestSettlement(other,
            MAX_CONVERT_DISTANCE, true);
        if (convert < (float)getType().getConvertThreshold()
            || (getUnitCount() + tile.getUnitCount()) <= 2
            || colony == null) {
            setConvertProgress((int)Math.floor(convert));
        } else {
            setConvertProgress(0);
            // FIXME: fix native AI to put the units just hanging
            // around (as distinct to those with DefendSettlement
            // missions) into the settlement so we can ignore the
            // tile-residents.
            ServerUnit brave = (ServerUnit)getRandomMember(logger, "Convert",
                getAllUnitsList(), random);
            colony.csAddConvert(brave, cs);
        }
    }

    /**
     * Add a standard number of units to this settlement and tile.  If
     * a pseudo-random number source is provided use it to pick a
     * random number of units within the ranges provided by the
     * settlement type, otherwise use the average.
     *
     * @param random An optional pseudo-random number source.
     */
    public void addUnits(Random random) {
        int low = getType().getMinimumSize();
        int high = getType().getMaximumSize();
        int count = (random == null) ? (high + low) / 2
            : randomInt(logger, "Units at " + getName(), random, high - low + 1)
                + low;
        addUnits(count);
    }

    /**
     * Add a given number of units to the settlement.
     *
     * @param count The number of units to add.
     */
    public void addUnits(int count) {
        final Specification spec = getSpecification();
        final Game game = getGame();
        final UnitType brave = spec.getDefaultUnitType(getOwner());

        for (int i = 0; i < count; i++) {
            Unit unit = new ServerUnit(game, this, getOwner(), brave,
                                       brave.getDefaultRole());
            unit.changeHomeIndianSettlement(this);
            unit.setLocation(this);
        }
    }

    /**
     * Convenience function to remove an amount of goods.
     *
     * @param type The {@code GoodsType} to remove.
     * @param amount The amount of goods to remove.
     */
    private void consumeGoods(GoodsType type, int amount) {
        if (getGoodsCount(type) > 0) {
            amount = Math.min(amount, getGoodsCount(type));
            removeGoods(type, amount);
        }
    }

    /**
     * Sets alarm towards the given player.
     *
     * -til: Might change tile appearance through most hated state
     *
     * @param player The {@code Player} to set the alarm level for.
     * @param newAlarm The new alarm value.
     */
    @Override
    public void setAlarm(Player player, Tension newAlarm) {
        if (player != null && player != owner) {
            super.setAlarm(player, newAlarm);
            updateMostHated();
        }
    }

    /**
     * Removes all alarm towards the given player.  Used the a player leaves
     * the game.
     *
     * -til: Might change tile appearance through most hated state
     *
     * @param player The {@code Player} to remove the alarm for.
     */
    public void removeAlarm(Player player) {
        if (player != null) {
            alarm.remove(player);
            updateMostHated();
        }
    }

    /**
     * Updates the most hated nation of this settlement.
     * Needs to be public so it can be set by backwards compatibility code
     * in FreeColServer.loadGame.
     *
     * -til: This might change the tile appearance.
     *
     * @return True if the most hated nation changed.
     */
    private boolean updateMostHated() {
        final Player old = this.mostHated;
        final Predicate<Player> hatedPred = p -> {
            Tension alarm = getAlarm(p);
            return alarm != null && alarm.getLevel() != Tension.Level.HAPPY;
        };
        final Comparator<Player> mostHatedComp
            = Comparator.comparingInt(p -> getAlarm(p).getValue());
        this.mostHated = maximize(getGame().getLiveEuropeanPlayers(),
                                  hatedPred, mostHatedComp);
        return this.mostHated != old;
    }

    /**
     * Change the alarm level of this settlement by a given amount.
     *
     * -til: Might change tile appearance through most hated state
     *
     * @param player The {@code Player} the alarm level changes wrt.
     * @param amount The amount to change the alarm by.
     * @return True if the {@code Tension.Level} of the
     *     settlement alarm changes as a result of this change.
     */
    private boolean changeAlarm(Player player, int amount) {
        Tension alarm = getAlarm(player);
        if (alarm == null) {
            initializeAlarm(player);
            alarm = getAlarm(player);
        }
        Tension.Level oldLevel = alarm.getLevel();
        alarm.modify(amount);
        boolean change = updateMostHated();
        return change || oldLevel != alarm.getLevel();
    }

    /**
     * Modifies the alarm level towards the given player due to an event
     * at this settlement, and propagate the alarm upwards through the
     * tribe.
     *
     * -til: Might change tile appearance through most hated state
     *
     * @param player The {@code Player} to modify alarm for.
     * @param add The amount to add to the current alarm level.
     * @param propagate If true, propagate the alarm change upward to the
     *     owning player.
     * @param cs A {@code ChangeSet} to update.
     * @return True if the alarm changed.
     */
    private boolean csChangeAlarm(Player player, int add, boolean propagate,
                                  ChangeSet cs) {
        boolean change = changeAlarm(player, add);
        if (propagate) {
            // Propagate alarm upwards.  Capital has a greater impact.
            ((ServerPlayer)getOwner()).csModifyTension(player,
                ((isCapital()) ? add : add/2), this, cs);
        }
        logger.finest("Alarm at " + getName()
            + " toward " + player.getName()
            + " modified by " + add
            + " now = " + getAlarm(player).getValue());
        return change;
    }

    /**
     * Modifies the alarm level towards the given player due to an event
     * at this settlement, and propagate the alarm upwards through the
     * tribe.
     *
     * +til: Handles tile visibility changes.
     *
     * @param player The {@code Player} to modify alarm for.
     * @param add The amount to add to the current alarm level.
     * @param propagate If true, propagate the alarm change upward to the
     *     owning player.
     * @param cs A {@code ChangeSet} to update.
     * @return True if the alarm changed and the tile added.
     */
    public boolean csModifyAlarm(Player player, int add, boolean propagate,
                                 ChangeSet cs) {
        Tile copied = getTile().getTileToCache();
        boolean change = csChangeAlarm(player, add, propagate, cs);//-til
        if (change) {
            getTile().cacheUnseen(copied);//+til
            cs.add(See.perhaps(), this);
        }
        return change;
    }

    /**
     * Changes the missionary for this settlement and updates other players.
     *
     * +vis: Handles the visibility implications.
     * +til: Handles the tile appearance change.
     *
     * @param missionary The new missionary for this settlement.
     * @param cs A {@code ChangeSet} to update.
     */
    public void csChangeMissionary(Unit missionary, ChangeSet cs) {
        final Unit old = getMissionary();
        if (missionary == old) return;

        final Tile tile = getTile();
        final Player newOwner = (missionary == null) ? null
            : missionary.getOwner();
        tile.cacheUnseen(newOwner);//+til

        if (old != null) {
            final Player oldOwner = old.getOwner(); 
            setMissionary(null);//-vis(oldOwner),-til
            tile.updateIndianSettlement(oldOwner);
            ((ServerUnit)old).csRemove(See.only(oldOwner),
                null, cs);//-vis(oldOwner)
            cs.add(See.only(oldOwner), tile);
            oldOwner.invalidateCanSeeTiles();//+vis(oldOwner)
        }

        if (missionary != null) {
            setMissionary(missionary);//-vis(newOwner)
            // Take the missionary off the map, and give it a fake
            // location at the settlement, bypassing the normal
            // validity checks.
            missionary.setLocation(null);//-vis(newOwner)
            missionary.setLocationNoUpdate(this);//-vis(newOwner),-til
            missionary.setMovesLeft(0);
            cs.add(See.only(newOwner), missionary);
            setConvertProgress(0);
            csChangeAlarm(newOwner, ALARM_NEW_MISSIONARY, true, cs);//-til
            tile.updateIndianSettlement(newOwner);

            cs.add(See.only(newOwner),
                ((ServerPlayer)newOwner).collectNewTiles(getMissionaryVisibleTiles()));
            cs.add(See.perhaps().always(newOwner), tile);
            newOwner.invalidateCanSeeTiles();//+vis(newOwner)
        }
    }

    /**
     * Kills the missionary at this settlement.
     *
     * @param destroy If true, the settlement is destroyed, if false the
     *     missionary is denounced, if null do not generate a message.
     * @param cs A {@code ChangeSet} to update.
     */
    public void csKillMissionary(Boolean destroy, ChangeSet cs) {
        Unit missionary = getMissionary();
        if (missionary == null) return;
        csChangeMissionary(null, cs);
        
        // Inform the enemy of loss of mission
        Player missionaryOwner = missionary.getOwner();
        if (destroy != null) {
            if (destroy) {
                cs.addMessage(missionaryOwner,
                    new ModelMessage(ModelMessage.MessageType.UNIT_LOST,
                                     "model.indianSettlement.mission.destroyed",
                                     this)
                        .addStringTemplate("%settlement%",
                            getLocationLabelFor(missionaryOwner)));
            } else {
                cs.addMessage(missionaryOwner,
                    new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                     "model.indianSettlement.mission.denounced",
                                     this)
                        .addStringTemplate("%settlement%",
                            getLocationLabelFor(missionaryOwner)));
            }
        }
    }

    /**
     * Equip a unit for a specific role.
     *
     * @param unit The {@code Unit} to equip.
     * @param role The {@code Role} to equip for.
     * @param roleCount The role count.
     * @param random A pseudo-random number source.
     * @param cs A {@code ChangeSet} to update.
     * @return True if the equipping succeeds.
     */
    public boolean csEquipForRole(Unit unit, Role role, int roleCount,
                                  Random random, ChangeSet cs) {
        boolean ret = equipForRole(unit, role, roleCount);

        if (ret) {
            cs.add(See.only(getOwner()), getTile());
        }
        return ret;
    }

    /**
     * Check if tension has increased with respect to an enemy.
     *
     * @param enemy The enemy {@code Player}.
     * @param oldLevel The previous tension {@code Level}.
     * @param cs A {@code ChangeSet} to update.
     */
    public void csCheckTension(Player enemy, Tension.Level oldLevel,
                               ChangeSet cs) {
        Tension newTension = getAlarm(enemy);
        Tension.Level newLevel = (newTension == null) ? null
            : newTension.getLevel();
        if (oldLevel == null || oldLevel == newLevel
            || !hasContacted(enemy) || !enemy.hasExplored(getTile())) return;

        // Tension has changed, update the enemy
        cs.add(See.only(null).perhapsOnly(enemy), this);

        // No messages about improving tension
        if (newLevel == null
            || oldLevel.getLimit() > newLevel.getLimit()) return;

        // Send a message to the enemy that tension has increased.
        String key = "model.player.alarmIncrease." + getAlarm(enemy).getKey();
        if (!Messages.containsKey(key)) return;
        cs.addMessage(enemy,
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                             key, this)
                .addStringTemplate("%nation%", getOwner().getNationLabel())
                .addStringTemplate("%enemy%", enemy.getNationLabel())
                .addName("%settlement%", getName()));
    }
        
    // Implement TurnTaker

    /**
     * New turn for this native settlement.
     *
     * @param random A {@code Random} number source.
     * @param lb A {@code LogBuilder} to log to.
     * @param cs A {@code ChangeSet} to update.
     */
    @Override
    public void csNewTurn(Random random, LogBuilder lb, ChangeSet cs) {
        final Specification spec = getSpecification();
        final Player owner = getOwner();
        lb.add(this);

        // Produce goods.
        List<GoodsType> goodsList = spec.getGoodsTypeList();
        for (GoodsType g : goodsList) {
            addGoods(g.getStoredAs(), getTotalProductionOf(g));
        }

        // Consume goods.
        for (GoodsType g : goodsList) {
            consumeGoods(g, getConsumptionOf(g));
        }

        // Now check the food situation
        int storedFood = getGoodsCount(spec.getPrimaryFoodType());
        if (storedFood <= 0 && getUnitCount() > 0) {
            Unit victim = getRandomMember(logger, "Choose starver",
                                          getUnits(), random);
            ((ServerUnit)victim).csRemove(See.only(owner),
                this, cs);//-vis(owner)
            lb.add(" FAMINE");
        }
        if (getUnitCount() <= 0) {
            if (tile.isEmpty()) {
                lb.add(" COLLAPSED, ");
                ((ServerPlayer)owner).csDisposeSettlement(this, cs);//+vis(owner)
                return;
            }
            tile.getFirstUnit().setLocation(this);//-vis,til: safe in settlement
        }

        // Check for new resident.
        // Alcohol also contributes to create children.
        GoodsType foodType = spec.getPrimaryFoodType();
        GoodsType rumType = spec.getGoodsType("model.goods.rum");
        List<UnitType> unitTypes
            = spec.getUnitTypesWithAbility(Ability.BORN_IN_INDIAN_SETTLEMENT);
        if (!unitTypes.isEmpty()
            && (getGoodsCount(foodType) + 4 * getGoodsCount(rumType)
                > FOOD_PER_COLONIST + KEEP_RAW_MATERIAL)) {
            if (ownedUnits.size() <= getType().getMaximumSize()) {
                // Allow one more brave than the initially generated
                // number.  This is more than sufficient. Do not
                // increase the amount without discussing it on the
                // developer's mailing list first.
                UnitType type = getRandomMember(logger, "Choose birth",
                                                unitTypes, random);
                Unit unit = new ServerUnit(getGame(), getTile(), owner,
                                           type);//-vis: safe within settlement
                consumeGoods(rumType, FOOD_PER_COLONIST/4);
                // New units quickly go out of their city and start annoying.
                addOwnedUnit(unit);
                unit.changeHomeIndianSettlement(this);
                lb.add(" new ", unit);
            }
            // Consume the food anyway
            consumeGoods(foodType, FOOD_PER_COLONIST);
        }

        // Try to breed horses
        // FIXME: Make this generic.
        GoodsType horsesType = spec.getGoodsType("model.goods.horses");
        // FIXME: remove this
        GoodsType grainType = spec.getGoodsType("model.goods.grain");
        int foodProdAvail = getTotalProductionOf(grainType) - getFoodConsumption();
        if (foodProdAvail > 0
            && getGoodsCount(horsesType) >= horsesType.getBreedingNumber()) {
            int nHorses = Math.min(MAX_HORSES_PER_TURN, foodProdAvail);
            addGoods(horsesType, nHorses);
            lb.add(" bred ", nHorses, " horses");
        }

        getGoodsContainer().removeAbove(getWarehouseCapacity());
        updateWantedGoods();
        cs.add(See.only(owner), this);
        lb.add(", ");
    }
}
