package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;

import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Unit;

/**
* The panel that allows a user to choose which unit will emigrate from Europe.
*/
public final class EmigrationPanel extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(EmigrationPanel.class.getName());

    private final JButton   person1,
                            person2,
                            person3;

    private static final int    EMIGRATE_1 = 1,
                                EMIGRATE_2 = 2,
                                EMIGRATE_3 = 3;

    /**
    * The constructor to use.
    */
    public EmigrationPanel() {
        //setFocusCycleRoot(true);

        JLabel  question = new JLabel("Choose which unit will emigrate from Europe.");

        person1 = new JButton();
        person2 = new JButton();
        person3 = new JButton();

        question.setSize(300, 20);
        person1.setSize(200, 20);
        person2.setSize(200, 20);
        person3.setSize(200, 20);

        question.setLocation(10, 10);
        person1.setLocation(60, 40);
        person2.setLocation(60, 65);
        person3.setLocation(60, 90);

        setLayout(null);

        person1.setActionCommand(String.valueOf(EMIGRATE_1));
        person2.setActionCommand(String.valueOf(EMIGRATE_2));
        person3.setActionCommand(String.valueOf(EMIGRATE_3));

        person1.addActionListener(this);
        person2.addActionListener(this);
        person3.addActionListener(this);

        add(question);
        add(person1);
        add(person2);
        add(person3);

        try {
            BevelBorder border = new BevelBorder(BevelBorder.RAISED);
            setBorder(border);
        }
        catch(Exception e) {
        }

        setSize(320, 130);
    }


    public void requestFocus() {
        person1.requestFocus();
    }


    /**
    * Updates this panel's labels so that the information it displays is up to date.
    * @param europe The Europe Object where we can find the units that are prepared to
    *               emigrate.
    */
    public void initialize(Europe europe) {
        person1.setText(Unit.getName(europe.getRecruitable(1)));
        person2.setText(Unit.getName(europe.getRecruitable(2)));
        person3.setText(Unit.getName(europe.getRecruitable(3)));
    }


    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
                case EMIGRATE_1:
                    setResponse(new Integer(1));
                    break;
                case EMIGRATE_2:
                    setResponse(new Integer(2));
                    break;
                case EMIGRATE_3:
                    setResponse(new Integer(3));
                    break;
                default:
                    logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}