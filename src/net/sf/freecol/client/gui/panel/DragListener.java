
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
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.ColonyTile;

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

        //Does not work on some platforms:
        //if (e.isPopupTrigger() && (comp instanceof UnitLabel)) {
        if (e.getButton() == MouseEvent.BUTTON3 && (comp instanceof UnitLabel)) {
            UnitLabel unitLabel = (UnitLabel)comp;
            Unit tempUnit = unitLabel.getUnit();

            if (tempUnit.isColonist()) {
                JPopupMenu menu = new JPopupMenu("Unit");
                JMenuItem menuItem;

                if (tempUnit.getLocation() instanceof ColonyTile) {
                    menuItem = new JMenuItem("Be a Farmer");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_FOOD));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Be a Sugar Planter");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_SUGAR));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Be a Tobacco Planter");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_TOBACCO));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Be a Cotton Planter");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_COTTON));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Be a Fur Trapper");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_FURS));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Be a Lumberjack");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_LUMBER));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Be an Ore Miner");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_ORE));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Be a Silver Miner");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_SILVER));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);

                    menu.addSeparator();
                }


                if (!tempUnit.isPioneer() && !tempUnit.isMissionary() && tempUnit.canArm()) {
                    if (tempUnit.isArmed()) {
                        menuItem = new JMenuItem("Disarm");
                    } else {
                        menuItem = new JMenuItem("Arm");
                    }
                    menuItem.setActionCommand(String.valueOf(UnitLabel.ARM));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                }

                if (!tempUnit.isPioneer() && !tempUnit.isMissionary() && tempUnit.canMount()) {
                    if (tempUnit.isMounted()) {
                        menuItem = new JMenuItem("Remove Horses");
                    } else {
                        menuItem = new JMenuItem("Mount");
                    }
                    menuItem.setActionCommand(String.valueOf(UnitLabel.MOUNT));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                }

                if (!tempUnit.isArmed() && !tempUnit.isMounted() && !tempUnit.isMissionary() && tempUnit.canEquipWithTools()) {
                    if (tempUnit.isPioneer()) {
                        menuItem = new JMenuItem("Remove Tools");
                    } else {
                        menuItem = new JMenuItem("Equip with Tools");
                    }
                    menuItem.setActionCommand(String.valueOf(UnitLabel.TOOLS));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                }

                if (!tempUnit.isArmed() && !tempUnit.isMounted() && !tempUnit.isPioneer() && tempUnit.canDressAsMissionary()) {

                    if (tempUnit.isMissionary()) {
                        menuItem = new JMenuItem("Take Off Silly Clothes");
                    } else {
                        menuItem = new JMenuItem("Dress as Missionaries");
                    }
                    menuItem.setActionCommand(String.valueOf(UnitLabel.DRESS));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                }

                menu.show(comp, e.getX(), e.getY());
            }
        } else {
            TransferHandler handler = comp.getTransferHandler();
            handler.exportAsDrag(comp, e, TransferHandler.COPY);

            if ((comp instanceof UnitLabel) && (((UnitLabel)comp).getUnit().isCarrier())) {
                if (parentPanel instanceof EuropePanel) {
                    ((EuropePanel) parentPanel).setSelectedUnit((UnitLabel)comp);
                } else if (parentPanel instanceof ColonyPanel) {
                    ((ColonyPanel) parentPanel).setSelectedUnit((UnitLabel)comp);
                }
            }
        }
    }
}
