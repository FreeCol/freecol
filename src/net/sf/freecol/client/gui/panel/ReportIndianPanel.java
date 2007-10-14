/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


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
        Iterator<Player> opponents = getCanvas().getClient().getGame().getPlayers().iterator();
        // Display Panel
        reportPanel.removeAll();
        reportPanel.setLayout(new GridLayout(8, 1));
        while (opponents.hasNext()) {
            buildIndianAdvisorLabel(player, opponents.next());
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
        if (!player.hasContacted(opponent)) {
          return;
        }
        String report = "<html><p align=center><b>" +
                        opponent.getNationAsString() + "</b>";

        int settlementCount = opponent.getSettlements().size();
        report += "<p>" + Messages.message("report.indian.knownCamps", 
                "%number%", String.valueOf(settlementCount));
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
