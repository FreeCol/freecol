
package net.sf.freecol.client.control;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ConnectException;
import java.util.logging.Logger;
import java.util.Iterator;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.*;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.Message;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


import net.sf.freecol.server.FreeColServer;


/**
* The controller that will be used while the game is played.
*/
public final class InGameController {
    private static final Logger logger = Logger.getLogger(InGameController.class.getName());


    private FreeColClient freeColClient;



    /**
    * The constructor to use.
    * @param freeColClient The main controller.
     */
    public InGameController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
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

        String name = freeColClient.getCanvas().showInputDialog("nameColony.text", "", "nameColony.yes", "nameColony.no");

        if (name == null) { // The user cancelled the action.
            return;
        }

        Element buildColonyElement = Message.createNewRootElement("buildColony");
        buildColonyElement.setAttribute("name", name);
        buildColonyElement.setAttribute("unit", unit.getID());

        Element reply = client.ask(buildColonyElement);

        if (reply.getTagName().equals("buildColonyConfirmed")) {
            Colony colony = new Colony(game, (Element) reply.getChildNodes().item(0));
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
     * Moves the specified unit in a specified direction. This may result in an attack, move... action.
     *
     * @param unit The unit to be moved.
     * @param direction The direction in which to move the Unit.
     */
    public void move(Unit unit, int direction) {
        Game game = freeColClient.getGame();

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
            //case Unit.ATTACK:         attack(unit, direction); break;
            case Unit.DISEMBARK:        disembark(unit, direction); break;
            case Unit.EMBARK:           embark(unit, direction); break;
            case Unit.MOVE_HIGH_SEAS:   moveHighSeas(unit, direction); break;
            case Unit.ILLEGAL_MOVE:     /*if (sfxPlayer != null) {
                                            sfxPlayer.play(sfxLibrary.get(sfxLibrary.ILLEGAL_MOVE));
                                            break;
                                        }*/
                                        break;
            default:                    throw new RuntimeException("unrecognised move: " + move);
        }
    }


    /**
    * Actually moves a unit in a specified direction.
    *
    * @param unit The unit to be moved.
    * @param direction The direction in which to move the Unit.
    */
    private void reallyMove(Unit unit, int direction) {
        Game game = freeColClient.getGame();
        GUI gui = freeColClient.getGUI();
        Canvas canvas = freeColClient.getCanvas();
        Client client = freeColClient.getClient();

        unit.move(direction);

        if (unit.getTile().getSettlement() != null) {
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
    }


    /**
     * Disembarks the specified unit in a specified direction.
     *
     * @param unit The unit to be disembarked.
     * @param direction The direction in which to disembark the Unit.
     */
    private void disembark(Unit unit, int direction) {
        Canvas canvas = freeColClient.getCanvas();

        if (canvas.showConfirmDialog("disembark.text", "disembark.yes", "disembark.no")) {
            unit.setStateToAllChildren(Unit.ACTIVE);

            Iterator unitIterator = unit.getUnitIterator();

            while (unitIterator.hasNext()) {
                Unit u = (Unit) unitIterator.next();

                if ((u.getState() == Unit.ACTIVE) && u.getMovesLeft() > 0) {
                    reallyMove(u, direction);
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

        Tile destinationTile = game.getMap().getNeighbourOrNull(direction, unit.getTile());
        Unit destinationUnit = null;

        if (destinationTile.getUnitCount() == 1) {
            destinationUnit = destinationTile.getFirstUnit();
        } else {
            // TODO: Present the user with a choice:
            Iterator unitIterator = destinationTile.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit nextUnit = (Unit) unitIterator.next();
                if (nextUnit.getSpaceLeft() >= unit.getTakeSpace()) {
                    destinationUnit = nextUnit;
                    break;
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
    */
    public void boardShip(Unit unit, Unit carrier) {
        if (carrier == null) {
            throw new NullPointerException();
        }

        Client client = freeColClient.getClient();
        
        unit.boardShip(carrier);

        Element boardShipElement = Message.createNewRootElement("boardShip");
        boardShipElement.setAttribute("unit", unit.getID());
        boardShipElement.setAttribute("carrier", carrier.getID());

        client.send(boardShipElement);
    }


    /**
    * Leave a ship. This method should only be invoked if the ship is in a harbour.
    *
    * @param unit The unit who is going to leave the ship where it is located.
    * @param carrier The carrier.
    */
    public void leaveShip(Unit unit) {
        Client client = freeColClient.getClient();

        unit.leaveShip();

        Element leaveShipElement = Message.createNewRootElement("leaveShip");
        leaveShipElement.setAttribute("unit", unit.getID());

        client.send(leaveShipElement);
    }

    
    /**
    * Moves a <code>Unit</code> to a <code>WorkLocation</code>.
    *
    * @param unit The <code>Unit</code>.
    * @param workLocation The <code>WorkLocation</code>.
    */
    public void work(Unit unit, WorkLocation workLocation) {
        Client client = freeColClient.getClient();

        unit.work(workLocation);

        Element workElement = Message.createNewRootElement("work");
        workElement.setAttribute("unit", unit.getID());
        workElement.setAttribute("workLocation", workLocation.getID());

        client.send(workElement);
    }

    
    /**
    * Puts the specified unit outside the colony.
    * @param unit The <code>Unit</code>
    */
    public void putOutsideColony(Unit unit) {
        Client client = freeColClient.getClient();

        unit.putOutsideColony();

        Element putOutsideColonyElement = Message.createNewRootElement("putOutsideColony");
        putOutsideColonyElement.setAttribute("unit", unit.getID());

        client.send(putOutsideColonyElement);
    }


    /**
     * Moves the specified unit in the "high seas" in a specified direction.
     * This may result in an ordinary move or a move to europe.
     *
     * @param unit The unit to be moved.
     * @param direction The direction in which to move the Unit.
     */
    private void moveHighSeas(Unit unit, int direction) {
        Canvas canvas = freeColClient.getCanvas();
        Map map = freeColClient.getGame().getMap();

        if (unit.getTile() == null || map.getNeighbourOrNull(direction, unit.getTile()) == null
            || canvas.showConfirmDialog("highseas.text", "highseas.yes", "highseas.no")) {
            
            moveToEurope(unit);
            nextActiveUnit();            
        } else {
            reallyMove(unit, direction);
        }
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

        if (myPlayer.getGold() < Unit.getPrice(unitType)) {
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

        if (myPlayer.getGold() < europe.getRecruitPrice()) {
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

        unit.skip();
        nextActiveUnit();
    }


    /**
    * Centers the map on the selected tile.
    */
    public void centerActiveUnit() {
        GUI gui = freeColClient.getGUI();

        gui.setFocus(gui.getSelectedTile());
    }


    /**
    * Makes a new unit active.
    */    
    public void nextActiveUnit() {
        nextActiveUnit(null);
    }


    /**
    * Makes a new unit active.
    * @param tile The tile to select if no new unit can be made active.
    */
    public void nextActiveUnit(Tile tile) {
        GUI gui = freeColClient.getGUI();
        Player myPlayer = freeColClient.getMyPlayer();

        Unit nextActiveUnit = myPlayer.getNextActiveUnit();

        if (nextActiveUnit != null) {
            gui.setActiveUnit(nextActiveUnit);
        } else if (tile != null) {
            gui.setSelectedTile(tile.getPosition());
            gui.setActiveUnit(null);
        } else {
            gui.setActiveUnit(null);
        }
    }

    
    /**
    * End the turn.
    */
    public void endTurn() {
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();

        canvas.setEnabled(false);
        canvas.showStatusPanel("Waiting for the other players to complete their turn...");

        Element endTurnElement = Message.createNewRootElement("endTurn");
        client.send(endTurnElement);
    }
}
