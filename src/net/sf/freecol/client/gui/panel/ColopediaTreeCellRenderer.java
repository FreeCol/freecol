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

import java.awt.Color;
import java.awt.Component;
import javax.swing.ImageIcon;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import net.sf.freecol.common.resources.ResourceManager;


/**
 * This TreeCellRenderer is responsible for rendering tree items in
 * the Colopedia.
 */
public class ColopediaTreeCellRenderer extends DefaultTreeCellRenderer {

    /**
     * The constructor makes sure that the backgrounds are transparent.
     */
    public ColopediaTreeCellRenderer() {
        setBackgroundNonSelectionColor(new Color(0,0,0,1));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
        boolean selected, boolean expanded, boolean leaf, int row,
        boolean hasFocus) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;

        super.getTreeCellRendererComponent(tree, value, selected, expanded,
                                           leaf, row, hasFocus);
        ColopediaTreeItem nodeItem = (ColopediaTreeItem)node.getUserObject();
        if (nodeItem.getIcon() != null) {
            setIcon(nodeItem.getIcon());
        } else {
            String key = "image.icon.Colopedia."
                + ((expanded) ? "open" : "closed") + "Section";
            setIcon(new ImageIcon(ResourceManager.getImage(key)));
        }
        return this;
    }
}
