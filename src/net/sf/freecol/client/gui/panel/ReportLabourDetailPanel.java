/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.event.ActionListener;

import javax.swing.JPanel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;


/**
 * This panel displays the Labour Report.
 */
public final class ReportLabourDetailPanel extends ReportPanel implements ActionListener {
    
    /**
     * The constructor that will add the items to this panel.
     * @param freeColClient 
     * @param parent The parent of this panel.
     */
    public ReportLabourDetailPanel(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, Messages.message("report.labour.details"));
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void setDetailPanel(JPanel detailPanel) {
        reportPanel.add(detailPanel);
    }

}
