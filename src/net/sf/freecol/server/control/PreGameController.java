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

package net.sf.freecol.server.control;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ReadyMessage;
import net.sf.freecol.common.networking.SetAvailableMessage;
import net.sf.freecol.common.networking.SetColorMessage;
import net.sf.freecol.common.networking.SetNationMessage;
import net.sf.freecol.common.networking.SetNationTypeMessage;
import net.sf.freecol.common.networking.UpdateGameOptionsMessage;
import net.sf.freecol.common.networking.UpdateMapGeneratorOptionsMessage;
import net.sf.freecol.common.networking.VacantPlayersMessage;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The control object that is responsible for setting parameters and
 * starting a new game.
 *
 * The game enters the state
 * {@link net.sf.freecol.server.FreeColServer.ServerState#IN_GAME}, when the
 *  has successfully been invoked.
 */
public final class PreGameController extends Controller {

    private static final Logger logger = Logger.getLogger(PreGameController.class.getName());


    /** Is the game launching yet. */
    private boolean launching = false;


    /**
     * The constructor to use.
     *
     * @param freeColServer The main {@code FreeColServer} object.
     */
    public PreGameController(FreeColServer freeColServer) {
        super(freeColServer);
    }


    /**
     * Set the launching state.
     *
     * @param launching The new launching state.
     * @return The former launching state.
     */
    private synchronized boolean setLaunching(boolean launching) {
        boolean old = this.launching;
        this.launching = launching;
        return old;
    }

    /**
     * A player changes its readiness.
     *
     * @param serverPlayer The {@code ServerPlayer} that changes its state.
     * @param ready The new readiness.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet ready(ServerPlayer serverPlayer, boolean ready) {
        serverPlayer.setReady(ready);
        getFreeColServer().sendToAll(new ReadyMessage(serverPlayer, ready),
                                     serverPlayer);
        return null;
    }
        
    /**
     * Launch the game if possible.
     * 
     * @param serverPlayer The {@code ServerPlayer} that requested launching.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet requestLaunch(ServerPlayer serverPlayer) {
        logger.info("Launching for " + serverPlayer);
        if (setLaunching(true)) {
            logger.warning("Already launching");
            return null;
        }

        final FreeColServer freeColServer = getFreeColServer();
        final Game game = getGame();
        final Specification spec = game.getSpecification();

        // Check if launching player is an admin.
        if (!serverPlayer.isAdmin()) {
            setLaunching(false);
            return serverPlayer.clientError(StringTemplate
                .template("server.onlyAdminCanLaunch"));
        }
        serverPlayer.setReady(true);

        // Check that no two players have the same nation
        List<Player> players = game.getLivePlayerList();
        List<Nation> nations = new ArrayList<>(players.size());
        for (Player p : players) {
            final Nation nation = spec.getNation(p.getNationId());
            if (nations.contains(nation)) {
                setLaunching(false);
                return serverPlayer.clientError(StringTemplate
                    .template("server.invalidPlayerNations"));
            }
            nations.add(nation);
        }

        // Check if all players are ready.
        if (!game.allPlayersReadyToLaunch()) {
            setLaunching(false);
            return serverPlayer.clientError(StringTemplate
                .template("server.notAllReady"));
        }
        try {
            freeColServer.startGame();
        } catch (FreeColException fce) {
            return serverPlayer.clientError(fce.getMessage());
        }

        return null;
    }

    /**
     * Handle a player changing its availability.
     *
     * @param serverPlayer The {@code ServerPlayer} that changed.
     * @param nation The changed {@code Nation}.
     * @param state The new {@code NationState}.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setAvailable(ServerPlayer serverPlayer, Nation nation,
                                  NationState state) {
        getGame().getNationOptions().setNationState(nation, state);
        getFreeColServer().sendToAll(new SetAvailableMessage(nation, state),
                                     serverPlayer);
        return null;
    }

    /**
     * Handle a player changing its color.
     *
     * @param serverPlayer The {@code ServerPlayer} that changed.
     * @param nation The changed {@code Nation}.
     * @param color The new {@code Color}.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setColor(ServerPlayer serverPlayer, Nation nation,
                              Color color) {
        nation.setColor(color);
        getFreeColServer().sendToAll(new SetColorMessage(nation, color),
                                     serverPlayer);
        return null;
    }

    /**
     * Handle a player changing its nation.
     *
     * @param serverPlayer The {@code ServerPlayer} that changed.
     * @param nation The changed {@code Nation}.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setNation(ServerPlayer serverPlayer, Nation nation) {
        serverPlayer.setNation(nation);
        getFreeColServer().sendToAll(new SetNationMessage(serverPlayer, nation),
                                     serverPlayer);
        return null;
    }

    /**
     * Handle a player changing its nation type.
     *
     * @param serverPlayer The {@code ServerPlayer} that changed.
     * @param nationType The changed {@code NationType}.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setNationType(ServerPlayer serverPlayer,
                                   NationType nationType) {
        serverPlayer.changeNationType(nationType);
        getFreeColServer().sendToAll(new SetNationTypeMessage(serverPlayer, nationType),
                                     serverPlayer);
        return null;
    }

    /**
     * Handle a player changing its game options.
     *
     * @param serverPlayer The {@code ServerPlayer} that changed.
     * @param options The new {@code OptionGroup} containing the game options.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet updateGameOptions(ServerPlayer serverPlayer,
                                       OptionGroup options) {
        final FreeColServer fcs = getFreeColServer();
        final Game game = fcs.getGame();
        final Specification spec = game.getSpecification();

        if (!spec.mergeGameOptions(options, "server")) {
            return serverPlayer.clientError("Game option merge failed");
        }
        fcs.sendToAll(new UpdateGameOptionsMessage(spec.getGameOptions()),
                                                   serverPlayer);
        return null;
    }

    /**
     * Handle a player changing its map generator options.
     *
     * @param serverPlayer The {@code ServerPlayer} that changed.
     * @param options The new {@code OptionGroup} containing the map
     *     generator options.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet updateMapGeneratorOptions(ServerPlayer serverPlayer,
                                               OptionGroup options) {
        final FreeColServer fcs = getFreeColServer();
        final Game game = fcs.getGame();
        final Specification spec = game.getSpecification();
        
        if (!spec.mergeMapGeneratorOptions(options, "server")) {
            return serverPlayer.clientError("Map option merge failed");
        }
        fcs.sendToAll(new UpdateMapGeneratorOptionsMessage(spec.getMapGeneratorOptions()),
                                                           serverPlayer);
        return null;
    }

    /**
     * Handle a request for vacant players.
     *
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet vacantPlayers() {
        return ChangeSet.simpleChange((Player)null,
            new VacantPlayersMessage()
                .setVacantPlayers(getFreeColServer().getGame()));
    }
}
