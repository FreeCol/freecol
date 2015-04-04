/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
 *  MERCHANTLIMIT or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model.mission;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Unit;

import org.w3c.dom.Element;


/**
 * The GoToMission causes a Unit to move towards its destination.
 */
public class GoToMission extends AbstractMission {

    /**
     * The number of turns this mission has been blocked.
     */
    private int blockedCount;

    /**
     * The destination of this Mission.
     */
    private Location destination;


    /**
     * Creates a new <code>GoToMission</code> instance.
     *
     * @param game a <code>Game</code> value
     */
    public GoToMission(Game game) {
        super(game);
    }

    /**
     * Creates a new <code>GoToMission</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param xr a <code>FreeColXMLReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public GoToMission(Game game,
                       FreeColXMLReader xr) throws XMLStreamException {
        super(game, xr);
    }

    /**
     * Creates a new <code>GoToMission</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param e an <code>Element</code> value
     */
    public GoToMission(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    /**
     * Creates a new <code>GoToMission</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param id The object identifier.
     */
    public GoToMission(Game game, String id) {
        super(game, id);
    }

    /**
     * Get the <code>Destination</code> value.
     *
     * @return an <code>Location</code> value
     */
    public final Location getDestination() {
        return destination;
    }

    /**
     * Set the <code>Destination</code> value.
     *
     * @param newDestination The new Destination value.
     */
    public final void setDestination(final Location newDestination) {
        this.destination = newDestination;
    }

    /**
     * Get the <code>BlockedCount</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getBlockedCount() {
        return blockedCount;
    }

    /**
     * Set the <code>BlockedCount</code> value.
     *
     * @param newBlockedCount The new BlockedCount value.
     */
    public final void setBlockedCount(final int newBlockedCount) {
        this.blockedCount = newBlockedCount;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MissionState doMission() {
        // FIXME: do we need access to the InGameController?
        return MissionState.OK;
    }


    /**
     * Returns true if the mission is still valid.
     *
     * @return a <code>boolean</code> value
     */
    @Override
    public boolean isValid() {
        return super.isValid()
            && destination != null
            // FIXME: check for disposed destinations
            && destination.canAdd(getUnit());
    }

    /**
     * Returns true if the given Unit has movement points. At the
     * moment, this is true for all units.
     *
     * @param unit an <code>Unit</code> value
     * @return false
     */
    public static boolean isValidFor(Unit unit) {
        return unit.getInitialMovesLeft() > 0;
    }


    // Serialization

    private static final String BLOCKED_COUNT_TAG = "blockedCount";
    private static final String DESTINATION_TAG = "destination";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(DESTINATION_TAG, destination);

        xw.writeAttribute(BLOCKED_COUNT_TAG, blockedCount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        destination = xr.getLocationAttribute(getGame(), DESTINATION_TAG,
                                              false);

        blockedCount = xr.getAttribute(BLOCKED_COUNT_TAG, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "goToMission"
     */
    public static String getXMLElementTagName() {
        return "goToMission";
    }



}
