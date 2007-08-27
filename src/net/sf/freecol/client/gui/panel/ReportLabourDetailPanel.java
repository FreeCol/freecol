package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import cz.autel.dmi.HIGLayout;
import net.sf.freecol.common.model.UnitType;

/**
 * This panel displays the Labour Report.
 */
public final class ReportLabourDetailPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private Player player;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportLabourDetailPanel(Canvas parent) {
        super(parent, Messages.message("report.labour.details"));
        player = parent.getClient().getMyPlayer();
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize(JPanel detailPanel, UnitType unitType) {
        reportPanel.setLayout(new HIGLayout(new int[] {0, 12, 0}, new int[] {0, 12, 0}));

        reportPanel.add(createUnitLabel(unitType), higConst.rc(1, 1, "t"));
        reportPanel.add(detailPanel, higConst.rc(1, 3));
        reportPanel.add(new JLabel(Messages.message("report.labour.canTrain")),
                        higConst.rc(3, 3, "l"));
    }

    private JLabel createUnitLabel(UnitType unitType) {
        int tools = 0;
        if (unitType.hasAbility("model.ability.expertPioneer")) {
            tools = 20;
        }
        int imageType = ImageLibrary.getUnitGraphicsType(unitType.getIndex(), false, false, tools, false);
        JLabel unitLabel = new JLabel(getLibrary().getUnitImageIcon(imageType));
        return unitLabel;
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
        } else if (command.equals(player.getEurope().getName())) {
            getCanvas().showEuropePanel();
        } else {
            getCanvas().showColonyPanel(player.getColony(command));
        }
    }
}
