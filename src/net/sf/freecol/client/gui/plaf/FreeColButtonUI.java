package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.metal.MetalButtonUI;
import         javax.swing.plaf.*;
import         javax.swing.*;
import         java.awt.*;


/**
* Sets the default opaque attribute to <i>false</i> and 
* uses a 10% black shading on the {@link #paintButtonPressed}.
*/
public class FreeColButtonUI extends MetalButtonUI {


    public static ComponentUI createUI(JComponent c) {
        return new FreeColButtonUI();
    }

    
    public void installUI(JComponent c) {
        super.installUI(c);
        
        c.setOpaque(false);
    }
    
    
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        if (b.isContentAreaFilled()) {
            Graphics2D g2d = (Graphics2D) g;
            Dimension size = b.getSize();
            Composite oldComposite = g2d.getComposite();
            Color oldColor = g2d.getColor();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, size.width, size.height);
            g2d.setComposite(oldComposite);
            g2d.setColor(oldColor);

        }
    }
}
