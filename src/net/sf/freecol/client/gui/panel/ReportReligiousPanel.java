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

import java.util.List;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.WorkLocation;


/**
 * This panel displays the Religious Report.
 *
 * TODO: remove hardcoded "crosses".
 */
public final class ReportReligiousPanel extends ReportPanel {

    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ReportReligiousPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportReligionAction");

        reportPanel.setLayout(new MigLayout("wrap 5, gap 20 20", "", ""));
        reportPanel.add(GUI.localizedLabel("crosses"));

        final Player player = getMyPlayer();
        final Specification spec = getSpecification();
        final GoodsType crosses = spec.getGoodsType("model.goods.crosses");

        FreeColProgressBar progressBar
            = new FreeColProgressBar(crosses);
        reportPanel.add(progressBar, "span");

        List<Colony> colonies = freeColClient.getMySortedColonies();
        for (Colony colony : colonies) {
            WorkLocation wl = colony.getWorkLocationForProducing(crosses);
            if (wl == null
                || !(wl instanceof Building)) // FIXME: non-OO
                continue;
            Building building = (Building)wl;
            reportPanel.add(createColonyButton(colony),
                            "split 2, flowy, align center");
            BuildingPanel bp = new BuildingPanel(getFreeColClient(), building);
            bp.initialize();
            reportPanel.add(bp);
        }

        progressBar.update(0, player.getImmigrationRequired(),
                           player.getImmigration(),
                           player.getTotalImmigrationProduction());
    }
}

