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
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class EmptyIterator<T> implements Iterator<T> {




    /**
     * Returns an instance of the EmptyIterator for the given type.
     * 
     * @param <T> the type of the empty iterator.
     * @return an EmptyIterator of the given type.
     */
    public static <T> EmptyIterator<T> getInstance() {
        return new EmptyIterator<T>();
    }

    // ----------------------------------------------------------- constructors

    private EmptyIterator() {
    }

    // ---------------------------------------------------- interface: Iterator

    public boolean hasNext() {
        return false;
    }

    public T next() {
        throw new NoSuchElementException("Programming error: next() should never be called on the EmptyIterator");
    }

    public void remove() {
        throw new IllegalStateException();
    }

}
