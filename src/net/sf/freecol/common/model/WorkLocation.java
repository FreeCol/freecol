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

package net.sf.freecol.common.model;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

/**
 * The <code>WorkLocation</code> is a place in a {@link Colony} where
 * <code>Units</code> can work. The unit capacity of a WorkLocation is
 * likely to be limited. ColonyTiles can only hold a single worker,
 * and Buildings can hold no more than three workers, for example.
 * WorkLocations do not store any Goods. They take any Goods they
 * consume from the Colony, and put all Goods they produce there,
 * too. Although the WorkLocation implements {@link Ownable}, its
 * owner can not be changed directly, as it is always owned by the
 * owner of the Colony.
 */
public abstract class WorkLocation extends UnitLocation implements Ownable {

    /**
     * Describe colony here.
     */
    private Colony colony;


    /**
     * Constructor for ServerWorkLocation.
     */
    protected WorkLocation() {
        // empty constructor
    }

    /**
     * Constructor for ServerWorkLocation.
     *
     * @param game The <code>Game</code> this object belongs to.
     */
    protected WorkLocation(Game game) {
        super(game);
    }

    /**
     * Initiates a new <code>WorkLocation</code> from an XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public WorkLocation(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
    }

    /**
     * Initiates a new <code>WorkLocation</code> from an XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param e An XML-element that will be used to initialize this object.
     */
    public WorkLocation(Game game, Element e) {
        super(game, e);

        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>WorkLocation</code> with the given ID. The object
     * should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public WorkLocation(Game game, String id) {
        super(game, id);
    }


    /**
     * Gets the production of the given type of goods.
     *
     * @param goodsType The type of goods to get the production of.
     * @return The production of the given type of goods.
     */
    public abstract int getProductionOf(GoodsType goodsType);

    /**
     * Gets the production of the given type of goods produced by a unit.
     *
     * @param unit The unit to do the work.
     * @param goodsType The type of goods to get the production of.
     * @return The production of the given type of goods.
     */
    public abstract int getProductionOf(Unit unit, GoodsType goodsType);

    /**
     * Gets the potential production of a given goods type from using
     * a unit of a given type in this work location.
     *
     * @param unitType The <code>UnitType</code> to produce the goods.
     * @param goodsType The <code>GoodsType</code> to produce.
     * @return The amount of goods potentially produced.
     */
    public abstract int getPotentialProduction(UnitType unitType,
                                               GoodsType goodsType);

    /**
     * Returns the <code>Colony</code> this <code>WorkLocation</code> is
     * located in.
     *
     * This method always returns a colony != null (in contrast to
     * Location.getColony(), which might return null).
     *
     * @return The <code>Colony</code> this <code>WorkLocation</code> is
     *         located in.
     *
     * @see Location#getColony
     */
    public final Colony getColony() {
        return colony;
    }

    /**
     * Set the <code>Colony</code> value.
     *
     * @param newColony The new Colony value.
     */
    public final void setColony(final Colony newColony) {
        this.colony = newColony;
    }

    /**
     * Gets the <code>Tile</code> where this work location is
     * located.
     *
     * @return The <code>Tile</code>.
     */
    public Tile getTile() {
        return colony.getTile();
    }

    /**
     * Returns the settlement containing this building.
     *
     * @return This colony.
     */
    public Settlement getSettlement() {
        return colony;
    }

    /**
     * Gets the owner of this <code>Ownable</code>.
     *
     * @return The <code>Player</code> controlling this
     *         {@link Ownable}.
     */
    public Player getOwner() {
        return colony.getOwner();
    }

    /**
     * Sets the owner of this <code>Ownable</code>. Do not call this
     * method, ever. Since the owner of this WorkLocation is the owner
     * of the Colony, you must set the owner of the Colony instead.
     *
     * @param p The <code>Player</code> that should take ownership
     *      of this {@link Ownable}.
     * @exception UnsupportedOperationException is always thrown by
     *      this method.
     */
    public void setOwner(Player p) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns <code>true</code> if this work location has the Ability to
     * teach skills.
     *
     * @see Ability#CAN_TEACH
     */
    public boolean canTeach() {
        return hasAbility(Ability.CAN_TEACH);
    }

    /**
     * Adds the specified locatable to this building.
     *
     * @param locatable The <code>Locatable</code> to add.
     */
    public void add(final Locatable locatable) {
        if (!(locatable instanceof Unit)) {
            throw new IllegalStateException("Not a unit: " + locatable);
        }

        Unit unit = (Unit) locatable;
        super.add(unit);

        if (canTeach()) {
            Unit student = unit.getStudent();
            if (student == null
                && (student = getColony().findStudent(unit)) != null) {
                unit.setStudent(student);
                student.setTeacher(unit);
            }
            unit.setWorkType(null);
        } else {
            Unit teacher = unit.getTeacher();
            if (teacher == null
                && (teacher = getColony().findTeacher(unit)) != null) {
                unit.setTeacher(teacher);
                teacher.setStudent(unit);
            }
        }
    }

    /**
     * Removes the specified locatable from this building.
     *
     * @param locatable The <code>Locatable</code> to remove.
     */
    public void remove(final Locatable locatable) {
        if (!(locatable instanceof Unit)) {
            throw new IllegalStateException("Not a unit: " + locatable);
        }

        Unit unit = (Unit) locatable;
        super.remove(unit);

        if (canTeach()) {
            Unit student = unit.getStudent();
            if (student != null) {
                student.setTeacher(null);
                unit.setStudent(null);
            }
        } else {
            Unit teacher = unit.getTeacher();
            if (teacher != null) {
                teacher.setStudent(null);
                unit.setTeacher(null);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        colony = getFreeColGameObject(in, "colony", Colony.class);
    }

    /**
     * {@inheritDoc}
     */
    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("colony", colony.getId());
    }

}
