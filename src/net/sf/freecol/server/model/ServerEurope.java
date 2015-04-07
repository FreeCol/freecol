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
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.UnitListOption;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.RandomChoice;
import static net.sf.freecol.common.util.RandomUtils.*;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.See;


/**
 * The server version of Europe.
 */
public class ServerEurope extends Europe implements ServerModelObject {

    private static final Logger logger = Logger.getLogger(ServerEurope.class.getName());


    /**
     * Trivial constructor required for all ServerModelObjects.
     */
    public ServerEurope(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new ServerEurope.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param owner The <code>Player</code> that will be using this object of
     *            <code>Europe</code>.
     */
    public ServerEurope(Game game, Player owner) {
        super(game, owner);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equipForRole(Unit unit, Role role, int roleCount) {
        if (!unit.roleIsAvailable(role)) return false;

        // Get the change in goods
        List<AbstractGoods> required = unit.getGoodsDifference(role, roleCount);

        // Check the pricing
        int price = priceGoods(required);
        if (price < 0 || !unit.getOwner().checkGold(price)) return false;

        // Sell any excess
        final ServerPlayer owner = (ServerPlayer)getOwner();
        for (AbstractGoods ag : required) {
            if (ag.getAmount() >= 0) continue;
            if (owner.canTrade(ag.getType(), Market.Access.EUROPE)) {
                int rm = owner.sell(null, ag.getType(), -ag.getAmount());
                if (rm > 0) {
                    owner.addExtraTrade(new AbstractGoods(ag.getType(), rm));
                }
            }
        }
        // Buy what is needed
        for (AbstractGoods ag : required) {
            if (ag.getAmount() <= 0) continue;
            int m = owner.buy(null, ag.getType(), ag.getAmount());
            if (m > 0) {
                owner.addExtraTrade(new AbstractGoods(ag.getType(), -m));
            }
        }

        unit.changeRole(role, roleCount);
        return true;
    }

    /**
     * Generates the initial recruits for this player.  Recruits may
     * be determined by the difficulty level, or generated randomly.
     *
     * @param random A pseudo-random number source.
     */
    public void initializeMigration(Random random) {
        final Specification spec = getGame().getSpecification();
        UnitType unitType;

        if (spec.hasOption(GameOptions.IMMIGRANTS)) {
            UnitListOption option
                = (UnitListOption)spec.getOption(GameOptions.IMMIGRANTS);
            for (AbstractUnit au : option.getOptionValues()) {
                unitType = au.getType(spec);
                addRecruitable(au.getType(spec));
            }
        } else {
            // @compat 0.10.3
            for (int index = 0;; index++) {
                String optionId = "model.option.recruitable.slot" + index;
                String unitTypeId;
                if (spec.hasOption(optionId)
                    && (unitTypeId = spec.getString(optionId)) != null
                    && (unitType = spec.getUnitType(unitTypeId)) != null
                    && addRecruitable(unitType)) continue;
                break; // Failed
            }
            // end @compat
        }

        // Fill out to the full amount of recruits if the above failed
        List<RandomChoice<UnitType>> recruits = generateRecruitablesList();
        do {
            unitType = RandomChoice.getWeightedRandom(logger, "Recruits",
                                                      recruits, random);
        } while (addRecruitable(unitType));
    }

    /**
     * Increases the base price and lower cap for recruits.
     */
    public void increaseRecruitmentDifficulty() {
        final Specification spec = getSpecification();
        recruitPrice += spec.getInteger(GameOptions.RECRUIT_PRICE_INCREASE);
        recruitLowerCap += spec.getInteger(GameOptions.LOWER_CAP_INCREASE);
    }

    /**
     * Extract the recruitable at a given slot, and replace it with
     * the given new recruitable type.
     *
     * Note that we shift the old units down, because the AI always
     * recruits from the lowest slot.
     *
     * @param slot The slot to recruit with.
     * @param random A pseudo-random number source.
     * @return The recruited <code>UnitType</code>.
     */
    public UnitType extractRecruitable(int slot, Random random) {
        // An invalid slot is normal when the player has no control over
        // recruit type.
        final int count = MigrationType.getMigrantCount();
        int index = (MigrationType.specificMigrantSlot(slot))
            ? MigrationType.migrantSlotToIndex(slot)
            : randomInt(logger, "Choose emigrant", random, count);
        UnitType result = recruitables.get(index);
        for (int i = index; i < count-1; i++) {
            recruitables.set(i, recruitables.get(i+1));
        }
        recruitables.set(count-1, RandomChoice.getWeightedRandom(logger,
                "Replace recruit", generateRecruitablesList(), random));
        return result;
    }

    /**
     * Generate a weighted list of unit types recruitable by this player.
     *
     * @return A weighted list of recruitable unit types.
     */
    public List<RandomChoice<UnitType>> generateRecruitablesList() {
        final Player owner = getOwner();
        List<RandomChoice<UnitType>> recruits = new ArrayList<>();
        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            if (unitType.isRecruitable()
                && owner.hasAbility(Ability.CAN_RECRUIT_UNIT, unitType)) {
                recruits.add(new RandomChoice<>(unitType,
                        unitType.getRecruitProbability()));
            }
        }
        return recruits;
    }

    /**
     * Replace any non-recruitable recruits.
     *
     * @param random A pseudo-random number source.
     * @return True if any recruit was replaced.
     */
    public boolean replaceRecruits(Random random) {
        List<RandomChoice<UnitType>> recruits = generateRecruitablesList();
        boolean result = false;
        int i = 0;
        for (UnitType ut : recruitables) {
            if (hasAbility(Ability.CAN_RECRUIT_UNIT, ut)) continue;
            UnitType newType = RandomChoice.getWeightedRandom(logger,
                "Replace recruit", recruits, random);
            recruitables.set(i, newType);
            result = true;
            i++;
        }
        return result;
    }

    /**
     * Generate new recruits following a Fountain of Youth discovery.
     *
     * FIXME: Get rid of this, it is only used because the AI is stupid.
     *
     * @param n The number of new units.
     * @param random A pseudo-random number source.
     */
    public void generateFountainRecruits(int n, Random random) {
        final Game game = getGame();
        final Player owner = getOwner();
        List<RandomChoice<UnitType>> recruits = generateRecruitablesList();
        for (int k = 0; k < n; k++) {
            UnitType ut = RandomChoice.getWeightedRandom(logger, "Choose FoY",
                                                         recruits, random);
            new ServerUnit(game, this, owner, ut);//-vis: safe, Europe
        }
    }

    /**
     * Increases the price for a unit.
     *
     * @param unitType The <code>UnitType</code>, trained or purchased
     * @param price The current price of the unit
     */
    public void increasePrice(UnitType unitType, int price) {
        final Specification spec = getSpecification();
        String baseOption = GameOptions.PRICE_INCREASE_PER_TYPE;
        String option = (spec.getBoolean(baseOption))
            ? "model.option.priceIncrease." + unitType.getSuffix()
            : "model.option.priceIncrease";
        int increase = (spec.hasOption(option)) ? spec.getInteger(option) : 0;
        if (increase != 0) {
            unitPrices.put(unitType, price + increase);
        }
    }

    /**
     * New turn for this colony tile.
     *
     * FIXME: give Europe a shipyard and remove this?
     *
     * @param random A <code>Random</code> number source.
     * @param lb A <code>LogBuilder</code> to log to.
     * @param cs A <code>ChangeSet</code> to update.
     */
    @Override
    public void csNewTurn(Random random, LogBuilder lb, ChangeSet cs) {
        logger.finest("ServerEurope.csNewTurn, for " + this);

        for (Unit unit : getUnitList()) {
            if (unit.isNaval() && unit.isDamaged()) {
                ((ServerUnit)unit).csRepairUnit(cs);
            }
        }
    }

    /**
     * Equip a unit for a specific role.
     *
     * @param unit The <code>Unit</code> to equip.
     * @param role The <code>Role</code> to equip for.
     * @param roleCount The role count.
     * @param random A pseudo-random number source.
     * @param cs A <code>ChangeSet</code> to update.
     * @return True if the equipping succeeds.
     */
    public boolean csEquipForRole(Unit unit, Role role, int roleCount,
                                  Random random, ChangeSet cs) {
        boolean ret = equipForRole(unit, role, roleCount);

        if (ret) {
            ServerPlayer serverPlayer = (ServerPlayer)getOwner();
            cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
            cs.add(See.only(serverPlayer), unit);
            serverPlayer.flushExtraTrades(random);
            serverPlayer.csFlushMarket(cs);
        }
        return ret;
    }


    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "serverEurope"
     */
    @Override
    public String getServerXMLElementTagName() {
        return "serverEurope";
    }
}
