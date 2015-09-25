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

package net.sf.freecol.client.gui;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.control.InGameController.*;
import net.sf.freecol.client.gui.panel.MiniMap;
import net.sf.freecol.client.gui.panel.Parameters;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * The API and common reusable functionality for the overall GUI.
 */
public class GUI {

    protected static final Logger logger = Logger.getLogger(GUI.class.getName());

    /** Warning levels. */
    protected static final String levels[] = {
        "low", "normal", "high"
    };

    /** View modes. */
    public static final int MOVE_UNITS_MODE = 0;
    public static final int VIEW_TERRAIN_MODE = 1;

    /** The client for the game. */
    protected final FreeColClient freeColClient;

    /** An image library to use. */
    protected final ImageLibrary imageLibrary;


    /**
     * Create the GUI.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param scaleFactor The scale factor for the GUI.
     */
    public GUI(FreeColClient freeColClient, float scaleFactor) {
        this.freeColClient = freeColClient;
        this.imageLibrary = new ImageLibrary(scaleFactor);
    }


    // Simple accessors

    protected InGameController igc() {
        return freeColClient.getInGameController();
    }

    public ImageLibrary getImageLibrary() {
        return imageLibrary;
    }

    public boolean isWindowed() {
        return true;
    }

    // Initialization related methods

    /** 
     * Swing system and look-and-feel initialization.
     * 
     * @param fontName An optional font name to be used.
     */
    public void installLookAndFeel(String fontName) throws FreeColException {
    }

    /**
     * Quit the GUI.  All that is required is to exit the full screen.
     */
    public void quit() throws Exception {
    }

    /**
     * In game initializations.
     * Called from PreGameController.startGame().
     *
     * @param tile An initial <code>Tile</code> to select.
     */
    public void initializeInGame(Tile tile) {
    }

    /**
     * Set up the mouse listeners for the canvas and map viewer.
     */
    public void setupMouseListeners() {
    }

    /**
     * Display the splash screen.
     *
     * @param splashStream A stream to find the image in.
     */
    public void displaySplashScreen(final InputStream splashStream) {
    }

    /**
     * Hide the splash screen.
     */
    public void hideSplashScreen() {
    }

    /**
     * Shows the <code>VideoPanel</code>.
     *
     * @param userMsg An optional user message.
     */
    public void showOpeningVideo(final String userMsg) {
    }

    /**
     * Starts the GUI by creating and displaying the GUI-objects.
     *
     * @param desiredWindowSize The desired size of the GUI window.
     */
    public void startGUI(final Dimension desiredWindowSize) {
        logger.info("It seems that the GraphicsEnvironment is headless!");
    }

    /**
     * Change the windowed mode.
     */
    public void changeWindowedMode() {
    }

    /**
     * Start the GUI for the map editor.
     */
    public void startMapEditorGUI() {
    }


    // Non-trivial public routines.

    /**
     * Start/stop the goto path display.
     */
    public void activateGotoPath() {
    }

    /**
     * Stop the goto path display.
     */
    public void clearGotoPath() {
    }

    /**
     * Create a thumbnail for the minimap.
     * 
     * FIXME: Delete all code inside this method and replace it with
     *        sensible code directly drawing in necessary size,
     *        without creating a throwaway GUI panel, drawing in wrong
     *        size and immediately resizing.
     * @return The created image.
     */
    public BufferedImage createMiniMapThumbNail() {
        MiniMap miniMap = new MiniMap(freeColClient);
        miniMap.setTileSize(MiniMap.MAX_TILE_SIZE);
        Game game = freeColClient.getGame();
        int width = game.getMap().getWidth() * MiniMap.MAX_TILE_SIZE
            + MiniMap.MAX_TILE_SIZE / 2;
        int height = game.getMap().getHeight() * MiniMap.MAX_TILE_SIZE / 4;
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

    /**
     * Tells the map controls that a chat message was received.
     *
     * @param player The player who sent the chat message.
     * @param message The chat message.
     * @param privateChat 'true' if the message is a private one, 'false'
     *            otherwise.
     * @see GUIMessage
     */
    public void displayChatMessage(Player player, String message,
                                   boolean privateChat) {
    }

    /**
     * Refresh the GUI.
     */
    public void refresh() {
    }

    /**
     * Reset the menu bar.
     */
    public void resetMenuBar() {
    }

    protected void resetMapZoom() {
        ResourceManager.clean();
    }

    public boolean canZoomInMap() {
        return false;
    }

    public boolean canZoomOutMap() {
        return false;
    }

    public void zoomInMap() {
        ResourceManager.clean();
    }

    public void zoomOutMap() {
        ResourceManager.clean();
    }

    /**
     * Set the active unit.
     *
     * @param unit The <code>Unit</code> to activate.
     * @return true if the focus was set.
     */
    public boolean setActiveUnit(Unit unit) {
        return false;
    }

    /**
     * Update the menu bar.
     */
    public void updateMenuBar() {
    }


    // Animation handling

    /**
     * Require the given tile to be in the onScreen()-area.
     *
     * @param tile The <code>Tile</code> to check.
     * @return True if the focus was set.
     */
    public boolean requireFocus(Tile tile) {
        return false;
    }
    
    /**
     * Animate a unit attack.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @param attackerTile The <code>Tile</code> to show the attacker on.
     * @param defenderTile The <code>Tile</code> to show the defender on.
     * @param success Did the attack succeed?
     */
    public void animateUnitAttack(Unit attacker, Unit defender,
                                  Tile attackerTile, Tile defenderTile,
                                  boolean success) {
    }

    /**
     * Animate a unit move.
     *
     * @param unit The <code>Unit</code> that is moving.
     * @param srcTile The <code>Tile</code> the unit starts at.
     * @param dstTile The <code>Tile</code> the unit moves to.
     */
    public void animateUnitMove(Unit unit, Tile srcTile, Tile dstTile) {
    }


    // MapControls handling

    /**
     * Enable the map controls.
     *
     * Called from the MapControlsAction.
     *
     * @param enable If true then enable.
     */
    public void enableMapControls(boolean enable) {
    }

    public void updateMapControls() {
    }

    public void zoomInMapControls() {
    }

    public void zoomOutMapControls() {
    }

    public boolean canZoomInMapControls() {
        return false;
    }

    public boolean canZoomOutMapControls() {
        return false;
    }

    public void miniMapToggleViewControls() {
    }

    public void miniMapToggleFogOfWarControls() {
    }


    // Dialogs that return values

    /**
     * Simple modal confirmation dialog.
     *
     * @param textKey A string to use as the message key.
     * @param okKey A key for the "ok" button.
     * @param cancelKey A key for the "cancel" button.
     * @return True if the "ok" button was selected.
     */
    public boolean confirm(String textKey, String okKey, String cancelKey) {
        return false;
    }

    /**
     * General modal confirmation dialog.
     *
     * @param tile An optional <code>Tile</code> to expose.
     * @param template The <code>StringTemplate</code> explaining the choice.
     * @param okKey A key for the "ok" button.
     * @param cancelKey A key for the "cancel" button.
     * @return True if the "ok" button was selected.
     */
    public boolean confirm(Tile tile, StringTemplate template,
                           String okKey, String cancelKey) {
        return false;
    }

    /**
     * General modal confirmation dialog.
     *
     * @param tile An optional <code>Tile</code> to expose.
     * @param template The <code>StringTemplate</code> explaining the choice.
     * @param unit An optional unit to make an icon for the dialog from.
     * @param okKey A key for the "ok" button.
     * @param cancelKey A key for the "cancel" button.
     * @return True if the "ok" button was selected.
     */
    public boolean confirm(Tile tile, StringTemplate template, Unit unit,
                           String okKey, String cancelKey) {
        return false;
    }

    public boolean confirm(Tile tile, StringTemplate template,
                           Settlement settlement,
                           String okKey, String cancelKey) {
        return false;
    }

    public boolean confirm(Tile tile, StringTemplate template,
                           GoodsType goodsType,
                           String okKey, String cancelKey) {
        return false;
    }

    /**
     * Confirm that a unit should abandon its educational activity.
     *
     * @param unit The <code>Unit</code> to check.
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
     * @param unit The <code>Unit</code> to check.
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
     * Confirm declaration of independence.
     *
     * @return A list of new nation and country names.
     */
    public List<String> confirmDeclaration() {
        return Collections.<String>emptyList();
    }

    /**
     * Confirm whether the player wants to demand tribute from a colony.
     *
     * @param attacker The potential attacking <code>Unit</code>.
     * @param colony The target <code>Colony</code>.
     * @param ns A <code>NationSummary</code> of the other nation.
     * @return The amount of tribute to demand, positive if the demand
     *     should proceed.
     */
    public int confirmEuropeanTribute(Unit attacker, Colony colony,
                                      NationSummary ns) {
        Player player = attacker.getOwner();
        Player other = colony.getOwner();
        int strength = player.calculateStrength(false);
        int otherStrength = ns.getMilitaryStrength();
        int mil = (otherStrength <= 1 || otherStrength * 5 < strength) ? 0
            : (strength == 0 || strength * 5 < otherStrength) ? 2
            : 1;

        StringTemplate t;
        int gold = ns.getGold();
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
     * @param attacker The potential attacking <code>Unit</code>.
     * @param target The target <code>Tile</code>.
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
     * @param unit The <code>Unit</code> that is leaving the colony.
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
     * @param attacker The potential attacking <code>Unit</code>.
     * @param is The target <code>IndianSettlement</code>.
     * @return The amount of tribute to demand, positive if the demand
     *     should proceed.
     */
    public int confirmNativeTribute(Unit attacker, IndianSettlement is) {
        Player player = attacker.getOwner();
        Player other = is.getOwner();
        int strength = player.calculateStrength(false);
        String messageId = (other.getNumberOfSettlements() >= strength)
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
     * @param attacker The attacking <code>Unit</code>.
     * @param tile The target <code>Tile</code>.
     * @return True to attack, false to abort.
     */
    public boolean confirmPreCombat(Unit attacker, Tile tile) {
        if (freeColClient.getClientOptions()
            .getBoolean(ClientOptions.SHOW_PRECOMBAT)) {
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
     * @param settlement The <code>Settlement</code> to consider.
     * @return The chosen action, tribute, attack or cancel.
     */
    public ArmedUnitSettlementAction getArmedUnitSettlementChoice(Settlement settlement) {
        final Player player = freeColClient.getMyPlayer();

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
     * @param goods The <code>Goods</code> to possibly dump.
     * @param europe The player <code>Europe</code> where the boycott
     *     is in force.
     * @return The chosen <code>BoycottAction</code>.
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
                BoycottAction.PAY_ARREARS));
        choices.add(new ChoiceItem<>(Messages.message("boycottedGoods.dumpGoods"),
                BoycottAction.DUMP_CARGO));

        return getChoice(null, template,
                         goods.getType(), "cancel", choices);
    }

    /**
     * Gets the user choice when negotiating a purchase from a settlement.
     *
     * @param unit The <code>Unit</code> that is buying.
     * @param settlement The <code>Settlement</code> to buy from.
     * @param goods The <code>Goods</code> to buy.
     * @param gold The current negotiated price.
     * @param canBuy True if buy is a valid option.
     * @return The chosen action, buy, haggle, or cancel.
     */
    public BuyAction getBuyChoice(Unit unit, Settlement settlement,
                                  Goods goods, int gold, boolean canBuy) {
        StringTemplate template = StringTemplate.template("buy.text")
            .addStringTemplate("%nation%", settlement.getOwner().getNationLabel())
            .addStringTemplate("%goods%", goods.getLabel(true))
            .addAmount("%gold%", gold);

        List<ChoiceItem<BuyAction>> choices = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("buy.takeOffer"),
                                     BuyAction.BUY, canBuy));
        choices.add(new ChoiceItem<>(Messages.message("buy.moreGold"),
                                     BuyAction.HAGGLE));

        return getChoice(unit.getTile(), template,
                         goods.getType(), "cancel", choices);
    }

    /**
     * Gets the user choice for claiming a tile.
     *
     * @param tile The <code>Tile</code> to claim.
     * @param player The <code>Player</code> that is claiming.
     * @param price An asking price, if any.
     * @param owner The <code>Player</code> that owns the land.
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
                                         ClaimAction.ACCEPT,
                                         player.checkGold(price)));
        } else {
            template = StringTemplate.template("indianLand.unknown");
        }

        choices.add(new ChoiceItem<>(Messages.message("indianLand.take"),
                                     ClaimAction.STEAL));

        return getChoice(tile, template,
                         owner.getNation(), "indianLand.cancel", choices);
    }

    /**
     * Get the user choice when trading with a native settlement.
     *
     * @param settlement The native settlement to trade with.
     * @param template A <code>StringTemplate</code> containing the message
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
     * @param unit The <code>Unit</code> speaking to the settlement.
     * @param settlement The <code>IndianSettlement</code> being visited.
     * @param canEstablish Is establish a valid option.
     * @param canDenounce Is denounce a valid option.
     * @return The chosen action, establish mission, denounce, incite
     *     or cancel.
     */
    public MissionaryAction getMissionaryChoice(Unit unit,
                                                IndianSettlement settlement,
                                                boolean canEstablish,
                                                boolean canDenounce) {
        StringTemplate template = StringTemplate.label("\n\n")
            .addStringTemplate(settlement.getAlarmLevelLabel(unit.getOwner()))
            .addStringTemplate(StringTemplate
                .template("missionarySettlement.question")
                .addName("%settlement%", settlement.getName()));

        List<ChoiceItem<MissionaryAction>> choices = new ArrayList<>();
        if (canEstablish) {
            choices.add(new ChoiceItem<>(Messages.message("missionarySettlement.establish"),
                                         MissionaryAction.ESTABLISH_MISSION,
                                         canEstablish));
        }
        if (canDenounce) {
            choices.add(new ChoiceItem<>(Messages.message("missionarySettlement.heresy"),
                                         MissionaryAction.DENOUNCE_HERESY,
                                         canDenounce));
        }
        choices.add(new ChoiceItem<>(Messages.message("missionarySettlement.incite"),
                                     MissionaryAction.INCITE_INDIANS));

        return getChoice(unit.getTile(), template,
                         settlement, "cancel", choices);
    }

    /**
     * Get a name for a new colony for a player.
     *
     * @param player The <code>Player</code> to get the colony name for.
     * @param tile The <code>Tile</code> for the new colony.
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
     * @param colony The <code>Colony</code> to be scouted.
     * @param unit The <code>Unit</code> that is scouting.
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
                                     ScoutColonyAction.FOREIGN_COLONY_NEGOTIATE,
                                     neg));
        choices.add(new ChoiceItem<>(Messages.message("scoutColony.spy"),
                                     ScoutColonyAction.FOREIGN_COLONY_SPY));
        choices.add(new ChoiceItem<>(Messages.message("scoutColony.attack"),
                                     ScoutColonyAction.FOREIGN_COLONY_ATTACK));

        return getChoice(unit.getTile(), template,
                         colony, "cancel", choices);
    }

    /**
     * Get the user choice for what to do at a native settlement.
     *
     * @param settlement The <code>IndianSettlement</code> to be scouted.
     * @param numberString The number of settlements in the settlement
     *     owner nation.
     * @return The chosen action, speak, tribute, attack or cancel.
     */
    public ScoutIndianSettlementAction getScoutIndianSettlementChoice(IndianSettlement settlement,
        String numberString) {
        final Player player = freeColClient.getMyPlayer();
        final Player owner = settlement.getOwner();

        StringTemplate template = StringTemplate.label("")
            .addStringTemplate(settlement.getAlarmLevelLabel(player))
            .addName("\n\n")
            .addStringTemplate(StringTemplate
                .template("scoutSettlement.greetings")
                .addStringTemplate("%nation%", owner.getNationLabel())
                .addName("%settlement%", settlement.getName())
                .addName("%number%", numberString)
                .add("%settlementType%",
                    ((IndianNationType)owner.getNationType()).getSettlementTypeKey(true)))
            .addName(" ");
        if (settlement.getLearnableSkill() != null) {
            template
                .addStringTemplate(StringTemplate
                    .template("scoutSettlement.skill")
                    .addNamed("%skill%", settlement.getLearnableSkill()))
                .addName(" ");
        }
        GoodsType[] wantedGoods = settlement.getWantedGoods();
        int present = 0;
        for (; present < wantedGoods.length; present++) {
            if (wantedGoods[present] == null) break;
        }
        if (present > 0) {
            StringTemplate t = StringTemplate.template("scoutSettlement.trade."
                + Integer.toString(present));
            for (int i = 0; i < present; i++) {
                String tradeKey = "%goods" + Integer.toString(i+1) + "%";
                t.addNamed(tradeKey, wantedGoods[i]);
            }
            template.addStringTemplate(t).addName("\n\n");
        }

        List<ChoiceItem<ScoutIndianSettlementAction>> choices
            = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("scoutSettlement.speak"),
                                     ScoutIndianSettlementAction.INDIAN_SETTLEMENT_SPEAK));
        choices.add(new ChoiceItem<>(Messages.message("scoutSettlement.tribute"),
                                     ScoutIndianSettlementAction.INDIAN_SETTLEMENT_TRIBUTE));
        choices.add(new ChoiceItem<>(Messages.message("scoutSettlement.attack"),
                                     ScoutIndianSettlementAction.INDIAN_SETTLEMENT_ATTACK));

        return getChoice(settlement.getTile(), template,
                         settlement, "cancel", choices);
    }

    /**
     * Get the user choice for negotiating a sale to a settlement.
     *
     * @param unit The <code>Unit</code> that is selling.
     * @param settlement The <code>Settlement</code> to sell to.
     * @param goods The <code>Goods</code> to sell.
     * @param gold The current negotiated price.
     * @return The chosen action, sell, gift or haggle, or null.
     */
    public SellAction getSellChoice(Unit unit, Settlement settlement,
                                    Goods goods, int gold) {
        StringTemplate goodsTemplate = goods.getLabel(true);
        StringTemplate template = StringTemplate.template("sell.text")
            .addStringTemplate("%nation%", settlement.getOwner().getNationLabel())
            .addStringTemplate("%goods%", goodsTemplate)
            .addAmount("%gold%", gold);

        List<ChoiceItem<SellAction>> choices = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("sell.takeOffer"),
                                     SellAction.SELL));
        choices.add(new ChoiceItem<>(Messages.message("sell.moreGold"),
                                     SellAction.HAGGLE));
        choices.add(new ChoiceItem<>(Messages.message(StringTemplate
                    .template("sell.gift")
                    .addStringTemplate("%goods%", goodsTemplate)),
                SellAction.GIFT));

        return getChoice(unit.getTile(), template,
                         goods.getType(), "cancel", choices);
    }


    /**
     * General modal choice dialog.
     *
     * @param tile An optional <code>Tile</code> to expose.
     * @param explain An object explaining the choice.
     * @param cancelKey A key for the "cancel" button.
     * @param choices A list a <code>ChoiceItem</code>s to choose from.
     * @return The selected value of the selected <code>ChoiceItem</code>,
     *     or null if cancelled.
     */
    public <T> T getChoice(Tile tile, Object explain,
                           String cancelKey, List<ChoiceItem<T>> choices) {
        return null;
    }

    public <T> T getChoice(Tile tile, Object explain, Unit unit,
                           String cancelKey, List<ChoiceItem<T>> choices) {
        return null;
    }

    public <T> T getChoice(Tile tile, Object explain, Settlement settlement,
                           String cancelKey, List<ChoiceItem<T>> choices) {
        return null;
    }

    public <T> T getChoice(Tile tile, Object explain, GoodsType goodsType,
                           String cancelKey, List<ChoiceItem<T>> choices) {
        return null;
    }

    public <T> T getChoice(Tile tile, Object explain, Nation nation,
                           String cancelKey, List<ChoiceItem<T>> choices) {
        return null;
    }

    /**
     * General modal string input dialog.
     *
     * @param tile An optional <code>Tile</code> to expose.
     * @param template A <code>StringTemplate</code> explaining the choice.
     * @param defaultValue The default value to show initially.
     * @param okKey A key for the "ok" button.
     * @param cancelKey A key for the "cancel" button.
     * @return The chosen value.
     */
    public String getInput(Tile tile, StringTemplate template,
                           String defaultValue,
                           String okKey, String cancelKey) {
        return null;
    }

    public void closeMainPanel() {
    }

    public void closeMenus() {
    }

    public void closeStatusPanel() {
    }

    public boolean containsInGameComponents() {
        return false;
    }

    public LoadingSavegameInfo getLoadingSavegameInfo() {
        return null;
    }

    public boolean isClientOptionsDialogShowing() {
        return false;
    }

    public boolean isMapboardActionsEnabled() {
        return false;
    }

    public boolean isShowingSubPanel() {
        return false;
    }

    public void paintImmediatelyCanvasIn(Rectangle rectangle) {
    }

    public void paintImmediatelyCanvasInItsBounds() {
    }

    public void refreshPlayersTable() {
    }

    public void removeInGameComponents() {
    }

    public void requestFocusForSubPanel() {
    }

    public boolean requestFocusInWindow() {
        return false;
    }

    public void returnToTitle() {
    }

    public void showAboutPanel() {
    }

    public void showCaptureGoodsDialog(final Unit unit, List<Goods> gl,
                                       DialogHandler<List<Goods>> handler) {
    }

    public void showChatPanel() {
    }

    public void showChooseFoundingFatherDialog(final List<FoundingFather> ffs,
            DialogHandler<FoundingFather> handler) {
    }

    public void showClientOptionsDialog() {
    }

    /**
     * Display the appropriate panel for a given settlement.
     *
     * @param settlement The <code>Settlement</code> to display.
     */
    void showSettlement(Settlement settlement) {
        if (settlement instanceof Colony) {
            if (settlement.getOwner().equals(freeColClient.getMyPlayer())) {
                showColonyPanel((Colony)settlement, null);
            } else if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
                showForeignColony(settlement);
            }
        } else if (settlement instanceof IndianSettlement) {
            showIndianSettlementPanel((IndianSettlement)settlement);
        } else {
            throw new IllegalStateException("Bogus settlement");
        }
    }

    protected void showForeignColony(Settlement settlement) {
    }

    public void showColonyPanel(Colony colony, Unit unit) {
    }

    public void showColopediaPanel(String nodeId) {
    }

    public void showCompactLabourReport() {
    }

    public void showDeclarationPanel() {
    }

    public OptionGroup showDifficultyDialog() {
        return null;
    }

    public void showDumpCargoDialog(Unit unit,
                                    DialogHandler<List<Goods>> handler) {
    }

    public boolean showEditOptionDialog(Option option) {
        return false;
    }

    public void showEmigrationDialog(final Player player,
                                     final boolean fountainOfYouth,
                                     DialogHandler<Integer> handler) {
    }

    public void showEndTurnDialog(final List<Unit> units,
                                  DialogHandler<Boolean> handler) {
    }

    public void showErrorMessage(StringTemplate template) {
    }

    public void showErrorMessage(String messageId) {
    }

    public void showErrorMessage(String messageID, String message) {
    }

    public void showEuropePanel() {
    }

    public void showEventPanel(String header, String image, String footer) {
    }

    public void showFindSettlementPanel() {
    }

    public OptionGroup showGameOptionsDialog(boolean editable, boolean custom) {
        return null;
    }

    public void showHighScoresPanel(String messageId, List<HighScore> scores) {
    }

    public void showIndianSettlementPanel(IndianSettlement indianSettlement) {
    }

    public void showInformationMessage(String messageId) {
        alertSound();
    }

    public void showInformationMessage(StringTemplate template) {
        alertSound();
    }

    final public void showInformationMessage(Settlement displayObject,
                                       String messageId) {
        showInformationMessage(displayObject, StringTemplate.key(messageId));
    }

    public void showInformationMessage(Settlement displayObject,
                                       StringTemplate template) {
        alertSound();
    }

    public void showInformationMessage(Unit displayObject,
                                       StringTemplate template) {
        alertSound();
    }

    final public void showInformationMessage(Tile displayObject,
                                       String messageId) {
        showInformationMessage(displayObject, StringTemplate.key(messageId));
    }

    public void showInformationMessage(Tile displayObject,
                                       StringTemplate template) {
        alertSound();
    }

    public void showInformationMessage(FreeColObject displayObject,
                                       String messageId) {
        alertSound();
    }

    public void showInformationMessage(FreeColObject displayObject,
                                       StringTemplate template) {
        alertSound();
    }

    public File showLoadDialog(File directory) {
        return null;
    }

    final public File showLoadSaveFileDialog() {
        File file = showLoadDialog(FreeColDirectories.getSaveDirectory());
        if (file != null && !file.isFile()) {
            showErrorMessage("error.noSuchFile");
            file = null;
        }
        return file;
    }

    public boolean showLoadingSavegameDialog(boolean publicServer,
                                             boolean singlePlayer) {
        return false;
    }

    public void showLogFilePanel() {
    }

    public void showMainPanel(String userMsg) {
    }

    public OptionGroup showMapGeneratorOptionsDialog(boolean editable) {
        return null;
    }

    public Dimension showMapSizeDialog() {
        return null;
    }

    public void showModelMessages(List<ModelMessage> modelMessages) {
    }

    public void showMonarchDialog(final MonarchAction action,
                                  StringTemplate template, String monarchKey,
                                  DialogHandler<Boolean> handler) {
    }

    public void showNamingDialog(StringTemplate template,
                                 final String defaultName,
                                 final Unit unit,
                                 DialogHandler<String> handler) {
    }

    public void showFirstContactDialog(final Player player, final Player other,
                                       final Tile tile, int settlementCount,
                                       DialogHandler<Boolean> handler) {
    }

    public DiplomaticTrade showNegotiationDialog(FreeColGameObject our,
                                                     FreeColGameObject other,
                                                     DiplomaticTrade agreement,
                                                     StringTemplate comment) {
        return null;
    }

    public void showNewPanel() {
    }

    public void showNewPanel(Specification specification) {
    }

    public void showSpyColonyPanel(final Tile tile, Runnable callback) {
    }

    public Parameters showParametersDialog() {
        return null;
    }

    public boolean showPreCombatDialog(Unit attacker,
                                       FreeColGameObject defender, Tile tile) {
        return false;
    }

    public void showReportCargoPanel() {
    }

    public void showReportColonyPanel() {
    }

    public void showReportContinentalCongressPanel() {
    }

    public void showReportEducationPanel() {
    }

    public void showReportExplorationPanel() {
    }

    public void showReportForeignAffairPanel() {
    }

    public void showReportHistoryPanel() {
    }

    public void showReportIndianPanel() {
    }

    public void showReportLabourPanel() {
    }

    public void showReportMilitaryPanel() {
    }

    public void showReportNavalPanel() {
    }

    public void showReportProductionPanel() {
    }

    public void showReportReligiousPanel() {
    }

    public void showReportRequirementsPanel() {
    }

    public void showReportTradePanel() {
    }

    public void showReportTurnPanel(List<ModelMessage> messages) {
    }

    public File showSaveDialog(File directory, String defaultName) {
        return null;
    }

    public Dimension showScaleMapSizeDialog() {
        return null;
    }

    public int showSelectAmountDialog(GoodsType goodsType, int available,
                                      int defaultAmount, boolean needToPay) {
        return -1;
    }

    public int showSelectTributeAmountDialog(StringTemplate question,
                                             int maximum) {
        return -1;
    }

    public Location showSelectDestinationDialog(Unit unit) {
        return null;
    }

    public void showStartGamePanel(Game game, Player player,
                                   boolean singlePlayerMode) {
    }

    public void showStatisticsPanel() {
    }

    public void showStatusPanel(String message) {
    }

    public void showTilePopUpAtSelectedTile() {
    }

    public void showTradeRoutePanel(Unit unit) {
    }

    public void showVictoryDialog(DialogHandler<Boolean> handler) {
    }

    public void updateGameOptions() {
    }

    public void updateMapGeneratorOptions() {
    }

    public void centerActiveUnit() {
    }

    public void changeViewMode(int newViewMode) {
    }

    public Unit getActiveUnit() {
        return null;
    }

    public Tile getFocus() {
        return null;
    }

    public Tile getSelectedTile() {
        return null;
    }

    public int getViewMode() {
        return -1;
    }

    public void setFocus(Tile tileToFocus) {
    }

    public boolean setSelectedTile(Tile newTileToSelect) {
        return true; // Pretending again.
    }

    public void toggleViewMode() {
    }


    // Forwarding to SoundController, only for gui classes in need of sound

    /**
     * Play a sound.
     *
     * @param sound The sound resource to play, or if null stop playing.
     */
    public void playSound(String sound) {
        freeColClient.getSoundController().playSound(sound);
    }

    /**
     * Plays an alert sound for an information message if the
     * option for it is turned on.
     */
    private void alertSound() {
        if (freeColClient.getClientOptions()
            .getBoolean(ClientOptions.AUDIO_ALERTS)) {
            freeColClient.getSoundController()
                .playSound("sound.event.alertSound");
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
        return freeColClient.getSoundController().getSoundMixerLabelText();
    }

    // invoke method forwarding

    /**
     * Wrapper for SwingUtilities.invokeLater that handles the case
     * where we are already in the EDT.
     *
     * @param runnable A <code>Runnable</code> to run.
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
     * @param runnable A <code>Runnable</code> to run.
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

}
