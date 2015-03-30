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
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;

import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.option.ModOption;


/**
 * This class provides visualization for a
 * {@link net.sf.freecol.common.option.ModOption} in order to enable
 * values to be both seen and changed.
 */
public final class ModOptionUI extends OptionUI<ModOption>  {


    private static class BoxRenderer
        extends FreeColComboBoxRenderer<FreeColModFile> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void setLabelValues(JLabel label, FreeColModFile value) {
            if (value != null) {
                ModOptionUI.labelModFile(label, value);
            }
        }
    }

    private static class ModOptionRenderer
        extends FreeColComboBoxRenderer<ModOption> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void setLabelValues(JLabel label, ModOption value) {
            FreeColModFile modFile = value.getValue();
            if (modFile == null) {
                label.setText(value.toString());
            } else {
                ModOptionUI.labelModFile(label, modFile);
            }
        }
    }


    /** The selection box for the various mod files. */
    private final JComboBox<FreeColModFile> box;


    /**
     * Creates a new <code>ModOptionUI</code> for the given
     * <code>ModOption</code>.
     *
     * @param option The <code>ModOption</code> to make a user interface for
     * @param editable boolean whether user can modify the setting
     */
    public ModOptionUI(final ModOption option, boolean editable) {
        super(option, editable);

        DefaultComboBoxModel<FreeColModFile> model
            = new DefaultComboBoxModel<>();
        for (FreeColModFile choice : option.getChoices()) {
            model.addElement(choice);
        }
        this.box = new JComboBox<>();
        this.box.setModel(model);
        this.box.setRenderer(new BoxRenderer());
        if (option.getValue() != null) {
            this.box.setSelectedItem(option.getValue());
        }
        initialize();
    }


    /**
     * Add information from a mod file to a label.
     *
     * @param label The <code>JLabel</code> to modify.
     * @param modFile The <code>FreeColModFile</code> to use.
     */
    private static void labelModFile(JLabel label, FreeColModFile modFile) {
        String key = "mod." + modFile.getId();
        label.setText(Messages.getName(key));
        if (Messages.containsKey(Messages.shortDescriptionKey(key))) {
            label.setToolTipText(Messages.getShortDescription(key));
        }
    }


    // Implement OptionUI

    /**
     * {@inheritDoc}
     */
    @Override
    public ListCellRenderer getListCellRenderer() {
        return new ModOptionRenderer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateOption() {
        getOption().setValue((FreeColModFile)this.box.getSelectedItem());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComboBox getComponent() {
        return this.box;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        this.box.setSelectedItem(getOption().getValue());
    }
}
