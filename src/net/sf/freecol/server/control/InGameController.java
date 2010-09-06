/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LastSale;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelController;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.PlayerExploredTile;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Settlement.SettlementType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.TradeRoute.Stop;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.ChangePriority;
import net.sf.freecol.server.control.ChangeSet.See;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The main server controller.
 */
public final class InGameController extends Controller {

    private static Logger logger = Logger.getLogger(InGameController.class.getName());

    // TODO: options, spec?
    // Alarm adjustments.
    public static final int ALARM_RADIUS = 2;
    public static final int ALARM_TILE_IN_USE = 2;
    public static final int ALARM_NEW_MISSIONARY = -100;
    public static final int ALARM_MISSIONARY_PRESENT = -10;

    // Score bonus on declaration of independence.
    public static final int SCORE_INDEPENDENCE_DECLARED = 100;

    // Score bonus on achieving independence.
    public static final int SCORE_INDEPENDENCE_GRANTED = 1000;

    // The server random number source.
    private final Random random;

    public int debugOnlyAITurns = 0;

    public static enum MigrationType {
        NORMAL,     // Unit decided to migrate
        RECRUIT,    // Player is paying
        FOUNTAIN    // As a result of a Fountain of Youth discovery
    }


    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public InGameController(FreeColServer freeColServer) {
        super(freeColServer);

        random = freeColServer.getServerRandom();
    }


    /**
     * Get a list of all server players, optionally excluding supplied ones.
     *
     * @param serverPlayers The <code>ServerPlayer</code>s to exclude.
     * @return A list of all connected server players, with exclusions.
     */
    public List<ServerPlayer> getOtherPlayers(ServerPlayer... serverPlayers) {
        List<ServerPlayer> result = new ArrayList<ServerPlayer>();
        outer: for (Player otherPlayer : getGame().getPlayers()) {
            ServerPlayer enemyPlayer = (ServerPlayer) otherPlayer;
            if (!enemyPlayer.isConnected()) continue;
            for (ServerPlayer exclude : serverPlayers) {
                if (enemyPlayer == exclude) continue outer;
            }
            result.add(enemyPlayer);
        }
        return result;
    }


    /**
     * Send a set of changes to all players.
     *
     * @param cs The <code>ChangeSet</code> to send.
     */
    private void sendToAll(ChangeSet cs) {
        sendToList(getOtherPlayers(), cs);
    }

    /**
     * Send an element to all players.
     * Deprecated, please avoid if possible.
     *
     * @param element The <code>Element</code> to send.
     */
    public void sendToAll(Element element) {
        sendToList(getOtherPlayers(), element);
    }

    /**
     * TODO: deprecated, kill as soon as last users in igih are gone.
     * Send an update to all players except one.
     *
     * @param serverPlayer A <code>ServerPlayer</code> to exclude.
     * @param objects The <code>FreeColGameObject</code>s to update.
     */
    public void sendToOthers(ServerPlayer serverPlayer,
                             FreeColGameObject... objects) {
        ChangeSet cs = new ChangeSet();
        for (FreeColGameObject fcgo : objects) cs.add(See.perhaps(), fcgo);
        sendToOthers(serverPlayer, cs);
    }

    /**
     * Send an update to all players except one.
     *
     * @param serverPlayer A <code>ServerPlayer</code> to exclude.
     * @param cs The <code>ChangeSet</code> encapsulating the update.
     */
    private void sendToOthers(ServerPlayer serverPlayer, ChangeSet cs) {
        sendToList(getOtherPlayers(serverPlayer), cs);
    }

    /**
     * Send an element to all players except one.
     * Deprecated, please avoid if possible.
     *
     * @param serverPlayer A <code>ServerPlayer</code> to exclude.
     * @param element An <code>Element</code> to send.
     */
    public void sendToOthers(ServerPlayer serverPlayer, Element element) {
        sendToList(getOtherPlayers(serverPlayer), element);
    }

    /**
     * Send an update to a list of players.
     *
     * @param serverPlayers The <code>ServerPlayer</code>s to send to.
     * @param cs The <code>ChangeSet</code> encapsulating the update.
     */
    private void sendToList(List<ServerPlayer> serverPlayers, ChangeSet cs) {
        for (ServerPlayer s : serverPlayers) sendElement(s, cs);
    }

    /**
     * Send an element to a list of players.
     * Deprecated, please avoid if possible.
     *
     * @param serverPlayers The <code>ServerPlayer</code>s to send to.
     * @param element An <code>Element</code> to send.
     */
    public void sendToList(List<ServerPlayer> serverPlayers, Element element) {
        if (element != null) {
            for (ServerPlayer s : serverPlayers) {
                sendElement(s, element);
            }
        }
    }

    /**
     * Send an element to a specific player.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to update.
     * @param cs A <code>ChangeSet</code> to build an <code>Element</code> with.
     */
    private void sendElement(ServerPlayer serverPlayer, ChangeSet cs) {
        sendElement(serverPlayer, cs.build(serverPlayer));
    }

    /**
     * Send an element to a specific player.
     * Deprecated, please avoid if possible.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to update.
     * @param element An <code>Element</code> containing the update.
     */
    public void sendElement(ServerPlayer serverPlayer, Element element) {
        if (element != null && serverPlayer.isConnected()) {
            try {
                serverPlayer.getConnection().sendAndWait(element);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Send element failure", e);
            }
        }
    }

    /**
     * Send an element to a specific player.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to update.
     * @param cs A <code>ChangeSet</code> to build an <code>Element</code> with.
     * @return An <code>Element</code> returned in response to the query.
     */
    private Element askElement(ServerPlayer serverPlayer, ChangeSet cs) {
        return askElement(serverPlayer, cs.build(serverPlayer));
    }

    /**
     * Ask for a reply from a specific player.
     * Deprecated, please avoid if possible.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to ask.
     * @param element An <code>Element</code> containing a query.
     * @return An <code>Element</code> returned in response to the query.
     */
    public Element askElement(ServerPlayer serverPlayer, Element element) {
        if (element != null && serverPlayer.isConnected()) {
            try {
                return serverPlayer.getConnection().ask(element);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Ask element failure", e);
            }
        }
        return null;
    }


    /**
     * Ends the turn of the given player.
     * 
     * @param player The player to end the turn of.
     */
    public void endTurn(ServerPlayer player) {
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer oldPlayer = (ServerPlayer) getGame().getCurrentPlayer();
        
        if (oldPlayer != player) {
            throw new IllegalArgumentException("It is not "
                + player.getName() + "'s turn, it is "
                + ((oldPlayer == null) ? "noone" : oldPlayer.getName()) + "'s!");
        }
        
        for (;;) {
            player.clearModelMessages();
            freeColServer.getModelController().clearTaskRegister();

            Player winner = checkForWinner();
            if (winner != null
                && !(freeColServer.isSingleplayer() && winner.isAI())) {
                ChangeSet cs = new ChangeSet();
                cs.addTrivial(See.all(), "gameEnded",
                              ChangePriority.CHANGE_NORMAL,
                              "winner", winner.getId());
                sendToAll(cs);
                return;
            }
        
            // Keep ending turn for non-AI connected players in debug mode.
            player = (ServerPlayer) nextPlayer();
            if (player == null
                || player.isAI()
                || (player.isConnected() && debugOnlyAITurns <= 0)) break;
        }
    }

    /**
     * Checks if anybody has won the game and returns that player.
     *
     * @return The <code>Player</code> who have won the game or <i>null</i>
     *         if the game is not finished.
     */
    public Player checkForWinner() {
        List<Player> players = getGame().getPlayers();
        GameOptions go = getGame().getGameOptions();
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_REF)) {
            for (Player player : players) {
                if (player.getPlayerType() == PlayerType.INDEPENDENT) {
                    return player;
                }
            }
        }
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_EUROPEANS)) {
            Player winner = null;
            for (Player player : players) {
                if (!player.isDead() && player.isEuropean()
                    && !player.isREF()) {
                    if (winner != null) { // A live European player
                        winner = null;
                        break;
                    }
                    winner = player;
                }
            }
            if (winner != null) return winner;
        }
        if (go.getBoolean(GameOptions.VICTORY_DEFEAT_HUMANS)) {
            Player winner = null;
            for (Player player : players) {
                if (!player.isDead() && !player.isAI()) {
                    if (winner != null) { // A live human player
                        winner = null;
                        break;
                    }
                    winner = player;
                }
            }
            if (winner != null) return winner;
        }
        return null;
    }

    /**
     * Sets a new current player and notifies the clients.
     *
     * @return The new current player.
     */
    private Player nextPlayer() {
        ServerGame game = getGame();

        boolean human = false;
        for (Player p : game.getPlayers()) {
            if (!p.isDead() && !p.isAI() && ((ServerPlayer) p).isConnected()) {
                human = true;
                break;
            }
        }
        if (!human) {
            game.setCurrentPlayer(null);
            return null;
        }

        if (game.isNextPlayerInNewTurn()) {
            ChangeSet cs = new ChangeSet();
            game.newTurn();
            if (game.getTurn().getAge() > 1 && !game.getSpanishSuccession()) {
                csSpanishSuccession(cs);
                game.setSpanishSuccession(true);
            }
            if (debugOnlyAITurns > 0) debugOnlyAITurns--;
            cs.addTrivial(See.all(), "newTurn", ChangePriority.CHANGE_NORMAL,
                "turn", Integer.toString(game.getTurn().getNumber()));
            sendToAll(cs);
        }

        ServerPlayer newPlayer = (ServerPlayer) game.getNextPlayer();
        game.setCurrentPlayer(newPlayer);
        if (newPlayer != null) {
            synchronized (newPlayer) {
                if (newPlayer.checkForDeath()) {
                    ChangeSet cs = new ChangeSet();
                    csKillPlayer(newPlayer, cs);
                    logger.info(newPlayer.getNation() + " is dead.");
                    sendToAll(cs);
                    return nextPlayer();
                }
            }

            {
                ChangeSet cs = new ChangeSet();
                csNewTurn(newPlayer, cs);
                cs.addTrivial(See.all(), "setCurrentPlayer",
                              ChangePriority.CHANGE_NORMAL,
                              "player", newPlayer.getId());
                sendToAll(cs);
            }
        }
        return newPlayer;
    }

    /**
     * Starts a new turn for a player.
     *
     * @param newPlayer The <code>ServerPlayer</code> to start a turn for.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csNewTurn(ServerPlayer newPlayer, ChangeSet cs) {
        Game game = getGame();
        if (newPlayer.isEuropean()) {
            csBombardEnemyShips(newPlayer, cs);

            csYearlyGoodsRemoval(newPlayer, cs);

            if (newPlayer.getCurrentFather() == null
                && newPlayer.getSettlements().size() > 0) {
                chooseFoundingFather(newPlayer);
            }

            if (newPlayer.getMonarch() != null && newPlayer.isConnected()) {
                List<RandomChoice<MonarchAction>> choices
                    = newPlayer.getMonarch().getActionChoices();
                final ServerPlayer player = newPlayer;
                final MonarchAction action
                    = (choices == null) ? MonarchAction.NO_ACTION
                    : RandomChoice.getWeightedRandom(random, choices);
                Thread t = new Thread("monarchAction") {
                        public void run() {
                            try {
                                monarchAction(player, action);
                            } catch (Exception e) {
                                logger.log(Level.WARNING, "Monarch action failed!", e);
                            }
                        }
                    };
                t.start();
            }

        } else if (newPlayer.isIndian()) {
            // We do not have to worry about Player level stance
            // changes driving Stance, as that is delegated to the AI.
            //
            // However we want to notify of individual settlements
            // that change tension level, but there are complex
            // interactions between settlement and player tensions.
            // The simple way to do it is just to save all old tension
            // levels and check if they have changed after applying
            // all the changes.
            List<IndianSettlement> allSettlements
                = newPlayer.getIndianSettlements();
            java.util.Map<IndianSettlement,
                java.util.Map<Player, Tension.Level>> oldLevels
                = new HashMap<IndianSettlement,
                java.util.Map<Player, Tension.Level>>();
            for (IndianSettlement settlement : allSettlements) {
                java.util.Map<Player, Tension.Level> oldLevel
                    = new HashMap<Player, Tension.Level>();
                oldLevels.put(settlement, oldLevel);
                for (Player enemy : getGame().getEuropeanPlayers()) {
                    Tension alarm = settlement.getAlarm(enemy);
                    if (alarm != null) oldLevel.put(enemy, alarm.getLevel());
                }
            }

            // Do the settlement alarms first.
            for (IndianSettlement settlement : allSettlements) {
                java.util.Map<Player, Integer> extra
                    = new HashMap<Player, Integer>();
                for (Player enemy : getGame().getEuropeanPlayers()) {
                    extra.put(enemy, new Integer(0));
                }

                // Look at the uses of tiles surrounding the settlement.
                int alarmRadius = settlement.getRadius() + ALARM_RADIUS;
                int alarm = 0;
                for (Tile tile: settlement.getTile()
                         .getSurroundingTiles(alarmRadius)) {
                    Colony colony = tile.getColony();
                    if (tile.getFirstUnit() != null) { // Military units
                        Player enemy =  tile.getFirstUnit().getOwner();
                        if (enemy.isEuropean()) {
                            alarm = extra.get(enemy);
                            for (Unit unit : tile.getUnitList()) {
                                if (unit.isOffensiveUnit() && !unit.isNaval()) {
                                    alarm += unit.getType().getOffence();
                                }
                            }
                            extra.put(enemy, alarm);
                        }
                    } else if (colony != null) { // Colonies
                        Player enemy = colony.getOwner();
                        extra.put(enemy, extra.get(enemy).intValue()
                                  + ALARM_TILE_IN_USE
                                  + colony.getUnitCount());
                    } else if (tile.getOwningSettlement() != null) { // Control
                        Player enemy = tile.getOwningSettlement().getOwner();
                        if (enemy != null && enemy.isEuropean()) {
                            extra.put(enemy, extra.get(enemy).intValue()
                                      + ALARM_TILE_IN_USE);
                        }
                    }
                }
                // Missionary helps reducing alarm a bit
                if (settlement.getMissionary() != null) {
                    Unit mission = settlement.getMissionary();
                    int missionAlarm = ALARM_MISSIONARY_PRESENT;
                    if (mission.hasAbility("model.ability.expertMissionary")) {
                        missionAlarm *= 2;
                    }
                    Player enemy = mission.getOwner();
                    extra.put(enemy,
                              extra.get(enemy).intValue() + missionAlarm);
                }
                // Apply modifiers, and commit the total change.
                for (Entry<Player, Integer> entry : extra.entrySet()) {
                    Player player = entry.getKey();
                    int change = entry.getValue().intValue();
                    if (change != 0) {
                        change = (int) player.getFeatureContainer()
                            .applyModifier(change,
                                           "model.modifier.nativeAlarmModifier",
                                           null, getGame().getTurn());
                        settlement.modifyAlarm(player, change);
                    }
                }
            }

            // Calm down a bit at the whole-tribe level.
            for (Player enemy : getGame().getEuropeanPlayers()) {
                if (newPlayer.getTension(enemy).getValue() > 0) {
                    int change = -newPlayer.getTension(enemy).getValue()/100
                        - 4;
                    newPlayer.modifyTension(enemy, change);
                }
            }

            // Now collect the settlements that changed.
            for (IndianSettlement settlement : allSettlements) {
                java.util.Map<Player, Tension.Level> oldLevel
                    = oldLevels.get(settlement);
                for (Entry<Player, Tension.Level> entry : oldLevel.entrySet()) {
                    Player enemy = entry.getKey();
                    Tension.Level newLevel
                        = settlement.getAlarm(enemy).getLevel();
                    if (entry.getValue() != newLevel) {
                        cs.add(See.only(null).perhaps((ServerPlayer) enemy),
                               settlement);
                    }
                }
            }

            for (IndianSettlement indianSettlement: newPlayer.getIndianSettlements()) {
                if (indianSettlement.checkForNewMissionaryConvert()) {
                    // an Indian brave gets converted by missionary
                    Unit missionary = indianSettlement.getMissionary();
                    ServerPlayer european = (ServerPlayer) missionary.getOwner();
                    // search for a nearby colony
                    Tile settlementTile = indianSettlement.getTile();
                    Tile targetTile = null;
                    Iterator<Position> ffi = getGame().getMap().getFloodFillIterator(settlementTile.getPosition());
                    while (ffi.hasNext()) {
                        Tile t = getGame().getMap().getTile(ffi.next());
                        if (settlementTile.getDistanceTo(t) > IndianSettlement.MAX_CONVERT_DISTANCE) {
                            break;
                        }
                        if (t.getSettlement() != null && t.getSettlement().getOwner() == european) {
                            targetTile = t;
                            break;
                        }
                    }
        
                    if (targetTile != null) {
                        
                        List<UnitType> converts = getGame().getSpecification().getUnitTypesWithAbility("model.ability.convert");
                        if (converts.size() > 0) {
                            // perform the conversion from brave to convert in the server
                            Unit brave = indianSettlement.getUnitIterator().next();
                            String nationId = brave.getOwner().getNationID();
                            brave.dispose();
                            ModelController modelController = getGame().getModelController();
                            int random = modelController.getRandom(indianSettlement.getId() + "getNewConvertType", converts.size());
                            UnitType unitType = converts.get(random);
                            Unit unit = modelController.createUnit(indianSettlement.getId() + "newTurn100missionary", targetTile,
                                                                   european, unitType);
                            // and send update information to the client
                            try {
                                Element updateElement = Message.createNewRootElement("newConvert");
                                updateElement.setAttribute("nation", nationId);
                                updateElement.setAttribute("colonyTile", targetTile.getId());
                                updateElement.appendChild(unit.toXMLElement(european,updateElement.getOwnerDocument()));
                                european.getConnection().send(updateElement);
                                logger.info("New convert created for " + european.getName() + " with ID=" + unit.getId());
                            } catch (IOException e) {
                                logger.log(Level.WARNING, "Could not send message to: " + european.getName(), e);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove a standard yearly amount of storable goods, and
     * a random extra amount of a random type.
     *
     * @param serverPlayer The <code>ServerPlayer</code> whose market
     *            is to be updated.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csYearlyGoodsRemoval(ServerPlayer serverPlayer,
                                      ChangeSet cs) {
        List<GoodsType> goodsTypes = getGame().getSpecification()
            .getGoodsTypeList();
        Market market = serverPlayer.getMarket();

        // Pick a random type of goods to remove an extra amount of.
        GoodsType removeType;
        do {
            int randomGoods = random.nextInt(goodsTypes.size());
            removeType = goodsTypes.get(randomGoods);
        } while (!removeType.isStorable());

        // Remove standard amount, and the extra amount.
        for (GoodsType type : goodsTypes) {
            if (type.isStorable() && market.hasBeenTraded(type)) {
                int amount = getGame().getTurn().getNumber() / 10;
                if (type == removeType && amount > 0) {
                    amount += random.nextInt(2 * amount + 1);
                }
                if (amount > 0) {
                    market.addGoodsToMarket(type, -amount);
                }
            }
            if (market.hasPriceChanged(type)) {
                cs.add(See.only(serverPlayer), market.getMarketData(type));
                cs.addMessage(See.only(serverPlayer),
                              market.makePriceChangeMessage(type));
                market.flushPriceChange(type);
            }
        }
    }

    /**
     * Public version of the yearly goods removal (public so it can be
     * use in the Market test code).  Sends the market and change
     * messages to the player.
     *
     * @param serverPlayer The <code>ServerPlayer</code> whose market
     *            is to be updated.
     */
    public void yearlyGoodsRemoval(ServerPlayer serverPlayer) {
        ChangeSet cs = new ChangeSet();
        csYearlyGoodsRemoval(serverPlayer, cs);
        sendElement(serverPlayer, cs);
    }

    /**
     * Marks a player dead and remove any leftovers.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to kill.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private ChangeSet csKillPlayer(ServerPlayer serverPlayer, ChangeSet cs) {

        // Mark the player as dead.
        serverPlayer.setDead(true);
        cs.addDead(serverPlayer);

        // Notify everyone.
        cs.addMessage(See.all().except(serverPlayer),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                             ((serverPlayer.isEuropean())
                              ? "model.diplomacy.dead.european"
                              : "model.diplomacy.dead.native"),
                             serverPlayer)
                .addStringTemplate("%nation%", serverPlayer.getNationName()));

        // Clean up missions
        if (serverPlayer.isEuropean()) {
            for (ServerPlayer other : getOtherPlayers(serverPlayer)) {
                for (IndianSettlement s : other.getIndianSettlementsWithMission(serverPlayer)) {
                    Unit unit = s.getMissionary();
                    s.setMissionary(null);
                    cs.addDispose(serverPlayer, s.getTile(), unit);
                    cs.add(See.perhaps(), s.getTile());
                    s.getTile().updatePlayerExploredTiles();
                }
            }
        }

        // Remove settlements.  Update formerly owned tiles.
        List<Settlement> settlements = serverPlayer.getSettlements();
        while (!settlements.isEmpty()) {
            Settlement settlement = settlements.remove(0);
            for (Tile tile : settlement.getOwnedTiles()) {
                cs.add(See.perhaps(), tile);
            }
            cs.addDispose(serverPlayer, settlement.getTile(), settlement);
        }

        // Remove units
        List<Unit> units = serverPlayer.getUnits();
        while (!units.isEmpty()) {
            Unit unit = units.remove(0);
            if (unit.getLocation() instanceof Tile) {
                cs.add(See.perhaps(), unit.getTile());
            }
            cs.addDispose(serverPlayer, unit.getLocation(), unit);
        }

        return cs;
    }

    /**
     * Checks for an performs the War of Spanish Succession changes.
     *
     * @param cs A <code>ChangeSet</code> to update.
     * @return True if the Spanish Succession event occurred.
     */
    private boolean csSpanishSuccession(ChangeSet cs) {
        Game game = getGame();
        Player weakestAIPlayer = null;
        Player strongestAIPlayer = null;
        int rebelPlayers = 0;
        for (Player player : game.getEuropeanPlayers()) {
            if (player.isREF()) continue;
            if (player.isAI()) {
                if (weakestAIPlayer == null
                    || weakestAIPlayer.getScore() > player.getScore()) {
                    weakestAIPlayer = player;
                }
                if (strongestAIPlayer == null
                    || strongestAIPlayer.getScore() < player.getScore()) {
                    strongestAIPlayer = player;
                }
            }
            if (player.getSoL() > 50) {
                rebelPlayers++;
            }
        }

        // Only eliminate the weakest AI if there is at least one
        // nation with 50% rebels, there is a distinct weakest nation,
        // and it is not the sole nation with 50% rebels.
        if (rebelPlayers > 0
            && weakestAIPlayer != null && strongestAIPlayer != null
            && weakestAIPlayer != strongestAIPlayer
            && (weakestAIPlayer.getSoL() <= 50 || rebelPlayers > 1)) {
            for (Player player : game.getPlayers()) {
                for (IndianSettlement settlement : player.getIndianSettlementsWithMission(weakestAIPlayer)) {
                    Unit missionary = settlement.getMissionary();
                    missionary.setOwner(strongestAIPlayer);
                    settlement.getTile().updatePlayerExploredTiles();
                    cs.add(See.perhaps()
                           .always((ServerPlayer)strongestAIPlayer),
                           settlement);
                }
            }
            for (Colony colony : weakestAIPlayer.getColonies()) {
                colony.changeOwner(strongestAIPlayer);
                for (Tile tile : colony.getOwnedTiles()) {
                    cs.add(See.perhaps(), tile);
                }
            }
            for (Unit unit : weakestAIPlayer.getUnits()) {
                unit.setOwner(strongestAIPlayer);
                cs.add(See.perhaps(), unit);
            }

            StringTemplate loser = weakestAIPlayer.getNationName();
            StringTemplate winner = strongestAIPlayer.getNationName();
            cs.addMessage(See.all(),
                new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                 "model.diplomacy.spanishSuccession",
                                 strongestAIPlayer)
                    .addStringTemplate("%loserNation%", loser)
                    .addStringTemplate("%nation%", winner));
            for (Player p : game.getEuropeanPlayers()) {
                if (p != weakestAIPlayer) {
                    cs.addHistory((ServerPlayer) p,
                        new HistoryEvent(game.getTurn(),
                            HistoryEvent.EventType.SPANISH_SUCCESSION)
                            .addStringTemplate("%loserNation%", loser)
                            .addStringTemplate("%nation%", winner));
                }
            }
            weakestAIPlayer.setDead(true);
            cs.addDead((ServerPlayer) weakestAIPlayer);
            return true;
        }
        return false;
    }
    
    private void chooseFoundingFather(ServerPlayer player) {
        final ServerPlayer nextPlayer = player;
        Thread t = new Thread(FreeCol.SERVER_THREAD+"FoundingFather-thread") {
                public void run() {
                    List<FoundingFather> randomFoundingFathers = getRandomFoundingFathers(nextPlayer);
                    boolean atLeastOneChoice = false;
                    Element chooseFoundingFatherElement = Message.createNewRootElement("chooseFoundingFather");
                    for (FoundingFather father : randomFoundingFathers) {
                        chooseFoundingFatherElement.setAttribute(father.getType().toString(),
                                                                 father.getId());
                        atLeastOneChoice = true;
                    }
                    if (!atLeastOneChoice) {
                        nextPlayer.setCurrentFather(null);
                    } else {
                        Connection conn = nextPlayer.getConnection();
                        if (conn != null) {
                            try {
                                Element reply = conn.ask(chooseFoundingFatherElement);
                                FoundingFather father = getGame().getSpecification().
                                    getFoundingFather(reply.getAttribute("foundingFather"));
                                if (!randomFoundingFathers.contains(father)) {
                                    throw new IllegalArgumentException();
                                }
                                nextPlayer.setCurrentFather(father);
                            } catch (IOException e) {
                                logger.log(Level.WARNING, "Could not send message to: " + nextPlayer.getName(), e);
                            }
                        }
                    }
                }
            };
        t.start();
    }

    /**
     * Build a list of random FoundingFathers, one per type.
     * Do not include any the player has or are not available.
     * 
     * @param player The <code>Player</code> that should pick a founding
     *            father from this list.
     * @return A list of FoundingFathers.
     */
    private List<FoundingFather> getRandomFoundingFathers(Player player) {
        // Build weighted random choice for each father type
        Specification spec = getGame().getSpecification();
        int age = getGame().getTurn().getAge();
        EnumMap<FoundingFatherType, List<RandomChoice<FoundingFather>>> choices
            = new EnumMap<FoundingFatherType,
            List<RandomChoice<FoundingFather>>>(FoundingFatherType.class);
        for (FoundingFather father : spec.getFoundingFathers()) {
            if (!player.hasFather(father) && father.isAvailableTo(player)) {
                FoundingFatherType type = father.getType();
                List<RandomChoice<FoundingFather>> rc = choices.get(type);
                if (rc == null) {
                    rc = new ArrayList<RandomChoice<FoundingFather>>();
                }
                int weight = father.getWeight(age);
                rc.add(new RandomChoice<FoundingFather>(father, weight));
                choices.put(father.getType(), rc);
            }
        }

        // Select one from each father type
        List<FoundingFather> randomFathers = new ArrayList<FoundingFather>();
        String logMessage = "Random fathers";
        for (FoundingFatherType type : FoundingFatherType.values()) {
            List<RandomChoice<FoundingFather>> rc = choices.get(type);
            if (rc != null) {
                FoundingFather father = RandomChoice.getWeightedRandom(random, rc);
                randomFathers.add(father);
                logMessage += ":" + father.getNameKey();
            }
        }
        logger.info(logMessage);
        return randomFathers;
    }

    /**
     * Performs the monarchs actions.
     * 
     * @param serverPlayer The <code>ServerPlayer</code> whose monarch
     *            is acting.
     * @param action The monarch action.
     */
    private void monarchAction(ServerPlayer serverPlayer, MonarchAction action) {
        Monarch monarch = serverPlayer.getMonarch();
        Connection conn = serverPlayer.getConnection();
        Element monarchActionElement = Message.createNewRootElement("monarchAction");
        monarchActionElement.setAttribute("action", String.valueOf(action));

        switch (action) {
        case RAISE_TAX:
            int oldTax = serverPlayer.getTax();
            int newTax = monarch.raiseTax(random);
            Goods goods = serverPlayer.getMostValuableGoods();
            if (goods == null) return;
            monarchActionElement.setAttribute("amount", String.valueOf(newTax));
            // TODO: don't use localized name
            monarchActionElement.setAttribute("goods", Messages.message(goods.getNameKey()));
            monarchActionElement.setAttribute("force", String.valueOf(false));
            try {
                serverPlayer.setTax(newTax); // to avoid cheating
                Element reply = conn.ask(monarchActionElement);
                boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                if (!accepted) {
                    Colony colony = (Colony) goods.getLocation();
                    if (colony.getGoodsCount(goods.getType()) >= goods.getAmount()) {
                        serverPlayer.setTax(oldTax); // player hasn't accepted, restoring tax
                        Element removeGoodsElement = Message.createNewRootElement("removeGoods");
                        colony.removeGoods(goods);
                        serverPlayer.setArrears(goods);
                        colony.getFeatureContainer().addModifier(Modifier
                                                                 .createTeaPartyModifier(getGame().getTurn()));
                        removeGoodsElement.appendChild(goods.toXMLElement(serverPlayer, removeGoodsElement
                                                                          .getOwnerDocument()));
                        conn.send(removeGoodsElement);
                    } else {
                        // player has cheated and removed goods from colony, don't restore tax
                        monarchActionElement.setAttribute("force", String.valueOf(true));
                        conn.send(monarchActionElement);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not send message to: " + serverPlayer.getName(), e);
            }
            break;
        case LOWER_TAX:
            int lowerTax = monarch.lowerTax(random);
            monarchActionElement.setAttribute("amount", String.valueOf(lowerTax));
            try {
                serverPlayer.setTax(lowerTax); // to avoid cheating
                conn.send(monarchActionElement);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not send message to: " + serverPlayer.getName(), e);
            }
            break;
        case ADD_TO_REF:
            List<AbstractUnit> unitsToAdd = monarch.addToREF(random);
            monarch.addToREF(unitsToAdd);
            Element additionElement = monarchActionElement.getOwnerDocument().createElement("addition");
            for (AbstractUnit unit : unitsToAdd) {
                additionElement.appendChild(unit.toXMLElement(serverPlayer,additionElement.getOwnerDocument()));
            }
            monarchActionElement.appendChild(additionElement);
            try {
                conn.send(monarchActionElement);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not send message to: " + serverPlayer.getName(), e);
            }
            break;
        case DECLARE_WAR:
            Player enemy = monarch.declareWar(random);
            if (enemy == null) { // this should not happen
                logger.warning("Declared war on nobody.");
                return;
            }
            monarchActionElement.setAttribute("enemy", enemy.getId());
            changeStance(serverPlayer, Stance.WAR, enemy, true);
            // TODO glom onto monarch element.
            break;
            /** TODO: restore
                case Monarch.SUPPORT_LAND:
                int[] additions = monarch.supportLand();
                createUnits(additions, monarchActionElement, serverPlayer);
                try {
                conn.send(monarchActionElement);
                } catch (IOException e) {
                logger.warning("Could not send message to: " + serverPlayer.getName());
                }
                break;
                case Monarch.SUPPORT_SEA:
                // TODO: make this generic
                UnitType unitType = getGame().getSpecification().getUnitType("model.unit.frigate");
                newUnit = new Unit(getGame(), serverPlayer.getEurope(), serverPlayer, unitType, UnitState.ACTIVE);
                //serverPlayer.getEurope().add(newUnit);
                monarchActionElement.appendChild(newUnit.toXMLElement(serverPlayer, monarchActionElement
                .getOwnerDocument()));
                try {
                conn.send(monarchActionElement);
                } catch (IOException e) {
                logger.warning("Could not send message to: " + serverPlayer.getName());
                }
                break;
            */
        case OFFER_MERCENARIES:
            Element mercenaryElement = monarchActionElement.getOwnerDocument().createElement("mercenaries");
            List<AbstractUnit> units = monarch.getMercenaries(random);
            int price = monarch.getPrice(units, true);
            monarchActionElement.setAttribute("price", String.valueOf(price));
            for (AbstractUnit unit : units) {
                mercenaryElement.appendChild(unit.toXMLElement(monarchActionElement.getOwnerDocument()));
            }
            monarchActionElement.appendChild(mercenaryElement);
            try {
                Element reply = conn.ask(monarchActionElement);
                boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                if (accepted) {
                    Element updateElement = Message.createNewRootElement("monarchAction");
                    updateElement.setAttribute("action", String.valueOf(MonarchAction.ADD_UNITS));
                    serverPlayer.modifyGold(-price);
                    createUnits(units, updateElement, serverPlayer);
                    conn.send(updateElement);
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not send message to: " + serverPlayer.getName(), e);
            }
            break;
        case NO_ACTION:
            // nothing to do here. :-)
            break;
        }
    }

    /**
     * Create the Royal Expeditionary Force player corresponding to
     * a given player that is about to rebel.
     *
     * @param serverPlayer The <code>ServerPlayer</code> about to rebel.
     * @return The REF player.
     */
    public ServerPlayer createREFPlayer(ServerPlayer serverPlayer) {
        Nation refNation = serverPlayer.getNation().getRefNation();
        ServerPlayer refPlayer = getFreeColServer().addAIPlayer(refNation);
        refPlayer.setEntryLocation(serverPlayer.getEntryLocation());
        Player.makeContact(serverPlayer, refPlayer); // Will change, setup only
        createREFUnits(serverPlayer, refPlayer);
        return refPlayer;
    }
    
    public List<Unit> createREFUnits(ServerPlayer player, ServerPlayer refPlayer){
        EquipmentType muskets = getGame().getSpecification().getEquipmentType("model.equipment.muskets");
        EquipmentType horses = getGame().getSpecification().getEquipmentType("model.equipment.horses");
        
        List<Unit> unitsList = new ArrayList<Unit>();
        List<Unit> navalUnits = new ArrayList<Unit>();
        List<Unit> landUnits = new ArrayList<Unit>();
        
        // Create naval units
        for (AbstractUnit unit : player.getMonarch().getNavalUnits()) {
            for (int index = 0; index < unit.getNumber(); index++) {
                Unit newUnit = new Unit(getGame(), refPlayer.getEurope(), refPlayer,
                                        unit.getUnitType(getGame().getSpecification()),
                                        UnitState.TO_AMERICA);
                navalUnits.add(newUnit);
            }
        }
        unitsList.addAll(navalUnits);
        
        // Create land units
        for (AbstractUnit unit : player.getMonarch().getLandUnits()) {
            EquipmentType[] equipment = EquipmentType.NO_EQUIPMENT;
            switch(unit.getRole()) {
            case SOLDIER:
                equipment = new EquipmentType[] { muskets };
                break;
            case DRAGOON:
                equipment = new EquipmentType[] { horses, muskets };
                break;
            default:
            }
            for (int index = 0; index < unit.getNumber(); index++) {
                landUnits.add(new Unit(getGame(), refPlayer.getEurope(), refPlayer,
                                       unit.getUnitType(getGame().getSpecification()),
                                       UnitState.ACTIVE, equipment));
            }
        }
        unitsList.addAll(landUnits);
            
        // Board land units
        Iterator<Unit> carriers = navalUnits.iterator();
        for(Unit unit : landUnits){
            //cycle through the naval units to find a carrier for this unit
            
            // check if there is space for this unit
            boolean noSpaceForUnit=true;
            for(Unit carrier : navalUnits){
                if (unit.getSpaceTaken() <= carrier.getSpaceLeft()) {
                    noSpaceForUnit=false;
                    break;
                }
            }
            // There is no space for this unit, stays in Europe
            if(noSpaceForUnit){
                continue;
            }
            // Find carrier
            Unit carrier = null;
            while (carrier == null){
                // got to the end of the list, restart
                if (!carriers.hasNext()) {
                    carriers = navalUnits.iterator();
                }
                carrier = carriers.next();
                // this carrier cant carry this unit
                if (unit.getSpaceTaken() > carrier.getSpaceLeft()) {
                    carrier = null;
                }
            }
            // set unit aboard carrier
            unit.setLocation(carrier);
            //XXX: why only the units that can be aboard are sent to the player?
            //unitsList.add(unit);
        }
        return unitsList;
    }

    private void createUnits(List<AbstractUnit> units, Element element, ServerPlayer nextPlayer) {
        String musketsTypeStr = null;
        String horsesTypeStr = null;
        if(nextPlayer.isIndian()){
            musketsTypeStr = "model.equipment.indian.muskets";
            horsesTypeStr = "model.equipment.indian.horses";
        } else {
            musketsTypeStr = "model.equipment.muskets";
            horsesTypeStr = "model.equipment.horses";
        }

        final EquipmentType muskets = getGame().getSpecification().getEquipmentType(musketsTypeStr);
        final EquipmentType horses = getGame().getSpecification().getEquipmentType(horsesTypeStr);

        EquipmentType[] soldier = new EquipmentType[] { muskets };
        EquipmentType[] dragoon = new EquipmentType[] { horses, muskets };
        for (AbstractUnit unit : units) {
            EquipmentType[] equipment = EquipmentType.NO_EQUIPMENT;
            for (int count = 0; count < unit.getNumber(); count++) {
                switch(unit.getRole()) {
                case SOLDIER:
                    equipment = soldier;
                    break;
                case DRAGOON:
                    equipment = dragoon;
                    break;
                default:
                }
                Unit newUnit = new Unit(getGame(), nextPlayer.getEurope(), nextPlayer,
                                        unit.getUnitType(getGame().getSpecification()), UnitState.ACTIVE, equipment);
                //nextPlayer.getEurope().add(newUnit);
                if (element != null) {
                    element.appendChild(newUnit.toXMLElement(nextPlayer, element.getOwnerDocument()));
                }
            }
        }
    }

    /**
     * Change stance and collect changes that need updating.
     *
     * @param player The originating <code>Player</code>.
     * @param stance The new <code>Stance</code>.
     * @param otherPlayer The <code>Player</code> wrt which the stance changes.
     * @param symmetric If true, change the otherPlayer stance as well.
     * @param cs A <code>ChangeSet</code> containing the changes.
     * @return True if there was a change in stance at all.
     */
    private boolean csChangeStance(Player player, Stance stance,
                                   Player otherPlayer, boolean symmetric,
                                   ChangeSet cs) {
        boolean change = false;
        Stance old = player.getStance(otherPlayer);

        if (old != stance) {
            try {
                int modifier = old.getTensionModifier(stance);
                player.setStance(otherPlayer, stance);
                if (modifier != 0) {
                    cs.add(See.only(null).perhaps((ServerPlayer) otherPlayer),
                           player.modifyTension(otherPlayer, modifier));
                }
                cs.addStance(See.perhaps(), player, stance, otherPlayer);
                change = true;
                logger.finest("Stance change " + player.getName()
                              + " " + old.toString()
                              + " -> " + stance.toString()
                              + " wrt " + otherPlayer.getName());
            } catch (IllegalStateException e) { // Catch illegal transitions
                logger.log(Level.WARNING, "Illegal stance transition", e);
            }
        }
        if (symmetric && (old = otherPlayer.getStance(player)) != stance) {
            try {
                int modifier = old.getTensionModifier(stance);
                otherPlayer.setStance(player, stance);
                if (modifier != 0) {
                    cs.add(See.only(null).perhaps((ServerPlayer) player),
                           otherPlayer.modifyTension(player, modifier));
                }
                cs.addStance(See.perhaps(), otherPlayer, stance, player);
                change = true;
                logger.finest("Stance change " + otherPlayer.getName()
                              + " " + old.toString()
                              + " -> " + stance.toString()
                              + " wrt " + player.getName());
            } catch (IllegalStateException e) { // Catch illegal transitions
                logger.log(Level.WARNING, "Illegal stance transition", e);
            }
        }

        return change;
    }

    /**
     * Change stance and inform all but the originating player.
     *
     * @param player The originating <code>Player</code>.
     * @param stance The new <code>Stance</code>.
     * @param otherPlayer The <code>Player</code> wrt which the stance changes.
     * @param symmetric If true, change the otherPlayer stance as well.
     * @return A <code>ChangeSet</code> encapsulating the resulting changes.
     */
    public void changeStance(Player player, Stance stance,
                             Player otherPlayer, boolean symmetric) {
        ChangeSet cs = new ChangeSet();
        if (csChangeStance(player, stance, otherPlayer, symmetric, cs)) {
            sendToOthers((ServerPlayer) player, cs);
        }
    }

    /**
     * All player colonies bombard all available targets.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is bombarding.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csBombardEnemyShips(ServerPlayer serverPlayer, ChangeSet cs) {
        for (Colony colony : serverPlayer.getColonies()) {
            if (colony.canBombardEnemyShip()) {
                for (Tile tile : colony.getTile().getSurroundingTiles(1)) {
                    if (!tile.isLand()
                        && tile.getFirstUnit() != null
                        && (ServerPlayer)(tile.getFirstUnit().getOwner())
                            != serverPlayer) {
                        for (Unit unit : new ArrayList<Unit>(tile.getUnitList())) {
                            if (serverPlayer.atWarWith(unit.getOwner())
                                || unit.hasAbility("model.ability.piracy")) {
                                csCombat(serverPlayer, colony, unit, null, cs);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle a player retiring.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is retiring.
     * @return An element cleaning up the player.
     */
    public Element retire(ServerPlayer serverPlayer) {
        FreeColServer freeColServer = getFreeColServer();
        boolean highScore = freeColServer.newHighScore(serverPlayer);
        if (highScore) {
            try {
                freeColServer.saveHighScores();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to save high scores", e);
                highScore = false;
            }
        }

        // Clean up the player.
        ChangeSet cs = new ChangeSet();
        csKillPlayer(serverPlayer, cs);
        cs.addAttribute(See.only(serverPlayer), "highScore",
                        Boolean.toString(highScore));

        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Cash in a treasure train.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is cashing in.
     * @param unit The treasure train <code>Unit</code> to cash in.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element cashInTreasureTrain(ServerPlayer serverPlayer, Unit unit) {
        ChangeSet cs = new ChangeSet();

        // Work out the cash in amount.
        int fullAmount = unit.getTreasureAmount();
        int cashInAmount = (fullAmount - unit.getTransportFee())
            * (100 - serverPlayer.getTax()) / 100;
        serverPlayer.modifyGold(cashInAmount);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold", "score");

        // Generate a suitable message.
        String messageId = (serverPlayer.getPlayerType() == PlayerType.REBEL
                            || serverPlayer.getPlayerType() == PlayerType.INDEPENDENT)
            ? "model.unit.cashInTreasureTrain.independent"
            : "model.unit.cashInTreasureTrain.colonial";
        cs.addMessage(See.only(serverPlayer),
            new ModelMessage(messageId, serverPlayer, unit)
                .addAmount("%amount%", fullAmount)
                .addAmount("%cashInAmount%", cashInAmount));

        // Dispose of the unit.
        cs.addDispose(serverPlayer, unit.getLocation(), unit);

        // Others can not see cash-ins which happen in colonies or Europe.
        return cs.build(serverPlayer);
    }


    /**
     * Declare independence.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is naming.
     * @param nationName The new name for the independent nation.
     * @param countryName The new name for its residents.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element declareIndependence(ServerPlayer serverPlayer,
                                       String nationName, String countryName) {
        ChangeSet cs = new ChangeSet();

        // Cross the Rubicon
        StringTemplate oldNation = serverPlayer.getNationName();
        serverPlayer.setIndependentNationName(nationName);
        serverPlayer.setNewLandName(countryName);
        serverPlayer.setPlayerType(PlayerType.REBEL);
        serverPlayer.getFeatureContainer().addAbility(new Ability("model.ability.independenceDeclared"));
        serverPlayer.modifyScore(SCORE_INDEPENDENCE_DECLARED);

        // Do not add history event to cs as we are going to update the
        // entire player.  Likewise clear model messages.
        Turn turn = getGame().getTurn();
        serverPlayer.addHistory(new HistoryEvent(turn,
                HistoryEvent.EventType.DECLARE_INDEPENDENCE));
        serverPlayer.clearModelMessages();

        // Dispose of units in Europe.
        Europe europe = serverPlayer.getEurope();
        StringTemplate seized = StringTemplate.label(", ");
        for (Unit u : europe.getUnitList()) {
            seized.addStringTemplate(u.getLabel());
        }
        if (!seized.getReplacements().isEmpty()) {
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.UNIT_LOST,
                                 "model.player.independence.unitsSeized",
                                 serverPlayer)
                    .addStringTemplate("%units%", seized));
        }

        // Generalized continental army muster
        java.util.Map<UnitType, UnitType> upgrades
            = new HashMap<UnitType, UnitType>();
        Specification spec = getGame().getSpecification();
        for (UnitType unitType : spec.getUnitTypeList()) {
            UnitType upgrade = unitType.getUnitTypeChange(ChangeType.INDEPENDENCE,
                                                          serverPlayer);
            if (upgrade != null) {
                upgrades.put(unitType, upgrade);
            }
        }
        for (Colony colony : serverPlayer.getColonies()) {
            int sol = colony.getSoL();
            if (sol > 50) {
                java.util.Map<UnitType, List<Unit>> unitMap = new HashMap<UnitType, List<Unit>>();
                List<Unit> allUnits = new ArrayList<Unit>(colony.getTile().getUnitList());
                allUnits.addAll(colony.getUnitList());
                for (Unit unit : allUnits) {
                    if (upgrades.containsKey(unit.getType())) {
                        List<Unit> unitList = unitMap.get(unit.getType());
                        if (unitList == null) {
                            unitList = new ArrayList<Unit>();
                            unitMap.put(unit.getType(), unitList);
                        }
                        unitList.add(unit);
                    }
                }
                for (Entry<UnitType, List<Unit>> entry : unitMap.entrySet()) {
                    int limit = (entry.getValue().size() + 2) * (sol - 50) / 100;
                    if (limit > 0) {
                        for (int index = 0; index < limit; index++) {
                            Unit unit = entry.getValue().get(index);
                            if (unit == null) break;
                            unit.setType(upgrades.get(entry.getKey()));
                            cs.add(See.only(serverPlayer), unit);
                        }
                        cs.addMessage(See.only(serverPlayer),
                            new ModelMessage(ModelMessage.MessageType.UNIT_IMPROVED,
                                             "model.player.continentalArmyMuster",
                                             serverPlayer, colony)
                                .addName("%colony%", colony.getName())
                                .addAmount("%number%", limit)
                                .add("%oldUnit%", entry.getKey().getNameKey())
                                .add("%unit%", upgrades.get(entry.getKey()).getNameKey()));
                    }
                }
            }
        }

        // Create the REF.
        ServerPlayer refPlayer = createREFPlayer(serverPlayer);

        // Now the REF is ready, we can dispose of the European connection.
        cs.addDispose(serverPlayer, null, serverPlayer.getEurope());
        serverPlayer.setEurope(null);
        serverPlayer.setMonarch(null);

        // Pity to have to update such a heavy object as the player,
        // but we do this, at most, once per player.  Other players
        // only need a partial player update and the stance change.
        // Put the stance change after the name change so that the
        // other players see the new nation name declaring war.  The
        // REF is hardwired to declare war on rebels so there is no
        // need to adjust its stance or tension.
        cs.addPartial(See.all().except(serverPlayer), serverPlayer,
                      "playerType", "independentNationName", "newLandName");
        cs.addMessage(See.all().except(serverPlayer),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                             "declareIndependence.announce",
                             serverPlayer)
                .addStringTemplate("%oldNation%", oldNation)
                .addStringTemplate("%newNation%", serverPlayer.getNationName())
                .add("%ruler%", serverPlayer.getRulerNameKey()));
        cs.add(See.only(serverPlayer), serverPlayer);
        csChangeStance(serverPlayer, Stance.WAR, refPlayer, false, cs);

        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Give independence.  Note that the REF player is granting, but
     * most of the changes happen to the newly independent player.
     * hence the special handling.
     *
     * @param serverPlayer The REF <code>ServerPlayer</code> that is granting.
     * @param independent The newly independent <code>ServerPlayer</code>.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element giveIndependence(ServerPlayer serverPlayer,
                                    ServerPlayer independent) {
        ChangeSet cs = new ChangeSet();

        // The rebels have won.
        if (!csChangeStance(serverPlayer, Stance.PEACE, independent,
                            true, cs)) {
            return Message.clientError("Unable to make peace!?!");
        }
        independent.setPlayerType(PlayerType.INDEPENDENT);
        Turn turn = getGame().getTurn();
        independent.modifyScore(SCORE_INDEPENDENCE_GRANTED - turn.getNumber());
        independent.setTax(0);
        independent.reinitialiseMarket();
        independent.addHistory(new HistoryEvent(turn,
                HistoryEvent.EventType.INDEPENDENCE));
        cs.addMessage(See.only(independent),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                             "model.player.independence",
                             independent)
                .addStringTemplate("%ref%", serverPlayer.getNationName()));

        // Who surrenders?
        List<Unit> surrenderUnits = new ArrayList<Unit>();
        for (Unit u : serverPlayer.getUnits()) {
            if (!u.isNaval()) surrenderUnits.add(u);
        }
        if (surrenderUnits.size() > 0) {
            StringTemplate surrender = StringTemplate.label(", ");
            for (Unit u : surrenderUnits) {
                UnitType downgrade = u.getTypeChange(ChangeType.CAPTURE,
                                                     independent);
                if (downgrade != null) u.setType(downgrade);
                u.setOwner(independent);
                surrender.addStringTemplate(u.getLabel());
                // Make sure the former owner is notified!
                cs.add(See.perhaps().always(serverPlayer), u);
            }
            cs.addMessage(See.only(independent),
                new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                 "model.player.independence.unitsAcquired",
                                 independent)
                    .addStringTemplate("%units%", surrender));
        }

        // Update player type.  Again, a pity to have to do a whole
        // player update, but a partial update works for other players.
        cs.addPartial(See.all().except(independent), independent,
                      "playerType");
        cs.addMessage(See.all().except(independent),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                             "model.player.independence.announce",
                             independent)
                .addStringTemplate("%nation%", independent.getNationName())
                .addStringTemplate("%ref%", serverPlayer.getNationName()));
        cs.add(See.only(independent), independent);

        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }


    /**
     * Rename an object.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is naming.
     * @param object The <code>Nameable</code> to rename.
     * @param newName The new name.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element renameObject(ServerPlayer serverPlayer, Nameable object,
                                String newName) {
        ChangeSet cs = new ChangeSet();

        object.setName(newName);
        FreeColGameObject fcgo = (FreeColGameObject) object;
        cs.addPartial(See.all(), fcgo, "name");

        // Others may be able to see the name change.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }


    /**
     * Convenience wrapper class for the map that underlies
     * transaction sessions.
     */
    public static class TransactionSession {
        private static java.util.Map<String, java.util.Map<String,
            TransactionSession>> transactionSessions
            = new HashMap<String, java.util.Map<String, TransactionSession>>();

        private java.util.Map<String, Object> session;

        private TransactionSession() {
            session = new HashMap<String, Object>();
        }

        public Object get(String id) {
            return session.get(id);
        }

        public void put(String id, Object val) {
            session.put(id, val);
        }

        /**
         * Looks up the TransactionSession unique to the specified fcgos in
         * transactionSessions.
         *
         * @param o1 First <code>FreeColGameObject</code>.
         * @param o2 Second <code>FreeColGameObject</code>.
         * @return The transaction session, or null if not found.
         */
        public static TransactionSession lookup(FreeColGameObject o1,
                                                FreeColGameObject o2) {
            return TransactionSession.lookup(o1.getId(), o2.getId());
        }

        /**
         * Looks up the TransactionSession unique to the specified ids in
         * transactionSessions.
         *
         * @param id1 First id.
         * @param id2 Second id.
         * @return The transaction session, or null if not found.
         */
        public static TransactionSession lookup(String id1, String id2) {
            java.util.Map<String, TransactionSession> base
                = transactionSessions.get(id1);
            return (base == null) ? null : base.get(id2);
        }

        /**
         * Create and remember a new TransactionSession unique to the
         * specified ids.
         *
         * @param o1 First <code>FreeColGameObject</code>.
         * @param o2 Second <code>FreeColGameObject</code>.
         * @return The transaction session.
         */
        public static TransactionSession create(FreeColGameObject o1,
                                                FreeColGameObject o2) {
            java.util.Map<String, TransactionSession> base
                = transactionSessions.get(o1.getId());
            if (base == null) {
                base = new HashMap<String, TransactionSession>();
                transactionSessions.put(o1.getId(), base);
            } else {
                if (base.containsKey(o2.getId())) base.remove(o2.getId());
            }
            TransactionSession session = new TransactionSession();
            base.put(o2.getId(), session);
            return session;
        }

        /**
         * Find a TransactionSession unique to the specified ids, or
         * create and remember a new one.
         *
         * @param o1 First <code>FreeColGameObject</code>.
         * @param o2 Second <code>FreeColGameObject</code>.
         * @return The transaction session.
         */
        public static TransactionSession find(FreeColGameObject o1,
                                              FreeColGameObject o2) {
            java.util.Map<String, TransactionSession> base
                = transactionSessions.get(o1.getId());
            if (base == null) {
                base = new HashMap<String, TransactionSession>();
                transactionSessions.put(o1.getId(), base);
            }
            TransactionSession session = base.get(o2.getId());
            if (session == null) {
                session = new TransactionSession();
                base.put(o2.getId(), session);
            }
            return session;
        }

        /**
         * Remember a TransactionSession (save to transactionSessions).
         *
         * @param o1 First <code>FreeColGameObject</code>.
         * @param o2 Second <code>FreeColGameObject</code>.
         * @param session The <code>TransactionSession</code> to save.
         */
        public static void remember(FreeColGameObject o1,
                                    FreeColGameObject o2,
                                    TransactionSession session) {
            java.util.Map<String, TransactionSession>
                base = transactionSessions.get(o1.getId());
            if (base == null) {
                base = new HashMap<String, TransactionSession>();
                transactionSessions.put(o1.getId(), base);
            }
            base.put(o2.getId(), session);
        }

        /**
         * Forget a TransactionSession (remove from transactionSessions).
         *
         * @param o1 First <code>FreeColGameObject</code>.
         * @param o2 Second <code>FreeColGameObject</code>.
         */
        public static void forget(FreeColGameObject o1, FreeColGameObject o2) {
            TransactionSession.forget(o1.getId(), o2.getId());
        }

        /**
         * Forget a TransactionSession (remove from transactionSessions).
         *
         * @param id1 The first id.
         * @param id2 The second id.
         */
        public static void forget(String id1, String id2) {
            java.util.Map<String, TransactionSession>
                base = transactionSessions.get(id1);
            if (base != null) {
                base.remove(id2);
                if (base.isEmpty()) transactionSessions.remove(id1);
            }
        }
    }

    /**
     * Gets a transaction session with a settlement.
     * Either the current one if it exists or create a fresh one.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return A session describing the transaction.
     */
    public TransactionSession getTransactionSession(Unit unit, Settlement settlement) {
        TransactionSession session = TransactionSession.lookup(unit,settlement);
        if (session != null) return session;

        // Session does not exist, create, store, and return it.
        session = TransactionSession.create(unit, settlement);
        // Default values
        session.put("actionTaken", false);
        session.put("unitMoves", unit.getMovesLeft());
        session.put("canGift", true);
        if (settlement.getOwner().atWarWith(unit.getOwner())) {
            session.put("canSell", false);
            session.put("canBuy", false);
        } else {
            session.put("canBuy", true);
            // The unit took nothing to sell, so nothing should be in
            // this session.
            session.put("canSell", unit.getSpaceTaken() != 0);
        }

        // Only keep track of human player sessions.
        if (!unit.getOwner().isAI()) {
            TransactionSession.remember(unit, settlement, session);
        }
        return session;
    }

    /**
     * Close a settlement transaction session.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     */
    public void closeTransactionSession(Unit unit, Settlement settlement) {
        // Only keep track of human player sessions.
        if (!unit.getOwner().isAI()) {
            TransactionSession.forget(unit, settlement);
        }
    }
    
    /**
     * Query whether a transaction session exists.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return True if a session is already open.
     */
    public boolean isTransactionSessionOpen(Unit unit, Settlement settlement) {
        // AI does not need to send a message to open a session
        if (unit.getOwner().isAI()) return true;

        return TransactionSession.lookup(unit, settlement) != null;
    }

    /**
     * Gets a settlement transaction session, either existing or
     * newly opened.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is trading.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element getTransaction(ServerPlayer serverPlayer, Unit unit,
                                  Settlement settlement) {
        ChangeSet cs = new ChangeSet();
        TransactionSession session;

        if (isTransactionSessionOpen(unit, settlement)) {
            session = getTransactionSession(unit, settlement);
        } else {
            if (unit.getMovesLeft() <= 0) {
                return Message.clientError("Unit " + unit.getId()
                                           + " has no moves left.");
            }
            session = getTransactionSession(unit, settlement);
            // Sets unit moves to zero to avoid cheating.  If no
            // action is taken, the moves will be restored when
            // closing the session
            unit.setMovesLeft(0);
            cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
        }

        // Add just the attributes the client needs.
        cs.addAttribute(See.only(serverPlayer), "canBuy",
                        ((Boolean) session.get("canBuy")).toString());
        cs.addAttribute(See.only(serverPlayer), "canSell",
                        ((Boolean) session.get("canSell")).toString());
        cs.addAttribute(See.only(serverPlayer), "canGift",
                        ((Boolean) session.get("canGift")).toString());

        // Others can not see transactions.
        return cs.build(serverPlayer);
    }

    /**
     * Close a transaction.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is trading.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element closeTransaction(ServerPlayer serverPlayer, Unit unit,
                                    Settlement settlement) {
        if (!isTransactionSessionOpen(unit, settlement)) {
            return Message.clientError("No such transaction session.");
        }

        ChangeSet cs = new ChangeSet();
        TransactionSession session = getTransactionSession(unit, settlement);

        // Restore unit movement if no action taken.
        Boolean actionTaken = (Boolean) session.get("actionTaken");
        if (!actionTaken) {
            Integer unitMoves = (Integer) session.get("unitMoves");
            unit.setMovesLeft(unitMoves);
            cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
        }
        closeTransactionSession(unit, settlement);

        // Others can not see end of transaction.
        return cs.build(serverPlayer);
    }


    /**
     * Get the goods for sale in a settlement.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return An list of <code>Goods</code> for sale at the settlement.
     */
    public List<Goods> getGoodsForSale(Unit unit, Settlement settlement)
        throws IllegalStateException {
        List<Goods> sellGoods = null;

        if (settlement instanceof IndianSettlement) {
            IndianSettlement indianSettlement = (IndianSettlement) settlement;
            sellGoods = indianSettlement.getSellGoods();
            if (!sellGoods.isEmpty()) {
                AIPlayer aiPlayer = (AIPlayer) getFreeColServer().getAIMain()
                    .getAIObject(indianSettlement.getOwner());
                for (Goods goods : sellGoods) {
                    aiPlayer.registerSellGoods(goods);
                }
            }
        } else { // Colony might be supported one day?
            throw new IllegalStateException("Bogus settlement");
        }
        return sellGoods;
    }


    /**
     * Price some goods for sale from a settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is buying.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to buy.
     * @param price The buyers proposed price for the goods.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element buyProposition(ServerPlayer serverPlayer,
                                  Unit unit, Settlement settlement,
                                  Goods goods, int price) {
        if (!isTransactionSessionOpen(unit, settlement)) {
            return Message.clientError("Proposing to buy without opening a transaction session?!");
        }
        TransactionSession session = getTransactionSession(unit, settlement);
        if (!(Boolean) session.get("canBuy")) {
            return Message.clientError("Proposing to buy in a session where buying is not allowed.");
        }

        ChangeSet cs = new ChangeSet();

        // AI considers the proposition, return with a gold value
        AIPlayer ai = (AIPlayer) getFreeColServer().getAIMain()
            .getAIObject(settlement.getOwner());
        int gold = ai.buyProposition(unit, settlement, goods, price);

        // Others can not see proposals.
        cs.addAttribute(See.only(serverPlayer), "gold", Integer.toString(gold));
        return cs.build(serverPlayer);
    }

    /**
     * Price some goods for sale to a settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is buying.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to sell.
     * @param price The sellers proposed price for the goods.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element sellProposition(ServerPlayer serverPlayer,
                                   Unit unit, Settlement settlement,
                                   Goods goods, int price) {
        if (!isTransactionSessionOpen(unit, settlement)) {
            return Message.clientError("Proposing to sell without opening a transaction session");
        }
        TransactionSession session = getTransactionSession(unit, settlement);
        if (!(Boolean) session.get("canSell")) {
            return Message.clientError("Proposing to sell in a session where selling is not allowed.");
        }

        ChangeSet cs = new ChangeSet();

        // AI considers the proposition, return with a gold value
        AIPlayer ai = (AIPlayer) getFreeColServer().getAIMain()
            .getAIObject(settlement.getOwner());
        int gold = ai.sellProposition(unit, settlement, goods, price);

        // Others can not see proposals.
        cs.addAttribute(See.only(serverPlayer), "gold", Integer.toString(gold));

        return cs.build(serverPlayer);
    }


    /**
     * Propagate an European market change to the other European markets.
     *
     * @param type The type of goods that was traded.
     * @param amount The amount of goods that was traded.
     * @param serverPlayer The player that performed the trade.
     */
    private void propagateToEuropeanMarkets(GoodsType type, int amount,
                                            ServerPlayer serverPlayer) {
        // Propagate 5-30% of the original change.
        final int lowerBound = 5; // TODO: make into game option?
        final int upperBound = 30;// TODO: make into game option?
        amount *= random.nextInt(upperBound - lowerBound + 1) + lowerBound;
        amount /= 100;
        if (amount == 0) return;

        // Do not need to update the clients here, these changes happen
        // while it is not their turn, and they will get a fresh copy
        // of the altered market in the update sent in nextPlayer above.
        Market market;
        for (ServerPlayer other : getOtherPlayers(serverPlayer)) {
            if (other.isEuropean() && (market = other.getMarket()) != null) {
                market.addGoodsToMarket(type, amount);
            }
        }
    }

    /**
     * Buy goods in Europe.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is buying.
     * @param unit The <code>Unit</code> to carry the goods.
     * @param type The <code>GoodsType</code> to buy.
     * @param amount The amount of goods to buy.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element buyGoods(ServerPlayer serverPlayer, Unit unit,
                            GoodsType type, int amount) {
        ChangeSet cs = new ChangeSet();
        Market market = serverPlayer.getMarket();

        // FIXME: market.buy() should be here in the controller, but
        // there are two cases remaining that are hard to move still.
        //
        // 1. There is a shortcut buying of equipment in Europe in
        // Unit.equipWith().
        // 2. Also for the goods required for a building in
        // Colony.payForBuilding().  This breaks the pattern implemented
        // here as there is no unit involved.
        market.buy(type, amount, serverPlayer);
        unit.getGoodsContainer().addGoods(type, amount);
        cs.add(See.only(serverPlayer), unit);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        if (market.hasPriceChanged(type)) {
            // This type of goods has changed price, so we will update
            // the market and send a message as well.
            cs.addMessage(See.only(serverPlayer),
                          market.makePriceChangeMessage(type));
            market.flushPriceChange(type);
            cs.add(See.only(serverPlayer), market.getMarketData(type));
        }
        propagateToEuropeanMarkets(type, amount, serverPlayer);

        // Action occurs in Europe, nothing is visible to other players.
        return cs.build(serverPlayer);
    }

    /**
     * Sell goods in Europe.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is selling.
     * @param unit The <code>Unit</code> carrying the goods.
     * @param type The <code>GoodsType</code> to sell.
     * @param amount The amount of goods to sell.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element sellGoods(ServerPlayer serverPlayer, Unit unit,
                             GoodsType type, int amount) {
        ChangeSet cs = new ChangeSet();
        Market market = serverPlayer.getMarket();

        // FIXME: market.sell() should be in the controller, but the
        // following cases will have to wait.
        //
        // 1. Unit.dumpEquipment() gets called from a few places.
        // 2. Colony.exportGoods() is in the newTurn mess.
        // Its also still in MarketTest, which needs to be moved to
        // ServerPlayerTest where it also is already.
        //
        // Try to sell.
        market.sell(type, amount, serverPlayer);
        unit.getGoodsContainer().addGoods(type, -amount);
        cs.add(See.only(serverPlayer), unit);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        if (market.hasPriceChanged(type)) {
            // This type of goods has changed price, so update the
            // market and send a message as well.
            cs.addMessage(See.only(serverPlayer),
                          market.makePriceChangeMessage(type));
            market.flushPriceChange(type);
            cs.add(See.only(serverPlayer), market.getMarketData(type));
        }
        propagateToEuropeanMarkets(type, amount, serverPlayer);

        // Action occurs in Europe, nothing is visible to other players.
        return cs.build(serverPlayer);
    }


    /**
     * A unit migrates from Europe.
     *
     * @param serverPlayer The <code>ServerPlayer</code> whose unit it will be.
     * @param slot The slot within <code>Europe</code> to select the unit from.
     * @param type The type of migration occurring.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element emigrate(ServerPlayer serverPlayer, int slot,
                            MigrationType type) {
        ChangeSet cs = new ChangeSet();

        // Valid slots are in [1,3], recruitable indices are in [0,2].
        // An invalid slot is normal when the player has no control over
        // recruit type.
        boolean selected = 1 <= slot && slot <= Europe.RECRUIT_COUNT;
        int index = (selected) ? slot-1 : random.nextInt(Europe.RECRUIT_COUNT);

        // Create the recruit, move it to the docks.
        Europe europe = serverPlayer.getEurope();
        UnitType recruitType = europe.getRecruitable(index);
        Game game = getGame();
        Unit unit = new Unit(game, europe, serverPlayer, recruitType,
                             UnitState.ACTIVE,
                             recruitType.getDefaultEquipment());
        unit.setLocation(europe);

        // Handle migration type specific changes.
        switch (type) {
        case FOUNTAIN:
            serverPlayer.setRemainingEmigrants(serverPlayer.getRemainingEmigrants() - 1);
            break;
        case RECRUIT:
            serverPlayer.modifyGold(-europe.getRecruitPrice());
            europe.increaseRecruitmentDifficulty();
            // Fall through
        case NORMAL:
            serverPlayer.updateImmigrationRequired();
            serverPlayer.reduceImmigration();
            cs.addPartial(See.only(serverPlayer), serverPlayer,
                          "immigration", "immigrationRequired");
            break;
        default:
            throw new IllegalArgumentException("Bogus migration type");
        }

        // Replace the recruit we used.
        europe.setRecruitable(index, serverPlayer.generateRecruitable());
        cs.add(See.only(serverPlayer), europe);

        // Return an informative message only if this was an ordinary
        // migration where we did not select the unit type.
        // Other cases were selected.
        if (!selected) {
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.UNIT_ADDED,
                                 "model.europe.emigrate",
                                 serverPlayer, unit)
                    .add("%europe%", europe.getNameKey())
                    .addStringTemplate("%unit%", unit.getLabel()));
        }

        // Do not update others, emigration is private.
        return cs.build(serverPlayer);
    }


    /**
     * If a unit moves, check if an opposing naval unit slows it down.
     * Note that the unit moves are reduced here.
     *
     * @param unit The <code>Unit</code> that is moving.
     * @param newTile The <code>Tile</code> the unit is moving to.
     * @return Either an enemy unit that causes a slowdown, or null if none.
     */
    private Unit getSlowedBy(Unit unit, Tile newTile) {
        Player player = unit.getOwner();
        Game game = unit.getGame();
        CombatModel combatModel = game.getCombatModel();
        boolean pirate = unit.hasAbility("model.ability.piracy");
        Unit attacker = null;
        float attackPower = 0, totalAttackPower = 0;

        if (!unit.isNaval() || unit.getMovesLeft() <= 0) return null;
        for (Tile tile : newTile.getSurroundingTiles(1)) {
            // Ships in settlements do not slow enemy ships, but:
            // TODO should a fortress slow a ship?
            Player enemy;
            if (tile.isLand()
                || tile.getColony() != null
                || tile.getFirstUnit() == null
                || (enemy = tile.getFirstUnit().getOwner()) == player) continue;
            for (Unit enemyUnit : tile.getUnitList()) {
                if ((pirate || enemyUnit.hasAbility("model.ability.piracy")
                     || (enemyUnit.isOffensiveUnit() && player.atWarWith(enemy)))
                    && enemyUnit.isNaval()
                    && combatModel.getOffencePower(enemyUnit, unit) > attackPower) {
                    attackPower = combatModel.getOffencePower(enemyUnit, unit);
                    totalAttackPower += attackPower;
                    attacker = enemyUnit;
                }
            }
        }
        if (attacker != null) {
            float defencePower = combatModel.getDefencePower(attacker, unit);
            float totalProbability = totalAttackPower + defencePower;
            if (random.nextInt(Math.round(totalProbability) + 1)
                < totalAttackPower) {
                int diff = Math.max(0, Math.round(totalAttackPower - defencePower));
                int moves = Math.min(9, 3 + diff / 3);
                unit.setMovesLeft(unit.getMovesLeft() - moves);
                logger.info(unit.getId()
                            + " slowed by " + attacker.getId()
                            + " by " + Integer.toString(moves) + " moves.");
            } else {
                attacker = null;
            }
        }
        return attacker;
    }

    /**
     * Returns a type of Lost City Rumour. The type of rumour depends on the
     * exploring unit, as well as player settings.
     *
     * @param lostCity The <code>LostCityRumour</code> to investigate.
     * @param unit The <code>Unit</code> exploring the lost city rumour.
     * @param difficulty The difficulty level.
     * @return The type of rumour.
     * TODO: Move all the magic numbers in here to the specification.
     *       Also change the logic so that the special events appear a
     *       fixed number of times throughout the game, according to
     *       the specification.  Names for the cities of gold is also
     *       on the wishlist.
     */
    private RumourType getLostCityRumourType(LostCityRumour lostCity,
                                             Unit unit, int difficulty) {
        Tile tile = unit.getTile();
        Player player = unit.getOwner();
        RumourType rumour = lostCity.getType();
        if (rumour != null) {
            // Filter out failing cases that could only occur if the
            // type was explicitly set in debug mode.
            switch (rumour) {
            case BURIAL_GROUND:
                if (tile.getOwner() == null || !tile.getOwner().isIndian()) {
                    rumour = RumourType.NOTHING;
                }
                break;
            case LEARN:
                if (unit.getType().getUnitTypesLearntInLostCity().isEmpty()) {
                    rumour = RumourType.NOTHING;
                }
                break;
            default:
                break;
            }
            return rumour;
        }

        // The following arrays contain percentage values for
        // "good" and "bad" events when scouting with a non-expert
        // at the various difficulty levels [0..4] exact values
        // but generally "bad" should increase, "good" decrease
        final int BAD_EVENT_PERCENTAGE[]  = { 11, 17, 23, 30, 37 };
        final int GOOD_EVENT_PERCENTAGE[] = { 75, 62, 48, 33, 17 };
        // remaining to 100, event NOTHING:   14, 21, 29, 37, 46

        // The following arrays contain the modifiers applied when
        // expert scout is at work exact values; modifiers may
        // look slightly "better" on harder levels since we're
        // starting from a "worse" percentage.
        final int BAD_EVENT_MOD[]  = { -6, -7, -7, -8, -9 };
        final int GOOD_EVENT_MOD[] = { 14, 15, 16, 18, 20 };

        // The scouting outcome is based on three factors: level,
        // expert scout or not, DeSoto or not.  Based on this, we
        // are going to calculate probabilites for neutral, bad
        // and good events.
        boolean isExpertScout = unit.hasAbility("model.ability.expertScout")
            && unit.hasAbility("model.ability.scoutIndianSettlement");
        boolean hasDeSoto = player.hasAbility("model.ability.rumoursAlwaysPositive");
        int percentNeutral;
        int percentBad;
        int percentGood;
        if (hasDeSoto) {
            percentBad  = 0;
            percentGood = 100;
            percentNeutral = 0;
        } else {
            // First, get "basic" percentages
            percentBad  = BAD_EVENT_PERCENTAGE[difficulty];
            percentGood = GOOD_EVENT_PERCENTAGE[difficulty];

            // Second, apply ExpertScout bonus if necessary
            if (isExpertScout) {
                percentBad  += BAD_EVENT_MOD[difficulty];
                percentGood += GOOD_EVENT_MOD[difficulty];
            }

            // Third, get a value for the "neutral" percentage,
            // unless the other values exceed 100 already
            if (percentBad + percentGood < 100) {
                percentNeutral = 100 - percentBad - percentGood;
            } else {
                percentNeutral = 0;
            }
        }

        // Now, the individual events; each section should add up to 100
        // The NEUTRAL
        int eventNothing = 100;

        // The BAD
        int eventVanish = 100;
        int eventBurialGround = 0;
        // If the tile not is European-owned, allow burial grounds rumour.
        if (tile.getOwner() != null && tile.getOwner().isIndian()) {
            eventVanish = 75;
            eventBurialGround = 25;
        }

        // The GOOD
        int eventLearn    = 30;
        int eventTrinkets = 30;
        int eventColonist = 20;
        // or, if the unit can't learn
        if (unit.getType().getUnitTypesLearntInLostCity().isEmpty()) {
            eventLearn    =  0;
            eventTrinkets = 50;
            eventColonist = 30;
        }

        // The SPECIAL
        // Right now, these are considered "good" events that happen randomly.
        int eventRuins    = 9;
        int eventCibola   = 6;
        int eventFountain = 5;

        // Finally, apply the Good/Bad/Neutral modifiers from
        // above, so that we end up with a ton of values, some of
        // them zero, the sum of which should be 10000.
        eventNothing      *= percentNeutral;
        eventVanish       *= percentBad;
        eventBurialGround *= percentBad;
        eventLearn        *= percentGood;
        eventTrinkets     *= percentGood;
        eventColonist     *= percentGood;
        eventRuins        *= percentGood;
        eventCibola       *= percentGood;
        eventFountain     *= percentGood;

        // Add all possible events to a RandomChoice List
        List<RandomChoice<RumourType>> choices = new ArrayList<RandomChoice<RumourType>>();
        if (eventNothing > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.NOTHING, eventNothing));
        }
        if (eventVanish > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.EXPEDITION_VANISHES, eventVanish));
        }
        if (eventBurialGround > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.BURIAL_GROUND, eventBurialGround));
        }
        if (eventLearn > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.LEARN, eventLearn));
        }
        if (eventTrinkets > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.TRIBAL_CHIEF, eventTrinkets));
        }
        if (eventColonist > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.COLONIST, eventColonist));
        }
        if (eventRuins > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.RUINS, eventRuins));
        }
        if (eventCibola > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.CIBOLA, eventCibola));
        }
        if (eventFountain > 0) {
            choices.add(new RandomChoice<RumourType>(RumourType.FOUNTAIN_OF_YOUTH, eventFountain));
        }
        return RandomChoice.getWeightedRandom(random, choices);
    }

    /**
     * Explore a lost city.
     *
     * @param unit The <code>Unit</code> that is exploring.
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param cs A <code>ChangeSet</code> to add changes to.
     */
    private void exploreLostCityRumour(ServerPlayer serverPlayer, Unit unit,
                                       ChangeSet cs) {
        Tile tile = unit.getTile();
        LostCityRumour lostCity = tile.getLostCityRumour();
        if (lostCity == null) return;

        Specification specification = getGame().getSpecification();
        int difficulty = specification.getRangeOption("model.option.difficulty").getValue();
        int dx = 10 - difficulty;
        Game game = unit.getGame();
        UnitType unitType;
        Unit newUnit = null;
        List<UnitType> treasureUnitTypes = null;

        switch (getLostCityRumourType(lostCity, unit, difficulty)) {
        case BURIAL_GROUND:
            Player indianPlayer = tile.getOwner();
            cs.add(See.only(serverPlayer),
                   indianPlayer.modifyTension(serverPlayer, Tension.Level.HATEFUL.getLimit()));
            cs.add(See.only(serverPlayer), indianPlayer);
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 "lostCityRumour.BurialGround",
                                 serverPlayer, unit)
                    .addStringTemplate("%nation%", indianPlayer.getNationName()));
            break;
        case EXPEDITION_VANISHES:
            cs.addDispose(serverPlayer, tile, unit);
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 "lostCityRumour.ExpeditionVanishes",
                                 serverPlayer));
            break;
        case NOTHING:
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 "lostCityRumour.Nothing",
                                 serverPlayer, unit));
            break;
        case LEARN:
            List<UnitType> learntUnitTypes = unit.getType().getUnitTypesLearntInLostCity();
            StringTemplate oldName = unit.getLabel();
            unit.setType(learntUnitTypes.get(random.nextInt(learntUnitTypes.size())));
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 "lostCityRumour.Learn",
                                 serverPlayer, unit)
                    .addStringTemplate("%unit%", oldName)
                    .add("%type%", unit.getType().getNameKey()));
            break;
        case TRIBAL_CHIEF:
            int chiefAmount = random.nextInt(dx * 10) + dx * 5;
            serverPlayer.modifyGold(chiefAmount);
            cs.addPartial(See.only(serverPlayer), serverPlayer,
                          "gold", "score");
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 "lostCityRumour.TribalChief",
                                 serverPlayer, unit)
                    .addAmount("%money%", chiefAmount));
            break;
        case COLONIST:
            List<UnitType> newUnitTypes = specification.getUnitTypesWithAbility("model.ability.foundInLostCity");
            newUnit = new Unit(game, tile, serverPlayer,
                               newUnitTypes.get(random.nextInt(newUnitTypes.size())),
                               UnitState.ACTIVE);
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                 "lostCityRumour.Colonist",
                                 serverPlayer, newUnit));
            break;
        case CIBOLA:
            String cityName = game.getCityOfCibola();
            if (cityName != null) {
                int treasureAmount = random.nextInt(dx * 600) + dx * 300;
                if (treasureUnitTypes == null) {
                    treasureUnitTypes = specification.getUnitTypesWithAbility("model.ability.carryTreasure");
                }
                unitType = treasureUnitTypes.get(random.nextInt(treasureUnitTypes.size()));
                newUnit = new Unit(game, tile, serverPlayer, unitType, UnitState.ACTIVE);
                newUnit.setTreasureAmount(treasureAmount);
                cs.addMessage(See.only(serverPlayer),
                    new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                     "lostCityRumour.Cibola",
                                     serverPlayer, newUnit)
                        .add("%city%", cityName)
                        .addAmount("%money%", treasureAmount));
                cs.addHistory(serverPlayer,
                    new HistoryEvent(game.getTurn(),
                                     HistoryEvent.EventType.CITY_OF_GOLD)
                        .add("%city%", cityName)
                        .addAmount("%treasure%", treasureAmount));
                break;
            }
            // Fall through, found all the cities of gold.
        case RUINS:
            int ruinsAmount = random.nextInt(dx * 2) * 300 + 50;
            if (ruinsAmount < 500) { // TODO remove magic number
                serverPlayer.modifyGold(ruinsAmount);
                cs.addPartial(See.only(serverPlayer), serverPlayer,
                              "gold", "score");
            } else {
                if (treasureUnitTypes == null) {
                    treasureUnitTypes = specification.getUnitTypesWithAbility("model.ability.carryTreasure");
                }
                unitType = treasureUnitTypes.get(random.nextInt(treasureUnitTypes.size()));
                newUnit = new Unit(game, tile, serverPlayer, unitType, UnitState.ACTIVE);
                newUnit.setTreasureAmount(ruinsAmount);
            }
            cs.addMessage(See.only(serverPlayer),
                 new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                  "lostCityRumour.Ruins",
                                  serverPlayer, ((newUnit != null) ? newUnit : unit))
                     .addAmount("%money%", ruinsAmount));
            break;
        case FOUNTAIN_OF_YOUTH:
            Europe europe = serverPlayer.getEurope();
            if (europe == null) {
                cs.addMessage(See.only(serverPlayer),
                     new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                      "lostCityRumour.FountainOfYouthWithoutEurope",
                                      serverPlayer, unit));
            } else {
                if (serverPlayer.hasAbility("model.ability.selectRecruit")
                    && !serverPlayer.isAI()) { // TODO: let the AI select
                    // Remember, and ask player to select
                    serverPlayer.setRemainingEmigrants(dx);
                    cs.addAttribute(See.only(serverPlayer),
                                    "fountainOfYouth", Integer.toString(dx));
                } else {
                    List<RandomChoice<UnitType>> recruitables
                        = serverPlayer.generateRecruitablesList();
                    for (int k = 0; k < dx; k++) {
                        new Unit(game, europe, serverPlayer,
                                 RandomChoice.getWeightedRandom(random, recruitables),
                                 UnitState.ACTIVE);
                    }
                    cs.add(See.only(serverPlayer), europe);
                }
                cs.addMessage(See.only(serverPlayer),
                     new ModelMessage(ModelMessage.MessageType.LOST_CITY_RUMOUR,
                                      "lostCityRumour.FountainOfYouth",
                                      serverPlayer, unit));
            }
            break;
        case NO_SUCH_RUMOUR:
        default:
            throw new IllegalStateException("No such rumour.");
        }
        tile.removeLostCityRumour();
    }

    /**
     * Check for a special contact panel for a nation.  If not found,
     * check for a more general one if allowed.
     *
     * @param player A European <code>Player</code> making contact.
     * @param other The <code>Player</code> nation to being contacted.
     * @return An <code>EventPanel</code> key, or null if none appropriate.
     */
    private String getContactKey(Player player, Player other) {
        String key = "EventPanel.MEETING_"
            + Messages.message(other.getNationName()).toUpperCase();
        if (!Messages.containsKey(key)) {
            if (other.isEuropean()) {
                key = (player.hasContactedEuropeans()) ? null
                    : "EventPanel.MEETING_EUROPEANS";
            } else {
                key = (player.hasContactedIndians()) ? null
                    : "EventPanel.MEETING_NATIVES";
            }
        }
        return key;
    }

    /**
     * Move a unit.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is moving.
     * @param unit The <code>Unit</code> to move.
     * @param newTile The <code>Tile</code> to move to.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element move(ServerPlayer serverPlayer, Unit unit, Tile newTile) {
        ChangeSet cs = new ChangeSet();
        csMove(serverPlayer, unit, newTile, cs);
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Move a unit.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is moving.
     * @param unit The <code>Unit</code> to move.
     * @param newTile The <code>Tile</code> to move to.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csMove(ServerPlayer serverPlayer, Unit unit, Tile newTile,
                        ChangeSet cs) {
        Game game = getGame();
        Turn turn = game.getTurn();

        // Plan to update tiles that could not be seen before but will
        // now be within the line-of-sight.
        List<FreeColGameObject> newTiles = new ArrayList<FreeColGameObject>();
        int los = unit.getLineOfSight();
        for (Tile tile : newTile.getSurroundingTiles(los)) {
            if (!serverPlayer.canSee(tile)) newTiles.add(tile);
        }

        // Update unit state.
        Location oldLocation = unit.getLocation();
        unit.setState(UnitState.ACTIVE);
        unit.setStateToAllChildren(UnitState.SENTRY);
        if (oldLocation instanceof Unit) {
            unit.setMovesLeft(0); // Disembark always consumes all moves.
        } else {
            if (unit.getMoveCost(newTile) <= 0) {
                logger.warning("Move of unit: " + unit.getId()
                               + " from: " + oldLocation.getTile().getId()
                               + " to: " + newTile.getId()
                               + " has bogus cost: " + unit.getMoveCost(newTile));
                unit.setMovesLeft(0);
            }
            unit.setMovesLeft(unit.getMovesLeft() - unit.getMoveCost(newTile));
        }


        // Do the move and explore a rumour if needed.
        unit.setLocation(newTile);
        if (newTile.hasLostCityRumour() && serverPlayer.isEuropean()) {
            exploreLostCityRumour(serverPlayer, unit, cs);
        }

        // Always update old location and new tile (making sure the
        // move is always visible even if the unit dies), and always
        // add the animation.  However dead units make no discoveries.
        cs.addMove(See.perhaps().always(serverPlayer), unit,
                   oldLocation, newTile);
        cs.add(See.perhaps().always(serverPlayer),
               (FreeColGameObject) oldLocation);
        cs.add(See.perhaps().always(serverPlayer), newTile);
        if (!unit.isDisposed()) {
            cs.add(See.only(serverPlayer), newTiles);
        }

        if (!unit.isDisposed()) {
            if (newTile.isLand()) {
                // Check for first landing
                if (serverPlayer.isEuropean()
                    && !serverPlayer.isNewLandNamed()) {
                    String newLand = Messages.getNewLandName(serverPlayer);
                    if (serverPlayer.isAI()) {
                        // TODO: Not convinced shortcutting the AI like
                        // this is a good idea, this really should be in
                        // the AI code.
                        serverPlayer.setNewLandName(newLand);
                    } else { // Ask player to name the land.
                        cs.addAttribute(See.only(serverPlayer),
                                        "nameNewLand", newLand);
                    }
                }

                // Check for new contacts.
                ServerPlayer welcomer = null;
                for (Tile t : newTile.getSurroundingTiles(1, 1)) {
                    if (t == null || !t.isLand()) {
                        continue; // Invalid tile for contact
                    }

                    ServerPlayer other = null;
                    Settlement settlement = t.getSettlement();
                    if (settlement != null) {
                        other = (ServerPlayer) t.getSettlement().getOwner();
                    } else if (t.getFirstUnit() != null) {
                        other = (ServerPlayer) t.getFirstUnit().getOwner();
                    }
                    if (other == null || other == serverPlayer) {
                        continue; // No contact
                    }

                    // Activate sentries
                    for (Unit u : t.getUnitList()) {
                        if (u.getState() == UnitState.SENTRY) {
                            u.setState(UnitState.ACTIVE);
                            cs.add(See.only(serverPlayer), u);
                        }
                    }

                    // Ignore previously contacted nations.
                    if (serverPlayer.hasContacted(other)) continue;

                    // Must be a first contact!
                    if (serverPlayer.isIndian()) {
                        // Ignore native-to-native contacts.
                        if (!other.isIndian()) {
                            String key = getContactKey(other, serverPlayer);
                            if (key != null) {
                                cs.addMessage(See.only(other),
                                    new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                                     key, other, serverPlayer));
                            }
                            cs.addHistory(other,
                                new HistoryEvent(turn,
                                    HistoryEvent.EventType.MEET_NATION)
                                    .addStringTemplate("%nation%", serverPlayer.getNationName()));
                        }
                    } else { // (serverPlayer.isEuropean)
                        // Initialize alarm for native settlements.
                        if (other.isIndian() && settlement != null) {
                            IndianSettlement is = (IndianSettlement) settlement;
                            if (!is.hasContactedSettlement(serverPlayer)) {
                                is.makeContactSettlement(serverPlayer);
                                cs.add(See.only(serverPlayer), is);
                            }
                        }

                        // Add first contact messages.
                        String key = getContactKey(serverPlayer, other);
                        if (key != null) {
                            cs.addMessage(See.only(serverPlayer),
                                new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                                 key, serverPlayer, other));
                        }

                        // History event for European players.
                        cs.addHistory(serverPlayer,
                            new HistoryEvent(turn,
                                HistoryEvent.EventType.MEET_NATION)
                                .addStringTemplate("%nation%", other.getNationName()));
                        // Extra special meeting on first landing!
                        if (other.isIndian()
                            && !serverPlayer.isNewLandNamed()
                            && (welcomer == null || newTile.getOwner() == other)) {
                            welcomer = other;
                        }
                    }

                    // Now make the contact properly.
                    csChangeStance(serverPlayer, Stance.PEACE, other, true, cs);
                    serverPlayer.setTension(other,
                                            new Tension(Tension.TENSION_MIN));
                    other.setTension(serverPlayer,
                                     new Tension(Tension.TENSION_MIN));
                }
                if (welcomer != null) {
                    cs.addAttribute(See.only(serverPlayer), "welcome",
                                    welcomer.getId());
                    cs.addAttribute(See.only(serverPlayer), "camps",
                        Integer.toString(welcomer.getNumberOfSettlements()));
                }
            }

            // Check for slowing units.
            Unit slowedBy = getSlowedBy(unit, newTile);
            if (slowedBy != null) {
                cs.addAttribute(See.only(serverPlayer), "slowedBy",
                                slowedBy.getId());
            }

            // Check for region discovery
            Region region = newTile.getDiscoverableRegion();
            if (serverPlayer.isEuropean() && region != null) {
                HistoryEvent h = null;
                if (region.isPacific()) {
                    cs.addAttribute(See.only(serverPlayer),
                                    "discoverPacific", "true");
                    cs.addRegion(serverPlayer, region, "model.region.pacific");
                } else {
                    String regionName = Messages.getDefaultRegionName(serverPlayer,
                                                                      region.getType());
                    if (serverPlayer.isAI()) {
                        // TODO: here is another dubious AI shortcut.
                        cs.addRegion(serverPlayer, region, regionName);
                    } else { // Ask player to name the region.
                        cs.addAttribute(See.only(serverPlayer),
                                        "discoverRegion", regionName);
                        cs.addAttribute(See.only(serverPlayer),
                                        "regionType",
                                        Messages.message(region.getLabel()));
                    }
                }
                if (h != null) cs.addHistory(serverPlayer, h);
            }
        }
    }

    /**
     * Set land name.
     *
     * @param serverPlayer The <code>ServerPlayer</code> who landed.
     * @param name The new land name.
     * @param welcomer An optional <code>ServerPlayer</code> that has offered
     *            a treaty.
     * @param accept True if the treaty has been accepted.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element setNewLandName(ServerPlayer serverPlayer, String name,
                                  ServerPlayer welcomer, boolean accept) {
        ChangeSet cs = new ChangeSet();

        // Special case of a welcome from an adjacent native unit,
        // offering the land the landing unit is on if a peace treaty
        // is accepted.  Slight awkwardness that we have to find the
        // unit that landed, which relies on this code being triggered
        // from the first landing and thus there is only one land unit
        // in the new world (which is not the case in a debug game).
        serverPlayer.setNewLandName(name);
        if (welcomer != null) {
            if (accept) { // Claim land
                for (Unit u : serverPlayer.getUnits()) {
                    if (u.isNaval()) continue;
                    Tile tile = u.getTile();
                    if (tile == null) continue;
                    if (tile.isLand() && tile.getOwner() == welcomer) {
                        tile.setOwner(serverPlayer);
                        cs.add(See.perhaps(), tile);
                        break;
                    }
                }
                welcomer = null;
            } else {
                // Consider not accepting the treaty to be an insult.  WWC1D?
                cs.add(See.only(null).perhaps(serverPlayer),
                       welcomer.modifyTension(serverPlayer,
                                              Tension.TENSION_ADD_MAJOR));
            }
        }

        // Update the name and note the history.
        cs.addPartial(See.only(serverPlayer), serverPlayer, "newLandName");
        Turn turn = serverPlayer.getGame().getTurn();
        cs.addHistory(serverPlayer,
            new HistoryEvent(turn, HistoryEvent.EventType.DISCOVER_NEW_WORLD)
                .addName("%name%", name));

        // Only the tile change is not private.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Set region name.
     *
     * @param serverPlayer The <code>ServerPlayer</code> discovering.
     * @param unit The <code>Unit</code> discovering the region.
     * @param region The <code>Region</code> to discover.
     * @param name The new region name.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element setNewRegionName(ServerPlayer serverPlayer, Unit unit,
                                    Region region, String name) {
        ChangeSet cs = new ChangeSet();
        cs.addRegion(serverPlayer, region, name);

        // Others do find out about region name changes.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Move a unit to Europe.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param unit The <code>Unit</code> to move to Europe.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element moveToEurope(ServerPlayer serverPlayer, Unit unit) {
        Europe europe = serverPlayer.getEurope();
        if (unit.getLocation() == europe) {
            // Unit already in Europe, nothing to see for the others.
            unit.setState(UnitState.TO_EUROPE);
            return new ChangeSet().add(See.only(serverPlayer), unit, europe)
                .build(serverPlayer);
        }

        ChangeSet cs = new ChangeSet();

        // Set entry location before setState (satisfy its check), then
        // set location.
        Tile tile = unit.getTile();
        unit.setEntryLocation(tile);
        unit.setState(UnitState.TO_EUROPE);
        unit.setLocation(europe);
        cs.addDisappear(serverPlayer, tile, unit);
        cs.add(See.only(serverPlayer), tile, europe);

        // Others see a disappearance, player sees tile and europe update
        // as europe now contains the unit.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Embark a unit onto a carrier.
     * Checking that the locations are appropriate is not done here.
     *
     * @param serverPlayer The <code>ServerPlayer</code> embarking.
     * @param unit The <code>Unit</code> that is embarking.
     * @param carrier The <code>Unit</code> to embark onto.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element embarkUnit(ServerPlayer serverPlayer, Unit unit,
                              Unit carrier) {
        if (unit.isNaval()) {
            return Message.clientError("Naval unit " + unit.getId()
                                       + " can not embark.");
        }
        if (carrier.getSpaceLeft() < unit.getSpaceTaken()) {
            return Message.clientError("No space available for unit "
                                       + unit.getId() + " to embark.");
        }

        ChangeSet cs = new ChangeSet();

        Location oldLocation = unit.getLocation();
        unit.setLocation(carrier);
        unit.setMovesLeft(0);
        unit.setState(UnitState.SENTRY);
        cs.add(See.only(serverPlayer), (FreeColGameObject) oldLocation);
        if (carrier.getLocation() != oldLocation) {
            cs.add(See.only(serverPlayer), carrier);
            cs.addMove(See.only(serverPlayer), unit, oldLocation,
                       carrier.getTile());
            cs.addDisappear(serverPlayer, carrier.getTile(), unit);
        }

        // Others might see the unit disappear, or the carrier capacity.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Disembark unit from a carrier.
     *
     * @param serverPlayer The <code>ServerPlayer</code> whose unit is
     *                     embarking.
     * @param unit The <code>Unit</code> that is disembarking.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element disembarkUnit(ServerPlayer serverPlayer, Unit unit) {
        if (unit.isNaval()) {
            return Message.clientError("Naval unit " + unit.getId()
                                       + " can not disembark.");
        }
        if (!(unit.getLocation() instanceof Unit)) {
            return Message.clientError("Unit " + unit.getId()
                                       + " is not embarked.");
        }

        ChangeSet cs = new ChangeSet();

        Unit carrier = (Unit) unit.getLocation();
        Location newLocation = carrier.getLocation();
        unit.setLocation(newLocation);
        unit.setMovesLeft(0); // In Col1 disembark consumes whole move.
        unit.setState(UnitState.ACTIVE);
        cs.add(See.perhaps(), (FreeColGameObject) newLocation);

        // Others can (potentially) see the location.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Combat.
     *
     * @param attackerPlayer The <code>ServerPlayer</code> who is attacking.
     * @param attacker The <code>FreeColGameObject</code> that is attacking.
     * @param defender The <code>FreeColGameObject</code> that is defending.
     * @param crs A list of <code>CombatResult</code>s defining the result.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element combat(ServerPlayer attackerPlayer,
                          FreeColGameObject attacker,
                          FreeColGameObject defender,
                          List<CombatResult> crs) {
        ChangeSet cs = new ChangeSet();
        try {
            csCombat(attackerPlayer, attacker, defender, crs, cs);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Combat FAIL", e);
            return Message.clientError(e.getMessage());
        }
        sendToOthers(attackerPlayer, cs);
        return cs.build(attackerPlayer);
    }

    /**
     * Combat.
     *
     * @param attackerPlayer The <code>ServerPlayer</code> who is attacking.
     * @param attacker The <code>FreeColGameObject</code> that is attacking.
     * @param defender The <code>FreeColGameObject</code> that is defending.
     * @param crs A list of <code>CombatResult</code>s defining the result.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csCombat(ServerPlayer attackerPlayer,
                          FreeColGameObject attacker,
                          FreeColGameObject defender,
                          List<CombatResult> crs,
                          ChangeSet cs) throws IllegalStateException {
        CombatModel combatModel = getGame().getCombatModel();
        boolean isAttack = combatModel.combatIsAttack(attacker, defender);
        boolean isBombard = combatModel.combatIsBombard(attacker, defender);
        Unit attackerUnit = null;
        Settlement attackerSettlement = null;
        Tile attackerTile = null;
        Unit defenderUnit = null;
        ServerPlayer defenderPlayer = null;
        Tile defenderTile = null;
        if (isAttack) {
            attackerUnit = (Unit) attacker;
            attackerTile = attackerUnit.getTile();
            defenderUnit = (Unit) defender;
            defenderPlayer = (ServerPlayer) defenderUnit.getOwner();
            defenderTile = defenderUnit.getTile();
            cs.addAttribute(See.only(attackerPlayer), "sound",
                (attackerUnit.isNaval())
                ? "sound.attack.naval"
                : (attackerUnit.hasAbility("model.ability.bombard"))
                ? "sound.attack.artillery"
                : (attackerUnit.isMounted())
                ? "sound.attack.mounted"
                : "sound.attack.foot");
        } else if (isBombard) {
            attackerSettlement = (Settlement) attacker;
            attackerTile = attackerSettlement.getTile();
            defenderUnit = (Unit) defender;
            defenderPlayer = (ServerPlayer) defenderUnit.getOwner();
            defenderTile = defenderUnit.getTile();
            cs.addAttribute(See.only(attackerPlayer), "sound",
                            "sound.attack.bombard");
        } else {
            throw new IllegalStateException("Bogus combat");
        }

        // If the combat results were not specified (usually the case),
        // query the combat model.
        if (crs == null) {
            crs = combatModel.generateAttackResult(random, attacker, defender);
        }
        if (crs.isEmpty()) {
            throw new IllegalStateException("empty attack result");
        }
        // Extract main result, insisting it is one of the fundamental cases,
        // and add the animation.
        // Set vis so that loser always sees things.
        // TODO: Bombard animations
        See vis; // Visibility that insists on the loser seeing the result.
        CombatResult result = crs.remove(0);
        switch (result) {
        case NO_RESULT:
            vis = See.perhaps();
            break; // Do not animate if there is no result.
        case WIN:
            vis = See.perhaps().always(defenderPlayer);
            if (isAttack) {
                cs.addAttack(vis, attackerUnit, defenderUnit, true);
            }
            break;
        case LOSE:
            vis = See.perhaps().always(attackerPlayer);
            if (isAttack) {
                cs.addAttack(vis, attackerUnit, defenderUnit, false);
            }
            break;
        default:
            throw new IllegalStateException("generateAttackResult returned: "
                                            + result);
        }
        // Now process the details.
        boolean attackerTileDirty = false;
        boolean defenderTileDirty = false;
        boolean moveAttacker = false;
        boolean burnedNativeCapital = false;
        Settlement settlement = defenderTile.getSettlement();
        Colony colony = defenderTile.getColony();
        IndianSettlement natives = (settlement instanceof IndianSettlement)
            ? (IndianSettlement) settlement
            : null;
        int attackerTension = 0;
        int defenderTension = 0;
        for (CombatResult cr : crs) {
            boolean ok;
            switch (cr) {
            case AUTOEQUIP_UNIT:
                ok = isAttack && settlement != null;
                if (ok) {
                    csAutoequipUnit(defenderUnit, settlement, cs);
                }
                break;
            case BURN_MISSIONS:
                ok = isAttack && result == CombatResult.WIN
                    && natives != null
                    && attackerPlayer.isEuropean() && defenderPlayer.isIndian();
                if (ok) {
                    defenderTileDirty
                        |= natives.getMissionary(attackerPlayer) != null;
                    csBurnMissions(attackerUnit, natives, cs);
                }
                break;
            case CAPTURE_AUTOEQUIP:
                ok = isAttack && result == CombatResult.WIN
                    && settlement != null
                    && defenderPlayer.isEuropean();
                if (ok) {
                    csCaptureAutoEquip(attackerUnit, defenderUnit, cs);
                    attackerTileDirty = defenderTileDirty = true;
                }
                break;
            case CAPTURE_COLONY:
                ok = isAttack && result == CombatResult.WIN
                    && colony != null
                    && attackerPlayer.isEuropean() && defenderPlayer.isEuropean();
                if (ok) {
                    csCaptureColony(attackerUnit, colony, cs);
                    attackerTileDirty = defenderTileDirty = true;
                    moveAttacker = true;
                    defenderTension += Tension.TENSION_ADD_MAJOR;
                }
                break;
            case CAPTURE_CONVERT:
                ok = isAttack && result == CombatResult.WIN
                    && natives != null
                    && attackerPlayer.isEuropean() && defenderPlayer.isIndian();
                if (ok) {
                    csCaptureConvert(attackerUnit, natives, cs);
                    attackerTileDirty = true;
                }
                break;
            case CAPTURE_EQUIP:
                ok = isAttack && result != CombatResult.NO_RESULT;
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csCaptureEquip(attackerUnit, defenderUnit, cs);
                    } else {
                        csCaptureEquip(defenderUnit, attackerUnit, cs);
                    }
                    attackerTileDirty = defenderTileDirty = true;
                }
                break;
            case CAPTURE_UNIT:
                ok = isAttack && result != CombatResult.NO_RESULT;
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csCaptureUnit(attackerUnit, defenderUnit, cs);
                    } else {
                        csCaptureUnit(defenderUnit, attackerUnit, cs);
                    }
                    attackerTileDirty = defenderTileDirty = true;
                }
                break;
            case DAMAGE_COLONY_SHIPS:
                ok = isAttack && result == CombatResult.WIN
                    && colony != null;
                if (ok) {
                    csDamageColonyShips(attackerUnit, colony, cs);
                    defenderTileDirty = true;
                }
                break;
            case DAMAGE_SHIP_ATTACK:
                ok = isAttack && result != CombatResult.NO_RESULT
                    && ((result == CombatResult.WIN) ? defenderUnit
                        : attackerUnit).isNaval();
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csDamageShipAttack(attackerUnit, defenderUnit, cs);
                        defenderTileDirty = true;
                    } else {
                        csDamageShipAttack(defenderUnit, attackerUnit, cs);
                        attackerTileDirty = true;
                    }
                }
                break;
            case DAMAGE_SHIP_BOMBARD:
                ok = isBombard && result == CombatResult.WIN
                    && defenderUnit.isNaval();
                if (ok) {
                    csDamageShipBombard(attackerSettlement, defenderUnit, cs);
                    defenderTileDirty = true;
                }
                break;
            case DEMOTE_UNIT:
                ok = isAttack && result != CombatResult.NO_RESULT;
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csDemoteUnit(attackerUnit, defenderUnit, cs);
                        defenderTileDirty = true;
                    } else {
                        csDemoteUnit(defenderUnit, attackerUnit, cs);
                        attackerTileDirty = true;
                    }
                }
                break;
            case DESTROY_COLONY:
                ok = isAttack && result == CombatResult.WIN
                    && colony != null
                    && attackerPlayer.isIndian() && defenderPlayer.isEuropean();
                if (ok) {
                    csDestroyColony(attackerUnit, colony, cs);
                    attackerTileDirty = defenderTileDirty = true;
                    moveAttacker = true;
                    attackerTension -= Tension.TENSION_ADD_NORMAL;
                    defenderTension += Tension.TENSION_ADD_MAJOR;
                }
                break;
            case DESTROY_SETTLEMENT:
                ok = isAttack && result == CombatResult.WIN
                    && natives != null
                    && defenderPlayer.isIndian();
                if (ok) {
                    csDestroySettlement(attackerUnit, natives, cs);
                    attackerTileDirty = defenderTileDirty = true;
                    moveAttacker = true;
                    burnedNativeCapital = settlement.isCapital();
                    attackerTension -= Tension.TENSION_ADD_NORMAL;
                    if (!burnedNativeCapital) {
                        defenderTension += Tension.TENSION_ADD_MAJOR;
                    }
                }
                break;
            case EVADE_ATTACK:
                ok = isAttack && result == CombatResult.NO_RESULT
                    && defenderUnit.isNaval();
                if (ok) {
                    csEvadeAttack(attackerUnit, defenderUnit, cs);
                }
                break;
            case EVADE_BOMBARD:
                ok = isBombard && result == CombatResult.NO_RESULT
                    && defenderUnit.isNaval();
                if (ok) {
                    csEvadeBombard(attackerSettlement, defenderUnit, cs);
                }
                break;
            case LOOT_SHIP:
                ok = isAttack && result != CombatResult.NO_RESULT
                    && attackerUnit.isNaval() && defenderUnit.isNaval();
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csLootShip(attackerUnit, defenderUnit, cs);
                    } else {
                        csLootShip(defenderUnit, attackerUnit, cs);
                    }
                }
                break;
            case LOSE_AUTOEQUIP:
                ok = isAttack && result == CombatResult.WIN
                    && settlement != null
                    && defenderPlayer.isEuropean();
                if (ok) {
                    csLoseAutoEquip(attackerUnit, defenderUnit, cs);
                    defenderTileDirty = true;
                }
                break;
            case LOSE_EQUIP:
                ok = isAttack && result != CombatResult.NO_RESULT;
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csLoseEquip(attackerUnit, defenderUnit, cs);
                        defenderTileDirty = true;
                    } else {
                        csLoseEquip(defenderUnit, attackerUnit, cs);
                        attackerTileDirty = true;
                    }
                }
                break;
            case PILLAGE_COLONY:
                ok = isAttack && result == CombatResult.WIN
                    && colony != null
                    && attackerPlayer.isIndian() && defenderPlayer.isEuropean();
                if (ok) {
                    csPillageColony(attackerUnit, colony, cs);
                    defenderTileDirty = true;
                    attackerTension -= Tension.TENSION_ADD_NORMAL;
                }
                break;
            case PROMOTE_UNIT:
                ok = isAttack && result != CombatResult.NO_RESULT;
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csPromoteUnit(attackerUnit, defenderUnit, cs);
                        attackerTileDirty = true;
                    } else {
                        csPromoteUnit(defenderUnit, attackerUnit, cs);
                        defenderTileDirty = true;
                    }
                }
                break;
            case SINK_COLONY_SHIPS:
                ok = isAttack && result == CombatResult.WIN
                    && colony != null;
                if (ok) {
                    csSinkColonyShips(attackerUnit, colony, cs);
                    defenderTileDirty = true;
                }
                break;
            case SINK_SHIP_ATTACK:
                ok = isAttack && result != CombatResult.NO_RESULT
                    && ((result == CombatResult.WIN) ? defenderUnit
                        : attackerUnit).isNaval();
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csSinkShipAttack(attackerUnit, defenderUnit, cs);
                        defenderTileDirty = true;
                    } else {
                        csSinkShipAttack(defenderUnit, attackerUnit, cs);
                        attackerTileDirty = true;
                    }
                }
                break;
            case SINK_SHIP_BOMBARD:
                ok = isBombard && result == CombatResult.WIN
                    && defenderUnit.isNaval();
                if (ok) {
                    csSinkShipBombard(attackerSettlement, defenderUnit, cs);
                    defenderTileDirty = true;
                }
                break;
            case SLAUGHTER_UNIT:
                ok = isAttack && result != CombatResult.NO_RESULT;
                if (ok) {
                    if (result == CombatResult.WIN) {
                        csSlaughterUnit(attackerUnit, defenderUnit, cs);
                        defenderTileDirty = true;
                        attackerTension -= Tension.TENSION_ADD_NORMAL;
                        defenderTension += getSlaughterTension(defenderUnit);
                    } else {
                        csSlaughterUnit(defenderUnit, attackerUnit, cs);
                        attackerTileDirty = true;
                        attackerTension += getSlaughterTension(attackerUnit);
                        defenderTension -= Tension.TENSION_ADD_NORMAL;
                    }
                }
                break;
            default:
                ok = false;
                break;
            }
            if (!ok) {
                throw new IllegalStateException("Attack (result=" + result
                                                + ") has bogus subresult: "
                                                + cr);
            }
        }

        // Handle stance and tension.
        // - Privateers do not provoke stance changes but can set the
        //     attackedByPrivateers flag
        // - Attacks among Europeans imply war
        // - Burning of a native capital results in surrender
        // - Other attacks involving natives do not imply war, but
        //     changes in Tension can drive Stance, however this is
        //     decided by the native AI in their turn so just adjust tension.
        if (attacker.hasAbility("model.ability.piracy")) {
            if (!defenderPlayer.getAttackedByPrivateers()) {
                defenderPlayer.setAttackedByPrivateers(true);
                cs.addPartial(See.only(defenderPlayer), defenderPlayer,
                              "attackedByPrivateers");
            }
        } else if (defender.hasAbility("model.ability.piracy")) {
            ; // do nothing
        } else if (attackerPlayer.isEuropean() && defenderPlayer.isEuropean()) {
            csChangeStance(attackerPlayer, Stance.WAR, defenderPlayer,
                           true, cs);
        } else if (burnedNativeCapital) {
            csChangeStance(attackerPlayer, Stance.PEACE, defenderPlayer,
                           true, cs);
            defenderPlayer.setTension(attackerPlayer,
                                      new Tension(Tension.SURRENDERED));
        } else { // At least one player is non-European
            if (attackerPlayer.isEuropean()) {
                csChangeStance(attackerPlayer, Stance.WAR, defenderPlayer,
                               false, cs);
            } else if (attackerPlayer.isIndian()) {
                if (result == CombatResult.WIN) {
                    attackerTension -= Tension.TENSION_ADD_MINOR;
                } else if (result == CombatResult.LOSE) {
                    attackerTension += Tension.TENSION_ADD_MINOR;
                }
            }
            if (defenderPlayer.isEuropean()) {
                csChangeStance(defenderPlayer, Stance.WAR, attackerPlayer,
                               false, cs);
            } else if (defenderPlayer.isIndian()) {
                if (result == CombatResult.WIN) {
                    defenderTension += Tension.TENSION_ADD_MINOR;
                } else if (result == CombatResult.LOSE) {
                    defenderTension -= Tension.TENSION_ADD_MINOR;
                }
            }
            if (attackerTension != 0) {
                cs.add(See.only(null).perhaps(defenderPlayer),
                       attackerPlayer.modifyTension(defenderPlayer,
                                                    attackerTension));
            }
            if (defenderTension != 0) {
                cs.add(See.only(null).perhaps(attackerPlayer),
                       defenderPlayer.modifyTension(attackerPlayer,
                                                    defenderTension));
            }
        }

        // Move the attacker if required.
        if (moveAttacker) {
            attackerUnit.setMovesLeft(attackerUnit.getInitialMovesLeft());
            csMove(attackerPlayer, attackerUnit, defenderTile, cs);
            // Move adds in updates for the tiles, but...
            attackerTileDirty = defenderTileDirty = false;
            // ...with visibility of perhaps().
            // Thus the defender might see the change,
            // but because its settlement is gone it also might not.
            // So add in another defender-specific update.
            // The worst that can happen is a duplicate update.
            cs.add(See.only(defenderPlayer), defenderTile);
        } else if (isAttack) {
            // The Revenger unit can attack multiple times, so spend
            // at least the eventual cost of moving to the tile.
            // Other units consume the entire move.
            if (attacker.hasAbility("model.ability.multipleAttacks")) {
                int movecost = attackerUnit.getMoveCost(defenderTile);
                attackerUnit.setMovesLeft(attackerUnit.getMovesLeft()
                                          - movecost);
            } else {
                attackerUnit.setMovesLeft(0);
            }
            if (!attackerTileDirty) {
                cs.addPartial(See.only(attackerPlayer), attacker, "movesLeft");
            }
        }

        // Make sure we always update the attacker and defender tile
        // if it is not already done yet.
        if (attackerTileDirty) cs.add(vis, attackerTile);
        if (defenderTileDirty) cs.add(vis, defenderTile);
    }

    /**
     * Gets the amount to raise tension by when a unit is slaughtered.
     *
     * @param loser The <code>Unit</code> that dies.
     * @return An amount to raise tension by.
     */
    private int getSlaughterTension(Unit loser) {
        // Tension rises faster when units die.
        Settlement settlement = loser.getTile().getSettlement();
        if (settlement != null) {
            if (settlement instanceof IndianSettlement) {
                return (((IndianSettlement) settlement).isCapital())
                    ? Tension.TENSION_ADD_CAPITAL_ATTACKED
                    : Tension.TENSION_ADD_SETTLEMENT_ATTACKED;
            } else {
                return Tension.TENSION_ADD_NORMAL;
            }
        } else { // attack in the open
            return (loser.getIndianSettlement() != null)
                ? Tension.TENSION_ADD_UNIT_DESTROYED
                : Tension.TENSION_ADD_MINOR;
        }
    }

    /**
     * Notifies of automatic arming.
     *
     * @param unit The <code>Unit</code> that is auto-equipping.
     * @param settlement The <code>Settlement</code> being defended.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csAutoequipUnit(Unit unit, Settlement settlement,
                                 ChangeSet cs) {
        ServerPlayer player = (ServerPlayer) unit.getOwner();
        cs.addMessage(See.only(player),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.automaticDefence",
                                       unit)
                      .addStringTemplate("%unit%", unit.getLabel())
                      .addName("%colony%", settlement.getName()));
    }

    /**
     * Burns a players missions.
     *
     * @param attacker The <code>Unit</code> that attacked.
     * @param settlement The <code>IndianSettlement</code> that was attacked.
     * @param cs The <code>ChangeSet</code> to update.
     */
    private void csBurnMissions(Unit attacker, IndianSettlement settlement,
                                ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate attackerNation = attackerPlayer.getNationName();
        ServerPlayer nativePlayer = (ServerPlayer) settlement.getOwner();
        StringTemplate nativeNation = nativePlayer.getNationName();

        // Message only for the European player
        cs.addMessage(See.only(attackerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.burnMissions",
                                       attacker, settlement)
                      .addStringTemplate("%nation%", attackerNation)
                      .addStringTemplate("%enemyNation%", nativeNation));

        // Burn down the missions
        for (IndianSettlement s : nativePlayer.getIndianSettlements()) {
            Unit missionary = s.getMissionary(attackerPlayer);
            if (missionary != null) {
                s.setMissionary(null);
                Tile tile = s.getTile();
                tile.updatePlayerExploredTiles();
                if (s != settlement) cs.add(See.perhaps(), tile);
            }
        }
    }

    /**
     * Defender autoequips but loses and attacker captures the equipment.
     *
     * @param attacker The <code>Unit</code> that attacked.
     * @param defender The <code>Unit</code> that defended and loses equipment.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csCaptureAutoEquip(Unit attacker, Unit defender,
                                    ChangeSet cs) {
        EquipmentType equip
            = defender.getBestCombatEquipmentType(defender.getAutomaticEquipment());
        csLoseAutoEquip(attacker, defender, cs);
        csCaptureEquipment(attacker, defender, equip, cs);
    }

    /**
     * Captures a colony.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param colony The <code>Colony</code> to capture.
     * @param cs The <code>ChangeSet</code> to update.
     */
    private void csCaptureColony(Unit attacker, Colony colony, ChangeSet cs) {
        Game game = attacker.getGame();
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate attackerNation = attackerPlayer.getNationName();
        ServerPlayer colonyPlayer = (ServerPlayer) colony.getOwner();
        StringTemplate colonyNation = colonyPlayer.getNationName();
        Tile tile = colony.getTile();
        int plunder = colony.getPlunder();

        // Handle history and messages before colony handover
        cs.addHistory(attackerPlayer,
                      new HistoryEvent(game.getTurn(),
                                       HistoryEvent.EventType.CONQUER_COLONY)
                      .addStringTemplate("%nation%", attackerNation)
                      .addName("%colony%", colony.getName()));
        cs.addHistory(colonyPlayer,
                      new HistoryEvent(game.getTurn(),
                                       HistoryEvent.EventType.COLONY_CONQUERED)
                      .addStringTemplate("%nation%", colonyNation)
                      .addName("%colony%", colony.getName()));
        cs.addMessage(See.only(attackerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.colonyCaptured",
                                       colony)
                      .addName("%colony%", colony.getName())
                      .addAmount("%amount%", plunder));
        cs.addMessage(See.only(colonyPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.colonyCapturedBy",
                                       colony.getTile())
                      .addName("%colony%", colony.getName())
                      .addAmount("%amount%", plunder)
                      .addStringTemplate("%player%", attackerNation));

        // Allocate some plunder
        attackerPlayer.modifyGold(plunder);
        cs.addPartial(See.only(attackerPlayer), attackerPlayer, "gold");
        colonyPlayer.modifyGold(-plunder);
        cs.addPartial(See.only(colonyPlayer), colonyPlayer, "gold");

        // Hand over the colony
        colony.changeOwner(attackerPlayer);

        // Process all the surrounding tiles
        int radius = Math.max(colony.getRadius(), colony.getLineOfSight());
        for (Tile t : colony.getTile().getSurroundingTiles(radius)) {
            if (t == colony.getTile() || t == attacker.getTile()) {
                ; // Will be updated when the attacker moves in
            } else if (t.getOwningSettlement() == colony) {
                // Tile changed owner
                cs.add(See.perhaps().always(colonyPlayer), t);
            } else if (!attackerPlayer.hasExplored(t)) {
                // New owner has now explored within settlement line of sight.
                attackerPlayer.setExplored(t);
                cs.add(See.only(attackerPlayer), t);
            }
        }
        cs.addAttribute(See.only(attackerPlayer), "sound",
                        "sound.event.captureColony");
    }

    /**
     * Extracts a convert from a native settlement.
     *
     * @pamam attacker The <code>Unit</code> that is attacking.
     * @param settlement The <code>IndianSettlement</code> under attack.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csCaptureConvert(Unit attacker, IndianSettlement natives,
                                  ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate convertNation = natives.getOwner().getNationName();
        List<UnitType> converts = getGame().getSpecification()
            .getUnitTypesWithAbility("model.ability.convert");
        UnitType type = converts.get(random.nextInt(converts.size()));
        Unit convert = natives.getLastUnit();

        cs.addMessage(See.only(attackerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.newConvertFromAttack",
                                       convert)
                      .addStringTemplate("%nation%", convertNation)
                      .addStringTemplate("%unit%", convert.getLabel()));

        convert.setOwner(attacker.getOwner());
        convert.setType(type);
        convert.setLocation(attacker.getTile());
    }

    /**
     * Captures equipment.
     *
     * @param winner The <code>Unit</code> that captures equipment.
     * @param loser The <code>Unit</code> that defended and loses equipment.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csCaptureEquip(Unit winner, Unit loser, ChangeSet cs) {
        EquipmentType equip
            = loser.getBestCombatEquipmentType(loser.getEquipment());
        csLoseEquip(winner, loser, cs);
        csCaptureEquipment(winner, loser, equip, cs);
    }

    /**
     * Capture equipment.
     *
     * @param winner The <code>Unit</code> that is capturing equipment.
     * @param loser The <code>Unit</code> that is losing equipment.
     * @param equip The <code>EquipmentType</code> to capture.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csCaptureEquipment(Unit winner, Unit loser,
                                    EquipmentType equip, ChangeSet cs) {
        ServerPlayer winnerPlayer = (ServerPlayer) winner.getOwner();
        ServerPlayer loserPlayer = (ServerPlayer) loser.getOwner();
        EquipmentType newEquip = equip;
        if (winnerPlayer.isIndian() != loserPlayer.isIndian()) {
            // May need to change the equipment type if the attacker is
            // native and the defender is not, or vice-versa.
            newEquip = equip.getCaptureEquipment(winnerPlayer.isIndian());
        }

        winner.equipWith(newEquip, true);

        // Currently can not capture equipment back so this only makes sense
        // for native players, and the message is native specific.
        if (winnerPlayer.isIndian()) {
            cs.addMessage(See.only(loserPlayer),
                          new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                           "model.unit.equipmentCaptured",
                                           winnerPlayer)
                          .addStringTemplate("%nation%", winnerPlayer.getNationName())
                          .add("%equipment%", newEquip.getNameKey()));

            // TODO: Immediately transferring the captured goods back
            // to a potentially remote settlement is pretty dubious.
            // Apparently Col1 did it, but its a CHEAT nonetheless.
            // Better would be to give the capturing unit a return-home-
            // -with-plunder mission.
            IndianSettlement settlement = winner.getIndianSettlement();
            if (settlement != null) {
                for (AbstractGoods goods : newEquip.getGoodsRequired()) {
                    settlement.addGoods(goods);
                }
                cs.add(See.only(winnerPlayer), settlement);
            }
        }
    }

    /**
     * Capture a unit.
     *
     * @param winner A <code>Unit</code> that is capturing.
     * @param loser A <code>Unit</code> to capture.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csCaptureUnit(Unit winner, Unit loser, ChangeSet cs) {
        ServerPlayer loserPlayer = (ServerPlayer) loser.getOwner();
        StringTemplate loserNation = loserPlayer.getNationName();
        StringTemplate loserLocation = loser.getLocation()
            .getLocationNameFor(loserPlayer);
        StringTemplate oldName = loser.getLabel();
        String messageId = loser.getType().getId() + ".captured";
        ServerPlayer winnerPlayer = (ServerPlayer) winner.getOwner();
        StringTemplate winnerNation = winnerPlayer.getNationName();
        StringTemplate winnerLocation = winner.getLocation()
            .getLocationNameFor(winnerPlayer);

        // Capture the unit
        UnitType type = loser.getTypeChange((winnerPlayer.isUndead())
                                            ? ChangeType.UNDEAD
                                            : ChangeType.CAPTURE, winnerPlayer);
        if (type != null) loser.setType(type);
        loser.setOwner(winnerPlayer);
        loser.setLocation(winner.getTile());
        loser.setState(UnitState.ACTIVE);

        // Winner message post-capture when it owns the loser
        cs.addMessage(See.only(winnerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       messageId, loser)
                      .setDefaultId("model.unit.unitCaptured")
                      .addStringTemplate("%nation%", loserNation)
                      .addStringTemplate("%unit%", oldName)
                      .addStringTemplate("%enemyNation%", winnerNation)
                      .addStringTemplate("%enemyUnit%", winner.getLabel())
                      .addStringTemplate("%location%", winnerLocation));
        cs.addMessage(See.only(loserPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       messageId, loser.getTile())
                      .setDefaultId("model.unit.unitCaptured")
                      .addStringTemplate("%nation%", loserNation)
                      .addStringTemplate("%unit%", oldName)
                      .addStringTemplate("%enemyNation%", winnerNation)
                      .addStringTemplate("%enemyUnit%", winner.getLabel())
                      .addStringTemplate("%location%", loserLocation));
    }

    /**
     * Damages all ships in a colony.
     *
     * @param attacker The <code>Unit</code> that is damaging.
     * @param colony The <code>Colony</code> to damage ships in.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csDamageColonyShips(Unit attacker, Colony colony,
                                     ChangeSet cs) {
        List<Unit> units = new ArrayList<Unit>(colony.getTile().getUnitList());
        while (!units.isEmpty()) {
            Unit unit = units.remove(0);
            if (unit.isNaval()) {
                csDamageShipAttack(attacker, unit, cs);
            }
        }
    }

    /**
     * Damage a ship through normal attack.
     *
     * @param attacker The attacker <code>Unit</code>.
     * @param ship The <code>Unit</code> which is a ship to damage.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csDamageShipAttack(Unit attacker, Unit ship,
                                    ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate attackerNation = attacker.getApparentOwnerName();
        ServerPlayer shipPlayer = (ServerPlayer) ship.getOwner();
        Location repair = ship.getTile().getRepairLocation(shipPlayer);
        StringTemplate repairLocationName = repair.getLocationNameFor(shipPlayer);
        Location oldLocation = ship.getLocation();
        StringTemplate shipNation = ship.getApparentOwnerName();

        cs.addMessage(See.only(attackerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.enemyShipDamaged",
                                       attacker)
                      .addStringTemplate("%unit%", attacker.getLabel())
                      .addStringTemplate("%enemyNation%", shipNation)
                      .addStringTemplate("%enemyUnit%", ship.getLabel()));
        cs.addMessage(See.only(shipPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.shipDamaged",
                                       ship)
                      .addStringTemplate("%unit%", ship.getLabel())
                      .addStringTemplate("%enemyUnit%", attacker.getLabel())
                      .addStringTemplate("%enemyNation%", attackerNation)
                      .addStringTemplate("%repairLocation%", repairLocationName));

        csDamageShip(ship, repair, cs);
    }

    /**
     * Damage a ship through bombard.
     *
     * @param settlement The attacker <code>Settlement</code>.
     * @param ship The <code>Unit</code> which is a ship to damage.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csDamageShipBombard(Settlement settlement, Unit ship,
                                     ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) settlement.getOwner();
        ServerPlayer shipPlayer = (ServerPlayer) ship.getOwner();
        Location repair = ship.getTile().getRepairLocation(shipPlayer);
        StringTemplate repairLocationName = repair.getLocationNameFor(shipPlayer);
        StringTemplate shipNation = ship.getApparentOwnerName();

        cs.addMessage(See.only(attackerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.enemyShipDamagedByBombardment",
                                       settlement)
                      .addName("%colony%", settlement.getName())
                      .addStringTemplate("%nation%", shipNation)
                      .addStringTemplate("%unit%", ship.getLabel()));
        cs.addMessage(See.only(shipPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.shipDamagedByBombardment",
                                       ship)
                      .addName("%colony%", settlement.getName())
                      .addStringTemplate("%unit%", ship.getLabel())
                      .addStringTemplate("%repairLocation%", repairLocationName));

        csDamageShip(ship, repair, cs);
    }

    /**
     * Damage a ship.
     *
     * @param ship The naval <code>Unit</code> to damage.
     * @param repair The <code>Location</code> to send it to.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csDamageShip(Unit ship, Location repair, ChangeSet cs) {
        ServerPlayer player = (ServerPlayer) ship.getOwner();

        // Lose the units aboard
        Unit u;
        while ((u = ship.getFirstUnit()) != null) {
            u.setLocation(null);
            cs.addDispose(player, ship, u);
        }

        // Damage the ship and send it off for repair
        ship.getGoodsContainer().removeAll();
        ship.setHitpoints(1);
        ship.setDestination(null);
        ship.setLocation(repair);
        ship.setState(UnitState.ACTIVE);
        ship.setMovesLeft(0);
        cs.add(See.only(player), (FreeColGameObject) repair);
    }

    /**
     * Demotes a unit.
     *
     * @param winner The <code>Unit</code> that won.
     * @param loser The <code>Unit</code> that lost and should be demoted.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csDemoteUnit(Unit winner, Unit loser, ChangeSet cs) {
        ServerPlayer loserPlayer = (ServerPlayer) loser.getOwner();
        StringTemplate loserNation = loserPlayer.getNationName();
        StringTemplate loserLocation = loser.getLocation()
            .getLocationNameFor(loserPlayer);
        StringTemplate oldName = loser.getLabel();
        String messageId = loser.getType().getId() + ".demoted";
        ServerPlayer winnerPlayer = (ServerPlayer) winner.getOwner();
        StringTemplate winnerNation = winnerPlayer.getNationName();
        StringTemplate winnerLocation = winner.getLocation()
            .getLocationNameFor(winnerPlayer);

        UnitType type = loser.getTypeChange(ChangeType.DEMOTION, loserPlayer);
        if (type == null || type == loser.getType()) {
            logger.warning("Demotion failed, type="
                           + ((type == null) ? "null"
                              : "same type: " + type));
            return;
        }
        loser.setType(type);

        cs.addMessage(See.only(winnerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       messageId, winner)
                      .setDefaultId("model.unit.unitDemoted")
                      .addStringTemplate("%nation%", loserNation)
                      .addStringTemplate("%oldName%", oldName)
                      .addStringTemplate("%unit%", loser.getLabel())
                      .addStringTemplate("%enemyNation%", winnerNation)
                      .addStringTemplate("%enemyUnit%", winner.getLabel())
                      .addStringTemplate("%location%", winnerLocation));
        cs.addMessage(See.only(loserPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       messageId, loser)
                      .setDefaultId("model.unit.unitDemoted")
                      .addStringTemplate("%nation%", loserNation)
                      .addStringTemplate("%oldName%", oldName)
                      .addStringTemplate("%unit%", loser.getLabel())
                      .addStringTemplate("%enemyNation%", winnerNation)
                      .addStringTemplate("%enemyUnit%", winner.getLabel())
                      .addStringTemplate("%location%", loserLocation));
    }

    /**
     * Destroy a colony.
     *
     * @param attacker The <code>Unit</code> that attacked.
     * @param colony The <code>Colony</code> that was attacked.
     * @param cs The <code>ChangeSet</code> to update.
     */
    private void csDestroyColony(Unit attacker, Colony colony,
                                 ChangeSet cs) {
        Game game = attacker.getGame();
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate attackerNation = attackerPlayer.getNationName();
        ServerPlayer colonyPlayer = (ServerPlayer) colony.getOwner();
        int plunder = colony.getPlunder();
        Tile tile = colony.getTile();

        // Handle history and messages before colony destruction.
        cs.addHistory(colonyPlayer,
                      new HistoryEvent(game.getTurn(),
                                       HistoryEvent.EventType.COLONY_DESTROYED)
                      .addStringTemplate("%nation%", attackerNation)
                      .addName("%colony%", colony.getName()));
        cs.addMessage(See.only(colonyPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.colonyBurning",
                                       colony.getTile())
                      .addName("%colony%", colony.getName())
                      .addAmount("%amount%", plunder)
                      .addStringTemplate("%nation%", attackerNation)
                      .addStringTemplate("%unit%", attacker.getLabel()));

        // Allocate some plunder.
        attackerPlayer.modifyGold(plunder);
        cs.addPartial(See.only(attackerPlayer), attackerPlayer, "gold");
        colonyPlayer.modifyGold(-plunder);
        cs.addPartial(See.only(colonyPlayer), colonyPlayer, "gold");

        // Dispose of the colony and its contents.
        csDisposeSettlement(colony, cs);
    }

    /**
     * Destroys an Indian settlement.
     *
     * @param attacker an <code>Unit</code> value
     * @param settlement an <code>IndianSettlement</code> value
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csDestroySettlement(Unit attacker,
                                     IndianSettlement settlement,
                                     ChangeSet cs) {
        Game game = getGame();
        Tile tile = settlement.getTile();
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        ServerPlayer nativePlayer = (ServerPlayer) settlement.getOwner();
        StringTemplate nativeNation = nativePlayer.getNationName();
        String settlementName = settlement.getName();

        // Destroy the settlement, update settlement tiles.
        csDisposeSettlement(settlement, cs);

        // Calculate the treasure amount.  Larger if Hernan Cortes is
        // present in the congress, from cities, and capitals.
        int treasure = random.nextInt(11);
        Set<Modifier> modifierSet = attackerPlayer.getFeatureContainer()
            .getModifierSet("model.modifier.nativeTreasureModifier");
        treasure = (int) FeatureContainer
            .applyModifierSet(treasure, game.getTurn(), modifierSet);
        SettlementType settlementType
            = ((IndianNationType) nativePlayer.getNationType())
            .getTypeOfSettlement();
        // TODO: move following to spec
        boolean isCity = settlementType == SettlementType.INCA_CITY
            || settlementType == SettlementType.AZTEC_CITY;
        treasure = (isCity) ? treasure * 500 + 1000
            : treasure * 50  + 300;
        if (settlement.isCapital()) treasure = (treasure * 3) / 2;

        // Make the treasure train.
        List<UnitType> unitTypes = getGame().getSpecification()
            .getUnitTypesWithAbility("model.ability.carryTreasure");
        UnitType type = unitTypes.get(random.nextInt(unitTypes.size()));
        Unit train = new Unit(game, tile, attackerPlayer, type,
                              UnitState.ACTIVE);
        train.setTreasureAmount(treasure);

        // This is an atrocity.
        int atrocities = Player.SCORE_SETTLEMENT_DESTROYED;
        if (isCity) atrocities *= 2;
        if (settlement.isCapital()) atrocities = (atrocities * 3) / 2;
        attackerPlayer.modifyScore(atrocities);
        cs.addPartial(See.only(attackerPlayer), attackerPlayer, "score");

        // Finish with messages and history.
        cs.addMessage(See.only(attackerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.indianTreasure",
                                       attacker)
                      .addName("%settlement%", settlementName)
                      .addAmount("%amount%", treasure));
        cs.addHistory(attackerPlayer,
                      new HistoryEvent(game.getTurn(),
                                       HistoryEvent.EventType.DESTROY_SETTLEMENT)
                      .addStringTemplate("%nation%", nativeNation)
                      .addName("%settlement%", settlementName));
        if (nativePlayer.getNumberOfSettlements() == 0) {
            cs.addHistory(attackerPlayer,
                          new HistoryEvent(game.getTurn(),
                                           HistoryEvent.EventType.DESTROY_NATION)
                          .addStringTemplate("%nation%", nativeNation));
        }
        cs.addAttribute(See.only(attackerPlayer), "sound",
                        "sound.event.destroySettlement");
    }

    /**
     * Disposes of a settlement and reassign its tiles.
     *
     * @param settlement The <code>Settlement</code> under attack.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csDisposeSettlement(Settlement settlement, ChangeSet cs) {
        ServerPlayer owner = (ServerPlayer) settlement.getOwner();
        HashMap<Settlement, Integer> votes = new HashMap<Settlement, Integer>();

        // Try to reassign the tiles
        List<Tile> owned = settlement.getOwnedTiles();
        while (!owned.isEmpty()) {
            Tile tile = owned.remove(0);
            votes.clear();
            for (Tile t : tile.getSurroundingTiles(1)) {
                // For each lost tile, find any neighbouring
                // settlements and give them a shout at claiming the tile.
                // Note that settlements now can own tiles outside
                // their radius--- if we encounter any of these clean
                // them up too.
                Settlement s = t.getOwningSettlement();
                if (s == null) {
                    ;
                } else if (s == settlement) {
                    // Add this to the tiles to process if its not
                    // there already.
                    if (!owned.contains(t)) owned.add(t);
                } else if (s.canClaimTile(t)) {
                    // Weight claimant settlements:
                    //   settlements owned by the same player
                    //     > settlements owned by same type of player
                    //     > other settlements
                    int value = (s.getOwner() == owner) ? 3
                        : (s.getOwner().isEuropean() == owner.isEuropean()) ? 2
                        : 1;
                    if (votes.get(s) != null) value += votes.get(s).intValue();
                    votes.put(s, new Integer(value));
                }
            }
            Settlement bestClaimant = null;
            int bestClaim = 0;
            for (Entry<Settlement, Integer> vote : votes.entrySet()) {
                if (vote.getValue().intValue() > bestClaim) {
                    bestClaimant = vote.getKey();
                    bestClaim = vote.getValue().intValue();
                }
            }
            if (bestClaimant == null) {
                settlement.disclaimTile(tile);
            } else {
                bestClaimant.claimTile(tile);
            }
            if (tile != settlement.getTile()) {
                cs.add(See.perhaps().always(owner), tile);
            }
        }

        // Settlement goes away
        cs.addDispose(owner, settlement.getTile(), settlement);
    }

    /**
     * Evade a normal attack.
     *
     * @param attacker The attacker <code>Unit</code>.
     * @param defender A naval <code>Unit</code> that evades the attacker.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csEvadeAttack(Unit attacker, Unit defender, ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate attackerNation = attacker.getApparentOwnerName();
        ServerPlayer defenderPlayer = (ServerPlayer) defender.getOwner();
        StringTemplate defenderNation = defender.getApparentOwnerName();

        cs.addMessage(See.only(attackerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.enemyShipEvaded",
                                       attacker)
                      .addStringTemplate("%unit%", attacker.getLabel())
                      .addStringTemplate("%enemyUnit%", defender.getLabel())
                      .addStringTemplate("%enemyNation%", defenderNation));
        cs.addMessage(See.only(defenderPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.shipEvaded",
                                       defender)
                      .addStringTemplate("%unit%", defender.getLabel())
                      .addStringTemplate("%enemyUnit%", attacker.getLabel())
                      .addStringTemplate("%enemyNation%", attackerNation));
    }

    /**
     * Evade a bombardment.
     *
     * @param settlement The attacker <code>Settlement</code>.
     * @param defender A naval <code>Unit</code> that evades the attacker.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csEvadeBombard(Settlement settlement, Unit defender,
                                ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) settlement.getOwner();
        ServerPlayer defenderPlayer = (ServerPlayer) defender.getOwner();
        StringTemplate defenderNation = defender.getApparentOwnerName();

        cs.addMessage(See.only(attackerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.shipEvadedBombardment",
                                       settlement)
                      .addName("%colony%", settlement.getName())
                      .addStringTemplate("%unit%", defender.getLabel())
                      .addStringTemplate("%nation%", defenderNation));
        cs.addMessage(See.only(defenderPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.shipEvadedBombardment",
                                       defender)
                      .addName("%colony%", settlement.getName())
                      .addStringTemplate("%unit%", defender.getLabel())
                      .addStringTemplate("%nation%", defenderNation));
    }

    /**
     * Loot a ship.
     *
     * @param winner The winning naval <code>Unit</code>.
     * @param loser The losing naval <code>Unit</code>
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csLootShip(Unit winner, Unit loser, ChangeSet cs) {
        List<Goods> capture = new ArrayList<Goods>(loser.getGoodsList());
        ServerPlayer winnerPlayer = (ServerPlayer) winner.getOwner();
        if (capture.size() <= 0) return;
        if (winnerPlayer.isAI()) {
            // This should be in the AI code.
            // Just loot in order of sale value.
            final Market market = winnerPlayer.getMarket();
            Collections.sort(capture, new Comparator<Goods>() {
                    public int compare(Goods g1, Goods g2) {
                        int p1 = market.getPaidForSale(g1.getType())
                            * g1.getAmount();
                        int p2 = market.getPaidForSale(g2.getType())
                            * g2.getAmount();
                        return p2 - p1;
                    }
                });
            while (capture.size() > 0) {
                Goods g = capture.remove(0);
                if (!winner.canAdd(g)) break;
                winner.add(g);
            }
        } else {
            for (Goods g : capture) g.setLocation(null);
            TransactionSession ts = TransactionSession.create(winner, loser);
            ts.put("lootCargo", capture);
            cs.addAttribute(See.only(winnerPlayer), "loot", "true");
        }
        loser.getGoodsContainer().removeAll();
        loser.setState(UnitState.ACTIVE);
    }

    /**
     * Unit autoequips but loses equipment.
     *
     * @param attacker The <code>Unit</code> that attacked.
     * @param defender The <code>Unit</code> that defended and loses equipment.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csLoseAutoEquip(Unit attacker, Unit defender, ChangeSet cs) {
        ServerPlayer defenderPlayer = (ServerPlayer) defender.getOwner();
        StringTemplate defenderNation = defenderPlayer.getNationName();
        Settlement settlement = defender.getTile().getSettlement();
        StringTemplate defenderLocation = defender.getLocation().getLocationNameFor(defenderPlayer);
        EquipmentType equip = defender
            .getBestCombatEquipmentType(defender.getAutomaticEquipment());
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate attackerNation = attackerPlayer.getNationName();

        // Autoequipment is not actually with the unit, it is stored
        // in the settlement of the unit.  Remove it from there.
        for (AbstractGoods goods : equip.getGoodsRequired()) {
            settlement.removeGoods(goods);
        }

        cs.addMessage(See.only((ServerPlayer) defender.getOwner()),
            new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                             "model.unit.unitLoseAutoEquip",
                             defender)
                .addStringTemplate("%location%", defenderLocation)
                .addStringTemplate("%nation%", defenderNation)
                .addStringTemplate("%unit%", defender.getLabel())
                .addName("%settlement%", settlement.getNameFor(defenderPlayer))
                .addStringTemplate("%enemyNation%", attackerNation)
                .addStringTemplate("%enemyUnit%", attacker.getLabel()));
    }

    /**
     * Unit drops some equipment.
     *
     * @param winner The <code>Unit</code> that won.
     * @param loser The <code>Unit</code> that lost and loses equipment.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csLoseEquip(Unit winner, Unit loser, ChangeSet cs) {
        ServerPlayer loserPlayer = (ServerPlayer) loser.getOwner();
        StringTemplate loserNation = loserPlayer.getNationName();
        StringTemplate loserLocation = loser.getLocation()
            .getLocationNameFor(loserPlayer);
        StringTemplate oldName = loser.getLabel();
        ServerPlayer winnerPlayer = (ServerPlayer) winner.getOwner();
        StringTemplate winnerNation = winnerPlayer.getNationName();
        StringTemplate winnerLocation = winner.getLocation()
            .getLocationNameFor(winnerPlayer);
        EquipmentType equip
            = loser.getBestCombatEquipmentType(loser.getEquipment());

        loser.removeEquipment(equip, 1, true);
        String messageId;
        if (loser.getEquipment().isEmpty()) {
            messageId = "model.unit.unitDemotedToUnarmed";
            loser.setState(UnitState.ACTIVE);
        } else {
            messageId = loser.getType().getId() + ".demoted";
        }

        cs.addMessage(See.only(winnerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       messageId, winner)
                      .setDefaultId("model.unit.unitDemoted")
                      .addStringTemplate("%nation%", loserNation)
                      .addStringTemplate("%oldName%", oldName)
                      .addStringTemplate("%unit%", loser.getLabel())
                      .addStringTemplate("%enemyNation%", winnerNation)
                      .addStringTemplate("%enemyUnit%", winner.getLabel())
                      .addStringTemplate("%location%", winnerLocation));
        cs.addMessage(See.only(loserPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       messageId, loser)
                      .setDefaultId("model.unit.unitDemoted")
                      .addStringTemplate("%nation%", loserNation)
                      .addStringTemplate("%oldName%", oldName)
                      .addStringTemplate("%unit%", loser.getLabel())
                      .addStringTemplate("%enemyNation%", winnerNation)
                      .addStringTemplate("%enemyUnit%", winner.getLabel())
                      .addStringTemplate("%location%", loserLocation));
    }

    /**
     * Damage a building or a ship or steal some goods or gold.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param colony The <code>Colony</code> to pillage.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csPillageColony(Unit attacker, Colony colony, ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) attacker.getOwner();
        StringTemplate attackerNation = attackerPlayer.getNationName();
        ServerPlayer colonyPlayer = (ServerPlayer) colony.getOwner();

        // Collect the damagable buildings, ships, movable goods.
        List<Building> buildingList = colony.getBurnableBuildingList();
        List<Unit> shipList = colony.getShipList();
        List<Goods> goodsList = colony.getLootableGoodsList();

        // Pick one, with one extra choice for stealing gold.
        int pillage = random.nextInt(buildingList.size() + shipList.size()
                                     + goodsList.size()
                                     + ((colony.getPlunder() == 0) ? 0 : 1));
        if (pillage < buildingList.size()) {
            Building building = buildingList.get(pillage);
            cs.addMessage(See.only(colonyPlayer),
                          new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                           "model.unit.buildingDamaged",
                                           colony)
                          .add("%building%", building.getNameKey())
                          .addName("%colony%", colony.getName())
                          .addStringTemplate("%enemyNation%", attackerNation)
                          .addStringTemplate("%enemyUnit%", attacker.getLabel()));
            colony.damageBuilding(building);
        } else if (pillage < buildingList.size() + shipList.size()) {
            Unit ship = shipList.get(pillage - buildingList.size());
            if (colony.getTile().getRepairLocation(colonyPlayer) == null) {
                csSinkShipAttack(attacker, ship, cs);
            } else {
                csDamageShipAttack(attacker, ship, cs);
            }
        } else if (pillage < buildingList.size() + shipList.size()
                   + goodsList.size()) {
            Goods goods = goodsList.get(pillage - buildingList.size()
                                        - shipList.size());
            goods.setAmount(Math.min(goods.getAmount() / 2, 50));
            colony.removeGoods(goods);
            if (attacker.getSpaceLeft() > 0) attacker.add(goods);
            cs.addMessage(See.only(colonyPlayer),
                          new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                           "model.unit.goodsStolen",
                                           colony, goods)
                          .addAmount("%amount%", goods.getAmount())
                          .add("%goods%", goods.getNameKey())
                          .addName("%colony%", colony.getName())
                          .addStringTemplate("%enemyNation%", attackerNation)
                          .addStringTemplate("%enemyUnit%", attacker.getLabel()));

        } else {
            int gold = colonyPlayer.getGold() / 10;
            colonyPlayer.modifyGold(-gold);
            attackerPlayer.modifyGold(gold);
            cs.addMessage(See.only(colonyPlayer),
                          new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                           "model.unit.indianPlunder",
                                           colony)
                          .addAmount("%amount%", gold)
                          .addName("%colony%", colony.getName())
                          .addStringTemplate("%enemyNation%", attackerNation)
                          .addStringTemplate("%enemyUnit%", attacker.getLabel()));
        }
    }

    /**
     * Promotes a unit.
     *
     * @param winner The <code>Unit</code> that won and should be promoted.
     * @param loser The <code>Unit</code> that lost.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csPromoteUnit(Unit winner, Unit loser, ChangeSet cs) {
        ServerPlayer winnerPlayer = (ServerPlayer) winner.getOwner();
        StringTemplate winnerNation = winnerPlayer.getNationName();
        StringTemplate oldName = winner.getLabel();

        UnitType type = winner.getTypeChange(ChangeType.PROMOTION,
                                             winnerPlayer);
        if (type == null || type == winner.getType()) {
            logger.warning("Promotion failed, type="
                           + ((type == null) ? "null"
                              : "same type: " + type));
            return;
        }
        winner.setType(type);
        cs.addMessage(See.only(winnerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.unitPromoted", winner)
                      .addStringTemplate("%oldName%", oldName)
                      .addStringTemplate("%unit%", winner.getLabel())
                      .addStringTemplate("%nation%", winnerNation));
    }

    /**
     * Sinks all ships in a colony.
     *
     * @param attacker The attacker <code>Unit</code>.
     * @param colony The <code>Colony</code> to sink ships in.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csSinkColonyShips(Unit attacker, Colony colony, ChangeSet cs) {
        List<Unit> units = new ArrayList<Unit>(colony.getTile().getUnitList());
        while (!units.isEmpty()) {
            Unit unit = units.remove(0);
            if (unit.isNaval()) {
                csSinkShipAttack(attacker, unit, cs);
            }
        }
    }

    /**
     * Sinks this ship as result of a normal attack.
     *
     * @param attacker The attacker <code>Unit</code>.
     * @param ship The naval <code>Unit</code> to sink.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csSinkShipAttack(Unit attacker, Unit ship, ChangeSet cs) {
        ServerPlayer shipPlayer = (ServerPlayer) ship.getOwner();
        StringTemplate shipNation = ship.getApparentOwnerName();
        Unit attackerUnit = (Unit) attacker;
        ServerPlayer attackerPlayer = (ServerPlayer) attackerUnit.getOwner();
        StringTemplate attackerNation = attackerUnit.getApparentOwnerName();

        cs.addMessage(See.only(attackerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.enemyShipSunk",
                                       attackerUnit)
                      .addStringTemplate("%unit%", attackerUnit.getLabel())
                      .addStringTemplate("%enemyUnit%", ship.getLabel())
                      .addStringTemplate("%enemyNation%", shipNation));
        cs.addMessage(See.only(shipPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.shipSunk",
                                       ship.getTile())
                      .addStringTemplate("%unit%", ship.getLabel())
                      .addStringTemplate("%enemyUnit%", attackerUnit.getLabel())
                      .addStringTemplate("%enemyNation%", attackerNation));

        csSinkShip(ship, attackerPlayer, cs);
    }

    /**
     * Sinks this ship as result of a bombard.
     *
     * @param settlement The bombarding <code>Settlement</code>.
     * @param ship The naval <code>Unit</code> to sink.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csSinkShipBombard(Settlement settlement, Unit ship,
                                   ChangeSet cs) {
        ServerPlayer attackerPlayer = (ServerPlayer) settlement.getOwner();
        ServerPlayer shipPlayer = (ServerPlayer) ship.getOwner();
        StringTemplate shipNation = ship.getApparentOwnerName();

        cs.addMessage(See.only(attackerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.shipSunkByBombardment",
                                       settlement)
                      .addName("%colony%", settlement.getName())
                      .addStringTemplate("%unit%", ship.getLabel())
                      .addStringTemplate("%nation%", shipNation));
        cs.addMessage(See.only(shipPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.shipSunkByBombardment",
                                       ship.getTile())
                      .addName("%colony%", settlement.getName())
                      .addStringTemplate("%unit%", ship.getLabel()));

        csSinkShip(ship, attackerPlayer, cs);
    }

    /**
     * Sink the ship.
     *
     * @param ship The naval <code>Unit</code> to sink.
     * @param attackerPlayer The <code>ServerPlayer</code> that attacked.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csSinkShip(Unit ship, ServerPlayer attackerPlayer,
                            ChangeSet cs) {
        ServerPlayer shipPlayer = (ServerPlayer) ship.getOwner();
        cs.addDispose(shipPlayer, ship.getLocation(), ship);
        cs.addAttribute(See.only(attackerPlayer), "sound",
                        "sound.event.shipSunk");
    }

    /**
     * Slaughter a unit.
     *
     * @param winner The <code>Unit</code> that is slaughtering.
     * @param loser The <code>Unit</code> to slaughter.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csSlaughterUnit(Unit winner, Unit loser, ChangeSet cs) {
        ServerPlayer winnerPlayer = (ServerPlayer) winner.getOwner();
        StringTemplate winnerNation = winnerPlayer.getNationName();
        StringTemplate winnerLocation = winner.getLocation()
            .getLocationNameFor(winnerPlayer);
        ServerPlayer loserPlayer = (ServerPlayer) loser.getOwner();
        StringTemplate loserNation = loserPlayer.getNationName();
        StringTemplate loserLocation = loser.getLocation()
            .getLocationNameFor(loserPlayer);
        String messageId = loser.getType().getId() + ".destroyed";

        cs.addMessage(See.only(winnerPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       messageId, winner)
                      .setDefaultId("model.unit.unitSlaughtered")
                      .addStringTemplate("%nation%", loserNation)
                      .addStringTemplate("%unit%", loser.getLabel())
                      .addStringTemplate("%enemyNation%", winnerNation)
                      .addStringTemplate("%enemyUnit%", winner.getLabel())
                      .addStringTemplate("%location%", winnerLocation));
        cs.addMessage(See.only(loserPlayer),
                      new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       messageId, loser.getTile())
                      .setDefaultId("model.unit.unitSlaughtered")
                      .addStringTemplate("%nation%", loserNation)
                      .addStringTemplate("%unit%", loser.getLabel())
                      .addStringTemplate("%enemyNation%", winnerNation)
                      .addStringTemplate("%enemyUnit%", winner.getLabel())
                      .addStringTemplate("%location%", loserLocation));

        // Transfer equipment, do not generate messages for the loser.
        EquipmentType equip;
        while ((equip = loser.getBestCombatEquipmentType(loser.getEquipment()))
               != null) {
            loser.removeEquipment(equip, 1, true);
            if ((equip = winner.canCaptureEquipment(equip, loser)) != null) {
                csCaptureEquipment(winner, loser, equip, cs);
            }
        }

        // Destroy unit.
        cs.addDispose(loserPlayer, loser.getLocation(), loser);
    }

    /**
     * Ask about learning a skill at an IndianSettlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is learning.
     * @param unit The <code>Unit</code> that is learning.
     * @param settlement The <code>Settlement</code> to learn from.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element askLearnSkill(ServerPlayer serverPlayer, Unit unit,
                                 IndianSettlement settlement) {
        ChangeSet cs = new ChangeSet();

        Tile tile = settlement.getTile();
        PlayerExploredTile pet = tile.getPlayerExploredTile(serverPlayer);
        settlement.makeContactSettlement(serverPlayer);
        pet.setVisited();
        pet.setSkill(settlement.getLearnableSkill());
        cs.add(See.only(serverPlayer), tile);
        unit.setMovesLeft(0);
        cs.addPartial(See.only(serverPlayer), unit, "movesLeft");

        // Do not update others, nothing to see yet.
        return cs.build(serverPlayer);
    }

    /**
     * Learn a skill at an IndianSettlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is learning.
     * @param unit The <code>Unit</code> that is learning.
     * @param settlement The <code>Settlement</code> to learn from.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element learnFromIndianSettlement(ServerPlayer serverPlayer,
                                             Unit unit,
                                             IndianSettlement settlement) {
        // Sanity checks.
        UnitType skill = settlement.getLearnableSkill();
        if (skill == null) {
            return Message.clientError("No skill to learn at "
                                       + settlement.getName());
        }
        if (!unit.getType().canBeUpgraded(skill, ChangeType.NATIVES)) {
            return Message.clientError("Unit " + unit.toString()
                                       + " can not learn skill " + skill
                                       + " at " + settlement.getName());
        }

        ChangeSet cs = new ChangeSet();

        // Try to learn
        unit.setMovesLeft(0);
        switch (settlement.getAlarm(serverPlayer).getLevel()) {
        case HATEFUL: // Killed
            cs.add(See.perhaps().always(serverPlayer),
                   (FreeColGameObject) unit.getLocation());
            cs.addDispose(serverPlayer, unit.getLocation(), unit);
            break;
        case ANGRY: // Learn nothing, not even a pet update
            cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
            break;
        default:
            // Teach the unit, and expend the skill if necessary.
            // Do a full information update as the unit is in the settlement.
            unit.setType(skill);
            if (!settlement.isCapital()) settlement.setLearnableSkill(null);
            Tile tile = settlement.getTile();
            tile.updateIndianSettlementInformation(serverPlayer);
            cs.add(See.only(serverPlayer), unit, tile);
            break;
        }

        // Others always see the unit, it may have died or been taught.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }


    /**
     * Demand a tribute from a native settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> demanding the tribute.
     * @param unit The <code>Unit</code> that is demanding the tribute.
     * @param settlement The <code>IndianSettlement</code> demanded of.
     * @return An <code>Element</code> encapsulating this action.
     * TODO: Move TURNS_PER_TRIBUTE magic number to the spec.
     */
    public Element demandTribute(ServerPlayer serverPlayer, Unit unit,
                                 IndianSettlement settlement) {
        ChangeSet cs = new ChangeSet();
        final int TURNS_PER_TRIBUTE = 5;

        Player indianPlayer = settlement.getOwner();
        int gold = 0;
        int year = getGame().getTurn().getNumber();
        settlement.makeContactSettlement(serverPlayer);
        if (settlement.getLastTribute() + TURNS_PER_TRIBUTE < year
            && indianPlayer.getGold() > 0) {
            switch (indianPlayer.getTension(serverPlayer).getLevel()) {
            case HAPPY: case CONTENT:
                gold = Math.min(indianPlayer.getGold() / 10, 100);
                break;
            case DISPLEASED:
                gold = Math.min(indianPlayer.getGold() / 20, 100);
                break;
            case ANGRY: case HATEFUL:
            default:
                break; // do nothing
            }
        }

        // Increase tension whether we paid or not.  Apply tension
        // directly to the settlement and let propagation work.
        cs.add(See.only(serverPlayer),
            settlement.modifyAlarm(serverPlayer, Tension.TENSION_ADD_NORMAL));
        settlement.setLastTribute(year);
        ModelMessage m;
        if (gold > 0) {
            indianPlayer.modifyGold(-gold);
            serverPlayer.modifyGold(gold);
            cs.addPartial(See.only(serverPlayer), serverPlayer, "gold", "score");
            m = new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                 "scoutSettlement.tributeAgree",
                                 unit, settlement)
                .addAmount("%amount%", gold);
        } else {
            m = new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                 "scoutSettlement.tributeDisagree",
                                 unit, settlement);
        }
        cs.addMessage(See.only(serverPlayer), m);
        unit.setMovesLeft(0);
        cs.addPartial(See.only(serverPlayer), unit, "movesLeft");

        // Do not update others, this is all private.
        return cs.build(serverPlayer);
    }

    /**
     * Scout a native settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is scouting.
     * @param unit The scout <code>Unit</code>.
     * @param settlement The <code>IndianSettlement</code> to scout.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element scoutIndianSettlement(ServerPlayer serverPlayer,
                                         Unit unit,
                                         IndianSettlement settlement) {
        ChangeSet cs = new ChangeSet();
        String result;

        // Hateful natives kill the scout right away.
        settlement.makeContactSettlement(serverPlayer);
        Tension tension = settlement.getAlarm(serverPlayer);
        if (tension.getLevel() == Tension.Level.HATEFUL) {
            cs.add(See.perhaps().always(serverPlayer),
                   (FreeColGameObject) unit.getLocation());
            cs.addDispose(serverPlayer, unit.getLocation(), unit);
            result = "die";
        } else {
            // Otherwise player gets to visit, and learn about the settlement.
            int gold = 0;
            Tile tile = settlement.getTile();
            int radius = unit.getLineOfSight();
            UnitType skill = settlement.getLearnableSkill();
            if (settlement.hasBeenVisited()) {
                // Pre-visited settlements are a noop.
                result = "nothing";
            } else if (skill != null
                       && skill.hasAbility("model.ability.expertScout")
                       && unit.getType().canBeUpgraded(skill, ChangeType.NATIVES)) {
                // If the scout can be taught to be an expert it will be.
                // TODO: in the old code the settlement retains the
                // teaching ability.  Is this Col1 compliant?
                unit.setType(settlement.getLearnableSkill());
                // settlement.setLearnableSkill(null);
                cs.add(See.perhaps(), unit);
                result = "expert";
            } else if (random.nextInt(3) == 0) {
                // Otherwise 1/3 of cases are tales...
                radius = Math.max(radius, IndianSettlement.TALES_RADIUS);
                result = "tales";
            } else {
                // ...and the rest are beads.
                gold = (random.nextInt(400) * settlement.getBonusMultiplier())
                    + 50;
                if (unit.hasAbility("model.ability.expertScout")) {
                    gold = (gold * 11) / 10;
                }
                serverPlayer.modifyGold(gold);
                settlement.getOwner().modifyGold(-gold);
                cs.addPartial(See.only(serverPlayer), serverPlayer,
                              "gold", "score");
                result = "beads";
            }

            // Update settlement tile with new information, and any
            // newly visible tiles, possibly with enhanced radius.
            settlement.setVisited(serverPlayer);
            tile.updateIndianSettlementInformation(serverPlayer);
            cs.add(See.only(serverPlayer), tile);
            for (Tile t : tile.getSurroundingTiles(radius)) {
                if (!serverPlayer.canSee(t) && (t.isLand() || t.isCoast())) {
                    serverPlayer.setExplored(t);
                    cs.add(See.only(serverPlayer), t);
                }
            }

            // If the unit did not get promoted, update it for moves.
            unit.setMovesLeft(0);
            if (!"expert".equals(result)) {
                cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
            }
        }
        // Always add result.
        cs.addAttribute(See.only(serverPlayer), "result", result);

        // Other players may be able to see unit disappearing, or
        // learning.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }


    /**
     * Denounce an existing mission.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is denouncing.
     * @param unit The <code>Unit</code> denouncing.
     * @param settlement The <code>IndianSettlement</code> containing the
     *                   mission to denounce.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element denounceMission(ServerPlayer serverPlayer, Unit unit,
                                   IndianSettlement settlement) {
        // Determine result
        Unit missionary = settlement.getMissionary();
        ServerPlayer enemy = (ServerPlayer) missionary.getOwner();
        double denounce = random.nextDouble() * enemy.getImmigration()
            / (serverPlayer.getImmigration() + 1);
        if (missionary.hasAbility("model.ability.expertMissionary")) {
            denounce += 0.2;
        }
        if (unit.hasAbility("model.ability.expertMissionary")) {
            denounce -= 0.2;
        }

        if (denounce < 0.5) { // Success, remove old mission and establish ours
            return establishMission(serverPlayer, unit, settlement);
        }

        ChangeSet cs = new ChangeSet();

        // Denounce failed
        Player owner = settlement.getOwner();
        settlement.makeContactSettlement(serverPlayer);
        cs.addMessage(See.only(serverPlayer),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                             "indianSettlement.mission.noDenounce",
                             serverPlayer, unit)
                .addStringTemplate("%nation%", owner.getNationName()));
        cs.add(See.perhaps().always(serverPlayer),
               (FreeColGameObject) unit.getLocation());
        cs.addDispose(serverPlayer, unit.getLocation(), unit);

        // Others can see missionary disappear
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Establish a new mission.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is establishing.
     * @param unit The missionary <code>Unit</code>.
     * @param settlement The <code>IndianSettlement</code> to establish at.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element establishMission(ServerPlayer serverPlayer, Unit unit,
                                    IndianSettlement settlement) {
        ChangeSet cs = new ChangeSet();

        Unit missionary = settlement.getMissionary();
        Tile tile = settlement.getTile();
        ServerPlayer enemy = null;
        if (missionary != null) {
            enemy = (ServerPlayer) missionary.getOwner();
            settlement.setMissionary(null);
            tile.updatePlayerExploredTile(serverPlayer);
            tile.updateIndianSettlementInformation(serverPlayer);

            // Inform the enemy of loss of mission
            cs.add(See.perhaps().always(enemy), settlement.getTile());
            cs.addDispose(enemy, settlement.getTile(), missionary);
            cs.addMessage(See.only(enemy),
                new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                 "indianSettlement.mission.denounced",
                                 settlement)
                    .addStringTemplate("%settlement%", settlement.getLocationNameFor(enemy)));
        }

        // Result depends on tension wrt this settlement.
        // Establish if at least not angry.
        settlement.makeContactSettlement(serverPlayer);
        switch (settlement.getAlarm(serverPlayer).getLevel()) {
        case HATEFUL: case ANGRY:
            cs.add(See.perhaps().always(serverPlayer),
                   (FreeColGameObject) unit.getLocation());
            cs.addDispose(serverPlayer, unit.getLocation(), unit);
            break;
        case HAPPY: case CONTENT: case DISPLEASED:
            cs.add(See.perhaps().always(serverPlayer), unit.getTile());
            unit.setLocation(null);
            settlement.setMissionary(unit);
            settlement.setConvertProgress(0);
            cs.add(See.only(serverPlayer),
                   settlement.modifyAlarm(serverPlayer, ALARM_NEW_MISSIONARY));
            tile.updatePlayerExploredTile(serverPlayer);
            tile.updateIndianSettlementInformation(serverPlayer);
            cs.add(See.perhaps().always(serverPlayer), tile);
            break;
        }
        String messageId = "indianSettlement.mission."
            + settlement.getAlarm(serverPlayer).toString();
        cs.addMessage(See.only(serverPlayer),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                             messageId, serverPlayer, unit)
                .addStringTemplate("%nation%", settlement.getOwner().getNationName()));

        // Others can see missionary disappear and settlement acquire
        // mission.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Incite a settlement against an enemy.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is inciting.
     * @param unit The missionary <code>Unit</code> inciting.
     * @param settlement The <code>IndianSettlement</code> to incite.
     * @param enemy The <code>Player</code> to be incited against.
     * @param gold The amount of gold in the bribe.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element incite(ServerPlayer serverPlayer, Unit unit,
                          IndianSettlement settlement,
                          Player enemy, int gold) {
        ChangeSet cs = new ChangeSet();

        // How much gold will be needed?
        settlement.makeContactSettlement(serverPlayer);
        ServerPlayer enemyPlayer = (ServerPlayer) enemy;
        Player nativePlayer = settlement.getOwner();
        int payingValue = nativePlayer.getTension(serverPlayer).getValue();
        int targetValue = nativePlayer.getTension(enemyPlayer).getValue();
        int goldToPay = (payingValue > targetValue) ? 10000 : 5000;
        goldToPay += 20 * (payingValue - targetValue);
        goldToPay = Math.max(goldToPay, 650);

        // Try to incite?
        if (gold < 0) { // Initial enquiry.
            cs.addAttribute(See.only(serverPlayer),
                            "gold", Integer.toString(goldToPay));
        } else if (gold < goldToPay || serverPlayer.getGold() < gold) {
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                 "indianSettlement.inciteGoldFail",
                                 serverPlayer, settlement)
                    .addStringTemplate("%player%", enemyPlayer.getNationName())
                    .addAmount("%amount%", goldToPay));
            cs.addAttribute(See.only(serverPlayer), "gold", "0");
            unit.setMovesLeft(0);
            cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
        } else {
            // Success.  Raise the tension for the native player with respect
            // to the european player.  Let resulting stance changes happen
            // naturally in the AI player turn/s.
            cs.add(See.only(null).perhaps(enemyPlayer),
                   nativePlayer.modifyTension(enemyPlayer,
                                              Tension.WAR_MODIFIER));
            cs.add(See.only(null).perhaps(serverPlayer),
                   enemyPlayer.modifyTension(serverPlayer,
                                             Tension.TENSION_ADD_WAR_INCITER));
            cs.addAttribute(See.only(serverPlayer),
                            "gold", Integer.toString(gold));
            serverPlayer.modifyGold(-gold);
            nativePlayer.modifyGold(gold);
            cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
            unit.setMovesLeft(0);
            cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
        }

        // Others might include enemy.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }


    /**
     * Set a unit destination.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param unit The <code>Unit</code> to set the destination for.
     * @param destination The <code>Location</code> to set as destination.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element setDestination(ServerPlayer serverPlayer, Unit unit,
                                  Location destination) {
        if (unit.getTradeRoute() != null) unit.setTradeRoute(null);
        unit.setDestination(destination);

        // Others can not see a destination change.
        return new ChangeSet().add(See.only(serverPlayer), unit)
            .build(serverPlayer);
    }


    /**
     * Is there work for a unit to do at a stop?
     *
     * @param unit The <code>Unit</code> to check.
     * @param stop The <code>Stop</code> to test.
     * @return True if the unit should load or unload cargo at the stop.
     */
    private boolean hasWorkAtStop(Unit unit, Stop stop) {
        ArrayList<GoodsType> stopGoods = stop.getCargo();
        int cargoSize = stopGoods.size();
        for (Goods goods : unit.getGoodsList()) {
            GoodsType type = goods.getType();
            if (stopGoods.contains(type)) {
                if (unit.getLoadableAmount(type) > 0) {
                    // There is space on the unit to load some more
                    // of this goods type, so return true if there is
                    // some available at the stop.
                    Location loc = stop.getLocation();
                    if (loc instanceof Colony) {
                        if (((Colony) loc).getExportAmount(type) > 0) {
                            return true;
                        }
                    } else if (loc instanceof Europe) {
                        return true;
                    }
                } else {
                    cargoSize--; // No room for more of this type.
                }
            } else {
                return true; // This type should be unloaded here.
            }
        }

        // Return true if there is space left, and something to load.
        return unit.getSpaceLeft() > 0 && cargoSize > 0;
    }

    /**
     * Set current stop of a unit to the next valid stop if any.
     *
     * @param serverPlayer The <code>ServerPlayer</code> the unit belongs to.
     * @param unit The <code>Unit</code> to update.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element updateCurrentStop(ServerPlayer serverPlayer, Unit unit) {
        // Check if there is a valid current stop?
        int current = unit.validateCurrentStop();
        if (current < 0) return null; // No valid stop.

        ArrayList<Stop> stops = unit.getTradeRoute().getStops();
        int next = current;
        for (;;) {
            if (++next >= stops.size()) next = 0;
            if (next == current) break;
            if (hasWorkAtStop(unit, stops.get(next))) break;
        }

        // Next is the updated stop.
        // Could do just a partial update of currentStop if we did not
        // also need to set the unit destination.
        unit.setCurrentStop(next);
        unit.setDestination(stops.get(next).getLocation());

        // Others can not see a stop change.
        return new ChangeSet().add(See.only(serverPlayer), unit)
            .build(serverPlayer);
    }

    /**
     * Move goods from current location to another.
     *
     * @param goods The <code>Goods</code> to move.
     * @param loc The new <code>Location</code>.
     */
    public void moveGoods(Goods goods, Location loc)
        throws IllegalStateException {
        Location oldLoc = goods.getLocation();
        if (oldLoc == null) {
            throw new IllegalStateException("Goods in null location.");
        } else if (loc == null) {
            ; // Dumping is allowed
        } else if (loc instanceof Unit) {
            if (((Unit) loc).isInEurope()) {
                if (!(oldLoc instanceof Unit && ((Unit) oldLoc).isInEurope())) {
                    throw new IllegalStateException("Goods and carrier not both in Europe.");
                }
            } else if (loc.getTile() == null) {
                throw new IllegalStateException("Carrier not on the map.");
            } else if (oldLoc instanceof IndianSettlement) {
                // Can not be co-located when buying from natives.
            } else if (loc.getTile() != oldLoc.getTile()) {
                throw new IllegalStateException("Goods and carrier not co-located.");
            }
        } else if (loc instanceof IndianSettlement) {
            // Can not be co-located when selling to natives.
        } else if (loc instanceof Colony) {
            if (oldLoc instanceof Unit
                && ((Unit) oldLoc).getOwner() != ((Colony) loc).getOwner()) {
                // Gift delivery
            } else if (loc.getTile() != oldLoc.getTile()) {
                throw new IllegalStateException("Goods and carrier not both in Colony.");
            }
        } else if (loc.getGoodsContainer() == null) {
            throw new IllegalStateException("New location with null GoodsContainer.");
        }

        oldLoc.remove(goods);
        goods.setLocation(null);

        if (loc != null) {
            loc.add(goods);
            goods.setLocation(loc);
        }
    }

    /**
     * Buy from a settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is buying.
     * @param unit The <code>Unit</code> that will carry the goods.
     * @param settlement The <code>IndianSettlement</code> to buy from.
     * @param goods The <code>Goods</code> to buy.
     * @param amount How much gold to pay.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element buyFromSettlement(ServerPlayer serverPlayer, Unit unit,
                                     IndianSettlement settlement,
                                     Goods goods, int amount) {
        settlement.makeContactSettlement(serverPlayer);
        if (!isTransactionSessionOpen(unit, settlement)) {
            return Message.clientError("Trying to buy without opening a transaction session");
        }
        TransactionSession session = getTransactionSession(unit, settlement);
        if (!(Boolean) session.get("canBuy")) {
            return Message.clientError("Trying to buy in a session where buying is not allowed.");
        }
        if (unit.getSpaceLeft() <= 0) {
            return Message.clientError("Unit is full, unable to buy.");
        }
        // Check that this is the agreement that was made
        AIPlayer ai = (AIPlayer) getFreeColServer().getAIMain().getAIObject(settlement.getOwner());
        int returnGold = ai.buyProposition(unit, settlement, goods, amount);
        if (returnGold != amount) {
            return Message.clientError("This was not the price we agreed upon! Cheater?");
        }
        // Check this is funded.
        if (serverPlayer.getGold() < amount) {
            return Message.clientError("Insufficient gold to buy.");
        }

        ChangeSet cs = new ChangeSet();

        // Valid, make the trade.
        moveGoods(goods, unit);
        cs.add(See.perhaps(), unit);

        Player settlementPlayer = settlement.getOwner();
        settlement.updateWantedGoods();
        settlement.getTile().updateIndianSettlementInformation(serverPlayer);
        cs.add(See.only(serverPlayer),
            settlement.modifyAlarm(serverPlayer, -amount / 50));
        settlementPlayer.modifyGold(amount);
        serverPlayer.modifyGold(-amount);
        cs.add(See.only(serverPlayer), settlement);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        session.put("actionTaken", true);
        session.put("canBuy", false);

        // Others can see the unit capacity.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Sell to a settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is selling.
     * @param unit The <code>Unit</code> carrying the goods.
     * @param settlement The <code>IndianSettlement</code> to sell to.
     * @param goods The <code>Goods</code> to sell.
     * @param amount How much gold to expect.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element sellToSettlement(ServerPlayer serverPlayer, Unit unit,
                                    IndianSettlement settlement,
                                    Goods goods, int amount) {
        settlement.makeContactSettlement(serverPlayer);
        if (!isTransactionSessionOpen(unit, settlement)) {
            return Message.clientError("Trying to sell without opening a transaction session");
        }
        TransactionSession session = getTransactionSession(unit, settlement);
        if (!(Boolean) session.get("canSell")) {
            return Message.clientError("Trying to sell in a session where selling is not allowed.");
        }

        // Check that the gold is the agreed amount
        AIPlayer ai = (AIPlayer) getFreeColServer().getAIMain().getAIObject(settlement.getOwner());
        int returnGold = ai.sellProposition(unit, settlement, goods, amount);
        if (returnGold != amount) {
            return Message.clientError("This was not the price we agreed upon! Cheater?");
        }

        ChangeSet cs = new ChangeSet();

        // Valid, make the trade.
        moveGoods(goods, settlement);
        cs.add(See.perhaps(), unit);

        Player settlementPlayer = settlement.getOwner();
        settlementPlayer.modifyGold(-amount);
        cs.add(See.only(serverPlayer), settlement.modifyAlarm(serverPlayer,
                -settlement.getPrice(goods) / 500));
        serverPlayer.modifyGold(amount);
        settlement.updateWantedGoods();
        settlement.getTile().updateIndianSettlementInformation(serverPlayer);
        cs.add(See.only(serverPlayer), settlement);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        session.put("actionTaken", true);
        session.put("canSell", false);
        cs.addSale(serverPlayer, settlement, goods.getType(),
                   (int) Math.round((float) amount / goods.getAmount()));

        // Others can see the unit capacity.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Deliver gift to settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is delivering.
     * @param unit The <code>Unit</code> that is delivering.
     * @param goods The <code>Goods</code> to deliver.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element deliverGiftToSettlement(ServerPlayer serverPlayer,
                                           Unit unit, Settlement settlement,
                                           Goods goods) {
        if (!isTransactionSessionOpen(unit, settlement)) {
            return Message.clientError("Trying to deliverGift without opening a transaction session");
        }

        ChangeSet cs = new ChangeSet();
        TransactionSession session = getTransactionSession(unit, settlement);

        Tile tile = settlement.getTile();
        moveGoods(goods, settlement);
        cs.add(See.perhaps(), unit);
        if (settlement instanceof IndianSettlement) {
            IndianSettlement indianSettlement = (IndianSettlement) settlement;
            indianSettlement.makeContactSettlement(serverPlayer);
            cs.add(See.only(serverPlayer),
                   indianSettlement.modifyAlarm(serverPlayer,
                   -indianSettlement.getPrice(goods) / 50));
            indianSettlement.updateWantedGoods();
            tile.updateIndianSettlementInformation(serverPlayer);
            cs.add(See.only(serverPlayer), settlement);
        }
        session.put("actionTaken", true);
        session.put("canGift", false);

        // Inform the receiver of the gift.
        ServerPlayer receiver = (ServerPlayer) settlement.getOwner();
        if (!receiver.isAI() && receiver.isConnected()
            && settlement instanceof Colony) {
            cs.add(See.only(receiver), unit);
            cs.add(See.only(receiver), settlement);
            cs.addMessage(See.only(receiver),
                new ModelMessage(ModelMessage.MessageType.GIFT_GOODS,
                                 "model.unit.gift", settlement, goods.getType())
                    .addStringTemplate("%player%", serverPlayer.getNationName())
                    .add("%type%", goods.getNameKey())
                    .addAmount("%amount%", goods.getAmount())
                    .addName("%colony%", settlement.getName()));
        }
        logger.info("Gift delivered by unit: " + unit.getId()
                    + " to settlement: " + settlement.getName());

        // Others can see unit capacity, receiver gets it own items.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }


    /**
     * Load cargo.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is loading.
     * @param unit The <code>Unit</code> to load.
     * @param goods The <code>Goods</code> to load.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element loadCargo(ServerPlayer serverPlayer, Unit unit,
                             Goods goods) {
        ChangeSet cs = new ChangeSet();

        goods.adjustAmount();
        moveGoods(goods, unit);
        if (unit.getInitialMovesLeft() != unit.getMovesLeft()) {
            unit.setMovesLeft(0);
        }

        // Only have to update the carrier location, as that *must*
        // include the original location of the goods.
        cs.add(See.only(serverPlayer),
               (FreeColGameObject) unit.getLocation());
        cs.add(See.perhaps().except(serverPlayer), unit);

        // Others might see capacity change.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Unload cargo.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is unloading.
     * @param unit The <code>Unit</code> to unload.
     * @param goods The <code>Goods</code> to unload.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element unloadCargo(ServerPlayer serverPlayer, Unit unit,
                               Goods goods) {
        ChangeSet cs = new ChangeSet();

        Location loc;
        if (unit.isInEurope()) { // Must be a dump of boycotted goods
            loc = null;
        } else if (unit.getTile() == null) {
            return Message.clientError("Unit not on the map.");
        } else if (unit.getTile().getSettlement() instanceof Colony) {
            loc = unit.getTile().getSettlement();
        } else { // Dump of goods onto a tile
            loc = null;
        }
        goods.adjustAmount();
        moveGoods(goods, loc);
        if (unit.getInitialMovesLeft() != unit.getMovesLeft()) {
            unit.setMovesLeft(0);
        }

        if (loc instanceof Settlement) {
            cs.add(See.only(serverPlayer), (FreeColGameObject) loc);
        }
        // Always update unit, to show goods are gone.
        cs.add(See.perhaps(), unit);

        // Others might see a capacity change.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }


    /**
     * Clear the specialty of a unit.
     *
     * @param serverPlayer The owner of the unit.
     * @param unit The <code>Unit</code> to clear the speciality of.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element clearSpeciality(ServerPlayer serverPlayer, Unit unit) {
        UnitType newType = unit.getTypeChange(ChangeType.CLEAR_SKILL,
                                              serverPlayer);
        if (newType == null) {
            return Message.clientError("Can not clear unit speciality: "
                                       + unit.getId());
        }
        // There can be some restrictions that may prevent the
        // clearing of the speciality.  For example, teachers cannot
        // not be cleared of their speciality.
        Location oldLocation = unit.getLocation();
        if (oldLocation instanceof Building
            && !((Building) oldLocation).canAdd(newType)) {
            return Message.clientError("Cannot clear speciality, building does not allow new unit type");
        }

        // Valid, change type.
        unit.setType(newType);

        // Update just the unit, others can not see it as this only happens
        // in-colony.  TODO: why not clear speciality in the open,
        // you can disband...
        return new ChangeSet().add(See.only(serverPlayer), unit)
            .build(serverPlayer);
    }


    /**
     * Disband a unit.
     * 
     * @param serverPlayer The owner of the unit.
     * @param unit The <code>Unit</code> to disband.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element disbandUnit(ServerPlayer serverPlayer, Unit unit) {
        ChangeSet cs = new ChangeSet();

        // Dispose of the unit.
        cs.add(See.perhaps().always(serverPlayer),
               (FreeColGameObject) unit.getLocation());
        cs.addDispose(serverPlayer, unit.getLocation(), unit);

        // Others can see the unit removal and the space it leaves.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }


    /**
     * Generates a skill that could be taught from a settlement on the
     * given Tile.
     *
     * @param map The <code>Map</code>.
     * @param tile The <code>Tile</code> where the settlement will be located.
     * @param nationType The <code>IndianNationType</code> teaching.
     * @return A skill that can be taught to Europeans.
     */
    private UnitType generateSkillForLocation(Map map, Tile tile,
                                              IndianNationType nationType) {
        List<RandomChoice<UnitType>> skills = nationType.getSkills();
        java.util.Map<GoodsType, Integer> scale
            = new HashMap<GoodsType, Integer>();

        for (RandomChoice<UnitType> skill : skills) {
            scale.put(skill.getObject().getExpertProduction(), 1);
        }

        for (Tile t: tile.getSurroundingTiles(1)) {
            for (GoodsType goodsType : scale.keySet()) {
                scale.put(goodsType, scale.get(goodsType).intValue()
                          + t.potential(goodsType, null));
            }
        }

        List<RandomChoice<UnitType>> scaledSkills
            = new ArrayList<RandomChoice<UnitType>>();
        for (RandomChoice<UnitType> skill : skills) {
            UnitType unitType = skill.getObject();
            int scaleValue = scale.get(unitType.getExpertProduction()).intValue();
            scaledSkills.add(new RandomChoice<UnitType>(unitType, skill.getProbability() * scaleValue));
        }

        UnitType skill = RandomChoice.getWeightedRandom(random, scaledSkills);
        if (skill == null) {
            // Seasoned Scout
            Specification spec = getGame().getSpecification();
            List<UnitType> unitList
                = spec.getUnitTypesWithAbility("model.ability.expertScout");
            return unitList.get(random.nextInt(unitList.size()));
        }
        return skill;
    }

    /**
     * Build a settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is building.
     * @param unit The <code>Unit</code> that is building.
     * @param name The new settlement name.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element buildSettlement(ServerPlayer serverPlayer, Unit unit,
                                   String name) {
        ChangeSet cs = new ChangeSet();
        Game game = serverPlayer.getGame();

        // Build settlement
        Tile tile = unit.getTile();
        Settlement settlement;
        if (Player.ASSIGN_SETTLEMENT_NAME.equals(name)) {
            name = serverPlayer.getSettlementName();
            if (Player.ASSIGN_SETTLEMENT_NAME.equals(name)) {
                // Load settlement names on demand.
                serverPlayer.installSettlementNames(Messages
                        .getSettlementNames(serverPlayer), random);
                name = serverPlayer.getSettlementName();
            }
        }
        if (serverPlayer.isEuropean()) {
            settlement = new Colony(game, serverPlayer, name, tile);
        } else {
            IndianNationType nationType
                = (IndianNationType) serverPlayer.getNationType();
            UnitType skill = generateSkillForLocation(game.getMap(), tile,
                                                      nationType);
            settlement = new IndianSettlement(game, serverPlayer, tile,
                                              name, false, skill,
                                              new HashSet<Player>(), null);
            // TODO: its lame that the settlement starts with no contacts
        }
        settlement.placeSettlement();

        // Join.
        unit.setState(UnitState.IN_COLONY);
        unit.setLocation(settlement);
        unit.setMovesLeft(0);

        // Update with settlement tile, and newly owned tiles.
        List<FreeColGameObject> tiles = new ArrayList<FreeColGameObject>();
        tiles.addAll(settlement.getOwnedTiles());
        cs.add(See.perhaps(), tiles);

        cs.addHistory(serverPlayer,
            new HistoryEvent(game.getTurn(),
                             HistoryEvent.EventType.FOUND_COLONY)
                .addName("%colony%", settlement.getName()));

        // Also send any tiles that can now be seen because the colony
        // can perhaps see further than the founding unit.
        if (settlement.getLineOfSight() > unit.getLineOfSight()) {
            tiles.clear();
            for (Tile t : tile.getSurroundingTiles(unit.getLineOfSight()+1,
                                                  settlement.getLineOfSight())) {
                if (!tiles.contains(t)) tiles.add(t);
            }
            cs.add(See.only(serverPlayer), tiles);
        }

        // Others can see tile changes.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Join a colony.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param unit The <code>Unit</code> that is joining.
     * @param colony The <code>Colony</code> to join.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element joinColony(ServerPlayer serverPlayer, Unit unit,
                              Colony colony) {
        ChangeSet cs = new ChangeSet();
        List<Tile> ownedTiles = colony.getOwnedTiles();
        Tile tile = colony.getTile();

        // Join.
        unit.setState(UnitState.IN_COLONY);
        unit.setLocation(colony);
        unit.setMovesLeft(0);

        // Update with colony tile, and tiles now owned.
        cs.add(See.only(serverPlayer), tile);
        for (Tile t : tile.getSurroundingTiles(colony.getRadius())) {
            if (t.getOwningSettlement() == colony && !ownedTiles.contains(t)) {
                cs.add(See.perhaps(), t);
            }
        }

        // Others might see a tile ownership change.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }


    /**
     * Abandon a settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is abandoning.
     * @param settlement The <code>Settlement</code> to abandon.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element abandonSettlement(ServerPlayer serverPlayer,
                                     Settlement settlement) {
        ChangeSet cs = new ChangeSet();

        // Collect the tiles the settlement owns before disposing,
        // except the center tile as that is in the dispose below.
        for (Tile t : settlement.getOwnedTiles()) {
            if (t != settlement.getTile()) cs.add(See.perhaps(), t);
        }

        // Create history event before disposing.
        if (settlement instanceof Colony) {
            cs.addHistory(serverPlayer,
                new HistoryEvent(getGame().getTurn(),
                                 HistoryEvent.EventType.ABANDON_COLONY)
                    .addName("%colony%", settlement.getName()));
        }

        // Now do the dispose.
        cs.add(See.perhaps().always(serverPlayer), settlement.getTile());
        cs.addDispose(serverPlayer, settlement.getTile(), settlement);

        // TODO: Player.settlements is still being fixed on the client side.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }


    /**
     * Claim land.
     *
     * @param serverPlayer The <code>ServerPlayer</code> claiming.
     * @param tile The <code>Tile</code> to claim.
     * @param settlement The <code>Settlement</code> to claim for.
     * @param price The price to pay for the land, which must agree
     *     with the owner valuation, unless negative which denotes stealing.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element claimLand(ServerPlayer serverPlayer, Tile tile,
                             Settlement settlement, int price) {
        ChangeSet cs = new ChangeSet();
        csClaimLand(serverPlayer, tile, settlement, price, cs);

        // Others can see the tile.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Claim land.
     *
     * @param serverPlayer The <code>ServerPlayer</code> claiming.
     * @param tile The <code>Tile</code> to claim.
     * @param settlement The <code>Settlement</code> to claim for.
     * @param price The price to pay for the land, which must agree
     *     with the owner valuation, unless negative which denotes stealing.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csClaimLand(ServerPlayer serverPlayer, Tile tile,
                             Settlement settlement, int price, ChangeSet cs) {
        Player owner = tile.getOwner();
        Settlement ownerSettlement = tile.getOwningSettlement();
        tile.setOwningSettlement(settlement);
        tile.setOwner(serverPlayer);
        tile.updatePlayerExploredTiles();

        // Update the tile for all, and privately any now-angrier
        // owners, or the player gold if a price was paid.
        cs.add(See.perhaps(), tile);
        if (price > 0) {
            serverPlayer.modifyGold(-price);
            owner.modifyGold(price);
            cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        } else if (price < 0 && owner.isIndian()) {
            IndianSettlement is = (IndianSettlement) ownerSettlement;
            is.makeContactSettlement(serverPlayer);
            cs.add(See.only(null).perhaps(serverPlayer),
                   owner.modifyTension(serverPlayer,
                                       Tension.TENSION_ADD_LAND_TAKEN, is));
        }
    }

    /**
     * Accept a diplomatic trade.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is trading.
     * @param other The other <code>ServerPlayer</code> that is trading.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> to trade with.
     * @param agreement The <code>DiplomaticTrade</code> to consider.
     * @return An <code>Element</code> encapsulating the changes.
     */
    private Element acceptTrade(ServerPlayer serverPlayer, ServerPlayer other,
                                Unit unit, Settlement settlement,
                                DiplomaticTrade agreement) {
        TransactionSession.forget(unit, settlement);

        ChangeSet cs = new ChangeSet();

        cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
        for (TradeItem tradeItem : agreement.getTradeItems()) {
            // Check trade carefully before committing.
            if (!tradeItem.isValid()) {
                logger.warning("Trade with invalid tradeItem: "
                               + tradeItem.toString());
                continue;
            }
            ServerPlayer source = (ServerPlayer) tradeItem.getSource();
            if (source != serverPlayer && source != other) {
                logger.warning("Trade with invalid source: "
                               + ((source == null) ? "null" : source.getId()));
                continue;
            }
            ServerPlayer dest = (ServerPlayer) tradeItem.getDestination();
            if (dest != serverPlayer && dest != other) {
                logger.warning("Trade with invalid destination: "
                               + ((dest == null) ? "null" : dest.getId()));
                continue;
            }

            // Collect changes for updating.  Not very OO but
            // TradeItem should not know about server internals.
            // Take care to show items that change hands to the *old*
            // owner too.
            Stance stance = tradeItem.getStance();
            if (stance != null
                && !csChangeStance(serverPlayer, stance, other, true, cs)) {
                logger.warning("Stance trade failure");
            }
            Colony colony = tradeItem.getColony();
            if (colony != null) {
                ServerPlayer former = (ServerPlayer) colony.getOwner();
                colony.changeOwner(tradeItem.getDestination());
                List<FreeColGameObject> tiles = new ArrayList<FreeColGameObject>();
                tiles.addAll(colony.getOwnedTiles());
                cs.add(See.perhaps().always(former), tiles);
            }
            int gold = tradeItem.getGold();
            if (gold > 0) {
                tradeItem.getSource().modifyGold(-gold);
                tradeItem.getDestination().modifyGold(gold);
                cs.addPartial(See.only(serverPlayer), serverPlayer,
                                  "gold", "score");
                cs.addPartial(See.only(other), other, "gold", "score");
            }
            Goods goods = tradeItem.getGoods();
            if (goods != null) {
                moveGoods(goods, settlement);
                cs.add(See.only(serverPlayer), unit);
                cs.add(See.only(other), settlement);
            }
            Unit newUnit = tradeItem.getUnit();
            if (newUnit != null) {
                ServerPlayer former = (ServerPlayer) newUnit.getOwner();
                unit.setOwner(tradeItem.getDestination());
                cs.add(See.perhaps().always(former), newUnit);
            }
        }

        // Original player also sees conclusion of diplomacy.
        sendToOthers(serverPlayer, cs);
        cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
               new DiplomacyMessage(unit, settlement, agreement));
        return cs.build(serverPlayer);
    }

    /**
     * Reject a diplomatic trade.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is trading.
     * @param other The other <code>ServerPlayer</code> that is trading.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> to trade with.
     * @param agreement The <code>DiplomaticTrade</code> to consider.
     * @return An <code>Element</code> encapsulating the changes.
     */
    private Element rejectTrade(ServerPlayer serverPlayer, ServerPlayer other,
                                Unit unit, Settlement settlement,
                                DiplomaticTrade agreement) {
        ChangeSet cs = new ChangeSet();

        TransactionSession.forget(unit, settlement);
        cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
        cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
               new DiplomacyMessage(unit, settlement, agreement));
        return cs.build(serverPlayer);
    }

    /**
     * Diplomatic trades.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is trading.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> to trade with.
     * @param agreement The <code>DiplomaticTrade</code> to consider.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element diplomaticTrade(ServerPlayer serverPlayer, Unit unit,
                                   Settlement settlement,
                                   DiplomaticTrade agreement) {
        TransactionSession session;
        DiplomaticTrade current;
        ServerPlayer other = (ServerPlayer) settlement.getOwner();
        unit.setMovesLeft(0);

        switch (agreement.getStatus()) {
        case ACCEPT_TRADE:
            session = TransactionSession.lookup(unit, settlement);
            if (session == null) {
                return Message.clientError("Accepting without open session.");
            }
            // Act on what was proposed, not what is in the accept
            // message to frustrate tricksy client changing the conditions.
            current = (DiplomaticTrade) session.get("agreement");
            current.setStatus(TradeStatus.ACCEPT_TRADE);

            sendElement(other, new ChangeSet().add(See.only(other),
                    ChangePriority.CHANGE_LATE,
                    new DiplomacyMessage(unit, settlement, current)));
            return acceptTrade(serverPlayer, other, unit, settlement, current);

        case REJECT_TRADE:
            session = TransactionSession.lookup(unit, settlement);
            if (session == null) {
                return Message.clientError("Rejecting without open session.");
            }
            current = (DiplomaticTrade) session.get("agreement");
            current.setStatus(TradeStatus.REJECT_TRADE);

            sendElement(other, new ChangeSet().add(See.only(other),
                    ChangePriority.CHANGE_LATE,
                    new DiplomacyMessage(unit, settlement, current)));
            return rejectTrade(serverPlayer, other, unit, settlement, current);

        case PROPOSE_TRADE:
            current = agreement;
            session = TransactionSession.find(unit, settlement);
            session.put("agreement", agreement);

            // If the unit is on a carrier we need to update the
            // client with it first as the diplomacy message refers to it.
            // Ask the other player about this proposal.
            ChangeSet cs = new ChangeSet();
            cs.add(See.only(other), ChangePriority.CHANGE_LATE,
                   new DiplomacyMessage(unit, settlement, agreement));
            if (!unit.isVisibleTo(other)) {
                cs.add(See.only(other), unit);
            }
            Element response = askElement(other, cs);

            // What did they think?
            DiplomacyMessage diplomacy = (response == null) ? null
                : new DiplomacyMessage(getGame(), response);
            agreement = (diplomacy == null) ? null : diplomacy.getAgreement();
            TradeStatus status = (agreement == null) ? TradeStatus.REJECT_TRADE
                : agreement.getStatus();
            switch (status) {
            case ACCEPT_TRADE:
                // Act on the proposed agreement, not what was passed back
                // as accepted.
                current.setStatus(TradeStatus.ACCEPT_TRADE);
                return acceptTrade(serverPlayer, other, unit, settlement,
                                   current);

            case PROPOSE_TRADE:
                // Save the counter-proposal, sanity test, then pass back.
                if ((ServerPlayer) agreement.getSender() == serverPlayer
                    && (ServerPlayer) agreement.getRecipient() == other) {
                    session.put("agreement", agreement);
                    return new ChangeSet().add(See.only(serverPlayer),
                                               ChangePriority.CHANGE_LATE,
                                               diplomacy)
                        .build(serverPlayer);
                }
                logger.warning("Trade counter-proposal was incompatible.");
                // Fall through

            case REJECT_TRADE:
            default:
                // Reject the current trade.
                current.setStatus(TradeStatus.REJECT_TRADE);
                return rejectTrade(serverPlayer, other, unit, settlement,
                                   current);
            }

        default:
            return Message.clientError("Bogus trade");
        }
    }

    /**
     * Spy on a settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is spying.
     * @param unit The <code>Unit</code> that is spying.
     * @param settlement The <code>Settlement</code> to spy on.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element spySettlement(ServerPlayer serverPlayer, Unit unit,
                                 Settlement settlement) {
        ChangeSet cs = new ChangeSet();

        unit.setMovesLeft(0);
        cs.addPartial(See.only(serverPlayer), unit, "movesLeft");

        // Spying is private.
        // Have to tack on the settlement.
        // TODO: eliminate the explicit Element hackery
        Element reply = cs.build(serverPlayer);
        Element child = settlement.toXMLElement(serverPlayer,
                                                reply.getOwnerDocument(),
                                                true, false);
        reply.appendChild(child);
        return reply;
    }

    /**
     * Change work location.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param unit The <code>Unit</code> to change the work location of.
     * @param workLocation The <code>WorkLocation</code> to change to.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element work(ServerPlayer serverPlayer, Unit unit,
                        WorkLocation workLocation) {
        ChangeSet cs = new ChangeSet();

        if (workLocation instanceof ColonyTile) {
            Tile tile = ((ColonyTile) workLocation).getWorkTile();
            Colony colony = workLocation.getColony();
            if (tile.getOwningSettlement() != colony) {
                // Claim known free land (because canAdd() succeeded).
                csClaimLand(serverPlayer, tile, colony, 0, cs);
            }
        }

        // Change the location.
        Location oldLocation = unit.getLocation();
        unit.setState(UnitState.IN_COLONY);
        unit.setLocation(workLocation);
        cs.add(See.perhaps(), (FreeColGameObject) unit.getLocation(),
               (FreeColGameObject) oldLocation);

        // Others can see colony change size
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Loot cargo.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the winner.
     * @param winner The <code>Unit</code> that looting.
     * @param loserId The id of the <code>Unit</code> that is looted.
     * @param loot The <code>Goods</code> to loot.
     * @return An <code>Element</code> encapsulating this action.
     */
    @SuppressWarnings("unchecked")
    public Element lootCargo(ServerPlayer serverPlayer, Unit winner,
                             String loserId, List<Goods> loot) {
        TransactionSession ts = TransactionSession.lookup(winner.getId(),
                                                          loserId);
        if (ts == null) {
            return Message.clientError("Bogus looting!");
        }
        if (winner.getSpaceLeft() == 0) {
            return Message.clientError("No space to loot to: "
                                       + winner.getId());
        }

        ChangeSet cs = new ChangeSet();
        List<Goods> available = (ArrayList<Goods>) ts.get("lootCargo");
        if (loot == null) { // Initial inquiry
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                   new LootCargoMessage(winner, loserId, available));
        } else {
            TransactionSession.forget(winner.getId(), loserId);
            for (Goods g : loot) {
                if (!available.contains(g)) {
                    return Message.clientError("Invalid loot: " + g.toString());
                }
                available.remove(g);
                if (!winner.canAdd(g)) {
                    return Message.clientError("Loot failed: " + g.toString());
                }
                winner.add(g);
            }

            // Others can see cargo capacity change.
            cs.add(See.perhaps(), winner);
            sendToOthers(serverPlayer, cs);
        }
        return cs.build(serverPlayer);
    }
}
