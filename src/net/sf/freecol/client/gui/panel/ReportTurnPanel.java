package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.MissingResourceException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;

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
    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final FreeColClient freeColClient;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     * @param freeColClient The main controller object for the client.
     */
    public ReportTurnPanel(Canvas parent, FreeColClient freeColClient) {
        super(parent, Messages.message("menuBar.report.turn"));
        this.freeColClient = freeColClient;
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
            if (groupBy == ClientOptions.MESSAGES_GROUP_BY_SOURCE &&
                message.getSource() != source) {
                source = message.getSource();
                headlines++;
            } else if (groupBy == ClientOptions.MESSAGES_GROUP_BY_TYPE &&
                       message.getType() != type) {
                type = message.getType();
                headlines++;
            }
        }

        // Display Panel
        reportPanel.removeAll();

        int[] widths = new int[] {0, margin, 0, margin, 0, margin, 0};
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
            if (groupBy == ClientOptions.MESSAGES_GROUP_BY_SOURCE &&
                message.getSource() != source) {
                source = message.getSource();
                reportPanel.add(getHeadline(source), higConst.rc(row, textColumn, "l"));
                row += 2;
            } else if (groupBy == ClientOptions.MESSAGES_GROUP_BY_TYPE &&
                       message.getType() != type) {
                type = message.getType();
                JLabel headline = new JLabel(message.getTypeName());
                headline.setFont(smallHeaderFont);
                reportPanel.add(headline, higConst.rc(row, textColumn, "l"));
                row += 2;
            }
            
            final JLabel label = new JLabel();
            if (message.getDisplay() != null) {
                label.setIcon(getCanvas().getImageIcon(message.getDisplay()));
            }
            reportPanel.add(label, higConst.rc(row, imageColumn, ""));

            final JTextArea textArea = new JTextArea();
            try {
                String text = Messages.message(message.getMessageID(),
                                               message.getData());
                textArea.setText(text);
                textArea.setColumns(42);
                textArea.setOpaque(false);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setFocusable(false);
                textArea.setFont(defaultFont);
                reportPanel.add(textArea, higConst.rc(row, textColumn, "l"));

            } catch (MissingResourceException e) {
                logger.warning("could not find message with id: " + message.getMessageID() + ".");
            }
            if (message.getType() == ModelMessage.WAREHOUSE_CAPACITY) {
                JButton ignoreButton = new JButton("x");
                ignoreButton.setToolTipText(Messages.message("model.message.ignore",
                                                             message.getData()));
                ignoreButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            boolean flag = label.isEnabled();
                            freeColClient.getInGameController().ignoreMessage(message, flag);
                            textArea.setEnabled(!flag);
                            label.setEnabled(!flag);
                        }
                    });
                reportPanel.add(ignoreButton, higConst.rc(row, button1Column, ""));
            }
            JButton filterButton = new JButton("X");
            filterButton.setToolTipText(Messages.message("model.message.filter",
                                                         new String[][] {{"%type%", message.getTypeName()}}));
            final BooleanOption filterOption = options.getBooleanOption(message);
            filterButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        boolean flag = filterOption.getValue();
                        filterOption.setValue(!flag);
                        textArea.setEnabled(!flag);
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
                                                   new String[][] {{"%player%", player.getName()},
                                                                   {"%nation%", player.getNationAsString()}}));
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
            Europe europe = getCanvas().getClient().getMyPlayer().getEurope();
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
        return headline;
    }


}

