package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Unit;
import cz.autel.dmi.HIGLayout;

/**
 * The panel that allows a user to choose which unit will emigrate from Europe.
 */
public final class EmigrationPanel extends FreeColDialog implements ActionListener {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(EmigrationPanel.class.getName());

    private static final int NUMBER_OF_PERSONS = 3;

    private static final JButton[] person = new JButton[NUMBER_OF_PERSONS];

    private static final JLabel question = new JLabel(Messages.message("chooseImmigrant"));


    /**
     * The constructor to use.
     */
    public EmigrationPanel(Canvas parent) {
        super(parent);
        for (int index = 0; index < NUMBER_OF_PERSONS; index++) {
            person[index] = new JButton();
            person[index].setActionCommand(String.valueOf(index));
            person[index].addActionListener(this);
        }
    }

    public void requestFocus() {
        person[0].requestFocus();
    }

    /**
     * Updates this panel's labels so that the information it displays is up to
     * date.
     * 
     * @param europe The Europe Object where we can find the units that are
     *            prepared to emigrate.
     */
    public void initialize(Europe europe) {

        ImageLibrary library = (ImageLibrary) getCanvas().getImageProvider();

        int[] widths = { 0 };
        int[] heights = { 0, margin, 0, margin, 0, margin, 0 };
        int column = 1;

        setLayout(new HIGLayout(widths, heights));

        int row = 1;
        add(question, higConst.rc(row, column));
        row += 2;

        for (int index = 0; index < NUMBER_OF_PERSONS; index++) {
            int unitType = europe.getRecruitable(index);
            int graphicsType = ImageLibrary.getUnitGraphicsType(unitType, false, false, 0, false);
            person[index].setText(Unit.getName(unitType));
            person[index].setIcon(library.getScaledUnitImageIcon(graphicsType, 0.66f));

            add(person[index], higConst.rc(row, column));
            row += 2;
        }

        setSize(getPreferredSize());
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            int action = Integer.valueOf(command).intValue();
            if (action >= 0 && action < NUMBER_OF_PERSONS) {
                setResponse(new Integer(action));
            } else {
                logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }

}
