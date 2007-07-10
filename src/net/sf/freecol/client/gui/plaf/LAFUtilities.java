package net.sf.freecol.client.gui.plaf;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

/**
 * Utility methods for Look-and-Feel classes.
 */
public final class LAFUtilities {
    
    private static final int AA_TEXT_SIZE = 16;
    
    /**
     * Modifies the given graphics object with any relevant
     * {@link JComponent#getClientProperty(Object) client property}
     * from the given component.
     * 
     * Currently only <code>RenderingHints.KEY_TEXT_ANTIALIASING</code>
     * is supported. Antialiasing is enabled explicitly if the text size
     * is larger or equal to 16.
     * 
     * @param g The graphics object to be updated.
     * @param c The component to get the properties from.
     */
    public static void setProperties(Graphics g, JComponent c) {
        if (c.getFont().getSize() >= AA_TEXT_SIZE) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        Object textAA = c.getClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING);
        if (textAA != null) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAA);
        }
    }
}
