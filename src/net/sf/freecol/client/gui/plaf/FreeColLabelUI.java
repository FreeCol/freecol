package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.metal.MetalLabelUI;
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
public class FreeColLabelUI extends MetalLabelUI {


    public static ComponentUI createUI(JComponent c) {
        return new FreeColLabelUI();
    }


    public void installUI(JComponent c) {
        super.installUI(c);

        c.setOpaque(false);
    }


}
