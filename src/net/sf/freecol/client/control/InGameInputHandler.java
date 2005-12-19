
package net.sf.freecol.client.control;

import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
* Handles the network messages that arrives while in the game.
*/
public final class InGameInputHandler extends InputHandler {
    private static final Logger logger = Logger.getLogger(InGameInputHandler.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


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
    * @return The reply.
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
            } else if (type.equals("newTurn")) {
                reply = newTurn(element);
            } else if (type.equals("setDead")) {
                reply = setDead(element);
            } else if (type.equals("gameEnded")) {
                reply = gameEnded(element);
            } else if (type.equals("chat")) {
                reply = chat(element);
            } else if (type.equals("disconnect")) {
                reply = disconnect(element);
            } else if (type.equals("error")) {
                reply = error(element);
            } else if (type.equals("chooseFoundingFather")) {
                reply = chooseFoundingFather(element);
            } else if (type.equals("deliverGift")) {
                reply = deliverGift(element);
            } else if (type.equals("reconnect")) {
                reply = reconnect(element);
            } else if (type.equals("setAI")) {
                reply = setAI(element);
	    } else if (type.equals("monarchAction")) {
		reply = monarchAction(element);
	    } else if (type.equals("removeGoods")) {
		reply = removeGoods(element);
            } else {
                logger.warning("Message is of unsupported type \"" + type + "\".");
            }
        }

        return reply;
    }


    /**
    * Handles an "reconnect"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element reconnect(Element element) {
        final FreeColClient freeColClient = getFreeColClient();
        if (freeColClient.getCanvas().showConfirmDialog("reconnect.text", "reconnect.yes", "reconnect.no")) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    freeColClient.getConnectController().reconnect();
                }
            });
        } else {
            getFreeColClient().quit();
        }

        return null;
    }
    

    /**
    * Handles an "update"-message.
    *
    * @param updateElement The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    * @return The reply.
    */
    public Element update(Element updateElement) {
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
            final Unit unit = (Unit) game.getFreeColGameObject(opponentMoveElement.getAttribute("unit"));

            if (unit == null) {
                logger.warning("Could not find the 'unit' in 'opponentMove'. Unit ID: "+ opponentMoveElement.getAttribute("unit"));
                return null;
            }

            if (unit.getTile() == null) {
                logger.warning("unit.getTile() == null");
                return null;
            }

            if (currentPlayer.canSee(map.getNeighbourOrNull(direction, unit.getTile())))  {
                final Tile oldTile = unit.getTile();
                try {
                    unit.move(direction);
                } catch (IllegalStateException e) {
                    System.err.println(unit.getTile().getPosition().getX() + ", " + unit.getTile().getPosition().getY());
                    throw e;
                }
                final Tile newTile = unit.getTile();

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getFreeColClient().getCanvas().refreshTile(oldTile);
                        getFreeColClient().getCanvas().refreshTile(newTile);
                        getFreeColClient().getGUI().setFocus(newTile.getPosition());
                    }
                });
            } else {
                final Tile oldTile = unit.getTile();
                unit.dispose();

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getFreeColClient().getCanvas().refreshTile(oldTile);
                    }
                });
            }
        } else {
            String tileID = opponentMoveElement.getAttribute("tile");

            Element unitElement = Message.getChildElement(opponentMoveElement, Unit.getXMLElementTagName());
            if (unitElement == null) {
                logger.warning("unitElement == null");
                throw new NullPointerException("unitElement == null");
            }
            final Unit unit = new Unit(game, unitElement);

            if (game.getFreeColGameObject(tileID) == null) {
                logger.warning("Could not find tile with id: " + tileID);
            }
            unit.setLocation((Tile) game.getFreeColGameObject(tileID));
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    getFreeColClient().getCanvas().refreshTile(unit.getTile());
                    getFreeColClient().getGUI().setFocus(unit.getTile().getPosition());
                }
            });
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
        Unit unit = (Unit) game.getFreeColGameObject(opponentAttackElement.getAttribute("unit"));
        Unit defender = (Unit) game.getFreeColGameObject(opponentAttackElement.getAttribute("defender"));

        if (unit == null && defender == null) {
            logger.warning("Both \"unit\" and \"defender\" is \"null\"!");
            throw new NullPointerException();
        }

        // For later use: int direction = Integer.parseInt(opponentAttackElement.getAttribute("direction"));
        int result = Integer.parseInt(opponentAttackElement.getAttribute("result"));
        int plunderGold = Integer.parseInt(opponentAttackElement.getAttribute("plunderGold"));

        if (opponentAttackElement.hasAttribute("update")) {
            if (opponentAttackElement.getAttribute("update").equals("unit")) {
                Element unitElement = Message.getChildElement(opponentAttackElement, Unit.getXMLElementTagName());
                if (unitElement == null) {
                    logger.warning("unitElement == null");
                    throw new NullPointerException("unitElement == null");
                }
                unit = new Unit(game, unitElement);
                unit.setLocation(unit.getTile());
                if (unit.getTile() == null) {
                    logger.warning("unit.getTile() == null");
                    throw new NullPointerException("unit.getTile() == null");
                }
            } else if (opponentAttackElement.getAttribute("update").equals("defender")) {
                Element defenderTileElement = Message.getChildElement(opponentAttackElement, Tile.getXMLElementTagName());
                if (defenderTileElement != null) {
                    Tile defenderTile = (Tile) game.getFreeColGameObject(defenderTileElement.getAttribute("ID"));
                    defenderTile.readFromXMLElement(defenderTileElement);
                }

                Element defenderElement = Message.getChildElement(opponentAttackElement, Unit.getXMLElementTagName());
                if (defenderElement == null) {
                    logger.warning("defenderElement == null");
                    throw new NullPointerException("defenderElement == null");
                }
                defender = new Unit(game, defenderElement);
                defender.setLocation(defender.getTile());

                if (defender.getTile() == null) {
                    logger.warning("defender.getTile() == null");
                    throw new NullPointerException();
                }
            } else {
                logger.warning("Unknown update.");
                throw new IllegalStateException("Unknown update.");
            }
        }

        if (unit == null) {
            logger.warning("unit == null");
            throw new NullPointerException("unit == null");
        }

        if (defender == null) {
            logger.warning("defender == null");
            throw new NullPointerException("defender == null");
        }
        
        unit.attack(defender, result, plunderGold);

        if (!unit.isDisposed() && (unit.getLocation() == null || !unit.isVisibleTo(getFreeColClient().getMyPlayer()))) {
            unit.dispose(); 
        }

        if (!defender.isDisposed() && (defender.getLocation() == null || !defender.isVisibleTo(getFreeColClient().getMyPlayer()))) {
            defender.dispose();
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
                freeColClient.quit();
            }
        }

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


    /**
    * Handles a "setAI"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element setAI(Element element) {
        Game game = getFreeColClient().getGame();

        Player p = (Player) game.getFreeColGameObject(element.getAttribute("player"));
        p.setAI(Boolean.valueOf(element.getAttribute("ai")).booleanValue());

        return null;
    }


    /**
    * Handles an "chooseFoundingFather"-request.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element chooseFoundingFather(Element element) {
        int[] possibleFoundingFathers = new int[FoundingFather.TYPE_COUNT];
        for (int i=0; i<FoundingFather.TYPE_COUNT; i++) {
            possibleFoundingFathers[i] = Integer.parseInt(element.getAttribute("foundingFather" + Integer.toString(i)));
        }

        int foundingFather = getFreeColClient().getCanvas().showChooseFoundingFatherDialog(possibleFoundingFathers);
        
        Element reply = Message.createNewRootElement("chosenFoundingFather");
        reply.setAttribute("foundingFather", Integer.toString(foundingFather));
        
        getFreeColClient().getMyPlayer().setCurrentFather(foundingFather);
        
        return reply;
    }
    
    
    /**
    * Handles an "deliverGift"-request.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element deliverGift(Element element) {
        Game game = getFreeColClient().getGame();
        Element unitElement = Message.getChildElement(element, Unit.getXMLElementTagName());

        Unit unit = (Unit) game.getFreeColGameObject(unitElement.getAttribute("ID"));
        unit.readFromXMLElement(unitElement);

        Settlement settlement = (Settlement) game.getFreeColGameObject(element.getAttribute("settlement"));
        Goods goods = new Goods(game, Message.getChildElement(element, Goods.getXMLElementTagName()));

        unit.deliverGift(settlement, goods);

        return null;
    }


    /**
     * Handles a "monarchAction"-request.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     */
    private Element monarchAction(Element element) {
        final FreeColClient freeColClient = getFreeColClient();
        Player player = freeColClient.getMyPlayer();
        Canvas canvas = freeColClient.getCanvas();
        int action = Integer.parseInt(element.getAttribute("action"));

        switch (action) {
        case Monarch.RAISE_TAX:
            Element reply = Message.createNewRootElement("acceptTax");
            if (freeColClient.getCanvas().
                showMonarchPanel(action,
                                 new String [][] {{"%replace%", element.getAttribute("amount")}})) {
                int amount = new Integer(element.getAttribute("amount")).intValue();
                freeColClient.getMyPlayer().setTax(amount);
                freeColClient.getCanvas().getEuropePanel().updateTaxLabel();
                reply.setAttribute("accepted", String.valueOf(true));
            } else {
                reply.setAttribute("accepted", String.valueOf(false));
                
            }
            return reply;
        case Monarch.ADD_TO_REF:
            Monarch monarch = player.getMonarch();
            int type = Integer.parseInt(element.getAttribute("type"));
            int number = Integer.parseInt(element.getAttribute("number"));
            Monarch.Addition addition = new Monarch.Addition(type, number);
            monarch.addToREF(addition);
            canvas.showMonarchPanel(action,
                                    new String [][] {{"%addition%", addition.getName()}});
            break;
        case Monarch.DECLARE_WAR:
            int nation = Integer.parseInt(element.getAttribute("nation"));
            player.setStance(nation, Player.WAR);
            canvas.showMonarchPanel(action,
                                    new String [][] {{"%nation%", player.getNationAsString(nation)}});
            break;
        case Monarch.SUPPORT_LAND:
            NodeList landList = element.getChildNodes();
            for (int i = 0; i < landList.getLength(); i++) {
                Element unitElement = (Element) landList.item(i);
                Unit newUnit = new Unit(freeColClient.getGame(), unitElement);
                player.getEurope().add(newUnit);
            }
            if (!canvas.showMonarchPanel(action, null)) {
                canvas.showEuropePanel();
            }
            break;
        case Monarch.SUPPORT_SEA:
            NodeList seaList = element.getChildNodes();
            for (int i = 0; i < seaList.getLength(); i++) {
                Element unitElement = (Element) seaList.item(i);
                Unit newUnit = new Unit(freeColClient.getGame(), unitElement);
                player.getEurope().add(newUnit);
            }
            if (!canvas.showMonarchPanel(action, null)) {
                canvas.showEuropePanel();
            }
            break;
        }
        return null;
    }

    /**
     * Handles a "diplomaticMessage"-request.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     */
    private Element diplomaticMessage(Element element) {
        final FreeColClient freeColClient = getFreeColClient();
        Game game = freeColClient.getGame();
        Player player = freeColClient.getMyPlayer();
        Canvas canvas = freeColClient.getCanvas();

        String type = element.getAttribute("type");
        if (type.equals("declarationOfWar")) {
            Player attacker = (Player) game.getFreeColGameObject(element.getAttribute("attacker"));
            Player defender = (Player) game.getFreeColGameObject(element.getAttribute("defender"));
        
            if (player.equals(defender)) {
                canvas.showInformationMessage("model.diplomacy.war.declared",
                                              new String [][] {{"%nation%",
                                                                attacker.getNationAsString()}});
                player.declareWar(attacker);
            } else {
                canvas.showInformationMessage("model.diplomacy.war.others",
                                              new String [][] {{"%attacker%",
                                                                attacker.getNationAsString()},
                                                               {"%defender%",
                                                                defender.getNationAsString()}});
            }
        }
            
        return null;
    }



    /**
     * Handles a "removeGoods"-request.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     */
    private Element removeGoods(Element element) {
        final FreeColClient freeColClient = getFreeColClient();
        Game game = getFreeColClient().getGame();
        ModelMessage m = null;

        NodeList nodeList = element.getChildNodes();
        Element goodsElement = (Element) nodeList.item(0);

        if (goodsElement == null) {
            // player has no colony or nothing to trade
            freeColClient.getCanvas().showMonarchPanel(Monarch.WAIVE_TAX, null);
        } else {
            Goods goods = new Goods(game, goodsElement);
            Colony colony = (Colony) goods.getLocation();
            colony.removeGoods(goods);

            Player player = freeColClient.getMyPlayer();
            if (!player.hasFather(FoundingFather.JACOB_FUGGER)) {
                player.setArrears(goods);
            }
        
            m = new ModelMessage(colony,
                                 "model.monarch.bostonTeaParty",
                                 new String [][] {{"%colony%", colony.getName()},
                                                  {"%amount%", String.valueOf(goods.getAmount())},
                                                  {"%goods%", goods.getName()}});
            freeColClient.getCanvas().showModelMessage(m);
        }
                         
        return null;
    }


    
}
