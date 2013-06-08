/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import javax.swing.ImageIcon;


/**
 * TreeCellItems are used to represent the name and icon of a node in
 * the Colopedia's tree.
 */
class ColopediaTreeItem {

    private ColopediaDetailPanel panelType;
    private String id;
    private String text;
    private ImageIcon icon;

    /**
     * The default constructor for a ColopediaTreeItem that
     * corresponds to a leaf node.
     *
     * @param panelType a <code>ColopediaDetailPanel</code> value
     * @param id The object identifier.
     * @param text a <code>String</code> value
     * @param icon an <code>ImageIcon</code> value
     */
    ColopediaTreeItem(ColopediaDetailPanel panelType, String id, String text, ImageIcon icon) {
        this.panelType = panelType;
        this.id = id;
        this.text = text;
        this.icon = icon;
    }

    /**
     * Returns the panel type this item belongs to.
     *
     * @return the panel type this item belongs to.
     */
    public ColopediaDetailPanel getPanelType() {
        return panelType;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return text;
    }

    /**
     * Returns the item's icon.
     *
     * @return the item's icon.
     */
    public ImageIcon getIcon() {
        return icon;
    }
}
