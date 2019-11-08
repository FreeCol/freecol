/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Role;
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

    private static class AbstractUnitRenderer
        extends FreeColComboBoxRenderer<AbstractUnitOption> {

        @Override
        public void setLabelValues(JLabel label, AbstractUnitOption value) {
            label.setText(Messages.message(value.getValue().getLabel()));
        }
    }

    private static class RoleRenderer
        extends FreeColComboBoxRenderer<String> {

        @Override
        public void setLabelValues(JLabel label, String value) {
            label.setText((value == null) ? "" : Messages.getName(value));
        }
    }

    private final JPanel panel;
    private final IntegerOptionUI numberUI;
    private final UnitTypeOptionUI typeUI;
    private final StringOptionUI roleUI;
    private final boolean roleEditable;


    /**
     * Creates a new {@code AbstractUnitOptionUI} for the given
     * {@code AbstractUnitOption}.
     *
     * @param option The {@code AbstractUnitOption} to make a
     *     user interface for
     * @param editable boolean whether user can modify the setting
     */
    public AbstractUnitOptionUI(final AbstractUnitOption option,
                                final boolean editable) {
        super(option, editable);

        this.panel = new MigPanel(new MigLayout());

        IntegerOption numberOption = option.getNumber();
        UnitTypeOption typeOption = option.getUnitType();
        StringOption roleOption = option.getRole();
        boolean numberEditable = editable
            && (numberOption.getMaximumValue() > numberOption.getMinimumValue());
        this.numberUI = new IntegerOptionUI(numberOption, numberEditable);
        Utility.localizeToolTip(this.numberUI.getComponent(), "number");
        this.panel.add(this.numberUI.getComponent(), "width 30%");

        boolean typeEditable = editable
            && typeOption.getChoices().size() > 1;
        this.typeUI = new UnitTypeOptionUI(typeOption, typeEditable);
        Utility.localizeToolTip(this.typeUI.getComponent(), "unitType");
        this.typeUI.getComponent().addItemListener(this);
        this.panel.add(this.typeUI.getComponent(), "width 35%");

        roleEditable = editable
            && roleOption.getChoices().size() > 1;
        this.roleUI = new StringOptionUI(roleOption, roleEditable);
        Utility.localizeToolTip(this.roleUI.getComponent(), "model.role.name");
        this.roleUI.getComponent().setRenderer(new RoleRenderer());
        this.panel.add(this.roleUI.getComponent(), "width 35%");

        initialize();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        // When the unit type changes, we have to reset the role choices
        JComboBox<String> box = this.roleUI.getComponent();
        DefaultComboBoxModel<String> model;
        boolean enable = false;
        UnitType type = (UnitType)this.typeUI.getComponent().getSelectedItem();
        if (type != null && type.hasAbility(Ability.CAN_BE_EQUIPPED)) {
            final Specification spec = type.getSpecification();
            final NationType nt = getOption().getNationType();
            int n = 0;
            model = new DefaultComboBoxModel<>();
            for (String ri : getOption().getRole().getChoices()) {
                Role role = spec.getRole(ri);
                if (role.isAvailableTo(type, nt)) {
                    model.addElement(ri);
                    n++;
                }
            }
            enable = n > 1 && isEditable();
        } else {
            model = new DefaultComboBoxModel<>(new String[] {
                    Specification.DEFAULT_ROLE_ID });
        }
        box.setModel(model);
        box.setEnabled(enable);
    }


    // Implement OptionUI

    /**
     * {@inheritDoc}
     */
    @Override
    public ListCellRenderer getListCellRenderer() {
        return new AbstractUnitRenderer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JPanel getComponent() {
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public void reset() {
        typeUI.reset();
        roleUI.reset();
        numberUI.reset();
    }
}
