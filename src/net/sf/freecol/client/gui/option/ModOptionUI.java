/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.option.ModOption;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * This class provides visualization for a
 * {@link net.sf.freecol.common.option.ModOption} in order to enable
 * values to be both seen and changed.
 */
public final class ModOptionUI extends OptionUI<ModOption>  {


    private static class BoxRenderer extends FreeColComboBoxRenderer<FreeColModFile> {

        private final GUI gui;
        
        private BoxRenderer(GUI gui) {
            this.gui = gui;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public void setLabelValues(JLabel label, FreeColModFile value) {
            if (value != null) {
                ModOptionUI.labelModFile(gui, label, value);
            }
        }
    }

    private static class ModOptionRenderer extends FreeColComboBoxRenderer<ModOption> {

        private final GUI gui;
        
        private ModOptionRenderer(GUI gui) {
            this.gui = gui;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public void setLabelValues(JLabel label, ModOption value) {
            FreeColModFile modFile = value.getValue();
            if (modFile == null) {
                label.setText(value.toString());
            } else {
                ModOptionUI.labelModFile(gui, label, modFile);
            }
        }
    }

    private final GUI gui;

    /** The selection box for the various mod files. */
    private final JComboBox<FreeColModFile> box;


    /**
     * Creates a new {@code ModOptionUI} for the given
     * {@code ModOption}.
     *
     * @param option The {@code ModOption} to make a user interface for
     * @param editable boolean whether user can modify the setting
     */
    public ModOptionUI(GUI gui, final ModOption option, boolean editable) {
        super(option, editable);

        this.gui = gui;
        
        DefaultComboBoxModel<FreeColModFile> model
            = new DefaultComboBoxModel<>();
        for (FreeColModFile choice : option.getChoices()) {
            model.addElement(choice);
        }
        this.box = new JComboBox<>();
        this.box.setModel(model);
        this.box.setRenderer(new BoxRenderer(gui));
        if (option.getValue() != null) {
            this.box.setSelectedItem(option.getValue());
        }
        initialize();
    }


    /**
     * Add information from a mod file to a label.
     *
     * @param label The {@code JLabel} to modify.
     * @param modFile The {@code FreeColModFile} to use.
     */
    private static void labelModFile(GUI gui, JLabel label, FreeColModFile modFile) {
        final String key = "mod." + modFile.getId();
        label.setText(Messages.getName(key));
        if (Messages.containsKey(Messages.shortDescriptionKey(key))) {
            label.setToolTipText(Messages.getShortDescription(key));
        }
        
        final boolean containsSpecification = modFile.hasSpecification();
        if (containsSpecification) {
            label.setIcon(new ImageIcon(ResourceManager.getImage("image.ui.includesSpecification")));
        } else {
            label.setIcon(new ImageIcon(ResourceManager.getImage("image.ui.noSpecification")));
        }
        
        final boolean shouldBeEnabled = gui.canGameChangingModsBeAdded() || !containsSpecification;
        label.setEnabled(shouldBeEnabled);
    }


    // Implement OptionUI

    /**
     * {@inheritDoc}
     */
    @Override
    public ListCellRenderer getListCellRenderer() {
        return new ModOptionRenderer(gui);
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
