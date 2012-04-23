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


package net.sf.freecol.client.control;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
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
import net.sf.freecol.common.networking.LoginMessage;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.util.XMLStream;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The controller responsible for starting a server and connecting to it.
 * {@link PreGameInputHandler} will be set as the input handler when a
 * successful login has been completed,
 */
public final class ConnectController {

    private static final Logger logger = Logger.getLogger(ConnectController.class.getName());

    private final FreeColClient freeColClient;

    private GUI gui;


    /**
     * Creates a new <code>ConnectController</code>.
     *
     * @param freeColClient The main client controller.
     */
    public ConnectController(FreeColClient freeColClient, GUI gui) {
        this.freeColClient = freeColClient;
        this.gui = gui;
    }

    
    /**
     * Shut down an existing server on a given port.
     *
     * @param port The port to unblock.
     * @return True if there should be no blocking server remaining.
     */
    private boolean unblockServer(int port) {
        FreeColServer freeColServer = freeColClient.getFreeColServer();
        if (freeColServer != null
            && freeColServer.getServer().getPort() == port) {
            if (gui.showConfirmDialog("stopServer.text",
                                      "stopServer.yes", "stopServer.no")) {
                freeColServer.getController().shutdown();
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Starts a multiplayer server and connects to it.
     *
     * @param specification The <code>Specification</code> for the game.
     * @param publicServer Whether to make the server public.
     * @param userName The name to use when logging in.
     * @param port The port in which the server should listen for new clients.
     * @param advantages The national <code>Advantages</code>.
     * @param level An <code>OptionGroup</code> containing difficulty options.
     */
    public void startMultiplayerGame(Specification specification,
                                     boolean publicServer,
                                     String userName, int port,
                                     Advantages advantages,
                                     OptionGroup level) {
        freeColClient.setMapEditor(false);

        if (freeColClient.isLoggedIn()) logout(true);

        if (!unblockServer(port)) return;

        FreeColServer freeColServer;
        try {
            freeColServer = new FreeColServer(specification, publicServer,
                                              false, port, null, advantages);
        } catch (NoRouteToServerException e) {
            gui.errorMessage("server.noRouteToServer");
            logger.log(Level.WARNING, "No route to server.", e);
            return;
        } catch (IOException e) {
            gui.errorMessage("server.couldNotStart");
            logger.log(Level.WARNING, "Could not start server.", e);
            return;
        }

        freeColClient.setFreeColServer(freeColServer);
        joinMultiplayerGame(userName, "localhost", port);
    }

    /**
     * Load current mod fragments into the specification.
     *
     * @param specification The <code>Specification</code> to load into.
     */
    private void loadModFragments(Specification specification) {
        boolean loadedMod = false;
        for (FreeColModFile f : freeColClient.getClientOptions()
                 .getActiveMods()) {
            InputStream sis = null;
            try {
                sis = f.getSpecificationInputStream();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "IO error in mod fragment "
                    + f.getId(), ioe);
            }
            if (sis != null) {
                try {
                    specification.loadFragment(sis);
                    loadedMod = true;
                    logger.info("Loaded mod fragment " + f.getId());
                } catch (RuntimeException rte) {
                    logger.log(Level.WARNING, "Parse error in mod fragment "
                        + f.getId(), rte);
                }
            }
        }
        if (loadedMod) { // Update actions in case new ones loaded.
            freeColClient.getActionManager().update();
        }
    }

    /**
     * Starts a new singleplayer game by connecting to the server.
     * TODO: connect client/server directly (not using network-classes)
     *
     * @param specification The <code>Specification</code> for the game.
     * @param userName The name to use when logging in.
     * @param advantages The national <code>Advantages</code>.
     */
    public void startSingleplayerGame(Specification specification,
                                      String userName, Advantages advantages) {
        freeColClient.setMapEditor(false);

        if (freeColClient.isLoggedIn()) logout(true);

        int port = FreeCol.getDefaultPort();
        if (!unblockServer(port)) return;

        loadModFragments(specification);

        FreeColServer freeColServer;
        try {
            freeColServer = new FreeColServer(specification, false,
                                              true, port, null, advantages);
        } catch (NoRouteToServerException e) {
            gui.errorMessage("server.noRouteToServer");
            logger.log(Level.WARNING, "No route to server (single player!).",
                e);
            return;
        } catch (IOException e) {
            gui.errorMessage("server.couldNotStart");
            logger.log(Level.WARNING, "Could not start server.", e);
            return;
        }

        if (freeColClient.getClientOptions()
            .getBoolean(ClientOptions.AUTOSAVE_DELETE)) {
            FreeColServer.removeAutosaves(Messages.message("clientOptions.savegames.autosave.fileprefix"));
        }
        freeColClient.setFreeColServer(freeColServer);
        freeColClient.setSingleplayer(true);
        if (login(userName, "127.0.0.1", freeColServer.getPort())) {
            freeColClient.getPreGameController().setReady(true);
            gui.showStartGamePanel(freeColClient.getGame(),
                                   freeColClient.getMyPlayer(), true);
        }
    }

    /**
     * Starts a new multiplayer game by connecting to the server.
     *
     * @param userName The name to use when logging in.
     * @param host The name of the machine running the server.
     * @param port The port to use when connecting to the host.
     */
    public void joinMultiplayerGame(String userName, String host, int port) {
        freeColClient.setMapEditor(false);

        if (freeColClient.isLoggedIn()) logout(true);

        List<String> vacantPlayers = getVacantPlayers(host, port);
        if (vacantPlayers != null) {
            String choice = gui.showSimpleChoiceDialog(null,
                "connectController.choicePlayer", "cancel",
                vacantPlayers);
            if (choice == null) return;
            userName = choice;
        }

        freeColClient.setSingleplayer(false);
        if (login(userName, host, port) && !freeColClient.isInGame()) {
            gui.showStartGamePanel(freeColClient.getGame(),
                                   freeColClient.getMyPlayer(), false);
        }
    }

    /**
     * Connects a client to host:port (or more).
     *
     * @param threadName The name for the thread.
     * @param host The name of the machine running the
     *            <code>FreeColServer</code>.
     * @param port The port to use when connecting to the host.
     * @return The client.
     * @throws ConnectException
     * @throws IOException
     */
    private Client connectClient(String threadName, String host, int port)
        throws ConnectException, IOException {
        Client client = null;
        int tries;
        if (port < 0) {
            port = FreeCol.getDefaultPort();
            tries = 10;
        } else {
            tries = 1;
        }
        for (int i = tries; i > 0; i--) {
            try {
                client = new Client(host, port,
                                    freeColClient.getPreGameInputHandler(),
                                    threadName);
                if (client != null) break;
            } catch (ConnectException e) {
                if (i == 1) throw e;
            } catch (IOException e) {
                if (i == 1) throw e;
            }
        }
        return client;
    }

    /**
     * Starts the client and connects to <i>host:port</i>.
     *
     * @param userName The name to use when logging in. This should be
     *            a unique identifier.
     * @param host The name of the machine running the
     *            <code>FreeColServer</code>.
     * @param port The port to use when connecting to the host.
     * @return True if the login succeeds.
     */
    public boolean login(String userName, String host, int port) {
        freeColClient.setMapEditor(false);

        Client client = freeColClient.getClient();
        if (client != null) client.disconnect();

        try {
            client = connectClient(FreeCol.CLIENT_THREAD + userName,
                                   host, port);
        } catch (Exception e) {
            gui.errorMessage("server.couldNotConnect", e.getMessage());
            return false;
        }
        freeColClient.setClient(client);

        LoginMessage msg = freeColClient.askServer()
            .login(userName, FreeCol.getVersion());
        Game game;
        if (msg == null || (game = msg.getGame()) == null) return false;

        // This completes the client's view of the spec with options
        // obtained from the server difficulty.  It should not be
        // required in the client, to be removed later, when newTurn()
        // only runs in the server
        freeColClient.setGame(game);
        Player player = game.getPlayerByName(userName);
        if (player == null) {
            logger.warning("New game does not contain player: " + userName);
            return false;
        }
        freeColClient.setMyPlayer(player);
        freeColClient.getActionManager()
            .addSpecificationActions(game.getSpecification());
        logger.info("FreeColClient logged in as " + userName
                    + "/" + player.getId());

        // Reconnect
        if (msg.getStartGame()) {
            Tile entryTile = player.getEntryLocation().getTile();
            freeColClient.setSingleplayer(msg.isSinglePlayer());
            freeColClient.getPreGameController().startGame();

            if (msg.isCurrentPlayer()) {
                freeColClient.getInGameController()
                    .setCurrentPlayer(player);
                Unit activeUnit = msg.getActiveUnit();
                if (activeUnit != null) {
                    activeUnit.getOwner().resetIterators();
                    activeUnit.getOwner().setNextActiveUnit(activeUnit);
                    gui.setActiveUnit(activeUnit);
                } else {
                    gui.setSelectedTile(entryTile, false);
                }
            } else {
                gui.setSelectedTile(player.getEntryLocation().getTile(), false);
            }
        }

        // All done.
        freeColClient.setLoggedIn(true);
        return true;
    }

    /**
     * Reconnects to the server.
     */
    public void reconnect() {
        final String userName = freeColClient.getMyPlayer().getName();
        final String host = freeColClient.getClient().getHost();
        final int port = freeColClient.getClient().getPort();

        gui.removeInGameComponents();
        logout(true);
        login(userName, host, port);
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
            // Get suggestions for "singleplayer" and "publicServer"
            // settings from the file
            final FreeColSavegameFile fis = new FreeColSavegameFile(theFile);
            xs = new XMLStream(fis.getSavegameInputStream());
            final XMLStreamReader in = xs.getXMLStreamReader();
            in.nextTag();
            String str = in.getAttributeValue(null, "singleplayer");
            final boolean defaultSinglePlayer = str != null
                && Boolean.valueOf(str).booleanValue();
            str = in.getAttributeValue(null, "publicServer");
            final boolean defaultPublicServer = str != null
                && Boolean.valueOf(str).booleanValue();
            xs.close();

            // Reload the client options saved with this game.
            try {
                ClientOptions options = freeColClient.getClientOptions();
                options.updateOptions(fis.getInputStream(FreeColSavegameFile.CLIENT_OPTIONS));
                options.fixClientOptions();
            } catch (FileNotFoundException e) {
                // no client options, we don't care
            }

            final int sgo = freeColClient.getClientOptions()
                .getInteger(ClientOptions.SHOW_SAVEGAME_SETTINGS);
            if (sgo == ClientOptions.SHOW_SAVEGAME_SETTINGS_ALWAYS
                || !defaultSinglePlayer
                && sgo == ClientOptions.SHOW_SAVEGAME_SETTINGS_MULTIPLAYER) {
                if (gui.showLoadingSavegameDialog(defaultPublicServer,
                                                  defaultSinglePlayer)) {
                    LoadingSavegameDialog lsd = gui.getLoadingSavegameDialog();
                    singleplayer = lsd.isSingleplayer();
                    name = lsd.getName();
                    port = lsd.getPort();
                } else {
                    return;
                }
            } else {
                singleplayer = defaultSinglePlayer;
                name = null;
                port = -1;
            }
        } catch (FileNotFoundException e) {
            SwingUtilities.invokeLater(new ErrorJob("fileNotFound"));
            logger.log(Level.WARNING, "Can not find file: " + file.getName(),
                e);
            return;
        } catch (IOException e) {
            SwingUtilities.invokeLater(new ErrorJob("server.couldNotStart"));
            logger.log(Level.WARNING, "Could not start server.", e);
            return;
        } catch (XMLStreamException e) {
            logger.log(Level.WARNING, "Error reading game from: "
                + file.getName(), e);
            SwingUtilities.invokeLater( new ErrorJob("server.couldNotStart") );
            return;
        } catch (Exception e) {
            SwingUtilities.invokeLater(new ErrorJob("couldNotLoadGame"));
            logger.log(Level.WARNING, "Could not load game from: "
                + file.getName(), e);
            return;
        } finally {
            if (xs != null) xs.close();
        }

        if (!unblockServer(port)) return;
        gui.showStatusPanel(Messages.message("status.loadingGame"));

        Runnable loadGameJob = new Runnable() {
            public void run() {
                FreeColServer freeColServer = null;
                try {
                    final FreeColSavegameFile saveGame
                        = new FreeColSavegameFile(theFile);
                    freeColServer = new FreeColServer(saveGame, port, name);
                    freeColClient.setFreeColServer(freeColServer);
                    final String userName = freeColServer.getOwner();
                    final int port = freeColServer.getPort();
                    freeColClient.setSingleplayer(singleplayer);
                    freeColClient.getInGameController().setGameConnected();
                    SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                ResourceManager.setScenarioMapping(saveGame.getResourceMapping());
                                login(userName, "127.0.0.1", port);
                                gui.closeStatusPanel();
                            }
                        });
                } catch (NoRouteToServerException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                gui.closeMainPanel();
                                gui.showMainPanel();
                            }
                        });
                    SwingUtilities.invokeLater(new ErrorJob("server.noRouteToServer"));
                    logger.log(Level.WARNING, "No route to server.", e);
                } catch (FileNotFoundException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                gui.closeMainPanel();
                                gui.showMainPanel();
                            }
                        });
                    SwingUtilities.invokeLater(new ErrorJob("fileNotFound"));
                    logger.log(Level.WARNING, "Can not find file.", e);
                } catch (IOException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                gui.closeMainPanel();
                                gui.showMainPanel();
                            }
                        });
                    SwingUtilities.invokeLater(new ErrorJob("server.couldNotStart"));
                    logger.log(Level.WARNING, "Error starting game.", e);
                } catch (FreeColException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                gui.closeMainPanel();
                                gui.showMainPanel();
                            }
                        });
                    SwingUtilities.invokeLater(new ErrorJob(e.getMessage()));
                    logger.log(Level.WARNING, "FreeCol error starting game.",
                        e);
                }
            }
        };
        freeColClient.worker.schedule(loadGameJob);
    }

    /**
     * Sends a logout message to the server.
     *
     * @param notifyServer Whether or not the server should be
     *     notified of the logout.  For example: if the server kicked us
     *     out then we don't need to confirm with a logout message.
     */
    public void logout(boolean notifyServer) {
        if (notifyServer) {
            Element logoutElement = DOMMessage.createNewRootElement("logout");
            logoutElement.setAttribute("reason", "User has quit the client.");
            freeColClient.getClient().sendAndWait(logoutElement);
        }

        try {
            freeColClient.getClient().getConnection().close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not close connection!", e);
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
     * Quits the current game. If a server is running it will be
     * stopped if bStopServer is <i>true</i>.  If a server is running
     * through this client and bStopServer is true then the clients
     * connected to that server will be notified. If a local client is
     * connected to a server then the server will be notified with a
     * logout in case <i>notifyServer</i> is true.
     *
     * @param bStopServer Indicates whether or not a server that was
     *     started through this client should be stopped.
     *
     * @param notifyServer Whether or not the server should be
     *     notified of the logout.  For example: if the server kicked us
     *     out then we don't need to confirm with a logout message.
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
     * Quits the current game. If a server is running it will be
     * stopped if bStopServer is <i>true</i>.  The server and perhaps
     * the clients (if a server is running through this client and
     * bStopServer is true) will be notified.
     *
     * @param bStopServer Indicates whether or not a server that was
     *     started through this client should be stopped.
     */
    public void quitGame(boolean bStopServer) {
        quitGame(bStopServer, true);
    }

    /**
     * Returns a list of vacant players on a given server.
     *
     * @param host The name of the machine running the
     *     <code>FreeColServer</code>.
     * @param port The port to use when connecting to the host.
     * @return A list of available {@link Player#getName() user names}.
     */
    private List<String> getVacantPlayers(String host, int port) {
        Connection mc;
        try {
            mc = new Connection(host, port, null, FreeCol.CLIENT_THREAD);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not connect to server.", e);
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
                logger.warning("The reply has an unknown type: "
                    + reply.getTagName());
                return null;
            }

            NodeList nl = reply.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                items.add(((Element)nl.item(i)).getAttribute("username"));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not send message to server.", e);
        } finally {
            try {
                mc.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not close connection.", e);
            }
        }

        return items;
    }

    /**
     * Gets a list of servers from the meta server.
     *
     * @return A list of {@link ServerInfo} objects.
     */
    public ArrayList<ServerInfo> getServerList() {
        Connection mc;
        try {
            mc = new Connection(FreeCol.META_SERVER_ADDRESS,
                                FreeCol.META_SERVER_PORT, null,
                                FreeCol.CLIENT_THREAD);
        } catch (IOException e) {
            gui.errorMessage("metaServer.couldNotConnect");
            logger.log(Level.WARNING, "Could not connect to meta-server.", e);
            return null;
        }

        try {
            Element gslElement = DOMMessage.createNewRootElement("getServerList");
            Element reply = mc.askDumping(gslElement);
            if (reply == null) {
                gui.errorMessage("metaServer.communicationError");
                logger.warning("The meta-server did not return a list.");
                return null;
            } else {
                ArrayList<ServerInfo> items = new ArrayList<ServerInfo>();
                NodeList nl = reply.getChildNodes();
                for (int i = 0; i < nl.getLength(); i++) {
                    items.add(new ServerInfo((Element)nl.item(i)));
                }
                return items;
            }
        } catch (IOException e) {
            gui.errorMessage("metaServer.communicationError");
            logger.log(Level.WARNING, "Network error with meta-server.", e);
            return null;
        } finally {
            try {
                mc.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not close meta-server.", e);
                return null;
            }
        }
    }
}
