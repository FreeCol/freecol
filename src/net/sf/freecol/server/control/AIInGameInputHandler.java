
package net.sf.freecol.server.control;

import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Game;

import net.sf.freecol.server.networking.*;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;

import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;

import java.util.logging.Logger;
import java.util.Iterator;

import java.io.IOException;



/**
* Handles the network messages that arrives while in the game.
*/
public final class AIInGameInputHandler implements MessageHandler {
    private static final Logger logger = Logger.getLogger(AIInGameInputHandler.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** The player for whom I work. */
    private final ServerPlayer me;
    private final FreeColServer freeColServer;


    /**
    * The constructor to use.
    * @param freeColServer The main server.
    * @param me The AI player that is being managed by this AIInGameInputHandler.
    */
    public AIInGameInputHandler(FreeColServer freeColServer, ServerPlayer me) {
        this.freeColServer = freeColServer;
        this.me = me;
        if (!me.isAI()) {
            logger.warning("VERY BAD: Applying AIInGameInputHandler to a non-AI player!!!");
        }
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

            // Since we're the server, we can see everything.
            // Therefore most of these messages are useless.
            if (type.equals("update")) {
            } else if (type.equals("remove")) {
            } else if (type.equals("startGame")) {
            } else if (type.equals("updateGame")) {
            } else if (type.equals("addPlayer")) {
            } else if (type.equals("opponentMove")) {
            } else if (type.equals("opponentAttack")) {
            } else if (type.equals("attackResult")) {
            } else if (type.equals("setCurrentPlayer")) {
                reply = setCurrentPlayer(connection, element);
            } else if (type.equals("emigrateUnitInEuropeConfirmed")) {
            } else if (type.equals("newTurn")) {
            } else if (type.equals("setDead")) {
            } else if (type.equals("gameEnded")) {
            } else if (type.equals("disconnect")) {
            } else if (type.equals("logout")) {
            } else if (type.equals("error")) {
            } else if (type.equals("chooseFoundingFather")) {            
                // TODO: Implement
            } else {
                logger.warning("Message is of unsupported type \"" + type + "\".");
            }
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
    private Element setCurrentPlayer(Connection connection, Element setCurrentPlayerElement) {
        Game game = freeColServer.getGame();
        Player currentPlayer = (Player) game.getFreeColGameObject(setCurrentPlayerElement.getAttribute("player"));
        Element reply = null;

        if (me.getID() == currentPlayer.getID()) {
            //TODO: add more for the AI players to do.
             Iterator unitsIterator = me.getUnitIterator();
             while (unitsIterator.hasNext()) {
                 Object theUnit = unitsIterator.next();
                 if (theUnit instanceof ServerUnit) {
                     ((ServerUnit)theUnit).doMission();
                 }
             }
             reply = Message.createNewRootElement("endTurn");
             if (connection instanceof DummyConnection) {
                 ((DummyConnection)connection).handleAndSendReply(reply);
             } else {
                 try {
                    connection.send(reply);
                 } catch (IOException e) {}
             }
        }

        return null;
    }
}
