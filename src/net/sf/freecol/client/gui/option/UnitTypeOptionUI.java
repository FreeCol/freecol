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

import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.UnitTypeOption;


/**
 * This class provides visualization for a
 * {@link net.sf.freecol.common.option.UnitTypeOption} in order to enable
 * values to be both seen and changed.
 */
public final class UnitTypeOptionUI extends OptionUI<UnitTypeOption>  {

    private static class ChoiceRenderer
        extends FreeColComboBoxRenderer<UnitType> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void setLabelValues(JLabel label, UnitType value) {
            label.setText((value == null) ? Messages.message("none")
                : Messages.getName(value));
        }
    }

    private final JComboBox<UnitType> box = new JComboBox<>();


    /**
     * Creates a new <code>UnitTypeOptionUI</code> for the given
     * <code>UnitTypeOption</code>.
     *
     * @param option The <code>UnitTypeOption</code> to make a user
     *     interface for.
     * @param editable boolean whether user can modify the setting
     */
    public UnitTypeOptionUI(final UnitTypeOption option, boolean editable) {
        super(option, editable);

        List<UnitType> choices = option.getChoices();
        box.setModel(new DefaultComboBoxModel<>(choices
                .toArray(new UnitType[choices.size()])));
        box.setSelectedItem(option.getValue());
        box.setRenderer(new ChoiceRenderer());

        initialize();
    }


    // Implement OptionUI

    /**
     * {@inheritDoc}
     */
    @Override
    public JComboBox getComponent() {
        return box;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateOption() {
        getOption().setValue((UnitType) box.getSelectedItem());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        box.setSelectedItem(getOption().getValue());
    }
}
