/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Unit;


/**
 * The AbstractMission provides basic methods for building Missions.
 */
public abstract class AbstractMission extends FreeColGameObject implements Mission {

    /**
     * The Unit this mission was assigned to. Must not be null.
     */
    private Unit unit;

    /**
     * The number of times this mission should be repeated. Defaults
     * to 1.
     */
    private int repeatCount = 1;

    /**
     * The number of turns this mission will take to carry out. In
     * most cases, this will be zero, since most missions do not take
     * a fixed number of turns to carry out. Building TileImprovements
     * and learning in school would be exceptions, however.
     */
    private int turnCount;


    /**
     * Creates a new {@code AbstractMission} instance.
     *
     * @param game a {@code Game} value
     * @param id The object identifier.
     */
    public AbstractMission(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new {@code AbstractMission} instance.
     *
     * @param game a {@code Game} value
     * @param xr a {@code FreeColXMLReader} value
     */
    protected AbstractMission(Game game, FreeColXMLReader xr) {
        super(game, null);
    }


    /**
     * Returns the Unit this mission was assigned to.
     *
     * @return an {@code Unit} value
     */
    @Override
    public final Unit getUnit() {
        return unit;
    }

    /**
     * Creates a new {@code AbstractMission} instance.
     *
     * @param game a {@code Game} value
     */
    public AbstractMission(Game game) {
        super(game);
    }

    /**
     * Set the {@code Unit} value.
     *
     * @param newUnit The new Unit value.
     */
    public final void setUnit(final Unit newUnit) {
        this.unit = newUnit;
    }

    /**
     * Get the {@code RepeatCount} value.
     *
     * @return an {@code int} value
     */
    public final int getRepeatCount() {
        return repeatCount;
    }

    /**
     * Set the {@code RepeatCount} value.
     *
     * @param newRepeatCount The new RepeatCount value.
     */
    public final void setRepeatCount(final int newRepeatCount) {
        this.repeatCount = newRepeatCount;
    }

    /**
     * Get the {@code TurnCount} value.
     *
     * @return an {@code int} value
     */
    public final int getTurnCount() {
        return turnCount;
    }

    /**
     * Set the {@code TurnCount} value.
     *
     * @param newTurnCount The new TurnCount value.
     */
    public final void setTurnCount(final int newTurnCount) {
        this.turnCount = newTurnCount;
    }

    /**
     * Returns true if the Unit this mission was assigned to is
     * neither null nor has been disposed, and the repeat count of the
     * mission is greater than zero.
     *
     * @return a {@code boolean} value
     */
    @Override
    public boolean isValid() {
        return repeatCount > 0
            && unit != null && !unit.isDisposed();
    }

    /**
     * Returns true if this is a valid Mission for the given
     * Unit. This method always returns false and needs to be
     * overridden.
     *
     * @param unit an {@code Unit} value
     * @return false
     */
    public static boolean isValidFor(@SuppressWarnings("unusued") Unit unit) {
        return false;
    }


    // Serialization

    private static final String REPEAT_COUNT_TAG = "repeatCount";
    private static final String TURN_COUNT_TAG = "turnCount";
    private static final String UNIT_TAG = "unit";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(UNIT_TAG, unit);

        xw.writeAttribute(TURN_COUNT_TAG, turnCount);

        xw.writeAttribute(REPEAT_COUNT_TAG, repeatCount);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        unit = xr.makeFreeColObject(getGame(), UNIT_TAG, Unit.class, true);

        turnCount = xr.getAttribute(TURN_COUNT_TAG, 0);

        repeatCount = xr.getAttribute(REPEAT_COUNT_TAG, 1);
    }
}
