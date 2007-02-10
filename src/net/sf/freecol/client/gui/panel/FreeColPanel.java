
package net.sf.freecol.client.gui.panel;

import java.awt.Font;
import java.awt.Image;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import cz.autel.dmi.HIGConstraints;


/**
* Superclass for all panels in FreeCol.
*/
public class FreeColPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(FreeColPanel.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    protected static final HIGConstraints higConst = new HIGConstraints();

    private static final int cancelKeyCode = KeyEvent.VK_ESCAPE;

    // Font to use for text areas
    protected static final Font defaultFont = new Font("Dialog", Font.BOLD, 12);

    // Fonts to use for report headers, etc.
    protected static final Font smallHeaderFont = ((Font) UIManager.get("HeaderFont")).deriveFont(0, 24);
    protected static final Font mediumHeaderFont = ((Font) UIManager.get("HeaderFont")).deriveFont(0, 36);
    protected static final Font bigHeaderFont = ((Font) UIManager.get("HeaderFont")).deriveFont(0, 48);

    // How many columns (em-widths) to use in the text area
    protected static final int columns = 20;

    // The margin to use for HIGLayout
    protected static final int margin = 12;


    /**
    * Default constructor.
    */
    public FreeColPanel() {
        this(new FlowLayout());
    }


    /**
    * Default constructor.
    * @param layout The <code>LayoutManager</code> to be used.
    */
    public FreeColPanel(LayoutManager layout) {
        super(layout);

        setFocusCycleRoot(true);

        Image menuborderN = (Image) UIManager.get("menuborder.n.image");
        Image menuborderNW = (Image) UIManager.get("menuborder.nw.image");
        Image menuborderNE = (Image) UIManager.get("menuborder.ne.image");
        Image menuborderW = (Image) UIManager.get("menuborder.w.image");
        Image menuborderE = (Image) UIManager.get("menuborder.e.image");
        Image menuborderS = (Image) UIManager.get("menuborder.s.image");
        Image menuborderSW = (Image) UIManager.get("menuborder.sw.image");
        Image menuborderSE = (Image) UIManager.get("menuborder.se.image");
        Image menuborderShadowSW = (Image) UIManager.get("menuborder.shadow.sw.image");
        Image menuborderShadowS = (Image) UIManager.get("menuborder.shadow.s.image");
        Image menuborderShadowSE = (Image) UIManager.get("menuborder.shadow.se.image");
        final FreeColImageBorder imageBorder = new FreeColImageBorder(menuborderN, menuborderW, menuborderS, menuborderE, menuborderNW, menuborderNE, menuborderSW, menuborderSE);
        setBorder(BorderFactory.createCompoundBorder(imageBorder, BorderFactory.createEmptyBorder(margin, margin, margin, margin)));
        

        //setBorder( new CompoundBorder(new BevelBorder(BevelBorder.RAISED), new EmptyBorder(5,5,5,5)) );

        // See the message of Ulf Onnen for more information about the presence of this fake mouse listener.
        addMouseListener(new MouseAdapter() {});
    }



    /**
     * Returns a text area with standard settings suitable for use in
     * FreeCol dialogs.
     *
     * @param text The text to display in the text area.
     * @return a text area with standard settings suitable for use in
     * FreeCol dialogs.
     */
    public static JTextArea getDefaultTextArea(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setColumns(columns);
        textArea.setOpaque(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFocusable(false);
        textArea.setFont(defaultFont);
        // necessary because of resizing
        textArea.setSize(textArea.getPreferredSize());
        return textArea;
    }

    /**
     * Returns the default header for panels.
     *
     * @param text a <code>String</code> value
     * @return a <code>JLabel</code> value
     */
    public static JLabel getDefaultHeader(String text) {
        JLabel header = new JLabel(text, JLabel.CENTER);
        header.setFont(bigHeaderFont);
        header.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        return header;
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
     * Registers enter key for a jbutton.
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
