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

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

import net.sf.freecol.common.model.Nation;

/**
 * A table cell editor that can be used to select a nation.
 */
public final class NationCellEditor extends DefaultCellEditor {

    /**
     * A standard constructor.
     *
     * @param nations array of <code>Nation</code>
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public NationCellEditor(Nation[] nations) {
        super(new JComboBox(nations));
    }
    
    @Override
    public Object getCellEditorValue() {
        return ((JComboBox) getComponent()).getSelectedItem();
    }
}
