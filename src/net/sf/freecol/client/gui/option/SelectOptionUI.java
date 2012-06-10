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

package net.sf.freecol.client.gui.option;


import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.option.SelectOption;


/**
 * This class provides visualization for an {@link
 * net.sf.freecol.common.option.SelectOption}. In order to enable
 * values to be both seen and changed.
 */
public final class SelectOptionUI extends OptionUI<SelectOption>  {

    private JComboBox box = new JComboBox();

    /**
    * Creates a new <code>SelectOptionUI</code> for the given <code>SelectOption</code>.
    *
    * @param option The <code>SelectOption</code> to make a user interface for
    * @param editable boolean whether user can modify the setting
    */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public SelectOptionUI(GUI gui, final SelectOption option, boolean editable) {
        super(gui, option, editable);

        String[] strings = option.getItemValues().values().toArray(new String[0]);
        if (option.localizeLabels()) {
            for (int index = 0; index < strings.length; index++) {
                strings[index] = Messages.message(strings[index]);
            }
        }

        box.setModel(new DefaultComboBoxModel(strings));
        box.setSelectedIndex(option.getValue());

        initialize();
    }

    /**
     * {@inheritDoc}
     */
    public void updateOption() {
        getOption().setValue(box.getSelectedIndex());
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        box.setSelectedIndex(getOption().getValue());
    }

    /**
     * {@inheritDoc}
     */
    public JComboBox getComponent() {
        return box;
    }

}
