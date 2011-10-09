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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.option.AbstractUnitOption;
import net.sf.freecol.common.option.UnitListOption;

/**
 * This class provides visualization for a List of {@link
 * net.sf.freecol.common.option.AbstractUnitOption}s. In order to
 * enable values to be both seen and changed.
 *
 * TODO: derive from ListOptionUI
 */
public final class UnitListOptionUI extends OptionUI<UnitListOption> {

    private JPanel panel = new JPanel();
    private List<AbstractUnitOptionUI> children = new ArrayList<AbstractUnitOptionUI>();

    private JButton addButton = new JButton(Messages.message("list.add"));
    private JButton removeButton = new JButton(Messages.message("list.remove"));
    private JButton upButton = new JButton(Messages.message("list.up"));
    private JButton downButton = new JButton(Messages.message("list.down"));



    /**
     * Creates a new <code>UnitListOptionUI</code> for the given
     * <code>ListOption</code>.
     *
     * @param option
     * @param editable boolean whether user can modify the setting
     */
    public UnitListOptionUI(final UnitListOption option, boolean editable) {
        super(option, editable);

        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         super.getLabel().getText()));

        panel.setLayout(new MigLayout("flowy", "[500]"));

        for (AbstractUnitOption o : option.getValue()) {
            AbstractUnitOptionUI ui = new AbstractUnitOptionUI(o, editable);
            children.add(ui);
            panel.add(ui.getComponent(), "growx");
        }

        initialize();
    }

    /**
     * Returns <code>null</code>, since this OptionUI does not require
     * an external label.
     *
     * @return null
     */
    @Override
    public final JLabel getLabel() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public JPanel getComponent() {
        return panel;
    }

    /**
     * Updates the value of the {@link net.sf.freecol.common.getOption().Option} this object keeps.
     */
    public void updateOption() {
        //getOption().setValue(getValue());
    }

    private List<AbstractUnit> getValue() {
        List<AbstractUnit> result = new ArrayList<AbstractUnit>();
        for (AbstractUnitOptionUI ui : children) {
            result.add(ui.getOption().getValue());
        }
        return result;
    }

    /**
     * Reset with the value from the Option.
     */
    public void reset() {
        //TODO
    }

    private static class ListOptionElement<T> {
        private final T object;
        private final String text;

        private ListOptionElement(final T object, final String text) {
            this.object = object;
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
