/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

package net.sf.freecol.client.control;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.option.FreeColActionUI;
import net.sf.freecol.common.debug.DebugUtils;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.i18n.NameCache;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.ColonyWas;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeContext;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Europe.MigrationType;
import net.sf.freecol.common.model.EuropeWas;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.GoldTradeItem;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.MarketWas;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.ModelMessage.MessageType;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.NoClaimReason;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StanceTradeItem;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TradeLocation;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TradeRouteStop;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.UnitWas;
import net.sf.freecol.common.model.WorkLocation;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.networking.ServerAPI;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.server.FreeColServer;


/**
 * The controller that will be used while the game is played.
 */
public final class InGameController extends FreeColClientHolder {

    private static final Logger logger = Logger.getLogger(InGameController.class.getName());

    /**
     * Selecting next unit depends on mode--- either from the active list,
     * from the going-to list, or flush going-to and end the turn.
     */
    private static enum MoveMode {
        NEXT_ACTIVE_UNIT,
        EXECUTE_GOTO_ORDERS,
        END_TURN;

        public MoveMode minimize(MoveMode m) {
            return (this.ordinal() > m.ordinal()) ? m : this;
        }

        public MoveMode maximize(MoveMode m) {
            return (this.ordinal() < m.ordinal()) ? m : this;
        }
    }

    private static final short UNIT_LAST_MOVE_DELAY = 300;

    /** A template to use as a magic cookie for aborted trades. */
    private static final StringTemplate abortTrade
        = StringTemplate.template("");

    /** A comparator for ordering trade route units. */
    private static final Comparator<Unit> tradeRouteUnitComparator
        = Comparator.comparing((Unit u) -> u.getTradeRoute().getName())
            .thenComparing(u -> (FreeColObject)u);

    /** Current mode for moving units. */
    private MoveMode moveMode = MoveMode.NEXT_ACTIVE_UNIT;

    /** A map of messages to be ignored. */
    private final HashMap<String, Integer> messagesToIgnore = new HashMap<>();

    /** The messages in the last turn report. */
    private final List<ModelMessage> turnReportMessages = new ArrayList<>();


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public InGameController(FreeColClient freeColClient) {
        super(freeColClient);

        // FIXME: fetch value of lastSaveGameFile from a persistent
        // client value
        //   lastSaveGameFile = new File(getClientOptions().getString(null));
    }


    // Simple utilities

    /**
     * Play a sound.
     *
     * @param soundKey The sound resource key.
     */
    private void sound(String soundKey) {
        getSoundController().playSound(soundKey);
    }
    
    /**
     * Require that it is this client's player's turn.
     * Put up the notYourTurn message if not.
     *
     * @return True if it is our turn.
     */
    private boolean requireOurTurn() {
        if (currentPlayerIsMyPlayer()) return true;
        if (getFreeColClient().isInGame()) {
            getGUI().showInformationMessage("info.notYourTurn");
        }
        return false;
    }

    /**
     * Display the colony panel for a colony, and select the unit that just
     * arrived there if it is a carrier.
     *
     * @param colony The <code>Colony</code> to display.
     * @param unit An optional <code>Unit</code> to select.
     */
    private void colonyPanel(Colony colony, Unit unit) {
        getGUI().showColonyPanel(colony, (unit.isCarrier()) ? unit : null);
    }

    /**
     * Convenience function to find an adjacent settlement.  Intended
     * to be called in contexts where we are expecting a settlement to
     * be there, such as when handling a particular move type.
     *
     * @param tile The <code>Tile</code> to start at.
     * @param direction The <code>Direction</code> to step.
     * @return A settlement on the adjacent tile if any.
     */
    private Settlement getSettlementAt(Tile tile, Direction direction) {
        return tile.getNeighbourOrNull(direction).getSettlement();
    }

    /**
     * Convenience function to find the nation controlling an adjacent
     * settlement.  Intended to be called in contexts where we are
     * expecting a settlement or unit to be there, such as when
     * handling a particular move type.
     *
     * @param tile The <code>Tile</code> to start at.
     * @param direction The <code>Direction</code> to step.
     * @return The name of the nation controlling a settlement on the
     *         adjacent tile if any.
     */
    private StringTemplate getNationAt(Tile tile, Direction direction) {
        Tile newTile = tile.getNeighbourOrNull(direction);
        Player player = null;
        if (newTile.hasSettlement()) {
            player = newTile.getSettlement().getOwner();
        } else if (newTile.getFirstUnit() != null) {
            player = newTile.getFirstUnit().getOwner();
        } else { // should not happen
            player = getGame().getUnknownEnemy();
        }
        return player.getNationLabel();
    }

    /**
     * Update the GUI and the active unit with a fallback tile.
     *
     * @param tile An optional fallback <code>Tile</code> to display if
     *     no active unit is found, useful when the last unit might have
     *     died.
     */
    private void updateGUI(Tile tile) {
        if (displayModelMessages(false, false)) {
            ; // If messages are displayed they probably refer to the
              // current unit, so do not update it.
        } else if (updateActiveUnit(tile)) {
            ; // setActiveUnit will update the menu bar
        } else {
            getGUI().updateMapControls();
            getGUI().updateMenuBar();
        }
    }


    // Server access routines called from multiple places.

    /**
     * Ask the server to assign a trade route.
     *
     * @param unit The <code>Unit</code> to assign to.
     * @param tradeRoute The <code>TradeRoute</code> to assign.
     * @return True if the assignment succeeds.
     */
    private boolean askAssignTradeRoute(Unit unit, TradeRoute tradeRoute) {
        if (tradeRoute == unit.getTradeRoute()) return true;

        if (tradeRoute != null && unit.getTradeRoute() != null) {
            if (!getGUI().confirmClearTradeRoute(unit)) return false;
        }

        return askServer().assignTradeRoute(unit, tradeRoute)
            && unit.getTradeRoute() == tradeRoute;
    }

    /**
     * Claim a tile.
     *
     * @param player The <code>Player</code> that is claiming.
     * @param tile The <code>Tile</code> to claim.
     * @param claimant The <code>Unit</code> or <code>Colony</code> claiming.
     * @param price The price required.
     * @return True if the claim succeeded.
     */
    private boolean askClaimTile(Player player, Tile tile,
                                 FreeColGameObject claimant, int price) {
        final Player owner = tile.getOwner();
        if (price < 0) { // not for sale
            return false;
        } else if (price > 0) { // for sale
            ClaimAction act
                = getGUI().getClaimChoice(tile, player, price, owner);
            if (act == null) return false; // Cancelled
            switch (act) {
            case CLAIM_ACCEPT: // accepted price
                break;
            case CLAIM_STEAL:
                price = STEAL_LAND;
                break;
            default:
                logger.warning("Claim dialog fail: " + act);
                return false;
            }
        } // else price == 0 and we can just proceed to claim

        // Ask the server
        return askServer().claimTile(tile, claimant, price)
            && player.owns(tile);
    }

    /**
     * Clears the goto orders of the given unit by setting its destination
     * to null.
     *
     * @param unit The <code>Unit</code> to clear the destination for.
     * @return True if the unit now has no destination or trade route.
     */
    private boolean askClearGotoOrders(Unit unit) {
        if (!askAssignTradeRoute(unit, null)) return false;

        if (unit.getDestination() == null) return true;

        if (askSetDestination(unit, null)) {
            getGUI().clearGotoPath();
            return true;
        }
        return false;
    }

    /**
     * Embark onto a carrier.
     *
     * @param unit The <code>Unit</code> to embark.
     * @param carrier The carrier <code>Unit</code> to board.
     * @return True if boarding succeeded.
     */
    private boolean askEmbark(Unit unit, Unit carrier) {
        ColonyWas colonyWas = (unit.getColony() != null)
            ? new ColonyWas(unit.getColony()) : null;
        EuropeWas europeWas = (unit.isInEurope())
            ? new EuropeWas(unit.getOwner().getEurope()) : null;
        UnitWas unitWas = new UnitWas(unit);
        if (askServer().embark(unit, carrier, null)
            && unit.getLocation() == carrier) {
            sound("sound.event.loadCargo");
            unitWas.fireChanges();
            if (colonyWas != null) colonyWas.fireChanges();
            if (europeWas != null) europeWas.fireChanges();
            return true;
        }
        return false;
    }
    
    /**
     * A unit in Europe emigrates.
     *
     * This is unusual for an ask* routine in that it uses a *Was
     * structure, but it is needed to extract the unit.
     *
     * @param europe The <code>Europe</code> where the unit appears.
     * @param slot The slot to choose, [0..RECRUIT_COUNT].
     * @return The new <code>Unit</code> or null on failure.
     */
    private Unit askEmigrate(Europe europe, int slot) {
        if (europe == null
            || !MigrationType.validMigrantSlot(slot)) return null;

        EuropeWas europeWas = new EuropeWas(europe);
        Unit newUnit = null;
        if (askServer().emigrate(getGame(), slot)
            && (newUnit = europeWas.getNewUnit()) != null) {
            europeWas.fireChanges();
        }
        return newUnit;
    }

    /**
     * Select all the units to emigrate from Europe.  If they are all
     * the same they can be picked automatically, but otherwise use
     * the emigration dialog.  Only to be called if the player is
     * allowed to select the unit type (i.e. FoY or has Brewster).
     *
     * The server contains the count of available FoY-units, and
     * maintains the immigration/immigrationRequired amounts, so this
     * routine will fail harmlessly if it asks for too much.
     *
     * @param player The <code>Player</code> that owns the unit.
     * @param n The number of units known to be eligible to emigrate.
     * @param fountainOfYouth True if this migration if due to a FoY.
     */
    private void emigration(Player player, int n, boolean fountainOfYouth) {
        final Europe europe = player.getEurope();
        if (europe == null) return;

        for (; n > 0 || player.checkEmigrate() ; n--) {
            if (!allSame(europe.getRecruitables())) {
                final int nf = n;
                getGUI().showEmigrationDialog(player, fountainOfYouth,
                    (Integer value) -> { // Value is a valid slot
                        emigrate(player,
                            Europe.MigrationType.convertToMigrantSlot(value),
                            nf-1, fountainOfYouth);
                    });
                return;
            }
            Unit u = askEmigrate(europe, Europe.MigrationType.getDefaultSlot());
            if (u == null) break; // Give up on failure, try again next turn
            player.addModelMessage(player.getEmigrationMessage(u));
        }
    }
   
    /**
     * Load some goods onto a carrier.
     *
     * @param loc The <code>Location</code> to load from.
     * @param type The <code>GoodsType</code> to load.
     * @param amount The amount of goods to load.
     * @param carrier The <code>Unit</code> to load onto.
     * @return True if the load succeeded.
     */
    private boolean askLoadGoods(Location loc, GoodsType type, int amount,
                                 Unit carrier) {
        TradeLocation trl = carrier.getTradeLocation();
        if (trl == null) return false;

        // Size check, if there are spare holds they can be filled, but...
        int loadable = carrier.getLoadableAmount(type);
        if (amount > loadable) amount = loadable;

        final Player player = carrier.getOwner();
        final Market market = player.getMarket();
        MarketWas marketWas = (market != null) ? new MarketWas(player) : null;

        if (carrier.isInEurope()) {
            // Are the goods boycotted?
            if (!player.canTrade(type)) return false;

            // Check that the purchase is funded.
            if (!player.checkGold(market.getBidPrice(type, amount))) {
                getGUI().showInformationMessage("info.notEnoughGold");
                return false;
            }
        }

        // Try to purchase.
        int oldAmount = carrier.getGoodsContainer().getGoodsCount(type);
        if (askServer().loadGoods(loc, type, amount, carrier)
            && carrier.getGoodsContainer().getGoodsCount(type) != oldAmount) {
            if (marketWas != null) marketWas.fireChanges(type, amount);
            return true;
        }
        return false;
    }

    /**
     * Set a destination for a unit.
     *
     * @param unit The <code>Unit</code> to direct.
     * @param destination The destination <code>Location</code>.
     * @return True if the destination was set.
     */
    private boolean askSetDestination(Unit unit, Location destination) {
        return askServer().setDestination(unit, destination)
            && unit.getDestination() == destination;
    }

    /**
     * Unload some goods from a carrier.
     *
     * @param type The <code>GoodsType</code> to unload.
     * @param amount The amount of goods to unload.
     * @param carrier The <code>Unit</code> carrying the goods.
     * @return True if the unload succeeded.
     */
    private boolean askUnloadGoods(GoodsType type, int amount, Unit carrier) {
        // Do not check for trade location, unloading can include dumping
        // which can happen anywhere
        final Player player = getMyPlayer();
        final Market market = player.getMarket();
        MarketWas marketWas = (market != null) ? new MarketWas(player) : null;

        int oldAmount = carrier.getGoodsContainer().getGoodsCount(type);
        if (askServer().unloadGoods(type, amount, carrier)
            && carrier.getGoodsContainer().getGoodsCount(type) != oldAmount) {
            if (marketWas != null) marketWas.fireChanges(type, -amount);
            return true;
        }
        return false;
    }


    // Utilities connected with saving the game

    /**
     * Get the trunk of the save game string.
     *
     * @param game The <code>Game</code> to query.
     * @return The trunk of the file name to use for saved games.
     */
    private String getSaveGameString(Game game) {
        final Player player = getMyPlayer();
        final String gid = Integer.toHexString(game.getUUID().hashCode());
        final Turn turn = game.getTurn();
        return (/* player.getName() + "_" */ gid
            + "_" + Messages.message(player.getNationLabel())
            + "_" + turn.getSaveGameSuffix()
            + "." + FreeCol.FREECOL_SAVE_EXTENSION)
            .replaceAll(" ", "_");
    }

    /**
     * Creates at least one autosave game file of the currently played
     * game in the autosave directory.  Does nothing if there is no
     * game running.
     */
    private void autoSaveGame () {
        final Game game = getGame();
        if (game == null) return;

        // unconditional save per round (fixed file "last-turn")
        final ClientOptions options = getClientOptions();
        final String prefix = options.getText(ClientOptions.AUTO_SAVE_PREFIX);
        final String lastTurnName = prefix + "-"
            + options.getText(ClientOptions.LAST_TURN_NAME)
            + "." + FreeCol.FREECOL_SAVE_EXTENSION;
        final String beforeLastTurnName = prefix + "-"
            + options.getText(ClientOptions.BEFORE_LAST_TURN_NAME)
            + "." + FreeCol.FREECOL_SAVE_EXTENSION;
        File autoSaveDir = FreeColDirectories.getAutosaveDirectory();
        File lastTurnFile = new File(autoSaveDir, lastTurnName);
        File beforeLastTurnFile = new File(autoSaveDir, beforeLastTurnName);
        // if "last-turn" file exists, shift it to "before-last-turn" file
        if (lastTurnFile.exists()) {
            beforeLastTurnFile.delete();
            lastTurnFile.renameTo(beforeLastTurnFile);
        }
        saveGame(lastTurnFile);

        // conditional save after user-set period
        int saveGamePeriod = options.getInteger(ClientOptions.AUTOSAVE_PERIOD);
        int turnNumber = game.getTurn().getNumber();
        if (saveGamePeriod >= 1 && turnNumber % saveGamePeriod == 0) {
            String fileName = prefix + "-" + getSaveGameString(game);
            saveGame(new File(autoSaveDir, fileName));
        }
    }

    /**
     * Saves the game to the given file.
     *
     * @param file The <code>File</code>.
     * @return True if the game was saved.
     */
    private boolean saveGame(final File file) {
        final FreeColServer server = getFreeColServer();
        boolean result = false;
        if (server != null) {
            getGUI().showStatusPanel(Messages.message("status.savingGame"));
            try {
                server.saveGame(file, getClientOptions(), getGUI().getActiveUnit());
                result = true;
            } catch (IOException e) {
                getGUI().showErrorMessage(FreeCol.badSave(file));
            } finally {
                getGUI().closeStatusPanel();
            }
        }
        return result;
    }


    // Utilities for message handling.

    /**
     * Provides an opportunity to filter the messages delivered to the canvas.
     *
     * @param message the message that is candidate for delivery to the canvas
     * @return true if the message should be delivered
     */
    private boolean shouldAllowMessage(ModelMessage message) {
        BooleanOption option = getClientOptions().getBooleanOption(message);
        return (option == null) ? true : option.getValue();
    }

    /**
     * Start ignoring a kind of message.
     *
     * @param key The key for a message to ignore.
     * @param turn The current <code>Turn</code>.
     */
    private synchronized void startIgnoringMessage(String key, Turn turn) {
        messagesToIgnore.put(key, turn.getNumber());
        logger.finer("Ignore message start: " + key);
    }

    /**
     * Stop ignoring a kind of message.
     *
     * @param key The key for a message to stop ignoring.
     */
    private synchronized void stopIgnoringMessage(String key) {
        messagesToIgnore.remove(key);
        logger.finer("Ignore message stop: " + key);
    }

    /**
     * Reap all ignored message keys that are older than the given turn.
     *
     * @param turn The <code>Turn</code> value to test against.
     */
    private synchronized void reapIgnoredMessages(Turn turn) {
        Iterator<String> keys = messagesToIgnore.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if (messagesToIgnore.get(key) < turn.getNumber()) {
                keys.remove();
                logger.finer("Ignore message reap: " + key);
            }
        }
    }

    /**
     * See if messages with a given key were ignored last turn.  If so,
     * continue to ignore them.
     *
     * @param key The key to check.
     * @param turn The current <code>Turn</code>.
     * @return True if the message should continue to be ignored.
     */
    private synchronized boolean continueIgnoreMessage(String key, Turn turn) {
        Integer value;
        if (key != null
            && (value = messagesToIgnore.get(key)) != null
            && value + 1 == turn.getNumber()) {
            messagesToIgnore.put(key, value + 1);
            logger.finer("Ignore message continue: " + key);
            return true;
        }
        return false;
    }

    /**
     * Displays the messages in the current turn report.
     */
    public void displayTurnReportMessages() {
        getGUI().showReportTurnPanel(turnReportMessages);
    }

    /**
     * Displays pending <code>ModelMessage</code>s.
     *
     * @param allMessages Display all messages or just the undisplayed ones.
     * @param endOfTurn Use a turn report panel if necessary.
     * @return True if any messages were displayed.
     */
    public boolean displayModelMessages(final boolean allMessages,
                                        final boolean endOfTurn) {
        final Player player = getMyPlayer();
        final Turn thisTurn = getGame().getTurn();
        final ArrayList<ModelMessage> messages = new ArrayList<>();

        for (ModelMessage m : ((allMessages) ? player.getModelMessages()
                : player.getNewModelMessages())) {
            if (shouldAllowMessage(m)
                && !continueIgnoreMessage(m.getIgnoredMessageKey(), thisTurn)) {
                messages.add(m);
            }
            m.setBeenDisplayed(true);
        }

        reapIgnoredMessages(thisTurn);

        if (!messages.isEmpty()) {
            Runnable uiTask;
            if (endOfTurn) {
                turnReportMessages.addAll(messages);
                uiTask = () -> { displayTurnReportMessages(); };
            } else {
                uiTask = () -> { getGUI().showModelMessages(messages); };
            }
            getGUI().invokeNowOrWait(uiTask);
        }
        return !messages.isEmpty();
    }


    // Utilities to handle the transitions between the active-unit,
    // execute-orders and end-turn states.

    /**
     * Do the goto orders operation.
     *
     * @return True if all goto orders have been performed and no units
     *     reached their destination and are free to move again.
     */
    private boolean doExecuteGotoOrders() {
        if (getGUI().isShowingSubPanel()) return false; // Clear the panel first

        final Player player = getMyPlayer();
        final Unit active = getGUI().getActiveUnit();
        Unit stillActive = null;

        // Ensure the goto mode sticks.
        moveMode = moveMode.maximize(MoveMode.EXECUTE_GOTO_ORDERS);

        // Deal with the trade route units first.
        List<ModelMessage> messages = new ArrayList<>();
        for (Unit unit : toSortedList(player.getUnits().stream()
                .filter(u -> u.isReadyToTrade() && player.owns(u)),
                tradeRouteUnitComparator)) {
            getGUI().setActiveUnit(unit);
            if (moveToDestination(unit, messages)) stillActive = unit;
        }
        if (!messages.isEmpty()) {
            for (ModelMessage m : messages) {
                player.addModelMessage(m);
                turnReportMessages.add(m);
            }
            displayModelMessages(false, false);
            getGUI().setActiveUnit((stillActive != null) ? stillActive : active);
            return false;
        }

        // The active unit might also be a going-to unit.  Make sure it
        // gets processed first.  setNextGoingToUnit will fail harmlessly
        // if it is not a going-to unit so this is safe.
        if (active != null) player.setNextGoingToUnit(active);

        // Process all units.
        boolean fail = false;
        while (player.hasNextGoingToUnit()) {
            Unit unit = player.getNextGoingToUnit();
            getGUI().setActiveUnit(unit);
            // Move the unit as much as possible
            if (moveToDestination(unit, null)) stillActive = unit;

            // Might have LCR messages to display
            displayModelMessages(false, false);

            // Give the player a chance to deal with any problems
            // shown in a popup before pressing on with more moves.
            if (getGUI().isShowingSubPanel()) {
                getGUI().requestFocusForSubPanel();
                fail = true;
                break;
            }
        }
        getGUI().setActiveUnit((stillActive != null) ? stillActive : active);
        return stillActive == null && !fail;
    }

    /**
     * End the turn.
     *
     * @param showDialog Show the end turn dialog?
     * @return True if the turn ended.
     */
    private boolean doEndTurn(boolean showDialog) {
        final Player player = getMyPlayer();
        if (showDialog) {
            List<Unit> units = transform(player.getUnits(), Unit::couldMove,
                Collectors.toList());
            if (!units.isEmpty()) {
                // Modal dialog takes over
                getGUI().showEndTurnDialog(units,
                    (Boolean value) -> {
                        if (value != null && value) {
                            endTurn(false);
                        }
                    });
                return false;
            }
        }

        // Ensure end-turn mode sticks.
        moveMode = moveMode.maximize(MoveMode.END_TURN);

        // Make sure all goto orders are complete before ending turn, and
        // that nothing (like a LCR exploration) has cancelled the end turn.
        if (!doExecuteGotoOrders()
            || moveMode.ordinal() < MoveMode.END_TURN.ordinal()) return false;

        // Check for desync as last thing!
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.DESYNC)
            && DebugUtils.checkDesyncAction(getFreeColClient())) {
            getConnectController().reconnect();
            return false;
        }

        // Clean up lingering menus.
        getGUI().closeMenus();

        // Clear active unit if any.
        getGUI().setActiveUnit(null);

        // Unskip all skipped, some may have been faked in-client.
        // Server-side skipped units are set active in csNewTurn.
        for (Unit unit : player.getUnits()) {
            if (unit.getState() == UnitState.SKIPPED) {
                unit.setState(UnitState.ACTIVE);
            }
        }

        // Restart the selection cycle.
        moveMode = MoveMode.NEXT_ACTIVE_UNIT;

        // Clear outdated turn report messages.
        turnReportMessages.clear();

        // Inform the server of end of turn.
        return askServer().endTurn(getGame());
    }

    /**
     * Makes a new unit active if any, or focus on a tile (useful if the
     * current unit just died).
     *
     * Displays any new <code>ModelMessage</code>s with
     * {@link #nextModelMessage}.
     *
     * @param tile The <code>Tile</code> to select if no new unit can
     *     be made active.
     * @return True if the active unit changes.
     */
    private boolean updateActiveUnit(Tile tile) {
        // Make sure the active unit is done.
        final Player player = getMyPlayer();
        Unit unit = getGUI().getActiveUnit();
        if (unit != null && unit.couldMove()) return false;

        // Flush any outstanding orders once the mode is raised.
        if (moveMode != MoveMode.NEXT_ACTIVE_UNIT
            && !doExecuteGotoOrders()) {
            return false;
        }

        // Successfully found a unit to display
        if (player.hasNextActiveUnit()) {
            getGUI().setActiveUnit(player.getNextActiveUnit());
            return true;
        }

        // No unit to find.
        getGUI().setActiveUnit(null);

        // No active units left.  Do the goto orders.
        if (!doExecuteGotoOrders()) return true;

        // If not already ending the turn, use the fallback tile if
        // supplied, then check for automatic end of turn, otherwise
        // just select nothing and wait.
        final ClientOptions options = getClientOptions();
        if (tile != null) {
            getGUI().setSelectedTile(tile);
        } else if (options.getBoolean(ClientOptions.AUTO_END_TURN)) {
            doEndTurn(options.getBoolean(ClientOptions.SHOW_END_TURN_DIALOG));
        }
        return true;
    }


    // Movement support.

    /**
     * Moves the given unit towards its destination/s if possible.
     *
     * @param unit The <code>Unit</code> to move.
     * @param messages An optional list in which to retain any
     *     trade route <code>ModelMessage</code>s generated.
     * @return True if the unit reached its destination, is still alive,
     *     and has more moves to make.
     */
    private boolean moveToDestination(Unit unit, List<ModelMessage> messages) {
        Location destination;
        if (!requireOurTurn()
            || unit.isAtSea()
            || unit.getMovesLeft() <= 0
            || unit.getState() == UnitState.SKIPPED) {
            return false;
        } else if (unit.getTradeRoute() != null) {
            return followTradeRoute(unit, messages);
        } else if ((destination = unit.getDestination()) == null) {
            return unit.getMovesLeft() > 0;
        } else if (!changeState(unit, UnitState.ACTIVE)) {
            return false;
        }
        getGUI().setActiveUnit(unit);

        // Find a path to the destination and try to follow it.
        final Player player = getMyPlayer();
        PathNode path = unit.findPath(destination);
        if (path == null) {
            StringTemplate src = unit.getLocation()
                .getLocationLabelFor(player);
            StringTemplate dst = destination.getLocationLabelFor(player);
            StringTemplate template = StringTemplate
                .template("info.moveToDestinationFailed")
                .addStringTemplate("%unit%",
                    unit.getLabel(Unit.UnitLabelType.NATIONAL))
                .addStringTemplate("%location%", src)
                .addStringTemplate("%destination%", dst);
            getGUI().showInformationMessage(unit, template);
            return false;
        }

        // Clear ordinary destinations if arrived.
        if (movePath(unit, path) && unit.isAtLocation(destination)) {
            askClearGotoOrders(unit);
            Colony colony = (unit.hasTile()) ? unit.getTile().getColony()
                : null;
            if (colony != null && !checkCashInTreasureTrain(unit)) {
                colonyPanel(colony, unit);
            }
            return unit.couldMove();
        }
        return false;
    }

    /**
     * Move a unit in a given direction.
     *
     * Public for the test suite.
     *
     * @param unit The <code>Unit</code> to move.
     * @param direction The <code>Direction</code> to move in.
     * @param interactive Interactive mode: play sounds and emit errors.
     * @return True if the unit can possibly move further.
     */
    public boolean moveDirection(Unit unit, Direction direction,
                                 boolean interactive) {
        // If this move would reach the unit destination but we
        // discover that it would be permanently impossible to complete,
        // clear the destination.
        Unit.MoveType mt = unit.getMoveType(direction);
        Location destination = unit.getDestination();
        Tile oldTile = unit.getTile();
        boolean clearDestination = destination != null
            && oldTile != null
            && Map.isSameLocation(oldTile.getNeighbourOrNull(direction),
                                  destination);

        // Consider all the move types.
        boolean result = mt.isLegal();
        switch (mt) {
        case MOVE_HIGH_SEAS:
            if (getMyPlayer().getEurope() == null) {
                ; // do nothing
            } else if (destination == null) {
                result = moveHighSeas(unit, direction);
                break;
            } else if (destination instanceof Europe) {
                result = moveTo(unit, destination);
                break;
            }
            // Fall through
        case MOVE:
            result = moveMove(unit, direction);
            break;
        case EXPLORE_LOST_CITY_RUMOUR:
            result = moveExplore(unit, direction);
            break;
        case ATTACK_UNIT:
            result = moveAttack(unit, direction);
            break;
        case ATTACK_SETTLEMENT:
            result = moveAttackSettlement(unit, direction);
            break;
        case EMBARK:
            result = moveEmbark(unit, direction);
            break;
        case ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST:
            result = moveLearnSkill(unit, direction);
            break;
        case ENTER_INDIAN_SETTLEMENT_WITH_SCOUT:
            result = moveScoutIndianSettlement(unit, direction);
            break;
        case ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY:
            result = moveUseMissionary(unit, direction);
            break;
        case ENTER_FOREIGN_COLONY_WITH_SCOUT:
            result = moveScoutColony(unit, direction);
            break;
        case ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS:
            result = moveTrade(unit, direction);
            break;

        // Illegal moves
        case MOVE_NO_ACCESS_BEACHED:
            if (interactive || clearDestination) {
                sound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                getGUI().showInformationMessage(unit, StringTemplate
                    .template("move.noAccessBeached")
                    .addStringTemplate("%nation%", nation));
            }
            break;
        case MOVE_NO_ACCESS_CONTACT:
            if (interactive || clearDestination) {
                sound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                getGUI().showInformationMessage(unit, StringTemplate
                    .template("move.noAccessContact")
                    .addStringTemplate("%nation%", nation));
            }
            break;
        case MOVE_NO_ACCESS_GOODS:
            if (interactive || clearDestination) {
                sound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                getGUI().showInformationMessage(unit, StringTemplate
                    .template("move.noAccessGoods")
                    .addStringTemplate("%nation%", nation)
                    .addStringTemplate("%unit%",
                        unit.getLabel(Unit.UnitLabelType.NATIONAL)));
            }
            break;
        case MOVE_NO_ACCESS_LAND:
            if (!moveDisembark(unit, direction)) {
                if (interactive) {
                    sound("sound.event.illegalMove");
                }
            }
            break;
        case MOVE_NO_ACCESS_MISSION_BAN:
            if (interactive || clearDestination) {
                sound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                getGUI().showInformationMessage(unit, StringTemplate
                    .template("move.noAccessMissionBan")
                    .addStringTemplate("%unit%",
                        unit.getLabel(Unit.UnitLabelType.NATIONAL))
                    .addStringTemplate("%nation%", nation));
            }
            break;
        case MOVE_NO_ACCESS_SETTLEMENT:
            if (interactive || clearDestination) {
                sound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                getGUI().showInformationMessage(unit, StringTemplate
                    .template("move.noAccessSettlement")
                    .addStringTemplate("%unit%",
                        unit.getLabel(Unit.UnitLabelType.NATIONAL))
                    .addStringTemplate("%nation%", nation));
            }
            break;
        case MOVE_NO_ACCESS_SKILL:
            if (interactive || clearDestination) {
                sound("sound.event.illegalMove");
                getGUI().showInformationMessage(unit, StringTemplate
                    .template("move.noAccessSkill")
                    .addStringTemplate("%unit%",
                        unit.getLabel(Unit.UnitLabelType.NATIONAL)));
            }
            break;
        case MOVE_NO_ACCESS_TRADE:
            if (interactive || clearDestination) {
                sound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                getGUI().showInformationMessage(unit, StringTemplate
                    .template("move.noAccessTrade")
                    .addStringTemplate("%nation%", nation));
            }
            break;
        case MOVE_NO_ACCESS_WAR:
            if (interactive || clearDestination) {
                sound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                getGUI().showInformationMessage(unit, StringTemplate
                    .template("move.noAccessWar")
                    .addStringTemplate("%nation%", nation));
            }
            break;
        case MOVE_NO_ACCESS_WATER:
            if (interactive || clearDestination) {
                sound("sound.event.illegalMove");
                getGUI().showInformationMessage(unit, StringTemplate
                    .template("move.noAccessWater")
                    .addStringTemplate("%unit%",
                        unit.getLabel(Unit.UnitLabelType.NATIONAL)));
            }
            break;
        case MOVE_NO_ATTACK_MARINE:
            if (interactive || clearDestination) {
                sound("sound.event.illegalMove");
                getGUI().showInformationMessage(unit, StringTemplate
                    .template("move.noAttackWater")
                    .addStringTemplate("%unit%",
                        unit.getLabel(Unit.UnitLabelType.NATIONAL)));
            }
            break;
        case MOVE_NO_MOVES:
            // The unit may have some moves left, but not enough
            // to move to the next node.  The move is illegal
            // this turn, but might not be next turn, so do not cancel the
            // destination but set the state to skipped instead.
            clearDestination = false;
            unit.setState(UnitState.SKIPPED);
            break;
        case MOVE_NO_TILE:
            if (interactive || clearDestination) {
                sound("sound.event.illegalMove");
                getGUI().showInformationMessage(unit, StringTemplate
                    .template("move.noTile")
                    .addStringTemplate("%unit%",
                        unit.getLabel(Unit.UnitLabelType.NATIONAL)));
            }
            break;
        default:
            if (interactive || clearDestination) {
                sound("sound.event.illegalMove");
            }
            result = false;
            break;
        }
        if (clearDestination && !unit.isDisposed()) {
            askClearGotoOrders(unit);
        }
        return result;
    }

    /**
     * Follow a path.
     *
     * @param unit The <code>Unit</code> to move.
     * @param path The path to follow.
     * @return True if the unit has completed the path and can move further.
     */
    private boolean movePath(Unit unit, PathNode path) {
        for (; path != null; path = path.next) {
            if (unit.isAtLocation(path.getLocation())) continue;

            if (path.getLocation() instanceof Europe) {
                if (unit.hasTile()
                    && unit.getTile().isDirectlyHighSeasConnected()) {
                    moveTo(unit, path.getLocation());
                } else {
                    logger.warning("Can not move to Europe from "
                        + unit.getLocation()
                        + " on path: " + path.fullPathToString());
                }
                return false;
            } else if (path.getLocation() instanceof Tile) {
                if (path.getDirection() == null) {
                    if (unit.isInEurope()) {
                        moveTo(unit, unit.getGame().getMap());
                    } else {
                        logger.warning("Null direction on path: "
                            + path.fullPathToString());
                    }
                    return false;
                } else {
                    if (!moveDirection(unit, path.getDirection(), false)) {
                        return false;
                    }
                    if (unit.hasTile()
                        && unit.getTile().getDiscoverableRegion() != null) {
                        // Break up the goto to allow region naming to occur,
                        // BR#2707
                        return false;
                    }
                }
            } else if (path.getLocation() instanceof Unit) {
                return moveEmbark(unit, path.getDirection());

            } else {
                logger.warning("Bad path: " + path.fullPathToString());
            }
        }
        return true;
    }

    /**
     * Confirm attack or demand a tribute from a native settlement, following
     * an attacking move.
     *
     * @param unit The <code>Unit</code> to perform the attack.
     * @param direction The direction in which to attack.
     * @return True if the unit could move further.
     */
    private boolean moveAttack(Unit unit, Direction direction) {
        Tile tile = unit.getTile();
        Tile target = tile.getNeighbourOrNull(direction);
        Unit u = target.getFirstUnit();
        if (u == null || unit.getOwner().owns(u)) return false;

        askClearGotoOrders(unit);
        if (getGUI().confirmHostileAction(unit, target)
            && getGUI().confirmPreCombat(unit, target)) {
            askServer().attack(unit, direction);
        }
        // Always return false, as the unit has either attacked and lost
        // its remaining moves, or the move can not proceed because it is
        // blocked.
        return false;
    }

    /**
     * Confirm attack or demand a tribute from a settlement, following
     * an attacking move.
     *
     * @param unit The <code>Unit</code> to perform the attack.
     * @param direction The direction in which to attack.
     * @return True if the unit could move further.
     */
    private boolean moveAttackSettlement(Unit unit, Direction direction) {
        Tile tile = unit.getTile();
        Tile target = tile.getNeighbourOrNull(direction);
        Settlement settlement = target.getSettlement();
        if (settlement == null
            || unit.getOwner().owns(settlement)) return false;

        ArmedUnitSettlementAction act
            = getGUI().getArmedUnitSettlementChoice(settlement);
        if (act == null) return true; // Cancelled
        switch (act) {
        case SETTLEMENT_ATTACK:
            if (getGUI().confirmHostileAction(unit, target)
                && getGUI().confirmPreCombat(unit, target)) {
                askServer().attack(unit, direction);
                Colony col = target.getColony();
                if (col != null && unit.getOwner().owns(col)) {
                    colonyPanel(col, unit);
                }
                return false;
            }
            break;
        case SETTLEMENT_TRIBUTE:
            int amount = (settlement instanceof Colony)
                ? getGUI().confirmEuropeanTribute(unit, (Colony)settlement,
                    getNationSummary(settlement.getOwner()))
                : (settlement instanceof IndianSettlement)
                ? getGUI().confirmNativeTribute(unit, (IndianSettlement)settlement)
                : -1;
            if (amount <= 0) return true; // Cancelled
            return moveTribute(unit, amount, direction);

        default:
            logger.warning("showArmedUnitSettlementDialog fail: " + act);
            break;
        }
        return true;
    }

    /**
     * Initiates diplomacy with a foreign power.
     *
     * @param unit The <code>Unit</code> negotiating.
     * @param direction The direction of a settlement to negotiate with.
     * @param dt The base <code>DiplomaticTrade</code> agreement to
     *     begin the negotiation with.
     * @return True if the unit can move further.
     */
    private boolean moveDiplomacy(Unit unit, Direction direction,
                                  DiplomaticTrade dt) {
        Settlement settlement = getSettlementAt(unit.getTile(), direction);
        if (settlement == null
            || !(settlement instanceof Colony)) return false;
        Colony colony = (Colony)settlement;

        // Can not negotiate with the REF.
        final Game game = getGame();
        final Player player = unit.getOwner();
        final Player other = colony.getOwner();
        if (other == player.getREFPlayer()) return false;

        StringTemplate nation = other.getNationLabel();
        while (dt != null) {
            // Inform server of current agreement.
            dt = askServer().diplomacy(unit, colony, dt);
            // Returned dt will be null if we sent or the other player
            // replied with an accept/reject.  Otherwise consider
            // counter proposal.
            if (dt != null) {
                dt = getGUI().showNegotiationDialog(unit, colony, dt,
                    dt.getSendMessage(player, colony));
            }
        }
        return false;
    }

    /**
     * Check the carrier for passengers to disembark, possibly
     * snatching a useful result from the jaws of a
     * MOVE_NO_ACCESS_LAND failure.
     *
     * @param unit The carrier containing the unit to disembark.
     * @param direction The direction in which to disembark the unit.
     * @return True if the disembark "succeeds" (which deliberately includes
     *     declined disembarks).
     */
    private boolean moveDisembark(Unit unit, final Direction direction) {
        Tile tile = unit.getTile().getNeighbourOrNull(direction);
        if (tile.getFirstUnit() != null
            && tile.getFirstUnit().getOwner() != unit.getOwner()) {
            return false; // Can not disembark onto other nation units.
        }

        // Disembark selected units able to move.
        unit.setStateToAllChildren(UnitState.ACTIVE);
        final List<Unit> disembarkable = transform(unit.getUnitList(),
            u -> u.getMoveType(tile).isProgress(), Collectors.toList());
        if (disembarkable.isEmpty()) return false; // Fail, did not find one
        if (disembarkable.size() == 1) {
            if (getGUI().confirm(tile,
                            StringTemplate.key("disembark.text"),
                            disembarkable.get(0), "ok", "cancel")) {
                moveDirection(disembarkable.get(0), direction, false);
            }
        } else {
            List<ChoiceItem<Unit>> choices = toList(map(disembarkable, u ->
                    new ChoiceItem<Unit>(u.getDescription(Unit.UnitLabelType.NATIONAL), u)));
            choices.add(new ChoiceItem<>(Messages.message("all"), unit));

            // Use moveDirection() to disembark units as while the
            // destination tile is known to be clear of other player
            // units or settlements, it may have a rumour or need
            // other special handling.
            Unit u = getGUI().getChoice(unit.getTile(),
                                   Messages.message("disembark.text"),
                                   unit,
                                   "none", choices);
            if (u == null) {
                // Cancelled, done.
            } else if (u == unit) {
                // Disembark all.
                for (Unit dUnit : disembarkable) {
                    // Guard against loss of control when asking the
                    // server to move the unit.
                    try {
                        moveDirection(dUnit, direction, false);
                    } finally {
                        continue;
                    }
                }
            } else {
                moveDirection(u, direction, false);
            }
        }
        return true;
    }

    /**
     * Embarks the specified unit onto a carrier in a specified direction
     * following a move of MoveType.EMBARK.
     *
     * @param unit The <code>Unit</code> that wishes to embark.
     * @param direction The direction in which to embark.
     * @return True if the unit could move further.
     */
    private boolean moveEmbark(Unit unit, Direction direction) {
        if (unit.getColony() != null
            && !getGUI().confirmLeaveColony(unit)) return false;

        Tile sourceTile = unit.getTile();
        Tile destinationTile = sourceTile.getNeighbourOrNull(direction);
        Unit carrier = null;
        List<ChoiceItem<Unit>> choices = transform(destinationTile.getUnitList(),
            u -> u.canAdd(unit),
            u -> new ChoiceItem<>(u.getDescription(Unit.UnitLabelType.NATIONAL), u),
            Collectors.toList());
        if (choices.isEmpty()) {
            throw new RuntimeException("Unit " + unit.getId()
                + " found no carrier to embark upon.");
        } else if (choices.size() == 1) {
            carrier = choices.get(0).getObject();
        } else {
            carrier = getGUI().getChoice(unit.getTile(),
                                    Messages.message("embark.text"),
                                    unit,
                                    "none", choices);
            if (carrier == null) return true; // User cancelled
        }

        // Proceed to embark
        askClearGotoOrders(unit);
        if (!askServer().embark(unit, carrier, direction)
            || unit.getLocation() != carrier) {
            unit.setState(UnitState.SKIPPED);
            return false;
        }
        unit.getOwner().invalidateCanSeeTiles();
        return false;
    }

    /**
     * Confirm exploration of a lost city rumour, following a move of
     * MoveType.EXPLORE_LOST_CITY_RUMOUR.
     *
     * @param unit The <code>Unit</code> that is exploring.
     * @param direction The direction of a rumour.
     * @return True if the unit can move further.
     */
    private boolean moveExplore(Unit unit, Direction direction) {
        Tile tile = unit.getTile().getNeighbourOrNull(direction);
        if (!getGUI().confirm(unit.getTile(),
                StringTemplate.key("exploreLostCityRumour.text"), unit,
                "exploreLostCityRumour.yes", "exploreLostCityRumour.no")) {
            if (unit.getDestination() != null) {
                askClearGotoOrders(unit);
                return false; // Need to break out of movePath
            }
            return true;
        }
        if (tile.getLostCityRumour().getType()== LostCityRumour.RumourType.MOUNDS
            && !getGUI().confirm(unit.getTile(),
                StringTemplate.key("exploreMoundsRumour.text"), unit,
                "exploreLostCityRumour.yes", "exploreLostCityRumour.no")) {
            askServer().declineMounds(unit, direction); // LCR goes away
        }
        // Prevent turn ending at once to allow FoY prompts to complete
        moveMode = moveMode.minimize(MoveMode.EXECUTE_GOTO_ORDERS);
        return moveMove(unit, direction);
    }

    /**
     * Moves a unit onto the "high seas" in a specified direction following
     * a move of MoveType.MOVE_HIGH_SEAS.
     * This may result in a move to Europe, no move, or an ordinary move.
     *
     * @param unit The <code>Unit</code> to be moved.
     * @param direction The direction in which to move.
     * @return True if the unit can move further.
     */
    private boolean moveHighSeas(Unit unit, Direction direction) {
        // Confirm moving to Europe if told to move to a null tile
        // (FIXME: can this still happen?), or if crossing the boundary
        // between coastal and high sea.  Otherwise just move.
        Tile oldTile = unit.getTile();
        Tile newTile = oldTile.getNeighbourOrNull(direction);
        if (newTile == null
            || (!oldTile.isDirectlyHighSeasConnected()
                && newTile.isDirectlyHighSeasConnected())) {
            if (unit.getTradeRoute() != null) {
                TradeRouteStop stop = unit.getStop();
                if (stop != null && TradeRoute.isStopValid(unit, stop)
                    && stop.getLocation() instanceof Europe) {
                    moveTo(unit, stop.getLocation());
                    return false;
                }
            } else if (unit.getDestination() instanceof Europe) {
                moveTo(unit, unit.getDestination());
                return false;
            } else {
                if (getGUI().confirm(oldTile, StringTemplate
                        .template("highseas.text")
                        .addAmount("%number%", unit.getSailTurns()),
                        unit, "highseas.yes", "highseas.no")) {
                    moveTo(unit, unit.getOwner().getEurope());
                    return false;
                }
            }
        }
        return moveMove(unit, direction);
    }

    /**
     * Move a free colonist to a native settlement to learn a skill following
     * a move of MoveType.ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST.
     * The colonist does not physically get into the village, it will
     * just stay where it is and gain the skill.
     *
     * @param unit The <code>Unit</code> to learn the skill.
     * @param direction The direction in which the Indian settlement lies.
     * @return True if the unit can move further.
     */
    private boolean moveLearnSkill(Unit unit, Direction direction) {
        askClearGotoOrders(unit);
        // Refresh knowledge of settlement skill.  It may have been
        // learned by another player.
        if (!askServer().askSkill(unit, direction)) return false;

        IndianSettlement settlement
            = (IndianSettlement)getSettlementAt(unit.getTile(), direction);
        UnitType skill = settlement.getLearnableSkill();
        if (skill == null) {
            getGUI().showInformationMessage(settlement, "info.noMoreSkill");
        } else if (!unit.getType().canBeUpgraded(skill, ChangeType.NATIVES)) {
            getGUI().showInformationMessage(settlement, StringTemplate
                .template("info.cantLearnSkill")
                .addStringTemplate("%unit%",
                    unit.getLabel(Unit.UnitLabelType.NATIONAL))
                .addNamed("%skill%", skill));
        } else if (getGUI().confirm(unit.getTile(), StringTemplate
                .template("learnSkill.text")
                .addNamed("%skill%", skill),
                unit, "learnSkill.yes", "learnSkill.no")) {
            if (askServer().learnSkill(unit, direction)) {
                if (unit.isDisposed()) {
                    getGUI().showInformationMessage(settlement, "learnSkill.die");
                    return false;
                }
                if (unit.getType() != skill) {
                    getGUI().showInformationMessage(settlement, "learnSkill.leave");
                }
            }
        }
        return false;
    }

    /**
     * Actually move a unit in a specified direction, following a move
     * of MoveType.MOVE.
     *
     * @param unit The <code>Unit</code> to be moved.
     * @param direction The direction in which to move the Unit.
     * @return True if the unit can move further.
     */
    private boolean moveMove(Unit unit, Direction direction) {
        final ClientOptions options = getClientOptions();
        if (unit.canCarryUnits() && unit.hasSpaceLeft()
            && options.getBoolean(ClientOptions.AUTOLOAD_SENTRIES)) {
            // Autoload sentries if selected
            List<Unit> waiting = (unit.getColony() != null)
                ? unit.getTile().getUnitList()
                : Collections.<Unit>emptyList();
            for (Unit u : waiting) {
                if (u.getState() != UnitState.SENTRY
                    || !unit.couldCarry(u)) continue;
                try {
                    askEmbark(u, unit);
                } finally {
                    if (u.getLocation() != unit) {
                        u.setState(UnitState.SKIPPED);
                    }
                    continue;
                }
            }
            // Boarding consumed this unit's moves.
            if (unit.getMovesLeft() <= 0) return false;
        }

        // Ask the server
        if (!askServer().move(unit, direction)) {
            // Can fail due to desynchronization.  Skip this unit so
            // we do not end up retrying indefinitely.
            unit.setState(UnitState.SKIPPED);
            return false;
        }

        unit.getOwner().invalidateCanSeeTiles();
        
        final Tile tile = unit.getTile();

        // Perform a short pause on an active unit's last move if
        // the option is enabled.
        if (unit.getMovesLeft() <= 0
            && options.getBoolean(ClientOptions.UNIT_LAST_MOVE_DELAY)) {
            getGUI().paintImmediatelyCanvasInItsBounds();
            try {
                Thread.sleep(UNIT_LAST_MOVE_DELAY);
            } catch (InterruptedException e) {} // Ignore
        }

        // Update the active unit and GUI.
        boolean ret = !unit.isDisposed() && !checkCashInTreasureTrain(unit);
        if (ret) {
            if (tile.getColony() != null && unit.isCarrier()) {
                final Colony colony = tile.getColony();
                if (unit.getTradeRoute() == null
                    && Map.isSameLocation(tile, unit.getDestination())) {
                    colonyPanel(colony, unit);
                }
            }
            ret = unit.getMovesLeft() > 0;
        }
        return ret;
    }

    /**
     * Move to a foreign colony and either attack, negotiate with the
     * foreign power or spy on them.  Follows a move of
     * MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT.
     *
     * FIXME: Unify trade and negotiation.
     *
     * @param unit The unit that will spy, negotiate or attack.
     * @param direction The direction in which the foreign colony lies.
     * @return True if the unit can move further.
     */
    private boolean moveScoutColony(Unit unit, Direction direction) {
        final Game game = getGame();
        Colony colony = (Colony) getSettlementAt(unit.getTile(), direction);
        boolean canNeg = colony.getOwner() != unit.getOwner().getREFPlayer();
        askClearGotoOrders(unit);

        ScoutColonyAction act
            = getGUI().getScoutForeignColonyChoice(colony, unit, canNeg);
        if (act == null) return true; // Cancelled
        switch (act) {
        case SCOUT_COLONY_ATTACK:
            return moveAttackSettlement(unit, direction);
        case SCOUT_COLONY_NEGOTIATE:
            Player player = unit.getOwner();
            DiplomaticTrade agreement
                = new DiplomaticTrade(game, TradeContext.DIPLOMATIC,
                                      player, colony.getOwner(), null, 0);
            agreement = getGUI().showNegotiationDialog(unit, colony,
                agreement, agreement.getSendMessage(player, colony));
            return (agreement == null
                || agreement.getStatus() == TradeStatus.REJECT_TRADE) ? true
                : moveDiplomacy(unit, direction, agreement);
        case SCOUT_COLONY_SPY:
            return moveSpy(unit, direction);
        default:
            logger.warning("showScoutForeignColonyDialog fail: " + act);
            break;
        }
        return true;
    }

    /**
     * Move a scout into an Indian settlement to speak with the chief,
     * or demand a tribute following a move of
     * MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT.
     * The scout does not physically get into the village, it will
     * just stay where it is.
     *
     * @param unit The <code>Unit</code> that is scouting.
     * @param direction The direction in which the Indian settlement lies.
     * @return True if the unit can move further.
     */
    private boolean moveScoutIndianSettlement(Unit unit, Direction direction) {
        Tile unitTile = unit.getTile();
        Tile tile = unitTile.getNeighbourOrNull(direction);
        IndianSettlement settlement = tile.getIndianSettlement();
        Player player = unit.getOwner();
        askClearGotoOrders(unit);

        // Offer the choices.
        if (!askServer().scoutSettlement(unit, direction)) return false;
        int count = player.getNationSummary(settlement.getOwner())
            .getNumberOfSettlements();
        String number = (count <= 0) ? Messages.message("many")
            : Integer.toString(count);
        ScoutIndianSettlementAction act
            = getGUI().getScoutIndianSettlementChoice(settlement, number);
        if (act == null) return true; // Cancelled
        switch (act) {
        case SCOUT_SETTLEMENT_ATTACK:
            if (!getGUI().confirmPreCombat(unit, tile)) return true;
            askServer().attack(unit, direction);
            return false;
        case SCOUT_SETTLEMENT_SPEAK:
            // Prevent turn ending to allow speaking results to complete
            moveMode = moveMode.minimize(MoveMode.EXECUTE_GOTO_ORDERS);
            askServer().scoutSpeakToChief(unit, settlement);
            return false;
        case SCOUT_SETTLEMENT_TRIBUTE:
            return moveTribute(unit, 1, direction);
        default:
            logger.warning("showScoutIndianSettlementDialog fail: " + act);
            break;
        }
        return true;
    }

    /**
     * Spy on a foreign colony.
     *
     * @param unit The <code>Unit</code> that is spying.
     * @param direction The <code>Direction</code> of a colony to spy on.
     * @return True if the unit can move further.
     */
    private boolean moveSpy(Unit unit, Direction direction) {
        Settlement settlement = getSettlementAt(unit.getTile(), direction);
        return (settlement instanceof Colony)
            ? askServer().spy(unit, settlement)
            : false;
    }

    /**
     * Arrive at a settlement with a laden carrier following a move of
     * MoveType.ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS.
     *
     * @param unit The carrier.
     * @param direction The direction to the settlement.
     * @return True if the unit can move further.
     */
    private boolean moveTrade(Unit unit, Direction direction) {
        askClearGotoOrders(unit);

        Settlement settlement = getSettlementAt(unit.getTile(), direction);
        if (settlement instanceof Colony) {
            final Game game = getGame();
            final Player player = unit.getOwner();
            DiplomaticTrade agreement
                = new DiplomaticTrade(game, TradeContext.TRADE,
                    player, settlement.getOwner(), null, 0);
            agreement = getGUI().showNegotiationDialog(unit, settlement,
                agreement, agreement.getSendMessage(player, settlement));
            return (agreement == null
                || agreement.getStatus() == TradeStatus.REJECT_TRADE) ? true
                : moveDiplomacy(unit, direction, agreement);
        } else if (settlement instanceof IndianSettlement) {
            return moveTradeIndianSettlement(unit, direction);
        } else {
            throw new RuntimeException("Bogus settlement: "
                + settlement.getId());
        }
    }

    /**
     * Trading with the natives, including buying, selling and
     * delivering gifts.  (Deliberate use of Settlement rather than
     * IndianSettlement throughout these routines as some unification
     * with colony trading is anticipated, and the native AI already
     * uses the same DeliverGiftMessage to deliver gifts to Colonies).
     *
     * @param unit The <code>Unit</code> that is a carrier containing goods.
     * @param direction The direction the unit could move in order to enter a
     *            <code>Settlement</code>.
     * @see Settlement
     * @return True if the unit can move further.
     */
    private boolean moveTradeIndianSettlement(Unit unit, Direction direction) {
        IndianSettlement is
            = (IndianSettlement)getSettlementAt(unit.getTile(), direction);

        StringTemplate baseTemplate = StringTemplate
            .template("tradeProposition.welcome")
            .addStringTemplate("%nation%",
                is.getOwner().getNationLabel())
            .addName("%settlement%", is.getName());
        StringTemplate template = baseTemplate;
        boolean[] results = askServer()
            .openTransactionSession(unit, is);
        while (results != null) {
            // The session tracks buy/sell/gift events and disables
            // them when one happens.  So only offer such options if
            // the session allows it and the carrier is in good shape.
            boolean buy = results[0] && unit.hasSpaceLeft();
            boolean sel = results[1] && unit.hasGoodsCargo();
            boolean gif = results[2] && unit.hasGoodsCargo();
            if (!buy && !sel && !gif) break;

            TradeAction act = getGUI().getIndianSettlementTradeChoice(is,
                template, buy, sel, gif);
            if (act == null) break;
            StringTemplate t = null;
            switch (act) {
            case BUY:
                t = attemptBuyFromSettlement(unit, is);
                if (t == null) {
                    results[0] = false;
                    template = baseTemplate;
                } else {
                    template = t;
                }
                break;
            case SELL:
                t = attemptSellToSettlement(unit, is);
                if (t == null) {
                    results[1] = false;
                    template = baseTemplate;
                } else {
                    template = t;
                }
                break;
            case GIFT:
                t = attemptGiftToSettlement(unit, is);
                if (t == null) {
                    results[2] = false;
                    template = baseTemplate;
                } else {
                    template = t;
                }
                break;
            default:
                logger.warning("showIndianSettlementTradeDialog fail: "
                    + act);
                results = null;
                break;
            }
            if (template == abortTrade) template = baseTemplate;
        }

        askServer().closeTransactionSession(unit, is);
        if (unit.getMovesLeft() > 0) getGUI().setActiveUnit(unit); // No trade?
        return false;
    }

    /**
     * Displays an appropriate trade failure message.
     *
     * @param fail The failure state.
     * @param settlement The <code>Settlement</code> that failed to trade.
     * @param goods The <code>Goods</code> that failed to trade.
     * @return A <code>StringTemplate</code> describing the failure.
     */
    private StringTemplate tradeFailMessage(int fail, Settlement settlement,
                                            Goods goods) {
        switch (fail) {
        case NO_TRADE_GOODS:
            return StringTemplate.template("trade.noTradeGoods")
                .addNamed("%goods%", goods);
        case NO_TRADE_HAGGLE:
            return StringTemplate.template("trade.noTradeHaggle");
        case NO_TRADE_HOSTILE:
            return StringTemplate.template("trade.noTradeHostile");
        case NO_TRADE: // Proposal was refused
        default:
            break;
        }
        return StringTemplate.template("trade.noTrade")
            .addStringTemplate("%settlement%",
                settlement.getLocationLabelFor(getMyPlayer()));
    }

    /**
     * User interaction for buying from the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param is The <code>Settlement</code> that is trading.
     * @return A <code>StringTemplate</code> containing a message if
     *     there is problem, or null on success.
     */
    private StringTemplate attemptBuyFromSettlement(Unit unit,
                                                    IndianSettlement is) {
        final Game game = getGame();
        Player player = getMyPlayer();
        Goods goods = null;

        // Get list of goods for sale
        if (!askServer().getGoodsForSaleInSettlement(unit, is)) {
            return StringTemplate.template("trade.nothingToSell");
        }
        List<Goods> forSale = is.getGoodsForSale();
        for (;;) {
            if (forSale.isEmpty()) { // Nothing to sell to the player
                return StringTemplate.template("trade.nothingToSell");
            }

            // Choose goods to buy
            goods = getGUI().getChoice(unit.getTile(),
                Messages.message("buyProposition.text"), is, "nothing",
                toList(map(forSale,
                        g -> new ChoiceItem<>(Messages.message(g.getLabel()), g))));
            if (goods == null) break; // Trade aborted by the player

            int gold = -1; // Initially ask for a price
            for (;;) {
                gold = askServer().buyProposition(unit, is, goods, gold);
                if (gold <= 0) {
                    return tradeFailMessage(gold, is, goods);
                }

                // Show dialog for buy proposal
                boolean canBuy = player.checkGold(gold);
                TradeBuyAction act
                    = getGUI().getBuyChoice(unit, is, goods, gold, canBuy);
                if (act == null) break; // User cancelled
                switch (act) {
                case BUY: // Accept price, make purchase
                    return (askServer().buyFromSettlement(unit,
                            is, goods, gold)) ? null
                        : abortTrade;
                case HAGGLE: // Try to negotiate a lower price
                    gold = gold * 9 / 10;
                    break;
                default:
                    logger.warning("showBuyDialog fail: " + act);
                    return null;
                }
            }
        }
        return abortTrade;
    }

    /**
     * User interaction for selling to the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param is The <code>IndianSettlement</code> that is trading.
     * @return A <code>StringTemplate</code> containing a message if
     *     there is problem, or null on success.
     */
    private StringTemplate attemptSellToSettlement(Unit unit,
                                                   IndianSettlement is) {
        Goods goods = null;
        for (;;) {
            // Choose goods to sell
            goods = getGUI().getChoice(unit.getTile(),
                Messages.message("sellProposition.text"), is, "nothing",
                toList(map(unit.getGoodsList(), g ->
                        new ChoiceItem<>(Messages.message(g.getLabel(true)), g))));
            if (goods == null) break; // Trade aborted by the player

            int gold = -1; // Initially ask for a price
            for (;;) {
                gold = askServer().sellProposition(unit, is,
                                                   goods, gold);
                if (gold <= 0) {
                    return tradeFailMessage(gold, is, goods);
                }

                // Show dialog for sale proposal
                TradeSellAction act
                    = getGUI().getSellChoice(unit, is, goods, gold);
                if (act == null) break; // Cancelled
                switch (act) {
                case SELL: // Accepted price, make the sale
                    return (askServer().sellToSettlement(unit, is, goods, gold))
                        ? null
                        : abortTrade;
                case HAGGLE: // Ask for more money
                    gold = (gold * 11) / 10;
                    break;
                case GIFT: // Decide to make a gift of the goods
                    askServer().deliverGiftToSettlement(unit,
                        is, goods);
                    return abortTrade;
                default:
                    logger.warning("showSellDialog fail: " + act);
                    return null;
                }
            }
        }
        return abortTrade;
    }

    /**
     * User interaction for delivering a gift to the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param is The <code>IndianSettlement</code> that is trading.
     * @return A <code>StringTemplate</code> containing a message if
     *     there is problem, or null on success.
     */
    private StringTemplate attemptGiftToSettlement(Unit unit,
                                                   IndianSettlement is) {
        Goods goods = getGUI().getChoice(unit.getTile(),
            Messages.message("gift.text"), is, "cancel",
            toList(map(unit.getGoodsList(), g ->
                    new ChoiceItem<>(Messages.message(g.getLabel(true)), g))));
        return (goods != null
            && askServer().deliverGiftToSettlement(unit, is, goods))
            ? null
            : abortTrade;
    }

    /**
     * Demand a tribute.
     *
     * @param unit The <code>Unit</code> to perform the attack.
     * @param amount An amount of tribute to demand.
     * @param direction The direction in which to attack.
     * @return True if the unit can move further.
     */
    private boolean moveTribute(Unit unit, int amount, Direction direction) {
        final Game game = getGame();
        Player player = unit.getOwner();
        Tile tile = unit.getTile();
        Tile target = tile.getNeighbourOrNull(direction);
        Settlement settlement = target.getSettlement();
        Player other = settlement.getOwner();

        // Indians are easy and can use the basic tribute mechanism.
        if (settlement.getOwner().isIndian()) {
            askServer().demandTribute(unit, direction);
            return false;
        }
        
        // Europeans might be human players, so we convert to a diplomacy
        // dialog.
        DiplomaticTrade agreement
            = new DiplomaticTrade(game, TradeContext.TRIBUTE, player, other,
                                  null, 0);
        agreement.add(new StanceTradeItem(game, player, other, Stance.PEACE));
        agreement.add(new GoldTradeItem(game, other, player, amount));
        return moveDiplomacy(unit, direction, agreement);
    }

    /**
     * Move a missionary into a native settlement, following a move of
     * MoveType.ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY.
     *
     * @param unit The <code>Unit</code> that will enter the settlement.
     * @param direction The direction in which the Indian settlement lies.
     * @return True if the unit can move further.
     */
    private boolean moveUseMissionary(Unit unit, Direction direction) {
        IndianSettlement settlement
            = (IndianSettlement)getSettlementAt(unit.getTile(), direction);
        Player player = unit.getOwner();
        boolean canEstablish = !settlement.hasMissionary();
        boolean canDenounce = !canEstablish
            && !settlement.hasMissionary(player);
        askClearGotoOrders(unit);

        // Offer the choices.
        MissionaryAction act = getGUI().getMissionaryChoice(unit, settlement,
            canEstablish, canDenounce);
        if (act == null) return true;
        switch (act) {
        case MISSIONARY_ESTABLISH_MISSION: case MISSIONARY_DENOUNCE_HERESY:
            if (askServer().missionary(unit, direction,
                    act == MissionaryAction.MISSIONARY_DENOUNCE_HERESY)
                && settlement.hasMissionary(player)) {
                sound("sound.event.missionEstablished");
                player.invalidateCanSeeTiles();
            }
            break;
        case MISSIONARY_INCITE_INDIANS:
            Player enemy = getGUI().getChoice(unit.getTile(),
                Messages.message("missionarySettlement.inciteQuestion"),
                unit, "missionarySettlement.cancel",
                toList(map(getGame().getLiveEuropeanPlayers(player), p ->
                        new ChoiceItem<>(Messages.message(p.getCountryLabel()), p))));
            if (enemy == null) return true;
            askServer().incite(unit, settlement, enemy, -1);
            break;
        default:
            logger.warning("showUseMissionaryDialog fail");
            break;
        }
        return false;
    }


    // Trade route support.

    /**
     * Follows a trade route, doing load/unload actions, moving the unit,
     * and updating the stop and destination.
     *
     * @param unit The <code>Unit</code> on the route.
     * @param messages An optional list in which to retain any
     *     <code>ModelMessage</code>s generated.
     * @return True if the unit should keep moving, which can only
     *     happen if the trade route is found to be broken and the
     *     unit is thrown off it.
     */
    private boolean followTradeRoute(Unit unit, List<ModelMessage> messages) {
        final Player player = unit.getOwner();
        final TradeRoute tr = unit.getTradeRoute();
        final boolean detailed = getClientOptions()
            .getBoolean(ClientOptions.SHOW_GOODS_MOVEMENT);
        final boolean checkProduction = getClientOptions()
            .getBoolean(ClientOptions.STOCK_ACCOUNTS_FOR_PRODUCTION);
        final List<TradeRouteStop> stops = unit.getCurrentStops();
        boolean result = false;

        // If required, accumulate a summary of all the activity of
        // this unit on its trade route.
        LogBuilder lb = new LogBuilder((detailed && !tr.isSilent()) ? 256
            : -1);
        lb.mark();

        // Validate the whole route.
        boolean valid = true;
        for (TradeRouteStop trs : stops) {
            if (!TradeRoute.isStopValid(unit, trs)) {
                lb.add(" ", Messages.message(trs.invalidStopLabel(player)));
                valid = false;
            }
        }
        if (!valid) {
            clearOrders(unit);
            stops.clear();
            result = unit.getMovesLeft() > 0;
        }

        // Try to find work to do on the current list of stops.
        while (!stops.isEmpty()) {
            TradeRouteStop stop = stops.remove(0);

            if (!unit.atStop(stop)) {
                // Not at stop, give up if no moves left or the path was
                // exhausted on a previous round.
                if (unit.getMovesLeft() <= 0
                    || unit.getState() == UnitState.SKIPPED) {
                    lb.add(" ", Messages.message(stop
                            .getLabelFor("tradeRoute.toStop", player)));
                    break;
                }

                // Find a path to the stop, skip if none.
                Location destination = stop.getLocation();
                PathNode path = unit.findPath(destination);
                if (path == null) {
                    lb.add("\n", Messages.message(stop
                            .getLabelFor("tradeRoute.pathStop", player)));
                    unit.setState(UnitState.SKIPPED);
                    break;
                }
                
                // Try to follow the path.  If the unit does not reach
                // the stop it is finished for now.
                movePath(unit, path);
                if (!unit.atStop(stop)) {
                    unit.setState(UnitState.SKIPPED);
                    break;
                }
            }

            // At the stop, do the work available.
            lb.mark();
            unloadUnitAtStop(unit, lb); // Anything to unload?
            loadUnitAtStop(unit, lb); // Anything to load?
            lb.grew("\n", Messages.message(stop.getLabelFor("tradeRoute.atStop",
                                                            player)));

            // If the un/load consumed the moves, break now before
            // updating the stop.  This allows next turn to retry
            // un/loading, but this time it will not consume the moves.
            if (unit.getMovesLeft() <= 0) break;

            // Find the next stop with work to do.
            TradeRouteStop next = null;
            List<TradeRouteStop> moreStops = unit.getCurrentStops();
            if (unit.atStop(moreStops.get(0))) moreStops.remove(0);
            for (TradeRouteStop trs : moreStops) {
                if (trs.hasWork(unit, (!checkProduction) ? 0
                                : unit.getTurnsToReach(trs.getLocation()))) {
                    next = trs;
                    break;
                }
            }
            if (next == null) {
                // No work was found anywhere on the trade route,
                // so we should skip this unit.
                lb.add(" ", Messages.message("tradeRoute.wait"));
                unit.setState(UnitState.SKIPPED);
                unit.setMovesLeft(0);
                break;
            }
            // Add a message for any skipped stops.
            List<TradeRouteStop> skipped
                = tr.getStopSublist(stops.get(0), next);
            if (!skipped.isEmpty()) {
                StringTemplate t = StringTemplate.label("")
                    .add("tradeRoute.skipped");
                String sep = " ";
                for (TradeRouteStop trs : skipped) {
                    t.addName(sep)
                        .addStringTemplate(trs.getLocation()
                            .getLocationLabelFor(player));
                    sep = ", ";
                }
                t.addName(".");
                lb.add(" ", Messages.message(t));
            }
            // Bring the next stop to the head of the stops list if it
            // is present.
            while (!stops.isEmpty() && stops.get(0) != next) {
                stops.remove(0);
            }
            // Set the new stop, skip on error.
            if (!askServer().setCurrentStop(unit, tr.getIndex(next))) {
                unit.setState(UnitState.SKIPPED);
                break;
            }
        }

        if (lb.grew()) {
            ModelMessage m = new ModelMessage(MessageType.GOODS_MOVEMENT,
                                              "tradeRoute.prefix", unit)
                .addName("%route%", tr.getName())
                .addStringTemplate("%unit%",
                    unit.getLabel(Unit.UnitLabelType.NATIONAL))
                .addName("%data%", lb.toString());
            if (messages != null) {
                messages.add(m);
            } else {
                player.addModelMessage(m);
                turnReportMessages.add(m);
            }
        }
        return result;
    }

    /**
     * Work out what goods to load onto a unit at a stop, and load them.
     *
     * @param unit The <code>Unit</code> to load.
     * @param lb A <code>LogBuilder</code> to update.
     * @return True if goods were loaded.
     */
    private boolean loadUnitAtStop(Unit unit, LogBuilder lb) {
        final boolean enhancedTradeRoutes = getSpecification()
            .getBoolean(GameOptions.ENHANCED_TRADE_ROUTES);
        final TradeRoute tradeRoute = unit.getTradeRoute();
        final TradeLocation trl = unit.getTradeLocation();
        if (trl == null) return false;

        final TradeRouteStop stop = unit.getStop();
        boolean ret = false;

        // A collapsed list of goods to load at this stop.
        List<AbstractGoods> toLoad = stop.getCompactCargo();
        // Templates to accumulate messages in.
        StringTemplate unexpected = StringTemplate.label(", ");
        StringTemplate noLoad = StringTemplate.label(", ");
        StringTemplate left = StringTemplate.label(", ");
        StringTemplate loaded = StringTemplate.label(", ");
        StringTemplate nonePresent = StringTemplate.label(", ");
        
        // Check the goods already on board.  If it is not expected to
        // be loaded at this stop then complain (unload must have
        // failed somewhere).  If it is expected to load, reduce the
        // loading amount by what is already on board.
        for (Goods g : unit.getCompactGoods()) {
            AbstractGoods ag = AbstractGoods.findByType(g.getType(), toLoad);
            if (ag == null) { // Excess goods on board, failed unload?
                unexpected.addStringTemplate("%goods%", ag.getLabel());
            } else {
                int goodsAmount = g.getAmount();
                if (ag.getAmount() <= goodsAmount) { // At capacity
                    noLoad.addStringTemplate(StringTemplate
                        .template("tradeRoute.loadStop.noLoad.carrier")
                            .addNamed("%goodsType%", ag.getType()));
                    toLoad.remove(ag);
                } else {
                    ag.setAmount(ag.getAmount() - goodsAmount);
                }
            }
        }

        // Adjust toLoad with the actual amount to load.
        // Drop goods that are:
        // - missing
        // - do not have an export surplus
        // - (optionally) are not needed by the destination
        // and add messages for them.
        //
        // Similarly, for each goods type, add an entry to the limit
        // map, with value:
        // - the unit, when it lacks capacity for all the goods present
        // - the stop when there is a non-zero export limit
        // - (optionally) the destination stop when there is a non-zero
        //   import limit
        // - otherwise null
        java.util.Map<GoodsType, Location> limit = new HashMap<>();
        Iterator<AbstractGoods> iterator = toLoad.iterator();
        while (iterator.hasNext()) {
            AbstractGoods ag = iterator.next();
            final GoodsType type = ag.getType();
            int present = stop.getGoodsCount(type);
            int exportAmount = stop.getExportAmount(type, 0);
            int importAmount = FreeColObject.INFINITY;
            TradeRouteStop unload = null;
            if (enhancedTradeRoutes) {
                final List<TradeRouteStop> stops = unit.getCurrentStops();
                stops.remove(0);
                Location start = unit.getLocation();
                int turns = 0;
                for (TradeRouteStop trs : stops) {
                    turns += unit.getTurnsToReach(start, trs.getLocation());
                    int amountIn = trs.getImportAmount(type, turns),
                        amountOut = trs.getExportAmount(type, turns);
                    if (AbstractGoods.findByType(type, trs.getCompactCargo()) == null
                        || amountIn > amountOut) {
                        importAmount = amountIn;
                        unload = trs;
                        break;
                    }
                    start = trs.getLocation();
                }
            }
            if (enhancedTradeRoutes && unload == null) {
                noLoad.addStringTemplate(StringTemplate
                    .template("tradeRoute.loadStop.noLoad.noUnload")
                        .addNamed("%goodsType%", type));
                ag.setAmount(0);
            } else if (present <= 0) { // None present
                nonePresent.addNamed(type);
                ag.setAmount(0);
            } else if (exportAmount <= 0) { // Export blocked
                noLoad.addStringTemplate(StringTemplate
                    .template("tradeRoute.loadStop.noLoad.export")
                        .addNamed("%goodsType%", type)
                        .addAmount("%more%", present));
                ag.setAmount(0);
            } else if (importAmount <= 0) { // Import blocked
                noLoad.addStringTemplate(StringTemplate
                    .template("tradeRoute.loadStop.noLoad.import")
                        .addNamed("%goodsType%", type)
                        .addAmount("%more%", present)
                        .addStringTemplate("%location%", unload.getLocation()
                            .getLocationLabelFor(unit.getOwner())));
                ag.setAmount(0);
            } else if (exportAmount < ag.getAmount() // Export limited
                && exportAmount <= importAmount) {
                ag.setAmount(exportAmount);
                limit.put(type, stop.getLocation());
            } else if (importAmount < ag.getAmount() // Import limited
                && importAmount <= exportAmount) {
                int already = unit.getGoodsCount(type);
                if (already >= importAmount) {
                    if (already > importAmount) {
                        askUnloadGoods(type, already - importAmount, unit);
                    }
                    noLoad.addStringTemplate(StringTemplate
                        .template("tradeRoute.loadStop.noLoad.already")
                            .addNamed("%goodsType%", type));
                    ag.setAmount(0);
                } else {
                    ag.setAmount(importAmount - already);
                }
                limit.put(type, unload.getLocation());
            } else if (present > ag.getAmount()) { // Carrier limited (last!)
                limit.put(type, unit);
            } else { // Expecting to load everything present
                limit.put(type, null);
            }

            // Do not load this goods type
            if (ag.getAmount() <= 0) iterator.remove();

            logger.log(Level.FINEST, "Load " + tradeRoute.getName()
                + " with " + unit.getId() + " at " + stop.getLocation()
                + " of " + type.getSuffix() + " from " + present
                + " exporting " + exportAmount + " importing " + importAmount
                + " to " + ((unload == null) ? "?"
                    : unload.getLocation().toString())
                + " limited by " + limit.get(type)
                + " -> " + ag.getAmount());
        }

        if (enhancedTradeRoutes) { // Prioritize by goods amount
            Collections.sort(toLoad, AbstractGoods.abstractGoodsComparator);
        }
        
        // Load the goods.
        boolean done = false;
        for (AbstractGoods ag : toLoad) {
            final GoodsType type = ag.getType();
            final int amount = ag.getAmount();
            if (!done) {
                done = unit.getLoadableAmount(type) < amount
                    || !askLoadGoods(stop.getLocation(), type, amount, unit);
            }
            if (done) {
                left.addNamed(ag);
                continue;
            }
            int present = stop.getGoodsCount(type);
            Location why = limit.get(type);
            if (present == 0) {
                loaded.addStringTemplate(ag.getLabel());
            } else if (why == null) {
                loaded.addStringTemplate(StringTemplate
                    .template("tradeRoute.loadStop.load.fail")
                        .addStringTemplate("%goods%", ag.getLabel())
                        .addAmount("%more%", present));
            } else if (why == unit) {
                loaded.addStringTemplate(StringTemplate
                    .template("tradeRoute.loadStop.load.carrier")
                        .addStringTemplate("%goods%", ag.getLabel())
                        .addAmount("%more%", present));
            } else if (Map.isSameLocation(why, stop.getLocation())) {
                loaded.addStringTemplate(StringTemplate
                    .template("tradeRoute.loadStop.load.export")
                    .addStringTemplate("%goods%", ag.getLabel())
                    .addAmount("%more%", present));
            } else {
                loaded.addStringTemplate(StringTemplate
                    .template("tradeRoute.loadStop.load.import")
                    .addStringTemplate("%goods%", ag.getLabel())
                    .addAmount("%more%", present)
                    .addStringTemplate("%location%",
                        why.getLocationLabelFor(unit.getOwner())));
            }
            ret = true;
        }
        if (!loaded.isEmpty()) {
            lb.add("\n  ", Messages.message(StringTemplate
                    .template("tradeRoute.loadStop.load")
                        .addStringTemplate("%goodsList%", loaded)));
        }
        if (!unexpected.isEmpty()) {
            lb.add("\n  ", Messages.message(StringTemplate
                    .template("tradeRoute.loadStop.unexpected")
                        .addStringTemplate("%goodsList%", unexpected)));
        }
        if (!left.isEmpty()) {
            noLoad.addStringTemplate(StringTemplate
                .template("tradeRoute.loadStop.noLoad.left")
                    .addStringTemplate("%goodsList%", left));
        }
        if (!nonePresent.isEmpty()) {
            noLoad.addStringTemplate(StringTemplate
                .template("tradeRoute.loadStop.noLoad.goods")
                    .addStringTemplate("%goodsList%", nonePresent));
        }
        if (!noLoad.isEmpty()) {
            lb.add("\n  ", Messages.message(StringTemplate
                    .template("tradeRoute.loadStop.noLoad")
                        .addStringTemplate("%goodsList%", noLoad)));
        }
        return ret;
    }

    /**
     * Work out what goods to unload from a unit at a stop, and unload them.
     *
     * @param unit The <code>Unit</code> to unload.
     * @param lb A <code>LogBuilder</code> to update.
     * @return True if something was unloaded.
     */
    private boolean unloadUnitAtStop(Unit unit, LogBuilder lb) {
        final TradeLocation trl = unit.getTradeLocation();
        if (trl == null) return false;

        final TradeRouteStop stop = unit.getStop();
        final List<GoodsType> goodsTypesToLoad = stop.getCargo();
        final StringTemplate unloaded = StringTemplate.label(", ");
        final StringTemplate noUnload = StringTemplate.label(", ");
        boolean ret = false;

        // Unload everything that is on the carrier but not listed to
        // be loaded at this stop.
        Game game = getGame();
        for (Goods goods : unit.getCompactGoodsList()) {
            GoodsType type = goods.getType();
            if (goodsTypesToLoad.contains(type)) continue; // Keep this cargo.
            int present = goods.getAmount();
            if (present <= 0) {
                logger.warning("Unexpected empty goods unload " + goods);
                continue;
            }
            int toUnload = present;
            int atStop = trl.getImportAmount(type, 0);
            int amount = toUnload;
            if (amount > atStop) {
                StringTemplate locName = ((Location)trl).getLocationLabel();
                int option = getClientOptions()
                    .getInteger(ClientOptions.UNLOAD_OVERFLOW_RESPONSE);
                switch (option) {
                case ClientOptions.UNLOAD_OVERFLOW_RESPONSE_ASK:
                    StringTemplate template = StringTemplate
                        .template("traderoute.warehouseCapacity")
                        .addStringTemplate("%unit%",
                            unit.getLabel(Unit.UnitLabelType.NATIONAL))
                        .addStringTemplate("%colony%", locName)
                        .addAmount("%amount%", toUnload - atStop)
                        .addNamed("%goods%", goods);
                    if (!getGUI().confirm(unit.getTile(), template,
                                     unit, "yes", "no")) {
                        if (atStop == 0) continue;
                        amount = atStop;
                    }
                    break;
                case ClientOptions.UNLOAD_OVERFLOW_RESPONSE_NEVER:
                    amount = atStop;
                    break;
                case ClientOptions.UNLOAD_OVERFLOW_RESPONSE_ALWAYS:
                    break;
                default:
                    logger.warning("Illegal UNLOAD_OVERFLOW_RESPONSE: "
                        + Integer.toString(option));
                    break;
                }
            }
            if (amount == 0) {
                noUnload.addStringTemplate(goods.getLabel());
                continue;
            }
            // Try to unload.
            ret = askUnloadGoods(type, amount, unit);
            int retained = unit.getGoodsCount(type);
            if (!ret || present == retained) {
                noUnload.addStringTemplate(StringTemplate
                    .template("tradeRoute.unloadStop.noUnload.fail")
                    .addStringTemplate("%goods%", goods.getLabel()));
                ret = false;
                break;
            }
            if (present - retained != amount) {
                unloaded.addStringTemplate(StringTemplate
                    .template("tradeRoute.unloadStop.unload.fail")
                    .addNamed("%goodsType%", type)
                    .addAmount("%amount%", amount)
                    .addAmount("%more%", retained));
            } else if (amount > atStop) {
                if (retained > 0) {
                    unloaded.addStringTemplate(StringTemplate
                        .template("tradeRoute.unloadStop.unload.keep")
                        .addNamed("%goodsType%", type)
                        .addAmount("%amount%", atStop)
                        .addAmount("%more%", retained));
                } else {
                    unloaded.addStringTemplate(StringTemplate
                        .template("tradeRoute.unloadStop.unload.overflow")
                        .addNamed("%goodsType%", type)
                        .addAmount("%amount%", atStop)
                        .addAmount("%more%", amount - atStop));
                }
            } else {
                unloaded.addStringTemplate(goods.getLabel());
            }
        }
        if (!unloaded.isEmpty()) {
            lb.add("\n  ", Messages.message(StringTemplate
                    .template("tradeRoute.unloadStop.unload")
                        .addStringTemplate("%goodsList%", unloaded)));
        }
        if (!noUnload.isEmpty()) {
            lb.add("\n  ", Messages.message(StringTemplate
                    .template("tradeRoute.unloadStop.noUnload")
                        .addStringTemplate("%goodsList%", noUnload)));
        }

        return ret;
    }

    /**
     * Gets a message describing a goods unloading.
     *
     * Normally just state that a certain amount of goods was
     * unloaded.  Make special mention if the actual unloaded amount
     * was short (unloaded &lt; amount), or an overflow is happening
     * (amount &gt; atStop) in which case distinguish dumping (amount
     * == toUnload) from retaining on board).
     *
     * @param unit The <code>Unit</code> that is unloading.
     * @param type The <code>GoodsType</code> the type of goods being unloaded.
     * @param amount The amount of goods requested to be unloaded.
     * @param present The amount of goods originally on the unit.
     * @param atStop The amount of goods space available at the stop.
     * @param toUnload The amount of goods that should be unloaded according
     *     to the trade route orders.
     * @return A summary of the unload.
     */
    private String getUnloadGoodsMessage(Unit unit, GoodsType type,
                                         int amount, int present,
                                         int atStop, int toUnload) {
        String key = null;
        int onBoard = unit.getGoodsCount(type);
        int unloaded = present - onBoard;
        int more = 0;

        if (unloaded < amount) {
            // Tried to unload %amount% %goods%, but %more% was unloaded
            key = "tradeRoute.unloadStopFail";
            more = unloaded;
        } else if (amount > atStop) {
            if (amount == toUnload) {
                // Unloaded %amount% %goods% and dumped %more%.
                key = "tradeRoute.unloadStopImport";
                more = toUnload - atStop;
            } else {
                // Unloaded %amount% %goods% with %more% more retained...
                key = (amount == 0) ? "tradeRoute.unloadStopNoExport"
                    : "tradeRoute.unloadStopExport";
                more = onBoard;
            }
        } else {
            // Unloaded %amount% %goods%
            key = "tradeRoute.unloadStop";
        }

        return Messages.message(StringTemplate.template(key)
            .addAmount("%amount%", amount)
            .addAmount("%more%", more)
            .addNamed("%goods%", type));
    }


    // Routines from here on are mostly user commands.  That is they
    // are called directly as a result of keyboard, menu, mouse or
    // panel/dialog actions.  Some though are called indirectly after
    // a call to the server routes information back through the
    // InGameInputHandler.  They should all be annotated as such to
    // confirm where they can come from.
    //
    // User command all return a success/failure indication, except if
    // the game is stopped.  IGIH-initiated routines do not need to.
    //
    // Successfully executed commands should update the GUI.

    /**
     * Abandon a colony with no units.
     *
     * Called from ColonyPanel.closeColonyPanel
     *
     * @param colony The <code>Colony</code> to be abandoned.
     * @return True if the colony was abandoned.
     */
    public boolean abandonColony(Colony colony) {
        final Player player = getMyPlayer();
        if (!requireOurTurn() || colony == null
            || !player.owns(colony) || colony.getUnitCount() > 0)
            return false;

        // Proceed to abandon
        final Tile tile = colony.getTile();
        boolean ret = askServer().abandonColony(colony)
            && !tile.hasSettlement();
        if (ret) {
            player.invalidateCanSeeTiles();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Animate an attack.
     *
     * Called from IGIH.animateAttack.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @param attackerTile The <code>Tile</code> the attack originates from.
     * @param defenderTile The <code>Tile</code> the defence takes place on.
     * @param success True if the attack succeeds.
     */
    public void animateAttack(Unit attacker, Unit defender,
                              Tile attackerTile, Tile defenderTile,
                              boolean success) {
        // Note: we used to focus the map on the unit even when
        // animation is off as long as the center-active-unit option
        // was set.  However IR#115 requested that if animation is off
        // that we display nothing so as to speed up the other player
        // moves as much as possible.
        if (getFreeColClient().getAnimationSpeed(attacker.getOwner()) > 0) {
            getGUI().animateUnitAttack(attacker, defender,
                                  attackerTile, defenderTile, success);
        }
        getGUI().refresh();
    }

    /**
     * Animate a move.
     *
     * Called from IGIH.animateMove.
     *
     * @param unit The <code>Unit</code> that moves.
     * @param oldTile The <code>Tile</code> the move begins at.
     * @param newTile The <code>Tile</code> the move ends at.
     */
    public void animateMove(Unit unit, Tile oldTile, Tile newTile) {
        // Note: we used to focus the map on the unit even when
        // animation is off as long as the center-active-unit option
        // was set.  However IR#115 requested that if animation is off
        // that we display nothing so as to speed up the other player
        // moves as much as possible.
        if (getFreeColClient().getAnimationSpeed(unit.getOwner()) > 0) {
            getGUI().animateUnitMove(unit, oldTile, newTile);
        } else if (getMyPlayer().owns(unit)) {
            getGUI().requireFocus(newTile);
        }
        getGUI().refresh();
    }

    /**
     * Assigns a student to a teacher.
     *
     * Called from UnitLabel
     *
     * @param student The student <code>Unit</code>.
     * @param teacher The teacher <code>Unit</code>.
     * @return True if the student was assigned.
     */
    public boolean assignTeacher(Unit student, Unit teacher) {
        final Player player = getMyPlayer();
        if (!requireOurTurn()
            || student == null
            || !player.owns(student)
            || student.getColony() == null
            || !student.isInColony()
            || teacher == null
            || !player.owns(teacher)
            || !student.canBeStudent(teacher)
            || teacher.getColony() == null
            || student.getColony() != teacher.getColony()
            || !teacher.getColony().canTrain(teacher))
            return false;

        UnitWas unitWas = new UnitWas(student);
        boolean ret = askServer().assignTeacher(student, teacher)
            && student.getTeacher() == teacher;
        if (ret) {
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Assigns a trade route to a unit.
     *
     * Called from EuropePanel.DestinationPanel, TradeRoutePanel(),
     * TradeRoutePanel.newRoute
     *
     * @param unit The <code>Unit</code> to assign a trade route to.
     * @param tradeRoute The <code>TradeRoute</code> to assign.
     * @return True if the route was successfully assigned.
     */
    public boolean assignTradeRoute(Unit unit, TradeRoute tradeRoute) {
        if (unit == null) return false;

        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askAssignTradeRoute(unit, tradeRoute);
        if (ret) {
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Boards a specified unit onto a carrier.
     * The carrier must be at the same location as the boarding unit.
     *
     * Called from CargoPanel, TilePopup.
     *
     * @param unit The <code>Unit</code> which is to board the carrier.
     * @param carrier The carrier to board.
     * @return True if the unit boards the carrier.
     */
    public boolean boardShip(Unit unit, Unit carrier) {
        if (!requireOurTurn() || unit == null || unit.isCarrier()
            || carrier == null || !carrier.canCarryUnits()
            || !unit.isAtLocation(carrier.getLocation())) return false;

        boolean ret = askEmbark(unit, carrier);
        if (ret) {
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Use the active unit to build a colony.
     *
     * Called from BuildColonyAction.
     *
     * @param unit The <code>Unit</code> to build the colony.
     * @return True if a colony was built.
     */
    public boolean buildColony(Unit unit) {
        if (!requireOurTurn() || unit == null) return false;

        // Check unit, which must be on the map and able to build.
        if (unit == null) return false;
        final Tile tile = unit.getTile();
        if (tile == null) return false;
        if (!unit.canBuildColony()) {
            getGUI().showInformationMessage(unit, StringTemplate
                .template("buildColony.badUnit")
                .addName("%unit%", unit.getName()));
            return false;
        }

        // Join existing colony if present
        if (joinColony(unit) || tile.getColony() != null) return false;

        // Check for other impediments.
        final Player player = getMyPlayer();
        NoClaimReason reason = player.canClaimToFoundSettlementReason(tile);
        switch (reason) {
        case NONE:
        case NATIVES: // Tile can still be claimed
            break;
        default:
            getGUI().showInformationMessage(reason.getDescriptionKey());
            return false;
        }

        // Show the warnings if applicable.
        if (getClientOptions().getBoolean(ClientOptions.SHOW_COLONY_WARNINGS)) {
            StringTemplate warnings = tile.getBuildColonyWarnings(unit);
            if (!warnings.getReplacements().isEmpty()
                && !getGUI().confirm(tile, warnings,
                                unit, "buildColony.yes", "buildColony.no")) {
                return false;
            }
        }

        // Get and check the name.
        String name = getGUI().getNewColonyName(player, tile);
        if (name == null) return false;

        // Claim tile from other owners before founding a settlement.
        // Only native owners that we can steal, buy from, or use a
        // bonus center tile exception should be possible by this point.
        UnitWas unitWas = new UnitWas(unit);
        boolean ret = player.owns(tile);
        if (!ret) {
            ret = askClaimTile(player, tile, unit, player.getLandPrice(tile));
            if (!ret) NameCache.putSettlementName(player, name);
        }            
        if (ret) {
            ret = askServer().buildColony(name, unit)
                && tile.hasSettlement();
            if (ret) {
                sound("sound.event.buildingComplete");
                player.invalidateCanSeeTiles();
                unitWas.fireChanges();
                // Check units present for treasure cash-in as they are now
                // at a colony.
                for (Unit u : tile.getUnitList()) checkCashInTreasureTrain(u);
                colonyPanel((Colony)tile.getSettlement(), unit);
            }
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Buy goods in Europe.
     * The amount of goods is adjusted to the space in the carrier.
     *
     * Called from CargoPanel, TilePopup, loadCargo()
     *
     * @param type The type of goods to buy.
     * @param amount The amount of goods to buy.
     * @param carrier The <code>Unit</code> acting as carrier.
     * @return True if the purchase succeeds.
     */
    public boolean buyGoods(GoodsType type, int amount, Unit carrier) {
        if (!requireOurTurn() || type == null || amount <= 0
            || carrier == null
            || !carrier.isInEurope()
            || !getMyPlayer().owns(carrier)) return false;

        final Europe europe = carrier.getOwner().getEurope();
        EuropeWas europeWas = new EuropeWas(europe);
        UnitWas unitWas = new UnitWas(carrier);
        boolean ret = askLoadGoods(europe, type, amount, carrier);
        if (ret) {
            sound("sound.event.loadCargo");
            europeWas.fireChanges();
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Chat with another player.
     *
     * Called from IGIH.chat.
     *
     * @param player The <code>Player</code> to chat with.
     * @param message What to say.
     * @param pri If true, the message is private.
     */
    public void chat(Player player, String message, boolean pri) {
        getGUI().displayChatMessage(player, message, pri);
    }

    /**
     * Changes the state of this <code>Unit</code>.
     *
     * Called from FortifyAction, SentryAction, TilePopup, UnitLabel
     *
     * @param unit The <code>Unit</code>
     * @param state The state of the unit.
     * @return True if the state was changed.
     */
    public boolean changeState(Unit unit, UnitState state) {
        if (!requireOurTurn() || unit == null) return false;
        if (unit.getState() == state) return true;
        if (!unit.checkSetState(state)) return false;

        // Check if this is a hostile fortification, and give the player
        // a chance to confirm.
        final Player player = getMyPlayer();
        if (state == UnitState.FORTIFYING && unit.isOffensiveUnit()
            && !unit.hasAbility(Ability.PIRACY)) {
            Tile tile = unit.getTile();
            if (tile != null && tile.getOwningSettlement() != null) {
                Player enemy = tile.getOwningSettlement().getOwner();
                if (player != enemy
                    && player.getStance(enemy) != Stance.ALLIANCE
                    && !getGUI().confirmHostileAction(unit, tile))
                    return false; // Aborted
            }
        }

        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askServer().changeState(unit, state)
            && unit.getState() == state;
        if (ret) {
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Changes the work type of this <code>Unit</code>.
     *
     * Called from ImprovementAction.
     *
     * @param unit The <code>Unit</code>
     * @param improvementType a <code>TileImprovementType</code> value
     * @return True if the improvement was changed.
     */
    public boolean changeWorkImprovementType(Unit unit,
        TileImprovementType improvementType) {
        if (!requireOurTurn() || unit == null || improvementType == null
            || !unit.hasTile()
            || !unit.checkSetState(UnitState.IMPROVING)
            || improvementType.isNatural()) return false;

        // May need to claim the tile first
        final Player player = getMyPlayer();
        final Tile tile = unit.getTile();
        UnitWas unitWas = new UnitWas(unit);
        boolean ret = player.owns(tile)
            || askClaimTile(player, tile, unit, player.getLandPrice(tile));
        if (ret) {
            ret = askServer()
                .changeWorkImprovementType(unit, improvementType)
                && unit.getWorkImprovement() != null
                && unit.getWorkImprovement().getType() == improvementType;
            if (ret) {
                unitWas.fireChanges();
            }
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Changes the work type of this <code>Unit</code>.
     *
     * Called from ColonyPanel.tryWork, UnitLabel
     *
     * @param unit The <code>Unit</code>
     * @param workType The new <code>GoodsType</code> to produce.
     * @return True if the work type was changed.
     */
    public boolean changeWorkType(Unit unit, GoodsType workType) {
        if (!requireOurTurn() || unit == null) return false;

        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askServer().changeWorkType(unit, workType)
            && unit.getWorkType() == workType;
        if (ret) {
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Check if a unit is a treasure train, and if it should be cashed in.
     * Transfers the gold carried by this unit to the {@link Player owner}.
     *
     * Called from TilePopup
     *
     * @param unit The <code>Unit</code> to be checked.
     * @return True if the unit was cashed in (and disposed).
     */
    public boolean checkCashInTreasureTrain(Unit unit) {
        if (!requireOurTurn() || unit == null
            || !unit.canCarryTreasure() || !unit.canCashInTreasureTrain())
            return false; // Fail quickly if just not a candidate.

        final Tile tile = unit.getTile();
        final Europe europe = unit.getOwner().getEurope();
        if (europe == null || unit.isInEurope()) {
            ;// No need to check for transport.
        } else {
            int fee = unit.getTransportFee();
            StringTemplate template;
            if (fee == 0) {
                template = StringTemplate.template("cashInTreasureTrain.free");
            } else {
                int percent = getSpecification()
                    .getInteger(GameOptions.TREASURE_TRANSPORT_FEE);
                template = StringTemplate.template("cashInTreasureTrain.pay")
                    .addAmount("%fee%", percent);
            }
            if (!getGUI().confirm(unit.getTile(), template, unit,
                             "accept", "reject")) return false;
        }

        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askServer().cashInTreasureTrain(unit)
            && unit.isDisposed();
        if (ret) {
            sound("sound.event.cashInTreasureTrain");
            unitWas.fireChanges();
            updateGUI(tile);
        }
        return ret;
    }

    /**
     * Choose a founding father from an offered list.
     *
     * Called from GUI.showChooseFoundingFatherDialog
     *
     * @param ffs A list of <code>FoundingFather</code>s to choose from.
     * @param ff The chosen <code>FoundingFather</code> (may be null).
     * @return True if a father was chosen.
     */
    public boolean chooseFoundingFather(List<FoundingFather> ffs,
                                        FoundingFather ff) {
        if (ffs == null) return false;

        final Player player = getMyPlayer();
        player.setCurrentFather(ff);
        return askServer().chooseFoundingFather(ffs, ff);
    }

    /**
     * Choose a founding father from an offered list.
     *
     * Called from IGIH.chooseFoundingFather.
     *
     * @param ffs A list of <code>FoundingFather</code>s to choose from.
     */
    public void chooseFoundingFather(List<FoundingFather> ffs) {
        if (ffs == null) return;
        getGUI().showChooseFoundingFatherDialog(ffs,
            (FoundingFather ff) -> chooseFoundingFather(ffs, ff));
    }

    /**
     * Claim a tile.
     *
     * Called from ColonyPanel.ASingleTilePanel, UnitLabel and work()
     *
     * @param tile The <code>Tile</code> to claim.
     * @param claimant The <code>Unit</code> or <code>Colony</code> claiming.
     * @return True if the claim succeeded.
     */
    public boolean claimTile(Tile tile, FreeColGameObject claimant) {
        if (!requireOurTurn() || tile == null
            || claimant == null) return false;

        final Player player = getMyPlayer();
        final int price = ((claimant instanceof Settlement)
                ? player.canClaimForSettlement(tile)
                : player.canClaimForImprovement(tile))
            ? 0
            : player.getLandPrice(tile);
        UnitWas unitWas = (claimant instanceof Unit)
            ? new UnitWas((Unit)claimant) : null;
        boolean ret = askClaimTile(player, tile, claimant, price);
        if (ret) {
            if (unitWas != null) unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Clears the goto orders of the given unit by setting its destination
     * to null.
     *
     * Called from CanvasMouseListener
     *
     * @param unit The <code>Unit</code> to clear the destination for.
     * @return True if the unit has no destination.
     */
    public boolean clearGotoOrders(Unit unit) {
        if (!requireOurTurn() || unit == null) return false;

        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askClearGotoOrders(unit);
        if (ret) {
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Clears the orders of the given unit.
     * Make the unit active and set a null destination and trade route.
     *
     * Called from ClearOrdersAction, TilePopup, TradeRoutePanel, UnitLabel
     *
     * @param unit The <code>Unit</code> to clear the orders of
     * @return boolean <b>true</b> if the orders were cleared
     */
    public boolean clearOrders(Unit unit) {
        if (!requireOurTurn() || unit == null) return false;

        if (unit.getState() == UnitState.IMPROVING
            && !getGUI().confirm(unit.getTile(), StringTemplate
                .template("clearOrders.text")
                .addAmount("%turns%", unit.getWorkTurnsLeft()),
                unit, "ok", "cancel")) {
            return false;
        }

        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askClearGotoOrders(unit)
            && (unit.getState() == UnitState.ACTIVE
                || askServer().changeState(unit, UnitState.ACTIVE));
        if (ret) {
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Clear the speciality of a Unit, making it a Free Colonist.
     *
     * Called from UnitLabel
     *
     * @param unit The <code>Unit</code> to clear the speciality of.
     * @return True if the speciality was cleared.
     */
    public boolean clearSpeciality(Unit unit) {
        if (!requireOurTurn() || unit == null) return false;

        UnitType oldType = unit.getType();
        UnitType newType = oldType.getTargetType(ChangeType.CLEAR_SKILL,
                                                 unit.getOwner());
        if (newType == null) {
            getGUI().showInformationMessage(unit, StringTemplate
                .template("clearSpeciality.impossible")
                .addStringTemplate("%unit%",
                    unit.getLabel(Unit.UnitLabelType.NATIONAL)));
            return false;
        }

        final Tile tile = (getGUI().isShowingSubPanel()) ? null : unit.getTile();
        if (!getGUI().confirm(tile, StringTemplate
                .template("clearSpeciality.areYouSure")
                .addStringTemplate("%oldUnit%",
                    unit.getLabel(Unit.UnitLabelType.NATIONAL))
                .addNamed("%unit%", newType),
                unit, "ok", "cancel")) {
            return false;
        }

        // Try to clear.
        // Note that this routine is only called out of UnitLabel,
        // where the unit icon is always updated anyway.
        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askServer().clearSpeciality(unit)
            && unit.getType() == newType;
        if (ret) {
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Close any open GUI menus.
     *
     * Called from IGIH.closeMenus.
     */
    public void closeMenus() {
        getGUI().closeMenus();
    }

    /**
     * Declares independence for the home country.
     *
     * Called from DeclareIndependenceAction
     *
     * @return True if independence was declared.
     */
    public boolean declareIndependence() {
        if (!requireOurTurn()) return false;

        final Player player = getMyPlayer();
        if (player.getNewLandName() == null) {
            return false; // Can only happen in debug mode.
        }

        // Check for adequate support.
        StringTemplate declare = player.checkDeclareIndependence();
        if (declare != null) {
            getGUI().showInformationMessage(declare);
            return false;
        }

        // Confirm intention, and collect nation+country names.
        List<String> names = getGUI().confirmDeclaration();
        if (names == null
            || names.get(0) == null || names.get(0).isEmpty()
            || names.get(1) == null || names.get(1).isEmpty()) {
            // Empty name => user cancelled.
            return false;
        }

        // Ask server.
        boolean ret = askServer()
            .declareIndependence(getGame(), names.get(0), names.get(1))
            && player.isRebel();
        if (ret) {
            getGUI().showDeclarationPanel();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Delete a trade route.
     *
     * Called from TradeRoutePanel button.
     *
     * @param tradeRoute The <code>TradeRoute</code> to delete.
     * @return True if the route was successfully deleted.
     */
    public boolean deleteTradeRoute(TradeRoute tradeRoute) {
        final Player player = getMyPlayer();
        final String name = tradeRoute.getName();
        boolean ret = askServer().deleteTradeRoute(tradeRoute);
        return ret && player.getTradeRouteByName(name, null) == null;
    }

    /**
     * Handle a diplomatic offer.
     *
     * Called from IGIH.diplomacy
     *
     * @param our Our <code>FreeColGameObject</code> that is negotiating.
     * @param other The other <code>FreeColGameObject</code>.
     * @param agreement The <code>DiplomaticTrade</code> agreement.
     * @return A counter agreement, a rejected agreement, or null if
     *     the original agreement was already decided.
     */
    public DiplomaticTrade diplomacy(FreeColGameObject our,
                                     FreeColGameObject other,
                                     DiplomaticTrade agreement) {
        final Player player = getMyPlayer();
        final Player otherPlayer = agreement.getOtherPlayer(player);
        StringTemplate t, nation = otherPlayer.getNationLabel();

        switch (agreement.getStatus()) {
        case ACCEPT_TRADE:
            boolean visibilityChange = false;
            for (Colony c : agreement.getColoniesGivenBy(player)) {
                player.removeSettlement(c);//-vis(player)
                visibilityChange = true;
            }
            for (Unit u : agreement.getUnitsGivenBy(player)) {
                player.removeUnit(u);//-vis(player)
                visibilityChange = true;
            }
            if (visibilityChange) player.invalidateCanSeeTiles();//+vis(player)
            ModelMessage mm
                = new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                   "diplomacy.offerAccepted", otherPlayer)
                    .addStringTemplate("%nation%", nation);
            player.addModelMessage(mm);
            updateGUI(null);
            break;
        case REJECT_TRADE:
            t = StringTemplate.template("diplomacy.offerRejected")
                .addStringTemplate("%nation%", nation);
            getGUI().showInformationMessage(t);
            break;
        case PROPOSE_TRADE:
            t = agreement.getReceiveMessage(otherPlayer);
            DiplomaticTrade ourAgreement
                = getGUI().showNegotiationDialog(our, other, agreement, t);
            if (ourAgreement == null) {
                agreement.setStatus(TradeStatus.REJECT_TRADE);
            } else {
                agreement = ourAgreement;
            }
            return agreement;
        default:
            logger.warning("Bogus trade status: " + agreement.getStatus());
            break;
        }
        return null;
    }

    /**
     * Disbands the active unit.
     *
     * Called from DisbandUnitAction.
     *
     * @param unit The <code>Unit</code> to disband.
     * @return True if the unit was disbanded.
     */
    public boolean disbandUnit(Unit unit) {
        if (!requireOurTurn() || unit == null) return false;

        if (unit.getColony() != null
            && !getGUI().confirmLeaveColony(unit)) return false;
        final Tile tile = (getGUI().isShowingSubPanel()) ? null : unit.getTile();
        if (!getGUI().confirm(tile, StringTemplate.key("disbandUnit.text"),
                         unit, "disbandUnit.yes", "cancel"))
            return false;

        // Try to disband
        boolean ret = askServer().disbandUnit(unit) && unit.isDisposed();
        if (ret) {
            updateGUI(tile);
        }
        return ret;
    }

    /**
     * Display the high scores.
     *
     * Called from IGIH.gameEnded, ReportHighScoresAction
     *
     * @param high A <code>Boolean</code> whose values indicates whether
     *     a new high score has been achieved, or no information if null.
     * @return True if the server interaction succeeded.
     */
    public boolean displayHighScores(Boolean high) {
        final Game game = getGame();
        return askServer().getHighScores(game,
            (high == null) ? null
            : (high) ? "highscores.yes"
            : "highscores.no");
    }

    /**
     * Display the high scores.
     *
     * Called from IGIH.highScore.
     *
     * @param key An optional message key.
     * @param scores The list of <code>HighScore</code> records to display.
     */
    public void displayHighScores(String key, List<HighScore> scores) {
        getGUI().showHighScoresPanel(key, scores);
    }

    /**
     * Displays pending <code>ModelMessage</code>s.
     *
     * Called from IGIH.displayModelMessagesRunnable
     *
     * @param allMessages Display all messages or just the undisplayed ones.
     * @return True if any messages were displayed.
     */
    public boolean displayModelMessages(boolean allMessages) {
        return displayModelMessages(allMessages, false);
    }

    /**
     * Emigrate a unit from Europe.
     *
     * Called from GUI.showEmigrationDialog.
     *
     * @param player The <code>Player</code> that owns the unit.
     * @param slot The slot to emigrate from, [0..RECRUIT_COUNT].
     * @param n The number of remaining units known to be eligible to migrate.
     * @param foY True if this migration is due to a fountain of youth event.
     */
    public void emigrate(Player player, int slot, int n, boolean foY) {
        if (player == null || !player.isColonial()
            || !MigrationType.validMigrantSlot(slot)) return;

        if (askEmigrate(player.getEurope(), slot) != null) {
            emigration(player, n, foY);
        }
    }

    /**
     * End the turn command.
     *
     * Called from EndTurnAction, GUI.showEndTurnDialog
     *
     * @param showDialog If false, suppress showing the end turn dialog.
     * @return True if the turn was ended.
     */
    public boolean endTurn(boolean showDialog) {
        if (!requireOurTurn()) return false;

        return doEndTurn(showDialog
            && getClientOptions().getBoolean(ClientOptions.SHOW_END_TURN_DIALOG));
    }

    /**
     * Change the role-equipment a unit has.
     *
     * Called from DefaultTransferHandler, QuickActionMenu
     *
     * @param unit The <code>Unit</code>.
     * @param role The <code>Role</code> to assume.
     * @param roleCount The role count.
     * @return True if the role is taken.
     */
    public boolean equipUnitForRole(Unit unit, Role role, int roleCount) {
        if (!requireOurTurn() || unit == null || role == null || 0 > roleCount
            || roleCount > role.getMaximumCount()) return false;
        if (role == unit.getRole()
            && roleCount == unit.getRoleCount()) return true;

        final Player player = getMyPlayer();
        final Colony colony = unit.getColony();
        ColonyWas colonyWas = (colony != null) ? new ColonyWas(colony) : null;
        final Europe europe = player.getEurope();
        EuropeWas europeWas = (europe != null) ? new EuropeWas(europe) : null;
        final Market market = (europe != null) ? player.getMarket() : null;
        MarketWas marketWas = (market != null) ? new MarketWas(player) : null;
        int price = -1;

        List<AbstractGoods> req = unit.getGoodsDifference(role, roleCount);
        if (unit.isInEurope()) {
            for (AbstractGoods ag : req) {
                GoodsType goodsType = ag.getType();
                if (!player.canTrade(goodsType) && !payArrears(goodsType)) {
                    return false; // payment failed
                }
            }
            price = player.getEurope().priceGoods(req);
            if (price < 0 || !player.checkGold(price)) return false;
        } else if (colony != null) {
            for (AbstractGoods ag : req) {
                if (colony.getGoodsCount(ag.getType()) < ag.getAmount()) {
                    StringTemplate template = StringTemplate
                        .template("equipUnit.impossible")
                        .addName("%colony%", colony.getName())
                        .addNamed("%equipment%", ag.getType())
                        .addStringTemplate("%unit%",
                            unit.getLabel(Unit.UnitLabelType.NATIONAL));
                    getGUI().showInformationMessage(unit, template);
                    return false;
                }
            }
        } else {
            return false;
        }

        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askServer().equipUnitForRole(unit, role, roleCount)
            && unit.getRole() == role;
        if (ret) {
            if (colonyWas != null) colonyWas.fireChanges();
            if (europeWas != null) europeWas.fireChanges();
            if (marketWas != null) marketWas.fireChanges(req);
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Display an error.
     *
     * Called from IGIH.error.
     *
     * @param messageId The i18n-keyname of the error message to display.
     * @param message An alternative non-i18n message to display if
     *     the resource specified by <code>messageId</code> is unavailable.
     */
    public void error(String messageId, String message) {
        logger.warning("Error: " + messageId + "/" + message);
        getGUI().showErrorMessage(messageId, message);
    }

    /**
     * Execute goto orders command.
     *
     * Called from ExecuteGotoOrdersAction.
     *
     * @return True if all goto orders have been performed and no units
     *     reached their destination and are free to move again.
     */
    public boolean executeGotoOrders() {
        if (!requireOurTurn()) return false;

        return doExecuteGotoOrders();
    }

    /**
     * A player makes first contact with a native player.
     *
     * Called from GUI.showFirstContactDialog
     *
     * @param player The <code>Player</code> making contact.
     * @param other The native <code>Player</code> being contacted.
     * @param tile An optional <code>Tile</code> to offer the player if
     *     they have made a first landing.
     * @param result Whether the initial treaty was accepted.
     * @return True if first contact occurs.
     */
    public boolean firstContact(Player player, Player other, Tile tile,
                                boolean result) {
        if (player == null || player == null || player == other
            || tile == null) return false;

        boolean ret = askServer().firstContact(player, other, tile, result);
        if (ret) {
            updateGUI(null);
        }
        return ret;
    }

    /**
     * A player makes first contact with a native player.
     *
     * Called from IGIH.firstContact.
     *
     * @param player The <code>Player</code> making contact.
     * @param other The native <code>Player</code> being contacted.
     * @param tile An optional <code>Tile</code> to offer the player if
     *     they have made a first landing.
     * @param n The number of settlements claimed by the native player.
     */
    public void firstContact(Player player, Player other, Tile tile, int n) {
        getGUI().showFirstContactDialog(player, other, tile, n,
            (Boolean b) -> firstContact(player, other, tile, b));
    }

    /**
     * Handle a fountain of youth event.
     *
     * Called from IGIH.fountainOfYouth.
     *
     * @param n The number of migrants available for selection.
     */
    public void fountainOfYouth(int n) {
        final Player player = getMyPlayer();
        final boolean fountainOfYouth = true;
        getGUI().showEmigrationDialog(player, fountainOfYouth,
            (Integer value) -> { // Value is a valid slot
                emigrate(player,
                         Europe.MigrationType.convertToMigrantSlot(value),
                         n-1, fountainOfYouth);
            });
    }

    /**
     * Get the nation summary for a player.
     *
     * Called from DiplomaticTradePanel, ReportForeignAffairsPanel,
     * ReportIndianPanel
     *
     * @param player The <code>Player</code> to summarize.
     * @return A summary of that nation, or null on error.
     */
    public NationSummary getNationSummary(Player player) {
        if (player == null) return null;

        final Player myPlayer = getMyPlayer();
        NationSummary ns = myPlayer.getNationSummary(player);
        if (ns != null) return ns;
        // Refresh from server
        if (askServer().nationSummary(myPlayer, player)) {
            return myPlayer.getNationSummary(player);
        }
        return null;
    }

    /**
     * Go to a tile.
     *
     * Called from CanvasMouseListener, TilePopup
     *
     * @param unit The <code>Unit</code> to move.
     * @param tile The <code>Tile</code> to move to.
     * @return True if the destination change was successful.
     */
    public boolean goToTile(Unit unit, Tile tile) {
        if (!requireOurTurn() || unit == null
            || !getMyPlayer().owns(unit)) return false;

        if (!getGUI().confirmClearTradeRoute(unit)) return false;

        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askSetDestination(unit, tile);
        if (ret) {
            moveToDestination(unit, null);
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Ignore this ModelMessage from now on until it is not generated
     * in a turn.
     *
     * Called from ReportTurnPanel
     *
     * @param message a <code>ModelMessage</code> value
     * @param flag whether to ignore the ModelMessage or not
     * @return True, ignore message status changes can not fail.
     */
    public boolean ignoreMessage(ModelMessage message, boolean flag) {
        String key;
        if (message == null
            || (key = message.getIgnoredMessageKey()) == null) return false;

        if (flag) {
            final Turn turn = getGame().getTurn();
            if (!continueIgnoreMessage(key, turn)) {
                startIgnoringMessage(key, turn);
            }
        } else {
            stopIgnoringMessage(key);
        }
        return true;
    }

    /**
     * Handle an incite response.
     *
     * Called from IGIH.incite.
     *
     * @param unit The <code>Unit</code> that is inciting.
     * @param is The <code>IndianSettlement</code> being incited.
     * @param enemy The <code>Player</code> incited against.
     * @param gold The gold required by the natives to become hostile.
     */
    public void incite(Unit unit, IndianSettlement is, Player enemy, int gold) {
        final Player player = getMyPlayer();
        
        if (gold < 0) {
            ; // protocol fail
        } else if (!player.checkGold(gold)) {
            getGUI().showInformationMessage(is, StringTemplate
                .template("missionarySettlement.inciteGoldFail")
                .add("%player%", enemy.getName())
                .addAmount("%amount%", gold));
        } else if (getGUI().confirm(unit.getTile(), StringTemplate
                .template("missionarySettlement.inciteConfirm")
                .addStringTemplate("%enemy%", enemy.getNationLabel())
                .addAmount("%amount%", gold),
                unit, "yes", "no")) {
            askServer().incite(unit, is, enemy, gold);
        }
    }

    /**
     * Handle a native demand at a colony.
     *
     * Called from IGIH.indianDemand
     *
     * @param unit The native <code>Unit</code> making the demand.
     * @param colony The <code>Colony</code> demanded of.
     * @param type The <code>GoodsType</code> demanded (null means gold).
     * @param amount The amount of goods/gold demanded.
     * @return Whether the demand was accepted or not.
     */
    public boolean indianDemand(Unit unit, Colony colony,
                                GoodsType type, int amount) {
        if (unit == null || colony == null) return false;

        final Player player = getMyPlayer();
        final int opt = getClientOptions()
            .getInteger(ClientOptions.INDIAN_DEMAND_RESPONSE);
        boolean accepted;
        ModelMessage m = null;
        String nation = Messages.message(unit.getOwner().getNationLabel());
        if (type == null) {
            switch (opt) {
            case ClientOptions.INDIAN_DEMAND_RESPONSE_ASK:
                accepted = getGUI().confirm(colony.getTile(), StringTemplate
                    .template("indianDemand.gold.text")
                    .addName("%nation%", nation)
                    .addName("%colony%", colony.getName())
                    .addAmount("%amount%", amount),
                    unit, "accept", "indianDemand.gold.no");
                break;
            case ClientOptions.INDIAN_DEMAND_RESPONSE_ACCEPT:
                m = new ModelMessage(ModelMessage.MessageType.DEMANDS,
                                     "indianDemand.gold.text", colony, unit)
                    .addName("%nation%", nation)
                    .addName("%colony%", colony.getName())
                    .addAmount("%amount%", amount);
                accepted = true;
                break;
            case ClientOptions.INDIAN_DEMAND_RESPONSE_REJECT:
                m = new ModelMessage(ModelMessage.MessageType.DEMANDS,
                                     "indianDemand.gold.text", colony, unit)
                    .addName("%nation%", nation)
                    .addName("%colony%", colony.getName())
                    .addAmount("%amount%", amount);
                accepted = false;
                break;
            default:
                throw new RuntimeException("Impossible option value.");
            }
        } else {
            switch (opt) {
            case ClientOptions.INDIAN_DEMAND_RESPONSE_ASK:
                if (type.isFoodType()) {
                    accepted = getGUI().confirm(colony.getTile(),
                        StringTemplate.template("indianDemand.food.text")
                            .addName("%nation%", nation)
                            .addName("%colony%", colony.getName())
                            .addAmount("%amount%", amount),
                        unit, "indianDemand.food.yes", "indianDemand.food.no");
                } else {
                    accepted = getGUI().confirm(colony.getTile(),
                        StringTemplate.template("indianDemand.other.text")
                            .addName("%nation%", nation)
                            .addName("%colony%", colony.getName())
                            .addAmount("%amount%", amount)
                            .addNamed("%goods%", type),
                        unit, "accept", "indianDemand.other.no");
                }
                break;
            case ClientOptions.INDIAN_DEMAND_RESPONSE_ACCEPT:
                if (type.isFoodType()) {
                    m = new ModelMessage(ModelMessage.MessageType.DEMANDS,
                                         "indianDemand.food.text",
                                         colony, unit)
                        .addName("%nation%", nation)
                        .addName("%colony%", colony.getName())
                        .addAmount("%amount%", amount);
                } else {
                    m = new ModelMessage(ModelMessage.MessageType.DEMANDS,
                                         "indianDemand.other.text",
                                         colony, unit)
                        .addName("%nation%", nation)
                        .addName("%colony%", colony.getName())
                        .addAmount("%amount%", amount)
                        .addNamed("%goods%", type);
                }
                accepted = true;
                break;
            case ClientOptions.INDIAN_DEMAND_RESPONSE_REJECT:
                if (type.isFoodType()) {
                    m = new ModelMessage(ModelMessage.MessageType.DEMANDS,
                                         "indianDemand.food.text",
                                         colony, unit)
                        .addName("%nation%", nation)
                        .addName("%colony%", colony.getName())
                        .addAmount("%amount%", amount);
                } else {
                    m = new ModelMessage(ModelMessage.MessageType.DEMANDS,
                                         "indianDemand.other.text",
                                         colony, unit)
                        .addName("%nation%", nation)
                        .addName("%colony%", colony.getName())
                        .addAmount("%amount%", amount)
                        .addNamed("%goods%", type);
                }
                accepted = false;
                break;
            default:
                throw new RuntimeException("Impossible option value.");
            }
        }
        if (m != null) {
            player.addModelMessage(m);
            displayModelMessages(false);
        }
        return accepted;
    }

    /**
     * Join the colony at a unit's current location.
     *
     * @param unit The <code>Unit</code> to use.
     * @return True if the unit joined a colony.
     */
    public boolean joinColony(Unit unit) {
        final Tile tile = unit.getTile();
        final Colony colony = (tile == null) ? null : tile.getColony();
        boolean ret = colony != null && askServer().joinColony(unit, colony)
            && unit.getState() == UnitState.IN_COLONY;
        if (ret) {
            updateGUI(null);
            colonyPanel(colony, unit);
        }
        return ret;
    }
    
    /**
     * Leave a ship.  The ship must be in harbour.
     *
     * Called from CargoPanel, ColonyPanel, EuropePanel.unloadAction,
     * UnitLabel
     *
     * @param unit The <code>Unit</code> which is to leave the ship.
     * @return True if the unit left the ship.
     */
    public boolean leaveShip(Unit unit) {
        Unit carrier;
        if (!requireOurTurn() || unit == null
            || (carrier = unit.getCarrier()) == null) return false;

        // Proceed to disembark
        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askServer().disembark(unit)
            && unit.getLocation() != carrier;
        if (ret) {
            checkCashInTreasureTrain(unit);
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Loads a cargo onto a carrier.
     *
     * Called from CargoPanel, ColonyPanel, LoadAction, TilePopup.
     *
     * @param goods The <code>Goods</code> which are going aboard the carrier.
     * @param carrier The <code>Unit</code> acting as carrier.
     * @return True if the goods were loaded.
     */
    public boolean loadCargo(Goods goods, Unit carrier) {
        if (!requireOurTurn() || goods == null || goods.getAmount() <= 0
            || goods.getLocation() == null
            || carrier == null || !carrier.isCarrier()) return false;

        if (goods.getLocation() instanceof Europe) {
            return buyGoods(goods.getType(), goods.getAmount(), carrier);
        }
        UnitWas carrierWas = new UnitWas(carrier);
        UnitWas sourceWas = null;
        ColonyWas colonyWas = null;
        if (goods.getLocation() instanceof Unit) {
            Unit source = (Unit)goods.getLocation();
            sourceWas = new UnitWas(source);
        } else {
            Colony colony = carrier.getColony();
            if (colony == null) return false;
            colonyWas = new ColonyWas(colony);
        }

        boolean ret = askLoadGoods(goods.getLocation(), goods.getType(),
                                   goods.getAmount(), carrier);
        if (ret) {
            sound("sound.event.loadCargo");
            if (colonyWas != null) colonyWas.fireChanges();
            if (sourceWas != null) sourceWas.fireChanges();
            carrierWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Opens a dialog where the user should specify the filename and
     * loads the game.
     *
     * Called from OpenAction.
     *
     * Returns no status as this game is stopped.
     */
    public void loadGame() {
        File file = getGUI().showLoadSaveFileDialog();
        if (file == null) return;
        if (getFreeColClient().isInGame()
            && !getGUI().confirmStopGame()) return;

        getConnectController().quitGame(true);
        turnReportMessages.clear();
        getGUI().setActiveUnit(null);
        getGUI().removeInGameComponents();
        FreeColDirectories.setSavegameFile(file.getPath());
        getConnectController().startSavedGame(file, null);
    }

    /**
     * Loot some cargo.
     *
     * Called from GUI.showCaptureGoodsDialog
     *
     * @param unit The <code>Unit</code> that is looting.
     * @param goods A list of <code>Goods</code> to choose from.
     * @param defenderId The identifier of the defender unit (may have sunk).
     * @return True if looting occurs.
     */
    public boolean lootCargo(Unit unit, List<Goods> goods, String defenderId) {
        if (unit == null || goods == null || goods.isEmpty()
            || defenderId == null) return false;

        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askServer().loot(unit, defenderId, goods);
        if (ret) {
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Loot some cargo.
     *
     * Called from IGIH.lootCargo.
     *
     * @param unit The <code>Unit</code> that is looting.
     * @param goods A list of <code>Goods</code> to choose from.
     * @param defenderId The identifier of the defender unit (may have sunk).
     */
    public void loot(Unit unit, List<Goods> goods, String defenderId) {
        getGUI().showCaptureGoodsDialog(unit, goods,
            (List<Goods> gl) -> lootCargo(unit, gl, defenderId));
    }

    /**
     * Accept or reject a monarch action.
     *
     * Called from GUI.showMonarchDialog
     *
     * @param action The <code>MonarchAction</code> performed.
     * @param accept If true, accept the action.
     * @return True if the monarch was answered.
     */
    public boolean monarchAction(MonarchAction action, boolean accept) {
        if (action == null) return false;

        boolean ret = false;
        switch (action) {
        case RAISE_TAX_ACT: case RAISE_TAX_WAR:
        case MONARCH_MERCENARIES: case HESSIAN_MERCENARIES:
            ret = askServer().answerMonarch(getGame(), action, accept);
            break;
        default:
            break;
        }
        if (ret) {
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Do a monarch interaction.
     *
     * Called from IGIH.monarchAction.
     *
     * @param action The <code>MonarchAction</code> to perform.
     * @param template A <code>StringTemplate</code> describing the action.
     * @param monarchKey A key for the monarch involved.
     */
    public void monarch(MonarchAction action, StringTemplate template,
                        String monarchKey) {
        getGUI().showMonarchDialog(action, template, monarchKey,
            (Boolean b) -> monarchAction(action, b));
    }

    /**
     * Moves the specified unit somewhere that requires crossing the
     * high seas.
     *
     * Called from EuropePanel.DestinationPanel, TilePopup
     *
     * @param unit The <code>Unit</code> to be moved.
     * @param destination The <code>Location</code> to be moved to.
     * @return True if the unit can possibly move further.
     */
    public boolean moveTo(Unit unit, Location destination) {
        if (!requireOurTurn() || unit == null
            || destination == null) return false;

        // Sanity check current state.
        if (destination instanceof Europe) {
            if (unit.isInEurope()) {
                sound("sound.event.illegalMove");
                return false;
            }
        } else if (destination instanceof Map) {
            if (unit.hasTile() && unit.getTile().getMap() == destination) {
                sound("sound.event.illegalMove");
                return false;
            }
        } else if (destination instanceof Settlement) {
            if (unit.hasTile()) {
                sound("sound.event.illegalMove");
                return false;
            }
        } else {
            return false;
        }

        // Autoload?
        boolean update = false;
        if (getClientOptions().getBoolean(ClientOptions.AUTOLOAD_EMIGRANTS)
            && unit.isInEurope()) {
            for (Unit u : unit.getOwner().getEurope().getUnitList()) {
                if (!u.isNaval()
                    && u.getState() == UnitState.SENTRY
                    && unit.canAdd(u)) {
                    if (askEmbark(u, unit)) update = true;
                }
            }
        }

        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askServer().moveTo(unit, destination);
        if (ret) {
            unitWas.fireChanges();
            update = true;
        }
        if (update) updateGUI(null);
        return ret;
    }

    /**
     * Moves the active unit in a specified direction. This may result in an
     * attack, move... action.
     *
     * Called from MoveAction, CornerMapControls
     *
     * @param unit The <code>Unit</code> to move.
     * @param direction The <code>Direction</code> in which to move
     *     the active unit.
     * @return True if the unit may move further.
     */
    public boolean moveUnit(Unit unit, Direction direction) {
        if (!requireOurTurn() || unit == null
            || direction == null || !unit.hasTile()) return false;

        if (!askClearGotoOrders(unit)) return false;

        final int unitCount = unit.getUnitCount(),
            goodsCount = unit.getGoodsList().size();
        final Tile oldTile = unit.getTile();
        UnitWas unitWas = new UnitWas(unit);
        ColonyWas colonyWas = (unit.getColony() == null) ? null
            : new ColonyWas(unit.getColony());
        unit.setState(UnitState.ACTIVE);
        moveDirection(unit, direction, true);
        boolean ret = unit.getTile() != oldTile
            || unitWas.fireChanges();
        if (ret) {
            if (colonyWas != null) colonyWas.fireChanges();
            updateGUI(null);
            if (!unit.couldMove() && unit.hasTile()) {
                // Show colony panel if unit out of moves
                Colony colony = unit.getTile().getColony();
                if (colony != null) colonyPanel(colony, unit);
            }
        }
        return ret;
    }

   /**
     * Move the tile cursor.
     *
     * Called from MoveAction in terrain mode.
     *
     * @param direction The <code>Direction</code> to move the tile cursor.
     * @return True if the tile cursor is moved.
     */
    public boolean moveTileCursor(Direction direction) {
        if (direction == null) return false;

        final Tile tile = getGUI().getSelectedTile();
        if (tile == null) return false;

        final Tile newTile = tile.getNeighbourOrNull(direction);
        if (newTile == null) return false;

        getGUI().setSelectedTile(newTile);
        return true;
    } 

    /**
     * A player names the New World.
     *
     * Called from GUI.showNameNewLandDialog
     *
     * @param unit The <code>Unit</code> that landed.
     * @param name The name to use.
     * @return True if the new land was named.
     */
    public boolean nameNewLand(Unit unit, String name) {
        if (unit == null || name == null) return false;

        // Respond to the server.
        if (!askServer().newLandName(unit, name)) return false;

        // The name is set, bring up the first landing panel.
        final Player player = unit.getOwner();
        StringTemplate t = StringTemplate.template("event.firstLanding")
            .addName("%name%", name);
        getGUI().showEventPanel(Messages.message(t), "image.flavor.event.firstLanding",
                           null);

        // Add tutorial message.
        final String key = FreeColActionUI
            .getHumanKeyStrokeText(getFreeColClient()
                .getActionManager().getFreeColAction("buildColonyAction")
                .getAccelerator());
        player.addModelMessage(new ModelMessage(ModelMessage.MessageType.TUTORIAL,
                               "buildColony.tutorial", player)
            .addName("%colonyKey%", key)
            .add("%colonyMenuItem%", "buildColonyAction.name")
            .add("%ordersMenuItem%", "menuBar.orders"));
        displayModelMessages(false);
        return true;
    }

    /**
     * The player names a new region.
     *
     * Called from newRegionName, GUI.showNameNewRegionDialog
     *
     * @param tile The <code>Tile</code> within the region.
     * @param unit The <code>Unit</code> that has discovered the region.
     * @param region The <code>Region</code> to name.
     * @param name The name to offer.
     * @return True if the new region was named.
     */
    public boolean nameNewRegion(final Tile tile, final Unit unit,
                                 final Region region, final String name) {
        if (tile == null || unit == null || region == null) return false;

        return askServer().newRegionName(region, tile, unit, name);
    }

    /**
     * Ask the player to name the new land.
     *
     * Called from IGIH.newLandName.
     *
     * @param defaultName The default name to use.
     * @param unit The <code>Unit</code> that has landed.
     */
    public void newLandName(String defaultName, Unit unit) {
        getGUI().showNamingDialog(
            StringTemplate.key("newLand.text"), defaultName, unit,
            (String name) -> {
                if (name == null || name.isEmpty()) name = defaultName;
                nameNewLand(unit, name);
            });
    }

    /**
     * Ask the player to name a new region.
     *
     * Called from IGIH.newRegionName.
     *
     * @param region The <code>Region</code> to name.
     * @param defaultName The default name to use.
     * @param tile The <code>Tile</code> the unit landed at.
     * @param unit The <code>Unit</code> that has landed.
     */
    public void newRegionName(Region region, String defaultName, Tile tile,
                              Unit unit) {
        if (region.hasName()) {
            if (region.isPacific()) {
                getGUI().showEventPanel(Messages.message("event.discoverPacific"),
                                   "image.flavor.event.discoverPacific", null);
            }
            nameNewRegion(tile, unit, region, defaultName);
        } else {
            getGUI().showNamingDialog(
                StringTemplate.template("nameRegion.text")
                              .addStringTemplate("%type%", region.getLabel()),
                defaultName, unit,
                (String name) -> {
                    if (name == null || name.isEmpty()) name = defaultName;
                    nameNewRegion(tile, unit, region, name);
                });
        }
    }

    /**
     * Gets a new trade route for a player.
     *
     * Called from TradeRoutePanel.newRoute.  Relies on new trade routes
     * being added at the end of the trade route list.
     *
     * @param player The <code>Player</code> to get a new trade route for.
     * @return A new <code>TradeRoute</code>.
     */
    public TradeRoute newTradeRoute(Player player) {
        if (player == null) return null;

        final int n = player.getTradeRouteCount();
        return (askServer().newTradeRoute(getGame())
            && player.getTradeRouteCount() == n + 1)
            ? player.getNewestTradeRoute()
            : null;
    }

    /**
     * Switch to a new turn.
     *
     * Called from IGIH.newTurn
     *
     * @param turn The turn number.
     * @return True if the new turn occurs.
     */
    public boolean newTurn(int turn) {
        final Game game = getGame();
        final Player player = getMyPlayer();

        if (turn < 0) {
            logger.warning("Bad turn in newTurn: " + turn);
            return false;
        }
        Turn newTurn = new Turn(turn);
        game.setTurn(newTurn);
        logger.info("New turn: " + newTurn + "/" + turn);

        final boolean alert = getClientOptions().getBoolean(ClientOptions.AUDIO_ALERTS);
        if (alert) sound("sound.event.alertSound");

        final Turn currTurn = game.getTurn();
        if (currTurn.isFirstSeasonTurn()) {
            player.addModelMessage(new ModelMessage(MessageType.WARNING,
                                                    "twoTurnsPerYear", player)
                .addStringTemplate("%year%", currTurn.getLabel())
                .addAmount("%amount%", currTurn.getSeasonNumber()));
        }
        player.clearNationCache();
        return true;
    }

    /**
     * Makes a new unit active.
     *
     * Called from PGC.startGame, ColonyPanel.closeColonyPanel
     *
     * @return True unless it was not our turn.
     */
    public boolean nextActiveUnit() {
        if (!requireOurTurn()) return false;

        updateGUI(null);
        return true;
    }

    /**
     * Displays the next <code>ModelMessage</code>.
     *
     * Called from CC.reconnect, CargoPanel,
     * ColonyPanel.closeColonyPanel, EuropePanel.exitAction,
     * EuropePanel.MarketPanel
     *
     * @return True if any messages were displayed.
     */
    public boolean nextModelMessage() {
        return displayModelMessages(false, false);
    }

    /**
     * Pays the tax arrears on this type of goods.
     *
     * Called from CargoPanel, EuropePanel.MarketPanel,
     * EuropePanel.unloadAction, QuickActionMenu
     *
     * @param type The type of goods for which to pay arrears.
     * @return True if the arrears were paid.
     */
    public boolean payArrears(GoodsType type) {
        if (!requireOurTurn() || type == null) return false;

        final Player player = getMyPlayer();
        int arrears = player.getArrears(type);
        if (arrears <= 0) return false;
        if (!player.checkGold(arrears)) {
            getGUI().showInformationMessage(StringTemplate
                .template("payArrears.noGold")
                    .addAmount("%amount%", arrears));
            return false;
        }

        StringTemplate t = StringTemplate.template("payArrears.text")
            .addAmount("%amount%", arrears);
        if (!getGUI().confirm(null, t, type, "ok", "cancel")) return false;

        boolean ret = askServer().payArrears(getGame(), type)
            && player.canTrade(type);
        if (ret) {
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Buys the remaining hammers and tools for the {@link Building} currently
     * being built in the given <code>Colony</code>.
     *
     * Called from BuildQueuePanel
     *
     * @param colony The <code>Colony</code> where the building should be
     *     bought.
     * @return True if the building was bought.
     */
    public boolean payForBuilding(Colony colony) {
        if (!requireOurTurn() || colony == null) return false;

        if (!getSpecification().getBoolean(GameOptions.PAY_FOR_BUILDING)) {
            getGUI().showInformationMessage("payForBuilding.disabled");
            return false;
        }

        if (!colony.canPayToFinishBuilding()) {
            getGUI().showInformationMessage("info.notEnoughGold");
            return false;
        }

        final int price = colony.getPriceForBuilding();
        StringTemplate t = StringTemplate.template("payForBuilding.text")
            .addAmount("%amount%", price);
        if (!getGUI().confirm(null, t, colony, "yes", "no")) return false;

        ColonyWas colonyWas = new ColonyWas(colony);
        boolean ret = askServer().payForBuilding(colony)
            && colony.getPriceForBuilding() == 0;
        if (ret) {
            colonyWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Puts the specified unit outside the colony.
     *
     * Called from ColonyPanel.OutsideColonyPanel, UnitLabel
     *
     * @param unit The <code>Unit</code>
     * @return True if the unit was successfully put outside the colony.
     */
    public boolean putOutsideColony(Unit unit) {
        Colony colony;
        if (!requireOurTurn() || unit == null
            || (colony = unit.getColony()) == null) return false;

        if (!getGUI().confirmLeaveColony(unit)) return false;

        ColonyWas colonyWas = new ColonyWas(colony);
        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askServer().putOutsideColony(unit)
            && unit.getLocation() == colony.getTile();
        if (ret) {
            colonyWas.fireChanges();
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Query whether the user wants to reconnect?
     *
     * Called from ReconnectAction, IGIH.reconnectRunnable
     *
     * Returns no status, this game is going away.
     */
    public void reconnect() {
        if (getGUI().confirm("reconnect.text", "reconnect.no", "reconnect.yes")) {
            logger.finest("Reconnect quit.");
            getFreeColClient().quit();
        } else {
            logger.finest("Reconnect accepted.");
            getConnectController().reconnect();
        }
    }

    /**
     * Recruit a unit from a specified index in Europe.
     *
     * Called from RecruitPanel
     *
     * @param index The index in Europe to recruit from, [0..RECRUIT_COUNT).
     * @return True if a unit was recruited.
     */
    public boolean recruitUnitInEurope(int index) {
        if (!requireOurTurn()
            || !MigrationType.validMigrantIndex(index)) return false;

        final Player player = getMyPlayer();
        if (!player.isColonial()) return false;

        if (!player.checkGold(player.getRecruitPrice())) {
            getGUI().showInformationMessage("info.notEnoughGold");
            return false;
        }

        Unit newUnit = askEmigrate(player.getEurope(),
                                   MigrationType.migrantIndexToSlot(index));
        if (newUnit != null) {
            player.setNextActiveUnit(newUnit);
            getGUI().setActiveUnit(newUnit);
            updateGUI(null);
        }
        return newUnit != null;
    }

    /**
     * Remove game objects.
     *
     * Called from IGIH.remove().
     *
     * @param objects A list of <code>FreeColGameObject</code>s to remove.
     * @param divert An object to divert to when the original disappears.
     */
    public void remove(List<FreeColGameObject> objects,
                       FreeColGameObject divert) {
        final Player player = getMyPlayer();
        boolean visibilityChange = false;
        for (FreeColGameObject fcgo : objects) {
            if (divert != null) player.divertModelMessages(fcgo, divert);
        
            if (fcgo instanceof Settlement) {
                Settlement settlement = (Settlement)fcgo;
                if (settlement != null && settlement.getOwner() != null) {
                    settlement.getOwner().removeSettlement(settlement);
                }
                visibilityChange = true;//-vis(player)
                
            } else if (fcgo instanceof Unit) {
                // Deselect the object if it is the current active unit.
                Unit u = (Unit)fcgo;
                if (u == getGUI().getActiveUnit()) getGUI().setActiveUnit(null);

                // Temporary hack until we have real containers.
                if (u != null && u.getOwner() != null) {
                    u.getOwner().removeUnit(u);
                }
                visibilityChange = true;//-vis(player)
            }

            // Do just the low level dispose that removes
            // reference to this object in the client.  The other
            // updates should have done the rest.
            fcgo.disposeResources();
        }
        if (visibilityChange) player.invalidateCanSeeTiles();//+vis(player)

        getGUI().refresh();
    }
        
    /**
     * Renames a <code>Nameable</code>.
     *
     * Apparently this can be done while it is not your turn.
     *
     * Called from RenameAction, TilePopup.
     *
     * @param object The object to rename.
     * @return True if the object was renamed.
     */
    public boolean rename(Nameable object) {
        final Player player = getMyPlayer();
        if (!(object instanceof Ownable)
            || !player.owns((Ownable)object)) return false;

        String name = null;
        if (object instanceof Colony) {
            Colony colony = (Colony) object;
            name = getGUI().getInput(colony.getTile(),
                                StringTemplate.key("renameColony.text"),
                                colony.getName(), "rename", "cancel");
            if (name == null) { // User cancelled
                return false;
            } else if (name.isEmpty()) { // Zero length invalid
                getGUI().showInformationMessage("info.enterSomeText");
                return false;
            } else if (colony.getName().equals(name)) { // No change
                return false;
            } else if (player.getSettlementByName(name) != null) {
                // Colony name must be unique.
                getGUI().showInformationMessage((Colony)object, StringTemplate
                    .template("nameColony.notUnique")
                    .addName("%name%", name));
                return false;
            }
        } else if (object instanceof Unit) {
            Unit unit = (Unit) object;
            name = getGUI().getInput(unit.getTile(),
                                StringTemplate.key("renameUnit.text"),
                                unit.getName(), "rename", "cancel");
            if (name == null) return false; // User cancelled
        } else {
            logger.warning("Tried to rename an unsupported Nameable: "
                + object);
            return false;
        }

        if (askServer().rename((FreeColGameObject)object, name)) {
            updateGUI(null);
            return true;
        }
        return false;
    }

    /**
     * Opens a dialog where the user should specify the filename and
     * saves the game.
     *
     * Called from SaveAction and SaveAndQuitAction.
     *
     * @return True if the game was saved.
     */
    public boolean saveGame() {
        if (!getFreeColClient().canSaveCurrentGame()) return false;

        final Game game = getGame();
        if (game == null) return false; // Keyboard handling can race init
        String fileName = getSaveGameString(game);
        File file = getGUI().showSaveDialog(FreeColDirectories.getSaveDirectory(),
                                       fileName);
        if (file == null) return false;
        if (!getClientOptions().getBoolean(ClientOptions.CONFIRM_SAVE_OVERWRITE)
            || !file.exists()
            || getGUI().confirm("saveConfirmationDialog.areYouSure.text",
                           "ok", "cancel")) {
            FreeColDirectories.setSavegameFile(file.getPath());
            return saveGame(file);
        }
        return false;
    }

    /**
     * Display the results of speaking to a chief.
     *
     * Called from IGIH.scoutSpeakToChief.
     *
     * @param unit The <code>Unit</code> that was speaking.
     * @param settlement The <code>IndianSettlement</code> spoken to.
     * @param result The result.
     */
    public void scoutSpeakToChief(Unit unit, IndianSettlement settlement,
                                  String result) {
        switch (result) {
        case "":
            break;
        case "die":
            getGUI().showInformationMessage(settlement,
                "scoutSettlement.speakDie");
            break;
        case "expert":
            getGUI().showInformationMessage(settlement, StringTemplate
                .template("scoutSettlement.expertScout")
                .addNamed("%unit%", unit.getType()));
            break;
        case "tales":
            getGUI().showInformationMessage(settlement,
                "scoutSettlement.speakTales");
            break;
        case "nothing":
            getGUI().showInformationMessage(settlement, StringTemplate
                .template("scoutSettlement.speakNothing")
                .addStringTemplate("%nation%", unit.getOwner().getNationLabel()));
            break;
        default: // result == amount of gold
            getGUI().showInformationMessage(settlement, StringTemplate
                .template("scoutSettlement.speakBeads")
                .add("%amount%", result));
            break;
        }
    }
    
    /**
     * Selects a destination for this unit. Europe and the player's
     * colonies are valid destinations.
     *
     * Called from GotoAction.
     *
     * @param unit The unit for which to select a destination.
     * @return True if the destination change succeeds.
     */
    public boolean selectDestination(Unit unit) {
        if (!requireOurTurn() || unit == null) return false;

        if (!getGUI().confirmClearTradeRoute(unit)) return false;
        Location destination = getGUI().showSelectDestinationDialog(unit);
        if (destination == null) return false;

        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askSetDestination(unit, destination);
        if (ret) {
            if (destination instanceof Europe) {
                if (unit.hasTile()
                    && unit.getTile().isDirectlyHighSeasConnected()) {
                    moveTo(unit, destination);
                } else {
                    moveToDestination(unit, null);
                }
            } else {
                if (unit.isInEurope()) {
                    moveTo(unit, destination);
                } else {
                    moveToDestination(unit, null);
                }
            }
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Sells goods in Europe.
     *
     * Called from EuropePanel.MarketPanel, EuropePanel.unloadAction,
     * unload(), unloadCargo()
     *
     * @param goods The goods to be sold.
     * @return True if the sale succeeds.
     */
    public boolean sellGoods(Goods goods) {
        if (!requireOurTurn() || goods == null
            || !(goods.getLocation() instanceof Unit)) return false;

        final Player player = getMyPlayer();
        Unit carrier = (Unit)goods.getLocation();

        Europe europe = player.getEurope();
        EuropeWas europeWas = new EuropeWas(europe);
        UnitWas unitWas = new UnitWas(carrier);
        boolean ret = askUnloadGoods(goods.getType(), goods.getAmount(), carrier);
        if (ret) {
            sound("sound.event.sellCargo");
            europeWas.fireChanges();
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Sends a public chat message.
     *
     * Called from ChatPanel
     *
     * @param chat The text of the message.
     * @return True if the message was sent.
     */
    public boolean sendChat(String chat) {
        if (chat == null) return false;

        return askServer().chat(getMyPlayer(), chat);
    }

    /**
     * Changes the current construction project of a <code>Colony</code>.
     *
     * Called from BuildQueuePanel
     *
     * @param colony The <code>Colony</code>
     * @param buildQueue List of <code>BuildableType</code>
     * @return True if the build queue was changed.
     */
    public boolean setBuildQueue(Colony colony,
                                 List<BuildableType> buildQueue) {
        if (!requireOurTurn() || colony == null
            || buildQueue == null) return false;

        ColonyWas colonyWas = new ColonyWas(colony);
        boolean ret = askServer().setBuildQueue(colony, buildQueue);
        if (ret) {
            colonyWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Set a player to be the new current player.
     *
     * Called from IGIH.newTurn, IGIH.setCurrentPlayer, CC.login
     *
     * @param player The <code>Player</code> to be the new current player.
     * @return True if the current player changes.
     */
    public boolean setCurrentPlayer(Player player) {
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)
            && currentPlayerIsMyPlayer()) {
            getGUI().closeMenus();
        }

        final Game game = getGame();
        game.setCurrentPlayer(player);

        if (getMyPlayer().equals(player)) {
            FreeColDebugger.finishDebugRun(getFreeColClient(), false);
            if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.DESYNC)
                && DebugUtils.checkDesyncAction(getFreeColClient())) {
                getConnectController().reconnect();
                return false;
            }

            // Save the game (if it isn't newly loaded)
            if (getFreeColServer() != null
                && game.getTurn().getNumber() > 0) autoSaveGame();

            // Get turn report out quickly before more message display occurs.
            player.removeDisplayedModelMessages();
            displayModelMessages(true, true);

            player.invalidateCanSeeTiles();

            // Check for emigration.
            Europe europe = player.getEurope();
            if (player.hasAbility(Ability.SELECT_RECRUIT)) {
                emigration(player, 0, false);
            } else {
                while (player.checkEmigrate()) {
                    askEmigrate(europe,
                        Europe.MigrationType.getUnspecificSlot());
                }
            }
            
            // Wake up human!
            if (!getFreeColClient().isSinglePlayer()) {
                sound("sound.anthem." + player.getNationId());
            }

            player.resetIterators();
            updateGUI(player.getFallbackTile());
        }
        return true;
    }

    /**
     * Set a player to be dead.
     *
     * Called from IGIH.setDead
     *
     * @param dead The dead <code>Player</code>.
     * @return True if the player is marked as dead.
     */
    public boolean setDead(Player dead) {
        if (dead == null) return false;

        final Player player = getMyPlayer();
        if (player == dead) {
            FreeColDebugger.finishDebugRun(getFreeColClient(), true);
            if (getFreeColClient().isSinglePlayer()) {
                if (player.getPlayerType() == Player.PlayerType.RETIRED) {
                    ; // Do nothing, retire routine will quit

                } else if (player.getPlayerType() != Player.PlayerType.UNDEAD
                    && getGUI().confirm("defeatedSinglePlayer.text",
                                   "defeatedSinglePlayer.yes", "quit")) {
                    askServer().enterRevengeMode(getGame());
                } else {
                    getFreeColClient().quit();
                }
            } else {
                if (!getGUI().confirm("defeated.text", "defeated.yes",
                                 "quit")) getFreeColClient().quit();
            }
        } else {
            player.setStance(dead, null);
        }
        return true;
    }

    /**
     * Informs this controller that a game has been newly loaded.
     *
     * Called from ConnectController.startSavedGame
     *
     * No status returned to connect controller.
     */
    public void setGameConnected () {
        final Player player = getMyPlayer();
        if (player != null) {
            player.refilterModelMessages(getClientOptions());
        }
    }

    /**
     * Sets the export settings of the custom house.
     *
     * Called from WarehouseDialog
     *
     * @param colony The colony with the custom house.
     * @param goodsType The goods for which to set the settings.
     * @return True if the levels were set.
     */
    public boolean setGoodsLevels(Colony colony, GoodsType goodsType) {
        if (colony == null || goodsType == null) return false;

        return askServer().setGoodsLevels(colony,
                                          colony.getExportData(goodsType));
    }

    /**
     * Sets the debug mode to include the extra menu commands.
     *
     * Called from DebugAction
     *
     * @return True, always succeeds.
     */
    public boolean setInDebugMode() {
        FreeColDebugger.enableDebugMode(FreeColDebugger.DebugMode.MENUS);
        updateGUI(null);
        return true;
    }

    /**
     * Notify the player that the stance between two players has changed.
     *
     * Called from IGIH.setStance
     *
     * @param stance The changed <code>Stance</code>.
     * @param first The first <code>Player</code>.
     * @param second The second <code>Player</code>.
     * @return True if the stance change succeeds.
     */
    public boolean setStance(Stance stance, Player first, Player second) {
        if (stance == null || first == null || second == null) return false;

        final Player player = getMyPlayer();
        Stance old = first.getStance(second);
        try {
            first.setStance(second, stance);
        } catch (IllegalStateException e) {
            logger.log(Level.WARNING, "Illegal stance transition", e);
            return false;
        }
        if (player == first && old == Stance.UNCONTACTED) {
            sound("sound.event.meet." + second.getNationId());
        }
        return true;
    }

    /**
     * Spy on a colony.
     *
     * Called from IGIH.spyResult.
     *
     * @param tile A special copy of the <code>Tile</code> with the colony.
     */
    public void spyColony(Tile tile) {
        getGUI().showSpyColonyPanel(tile);
    }
    
    /**
     * Trains a unit of a specified type in Europe.
     *
     * Called from NewUnitPanel
     *
     * @param unitType The type of unit to be trained.
     * @return True if a new unit was trained.
     */
    public boolean trainUnitInEurope(UnitType unitType) {
        if (!requireOurTurn() || unitType == null) return false;

        final Player player = getMyPlayer();
        final Europe europe = player.getEurope();
        if (!player.checkGold(europe.getUnitPrice(unitType))) {
            getGUI().showInformationMessage("info.notEnoughGold");
            return false;
        }

        EuropeWas europeWas = new EuropeWas(europe);
        Unit newUnit = null;
        boolean ret = askServer().trainUnitInEurope(getGame(), unitType)
            && (newUnit = europeWas.getNewUnit()) != null;
        if (ret) {
            europeWas.fireChanges();
            player.setNextActiveUnit(newUnit);
            getGUI().setActiveUnit(newUnit);
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Unload, including dumping cargo.
     *
     * Called from UnloadAction, UnitLabel
     *
     * @param unit The <code>Unit</code> that is dumping.
     * @return True if the unit unloaded.
     */
    public boolean unload(Unit unit) {
        if (!requireOurTurn() || unit == null
            || !unit.isCarrier()) return false;

        boolean ret = true;
        Colony colony = unit.getColony();
        if (colony != null) { // In colony, unload units and goods.
            for (Unit u : unit.getUnitList()) {
                ret = leaveShip(u) && ret;
            }
            for (Goods goods : unit.getGoodsList()) {
                ret = unloadCargo(goods, false) && ret;
            }
        } else if (unit.isInEurope()) { // In Europe, unload non-boycotted goods
            final Player player = getMyPlayer();
            for (Goods goods : unit.getCompactGoodsList()) {
                if (player.canTrade(goods.getType())) {
                    ret = sellGoods(goods) && ret;
                }
            }
            if (unit.hasGoodsCargo()) { // Goods left here must be dumped.
                getGUI().showDumpCargoDialog(unit,
                    (List<Goods> goodsList) -> {
                        for (Goods g : goodsList) unloadCargo(g, true);
                    });
                return false;
            }
        } else { // Dump goods, units dislike jumping overboard
            for (Goods goods : unit.getGoodsList()) {
                ret = unloadCargo(goods, false) && ret;
            }
        }
        return ret;
    }

    /**
     * Unload cargo.  If the unit carrying the cargo is not in a
     * harbour, or if the given boolean is true, the goods will be
     * dumped.
     *
     * Called from CargoPanel, ColonyPanel, EuropePanel.MarketPanel,
     * GUI.showDumpCargoDialog, QuickActionMenu, unload()
     *
     * @param goods The <code>Goods</code> to unload.
     * @param dump If true, dump the goods.
     * @return True if the unload succeeds.
     */
    public boolean unloadCargo(Goods goods, boolean dump) {
        if (!requireOurTurn() || goods == null
            || goods.getAmount() <= 0
            || !(goods.getLocation() instanceof Unit)) return false;

        // Find the carrier
        final Unit carrier = (Unit)goods.getLocation();

        // Use Europe-specific routine if needed
        if (carrier.isInEurope()) return sellGoods(goods);

        // Check for a colony
        final Colony colony = carrier.getColony();

        // Unload
        ColonyWas colonyWas = (colony == null) ? null : new ColonyWas(colony);
        UnitWas unitWas = new UnitWas(carrier);
        boolean ret = askUnloadGoods(goods.getType(), goods.getAmount(), carrier);
        if (ret) {
            if (!dump) sound("sound.event.unloadCargo");
            if (colonyWas != null) colonyWas.fireChanges();
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }

    /**
     * Updates a trade route.
     *
     * Called from TradeRoutePanel(), TradeRoutePanel.newRoute
     *
     * @param route The trade route to update.
     * @return True if the trade route was updated.
     */
    public boolean updateTradeRoute(TradeRoute route) {
        if (route == null) return false;

        return askServer().updateTradeRoute(route);
    }

    /**
     * The player has won, show the high scores and victory dialog.
     *
     * Called from IGIH.gameEnded.
     *
     * @param score If "true", a new high score was reached.
     */
    public void victory(String score) {
        displayHighScores("true".equalsIgnoreCase(score));
        getGUI().showVictoryDialog((Boolean result) -> victory(result));
    }

    /**
     * The player has won!
     *
     * Called from GUI.showVictoryDialog
     *
     * @param quit If true, leave this game and start a new one.
     * @return True.
     */
    public boolean victory(Boolean quit) {
        if (quit) {
            getFreeColClient().newGame(false);
        } else {
            askServer().continuePlaying();
        }
        return true;
    }
        
    /**
     * Tell a unit to wait.
     *
     * Called from WaitAction.
     *
     * @return True, this can not fail.
     */
    public boolean waitUnit() {
        if (!requireOurTurn()) return false;

        // Defeat the normal check for whether the current unit can move.
        getGUI().setActiveUnit(null);
        updateGUI(null);
        return true;
    }

    /**
     * Moves a <code>Unit</code> to a <code>WorkLocation</code>.
     *
     * Called from ColonyPanel.tryWork, UnitLabel
     *
     * @param unit The <code>Unit</code>.
     * @param workLocation The new <code>WorkLocation</code>.
     * @return True if the unit is now working at the new work location.
     */
    public boolean work(Unit unit, WorkLocation workLocation) {
        if (!requireOurTurn() || unit == null
            || workLocation == null) return false;

        StringTemplate template;
        if (unit.getStudent() != null
            && !getGUI().confirmAbandonEducation(unit, false)) return false;

        Colony colony = workLocation.getColony();
        if (workLocation instanceof ColonyTile) {
            Tile tile = ((ColonyTile)workLocation).getWorkTile();
            if (tile.hasLostCityRumour()) {
                getGUI().showInformationMessage("tileHasRumour");
                return false;
            }
            if (!unit.getOwner().owns(tile)) {
                if (!claimTile(tile, colony)) return false;
            }
        }

        // Try to change the work location.
        ColonyWas colonyWas = new ColonyWas(colony);
        UnitWas unitWas = new UnitWas(unit);
        boolean ret = askServer().work(unit, workLocation)
            && unit.getLocation() == workLocation;
        if (ret) {
            colonyWas.fireChanges();
            unitWas.fireChanges();
            updateGUI(null);
        }
        return ret;
    }
}
