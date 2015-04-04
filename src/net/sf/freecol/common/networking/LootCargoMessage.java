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

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message sent when looting cargo.
 */
public class LootCargoMessage extends DOMMessage {

    /** The object identifier of the unit that is looting. */
    private final String winnerId;

    /** The object identifier of the unit that is looted. */
    private final String loserId;

    /** The goods to be looted. */
    private final List<Goods> goods;


    /**
     * Create a new <code>LootCargoMessage</code>.
     *
     * @param winner The <code>Unit</code> that is looting.
     * @param loserId The identifier of the <code>Unit</code> that is looted.
     * @param goods The <code>AbstractGoods</code> to loot.
     */
    public LootCargoMessage(Unit winner, String loserId, List<Goods> goods) {
        super(getXMLElementTagName());

        this.winnerId = winner.getId();
        this.loserId = loserId;
        this.goods = (goods == null) ? null : new ArrayList<>(goods);
    }

    /**
     * Create a new <code>LootCargoMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public LootCargoMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.winnerId = element.getAttribute("winner");
        this.loserId = element.getAttribute("loser");
        NodeList children = element.getChildNodes();
        if (children.getLength() == 0) {
            this.goods = null;
        } else {
            this.goods = new ArrayList<>();
            for (int i = 0; i < children.getLength(); i++) {
                this.goods.add(new Goods(game, (Element) children.item(i)));
            }
        }
    }


    // Public interface

    /**
     * Public accessor to help the client in game controller.
     *
     * @return The winner unit.
     */
    public Unit getUnit(Game game) throws ClassCastException {
        return game.getFreeColGameObject(winnerId, Unit.class);
    }

    /**
     * Public accessor to help the client in game controller.
     *
     * @return The defender Object Identifier.
     */
    public String getDefenderId() {
        return loserId;
    }

    /**
     * Public accessor to help the client in game controller.
     *
     * @return The goods to loot.
     */
    public List<Goods> getGoods() {
        return goods;
    }


    /**
     * Handle a "lootCargo"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An <code>Element</code> encapsulating the looting.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = server.getGame();

        Unit winner;
        try {
            winner = getUnit(game);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }
        // Do not check loserId, as it might have sunk.  It is enough
        // that the attacker knows it.  Similarly the server is better
        // placed to check the goods validity.

        // Try to loot.
        return server.getInGameController()
            .lootCargo(serverPlayer, winner, loserId, goods);
    }

    /**
     * Convert this LootCargoMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "winner", winnerId,
            "loser", loserId);
        if (goods != null) {
            for (Goods g : goods) {
                result.appendChild(g.toXMLElement(result.getOwnerDocument()));
            }
        }
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "lootCargo".
     */
    public static String getXMLElementTagName() {
        return "lootCargo";
    }
}
