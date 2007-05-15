package net.sf.freecol.client.gui.panel;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.BorderFactory;
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
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private final FreeColClient freeColClient;

    private StyledDocument document;

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     * @param freeColClient The main controller object for the client.
     */
    public ReportTurnPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.turn"));
        this.freeColClient = parent.getClient();
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize(ArrayList<ModelMessage> messages) {

        Comparator<ModelMessage> comparator = getCanvas().getClient().getClientOptions().getModelMessageComparator();
        if (comparator != null) {
            Collections.sort(messages, comparator);
        }

        ClientOptions options = getCanvas().getClient().getClientOptions();
        int groupBy = options.getInteger(ClientOptions.MESSAGES_GROUP_BY);

        Object source = this;
        int type = -1;
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
        type = -1;

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
                if (message.getDisplay() instanceof Colony) {
                    final JButton button = new JButton();
                    button.setIcon(getCanvas().getImageIcon(message.getDisplay()));
                    button.setActionCommand(((Colony) message.getDisplay()).getName());
                    button.addActionListener(this);
                    button.setBorder(BorderFactory.createEmptyBorder());
                    reportPanel.add(button, higConst.rc(row, imageColumn, ""));
                } else {
                    label.setIcon(getCanvas().getImageIcon(message.getDisplay()));
                    reportPanel.add(label, higConst.rc(row, imageColumn, ""));
                }
            }

            final JTextPane textPane = getTextPane(message);
            reportPanel.add(textPane, higConst.rc(row, textColumn));
            if (message.getType() == ModelMessage.WAREHOUSE_CAPACITY) {
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
            filterButton.setToolTipText(Messages.message("model.message.filter", new String[][] { { "%type%",
                    message.getTypeName() } }));
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
            headline = new JLabel(Messages.message("playerNation", new String[][] { { "%player%", player.getName() },
                    { "%nation%", player.getNationAsString() } }));
        } else if (source instanceof Europe) {
            Europe europe = (Europe) source;
            JButton button = new JButton(europe.getName());
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    getCanvas().showEuropePanel();
                }
            });
            headline = button;
        } else if (source instanceof Market) {
            // Europe europe =
            // getCanvas().getClient().getMyPlayer().getEurope();
            JButton button = new JButton(Messages.message("model.message.marketPrices"));
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    getCanvas().showEuropePanel();
                }
            });
            headline = button;
        } else if (source instanceof Colony) {
            final Colony colony = (Colony) source;
            JButton button = new JButton(colony.getName());
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    getCanvas().showColonyPanel(colony);
                }
            });
            headline = button;
        } else if (source instanceof Unit) {
            headline = new JLabel(((Unit) source).getName());
        } else if (source instanceof Nameable) {
            headline = new JLabel(((Nameable) source).getName());
        } else {
            headline = new JLabel(source.toString());
        }

        headline.setFont(smallHeaderFont);
        headline.setOpaque(false);
        headline.setForeground(LINK_COLOR);
        headline.setBorder(BorderFactory.createEmptyBorder());
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

        try {
            String input = Messages.message(message.getMessageID());
            int start = input.indexOf('%');
            if (start == -1) {
                // no variables present
                insertText(input.substring(0));
                return;
            } else if (start > 0) {
                // output any string before the first occurence of '%'
                insertText(input.substring(0, start));
            }
            int end;

            loop: while ((end = input.indexOf('%', start + 1)) >= 0) {
                String var = input.substring(start, end + 1);
                for (String[] item : message.getData()) {
                    if (item == null || item.length != 2) {
                        logger.warning("Data has a wrong format for message: " + message);
                    } else if (item[0] == null || item[1] == null) {
                        logger.warning("Data in model message is 'null': " + message + ", " +
                                       item[0] + ", " + item[1]);
                    } else if (var.equals(item[0])) {
                        // found variable to replace
                        if (var.equals("%colony%")) {
                            insertColonyButton(item[1]);
                        } else if ((var.equals("%unit%") ||
                                    var.equals("%newName%")) &&
                                   message.getSource() instanceof Unit) {
                            insertUnitButton(item[1], (Unit) message.getSource());
                        } else {
                            insertText(item[1]);
                        }
                        start = end + 1;
                        continue loop;
                    }
                }

                // found no variable to replace: either a single '%', or
                // some unnecessary variable
                insertText(input.substring(start, end));
                start = end;
            }

            // output any string after the last occurence of '%'
            if (start < input.length()) {
                insertText(input.substring(start));
            }
        } catch(Exception e) {
            logger.warning(e.toString());
        }
        

    }

    private void insertText(String text) throws Exception {
        document.insertString(document.getLength(), text,
                              document.getStyle("regular"));
    }


    private void insertColonyButton(String colonyName) throws Exception {
        JButton button = createLinkButton(colonyName);
        button.setActionCommand(colonyName);
        StyleConstants.setComponent(document.getStyle("button"), button);
        document.insertString(document.getLength(), " ", document.getStyle("button"));
    }
        
    private void insertUnitButton(String unitName, Unit unit) throws Exception {
        JButton button = createLinkButton(unitName);
        button.setActionCommand(unit.getID());
        StyleConstants.setComponent(document.getStyle("button"), button);
        document.insertString(document.getLength(), " ", document.getStyle("button"));
    }
        
    private JButton createLinkButton(String name) {
        JButton button = new JButton(name);
        button.setMargin(new Insets(0,0,0,0));
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setAlignmentY(0.8f);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.addActionListener(this);
        return button;
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals("-1")) {
            super.actionPerformed(event);
        } else if (command.startsWith("unit:")) {
            Unit unit = (Unit) freeColClient.getGame().getFreeColGameObject(command);
            Tile tile = unit.getTile();
            if (tile != null) {
                getCanvas().getGUI().setFocus(tile.getPosition());
            }
        } else {
            getCanvas().showColonyPanel(freeColClient.getMyPlayer().getColony(command));
        }
    }

}
