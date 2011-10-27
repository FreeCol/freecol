/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;


/**
 * The message sent when the client requests abandoning of a colony.
 */
public class ChooseFoundingFatherMessage extends DOMMessage {

    private List<FoundingFather> fathers;
    private FoundingFather foundingFather;


    /**
     * Create a new <code>ChooseFoundingFatherMessage</code> with the specified
     * fathers.
     *
     * @param fathers The <code>FoundingFather</code>s to choose from.
     */
    public ChooseFoundingFatherMessage(List<FoundingFather> fathers) {
        this.fathers = fathers;
        this.foundingFather = null;
    }

    /**
     * Create a new <code>ChooseFoundingFatherMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public ChooseFoundingFatherMessage(Game game, Element element) {
        Specification spec = game.getSpecification();
        String id;
        FoundingFather f;
        fathers = new ArrayList<FoundingFather>();
        for (FoundingFatherType type : FoundingFatherType.values()) {
            id = element.getAttribute(type.toString());
            if (id == null || "".equals(id)) continue;
            f = spec.getFoundingFather(id);
            if (f == null) continue;
            fathers.add(f);
        }
        foundingFather = ((id = element.getAttribute("foundingFather")) == null
            || "".equals(id)
            || (f = spec.getFoundingFather(id)) == null) ? null : f;
    }

    /**
     * Client-side convenience to get the list of fathers on offer.
     *
     * @return The list of fathers to choose from.
     */
    public List<FoundingFather> getFathers() {
        return fathers;
    }

    /**
     * Gets the chosen father.
     *
     * @return The chosen father.
     */
    public FoundingFather getResult() {
        return foundingFather;
    }

    /**
     * Sets the chosen father.
     *
     * @param foundingFather The <code>FoundingFather</code> to choose.
     */
    public void setResult(FoundingFather foundingFather) {
        this.foundingFather = foundingFather;
    }

    /**
     * Handle a "chooseFoundingFather"-message.
     * The server does not need to handle this message type.
     *
     * @param server The <code>FreeColServer</code> handling the request.
     * @param player The <code>Player</code> abandoning the colony.
     * @param connection The <code>Connection</code> the message is from.
     *
     * @return Null.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        return null;
    }

    /**
     * Convert this ChooseFoundingFatherMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        for (FoundingFather f : fathers) {
            result.setAttribute(f.getType().toString(), f.getId());
        }
        if (foundingFather != null) {
            result.setAttribute("foundingFather", foundingFather.getId());
        }
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "chooseFoundingFather".
     */
    public static String getXMLElementTagName() {
        return "chooseFoundingFather";
    }
}
