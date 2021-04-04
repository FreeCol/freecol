/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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

package net.sf.freecol.server.ai.mission;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.option.GameOptions;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for demanding goods from a specified player.
 */
public final class IndianDemandMission extends Mission {

    private static final Logger logger = Logger.getLogger(IndianDemandMission.class.getName());

    public static final String TAG = "indianDemandMission";

    /** The minimum amount of goods to demand. */
    private static final int GOODS_DEMAND_MIN = 30;
    
    /** The tag for this mission. */
    private static final String tag = "AI native demander";

    /** Predicates for demand goods selection. */
    private static final List<Predicate<GoodsType>> selectPredicates
        = new ArrayList<>();
    static {
        selectPredicates.add(GoodsType::getMilitary);
        selectPredicates.add(GoodsType::isBuildingMaterial);
        selectPredicates.add(GoodsType::isTradeGoods);
        selectPredicates.add(GoodsType::isRefined);
    };
    
    /** The colony to demand from. */
    private Colony colony;

    /** Whether the demand has been made or not. */
    private boolean demanded;

    /** Whether the demand succeeded. */
    private boolean succeeded;


    /**
     * Creates a mission for the given {@code AIUnit}.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The {@code AIUnit} this mission is created for.
     * @param target The {@code Colony} receiving the gift.
     */
    public IndianDemandMission(AIMain aiMain, AIUnit aiUnit, Colony target) {
        super(aiMain, aiUnit);

        setTarget(target);
        this.demanded = this.succeeded = false;
    }

    /**
     * Creates a new {@code IndianDemandMission} from a reader.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The {@code AIUnit} this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public IndianDemandMission(AIMain aiMain, AIUnit aiUnit,
                               FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
    }


    /**
     * Checks if the unit is carrying a tribute (goods).
     *
     * @return True if the unit is carrying goods.
     */
    private boolean hasTribute() {
        return hasMissionTribute(getAIUnit());
    }

    /**
     * Checks if a unit is carrying a tribute.
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @return True if the unit is carrying goods.
     */
    private static boolean hasMissionTribute(AIUnit aiUnit) {
        return aiUnit.getUnit().hasGoodsCargo();
    }

    /**
     * Set the result of the demand.
     *
     * @param result The result of making the demand.
     */
    public void setSucceeded(boolean result) {
        this.succeeded = result;
    }

    /**
     * Selects the most desirable goods from the colony.
     *
     * @param target The target {@code Colony}.
     * @return The goods to demand.
     */
    private Goods selectGoods(Colony target) {
        final Specification spec = getSpecification();
        final List<GoodsType> goodsTypes = transform(spec.getGoodsTypeList(),
            gt -> target.getGoodsCount(gt) > 0);
        final int dx = spec.getInteger(GameOptions.NATIVE_DEMANDS) + 1;
        final Game game = target.getGame();
        final Market market = target.getOwner().getMarket();
        final Comparator<Goods> marketPrice
            = Comparator.comparingInt(g -> market.getSalePrice(g));
        final Function<Goods, Goods> makeGoods = g ->
            new Goods(game, target, g.getType(), capAmount(g.getAmount(), dx));
        final Tension.Level tension = getUnit().getOwner()
            .getTension(target.getOwner()).getLevel();
        final GoodsType food = spec.getPrimaryFoodType();
        final int cutoff = capAmount(target.getGoodsCount(food), dx);
        Goods goods = null;

        // When content ask for food at cutoff
        if (tension.compareTo(Tension.Level.CONTENT) <= 0
            && target.getGoodsCount(food) >= cutoff) {
            goods = new Goods(getGame(), target, food, cutoff);
        }

        // When displeased, ask for expensive non-food or military
        if (goods == null
            && tension.compareTo(Tension.Level.DISPLEASED) <= 0) {
            final Predicate<Goods> angryPred = g ->
                !g.isFoodType() && !g.getType().getMilitary();
            goods = maximize(target.getCompactGoodsList(), angryPred, marketPrice);
            if (goods != null) goods = makeGoods.apply(goods);
        }

        // Otherwise try military, building, trade, refined goods in order,
        if (goods == null) {
            GoodsType goodsType = first(flatten(selectPredicates,
                    pred -> transform(goodsTypes, gt -> pred.test(gt)).stream()));
            if (goodsType != null) {
                goods = new Goods(getGame(), target, goodsType,
                    capAmount(target.getGoodsCount(goodsType), dx));
            }
        }

        // Finally just go for expense
        if (goods == null) {
            goods = maximize(target.getCompactGoodsList(), marketPrice);
            if (goods != null) goods = makeGoods.apply(goods);
        }

        return goods;
    }

    private int capAmount(int amount, int difficulty) {
        return Math.min(Math.max(amount * difficulty / 6, GOODS_DEMAND_MIN),
                        GoodsContainer.CARGO_SIZE); // One load of goods max
    }

    private static IndianSettlement getHome(AIUnit aiUnit) {
        return aiUnit.getUnit().getHomeIndianSettlement();
    }

    /**
     * Why would this mission be invalid with the given unit?
     *
     * @param aiUnit The {@code AIUnit} to test.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidUnitReason(AIUnit aiUnit) {
        String reason = invalidAIUnitReason(aiUnit);
        IndianSettlement home;
        return (reason != null)
            ? reason
            : ((home = getHome(aiUnit)) == null || home.isDisposed())
            ? "home-destroyed"
            : null;
    }

    /**
     * Why would an IndianDemandMission be invalid with the given
     * unit and colony.
     *
     * @param aiUnit The {@code AIUnit} to test.
     * @param colony The {@code Colony} to test.
     * @return A reason why the mission would be invalid with the unit
     *     and colony or null if none found.
     */
    private static String invalidColonyReason(AIUnit aiUnit, Colony colony) {
        String reason = invalidTargetReason(colony);
        if (reason != null) return reason;
        final Unit unit = aiUnit.getUnit();
        final Player owner = unit.getOwner();
        Player targetPlayer = colony.getOwner();
        switch (owner.getStance(targetPlayer)) {
        case UNCONTACTED: case PEACE: case ALLIANCE:
            return "bad-stance";
        case WAR: case CEASE_FIRE:
            Tension tension = unit.getHomeIndianSettlement()
                .getAlarm(targetPlayer);
            if (tension != null && tension.getLevel()
                .compareTo(Tension.Level.CONTENT) <= 0) return "happy";
            break;
        }
        return null;
    }

    /**
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidMissionReason(AIUnit aiUnit) {
        return invalidUnitReason(aiUnit);
    }

    /**
     * Why would this mission be invalid with the given AI unit and location?
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @param loc The {@code Location} to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidMissionReason(AIUnit aiUnit, Location loc) {
        String reason = invalidMissionReason(aiUnit);
        return (reason != null)
            ? reason
            : (loc instanceof Colony)
            ? invalidColonyReason(aiUnit, (Colony)loc)
            : (loc instanceof IndianSettlement)
            ? invalidTargetReason(loc, aiUnit.getUnit().getOwner())
            : Mission.TARGETINVALID;
    }


    // Implement Mission
    //   Inherit dispose, getBaseTransportPriority, isOneTime

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportDestination() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTarget() {
        return (this.demanded && this.succeeded)
            ? getUnit().getHomeIndianSettlement()
            : this.colony;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTarget(Location target) {
        if (target instanceof Colony) {
            this.colony = (Colony)target;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location findTarget() {
        return getTarget();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String invalidReason() {
        return invalidMissionReason(getAIUnit(), getTarget());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        String reason = invalidReason();
        if (reason != null) return lbFail(lb, false, reason);

        final AIUnit aiUnit = getAIUnit();
        final Unit unit = getUnit();
        final IndianSettlement is = unit.getHomeIndianSettlement();
        Direction d;

        if (!this.demanded) {
            Unit.MoveType mt = travelToTarget(getTarget(), null, lb);
            switch (mt) {
            case MOVE_HIGH_SEAS: case MOVE_NO_MOVES: case MOVE_ILLEGAL:
                return lbWait(lb);

            case MOVE_NO_REPAIR:
                return lbFail(lb, false, AIUNITDIED);

            case MOVE_NO_TILE:
                return this;

            case ATTACK_SETTLEMENT: // Arrived?
                d = unit.getTile().getDirection(getTarget().getTile());
                if (d != null) break; // Yes, arrived at target
                // Fall through
            case ATTACK_UNIT: // Something is blocking our path
                Location blocker = resolveBlockage(aiUnit, getTarget());
                if (blocker == null) {
                    moveRandomly(tag, null);
                    return lbWait(lb);
                }
                d = unit.getTile().getDirection(blocker.getTile());
                return (AIMessage.askAttack(aiUnit, d)) ? lbAttack(lb, blocker)
                    : lbWait(lb);

            default:
                return lbMove(lb, mt);
            }

            // Load the goods.
            lbAt(lb);
            AIPlayer aiPlayer = getAIPlayer();
            Colony colony = (Colony)getTarget();
            Player enemy = colony.getOwner();
            Goods goods = selectGoods(colony);
            GoodsType type = (goods == null) ? null : goods.getType();
            int amount = (goods == null) ? 0 : goods.getAmount();
            if (goods == null) {
                if (!enemy.checkGold(1)) {
                    return lbDone(lb, false, "empty handed");
                }
                amount = enemy.getGold() / 20;
                if (amount == 0) amount = enemy.getGold();
            }
            this.demanded = AIMessage.askIndianDemand(aiPlayer, unit, colony,
                                                      type, amount, null);
            lb.add(", demand made ", (this.demanded) ? "" : "un",
                   "successfully");
            return lbWait(lb);

        } else if (!this.succeeded) { // Consider attacking if not content.
            Colony colony = (Colony)getTarget();
            Player enemy = colony.getOwner();
            int unitTension = (is == null) ? 0 : is.getAlarm(enemy).getValue();
            int tension = Math.max(unitTension,
                unit.getOwner().getTension(enemy).getValue());
            d = unit.getTile().getDirection(colony.getTile());
            if (d != null && tension >= Tension.Level.CONTENT.getLimit()) {
                if (AIMessage.askAttack(aiUnit, d)) lbAttack(lb, colony);
            }
            return lbDone(lb, false, "refused at ", colony);

        } else if (!hasTribute()) {
            return lbDone(lb, true, "accepted gold at ", colony);

        } else { // Take the goods home
            for (;;) {
                Unit.MoveType mt = travelToTarget(getTarget(),
                    CostDeciders.avoidSettlementsAndBlockingUnits(), lb);
                switch (mt) {
                case MOVE: // Arrived
                    break;
                    
                case MOVE_HIGH_SEAS: case MOVE_NO_MOVES: case MOVE_ILLEGAL:
                    return lbWait(lb);
                    
                case MOVE_NO_REPAIR:
                    return lbFail(lb, false, AIUNITDIED);
                    
                case MOVE_NO_TILE:
                    return this;
                    
                default:
                    return lbMove(lb, mt);
                }
                
                // Unload the goods
                lbAt(lb);
                GoodsContainer container = unit.getGoodsContainer();
                for (Goods goods : container.getCompactGoodsList()) {
                    Goods tribute = container.removeGoods(goods.getType());
                    is.addGoods(tribute);
                }
                return lbDone(lb, false, "unloaded tribute");
            }
        }
    }


    // Serialization

    private static final String COLONY_TAG = "colony";
    private static final String DEMANDED_TAG = "demanded";
    private static final String SUCCEEDED_TAG = "succeeded";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (this.colony != null) {
            xw.writeAttribute(COLONY_TAG, this.colony.getId());
        }

        xw.writeAttribute(DEMANDED_TAG, this.demanded);

        xw.writeAttribute(SUCCEEDED_TAG, this.succeeded);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        this.colony = xr.getAttribute(getGame(), COLONY_TAG,
                                      Colony.class, (Colony)null);

        this.demanded = xr.getAttribute(DEMANDED_TAG, false);

        this.succeeded = xr.getAttribute(SUCCEEDED_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return TAG; }
}
