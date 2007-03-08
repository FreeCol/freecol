
package net.sf.freecol.client.gui.panel;


import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;


/**
 * This panel displays the Foreign Affairs Report.
 */
public final class ReportIndianPanel extends ReportPanel implements ActionListener {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportIndianPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.indian"));
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = getCanvas().getClient().getMyPlayer();
        Iterator opponents = getCanvas().getClient().getGame().getPlayers().iterator();
        // Display Panel
        reportPanel.removeAll();
        reportPanel.setLayout(new GridLayout(8, 1));
        while (opponents.hasNext()) {
            buildIndianAdvisorLabel(player, (Player) opponents.next());
        }
        reportPanel.doLayout();
    }

    /**
     * 
     */
    private void buildIndianAdvisorLabel(Player player, Player opponent) {
        if (opponent.isEuropean() || opponent.isREF()) {
          return;
        }
        if (opponent.isDead()) {
          return;
        }
        if (!player.hasContacted(opponent.getNation())) {
          return;
        }
        String report = "<html><p align=center><b>" +
                        opponent.getNationAsString() + "</b>";

        int settlementCount = opponent.getSettlements().size();
        report += "<p>" + settlementCount + " known camps";
        String tensionString = opponent.getTension(player).toString();
        report += "<p>" + Messages.message("tension") + ": " + tensionString;
        report += "</html>";
        JLabel label;
        label = new JLabel(report);
        label.setVerticalAlignment(SwingConstants.TOP);
        label.setVerticalTextPosition(SwingConstants.TOP);
        reportPanel.add(label);
    }
}
