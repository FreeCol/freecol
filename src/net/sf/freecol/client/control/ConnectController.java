
package net.sf.freecol.client.control;


import java.io.*;
import java.net.ConnectException;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.client.gui.Canvas;

import net.sf.freecol.common.model.*;
import net.sf.freecol.common.networking.Message;

import org.w3c.dom.Element;


import net.sf.freecol.server.FreeColServer;


/**
* The controller responsible for starting a server and
* connecting to it. {@link PreGameInputHandler} will be set
* as the input handler when a successful login has been completed,
*/
public final class ConnectController {
    private static final Logger logger = Logger.getLogger(ConnectController.class.getName());


    private FreeColClient freeColClient;



    /**
    * Creates a new <code>ConnectController</code>.
    * @param freeColClient The main controller.
    */
    public ConnectController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
    }



    /**
    * Starts a multiplayer server and connects to it.
    *
    * @param username The name to use when logging in.
    * @param port The port in which the server should listen for new clients.
    */
    public void startMultiplayerGame(String username, int port) {
        if (freeColClient.isLoggedIn()) {
            logout(true);
        }

        if (freeColClient.getFreeColServer() != null && freeColClient.getFreeColServer().getServer().getPort() == port) {
            if (freeColClient.getCanvas().showConfirmDialog("stopServer.text", "stopServer.yes", "stopServer.no")) {
                freeColClient.getFreeColServer().getController().shutdown();
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
        if (freeColClient.isLoggedIn()) {
            logout(true);
        }

        // TODO-MUCH-LATER: connect client/server directly (not using network-classes)
        int port = 3541;

        if (freeColClient.getFreeColServer() != null && freeColClient.getFreeColServer().getServer().getPort() == port) {
            if (freeColClient.getCanvas().showConfirmDialog("stopServer.text", "stopServer.yes", "stopServer.no")) {
                freeColClient.getFreeColServer().getController().shutdown();
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
        freeColClient.getCanvas().showStartGamePanel(freeColClient.getGame(), freeColClient.getMyPlayer(), true);
    }


    /**
    * Starts a new multiplayer game by connecting to the server.
    *
    * @param username The name to use when logging in.
    * @param host The name of the machine running the <code>FreeColServer</code>.
    * @param port The port to use when connecting to the host.
    */
    public void joinMultiplayerGame(String username, String host, int port) {
        if (freeColClient.isLoggedIn()) {
            logout(true);
        }

        freeColClient.setSingleplayer(false);
        if (login(username, host, port) && !freeColClient.getGUI().isInGame()) {
            freeColClient.getCanvas().showStartGamePanel(freeColClient.getGame(), freeColClient.getMyPlayer(), false);
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
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();

        if (client != null) {
            client.disconnect();
        }

        try {
            client = new Client(host, port, freeColClient.getPreGameInputHandler());
        } catch (ConnectException e) {
            canvas.errorMessage("server.couldNotConnect");
            return false;
        } catch (IOException e) {
            canvas.errorMessage("server.couldNotConnect");
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

            Game game = new Game(freeColClient.getModelController(), (Element) reply.getElementsByTagName(Game.getXMLElementTagName()).item(0));
            Player thisPlayer = game.getPlayerByName(username);

            freeColClient.setGame(game);
            freeColClient.setMyPlayer(thisPlayer);

            // If (true) --> reconnect
            if (reply.hasAttribute("startGame") && Boolean.valueOf(reply.getAttribute("startGame")).booleanValue()) {
                freeColClient.setSingleplayer(false);
                freeColClient.getPreGameController().startGame();

                if (Boolean.valueOf(reply.getAttribute("isCurrentPlayer")).booleanValue()) {
                    freeColClient.getInGameController().setCurrentPlayer(thisPlayer);
                }
            }
        } else if (type.equals("error")) {
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

        freeColClient.setLoggedIn(true);

        return true;
    }


    /**
    * Opens a dialog where the user should specify the filename
    * and loads the game.
    */  
    public void loadGame() {  
        File file = freeColClient.getCanvas().showLoadDialog(FreeCol.getSaveDirectory());  
        if (file != null) {               
            loadGame(file);
        }
    }
    
        
    /**
    * Loads a game from the given file.
    * @param file The <code>File</code>.
    */
    public void loadGame(File file) {
        Canvas canvas = freeColClient.getCanvas();
        int port = 3541;
        
        if (freeColClient.getFreeColServer() != null && freeColClient.getFreeColServer().getServer().getPort() == port) {
            if (freeColClient.getCanvas().showConfirmDialog("stopServer.text", "stopServer.yes", "stopServer.no")) {
                freeColClient.getFreeColServer().getController().shutdown();
            } else {
                return;
            }
        }
        
        try {
            FreeColServer freeColServer = new FreeColServer(file, port);
            freeColClient.setFreeColServer(freeColServer);
            
            String username = freeColServer.getOwner();
            
            freeColClient.setSingleplayer(freeColServer.isSingleplayer());
            login(username, "127.0.0.1", 3541);            
        } catch (FileNotFoundException fe) {
            canvas.errorMessage("fileNotFound");
            return;
        } catch (IOException e) {
            freeColClient.getCanvas().errorMessage("server.couldNotStart");
            return;
        }                                       
    }
    
        
    /**
    * Sends a logout message to the server.
    *
    * @param notifyServer Whether or not the server should be notified of the logout.
    * For example: if the server kicked us out then we don't need to confirm with a logout
    * message.
    */
    public void logout(boolean notifyServer) {
        if (notifyServer) {
            Element logoutMessage = Message.createNewRootElement("logout");
            logoutMessage.setAttribute("reason", "User has quit the client.");

            freeColClient.getClient().send(logoutMessage);
        }

        freeColClient.getGUI().setInGame(false);
        freeColClient.setGame(null);
        freeColClient.setMyPlayer(null);
        freeColClient.setClient(null);

        freeColClient.setLoggedIn(false);
    }


    /**
    * Quits the current game. If a server is running it will be stopped if bStopServer is
    * <i>true</i>.
    * If a server is running through this client and bStopServer is true then the clients
    * connected to that server will be notified. If a local client is connected to a server
    * then the server will be notified with a logout in case <i>notifyServer</i> is true.
    *
    * @param bStopServer Indicates whether or not a server that was started through this
    * client should be stopped.
    *
    * @param notifyServer Whether or not the server should be notified of the logout.
    * For example: if the server kicked us out then we don't need to confirm with a logout
    * message.
    */
    public void quitGame(boolean bStopServer, boolean notifyServer) {
        if (bStopServer) {
            final FreeColServer server = freeColClient.getFreeColServer();
            if (server != null) {
                try {
                    freeColClient.getClient().getConnection().reallyClose();
                } catch (IOException e) {
                    logger.warning("Exception: " + e);
                }

                server.getController().shutdown();

                freeColClient.setFreeColServer(null);
                freeColClient.setLoggedIn(false);
            }
        }

        if (freeColClient.isLoggedIn()) {
            logout(notifyServer);
        }
    }


    /**
    * Quits the current game. If a server is running it will be stopped if bStopServer is
    * <i>true</i>.
    * The server and perhaps the clients (if a server is running through this client and
    * bStopServer is true) will be notified.
    *
    * @param bStopServer Indicates whether or not a server that was started through this
    * client should be stopped.
    */
    public void quitGame(boolean bStopServer) {
        quitGame(bStopServer, true);
    }
}
