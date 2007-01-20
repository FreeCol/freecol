
package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;

import cz.autel.dmi.HIGLayout;

/**
 * The panel that allows a user to recruit people in Europe.
 */
public final class RecruitDialog extends FreeColDialog implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(RecruitDialog.class.getName());

    private static final int RECRUIT_CANCEL = 0,
                                 RECRUIT_1 = 1,
                                 RECRUIT_2 = 2,
                                 RECRUIT_3 = 3;

        private final JLabel price;
        private final JButton person1,
                              person2,
                              person3;
        private final JButton cancel;
        private final JTextArea question;
    private final Canvas parent;
    private final FreeColClient freeColClient;
    private final InGameController inGameController;


        /**
        * The constructor to use.
        */
        public RecruitDialog(Canvas parent) {
            super();
            this.parent = parent;
            this.freeColClient = parent.getClient();
            this.inGameController = freeColClient.getInGameController();
            setFocusCycleRoot(true);
            ActionListener actionListener = this;

            cancel = new JButton( Messages.message("cancel") );
            setCancelComponent(cancel);

            question = getDefaultTextArea(Messages.message("recruitDialog.clickOn"));
            price = new JLabel();
            person1 = new JButton();
            person2 = new JButton();
            person3 = new JButton();

            person1.setActionCommand(String.valueOf(RECRUIT_1));
            person2.setActionCommand(String.valueOf(RECRUIT_2));
            person3.setActionCommand(String.valueOf(RECRUIT_3));
            cancel.setActionCommand(String.valueOf(RECRUIT_CANCEL));

            person1.addActionListener(actionListener);
            person2.addActionListener(actionListener);
            person3.addActionListener(actionListener);
            cancel.addActionListener(actionListener);

            initialize();

        }


        public void requestFocus() {
            cancel.requestFocus();
        }


        /**
        * Updates this panel's labels so that the information it displays is up to date.
        */
        public void initialize() {
            int recruitPrice = 0;
            Player player = freeColClient.getMyPlayer();
            if ((freeColClient.getGame() != null) && (player != null)) {
                recruitPrice = player.getRecruitPrice();

                person1.setText(Unit.getName(player.getEurope().getRecruitable(1)));
                person2.setText(Unit.getName(player.getEurope().getRecruitable(2)));
                person3.setText(Unit.getName(player.getEurope().getRecruitable(3)));

                if (recruitPrice > player.getGold()) {
                    person1.setEnabled(false);
                    person2.setEnabled(false);
                    person3.setEnabled(false);
                } else {
                    person1.setEnabled(true);
                    person2.setEnabled(true);
                    person3.setEnabled(true);
                }
            }

            price.setText(Messages.message("recruitDialog.price", 
                                           new String[][] {{"%amount%", String.valueOf(recruitPrice)}}));

            int[] widths = new int[] {0};
            int[] heights = new int[11];
            for (int index = 0; index < 5; index++) {
                heights[2 * index + 1] = margin;
            }
            setLayout(new HIGLayout(widths, heights));
            int row = 1;

            add(question, higConst.rc(row, 1));
            row += 2;
            add(price, higConst.rc(row, 1));
            row += 2;
            add(person1, higConst.rc(row, 1));
            row += 2;
            add(person2, higConst.rc(row, 1));
            row += 2;
            add(person3, higConst.rc(row, 1));
            row += 2;
            add(cancel, higConst.rc(row, 1));

            setSize(getPreferredSize());
        }


        /**
        * Analyzes an event and calls the right external methods to take
        * care of the user's request.
        *
        * @param event The incoming action event
        */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            try {
                switch (Integer.valueOf(command).intValue()) {

                case RECRUIT_CANCEL:
                    setResponse(new Boolean(false));
                    break;
                case RECRUIT_1:
                    inGameController.recruitUnitInEurope(1);
                    setResponse(new Boolean(true));
                    break;
                case RECRUIT_2:
                    inGameController.recruitUnitInEurope(2);
                    setResponse(new Boolean(true));
                    break;
                case RECRUIT_3:
                    inGameController.recruitUnitInEurope(3);
                    setResponse(new Boolean(true));
                    break;
                default:
                    logger.warning("Invalid action command");
                    setResponse(new Boolean(false));
                }
            } catch (NumberFormatException e) {
                logger.warning("Invalid action number");
                setResponse(new Boolean(false));
            }
        }
    }

