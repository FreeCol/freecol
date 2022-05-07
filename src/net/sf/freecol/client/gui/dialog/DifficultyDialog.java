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

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.option.OptionGroup;


/**
 * Dialog for displaying and modifying the difficulty level.
 *
 * @see OptionGroup
 */
public final class DifficultyDialog extends OptionsDialog
    implements TreeSelectionListener {

    private static final Logger logger = Logger.getLogger(DifficultyDialog.class.getName());

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
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     * @param specification The {@code Specification} to base the difficulty on.
     * @param level An {@code OptionGroup} encapsulating the difficulty
     *     level to display.
     * @param editable Is the dialog editable?
     */
    public DifficultyDialog(FreeColClient freeColClient, JFrame frame,
                            Specification specification, OptionGroup level,
                            boolean editable) {
        super(freeColClient, level, "difficultyDialog",
              FreeColDirectories.CUSTOM_DIFFICULTY_FILE_NAME,
              "model.difficulty.custom", editable);

        this.specification = specification;
        this.selected = level;
        
        getOptionUI().getTree().addTreeSelectionListener(this);
        
        final List<JButton> extraButtons = new ArrayList<>();
        if (isEditable()) {
            final JButton resetButton = Utility.localizedButton("reset");
            addResetAction(resetButton);
            
            final JButton loadButton = Utility.localizedButton("load");
            addLoadAction(loadButton);
                    
            final JButton saveButton = Utility.localizedButton("save");
            addSaveAction(saveButton);

            extraButtons.add(resetButton);
            extraButtons.add(loadButton);
            extraButtons.add(saveButton);
        }
        initialize(frame, extraButtons);
    }


    /**
     * Gets this dialog's instance of the {@code Specification}.
     *
     * @return The {@code Specification}.
     */
    @Override
    public Specification getSpecification() {
        return this.specification;
    }


    // Internals

    /**
     * Add a reset action to a button.
     *
     * @param button The {@code JButton} to add the action to.
     */
    private void addResetAction(JButton button) {
        button.addActionListener((ActionEvent ae) -> {
                getOptionUI().reset();
            });
    }

    /**
     * Add a load action to a button.
     *
     * @param button The {@code JButton} to add the action to.
     */
    private void addLoadAction(JButton button) {
        button.addActionListener((ActionEvent ae) -> {
                File dir = FreeColDirectories.getOptionsDirectory();
                File file = getGUI().showLoadDialog(dir, "xml");
                if (file != null) {
                    if (load(file)) {
                        ; // OptionsDialog.load should update the GUI
                    } else {
                        StringTemplate err = StringTemplate
                            .template("error.couldNotLoadDifficulty")
                            .addName("%name%", file.getPath());
                        getGUI().showErrorPanel(err);
                    }
                }
            });
    }

    /**
     * Add a save action to a button.
     *
     * @param button The {@code JButton} to add the action to.
     */
    private void addSaveAction(JButton button) {
        button.addActionListener((ActionEvent ae) -> {
                File dir = FreeColDirectories.getOptionsDirectory();
                File file = getGUI().showSaveDialog(dir, getDefaultFileName());
                if (file != null) {
                    getOptionUI().updateOption();
                    save(file);
                }
            });
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

    @Override
    protected boolean saveDefaultOptions() {
        // No saving of default options.
        return false;
    }
}
