/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.logging.Logger;

import javax.swing.JLabel;


/**
 * Represents an image selection that can be selected and
 * dragged/dropped to/from Swing components.
 */
public final class ImageSelection implements Transferable {
    
    /** The {@code JLabel} to transfer. */
    private final JLabel label;


    /**
     * Create a new image selection.
     *
     * @param label The {@code JLabel} that this selection should hold.
     */
    public ImageSelection(JLabel label) {
        this.label = label;
    }


    // Implement Transferable
    // This Transferable is only intended to work with our
    // DefaultTransferHandler flavor.

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getTransferData(DataFlavor flavor) {
        return (isDataFlavorSupported(flavor)) ? this.label : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { DefaultTransferHandler.flavor };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DefaultTransferHandler.flavor);
    }
}
