/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message sent when doing a monarch action.
 */
public class MonarchActionMessage extends DOMMessage {

    // The monarch action.
    private MonarchAction action;

    // A template describing the action.
    private StringTemplate template;

    // The tax rate, if appropriate.
    private String tax;

    // Is the offer accepted?  Valid in replies from client.
    private String resultString;


    /**
     * Create a new <code>MonarchActionMessage</code> with the given action.
     *
     * @param action The <code>MonarchAction</code> to do.
     */
    public MonarchActionMessage(MonarchAction action, StringTemplate template) {
        this.action = action;
        this.template = template;
        this.tax = null;
        this.resultString = null;
    }

    /**
     * Create a new <code>MonarchActionMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public MonarchActionMessage(Game game, Element element) {
        this.action = Enum.valueOf(MonarchAction.class,
                                   element.getAttribute("action"));
        this.tax = element.getAttribute("tax");
        this.resultString = element.getAttribute("result");
        NodeList children = element.getChildNodes();
        if (children.getLength() == 1) {
            this.template = StringTemplate.label(" ");
            this.template.readFromXMLElement((Element) children.item(0));
        } else {
            this.template = null;
        }
    }

    /**
     * Gets the monarch action type of this message.
     *
     * @return The monarch action type.
     */
    public MonarchAction getAction() {
        return action;
    }

    /**
     * Gets the template of this message.
     *
     * @return The template.
     */
    public StringTemplate getTemplate() {
        return template;
    }

    /**
     * Gets the tax amount attached to this message.
     *
     * @return The tax amount, or negative if none present.
     */
    public int getTax() {
        try {
            return Integer.parseInt(tax);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Sets the tax amount attached to this message.
     *
     * @param tax The tax amount.
     */
    public void setTax(int tax) {
        this.tax = Integer.toString(tax);
    }

    /**
     * Gets the result.
     *
     * @return The result.
     */
    public boolean getResult() {
        return Boolean.valueOf(resultString);
    }

    /**
     * Sets the result.
     *
     * @param accept The new result.
     */
    public void setResult(boolean accept) {
        this.resultString = Boolean.toString(accept);
    }

    /**
     * Handles a "monarchAction"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return Null.  This should not be called.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        return null;
    }

    /**
     * Convert this MonarchMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "action", action.toString());
        if (tax != null) result.setAttribute("tax", tax);
        if (resultString != null) result.setAttribute("result", resultString);
        result.appendChild(template.toXMLElement(null,
                                                 result.getOwnerDocument()));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "monarchAction".
     */
    public static String getXMLElementTagName() {
        return "monarchAction";
    }
}
