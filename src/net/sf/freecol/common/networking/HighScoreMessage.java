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

import net.sf.freecol.common.util.DOMUtils;
import org.w3c.dom.Element;


/**
 * The message sent when an highScore occurs.
 */
public class HighScoreMessage extends ObjectMessage {

    public static final String TAG = "highScore";
    private static final String KEY_TAG = "key";

    /** The high scores. */
    private final List<HighScore> scores = new ArrayList<>();
    

    /**
     * Create a new {@code HighScoreMessage} in request form (no
     * scores attached).
     *
     * @param key A message key for the final display.
     */
    public HighScoreMessage(String key) {
        super(TAG, KEY_TAG, key);

        this.scores.clear();
    }

    /**
     * Create a new {@code HighScoreMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public HighScoreMessage(Game game, Element element) {
        this(getStringAttribute(element, KEY_TAG));

        setScores(DOMUtils.getChildren(game, element, HighScore.class));
    }

    /**
     * Create a new {@code HighScoreMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public HighScoreMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, KEY_TAG);

        this.scores.clear();
        while (xr.moreTags()) {
            String tag = xr.getLocalName();
            if (HighScore.TAG.equals(tag)) {
                HighScore hs = xr.readFreeColObject(game, HighScore.class);
                if (hs != null) this.scores.add(hs);
            } else {
                expected(HighScore.TAG, tag);
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
        final String key = getKey();
        final List<HighScore> scores = getScores();

        igc(freeColClient).highScoreHandler(key, scores);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        for (HighScore hs : this.scores) hs.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            KEY_TAG, getKey())
            .add(this.scores).toXMLElement();
    }

    
    // Public interface

    /**
     * Accessor for the key.
     *
     * @return The key.
     */
    public String getKey() {
        return getStringAttribute(KEY_TAG);
    }

    /**
     * Accessor for the scores list.
     *
     * @return The list of {@code HighScore}s.
     */
    public List<HighScore> getScores() {
        return this.scores;
    }

    /**
     * Set the scores for this message.
     *
     * @param scores A list of {@code HighScore}s to add.
     * @return This message.
     */
    public HighScoreMessage setScores(List<HighScore> scores) {
        this.scores.clear();
        this.scores.addAll(scores);
        return this;
    }
}
