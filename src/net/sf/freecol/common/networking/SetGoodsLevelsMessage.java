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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when setting goods levels.
 */
public class SetGoodsLevelsMessage extends ObjectMessage {

    public static final String TAG = "setGoodsLevels";
    private static final String COLONY_TAG = "colony";

    /** The new ExportData. */
    private ExportData data = null;


    /**
     * Create a new {@code SetGoodsLevelsMessage} with the
     * supplied colony and data.
     *
     * @param colony The {@code Colony} where the goods leves are set.
     * @param data The new {@code ExportData}.
     */
    public SetGoodsLevelsMessage(Colony colony, ExportData data) {
        super(TAG, COLONY_TAG, colony.getId());

        this.data = data;
    }

    /**
     * Create a new {@code SetGoodsLevelsMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public SetGoodsLevelsMessage(Game game, Element element) {
        super(TAG, COLONY_TAG, getStringAttribute(element, COLONY_TAG));

        this.data = getChild(game, element, 0, ExportData.class);
    }

    /**
     * Create a new {@code SetGoodsLevelsMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public SetGoodsLevelsMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, COLONY_TAG);

        this.data = null;
        while (xr.moreTags()) {
            String tag = xr.getLocalName();
            if (ExportData.TAG.equals(tag)) {
                if (this.data == null) {
                    this.data = xr.readFreeColObject(game, ExportData.class);
                } else {
                    expected(TAG, tag);
                }
            } else {
                expected(ExportData.TAG, tag);
            }
            xr.expectTag(tag);
        }
        xr.expectTag(TAG);
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
    public MessagePriority getPriority() {
        return MessagePriority.NORMAL;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        Colony colony;
        try {
            colony = serverPlayer.getOurFreeColGameObject(getStringAttribute(COLONY_TAG),
                                                          Colony.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        // Proceed to set.
        return igc(freeColServer)
            .setGoodsLevels(serverPlayer, colony, this.data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        if (this.data != null) this.data.toXML(xw);
    }

    /**
     * Convert this SetGoodsLevelsMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            COLONY_TAG, getStringAttribute(COLONY_TAG))
            .add(this.data).toXMLElement();
    }
}
