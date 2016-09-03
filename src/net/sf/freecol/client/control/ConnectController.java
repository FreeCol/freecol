/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol; 
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.LoadingSavegameInfo;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.ServerInfo;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.GameStateMessage;
import net.sf.freecol.common.networking.LoginMessage;
import net.sf.freecol.common.networking.ServerListMessage;
import net.sf.freecol.common.networking.VacantPlayersMessage;
import net.sf.freecol.common.resources.ResourceManager;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.FreeColServer.GameState;


/**
 * The controller responsible for starting a server and connecting to it.
 * {@link PreGameInputHandler} will be set as the input handler when a
 * successful login has been completed,
 */
public final class ConnectController extends FreeColClientHolder {

    private static final Logger logger = Logger.getLogger(ConnectController.class.getName());


    /**
     * Creates a new {@code ConnectController}.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ConnectController(FreeColClient freeColClient) {
        super(freeColClient);
    }


    /**
     * The game is finishing.  Release/unhook everything.
     */
    private void finish() {
        ResourceManager.setScenarioMapping(null);

        final FreeColClient fcc = getFreeColClient();
        if (!fcc.isHeadless()) fcc.setInGame(false);
        fcc.setGame(null);
        fcc.setMyPlayer(null);
        askServer().reset();
        fcc.setLoggedIn(false);
    }

    /**
     * Shut down an existing server on a given port.
     *
     * @param port The port to unblock.
     * @return True if there should be no blocking server remaining.
     */
    private boolean unblockServer(int port) {
        FreeColServer freeColServer = getFreeColServer();
        if (freeColServer != null
            && freeColServer.getServer().getPort() == port) {
            if (getGUI().confirm("stopServer.text", "stopServer.yes",
                                 "stopServer.no")) {
                freeColServer.getController().shutdown();
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Start a server.
     *
     * @param publicServer If true, add to the meta-server.
     * @param singlePlayer True if this is a single player game.
     * @param spec The {@code Specification} to use in this game.
     * @param port The TCP port to use for the public socket.
     * @return A new {@code FreeColServer} or null on error.
     */
    private FreeColServer startServer(boolean publicServer,
        boolean singlePlayer, Specification spec, int port) {
        FreeColServer freeColServer;
        try {
            freeColServer = new FreeColServer(publicServer, singlePlayer,
                                              spec, port, null);
        } catch (IOException e) {
            freeColServer = null;
            getGUI().showErrorMessage(StringTemplate
                .template("server.initialize"));
            logger.log(Level.WARNING, "Could not start server.", e);
        }
        if (publicServer && freeColServer != null
            && !freeColServer.getPublicServer()) {
            getGUI().showErrorMessage(StringTemplate
                .template("server.noRouteToServer"));
        }
        return freeColServer;
    }

    /**
     * Ask the server a question, but do not make a persistent
     * connection yet.
     *
     * Handle showing error messages on the GUI.  Only simple messages
     * will work here.
     *
     * @param host The name of the machine running the
     *     {@code FreeColServer}.
     * @param port The port to use when connecting to the host.
     * @param query The {@code DOMMessage} query to send.
     * @param replyTag The expected tag of the reply, or null for anything.
     * @param err An optional {@code StringTemplate} override for any
     *     error messages.
     * @return The reply message matching the specified tag, or null on error.
     */
    private DOMMessage ask(String host, int port, DOMMessage query,
                           String replyTag, StringTemplate err) {
        DOMMessage reply;
        try (
            Connection c = new Connection(host, port, null,
                                          FreeCol.CLIENT_THREAD)
        ) {
            reply = c.ask(getGame(), query, replyTag);
        } catch (IOException ioe) {
            getGUI().showErrorMessage(FreeCol.errorFromException(ioe,
                    "server.couldNotConnect"));
            logger.log(Level.WARNING, "Could not connect to " + host
                + ":" + port, ioe);
            return null;
        }
        if (reply == null) {
            ;
        } else if (replyTag == null || reply.isType(replyTag)) {
            return reply;
        } else if (reply.isType(ErrorMessage.TAG)) {
            ErrorMessage em = (ErrorMessage)reply;
            if (err != null) em.setTemplate(err);
            getGUI().showErrorMessage(em.getTemplate(), em.getMessage());
        } else {
            throw new IllegalStateException("Bogus tag: " + reply.getType());
        }
        return null;
    }
        
    /**
     * Starts the client and connects to <i>host:port</i>.
     *
     * Public for the test suite.
     *
     * @param user The name of the player to use.
     * @param host The name of the machine running the
     *            {@code FreeColServer}.
     * @param port The port to use when connecting to the host.
     * @return Null if the login succeeds, otherwise an error message template.
     */
    public StringTemplate login(String user, String host, int port) {
        final FreeColClient fcc = getFreeColClient();
        fcc.setMapEditor(false);
 
        try {
            askServer().disconnect();
        } catch (IOException ioe) {} // Ignore            

        // Establish the full connection here
        StringTemplate err = null;
        String message = null;
        try {
            if (!askServer().connect(FreeCol.CLIENT_THREAD + user,
                                     host, port,
                                     fcc.getPreGameInputHandler())) {
                err = StringTemplate.template("server.couldNotConnect");
            }
        } catch (Exception ex) {
            err = FreeCol.errorFromException(ex, "server.couldNotConnect");
        }
        if (err != null) return err;
        logger.info("Connected to " + host + ":" + port);

        // Ask the server to log in a player with the given user name
        // and return the game with the player inside.
        // The work is done in PGIH.login().
        Game game;
        return (!askServer().login(user, FreeCol.getVersion()))
            ? StringTemplate.template("server.couldNotLogin")
            : ((game = getGame()) == null)
            ? StringTemplate.template("server.noSuchGame")
            : (getMyPlayer() == null)
            ? StringTemplate.template("server.noSuchPlayer")
                .addName("%player%", user)
            : null;
    }

    //
    // There are several ways to start a game.
    // - multi-player
    // - single player
    // - restore from saved game
    //
    // They all ultimately have to establish a connection to the server,
    // and get the game from there, which is done in {@link #login()}.
    //
    // When restoring from saved we are mostly done at this point and
    // the game will begin.  Otherwise we may still need to select a
    // nation and change game or map options, which is done in the
    // {@link StartGamePanel}.  The start game panel can then send a
    // requestLaunch message which will tell the server to generate
    // the game and map with the required parameters.  The updated
    // game is sent to all clients with an "update" message.
    //
    // The server then tells the clients that the game is starting
    // with a "startGame" message.  Except for saved games where it
    // cheats and sets a "startGame" flag in the login response that
    // short circuits this.  FIXME: which is awkward, tidy?
    //
    // "startGame" eventually ends up in FreeColServer.startGame,
    // where the inGame state is finally set to true, and the game
    // begins.
    //

    /**
     * Starts a multiplayer server and connects to it.
     *
     * @param specification The {@code Specification} for the game.
     * @param publicServer Whether to make the server public.
     * @param port The port in which the server should listen for new clients.
     * @return True if the game is started successfully.
     */
    public boolean startMultiplayerGame(Specification specification,
                                        boolean publicServer, int port) {
        final FreeColClient fcc = getFreeColClient();
        fcc.setMapEditor(false);

        if (fcc.isLoggedIn()) logout(true);

        if (!unblockServer(port)) return false;

        FreeColServer freeColServer = startServer(publicServer, false,
                                                  specification, port);
        if (freeColServer == null) return false;

        fcc.setFreeColServer(freeColServer);
        return joinMultiplayerGame(freeColServer.getHost(),
                                   freeColServer.getPort());
    }

    /**
     * Join an existing multiplayer game.
     *
     * @param host The name of the machine running the server.
     * @param port The port to use when connecting to the host.
     * @return True if the game starts successfully.
     */
    public boolean joinMultiplayerGame(String host, int port) {
        final FreeColClient fcc = getFreeColClient();
        fcc.setMapEditor(false);

        if (fcc.isLoggedIn()) logout(true);

        DOMMessage msg = ask(host, port, new GameStateMessage(),
            GameStateMessage.TAG, StringTemplate.template("client.noState"));
        GameState state = (msg instanceof GameStateMessage)
            ? ((GameStateMessage)msg).getGameState()
            : null;        
        if (state == null) return false;

        StringTemplate err;
        switch (state) {
        case STARTING_GAME:
            if ((err = login(FreeCol.getName(), host, port)) != null) {
                getGUI().showErrorMessage(err);
                return false;
            }
            fcc.setLoggedIn(true);
            getGUI().showStartGamePanel(getGame(), getMyPlayer(), false);
            break;

        case IN_GAME:
            // Disable this check if you need to debug a multiplayer client.
            if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
                getGUI().showErrorMessage(StringTemplate
                    .template("client.debugConnect"));
                return false;
            }
            msg = ask(host, port, new VacantPlayersMessage(),
                      VacantPlayersMessage.TAG,
                      StringTemplate.template("client.noPlayers"));
            List<String> names = (msg instanceof VacantPlayersMessage)
                ? ((VacantPlayersMessage)msg).getVacantPlayers()
                : null;
            if (names == null || names.isEmpty()) return false;

            String choice = getGUI().getChoice(null,
                Messages.message("client.choicePlayer"),
                "cancel",
                transform(names, alwaysTrue(), n ->
                    new ChoiceItem<>(Messages.message(StringTemplate
                            .template("countryName")
                            .add("%nation%", Messages.nameKey(n))), n)));
            if (choice == null) return false; // User cancelled

            if ((err = login(Messages.getRulerName(choice), host, port)) != null) {
                getGUI().showErrorMessage(err);
                return false;
            }
            fcc.setLoggedIn(true);
            break;

        case ENDING_GAME: default:
            getGUI().showErrorMessage(StringTemplate.template("client.ending"));
            return false;
        }
        return true;
    }

    /**
     * Starts a new single player game by connecting to the server.
     *
     * FIXME: connect client/server directly (not using network-classes)
     *
     * @param spec The {@code Specification} for the game.
     * @param skip Skip the start game panel.
     * @return True if the game starts successfully.
     */
    public boolean startSinglePlayerGame(Specification spec, boolean skip) {
        final FreeColClient fcc = getFreeColClient();
        fcc.setMapEditor(false);

        if (fcc.isLoggedIn()) logout(true);

        if (!unblockServer(FreeCol.getServerPort())) return false;

        // Load the player mods into the specification that is about to be
        // used to initialize the server.
        //
        // ATM we only allow mods in single player games.
        // FIXME: allow in stand alone server starts?
        List<FreeColModFile> mods = getClientOptions().getActiveMods();
        spec.loadMods(mods);
        Messages.loadActiveModMessageBundle(mods, FreeCol.getLocale());
        FreeColServer freeColServer = startServer(false, true, spec, -1);
        if (freeColServer == null) return false;

        fcc.setFreeColServer(freeColServer);
        fcc.setSinglePlayer(true);
        StringTemplate err = login(FreeCol.getName(), freeColServer.getHost(),
                                   freeColServer.getPort());
        if (err != null) {
            getGUI().showErrorMessage(err);
            return false;
        }

        final ClientOptions co = getClientOptions();
        if (co.getBoolean(ClientOptions.AUTOSAVE_DELETE)) {
            FreeColServer.removeAutosaves(co.getText(ClientOptions.AUTO_SAVE_PREFIX));
        }
        fcc.getPreGameController().setReady(true);
        if (skip) {
            fcc.getPreGameController().requestLaunch();
        } else {
            getGUI().showStartGamePanel(getGame(), getMyPlayer(), true);
        }
        return true;
    }

    /**
     * Loads and starts a game from the given file.
     *
     * @param file The saved game.
     * @param userMsg An optional message key to be displayed early.
     * @return True if the game starts successully.
     */
    public boolean startSavedGame(File file, final String userMsg) {
        final FreeColClient fcc = getFreeColClient();
        fcc.setMapEditor(false);

        class ErrorJob implements Runnable {
            private final StringTemplate template;
            private final Runnable runnable;
            
            ErrorJob(StringTemplate template) {
                this.template = template;
                this.runnable = null;
            }

            ErrorJob(StringTemplate template, Runnable runnable) {
                this.template = template;
                this.runnable = runnable;
            }
            
            @Override
            public void run() {
                getGUI().closeMenus();
                getGUI().showErrorMessage(template, null, runnable);
            }
        }

        final ClientOptions options = getClientOptions();
        final boolean defaultSinglePlayer;
        final boolean defaultPublicServer;
        FreeColSavegameFile fis = null;
        try {
            fis = new FreeColSavegameFile(file);
        } catch (IOException ioe) {
            SwingUtilities.invokeLater(new ErrorJob(FreeCol.badFile("error.couldNotLoad", file)));
            logger.log(Level.WARNING, "Could not open save file: "
                + file.getName());
            return false;
        }
        options.merge(fis);
        options.fixClientOptions();

        // Get suggestions for "singlePlayer" and "publicServer"
        // settings from the file, and update the client options if
        // possible.
        try (
            FreeColXMLReader xr = fis.getSavedGameFreeColXMLReader();
        ) {
            xr.nextTag();

            String str = xr.getAttribute(FreeColServer.OWNER_TAG,
                                         (String)null);
            if (str != null) FreeCol.setName(str);

            defaultSinglePlayer
                = xr.getAttribute(FreeColServer.SINGLE_PLAYER_TAG, false);
            defaultPublicServer
                = xr.getAttribute(FreeColServer.PUBLIC_SERVER_TAG, false);

        } catch (FileNotFoundException fnfe) {
            SwingUtilities.invokeLater(new ErrorJob(FreeCol.errorFromException(fnfe, FreeCol.badFile("error.couldNotFind", file))));
            logger.log(Level.WARNING, "Can not find file: " + file.getName(),
                fnfe);
            return false;
        } catch (XMLStreamException xse) {
            SwingUtilities.invokeLater(new ErrorJob(FreeCol.errorFromException(xse, FreeCol.badFile("error.couldNotLoad", file))));
            logger.log(Level.WARNING, "Error reading game from: "
                + file.getName(), xse);
            return false;
        } catch (Exception ex) {
                SwingUtilities.invokeLater(new ErrorJob(FreeCol.errorFromException(ex, FreeCol.badFile("error.couldNotLoad", file))));
            logger.log(Level.WARNING, "Could not load game from: "
                + file.getName(), ex);
            return false;
        }

        // Reload the client options saved with this game.
        final boolean singlePlayer;
        final String name;
        final int port;
        final int sgo = options.getInteger(ClientOptions.SHOW_SAVEGAME_SETTINGS);
        boolean show = sgo == ClientOptions.SHOW_SAVEGAME_SETTINGS_ALWAYS
            || (!defaultSinglePlayer
                && sgo == ClientOptions.SHOW_SAVEGAME_SETTINGS_MULTIPLAYER);
        if (show) {
            if (!getGUI().showLoadingSavegameDialog(defaultPublicServer,
                                                    defaultSinglePlayer))
                return false;
            LoadingSavegameInfo lsd = getGUI().getLoadingSavegameInfo();
            singlePlayer = lsd.isSinglePlayer();
            name = lsd.getServerName();
            port = lsd.getPort();
        } else {
            singlePlayer = defaultSinglePlayer;
            name = null;
            port = -1;
        }
        Messages.loadActiveModMessageBundle(options.getActiveMods(),
                                            FreeCol.getLocale());
        if (!unblockServer(port)) return false;
        getGUI().showStatusPanel(Messages.message("status.loadingGame"));

        final File theFile = file;
        Runnable loadGameJob = () -> {
            FreeColServer freeColServer = null;
            StringTemplate err = null;
            try {
                final FreeColSavegameFile saveGame
                    = new FreeColSavegameFile(theFile);
                freeColServer = new FreeColServer(saveGame,
                    (Specification)null, port, name);
                fcc.setFreeColServer(freeColServer);
                // Server might have bounced to another port.
                fcc.setSinglePlayer(singlePlayer);
                igc().setGameConnected();
                err = login(FreeCol.getName(), freeColServer.getHost(), 
                            freeColServer.getPort());
                if (err == null) {
                    SwingUtilities.invokeLater(() -> {
                            ResourceManager.setScenarioMapping(saveGame.getResourceMapping());
                            if (userMsg != null) {
                                getGUI().showInformationMessage(userMsg);
                            }
                            getGUI().closeStatusPanel();
                        });
                    return; // Success!
                }
                logger.warning("Could not log in: " + Messages.message(err));
            } catch (FileNotFoundException fnfe) {
                err = FreeCol.errorFromException(fnfe,
                    FreeCol.badFile("error.couldNotFind", theFile));
                logger.log(Level.WARNING, "Can not find file.", fnfe);
            } catch (IOException ioe) {
                err = FreeCol.errorFromException(ioe, "server.initialize");
                logger.log(Level.WARNING, "Error starting game.", ioe);
            } catch (XMLStreamException xse) {
                err = FreeCol.errorFromException(xse,
                    FreeCol.badFile("error.couldNotLoad", theFile));
                logger.log(Level.WARNING, "Stream error.", xse);
            } catch (Exception ex) {
                err = FreeCol.errorFromException(ex, "server.initialize");
                logger.log(Level.WARNING, "FreeCol error.", ex);
            }
            if (err != null) {
                // If this is a debug run, fail hard.
                if (fcc.isHeadless() || FreeColDebugger.getDebugRunTurns() >= 0) {
                    FreeCol.fatal(Messages.message(err));
                }
                // login may have received an error message from the server,
                // which is already being displayed.  Do not override it.
                if (!getGUI().onClosingErrorPanel(fcc.invokeMainPanel(null))) {
                    SwingUtilities.invokeLater(new ErrorJob(err,
                            fcc.invokeMainPanel(null)));
                }
            }
        };
        fcc.setWork(loadGameJob);
        return true;
    }

    /**
     * Reconnects to the server.
     *
     * @return True if the reconnection succeeds.
     */
    public boolean reconnect() {
        final String host = askServer().getHost();
        final int port = askServer().getPort();

        getGUI().removeInGameComponents();
        logout(true);
        StringTemplate err = login(FreeCol.getName(), host, port);
        if (err != null) {
            getGUI().showErrorMessage(err);
            return false;
        }
        igc().nextModelMessage();
        return true;
    }

    /**
     * Sends a logout message to the server.
     *
     * @param notifyServer Whether or not the server should be
     *     notified of the logout.  For example: if the server kicked us
     *     out then we don't need to confirm with a logout message.
     */
    public void logout(boolean notifyServer) {
        if (notifyServer) askServer().logout();
        try {
            askServer().disconnect();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Disconnection error", ioe);
        }
        finish();
    }

    /**
     * Quits the current game, optionally notifying and stopping the server.
     *
     * @param stopServer Whether to stop the server.
     * @param notifyServer Whether or not the server should be
     *     notified of the logout.  For example: if the server kicked us
     *     out then we don't need to confirm with a logout message.
     */
    public void quitGame(boolean stopServer, boolean notifyServer) {
        final FreeColServer server = getFreeColServer();
        if (stopServer && server != null) {
            server.getController().shutdown();
            getFreeColClient().setFreeColServer(null);
            finish();
        } else {
            if (getFreeColClient().isLoggedIn()) logout(notifyServer);
        }
    }

    /**
     * Quits the current game. If a server is running it will be
     * stopped if stopServer is <i>true</i>.  The server and perhaps
     * the clients (if a server is running through this client and
     * stopServer is true) will be notified.
     *
     * @param stopServer Indicates whether or not a server that was
     *     started through this client should be stopped.
     */
    public void quitGame(boolean stopServer) {
        quitGame(stopServer, true);
    }

    /**
     * Gets a list of servers from the meta server.
     *
     * @return A list of {@link ServerInfo} objects, or null on error.
     */
    public List<ServerInfo> getServerList() {
        DOMMessage msg = ask(FreeCol.META_SERVER_ADDRESS,
                             FreeCol.META_SERVER_PORT, new ServerListMessage(),
                             ServerListMessage.TAG,
                             StringTemplate.template("metaServer.communicationError"));
        return (msg instanceof ServerListMessage)
            ? ((ServerListMessage)msg).getServers()
            : null;
    }
}
