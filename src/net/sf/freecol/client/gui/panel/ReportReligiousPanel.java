/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.awt.Font;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;


/**
 * This panel displays the Religious Report.
 */
public final class ReportReligiousPanel extends ReportPanel {

    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ReportReligiousPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportReligionAction");

        final Font font = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.SMALLER, Font.BOLD,
            freeColClient.getGUI().getImageLibrary().getScaleFactor());
        final Player player = getMyPlayer();
        final Specification spec = getSpecification();

        reportPanel.setLayout(new MigLayout("wrap 6, fill", "center"));

        for (GoodsType gt : spec.getImmigrationGoodsTypeList()) {
            JLabel crosses = Utility.localizedLabel(gt);
            crosses.setFont(font);
            reportPanel.add(crosses, "span, split 2");
            FreeColProgressBar progressBar = new FreeColProgressBar(gt, 0,
                player.getImmigrationRequired(), player.getImmigration(),
                player.getTotalImmigrationProduction());
            reportPanel.add(progressBar, "span");

            for (Colony colony : freeColClient.getMySortedColonies()) {
                Building building = colony.getBuildingForProducing(gt);
                if (building != null) {
                    reportPanel.add(createColonyButton(colony),
                        "split 2, flowy");
                    BuildingPanel bp = new BuildingPanel(getFreeColClient(),
                                                         building);
                    bp.initialize();
                    reportPanel.add(bp);
                }
            }
        }
    }
}
