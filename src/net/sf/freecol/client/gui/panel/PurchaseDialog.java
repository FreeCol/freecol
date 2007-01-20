package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
 * The panel that allows a user to purchase ships and artillery in Europe.
 */
public final class PurchaseDialog extends FreeColDialog implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(PurchaseDialog.class.getName());

    private static final int PURCHASE_CANCEL = 0,
                                 PURCHASE_ARTILLERY = 1,
                                 PURCHASE_CARAVEL = 2,
                                 PURCHASE_MERCHANTMAN = 3,
                                 PURCHASE_GALLEON = 4,
                                 PURCHASE_PRIVATEER = 5,
                                 PURCHASE_FRIGATE = 6;

    private JButton   artilleryButton,
                                caravelButton,
                                merchantmanButton,
                                galleonButton,
                                privateerButton,
                                frigateButton;
    private JButton cancel;
    private JLabel artilleryLabel = new JLabel("?");
    private final Canvas parent;
    private final FreeColClient freeColClient;
    private final InGameController inGameController;

    /**
     * The constructor to use.
     */
    public PurchaseDialog(Canvas parent) {
        super();
        this.parent = parent;
        this.freeColClient = parent.getClient();
        this.inGameController = freeColClient.getInGameController();
        setFocusCycleRoot(true);
        ActionListener actionListener = this;
        setLayout(new HIGLayout(new int[] {0}, new int[] {0, margin, 0, margin, 0}));

        JTextArea question = getDefaultTextArea(Messages.message("purchaseDialog.clickOn"));

        JLabel  caravelLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.CARAVEL))),
                merchantmanLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MERCHANTMAN))),
                galleonLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.GALLEON))),
                privateerLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.PRIVATEER))),
                frigateLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.FRIGATE)));
        cancel = new JButton(Messages.message("cancel"));
        setCancelComponent(cancel);
            
        artilleryButton = new JButton(Unit.getName(Unit.ARTILLERY));
        caravelButton = new JButton(Unit.getName(Unit.CARAVEL));
        merchantmanButton = new JButton(Unit.getName(Unit.MERCHANTMAN));
        galleonButton = new JButton(Unit.getName(Unit.GALLEON));
        privateerButton = new JButton(Unit.getName(Unit.PRIVATEER));
        frigateButton = new JButton(Unit.getName(Unit.FRIGATE));
        cancel.setActionCommand(String.valueOf(PURCHASE_CANCEL));
        artilleryButton.setActionCommand(String.valueOf(PURCHASE_ARTILLERY));
        caravelButton.setActionCommand(String.valueOf(PURCHASE_CARAVEL));
        merchantmanButton.setActionCommand(String.valueOf(PURCHASE_MERCHANTMAN));
        galleonButton.setActionCommand(String.valueOf(PURCHASE_GALLEON));
        privateerButton.setActionCommand(String.valueOf(PURCHASE_PRIVATEER));
        frigateButton.setActionCommand(String.valueOf(PURCHASE_FRIGATE));

        cancel.addActionListener(actionListener);
        artilleryButton.addActionListener(actionListener);
        caravelButton.addActionListener(actionListener);
        merchantmanButton.addActionListener(actionListener);
        galleonButton.addActionListener(actionListener);
        privateerButton.addActionListener(actionListener);
        frigateButton.addActionListener(actionListener);

        int[] widths = new int[] {0, margin, 0};
        int[] heights = new int[11];
        for (int index = 0; index < 5; index++) {
            heights[2 * index + 1] = margin;
        }
        int buttonColumn = 1;
        int labelColumn = 3;

        JPanel purchasePanel = new JPanel();
        purchasePanel.setLayout(new HIGLayout(widths, heights));

        int row = 1;
        purchasePanel.add(artilleryButton, higConst.rc(row, buttonColumn));
        purchasePanel.add(artilleryLabel, higConst.rc(row, labelColumn));
        row += 2;
        purchasePanel.add(caravelButton, higConst.rc(row, buttonColumn));
        purchasePanel.add(caravelLabel, higConst.rc(row, labelColumn));
        row += 2;
        purchasePanel.add(merchantmanButton, higConst.rc(row, buttonColumn));
        purchasePanel.add(merchantmanLabel, higConst.rc(row, labelColumn));
        row += 2;
        purchasePanel.add(galleonButton, higConst.rc(row, buttonColumn));
        purchasePanel.add(galleonLabel, higConst.rc(row, labelColumn));
        row += 2;
        purchasePanel.add(privateerButton, higConst.rc(row, buttonColumn));
        purchasePanel.add(privateerLabel, higConst.rc(row, labelColumn));
        row += 2;
        purchasePanel.add(frigateButton, higConst.rc(row, buttonColumn));
        purchasePanel.add(frigateLabel, higConst.rc(row, labelColumn));

        add(question, higConst.rc(1, 1));
        add(purchasePanel, higConst.rc(3, 1, ""));
        add(cancel, higConst.rc(5, 1));

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
        if ((freeColClient.getGame() != null) && (player != null)) {

            artilleryLabel.setText(Integer.toString(player.getEurope().getArtilleryPrice()));

            if (player.getEurope().getArtilleryPrice() > player.getGold()) {
                artilleryButton.setEnabled(false);
            } else {
                artilleryButton.setEnabled(true);
            }

            if (Unit.getPrice(Unit.CARAVEL) > player.getGold()) {
                caravelButton.setEnabled(false);
            } else {
                caravelButton.setEnabled(true);
            }

            if (Unit.getPrice(Unit.MERCHANTMAN) > player.getGold()) {
                merchantmanButton.setEnabled(false);
            } else {
                merchantmanButton.setEnabled(true);
            }

            if (Unit.getPrice(Unit.GALLEON) > player.getGold()) {
                galleonButton.setEnabled(false);
            } else {
                galleonButton.setEnabled(true);
            }

            if (Unit.getPrice(Unit.PRIVATEER) > player.getGold()) {
                privateerButton.setEnabled(false);
            } else {
                privateerButton.setEnabled(true);
            }

            if (Unit.getPrice(Unit.FRIGATE) > player.getGold()) {
                frigateButton.setEnabled(false);
            } else {
                frigateButton.setEnabled(true);
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
            switch (Integer.valueOf(command).intValue()) {

            case PURCHASE_CANCEL:
                setResponse(new Integer(-1));
                break;
            case PURCHASE_ARTILLERY:
                inGameController.purchaseUnitFromEurope(Unit.ARTILLERY);
                setResponse(new Integer(Unit.ARTILLERY));
                break;
            case PURCHASE_CARAVEL:
                inGameController.purchaseUnitFromEurope(Unit.CARAVEL);
                setResponse(new Integer(Unit.CARAVEL));
                break;
            case PURCHASE_MERCHANTMAN:
                inGameController.purchaseUnitFromEurope(Unit.MERCHANTMAN);
                setResponse(new Integer(Unit.MERCHANTMAN));
                break;
            case PURCHASE_GALLEON:
                inGameController.purchaseUnitFromEurope(Unit.GALLEON);
                setResponse(new Integer(Unit.GALLEON));
                break;
            case PURCHASE_PRIVATEER:
                inGameController.purchaseUnitFromEurope(Unit.PRIVATEER);
                setResponse(new Integer(Unit.PRIVATEER));
                break;
            case PURCHASE_FRIGATE:
                inGameController.purchaseUnitFromEurope(Unit.FRIGATE);
                setResponse(new Integer(Unit.FRIGATE));
                break;
            default:
                logger.warning("Invalid action command");
                setResponse(new Integer(-1));
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid action number");
            setResponse(new Integer(-1));
        }
    }
}

