/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.util.Collections;
import java.util.List;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message sent when doing a foreign affairs report.
 */
public class ForeignAffairsMessage extends Message {

    /**
     * Cover European nations or native nations?
     */
    private boolean european;

    /**
     * A list of nation summaries.
     */
    private List<NationSummary> summaries;

    /**
     * Create a new <code>ForeignAffairsMessage</code>.
     * Used by the client to request the foreign affairs data.
     *
     * @param european True if the report should cover European players,
     *     otherwise it covers native players.
     */
    public ForeignAffairsMessage(boolean european) {
        this.european = european;
        summaries = Collections.emptyList();
    }

    /**
     * Create a new <code>ForeignAffairsMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public ForeignAffairsMessage(Game game, Element element) {
        this.european = element.hasAttribute("european");
        this.summaries = new ArrayList<NationSummary>();
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            summaries.add(new NationSummary((Element) nodes.item(i)));
        }
    }

    /**
     * Gets the nation summaries attached to this message.
     * Client-side helper.
     *
     * @return The nation summaries.
     */
    public List<NationSummary> getNationSummaries() {
        return summaries;
    }

    /**
     * Handle a "foreignAffairs"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the nation summaries,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);

        // Proceed to get the foreign affairs.
        summaries = server.getInGameController()
            .getForeignAffairs(serverPlayer, european);
        return toXMLElement();
    }

    /**
     * Convert this ForeignAffairsMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        Document doc = result.getOwnerDocument();
        if (european) result.setAttribute("european", "true");
        for (NationSummary summary : summaries) {
            result.appendChild(summary.toXMLElement(null, doc));
        }
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "foreignAffairs".
     */
    public static String getXMLElementTagName() {
        return "foreignAffairs";
    }
}
