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

import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
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
                    reportPanel.add(getHeadline(messageSource), "newline 20, skip");
                }
                break;
            case ClientOptions.MESSAGES_GROUP_BY_TYPE:
                if (message.getMessageType() != type) {
                    type = message.getMessageType();
                    JLabel headline = Utility.localizedHeaderLabel(
                        message.getMessageType(), FontLibrary.FontSize.SMALL);
                    reportPanel.add(headline, "newline 20, skip, span");
                }
                break;
            default:
                break;
            }
            
            JComponent component = new JLabel();
            FreeColObject messageDisplay = game.getMessageDisplay(message);
            final ImageLibrary lib = getImageLibrary();
            if (messageDisplay != null) {
                Image image = lib.getObjectImage(messageDisplay, 1f);
                ImageIcon icon = (image == null) ? null : new ImageIcon(image);
                if (messageDisplay instanceof Colony
                    || messageDisplay instanceof Europe) {
                    JButton button = Utility.getLinkButton(null, icon,
                        messageDisplay.getId());
                    button.addActionListener(this);
                    component = button;
                } else if (messageDisplay instanceof Unit) {
                    JButton button = Utility.getLinkButton(null, icon,
                        ((Unit)messageDisplay).up().getId());
                    button.addActionListener(this);
                    component = button;
                } else { // includes Player
                    component = new JLabel(icon);
                }
            }

            reportPanel.add(component, "newline");
            
            final JTextPane textPane = Utility.getDefaultTextPane();
            try {
                insertMessage(textPane.getStyledDocument(), message,
                              getMyPlayer());
            } catch (BadLocationException ble) {
                logger.log(Level.WARNING, "message insert fail", ble);
            }
            reportPanel.add(textPane);

            boolean ignore = false;
            final JComponent label = component;
            switch (message.getMessageType()) {
            case WAREHOUSE_CAPACITY:
                JButton ignoreButton = new JButton("x");
                Utility.localizeToolTip(ignoreButton, 
                    StringTemplate.copy("report.turn.ignore", message));
                final ModelMessage m = message;
                ignoreButton.addActionListener((ActionEvent ae) -> {
                        boolean flag = label.isEnabled();
                        igc().ignoreMessage(m, flag);
                        textPane.setEnabled(!flag);
                        label.setEnabled(!flag);
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
                filterButton.addActionListener((ActionEvent ae) -> {
                        boolean flag = filterOption.getValue();
                        filterOption.setValue(!flag);
                        //textPane.setEnabled(!flag);
                        //label.setEnabled(!flag);
                        setEnabledByType(m.getMessageType(), !flag);
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

    private JComponent getHeadline(FreeColGameObject source) {
        String text;
        String commandId = null;
        if (source == null) {
            text = "";
        } else if (source instanceof Player) {
            Player player = (Player) source;
            StringTemplate template = StringTemplate
                .template("report.turn.playerNation")
                .addName("%player%", player.getName())
                .addStringTemplate("%nation%", player.getNationLabel());
            text = Messages.message(template);
        } else if (source instanceof Europe) {
            Europe europe = (Europe) source;
            text = Messages.getName(europe);
            commandId = europe.getId();
        } else if (source instanceof Market) {
            Market market = (Market) source;
            StringTemplate template = market.getOwner().getMarketName();
            text = Messages.message(template);
            commandId = getMyPlayer().getEurope().getId();
        } else if (source instanceof Colony) {
            final Colony colony = (Colony) source;
            text = colony.getName();
            commandId = colony.getId();
        } else if (source instanceof Unit) {
            final Unit unit = (Unit) source;
            text = unit.getDescription(Unit.UnitLabelType.NATIONAL);
            commandId = unit.getLocation().getId();
        } else if (source instanceof Tile) {
            final Tile tile = (Tile) source;
            StringTemplate template = tile.getLocationLabelFor(getMyPlayer());
            text = Messages.message(template);
            commandId = tile.getId();
        } else if (source instanceof Nameable) {
            text = ((Nameable) source).getName();
        } else {
            text = source.toString();
        }

        Font font = FontLibrary.createCompatibleFont(text,
            FontLibrary.FontType.HEADER, FontLibrary.FontSize.SMALL);
        JComponent headline;
        if(commandId != null) {
            JButton button = new JButton(text);
            button.addActionListener(this);
            button.setActionCommand(commandId);
            headline = button;
            headline.setForeground(Utility.LINK_COLOR);
        } else {
            headline = new JLabel(text);
        }
        headline.setFont(font);
        headline.setOpaque(false);
        headline.setBorder(Utility.blankBorder(5, 0, 0, 0));
        return headline;
    }

    private void insertMessage(StyledDocument document, ModelMessage message,
                               Player player) throws BadLocationException {
        for (Object o : message.splitLinks(player)) {
            if (o instanceof String) {
                document.insertString(document.getLength(), (String)o,
                                      document.getStyle("regular"));
            } else if (o instanceof JButton) {
                JButton b = (JButton)o;
                b.addActionListener(this);
                StyleConstants.setComponent(document.getStyle("button"), b);
                document.insertString(document.getLength(), " ",
                                      document.getStyle("button"));
            }
        }
    }
}
