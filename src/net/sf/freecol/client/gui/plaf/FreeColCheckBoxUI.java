package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.basic.*;
import         javax.swing.plaf.*;
import         javax.swing.*;
import         java.awt.*;

import java.io.File;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.ImageIcon;


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
