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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.option.BooleanOption;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Turn Report.
 */
public final class ReportTurnPanel extends ReportPanel implements ActionListener {



    private final FreeColClient freeColClient;

    private StyledDocument document;

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportTurnPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.turn"));
        this.freeColClient = parent.getClient();
    }

    /**
     * Prepares this panel to be displayed.
     *
     * @param messages The messages to be displayed
     */
    public void initialize(ArrayList<ModelMessage> messages) {

        Comparator<ModelMessage> comparator = getCanvas().getClient().getClientOptions().getModelMessageComparator();
        if (comparator != null) {
            Collections.sort(messages, comparator);
        }

        ClientOptions options = getCanvas().getClient().getClientOptions();
        int groupBy = options.getInteger(ClientOptions.MESSAGES_GROUP_BY);

        Object source = this;
        ModelMessage.MessageType type = null;
        int headlines = 0;

        // count number of headlines
        for (final ModelMessage message : messages) {
            if (groupBy == ClientOptions.MESSAGES_GROUP_BY_SOURCE && message.getSource() != source) {
                source = message.getSource();
                headlines++;
            } else if (groupBy == ClientOptions.MESSAGES_GROUP_BY_TYPE && message.getType() != type) {
                type = message.getType();
                headlines++;
            }
        }

        // Display Panel
        reportPanel.removeAll();

        int[] widths = new int[] { 0, margin, 500, margin, 0, margin, 0 };
        int[] heights = new int[2 * (messages.size() + headlines) - 1];
        int imageColumn = 1;
        int textColumn = 3;
        int button1Column = 5;
        int button2Column = 7;

        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }

        HIGLayout layout = new HIGLayout(widths, heights);
        layout.setColumnWeight(textColumn, 1);
        reportPanel.setLayout(layout);

        source = this;
        type = null;

        int row = 1;
        for (final ModelMessage message : messages) {
            // add headline if necessary
            if (groupBy == ClientOptions.MESSAGES_GROUP_BY_SOURCE && message.getSource() != source) {
                source = message.getSource();
                reportPanel.add(getHeadline(source), higConst.rc(row, textColumn, "l"));
                row += 2;
            } else if (groupBy == ClientOptions.MESSAGES_GROUP_BY_TYPE && message.getType() != type) {
                type = message.getType();
                JLabel headline = new JLabel(message.getTypeName());
                headline.setFont(smallHeaderFont);
                reportPanel.add(headline, higConst.rc(row, textColumn, "l"));
                row += 2;
            }

            final JLabel label = new JLabel();
            if (message.getDisplay() != null) {

                // TODO Scale icons relative to fond size.
                ImageIcon icon = getCanvas().getImageIcon(message.getDisplay(), false);
                if (icon != null && icon.getIconHeight() > 40) {
                    Image image = icon.getImage();
                    int newWidth = (int)((double)image.getWidth(null)/image.getHeight(null)*40.0);
                    image = image.getScaledInstance(newWidth, 40, Image.SCALE_SMOOTH);
                    icon.setImage(image);
                }

                if (message.getDisplay() instanceof Colony) {
                    final JButton button = new JButton();
                    button.setIcon(icon);
                    button.setActionCommand(((Colony) message.getDisplay()).getId());
                    button.addActionListener(this);
                    button.setBorder(BorderFactory.createEmptyBorder());
                } else if (message.getDisplay() instanceof Unit) {
                    label.setIcon(icon);
                } else if (message.getDisplay() instanceof Player) {
                    label.setIcon(icon);
                } else {
                    label.setIcon(icon);
                }

                reportPanel.add(label, higConst.rc(row, imageColumn, ""));
            }

            final JTextPane textPane = getTextPane(message);
            reportPanel.add(textPane, higConst.rc(row, textColumn));
            if (message.getType() == ModelMessage.MessageType.WAREHOUSE_CAPACITY) {
                JButton ignoreButton = new JButton("x");
                ignoreButton.setToolTipText(Messages.message("model.message.ignore", message.getData()));
                ignoreButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        boolean flag = label.isEnabled();
                        freeColClient.getInGameController().ignoreMessage(message, flag);
                        textPane.setEnabled(!flag);
                        label.setEnabled(!flag);
                    }
                });
                reportPanel.add(ignoreButton, higConst.rc(row, button1Column, ""));
            }
            JButton filterButton = new JButton("X");
            filterButton.setToolTipText(Messages.message("model.message.filter", 
                    "%type%", message.getTypeName()));
            final BooleanOption filterOption = options.getBooleanOption(message);
            filterButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    boolean flag = filterOption.getValue();
                    filterOption.setValue(!flag);
                    textPane.setEnabled(!flag);
                    label.setEnabled(!flag);
                }
            });
            reportPanel.add(filterButton, higConst.rc(row, button2Column, ""));

            row += 2;
        }

    }

    private JComponent getHeadline(Object source) {
        JComponent headline;
        if (source == null) {
            return new JLabel();
        } else if (source instanceof Player) {
            Player player = (Player) source;
            headline = new JLabel(Messages.message("playerNation", 
                    "%player%", player.getName(),
                    "%nation%", player.getNationAsString()));
        } else if (source instanceof Europe) {
            Europe europe = (Europe) source;
            JButton button = new JButton(europe.getName());
            button.addActionListener(this);
            button.setActionCommand(europe.getId());
            headline = button;
        } else if (source instanceof Market) {
            JButton button = new JButton(Messages.message("model.message.marketPrices"));
            button.addActionListener(this);
            button.setActionCommand(freeColClient.getMyPlayer().getEurope().getId());
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
            button.setActionCommand(unit.getTile().getId());
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

    //Create a text pane.
    private JTextPane getTextPane(ModelMessage message) {
        JTextPane textPane = new JTextPane();
        textPane.setOpaque(false);
        textPane.setEditable(false);

        document = textPane.getStyledDocument();
        //Initialize some styles.
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        
        Style regular = document.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "Dialog");
        StyleConstants.setBold(def, true);
        StyleConstants.setFontSize(def, 12);

        Style buttonStyle = document.addStyle("button", regular);
        StyleConstants.setForeground(buttonStyle, LINK_COLOR);

        insertMessage(message);
        return textPane;
    }



    private void insertMessage(ModelMessage message) {

        Player player = freeColClient.getMyPlayer();
        try {
            String input = Messages.message(message.getId());
            int start = input.indexOf('%');
            if (start == -1) {
                // no variables present
                insertText(input.substring(0));
                return;
            } else if (start > 0) {
                // output any string before the first occurrence of '%'
                insertText(input.substring(0, start));
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
                            insertLinkButton(colony, item[1]);
                        } else if (message.getSource() instanceof Tile) {
                            insertLinkButton(message.getSource(), item[1]);
                        } else {
                            insertText(item[1]);
                        }
                    } else if (var.equals("%europe%")) {
                        insertLinkButton(player.getEurope(), player.getEurope().getName());
                    } else if (var.equals("%unit%") || var.equals("%newName%")) {
                        Tile tile = null;
                        if (message.getSource() instanceof Unit) {
                            tile = ((Unit) message.getSource()).getTile();
                        } else if (message.getSource() instanceof Tile) {
                            tile = (Tile)message.getSource();
                        }
                        if (tile != null) {
                            insertLinkButton(tile, item[1]);
                        } else {
                            insertText(item[1]);
                        }
                    } else {
                        insertText(item[1]);
                    }
                    start = end + 1;
                } else {
                    // found no variable to replace: either a single '%', or
                    // some unnecessary variable
                    insertText(input.substring(start, end));
                    start = end;
                }
            }

            // output any string after the last occurrence of '%'
            if (start < input.length()) {
                insertText(input.substring(start));
            }
        } catch(Exception e) {
            logger.warning(e.toString());
        }
    }
    
    private String[] findReplacementData(ModelMessage message, String variable) {
        String[] data = message.getData();
        if (data == null) {
            // message with no variables
            return null;
        } else if (data.length % 2 == 0) {
            for (int index = 0; index < data.length; index += 2) {
                if (variable.equals(data[index])) {
                    return new String[] { variable, data[index + 1] };
                }
            }
        } else {
            logger.warning("Data has a wrong format for message: " + message);
        }
        return null;
    }

    private void insertText(String text) throws Exception {
        document.insertString(document.getLength(), text,
                              document.getStyle("regular"));
    }


    private void insertLinkButton(FreeColGameObject object, String name) throws Exception {
        JButton button = new JButton(name);
        button.setMargin(new Insets(0,0,0,0));
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setAlignmentY(0.8f);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.addActionListener(this);
        button.setActionCommand(object.getId());
        StyleConstants.setComponent(document.getStyle("button"), button);
        document.insertString(document.getLength(), " ", document.getStyle("button"));
    }


    /**
     * This function analyzes an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        //System.out.println(command);
        if (command.equals(String.valueOf(OK))) {
            super.actionPerformed(event);
        } else {
            FreeColGameObject object = freeColClient.getGame().getFreeColGameObject(command);
            if (object instanceof Europe) {
                getCanvas().showEuropePanel();
            } else if (object instanceof Tile) {
                getCanvas().getGUI().setFocus(((Tile) object).getPosition());
            } else if (object instanceof Colony) {
                getCanvas().showColonyPanel((Colony) object);
            }
        }
    }

}
