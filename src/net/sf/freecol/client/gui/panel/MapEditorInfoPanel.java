/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JPanel;
import java.util.List;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.MapTransform;


/** Panel for ending the turn. */
public final class MapEditorInfoPanel extends FreeColPanel
    implements PropertyChangeListener {

    /** The map transform to display. */
    private MapTransform mapTransform = null;

    
    /**
     * Build a new map editor information panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public MapEditorInfoPanel(FreeColClient freeColClient) {
        super(freeColClient, null, new BorderLayout());

        this.mapTransform = null;

        this.setSize(InfoPanel.PREFERRED_SIZE);
        this.setBorder(null);
        this.setOpaque(false);
    }

    /**
     * Updates this panel to use a new map transform.
     *
     * @param mapTransform The displayed {@code MapTransform} (may be null).
     */
    public void update(MapTransform mapTransform) {
        if (this.mapTransform != mapTransform) {
            this.mapTransform = mapTransform;
            update();
        }
    }

    /**
     * Unconditionally update this panel.
     */
    private void update() {
        removeAll();

        final JPanel p = (this.mapTransform == null) ? null
            : this.mapTransform.getDescriptionPanel();
        if (p != null) {
            p.setOpaque(false);
            final Dimension d = p.getPreferredSize();
            p.setBounds(0, (this.getHeight() - d.height)/2,
                        this.getWidth(), d.height);
            this.add(p, BorderLayout.CENTER);
        }

        revalidate();
    }    

    // Interface PropertyChangeListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        update();
    }
}
