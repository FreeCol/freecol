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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.xml.stream.XMLStreamException;

import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * An {@code Iterator} of {@link Unit}s that can be made active.
 */
public class UnitIterator implements Iterator<Unit> {

    /** The player that owns the units. */
    private final Player owner;

    /** The admission predicate. */
    private final Predicate<Unit> predicate;

    /** The current cache of units. */
    private final List<Unit> units = new ArrayList<>();


    /**
     * Creates a new {@code UnitIterator}.
     *
     * @param owner The {@code Player} that needs an iterator
     *     of it's units.
     * @param predicate A {@code Predicate} for deciding
     *     whether a {@code Unit} should be included in the
     *     {@code Iterator} or not.
     */
    public UnitIterator(Player owner, Predicate<Unit> predicate) {
        this.owner = owner;
        this.predicate = predicate;
        update();
    }


    /**
     * Update the internal units list with units that satisfy the
     * predicate.
     */
    private final void update() {
        this.units.clear();
        this.units.addAll(transform(owner.getUnits(), u -> predicate.test(u),
                          Function.<Unit>identity(), Unit.locComparator));
    }

    /**
     * Set the next valid unit.
     *
     * @param unit The {@code Unit} to put at the front of the list.
     * @return True if the operation succeeds.
     */
    public boolean setNext(Unit unit) {
        if (this.predicate.test(unit)) { // Of course, it has to be valid...
            final Unit sentinel = first(this.units);
            while (!this.units.isEmpty()) {
                if (this.units.get(0) == unit) return true;
                this.units.remove(0);
            }
            update();
            while (!this.units.isEmpty() && this.units.get(0) != sentinel) {
                if (this.units.get(0) == unit) return true;
                this.units.remove(0);
            }
        }
        return false;
    }

    /**
     * Removes a specific unit from this unit iterator.
     *
     * @param u The {@code Unit} to remove.
     * @return True if the unit was removed.
     */
    public boolean remove(Unit u) {
        return this.units.remove(u);
    }

    /**
     * Reset the iterator.
     */
    public void reset() {
        update();
    }


    // Implement Iterator

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        // Try to find a unit that still satisfies the predicate.
        while (!this.units.isEmpty()) {
            if (predicate.test(this.units.get(0))) {
                return true; // Still valid
            }
            this.units.remove(0);
        }
        // Nothing left, so refill the units list.  If it is still
        // empty then there is definitely nothing left.
        update();
        return !this.units.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Unit next() {
        return (hasNext()) ? this.units.remove(0) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        next(); // Ignore value
    }
}
