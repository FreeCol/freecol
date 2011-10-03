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

package net.sf.freecol.client.gui.panel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * This panel displays the Colopedia.
 */
public class ConceptDetailPanel extends FreeColPanel
    implements ColopediaDetailPanel<String> {

    private static final String id = "colopediaAction." + PanelType.CONCEPTS;

    private static final String[] concepts = new String[] {
        "taxes"

    };

    /**
     * Creates a new instance of this ColopediaDetailPanel.
     *
     * @param colopediaPanel the ColopediaPanel
     */
    public ConceptDetailPanel(ColopediaPanel colopediaPanel) {
        super(colopediaPanel.getCanvas());
    }

    public String getName() {
        return Messages.message(id + ".name");
    }

    /**
     * Adds one or several subtrees for all the objects for which this
     * ColopediaDetailPanel could build a detail panel to the given
     * root node.
     *
     * @param root a <code>DefaultMutableTreeNode</code>
     */
    public void addSubTrees(DefaultMutableTreeNode root) {
        DefaultMutableTreeNode node =
            new DefaultMutableTreeNode(new ColopediaTreeItem(this, id, getName(), null));
        for (String concept : concepts) {
            String nodeId = "colopedia.concepts." + concept;
            String nodeName = Messages.message(nodeId + ".name");
            node.add(new DefaultMutableTreeNode(new ColopediaTreeItem(this, nodeId, nodeName, null)));
        }
        root.add(node);
    }

    /**
     * Builds the details panel for the given ID.
     *
     * @param id the ID of the object to display
     * @param panel the detail panel to build
     */
    public void buildDetail(String id, JPanel panel) {
        if (this.id.equals(id)) {
            return;
        }

        panel.setLayout(new MigLayout("wrap 1"));

        JLabel header = localizedLabel(id + ".name");
        header.setFont(smallHeaderFont);
        panel.add(header, "align center, wrap 20");

        panel.add(getDefaultTextArea(Messages.message(id + ".description")));

    }

}
