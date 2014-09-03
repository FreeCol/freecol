/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.UnitListOption;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.server.control.ChangeSet;


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
        ServerPlayer player = (ServerPlayer) getOwner();
        List<RandomChoice<UnitType>> recruits
            = player.generateRecruitablesList();
        if (spec.hasOption(GameOptions.IMMIGRANTS)) {
            UnitListOption option
                = (UnitListOption)spec.getOption(GameOptions.IMMIGRANTS);
            for (AbstractUnit immigrant : option.getOptionValues()) {
                addRecruitable(immigrant.getType(spec));
            }
        } else {
            // @compat 0.10.3
            for (int index = 0; index < RECRUIT_COUNT; index++) {
                String optionId = "model.option.recruitable.slot" + index;
                if (spec.hasOption(optionId)) {
                    String unitTypeId = spec.getString(optionId);
                    if (unitTypeId != null) {
                        addRecruitable(spec.getUnitType(unitTypeId));
                    }
                }
            }
            // end @compat
        }
        while (recruitables.size() < RECRUIT_COUNT) {
            addRecruitable(RandomChoice
                .getWeightedRandom(logger, "Recruits", recruits, random));
        }
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
     * Increases the price for a unit.
     *
     * @param unitType The <code>UnitType</code>, trained or purchased
     * @param price The current price of the unit
     */
    public void increasePrice(UnitType unitType, int price) {
        final Specification spec = getSpecification();
        String baseOption = GameOptions.PRICE_INCREASE_PER_TYPE;
        String name = unitType.getId().substring(unitType.getId().lastIndexOf('.'));
        String option = (spec.getBoolean(baseOption))
            ? "model.option.priceIncrease" + name
            : "model.option.priceIncrease";
        int increase = (spec.hasOption(option)) ? spec.getInteger(option) : 0;
        if (increase != 0) {
            unitPrices.put(unitType, Integer.valueOf(price + increase));
        }
    }

    /**
     * New turn for this colony tile.
     * TODO: give Europe a shipyard and remove this
     *
     * @param random A <code>Random</code> number source.
     * @param lb A <code>LogBuilder</code> to log to.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csNewTurn(Random random, LogBuilder lb, ChangeSet cs) {
        logger.finest("ServerEurope.csNewTurn, for " + this);

        for (Unit unit : getUnitList()) {
            if (unit.isNaval() && unit.isDamaged()) {
                ((ServerUnit)unit).csRepairUnit(cs);
            }
        }
    }


    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "serverEurope"
     */
    public String getServerXMLElementTagName() {
        return "serverEurope";
    }
}
