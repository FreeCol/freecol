
package net.sf.freecol.client.control;

import java.util.logging.Logger;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.FreeColMenuBar;
import net.sf.freecol.client.gui.sound.*;
import net.sf.freecol.client.gui.panel.EventPanel;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.FreeColDialog;

import net.sf.freecol.common.model.*;
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

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final FreeColClient freeColClient;


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

        if (freeColClient.getMyPlayer().isAdmin() && freeColClient.getFreeColServer() != null) {
            final File file = canvas.showSaveDialog(FreeCol.getSaveDirectory());

            if (file != null) {
                canvas.showStatusPanel(Messages.message("status.savingGame"));
                Thread t = new Thread() {
                    public void run() {
                        try {
                            freeColClient.getFreeColServer().saveGame(file, freeColClient.getMyPlayer().getUsername());
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    canvas.closeStatusPanel();
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
        }
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
    * {@link FreeCol#setInDebugMode} and reinitialize the
    * <code>FreeColMenuBar</code>.
    * 
    * @param debug Should be set to <code>true</code> in order
    *       to enable debug mode.
    */
    public void setInDebugMode(boolean debug) {
        FreeCol.setInDebugMode(debug);
        freeColClient.getCanvas().setJMenuBar(new FreeColMenuBar(freeColClient, freeColClient.getCanvas(), freeColClient.getGUI()));
        freeColClient.getCanvas().updateJMenuBar();
    }


    /**
    * Sends a public chat message.
    * @param message The chat message.
    */
    public void sendChat(String message) {
        Element chatElement = Message.createNewRootElement("chat");
        chatElement.setAttribute("message", message);
        chatElement.setAttribute("privateChat", "false");
        freeColClient.getClient().send(chatElement);
    }


    /**
    * Sets <code>player</code> as the new <code>currentPlayer</code> of the game.
    * @param currentPlayer The player.
    */
    public void setCurrentPlayer(Player currentPlayer) {
        Game game = freeColClient.getGame();

        game.setCurrentPlayer(currentPlayer);

        if (freeColClient.getMyPlayer().equals(currentPlayer)) {
            removeUnitsOutsideLOS();
            freeColClient.getCanvas().setEnabled(true);
            freeColClient.getCanvas().closeMenus();
            if (currentPlayer.checkEmigrate()) {
                emigrateUnitInEurope(
                        currentPlayer.hasFather(FoundingFather.WILLIAM_BREWSTER)
                        ? freeColClient.getCanvas().showEmigrationPanel() : 0
                    );
            }

            nextActiveUnit();
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
        Client client = freeColClient.getClient();
        Game game = freeColClient.getGame();
        GUI gui = freeColClient.getGUI();

        Unit unit = freeColClient.getGUI().getActiveUnit();

        if (unit == null) {
            return;
        }

        if (!unit.canBuildColony()) {
            return;
        }

        String name = freeColClient.getCanvas().showInputDialog("nameColony.text", 
                freeColClient.getMyPlayer().getDefaultColonyName(), "nameColony.yes", "nameColony.no");

        if (name == null) { // The user cancelled the action.
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

            Colony colony = new Colony(game, (Element) reply.getChildNodes().item(0));
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
        Player player = freeColClient.getMyPlayer();
        Map map = freeColClient.getGame().getMap();
        boolean naval = unit.isNaval();
        Iterator colonyIterator = player.getColonyIterator();
        ArrayList destinations = new ArrayList();
        if (naval) {
            destinations.add(player.getEurope());
        }
        while (colonyIterator.hasNext()) {
            Colony colony = (Colony) colonyIterator.next();
            if (naval && colony.isLandLocked()) {
                continue;
            } else if (unit.getTile() == colony.getTile()) {
                continue;
            }
            // TODO: check if unit can really reach colony
            destinations.add(colony);
        }

        Canvas canvas = freeColClient.getCanvas();
        Object destination = canvas.showChoiceDialog(Messages.message("selectDestination.text"),
                                                     Messages.message("selectDestination.cancel"),
                                                     destinations.toArray());

        unit.setDestination((Location) destination);
        if (destination == null) {
            // user aborted
            return;
        } else if (destination instanceof Europe) {
            PathNode path = map.findPathToEurope(unit, unit.getTile());
            if (path == null) {
                canvas.showInformationMessage("selectDestination.failed",
                                              new String [][] {{"%destination%",
                                                                ((Europe) destination).toString()}});
            } else {
                moveAlongPath(path);
            }
        } else if (destination instanceof Colony) {
            PathNode path = map.findPath(unit, unit.getTile(), ((Colony) destination).getTile());
            if (path == null) {
                canvas.showInformationMessage("selectDestination.failed",
                                              new String [][] {{"%destination%",
                                                                ((Colony) destination).getName()}});
            } else {
                moveAlongPath(path);
            }
        } else {
            throw new IllegalArgumentException("Unknown type of destination");
        }
    }

    /**
     * Moves the active unit along a given path. The path may be the
     * result of a unit drag or of selecting a destination.
     *
     * @param path The path to follow.
     */
    public void moveAlongPath(PathNode path) {
        Unit unit = freeColClient.getGUI().getActiveUnit();

        if (unit != null && path != null) {
            unit.setPath(path);
            // only for the client
            unit.setState(Unit.GOING_TO);
            unit.setStateToAllChildren(Unit.SENTRY);
            moveAlongPath(unit);
        } // else: nothing: There is no active unit that can be moved.
    }
        

    /**
     * Moves the given unit along the unit's path.
     *
     * @param unit The unit to move.
     */
    public void moveAlongPath(Unit unit) {
        Map map = freeColClient.getGame().getMap();
        PathNode path = unit.getPath();
        boolean active = false;
        while (unit.getMovesLeft() >= 0) {
            if (path == null) {
                if (unit.getDestination() instanceof Europe) {
                    unit.setDestination(null);
                    unit.setPath(null);
                    moveToEurope(unit);
                } else {
                    active = true;
                }
                break;
            }                
            int direction = path.getDirection();
            Tile target = map.getNeighbourOrNull(direction, unit.getTile());
            int moveCost = unit.getMoveCost(target);
            if (moveCost > unit.getMovesLeft()) {
                // we can't go there now, but we don't want to wake up
                unit.setMovesLeft(0);
                logger.info("Setting moves to zero.");
                break;
            } else {
                int moveType = unit.getMoveType(target);
                if (moveType == Unit.MOVE ||
                    moveType == Unit.MOVE_HIGH_SEAS) {
                    reallyMove(unit, direction);
                    path = path.next;
                    freeColClient.getCanvas().refresh();
                } else {
                    // something has got in our way
                    logger.info("Aborting goto: move type was " + moveType);
                    active = true;
                    break;
                }
            }
        }
        unit.setPath(path);
        freeColClient.getActionManager().update();
        freeColClient.getCanvas().updateJMenuBar();

        if (active) {
            // only for the client
            unit.setState(Unit.ACTIVE);
            unit.setPath(null);
            unit.setDestination(null);
            freeColClient.getGUI().setActiveUnit(unit);
        } else {
            // only for the client
            if (path != null) {
                unit.setState(Unit.GOING_TO);
            }
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
                                        exploreLostCityRumour(unit, direction); break;
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
    }


    /**
    * Buys the given land from the indians.
    * @param tile The land which should be bought from the indians. 
    */
    public void buyLand(Tile tile) {   
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
        Client client = freeColClient.getClient();

        Element tradeElement = Message.createNewRootElement("trade");
        tradeElement.setAttribute("unit", unit.getID());
        tradeElement.setAttribute("settlement", settlement.getID());
        tradeElement.setAttribute("gold", Integer.toString(gold));
        tradeElement.appendChild(goods.toXMLElement(null, tradeElement.getOwnerDocument()));

        client.send(tradeElement);

        unit.trade(settlement, goods, gold);
        
        freeColClient.getCanvas().updateGoldLabel();
    }


    /**
    * Trades the given goods. The goods gets transferred
    * from the given <code>Unit</code> to the given <code>Settlement</code>.
    */
    private void deliverGiftToSettlement(Unit unit, Settlement settlement, Goods goods) {
        Client client = freeColClient.getClient();

        Element deliverGiftElement = Message.createNewRootElement("deliverGift");
        deliverGiftElement.setAttribute("unit", unit.getID());
        deliverGiftElement.setAttribute("settlement", settlement.getID());
        deliverGiftElement.appendChild(goods.toXMLElement(null, deliverGiftElement.getOwnerDocument()));

        client.send(deliverGiftElement);

        unit.deliverGift(settlement, goods);
    }

    /**
     * Explores a lost city rumour.
     *
     * @param unit The unit to be moved.
     * @param direction The direction in which to move the Unit.
     */
    private void exploreLostCityRumour(Unit unit, int direction) {
        Client client = freeColClient.getClient();
        // first, really move in that direction
        reallyMove(unit, direction);

        // next, see what we find there
        Element exploreElement = Message.createNewRootElement("explore");
        exploreElement.setAttribute("unit", unit.getID());

        Element reply = client.ask(exploreElement);
        freeColClient.getInGameInputHandler().handle(client.getConnection(), reply);

        nextModelMessage();
    }

    /**
     * Transfers the gold carried by this unit to the {@link Player owner}.
     *
     * @exception IllegalStateException if this unit is not a treasure train.
     *                                  or if it cannot be cashed in at it's current
     *                                  location.
     */
    private void cashInTreasureTrain(Unit unit) {
        Client client = freeColClient.getClient();

        if (unit.getType() != Unit.TREASURE_TRAIN) {
            throw new IllegalStateException("Not a treasure train");
        }

        // Inform the server:
        Element cashInTreasureTrainElement = Message.createNewRootElement("cashInTreasureTrain");
        cashInTreasureTrainElement.setAttribute("unit", unit.getID());

        client.send(cashInTreasureTrainElement);

        unit.cashInTreasureTrain();
        
        freeColClient.getCanvas().updateGoldLabel();

        nextModelMessage();
    }


    /**
    * Actually moves a unit in a specified direction.
    *
    * @param unit The unit to be moved.
    * @param direction The direction in which to move the Unit.
    */
    private void reallyMove(Unit unit, int direction) {
        GUI gui = freeColClient.getGUI();
        Canvas canvas = freeColClient.getCanvas();
        Client client = freeColClient.getClient();

        unit.move(direction);

        if (unit.getTile().isLand() && unit.getOwner().getNewLandName() == null) {
            String newLandName = canvas.showInputDialog("newLand.text", unit.getOwner().getDefaultNewLandName(), "newLand.yes", null);
            unit.getOwner().setNewLandName(newLandName);
            Element setNewLandNameElement = Message.createNewRootElement("setNewLandName");
            setNewLandNameElement.setAttribute("newLandName", newLandName);
            client.send(setNewLandNameElement);
            canvas.showEventDialog(EventPanel.FIRST_LANDING);
        }

        if (unit.getTile().getSettlement() != null && unit.isCarrier()) {
            canvas.showColonyPanel((Colony) unit.getTile().getSettlement());
        } else if (unit.getMovesLeft() > 0) {
            gui.setActiveUnit(unit);
        } else {
            nextActiveUnit(unit.getTile());
        }

        // Inform the server:
        Element moveElement = Message.createNewRootElement("move");
        moveElement.setAttribute("unit", unit.getID());
        moveElement.setAttribute("direction", Integer.toString(direction));

        //client.send(moveElement);
        Element reply = client.ask(moveElement);
        freeColClient.getInGameInputHandler().handle(client.getConnection(), reply);

        nextModelMessage();
    }


    /**
    * Performs an attack in a specified direction. Note that the server
    * handles the attack calculations here.
    *
    * @param unit The unit to perform the attack.
    * @param direction The direction in which to attack.
    */
    private void attack(Unit unit, int direction) {
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        Game game = freeColClient.getGame();
        Map map = game.getMap();
        Player enemy;
        Tile target = game.getMap().getNeighbourOrNull(direction, unit.getTile());
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
        if (unit.getType() == Unit.PRIVATEER) {
            enemy.setAttackedByPrivateers();
        } else {
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
            // make sure we are at war
            unit.getOwner().setStance(enemy, Player.WAR);

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

        // Get the defender:
        Element unitElement = getChildElement(attackResultElement, Unit.getXMLElementTagName());
        if (unitElement != null) {
            defender = new Unit(game, unitElement);
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
        if (!defender.isDisposed() && (defender.getLocation() == null || !defender.isVisibleTo(freeColClient.getMyPlayer()))) {
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

        client.send(embarkElement);
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

            client.send(boardShipElement);

            return true;
        } else {
            return false;
        }
    }


    /**
    * Clear the speciality of a <code>Unit</code>. That is, makes it a
    * <code>Unit.FREE_COLONIST</code>.
    *
    * @param unit The <code>Unit</code> to clear the speciality of.
    */
    public void clearSpeciality(Unit unit) {
        Client client = freeColClient.getClient();

        Element clearSpecialityElement = Message.createNewRootElement("clearSpeciality");
        clearSpecialityElement.setAttribute("unit", unit.getID());

        unit.clearSpeciality();

        client.send(clearSpecialityElement);
    }


    /**
    * Leave a ship. This method should only be invoked if the ship is in a harbour.
    *
    * @param unit The unit who is going to leave the ship where it is located.
    */
    public void leaveShip(Unit unit) {
        Client client = freeColClient.getClient();

        unit.leaveShip();

        Element leaveShipElement = Message.createNewRootElement("leaveShip");
        leaveShipElement.setAttribute("unit", unit.getID());

        client.send(leaveShipElement);
    }

    /**
    * Loads a cargo onto a carrier.
    *
    * @param goods The goods which are going aboard the carrier.
    * @param carrier The carrier.
    */
    public void loadCargo(Goods goods, Unit carrier) {
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

        client.send(loadCargoElement);
    }


    /**
    * Unload cargo. This method should only be invoked if the unit carrying the
    * cargo is in a harbour.
    *
    * @param goods The goods which are going to leave the ship where it is located.
    */
    public void unloadCargo(Goods goods) {
        Client client = freeColClient.getClient();

        goods.adjustAmount();

        Element unloadCargoElement = Message.createNewRootElement("unloadCargo");
        unloadCargoElement.appendChild(goods.toXMLElement(freeColClient.getMyPlayer(), unloadCargoElement.getOwnerDocument()));

        goods.unload();

        client.send(unloadCargoElement);
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

        client.send(buyGoodsElement);
    }


    /**
    * Sells goods in Europe.
    *
    * @param goods The goods to be sold.
    */
    public void sellGoods(Goods goods) {
        Client client = freeColClient.getClient();
        Player player = freeColClient.getMyPlayer();

        freeColClient.playSound(SfxLibrary.SELL_CARGO);

        goods.adjustAmount();

        Element sellGoodsElement = Message.createNewRootElement("sellGoods");
        sellGoodsElement.appendChild(goods.toXMLElement(freeColClient.getMyPlayer(), sellGoodsElement.getOwnerDocument()));

        player.getGame().getMarket().sell(goods, player);
        freeColClient.getCanvas().updateGoldLabel();

        client.send(sellGoodsElement);
    }

    /**
     * Toggles the export settings of the custom house.
     *
     * @param colony The colony with the custom house.
     * @param goods The goods for which to toggle the settings.
     */
    public void toggleExports(Colony colony, Goods goods) {
        Client client = freeColClient.getClient();

        Element toggleExportsElement = Message.createNewRootElement("toggleExports");
        toggleExportsElement.setAttribute("colony", colony.getID());
        toggleExportsElement.setAttribute("goods", String.valueOf(goods.getType()));

        colony.toggleExports(goods);
        client.send(toggleExportsElement);
    }
        
    
    /**
    * Equips or unequips a <code>Unit</code> with a certain type of <code>Goods</code>.
    *
    * @param unit The <code>Unit</code>.
    * @param type The type of <code>Goods</code>.
    * @param amount How many of these goods the unit should have.
    */
    public void equipUnit(Unit unit, int type, int amount) {
        Client client = freeColClient.getClient();

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
                unit.setArmed((amount > 0)); // So give them muskets if the amount we want is greater than zero.
                break;
            case Goods.HORSES:
                unit.setMounted((amount > 0)); // As above.
                break;
            case Goods.TOOLS:
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

        client.send(equipUnitElement);

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
        Client client = freeColClient.getClient();

        Element workElement = Message.createNewRootElement("work");
        workElement.setAttribute("unit", unit.getID());
        workElement.setAttribute("workLocation", workLocation.getID());

        unit.work(workLocation);

        client.send(workElement);
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
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();

        if (skipCheck || unit.getTile().getColony().getUnitCount() > 1 || !(unit.getLocation() instanceof Colony ||
                unit.getLocation() instanceof Building || unit.getLocation() instanceof ColonyTile)
                || canvas.showConfirmDialog("abandonColony.text", "abandonColony.yes", "abandonColony.no")) {

            Element putOutsideColonyElement = Message.createNewRootElement("putOutsideColony");
            putOutsideColonyElement.setAttribute("unit", unit.getID());
            unit.putOutsideColony();
            client.send(putOutsideColonyElement);

            return true;
        } else {
            return false;
        }
    }

    /**
    * Changes the work type of this <code>Unit</code>.
    * @param unit The <code>Unit</code>
    * @param workType The new work type.
    */
    public void changeWorkType(Unit unit, int workType) {
        Client client = freeColClient.getClient();

        Element changeWorkTypeElement = Message.createNewRootElement("changeWorkType");
        changeWorkTypeElement.setAttribute("unit", unit.getID());
        changeWorkTypeElement.setAttribute("workType", Integer.toString(workType));

        unit.setWorkType(workType);

        client.send(changeWorkTypeElement);
    }

    /**
    * Changes the current construction project of a <code>Colony</code>.
    * @param colony The <code>Colony</code>
    * @param type The new type of building to build.
    */
    public void setCurrentlyBuilding(Colony colony, int type) {
        Client client = freeColClient.getClient();

        colony.setCurrentlyBuilding(type);

        Element setCurrentlyBuildingElement = Message.createNewRootElement("setCurrentlyBuilding");
        setCurrentlyBuildingElement.setAttribute("colony", colony.getID());
        setCurrentlyBuildingElement.setAttribute("type", Integer.toString(type));

        client.send(setCurrentlyBuildingElement);
    }


    /**
    * Changes the state of this <code>Unit</code>.
    * @param unit The <code>Unit</code>
    * @param state The state of the unit.
    */
    public void changeState(Unit unit, int state) {
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
                    } else {
                        buyLand(unit.getTile());
                    }
                }
            }
        }

        Element changeStateElement = Message.createNewRootElement("changeState");
        changeStateElement.setAttribute("unit", unit.getID());
        changeStateElement.setAttribute("state", Integer.toString(state));

        unit.setState(state);

        if (unit.getMovesLeft() == 0) {
            nextActiveUnit();
        } else {
            freeColClient.getCanvas().refresh();
        }

        client.send(changeStateElement);
    }


    /**
    * Clears the orders of the given <code>Unit</code>.
    * The orders are cleared by making the unit {@link Unit#ACTIVE}.
    *
    * @param unit The <code>Unit</code>.
    */
    public void clearOrders(Unit unit) {
        if (unit == null) {
            return;
        }
        // affect client only
        unit.setDestination(null);
        unit.setPath(null);
        changeState(unit, Unit.ACTIVE);
    }


    /**
     * Moves the specified unit in the "high seas" in a specified direction.
     * This may result in an ordinary move, no move or a move to europe.
     *
     * @param unit The unit to be moved.
     * @param direction The direction in which to move the Unit.
     */
    private void moveHighSeas(Unit unit, int direction) {
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
            }
            else {
                Element learnSkill = Message.createNewRootElement("learnSkillAtSettlement");
                learnSkill.setAttribute("unit", unit.getID());
                learnSkill.setAttribute("direction", Integer.toString(direction));

                if (canvas.showConfirmDialog("learnSkill.text",
                                             "learnSkill.yes",
                                             "learnSkill.no",
                                             new String [][] {{"%replace%", skillName}})) {
                    unit.setType(skill);
                    settlement.setLearnableSkill(IndianSettlement.NONE);
                }
                else {
                    learnSkill.setAttribute("action", "cancel");
                }

                client.send(learnSkill);
            }
        }
        else {
            canvas.errorMessage("indianSettlement.noMoreSkill");
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
            settlement.setHighlyWantedGoods(Integer.parseInt(reply.getAttribute("highlyWantedGoods")));
            settlement.setWantedGoods1(Integer.parseInt(reply.getAttribute("wantedGoods1")));
            settlement.setWantedGoods2(Integer.parseInt(reply.getAttribute("wantedGoods2")));
        } else {
            logger.warning("Server gave an invalid reply to an askSkill message");
            return;
        }

        int userAction = canvas.showScoutIndianSettlementDialog(settlement);

        switch (userAction) {
            case FreeColDialog.SCOUT_INDIAN_SETTLEMENT_ATTACK:
                scoutMessage.setAttribute("action", "attack");
                client.send(scoutMessage);
                attack(unit, direction);
            case FreeColDialog.SCOUT_INDIAN_SETTLEMENT_CANCEL:
                scoutMessage.setAttribute("action", "cancel");
                client.send(scoutMessage);
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
            if (action.equals("speak") && result.equals("tales")) {
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
            else if (action.equals("speak") && result.equals("die")) {
                unit.dispose();
                canvas.showInformationMessage("scoutSettlement.speakDie");
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
            logger.warning("Server gave an invalid reply to an askSkill message");
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
                client.send(missionaryMessage);
                break;
            case FreeColDialog.MISSIONARY_ESTABLISH:
                missionaryMessage.setAttribute("action", "establish");
                settlement.setMissionary(unit);
                client.send(missionaryMessage);
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

                    client.send(inciteMessage);
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
        Client client = freeColClient.getClient();

        unit.moveToEurope();

        Element moveToEuropeElement = Message.createNewRootElement("moveToEurope");
        moveToEuropeElement.setAttribute("unit", unit.getID());

        client.send(moveToEuropeElement);
    }


    /**
     * Moves the specified unit to America.
     * @param unit The unit to be moved to America.
     */
    public void moveToAmerica(Unit unit) {
        Client client = freeColClient.getClient();

        unit.moveToAmerica();

        Element moveToAmericaElement = Message.createNewRootElement("moveToAmerica");
        moveToAmericaElement.setAttribute("unit", unit.getID());

        client.send(moveToAmericaElement);
    }


    /**
    * Trains a unit of a specified type in Europe.
    * @param unitType The type of unit to be trained.
    */
    public void trainUnitInEurope(int unitType) {
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        Game game = freeColClient.getGame();
        Player myPlayer = freeColClient.getMyPlayer();
        Europe europe = myPlayer.getEurope();

        if (unitType != Unit.ARTILLERY && myPlayer.getGold() < Unit.getPrice(unitType) ||
                myPlayer.getGold() < europe.getArtilleryPrice()) {
            canvas.errorMessage("notEnoughGold");
            return;
        }

        Element trainUnitInEuropeElement = Message.createNewRootElement("trainUnitInEurope");
        trainUnitInEuropeElement.setAttribute("unitType", Integer.toString(unitType));

        Element reply = client.ask(trainUnitInEuropeElement);
        if (reply.getTagName().equals("trainUnitInEuropeConfirmed")) {
            Unit unit = new Unit(game, (Element) reply.getChildNodes().item(0));
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

        freeColClient.getClient().send(payForBuildingElement);
    }

    /**
    * Recruit a unit from a specified "slot" in Europe.
    * @param slot The slot to recruit the unit from. Either 1, 2 or 3.
    */
    public void recruitUnitInEurope(int slot) {
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
            Unit unit = new Unit(game, (Element) reply.getChildNodes().item(0));
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
        Client client = freeColClient.getClient();
        Game game = freeColClient.getGame();
        Player myPlayer = freeColClient.getMyPlayer();
        Europe europe = myPlayer.getEurope();

        Element emigrateUnitInEuropeElement = Message.createNewRootElement("emigrateUnitInEurope");
        if (myPlayer.hasFather(FoundingFather.WILLIAM_BREWSTER)) {
            emigrateUnitInEuropeElement.setAttribute("slot", Integer.toString(slot));
        }

        Element reply = client.ask(emigrateUnitInEuropeElement);

        if (!reply.getTagName().equals("emigrateUnitInEuropeConfirmed")) {
            throw new IllegalStateException();
        }

        if (!myPlayer.hasFather(FoundingFather.WILLIAM_BREWSTER)) {
            slot = Integer.parseInt(reply.getAttribute("slot"));
        }

        Unit unit = new Unit(game, (Element) reply.getChildNodes().item(0));
        int newRecruitable = Integer.parseInt(reply.getAttribute("newRecruitable"));
        europe.emigrate(slot, unit, newRecruitable);

        freeColClient.getCanvas().updateGoldLabel();
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
        Client client = freeColClient.getClient();
        Game game = freeColClient.getGame();
        Player player = freeColClient.getMyPlayer();

        int arrears = player.getArrears(type);
        if (player.getGold() >= arrears) {
            if (freeColClient.getCanvas().
                showConfirmDialog("model.europe.payArrears",
                                  "ok", "cancel",
                                  new String [][] {{"%replace%", String.valueOf(arrears)}})) {
                player.modifyGold(-arrears);
                freeColClient.getCanvas().updateGoldLabel();
                freeColClient.getCanvas().getEuropePanel().updateGoldLabel();
                player.setArrears(type, 0);
                // send to server
                Element payArrearsElement = Message.createNewRootElement("payArrears");
                payArrearsElement.setAttribute("type", String.valueOf(type));
                client.send(payArrearsElement);        
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
        GUI gui = freeColClient.getGUI();

        Unit unit = gui.getActiveUnit();

        if (unit != null) {
            unit.skip();
        }

        nextActiveUnit();
    }

    /**
     * Disbands the active unit.
     */
    public void disbandActiveUnit() {
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

        nextActiveUnit();

        client.send(disbandUnit);
    }


    /**
    * Centers the map on the selected tile.
    */
    public void centerActiveUnit() {
        GUI gui = freeColClient.getGUI();

        if (gui.getActiveUnit() != null && gui.getActiveUnit().getTile() != null) {
            gui.setFocus(gui.getActiveUnit().getTile().getPosition());
        }
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
        nextModelMessage();

        GUI gui = freeColClient.getGUI();
        Player myPlayer = freeColClient.getMyPlayer();

        Unit nextActiveUnit = myPlayer.getNextActiveUnit();

        if (nextActiveUnit != null) {
            gui.setActiveUnit(nextActiveUnit);
        } else {
            // no more active units, so we can move the others
            nextActiveUnit = myPlayer.getNextGoingToUnit();
            if (nextActiveUnit != null) {
                moveAlongPath(nextActiveUnit);
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


    /**
    * Displays the next <code>ModelMessage</code>.
    * @see net.sf.freecol.common.model.ModelMessage ModelMessage
    */
    public void nextModelMessage() {
        Canvas canvas = freeColClient.getCanvas();

        ArrayList messages = new ArrayList();

        Iterator i = freeColClient.getGame().getModelMessageIterator(freeColClient.getMyPlayer());
        if (i.hasNext()) {
            ModelMessage first = (ModelMessage) i.next();
            first.setBeenDisplayed(true);
            messages.add(first);
            while (i.hasNext()) {
                ModelMessage m = (ModelMessage) i.next();
                if (m.getSource() == first.getSource()) {
                    m.setBeenDisplayed(true);
                    boolean unique = true;
                    for (int j=0; j<messages.size(); j++) {
                        if (messages.get(j).equals(m)) {
                            unique = false;
                            break;
                        }
                    }

                    if (unique) {
                        messages.add(m);
                    }
                }
            }

        }

        if (messages.size() > 0) {
            for (int j=0; j<messages.size(); j++) {
                canvas.showModelMessage((ModelMessage) messages.get(j));
            }
        }
    }


    /**
    * End the turn.
    */
    public void endTurn() {
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();

        canvas.setEnabled(false);
        canvas.showStatusPanel(Messages.message("waitingForOtherPlayers"));

        Element endTurnElement = Message.createNewRootElement("endTurn");
        client.send(endTurnElement);
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

