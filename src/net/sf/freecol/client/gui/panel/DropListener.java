
package net.sf.freecol.client.gui.panel;

import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
* A DropListener should be attached to Swing components that have a
* TransferHandler attached. The DropListener will make sure that the
* Swing component to which it is attached can accept dragable data.
*/
public final class DropListener extends MouseAdapter {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static Logger logger = Logger.getLogger(DropListener.class.getName());

    /**
    * Gets called when the mouse was released on a Swing component that has this
    * object as a MouseListener.
    * @param e The event that holds the information about the mouse click.
    */
    public void mouseReleased(MouseEvent e) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable clipData = clipboard.getContents(clipboard);
        if (clipData != null) {
            if (clipData.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                JComponent comp = (JComponent)e.getSource();
                TransferHandler handler = comp.getTransferHandler();
                handler.importData(comp, clipData);
            }
        }
    }
}
