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
 *  MERCHANTLIMIT or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model.mission;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;

import org.w3c.dom.Element;

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
     * Returns the Unit this mission was assigned to.
     *
     * @return an <code>Unit</code> value
     */
    public final Unit getUnit() {
        return unit;
    }

    /**
     * Creates a new <code>AbstractMission</code> instance.
     *
     * @param game a <code>Game</code> value
     */
    public AbstractMission(Game game) {
        super(game);
    }

    /**
     * Creates a new <code>AbstractMission</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public AbstractMission(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, null);
    }

    /**
     * Creates a new <code>AbstractMission</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param e an <code>Element</code> value
     */
    public AbstractMission(Game game, Element e) {
        super(game, null);

        readFromXMLElement(e);
    }

    /**
     * Creates a new <code>AbstractMission</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param id The object identifier.
     */
    public AbstractMission(Game game, String id) {
        super(game, id);
    }

    /**
     * Set the <code>Unit</code> value.
     *
     * @param newUnit The new Unit value.
     */
    public final void setUnit(final Unit newUnit) {
        this.unit = newUnit;
    }

    /**
     * Get the <code>RepeatCount</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getRepeatCount() {
        return repeatCount;
    }

    /**
     * Set the <code>RepeatCount</code> value.
     *
     * @param newRepeatCount The new RepeatCount value.
     */
    public final void setRepeatCount(final int newRepeatCount) {
        this.repeatCount = newRepeatCount;
    }

    /**
     * Get the <code>TurnCount</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getTurnCount() {
        return turnCount;
    }

    /**
     * Set the <code>TurnCount</code> value.
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
     * @return a <code>boolean</code> value
     */
    public boolean isValid() {
        return unit != null
            && !unit.isDisposed()
            && repeatCount > 0;
    }

    /**
     * Returns true if this is a valid Mission for the given
     * Unit. This method always returns false and needs to be
     * overridden.
     *
     * @param unit an <code>Unit</code> value
     * @return false
     */
    public static boolean isValidFor(Unit unit) {
        return false;
    }


    // Serialization


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("unit", unit.getId());
        out.writeAttribute("turnCount", Integer.toString(turnCount));
        out.writeAttribute("repeatCount", Integer.toString(repeatCount));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        unit = makeFreeColGameObject(in, "unit", Unit.class, true);

        turnCount = getAttribute(in, "turnCount", 0);

        repeatCount = getAttribute(in, "repeatCount", 1);
    }
}
