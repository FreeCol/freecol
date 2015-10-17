/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Named;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * Static utilities for panels and dialogs.
 */
public final class Utility {


    /** The color to use for borders. */
    public static final Color BORDER_COLOR
        = ResourceManager.getColor("color.border.LookAndFeel");

    /** The color to use for links. */
    public static final Color LINK_COLOR
        = ResourceManager.getColor("color.link.LookAndFeel");

    /** The color to use for things the player probably should not do. */
    public static final Color WARNING_COLOR
        = ResourceManager.getColor("color.warning.LookAndFeel");


    /** Useful static borders. */
    public static final Border TRIVIAL_LINE_BORDER
        = BorderFactory.createLineBorder(BORDER_COLOR);

    public static final Border BEVEL_BORDER
        = BorderFactory.createBevelBorder(BevelBorder.LOWERED);

    public static final Border COLOR_CELL_BORDER = BorderFactory
        .createCompoundBorder(
            BorderFactory.createMatteBorder(5, 10, 5, 10,
                new ImageIcon(ResourceManager.getImage("image.background.ColorCellRenderer"))),
            BorderFactory.createLineBorder(BORDER_COLOR));

    public static final Border DIALOG_BORDER = BorderFactory
        .createCompoundBorder(TRIVIAL_LINE_BORDER, blankBorder(10, 20, 10, 20));

    public static final Border ETCHED_BORDER
        = BorderFactory.createEtchedBorder();

    public static final Border PRODUCTION_BORDER = BorderFactory
        .createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                                                              BORDER_COLOR),
                              blankBorder(2, 2, 2, 2));

    public static final Border PROGRESS_BORDER = BorderFactory
        .createLineBorder(new Color(122, 109, 82));

    public static final Border SIMPLE_LINE_BORDER = BorderFactory
        .createCompoundBorder(TRIVIAL_LINE_BORDER, blankBorder(5, 5, 5, 5));

    // The borders to use for table cells
    public static final Border TOPCELLBORDER
        = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 1, BORDER_COLOR),
            BorderFactory.createEmptyBorder(2, 2, 2, 2));

    public static final Border CELLBORDER
        = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER_COLOR),
            BorderFactory.createEmptyBorder(2, 2, 2, 2));

    public static final Border LEFTCELLBORDER
        = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 1, 1, BORDER_COLOR),
            BorderFactory.createEmptyBorder(2, 2, 2, 2));

    public static final Border TOPLEFTCELLBORDER
        = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, BORDER_COLOR),
            BorderFactory.createEmptyBorder(2, 2, 2, 2));


    /** How many columns (em-widths) to use in the text area. */
    private static final int DEFAULT_TEXT_COLUMNS = 20;

    /** The margin to use for a link button. */
    public static final Insets EMPTY_MARGIN = new Insets(0, 0, 0, 0);

    /** A style context to use for panels and dialogs. */
    public static StyleContext STYLE_CONTEXT = null;

    public static void initStyleContext(Font font) {
        Style defaultStyle = StyleContext.getDefaultStyleContext()
            .getStyle(StyleContext.DEFAULT_STYLE);

        STYLE_CONTEXT = new StyleContext();
        Style regular = STYLE_CONTEXT.addStyle("regular", defaultStyle);
        StyleConstants.setFontFamily(regular, font.getFamily());
        StyleConstants.setFontSize(regular, font.getSize());

        Style buttonStyle = STYLE_CONTEXT.addStyle("button", regular);
        StyleConstants.setForeground(buttonStyle, LINK_COLOR);

        Style right = STYLE_CONTEXT.addStyle("right", regular);
        StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);
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
        button.setMargin(EMPTY_MARGIN);
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setAlignmentY(0.8f);
        button.setBorder(blankBorder(0, 0, 0, 0));
        button.setActionCommand(action);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Make a suitable link button for a given key.
     *
     * Colonies and Europe-like objects are obvious, locations and units
     * are dependent on the message source.
     * TODO: Are there more useful possibilities?
     *
     * @param key The message key to make a link for.
     * @param val The text for the link.
     * @param player The <code>Player</code> to make a link for.
     * @param source The message source <code>FreeColGameObject</code>.
     * @return A <code>JButton</code> for the link, or null if no good
     *     choice found.
     */
    public static JButton getMessageButton(String key, String val,
        Player player, FreeColGameObject source) {
        FreeColGameObject link = null;
        if ("%colony%".equals(key) || key.endsWith("Colony%")) {
            Settlement settlement = player.getGame().getSettlementByName(val);
            link = (settlement == null) ? null
                : (player.owns(settlement)) ? settlement
                : settlement.getTile();
        } else if ("%europe%".equals(key) || "%market%".equals(key)) {
            link = player.getEurope();
        } else if ("%location%".equals(key) || key.endsWith("Location%")) {
            if (source instanceof Location) {
                link = source.getLinkTarget(player);
            }
        } else if ("%unit%".equals(key) || key.endsWith("Unit%")) {
            if (source instanceof Unit) {
                link = source.getLinkTarget(player);
            }
        }
        return (link == null) ? null
            : getLinkButton(val, null, link.getId());
    }

    /**
     * Creates a text area with standard settings suitable for use in FreeCol
     * panels, without setting its size.
     *
     * @param text The text to display in the text area.
     * @return A suitable text area.
     */
    public static JTextArea createTextArea(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setOpaque(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFocusable(false);
        return textArea;
    }

    /**
     * Gets a text area with standard settings suitable for use in FreeCol
     * panels.
     *
     * @param text The text to display in the text area.
     * @return A suitable text area.
     */
    public static JTextArea getDefaultTextArea(String text) {
        return getDefaultTextArea(text, DEFAULT_TEXT_COLUMNS);
    }

    /**
     * Gets a text area with standard settings suitable for use in FreeCol
     * panels, which adapt their size based on what they contain.
     *
     * @param text The text to display in the text area.
     * @param columns The em-width number of columns to display the text in.
     * @return A suitable text area.
     */
    public static JTextArea getDefaultTextArea(String text, int columns) {
        JTextArea textArea = createTextArea(text);
        textArea.setColumns(columns);
        textArea.setSize(textArea.getPreferredSize());
        return textArea;
    }

    /**
     * Gets a text area with standard settings suitable for use in FreeCol
     * panels, which can not adapt their size.
     *
     * @param text The text to display in the text area.
     * @param size The size of the area to display the text in.
     * @return A suitable text area.
     */
    public static JTextArea getDefaultTextArea(String text, Dimension size) {
        JTextArea textArea = createTextArea(text);
        textArea.setPreferredSize(size);
        return textArea;
    }

    /**
     * Get a <code>JTextPane</code> with default styles.
     *
     * @return The default <code>JTextPane</code> to use.
     */
    public static JTextPane getDefaultTextPane() {
        DefaultStyledDocument document
            = new DefaultStyledDocument(STYLE_CONTEXT);

        JTextPane textPane = new JTextPane(document);
        textPane.setOpaque(false);
        textPane.setEditable(false);
        textPane.setLogicalStyle(STYLE_CONTEXT.getStyle("regular"));
        return textPane;
    }

    /**
     * Get a <code>JTextPane</code> with default styles and given text.
     *
     * @param text The text to display.
     * @return A suitable <code>JTextPane</code>.
     */
    public static JTextPane getDefaultTextPane(String text) {
        JTextPane textPane = getDefaultTextPane();
        textPane.setText(text);
        return textPane;
    }

    /**
     * Get a border consisting of empty space.
     *
     * @param top Top spacing.
     * @param left left spacing.
     * @param bottom Bottom spacing.
     * @param right Right spacing.
     * @return A blank border.
     */
    public static Border blankBorder(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    public static void padBorder(JComponent component, int top, int left,
                                 int bottom, int right) {
        component.setBorder(BorderFactory.createCompoundBorder(
                blankBorder(top, left, bottom, right),
                component.getBorder()));
    }

    /**
     * Localize the a titled border.
     *
     * @param component The <code>JComponent</code> to localize.
     * @param template The <code>StringTemplate</code> to use.
     */
    public static void localizeBorder(JComponent component,
                                      StringTemplate template) {
        TitledBorder tb = (TitledBorder)component.getBorder();
        tb.setTitle(Messages.message(template));
    }

    /**
     * Get a titled border for a Named object.
     *
     * @param named The <code>Named</code> to use.
     * @return The <code>TitledBorder</code>.
     */
    public static TitledBorder localizedBorder(Named named) {
        return localizedBorder(named.getNameKey());
    }

    /**
     * Get a titled border with Messages.message(key) as text.
     *
     * @param key The key to use.
     * @return The <code>TitledBorder</code>.
     */
    public static TitledBorder localizedBorder(String key) {
        return BorderFactory.createTitledBorder(BorderFactory
            .createEmptyBorder(), Messages.message(key));
    }

    /**
     * Get a titled border with Messages.message(key) as text and a given
     * colored line border.
     *
     * @param key The key to use.
     * @param color The color to use.
     * @return The <code>TitledBorder</code>.
     */
    public static TitledBorder localizedBorder(String key, Color color) {
        return BorderFactory.createTitledBorder(BorderFactory
            .createLineBorder(color, 1), Messages.message(key));
    }

    /**
     * Get a JButton with Messages.message(key) as text.
     *
     * @param key The key to use.
     * @return The <code>JButton</code>.
     */
    public static JButton localizedButton(String key) {
        return new JButton(Messages.message(key));
    }

    /**
     * Get a JButton with Messages.message(template) as text.
     *
     * @param template The <code>StringTemplate</code> to use.
     * @return The <code>JButton</code>.
     */
    public static JButton localizedButton(StringTemplate template) {
        return new JButton(Messages.message(template));
    }

    /**
     * Get a JCheckBoxMenuItem with Messages.message(key) as text.
     *
     * @param key The key to use.
     * @param value The initial value.
     * @return The <code>JCheckBoxMenuItem</code>.
     */
    public static JCheckBoxMenuItem localizedCheckBoxMenuItem(String key,
                                                              boolean value) {
        return new JCheckBoxMenuItem(Messages.message(key), value);
    }

    /**
     * Gets a default header for panels containing a localized message.
     *
     * @param key The message key to use.
     * @param small If true, use a smaller font.
     * @return A suitable <code>JLabel</code>.
     */
    public static JLabel localizedHeader(String key, boolean small) {
        JLabel header = localizedHeaderLabel(key, SwingConstants.CENTER,
            (small ? FontLibrary.FontSize.SMALL : FontLibrary.FontSize.BIG));
        header.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        return header;
    }

    /**
     * Gets a label containing a localized message using the header font.
     *
     * @param key The message key to use.
     * @param alignment The alignment.
     * @param size The font size.
     * @return A suitable <code>JLabel</code>.
     */
    public static JLabel localizedHeaderLabel(String key, int alignment,
                                              FontLibrary.FontSize size) {
        String text = Messages.message(key);
        JLabel header = new JLabel(text, alignment);
        header.setFont(FontLibrary.createCompatibleFont(
            text, FontLibrary.FontType.HEADER, size));
        header.setOpaque(false);
        return header;
    }

    public static JLabel localizedHeaderLabel(StringTemplate template,
                                              int alignment,
                                              FontLibrary.FontSize size) {
        String text = Messages.message(template);
        JLabel header = new JLabel(text, alignment);
        header.setFont(FontLibrary.createCompatibleFont(
            text, FontLibrary.FontType.HEADER, size));
        header.setOpaque(false);
        return header;
    }

    public static JLabel localizedHeaderLabel(Named named,
                                              FontLibrary.FontSize size) {
        return localizedHeaderLabel(named.getNameKey(),
                                    SwingConstants.LEADING, size);
    }

    /**
     * Get a JLabel with a named object.
     *
     * @param named The <code>Named</code> to use.
     * @return The <code>JLabel</code>.
     */
    public static JLabel localizedLabel(Named named) {
        return localizedLabel(named.getNameKey());
    }

    /**
     * Get a JLabel with Messages.message(key) as text.
     *
     * @param key The key to use.
     * @return The <code>JLabel</code>.
     */
    public static JLabel localizedLabel(String key) {
        return localizedLabel(StringTemplate.key(key));
    }

    /**
     * Get a JLabel with Messages.message(template) as text.
     *
     * @param template The <code>StringTemplate</code> to use.
     * @return The <code>JLabel</code>.
     */
    public static JLabel localizedLabel(StringTemplate template) {
        JLabel label = new JLabel(Messages.message(template));
        label.setOpaque(false);
        return label;
    }

    /**
     * Get a JLabel with Messages.message(template) as text.
     *
     * @param template The <code>StringTemplate</code> to use.
     * @param icon The icon to use.
     * @param alignment The alignment.
     * @return The <code>JLabel</code>.
     */
    public static JLabel localizedLabel(StringTemplate template, Icon icon,
                                        int alignment) {
        JLabel label = new JLabel(Messages.message(template), icon, alignment);
        label.setOpaque(false);
        return label;
    }

    /**
     * Get a JMenu with Messages.message(key) as text.
     *
     * @param key The key to use.
     * @return The <code>JMenu</code>.
     */
    public static JMenu localizedMenu(String key) {
        return new JMenu(Messages.message(key));
    }

    /**
     * Get a JMenu with Messages.message(template) as text.
     *
     * @param template The <code>StringTemplate</code> to use.
     * @return The <code>JMenu</code>.
     */
    public static JMenu localizedMenu(StringTemplate template) {
        return new JMenu(Messages.message(template));
    }

    /**
     * Get a JMenuItem with Messages.message(key) as text.
     *
     * @param key The key to use.
     * @return The <code>JMenuItem</code>.
     */
    public static JMenuItem localizedMenuItem(String key) {
        return localizedMenuItem(key, null);
    }

    /**
     * Get a JMenuItem with Messages.message(key) as text.
     *
     * @param key The key to use.
     * @param icon The icon to use.
     * @return The <code>JMenuItem</code>.
     */
    public static JMenuItem localizedMenuItem(String key, Icon icon) {
        return new JMenuItem(Messages.message(key), icon);
    }

    /**
     * Get a JMenuItem with Messages.message(template) as text.
     *
     * @param template The <code>StringTemplate</code> to use.
     * @return The <code>JMenuItem</code>.
     */
    public static JMenuItem localizedMenuItem(StringTemplate template) {
        return localizedMenuItem(template, null);
    }

    /**
     * Get a JMenuItem with Messages.message(template) as text.
     *
     * @param template The <code>StringTemplate</code> to use.
     * @param icon The icon to use.
     * @return The <code>JMenuItem</code>.
     */
    public static JMenuItem localizedMenuItem(StringTemplate template,
                                              Icon icon) {
        return new JMenuItem(Messages.message(template), icon);
    }

    /**
     * Get a JRadioButtonMenuItem with Messages.message(template) as text.
     *
     * @param template The <code>StringTemplate</code> to generate the text.
     * @param value The initial value.
     * @return The <code>JRadioButtonMenuItem</code>.
     */
    public static JRadioButtonMenuItem localizedRadioButtonMenuItem(StringTemplate template,
                                                                    boolean value) {
        return new JRadioButtonMenuItem(Messages.message(template), value);
    }

    /**
     * Get a text area containing a localized message.
     *
     * @param key The message key.
     * @return A suitable <code>JTextArea</code>.
     */
    public static JTextArea localizedTextArea(String key) {
        return localizedTextArea(StringTemplate.key(key));
    }

    /**
     * Get a text area containing a localized message.
     *
     * @param key The message key.
     * @param columns The em-width number of columns to display the text in.
     * @return A suitable <code>JTextArea</code>.
     */
    public static JTextArea localizedTextArea(String key, int columns) {
        return localizedTextArea(StringTemplate.key(key), columns);
    }

    /**
     * Get a text area containing a localized message.
     *
     * @param template The <code>StringTemplate</code> to use.
     * @return A suitable <code>JTextArea</code>.
     */
    public static JTextArea localizedTextArea(StringTemplate template) {
        return localizedTextArea(template, DEFAULT_TEXT_COLUMNS);
    }

    /**
     * Get a text area containing a localized message.
     *
     * @param template The <code>StringTemplate</code> to use.
     * @param columns The em-width number of columns to display the text in.
     * @return A suitable <code>JTextArea</code>.
     */
    public static JTextArea localizedTextArea(StringTemplate template,
                                              int columns) {
        return getDefaultTextArea(Messages.message(template), columns);
    }

    /**
     * Localize the tool tip message for a JComponent.
     *
     * @param comp The <code>JComponent</code> to localize.
     * @param key The key to use.
     * @return The original <code>JComponent</code>.
     */
    public static JComponent localizeToolTip(JComponent comp, String key) {
        comp.setToolTipText(Messages.message(key));
        return comp;
    }

    /**
     * Localize the tool tip message for a JComponent.
     *
     * @param comp The <code>JComponent</code> to localize.
     * @param template The <code>StringTemplate</code> to use.
     * @return The original <code>JComponent</code>.
     */
    public static JComponent localizeToolTip(JComponent comp,
                                             StringTemplate template) {
        comp.setToolTipText(Messages.message(template));
        return comp;
    }

}
