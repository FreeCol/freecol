package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.basic.*;
import         javax.swing.plaf.*;
import         javax.swing.*;


/**
* Sets the default opaque attribute to <i>false</i>.
*/
public class FreeColCheckBoxUI extends BasicCheckBoxUI {


    public static ComponentUI createUI(JComponent c) {
        return new FreeColCheckBoxUI();
    }

    
    public void installUI(JComponent c) {
        super.installUI(c);
        
        c.setOpaque(false);
    }
}
