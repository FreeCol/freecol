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
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.NameCache;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Event;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsLocation;
import net.sf.freecol.common.model.HighSeas;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Limit;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.ModelMessage.MessageType;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.SimpleCombatModel;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitChangeType;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ChangeSet.See;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.NewTurnMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.option.GameOptions;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * The server representation of the game.
 */
public class ServerGame extends Game implements TurnTaker {

    private static final Logger logger = Logger.getLogger(ServerGame.class.getName());

    /** Timestamp of last move, if any.  Do not serialize. */
    private long lastTime = -1L;


    /**
     * Creates a new game model.
     *
     * Public for the test suite.
     *
     * @param specification The {@code Specification} to use in this game.
     */
    public ServerGame(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new game model.
     *
     * Called from the FreeColServer constructor to create a
     * completely new game.
     *
     * @param specification The {@code Specification} to use in this game.
     * @param random A pseudo-random number source.
     */
    public ServerGame(Specification specification, Random random) {
        this(specification);

        this.setNationOptions(new NationOptions(specification));
        this.randomize(random);
        this.establishUnknownEnemy();
    }

    /**
     * Initiate a new {@code ServerGame} with information from a
     * saved game.
     *
     * Called from FreeColServer.readGame when restoring a saved game.
     *
     * @param specification The {@code Specification} to use in this game.
     * @param xr The input stream containing the XML.
     * @exception XMLStreamException if an error occurred during parsing.
     */
    public ServerGame(Specification specification, FreeColXMLReader xr)
        throws XMLStreamException {
        this(specification);

        readFromXML(xr);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getNextId() {
        int ret = this.nextId;
        this.nextId++;
        return ret;
    }

    /**
     * Get a list of connected players, optionally excluding supplied ones.
     *
     * Do *not* restrict this only to live players, dead players still
     * need to be communicated with (e.g. to tell them they are dead,
     * or to revive them).
     *
     * @param players The {@code Player}s to exclude.
     * @return A list of all connected players, with exclusions.
     */
    public List<Player> getConnectedPlayers(Player... players) {
        final Predicate<Player> connPred = p ->
            p.isConnected() && none(players, matchKey(p));
        synchronized (this.players) {
            return transform(this.players, connPred);
        }
    }

    /**
     * Send a change set to all live players.
     *
     * @param cs The {@code ChangeSet} to send.
     */
    public void sendToAll(ChangeSet cs) {
        sendToList(getConnectedPlayers(), cs);
    }
    
    /**
     * Send a change set to all players, optionally excluding one.
     *
     * @param player A {@code Player} to exclude.
     * @param cs The {@code ChangeSet} encapsulating the update.
     */
    public void sendToOthers(Player player, ChangeSet cs) {
        sendToList(getConnectedPlayers(player), cs);
    }

    /**
     * Send a change set to a list of players.
     *
     * @param players The list of {@code Player}s to send to.
     * @param cs The {@code ChangeSet} to send.
     */
    public void sendToList(List<Player> players, ChangeSet cs) {
        for (Player p : players) sendTo(p, cs);
    }

    /**
     * Send a change set to one player.
     *
     * @param player The {@code Player} to send to.
     * @param cs The {@code ChangeSet} to send.
     * @return True if the change was sent.
     */
    public boolean sendTo(Player player, ChangeSet cs) {
        try {
            return player.send(cs);
        } catch (Exception e) {
            // Catch all manner of exceptions here to localize failure
            // to just one player.  Nothing is *expected*, but the
            // entire output side of serialization is potentially
            // exercised here, so it is a good place to find new fails.
            logger.log(Level.WARNING, "sendTo(" + player.getId()
                + "," + cs + ") failed", e);
        }
        return false;
    }

    /** Currently unused
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
    private Object makeServerObject(String type, String id)
        throws ClassNotFoundException, IllegalAccessException,
               InstantiationException, InvocationTargetException,
               NoSuchMethodException {
        type = "net.sf.freecol.server.model." + capitalize(type);
        Class<?> c = Class.forName(type);
        return c.getConstructor(Game.class, String.class)
            .newInstance(this, id);
    }
     */

    /**
     * Establish the unknown enemy player.
     *
     * @return The new unknown enemy {@code Player}.
     */
    public final Player establishUnknownEnemy() {
        Player ret = getUnknownEnemy();
        if (ret != null) return ret;
        ServerPlayer enemy = new ServerPlayer(this, false,
            getSpecification().getUnknownEnemyNation());
        setUnknownEnemy(enemy);
        return enemy;
    }

    /**
     * Update the players.
     *
     * @param players A list of new {@code Player}s.
     */
    public void updatePlayers(List<? extends Player> players) {
        if (!players.isEmpty()) {
            sendToAll(new ChangeSet().addPlayers(players));
        }
    }

    /**
     * Change the AI state of a player.
     *
     * @param player The {@code Player} to change.
     * @param ai The new AI state.
     */
    public void changeAI(Player player, boolean ai) {
        player.setAI(ai);
        sendToAll(ChangeSet.aiChange(player, ai));
    }

    /**
     * Randomize a new game.
     *
     * @param random A pseudo-random number source.
     */
    private final void randomize(Random random) {
        if (random != null) NameCache.requireCitiesOfCibola(random);
    }

    /**
     * Checks if anybody has won this game.
     *
     * @return The {@code ServerPlayer} who has won the game or null if none.
     */
    public ServerPlayer checkForWinner() {
        final Specification spec = getSpecification();
        if (spec.getBoolean(GameOptions.VICTORY_DEFEAT_REF)) {
            Player winner = find(getLiveEuropeanPlayers(),
                p -> p.getPlayerType() == Player.PlayerType.INDEPENDENT);
            if (winner != null) return (ServerPlayer)winner;
        }
        if (spec.getBoolean(GameOptions.VICTORY_DEFEAT_EUROPEANS)) {
            List<Player> winners = transform(getLiveEuropeanPlayers(),
                                             p -> !p.isREF());
            if (winners.size() == 1) return (ServerPlayer)winners.get(0);
        }
        if (spec.getBoolean(GameOptions.VICTORY_DEFEAT_HUMANS)) {
            List<Player> winners = transform(getLiveEuropeanPlayers(),
                                             p -> !p.isAI());
            if (winners.size() == 1) return (ServerPlayer)winners.get(0);
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
        logger.finest("Turn is now " + getTurn() + "/" + duration);
        cs.add(See.all(), new NewTurnMessage(getTurn()));
    }

    /**
     * Is it time for the Spanish Succession settlement?
     *
     * @param event The Spanish Succession {@code Event}.
     * @param players A list to fill with the strongest and weakest AIs.
     * @param lb A {@code LogBuilder} to log to.
     * @return True if the Spanish Succession can proceed.
     */
    private boolean spanishSuccessionReady(Event event, List<Player> players,
                                           LogBuilder lb) {
        final Limit yearLimit
            = event.getLimit("model.limit.spanishSuccession.year");
        if (!yearLimit.evaluate(this)) return false;

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
        players.add(weakAI);
        players.add(0, strongAI); // Strong first
        return ready;
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
        // Do not proceed if no player meets the support limit or if there
        // are not clearly identifiable strong and weak AIs.
        List<Player> ail = new ArrayList<>();
        if (!spanishSuccessionReady(event, ail, lb)) return null;
        final Player strongAI = ail.get(0);
        final Player weakAI = ail.get(1);
        if (weakAI == null || strongAI == null
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
                is.setContacted(strongAI);//-til
                ServerUnit missionary = (ServerUnit)is.getMissionary();
                if (weakest.csChangeOwner(missionary, strongAI,//-vis(both),-til
                                          UnitChangeType.CAPTURE, null, cs)) {
                    is.getTile().updateIndianSettlement(strongAI);
                    cs.add(See.perhaps().always(strongAI), is);
                }
            });
        List<Colony> cl = weakAI.getColonyList();
        Set<Unit> contacts = new HashSet<>(cl.size());
        for (Colony c : cl) {
            updated.addAll(c.getOwnedTiles());
            ((ServerColony)c).csChangeOwner(strongAI, false, cs);//-vis(both),-til
            lb.add(" ", c.getName());
            contacts.add(c.getFirstUnit());
        }
        for (Unit unit : weakAI.getUnitSet()) {
            lb.add(" ", unit.getId());
            if (unit.isOnCarrier()) {
                ; // Allow carrier to handle
            } else if (!weakest.csChangeOwner(unit, strongAI, //-vis(both)
                    UnitChangeType.CAPTURE, null, cs)) {
                logger.warning("Owner change failed for " + unit);
            } else {
                unit.setMovesLeft(0);
                unit.setState(Unit.UnitState.ACTIVE);
                if (unit.getLocation() instanceof Europe) {
                    unit.setLocation(strongAI.getEurope());//-vis
                    cs.add(See.only(strongAI), unit);
                } else if (unit.getLocation() instanceof HighSeas) {
                    unit.setLocation(strongAI.getHighSeas());//-vis
                    cs.add(See.only(strongAI), unit);
                } else if (unit.isOnTile()) {
                    Tile tile = unit.getTile();
                    if (!tiles.contains(tile)) tiles.add(tile);
                }
                contacts.add(unit);
            }
        }
        for (Unit u : contacts) {
            if (u.hasTile()) {
                ((ServerUnit)u).csNewContactCheck(u.getTile(), false, cs);
            }
        }
        
        StringTemplate loser = weakAI.getNationLabel();
        StringTemplate winner = strongAI.getNationLabel();
        setSpanishSuccession(true);
        cs.addPartial(See.all(), this,
            "spanishSuccession", Boolean.TRUE.toString());
        tiles.removeAll(updated);
        cs.add(See.perhaps(), tiles);
        
        weakest.csWithdraw(cs, //+vis(weakest)
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                             "model.game.spanishSuccession", strongAI)
                .addStringTemplate("%loserNation%", loser)
                .addStringTemplate("%nation%", winner),
            new HistoryEvent(getTurn(), HistoryEvent.HistoryEventType.SPANISH_SUCCESSION, null)
                .addStringTemplate("%loserNation%", loser)
                .addStringTemplate("%nation%", winner));
        strongAI.invalidateCanSeeTiles();//+vis(strongest)

        // Trace fail where not all units are transferred
        for (FreeColGameObject fcgo : getFreeColGameObjectList()) {
            if (fcgo instanceof Ownable
                && ((Ownable)fcgo).getOwner() == weakAI) {
                throw new RuntimeException("Lurking " + weakAI.getId()
                    + " fcgo: " + fcgo);
            }
        }

        return weakest;
    }

    /**
     * Accept a diplomatic trade.  Handles the transfers of TradeItems.
     *
     * Note that first contact contexts may not necessarily have a settlement,
     * but this is ok because first contact trade can only include stance
     * and gold trade items.
     *
     * @param agreement The {@code DiplomacyTrade} agreement.
     * @param unit The {@code Unit} that is trading.
     * @param settlement The {@code Settlement} that is trading.
     * @param cs A {@code ChangeSet} to update.
     * @return True if the trade was completed successfully.
     */
    public boolean csAcceptTrade(DiplomaticTrade agreement, Unit unit,
                                 Settlement settlement, ChangeSet cs) {
        final Player srcPlayer = agreement.getSender();
        final Player dstPlayer = agreement.getRecipient();
        boolean visibilityChange = false;
        
        // Check trade carefully before committing.
        boolean fail = false;
        for (TradeItem tradeItem : agreement.getItems()) {
            final Player source = tradeItem.getSource();
            final Player dest = tradeItem.getDestination();
            if (!tradeItem.isValid()) {
                logger.warning("Trade with invalid tradeItem: " + tradeItem);
                fail = true;
                continue;
            }
            if (source != srcPlayer && source != dstPlayer) {
                logger.warning("Trade with invalid source: "
                               + ((source == null) ? "null" : source.getId()));
                fail = true;
                continue;
            }
            if (dest != srcPlayer && dest != dstPlayer) {
                logger.warning("Trade with invalid destination: "
                               + ((dest == null) ? "null" : dest.getId()));
                fail = true;
                continue;
            }

            Colony colony = tradeItem.getColony(getGame());
            if (colony != null && !source.owns(colony)) {
                logger.warning("Trade with invalid source owner: " + colony);
                fail = true;
                continue;
            }
            int gold = tradeItem.getGold();
            if (gold > 0 && !source.checkGold(gold)) {
                logger.warning("Trade with invalid gold: " + gold);
                fail = true;
                continue;
            }

            Goods goods = tradeItem.getGoods();
            if (goods != null) {
                Location loc = goods.getLocation();
                if (loc instanceof Ownable
                    && !source.owns((Ownable)loc)) {
                    logger.warning("Trade with invalid source owner: " + loc);
                    fail = true;
                } else if (!(loc instanceof GoodsLocation
                        && loc.contains(goods))) {
                    logger.warning("Trade of unavailable goods " + goods
                        + " at " + loc);
                    fail = true;
                } else if (dest.owns(unit) && !unit.couldCarry(goods)) {
                    logger.warning("Trade unit can not carry traded goods: "
                        + goods);
                    fail = true;
                }
            }

            // Stance trade fail is harmless
            Unit u = tradeItem.getUnit();
            if (u != null) {
                if (!source.owns(u)) {
                    logger.warning("Trade with invalid source owner: " + u);
                    fail = true;
                    continue;
                } else if (dest.owns(unit) && !unit.couldCarry(u)) {
                    logger.warning("Trade unit can not carry traded unit: "
                        + u);
                    fail = true;
                }
            }
        }
        if (fail) return false;

        for (TradeItem tradeItem : agreement.getItems()) {
            final Player source = tradeItem.getSource();
            final Player dest = tradeItem.getDestination();
            // Collect changes for updating.  Not very OO but
            // TradeItem should not know about server internals.
            // Take care to show items that change hands to the *old*
            // owner too.
            Stance stance = tradeItem.getStance();
            if (stance != null
                && source.getStance(dest) != stance
                && !((ServerPlayer)source).csChangeStance(stance, dest, true, cs)) {
                logger.warning("Stance trade failure: " + stance);
            }
            Colony colony = tradeItem.getColony(getGame());
            if (colony != null) {
                ((ServerColony)colony).csChangeOwner(dest, false, cs);//-vis(both),-til
                visibilityChange = true;
            }
            int gold = tradeItem.getGold();
            if (gold > 0) {
                source.modifyGold(-gold);
                dest.modifyGold(gold);
                cs.addPartial(See.only(source), source,
                    "gold", String.valueOf(source.getGold()),
                    "score", String.valueOf(source.getScore()));
                cs.addPartial(See.only(dest), dest,
                    "gold", String.valueOf(dest.getGold()),
                    "score", String.valueOf(dest.getScore()));
            }
            Goods goods = tradeItem.getGoods();
            if (goods != null && settlement != null) {
                if (dest.owns(settlement)) {
                    goods.setLocation(unit);
                    GoodsLocation.moveGoods(unit, goods.getType(), goods.getAmount(), settlement);
                    cs.add(See.only(source), unit);
                    cs.add(See.only(dest), settlement.getGoodsContainer());
                } else {
                    goods.setLocation(settlement);
                    GoodsLocation.moveGoods(settlement, goods.getType(), goods.getAmount(), unit);
                    cs.add(See.only(dest), unit);
                    cs.add(See.only(source), settlement.getGoodsContainer());
                }
            }
            Player victim = tradeItem.getVictim();
            if (victim != null) {
                if (((ServerPlayer)source).csChangeStance(Stance.WAR, victim, true, cs)) {
                    // Have to add in an explicit stance change and
                    // message because the player does not normally
                    // have visibility of stance changes between other nations.
                    cs.addStance(See.only(dest), source, Stance.WAR, victim);
                    cs.addMessage(dest,
                        new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                         Stance.WAR.getOtherStanceChangeKey(),
                                         source)
                            .addStringTemplate("%attacker%",
                                source.getNationLabel())
                            .addStringTemplate("%defender%",
                                victim.getNationLabel()));
                } else {
                    logger.warning("Incite trade failure: " + victim);
                }
            }                
            ServerUnit newUnit = (ServerUnit)tradeItem.getUnit();
            if (newUnit != null && settlement != null) {
                Player former = newUnit.getOwner();
                Tile oldTile = newUnit.getTile();
                Location newLoc;
                if (unit.isOnCarrier()) {
                    Unit carrier = unit.getCarrier();
                    if (!carrier.couldCarry(newUnit)) {
                        logger.warning("Can not add " + newUnit
                            + " to " + carrier);
                        continue;
                    }
                    newLoc = carrier;
                } else if (dest == unit.getOwner()) {
                    newLoc = unit.getTile();
                } else {
                    newLoc = settlement.getTile();
                }
                if (((ServerPlayer)source).csChangeOwner(newUnit, dest,
                        UnitChangeType.CAPTURE, newLoc, cs)) {//-vis(both)
                    newUnit.setMovesLeft(0);
                    cs.add(See.perhaps().always(former), oldTile);
                }
                visibilityChange = true;
            }
        }
        if (visibilityChange) {
            srcPlayer.invalidateCanSeeTiles();//+vis(srcPlayer)
            dstPlayer.invalidateCanSeeTiles();//+vis(dstPlayer)
        }
        return true;
    }


    // Implement TurnTaker

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

    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        // Two games are not the same just because they have the same
        // identifier, but to avoid having to check everything in the
        // Game just insist on object equality for the equals() test,
        // and accept the basic id-based hashCode().
        return this == o;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Utils.hashCode(getId());
    }
}
