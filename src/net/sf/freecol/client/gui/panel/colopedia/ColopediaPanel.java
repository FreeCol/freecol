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

package net.sf.freecol.client.gui.panel.colopedia;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
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
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.client.gui.panel.Utility;
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
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param id The object identifier of the item to select.
     */
    public ColopediaPanel(FreeColClient freeColClient, String id) {
        super(freeColClient, "ColopediaPanelUI",
              new MigLayout("fill", "[grow, fill]",
                            "[][grow, fill][]"));

        add(Utility.localizedHeader("colopedia", Utility.FONTSPEC_TITLE),
            "span, align center");

        listPanel = new MigPanel(new MigLayout("fill"));
        listPanel.setOpaque(true);
        
        tree = buildTree();
        listPanel.add(tree, "grow");
        
        JScrollPane sl = new JScrollPane(listPanel,
                                         JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            @Override
            public Dimension getPreferredSize() {
                final Dimension preferredSize = listPanel.getPreferredSize();
                return new Dimension(preferredSize.width + 32, preferredSize.height);
            }
        };

        sl.getVerticalScrollBar().setUnitIncrement(16);
        sl.getViewport().setOpaque(false);

        detailPanel = new MigPanel("ColopediaPanelUI");
        detailPanel.setOpaque(true);
        JScrollPane detail = new JScrollPane(detailPanel,
                                             JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        detail.getVerticalScrollBar().setUnitIncrement(16);
        detail.getViewport().setOpaque(false);
        
        select(id);

        final int width = getImageLibrary().scaleInt(1050);
        final int height = getImageLibrary().scaleInt(725);
        getGUI().restoreSavedSize(this, new Dimension(width, height));
        
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sl, detail);

        add(splitPane, "grow");
        add(okButton, "newline, span, tag ok");
        
        setEscapeAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                okButton.doClick();
            }
        });
    }

    /**
     * Creates a new {@code ColopediaPanel} instance suitable
     * only for the construction of ColopediaDetailPanels.
     *
     * FIXME: find a more elegant solution.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
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
        tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setCellRenderer(new ColopediaTreeCellRenderer(this, getImageLibrary()));
        tree.setOpaque(false);
        tree.addTreeSelectionListener(this);

        Enumeration allNodes = root.depthFirstEnumeration();
        while (allNodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) allNodes.nextElement();
            ColopediaTreeItem item = (ColopediaTreeItem) node.getUserObject();
            nodeMap.put(item.getId(), node);
        }
        return tree;
    }
    
    /**
     * Gets the preferred size for the list item images in the colopedia tree.
     *
     * @return The {@code Dimension} to use.
     */
    public Dimension getListItemIconSize() {
        final int width = getImageLibrary().scaleInt(ImageLibrary.ICON_SIZE.width * 3 / 2);
        final int height = getImageLibrary().scaleInt(ImageLibrary.ICON_SIZE.height);
        
        return new Dimension(width, height);
    }

    /**
     * This function analyzes a tree selection event and calls the
     * right methods to take care of building the requested unit's
     * details.
     *
     * @param event The incoming {@code TreeSelectionEvent}.
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
            if (null != path[1]) {
                switch (path[1]) {
                case FreeColObject.ID_ATTRIBUTE_TAG:
                    select(path[2]);
                    break;
                case "action":
                    getFreeColClient().getActionManager()
                        .getFreeColAction(path[2]).actionPerformed(null);
                    break;
                default:
                    break;
                }
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
            getGUI().removeComponent(this);
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
