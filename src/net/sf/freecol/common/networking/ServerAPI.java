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


package net.sf.freecol.common.networking;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.NationType;
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
import net.sf.freecol.common.option.OptionGroup;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * The API for client->server messaging.
 */
public abstract class ServerAPI {

    private static final Logger logger = Logger.getLogger(ServerAPI.class.getName());

    /** The Client used to communicate with the server. */
    private Client client;


    /**
     * Creates a new <code>ServerAPI</code>.
     */
    public ServerAPI() {}


    /**
     * Do local client processing for a reply.
     *
     * @param reply The reply <code>Element</code>.
     */
    protected abstract void doClientProcessingFor(Element reply);

    /**
     * Handle error conditions detected by the client.
     *
     * @param complaint The error message.
     */
    protected abstract void doRaiseErrorMessage(String complaint);


    // Internal message passing routines

    /**
     * Sends an Element to the server.
     *
     * FIXME: remove all uses of this.
     *
     * @param element The <code>Element</code> to send.
     * @return True if the send succeeded.
     */
    private boolean send(Element element) {
        try {
            client.send(element);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not send: " + element, e);
        }
        return false;
    }

    /**
     * Sends a DOMMessage to the server.
     *
     * @param message The <code>DOMMessage</code> to send.
     * @return True if the send succeeded.
     */
    private boolean send(DOMMessage message) {
        try {
            client.send(message);
            return true;
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Could not send: " + message.getType(),
                       ioe);
        }
        return false;
    }

    /**
     * Sends a DOMMessage to the server and waits for a reply.
     *
     * @param message The <code>DOMMessage</code> to send.
     * @return True if the send succeeded.
     */
    private boolean sendAndWait(DOMMessage message) {
        try {
            client.sendAndWait(message);
            return true;
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Could not send: " + message.getType(),
                       ioe);
        }
        return false;
    }

    /**
     * Sends a DOMMessage query the server and waits for a reply.
     *
     * @param message The <code>DOMMessage</code> to send.
     * @return The reply, or null if there was a problem.
     */
    private Element ask(DOMMessage message) {
        Element reply = null;
        try {
            reply = client.ask(message);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Could not ask: " + message.getType(),
                       ioe);
        }
        return reply;
    }

    /**
     * Loop sending requests and handling replies from the server until
     * they reduce to null.
     *
     * @param request The initial request <code>Element</code>.
     */
    private void resolve(Element request) {
        while (request != null) {
            try {
                request = client.handleReply(client.ask(request));
            } catch (IOException ioe) {
                logger.warning("Could not resolve: " + request.getTagName());
                break;
            }
        }
    }

    /**
     * Sends the specified message to the server and returns the reply,
     * if it has the specified tag.
     * Handle "error" replies if they have a messageID or when in debug mode.
     * This routine allows code simplification in much of the following
     * client-server communication.
     *
     * In following routines we follow the convention that server I/O
     * is confined to the ask<foo>() routine, which typically returns
     * true if the server interaction succeeded, which does *not*
     * necessarily imply that the actual substance of the request was
     * allowed (e.g. a move may result in the death of a unit rather
     * than actually moving).
     *
     * @param message A <code>DOMMessage</code> to send.
     * @param tag The expected tag
     * @param results A <code>Map</code> to store special attribute results in.
     * @return The answer from the server if it has the specified tag,
     *         otherwise <code>null</code>.
     */
    private Element askExpecting(DOMMessage message, String tag,
                                 HashMap<String, String> results) {
        Element reply = ask(message);
        if (reply == null) return null;

        if ("error".equals(reply.getTagName())) {
            String messageId = reply.getAttribute("messageID");
            String messageText = reply.getAttribute("message");
            if (messageId != null && messageText != null
                && FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.COMMS)) {
                // If debugging suppress the bland but i18n compliant
                // failure message in favour of the higher detail
                // non-i18n text.
                reply.removeAttribute("messageID");
            }
            logger.warning("ServerAPI. " + message.getType() + " error,"
                + " messageId: " + messageId
                + " message: " + messageText);
            client.handleReply(reply);
            return null;
        }

        // Success!
        if (tag == null || tag.equals(reply.getTagName())) {
            // Do the standard processing.
            doClientProcessingFor(reply);
            // Look for special attributes
            if (results != null) {
                if (results.containsKey("*")) {
                    results.remove("*");
                    int len = reply.getAttributes().getLength();
                    for (int i = 0; i < len; i++) {
                        Node n = reply.getAttributes().item(i);
                        results.put(n.getNodeName(), n.getNodeValue());
                    }
                } else {
                    for (String k : results.keySet()) {
                        if (reply.hasAttribute(k)) {
                            results.put(k, reply.getAttribute(k));
                        }
                    }
                }
            }
            return reply;
        }

        // Process multiple returns, pick out expected element if present
        if ("multiple".equals(reply.getTagName())) {
            List<Element> replies = new ArrayList<>();
            NodeList nodes = reply.getChildNodes();
            Element result = null;

            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i) instanceof Element
                    && ((Element)nodes.item(i)).getTagName().equals(tag)) {
                    result = (Element)nodes.item(i);
                } else {
                    Element e = client.handleReply((Element)nodes.item(i));
                    if (e != null) replies.add(e);
                }
            }
            resolve(DOMMessage.collapseElements(replies));
            if (result != null) return result;
        }

        // Unexpected reply.  Whine and fail.
        String complaint = "Received reply with tag " + reply.getTagName()
            + " which should have been " + tag
            + " to message " + message;
        logger.warning(complaint);
        doRaiseErrorMessage(complaint);
        return null;
    }

    /**
     * Extends askExpecting to also handle returns from the server.
     *
     * @param message A <code>DOMMessage</code> to send.
     * @param tag The expected tag
     * @param results A <code>Map</code> to store special attribute results in.
     * @return True if the server interaction succeeded and an element
     *     with the expected tag was found in the reply, else false.
     */
    private boolean askHandling(DOMMessage message, String tag,
                                HashMap<String, String> results) {
        Element reply = askExpecting(message, tag, results);
        if (reply == null) return false;
        resolve(client.handleReply(reply));
        return true;
    }

    /**
     * Helper to load a map.
     *
     * @param queries Query strings.
     * @return A map with null mappings for the query strings.
     */
    private HashMap<String, String> loadMap(String... queries) {
        HashMap<String, String> result = new HashMap<>();
        for (String q : queries) result.put(q, null);
        return result;
    }


    // Public routines for manipulation of the connection to the server.

    /**
     * Get the host we are connected to.
     *
     * @return The current host, or null if none.
     */
    public String getHost() {
        return (client == null) ? null : client.getHost();
    }

    /**
     * Get the port we are connected to.
     *
     * @return The current port, or negative if none.
     */     
    public int getPort() {
        return (client == null) ? -1 : client.getPort();
    }

    /**
     * Get the raw connection.
     *
     * Do not use this.  It exists only for debugging purposes.
     *
     * @return The server <code>Connection</code>.
     */
    public Connection getConnection() {
        return (client == null) ? null : client.getConnection();
    }

    /**
     * Register a message handler to handle messages from the server.
     * Used when switching from pre-game to in-game.
     *
     * @param messageHandler The new <code>MessageHandler</code>.
     */
    public void registerMessageHandler(MessageHandler messageHandler) {
        if (client != null) {
            client.setMessageHandler(messageHandler);
        }
    }

    /**
     * Disconnect the client.
     */
    public void disconnect() {
        if (client != null) {
            client.disconnect();
            reset();
        }
    }

    /**
     * Just forget about the client.
     * Only call this if sure it is dead.
     */
    public void reset() {
        client = null;
    }

    /**
     * Connects a client to host:port (or more).
     *
     * @param threadName The name for the thread.
     * @param host The name of the machine running the
     *     <code>FreeColServer</code>.
     * @param port The port to use when connecting to the host.
     * @return True if the connection succeeded.
     * @exception IOException on connection failure.
     */
    public boolean connect(String threadName, String host, int port,
                           MessageHandler messageHandler) 
        throws IOException {
        int tries;
        if (port < 0) {
            port = FreeCol.getServerPort();
            tries = 10;
        } else {
            tries = 1;
        }
        for (int i = tries; i > 0; i--) {
            try {
                client = new Client(host, port, messageHandler, threadName);
                if (client != null) break;
            } catch (IOException e) {
                if (i <= 1) throw e;
            }
        }
        return client != null;
    }


    // Public messaging routines for game actions

    /**
     * Server query-response to abandon a colony.
     *
     * @param colony The <code>Colony</code> to abandon.
     * @return True if the server interaction succeeded.
     */
    public boolean abandonColony(Colony colony) {
        return askHandling(new AbandonColonyMessage(colony),
            null, null);
    }

    /**
     * Server query-response to respond to a monarch offer.
     *
     * @param action The monarch action responded to.
     * @param accept Accept or reject the offer.
     * @return True if the server interaction succeeded.
     */
    public boolean answerMonarch(MonarchAction action, boolean accept) {
        return askHandling(new MonarchActionMessage(action, null, "")
            .setResult(accept),
            null, null);
    }

    /**
     * Server query-response for finding out the skill taught at a settlement.
     *
     * @param unit The <code>Unit</code> that is asking.
     * @param direction The direction to a settlement to ask.
     * @return True if the server interaction succeeded.
     */
    public boolean askSkill(Unit unit, Direction direction) {
        return askHandling(new AskSkillMessage(unit, direction),
            null, null);
    }

    /**
     * Server query-response for assigning a teacher.
     *
     * @param student The student <code>Unit</code>.
     * @param teacher The teacher <code>Unit</code>.
     * @return True if the server interaction succeeded.
     */
    public boolean assignTeacher(Unit student, Unit teacher) {
        return askHandling(new AssignTeacherMessage(student, teacher),
            null, null);
    }

    /**
     * Server query-response for assigning a trade route to a unit.
     *
     * @param unit The <code>Unit</code> to assign a trade route to.
     * @param tradeRoute The <code>TradeRoute</code> to assign.
     * @return True if the server interaction succeeded.
     */
    public boolean assignTradeRoute(Unit unit, TradeRoute tradeRoute) {
        return askHandling(new AssignTradeRouteMessage(unit, tradeRoute),
            null, null);
    }

    /**
     * Server query-response for attacking.
     *
     * @param unit The <code>Unit</code> to perform the attack.
     * @param direction The direction in which to attack.
     * @return True if the server interaction succeeded.
     */
    public boolean attack(Unit unit, Direction direction) {
        return askHandling(new AttackMessage(unit, direction),
            null, null);
    }

    /**
     * Server query-response for building a colony.
     *
     * @param name The name for the colony.
     * @param unit The <code>Unit</code> that will build.
     * @return True if the server interaction succeeded.
     */
    public boolean buildColony(String name, Unit unit) {
        return askHandling(new BuildColonyMessage(name, unit),
            null, null);
    }

    /**
     * Server query-response to buy the given goods from the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to buy.
     * @param gold The agreed price.
     * @return True if the server interaction succeeded.
     */
    public boolean buyFromSettlement(Unit unit, Settlement settlement,
                                     Goods goods, int gold) {
        return askHandling(new BuyMessage(unit, settlement, goods, gold),
            null, null);
    }

    /**
     * Server query-response to ask the natives if a purchase is acceptable.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to trade.
     * @param gold The proposed price (including query on negative).
     * @return The price of the goods.
     */
    public int buyProposition(Unit unit, Settlement settlement,
                              Goods goods, int gold) {
        HashMap<String, String> results = loadMap("gold");
        if (askHandling(new BuyPropositionMessage(unit, settlement,
                    goods, gold), null, results)) {
            try {
                return Integer.parseInt(results.get("gold"));
            } catch (NumberFormatException e) {}
        }
        return NetworkConstants.NO_TRADE;
    }

    /**
     * Server query-response to cash in a treasure train.
     *
     * @param unit The treasure train <code>Unit</code> to cash in.
     * @return True if the server interaction succeeded.
     */
    public boolean cashInTreasureTrain(Unit unit) {
        return askHandling(new CashInTreasureTrainMessage(unit),
            null, null);
    }

    /**
     * Server query-response for changing unit state.
     *
     * @param unit The <code>Unit</code> to change the state of.
     * @param state The new <code>UnitState</code>.
     * @return boolean <b>true</b> if the server interaction succeeded.
     */
    public boolean changeState(Unit unit, UnitState state) {
        return askHandling(new ChangeStateMessage(unit, state),
            null, null);
    }

    /**
     * Server query-response for changing work improvement type.
     *
     * @param unit The <code>Unit</code> to change the work type of.
     * @param type The new <code>TileImprovementType</code> to work on.
     * @return True if the server interaction succeeded.
     */
    public boolean changeWorkImprovementType(Unit unit,
                                             TileImprovementType type) {
        return askHandling(new ChangeWorkImprovementTypeMessage(unit, type),
            null, null);
    }

    /**
     * Server query-response for changing work type.
     *
     * @param unit The <code>Unit</code> to change the work type of.
     * @param workType The new <code>GoodsType</code> to produce.
     * @return True if the server interaction succeeded.
     */
    public boolean changeWorkType(Unit unit, GoodsType workType) {
        return askHandling(new ChangeWorkTypeMessage(unit, workType),
            null, null);
    }

    /**
     * Send a chat message (pre and in-game).
     *
     * @param chat The text of the message.
     * @return True if the send succeeded.
     */
    public boolean chat(Player player, String chat) {
        return send(new ChatMessage(player, chat, false));
    }

    /**
     * Send a chooseFoundingFather message.
     *
     * @param ffs A list of <code>FoundingFather</code>s to choose from.
     * @param ff The chosen <code>FoundingFather</code> (may be null).
     * @return True if the send succeeded.
     */
    public boolean chooseFoundingFather(List<FoundingFather> ffs,
                                        FoundingFather ff) {
        return send(new ChooseFoundingFatherMessage(ffs, ff));
    }

    /**
     * Server query-response to claim a tile.
     *
     * @param tile The <code>Tile</code> to claim.
     * @param claimant The <code>Unit</code> or <code>Settlement</code> that is
     *     claiming the tile.
     * @param price The amount to pay.
     * @return True if the server interaction succeeded.
     */
    public boolean claimTile(Tile tile, FreeColGameObject claimant, int price) {
        return askHandling(new ClaimLandMessage(tile, claimant, price),
            null, null);
    }

    /**
     * Server query-response for clearing a unit speciality.
     *
     * @param unit The <code>Unit</code> to operate on.
     * @return True if the server interaction succeeded.
     */
    public boolean clearSpeciality(Unit unit) {
        return askHandling(new ClearSpecialityMessage(unit),
            null, null);
    }

    /**
     * Server query-response to close a transaction session for a trade.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return True if the server interaction succeeded.
     */
    public boolean closeTransactionSession(Unit unit, Settlement settlement) {
        return askHandling(new CloseTransactionMessage(unit, settlement),
            null, null);
    }

    /**
     * Server query-response to continue with a won game.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean continuePlaying() {
        return send(new TrivialMessage("continuePlaying"));        
    }

    /**
     * Server query-response for declaring independence.
     *
     * @param nation The name for the new nation.
     * @param country The name for the new country.
     * @return True if the server interaction succeeded.
     */
    public boolean declareIndependence(String nation, String country) {
        return askHandling(new DeclareIndependenceMessage(nation, country),
            null, null);
    }

    /**
     * Server query-response for the special case of deciding to
     * explore a rumour but then declining not to investigate the
     * strange mounds.
     *
     * @param unit The <code>Unit</code> that is exploring.
     * @param direction The <code>Direction</code> to move.
     * @return True if the server interaction succeeded.
     */
    public boolean declineMounds(Unit unit, Direction direction) {
        return askHandling(new DeclineMoundsMessage(unit, direction),
            null, null);
    }

    /**
     * Server query-response to give the given goods to the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to give.
     * @return True if the server interaction succeeded.
     */
    public boolean deliverGiftToSettlement(Unit unit, Settlement settlement,
                                           Goods goods) {
        return askHandling(new DeliverGiftMessage(unit, settlement, goods),
            null, null);
    }

    /**
     * Server query-response for demanding a tribute from a native
     * settlement.
     *
     * @param unit The <code>Unit</code> that demands.
     * @param direction The direction to demand in.
     * @return True if the server interaction succeeded.
     */
    public boolean demandTribute(Unit unit, Direction direction) {
        return askHandling(new DemandTributeMessage(unit, direction),
            null, null);
    }

    /**
     * Handler server query-response for diplomatic messages.
     *
     * @param ourUnit Our <code>Unit</code> conducting the diplomacy.
     * @param otherColony The other <code>Colony</code> to negotiate with.
     * @param agreement The <code>DiplomaticTrade</code> agreement to propose.
     * @return The resulting agreement or null if none present.
     */
    public DiplomaticTrade diplomacy(Game game, Unit ourUnit,
                                     Colony otherColony, 
                                     DiplomaticTrade agreement) {
        Element reply = askExpecting(new DiplomacyMessage(ourUnit, otherColony,
                                                          agreement),
            null, null);
        // Often returns diplomacy, but also just an update on accept or
        // null on reject.
        if (reply == null) {
            return null;
        } else if (DiplomacyMessage.getXMLElementTagName().equals(reply.getTagName())) {
            return new DiplomacyMessage(game, reply).getAgreement();
        } else {
            client.handleReply(reply);
            return null;
        }
    }

    /**
     * Handler server query-response for diplomatic messages.
     *
     * @param ourUnit Out <code>Unit</code> conducting the diplomacy.
     * @param otherUnit The other <code>Unit</code> to negotiate with.
     * @param agreement The <code>DiplomaticTrade</code> agreement to propose.
     * @return The resulting agreement or null if none present.
     */
    public DiplomaticTrade diplomacy(Game game, Unit ourUnit,
                                     Unit otherUnit, 
                                     DiplomaticTrade agreement) {
        Element reply = askExpecting(new DiplomacyMessage(ourUnit, otherUnit,
                                                          agreement),
            DiplomacyMessage.getXMLElementTagName(), null);
        if (reply == null) {
            return null;
        } else if (DiplomacyMessage.getXMLElementTagName().equals(reply.getTagName())) {
            return new DiplomacyMessage(game, reply).getAgreement();
        } else {
            client.handleReply(reply);
            return null;
        }
    }

    /**
     * Handler server query-response for diplomatic messages.
     *
     * @param ourColony Out <code>Colony</code> conducting the diplomacy.
     * @param otherUnit The other <code>Unit</code> to negotiate with.
     * @param agreement The <code>DiplomaticTrade</code> agreement to propose.
     * @return The resulting agreement or null if none present.
     */
    public DiplomaticTrade diplomacy(Game game, Colony ourColony,
                                     Unit otherUnit, 
                                     DiplomaticTrade agreement) {
        Element reply = askExpecting(new DiplomacyMessage(ourColony, otherUnit,
                                                          agreement),
            DiplomacyMessage.getXMLElementTagName(), null);
        if (reply == null) {
            return null;
        } else if (DiplomacyMessage.getXMLElementTagName().equals(reply.getTagName())) {
            return new DiplomacyMessage(game, reply).getAgreement();
        } else {
            client.handleReply(reply);
            return null;
        }
    }

    /**
     * Server query-response for disbanding a unit.
     *
     * @param unit The <code>Unit</code> to operate on.
     * @return True if the server interaction succeeded.
     */
    public boolean disbandUnit(Unit unit) {
        return askHandling(new DisbandUnitMessage(unit),
            null, null);
    }

    /**
     * Server query-response for disembarking from a carrier.
     *
     * @param unit The <code>Unit</code> that is disembarking.
     * @return True if the server interaction succeeded.
     */
    public boolean disembark(Unit unit) {
        return askHandling(new DisembarkMessage(unit),
            null, null);
    }

    /**
     * Server query-response for boarding a carrier.
     *
     * @param unit The <code>Unit</code> that is boarding.
     * @param carrier The carrier <code>Unit</code>.
     * @param direction An optional direction if the unit is boarding from
     *        an adjacent tile, or null if from the same tile.
     * @return True if the server interaction succeeded.
     */
    public boolean embark(Unit unit, Unit carrier, Direction direction) {
        return askHandling(new EmbarkMessage(unit, carrier, direction),
            null, null);
    }

    /**
     * Server query-response for emigration.
     *
     * @param slot The slot from which the unit migrates, 1-3 selects
     *             a specific one, otherwise the server will choose one.
     * @return True if the client-server interaction succeeded.
     */
    public boolean emigrate(int slot) {
        return askHandling(new EmigrateUnitMessage(slot),
            null, null);
    }

    /**
     * Server query-response for asking for the turn to end.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean endTurn() {
        return askHandling(new TrivialMessage("endTurn"),
            null, null);
    }

    /**
     * Server query-response for asking to enter revenge mode (post-game).
     *
     * @return True if the server interaction succeeded.
     */
    public boolean enterRevengeMode() {
        return askHandling(new TrivialMessage("enterRevengeMode"),
            null, null);
    }

    /**
     * Server query-response for equipping a unit for a role.
     *
     * @param unit The <code>Unit</code> to equip.
     * @param role The <code>Role</code> to assume.
     * @param roleCount The role count.
     * @return True if the server interaction succeeded.
     */
    public boolean equipUnitForRole(Unit unit, Role role, int roleCount) {
        return askHandling(new EquipForRoleMessage(unit, role, roleCount),
            null, null);
    }

    /**
     * Server query-response for responding to a first contact message.
     *
     * @param player The <code>Player</code> making contact.
     * @param other The native <code>Player</code> being contacted.
     * @param tile An optional <code>Tile</code> to offer the player if
     *     they have made a first landing.
     * @param result Whether the initial peace treaty was accepted.
     * @return True if the server interaction succeeded.
     */
    public boolean firstContact(Player player, Player other, Tile tile,
                                boolean result) {
        return askHandling(new FirstContactMessage(player, other, tile)
                .setResult(result),
            null, null);
    }

    /**
     * Server query-response to get a list of goods for sale from a settlement.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return The goods for sale in the settlement,
     *     or null if the server interaction failed.
     */
    public List<Goods> getGoodsForSaleInSettlement(Game game, Unit unit,
                                                   Settlement settlement) {
        GoodsForSaleMessage message
            = new GoodsForSaleMessage(unit, settlement, null);
        Element reply = askExpecting(message,
            GoodsForSaleMessage.getXMLElementTagName(), null);
        return (reply == null) ? Collections.<Goods>emptyList()
            : new GoodsForSaleMessage(game, reply).getGoods();
    }

    /**
     * Server query-response for asking for the high scores list.
     *
     * @return The list of high scores.
     */
    public List<HighScore> getHighScores() {
        Element reply = askExpecting(new TrivialMessage("getHighScores"),
            null, null);
        if (reply == null) return Collections.<HighScore>emptyList();

        List<HighScore> result = new ArrayList<>();
        NodeList childElements = reply.getChildNodes();
        for (int i = 0; i < childElements.getLength(); i++) {
            result.add(new HighScore((Element)childElements.item(i)));
        }
        return result;
    }

    /**
     * Server query-response for asking for the nation summary of a player.
     *
     * @param player The <code>Player</code> to summarize.
     * @return A summary of that nation, or null on error.
     */
    public NationSummary getNationSummary(Player player) {
        GetNationSummaryMessage message = new GetNationSummaryMessage(player);
        Element reply = askExpecting(message,
            GetNationSummaryMessage.getXMLElementTagName(), null);
        if (reply == null) return null;

        return new GetNationSummaryMessage(reply).getNationSummary();
    }

    /**
     * Server query-response for creating a new trade route.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean getNewTradeRoute() {
        return askHandling(new TrivialMessage("getNewTradeRoute"),
            null, null);
    }

    /**
     * Server query-response for asking about a players REF.
     *
     * @return A list of REF units for the player.
     */
    public List<AbstractUnit> getREFUnits() {
        Element reply = askExpecting(new TrivialMessage("getREFUnits"),
            null, null);
        if (reply == null) return Collections.<AbstractUnit>emptyList();

        List<AbstractUnit> result = new ArrayList<>();
        NodeList childElements = reply.getChildNodes();
        for (int index = 0; index < childElements.getLength(); index++) {
            AbstractUnit unit = new AbstractUnit();
            unit.readFromXMLElement((Element) childElements.item(index));
            result.add(unit);
        }
        return result;
    }

    /**
     * Server query-response for asking for the server statistics.
     *
     * @return The server statistics.
     */
    public java.util.Map<String, String> getStatistics() {
        HashMap<String, String> results = loadMap("*");
        return (askExpecting(new TrivialMessage("getStatistics"),
                "statistics", results) == null) ? null
            : results;
    }

    /**
     * Server query-response for inciting the natives.
     *
     * @param unit The missionary <code>Unit</code>.
     * @param direction The direction to a settlement to speak to.
     * @param enemy An enemy <code>Player</code>.
     * @param gold The amount of bribe, negative to enquire.
     * @return The amount of gold to bribe with if this was an inquiry,
     *     zero for failure to incite and >0 for successful incitement,
     *     negative if the server interaction failed.
     */
    public int incite(Unit unit, Direction direction, Player enemy, int gold) {
        HashMap<String, String> results = loadMap("gold");
        if (!askHandling(new InciteMessage(unit, direction, enemy, gold),
                null, results)) return -1;
        try {
            return Integer.parseInt(results.get("gold"));
        } catch (NumberFormatException e) {}
        return -1;
    }

    /**
     * Server query-response for joining a colony.
     *
     * @param unit The <code>Unit</code> that will join.
     * @param colony The <code>Colony</code> to join.
     * @return True if the server interaction succeeded.
     */
    public boolean joinColony(Unit unit, Colony colony) {
        return askHandling(new JoinColonyMessage(colony, unit),
            null, null);
    }

    /**
     * Server query-response for learning the skill taught at a settlement.
     *
     * @param unit The <code>Unit</code> that is asking.
     * @param direction The direction to a settlement to ask.
     * @return True if the server interaction succeeded.
     */
    public boolean learnSkill(Unit unit, Direction direction) {
        return askHandling(new LearnSkillMessage(unit, direction),
            null, null);
    }

    /**
     * Server query-response for loading goods.
     *
     * @param loc The <code>Location</code> where the goods are.
     * @param type The <code>GoodsType</code> to load.
     * @param amount The amount of goods to load.
     * @param carrier The <code>Unit</code> to load onto.
     * @return True if the query-response succeeds.
     */
    public boolean loadGoods(Location loc, GoodsType type, int amount,
                             Unit carrier) {
        return askHandling(new LoadGoodsMessage(loc, type, amount, carrier),
            null, null);
    }

    /**
     * Server query-response for logging in a player (pre-game).
     *
     * @param userName The user name.
     * @param version The client version.
     * @return A <code>LoginMessage</code> on success, or null on error.
     */
    public LoginMessage login(String userName, String version) {
        Element reply = askExpecting(new TrivialMessage("login",
                                                        "userName", userName,
                                                        "version", version),
                                     "login", null);
        return (reply == null) ? null : new LoginMessage(null, reply);
    }

    /**
     * Server query-response for logging out.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean logout() {
        return sendAndWait(new TrivialMessage("logout",
                "reason", "User has quit the client."));
    }

    /**
     * Server query-response for looting.  Handles both an initial query and
     * the actual looting.
     *
     * @param winner The <code>Unit</code> that is looting.
     * @param defenderId The identifier of the defender unit (it may have sunk).
     * @param goods A list of <code>Goods</code>, if empty this is a query
     *     as to what is to be looted which is filled into the list,
     *     if non-empty, then the list of goods to loot.
     * @return True if the server interaction succeeded.
     */
    public boolean loot(Unit winner, String defenderId, List<Goods> goods) {
        return askHandling(new LootCargoMessage(winner, defenderId, goods),
            null, null);
    }

    /**
     * Server query-response for establishing/denouncing a mission.
     *
     * @param unit The missionary <code>Unit</code>.
     * @param direction The direction to a settlement to establish with.
     * @param denounce True if this is a denouncement.
     * @return True if the server interaction succeeded.
     */
    public boolean missionary(Unit unit, Direction direction,
                              boolean denounce) {
        return askHandling(new MissionaryMessage(unit, direction, denounce),
            null, null);
    }

    /**
     * Server query-response for moving a unit.
     *
     * @param unit The <code>Unit</code> to move.
     * @param direction The direction to move in.
     * @return True if the server interaction succeeded.
     */
    public boolean move(Unit unit, Direction direction) {
        return askHandling(new MoveMessage(unit, direction),
            null, null);
    }

    /**
     * Server query-response for moving to across the high seas.
     *
     * @param unit The <code>Unit</code> to move.
     * @param destination The <code>Location</code> to move to.
     * @return True if the server interaction succeeded.
     */
    public boolean moveTo(Unit unit, Location destination) {
        return askHandling(new MoveToMessage(unit, destination),
            null, null);
    }

    /**
     * Server query-response for naming a new land.
     *
     * @param unit The <code>Unit</code> that has come ashore.
     * @param name The new land name.
     * @return True if the server interaction succeeded.
     */
    public boolean newLandName(Unit unit, String name) {
        return askHandling(new NewLandNameMessage(unit, name),
            null, null);
    }

    /**
     * Server query-response for naming a new region.
     *
     * @param region The <code>Region</code> that is being discovered.
     * @param tile The <code>Tile</code> where the region is discovered.
     * @param unit The <code>Unit</code> discovering the region.
     * @param name The new region name.
     * @return True if the server interaction succeeded.
     */
    public boolean newRegionName(Region region, Tile tile, Unit unit, 
                                 String name) {
        return askHandling(new NewRegionNameMessage(region, tile, unit, name),
            null, null);
    }

    /**
     * Server query-response to get the transaction session for a trade.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return An array of booleans for the buy/sell/gift status,
     *     or null if the server interaction failed.
     */
    public boolean[] openTransactionSession(Unit unit, Settlement settlement) {
        HashMap<String, String> results
            = loadMap("canBuy", "canSell", "canGift");
        if (askExpecting(new GetTransactionMessage(unit, settlement),
                null, results) == null
            || results.get("canBuy") == null
            || results.get("canSell") == null
            || results.get("canGift") == null) return null;
        return new boolean[] {
            Boolean.parseBoolean(results.get("canBuy")),
            Boolean.parseBoolean(results.get("canSell")),
            Boolean.parseBoolean(results.get("canGift"))
        };
    }

    /**
     * Server query-response for tax paying arrears.
     *
     * @param type The <code>GoodsType</code> to pay the arrears for.
     * @return True if the server interaction succeeded.
     */
    public boolean payArrears(GoodsType type) {
        return askHandling(new PayArrearsMessage(type),
            null, null);
    }

    /**
     * Server query-response for paying for a building.
     *
     * @param colony The <code>Colony</code> that is building.
     * @return True if the server interaction succeeded.
     */
    public boolean payForBuilding(Colony colony) {
        return askHandling(new PayForBuildingMessage(colony),
            null, null);
    }

    /**
     * Server query-response for putting a unit outside a colony.
     *
     * @param unit The <code>Unit</code> to put out.
     * @return True if the server interaction succeeded.
     */
    public boolean putOutsideColony(Unit unit) {
        return askHandling(new PutOutsideColonyMessage(unit),
            null, null);
    }

    /**
     * Server query-response for renaming an object.
     *
     * @param object A <code>FreeColGameObject</code> to rename.
     * @param name The name to apply.
     * @return True if the server interaction succeeded.
     */
    public boolean rename(FreeColGameObject object, String name) {
        return askHandling(new RenameMessage(object, name),
            null, null);
    }

    /**
     * Server query-response to launch the game (pre-game).
     *
     * @return True if the server interaction succeeded.
     */
    public boolean requestLaunch() {
        return send(new TrivialMessage("requestLaunch"));    
    }

    /**
     * Server query-response to retire the player from the game.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean retire() {
        return askHandling(new TrivialMessage("retire"), null, null);
    }

    /**
     * Server query-response for the dialog on scouting a native
     * settlement, *before* choosing to speak to the chief, attack, et
     * al.
     *
     * @param unit The <code>Unit</code> that is speaking.
     * @param direction The direction to a settlement to ask.
     * @return The number of settlements of this tribe (which is
     *     needed in the dialog following), or negative on error.
     */
    public String scoutSettlement(Unit unit, Direction direction) {
        HashMap<String, String> results = loadMap("settlements");
        return (askHandling(new ScoutIndianSettlementMessage(unit, direction),
                            null, results)) ? results.get("settlements")
            : null;
    }
   
    /**
     * Server query-response for speaking with a native chief.
     *
     * @param unit The <code>Unit</code> that is speaking.
     * @param direction The direction to a settlement to ask.
     * @return A string describing the result,
     *     or null if the server interaction failed.
     */
    public String scoutSpeakToChief(Unit unit, Direction direction) {
        HashMap<String, String> results = loadMap("result");
        return (askHandling(new ScoutSpeakToChiefMessage(unit, direction),
                null, results)) ? results.get("result")
            : null;
    }

    /**
     * Server query-response to ask the natives if a sale is acceptable.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to trade.
     * @param gold The proposed price (including query on negative).
     * @return The selling price for the goods.
     */
    public int sellProposition(Unit unit, Settlement settlement,
                               Goods goods, int gold) {
        HashMap<String, String> results = loadMap("gold");
        if (askHandling(new SellPropositionMessage(unit, settlement,
                    goods, gold), null, results)) {
            try {
                return Integer.parseInt(results.get("gold"));
            } catch (NumberFormatException e) {}
        }
        return NetworkConstants.NO_TRADE;
    }

    /**
     * Server query-response to sell the given goods to the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to sell.
     * @param gold The agreed price.
     * @return True if the server interaction succeeded.
     */
    public boolean sellToSettlement(Unit unit, Settlement settlement,
                                    Goods goods, int gold) {
        return askHandling(new SellMessage(unit, settlement, goods, gold),
            null, null);
    }

    /**
     * Server query-response to set a nation's availablility (pre-game).
     *
     * @param nation The <code>Nation</code> to whose availability is to be set.
     * @param state The <code>NationState</code> defining the availability.
     * @return True if the server interaction succeeded.
     */
    public boolean setAvailable(Nation nation, NationState state) {
        return askHandling(new TrivialMessage("setAvailable",
                "nation", nation.getId(),
                "state", state.toString()),
            null, null);
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
        return askHandling(new SetBuildQueueMessage(colony, buildQueue),
            null, null);
    }

    /**
     * Server query-response to set a nation colour
     * (pre-game).
     *
     * @param nation The <code>Nation</code> to set the color for.
     * @param color The <code>Color</code> selected.
     * @return True if the server interaction succeeded.
     */
    public boolean setColor(Nation nation, Color color) {
        return askHandling(new TrivialMessage("setColor",
                "nation", nation.getId(),
                "color", Integer.toString(color.getRGB())),
            null, null);
    }

    /**
     * Server query-response to set the current stop.
     *
     * @param unit The <code>Unit</code> whose stop is to be updated.
     * @param index The stop index.
     * @return True if the query-response succeeds.
     */
    public boolean setCurrentStop(Unit unit, int index) {
        return askHandling(new SetCurrentStopMessage(unit, index),
            null, null);
    }

    /**
     * Server query-response to set the destination of the given unit.
     *
     * @param unit The <code>Unit</code> to direct.
     * @param destination The destination <code>Location</code>.
     * @return True if the server interaction succeeded.
     * @see Unit#setDestination(Location)
     */
    public boolean setDestination(Unit unit, Location destination) {
        return askHandling(new SetDestinationMessage(unit, destination),
            null, null);
    }

    /**
     * Server query-response for setting goods levels.
     *
     * @param colony The <code>Colony</code> where the levels are set.
     * @param data The <code>ExportData</code> setting.
     * @return True if the server interaction succeeded.
     */
    public boolean setGoodsLevels(Colony colony, ExportData data) {
        return askHandling(new SetGoodsLevelsMessage(colony, data),
            null, null);
    }

    /**
     * Server query-response to show which nation a player has selected
     * (pre-game).
     *
     * @param nation The <code>Nation</code> selected.
     * @return True if the server interaction succeeded.
     */
    public boolean setNation(Nation nation) {
        return askHandling(new TrivialMessage("setNation",
                "value", nation.getId()),
            null, null);
    }

    /**
     * Server query-response to show which nation type a player has selected
     * (pre-game).
     *
     * @param nationType The <code>NationType</code> selected.
     * @return True if the server interaction succeeded.
     */
    public boolean setNationType(NationType nationType) {
        return askHandling(new TrivialMessage("setNationType",
                "value", nationType.getId()),
            null, null);
    }

    /**
     * Server query-response for indicating that this player is ready
     * (pre-game).
     *
     * @param ready The readiness state to signal.
     * @return True if the server interaction succeeded.
     */
    public boolean setReady(boolean ready) {
        return send(new TrivialMessage("ready",
                "value", Boolean.toString(ready)));
    }

    /**
     * Server query-response for setting the trade routes.
     *
     * @param routes A list of trade routes to update.
     * @return True if the server interaction succeeded.
     */
    public boolean setTradeRoutes(List<TradeRoute> routes) {
        return askHandling(new SetTradeRoutesMessage(routes),
            null, null);
    }

    /**
     * Server query-response for spying on a colony.
     *
     * @param unit The <code>Unit</code> that is spying.
     * @param direction The <code>Direction</code> of a colony to spy on.
     * @return True if the client/server interaction succeeded.
     */
    public boolean spy(Unit unit, Direction direction) {
        return askHandling(new SpySettlementMessage(unit, direction),
            null, null);
    }

    /**
     * Server query-response for starting to skip turns.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean startSkipping() {
        return send(new TrivialMessage("endTurn"));
    }

    /**
     * Server query-response for training a unit in Europe.
     *
     * @param type The <code>UnitType</code> to train.
     * @return True if the server interaction succeeded.
     */
    public boolean trainUnitInEurope(UnitType type) {
        return askHandling(new TrainUnitInEuropeMessage(type),
            null, null);
    }

    /**
     * Server query-response for unloading goods.
     *
     * @param type The <code>GoodsType</code> to unload.
     * @param amount The amount of goods to unload.
     * @param carrier The <code>Unit</code> to unload from.
     * @return True if the query-response succeeds.
     */
    public boolean unloadGoods(GoodsType type, int amount, Unit carrier) {
        return askHandling(new UnloadGoodsMessage(type, amount, carrier),
            null, null);
    }

    /**
     * Server query-response to update the game options
     * (pre-game).
     *
     * @param gameOptions The <code>OptionGroup</code> containing the
     *     game options.
     * @return True if the server interaction succeeded.
     */
    public boolean updateGameOptions(OptionGroup gameOptions) {
        Element up = DOMMessage.createMessage("updateGameOptions");
        up.appendChild(gameOptions.toXMLElement(up.getOwnerDocument()));
        return send(up);        
    }

    /**
     * Server query-response to update the map generator options
     * (pre-game).
     *
     * @param mapOptions The <code>OptionGroup</code> containing the
     *     map generator options.
     * @return True if the server interaction succeeded.
     */
    public boolean updateMapGeneratorOptions(OptionGroup mapOptions) {
        Element up = DOMMessage.createMessage("updateMapGeneratorOptions");
        up.appendChild(mapOptions.toXMLElement(up.getOwnerDocument()));
        return send(up);    
    }

    /**
     * Server query-response for asking for updating the trade route.
     *
     * @param route The trade route to update.
     * @return True if the server interaction succeeded.
     */
    public boolean updateTradeRoute(TradeRoute route) {
        return askHandling(new UpdateTradeRouteMessage(route),
            null, null);
    }

    /**
     * Server query-response for changing a work location.
     *
     * @param unit The <code>Unit</code> to change the workLocation of.
     * @param workLocation The <code>WorkLocation</code> to change to.
     * @return True if the server interaction succeeded.
     */
    public boolean work(Unit unit, WorkLocation workLocation) {
        return askHandling(new WorkMessage(unit, workLocation),
            null, null);
    }
}
