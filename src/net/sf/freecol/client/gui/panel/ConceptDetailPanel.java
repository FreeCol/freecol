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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
        "taxes",
        "efficiency",
        "independence"
    };

    private static final Comparator<DefaultMutableTreeNode> nodeComparator
        = new Comparator<DefaultMutableTreeNode>() {
        public int compare(DefaultMutableTreeNode node1, DefaultMutableTreeNode node2) {
            return ((ColopediaTreeItem) node1.getUserObject()).getText()
            .compareTo(((ColopediaTreeItem) node2.getUserObject()).getText());
        }
    };

    private ColopediaPanel colopediaPanel;


    /**
     * Creates a new instance of this ColopediaDetailPanel.
     *
     * @param colopediaPanel the ColopediaPanel
     */
    public ConceptDetailPanel(ColopediaPanel colopediaPanel) {
        super(colopediaPanel.getCanvas());
        this.colopediaPanel = colopediaPanel;
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
        List<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();
        for (String concept : concepts) {
            String nodeId = "colopedia.concepts." + concept;
            String nodeName = Messages.message(nodeId + ".name");
            nodes.add(new DefaultMutableTreeNode(new ColopediaTreeItem(this, nodeId, nodeName, null)));
        }
        Collections.sort(nodes, nodeComparator);
        for (DefaultMutableTreeNode n : nodes) {
            node.add(n);
        };
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

        panel.setLayout(new MigLayout("wrap 1, center"));

        JLabel header = localizedLabel(id + ".name");
        header.setFont(smallHeaderFont);
        panel.add(header, "align center, wrap 20");

        //panel.add(getDefaultTextArea(Messages.message(id + ".description"), 40));
        JEditorPane editorPane = new JEditorPane("text/html", Messages.message(id + ".description")) {

            @Override
            public void paintComponent(Graphics g) {
                Graphics2D graphics2d = (Graphics2D) g;
                graphics2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                /*
                graphics2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                                            RenderingHints.VALUE_RENDER_QUALITY);
                graphics2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                                            RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                */

                super.paintComponent(graphics2d);
            }
        };

        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,
                                     Boolean.TRUE);
        editorPane.setFont(panel.getFont());
        editorPane.setOpaque(false);
        editorPane.setEditable(false);
        editorPane.addHyperlinkListener(colopediaPanel);

        panel.add(editorPane, "width 95%");

    }

}
