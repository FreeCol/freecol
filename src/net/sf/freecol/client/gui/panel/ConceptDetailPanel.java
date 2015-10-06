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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.common.i18n.Messages;


/**
 * This panel displays the concepts within the Colopedia.
 */
public class ConceptDetailPanel extends FreeColPanel
    implements ColopediaDetailPanel<String> {

    private static final String id = "colopediaAction."
        + PanelType.CONCEPTS.getKey();

    private static final String[] concepts = {
        "taxes",
        "efficiency",
        "education",
        "fortification",
        "independence",
        "ref",
        "interventionForce"
    };

    private static final Comparator<DefaultMutableTreeNode> nodeComparator
        = Comparator.comparing(tn ->
            ((ColopediaTreeItem)tn.getUserObject()).getText());

    private ColopediaPanel colopediaPanel;


    /**
     * Creates a new instance of this ColopediaDetailPanel.
     *
     * @param colopediaPanel the ColopediaPanel
     */
    public ConceptDetailPanel(FreeColClient freeColClient,
                              ColopediaPanel colopediaPanel) {
        super(freeColClient);

        this.colopediaPanel = colopediaPanel;
    }


    @Override
    public String getName() {
        return Messages.getName(id);
    }

    // Implement ColopediaDetailPanel

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSubTrees(DefaultMutableTreeNode root) {
        DefaultMutableTreeNode node
            = new DefaultMutableTreeNode(new ColopediaTreeItem(this, id,
                    getName(), null));
        List<DefaultMutableTreeNode> nodes = new ArrayList<>();
        for (String concept : concepts) {
            String nodeId = "colopedia.concepts." + concept;
            String nodeName = Messages.getName(nodeId);
            nodes.add(new DefaultMutableTreeNode(new ColopediaTreeItem(this,
                        nodeId, nodeName, null)));
        }
        Collections.sort(nodes, nodeComparator);
        for (DefaultMutableTreeNode n : nodes) {
            node.add(n);
        }
        root.add(node);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildDetail(String id, JPanel panel) {
        if (this.id.equals(id)) return;

        panel.setLayout(new MigLayout("wrap 1, center"));

        JLabel header = Utility.localizedHeaderLabel(Messages.nameKey(id),
            SwingConstants.LEADING, FontLibrary.FontSize.SMALL);
        panel.add(header, "align center, wrap 20");

        JEditorPane editorPane = new JEditorPane("text/html",
            Messages.getDescription(id)) {

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
