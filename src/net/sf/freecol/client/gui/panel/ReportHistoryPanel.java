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

import java.util.List;

import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Turn;

import net.miginfocom.swing.MigLayout;

/**
 * This panel displays the History Report.
 */
public final class ReportHistoryPanel extends ReportPanel {

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportHistoryPanel(Canvas parent) {

        super(parent, Messages.message("menuBar.report.history"));

        List<HistoryEvent> history = getMyPlayer().getHistory();

        // Display Panel
        reportPanel.removeAll();
        if (history.size() == 0) {
            return;
        }

        reportPanel.setLayout(new MigLayout("wrap 2", "[]20[fill]", ""));

        for (HistoryEvent event : history) {
            reportPanel.add(new JLabel(Messages.message(Turn.getLabel(event.getTurn()))));
            reportPanel.add(getDefaultTextArea(Messages.message(event), 40));
        }
    }
}
