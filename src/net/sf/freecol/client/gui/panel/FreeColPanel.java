
package net.sf.freecol.client.gui.panel;

import java.util.logging.Logger;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.BevelBorder;


/**
* Superclass for all panels in FreeCol.
*/
public class FreeColPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(FreeColPanel.class.getName());

    private static final int cancelKeyCode = KeyEvent.VK_ESCAPE;
    
    /**
    * Default constructor.
    */
    public FreeColPanel() {
        this(new FlowLayout());
    }
    
    
    /**
    * Default constructor.
    */
    public FreeColPanel(LayoutManager layout) {    
        super(layout);

        setFocusCycleRoot(true);

        try {
            BevelBorder border = new BevelBorder(BevelBorder.RAISED);
            setBorder(border);
        } catch(Exception e) {}
    }

    
    public void setCancelComponent(AbstractButton c) {
        if (c == null) {
            throw new NullPointerException();
        }

        InputMap inputMap = new ComponentInputMap(c);
        inputMap.put(KeyStroke.getKeyStroke(cancelKeyCode, 0, false), "pressed");
        inputMap.put(KeyStroke.getKeyStroke(cancelKeyCode, 0, true), "released");
        SwingUtilities.replaceUIInputMap(c, JComponent.WHEN_IN_FOCUSED_WINDOW, inputMap);
    }
}