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

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the ContinentalCongress Report.
 */
public final class ReportContinentalCongressPanel extends ReportPanel {



    static final String title = Messages.message("report.continentalCongress.title");

    static final String none = Messages.message("report.continentalCongress.none");

    private final ReportProductionPanel productionPanel;

    private final JPanel summaryPanel;

    private final JPanel fatherPanel;


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportContinentalCongressPanel(Canvas parent) {
        super(parent, title);

        int[] widths = { 0 };
        int[] heights = { 0, 12, 0, 12, 0 };
        reportPanel.setLayout(new HIGLayout(widths, heights));

        summaryPanel = new JPanel();
        summaryPanel.setOpaque(false);
        fatherPanel = new JPanel(new GridLayout(0, 4));
        fatherPanel.setOpaque(false);
        productionPanel = new ReportProductionPanel(Goods.BELLS, getCanvas(), this);
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = getCanvas().getClient().getMyPlayer();

        // Display Panel
        productionPanel.initialize();
        summaryPanel.removeAll();
        fatherPanel.removeAll();
        reportPanel.removeAll();

        // summary
        summaryPanel.add(new JLabel(Messages.message("report.continentalCongress.recruiting")), higConst.rc(1, 1));
        if (player.getCurrentFather() == null) {
            summaryPanel.add(new JLabel(none), higConst.rc(1, 3));
        } else {
            FoundingFather father = player.getCurrentFather();
            JLabel currentFatherLabel = new JLabel(father.getName());
            currentFatherLabel.setToolTipText(father.getDescription());
            summaryPanel.add(currentFatherLabel, higConst.rc(1, 3));
            int bells = player.getBells();
            int required = player.getTotalFoundingFatherCost();
            int production = productionPanel.getTotalProduction();

            FreeColProgressBar progressBar = new FreeColProgressBar(getCanvas(), Goods.BELLS);
            progressBar.update(0, required, bells, production);
            summaryPanel.add(progressBar);
        }

        // founding fathers
        if (player.getFatherCount() < 1) {
            JLabel fatherLabel = new JLabel(none);
            fatherLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            fatherPanel.add(fatherLabel);
        } else {
            for (FoundingFather father : FreeCol.getSpecification().getFoundingFathers()) {
                if (player.hasFather(father)) {
                    JLabel fatherLabel = new JLabel(Messages.message(father.getName()));
                    fatherLabel.setToolTipText(Messages.message(father.getDescription()));
                    fatherLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                    fatherPanel.add(fatherLabel);
                }
            }
        }
        fatherPanel.setBorder(BorderFactory.createTitledBorder(player.getNationAsString() + " " + title));

        reportPanel.add(summaryPanel, higConst.rc(1, 1));
        reportPanel.add(fatherPanel, higConst.rc(3, 1));
        reportPanel.add(productionPanel, higConst.rc(5, 1));
    }
}
