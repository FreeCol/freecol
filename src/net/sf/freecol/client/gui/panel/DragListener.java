
package net.sf.freecol.client.gui.panel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;

import net.sf.freecol.common.model.Unit;

/**
* A DragListener should be attached to Swing components that have a
* TransferHandler attached. The DragListener will make sure that the
* Swing component to which it is attached is draggable (moveable to be
* precise).
*/
public final class DragListener extends MouseAdapter {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static Logger logger = Logger.getLogger(DragListener.class.getName());
    
    private final JLayeredPane parentPanel;

    /**
    * The constructor to use.
    * @param parentPanel The layered pane that contains the components to which a
    * DragListener might be attached.
    */
    public DragListener(JLayeredPane parentPanel) {
        this.parentPanel = parentPanel;
    }
    
    /**
    * Gets called when the mouse was pressed on a Swing component that has this
    * object as a MouseListener.
    * @param e The event that holds the information about the mouse click.
    */
    public void mousePressed(MouseEvent e) {
        JComponent comp = (JComponent)e.getSource();
        
        if (e.isPopupTrigger() && (comp instanceof UnitLabel)) {
            UnitLabel unitLabel = (UnitLabel)comp;
            Unit tempUnit = unitLabel.getUnit();

            if (!tempUnit.isCarrier()) {
                JPopupMenu menu = new JPopupMenu("Unit");
                JMenuItem menuItem;

                if (tempUnit.isArmed()) {
                    menuItem = new JMenuItem("Disarm");
                } else {
                    menuItem = new JMenuItem("Arm");
                }
                menuItem.setActionCommand(String.valueOf(UnitLabel.ARM));
                menuItem.addActionListener(unitLabel);
                menu.add(menuItem);

                if (tempUnit.isMounted()) {
                    menuItem = new JMenuItem("Sell Horses");
                } else {
                    menuItem = new JMenuItem("Mount");
                }
                menuItem.setActionCommand(String.valueOf(UnitLabel.MOUNT));
                menuItem.addActionListener(unitLabel);
                menu.add(menuItem);

                if (tempUnit.getNumberOfTools() > 0) {
                    menuItem = new JMenuItem("Sell Tools");
                } else {
                    menuItem = new JMenuItem("Equip with Tools");
                }
                menuItem.setActionCommand(String.valueOf(UnitLabel.TOOLS));
                menuItem.addActionListener(unitLabel);
                menu.add(menuItem);

                if (tempUnit.isMissionary()) {
                    menuItem = new JMenuItem("Take Off Silly Clothes");
                } else {
                    menuItem = new JMenuItem("Dress as Missionaries");
                }
                menuItem.setActionCommand(String.valueOf(UnitLabel.DRESS));
                menuItem.addActionListener(unitLabel);
                menu.add(menuItem);

                menu.show(comp, e.getX(), e.getY());
            }
        } else {
            TransferHandler handler = comp.getTransferHandler();
            handler.exportAsDrag(comp, e, TransferHandler.COPY);

            if ((comp instanceof UnitLabel) && (((UnitLabel)comp).getUnit().isNaval())) {
                if (parentPanel instanceof EuropePanel) {
                    ((EuropePanel) parentPanel).setSelectedUnit((UnitLabel)comp);
                } else if (parentPanel instanceof ColonyPanel) {
                    ((ColonyPanel) parentPanel).setSelectedUnit((UnitLabel)comp);
                }
            }
        }
    }
}
