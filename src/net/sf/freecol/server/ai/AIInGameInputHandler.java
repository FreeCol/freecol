/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.ChooseFoundingFatherMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.MonarchActionMessage;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.DummyConnection;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Handles the network messages that arrives while in the game.
 */
public final class AIInGameInputHandler implements MessageHandler {

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
                } else if (type.equals("featureChange")) {
                } else if (type.equals("newLandName")) {
                    reply = newLandName((DummyConnection) connection, element);
                } else if (type.equals("newRegionName")) {
                    reply = newRegionName((DummyConnection) connection, element);
                } else if (type.equals("fountainOfYouth")) {
                    reply = fountainOfYouth((DummyConnection) connection, element);
                } else if (type.equals("lootCargo")) {
                    reply = lootCargo(connection, element);
                } else if (type.equals("multiple")) {
                    reply = multiple((DummyConnection) connection, element);
                } else {
                    logger.warning("Message is of unsupported type \"" + type + "\".");
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "AI input handler for " + serverPlayer
                + " caught error handling " + element.getTagName(), e);
        }
        return reply;
    }

    /**
     * Gets the <code>AIPlayer</code> using this
     * <code>AIInGameInputHandler</code>.
     *
     * @return The <code>AIPlayer</code>.
     */
    private AIPlayer getAIPlayer() {
        return aiMain.getAIPlayer(serverPlayer);
    }

    /**
     * Gets the AI unit corresponding to a given unit, if any.
     *
     * @param unit The <code>Unit</code> to look up.
     * @return The corresponding AI unit or null if not found.
     */
    private AIUnit getAIUnit(Unit unit) {
        return aiMain.getAIUnit(unit);
    }

    /**
     * Handles a "setCurrentPlayer"-message.
     *
     * @param connection The connection the message was received on.
     * @param setCurrentPlayerElement The element (root element in a DOM-parsed
     *            XML tree) that holds all the information.
     */
    private Element setCurrentPlayer(final DummyConnection connection,
                                     final Element setCurrentPlayerElement) {
        final Game game = freeColServer.getGame();

        String str = setCurrentPlayerElement.getAttribute("player");
        final Player currentPlayer = game.getFreeColGameObject(str, Player.class);

        if (currentPlayer != null
            && serverPlayer.getId() == currentPlayer.getId()) {
            logger.finest("Starting new Thread for " + serverPlayer.getName());
            Thread t = new Thread(FreeCol.SERVER_THREAD+"AIPlayer (" + serverPlayer.getName() + ")") {
                public void run() {
                    try {
                        getAIPlayer().startWorking();
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "AI player failed while working!", e);
                    }
                    AIMessage.askEndTurn(getAIPlayer());
                }
            };
            t.start();
        }
        return null;
    }

    /**
     * Handles a "chooseFoundingFather"-message.
     * Only meaningful for AIPlayer types that implement selectFoundingFather.
     *
     * @param connection The connection the message was received on.
     * @param element The element (root element in a DOM-parsed XML tree) that
     *     holds all the information.
     */
    private Element chooseFoundingFather(final DummyConnection connection,
                                         Element element) {
        ChooseFoundingFatherMessage message
            = new ChooseFoundingFatherMessage(aiMain.getGame(), element);
        AIPlayer aiPlayer = getAIPlayer();
        FoundingFather ff = aiPlayer.selectFoundingFather(message.getFathers());
        if (ff != null) message.setResult(ff);
        return message.toXMLElement();
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
        Game game = aiMain.getGame();
        MonarchActionMessage message = new MonarchActionMessage(game, element);
        MonarchAction action = message.getAction();
        boolean accept;

        switch (action) {
        case RAISE_TAX_WAR: case RAISE_TAX_ACT:
            accept = getAIPlayer().acceptTax(message.getTax());
            message.setResult(accept);
            logger.finest("AI player monarch action " + action
                          + " = " + accept);
            break;

        case OFFER_MERCENARIES:
            accept = getAIPlayer().acceptMercenaries();
            message.setResult(accept);
            logger.finest("AI player monarch action " + action
                          + " = " + accept);
            break;

        default:
            logger.finest("AI player ignoring monarch action " + action);
            return null;
        }

        return message.toXMLElement();
    }

    /**
     * Handles an "indianDemand"-message.
     *
     * @param connection The connection the message was received on.
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     * @return The original message with the acceptance state set if querying
     *     the colony player (result == null), or null if reporting the final
     *     result to the native player (result != null).
     */
    private Element indianDemand(DummyConnection connection, Element element) {
        Game game = aiMain.getGame();
        IndianDemandMessage message = new IndianDemandMessage(game, element);
        boolean accept = getAIPlayer()
            .indianDemand(message.getUnit(game), message.getColony(game),
                message.getGoods(), message.getGold());
        message.setResult(accept);
        logger.finest("AI handling native demand by " + message.getUnit(game)
            + " at " + message.getColony(game).getName()
            + " result: " + accept);
        return message.toXMLElement();
    }

    /**
     * Handles an "diplomaticTrade"-message.
     *
     * @param connection The connection the message was received on.
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     * @return The original message with the acceptance state set.
     */
    private Element diplomaticTrade(DummyConnection connection, Element element) {
        DiplomacyMessage message
            = new DiplomacyMessage(freeColServer.getGame(), element);
        DiplomaticTrade agreement = message.getAgreement();
        boolean accept = getAIPlayer().acceptDiplomaticTrade(agreement);
        agreement.setStatus((accept) ? TradeStatus.ACCEPT_TRADE
                            : TradeStatus.REJECT_TRADE);
        return message.toXMLElement();
    }

    /**
     * Replies to offer to name the new land.
     *
     * @param connection The connection the message was received on.
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     * @return The default offer.
     */
    private Element newLandName(DummyConnection connection, Element element) {
        NewLandNameMessage message
            = new NewLandNameMessage(freeColServer.getGame(), element);
        message.setAccept(true); // Always accept new land
        return message.toXMLElement();
    }

    /**
     * Replies to offer to name a new region name.
     *
     * @param connection The connection the message was received on.
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     * @return The default offer.
     */
    private Element newRegionName(DummyConnection connection, Element element) {
        return new NewRegionNameMessage(freeColServer.getGame(), element)
            .toXMLElement();
    }

    /**
     * Replies to fountain of youth offer.
     *
     * @param connection The connection the message was received on.
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     * @return Null.
     */
    private Element fountainOfYouth(DummyConnection connection, Element element) {
        String migrants = element.getAttribute("migrants");
        int n;
        try {
            n = Integer.parseInt(migrants);
        } catch (NumberFormatException e) {
            n = -1;
        }
        for (int i = 0; i < n; i++) {
            AIMessage.askEmigrate(getAIPlayer(), 0);
        }
        return null;
    }

    /**
     * Replies to loot cargo offer.
     *
     * @param connection The connection the message was received on.
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information.
     * @return Null.
     */
    private Element lootCargo(Connection connection, Element element) {
        Game game = freeColServer.getGame();
        LootCargoMessage message = new LootCargoMessage(game, element);
        Unit unit = message.getUnit(game);
        List<Goods> goods = message.getGoods();
        final Market market = serverPlayer.getMarket();
        Collections.sort(goods, new Comparator<Goods>() {
                public int compare(Goods g1, Goods g2) {
                    int p1 = market.getPaidForSale(g1.getType())
                        * g1.getAmount();
                    int p2 = market.getPaidForSale(g2.getType())
                        * g2.getAmount();
                    return p2 - p1;
                }
            });
        List<Goods> loot = new ArrayList<Goods>();
        int space = unit.getSpaceLeft();
        while (!goods.isEmpty()) {
            Goods g = goods.remove(0);
            if (g.getSpaceTaken() > space) continue; // Approximate
            loot.add(g);
            space -= g.getSpaceTaken();
        }
        AIMessage.askLoot(getAIUnit(unit), message.getDefenderId(), loot);
        return null;
    }


    /**
     * Handle all the children of this element.
     *
     * @param connection The <code>Connection</code> the element arrived on.
     * @param element The <code>Element</code> to process.
     * @return An <code>Element</code> containing the response/s.
     */
    public Element multiple(Connection connection, Element element) {
        NodeList nodes = element.getChildNodes();
        List<Element> results = new ArrayList<Element>();

        for (int i = 0; i < nodes.getLength(); i++) {
            try {
                Element reply = handle(connection, (Element) nodes.item(i));
                if (reply != null) results.add(reply);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Caught crash in multiple item " + i
                    + ", continuing.", e);
            }
        }
        return DOMMessage.collapseElements(results);
    }
}
