
package net.sf.freecol.client.control;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ConnectException;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.client.gui.Canvas;

import net.sf.freecol.common.model.*;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.Message;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


import net.sf.freecol.server.FreeColServer;


/**
* The controller responsible for starting a server and
* connecting to it. {@link PreGameInputHandler} will be set
* as the input handler when a successful login has been completed,
*/
public final class ConnectController {
    private static final Logger logger = Logger.getLogger(ConnectController.class.getName());


    private FreeColClient freeColClient;


    /** The network <code>Client</code> that can be used to send messages to the server. */
    private Client client;





    /**
    * Creates a new <code>ConnectController</code>.
    * @param freeColClient The main controller.
    */
    public ConnectController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
        //this.canvas = canvas;
    }





    /**
    * Starts a multiplayer server and connects to it.
    *
    * @param username The name to use when logging in.
    * @param port The port in which the server should listen for new clients.
    */
    public void startMultiplayerGame(String username, int port) {
        if (freeColClient.getFreeColServer() != null && freeColClient.getFreeColServer().getServer().getPort() == port) {
            if (freeColClient.getCanvas().showConfirmDialog("stopServer.text", "stopServer.yes", "stopServer.no")) {
                freeColClient.getFreeColServer().shutdown();
            } else {
                return;
            }
        }

        try {
            FreeColServer freeColServer = new FreeColServer(false, port);
            freeColClient.setFreeColServer(freeColServer);
        } catch (IOException e) {
            freeColClient.getCanvas().errorMessage("server.couldNotStart");
            return;
        }

        joinMultiplayerGame(username, "localhost", port);
    }


    /**
    * Starts a new singleplayer game by connecting to the server.
    *
    * @param username The name to use when logging in.
    */
    public void startSingleplayerGame(String username) {
        // TODO-MUCH-LATER: connect client/server directly (not using network-classes)
        int port = 3541;

        if (freeColClient.getFreeColServer() != null && freeColClient.getFreeColServer().getServer().getPort() == port) {
            if (freeColClient.getCanvas().showConfirmDialog("stopServer.text", "stopServer.yes", "stopServer.no")) {
                freeColClient.getFreeColServer().shutdown();
            } else {
                return;
            }
        }

        try {
            FreeColServer freeColServer = new FreeColServer(true, port);
            freeColClient.setFreeColServer(freeColServer);            
        } catch (IOException e) {
            freeColClient.getCanvas().errorMessage("server.couldNotStart");
            return;
        }

        freeColClient.setSingleplayer(true);
        login(username, "127.0.0.1", 3541);

        freeColClient.getPreGameController().setReady(true);
        freeColClient.getCanvas().showStartGamePanel(freeColClient.getGame(), freeColClient.getMyPlayer());
    }


    /**
    * Starts a new multiplayer game by connecting to the server.
    *
    * @param username The name to use when logging in.
    * @param host The name of the machine running the <code>FreeColServer</code>.
    * @param port The port to use when connecting to the host.
    */
    public void joinMultiplayerGame(String username, String host, int port) {
        freeColClient.setSingleplayer(false);
        if (login(username, host, port)) {
            freeColClient.getCanvas().showStartGamePanel(freeColClient.getGame(), freeColClient.getMyPlayer());
        }
    }


    /**
    * Starts the client and connects to <i>host:port</i>.
    *
    * @param username The name to use when logging in. This should be a unique identifier.
    * @param host The name of the machine running the <code>FreeColServer</code>.
    * @param port The port to use when connecting to the host.
    * @return <i>true</i> if the login was completed successfully.
    */
    private boolean login(String username, String host, int port) {
        if (client != null) {
            client.disconnect();
        }

        try {
            client = new Client(host, port, freeColClient.getPreGameInputHandler());
        } catch (ConnectException e) {
            freeColClient.getCanvas().errorMessage("server.couldNotConnect");
            return false;
        } catch (IOException e) {
            freeColClient.getCanvas().errorMessage("server.couldNotConnect");
            return false;
        }

        freeColClient.setClient(client);

        Element element = Message.createNewRootElement("login");
        element.setAttribute("username", username);
        element.setAttribute("freeColVersion", FreeCol.getVersion());

        Element reply = client.ask(element);
        String type = reply.getTagName();

        if (type.equals("loginConfirmed")) {
            boolean admin = (new Boolean(reply.getAttribute("admin"))).booleanValue();
            freeColClient.setAdmin(admin);

            Game game = new Game((Element) reply.getElementsByTagName(Game.getXMLElementTagName()).item(0));
            Player thisPlayer = game.getPlayerByName(username);

            freeColClient.setGame(game);
            freeColClient.setMyPlayer(thisPlayer);
        } else if (type.equals("error")) {
            Canvas canvas = freeColClient.getCanvas();
            if (reply.hasAttribute("messageID")) {
                canvas.errorMessage(reply.getAttribute("messageID"), reply.getAttribute("message"));
            } else {
                canvas.errorMessage(null, reply.getAttribute("message"));
            }
            
            return false;
        } else {
            logger.warning("Unkown message received: " + reply);
            return false;
        }
        
        return true;
    }
}
