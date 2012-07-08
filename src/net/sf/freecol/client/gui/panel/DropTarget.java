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


package net.sf.freecol.client.gui.panel;

import java.awt.Component;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Unit;


public interface DropTarget {

    /**
     * Returns <code>true</code> if the given Unit could be dropped on
     * this target.
     *
     * @param unit an <code>Unit</code> value
     * @return a <code>boolean</code> value
     */
    public boolean accepts(Unit unit);

    /**
     * Returns <code>true</code> if the given Goods could be dropped on
     * this target.
     *
     * @param goods a <code>Goods</code> value
     * @return a <code>boolean</code> value
     */
    public boolean accepts(Goods goods);

    /**
     * Adds the given component to this target.
     *
     * @param comp a <code>Component</code> value
     * @param editState a <code>boolean</code> value
     * @return a <code>Component</code> value
     */
    public Component add(Component comp, boolean editState);

}
