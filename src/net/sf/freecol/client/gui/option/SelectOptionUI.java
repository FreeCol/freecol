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

package net.sf.freecol.client.gui.option;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.option.SelectOption;


/**
 * This class provides visualization for a
 * {@link net.sf.freecol.common.option.SelectOption} in order to enable
 * values to be both seen and changed.
 */
public final class SelectOptionUI extends OptionUI<SelectOption> {

    private final JComboBox<String> box = new JComboBox<>();


    /**
     * Creates a new <code>SelectOptionUI</code> for the given
     * <code>SelectOption</code>.
     *
     * @param option The <code>SelectOption</code> to make a user
     *     interface for.
     * @param editable Whether user can modify the setting.
     */
    public SelectOptionUI(final SelectOption option, boolean editable) {
        super(option, editable);

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (String string : option.getItemValues().values()) {
            model.addElement(option.localizeLabels() ? Messages.message(string)
                : string);
        }

        box.setModel(model);
        box.setSelectedIndex(option.getValue());

        initialize();
    }


    // Implement OptionUI

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent getComponent() {
        return box;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateOption() {
        getOption().setValue(box.getSelectedIndex());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        box.setSelectedIndex(getOption().getValue());
    }
}
