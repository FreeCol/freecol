
package net.sf.freecol.client.gui.panel;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Goods;

/**
* The transferhandler that is capable of creating ImageSelection objects.
* Those ImageSelection objects are Transferable. The DefaultTransferHandler
* should be attached to JPanels or custom JLabels.
*/
public final class DefaultTransferHandler extends TransferHandler {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(DefaultTransferHandler.class.getName());

    private static final DataFlavor flavor = DataFlavor.imageFlavor;

    private final JLayeredPane parentPanel;

    /**
    * The constructor to use.
    * @param parentPanel The layered pane that holds all kinds of information.
    */
    public DefaultTransferHandler(JLayeredPane parentPanel) {
        this.parentPanel = parentPanel;
    }

    /**
    * Returns the action that can be done to an ImageSelection on the given component.
    * @return The action that can be done to an ImageSelection on the given component.
    */
    public int getSourceActions(JComponent c) {
        return TransferHandler.COPY;
    }


    /**
    * Returns 'true' if the given component can import a selection of the
    * flavor that is indicated by the second parameter, 'false' otherwise.
    * @param comp The component that needs to be checked.
    * @param flavor The flavor that needs to be checked for.
    * @return 'true' if the given component can import a selection of the
    * flavor that is indicated by the second parameter, 'false' otherwise.
    */
    public boolean canImport(JComponent comp, DataFlavor flavor[]) {
        if (!(comp instanceof UnitLabel) && !(comp instanceof GoodsLabel) && !(comp instanceof MarketLabel) && !(comp instanceof JPanel) && !(comp instanceof JLabel)) {
            return false;
        }
        for (int i = 0; i < flavor.length; i++) {
            if (flavor[i].equals(this.flavor)) {
                return true;
            }
        }
        return false;
    }

    /**
    * Creates a Transferable (an ImageSelection to be precise) of the
    * data that is represented by the given component and returns that
    * object.
    * @param comp The component to create a Transferable of.
    * @return The resulting Transferable (an ImageSelection object).
    */
    public Transferable createTransferable(JComponent comp) {
        if (comp instanceof UnitLabel) {
            return new ImageSelection((UnitLabel)comp);
        } else if (comp instanceof GoodsLabel) {
            return new ImageSelection((GoodsLabel)comp);
        } else if (comp instanceof MarketLabel) {
            return new ImageSelection((MarketLabel)comp);
        }
        return null;
    }

    /**
    * Imports the data represented by the given Transferable into
    * the given component. Returns 'true' on success, 'false' otherwise.
    * @param comp The component to import the data to.
    * @param t The Transferable that holds the data.
    * @return 'true' on success, 'false' otherwise.
    */
    public boolean importData(JComponent comp, Transferable t) {
        try {
            JLabel data;
            
            // Check flavor.
            if (t.isDataFlavorSupported(this.flavor)) {
                data = (JLabel)t.getTransferData(this.flavor);
            }
            else {
                logger.warning("Data flavor is not supported!");
                return false;
            }

            // Make sure we don't drop onto other Labels.
            if (comp instanceof UnitLabel) {
            
                /*
                  If the unit/cargo is dropped on a carrier in port (EuropePanel.InPortPanel),
                  then the ship is selected and the unit is added to its cargo.

                  If not, assume that the user wished to drop the unit/cargo on the panel below.
                */
                if (((UnitLabel) comp).getUnit().isCarrier() && ((UnitLabel) comp).getParent() instanceof EuropePanel.InPortPanel) {
                    ((EuropePanel) parentPanel).setSelectedUnit((UnitLabel) comp);
                    comp = ((EuropePanel) parentPanel).getCargoPanel();
                } else if (((UnitLabel) comp).getUnit().isCarrier() && ((UnitLabel) comp).getParent() instanceof ColonyPanel.InPortPanel) {
                    ((ColonyPanel) parentPanel).setSelectedUnit((UnitLabel) comp);
                    comp = ((ColonyPanel) parentPanel).getCargoPanel();
                } else {
                    try {
                        comp = (JComponent)comp.getParent();
                    } catch (ClassCastException e) {
                        return false;
                    }

                    // This is because we use an extra panel for layout in this particular case; may find a better solution later.
                    try {
                        if ((JComponent)comp.getParent() instanceof ColonyPanel.BuildingsPanel.ASingleBuildingPanel) {
                            comp = (JComponent)comp.getParent();
                        }
                    } catch (ClassCastException e) {}

                }
            } else if ((comp instanceof GoodsLabel) || (comp instanceof MarketLabel)) {


                try {
                    comp = (JComponent)comp.getParent();
                } catch (ClassCastException e) {
                    return false;
                }
            }

            if (data instanceof UnitLabel) {

                // Check if the unit can be dragged to comp.

                Unit unit = ((UnitLabel)data).getUnit();

                if ((unit.getState() == Unit.TO_AMERICA) && (!(comp instanceof EuropePanel.ToEuropePanel))) {
                    return false;
                }

                if ((unit.getState() == Unit.TO_EUROPE) && (!(comp instanceof EuropePanel.ToAmericaPanel))) {
                    return false;
                }

                /*if (((unit.getState() == Unit.ACTIVE) || ((unit.getState() == Unit.SENTRY) && (!unit.isNaval())))
                        && (!((comp instanceof EuropePanel.ToAmericaPanel) && (unit.isNaval())))
                        && (!((comp instanceof EuropePanel.DocksPanel) && (!unit.isNaval())
                        && (unit.getLocation() instanceof Unit) && (((Unit)unit.getLocation()).getState() == Unit.ACTIVE)))
                        && (!((comp instanceof EuropePanel.CargoPanel) && (!unit.isNaval())
                        && (unit.getLocation() instanceof Europe) && (((EuropePanel)parentPanel).getSelectedUnit() != null)
                        && (((EuropePanel)parentPanel).getSelectedUnit().getState() == Unit.ACTIVE)
                        && (((EuropePanel)parentPanel).getSelectedUnit().getSpaceLeft() > 0)))
                        && (!(comp instanceof ColonyPanel.InPortPanel))
                        && (!(comp instanceof ColonyPanel.BuildingsPanel.ASingleBuildingPanel))
                        && (!(comp instanceof ColonyPanel.OutsideColonyPanel))
                        && (!(comp instanceof ColonyPanel.InColonyPanel))
                        && (!(comp instanceof ColonyPanel.CargoPanel) && (!unit.isNaval()))
                        && (!(comp instanceof ColonyPanel.TilePanel))) {

                    return false;
                }*/

                if (comp instanceof JLabel) {
                    logger.warning("Oops, I thought we didn't have to write this part.");
                    return true;
                } else if (comp instanceof JPanel) {
                    // Do this in the 'add'-methods instead:
                    //data.getParent().remove(data);

                    if (comp instanceof EuropePanel.ToEuropePanel) {
                        ((EuropePanel.ToEuropePanel)comp).add(data, true);
                    } else if (comp instanceof EuropePanel.ToAmericaPanel) {
                        ((EuropePanel.ToAmericaPanel)comp).add(data, true);
                    } else if (comp instanceof EuropePanel.DocksPanel) {
                        ((EuropePanel.DocksPanel)comp).add(data, true);
                    } else if (comp instanceof EuropePanel.CargoPanel) {
                        ((EuropePanel.CargoPanel)comp).add(data, true);
                    } else if (comp instanceof ColonyPanel.BuildingsPanel.ASingleBuildingPanel) {
                        ((ColonyPanel.BuildingsPanel.ASingleBuildingPanel) comp).add(data, true);
                    } else if (comp instanceof ColonyPanel.OutsideColonyPanel) {
                        ((ColonyPanel.OutsideColonyPanel) comp).add(data, true);
                    } else if (comp instanceof ColonyPanel.CargoPanel) {
                        ((ColonyPanel.CargoPanel)comp).add(data, true);
                    } else if (comp instanceof ColonyPanel.TilePanel.ASingleTilePanel) {
                        ((ColonyPanel.TilePanel.ASingleTilePanel)comp).add(data, true);
                    } else {
                        logger.warning("The receiving component is of an invalid type.");
                        return false;
                    }

                    comp.revalidate();
                    return true;
                }
            } else if (data instanceof GoodsLabel) {

                // Check if the unit can be dragged to comp.

                //Goods g = ((GoodsLabel)data).getGoods();

                // Import the data.


                if (!(comp instanceof ColonyPanel.WarehousePanel || comp instanceof ColonyPanel.CargoPanel
                        || comp instanceof EuropePanel.MarketPanel || comp instanceof EuropePanel.CargoPanel)
                    || (comp instanceof EuropePanel.CargoPanel && !((EuropePanel.CargoPanel) comp).isActive())
                    || (comp instanceof ColonyPanel.CargoPanel && !((ColonyPanel.CargoPanel) comp).isActive())) {

                    return false;
                }


                if (comp instanceof JLabel) {
                    logger.warning("Oops, I thought we didn't have to write this part.");
                    return true;
                } else if (comp instanceof JPanel) {
                    //data.getParent().remove(data);

                    if (comp instanceof ColonyPanel.WarehousePanel) {
                        ((ColonyPanel.WarehousePanel)comp).add(data, true);
                    } else if (comp instanceof ColonyPanel.CargoPanel) {
                        ((ColonyPanel.CargoPanel)comp).add(data, true);
                    } else if (comp instanceof EuropePanel.MarketPanel) {
                        ((EuropePanel.MarketPanel)comp).add(data, true);
                    } else if (comp instanceof EuropePanel.CargoPanel) {
                        ((EuropePanel.CargoPanel)comp).add(data, true);
                    } else {
                        logger.warning("The receiving component is of an invalid type.");
                        return false;
                    }

                    comp.revalidate();
                    return true;
                }
            } else if (data instanceof MarketLabel) {

                // Check if the unit can be dragged to comp.

                //Goods g = ((GoodsLabel)data).getGoods();

                // Import the data.


                if (comp instanceof JLabel) {
                    logger.warning("Oops, I thought we didn't have to write this part.");
                    return true;
                } else if (comp instanceof JPanel) {
                    // Be not removing MarketLabels from their home. -sjm
                    //data.getParent().remove(data);

                    if (comp instanceof EuropePanel.CargoPanel) {
                        ((EuropePanel.CargoPanel)comp).add(data, true);
                    } else {
                        logger.warning("The receiving component is of an invalid type.");
                        return false;
                    }

                    comp.revalidate();
                    return true;
                }
            }

            logger.warning("The dragged component is of an invalid type.");

        } catch (UnsupportedFlavorException ignored) {
        } catch (IOException ignored) {}

        return false;
    }
}
