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


package net.sf.freecol.common.networking;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Constants;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.IndianSettlement;
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
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.MultipleMessage;
import net.sf.freecol.common.option.OptionGroup;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * The API for client/server messaging.
 */
public abstract class ServerAPI {

    private static final Logger logger = Logger.getLogger(ServerAPI.class.getName());


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
     * Get the connection to communicate with the server.
     *
     * @return The <code>Connection</code> to the server.
     */
    protected abstract Connection getConnection();


    // Internal message passing routines

    /**
     * Sends a DOMMessage to the server.
     *
     * @param message The <code>DOMMessage</code> to send.
     * @return True if the send succeeded.
     */
    private boolean send(DOMMessage message) {
        try {
            getConnection().send(message);
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
            getConnection().sendAndWait(message);
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
            reply = getConnection().ask(message);
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
     * @param e The initial request <code>Element</code>.
     */
    private void resolve(Element e) {
        final Connection c = getConnection();
        while (e != null) {
            try {
                e = handle(c.ask(e));
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Could not resolve: " + e.getTagName(),
                    ioe);
                break;
            }
        }
    }

    /**
     * Handle an element.
     *
     * @param e The <code>Element</code> to handle.
     * @return The resulting element.
     */
    private Element handle(Element e) {
        if (e == null) return null;
        final Connection c = getConnection();
        if (c == null) return null;
        try {
            return c.handle(e);
        } catch (FreeColException fce) {
            logger.log(Level.WARNING, "Could not handle: " + e.getTagName(),
                fce);
        }
        return null;
    }

    /**
     * Sends the specified message to the server and returns the reply,
     * if it has the specified tag.
     *
     * Handle error replies if they have a messageId or when in debug mode.
     * This routine allows code simplification in much of the following
     * client-server communication.
     *
     * In following routines we follow the convention that server I/O
     * is confined to the ask<em>foo</em>() routine, which typically
     * returns true if the server interaction succeeded, which does
     * *not* necessarily imply that the actual substance of the
     * request was allowed (e.g. a move may result in the death of a
     * unit rather than actually moving).
     *
     * @param game The current <code>Game</code>.
     * @param message A <code>DOMMessage</code> to send.
     * @param tag The expected tag.
     * @return The answer from the server if it has the specified tag,
     *     otherwise <code>null</code>.
     */
    private Element askExpecting(Game game, DOMMessage message, String tag) {
        Element reply = ask(message);
        if (reply == null) return null;

        if (MultipleMessage.TAG.equals(reply.getTagName())) {
            // Process multiple returns, pick out expected element if
            // present and continue processing the reply.
            MultipleMessage mm = new MultipleMessage(game, reply);
            Element e = mm.extract(tag);
            resolve(handle(e));
            if (mm == null) return null;
            reply = mm.toXMLElement();
        }

        if (ErrorMessage.TAG.equals(reply.getTagName())) {
            // Shortcut error processing
            handle(reply);
            return null;
        }

        if (tag == null || tag.equals(reply.getTagName())) {
            // Success.  Do the standard processing.
            doClientProcessingFor(reply);
            return reply;
        }

        // Unexpected reply.  Whine and fail.
        logger.warning("Received reply with tag " + reply.getTagName()
            + " which should have been " + tag
            + " to message " + message);
        return null;
    }

    /**
     * Extends askExpecting to also handle returns from the server.
     *
     * @param game The current <code>Game</code>.
     * @param message A <code>DOMMessage</code> to send.
     * @param tag The expected tag
     * @return True if the server interaction succeeded and an element
     *     with the expected tag was found in the reply, else false.
     */
    private boolean askHandling(Game game, DOMMessage message, String tag) {
        Element reply = askExpecting(game, message, tag);
        if (reply == null) return false;
        resolve(handle(reply));
        return true;
    }

    // Public messaging routines for game actions

    /**
     * Server query-response to abandon a colony.
     *
     * @param colony The <code>Colony</code> to abandon.
     * @return True if the server interaction succeeded.
     */
    public boolean abandonColony(Colony colony) {
        return askHandling(colony.getGame(),
            new AbandonColonyMessage(colony), null);
    }

    /**
     * Server query-response to respond to a monarch offer.
     *
     * @param game The <code>Game</code> to construct a reply message in.
     * @param action The monarch action responded to.
     * @param accept Accept or reject the offer.
     * @return True if the server interaction succeeded.
     */
    public boolean answerMonarch(Game game, MonarchAction action,
                                 boolean accept) {
        return askHandling(game, new MonarchActionMessage(action, null, "")
            .setResult(accept), null);
    }

    /**
     * Server query-response for finding out the skill taught at a settlement.
     *
     * @param unit The <code>Unit</code> that is asking.
     * @param direction The direction to a settlement to ask.
     * @return True if the server interaction succeeded.
     */
    public boolean askSkill(Unit unit, Direction direction) {
        return askHandling(unit.getGame(),
            new AskSkillMessage(unit, direction), null);
    }

    /**
     * Server query-response for assigning a teacher.
     *
     * @param student The student <code>Unit</code>.
     * @param teacher The teacher <code>Unit</code>.
     * @return True if the server interaction succeeded.
     */
    public boolean assignTeacher(Unit student, Unit teacher) {
        return askHandling(student.getGame(),
            new AssignTeacherMessage(student, teacher), null);
    }

    /**
     * Server query-response for assigning a trade route to a unit.
     *
     * @param unit The <code>Unit</code> to assign a trade route to.
     * @param tradeRoute The <code>TradeRoute</code> to assign.
     * @return True if the server interaction succeeded.
     */
    public boolean assignTradeRoute(Unit unit, TradeRoute tradeRoute) {
        return askHandling(unit.getGame(),
            new AssignTradeRouteMessage(unit, tradeRoute), null);
    }

    /**
     * Server query-response for attacking.
     *
     * @param unit The <code>Unit</code> to perform the attack.
     * @param direction The direction in which to attack.
     * @return True if the server interaction succeeded.
     */
    public boolean attack(Unit unit, Direction direction) {
        return askHandling(unit.getGame(),
            new AttackMessage(unit, direction), null);
    }

    /**
     * Server query-response for building a colony.
     *
     * @param name The name for the colony.
     * @param unit The <code>Unit</code> that will build.
     * @return True if the server interaction succeeded.
     */
    public boolean buildColony(String name, Unit unit) {
        return askHandling(unit.getGame(),
            new BuildColonyMessage(name, unit), null);
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
        return askHandling(unit.getGame(),
            new BuyMessage(unit, settlement, goods, gold), null);
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
        Element reply = askExpecting(unit.getGame(),
            new BuyPropositionMessage(unit, settlement, goods, gold),
            BuyPropositionMessage.getTagName());
        return (reply == null
            || !BuyPropositionMessage.getTagName().equals(reply.getTagName()))
            ? Constants.NO_TRADE
            : new BuyPropositionMessage(unit.getGame(), reply).getGold();
    }

    /**
     * Server query-response to cash in a treasure train.
     *
     * @param unit The treasure train <code>Unit</code> to cash in.
     * @return True if the server interaction succeeded.
     */
    public boolean cashInTreasureTrain(Unit unit) {
        return askHandling(unit.getGame(),
            new CashInTreasureTrainMessage(unit), null);
    }

    /**
     * Server query-response for changing unit state.
     *
     * @param unit The <code>Unit</code> to change the state of.
     * @param state The new <code>UnitState</code>.
     * @return boolean <b>true</b> if the server interaction succeeded.
     */
    public boolean changeState(Unit unit, UnitState state) {
        return askHandling(unit.getGame(),
            new ChangeStateMessage(unit, state), null);
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
        return askHandling(unit.getGame(),
            new ChangeWorkImprovementTypeMessage(unit, type), null);
    }

    /**
     * Server query-response for changing work type.
     *
     * @param unit The <code>Unit</code> to change the work type of.
     * @param workType The new <code>GoodsType</code> to produce.
     * @return True if the server interaction succeeded.
     */
    public boolean changeWorkType(Unit unit, GoodsType workType) {
        return askHandling(unit.getGame(),
            new ChangeWorkTypeMessage(unit, workType), null);
    }

    /**
     * Send a chat message (pre and in-game).
     *
     * @param player The <code>Player</code> to chat to.
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
        return askHandling(tile.getGame(),
            new ClaimLandMessage(tile, claimant, price), null);
    }

    /**
     * Server query-response for clearing a unit speciality.
     *
     * @param unit The <code>Unit</code> to operate on.
     * @return True if the server interaction succeeded.
     */
    public boolean clearSpeciality(Unit unit) {
        return askHandling(unit.getGame(),
            new ClearSpecialityMessage(unit), null);
    }

    /**
     * Server query-response to close a transaction session for a trade.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return True if the server interaction succeeded.
     */
    public boolean closeTransactionSession(Unit unit, Settlement settlement) {
        return askHandling(unit.getGame(),
            new CloseTransactionMessage(unit, settlement), null);
    }

    /**
     * Server query-response to continue with a won game.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean continuePlaying() {
        return send(new DOMMessage("continuePlaying"));
    }

    /**
     * Server query-response for declaring independence.
     *
     * @param game The <code>Game</code> to construct a reply message in.
     * @param nation The name for the new nation.
     * @param country The name for the new country.
     * @return True if the server interaction succeeded.
     */
    public boolean declareIndependence(Game game, String nation, String country) {
        return askHandling(game,
            new DeclareIndependenceMessage(nation, country), null);
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
        return askHandling(unit.getGame(),
            new DeclineMoundsMessage(unit, direction), null);
    }

    /**
     * Server query-response for deleting a trade route.
     *
     * @param tradeRoute The <code>TradeRoute</code> to delete.
     * @return True if the server interaction succeeded.
     */
    public boolean deleteTradeRoute(TradeRoute tradeRoute) {
        return askHandling(tradeRoute.getGame(),
            new DeleteTradeRouteMessage(tradeRoute), null);
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
        return askHandling(unit.getGame(),
            new DeliverGiftMessage(unit, settlement, goods), null);
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
        return askHandling(unit.getGame(),
            new DemandTributeMessage(unit, direction), null);
    }

    /**
     * Handler server query-response for diplomatic messages.
     *
     * @param ourUnit Our <code>Unit</code> conducting the diplomacy.
     * @param otherColony The other <code>Colony</code> to negotiate with.
     * @param agreement The <code>DiplomaticTrade</code> agreement to propose.
     * @return The resulting agreement or null if none present.
     */
    public DiplomaticTrade diplomacy(Unit ourUnit, Colony otherColony, 
                                     DiplomaticTrade agreement) {
        Element reply = askExpecting(ourUnit.getGame(),
            new DiplomacyMessage(ourUnit, otherColony, agreement),
            null);
        // Often returns diplomacy, but also just an update on accept or
        // null on reject.
        if (reply == null) {
            return null;
        } else if (DiplomacyMessage.getTagName().equals(reply.getTagName())) {
            return new DiplomacyMessage(ourUnit.getGame(), reply).getAgreement();
        } else {
            handle(reply);
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
    public DiplomaticTrade diplomacy(Unit ourUnit, Unit otherUnit, 
                                     DiplomaticTrade agreement) {
        Element reply = askExpecting(ourUnit.getGame(),
            new DiplomacyMessage(ourUnit, otherUnit, agreement),
            DiplomacyMessage.getTagName());
        if (reply == null) {
            return null;
        } else if (DiplomacyMessage.getTagName().equals(reply.getTagName())) {
            return new DiplomacyMessage(ourUnit.getGame(), reply).getAgreement();
        } else {
            handle(reply);
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
    public DiplomaticTrade diplomacy(Colony ourColony, Unit otherUnit, 
                                     DiplomaticTrade agreement) {
        Element reply = askExpecting(ourColony.getGame(),
            new DiplomacyMessage(ourColony, otherUnit, agreement),
            DiplomacyMessage.getTagName());
        if (reply == null) {
            return null;
        } else if (DiplomacyMessage.getTagName().equals(reply.getTagName())) {
            return new DiplomacyMessage(ourColony.getGame(), reply).getAgreement();
        } else {
            handle(reply);
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
        return askHandling(unit.getGame(),
            new DisbandUnitMessage(unit), null);
    }

    /**
     * Server query-response for disembarking from a carrier.
     *
     * @param unit The <code>Unit</code> that is disembarking.
     * @return True if the server interaction succeeded.
     */
    public boolean disembark(Unit unit) {
        return askHandling(unit.getGame(),
            new DisembarkMessage(unit), null);
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
        return askHandling(unit.getGame(),
            new EmbarkMessage(unit, carrier, direction), null);
    }

    /**
     * Server query-response for emigration.
     *
     * @param game The <code>Game</code> to construct a reply message in.
     * @param slot The slot from which the unit migrates, 1-3 selects
     *             a specific one, otherwise the server will choose one.
     * @return True if the client-server interaction succeeded.
     */
    public boolean emigrate(Game game, int slot) {
        return askHandling(game, new EmigrateUnitMessage(slot), null);
    }

    /**
     * Server query-response for asking for the turn to end.
     *
     * @param game The <code>Game</code> to construct a reply message in.
     * @return True if the server interaction succeeded.
     */
    public boolean endTurn(Game game) {
        return askHandling(game,
            new DOMMessage("endTurn"), null);
    }

    /**
     * Server query-response for asking to enter revenge mode (post-game).
     *
     * @param game The <code>Game</code> to construct a reply message in.
     * @return True if the server interaction succeeded.
     */
    public boolean enterRevengeMode(Game game) {
        return askHandling(game,
            new DOMMessage("enterRevengeMode"), null);
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
        return askHandling(unit.getGame(),
            new EquipForRoleMessage(unit, role, roleCount), null);
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
        return askHandling(player.getGame(),
            new FirstContactMessage(player, other, tile).setResult(result),
            null);
    }

    /**
     * Server query-response to get a list of goods for sale from a settlement.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return True if the server interaction succeeded.
     */
    public boolean getGoodsForSaleInSettlement(Unit unit,
                                               Settlement settlement) {
        return askHandling(unit.getGame(),
            new GoodsForSaleMessage(unit, settlement, null),
            null);
    }

    /**
     * Server query-response for asking for the high scores list.
     *
     * @param game The <code>Game</code> to extract scores from.
     * @param key The high score key to query.
     * @return True if the server interaction succeeded.
     */
    public boolean getHighScores(Game game, String key) {
        return askHandling(game,
            new HighScoreMessage(key), null);
    }

    /**
     * Server query-response for asking for the nation summary of a player.
     *
     * @param self The <code>Player</code> requesting the summary.
     * @param player The <code>Player</code> to summarize.
     * @return True if the server interaction succeeded.
     */
    public boolean nationSummary(Player self, Player player) {
        return askHandling(self.getGame(),
            new NationSummaryMessage(player),
            null);
    }

    /**
     * Server query-response for inciting the natives.
     *
     * @param unit The missionary <code>Unit</code>.
     * @param is The <code>IndianSettlement</code> to incite.
     * @param enemy An enemy <code>Player</code>.
     * @param gold The amount of bribe, negative to enquire.
     * @return True if the server interaction succeeded.
     */
    public boolean incite(Unit unit, IndianSettlement is, Player enemy,
                          int gold) {
        return askHandling(unit.getGame(),
            new InciteMessage(unit, is, enemy, gold),
            null);
    }

    /**
     * Makes demands to a colony.  One and only one of goods or gold is valid.
     *
     * @param unit The <code>Unit</code> that is demanding.
     * @param colony The <code>Colony</code> to demand of.
     * @param type The <code>GoodsType</code> to demand.
     * @param amount The amount of goods to demand.
     * @return True if the server interaction succeeded.
     */
    public boolean indianDemand(Unit unit, Colony colony,
                                GoodsType type, int amount) {
        return askHandling(unit.getGame(),
            new IndianDemandMessage(unit, colony, type, amount), null);
    }
            
    /**
     * Server query-response for joining a colony.
     *
     * @param unit The <code>Unit</code> that will join.
     * @param colony The <code>Colony</code> to join.
     * @return True if the server interaction succeeded.
     */
    public boolean joinColony(Unit unit, Colony colony) {
        return askHandling(unit.getGame(),
            new JoinColonyMessage(colony, unit), null);
    }

    /**
     * Server query-response for learning the skill taught at a settlement.
     *
     * @param unit The <code>Unit</code> that is asking.
     * @param direction The direction to a settlement to ask.
     * @return True if the server interaction succeeded.
     */
    public boolean learnSkill(Unit unit, Direction direction) {
        return askHandling(unit.getGame(),
            new LearnSkillMessage(unit, direction), null);
    }

    /**
     * Server query-response for loading goods.
     *
     * @param loc The <code>Location</code> where the goods are.
     * @param type The <code>GoodsType</code> to load.
     * @param amount The amount of goods to load.
     * @param carrier The <code>Unit</code> to load onto.
     * @return True if the server interaction succeeded.
     */
    public boolean loadGoods(Location loc, GoodsType type, int amount,
                             Unit carrier) {
        return askHandling(carrier.getGame(),
            new LoadGoodsMessage(loc, type, amount, carrier), null);
    }

    /**
     * Server query-response for logging in a player (pre-game).
     *
     * @param userName The user name.
     * @param version The client version.
     * @return True if the server interaction succeeded.
     */
    public boolean login(String userName, String version) {
        return askHandling(null,
            new LoginMessage(userName, version), null);
    }

    /**
     * Server query-response for logging out.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean logout() {
        return sendAndWait(new DOMMessage("logout",
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
        return askHandling(winner.getGame(),
            new LootCargoMessage(winner, defenderId, goods), null);
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
        return askHandling(unit.getGame(),
            new MissionaryMessage(unit, direction, denounce), null);
    }

    /**
     * Server query-response for moving a unit.
     *
     * @param unit The <code>Unit</code> to move.
     * @param direction The direction to move in.
     * @return True if the server interaction succeeded.
     */
    public boolean move(Unit unit, Direction direction) {
        return askHandling(unit.getGame(),
            new MoveMessage(unit, direction), null);
    }

    /**
     * Server query-response for moving to across the high seas.
     *
     * @param unit The <code>Unit</code> to move.
     * @param destination The <code>Location</code> to move to.
     * @return True if the server interaction succeeded.
     */
    public boolean moveTo(Unit unit, Location destination) {
        return askHandling(unit.getGame(),
            new MoveToMessage(unit, destination), null);
    }

    /**
     * Server query-response for naming a new land.
     *
     * @param unit The <code>Unit</code> that has come ashore.
     * @param name The new land name.
     * @return True if the server interaction succeeded.
     */
    public boolean newLandName(Unit unit, String name) {
        return askHandling(unit.getGame(),
            new NewLandNameMessage(unit, name), null);
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
        return askHandling(unit.getGame(),
            new NewRegionNameMessage(region, tile, unit, name), null);
    }

    /**
     * Server query-response for creating a new trade route.
     *
     * @param game The <code>Game</code> to construct a reply message in.
     * @return True if the server interaction succeeded.
     */
    public boolean newTradeRoute(Game game) {
        return askHandling(game,
            new NewTradeRouteMessage(), null);
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
        Element reply = askExpecting(unit.getGame(),
            new GetTransactionMessage(unit, settlement),
            null);
        return new boolean[] {
            DOMMessage.getBooleanAttribute(reply, "canBuy", false),
            DOMMessage.getBooleanAttribute(reply, "canSell", false),
            DOMMessage.getBooleanAttribute(reply, "canGift", false)
        };
    }

    /**
     * Server query-response for tax paying arrears.
     *
     * @param game The current <code>Game</code>.
     * @param type The <code>GoodsType</code> to pay the arrears for.
     * @return True if the server interaction succeeded.
     */
    public boolean payArrears(Game game, GoodsType type) {
        return askHandling(game,
            new PayArrearsMessage(type), null);
    }

    /**
     * Server query-response for paying for a building.
     *
     * @param colony The <code>Colony</code> that is building.
     * @return True if the server interaction succeeded.
     */
    public boolean payForBuilding(Colony colony) {
        return askHandling(colony.getGame(),
            new PayForBuildingMessage(colony), null);
    }

    /**
     * Server query-response for putting a unit outside a colony.
     *
     * @param unit The <code>Unit</code> to put out.
     * @return True if the server interaction succeeded.
     */
    public boolean putOutsideColony(Unit unit) {
        return askHandling(unit.getGame(),
            new PutOutsideColonyMessage(unit), null);
    }

    /**
     * Rearrange a colony.
     *
     * @param colony The <code>Colony</code> to rearrange.
     * @param workers A list of worker <code>Unit</code>s that may change.
     * @param scratch A copy of the underlying <code>Colony</code> with the
     *     workers arranged as required.
     * @return True if the server interaction succeeds.
     */
    public boolean rearrangeColony(Colony colony, List<Unit> workers,
                                   Colony scratch) {
        RearrangeColonyMessage message
            = new RearrangeColonyMessage(colony, workers, scratch);
        return (message.isEmpty()) ? true
            : askHandling(colony.getGame(), message, null);
    }

    /**
     * Server query-response for renaming an object.
     *
     * @param object A <code>FreeColGameObject</code> to rename.
     * @param name The name to apply.
     * @return True if the server interaction succeeded.
     */
    public boolean rename(FreeColGameObject object, String name) {
        return askHandling(object.getGame(),
            new RenameMessage(object, name), null);
    }

    /**
     * Server query-response to launch the game (pre-game).
     *
     * @return True if the server interaction succeeded.
     */
    public boolean requestLaunch() {
        return send(new DOMMessage("requestLaunch"));
    }

    /**
     * Server query-response to retire the player from the game.
     *
     * @param game The <code>Game</code> to construct a reply message in.
     * @return True if the server interaction succeeded.
     */
    public boolean retire(Game game) {
        return askHandling(game,
            new DOMMessage("retire"), null);
    }

    /**
     * Server query-response for the dialog on scouting a native
     * settlement, *before* choosing to speak to the chief, attack, et
     * al.
     *
     * @param unit The <code>Unit</code> that is speaking.
     * @param direction The direction to a settlement to ask.
     * @return True if the server interaction succeeded.
     */
    public boolean scoutSettlement(Unit unit, Direction direction) {
        return askHandling(unit.getGame(),
            new ScoutIndianSettlementMessage(unit, direction),
            null);
    }
   
    /**
     * Server query-response for speaking with a native chief.
     *
     * @param unit The <code>Unit</code> that is speaking.
     * @param settlement The <code>IndianSettlement</code> to ask.
     * @return True if the server interaction succeeded.
     */
    public boolean scoutSpeakToChief(Unit unit, IndianSettlement settlement) {
        return askHandling(unit.getGame(),
            new ScoutSpeakToChiefMessage(unit, settlement, null),
            null);
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
        Element reply = askExpecting(unit.getGame(),
            new SellPropositionMessage(unit, settlement, goods, gold),
            SellPropositionMessage.getTagName());
        return (reply == null
            || !SellPropositionMessage.getTagName().equals(reply.getTagName()))
            ? Constants.NO_TRADE
            : new SellPropositionMessage(unit.getGame(), reply).getGold();
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
        return askHandling(unit.getGame(),
            new SellMessage(unit, settlement, goods, gold), null);
    }

    /**
     * Server query-response to set a nation's availablility (pre-game).
     *
     * @param nation The <code>Nation</code> to whose availability is to be set.
     * @param state The <code>NationState</code> defining the availability.
     * @return True if the server interaction succeeded.
     */
    public boolean setAvailable(Nation nation, NationState state) {
        return askHandling(null, new DOMMessage("setAvailable",
                "nation", nation.getId(),
                "state", state.toString()), null);
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
        return askHandling(colony.getGame(),
            new SetBuildQueueMessage(colony, buildQueue), null);
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
        return askHandling(null, new DOMMessage("setColor",
                "nation", nation.getId(),
                "color", Integer.toString(color.getRGB())), null);
    }

    /**
     * Server query-response to set the current stop.
     *
     * @param unit The <code>Unit</code> whose stop is to be updated.
     * @param index The stop index.
     * @return True if the query-response succeeds.
     */
    public boolean setCurrentStop(Unit unit, int index) {
        return askHandling(unit.getGame(),
            new SetCurrentStopMessage(unit, index), null);
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
        return askHandling(unit.getGame(),
            new SetDestinationMessage(unit, destination), null);
    }

    /**
     * Server query-response for setting goods levels.
     *
     * @param colony The <code>Colony</code> where the levels are set.
     * @param data The <code>ExportData</code> setting.
     * @return True if the server interaction succeeded.
     */
    public boolean setGoodsLevels(Colony colony, ExportData data) {
        return askHandling(colony.getGame(),
            new SetGoodsLevelsMessage(colony, data), null);
    }

    /**
     * Server query-response to show which nation a player has selected
     * (pre-game).
     *
     * @param nation The <code>Nation</code> selected.
     * @return True if the server interaction succeeded.
     */
    public boolean setNation(Nation nation) {
        return askHandling(null, new DOMMessage("setNation",
                "value", nation.getId()), null);
    }

    /**
     * Server query-response to show which nation type a player has selected
     * (pre-game).
     *
     * @param nationType The <code>NationType</code> selected.
     * @return True if the server interaction succeeded.
     */
    public boolean setNationType(NationType nationType) {
        return askHandling(null, new DOMMessage("setNationType",
                "value", nationType.getId()), null);
    }

    /**
     * Server query-response for indicating that this player is ready
     * (pre-game).
     *
     * @param ready The readiness state to signal.
     * @return True if the server interaction succeeded.
     */
    public boolean setReady(boolean ready) {
        return send(new DOMMessage("ready", "value", Boolean.toString(ready)));
    }

    /**
     * Server query-response for spying on a colony.
     *
     * @param unit The <code>Unit</code> that is spying.
     * @param settlement The <code>Settlement</code> to spy on.
     * @return True if the client/server interaction succeeded.
     */
    public boolean spy(Unit unit, Settlement settlement) {
        return askHandling(unit.getGame(),
            new SpySettlementMessage(unit, settlement), null);
    }

    /**
     * Server query-response for starting to skip turns.
     *
     * @return True if the server interaction succeeded.
     */
    public boolean startSkipping() {
        return send(new DOMMessage("endTurn"));
    }

    /**
     * Server query-response for training a unit in Europe.
     *
     * @param game The <code>Game</code> to construct a reply message in.
     * @param type The <code>UnitType</code> to train.
     * @return True if the server interaction succeeded.
     */
    public boolean trainUnitInEurope(Game game, UnitType type) {
        return askHandling(game,
            new TrainUnitInEuropeMessage(type), null);
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
        return askHandling(carrier.getGame(),
            new UnloadGoodsMessage(type, amount, carrier), null);
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
        return send(new UpdateGameOptionsMessage(gameOptions));
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
        return send(new UpdateMapGeneratorOptionsMessage(mapOptions));
    }

    /**
     * Server query-response for asking for updating the trade route.
     *
     * @param route The trade route to update.
     * @return True if the server interaction succeeded.
     */
    public boolean updateTradeRoute(TradeRoute route) {
        return askHandling(route.getGame(),
            new UpdateTradeRouteMessage(route), null);
    }

    /**
     * Server query-response for changing a work location.
     *
     * @param unit The <code>Unit</code> to change the workLocation of.
     * @param workLocation The <code>WorkLocation</code> to change to.
     * @return True if the server interaction succeeded.
     */
    public boolean work(Unit unit, WorkLocation workLocation) {
        return askHandling(unit.getGame(),
            new WorkMessage(unit, workLocation), null);
    }
}
