
package net.sf.freecol.client.control;

import java.util.logging.Logger;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.*;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.FreeColMenuBar;
import net.sf.freecol.client.gui.sound.*;
import net.sf.freecol.client.gui.panel.EventPanel;

import net.sf.freecol.common.model.*;
import net.sf.freecol.common.networking.Message;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


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
    * Opens a dialog where the user should specify the filename
    * and saves the game.
    */
    public void saveGame() {    
        Canvas canvas = freeColClient.getCanvas();
        
        if (freeColClient.getMyPlayer().isAdmin() && freeColClient.getFreeColServer() != null) {               
            File file = canvas.showSaveDialog(FreeCol.getSaveDirectory());
            try {
                freeColClient.getFreeColServer().saveGame(file, freeColClient.getMyPlayer().getUsername());                              
            } catch (IOException e) {
                canvas.errorMessage("couldNotSaveGame");
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
        
        if(file == null || !canvas.showConfirmDialog("stopCurrentGame.text", "stopCurrentGame.yes", "stopCurrentGame.no")) {
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
    */
    public void setInDebugMode(boolean debug) {
        FreeCol.setInDebugMode(debug);
        freeColClient.getCanvas().setJMenuBar(new FreeColMenuBar(freeColClient, freeColClient.getCanvas(), freeColClient.getGUI()));
    }


    /**
    * Sends a public chat message.
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
                freeColClient.getInGameController().emigrateUnitInEurope((int) ((Math.random() * 3) + 1));
            }
            freeColClient.getInGameController().nextActiveUnit();
        }
    }


    /**
    * Removes the units we cannot see anymore from the map.
    */
    private void removeUnitsOutsideLOS() {
        Player player = freeColClient.getMyPlayer();
        Map map = freeColClient.getGame().getMap();

        Iterator tileIterator = map.getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile t = map.getTile((Map.Position) tileIterator.next());
            if (t != null && !player.canSee(t) && t.getFirstUnit() != null) {
                t.disposeAllUnits();
            }
        }
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
            if (reply.getElementsByTagName("update").getLength() > 0) {
                Element updateElement = (Element) reply.getElementsByTagName("update").item(0);
                freeColClient.getInGameInputHandler().update(updateElement);
            }

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
            case Unit.ATTACK:           attack(unit, direction); break;
            case Unit.DISEMBARK:        disembark(unit, direction); break;
            case Unit.EMBARK:           embark(unit, direction); break;
            case Unit.MOVE_HIGH_SEAS:   moveHighSeas(unit, direction); break;
            case Unit.ILLEGAL_MOVE:     freeColClient.playSound(SfxLibrary.ILLEGAL_MOVE); break;
            default:                    throw new RuntimeException("unrecognised move: " + move);
        }

        freeColClient.getCanvas().getMapControls().update();
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
        Game game = freeColClient.getGame();
        Map map = game.getMap();

        if (unit.getType() == Unit.ARTILLERY || unit.getType() == Unit.DAMAGED_ARTILLERY) {
            freeColClient.playSound(SfxLibrary.ARTILLERY);
        }

        Element attackElement = Message.createNewRootElement("attack");
        attackElement.setAttribute("unit", unit.getID());
        attackElement.setAttribute("direction", Integer.toString(direction));

        // Get the result of the attack from the server:
        Element attackResultElement = client.ask(attackElement);

        int result = Integer.parseInt(attackResultElement.getAttribute("result"));

        // If a successful attack against a colony, we need to update the tile:
        Element utElement = getChildElement(attackResultElement, Tile.getXMLElementTagName());
        if (utElement != null) {
            Tile updateTile = (Tile) game.getFreeColGameObject(utElement.getAttribute("ID"));
            updateTile.readFromXMLElement(utElement);
        }

        Unit defender = map.getNeighbourOrNull(direction, unit.getTile()).getDefendingUnit(unit);

        if (result == Unit.ATTACKER_LOSS) {
            unit.loseAttack();
        } else {
            unit.winAttack(defender);
        }

        Element updateElement = (Element) attackResultElement.getElementsByTagName("update").item(0);
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
    * @param unit The goods which are going to leave the ship where it is located.
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
    * Buys goods in Europe.
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
            int maxAmount = carrier.getGoodsContainer().getGoodsCount(type);
            while (maxAmount > 100) {
                maxAmount -= 100;
            }
            maxAmount = 100 - maxAmount;

            amount = (amount > maxAmount) ? maxAmount : amount;
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

        client.send(sellGoodsElement);
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
    * @param unit The <code>Unit</code>
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
    * @param typ The new type of building to build.
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
    * @param occupation The state of the unit.
    */
    public void changeState(Unit unit, int state) {
        Client client = freeColClient.getClient();

        if (!(unit.checkSetState(state))) {
            logger.warning("Can't set state " + state);
            return; // Don't bother.
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
    }


    /**
    * Cause a unit to emigrate from a specified "slot" in Europe.
    * @param slot The slot from which the unit emigrates. Either 1, 2 or 3.
    */
    public void emigrateUnitInEurope(int slot) {
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        Game game = freeColClient.getGame();
        Player myPlayer = freeColClient.getMyPlayer();
        Europe europe = myPlayer.getEurope();

        Element emigrateUnitInEuropeElement = Message.createNewRootElement("emigrateUnitInEurope");
        emigrateUnitInEuropeElement.setAttribute("slot", Integer.toString(slot));

        Element reply = client.ask(emigrateUnitInEuropeElement);
        
        if (!reply.getTagName().equals("emigrateUnitInEuropeConfirmed")) {
            throw new IllegalStateException();
        }

        slot = Integer.parseInt(reply.getAttribute("slot")); // The server may have changed the slot.
        Unit unit = new Unit(game, (Element) reply.getChildNodes().item(0));
        int newRecruitable = Integer.parseInt(reply.getAttribute("newRecruitable"));
        europe.emigrate(slot, unit, newRecruitable);
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
        } else if (tile != null) {
            gui.setSelectedTile(tile.getPosition());
            gui.setActiveUnit(null);
        } else {
            gui.setActiveUnit(null);
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
        canvas.showStatusPanel("Waiting for the other players to complete their turn...");

        Element endTurnElement = Message.createNewRootElement("endTurn");
        client.send(endTurnElement);
    }
    
    
    

    /**
    * Convenience method: returns the first child element with the
    * specified tagname.
    *
    * @param element The <code>Element</code> to search for the child element.
    * @param tagName The tag name of the child element to be found.
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
