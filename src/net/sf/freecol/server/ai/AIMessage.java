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

package net.sf.freecol.server.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.AttackMessage;
import net.sf.freecol.common.networking.BuildColonyMessage;
import net.sf.freecol.common.networking.CashInTreasureTrainMessage;
import net.sf.freecol.common.networking.ChangeStateMessage;
import net.sf.freecol.common.networking.ChangeWorkImprovementTypeMessage;
import net.sf.freecol.common.networking.ChangeWorkTypeMessage;
import net.sf.freecol.common.networking.ClaimLandMessage;
import net.sf.freecol.common.networking.ClearSpecialityMessage;
import net.sf.freecol.common.networking.CloseTransactionMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.DeliverGiftMessage;
import net.sf.freecol.common.networking.DisbandUnitMessage;
import net.sf.freecol.common.networking.DisembarkMessage;
import net.sf.freecol.common.networking.EmbarkMessage;
import net.sf.freecol.common.networking.EmigrateUnitMessage;
import net.sf.freecol.common.networking.EquipForRoleMessage;
import net.sf.freecol.common.networking.GetNationSummaryMessage;
import net.sf.freecol.common.networking.GetTransactionMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.LoadGoodsMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.MissionaryMessage;
import net.sf.freecol.common.networking.MoveMessage;
import net.sf.freecol.common.networking.MoveToMessage;
import net.sf.freecol.common.networking.PutOutsideColonyMessage;
import net.sf.freecol.common.networking.RearrangeColonyMessage;
import net.sf.freecol.common.networking.ScoutSpeakToChiefMessage;
import net.sf.freecol.common.networking.SetBuildQueueMessage;
import net.sf.freecol.common.networking.TrainUnitInEuropeMessage;
import net.sf.freecol.common.networking.UnloadGoodsMessage;
import net.sf.freecol.common.networking.WorkMessage;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Wrapper class for AI message handling.
 */
public class AIMessage {

    private static final Logger logger = Logger.getLogger(AIMessage.class.getName());


    /**
     * Ask the server a question.
     *
     * @param conn The <code>Connection</code> to use when
     *     communicating with the server.
     * @param request The <code>Element</code> to send.
     * @return The reply <code>Element</code>.
     */
    private static Element ask(Connection conn, Element request) {
        Element reply;
        try {
            reply = conn.ask(request);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not send \""
                + request.getTagName() + "\"-message.", e);
            reply = null;
        }
        return reply;
    }

    /**
     * Handle error elements.
     *
     * @param element The <code>Element</code> to check.
     * @param tag The tag that was originally sent.
     * @return True if there was an error.
     */
    private static boolean checkError(Element element, String tag) {
        if (element != null && "error".equals(element.getTagName())) {
            String messageId = element.getAttribute("messageID");
            String messageText = element.getAttribute("message");
            logger.warning("AIMessage." + tag + " error,"
                + " messageId: " + messageId
                + " message: " + messageText);
            return true;
        }
        return false;
    }

    /**
     * Ask the server a question, handling the reply.
     *
     * @param conn The <code>Connection</code> to use when
     *     communicating with the server.
     * @param request The <code>Element</code> to send.
     * @return True if the message was sent, handled, and no error returned.
     */
    private static boolean askHandling(Connection conn, Element request) {
        while (request != null) {
            Element reply = ask(conn, request);
            if (reply == null) break;
            if (checkError(reply, request.getTagName())) return false;
            try {
                request = conn.handle(reply);
            } catch (FreeColException fce) {
                logger.log(Level.WARNING, "AI handler failed: " + reply, fce);
                return false;
            }
        }
        return true;
    }

    /**
     * Ask the server a question, returning non-error replies.
     *
     * @param conn The <code>Connection</code> to use when
     *     communicating with the server.
     * @param request The <code>Element</code> to send.
     * @return The reply <code>Element</code> if not an error, otherwise null.
     */
    private static Element askExpecting(Connection conn, Element request,
                                        String expect) {
        Element reply = ask(conn, request);
        if (checkError(reply, request.getTagName())
            || reply == null) return null;
       
        final String tag = reply.getTagName();
        if ("multiple".equals(tag)) {
            List<Element> replies = new ArrayList<>();
            NodeList nodes = reply.getChildNodes();
            Element result = null;

            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i) instanceof Element
                    && ((Element)nodes.item(i)).getTagName().equals(expect)) {
                    result = (Element)nodes.item(i);
                    continue;
                }
                try {
                    Element e = conn.handle((Element)nodes.item(i));
                    if (e != null) replies.add(e);
                } catch (FreeColException fce) {
                    logger.log(Level.WARNING, "AI handler failed: " + reply, fce);
                }
            }
            if (!askHandling(conn, DOMMessage.collapseElements(replies))
                || result == null) return null;
            reply = result;
        }

        if (expect.equals(reply.getTagName())) return reply;
        logger.log(Level.WARNING, "AI handler expected " + expect
            + ", recieved " + tag);
        return null;
    }
        
    /**
     * Sends a DOMMessage to the server.
     *
     * @param connection The <code>Connection</code> to use
     *     when communicating with the server.
     * @param request The <code>Element</code> to send.
     * @return True if the message was sent and a non-error reply returned.
     */
    private static boolean sendMessage(Connection connection,
                                       Element request) {
        return askHandling(connection, request);
    }

    /**
     * Send a message to the server.
     *
     * @param connection The <code>Connection</code> to use
     *     when communicating with the server.
     * @param message The <code>Message</code> to send.
     * @return True if the message was sent, and a non-null, non-error
     *     reply returned.
     */
    private static boolean sendMessage(Connection connection,
                                       DOMMessage message) {
        return (connection != null && message != null)
            ? sendMessage(connection, message.toXMLElement())
            : false;
    }

    /**
     * Send a trivial message.
     *
     * @param connection The <code>Connection</code> to send on.
     * @param tag The tag of the message.
     * @param attributes Attributes to add to the message.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean sendTrivial(Connection connection, String tag,
                                      String... attributes) {
        return sendMessage(connection, makeTrivial(tag, attributes));
    }

    /**
     * Make a trivial message.
     *
     * @param tag The tag of the message.
     * @param attributes Attributes to add to the message.
     * @return The Element encapsulating the message.
     */
    public static Element makeTrivial(String tag, String... attributes) {
        if ((attributes.length & 1) == 1) {
            throw new IllegalArgumentException("Attributes list must have even length");
        }
        return DOMMessage.createMessage(tag, attributes);
    }


    /**
     * An AIUnit attacks in the given direction.
     *
     * @param aiUnit The <code>AIUnit</code> to attack with.
     * @param direction The <code>Direction</code> to attack in.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askAttack(AIUnit aiUnit, Direction direction) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new AttackMessage(aiUnit.getUnit(), direction));
    }


    /**
     * An AIUnit builds a colony.
     *
     * @param aiUnit The <code>AIUnit</code> to build the colony.
     * @param name The name of the colony.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askBuildColony(AIUnit aiUnit, String name) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new BuildColonyMessage(name, aiUnit.getUnit()));
    }


    /**
     * An AIUnit cashes in.
     *
     * @param aiUnit The <code>AIUnit</code> cashing in.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askCashInTreasureTrain(AIUnit aiUnit) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new CashInTreasureTrainMessage(aiUnit.getUnit()));
    }


    /**
     * An AIUnit changes state.
     *
     * @param aiUnit The <code>AIUnit</code> to change the state of.
     * @param state The new <code>UnitState</code>.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askChangeState(AIUnit aiUnit, UnitState state) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new ChangeStateMessage(aiUnit.getUnit(), state));
    }


    /**
     * An AIUnit changes its work type.
     *
     * @param aiUnit The <code>AIUnit</code> to change the work type of.
     * @param type The <code>GoodsType</code> to produce.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askChangeWorkType(AIUnit aiUnit, GoodsType type) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new ChangeWorkTypeMessage(aiUnit.getUnit(), type));
    }


   /**
     * An AIUnit changes its work improvement type.
     *
     * @param aiUnit The <code>AIUnit</code> to change the work type of.
     * @param type The <code>TileImprovementType</code> to produce.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askChangeWorkImprovementType(AIUnit aiUnit,
                                                  TileImprovementType type) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
            new ChangeWorkImprovementTypeMessage(aiUnit.getUnit(), type));
    }


    /**
     * Claims a tile for a colony.
     *
     * @param tile The <code>Tile</code> to claim.
     * @param claimant The <code>AIUnit</code> or <code>AIColony</code>
     *     that is claiming.
     * @param price The price to pay.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askClaimLand(Tile tile, AIObject claimant,
                                       int price) {
        FreeColGameObject fcgo;
        Player owner;
        if (claimant instanceof AIUnit) {
            fcgo = ((AIUnit)claimant).getUnit();
            owner = ((Unit)fcgo).getOwner();
        } else if (claimant instanceof AIColony) {
            fcgo = ((AIColony)claimant).getColony();
            owner = ((Colony)fcgo).getOwner();
        } else {
            throw new IllegalArgumentException("Claimant must be an AIUnit"
                + " or AIColony: " + claimant.getId());
        }
        return sendMessage(claimant.getAIMain().getAIPlayer(owner)
            .getConnection(), new ClaimLandMessage(tile, fcgo, price));
    }


    /**
     * Clears the speciality of a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to clear.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askClearSpeciality(AIUnit aiUnit) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new ClearSpecialityMessage(aiUnit.getUnit()));
    }


    /**
     * An AIUnit closes a transaction.
     *
     * @param aiUnit The <code>AIUnit</code> that closes the transaction.
     * @param settlement The target <code>Settlement</code>.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askCloseTransaction(AIUnit aiUnit,
                                              Settlement settlement) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new CloseTransactionMessage(aiUnit.getUnit(),
                                                       settlement));
    }


    /**
     * An AIUnit delivers a gift.
     *
     * @param aiUnit The <code>AIUnit</code> delivering the gift.
     * @param settlement The <code>Settlement</code> to give to.
     * @param goods The <code>Goods</code> to give.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askDeliverGift(AIUnit aiUnit, Settlement settlement,
                                         Goods goods) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new DeliverGiftMessage(aiUnit.getUnit(),
                                                  settlement, goods));
    }


    /**
     * An AIUnit disbands.
     *
     * @param aiUnit The <code>AIUnit</code> to disband.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askDisband(AIUnit aiUnit) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new DisbandUnitMessage(aiUnit.getUnit()));
    }


    /**
     * An AIUnit disembarks.
     *
     * @param aiUnit The <code>AIUnit</code> disembarking.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askDisembark(AIUnit aiUnit) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new DisembarkMessage(aiUnit.getUnit()));
    }


    /**
     * An AIUnit embarks.
     *
     * @param aiUnit The <code>AIUnit</code> carrier.
     * @param unit The <code>Unit</code> that is embarking.
     * @param direction The <code>Direction</code> to embark in (may be null).
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEmbark(AIUnit aiUnit, Unit unit,
                                    Direction direction) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new EmbarkMessage(unit, aiUnit.getUnit(),
                                             direction));
    }


    /**
     * A unit in Europe emigrates.
     *
     * @param aiPlayer The <code>AIPlayer</code> requiring emigration.
     * @param slot The slot to emigrate from.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEmigrate(AIPlayer aiPlayer, int slot) {
        return sendMessage(aiPlayer.getConnection(),
                           new EmigrateUnitMessage(slot));
    }


    /**
     * Ends the player turn.
     *
     * @param aiPlayer The <code>AIPlayer</code> ending the turn.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEndTurn(AIPlayer aiPlayer) {
        return sendTrivial(aiPlayer.getConnection(), "endTurn");
    }


    /**
     * Change the role of a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to equip.
     * @param role The <code>Role</code> to equip for.
     * @param roleCount The role count.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEquipForRole(AIUnit aiUnit, Role role,
                                          int roleCount) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
            new EquipForRoleMessage(aiUnit.getUnit(), role, roleCount));
    }


    /**
     * Establishes a mission in the given direction.
     *
     * @param aiUnit The <code>AIUnit</code> establishing the mission.
     * @param direction The <code>Direction</code> to move the unit.
     * @param denounce Is this a denunciation?
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEstablishMission(AIUnit aiUnit,
                                              Direction direction,
                                              boolean denounce) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new MissionaryMessage(aiUnit.getUnit(), direction,
                                                 denounce));
    }


    /**
     * Gets a nation summary for a player.
     *
     * @param owner The <code>AIPlayer</code> making the inquiry.
     * @param player The <code>Player</code> to summarize.
     * @return A <code>NationSummary</code> if the message was sent,
     *      and a non-error reply returned.
     */
    public static NationSummary askGetNationSummary(AIPlayer owner,
                                                    Player player) {
        Element reply = askExpecting(owner.getConnection(),
            new GetNationSummaryMessage(player).toXMLElement(),
            GetNationSummaryMessage.getXMLElementTagName());
        return new GetNationSummaryMessage(reply).getNationSummary();
    }


    /**
     * An AIUnit gets a transaction.
     *
     * @param aiUnit The <code>AIUnit</code> that gets a transaction.
     * @param settlement The target <code>Settlement</code>.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askGetTransaction(AIUnit aiUnit,
                                            Settlement settlement) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new GetTransactionMessage(aiUnit.getUnit(),
                                                     settlement));
    }


    /**
     * Makes demands to a colony.  One and only one of goods or gold is valid.
     *
     * @param aiUnit The <code>AIUnit</code> that is demanding.
     * @param colony The <code>Colony</code> to demand of.
     * @param type The <code>GoodsType</code> to demand.
     * @param amount The amount of goods to demand.
     * @return True if the message was sent, a non-error reply returned, and
     *     the demand was accepted.
     */
    public static boolean askIndianDemand(AIUnit aiUnit, Colony colony,
                                          GoodsType type, int amount) {
        Element reply = askExpecting(aiUnit.getAIOwner().getConnection(),
            new IndianDemandMessage(aiUnit.getUnit(), colony,
                                    type, amount).toXMLElement(),
            IndianDemandMessage.getXMLElementTagName());
        return new IndianDemandMessage(colony.getGame(), reply).getResult();
    }


    /**
     * An AI unit loads some cargo.
     *
     * @param loc The <code>Location</code> where the goods are.
     * @param type The <code>GoodsType</code> to load.
     * @param amount The amount of goods to load.
     * @param aiUnit The <code>AIUnit</code> that is loading.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askLoadGoods(Location loc, GoodsType type,
                                       int amount, AIUnit aiUnit) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
            new LoadGoodsMessage(loc, type, amount, aiUnit.getUnit()));
    }


    /**
     * An AI unit loots some cargo.
     *
     * @param aiUnit The <code>AIUnit</code> that is looting.
     * @param defenderId The object identifier of the defending unit.
     * @param goods A list of <code>Goods</code> to loot.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askLoot(AIUnit aiUnit, String defenderId,
                                  List<Goods> goods) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new LootCargoMessage(aiUnit.getUnit(), defenderId,
                                                goods));
    }


    /**
     * Moves an AIUnit in the given direction.
     *
     * @param aiUnit The <code>AIUnit</code> to move.
     * @param direction The <code>Direction</code> to move the unit.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askMove(AIUnit aiUnit, Direction direction) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new MoveMessage(aiUnit.getUnit(), direction));
    }


    /**
     * Moves an AIUnit across the high seas.
     *
     * @param aiUnit The <code>AIUnit</code> to move.
     * @param destination The <code>Location</code> to move to.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askMoveTo(AIUnit aiUnit, Location destination) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new MoveToMessage(aiUnit.getUnit(), destination));
    }


    /**
     * An AIUnit is put outside a colony.
     *
     * @param aiUnit The <code>AIUnit</code> to put out.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askPutOutsideColony(AIUnit aiUnit) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new PutOutsideColonyMessage(aiUnit.getUnit()));
    }


    /**
     * Rearrange an AI colony.
     *
     * @param aiColony The <code>AIColony</code> to rearrange.
     * @param workers A list of worker <code>Unit</code>s that may change.
     * @param scratch A copy of the underlying <code>Colony</code> with the
     *     workers arranged as required.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askRearrangeColony(AIColony aiColony,
                                             List<Unit> workers,
                                             Colony scratch) {
        Colony colony = aiColony.getColony();
        RearrangeColonyMessage message = new RearrangeColonyMessage(colony);
        for (Unit u : workers) {
            Unit su = scratch.getCorresponding(u);
            if (u.getLocation().getId().equals(su.getLocation().getId())
                && u.getWorkType() == su.getWorkType()
                && u.getRole() == su.getRole()
                && u.getRoleCount() == su.getRoleCount()) continue;
            message.addChange(u,
                (Location)colony.getCorresponding((FreeColObject)su.getLocation()),
                su.getWorkType(), su.getRole(), su.getRoleCount());
        }
        return (message.isEmpty()) ? false
            : sendMessage(aiColony.getAIOwner().getConnection(), message);
    }
        

    /**
     * An AI unit scouts a native settlement.
     *
     * @param aiUnit The <code>AIUnit</code> that is scouting.
     * @param direction The <code>Direction</code> to move.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askScoutSpeakToChief(AIUnit aiUnit,
                                               Direction direction) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new ScoutSpeakToChiefMessage(aiUnit.getUnit(), 
                                                        direction));
    }


    /**
     * Set the build queue in a colony.
     *
     * @param aiColony The <code>AIColony</code> that is building.
     * @param queue The list of <code>BuildableType</code>s to build.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askSetBuildQueue(AIColony aiColony,
                                           List<BuildableType> queue) {
        return sendMessage(aiColony.getAIOwner().getConnection(),
                           new SetBuildQueueMessage(aiColony.getColony(),
                                                    queue));
    }


    /**
     * Train unit in Europe.
     *
     * @param aiPlayer The <code>AIPlayer</code> requiring training.
     * @param type The <code>UnitType</code> to train.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askTrainUnitInEurope(AIPlayer aiPlayer,
                                               UnitType type) {
        return sendMessage(aiPlayer.getConnection(),
                           new TrainUnitInEuropeMessage(type));
    }


    /**
     * An AI unit unloads some cargo.
     *
     * @param type The <code>GoodsType</code> to unload.
     * @param amount The amount of goods to unload.
     * @param aiUnit The <code>AIUnit</code> that is unloading.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askUnloadGoods(GoodsType type, int amount,
                                         AIUnit aiUnit) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
            new UnloadGoodsMessage(type, amount, aiUnit.getUnit()));
    }


    /**
     * Set a unit to work in a work location.
     *
     * @param aiUnit The <code>AIUnit</code> to work.
     * @param workLocation The <code>WorkLocation</code> to work in.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askWork(AIUnit aiUnit, WorkLocation workLocation) {
        return sendMessage(aiUnit.getAIOwner().getConnection(),
                           new WorkMessage(aiUnit.getUnit(), workLocation));
    }
}
