package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.metal.MetalComboBoxUI;
import         javax.swing.plaf.metal.*;
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
public class FreeColComboBoxUI extends MetalComboBoxUI {


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
}
