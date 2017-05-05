/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when spying on a settlement.
 */
public class SpySettlementMessage extends ObjectMessage {

    public static final String TAG = "spySettlement";
    private static final String SETTLEMENT_TAG = "settlement";
    private static final String UNIT_TAG = "unit";

    /**
     * A copy of the tile with the settlement on it, but including all
     * the extra (normally invisible) information.
     */
    private Tile spyTile;


    /**
     * Create a new {@code SpySettlementMessage} request with the
     * supplied unit and settlement
     *
     * @param unit The {@code Unit} that is spying.
     * @param settlement The {@code Settlement} the unit is looking at.
     */
    public SpySettlementMessage(Unit unit, Settlement settlement) {
        super(TAG, SETTLEMENT_TAG, settlement.getId(), UNIT_TAG, unit.getId());

        this.spyTile = settlement.getTile();
    }

    /**
     * Create a new {@code SpySettlementMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public SpySettlementMessage(Game game, Element element) {
        super(TAG, SETTLEMENT_TAG, getStringAttribute(element, SETTLEMENT_TAG),
              UNIT_TAG, getStringAttribute(element, UNIT_TAG));

        this.spyTile = getChild(game, element, 0, false, Tile.class);
    }

    /**
     * Create a new {@code SpySettlementMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public SpySettlementMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, SETTLEMENT_TAG, UNIT_TAG);

        this.spyTile = null;
        while (xr.moreTags()) {
            String tag = xr.getLocalName();
            if (Tile.TAG.equals(tag)) {
                if (this.spyTile == null) {
                    this.spyTile = xr.readFreeColObject(game, Tile.class);
                } else {
                    expected(TAG, tag);
                }
            } else {
                expected(Tile.TAG, tag);
            }
            xr.expectTag(tag);
        }
        xr.expectTag(TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Tile spyTile = getSpyTile();

        igc(freeColClient).spySettlementHandler(spyTile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();

        String unitId = getStringAttribute(UNIT_TAG);
        Unit unit;
        try {
            unit = getUnit(serverPlayer);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        if (!unit.hasAbility(Ability.SPY_ON_COLONY)) {
            return serverPlayer.clientError("Unit lacks ability"
                + " to spy on colony: " + unitId);
        }

        String settlementId = getStringAttribute(SETTLEMENT_TAG);
        Colony colony = getColony(game);
        if (colony == null) {
            return serverPlayer.clientError("Not a colony: " + settlementId);
        }
        Tile tile = colony.getTile();
        if (!unit.getTile().isAdjacent(tile)) {
            return serverPlayer.clientError("Unit " + unitId
                + " not adjacent to colony: " + settlementId);
        }

        MoveType type = unit.getMoveType(tile);
        if (type != MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT) {
            return serverPlayer.clientError("Unable to enter at: "
                + colony.getName() + ": " + type.whyIllegal());
        }

        // Spy on the settlement
        return igc(freeColServer)
            .spySettlement(serverPlayer, unit, colony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        if (this.spyTile != null) {
            WriteScope ws = xw.replaceScope(WriteScope.toServer());
            try {
                this.spyTile.toXML(xw);
            } finally {
                xw.replaceScope(ws);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            UNIT_TAG, getStringAttribute(UNIT_TAG),
            SETTLEMENT_TAG, getStringAttribute(SETTLEMENT_TAG))
            .add(this.spyTile).toXMLElement();
        
    }


    // Public interface

    public Tile getSpyTile() {
        return this.spyTile;
    }

    public Unit getUnit(Player player) {
        return player.getOurFreeColGameObject(getStringAttribute(UNIT_TAG),
                                              Unit.class);
    }

    public Colony getColony(Game game) {
        return game.getFreeColGameObject(getStringAttribute(SETTLEMENT_TAG),
                                         Colony.class);
    }
}
