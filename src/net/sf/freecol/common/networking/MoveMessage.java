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

package net.sf.freecol.common.networking;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when moving a unit.
 */
public class MoveMessage extends Message {
    /**
     * The id of the object to be moved.
     */
    private String unitId;

    /**
     * The direction to move.
     */
    private String directionString;

    /**
     * Create a new <code>MoveMessage</code> for the supplied unit and
     * direction.
     *
     * @param unit The <code>Unit</code> to move.
     * @param direction The <code>Direction</code> to move in.
     */
    public MoveMessage(Unit unit, Direction direction) {
        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
    }

    /**
     * Create a new <code>MoveMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public MoveMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.directionString = element.getAttribute("direction");
    }

    /**
     * Update other players when a unit moves.
     *
     * @param others A list of other <code>ServerPlayer</code>s to update.
     * @param contacts A list of <code>ServerPlayer</code>s newly
     *                 contacting this player.
     * @param unit The <code>Unit</code> that is moving.
     * @param oldTile The <code>Tile</code> the unit was at.
     * @param direction The <code>Direction</code> the unit moved.
     * @param newTile The <code>Tile</code> the unit has moved to.
     */
    private void updateOthers(List<ServerPlayer> others,
                              List<ServerPlayer> contacts,
                              Unit unit, Tile oldTile,
                              Direction direction, Tile newTile) {
        for (ServerPlayer enemyPlayer : others) {
            if (!enemyPlayer.isConnected()) continue;
            Boolean seeOld = enemyPlayer.canSee(oldTile)
                && oldTile.getSettlement() == null;
            Boolean seeNew = enemyPlayer.canSee(newTile)
                && newTile.getSettlement() == null;
            if (seeOld || seeNew) {
                Element multiple = Message.createNewRootElement("multiple");
                Document doc = multiple.getOwnerDocument();
                // To animate we need to know which unit is moving,
                // from, and to.  We put this in attributes on
                // animateMove up front as it needs to be independent
                // of the real update.
                Element animate = doc.createElement("animateMove");
                multiple.appendChild(animate);
                animate.setAttribute("unit", unit.getId());
                animate.setAttribute("oldTile", oldTile.getId());
                animate.setAttribute("newTile", newTile.getId());
                // We can not rely on the unit that is about to move
                // being present on the client side, and it is needed
                // before we can run the animation, so it is sent in
                // the opponentMove (albeit in the new position).  The
                // previous code only sent the unit when the client
                // was believed to not be able to see it and thus not
                // have any record thereof, however when there was any
                // disagreement on this point desynchronization
                // occurred.  Thus for the present, the unit is always
                // sent, on the principle of getting it right first and
                // optimizing later.
                animate.appendChild(unit.toXMLElement(enemyPlayer, doc,
                                                      false, false));

                // The real state-changing part of the message follows.
                // Add stance setting if this is a new contact.
                if (contacts.contains(enemyPlayer)) {
                    // TODO: make a message type.
                    Element stance = doc.createElement("setStance");
                    multiple.appendChild(stance);
                    stance.setAttribute("stance", Stance.PEACE.toString());
                    stance.setAttribute("first", enemyPlayer.getId());
                    stance.setAttribute("second", unit.getOwner().getId());
                }

                // If this player can see the new tile we just update
                // both tiles. If not, update the old tile and remove
                // the unit.
                Element update = doc.createElement("update");
                multiple.appendChild(update);
                if (seeOld) {
                    update.appendChild(oldTile.toXMLElement(enemyPlayer, doc,
                                                            false, false));
                }
                if (seeNew && !unit.isDisposed()) {
                    update.appendChild(newTile.toXMLElement(enemyPlayer, doc,
                                                            false, false));
                } else {
                    Element remove = doc.createElement("remove");
                    multiple.appendChild(remove);
                    unit.addToRemoveElement(remove);
                }
                try {
                    enemyPlayer.getConnection().sendAndWait(multiple);
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
    }

    /**
     * Handle a "move"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the moved unit,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = server.getGame();
        Unit unit;
        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        Tile oldTile = unit.getTile();
        if (oldTile == null) {
            return Message.clientError("Unit is not on the map: " + unitId);
        }
        Location oldLocation = unit.getLocation();
        Direction direction = Enum.valueOf(Direction.class, directionString);
        Tile newTile = game.getMap().getNeighbourOrNull(direction, oldTile);
        if (newTile == null) {
            return Message.clientError("Could not find tile"
                                       + " in direction: " + direction
                                       + " from unit: " + unitId);
        }
        MoveType moveType = unit.getMoveType(direction);
        if (!moveType.isProgress()) {
            return Message.clientError("Illegal move for: " + unitId
                                       + " type: " + moveType
                                       + " from: " + oldLocation.getId()
                                       + " to: " + newTile.getId());
        }

        // Plan to update tiles that could not be seen before but will
        // now be within the line-of-sight.
        InGameController controller = server.getInGameController();
        List<FreeColObject> objects = new ArrayList<FreeColObject>();
        int los = unit.getLineOfSight();
        for (Tile tile : game.getMap().getSurroundingTiles(newTile, los)) {
            if (!player.canSee(tile)) {
                objects.add(tile);
            }
        }

        // Collect the new contacts, and make the move (including
        // resolving any rumours).
        List<ServerPlayer> contacts
            = controller.findAdjacentUncontacted(serverPlayer, newTile);
        objects.addAll(controller.move(serverPlayer, unit, newTile));

        // Inform others about the move.
        updateOthers(controller.getOtherPlayers(serverPlayer), contacts,
                     unit, oldLocation.getTile(), direction, newTile);

        // Begin building the reply,
        Element reply = Message.createNewRootElement("multiple");
        Document doc = reply.getOwnerDocument();
        Element update = doc.createElement("update");
        Element addMessages = doc.createElement("addMessages");
        Element addHistory = doc.createElement("addHistory");
        Element remove = doc.createElement("remove");

        // always updating the old location,
        if (oldLocation instanceof Tile) {
            update.appendChild(((Tile) oldLocation).toXMLElement(player, doc));
        } else if (oldLocation instanceof Unit) {
            update.appendChild(((Unit) oldLocation).toXMLElement(player, doc));
            unit.setMovesLeft(0); // Disembark always consumes moves.
        } else {
            throw new IllegalArgumentException("Location not a tile or unit!?!: " + unit.getId());
        }
        // ...but delay doing the new location as more can happen there still.

        // If the unit dies, remove it, if not, animate the move.
        if (unit.isDisposed()) {
            remove = doc.createElement("remove");
            unit.addToRemoveElement(remove);
        } else {
            Element animate = doc.createElement("animateMove");
            reply.appendChild(animate);
            animate.setAttribute("unit", unit.getId());
            animate.setAttribute("oldTile", oldLocation.getTile().getId());
            animate.setAttribute("newTile", newTile.getId());
        }

        // Consider all the other objects known to have changed.
        for (FreeColObject object : objects) {
            if (object == player) { // Only returned if received gold
                update.appendChild(player.toXMLElementPartial(doc, "gold", "score"));
            } else if (object instanceof ModelMessage) {
                addMessages.appendChild(object.toXMLElement(player, doc));
            } else if (object instanceof HistoryEvent) {
                addHistory.appendChild(object.toXMLElement(player, doc));
            } else if (object instanceof ServerPlayer && contacts.contains(object)) {
                ServerPlayer other = (ServerPlayer) object;
                Element stance = doc.createElement("setStance");
                reply.appendChild(stance);
                stance.setAttribute("stance", Stance.PEACE.toString());
                stance.setAttribute("first", serverPlayer.getId());
                stance.setAttribute("second", other.getId());
                // Add a history event for first contacts,
                HistoryEvent h = new HistoryEvent(game.getTurn().getNumber(),
                                                  HistoryEvent.EventType.MEET_NATION)
                    .addStringTemplate("%nation%", other.getNationName());
                serverPlayer.addHistory(h);
                addHistory.appendChild(h.toXMLElement(player, doc));
            } else { // native player, Europe, Tile
                update.appendChild(object.toXMLElement(player, doc,
                                                        false, false));
            }
        }

        // Add on a bunch of special attributes to trigger further
        // action by European player clients.
        if (!unit.isDisposed() && player.isEuropean()) {
            Unit slowedBy = controller.getSlowedBy(unit, newTile);
            if (slowedBy != null) {
                reply.setAttribute("slowedBy", slowedBy.getId());
            }

            if (newTile.isLand() && !player.isNewLandNamed()) {
                String newLandName = player.getDefaultNewLandName();
                if (player.isAI()) {
                    // TODO: Not convinced shortcutting the AI like
                    // this is a good idea--- even though this is
                    // trivial functionality, it really should be in
                    // the AI code.
                    player.setNewLandName(newLandName);
                } else { // Ask player to name the land.
                    reply.setAttribute("nameNewLand", newLandName);
                }
            }

            Region region = newTile.getDiscoverableRegion();
            if (region != null) {
                HistoryEvent h = null;
                if (region.isPacific()) {
                    reply.setAttribute("discoverPacific", "true");
                    h = region.discover(serverPlayer, game.getTurn(),
                                        "model.region.pacific");
                } else {
                    String regionName = player.getDefaultRegionName(region.getType());
                    if (player.isAI()) {
                        // TODO: here is another dubious AI shortcut.
                        h = region.discover(serverPlayer, game.getTurn(),
                                            regionName);
                        controller.sendUpdateToAll(serverPlayer, region);
                    } else { // Ask player to name the region.
                        reply.setAttribute("discoverRegion", regionName);
                        reply.setAttribute("regionType", Messages.message(region.getLabel()));
                    }
                }
                if (h != null) {
                    serverPlayer.addHistory(h);
                    addHistory.appendChild(h.toXMLElement(player, doc));
                    update.appendChild(region.toXMLElement(player, doc));
                }
            }

            int emigrants = serverPlayer.getRemainingEmigrants();
            if (emigrants > 0) {
                reply.setAttribute("fountainOfYouth", Integer.toString(emigrants));
            }
        }

        // At last, update the new tile.  It was necessary to wait
        // until after the slowedBy processing or the update would
        // miss the loss of moves.  Note that it is *always* necessary
        // to update the new tile, even if the unit died as that only
        // happens as a result of a LostCityRumour so the tile must
        // now show the rumour has been explored.
        update.appendChild(newTile.toXMLElement(player, doc, false, false));

        // Add on the parts.
        reply.appendChild(update);
        if (addMessages.hasChildNodes()) reply.appendChild(addMessages);
        if (addHistory.hasChildNodes()) reply.appendChild(addHistory);
        if (remove.hasChildNodes()) reply.appendChild(remove);
        return reply;
    }

    /**
     * Convert this MoveMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", this.unitId);
        result.setAttribute("direction", this.directionString);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "move".
     */
    public static String getXMLElementTagName() {
        return "move";
    }
}
