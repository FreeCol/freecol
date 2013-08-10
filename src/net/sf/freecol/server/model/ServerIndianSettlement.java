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

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.See;


/**
 * The server version of an Indian Settlement.
 */
public class ServerIndianSettlement extends IndianSettlement
    implements ServerModelObject {

    private static final Logger logger = Logger.getLogger(ServerIndianSettlement.class.getName());

    public static final int MAX_HORSES_PER_TURN = 2;


    /**
     * Trivial constructor for all ServerModelObjects.
     */
    public ServerIndianSettlement(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new ServerIndianSettlement.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param owner The <code>Player</code> owning this settlement.
     * @param name The name for this settlement.
     * @param tile The location of the <code>IndianSettlement</code>.
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
     * @param game The <code>Game</code> in which this object belong.
     * @param owner The <code>Player</code> owning this settlement.
     * @param tile The location of the <code>IndianSettlement</code>.
     * @param template The template <code>IndianSettlement</code> to copy.
     */
    public ServerIndianSettlement(Game game, Player owner, Tile tile,
                                  IndianSettlement template) {
        super(game, owner, template.getName(), tile);

        setLearnableSkill(template.getLearnableSkill());
        setCapital(template.isCapital());
        // TODO: the template settlement might have additional owned
        // units
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
        for (Goods goods : template.getCompactGoods()) {
            GoodsType type = spec.getGoodsType(goods.getType().getId());
            addGoods(type, goods.getAmount());
        }
        wantedGoods = template.getWantedGoods();
    }


    /**
     * New turn for this native settlement.
     *
     * @param random A <code>Random</code> number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csNewTurn(Random random, ChangeSet cs) {
        logger.finest("ServerIndianSettlement.csNewTurn, for " + toString());
        ServerPlayer owner = (ServerPlayer) getOwner();
        Specification spec = getSpecification();

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
            Unit victim = Utils.getRandomMember(logger, "Choose starver",
                                                getUnitList(), random);
            cs.addDispose(See.only(owner), this, victim);//-vis(owner)
            logger.finest("Famine in " + getName());
        }
        if (getUnitCount() <= 0) {
            if (tile.isEmpty()) {
                logger.info(getName() + " collapsed.");
                owner.csDisposeSettlement(this, cs);//+vis(owner)
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
                > FOOD_PER_COLONIST + KEEP_RAW_MATERIAL)
            && ownedUnits.size() <= getType().getMaximumSize()) {
            // Allow one more brave than the initially generated number.
            // This is more than sufficient. Do not increase the amount
            // without discussing it on the developer's mailing list first.
            UnitType type = Utils.getRandomMember(logger, "Choose birth",
                                                  unitTypes, random);
            Unit unit = new ServerUnit(getGame(), getTile(), owner,
                                       type);//-vis: safe within settlement
            consumeGoods(foodType, FOOD_PER_COLONIST);
            consumeGoods(rumType, FOOD_PER_COLONIST/4);
            // New units quickly go out of their city and start annoying.
            addOwnedUnit(unit);
            unit.setHomeIndianSettlement(this);
            logger.info("New native created in " + getName()
                        + ": " + unit.getId());
        }

        // Try to breed horses
        // TODO: Make this generic.
        GoodsType horsesType = spec.getGoodsType("model.goods.horses");
        // TODO: remove this
        GoodsType grainType = spec.getGoodsType("model.goods.grain");
        int foodProdAvail = getTotalProductionOf(grainType) - getFoodConsumption();
        if (getGoodsCount(horsesType) >= horsesType.getBreedingNumber()
            && foodProdAvail > 0) {
            int nHorses = Math.min(MAX_HORSES_PER_TURN, foodProdAvail);
            addGoods(horsesType, nHorses);
            logger.finest("Settlement " + getName() + " bred " + nHorses);
        }

        getGoodsContainer().removeAbove(getWarehouseCapacity());
        updateWantedGoods();
        cs.add(See.only(owner), this);
    }

    /**
     * Convenience function to remove an amount of goods.
     *
     * @param type The <code>GoodsType</code> to remove.
     * @param amount The amount of goods to remove.
     */
    private void consumeGoods(GoodsType type, int amount) {
        if (getGoodsCount(type) > 0) {
            amount = Math.min(amount, getGoodsCount(type));
            removeGoods(type, amount);
        }
    }

    /**
     * Modifies the alarm level towards the given player due to an event
     * at this settlement, and propagate the alarm upwards through the
     * tribe.
     *
     * @param player The <code>Player</code>.
     * @param addToAlarm The amount to add to the current alarm level.
     * @return A list of settlements whose alarm level has changed.
     */
    public List<FreeColGameObject> modifyAlarm(Player player, int addToAlarm) {
        boolean change = changeAlarm(player, addToAlarm);

        // Propagate alarm upwards.  Capital has a greater impact.
        List<FreeColGameObject> modified = owner.modifyTension(player,
                ((isCapital()) ? addToAlarm : addToAlarm/2), this);
        if (change && player.hasExplored(getTile())) {
            modified.add(this);
        }
        logger.finest("Alarm at " + getName()
            + " toward " + player.getName()
            + " modified by " + addToAlarm
            + " now = " + getAlarm(player).getValue());
        return modified;
    }

    /**
     * Changes the missionary for this settlement and updates other players.
     *
     * +vis: Handles the visibility implications.
     * +til: Handles the tile appearance change.
     *
     * @param missionary The new missionary for this settlement.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csChangeMissionary(Unit missionary, ChangeSet cs) {
        Tile tile = getTile();
        Unit old = getMissionary();
        ServerPlayer oldOwner = null, newOwner = null;

        if (old != null) {
            oldOwner = (ServerPlayer)old.getOwner(); 
            setMissionary(null);//-vis(oldOwner),-til
            tile.updateIndianSettlement(oldOwner);
            cs.addDispose(See.perhaps(), tile, old);//-vis(oldOwner)
        }

        if (missionary != null) {
            newOwner = (ServerPlayer)missionary.getOwner();
            setMissionary(missionary);//-vis(newOwner)
            // Take the missionary off the map, and give it a fake
            // location at the settlement, bypassing the normal
            // validity checks.
            missionary.setLocation(null);//-vis(newOwner)
            missionary.setLocationNoUpdate(this);//-vis(newOwner),-til
            cs.add(See.only(newOwner), newOwner.exploreForSettlement(this));
            tile.updateIndianSettlement(newOwner);
        }

        tile.updatePlayerExploredTiles();//+til
        cs.add(See.perhaps().always(oldOwner), tile);
        if (oldOwner != null) oldOwner.invalidateCanSeeTiles();//+vis(oldOwner)
        if (newOwner != null) newOwner.invalidateCanSeeTiles();//+vis(newOwner)
    }

    /**
     * Kills the missionary at this settlement.
     *
     * @param messageId An optional messageId to send.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csKillMissionary(String messageId, ChangeSet cs) {
        Unit missionary = getMissionary();
        if (missionary == null) return;
        csChangeMissionary(null, cs);
        
        // Inform the enemy of loss of mission
        ServerPlayer missionaryOwner = (ServerPlayer)missionary.getOwner();
        if ("indianSettlement.mission.denounced".equals(messageId)) {
            cs.addMessage(See.only(missionaryOwner),
                new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                 messageId, this)
                    .addStringTemplate("%settlement%", 
                        getLocationNameFor(missionaryOwner)));
        } else if ("indianSettlement.mission.destroyed".equals(messageId)) {
            cs.addMessage(See.only(missionaryOwner),
                new ModelMessage(ModelMessage.MessageType.UNIT_LOST,
                                 messageId, this)
                    .addStringTemplate("%settlement%", 
                        getLocationNameFor(missionaryOwner)));
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "serverIndianSettlement"
     */
    public String getServerXMLElementTagName() {
        return "serverIndianSettlement";
    }
}
