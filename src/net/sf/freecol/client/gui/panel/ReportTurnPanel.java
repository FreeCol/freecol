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

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Arrays;
import java.util.Comparator;

import java.util.List;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.option.BooleanOption;

import net.miginfocom.swing.MigLayout;

/**
 * This panel displays the Turn Report.
 */
public final class ReportTurnPanel extends ReportPanel {

    private ModelMessage[] _messages;
    private Hashtable<String, Vector<JComponent>> textPanesByMessage = new Hashtable<String, Vector<JComponent>>();
    private Hashtable<String, Vector<JComponent>> labelsByMessage = new Hashtable<String, Vector<JComponent>>();

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportTurnPanel(Canvas parent, ModelMessage... messages) {
        super(parent, Messages.message("menuBar.report.turn"));

        this._messages = messages;

        Comparator<ModelMessage> comparator = getClient().getClientOptions().getModelMessageComparator();
        if (comparator != null) {
            Arrays.sort(messages, comparator);
        }

        ClientOptions options = getClient().getClientOptions();
        int groupBy = options.getInteger(ClientOptions.MESSAGES_GROUP_BY);

        Object source = this;
        ModelMessage.MessageType type = null;
        int headlines = 0;

        // count number of headlines
        for (final ModelMessage message : messages) {
            if (groupBy == ClientOptions.MESSAGES_GROUP_BY_SOURCE
                && message.getSource() != source) {
                source = message.getSource();
                headlines++;
            } else if (groupBy == ClientOptions.MESSAGES_GROUP_BY_TYPE
                       && message.getMessageType() != type) {
                type = message.getMessageType();
                headlines++;
            }
        }

        // Display Panel
        reportPanel.removeAll();
        reportPanel.setLayout(new MigLayout("wrap 4", "[center][550!]:push[][]", ""));

        source = this;
        type = null;

        int row = 1;
        for (final ModelMessage message : messages) {
            // add headline if necessary
            if (groupBy == ClientOptions.MESSAGES_GROUP_BY_SOURCE
                && message.getSource() != source) {
                source = message.getSource();
                reportPanel.add(getHeadline(source), "newline 20, skip");
            } else if (groupBy == ClientOptions.MESSAGES_GROUP_BY_TYPE
                       && message.getMessageType() != type) {
                type = message.getMessageType();
                JLabel headline = localizedLabel(message.getMessageTypeName());
                headline.setFont(smallHeaderFont);
                reportPanel.add(headline, "newline 20, skip, span");
            }

            JComponent component = new JLabel();
            if (message.getDisplay() != null) {

                // TODO: Scale icons relative to font size.
                ImageIcon icon = getCanvas().getImageIcon(message.getDisplay(), false);
                if (icon != null && icon.getIconHeight() > 40) {
                    Image image = icon.getImage();
                    int newWidth = (int)((double)image.getWidth(null)/image.getHeight(null)*40.0);
                    image = image.getScaledInstance(newWidth, 40, Image.SCALE_SMOOTH);
                    icon.setImage(image);
                }

                if (message.getDisplay() instanceof Colony) {
                    JButton button = new JButton();
                    button.setIcon(icon);
                    button.setActionCommand(((Colony) message.getDisplay()).getId());
                    button.addActionListener(this);
                    button.setBorder(BorderFactory.createEmptyBorder());
                    component = button;
                } else if (message.getDisplay() instanceof Unit) {
                    JButton button = new JButton();
                    button.setIcon(icon);
                    button.setActionCommand(((Unit) message.getDisplay()).getLocation().getId());
                    button.addActionListener(this);
                    button.setBorder(BorderFactory.createEmptyBorder());
                    component = button;
                } else if (message.getDisplay() instanceof Player) {
                    component = new JLabel(icon);
                } else {
                    component = new JLabel(icon);
                }
            }
            reportPanel.add(component, "newline");

            final JTextPane textPane = getDefaultTextPane();

            insertMessage(textPane.getStyledDocument(), message, getMyPlayer());
            reportPanel.add(textPane);

            boolean ignore = false;
            final JComponent label = component;
            if (message.getMessageType() == ModelMessage.MessageType.WAREHOUSE_CAPACITY) {
                JButton ignoreButton = new JButton("x");
                ignoreButton.setToolTipText(Messages.message(new StringTemplate("model.message.ignore", message)));
                ignoreButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        boolean flag = label.isEnabled();
                        getController().ignoreMessage(message, flag);
                        textPane.setEnabled(!flag);
                        label.setEnabled(!flag);
                    }
                });
                reportPanel.add(ignoreButton);
                ignore = true;
            }

            // So that we can iterate through rows in ActionListeners by message ID.
            if (!textPanesByMessage.containsKey(message.getId())) {
                textPanesByMessage.put(message.getId(), new Vector<JComponent>());
            }
            textPanesByMessage.get(message.getId()).add(textPane);

            if (!labelsByMessage.containsKey(message.getId())) {
                labelsByMessage.put(message.getId(), new Vector<JComponent>());
            }
            textPanesByMessage.get(message.getId()).add(textPane);
            textPanesByMessage.get(message.getId()).add(label);

            final BooleanOption filterOption = options.getBooleanOption(message);
            // Message type can be filtered
            if (filterOption != null) {
                JButton filterButton = new JButton("X");
                filterButton.setToolTipText(Messages.message("model.message.filter", "%type%",
                                                             message.getMessageTypeName()));
                filterButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent event) {
                        boolean flag = filterOption.getValue();
                        filterOption.setValue(!flag);
                        //textPane.setEnabled(!flag);
                        //label.setEnabled(!flag);

                        setEnabledByType(message.getMessageType(), !flag);
                    }

                });
                if (ignore) {
                    reportPanel.add(filterButton);
                } else {
                    reportPanel.add(filterButton, "skip");
                }
            }
        }
    }

    private void setEnabledByType(ModelMessage.MessageType type, boolean enabled) {
        for (int i = 0; i < _messages.length; i++) {
            if (_messages[i].getMessageType() == type) {
                for (JComponent textPane: textPanesByMessage.get(_messages[i].getId())) {
                    textPane.setEnabled(enabled);
                }
                for (JComponent label: labelsByMessage.get(_messages[i].getId())) {
                    label.setEnabled(enabled);
                }
            }
        }
    }

    private JComponent getHeadline(Object source) {
        JComponent headline;
        if (source == null) {
            return new JLabel();
        } else if (source instanceof Player) {
            Player player = (Player) source;
            headline = localizedLabel(StringTemplate.template("playerNation")
                                      .addName("%player%", player.getName())
                                      .addStringTemplate("%nation%", player.getNationName()));
        } else if (source instanceof Europe) {
            Europe europe = (Europe) source;
            JButton button = new JButton(europe.getName());
            button.addActionListener(this);
            button.setActionCommand(europe.getId());
            headline = button;
        } else if (source instanceof Market) {
            JButton button = new JButton(Messages.message("clientOptions.messages.guiShowMarketPrices.name"));
            button.addActionListener(this);
            button.setActionCommand(getMyPlayer().getEurope().getId());
            headline = button;
        } else if (source instanceof Colony) {
            final Colony colony = (Colony) source;
            JButton button = new JButton(colony.getName());
            button.addActionListener(this);
            button.setActionCommand(colony.getId());
            headline = button;
        } else if (source instanceof Unit) {
            final Unit unit = (Unit) source;
            JButton button = new JButton(unit.getName());
            button.addActionListener(this);
            button.setActionCommand(unit.getLocation().getId());
            headline = button;
        } else if (source instanceof Tile) {
            final Tile tile = (Tile) source;
            JButton button = new JButton(Messages.message(tile.getLocationName()));
            button.addActionListener(this);
            button.setActionCommand(tile.getId());
            headline = button;
        } else if (source instanceof Nameable) {
            headline = new JLabel(((Nameable) source).getName());
        } else {
            headline = new JLabel(source.toString());
        }

        headline.setFont(smallHeaderFont);
        headline.setOpaque(false);
        headline.setForeground(LINK_COLOR);
        headline.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        return headline;
    }

    private void insertMessage(StyledDocument document, ModelMessage message, Player player) {
        try {
            String input = Messages.message(message.getId());
            int start = input.indexOf('%');
            if (start == -1) {
                // no variables present
                insertText(document, input.substring(0));
                return;
            } else if (start > 0) {
                // output any string before the first occurrence of '%'
                insertText(document, input.substring(0, start));
            }
            int end;

            while ((end = input.indexOf('%', start + 1)) >= 0) {
                String var = input.substring(start, end + 1);
                String[] item = findReplacementData(message, var);
                if (item != null && var.equals(item[0])) {
                    // found variable to replace
                    if (var.equals("%colony%")) {
                        Colony colony = player.getColony(item[1]);
                        if (colony != null) {
                            insertLinkButton(document, colony, item[1]);
                        } else if (message.getSource() instanceof Tile) {
                            insertLinkButton(document, message.getSource(), item[1]);
                        } else {
                            insertText(document, item[1]);
                        }
                    } else if (var.equals("%europe%")) {
                        insertLinkButton(document, player.getEurope(), player.getEurope().getName());
                    } else if (var.equals("%unit%") || var.equals("%newName%")) {
                        Tile tile = null;
                        if (message.getSource() instanceof Unit) {
                            tile = ((Unit) message.getSource()).getTile();
                        } else if (message.getSource() instanceof Tile) {
                            tile = (Tile)message.getSource();
                        }
                        if (tile != null) {
                            insertLinkButton(document, tile, item[1]);
                        } else {
                            insertText(document, item[1]);
                        }
                    } else {
                        insertText(document, item[1]);
                    }
                    start = end + 1;
                } else {
                    // found no variable to replace: either a single '%', or
                    // some unnecessary variable
                    insertText(document, input.substring(start, end));
                    start = end;
                }
            }

            // output any string after the last occurrence of '%'
            if (start < input.length()) {
                insertText(document, input.substring(start));
            }

        } catch(Exception e) {
            logger.warning(e.toString());
        }
    }
    
    private String[] findReplacementData(ModelMessage message, String variable) {
        List<String> data = message.getKeys();
        if (data != null) {
            for (int index = 0; index < data.size(); index++) {
                if (variable.equals(data.get(index))) {
                    return new String[] {
                        variable,
                        Messages.message(message.getReplacements().get(index))
                    };
                }
            }
        }
        return null;
    }

    private void insertText(StyledDocument document, String text) throws Exception {
        document.insertString(document.getLength(), text,
                              document.getStyle("regular"));
    }


    private void insertLinkButton(StyledDocument document, FreeColGameObject object, String name)
        throws Exception {
        JButton button = getLinkButton(name, null, object.getId());
        button.addActionListener(this);
        StyleConstants.setComponent(document.getStyle("button"), button);
        document.insertString(document.getLength(), " ", document.getStyle("button"));
    }

}
