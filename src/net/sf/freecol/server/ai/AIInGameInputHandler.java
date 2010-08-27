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

package net.sf.freecol.server.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.StreamedMessageHandler;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.DummyConnection;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Handles the network messages that arrives while in the game.
 */
public final class AIInGameInputHandler implements MessageHandler, StreamedMessageHandler {

    private static final Logger logger = Logger.getLogger(AIInGameInputHandler.class.getName());

    /** The player for whom I work. */
    private final ServerPlayer serverPlayer;

    /** The server. */
    private final FreeColServer freeColServer;

    private final AIMain aiMain;


    /**
     * 
     * The constructor to use.
     * 
     * @param freeColServer The main server.
     * 
     * @param me The AI player that is being managed by this
     *            AIInGameInputHandler.
     * 
     * @param aiMain The main AI-object.
     * 
     */
    public AIInGameInputHandler(FreeColServer freeColServer, ServerPlayer me, AIMain aiMain) {
        this.freeColServer = freeColServer;
        this.serverPlayer = me;
        this.aiMain = aiMain;
        if (freeColServer == null) {
            throw new NullPointerException("freeColServer == null");
        } else if (me == null) {
            throw new NullPointerException("me == null");
        } else if (aiMain == null) {
            throw new NullPointerException("aiMain == null");
        }
        if (!me.isAI()) {
            logger.warning("VERY BAD: Applying AIInGameInputHandler to a non-AI player!!!");
        }
    }

    /**
     * Deals with incoming messages that have just been received.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The root element of the message.
     * @return The reply.
     */
    public synchronized Element handle(Connection connection, Element element) {
        Element reply = null;
        try {
            if (element != null) {
                String type = element.getTagName();

                // Since we're the server, we can see everything.
                // Therefore most of these messages are useless.
                if (type.equals("update")) {
                } else if (type.equals("remove")) {
                } else if (type.equals("startGame")) {
                } else if (type.equals("updateGame")) {
                } else if (type.equals("addPlayer")) {
                } else if (type.equals("animateMove")) {
                } else if (type.equals("animateAttack")) {
                } else if (type.equals("setCurrentPlayer")) {
                    reply = setCurrentPlayer((DummyConnection) connection, element);
                } else if (type.equals("newTurn")) {
                } else if (type.equals("setDead")) {
                } else if (type.equals("gameEnded")) {
                } else if (type.equals("disconnect")) {
                } else if (type.equals("logout")) {
                } else if (type.equals("error")) {
                } else if (type.equals("setAI")) {
                } else if (type.equals("chat")) {
                } else if (type.equals("chooseFoundingFather")) {
                    reply = chooseFoundingFather((DummyConnection) connection, element);
                } else if (type.equals("reconnect")) {
                    logger.warning("The server requests a reconnect. This means an illegal operation has been performed. Please refer to any previous error message.");
                } else if (type.equals("setStance")) {
                } else if (type.equals("monarchAction")) {
                    reply = monarchAction((DummyConnection) connection, element);
                } else if (type.equals("removeGoods")) {
                } else if (type.equals("indianDemand")) {
                    reply = indianDemand((DummyConnection) connection, element);
                } else if (type.equals("diplomacy")) {
                    reply = diplomaticTrade((DummyConnection) connection, element);
                } else if (type.equals("addObject")) {
                } else if (type.equals("multiple")) {
                    reply = multiple((DummyConnection) connection, element);
                } else {
                    logger.warning("Message is of unsupported type \"" + type + "\".");
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "AI input handler for " + serverPlayer + " caught error handling "
                    + element.getTagName(), e);
        }
        return reply;
    }

    /**
     * Handles the main element of an XML message.
     * 
     * @param connection The connection the message came from.
     * @param in The stream containing the message.
     * @param out The output stream for the reply.
     */
    public void handle(Connection connection, XMLStreamReader in, XMLStreamWriter out) {
        // TODO: not yet implemented!
    }

    /**
     * 
     * Checks if the message handler support the given message.
     * 
     * @param tagName The tag name of the message to check.
     * @return The result (currently always false).
     */
    public boolean accepts(String tagName) {
        return false;
    }

    /**
     * Handles a "setCurrentPlayer"-message.
     * 
     * @param connection The connection the message was received on.
     * @param setCurrentPlayerElement The element (root element in a DOM-parsed
     *            XML tree) that holds all the information.
     */
    private Element setCurrentPlayer(final DummyConnection connection, final Element setCurrentPlayerElement) {
        logger.finest("Entering setCurrentPlayer");
        final Game game = freeColServer.getGame();
        final Player currentPlayer = (Player) game.getFreeColGameObject(setCurrentPlayerElement.getAttribute("player"));

        if (serverPlayer.getId() == currentPlayer.getId()) {
            logger.finest("Starting new Thread for " + serverPlayer.getName());
            Thread t = new Thread(FreeCol.SERVER_THREAD+"AIPlayer (" + serverPlayer.getName() + ")") {
                public void run() {
                    try {
                        getAIPlayer().startWorking();
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "AI player failed while working!", e);
                    }
                    try {
                        connection.send(Message.createNewRootElement("endTurn"));
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Could not send \"endTurn\"-message!", e);
                    }
                }
            };
            t.start();
        }
        logger.finest("About to exit from setCurrentPlayerElement.");
        return null;
    }

    /**
     * 
     * Handles a "chooseFoundingFather"-message.
     * 
     * 
     * 
     * @param connection The connectio the message was received on.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     * 
     * holds all the information.
     * 
     */
    private Element chooseFoundingFather(DummyConnection connection, Element element) {
        final List<FoundingFather> possibleFoundingFathers = new ArrayList<FoundingFather>();
        for (FoundingFatherType type : FoundingFatherType.values()) {
            String id = element.getAttribute(type.toString());
            if (id != null && id != "") {
                possibleFoundingFathers.add(aiMain.getGame().getSpecification().getFoundingFather(id));
            }
        }

        FoundingFather foundingFather = getAIPlayer().selectFoundingFather(possibleFoundingFathers);
        Element reply = Message.createNewRootElement("chosenFoundingFather");
        reply.setAttribute("foundingFather", foundingFather.getId());
        serverPlayer.setCurrentFather(foundingFather);

        return reply;
    }

    /**
     * Handles a "monarchAction"-message.
     * 
     * @param connection The connection the message was received on.
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     * 
     */
    private Element monarchAction(DummyConnection connection, Element element) {
        MonarchAction action = Enum.valueOf(MonarchAction.class, element.getAttribute("action"));
        Element reply = null;
        boolean accept = false;
        switch (action) {
        case RAISE_TAX:
            int tax = Integer.parseInt(element.getAttribute("amount"));
            accept = getAIPlayer().acceptTax(tax);
            reply = Message.createNewRootElement("acceptTax");
            reply.setAttribute("accepted", String.valueOf(accept));
            break;
            
        case LOWER_TAX:
            int newTax = Integer.parseInt(element.getAttribute("amount"));
            getAIPlayer().getPlayer().setTax(newTax);
            break;

        case OFFER_MERCENARIES:
            reply = Message.createNewRootElement("hireMercenaries");
            accept = getAIPlayer().acceptMercenaryOffer();
            reply.setAttribute("accepted", String.valueOf(accept));
            break;

        default:
            logger.info("AI player ignoring monarch action " + action);
        }

        return reply;
    }

    /**
     * Handles an "indianDemand"-message.
     * 
     * @param connection The connection the message was received on.
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    private Element indianDemand(DummyConnection connection, Element element) {
        Game game = freeColServer.getGame();
        Unit unit = (Unit) game.getFreeColGameObject(element.getAttribute("unit"));
        Colony colony = (Colony) game.getFreeColGameObject(element.getAttribute("colony"));
        int gold = 0;
        Goods goods = null;
        Element goodsElement = Message.getChildElement(element, Goods.getXMLElementTagName());
        if (goodsElement == null) {
            gold = Integer.parseInt(element.getAttribute("gold"));
        } else {
            goods = new Goods(game, goodsElement);
        }
        boolean accept = getAIPlayer().acceptIndianDemand(unit, colony, goods, gold);
        element.setAttribute("accepted", String.valueOf(accept));
        return element;
    }

    /**
     * Handles an "diplomaticTrade"-message.
     * 
     * @param connection The connection the message was received on.
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     * @return Either the original message with "accept" set, or null on rejection.
     */
    private Element diplomaticTrade(DummyConnection connection, Element element) {
        // TODO: make an informed decision
        DiplomacyMessage message = new DiplomacyMessage(freeColServer.getGame(), element);
        DiplomaticTrade agreement = message.getAgreement();
        boolean accept = getAIPlayer().acceptDiplomaticTrade(agreement);
        agreement.setStatus((accept) ? TradeStatus.ACCEPT_TRADE
                            : TradeStatus.REJECT_TRADE);
        return message.toXMLElement();
    }

    /**
     * Handle all the children of this element.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     */
    public Element multiple(Connection connection, Element element) {
        NodeList nodes = element.getChildNodes();
        Element reply = null;

        for (int i = 0; i < nodes.getLength(); i++) {
            reply = handle(connection, (Element) nodes.item(i));
        }
        return reply;
    }

    /**
     * 
     * Gets the <code>AIPlayer</code> using this
     * 
     * <code>AIInGameInputHandler</code>.
     * 
     * 
     * 
     * @return The <code>AIPlayer</code>.
     * 
     */
    public AIPlayer getAIPlayer() {
        return (AIPlayer) aiMain.getAIObject(serverPlayer);
    }
}
