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
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Modifier;

/**
 * Superclass for all panels in FreeCol.
 */
public class FreeColPanel extends JPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FreeColPanel.class.getName());

    public static final Insets emptyMargin = new Insets(0,0,0,0);

    private static final int cancelKeyCode = KeyEvent.VK_ESCAPE;

    // The decimal format to use for Modifiers
    private static final DecimalFormat modifierFormat = new DecimalFormat("0.00");


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
    protected static final int COLUMNS = 20;

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
     * Constructor.
     */
    public FreeColPanel(Canvas parent) {
        this(parent, new FlowLayout());
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

        Image menuborderN = ResourceManager.getImage("menuborder.n.image");
        Image menuborderNW = ResourceManager.getImage("menuborder.nw.image");
        Image menuborderNE = ResourceManager.getImage("menuborder.ne.image");
        Image menuborderW = ResourceManager.getImage("menuborder.w.image");
        Image menuborderE = ResourceManager.getImage("menuborder.e.image");
        Image menuborderS = ResourceManager.getImage("menuborder.s.image");
        Image menuborderSW = ResourceManager.getImage("menuborder.sw.image");
        Image menuborderSE = ResourceManager.getImage("menuborder.se.image");
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
     * Returns the ImageLibrary.
     *
     * @return the ImageLibrary.
     */
    public ImageLibrary getLibrary() {
        return canvas.getGUI().getImageLibrary();
    }

    /**
     * Describe <code>getClient</code> method here.
     *
     * @return a <code>FreeColClient</code> value
     */
    public FreeColClient getClient() {
        return canvas.getClient();
    }

    /**
     * Describe <code>getGame</code> method here.
     *
     * @return a <code>Game</code> value
     */
    public Game getGame() {
        return canvas.getClient().getGame();
    }

    /**
     * Checks if this panel is editable
     */
    public boolean isEditable() {
        return editable;
    }

    public static JTextPane getDefaultTextPane() {
        JTextPane textPane = new JTextPane();
        textPane.setOpaque(false);
        textPane.setEditable(false);

        StyledDocument doc = textPane.getStyledDocument();
        //Initialize some styles.
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        
        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "Dialog");
        StyleConstants.setBold(def, true);
        StyleConstants.setFontSize(def, 12);

        Style buttonStyle = doc.addStyle("button", regular);
        StyleConstants.setForeground(buttonStyle, LINK_COLOR);

        Style right = doc.addStyle("right", regular);
        StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);

        return textPane;
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
        return getDefaultTextArea(text, COLUMNS);
    }
    /**
     * Returns a text area with standard settings suitable for use in FreeCol
     * dialogs.
     * 
     * @param text The text to display in the text area.
     * @param columns an <code>int</code> value
     * @return a text area with standard settings suitable for use in FreeCol
     *         dialogs.
     */
    public static JTextArea getDefaultTextArea(String text, int columns) {
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

    public static JButton getLinkButton(String text, Icon icon, String action) {
        JButton button = new JButton(text, icon);
        button.setMargin(emptyMargin);
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setAlignmentY(0.8f);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setActionCommand(action);
        return button;
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

    public static final DecimalFormat getModifierFormat() {
        return modifierFormat;
    }

    public Set<Modifier> sortModifiers(Set<Modifier> result) {
        EnumMap<Modifier.Type, List<Modifier>> modifierMap =
            new EnumMap<Modifier.Type, List<Modifier>>(Modifier.Type.class);
        for (Modifier.Type type : Modifier.Type.values()) {
            modifierMap.put(type, new ArrayList<Modifier>());
        }
        for (Modifier modifier : result) {
            modifierMap.get(modifier.getType()).add(modifier);
        }
        Set<Modifier> sortedResult = new LinkedHashSet<Modifier>();
        for (Modifier.Type type : Modifier.Type.values()) {
            sortedResult.addAll(modifierMap.get(type));
        }
        return sortedResult;
    }

}
