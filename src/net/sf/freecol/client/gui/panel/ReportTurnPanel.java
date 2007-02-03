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
        Comparator<ModelMessage> comparator = parent.getClient().getClientOptions().getModelMessageComparator();
        if (comparator != null) {
            Collections.sort(messages, comparator);
        }

        int groupBy = parent.getClient().getClientOptions().getInteger(ClientOptions.MESSAGES_GROUP_BY);
        ArrayList<JLabel> images = new ArrayList<JLabel>();
        ArrayList<JComponent> texts = new ArrayList<JComponent>();
        ArrayList<JButton> buttons = new ArrayList<JButton>();
        Object source = this;
        int type = -1;
        for (final ModelMessage message : messages) {
            if (groupBy == ClientOptions.MESSAGES_GROUP_BY_SOURCE &&
                message.getSource() != source) {
                source = message.getSource();
                images.add(new JLabel());
                texts.add(getHeadline(source));
                buttons.add(null);
            } else if (groupBy == ClientOptions.MESSAGES_GROUP_BY_TYPE &&
                       message.getType() != type) {
                type = message.getType();
                images.add(new JLabel());
                JLabel headline = new JLabel(message.getTypeName());
                headline.setFont(headline.getFont().deriveFont(Font.BOLD, 16f));
                texts.add(headline);
                buttons.add(null);
            }
            final JLabel label = new JLabel();
            if (message.getDisplay() != null) {
                label.setIcon(parent.getImageIcon(message.getDisplay()));
            }
            images.add(label);
            final JTextArea textArea = new JTextArea();
            try {
                String text = Messages.message(message.getMessageID(),
                                               message.getData());
                textArea.setText(text);
                textArea.setColumns(50);
                textArea.setOpaque(false);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setFocusable(false);
                textArea.setFont(defaultFont);
                texts.add(textArea);
            } catch (MissingResourceException e) {
                logger.warning("could not find message with id: " + message.getMessageID() + ".");
            }
            if (message.getType() == ModelMessage.WAREHOUSE_CAPACITY) {
                JButton button = new JButton(Messages.message("model.message.ignore"));
                button.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            boolean flag = label.isEnabled();
                            freeColClient.getInGameController().ignoreMessage(message, flag);
                            textArea.setEnabled(!flag);
                            label.setEnabled(!flag);
                        }
                    });
                buttons.add(button);
            } else {
                buttons.add(null);
            }

        }

        // Display Panel
        reportPanel.removeAll();

        int[] widths = new int[] {0, margin, 0, margin, 0};
        int[] heights = new int[2 * texts.size() - 1];
        int imageColumn = 1;
        int textColumn = 3;
        int buttonColumn = 5;

        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }

        HIGLayout layout = new HIGLayout(widths, heights);
        layout.setColumnWeight(textColumn, 1);
        reportPanel.setLayout(layout);

        int row = 1;
        for (int index = 0; index < texts.size(); index++) {
            reportPanel.add(images.get(index),
                            higConst.rc(row, imageColumn));
            reportPanel.add(texts.get(index),
                            higConst.rc(row, textColumn, "l"));
            if (buttons.get(index) != null) {
                reportPanel.add(buttons.get(index),
                                higConst.rc(row, buttonColumn));
            }
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
                        parent.showEuropePanel();
                    }
                });
            headline = button;
        } else if (source instanceof Market) {
            Europe europe = parent.getClient().getMyPlayer().getEurope();
            JButton button = new JButton(Messages.message("model.message.marketPrices"));
            button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        parent.showEuropePanel();
                    }
                });
            headline = button;
        } else if (source instanceof Colony) {
            final Colony colony = (Colony) source;
            JButton button = new JButton(colony.getName());
            button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        parent.showColonyPanel(colony);
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

        headline.setFont(headline.getFont().deriveFont(Font.BOLD, 16f));
        return headline;
    }


}

