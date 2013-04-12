/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when naming a new land.
 */
public class NewLandNameMessage extends DOMMessage {

    /**
     * The unit that has come ashore.
     */
    private String unitId;

    /**
     * The name to use.
     */
    private String newLandName;

    /**
     * An optional welcoming player.
     */
    private String welcomerId;

    /**
     * An optional number of camps for the welcome message.
     */
    private String campCount;

    /**
     * Has a treaty been accepted with the welcomer?
     */
    private String acceptString;

    /**
     * Create a new <code>NewLandNameMessage</code> with the
     * supplied name.
     *
     * @param unit The <code>Unit</code> that has come ashore.
     * @param newLandName The new land name.
     * @param welcomer The optional <Player>welcomer</code> nation.
     * @param camps The optional number of camps of the welcomer nation.
     * @param accept Accept the welcomer offer?
     */
    public NewLandNameMessage(Unit unit, String newLandName,
                              Player welcomer, int camps, boolean accept) {
        this.unitId = unit.getId();
        this.newLandName = newLandName;
        if (welcomer == null) {
            this.welcomerId = null;
            this.campCount = null;
        } else {
            this.welcomerId = welcomer.getId();
            this.campCount = Integer.toString(camps);
        }
        this.acceptString = Boolean.toString(accept);
    }

    /**
     * Create a new <code>NewLandNameMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public NewLandNameMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.newLandName = element.getAttribute("newLandName");
        if (element.hasAttribute("welcomer")) {
            this.welcomerId = element.getAttribute("welcomer");
            this.campCount = element.getAttribute("camps");
        } else {
            this.welcomerId = null;
            this.campCount = null;
        }
        this.acceptString = element.getAttribute("accept");
    }

    /**
     * Public accessor for the unit.
     *
     * @param game The <code>Game</code> to look for a unit in.
     * @return The unit of this message.
     */
    public Unit getUnit(Game game) {
        return game.getFreeColGameObject(unitId, Unit.class);
    }

    /**
     * Public accessor for the new land name.
     *
     * @return The new land name of this message.
     */
    public String getNewLandName() {
        return newLandName;
    }

    /**
     * Public accessor for the welcomer.
     *
     * @param game The <code>Game</code> to look for a welcomer in.
     * @return The welcomer of this message.
     */
    public Player getWelcomer(Game game) {
        return game.getFreeColGameObject(welcomerId, Player.class);
    }

    /**
     * Sets the accept value of this message.
     *
     * @param accept The new accept value.
     */
    public void setAccept(boolean accept) {
        this.acceptString = Boolean.toString(accept);
    }

    /**
     * Public accessor for the camp count.
     *
     * @return The camp count of this message.
     */
    public String getCamps() {
        return campCount;
    }

    /**
     * Handle a "newLandName"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update setting the new land name, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        Game game = server.getGame();
        ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        Tile tile = unit.getTile();
        if (tile == null) {
            return DOMMessage.clientError("Unit is not on the map: " + unitId);
        } else if (!tile.isLand()) {
            return DOMMessage.clientError("Unit is not in the new world: "
                + unitId);
        }

        if (newLandName == null || newLandName.length() == 0) {
            return DOMMessage.clientError("Empty new land name");
        }

        ServerPlayer welcomer = null;
        int camps = 0;
        if (welcomerId != null) {
            welcomer = game.getFreeColGameObject(welcomerId,
                                                 ServerPlayer.class);
            if (welcomer == null) {
                return DOMMessage.clientError("Not a player: " + welcomerId);
            } else if (!welcomer.isIndian()) {
                return DOMMessage.clientError("Not a native player: "
                    + welcomerId);
            }
            boolean foundWelcomer = false;
            for (Tile t : tile.getSurroundingTiles(1)) {
                if (t.getFirstUnit() != null
                    && t.getFirstUnit().getOwner() == welcomer) {
                    foundWelcomer = true;
                    break;
                }
            }
            if (!foundWelcomer) {
                return DOMMessage.clientError("Unit is not next to welcomer.");
            } else if (!welcomer.owns(tile)) {
                return DOMMessage.clientError("Welcomer offers unowned tile: "
                    + tile.getId());
            }
            try {
                camps = Integer.parseInt(campCount);
            } catch (NumberFormatException e) {
                return DOMMessage.clientError("Invalid camp count: "
                    + campCount);
            }
        }

        boolean accept = Boolean.valueOf(acceptString);

        // Set name.
        return server.getInGameController()
            .setNewLandName(serverPlayer, unit, newLandName, welcomer, camps,
                accept);
    }

    /**
     * Convert this NewLandNameMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "unit", unitId,
            "newLandName", newLandName);
        if (welcomerId != null) result.setAttribute("welcomer", welcomerId);
        if (campCount != null) result.setAttribute("camps", campCount);
        if (acceptString != null) result.setAttribute("accept", acceptString);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "newLandName".
     */
    public static String getXMLElementTagName() {
        return "newLandName";
    }
}
