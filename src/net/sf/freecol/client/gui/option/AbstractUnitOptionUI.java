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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.AbstractUnitOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.StringOption;
import net.sf.freecol.common.option.UnitTypeOption;


/**
 * This class provides visualization for an
 * {@link net.sf.freecol.common.option.AbstractUnitOption} in order to enable
 * values to be both seen and changed.
 */
public final class AbstractUnitOptionUI extends OptionUI<AbstractUnitOption>
    implements ItemListener {

    private class AbstractUnitRenderer
        extends FreeColComboBoxRenderer<AbstractUnitOption> {

        @Override
        public void setLabelValues(JLabel label, AbstractUnitOption value) {
            label.setText(Messages.message(value.getValue().getLabel()));
        }
    }

    private class RoleRenderer
        extends FreeColComboBoxRenderer<String> {

        @Override
        public void setLabelValues(JLabel label, String value) {
            label.setText(Messages.getName(value));
        }
    }

    private JPanel panel;
    private IntegerOptionUI numberUI;
    private UnitTypeOptionUI typeUI;
    private StringOptionUI roleUI;
    private boolean roleEditable;


    /**
     * Creates a new <code>AbstractUnitOptionUI</code> for the given
     * <code>AbstractUnitOption</code>.
     *
     * @param option The <code>AbstractUnitOption</code> to make a
     *     user interface for
     * @param editable boolean whether user can modify the setting
     */
    public AbstractUnitOptionUI(GUI gui, final AbstractUnitOption option,
                                boolean editable) {
        super(gui, option, editable);

        panel = new MigPanel();
        panel.setLayout(new MigLayout());

        IntegerOption numberOption = option.getNumber();
        UnitTypeOption typeOption = option.getUnitType();
        StringOption roleOption = option.getRole();

        boolean numberEditable = editable
            && (numberOption.getMaximumValue() > numberOption.getMinimumValue());
        numberUI = new IntegerOptionUI(gui, numberOption, numberEditable);
        GUI.localizeToolTip(numberUI.getComponent(), "report.numberOfUnits");
        panel.add(numberUI.getComponent(), "width 30%");

        boolean typeEditable = editable
            && typeOption.getChoices().size() > 1;
        typeUI = new UnitTypeOptionUI(gui, typeOption, typeEditable);

        GUI.localizeToolTip(typeUI.getComponent(), "model.unit.type");
        typeUI.getComponent().addItemListener(this);
        panel.add(typeUI.getComponent(), "width 35%");

        roleEditable = editable
            && roleOption.getChoices().size() > 1;
        roleUI = new StringOptionUI(gui, roleOption, roleEditable);
        GUI.localizeToolTip(roleUI.getComponent(), "model.role.name");
        roleUI.getComponent().setRenderer(new RoleRenderer());
        panel.add(roleUI.getComponent(), "width 35%");

        initialize();
    }


    public void itemStateChanged(ItemEvent e) {
        JComboBox<String> box = roleUI.getComponent();
        DefaultComboBoxModel<String> model;
        boolean enable = false;
        UnitType type = (UnitType)typeUI.getComponent().getSelectedItem();
        if (type.hasAbility(Ability.CAN_BE_EQUIPPED)) {
            model = new DefaultComboBoxModel<String>(roleUI.getOption()
                .getChoices().toArray(new String[0]));
            enable = roleEditable;
        } else {
            model = new DefaultComboBoxModel<String>(new String[] {
                    Specification.DEFAULT_ROLE_ID });
        }
        box.setModel(model);
        box.setEnabled(enable);
    }


    // Implement OptionUI

    /**
     * {@inheritDoc}
     */
    public ListCellRenderer getListCellRenderer() {
        return new AbstractUnitRenderer();
    }

    /**
     * {@inheritDoc}
     */
    public JPanel getComponent() {
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    public void updateOption() {
        typeUI.updateOption();
        roleUI.updateOption();
        numberUI.updateOption();
        UnitType type = typeUI.getOption().getValue();
        String roleId = roleUI.getOption().getValue();
        int number = numberUI.getOption().getValue();
        getOption().setValue(new AbstractUnit(type, roleId, number));
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        typeUI.reset();
        roleUI.reset();
        numberUI.reset();
    }
}
