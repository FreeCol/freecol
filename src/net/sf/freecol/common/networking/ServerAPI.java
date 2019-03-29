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


package net.sf.freecol.common.networking;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Constants.IndianDemandAction;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Game.LogoutReason;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.NativeTrade;
import net.sf.freecol.common.model.NativeTrade.NativeTradeAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.option.OptionGroup;


/**
 * The API for client/server messaging.
 */
public abstract class ServerAPI {

    private static final Logger logger = Logger.getLogger(ServerAPI.class.getName());


    /**
     * Creates a new {@code ServerAPI}.
     */
    public ServerAPI() {}


    /**
     * Connects a client to host:port (or more).
     *
     * @param name The name for the thread.
     * @param host The name of the machine running the
     *     {@code FreeColServer}.
     * @param port The port to use when connecting to the host.
     * @return True if the connection succeeded.
     * @exception IOException on connection failure.
     */
    public abstract Connection connect(String name, String host, int port)
        throws IOException;

    /**
     * Disconnect from the server.
     *
     * @return True if disconnected.
     */
    public abstract boolean disconnect();

    /**
     * Reconnect to the server.
     *
     * @return The reestablished {@code Connection}.
     * @exception IOException on failure to connect.
     */
    public abstract Connection reconnect() throws IOException;

    /**
     * Get the connection to communicate with the server.
     *
     * @return The {@code Connection} to the server.
     */
    public abstract Connection getConnection();


    // Utilities

    /**
     * Sets the message handler for the connection.
     *
     * @param mh The new {@code MessageHandler}.
     */
    public void setMessageHandler(MessageHandler mh) {
        Connection c = getConnection();
        if (c != null) c.setMessageHandler(mh);
    }
    
    /**
     * Convenience utility to check the connection.
     *
     * @return True if the server connection is open.
     */
    public boolean isConnected() {
        return getConnection() != null;
    }


    // Internal message passing routines

    /**
     * Check the connection.
     *
     * @param operation The proposed operation.
     * @param type The proposed type of message.
     * @return The {@code Connection} if valid, or null if not.
     */
    private Connection check(String operation, String type) {
        final Connection c = getConnection();
        if (c == null) {
            logger.log(Level.WARNING, "Not connected, did not " + operation
                + ": " + type);
        }
        return c;
    }

    /**
     * Sends a message to the server.
     *
     * @param message The {@code Message} to send.
     * @return True if the send succeeded.
     */
    private boolean send(Message message) {
        if (message == null) return true;
        final Connection c = check("send", message.getType());
        if (c != null) {
            try {
                c.send(message);
                return true;
            } catch (FreeColException|IOException|XMLStreamException ex) {
                logger.log(Level.WARNING, "Failed to send", ex);
            }
        }
        return false;
    }

    /**
     * Sends the specified message to the server and processes the reply.
     *
     * In following routines we follow the convention that server I/O
     * is confined to the ask<em>foo</em>() routine, which typically
     * returns true if the server interaction succeeded, which does
     * *not* necessarily imply that the actual substance of the
     * request was allowed (e.g. a move may result in the death of a
     * unit rather than actually moving).
     *
     * @param message A {@code Message} to send.
     * @return True if the server interaction succeeded, that is, there was
     *     no I/O problem and the reply was not an error message.
     */
    private boolean ask(Message message) {
        if (message == null) return true;
        final Connection c = check("ask", message.getType());
        if (c != null) {
            try {
                c.request(message);
                return true;
            } catch (FreeColException|IOException|XMLStreamException ex) {
                logger.log(Level.WARNING, "Failed to ask", ex);
            }
        }
        return false;
                
    }


    // Public messaging routines for game actions

    /**
     * Server query-response to abandon a colony.
     *
     * @param colony The {@code Colony} to abandon.
     * @return True if the server interaction succeeded.
     */
    public boolean abandonColony(Colony colony) {
        return ask(new AbandonColonyMessage(colony));
    }

    /**
     * Server query-response to respond to a monarch offer.
     *
     * @param action The monarch action responded to.
     * @param accept Accept or reject the offer.
     * @return True if the server interaction succeeded.
     */
    public boolean answerMonarch(MonarchAction action, boolean accept) {
        return ask(new MonarchActionMessage(action, null, "")
                       .setResult(accept));
    }

    /**
     * Server query-response for finding out the skill taught at a settlement.
     *
     * @param unit The {@code Unit} that is asking.
     * @param direction The direction to a settlement to ask.
     * @return True if the server interaction succeeded.
     */
    public boolean askSkill(Unit unit, Direction direction) {
        return ask(new AskSkillMessage(unit, direction));
    }

    /**
     * Server query-response for assigning a teacher.
     *
     * @param student The student {@code Unit}.
     * @param teacher The teacher {@code Unit}.
     * @return True if the server interaction succeeded.
     */
    public boolean assignTeacher(Unit student, Unit teacher) {
        return ask(new AssignTeacherMessage(student, teacher));
    }

    /**
     * Server query-response for assigning a trade route to a unit.
     *
     * @param unit The {@code Unit} to assign a trade route to.
     * @param tradeRoute The {@code TradeRoute} to assign.
     * @return True if the server interaction succeeded.
     */
    public boolean assignTradeRoute(Unit unit, TradeRoute tradeRoute) {
        return ask(new AssignTradeRouteMessage(unit, tradeRoute));
    }

    /**
     * Server query-response for attacking.
     *
     * @param unit The {@code Unit} to perform the attack.
     * @param direction The direction in which to attack.
     * @return True if the server interaction succeeded.
     */
    public boolean attack(Unit unit, Direction direction) {
        return ask(new AttackMessage(unit, direction));
    }

    /**
     * Server query-response for building a colony.
     *
     * @param name The name for the colony.
     * @param unit The {@code Unit} that will build.
     * @return True if the server interaction succeeded.
     */
    public boolean buildColony(String name, Unit unit) {
        return ask(new BuildColonyMessage(name, unit));
    }

    /**
     * Server query-response to cash in a treasure train.
     *
     * @param unit The treasure train {@code Unit} to cash in.
     * @return True if the server interaction succeeded.
     */
    public boolean cashInTreasureTrain(Unit unit) {
        return ask(new CashInTreasureTrainMessage(unit));
    }

    /**
     * Server query-response for changing unit state.
     *
     * @param unit The {@code Unit} to change the state of.
     * @param state The new {@code UnitState}.
     * @return boolean <b>true</b> if the server interaction succeeded.
     */
    public boolean changeState(Unit unit, UnitState state) {
        return ask(new ChangeStateMessage(unit, state));
    }

    /**
     * Server query-response for changing work improvement type.
     *
     * @param unit The {@code Unit} to change the work type of.
     * @param type The new {@code TileImprovementType} to work on.
     * @return True if the server interaction succeeded.
     */
    public boolean changeWorkImprovementType(Unit unit,
                                             TileImprovementType type) {
        return ask(new ChangeWorkImprovementTypeMessage(unit, type));
    }

    /**
     * Server query-response for changing work type.
     *
     * @param unit The {@code Unit} to change the work type of.
     * @param workType The new {@code GoodsType} to produce.
     * @return True if the server interaction succeeded.
     */
    public boolean changeWorkType(Unit unit, GoodsType workType) {
        return ask(new ChangeWorkTypeMessage(unit, workType));
    }

    /**
     * Send a chat message (pre and in-game).
     *
     * @param player The {@code Player} to chat to.
     * @param chat The text of the message.
     * @return True if the send succeeded.
     */
    public boolean chat(Player player, String chat) {
        return send(new ChatMessage(player, chat, false));
    }

    /**
     * Send a chooseFoundingFather message.
     *
     * @param ffs A list of {@code FoundingFather}s to choose from.
     * @param ff The chosen {@code FoundingFather} (may be null).
     * @return True if the send succeeded.
     */
    public boolean chooseFoundingFather(List<FoundingFather> ffs,
                                        FoundingFather ff) {
        return ask(new ChooseFoundingFatherMessage(ffs, ff));
    }

    /**
     * Server query-response to claim a tile.
     *
     * @param tile The {@code Tile} to claim.
     * @param claimant The {@code Unit} or {@code Settlement} that is
     *     claiming the tile.
     * @param price The amount to pay.
     * @return True if the server interaction succeeded.
     */
    public boolean claimTile(Tile tile, FreeColGameObject claimant, int price) {
        return ask(new ClaimLandMessage(tile, claimant, price));
    }

    /**
     * Server query-response for clearing a unit speciality.
     *
     * @param unit The {@code Unit} to operate on.
     * @return True if the server interaction succeeded.
     */
    public boolean clearSpeciality(Unit unit) {
        return ask(new ClearSpecialityMessage(unit));
    }

    /**
     * Server query-response to continue with a won game.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean continuePlaying() {
        return send(TrivialMessage.continueMessage);
    }

    /**
     * Server query-response for declaring independence.
     *
     * @param nation The name for the new nation.
     * @param country The name for the new country.
     * @return True if the server interaction succeeded.
     */
    public boolean declareIndependence(String nation, String country) {
        return ask(new DeclareIndependenceMessage(nation, country));
    }

    /**
     * Server query-response for the special case of deciding to
     * explore a rumour but then declining not to investigate the
     * strange mounds.
     *
     * @param unit The {@code Unit} that is exploring.
     * @param direction The {@code Direction} to move.
     * @return True if the server interaction succeeded.
     */
    public boolean declineMounds(Unit unit, Direction direction) {
        return ask(new DeclineMoundsMessage(unit, direction));
    }

    /**
     * Server query-response for deleting a trade route.
     *
     * @param tradeRoute The {@code TradeRoute} to delete.
     * @return True if the server interaction succeeded.
     */
    public boolean deleteTradeRoute(TradeRoute tradeRoute) {
        return ask(new DeleteTradeRouteMessage(tradeRoute));
    }

    /**
     * Server query-response to give the given goods to the natives.
     *
     * @param unit The {@code Unit} that is trading.
     * @param is The {@code IndianSettlement} that is trading.
     * @param goods The {@code Goods} to give.
     * @return True if the server interaction succeeded.
     */
    public boolean deliverGiftToSettlement(Unit unit, IndianSettlement is,
                                           Goods goods) {
        return ask(new DeliverGiftMessage(unit, is, goods));
    }

    /**
     * Server query-response for demanding a tribute from a native
     * settlement.
     *
     * @param unit The {@code Unit} that demands.
     * @param direction The direction to demand in.
     * @return True if the server interaction succeeded.
     */
    public boolean demandTribute(Unit unit, Direction direction) {
        return ask(new DemandTributeMessage(unit, direction));
    }

    /**
     * Handler server query-response for diplomatic messages.
     *
     * @param our Our object ({@code Unit} or {@code Colony})
     *     conducting the diplomacy.
     * @param other The other object ({@code Unit} or {@code Colony})
     *     to negotiate with.
     * @param dt The {@code DiplomaticTrade} agreement to propose.
     * @return The resulting agreement or null if none present.
     */
    public boolean diplomacy(FreeColGameObject our, FreeColGameObject other, 
                             DiplomaticTrade dt) {
        return ask(new DiplomacyMessage(our, other, dt));
    }

    /**
     * Server query-response for disbanding a unit.
     *
     * @param unit The {@code Unit} to operate on.
     * @return True if the server interaction succeeded.
     */
    public boolean disbandUnit(Unit unit) {
        return ask(new DisbandUnitMessage(unit));
    }

    /**
     * Server query-response for disembarking from a carrier.
     *
     * @param unit The {@code Unit} that is disembarking.
     * @return True if the server interaction succeeded.
     */
    public boolean disembark(Unit unit) {
        return ask(new DisembarkMessage(unit));
    }

    /**
     * Server query-response for boarding a carrier.
     *
     * @param unit The {@code Unit} that is boarding.
     * @param carrier The carrier {@code Unit}.
     * @param direction An optional direction if the unit is boarding from
     *        an adjacent tile, or null if from the same tile.
     * @return True if the server interaction succeeded.
     */
    public boolean embark(Unit unit, Unit carrier, Direction direction) {
        return ask(new EmbarkMessage(unit, carrier, direction));
    }

    /**
     * Server query-response for emigration.
     *
     * @param slot The slot from which the unit migrates, 1-3 selects
     *             a specific one, otherwise the server will choose one.
     * @return True if the client-server interaction succeeded.
     */
    public boolean emigrate(int slot) {
        return ask(new EmigrateUnitMessage(slot));
    }

    /**
     * Server query-response for asking for the turn to end.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean endTurn() {
        return ask(TrivialMessage.endTurnMessage);
    }

    /**
     * Server query-response for asking to enter revenge mode (post-game).
     *
     * @return True if the server interaction succeeded.
     */
    public boolean enterRevengeMode() {
        return ask(TrivialMessage.enterRevengeModeMessage);
    }

    /**
     * Server query-response for equipping a unit for a role.
     *
     * @param unit The {@code Unit} to equip.
     * @param role The {@code Role} to assume.
     * @param roleCount The role count.
     * @return True if the server interaction succeeded.
     */
    public boolean equipUnitForRole(Unit unit, Role role, int roleCount) {
        return ask(new EquipForRoleMessage(unit, role, roleCount));
    }

    /**
     * Server query-response for responding to a first contact message.
     *
     * @param player The {@code Player} making contact.
     * @param other The native {@code Player} being contacted.
     * @param tile An optional {@code Tile} to offer the player if
     *     they have made a first landing.
     * @param result Whether the initial peace treaty was accepted.
     * @return True if the server interaction succeeded.
     */
    public boolean firstContact(Player player, Player other, Tile tile,
                                boolean result) {
        return ask(new FirstContactMessage(player, other, tile)
                       .setResult(result));
    }

    /**
     * Server query-response for asking for the high scores list.
     *
     * @param key The high score key to query.
     * @return True if the server interaction succeeded.
     */
    public boolean getHighScores(String key) {
        return ask(new HighScoresMessage(key, null));
    }

    /**
     * Server query-response for asking for the nation summary of a player.
     *
     * @param self The {@code Player} requesting the summary.
     * @param player The {@code Player} to summarize.
     * @return True if the server interaction succeeded.
     */
    public boolean nationSummary(Player self, Player player) {
        return ask(new NationSummaryMessage(player, null));
    }

    /**
     * Server query-response for inciting the natives.
     *
     * @param unit The missionary {@code Unit}.
     * @param is The {@code IndianSettlement} to incite.
     * @param enemy An enemy {@code Player}.
     * @param gold The amount of bribe, negative to enquire.
     * @return True if the server interaction succeeded.
     */
    public boolean incite(Unit unit, IndianSettlement is, Player enemy,
                          int gold) {
        return ask(new InciteMessage(unit, is, enemy, gold));
    }

    /**
     * Makes demands to a colony.  One and only one of goods or gold is valid.
     *
     * @param unit The {@code Unit} that is demanding.
     * @param colony The {@code Colony} to demand of.
     * @param type The {@code GoodsType} to demand.
     * @param amount The amount of goods to demand.
     * @param result The result of the demand.
     * @return True if the server interaction succeeded.
     */
    public boolean indianDemand(Unit unit, Colony colony,
                                GoodsType type, int amount,
                                IndianDemandAction result) {
        return ask(new IndianDemandMessage(unit, colony, type, amount)
                       .setResult(result));
    }
            
    /**
     * Server query-response for joining a colony.
     *
     * @param unit The {@code Unit} that will join.
     * @param colony The {@code Colony} to join.
     * @return True if the server interaction succeeded.
     */
    public boolean joinColony(Unit unit, Colony colony) {
        return ask(new JoinColonyMessage(colony, unit));
    }

    /**
     * Server query-response for learning the skill taught at a settlement.
     *
     * @param unit The {@code Unit} that is asking.
     * @param direction The direction to a settlement to ask.
     * @return True if the server interaction succeeded.
     */
    public boolean learnSkill(Unit unit, Direction direction) {
        return ask(new LearnSkillMessage(unit, direction));
    }

    /**
     * Server query-response for loading goods.
     *
     * @param loc The {@code Location} where the goods are.
     * @param type The {@code GoodsType} to load.
     * @param amount The amount of goods to load.
     * @param carrier The {@code Unit} to load onto.
     * @return True if the server interaction succeeded.
     */
    public boolean loadGoods(Location loc, GoodsType type, int amount,
                             Unit carrier) {
        return ask(new LoadGoodsMessage(loc, type, amount, carrier));
    }

    /**
     * Server query-response for logging in a player (pre-game).
     *
     * @param userName The user name.
     * @param version The client version.
     * @param single True if this is a single player login.
     * @param current True if the requesting player should become the
     *     current player.
     * @return True if the server interaction succeeded.
     */
    public boolean login(String userName, String version,
                         boolean single, boolean current) {
        return ask(new LoginMessage(null, userName, version, null,
                                    single, current, null));
    }

    /**
     * Server query-response for logging out.
     *
     * @param player The {@code Player} that has logged out.
     * @param reason The reason for logging out.
     * @return True if the server interaction succeeded.
     */
    public boolean logout(Player player, LogoutReason reason) {
        return ask(new LogoutMessage(player, reason));
    }

    /**
     * Server query-response for looting.  Handles both an initial query and
     * the actual looting.
     *
     * @param winner The {@code Unit} that is looting.
     * @param defenderId The identifier of the defender unit (it may have sunk).
     * @param goods A list of {@code Goods}, if empty this is a query
     *     as to what is to be looted which is filled into the list,
     *     if non-empty, then the list of goods to loot.
     * @return True if the server interaction succeeded.
     */
    public boolean loot(Unit winner, String defenderId, List<Goods> goods) {
        return ask(new LootCargoMessage(winner, defenderId, goods));
    }

    /**
     * Server query-response for establishing/denouncing a mission.
     *
     * @param unit The missionary {@code Unit}.
     * @param direction The direction to a settlement to establish with.
     * @param denounce True if this is a denouncement.
     * @return True if the server interaction succeeded.
     */
    public boolean missionary(Unit unit, Direction direction,
                              boolean denounce) {
        return ask(new MissionaryMessage(unit, direction, denounce));
    }

    /**
     * Server query-response for moving a unit.
     *
     * @param unit The {@code Unit} to move.
     * @param direction The direction to move in.
     * @return True if the server interaction succeeded.
     */
    public boolean move(Unit unit, Direction direction) {
        return ask(new MoveMessage(unit, direction));
    }

    /**
     * Server query-response for moving to across the high seas.
     *
     * @param unit The {@code Unit} to move.
     * @param destination The {@code Location} to move to.
     * @return True if the server interaction succeeded.
     */
    public boolean moveTo(Unit unit, Location destination) {
        return ask(new MoveToMessage(unit, destination));
    }

    /**
     * A native Unit delivers the gift it is carrying to a colony.
     *
     * @param unit The {@code Unit} delivering the gift.
     * @param colony The {@code Colony} to give to.
     * @return True if the server interaction succeeded.
     */
    public boolean nativeGift(Unit unit, Colony colony) {
        return ask(new NativeGiftMessage(unit, colony));
    }

    /**
     * Server query-response for naming a new land.
     *
     * @param unit The {@code Unit} that has come ashore.
     * @param name The new land name.
     * @return True if the server interaction succeeded.
     */
    public boolean newLandName(Unit unit, String name) {
        return ask(new NewLandNameMessage(unit, name));
    }

    /**
     * Server query-response for simple native trades.
     *
     * @param nt The {@code NativeTrade} underway.
     * @param action The {@code NativeTradeAction} to perform.
     * @return True if the server interaction succeeded.
     */
    public boolean nativeTrade(NativeTradeAction action, NativeTrade nt) {
        return ask(new NativeTradeMessage(action, nt));
    }

    /**
     * Server query-response to get the session for a trade.
     *
     * @param unit The {@code Unit} that is trading.
     * @param is The {@code IndianSettlement} that is trading.
     * @return True if the server interaction succeeded.
     */
    public boolean newNativeTradeSession(Unit unit, IndianSettlement is) {
        return ask(new NativeTradeMessage(unit, is));
    }

    /**
     * Server query-response for naming a new region.
     *
     * @param region The {@code Region} that is being discovered.
     * @param tile The {@code Tile} where the region is discovered.
     * @param unit The {@code Unit} discovering the region.
     * @param name The new region name.
     * @return True if the server interaction succeeded.
     */
    public boolean newRegionName(Region region, Tile tile, Unit unit, 
                                 String name) {
        return ask(new NewRegionNameMessage(region, tile, unit, name));
    }

    /**
     * Server query-response for creating a new trade route.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean newTradeRoute() {
        return ask(new NewTradeRouteMessage(null));
    }

    /**
     * Server query-response for tax paying arrears.
     *
     * @param type The {@code GoodsType} to pay the arrears for.
     * @return True if the server interaction succeeded.
     */
    public boolean payArrears(GoodsType type) {
        return ask(new PayArrearsMessage(type));
    }

    /**
     * Server query-response for paying for a building.
     *
     * @param colony The {@code Colony} that is building.
     * @return True if the server interaction succeeded.
     */
    public boolean payForBuilding(Colony colony) {
        return ask(new PayForBuildingMessage(colony));
    }

    /**
     * Server query-response for putting a unit outside a colony.
     *
     * @param unit The {@code Unit} to put out.
     * @return True if the server interaction succeeded.
     */
    public boolean putOutsideColony(Unit unit) {
        return ask(new PutOutsideColonyMessage(unit));
    }

    /**
     * Rearrange a colony.
     *
     * @param colony The {@code Colony} to rearrange.
     * @param workers A list of worker {@code Unit}s that may change.
     * @param scratch A copy of the underlying {@code Colony} with the
     *     workers arranged as required.
     * @return True if the server interaction succeeds.
     */
    public boolean rearrangeColony(Colony colony, List<Unit> workers,
                                   Colony scratch) {
        RearrangeColonyMessage message
            = new RearrangeColonyMessage(colony, workers, scratch);
        return (message.isEmpty()) ? true : ask(message);
    }

    /**
     * Server query-response for renaming an object.
     *
     * @param object A {@code FreeColGameObject} to rename.
     * @param name The name to apply.
     * @return True if the server interaction succeeded.
     */
    public boolean rename(FreeColGameObject object, String name) {
        return ask(new RenameMessage(object, name));
    }

    /**
     * Server query-response to launch the game (pre-game).
     *
     * @return True if the server interaction succeeded.
     */
    public boolean requestLaunch() {
        return send(TrivialMessage.requestLaunchMessage);
    }

    /**
     * Server query-response to retire the player from the game.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean retire() {
        return ask(TrivialMessage.retireMessage);
    }

    /**
     * Server query-response for the dialog on scouting a native
     * settlement, *before* choosing to speak to the chief, attack, et
     * al.
     *
     * @param unit The {@code Unit} that is speaking.
     * @param direction The direction to a settlement to ask.
     * @return True if the server interaction succeeded.
     */
    public boolean scoutSettlement(Unit unit, Direction direction) {
        return ask(new ScoutIndianSettlementMessage(unit, direction));
    }
   
    /**
     * Server query-response for speaking with a native chief.
     *
     * @param unit The {@code Unit} that is speaking.
     * @param is The {@code IndianSettlement} to ask.
     * @return True if the server interaction succeeded.
     */
    public boolean scoutSpeakToChief(Unit unit, IndianSettlement is) {
        return ask(new ScoutSpeakToChiefMessage(unit, is, null));
    }

    /**
     * Server query-response to set a nation's availablility (pre-game).
     *
     * @param nation The {@code Nation} to whose availability is to be set.
     * @param state The {@code NationState} defining the availability.
     * @return True if the server interaction succeeded.
     */
    public boolean setAvailable(Nation nation, NationState state) {
        return ask(new SetAvailableMessage(nation, state));
    }

    /**
     * Server query-response for changing a build queue.
     *
     * @param colony the Colony
     * @param buildQueue the new values for the build queue
     * @return True if the server interaction succeeded.
     */
    public boolean setBuildQueue(Colony colony,
                                 List<BuildableType> buildQueue) {
        return ask(new SetBuildQueueMessage(colony, buildQueue));
    }

    /**
     * Server query-response to set a nation colour
     * (pre-game).
     *
     * @param nation The {@code Nation} to set the color for.
     * @param color The {@code Color} selected.
     * @return True if the server interaction succeeded.
     */
    public boolean setColor(Nation nation, Color color) {
        return ask(new SetColorMessage(nation, color));
    }

    /**
     * Server query-response to set the current stop.
     *
     * @param unit The {@code Unit} whose stop is to be updated.
     * @param index The stop index.
     * @return True if the query-response succeeds.
     */
    public boolean setCurrentStop(Unit unit, int index) {
        return ask(new SetCurrentStopMessage(unit, index));
    }

    /**
     * Server query-response to set the destination of the given unit.
     *
     * @param unit The {@code Unit} to direct.
     * @param destination The destination {@code Location}.
     * @return True if the server interaction succeeded.
     * @see Unit#setDestination(Location)
     */
    public boolean setDestination(Unit unit, Location destination) {
        return ask(new SetDestinationMessage(unit, destination));
    }

    /**
     * Server query-response for setting goods levels.
     *
     * @param colony The {@code Colony} where the levels are set.
     * @param data The {@code ExportData} setting.
     * @return True if the server interaction succeeded.
     */
    public boolean setGoodsLevels(Colony colony, ExportData data) {
        return ask(new SetGoodsLevelsMessage(colony, data));
    }

    /**
     * Server query-response to show which nation a player has selected
     * (pre-game).
     *
     * @param nation The {@code Nation} selected.
     * @return True if the server interaction succeeded.
     */
    public boolean setNation(Nation nation) {
        return ask(new SetNationMessage(null, nation));
    }

    /**
     * Server query-response to show which nation type a player has selected
     * (pre-game).
     *
     * @param nationType The {@code NationType} selected.
     * @return True if the server interaction succeeded.
     */
    public boolean setNationType(NationType nationType) {
        return ask(new SetNationTypeMessage(null, nationType));
    }

    /**
     * Server query-response for indicating that this player is ready
     * (pre-game).
     *
     * @param ready The readiness state to signal.
     * @return True if the server interaction succeeded.
     */
    public boolean setReady(boolean ready) {
        return send(new ReadyMessage(null, ready));
    }

    /**
     * Server query-response for spying on a colony.
     *
     * @param unit The {@code Unit} that is spying.
     * @param settlement The {@code Settlement} to spy on.
     * @return True if the client/server interaction succeeded.
     */
    public boolean spy(Unit unit, Settlement settlement) {
        return ask(new SpySettlementMessage(unit, settlement));
    }

    /**
     * Server query-response for starting to skip turns.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean startSkipping() {
        return send(TrivialMessage.endTurnMessage);
    }

    /**
     * Server query-response for training a unit in Europe.
     *
     * @param type The {@code UnitType} to train.
     * @return True if the server interaction succeeded.
     */
    public boolean trainUnitInEurope(UnitType type) {
        return ask(new TrainUnitInEuropeMessage(type));
    }

    /**
     * Server query-response for unloading goods.
     *
     * @param type The {@code GoodsType} to unload.
     * @param amount The amount of goods to unload.
     * @param carrier The {@code Unit} to unload from.
     * @return True if the query-response succeeds.
     */
    public boolean unloadGoods(GoodsType type, int amount, Unit carrier) {
        return ask(new UnloadGoodsMessage(type, amount, carrier));
    }

    /**
     * Server query-response to update the game options
     * (pre-game).
     *
     * @param gameOptions The {@code OptionGroup} containing the
     *     game options.
     * @return True if the server interaction succeeded.
     */
    public boolean updateGameOptions(OptionGroup gameOptions) {
        return send(new UpdateGameOptionsMessage(gameOptions));
    }

    /**
     * Server query-response to update the map generator options
     * (pre-game).
     *
     * @param mapOptions The {@code OptionGroup} containing the
     *     map generator options.
     * @return True if the server interaction succeeded.
     */
    public boolean updateMapGeneratorOptions(OptionGroup mapOptions) {
        return send(new UpdateMapGeneratorOptionsMessage(mapOptions));
    }

    /**
     * Server query-response for asking for updating the trade route.
     *
     * @param route The trade route to update.
     * @return True if the server interaction succeeded.
     */
    public boolean updateTradeRoute(TradeRoute route) {
        return ask(new UpdateTradeRouteMessage(route));
    }

    /**
     * Server query-response for changing a work location.
     *
     * @param unit The {@code Unit} to change the workLocation of.
     * @param workLocation The {@code WorkLocation} to change to.
     * @return True if the server interaction succeeded.
     */
    public boolean work(Unit unit, WorkLocation workLocation) {
        return ask(new WorkMessage(unit, workLocation));
    }
}
