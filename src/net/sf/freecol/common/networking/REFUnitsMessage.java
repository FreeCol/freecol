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

import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message used to query the REF.
 */
public class REFUnitsMessage extends DOMMessage {

    public static final String TAG = "REFUnits";

    /** The units defining the REF. */
    private final List<AbstractUnit> refUnits = new ArrayList<>();


    /**
     * Create a new <code>REFUnitsMessage</code> with the given units.
     *
     * @param units An optional list of <code>AbstractUnit</code>s
     *     defining the REF.
     */
    public REFUnitsMessage(List<AbstractUnit> units) {
        super(getTagName());

        this.refUnits.clear();
        if (units != null) this.refUnits.addAll(units);
    }

    /**
     * Create a new <code>REFUnitsMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public REFUnitsMessage(Game game, Element element) {
        super(getTagName());

        this.refUnits.clear();
        NodeList childElements = element.getChildNodes();
        for (int index = 0; index < childElements.getLength(); index++) {
            AbstractUnit unit = new AbstractUnit();
            readFromXMLElement(unit, (Element)childElements.item(index));
            this.refUnits.add(unit);
        }
    }


    // Public interface

    /**
     * Get the REF units.
     *
     * @return A list of <code>AbstractUnit</code> defining the REF.
     */
    public List<AbstractUnit> getREFUnits() {
        return this.refUnits;
    }
    
    
    /**
     * Handle a "REFUnits"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return A REFUnits message containing the REF units.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        this.refUnits.clear();
        this.refUnits.addAll(server.getInGameController()
            .getREFUnits(server.getPlayer(connection)));
        return this.toXMLElement();
    }

    /**
     * Convert this REFUnitsMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        for (AbstractUnit au : this.refUnits) this.add(au);
        return super.toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "REFUnits".
     */
    public static String getTagName() {
        return TAG;
    }
}
