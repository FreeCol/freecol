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

import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;

import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * The CompoundMission provides a wrapper for several more basic
 * Missions that will be carried out in order.
 */
public class CompoundMission extends AbstractMission {

    public static final String TAG = "compoundMission";
    
    /**
     * The individual missions this CompoundMission wraps.
     */
    private List<Mission> missions;

    /**
     * The index of the current mission.
     */
    private int index;


    /**
     * Creates a new {@code CompoundMission} instance.
     *
     * @param game a {@code Game} value
     */
    public CompoundMission(Game game) {
        super(game);
    }

    /**
     * Creates a new {@code CompoundMission} instance.
     *
     * @param game a {@code Game} value
     * @param xr a {@code FreeColXMLReader} value
     */
    public CompoundMission(Game game, FreeColXMLReader xr) {
        super(game, xr);
    }

    /**
     * Creates a new {@code CompoundMission} instance.
     *
     * @param game a {@code Game} value
     * @param id The object identifier.
     */
    public CompoundMission(Game game, String id) {
        super(game, id);
    }


    /**
     * Get the {@code Missions} value.
     *
     * @return A list of {@code Mission}s.
     */
    public final List<Mission> getMissions() {
        return missions;
    }

    /**
     * Set the {@code Missions} value.
     *
     * @param newMissions The new Missions value.
     */
    public final void setMissions(final List<Mission> newMissions) {
        this.missions = newMissions;
    }

    /**
     * Get the {@code Index} value.
     *
     * @return an {@code int} value
     */
    public final int getIndex() {
        return index;
    }

    /**
     * Set the {@code Index} value.
     *
     * @param newIndex The new Index value.
     */
    public final void setIndex(final int newIndex) {
        this.index = newIndex;
    }

    /**
     * Returns true if the mission is valid.
     *
     * @return a {@code boolean} value
     */
    @Override
    public boolean isValid() {
        return (super.isValid() && !missions.isEmpty())
            ? all(missions, Mission::isValid)
            : false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MissionState doMission() {
        while (true) {
            MissionState state = missions.get(index).doMission();
            if (state == MissionState.COMPLETED) {
                index++;
                if (index == missions.size()) {
                    setRepeatCount(getRepeatCount() - 1);
                    if (getRepeatCount() > 0) {
                        index = 0;
                    } else {
                        return MissionState.COMPLETED;
                    }
                }
                if (getUnit().getMovesLeft() > 0) {
                    continue;
                }
            }
            return state;
        }
    }

    // Serialization.

    private final static String INDEX_TAG = "index";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(INDEX_TAG, index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (Mission mission : missions) {
            mission.toXML(xw);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        index = xr.getAttribute(INDEX_TAG, 0);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        missions.clear();

        while (xr.moreTags()) {
            Mission mission = MissionManager.getMission(getGame(), xr);
            if (mission != null) missions.add(mission);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return TAG; }
}
