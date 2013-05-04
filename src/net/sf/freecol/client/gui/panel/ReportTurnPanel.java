/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.option.BooleanOption;


/**
 * This panel displays the Turn Report.
 */
public final class ReportTurnPanel extends ReportPanel {
	

    private static final Logger logger = Logger.getLogger(ReportTurnPanel.class.getName());

    /** The messages to display. */
    private ModelMessage[] messages;

    private Hashtable<String, Vector<JComponent>> textPanesByMessage
        = new Hashtable<String, Vector<JComponent>>();
    private Hashtable<String, Vector<JComponent>> labelsByMessage
        = new Hashtable<String, Vector<JComponent>>();


    /**
     * Creates the turn report.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param gui The <code>GUI</code> to display on.
     * @param messages The <code>ModelMessages</code> to display in the report.
     */
    public ReportTurnPanel(FreeColClient freeColClient, GUI gui,
                           ModelMessage... messages) {
        super(freeColClient, gui, Messages.message("reportTurnAction.name"));

        // Display Panel
        reportPanel.removeAll();
        reportPanel.setLayout(new MigLayout("wrap 4", "[center][550!]:push[][]", ""));
        setMessages(messages);
    }

    /**
     * Set the messages being displayed by this report.
     *
     * @param messages The <code>ModelMessages</code> to display in the report.
     */
    public void setMessages(ModelMessage[] messages) {
        this.messages = messages;
        if (messages != null) displayMessages();
    }
        
    private void displayMessages() {
        final Game game = getFreeColClient().getGame();
        final ClientOptions options = getClientOptions();
        final int groupBy = options.getInteger(ClientOptions.MESSAGES_GROUP_BY);
        final Comparator<ModelMessage> comparator
            = options.getModelMessageComparator(game);

        // count number of headlines
        Object source = this;
        ModelMessage.MessageType type = null;
        if (comparator != null) Arrays.sort(messages, comparator);
        for (final ModelMessage message : messages) {
            if (groupBy == ClientOptions.MESSAGES_GROUP_BY_SOURCE) {
                FreeColGameObject messageSource = game.getMessageSource(message);
                if (messageSource != source) {
                    source = messageSource;
                }
            } else if (groupBy == ClientOptions.MESSAGES_GROUP_BY_TYPE
                && message.getMessageType() != type) {
                type = message.getMessageType();
            }
        }
        
        source = this;
        type = null;
        for (final ModelMessage message : messages) {
            // add headline if necessary
            if (groupBy == ClientOptions.MESSAGES_GROUP_BY_SOURCE) {
                FreeColGameObject messageSource = game.getMessageSource(message);
                if (messageSource != source) {
                    source = messageSource;
                    reportPanel.add(getHeadline(source), "newline 20, skip");
                }
            } else if (groupBy == ClientOptions.MESSAGES_GROUP_BY_TYPE
                && message.getMessageType() != type) {
                type = message.getMessageType();
                JLabel headline = localizedLabel(message.getMessageTypeName());
                headline.setFont(smallHeaderFont);
                reportPanel.add(headline, "newline 20, skip, span");
            }
            
            JComponent component = new JLabel();
            FreeColObject messageDisplay = game.getMessageDisplay(message);
            if (messageDisplay != null) {
                // TODO: Scale icons relative to font size.
                ImageIcon icon = getGUI().getImageIcon(messageDisplay, false);
                if (icon != null && icon.getIconHeight() > 40) {
                    Image image = icon.getImage();
                    int newWidth = (int)((double)image.getWidth(null)/image.getHeight(null)*40.0);
                    image = image.getScaledInstance(newWidth, 40, Image.SCALE_SMOOTH);
                    icon.setImage(image);
                }
                
                if (messageDisplay instanceof Colony) {
                    JButton button = new JButton();
                    button.setIcon(icon);
                    button.setActionCommand(((Colony) messageDisplay).getId());
                    button.addActionListener(this);
                    button.setBorder(BorderFactory.createEmptyBorder());
                    component = button;
                } else if (messageDisplay instanceof Unit) {
                    JButton button = new JButton();
                    button.setIcon(icon);
                    button.setActionCommand(((Unit) messageDisplay).getLocation().getId());
                    button.addActionListener(this);
                    button.setBorder(BorderFactory.createEmptyBorder());
                    component = button;
                } else if (messageDisplay instanceof Player) {
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
            
            // So that we can iterate through rows in ActionListeners
            // by message identifier.
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
                filterButton.setToolTipText(Messages.message(StringTemplate.template("model.message.filter")
                        .add("%type%", message.getMessageTypeName())));
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
        for (int i = 0; i < messages.length; i++) {
            if (messages[i].getMessageType() == type) {
                for (JComponent textPane: textPanesByMessage.get(messages[i].getId())) {
                    textPane.setEnabled(enabled);
                }
                for (JComponent label: labelsByMessage.get(messages[i].getId())) {
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
                                      .addName("%player%", player.getDisplayName())
                                      .addStringTemplate("%nation%", player.getNationName()));
        } else if (source instanceof Europe) {
            Europe europe = (Europe) source;
            JButton button = new JButton(Messages.message(europe.getNameKey()));
            button.addActionListener(this);
            button.setActionCommand(europe.getId());
            headline = button;
        } else if (source instanceof Market) {
            Market market = (Market) source;
            JButton button = new JButton(Messages.message(market.getOwner().getMarketName()));
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
            JButton button = new JButton(Messages.message(unit.getLabel()));
            button.addActionListener(this);
            button.setActionCommand(unit.getLocation().getId());
            headline = button;
        } else if (source instanceof Tile) {
            final Tile tile = (Tile) source;
            JButton button = new JButton(Messages.message(tile.getLocationNameFor(getMyPlayer())));
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
            String input = null;
            String id = message.getId();
            if (id == null || id.equals(input = Messages.message(id))) {
                // id not present, fallback to default
                input = Messages.message(message.getDefaultId());
            }
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
                FreeColGameObject messageSource = getFreeColClient().getGame().getMessageSource(message);
                if (item != null && var.equals(item[0])) {
                    // found variable to replace
                    if (var.equals("%colony%")) {
                        Colony colony = player.getColonyByName(item[1]);
                        if (colony != null) {
                            insertLinkButton(document, colony, item[1]);
                        } else if (messageSource instanceof Tile) {
                            insertLinkButton(document, messageSource, item[1]);
                        } else {
                            insertText(document, item[1]);
                        }
                    } else if (var.equals("%europe%")) {
                        insertLinkButton(document, player.getEurope(),
                                         Messages.message(player.getEurope().getNameKey()));
                    } else if (var.equals("%unit%") || var.equals("%newName%")) {
                        Tile tile = null;
                        if (messageSource instanceof Unit) {
                            tile = ((Unit) messageSource).getTile();
                        } else if (messageSource instanceof Tile) {
                            tile = (Tile) messageSource;
                        }
                        if (tile != null) {
                            insertLinkButton(document, tile, item[1]);
                        } else {
                            insertText(document, item[1]);
                        }
                    } else if (var.equals("%repairLocation%")) {
                        if (messageSource instanceof Europe) {
                            insertLinkButton(document, player.getEurope(),
                                             Messages.message(player.getEurope().getNameKey()));
                        } else if (messageSource instanceof Colony) {
                            insertLinkButton(document, (Colony) messageSource, item[1]);
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

        } catch (Exception e) {
            logger.log(Level.WARNING, "Insert fail: " + message.toString(), e);
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
    
    @Override
    public void actionPerformed(ActionEvent event) {
        super.actionPerformed(event);
    }
}
