/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.Locale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.option.AbstractUnitOption;


/**
 * This class provides visualization for an {@link
 * net.sf.freecol.common.option.AbstractUnitOption}. In order to enable
 * values to be both seen and changed.
 */
public final class AbstractUnitOptionUI extends OptionUI<AbstractUnitOption>  {

    private JPanel panel = new JPanel();
    private JComboBox typeBox;
    private JComboBox roleBox;
    private JSpinner spinner;

    /**
    * Creates a new <code>AbstractUnitOptionUI</code> for the given <code>AbstractUnitOption</code>.
    *
    * @param option The <code>AbstractUnitOption</code> to make a user interface for
    * @param editable boolean whether user can modify the setting
    */
    public AbstractUnitOptionUI(final AbstractUnitOption option, boolean editable) {
        super(option, editable);

        List<String> choices = option.getChoices();

        typeBox = new JComboBox();
        typeBox.setModel(new DefaultComboBoxModel(choices.toArray(new String[choices.size()])));
        typeBox.setSelectedItem(option.getValue());
        typeBox.setRenderer(new ChoiceRenderer());

        typeBox.setEnabled(editable);
        typeBox.setOpaque(false);
        panel.add(typeBox);

        if (option.getSelectRole()) {
            roleBox = new JComboBox();
            roleBox.setModel(new DefaultComboBoxModel(Role.values()));
            roleBox.setSelectedItem(option.getValue());
            roleBox.setRenderer(new ChoiceRenderer());

            roleBox.setEnabled(editable);
            roleBox.setOpaque(false);
            panel.add(roleBox);
        }

        if (editable && option.getMaximumValue() > option.getMinimumValue()) {
            int stepSize = Math.min((option.getMaximumValue() - option.getMinimumValue()) / 10, 1000);
            spinner = new JSpinner();
            spinner.setModel(new SpinnerNumberModel(option.getValue().getNumber(), option.getMinimumValue(),
                                                    option.getMaximumValue(), Math.max(1, stepSize)));
        } else {
            int value = option.getValue().getNumber();
            spinner.setModel(new SpinnerNumberModel(value, value, value, 1));
        }
        panel.add(spinner);

        initialize();

    }

    /**
     * {@inheritDoc}
     */
    public JPanel getComponent() {
        return panel;
    }

    /**
     * Updates the value of the {@link net.sf.freecol.common.option.Option} this object keeps.
     */
    public void updateOption() {
        getOption().setValue(new AbstractUnit((String) typeBox.getSelectedItem(),
                                              (Role) roleBox.getSelectedItem(),
                                              (Integer) spinner.getValue()));
    }

    /**
     * Reset with the value from the option.
     */
    public void reset() {
        AbstractUnitOption option = getOption();
        typeBox.setSelectedItem(option.getValue().getId());
        roleBox.setSelectedItem(option.getValue().getRole());
        spinner.setValue(option.getValue().getNumber());
    }

    private class ChoiceRenderer extends FreeColComboBoxRenderer {

        @Override
        public void setLabelValues(JLabel label, Object value) {
            label.setText(Messages.message((String) value + ".name"));
        }
    }


    private class RoleRenderer extends FreeColComboBoxRenderer {

        @Override
        public void setLabelValues(JLabel label, Object value) {
            label.setText(Messages.message("model.unit.role." + ((String) value).toLowerCase(Locale.US)));
        }
    }

}
