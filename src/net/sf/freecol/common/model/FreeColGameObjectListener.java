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
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.common.model;


/**
 * Interface for retriving information about
 * a the creation/deletion of {@link FreeColGameObject}s.
 */
public interface FreeColGameObjectListener {

    /**
     * Notify a listener (if any) of a new object.
     *
     * @param id The object identifier.
     * @param fcgo The new <code>FreeColGameObject</code>.
     */
    public void setFreeColGameObject(String id, FreeColGameObject fcgo);

    /**
     * Notify a listener (if any) of that an object has gone.
     *
     * @param id The object identifier.
     */
    public void removeFreeColGameObject(String id);

    /**
     * Notify a listener (if any) of that an object has changed owner.
     *
     * @param source The <code>FreeColGameObject</code> that changed owner.
     * @param oldOwner The old owning <code>Player</code>.
     * @param newOwner The new owning <code>Player</code>.
     */
    public void ownerChanged(FreeColGameObject source,
                             Player oldOwner, Player newOwner);
}
