
package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.UnitType;

import cz.autel.dmi.HIGLayout;

/**
 * The panel that allows a user to train people in Europe.
 */

public final class TrainDialog extends FreeColDialog implements ActionListener{
    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(TrainDialog.class.getName());

    private static final int TRAIN_CANCEL = -1;

    private static final JButton cancel = new JButton(Messages.message("trainDialog.cancel"));

    @SuppressWarnings("unused")
    private final Canvas parent;
    private final FreeColClient freeColClient;
    private final InGameController inGameController;

    private class NumberedUnitType {

        public final UnitType type;
        public final int index;

        public NumberedUnitType(UnitType type, int index) {
            this.type = type;
            this.index = index;
        }
    }

    private static final ArrayList<NumberedUnitType> trainableUnits = new ArrayList<NumberedUnitType>();
    private static final ArrayList<JButton> buttons = new ArrayList<JButton>();

    private static final Comparator<NumberedUnitType> priceComparator = new Comparator<NumberedUnitType>() {
        public int compare(NumberedUnitType type1, NumberedUnitType type2) {
            return type1.type.price - type2.type.price;
        }
    };

    /**
     * The constructor to use.
     */
    public TrainDialog(Canvas parent) {
        super();
        this.parent = parent;
        this.freeColClient = parent.getClient();
        this.inGameController = freeColClient.getInGameController();
        setFocusCycleRoot(true);

        ImageLibrary library = (ImageLibrary) parent.getImageProvider();

        int numberOfTypes = FreeCol.specification.numberOfUnitTypes();
        for (int type = 0; type < numberOfTypes; type++) {
            UnitType unitType = FreeCol.specification.unitType(type);
            if (unitType.price > 0 && unitType.skill > 0) {
                trainableUnits.add(new NumberedUnitType(unitType, type));
            }
        }
        Collections.sort(trainableUnits, priceComparator);
        
        setLayout(new HIGLayout(new int[] {0}, new int[] {0, margin, 0, margin, 0}));

        int rows = trainableUnits.size();
        if (rows % 2 == 0) {
            rows--;
        }

        int[] widths = new int[] {0, margin, 0, margin, 0, margin, 0};
        int[] heights = new int[rows];
        
        for (int index = 0; index < rows/2; index++) {
            heights[2 * index + 1] = margin;
        }

        int[] labelColumn = {1, 5};
        int[] buttonColumn = {3, 7};
        JPanel trainPanel = new JPanel(new HIGLayout(widths, heights));

        int row = 1;
        int counter = 0;
        for (NumberedUnitType unitType : trainableUnits) {
            int graphicsType = ImageLibrary.getUnitGraphicsType(unitType.index, false, false, 0, false);
            JButton newButton = new JButton(unitType.type.name,
                                            library.getScaledUnitImageIcon(graphicsType, 0.66f));
            newButton.setActionCommand(String.valueOf(unitType.index));
            newButton.addActionListener(this);
            newButton.setIconTextGap(margin);
            buttons.add(newButton);
            trainPanel.add(new JLabel(String.valueOf(unitType.type.price)),
                           higConst.rc(row, labelColumn[counter]));
            trainPanel.add(newButton, higConst.rc(row, buttonColumn[counter]));
            if (counter == 1) {
                counter = 0;
                row += 2;
            } else {
                counter = 1;
            }
        }

        JLabel question = new JLabel(Messages.message("trainDialog.clickOn"));

        cancel.setActionCommand(String.valueOf(TRAIN_CANCEL));
        cancel.addActionListener(this);

        add(question, higConst.rc(1, 1, ""));
        add(trainPanel, higConst.rc(3, 1));
        add(cancel, higConst.rc(5, 1, ""));

        setSize(getPreferredSize());

    }


    public void requestFocus() {
        cancel.requestFocus();
    }

                

    /**
     * Updates this panel's labels so that the information it displays is up to date.
     */
    public void initialize() {
        Player player = freeColClient.getMyPlayer();
        int numberOfTypes = trainableUnits.size();
        for (int index = 0; index < numberOfTypes; index++) {
            if (trainableUnits.get(index).type.price > player.getGold()) {
                buttons.get(index).setEnabled(false);
            } else {
                buttons.get(index).setEnabled(true);
            }
        }
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
            int value = Integer.valueOf(command).intValue();
            if (value == TRAIN_CANCEL) {
                setResponse(new Boolean(false));
            } else {
                inGameController.trainUnitInEurope(value);
                setResponse(new Boolean(true));
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid action number");
            setResponse(new Boolean(false));
        }
    }
}


