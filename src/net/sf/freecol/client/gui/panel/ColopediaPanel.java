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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.FreeColObject;


/**
 * This panel displays the Colopedia.
 */
public final class ColopediaPanel extends FreeColPanel
    implements HyperlinkListener, TreeSelectionListener {

    private static final Logger logger = Logger.getLogger(ColopediaPanel.class.getName());

    private JPanel listPanel;

    private JPanel detailPanel;

    private JTree tree;

    private Map<String, DefaultMutableTreeNode> nodeMap = new HashMap<>();


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param id The object identifier of the item to select.
     */
    public ColopediaPanel(FreeColClient freeColClient, String id) {
        super(freeColClient, new MigLayout("fill", 
                "[200:]unrelated[550:, grow, fill]", "[][grow, fill][]"));

        add(Utility.localizedHeader("colopedia", false),
            "span, align center");

        listPanel = new MigPanel("ColopediaPanelUI");
        listPanel.setOpaque(true);
        JScrollPane sl = new JScrollPane(listPanel,
                                         JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sl.getVerticalScrollBar().setUnitIncrement(16);
        sl.getViewport().setOpaque(false);
        add(sl);

        detailPanel = new MigPanel("ColopediaPanelUI");
        detailPanel.setOpaque(true);
        JScrollPane detail = new JScrollPane(detailPanel,
                                             JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        detail.getVerticalScrollBar().setUnitIncrement(16);
        detail.getViewport().setOpaque(false);
        add(detail, "grow");

        add(okButton, "newline 20, span, tag ok");

        float scale = getImageLibrary().getScaleFactor();
        getGUI().restoreSavedSize(this, 200 + (int)(scale*850), 200 + (int)(scale*525));
        tree = buildTree();

        select(id);
    }

    /**
     * Creates a new <code>ColopediaPanel</code> instance suitable
     * only for the construction of ColopediaDetailPanels.
     *
     * FIXME: find a more elegant solution.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ColopediaPanel(FreeColClient freeColClient) {
        super(freeColClient);
    }


    /**
     * Builds the JTree which represents the navigation menu and then returns it
     *
     * @return The navigation tree.
     */
    private JTree buildTree() {
        String name = Messages.message("colopedia");
        DefaultMutableTreeNode root
            = new DefaultMutableTreeNode(new ColopediaTreeItem(null, null, name, null));

        FreeColClient fcc = getFreeColClient();
        new TerrainDetailPanel(fcc, this).addSubTrees(root);
        new ResourcesDetailPanel(fcc, this).addSubTrees(root);
        new GoodsDetailPanel(fcc, this).addSubTrees(root);
        new UnitDetailPanel(fcc, this).addSubTrees(root);
        new BuildingDetailPanel(fcc, this).addSubTrees(root);
        new FatherDetailPanel(fcc, this).addSubTrees(root);
        new NationDetailPanel(fcc, this).addSubTrees(root);
        new NationTypeDetailPanel(fcc, this).addSubTrees(root);
        new ConceptDetailPanel(fcc, this).addSubTrees(root);

        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel) {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(
                        (int)(200 * getImageLibrary().getScaleFactor()),
                        super.getPreferredSize().height);
                }
            };
        tree.setRootVisible(false);
        tree.setCellRenderer(new ColopediaTreeCellRenderer());
        tree.setOpaque(false);
        tree.addTreeSelectionListener(this);

        listPanel.add(tree);
        Enumeration allNodes = root.depthFirstEnumeration();
        while (allNodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) allNodes.nextElement();
            ColopediaTreeItem item = (ColopediaTreeItem) node.getUserObject();
            nodeMap.put(item.getId(), node);
        }
        return tree;
    }

    /**
     * This function analyzes a tree selection event and calls the
     * right methods to take care of building the requested unit's
     * details.
     *
     * @param event The incoming <code>TreeSelectionEvent</code>.
     */
    @Override
    public void valueChanged(TreeSelectionEvent event) {
        DefaultMutableTreeNode node
            = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
        if (node != null) {
            showDetails((ColopediaTreeItem)node.getUserObject());
        }
    }

    private void showDetails(ColopediaTreeItem nodeItem) {
        detailPanel.removeAll();
        if (nodeItem.getPanelType() != null && nodeItem.getId() != null) {
            nodeItem.getPanelType().buildDetail(nodeItem.getId(), detailPanel);
        }
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private void select(String id) {
        DefaultMutableTreeNode node = nodeMap.get(id);
        if (node == null) {
            logger.warning("Unable to find node with id '" + id + "'.");
        } else {
            TreePath oldPath = tree.getSelectionPath();
            if (oldPath != null && oldPath.getParentPath() != null) {
                tree.collapsePath(oldPath.getParentPath());
            }
            TreePath newPath = new TreePath(node.getPath());
            tree.scrollPathToVisible(newPath);
            tree.expandPath(newPath);
            showDetails((ColopediaTreeItem) node.getUserObject());
        }
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        HyperlinkEvent.EventType type = e.getEventType();
        if (type == HyperlinkEvent.EventType.ACTIVATED) {
            String[] path = e.getURL().getPath().split("/");
            if (null != path[1]) switch (path[1]) {
                case FreeColObject.ID_ATTRIBUTE_TAG:
                    select(path[2]);
                    break;
                case "action":
                    getFreeColClient().getActionManager().getFreeColAction(path[2])
                            .actionPerformed(null);
                    break;
            }
        }
    }

    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String command = ae.getActionCommand();
        if (OK.equals(command)) {
            getGUI().removeFromCanvas(this);
        } else {
            select(command);
        }
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        removeAll();
        detailPanel = null;
        listPanel = null;
        tree = null;
        nodeMap = null;
    }
}
