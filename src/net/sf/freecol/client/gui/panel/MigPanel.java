/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import java.awt.LayoutManager;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;


/**
 * A panel that clears its layout on close, working around a bug in
 * some versions of MigLayout.
 */
public class MigPanel extends JPanel {

    private String uiClassId;


    public MigPanel(String uiClassId) {
        assert SwingUtilities.isEventDispatchThread();
        
        this.uiClassId = uiClassId;
    }

    public MigPanel(LayoutManager layout) {
        super(layout);

        assert SwingUtilities.isEventDispatchThread();
        
        this.uiClassId = null;
    }

    protected MigPanel(String uiClassId, LayoutManager layout) {
        super(layout);
        
        assert SwingUtilities.isEventDispatchThread();
        
        this.uiClassId = uiClassId;
    }


    // Override JPanel
    
    public MigLayout getMigLayout() {
    	return (MigLayout) getLayout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUIClassID() {
        return (uiClassId != null) ? uiClassId : super.getUIClassID();
    }
}
