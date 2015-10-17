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

package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.OptionGroup;


/**
 * Dialog for displaying and modifying the difficulty level.
 *
 * @see OptionGroup
 */
public final class DifficultyDialog extends OptionsDialog
    implements TreeSelectionListener {

    private static final Logger logger = Logger.getLogger(DifficultyDialog.class.getName());

    /** File filters array to filter for XML files. */
    private static final FileFilter[] filters = { null };

    /** The currently selected subgroup. */
    private OptionGroup selected;

    /**
     * We need our own copy of the specification, as the dialog is
     * used before the game has been started.
     */
    private final Specification specification;


    /**
     * Use this constructor to display the difficulty level of the
     * current game read-only.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     * @param specification The <code>Specification</code> to base the
     *     difficulty on.
     * @param level An <code>OptionGroup</code> encapsulating the difficulty
     *     level to display.
     * @param editable Is the dialog editable?
     */
    public DifficultyDialog(FreeColClient freeColClient, JFrame frame,
            Specification specification, OptionGroup level, boolean editable) {
        super(freeColClient, frame, editable, level, "difficultyDialog",
              FreeColDirectories.CUSTOM_DIFFICULTY_FILE_NAME,
              "model.difficulty.custom");

        this.specification = specification;
        this.selected = level;

        getOptionUI().getTree().addTreeSelectionListener(this);

        if (isEditable()) {
            loadDefaultOptions();

            JButton resetButton = Utility.localizedButton("reset");
            addResetAction(resetButton);
            
            JButton loadButton = Utility.localizedButton("load");
            addLoadAction(loadButton);
                    
            JButton saveButton = Utility.localizedButton("save");
            addSaveAction(saveButton);

            this.panel.add(resetButton, "span, split 3");
            this.panel.add(loadButton);
            this.panel.add(saveButton);
        }
        initialize(frame);
    }


    /**
     * Gets this dialog's instance of the <code>Specification</code>.
     *
     * @return The <code>Specification</code>.
     */
    @Override
    public Specification getSpecification() {
        return specification;
    }


    // Internals

    /**
     * Add a reset action to a button.
     *
     * @param button The <code>JButton</code> to add the action to.
     */
    private void addResetAction(JButton button) {
        button.addActionListener((ActionEvent ae) -> {
                getOptionUI().reset();
            });
    }

    /**
     * Add a load action to a button.
     *
     * @param button The <code>JButton</code> to add the action to.
     */
    private void addLoadAction(JButton button) {
        initializeFilters();
        button.addActionListener((ActionEvent ae) -> {
                File dir = FreeColDirectories.getOptionsDirectory();
                File file = getGUI().showLoadDialog(dir, filters);
                if (file != null && load(file)) {
                    invalidate();
                    validate();
                    repaint();
                }
            });
    }

    /**
     * Add a save action to a button.
     *
     * @param button The <code>JButton</code> to add the action to.
     */
    private void addSaveAction(JButton button) {
        initializeFilters();
        button.addActionListener((ActionEvent ae) -> {
                File dir = FreeColDirectories.getOptionsDirectory();
                File file = getGUI().showSaveDialog(dir, filters,
                                                    getDefaultFileName());
                if (file != null) {
                    getOptionUI().updateOption();
                    save(file);
                }
            });
    }

    /**
     * Initialize the XML file filter.
     */
    private void initializeFilters() {
        synchronized (filters) {
            if (filters[0] == null) {
                String desc = Messages.message("filter.xml");
                filters[0] = new FileNameExtensionFilter(desc, "xml");
            }
        }
    }


    // Implement TreeSelectionListener

    @Override
    public void valueChanged(TreeSelectionEvent event) {
        TreePath path = event.getPath();
        if (path.getPathCount() >= 2) {
            DefaultMutableTreeNode node
                = (DefaultMutableTreeNode)path.getPathComponent(1);
            this.selected = (OptionGroup)node.getUserObject();
        }
    }


    // Override OptionsDialog

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup getResponse() {
        OptionGroup value = super.getResponse();
        if (value != null) {
            FreeCol.setDifficulty(value);
        }
        return value;
    }
}
