package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.MissingResourceException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
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

    private HashMap<String, Colony> colonies;

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

        colonies = new HashMap<String, Colony>();
        Player player = getCanvas().getClient().getMyPlayer();

        for (Colony colony : player.getColonies()) {
            colonies.put(colony.getName(), colony);
        }

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

        StyledDocument doc = textPane.getStyledDocument();
        //Initialize some styles.
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
	
        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "Dialog");
        StyleConstants.setBold(def, true);
	StyleConstants.setFontSize(def, 12);

        Style buttonStyle = doc.addStyle("button", regular);
        StyleConstants.setForeground(buttonStyle, LINK_COLOR);

        String text = Messages.message(message.getMessageID());
        String colonyName = null;
        String[][] data = message.getData();
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                if (data[i] == null || data[i].length != 2) {
                    logger.warning("Data has a wrong format for message: " + message);
                } else if (data[i][0] == null || data[i][1] == null) {
                    logger.warning("Data in model message is 'null': " + message + ", " + data[i][0] + ", "
                            + data[i][1]);
                } else if (data[i][0].equals("%colony%")) {
                    colonyName = data[i][1];
                    continue;
                } else {
                    text = text.replaceAll(data[i][0], data[i][1]);
                }
            }
        }

        int index = text.indexOf("%colony%");
        try {
            if (index == -1) {
                doc.insertString(doc.getLength(),
                                 Messages.message(message.getMessageID(), message.getData()),
                                 doc.getStyle("regular"));
            } else {
                doc.insertString(doc.getLength(), 
                                 text.substring(0, index),
                                 doc.getStyle("regular"));
                StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colonyName));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                doc.insertString(doc.getLength(), 
                                 text.substring(index + "%colony%".length()),
                                 doc.getStyle("regular"));
            }
        } catch(Exception e) {
            logger.warning(e.toString());
        }

        return textPane;
    }


    private JButton createColonyButton(String colonyName) {

        JButton button = new JButton(colonyName);
        button.setMargin(new Insets(0,0,0,0));
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setAlignmentY(0.8f);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setActionCommand(colonyName);
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
        } else {
            getCanvas().showColonyPanel(colonies.get(command));
        }
    }

}
