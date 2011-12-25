/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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


package net.sf.freecol.client.control;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.LoadingSavegameDialog;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.ServerInfo;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.util.XMLStream;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The controller responsible for starting a server and
 * connecting to it. {@link PreGameInputHandler} will be set
 * as the input handler when a successful login has been completed,
 */
public final class ConnectController {

    private static final Logger logger = Logger.getLogger(ConnectController.class.getName());

    private final FreeColClient freeColClient;

    private GUI gui;

    /**
     * Creates a new <code>ConnectController</code>.
     * @param freeColClient The main controller.
     */
    public ConnectController(FreeColClient freeColClient, GUI gui) {
        this.freeColClient = freeColClient;
        this.gui = gui;
    }


    /**
     * Starts a multiplayer server and connects to it.
     *
     * @param username The name to use when logging in.
     * @param port The port in which the server should listen for new clients.
     * @param level a <code>DifficultyLevel</code> value
     */
    public void startMultiplayerGame(Specification specification, boolean publicServer, String username, int port,
                                     Advantages advantages, OptionGroup level) {

        freeColClient.setMapEditor(false);

        if (freeColClient.isLoggedIn()) {
            logout(true);
        }

        if (freeColClient.getFreeColServer() != null &&
            freeColClient.getFreeColServer().getServer().getPort() == port) {
            if (gui.showConfirmDialog("stopServer.text",
                                                            "stopServer.yes",
                                                            "stopServer.no")) {
                freeColClient.getFreeColServer().getController().shutdown();
            } else {
                return;
            }
        }

        try {
            FreeColServer freeColServer = new FreeColServer(specification, publicServer, false, port, null, advantages);
            freeColClient.setFreeColServer(freeColServer);
        } catch (NoRouteToServerException e) {
            gui.errorMessage("server.noRouteToServer");
            return;
        } catch (IOException e) {
            gui.errorMessage("server.couldNotStart");
            return;
        }

        joinMultiplayerGame(username, "localhost", port);
    }


    /**
     * Starts a new singleplayer game by connecting to the server.
     *
     * @param specification a <code>Specification</code> value
     * @param username The name to use when logging in.
     * @param advantages an <code>Advantages</code> value
     */
    public void startSingleplayerGame(Specification specification, String username, Advantages advantages) {

        freeColClient.setMapEditor(false);

        if (freeColClient.isLoggedIn()) {
            logout(true);
        }

        // TODO: connect client/server directly (not using network-classes)
        int port = FreeCol.getDefaultPort();

        if (freeColClient.getFreeColServer() != null
            && freeColClient.getFreeColServer().getServer().getPort() == port) {
            if (gui.showConfirmDialog("stopServer.text",
                                                            "stopServer.yes",
                                                            "stopServer.no")) {
                freeColClient.getFreeColServer().getController().shutdown();
            } else {
                return;
            }
        }

        loadModFragments(specification);

        try {
            FreeColServer freeColServer = new FreeColServer(specification, false, true, port, null, advantages);
            if (freeColClient.getClientOptions().getBoolean(ClientOptions.AUTOSAVE_DELETE)) {
                FreeColServer.removeAutosaves(Messages.message("clientOptions.savegames.autosave.fileprefix"));
            }
            freeColClient.setFreeColServer(freeColServer);
        } catch (NoRouteToServerException e) {
            logger.warning("Illegal state: An exception occured that can only appear in public multiplayer games.");
            return;
        } catch (IOException e) {
            gui.errorMessage("server.couldNotStart");
            return;
        }

        freeColClient.setSingleplayer(true);

        if (login(username, "127.0.0.1", port)) {
            freeColClient.getPreGameController().setReady(true);
            gui.showStartGamePanel(freeColClient.getGame(), freeColClient.getMyPlayer(),
                                                         true);

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
        freeColClient.setMapEditor(false);

        if (freeColClient.isLoggedIn()) {
            logout(true);
        }

        List<String> vacantPlayers = getVacantPlayers(host, port);
        if (vacantPlayers != null) {
            String choice = gui.showSimpleChoiceDialog(null,
                                                          "connectController.choicePlayer",
                                                          "cancel",
                                                          vacantPlayers);
            if (choice != null) {
                username = choice;
            } else {
                return;
            }
        }

        freeColClient.setSingleplayer(false);
        if (login(username, host, port) && !freeColClient.isInGame()) {
            gui.showStartGamePanel(freeColClient.getGame(), freeColClient.getMyPlayer(), false);
        }
    }


    /**
     * Starts the client and connects to <i>host:port</i>.
     *
     * @param username The name to use when logging in. This should be a unique identifier.
     * @param host The name of the machine running the <code>FreeColServer</code>.
     * @param port The port to use when connecting to the host.
     * @return a <code>boolean</code> value
     */
    public boolean login(String username, String host, int port) {
        Client client = freeColClient.getClient();
        freeColClient.setMapEditor(false);

        if (client != null) {
            client.disconnect();
        }

        try {
            client = new Client(host, port,
                                freeColClient.getPreGameInputHandler(),
                                FreeCol.CLIENT_THREAD + username);
        } catch (ConnectException e) {
            gui.errorMessage("server.couldNotConnect");
            return false;
        } catch (IOException e) {
            gui.errorMessage("server.couldNotConnect");
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
                String activeUnitId = in.getAttributeValue(null, "activeUnit");

                in.nextTag();
                Game game = new Game(in, username);

                // this completes the client's view of the spec with options obtained from the server difficulty
                // it should not be required in the client, to be removed later, when newTurn() only runs in the server

                Player thisPlayer = game.getPlayerByName(username);

                freeColClient.setGame(game);
                freeColClient.setMyPlayer(thisPlayer);

                freeColClient.getActionManager().addSpecificationActions(game.getSpecification());

                c.endTransmission(in);

                // If (true) --> reconnect
                if (startGame) {
                    Tile entryTile = thisPlayer.getEntryLocation().getTile();
                    freeColClient.setSingleplayer(singleplayer);
                    freeColClient.getPreGameController().startGame();

                    if (isCurrentPlayer) {
                        freeColClient.getInGameController()
                            .setCurrentPlayer(thisPlayer);
                        if (activeUnitId != null) {
                            Unit active = (Unit) freeColClient.getGame().getFreeColGameObject(activeUnitId);
                            if (active != null) {
                                active.getOwner().resetIterators();
                                active.getOwner().setNextActiveUnit(active);
                                gui.setActiveUnit(active);
                            }
                            
                        } else {
                            gui.setSelectedTile(entryTile, false);
                        }
                    } else {
                        gui.setSelectedTile(entryTile, false);
                    }
                    gui.setSelectedTile(thisPlayer
                        .getEntryLocation().getTile(), false);
                }
            } else if (in.getLocalName().equals("error")) {
                gui.errorMessage(in.getAttributeValue(null, "messageID"), in.getAttributeValue(null, "message"));

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
            gui.errorMessage(null, "Could not send XML to the server.");
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
        final String username = freeColClient.getMyPlayer().getName();
        final String host = freeColClient.getClient().getHost();
        final int port = freeColClient.getClient().getPort();

        gui.getCanvas().removeInGameComponents();
        logout(true);
        login(username, host, port);
        freeColClient.getInGameController().nextModelMessage();
    }


    /**
    * Opens a dialog where the user should specify the filename
    * and loads the game.
    */
    public void loadGame() {
        File file = gui.showLoadDialog(FreeCol.getSaveDirectory());
        if (file != null) {
            //FreeCol.setSaveDirectory(file.getParentFile());
            loadGame(file);
        }
    }

    /**
     * Loads a game from the given file.
     *
     * @param file The <code>File</code>.
     */
    public void loadGame(File file) {
        final File theFile = file;

        freeColClient.setMapEditor(false);

        class ErrorJob implements Runnable {
            private final String message;
            ErrorJob( String message ) {
                this.message = message;
            }
            public void run() {
                gui.closeMenus();
                gui.errorMessage( message );
            }
        }

        final boolean singleplayer;
        final String name;
        final int port;
        XMLStream xs = null;
        try {
            // Get suggestions for "singleplayer" and "public game" settings from the file:
            final FreeColSavegameFile fis = new FreeColSavegameFile(theFile);
            xs = new XMLStream(fis.getSavegameInputStream());
            final XMLStreamReader in = xs.getXMLStreamReader();
            in.nextTag();
            final boolean defaultSingleplayer = Boolean.valueOf(in.getAttributeValue(null, "singleplayer")).booleanValue();
            final boolean defaultPublicServer;
            final String publicServerStr =  in.getAttributeValue(null, "publicServer");
            if (publicServerStr != null) {
                defaultPublicServer = Boolean.valueOf(publicServerStr).booleanValue();
            } else {
                defaultPublicServer = false;
            }
            xs.close();

            // Reload the client options saved with this game.
            try {
                ClientOptions options = freeColClient.getClientOptions();
                options.updateOptions(fis.getInputStream(FreeColSavegameFile.CLIENT_OPTIONS));
                options.fixClientOptions();
            } catch (FileNotFoundException e) {
                // no client options, we don't care
            }
            final int sgo = freeColClient.getClientOptions().getInteger(ClientOptions.SHOW_SAVEGAME_SETTINGS);
            if (sgo == ClientOptions.SHOW_SAVEGAME_SETTINGS_ALWAYS
                    || !defaultSingleplayer && sgo == ClientOptions.SHOW_SAVEGAME_SETTINGS_MULTIPLAYER) {
                if (gui.getCanvas().showLoadingSavegameDialog(defaultPublicServer, defaultSingleplayer)) {
                    LoadingSavegameDialog lsd = gui.getCanvas().getLoadingSavegameDialog();
                    singleplayer = lsd.isSingleplayer();
                    name = lsd.getName();
                    port = lsd.getPort();
                } else {
                    return;
                }
            } else {
                singleplayer = defaultSingleplayer;
                name = null;
                port = FreeCol.getDefaultPort();
            }
        } catch (FileNotFoundException e) {
            SwingUtilities.invokeLater( new ErrorJob("fileNotFound") );
            return;
        } catch (IOException e) {
            SwingUtilities.invokeLater( new ErrorJob("server.couldNotStart") );
            return;
        } catch (NullPointerException e) {
            SwingUtilities.invokeLater( new ErrorJob("couldNotLoadGame") );
            return;
        } catch (XMLStreamException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            SwingUtilities.invokeLater( new ErrorJob("server.couldNotStart") );
            return;
        } finally {
            if (xs != null) {
                xs.close();
            }
        }

        if (freeColClient.getFreeColServer() != null && freeColClient.getFreeColServer().getServer().getPort() == port) {
            if (gui.showConfirmDialog("stopServer.text", "stopServer.yes", "stopServer.no")) {
                freeColClient.getFreeColServer().getController().shutdown();
            } else {
                return;
            }
        }

        gui.showStatusPanel(Messages.message("status.loadingGame"));

        Runnable loadGameJob = new Runnable() {
            public void run() {
                FreeColServer freeColServer = null;
                try {
                    final FreeColSavegameFile savegame = new FreeColSavegameFile(theFile);
                    freeColServer = new FreeColServer(savegame, port, name);
                    freeColClient.setFreeColServer(freeColServer);
                    final String username = freeColServer.getOwner();
                    freeColClient.setSingleplayer(singleplayer);
                    freeColClient.getInGameController().setGameConnected();
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            ResourceManager.setScenarioMapping(savegame.getResourceMapping());
                            login(username, "127.0.0.1", FreeCol.getDefaultPort());
                            gui.closeStatusPanel();
                        }
                    } );
                } catch (NoRouteToServerException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            gui.closeMainPanel();
                            gui.getCanvas().showMainPanel();
                        }
                    });
                    SwingUtilities.invokeLater( new ErrorJob("server.noRouteToServer") );
                } catch (FileNotFoundException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            gui.closeMainPanel();
                            gui.getCanvas().showMainPanel();
                        }
                    });
                    SwingUtilities.invokeLater( new ErrorJob("fileNotFound") );
                } catch (IOException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            gui.closeMainPanel();
                            gui.getCanvas().showMainPanel();
                        }
                    });
                    SwingUtilities.invokeLater( new ErrorJob("server.couldNotStart") );
                } catch (FreeColException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            gui.closeMainPanel();
                            gui.getCanvas().showMainPanel();
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
            Element logoutMessage = DOMMessage.createNewRootElement("logout");
            logoutMessage.setAttribute("reason", "User has quit the client.");

            freeColClient.getClient().sendAndWait(logoutMessage);
        }

        try {
            freeColClient.getClient().getConnection().close();
        } catch (IOException e) {
            logger.warning("Could not close connection!");
        }

        ResourceManager.setScenarioMapping(null);
        ResourceManager.setCampaignMapping(null);

        if (!freeColClient.isHeadless()) {
            freeColClient.setInGame(false);
        }
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

            ResourceManager.setScenarioMapping(null);
            ResourceManager.setCampaignMapping(null);
            freeColClient.setInGame(false);
            freeColClient.setGame(null);
            freeColClient.setMyPlayer(null);
            freeColClient.setIsRetired(false);
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
    * @return A list of available {@link Player#getName() usernames}.
    */
    private List<String> getVacantPlayers(String host, int port) {
        Connection mc;
        try {
            mc = new Connection(host, port, null, FreeCol.CLIENT_THREAD);
        } catch (IOException e) {
            logger.warning("Could not connect to server.");
            return null;
        }

        ArrayList<String> items = new ArrayList<String>();
        Element element = DOMMessage.createNewRootElement("getVacantPlayers");
        try {
            Element reply = mc.askDumping(element);
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
    public ArrayList<ServerInfo> getServerList() {
        Connection mc;
        try {
            mc = new Connection(FreeCol.META_SERVER_ADDRESS, FreeCol.META_SERVER_PORT, null, FreeCol.CLIENT_THREAD);
        } catch (IOException e) {
            logger.warning("Could not connect to meta-server.");
            gui.errorMessage("metaServer.couldNotConnect");
            return null;
        }

        try {
            Element gslElement = DOMMessage.createNewRootElement("getServerList");
            Element reply = mc.askDumping(gslElement);
            if (reply == null) {
                logger.warning("The meta-server did not return a list.");
                gui.errorMessage("metaServer.communicationError");
                return null;
            } else {
                ArrayList<ServerInfo> items = new ArrayList<ServerInfo>();
                NodeList nl = reply.getChildNodes();
                for (int i=0; i<nl.getLength(); i++) {
                    items.add(new ServerInfo((Element) nl.item(i)));
                }
                return items;
            }
        } catch (IOException e) {
            logger.warning("Network error while communicating with the meta-server.");
            gui.errorMessage("metaServer.communicationError");
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

    private void loadModFragments(Specification specification) {
        boolean loadedMod = false;
        for (FreeColModFile f : freeColClient.getClientOptions()
                 .getActiveMods()) {
            InputStream sis = null;
            try {
                sis = f.getSpecificationInputStream();
            } catch (IOException ioe) {
                logger.warning("IO error in mod fragment " + f.getId()
                    + ": " + ioe.getMessage());
            }
            if (sis != null) {
                try {
                    specification.loadFragment(sis);
                    loadedMod = true;
                    logger.info("Loaded mod fragment " + f.getId());
                } catch (RuntimeException rte) {
                    logger.warning("Parse error in mod fragment " + f.getId()
                        + ": " + rte.getMessage());
                }
            }
        }
        if (loadedMod) { // Update actions in case new ones loaded.
            freeColClient.getActionManager().update();
        }
    }
}
