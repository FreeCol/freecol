/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.resources.ResourceManager;

/**
 * Superclass for all panels in FreeCol.
 */
public abstract class FreeColPanel extends JPanel implements ActionListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FreeColPanel.class.getName());

    protected static final String OK = "OK";
    protected static final String HELP = "HELP";

    public static final Insets emptyMargin = new Insets(0,0,0,0);

    private static final int cancelKeyCode = KeyEvent.VK_ESCAPE;

    // The decimal format to use for Modifiers
    protected static final DecimalFormat modifierFormat = new DecimalFormat("0.00");

    /**
     * The canvas all panels belong to.
     */
    private Canvas canvas = null;

    // Font to use for text areas
    protected static final Font defaultFont = ResourceManager.getFont("NormalFont", 13f);

    // Fonts to use for report headers, etc.
    protected static final Font  smallHeaderFont = ResourceManager.getFont("HeaderFont", 24f);
    protected static final Font mediumHeaderFont = ResourceManager.getFont("HeaderFont", 36f);
    protected static final Font    bigHeaderFont = ResourceManager.getFont("HeaderFont", 48f);

    // How many columns (em-widths) to use in the text area
    protected static final int COLUMNS = 20;

    // The margin to use.
    protected static final int margin = 3;

    // The color to use for things the player probably shouldn't do
    protected static final Color WARNING_COLOR
        = ResourceManager.getColor("lookAndFeel.warning.color");

    // The color to use for links
    protected static final Color LINK_COLOR
        = ResourceManager.getColor("lookAndFeel.link.color");

    // The color to use for borders
    protected static final Color BORDER_COLOR
        = ResourceManager.getColor("lookAndFeel.border.color");

    // The borders to use for table cells
    public static final Border TOPCELLBORDER =
        BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 1, 1, BORDER_COLOR),
                                           BorderFactory.createEmptyBorder(2, 2, 2, 2));
    public static final Border CELLBORDER =
        BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER_COLOR),
                                           BorderFactory.createEmptyBorder(2, 2, 2, 2));
    public static final Border LEFTCELLBORDER =
        BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, BORDER_COLOR),
                                           BorderFactory.createEmptyBorder(2, 2, 2, 2));
    public static final Border TOPLEFTCELLBORDER =
        BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, BORDER_COLOR),
                                           BorderFactory.createEmptyBorder(2, 2, 2, 2));


    protected boolean editable = true;

    protected JButton okButton = new JButton(Messages.message("ok"));

    private FreeColClient freeColClient;

    protected static StyleContext styleContext = new StyleContext();

    static {
        Style defaultStyle = StyleContext.getDefaultStyleContext()
            .getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = styleContext.addStyle("regular", defaultStyle);
        StyleConstants.setFontFamily(regular, "NormalFont");
        StyleConstants.setFontSize(regular, 13);

        Style buttonStyle = styleContext.addStyle("button", regular);
        StyleConstants.setForeground(buttonStyle, LINK_COLOR);

        Style right = styleContext.addStyle("right", regular);
        StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);
    }

    /**
     * Constructor.
     *
     * @param parent <code>Canvas</code>
     */
    public FreeColPanel(FreeColClient freeColClient, Canvas parent) {
        this(freeColClient, parent, new FlowLayout());
    }

    /**
     * Default constructor.
     *
     * @param parent The <code>Canvas</code> all panels belong to.
     * @param layout The <code>LayoutManager</code> to be used.
     */
    public FreeColPanel(FreeColClient freeColClient, Canvas parent, LayoutManager layout) {
        super(layout);

        this.freeColClient = freeColClient;
        this.canvas = parent;
        setFocusCycleRoot(true);

        setBorder(FreeColImageBorder.imageBorder);

        // See the message of Ulf Onnen for more information about the presence
        // of this fake mouse listener.
        addMouseListener(new MouseAdapter() {
        });

        okButton.setActionCommand(OK);
        okButton.addActionListener(this);
        enterPressesWhenFocused(okButton);
        setCancelComponent(okButton);

    }


    /**
     * Return the client's <code>ClientOptions</code>.
     *
     * @return a <code>ClientOptions</code> value
     */
    protected ClientOptions getClientOptions() {
        return freeColClient.getClientOptions();
    }

    /**
     * Return the player's Colonies, sorted according to player's
     * <code>ClientOptions</code>.
     *
     * @return a sorted List of Colonies
     */
    protected List<Colony> getSortedColonies() {
        return freeColClient.getClientOptions()
            .getSortedColonies(getMyPlayer());
    }

    /**
     * Return an <code>int</code> associated with the name of the
     * panel's class plus the given key from the saved ClientOptions.
     *
     * @param key a <code>String</code> value
     * @return an <code>int</code> value
     */
    private int getInteger(String key) {
        return freeColClient.getClientOptions()
            .getInteger(getClass().getName() + key);
    }

    /**
     * Returns the saved size of this panel, null by default.
     *
     * @return a <code>Dimension</code> value
     */
    public final Dimension getSavedSize() {
        try {
            return new Dimension(getInteger(".w"), getInteger(".h"));
        } catch(Exception e) {
            return null;
        }
    }


    /**
     * Save an <code>int</code> value to the saved ClientOptions,
     * using the name of the panel's class plus the given key as and
     * ID.
     *
     * @param key a <code>String</code> value
     * @param value an <code>int</code> value
     */
    private void saveInteger(String key, int value) {
        Option o = freeColClient.getClientOptions()
            .getOption(getClass().getName() + key);
        if (o == null) {
            IntegerOption io = new IntegerOption(getClass().getName() + key);
            io.setValue(value);
            freeColClient.getClientOptions().add(io);
        } else if (o instanceof IntegerOption) {
            ((IntegerOption) o).setValue(value);
        }
    }

    /**
     * Set preferred size to saved size, or to the given
     * <code>Dimension</code> if no saved size was found. Call this
     * method in the constructor of a FreeColPanel in order to
     * remember its size and position.
     *
     * @param d a <code>Dimension</code> value
     */
    protected void restoreSavedSize(Dimension d) {
        Dimension size = getSavedSize();
        if (size == null) {
            size = d;
            saveSize(size);
        }
        if (!getPreferredSize().equals(size)) {
            setPreferredSize(size);
        }
    }

    /**
     * Set preferred size to saved size, or to [w, h] if no saved size
     * was found. Call this method in the constructor of a
     * FreeColPanel in order to remember its size and position.
     *
     * @param w an <code>int</code> value
     * @param h an <code>int</code> value
     */
    protected void restoreSavedSize(int w, int h) {
        restoreSavedSize(new Dimension(w, h));
    }


    /**
     * Returns the saved position of this panel, null by default.
     *
     * @return a <code>Point</code> value
     */
    public Point getSavedPosition() {
        try {
            return new Point(getInteger(".x"), getInteger(".y"));
        } catch(Exception e) {
            return null;
        }
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
        return canvas.getImageLibrary();
    }

    /**
     * Gets the FreeColClient from the canvas.
     *
     * @return A current <code>FreeColClient</code>.
     */
    public FreeColClient getFreeColClient() {
        return canvas.getFreeColClient();
    }

    /**
     * Describe <code>getGame</code> method here.
     *
     * @return a <code>Game</code> value
     */
    public Game getGame() {
        return canvas.getFreeColClient().getGame();
    }

    /**
     * Describe <code>getSpecification</code> method here.
     *
     * @return a <code>Specification</code> value
     */
    public Specification getSpecification() {
        return canvas.getSpecification();
    }

    /**
     * Describe <code>getController</code> method here.
     *
     * @return an <code>InGameController</code> value
     */
    public InGameController getController() {
        return canvas.getFreeColClient().getInGameController();
    }

    /**
     * Describe <code>getMyPlayer</code> method here.
     *
     * @return a <code>Player</code> value
     */
    public Player getMyPlayer() {
        return canvas.getFreeColClient().getMyPlayer();
    }

    /**
     * Returns the <code>Turn</code>s during which a Player's
     * FoundingFathers were elected to the Continental Congress
     *
     * @return a <code>Turn</code> value
     */
    public Map<String, Turn> getElectionTurns() {
        Map<String, Turn> result = new HashMap<String, Turn>();
        if (!getMyPlayer().getFathers().isEmpty()) {
            for (HistoryEvent event : getMyPlayer().getHistory()) {
                if (event.getEventType() == HistoryEvent.EventType.FOUNDING_FATHER) {
                    result.put(event.getReplacement("%father%").getId(), event.getTurn());
                }
            }
        }
        return result;
    }



    /**
     * Checks if this panel is editable
     * @return boolean
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Set the <code>Editable</code> value.
     *
     * @param newEditable boolean, the new Editable value
     */
    public void setEditable(boolean newEditable) {
        this.editable = newEditable;
    }

    /**
     * The OK button requests focus.
     *
     */
    @Override
    public void requestFocus() {
        okButton.requestFocus();
    }

    /**
     * Get a JTextPane with default styles.
     *
     * @return a <code>JTextPane</code> value
     */
    public static JTextPane getDefaultTextPane() {
        return getDefaultTextPane(null);
    }

    /**
     * Get a JTextPane with default styles and given text.
     *
     * @param text a <code>String</code> value
     * @return a <code>JTextPane</code> value
     */
    public static JTextPane getDefaultTextPane(String text) {

        DefaultStyledDocument document = new DefaultStyledDocument(styleContext) {
             @Override
             public Font getFont(AttributeSet attr) {
                 Font font = ResourceManager.getFont(StyleConstants.getFontFamily(attr),
                                                     StyleConstants.getFontSize(attr));
                 if (font == null) {
                     return super.getFont(attr);
                 } else {
                     int fontStyle = Font.PLAIN;
                     if (StyleConstants.isBold(attr)) fontStyle |= Font.BOLD;
                     if (StyleConstants.isItalic(attr)) fontStyle |= Font.ITALIC;
                     return (fontStyle == Font.PLAIN) ? font : font.deriveFont(fontStyle);
                 }
             }
         };

        JTextPane textPane = new JTextPane(document);
        textPane.setOpaque(false);
        textPane.setEditable(false);
        textPane.setLogicalStyle(styleContext.getStyle("regular"));

        textPane.setText(text);
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

    /**
     * Return a button suitable for linking to another panel
     * (e.g. ColopediaPanel).
     *
     * @param text a <code>String</code> value
     * @param icon an <code>Icon</code> value
     * @param action a <code>String</code> value
     * @return a <code>JButton</code> value
     */
    public static JButton getLinkButton(String text, Icon icon, String action) {
        JButton button = new JButton(text, icon);
        button.setMargin(emptyMargin);
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setAlignmentY(0.8f);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setActionCommand(action);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public JButton createColonyButton(Colony colony) {
        JButton button = getLinkButton(colony.getName(), null, colony.getId());
        button.addActionListener(this);
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

    /**
     * Return a JLabel with Messages.message(key) as text.
     *
     * @param key a <code>String</code> value
     * @return a <code>JLabel</code> value
     */
    public JLabel localizedLabel(String key) {
        return new JLabel(Messages.message(key));
    }

    /**
     * Return a JLabel with Messages.localize(template) as text.
     *
     * @param template a <code>StringTemplate</code> value
     * @return a <code>JLabel</code> value
     */
    public JLabel localizedLabel(StringTemplate template) {
        return new JLabel(Messages.message(template));
    }


    /**
     * Make the given button the CANCEL button.
     *
     * @param cancelButton an <code>AbstractButton</code> value
     */
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
     * Registers enter key for a JButton.
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

    /**
     * Returns the default modifier value format.
     *
     * @return a <code>DecimalFormat</code> value
     */
    public static final DecimalFormat getModifierFormat() {
        return modifierFormat;
    }

    /**
     * Sort the given modifiers according to type.
     *
     * @param result Set of <code>Modifier</code>
     * @return a sorted Set of <code>Modifier</code>
     */
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

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     *
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals(OK)) {
            getCanvas().remove(this);
        }
    }

    /**
     * Notify this panel that it is being removed from the
     * canvas. Saves the current size and position of the panel to the
     * ClientOptions, which are included in the savegame file.
     *
     * @see Canvas#remove(Component)
     */
    public void notifyClose() {
        firePropertyChange("closing", false, true);
        Component frame = SwingUtilities.getAncestorOfClass(JInternalFrame.class, this);
        if (frame != null) {
            saveInteger(".x", frame.getLocation().x);
            saveInteger(".y", frame.getLocation().y);
        }
        saveSize();
    }

    /**
     * Save the current size of the panel.
     *
     */
    private void saveSize() {
        saveSize(getSize());
    }

    /**
     * Save the given Dimension as size of the panel.
     *
     * @param size a <code>Dimension</code> value
     */
    private void saveSize(Dimension size) {
        saveInteger(".w", size.width);
        saveInteger(".h", size.height);
    }




    /**
     * Add a routine to be called when this panel closes.
     * Triggered by notifyClose() above.
     *
     * @param runnable Some code to run on close.
     */
    public void addClosingCallback(final Runnable runnable) {
        final FreeColPanel fcp = this;
        addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    if ("closing".equals(e.getPropertyName())) {
                        runnable.run();
                        fcp.removePropertyChangeListener(this);
                    }
                }
            });
    }

    /**
     * Creates a <code>MouseListener</code> which forwards events
     * to the given <code>Component</code>.
     *
     * @param c The <code>Component</code> the events should be forwarded to.
     * @return <code>MouseListener</code>
     */
    public static MouseListener createEventForwardingMouseListener(final Component c) {
        final MouseListener ml = new MouseListener() {
            private void forward(MouseEvent e) {
                c.dispatchEvent(javax.swing.SwingUtilities.convertMouseEvent(e.getComponent(), e, c));
            }

            public void mouseClicked(MouseEvent e) {
                forward(e);
            }
            public void mouseEntered(MouseEvent e) {
                forward(e);
            }
            public void mouseExited(MouseEvent e) {
                forward(e);
            }
            public void mousePressed(MouseEvent e) {
                forward(e);
            }
            public void mouseReleased(MouseEvent e) {
                forward(e);
            }
        };
        return ml;
    }

    /**
     * Creates a <code>MouseMotionListener</code> which forwards events
     * to the given <code>Component</code>.
     *
     * @param c The <code>Component</code> the events should be forwarded to
     * @return <code>MouseMotionListener</code>
     */
    public static MouseMotionListener createEventForwardingMouseMotionListener(final Component c) {
        final MouseMotionListener ml = new MouseMotionListener() {
            private void forward(MouseEvent e) {
                c.dispatchEvent(javax.swing.SwingUtilities.convertMouseEvent(e.getComponent(), e, c));
            }

            public void mouseDragged(MouseEvent e) {
                forward(e);
            }
            public void mouseMoved(MouseEvent e) {
                forward(e);
            }
        };
        return ml;
    }
}
