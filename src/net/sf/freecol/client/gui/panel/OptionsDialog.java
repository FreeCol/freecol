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

package net.sf.freecol.client.gui.panel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.OptionGroupUI;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.option.OptionGroup;


/**
 * Dialog for changing the options of an {@link OptionGroup}.
 */
public abstract class OptionsDialog extends FreeColDialog<OptionGroup> {

    private static final Logger logger = Logger.getLogger(OptionsDialog.class.getName());

    private boolean editable;
    private OptionGroup group;
    private String header;
    private OptionGroupUI ui;
    private String defaultFileName;
    private String optionGroupId;
    private JScrollPane scrollPane;
    private MigPanel optionPanel;
    protected MigPanel panel;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param editable Whether the dialog is editable.
     */
    public OptionsDialog(FreeColClient freeColClient, boolean editable,
                         OptionGroup group, String header,
                         String defaultFileName, String optionGroupId) {
        super(freeColClient);

        this.editable = editable;
        this.group = group;
        this.header = header;
        this.ui = new OptionGroupUI(getGUI(), this.group, this.editable);
        this.defaultFileName = defaultFileName;
        this.optionGroupId = optionGroupId;
        preparePanel(this.header, this.ui);
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
     * @return The <code>OptionGroup</code>.
     */
    protected OptionGroup getGroup() {
        return this.group;
    }

    /**
     * Get the option group UI controlling this dialog.
     *
     * @return The <code>OptionGroupUI</code>.
     */
    protected OptionGroupUI getOptionUI() {
        return this.ui;
    }

    /**
     * Get the default name of the file to save the <code>OptionGroup</code>.
     *
     * @return A default file name.
     */
    protected String getDefaultFileName() {
        return this.defaultFileName;
    }

    /**
     * Get the identifier of the <code>OptionGroup</code>.
     *
     * @return The option group identifier.
     */
    protected String getOptionGroupId() {
        return this.optionGroupId;
    }

    /**
     * Load the panel.
     */
    private void preparePanel(String header, OptionGroupUI ui) {
        optionPanel = new MigPanel("ReportPanelUI");
        optionPanel.setPreferredSize(new Dimension(800, 500));
        optionPanel.setOpaque(true);
        optionPanel.add(ui);
        optionPanel.setSize(optionPanel.getPreferredSize());
        this.scrollPane = new JScrollPane(optionPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        this.panel = new MigPanel(new MigLayout("wrap 1, fill"));
        this.panel.add(GUI.getDefaultHeader(header), "center");
    }

    /**
     * Initialize this dialog.
     */
    protected void initialize() {
        this.panel.add(this.scrollPane, "grow"); //"height 100%, width 100%"
        this.panel.setPreferredSize(new Dimension(850, 650));
        this.panel.setSize(this.panel.getPreferredSize());

        List<ChoiceItem<OptionGroup>> c = choices();
        c.add(new ChoiceItem<OptionGroup>(Messages.message("ok"),
                this.group).okOption());
        c.add(new ChoiceItem<OptionGroup>(Messages.message("cancel"),
                (OptionGroup)null,
                isEditable()).cancelOption().defaultOption());
        initialize(DialogType.PLAIN, true, this.panel, null, c);
    }

    /**
     * Load an option group from given File.
     *
     * @param file A <code>File</code> to load from.
     * @return True if the load succeeded.
     */
    protected boolean load(File file) {
        boolean ret = false;
        FreeColXMLReader xr = null;
        try {
            xr = new FreeColXMLReader(new FileInputStream(file));
            xr.nextTag();
            // TODO: read into group rather than specification
            OptionGroup group = new OptionGroup(getSpecification());
            group.readFromXML(xr);
            String expect = getOptionGroupId();
            if (!expect.equals(group.getId())) {
                try {
                    group = group.getOptionGroup(expect);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Options file " + file.getPath()
                        + " does not contain expected group " + expect, e);
                }
            }
            if (group != null) {
                getSpecification().getOptionGroup(expect).setValue(group);
                logger.info("Loaded options from file " + file.getPath());
                reset(group);
                ret = true;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load OptionGroup "
                + getOptionGroupId() + " from " + file.getName(), e);
        } finally {
            if (xr != null) xr.close();
        }
        return ret;
    }

    /**
     * Reset the group for this panel.
     *
     * @param group The new <code>OptionGroup</code>.
     */
    private void reset(OptionGroup group) {
        this.group = group;
        this.optionPanel.removeAll();
        this.ui = new OptionGroupUI(getGUI(), this.group, this.editable);
        this.optionPanel.add(this.ui);
        revalidate();
        repaint();
    }

    /**
     * Save an option group to a given File.
     *
     * @param file The <code>File</code> to save to.
     * @return True if the save succeeded.
     */
    protected boolean save(File file) {
        try {
            group.save(file);
            return true;
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "Save failed", e);
            StringTemplate t = StringTemplate.template("failedToSave")
                .addName("%name%", file.getPath());
            getGUI().showInformationMessage(t);
            return false;
        }
    }

    /**
     * Load a custom option group from the default file.
     *
     * @return True if the options were loaded.
     */
    protected boolean loadCustomOptions() {
        File customFile = new File(FreeColDirectories.getOptionsDirectory(),
                                   getDefaultFileName());
        return (customFile.exists()) ? load(customFile) : false;
    }


    // Override FreeColDialog

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup getResponse() {
        OptionGroup value = super.getResponse();
        if (value == null) {
            getOptionUI().reset();
        } else {
            getOptionUI().updateOption();
        }
        return value;
    }
}
