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

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.OptionGroup;


/**
 * Dialog for displaying and modifying the difficulty level.
 *
 * @see OptionGroup
 */
public final class DifficultyDialog extends OptionsDialog implements TreeSelectionListener {

    private static final Logger logger = Logger.getLogger(DifficultyDialog.class.getName());

    private static final String EDIT = "EDIT";


    private JButton edit = new JButton(Messages.message("edit"));

    private String CUSTOM_LEVEL = "model.difficulty.custom";

    private OptionGroup selected;

    /**
     * We need our own copy of the specification, as the dialog is
     * used before the game has been started.
     */
    private Specification specification;


    /**
     * Use this constructor to display the difficulty level of the
     * current game read-only.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param specification The <code>Specification</code> to base the
     *     difficulty on.
     * @param level An <code>OptionGroup</code> encapsulating the difficulty
     *     level to display.
     * @param editable a <code>boolean</code> value
     */
    public DifficultyDialog(FreeColClient freeColClient,
                            Specification specification,
                            OptionGroup level, boolean editable) {
        super(freeColClient, editable);

        this.specification = specification;
        selected = level;
        initialize(level, Messages.message("difficulty"), null);
        getOptionUI().getTree().addTreeSelectionListener(this);
    }

    private boolean isGroupEditable() {
        return selected != null && selected.isEditable();
    }

    @Override
    public String getOptionGroupId() {
        return CUSTOM_LEVEL;
    }

    /**
     * Returns this dialog's instance of the <code>Specification</code>.
     *
     * @return a <code>Specification</code> value
     */
    @Override
    public Specification getSpecification() {
        return specification;
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            getOptionUI().updateOption();
            getGUI().removeFromCanvas(this);
            setResponse(selected);
            FreeCol.setDifficulty(selected);
        } else if (EDIT.equals(command)) {
            OptionGroup custom = specification.getOptionGroup(CUSTOM_LEVEL);
            custom.setValue(selected);
            JTree tree = getOptionUI().getTree();
            for (int row = tree.getRowCount() - 1; row >= 0; row--) {
                tree.collapseRow(row);
            }
            selectLevel(tree, CUSTOM_LEVEL);
        } else {
            super.actionPerformed(event);
        }
    }

    private void selectLevel(JTree tree, String id) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        for (int index = 0; index < root.getChildCount(); index++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(index);
            OptionGroup group = (OptionGroup) node.getUserObject();
            if (id.equals(group.getId())) {
                TreeNode[] nodes = new TreeNode[] { root, node, node.getFirstChild() };
                tree.setSelectionPath(new TreePath(nodes));
                break;
            }
        }
    }

    public OptionGroup getSelectedGroup(TreePath path) {
        if (path.getPathCount() < 2) {
            return null;
        } else {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getPathComponent(1);
            return (OptionGroup) node.getUserObject();
        }
    }

    public void valueChanged(TreeSelectionEvent event) {
        selected = getSelectedGroup(event.getPath());
        edit.setEnabled(!isGroupEditable());
        save.setEnabled(isGroupEditable());
    }


    /**
     * Returns the default file name to save the custom difficulty
     * level.
     *
     * @return "custom.xml"
     */
    public String getDefaultFileName() {
        return "custom.xml";
    }
}
