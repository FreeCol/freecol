/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.Border;

import net.sf.freecol.client.gui.Canvas;
import cz.autel.dmi.HIGConstraints;

/**
 * Superclass for all panels in FreeCol.
 */
public class FreeColPanel extends JPanel {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FreeColPanel.class.getName());




    protected static final HIGConstraints higConst = new HIGConstraints();

    private static final int cancelKeyCode = KeyEvent.VK_ESCAPE;

    /**
     * The canvas all panels belong to.
     */
    private Canvas canvas = null;

    // Font to use for text areas
    protected static final Font defaultFont = new Font("Dialog", Font.BOLD, 12);

    // Fonts to use for report headers, etc.
    protected static final Font headerFont = ((Font) UIManager.get("HeaderFont")).deriveFont(0, 12);

    protected static final Font smallHeaderFont = ((Font) UIManager.get("HeaderFont")).deriveFont(0, 24);

    protected static final Font mediumHeaderFont = ((Font) UIManager.get("HeaderFont")).deriveFont(0, 36);

    protected static final Font bigHeaderFont = ((Font) UIManager.get("HeaderFont")).deriveFont(0, 48);

    // How many columns (em-widths) to use in the text area
    protected static final int columns = 20;

    // The margin to use for HIGLayout
    protected static final int margin = 3;

    // The color to use for links
    protected static final Color LINK_COLOR = new Color(122, 109, 82);

    // The borders to use for table cells
    public static final Border TOPCELLBORDER =
        BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 1, 1, LINK_COLOR),
                                           BorderFactory.createEmptyBorder(2, 2, 2, 2));
    public static final Border CELLBORDER = 
        BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, LINK_COLOR),
                                           BorderFactory.createEmptyBorder(2, 2, 2, 2));
    public static final Border LEFTCELLBORDER = 
        BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, LINK_COLOR),
                                           BorderFactory.createEmptyBorder(2, 2, 2, 2));
    public static final Border TOPLEFTCELLBORDER = 
        BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, LINK_COLOR),
                                           BorderFactory.createEmptyBorder(2, 2, 2, 2));

    protected boolean editable = true;


    /**
     * Default constructor.
     */
    public FreeColPanel() {
        this(null, new FlowLayout());
    }

    /**
     * Constructor.
     */
    public FreeColPanel(Canvas parent) {
        this(parent, new FlowLayout());
    }

    /**
     * Constructor.
     */
    public FreeColPanel(LayoutManager layout) {
        this(null, layout);
    }

    /**
     * Default constructor.
     * 
     * @param parent The <code>Canvas</code> all panels belong to.
     * @param layout The <code>LayoutManager</code> to be used.
     */
    public FreeColPanel(Canvas parent, LayoutManager layout) {
        super(layout);

        this.canvas = parent;
        setFocusCycleRoot(true);

        Image menuborderN = (Image) UIManager.get("menuborder.n.image");
        Image menuborderNW = (Image) UIManager.get("menuborder.nw.image");
        Image menuborderNE = (Image) UIManager.get("menuborder.ne.image");
        Image menuborderW = (Image) UIManager.get("menuborder.w.image");
        Image menuborderE = (Image) UIManager.get("menuborder.e.image");
        Image menuborderS = (Image) UIManager.get("menuborder.s.image");
        Image menuborderSW = (Image) UIManager.get("menuborder.sw.image");
        Image menuborderSE = (Image) UIManager.get("menuborder.se.image");
        final FreeColImageBorder imageBorder = new FreeColImageBorder(menuborderN, menuborderW, menuborderS,
                menuborderE, menuborderNW, menuborderNE, menuborderSW, menuborderSE);
        setBorder(BorderFactory.createCompoundBorder(imageBorder,
                BorderFactory.createEmptyBorder(margin, margin,margin,margin)));

        // setBorder( new CompoundBorder(new BevelBorder(BevelBorder.RAISED),
        // new EmptyBorder(5,5,5,5)) );

        // See the message of Ulf Onnen for more information about the presence
        // of this fake mouse listener.
        addMouseListener(new MouseAdapter() {
        });
    }

    /**
     * Get the <code>Canvas</code> value.
     * 
     * @return a <code>Canvas</code> value
     */
    public final Canvas getCanvas() {
        return canvas;
    }

    /**
     * Checks if this panel is editable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Returns a text area with standard settings suitable for use in FreeCol
     * dialogs.
     * 
     * @param text The text to display in the text area.
     * @return a text area with standard settings suitable for use in FreeCol
     *         dialogs.
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

    public void setCancelComponent(AbstractButton cancelButton) {
        if (cancelButton == null) {
            throw new NullPointerException();
        }
        
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(cancelKeyCode, 0, true), "release");
                
        Action cancelAction = cancelButton.getAction();
        getActionMap().put("release", cancelAction);
    }

    /**
     * Registers enter key for a jbutton.
     * 
     * @param button
     */
    public static void enterPressesWhenFocused(JButton button) {
        button.registerKeyboardAction(
                button.getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false)), KeyStroke
                        .getKeyStroke(KeyEvent.VK_ENTER, 0, false), JComponent.WHEN_FOCUSED);

        button.registerKeyboardAction(button.getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true)),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), JComponent.WHEN_FOCUSED);
    }

}
