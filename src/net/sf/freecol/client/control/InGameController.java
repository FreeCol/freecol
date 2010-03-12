/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.animation.Animations;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.Canvas.BuyAction;
import net.sf.freecol.client.gui.Canvas.ClaimAction;
import net.sf.freecol.client.gui.Canvas.EventType;
import net.sf.freecol.client.gui.Canvas.MissionaryAction;
import net.sf.freecol.client.gui.Canvas.ScoutColonyAction;
import net.sf.freecol.client.gui.Canvas.ScoutIndianSettlementAction;
import net.sf.freecol.client.gui.Canvas.SellAction;
import net.sf.freecol.client.gui.Canvas.TradeAction;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.FreeColActionUI;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.sound.SoundLibrary.SoundEffect;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileItemContainer;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TransactionListener;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.CombatModel.CombatResultType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.TradeRoute.Stop;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.networking.AbandonColonyMessage;
import net.sf.freecol.common.networking.AskSkillMessage;
import net.sf.freecol.common.networking.BuildColonyMessage;
import net.sf.freecol.common.networking.BuyGoodsMessage;
import net.sf.freecol.common.networking.BuyMessage;
import net.sf.freecol.common.networking.BuyPropositionMessage;
import net.sf.freecol.common.networking.CashInTreasureTrainMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.ClaimLandMessage;
import net.sf.freecol.common.networking.ClearSpecialityMessage;
import net.sf.freecol.common.networking.CloseTransactionMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DebugForeignColonyMessage;
import net.sf.freecol.common.networking.DeclareIndependenceMessage;
import net.sf.freecol.common.networking.DeliverGiftMessage;
import net.sf.freecol.common.networking.DemandTributeMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.DisembarkMessage;
import net.sf.freecol.common.networking.EmbarkMessage;
import net.sf.freecol.common.networking.EmigrateUnitMessage;
import net.sf.freecol.common.networking.GetTransactionMessage;
import net.sf.freecol.common.networking.GoodsForSaleMessage;
import net.sf.freecol.common.networking.InciteMessage;
import net.sf.freecol.common.networking.JoinColonyMessage;
import net.sf.freecol.common.networking.LearnSkillMessage;
import net.sf.freecol.common.networking.LoadCargoMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MissionaryMessage;
import net.sf.freecol.common.networking.MoveMessage;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.networking.RenameMessage;
import net.sf.freecol.common.networking.ScoutIndianSettlementMessage;
import net.sf.freecol.common.networking.SellGoodsMessage;
import net.sf.freecol.common.networking.SellMessage;
import net.sf.freecol.common.networking.SellPropositionMessage;
import net.sf.freecol.common.networking.SetDestinationMessage;
import net.sf.freecol.common.networking.SpySettlementMessage;
import net.sf.freecol.common.networking.StatisticsMessage;
import net.sf.freecol.common.networking.UnloadCargoMessage;
import net.sf.freecol.common.networking.UpdateCurrentStopMessage;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The controller that will be used while the game is played.
 */
public final class InGameController implements NetworkConstants {

    private static final Logger logger = Logger.getLogger(InGameController.class.getName());

    private final FreeColClient freeColClient;

    private final short UNIT_LAST_MOVE_DELAY = 300;
    
    /**
     * Sets that the turn will be ended when all going-to units have been moved.
     */
    private boolean endingTurn = false;

    /**
     * If true, then at least one unit has been active and the turn may automatically be ended.
     */
    private boolean canAutoEndTurn = false;
    
    /**
     * Sets that all going-to orders should be executed.
     */
    private boolean executeGoto = false;

    /**
     * A hash map of messages to be ignored.
     */
    private HashMap<String, Integer> messagesToIgnore = new HashMap<String, Integer>();

    /**
     * A list of save game files.
     */
    private ArrayList<File> allSaveGames = new ArrayList<File>();

    /**
     * The constructor to use.
     * 
     * @param freeColClient The main controller.
     */
    public InGameController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
    }

    /**
     * Opens a dialog where the user should specify the filename and
     * saves the game.
     */
    public void saveGame() {
        Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();
        String fileName = player.getName() + "_" + Messages.message(player.getNationName())
            + "_" + getSaveGameString(freeColClient.getGame().getTurn());
        fileName = fileName.replaceAll(" ", "_");
        if (freeColClient.canSaveCurrentGame()) {
            final File file = canvas.showSaveDialog(FreeCol.getSaveDirectory(), fileName);
            if (file != null) {
                FreeCol.setSaveDirectory(file.getParentFile());
                saveGame(file);
            }
        }
    }

    /**
     * Saves the game to the given file.
     * 
     * @param file The <code>File</code>.
     */
    public void saveGame(final File file) {
        Canvas canvas = freeColClient.getCanvas();
        canvas.showStatusPanel(Messages.message("status.savingGame"));
        try {
            freeColClient.getFreeColServer().saveGame(file, freeColClient.getMyPlayer().getName());
            canvas.closeStatusPanel();
        } catch (IOException e) {
            canvas.errorMessage("couldNotSaveGame");
        }
        canvas.requestFocusInWindow();
    }

    /**
     * Opens a dialog where the user should specify the filename and
     * loads the game.
     */
    public void loadGame() {
        Canvas canvas = freeColClient.getCanvas();
        File file = canvas.showLoadDialog(FreeCol.getSaveDirectory());
        if (file == null) {
            return;
        }
        if (!file.isFile()) {
            canvas.errorMessage("fileNotFound");
            return;
        }
        if (!canvas.showConfirmDialog("stopCurrentGame.text",
                                      "stopCurrentGame.yes",
                                      "stopCurrentGame.no")) {
            return;
        }

        freeColClient.getConnectController().quitGame(true);
        canvas.removeInGameComponents();
        freeColClient.getConnectController().loadGame(file);
    }

    
    /**
     * Sets the "debug mode" to be active or not. Calls
     * {@link FreeCol#setInDebugMode(boolean)} and reinitialize the
     * <code>FreeColMenuBar</code>.
     * 
     * @param debug Set to <code>true</code> to enable debug mode.
     */
    public void setInDebugMode(boolean debug) {
        FreeCol.setInDebugMode(debug);
        freeColClient.updateMenuBar();
    }


    /**
     * Sends the specified message to the server and returns the reply,
     * if it has the specified tag.
     * Handle "error" replies if they have a messageID or when in debug mode.
     * This routine allows code simplification in much of the following
     * client-server communication.
     *
     * In following routines we follow the convention that server I/O
     * is confined to the ask<foo>() routine, which typically returns
     * true if the server interaction succeeded, which does *not*
     * necessarily imply that the actual substance of the request was
     * allowed (e.g. a move may result in the death of a unit rather
     * than actually moving).
     *
     * @param client a <code>Client</code> value
     * @param element The <code>Element</code> (root element in a
     *        DOM-parsed XML tree) that holds all the information
     * @param tag The expected tag
     * @return The answer from the server if it has the specified tag,
     *         otherwise <code>null</code>.
     */
    private Element askExpecting(Client client, Element element, String tag) {
        // Send the element, return null on failure or null return.
        Element reply = null;
        try {
            reply = client.ask(element);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not send " + element, e);
            return null;
        }
        if (reply == null) {
            logger.warning("Received null reply to " + element);
            return null;
        }

        // Process explicit errors.
        if ("error".equals(reply.getTagName())) {
            String messageId = reply.getAttribute("messageID");
            String message = reply.getAttribute("message");
            if (FreeCol.isInDebugMode() && messageId != null && message != null) {
                // If debugging suppress the bland failure in favour
                // of the higher detail text.
                reply.removeAttribute("messageID");
            }
            if (messageId == null && message == null) {
                logger.warning("Received null error response");
            } else {
                logger.warning("Received error response: "
                               + ((messageId != null) ? messageId : "")
                               + "/" + ((message != null) ? message : ""));
                Connection conn = client.getConnection();
                freeColClient.getInGameInputHandler().handle(conn, reply);
            }
            return null;
        }

        // Success!
        if (tag == null || tag.equals(reply.getTagName())) {
            return reply;
        }

        // Unexpected reply.  Whine and fail.
        String complaint = "Received reply with tag " + reply.getTagName()
            + " which should have been " + tag
            + " to message " + element;
        logger.warning(complaint);
        if (FreeCol.isInDebugMode()) {
            freeColClient.getCanvas().errorMessage(null, complaint);
        }            
        return null;
    }


    /**
     * Set a player to be the new current player.
     *
     * @param player The <code>Player</code> to be the new current player.
     */
    public void setCurrentPlayer(Player player) {
        logger.finest("Entering client setCurrentPlayer: " + player.getName());
        Game game = freeColClient.getGame();
        game.setCurrentPlayer(player);

        if (freeColClient.getMyPlayer().equals(player)) {
            // Autosave the game.
            ClientOptions options = freeColClient.getClientOptions();
            int savegamePeriod = options.getInteger(ClientOptions.AUTOSAVE_PERIOD);
            int turnNumber = game.getTurn().getNumber();
            if (savegamePeriod <= 1
                || (savegamePeriod != 0 && turnNumber % savegamePeriod == 0)) {
                String filename = Messages.message("clientOptions.savegames.autosave.fileprefix")
                    + '-' + getSaveGameString(game.getTurn()) + ".fsg";
                File saveGameFile = new File(FreeCol.getAutosaveDirectory(),
                                             filename);
                saveGame(saveGameFile);
                int generations = options.getInteger(ClientOptions.AUTOSAVE_GENERATIONS);
                if (generations > 0) {
                    allSaveGames.add(saveGameFile);
                    if (allSaveGames.size() > generations) {
                        File fileToDelete = allSaveGames.remove(0);
                        fileToDelete.delete();
                    }
                }
            }

            // Removes the units we cannot see anymore from the map.
            Map map = game.getMap();
            player.resetCanSeeTiles();
            Iterator<Position> tileIterator = map.getWholeMapIterator();
            while (tileIterator.hasNext()) {
                Tile t = map.getTile(tileIterator.next());
                if (t != null && !player.canSee(t)
                    && t.getFirstUnit() != null) {
                    if (t.getFirstUnit().getOwner() == player) {
                        logger.warning("Could not see one of my own units!");
                    }
                    t.disposeAllUnits();
                }
            }
            player.resetCanSeeTiles();

            // Check for emigration.
            if (player.checkEmigrate()) {
                if (player.hasAbility("model.ability.selectRecruit")
                    && player.getEurope().recruitablesDiffer()) {
                    Canvas canvas = freeColClient.getCanvas();
                    int index = canvas.showEmigrationPanel(false);
                    askEmigrate(index + 1);
                } else {
                    askEmigrate(0);
                }
            }

            // Follow trade routes for units in Europe.
            // TODO: Why is Europe special?
            Europe europe = player.getEurope();
            if (europe != null) {
                for (Unit unit : europe.getUnitList()) {
                    if (unit.getTradeRoute() != null && unit.isInEurope()) {
                        // Must call isInEurope to filter out units in
                        // Europe but in the TO_EUROPE or TO_AMERICA states.
                        followTradeRoute(unit);
                    }
                }
            }

            // GUI management.
            if (!freeColClient.isSingleplayer()) {
                freeColClient.playSound(player.getNation().getAnthem());
            }
            displayModelMessages(true);
            nextActiveUnit();
        }
        logger.finest("Exiting client setCurrentPlayer: " + player.getName());
    }

    /**
     * Handle server query-response for emigration.
     *
     * @param slot The slot from which the unit migrates, 1-3 selects
     *             a specific one, otherwise the server will choose one.
     */
    private void askEmigrate(int slot) {
        Client client = freeColClient.getClient();
        EmigrateUnitMessage message = new EmigrateUnitMessage(slot);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
    }

    /**
     * Follow a trade route.
     *
     * @param unit The <code>Unit</code> to move.
     */
    private void followTradeRoute(Unit unit) {
        // Complain and return if the stop is no longer valid.
        Canvas canvas = freeColClient.getCanvas();
        Stop stop = unit.getStop();
        if (!TradeRoute.isStopValid(unit, stop)) {
            String name = unit.getTradeRoute().getName();
            canvas.showInformationMessage("traderoute.broken", unit,
                                          "%name%", name);
            return;
        }

        // Return if the stop is Europe but the unit has not arrived yet.
        if (freeColClient.getMyPlayer().getEurope() == stop.getLocation()
            && !unit.isInEurope()) {
            return;
        }

        if (unit.getInitialMovesLeft() == unit.getMovesLeft()) {
            // The unit was already at the stop at the beginning of the
            // turn, so loading is allowed.
            loadUnitAtStop(unit);

            // Update to next stop, and move towards it unless the
            // unit is already there.
            if (askUpdateCurrentStop(unit)) {
                Stop nextStop = unit.getStop();
                if (nextStop != null
                    && nextStop.getLocation() != unit.getColony()) {
                    if (unit.isInEurope()) {
                        moveToAmerica(unit);
                    } else {
                        moveToDestination(unit);
                    }
                }
            }
        } else {
            // The unit has just arrived, stop here and unload.
            unloadUnitAtStop(unit);
        }

        // Pretend to be finished moving.  This is a client-side
        // convenience and does not need to be in the server.
        unit.setMovesLeft(0);
    }

    /**
     * Handle server query-response for updating the current stop.
     *
     * @param unit The <code>Unit</code> whose stop is to be updated.
     * @return True if the query-response succeeds.
     */
    private boolean askUpdateCurrentStop(Unit unit) {
        Client client = freeColClient.getClient();
        UpdateCurrentStopMessage message = new UpdateCurrentStopMessage(unit);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "update");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Work out what goods to load onto a unit at a stop, and load them.
     *
     * @param unit The <code>Unit</code> to load.
     */
    private void loadUnitAtStop(Unit unit) {
        // Copy the list of goods types to load at this stop.
        Stop stop = unit.getStop();
        ArrayList<GoodsType> goodsTypesToLoad
            = new ArrayList<GoodsType>(stop.getCargo());

        // First handle partial loads.
        // For each cargo the unit is already carrying, and which is
        // not to be unloaded at this stop, check if the cargo is
        // completely full and if not, try to fill to capacity.
        Colony colony = unit.getColony();
        Game game = freeColClient.getGame();
        ArrayList<Goods> loaded = new ArrayList<Goods>();
        for (Goods goods : unit.getGoodsList()) {
            GoodsType type = goods.getType();
            int index, toLoad;
            if ((toLoad = GoodsContainer.CARGO_SIZE - goods.getAmount()) > 0
                && (index = goodsTypesToLoad.indexOf(type)) >= 0) {
                int atStop = (colony == null) ? Integer.MAX_VALUE // Europe
                    : colony.getExportAmount(type);
                if (atStop > 0) {
                    Goods cargo = new Goods(game, colony, type,
                                            Math.min(toLoad, atStop));
                    if (loadGoods(cargo, colony, unit)) {
                        loaded.add(cargo);
                    }
                }
                // Do not try to load this goods type again.  Either
                // it has already succeeded, or it can not ever
                // succeed because there is nothing available.
                goodsTypesToLoad.remove(index);
            }
        }

        // Then fill any remaining empty cargo slots.
        for (GoodsType type : goodsTypesToLoad) {
            if (unit.getSpaceLeft() <= 0) break; // Full
            int toLoad = GoodsContainer.CARGO_SIZE;
            int atStop = (colony == null) ? Integer.MAX_VALUE // Europe
                : colony.getExportAmount(type);
            if (atStop > 0) {
                Goods cargo = new Goods(game, colony, type,
                                        Math.min(toLoad, atStop));
                if (loadGoods(cargo, colony, unit)) {
                    loaded.add(cargo);
                }
            }
        }

        // Report on what has been loaded.
        if (!loaded.isEmpty()) {
            logger.fine("Load " + unit.getId()
                        + " in trade route " + unit.getTradeRoute().getName()
                        + " at " + stop.getLocation().getLocationName()
                        + " goods " + goodsSummary(loaded));
        }
    }

    /**
     * Load some goods onto a carrier.
     *
     * @param goods The <code>Goods</code> to load.
     * @param colony The <code>Colony</code> to load from,
     *               or null if in Europe.
     * @param carrier The <code>Unit</code> to load onto.
     * @return True if the load succeeded.
     */
    private boolean loadGoods(Goods goods, Colony colony, Unit carrier) {
        if (colony == null) {
            return buyGoods(goods.getType(), goods.getAmount(), carrier);
        }
        GoodsType type = goods.getType();
        GoodsContainer container = colony.getGoodsContainer();
        int oldAmount = container.getGoodsCount(type);
        int newAmount;
        if (askLoadCargo(goods, carrier)
            && (newAmount = container.getGoodsCount(type)) != oldAmount) {
            carrier.firePropertyChange(Unit.CARGO_CHANGE, null, goods);
            colony.firePropertyChange(type.getId(), oldAmount, newAmount);
            return true;
        }
        return false;
    }

    /**
     * Handle server query-response for loading cargo.
     *
     * @param goods The <code>Goods</code> to load.
     * @param carrier The <code>Unit</code> to load onto.
     * @return True if the query-response succeeds.
     */
    private boolean askLoadCargo(Goods goods, Unit carrier) {
        Client client = freeColClient.getClient();
        LoadCargoMessage message = new LoadCargoMessage(goods, carrier);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "update");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Work out what goods to unload from a unit at a stop, and unload them.
     *
     * @param unit The <code>Unit</code> to unload.
     */
    private void unloadUnitAtStop(Unit unit) {
        Colony colony = unit.getColony();
        Stop stop = unit.getStop();
        final ArrayList<GoodsType> goodsTypesToLoad = stop.getCargo();

        // Unload everything that is on the carrier but not listed to
        // be loaded at this stop.
        Game game = freeColClient.getGame();
        ArrayList<Goods> unloaded = new ArrayList<Goods>();
        for (Goods goods : new ArrayList<Goods>(unit.getGoodsList())) {
            GoodsType type = goods.getType();
            if (goodsTypesToLoad.contains(type)) {
                continue; // Keep this cargo.
            }

            int atStop = (colony == null) ? Integer.MAX_VALUE // Europe
                : colony.getImportAmount(type);
            int toUnload = goods.getAmount();
            if (toUnload > atStop) {
                // Unloading here will overflow the colony warehouse
                // (can not be Europe!).  Decide whether to unload the
                // whole cargo or not.
                Canvas canvas = freeColClient.getCanvas();
                String locName = colony.getName();
                String overflowMessage = null;
                String overflow = Integer.toString(toUnload - atStop);
                int option = freeColClient.getClientOptions()
                    .getInteger(ClientOptions.UNLOAD_OVERFLOW_RESPONSE);
                switch (option) {
                case ClientOptions.UNLOAD_OVERFLOW_RESPONSE_ASK:
                    StringTemplate template =
                        StringTemplate.template("traderoute.warehouseCapacity")
                        .addStringTemplate("%unit%", Messages.getLabel(unit))
                        .addName("%colony%", locName)
                        .addName("%amount%", overflow)
                        .add("%goods%", goods.getNameKey());
                    if (!canvas.showConfirmDialog(colony.getTile(), template,
                                                  "yes", "no")) {
                        toUnload = atStop;
                    }
                    break;
                case ClientOptions.UNLOAD_OVERFLOW_RESPONSE_NEVER:
                    toUnload = atStop;
                    overflowMessage = "traderoute.nounload";
                    break;
                case ClientOptions.UNLOAD_OVERFLOW_RESPONSE_ALWAYS:
                    overflowMessage = "traderoute.overflow";
                    break;
                default:
                    logger.warning("Illegal UNLOAD_OVERFLOW_RESPONSE: "
                                   + Integer.toString(option));
                    break;
                }
                if (overflowMessage != null) {
                    Player player = freeColClient.getMyPlayer();
                    ModelMessage m =
                        new ModelMessage(ModelMessage.MessageType.WAREHOUSE_CAPACITY,
                                         overflowMessage, player)
                        .add("%colony%", locName)
                        .addStringTemplate("%unit%", Messages.getLabel(unit))
                        .addName("%overflow%", overflow)
                        .add("%goods%", goods.getNameKey());
                    player.addModelMessage(m);
                }
            }

            // Try to unload.
            Goods cargo = (goods.getAmount() == toUnload) ? goods
                : new Goods(game, unit, type, toUnload);
            if (unloadGoods(cargo, unit, colony)) {
                unloaded.add(cargo);
            }
        }

        // Report on what was unloaded.
        if (!unloaded.isEmpty()) {
            logger.fine("Unload " + unit.getId()
                        + " in trade route " + unit.getTradeRoute().getName()
                        + " at " + stop.getLocation().getLocationName()
                        + " goods " + goodsSummary(unloaded));
        }
    }

    /**
     * Unload some goods from a carrier.
     *
     * @param goods The <code>Goods</code> to unload.
     * @param carrier The <code>Unit</code> carrying the goods.
     * @param colony The <code>Colony</code> to unload to,
     *               or null if unloading in Europe.
     * @return True if the unload succeeded.
     */
    private boolean unloadGoods(Goods goods, Unit carrier, Colony colony) {
        if (colony == null && carrier.isInEurope()) {
            return sellGoods(goods);
        }
        GoodsType type = goods.getType();
        int oldAmount = (colony == null) ? 0 
            : colony.getGoodsContainer().getGoodsCount(type);
        if (askUnloadCargo(goods)
            && (carrier.getGoodsContainer().getGoodsCount(type)) == 0) {
            carrier.firePropertyChange(Unit.CARGO_CHANGE, goods, null);
            if (colony != null) {
                colony.firePropertyChange(type.getId(), oldAmount,
                                          colony.getGoodsContainer().getGoodsCount(type));
            }
            return true;
        }
        return false;
    }

    /**
     * Handle server query-response for unloading cargo.
     *
     * @param goods The <code>Goods</code> to unload.
     * @return True if the query-response succeeds.
     */
    private boolean askUnloadCargo(Goods goods) {
        Client client = freeColClient.getClient();
        UnloadCargoMessage message = new UnloadCargoMessage(goods);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "update");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Build a string summarizing the contents of a list of goods.
     */
    private String goodsSummary(List<Goods> goodsList) {
        String result = "";
        final String separator = " + ";
        for (Goods goods : goodsList) {
            result += Integer.toString(goods.getAmount())
                + " " + goods.getType() + separator;
        }
        return result.substring(0, result.length() - separator.length());
    }


    /**
     * Moves the active unit in a specified direction. This may result in an
     * attack, move... action.
     *
     * @param direction The direction in which to move the active unit.
     */
    public void moveActiveUnit(Direction direction) {
        if (freeColClient.getGame().getCurrentPlayer()
            != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Unit unit = freeColClient.getGUI().getActiveUnit();
        if (unit != null) {
            clearGotoOrders(unit);
            move(unit, direction);

            // Centers unit if option "always center" is active.  A
            // few checks need to be remade, as the unit may no longer
            // exist or no longer be on the map.
            boolean alwaysCenter = freeColClient.getClientOptions().getBoolean(ClientOptions.ALWAYS_CENTER);
            if (alwaysCenter && unit.getTile() != null) {
                centerOnUnit(unit);
            }
        } // else: nothing: There is no active unit that can be moved.
    }

    /**
     * Selects a destination for this unit. Europe and the player's
     * colonies are valid destinations.
     * 
     * @param unit The unit for which to select a destination.
     */
    public void selectDestination(Unit unit) {
        final Player player = freeColClient.getMyPlayer();
        Map map = freeColClient.getGame().getMap();
        Canvas canvas = freeColClient.getCanvas();
        Location destination = canvas.showSelectDestinationDialog(unit);
        if (destination == null) {
            // user aborted
            return;
        }

        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            setDestination(unit, destination);
            return;
        }

        if (destination instanceof Europe && unit.getTile() != null
            && (unit.getTile().canMoveToEurope() || map.isAdjacentToMapEdge(unit.getTile()))) {
            moveToEurope(unit);
            nextActiveUnit();
        } else {
            setDestination(unit, destination);
            moveToDestination(unit);
        }
    }


    /**
     * Moves the given unit towards the destination given by
     * {@link Unit#getDestination()}.
     *
     * @param unit The <code>Unit</code> to move.
     */
    public void moveToDestination(Unit unit) {
        Canvas canvas = freeColClient.getCanvas();
        if (freeColClient.getGame().getCurrentPlayer()
            != freeColClient.getMyPlayer()) {
            canvas.showInformationMessage("notYourTurn");
            return;
        }

        if (unit.getTradeRoute() != null) {
            Stop currStop = unit.getStop();
            if (!TradeRoute.isStopValid(unit, currStop)) {
                String oldTradeRouteName = unit.getTradeRoute().getName();
                logger.warning("Trade unit " + unit.getId()
                               + " in route " + oldTradeRouteName
                               + " cannot continue: stop invalid.");
                canvas.showInformationMessage("traderoute.broken", unit,
                                              "%name%", oldTradeRouteName);
                clearOrders(unit);
                return;
            }

            // If the unit was activated the destination is cleared,
            // make sure we reset the destination to the next stop,
            // which will avoid the unit being considered an "active" unit.
            if (unit.getDestination() == null) {
                unit.setDestination(currStop.getLocation());
            }
        	
            if (unit.getLocation().getTile() == currStop.getLocation().getTile()) {
                // Trade unit is at current stop
                logger.info("Trade unit " + unit.getId()
                            + " in route " + unit.getTradeRoute().getName()
                            + " is at " + unit.getStop().getLocation().getLocationName());
                followTradeRoute(unit);
                return;
            } else {
                logger.info("Unit " + unit.getId()
                            + " is a trade unit in route " + unit.getTradeRoute().getName()
                            + ", going to " + unit.getStop().getLocation().getLocationName());
            }
        } else {
            logger.info("Moving unit " + unit.getId()
                        + " to position " + unit.getDestination().getLocationName());
        }

        // Destination is either invalid (like an abandoned colony,
        // for example) or is current tile.
        final Location destination = unit.getDestination();
        if (!(destination instanceof Europe)
            && (destination.getTile() == null
                || unit.getTile() == destination.getTile())) {
            clearGotoOrders(unit);
            return;
        }
        
        final Map map = freeColClient.getGame().getMap();
        PathNode path;
        if (destination instanceof Europe) {
            path = map.findPathToEurope(unit, unit.getTile());
        } else {
            path = map.findPath(unit, unit.getTile(), destination.getTile());
        }

        if (path == null) {
            canvas.showInformationMessage(StringTemplate.template("selectDestination.failed")
                                          .addStringTemplate("%destination%", destination.getLocationName()),
                                          unit);
            setDestination(unit, null);
            return;
        }

        while (path != null) {
            MoveType mt = unit.getMoveType(path.getDirection());
            switch (mt) {
            case MOVE:
                moveMove(unit, path.getDirection());
                break;
            case EXPLORE_LOST_CITY_RUMOUR:
                moveExplore(unit, path.getDirection());
                if (unit.isDisposed())
                    return;
                break;
            case MOVE_HIGH_SEAS:
                if (destination instanceof Europe) {
                    moveToEurope(unit);
                    path = null;
                } else if (path == path.getLastNode()) {
                    move(unit, path.getDirection());
                    path = null;
                } else {
                    moveMove(unit, path.getDirection());
                }
                break;
            case MOVE_NO_MOVES:
                // The unit may have some moves left,
                // but not enough to move to the destination.
                unit.setMovesLeft(0); //TODO: should be in server
                return;
            default:
                if (path == path.getLastNode() && mt.isLegal()
                    && (mt != MoveType.ATTACK || knownEnemyOnLastTile(path))) {
                    move(unit, path.getDirection());
                    // unit may have been destroyed while moving
                    if (unit.isDisposed()) {
                        return;
                    }
                } else {
                    freeColClient.getGUI().setActiveUnit(unit);
                    return;
                }
            }
            if (path != null) {
                path = path.next;
            }
        }

        if (unit.getTile() != null && destination instanceof Europe
            && map.isAdjacentToMapEdge(unit.getTile())) {
            moveToEurope(unit);
        }

        // We have reached our destination.
        // If in a trade route, unload and update next stop.
        if (unit.getTradeRoute() == null) {
            setDestination(unit, null);
        } else {
            followTradeRoute(unit);
        }

        // Display a "cash in"-dialog if a treasure train have been
        // moved into a coastal colony:
        if (checkCashInTreasureTrain(unit)) {
            unit = null;
        }

        if (unit != null && unit.getMovesLeft() > 0 && unit.getTile() != null) {
            freeColClient.getGUI().setActiveUnit(unit);
        } else if (unit == null || freeColClient.getGUI().getActiveUnit() == unit) {
            nextActiveUnit();
        }
        return;
    }

    private boolean knownEnemyOnLastTile(PathNode path) {
        if ((path != null) && path.getLastNode() != null) {
            Tile tile = path.getLastNode().getTile();
            return ((tile.getFirstUnit() != null &&
                     tile.getFirstUnit().getOwner() != freeColClient.getMyPlayer()) ||
                    (tile.getSettlement() != null &&
                     tile.getSettlement().getOwner() != freeColClient.getMyPlayer()));
        } else {
            return false;
        }
    }


    // Public user actions that may require interactive confirmation
    // before requesting an update from the server.

    /**
     * Declares independence for the home country.
     *
     * TODO: Move magic 50% number to the spec.
     */
    public void declareIndependence() {
        Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();
        if (freeColClient.getGame().getCurrentPlayer() != player) {
            canvas.showInformationMessage("notYourTurn");
            return;
        }

        // Check for adequate support.
        if (player.getSoL() < 50) {
            canvas.showInformationMessage("declareIndependence.notMajority",
                FreeCol.getSpecification().getGoodsType("model.goods.bells"),
                "%percentage%", Integer.toString(player.getSoL()));
            return;
        }
        if (!player.hasCoastalColony()) {
            canvas.showInformationMessage("declareIndependence.needCoastalColony");
            return;
        }

        // Confirm intention, and collect nation+country names.
        List<String> names = canvas.showConfirmDeclarationDialog();
        if (names == null
            || names.get(0) == null || names.get(0).length() == 0
            || names.get(1) == null || names.get(1).length() == 0) {
            // Empty name => user cancelled.
            return;
        }

        // Ask server.
        String nationName = names.get(0);
        String countryName = names.get(1);
        if (askDeclare(nationName, countryName)
            && player.getPlayerType() == PlayerType.REBEL) {
            canvas.showDeclarationDialog();
            freeColClient.getActionManager().update();
            nextModelMessage();
        }
    }

    /**
     * Handle server query-response for declaring independence.
     *
     * @param nation The name for the new nation.
     * @param country The name for the new country.
     * @return True if the server interaction succeeded.
     */
    private boolean askDeclare(String nation, String country) {
        Client client = freeColClient.getClient();
        DeclareIndependenceMessage message
            = new DeclareIndependenceMessage(nation, country);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Sends a public chat message.
     *
     * @param message The text of the message.
     */
    public void sendChat(String message) {
        ChatMessage chatMessage
            = new ChatMessage(freeColClient.getMyPlayer(), message, false);
        freeColClient.getClient().sendAndWait(chatMessage.toXMLElement());
    }


    /**
     * Renames a <code>Nameable</code>.
     * Apparently this can be done while it is not your turn.
     *
     * @param object The object to rename.
     */
    public void rename(Nameable object) {
        Player player = freeColClient.getMyPlayer();
        if (!(object instanceof Ownable)
            || ((Ownable) object).getOwner() != player) {
            return;
        }

        Canvas canvas = freeColClient.getCanvas();
        String name = null;
        if (object instanceof Colony) {
            Colony colony = (Colony) object;
            name = canvas.showInputDialog(colony.getTile(),
                                          "renameColony.text",
                                          colony.getName(),
                                          "renameColony.yes", "renameColony.no",
                                          true);
            if (name == null) return; // User cancelled, 0-length invalid.
            if (player.getSettlement(name) != null) {
                // Colony name must be unique.
                canvas.showInformationMessage("nameColony.notUnique",
                                              (Colony) object,
                                              "%name%", name);
                return;
            }
        } else if (object instanceof Unit) {
            Unit unit = (Unit) object;
            name = canvas.showInputDialog(unit.getTile(),
                                          "renameUnit.text",
                                          unit.getName(),
                                          "renameUnit.yes", "renameUnit.no",
                                          false);
            if (name == null) return; // User cancelled, 0-length clears name.
        } else {
            logger.warning("Tried to rename an unsupported Nameable: "
                           + object.toString());
            return;
        }

        askRename((FreeColGameObject) object, name);
    }

    /**
     * Server query-response for renaming an object.
     *
     * @param object A <code>FreeColGameObject</code> to rename.
     * @param name The name to apply.
     * @return True if the renaming succeeded.
     */
    private boolean askRename(FreeColGameObject object, String name) {
        RenameMessage message = new RenameMessage(object, name);
        Client client = freeColClient.getClient();
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "update");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Use the active unit to build a colony.
     */
    public void buildColony() {
        Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();
        if (freeColClient.getGame().getCurrentPlayer() != player) {
            canvas.showInformationMessage("notYourTurn");
            return;
        }

        // Check unit can build, and is on the map.
        // Show the colony warnings if required.
        GUI gui = freeColClient.getGUI();
        Unit unit = gui.getActiveUnit();
        if (unit == null) return;
        Tile tile = unit.getTile();
        if (tile == null) return;
        if (tile.getColony() != null) {
            askJoinColony(unit, tile.getColony());
            return;
        }
        if (!unit.canBuildColony()) return;
        if (freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_COLONY_WARNINGS)
            && !showColonyWarnings(tile, unit)) {
            return;
        }
        
        // check ownership of central tile
        if(tile.getOwner() != null && tile.getOwner()!= player){
        	boolean isNewOwner = claimLand(tile, null, 0);
        	if(!isNewOwner){
        		return;
        	}
        }

        // Get and check the name.
        String name = canvas.showInputDialog(tile, "nameColony.text",
                                             Messages.getDefaultSettlementName(player, false),
                                             "nameColony.yes", "nameColony.no",
                                             true);
        if (name == null) return; // User cancelled, 0-length invalid.
        if (player.getSettlement(name) != null) {
            // Colony name must be unique.
            canvas.showInformationMessage("nameColony.notUnique",
                                          "%name%", name);
            return;
        }

        if (askBuildColony(name, unit) && tile.getSettlement() != null) {
            player.invalidateCanSeeTiles();
            freeColClient.playSound(SoundEffect.BUILDING_COMPLETE);
            gui.setActiveUnit(null);
            gui.setSelectedTile(tile.getPosition());

            // Check units present for treasure cash-in as they are now
            // suddenly in-colony.
            ArrayList<Unit> units = new ArrayList<Unit>(tile.getUnitList());
            for (Unit unitInTile : units) {
                checkCashInTreasureTrain(unitInTile);
            }
        }
    }

    /**
     * A colony is proposed to be built.  Show warnings if this has
     * disadvantages.
     *
     * @param tile The <code>Tile</code> on which the colony is to be built.
     * @param unit The <code>Unit</code> which is to build the colony.
     */
    private boolean showColonyWarnings(Tile tile, Unit unit) {
        boolean landLocked = true;
        boolean ownedByEuropeans = false;
        boolean ownedBySelf = false;
        boolean ownedByIndians = false;

        java.util.Map<GoodsType, Integer> goodsMap = new HashMap<GoodsType, Integer>();
        for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
            if (goodsType.isFoodType()) {
                int potential = 0;
                if (tile.getType().isPrimaryGoodsType(goodsType)) {
                    potential = tile.potential(goodsType, null);
                }
                goodsMap.put(goodsType, new Integer(potential));
            } else if (goodsType.isBuildingMaterial()) {
                while (goodsType.isRefined()) {
                    goodsType = goodsType.getRawMaterial();
                }
                int potential = 0;
                if (tile.getType().isSecondaryGoodsType(goodsType)) {
                    potential = tile.potential(goodsType, null);
                }
                goodsMap.put(goodsType, new Integer(potential));
            }
        }

        Map map = tile.getGame().getMap();
        Iterator<Position> tileIterator = map.getAdjacentIterator(tile.getPosition());
        while (tileIterator.hasNext()) {
            Tile newTile = map.getTile(tileIterator.next());
            if (newTile.isLand()) {
                for (Entry<GoodsType, Integer> entry : goodsMap.entrySet()) {
                    entry.setValue(entry.getValue().intValue() +
                                   newTile.potential(entry.getKey(), null));
                }
                Player tileOwner = newTile.getOwner();
                if (tileOwner == unit.getOwner()) {
                    if (newTile.getOwningSettlement() != null) {
                        // we are using newTile
                        ownedBySelf = true;
                    } else {
                        Iterator<Position> ownTileIt = map.getAdjacentIterator(newTile.getPosition());
                        while (ownTileIt.hasNext()) {
                            Colony colony = map.getTile(ownTileIt.next()).getColony();
                            if (colony != null && colony.getOwner() == unit.getOwner()) {
                                // newTile can be used from an own colony
                                ownedBySelf = true;
                                break;
                            }
                        }
                    }
                } else if (tileOwner != null && tileOwner.isEuropean()) {
                    ownedByEuropeans = true;
                } else if (tileOwner != null) {
                    ownedByIndians = true;
                }
            } else {
                landLocked = false;
            }
        }

        int food = 0;
        for (Entry<GoodsType, Integer> entry : goodsMap.entrySet()) {
            if (entry.getKey().isFoodType()) {
                food += entry.getValue().intValue();
            }
        }

        ArrayList<ModelMessage> messages = new ArrayList<ModelMessage>();
        if (landLocked) {
            messages.add(new ModelMessage(ModelMessage.MessageType.MISSING_GOODS,
                                          "buildColony.landLocked", unit, 
                                          FreeCol.getSpecification().getGoodsType("model.goods.fish")));
        }
        if (food < 8) {
            messages.add(new ModelMessage(ModelMessage.MessageType.MISSING_GOODS,
                                          "buildColony.noFood", unit,
                                          FreeCol.getSpecification().getGoodsType("model.goods.food")));
        }
        for (Entry<GoodsType, Integer> entry : goodsMap.entrySet()) {
            if (!entry.getKey().isFoodType() && entry.getValue().intValue() < 4) {
                messages.add(new ModelMessage(ModelMessage.MessageType.MISSING_GOODS,
                                              "buildColony.noBuildingMaterials",
                                              unit, entry.getKey())
                             .add("%goods%", entry.getKey().getNameKey()));
            }
        }

        if (ownedBySelf) {
            messages.add(new ModelMessage(ModelMessage.MessageType.WARNING,
                                          "buildColony.ownLand", unit));
        }
        if (ownedByEuropeans) {
            messages.add(new ModelMessage(ModelMessage.MessageType.WARNING,
                                          "buildColony.EuropeanLand", unit));
        }
        if (ownedByIndians) {
            messages.add(new ModelMessage(ModelMessage.MessageType.WARNING,
                                          "buildColony.IndianLand", unit));
        }

        if (messages.isEmpty()) return true;
        ModelMessage[] modelMessages = messages.toArray(new ModelMessage[messages.size()]);
        return freeColClient.getCanvas().showConfirmDialog(unit.getTile(),
                                                           modelMessages,
                                                           "buildColony.yes",
                                                           "buildColony.no");
    }

    /**
     * Server query-response for building a colony.
     *
     * @param name The name for the colony.
     * @param unit The <code>Unit</code> that will build.
     * @return True if the server interaction succeeded.
     */
    private boolean askBuildColony(String name, Unit unit) {
        Client client = freeColClient.getClient();
        BuildColonyMessage message = new BuildColonyMessage(name, unit);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Server query-response for joining a colony.
     *
     * @param unit The <code>Unit</code> that will join.
     * @param colony The <code>Colony</code> to join.
     * @return True if the server interaction succeeded.
     */
    private boolean askJoinColony(Unit unit, Colony colony) {
        Client client = freeColClient.getClient();
        JoinColonyMessage message = new JoinColonyMessage(colony, unit);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Abandon a colony with no units.
     *
     * @param colony The <code>Colony</code> to be abandoned.
     */
    public void abandonColony(Colony colony) {
        Player player = freeColClient.getMyPlayer();
        Canvas canvas = freeColClient.getCanvas();
        if (freeColClient.getGame().getCurrentPlayer() != player) {
            canvas.showInformationMessage("notYourTurn");
            return;
        }

        // Sanity check
        if (colony == null || colony.getOwner() != player
            || colony.getUnitCount() > 0) {
            throw new IllegalStateException("Abandon bogus colony");
        }

        // Proceed to abandon
        Tile tile = colony.getTile();
        if (askAbandonColony(colony) && tile.getSettlement() == null) {
            player.invalidateCanSeeTiles();
            GUI gui = freeColClient.getGUI();
            gui.setActiveUnit(null);
            gui.setSelectedTile(tile.getPosition());
        }
    }

    /**
     * Server query-response to abandon a colony.
     *
     * @param colony The <code>Colony</code> to abandon.
     * @return True if the server interaction succeeded.
     */
    private boolean askAbandonColony(Colony colony) {
        AbandonColonyMessage message = new AbandonColonyMessage(colony);
        Client client = freeColClient.getClient();
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Set the destination of the given unit.
     *
     * @param unit The <code>Unit</code> to direct.
     * @param destination The destination <code>Location</code>.
     * @see Unit#setDestination(Location)
     */
    public void setDestination(Unit unit, Location destination) {
        askSetDestination(unit, destination);
    }

    /**
     * Server query-response to set the destination of the given unit.
     *
     * @param unit The <code>Unit</code> to direct.
     * @param destination The destination <code>Location</code>.
     * @return True if the server interaction succeeded.
     * @see Unit#setDestination(Location)
     */
    private boolean askSetDestination(Unit unit, Location destination) {
        SetDestinationMessage message
            = new SetDestinationMessage(unit, destination);
        Client client = freeColClient.getClient();
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "update");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Moves the specified unit in a specified direction. This may
     * result in many different types of action.
     *
     * @param unit The <code>Unit</code> to be moved.
     * @param direction The direction in which to move the unit.
     * TODO: Unify trade and negotiation.
     */
    public void move(Unit unit, Direction direction) {
        Canvas canvas = freeColClient.getCanvas();
        if (freeColClient.getGame().getCurrentPlayer()
            != freeColClient.getMyPlayer()) {
            canvas.showInformationMessage("notYourTurn");
            return;
        }
        StringTemplate template;

        // Consider all the move types
        Tile tile = unit.getTile();
        MoveType move = unit.getMoveType(direction);
        switch (move) {
        case MOVE:
            moveMove(unit, direction);
            break;
        case MOVE_HIGH_SEAS:
            moveHighSeas(unit, direction);
            break;
        case EXPLORE_LOST_CITY_RUMOUR:
            moveExplore(unit, direction);
            break;
        case ATTACK:
            moveAttack(unit, direction);
            break;
        case EMBARK:
            moveEmbark(unit, direction);
            break;
        case ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST:
            moveLearnSkill(unit, direction);
            break;
        case ENTER_INDIAN_SETTLEMENT_WITH_SCOUT:
            moveScoutIndianSettlement(unit, direction);
            break;
        case ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY:
            moveUseMissionary(unit, direction);
            break;
        case ENTER_FOREIGN_COLONY_WITH_SCOUT:
            moveScoutColony(unit, direction);
            break;
        case ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS:
            moveTrade(unit, direction);
            break;

        case MOVE_NO_ACCESS_BEACHED:
            freeColClient.playSound(SoundEffect.ILLEGAL_MOVE);
            canvas.showInformationMessage("move.noAccessBeached", unit,
                                          "%nation%", Messages.message(getNationAt(tile, direction)));
            break;
        case MOVE_NO_ACCESS_CONTACT:
            freeColClient.playSound(SoundEffect.ILLEGAL_MOVE);
            canvas.showInformationMessage("move.noAccessContact", unit,
                                          "%nation%", Messages.message(getNationAt(tile, direction)));
            break;
        case MOVE_NO_ACCESS_LAND:
            if (!moveDisembark(unit, direction)) {
                freeColClient.playSound(SoundEffect.ILLEGAL_MOVE);
            }
            break;
        case MOVE_NO_ACCESS_SETTLEMENT:
            freeColClient.playSound(SoundEffect.ILLEGAL_MOVE);
            template = StringTemplate.template("move.noAccessSettlement")
                .addStringTemplate("%unit%", Messages.getLabel(unit))
                .addStringTemplate("%nation%", getNationAt(tile, direction));
            canvas.showInformationMessage(template, unit);
            break;
        case MOVE_NO_ACCESS_SKILL:
            freeColClient.playSound(SoundEffect.ILLEGAL_MOVE);
            template = StringTemplate.template("move.noAccessSkill")
                .addStringTemplate("%unit%", Messages.getLabel(unit));
            canvas.showInformationMessage(template, unit);
            break;
        case MOVE_NO_ACCESS_TRADE:
            freeColClient.playSound(SoundEffect.ILLEGAL_MOVE);
            canvas.showInformationMessage(StringTemplate.template("move.noAccessTrade")
                                          .addStringTemplate("%nation%", getNationAt(tile, direction)),
                                          unit);
            break;
        case MOVE_NO_ACCESS_WAR:
            freeColClient.playSound(SoundEffect.ILLEGAL_MOVE);
            canvas.showInformationMessage(StringTemplate.template("move.noAccessWar")
                                          .addStringTemplate("%nation%", getNationAt(tile, direction)),
                                          unit);
            break;
        case MOVE_NO_ACCESS_WATER:
            freeColClient.playSound(SoundEffect.ILLEGAL_MOVE);
            template = StringTemplate.template("move.noAccessWater")
                .addStringTemplate("%unit%", Messages.getLabel(unit));
            canvas.showInformationMessage(template, unit);
            break;
        default:
            freeColClient.playSound(SoundEffect.ILLEGAL_MOVE);
            break;
        }

        // TODO: check if this is necessary for all actions?
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    freeColClient.getActionManager().update();
                    freeColClient.updateMenuBar();
                }
            });
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
        Map map = freeColClient.getGame().getMap();
        return map.getNeighbourOrNull(direction, tile).getSettlement();
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
        Map map = freeColClient.getGame().getMap();
        Tile newTile = map.getNeighbourOrNull(direction, tile);
        Player player = null;
        if (newTile.getSettlement() != null) {
            player = newTile.getSettlement().getOwner();
        } else if (newTile.getFirstUnit() != null) {
            player = newTile.getFirstUnit().getOwner();
        } else { // should not happen
            player = freeColClient.getGame().getUnknownEnemy();
        }
        return player.getNationName();
    }

    /**
     * Actually move a unit in a specified direction, following a move
     * of MoveType.MOVE.
     *
     * @param unit The <code>Unit</code> to be moved.
     * @param direction The direction in which to move the Unit.
     */
    private void moveMove(Unit unit, Direction direction) {
        // If we are in a colony, or Europe, load sentries.
        if (unit.getSpaceLeft() > 0
            && (unit.getColony() != null || unit.isInEurope())) {
            for (Unit sentry : new ArrayList<Unit>(unit.getLocation().getUnitList())) {
                if (sentry.getState() == UnitState.SENTRY) {
                    if (sentry.getSpaceTaken() <= unit.getSpaceLeft()) {
                        boardShip(sentry, unit);
                        logger.finest("Unit " + unit.toString()
                                      + " loaded sentry " + sentry.toString());
                    } else {
                        logger.finest("Unit " + sentry.toString()
                                      + " is too big to board " + unit.toString());
                    }
                }
            }
        }

        // Ask the server
        Element reply = askMove(unit, direction);
        if (reply == null) return;

        // Handle special cases
        Game game = freeColClient.getGame();
        final Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();
        if (reply.hasAttribute("slowedBy")) { // ship slowed
            Unit slowedBy = (Unit) game.getFreeColGameObject(reply.getAttribute("slowedBy"));
            StringTemplate enemy = slowedBy.getOwner().getNationName();
            canvas.showInformationMessage(StringTemplate.template("model.unit.slowed")
                                          .addStringTemplate("%unit%", Messages.getLabel(unit))
                                          .addStringTemplate("%enemyUnit%", Messages.getLabel(slowedBy))
                                          .addStringTemplate("%enemyNation%", enemy),
                                          slowedBy);
        }

        ModelMessage m = null;
        if (reply.hasAttribute("nameNewLand")) {
            String defaultName = reply.getAttribute("nameNewLand");
            String newLandName = canvas.showInputDialog(unit.getTile(),
                                                        "newLand.text",
                                                        defaultName,
                                                        "newLand.yes", null,
                                                        true);
            // Default out on null, 0-length invalid.
            if (newLandName == null) newLandName = defaultName;
            if (askNewLandName(newLandName)
                && newLandName.equals(player.getNewLandName())) {
                canvas.showEventPanel(unit.getTile(), EventType.FIRST_LANDING);
                String key = FreeColActionUI.getHumanKeyStrokeText(freeColClient.getActionManager()
                                                                   .getFreeColAction("buildColonyAction").getAccelerator());
                m = new ModelMessage(ModelMessage.MessageType.TUTORIAL,
                                     "tutorial.buildColony", player)
                    .addName("%build_colony_key%", key)
                    .add("%build_colony_menu_item%", "unit.state.7")
                    .add("%orders_menu_item%", "menuBar.orders");
                player.addModelMessage(m);
            }
        }

        if (reply.hasAttribute("discoverPacific")) {
            canvas.showEventPanel(unit.getTile(), EventType.DISCOVER_PACIFIC);
        }
        if (reply.hasAttribute("discoverRegion")
            && reply.hasAttribute("regionType")) {
            String newRegionType = reply.getAttribute("regionType");
            String defaultName = reply.getAttribute("discoverRegion");
            String newRegionName = canvas.showInputDialog(unit.getTile(),
                                                          "nameRegion.text",
                                                          defaultName,
                                                          "ok", "cancel",
                                                          true,
                                                          "%name%", newRegionType);
            // Default out on null, 0-length invalid.
            if (newRegionName == null) newRegionName = defaultName;
            askNewRegionName(newRegionName, unit);
        }

        if (reply.hasAttribute("fountainOfYouth")) {
            // Without Brewster, the emigrants have already been selected
            // and were updated to the European docks by the server.
            final int emigrants = Integer.parseInt(reply.getAttribute("fountainOfYouth"));
            freeColClient.playMusicOnce("fountain");
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (int i = 0; i < emigrants; i++) {
                            int index = canvas.showEmigrationPanel(true);
                            askEmigrate(index + 1);
                        }
                    }
                });
        }

        // Perform a short pause on an active unit's last move if
        // the option is enabled.
        ClientOptions options = freeColClient.getClientOptions();
        if (unit.getMovesLeft() <= 0
            && options.getBoolean(ClientOptions.UNIT_LAST_MOVE_DELAY)) {
            canvas.paintImmediately(canvas.getBounds());
            try {
                // UNIT_LAST_MOVE_DELAY is an instance variable
                // located at the top of this class.
                Thread.sleep(UNIT_LAST_MOVE_DELAY);
            } catch (InterruptedException e) {} // Ignore
        }

        // Update the active unit and GUI.
        Tile tile = unit.getTile();
        if (unit.isDisposed() || checkCashInTreasureTrain(unit)) {
            nextActiveUnit(tile);
        } else {
            if (tile.getSettlement() instanceof Colony
                && unit.isCarrier()
                && unit.getTradeRoute() == null
                && (unit.getDestination() == null
                    || unit.getDestination().getTile() == tile.getTile())) {
                canvas.showColonyPanel((Colony) tile.getSettlement());
            }
            if (unit.getMovesLeft() == 0) nextActiveUnit();
        }
    }

    /**
     * Server query-response for moving a unit.
     *
     * @param unit The <code>Unit</code> to move.
     * @param direction The direction to move in.
     * @return An <code>Element</code> containing the response, which
     *         may have special attributes set indicating further action,
     *         or null on failure.
     */
    private Element askMove(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        MoveMessage message = new MoveMessage(unit, direction);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return null;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return reply;
    }

    /**
     * Server query-response for naming a new land.
     *
     * @param name The new land name.
     * @return True if the server interaction succeeded.
     */
    private boolean askNewLandName(String name) {
        Client client = freeColClient.getClient();
        NewLandNameMessage message = new NewLandNameMessage(name);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Server query-response for naming a new region.
     *
     * @param name The new region name.
     * @param unit The <code>Unit</code> that discovered the region.
     * @return True if the server interaction succeeded.
     */
    private boolean askNewRegionName(String name, Unit unit) {
        Client client = freeColClient.getClient();
        NewRegionNameMessage message
            = new NewRegionNameMessage(name, unit);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Moves a unit onto the "high seas" in a specified direction following
     * a move of MoveType.MOVE_HIGH_SEAS.
     * This may result in a move to Europe, no move, or an ordinary move.
     *
     * @param unit The <code>Unit</code> to be moved.
     * @param direction The direction in which to move.
     * TODO: Perhaps can throw away the AlreadyOnHighSeas flag?
     */
    private void moveHighSeas(Unit unit, Direction direction) {
        // Confirm moving to Europe if told to move to a null tile
        // (TODO: can this still happen?), or if crossing the boundary
        // between coastal and high sea.  Otherwise just move.
        Map map = freeColClient.getGame().getMap();
        Tile oldTile = unit.getTile();
        Tile newTile = map.getNeighbourOrNull(direction, oldTile);
        Canvas canvas = freeColClient.getCanvas();
        if ((newTile == null
             || (!oldTile.canMoveToEurope() && newTile.canMoveToEurope()))
            && canvas.showConfirmDialog(oldTile, "highseas.text",
                                        "highseas.yes", "highseas.no")) {
            moveToEurope(unit);
            nextActiveUnit();
        } else {
            moveMove(unit, direction);
        }
    }

    /**
     * Confirm exploration of a lost city rumour, following a move of
     * MoveType.EXPLORE_LOST_CITY_RUMOUR.
     *
     * @param unit The <code>Unit</code> that is exploring.
     * @param direction The direction of a rumour.
     */
    private void moveExplore(Unit unit, Direction direction) {
        // Confirm exploration.
        Canvas canvas = freeColClient.getCanvas();
        if (canvas.showConfirmDialog(unit.getTile(),
                                     "exploreLostCityRumour.text",
                                     "exploreLostCityRumour.yes",
                                     "exploreLostCityRumour.no")) {
            moveMove(unit, direction);
        }
    }

    /**
     * Confirm attack or demand a tribute from a native settlement, following
     * a move of MoveType.ATTACK.
     *
     * @param unit The <code>Unit</code> to perform the attack.
     * @param direction The direction in which to attack.
     */
    private void moveAttack(Unit unit, Direction direction) {
        Canvas canvas = freeColClient.getCanvas();
        Game game = freeColClient.getGame();

        // Extra option with native settlement
        Tile tile = unit.getTile();
        Tile target = game.getMap().getNeighbourOrNull(direction, tile);
        Settlement settlement = target.getSettlement();
        if (settlement != null
            && settlement instanceof IndianSettlement
            && unit.isArmed()) {
            IndianSettlement natives = (IndianSettlement) settlement;
            switch (canvas.showArmedUnitIndianSettlementDialog(natives)) {
            case CANCEL:
                return;
            case INDIAN_SETTLEMENT_ATTACK:
                break; // Go on to usual attack confirmation.
            case INDIAN_SETTLEMENT_TRIBUTE:
                moveTribute(unit, direction);
                return;
            default:
                logger.warning("showArmedUnitIndianSettlementDialog failure.");
                return;
            }
        }

        // Normal attack confirmation.
        if (confirmHostileAction(unit, target)
            && confirmPreCombat(unit, target)) {
            attack(unit, direction);
        }
    }

    /**
     * Demand a tribute.
     *
     * @param unit The <code>Unit</code> to perform the attack.
     * @param direction The direction in which to attack.
     */
    private void moveTribute(Unit unit, Direction direction) {
        if (askDemandTribute(unit, direction)) {
            // Assume tribute paid
            freeColClient.getCanvas().updateGoldLabel();
            nextActiveUnit();
        }
    }

    /**
     * Server query-response for demanding a tribute from a native
     * settlement.
     *
     * @param unit The <code>Unit</code> that demands.
     * @param direction The direction to demand in.
     * @return True if the server interaction succeeded.
     */
    private boolean askDemandTribute(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        DemandTributeMessage message
            = new DemandTributeMessage(unit, direction);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Check if an attack results in a transition from peace or cease fire to
     * war and, if so, warn the player.
     *
     * @param attacker The potential attacking <code>Unit</code>.
     * @param target The target <code>Tile</code>.
     * @return True to attack, false to abort.
     */
    private boolean confirmHostileAction(Unit attacker, Tile target) {
        if (attacker.hasAbility("model.ability.piracy")) {
            // Privateers can attack and remain at peace
            return true;
        }

        Player enemy;
        if (target.getSettlement() != null) {
            enemy = target.getSettlement().getOwner();
        } else {
            Unit defender = target.getDefendingUnit(attacker);
            if (defender == null) {
                logger.warning("Attacking, but no defender - will try!");
                return true;
            }
            if (defender.hasAbility("model.ability.piracy")) {
                // Privateers can be attacked and remain at peace
                return true;
            }
            enemy = defender.getOwner();
        }

        // Confirm attack given current stance
        Canvas canvas = freeColClient.getCanvas();
        String enemyNation = Messages.message(enemy.getNationName());
        switch (attacker.getOwner().getStance(enemy)) {
        case UNCONTACTED: case PEACE:
            return canvas.showConfirmDialog(attacker.getTile(),
                                            "model.diplomacy.attack.peace",
                                            "model.diplomacy.attack.confirm",
                                            "cancel",
                                            "%nation%", enemyNation);
        case WAR:
            logger.finest("Player at war, no confirmation needed");
            break;
        case CEASE_FIRE:
            return canvas.showConfirmDialog(attacker.getTile(),
                                            "model.diplomacy.attack.ceaseFire",
                                            "model.diplomacy.attack.confirm",
                                            "cancel",
                                            "%nation%", enemyNation);
        case ALLIANCE:
            return canvas.showConfirmDialog(attacker.getTile(),
                                            "model.diplomacy.attack.alliance",
                                            "model.diplomacy.attack.confirm",
                                            "cancel",
                                            "%nation%", enemyNation);
        }
        return true;
    }

    /**
     * If the client options include a pre-combat dialog, allow the
     * user to view the odds and possibly cancel the attack.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param tile The target <code>Tile</code>.
     * @return True to attack, false to abort.
     */
    private boolean confirmPreCombat(Unit attacker, Tile tile) {
        if (freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_PRECOMBAT)) {
            Settlement settlement = tile.getSettlement();
            // Don't tell the player how a settlement is defended!
            Unit defender = (settlement != null) ? null
                : tile.getDefendingUnit(attacker);
            Canvas canvas = freeColClient.getCanvas();
            return canvas.showPreCombatDialog(attacker, defender, settlement);
        }
        return true;
    }

    /**
     * Performs an attack in a specified direction.
     * Note that the server handles the attack calculations here.
     *
     * @param unit The <code>Unit</code> to perform the attack.
     * @param direction The direction in which to attack.
     */
    private void attack(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        Game game = freeColClient.getGame();
        Tile target = game.getMap().getNeighbourOrNull(direction, unit.getTile());

        Element attackElement = Message.createNewRootElement("attack");
        attackElement.setAttribute("unit", unit.getId());
        attackElement.setAttribute("direction", direction.toString());

        // Get the result of the attack from the server:
        Element attackResultElement = client.ask(attackElement);
        if (attackResultElement != null &&
            attackResultElement.getTagName().equals("attackResult")) {
            // process the combat result
            CombatResultType result = Enum.valueOf(CombatResultType.class, attackResultElement.getAttribute("result"));
            int damage = Integer.parseInt(attackResultElement.getAttribute("damage"));
            int plunderGold = Integer.parseInt(attackResultElement.getAttribute("plunderGold"));
            Location repairLocation = (Location) game.getFreeColGameObjectSafely(attackResultElement.getAttribute("repairIn"));

            // If a successful attack against a colony, we need to update the
            // tile:
            Element utElement = getChildElement(attackResultElement, Tile.getXMLElementTagName());
            if (utElement != null) {
                Tile updateTile = (Tile) game.getFreeColGameObject(utElement.getAttribute("ID"));
                updateTile.readFromXMLElement(utElement);
            }

            // If there are captured goods, add to unit
            NodeList capturedGoods = attackResultElement.getElementsByTagName("capturedGoods");
            for (int i = 0; i < capturedGoods.getLength(); ++i) {
                Element goods = (Element) capturedGoods.item(i);
                GoodsType type = FreeCol.getSpecification().getGoodsType(goods.getAttribute("type"));
                int amount = Integer.parseInt(goods.getAttribute("amount"));
                unit.getGoodsContainer().addGoods(type, amount);
            }

            // Get the defender:
            Element unitElement = getChildElement(attackResultElement, Unit.getXMLElementTagName());
            Unit defender;
            if (unitElement != null) {
                defender = (Unit) game.getFreeColGameObject(unitElement.getAttribute("ID"));
                if (defender == null) {
                    defender = new Unit(game, unitElement);
                } else {
                    defender.readFromXMLElement(unitElement);
                }
                defender.setLocation(target);
            } else {
                // TODO: Erik - ensure this cannot happen!
                logger.log(Level.SEVERE, "Server reallyAttack did not return a defender!");
                defender = target.getDefendingUnit(unit);
                if (defender == null) {
                    throw new IllegalStateException("No defender available!");
                }
            }

            if (result == CombatResultType.DONE_SETTLEMENT) {
                freeColClient.playSound(SoundEffect.CAPTURED_BY_ARTILLERY);
            } else if (defender.isNaval() && result == CombatResultType.GREAT_WIN 
                       || unit.isNaval() && result == CombatResultType.GREAT_LOSS) {
                freeColClient.playSound(SoundEffect.SUNK);
            } else if (unit.isNaval()) {
                freeColClient.playSound(SoundEffect.ATTACK_NAVAL);
            } else if (unit.hasAbility("model.ability.bombard")) {
                freeColClient.playSound(SoundEffect.ATTACK_ARTILLERY);
            } else if (unit.isMounted()) {
                freeColClient.playSound(SoundEffect.ATTACK_DRAGOON);
            }
            
            Animations.unitAttack(freeColClient.getCanvas(), unit, defender, result);

            try {
                game.getCombatModel().attack(unit, defender, new CombatResult(result, damage), plunderGold, repairLocation);
            } catch (Exception e) {
                // Ignore the exception (the update further down will fix any
                // problems).
                LogRecord lr = new LogRecord(Level.WARNING, "Exception in reallyAttack");
                lr.setThrown(e);
                logger.log(lr);
            }

            // Get the convert
            Element convertElement = getChildElement(attackResultElement, "convert");
            Unit convert;
            if (convertElement != null) {
                unitElement = (Element) convertElement.getFirstChild();
                convert = (Unit) game.getFreeColGameObject(unitElement.getAttribute("ID"));
                if (convert == null) {
                    convert = new Unit(game, unitElement);
                } else {
                    convert.readFromXMLElement(unitElement);
                }
                convert.setLocation(convert.getLocation());
                
                StringTemplate nation = defender.getOwner().getNationName();
                ModelMessage message = new ModelMessage(ModelMessage.MessageType.UNIT_ADDED,
                                                        "model.unit.newConvertFromAttack", convert)
                    .addStringTemplate("%nation%", nation)
                    .addStringTemplate("%unit%", Messages.getLabel(convert));
                freeColClient.getMyPlayer().addModelMessage(message);
                nextModelMessage();
            }
            
            if (defender.canCarryTreasure() &&
                (result == CombatResultType.WIN ||
                 result == CombatResultType.GREAT_WIN)) {
                checkCashInTreasureTrain(defender);
            }
                
            if (!defender.isDisposed()
                && ((result == CombatResultType.DONE_SETTLEMENT && unitElement != null)
                    || defender.getLocation() == null || !defender.isVisibleTo(freeColClient.getMyPlayer()))) {
                defender.dispose();
            }
 
            Element updateElement = getChildElement(attackResultElement, "update");
            if (updateElement != null) {
                freeColClient.getInGameInputHandler().handle(client.getConnection(), updateElement);
            }
            
            // settlement was indian capital, indians surrender
            if(attackResultElement.getAttribute("indianCapitalBurned") != ""){
            	Player indianPlayer = defender.getOwner();
            	indianPlayer.surrenderTo(freeColClient.getMyPlayer());
            	//show message
            	ModelMessage message = new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                        "indianSettlement.capitalBurned", indianPlayer)
                    .addName("%name%", Messages.getDefaultSettlementName(indianPlayer, true))
                    .addStringTemplate("%nation%", indianPlayer.getNationName());
            	freeColClient.getMyPlayer().addModelMessage(message);
            	nextModelMessage();
            }

            if (unit.getMovesLeft() <= 0) {
                nextActiveUnit(unit.getTile());
            }

            freeColClient.getCanvas().refresh();
        } else {
            logger.log(Level.SEVERE, "Server returned null from attack!");
        }
    }

    /**
     * Embarks the specified unit onto a carrier in a specified direction
     * following a move of MoveType.EMBARK.
     *
     * @param unit The <code>Unit</code> that wishes to embark.
     * @param direction The direction in which to embark.
     */
    private void moveEmbark(Unit unit, Direction direction) {
        // Choose which carrier to embark upon.
        Canvas canvas = freeColClient.getCanvas();
        Tile sourceTile = unit.getTile();
        Map map = freeColClient.getGame().getMap();
        Tile destinationTile = map.getNeighbourOrNull(direction, sourceTile);
        Unit carrier = null;
        ArrayList<Unit> choices = new ArrayList<Unit>();
        for (Unit nextUnit : destinationTile.getUnitList()) {
            if (nextUnit.getSpaceLeft() >= unit.getType().getSpaceTaken()) {
                choices.add(nextUnit);
            }
        }
        if (choices.size() == 0) {
            throw new RuntimeException("Unit " + unit.getId()
                                       + " found no carrier to embark upon.");
        } else if (choices.size() == 1) {
            carrier = choices.get(0);
        } else {
            carrier = canvas.showSimpleChoiceDialog(unit.getTile(),
                                                    "embark.text",
                                                    "embark.cancel",
                                                    choices);
            if (carrier == null) return; // User cancelled
        }

        // Proceed to embark
        if (askEmbark(unit, carrier, direction)
            && unit.getLocation() == carrier) {
            if (carrier.getMovesLeft() > 0) {
                freeColClient.getGUI().setActiveUnit(carrier);
            } else {
                nextActiveUnit();
            }
        }
    }

    /**
     * Check the carrier for passengers to disembark, possibly
     * snatching a useful result from the jaws of a
     * MOVE_NO_ACCESS_LAND failure.
     *
     * @param unit The carrier containing the unit to disembark.
     * @param direction The direction in which to disembark the unit.
     * @return True if the disembark "succeeds" (which deliberately includes
     *         declined disembarks).
     */
    private boolean moveDisembark(Unit unit, Direction direction) {
        Map map = freeColClient.getGame().getMap();
        Tile tile = map.getNeighbourOrNull(direction, unit.getTile());
        if (tile.getFirstUnit() != null
            && tile.getFirstUnit().getOwner() != unit.getOwner()) {
            return false; // Can not disembark onto other nation units.
        }

        // Disembark selected units able to move.
        ArrayList<Unit> disembarkable = new ArrayList<Unit>();
        unit.setStateToAllChildren(UnitState.ACTIVE);
        for (Unit u : unit.getUnitList()) {
            if (u.getMoveType(tile).isProgress()) {
                disembarkable.add(u);
            }
        }
        if (disembarkable.size() == 0) {
            // Did not find any unit that could disembark, fail.
            return false;
        }

        // Pick units the user wants to disembark.
        Canvas canvas = freeColClient.getCanvas();
        while (disembarkable.size() > 0) {
            List<ChoiceItem<Unit>> choices = new ArrayList<ChoiceItem<Unit>>();
            for (Unit dUnit : disembarkable) {
                choices.add(new ChoiceItem<Unit>(Messages.message(Messages.getLabel(dUnit)), dUnit));
            }
            Unit u = canvas.showChoiceDialog(unit.getTile(),
                                             Messages.message("disembark.text"),
                                             Messages.message("disembark.cancel"),
                                             choices);
            if (u == null) break; // Done
            // Call move() as while the destination tile is known to
            // be clear of settlements or other player units, it *may*
            // have a rumour.
            move(u, direction);
            disembarkable.remove(u);
        }
        return true;
    }

    /**
     * Move a free colonist to a native settlement to learn a skill following
     * a move of MoveType.ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST.
     * The colonist does not physically get into the village, it will
     * just stay where it is and gain the skill.
     *
     * @param unit The <code>Unit</code> to learn the skill.
     * @param direction The direction in which the Indian settlement lies.
     */
    private void moveLearnSkill(Unit unit, Direction direction) {
        // Refresh knowledge of settlement skill.  It may have been
        // learned by another player.
        if (!askSkill(unit, direction)) {
            return;
        }

        Canvas canvas = freeColClient.getCanvas();
        IndianSettlement settlement
            = (IndianSettlement) getSettlementAt(unit.getTile(), direction);
        UnitType skill = settlement.getLearnableSkill();
        if (skill == null) {
            canvas.showInformationMessage("indianSettlement.noMoreSkill",
                                          settlement);
        } else if (!unit.getType().canBeUpgraded(skill, ChangeType.NATIVES)) {
            canvas.showInformationMessage(StringTemplate.template("indianSettlement.cantLearnSkill")
                                          .addStringTemplate("%unit%", Messages.getLabel(unit))
                                          .add("%skill%", skill.getNameKey()),
                                          settlement);
        } else if (canvas.showConfirmDialog(unit.getTile(),
                                            StringTemplate.template("learnSkill.text")
                                            .add("%skill%", skill.getNameKey()),
                                            "learnSkill.yes", "learnSkill.no")) {
            if (askLearnSkill(unit, direction)) {
                if (unit.isDisposed()) {
                    canvas.showInformationMessage("learnSkill.die",
                                                  settlement);
                    nextActiveUnit(unit.getTile());
                    return;
                }
                if (unit.getType() != skill) {
                    canvas.showInformationMessage("learnSkill.leave",
                                                  settlement);
                }
            }
        }
        nextActiveUnit();
    }

    /**
     * Server query-response for finding out the skill taught at a settlement.
     *
     * @param unit The <code>Unit</code> that is asking.
     * @param direction The direction to a settlement to ask.
     * @return True if the server interaction succeeded.
     */
    private boolean askSkill(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        AskSkillMessage message = new AskSkillMessage(unit, direction);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "update");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Server query-response for learning the skill taught at a settlement.
     *
     * @param unit The <code>Unit</code> that is asking.
     * @param direction The direction to a settlement to ask.
     * @return True if the server interaction succeeded.
     */
    private boolean askLearnSkill(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        LearnSkillMessage message = new LearnSkillMessage(unit, direction);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
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
     */
    private void moveScoutIndianSettlement(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        Map map = freeColClient.getGame().getMap();
        Tile unitTile = unit.getTile();
        Tile tile = map.getNeighbourOrNull(direction, unitTile);
        IndianSettlement settlement = (IndianSettlement) tile.getSettlement();

        // Offer the choices.
        switch (canvas.showScoutIndianSettlementDialog(settlement)) {
        case CANCEL:
            return;
        case INDIAN_SETTLEMENT_ATTACK:
            if (confirmPreCombat(unit, tile)) {
                attack(unit, direction);
            }
            return;
        case INDIAN_SETTLEMENT_SPEAK:
            Player player = unit.getOwner();
            int gold = player.getGold();
            String result = askScoutSpeak(unit, direction);
            if (result == null) {
                logger.warning("Null result from askScoutSpeak");
            } else if ("die".equals(result)) {
                canvas.showInformationMessage("scoutSettlement.speakDie",
                                              settlement);
                nextActiveUnit(unitTile);
                return;
            } else if ("expert".equals(result)) {
                canvas.showInformationMessage(StringTemplate.template("scoutSettlement.expertScout")
                                              .add("%unit%", unit.getType().getNameKey()),
                                              settlement);
            } else if ("tales".equals(result)) {
                canvas.showInformationMessage("scoutSettlement.speakTales",
                                              settlement);
            } else if ("beads".equals(result)) {
                canvas.updateGoldLabel();
                canvas.showInformationMessage("scoutSettlement.speakBeads",
                                              settlement,
                                              "%amount%", Integer.toString(player.getGold() - gold));
            } else if ("nothing".equals(result)) {
                canvas.showInformationMessage("scoutSettlement.speakNothing",
                                              settlement);
            } else {
                logger.warning("Invalid result from askScoutSpeak: " + result);
            }
            nextActiveUnit();
            break;
        case INDIAN_SETTLEMENT_TRIBUTE:
            moveTribute(unit, direction);
            break;
        default:
            throw new IllegalArgumentException("showScoutIndianSettlementDialog fail");
        }
    }

    /**
     * Server query-response for speaking with a native chief.
     *
     * @param unit The <code>Unit</code> that is speaking.
     * @param direction The direction to a settlement to ask.
     * @return A string containing the value of the "result" attribute on the
     *         reply from the server, or null on failure.
     */
    private String askScoutSpeak(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        ScoutIndianSettlementMessage message
            = new ScoutIndianSettlementMessage(unit, direction);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return null;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return reply.getAttribute("result");
    }

    /**
     * Move a missionary into a native settlement, following a move of
     * MoveType.ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY.
     *
     * @param unit The <code>Unit</code> that will enter the settlement.
     * @param direction The direction in which the Indian settlement lies.
     */
    private void moveUseMissionary(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        Map map = freeColClient.getGame().getMap();
        IndianSettlement settlement
            = (IndianSettlement) getSettlementAt(unit.getTile(), direction);
        Unit missionary = settlement.getMissionary();
        boolean canEstablish = missionary == null;
        boolean canDenounce = missionary != null
            && missionary.getOwner() != unit.getOwner();

        // Offer the choices.
        switch (canvas.showUseMissionaryDialog(unit, settlement,
                                               canEstablish, canDenounce)) {
        case CANCEL:
            return;
        case ESTABLISH_MISSION:
            if (askMissionary(unit, direction, false)) {
                if (settlement.getMissionary() == unit) {
                    freeColClient.playSound(SoundEffect.MISSION_ESTABLISHED);
                }
                nextActiveUnit();
            }
            break;
        case DENOUNCE_HERESY:
            if (askMissionary(unit, direction, true)) {
                if (settlement.getMissionary() == unit) {
                    freeColClient.playSound(SoundEffect.MISSION_ESTABLISHED);
                }
                nextModelMessage();
                nextActiveUnit();
            }
            break;
        case INCITE_INDIANS:
            List<Player> enemies
                = new ArrayList<Player>(freeColClient.getGame().getEuropeanPlayers());
            enemies.remove(freeColClient.getMyPlayer());
            Player enemy = canvas.showSimpleChoiceDialog(unit.getTile(),
                                                         "missionarySettlement.inciteQuestion",
                                                         "missionarySettlement.cancel",
                                                         enemies);
            if (enemy == null) return;
            int gold = askIncite(unit, direction, enemy, -1);
            if (gold < 0) return;
            if (canvas.showConfirmDialog(unit.getTile(),
                                         "missionarySettlement.inciteConfirm",
                                         "yes", "no",
                                         "%player%", enemy.getName(),
                                         "%amount%", String.valueOf(gold))) {
                int goldOut = askIncite(unit, direction, enemy, gold);
                if (goldOut < 0) {
                    ; // protocol fail
                } else if (goldOut == 0) {
                    canvas.showInformationMessage("notEnoughGold", settlement);
                } else {
                    // model messages from setStance are asynchronous
                    // at present
                    canvas.updateGoldLabel();
                }
            }
            nextActiveUnit();
            break;
        default:
            logger.warning("showUseMissionaryDialog fail");
            break;
        }
    }

    /**
     * Server query-response for establishing/denouncing a mission.
     *
     * @param unit The missionary <code>Unit</code>.
     * @param direction The direction to a settlement to establish with.
     * @param denounce True if this is a denouncement.
     * @return True if the server interaction succeeded.
     */
    private boolean askMissionary(Unit unit, Direction direction,
                                  boolean denounce) {
        Client client = freeColClient.getClient();
        MissionaryMessage message
            = new MissionaryMessage(unit, direction, denounce);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Server query-response for inciting the natives.
     *
     * @param unit The missionary <code>Unit</code>.
     * @param direction The direction to a settlement to speak to.
     * @param enemy An enemy <code>Player</code>.
     * @param gold The amount of bribe, negative to enquire.
     * @return An amount of gold needed or paid, or negative if the
     *         server interaction failed.
     */
    private int askIncite(Unit unit, Direction direction, Player enemy,
                          int gold) {
        Client client = freeColClient.getClient();
        InciteMessage message
            = new InciteMessage(unit, direction, enemy, gold);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "update");
        if (reply == null || reply.getAttribute("gold") == null) return -1;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return Integer.parseInt(reply.getAttribute("gold"));
    }

    /**
     * Move to a foreign colony and either attack, negotiate with the
     * foreign power or spy on them.  Follows a move of
     * MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT.
     *
     * @param unit The unit that will spy, negotiate or attack.
     * @param direction The direction in which the foreign colony lies.
     */
    private void moveScoutColony(Unit unit, Direction direction) {
        Canvas canvas = freeColClient.getCanvas();
        Colony colony = (Colony) getSettlementAt(unit.getTile(), direction);
        boolean canNeg = colony.getOwner() != unit.getOwner().getREFPlayer();
        switch (canvas.showScoutForeignColonyDialog(colony, unit, canNeg)) {
        case CANCEL:
            break;
        case FOREIGN_COLONY_ATTACK:
            moveAttack(unit, direction);
            break;
        case FOREIGN_COLONY_NEGOTIATE:
            moveTradeColony(unit, direction);
            break;
        case FOREIGN_COLONY_SPY:
            moveSpy(unit, direction);
            break;
        default:
            throw new IllegalArgumentException("showScoutForeignColonyDialog fail");
        }
    }

    /**
     * Initiates a negotiation with a foreign power. The player
     * creates a DiplomaticTrade with the NegotiationDialog. The
     * DiplomaticTrade is sent to the other player. If the other
     * player accepts the offer, the trade is concluded.  If not, this
     * method returns, since the next offer must come from the other
     * player.
     *
     * @param unit The <code>Unit</code> negotiating.
     * @param direction The direction of a settlement to negotiate with.
     */
    private void moveTradeColony(Unit unit, Direction direction) {
        Settlement settlement = getSettlementAt(unit.getTile(), direction);
        if (settlement == null) return;

        // Can not negotiate with the REF.
        if (settlement.getOwner() == unit.getOwner().getREFPlayer()) {
            throw new IllegalStateException("Unit tried to negotiate with REF");
        }

        Player player = freeColClient.getMyPlayer();
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        DiplomaticTrade oldAgreement = null;
        DiplomaticTrade newAgreement = null;
        DiplomacyMessage message;
        Element reply;
        for (;;) {
            newAgreement = canvas.showNegotiationDialog(unit, settlement,
                                                        oldAgreement);
            if (newAgreement == null) {
                if (oldAgreement != null) {
                    // Inform of rejection of the old agreement
                    message = new DiplomacyMessage(unit, direction,
                                                   oldAgreement);
                    message.setReject();
                    client.sendAndWait(message.toXMLElement());
                }
                break;
            }

            // Send this acceptance or proposal to the other player
            message = askDiplomacy(unit, direction, newAgreement);

            // What did they say?
            if (message == null || message.isReject()) {
                String nation = message.getOtherNationName(player);
                canvas.showInformationMessage("negotiationDialog.offerRejected",
                                              settlement,
                                              "%nation%", nation);
                break;
            } else if (message.isAccept()) {
                String nation = message.getOtherNationName(player);
                canvas.showInformationMessage("negotiationDialog.offerAccepted",
                                              settlement,
                                              "%nation%", nation);
                break;
            } else { // Loop with this proposal
                oldAgreement = message.getAgreement();
            }
        }
        nextActiveUnit();
    }

    /**
     * Handler server query-response for diplomatic messages.
     *
     * @param unit The <code>Unit</code> conducting the diplomacy.
     * @param direction The <code>Direction</code> in which the settlement is.
     * @param agreement The <code>DiplomaticTrade</code> agreement to propose.
     * @return The agreement returned from the other party, or null.
     */
    private DiplomacyMessage askDiplomacy(Unit unit, Direction direction,
                                          DiplomaticTrade agreement) {
        Client client = freeColClient.getClient();
        Game game = freeColClient.getGame();
        DiplomacyMessage message = new DiplomacyMessage(unit, direction,
                                                        agreement);
        if (agreement.isAccept()) message.setAccept();
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return null;

        // The reply should contain updates, and the diplomacy last.
        Node diplomacy = reply.getLastChild();
        if (diplomacy == null
            || diplomacy.getNodeType() != Node.ELEMENT_NODE
            || !diplomacy.getNodeName().equals("diplomacy")) {
            return null;
        }

        // We extract the diplomacy to return for interactive handling
        // rather than processing it in the input handler.
        diplomacy = reply.removeChild(diplomacy);
        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return new DiplomacyMessage(game, (Element) diplomacy);
    }

    /**
     * Spy on a foreign colony.
     *
     * @param unit The <code>Unit</code> that is spying.
     * @param direction The <code>Direction</code> of a colony to spy on.
     */
    private void moveSpy(Unit unit, Direction direction) {
        Colony colony = (Colony) getSettlementAt(unit.getTile(), direction);
        Element reply;
        if (colony != null && (reply = askSpy(unit, direction)) != null) {
            // Sleight of hand here.  The update contains two versions
            // of the colony tile.  The first child node is a detailed
            // view, which is read explicitly, displayed, removed, then
            // superseded as the update is processed normally.
            Element tileElement = (Element) reply.getFirstChild();
            colony.getTile().readFromXMLElement(tileElement);
            freeColClient.getCanvas().showColonyPanel(colony);
            reply.removeChild(tileElement);
            freeColClient.getInGameInputHandler().update(reply);
        }
        nextActiveUnit();
    }

    /**
     * Server query-response for spying on a colony.
     *
     * @param unit The <code>Unit</code> that is spying.
     * @param direction The <code>Direction</code> of a colony to spy on.
     * @return An element containing two views of the tile containing the colony.
     */
    private Element askSpy(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        SpySettlementMessage message = new SpySettlementMessage(unit, direction);
        return askExpecting(client, message.toXMLElement(), "update");
    }

    /**
     * Arrive at a settlement with a laden carrier following a move of
     * MoveType.ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS.
     *
     * @param unit The carrier.
     * @param direction The direction to the settlement.
     */
    private void moveTrade(Unit unit, Direction direction) {
        Settlement settlement = getSettlementAt(unit.getTile(), direction);
        if (settlement instanceof Colony) {
            moveTradeColony(unit, direction);
        } else if (settlement instanceof IndianSettlement) {
            moveTradeIndianSettlement(unit, direction);
        } else {
            logger.warning("Bogus settlement: " + settlement.getId());
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
     * @exception IllegalArgumentException if the unit is not a carrier, or if
     *                there is no <code>Settlement</code> in the given
     *                direction.
     * @see Settlement
     */
    private void moveTradeIndianSettlement(Unit unit, Direction direction) {
        Canvas canvas = freeColClient.getCanvas();
        Settlement settlement = getSettlementAt(unit.getTile(), direction);
        java.util.Map<String, Boolean> session;
        boolean done = false;

        while (!done) {
            session = askOpenTransactionSession(unit, settlement);
            if (session == null) break;
            // The session tracks buy/sell/gift events and disables
            // canFoo when one happens.  So only offer such options if
            // the session allows it and the carrier is in good shape.
            boolean buy = session.get("canBuy")  && (unit.getSpaceLeft() > 0);
            boolean sel = session.get("canSell") && (unit.getGoodsCount() > 0);
            boolean gif = session.get("canGift") && (unit.getGoodsCount() > 0);
            if (!buy && !sel && !gif) break;

            switch (canvas.showIndianSettlementTradeDialog(settlement,
                                                           buy, sel, gif)) {
            case CANCEL:
                done = true;
                break;
            case BUY:
                attemptBuyFromSettlement(unit, settlement);
                break;
            case SELL:
                attemptSellToSettlement(unit, settlement);
                break;
            case GIFT:
                attemptGiftToSettlement(unit, settlement);
                break;
            default:
                throw new IllegalArgumentException("showIndianSettlementTradeDialog fail");
            }
        }

        askCloseTransactionSession(unit, settlement);
        if (unit.getMovesLeft() > 0) { // May have been restored if no trade
            freeColClient.getGUI().setActiveUnit(unit);
        } else {
            nextActiveUnit();
        }
    }

    /**
     * Server query-response to get the transaction session for a trade.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return A transaction session or null on failure.
     */
    private java.util.Map<String,Boolean> askOpenTransactionSession(Unit unit, Settlement settlement) {
        Client client = freeColClient.getClient();
        GetTransactionMessage message = new GetTransactionMessage(unit, settlement);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "getTransactionAnswer");
        if (reply == null) return null;

        java.util.Map<String,Boolean> session = new HashMap<String,Boolean>();
        session.put("canBuy", new Boolean(reply.getAttribute("canBuy")));
        session.put("canSell", new Boolean(reply.getAttribute("canSell")));
        session.put("canGift", new Boolean(reply.getAttribute("canGift")));
        return session;
    }

    /**
     * Server query-response to close a transaction session for a trade.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return True if the server interaction succeeded.
     */
    private boolean askCloseTransactionSession(Unit unit, Settlement settlement) {
        Client client = freeColClient.getClient();
        CloseTransactionMessage message
            = new CloseTransactionMessage(unit, settlement);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "update");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * User interaction for buying from the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     */
    private void attemptBuyFromSettlement(Unit unit, Settlement settlement) {
        // Get list of goods for sale
        List<Goods> forSale = askGoodsForSaleInSettlement(unit, settlement);

        Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();
        Goods goods = null;
        
        for (;;) {
            if (forSale.isEmpty()) {
                // There is nothing to sell to the player
                canvas.showInformationMessage("trade.nothingToSell",
                                              settlement);
                return;
            }

            // Choose goods to buy
            goods = canvas.showSimpleChoiceDialog(unit.getTile(),
                                                  "buyProposition.text",
                                                  "buyProposition.nothing",
                                                  forSale);
            if (goods == null) break; // Trade aborted by the player

            int gold = -1; // Initially ask for a price
            for (;;) {
                gold = askBuyPriceFromSettlement(unit, settlement, goods, gold);
                if (gold == NO_TRADE) { // Proposal was refused
                    canvas.showInformationMessage("trade.noTrade",
                                                  settlement);
                    return;
                } else if (gold < NO_TRADE) { // failure
                    return;
                }

                // Show dialog for buy proposal
                boolean canBuy = gold <= player.getGold();
                switch (canvas.showBuyDialog(unit, settlement, goods, gold,
                                             canBuy)) {
                case CANCEL: // User cancelled
                    return;
                case BUY: // Accept price, make purchase
                    if (askBuyFromSettlement(unit, settlement, goods, gold)) {
                        canvas.updateGoldLabel(); // Assume success
                    }
                    return;
                case HAGGLE: // Try to negotiate a lower price
                    gold = gold * 9 / 10;
                    break;
                default:
                    throw new IllegalStateException("showBuyDialog fail");
                }
            }
        }
    }

    /**
     * Server query-response to get a list of goods for sale from a settlement.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return The list of goods for sale, or null on failure.
     */
    private List<Goods> askGoodsForSaleInSettlement(Unit unit,
                                                    Settlement settlement) {
        Client client = freeColClient.getClient();
        GoodsForSaleMessage message = new GoodsForSaleMessage(unit, settlement);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     message.getXMLElementTagName());
        if (reply == null) return null;

        Game game = freeColClient.getGame();
        ArrayList<Goods> goodsOffered = new ArrayList<Goods>();
        NodeList childNodes = reply.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            goodsOffered.add(new Goods(game, (Element) childNodes.item(i)));
        }
        return goodsOffered;
    }

    /**
     * Server query-response to ask the natives if a purchase is acceptable.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to trade.
     * @param gold The proposed price (including query on negative).
     * @return The asking price,
     *         or NO_TRADE if the trade is outright refused,
     *         or NO_TRADE-1 on error.
     */
    private int askBuyPriceFromSettlement(Unit unit, Settlement settlement,
                                          Goods goods, int gold) {
        Client client = freeColClient.getClient();
        BuyPropositionMessage message
            = new BuyPropositionMessage(unit, settlement, goods, gold);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     message.getXMLElementTagName());
        if (reply == null) return NO_TRADE - 1; // signal failure

        message = new BuyPropositionMessage(freeColClient.getGame(), reply);
        return message.getGold();
    }

    /**
     * Server query-response to buy the given goods from the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to buy.
     * @param gold The agreed price.
     * @return True if the server interaction succeeded.
     */
    private boolean askBuyFromSettlement(Unit unit, Settlement settlement,
                                         Goods goods, int gold) {
        Client client = freeColClient.getClient();
        BuyMessage message = new BuyMessage(unit, settlement, goods, gold);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "update");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * User interaction for selling to the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     */
    private void attemptSellToSettlement(Unit unit, Settlement settlement) {
        Canvas canvas = freeColClient.getCanvas();
        Client client = freeColClient.getClient();
        Goods goods = null;
        for (;;) {
            // Choose goods to sell
            goods = canvas.showSimpleChoiceDialog(unit.getTile(),
                                                  "sellProposition.text",
                                                  "sellProposition.nothing",
                                                  unit.getGoodsList());
            if (goods == null) break; // Trade aborted by the player

            int gold = -1; // Initially ask for a price
            for (;;) {
                gold = askSellPriceToSettlement(unit, settlement, goods, gold);
                if (gold == NO_NEED_FOR_THE_GOODS) {
                    canvas.showInformationMessage(StringTemplate.template("trade.noNeedForTheGoods")
                                                  .add("%goods%", goods.getNameKey()),
                                                  settlement);
                    return;
                } else if (gold == NO_TRADE) {
                    canvas.showInformationMessage("trade.noTrade",
                                                  settlement);
                    return;
                } else if (gold < NO_TRADE) { // error
                    return;
                }

                // Show dialog for sale proposal
                switch (canvas.showSellDialog(unit, settlement, goods, gold)) {
                case CANCEL:
                    return;
                case SELL: // Accepted price, make the sale
                    if (askSellToSettlement(unit, settlement, goods, gold)) {
                        canvas.updateGoldLabel(); // Assume success
                    }
                    return;
                case HAGGLE: // Ask for more money
                    gold = (gold * 11) / 10;
                    break;
                case GIFT: // Decide to make a gift of the goods
                    askDeliverGiftToSettlement(unit, settlement, goods);
                    return;
                default:
                    throw new IllegalStateException("showSellDialog fail");
                }
            }
        }
    }

    /**
     * Server query-response to ask the natives if a sale is acceptable.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to trade.
     * @param gold The proposed price (including query on negative).
     * @return The asking price, or NO_NEED_FOR_GOODS if they do not want them,
     *         NO_TRADE if the trade is outright refused,
     *         or NO_TRADE-1 on error.
     */
    private int askSellPriceToSettlement(Unit unit, Settlement settlement,
                                         Goods goods, int gold) {
        Client client = freeColClient.getClient();
        SellPropositionMessage message
            = new SellPropositionMessage(unit, settlement, goods, gold);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     message.getXMLElementTagName());
        if (reply == null) return NO_TRADE - 1; // Signal failure

        message = new SellPropositionMessage(freeColClient.getGame(), reply);
        return message.getGold();
    }

    /**
     * Server query-response to sell the given goods to the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to sell.
     * @param gold The agreed price.
     * @return True if the server interaction succeeded.
     */
    private boolean askSellToSettlement(Unit unit, Settlement settlement,
                                        Goods goods, int gold) {
        Client client = freeColClient.getClient();
        SellMessage message = new SellMessage(unit, settlement, goods, gold);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "update");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * User interaction for delivering a gift to the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     */
    private void attemptGiftToSettlement(Unit unit, Settlement settlement) {
        Canvas canvas = freeColClient.getCanvas();
        Goods goods = canvas.showSimpleChoiceDialog(unit.getTile(),
                                                    "gift.text", "cancel",
                                                    unit.getGoodsList());
        if (goods != null) {
            askDeliverGiftToSettlement(unit, settlement, goods);
        }
    }

    /**
     * Server query-response to give the given goods to the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to give.
     * @return True if the server interaction succeeded.
     */
    private boolean askDeliverGiftToSettlement(Unit unit, Settlement settlement,
                                               Goods goods) {
        Client client = freeColClient.getClient();
        DeliverGiftMessage message
            = new DeliverGiftMessage(unit, settlement, goods);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "update");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    // End of move-consequents


    /**
     * Detailed view of a foreign colony when in debug mode.
     *
     * @param tile The <code>Tile</code> with the colony.
     */
    public void debugForeignColony(Tile tile) {
        if (FreeCol.isInDebugMode() && tile != null) {
            DebugForeignColonyMessage message = new DebugForeignColonyMessage(tile);
            Element reply = askExpecting(freeColClient.getClient(),
                                         message.toXMLElement(),
                                         "update");
            if (reply != null) {
                // Similar sleight of hand as in moveSpy.
                Element tileElement = (Element) reply.getFirstChild();
                tile.readFromXMLElement(tileElement);
                freeColClient.getCanvas().showColonyPanel(tile.getColony());
                reply.removeChild(tileElement);
                freeColClient.getInGameInputHandler().update(reply);
            }
        }
    }


    /**
     * Claim a tile.
     *
     * @param tile The <code>Tile</code> to claim.
     * @param colony An optional <code>Colony</code> to own the tile.
     * @param offer An offer to pay.
     * @return True if the claim succeeded.
     */
    public boolean claimLand(Tile tile, Colony colony, int offer) {
        Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();
        if (freeColClient.getGame().getCurrentPlayer() != player) {
            canvas.showInformationMessage("notYourTurn");
            return false;
        }

        Player owner = tile.getOwner();
        int price = (owner == null) ? 0 : player.getLandPrice(tile);
        if (price < 0) { // not for sale
            return false;
        } else if (price > 0) { // for sale by natives
            if (offer >= price) { // offered more than enough
                price = offer;
            } else if (offer < 0) { // plan to steal
                price = ClaimLandMessage.STEAL_LAND;
            } else {
                boolean canAccept = price <= player.getGold();
                switch (canvas.showClaimDialog(tile, player, price,
                                               owner, canAccept)) {
                case CANCEL:
                    return false;
                case ACCEPT: // accepted price
                    break;
                case STEAL:
                    price = ClaimLandMessage.STEAL_LAND;
                    break;
                default:
                    throw new IllegalStateException("showClaimDialog fail");
                }
            }
        } // else price == 0 and we can just proceed

        // Ask the server
        if (askClaimLand(tile, colony, price) && tile.getOwner() == player) {
            canvas.updateGoldLabel();
            return true;
        }
        return false;
    }

    /**
     * Server query-response to claim a piece of land.
     *
     * @param tile The land to claim.
     * @param colony An optional <code>Colony</code> to own the land.
     * @param price The amount to pay.
     * @return True if the server interaction succeeded.
     */
    private boolean askClaimLand(Tile tile, Colony colony, int price) {
        Client client = freeColClient.getClient();
        ClaimLandMessage message = new ClaimLandMessage(tile, colony, price);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "update");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Check if a unit is a treasure train, and if it should be cashed in.
     * Transfers the gold carried by this unit to the {@link Player owner}.
     *
     * @param unit The <code>Unit</code> to be checked.
     * @return True if the unit was cashed in (and disposed).
     */
    public boolean checkCashInTreasureTrain(Unit unit) {
        if (!unit.canCarryTreasure() || !unit.canCashInTreasureTrain()) {
            return false; // Fail quickly if just not a candidate.
        }

        Canvas canvas = freeColClient.getCanvas();
        if (freeColClient.getGame().getCurrentPlayer()
            != freeColClient.getMyPlayer()) {
            canvas.showInformationMessage("notYourTurn");
            return false;
        }

        // Cash in or not?
        boolean cash;
        Tile tile = unit.getTile();
        Europe europe = unit.getOwner().getEurope();
        if (europe == null || unit.getLocation() == europe) {
            cash = true; // No need to check for transport.
        } else {
            String confirm = (unit.getTransportFee() == 0)
                ? "cashInTreasureTrain.free"
                : "cashInTreasureTrain.pay";
            cash = canvas.showConfirmDialog(unit.getTile(), confirm,
                                            "cashInTreasureTrain.yes",
                                            "cashInTreasureTrain.no");
        }

        // Update if cash in succeeds.
        FreeColGameObject oldLocation = (FreeColGameObject) unit.getLocation();
        if (cash && askCashInTreasureTrain(unit) && unit.isDisposed()) {
            oldLocation.firePropertyChange(Tile.UNIT_CHANGE, unit, null);
            canvas.updateGoldLabel();
            nextActiveUnit(tile);
            return true;
        }
        return false;
    }

    /**
     * Server query-response to cash in a treasure train.
     *
     * @param unit The treasure train <code>Unit</code> to cash in.
     * @return True if the server interaction succeeded.
     */
    private boolean askCashInTreasureTrain(Unit unit) {
        Client client = freeColClient.getClient();
        CashInTreasureTrainMessage message
            = new CashInTreasureTrainMessage(unit);
        Element reply = askExpecting(client, message.toXMLElement(),
                                    "multiple");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Boards a specified unit onto a carrier.
     * The carrier must be at the same location as the boarding unit.
     *
     * @param unit The <code>Unit</code> which is to board the carrier.
     * @param carrier The carrier to board.
     * @return True if the unit boards the carrier.
     */
    public boolean boardShip(Unit unit, Unit carrier) {
        if (freeColClient.getGame().getCurrentPlayer()
            != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return false;
        }

        // Sanity checks.
        if (unit == null) {
            logger.warning("unit == null");
            return false;
        }
        if (carrier == null) {
            logger.warning("Trying to load onto a non-existent carrier.");
            return false;
        }
        if (unit.isNaval()) {
            logger.warning("Trying to load a ship onto another carrier.");
            return false;
        }
        if (unit.isInEurope() != carrier.isInEurope()
            || unit.getTile() != carrier.getTile()) {
            logger.warning("Unit and carrier are not co-located.");
            return false;
        }

        // Proceed to board
        Location oldLocation = unit.getLocation();
        if (askEmbark(unit, carrier, null) && unit.getLocation() == carrier) {
            freeColClient.playSound(SoundEffect.LOAD_CARGO);
            if (oldLocation instanceof Tile) {
                ((Tile) oldLocation).firePropertyChange(Tile.UNIT_CHANGE, unit, null);
            } else if (oldLocation instanceof Europe) {
                ((Europe) oldLocation).firePropertyChange(Europe.UNIT_CHANGE, unit, null);
            }
            carrier.firePropertyChange(Unit.CARGO_CHANGE, null, unit);
            nextActiveUnit();
            return true;
        }
        return false;
    }

    /**
     * Server query-response for boarding a carrier.
     *
     * @param unit The <code>Unit</code> that is boarding.
     * @param carrier The carrier <code>Unit</code>.
     * @param direction An optional direction if the unit is boarding from
     *        an adjacent tile, or null if from the same tile.
     * @return True if the server interaction succeeded.
     */
    private boolean askEmbark(Unit unit, Unit carrier, Direction direction) {
        Client client = freeColClient.getClient();
        EmbarkMessage message = new EmbarkMessage(unit, carrier, direction);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Leave a ship.  The ship must be in harbour.
     *
     * @param unit The <code>Unit</code> which is to leave the ship.
     */
    public boolean leaveShip(Unit unit) {
        if (freeColClient.getGame().getCurrentPlayer()
            != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return false;
        }

        // Sanity check, and find our carrier before we get off.
        if (!(unit.getLocation() instanceof Unit)) {
            logger.warning("Unit " + unit.getId() + " is not on a carrier.");
            return false;
        }
        Unit carrier = (Unit) unit.getLocation();

        // Ask the server
        if (askDisembark(unit) && unit.getLocation() != carrier) {
            carrier.firePropertyChange(Unit.CARGO_CHANGE, unit, null);
            if (!checkCashInTreasureTrain(unit)) {
                Location newLocation = unit.getLocation();
                if (newLocation instanceof Tile) {
                    ((Tile) newLocation).firePropertyChange(Tile.UNIT_CHANGE, null, unit);
                } else if (newLocation instanceof Europe) {
                    ((Europe) newLocation).firePropertyChange(Europe.UNIT_CHANGE, null, unit);
                }
            }
            nextActiveUnit();
            return true;
        }
        return false;
    }

    /**
     * Server query-response for disembarking from a carrier.
     *
     * @param unit The <code>Unit</code> that is disembarking.
     * @return True if the server interaction succeeded.
     */
    private boolean askDisembark(Unit unit) {
        Client client = freeColClient.getClient();
        DisembarkMessage message = new DisembarkMessage(unit);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "update");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Loads a cargo onto a carrier.
     *
     * @param goods The <code>Goods</code> which are going aboard the carrier.
     * @param carrier The <code>Unit</code> acting as carrier.
     */
    public void loadCargo(Goods goods, Unit carrier) {
        if (freeColClient.getGame().getCurrentPlayer()
            != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        // Sanity checks.
        Colony colony = null;
        if (goods == null) {
            throw new IllegalArgumentException("Null goods.");
        } else if (goods.getAmount() <= 0) {
            throw new IllegalArgumentException("Empty goods.");
        } else if (carrier == null) {
            throw new IllegalArgumentException("Null carrier.");
        } else if (carrier.isInEurope()) {
            ;
        } else if ((colony = carrier.getColony()) == null) {
            throw new IllegalArgumentException("Carrier not at colony or Europe.");
        }

        // Try to load.
        if (loadGoods(goods, colony, carrier)) {
            freeColClient.playSound(SoundEffect.LOAD_CARGO);
        }
    }

    /**
     * Unload cargo. If the unit carrying the cargo is not in a
     * harbour, or if the given boolean is true, the goods will be
     * dumped.
     *
     * @param goods The <code>Goods<code> to unload.
     * @param dump If true, dump the goods.
     */
    public void unloadCargo(Goods goods, boolean dump) {
        if (freeColClient.getGame().getCurrentPlayer()
            != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        // Sanity tests.
        if (goods == null) {
            throw new IllegalArgumentException("Null goods.");
        } else if (goods.getAmount() <= 0) {
            throw new IllegalArgumentException("Empty goods.");
        }
        Unit carrier = null;
        if (!(goods.getLocation() instanceof Unit)) {
            throw new IllegalArgumentException("Unload from non-unit.");
        }
        carrier = (Unit) goods.getLocation();
        Colony colony = null;
        if (!carrier.isInEurope()) {
            if (carrier.getTile() == null) {
                throw new IllegalArgumentException("Carrier with null location.");
            }
            colony = carrier.getColony();
            if (!dump && colony == null) {
                throw new IllegalArgumentException("Unload is really a dump.");
            }
        }

        // Try to unload.  TODO: should there be a sound for this?
        unloadGoods(goods, carrier, colony);
    }


    /**
     * Buy goods in Europe.
     * The amount of goods is adjusted to the space in the carrier.
     *
     * @param type The type of goods to buy.
     * @param amount The amount of goods to buy.
     * @param carrier The <code>Unit</code> acting as carrier.
     * @return True if the purchase succeeds.
     */
    public boolean buyGoods(GoodsType type, int amount, Unit carrier) {
        Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();
        if (freeColClient.getGame().getCurrentPlayer() != player) {
            canvas.showInformationMessage("notYourTurn");
            return false;
        }

        // Sanity checks.  Should not happen!
        if (type == null) {
            throw new NullPointerException("Goods type must not be null.");
        } else if (carrier == null) {
            throw new NullPointerException("Carrier must not be null.");
        } else if (carrier.getOwner() != player) {
            throw new IllegalArgumentException("Carrier owned by someone else.");
        } else if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        // Size check, if there are spare holds they can be filled, but...
        int toBuy = GoodsContainer.CARGO_SIZE;
        if (carrier.getSpaceLeft() <= 0) {
            // ...if there are no spare holds, we can only fill a hold
            // already partially filled with this type, otherwise fail.
            int partial = carrier.getGoodsContainer().getGoodsCount(type)
                % GoodsContainer.CARGO_SIZE;
            if (partial == 0) return false;
            toBuy -= partial;
        }
        if (amount < toBuy) toBuy = amount;

        // Check that the purchase is funded.
        Market market = player.getMarket();
        if (market.getBidPrice(type, toBuy) > player.getGold()) {
            canvas.errorMessage("notEnoughGold");
            return false;
        }

        // Try to purchase.
        int oldAmount = carrier.getGoodsContainer().getGoodsCount(type);
        int newAmount;
        if (askBuyGoods(carrier, type, toBuy)
            && (newAmount = carrier.getGoodsContainer().getGoodsCount(type)) > oldAmount) {
            freeColClient.playSound(SoundEffect.LOAD_CARGO);
            canvas.updateGoldLabel();
            carrier.firePropertyChange(Unit.CARGO_CHANGE, oldAmount, newAmount);
            for (TransactionListener listener : market.getTransactionListener()) {
                listener.logPurchase(type, toBuy, market.costToBuy(type));
            }
            return true;
        }

        // Purchase failed for some reason.
        return false;
    }

    /**
     * Server query-response for buying goods in Europe.
     *
     * @param carrier The <code>Unit</code> to load with the goods.
     * @param type The type of goods to buy.
     * @param amount The amount of goods to buy.
     * @return True if the server interaction succeeded.
     */
    private boolean askBuyGoods(Unit carrier, GoodsType type, int amount) {
        Client client = freeColClient.getClient();
        BuyGoodsMessage message = new BuyGoodsMessage(carrier, type, amount);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Sells goods in Europe.
     *
     * @param goods The goods to be sold.
     * @return True if the sale succeeds.
     */
    public boolean sellGoods(Goods goods) {
        Player player = freeColClient.getMyPlayer();
        Canvas canvas = freeColClient.getCanvas();
        if (freeColClient.getGame().getCurrentPlayer() != player) {
            canvas.showInformationMessage("notYourTurn");
            return false;
        }

        // Sanity checks.
        if (goods == null) {
            throw new NullPointerException("Goods must not be null.");
        }
        Unit carrier = null;
        if (goods.getLocation() instanceof Unit) {
            carrier = (Unit) goods.getLocation();
        }
        if (carrier == null) {
            throw new IllegalStateException("Goods not on carrier.");
        }
        if (!carrier.isInEurope()) {
            throw new IllegalStateException("Goods not on carrier in Europe.");
        }

        // Try to sell.
        if (askSellGoods(goods, carrier)) {
            freeColClient.playSound(SoundEffect.SELL_CARGO);
            canvas.updateGoldLabel();
            carrier.firePropertyChange(Unit.CARGO_CHANGE, goods, null);
            Market market = player.getMarket();
            for (TransactionListener listener : market.getTransactionListener()) {
                listener.logSale(goods.getType(), goods.getAmount(),
                                 market.paidForSale(goods.getType()),
                                 player.getTax());
            }
            return true;
        }

        // Sale failed for some reason.
        return false;
    }

    /**
     * Server query-response for selling goods in Europe.
     *
     * @param goods The <code>Goods</code> to sell.
     * @param carrier The <code>Unit</code> in Europe with the goods.
     * @return True if the server interaction succeeded.
     */
    private boolean askSellGoods(Goods goods, Unit carrier) {
        Client client = freeColClient.getClient();
        SellGoodsMessage message = new SellGoodsMessage(goods, carrier);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "multiple");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Clear the speciality of a Unit, making it a Free Colonist.
     *
     * @param unit The <code>Unit</code> to clear the speciality of.
     */
    public void clearSpeciality(Unit unit) {
        Canvas canvas = freeColClient.getCanvas();
        if (freeColClient.getGame().getCurrentPlayer()
            != freeColClient.getMyPlayer()) {
            canvas.showInformationMessage("notYourTurn");
            return;
        }

        // Check this makes sense and confirm.
        UnitType oldType = unit.getType();
        UnitType newType = oldType.getUnitTypeChange(ChangeType.CLEAR_SKILL,
                                                     unit.getOwner());
        if (newType == null) {
            StringTemplate template = StringTemplate.template("clearSpeciality.impossible")
                .addStringTemplate("%unit%", Messages.getLabel(unit));
            canvas.showInformationMessage(template, unit);
            return;
        }

        Tile tile = (canvas.isShowingSubPanel()) ? null : unit.getTile();
        if (!canvas.showConfirmDialog(tile,
                                      StringTemplate.template("clearSpeciality.areYouSure")
                                      .addStringTemplate("%oldUnit%", Messages.getLabel(unit))
                                      .add("%unit%", newType.getNameKey()),
                                      "yes", "no")) {
            return;
        }

        // Try to clear.
        if (askClearSpeciality(unit) && unit.getType() == newType) {
            ;//unit.firePropertyChange(Unit.UNIT_TYPE_CHANGE, oldType, newType);
        }
        nextActiveUnit();
    }

    /**
     * Server query-response for clearing a unit speciality.
     *
     * @param unit The <code>Unit</code> to operate on.
     * @return True if the server interaction succeeded.
     */
    private boolean askClearSpeciality(Unit unit) {
        Client client = freeColClient.getClient();
        ClearSpecialityMessage message = new ClearSpecialityMessage(unit);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "update");
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Sets the export settings of the custom house.
     *
     * @param colony The colony with the custom house.
     * @param goodsType The goods for which to set the settings.
     */
    public void setGoodsLevels(Colony colony, GoodsType goodsType) {
        Client client = freeColClient.getClient();
        ExportData data = colony.getExportData(goodsType);

        Element setGoodsLevelsElement = Message.createNewRootElement("setGoodsLevels");
        setGoodsLevelsElement.setAttribute("colony", colony.getId());
        setGoodsLevelsElement.appendChild(data.toXMLElement(colony.getOwner(), setGoodsLevelsElement
                                                            .getOwnerDocument()));

        client.sendAndWait(setGoodsLevelsElement);
    }

    /**
     * Equips or unequips a <code>Unit</code> with a certain type of
     * <code>Goods</code>.
     *
     * @param unit The <code>Unit</code>.
     * @param type an <code>EquipmentType</code> value
     * @param amount How many of these goods the unit should have.
     */
    public void equipUnit(Unit unit, EquipmentType type, int amount) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        if (amount == 0) {
            // no changes
            return;
        }

        Client client = freeColClient.getClient();
        Player myPlayer = freeColClient.getMyPlayer();

        Unit carrier = null;
        if (unit.isOnCarrier()) {
            carrier = (Unit) unit.getLocation();
            if (!leaveShip(unit)) return;
        }

        Element equipUnitElement = Message.createNewRootElement("equipUnit");
        equipUnitElement.setAttribute("unit", unit.getId());
        equipUnitElement.setAttribute("type", type.getId());
        equipUnitElement.setAttribute("amount", Integer.toString(amount));

        if (amount > 0) {
            for (AbstractGoods requiredGoods : type.getGoodsRequired()) {
                GoodsType goodsType = requiredGoods.getType();
                if (unit.isInEurope()) {
                    if (!myPlayer.canTrade(goodsType)) {
                        payArrears(goodsType);
                        if (!myPlayer.canTrade(goodsType)) {
                            return; // The user cancelled the action.
                        }
                    }
                }
            }
            unit.equipWith(type, amount);
        } else {
            unit.removeEquipment(type, -amount);
        }

        freeColClient.getCanvas().updateGoldLabel();

        client.sendAndWait(equipUnitElement);

        if (unit.getLocation() instanceof Colony || unit.getLocation() instanceof Building
            || unit.getLocation() instanceof ColonyTile) {
            putOutsideColony(unit);
        } else if (carrier != null) {
            boardShip(unit, carrier);
        }
    }

    /**
     * Moves a <code>Unit</code> to a <code>WorkLocation</code>.
     *
     * @param unit The <code>Unit</code>.
     * @param workLocation The <code>WorkLocation</code>.
     */
    public void work(Unit unit, WorkLocation workLocation) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Tile tile = workLocation.getTile();
        if ((tile.getOwner() != unit.getOwner()
             || tile.getOwningSettlement() != workLocation.getColony())
            && !claimLand(tile, workLocation.getColony(), 0)) {
            logger.warning("Unit " + unit.getId()
                           + " is unable to claim tile " + tile.toString());
            return;
        }

        Client client = freeColClient.getClient();
        Element workElement = Message.createNewRootElement("work");
        workElement.setAttribute("unit", unit.getId());
        workElement.setAttribute("workLocation", workLocation.getId());

        unit.work(workLocation);

        client.sendAndWait(workElement);
        if (workLocation instanceof ColonyTile) {
            ((ColonyTile) workLocation).firePropertyChange(ColonyTile.UNIT_CHANGE, null, unit);
        }
    }

    /**
     * Puts the specified unit outside the colony.
     *
     * @param unit The <code>Unit</code>
     * @return <i>true</i> if the unit was successfully put outside the colony.
     */
    public boolean putOutsideColony(Unit unit) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            throw new IllegalStateException("Not your turn.");
        } else if (unit.getColony() == null) {
            throw new IllegalStateException("Unit is not in colony.");
        } else if (!unit.getColony().canReducePopulation()) {
            throw new IllegalStateException("Colony can not reduce population.");
        }

        Colony colony = unit.getColony();
        colony.updatePopulation(-1);
        Location oldLocation = unit.getLocation();

        Element putOutsideColonyElement = Message.createNewRootElement("putOutsideColony");
        putOutsideColonyElement.setAttribute("unit", unit.getId());

        Client client = freeColClient.getClient();
        Element reply = client.ask(putOutsideColonyElement);
        if (reply != null && reply.getTagName().equals("update")) {
            freeColClient.getInGameInputHandler().update(reply);
            // TODO: this really should be handled by the update
            if (oldLocation instanceof Building) {
                ((Building) oldLocation).firePropertyChange(Building.UNIT_CHANGE, unit, null);
            } else if (oldLocation instanceof ColonyTile) {
                ((ColonyTile) oldLocation).firePropertyChange(ColonyTile.UNIT_CHANGE, unit, null);
            }
            unit.getTile().firePropertyChange(Tile.UNIT_CHANGE, null, unit);
        } else {
            logger.warning("putOutsideColony message missing update");
        }

        return true;
    }

    /**
     * Changes the work type of this <code>Unit</code>.
     *
     * @param unit The <code>Unit</code>
     * @param workType The new <code>GoodsType</code> to produce.
     */
    public void changeWorkType(Unit unit, GoodsType workType) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();

        Element changeWorkTypeElement = Message.createNewRootElement("changeWorkType");
        changeWorkTypeElement.setAttribute("unit", unit.getId());
        changeWorkTypeElement.setAttribute("workType", workType.getId());

        unit.setWorkType(workType);

        client.sendAndWait(changeWorkTypeElement);
    }

    /**
     * Changes the work type of this <code>Unit</code>.
     *
     * @param unit The <code>Unit</code>
     * @param improvementType a <code>TileImprovementType</code> value
     */
    public void changeWorkImprovementType(Unit unit, TileImprovementType improvementType) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        if (!(unit.checkSetState(UnitState.IMPROVING))) {
            return; // Don't bother (and don't log, this is not exceptional)
        }

        if (!improvementType.isNatural()
            && freeColClient.getMyPlayer() != unit.getTile().getOwner()
            && !claimLand(unit.getTile(), null, 0)) {
            logger.warning("Unit " + unit.getId()
                           + " is unable to claim tile " + unit.getTile().toString());
            return;
        }

        Element changeWorkTypeElement = Message.createNewRootElement("workImprovement");
        changeWorkTypeElement.setAttribute("unit", unit.getId());
        changeWorkTypeElement.setAttribute("improvementType", improvementType.getId());

        Element reply = freeColClient.getClient().ask(changeWorkTypeElement);
        Element containerElement = getChildElement(reply, TileItemContainer.getXMLElementTagName());
        if (containerElement != null) {
            TileItemContainer container = (TileItemContainer) freeColClient.getGame()
                .getFreeColGameObject(containerElement.getAttribute("ID"));
            if (container == null) {
                container = new TileItemContainer(freeColClient.getGame(), unit.getTile(), containerElement);
                unit.getTile().setTileItemContainer(container);
            } else {
                container.readFromXMLElement(containerElement);
            }
        }
        Element improvementElement = getChildElement(reply, TileImprovement.getXMLElementTagName());
        if (improvementElement != null) {
            TileImprovement improvement = (TileImprovement) freeColClient.getGame()
                .getFreeColGameObject(improvementElement.getAttribute("ID"));
            if (improvement == null) {
                improvement = new TileImprovement(freeColClient.getGame(), improvementElement);
                unit.getTile().add(improvement);
            } else {
                improvement.readFromXMLElement(improvementElement);
            }
            unit.work(improvement);
        }
    }

    /**
     * Assigns a unit to a teacher <code>Unit</code>.
     *
     * @param student an <code>Unit</code> value
     * @param teacher an <code>Unit</code> value
     */
    public void assignTeacher(Unit student, Unit teacher) {
        Player player = freeColClient.getMyPlayer();

        if (freeColClient.getGame().getCurrentPlayer() != player) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        if (!student.canBeStudent(teacher)) {
            throw new IllegalStateException("Unit can not be student!");
        }
        if (!teacher.getColony().canTrain(teacher)) {
            throw new IllegalStateException("Unit can not be teacher!");
        }
        if (student.getOwner() != player) {
            throw new IllegalStateException("Student is not your unit!");
        }
        if (teacher.getOwner() != player) {
            throw new IllegalStateException("Teacher is not your unit!");
        }
        if (student.getColony() != teacher.getColony()) {
            throw new IllegalStateException("Student and teacher are not in the same colony!");
        }
        if (!(student.getLocation() instanceof WorkLocation)) {
            throw new IllegalStateException("Student is not in a WorkLocation!");
        }

        Element assignTeacherElement = Message.createNewRootElement("assignTeacher");
        assignTeacherElement.setAttribute("student", student.getId());
        assignTeacherElement.setAttribute("teacher", teacher.getId());

        if (student.getTeacher() != null) {
            student.getTeacher().setStudent(null);
        }
        student.setTeacher(teacher);
        if (teacher.getStudent() != null) {
            teacher.getStudent().setTeacher(null);
        }
        teacher.setStudent(student);

        freeColClient.getClient().sendAndWait(assignTeacherElement);
    }

    /**
     * Changes the current construction project of a <code>Colony</code>.
     *
     * @param colony The <code>Colony</code>
     */
    public void setBuildQueue(Colony colony, List<BuildableType> buildQueue) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        colony.setBuildQueue(buildQueue);

        Element setBuildQueueElement = Message.createNewRootElement("setBuildQueue");
        setBuildQueueElement.setAttribute("colony", colony.getId());
        setBuildQueueElement.setAttribute("size", Integer.toString(buildQueue.size()));
        for (int x = 0; x < buildQueue.size(); x++) {
            setBuildQueueElement.setAttribute("x" + Integer.toString(x), buildQueue.get(x).getId());
        }
        freeColClient.getClient().sendAndWait(setBuildQueueElement);
    }

    /**
     * Changes the state of this <code>Unit</code>.
     *
     * @param unit The <code>Unit</code>
     * @param state The state of the unit.
     */
    public void changeState(Unit unit, UnitState state) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();
        Game game = freeColClient.getGame();
        Canvas canvas = freeColClient.getCanvas();

        if (!(unit.checkSetState(state))) {
            return; // Don't bother (and don't log, this is not exceptional)
        }
        if (state == UnitState.FORTIFYING && unit.isOffensiveUnit() &&
            !unit.hasAbility("model.ability.piracy")) { // check if it's going to occupy a work tile
            Tile tile = unit.getTile();
            if (tile != null && tile.getOwningSettlement() != null) { // check stance with settlement's owner
                Player myPlayer = unit.getOwner();
                Player enemy = tile.getOwningSettlement().getOwner();
                if (myPlayer != enemy && myPlayer.getStance(enemy) != Stance.ALLIANCE
                    && !confirmHostileAction(unit, tile.getOwningSettlement().getTile())) { // player has aborted
                    return;
                }
            }
        }

        unit.setState(state);

        // NOTE! The call to nextActiveUnit below can lead to the dreaded
        // "not your turn" error, so let's finish networking first.
        Element changeStateElement = Message.createNewRootElement("changeState");
        changeStateElement.setAttribute("unit", unit.getId());
        changeStateElement.setAttribute("state", state.toString());
        client.sendAndWait(changeStateElement);

        if (!freeColClient.getCanvas().isShowingSubPanel() &&
            (unit.getMovesLeft() == 0 || unit.getState() == UnitState.SENTRY ||
             unit.getState() == UnitState.SKIPPED)) {
            nextActiveUnit();
        } else {
            freeColClient.getCanvas().refresh();
        }

    }

    /**
     * Clears the orders of the given <code>Unit</code> The orders are cleared
     * by making the unit {@link UnitState#ACTIVE} and setting a null destination
     *
     * @param unit The <code>Unit</code>.
     */
    public void clearOrders(Unit unit) {
        Canvas canvas = freeColClient.getCanvas();
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            canvas.showInformationMessage("notYourTurn");
            return;
        }

        if (unit == null) {
            return;
        }

        // Ask the user for confirmation, as this is a classic mistake.
        // Cancelling a pioneer terrain improvement is a waste of many turns.
        if (unit.getState() == UnitState.IMPROVING
            && !canvas.showConfirmDialog(unit.getTile(),
                                         "model.unit.confirmCancelWork",
                                         "yes", "no",
                                         "%turns%", Integer.toString(unit.getWorkLeft()))) {
            return;
        }

        /*
         * report to server, in order not to restore destination if it's
         * received in a update message
         */
        clearGotoOrders(unit);
        assignTradeRoute(unit, TradeRoute.NO_TRADE_ROUTE);
        changeState(unit, UnitState.ACTIVE);
    }

    /**
     * Clears the orders of the given <code>Unit</code>. The orders are
     * cleared by making the unit {@link UnitState#ACTIVE}.
     *
     * @param unit The <code>Unit</code>.
     */
    public void clearGotoOrders(Unit unit) {
        if (unit == null) {
            return;
        }

        /*
         * report to server, in order not to restore destination if it's
         * received in a update message
         */
        if (unit.getDestination() != null)
            setDestination(unit, null);
    }

    /**
     * Moves the specified unit to Europe.
     * 
     * @param unit The unit to be moved to Europe.
     */
    public void moveToEurope(Unit unit) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();

        unit.moveToEurope();

        Element moveToEuropeElement = Message.createNewRootElement("moveToEurope");
        moveToEuropeElement.setAttribute("unit", unit.getId());

        client.sendAndWait(moveToEuropeElement);
    }

    /**
     * Moves the specified unit to America.
     * 
     * @param unit The unit to be moved to America.
     */
    public void moveToAmerica(Unit unit) {
        final Canvas canvas = freeColClient.getCanvas();
        final Player player = freeColClient.getMyPlayer();
        if (freeColClient.getGame().getCurrentPlayer() != player) {
            canvas.showInformationMessage("notYourTurn");
            return;
        }

        final Client client = freeColClient.getClient();
        final ClientOptions co = canvas.getClient().getClientOptions();

        // Ask for autoload emigrants
        if (unit.getLocation() instanceof Europe) {
            final boolean autoload = co.getBoolean(ClientOptions.AUTOLOAD_EMIGRANTS);
            if (autoload) {
                int spaceLeft = unit.getSpaceLeft();
                List<Unit> unitsInEurope = new ArrayList<Unit>(unit.getLocation().getUnitList());
                for (Unit possiblePassenger : unitsInEurope) {
                    if (possiblePassenger.isNaval()) {
                        continue;
                    }
                    if (possiblePassenger.getType().getSpaceTaken() <= spaceLeft) {
                        boardShip(possiblePassenger, unit);
                        spaceLeft -= possiblePassenger.getType().getSpaceTaken();
                    } else {
                        break;
                    }
                }
            }
        }
        unit.moveToAmerica();

        Element moveToAmericaElement = Message.createNewRootElement("moveToAmerica");
        moveToAmericaElement.setAttribute("unit", unit.getId());

        client.sendAndWait(moveToAmericaElement);
    }

    /**
     * Trains a unit of a specified type in Europe.
     * 
     * @param unitType The type of unit to be trained.
     */
    public void trainUnitInEurope(UnitType unitType) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        Game game = freeColClient.getGame();
        Player myPlayer = freeColClient.getMyPlayer();
        Europe europe = myPlayer.getEurope();

        if (myPlayer.getGold() < europe.getUnitPrice(unitType)) {
            canvas.errorMessage("notEnoughGold");
            return;
        }

        Element trainUnitInEuropeElement = Message.createNewRootElement("trainUnitInEurope");
        trainUnitInEuropeElement.setAttribute("unitType", unitType.getId());

        Element reply = client.ask(trainUnitInEuropeElement);
        if (reply.getTagName().equals("trainUnitInEuropeConfirmed")) {
            Element unitElement = (Element) reply.getFirstChild();
            Unit unit = (Unit) game.getFreeColGameObject(unitElement.getAttribute("ID"));
            if (unit == null) {
                unit = new Unit(game, unitElement);
            } else {
                unit.readFromXMLElement(unitElement);
            }
            europe.train(unit);
        } else {
            logger.warning("Could not train unit in europe.");
            return;
        }
        canvas.updateGoldLabel();
    }

    /**
     * Buys the remaining hammers and tools for the {@link Building} currently
     * being built in the given <code>Colony</code>.
     * 
     * @param colony The {@link Colony} where the building should be bought.
     */
    public void payForBuilding(Colony colony) {
        Canvas canvas = freeColClient.getCanvas();
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            canvas.showInformationMessage("notYourTurn");
            return;
        }

        if (!canvas.showConfirmDialog(null, "payForBuilding.text",
                                      "payForBuilding.yes", "payForBuilding.no",
                                      "%replace%", Integer.toString(colony.getPriceForBuilding()))) {
            return;
        }

        if (!colony.canPayToFinishBuilding()) {
            canvas.errorMessage("notEnoughGold");
            return;
        }

        Element payForBuildingElement = Message.createNewRootElement("payForBuilding");
        payForBuildingElement.setAttribute("colony", colony.getId());

        colony.payForBuilding();

        freeColClient.getClient().sendAndWait(payForBuildingElement);
    }

    /**
     * Recruit a unit from a specified "slot" in Europe.
     * 
     * @param slot The slot to recruit the unit from. Either 1, 2 or 3.
     */
    public void recruitUnitInEurope(int slot) {
        Canvas canvas = freeColClient.getCanvas();
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            canvas.showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();
        Game game = freeColClient.getGame();
        Player myPlayer = freeColClient.getMyPlayer();
        Europe europe = myPlayer.getEurope();

        if (myPlayer.getGold() < myPlayer.getRecruitPrice()) {
            canvas.errorMessage("notEnoughGold");
            return;
        }

        Element recruitUnitInEuropeElement = Message.createNewRootElement("recruitUnitInEurope");
        recruitUnitInEuropeElement.setAttribute("slot", Integer.toString(slot));

        Element reply = client.ask(recruitUnitInEuropeElement);
        if (reply.getTagName().equals("recruitUnitInEuropeConfirmed")) {
            Element unitElement = (Element) reply.getFirstChild();
            Unit unit = (Unit) game.getFreeColGameObject(unitElement.getAttribute("ID"));
            if (unit == null) {
                unit = new Unit(game, unitElement);
            } else {
                unit.readFromXMLElement(unitElement);
            }
            String unitId = reply.getAttribute("newRecruitable");
            UnitType unitType = FreeCol.getSpecification().getUnitType(unitId);
            europe.recruit(slot, unit, unitType);
        } else {
            logger.warning("Could not recruit the specified unit in europe.");
            return;
        }
        canvas.updateGoldLabel();
    }

    /**
     * Updates a trade route.
     * 
     * @param route The trade route to update.
     */
    public void updateTradeRoute(TradeRoute route) {
        logger.finest("Entering method updateTradeRoute");
        /*
         * if (freeColClient.getGame().getCurrentPlayer() !=
         * freeColClient.getMyPlayer()) {
         * freeColClient.getCanvas().showInformationMessage("notYourTurn");
         * return; }
         */
        Element tradeRouteElement = Message.createNewRootElement("updateTradeRoute");
        tradeRouteElement.appendChild(route.toXMLElement(null, tradeRouteElement.getOwnerDocument()));
        freeColClient.getClient().sendAndWait(tradeRouteElement);

    }

    /**
     * Sets the trade routes for this player
     * 
     * @param routes The trade routes to set.
     */
    public void setTradeRoutes(List<TradeRoute> routes) {
        Player myPlayer = freeColClient.getMyPlayer();
        myPlayer.setTradeRoutes(routes);
        /*
         * if (freeColClient.getGame().getCurrentPlayer() !=
         * freeColClient.getMyPlayer()) {
         * freeColClient.getCanvas().showInformationMessage("notYourTurn");
         * return; }
         */
        Element tradeRoutesElement = Message.createNewRootElement("setTradeRoutes");
        for(TradeRoute route : routes) {
            Element routeElement = tradeRoutesElement.getOwnerDocument().createElement(TradeRoute.getXMLElementTagName());
            routeElement.setAttribute("id", route.getId());
            tradeRoutesElement.appendChild(routeElement);
        }
        freeColClient.getClient().sendAndWait(tradeRoutesElement);

    }

    /**
     * Assigns a trade route to a unit.
     * 
     * @param unit The unit to assign a trade route to.
     */
    public void assignTradeRoute(Unit unit) {
        Canvas canvas = freeColClient.getCanvas();
        assignTradeRoute(unit, canvas.showTradeRouteDialog(unit));
    }

    public void assignTradeRoute(Unit unit, TradeRoute tradeRoute) {
        if (tradeRoute != null) {
            Element assignTradeRouteElement = Message.createNewRootElement("assignTradeRoute");
            assignTradeRouteElement.setAttribute("unit", unit.getId());
            if (tradeRoute == TradeRoute.NO_TRADE_ROUTE) {
                unit.setTradeRoute(null);
                freeColClient.getClient().sendAndWait(assignTradeRouteElement);
                setDestination(unit, null);
            } else {
                unit.setTradeRoute(tradeRoute);
                assignTradeRouteElement.setAttribute("tradeRoute", tradeRoute.getId());
                freeColClient.getClient().sendAndWait(assignTradeRouteElement);
                Location location = unit.getLocation();
                if (location instanceof Tile)
                    location = ((Tile) location).getColony();
                if (tradeRoute.getStops().get(0).getLocation() == location) {
                    followTradeRoute(unit);
                } else if (freeColClient.getGame().getCurrentPlayer() == freeColClient.getMyPlayer()) {
                    moveToDestination(unit);
                }
            }
        }
    }

    /**
     * Pays the tax arrears on this type of goods.
     * 
     * @param goods The goods for which to pay arrears.
     */
    public void payArrears(Goods goods) {
        payArrears(goods.getType());
    }

    /**
     * Pays the tax arrears on this type of goods.
     * 
     * @param type The type of goods for which to pay arrears.
     */
    public void payArrears(GoodsType type) {
        Canvas canvas = freeColClient.getCanvas();
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            canvas.showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();
        Player player = freeColClient.getMyPlayer();

        int arrears = player.getArrears(type);
        if (player.getGold() >= arrears) {
            if (canvas.showConfirmDialog(null, "model.europe.payArrears",
                                         "ok", "cancel",
                                         "%replace%", String.valueOf(arrears))) {
                player.modifyGold(-arrears);
                canvas.updateGoldLabel();
                player.resetArrears(type);
                // send to server
                Element payArrearsElement = Message.createNewRootElement("payArrears");
                payArrearsElement.setAttribute("goodsType", type.getId());
                client.sendAndWait(payArrearsElement);
            }
        } else {
            canvas.showInformationMessage("model.europe.cantPayArrears",
                                          "%amount%", String.valueOf(arrears));
        }
    }

    /**
     * Purchases a unit of a specified type in Europe.
     * 
     * @param unitType The type of unit to be purchased.
     */
    public void purchaseUnitFromEurope(UnitType unitType) {
        trainUnitInEurope(unitType);
    }

    /**
     * Gathers information about opponents.
     */
    public Element getForeignAffairsReport() {
        return freeColClient.getClient().ask(Message.createNewRootElement("foreignAffairs"));
    }

    /**
     * Retrieves high scores from server.
     */
    public Element getHighScores() {
        return freeColClient.getClient().ask(Message.createNewRootElement("highScores"));
    }

    /**
     * Gathers information about the REF.
     * @return a <code>List</code> value
     */
    public List<AbstractUnit> getREFUnits() {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return Collections.emptyList();
        }

        Element reply = freeColClient.getClient().ask(Message.createNewRootElement("getREFUnits"));
        if (reply == null) {
            return Collections.emptyList();
        } else {
            List<AbstractUnit> result = new ArrayList<AbstractUnit>();
            NodeList childElements = reply.getChildNodes();
            for (int index = 0; index < childElements.getLength(); index++) {
                AbstractUnit unit = new AbstractUnit();
                unit.readFromXMLElement((Element) childElements.item(index));
                result.add(unit);
            }
            return result;
        }
    }

    /**
     * Disbands the active unit.
     */
    public void disbandActiveUnit() {
        Canvas canvas = freeColClient.getCanvas();
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            canvas.showInformationMessage("notYourTurn");
            return;
        }

        GUI gui = freeColClient.getGUI();
        Unit unit = gui.getActiveUnit();
        Client client = freeColClient.getClient();

        if (unit == null) {
            return;
        }

        Tile tile = (canvas.isShowingSubPanel()) ? null : unit.getTile();
        if (!canvas.showConfirmDialog(tile, "disbandUnit.text",
                                      "disbandUnit.yes", "disbandUnit.no")) {
            return;
        }

        Element disbandUnit = Message.createNewRootElement("disbandUnit");
        disbandUnit.setAttribute("unit", unit.getId());

        unit.dispose();

        client.sendAndWait(disbandUnit);

        nextActiveUnit();
    }

    /**
     * Centers the map on the selected tile.
     */
    public void centerActiveUnit() {
        Unit activeUnit = freeColClient.getGUI().getActiveUnit();
        if (activeUnit == null){
            return;
        }

        centerOnUnit(activeUnit);
    }

    /**
     * Centers the map on the given unit location.
     */
    public void centerOnUnit(Unit unit) {
        // Sanitation
        if(unit == null){
            return;
        }
        Tile unitTile = unit.getTile();
        if(unitTile == null){
            return;
        }
        
        freeColClient.getGUI().setFocus(unitTile.getPosition());
    }
    
    /**
     * Executes the units' goto orders.
     */
    public void executeGotoOrders() {
        executeGoto = true;
        nextActiveUnit(null);
    }

    /**
     * Makes a new unit active.
     */
    public void nextActiveUnit() {
        nextActiveUnit(null);
    }

    /**
     * Makes a new unit active. Displays any new <code>ModelMessage</code>s
     * (uses {@link #nextModelMessage}).
     * 
     * @param tile The tile to select if no new unit can be made active.
     */
    public void nextActiveUnit(Tile tile) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        nextModelMessage();

        Canvas canvas = freeColClient.getCanvas();
        Player myPlayer = freeColClient.getMyPlayer();
        if (endingTurn || executeGoto) {
            while (!freeColClient.getCanvas().isShowingSubPanel() && myPlayer.hasNextGoingToUnit()) {
                Unit unit = myPlayer.getNextGoingToUnit();
                moveToDestination(unit);
                nextModelMessage();
                if (unit.getMovesLeft() > 0) {
                    if (endingTurn) {
                        unit.setMovesLeft(0);
                    } else {
                        return;
                    }
                }
            }

            if (!myPlayer.hasNextGoingToUnit() && !freeColClient.getCanvas().isShowingSubPanel()) {
                if (endingTurn) {
                    canvas.getGUI().setActiveUnit(null);
                    endingTurn = false;

                    Element endTurnElement = Message.createNewRootElement("endTurn");
                    freeColClient.getClient().send(endTurnElement);
                    return;
                } else {
                    executeGoto = false;
                }
            }
        }
        
        GUI gui = freeColClient.getGUI();
        Unit nextActiveUnit = myPlayer.getNextActiveUnit();

        if (nextActiveUnit != null) {
            canAutoEndTurn = true;
            gui.setActiveUnit(nextActiveUnit);
        } else {
            // no more active units, so we can move the others
            nextActiveUnit = myPlayer.getNextGoingToUnit();
            if (nextActiveUnit != null) {
                moveToDestination(nextActiveUnit);
            } else if (tile != null) {
                Position p = tile.getPosition();
                if (p != null) {
                    // this really shouldn't happen
                    gui.setSelectedTile(p);
                }
                gui.setActiveUnit(null);
            } else {
                gui.setActiveUnit(null);
            }
            
            if (canAutoEndTurn && !endingTurn
                && freeColClient.getClientOptions().getBoolean(ClientOptions.AUTO_END_TURN)) {
                endTurn();
            }
        }
    }

    /**
     * Ignore this ModelMessage from now on until it is not generated in a turn.
     * 
     * @param message a <code>ModelMessage</code> value
     * @param flag whether to ignore the ModelMessage or not
     */
    public synchronized void ignoreMessage(ModelMessage message, boolean flag) {
        String key = message.getSourceId();
        if (message.getTemplateType() == StringTemplate.TemplateType.TEMPLATE) {
            for (String otherkey : message.getKeys()) {
                if ("%goods%".equals(otherkey)) {
                    key += otherkey;
                }
                break;
            }
        }
        if (flag) {
            startIgnoringMessage(key, freeColClient.getGame().getTurn().getNumber());
        } else {
            stopIgnoringMessage(key);
        }
    }

    /**
     * Displays the next <code>ModelMessage</code>.
     * 
     * @see net.sf.freecol.common.model.ModelMessage ModelMessage
     */
    public void nextModelMessage() {
        displayModelMessages(false);
    }

    public void displayModelMessages(final boolean allMessages) {

        int thisTurn = freeColClient.getGame().getTurn().getNumber();

        final ArrayList<ModelMessage> messageList = new ArrayList<ModelMessage>();
        List<ModelMessage> inputList;
        if (allMessages) {
            inputList = freeColClient.getMyPlayer().getModelMessages();
        } else {
            inputList = freeColClient.getMyPlayer().getNewModelMessages();
        }

        for (ModelMessage message : inputList) {
            if (shouldAllowMessage(message)) {
                if (message.getMessageType() == ModelMessage.MessageType.WAREHOUSE_CAPACITY) {
                    String key = message.getSourceId();
                    if (message.getTemplateType() == StringTemplate.TemplateType.TEMPLATE) {
                        for (String otherkey : message.getKeys()) {
                            if ("%goods%".equals(otherkey)) {
                                key += otherkey;
                                break;
                            }
                        }
                    }

                    Integer turn = getTurnForMessageIgnored(key);
                    if (turn != null && turn.intValue() == thisTurn - 1) {
                        startIgnoringMessage(key, thisTurn);
                        message.setBeenDisplayed(true);
                        continue;
                    }
                } else if (message.getMessageType() == ModelMessage.MessageType.BUILDING_COMPLETED) {
                    freeColClient.playSound(SoundEffect.BUILDING_COMPLETE);
                } else if (message.getMessageType() == ModelMessage.MessageType.FOREIGN_DIPLOMACY) {
                    if (message.getId().equals("EventPanel.MEETING_AZTEC")) {
                        freeColClient.playMusicOnce("aztec");
                    }
                }
                messageList.add(message);
            }

            // flag all messages delivered as "beenDisplayed".
            message.setBeenDisplayed(true);
        }

        purgeOldMessagesFromMessagesToIgnore(thisTurn);
        final ModelMessage[] messages = messageList.toArray(new ModelMessage[0]);

        Runnable uiTask = new Runnable() {
                public void run() {
                    Canvas canvas = freeColClient.getCanvas();
                    if (messageList.size() > 0) {
                        if (allMessages || messageList.size() > 5) {
                            canvas.showReportTurnPanel(messages);
                        } else {
                            canvas.showModelMessages(messages);
                        }
                    }
                    freeColClient.getActionManager().update();
                }
            };
        if (SwingUtilities.isEventDispatchThread()) {
            uiTask.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(uiTask);
            } catch (InterruptedException e) {
                // Ignore
            } catch (InvocationTargetException e) {
                // Ignore
            }
        }
    }

    private synchronized Integer getTurnForMessageIgnored(String key) {
        return messagesToIgnore.get(key);
    }

    private synchronized void startIgnoringMessage(String key, int turn) {
        logger.finer("Ignoring model message with key " + key);
        messagesToIgnore.put(key, new Integer(turn));
    }

    private synchronized void stopIgnoringMessage(String key) {
        logger.finer("Removing model message with key " + key + " from ignored messages.");
        messagesToIgnore.remove(key);
    }

    private synchronized void purgeOldMessagesFromMessagesToIgnore(int thisTurn) {
        List<String> keysToRemove = new ArrayList<String>();
        for (Entry<String, Integer> entry : messagesToIgnore.entrySet()) {
            if (entry.getValue().intValue() < thisTurn - 1) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Removing old model message with key " + entry.getKey() + " from ignored messages.");
                }
                keysToRemove.add(entry.getKey());
            }
        }
        for (String key : keysToRemove) {
            stopIgnoringMessage(key);
        }
    }

    /**
     * Provides an opportunity to filter the messages delivered to the canvas.
     * 
     * @param message the message that is candidate for delivery to the canvas
     * @return true if the message should be delivered
     */
    private boolean shouldAllowMessage(ModelMessage message) {

        switch (message.getMessageType()) {
        case DEFAULT:
            return true;
        case WARNING:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_WARNING);
        case SONS_OF_LIBERTY:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_SONS_OF_LIBERTY);
        case GOVERNMENT_EFFICIENCY:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_GOVERNMENT_EFFICIENCY);
        case WAREHOUSE_CAPACITY:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_WAREHOUSE_CAPACITY);
        case UNIT_IMPROVED:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_UNIT_IMPROVED);
        case UNIT_DEMOTED:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_UNIT_DEMOTED);
        case UNIT_LOST:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_UNIT_LOST);
        case UNIT_ADDED:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_UNIT_ADDED);
        case BUILDING_COMPLETED:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_BUILDING_COMPLETED);
        case FOREIGN_DIPLOMACY:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_FOREIGN_DIPLOMACY);
        case MARKET_PRICES:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_MARKET_PRICES);
        case MISSING_GOODS:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_MISSING_GOODS);
        case TUTORIAL:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_TUTORIAL);
        default:
            return true;
        }
    }

    /**
     * End the turn.
     */
    public void endTurn() {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        endingTurn = true;
        canAutoEndTurn = false;

        nextActiveUnit(null);
    }

    /**
     * Convenience method: returns the first child element with the specified
     * tagname.
     * 
     * @param element The <code>Element</code> to search for the child
     *            element.
     * @param tagName The tag name of the child element to be found.
     * @return The child of the given <code>Element</code> with the given
     *         <code>tagName</code> or <code>null</code> if no such child
     *         exists.
     */
    protected Element getChildElement(Element element, String tagName) {
        NodeList n = element.getChildNodes();
        for (int i = 0; i < n.getLength(); i++) {
            if (((Element) n.item(i)).getTagName().equals(tagName)) {
                return (Element) n.item(i);
            }
        }

        return null;
    }

    /**
     * Retrieves server statistics
     */
    public StatisticsMessage getServerStatistics() {
        Element request = Message.createNewRootElement(StatisticsMessage.getXMLElementTagName());
        Element reply = freeColClient.getClient().ask(request);
        StatisticsMessage m = new StatisticsMessage(reply);
        return m;
    }

    /**
     * Returns a string representation of the given turn suitable for
     * savegame files.
     * @param turn a <code>Turn</code> value
     * @return A string with the format: "<i>[season] year</i>".
     *         Examples: "1602_1_Spring", "1503"...
     */
    public String getSaveGameString(Turn turn) {
        int year = turn.getYear();
        switch (turn.getSeason()) {
        case SPRING:
            return Integer.toString(year) + "_1_" + Messages.message("spring");
        case AUTUMN:
            return Integer.toString(year) + "_2_" + Messages.message("autumn");
        case YEAR:
        default:
            return Integer.toString(year);
        }
    }


}
