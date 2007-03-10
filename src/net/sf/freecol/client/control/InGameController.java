
package net.sf.freecol.client.control;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.FreeColMenuBar;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.EventPanel;
import net.sf.freecol.client.gui.panel.FreeColDialog;
import net.sf.freecol.client.gui.sound.SfxLibrary;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoalDecider;
import net.sf.freecol.common.model.Goods;
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
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TradeRoute.Stop;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.NetworkConstants;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
* The controller that will be used while the game is played.
*/
public final class InGameController implements NetworkConstants {
    private static final Logger logger = Logger.getLogger(InGameController.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final FreeColClient freeColClient;


    /**
     * Sets that the turn will be ended when all going-to units have been moved.
     */
    private boolean endingTurn = false;

    /**
     * Sets that all going-to orders should be executed.
     */
    private boolean executeGoto = false;

    /**
     * A hash map of messages to be ignored.
     */
    private HashMap<String, Integer> messagesToIgnore = new HashMap<String, Integer>();


    /**
    * The constructor to use.
    * @param freeColClient The main controller.
     */
    public InGameController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
    }


    /**
    * Opens a dialog where the user should specify the filename
    * and saves the game.
    */
    public void saveGame() {
        final Canvas canvas = freeColClient.getCanvas();
        String fileName = freeColClient.getMyPlayer().getName() + "_" + 
                          freeColClient.getMyPlayer().getNationAsString() + "_" +
                          freeColClient.getGame().getTurn();
        fileName = fileName.replaceAll(" ", "_");
        if (freeColClient.getMyPlayer().isAdmin() && freeColClient.getFreeColServer() != null) {
            final File file = canvas.showSaveDialog(FreeCol.getSaveDirectory(), fileName);
            if (file != null) {
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
                                freeColClient.getFreeColServer().saveGame(file, freeColClient.getMyPlayer().getUsername());
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
    * Opens a dialog where the user should specify the filename
    * and loads the game.
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
    * @param debug Should be set to <code>true</code> in order
    *       to enable debug mode.
    */
    public void setInDebugMode(boolean debug) {
        FreeCol.setInDebugMode(debug);
        freeColClient.getCanvas().setJMenuBar(new FreeColMenuBar(freeColClient));
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
            canvas.showInformationMessage("declareIndependence.notMajority", new String[][] {{"%percentage%", Integer.toString(freeColClient.getMyPlayer().getSoL())}});
            return;
        }
        if (!canvas.showConfirmDialog("declareIndependence.areYouSure.text", "declareIndependence.areYouSure.yes", "declareIndependence.areYouSure.no")) {
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
    * @param message The chat message.
    */
    public void sendChat(String message) {
        Element chatElement = Message.createNewRootElement("chat");
        chatElement.setAttribute("message", message);
        chatElement.setAttribute("privateChat", "false");
        freeColClient.getClient().sendAndWait(chatElement);
    }


    /**
    * Sets <code>player</code> as the new <code>currentPlayer</code> of the game.
    * @param currentPlayer The player.
    */
    public void setCurrentPlayer(Player currentPlayer) {
        logger.finest("Setting current player " + currentPlayer.getName());
        Game game = freeColClient.getGame();
        game.setCurrentPlayer(currentPlayer);

        if (freeColClient.getMyPlayer().equals(currentPlayer)) {           
            // Autosave the game:
            final int turnNumber = freeColClient.getGame().getTurn().getNumber();
            final int savegamePeriod = freeColClient.getClientOptions().getInteger(ClientOptions.AUTOSAVE_PERIOD);        
            if (savegamePeriod == 1 || 
                        (savegamePeriod != 0 && turnNumber % savegamePeriod == 0)) {
                final String turn = freeColClient.getGame().getTurn().toString().replaceAll(" ", "");
                final String filename = Messages.message("clientOptions.savegames.autosave.fileprefix") + '-' + turn + ".fsg";
                saveGame(new File(FreeCol.getAutosaveDirectory(), filename));
            }
            
            removeUnitsOutsideLOS();
            freeColClient.getCanvas().closeMenus();
            freeColClient.getCanvas().setEnabled(true);
            if (currentPlayer.checkEmigrate()) {
                emigrateUnitInEurope(
                        currentPlayer.hasFather(FoundingFather.WILLIAM_BREWSTER)
                        ? freeColClient.getCanvas().showEmigrationPanel() : 0
                    );
            }

            freeColClient.getGUI().setActiveUnit(null);
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
        if (!(object instanceof Ownable) ||
            ((Ownable) object).getOwner() != freeColClient.getMyPlayer()) {
            return;
        }

        String name = null;
        if (object instanceof Colony) {
            name = freeColClient.getCanvas().showInputDialog("renameColony.text", 
                                                             object.getName(),
                                                             "renameColony.yes",
                                                             "renameColony.no");
        } else if (object instanceof Unit) {
            name = freeColClient.getCanvas().showInputDialog("renameUnit.text", 
                                                             object.getName(),
                                                             "renameUnit.yes",
                                                             "renameUnit.no");
        }
        if (name != null) {
            object.setName(name);
            Element renameElement = Message.createNewRootElement("rename");
            renameElement.setAttribute("nameable", ((FreeColGameObject) object).getID());
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

        Iterator tileIterator = map.getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile t = map.getTile((Map.Position) tileIterator.next());
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
            Iterator tileIterator = map.getAdjacentIterator(tile.getPosition());
            while (tileIterator.hasNext()) {
                Tile newTile = map.getTile((Position) tileIterator.next());
                if (newTile.isLand()) {
                    lumber += newTile.potential(Goods.LUMBER);
                    food += newTile.potential(Goods.FOOD);
                    ore += newTile.potential(Goods.ORE);
                    int tileOwner = newTile.getNationOwner();
                    if (tileOwner == unit.getNation()) {
                        ownedBySelf = true;
                    } else if (Player.isEuropean(tileOwner)) {
                        ownedByEuropeans = true;
                    } else if (tileOwner != Player.NO_NATION) {
                        ownedByIndians = true;
                    }
                } else {
                    landLocked = false;
                }
            }

            ArrayList messages = new ArrayList();
            if (landLocked) {
                messages.add(new ModelMessage(unit, "buildColony.landLocked", null, 
                                              ModelMessage.MISSING_GOODS, new Goods(Goods.FISH)));
                }
            if (food < 8) {
                messages.add(new ModelMessage(unit, "buildColony.noFood", null,
                                              ModelMessage.MISSING_GOODS, new Goods(Goods.FOOD)));
            }
            if (lumber < 4) {
                messages.add(new ModelMessage(unit, "buildColony.noLumber", null,
                                              ModelMessage.MISSING_GOODS, new Goods(Goods.LUMBER)));
            }
            if (ore < 2) {
                messages.add(new ModelMessage(unit, "buildColony.noOre", null,
                                              ModelMessage.MISSING_GOODS, new Goods(Goods.ORE)));
            }
            if (ownedBySelf) {
                messages.add(new ModelMessage(unit, "buildColony.ownLand", null,
                                              ModelMessage.WARNING));
            }
            if (ownedByEuropeans) {
                messages.add(new ModelMessage(unit, "buildColony.EuropeanLand", null,
                                              ModelMessage.WARNING));
            }
            if (ownedByIndians) {
                messages.add(new ModelMessage(unit, "buildColony.IndianLand", null,
                                              ModelMessage.WARNING));
            }

            if (messages.size() > 0) {
                ModelMessage[] modelMessages = new ModelMessage[messages.size()];
                for (int i = 0; i < messages.size(); i++) {
                    modelMessages[i] = (ModelMessage) messages.get(i);
                }
                if (!freeColClient.getCanvas().showConfirmDialog(modelMessages, "buildColony.yes", "buildColony.no")) {
                    return;
                }
            }
        }

        String name = freeColClient.getCanvas().showInputDialog("nameColony.text", 
                freeColClient.getMyPlayer().getDefaultColonyName(), "nameColony.yes", "nameColony.no");

        if (name == null) { // The user cancelled the action.
            return;
        } else if (freeColClient.getMyPlayer().getColony(name) != null) {
            // colony name must be unique (per Player)
            freeColClient.getCanvas().showInformationMessage("nameColony.notUnique",
                                                             new String[][] {{"%name%", name}});
            return;
        }

        Element buildColonyElement = Message.createNewRootElement("buildColony");
        buildColonyElement.setAttribute("name", name);
        buildColonyElement.setAttribute("unit", unit.getID());

        Element reply = client.ask(buildColonyElement);

        if (reply.getTagName().equals("buildColonyConfirmed")) {
            if (reply.getElementsByTagName("update").getLength() > 0) {
                Element updateElement = (Element) reply.getElementsByTagName("update").item(0);
                freeColClient.getInGameInputHandler().update(updateElement);
            }

            Colony colony = (Colony) game.getFreeColGameObject(((Element) reply.getChildNodes().item(0)).getAttribute("ID"));
            if (colony == null) {
                colony = new Colony(game, (Element) reply.getChildNodes().item(0));
            } else {
                colony.readFromXMLElement((Element) reply.getChildNodes().item(0));
            }
            
            changeWorkType(unit, Goods.FOOD);
            unit.buildColony(colony);
            gui.setActiveUnit(null);
            gui.setSelectedTile(colony.getTile().getPosition());
        } else {
            // Handle errormessage.
        }
    }


    /**
     * Moves the active unit in a specified direction. This may result in an attack, move... action.
     * @param direction The direction in which to move the Unit.
     */
    public void moveActiveUnit(int direction) {
        Unit unit = freeColClient.getGUI().getActiveUnit();

        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        if (unit != null) {
            move(unit, direction);
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
        
        final ArrayList destinations = new ArrayList();        
        if (unit.isNaval() && unit.getOwner().canMoveToEurope()) {
            PathNode path = map.findPathToEurope(unit, unit.getTile());
            if (path != null) {
                int turns = path.getTotalTurns();
                destinations.add(new ChoiceItem(player.getEurope() + " (" + turns + ")", player.getEurope()));
            } else if (unit.getTile() != null 
                    && (unit.getTile().getType() == Tile.HIGH_SEAS
                            || map.isAdjacentToMapEdge(unit.getTile()))) {
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
               if (p.getTile().getSettlement() != null
                       && p.getTile().getSettlement().getOwner() == player
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
        ChoiceItem choice = (ChoiceItem) canvas.showChoiceDialog(Messages.message("selectDestination.text"),
                                                     Messages.message("selectDestination.cancel"),
                                                     destinations.toArray());
        if (choice == null) {
            // user aborted
            return;
        }
        
        Location destination = (Location) choice.getObject();
        
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            setDestination(unit, destination);
            return;
        }
        
        if (destination instanceof Europe
                && unit.getTile() != null
                && (unit.getTile().getType() == Tile.HIGH_SEAS
                        || map.isAdjacentToMapEdge(unit.getTile()))) {            
            moveToEurope(unit);
            nextActiveUnit();
        } else {
            setDestination(unit, destination);
            moveToDestination(unit);
        }
    }

    /**
     * Sets the destination of the given unit
     * 
     * @param unit The <code>Unit</code>.
     * @param destination The <code>Location</code>.
     * @see Unit#setDestination(Location)
     */
    public void setDestination(Unit unit, Location destination) {
        Element setDestinationElement = Message.createNewRootElement("setDestination");        
        setDestinationElement.setAttribute("unit", unit.getID());
        if (destination != null) {
            setDestinationElement.setAttribute("destination", destination.getID());    
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
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        /* Fix a bug I don't know what caused it. Sometimes a unit have a goto order
         * but it's already in destination
         */
        if (destination.getTile() == unit.getTile()) {
            clearOrders(unit);
            return;
        }
        
        PathNode path;
        if (destination instanceof Europe) {
            path = map.findPathToEurope(unit, unit.getTile());
        } else {
            path = map.findPath(unit, unit.getTile(), destination.getTile());
        }
        
        if (path == null) {
            canvas.showInformationMessage("selectDestination.failed",
                    new String [][] {{"%destination%",
                                      destination.getLocationName()}});
            setDestination(unit, null);
            return;
        }   
        
        boolean knownEnemyOnLastTile = path != null
                && path.getLastNode() != null 
                && ((path.getLastNode().getTile().getFirstUnit() != null
                     && path.getLastNode().getTile().getFirstUnit().getOwner() != freeColClient.getMyPlayer())
                   || (path.getLastNode().getTile().getSettlement() != null
                       && path.getLastNode().getTile().getSettlement().getOwner() != freeColClient.getMyPlayer()));
        
        while (path != null) {
            int mt = unit.getMoveType(path.getDirection());
            switch (mt) {
            case Unit.MOVE:                    
                reallyMove(unit, path.getDirection());
                break;
            case Unit.EXPLORE_LOST_CITY_RUMOUR:
                exploreLostCityRumour(unit, path.getDirection());
                if (unit.isDisposed())
                    return;
                break;
            case Unit.MOVE_HIGH_SEAS:
                if (destination instanceof Europe) {
                    moveToEurope(unit);
                    path = null;
                } else if (path == path.getLastNode()){
                    move(unit, path.getDirection());
                    path = null;
                } else {
                    reallyMove(unit, path.getDirection());    
                }
                break;
            case Unit.DISEMBARK:
                disembark(unit, path.getDirection());
                path = null;
                break;
            default:
                if (path == path.getLastNode() 
                    && mt != Unit.ILLEGAL_MOVE
                    && (mt != Unit.ATTACK || knownEnemyOnLastTile)) {
                    move(unit, path.getDirection());
                } else {
                    Tile target = map.getNeighbourOrNull(path.getDirection(), unit.getTile());
                    int moveCost = unit.getMoveCost(target);                
                    if (unit.getMovesLeft() == 0 || (moveCost > unit.getMovesLeft() &&
                            (target.getFirstUnit() == null ||
                                target.getFirstUnit().getOwner() == unit.getOwner()) &&
                            (target.getSettlement() == null ||
                                target.getSettlement().getOwner() == unit.getOwner()))) {
                        // we can't go there now, but we don't want to wake up        
                        unit.setMovesLeft(0);
                        nextActiveUnit();
                        return;
                    } else {           
                        // Active unit to show path and permit to move it manually
                        freeColClient.getGUI().setActiveUnit(unit);
                        return;
                    }
                }
            }            
            if (path != null) {
                path = path.next;
            }
        }
        
        if (unit.getTile() != null
                && destination instanceof Europe
                && map.isAdjacentToMapEdge(unit.getTile())) {
            moveToEurope(unit);
        }

        // we have reached our destination
        if (unit.getTradeRoute() != null) {
            Stop stop = unit.getTradeRoute().nextStop();
            if (stop != null) {
                setDestination(unit, stop.getLocation());
            }
            ArrayList<Integer> goodsTypes = (ArrayList<Integer>) stop.getCargo().clone();
            Iterator goodsIterator = unit.getGoodsIterator();
            test: while (goodsIterator.hasNext()) {
                Goods goods = (Goods) goodsIterator.next();
                for (int index = 0; index < goodsTypes.size(); index++) {
                    if (goods.getType() == goodsTypes.get(index).intValue()) {
                        // remove item: other items of the same type
                        // may or may not be present
                        goodsTypes.remove(index);
                        continue test;
                    }
                }
                // this type of goods was not in the cargo list
                unloadCargo(goods);
            }
            // TODO: do we want to load/unload units as well? 
            // if so, when?
        } else {
            setDestination(unit, null);
        }
        
        // Display a "cash in"-dialog if a treasure train have been moved into a colony:  
        if (unit.getType() == Unit.TREASURE_TRAIN
                && unit.getLocation() != null
                && unit.getLocation() instanceof Tile
                && unit.getLocation().getTile().getColony() != null) {
            String message = (unit.getOwner().hasFather(FoundingFather.HERNAN_CORTES)) ? "cashInTreasureTrain.text.free" : "cashInTreasureTrain.text.pay";
            if (canvas.showConfirmDialog(message, "cashInTreasureTrain.yes", "cashInTreasureTrain.no")) {
                cashInTreasureTrain(unit);
                unit = null;
            }
        }
        
        if (unit != null
                && unit.getMovesLeft() > 0
                && unit.getTile() != null) {
            freeColClient.getGUI().setActiveUnit(unit);
        } else if (freeColClient.getGUI().getActiveUnit() == unit) {
            nextActiveUnit();
        }
    }


    /**
     * Moves the specified unit in a specified direction. This may result in an attack, move... action.
     *
     * @param unit The unit to be moved.
     * @param direction The direction in which to move the Unit.
     */
    public void move(Unit unit, int direction) {
        Canvas canvas = freeColClient.getCanvas();

        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        // Be certain the tile we are about to move into has been updated by the server:
        // Can be removed if we use 'client.ask' when moving:
        /*
        try {
            while (game.getMap().getNeighbourOrNull(direction, unit.getTile()) != null && game.getMap().getNeighbourOrNull(direction, unit.getTile()).getType() == Tile.UNEXPLORED) {
                Thread.sleep(5);
            }
        } catch (InterruptedException ie) {}
        */

        int move = unit.getMoveType(direction);

        switch (move) {
            case Unit.MOVE:             reallyMove(unit, direction); break;
            case Unit.ATTACK:           attack(unit, direction); break;
            case Unit.DISEMBARK:        disembark(unit, direction); break;
            case Unit.EMBARK:           embark(unit, direction); break;
            case Unit.MOVE_HIGH_SEAS:   moveHighSeas(unit, direction); break;
            case Unit.ENTER_INDIAN_VILLAGE_WITH_SCOUT:
                                        scoutIndianSettlement(unit, direction); break;
            case Unit.ENTER_INDIAN_VILLAGE_WITH_MISSIONARY:
                                        useMissionary(unit, direction); break;
            case Unit.ENTER_INDIAN_VILLAGE_WITH_FREE_COLONIST:
                                        learnSkillAtIndianSettlement(unit, direction); break;
            case Unit.ENTER_FOREIGN_COLONY_WITH_SCOUT:
                                        // TODO
                                        freeColClient.playSound(SfxLibrary.ILLEGAL_MOVE); break;
            case Unit.ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS:
                                        tradeWithSettlement(unit, direction); break;
            case Unit.EXPLORE_LOST_CITY_RUMOUR:
                exploreLostCityRumour(unit, direction);
                break;
            case Unit.ILLEGAL_MOVE:     freeColClient.playSound(SfxLibrary.ILLEGAL_MOVE); break;
            default:                    throw new RuntimeException("unrecognised move: " + move);
        }

        // Display a "cash in"-dialog if a treasure train have been moved into a colony:  
        if (unit.getType() == Unit.TREASURE_TRAIN && unit.getLocation() != null && unit.getLocation() instanceof Tile && unit.getLocation().getTile().getColony() != null) {
            String message = (unit.getOwner().hasFather(FoundingFather.HERNAN_CORTES)) ? "cashInTreasureTrain.text.free" : "cashInTreasureTrain.text.pay";
            if (canvas.showConfirmDialog(message, "cashInTreasureTrain.yes", "cashInTreasureTrain.no")) {
                cashInTreasureTrain(unit);
            }
        }

        freeColClient.getActionManager().update();
        freeColClient.getCanvas().updateJMenuBar();
        
        nextModelMessage();   
    }

    /**
     * Ask for explore a lost city rumour, and move unit if player accepts
     *
     * @param unit The unit to be moved.
     * @param direction The direction in which to move the Unit.
     */
    private void exploreLostCityRumour(Unit unit, int direction) {
        if (freeColClient.getCanvas().showConfirmDialog("exploreLostCityRumour.text",
                                     "exploreLostCityRumour.yes",
                                     "exploreLostCityRumour.no")) {
            reallyMove(unit, direction);
        }
    }

    /**
    * Buys the given land from the indians.
    * @param tile The land which should be bought from the indians. 
    */
    public void buyLand(Tile tile) {   
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Element buyLandElement = Message.createNewRootElement("buyLand");
        buyLandElement.setAttribute("tile", tile.getID());

        freeColClient.getMyPlayer().buyLand(tile);

        freeColClient.getClient().sendAndWait(buyLandElement);
        
        freeColClient.getCanvas().updateGoldLabel();
    }


    /**
    * Uses the given unit to trade with a <code>Settlement</code> in
    * the given direction.
    *
    * @param unit The <code>Unit</code> that is a carrier containing goods.
    * @param direction The direction the unit could move in order to enter
    *            the <code>Settlement</code>.
    * @exception IllegalArgumentException if the unit is not a carrier, or
    *            if there is no <code>Settlement</code> in the given direction.
    * @see Settlement
    */
    private void tradeWithSettlement(Unit unit, int direction) {
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

        Goods goods = (Goods) canvas.showChoiceDialog(Messages.message("tradeProposition.text"), Messages.message("tradeProposition.cancel"), unit.getGoodsIterator());
        if (goods == null) { // == Trade aborted by the player.
            return;
        }

        Element tradePropositionElement = Message.createNewRootElement("tradeProposition");
        tradePropositionElement.setAttribute("unit", unit.getID());
        tradePropositionElement.setAttribute("settlement", settlement.getID());
        tradePropositionElement.appendChild(goods.toXMLElement(null, tradePropositionElement.getOwnerDocument()));

        Element reply = client.ask(tradePropositionElement);
        while (reply != null) {
            if (!reply.getTagName().equals("tradePropositionAnswer")) {
                logger.warning("Illegal reply.");
                throw new IllegalStateException();
            }

            int gold = Integer.parseInt(reply.getAttribute("gold"));
            if (gold == NO_NEED_FOR_THE_GOODS) {
                canvas.showInformationMessage("noNeedForTheGoods", new String[][] {{"%goods%", goods.getName()}});
                return;
            } else if (gold <= NO_TRADE) {
                canvas.showInformationMessage("noTrade");
                return;
            } else {
                ChoiceItem[] objects = {
                    new ChoiceItem(Messages.message("trade.takeOffer"), 1),
                    new ChoiceItem(Messages.message("trade.moreGold"), 2),
                    new ChoiceItem(Messages.message("trade.gift").replaceAll("%goods%", goods.getName()), 0),
                };

                String text = Messages.message("trade.text").replaceAll("%nation%", settlement.getOwner().getNationAsString());
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
            tradePropositionElement.setAttribute("unit", unit.getID());
            tradePropositionElement.setAttribute("settlement", settlement.getID());
            tradePropositionElement.appendChild(goods.toXMLElement(null, tradePropositionElement.getOwnerDocument()));
            tradePropositionElement.setAttribute("gold", Integer.toString((gold*11)/10));

            reply = client.ask(tradePropositionElement);
        }

        if (reply == null) {
            logger.warning("reply == null");
        }
    }


    /**
    * Trades the given goods. The goods gets transferred
    * from the given <code>Unit</code> to the given <code>Settlement</code>,
    * and the {@link Unit#getOwner unit's owner} collects the payment.
    */
    private void tradeWithSettlement(Unit unit, Settlement settlement, Goods goods, int gold) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();

        Element tradeElement = Message.createNewRootElement("trade");
        tradeElement.setAttribute("unit", unit.getID());
        tradeElement.setAttribute("settlement", settlement.getID());
        tradeElement.setAttribute("gold", Integer.toString(gold));
        tradeElement.appendChild(goods.toXMLElement(null, tradeElement.getOwnerDocument()));

        client.sendAndWait(tradeElement);

        unit.trade(settlement, goods, gold);
        nextActiveUnit(unit.getTile());
        
        freeColClient.getCanvas().updateGoldLabel();
    }


    /**
    * Trades the given goods. The goods gets transferred
    * from the given <code>Unit</code> to the given <code>Settlement</code>.
    */
    private void deliverGiftToSettlement(Unit unit, Settlement settlement, Goods goods) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();

        Element deliverGiftElement = Message.createNewRootElement("deliverGift");
        deliverGiftElement.setAttribute("unit", unit.getID());
        deliverGiftElement.setAttribute("settlement", settlement.getID());
        deliverGiftElement.appendChild(goods.toXMLElement(null, deliverGiftElement.getOwnerDocument()));

        client.sendAndWait(deliverGiftElement);

        unit.deliverGift(settlement, goods);
        nextActiveUnit(unit.getTile());
    }

    /**
     * Transfers the gold carried by this unit to the {@link Player owner}.
     *
     * @exception IllegalStateException if this unit is not a treasure train.
     *                                  or if it cannot be cashed in at it's current
     *                                  location.
     */
    private void cashInTreasureTrain(Unit unit) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();

        if (unit.getType() != Unit.TREASURE_TRAIN) {
            throw new IllegalStateException("Not a treasure train");
        }

        // Inform the server:
        Element cashInTreasureTrainElement = Message.createNewRootElement("cashInTreasureTrain");
        cashInTreasureTrainElement.setAttribute("unit", unit.getID());

        client.sendAndWait(cashInTreasureTrainElement);

        unit.cashInTreasureTrain();
        
        freeColClient.getCanvas().updateGoldLabel();

        nextActiveUnit();
    }


    /**
    * Actually moves a unit in a specified direction.
    *
    * @param unit The unit to be moved.
    * @param direction The direction in which to move the Unit.
    */
    private void reallyMove(Unit unit, int direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        GUI gui = freeColClient.getGUI();
        Canvas canvas = freeColClient.getCanvas();
        Client client = freeColClient.getClient();

        // Inform the server:
        Element moveElement = Message.createNewRootElement("move");
        moveElement.setAttribute("unit", unit.getID());
        moveElement.setAttribute("direction", Integer.toString(direction));

        // move before ask to server, to be in new tile in case there is a rumour
        unit.move(direction);
       
        //client.send(moveElement);
        Element reply = client.ask(moveElement);
        freeColClient.getInGameInputHandler().handle(client.getConnection(), reply);
        
        // set location again in order to meet with people player don't see before move        
        if (!unit.isDisposed()) {
            unit.setLocation(unit.getTile());
        }

        if (unit.getTile().isLand() && unit.getOwner().getNewLandName() == null) {
            String newLandName = canvas.showInputDialog("newLand.text", unit.getOwner().getDefaultNewLandName(), "newLand.yes", null);
            unit.getOwner().setNewLandName(newLandName);
            Element setNewLandNameElement = Message.createNewRootElement("setNewLandName");
            setNewLandNameElement.setAttribute("newLandName", newLandName);
            client.sendAndWait(setNewLandNameElement);
            canvas.showEventDialog(EventPanel.FIRST_LANDING);
        }

        if (unit.getTile().getSettlement() != null && unit.isCarrier() &&
                (unit.getDestination() == null ||
                unit.getDestination().getTile() == unit.getTile())) {
            canvas.showColonyPanel((Colony) unit.getTile().getSettlement());
        } else if (unit.getMovesLeft() > 0 && !unit.isDisposed()) {
            gui.setActiveUnit(unit);
        } else {
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
    private void attack(Unit unit, int direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Canvas canvas = freeColClient.getCanvas();
        Map map = freeColClient.getGame().getMap();
        Tile target = map.getNeighbourOrNull(direction, unit.getTile());
        if (target.getSettlement() != null && target.getSettlement() instanceof IndianSettlement
                && unit.isArmed()) {
            IndianSettlement settlement = (IndianSettlement) target.getSettlement();
            int userAction = canvas.showArmedUnitIndianSettlementDialog(settlement);
            Element reply;

            switch (userAction) {
                case FreeColDialog.SCOUT_INDIAN_SETTLEMENT_ATTACK:
                    reallyAttack(unit, direction);
                    return;
                case FreeColDialog.SCOUT_INDIAN_SETTLEMENT_CANCEL:
                    return;
                case FreeColDialog.SCOUT_INDIAN_SETTLEMENT_TRIBUTE:
                    Element demandMessage = Message.createNewRootElement("armedUnitDemandTribute");
                    demandMessage.setAttribute("unit", unit.getID());
                    demandMessage.setAttribute("direction", Integer.toString(direction));
                    reply = freeColClient.getClient().ask(demandMessage);
                    break;
                default:
                    logger.warning("Incorrect response returned from Canvas.showArmedUnitIndianSettlementDialog()");
                    return;
            }         
            
            if (reply.getTagName().equals("armedUnitDemandTributeResult")) {
                String result = reply.getAttribute("result");
                if (result.equals("agree")) {
                    String amount = reply.getAttribute("amount");
                    unit.getOwner().modifyGold(Integer.parseInt(amount));
                    freeColClient.getCanvas().updateGoldLabel();
                    canvas.showInformationMessage("scoutSettlement.tributeAgree", new String[][] {{"%replace%", amount}});
                } else if (result.equals("disagree")) {
                    canvas.showInformationMessage("scoutSettlement.tributeDisagree");
                }
            } else {
                logger.warning("Server gave an invalid reply to an armedUnitDemandTribute message");
                return;
            }

            nextActiveUnit(unit.getTile());
        } else {
            reallyAttack(unit, direction);
            return;
        }
    }
    
    /**
    * Performs an attack in a specified direction. Note that the server
    * handles the attack calculations here.
    *
    * @param unit The unit to perform the attack.
    * @param direction The direction in which to attack.
    */
    private void reallyAttack(Unit unit, int direction) {
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        Game game = freeColClient.getGame();
        Map map = game.getMap();
        Player enemy;
        Tile target = map.getNeighbourOrNull(direction, unit.getTile());
        Unit defender = target.getDefendingUnit(unit);
        if (defender == null) {
            enemy = target.getSettlement().getOwner();
        } else {
            enemy = defender.getOwner();
        }
        int stance = unit.getOwner().getStance(enemy);
        /**
         * If the owner and the other player are already at
         * war, attack.  Otherwise make sure the player knows
         * what he/she is doing.
         */
        if (unit.getType() != Unit.PRIVATEER) {
            switch (stance) {
            case Player.CEASE_FIRE:
                if (!canvas.showConfirmDialog("model.diplomacy.attack.ceaseFire",
                                              "model.diplomacy.attack.confirm",
                                              "cancel",
                                              new String [][] {{"%replace%", enemy.getNationAsString()}})) {
                    return;
                }
                break;
            case Player.PEACE:
                if (!canvas.showConfirmDialog("model.diplomacy.attack.peace",
                                              "model.diplomacy.attack.confirm",
                                              "cancel",
                                              new String [][] {{"%replace%", enemy.getNationAsString()}})) {
                    return;
                }
                break;
            case Player.ALLIANCE:
                freeColClient.playSound(SfxLibrary.ILLEGAL_MOVE); 
                canvas.showInformationMessage("model.diplomacy.attack.alliance",
                                              new String [][] {{"%replace%", enemy.getNationAsString()}});
                return;
            }
        }

        if (freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_PRECOMBAT) &&
            !canvas.showPreCombatDialog(unit, defender, target.getSettlement())) {
            return;
        }
        
        if (unit.getType() == Unit.ARTILLERY || unit.getType() == Unit.DAMAGED_ARTILLERY || unit.isNaval()) {
            freeColClient.playSound(SfxLibrary.ARTILLERY);
        }

        Element attackElement = Message.createNewRootElement("attack");
        attackElement.setAttribute("unit", unit.getID());
        attackElement.setAttribute("direction", Integer.toString(direction));

        // Get the result of the attack from the server:
        Element attackResultElement = client.ask(attackElement);

        int result = Integer.parseInt(attackResultElement.getAttribute("result"));
        int plunderGold = Integer.parseInt(attackResultElement.getAttribute("plunderGold"));

        // If a successful attack against a colony, we need to update the tile:
        Element utElement = getChildElement(attackResultElement, Tile.getXMLElementTagName());
        if (utElement != null) {
            Tile updateTile = (Tile) game.getFreeColGameObject(utElement.getAttribute("ID"));
            updateTile.readFromXMLElement(utElement);
        }

        // If there are captured goods, add to unit
        NodeList capturedGoods = attackResultElement.getElementsByTagName("capturedGoods");
        for (int i=0; i < capturedGoods.getLength(); ++i) {
            Element goods = (Element) capturedGoods.item(i);
            int type = Integer.parseInt(goods.getAttribute("type"));
            int amount = Integer.parseInt(goods.getAttribute("amount"));
            unit.getGoodsContainer().addGoods(type, amount);
        }

        // Get the defender:
        Element unitElement = getChildElement(attackResultElement, Unit.getXMLElementTagName());
        if (unitElement != null) {            
            defender = (Unit) game.getFreeColGameObject(unitElement.getAttribute("ID"));
            if (defender == null) {
                defender = new Unit(game, unitElement);
            } else {
                defender.readFromXMLElement(unitElement);
            }
            defender.setLocation(target);
        } else {
            defender = map.getNeighbourOrNull(direction, unit.getTile()).getDefendingUnit(unit);
        }

        if (defender == null) {
            logger.warning("defender == null");
            throw new NullPointerException("defender == null");
        }


        if (!unit.isNaval()) { 
            Unit winner;
            if (result >= Unit.ATTACK_EVADES) {
                winner = unit;
            } else {
                winner = defender;
            }
            if (winner.isArmed()) {
                if (winner.isMounted()) {
                    if (winner.getType() == Unit.BRAVE) {
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
            if (result >= Unit.ATTACK_GREAT_WIN || result <= Unit.ATTACK_GREAT_LOSS) {
                freeColClient.playSound(SfxLibrary.SUNK);
            } 
         } 


        unit.attack(defender, result, plunderGold);
        if (!defender.isDisposed() && ((result == Unit.ATTACK_DONE_SETTLEMENT && unitElement != null) ||
                defender.getLocation() == null || !defender.isVisibleTo(freeColClient.getMyPlayer()))) {
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
    }


    /**
     * Disembarks the specified unit in a specified direction.
     *
     * @param unit The unit to be disembarked.
     * @param direction The direction in which to disembark the Unit.
     */
    private void disembark(Unit unit, int direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Game game = freeColClient.getGame();
        Canvas canvas = freeColClient.getCanvas();
        Tile destinationTile = game.getMap().getNeighbourOrNull(direction, unit.getTile());

        if (canvas.showConfirmDialog("disembark.text", "disembark.yes", "disembark.no")) {
            unit.setStateToAllChildren(Unit.ACTIVE);

            Iterator unitIterator = unit.getUnitIterator();

            while (unitIterator.hasNext()) {
                Unit u = (Unit) unitIterator.next();

                if ((u.getState() == Unit.ACTIVE) && u.getMovesLeft() > 0) {
                    if (destinationTile.hasLostCityRumour()) {
                        exploreLostCityRumour(u, direction);
                    } else {
                        reallyMove(u, direction);
                    }
                    return;
                }
            }
        }
    }


    /**
     * Embarks the specified unit in a specified direction.
     *
     * @param unit The unit to be embarked.
     * @param direction The direction in which to embark the Unit.
     */
    private void embark(Unit unit, int direction) {
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
            ArrayList choices = new ArrayList();
            Iterator unitIterator = destinationTile.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit nextUnit = (Unit) unitIterator.next();
                if (nextUnit.getSpaceLeft() >= unit.getTakeSpace()) {
                    choices.add(nextUnit);
                }
            }

            if (choices.size() == 1) {
                destinationUnit = (Unit) choices.get(0);
            } else if (choices.size() == 0) {
                throw new IllegalStateException();
            } else {
                destinationUnit = (Unit) canvas.showChoiceDialog(Messages.message("embark.text"), Messages.message("embark.cancel"), choices.iterator());
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
        embarkElement.setAttribute("unit", unit.getID());
        embarkElement.setAttribute("direction", Integer.toString(direction));
        embarkElement.setAttribute("embarkOnto", destinationUnit.getID());

        client.sendAndWait(embarkElement);
    }


    /**
    * Boards a specified unit onto a carrier. The carrier should be
    * at the same tile as the boarding unit.
    *
    * @param unit The unit who is going to board the carrier.
    * @param carrier The carrier.
    * @return <i>true</i> if the <code>unit</code> actually gets
    *         on the <code>carrier</code>.
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

        if (unit.getTile() == null || unit.getTile().getColony() == null ||
                unit.getTile().getColony().getUnitCount() > 1 ||
                !(unit.getLocation() instanceof Colony || unit.getLocation() instanceof Building ||
                unit.getLocation() instanceof ColonyTile) ||
                freeColClient.getCanvas().showConfirmDialog("abandonColony.text", "abandonColony.yes", "abandonColony.no")) {

            freeColClient.playSound(SfxLibrary.LOAD_CARGO);

            Element boardShipElement = Message.createNewRootElement("boardShip");
            boardShipElement.setAttribute("unit", unit.getID());
            boardShipElement.setAttribute("carrier", carrier.getID());

            unit.boardShip(carrier);

            client.sendAndWait(boardShipElement);

            return true;
        }

        return false;
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
        clearSpecialityElement.setAttribute("unit", unit.getID());

        unit.clearSpeciality();

        client.sendAndWait(clearSpecialityElement);
    }


    /**
    * Leave a ship. This method should only be invoked if the ship is in a harbour.
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
        leaveShipElement.setAttribute("unit", unit.getID());

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
        loadCargoElement.setAttribute("carrier", carrier.getID());
        loadCargoElement.appendChild(goods.toXMLElement(freeColClient.getMyPlayer(), loadCargoElement.getOwnerDocument()));

        goods.loadOnto(carrier);

        client.sendAndWait(loadCargoElement);
    }


    /**
    * Unload cargo. This method should only be invoked if the unit carrying the
    * cargo is in a harbour.
    *
    * @param goods The goods which are going to leave the ship where it is located.
    */
    public void unloadCargo(Goods goods) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();

        goods.adjustAmount();

        Element unloadCargoElement = Message.createNewRootElement("unloadCargo");
        unloadCargoElement.appendChild(goods.toXMLElement(freeColClient.getMyPlayer(), unloadCargoElement.getOwnerDocument()));

        goods.unload();

        client.sendAndWait(unloadCargoElement);
    }


    /**
    * Buys goods in Europe. The amount of goods is adjusted if there
    * is lack of space in the <code>carrier</code>.
    *
    * @param type The type of goods to buy.
    * @param amount The amount of goods to buy.
    * @param carrier The carrier.
    */
    public void buyGoods(int type, int amount, Unit carrier) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();
        Game game = freeColClient.getGame();
        Player myPlayer = freeColClient.getMyPlayer();
        Canvas canvas = freeColClient.getCanvas();

        if (carrier == null) {
            throw new NullPointerException();
        }

        if (carrier.getOwner() != myPlayer || (carrier.getSpaceLeft() <= 0 &&
                (carrier.getGoodsContainer().getGoodsCount(type) % 100 == 0))) {
            return;
        }

        if (carrier.getSpaceLeft() <= 0) {
            amount = Math.min(amount, 100 - carrier.getGoodsContainer().getGoodsCount(type) % 100);
        }

        if (game.getMarket().getBidPrice(type, amount) > myPlayer.getGold()) {
            canvas.errorMessage("notEnoughGold");
            return;
        }

        freeColClient.playSound(SfxLibrary.LOAD_CARGO);

        Element buyGoodsElement = Message.createNewRootElement("buyGoods");
        buyGoodsElement.setAttribute("carrier", carrier.getID());
        buyGoodsElement.setAttribute("type", Integer.toString(type));
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
        sellGoodsElement.appendChild(goods.toXMLElement(freeColClient.getMyPlayer(), sellGoodsElement.getOwnerDocument()));

        player.getGame().getMarket().sell(goods, player);
        freeColClient.getCanvas().updateGoldLabel();

        client.sendAndWait(sellGoodsElement);
    }

    /**
     * Sets the export settings of the custom house.
     *
     * @param colony The colony with the custom house.
     * @param goods The goods for which to set the settings.
     */
    public void setExports(Colony colony, int goodsType, boolean value) {
        Client client = freeColClient.getClient();

        Element setExportsElement = Message.createNewRootElement("setExports");
        setExportsElement.setAttribute("colony", colony.getID());
        setExportsElement.setAttribute("goods", String.valueOf(goodsType));
        setExportsElement.setAttribute("value", String.valueOf(value));

        colony.setExports(goodsType, value);
        client.sendAndWait(setExportsElement);
    }
        
    
    /**
    * Equips or unequips a <code>Unit</code> with a certain type of <code>Goods</code>.
    *
    * @param unit The <code>Unit</code>.
    * @param type The type of <code>Goods</code>.
    * @param amount How many of these goods the unit should have.
    */
    public void equipUnit(Unit unit, int type, int amount) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();
        Player myPlayer = freeColClient.getMyPlayer();

        Unit carrier = null;
        if (unit.getLocation() instanceof Unit) {
            carrier = (Unit) unit.getLocation();
            leaveShip(unit);
        }

        if (unit.getTile() == null || unit.getTile().getColony() == null || unit.getTile().getColony().getUnitCount() > 1 ||
                !(unit.getLocation() instanceof Colony || unit.getLocation() instanceof Building || unit.getLocation() instanceof ColonyTile)
                || freeColClient.getCanvas().showConfirmDialog("abandonColony.text", "abandonColony.yes", "abandonColony.no")) {
            // Equip the unit first.
        } else {
            return; // The user cancelled the action.
        }

        Element equipUnitElement = Message.createNewRootElement("equipunit");
        equipUnitElement.setAttribute("unit", unit.getID());
        equipUnitElement.setAttribute("type", Integer.toString(type));
        equipUnitElement.setAttribute("amount", Integer.toString(amount));


        switch(type) {
            case Goods.CROSSES:
                unit.setMissionary((amount > 0));
                break;
            case Goods.MUSKETS:
                if (unit.isInEurope()) {
                    if (!myPlayer.canTrade(type)) {
                        payArrears(type);
                        if (!myPlayer.canTrade(type)) {
                            return; // The user cancelled the action.
                        }
                    }
                }
                unit.setArmed((amount > 0)); // So give them muskets if the amount we want is greater than zero.
                break;
            case Goods.HORSES:
                if (unit.isInEurope()) {
                    if (!myPlayer.canTrade(type)) {
                        payArrears(type);
                        if (!myPlayer.canTrade(type)) {
                            return; // The user cancelled the action.
                        }
                    }
                }
                unit.setMounted((amount > 0)); // As above.
                break;
            case Goods.TOOLS:
                if (unit.isInEurope()) {
                    if (!myPlayer.canTrade(type)) {
                        payArrears(type);
                        if (!myPlayer.canTrade(type)) {
                            return; // The user cancelled the action.
                        }
                    }
                }
                int actualAmount = amount;
                if ((actualAmount % 20) > 0) {
                    logger.warning("Trying to set a number of tools that is not a multiple of 20.");
                    actualAmount -= (actualAmount % 20);
                }
                unit.setNumberOfTools(actualAmount);
                break;
            default:
                logger.warning("Invalid type of goods to equip.");
                return;
        }

        freeColClient.getCanvas().updateGoldLabel();

        client.sendAndWait(equipUnitElement);

        if (unit.getLocation() instanceof Colony || unit.getLocation() instanceof Building || unit.getLocation() instanceof ColonyTile) {
            putOutsideColony(unit, true);
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
        workElement.setAttribute("unit", unit.getID());
        workElement.setAttribute("workLocation", workLocation.getID());

        unit.work(workLocation);

        client.sendAndWait(workElement);
    }


    /**
    * Puts the specified unit outside the colony.
    * @param unit The <code>Unit</code>
    * @return <i>true</i> if the unit was successfully put outside the colony.
    */
    public boolean putOutsideColony(Unit unit) {
        return putOutsideColony(unit, false);
    }

    /**
    * Puts the specified unit outside the colony.
    * 
    * @param unit The <code>Unit</code>
    * @param skipCheck The confirmation dialog for abandoning the colony
    *       will not be displayed if this parameter is set to <code>true</code>.
    * @return <i>true</i> if the unit was successfully put outside the colony.
    */
    public boolean putOutsideColony(Unit unit, boolean skipCheck) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            throw new IllegalStateException("Not your turn.");
        }
        
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();

        if (skipCheck || unit.getTile().getColony().getUnitCount() > 1 || !(unit.getLocation() instanceof Colony ||
                unit.getLocation() instanceof Building || unit.getLocation() instanceof ColonyTile)
                || canvas.showConfirmDialog("abandonColony.text", "abandonColony.yes", "abandonColony.no")) {

            Element putOutsideColonyElement = Message.createNewRootElement("putOutsideColony");
            putOutsideColonyElement.setAttribute("unit", unit.getID());
            unit.putOutsideColony();
            client.sendAndWait(putOutsideColonyElement);

            return true;
        }

        return false;
    }

    /**
    * Changes the work type of this <code>Unit</code>.
    * @param unit The <code>Unit</code>
    * @param workType The new work type.
    */
    public void changeWorkType(Unit unit, int workType) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();

        Element changeWorkTypeElement = Message.createNewRootElement("changeWorkType");
        changeWorkTypeElement.setAttribute("unit", unit.getID());
        changeWorkTypeElement.setAttribute("workType", Integer.toString(workType));

        unit.setWorkType(workType);

        client.sendAndWait(changeWorkTypeElement);
    }

    /**
    * Changes the current construction project of a <code>Colony</code>.
    * @param colony The <code>Colony</code>
    * @param type The new type of building to build.
    */
    public void setCurrentlyBuilding(Colony colony, int type) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();

        colony.setCurrentlyBuilding(type);

        Element setCurrentlyBuildingElement = Message.createNewRootElement("setCurrentlyBuilding");
        setCurrentlyBuildingElement.setAttribute("colony", colony.getID());
        setCurrentlyBuildingElement.setAttribute("type", Integer.toString(type));

        client.sendAndWait(setCurrentlyBuildingElement);
    }


    /**
    * Changes the state of this <code>Unit</code>.
    * @param unit The <code>Unit</code>
    * @param state The state of the unit.
    */
    public void changeState(Unit unit, int state) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();
        Game game = freeColClient.getGame();
        Canvas canvas = freeColClient.getCanvas();

        if (!(unit.checkSetState(state))) {
            logger.warning("Can't set state " + state);
            return; // Don't bother.
        }

        if (state == Unit.PLOW || state == Unit.BUILD_ROAD) {
            if (unit.getTile().getNationOwner() != Player.NO_NATION
                    && unit.getTile().getNationOwner() != unit.getOwner().getNation()
                    && !Player.isEuropean(unit.getTile().getNationOwner())
                    && !unit.getOwner().hasFather(FoundingFather.PETER_MINUIT)) {
                int nation = unit.getTile().getNationOwner();
                int price = game.getPlayer(unit.getTile().getNationOwner()).getLandPrice(unit.getTile());
                ChoiceItem[] choices = {
                    new ChoiceItem(Messages.message("indianLand.pay").replaceAll("%amount%", Integer.toString(price)), 1),
                    new ChoiceItem(Messages.message("indianLand.take"), 2)
                };
                ChoiceItem ci = (ChoiceItem) canvas.showChoiceDialog(
                    Messages.message("indianLand.text").replaceAll("%player%", Player.getNationAsString(nation)),
                    Messages.message("indianLand.cancel"), choices
                );
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
        }

        Element changeStateElement = Message.createNewRootElement("changeState");
        changeStateElement.setAttribute("unit", unit.getID());
        changeStateElement.setAttribute("state", Integer.toString(state));

        unit.setState(state);

        if (!freeColClient.getCanvas().getColonyPanel().isShowing() &&
                (unit.getMovesLeft() == 0 || unit.getState() == Unit.SENTRY)) {
            nextActiveUnit();
        } else {
            freeColClient.getCanvas().refresh();
        }

        client.sendAndWait(changeStateElement);
    }


    /**
    * Clears the orders of the given <code>Unit</code>
    * The orders are cleared by making the unit {@link Unit#ACTIVE} and
    * setting a null destination
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
        /* report to server, in order not to restore destination
         * if it's received in a update message
         */
        clearGotoOrders(unit);
        changeState(unit, Unit.ACTIVE);
    }

    /**
     * Clears the orders of the given <code>Unit</code>.
     * The orders are cleared by making the unit {@link Unit#ACTIVE}.
     *
     * @param unit The <code>Unit</code>.
     */
    public void clearGotoOrders(Unit unit) {
        if (unit == null) {
            return;
        }

        /* report to server, in order not to restore destination
         * if it's received in a update message
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
    private void moveHighSeas(Unit unit, int direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Canvas canvas = freeColClient.getCanvas();
        Map map = freeColClient.getGame().getMap();

        if (unit.getTile() == null || canvas.showConfirmDialog("highseas.text", "highseas.yes", "highseas.no")) {
            moveToEurope(unit);
            nextActiveUnit();
        } else if (map.getNeighbourOrNull(direction, unit.getTile()) != null) {
            reallyMove(unit, direction);
        }
    }


    /**
     * Moves the specified free colonist into an Indian settlement to learn a skill.
     * Of course, the colonist won't physically get into the village, it will
     * just stay where it is and gain the skill.
     *
     * @param unit The unit to learn the skill.
     * @param direction The direction in which the Indian settlement lies.
     */
    private void learnSkillAtIndianSettlement(Unit unit, int direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        Map map = freeColClient.getGame().getMap();
        IndianSettlement settlement = (IndianSettlement) map.getNeighbourOrNull(direction, unit.getTile()).getSettlement();

        if (settlement.getLearnableSkill() != IndianSettlement.NONE) {
            unit.setMovesLeft(0);

            String skillName;

            Element askSkill = Message.createNewRootElement("askSkill");
            askSkill.setAttribute("unit", unit.getID());
            askSkill.setAttribute("direction", Integer.toString(direction));

            Element reply = client.ask(askSkill);
            int skill;

            if (reply.getTagName().equals("provideSkill")) {
                skill = Integer.parseInt(reply.getAttribute("skill"));
                if (skill < 0) {
                    skillName = null;
                }
                else {
                    skillName = Unit.getName(skill);
                }
            }
            else {
                logger.warning("Server gave an invalid reply to an askSkill message");
                return;
            }

            settlement.setLearnableSkill(skill);

            if (skillName == null) {
                canvas.errorMessage("indianSettlement.noMoreSkill");
            } else {
                Element learnSkill = Message.createNewRootElement("learnSkillAtSettlement");
                learnSkill.setAttribute("unit", unit.getID());
                learnSkill.setAttribute("direction", Integer.toString(direction));

                if (!canvas.showConfirmDialog("learnSkill.text",
                                             "learnSkill.yes",
                                             "learnSkill.no",
                                             new String [][] {{"%replace%", skillName}})) {
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
                    settlement.setLearnableSkill(IndianSettlement.NONE);
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
     * Moves the specified scout into an Indian settlement to speak with the chief
     * or demand a tribute etc.
     * Of course, the scout won't physically get into the village, it will
     * just stay where it is.
     *
     * @param unit The unit that will speak, attack or ask tribute.
     * @param direction The direction in which the Indian settlement lies.
     */
    private void scoutIndianSettlement(Unit unit, int direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        Map map = freeColClient.getGame().getMap();
        IndianSettlement settlement = (IndianSettlement) map.getNeighbourOrNull(direction, unit.getTile()).getSettlement();

        // The scout loses his moves because the skill data and tradeable goods data is fetched
        // from the server and the moves are the price we have to pay to obtain that data.
        unit.setMovesLeft(0);

        Element scoutMessage = Message.createNewRootElement("scoutIndianSettlement");
        scoutMessage.setAttribute("unit", unit.getID());
        scoutMessage.setAttribute("direction", Integer.toString(direction));
        scoutMessage.setAttribute("action", "basic");
        Element reply = client.ask(scoutMessage);

        if (reply.getTagName().equals("scoutIndianSettlementResult")) {
            settlement.setLearnableSkill(Integer.parseInt(reply.getAttribute("skill")));
            settlement.setWantedGoods(0, Integer.parseInt(reply.getAttribute("highlyWantedGoods")));
            settlement.setWantedGoods(1, Integer.parseInt(reply.getAttribute("wantedGoods1")));
            settlement.setWantedGoods(2, Integer.parseInt(reply.getAttribute("wantedGoods2")));
        } else {
            logger.warning("Server gave an invalid reply to an askSkill message");
            return;
        }

        int userAction = canvas.showScoutIndianSettlementDialog(settlement);

        switch (userAction) {
            case FreeColDialog.SCOUT_INDIAN_SETTLEMENT_ATTACK:
                scoutMessage.setAttribute("action", "attack");
                // The movesLeft has been set to 0 when the scout initiated its action.
                // If it wants to attack then it can and it will need some moves to do it.
                unit.setMovesLeft(1);
                client.sendAndWait(scoutMessage);
                reallyAttack(unit, direction);
                return;
            case FreeColDialog.SCOUT_INDIAN_SETTLEMENT_CANCEL:
                scoutMessage.setAttribute("action", "cancel");
                client.sendAndWait(scoutMessage);
                return;
            case FreeColDialog.SCOUT_INDIAN_SETTLEMENT_SPEAK:
                scoutMessage.setAttribute("action", "speak");
                reply = client.ask(scoutMessage);
                break;
            case FreeColDialog.SCOUT_INDIAN_SETTLEMENT_TRIBUTE:
                scoutMessage.setAttribute("action", "tribute");
                reply = client.ask(scoutMessage);
                break;
            default:
                logger.warning("Incorrect response returned from Canvas.showScoutIndianSettlementDialog()");
                return;
        }

        if (reply.getTagName().equals("scoutIndianSettlementResult")) {
            String result = reply.getAttribute("result"),
                action = scoutMessage.getAttribute("action");
            if (result.equals("die")) {
                unit.dispose();
                canvas.showInformationMessage("scoutSettlement.speakDie");
            }
            else if (action.equals("speak") && result.equals("tales")) {
                // Parse the tiles.
                Element updateElement = getChildElement(reply, "update");
                if (updateElement != null) {
                    freeColClient.getInGameInputHandler().handle(client.getConnection(), updateElement);
                }

                canvas.showInformationMessage("scoutSettlement.speakTales");
            }
            else if (action.equals("speak") && result.equals("beads")) {
                String amount = reply.getAttribute("amount");
                unit.getOwner().modifyGold(Integer.parseInt(amount));
                freeColClient.getCanvas().updateGoldLabel();
                canvas.showInformationMessage("scoutSettlement.speakBeads", new String[][] {{"%replace%", amount}});
            }
            else if (action.equals("speak") && result.equals("nothing")) {
                canvas.showInformationMessage("scoutSettlement.speakNothing");
            }
            else if (action.equals("tribute") && result.equals("agree")) {
                String amount = reply.getAttribute("amount");
                unit.getOwner().modifyGold(Integer.parseInt(amount));
                freeColClient.getCanvas().updateGoldLabel();
                canvas.showInformationMessage("scoutSettlement.tributeAgree", new String[][] {{"%replace%", amount}});
            }
            else if (action.equals("tribute") && result.equals("disagree")) {
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
    * @param unit The unit that will enter the settlement.
    * @param direction The direction in which the Indian settlement lies.
    */
    private void useMissionary(Unit unit, int direction) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        Map map = freeColClient.getGame().getMap();
        IndianSettlement settlement = (IndianSettlement) map.getNeighbourOrNull(direction, unit.getTile()).getSettlement();

        List response = canvas.showUseMissionaryDialog(settlement);
        int action = ((Integer)response.get(0)).intValue();

        Element missionaryMessage = Message.createNewRootElement("missionaryAtSettlement");
        missionaryMessage.setAttribute("unit", unit.getID());
        missionaryMessage.setAttribute("direction", Integer.toString(direction));

        Element reply = null;

        unit.setMovesLeft(0);

        switch (action) {
            case FreeColDialog.MISSIONARY_CANCEL:
                missionaryMessage.setAttribute("action", "cancel");
                client.sendAndWait(missionaryMessage);
                break;
            case FreeColDialog.MISSIONARY_ESTABLISH:
                missionaryMessage.setAttribute("action", "establish");
                settlement.setMissionary(unit);
                client.sendAndWait(missionaryMessage);
                nextActiveUnit(); // At this point: unit.getTile() == null
                return;
            case FreeColDialog.MISSIONARY_DENOUNCE_AS_HERESY:
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
            case FreeColDialog.MISSIONARY_INCITE_INDIANS:
                missionaryMessage.setAttribute("action", "incite");
                missionaryMessage.setAttribute("incite", ((Player)response.get(1)).getID());

                reply = client.ask(missionaryMessage);

                if (reply.getTagName().equals("missionaryReply")) {
                    int amount = Integer.parseInt(reply.getAttribute("amount"));

                    boolean confirmed = canvas.showInciteDialog((Player)response.get(1), amount);

                    Element inciteMessage = Message.createNewRootElement("inciteAtSettlement");
                    inciteMessage.setAttribute("unit", unit.getID());
                    inciteMessage.setAttribute("direction", Integer.toString(direction));
                    inciteMessage.setAttribute("confirmed", confirmed ? "true" : "false");
                    inciteMessage.setAttribute("enemy", ((Player)response.get(1)).getID());

                    if (confirmed) {
                        unit.getOwner().modifyGold(-amount);

                        // Maybe at this point we can keep track of the fact that the indian is now at
                        // war with the chosen european player, but is this really necessary at the client
                        // side?
                        settlement.getOwner().setStance((Player)response.get(1), Player.WAR);
                        ((Player)response.get(1)).setStance(settlement.getOwner(), Player.WAR);
                    }

                    client.sendAndWait(inciteMessage);
                }
                else {
                    logger.warning("Server gave an invalid reply to a missionaryAtSettlement message");
                    return;
                }
        }

        nextActiveUnit(unit.getTile());
    }


    /**
     * Moves the specified unit to Europe.
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
        moveToEuropeElement.setAttribute("unit", unit.getID());

        client.sendAndWait(moveToEuropeElement);
    }


    /**
     * Moves the specified unit to America.
     * @param unit The unit to be moved to America.
     */
    public void moveToAmerica(Unit unit) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();

        unit.moveToAmerica();

        Element moveToAmericaElement = Message.createNewRootElement("moveToAmerica");
        moveToAmericaElement.setAttribute("unit", unit.getID());

        client.sendAndWait(moveToAmericaElement);
    }


    /**
    * Trains a unit of a specified type in Europe.
    * @param unitType The type of unit to be trained.
    */
    public void trainUnitInEurope(int unitType) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        Game game = freeColClient.getGame();
        Player myPlayer = freeColClient.getMyPlayer();
        Europe europe = myPlayer.getEurope();

        if ((unitType != Unit.ARTILLERY && myPlayer.getGold() < Unit.getPrice(unitType)) ||
            (unitType == Unit.ARTILLERY && myPlayer.getGold() < europe.getArtilleryPrice())) {
            System.out.println("Price: " + Unit.getPrice(unitType) + ", Gold: " + myPlayer.getGold());
            canvas.errorMessage("notEnoughGold");
            return;
        }

        Element trainUnitInEuropeElement = Message.createNewRootElement("trainUnitInEurope");
        trainUnitInEuropeElement.setAttribute("unitType", Integer.toString(unitType));

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
    * Buys the remaining hammers and tools for the {@link Building}
    * currently being built in the given <code>Colony</code>.
    *
    * @param colony The {@link Colony} where the building should be bought.
    */
    public void payForBuilding(Colony colony) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        if (!freeColClient.getCanvas().
            showConfirmDialog("payForBuilding.text",
                              "payForBuilding.yes",
                              "payForBuilding.no",
                              new String [][] {{"%replace%", Integer.toString(colony.getPriceForBuilding())}})) {
            return;
        }

        if (colony.getPriceForBuilding() > freeColClient.getMyPlayer().getGold()) {
            freeColClient.getCanvas().errorMessage("notEnoughGold");
            return;
        }

        Element payForBuildingElement = Message.createNewRootElement("payForBuilding");
        payForBuildingElement.setAttribute("colony", colony.getID());

        colony.payForBuilding();

        freeColClient.getClient().sendAndWait(payForBuildingElement);
    }

    /**
    * Recruit a unit from a specified "slot" in Europe.
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
            europe.recruit(slot, unit, Integer.parseInt(reply.getAttribute("newRecruitable")));
        } else {
            logger.warning("Could not recruit the specified unit in europe.");
            return;
        }
        
        freeColClient.getCanvas().updateGoldLabel();
    }


    /**
    * Cause a unit to emigrate from a specified "slot" in Europe. If the player doesn't have
    * William Brewster in the congress then the value of the slot parameter is not important
    * (it won't be used).
    * @param slot The slot from which the unit emigrates. Either 1, 2 or 3 if William Brewster
    * is in the congress.
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
        if (myPlayer.hasFather(FoundingFather.WILLIAM_BREWSTER)) {
            emigrateUnitInEuropeElement.setAttribute("slot", Integer.toString(slot));
        }

        Element reply = client.ask(emigrateUnitInEuropeElement);

        if (reply == null || !reply.getTagName().equals("emigrateUnitInEuropeConfirmed")) {
            logger.warning("Could not recruit unit: " + myPlayer.getCrosses() + "/" + myPlayer.getCrossesRequired());
            throw new IllegalStateException();
        }

        if (!myPlayer.hasFather(FoundingFather.WILLIAM_BREWSTER)) {
            slot = Integer.parseInt(reply.getAttribute("slot"));
        }

        Element unitElement = (Element) reply.getChildNodes().item(0);
        Unit unit = (Unit) game.getFreeColGameObject(unitElement.getAttribute("ID"));
        if (unit == null) {
            unit = new Unit(game, unitElement);
        } else {
            unit.readFromXMLElement(unitElement);
        }
        int newRecruitable = Integer.parseInt(reply.getAttribute("newRecruitable"));
        europe.emigrate(slot, unit, newRecruitable);

        freeColClient.getCanvas().updateGoldLabel();
    }

    /**
     * Updates a trade route.
     */
    public void updateTradeRoute(TradeRoute route) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        Element tradeRouteElement = Message.createNewRootElement("updateTradeRoute");
        tradeRouteElement.setAttribute("ID", route.getID());
        tradeRouteElement.appendChild(route.toXMLElement(null, tradeRouteElement.getOwnerDocument()));
        freeColClient.getClient().sendAndWait(tradeRouteElement);
        
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
    public void payArrears(int type) {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        Client client = freeColClient.getClient();
        Player player = freeColClient.getMyPlayer();

        int arrears = player.getArrears(type);
        if (player.getGold() >= arrears) {
            if (freeColClient.getCanvas().
                showConfirmDialog("model.europe.payArrears",
                                  "ok", "cancel",
                                  new String [][] {{"%replace%", String.valueOf(arrears)}})) {
                player.modifyGold(-arrears);
                freeColClient.getCanvas().updateGoldLabel();
                player.resetArrears(type);
                // send to server
                Element payArrearsElement = Message.createNewRootElement("payArrears");
                payArrearsElement.setAttribute("goodsType", String.valueOf(type));
                client.sendAndWait(payArrearsElement);        
            }
        } else {
            freeColClient.getCanvas().
                showInformationMessage("model.europe.cantPayArrears",
                                       new String [][] {{"%amount%", String.valueOf(arrears)}});
        }
    }
    
    /**
    * Purchases a unit of a specified type in Europe.
    * @param unitType The type of unit to be purchased.
    */
    public void purchaseUnitFromEurope(int unitType) {
        trainUnitInEurope(unitType);
    }


    /**
    * Skips the active unit by setting it's <code>movesLeft</code> to 0.
    */
    public void skipActiveUnit() {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return;
        }
        
        GUI gui = freeColClient.getGUI();

        Unit unit = gui.getActiveUnit();
        
        if (unit != null) {
            Element skipUnit = Message.createNewRootElement("skipUnit");
            skipUnit.setAttribute("unit", unit.getID());
           
            unit.skip();
            
            freeColClient.getClient().sendAndWait(skipUnit);
        }

        nextActiveUnit();
    }

    /**
     * Gathers information about opponents.
     */
    public Element getForeignAffairsReport() {
        if (freeColClient.getGame().getCurrentPlayer() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return null;
        }

        Element reply = freeColClient.getClient().ask(Message.createNewRootElement("foreignAffairs"));
        return reply;
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

        if(unit == null) {
            return;
        }

        if(!freeColClient.getCanvas().showConfirmDialog("disbandUnit.text", "disbandUnit.yes", "disbandUnit.no")) {
            return;
        }

        Element disbandUnit = Message.createNewRootElement("disbandUnit");
        disbandUnit.setAttribute("unit", unit.getID());

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
            while (!freeColClient.getCanvas().isShowingSubPanel()
                    && myPlayer.hasNextGoingToUnit()) {
                moveToDestination(myPlayer.getNextGoingToUnit());
                nextModelMessage();
            }

            if (!myPlayer.hasNextGoingToUnit()
                    && !freeColClient.getCanvas().isShowingSubPanel()) {
                if (endingTurn) {
                    canvas.getGUI().setActiveUnit(null);
                    //canvas.setEnabled(false);

                    Element endTurnElement = Message.createNewRootElement("endTurn");
                    freeColClient.getClient().sendAndWait(endTurnElement);

                    endingTurn = false;
                } else {
                    executeGoto = false;
                }
            }
        } else {
            GUI gui = freeColClient.getGUI();
            Unit nextActiveUnit = myPlayer.getNextActiveUnit();

            if (nextActiveUnit != null) {
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
            }
        }
    }


    /**
     * Ignore this ModelMessage from now on until it is not generated
     * in a turn.
     *
     * @param message a <code>ModelMessage</code> value
     * @param flag whether to ignore the ModelMessage or not
     */
    public void ignoreMessage(ModelMessage message, boolean flag) {
        String key = message.getSource().getID();
        for (String[] replacement : message.getData()) {
            if (replacement[0].equals("%goods%")) {
                key += replacement[1];
                break;
            }
        }
        if (flag) {
            logger.finer("Ignore model message with key " + key);
            messagesToIgnore.put(key, new Integer(freeColClient.getGame().getTurn().getNumber()));
        } else {
            logger.finer("Removing model message with key " + key +
                         " from ignored messages.");
            messagesToIgnore.remove(key);
        }
    }

    /**
    * Displays the next <code>ModelMessage</code>.
    * @see net.sf.freecol.common.model.ModelMessage ModelMessage
    */
    public void nextModelMessage() {
        Canvas  canvas = freeColClient.getCanvas();
        int thisTurn = freeColClient.getGame().getTurn().getNumber();

        ArrayList<ModelMessage> messageList = new ArrayList<ModelMessage>();

        for ( Iterator i = freeColClient.getGame().getModelMessageIterator(freeColClient.getMyPlayer());
              i.hasNext(); ) {

            ModelMessage message = (ModelMessage) i.next();
            if (shouldAllowMessage(message)) {
                if (message.getType() == ModelMessage.WAREHOUSE_CAPACITY) {
                    String key = message.getSource().getID();
                    for (String[] replacement : message.getData()) {
                        if (replacement[0].equals("%goods%")) {
                            key += replacement[1];
                            break;
                        }
                    }

                    Integer turn = messagesToIgnore.get(key);
                    if (turn != null &&
                        turn.intValue() == thisTurn - 1) {
                        logger.finer("Ignoring model message with key " + key);
                        messagesToIgnore.put(key, new Integer(thisTurn));
                        message.setBeenDisplayed(true);
                        continue;
                    }
                } else if (message.getSource() instanceof Market) {
                    // TODO: remove as soon as all players have their
                    // own market
                    message.setSource(freeColClient.getMyPlayer().getEurope());
                }
                messageList.add(message);
            }

            // flag all messages delivered as "beenDisplayed". 
            message.setBeenDisplayed(true);
        }

        Iterator mapIterator = messagesToIgnore.entrySet().iterator();
        while (mapIterator.hasNext()) {
            java.util.Map.Entry entry = (java.util.Map.Entry) mapIterator.next();
            if (((Integer) entry.getValue()).intValue() < thisTurn - 1) {
                logger.finer("Removing old model message with key " + entry.getKey() +
                             " from ignored messages.");
                mapIterator.remove();
            }
        }

        if (messageList.size() > 1) {
            canvas.showTurnReport(messageList);
        } else if (messageList.size() == 1) {
            canvas.showModelMessage(messageList.get(0));
        }
        
        freeColClient.getActionManager().update();
    }


    /**
     * Provides an opportunity to filter the messages delivered to the canvas.
     *
     * @param  message  the message that is candidate for delivery to the canvas
     * @return  true if the message should be delivered
     */
    private boolean shouldAllowMessage( ModelMessage message ) {

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

        nextActiveUnit(null);
    }




    /**
    * Convenience method: returns the first child element with the
    * specified tagname.
    *
    * @param element The <code>Element</code> to search for the child element.
    * @param tagName The tag name of the child element to be found.
    * @return The child of the given <code>Element</code> with the given
    *       <code>tagName</code> or <code>null</code> if no such child exists.
    */
    protected Element getChildElement(Element element, String tagName) {
        NodeList n = element.getChildNodes();
        for (int i=0; i<n.getLength(); i++) {
            if (((Element) n.item(i)).getTagName().equals(tagName)) {
                return (Element) n.item(i);
            }
        }

        return null;
    }
}


