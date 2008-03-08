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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.InGameMenuBar;
import net.sf.freecol.client.gui.action.BuildColonyAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.FreeColActionUI;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.EventPanel;
import net.sf.freecol.client.gui.panel.FreeColDialog.MissionaryAction;
import net.sf.freecol.client.gui.panel.FreeColDialog.ScoutAction;
import net.sf.freecol.client.gui.sound.SfxLibrary;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.CombatModel.CombatResultType;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoalDecider;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.TradeRoute.Stop;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.NetworkConstants;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The controller that will be used while the game is played.
 */
public final class InGameController implements NetworkConstants {
    private static final Logger logger = Logger.getLogger(InGameController.class.getName());




    private final FreeColClient freeColClient;

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
     * Opens a dialog where the user should specify the filename and saves the
     * game.
     */
    public void saveGame() {
        final Canvas canvas = freeColClient.getCanvas();
        String fileName = freeColClient.getMyPlayer().getName() + "_" + freeColClient.getMyPlayer().getNationAsString()
                + "_" + freeColClient.getGame().getTurn().toSaveGameString();
        fileName = fileName.replaceAll(" ", "_");
        if (freeColClient.getMyPlayer().isAdmin() && freeColClient.getFreeColServer() != null) {
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
        final Canvas canvas = freeColClient.getCanvas();

        canvas.showStatusPanel(Messages.message("status.savingGame"));
        Thread t = new Thread() {
            public void run() {
                try {
                    freeColClient.getFreeColServer().saveGame(file, freeColClient.getMyPlayer().getName());
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            canvas.closeStatusPanel();
                            canvas.requestFocusInWindow();
                        }
                    });
                } catch (IOException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            canvas.errorMessage("couldNotSaveGame");
                        }
                    });
                }
            }
        };
        t.start();
    }

    /**
     * Opens a dialog where the user should specify the filename and loads the
     * game.
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

        if (!canvas.showConfirmDialog("stopCurrentGame.text", "stopCurrentGame.yes", "stopCurrentGame.no")) {
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
     * @param debug Should be set to <code>true</code> in order to enable
     *            debug mode.
     */
    public void setInDebugMode(boolean debug) {
        FreeCol.setInDebugMode(debug);
        freeColClient.getCanvas().setJMenuBar(new InGameMenuBar(freeColClient));
        freeColClient.getCanvas().updateJMenuBar();
    }

    /**
     * Declares independence for the home country.
     */
    public void declareIndependence() {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Canvas canvas = freeColClient.getCanvas();
        if (freeColClient.getMyPlayer().getSoL() < 50) {
            canvas.showInformationMessage("declareIndependence.notMajority", new String[][] { { "%percentage%",
                    Integer.toString(freeColClient.getMyPlayer().getSoL()) } });
            return;
        }
        if (!canvas.showConfirmDialog("declareIndependence.areYouSure.text", "declareIndependence.areYouSure.yes",
                "declareIndependence.areYouSure.no")) {
            return;
        }

        Element declareIndependenceElement = Message.createNewRootElement("declareIndependence");
        freeColClient.getMyPlayer().declareIndependence();
        freeColClient.getActionManager().update();
        freeColClient.getClient().sendAndWait(declareIndependenceElement);
        freeColClient.getMyPlayer().setMonarch(null);

        canvas.showDeclarationDialog();
    }

    /**
     * Sends a public chat message.
     * 
     * @param message The chat message.
     */
    public void sendChat(String message) {
        Element chatElement = Message.createNewRootElement("chat");
        chatElement.setAttribute("message", message);
        chatElement.setAttribute("privateChat", "false");
        freeColClient.getClient().sendAndWait(chatElement);
    }

    /**
     * Sets <code>player</code> as the new <code>currentPlayer</code> of the
     * game.
     * 
     * @param currentPlayer The player.
     */
    public void setCurrentPlayer(Player currentPlayer) {
        logger.finest("Setting current player " + currentPlayer.getName());
        Game game = freeColClient.getGame();
        game.setCurrentPlayer(currentPlayer);

        if (freeColClient.getMyPlayer().equals(currentPlayer)) {
            // Autosave the game:
            if (freeColClient.getFreeColServer() != null) {
                final int turnNumber = freeColClient.getGame().getTurn().getNumber();
                final int savegamePeriod = freeColClient.getClientOptions().getInteger(ClientOptions.AUTOSAVE_PERIOD);
                if (savegamePeriod == 1 || (savegamePeriod != 0 && turnNumber % savegamePeriod == 0)) {
                    final String filename = Messages.message("clientOptions.savegames.autosave.fileprefix") + '-'
                            + freeColClient.getGame().getTurn().toSaveGameString() + ".fsg";
                    File saveGameFile = new File(FreeCol.getAutosaveDirectory(), filename);
                    saveGame(saveGameFile);
                    int generations = freeColClient.getClientOptions().getInteger(ClientOptions.AUTOSAVE_GENERATIONS);
                    if (generations > 0) {
                        allSaveGames.add(saveGameFile);
                        if (allSaveGames.size() > generations) {
                            File fileToDelete = allSaveGames.remove(0);
                            fileToDelete.delete();
                        }
                    }
                }
            }

            removeUnitsOutsideLOS();
            if (currentPlayer.checkEmigrate()) {
                if (currentPlayer.hasAbility("model.ability.selectRecruit") &&
                    currentPlayer.getEurope().recruitablesDiffer()) {
                    emigrateUnitInEurope(freeColClient.getCanvas().showEmigrationPanel());
                } else {
                    emigrateUnitInEurope(0);
                }
            }

            if (!freeColClient.isSingleplayer()) {
                freeColClient.playSound(SfxLibrary.ANTHEM_BASE + currentPlayer.getNationType().getIndex());
            }
            
            checkTradeRoutesInEurope();

            displayModelMessages(true);
            nextActiveUnit();
        }
        logger.finest("Exiting method setCurrentPlayer()");
    }

    /**
     * Renames a <code>Renameable</code>.
     * 
     * @param object The object to rename.
     */
    public void rename(Nameable object) {
        if (!(object instanceof Ownable) || ((Ownable) object).getOwner() != freeColClient.getMyPlayer()) {
            return;
        }

        String name = null;
        if (object instanceof Colony) {
            name = freeColClient.getCanvas().showInputDialog("renameColony.text", object.getName(), "renameColony.yes",
                    "renameColony.no");
        } else if (object instanceof Unit) {
            name = freeColClient.getCanvas().showInputDialog("renameUnit.text", object.getName(), "renameUnit.yes",
                    "renameUnit.no");
        }
		
        if (name == null) {
            return;
        } else if (object instanceof Colony && (freeColClient.getMyPlayer().getColony(name) != null)) {
            // colony name must be unique (per Player)
            freeColClient.getCanvas().showInformationMessage("nameColony.notUnique",
                new String[][] { { "%name%", name } });
            return;
        } else {
            object.setName(name);
            Element renameElement = Message.createNewRootElement("rename");
            renameElement.setAttribute("nameable", ((FreeColGameObject) object).getId());
            renameElement.setAttribute("name", name);
            freeColClient.getClient().sendAndWait(renameElement);
        }
    }

    /**
     * Removes the units we cannot see anymore from the map.
     */
    private void removeUnitsOutsideLOS() {
        Player player = freeColClient.getMyPlayer();
        Map map = freeColClient.getGame().getMap();

        player.resetCanSeeTiles();

        Iterator<Position> tileIterator = map.getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile t = map.getTile(tileIterator.next());
            if (t != null && !player.canSee(t) && t.getFirstUnit() != null) {
                if (t.getFirstUnit().getOwner() == player) {
                    logger.warning("Could not see one of my own units!");
                }
                t.disposeAllUnits();
            }
        }

        player.resetCanSeeTiles();
    }

    /**
     * Uses the active unit to build a colony.
     */
    public void buildColony() {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();
        Game game = freeColClient.getGame();
        GUI gui = freeColClient.getGUI();

        Unit unit = freeColClient.getGUI().getActiveUnit();

        if (unit == null || !unit.canBuildColony()) {
            return;
        }

        Tile tile = unit.getTile();

        if (tile == null) {
            return;
        }

        if (freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_COLONY_WARNINGS)) {

            boolean landLocked = true;
            int lumber = 0;
            int food = tile.potential(Goods.FOOD);
            int ore = 0;
            boolean ownedByEuropeans = false;
            boolean ownedBySelf = false;
            boolean ownedByIndians = false;

            if (tile.secondaryGoods() == Goods.ORE) {
                ore = 3;
            }

            Map map = game.getMap();
            Iterator<Position> tileIterator = map.getAdjacentIterator(tile.getPosition());
            while (tileIterator.hasNext()) {
                Tile newTile = map.getTile(tileIterator.next());
                if (newTile.isLand()) {
                    lumber += newTile.potential(Goods.LUMBER);
                    food += newTile.potential(Goods.FOOD);
                    ore += newTile.potential(Goods.ORE);
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

            ArrayList<ModelMessage> messages = new ArrayList<ModelMessage>();
            if (landLocked) {
                messages.add(new ModelMessage(unit, "buildColony.landLocked", null, ModelMessage.MISSING_GOODS,
                                              FreeCol.getSpecification().getGoodsType("model.goods.fish")));
            }
            if (food < 8) {
                messages.add(new ModelMessage(unit, "buildColony.noFood", null, ModelMessage.MISSING_GOODS, 
                                              FreeCol.getSpecification().getGoodsType("model.goods.food")));
            }
            if (lumber < 4) {
                messages.add(new ModelMessage(unit, "buildColony.noLumber", null, ModelMessage.MISSING_GOODS,
                                              FreeCol.getSpecification().getGoodsType("model.goods.lumber")));
            }
            if (ore < 2) {
                messages.add(new ModelMessage(unit, "buildColony.noOre", null, ModelMessage.MISSING_GOODS, 
                                              FreeCol.getSpecification().getGoodsType("model.goods.ore")));
            }
            if (ownedBySelf) {
                messages.add(new ModelMessage(unit, "buildColony.ownLand", null, ModelMessage.WARNING));
            }
            if (ownedByEuropeans) {
                messages.add(new ModelMessage(unit, "buildColony.EuropeanLand", null, ModelMessage.WARNING));
            }
            if (ownedByIndians) {
                messages.add(new ModelMessage(unit, "buildColony.IndianLand", null, ModelMessage.WARNING));
            }

            if (messages.size() > 0) {
                ModelMessage[] modelMessages = messages.toArray(new ModelMessage[0]);
                if (!freeColClient.getCanvas().showConfirmDialog(modelMessages, "buildColony.yes", "buildColony.no")) {
                    return;
                }
            }
        }

        String name = freeColClient.getCanvas().showInputDialog("nameColony.text",
                freeColClient.getMyPlayer().getDefaultColonyName(), "nameColony.yes", "nameColony.no");

        if (name == null) { // The user canceled the action.
            return;
        } else if (freeColClient.getMyPlayer().getColony(name) != null) {
            // colony name must be unique (per Player)
            freeColClient.getCanvas().showInformationMessage("nameColony.notUnique",
                    new String[][] { { "%name%", name } });
            return;
        }

        Element buildColonyElement = Message.createNewRootElement("buildColony");
        buildColonyElement.setAttribute("name", name);
        buildColonyElement.setAttribute("unit", unit.getId());

        Element reply = client.ask(buildColonyElement);

        if (reply.getTagName().equals("buildColonyConfirmed")) {
            if (reply.getElementsByTagName("update").getLength() > 0) {
                Element updateElement = (Element) reply.getElementsByTagName("update").item(0);
                freeColClient.getInGameInputHandler().update(updateElement);
            }

            Colony colony = (Colony) game.getFreeColGameObject(((Element) reply.getChildNodes().item(0))
                    .getAttribute("ID"));
            if (colony == null) {
                colony = new Colony(game, (Element) reply.getChildNodes().item(0));
            } else {
                colony.readFromXMLElement((Element) reply.getChildNodes().item(0));
            }

            changeWorkType(unit, Goods.FOOD);
            unit.buildColony(colony);
            
            for(Unit unitInTile : tile.getUnitList()) {
                if (unitInTile.canCarryTreasure()) {
                    checkCashInTreasureTrain(unitInTile);
                }
            }
            
            gui.setActiveUnit(null);
            gui.setSelectedTile(colony.getTile().getPosition());
        } else {
            // Handle error message.
        }
    }

    /**
     * Moves the active unit in a specified direction. This may result in an
     * attack, move... action.
     * 
     * @param direction The direction in which to move the Unit.
     */
    public void moveActiveUnit(Direction direction) {
        Unit unit = freeColClient.getGUI().getActiveUnit();

        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        if (unit != null) {
            clearGotoOrders(unit);
            move(unit, direction);

            //centers unit if option "always center" is active
            if(freeColClient.getClientOptions().getBoolean(ClientOptions.ALWAYS_CENTER)){
            	Position unitPosition = unit.getTile().getPosition();
            	freeColClient.getGUI().setFocus(unitPosition);
            }
        } // else: nothing: There is no active unit that can be moved.
    }

    /**
     * Selects a destination for this unit. Europe and the player's colonies are
     * valid destinations.
     * 
     * @param unit The unit for which to select a destination.
     */
    public void selectDestination(Unit unit) {
        final Player player = freeColClient.getMyPlayer();
        Map map = freeColClient.getGame().getMap();

        final ArrayList<ChoiceItem> destinations = new ArrayList<ChoiceItem>();
        if (unit.isNaval() && unit.getOwner().canMoveToEurope()) {
            PathNode path = map.findPathToEurope(unit, unit.getTile());
            if (path != null) {
                int turns = path.getTotalTurns();
                destinations.add(new ChoiceItem(player.getEurope() + " (" + turns + ")", player.getEurope()));
            } else if (unit.getTile() != null
                    && (unit.getTile().getType().canSailToEurope() || map.isAdjacentToMapEdge(unit.getTile()))) {
                destinations.add(new ChoiceItem(player.getEurope() + " (0)", player.getEurope()));
            }
        }

        final Settlement inSettlement = (unit.getTile() != null) ? unit.getTile().getSettlement() : null;

        // Search for destinations we can reach:
        map.search(unit, new GoalDecider() {
            public PathNode getGoal() {
                return null;
            }

            public boolean check(Unit u, PathNode p) {
                if (p.getTile().getSettlement() != null && p.getTile().getSettlement().getOwner() == player
                        && p.getTile().getSettlement() != inSettlement) {
                    Settlement s = p.getTile().getSettlement();
                    int turns = p.getTurns();
                    destinations.add(new ChoiceItem(s.toString() + " (" + turns + ")", s));
                }
                return false;
            }

            public boolean hasSubGoals() {
                return false;
            }
        }, Integer.MAX_VALUE);

        Canvas canvas = freeColClient.getCanvas();
        ChoiceItem choice = (ChoiceItem) canvas.showChoiceDialog(Messages.message("selectDestination.text"), Messages
                .message("selectDestination.cancel"), destinations.toArray());
        if (choice == null) {
            // user aborted
            return;
        }

        Location destination = (Location) choice.getObject();

        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            setDestination(unit, destination);
            return;
        }

        if (destination instanceof Europe && unit.getTile() != null
                && (unit.getTile().getType().canSailToEurope() || map.isAdjacentToMapEdge(unit.getTile()))) {
            moveToEurope(unit);
            nextActiveUnit();
        } else {
            setDestination(unit, destination);
            moveToDestination(unit);
        }
    }

    /**
     * Sets the destination of the given unit and send the server a message for this action.
     * 
     * @param unit The <code>Unit</code>.
     * @param destination The <code>Location</code>.
     * @see Unit#setDestination(Location)
     */
    public void setDestination(Unit unit, Location destination) {
        Element setDestinationElement = Message.createNewRootElement("setDestination");
        setDestinationElement.setAttribute("unit", unit.getId());
        if (destination != null) {
            setDestinationElement.setAttribute("destination", destination.getId());
        }

        unit.setDestination(destination);

        freeColClient.getClient().sendAndWait(setDestinationElement);
    }

    /**
     * Moves the given unit towards the destination given by
     * {@link Unit#getDestination()}.
     * 
     * @param unit The unit to move.
     */
    public void moveToDestination(Unit unit) {
        final Canvas canvas = freeColClient.getCanvas();
        final Map map = freeColClient.getGame().getMap();
        final Location destination = unit.getDestination();

        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
        	canvas.showInformationMessage("notYourTurn");
            return;
        }

        // Unit can be waiting to load and continuing with a trade route
        if (destination.getTile() == unit.getTile()) {
            // try to load and follow the trade route
            if (unit.getTradeRoute() != null) {
                followTradeRoute(unit);
            }
            return;
        }

        PathNode path;
        if (destination instanceof Europe) {
            path = map.findPathToEurope(unit, unit.getTile());
        } else {
            if (destination.getTile() == null) {
                // Destination is an abandoned colony, for example
                clearGotoOrders(unit);
                return;
            }
            path = map.findPath(unit, unit.getTile(), destination.getTile());
        }

        if (path == null) {
            canvas.showInformationMessage("selectDestination.failed", new String[][] { { "%destination%",
                    destination.getLocationName() } });
            setDestination(unit, null);
            return;
        }

        boolean knownEnemyOnLastTile = path != null
                && path.getLastNode() != null
                && ((path.getLastNode().getTile().getFirstUnit() != null && path.getLastNode().getTile().getFirstUnit()
                        .getOwner() != freeColClient.getMyPlayer()) || (path.getLastNode().getTile().getSettlement() != null && path
                        .getLastNode().getTile().getSettlement().getOwner() != freeColClient.getMyPlayer()));

        while (path != null) {
            MoveType mt = unit.getMoveType(path.getDirection());
            switch (mt) {
            case MOVE:
                reallyMove(unit, path.getDirection());
                break;
            case EXPLORE_LOST_CITY_RUMOUR:
                exploreLostCityRumour(unit, path.getDirection());
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
                    reallyMove(unit, path.getDirection());
                }
                break;
            case DISEMBARK:
                disembark(unit, path.getDirection());
                path = null;
                break;
            default:
                if (path == path.getLastNode() && mt != MoveType.ILLEGAL_MOVE
                        && (mt != MoveType.ATTACK || knownEnemyOnLastTile)) {
                    move(unit, path.getDirection());
                } else {
                    Tile target = map.getNeighbourOrNull(path.getDirection(), unit.getTile());
                    int moveCost = unit.getMoveCost(target);
                    if (unit.getMovesLeft() == 0
                            || (moveCost > unit.getMovesLeft()
                                    && (target.getFirstUnit() == null || target.getFirstUnit().getOwner() == unit
                                            .getOwner()) && (target.getSettlement() == null || target.getSettlement()
                                    .getOwner() == unit.getOwner()))) {
                        // we can't go there now, but we don't want to wake up
                        unit.setMovesLeft(0);
                        nextActiveUnit();
                        return;
                    } else {
                        // Active unit to show path and permit to move it
                        // manually
                        freeColClient.getGUI().setActiveUnit(unit);
                        return;
                    }
                }
            }
            if (path != null) {
                path = path.next;
            }
        }

        if (unit.getTile() != null && destination instanceof Europe && map.isAdjacentToMapEdge(unit.getTile())) {
            moveToEurope(unit);
        }

        // we have reached our destination
        if (unit.getTradeRoute() != null) {
            if (unit.getCurrentStop().getLocation() != unit.getLocation()) {
                setDestination(unit, unit.getCurrentStop().getLocation());
                moveToDestination(unit);
                return;
            } else {
                followTradeRoute(unit);
            }
        } else {
            setDestination(unit, null);
        }

        // Display a "cash in"-dialog if a treasure train have been
        // moved into a coastal colony:
        if (unit.canCarryTreasure() && checkCashInTreasureTrain(unit)) {
            unit = null;
        }

        if (unit != null && unit.getMovesLeft() > 0 && unit.getTile() != null) {
            freeColClient.getGUI().setActiveUnit(unit);
        } else if (freeColClient.getGUI().getActiveUnit() == unit) {
            nextActiveUnit();
        }
        return;
    }

    private void checkTradeRoutesInEurope() {
        Europe europe = freeColClient.getMyPlayer().getEurope();
        if (europe == null) {
            return;
        }
        List<Unit> units = europe.getUnitList();
        for(Unit unit : units) {
            if (unit.getTradeRoute() != null) {
                followTradeRoute(unit);
            }
        }
    }
    
    private void followTradeRoute(Unit unit) {
        Stop stop = unit.getCurrentStop();
        if (stop == null) {
            return;
        }

        Location location = unit.getLocation();
        // TODO: Not all instances handled?
        if (location instanceof Tile) {
            logger.finer("Stopped in colony " + unit.getColony().getName());
            stopInColony(unit);
        } else if (location instanceof Europe && unit.isInEurope()) {
            logger.finer("Stopped in Europe.");
            stopInEurope(unit);
        }
    }

    private void stopInColony(Unit unit) {

        Stop stop = unit.getCurrentStop();
        Colony colony = unit.getColony();

        GoodsContainer warehouse = colony.getGoodsContainer();
        if (warehouse == null) {
            throw new IllegalStateException("No warehouse in a stop's location");
        }

        // unload cargo that should not be on board and complete loaded goods
        // with less than 100 units
        ArrayList<GoodsType> goodsTypesToLoad = stop.getCargo();
        Iterator<Goods> goodsIterator = unit.getGoodsIterator();
        test: while (goodsIterator.hasNext()) {
            Goods goods = goodsIterator.next();
            for (int index = 0; index < goodsTypesToLoad.size(); index++) {
                GoodsType goodsType = goodsTypesToLoad.get(index);
                if (goods.getType() == goodsType) {
                    if (goods.getAmount() < 100) {
                        // complete goods until 100 units
                        // respect the lower limit for TradeRoute
                        int amountPresent = warehouse.getGoodsCount(goodsType) - 
                            colony.getExportData(goodsType).getExportLevel();
                        if (amountPresent > 0) {
                            logger.finest("Automatically loading goods " + goods.getName());
                            int amountToLoad = Math.min(100 - goods.getAmount(), amountPresent);
                            loadCargo(new Goods(freeColClient.getGame(), colony, goods.getType(), amountToLoad), unit);
                        }
                    }
                    // remove item: other items of the same type
                    // may or may not be present
                    goodsTypesToLoad.remove(index);
                    continue test;
                }
            }
            // this type of goods was not in the cargo list: do not
            // unload more than the warehouse can store
            int capacity = colony.getWarehouseCapacity() - warehouse.getGoodsCount(goods.getType());
            if (capacity < goods.getAmount() &&
                !freeColClient.getCanvas().showConfirmDialog(Messages.message("traderoute.warehouseCapacity",
                                                                              "%unit%", unit.getName(),
                                                                              "%colony%", colony.getName(),
                                                                              "%amount%", String.valueOf(goods.getAmount() - capacity),
                                                                              "%goods%", goods.getName()),
                                                             "yes", "no")) {
                logger.finest("Automatically unloading " + capacity + " " + goods.getName());
                unloadCargo(new Goods(freeColClient.getGame(), unit, goods.getType(), capacity));
            } else {
                logger.finest("Automatically unloading " + goods.getAmount() + " " + goods.getName());
                unloadCargo(goods);
            }
        }

        // load cargo that should be on board
        for (GoodsType goodsType : goodsTypesToLoad) {
            // respect the lower limit for TradeRoute
            int amountPresent = warehouse.getGoodsCount(goodsType) -
                colony.getExportData(goodsType).getExportLevel();
            if (amountPresent > 0) {
                if (unit.getSpaceLeft() > 0) {
                    logger.finest("Automatically loading goods " + goodsType.getName());
                    loadCargo(new Goods(freeColClient.getGame(), colony, goodsType,
                            Math.min(amountPresent, 100)), unit);
                }
            }
        }

        // TODO: do we want to load/unload units as well?
        // if so, when?

        updateCurrentStop(unit);
    }
    
    private void updateCurrentStop(Unit unit) {
        // Set destination to next stop's location
        Element updateCurrentStopElement = Message.createNewRootElement("updateCurrentStop");
        updateCurrentStopElement.setAttribute("unit", unit.getId());
        freeColClient.getClient().sendAndWait(updateCurrentStopElement);
        
        Stop stop = unit.nextStop();
        // go to next stop, unit can already be there waiting to load
        if (stop != null && stop.getLocation() != unit.getColony()) {
            if (unit.isInEurope()) {
                moveToAmerica(unit);
            } else {
                moveToDestination(unit);
            }
        }
    }

    private void stopInEurope(Unit unit) {

        Stop stop = unit.getCurrentStop();

        // unload cargo that should not be on board and complete loaded goods
        // with less than 100 units
        ArrayList<GoodsType> goodsTypesToLoad = stop.getCargo();
        Iterator<Goods> goodsIterator = unit.getGoodsIterator();
        test: while (goodsIterator.hasNext()) {
            Goods goods = goodsIterator.next();
            for (int index = 0; index < goodsTypesToLoad.size(); index++) {
                GoodsType goodsType = goodsTypesToLoad.get(index);
                if (goods.getType() == goodsType) {
                    if (goods.getAmount() < 100) {
                        logger.finest("Automatically loading goods " + goods.getName());
                        buyGoods(goods.getType(), (100 - goods.getAmount()), unit);
                    }
                    // remove item: other items of the same type
                    // may or may not be present
                    goodsTypesToLoad.remove(index);
                    continue test;
                }
            }
            // this type of goods was not in the cargo list
            logger.finest("Automatically unloading " + goods.getName());
            sellGoods(goods);
        }

        // load cargo that should be on board
        for (GoodsType goodsType : goodsTypesToLoad) {
            if (unit.getSpaceLeft() > 0) {
                logger.finest("Automatically loading goods " + goodsType.getName());
                buyGoods(goodsType, 100, unit);
            }
        }

        // TODO: do we want to load/unload units as well?
        // if so, when?

        updateCurrentStop(unit);
    }

    /**
     * Moves the specified unit in a specified direction. This may result in an
     * attack, move... action.
     * 
     * @param unit The unit to be moved.
     * @param direction The direction in which to move the Unit.
     */
    public void move(Unit unit, Direction direction) {

        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        // Be certain the tile we are about to move into has been updated by the
        // server:
        // Can be removed if we use 'client.ask' when moving:
        /*
         * try { while (game.getMap().getNeighbourOrNull(direction,
         * unit.getTile()) != null &&
         * game.getMap().getNeighbourOrNull(direction, unit.getTile()).getType() ==
         * Tile.UNEXPLORED) { Thread.sleep(5); } } catch (InterruptedException
         * ie) {}
         */

        MoveType move = unit.getMoveType(direction);

        switch (move) {
        case MOVE:
            reallyMove(unit, direction);
            break;
        case ATTACK:
            attack(unit, direction);
            break;
        case DISEMBARK:
            disembark(unit, direction);
            break;
        case EMBARK:
            embark(unit, direction);
            break;
        case MOVE_HIGH_SEAS:
            moveHighSeas(unit, direction);
            break;
        case ENTER_INDIAN_VILLAGE_WITH_SCOUT:
            scoutIndianSettlement(unit, direction);
            break;
        case ENTER_INDIAN_VILLAGE_WITH_MISSIONARY:
            useMissionary(unit, direction);
            break;
        case ENTER_INDIAN_VILLAGE_WITH_FREE_COLONIST:
            learnSkillAtIndianSettlement(unit, direction);
            break;
        case ENTER_FOREIGN_COLONY_WITH_SCOUT:
            scoutForeignColony(unit, direction);
            break;
        case ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS:
            //TODO: unify trade and negotiations
            Map map = freeColClient.getGame().getMap();
            Settlement settlement = map.getNeighbourOrNull(direction, unit.getTile()).getSettlement();
            if (settlement instanceof Colony) {
                negotiate(unit, direction);
            } else {
                tradeWithSettlement(unit, direction);
            }
            break;
        case EXPLORE_LOST_CITY_RUMOUR:
            exploreLostCityRumour(unit, direction);
            break;
        case ILLEGAL_MOVE:
            freeColClient.playSound(SfxLibrary.ILLEGAL_MOVE);
            break;
        default:
            throw new RuntimeException("unrecognised move: " + move);
        }

        // Display a "cash in"-dialog if a treasure train have been moved into a
        // colony:
        if (unit.canCarryTreasure()) {
            checkCashInTreasureTrain(unit);
            if (unit.isDisposed()) {
                nextActiveUnit();
            }
        }

        nextModelMessage();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                freeColClient.getActionManager().update();
                freeColClient.getCanvas().updateJMenuBar();
            }
        });
    }


    /**
     * Initiates a negotiation with a foreign power. The player
     * creates a DiplomaticTrade with the NegotiationDialog. The
     * DiplomaticTrade is sent to the other player. If the other
     * player accepts the offer, the trade is concluded. If not, this
     * method returns, since the next offer must come from the other
     * player.
     *
     * @param unit an <code>Unit</code> value
     * @param direction an <code>int</code> value
     */
    private void negotiate(Unit unit, Direction direction) {
        Map map = freeColClient.getGame().getMap();
        Settlement settlement = map.getNeighbourOrNull(direction, unit.getTile()).getSettlement();

        Element spyElement = Message.createNewRootElement("spySettlement");
        spyElement.setAttribute("unit", unit.getId());
        spyElement.setAttribute("direction", String.valueOf(direction));
        Element reply = freeColClient.getClient().ask(spyElement);
        if (reply != null) {
            NodeList childNodes = reply.getChildNodes();
            settlement.readFromXMLElement((Element) childNodes.item(0));
        }

        DiplomaticTrade agreement = freeColClient.getCanvas().showNegotiationDialog(unit, settlement, null);
        if (agreement != null) {
            unit.setMovesLeft(0);
            String nation = agreement.getRecipient().getNationAsString();
            reply = null;
            
            do {
                Element diplomaticElement = Message.createNewRootElement("diplomaticTrade");
                diplomaticElement.setAttribute("unit", unit.getId());
                diplomaticElement.setAttribute("direction", String.valueOf(direction));
                diplomaticElement.appendChild(agreement.toXMLElement(null, diplomaticElement.getOwnerDocument()));
                reply = freeColClient.getClient().ask(diplomaticElement);
                
                if (reply != null) {
                    String accept = reply.getAttribute("accept");
                    if (accept != null && accept.equals("accept")) {
                        freeColClient.getCanvas().showInformationMessage("negotiationDialog.offerAccepted",
                                                                         new String[][] {{"%nation%", nation}});
                        agreement.makeTrade();
                        return;
                    } else {
                        Element childElement = (Element) reply.getChildNodes().item(0);
                        DiplomaticTrade proposal = new DiplomaticTrade(freeColClient.getGame(), childElement);
                        agreement = freeColClient.getCanvas().showNegotiationDialog(unit, settlement, proposal);
                    }
                } else if (agreement.isAccept()) {
                    // We have accepted the contra-proposal
                    agreement.makeTrade();
                    return;
                }
            } while (reply != null);
            freeColClient.getCanvas().showInformationMessage("negotiationDialog.offerRejected",
                                                             new String[][] {{"%nation%", nation}});
        }
    }

    /**
     * Enter in a foreign colony to spy it.
     *
     * @param unit an <code>Unit</code> value
     * @param direction an <code>int</code> value
     */
    private void spy(Unit unit, Direction direction) {
        Game game = freeColClient.getGame();
        Colony colony = game.getMap().getNeighbourOrNull(direction,
                unit.getTile()).getColony();

        Element spyElement = Message.createNewRootElement("spySettlement");
        spyElement.setAttribute("unit", unit.getId());
        spyElement.setAttribute("direction", String.valueOf(direction));
        Element reply = freeColClient.getClient().ask(spyElement);
        if (reply != null) {
            unit.setMovesLeft(0);
            NodeList childNodes = reply.getChildNodes();
            colony.readFromXMLElement((Element) childNodes.item(0));
            Tile tile = colony.getTile();
            for(int i=1; i < childNodes.getLength(); i++) {
                Element unitElement = (Element) childNodes.item(i);
                Unit foreignUnit = (Unit) game.getFreeColGameObject(unitElement.getAttribute("ID"));
                if (foreignUnit == null) {
                    foreignUnit = new Unit(game, unitElement);
                } else {
                    foreignUnit.readFromXMLElement(unitElement);
                }
                tile.add(foreignUnit);
            }
            freeColClient.getCanvas().showColonyPanel(colony);
        }
    }

    /**
     * Ask for explore a lost city rumour, and move unit if player accepts
     * 
     * @param unit The unit to be moved.
     * @param direction The direction in which to move the Unit.
     */
    private void exploreLostCityRumour(Unit unit, Direction direction) {
        if (freeColClient.getCanvas().showConfirmDialog("exploreLostCityRumour.text", "exploreLostCityRumour.yes",
                "exploreLostCityRumour.no")) {
            reallyMove(unit, direction);
        }
    }

    /**
     * Buys the given land from the indians.
     * 
     * @param tile The land which should be bought from the indians.
     */
    public void buyLand(Tile tile) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Element buyLandElement = Message.createNewRootElement("buyLand");
        buyLandElement.setAttribute("tile", tile.getId());

        freeColClient.getMyPlayer().buyLand(tile);

        freeColClient.getClient().sendAndWait(buyLandElement);

        freeColClient.getCanvas().updateGoldLabel();
    }

    /**
     * Uses the given unit to trade with a <code>Settlement</code> in the
     * given direction.
     * 
     * @param unit The <code>Unit</code> that is a carrier containing goods.
     * @param direction The direction the unit could move in order to enter the
     *            <code>Settlement</code>.
     * @exception IllegalArgumentException if the unit is not a carrier, or if
     *                there is no <code>Settlement</code> in the given
     *                direction.
     * @see Settlement
     */
    private void tradeWithSettlement(Unit unit, Direction direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Canvas canvas = freeColClient.getCanvas();
        Map map = freeColClient.getGame().getMap();
        Client client = freeColClient.getClient();

        if (!unit.isCarrier()) {
            throw new IllegalArgumentException("The unit has to be a carrier in order to trade!");
        }

        Settlement settlement = map.getNeighbourOrNull(direction, unit.getTile()).getSettlement();
        if (settlement == null) {
            throw new IllegalArgumentException("No settlement in given direction!");
        }

        if (unit.getGoodsCount() == 0) {
            canvas.errorMessage("noGoodsOnboard");
            return;
        }

        Goods goods = (Goods) canvas.showChoiceDialog(Messages.message("tradeProposition.text"), Messages
                .message("tradeProposition.cancel"), unit.getGoodsIterator());
        if (goods == null) { // == Trade aborted by the player.
            return;
        }

        Element tradePropositionElement = Message.createNewRootElement("tradeProposition");
        tradePropositionElement.setAttribute("unit", unit.getId());
        tradePropositionElement.setAttribute("settlement", settlement.getId());
        tradePropositionElement.appendChild(goods.toXMLElement(null, tradePropositionElement.getOwnerDocument()));

        Element reply = client.ask(tradePropositionElement);
        while (reply != null) {
            if (!reply.getTagName().equals("tradePropositionAnswer")) {
                logger.warning("Illegal reply.");
                throw new IllegalStateException();
            }

            int gold = Integer.parseInt(reply.getAttribute("gold"));
            if (gold == NO_NEED_FOR_THE_GOODS) {
                canvas.showInformationMessage("noNeedForTheGoods", new String[][] { { "%goods%", goods.getName() } });
                return;
            } else if (gold <= NO_TRADE) {
                canvas.showInformationMessage("noTrade");
                return;
            } else {
                ChoiceItem[] objects = { new ChoiceItem(Messages.message("trade.takeOffer"), 1),
                        new ChoiceItem(Messages.message("trade.moreGold"), 2),
                        new ChoiceItem(Messages.message("trade.gift").replaceAll("%goods%", goods.getName()), 0), };

                String text = Messages.message("trade.text").replaceAll("%nation%",
                        settlement.getOwner().getNationAsString());
                text = text.replaceAll("%goods%", goods.getName());
                text = text.replaceAll("%gold%", Integer.toString(gold));
                ChoiceItem ci = (ChoiceItem) canvas.showChoiceDialog(text, Messages.message("trade.cancel"), objects);
                if (ci == null) { // == Trade aborted by the player.
                    return;
                }
                int ret = ci.getChoice();
                if (ret == 1) {
                    tradeWithSettlement(unit, settlement, goods, gold);
                    return;
                } else if (ret == 0) {
                    deliverGiftToSettlement(unit, settlement, goods);
                    return;
                }
            } // Ask for more gold (ret == 2):

            tradePropositionElement = Message.createNewRootElement("tradeProposition");
            tradePropositionElement.setAttribute("unit", unit.getId());
            tradePropositionElement.setAttribute("settlement", settlement.getId());
            tradePropositionElement.appendChild(goods.toXMLElement(null, tradePropositionElement.getOwnerDocument()));
            tradePropositionElement.setAttribute("gold", Integer.toString((gold * 11) / 10));

            reply = client.ask(tradePropositionElement);
        }

        if (reply == null) {
            logger.warning("reply == null");
        }
    }

    /**
     * Trades the given goods. The goods gets transferred from the given
     * <code>Unit</code> to the given <code>Settlement</code>, and the
     * {@link Unit#getOwner unit's owner} collects the payment.
     */
    private void tradeWithSettlement(Unit unit, Settlement settlement, Goods goods, int gold) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();

        Element tradeElement = Message.createNewRootElement("trade");
        tradeElement.setAttribute("unit", unit.getId());
        tradeElement.setAttribute("settlement", settlement.getId());
        tradeElement.setAttribute("gold", Integer.toString(gold));
        tradeElement.appendChild(goods.toXMLElement(null, tradeElement.getOwnerDocument()));

        Element reply = client.ask(tradeElement);

        unit.trade(settlement, goods, gold);

        freeColClient.getCanvas().updateGoldLabel();
        
        if (reply != null) {
            if (!reply.getTagName().equals("sellProposition")) {
                logger.warning("Illegal reply.");
                throw new IllegalStateException();
            }
            
            ArrayList<Goods> goodsOffered = new ArrayList<Goods>();
            NodeList childNodes = reply.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                goodsOffered.add(new Goods(freeColClient.getGame(), (Element) childNodes.item(i)));
            }
            Goods goodsToBuy = (Goods) freeColClient.getCanvas().showChoiceDialog(Messages.message("buyProposition.text"),
                    Messages.message("buyProposition.cancel"), goodsOffered.iterator());
            if (goodsToBuy != null) {
                buyFromSettlement(unit, goodsToBuy);
            }
        }
        
        nextActiveUnit(unit.getTile());
    }

    /**
     * Uses the given unit to try buying the given goods from an
     * <code>IndianSettlement</code>.
     */
    private void buyFromSettlement(Unit unit, Goods goods) {
        Canvas canvas = freeColClient.getCanvas();
        Client client = freeColClient.getClient();
        
        Element buyPropositionElement = Message.createNewRootElement("buyProposition");
        buyPropositionElement.setAttribute("unit", unit.getId());
        buyPropositionElement.appendChild(goods.toXMLElement(null, buyPropositionElement.getOwnerDocument()));

        Element reply = client.ask(buyPropositionElement);
        while (reply != null) {
            if (!reply.getTagName().equals("buyPropositionAnswer")) {
                logger.warning("Illegal reply.");
                throw new IllegalStateException();
            }

            int gold = Integer.parseInt(reply.getAttribute("gold"));
            if (gold <= NO_TRADE) {
                canvas.showInformationMessage("noTrade");
                return;
            } else {
                ChoiceItem[] objects = { new ChoiceItem(Messages.message("buy.takeOffer"), 1),
                        new ChoiceItem(Messages.message("buy.moreGold"), 2), };

                IndianSettlement settlement = (IndianSettlement) goods.getLocation();
                String text = Messages.message("buy.text").replaceAll("%nation%",
                        settlement.getOwner().getNationAsString());
                text = text.replaceAll("%goods%", goods.getName());
                text = text.replaceAll("%gold%", Integer.toString(gold));
                ChoiceItem ci = (ChoiceItem) canvas.showChoiceDialog(text, Messages.message("buy.cancel"), objects);
                if (ci == null) { // == Trade aborted by the player.
                    return;
                }
                int ret = ci.getChoice();
                if (ret == 1) {
                    buyFromSettlement(unit, goods, gold);
                    return;
                }
            } // Ask for more gold (ret == 2):

            buyPropositionElement = Message.createNewRootElement("buyProposition");
            buyPropositionElement.setAttribute("unit", unit.getId());
            buyPropositionElement.appendChild(goods.toXMLElement(null, buyPropositionElement.getOwnerDocument()));
            buyPropositionElement.setAttribute("gold", Integer.toString((gold * 9) / 10));

            reply = client.ask(buyPropositionElement);
        }
    }

    /**
     * Trades the given goods. The goods gets transferred from their location
     * to the given <code>Unit</code>, and the {@link Unit#getOwner unit's owner}
     * pays the given gold.
     */
    private void buyFromSettlement(Unit unit, Goods goods, int gold) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();

        Element buyElement = Message.createNewRootElement("buy");
        buyElement.setAttribute("unit", unit.getId());
        buyElement.setAttribute("gold", Integer.toString(gold));
        buyElement.appendChild(goods.toXMLElement(null, buyElement.getOwnerDocument()));

        client.ask(buyElement);

        IndianSettlement settlement = (IndianSettlement) goods.getLocation();
        // add goods to settlement in order to client will be able to transfer the goods
        settlement.add(goods);
        unit.buy(settlement, goods, gold);

        freeColClient.getCanvas().updateGoldLabel();
    }

    /**
     * Trades the given goods. The goods gets transferred from the given
     * <code>Unit</code> to the given <code>Settlement</code>.
     */
    private void deliverGiftToSettlement(Unit unit, Settlement settlement, Goods goods) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();

        Element deliverGiftElement = Message.createNewRootElement("deliverGift");
        deliverGiftElement.setAttribute("unit", unit.getId());
        deliverGiftElement.setAttribute("settlement", settlement.getId());
        deliverGiftElement.appendChild(goods.toXMLElement(null, deliverGiftElement.getOwnerDocument()));

        client.sendAndWait(deliverGiftElement);

        unit.deliverGift(settlement, goods);
        nextActiveUnit(unit.getTile());
    }

    /**
     * Transfers the gold carried by this unit to the {@link Player owner}.
     * 
     * @exception IllegalStateException if this unit is not a treasure train. or
     *                if it cannot be cashed in at it's current location.
     */
    private boolean checkCashInTreasureTrain(Unit unit) {
        Canvas canvas = freeColClient.getCanvas();
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            canvas.showInformationMessage("notYourTurn");
            return false;
        }

        Client client = freeColClient.getClient();

        if (unit.canCashInTreasureTrain()) {
            boolean cash;
            if (unit.getOwner().getEurope() == null) {
                canvas.showInformationMessage("cashInTreasureTrain.text.independence",
                        new String[][] { { "%nation%", unit.getOwner().getNationAsString() } });
                cash = true;
            } else {
                int transportFee = unit.getTransportFee();
                String message = (transportFee == 0) ? 
                    "cashInTreasureTrain.text.free" :
                    "cashInTreasureTrain.text.pay";
                cash = canvas.showConfirmDialog(message, "cashInTreasureTrain.yes", "cashInTreasureTrain.no");
            }
            if (cash) {
                // Inform the server:
                Element cashInTreasureTrainElement = Message.createNewRootElement("cashInTreasureTrain");
                cashInTreasureTrainElement.setAttribute("unit", unit.getId());

                client.sendAndWait(cashInTreasureTrainElement);

                unit.cashInTreasureTrain();

                freeColClient.getCanvas().updateGoldLabel();
                return true;
            }
        }
        return false;
    }

    /**
     * Actually moves a unit in a specified direction.
     * 
     * @param unit The unit to be moved.
     * @param direction The direction in which to move the Unit.
     */
    private void reallyMove(Unit unit, Direction direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Canvas canvas = freeColClient.getCanvas();
        Client client = freeColClient.getClient();

        // Inform the server:
        Element moveElement = Message.createNewRootElement("move");
        moveElement.setAttribute("unit", unit.getId());
        moveElement.setAttribute("direction", direction.toString());

        // TODO: server can actually fail (illegal move)!

        // move before ask to server, to be in new tile in case there is a
        // rumours
        unit.move(direction);

        // client.send(moveElement);
        Element reply = client.ask(moveElement);
        freeColClient.getInGameInputHandler().handle(client.getConnection(), reply);
        if (reply.hasAttribute("movesSlowed")) {
            // ship slowed
            unit.setMovesLeft(unit.getMovesLeft() - Integer.parseInt(reply.getAttribute("movesSlowed")));
            Unit slowedBy = (Unit) freeColClient.getGame().getFreeColGameObject(reply.getAttribute("slowedBy"));
            canvas.showInformationMessage("model.unit.slowed", new String[][] {{"%unit%", unit.getName()}, 
                {"%enemyUnit%", slowedBy.getName()}, {"%enemyNation%", slowedBy.getOwner().getNationAsString()}});
        }

        // set location again in order to meet with people player don't see
        // before move
        if (!unit.isDisposed()) {
            unit.setLocation(unit.getTile());
        }

        if (unit.getTile().isLand() && !unit.getOwner().isNewLandNamed()) {
            String newLandName = canvas.showInputDialog("newLand.text", unit.getOwner().getNewLandName(),
                    "newLand.yes", null);
            unit.getOwner().setNewLandName(newLandName);
            Element setNewLandNameElement = Message.createNewRootElement("setNewLandName");
            setNewLandNameElement.setAttribute("newLandName", newLandName);
            client.sendAndWait(setNewLandNameElement);
            canvas.showEventDialog(EventPanel.FIRST_LANDING);
            
            final Player player = freeColClient.getMyPlayer();
            final BuildColonyAction bca = (BuildColonyAction) freeColClient.getActionManager().getFreeColAction(BuildColonyAction.id);
            final KeyStroke keyStroke = bca.getAccelerator();
            final String[][] data = new String[][] {
                {"%build_colony_key%", FreeColActionUI.getHumanKeyStrokeText(keyStroke)},
                {"%build_colony_menu_item%", Messages.message("unit.state.7")},
                {"%orders_menu_item%", Messages.message("menuBar.orders")}
            };
            player.addModelMessage(new ModelMessage(player, "tutorial.buildColony", data, ModelMessage.TUTORIAL, player));
            nextModelMessage();
        }

        if (unit.getTile().getSettlement() != null && unit.isCarrier() && unit.getTradeRoute() == null
                && (unit.getDestination() == null || unit.getDestination().getTile() == unit.getTile())) {
            canvas.showColonyPanel((Colony) unit.getTile().getSettlement());
        } else if (unit.getMovesLeft() <= 0 || unit.isDisposed()) {
        	nextActiveUnit(unit.getTile()); 
        } 

        nextModelMessage();
    }

    /**
     * Ask for attack or demand a tribute when attacking an indian settlement,
     * attack in other cases
     * 
     * @param unit The unit to perform the attack.
     * @param direction The direction in which to attack.
     */
    private void attack(Unit unit, Direction direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Tile target = freeColClient.getGame().getMap().getNeighbourOrNull(direction, unit.getTile());

        if (target.getSettlement() != null && target.getSettlement() instanceof IndianSettlement && unit.isArmed()) {
            IndianSettlement settlement = (IndianSettlement) target.getSettlement();
            switch (freeColClient.getCanvas().showArmedUnitIndianSettlementDialog(settlement)) {
            case INDIAN_SETTLEMENT_ATTACK:
                if (confirmHostileAction(unit, target) && confirmPreCombat(unit, target)) {
                    reallyAttack(unit, direction);
                }
                return;
            case CANCEL:
                return;
            case INDIAN_SETTLEMENT_TRIBUTE:
                Element demandMessage = Message.createNewRootElement("armedUnitDemandTribute");
                demandMessage.setAttribute("unit", unit.getId());
                demandMessage.setAttribute("direction", direction.toString());
                Element reply = freeColClient.getClient().ask(demandMessage);
                if (reply != null && reply.getTagName().equals("armedUnitDemandTributeResult")) {
                    String result = reply.getAttribute("result");
                    if (result.equals("agree")) {
                        String amount = reply.getAttribute("amount");
                        unit.getOwner().modifyGold(Integer.parseInt(amount));
                        freeColClient.getCanvas().updateGoldLabel();
                        freeColClient.getCanvas().showInformationMessage("scoutSettlement.tributeAgree",
                                new String[][] { { "%replace%", amount } });
                    } else if (result.equals("disagree")) {
                        freeColClient.getCanvas().showInformationMessage("scoutSettlement.tributeDisagree");
                    }
                    unit.setMovesLeft(0);
                } else {
                    logger.warning("Server gave an invalid reply to an armedUnitDemandTribute message");
                    return;
                }
                nextActiveUnit(unit.getTile());
                break;
            default:
                logger.warning("Incorrect response returned from Canvas.showArmedUnitIndianSettlementDialog()");
                return;
            }
        } else {
            if (confirmHostileAction(unit, target) && confirmPreCombat(unit, target)) {
                reallyAttack(unit, direction);
            }
            return;
        }
    }

    /**
     * Check if an attack results in a transition from peace or cease fire to
     * war and, if so, warn the player.
     * 
     * @param attacker The potential attacker.
     * @param target The target tile.
     * @return true to attack, false to abort.
     */
    private boolean confirmHostileAction(Unit attacker, Tile target) {
        if (!attacker.hasAbility("model.ability.piracy")) {
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
            switch (attacker.getOwner().getStance(enemy)) {
            case CEASE_FIRE:
                return freeColClient.getCanvas().showConfirmDialog("model.diplomacy.attack.ceaseFire",
                        "model.diplomacy.attack.confirm", "cancel",
                        new String[][] { { "%replace%", enemy.getNationAsString() } });
            case PEACE:
                return freeColClient.getCanvas().showConfirmDialog("model.diplomacy.attack.peace",
                        "model.diplomacy.attack.confirm", "cancel",
                        new String[][] { { "%replace%", enemy.getNationAsString() } });
            case ALLIANCE:
                freeColClient.playSound(SfxLibrary.ILLEGAL_MOVE);
                freeColClient.getCanvas().showInformationMessage("model.diplomacy.attack.alliance",
                        new String[][] { { "%replace%", enemy.getNationAsString() } });
                return false;
            case WAR:
                logger.finest("Player at war, no confirmation needed");
                return true;
            default:
                logger.warning("Unknown stance " + attacker.getOwner().getStance(enemy));
                return true;
            }
        } else {
            // Privateers can attack and remain at peace
            return true;
        }
    }

    /**
     * If the client options include a pre-combat dialog, allow the user to view
     * the odds and possibly cancel the attack.
     * 
     * @param attacker The attacker.
     * @param target The target tile.
     * @return true to attack, false to abort.
     */
    private boolean confirmPreCombat(Unit attacker, Tile target) {
        if (freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_PRECOMBAT)) {
            Settlement settlementOrNull = target.getSettlement();
            // Don't tell the player how a settlement is defended!
            Unit defenderOrNull = settlementOrNull != null ? null : target.getDefendingUnit(attacker);
            return freeColClient.getCanvas().showPreCombatDialog(attacker, defenderOrNull, settlementOrNull);
        }
        return true;
    }

    /**
     * Performs an attack in a specified direction. Note that the server handles
     * the attack calculations here.
     * 
     * @param unit The unit to perform the attack.
     * @param direction The direction in which to attack.
     */
    private void reallyAttack(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        Game game = freeColClient.getGame();
        Tile target = game.getMap().getNeighbourOrNull(direction, unit.getTile());

        if (unit.hasAbility("model.ability.bombard") || unit.isNaval()) {
            freeColClient.playSound(SfxLibrary.ARTILLERY);
        }

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
                GoodsType type = FreeCol.getSpecification().getGoodsType(Integer.parseInt(goods.getAttribute("type")));
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

            if (!unit.isNaval()) {
                Unit winner;
                if (result == CombatResultType.WIN || result == CombatResultType.GREAT_WIN) {
                    winner = unit;
                } else {
                    winner = defender;
                }
                if (winner.isArmed()) {
                    if (winner.isMounted()) {
                        if (winner.getOwner().isIndian()) {
                            freeColClient.playSound(SfxLibrary.MUSKETSHORSES);
                        } else {
                            freeColClient.playSound(SfxLibrary.DRAGOON);
                        }
                    } else {
                        freeColClient.playSound(SfxLibrary.ATTACK);
                    }
                } else if (winner.isMounted()) {
                    freeColClient.playSound(SfxLibrary.DRAGOON);
                }
            } else {
                if (result == CombatResultType.GREAT_WIN || result == CombatResultType.GREAT_LOSS) {
                    freeColClient.playSound(SfxLibrary.SUNK);
                }
            }
            try {
                game.getCombatModel().attack(unit, defender, new CombatResult(result, damage), plunderGold);
            } catch (Exception e) {
                // Ignore the exception (the update further down will fix any
                // problems).
                LogRecord lr = new LogRecord(Level.WARNING, "Exception in reallyAttack");
                lr.setThrown(e);
                logger.log(lr);
            }

            // Get the convert:
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
                
                String nation = defender.getOwner().getNationAsString();
                ModelMessage message = new ModelMessage(convert,
                                                "model.unit.newConvertFromAttack",
                                                new String[][] {{"%nation%", nation}},
                                                ModelMessage.UNIT_ADDED);
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
            if (unit.getMovesLeft() <= 0) {
                nextActiveUnit(unit.getTile());
            }

            freeColClient.getCanvas().refresh();
        } else {
            logger.log(Level.SEVERE, "Server returned null from reallyAttack!");
        }
    }

    /**
     * Disembarks the specified unit in a specified direction.
     * 
     * @param unit The unit to be disembarked.
     * @param direction The direction in which to disembark the Unit.
     */
    private void disembark(Unit unit, Direction direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        // Make sure it is a carrier.
        if (!unit.isCarrier()) {
            throw new RuntimeException("Programming error: disembark called on non carrier.");
        }
        // Check if user wants to disembark.
        Canvas canvas = freeColClient.getCanvas();
        if (!canvas.showConfirmDialog("disembark.text", "disembark.yes", "disembark.no")) {
            return;
        }

        Game game = freeColClient.getGame();
        Tile destinationTile = game.getMap().getNeighbourOrNull(direction, unit.getTile());

        unit.setStateToAllChildren(UnitState.ACTIVE);

        // Disembark only the first unit.
        Unit toDisembark = unit.getFirstUnit();
        if (toDisembark.getMovesLeft() > 0) {
            if (destinationTile.hasLostCityRumour()) {
                exploreLostCityRumour(toDisembark, direction);
            } else {
                reallyMove(toDisembark, direction);
            }
        }
    }

    /**
     * Embarks the specified unit in a specified direction.
     * 
     * @param unit The unit to be embarked.
     * @param direction The direction in which to embark the Unit.
     */
    private void embark(Unit unit, Direction direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Game game = freeColClient.getGame();
        Client client = freeColClient.getClient();
        GUI gui = freeColClient.getGUI();
        Canvas canvas = freeColClient.getCanvas();

        Tile destinationTile = game.getMap().getNeighbourOrNull(direction, unit.getTile());
        Unit destinationUnit = null;

        if (destinationTile.getUnitCount() == 1) {
            destinationUnit = destinationTile.getFirstUnit();
        } else {
            ArrayList<Unit> choices = new ArrayList<Unit>();
            for (Unit nextUnit : destinationTile.getUnitList()) {
                if (nextUnit.getSpaceLeft() >= unit.getType().getSpaceTaken()) {
                    choices.add(nextUnit);
                }
            }

            if (choices.size() == 1) {
                destinationUnit = choices.get(0);
            } else if (choices.size() == 0) {
                throw new IllegalStateException();
            } else {
                destinationUnit = (Unit) canvas.showChoiceDialog(Messages.message("embark.text"), Messages
                        .message("embark.cancel"), choices.iterator());
                if (destinationUnit == null) { // == user cancelled
                    return;
                }
            }
        }

        unit.embark(destinationUnit);

        if (destinationUnit.getMovesLeft() > 0) {
            gui.setActiveUnit(destinationUnit);
        } else {
            nextActiveUnit(destinationUnit.getTile());
        }

        Element embarkElement = Message.createNewRootElement("embark");
        embarkElement.setAttribute("unit", unit.getId());
        embarkElement.setAttribute("direction", direction.toString());
        embarkElement.setAttribute("embarkOnto", destinationUnit.getId());

        client.sendAndWait(embarkElement);
    }

    /**
     * Boards a specified unit onto a carrier. The carrier should be at the same
     * tile as the boarding unit.
     * 
     * @param unit The unit who is going to board the carrier.
     * @param carrier The carrier.
     * @return <i>true</i> if the <code>unit</code> actually gets on the
     *         <code>carrier</code>.
     */
    public boolean boardShip(Unit unit, Unit carrier) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            throw new IllegalStateException("Not your turn.");
        }

        if (unit == null) {
            logger.warning("unit == null");
            return false;
        }

        if (carrier == null) {
            logger.warning("Trying to load onto a non-existent carrier.");
            return false;
        }

        Client client = freeColClient.getClient();

        if (unit.isCarrier()) {
            logger.warning("Trying to load a carrier onto another carrier.");
            return false;
        }

        freeColClient.playSound(SfxLibrary.LOAD_CARGO);

        Element boardShipElement = Message.createNewRootElement("boardShip");
        boardShipElement.setAttribute("unit", unit.getId());
        boardShipElement.setAttribute("carrier", carrier.getId());

        unit.boardShip(carrier);

        client.sendAndWait(boardShipElement);

        return true;
    }

    /**
     * Clear the speciality of a <code>Unit</code>. That is, makes it a
     * <code>Unit.FREE_COLONIST</code>.
     * 
     * @param unit The <code>Unit</code> to clear the speciality of.
     */
    public void clearSpeciality(Unit unit) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();

        Element clearSpecialityElement = Message.createNewRootElement("clearSpeciality");
        clearSpecialityElement.setAttribute("unit", unit.getId());

        unit.clearSpeciality();

        client.sendAndWait(clearSpecialityElement);
    }

    /**
     * Leave a ship. This method should only be invoked if the ship is in a
     * harbour.
     * 
     * @param unit The unit who is going to leave the ship where it is located.
     */
    public void leaveShip(Unit unit) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();

        unit.leaveShip();

        Element leaveShipElement = Message.createNewRootElement("leaveShip");
        leaveShipElement.setAttribute("unit", unit.getId());

        client.sendAndWait(leaveShipElement);
    }

    /**
     * Loads a cargo onto a carrier.
     * 
     * @param goods The goods which are going aboard the carrier.
     * @param carrier The carrier.
     */
    public void loadCargo(Goods goods, Unit carrier) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        if (carrier == null) {
            throw new NullPointerException();
        }

        freeColClient.playSound(SfxLibrary.LOAD_CARGO);

        Client client = freeColClient.getClient();
        goods.adjustAmount();

        Element loadCargoElement = Message.createNewRootElement("loadCargo");
        loadCargoElement.setAttribute("carrier", carrier.getId());
        loadCargoElement.appendChild(goods.toXMLElement(freeColClient.getMyPlayer(), loadCargoElement
                .getOwnerDocument()));

        goods.loadOnto(carrier);

        client.sendAndWait(loadCargoElement);
    }

    /**
     * Unload cargo. If the unit carrying the cargo is not in a
     * harbour, the goods will be dumped.
     * 
     * @param goods The goods which are going to leave the ship where it is
     *            located.
     */
    public void unloadCargo(Goods goods) {
        unloadCargo(goods, false);
    }

    /**
     * Unload cargo. If the unit carrying the cargo is not in a
     * harbour, or if the given boolean is true, the goods will be
     * dumped.
     * 
     * @param goods The goods which are going to leave the ship where it is
     *            located.
     * @param dump a <code>boolean</code> value
     */
    public void unloadCargo(Goods goods, boolean dump) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();

        goods.adjustAmount();

        Element unloadCargoElement = Message.createNewRootElement("unloadCargo");
        unloadCargoElement.appendChild(goods.toXMLElement(freeColClient.getMyPlayer(), unloadCargoElement
                .getOwnerDocument()));

        if (!dump && goods.getLocation() instanceof Unit &&
            ((Unit) goods.getLocation()).getColony() != null) {
            goods.unload();
        } else {
            goods.setLocation(null);
        }

        client.sendAndWait(unloadCargoElement);
    }

    /**
     * Buys goods in Europe. The amount of goods is adjusted if there is lack of
     * space in the <code>carrier</code>.
     * 
     * @param type The type of goods to buy.
     * @param amount The amount of goods to buy.
     * @param carrier The carrier.
     */
    public void buyGoods(GoodsType type, int amount, Unit carrier) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();
        Player myPlayer = freeColClient.getMyPlayer();
        Canvas canvas = freeColClient.getCanvas();

        if (carrier == null) {
            throw new NullPointerException();
        }

        if (carrier.getOwner() != myPlayer
                || (carrier.getSpaceLeft() <= 0 && (carrier.getGoodsContainer().getGoodsCount(type) % 100 == 0))) {
            return;
        }

        if (carrier.getSpaceLeft() <= 0) {
            amount = Math.min(amount, 100 - carrier.getGoodsContainer().getGoodsCount(type) % 100);
        }

        if (myPlayer.getMarket().getBidPrice(type, amount) > myPlayer.getGold()) {
            canvas.errorMessage("notEnoughGold");
            return;
        }

        freeColClient.playSound(SfxLibrary.LOAD_CARGO);

        Element buyGoodsElement = Message.createNewRootElement("buyGoods");
        buyGoodsElement.setAttribute("carrier", carrier.getId());
        buyGoodsElement.setAttribute("type", Integer.toString(type.getIndex()));
        buyGoodsElement.setAttribute("amount", Integer.toString(amount));

        carrier.buyGoods(type, amount);
        freeColClient.getCanvas().updateGoldLabel();

        client.sendAndWait(buyGoodsElement);
    }

    /**
     * Sells goods in Europe.
     * 
     * @param goods The goods to be sold.
     */
    public void sellGoods(Goods goods) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();
        Player player = freeColClient.getMyPlayer();

        freeColClient.playSound(SfxLibrary.SELL_CARGO);

        goods.adjustAmount();

        Element sellGoodsElement = Message.createNewRootElement("sellGoods");
        sellGoodsElement.appendChild(goods.toXMLElement(freeColClient.getMyPlayer(), sellGoodsElement
                .getOwnerDocument()));

        player.getMarket().sell(goods, player);
        freeColClient.getCanvas().updateGoldLabel();

        client.sendAndWait(sellGoodsElement);
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
        if (unit.getLocation() instanceof Unit) {
            carrier = (Unit) unit.getLocation();
            leaveShip(unit);
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
            for (int count = 0; count < amount; count++) {
                unit.equipWith(type);
            }
        } else {
            for (int count = 0; count > amount; count--) {
                unit.removeEquipment(type);
            }
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

        Client client = freeColClient.getClient();

        Element workElement = Message.createNewRootElement("work");
        workElement.setAttribute("unit", unit.getId());
        workElement.setAttribute("workLocation", workLocation.getId());

        unit.work(workLocation);

        client.sendAndWait(workElement);
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
        }

        Element putOutsideColonyElement = Message.createNewRootElement("putOutsideColony");
        putOutsideColonyElement.setAttribute("unit", unit.getId());
        unit.putOutsideColony();

        Client client = freeColClient.getClient();
        client.sendAndWait(putOutsideColonyElement);

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
        changeWorkTypeElement.setAttribute("workType", Integer.toString(workType.getIndex()));

        unit.setWorkType(workType);

        client.sendAndWait(changeWorkTypeElement);
    }

    /**
     * Changes the work type of this <code>Unit</code>.
     * 
     * @param unit The <code>Unit</code>
     * @param improvementType a <code>TileImprovementType</code> value
     */
    public void changeWorkType(Unit unit, TileImprovementType improvementType) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Element changeWorkTypeElement = Message.createNewRootElement("workImprovement");
        changeWorkTypeElement.setAttribute("unit", unit.getId());
        changeWorkTypeElement.setAttribute("improvementType", improvementType.getId());

        Element reply = freeColClient.getClient().ask(changeWorkTypeElement);
        Element improvementElement = (Element) reply.getChildNodes().item(0);
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
     * @param type The new type of building to build.
     */
    public void setCurrentlyBuilding(Colony colony, BuildableType type) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();

        colony.setCurrentlyBuilding(type);

        Element setCurrentlyBuildingElement = Message.createNewRootElement("setCurrentlyBuilding");
        setCurrentlyBuildingElement.setAttribute("colony", colony.getId());
        setCurrentlyBuildingElement.setAttribute("type", type.getId());

        client.sendAndWait(setCurrentlyBuildingElement);
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

        if (state == UnitState.IMPROVING) {
            int price = unit.getOwner().getLandPrice(unit.getTile());
            if (price > 0) {
                Player nation = unit.getTile().getOwner();
                ChoiceItem[] choices = {
                    new ChoiceItem(Messages.message("indianLand.pay" ,"%amount%",
                                                    Integer.toString(price)), 1),
                    new ChoiceItem(Messages.message("indianLand.take"), 2) };
                ChoiceItem ci = (ChoiceItem) canvas.showChoiceDialog(Messages.message("indianLand.text",
                                                                                      "%player%", nation.getName()),
                                                                     Messages.message("indianLand.cancel"), choices);
                if (ci == null) {
                    return;
                } else if (ci.getChoice() == 1) {
                    if (price > freeColClient.getMyPlayer().getGold()) {
                        canvas.errorMessage("notEnoughGold");
                        return;
                    }

                    buyLand(unit.getTile());
                }
            }
        } else if (state == UnitState.FORTIFYING && unit.isOffensiveUnit() &&
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
     * by making the unit {@link Unit#UnitState#ACTIVE} and setting a null destination
     * 
     * @param unit The <code>Unit</code>.
     */
    public void clearOrders(Unit unit) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        if (unit == null) {
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
     * cleared by making the unit {@link Unit#UnitState#ACTIVE}.
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
     * Moves the specified unit in the "high seas" in a specified direction.
     * This may result in an ordinary move, no move or a move to europe.
     * 
     * @param unit The unit to be moved.
     * @param direction The direction in which to move the Unit.
     */
    private void moveHighSeas(Unit unit, Direction direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Canvas canvas = freeColClient.getCanvas();
        Map map = freeColClient.getGame().getMap();

        // getTile() == null : Unit in europe

        if (!unit.isAlreadyOnHighSea()
                && (unit.getTile() == null || canvas.showConfirmDialog("highseas.text", "highseas.yes", "highseas.no"))) {
            moveToEurope(unit);
            nextActiveUnit();
        } else if (map.getNeighbourOrNull(direction, unit.getTile()) != null) {
            reallyMove(unit, direction);
        }
    }

    /**
     * Moves the specified free colonist into an Indian settlement to learn a
     * skill. Of course, the colonist won't physically get into the village, it
     * will just stay where it is and gain the skill.
     * 
     * @param unit The unit to learn the skill.
     * @param direction The direction in which the Indian settlement lies.
     */
    private void learnSkillAtIndianSettlement(Unit unit, Direction direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        Map map = freeColClient.getGame().getMap();
        IndianSettlement settlement = (IndianSettlement) map.getNeighbourOrNull(direction, unit.getTile())
                .getSettlement();

        if (settlement.getLearnableSkill() != null || !settlement.hasBeenVisited()) {
            unit.setMovesLeft(0);
            String skillName;

            Element askSkill = Message.createNewRootElement("askSkill");
            askSkill.setAttribute("unit", unit.getId());
            askSkill.setAttribute("direction", direction.toString());

            Element reply = client.ask(askSkill);
            UnitType skill = null;

            if (reply.getTagName().equals("provideSkill")) {
                String skillStr = reply.getAttribute("skill");
                if (skillStr == null) {
                    skillName = null;
                } else {
                    skill = FreeCol.getSpecification().getUnitType(Integer.parseInt(skillStr));
                    skillName = skill.getName();
                }
            } else {
                logger.warning("Server gave an invalid reply to an askSkill message");
                return;
            }

            settlement.setLearnableSkill(skill);
            settlement.setVisited();

            if (skillName == null) {
                canvas.errorMessage("indianSettlement.noMoreSkill");
            } else if (!unit.getType().canBeUpgraded(skill, UnitType.UpgradeType.NATIVES)) {
                canvas.showInformationMessage("indianSettlement.cantLearnSkill",
                        new String[][] { {"%unit%", unit.getName()}, {"%skill%", skillName} });
            } else {
                Element learnSkill = Message.createNewRootElement("learnSkillAtSettlement");
                learnSkill.setAttribute("unit", unit.getId());
                learnSkill.setAttribute("direction", direction.toString());

                if (!canvas.showConfirmDialog("learnSkill.text", "learnSkill.yes", "learnSkill.no", new String[][] { {
                        "%replace%", skillName } })) {
                    // the player declined to learn the skill
                    learnSkill.setAttribute("action", "cancel");
                }

                Element reply2 = freeColClient.getClient().ask(learnSkill);
                String result = reply2.getAttribute("result");
                if (result.equals("die")) {
                    unit.dispose();
                    canvas.showInformationMessage("learnSkill.die");
                } else if (result.equals("leave")) {
                    canvas.showInformationMessage("learnSkill.leave");
                } else if (result.equals("success")) {
                    unit.setType(skill);
                    if (!settlement.isCapital())
                        settlement.setLearnableSkill(null);
                } else if (result.equals("cancelled")) {
                    // do nothing
                } else {
                    logger.warning("Server gave an invalid reply to an learnSkillAtSettlement message");
                }
            }
        } else if (unit.getDestination() != null) {
            setDestination(unit, null);
        }

        nextActiveUnit(unit.getTile());
    }
   /**
     * Ask for spy the foreign colony, negotiate with the foreign power
     * or attack the colony
     * 
     * @param unit The unit that will spy, negotiate or attack.
     * @param direction The direction in which the foreign colony lies.
     */
    private void scoutForeignColony(Unit unit, Direction direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Canvas canvas = freeColClient.getCanvas();
        Map map = freeColClient.getGame().getMap();
        Tile tile = map.getNeighbourOrNull(direction, unit.getTile());
        Colony colony = tile.getColony();

        ScoutAction userAction = canvas.showScoutForeignColonyDialog(colony, unit);

        switch (userAction) {
        case CANCEL:
            break;
        case FOREIGN_COLONY_ATTACK:
            attack(unit, direction);
            break;
        case FOREIGN_COLONY_NEGOTIATE:
             negotiate(unit, direction);
            break;
        case FOREIGN_COLONY_SPY:
            spy(unit, direction);
            break;
        default:
            logger.warning("Incorrect response returned from Canvas.showScoutForeignColonyDialog()");
            return;
        }
    }

   /**
     * Moves the specified scout into an Indian settlement to speak with the
     * chief or demand a tribute etc. Of course, the scout won't physically get
     * into the village, it will just stay where it is.
     * 
     * @param unit The unit that will speak, attack or ask tribute.
     * @param direction The direction in which the Indian settlement lies.
     */
    private void scoutIndianSettlement(Unit unit, Direction direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        Map map = freeColClient.getGame().getMap();
        Tile tile = map.getNeighbourOrNull(direction, unit.getTile());
        IndianSettlement settlement = (IndianSettlement) tile.getSettlement();

        // The scout loses his moves because the skill data and tradeable goods
        // data is fetched
        // from the server and the moves are the price we have to pay to obtain
        // that data.
        unit.setMovesLeft(0);

        Element scoutMessage = Message.createNewRootElement("scoutIndianSettlement");
        scoutMessage.setAttribute("unit", unit.getId());
        scoutMessage.setAttribute("direction", direction.toString());
        scoutMessage.setAttribute("action", "basic");
        Element reply = client.ask(scoutMessage);

        if (reply.getTagName().equals("scoutIndianSettlementResult")) {
            UnitType skill = null;
            String skillStr = reply.getAttribute("skill");
            if (skillStr != null) {
                skill = FreeCol.getSpecification().getUnitType(skillStr);
            }
            settlement.setLearnableSkill(skill);
            settlement.setWantedGoods(0, FreeCol.getSpecification().getGoodsType(reply.getAttribute("highlyWantedGoods")));
            settlement.setWantedGoods(1, FreeCol.getSpecification().getGoodsType(reply.getAttribute("wantedGoods1")));
            settlement.setWantedGoods(2, FreeCol.getSpecification().getGoodsType(reply.getAttribute("wantedGoods2")));
            settlement.setVisited();
            settlement.getOwner().setNumberOfSettlements(Integer.parseInt(reply.getAttribute("numberOfCamps")));
            freeColClient.getInGameInputHandler().update(reply);
        } else {
            logger.warning("Server gave an invalid reply to an askSkill message");
            return;
        }

        ScoutAction userAction = canvas.showScoutIndianSettlementDialog(settlement);

        switch (userAction) {
        case INDIAN_SETTLEMENT_ATTACK:
            scoutMessage.setAttribute("action", "attack");
            // The movesLeft has been set to 0 when the scout initiated its
            // action.If it wants to attack then it can and it will need some
            // moves to do it.
            unit.setMovesLeft(1);
            client.sendAndWait(scoutMessage);
            // TODO: Check if this dialog is needed, one has just been displayed
            if (confirmPreCombat(unit, tile)) {
                reallyAttack(unit, direction);
            }
            return;
        case CANCEL:
            scoutMessage.setAttribute("action", "cancel");
            client.sendAndWait(scoutMessage);
            return;
        case INDIAN_SETTLEMENT_SPEAK:
            scoutMessage.setAttribute("action", "speak");
            reply = client.ask(scoutMessage);
            break;
        case INDIAN_SETTLEMENT_TRIBUTE:
            scoutMessage.setAttribute("action", "tribute");
            reply = client.ask(scoutMessage);
            break;
        default:
            logger.warning("Incorrect response returned from Canvas.showScoutIndianSettlementDialog()");
            return;
        }

        if (reply.getTagName().equals("scoutIndianSettlementResult")) {
            String result = reply.getAttribute("result"), action = scoutMessage.getAttribute("action");
            if (result.equals("die")) {
                unit.dispose();
                canvas.showInformationMessage("scoutSettlement.speakDie");
            } else if (action.equals("speak") && result.equals("tales")) {
                // Parse the tiles.
                Element updateElement = getChildElement(reply, "update");
                if (updateElement != null) {
                    freeColClient.getInGameInputHandler().handle(client.getConnection(), updateElement);
                }

                canvas.showInformationMessage("scoutSettlement.speakTales");
            } else if (action.equals("speak") && result.equals("beads")) {
                String amount = reply.getAttribute("amount");
                unit.getOwner().modifyGold(Integer.parseInt(amount));
                freeColClient.getCanvas().updateGoldLabel();
                canvas.showInformationMessage("scoutSettlement.speakBeads", new String[][] { { "%replace%", amount } });
            } else if (action.equals("speak") && result.equals("nothing")) {
                canvas.showInformationMessage("scoutSettlement.speakNothing");
            } else if (action.equals("tribute") && result.equals("agree")) {
                String amount = reply.getAttribute("amount");
                unit.getOwner().modifyGold(Integer.parseInt(amount));
                freeColClient.getCanvas().updateGoldLabel();
                canvas.showInformationMessage("scoutSettlement.tributeAgree",
                        new String[][] { { "%replace%", amount } });
            } else if (action.equals("tribute") && result.equals("disagree")) {
                canvas.showInformationMessage("scoutSettlement.tributeDisagree");
            }
        } else {
            logger.warning("Server gave an invalid reply to an scoutIndianSettlement message");
            return;
        }

        nextActiveUnit(unit.getTile());
    }

    /**
     * Moves a missionary into an indian settlement.
     * 
     * @param unit The unit that will enter the settlement.
     * @param direction The direction in which the Indian settlement lies.
     */
    private void useMissionary(Unit unit, Direction direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        Map map = freeColClient.getGame().getMap();
        IndianSettlement settlement = (IndianSettlement) map.getNeighbourOrNull(direction, unit.getTile())
                .getSettlement();

        List<Object> response = canvas.showUseMissionaryDialog(settlement);
        MissionaryAction action = (MissionaryAction) response.get(0);

        Element missionaryMessage = Message.createNewRootElement("missionaryAtSettlement");
        missionaryMessage.setAttribute("unit", unit.getId());
        missionaryMessage.setAttribute("direction", direction.toString());

        Element reply = null;

        unit.setMovesLeft(0);

        switch (action) {
        case CANCEL:
            missionaryMessage.setAttribute("action", "cancel");
            client.sendAndWait(missionaryMessage);
            break;
        case ESTABLISH_MISSION:
            missionaryMessage.setAttribute("action", "establish");
            settlement.setMissionary(unit);
            client.sendAndWait(missionaryMessage);
            nextActiveUnit(); // At this point: unit.getTile() == null
            return;
        case DENOUNCE_HERESY:
            missionaryMessage.setAttribute("action", "heresy");
            reply = client.ask(missionaryMessage);

            if (!reply.getTagName().equals("missionaryReply")) {
                logger.warning("Server gave an invalid reply to a missionaryAtSettlement message");
                return;
            }

            String success = reply.getAttribute("success");
            if (success.equals("true")) {
                settlement.setMissionary(unit);
                nextActiveUnit(); // At this point: unit.getTile() == null
            } else {
                unit.dispose();
                nextActiveUnit(); // At this point: unit == null
            }
            return;
        case INCITE_INDIANS:
            missionaryMessage.setAttribute("action", "incite");
            missionaryMessage.setAttribute("incite", ((Player) response.get(1)).getId());

            reply = client.ask(missionaryMessage);

            if (reply.getTagName().equals("missionaryReply")) {
                int amount = Integer.parseInt(reply.getAttribute("amount"));

                boolean confirmed = canvas.showInciteDialog((Player) response.get(1), amount);
                if (confirmed && unit.getOwner().getGold() < amount) {
                    canvas.showInformationMessage("notEnoughGold");
                    confirmed = false;
                }
                
                Element inciteMessage = Message.createNewRootElement("inciteAtSettlement");
                inciteMessage.setAttribute("unit", unit.getId());
                inciteMessage.setAttribute("direction", direction.toString());
                inciteMessage.setAttribute("confirmed", confirmed ? "true" : "false");
                inciteMessage.setAttribute("enemy", ((Player) response.get(1)).getId());

                if (confirmed) {
                    unit.getOwner().modifyGold(-amount);

                    // Maybe at this point we can keep track of the fact that
                    // the indian is now at
                    // war with the chosen european player, but is this really
                    // necessary at the client
                    // side?
                    settlement.getOwner().setStance((Player) response.get(1), Stance.WAR);
                    ((Player) response.get(1)).setStance(settlement.getOwner(), Stance.WAR);
                }

                client.sendAndWait(inciteMessage);
            } else {
                logger.warning("Server gave an invalid reply to a missionaryAtSettlement message");
                return;
            }
        }

        nextActiveUnit(unit.getTile());
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
                List<Unit> unitsInEurope = unit.getLocation().getUnitList();
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
            Element unitElement = (Element) reply.getChildNodes().item(0);
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

        freeColClient.getCanvas().updateGoldLabel();
    }

    /**
     * Buys the remaining hammers and tools for the {@link Building} currently
     * being built in the given <code>Colony</code>.
     * 
     * @param colony The {@link Colony} where the building should be bought.
     */
    public void payForBuilding(Colony colony) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        if (!freeColClient.getCanvas()
                .showConfirmDialog("payForBuilding.text", "payForBuilding.yes", "payForBuilding.no",
                        new String[][] { { "%replace%", Integer.toString(colony.getPriceForBuilding()) } })) {
            return;
        }

        if (colony.getPriceForBuilding() > freeColClient.getMyPlayer().getGold()) {
            freeColClient.getCanvas().errorMessage("notEnoughGold");
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
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
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
            Element unitElement = (Element) reply.getChildNodes().item(0);
            Unit unit = (Unit) game.getFreeColGameObject(unitElement.getAttribute("ID"));
            if (unit == null) {
                unit = new Unit(game, unitElement);
            } else {
                unit.readFromXMLElement(unitElement);
            }
            int unitIndex = Integer.parseInt(reply.getAttribute("newRecruitable"));
            UnitType unitType = FreeCol.getSpecification().getUnitType(unitIndex);
            europe.recruit(slot, unit, unitType);
        } else {
            logger.warning("Could not recruit the specified unit in europe.");
            return;
        }

        freeColClient.getCanvas().updateGoldLabel();
    }

    /**
     * Cause a unit to emigrate from a specified "slot" in Europe. If the player
     * doesn't have William Brewster in the congress then the value of the slot
     * parameter is not important (it won't be used).
     * 
     * @param slot The slot from which the unit emigrates. Either 1, 2 or 3 if
     *            William Brewster is in the congress.
     */
    private void emigrateUnitInEurope(int slot) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();
        Game game = freeColClient.getGame();
        Player myPlayer = freeColClient.getMyPlayer();
        Europe europe = myPlayer.getEurope();

        Element emigrateUnitInEuropeElement = Message.createNewRootElement("emigrateUnitInEurope");
        if (myPlayer.hasAbility("model.ability.selectRecruit")) {
            emigrateUnitInEuropeElement.setAttribute("slot", Integer.toString(slot));
        }

        Element reply = client.ask(emigrateUnitInEuropeElement);

        if (reply == null || !reply.getTagName().equals("emigrateUnitInEuropeConfirmed")) {
            logger.warning("Could not recruit unit: " + myPlayer.getCrosses() + "/" + myPlayer.getCrossesRequired());
            throw new IllegalStateException();
        }

        if (!myPlayer.hasAbility("model.ability.selectRecruit")) {
            slot = Integer.parseInt(reply.getAttribute("slot"));
        }

        Element unitElement = (Element) reply.getChildNodes().item(0);
        Unit unit = (Unit) game.getFreeColGameObject(unitElement.getAttribute("ID"));
        if (unit == null) {
            unit = new Unit(game, unitElement);
        } else {
            unit.readFromXMLElement(unitElement);
        }
        int unitIndex = Integer.parseInt(reply.getAttribute("newRecruitable"));
        UnitType newRecruitable = FreeCol.getSpecification().getUnitType(unitIndex);
        europe.emigrate(slot, unit, newRecruitable);

        freeColClient.getCanvas().updateGoldLabel();
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
        assignTradeRoute(unit,
                freeColClient.getCanvas().showTradeRouteDialog(unit.getTradeRoute()));
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
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        Client client = freeColClient.getClient();
        Player player = freeColClient.getMyPlayer();

        int arrears = player.getArrears(type);
        if (player.getGold() >= arrears) {
            if (freeColClient.getCanvas().showConfirmDialog("model.europe.payArrears", "ok", "cancel",
                    new String[][] { { "%replace%", String.valueOf(arrears) } })) {
                player.modifyGold(-arrears);
                freeColClient.getCanvas().updateGoldLabel();
                player.resetArrears(type);
                // send to server
                Element payArrearsElement = Message.createNewRootElement("payArrears");
                payArrearsElement.setAttribute("goodsType", type.getId());
                client.sendAndWait(payArrearsElement);
            }
        } else {
            freeColClient.getCanvas().showInformationMessage("model.europe.cantPayArrears",
                    new String[][] { { "%amount%", String.valueOf(arrears) } });
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
     * Gathers information about the REF.
     */
    public Element getREFUnits() {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return null;
        }

        Element reply = freeColClient.getClient().ask(Message.createNewRootElement("getREFUnits"));
        return reply;
    }

    /**
     * Disbands the active unit.
     */
    public void disbandActiveUnit() {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        GUI gui = freeColClient.getGUI();
        Unit unit = gui.getActiveUnit();
        Client client = freeColClient.getClient();

        if (unit == null) {
            return;
        }

        if (!freeColClient.getCanvas().showConfirmDialog("disbandUnit.text", "disbandUnit.yes", "disbandUnit.no")) {
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
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }

        GUI gui = freeColClient.getGUI();

        if (gui.getActiveUnit() != null && gui.getActiveUnit().getTile() != null) {
            gui.setFocus(gui.getActiveUnit().getTile().getPosition());
        }
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
        String key = message.getSource().getId();
        for (String[] replacement : message.getData()) {
            if (replacement[0].equals("%goods%")) {
                key += replacement[1];
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
                if (message.getType() == ModelMessage.WAREHOUSE_CAPACITY) {
                    String key = message.getSource().getId();
                    for (String[] replacement : message.getData()) {
                        if (replacement[0].equals("%goods%")) {
                            key += replacement[1];
                            break;
                        }
                    }

                    Integer turn = getTurnForMessageIgnored(key);
                    if (turn != null && turn.intValue() == thisTurn - 1) {
                        startIgnoringMessage(key, thisTurn);
                        message.setBeenDisplayed(true);
                        continue;
                    }
                }
                messageList.add(message);
            }

            // flag all messages delivered as "beenDisplayed".
            message.setBeenDisplayed(true);
        }

        purgeOldMessagesFromMessagesToIgnore(thisTurn);

        Runnable uiTask = new Runnable() {
            public void run() {
                if (messageList.size() > 0) {
                    if (allMessages || messageList.size() > 5) {
                        freeColClient.getCanvas().showTurnReport(messageList);
                    } else {
                        freeColClient.getCanvas().showModelMessages(
                                messageList.toArray(new ModelMessage[0]));
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

        switch (message.getType()) {
        case ModelMessage.DEFAULT:
            return true;
        case ModelMessage.WARNING:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_WARNING);
        case ModelMessage.SONS_OF_LIBERTY:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_SONS_OF_LIBERTY);
        case ModelMessage.GOVERNMENT_EFFICIENCY:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_GOVERNMENT_EFFICIENCY);
        case ModelMessage.WAREHOUSE_CAPACITY:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_WAREHOUSE_CAPACITY);
        case ModelMessage.UNIT_IMPROVED:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_UNIT_IMPROVED);
        case ModelMessage.UNIT_DEMOTED:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_UNIT_DEMOTED);
        case ModelMessage.UNIT_LOST:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_UNIT_LOST);
        case ModelMessage.UNIT_ADDED:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_UNIT_ADDED);
        case ModelMessage.BUILDING_COMPLETED:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_BUILDING_COMPLETED);
        case ModelMessage.FOREIGN_DIPLOMACY:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_FOREIGN_DIPLOMACY);
        case ModelMessage.MARKET_PRICES:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_MARKET_PRICES);
        case ModelMessage.MISSING_GOODS:
            return freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_MISSING_GOODS);
        case ModelMessage.TUTORIAL:
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
     * Abandon a colony with no units
     *
     * @param colony The colony to be abandoned
     */
    public void abandonColony(Colony colony) {
        if (colony == null) {
            return;
        }

        Client client = freeColClient.getClient();

        Element abandonColony = Message.createNewRootElement("abandonColony");
        abandonColony.setAttribute("colony", colony.getId());

        colony.dispose();
        client.sendAndWait(abandonColony);
    }
}
