package net.sf.freecol.client.gui.plaf;

import javax.swing.JComponent;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalComboBoxUI;


/**
* Sets the default opaque attribute to <i>false</i>.
*/
public class FreeColComboBoxUI extends MetalComboBoxUI {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static ComponentUI createUI(JComponent c) {
        return new FreeColComboBoxUI();
    }


    public void installUI(JComponent c) {
        super.installUI(c);

        c.setOpaque(false);
    }


    /*protected  JButton createArrowButton() {
        JButton button = super.createArrowButton();

        // TODO: Make button prettier?

        return button;
    }*/
    
    protected ListCellRenderer createRenderer() {
        return new FreeColComboBoxRenderer.UIResource();
    }
}
