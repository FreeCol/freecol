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

import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Unit;


/**
 * The Mission interface describes some kind of order that can be
 * given to a {@link Unit}, such as the order to move to a certain
 * Tile, attack a certain Unit, or build a TileImprovement, for
 * example. Missions can be atomic, or combine a number of simpler
 * Missions.
 */
public interface Mission {

    public static enum MissionState {
        /**
         * Mission is in progress.
         */
        OK,
        /**
         * Mission has been completed.
         */
        COMPLETED,
        /**
         * Mission is temporarily blocked, e.g. by another Unit.
         */
        BLOCKED,
        /**
         * Mission has been aborted, e.g. because a target or
         * destination has been destroyed.
         */
        ABORTED
    };


    /**
     * Attempts to carry out the mission and returns an appropriate
     * MissionState.
     *
     * @return a <code>MissionState</code> value
     */
    public MissionState doMission();


    /**
     * Returns true if the mission is still valid. This might not be
     * the case if its target or destination have been destroyed, or
     * if the Unit this mission was assigned to was destroyed or
     * changed owner, for example.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isValid();


    /**
     * Return the Unit this mission was assigned to.
     *
     * @return an <code>Unit</code> value
     */
    public Unit getUnit();

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param xw The <code>FreeColXMLWriter</code> to write to.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException;
}
