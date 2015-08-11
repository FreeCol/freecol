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
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
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

    private final Hashtable<String, Vector<JComponent>> textPanesByMessage
        = new Hashtable<>();
    private final Hashtable<String, Vector<JComponent>> labelsByMessage
        = new Hashtable<>();


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
                    JLabel headline = Utility.localizedLabel(message.getMessageType());
                    headline.setFont(FontLibrary.createFont(FontLibrary.FontType.HEADER,
                        FontLibrary.FontSize.SMALL));
                    reportPanel.add(headline, "newline 20, skip, span");
                }
                break;
            default:
                break;
            }
            
            JComponent component = new JLabel();
            FreeColObject messageDisplay = game.getMessageDisplay(message);
            if (messageDisplay != null) {
                Image image = getImageLibrary().getObjectImage(messageDisplay, 1f);
                ImageIcon icon = (image == null) ? null : new ImageIcon(image);

                if (messageDisplay instanceof Colony
                    || messageDisplay instanceof Europe) {
                    JButton button = Utility.getLinkButton(null, icon,
                                                       messageDisplay.getId());
                    button.addActionListener(this);
                    component = button;
                } else if (messageDisplay instanceof Unit) {
                    JButton button = Utility.getLinkButton(null, icon,
                        upLoc(((Unit)messageDisplay).getLocation()).getId());
                    button.addActionListener(this);
                    component = button;
                } else { // includes Player
                    component = new JLabel(icon);
                }
            }

            reportPanel.add(component, "newline");
            
            final JTextPane textPane = Utility.getDefaultTextPane();
            insertMessage(textPane.getStyledDocument(), message,
                          getMyPlayer());
            reportPanel.add(textPane);

            boolean ignore = false;
            final JComponent label = component;
            switch (message.getMessageType()) {
            case WAREHOUSE_CAPACITY:
                JButton ignoreButton = new JButton("x");
                Utility.localizeToolTip(ignoreButton, 
                    StringTemplate.copy("report.turn.ignore", message));
                final ModelMessage m = message;
                ignoreButton.addActionListener(new ActionListener() {
                        @Override
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
                Utility.localizeToolTip(filterButton, StringTemplate
                    .template("report.turn.filter")
                    .addNamed("%type%", message.getMessageType()));
                final ModelMessage m = message;
                filterButton.addActionListener(new ActionListener() {
                        
                        @Override
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
            headline = Utility.localizedLabel(StringTemplate
                .template("report.turn.playerNation")
                .addName("%player%", player.getName())
                .addStringTemplate("%nation%", player.getNationLabel()));
        } else if (source instanceof Europe) {
            Europe europe = (Europe) source;
            JButton button = new JButton(Messages.getName(europe));
            button.addActionListener(this);
            button.setActionCommand(europe.getId());
            headline = button;
        } else if (source instanceof Market) {
            Market market = (Market) source;
            JButton button = Utility.localizedButton(market.getOwner().getMarketName());
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
            JButton button = Utility.localizedButton(tile.getLocationLabelFor(getMyPlayer()));
            button.addActionListener(this);
            button.setActionCommand(tile.getId());
            headline = button;
        } else if (source instanceof Nameable) {
            headline = new JLabel(((Nameable) source).getName());
        } else {
            headline = new JLabel(source.toString());
        }

        headline.setFont(FontLibrary.createFont(FontLibrary.FontType.HEADER,
            FontLibrary.FontSize.SMALL));
        headline.setOpaque(false);
        headline.setForeground(Utility.LINK_COLOR);
        headline.setBorder(Utility.blankBorder(5, 0, 0, 0));
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
                String item = Messages.message(message.getReplacement(var));
                FreeColGameObject messageSource = getFreeColClient().getGame()
                    .getMessageSource(message);
                if (item != null) {
                    // found variable to replace
                    if ("%colony%".equals(var) || var.endsWith("Colony%")) {
                        Colony colony = player.getColonyByName(item);
                        if (colony != null) {
                            insertLinkButton(document, colony, item);
                        } else if (messageSource instanceof Tile) {
                            insertLinkButton(document, messageSource, item);
                        } else {
                            insertText(document, item);
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
                            tile = ((Unit)messageSource).getTile();
                        } else if (messageSource instanceof Tile) {
                            tile = (Tile)messageSource;
                        }
                        if (tile != null) {
                            Settlement settlement = tile.getSettlement();
                            if (settlement != null) {
                                insertLinkButton(document, settlement, item);
                            } else {
                                insertLinkButton(document, tile, item);
                            }
                        } else {
                            insertText(document, item);
                        }
                    } else if ("%location%".equals(var)
                        || var.endsWith("Location%")) {
                        if (messageSource instanceof Europe) {
                            insertLinkButton(document, player.getEurope(),
                                Messages.getName(player.getEurope()));
                        } else if (messageSource instanceof Location) {
                            Location loc = upLoc((Location)messageSource);
                            insertLinkButton(document, (FreeColGameObject)loc,
                                             item);
                        } else {
                            insertText(document, item);
                        }
                    } else {
                        insertText(document, item);
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

    private void insertText(StyledDocument document, String text)
        throws BadLocationException {
        document.insertString(document.getLength(), text,
                              document.getStyle("regular"));
    }

    private void insertLinkButton(StyledDocument document,
                                  FreeColGameObject object, String name)
        throws BadLocationException {
        JButton button = Utility.getLinkButton(name, null, object.getId());
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
    @Override
    public void actionPerformed(ActionEvent event) {
        super.actionPerformed(event);
    }
}
