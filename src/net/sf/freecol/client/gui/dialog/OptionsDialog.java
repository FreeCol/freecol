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

package net.sf.freecol.client.gui.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.DialogHandler;
import net.sf.freecol.client.gui.option.OptionGroupUI;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.option.OptionGroup;


/**
 * Dialog for changing the options of an {@link OptionGroup}.
 */
public abstract class OptionsDialog extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(OptionsDialog.class.getName());

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
    private MigPanel optionPanel;
    protected MigPanel panel;
    
    private DialogHandler<OptionGroup> dialogHandler;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param group The {@code OptionGroup} to display.
     * @param headerKey The message identifier for the header.
     * @param defaultFileName The name of the default file to back
     *     this dialog with.
     * @param optionGroupId The identifier for the overall option group.
     */
    protected OptionsDialog(FreeColClient freeColClient, OptionGroup group, String headerKey,
                            String defaultFileName, String optionGroupId, boolean editable) {
        super(freeColClient, null, new MigLayout("fill"));

        this.editable = editable;
        this.group = group;
        this.ui = new OptionGroupUI(getGUI(), this.group, editable);
        this.defaultFileName = defaultFileName;
        this.optionGroupId = optionGroupId;
        preparePanel(headerKey, this.ui);
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
    
    public void setDialogHandler(DialogHandler<OptionGroup> dialogHandler) {
        this.dialogHandler = dialogHandler;
    }

    /**
     * Load the panel.
     *
     * @param headerKey A message key for the panel title.
     * @param ui The {@code OptionGroupUI} to encapsulate.
     */
    private void preparePanel(String headerKey, OptionGroupUI ui) {
        this.optionPanel = new MigPanel(new MigLayout("fill, insets 0"));
        this.optionPanel.setOpaque(false);
        this.optionPanel.add(ui, "grow, gap 0 0, pad 0");
        this.optionPanel.setSize(this.optionPanel.getPreferredSize());

        this.panel = new MigPanel(new MigLayout("wrap 1, fill, insets 0"));
        this.panel.add(Utility.localizedHeader(Messages.nameKey(headerKey),
                                               Utility.FONTSPEC_TITLE),
                       "span, center");
    }

    /**
     * Initialize this dialog.
     * 
     * @param frame The owner frame.
     * @param c Extra choices to add beyond the default ok and cancel.
     */
    protected void initialize(JFrame frame, List<JButton> extraButtons) {
        this.panel.add(this.optionPanel, "width 100%, height 100%, gap 0 0, pad 0");

        add(panel, "grow, gap 0 0, pad 0");
        add(okButton, "newline, split, tag ok");
        
        if (isEditable()) {
            final JButton cancelButton = Utility.localizedButton("cancel");
            cancelButton.setActionCommand(CANCEL);
            cancelButton.addActionListener(this);
            add(cancelButton, "tag cancel");
            setEscapeAction(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        OptionsDialog.this.cancelOptionsDialog();
                    }
                });
        }
        
        for (JButton button : extraButtons) {
            add(button, "tag left");
        }
        
        final float scaleFactor = getImageLibrary().getScaleFactor();
        final int maxWidth = (int) (850 * scaleFactor);
        final int maxHeight = (int) (650 * scaleFactor);
        final int width = Math.min(maxWidth, frame.getWidth() - 200);
        final int height = Math.min(maxHeight, frame.getHeight() - 200);
        panel.setSize(new Dimension(width, height));
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
        this.optionPanel.add(this.ui, "grow");
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
        OptionGroup og = group;
        if (og == null) {
            og = OptionGroup.loadOptionGroup(file, getOptionGroupId(), getSpecification());
        } else {
            group.load(file);
        }
        
        if (og == null) {
            return false;
        }
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
        getGUI().showErrorPanel(FreeCol.badFile("error.couldNotSave", file));
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
   

    /**
     * Resets so that the option with preview (volume)
     * gets restored on cancel.
     */
    public void cancelOptionsDialog() {
        getOptionUI().reset();
        
        getGUI().removeComponent(this);
        if (dialogHandler != null) {
            dialogHandler.handle(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String command = ae.getActionCommand();
        if (OK.equals(command)) {
            if (!isEditable()) {
                getGUI().removeComponent(this);
                if (dialogHandler != null) {
                    dialogHandler.handle(null);
                }
                return;
            }
            getOptionUI().updateOption();
            saveDefaultOptions();
            
            getGUI().removeComponent(this);
            if (dialogHandler != null) {
                dialogHandler.handle(group);
            }
        } else if (CANCEL.equals(command)) {
            cancelOptionsDialog();
        } else {
            logger.warning("Bad event: " + command);
        }
    }
}
