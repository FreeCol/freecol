/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.NameCache;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Event;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColGameObjectListener;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.HighSeas;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Limit;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.SimpleCombatModel;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
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


    /**
     * Creates a new game model.
     *
     * @param specification The <code>Specification</code> to use in this game.
     * @see net.sf.freecol.server.FreeColServer
     */
    public ServerGame(Specification specification) {
        super(specification);

        this.combatModel = new SimpleCombatModel();
        currentPlayer = null;
    }

    /**
     * Initiate a new <code>ServerGame</code> with information from a
     * saved game.
     *
     * @param freeColGameObjectListener A listener that should be monitoring
     *     this <code>Game</code>.
     * @param xr The input stream containing the XML.
     * @param specification The <code>Specification</code> to use in this game.
     * @exception XMLStreamException if an error occurred during parsing.
     * @see net.sf.freecol.server.FreeColServer#loadGame
     */
    public ServerGame(FreeColGameObjectListener freeColGameObjectListener,
                      FreeColXMLReader xr, Specification specification)
        throws XMLStreamException {
        this(specification);

        this.freeColGameObjectListener = freeColGameObjectListener;
        this.setGame(this);
        readFromXML(xr);
    }


    /**
     * Get a list of connected server players, optionally excluding
     * supplied ones.
     *
     * @param serverPlayers The <code>ServerPlayer</code>s to exclude.
     * @return A list of all connected server players, with exclusions.
     */
    public List<ServerPlayer> getConnectedPlayers(ServerPlayer... serverPlayers) {
        return getLivePlayers(null).stream()
            .map(p -> (ServerPlayer)p)
            .filter(sp -> sp.isConnected()
                && none(serverPlayers, s -> s == sp))
            .collect(Collectors.toList());
    }

    /**
     * Send a change set to all live players, and optional extras.
     *
     * @param cs The <code>ChangeSet</code> to send.
     * @param serverPlayers Optional extra <code>ServerPlayer</code>s
     *     to include (useful when a player dies).
     */
    public void sendToAll(ChangeSet cs, ServerPlayer... serverPlayers) {
        List<ServerPlayer> live = getConnectedPlayers();
        for (ServerPlayer sp : serverPlayers) {
            if (!live.contains(sp)) live.add(sp);
        }
        sendToList(live, cs);
    }
    
    /**
     * Send a change set to all players, optionally excluding one.
     *
     * @param serverPlayer A <code>ServerPlayer</code> to exclude.
     * @param cs The <code>ChangeSet</code> encapsulating the update.
     */
    public void sendToOthers(ServerPlayer serverPlayer, ChangeSet cs) {
        sendToList(getConnectedPlayers(serverPlayer), cs);
    }

    /**
     * Send a change set to a list of players.
     *
     * @param serverPlayers The list of <code>ServerPlayer</code>s to send to.
     */
    public void sendToList(List<ServerPlayer> serverPlayers, ChangeSet cs) {
        for (ServerPlayer s : serverPlayers) s.send(cs);
    }
    

    /**
     * Makes a trivial server object in this game given a server object tag
     * and an identifier.
     *
     * @param type The server object tag.
     * @param id The object identifier.
     * @return A trivial server object.
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
     * @param players A list of new <code>ServerPlayer</code>s.
     */
    public void updatePlayers(List<ServerPlayer> players) {
        ChangeSet cs = new ChangeSet();
        for (ServerPlayer sp : players) cs.addPlayer(sp);
        sendToAll(cs);
    }

    /**
     * Get a unique identifier to identify a <code>FreeColGameObject</code>.
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
     * @return The <code>Player</code> who has won the game or null if none.
     */
    public Player checkForWinner() {
        final Specification spec = getSpecification();
        if (spec.getBoolean(GameOptions.VICTORY_DEFEAT_REF)) {
            Player winner = find(getLiveEuropeanPlayers(null),
                p -> p.getPlayerType() == Player.PlayerType.INDEPENDENT);
            if (winner != null) return winner;
        }
        if (spec.getBoolean(GameOptions.VICTORY_DEFEAT_EUROPEANS)) {
            List<Player> winners = getLiveEuropeanPlayers(null).stream()
                .filter(p -> !p.isREF())
                .collect(Collectors.toList());
            if (winners.size() == 1) return winners.get(0);
        }
        if (spec.getBoolean(GameOptions.VICTORY_DEFEAT_HUMANS)) {
            List<Player> winners = getLiveEuropeanPlayers(null).stream()
                .filter(p -> !p.isAI())
                .collect(Collectors.toList());
            if (winners.size() == 1) return winners.get(0);
        }
        return null;
    }


    /**
     * Is the next player in a new turn?
     */
    public boolean isNextPlayerInNewTurn() {
        Player nextPlayer = getNextPlayer();
        return players.indexOf(currentPlayer) > players.indexOf(nextPlayer)
            || currentPlayer == nextPlayer;
    }


    /**
     * Change to the next turn for this game.
     *
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csNextTurn(ChangeSet cs) {
        String duration = null;
        long now = new Date().getTime();
        if (lastTime >= 0) {
            duration = ", previous turn duration = " + (now - lastTime) + "ms";
        }
        lastTime = now;

        TransactionSession.completeAll(cs);
        setTurn(getTurn().next());
        logger.finest("Turn is now " + getTurn() + duration);
        cs.addTrivial(See.all(), "newTurn", ChangePriority.CHANGE_NORMAL,
                      "turn", Integer.toString(getTurn().getNumber()));
    }

    /**
     * Build the updates for a new turn for all the players in this game.
     *
     * @param random A <code>Random</code> number source.
     * @param lb A <code>LogBuilder</code> to log to.
     * @param cs A <code>ChangeSet</code> to update.
     */
    @Override
    public void csNewTurn(Random random, LogBuilder lb, ChangeSet cs) {
        lb.add("GAME ", getId(), ", ");
        for (Player player : getLivePlayers(null)) {
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
     * Checks for and if necessary performs the War of Spanish
     * Succession changes.
     *
     * Visibility changes for the winner, loser is killed/irrelevant.
     *
     * @param cs A <code>ChangeSet</code> to update.
     * @param lb A <code>LogBuilder</code> to log to.
     * @param event The Spanish Succession <code>Event</code>.
     * @return The <code>ServerPlayer</code> that is eliminated if
     *     any, or null if none found.
     */
    private ServerPlayer csSpanishSuccession(ChangeSet cs, LogBuilder lb,
                                             Event event) {
        Limit yearLimit
            = event.getLimit("model.limit.spanishSuccession.year");
        if (!yearLimit.evaluate(this)) return null;

        Limit weakLimit
            = event.getLimit("model.limit.spanishSuccession.weakestPlayer");
        Limit strongLimit
            = event.getLimit("model.limit.spanishSuccession.strongestPlayer");
        Map<Player, Integer> scores = new HashMap<>();
        boolean ready = false;
        for (Player player : getLiveEuropeanPlayers(null)) {
            if (player.isREF()) continue;
            ready |= strongLimit.evaluate(player);
            // Human players can trigger the event, but only transfer assets
            // between AI players.
            if (player.isAI()) { 
                scores.put(player, player.getSpanishSuccessionScore());
            }
        }
        if (!ready) return null; // No player meets the support limit.

        int bestScore = Integer.MIN_VALUE;
        int worstScore = Integer.MAX_VALUE;
        Player weakestAIPlayer = null;
        Player strongestAIPlayer = null;
        for (Entry<Player, Integer> entry : scores.entrySet()) {
            Player player = entry.getKey();
            int score = entry.getValue();
            if (worstScore > score && weakLimit.evaluate(player)) {
                worstScore = score;
                weakestAIPlayer = player;
            }
            if (bestScore < score) {
                bestScore = score;
                strongestAIPlayer = player;
            }
        }
        if (weakestAIPlayer == null
            || strongestAIPlayer == null
            || weakestAIPlayer == strongestAIPlayer) return null;

        lb.add("Spanish succession scores[");
        for (Entry<Player, Integer> entry : scores.entrySet()) {
            lb.add(" ", entry.getKey().getName(), "=", entry.getValue());
        }
        lb.add(" ]=> ", weakestAIPlayer.getName(),
               " cedes to ", strongestAIPlayer.getName(), ":");
        List<Tile> tiles = new ArrayList<>();
        ServerPlayer strongest = (ServerPlayer)strongestAIPlayer;
        ServerPlayer weakest = (ServerPlayer)weakestAIPlayer;
        for (Player player : getLiveNativePlayers(null)) {
            for (IndianSettlement is : player.getIndianSettlements()) {
                if (!is.hasMissionary(weakest)) continue;
                lb.add(" ", is.getName(), "(mission)");
                is.getTile().cacheUnseen(strongest);//+til
                tiles.add(is.getTile());
                is.setContacted(strongest);//-til
                ServerUnit missionary = (ServerUnit)is.getMissionary();
                if (weakest.csChangeOwner(missionary, strongest,
                        ChangeType.CAPTURE, null, cs)) {//-vis(both),-til
                    is.getTile().updateIndianSettlement(strongest);
                    cs.add(See.perhaps().always(strongest), is);
                }
            }
        }
        for (Colony colony : weakest.getColonies()) {
            for (Tile t : colony.getOwnedTiles()) {
                t.cacheUnseen();//+til
                tiles.add(t);
            }
            ((ServerColony)colony).csChangeOwner(strongest,
                                                 cs);//-vis(both),-til
            cs.add(See.only(strongest),
                strongest.exploreForSettlement(colony));
            lb.add(" ", colony.getName());
        }
        for (Unit unit : weakest.getUnits()) {
            if (weakest.csChangeOwner(unit, strongest, 
                    ChangeType.CAPTURE, null, cs)) { //-vis(both)
                unit.setMovesLeft(0);
                unit.setState(Unit.UnitState.ACTIVE);
                lb.add(" ", unit.getId());
                if (unit.getLocation() instanceof Europe) {
                    unit.setLocation(strongestAIPlayer.getEurope());//-vis
                    cs.add(See.only(strongest), unit);
                } else if (unit.getLocation() instanceof HighSeas) {
                    unit.setLocation(strongestAIPlayer.getHighSeas());//-vis
                    cs.add(See.only(strongest), unit);
                } else if (unit.getLocation() instanceof Tile) {
                    Tile tile = unit.getTile();
                    if (!tiles.contains(tile)) tiles.add(tile);
                }
            }
        }

        StringTemplate loser = weakestAIPlayer.getNationLabel();
        StringTemplate winner = strongestAIPlayer.getNationLabel();
        cs.addMessage(See.all(),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                "model.game.spanishSuccession",
                strongestAIPlayer)
                .addStringTemplate("%loserNation%", loser)
                .addStringTemplate("%nation%", winner));
        cs.addGlobalHistory(this,
            new HistoryEvent(getTurn(),
                HistoryEvent.HistoryEventType.SPANISH_SUCCESSION, null)
                .addStringTemplate("%loserNation%", loser)
                .addStringTemplate("%nation%", winner));
        setSpanishSuccession(true);
        cs.addPartial(See.all(), this, "spanishSuccession");
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


    // Serialization

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "serverGame".
     */
    @Override
    public String getServerXMLElementTagName() {
        return "serverGame";
    }
}
