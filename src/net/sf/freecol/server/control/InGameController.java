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

package net.sf.freecol.server.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Europe.MigrationType;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.HighSeas;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Market.Access;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.mission.GoToMission;
import net.sf.freecol.common.model.mission.ImprovementMission;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.RandomRange;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TradeRoute.Stop;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitLocation;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.GoodsForSaleMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.MonarchActionMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.REFAIPlayer;
import net.sf.freecol.server.control.ChangeSet.ChangePriority;
import net.sf.freecol.server.control.ChangeSet.See;
import net.sf.freecol.server.model.DemandSession;
import net.sf.freecol.server.model.DiplomacySession;
import net.sf.freecol.server.model.LootSession;
import net.sf.freecol.server.model.MercenariesSession;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerEurope;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.server.model.TaxSession;
import net.sf.freecol.server.model.TradeSession;
import net.sf.freecol.server.model.TransactionSession;

import org.w3c.dom.Element;


/**
 * The main server controller.
 */
public final class InGameController extends Controller {

    private static Logger logger = Logger.getLogger(InGameController.class.getName());

    // TODO: options, spec?
    // Alarm adjustments.
    public static final int ALARM_NEW_MISSIONARY = -100;

    // Score bonus on declaration of independence.
    public static final int SCORE_INDEPENDENCE_DECLARED = 100;

    // Score bonus on achieving independence.
    public static final int SCORE_INDEPENDENCE_GRANTED = 1000;

    // The server random number source.
    private final Random random;

    // Debug helpers
    private int debugOnlyAITurns = 0;
    private MonarchAction debugMonarchAction = null;
    private ServerPlayer debugMonarchPlayer = null;


    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     * @param random The pseudo-random number source to use.
     */
    public InGameController(FreeColServer freeColServer, Random random) {
        super(freeColServer);

        this.random = random;
    }

    /**
     * Gets the number of AI turns to skip through.
     *
     * @return The number of terms to skip.
     */
    public int getSkippedTurns() {
        return (FreeCol.isInDebugMode()) ? debugOnlyAITurns : -1;
    }

    /**
     * Sets the number of AI turns to skip through as a debug helper.
     *
     * @param turns The number of turns to skip through.
     */
    public void setSkippedTurns(int turns) {
        if (FreeCol.isInDebugMode()) {
            debugOnlyAITurns = turns;
        }
    }

    /**
     * Sets a monarch action to debug/test.
     *
     * @param player The <code>Player</code> whose monarch should act.
     * @param action The <code>MonarchAction</code> to be taken.
     */
    public void setMonarchAction(Player player, MonarchAction action) {
        if (FreeCol.isInDebugMode()) {
            debugMonarchPlayer = (ServerPlayer) player;
            debugMonarchAction = action;
        }
    }

    /**
     * Debug convenience to step the random number generator.
     *
     * @return The next random number in series, in the range 0-99.
     */
    public int stepRandom() {
        return Utils.randomInt(logger, "step random", random, 100);
    }

    /**
     * Public version of the yearly goods adjust (public so it can be
     * use in the Market test code).  Sends the market and change
     * messages to the player.
     *
     * @param serverPlayer The <code>ServerPlayer</code> whose market
     *            is to be updated.
     */
    public void yearlyGoodsAdjust(ServerPlayer serverPlayer) {
        ChangeSet cs = new ChangeSet();
        serverPlayer.csYearlyGoodsAdjust(random, cs);
        sendElement(serverPlayer, cs);
    }

    /**
     * Public version of csAddFoundingFather so it can be used in the
     * test code and DebugMenu.
     *
     * @param serverPlayer The <code>ServerPlayer</code> who gains a father.
     * @param father The <code>FoundingFather</code> to add.
     */
    public void addFoundingFather(ServerPlayer serverPlayer,
                                  FoundingFather father) {
        ChangeSet cs = new ChangeSet();
        serverPlayer.csAddFoundingFather(father, random, cs);
        sendElement(serverPlayer, cs);
    }

    /**
     * Public change stance and inform all routine.  Mostly used in the
     * test suite, but the AIs also call it.
     *
     * @param player The originating <code>Player</code>.
     * @param stance The new <code>Stance</code>.
     * @param otherPlayer The <code>Player</code> wrt which the stance changes.
     * @param symmetric If true, change the otherPlayer stance as well.
     */
    public void changeStance(Player player, Stance stance,
                             Player otherPlayer, boolean symmetric) {
        ChangeSet cs = new ChangeSet();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        if (serverPlayer.csChangeStance(stance, otherPlayer, symmetric, cs)) {
            sendToAll(cs);
        }
    }

    /**
     * Gets a nation summary.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is querying.
     * @param player The <code>Player</code> to summarize.
     * @return An <code>Element</code> encapsulating this action.
     */
    public NationSummary getNationSummary(ServerPlayer serverPlayer,
                                          Player player) {
        return new NationSummary(player, serverPlayer);
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
            } else if (oldLoc instanceof Settlement) {
                // Can not be co-located when buying from natives, or
                // when natives are demanding goods from a colony.
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

        // Save state of the goods container/s, allowing simpler updates.
        oldLoc.getGoodsContainer().saveState();
        if (loc != null) loc.getGoodsContainer().saveState();

        oldLoc.remove(goods);
        goods.setLocation(null);

        if (loc != null) {
            loc.add(goods);
            goods.setLocation(loc);
        }
    }

    /**
     * Create the Royal Expeditionary Force player corresponding to
     * a given player that is about to rebel.
     * Public for the test suite.
     *
     * @param serverPlayer The <code>ServerPlayer</code> about to rebel.
     * @return The REF player.
     */
    public ServerPlayer createREFPlayer(ServerPlayer serverPlayer) {
        Nation refNation = serverPlayer.getNation().getRefNation();
        Monarch monarch = serverPlayer.getMonarch();
        ServerPlayer refPlayer = getFreeColServer().addAIPlayer(refNation);
        refPlayer.setEntryLocation(null); // Trigger initial placement routine
        Player.makeContact(serverPlayer, refPlayer); // Will change, setup only

        // Instantiate the REF in Europe
        List<Unit> landUnits
            = refPlayer.createUnits(monarch.getREFLandUnits());
        List<Unit> navalUnits
            = refPlayer.createUnits(monarch.getREFNavalUnits());
        List<Unit> unitsList = new ArrayList<Unit>();
        unitsList.addAll(navalUnits);
        unitsList.addAll(landUnits);

        // Embark the land units.  For all land units, find a naval
        // unit to carry it.  Fill greedily, so as if there is excess
        // naval capacity then the naval units at the end of the list
        // will tend to be empty or very lightly filled, allowing them
        // to defend the whole fleet at full strength against the
        // rebel navy.
        Collections.shuffle(navalUnits, random);
        Collections.shuffle(landUnits, random);
        for (Unit unit : landUnits) {
            for (Unit carrier : navalUnits) {
                if (unit.getSpaceTaken() <= carrier.getSpaceLeft()) {
                    unit.setLocation(carrier);
                    continue;
                }
            }
        }
        // Send the navy on its way
        for (Unit u : navalUnits) {
            u.setWorkLeft(1);
            u.setMission(new GoToMission(u, getGame().getMap()));
            u.setLocation(u.getOwner().getHighSeas());
        }

        return refPlayer;
    }


    // Client-server communication utilities

    /**
     * Get a list of all server players, optionally excluding supplied ones.
     *
     * @param serverPlayers The <code>ServerPlayer</code>s to exclude.
     * @return A list of all connected server players, with exclusions.
     */
    private List<ServerPlayer> getOtherPlayers(ServerPlayer... serverPlayers) {
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
    private void sendToOthers(ServerPlayer serverPlayer, Element element) {
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
    private void sendToList(List<ServerPlayer> serverPlayers, Element element) {
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
    private void sendElement(ServerPlayer serverPlayer, Element element) {
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
    private Element askElement(ServerPlayer serverPlayer, Element element) {
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
     * Speaks to a chief in a native settlement, but only if it is as
     * a result of a scout actually asking to speak to the chief, or
     * for other settlement-contacting events such as missionary
     * actions, demanding tribute, learning skills and trading if the
     * settlementActionsContactChief game option is enabled.
     * It is still unclear what Col1 did here.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is contacting
     *     the settlement.
     * @param is The <code>IndianSettlement</code> to contact.
     * @param scout True if this contact is due to a scout asking to
     *     speak to the chief.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csSpeakToChief(ServerPlayer serverPlayer, IndianSettlement is,
                                boolean scout, ChangeSet cs) {
        serverPlayer.csContact((ServerPlayer) is.getOwner(), null, cs);
        is.makeContactSettlement(serverPlayer);
        if (scout || getGame().getSpecification()
            .getBooleanOption("model.option.settlementActionsContactChief")
            .getValue()) {
            is.setSpokenToChief(serverPlayer);
        }
    }

    // Routines that follow implement the controller response to
    // messages.
    // The convention is to return an element to be passed back to the
    // client by the invoking message handler.

    /**
     * Ends the turn of the given player.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to end the turn of.
     * @return Null.
     */
    public Element endTurn(ServerPlayer serverPlayer) {
        FreeColServer freeColServer = getFreeColServer();
        ServerGame game = getGame();
        ServerPlayer player = (ServerPlayer) game.getCurrentPlayer();

        if (serverPlayer != player) {
            throw new IllegalArgumentException("It is not "
                + serverPlayer.getName() + "'s turn, it is "
                + ((player == null) ? "noone" : player.getName()) + "'s!");
        }

        for (;;) {
            player.clearModelMessages();

            // Has anyone won?
            // Do not end single player games where an AI has won,
            // that would stop revenge mode.
            Player winner = game.checkForWinner();
            if (winner != null
                && !(freeColServer.isSingleplayer() && winner.isAI())) {
                ChangeSet cs = new ChangeSet();
                cs.addTrivial(See.all(), "gameEnded",
                              ChangePriority.CHANGE_NORMAL,
                              "winner", winner.getId());
                sendToOthers(serverPlayer, cs);
                return cs.build(serverPlayer);
            }

            // Are there humans left?
            // TODO: see if this can be relaxed so we can run large
            // AI-only simulations.
            boolean human = false;
            for (Player p : game.getPlayers()) {
                if (!p.isDead() && !p.isAI()
                    && ((ServerPlayer) p).isConnected()) {
                    human = true;
                    break;
                }
            }
            if (!human) {
                game.setCurrentPlayer(null);
                return null;
            }

            // Check for new turn
            ChangeSet cs = new ChangeSet();
            if (game.isNextPlayerInNewTurn()) {
                game.csNewTurn(random, cs);
                if (debugOnlyAITurns > 0) {
                    if (--debugOnlyAITurns <= 0) {
                        // If this was a debug run, complete it.  This will
                        // possibly signal the client to save and quit.
                        if (FreeCol.getDebugRunTurns() > 0) {
                            FreeCol.completeDebugRun();
                        }
                    }
                }
            }

            if ((player = (ServerPlayer) game.getNextPlayer()) == null) {
                // "can not happen"
                return DOMMessage.clientError("Can not get next player");
            }
            if (player.checkForDeath()) { // Remove dead players and retry
                player.csWithdraw(cs);
                sendToAll(cs);
                logger.info(player.getNation() + " is dead.");
                continue;
            } else if (player.isREF() && player.checkForREFDefeat()) {
                for (Player p : player.getRebels()) {
                    csGiveIndependence(player, (ServerPlayer) p, cs);
                }
                player.csWithdraw(cs);
                sendToAll(cs);
                logger.info(player.getNation() + " is defeated.");
                continue;
            }

            // Do "new turn"-like actions that need to wait until right
            // before the player is about to move.
            game.setCurrentPlayer(player);
            if (player.isREF() && player.getEntryLocation() == null) {
                // Initialize this newly created REF, determining its
                // entry location.
                // If the teleportREF option is enabled, teleport it in.
                REFAIPlayer refAIPlayer = (REFAIPlayer) freeColServer
                    .getAIPlayer(player);
                boolean teleport = getGame().getSpecification()
                    .getBoolean(GameOptions.TELEPORT_REF);
                Tile entry = refAIPlayer.initialize(teleport);
                if (entry == null) {
                    for (Player p : player.getRebels()) {
                        entry = p.getEntryLocation().getTile();
                        break;
                    }
                }
                player.setEntryLocation(entry);
                logger.info(player.getName() + " will appear at " + entry);
                if (teleport) {
                    for (Unit u : player.getUnits()) {
                        if (u.isNaval()) {
                            u.setLocation(entry);
                            u.setWorkLeft(-1);
                            u.setState(Unit.UnitState.ACTIVE);
                        }
                    }
                    cs.add(See.perhaps(), entry);
                }
            }
            player.csStartTurn(random, cs);
            cs.addTrivial(See.all(), "setCurrentPlayer",
                          ChangePriority.CHANGE_LATE,
                          "player", player.getId());
            Monarch monarch = player.getMonarch();
            if (monarch != null) {
                MonarchAction action = null;
                if (debugMonarchAction != null
                    && player == debugMonarchPlayer) {
                    action = debugMonarchAction;
                    debugMonarchAction = null;
                    debugMonarchPlayer = null;
                    logger.finest("Debug monarch action: " + action);
                } else {
                    action = RandomChoice.getWeightedRandom(logger,
                            "Choose monarch action", random,
                            monarch.getActionChoices());
                }
                if (action != null) {
                    if (monarch.actionIsValid(action)) {
                        logger.finest("Monarch action: " + action);
                        csMonarchAction(player, action, cs);
                    } else {
                        logger.finest("Skipping invalid monarch action: "
                            + action);
                    }
                }
            }

            // First, flush accumulated changes to other players.
            // Then, if this is an AI or normal connected player in
            // non-debug mode then return the accumulated changes directly,
            // otherwise flush them out and retry.
            sendToOthers(serverPlayer, cs);
            if (player.isAI()
                || (player.isConnected() && debugOnlyAITurns <= 0)) {
                return cs.build(serverPlayer);
            } else {
                sendElement(serverPlayer, cs);
            }
        }
    }

    /**
     * Give independence.  Note that the REF player is granting, but
     * most of the changes happen to the newly independent player.
     * hence the special handling.
     *
     * @param serverPlayer The REF <code>ServerPlayer</code> that is granting.
     * @param independent The newly independent <code>ServerPlayer</code>.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csGiveIndependence(ServerPlayer serverPlayer,
                                    ServerPlayer independent, ChangeSet cs) {
        serverPlayer.csChangeStance(Stance.PEACE, independent, true, cs);
        independent.setPlayerType(PlayerType.INDEPENDENT);
        Game game = getGame();
        Turn turn = game.getTurn();
        independent.modifyScore(SCORE_INDEPENDENCE_GRANTED - turn.getNumber());
        independent.setTax(0);
        independent.reinitialiseMarket();
        cs.addGlobalHistory(game,
            new HistoryEvent(turn, HistoryEvent.EventType.INDEPENDENCE));
        cs.addMessage(See.only(independent),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                "model.player.independence", independent)
            .addStringTemplate("%ref%", serverPlayer.getNationName()));

        // Who surrenders?
        List<Unit> surrenderUnits = new ArrayList<Unit>();
        for (Unit u : serverPlayer.getUnits()) {
            if (!u.isNaval()) surrenderUnits.add(u);
        }
        if (surrenderUnits.size() > 0) {
            for (Unit u : surrenderUnits) {
                UnitType downgrade = u.getTypeChange(ChangeType.CAPTURE,
                                                     independent);
                if (downgrade != null) u.setType(downgrade);
                u.setOwner(independent);
                // Make sure the former owner is notified!
                cs.add(See.perhaps().always(serverPlayer), u);
            }
            cs.addMessage(See.only(independent),
                new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                    "model.player.independence.unitsAcquired", independent)
                .addStringTemplate("%units%",
                    unitTemplate(", ", surrenderUnits)));
        }

        // Update player type.  Again, a pity to have to do a whole
        // player update, but a partial update works for other players.
        cs.addPartial(See.all().except(independent), independent, "playerType");
        cs.addMessage(See.all().except(independent),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                "model.player.independence.announce", independent)
                .addStringTemplate("%nation%", independent.getNationName())
                .addStringTemplate("%ref%", serverPlayer.getNationName()));
        cs.add(See.only(independent), independent);
    }

    private StringTemplate unitTemplate(String base, List<Unit> units) {
        StringTemplate template = StringTemplate.label(base);
        for (Unit u : units) {
            template.addStringTemplate(u.getLabel());
        }
        return template;
    }

    private StringTemplate abstractUnitTemplate(String base,
                                                List<AbstractUnit> units) {
        StringTemplate template = StringTemplate.label(base);
        Specification spec = getGame().getSpecification();
        for (AbstractUnit au : units) {
            template.addStringTemplate(au.getLabel(spec));
        }
        return template;
    }

    /**
     * Performs a monarch action.
     *
     * Note that CHANGE_LATE is used so that these actions follow
     * setting the current player, so that it is the players turn when
     * they respond to a monarch action.
     *
     * @param serverPlayer The <code>ServerPlayer</code> being acted upon.
     * @param action The monarch action.
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csMonarchAction(ServerPlayer serverPlayer,
                                 MonarchAction action, ChangeSet cs) {
        final Monarch monarch = serverPlayer.getMonarch();
        boolean valid = monarch.actionIsValid(action);
        if (!valid) return;
        String messageId = "model.monarch.action." + action.toString();
        StringTemplate template;

        switch (action) {
        case NO_ACTION:
            break;
        case RAISE_TAX_WAR: case RAISE_TAX_ACT:
            int taxRaise = monarch.raiseTax(random);
            Goods goods = serverPlayer.getMostValuableGoods();
            if (goods == null) {
                logger.finest("Ignoring tax raise, no goods to boycott.");
                break;
            }
            template = StringTemplate.template("model.monarch.action."
                + action.toString())
                .addStringTemplate("%goods%", goods.getType().getLabel(true))
                .addAmount("%amount%", taxRaise);
            if (action == MonarchAction.RAISE_TAX_WAR) {
                template = template.add("%nation%", getNonPlayerNation());
            } else if (action == MonarchAction.RAISE_TAX_ACT) {
                template = template.addAmount("%number%", random.nextInt(6))
                    .addName("%newWorld%", serverPlayer.getNewLandName());
            }
            MonarchActionMessage message
                = new MonarchActionMessage(action, template);
            message.setTax(taxRaise);
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                message);
            TaxSession taxSession = new TaxSession(monarch, serverPlayer);
            taxSession.setTax(taxRaise);
            taxSession.setGoods(goods);
            break;
        case LOWER_TAX_WAR: case LOWER_TAX_OTHER:
            int oldTax = serverPlayer.getTax();
            int taxLower = monarch.lowerTax(random);
            serverPlayer.csSetTax(taxLower, cs);
            template = StringTemplate.template(messageId)
                .addAmount("%difference%", oldTax - taxLower)
                .addAmount("%newTax%", taxLower);
            if (action == MonarchAction.LOWER_TAX_WAR) {
                template = template.add("%nation%", getNonPlayerNation());
            } else {
                template = template.addAmount("%number%", random.nextInt(5));
            }
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new MonarchActionMessage(action, template));
            break;
        case WAIVE_TAX:
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_NORMAL,
                new MonarchActionMessage(action,
                    StringTemplate.template(messageId)));
            break;
        case ADD_TO_REF:
            List<AbstractUnit> refAdditions = monarch.chooseForREF(random);
            if (refAdditions.isEmpty()) break;
            monarch.addToREF(refAdditions);
            cs.add(See.only(serverPlayer), monarch);
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new MonarchActionMessage(action,
                    StringTemplate.template(messageId)
                        .addStringTemplate("%addition%",
                            abstractUnitTemplate(", ", refAdditions))));
            break;
        case DECLARE_WAR:
            List<Player> enemies = monarch.collectPotentialEnemies();
            if (enemies.isEmpty()) break;
            Player enemy = Utils.getRandomMember(logger, "Choose enemy",
                enemies, random);
            serverPlayer.csChangeStance(Stance.WAR, enemy, true, cs);
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new MonarchActionMessage(action,
                    StringTemplate.template(messageId)
                        .addStringTemplate("%nation%", enemy.getNationName())));
            break;
        case SUPPORT_LAND: case SUPPORT_SEA:
            boolean sea = action == MonarchAction.SUPPORT_SEA;
            List<AbstractUnit> support = monarch.getSupport(random, sea);
            if (support.isEmpty()) break;
            serverPlayer.createUnits(support);
            cs.add(See.only(serverPlayer), serverPlayer.getEurope());
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new MonarchActionMessage(action,
                    StringTemplate.template(messageId)
                    .addStringTemplate("%addition%",
                        abstractUnitTemplate(", ", support))));
            break;
        case OFFER_MERCENARIES:
            List<AbstractUnit> mercenaries = monarch.getMercenaries(random);
            if (mercenaries.isEmpty()) break;
            int mercPrice = serverPlayer.priceMercenaries(mercenaries);
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new MonarchActionMessage(MonarchAction.OFFER_MERCENARIES,
                    StringTemplate.template("model.monarch.action.OFFER_MERCENARIES")
                        .addAmount("%gold%", mercPrice)
                        .addStringTemplate("%mercenaries%",
                            abstractUnitTemplate(", ", mercenaries))));
            MercenariesSession mercenariesSession
                = new MercenariesSession(monarch, serverPlayer);
            mercenariesSession.setMercenaries(mercenaries);
            mercenariesSession.setPrice(mercPrice);
            break;
        case DISPLEASURE: default:
            logger.warning("Bogus action: " + action);
            break;
        }
    }

    /**
     * Handles a response to a tax raise.
     *
     * @param serverPlayer The <code>ServerPlayer</code> whose tax to raise.
     * @param accepted Did the player accept the tax raise.
     * @return An element encapsulating a suitable update.
     */
    public Element monarchRaiseTax(ServerPlayer serverPlayer,
                                   boolean accepted) {
        TaxSession session = TransactionSession.lookup(TaxSession.class,
            serverPlayer.getMonarch(), serverPlayer);
        if (session == null) {
            return DOMMessage.clientError("Invalid tax reply.");
        }
        session.setAccepted(accepted);
        ChangeSet cs = new ChangeSet();
        session.complete(cs);
        return cs.build(serverPlayer);
    }

    /**
     * Handles the response to an offer to player of some mercenaries.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to offer to.
     * @param accepted Whether the offer was accepted or not.
     * @return An element encapsulating a suitable update.
     */
    public Element monarchOfferMercenaries(ServerPlayer serverPlayer,
                                           boolean accepted) {
        MercenariesSession session
            = TransactionSession.lookup(MercenariesSession.class,
                serverPlayer.getMonarch(), serverPlayer);
        if (session == null) {
            return DOMMessage.clientError("Invalid mercenary reply");
        }

        ChangeSet cs = new ChangeSet();
        if (accepted) {
            int price = session.getPrice();
            if (serverPlayer.checkGold(price)) {
                serverPlayer.createUnits(session.getMercenaries());
                cs.add(See.only(serverPlayer), serverPlayer.getEurope());
                serverPlayer.modifyGold(-price);
                cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
            } else {
                serverPlayer.getMonarch().setDispleasure(true);
                cs.add(See.only(serverPlayer), ChangePriority.CHANGE_NORMAL,
                    new MonarchActionMessage(MonarchAction.DISPLEASURE,
                        StringTemplate.template("model.monarch.action.DISPLEASURE")));
            }
        }
        session.complete(cs);
        return cs.build(serverPlayer);
    }

    /**
     * Check the high scores.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is retiring.
     * @return An element indicating the players high score state.
     */
    public Element checkHighScore(ServerPlayer serverPlayer) {
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

        ChangeSet cs = new ChangeSet();
        cs.addAttribute(See.only(serverPlayer),
            "highScore", Boolean.toString(highScore));
        return cs.build(serverPlayer);
    }

    /**
     * Handle a player retiring.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is retiring.
     * @return An element cleaning up the player.
     */
    public Element retire(ServerPlayer serverPlayer) {
        ChangeSet cs = new ChangeSet();
        serverPlayer.csWithdraw(cs); // Clean up the player.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Continue playing after winning.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that plays on.
     * @return Null.
     */
    public Element continuePlaying(ServerPlayer serverPlayer) {
        ServerGame game = (ServerGame) getGame();
        Element reply = null;
        if (!getFreeColServer().isSingleplayer()) {
            logger.warning("Can not continue playing in multiplayer!");
        } else if (serverPlayer != game.checkForWinner()) {
            logger.warning("Can not continue playing, as "
                           + serverPlayer.getName()
                           + " has not won the game!");
        } else {
            Specification spec = game.getSpecification();
            spec.getBooleanOption(GameOptions.VICTORY_DEFEAT_REF)
                .setValue(false);
            spec.getBooleanOption(GameOptions.VICTORY_DEFEAT_EUROPEANS)
                .setValue(false);
            spec.getBooleanOption(GameOptions.VICTORY_DEFEAT_HUMANS)
                .setValue(false);
            // The victory panel is shown after end turn, end turn again
            // to start turn of next player.
            reply = endTurn((ServerPlayer) game.getCurrentPlayer());
        }
        return reply;
    }


    /**
     * Choose a founding father from the current offered fathers.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is choosing.
     * @param id The id of the <code>FoundingFather</code> to pick.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element chooseFoundingFather(ServerPlayer serverPlayer, String id) {
        FoundingFather father = getGame().getSpecification()
            .getFoundingFather(id);
        if (father == null) {
            return DOMMessage.clientError("Not a founding father: " + id);
        } else if (serverPlayer.getCurrentFather() != null) {
            return DOMMessage.clientError("Already recruiting: "
                + serverPlayer.getCurrentFather().getId());
        } else if (!serverPlayer.getOfferedFathers().contains(father)) {
            return DOMMessage.clientError("Not an offered father: " + id);
        } else {
            serverPlayer.setCurrentFather(father);
            serverPlayer.setOfferedFathers(new ArrayList<FoundingFather>());
            logger.info("Selected founding father: " + father.getId());
        }
        return null;
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

        // Work out the cash in amount and the message to send.
        int fullAmount = unit.getTreasureAmount();
        int cashInAmount;
        String messageId;
        if (serverPlayer.getPlayerType() == PlayerType.COLONIAL) {
            // Charge transport fee and apply tax
            cashInAmount = (fullAmount - unit.getTransportFee())
                * (100 - serverPlayer.getTax()) / 100;
            messageId = "model.unit.cashInTreasureTrain.colonial";
        } else {
            // No fee possible, no tax applies.
            cashInAmount = fullAmount;
            messageId = "model.unit.cashInTreasureTrain.independent";
        }
            
        serverPlayer.modifyGold(cashInAmount);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold", "score");
        cs.addMessage(See.only(serverPlayer),
            new ModelMessage(messageId, serverPlayer, unit)
                .addAmount("%amount%", fullAmount)
                .addAmount("%cashInAmount%", cashInAmount));
        messageId = (serverPlayer.getPlayerType() == PlayerType.REBEL
                     || serverPlayer.getPlayerType() == PlayerType.INDEPENDENT)
            ? "model.unit.cashInTreasureTrain.other.independent"
            : "model.unit.cashInTreasureTrain.other.colonial";
        cs.addMessage(See.all().except(serverPlayer),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                             messageId, serverPlayer)
                .addAmount("%amount%", fullAmount)
                .addStringTemplate("%nation%", serverPlayer.getNationName()));

        // Dispose of the unit, only visible to the owner.
        cs.add(See.only(serverPlayer), (FreeColGameObject) unit.getLocation());
        cs.addDispose(See.only(serverPlayer), null, unit);

        // Others can see the cash in message.
        sendToOthers(serverPlayer, cs);
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
        cs.addGlobalHistory(getGame(), new HistoryEvent(turn,
                HistoryEvent.EventType.DECLARE_INDEPENDENCE));
        serverPlayer.clearModelMessages();
        cs.addMessage(See.only(serverPlayer),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                "warOfIndependence.independenceDeclared", serverPlayer));

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
            UnitType upgrade = unitType.getTargetType(ChangeType.INDEPENDENCE,
                                                      serverPlayer);
            if (upgrade != null) {
                upgrades.put(unitType, upgrade);
            }
        }
        for (Colony colony : serverPlayer.getColonies()) {
            int sol = colony.getSoL();
            if (sol > 50) {
                java.util.Map<UnitType, List<Unit>> unitMap = new HashMap<UnitType, List<Unit>>();
                List<Unit> allUnits = colony.getTile().getUnitList();
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
        serverPlayer.getHighSeas().removeDestination(europe);
        cs.addDispose(See.only(serverPlayer), null, europe);
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
        serverPlayer.csChangeStance(Stance.WAR, refPlayer, true, cs);

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
        TradeSession session = TransactionSession.lookup(TradeSession.class,
            unit, settlement);
        if (session == null) {
            if (unit.getMovesLeft() <= 0) {
                return DOMMessage.clientError("Unit " + unit.getId()
                    + " has no moves left.");
            }
            session = new TradeSession(unit, settlement);
            // Sets unit moves to zero to avoid cheating.  If no
            // action is taken, the moves will be restored when
            // closing the session
            unit.setMovesLeft(0);
            cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
        }

        // Add just the attributes the client needs.
        cs.addAttribute(See.only(serverPlayer), "canBuy",
            Boolean.toString(session.getBuy()));
        cs.addAttribute(See.only(serverPlayer), "canSell",
            Boolean.toString(session.getSell()));
        cs.addAttribute(See.only(serverPlayer), "canGift",
            Boolean.toString(session.getGift()));

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
        TradeSession session
            = TradeSession.lookup(TradeSession.class, unit, settlement);
        if (session == null) {
            return DOMMessage.clientError("No such transaction session.");
        }

        ChangeSet cs = new ChangeSet();
        // Restore unit movement if no action taken.
        if (!session.getActionTaken()) {
            unit.setMovesLeft(session.getMovesLeft());
            cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
        }
        session.complete(cs);

        // Others can not see end of transaction.
        return cs.build(serverPlayer);
    }


    /**
     * Get the goods for sale in a settlement.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is enquiring.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element getGoodsForSale(ServerPlayer serverPlayer, Unit unit,
                                   Settlement settlement) {
        List<Goods> sellGoods = null;

        if (settlement instanceof IndianSettlement) {
            IndianSettlement indianSettlement = (IndianSettlement) settlement;
            AIPlayer aiPlayer = getFreeColServer()
                .getAIPlayer(indianSettlement.getOwner());
            sellGoods = indianSettlement.getSellGoods(3, unit);
            for (Goods goods : sellGoods) {
                aiPlayer.registerSellGoods(goods);
            }
        } else { // Colony might be supported one day?
            return DOMMessage.clientError("Bogus settlement");
        }
        return new GoodsForSaleMessage(unit, settlement, sellGoods)
            .toXMLElement();
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
        TradeSession session
            = TradeSession.lookup(TradeSession.class, unit, settlement);
        if (session == null) {
            return DOMMessage.clientError("Proposing to buy without opening a transaction session?!");
        }
        if (!session.getBuy()) {
            return DOMMessage.clientError("Proposing to buy in a session where buying is not allowed.");
        }

        // AI considers the proposition, return with a gold value
        AIPlayer ai = getFreeColServer().getAIPlayer(settlement.getOwner());
        int gold = ai.buyProposition(unit, settlement, goods, price);

        // Others can not see proposals.
        ChangeSet cs = new ChangeSet();
        cs.addAttribute(See.only(serverPlayer),
                        "gold", Integer.toString(gold));
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
        TradeSession session
            = TradeSession.lookup(TradeSession.class, unit, settlement);
        if (session == null) {
            return DOMMessage.clientError("Proposing to sell without opening a transaction session");
        }
        if (!session.getSell()) {
            return DOMMessage.clientError("Proposing to sell in a session where selling is not allowed.");
        }

        // AI considers the proposition, return with a gold value
        AIPlayer ai = getFreeColServer().getAIPlayer(settlement.getOwner());
        int gold = ai.sellProposition(unit, settlement, goods, price);

        // Others can not see proposals.
        ChangeSet cs = new ChangeSet();
        cs.addAttribute(See.only(serverPlayer),
                        "gold", Integer.toString(gold));
        return cs.build(serverPlayer);
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
        if (!serverPlayer.canTrade(type, Access.EUROPE)) {
            return DOMMessage.clientError("Can not trade boycotted goods");
        }
        ChangeSet cs = new ChangeSet();
        GoodsContainer container = unit.getGoodsContainer();
        container.saveState();
        serverPlayer.buy(container, type, amount, random);
        serverPlayer.csFlushMarket(type, cs);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        cs.add(See.only(serverPlayer), container);
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
        if (!serverPlayer.canTrade(type, Access.EUROPE)) {
            return DOMMessage.clientError("Can not trade boycotted goods");
        }
        ChangeSet cs = new ChangeSet();
        GoodsContainer container = unit.getGoodsContainer();
        container.saveState();
        serverPlayer.sell(container, type, amount, random);
        serverPlayer.csFlushMarket(type, cs);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        cs.add(See.only(serverPlayer), container);
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
        serverPlayer.csEmigrate(slot, type, random, cs);

        // Do not update others, emigration is private.
        return cs.build(serverPlayer);
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
        ((ServerUnit) unit).csMove(newTile, random, cs);
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Decline to investigate strange mounds.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param tile The <code>Tile</code> where the mounds are.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element declineMounds(ServerPlayer serverPlayer, Tile tile) {
        tile.removeLostCityRumour();

        // Others might see rumour disappear
        ChangeSet cs = new ChangeSet();
        cs.add(See.perhaps(), tile);
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Set land name.
     *
     * @param serverPlayer The <code>ServerPlayer</code> who landed.
     * @param unit The <code>Unit</code> that has come ashore.
     * @param name The new land name.
     * @param welcomer An optional <code>ServerPlayer</code> that has offered
     *            a treaty.
     * @param camps An optional number of camps for the welcome message.
     * @param accept True if the treaty has been accepted.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element setNewLandName(ServerPlayer serverPlayer, Unit unit,
                                  String name, ServerPlayer welcomer, int camps,
                                  boolean accept) {
        ChangeSet cs = new ChangeSet();

        // Special case of a welcome from an adjacent native unit,
        // offering the land the landing unit is on if a peace treaty
        // is accepted.
        serverPlayer.setNewLandName(name);
        if (welcomer != null) {
            if (accept) { // Claim land
                Tile tile = unit.getTile();
                tile.changeOwnership(serverPlayer, null);
                cs.add(See.perhaps(), tile);
            } else { // Consider not accepting the treaty to be an insult.
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
     * Move a unit across the high seas.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param unit The <code>Unit</code> to move.
     * @param destination The <code>Location</code> to move to.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element moveTo(ServerPlayer serverPlayer, Unit unit,
                          Location destination) {
        ChangeSet cs = new ChangeSet();
        HighSeas highSeas = serverPlayer.getHighSeas();
        Location current = unit.getDestination();
        boolean others = false; // Notify others?
        boolean invalid = false; // Not a highSeas move?

        if (destination instanceof Europe) {
            if (!highSeas.getDestinations().contains(destination)) {
                return DOMMessage.clientError("HighSeas does not connect to: "
                    + ((FreeColGameObject) destination).getId());
            } else if (unit.getLocation() == highSeas) {
                if (!(current instanceof Europe)) {
                    // Changed direction
                    unit.setWorkLeft(unit.getSailTurns()
                        - unit.getWorkLeft() + 1);
                }
                unit.setMission(new GoToMission(unit, destination));
                cs.add(See.only(serverPlayer), unit, highSeas);
            } else if (unit.getTile() != null) {
                Tile tile = unit.getTile();
                unit.setEntryLocation(tile);
                unit.setWorkLeft(unit.getSailTurns());
                unit.setMission(new GoToMission(unit, destination));
                unit.setMovesLeft(0);
                unit.setLocation(highSeas);
                cs.addDisappear(serverPlayer, tile, unit);
                cs.add(See.only(serverPlayer), tile, highSeas);
                others = true;
            } else {
                invalid = true;
            }
        } else if (destination instanceof Map) {
            if (!highSeas.getDestinations().contains(destination)) {
                return DOMMessage.clientError("HighSeas does not connect to: "
                    + ((FreeColGameObject) destination).getId());
            } else if (unit.getLocation() == highSeas) {
                if (current != destination && (current == null
                        || current.getTile() == null
                        || current.getTile().getMap() != destination)) {
                    // Changed direction
                    unit.setWorkLeft(unit.getSailTurns()
                        - unit.getWorkLeft() + 1);
                }
                unit.setMission(new GoToMission(unit, destination));
                cs.add(See.only(serverPlayer), unit, highSeas);
            } else if (unit.getLocation() instanceof Europe) {
                Europe europe = (Europe) unit.getLocation();
                unit.setWorkLeft(unit.getSailTurns());
                unit.setMission(new GoToMission(unit, destination));
                unit.setMovesLeft(0);
                unit.setLocation(highSeas);
                cs.add(See.only(serverPlayer), unit, europe, highSeas);
            } else {
                invalid = true;
            }
        } else if (destination instanceof Settlement) {
            Tile tile = destination.getTile();
            if (!highSeas.getDestinations().contains(tile.getMap())) {
                return DOMMessage.clientError("HighSeas does not connect to: "
                    + ((FreeColGameObject) destination).getId());
            } else if (unit.getLocation() == highSeas) {
                // Direction is somewhat moot, so just reset.
                unit.setWorkLeft(unit.getSailTurns());
                unit.setMission(new GoToMission(unit, destination));
                cs.add(See.only(serverPlayer), unit, highSeas);
            } else if (unit.getLocation() instanceof Europe) {
                Europe europe = (Europe) unit.getLocation();
                unit.setWorkLeft(unit.getSailTurns());
                unit.setMission(new GoToMission(unit, destination));
                unit.setMovesLeft(0);
                unit.setLocation(highSeas);
                cs.add(See.only(serverPlayer), unit, europe, highSeas);
            } else {
                invalid = true;
            }
        } else {
            return DOMMessage.clientError("Bogus moveTo destination: "
                + ((FreeColGameObject) destination).getId());
        }
        if (invalid) {
            return DOMMessage.clientError("Invalid moveTo: unit=" + unit.getId()
                + " from=" + ((FreeColGameObject) unit.getLocation()).getId()
                + " to=" + ((FreeColGameObject) destination).getId());
        }

        if (others) sendToOthers(serverPlayer, cs);
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
            return DOMMessage.clientError("Naval unit " + unit.getId()
                + " can not embark.");
        }
        if (carrier.getSpaceLeft() < unit.getSpaceTaken()) {
            return DOMMessage.clientError("No space available for unit "
                + unit.getId() + " to embark.");
        }

        ChangeSet cs = new ChangeSet();

        Location oldLocation = unit.getLocation();
        unit.setLocation(carrier);
        unit.setMovesLeft(0);
        cs.add(See.only(serverPlayer), (FreeColGameObject) oldLocation);
        if (carrier.getLocation() != oldLocation) {
            cs.add(See.only(serverPlayer), carrier);
        }
        if (oldLocation instanceof Tile) {
            cs.addMove(See.only(serverPlayer), unit, (Tile) oldLocation,
                carrier.getTile());
            cs.addDisappear(serverPlayer, (Tile) oldLocation, unit);
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
            return DOMMessage.clientError("Naval unit " + unit.getId()
                + " can not disembark.");
        }
        if (!(unit.getLocation() instanceof Unit)) {
            return DOMMessage.clientError("Unit " + unit.getId()
                + " is not embarked.");
        }

        ChangeSet cs = new ChangeSet();

        Unit carrier = (Unit) unit.getLocation();
        Location newLocation = carrier.getLocation();
        List<Tile> newTiles = (newLocation.getTile() == null) ? null
            : ((ServerUnit) unit).collectNewTiles(newLocation.getTile());
        unit.setLocation(newLocation);
        unit.setMovesLeft(0); // In Col1 disembark consumes whole move.
        cs.add(See.perhaps(), (FreeColGameObject) newLocation);
        if (newTiles != null) {
            serverPlayer.csSeeNewTiles(newTiles, cs);
        }

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
            attackerPlayer.csCombat(attacker, defender, crs, random, cs);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Combat FAIL", e);
            return DOMMessage.clientError(e.getMessage());
        }
        sendToOthers(attackerPlayer, cs);
        return cs.build(attackerPlayer);
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

        csSpeakToChief(serverPlayer, settlement, false, cs);
        Tile tile = settlement.getTile();
        tile.updatePlayerExploredTile(serverPlayer, true);
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
            return DOMMessage.clientError("No skill to learn at "
                + settlement.getName());
        }
        if (!unit.getType().canBeUpgraded(skill, ChangeType.NATIVES)) {
            return DOMMessage.clientError("Unit " + unit.toString()
                + " can not learn skill " + skill
                + " at " + settlement.getName());
        }

        // Try to learn
        ChangeSet cs = new ChangeSet();
        unit.setMovesLeft(0);
        csSpeakToChief(serverPlayer, settlement, false, cs);
        switch (settlement.getAlarm(serverPlayer).getLevel()) {
        case HATEFUL: // Killed, might be visible to other players.
            cs.add(See.perhaps().always(serverPlayer),
                   (FreeColGameObject) unit.getLocation());
            cs.addDispose(See.perhaps().always(serverPlayer),
                unit.getLocation(), unit);
            break;
        case ANGRY: // Learn nothing, not even a pet update
            cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
            break;
        default:
            // Teach the unit, and expend the skill if necessary.
            // Do a full information update as the unit is in the settlement.
            unit.setType(skill);
            if (!settlement.isCapital()
                && !(settlement.getMissionary(serverPlayer) != null
                    && getGame().getSpecification()
                    .getBoolean("model.option.enhancedMissionaries"))) {
                settlement.setLearnableSkill(null);
            }
            break;
        }
        Tile tile = settlement.getTile();
        tile.updatePlayerExploredTile(serverPlayer, true);
        cs.add(See.only(serverPlayer), unit, tile);

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

        csSpeakToChief(serverPlayer, settlement, false, cs);

        Player indianPlayer = settlement.getOwner();
        int gold = 0;
        int year = getGame().getTurn().getNumber();
        RandomRange gifts = settlement.getType().getGifts(unit);
        if (settlement.getLastTribute() + TURNS_PER_TRIBUTE < year
            && gifts != null) {
            switch (indianPlayer.getTension(serverPlayer).getLevel()) {
            case HAPPY: case CONTENT:
                gold = Math.min(gifts.getAmount("Tribute", random, true) / 10,
                                100);
                break;
            case DISPLEASED:
                gold = Math.min(gifts.getAmount("Tribute", random, true) / 20,
                                100);
                break;
            case ANGRY: case HATEFUL:
            default:
                gold = 0; // No tribute for you.
                break;
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
        Tile tile = settlement.getTile();
        tile.updatePlayerExploredTile(serverPlayer, true);
        cs.add(See.only(serverPlayer), tile);
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
        Tile tile = settlement.getTile();
        boolean tileDirty = settlement.makeContactSettlement(serverPlayer);
        String result;

        // Hateful natives kill the scout right away.
        Tension tension = settlement.getAlarm(serverPlayer);
        if (tension.getLevel() == Tension.Level.HATEFUL) {
            cs.add(See.perhaps().always(serverPlayer),
                   (FreeColGameObject) unit.getLocation());
            cs.addDispose(See.perhaps().always(serverPlayer),
                unit.getLocation(), unit);
            result = "die";
        } else {
            // Otherwise player gets to visit, and learn about the settlement.
            int radius = unit.getLineOfSight();
            UnitType skill = settlement.getLearnableSkill();
            if (settlement.hasSpokenToChief()) {
                // Do nothing if already spoken to.
                result = "nothing";
            } else if (skill != null
                       && skill.hasAbility(Ability.EXPERT_SCOUT)
                       && unit.getType().canBeUpgraded(skill,
                                                       ChangeType.NATIVES)) {
                // If the scout can be taught to be an expert it will be.
                // TODO: in the old code the settlement retains the
                // teaching ability.  WWC1D?
                unit.setType(settlement.getLearnableSkill());
                result = "expert";
            } else {
                // Choose tales 1/3 of the time, or if there are no beads.
                RandomRange gifts = settlement.getType().getGifts(unit);
                int gold = (gifts == null) ? 0
                    : gifts.getAmount("Base beads amount", random, true);
                if (gold <= 0
                    || Utils.randomInt(logger, "Tales", random, 3) == 0) {
                    radius = Math.max(radius, IndianSettlement.TALES_RADIUS);
                    result = "tales";
                } else {
                    if (unit.hasAbility(Ability.EXPERT_SCOUT)) {
                        gold = (gold * 11) / 10; // TODO: magic number
                    }
                    serverPlayer.modifyGold(gold);
                    settlement.getOwner().modifyGold(-gold);
                    result = "beads";
                }
            }

            // Have now spoken to the chief.
            csSpeakToChief(serverPlayer, settlement, true, cs);
            tileDirty = true;

            // Update settlement tile with new information, and any
            // newly visible tiles, possibly with enhanced radius.
            for (Tile t : tile.getSurroundingTiles(radius)) {
                if (!serverPlayer.canSee(t) && (t.isLand() || t.isCoast())) {
                    serverPlayer.setExplored(t);
                    cs.add(See.only(serverPlayer), t);
                }
            }

            // If the unit was promoted, update it completely, otherwise just
            // update moves and possibly gold+score.
            unit.setMovesLeft(0);
            if ("expert".equals(result)) {
                cs.add(See.perhaps(), unit);
            } else {
                cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
                if ("beads".equals(result)) {
                    cs.addPartial(See.only(serverPlayer), serverPlayer,
                                  "gold", "score");
                }
            }
        }
        if (tileDirty) {
            tile.updatePlayerExploredTile(serverPlayer, true);
            cs.add(See.only(serverPlayer), tile);
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
        ChangeSet cs = new ChangeSet();
        csSpeakToChief(serverPlayer, settlement, false, cs);

        // Determine result
        Unit missionary = settlement.getMissionary();
        if (missionary == null) {
            return DOMMessage.clientError("Denouncing null missionary");
        }
        ServerPlayer enemy = (ServerPlayer) missionary.getOwner();
        double denounce = Utils.randomDouble(logger, "Denounce base", random)
            * enemy.getImmigration() / (serverPlayer.getImmigration() + 1);
        if (missionary.hasAbility("model.ability.expertMissionary")) {
            denounce += 0.2;
        }
        if (unit.hasAbility("model.ability.expertMissionary")) {
            denounce -= 0.2;
        }

        if (denounce < 0.5) { // Success, remove old mission and establish ours
            return establishMission(serverPlayer, unit, settlement);
        }

        // Denounce failed
        Player owner = settlement.getOwner();
        cs.add(See.only(serverPlayer), settlement);
        cs.addMessage(See.only(serverPlayer),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                             "indianSettlement.mission.noDenounce",
                             serverPlayer, unit)
                .addStringTemplate("%nation%", owner.getNationName()));
        cs.addMessage(See.only(enemy),
            new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                             "indianSettlement.mission.enemyDenounce",
                             enemy, settlement)
                .addStringTemplate("%enemy%", serverPlayer.getNationName())
                .addName("%settlement%", settlement.getNameFor(enemy))
                .addStringTemplate("%nation%", owner.getNationName()));
        cs.add(See.perhaps().always(serverPlayer),
               (FreeColGameObject) unit.getLocation());
        cs.addDispose(See.perhaps().always(serverPlayer),
            unit.getLocation(), unit);

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
        csSpeakToChief(serverPlayer, settlement, false, cs);

        Unit missionary = settlement.getMissionary();
        if (missionary != null) {
            ServerPlayer enemy = (ServerPlayer) missionary.getOwner();
            enemy.csKillMissionary(settlement, cs);
        }

        // Result depends on tension wrt this settlement.
        // Establish if at least not angry.
        switch (settlement.getAlarm(serverPlayer).getLevel()) {
        case HATEFUL: case ANGRY:
            cs.add(See.perhaps().always(serverPlayer),
                (FreeColGameObject) unit.getLocation());
            cs.addDispose(See.perhaps().always(serverPlayer),
                unit.getLocation(), unit);
            break;
        case HAPPY: case CONTENT: case DISPLEASED:
            cs.add(See.perhaps().always(serverPlayer), unit.getTile());
            unit.setLocation(null);
            unit.setMovesLeft(0);
            cs.add(See.only(serverPlayer), unit);
            settlement.changeMissionary(unit);
            settlement.setConvertProgress(0);
            List<FreeColGameObject> modifiedSettlements
                = settlement.modifyAlarm(serverPlayer, ALARM_NEW_MISSIONARY);
            modifiedSettlements.remove(settlement);
            if (!modifiedSettlements.isEmpty()) {
                cs.add(See.only(serverPlayer), modifiedSettlements);
            }
            break;
        }
        Tile tile = settlement.getTile();
        tile.updatePlayerExploredTile(serverPlayer, true);
        cs.add(See.perhaps().always(serverPlayer), tile);
        String messageId = "indianSettlement.mission."
            + settlement.getAlarm(serverPlayer).getKey();
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

        Tile tile = settlement.getTile();
        csSpeakToChief(serverPlayer, settlement, false, cs);
        tile.updatePlayerExploredTile(serverPlayer, true);
        cs.add(See.only(serverPlayer), tile);

        // How much gold will be needed?
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
        } else if (gold < goldToPay || !serverPlayer.checkGold(gold)) {
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
            // to the European player.  Let resulting stance changes happen
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
        if (destination == null) {
            unit.setMission(null);
        } else {
            unit.setMission(new GoToMission(unit, destination));
        }

        // Others can not see a destination change.
        return new ChangeSet().add(See.only(serverPlayer), unit)
            .build(serverPlayer);
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

        List<Stop> stops = unit.getTradeRoute().getStops();
        int next = current;
        for (;;) {
            if (++next >= stops.size()) next = 0;
            if (next == current) return null; // No work at any stop, stay put.
            Stop nextStop = stops.get(next);
            if (((ServerUnit) unit).hasWorkAtStop(nextStop)) break;
            logger.finest("Unit " + unit
                + " in trade route " + unit.getTradeRoute().getName()
                + " found no work at stop: " + (FreeColGameObject) nextStop.getLocation());
        }

        // Next is the updated stop.
        // Could do just a partial update of currentStop if we did not
        // also need to set the unit destination.
        unit.setCurrentStop(next);
        unit.setMission(new GoToMission(unit, stops.get(next).getLocation()));

        // Others can not see a stop change.
        return new ChangeSet().add(See.only(serverPlayer), unit)
            .build(serverPlayer);
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
        ChangeSet cs = new ChangeSet();
        csSpeakToChief(serverPlayer, settlement, false, cs);

        TradeSession session
            = TradeSession.lookup(TradeSession.class, unit, settlement);
        if (session == null) {
            return DOMMessage.clientError("Trying to buy without opening a transaction session");
        }
        if (!session.getBuy()) {
            return DOMMessage.clientError("Trying to buy in a session where buying is not allowed.");
        }
        if (unit.getSpaceLeft() <= 0) {
            return DOMMessage.clientError("Unit is full, unable to buy.");
        }

        // Check that this is the agreement that was made
        AIPlayer ai = getFreeColServer().getAIPlayer(settlement.getOwner());
        int returnGold = ai.buyProposition(unit, settlement, goods, amount);
        if (returnGold != amount) {
            return DOMMessage.clientError("This was not the price we agreed upon! Cheater?");
        }
        if (!serverPlayer.checkGold(amount)) { // Check this is funded.
            return DOMMessage.clientError("Insufficient gold to buy.");
        }

        // Valid, make the trade.
        moveGoods(goods, unit);
        cs.add(See.perhaps(), unit);

        Player settlementPlayer = settlement.getOwner();
        Tile tile = settlement.getTile();
        settlement.updateWantedGoods();
        settlementPlayer.modifyGold(amount);
        serverPlayer.modifyGold(-amount);
        cs.add(See.only(serverPlayer),
            settlement.modifyAlarm(serverPlayer, -amount / 50));
        tile.updatePlayerExploredTile(serverPlayer, true);
        cs.add(See.only(serverPlayer), tile);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        session.setBuy();
        logger.finest(serverPlayer.getName() + " " + unit + " buys " + goods
                      + " at " + settlement.getName() + " for " + amount);

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
        ChangeSet cs = new ChangeSet();
        csSpeakToChief(serverPlayer, settlement, false, cs);

        TradeSession session
            = TransactionSession.lookup(TradeSession.class, unit, settlement);
        if (session == null) {
            return DOMMessage.clientError("Trying to sell without opening a transaction session");
        }
        if (!session.getSell()) {
            return DOMMessage.clientError("Trying to sell in a session where selling is not allowed.");
        }

        // Check that the gold is the agreed amount
        AIPlayer ai = getFreeColServer().getAIPlayer(settlement.getOwner());
        int returnGold = ai.sellProposition(unit, settlement, goods, amount);
        if (returnGold != amount) {
            return DOMMessage.clientError("This was not the price we agreed upon! Cheater?");
        }

        // Valid, make the trade.
        moveGoods(goods, settlement);
        cs.add(See.perhaps(), unit);

        Player settlementPlayer = settlement.getOwner();
        settlementPlayer.modifyGold(-amount);
        serverPlayer.modifyGold(amount);
        cs.add(See.only(serverPlayer), settlement.modifyAlarm(serverPlayer,
                -amount / 500));
        Tile tile = settlement.getTile();
        settlement.updateWantedGoods();
        tile.updatePlayerExploredTile(serverPlayer, true);
        cs.add(See.only(serverPlayer), tile);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        session.setSell();
        cs.addSale(serverPlayer, settlement, goods.getType(),
                   (int) Math.round((float) amount / goods.getAmount()));
        logger.finest(serverPlayer.getName() + " " + unit + " sells " + goods
                      + " at " + settlement.getName() + " for " + amount);

        // Others can see the unit capacity.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    /**
     * Deliver gift to settlement.
     * Note that this includes both European and native gifts.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is delivering.
     * @param unit The <code>Unit</code> that is delivering.
     * @param goods The <code>Goods</code> to deliver.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element deliverGiftToSettlement(ServerPlayer serverPlayer,
                                           Unit unit, Settlement settlement,
                                           Goods goods) {
        TradeSession session
            = TransactionSession.lookup(TradeSession.class, unit, settlement);
        if (session == null) {
            return DOMMessage.clientError("Trying to deliver gift without opening a session");
        }
        if (!session.getGift()) {
            return DOMMessage.clientError("Trying to deliver gift in a session where gift giving is not allowed: " + unit + " " + settlement + " " + session);
        }

        ChangeSet cs = new ChangeSet();
        Tile tile = settlement.getTile();
        moveGoods(goods, settlement);
        cs.add(See.perhaps(), unit);
        if (settlement instanceof IndianSettlement) {
            IndianSettlement indianSettlement = (IndianSettlement) settlement;
            csSpeakToChief(serverPlayer, indianSettlement, false, cs);
            cs.add(See.only(serverPlayer),
                   indianSettlement.modifyAlarm(serverPlayer,
                   -indianSettlement.getPriceToBuy(goods) / 50));
            indianSettlement.updateWantedGoods();
            tile.updatePlayerExploredTile(serverPlayer, true);
            cs.add(See.only(serverPlayer), tile);
        }
        session.setGift();

        // Inform the receiver of the gift.
        ServerPlayer receiver = (ServerPlayer) settlement.getOwner();
        if (receiver.isConnected() && settlement instanceof Colony) {
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
        Location oldLocation = goods.getLocation();

        goods.adjustAmount();
        moveGoods(goods, unit);
        boolean moved = false;
        if (unit.getInitialMovesLeft() != unit.getMovesLeft()) {
            unit.setMovesLeft(0);
            moved = true;
        }
        Unit oldUnit = null;
        if (oldLocation instanceof Unit) {
            oldUnit = (Unit) oldLocation;
            if (oldUnit.getInitialMovesLeft() != oldUnit.getMovesLeft()) {
                oldUnit.setMovesLeft(0);
            } else {
                oldUnit = null;
            }
        }

        // Update the goodsContainers only if within a settlement,
        // otherwise update the shared location so that others can
        // see capacity changes.
        if (unit.getSettlement() != null) {
            cs.add(See.only(serverPlayer), oldLocation.getGoodsContainer());
            cs.add(See.only(serverPlayer), unit.getGoodsContainer());
            if (moved) {
                cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
            }
            if (oldUnit != null) {
                cs.addPartial(See.only(serverPlayer), oldUnit, "movesLeft");
            }
        } else {
            cs.add(See.perhaps(), (FreeColGameObject) unit.getLocation());
        }

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
        Settlement settlement = null;
        if (unit.isInEurope()) { // Must be a dump of boycotted goods
            loc = null;
        } else if (unit.getTile() == null) {
            return DOMMessage.clientError("Unit not on the map.");
        } else if (unit.getSettlement() != null) {
            settlement = unit.getTile().getSettlement();
            loc = settlement;
        } else { // Dump of goods onto a tile
            loc = null;
        }
        goods.adjustAmount();
        moveGoods(goods, loc);
        boolean moved = false;
        if (unit.getInitialMovesLeft() != unit.getMovesLeft()) {
            unit.setMovesLeft(0);
            moved = true;
        }

        if (settlement != null) {
            cs.add(See.only(serverPlayer), settlement.getGoodsContainer());
            cs.add(See.only(serverPlayer), unit.getGoodsContainer());
            if (moved) cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
        } else {
            cs.add(See.perhaps(), (FreeColGameObject) unit.getLocation());
        }

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
            return DOMMessage.clientError("Can not clear unit speciality: "
                + unit.getId());
        }

        // There can be some restrictions that may prevent the
        // clearing of the speciality.  AFAICT the only ATM is that a
        // teacher can not lose its speciality, but this will need to
        // be revisited if we invent a work location that requires a
        // particular unit type.
        if (unit.getStudent() != null) {
            return DOMMessage.clientError("Can not clear speciality of a teacher.");
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
        cs.addDispose(See.perhaps().always(serverPlayer),
            unit.getLocation(), unit);

        // Others can see the unit removal and the space it leaves.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
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
            settlement = new ServerColony(game, serverPlayer, name, tile);
            settlement.placeSettlement(serverPlayer.isAI());
        } else {
            IndianNationType nationType
                = (IndianNationType) serverPlayer.getNationType();
            UnitType skill = RandomChoice.getWeightedRandom(logger,
                    "Choose skill", random,
                    nationType.generateSkillsForTile(tile));
            if (skill == null) { // Seasoned Scout
                List<UnitType> scouts = getGame().getSpecification()
                    .getUnitTypesWithAbility(Ability.EXPERT_SCOUT);
                skill = Utils.getRandomMember(logger, "Choose scout",
                                              scouts, random);
            }
            settlement = new ServerIndianSettlement(game, serverPlayer, name,
                                                    tile, false, skill,
                                                    new HashSet<Player>(),
                                                    null);
            settlement.placeSettlement(true);
            // TODO: its lame that the settlement starts with no contacts
        }

        // Join.  Remove equipment first in case role confuses placement.
        ((ServerUnit)unit).csRemoveEquipment(settlement,
            new HashSet<EquipmentType>(unit.getEquipment().keySet()),
            0, random, cs);
        unit.setLocation(settlement);
        unit.setMovesLeft(0);

        // Update with settlement tile, and newly owned tiles.
        List<FreeColGameObject> tiles = new ArrayList<FreeColGameObject>();
        tiles.addAll(settlement.getOwnedTiles());
        cs.add(See.perhaps(), tiles);

        cs.addHistory(serverPlayer, new HistoryEvent(game.getTurn(),
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
        unit.setLocation(colony);
        unit.setMovesLeft(0);
        ((ServerUnit)unit).csRemoveEquipment(colony,
            new HashSet<EquipmentType>(unit.getEquipment().keySet()),
            0, random, cs);

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
        cs.addDispose(See.perhaps().always(serverPlayer),
            settlement.getTile(), settlement);

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
        serverPlayer.csClaimLand(tile, settlement, price, cs);

        // Others can see the tile.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }


    /**
     * Accept a diplomatic trade.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is trading.
     * @param other The other <code>ServerPlayer</code> that is trading.
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> to trade with.
     * @param session a <code>DiplomacySession</code> value
     * @return An <code>Element</code> encapsulating the changes.
     */
    private Element acceptTrade(ServerPlayer serverPlayer, ServerPlayer other,
                                Unit unit, Settlement settlement,
                                DiplomacySession session) {
        ChangeSet cs = new ChangeSet();
        session.complete(cs);

        DiplomaticTrade agreement = session.getAgreement();
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
                && !serverPlayer.csChangeStance(stance, other, true, cs)) {
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
                cs.add(See.only(other), settlement.getGoodsContainer());
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
     * @param session a <code>DiplomacySession</code> value
     * @return An <code>Element</code> encapsulating the changes.
     */
    private Element rejectTrade(ServerPlayer serverPlayer, ServerPlayer other,
                                Unit unit, Settlement settlement,
                                DiplomacySession session) {
        ChangeSet cs = new ChangeSet();
        session.complete(cs);

        cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
        cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
            new DiplomacyMessage(unit, settlement, session.getAgreement()));
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
        DiplomacySession session;
        DiplomaticTrade current;
        ServerPlayer other = (ServerPlayer) settlement.getOwner();
        unit.setMovesLeft(0);

        switch (agreement.getStatus()) {
        case ACCEPT_TRADE:
            session = TransactionSession.lookup(DiplomacySession.class,
                unit, settlement);
            if (session == null) {
                return DOMMessage.clientError("Accepting without open session.");
            }
            // Act on what was proposed, not what is in the accept
            // message to frustrate tricksy client changing the conditions.
            current = session.getAgreement();
            current.setStatus(TradeStatus.ACCEPT_TRADE);

            sendElement(other, new ChangeSet().add(See.only(other),
                    ChangePriority.CHANGE_LATE,
                    new DiplomacyMessage(unit, settlement, current)));
            return acceptTrade(serverPlayer, other, unit, settlement, session);

        case REJECT_TRADE:
            session = TransactionSession.lookup(DiplomacySession.class,
                unit, settlement);
            if (session == null) {
                return DOMMessage.clientError("Rejecting without open session.");
            }
            current = session.getAgreement();
            current.setStatus(TradeStatus.REJECT_TRADE);

            sendElement(other, new ChangeSet().add(See.only(other),
                    ChangePriority.CHANGE_LATE,
                    new DiplomacyMessage(unit, settlement, current)));
            return rejectTrade(serverPlayer, other, unit, settlement, session);

        case PROPOSE_TRADE:
            current = agreement;
            session = TransactionSession.lookup(DiplomacySession.class,
                unit, settlement);
            if (session == null) {
                session = new DiplomacySession(unit, settlement);
            }
            session.setAgreement(agreement);

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
                    session);

            case PROPOSE_TRADE:
                // Save the counter-proposal, sanity test, then pass back.
                if ((ServerPlayer) agreement.getSender() == serverPlayer
                    && (ServerPlayer) agreement.getRecipient() == other) {
                    session.setAgreement(agreement);
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
                    session);
            }

        default:
            return DOMMessage.clientError("Bogus trade");
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

        cs.addSpy(See.only(serverPlayer), settlement);
        unit.setMovesLeft(0);
        cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
        return cs.build(serverPlayer);
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
        Colony colony = workLocation.getColony();
        colony.getGoodsContainer().saveState();

        if (workLocation instanceof ColonyTile) {
            Tile tile = ((ColonyTile) workLocation).getWorkTile();
            if (tile.getOwningSettlement() != colony) {
                // Claim known free land (because canAdd() succeeded).
                serverPlayer.csClaimLand(tile, colony, 0, cs);
            }
        }

        // Remove any unit equipment
        if (!unit.getEquipment().isEmpty()) {
            ((ServerUnit)unit).csRemoveEquipment(colony,
                new HashSet<EquipmentType>(unit.getEquipment().keySet()),
                0, random, cs);
          
        }

        // Check for upgrade.
        UnitType oldType = unit.getType();
        UnitTypeChange change = oldType
            .getUnitTypeChange(ChangeType.ENTER_COLONY, unit.getOwner());
        if (change != null) {
            unit.setType(change.getNewUnitType());
        }

        // Change the location.
        // We could avoid updating the whole tile if we knew that this
        // was definitely a move between locations and no student/teacher
        // interaction occurred.
        unit.setLocation(workLocation);
        cs.add(See.perhaps(), colony.getTile());
        // Others can see colony change size
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }


    /**
     * Loot cargo.
     *
     * Note loser is passed by id, as by the time we get here the unit
     * may have been sunk.
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
        LootSession session = TransactionSession.lookup(LootSession.class,
            winner.getId(), loserId);
        if (session == null) {
            return DOMMessage.clientError("Bogus looting!");
        }
        if (winner.getSpaceLeft() == 0) {
            return DOMMessage.clientError("No space to loot to: "
                + winner.getId());
        }

        ChangeSet cs = new ChangeSet();
        List<Goods> available = session.getCapture();
        if (loot == null) { // Initial inquiry
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                   new LootCargoMessage(winner, loserId, available));
        } else {
            for (Goods g : loot) {
                if (!available.contains(g)) {
                    return DOMMessage.clientError("Invalid loot: "
                        + g.toString());
                }
                available.remove(g);
                if (!winner.canAdd(g)) {
                    return DOMMessage.clientError("Loot failed: "
                        + g.toString());
                }
                winner.add(g);
            }

            // Others can see cargo capacity change.
            session.complete(cs);
            cs.add(See.perhaps(), winner);
            sendToOthers(serverPlayer, cs);
        }
        return cs.build(serverPlayer);
    }


    /**
     * Pay arrears.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param type The <code>GoodsType</code> to pay the arrears for.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element payArrears(ServerPlayer serverPlayer, GoodsType type) {
        int arrears = serverPlayer.getArrears(type);
        if (arrears <= 0) {
            return DOMMessage.clientError("No arrears for pay for: "
                + type.getId());
        } else if (!serverPlayer.checkGold(arrears)) {
            return DOMMessage.clientError("Not enough gold to pay arrears for: "
                + type.getId());
        }

        ChangeSet cs = new ChangeSet();
        Market market = serverPlayer.getMarket();
        serverPlayer.modifyGold(-arrears);
        market.setArrears(type, 0);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        cs.add(See.only(serverPlayer), market.getMarketData(type));
        // Arrears payment is private.
        return cs.build(serverPlayer);
    }


    /**
     * Equip a unit.
     * Currently the unit is either in Europe or in a settlement.
     * Might one day allow the unit to be on a tile co-located with
     * an equipment-bearing wagon.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param unit The <code>Unit</code> to equip.
     * @param type The <code>EquipmentType</code> to equip with.
     * @param amount The change in the amount of equipment.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element equipUnit(ServerPlayer serverPlayer, Unit unit,
                             EquipmentType type, int amount) {
        Settlement settlement = (unit.getTile() == null) ? null
            : unit.getSettlement();
        GoodsContainer container = null;
        boolean tileDirty = false;
        if (unit.isInEurope()) {
            // Refuse to trade in boycotted goods
            for (AbstractGoods goods : type.getGoodsRequired()) {
                GoodsType goodsType = goods.getType();
                if (!serverPlayer.canTrade(goodsType)) {
                    return DOMMessage.clientError("No equip of " + type.getId()
                        + " due to boycott of " + goodsType.getId());
                }
            }
            // Will need a fake container to contain the goods to buy
            // in Europe.  Units may not necessarily have one.
            container = new GoodsContainer(getGame(), serverPlayer.getEurope());
        } else if (settlement != null) {
            // Equipping a unit at work in a colony should remove the unit
            // from the work location.
            if (unit.getLocation() instanceof WorkLocation) {
                unit.setLocation(settlement.getTile());
                tileDirty = true;
            }
            settlement.getGoodsContainer().saveState();
        }

        ChangeSet cs = new ChangeSet();
        List<EquipmentType> remove = null;
        // Process adding equipment first, so as to settle what has to
        // be removed.
        if (amount > 0) {
            for (AbstractGoods goods : type.getGoodsRequired()) {
                GoodsType goodsType = goods.getType();
                int n = amount * goods.getAmount();
                if (unit.isInEurope()) {
                    try {
                        serverPlayer.buy(container, goodsType, n, random);
                        serverPlayer.csFlushMarket(goodsType, cs);
                    } catch (IllegalStateException e) {
                        return DOMMessage.clientError(e.getMessage());
                    }
                } else if (settlement != null) {
                    if (settlement.getGoodsCount(goodsType) < n) {
                        return DOMMessage.clientError("Failed to equip: "
                            + unit.getId() + " not enough " + goodsType
                            + " in settlement " + settlement.getId());
                    }
                    settlement.removeGoods(goodsType, n);
                }
            }
            remove = unit.changeEquipment(type, amount);
            amount = 0; // 0 => all, now
        } else if (amount < 0) {
            remove = new ArrayList<EquipmentType>();
            remove.add(type);
            amount = -amount;
        } else {
            return null; // Nothing to do.
        }

        // Now do removal of equipment.
        ((ServerUnit)unit).csRemoveEquipment(settlement, remove, amount,
                                             random, cs);

        // Nothing for others to see except if the settlement population
        // changes.
        // If in Europe, we can get away with just updating the unit
        // as sell() will have added sales changes.  In a settlement,
        // the goods container will always be dirty, but the whole tile
        // will only need to be updated if the unit moved into it.
        if (unit.getInitialMovesLeft() != unit.getMovesLeft()) {
            unit.setMovesLeft(0);
        }
        if (unit.isInEurope()) {
            cs.add(See.only(serverPlayer), unit);
            cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        } else if (settlement != null) {
            if (tileDirty) {
                cs.add(See.perhaps(), settlement.getTile());
            } else {
                cs.add(See.only(serverPlayer), unit,
                       settlement.getGoodsContainer());
            }
        }
        return cs.build(serverPlayer);
    }


    /**
     * Pay for a building.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the colony.
     * @param colony The <code>Colony</code> that is building.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element payForBuilding(ServerPlayer serverPlayer, Colony colony) {
        BuildableType build = colony.getCurrentlyBuilding();
        if (build == null) {
            return DOMMessage.clientError("Colony " + colony.getId()
                + " is not building anything!");
        }
        HashMap<GoodsType, Integer> required
            = colony.getGoodsForBuilding(build);
        int price = colony.priceGoodsForBuilding(required);
        if (!serverPlayer.checkGold(price)) {
            return DOMMessage.clientError("Insufficient funds to pay for build.");
        }

        // Save the correct final gold for the player, as we are going to
        // use buy() below, but it deducts the normal uninflated price for
        // the goods being bought.  We restore this correct amount later.
        int savedGold = serverPlayer.modifyGold(-price);
        serverPlayer.modifyGold(price);

        ChangeSet cs = new ChangeSet();
        GoodsContainer container = colony.getGoodsContainer();
        container.saveState();
        for (GoodsType type : required.keySet()) {
            int amount = required.get(type);
            if (type.isStorable()) {
                // TODO: should also check canTrade(type, Access.?)
                serverPlayer.buy(container, type, amount, random);
                serverPlayer.csFlushMarket(type, cs);
            } else {
                container.addGoods(type, amount);
            }
        }
        colony.invalidateCache();

        // Nothing to see for others, colony internal.
        serverPlayer.setGold(savedGold);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        cs.add(See.only(serverPlayer), container);
        return cs.build(serverPlayer);
    }


    /**
     * Indians making demands of a colony.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is demanding.
     * @param unit The <code>Unit</code> making the demands.
     * @param colony The <code>Colony</code> that is demanded of.
     * @param goods The <code>Goods</code> being demanded.
     * @param gold The amount of gold being demanded.
     * @param result The result of the demand.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element indianDemand(ServerPlayer serverPlayer, Unit unit,
                                Colony colony, Goods goods, int gold,
                                String result) {
        ServerPlayer demander = (ServerPlayer) unit.getOwner();
        ServerPlayer victim = (ServerPlayer) colony.getOwner();
        DemandSession session = TransactionSession.lookup(DemandSession.class,
            unit, colony);
        int difficulty = getGame().getSpecification()
            .getIntegerOption("model.option.nativeDemands").getValue();
        ChangeSet cs = new ChangeSet();

        if (session == null) { // Initial demand
            if (serverPlayer != demander) {
                return DOMMessage.clientError("Colony player can not demand");
            }
            if (!victim.isConnected()) {
                return DOMMessage.clientError("No connection to colony owner");
            }
            session = new DemandSession(unit, colony);
            session.setGoods(goods);
            session.setGold(gold);
            session.setTension((difficulty + 1) * 50);

            cs.add(See.only(victim), ChangePriority.CHANGE_NORMAL,
                new IndianDemandMessage(unit, colony, goods, gold));
            sendElement(victim, cs);
            logger.info(demander.getName() + " unit " + unit
                + " demands " + goods + " goods and " + gold + " gold "
                + " from " + colony.getName());
            return null; // Do not block waiting for response.
        }

        // Reply to demand
        if (serverPlayer != victim) {
            return DOMMessage.clientError("Native player can not reply");
        }
        if (session.getGold() != gold || session.getGoods() != goods) {
            return DOMMessage.clientError("Transaction mismatch, cheat?");
        }
        logger.info(demander.getName() + " unit " + unit
            + " demand from colony " + colony.getName()
            + " result: " + result);
        if (Boolean.TRUE.toString().equals(result)) {
            if (goods != null) {
                GoodsContainer colonyContainer = colony.getGoodsContainer();
                colonyContainer.saveState();
                GoodsContainer unitContainer = unit.getGoodsContainer();
                unitContainer.saveState();
                moveGoods(goods, unit);
                cs.add(See.only(victim), colonyContainer);
                cs.add(See.only(demander), unitContainer);
            }
            if (gold > 0) {
                victim.modifyGold(-gold);
                demander.modifyGold(gold);
                cs.addPartial(See.only(victim), victim, "gold");
                cs.addPartial(See.only(demander), demander, "gold");
            }
            cs.add(See.only(null).perhaps(victim),
                demander.modifyTension(victim, -(5 - difficulty) * 50));
            session.setTension(-1); // Disarm the complete() routine.
        }
        session.complete(cs);
        sendElement(victim, cs);
        return cs.build(serverPlayer);
    }


    /**
     * Train a unit in Europe.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is demanding.
     * @param type The <code>UnitType</code> to train.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element trainUnitInEurope(ServerPlayer serverPlayer, UnitType type) {
        Europe europe = serverPlayer.getEurope();
        if (europe == null) {
            return DOMMessage.clientError("No Europe to train in.");
        }
        int price = europe.getUnitPrice(type);
        if (price <= 0) {
            return DOMMessage.clientError("Bogus price: " + price);
        } else if (!serverPlayer.checkGold(price)) {
            return DOMMessage.clientError("Not enough gold to train " + type);
        }

        new ServerUnit(getGame(), europe, serverPlayer, type);
        serverPlayer.modifyGold(-price);
        ((ServerEurope) europe).increasePrice(type, price);

        // Only visible in Europe
        ChangeSet cs = new ChangeSet();
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        cs.add(See.only(serverPlayer), europe);
        return cs.build(serverPlayer);
    }


    /**
     * Set build queue.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the colony.
     * @param colony The <code>Colony</code> to set the queue of.
     * @param queue The new build queue.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element setBuildQueue(ServerPlayer serverPlayer, Colony colony,
                                 List<BuildableType> queue) {
        colony.setBuildQueue(queue);

        // Only visible to player.
        ChangeSet cs = new ChangeSet();
        cs.add(See.only(serverPlayer), colony);
        return cs.build(serverPlayer);
    }


    /**
     * Set goods levels.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the colony.
     * @param colony The <code>Colony</code> to set the goods levels in.
     * @param exportData The new <code>ExportData</code>.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element setGoodsLevels(ServerPlayer serverPlayer, Colony colony,
                                  ExportData exportData) {
        colony.setExportData(exportData);
        return new ChangeSet().add(See.only(serverPlayer), colony)
            .build(serverPlayer);
    }


    /**
     * Put outside colony.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param unit The <code>Unit</code> to be put out.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element putOutsideColony(ServerPlayer serverPlayer, Unit unit) {
        Tile tile = unit.getTile();
        Colony colony = unit.getColony();
        unit.setLocation(tile);

        // Full tile update for the player, the rest get their limited
        // view of the colony so that population changes.
        ChangeSet cs = new ChangeSet();
        cs.add(See.only(serverPlayer), tile);
        cs.add(See.perhaps().except(serverPlayer), colony);
        return cs.build(serverPlayer);
    }


    /**
     * Change work type.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param unit The <code>Unit</code> to change the work type of.
     * @param type The new <code>GoodsType</code> to produce.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element changeWorkType(ServerPlayer serverPlayer, Unit unit,
                                  GoodsType type) {
        if (unit.getWorkType() != type) {
            unit.setExperience(0);
            unit.setWorkType(type);
        }

        // Private update of the unit.
        return new ChangeSet().add(See.only(serverPlayer), unit)
            .build(serverPlayer);
    }


    /**
     * Change improvement work type.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param unit The <code>Unit</code> to change the work type of.
     * @param type The new <code>TileImprovementType</code> to produce.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element changeWorkImprovementType(ServerPlayer serverPlayer,
                                             Unit unit,
                                             TileImprovementType type) {
        Tile tile = unit.getTile();
        TileImprovement improvement = tile.findTileImprovementType(type);
        if (improvement == null) { // Create the new improvement.
            improvement = new TileImprovement(getGame(), tile, type);
            tile.add(improvement);
        }

        //unit.setWorkImprovement(improvement);
        unit.setMission(new ImprovementMission(unit, improvement));
        unit.setState(UnitState.IMPROVING);

        // Private update of the tile.
        return new ChangeSet().add(See.only(serverPlayer), tile)
            .build(serverPlayer);
    }


    /**
     * Change a units state.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param unit The <code>Unit</code> to change the state of.
     * @param state The new <code>UnitState</code>.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element changeState(ServerPlayer serverPlayer, Unit unit,
                               UnitState state) {
        ChangeSet cs = new ChangeSet();

        if (state == UnitState.FORTIFYING) {
            Tile tile = unit.getTile();
            ServerColony colony
                = (tile.getOwningSettlement() instanceof Colony)
                ? (ServerColony) tile.getOwningSettlement()
                : null;
            if (colony != null
                && colony.getOwner() != unit.getOwner()
                && colony.isTileInUse(tile)) {
                colony.csEvictUser(unit, cs);
            }
        }

        unit.setState(state);
        cs.add(See.perhaps(), unit.getTile());

        // Others might be able to see the unit.
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }


    /**
     * Assign a student to a teacher.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param student The student <code>Unit</code>.
     * @param teacher The teacher <code>Unit</code>.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element assignTeacher(ServerPlayer serverPlayer, Unit student,
                                 Unit teacher) {
        Unit oldStudent = teacher.getStudent();
        Unit oldTeacher = student.getTeacher();

        // Only update units that changed their teaching situation.
        ChangeSet cs = new ChangeSet();
        if (oldTeacher != null) {
            oldTeacher.setStudent(null);
            cs.add(See.only(serverPlayer), oldTeacher);
        }
        if (oldStudent != null) {
            oldStudent.setTeacher(null);
            cs.add(See.only(serverPlayer), oldStudent);
        }
        teacher.setStudent(student);
        teacher.setWorkType(null);
        student.setTeacher(teacher);
        cs.add(See.only(serverPlayer), student, teacher);
        return cs.build(serverPlayer);
    }


    /**
     * Assign a trade route to a unit.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that owns the unit.
     * @param unit The unit <code>Unit</code> to assign to.
     * @param tradeRoute The <code>TradeRoute</code> to assign.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element assignTradeRoute(ServerPlayer serverPlayer, Unit unit,
                                    TradeRoute tradeRoute) {
        unit.setTradeRoute(tradeRoute);
        unit.setMission(null);
        if (tradeRoute != null) {
            List<Stop> stops = tradeRoute.getStops();
            int found = -1;
            for (int i = 0; i < stops.size(); i++) {
                if (unit.getLocation() == stops.get(i).getLocation()) {
                    found = i;
                    break;
                }
            }
            if (found < 0) found = 0;
            unit.setCurrentStop(found);
            unit.setMission(new GoToMission(unit, stops.get(found).getLocation()));
        }

        // Only visible to the player
        return new ChangeSet().add(See.only(serverPlayer), unit)
            .build(serverPlayer);
    }

    /**
     * Set trade routes for a player.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to set trade
     *    routes for.
     * @param routes The new list of <code>TradeRoute</code>s.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element setTradeRoutes(ServerPlayer serverPlayer,
                                  List<TradeRoute> routes) {
        serverPlayer.setTradeRoutes(routes);
        // Have to update the whole player alas.
        return new ChangeSet().add(See.only(serverPlayer), serverPlayer)
            .build(serverPlayer);
    }

    /**
     * Get a new trade route for a player.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to get a trade
     *    route for.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element getNewTradeRoute(ServerPlayer serverPlayer) {
        List<TradeRoute> routes
            = new ArrayList<TradeRoute>(serverPlayer.getTradeRoutes());
        TradeRoute route = new TradeRoute(getGame(), "", serverPlayer);
        routes.add(route);
        serverPlayer.setTradeRoutes(routes);
        return new ChangeSet().addTradeRoute(serverPlayer, route)
            .build(serverPlayer);
    }


    /**
     * Get a list of abstract REF units for a player.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to query the REF of.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element getREFUnits(ServerPlayer serverPlayer) {
        Game game = getGame();
        Specification spec = game.getSpecification();
        List<AbstractUnit> units = new ArrayList<AbstractUnit>();
        UnitType defaultType = spec.getUnitType("model.unit.freeColonist");

        if (serverPlayer.getMonarch() == null) {
            ServerPlayer REFPlayer = (ServerPlayer) serverPlayer.getREFPlayer();
            java.util.Map<UnitType, EnumMap<Role, Integer>> unitHash
                = new HashMap<UnitType, EnumMap<Role, Integer>>();
            for (Unit unit : REFPlayer.getUnits()) {
                if (unit.isOffensiveUnit()) {
                    UnitType unitType = defaultType;
                    if (unit.getType().getOffence() > 0
                        || unit.hasAbility(Ability.EXPERT_SOLDIER)) {
                        unitType = unit.getType();
                    }
                    EnumMap<Role, Integer> roleMap = unitHash.get(unitType);
                    if (roleMap == null) {
                        roleMap = new EnumMap<Role, Integer>(Role.class);
                    }
                    Role role = unit.getRole();
                    Integer count = roleMap.get(role);
                    if (count == null) {
                        roleMap.put(role, new Integer(1));
                    } else {
                        roleMap.put(role, new Integer(count.intValue() + 1));
                    }
                    unitHash.put(unitType, roleMap);
                }
            }
            for (java.util.Map.Entry<UnitType, EnumMap<Role, Integer>> typeEntry : unitHash.entrySet()) {
                for (java.util.Map.Entry<Role, Integer> roleEntry : typeEntry.getValue().entrySet()) {
                    units.add(new AbstractUnit(typeEntry.getKey(), roleEntry.getKey(), roleEntry.getValue()));
                }
            }
        } else {
            units = serverPlayer.getMonarch().getREF();
        }

        ChangeSet cs = new ChangeSet();
        cs.addTrivial(See.only(serverPlayer), "getREFUnits",
                      ChangePriority.CHANGE_NORMAL);
        Element reply = cs.build(serverPlayer);
        // TODO: eliminate explicit Element hackery
        for (AbstractUnit unit : units) {
            reply.appendChild(unit.toXMLElement(serverPlayer,
                                                reply.getOwnerDocument()));
        }
        return reply;
    }


    /**
     * Gets the list of high scores.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is querying.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element getHighScores(ServerPlayer serverPlayer) {
        ChangeSet cs = new ChangeSet();
        cs.addTrivial(See.only(serverPlayer), "getHighScores",
                      ChangePriority.CHANGE_NORMAL);
        Element reply = cs.build(serverPlayer);
        for (HighScore score : getFreeColServer().getHighScores()) {
            reply.appendChild(score.toXMLElement(serverPlayer,
                                                 reply.getOwnerDocument()));
        }
        return reply;
    }


    /**
     * Chat.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that is chatting.
     * @param message The chat message.
     * @param pri A privacy setting, currently a noop.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element chat(ServerPlayer serverPlayer, String message,
                        boolean pri) {
        sendToOthers(serverPlayer,
                     new ChatMessage(serverPlayer, message, false)
                     .toXMLElement());
        return null;
    }

    /**
     * Get the current game statistics.
     *
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element getStatistics(ServerPlayer serverPlayer) {
        // Convert statistics map to a list.
        java.util.Map<String, String> stats = getGame()
            .getStatistics(getFreeColServer().getAIMain());
        List<String> all = new ArrayList<String>();
        List<String> keys = new ArrayList<String>(stats.keySet());
        Collections.sort(keys);
        for (String k : keys) {
            all.add(k);
            all.add(stats.get(k));
        }

        // Return as statistics element.
        ChangeSet cs = new ChangeSet();
        cs.addTrivial(See.only(serverPlayer), "statistics",
                      ChangePriority.CHANGE_NORMAL,
                      all.toArray(new String[0]));
        return cs.build(serverPlayer);
    }

    /**
     * Enters revenge mode against those evil AIs.
     *
     * @param serverPlayer The <code>ServerPlayer</code> entering revenge mode.
     * @return An <code>Element</code> encapsulating this action.
     */
    public Element enterRevengeMode(ServerPlayer serverPlayer) {
        if (!getFreeColServer().isSingleplayer()) {
            return DOMMessage.clientError("Can not enter revenge mode,"
                + " as this is not a single player game.");
        }
        Game game = getGame();
        List<UnitType> undeads = game.getSpecification()
            .getUnitTypesWithAbility("model.ability.undead");
        List<UnitType> navalUnits = new ArrayList<UnitType>();
        List<UnitType> landUnits = new ArrayList<UnitType>();
        for (UnitType undead : undeads) {
            if (undead.hasAbility(Ability.NAVAL_UNIT)) {
                navalUnits.add(undead);
            } else if (undead.hasAbility("model.ability.multipleAttacks")) {
                landUnits.add(undead);
            }
        }
        if (navalUnits.size() == 0 || landUnits.size() == 0) {
            return DOMMessage.clientError("Can not enter revenge mode,"
                + " because we can not find the undead units.");
        }

        ChangeSet cs = new ChangeSet();
        UnitType navalType = navalUnits.get(Utils.randomInt(logger,
                "Choose undead navy", random, navalUnits.size()));
        Tile start = ((Tile) serverPlayer.getEntryLocation())
            .getSafeTile(serverPlayer, random);
        Unit theFlyingDutchman
            = new ServerUnit(game, start, serverPlayer, navalType);
        UnitType landType = landUnits.get(Utils.randomInt(logger,
                "Choose undead army", random, landUnits.size()));
        new ServerUnit(game, theFlyingDutchman, serverPlayer, landType);
        serverPlayer.setDead(false);
        serverPlayer.setPlayerType(PlayerType.UNDEAD);

        // No one likes the undead.
        for (Player p : game.getPlayers()) {
            if (serverPlayer != (ServerPlayer) p
                && serverPlayer.hasContacted(p)) {
                serverPlayer.csChangeStance(Stance.WAR, p, true, cs);
            }
        }

        // Others can tell something has happened to the player,
        // and possibly see the units.
        cs.add(See.all(), serverPlayer);
        cs.add(See.perhaps(), start);
        sendToOthers(serverPlayer, cs);
        return cs.build(serverPlayer);
    }

    private String getNonPlayerNation() {
        int nations = Nation.EUROPEAN_NATIONS.length;
        int start = random.nextInt(nations);
        for (int index = 0; index < nations; index++) {
            String nationId = "model.nation." + Nation.EUROPEAN_NATIONS[(start+index)%nations];
            if (getGame().getPlayer(nationId) == null) {
                return nationId + ".name";
            }
        }
        // this should never happen
        return "";
    }

}
