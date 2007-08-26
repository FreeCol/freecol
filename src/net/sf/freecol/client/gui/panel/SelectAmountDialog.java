package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextArea;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Unit;
import cz.autel.dmi.HIGLayout;

/**
 * The panel that allows a user to recruit people in Europe.
 */
public final class SelectAmountDialog extends FreeColDialog implements ActionListener {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(SelectAmountDialog.class.getName());

    private static final int SELECT_CANCEL = -1;

    private final JButton cancel;

    private final JTextArea question;

    private final JComboBox comboBox;

    private final FreeColClient freeColClient;

    private final InGameController inGameController;


    /**
     * The constructor to use.
     */
    public SelectAmountDialog(Canvas parent, GoodsType goodsType, int available, boolean needToPay) {
        super(parent);

        this.freeColClient = parent.getClient();
        this.inGameController = freeColClient.getInGameController();
        setFocusCycleRoot(true);

        question = getDefaultTextArea(Messages.message("goodsTransfer.text"));

        if (needToPay) {
            int gold = parent.getClient().getMyPlayer().getGold();
            int price = parent.getClient().getMyPlayer().getMarket().costToBuy(goodsType);
            available = Math.min(available, gold/price);
        }
        int[] amounts = {20, 40, 50, 60, 80, 100};

        Vector<Integer> values = new Vector<Integer>();
        for (int index = 0; index < amounts.length; index++) {
            if (amounts[index] < available) {
                values.add(amounts[index]);
            } else {
                values.add(available);
                break;
            }
        }

        comboBox = new JComboBox(values);
        comboBox.setEditable(true);
        comboBox.addActionListener(this);

        cancel = new JButton(Messages.message("cancel"));
        enterPressesWhenFocused(cancel);
        cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    setResponse(new Integer(SELECT_CANCEL));
                }
            });
        setCancelComponent(cancel);

        initialize();

    }

    public void requestFocus() {
        cancel.requestFocus();
    }

    /**
     * Updates this panel's labels so that the information it displays is up to
     * date.
     */
    public void initialize() {

        int[] widths = new int[] { 0 };
        int[] heights = new int[5];
        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }
        setLayout(new HIGLayout(widths, heights));

        int row = 1;
        int column = 1;

        add(question, higConst.rc(row, column));
        row += 2;
        add(comboBox, higConst.rc(row, column));
        row += 2;
        add(cancel, higConst.rc(row, column, ""));

        setSize(getPreferredSize());

    }

    /**
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     * 
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        setResponse(comboBox.getSelectedItem());
    }
}
