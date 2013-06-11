/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.util.List;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.model.EuropeanNationType;


/**
 * A table cell editor that can be used to select a nation.
 */
public final class AdvantageCellEditor extends DefaultCellEditor {

    /**
     * A standard constructor.
     *
     * @param nationTypes List of <code>EuropeanNationType></code>
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public AdvantageCellEditor(List<EuropeanNationType> nationTypes) {
        super(new JComboBox(new Vector<EuropeanNationType>(nationTypes)));
        ((JComboBox) getComponent()).setRenderer(new AdvantageRenderer());
    }
    
    @Override
    public Object getCellEditorValue() {
        return ((JComboBox) getComponent()).getSelectedItem();
    }

    private class AdvantageRenderer extends FreeColComboBoxRenderer {
        @Override
        public void setLabelValues(JLabel label, Object value) {
            label.setText((value == null) ? "" : Messages.message(value.toString() + ".name"));
        }
    }


}
