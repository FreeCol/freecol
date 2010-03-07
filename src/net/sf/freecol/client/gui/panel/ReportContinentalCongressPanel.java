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


import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;

import net.miginfocom.swing.MigLayout;


/**
 * This panel displays the ContinentalCongress Report.
 */
public final class ReportContinentalCongressPanel extends ReportPanel {

    static final String title = Messages.message("report.continentalCongress.title");

    static final String none = Messages.message("report.continentalCongress.none");

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportContinentalCongressPanel(Canvas parent) {
        super(parent, title);

        reportPanel.setLayout(new MigLayout("fill, wrap 3", "", ""));

        Player player = getMyPlayer();

        JLabel recruiting = new JLabel(Messages.message("report.continentalCongress.recruiting"));
        recruiting.setFont(smallHeaderFont);
        reportPanel.add(recruiting, "align center, growy");
        if (player.getCurrentFather() == null) {
            reportPanel.add(new JLabel(none), "wrap 20");
        } else {
            FoundingFather father = player.getCurrentFather();
            JLabel currentFatherLabel = new JLabel(Messages.message(father.getNameKey()),
                                                   new ImageIcon(getLibrary().getFoundingFatherImage(father)),
                                                   JLabel.CENTER);
            currentFatherLabel.setToolTipText(Messages.message(father.getDescriptionKey()));
            currentFatherLabel.setVerticalTextPosition(JLabel.TOP);
            currentFatherLabel.setHorizontalTextPosition(JLabel.CENTER);
            reportPanel.add(currentFatherLabel);
            GoodsType bellsType = FreeCol.getSpecification().getGoodsType("model.goods.bells");
            FreeColProgressBar progressBar = new FreeColProgressBar(getCanvas(), bellsType);
            int total = 0;
            for (Colony colony : player.getColonies()) {
                total += colony.getProductionNetOf(bellsType);
            }
            int bells = player.getLiberty();
            int required = player.getTotalFoundingFatherCost();
            progressBar.update(0, required, bells, total);
            reportPanel.add(progressBar, "wrap 20");
        }

        // founding fathers
        if (player.getFatherCount() > 0) {
            for (FoundingFather father : FreeCol.getSpecification().getFoundingFathers()) {
                if (player.hasFather(father)) {
                    JLabel fatherLabel = new JLabel(Messages.message(father.getNameKey()),
                                                   new ImageIcon(getLibrary().getFoundingFatherImage(father)),
                                                   JLabel.CENTER);
                    fatherLabel.setVerticalTextPosition(JLabel.TOP);
                    fatherLabel.setHorizontalTextPosition(JLabel.CENTER);
                    fatherLabel.setToolTipText(Messages.message(father.getDescriptionKey()));
                    reportPanel.add(fatherLabel);
                }
            }
        }
    }
}
