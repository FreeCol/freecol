
package net.sf.freecol.client.control;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.LoadingSavegameDialog;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.ServerInfo;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.generator.MapGeneratorOptions;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
* The controller responsible for starting a server and
* connecting to it. {@link PreGameInputHandler} will be set
* as the input handler when a successful login has been completed,
*/
public final class ConnectController {
    private static final Logger logger = Logger.getLogger(ConnectController.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final FreeColClient freeColClient;



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
    * @param publicServer Should this server be listed at the meta server.
    * @param username The name to use when logging in.
    * @param port The port in which the server should listen for new clients.
    */
    public void startMultiplayerGame(boolean publicServer, String username, int port) {
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
            FreeColServer freeColServer = new FreeColServer(publicServer, false, port, null);
            freeColClient.setFreeColServer(freeColServer);
        }
        catch (IOException e) {
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
            FreeColServer freeColServer = new FreeColServer(false, true, port, null);
            freeColClient.setFreeColServer(freeColServer);
        }
        catch (IOException e) {
            freeColClient.getCanvas().errorMessage("server.couldNotStart");
            return;
        }

        freeColClient.setSingleplayer(true);
        boolean loggedIn = login(username, "127.0.0.1", 3541);

        if (loggedIn) {
            freeColClient.getPreGameController().setReady(true);
            freeColClient.getCanvas().showStartGamePanel(freeColClient.getGame(), freeColClient.getMyPlayer(), true);
        }
    }


    /**
    * Starts a new multiplayer game by connecting to the server.
    *
    * @param username The name to use when logging in.
    * @param host The name of the machine running the <code>FreeColServer</code>.
    * @param port The port to use when connecting to the host.
    */
    public void joinMultiplayerGame(String username, String host, int port) {
        final Canvas canvas = freeColClient.getCanvas();

        if (freeColClient.isLoggedIn()) {
            logout(true);
        }

        List vacantPlayers = getVacantPlayers(host, port);
        if (vacantPlayers != null) {
            Object choice = canvas.showChoiceDialog(Messages.message("connectController.choicePlayer"),
                                                    Messages.message("cancel"),
                                                    vacantPlayers.iterator());
            if (choice != null) {
                username = (String) choice;
            } else {
                return;
            }
        }

        freeColClient.setSingleplayer(false);
        if (login(username, host, port) && !freeColClient.getGUI().isInGame()) {
            canvas.showStartGamePanel(freeColClient.getGame(), freeColClient.getMyPlayer(), false);
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

        Connection c = client.getConnection();
        XMLStreamReader in = null;
        try {
            XMLStreamWriter out = c.ask();
            out.writeStartElement("login");
            out.writeAttribute("username", username);
            out.writeAttribute("freeColVersion", FreeCol.getVersion());
            out.writeEndElement();
            in = c.getReply();
            if (in.getLocalName().equals("loginConfirmed")) {
                final String startGameStr = in.getAttributeValue(null, "startGame");
                boolean startGame = (startGameStr != null) && Boolean.valueOf(startGameStr).booleanValue();
                boolean singleplayer = Boolean.valueOf(in.getAttributeValue(null, "singleplayer")).booleanValue();
                boolean isCurrentPlayer = Boolean.valueOf(in.getAttributeValue(null, "isCurrentPlayer")).booleanValue();                
                
                in.nextTag();
                Game game = new Game(freeColClient.getModelController(), in, username);
                Player thisPlayer = game.getPlayerByName(username);

                freeColClient.setGame(game);
                freeColClient.setMyPlayer(thisPlayer);

                final MapGeneratorOptions mgo;
                if (in.getLocalName().equals(MapGeneratorOptions.getXMLElementTagName())) {
                    mgo = new MapGeneratorOptions(in);
                } else {
                    mgo = new MapGeneratorOptions();
                }
                freeColClient.getPreGameController().setMapGeneratorOptions(mgo);
                
                c.endTransmission(in);
                
                // If (true) --> reconnect
                if (startGame) {
                    freeColClient.setSingleplayer(singleplayer);
                    freeColClient.getPreGameController().startGame();

                    if (isCurrentPlayer) {
                        freeColClient.getInGameController().setCurrentPlayer(thisPlayer);
                    }
                }
            } else if (in.getLocalName().equals("error")) {
                canvas.errorMessage(in.getAttributeValue(null, "messageID"), in.getAttributeValue(null, "message"));

                c.endTransmission(in);
                return false;
            } else {
                logger.warning("Unkown message received: " + in.getLocalName());
                c.endTransmission(in);
                return false;
            }            
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            canvas.errorMessage(null, "Could not send XML to the server.");
            try {
                c.endTransmission(in);
            } catch (IOException ie) {
                logger.warning("Exception while trying to end transmission: " + ie.toString());
            }
        }

        freeColClient.setLoggedIn(true);

        return true;
    }


    /**
    * Reconnects to the server.
    */
    public void reconnect() {
        final String username = freeColClient.getMyPlayer().getUsername();
        final String host = freeColClient.getClient().getHost();
        final int port = freeColClient.getClient().getPort();
        
        freeColClient.getCanvas().removeInGameComponents();
        logout(true);
        login(username, host, port);
        freeColClient.getInGameController().nextModelMessage();
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
        final Canvas canvas = freeColClient.getCanvas();
        final File theFile = file;

        class ErrorJob implements Runnable {
            private final  String  message;
            ErrorJob( String message ) {
                this.message = message;
            }
            public void run() {
                canvas.closeMenus();
                canvas.errorMessage( message );
            }
        };

        final boolean publicServer;
        final boolean singleplayer;
        final String name;
        final int port;
        try {
            // Get suggestions for "singleplayer" and "public game" settings from the file:
            XMLStreamReader in = FreeColServer.createXMLStreamReader(theFile);
            in.nextTag();                    
            final boolean defaultSingleplayer = Boolean.valueOf(in.getAttributeValue(null, "singleplayer")).booleanValue();
            final boolean defaultPublicServer;
            final String publicServerStr =  in.getAttributeValue(null, "publicServer");
            if (publicServerStr != null) {
                defaultPublicServer = Boolean.valueOf(publicServerStr).booleanValue();
            } else {
                defaultPublicServer = false;
            }
            in.close();
            
            final int sgo = freeColClient.getClientOptions().getInteger(ClientOptions.SHOW_SAVEGAME_SETTINGS);
            if (sgo == ClientOptions.SHOW_SAVEGAME_SETTINGS_ALWAYS
            		|| !defaultSingleplayer && sgo == ClientOptions.SHOW_SAVEGAME_SETTINGS_MULTIPLAYER) {
            	if (canvas.showLoadingSavegameDialog(defaultPublicServer, defaultSingleplayer)) {
            		LoadingSavegameDialog lsd = canvas.getLoadingSavegameDialog();
            		publicServer = lsd.isPublic();
            		singleplayer = lsd.isSingleplayer();
            		name = lsd.getName();
            		port = lsd.getPort();
            	} else {
            		return;
            	}
            } else {
            	publicServer = defaultPublicServer;
            	singleplayer = defaultSingleplayer;
            	name = null;
            	port = 3541;
            }
        } catch (FileNotFoundException e) {
            SwingUtilities.invokeLater( new ErrorJob("fileNotFound") );
            return;
        } catch (IOException e) {
            SwingUtilities.invokeLater( new ErrorJob("server.couldNotStart") );
            return;
        } catch (XMLStreamException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());                    
            SwingUtilities.invokeLater( new ErrorJob("server.couldNotStart") );
            return;
        }
        
        if (freeColClient.getFreeColServer() != null && freeColClient.getFreeColServer().getServer().getPort() == port) {
            if (freeColClient.getCanvas().showConfirmDialog("stopServer.text", "stopServer.yes", "stopServer.no")) {
                freeColClient.getFreeColServer().getController().shutdown();
            } else {
                return;
            }
        }

        canvas.showStatusPanel(Messages.message("status.loadingGame"));
        
        Runnable loadGameJob = new Runnable() {
            public void run() {
                FreeColServer freeColServer = null;
                try {                    
                    freeColServer = new FreeColServer(theFile, publicServer, singleplayer, port, name);
                    freeColClient.setFreeColServer(freeColServer);
                    final String username = freeColServer.getOwner();
                    freeColClient.setSingleplayer(singleplayer);
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            login(username, "127.0.0.1", 3541);               
                            canvas.closeStatusPanel();
                        }
                    } );                    
                } catch (FileNotFoundException e) {                    
                    final FreeColServer theFreeColServer = freeColServer;
                    SwingUtilities.invokeLater(new Runnable() {
                    	public void run() {
                            if (theFreeColServer != null) {
                            	theFreeColServer.getServer().shutdown();
                            }
                            freeColClient.getCanvas().closeMainPanel();
                          	freeColClient.getCanvas().showMainPanel();
                    	}
                    });
                    SwingUtilities.invokeLater( new ErrorJob("fileNotFound") );
                } catch (IOException e) {
                    final FreeColServer theFreeColServer = freeColServer;
                    SwingUtilities.invokeLater(new Runnable() {
                    	public void run() {
                            if (theFreeColServer != null) {
                            	theFreeColServer.getServer().shutdown();
                            }
                            freeColClient.getCanvas().closeMainPanel();
                          	freeColClient.getCanvas().showMainPanel();
                    	}
                    });
                    SwingUtilities.invokeLater( new ErrorJob("server.couldNotStart") );
                } catch (FreeColException e) {
                    final FreeColServer theFreeColServer = freeColServer;
                    SwingUtilities.invokeLater(new Runnable() {
                    	public void run() {
                            if (theFreeColServer != null) {
                            	theFreeColServer.getServer().shutdown();
                            }
                            freeColClient.getCanvas().closeMainPanel();
                          	freeColClient.getCanvas().showMainPanel();
                    	}
                    });
                    SwingUtilities.invokeLater( new ErrorJob(e.getMessage()) );                    
                }
            }
        };
        freeColClient.worker.schedule( loadGameJob );        
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

            freeColClient.getClient().sendAndWait(logoutMessage);
        }

        try {
            freeColClient.getClient().getConnection().close();
        } catch (IOException e) {
            logger.warning("Could not close connection!");
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
        final FreeColServer server = freeColClient.getFreeColServer();
        if (bStopServer && server != null) {            
            server.getController().shutdown();
            freeColClient.setFreeColServer(null);

            freeColClient.getGUI().setInGame(false);
            freeColClient.setGame(null);
            freeColClient.setMyPlayer(null);
            freeColClient.setClient(null);                
            freeColClient.setLoggedIn(false);            
        } else if (freeColClient.isLoggedIn()) {
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


    /**
    * Returns a list of vacant players on a given server.
    *
    * @param host The name of the machine running the <code>FreeColServer</code>.
    * @param port The port to use when connecting to the host.
    * @return A list of available {@link Player#getUsername() usernames}.
    */
    private List getVacantPlayers(String host, int port) {
        Connection mc;
        try {
            mc = new Connection(host, port, null);
        } catch (IOException e) {
            logger.warning("Could not connect to server.");
            return null;
        }

        List items = new ArrayList();
        Element element = Message.createNewRootElement("getVacantPlayers");
        try {
            Element reply = mc.ask(element);
            if (reply == null) {
                logger.warning("The server did not return a list.");
                return null;
            }
            if (!reply.getTagName().equals("vacantPlayers")) {
                logger.warning("The reply has an unknown type: " + reply.getTagName());
                return null;
            }
                        
            NodeList nl = reply.getChildNodes();
            for (int i=0; i<nl.getLength(); i++) {
                items.add(((Element) nl.item(i)).getAttribute("username"));
            }
        } catch (IOException e) {
            logger.warning("Could not send message to server.");
        } finally {
            try {
                mc.close();
            } catch (IOException e) {
                logger.warning("Could not close connection.");
            }
        }
                        
        return items;
    }


    /**
    * Gets a list of servers from the meta server.
    * @return A list of {@link ServerInfo} objects.
    */
    public ArrayList getServerList() {
        Canvas canvas = freeColClient.getCanvas();

        Connection mc;
        try {
            mc = new Connection(FreeCol.META_SERVER_ADDRESS, FreeCol.META_SERVER_PORT, null);
        } catch (IOException e) {
            logger.warning("Could not connect to meta-server.");
            canvas.errorMessage("metaServer.couldNotConnect");
            return null;
        }

        try {
            Element gslElement = Message.createNewRootElement("getServerList");
            Element reply = mc.ask(gslElement);
            if (reply == null) {
                logger.warning("The meta-server did not return a list.");
                canvas.errorMessage("metaServer.communicationError");
                return null;
            } else {
                ArrayList items = new ArrayList();
                NodeList nl = reply.getChildNodes();
                for (int i=0; i<nl.getLength(); i++) {
                    items.add(new ServerInfo((Element) nl.item(i)));
                }
                return items;
            }
        } catch (IOException e) {
            logger.warning("Network error while communicating with the meta-server.");
            canvas.errorMessage("metaServer.communicationError");
            return null;
        } finally {
            try {
                mc.close();
            } catch (IOException e) {
                logger.warning("Could not close connection to meta-server.");
                return null;
            }
        }
    }
}
