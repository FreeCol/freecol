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

    /** An optional key for the final display. */
    private final String key;

    /** The high scores. */
    private final List<HighScore> scores = new ArrayList<>();
    

    /**
     * Create a new {@code HighScoreMessage} in request form (no
     * scores attached).
     *
     * @param key A message key for the final display.
     */
    public HighScoreMessage(String key) {
        super(TAG);

        this.key = key;
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
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }


    // Public interface

    /**
     * Accessor for the key.
     *
     * @return The key.
     */
    public String getKey() {
        return this.key;
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

    
    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        return freeColServer.getInGameController()
            .getHighScores(serverPlayer, this.key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        // Suppress toXML for now
        throw new XMLStreamException(getType() + ".toXML NYI");
    }

    /**
     * Convert this HighScoreMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            KEY_TAG, this.key).add(this.scores).toXMLElement();
    }
}
