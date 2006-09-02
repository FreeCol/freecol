
package net.sf.freecol.server.ai;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.DummyConnection;

import org.w3c.dom.Element;


/**
* Handles the network messages that arrives while in the game.
*/
public final class AIInGameInputHandler implements MessageHandler {
    private static final Logger logger = Logger.getLogger(AIInGameInputHandler.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** The player for whom I work. */
    private final ServerPlayer me;
    private final FreeColServer freeColServer;
    private final AIMain aiMain;


    /**
    * The constructor to use.
    * @param freeColServer The main server.
    * @param me The AI player that is being managed by this AIInGameInputHandler.
    * @param aiMain The main AI-object.
    */
    public AIInGameInputHandler(FreeColServer freeColServer, ServerPlayer me, AIMain aiMain) {
        this.freeColServer = freeColServer;
        this.me = me;
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
    * @param connection The <code>Connection</code> the message was received on.
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
                } else if (type.equals("setAI")) {
                } else if (type.equals("startGame")) {
                } else if (type.equals("updateGame")) {
                } else if (type.equals("addPlayer")) {
                } else if (type.equals("opponentMove")) {
                } else if (type.equals("opponentAttack")) {
                } else if (type.equals("attackResult")) {
                } else if (type.equals("setCurrentPlayer")) {
                    reply = setCurrentPlayer((DummyConnection) connection, element);
                } else if (type.equals("emigrateUnitInEuropeConfirmed")) {
                } else if (type.equals("newTurn")) {
                } else if (type.equals("setDead")) {
                } else if (type.equals("gameEnded")) {
                } else if (type.equals("disconnect")) {
                } else if (type.equals("logout")) {
                } else if (type.equals("error")) {
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
                } else if (type.equals("giveIndependence")) {
                } else if (type.equals("lostCityRumour")) {
                } else {
                    logger.warning("Message is of unsupported type \"" + type + "\".");
                }
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
        }

        return reply;
    }



    /**
    * Handles a "setCurrentPlayer"-message.
    *
    * @param connection The connectio the message was received on.
    * @param setCurrentPlayerElement The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element setCurrentPlayer(final DummyConnection connection, final Element setCurrentPlayerElement) {
        final Game game = freeColServer.getGame();
        final Player currentPlayer = (Player) game.getFreeColGameObject(setCurrentPlayerElement.getAttribute("player"));

        if (me.getID() == currentPlayer.getID()) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        getAIPlayer().startWorking();
                    } catch (Exception e) {
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        logger.warning(sw.toString());
                    }
                    
                    Element replyElement = Message.createNewRootElement("endTurn");                    
                    try {
                        connection.send(replyElement);
                    } catch (IOException e) {
                        logger.warning("Could not send \"endTurn\"-message!");
                    }
                    
                }
            };
            t.start();
        }

        return null;
    }


    /**
    * Handles a "chooseFoundingFather"-message.
    *
    * @param connection The connectio the message was received on.
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element chooseFoundingFather(DummyConnection connection, Element element) {
        int[] possibleFoundingFathers = new int[FoundingFather.TYPE_COUNT];
        for (int i=0; i<FoundingFather.TYPE_COUNT; i++) {
            possibleFoundingFathers[i] = Integer.parseInt(element.getAttribute("foundingFather" + Integer.toString(i)));
        }

        int foundingFather = getAIPlayer().selectFoundingFather(possibleFoundingFathers);

        Element reply = Message.createNewRootElement("chosenFoundingFather");
        reply.setAttribute("foundingFather", Integer.toString(foundingFather));

        me.setCurrentFather(foundingFather);

        return reply;
    }


    /**
     * Handles a "monarchAction"-message.
     *
     * @param connection The connection the message was received on.
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     */
    private Element monarchAction(DummyConnection connection, Element element) {
        int action = Integer.parseInt(element.getAttribute("action"));

        Element reply = null;
        switch (action) {
        case Monarch.RAISE_TAX:
            int tax = Integer.parseInt(element.getAttribute("amount"));
            boolean accept = getAIPlayer().acceptTax(tax);
            reply = Message.createNewRootElement("acceptTax");
            reply.setAttribute("accepted", String.valueOf(accept));
            break;
        case Monarch.OFFER_MERCENARIES:
            reply = Message.createNewRootElement("hireMercenaries");
            if (getAIPlayer().getStrategy() == AIPlayer.STRATEGY_CONQUEST ||
                getAIPlayer().getPlayer().isAtWar()) {
                reply.setAttribute("accepted", String.valueOf(true));
            } else {
                reply.setAttribute("accepted", String.valueOf(false));
            }
        }

        return reply;
    }   

    /**
     * Handles an "indianDemand"-message.
     *
     * @param connection The connection the message was received on.
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
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
     * Gets the <code>AIPlayer</code> using this
     * <code>AIInGameInputHandler</code>.
     * 
     * @return The <code>AIPlayer</code>.
     */
    public AIPlayer getAIPlayer() {
        return (AIPlayer) aiMain.getAIObject(me);
    }
}
