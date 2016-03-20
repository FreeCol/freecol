/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when an highScore occurs.
 */
public class HighScoreMessage extends DOMMessage {

    public static final String TAG = "highScore";
    private static final String KEY_TAG = "key";

    /** An optional key for the final display. */
    private final String key;

    /** The high scores. */
    private final List<HighScore> scores = new ArrayList<>();
    

    /**
     * Create a new <code>HighScoreMessage</code> in request form (no
     * scores attached).
     *
     * @param key A message key for the final display.
     */
    public HighScoreMessage(String key) {
        super(getTagName());

        this.key = key;
    }

    /**
     * Create a new <code>HighScoreMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public HighScoreMessage(Game game, Element element) {
        super(getTagName());

        this.key = getStringAttribute(element, KEY_TAG);
        this.scores.clear();
        this.scores.addAll(getChildren(game, element, HighScore.class));
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
     * @return The list of <code>HighScore</code>s.
     */
    public List<HighScore> getScores() {
        return this.scores;
    }

    
    /**
     * Handle a "highScore"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the high scores.
     */
    public Element handle(FreeColServer server, Connection connection) {
        this.scores.addAll(server.getInGameController().getHighScores());
        return this.toXMLElement();
    }

    /**
     * Convert this HighScoreMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            KEY_TAG, this.key).add(this.scores).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "highScore".
     */
    public static String getTagName() {
        return TAG;
    }
}
