package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.basic.*;
import         javax.swing.plaf.*;
import         javax.swing.*;



public class FreeColScrollPaneUI extends BasicScrollPaneUI {


    public static ComponentUI createUI(JComponent c) {
        return new FreeColScrollPaneUI();
    }


    public void installUI(JComponent c) {
        super.installUI(c);

        c.setOpaque(false);
    }
}
