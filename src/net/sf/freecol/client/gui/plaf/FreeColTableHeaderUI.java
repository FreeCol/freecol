package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.basic.*;
import         javax.swing.plaf.*;
import         javax.swing.*;
import         javax.swing.table.*;



public class FreeColTableHeaderUI extends BasicTableHeaderUI {


    public static ComponentUI createUI(JComponent c) {
        return new FreeColTableHeaderUI();
    }

    
    public void installUI(JComponent c) {
        super.installUI(c);

        JTableHeader j = (JTableHeader) c;
        j.setOpaque(false);

        DefaultTableCellRenderer dtcr = (DefaultTableCellRenderer) j.getDefaultRenderer();
        dtcr.setOpaque(false);
    }

}
