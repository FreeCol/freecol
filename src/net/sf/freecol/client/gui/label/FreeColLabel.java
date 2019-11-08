/**
 * Copyright (C) 2002-2019   The FreeCol Team
 *
 * This file is part of FreeCol.
 *
 * FreeCol is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * FreeCol is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.label;

import javax.swing.Icon;
import javax.swing.JLabel;

/**
 * A simple wrapper for {@code JLabel}
 *
 * All members of the net.sf.freecol.client.gui.label
 *      package extend this abstract class.
 *
 * @since 0.11.7
 */
public abstract class FreeColLabel extends JLabel {

    /**
     * Creates a {@code FreeColLabel} instance with
     * no image and with an empty string for the title.
     * <p>
     * The label is centered vertically in its display area.
     * <p>
     * The label's contents, once set, will be displayed on the leading edge
     * of the label's display area.
     */
    public FreeColLabel() {
        super("", null, LEADING);
    }


    /**
     * Creates a {@code FreeColLabel} instance with the
     * specified image. The label is centered vertically
     * and horizontally in its display area.
     *
     * @param image The image to be displayed by the label.
     */
    public FreeColLabel(Icon image) {
        super(null, image, CENTER);
    }
}
