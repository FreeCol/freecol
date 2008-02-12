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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.UnitType;

import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Foreign Affairs Report.
 */
public final class ReportIndianPanel extends ReportPanel implements ActionListener {

    public static final int EXTRA_ROWS = 10;
    
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
        // Display Panel
        reportPanel.removeAll();
        reportPanel.setLayout(new GridLayout(8, 1));
        for (Player opponent : getCanvas().getClient().getGame().getPlayers()) {
            buildIndianAdvisorPanel(player, opponent);
        }
        reportPanel.doLayout();
    }

    /**
     * Describe <code>buildIndianAdvisorPanel</code> method here.
     *
     * @param player a <code>Player</code> value
     * @param opponent a <code>Player</code> value
     */
    private void buildIndianAdvisorPanel(Player player, Player opponent) {
        if (opponent.isEuropean() || opponent.isREF()) {
          return;
        }
        if (opponent.isDead()) {
          return;
        }
        if (!player.hasContacted(opponent)) {
          return;
        }

        int numberOfSettlements = opponent.getSettlements().size();
        int heights[] = new int[2 * (numberOfSettlements + EXTRA_ROWS)];
        int widths[] = new int[] { 0, 10, 0 };
        int labelColumn = 1;
        int valueColumn = 3;
        int row = 1;

        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = 3;
        }
        heights[EXTRA_ROWS - 1] = 16;
        heights[EXTRA_ROWS + 1] = 6;

        JPanel result = new JPanel(new HIGLayout(widths, heights));

        result.add(new JLabel(Messages.message("report.indian.nameOfTribe")),
                   higConst.rc(row, labelColumn));
        result.add(new JLabel(opponent.getNationAsString()),
                   higConst.rc(row, valueColumn));
        row += 2;
        result.add(new JLabel(Messages.message("report.indian.chieftain")),
                   higConst.rc(row, labelColumn));
        result.add(new JLabel(opponent.getName()),
                   higConst.rc(row, valueColumn));
        row += 2;
        result.add(new JLabel(Messages.message("report.indian.typeOfSettlements")),
                   higConst.rc(row, labelColumn));
        result.add(new JLabel(String.valueOf(((IndianNationType) opponent.getNationType()).getTypeOfSettlement())),
                   higConst.rc(row, valueColumn, "r"));
        row += 2;
        result.add(new JLabel(Messages.message("report.indian.numberOfSettlements")),
                   higConst.rc(row, labelColumn));
        result.add(new JLabel(String.valueOf(numberOfSettlements)),
                   higConst.rc(row, valueColumn, "r"));
        row += 2;
        result.add(new JLabel(Messages.message("report.indian.tension")),
                   higConst.rc(row, labelColumn));
        result.add(new JLabel(opponent.getTension(player).toString()),
                   higConst.rc(row, valueColumn));
        row += 2;
        result.add(new JLabel(Messages.message("report.indian.location")),
                   higConst.rc(row, labelColumn));
        result.add(new JLabel(Messages.message("report.indian.skillTaught")),
                   higConst.rc(row, valueColumn));
        row += 2;

        for (IndianSettlement settlement : opponent.getIndianSettlements()) {
            String locationName = settlement.getLocationName() + 
                " (" + settlement.getTile().getX() + ", " +
                settlement.getTile().getY() + ")";
            UnitType skillType = settlement.getLearnableSkill();
            String skill = Messages.message("indianSettlement.skillUnknown");
            if (skillType != null) {
                skill = skillType.getName();
            }
            result.add(new JLabel(locationName),
                       higConst.rc(row, labelColumn));
            result.add(new JLabel(skill),
                       higConst.rc(row, valueColumn));
            row += 2;
        }
        reportPanel.add(result);
    }
}
