package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.metal.MetalScrollPaneUI;
import         javax.swing.plaf.basic.*;
import         javax.swing.plaf.*;
import         javax.swing.*;
import         javax.swing.border.*;
import         java.awt.*;

import java.io.File;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.ImageIcon;



public class FreeColScrollPaneUI extends BasicScrollPaneUI {


    public static ComponentUI createUI(JComponent c) {
        return new FreeColScrollPaneUI();
    }


    public void installUI(JComponent c) {
        super.installUI(c);

        c.setOpaque(false);
    }
}
