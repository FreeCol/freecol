
package net.sf.freecol.client.control;

import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.FreeColGameObject;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;

import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import java.util.logging.Logger;
import java.util.Iterator;



/**
* Handles the network messages that arrives while in the game.
*/
public final class InGameInputHandler extends InputHandler {
    private static final Logger logger = Logger.getLogger(InGameInputHandler.class.getName());



    /**
    * The constructor to use.
    * @param freeColClient The main controller.
    */
    public InGameInputHandler(FreeColClient freeColClient) {
        super(freeColClient);
    }





    /**
    * Deals with incoming messages that have just been received.
    *
    * @param connection The <code>Connection</code> the message was received on.
    * @param element The root element of the message.
    */
    public synchronized Element handle(Connection connection, Element element) {
        Element reply = null;

        if (element != null) {

            String type = element.getTagName();

            if (type.equals("update")) {
                reply = update(element);
            } else if (type.equals("remove")) {
                reply = remove(element);
            } else if (type.equals("opponentMove")) {
                reply = opponentMove(element);
            } else if (type.equals("opponentAttack")) {
                reply = opponentAttack(element);
            } else if (type.equals("setCurrentPlayer")) {
                reply = setCurrentPlayer(element);
            } else if (type.equals("emigrateUnitInEuropeConfirmed")) {
                reply = emigrateUnitInEuropeConfirmed(element);
            } else if (type.equals("newTurn")) {
                reply = newTurn(element);
            } else if (type.equals("setDead")) {
                reply = setDead(element);
            } else if (type.equals("createUnit")) {
                reply = createUnit(element);
            } else if (type.equals("gameEnded")) {
                reply = gameEnded(element);
            } else if (type.equals("chat")) {
                reply = chat(element);
            } else if (type.equals("disconnect")) {
                reply = disconnect(element);
            } else if (type.equals("error")) {
                reply = error(element);
            } else {
                logger.warning("Message is of unsupported type \"" + type + "\".");
            }
        }

        return reply;
    }


    /**
    * Handles an "update"-message.
    *
    * @param updateElement The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element update(Element updateElement) {
        Game game = getFreeColClient().getGame();

        NodeList nodeList = updateElement.getChildNodes();
        for (int i=0; i<nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            FreeColGameObject fcgo = game.getFreeColGameObject(element.getAttribute("ID"));

            if (fcgo != null) {
                fcgo.readFromXMLElement(element);
            } else {
                logger.warning("Could not find 'FreeColGameObject' with ID: " + element.getAttribute("ID"));
            }
        }

        // TODO: Refresh only the updated tiles:
        getFreeColClient().getCanvas().refresh();

        return null;
    }


    /**
    * Handles a "remove"-message.
    *
    * @param removeElement The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element remove(Element removeElement) {
        Game game = getFreeColClient().getGame();

        NodeList nodeList = removeElement.getChildNodes();
        for (int i=0; i<nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            FreeColGameObject fcgo = game.getFreeColGameObject(element.getAttribute("ID"));

            if (fcgo != null) {
                fcgo.dispose();
            } else {
                logger.warning("Could not find 'FreeColGameObject' with ID: " + element.getAttribute("ID"));
            }
        }

        // TODO: Refresh only the updated tiles:
        getFreeColClient().getCanvas().refresh();
        return null;
    }


    /**
    * Handles an "opponentMove"-message.
    *
    * @param opponentMoveElement The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element opponentMove(Element opponentMoveElement) {
        Game game = getFreeColClient().getGame();
        Map map = game.getMap();

        Player currentPlayer = getFreeColClient().getMyPlayer();

        int direction = Integer.parseInt(opponentMoveElement.getAttribute("direction"));

        if (!opponentMoveElement.hasAttribute("tile")) {
            Unit unit = (Unit) game.getFreeColGameObject(opponentMoveElement.getAttribute("unit"));

            if (unit == null) {
                //throw new NullPointerException();
                logger.warning("Could not find the 'unit' in 'opponentMove'.");
                return null;
            }

            if ((unit.getTile() != null) && (currentPlayer.canSee(map.getNeighbourOrNull(direction, unit.getTile())))) {
                unit.move(direction);
            } else {
                unit.dispose();
            }
        } else {
            String tileID = opponentMoveElement.getAttribute("tile");

            NodeList nl = opponentMoveElement.getElementsByTagName(Unit.getXMLElementTagName());
            Unit unit = new Unit(game, (Element) nl.item(0));

            if (game.getFreeColGameObject(tileID) == null) {
                logger.warning("Could not find tile with id: " + tileID);
            }
            unit.setLocation((Tile) game.getFreeColGameObject(tileID));
        }

        return null;
    }


    /**
    * Handles an "opponentAttack"-message.
    *
    * @param opponentAttackElement The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element opponentAttack(Element opponentAttackElement) {
        Game game = getFreeColClient().getGame();
        Map map = game.getMap();

        int direction = Integer.parseInt(opponentAttackElement.getAttribute("direction"));
        int result = Integer.parseInt(opponentAttackElement.getAttribute("result"));

        Unit unit = (Unit) game.getFreeColGameObject(opponentAttackElement.getAttribute("unit"));
        Unit defender = map.getNeighbourOrNull(direction, unit.getTile()).getDefendingUnit(unit);
        Player player = unit.getOwner();

        if (result == Unit.ATTACKER_LOSS) {
            unit.loseAttack();
        } else {
            unit.winAttack(defender);
        }

        getFreeColClient().getCanvas().refresh();

        return null;
    }


    /**
    * Handles a "setCurrentPlayer"-message.
    *
    * @param setCurrentPlayerElement The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element setCurrentPlayer(Element setCurrentPlayerElement) {
        FreeColClient freeColClient = getFreeColClient();
        Game game = freeColClient.getGame();

        Player currentPlayer = (Player) game.getFreeColGameObject(setCurrentPlayerElement.getAttribute("player"));

        freeColClient.getInGameController().setCurrentPlayer(currentPlayer);

        return null;
    }

    
    /**
    * Handles an "emigrateUnitInEuropeConfirmed"-message.
    *
    * @param setCurrentPlayerElement The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element emigrateUnitInEuropeConfirmed(Element emigrateUnitInEuropeConfirmedElement) {
        Game game = getFreeColClient().getGame();
        Unit unit = new Unit(game, (Element) emigrateUnitInEuropeConfirmedElement.getChildNodes().item(0));
        getFreeColClient().getMyPlayer().getEurope().emigrate(Integer.parseInt(emigrateUnitInEuropeConfirmedElement.getAttribute("slot")),
                                                         unit,
                                                         Integer.parseInt(emigrateUnitInEuropeConfirmedElement.getAttribute("newRecruitable")));
        return null;
    }

    
    /**
    * Handles a "newTurn"-message.
    *
    * @param newTurnElement The element (root element in a DOM-parsed XML tree) that
    *                       holds all the information.
    */
    private Element newTurn(Element newTurnElement) {
        getFreeColClient().getGame().newTurn();

        return null;
    }


    /**
    * Handles a "setDead"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element setDead(Element element) {
        FreeColClient freeColClient = getFreeColClient();
        Game game = freeColClient.getGame();
        Player player = (Player) game.getFreeColGameObject(element.getAttribute("player"));
        player.setDead(true);

        if (player == freeColClient.getMyPlayer()) {
            Canvas canvas = freeColClient.getCanvas();
            if (!canvas.showConfirmDialog("defeated.text", "defeated.yes", "defeated.no")) {
                canvas.reallyQuit();
            }
        }

        return null;
    }


    /**
    * Handles a "createUnit"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element createUnit(Element element) {
        Game game = getFreeColClient().getGame();

        Location location = (Location) game.getFreeColGameObject(element.getAttribute("location"));
        Unit unit = new Unit(game, (Element) element.getChildNodes().item(0));

        // TODO-WHEN-EXTENDING-GAME: Add "createUnit" to interface Location.
        //                           in order to make it possible to produce units
        //                           other places than the colonies.
        ((Colony) location).createUnit(unit);

        return null;
    }


    /**
    * Handles a "gameEnded"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element gameEnded(Element element) {
        FreeColClient freeColClient = getFreeColClient();
        Game game = freeColClient.getGame();

        Player winner = (Player) game.getFreeColGameObject(element.getAttribute("winner"));
        if (winner == freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showVictoryPanel();
        } // else: The client has already received the message of defeat.

        return null;
    }


    /**
    * Handles a "chat"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element chat(Element element) {
        Game game = getFreeColClient().getGame();

        Player sender = (Player) game.getFreeColGameObject(element.getAttribute("sender"));
        String message = element.getAttribute("message");
        boolean privateChat = Boolean.valueOf(element.getAttribute("privateChat")).booleanValue();

        getFreeColClient().getCanvas().displayChatMessage(sender, message, privateChat);

        return null;
    }


    /**
    * Handles an "error"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element error(Element element) {
        Canvas canvas = getFreeColClient().getCanvas();

        if (element.hasAttribute("messageID")) {
            canvas.errorMessage(element.getAttribute("messageID"), element.getAttribute("message"));
        } else {
            canvas.errorMessage(null, element.getAttribute("message"));
        }

        return null;
    }
}
