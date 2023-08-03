/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.client.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.client.control.MapTransform;
import net.sf.freecol.client.gui.dialog.FreeColDialog;
import net.sf.freecol.client.gui.dialog.Parameters;
import net.sf.freecol.client.gui.mapviewer.MapAsyncPainter;
import net.sf.freecol.client.gui.mapviewer.MapViewer;
import net.sf.freecol.client.gui.mapviewer.MapViewerRepaintManager;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.panel.report.LabourData.UnitData;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.metaserver.ServerInfo;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
// Imports all ENUMS.
import net.sf.freecol.common.model.Constants.ArmedUnitSettlementAction;
import net.sf.freecol.common.model.Constants.BoycottAction;
import net.sf.freecol.common.model.Constants.ClaimAction;
import net.sf.freecol.common.model.Constants.MissionaryAction;
import net.sf.freecol.common.model.Constants.ScoutColonyAction;
import net.sf.freecol.common.model.Constants.ScoutIndianSettlementAction;
import net.sf.freecol.common.model.Constants.TradeAction;
import net.sf.freecol.common.model.Constants.TradeBuyAction;
import net.sf.freecol.common.model.Constants.TradeSellAction;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * The API and common reusable functionality for the overall GUI.
 */
public class GUI extends FreeColClientHolder {

    protected static final Logger logger = Logger.getLogger(GUI.class.getName());

    /** View modes. */
    public enum ViewMode {
        MOVE_UNITS,
        TERRAIN,
        MAP_TRANSFORM,
        END_TURN
    };

    /** Levels (danger, finance) for confirmEuropeanTribute(). */
    private static final String levels[] = { "low", "normal", "high" };


    /**
     * Create the GUI.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public GUI(FreeColClient freeColClient) {
        super(freeColClient);
    }


    // Implementations of high level dialogs, using the dialog primitives

    /**
     * Simple modal confirmation dialog.
     *
     * @param textKey A string to use as the message key.
     * @param okKey A key for the message on the "ok" button.
     * @param cancelKey A key for the message on the "cancel" button.
     * @return True if the "ok" button was selected.
     */
    public final boolean confirm(String textKey,
                                 String okKey, String cancelKey) {
        return confirm(null, StringTemplate.key(textKey), (ImageIcon)null,
                       okKey, cancelKey);
    }

    /**
     * Primitive modal confirmation dialog.
     *
     * @param template The {@code StringTemplate} explaining the choice.
     * @param okKey A key for the "ok" button.
     * @param cancelKey A key for the "cancel" button.
     * @return True if the "ok" button was selected.
     */
    public final boolean confirm(StringTemplate template,
                                 String okKey, String cancelKey) {
        return confirm(null, template, (ImageIcon)null,
                       okKey, cancelKey);
    }

    /**
     * GoodsType-specific modal confirmation dialog.
     *
     * @param tile An optional {@code Tile} to expose.
     * @param template The {@code StringTemplate} explaining the choice.
     * @param goodsType A goods type to make an icon for the dialog from.
     * @param okKey A key for the "ok" button.
     * @param cancelKey A key for the "cancel" button.
     * @return True if the "ok" button was selected.
     */
    public final boolean confirm(Tile tile, StringTemplate template,
                                 GoodsType goodsType,
                                 String okKey, String cancelKey) {
        ImageIcon icon = new ImageIcon(getFixedImageLibrary()
            .getScaledGoodsTypeImage(goodsType));
        return confirm(tile, template, icon, okKey, cancelKey);
    }

    /**
     * Settlement-specific modal confirmation dialog.
     *
     * @param tile An optional {@code Tile} to expose.
     * @param template The {@code StringTemplate} explaining the choice.
     * @param settlement A settlement to make an icon for the dialog from.
     * @param okKey A key for the "ok" button.
     * @param cancelKey A key for the "cancel" button.
     * @return True if the "ok" button was selected.
     */
    public final boolean confirm(Tile tile, StringTemplate template,
                                 Settlement settlement,
                                 String okKey, String cancelKey) {
        ImageIcon icon = new ImageIcon(getFixedImageLibrary()
            .getScaledSettlementImage(settlement));
        return confirm(tile, template, icon, okKey, cancelKey);
    }

    /**
     * Unit-specific modal confirmation dialog.
     *
     * @param tile An optional {@code Tile} to expose.
     * @param template The {@code StringTemplate} explaining the choice.
     * @param unit A unit to make an icon for the dialog from.
     * @param okKey A key for the "ok" button.
     * @param cancelKey A key for the "cancel" button.
     * @return True if the "ok" button was selected.
     */
    public final boolean confirm(Tile tile, StringTemplate template, Unit unit,
                                 String okKey, String cancelKey) {
        ImageIcon icon = new ImageIcon(getFixedImageLibrary()
            .getScaledUnitImage(unit));
        return confirm(tile, template, icon, okKey, cancelKey);
    }

    /**
     * Confirm that a unit should abandon its educational activity.
     *
     * @param unit The {@code Unit} to check.
     * @param leaveColony True if the unit is about to leave the colony,
     *     not just the education building.
     * @return True if the unit can proceed.
     */
    public boolean confirmAbandonEducation(Unit unit, boolean leaveColony) {
        if (!unit.isInColony()) return true;
        boolean teacher = unit.getStudent() != null;
        // if leaving the colony, the student loses learning spot, so
        // check with player
        boolean student = leaveColony && unit.getTeacher() != null;
        if (!teacher && !student) return true;

        Building school = (Building)((teacher) ? unit.getLocation()
            : unit.getTeacher().getLocation());
        StringTemplate label = unit.getLabel(Unit.UnitLabelType.NATIONAL);
        StringTemplate template = (leaveColony) ? StringTemplate
            .template("abandonEducation.text")
            .addStringTemplate("%unit%", label)
            .addName("%colony%", school.getColony().getName())
            .addNamed("%building%", school)
            .addStringTemplate("%action%", (teacher)
                ? StringTemplate.key("abandonEducation.action.teaching")
                : StringTemplate.key("abandonEducation.action.studying"))
            : (teacher)
            ? StringTemplate.template("abandonTeaching.text")
                .addStringTemplate("%unit%", label)
                .addNamed("%building%", school)
            : null;
        return template == null
            || confirm(unit.getTile(), template, unit,
                       "abandonEducation.yes", "abandonEducation.no");
    }

    /**
     * If a unit has a trade route, get confirmation that it is
     * ok to clear it and set a destination.
     *
     * @param unit The {@code Unit} to check.
     * @return Whether it is acceptable to set a destination for this unit.
     */
    public boolean confirmClearTradeRoute(Unit unit) {
        TradeRoute tr = unit.getTradeRoute();
        if (tr == null) return true;
        StringTemplate template = StringTemplate
            .template("clearTradeRoute.text")
            .addStringTemplate("%unit%",
                unit.getLabel(Unit.UnitLabelType.NATIONAL))
            .addName("%route%", tr.getName());
        return confirm(unit.getTile(), template, unit, "yes", "no");
    }

    /**
     * Confirm whether the player wants to demand tribute from a colony.
     *
     * @param attacker The potential attacking {@code Unit}.
     * @param colony The target {@code Colony}.
     * @param ns A {@code NationSummary} of the other nation.
     * @return The amount of tribute to demand, positive if the demand
     *     should proceed.
     */
    public int confirmEuropeanTribute(Unit attacker, Colony colony,
                                      NationSummary ns) {
        Player player = attacker.getOwner();
        Player other = colony.getOwner();
        int strength = player.calculateStrength(false);
        int otherStrength = (ns == null) ? strength : ns.getMilitaryStrength();
        int mil = (otherStrength <= 1 || otherStrength * 5 < strength) ? 0
            : (strength == 0 || strength * 5 < otherStrength) ? 2
            : 1;

        StringTemplate t;
        int gold = (ns == null) ? 0 : ns.getGold();
        if (gold == 0) {
            t = StringTemplate.template("confirmTribute.broke")
                .addStringTemplate("%nation%", other.getNationLabel());
            showInformationPanel(t);
            return -1;
        }

        int fin = (gold <= 100) ? 0 : (gold <= 1000) ? 1 : 2;
        t = StringTemplate.template("confirmTribute.european")
            .addStringTemplate("%nation%", other.getNationLabel())
            .addStringTemplate("%danger%",
                StringTemplate.template("danger." + levels[mil]))
            .addStringTemplate("%finance%",
                StringTemplate.template("finance." + levels[fin]));
        return showSelectTributeAmountDialog(t, gold);
    }

    /**
     * Check if an attack results in a transition from peace or cease fire to
     * war and, if so, warn the player.
     *
     * @param attacker The potential attacking {@code Unit}.
     * @param target The target {@code Tile}.
     * @return True to attack, false to abort.
     */
    public boolean confirmHostileAction(Unit attacker, Tile target) {
        if (attacker.hasAbility(Ability.PIRACY)) {
            // Privateers can attack and remain at peace
            return true;
        }

        Player enemy;
        if (target.hasSettlement()) {
            enemy = target.getSettlement().getOwner();
        } else if (target == attacker.getTile()) {
            // Fortify on tile owned by another nation
            enemy = target.getOwner();
            if (enemy == null) return true;
        } else {
            Unit defender = target.getDefendingUnit(attacker);
            if (defender == null) {
                logger.warning("Attacking, but no defender - will try!");
                return true;
            }
            if (defender.hasAbility(Ability.PIRACY)) {
                // Privateers can be attacked and remain at peace
                return true;
            }
            enemy = defender.getOwner();
        }

        String messageId;
        switch (attacker.getOwner().getStance(enemy)) {
        case WAR:
            logger.finest("Player at war, no confirmation needed");
            return true;
        case CEASE_FIRE:
            messageId = "confirmHostile.ceaseFire";
            break;
        case ALLIANCE:
            messageId = "confirmHostile.alliance";
            break;
        case UNCONTACTED: case PEACE: default:
            messageId = "confirmHostile.peace";
            break;
        }
        StringTemplate t = StringTemplate.template(messageId)
            .addStringTemplate("%nation%", enemy.getNationLabel());
        return confirm(attacker.getTile(), t, attacker,
                       "confirmHostile.yes", "cancel");
    }

    /**
     * Confirm that a unit can leave its colony.
     * - Check for population limit.
     * - Query if education should be abandoned.
     *
     * @param unit The {@code Unit} that is leaving the colony.
     * @return True if the unit is allowed to leave.
     */
    public boolean confirmLeaveColony(Unit unit) {
        Colony colony = unit.getColony();
        StringTemplate message = colony.getReducePopulationMessage();
        if (message != null) {
            showInformationPanel(message);
            return false;
        }
        return confirmAbandonEducation(unit, true);
    }

    /**
     * Confirm whether the player wants to demand tribute from a native
     * settlement.
     *
     * @param attacker The potential attacking {@code Unit}.
     * @param is The target {@code IndianSettlement}.
     * @return The amount of tribute to demand, positive if the demand
     *     should proceed.
     */
    public int confirmNativeTribute(Unit attacker, IndianSettlement is) {
        Player player = attacker.getOwner();
        Player other = is.getOwner();
        int strength = player.calculateStrength(false);
        String messageId = (other.getSettlementCount() >= strength)
            ? "confirmTribute.unwise"
            : (other.getStance(player) == Stance.CEASE_FIRE)
            ? "confirmTribute.warLikely"
            : (is.getAlarm(player).getLevel() == Tension.Level.HAPPY)
            ? "confirmTribute.happy"
            : "confirmTribute.normal";
        StringTemplate t = StringTemplate.template(messageId)
            .addStringTemplate("%settlement%", is.getLocationLabelFor(player))
            .addStringTemplate("%nation%", other.getNationLabel());
        return (confirm(is.getTile(), t, attacker,
                        "confirmTribute.yes", "confirmTribute.no")) ? 1 : -1;
    }

    /**
     * Shows the pre-combat dialog if enabled, allowing the user to
     * view the odds and possibly cancel the attack.
     *
     * @param attacker The attacking {@code Unit}.
     * @param tile The target {@code Tile}.
     * @return True to attack, false to abort.
     */
    public boolean confirmPreCombat(Unit attacker, Tile tile) {
        if (getClientOptions().getBoolean(ClientOptions.SHOW_PRECOMBAT)) {
            Settlement settlement = tile.getSettlement();
            // Don't tell the player how a settlement is defended!
            FreeColGameObject defender = (settlement != null) ? settlement
                : tile.getDefendingUnit(attacker);
            return showPreCombatDialog(attacker, defender, tile);
        }
        return true;
    }

    /**
     * Confirm whether to stop the current game.
     *
     * @return True if confirmation was given.
     */
    public boolean confirmStopGame() {
        return confirm("stopCurrentGame.text",
                       "stopCurrentGame.yes", "stopCurrentGame.no");
    }

    /**
     * Get the choice of what a user wants to do with an armed unit at
     * a foreign settlement.
     *
     * @param settlement The {@code Settlement} to consider.
     * @return The chosen action, tribute, attack or cancel.
     */
    public ArmedUnitSettlementAction
        getArmedUnitSettlementChoice(Settlement settlement) {
        final Player player = getMyPlayer();

        List<ChoiceItem<ArmedUnitSettlementAction>> choices = new ArrayList<>();
        String msg = Messages.message("armedUnitSettlement.tribute");
        choices.add(new ChoiceItem<>(msg,
                ArmedUnitSettlementAction.SETTLEMENT_TRIBUTE));
        msg = Messages.message("armedUnitSettlement.attack");
        choices.add(new ChoiceItem<>(msg,
                ArmedUnitSettlementAction.SETTLEMENT_ATTACK));

        return getChoice(settlement.getTile(),
                         settlement.getAlarmLevelLabel(player),
                         settlement, "cancel", choices);
    }

    /**
     * Get the user choice of whether to pay arrears for boycotted
     * goods or to dump them instead.
     *
     * @param goods The {@code Goods} to possibly dump.
     * @param europe The player {@code Europe} where the boycott is in force.
     * @return The chosen {@code BoycottAction}.
     */
    public BoycottAction getBoycottChoice(Goods goods, Europe europe) {
        int arrears = europe.getOwner().getArrears(goods.getType());
        StringTemplate template = StringTemplate
            .template("boycottedGoods.text")
            .addNamed("%goods%", goods)
            .addNamed("%europe%", europe)
            .addAmount("%amount%", arrears);

        List<ChoiceItem<BoycottAction>> choices = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("payArrears"),
                                     BoycottAction.BOYCOTT_PAY_ARREARS));
        choices.add(new ChoiceItem<>(Messages.message("boycottedGoods.dumpGoods"),
                                     BoycottAction.BOYCOTT_DUMP_CARGO));

        return getChoice(null, template,
                         goods.getType(), "cancel", choices);
    }

    /**
     * Gets the user choice when negotiating a purchase from a settlement.
     *
     * @param unit The {@code Unit} that is buying.
     * @param settlement The {@code Settlement} to buy from.
     * @param goods The {@code Goods} to buy.
     * @param gold The current negotiated price.
     * @param canBuy True if buy is a valid option.
     * @return The chosen action, buy, haggle, or cancel.
     */
    public TradeBuyAction getBuyChoice(Unit unit, Settlement settlement,
                                       Goods goods, int gold, boolean canBuy) {
        // Get Buy price on Europe Market for comparison
        int euroPrice = unit.getOwner().getMarket()
            .getBidPrice(goods.getType(), goods.getAmount());
        StringTemplate nation = settlement.getOwner().getNationLabel();
        StringTemplate template = StringTemplate.template("buy.text")
            .addStringTemplate("%nation%", nation)
            .addStringTemplate("%goods%", goods.getLabel(true))
            .addAmount("%gold%", gold)
            .addAmount("%euprice%", euroPrice);

        List<ChoiceItem<TradeBuyAction>> choices = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("buy.takeOffer"),
                                     TradeBuyAction.BUY, canBuy));
        choices.add(new ChoiceItem<>(Messages.message("buy.moreGold"),
                                     TradeBuyAction.HAGGLE));

        return getChoice(unit.getTile(), template,
                         goods.getType(), "cancel", choices);
    }

    /**
     * General modal choice dialog.
     *
     * @param <T> The choice type.
     * @param explain A {@code StringTemplate} explaining the choice.
     * @param cancelKey A key for the "cancel" button.
     * @param choices A list a {@code ChoiceItem}s to choose from.
     * @return The selected value of the selected {@code ChoiceItem},
     *     or null if cancelled.
     */
    public final <T> T getChoice(StringTemplate explain, String cancelKey,
                                 List<ChoiceItem<T>> choices) {
        ImageIcon icon = new ImageIcon(getFixedImageLibrary()
            .getPlaceholderImage());
        return getChoice(null, explain, icon, cancelKey, choices);
    }

    /**
     * Goods-specific modal choice dialog.
     *
     * @param <T> The choice type.
     * @param tile An optional {@code Tile} to expose.
     * @param template A {@code StringTemplate} explaining the choice.
     * @param goodsType A {@code GoodsType} to display in dialog.
     * @param cancelKey A key for the "cancel" button.
     * @param choices A list a {@code ChoiceItem}s to choose from.
     * @return The selected value of the selected {@code ChoiceItem},
     *     or null if cancelled.
     */
    private final <T> T getChoice(Tile tile, StringTemplate template,
                                  GoodsType goodsType, String cancelKey,
                                  List<ChoiceItem<T>> choices) {
        ImageIcon icon = new ImageIcon(getFixedImageLibrary()
            .getScaledGoodsTypeImage(goodsType));
        return getChoice(tile, template, icon, cancelKey, choices);
    }

    /**
     * Nation-specific modal choice dialog.
     *
     * @param <T> The choice type.
     * @param tile An optional {@code Tile} to expose.
     * @param template A {@code StringTemplate} explaining the choice.
     * @param nation A {@code Nation} to display in dialog.
     * @param cancelKey A key for the "cancel" button.
     * @param choices A list a {@code ChoiceItem}s to choose from.
     * @return The selected value of the selected {@code ChoiceItem},
     *     or null if cancelled.
     */
    private final <T> T getChoice(Tile tile, StringTemplate template,
                                  Nation nation, String cancelKey,
                                  List<ChoiceItem<T>> choices) {
        ImageIcon icon = new ImageIcon(getFixedImageLibrary()
            .getScaledNationImage(nation));
        return getChoice(tile, template, icon, cancelKey, choices);
    }

    /**
     * Settlement-specific modal choice dialog.
     *
     * @param <T> The choice type.
     * @param tile An optional {@code Tile} to expose.
     * @param template A {@code StringTemplate} explaining the choice.
     * @param settlement A {@code Settlement} to display in dialog.
     * @param cancelKey A key for the "cancel" button.
     * @param choices A list a {@code ChoiceItem}s to choose from.
     * @return The selected value of the selected {@code ChoiceItem},
     *     or null if cancelled.
     */
    public final <T> T getChoice(Tile tile, StringTemplate template,
                                 Settlement settlement, String cancelKey,
                                 List<ChoiceItem<T>> choices) {
        ImageIcon icon = new ImageIcon(getFixedImageLibrary()
            .getScaledSettlementImage(settlement));
        return getChoice(tile, template, icon, cancelKey, choices);
    }

    /**
     * Unit-specific modal choice dialog.
     *
     * @param <T> The choice type.
     * @param tile An optional {@code Tile} to expose.
     * @param template A {@code StringTemplate} explaining the choice.
     * @param unit A {@code Unit} to display in dialog.
     * @param cancelKey A key for the "cancel" button.
     * @param choices A list a {@code ChoiceItem}s to choose from.
     * @return The selected value of the selected {@code ChoiceItem},
     *     or null if cancelled.
     */
    public final <T> T getChoice(Tile tile, StringTemplate template,
                                 Unit unit, String cancelKey,
                                 List<ChoiceItem<T>> choices) {
        ImageIcon icon = new ImageIcon(getFixedImageLibrary()
            .getScaledUnitImage(unit));
        return getChoice(tile, template, icon, cancelKey, choices);
    }

    /**
     * Gets the user choice for claiming a tile.
     *
     * @param tile The {@code Tile} to claim.
     * @param player The {@code Player} that is claiming.
     * @param price An asking price, if any.
     * @param owner The {@code Player} that owns the land.
     * @return The chosen action, accept, steal or cancel.
     */
    public ClaimAction getClaimChoice(Tile tile, Player player, int price,
                                      Player owner) {
        List<ChoiceItem<ClaimAction>> choices = new ArrayList<>();
        StringTemplate template;
        if (owner.hasContacted(player)) {
            template = StringTemplate.template("indianLand.text")
                .addStringTemplate("%player%", owner.getNationLabel());
            StringTemplate pay = StringTemplate.template("indianLand.pay")
                .addAmount("%amount%", price);
            choices.add(new ChoiceItem<>(Messages.message(pay),
                                         ClaimAction.CLAIM_ACCEPT,
                                         player.checkGold(price)));
        } else {
            template = StringTemplate.template("indianLand.unknown");
        }
        choices.add(new ChoiceItem<>(Messages.message("indianLand.take"),
                                     ClaimAction.CLAIM_STEAL));

        return getChoice(tile, template, owner.getNation(),
                         "indianLand.cancel", choices);
    }

    /**
     * Get the user choice when trading with a native settlement.
     *
     * @param settlement The native settlement to trade with.
     * @param template A {@code StringTemplate} containing the message
     *     to display.
     * @param canBuy Show a "buy" option.
     * @param canSell Show a "sell" option.
     * @param canGift Show a "gift" option.
     * @return The chosen action, buy, sell, gift or cancel.
     */
    public TradeAction getIndianSettlementTradeChoice(Settlement settlement,
                                                      StringTemplate template,
                                                      boolean canBuy,
                                                      boolean canSell,
                                                      boolean canGift) {
        String msg;
        ArrayList<ChoiceItem<TradeAction>> choices = new ArrayList<>();
        if (canBuy) {
            msg = Messages.message("tradeProposition.toBuy");
            choices.add(new ChoiceItem<>(msg, TradeAction.BUY, canBuy));
        }
        if (canSell) {
            msg = Messages.message("tradeProposition.toSell");
            choices.add(new ChoiceItem<>(msg, TradeAction.SELL, canSell));
        }
        if (canGift) {
            msg = Messages.message("tradeProposition.toGift");
            choices.add(new ChoiceItem<>(msg, TradeAction.GIFT, canGift));
        }
        if (choices.isEmpty()) return null;

        return getChoice(settlement.getTile(), template,
                         settlement, "cancel", choices);
    }

    /**
     * Get the user choice of what to do with a missionary at a native
     * settlement.
     *
     * @param unit The {@code Unit} speaking to the settlement.
     * @param is The {@code IndianSettlement} being visited.
     * @param canEstablish Is establish a valid option.
     * @param canDenounce Is denounce a valid option.
     * @return The chosen action, establish mission, denounce, incite
     *     or cancel.
     */
    public MissionaryAction getMissionaryChoice(Unit unit,
                                                IndianSettlement is,
                                                boolean canEstablish,
                                                boolean canDenounce) {
        StringTemplate template;
        if (is.hasContacted(unit.getOwner())) {
            StringTemplate q = StringTemplate
                .template("missionarySettlement.question")
                .addStringTemplate("%settlement%",
                    is.getLocationLabelFor(unit.getOwner()));
            template = StringTemplate.label("\n\n")
                .addStringTemplate(is.getAlarmLevelLabel(unit.getOwner()))
                .addStringTemplate(q);
        } else {
            template = StringTemplate
                .template("missionarySettlement.questionUncontacted");
        }
        List<ChoiceItem<MissionaryAction>> choices = new ArrayList<>();
        String msg;
        if (canEstablish) {
            msg = Messages.message("missionarySettlement.establish");
            choices.add(new ChoiceItem<>(msg,
                    MissionaryAction.MISSIONARY_ESTABLISH_MISSION,
                    canEstablish));
        }
        if (canDenounce) {
            msg = Messages.message("missionarySettlement.heresy");
            choices.add(new ChoiceItem<>(msg,
                    MissionaryAction.MISSIONARY_DENOUNCE_HERESY,
                    canDenounce));
        }
        msg = Messages.message("missionarySettlement.incite");
        choices.add(new ChoiceItem<>(msg,
                MissionaryAction.MISSIONARY_INCITE_INDIANS));

        return getChoice(unit.getTile(), template,
                         is, "cancel", choices);
    }

    /**
     * Get a name for a new colony for a player.
     *
     * @param player The {@code Player} to get the colony name for.
     * @param tile The {@code Tile} for the new colony.
     * @return A colony name, or null if the user has reconsidered.
     */
    public String getNewColonyName(Player player, Tile tile) {
        String suggested = player.getSettlementName(null);
        StringTemplate t = StringTemplate.template("nameColony.text");
        String name = getInput(tile, t, suggested, "accept", "cancel");
        if (name == null) {
            // Cancelled
        } else if (name.isEmpty()) {
            showInformationPanel("enterSomeText"); // 0-length is invalid
        } else if (player.getSettlementByName(name) != null) {
            // Must be unique
            showInformationPanel(tile,
                StringTemplate.template("nameColony.notUnique")
                    .addName("%name%", name));
        } else {
            return name;
        }
        player.putSettlementName(suggested);
        return null;
    }

    /**
     * Get the user choice for what to do with a scout at a foreign colony.
     *
     * @param colony The {@code Colony} to be scouted.
     * @param unit The {@code Unit} that is scouting.
     * @param neg True if negotation is a valid choice.
     * @return The selected action, either negotiate, spy, attack or cancel.
     */
    public ScoutColonyAction getScoutForeignColonyChoice(Colony colony,
                                                         Unit unit,
                                                         boolean neg) {
        StringTemplate u = unit.getLabel(Unit.UnitLabelType.NATIONAL);
        StringTemplate template = StringTemplate.template("scoutColony.text")
            .addStringTemplate("%unit%", u)
            .addName("%colony%", colony.getName());

        List<ChoiceItem<ScoutColonyAction>> choices = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("scoutColony.negotiate"),
                                     ScoutColonyAction.SCOUT_COLONY_NEGOTIATE,
                                     neg));
        choices.add(new ChoiceItem<>(Messages.message("scoutColony.spy"),
                                     ScoutColonyAction.SCOUT_COLONY_SPY));
        choices.add(new ChoiceItem<>(Messages.message("scoutColony.attack"),
                                     ScoutColonyAction.SCOUT_COLONY_ATTACK));

        return getChoice(unit.getTile(), template,
                         colony, "cancel", choices);
    }

    /**
     * Get the user choice for what to do at a native settlement.
     *
     * @param is The {@code IndianSettlement} to be scouted.
     * @param numberString The number of settlements in the settlement
     *     owner nation.
     * @return The chosen action, speak, tribute, attack or cancel.
     */
    public ScoutIndianSettlementAction
        getScoutIndianSettlementChoice(IndianSettlement is,
                                       String numberString) {
        final Player player = getMyPlayer();
        final Player owner = is.getOwner();

        StringTemplate template;
        if (is.hasContacted(player)) {
            StringTemplate skillPart = (is.getLearnableSkill() != null)
                ? StringTemplate.template("scoutSettlement.skill")
                                .addNamed("%skill%", is.getLearnableSkill())
                : StringTemplate.name(" ");
            StringTemplate goodsPart;
            int present = is.getWantedGoodsCount();
            if (present > 0) {
                goodsPart = StringTemplate.template("scoutSettlement.trade."
                    + Integer.toString(present));
                for (int i = 0; i < present; i++) {
                    String tradeKey = "%goods" + Integer.toString(i+1) + "%";
                    goodsPart.addNamed(tradeKey, is.getWantedGoods(i));
                }
            } else {
                goodsPart = StringTemplate.name(" ");
            }
            IndianNationType nt = (IndianNationType)owner.getNationType();
            StringTemplate l = is.getLocationLabelFor(player);
            template = StringTemplate.template("scoutSettlement.greetings")
                .addStringTemplate("%alarmPart%", is.getAlarmLevelLabel(player))
                .addStringTemplate("%nation%", owner.getNationLabel())
                .addStringTemplate("%settlement%", l)
                .addName("%number%", numberString)
                .add("%settlementType%", nt.getSettlementTypeKey(true))
                .addStringTemplate("%skillPart%", skillPart)
                .addStringTemplate("%goodsPart%", goodsPart);
        } else {
            template = StringTemplate
                .template("scoutSettlement.greetUncontacted")
                .addStringTemplate("%nation%", owner.getNationLabel());
        }
        List<ChoiceItem<ScoutIndianSettlementAction>> choices
            = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("scoutSettlement.speak"),
                ScoutIndianSettlementAction.SCOUT_SETTLEMENT_SPEAK));
        choices.add(new ChoiceItem<>(Messages.message("scoutSettlement.tribute"),
                ScoutIndianSettlementAction.SCOUT_SETTLEMENT_TRIBUTE));
        choices.add(new ChoiceItem<>(Messages.message("scoutSettlement.attack"),
                ScoutIndianSettlementAction.SCOUT_SETTLEMENT_ATTACK));

        return getChoice(is.getTile(), template, is, "cancel", choices);
    }

    /**
     * Get the user choice for negotiating a sale to a settlement.
     *
     * @param unit The {@code Unit} that is selling.
     * @param settlement The {@code Settlement} to sell to.
     * @param goods The {@code Goods} to sell.
     * @param gold The current negotiated price.
     * @return The chosen action, sell, gift or haggle, or null.
     */
    public TradeSellAction getSellChoice(Unit unit, Settlement settlement,
                                         Goods goods, int gold) {
        //Get Sale price on Europe Market for comparison
        int euroPrice = unit.getOwner().getMarket()
            .getSalePrice(goods.getType(), goods.getAmount());
        StringTemplate goodsTemplate = goods.getLabel(true);
        StringTemplate nation = settlement.getOwner().getNationLabel();
        StringTemplate template = StringTemplate.template("sell.text")
            .addStringTemplate("%nation%", nation)
            .addStringTemplate("%goods%", goodsTemplate)
            .addAmount("%gold%", gold)
            .addAmount("%euprice%", euroPrice);

        List<ChoiceItem<TradeSellAction>> choices = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("sell.takeOffer"),
                                     TradeSellAction.SELL));
        choices.add(new ChoiceItem<>(Messages.message("sell.moreGold"),
                                     TradeSellAction.HAGGLE));
        choices.add(new ChoiceItem<>(Messages.message(StringTemplate
                    .template("sell.gift")
                    .addStringTemplate("%goods%", goodsTemplate)),
                TradeSellAction.GIFT));

        return getChoice(unit.getTile(), template,
                         goods.getType(), "cancel", choices);
    }

    /**
     * Shows the current difficulty as an uneditable dialog.
     */
    public final void showDifficultyDialog() {
        final Specification spec = getSpecification();
        showDifficultyDialog(spec, spec.getDifficultyOptionGroup(), false, null);
    }

    /**
     * Show an i18n compliant error message derived from a template.
     *
     * @param template The {@code StringTemplate} containing the message.
     * @return The panel shown.
     */
    public final FreeColPanel showErrorPanel(StringTemplate template) {
        return showErrorPanel(template, null);
    }

    /**
     * Show an i18n compliant error message derived from a template,
     * with optional extra debug information.
     *
     * @param template The {@code StringTemplate} containing the message.
     * @param message Optional extra debug information.
     * @return The panel shown.
     */
    public final FreeColPanel showErrorPanel(StringTemplate template,
                                             String message) {
        return showErrorPanel(template, message, null);
    }
    
    /**
     * Show an i18n compliant error message derived from a template,
     * with optional extra debug information and an optional callback.
     *
     * @param template The {@code StringTemplate} containing the message.
     * @param message Optional extra debug information.
     * @param callback Optional routine to run when the error panel is closed.
     * @return The panel shown.
     */
    public final FreeColPanel showErrorPanel(StringTemplate template,
                                             String message,
                                             Runnable callback) {
        String display = Messages.message(template);
        if (message != null && FreeColDebugger.isInDebugMode()) {
            display += "/" + message + "/";
        }
        return showErrorPanel(display, callback);
    }

    /**
     * Show a serious error message with an exception and return
     * to the main panel when done.  This time, do not return the panel
     * as we have already defined a callback.
     *
     * @param ex An optional {@code Exception} to display.
     * @param template A {@code StringTemplate} for the message.
     */
    public final void showErrorPanel(Exception ex, StringTemplate template) {
        final StringTemplate t = (ex == null) ? template
            : FreeCol.errorFromException(ex, template);
        invokeNowOrLater(() -> showErrorPanel(t, null, () -> {
                    closeMenus();
                    showMainPanel(null);
                }));
    }
                
    /**
     * Show an information message.
     *
     * @param messageId The message key.
     * @return The panel shown.
     */
    public final FreeColPanel showInformationPanel(String messageId) {
        return showInformationPanel(StringTemplate.key(messageId));
    }

    /**
     * Show an information message.
     *
     * @param template The message template.
     * @return The panel shown.
     */
    public final FreeColPanel showInformationPanel(StringTemplate template) {
        return showInformationPanel(null, template);
    }

    /**
     * Show an information message.
     *
     * @param displayObject An optional object to display as an icon.
     * @param messageId The message key.
     * @return The panel shown.
     */
    public final FreeColPanel showInformationPanel(FreeColObject displayObject,
                                                   String messageId) {
        return showInformationPanel(displayObject,
                                    StringTemplate.key(messageId));
    }

    /**
     * Show a save file dialog, selecting one to load.
     *
     * @param root The root directory to look in.
     * @param extension The file extensions to look for.
     * @return The {@code File} selected, or null on error.
     */
    public final File showLoadSaveFileDialog(File root, String... extension) {
        File file = showLoadDialog(root, extension);
        if (file != null && !file.isFile()) {
            showErrorPanel(FreeCol.badFile("error.noSuchFile", file));
            file = null;
        }
        return file;
    }

    /**
     * Show the NewPanel.
     */
    public final void showNewPanel() {
        showNewPanel(null);
    }


    // Sound routines, delegated to the SoundController, only useful
    // for GUI classes in need of sound

    /**
     * Play a sound.
     *
     * @param sound The sound resource to play, or if null stop playing.
     */
    public void playSound(String sound) {
        if (sound != null && ResourceManager.getString(sound + ".type", "").equals("music")) {
            getFreeColClient().getSoundController().playMusic(sound);
        } else {
            getFreeColClient().getSoundController().playSound(sound);
        }
    }

    /**
     * Get the label text for the sound player mixer.
     *
     * Used by: AudioMixerOptionUI
     *
     * @return The text.
     */
    public String getSoundMixerLabelText() {
        return getFreeColClient().getSoundController().getSoundMixerLabelText();
    }


    // Below here, routines should be just stubs that need full
    // implementations.

    
    // Simple accessors

    /**
     * Get the fixed image library for use on panels.
     *
     * Used by: ColonyPanel, ConstructionPanel, InfoPanel, certain UnitLabels.
     *
     * @return Null here, real implementations will override.
     */
    public ImageLibrary getFixedImageLibrary() { return null; }

    /**
     * Get the scaled image library for use on the map.
     *
     * @return Null here, real implementations will override.
     */
    public ImageLibrary getScaledImageLibrary() { return null; }

    /**
     * Is this GUI in windowed mode?
     *
     * Used by: DragListener for a nasty workaround that should go away
     *
     * @return True by default, real implementations will override.
     */
    public boolean isWindowed() { return true; }


    // Invocation

    /**
     * Run in the EDT, either immediately if in it or later when it wakes up.
     *
     * @param runnable A {@code Runnable} to run.
     */
    public void invokeNowOrLater(Runnable runnable) {}
    
    /**
     * Run in the EDT, either immediately or wait for it.
     *
     * @param runnable A {@code Runnable} to run.
     */
    public void invokeNowOrWait(Runnable runnable) {}


    // Initialization and teardown

    /**
     * Change the windowed mode (really a toggle).
     *
     * Used by: ChangeWindowedModeAction
     */
    public void changeWindowedMode() {}

    /** 
     * Swing system and look-and-feel initialization.
     * 
     * Used by: FreeColClient
     * 
     * @param fontName An optional font name to be used.
     * @exception FreeColException if the LAF is incompatible with the GUI.
     */
    public void installLookAndFeel(String fontName) throws FreeColException {}

    /**
     * Quit the GUI.  All that is required is to exit the full screen.
     * 
     * Used by: FreeColClient.quit
     */
    public void quitGUI() {}

    /**
     * Reset the GUI on reconnect.
     *
     * Used by: FreeColClient.restoreGUI
     * 
     * @param active An optional active {@code Unit}.
     * @param tile An optional {@code Tile} to focus on if there is no
     *     active unit.
     */
    public void reconnectGUI(Unit active, Tile tile) {}

    /**
     * Remove all in-game components (i.e. all the Listeners).
     *
     * Used by: ContinueAction, ConnectController.{mainTitle,newGame}
     *     InGameController.loadGame, MapEditorController.newMap, StartMapAction
     */    
    public void removeInGameComponents() {}

    /**
     * Shows the {@code VideoPanel}.
     *
     * Used by: FreeColClient
     * 
     * @param userMsg An optional user message.
     * @param callback A {@code Runnable} to run when the video completes.
     */
    public void showOpeningVideo(final String userMsg, Runnable callback) {}

    /**
     * Starts the GUI by creating and displaying the GUI-objects.
     *
     * Used by: FreeColClient
     * 
     * @param desiredWindowSize The desired size of the GUI window.
     */
    public void startGUI(final Dimension desiredWindowSize) {
        logger.info("It seems that the GraphicsEnvironment is headless!");
    }
    
    public Dimension getMapViewDimension() {
        return null;
    }

    /**
     * Start the GUI for the map editor.
     *
     * Used by: NewPanel
     */
    public void startMapEditorGUI() {}


    // Animation handling

    /**
     * Animate a unit attack.
     *
     * Used by: client InGameController
     *
     * @param attacker The attacking {@code Unit}.
     * @param defender The defending {@code Unit}.
     * @param attackerTile The {@code Tile} to show the attacker on.
     * @param defenderTile The {@code Tile} to show the defender on.
     * @param success Did the attack succeed?
     */
    public void animateUnitAttack(Unit attacker, Unit defender,
                                  Tile attackerTile, Tile defenderTile,
                                  boolean success) {}

    /**
     * Animate a unit move.
     *
     * Used by: client InGameController
     *
     * @param unit The {@code Unit} that is moving.
     * @param srcTile The {@code Tile} the unit starts at.
     * @param dstTile The {@code Tile} the unit moves to.
     */
    public void animateUnitMove(Unit unit, Tile srcTile, Tile dstTile) {}


    // Dialog primitives

    /**
     * General modal confirmation dialog.
     *
     * @param tile An optional {@code Tile} to expose.
     * @param template The {@code StringTemplate} explaining the choice.
     * @param icon An {@code ImageIcon} to display in dialog.
     * @param okKey A key for the message on the "ok" button.
     * @param cancelKey A key for the message on the "cancel" button.
     * @return True if the "ok" button was selected.
     */
    public boolean confirm(Tile tile, StringTemplate template,
                           ImageIcon icon,
                           String okKey, String cancelKey) {
        return false;
    }

    /**
     * General modal choice dialog.
     *
     * @param <T> The choice type.
     * @param tile An optional {@code Tile} to expose.
     * @param template A {@code StringTemplate} explaining the choice.
     * @param icon An optional {@code ImageIcon} to display in dialog.
     * @param cancelKey A key for the message on the "cancel" button.
     * @param choices A list a {@code ChoiceItem}s to choose from.
     * @return The selected value of the selected {@code ChoiceItem},
     *     or null if cancelled.
     */
    protected <T> T getChoice(Tile tile, StringTemplate template,
                              ImageIcon icon, String cancelKey,
                              List<ChoiceItem<T>> choices) {
        return null;
    }

    /**
     * General modal string input dialog.
     *
     * @param tile An optional {@code Tile} to expose.
     * @param template A {@code StringTemplate} explaining the choice.
     * @param defaultValue The default value to show initially.
     * @param okKey A key for the message on the "ok" button.
     * @param cancelKey A key for the message on the "cancel" button.
     * @return The chosen value.
     */
    public String getInput(Tile tile, StringTemplate template,
                           String defaultValue,
                           String okKey, String cancelKey) {
        return null;
    }


    // Focus control

    /**
     * Get the current focus tile.
     *
     * Used by: MiniMap.paintMap
     *
     * @return The focus {@code Tile}.
     */
    public Tile getFocus() {
        return null;
    }
    
    /**
     * Gets the current focus of the visible map given in pixels.
     * 
     * @return The current focus map point that is between {@code 0}
     *   and <code>map.getWidth() * tileBounds.getWidth()</code>.
     */
    public Point getFocusMapPoint() {
        return null;
    }

    /**
     * Set the current focus tile.
     *
     * Used by: CanvasMapEditorMouseListener, CenterAction,
     *   FindSettlementPanel, InfoPanel, MiniMap.focus, MapEditorController,
     *   SelectDestinationDialog.recenter
     *
     * @param tile The new focus {@code Tile}.
     */
    public void setFocus(Tile tile) {}
    
    /**
     * The current focus of the visible map given in pixels.
     * 
     * The maximum width of the map in pixels is:
     * <code>map.getWidth() * tileBounds.getWidth()</code>.
     * 
     * @param pointToFocus The new focus point.
     */
    public void setFocusMapPoint(Point pointToFocus) {}

    // Path handling

    /**
     * Set the path for the active unit.
     *
     * Used by: TilePopup
     *
     * @param path The new unit path.
     */
    public void setUnitPath(PathNode path) {}

    /**
     * Start/stop the goto path display.
     *
     * Used by: GotoTileAction
     */
    public void activateGotoPath() {}

    /**
     * Stop the goto path display.
     *
     * Used by: client InGameController.askClearGotoOrders
     */
    public void clearGotoPath() {}

    /**
     * Check if the user has  GoTo mode enabled.
     *
     * Used by: CanvasMouseListener
     *
     * @return True if the user has toggled GoTo mode.
     */
    public boolean isGotoStarted() {
        return false;
    }

    /**
     * Perform an immediate goto to a tile with the active unit.
     *
     * Used by: TilePopup
     *
     * @param tile The {@code Tile} to go to.
     */
    public void performGoto(Tile tile) {}

    /**
     * Perform an immediate goto to a point on the map.
     *
     * Used by: CanvasMouseListener
     *
     * @param x The x coordinate of the goto destination (pixels).
     * @param y The x coordinate of the goto destination (pixels).
     */
    public void performGoto(int x, int y) {}
    
    /**
     * Send the active unit along the current goto path as far as possible.
     *
     * Used by: CanvasMouseListener
     */
    public void traverseGotoPath() {}

    /**
     * Update the goto path to a new position on the map.
     *
     * Used by: CanvasMouseMotionListener
     *
     * @param x The x coordinate for the new goto path destination (pixels).
     * @param y The y coordinate for the new goto path destination (pixels).
     * @param start If true start a new goto if one is not underway.
     */
    public void updateGoto(int x, int y, boolean start) {}

    /**
     * Prepare a drag from the given coordinates.  This may turn into
     * a goto if further drag motion is detected.
     *
     * Used by: CanvasMouseListener
     *
     * @param x Drag x coordinate (pixels).
     * @param y Drag x coordinate (pixels).
     */
    public void prepareDrag(int x, int y) {}

    
    // MapControls handling

    /**
     * Is the map able to zoom in further?
     *
     * Used by: MiniMapZoomInAction
     *
     * @return True if the map can zoom in.
     */
    public boolean canZoomInMapControls() {
        return false;
    }

    /**
     * Is the map able to zoom out further?
     *
     * Used by: MiniMapZoomOutAction
     *
     * @return True if the map can zoom out.
     */
    public boolean canZoomOutMapControls() {
        return false;
    }

    /**
     * Enable the map controls.
     *
     * Used by: MapControlsAction.
     *
     * @param enable If true then enable.
     */
    public void enableMapControls(boolean enable) {}

    /**
     * Toggle the fog of war control.
     *
     * Used by: MiniMapToggleFogOfWarAction
     */
    public void miniMapToggleFogOfWarControls() {}

    /**
     * Toggle the view control.
     *
     * Used by: MiniMapToggleFogOfWarAction
     */
    public void miniMapToggleViewControls() {}

    /**
     * Update the map controls, including the InfoPanel according to
     * the view mode.
     *
     * Used by: client InGameController.updateGUI, MapEditorController
     */
    public void updateMapControls() {}

    /**
     * Zoom in the map controls.
     *
     * Used by: MiniMapZoomInAction
     */
    public void zoomInMapControls() {}

    /**
     * Zoom out the map controls.
     *
     * Used by: MiniMapZoomOutAction
     */
    public void zoomOutMapControls() {}


    // Menu handling

    /**
     * Close any open menus.
     *
     * Used by: FreeColClient.skipTurns,
     *   client InGameController.{endTurn,setCurrentPlayer}
     *   MapEditorController, PreGameController
     */
    public void closeMenus() {}

    /**
     * Resets the map controls in order to properly reference any
     * newly recreated action.
     */
    public void resetMapControls() {}
    
    /**
     * Resets the menu bar in order to properly reference any
     * newly recreated action.
     */
    public void resetMenuBar() {}
    
    /**
     * Update the menu bar.
     *
     * Used by: InGameController.updateGUI, MapEditorController,
     *   NewEmptyMapAction
     */
    public void updateMenuBar() {}

    /**
     * Display a popup menu.
     *
     * Used by: ColonyPanel, DragListener
     *
     * @param menu The {@code JPopupMenu} to display.
     * @param x The menu x coordinate.
     * @param y The menu y coordinate.
     */
    public void showPopupMenu(JPopupMenu menu, int x, int y) {}


    // Scrolling

    /**
     * Work out what direction to scroll the map if a coordinate is close
     * to an edge.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param scrollSpace The clearance from the relevant edge
     * @param ignoreTop If the top should be ignored
     * @return The {@code Direction} to scroll, or null if not.
     */
    public Direction getScrollDirection(int x, int y, int scrollSpace,
                                        boolean ignoreTop) { return null; }

    /**
     * Scroll the map in a given direction.
     *
     * @param direction The {@code Direction} to scroll.
     * @param performRepaints If {@code true}, then repaints are performed
     *      after scrolling.
     * @return True if scrolling can continue.
     */
    public boolean scrollMap(Direction direction, boolean performRepaints) { return false; }
    
    /**
     * Sets the scroll speed back to the initial value. This is needed
     * when the scrolling stops, since the scrolling accelerates.
     */
    public void resetScrollSpeed() { }
    
    /**
     * Paint the whole canvas now.
     * 
     * This should only be called for very special cases, like animations.
     * Normally, use {@link #repaint()} instead.
     */
    public void paintImmediately() {}
    
    /**
     * Enables asynchronous painting. That is, the painting of the
     * {@link MapViewer} is performed in a thread other than the EDT
     * (normal GUI thread).
     * 
     * This allows better performance and lower latency, but will sometimes
     * produce visual artifacts when the game state changes during painting.
     *    
     * @see #stopMapAsyncPainter()
     */
    public MapAsyncPainter useMapAsyncPainter() {
        return null;
    }
    
    /**
     * Stops asynchronous painting.
     * 
     * @see #useMapAsyncPainter()
     */
    public void stopMapAsyncPainter() {}

    // Tile image manipulation

    // Used by: InfoPanel
    public BufferedImage createTileImageWithBeachBorderAndItems(Tile tile) { return null; }

    // Used by: TilePanel
    public BufferedImage createTileImage(Tile tile, Player player) { return null; }

    // Used by: WorkProductionPanel
    public BufferedImage createColonyTileImage(Tile tile, Colony colony) { return null; }

    /**
     * Display the ColonyTiles of a Colony.
     *
     * Used by: ColonyPanel.TilesPanel
     *
     * @param g2d A {@code Graphics2D} to draw to.
     * @param tiles The {@code Tile}s to display.
     * @param colony The enclosing {@code Colony}.
     */
    public void displayColonyTiles(Graphics2D g2d, Tile[][] tiles,
                                   Colony colony) {}


    // View mode handling, including accessors for the active unit for
    // MOVE_UNITS mode, and the selected tile for VIEW_TERRAIN mode.
    // In MAP_TRANSFORM mode the map transform lives in the
    // MapEditorController.

    /**
     * Get the current view mode.
     *
     * Used by: MoveAction, ToggleViewModeAction
     *
     * @return One of the view mode constants, or negative on error.
     */
    public ViewMode getViewMode() {
        return ViewMode.END_TURN;
    }

    /**
     * Get the active unit.
     *
     * Used by: many
     *
     * @return The current active {@code Unit}.
     */
    public Unit getActiveUnit() {
        return null;
    }

    /**
     * Get the selected tile.
     *
     * Used by: MoveAction, TilePopupAction, ToggleViewModeAction
     *
     * @return The selected {@code Tile}.
     */
    public Tile getSelectedTile() {
        return null;
    }

    /**
     * Change to terrain mode and select a tile.
     *
     * Used by: CanvasMapEditorMouseListener,
     *   client InGameController.{updateActiveUnit,moveTileCursor}
     *
     * @param tile The {@code Tile} to select.
     */
    public void changeView(Tile tile) {}

    /**
     * Change to move units mode, and select a unit.
     *
     * Used by: ChangeAction, DebugUtils, EndTurnDialog,
     *   client InGameController (several)
     *   MapEditorController, TilePopup, QuickActionMenu, UnitLabel
     *
     * @param unit The {@code Unit} to select.
     * @param force Set true if the unit is the same, but *has*
     *     changed in some way (e.g. moves left).
     */
    public void changeView(Unit unit, boolean force) {}

    /**
     * Change to map transform mode, and select a transform.
     *
     * Used by: MapEditorController
     *
     * @param transform The {@code MapTransform} to select.
     */
    public void changeView(MapTransform transform) {}
    
    /**
     * Change to end turn mode.
     *
     * Used by: client InGameController.updateActiveUnit
     */
    public void changeView() {}

    /**
     * Sets if ranged attack mode should be activated.
     * 
     * @param rangedAttackMode If {@code true}, then display tiles reachable
     *      by ranged attack and allow the attack to be performed by clicking. 
     */
    public void setRangedAttackMode(boolean rangedAttackMode) {}
    
    /**
     * Toggles if ranged attack mode should be activated.
     * 
     * @see #setRangedAttackMode(boolean)
     */
    public void toggleRangedAttackMode() {}
    

    // Zoom controls

    /**
     * Can the map be zoomed in?
     *
     * Used by: ZoomInAction
     *
     * @return True if the map can zoom in.
     */
    public boolean canZoomInMap() {
        return false;
    }

    /**
     * Can the map be zoomed out?
     *
     * Used by: ZoomOutAction
     *
     * @return True if the map can zoom out.
     */
    public boolean canZoomOutMap() {
        return false;
    }

    /**
     * Zoom the map in.
     *
     * Used by: ZoomInAction
     */
    public void zoomInMap() {}

    /**
     * Zoom the map out.
     *
     * Used by: ZoomOutAction
     */
    public void zoomOutMap() {}


    // Miscellaneous gui manipulation

    /**
     * Handle a click on the canvas.
     *
     * Used by: CanvasMouseListener
     *
     * @param count The click count.
     * @param x The x coordinate of the click.
     * @param y The y coordinate of the click.
     */
    public void clickAt(int count, int x, int y) {}

    /**
     * Close a panel.
     *
     * Used by: client InGameController.closehandler
     *
     * @param panel The identifier for the panel to close.
     */
    public void closePanel(String panel) {}

    /**
     * Close the main panel if present.
     *
     * Used by: MapEditorController, PreGameController
     */
    public void closeMainPanel() {}

    /**
     * Close the status panel if present.
     *
     * Used by: FreeColClient, MapEditorController, client InGameController, 
     *   PreGameController
     */
    public void closeStatusPanel() {}

    /**
     * Update with a new chat message.
     *
     * Used by: client InGameController.{chat,chatHandler}
     *
     * @param sender The message sender.
     * @param message The chat message.
     * @param color The message color.
     * @param privateChat True if the message is private.
     */
    public void displayChat(String sender, String message, Color color,
                            boolean privateChat) {}

    /**
     * Show the appropriate panel for an object.
     *
     * @param fco The {@code FreeColObject} to display.
     */
    public void displayObject(FreeColObject fco) {}

    /**
     * A chat message was received during the pre-game setup.
     *
     * Used by: PreGameController.chatHandler
     *
     * @param sender The player who sent the chat message.
     * @param message The chat message.
     * @param privateChat True if the message is private.
     */
    public void displayStartChat(String sender, String message,
                                 boolean privateChat) {}


    /**
     * Checks if a client options dialog is present.
     *
     * Used by: FreeColAction.shouldBeEnabled
     *
     * @return True if the client options are showing.
     */
    public boolean isClientOptionsDialogShowing() {
        return false;
    }

    /**
     * Is another panel being displayed.
     *
     * Used by: many Actions
     *
     * @return True if there is another panel present.
     */
    public boolean isPanelShowing() {
        return false;
    }

    /**
     * Refresh the whole GUI.
     * 
     * This is done by invalidating any cached rendering and then repainting the
     * entire screen. Please use only when the entire map should be fully updated.
     * 
     * Please use {@link MapViewerRepaintManager#markAsDirty(Tile)}
     * and {@link #repaint()} instead, if only parts of the map need to be updated.
     */
    public void refresh() {}
    
    /**
     * Repaints the entire screen, but uses image caches to avoid unnecessary
     * painting of the map.
     * 
     * Please use {@link MapViewerRepaintManager#markAsDirty(Tile)} in order to
     * invalidate the caches for a given tile.
     * 
     * @see #refresh()
     */
    public void repaint() {}
    
    public void refreshTile(Tile tile) {}

    /**
     * Refresh the players table in the StartGamePanel.
     *
     * Used by: SetNationMessage.clientHandler
     */
    public void refreshPlayersTable() {}

    /**
     * Remove a component from the GUI.
     *
     * Used by: Many panels to close themselves. TODO: is this right?
     *
     * @param component The {@code Component} to remove.
     */
    public void removeComponent(Component component) {}

    /**
     * Remove a dialog from the GUI.
     *
     * Used by: FreeColDialog.removeNotify
     *
     * @param fcd The {@code FreeColDialog} to remove.
     */
    public void removeDialog(FreeColDialog<?> fcd) {}

    /**
     * Remove a trade route panel and associated input on an associated
     * TradeRouteInputPanel.
     *
     * Used by: TradeRoutePanel
     *
     * @param panel The {@code FreeColPanel} to remove.
     */
    public void removeTradeRoutePanel(FreeColPanel panel) {}
            
    /**
     * Set dialog preferred size to saved size or to the given
     * {@code Dimension} if no saved size was found.
     *
     * Call this method in the constructor of a FreeColPanel in order
     * to remember its size and position.
     *
     * Used by: *Panel
     *
     * @param comp The {@code Component} to use.
     * @param d The {@code Dimension} to use as default.
     */
    public void restoreSavedSize(Component comp, Dimension d) {}

    /**
     * Shows a tile popup for a given tile.
     *
     * @param tile The {@code Tile} where the popup occurred.
     */
    public void showTilePopup(Tile tile) {}

    /**
     * Get the tile at given coordinate.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @return The {@code Tile} found.
     */
    public Tile tileAt(int x, int y) { return null; }

    /**
     * Update all panels derived from the EuropePanel.
     *
     * Used by: NewUnitPanel, RecruitUnitPanel
     */
    public void updateEuropeanSubpanels() {}


    // Panel display, usually used just by the associated action

    /**
     * Show the AboutPanel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showAboutPanel() { return null; }

    /**
     * Show the build queue for a colony.
     *
     * @param colony The {@code Colony} to show a panel for.
     * @return The build queue panel.
     */
    public FreeColPanel showBuildQueuePanel(Colony colony) { return null; }

    /**
     * Show the dialog to select captured goods.
     *
     * @param unit The {@code Unit} capturing goods.
     * @param gl The list of {@code Goods} to choose from.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showCaptureGoodsDialog(final Unit unit, List<Goods> gl,
                                       DialogHandler<List<Goods>> handler) {}

    /**
     * Show the chat panel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showChatPanel() { return null; }

    /**
     * Show the founding father choice panel.
     *
     * @param ffs The list of {@code FoundingFather}s to choose from.
     * @param handler The callback to pass the choice to.
     */
    public void showChooseFoundingFatherDialog(final List<FoundingFather> ffs,
                                               DialogHandler<FoundingFather> handler) {}

    /**
     * Show the client options dialog.
     */
    public void showClientOptionsDialog() {}

    /**
     * Refreshes the GUI with settings from the client options.
     */
    public void refreshGuiUsingClientOptions() {}
    
    /**
     * Show the colony panel
     *
     * @param colony The {@code Colony} to display.
     * @param unit An optional {@code Unit} to select within the panel.
     * @return The panel shown.
     */
    public FreeColPanel showColonyPanel(Colony colony, Unit unit) { return null; }

    /**
     * Show a colopedia panel.
     *
     * @param nodeId The identifier for the colopedia node to show.
     * @return The panel shown.
     */
    public FreeColPanel showColopediaPanel(String nodeId) { return null; }

    /**
     * Show a color chooser panel.
     *
     * @param al An {@code ActionListener} to handle panel button presses.
     * @return The panel shown.
     */
    public FreeColPanel showColorChooserPanel(ActionListener al) { return null; }

    /**
     * Show the compact labour report panel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showCompactLabourReport() { return null; }

    /**
     * Show the compact labour report for the specified unit data.
     *
     * @param unitData The {@code UnitData} to display.
     * @return The panel shown.
     */
    public FreeColPanel showCompactLabourReport(UnitData unitData) { return null; }
    
    /**
     * Confirm declaration of independence.
     *
     * @return A list of new nation and country names.
     */
    public List<String> showConfirmDeclarationDialog() { return Collections.<String>emptyList(); }

    /**
     * Show the declaration panel with the declaration of independence and
     * an animated signature.
     * 
     * @param afterClosing A callback that is executed after the panel closes.
     */
    public void showDeclarationPanel(Runnable afterClosing) { }

    /**
     * Show a dialog for a difficulty option group.
     *
     * @param spec The enclosing {@code Specification}.
     * @param group The {@code OptionGroup} to show.
     * @param editable If true, the option group can be edited.
     * @return The (possibly modified) {@code OptionGroup}.
     */
    public void showDifficultyDialog(Specification spec,
                                            OptionGroup group,
                                            boolean editable,
                                            DialogHandler<OptionGroup> dialogHandler) {  }

    /**
     * Show a dialog to choose what goods to dump.
     *
     * @param unit The {@code Unit} that is dumping goods.
     * @param handler A callback to pass the dumped goods list to.
     */
    public void showDumpCargoDialog(Unit unit,
                                    DialogHandler<List<Goods>> handler) {}

    /**
     * Show a dialog for editing an individual option.
     *
     * @param option The {@code Option} to edit.
     * @return True if the option edit was accepted.
     */
    public boolean showEditOptionDialog(Option option) { return false; }

    /**
     * Show a dialog for editing a settlmeent.
     *
     * @param is The {@code IndianSettlement} to edit.
     * @return The settlement post-edit.
     */
    public IndianSettlement showEditSettlementDialog(IndianSettlement is) { return null; }

    /**
     * Show a dialog to handle emigration.
     *
     * @param player The {@code Player} whose emigration state needs work.
     * @param fountainOfYouth True if a Fountain of Youth event occurred.
     * @param handler A callback to pass a selected emigration index to.
     */
    public void showEmigrationDialog(final Player player,
                                     final boolean fountainOfYouth,
                                     DialogHandler<Integer> handler) {}

    /**
     * Show a dialog for the end of turn.
     *
     * @param units A list of {@code Unit}s that can still move.
     * @param handler A callback to handle the user selected end turn state.
     */
    public void showEndTurnDialog(final List<Unit> units,
                                  DialogHandler<Boolean> handler) {}

    /**
     * Show an error panel.
     *
     * @param message The error message to display.
     * @param callback An optional {@code Runnable} to run on close.
     * @return The panel shown.
     */
    public FreeColPanel showErrorPanel(String message, Runnable callback) { return null; }

    /**
     * Show the Europe panel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showEuropePanel() { return null; }

    /**
     * Show an event panel.
     *
     * @param header The title.
     * @param image A resource key for the image to display.
     * @param footer Optional footer text.
     * @return The panel shown.
     */
    public FreeColPanel showEventPanel(String header, String image,
                                       String footer) { return null; }

    /**
     * Show the FindSettlement panel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showFindSettlementPanel() { return null; }

    /**
     * Show a first contact dialog.
     *
     * @param player The {@code Player} making contact.
     * @param other The {@code Player} being contacted.
     * @param tile The {@code Tile} where the contact occurs.
     * @param settlementCount A count of settlements described by the
     *     other player.
     * @param handler A callback to handle the player decision to be friendly.
     */
    public void showFirstContactDialog(final Player player, final Player other,
                                       final Tile tile, int settlementCount,
                                       DialogHandler<Boolean> handler) {}

    /**
     * Show the Game options dialog.
     *
     * @param editable True if the options can be edited.
     * @param dialogHandler A callback for handling the closing of the dialog.
     */
    public void showGameOptionsDialog(boolean editable, DialogHandler<OptionGroup> dialogHandler) { }

    /**
     * Show the high scores panel.
     *
     * @param messageId The message identifier.
     * @param scores The {@code HighScore}s to display.
     * @return The panel shown.
     */
    public FreeColPanel showHighScoresPanel(String messageId,
                                            List<HighScore> scores) { return null; }

    /**
     * Show a panel for a native settlement.
     *
     * @param is The {@code IndianSettlement} to display.
     * @return The panel shown.
     */
    public FreeColPanel showIndianSettlementPanel(IndianSettlement is) { return null; }

    /**
     * Show an information message.
     *
     * @param displayObject Optional object for displaying as an icon.
     * @param template The {@code StringTemplate} to display.
     * @return The panel shown.
     */
    public FreeColPanel showInformationPanel(FreeColObject displayObject,
                                             StringTemplate template) { return null; }

    /**
     * Show a dialog where the user may choose a file.
     *
     * @param directory The directory containing the files.
     * @param extension An extension to select with.
     * @return The selected {@code File}.
     */
    public File showLoadDialog(File directory, String... extension) { return null; }

    /**
     * Show the LoadingSavegameDialog.
     *
     * @param publicServer FIXME
     * @param singlePlayer FIXME
     * @return The {@code LoadingSavegameInfo} from the dialog.
     */
    public LoadingSavegameInfo showLoadingSavegameDialog(boolean publicServer,
                                                         boolean singlePlayer) { return null; }

    /**
     * Show the log file panel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showLogFilePanel() { return null; }

    /**
     * Show the main panel.
     *
     * @param userMsg An optional user message to display.
     * @return The panel shown.
     */
    public FreeColPanel showMainPanel(String userMsg) { return null; }

    /**
     * Complete reset back to the main panel.
     */
    public void showMainTitle() {}

    /**
     * Show the map generator options.
     *
     * @param editable If true, allow edits.
     */
    public void showMapGeneratorOptionsDialog(boolean editable, DialogHandler<OptionGroup> dialogHandler) { }

    /**
     * Show the map size dialog.
     *
     * @return The selected map size as a {@code Dimension}.
     */
    public Dimension showMapSizeDialog() { return null; }

    /**
     * Show model messages.
     *
     * @param modelMessages A list of {@code ModelMessage}s to display.
     * @return The panel shown.
     */
    public FreeColPanel showModelMessages(List<ModelMessage> modelMessages) { return null; }

    /**
     * Show the monarch dialog.
     *
     * @param action The action the monarch is taking.
     * @param template A message template.
     * @param monarchKey The identifier for the monarch.
     * @param handler A callback to handle the user response to the
     *     monarch action.
     */
    public void showMonarchDialog(final MonarchAction action,
                                  StringTemplate template, String monarchKey,
                                  DialogHandler<Boolean> handler) {}

    /**
     * Show the naming dialog.
     *
     * @param template A message template.
     * @param defaultName The default name for the object.
     * @param unit The {@code Unit} that is naming.
     * @param handler A callback to handle the user response.
     */
    public void showNamingDialog(StringTemplate template,
                                 final String defaultName,
                                 final Unit unit,
                                 DialogHandler<String> handler) {}

    /**
     * Show the native demand dialog.
     *
     * @param unit The demanding {@code Unit}.
     * @param colony The {@code Colony} being demanded of.
     * @param type The {@code GoodsType} demanded (null for gold).
     * @param amount The amount of goods or gold demanded.
     * @param handler A callback to handle the user response.
     */
    public void showNativeDemandDialog(Unit unit, Colony colony,
                                       GoodsType type, int amount,
                                       DialogHandler<Boolean> handler) {}

    /**
     * Show the negotiation dialog.
     *
     * @param our Our {@code FreeColGameObject} that is negotiating.
     * @param other The other {@code FreeColGameObject}.
     * @param agreement The current {@code DiplomaticTrade} agreement.
     * @param comment An optional {@code StringTemplate} containing a
     *     commentary message.
     * @return The negotiated {@code DiplomaticTrade} agreement.
     */
    public DiplomaticTrade showNegotiationDialog(FreeColGameObject our,
                                                 FreeColGameObject other,
                                                 DiplomaticTrade agreement,
                                                 StringTemplate comment) { return null; }

    /**
     * Show the NewPanel.
     *
     * @param spec The {@code Specification} to use.
     * @return The panel shown.
     */
    public FreeColPanel showNewPanel(Specification spec) { return null; }

    /**
     * Show the parameter choice dialog.
     *
     * @return The chosen parameters.
     */
    public Parameters showParametersDialog() { return null; }

    /**
     * Show the pre-combat dialog.
     *
     * @param attacker The {@code Unit} that is attacking.
     * @param defender The {@code FreeColObject} that is defending.
     * @param tile The {@code Tile} where the attack occurs.
     * @return True if the player decided to attack.
     */
    public boolean showPreCombatDialog(Unit attacker,
                                       FreeColGameObject defender,
                                       Tile tile) { return false; }

    /**
     * Displays the purchase panel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showPurchasePanel() { return null; }

    /**
     * Displays the recruit panel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showRecruitPanel() { return null; }

    /**
     * Show the Cargo Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportCargoPanel() { return null; }

    /**
     * Show the Colony Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportColonyPanel() { return null; }

    /**
     * Show the Continental Congress Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportContinentalCongressPanel() { return null; }

    /**
     * Show the Education Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportEducationPanel() { return null; }

    /**
     * Show the Exploration Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportExplorationPanel() { return null; }

    /**
     * Show the Foreign Affairs Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportForeignAffairPanel() { return null; }

    /**
     * Show the History Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportHistoryPanel() { return null; }

    /**
     * Show the Native Affairs Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportIndianPanel() { return null; }

    /**
     * Show the Labour Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportLabourPanel() { return null; }

    /**
     * Display the labour detail panel.
     *
     * @param unitType The {@code UnitType} to display.
     * @param data The labour data.
     * @param unitCount A map of unit distribution.
     * @param colonies The list of player {@code Colony}s.
     * @return The panel shown.
     */
    public FreeColPanel showReportLabourDetailPanel(UnitType unitType,
        Map<UnitType, Map<Location, Integer>> data,
        TypeCountMap<UnitType> unitCount, List<Colony> colonies) { return null; }

    /**
     * Show the Military Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportMilitaryPanel() { return null; }

    /**
     * Show the Naval Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportNavalPanel() { return null; }

    /**
     * Show the Production Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportProductionPanel() { return null; }

    /**
     * Show the Religion Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportReligiousPanel() { return null; }

    /**
     * Show the Requirements Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportRequirementsPanel() { return null; }

    /**
     * Show the Trade Report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showReportTradePanel() { return null; }

    /**
     * Show the Turn Report.
     *
     * @param messages The {@code ModelMessage}s that make up the report.
     * @return The panel shown.
     */
    public FreeColPanel showReportTurnPanel(List<ModelMessage> messages) { return null; }

    /**
     * Show the river style dialog.
     *
     * @param styles The river styles a choice is made from.
     * @return The response returned by the dialog.
     */
    public String showRiverStyleDialog(List<String> styles) { return null; }

    /**
     * Show the save dialog.
     *
     * @param directory The directory containing the files.
     * @param defaultName The default game to save.
     * @return The selected file.
     */
    public File showSaveDialog(File directory, String defaultName) { return null; }

    /**
     * Show the map scale dialog.
     *
     * @return The map scale as a {@code Dimension}.
     */
    public Dimension showScaleMapSizeDialog() { return null; }

    /**
     * Show a dialog allowing selecting an amount of goods.
     *
     * @param goodsType The {@code GoodsType} to select an amount of.
     * @param available The amount of goods available.
     * @param defaultAmount The amount to select to start with.
     * @param needToPay If true, check the player has sufficient funds.
     * @return The amount selected.
     */
    public int showSelectAmountDialog(GoodsType goodsType, int available,
                                      int defaultAmount,
                                      boolean needToPay) { return -1; }

    /**
     * Show a dialog allowing the user to select a destination for
     * a given unit.
     *
     * @param unit The {@code Unit} to select a destination for.
     * @return A destination for the unit, or null.
     */
    public Location showSelectDestinationDialog(Unit unit) { return null; }

    /**
     * Show the select-tribute-amount dialog.
     *
     * @param question a {@code StringTemplate} describing the
     *     amount of tribute to demand.
     * @param maximum The maximum amount available.
     * @return The amount selected.
     */
    public int showSelectTributeAmountDialog(StringTemplate question,
                                             int maximum) { return -1; }

    /**
     * Show the {@code ServerListPanel}.
     *
     * @param serverList The list containing the servers retrieved from the
     *     metaserver.
     * @return The panel shown.
     */
    public FreeColPanel showServerListPanel(List<ServerInfo> serverList) { return null; }

    /**
     * Show the StartGamePanel.
     *
     * @param game The {@code Game} that is about to start.
     * @param player The {@code Player} using this client.
     * @param singlePlayerMode True to start a single player game.
     * @return The panel shown.
     */
    public FreeColPanel showStartGamePanel(Game game, Player player,
                                           boolean singlePlayerMode) { return null; }

    /**
     * Show the statistics panel.
     *
     * @param serverStats A map of server statistics key,value pairs.
     * @param clientStats A map of client statistics key,value pairs.
     * @return The panel shown.
     */
    public FreeColPanel showStatisticsPanel(Map<String, String> serverStats,
                                            Map<String, String> clientStats) { return null; }

    /**
     * Shows a status message which goes away when a new component is added.
     *
     * @param message The text message to display on the status panel.
     * @return The panel shown.
     */
    public FreeColPanel showStatusPanel(String message) { return null; }

    /**
     * Show the tile panel for a given tile.
     *
     * @param tile The {@code Tile} to display.
     * @return The panel shown.
     */
    public FreeColPanel showTilePanel(Tile tile) { return null; }

    /**
     * Show the trade route input panel for a given trade route.
     *
     * @param tr The {@code TradeRoute} to display.
     * @return The panel shown.
     */
    public FreeColPanel showTradeRouteInputPanel(TradeRoute tr) { return null; }

    /**
     * Show a panel to select a trade route for a unit.
     *
     * @param unit An optional {@code Unit} to select a trade route for.
     * @return The panel shown.
     */
    public FreeColPanel showTradeRoutePanel(Unit unit) { return null; }

    /**
     * Show the training panel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showTrainPanel() { return null; }

    /**
     * Show the victory dialog.
     *
     * @param handler A callback to handle the continuation decision.
     */
    public void showVictoryDialog(DialogHandler<Boolean> handler) {}

    /**
     * Show the warehouse dialog for a colony.
     *
     * Run out of ColonyPanel, so the tile is already displayed.
     *
     * @param colony The {@code Colony} to display.
     * @param dialogHandler A {@code DialogHandler} for the dialog response.
     */
    public void showWarehouseDialog(Colony colony, DialogHandler<Boolean> dialogHandler) {  }

    /**
     * Show the production of a unit.
     *
     * @param unit The {@code Unit} to display.
     * @return The panel shown.
     */
    public FreeColPanel showWorkProductionPanel(Unit unit) { return null; }

    /**
     * Reloads all images managed by {@code ResourceManager}.
     */
    public void reloadResources() {

    }
    
    /**
     * Prepares showing the main menu by removing almost everything from the view.
     */
    public void prepareShowingMainMenu() {
       
    }

    /**
     * Checks if mods with a specification can be added now.
     */
    public boolean canGameChangingModsBeAdded() {
        return true;
    }


    /**
     * A method to be called only on serious Errors in order
     * to ensure sufficient memory for displaying an error
     * message.
     */
    public void emergencyPurge() {

    }
}
