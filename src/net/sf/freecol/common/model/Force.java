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
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.FreeColSpecObject;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.UnitType;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * A group of units with a common origin and purpose.
 */
public class Force extends FreeColSpecObject {

    public static final String TAG = "force";

    /** The number of land units in the REF. */
    private final List<AbstractUnit> landUnits = new ArrayList<>();

    /** The number of naval units in the REF. */
    private final List<AbstractUnit> navalUnits = new ArrayList<>();

    // Internal variables that do not need serialization.
    /** The space required to transport all land units. */
    private int spaceRequired;

    /** The current naval transport capacity. */
    private int capacity;


    /**
     * Basic constructor.
     *
     * @param specification The {@code Specification} for this object.
     */
    public Force(Specification specification) {
        super(specification);
    }

    /**
     * Create a new Force.
     *
     * @param spec The {@code Specification} for this object.
     * @param units A list of {@code AbstractUnit}s defining the force.
     * @param ability An optional ability name required of the units
     *     in the force.
     */
    public Force(Specification spec, List<AbstractUnit> units, String ability) {
        this(spec);

        this.capacity = this.spaceRequired = 0;
        for (AbstractUnit au : units) {
            if (ability == null || au.getType(spec).hasAbility(ability)) {
                add(au);
            } else {
                logger.warning("Found unit lacking required ability \""
                    + ability + "\": " + au);
            }
        }
    }


    /**
     * Get the cargo space required for the land units of this force.
     *
     * @return The required cargo space.
     */
    public final int getSpaceRequired() {
        return this.spaceRequired;
    }

    /**
     * Get the cargo space provided by the naval units of this force.
     *
     * @return The provided cargo space.
     */
    public final int getCapacity() {
        return this.capacity;
    }

    /**
     * Gets all units.
     *
     * @return A copy of the list of all units.
     */
    public final List<AbstractUnit> getUnitList() {
        List<AbstractUnit> result = new ArrayList<>();
        result.addAll(this.landUnits);
        result.addAll(this.navalUnits);
        return result;
    }

    /**
     * Clear the land unit list.
     */
    public final void clearLandUnits() {
        this.landUnits.clear();
        this.spaceRequired = 0;
    }

    /**
     * Clear the naval unit list.
     */
    public final void clearNavalUnits() {
        this.navalUnits.clear();
        this.capacity = 0;
    }

    /**
     * Gets the land units.
     *
     * @return A list of the  land units.
     */
    public final List<AbstractUnit> getLandUnitsList() {
        return AbstractUnit.deepCopy(this.landUnits);
    }

    /**
     * Set the land unit list.
     *
     * @param landUnits Set the new land {@code AbstractUnit}s.
     */
    protected void setLandUnitList(List<AbstractUnit> landUnits) {
        clearLandUnits();
        this.landUnits.addAll(landUnits);
    }
    
    /**
     * Gets the naval units.
     *
     * @return A copy of the list of the naval units.
     */
    public final List<AbstractUnit> getNavalUnitsList() {
        return AbstractUnit.deepCopy(this.navalUnits);
    }

    /**
     * Set the naval unit list.
     *
     * @param navalUnits Set the new land {@code AbstractUnit}s.
     */
    protected void setNavalUnitList(List<AbstractUnit> navalUnits) {
        clearNavalUnits();
        this.navalUnits.addAll(navalUnits);
    }
    
    /**
     * Is this Force empty?
     *
     * @return True if there are no land or naval units.
     */
    public final boolean isEmpty() {
        return this.landUnits.isEmpty() && this.navalUnits.isEmpty();
    }

    /**
     * Add abstract units to this Force.
     *
     * @param au The addition to this {@code Force}.
     */
    public final void add(AbstractUnit au) {
        final Specification spec = getSpecification();
        final UnitType unitType = au.getType(spec);
        final int n = au.getNumber();
        final Predicate<AbstractUnit> matchPred = AbstractUnit.matcher(au);

        if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
            AbstractUnit refUnit = find(this.navalUnits, matchPred);
            if (refUnit != null) {
                refUnit.addToNumber(n);
                if (unitType.canCarryUnits()) {
                    this.capacity += unitType.getSpace() * n;
                }
            } else {
                this.navalUnits.add(au);
            }
        } else {
            AbstractUnit refUnit = find(this.landUnits, matchPred);
            if (refUnit != null) {
                refUnit.addToNumber(n);
                this.spaceRequired += unitType.getSpaceTaken() * n;
            } else {
                this.landUnits.add(au);
            }
        }
    }

    /**
     * Calculate the approximate offence power of this force.
     *
     * @param naval If true, consider only naval units, otherwise
     *     consider the land units.
     * @return The approximate offence power.
     */
    public double calculateStrength(boolean naval) {
        return AbstractUnit.calculateStrength(getSpecification(),
            (naval) ? this.navalUnits : this.landUnits);
    }

    /**
     * Defend against underprovisioned navies.
     *
     * @return True if the navy can carry the army.
     */
    public boolean prepareToBoard() {
        final Specification spec = getSpecification();
        AbstractUnit ship0 = find(this.navalUnits,
            au -> au.getType(spec).getSpace() > 0);
        if (ship0 != null) {
            int sp = ship0.getType(spec).getSpace(),
                more = (this.spaceRequired - this.capacity) / sp + 1;
            if (more > 0) {
                ship0.setNumber(ship0.getNumber() + more);
                this.capacity += sp * more;
            }
        }
        return this.spaceRequired <= this.capacity;
    }

    /**
     * Does another force match?
     *
     * @param other The other <code>Force</code> to test.
     * @return True if the other force contains the same units.
     */
    public boolean matchAll(Force other) {
        return AbstractUnit.matchUnits(this.landUnits, other.landUnits)
            && AbstractUnit.matchUnits(this.navalUnits, other.navalUnits);
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Force o = copyInCast(other, Force.class);
        if (o == null || !super.copyIn(o)) return false;
        this.setLandUnitList(o.getLandUnitsList());
        this.setNavalUnitList(o.getNavalUnitsList());
        this.spaceRequired = o.getSpaceRequired();
        this.capacity = o.getCapacity();
        return true;
    }


    // Serialization

    private static final String LAND_UNITS_TAG = "landUnits";
    private static final String NAVAL_UNITS_TAG = "navalUnits";


    /**
     * {@inheritDoc}
     */
    @Override
    public void toXML(FreeColXMLWriter xw, String tag) throws XMLStreamException {
        xw.writeStartElement(tag);

        xw.writeStartElement(NAVAL_UNITS_TAG);

        for (AbstractUnit unit : this.navalUnits) unit.toXML(xw);

        xw.writeEndElement();

        xw.writeStartElement(LAND_UNITS_TAG);

        for (AbstractUnit unit : this.landUnits) unit.toXML(xw);

        xw.writeEndElement();

        xw.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        clearLandUnits();
        clearNavalUnits();

        while (xr.moreTags()) {
            final String tag = xr.getLocalName();

            if (LAND_UNITS_TAG.equals(tag)) {
                while (xr.moreTags()) {
                    add(new AbstractUnit(xr));
                }
            } else if (NAVAL_UNITS_TAG.equals(tag)) {
                while (xr.moreTags()) {
                    add(new AbstractUnit(xr));
                }
            } else {
                logger.warning("Bogus Force tag: " + tag);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("<Force");
        for (AbstractUnit au : this.landUnits) sb.append(' ').append(au);
        for (AbstractUnit au : this.navalUnits) sb.append(' ').append(au);
        sb.append('>');
        return sb.toString();
    }
}
