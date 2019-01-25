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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when delivering a gift to a Settlement.
 */
public class DeliverGiftMessage extends ObjectMessage {

    public static final String TAG = "deliverGift";
    private static final String SETTLEMENT_TAG = "settlement";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code DeliverGiftMessage}.
     *
     * @param unit The {@code Unit} that is trading.
     * @param is The {@code IndianSettlement} that is trading.
     * @param goods The {@code Goods} to deliverGift.
     */
    public DeliverGiftMessage(Unit unit, IndianSettlement is, Goods goods) {
        super(TAG, UNIT_TAG, unit.getId(), SETTLEMENT_TAG, is.getId());

        appendChild(goods);
    }

    /**
     * Create a new {@code DeliverGiftMessage} from stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there a problem reading the stream.
     */
    public DeliverGiftMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, UNIT_TAG, SETTLEMENT_TAG);

        Goods goods = null;
        while (xr.moreTags()) {
            String tag = xr.getLocalName();
            if (Goods.TAG.equals(tag)) {
                if (goods == null) {
                    goods = xr.readFreeColObject(game, Goods.class);
                } else {
                    expected(TAG, tag);
                }
            } else {
                expected(Goods.TAG, tag);
            }
            xr.expectTag(tag);
        }
        xr.expectTag(TAG);
        appendChild(goods);
    }
    

    /** Currently unused
     * Get the {@code Unit} which is delivering the gift.  This
     * is a helper routine to be called in-client as it blindly trusts
     * its field.
     *
     * @param game The {@code Game} to lookup the unit in.
     * @return The {@code Unit}, or null if none.
    private Unit getUnit(Game game) {
        return game.getFreeColGameObject(getStringAttribute(UNIT_TAG),
                                         Unit.class);
    }
     */

    /** Currently unused
     * Get the {@code Settlement} which is receiving the gift.
     * This is a helper routine to be called in-client as it blindly trusts
     * its field.
     *
     * @param game The {@code Game} to lookup the unit in.
     * @return The {@code Settlement}, or null if none.
    private IndianSettlement getSettlement(Game game) {
        return game.getFreeColGameObject(getStringAttribute(SETTLEMENT_TAG),
                                         IndianSettlement.class);
    }
     */

    /**
     * Get the {@code Goods} delivered as a gift.
     *
     * @return The {@code Goods}, or null if none.
     */
    private Goods getGoods() {
        return getChild(0, Goods.class);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean currentPlayerMessage() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        String unitId = getStringAttribute(UNIT_TAG);
        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        IndianSettlement is;
        try {
            is = unit.getAdjacentSettlement(getStringAttribute(SETTLEMENT_TAG),
                                            IndianSettlement.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        Goods goods = getGoods();
        if (goods == null) {
            return serverPlayer.clientError("No goods found");
        } else if (goods.getLocation() != unit) {
            // Make sure we are trying to deliver something that is there
            return serverPlayer.clientError("Gift " + goods.getId()
                + " is not with unit " + unitId);
        }

        // Proceed to deliver.
        return igc(freeColServer)
            .deliverGiftToSettlement(serverPlayer, unit, is, goods);
    }
}
