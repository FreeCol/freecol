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
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when setting goods levels.
 */
public class SetGoodsLevelsMessage extends ObjectMessage {

    public static final String TAG = "setGoodsLevels";
    private static final String COLONY_TAG = "colony";


    /**
     * Create a new {@code SetGoodsLevelsMessage} with the
     * supplied colony and data.
     *
     * @param colony The {@code Colony} where the goods leves are set.
     * @param data The new {@code ExportData}.
     */
    public SetGoodsLevelsMessage(Colony colony, ExportData data) {
        super(TAG, COLONY_TAG, colony.getId());

        appendChild(data);
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

        FreeColXMLReader.ReadScope rs
            = xr.replaceScope(FreeColXMLReader.ReadScope.NOINTERN);
        ExportData data = null;
        try {
            while (xr.moreTags()) {
                String tag = xr.getLocalName();
                if (ExportData.TAG.equals(tag)) {
                    if (data == null) {
                        data = xr.readFreeColObject(game, ExportData.class);
                    } else {
                        expected(TAG, tag);
                    }
                } else {
                    expected(ExportData.TAG, tag);
                }
                xr.expectTag(tag);
            }
            xr.expectTag(TAG);
        } finally {
            xr.replaceScope(rs);
        }
        appendChild(data);
    }


    /**
     * Get the export data.
     *
     * @return The {@code ExportData}.
     */
    private ExportData getExportData() {
        return getChild(0, ExportData.class);
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
        ExportData exportData = getExportData();
        if (exportData == null) {
            return serverPlayer.clientError("No export data present.");
        }

        // Proceed to set.
        return igc(freeColServer)
            .setGoodsLevels(serverPlayer, colony, exportData);
    }
}
