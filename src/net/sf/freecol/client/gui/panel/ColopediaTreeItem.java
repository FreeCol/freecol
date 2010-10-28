/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import net.sf.freecol.client.gui.action.ColopediaAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ColopediaPanel.PanelType;
import net.sf.freecol.common.model.FreeColGameObjectType;

/**
 * TreeCellItems are used to represent the name and icon of a node in
 * the Colopedia's tree.
 */
class ColopediaTreeItem {

    private PanelType panelType;
    private FreeColGameObjectType objectType;
    private String text;
    private ImageIcon icon;

    /**
     * The constructor that will add the items to this panel.
     *
     * @param panelType The panel type.
     */
    ColopediaTreeItem(PanelType panelType) {
        this.panelType = panelType;
        this.text = Messages.message(ColopediaAction.id + "." + panelType + ".name");
    }

    /**
     * The constructor that will add the items to this panel.
     *
     * @param panelType The panel type.
     * @param text The name of the item.
     */
    ColopediaTreeItem(PanelType panelType, String text) {
        this.panelType = panelType;
        this.text = text;
    }

    /**
     * The constructor that will add the items to this panel.
     *
     * @param objectType The type represented by this item.
     * @param text The name of the item.
     * @param icon The icon of the item.
     */
    ColopediaTreeItem(FreeColGameObjectType objectType, String text, ImageIcon icon) {
        this.objectType = objectType;
        this.text = text;
        this.icon = icon;
    }

    /**
     * Returns the type this item represents.
     *
     * @return the type this item represents.
     */
    public FreeColGameObjectType getFreeColGameObjectType() {
        return objectType;
    }

    /**
     * Returns the panel type this item belongs to.
     *
     * @return the panel type this item belongs to.
     */
    public PanelType getPanelType() {
        return panelType;
    }

    /**
     * Returns the item's name.
     *
     * @return The item's name
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
