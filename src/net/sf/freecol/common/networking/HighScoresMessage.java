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

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when an high score query/response occurs.
 */
public class HighScoresMessage extends ObjectMessage {

    public static final String TAG = "highScores";
    private static final String KEY_TAG = "key";


    /**
     * Create a new {@code HighScoresMessage} in request form (no
     * scores attached).
     *
     * @param key A message key for the final display.
     * @param scores The list of high scores, or null.
     */
    public HighScoresMessage(String key, List<HighScore> scores) {
        super(TAG, KEY_TAG, key);

        appendChildren(scores);
    }

    /**
     * Create a new {@code HighScoresMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public HighScoresMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, KEY_TAG);

        List<HighScore> scores = new ArrayList<>();
        while (xr.moreTags()) {
            String tag = xr.getLocalName();
            if (HighScore.TAG.equals(tag)) {
                scores.add(new HighScore(xr));
            } else {
                expected(HighScore.TAG, tag);
            }
            xr.expectTag(tag);
        }
        xr.expectTag(TAG);
        appendChildren(scores);
    }


    /**
     * Accessor for the key.
     *
     * @return The key.
     */
    private String getKey() {
        return getStringAttribute(KEY_TAG);
    }

    /**
     * Accessor for the scores list.
     *
     * @return The list of {@code HighScore}s.
     */
    private List<HighScore> getScores() {
        return getChildren(HighScore.class);
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
        final String key = getKey();
        final List<HighScore> scores = getScores();

        igc(freeColClient).highScoresHandler(key, scores);
        clientGeneric(freeColClient);
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        return igc(freeColServer)
            .getHighScores(serverPlayer, getKey());
    }
}
