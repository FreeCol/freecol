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
import org.w3c.dom.NodeList;


/**
 * The message sent when an highScore occurs.
 */
public class HighScoreMessage extends DOMMessage {

    public static final String TAG = "highScore";

    /** The high scores. */
    private final List<HighScore> scores = new ArrayList<>();
    

    /**
     * Create a new <code>HighScoreMessage</code> in request form (no
     * scores attached).
     */
    public HighScoreMessage() {
        super(getTagName());
    }

    /**
     * Create a new <code>HighScoreMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public HighScoreMessage(Game game, Element element) {
        super(getTagName());

        scores.clear();
        NodeList childElements = element.getChildNodes();
        for (int i = 0; i < childElements.getLength(); i++) {
            scores.add(new HighScore((Element)childElements.item(i)));
        }
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
        DOMMessage result = new DOMMessage(getTagName());
        for (HighScore hs : this.scores) result.add(hs);
        return result.toXMLElement();
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
