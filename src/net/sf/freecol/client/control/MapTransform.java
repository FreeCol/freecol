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

package net.sf.freecol.client.control;

import javax.swing.JPanel;

import net.sf.freecol.common.model.Tile;


/**
 * Represents a transformation that can be applied to
 * a {@code Tile}.
 *
 * @see #transform(Tile)
 */
public abstract class MapTransform {

    /**
     * A panel with information about this transformation.
     */
    private JPanel descriptionPanel = null;

    /**
     * Applies this transformation to the given tile.
     *
     * @param t The {@code Tile} to be transformed,
     */
    public abstract void transform(Tile t);

    /**
     * A panel with information about this transformation.  This panel
     * is currently displayed on the
     * {@link net.sf.freecol.client.gui.panel.InfoPanel} when selected,
     * but might be used elsewhere as well.
     *
     * @return The panel or {@code null} if no panel
     *      has been set.
     */
    public JPanel getDescriptionPanel() {
        return descriptionPanel;
    }

    /**
     * Sets a panel that can be used for describing this
     * transformation to the user.
     *
     * @param descriptionPanel The panel.
     */
    public void setDescriptionPanel(JPanel descriptionPanel) {
        this.descriptionPanel = descriptionPanel;
    }
}
