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

package net.sf.freecol.client.gui.option;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.option.ModOption;


/**
 * This class provides visualization for a
 * {@link net.sf.freecol.common.option.ModOption} in order to enable
 * values to be both seen and changed.
 */
public final class ModOptionUI extends OptionUI<ModOption>  {

    private JComboBox box = new JComboBox();

    /**
    * Creates a new <code>ModOptionUI</code> for the given <code>ModOption</code>.
    *
    * @param option The <code>ModOption</code> to make a user interface for
    * @param editable boolean whether user can modify the setting
    */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public ModOptionUI(GUI gui, final ModOption option, boolean editable) {
        super(gui, option, editable);

        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (FreeColModFile choice : option.getChoices()) {
            model.addElement(choice);
        }
        box.setModel(model);
        box.setRenderer(new ChoiceRenderer());
        if (option.getValue() != null) {
            box.setSelectedItem(option.getValue());
        }
        initialize();
    }

    /**
     * {@inheritDoc}
     */
    public void updateOption() {
        getOption().setValue((FreeColModFile) box.getSelectedItem());
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        box.setSelectedItem(getOption().getValue());
    }

    /**
     * {@inheritDoc}
     */
    public JComboBox getComponent() {
        return box;
    }

    private class ChoiceRenderer extends FreeColComboBoxRenderer {

        @Override
        public void setLabelValues(JLabel label, Object value) {
            FreeColModFile modFile = null;
            if (value instanceof FreeColModFile) {
                modFile = (FreeColModFile) value;
            } else if (value instanceof ModOption) {
                modFile = (FreeColModFile) ((ModOption) value).getValue();
            }
            if (modFile == null) {
                label.setText(value.toString());
            } else {
                String key = "mod." + modFile.getId() + ".name";
                label.setText(Messages.message(key));
                if (Messages.containsKey(key + ".shortDescription")) {
                    label.setToolTipText(Messages.message(key + ".shortDescription"));
                }
            }
        }
    }

    public ListCellRenderer getListCellRenderer() {
        return new ChoiceRenderer();
    }

}
