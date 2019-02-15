/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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


/**
 * The GoToMission causes a Unit to move towards its destination.
 */
public class GoToMission extends AbstractMission {

    public static final String TAG = "goToMission";

    /**
     * The number of turns this mission has been blocked.
     */
    private int blockedCount;

    /**
     * The destination of this Mission.
     */
    private Location destination;


    /**
     * Creates a new {@code GoToMission} instance.
     *
     * @param game a {@code Game} value
     */
    public GoToMission(Game game) {
        super(game);
    }

    /**
     * Creates a new {@code GoToMission} instance.
     *
     * @param game a {@code Game} value
     * @param xr a {@code FreeColXMLReader} value
     */
    public GoToMission(Game game, FreeColXMLReader xr) {
        super(game, xr);
    }

    /**
     * Creates a new {@code GoToMission} instance.
     *
     * @param game a {@code Game} value
     * @param id The object identifier.
     */
    public GoToMission(Game game, String id) {
        super(game, id);
    }


    /**
     * Get the {@code Destination} value.
     *
     * @return an {@code Location} value
     */
    public final Location getDestination() {
        return destination;
    }

    /**
     * Set the {@code Destination} value.
     *
     * @param newDestination The new Destination value.
     */
    public final void setDestination(final Location newDestination) {
        this.destination = newDestination;
    }

    /**
     * Get the {@code BlockedCount} value.
     *
     * @return an {@code int} value
     */
    public final int getBlockedCount() {
        return blockedCount;
    }

    /**
     * Set the {@code BlockedCount} value.
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
     * @return a {@code boolean} value
     */
    @Override
    public boolean isValid() {
        // FIXME: check for disposed destinations
        return destination != null && destination.canAdd(getUnit())
            && super.isValid();
    }

    /**
     * Returns true if the given Unit has movement points. At the
     * moment, this is true for all units.
     *
     * @param unit an {@code Unit} value
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
    public String getXMLTagName() { return TAG; }
}
