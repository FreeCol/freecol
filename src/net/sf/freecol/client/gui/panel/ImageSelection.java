
package net.sf.freecol.client.gui.panel;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.logging.Logger;

import javax.swing.JLabel;

/**
* Represents an image selection that can be selected and dragged/dropped to/from
* Swing components.
*/
public final class ImageSelection implements Transferable {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static Logger logger = Logger.getLogger(ImageSelection.class.getName());

    //private static final DataFlavor[] flavors = {DataFlavor.imageFlavor};

    private JLabel label;

    /**
    * The constructor to use.
    * @param label The data that this ImageSelection should hold.
    */
    public ImageSelection(JLabel label) {
        this.label = label;
    }

    /**
    * Returns the data that this Transferable represents or 'null' if
    * the data is not of the given flavor.
    * @param flavor The flavor that the data should have.
    * @return The data that this Transferable represents or 'null' if
    * the data is not of the given flavor.
    */
    public Object getTransferData(DataFlavor flavor) {
        if (isDataFlavorSupported(flavor)) {
            return label;
        }
        return null;
    }

    /**
    * Returns the flavors that are supported by this Transferable.
    * @return The flavors that are supported by this Transferable.
    */
    public DataFlavor[] getTransferDataFlavors() {
        DataFlavor[] flavors = {DefaultTransferHandler.flavor};
        return flavors;
    }

    /**
    * Checks if the given data flavor is supported by this Transferable.
    * @param flavor The data flavor to check.
    * @return 'true' if the given data flavor is supported by this Transferable,
    * 'false' otherwise.
    */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DefaultTransferHandler.flavor);
    }
}
