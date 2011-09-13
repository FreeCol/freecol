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


package net.sf.freecol.common.networking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.networking.AbandonColonyMessage;
import net.sf.freecol.common.networking.AskSkillMessage;
import net.sf.freecol.common.networking.AssignTeacherMessage;
import net.sf.freecol.common.networking.AssignTradeRouteMessage;
import net.sf.freecol.common.networking.AttackMessage;
import net.sf.freecol.common.networking.BuildColonyMessage;
import net.sf.freecol.common.networking.BuyGoodsMessage;
import net.sf.freecol.common.networking.BuyMessage;
import net.sf.freecol.common.networking.BuyPropositionMessage;
import net.sf.freecol.common.networking.CashInTreasureTrainMessage;
import net.sf.freecol.common.networking.ChangeStateMessage;
import net.sf.freecol.common.networking.ChangeWorkImprovementTypeMessage;
import net.sf.freecol.common.networking.ChangeWorkTypeMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.ClaimLandMessage;
import net.sf.freecol.common.networking.ClearSpecialityMessage;
import net.sf.freecol.common.networking.CloseTransactionMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DeclareIndependenceMessage;
import net.sf.freecol.common.networking.DeclineMoundsMessage;
import net.sf.freecol.common.networking.DeliverGiftMessage;
import net.sf.freecol.common.networking.DemandTributeMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.DisbandUnitMessage;
import net.sf.freecol.common.networking.DisembarkMessage;
import net.sf.freecol.common.networking.EmbarkMessage;
import net.sf.freecol.common.networking.EmigrateUnitMessage;
import net.sf.freecol.common.networking.EquipUnitMessage;
import net.sf.freecol.common.networking.GetNationSummaryMessage;
import net.sf.freecol.common.networking.GetTransactionMessage;
import net.sf.freecol.common.networking.GoodsForSaleMessage;
import net.sf.freecol.common.networking.InciteMessage;
import net.sf.freecol.common.networking.JoinColonyMessage;
import net.sf.freecol.common.networking.LearnSkillMessage;
import net.sf.freecol.common.networking.LoadCargoMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.MissionaryMessage;
import net.sf.freecol.common.networking.MoveMessage;
import net.sf.freecol.common.networking.MoveToMessage;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.networking.PayArrearsMessage;
import net.sf.freecol.common.networking.PayForBuildingMessage;
import net.sf.freecol.common.networking.PutOutsideColonyMessage;
import net.sf.freecol.common.networking.RenameMessage;
import net.sf.freecol.common.networking.ScoutIndianSettlementMessage;
import net.sf.freecol.common.networking.SellGoodsMessage;
import net.sf.freecol.common.networking.SellMessage;
import net.sf.freecol.common.networking.SellPropositionMessage;
import net.sf.freecol.common.networking.SetBuildQueueMessage;
import net.sf.freecol.common.networking.SetDestinationMessage;
import net.sf.freecol.common.networking.SetGoodsLevelsMessage;
import net.sf.freecol.common.networking.SetTradeRoutesMessage;
import net.sf.freecol.common.networking.SpySettlementMessage;
import net.sf.freecol.common.networking.TrainUnitInEuropeMessage;
import net.sf.freecol.common.networking.UnloadCargoMessage;
import net.sf.freecol.common.networking.UpdateCurrentStopMessage;
import net.sf.freecol.common.networking.UpdateTradeRouteMessage;
import net.sf.freecol.common.networking.WorkMessage;

import org.xml.sax.SAXException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * The API for client->server messaging.
 */
public class ServerAPI {

    private static final Logger logger = Logger.getLogger(ServerAPI.class.getName());

    private FreeColClient freeColClient; // cached client reference


    /**
     * Creates a new <code>ServerAPI</code>.
     *
     * @param freeColClient The <code>FreeColClient</code> that is
     *     communicating with a server.
     */
    public ServerAPI(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
    }


    /** Temporary trivial message wrapper. */
    private class TrivialMessage extends DOMMessage {

        private String tag;
        private String[] attributes;

        public TrivialMessage(String tag, String... attributes) {
            this.tag = tag;
            this.attributes = attributes;
        }

        public Element toXMLElement() {
            Element e = createNewRootElement(tag);
            for (int i = 0; i < attributes.length; i += 2) {
                e.setAttribute(attributes[i], attributes[i+1]);
            }
            return e;
        }
    };


    /**
     * Helper to load a map.
     *
     * @param queries Query strings.
     * @return A map with null mappings for the query strings.
     */
    private HashMap<String, String> loadMap(String... queries) {
        HashMap<String, String> result = new HashMap<String, String>();
        for (String q : queries) result.put(q, null);
        return result;
    }

    /**
     * Sends a DOMMessage to the server.
     *
     * @param message The <code>DOMMessage</code> to send.
     * @return True if the send succeeded.
     */
    private boolean send(DOMMessage message) {
        freeColClient.getClient().send(message.toXMLElement());
        return true;
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
        // Send the element, return null on failure or null return.
        Client client = freeColClient.getClient();
        Element element = message.toXMLElement();
        Element reply = null;
        try {
            reply = client.ask(element);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not send " + element, e);
            return null;
        }
        if (reply == null) {
            logger.warning("Received null reply to " + element);
            return null;
        }

        // Process explicit errors.
        if ("error".equals(reply.getTagName())) {
            String messageId = reply.getAttribute("messageID");
            String messageText = reply.getAttribute("message");
            if (messageId != null && messageText != null
                && FreeCol.isInDebugMode()) {
                // If debugging suppress the bland but i18n compliant
                // failure message in favour of the higher detail
                // non-i18n text.
                reply.removeAttribute("messageID");
            }
            if (messageId == null && messageText == null) {
                logger.warning("Received null error response");
            } else {
                logger.warning("Received error response: "
                               + ((messageId != null) ? messageId : "")
                               + "/" + ((messageText != null)
                                   ? messageText : ""));
                freeColClient.getInGameInputHandler()
                    .handle(client.getConnection(), reply);
            }
            return null;
        }

        // Success!
        if (tag == null || tag.equals(reply.getTagName())) {
            // Do the standard processing.
            String sound = reply.getAttribute("sound");
            if (sound != null && !sound.isEmpty()) {
                freeColClient.playSound(sound);
            }
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

        // Unexpected reply.  Whine and fail.
        String complaint = "Received reply with tag " + reply.getTagName()
            + " which should have been " + tag
            + " to message " + message;
        logger.warning(complaint);
        if (FreeCol.isInDebugMode()) {
            freeColClient.getCanvas().errorMessage(null, complaint);
        }
        return null;
    }

    /**
     * Extends askExpecting to also handle returns from the server.
     *
     * @param message A <code>DOMMessage</code> to send.
     * @param tag The expected tag
     * @param results A <code>Map</code> to store special attribute results in.
     * @return True if the server interaction succeeded, else false.
     */
    private boolean askHandling(DOMMessage message, String tag,
                                HashMap<String, String> results) {
        Element reply = askExpecting(message, tag, results);
        if (reply == null) return false;

        freeColClient.getInGameInputHandler()
            .handle(freeColClient.getClient().getConnection(), reply);
        return true;
    }


    // Public interface

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
        return askHandling(new TrivialMessage("monarchAction",
                "action", action.toString(),
                "accepted", Boolean.toString(accept)),
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
     * Server query-response for buying goods in Europe.
     *
     * @param carrier The <code>Unit</code> to load with the goods.
     * @param type The type of goods to buy.
     * @param amount The amount of goods to buy.
     * @return True if the server interaction succeeded.
     */
    public boolean buyGoods(Unit carrier, GoodsType type, int amount) {
        return askHandling(new BuyGoodsMessage(carrier, type, amount),
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
     * Server query-response for checking the high score.
     *
     * @return True if the player has achieved a new high score.
     */
    public boolean checkHighScore() {
        HashMap<String, String> results = loadMap("highScore");
        return (askHandling(new TrivialMessage("checkHighScore"),
                            null, results)
            && results.get("highScore") != null)
            ? Boolean.parseBoolean(results.get("highScore"))
            : false;
    }

    /**
     * Server query-response for choosing a founding father.
     *
     * @param father The <code>FoundingFather</code> to choose.
     * @return True if the server interaction succeeded.
     */
    public boolean chooseFoundingFather(FoundingFather father) {
        return send(new TrivialMessage("chooseFoundingFather",
                "foundingFather", father.getId()));
    }

    /**
     * Server query-response to claim a piece of land.
     *
     * @param tile The land to claim.
     * @param colony An optional <code>Colony</code> to own the land.
     * @param price The amount to pay.
     * @return True if the server interaction succeeded.
     */
    public boolean claimLand(Tile tile, Colony colony, int price) {
        return askHandling(new ClaimLandMessage(tile, colony, price),
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
     * Send a chat message.
     *
     * @param chat The text of the message.
     * @return True if the send succeeded.
     */
    public boolean chat(String chat) {
        return send(new ChatMessage(freeColClient.getMyPlayer(), chat, false));
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
     * @param unit The <code>Unit</code> conducting the diplomacy.
     * @param settlement The <code>Settlement</code> to negotiate with.
     * @param agreement The <code>DiplomaticTrade</code> agreement to propose.
     * @return True if the server interaction succeeded.
     */
    public boolean diplomacy(Unit unit, Settlement settlement,
                             DiplomaticTrade agreement) {
        return askHandling(new DiplomacyMessage(unit, settlement, agreement),
            null, null);
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
     * Server query-response for asking to enter revenge mode.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean enterRevengeMode() {
        return askHandling(new TrivialMessage("enterRevengeMode"),
            null, null);
    }

    /**
     * Server query-response for equipping a unit.
     *
     * @param unit The <code>Unit</code> to equip on.
     * @param type The <code>EquipmentType</code> to equip with.
     * @param amount The amount of equipment.
     * @return True if the server interaction succeeded.
     */
    public boolean equipUnit(Unit unit, EquipmentType type, int amount) {
        return askHandling(new EquipUnitMessage(unit, type, amount),
            null, null);
    }

    /**
     * Server query-response for asking for the high scores list.
     *
     * @return The list of high scores.
     */
    public List<HighScore> getHighScores() {
        Element reply = askExpecting(new TrivialMessage("getHighScores"),
            null, null);
        if (reply == null) return Collections.emptyList();

        List<HighScore> result = new ArrayList<HighScore>();
        NodeList childElements = reply.getChildNodes();
        for (int i = 0; i < childElements.getLength(); i++) {
            try {
                HighScore score = new HighScore((Element)childElements.item(i));
                result.add(score);
            } catch (XMLStreamException e) {
                logger.warning("Unable to read score element: "
                               + e.getMessage());
            }
        }
        return result;
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
        if (reply == null) return Collections.emptyList();

        List<AbstractUnit> result = new ArrayList<AbstractUnit>();
        NodeList childElements = reply.getChildNodes();
        for (int index = 0; index < childElements.getLength(); index++) {
            AbstractUnit unit = new AbstractUnit();
            unit.readFromXMLElement((Element) childElements.item(index));
            result.add(unit);
        }
        return result;
    }

    /**
     * Server query-response to get a list of goods for sale from a settlement.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return The goods for sale in the settlement,
     *     or null if the server interaction failed.
     */
    public List<Goods> getGoodsForSaleInSettlement(Unit unit,
                                                   Settlement settlement) {
        GoodsForSaleMessage message
            = new GoodsForSaleMessage(unit, settlement, null);
        Element reply = askExpecting(message,
            GoodsForSaleMessage.getXMLElementTagName(), null);
        if (reply == null) return null;

        Game game = freeColClient.getGame();
        List<Goods> goodsOffered = new ArrayList<Goods>();
        NodeList childNodes = reply.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            goodsOffered.add(new Goods(game, (Element) childNodes.item(i)));
        }
        return goodsOffered;
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
     * Server query-response for loading cargo.
     *
     * @param goods The <code>Goods</code> to load.
     * @param carrier The <code>Unit</code> to load onto.
     * @return True if the query-response succeeds.
     */
    public boolean loadCargo(Goods goods, Unit carrier) {
        return askHandling(new LoadCargoMessage(goods, carrier),
            null, null);
    }

    /**
     * Server query-response for looting.  Handles both an initial query and
     * the actual looting.
     *
     * @param winner The <code>Unit</code> that is looting.
     * @param defenderId The id of the defender unit (it may have sunk).
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

        Game game = freeColClient.getGame();
        return new GetNationSummaryMessage(game, reply).getNationSummary();
    }

    /**
     * Server query-response for naming a new land.
     *
     * @param unit The <code>Unit</code> that has come ashore.
     * @param name The new land name.
     * @param welcomer A welcoming native player with whom to make a treaty.
     * @param accept True if the treaty was accepted.
     * @return True if the server interaction succeeded.
     */
    public boolean newLandName(Unit unit, String name, Player welcomer,
                               boolean accept) {
        return askHandling(new NewLandNameMessage(unit, name, welcomer, -1,
                accept),
            null, null);
    }

    /**
     * Server query-response for naming a new region.
     *
     * @param region The <code>Region</code> that is being discovered.
     * @param unit The <code>Unit</code> that discovered the region.
     * @param name The new region name.
     * @return True if the server interaction succeeded.
     */
    public boolean newRegionName(Region region, Unit unit, String name) {
        return askHandling(new NewRegionNameMessage(region, unit, name),
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
     * Server query-response for renaming an object.
     *
     * @param object A <code>FreeColGameObject</code> to rename.
     * @param name The name to apply.
     * @return True if the renaming succeeded.
     */
    public boolean rename(FreeColGameObject object, String name) {
        return askHandling(new RenameMessage(object, name),
            null, null);
    }

    /**
     * Retires the player from the game.
     *
     * @return True if the player achieved a new high score.
     */
    public boolean retire() {
        return askHandling(new TrivialMessage("retire"),
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
     * Server query-response for speaking with a native chief.
     *
     * @param unit The <code>Unit</code> that is speaking.
     * @param direction The direction to a settlement to ask.
     * @return A string describing the result,
     *     or null if the server interaction failed.
     */
    public String scoutSpeak(Unit unit, Direction direction) {
        HashMap<String, String> results = loadMap("result");
        return (askHandling(new ScoutIndianSettlementMessage(unit, direction),
                null, results)) ? results.get("result")
            : null;
    }

    /**
     * Server query-response for selling goods in Europe.
     *
     * @param goods The <code>Goods</code> to sell.
     * @param carrier The <code>Unit</code> in Europe with the goods.
     * @return True if the server interaction succeeded.
     */
    public boolean sellGoods(Goods goods, Unit carrier) {
        return askHandling(new SellGoodsMessage(goods, carrier),
            null, null);
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
     * Server query-response for unloading cargo.
     *
     * @param goods The <code>Goods</code> to unload.
     * @return True if the query-response succeeds.
     */
    public boolean unloadCargo(Goods goods) {
        return askHandling(new UnloadCargoMessage(goods),
            null, null);
    }

    /**
     * Server query-response for updating the current stop.
     *
     * @param unit The <code>Unit</code> whose stop is to be updated.
     * @return True if the query-response succeeds.
     */
    public boolean updateCurrentStop(Unit unit) {
        return askHandling(new UpdateCurrentStopMessage(unit),
            null, null);
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
