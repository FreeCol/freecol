
package net.sf.freecol.client.gui.panel;

import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import cz.autel.dmi.HIGConstraints;


/**
* Superclass for all panels in FreeCol.
*/
public class FreeColPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(FreeColPanel.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    protected static final HIGConstraints higConst = new HIGConstraints();

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

        setBorder( new CompoundBorder(new BevelBorder(BevelBorder.RAISED), new EmptyBorder(5,5,5,5)) );

        // See the message of Ulf Onnen for more information about the presence of this fake mouse listener.
        addMouseListener(new MouseAdapter() {});
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

    /**
     * Registers enter key for a jbutton
     * @param button
     */
    public static void enterPressesWhenFocused(JButton button) {
        button.registerKeyboardAction(
            button.getActionForKeyStroke(
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false)),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false),
                JComponent.WHEN_FOCUSED);

        button.registerKeyboardAction(
            button.getActionForKeyStroke(
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true)),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true),
                JComponent.WHEN_FOCUSED);
    }

}
