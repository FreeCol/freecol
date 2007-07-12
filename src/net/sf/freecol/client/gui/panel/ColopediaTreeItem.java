package net.sf.freecol.client.gui.panel;

import javax.swing.*;

/**
 * TreeCellItems are used to represent the name and icon of a node in the Colopedia's tree.
 */
class ColopediaTreeItem {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String REVISION = "$Revision$";
    
    private String text;
    private ImageIcon icon;

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param text The name of the item.
     * @param icon The icon of the item.
     */
    ColopediaTreeItem(String text, ImageIcon icon) {
        this.text = text;
        this.icon = icon;
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
