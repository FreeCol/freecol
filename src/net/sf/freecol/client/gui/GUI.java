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

package net.sf.freecol.client.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.client.control.MapTransform;
import net.sf.freecol.client.gui.panel.BuildQueuePanel;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.ColorChooserPanel;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.panel.MiniMap;
import net.sf.freecol.client.gui.panel.report.LabourData.UnitData;
import net.sf.freecol.client.gui.panel.TradeRouteInputPanel;
import net.sf.freecol.client.gui.dialog.FreeColDialog;
import net.sf.freecol.client.gui.dialog.Parameters;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.debug.DebugUtils;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.metaserver.ServerInfo;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Constants.*; // Imports all ENUMS.
import net.sf.freecol.common.model.DiplomaticTrade;
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
import net.sf.freecol.common.model.TileType;
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

    /**
     * Error handler class to display a message with this GUI.
     */
    public class ErrorJob implements Runnable {

        private final StringTemplate template;
        private Runnable runnable;

        
        public ErrorJob(Exception ex, String key) {
            this.template = FreeCol.errorFromException(ex, key);
            this.runnable = null;
        }

        public ErrorJob(Exception ex, StringTemplate tmpl) {
            this.template = FreeCol.errorFromException(ex, tmpl);
            this.runnable = null;
        }

        public ErrorJob(String key) {
            this.template = StringTemplate.template(key);
            this.runnable = null;
        }

        public ErrorJob(StringTemplate template) {
            this.template = template;
            this.runnable = null;
        }

        public ErrorJob setRunnable(Runnable runnable) {
            this.runnable = runnable;
            return this;
        }

        public void invokeLater() {
            SwingUtilities.invokeLater(this);
        }

        @Override
        public void run() {
            GUI.this.closeMenus();
            GUI.this.showErrorMessage(this.template, null, this.runnable);
        }

        @Override
        public String toString() {
            return Messages.message(this.template);
        }
    }

    /** Warning levels. */
    private static final String levels[] = {
        "low", "normal", "high"
    };

    /** An image library to use. */
    protected final ImageLibrary imageLibrary;


    /**
     * Create the GUI.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param scaleFactor The scale factor for the GUI.
     */
    public GUI(FreeColClient freeColClient, float scaleFactor) {
        super(freeColClient);

        this.imageLibrary = new ImageLibrary(scaleFactor);
    }


    // Useful utilities provided in addition to the implementable interface

    // Miscellaneous

    /**
     * Get the image library.
     *
     * @return The base image library at the current scale.
     */
    public ImageLibrary getImageLibrary() {
        return this.imageLibrary;
    }

    /**
     * Toggle the current view mode.
     *
     * Only really toggles between terrain and units modes.
     */
    public void toggleViewMode() {
        ViewMode vm = getViewMode();
        switch (vm) {
        case MOVE_UNITS:
            changeView(getSelectedTile());
            break;
        case TERRAIN:
            changeView(getActiveUnit());
            break;
        default:
            break;
        }
    }

    
    // Error handling

    /**
     * Create a new error job from a given exception and message key.
     *
     * @param ex The {@code Exception} to use.
     * @param key The message key.
     * @return An {@code ErrorJob} to display the error using this GUI.
     */
    public ErrorJob errorJob(Exception ex, String key) {
        return new ErrorJob(ex, key);
    }

    /**
     * Create a new error job from a given exception and template.
     *
     * @param ex The {@code Exception} to use.
     * @param template The {@code StringTemplate}.
     * @return An {@code ErrorJob} to display the error using this GUI.
     */
    public ErrorJob errorJob(Exception ex, StringTemplate template) {
        return new ErrorJob(ex, template);
    }
    
    /**
     * Create a new error job from a given message key.
     *
     * @param key The message key.
     * @return An {@code ErrorJob} to display the error using this GUI.
     */
    public ErrorJob errorJob(String key) {
        return new ErrorJob(key);
    }

    /**
     * Create a new error job from a given template.
     *
     * @param template The {@code StringTemplate}.
     * @return An {@code ErrorJob} to display the error using this GUI.
     */
    public ErrorJob errorJob(StringTemplate template) {
        return new ErrorJob(template);
    }
    
    
    // Invocation methods

    /**
     * Wrapper for SwingUtilities.invokeLater that handles the case
     * where we are already in the EDT.
     *
     * @param runnable A {@code Runnable} to run.
     */
    public void invokeNowOrLater(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    /**
     * Wrapper for SwingUtilities.invokeAndWait that handles the case
     * where we are already in the EDT.
     *
     * @param runnable A {@code Runnable} to run.
     */
    public void invokeNowOrWait(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (InterruptedException | InvocationTargetException ex) {
                logger.log(Level.WARNING, "Client GUI interaction", ex);
            }
        }
    }


    // High level dialogs, usually using the dialog primitives

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
        return confirm(tile, template,
            new ImageIcon(imageLibrary.getScaledGoodsTypeImage(goodsType)),
            okKey, cancelKey);
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
        return confirm(tile, template,
            new ImageIcon(imageLibrary.getScaledSettlementImage(settlement)),
            okKey, cancelKey);
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
        return confirm(tile, template,
            new ImageIcon(imageLibrary.getScaledUnitImage(unit)),
            okKey, cancelKey);
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
            showInformationMessage(t);
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
        return confirm(attacker.getTile(), StringTemplate
            .template(messageId)
            .addStringTemplate("%nation%", enemy.getNationLabel()),
            attacker, "confirmHostile.yes", "cancel");
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
            showInformationMessage(message);
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
        return (confirm(is.getTile(), StringTemplate.template(messageId)
                .addName("%settlement%", is.getName())
                .addStringTemplate("%nation%", other.getNationLabel()),
                attacker, "confirmTribute.yes", "confirmTribute.no"))
            ? 1 : -1;
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
    public ArmedUnitSettlementAction getArmedUnitSettlementChoice(Settlement settlement) {
        final Player player = getMyPlayer();

        List<ChoiceItem<ArmedUnitSettlementAction>> choices = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("armedUnitSettlement.tribute"),
                ArmedUnitSettlementAction.SETTLEMENT_TRIBUTE));
        choices.add(new ChoiceItem<>(Messages.message("armedUnitSettlement.attack"),
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
     * @param europe The player {@code Europe} where the boycott
     *     is in force.
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
        //Get Buy price on Europe Market for comparison
        int euroPrice = unit.getOwner().getMarket()
            .getBidPrice(goods.getType(), goods.getAmount());
        StringTemplate template = StringTemplate.template("buy.text")
            .addStringTemplate("%nation%", settlement.getOwner().getNationLabel())
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
        return getChoice(null, explain,
            new ImageIcon(ImageLibrary.getPlaceholderImage()),
            cancelKey, choices);
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
        return getChoice(tile, template,
            new ImageIcon(imageLibrary.getScaledGoodsTypeImage(goodsType)),
            cancelKey, choices);
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
        return getChoice(tile, template,
            new ImageIcon(imageLibrary.getScaledNationImage(nation)),
            cancelKey, choices);
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
        return getChoice(tile, template,
            new ImageIcon(imageLibrary.getScaledSettlementImage(settlement)),
            cancelKey, choices);
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
        return getChoice(tile, template,
            new ImageIcon(imageLibrary.getScaledUnitImage(unit)),
            cancelKey, choices);
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

        return getChoice(tile, template,
                         owner.getNation(), "indianLand.cancel", choices);
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

        ArrayList<ChoiceItem<TradeAction>> choices = new ArrayList<>();
        if (canBuy) {
            choices.add(new ChoiceItem<>(Messages.message("tradeProposition.toBuy"),
                                         TradeAction.BUY, canBuy));
        }
        if (canSell) {
            choices.add(new ChoiceItem<>(Messages.message("tradeProposition.toSell"),
                                         TradeAction.SELL, canSell));
        }
        if (canGift) {
            choices.add(new ChoiceItem<>(Messages.message("tradeProposition.toGift"),
                                         TradeAction.GIFT, canGift));
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
        StringTemplate template = StringTemplate.label("\n\n")
            .addStringTemplate(is.getAlarmLevelLabel(unit.getOwner()))
            .addStringTemplate(StringTemplate
                .template("missionarySettlement.question")
                .addName("%settlement%", is.getName()));

        List<ChoiceItem<MissionaryAction>> choices = new ArrayList<>();
        if (canEstablish) {
            choices.add(new ChoiceItem<>(Messages.message("missionarySettlement.establish"),
                    MissionaryAction.MISSIONARY_ESTABLISH_MISSION,
                    canEstablish));
        }
        if (canDenounce) {
            choices.add(new ChoiceItem<>(Messages.message("missionarySettlement.heresy"),
                    MissionaryAction.MISSIONARY_DENOUNCE_HERESY,
                    canDenounce));
        }
        choices.add(new ChoiceItem<>(Messages.message("missionarySettlement.incite"),
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
        String name = getInput(tile, StringTemplate
            .template("nameColony.text"), suggested,
            "accept", "cancel");
        if (name == null) {
            // Cancelled
        } else if (name.isEmpty()) {
            showInformationMessage("enterSomeText"); // 0-length is invalid
        } else if (player.getSettlementByName(name) != null) {
            // Must be unique
            showInformationMessage(tile, StringTemplate
                .template("nameColony.notUnique")
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
        StringTemplate template = StringTemplate.template("scoutColony.text")
            .addStringTemplate("%unit%", unit.getLabel(Unit.UnitLabelType.NATIONAL))
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
    public ScoutIndianSettlementAction getScoutIndianSettlementChoice(IndianSettlement is,
        String numberString) {
        final Player player = getMyPlayer();
        final Player owner = is.getOwner();

        StringTemplate template = StringTemplate.label("")
            .addStringTemplate(is.getAlarmLevelLabel(player))
            .addName("\n\n")
            .addStringTemplate(StringTemplate
                .template("scoutSettlement.greetings")
                .addStringTemplate("%nation%", owner.getNationLabel())
                .addName("%settlement%", is.getName())
                .addName("%number%", numberString)
                .add("%settlementType%",
                    ((IndianNationType)owner.getNationType()).getSettlementTypeKey(true)))
            .addName(" ");
        if (is.getLearnableSkill() != null) {
            template
                .addStringTemplate(StringTemplate
                    .template("scoutSettlement.skill")
                    .addNamed("%skill%", is.getLearnableSkill()))
                .addName(" ");
        }
        int present = is.getWantedGoodsCount();
        if (present > 0) {
            StringTemplate t = StringTemplate.template("scoutSettlement.trade."
                + Integer.toString(present));
            for (int i = 0; i < present; i++) {
                String tradeKey = "%goods" + Integer.toString(i+1) + "%";
                t.addNamed(tradeKey, is.getWantedGoods(i));
            }
            template.addStringTemplate(t).addName("\n\n");
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
        int euroPrice = unit.getOwner().getMarket().getSalePrice(goods.getType(), goods.getAmount());
        StringTemplate goodsTemplate = goods.getLabel(true);
        StringTemplate template = StringTemplate.template("sell.text")
            .addStringTemplate("%nation%", settlement.getOwner().getNationLabel())
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

    public final OptionGroup showDifficultyDialog() {
        final Specification spec = getSpecification();
        return showDifficultyDialog(spec, spec.getDifficultyOptionGroup(),
                                    false);
    }

    /**
     * Show an i18n compliant error message derived from a template.
     *
     * @param template The {@code StringTemplate} containing the message.
     */
    public final void showErrorMessage(StringTemplate template) {
        showErrorMessage(template, null);
    }

    /**
     * Show an i18n compliant error message derived from a template,
     * with optional extra debug information.
     *
     * @param template The {@code StringTemplate} containing the message.
     * @param message Optional extra debug information.
     */
    public final void showErrorMessage(StringTemplate template,
                                       String message) {
        showErrorMessage(template, message, null);
    }
    
    /**
     * Show an i18n compliant error message derived from a template,
     * with optional extra debug information and an optional callback.
     *
     * @param template The {@code StringTemplate} containing the message.
     * @param message Optional extra debug information.
     * @param callback Optional routine to run when the error panel is closed.
     */
    public final void showErrorMessage(StringTemplate template, String message,
                                       Runnable callback) {
        String display = Messages.message(template);
        if (message != null && FreeColDebugger.isInDebugMode()) {
            display += "/" + message + "/";
        }
        showErrorMessage(display, callback);
    }

    /**
     * Show an information message.
     *
     * @param messageId The message key.
     */
    public final void showInformationMessage(String messageId) {
        showInformationMessage(StringTemplate.key(messageId));
    }

    /**
     * Show an information message.
     *
     * @param template The message template.
     */
    public final void showInformationMessage(StringTemplate template) {
        showInformationMessage(null, template);
    }

    /**
     * Show an information message.
     *
     * @param displayObject An optional object to display as an icon.
     * @param messageId The message key.
     */
    public final void showInformationMessage(FreeColObject displayObject,
                                             String messageId) {
        showInformationMessage(displayObject, StringTemplate.key(messageId));
    }

    /**
     * Show a save file dialog, selecting one to load.
     *
     * @param root The root directory to look in.
     * @param extension The file extension to look for.
     * @return The {@code File} selected, or null on error.
     */
    public final File showLoadSaveFileDialog(File root, String extension) {
        File file = showLoadDialog(root, extension);
        if (file != null && !file.isFile()) {
            showErrorMessage(FreeCol.badFile("error.noSuchFile", file));
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

    /**
     * Show a settlement.
     *
     * @param settlement The {@code Settlement} to display.
     */
    private final void showSettlement(Settlement settlement) {
        if (settlement instanceof Colony) {
            if (getMyPlayer().owns(settlement)) {
                showColonyPanel((Colony)settlement, null);
            } else {
                DebugUtils.showForeignColony(getFreeColClient(),
                                             (Colony)settlement);
            }
        } else if (settlement instanceof IndianSettlement) {
            showIndianSettlement((IndianSettlement)settlement);
        }
    }
        
    /**
     * Display the appropriate panel for any settlement on a tile, as visible
     * to a given player.
     *
     * @param tile The {@code Tile} to check for settlements.
     */
    public final void showTileSettlement(Tile tile) {
        if (tile == null) return;
        Settlement settlement = tile.getSettlement();
        if (settlement == null) return;
        showSettlement(settlement);
    }


    // Sound routines, delegated to the SoundController, only useful
    // for GUI classes in need of sound

    /**
     * Play a sound.
     *
     * @param sound The sound resource to play, or if null stop playing.
     */
    public void playSound(String sound) {
        getSoundController().playSound(sound);
    }

    /**
     * Plays an alert sound for an information message if the
     * option for it is turned on.
     */
    private void alertSound() {
        if (getClientOptions().getBoolean(ClientOptions.AUDIO_ALERTS)) {
            playSound("sound.event.alertSound");
        }
    }

    /**
     * Get the label text for the sound player mixer.
     *
     * Needed by the audio mixer option UI.
     *
     * @return The text.
     */
    public String getSoundMixerLabelText() {
        return getSoundController().getSoundMixerLabelText();
    }


    // Miscellaneous higher level utilities

    /**
     * Create a thumbnail for the minimap.
     * 
     * FIXME: Delete all code inside this method and replace it with
     *        sensible code directly drawing in necessary size,
     *        without creating a throwaway GUI panel, drawing in wrong
     *        size and immediately resizing.
     * @return The created {@code BufferedImage}.
     */
    public BufferedImage createMiniMapThumbNail() {
        MiniMap miniMap = new MiniMap(getFreeColClient());
        miniMap.setTileSize(MiniMap.MAX_TILE_SIZE);
        Game game = getGame();
        int width = game.getMap().getWidth() * MiniMap.MAX_TILE_SIZE
            + MiniMap.MAX_TILE_SIZE / 2;
        int height = game.getMap().getHeight() * MiniMap.MAX_TILE_SIZE / 4;
        miniMap.setSize(width, height);
        BufferedImage image = new BufferedImage(
            width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = image.createGraphics();
        miniMap.paintMap(g1);
        g1.dispose();

        int scaledWidth = Math.min((int)((64 * width) / (float)height), 128);
        BufferedImage scaledImage = new BufferedImage(scaledWidth, 64,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaledImage.createGraphics();
        g2.drawImage(image, 0, 0, scaledWidth, 64, null);
        g2.dispose();
        return scaledImage;
    }


    // Below here, routines should be just stubs that need full
    // implementations.

    
    // Simple accessors

    /**
     * Get the canvas.
     *
     * @return Null here, real implementations will override.
     */
    public Canvas getCanvas() {
        return null;
    }

    /**
     * Get the tile image library.
     *
     * @return Null here, real implementations will override.
     */
    public ImageLibrary getTileImageLibrary() {
        return null;
    }

    /**
     * Is this GUI in windowed mode?
     *
     * @return True by default, real implementations will override.
     */
    public boolean isWindowed() {
        return true;
    }


    // Initialization and teardown

    /**
     * Change the windowed mode.
     */
    public void changeWindowedMode() {}

    /**
     * Display the splash screen.
     *
     * @param splashStream A stream to find the image in.
     */
    public void displaySplashScreen(final InputStream splashStream) {}

    /**
     * Hide the splash screen.
     */
    public void hideSplashScreen() {}

    /** 
     * Swing system and look-and-feel initialization.
     * 
     * @param fontName An optional font name to be used.
     * @exception FreeColException if the LAF is incompatible with the GUI.
     */
    public void installLookAndFeel(String fontName) throws FreeColException {}

    /**
     * Quit the GUI.  All that is required is to exit the full screen.
     */
    public void quit() {}

    /**
     * Reset the GUI on reconnect.
     *
     * @param active An optional active {@code Unit}.
     * @param tile An optional {@code Tile} to focus on if there is no
     *     active unit.
     */
    public void reconnect(Unit active, Tile tile) {}

    /**
     * Remove all in-game components (i.e. all the Listeners).
     */    
    public void removeInGameComponents() {}

    /**
     * Shows the {@code VideoPanel}.
     *
     * @param userMsg An optional user message.
     */
    public void showOpeningVideo(final String userMsg) {}

    /**
     * Starts the GUI by creating and displaying the GUI-objects.
     *
     * @param desiredWindowSize The desired size of the GUI window.
     */
    public void startGUI(final Dimension desiredWindowSize) {
        logger.info("It seems that the GraphicsEnvironment is headless!");
    }

    /**
     * Start the GUI for the map editor.
     */
    public void startMapEditorGUI() {}


    // Animation handling

    /**
     * Animate a unit attack.
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
     * @param unit The {@code Unit} that is moving.
     * @param srcTile The {@code Tile} the unit starts at.
     * @param dstTile The {@code Tile} the unit moves to.
     */
    public void animateUnitMove(Unit unit, Tile srcTile, Tile dstTile) {}

    /**
     * Update the GUI to warn that a unit is executing an animation and
     * should be ignored for its duration.
     *
     * @param unit The {@code} Unit that is animating.
     * @param sourceTile A {@code Tile} where the animation is occuring.
     * @param r A callback for the end of animation.
     */
    public void executeWithUnitOutForAnimation(Unit unit, Tile sourceTile,
                                               OutForAnimationCallback r) {}

    /**
     * Get the animation position.
     *
     * @param labelWidth The width of the label.
     * @param labelHeight The height of the label.
     * @param tileP The position of the {@code Tile} on the screen.
     * @return A point on the map to place a unit for movement.
     */
    public Point getAnimationPosition(int labelWidth, int labelHeight,
                                      Point tileP) {
        return tileP;
    }

    /**
     * Get the scale for animations.
     *
     * @return A scale factor for animations.
     */
    public float getAnimationScale() {
        return 1.0f;
    }

    /**
     * Get the bounds for a tile.
     *
     * @param tile The {@code Tile} to check.
     * @return The tile bounds.
     */
    public Rectangle getAnimationTileBounds(Tile tile) {
        return null;
    }

    /**
     * Get the map position for a tile.
     *
     * @param tile The {@code Tile} to check.
     * @return The tile position.
     */
    public Point getAnimationTilePosition(Tile tile) {
        return null;
    }


    // Dialog primitives

    /**
     * Simple modal confirmation dialog.
     *
     * @param textKey A string to use as the message key.
     * @param okKey A key for the message on the "ok" button.
     * @param cancelKey A key for the message on the "cancel" button.
     * @return True if the "ok" button was selected.
     */
    public boolean confirm(String textKey, String okKey, String cancelKey) {
        return false;
    }

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
     * @return The focus {@code Tile}.
     */
    public Tile getFocus() {
        return null;
    }

    /**
     * Request the Java-level focus go to the current subpanel.
     *
     * @return False if the focus request can not succeed.
     */
    public boolean requestFocusForSubPanel() {
        return false;
    }

    /**
     * Request the Java-level focus to the main window.
     *
     * @return False if the focus request can not succeed.
     */
    public boolean requestFocusInWindow() {
        return false;
    }

    /**
     * Require the given tile to be in the onScreen()-area.
     *
     * @param tile The {@code Tile} to check.
     * @return True if the focus was set.
     */
    public boolean requireFocus(Tile tile) {
        return false;
    }

    /**
     * Set the current focus tile.
     *
     * @param tileToFocus The new focus {@code Tile}.
     */
    public void setFocus(Tile tileToFocus) {}

    /**
     * Focus on the active unit.
     */
    public void focusActiveUnit() {
        final Unit active = getActiveUnit();
        if (active == null) return;
        Tile tile = active.getTile();
        if (tile == null) return;
        setFocus(tile);
    }


    // General GUI manipulation

    /**
     * Repaint the canvas now.
     */
    public void paintImmediately() {}

    /**
     * Repaint a part of the canvas now.
     *
     * @param rectangle The area to repaint.
     */
    public void paintImmediately(Rectangle rectangle) {}
    
    /**
     * Refresh the whole GUI.
     */
    public void refresh() {}

    /**
     * Refresh a particular tile.
     *
     * @param tile The {@code Tile} to refresh.
     */
    public void refreshTile(Tile tile) {}
    

    // Path handling

    /**
     * Set the path for the active unit.
     *
     * @param path The new unit path.
     */
    public void setUnitPath(PathNode path) {}

    /**
     * Start/stop the goto path display.
     */
    public void activateGotoPath() {}

    /**
     * Stop the goto path display.
     */
    public void clearGotoPath() {}

    /**
     * Perform an immediate goto to a tile with the active unit.
     *
     * Called from {@link TilePopup}.
     *
     * @param tile The {@code Tile} to go to.
     */
    public void performGoto(Tile tile) {}

    /**
     * Perform an immediate goto to a point on the map.
     *
     * Called from {@link CanvasMouseListener}.
     *
     * @param x The x coordinate of the goto destination (pixels).
     * @param y The x coordinate of the goto destination (pixels).
     */
    public void performGoto(int x, int y) {}
    
    /**
     * Send the active unit along the current goto path as far as possible.
     */
    public void traverseGotoPath() {}

    /**
     * Update the goto path to a new position on the map.
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
     * @param x Drag x coordinate (pixels).
     * @param y Drag x coordinate (pixels).
     */
    public void prepareDrag(int x, int y) {}

    
    // MapControls handling

    /**
     * Is the map able to zoom in further?
     *
     * @return True if the map can zoom in.
     */
    public boolean canZoomInMapControls() {
        return false;
    }

    /**
     * Is the map able to zoom out further?
     *
     * @return True if the map can zoom out.
     */
    public boolean canZoomOutMapControls() {
        return false;
    }

    /**
     * Enable the map controls.
     *
     * Called from the MapControlsAction.
     *
     * @param enable If true then enable.
     */
    public void enableMapControls(boolean enable) {}

    /**
     * Toggle the fog of war control.
     */
    public void miniMapToggleFogOfWarControls() {}

    /**
     * Toggle the view control.
     */
    public void miniMapToggleViewControls() {}

    /**
     * Update the map controls, including the InfoPanel according to
     * the view mode.
     */
    public void updateMapControls() {}

    /**
     * Map control update by removing and re-adding.
     * TODO: does this overlap with the preceding?
     */
    public void updateMapControlsInCanvas() {}

    /**
     * Zoom in the map controls.
     */
    public void zoomInMapControls() {}

    /**
     * Zoom out the map controls.
     */
    public void zoomOutMapControls() {}


    // Menu handling

    /**
     * Close any open menus.
     */
    public void closeMenus() {}

    /**
     * Reset the menu bar.
     */
    public void resetMenuBar() {}

    /**
     * Update the menu bar.
     */
    public void updateMenuBar() {}


    // Tile image manipulation

    public BufferedImage createTileImageWithOverlayAndForest(TileType type,
                                                             Dimension size) {
        return null;
    }

    public BufferedImage createTileImageWithBeachBorderAndItems(Tile tile) {
        return null;
    }

    public BufferedImage createTileImage(Tile tile, Player player) {
        return null;
    }

    public BufferedImage createColonyTileImage(Tile tile, Colony colony) {
        return null;
    }

    public void displayColonyTiles(Graphics2D g, Tile[][] tiles, Colony colony) {
    }


    // View mode handling, including accessors for the active unit for
    // MOVE_UNITS mode, and the selected tile for VIEW_TERRAIN mode.
    // In MAP_TRANSFORM mode the map transform lives in the
    // MapEditorController.

    /**
     * Get the current view mode.
     *
     * @return One of the view mode constants, or negative on error.
     */
    public ViewMode getViewMode() {
        return ViewMode.END_TURN;
    }

    /**
     * Get the active unit.
     *
     * @return The current active {@code Unit}.
     */
    public Unit getActiveUnit() {
        return null;
    }

    /**
     * Get the selected tile.
     *
     * @return The selected {@code Tile}.
     */
    public Tile getSelectedTile() {
        return null;
    }

    /**
     * Change to terrain mode and select a tile.
     *
     * @param tile The {@code Tile} to select.
     */
    public void changeView(Tile tile) {}

    /**
     * Change to move units mode, and select a unit.
     *
     * @param unit The {@code Unit} to select.
     */
    public void changeView(Unit unit) {}

    /**
     * Change to map transform mode, and select a transform.
     *
     * @param transform The {@code MapTransform} to select.
     */
    public void changeView(MapTransform transform) {}
    
    /**
     * Change to end turn mode.
     */
    public void changeView() {}


    // Zoom controls

    public boolean canZoomInMap() {
        return false;
    }

    public boolean canZoomOutMap() {
        return false;
    }

    protected void resetMapZoom() {
        ResourceManager.clearImageCache();
    }

    public void zoomInMap() {
        ResourceManager.clearImageCache();
    }

    public void zoomOutMap() {
        ResourceManager.clearImageCache();
    }


    // High level panel manipulation

    /**
     * Handle a click on the canvas.
     *
     * @param count The click count.
     * @param x The x coordinate of the click.
     * @param y The y coordinate of the click.
     */
    public void clickAt(int count, int x, int y) {}

    /**
     * Close a panel.
     *
     * @param panel The identifier for the panel to close.
     */
    public void closePanel(String panel) {}

    /**
     * Close the main panel if present.
     */
    public void closeMainPanel() {}

    /**
     * Close the status panel if present.
     */
    public void closeStatusPanel() {}

    /**
     * Confirm declaration of independence.
     *
     * @return A list of new nation and country names.
     */
    public List<String> confirmDeclaration() {
        return Collections.<String>emptyList();
    }

    /**
     * Update with a new chat message.
     *
     * @param player The player who sent the chat message.
     * @param message The chat message.
     * @param privateChat True if the message is private.
     */
    public void displayChat(Player player, String message,
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
     * @param player The player who sent the chat message.
     * @param message The chat message.
     * @param privateChat True if the message is private.
     */
    public void displayStartChat(Player player, String message,
                                 boolean privateChat) {}

    /**
     * Checks if a client options dialog is present.
     *
     * @return True if the client options are showing.
     */
    public boolean isClientOptionsDialogShowing() {
        return false;
    }

    /**
     * Is another panel being displayed.
     *
     * @return True if there is another panel present.
     */
    public boolean isShowingSubPanel() {
        return false;
    }

    /**
     * Attach a closing callback to any current error panel.
     *
     * @param callback The {@code Runnable} to attach.
     * @return True if an error panel was present.
     */
    public boolean onClosingErrorPanel(Runnable callback) {
        return false;
    }

    /**
     * Refresh the players table in the StartGamePanel.
     */
    public void refreshPlayersTable() {}

    /**
     * Remove a component from the GUI.
     *
     * @param component The {@code Component} to remove.
     */
    public void removeComponent(Component component) {}

    /**
     * Remove a dialog from the GUI.
     *
     * @param fcd The {@code FreeColDialog} to remove.
     */
    public void removeDialog(FreeColDialog<?> fcd) {}

    /**
     * Set dialog preferred size to saved size or to the given
     * {@code Dimension} if no saved size was found.
     *
     * Call this method in the constructor of a FreeColPanel in order
     * to remember its size and position.
     *
     * @param comp The {@code Component} to use.
     * @param d The {@code Dimension} to use as default.
     */
    public void restoreSavedSize(Component comp, Dimension d) {}

    /**
     * Show the AboutPanel.
     */
    public void showAboutPanel() {}

    /**
     * Show the build queue for a colony.
     *
     * @param colony The {@code Colony} to show a panel for.
     * @return The {@code BuildQueuePanel} showing.
     */
    public BuildQueuePanel showBuildQueuePanel(Colony colony) {
        return null;
    }

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
     */
    public void showChatPanel() {}

    /**
     * Show the founding father choice panel.
     *
     * @param ffs The list of {@code FoundingFather}s to choose from.
     * @param handler The callback to pass the choice to.
     */
    public void showChooseFoundingFatherDialog(final List<FoundingFather> ffs,
            DialogHandler<FoundingFather> handler) {
    }

    /**
     * Show the client options dialog.
     */
    public void showClientOptionsDialog() {}

    /**
     * Show the colony panel
     *
     * @param colony The {@code Colony} to display.
     * @param unit An optional {@code Unit} to select within the panel.
     * @return The {@code ColonyPanel} that is showing.
     */
    public ColonyPanel showColonyPanel(Colony colony, Unit unit) {
        return null;
    }

    /**
     * Show a colopedia panel.
     *
     * @param nodeId The identifier for the colopedia node to show.
     */
    public void showColopediaPanel(String nodeId) {}

    /**
     * Show a color chooser panel.
     *
     * @param al An {@code ActionListener} to handle panel button presses.
     * @return The {@code ColorChooserPanel} created.
     */
    public ColorChooserPanel showColorChooserPanel(ActionListener al) {
        return null;
    }

    /**
     * Show the compact labour report panel.
     */
    public void showCompactLabourReport() {}

    /**
     * Show the compact labour report for the specified unit data.
     *
     * @param unitData The {@code UnitData} to display.
     */
    public void showCompactLabourReport(UnitData unitData) {}
    
    /**
     * Show the declaration panel with the declaration of independence and
     * an animated signature.
     */
    public void showDeclarationPanel() {}

    /**
     * Show a dialog for a difficulty option group.
     *
     * @param spec The enclosing {@code Specification}.
     * @param group The {@code OptionGroup} to show.
     * @param editable If true, the option group can be edited.
     * @return The (possibly modified) {@code OptionGroup}.
     */
    public OptionGroup showDifficultyDialog(Specification spec,
                                            OptionGroup group,
                                            boolean editable) {
        return null;
    }

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
    public boolean showEditOptionDialog(Option option) {
        return false;
    }

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
     * Show an error message.  The error message should be fully formatted
     * by now.
     *
     * @param message The actual final error message.
     * @param callback Optional routine to run when the error panel is closed.
     */
    protected void showErrorMessage(String message, Runnable callback) {}

    /**
     * Show the Europe panel.
     */
    public void showEuropePanel() {}

    /**
     * Show an event panel.
     *
     * @param header The title.
     * @param image A resource key for the image to display.
     * @param footer Optional footer text.
     */
    public void showEventPanel(String header, String image, String footer) {}

    /**
     * Show the FindSettlement panel.
     */
    public void showFindSettlementPanel() {}

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
     * @return The game options {@code OptionGroup}.
     */
    public OptionGroup showGameOptionsDialog(boolean editable) {
        return null;
    }

    /**
     * Show the high scores panel.
     *
     * @param messageId The message identifier.
     * @param scores The {@code HighScore}s to display.
     */
    public void showHighScoresPanel(String messageId,
                                    List<HighScore> scores) {}

    /**
     * Show a panel for a native settlement.
     *
     * @param indianSettlement The {@code IndianSettlement} to display.
     */
    public void showIndianSettlement(IndianSettlement indianSettlement) {}

    /**
     * Show an information message.
     *
     * @param displayObject Optional object for displaying as an icon.
     * @param template The {@code StringTemplate} to display.
     */
    public void showInformationMessage(FreeColObject displayObject,
                                       StringTemplate template) {
        alertSound();
    }

    /**
     * Show a dialog where the user may choose a file.
     *
     * @param directory The directory containing the files.
     * @param extension An extension to select with.
     * @return The selected {@code File}.
     */
    public File showLoadDialog(File directory, String extension) {
        return null;
    }

    /**
     * Show the LoadingSavegameDialog.
     *
     * @param publicServer FIXME
     * @param singlePlayer FIXME
     * @return The {@code LoadingSavegameInfo} from the dialog.
     */
    public LoadingSavegameInfo showLoadingSavegameDialog(boolean publicServer,
                                                         boolean singlePlayer) {
        return null;
    }

    /**
     * Show the log file panel.
     */
    public void showLogFilePanel() {}

    /**
     * Show the main panel.
     *
     * @param userMsg An optional user message to display.
     */
    public void showMainPanel(String userMsg) {}

    /**
     * Complete reset back to the main panel.
     */
    public void showMainTitle() {}

    /**
     * Show the map generator options.
     *
     * @param editable If true, allow edits.
     * @return The map generator {@code OptionGroup}.
     */
    public OptionGroup showMapGeneratorOptionsDialog(boolean editable) {
        return null;
    }

    /**
     * Show the map size dialog.
     *
     * @return The selected map size as a {@code Dimension}.
     */
    public Dimension showMapSizeDialog() {
        return null;
    }

    /**
     * Show model messages.
     *
     * @param modelMessages A list of {@code ModelMessage}s to display.
     */
    public void showModelMessages(List<ModelMessage> modelMessages) {}

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
                                                 StringTemplate comment) {
        return null;
    }

    /**
     * Show the NewPanel.
     *
     * @param spec The {@code Specification} to use.
     */
    public void showNewPanel(Specification spec) {}

    /**
     * Show the parameter choice dialog.
     *
     * @return The chosen parameters.
     */
    public Parameters showParametersDialog() {
        return null;
    }

    /**
     * Show the pre-combat dialog.
     *
     * @param attacker The {@code Unit} that is attacking.
     * @param defender The {@code FreeColObject} that is defending.
     * @param tile The {@code Tile} where the attack occurs.
     * @return True if the player decided to attack.
     */
    public boolean showPreCombatDialog(Unit attacker,
                                       FreeColGameObject defender, Tile tile) {
        return false;
    }

    /**
     * Displays the purchase panel.
     */
    public void showPurchasePanel() {}

    /**
     * Displays the recruit panel.
     */
    public void showRecruitPanel() {}

    /**
     * Show the Cargo Report.
     */
    public void showReportCargoPanel() {}

    /**
     * Show the Colony Report.
     */
    public void showReportColonyPanel() {}

    /**
     * Show the Continental Congress Report.
     */
    public void showReportContinentalCongressPanel() {}

    /**
     * Show the Education Report.
     */
    public void showReportEducationPanel() {}

    /**
     * Show the Exploration Report.
     */
    public void showReportExplorationPanel() {}

    /**
     * Show the Foreign Affairs Report.
     */
    public void showReportForeignAffairPanel() {}

    /**
     * Show the History Report.
     */
    public void showReportHistoryPanel() {}

    /**
     * Show the Native Affairs Report.
     */
    public void showReportIndianPanel() {}

    /**
     * Show the Labour Report.
     */
    public void showReportLabourPanel() {}

    /**
     * Display the labour detail panel.
     *
     * @param unitType The {@code UnitType} to display.
     * @param data The labour data.
     * @param unitCount A map of unit distribution.
     * @param colonies The list of player {@code Colony}s.
     */
    public void showReportLabourDetailPanel(UnitType unitType,
        Map<UnitType, Map<Location, Integer>> data,
        TypeCountMap<UnitType> unitCount, List<Colony> colonies) {}

    /**
     * Show the Military Report.
     */
    public void showReportMilitaryPanel() {}

    /**
     * Show the Naval Report.
     */
    public void showReportNavalPanel() {}

    /**
     * Show the Production Report.
     */
    public void showReportProductionPanel() {}

    /**
     * Show the Religion Report.
     */
    public void showReportReligiousPanel() {}

    /**
     * Show the Requirements Report.
     */
    public void showReportRequirementsPanel() {}

    /**
     * Show the Trade Report.
     */
    public void showReportTradePanel() {}

    /**
     * Show the Turn Report.
     *
     * @param messages The {@code ModelMessage}s that make up the report.
     */
    public void showReportTurnPanel(List<ModelMessage> messages) {}

    /**
     * Show the river style dialog.
     *
     * @param styles The river styles a choice is made from.
     * @return The response returned by the dialog.
     */
    public String showRiverStyleDialog(List<String> styles) {
        return null;
    }

    /**
     * Show the save dialog.
     *
     * @param directory The directory containing the files.
     * @param defaultName The default game to save.
     * @return The selected file.
     */
    public File showSaveDialog(File directory, String defaultName) {
        return null;
    }

    /**
     * Show the map scale dialog.
     *
     * @return The map scale as a {@code Dimension}.
     */
    public Dimension showScaleMapSizeDialog() {
        return null;
    }

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
                                      int defaultAmount, boolean needToPay) {
        return -1;
    }

    /**
     * Show a dialog allowing the user to select a destination for
     * a given unit.
     *
     * @param unit The {@code Unit} to select a destination for.
     * @return A destination for the unit, or null.
     */
    public Location showSelectDestinationDialog(Unit unit) {
        return null;
    }

    /**
     * Show the select-tribute-amount dialog.
     *
     * @param question a {@code StringTemplate} describing the
     *     amount of tribute to demand.
     * @param maximum The maximum amount available.
     * @return The amount selected.
     */
    public int showSelectTributeAmountDialog(StringTemplate question,
                                             int maximum) {
        return -1;
    }

    /**
     * Show the {@code ServerListPanel}.
     *
     * @param serverList The list containing the servers retrieved from the
     *     metaserver.
     */
    public void showServerListPanel(List<ServerInfo> serverList) {}

    /**
     * Show the StartGamePanel.
     *
     * @param game The {@code Game} that is about to start.
     * @param player The {@code Player} using this client.
     * @param singlePlayerMode True to start a single player game.
     */
    public void showStartGamePanel(Game game, Player player,
                                   boolean singlePlayerMode) {}

    /**
     * Show the statistics panel.
     *
     * @param serverStats A map of server statistics key,value pairs.
     * @param clientStats A map of client statistics key,value pairs.
     */
    public void showStatisticsPanel(Map<String, String> serverStats,
                                    Map<String, String> clientStats) {}

    /**
     * Shows a status message which goes away when a new component is added.
     *
     * @param message The text message to display on the status panel.
     */
    public void showStatusPanel(String message) {}

    /**
     * Show the tile panel for a given tile.
     *
     * @param tile The {@code Tile} to display.
     */
    public void showTilePanel(Tile tile) {}

    /**
     * Show the tile popup for the current selected tile.
     */
    public void showTilePopup() {
        showTilePopup(getSelectedTile());
    }

    /**
     * Shows a tile popup for a given tile.
     *
     * @param tile The {@code Tile} where the popup occurred.
     */
    public void showTilePopup(Tile tile) {}

    /**
     * Shows a tile popup at a given coordinate.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    public void showTilePopup(int x, int y) {}

    /**
     * Show the trade route input panel for a given trade route.
     *
     * @param tr The {@code TradeRoute} to display.
     * @return The {@code TradeRouteInputPanel}.
     */
    public TradeRouteInputPanel showTradeRouteInputPanel(TradeRoute tr) {
        return null;
    }

    /**
     * Show a panel to select a trade route for a unit.
     *
     * @param unit An optional {@code Unit} to select a trade route for.
     */
    public void showTradeRoutePanel(Unit unit) {}

    /**
     * Show the training panel.
     */
    public void showTrainPanel() {}

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
     * @return The response returned by the dialog.
     */
    public boolean showWarehouseDialog(Colony colony) {
        return false;
    }

    /**
     * Show the production of a unit.
     *
     * @param unit The {@code Unit} to display.
     */
    public void showWorkProductionPanel(Unit unit) {}

    /**
     * Update all panels derived from the EuropePanel.
     */
    public void updateEuropeanSubpanels() {}
}
