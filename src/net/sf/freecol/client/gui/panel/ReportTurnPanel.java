/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
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
    private List<ModelMessage> messages;

    private Hashtable<String, Vector<JComponent>> textPanesByMessage
        = new Hashtable<String, Vector<JComponent>>();
    private Hashtable<String, Vector<JComponent>> labelsByMessage
        = new Hashtable<String, Vector<JComponent>>();


    /**
     * Creates the turn report.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param messages The <code>ModelMessages</code> to display in the report.
     */
    public ReportTurnPanel(FreeColClient freeColClient,
                           List<ModelMessage> messages) {
        super(freeColClient, "reportTurnAction");

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
    public void setMessages(List<ModelMessage> messages) {
        this.messages = messages;
        if (messages != null) displayMessages();
    }
        
    private void displayMessages() {
        final Game game = getFreeColClient().getGame();
        final ClientOptions options = getClientOptions();
        final int groupBy = options.getInteger(ClientOptions.MESSAGES_GROUP_BY);

        // Sort if requested
        final Comparator<ModelMessage> comparator
            = options.getModelMessageComparator(game);
        if (comparator != null) Collections.sort(messages, comparator);

        Object source = this;
        ModelMessage.MessageType type = null;
        for (ModelMessage message : messages) {
            // Add headline if the grouping changed
            switch (groupBy) {
            case ClientOptions.MESSAGES_GROUP_BY_SOURCE:
                FreeColGameObject messageSource = game.getMessageSource(message);
                if (messageSource != source) {
                    source = messageSource;
                    reportPanel.add(getHeadline(source), "newline 20, skip");
                }
                break;
            case ClientOptions.MESSAGES_GROUP_BY_TYPE:
                if (message.getMessageType() != type) {
                    type = message.getMessageType();
                    JLabel headline = GUI.localizedLabel(message.getMessageTypeName());
                    headline.setFont(GUI.SMALL_HEADER_FONT);
                    reportPanel.add(headline, "newline 20, skip, span");
                }
                break;
            default:
                break;
            }
            
            JComponent component = new JLabel();
            FreeColObject messageDisplay = game.getMessageDisplay(message);
            if (messageDisplay != null) {
                // FIXME: Scale icons relative to font size.
                ImageIcon icon = getGUI().getImageIcon(messageDisplay, false);
                if (icon != null && icon.getIconHeight() > 40) {
                    Image image = icon.getImage();
                    int newWidth = (int)((double)image.getWidth(null)
                        / image.getHeight(null)*40.0);
                    image = image.getScaledInstance(newWidth, 40,
                                                    Image.SCALE_SMOOTH);
                    icon.setImage(image);
                }
                
                if (messageDisplay instanceof Colony
                    || messageDisplay instanceof Europe) {
                    JButton button = GUI.getLinkButton(null, icon,
                                                       messageDisplay.getId());
                    button.addActionListener(this);
                    component = button;
                } else if (messageDisplay instanceof Unit) {
                    JButton button = GUI.getLinkButton(null, icon,
                        upLoc(((Unit)messageDisplay).getLocation()).getId());
                    button.addActionListener(this);
                    component = button;
                } else { // includes Player
                    component = new JLabel(icon);
                }
            }

            reportPanel.add(component, "newline");
            
            final JTextPane textPane = GUI.getDefaultTextPane();
            insertMessage(textPane.getStyledDocument(), message,
                          getMyPlayer());
            reportPanel.add(textPane);

            boolean ignore = false;
            final JComponent label = component;
            switch (message.getMessageType()) {
            case WAREHOUSE_CAPACITY:
                JButton ignoreButton = new JButton("x");
                GUI.localizeToolTip(ignoreButton, 
                    new StringTemplate("model.message.ignore", message));
                final ModelMessage m = message;
                ignoreButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            boolean flag = label.isEnabled();
                            igc().ignoreMessage(m, flag);
                            textPane.setEnabled(!flag);
                            label.setEnabled(!flag);
                        }
                    });
                reportPanel.add(ignoreButton);
                ignore = true;
                break;
            default:
                break;
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
                GUI.localizeToolTip(filterButton, StringTemplate
                    .template("model.message.filter")
                    .add("%type%", message.getMessageTypeName()));
                final ModelMessage m = message;
                filterButton.addActionListener(new ActionListener() {
                        
                        public void actionPerformed(ActionEvent event) {
                            boolean flag = filterOption.getValue();
                            filterOption.setValue(!flag);
                            //textPane.setEnabled(!flag);
                            //label.setEnabled(!flag);
                            
                            setEnabledByType(m.getMessageType(), !flag);
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

    private void setEnabledByType(ModelMessage.MessageType type,
                                  boolean enabled) {
        for (ModelMessage m : messages) {
            if (m.getMessageType() == type) {
                for (JComponent textPane: textPanesByMessage.get(m.getId())) {
                    textPane.setEnabled(enabled);
                }
                for (JComponent label: labelsByMessage.get(m.getId())) {
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
            headline = GUI.localizedLabel(StringTemplate.template("playerNation")
                                      .addName("%player%", player.getName())
                                      .addStringTemplate("%nation%", player.getNationName()));
        } else if (source instanceof Europe) {
            Europe europe = (Europe) source;
            JButton button = new JButton(Messages.getName(europe));
            button.addActionListener(this);
            button.setActionCommand(europe.getId());
            headline = button;
        } else if (source instanceof Market) {
            Market market = (Market) source;
            JButton button = GUI.localizedButton(market.getOwner().getMarketName());
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
            JButton button
                = new JButton(unit.getDescription(Unit.UnitLabelType.NATIONAL));
            button.addActionListener(this);
            button.setActionCommand(unit.getLocation().getId());
            headline = button;
        } else if (source instanceof Tile) {
            final Tile tile = (Tile) source;
            JButton button = GUI.localizedButton(tile.getLocationLabelFor(getMyPlayer()));
            button.addActionListener(this);
            button.setActionCommand(tile.getId());
            headline = button;
        } else if (source instanceof Nameable) {
            headline = new JLabel(((Nameable) source).getName());
        } else {
            headline = new JLabel(source.toString());
        }

        headline.setFont(GUI.SMALL_HEADER_FONT);
        headline.setOpaque(false);
        headline.setForeground(GUI.LINK_COLOR);
        headline.setBorder(GUI.blankBorder(5, 0, 0, 0));
        return headline;
    }

    private void insertMessage(StyledDocument document, ModelMessage message,
                               Player player) {
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
                insertText(document, input);
                return;
            } else if (start > 0) {
                // output any string before the first occurrence of '%'
                insertText(document, input.substring(0, start));
            }

            int end;
            while ((end = input.indexOf('%', start + 1)) >= 0) {
                String var = input.substring(start, end + 1);
                String[] item = findReplacementData(message, var);
                FreeColGameObject messageSource = getFreeColClient().getGame()
                    .getMessageSource(message);
                if (item != null && var.equals(item[0])) {
                    // found variable to replace
                    if ("%colony%".equals(var) || var.endsWith("Colony%")) {
                        Colony colony = player.getColonyByName(item[1]);
                        if (colony != null) {
                            insertLinkButton(document, colony, item[1]);
                        } else if (messageSource instanceof Tile) {
                            insertLinkButton(document, messageSource, item[1]);
                        } else {
                            insertText(document, item[1]);
                        }
                    } else if ("%europe%".equals(var)
                        || ("%market%".equals(var) && player.isColonial())) {
                        insertLinkButton(document, player.getEurope(),
                            Messages.getName(player.getEurope()));
                    } else if ("%unit%".equals(var)
                        || var.endsWith("Unit%")
                        || "%newName%".equals(var)) {
                        Tile tile = null;
                        if (messageSource instanceof Unit) {
                            tile = ((Unit) messageSource).getTile();
                        } else if (messageSource instanceof Tile) {
                            tile = (Tile) messageSource;
                        }
                        if (tile != null) {
                            Settlement settlement = tile.getSettlement();
                            if (settlement != null) {
                                insertLinkButton(document, settlement, item[1]);
                            } else {
                                insertLinkButton(document, tile, item[1]);
                            }
                        } else {
                            insertText(document, item[1]);
                        }
                    } else if ("%location%".equals(var)
                        || var.endsWith("Location%")) {
                        if (messageSource instanceof Europe) {
                            insertLinkButton(document, player.getEurope(),
                                Messages.getName(player.getEurope()));
                        } else if (messageSource instanceof Location) {
                            Location loc = upLoc((Location)messageSource);
                            insertLinkButton(document, (FreeColGameObject)loc,
                                             item[1]);
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
            logger.log(Level.WARNING, "Insert fail: " + message, e);
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

    private void insertText(StyledDocument document, String text)
        throws Exception {
        document.insertString(document.getLength(), text,
                              document.getStyle("regular"));
    }

    private void insertLinkButton(StyledDocument document,
                                  FreeColGameObject object, String name)
        throws Exception {
        JButton button = GUI.getLinkButton(name, null, object.getId());
        button.addActionListener(this);
        StyleConstants.setComponent(document.getStyle("button"), button);
        document.insertString(document.getLength(), " ",
                              document.getStyle("button"));
    }
    
    public static Location upLoc(Location loc) {
        if (loc instanceof Unit) loc = ((Unit)loc).getLocation();
        return (loc == null) ? null
            : (loc.getSettlement() != null) ? loc.getSettlement()
            : loc;
    }

    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent event) {
        super.actionPerformed(event);
    }
}
