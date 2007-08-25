package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import cz.autel.dmi.HIGLayout;

/**
 * This panel is used to show information about an Indian settlement.
 */
public final class IndianSettlementPanel extends FreeColDialog implements ActionListener {
    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(EventPanel.class.getName());

    private static final int OK = 0;

    private final JLabel skillLabel, highlyWantedLabel, otherWantedLabel;

    private final JButton okButton;


    /**
     * The constructor that will add the items to this panel.
     */
    public IndianSettlementPanel() {
        int[] w = { 10, 0, 0, 10 };
        int[] h = { 10, 0, 5, 0, 5, 0, 10, 0, 10 };
        setLayout(new HIGLayout(w, h));

        okButton = new JButton(Messages.message("ok"));
        okButton.setActionCommand(String.valueOf(OK));
        okButton.addActionListener(this);

        skillLabel = new JLabel();
        highlyWantedLabel = new JLabel();
        otherWantedLabel = new JLabel();

        add(new JLabel(Messages.message("indianSettlement.learnableSkill") + " "), higConst.rc(2, 2));
        add(skillLabel, higConst.rc(2, 3));
        add(new JLabel(Messages.message("indianSettlement.highlyWanted") + " "), higConst.rc(4, 2));
        add(highlyWantedLabel, higConst.rc(4, 3));
        add(new JLabel(Messages.message("indianSettlement.otherWanted") + " "), higConst.rc(6, 2));
        add(otherWantedLabel, higConst.rc(6, 3));
        add(okButton, higConst.rc(8, 3));
    }

    public void requestFocus() {
        okButton.requestFocus();
    }

    /**
     * Initializes the information that is being displayed on this panel. The
     * information displayed will be based on the given settlement.
     * 
     * @param settlement The IndianSettlement whose information should be
     *            displayed.
     */
    public void initialize(IndianSettlement settlement) {
        // TODO: fix message for learnable skill
        String[] parts = settlement.getLearnableSkill().getId().split("\\.");
        skillLabel.setText(Messages.message("indianSettlement.skill." + parts[parts.length-1]));

        GoodsType[] wantedGoods = settlement.getWantedGoods();
        highlyWantedLabel.setText(Goods.getName(wantedGoods[0]));
        otherWantedLabel.setText(Goods.getName(wantedGoods[1]) + ", " + Goods.getName(wantedGoods[2]));
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
            switch (Integer.valueOf(command).intValue()) {
            case OK:
                setResponse(new Boolean(true));
                break;
            default:
                logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
