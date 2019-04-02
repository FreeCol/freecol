/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol; 
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.LoadingSavegameInfo;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Game.LogoutReason;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.FreeColServer.ServerState;


/**
 * The controller responsible for starting a server and connecting to it.
 */
public final class ConnectController extends FreeColClientHolder {

    private static final Logger logger = Logger.getLogger(ConnectController.class.getName());

    /** Fixed argument list for startSavedGame. */
    private static final List<String> savedKeys
        = makeUnmodifiableList(FreeColServer.OWNER_TAG,
                               FreeColServer.SINGLE_PLAYER_TAG,
                               FreeColServer.PUBLIC_SERVER_TAG);


    /**
     * Creates a new {@code ConnectController}.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ConnectController(FreeColClient freeColClient) {
        super(freeColClient);
    }


    /**
     * Establish the user connection.
     *
     * @param user The player name.
     * @param host The host name to connect to.
     * @param port The host port to connect to.
     * @return Null on success, an {@code StringTemplate} error
     *     message on failure.
     */
    private StringTemplate connect(String user, String host, int port) {
        if (askServer().isConnected()) return null;

        StringTemplate err = null;
        try {
            if (askServer().connect(FreeCol.CLIENT_THREAD + user,
                                    host, port) != null) {
                getFreeColClient().changeClientState(false);
                logger.info("Connected to " + host + ":" + port
                    + " as " + user);
            } else {
                logger.warning("Failed to connect to "
                    + host + ":" + port + " as " + user);
                err = StringTemplate.template("server.couldNotConnect");
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception when connecting to "
                + host + ":" + port + " as " + user, ex);
            err = FreeCol.errorFromException(ex, "server.couldNotConnect");
        }
        return err;
    }

    /**
     * Request that this client log out from the server.
     *
     * @param reason The reason to logout from the server.
     * @return True if the player is already logged out,
     *     or a logout request was sent.
     */
    public boolean requestLogout(LogoutReason reason) {
        final FreeColClient fcc = getFreeColClient();
        final Player player = fcc.getMyPlayer();
        if (!fcc.isLoggedIn()) return true;

        // Save our active unit on reconnect.
        if (reason == LogoutReason.RECONNECT) {
            Unit active = getGUI().getActiveUnit();
            if (active != null && player.owns(active)) {
                FreeColServer fcs = fcc.getFreeColServer();
                if (fcs != null) {
                    fcs.getGame().setInitialActiveUnitId(active.getId());
                }
            }
        }
            
        logger.info("Logout begin for client " + player.getName()
            + ": " + reason);
        return askServer().logout(player, reason);
    }

    /**
     * Complete log out of this client from the server.
     *
     * @param reason The reason to logout from the server.
     * @return True if the logout completes.
     */
    public boolean logout(LogoutReason reason) {
        final FreeColClient fcc = getFreeColClient();
        final Player player = fcc.getMyPlayer();
        logger.info("Logout end for client " + player.getName()
            + ": " + reason);

        askServer().disconnect();

        switch (reason) {
        case DEFEATED: case QUIT:
            fcc.logout(false);
            fcc.quit();
            break;
        case LOGIN: // FIXME: This should not happen, drop when convinced
            FreeCol.trace(logger, "logout(LOGIN) detected");
            fcc.logout(false);
            break;
        case MAIN_TITLE: // All the way back to the MainPanel
            fcc.logout(false);
            mainTitle();
            break;
        case NEW_GAME: // Back to the NewPanel
            fcc.logout(false);
            newGame();
            break;
        case RECONNECT:
        default: // default "can not happen", but if so, reconnect is safest
            fcc.logout(false);
            final String name = player.getName();
            try {
                if (askServer().reconnect() != null
                    && askServer().login(name, FreeCol.getVersion(),
                                         fcc.getSinglePlayer(),
                                         fcc.currentPlayerIsMyPlayer())) {
                    logger.info("Reconnected for client " + name);
                } else {
                    logger.severe("Reconnect failed for client " + name);
                    fcc.askToQuit();
                }
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Reconnect exception for client "
                    + name, ioe);
                fcc.quit();
            }
            break;
        }
        return true;
    }

    /**
     * Request this client log in to <i>host:port</i>.
     *
     * Public for the test suite.
     *
     * @param user The name of the player to use.
     * @param host The name of the machine running the {@code FreeColServer}.
     * @param port The port to use when connecting to the host.
     * @return True if the player was already logged in, or if the login
     *     message was sent.
     */
    public boolean requestLogin(String user, String host, int port) {
        final FreeColClient fcc = getFreeColClient();
        if (fcc.isLoggedIn()) return true;
        fcc.setMapEditor(false);

        // Clean up any old connections
        askServer().disconnect();

        // Establish the full connection here
        StringTemplate err = connect(user, host, port);
        if (err == null) {
            // Ask the server to log in a player with the given user
            // name.  Control effectively transfers through the server
            // back to PGIH.login() and then to login() below.
            logger.info("Login request for client " + FreeCol.getName());
            if (askServer().login(user, FreeCol.getVersion(),
                                  fcc.getSinglePlayer(),
                                  fcc.currentPlayerIsMyPlayer())) {
                return true;
            }
            err = StringTemplate.template("server.couldNotLogin");
        }
        getGUI().showErrorMessage(err);
        return false;
    }

    /**
     * Complete a login.
     *
     * If we are joining a running game, there is little more
     * needed to be done.  If we are restoring from saved, the
     * game should include a map and a full complement of players,
     * including ours.
     *
     * Otherwise we may still need to select a nation, and optionally
     * change game or map options (using several possible messages).
     * {@link net.sf.freecol.client.gui.panel.StartGamePanel} does
     * this.
     *
     * When all the parameters are in place and all players are
     * ready (trivially true in single player, needs checking in
     * multiplayer) the client needs to send a requestLaunch
     * message to ask the server to start the game.  That is
     * either done here or by the start game panel.
     *
     * requestLaunch effectively transfers control to
     * {@link FreeColServer#startGame}.
     *
     * @param state The state of the server.
     * @param game The new {@code Game} to attach to.
     * @param user The name of the player in the game.
     * @param single True if this is a single player game.
     * @param current True if the player is the current player.
     */
    public void login(ServerState state, Game game, String user,
                      boolean single, boolean current) {
        final FreeColClient fcc = getFreeColClient();

        Player player = game.getPlayerByName(user);
        if (player == null) {
            StringTemplate err = StringTemplate.template("server.noSuchPlayer")
                .addName("%player%", user);
            getGUI().showErrorMessage(err);
            logger.warning(Messages.message(err));
            return;
        }

        // Reattach to the game
        fcc.login(state == ServerState.IN_GAME, game, player, single);
        if (current) game.setCurrentPlayer(player);
        logger.info("Login accepted for client " + player.getName()
            + " to " + ((fcc.isInGame()) ? "running"
                : (game.allPlayersReadyToLaunch()) ? "ready" : "new")
            + " " + ((single) ? "single" : "multi")
            + "-player game as " + user + "/" + player.getId());

        if (fcc.isInGame()) { // Joining existing game or possibly reconnect
            fcc.restoreGUI(player);
            igc().nextModelMessage();
        } else if (game.getMap() != null
            && game.allPlayersReadyToLaunch()) { // Ready to launch!
            pgc().requestLaunch();
        } else { // More parameters need to be set or players to become ready
            getGUI().showStartGamePanel(game, player, single);
        }
    }

    //
    // There are several ways to start a game.
    // - single player
    // - restore from saved game
    // - multi-player (can also be joined while in play)
    // - shortcut debug/fast-start
    //
    // They all ultimately have to establish a connection to the server,
    // and get the game from there, which is done in {@link #login()}.
    // Control then effectively transfers to the handler for the login
    // message.
    //

    /**
     * Starts a new single player game by connecting to the server.
     *
     * FIXME: connect client/server directly (not using network-classes)
     *
     * @param spec The {@code Specification} for the game.
     * @return True if the game starts successfully.
     */
    public boolean startSinglePlayerGame(Specification spec) {
        final FreeColClient fcc = getFreeColClient();
        fcc.setMapEditor(false);

        if (!fcc.unblockServer(FreeCol.getServerPort())) return false;

        if (fcc.isLoggedIn()) { // Should not happen, warn and suppress
            logger.warning("startSinglePlayer while logged in!");
            requestLogout(LogoutReason.LOGIN);
        }

        // Load the player mods into the specification that is about to be
        // used to initialize the server.
        //
        // ATM we only allow mods in single player games.
        // FIXME: allow in stand alone server starts?
        List<FreeColModFile> mods = getClientOptions().getActiveMods();
        spec.loadMods(mods);
        Messages.loadActiveModMessageBundle(mods, FreeCol.getLocale());

        FreeColServer fcs = fcc.startServer(false, true, spec, -1);
        return (fcs == null) ? false
            : requestLogin(FreeCol.getName(),
                           fcs.getHost(), fcs.getPort());
    }

    /**
     * Loads and starts a game from the given file.
     *
     * @param file The saved game.
     * @return True if the game starts successully.
     */
    public boolean startSavedGame(File file) {
        final FreeColClient fcc = getFreeColClient();
        final GUI gui = getGUI();
        fcc.setMapEditor(false);

        // Get suggestions for player name, single/multiplayer and
        // public server state from the file, and update the client
        // options if possible.
        final ClientOptions options = getClientOptions();
        final boolean defaultSinglePlayer;
        final boolean defaultPublicServer;
        FreeColSavegameFile fis = null;
        try {
            fis = new FreeColSavegameFile(file);
        } catch (FileNotFoundException fnfe) {
            gui.errorJob(fnfe, FreeCol.badFile("error.couldNotFind", file))
                .invokeLater();
            logger.log(Level.WARNING, "Can not find file: " + file.getName(),
                       fnfe);
            return false;
        } catch (IOException ioe) {
            gui.errorJob(FreeCol.badFile("error.couldNotLoad", file))
                .invokeLater();
            logger.log(Level.WARNING, "Could not load file: " + file.getName(),
                       ioe);
            return false;
        }
        options.merge(fis);
        options.fixClientOptions();
        List<String> values = null;
        try {
            values = fis.peekAttributes(savedKeys);
        } catch (Exception ex) {
            gui.errorJob(ex, FreeCol.badFile("error.couldNotLoad", file))
                .invokeLater();
            logger.log(Level.WARNING, "Could not read from: " + file.getName(),
                       ex);
            return false;
        }
        if (values != null && values.size() == savedKeys.size()) {
            String str = values.get(0);
            if (str != null) FreeCol.setName(str);
            defaultSinglePlayer = Boolean.parseBoolean(values.get(1));
            defaultPublicServer = Boolean.parseBoolean(values.get(2));
        } else {
            defaultSinglePlayer = true;
            defaultPublicServer = false;
        }

        // Reload the client options saved with this game.
        final boolean publicServer = defaultPublicServer;
        final boolean singlePlayer;
        final String serverName;
        final int port;
        final int sgo = options.getInteger(ClientOptions.SHOW_SAVEGAME_SETTINGS);
        boolean show = sgo == ClientOptions.SHOW_SAVEGAME_SETTINGS_ALWAYS
            || (!defaultSinglePlayer
                && sgo == ClientOptions.SHOW_SAVEGAME_SETTINGS_MULTIPLAYER);
        if (show) {
            LoadingSavegameInfo lsi = getGUI()
                .showLoadingSavegameDialog(defaultPublicServer,
                                           defaultSinglePlayer);
            if (lsi == null) return false;
            singlePlayer = lsi.isSinglePlayer();
            serverName = lsi.getServerName();
            port = lsi.getPort();
        } else {
            singlePlayer = defaultSinglePlayer;
            serverName = null;
            port = -1;
        }
        Messages.loadActiveModMessageBundle(options.getActiveMods(),
                                            FreeCol.getLocale());
        if (!fcc.unblockServer(port)) return false;

        if (fcc.isLoggedIn()) { // Should not happen, warn and suppress
            logger.warning("startSavedGame while logged in!");
            requestLogout(LogoutReason.LOGIN);
        }

        FreeColServer fcs = fcc.startServer(publicServer, singlePlayer, file,
                                            port, serverName);
        if (fcs == null) return false;
        fcc.setFreeColServer(fcs);
        fcc.setSinglePlayer(true);
        return requestLogin(FreeCol.getName(), fcs.getHost(), fcs.getPort());
    }

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

        if (!fcc.unblockServer(port)) return false;

        if (fcc.isLoggedIn()) { // Should not happen, warn and suppress
            logger.warning("startMultiPlayer while logged in!");
            requestLogout(LogoutReason.LOGIN);
        }

        FreeColServer fcs = fcc.startServer(publicServer, false,
                                            specification, port);
        if (fcs == null) return false;
        fcc.setFreeColServer(fcs);
        fcc.setSinglePlayer(false);
        return requestLogin(FreeCol.getName(), fcs.getHost(), fcs.getPort());
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

        if (fcc.isLoggedIn()) { // Should not happen, warn and suppress
            logger.warning("joinMultiPlayer while logged in!");
            requestLogout(LogoutReason.LOGIN);
        }

        // Connect and disconnect, allow GameState message to arrive
        String name = FreeCol.getName();
        StringTemplate err = connect(name, host, port);
        if (err != null) {
            getGUI().showErrorMessage(err);
            return false;
        }
        while (fcc.getServerState() == null) Utils.delay(1000, null);
        askServer().disconnect();
        
        switch (fcc.getServerState()) {
        case PRE_GAME: case LOAD_GAME:
            // Name is good
            break;

        case IN_GAME:
            /*
            // Disable this check if you need to debug a multiplayer client.
            if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
                getGUI().showErrorMessage(StringTemplate
                    .template("client.debugConnect"));
                return false;
            }
            */
            // Find the players, choose one.
            List<String> names = fcc.getVacantPlayerNames();
            if (names.isEmpty()) return false;
            if (names.contains(name)) break; // Already there, use it
            StringTemplate tmpl = StringTemplate.template("client.choicePlayer");
            String choice = getGUI().getChoice(tmpl, "cancel",
                    transform(names, alwaysTrue(), n ->
                        new ChoiceItem<>(Messages.message(StringTemplate
                                .template("countryName")
                                .add("%nation%", Messages.nameKey(n))), n)));
            if (choice == null) return false; // User cancelled
            name = Messages.getRulerName(choice);
            break;

        case END_GAME: default:
            getGUI().showErrorMessage(StringTemplate.template("client.ending"));
            return false;
        }
        return requestLogin(name, host, port);
    }

    /**
     * Reset to the MainPanel.
     *
     * Called from ShowMainAction.
     */
    public void mainTitle() {
        final FreeColClient fcc = getFreeColClient();
        
        if (fcc.isMapEditor()) fcc.setMapEditor(false);

        if (fcc.isLoggedIn()) {
            if (getGUI().confirmStopGame()) {
                requestLogout(LogoutReason.MAIN_TITLE);
            }
            return;
        }
            
        fcc.stopServer();
        getGUI().removeInGameComponents();
        getGUI().showMainTitle();
    }
    
    /**
     * Reset to the NewPanel (except in the map editor).
     */
    public void newGame() {
        final FreeColClient fcc = getFreeColClient();

        if (fcc.isMapEditor()) {
            fcc.getMapEditorController().newMap();
            return;
        }

        if (fcc.isLoggedIn()) {
            if (getGUI().confirmStopGame()) {
                requestLogout(LogoutReason.NEW_GAME);
            }
            return;
        }

        fcc.stopServer();
        getGUI().removeInGameComponents();
        getGUI().showNewPanel(null);
    }
}
