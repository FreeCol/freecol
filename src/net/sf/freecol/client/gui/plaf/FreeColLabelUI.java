package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.metal.MetalLabelUI;
import         javax.swing.plaf.*;
import         javax.swing.*;


/**
* Sets the default opaque attribute to <i>false</i>.
*/
public class FreeColLabelUI extends MetalLabelUI {


    public static ComponentUI createUI(JComponent c) {
        return new FreeColLabelUI();
    }


    public void installUI(JComponent c) {
        super.installUI(c);

        c.setOpaque(false);
    }


}
