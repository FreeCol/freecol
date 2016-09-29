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

package net.sf.freecol.server.model;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.NameCache;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Event;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.HighSeas;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Limit;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.SimpleCombatModel;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitChangeType;
import net.sf.freecol.common.networking.DOMMessage;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.ChangePriority;
import net.sf.freecol.server.control.ChangeSet.See;


/**
 * The server representation of the game.
 */
public class ServerGame extends Game implements ServerModelObject {

    private static final Logger logger = Logger.getLogger(ServerGame.class.getName());

    /** Timestamp of last move, if any.  Do not serialize. */
    private long lastTime = -1L;

    /** An executor for askTimeout. */
    private final ExecutorService executor = Executors.newCachedThreadPool();


    /**
     * Creates a new game model.
     *
     * @param specification The {@code Specification} to use in this game.
     * @see net.sf.freecol.server.FreeColServer
     */
    public ServerGame(Specification specification) {
        super(specification);

        this.combatModel = new SimpleCombatModel();
        currentPlayer = null;
    }

    /**
     * Initiate a new {@code ServerGame} with information from a
     * saved game.
     *
     * @param xr The input stream containing the XML.
     * @param specification The {@code Specification} to use in this game.
     * @exception XMLStreamException if an error occurred during parsing.
     * @see net.sf.freecol.server.FreeColServer#loadGame
     */
    public ServerGame(FreeColXMLReader xr, Specification specification)
        throws XMLStreamException {
        this(specification);

        this.setGame(this);
        readFromXML(xr);
    }


    /**
     * Get a list of connected server players, optionally excluding
     * supplied ones.
     *
     * @param serverPlayers The {@code ServerPlayer}s to exclude.
     * @return A list of all connected server players, with exclusions.
     */
    public List<ServerPlayer> getConnectedPlayers(ServerPlayer... serverPlayers) {
        return transform(getLivePlayers(),
                         p -> ((ServerPlayer)p).isConnected()
                             && none(serverPlayers, matchKey((ServerPlayer)p)),
                         p -> (ServerPlayer)p);
    }

    /**
     * Send a change set to all live players, and optional extras.
     *
     * @param cs The {@code ChangeSet} to send.
     * @param serverPlayers Optional extra {@code ServerPlayer}s
     *     to include (useful when a player dies).
     */
    public void sendToAll(ChangeSet cs, ServerPlayer... serverPlayers) {
        sendToList(getConnectedPlayers(), cs);
    }
    
    /**
     * Send a change set to all players, optionally excluding one.
     *
     * @param serverPlayer A {@code ServerPlayer} to exclude.
     * @param cs The {@code ChangeSet} encapsulating the update.
     */
    public void sendToOthers(ServerPlayer serverPlayer, ChangeSet cs) {
        sendToList(getConnectedPlayers(serverPlayer), cs);
    }

    /**
     * Send a change set to a list of players.
     *
     * @param serverPlayers The list of {@code ServerPlayer}s to send to.
     * @param cs The {@code ChangeSet} to send.
     */
    public void sendToList(List<ServerPlayer> serverPlayers, ChangeSet cs) {
        for (ServerPlayer s : serverPlayers) sendTo(s, cs);
    }

    /**
     * Send a change set to one player.
     *
     * @param serverPlayer The {@code ServerPlayer} to send to.
     * @param cs The {@code ChangeSet} to send.
     */
    public void sendTo(ServerPlayer serverPlayer, ChangeSet cs) {
        serverPlayer.send(cs);
    }

    /**
     * Asks a question of a player with a timeout.
     *
     * @param serverPlayer The {@code ServerPlayer} to ask.
     * @param timeout The timeout, in seconds.
     * @param request The {@code DOMMessage} question.
     * @return The response to the question, or null if none.
     */
    public DOMMessage askTimeout(final ServerPlayer serverPlayer, int timeout,
                                 final DOMMessage request) {
        Callable<DOMMessage> callable = () -> serverPlayer.ask(this, request);
        Future<DOMMessage> future = executor.submit(callable);
        DOMMessage reply;
        try {
            reply = future.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            reply = null;
            sendTo(serverPlayer, new ChangeSet()
                .addTrivial(See.only(serverPlayer), "closeMenus",
                    ChangePriority.CHANGE_NORMAL));
        } catch (InterruptedException | ExecutionException e) {
            reply = null;
            logger.log(Level.WARNING, "Exception completing future", e);
        }
        return reply;
    }
    
    /**
     * Makes a trivial server object in this game given a server object tag
     * and an identifier.
     *
     * @param type The server object tag.
     * @param id The object identifier.
     * @return A trivial server object.
     * @exception ClassNotFoundException if there is no such type.
     * @exception IllegalAccessException if the target exists but is hidden.
     * @exception InstantiationException if the instantiation fails.
     * @exception InvocationTargetException if the target in not available.
     * @exception NoSuchMethodException if the tag does not refer to a
     *      server type.
     */
    private Object makeServerObject(String type, String id)
        throws ClassNotFoundException, IllegalAccessException,
               InstantiationException, InvocationTargetException,
               NoSuchMethodException {
        type = "net.sf.freecol.server.model."
            + type.substring(0,1).toUpperCase() + type.substring(1);
        Class<?> c = Class.forName(type);
        return c.getConstructor(Game.class, String.class)
            .newInstance(this, id);
    }

    /**
     * Collects a list of all the ServerModelObjects in this game.
     *
     * @return A list of all the ServerModelObjects in this game.
     */
    public List<ServerModelObject> getServerModelObjects() {
        List<ServerModelObject> objs = new ArrayList<>();
        for (FreeColGameObject fcgo : getFreeColGameObjects()) {
            if (fcgo instanceof ServerModelObject) {
                objs.add((ServerModelObject)fcgo);
            }
        }
        return objs;
    }

    /**
     * Update the players.
     *
     * @param players A list of new {@code ServerPlayer}s.
     */
    public void updatePlayers(List<ServerPlayer> players) {
        ChangeSet cs = new ChangeSet();
        for (ServerPlayer sp : players) cs.addPlayer(sp);
        sendToAll(cs);
    }

    /**
     * Get a unique identifier to identify a {@code FreeColGameObject}.
     * 
     * @return A unique identifier.
     */
    @Override
    public String getNextId() {
        String id = Integer.toString(nextId);
        nextId++;
        return id;
    }

    /**
     * Randomize a new game.
     *
     * @param random A pseudo-random number source.
     */
    public void randomize(Random random) {
        if (random != null) NameCache.requireCitiesOfCibola(random);
    }

    /**
     * Checks if anybody has won this game.
     *
     * @return The {@code Player} who has won the game or null if none.
     */
    public Player checkForWinner() {
        final Specification spec = getSpecification();
        if (spec.getBoolean(GameOptions.VICTORY_DEFEAT_REF)) {
            Player winner = find(getLiveEuropeanPlayers(),
                p -> p.getPlayerType() == Player.PlayerType.INDEPENDENT);
            if (winner != null) return winner;
        }
        if (spec.getBoolean(GameOptions.VICTORY_DEFEAT_EUROPEANS)) {
            List<Player> winners = transform(getLiveEuropeanPlayers(),
                                             p -> !p.isREF());
            if (winners.size() == 1) return winners.get(0);
        }
        if (spec.getBoolean(GameOptions.VICTORY_DEFEAT_HUMANS)) {
            List<Player> winners = transform(getLiveEuropeanPlayers(),
                                             p -> !p.isAI());
            if (winners.size() == 1) return winners.get(0);
        }
        return null;
    }


    /**
     * Is the next player in a new turn?
     *
     * @return True if the next turn is due.
     */
    public boolean isNextPlayerInNewTurn() {
        Player nextPlayer = getNextPlayer();
        return players.indexOf(currentPlayer) > players.indexOf(nextPlayer)
            || currentPlayer == nextPlayer;
    }


    /**
     * Change to the next turn for this game.
     *
     * @param cs A {@code ChangeSet} to update.
     */
    public void csNextTurn(ChangeSet cs) {
        String duration = null;
        long now = new Date().getTime();
        if (lastTime >= 0) {
            duration = ", previous turn duration = " + (now - lastTime) + "ms";
        }
        lastTime = now;

        Session.completeAll(cs);
        setTurn(getTurn().next());
        logger.finest("Turn is now " + getTurn() + duration);
        cs.addTrivial(See.all(), "newTurn", ChangePriority.CHANGE_NORMAL,
                      "turn", Integer.toString(getTurn().getNumber()));
    }

    /**
     * Checks for and if necessary performs the War of Spanish
     * Succession changes.
     *
     * Visibility changes for the winner, loser is killed/irrelevant.
     *
     * @param cs A {@code ChangeSet} to update.
     * @param lb A {@code LogBuilder} to log to.
     * @param event The Spanish Succession {@code Event}.
     * @return The {@code ServerPlayer} that is eliminated if
     *     any, or null if none found.
     */
    private ServerPlayer csSpanishSuccession(ChangeSet cs, LogBuilder lb,
                                             Event event) {
        final Limit yearLimit
            = event.getLimit("model.limit.spanishSuccession.year");
        if (!yearLimit.evaluate(this)) return null;

        final Limit weakLimit
            = event.getLimit("model.limit.spanishSuccession.weakestPlayer");
        final Limit strongLimit
            = event.getLimit("model.limit.spanishSuccession.strongestPlayer");
        Player weakAI = null, strongAI = null;
        int weakScore = Integer.MAX_VALUE, strongScore = Integer.MIN_VALUE;
        boolean ready = false;
        lb.add("Spanish succession scores[");
        final String sep = ", ";
        for (Player player : transform(getLiveEuropeanPlayers(),
                                       p -> !p.isREF())) {
            // Has anyone met the triggering limit?
            boolean ok = strongLimit.evaluate(player);
            ready |= ok;
            lb.add(player.getName(), "(", ok, ")");

            // Human players can trigger the event, but we only
            // transfer assets between AI players.
            if (!player.isAI()) continue;
            
            final int score = player.getSpanishSuccessionScore();
            lb.add("=", score, sep);
            if (strongAI == null || strongScore < score) {
                strongScore = score;
                strongAI = player;
            }
            if (weakLimit.evaluate(player)
                && (weakAI == null || weakScore > score)) {
                weakScore = score;
                weakAI = player;
            }
        }
        lb.truncate(lb.size() - sep.length());
        lb.add("]");
        // Do not proceed if no player meets the support limit or if there
        // are not clearly identifiable strong and weak AIs.
        if (!ready
            || weakAI == null || strongAI == null
            || weakAI == strongAI) return null;

        lb.add(" => ", weakAI.getName(), " cedes ", strongAI.getName(), ":");
        List<Tile> tiles = new ArrayList<>();
        Set<Tile> updated = new HashSet<>();
        ServerPlayer strongest = (ServerPlayer)strongAI;
        ServerPlayer weakest = (ServerPlayer)weakAI;
        forEach(flatten(getLiveNativePlayers(),
                        p -> p.getIndianSettlementsWithMissionary(weakest)),
            is -> {
                lb.add(" ", is.getName(), "(mission)");
                is.getTile().cacheUnseen(strongest);//+til
                tiles.add(is.getTile());
                is.setContacted(strongest);//-til
                ServerUnit missionary = (ServerUnit)is.getMissionary();
                if (weakest.csChangeOwner(missionary, strongest,//-vis(both),-til
                                          UnitChangeType.CAPTURE, null, cs)) {
                    is.getTile().updateIndianSettlement(strongest);
                    cs.add(See.perhaps().always(strongest), is);
                }
            });
        for (Colony c : weakest.getColonyList()) {
            updated.addAll(c.getOwnedTiles());
            ((ServerColony)c).csChangeOwner(strongest, false, cs);//-vis(both),-til
            lb.add(" ", c.getName());
        }
        for (Unit unit : weakest.getUnitList()) {
            lb.add(" ", unit.getId());
            if (unit.isOnCarrier()) {
                ; // Allow carrier to handle
            } else if (!weakest.csChangeOwner(unit, strongest, //-vis(both)
                    UnitChangeType.CAPTURE, null, cs)) {
                logger.warning("Owner change failed for " + unit);
            } else {
                unit.setMovesLeft(0);
                unit.setState(Unit.UnitState.ACTIVE);
                if (unit.getLocation() instanceof Europe) {
                    unit.setLocation(strongAI.getEurope());//-vis
                    cs.add(See.only(strongest), unit);
                } else if (unit.getLocation() instanceof HighSeas) {
                    unit.setLocation(strongAI.getHighSeas());//-vis
                    cs.add(See.only(strongest), unit);
                } else if (unit.getLocation() instanceof Tile) {
                    Tile tile = unit.getTile();
                    if (!tiles.contains(tile)) tiles.add(tile);
                }
            }
        }

        StringTemplate loser = weakAI.getNationLabel();
        StringTemplate winner = strongAI.getNationLabel();
        cs.addMessage(See.all(),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                             "model.game.spanishSuccession", strongAI)
                .addStringTemplate("%loserNation%", loser)
                .addStringTemplate("%nation%", winner));
        cs.addGlobalHistory(this,
            new HistoryEvent(getTurn(),
                HistoryEvent.HistoryEventType.SPANISH_SUCCESSION, null)
                   .addStringTemplate("%loserNation%", loser)
                   .addStringTemplate("%nation%", winner));
        setSpanishSuccession(true);
        cs.addPartial(See.all(), this, "spanishSuccession");
        tiles.removeAll(updated);
        cs.add(See.perhaps(), tiles);
        
        weakest.csKill(cs);//+vis(weakest)
        strongest.invalidateCanSeeTiles();//+vis(strongest)

        // Trace fail where not all units are transferred
        for (FreeColGameObject fcgo : getFreeColGameObjects()) {
            if (fcgo instanceof Ownable
                && ((Ownable)fcgo).getOwner() == weakest) {
                throw new RuntimeException("Lurking " + weakest.getId()
                    + " fcgo: " + fcgo);
            }
        }

        return weakest;
    }


    // Implement ServerModelObject

    /**
     * Build the updates for a new turn for all the players in this game.
     *
     * @param random A {@code Random} number source.
     * @param lb A {@code LogBuilder} to log to.
     * @param cs A {@code ChangeSet} to update.
     */
    @Override
    public void csNewTurn(Random random, LogBuilder lb, ChangeSet cs) {
        lb.add("GAME ", getId(), ", ");
        for (Player player : getLivePlayerList()) {
            ((ServerPlayer)player).csNewTurn(random, lb, cs);
        }

        final Specification spec = getSpecification();
        Event succession = spec.getEvent("model.event.spanishSuccession");
        if (succession != null && !getSpanishSuccession()) {
            ServerPlayer loser = csSpanishSuccession(cs, lb, succession);
            // TODO: send update to loser.  It will not see anything
            // because it is no longer a live player.
            // if (loser != null) sendElement(loser, cs);
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "serverGame".
     */
    @Override
    public String getServerXMLElementTagName() {
        return "serverGame";
    }


    // Interface Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        // ServerGame does not add any significant fields, so Game.equals
        // suffices.
        return super.equals(o);
    }
}
