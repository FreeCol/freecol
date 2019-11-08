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

package net.sf.freecol.client.gui.dialog;

import java.awt.Dimension;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.option.OptionGroupUI;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.option.OptionGroup;


/**
 * Dialog for changing the options of an {@link OptionGroup}.
 */
public abstract class OptionsDialog extends FreeColDialog<OptionGroup> {

    private static final Logger logger = Logger.getLogger(OptionsDialog.class.getName());

    /** Are the settings in this dialog editable? */
    private final boolean editable;

    /** The option group to display. */
    private OptionGroup group;

    /** The supporting UI for the displayed group. */
    private OptionGroupUI ui;

    /** The name of the file containing the default settings. */
    private final String defaultFileName;

    /**
     * The option group identifier defining the group to load within the
     * default settings file.
     */
    private final String optionGroupId;

    /** Dialog internal parts. */
    private JScrollPane scrollPane;
    private MigPanel optionPanel;
    protected MigPanel panel;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     * @param editable Whether the dialog is editable.
     * @param group The {@code OptionGroup} to display.
     * @param headerKey The message identifier for the header.
     * @param defaultFileName The name of the default file to back
     *     this dialog with.
     * @param optionGroupId The identifier for the overall option group.
     */
    protected OptionsDialog(FreeColClient freeColClient, JFrame frame,
                            boolean editable, OptionGroup group, String headerKey,
                            String defaultFileName, String optionGroupId) {
        super(freeColClient, frame);

        this.editable = editable;
        this.group = group;
        this.ui = new OptionGroupUI(getGUI(), this.group, this.editable);
        this.defaultFileName = defaultFileName;
        this.optionGroupId = optionGroupId;
        preparePanel(headerKey, this.ui);
    }


    /**
     * Is this dialog editable?
     *
     * @return True if the dialog is editable.
     */
    protected boolean isEditable() {
        return this.editable;
    }

    /**
     * Get the option group being displayed by this dialog.
     *
     * @return The {@code OptionGroup}.
     */
    protected OptionGroup getGroup() {
        return this.group;
    }

    /**
     * Get the option group UI controlling this dialog.
     *
     * @return The {@code OptionGroupUI}.
     */
    protected OptionGroupUI getOptionUI() {
        return this.ui;
    }

    /**
     * Get the default name of the file to save the {@code OptionGroup}.
     *
     * @return A default file name.
     */
    protected String getDefaultFileName() {
        return this.defaultFileName;
    }

    /**
     * Get the identifier of the {@code OptionGroup}.
     *
     * @return The option group identifier.
     */
    protected String getOptionGroupId() {
        return this.optionGroupId;
    }

    /**
     * Load the panel.
     *
     * @param headerKey A message key for the panel title.
     * @param ui The {@code OptionGroupUI} to encapsulate.
     */
    private void preparePanel(String headerKey, OptionGroupUI ui) {
        this.optionPanel = new MigPanel("ReportPanelUI");
        this.optionPanel.setOpaque(true);
        this.optionPanel.add(ui);
        this.optionPanel.setSize(this.optionPanel.getPreferredSize());
        this.scrollPane = new JScrollPane(this.optionPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        this.panel = new MigPanel(new MigLayout("wrap 1, fill"));
        this.panel.add(Utility.localizedHeader(Messages.nameKey(headerKey),
                                               false), "span, center");
    }

    /**
     * Initialize this dialog.
     * 
     * @param frame The owner frame.
     * @param c Extra choices to add beyond the default ok and cancel.
     */
    protected void initialize(JFrame frame, List<ChoiceItem<OptionGroup>> c) {
        this.panel.add(this.scrollPane, "height 100%, width 100%");
        this.panel.setPreferredSize(new Dimension(850, 650));
        this.panel.setSize(this.panel.getPreferredSize());

        c.add(new ChoiceItem<>(Messages.message("ok"), this.group).okOption());
        c.add(new ChoiceItem<>(Messages.message("cancel"), (OptionGroup)null,
                               isEditable()).cancelOption().defaultOption());
        
        initializeDialog(frame, DialogType.PLAIN, true, this.panel, null, c);
    }

    /**
     * Set the group for this panel.
     *
     * @param group The new {@code OptionGroup}.
     */
    protected void set(OptionGroup group) {
        this.group = group;
        update();
    }

    /**
     * Completely update the ui.
     */
    private void update() {
        this.optionPanel.removeAll();
        this.ui = new OptionGroupUI(getGUI(), this.group, this.editable);
        this.optionPanel.add(this.ui);
        invalidate();
        validate();
        repaint();
    }

    /**
     * Load an option group from given File.
     *
     * @param file A {@code File} to load from.
     * @return True if the load succeeded.
     */
    protected boolean load(File file) {
        OptionGroup og = OptionGroup.loadOptionGroup(file, getOptionGroupId(),
                                                     getSpecification());
        if (og == null) return false;
        set(og);
        return true;
    }

    /**
     * Save an option group to a given File.
     *
     * @param file The {@code File} to save to.
     * @return True if the save succeeded.
     */
    protected boolean save(File file) {
        if (this.group.save(file, null, true)) return true;
        getGUI().showErrorMessage(FreeCol.badFile("error.couldNotSave", file));
        return false;
    }

    /**
     * Load the option group from the default file.
     *
     * @return True if the options were loaded.
     */
    protected boolean loadDefaultOptions() {
        File f = FreeColDirectories.getOptionsFile(getDefaultFileName());
        return (f.exists()) ? load(f) : false;
    }

    /**
     * Save the option group to the default file.
     *
     * @return True if the options were saved.
     */
    protected boolean saveDefaultOptions() {
        File f = FreeColDirectories.getOptionsFile(getDefaultFileName());
        return save(f);
    }


    // Override FreeColDialog

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup getResponse() {
        OptionGroup value = super.getResponse();
        if (value == null) {
            ; // Cancelled, do nothing
        } else {
            getOptionUI().updateOption();
        }
        return value;
    }
}
