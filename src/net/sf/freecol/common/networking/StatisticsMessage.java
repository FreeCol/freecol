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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


/**
 * The message sent to gather statistics.
 */
public class StatisticsMessage extends DOMMessage {

    /** A map of statistics key,value pairs. */
    private final Map<String, String> stats = new HashMap<>();


    /**
     * Create a new <code>StatisticsMessage</code> with the given message
     * identifier and message.
     *
     * @param map An optional map of statistics key,value pairs.
     */
    public StatisticsMessage(Map<String, String> map) {
        super(getTagName());

        if (map != null) this.stats.putAll(map);
    }

    /**
     * Create a new <code>StatisticsMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public StatisticsMessage(Game game, Element element) {
        super(getTagName());

        final NamedNodeMap map = element.getAttributes();
        final int n = map.getLength();
        for (int i = 0; i < n; i++) {
            Node node = map.item(i);
            this.stats.put(node.getNodeName(), node.getNodeValue());
        }
    }


    // Public interface

    /**
     * Get the statistics map.
     *
     * @return A map of statistics key,value pairs.
     */
    public Map<String, String> getStatistics() {
        return this.stats;
    }
    
    
    /**
     * Handle a "statistics"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the statistics.
     */
    public Element handle(FreeColServer server, Connection connection) {
        this.stats.clear();
        this.stats.putAll(server.getInGameController().getStatistics());
        return this.toXMLElement();
    }

    /**
     * Convert this StatisticsMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        for (Entry<String, String> e : stats.entrySet()) {
            this.setAttribute(e.getKey(), e.getValue());
        }
        return super.toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "statistics".
     */
    public static String getTagName() {
        return "statistics";
    }
}
