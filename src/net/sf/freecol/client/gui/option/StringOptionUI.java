/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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

import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.option.StringOption;


/**
 * This class provides visualization for a
 * {@link net.sf.freecol.common.option.StringOption} in order to enable
 * values to be both seen and changed.
 */
public final class StringOptionUI extends OptionUI<StringOption>  {

    private JComboBox<String> box = new JComboBox<String>();


    /**
     * Creates a new <code>StringOptionUI</code> for the given
     * <code>StringOption</code>.
     *
     * @param option The <code>StringOption</code> to make a user
     *     interface for.
     * @param editable Whether user can modify the setting.
     */
    public StringOptionUI(GUI gui, final StringOption option,
                          boolean editable) {
        super(gui, option, editable);

        List<String> choices = option.getChoices();
        box.setModel(new DefaultComboBoxModel<String>(choices
                .toArray(new String[choices.size()])));
        box.setSelectedItem(option.getValue());
        box.setRenderer(new FreeColComboBoxRenderer<String>("", true));

        initialize();
    }


    // Implement OptionUI

    /**
     * {@inheritDoc}
     */
    public JComboBox<String> getComponent() {
        return box;
    }

    /**
     * {@inheritDoc}
     */
    public void updateOption() {
        getOption().setValue((String)box.getSelectedItem());
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        box.setSelectedItem(getOption().getValue());
    }
}
