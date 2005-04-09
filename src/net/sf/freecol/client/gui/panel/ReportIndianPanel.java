package net.sf.freecol.client.gui.panel;

import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.model.Player;

/**
 * This panel displays the Foreign Affairs Report.
 */
public final class ReportIndianPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportIndianPanel(Canvas parent) {
        super(parent, "Indian Advisor");
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = parent.getClient().getMyPlayer();
        Iterator opponents = parent.getClient().getFreeColServer().getGame().getPlayers().iterator();
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
        int settlementCount = 0;
        Iterator colonies = opponent.getIndianSettlementIterator();
        while (colonies.hasNext()) {
            settlementCount++;
            colonies.next();
        }
        report += "<p>" + settlementCount + " camps";
        int tension = opponent.getTension(player);
        String tensionString = null;
        if (tension < Player.TENSION_HAPPY) {
            tensionString = "Happy";
        }
        else if (tension < 5 * Player.TENSION_HAPPY) {
            tensionString = "Wary";
        }
        else {
            tensionString = "Angry";
        }
        report += "<p>Tension: " + tensionString;
        report += "</html>";
        JLabel label;
        label = new JLabel(report);
        label.setVerticalAlignment(JLabel.TOP);
        label.setVerticalTextPosition(JLabel.TOP);
        reportPanel.add(label);
    }
}
