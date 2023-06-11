/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.TextLayout;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
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
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.FreeColButton.ButtonStyle;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColSpecObjectType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Named;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;


/**
 * Static utilities for panels and dialogs.
 */
public final class Utility {

    /** Useful static borders. */
    public static final Border BEVEL_BORDER
        = BorderFactory.createBevelBorder(BevelBorder.LOWERED);

    public static final Border ETCHED_BORDER
        = BorderFactory.createEtchedBorder();

    // FIXME: color should be a Resource
    public static final Border PROGRESS_BORDER = BorderFactory
        .createLineBorder(new Color(122, 109, 82));

    private static Border COLOR_CELL_BORDER = null,
        DIALOG_BORDER = null,
        SIMPLE_LINE_BORDER = null,
        TOPCELLBORDER = null,
        CELLBORDER = null,
        LEFTCELLBORDER = null,
        TOPLEFTCELLBORDER = null;

    /** How many columns (em-widths) to use in the text area. */
    private static final int DEFAULT_TEXT_COLUMNS = 20;

    /** The margin to use for a link button. */
    public static final Insets EMPTY_MARGIN = new Insets(0, 0, 0, 0);

    /** A style context to use for panels and dialogs. */
    private static StyleContext STYLE_CONTEXT = null;

    /** Font specification for panel titles. */
    public static String FONTSPEC_TITLE = "header-plain-max";
    /** Font specification for panel subtitles. */
    public static String FONTSPEC_SUBTITLE = "header-plain-large";
    
    public static void initStyleContext(Font font) {
        Style defaultStyle = StyleContext.getDefaultStyleContext()
            .getStyle(StyleContext.DEFAULT_STYLE);

        STYLE_CONTEXT = new StyleContext();
        Style regular = STYLE_CONTEXT.addStyle("regular", defaultStyle);
        StyleConstants.setFontFamily(regular, font.getFamily());
        StyleConstants.setFontSize(regular, font.getSize());

        Style buttonStyle = STYLE_CONTEXT.addStyle("button", regular);
        StyleConstants.setForeground(buttonStyle, getLinkColor());

        Style right = STYLE_CONTEXT.addStyle("right", regular);
        StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);
    }

    /** 
     * Get the standard border color.
     *
     * @return The border {@code Color}.
     */
    public static Color getBorderColor() {
        return ImageLibrary.getColor("color.border.LookAndFeel",
                                     Color.BLACK);
    }
    
    /** 
     * Get the color to use for links.
     *
     * @return The link {@code Color}.
     */
    public static Color getLinkColor() {
        return ImageLibrary.getColor("color.link.LookAndFeel", Color.BLUE);
    }

    /** 
     * Get the color to use for things the player probably should not do.
     *
     * @return The warning {@code Color}.
     */
    public static Color getWarningColor() {
        return ImageLibrary.getColor("color.warning.LookAndFeel", Color.RED);
    }

    // Borders that depend on Resources
    
    public static synchronized Border getColorCellBorder() {
        if (COLOR_CELL_BORDER == null) {
            ImageIcon icon = new ImageIcon(ImageLibrary
                .getColorCellRendererBackground());
            COLOR_CELL_BORDER = BorderFactory
                .createCompoundBorder(BorderFactory
                    .createMatteBorder(5, 10, 5, 10, icon),
                    BorderFactory.createLineBorder(getBorderColor()));
        }
        return COLOR_CELL_BORDER;
    }
    
    public static synchronized Border getDialogBorder() {
        if (DIALOG_BORDER == null) {
            DIALOG_BORDER = BorderFactory
                .createCompoundBorder(getTrivialLineBorder(),
                                      blankBorder(10, 20, 10, 20));
        }
        return DIALOG_BORDER;
    }

    public static synchronized Border getProductionBorder() {
        return BorderFactory.createCompoundBorder(BorderFactory
            .createMatteBorder(1, 0, 0, 0, getBorderColor()),
                               blankBorder(2, 2, 2, 2));
    }
    
    public static synchronized Border getSimpleLineBorder() {
        if (SIMPLE_LINE_BORDER == null) {
            SIMPLE_LINE_BORDER = BorderFactory
                .createCompoundBorder(getTrivialLineBorder(),
                                      blankBorder(5, 5, 5, 5));
        }
        return SIMPLE_LINE_BORDER;
    }

    public static synchronized Border getTrivialLineBorder() {
        return BorderFactory.createLineBorder(getBorderColor());
    }

    // The borders to use for table cells

    public static synchronized Border getTopCellBorder() {
        if (TOPCELLBORDER == null) {
            TOPCELLBORDER = BorderFactory
                .createCompoundBorder(BorderFactory
                    .createMatteBorder(1, 0, 1, 1, getBorderColor()),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }
        return TOPCELLBORDER;
    }

    public static synchronized Border getCellBorder() {
        if (CELLBORDER == null) {
            CELLBORDER = BorderFactory
                .createCompoundBorder(BorderFactory
                    .createMatteBorder(0, 0, 1, 1, getBorderColor()),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }
        return CELLBORDER;
    }

    public static synchronized Border getLeftCellBorder() {
        if (LEFTCELLBORDER == null) {
            LEFTCELLBORDER = BorderFactory
                .createCompoundBorder(BorderFactory
                    .createMatteBorder(0, 1, 1, 1, getBorderColor()),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }
        return LEFTCELLBORDER;
    }

    public static synchronized Border getTopLeftCellBorder() {
        if (TOPLEFTCELLBORDER == null) {
            TOPLEFTCELLBORDER = BorderFactory
                .createCompoundBorder(BorderFactory
                    .createMatteBorder(1, 1, 1, 1, getBorderColor()),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }
        return TOPLEFTCELLBORDER;
    }

    
    /**
     * Return a button suitable for linking to another panel
     * (e.g. ColopediaPanel).
     *
     * @param text a {@code String} value
     * @param icon an {@code Icon} value
     * @param action a {@code String} value
     * @return a {@code JButton} value
     */
    public static JButton getLinkButton(String text, Icon icon, String action) {
        JButton button = new FreeColButton(text, icon).withButtonStyle(ButtonStyle.TRANSPARENT);
        button.setMargin(EMPTY_MARGIN);
        button.setOpaque(false);
        button.setForeground(getLinkColor());
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
     * @param player The {@code Player} to make a link for.
     * @param source The message source {@code FreeColGameObject}.
     * @return A {@code JButton} for the link, or null if no good
     *     choice found.
     */
    public static JButton getMessageButton(String key, String val,
        Player player, FreeColGameObject source) {
        FreeColGameObject link = null;
        if ("%colony%".equals(key) || key.endsWith("Colony%")
            || "%settlement%".equals(key)) {
            Settlement settlement = player.getGame().getSettlementByName(val);
            link = (settlement == null) ? null
                : (player.owns(settlement)) ? settlement
                : settlement.getTile();
        } else if ("%europe%".equals(key) || "%market%".equals(key)) {
            link = player.getEurope();
        } else if ("%location%".equals(key)
            || "%repairLocation%".equals(key)) {
            if (source instanceof Location) {
                link = source.getLinkTarget(player);
            }
        } else if ("%unit%".equals(key)) {
            if (source instanceof Unit && player.owns((Unit)source)) {
                link = source.getLinkTarget(player);
            }
        } else if ("%enemyUnit%".equals(key)) {
            if (source instanceof Unit && !player.owns((Unit)source)) {
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
    private static JTextArea createTextArea(String text) {
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
     * Get a {@code JTextPane} with default styles.
     *
     * @return The default {@code JTextPane} to use.
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
     * Get a {@code JTextPane} with default styles and given text.
     *
     * @param text The text to display.
     * @return A suitable {@code JTextPane}.
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
     * Returns a combo box for selecting a possible server address.
     * @return The combo box.
     */
    public static JComboBox<InetAddress> createServerInetAddressBox() {
        final List<InetAddress> serverAddresses = getPossibleServerAddresses();
        final JComboBox<InetAddress> serverAddressBox = new JComboBox<>(serverAddresses.toArray(new InetAddress[0]));
        serverAddressBox.setRenderer(new FreeColComboBoxRenderer<>());
        return serverAddressBox;
    }

    private static List<InetAddress> getPossibleServerAddresses() {
        List<InetAddress> serverAddresses;
        try {
            serverAddresses = NetworkInterface.networkInterfaces()
                    .flatMap(NetworkInterface::inetAddresses)
                    .filter(ia -> ia instanceof Inet4Address)
                    .collect(Collectors.toList());
        } catch (SocketException e) {
            serverAddresses = List.of(Inet4Address.getLoopbackAddress());
        }
        return serverAddresses;
    }


    /**
     * Localize the a titled border.
     *
     * @param component The {@code JComponent} to localize.
     * @param template The {@code StringTemplate} to use.
     */
    public static void localizeBorder(JComponent component,
                                      StringTemplate template) {
        TitledBorder tb = (TitledBorder)component.getBorder();
        tb.setTitle(Messages.message(template));
    }

    /**
     * Get a titled border for a Named object.
     *
     * @param named The {@code Named} to use.
     * @return The {@code TitledBorder}.
     */
    public static TitledBorder localizedBorder(Named named) {
        return localizedBorder(named.getNameKey());
    }

    /**
     * Get a titled border for a Named object and a given
     * colored line border.
     *
     * @param named The {@code Named} to use.
     * @param color The color to use.
     * @return The {@code TitledBorder}.
     */
    public static TitledBorder localizedBorder(Named named, Color color) {
        return localizedBorder(named.getNameKey(), color);
    }

    /**
     * Get a titled border with Messages.message(key) as text.
     *
     * @param key The key to use.
     * @return The {@code TitledBorder}.
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
     * @return The {@code TitledBorder}.
     */
    public static TitledBorder localizedBorder(String key, Color color) {
        return BorderFactory.createTitledBorder(BorderFactory
            .createLineBorder(color, 1), Messages.message(key));
    }

    /**
     * Get a JButton with Messages.message(key) as text.
     *
     * @param key The key to use.
     * @return The {@code JButton}.
     */
    public static FreeColButton localizedButton(String key) {
        return new FreeColButton(Messages.message(key));
    }

    /**
     * Get a JButton with Messages.message(template) as text.
     *
     * @param template The {@code StringTemplate} to use.
     * @return The {@code JButton}.
     */
    public static JButton localizedButton(StringTemplate template) {
        return new JButton(Messages.message(template));
    }

    /**
     * Get a JCheckBoxMenuItem with Messages.message(key) as text.
     *
     * @param key The key to use.
     * @param value The initial value.
     * @return The {@code JCheckBoxMenuItem}.
     */
    public static JCheckBoxMenuItem localizedCheckBoxMenuItem(String key,
                                                              boolean value) {
        return new JCheckBoxMenuItem(Messages.message(key), value);
    }

    /**
     * Gets a default header for panels containing a localized message.
     *
     * @param key The message key to use.
     * @param fontSpec A font-specification for the font to use.
     * @return A suitable {@code JLabel}.
     */
    public static JLabel localizedHeader(String key, String fontSpec) {
        JLabel header = localizedHeaderLabel(key, SwingConstants.CENTER,
                                             fontSpec);
        header.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        return header;
    }

    /**
     * Gets a label containing a localized message using the header font.
     *
     * @param key The message key to use.
     * @param alignment The alignment.
     * @param fontSpec A font specification.
     * @return A suitable {@code JLabel}.
     */
    public static JLabel localizedHeaderLabel(String key, int alignment,
                                              String fontSpec) {
        String text = Messages.message(key);
        JLabel header = new JLabel(text, alignment);
        header.setFont(FontLibrary.getScaledFont(fontSpec, text));
        header.setOpaque(false);
        return header;
    }

    public static JLabel localizedHeaderLabel(StringTemplate template,
                                              int alignment,
                                              String fontSpec) {
        String text = Messages.message(template);
        JLabel header = new JLabel(text, alignment);
        header.setFont(FontLibrary.getScaledFont(fontSpec, text));
        header.setOpaque(false);
        return header;
    }

    public static JLabel localizedHeaderLabel(Named named,
                                              String fontSpec) {
        return localizedHeaderLabel(named.getNameKey(),
                                    SwingConstants.LEADING, fontSpec);
    }

    /**
     * Get a JLabel for a FreeColSpecObjectType.
     *
     * @param fcgot The {@code FreeColSpecObjectType} to use.
     * @return The {@code JLabel}.
     */
    public static JLabel localizedLabel(FreeColSpecObjectType fcgot) {
        return localizedLabel(fcgot.getNameKey());
    }

    /**
     * Get a JLabel with Messages.message(key) as text.
     *
     * @param key The key to use.
     * @return The {@code JLabel}.
     */
    public static JLabel localizedLabel(String key) {
        return localizedLabel(StringTemplate.key(key));
    }

    /**
     * Get a JLabel with Messages.message(template) as text.
     *
     * @param template The {@code StringTemplate} to use.
     * @return The {@code JLabel}.
     */
    public static JLabel localizedLabel(StringTemplate template) {
        JLabel label = new JLabel(Messages.message(template));
        label.setOpaque(false);
        return label;
    }

    /**
     * Get a JLabel with Messages.message(template) as text.
     *
     * @param template The {@code StringTemplate} to use.
     * @param icon The icon to use.
     * @param alignment The alignment.
     * @return The {@code JLabel}.
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
     * @return The {@code JMenu}.
     */
    public static JMenu localizedMenu(String key) {
        return new JMenu(Messages.message(key));
    }

    /**
     * Get a JMenu with Messages.message(template) as text.
     *
     * @param template The {@code StringTemplate} to use.
     * @return The {@code JMenu}.
     */
    public static JMenu localizedMenu(StringTemplate template) {
        return new JMenu(Messages.message(template));
    }

    /**
     * Get a JMenuItem with Messages.message(key) as text.
     *
     * @param key The key to use.
     * @return The {@code JMenuItem}.
     */
    public static JMenuItem localizedMenuItem(String key) {
        return localizedMenuItem(key, null);
    }

    /**
     * Get a JMenuItem with Messages.message(key) as text.
     *
     * @param key The key to use.
     * @param icon The icon to use.
     * @return The {@code JMenuItem}.
     */
    public static JMenuItem localizedMenuItem(String key, Icon icon) {
        return new JMenuItem(Messages.message(key), icon);
    }

    /**
     * Get a JMenuItem with Messages.message(template) as text.
     *
     * @param template The {@code StringTemplate} to use.
     * @return The {@code JMenuItem}.
     */
    public static JMenuItem localizedMenuItem(StringTemplate template) {
        return localizedMenuItem(template, null);
    }

    /**
     * Get a JMenuItem with Messages.message(template) as text.
     *
     * @param template The {@code StringTemplate} to use.
     * @param icon The icon to use.
     * @return The {@code JMenuItem}.
     */
    public static JMenuItem localizedMenuItem(StringTemplate template,
                                              Icon icon) {
        return new JMenuItem(Messages.message(template), icon);
    }

    /**
     * Get a JRadioButtonMenuItem with Messages.message(template) as text.
     *
     * @param template The {@code StringTemplate} to generate the text.
     * @param value The initial value.
     * @return The {@code JRadioButtonMenuItem}.
     */
    public static JRadioButtonMenuItem localizedRadioButtonMenuItem(StringTemplate template,
                                                                    boolean value) {
        return new JRadioButtonMenuItem(Messages.message(template), value);
    }

    /**
     * Get a text area containing a localized message.
     *
     * @param key The message key.
     * @return A suitable {@code JTextArea}.
     */
    public static JTextArea localizedTextArea(String key) {
        return localizedTextArea(StringTemplate.key(key));
    }

    /**
     * Get a text area containing a localized message.
     *
     * @param key The message key.
     * @param columns The em-width number of columns to display the text in.
     * @return A suitable {@code JTextArea}.
     */
    public static JTextArea localizedTextArea(String key, int columns) {
        return localizedTextArea(StringTemplate.key(key), columns);
    }

    /**
     * Get a text area containing a localized message.
     *
     * @param template The {@code StringTemplate} to use.
     * @return A suitable {@code JTextArea}.
     */
    public static JTextArea localizedTextArea(StringTemplate template) {
        return localizedTextArea(template, DEFAULT_TEXT_COLUMNS);
    }

    /**
     * Get a text area containing a localized message.
     *
     * @param template The {@code StringTemplate} to use.
     * @param columns The em-width number of columns to display the text in.
     * @return A suitable {@code JTextArea}.
     */
    public static JTextArea localizedTextArea(StringTemplate template,
                                              int columns) {
        return getDefaultTextArea(Messages.message(template), columns);
    }

    /**
     * Get a panel with a localized message and icon.
     *
     * @param template The {@code StringTemplate} to use.
     * @param icon An {@code ImageIcon} to use.
     * @return The resulting {@code JPanel}.
     */
    public static JPanel localizedTextPanel(StringTemplate template,
                                            ImageIcon icon) {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel(icon));
        panel.add(Utility.localizedTextArea(template));
        panel.setBorder(getDialogBorder());
        return panel;
    }
        
    /**
     * Localize the tool tip message for a JComponent.
     *
     * @param comp The {@code JComponent} to localize.
     * @param key The key to use.
     */
    public static void localizeToolTip(JComponent comp, String key) {
        comp.setToolTipText(Messages.message(key));
    }

    /**
     * Localize the tool tip message for a JComponent.
     *
     * @param comp The {@code JComponent} to localize.
     * @param template The {@code StringTemplate} to use.
     */
    public static void localizeToolTip(JComponent comp,
                                       StringTemplate template) {
        comp.setToolTipText(Messages.message(template));
    }

    public static void drawGoldenText(final String text, Graphics2D g2d, final Font font, final int x, final int y) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        final Shape textShape = new TextLayout(text, font, g2d.getFontRenderContext()).getOutline(null);
        final float strokeScaling = FontLibrary.getFontScaling() / 2;
        final Stroke oldStroke = g2d.getStroke();        
        g2d.translate(x, y);
        
        g2d.setStroke(new BasicStroke(strokeScaling * 4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(Color.BLACK);
        g2d.draw(textShape);
        
        g2d.setStroke(new BasicStroke(strokeScaling * 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(162, 136, 105));
        g2d.draw(textShape);
        
        g2d.setStroke(new BasicStroke(strokeScaling * 1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(64, 31, 6));
        g2d.draw(textShape);

        g2d.setStroke(oldStroke);
        g2d.setColor(new Color(222, 194, 161));
        g2d.fill(textShape);
        
        g2d.translate(-x, -y);
    }
}
