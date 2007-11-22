/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TradeRoute.Stop;
import cz.autel.dmi.HIGLayout;

/**
 * Allows the user to edit trade routes.
 */
public final class TradeRouteInputDialog extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(TradeRouteInputDialog.class.getName());




    private static final int OK = 0, CANCEL = 1;

    private TradeRoute originalRoute;

    private final JButton ok = new JButton(Messages.message("ok"));

    private final JButton cancel = new JButton(Messages.message("cancel"));

    private final JButton addStopButton = new JButton(Messages.message("traderouteDialog.addStop"));

    private final JButton removeStopButton = new JButton(Messages.message("traderouteDialog.removeStop"));

    private final JPanel tradeRoutePanel = new JPanel();

    private final JPanel buttonPanel = new JPanel();

    private final CargoHandler cargoHandler = new CargoHandler();

    private final MouseListener dragListener = new DragListener(this);

    private final MouseListener dropListener = new DropListener();

    private final GoodsPanel goodsPanel;

    private final CargoPanel cargoPanel;

    private final JComboBox destinationSelector = new JComboBox();

    private final JTextField tradeRouteName = new JTextField(Messages.message("traderouteDialog.newRoute"));

    private final DefaultListModel listModel = new DefaultListModel();

    private final JList stopList = new JList(listModel);

    private final JScrollPane tradeRouteView = new JScrollPane(stopList);

    private final JLabel nameLabel = new JLabel(Messages.message("traderouteDialog.nameLabel"));

    private final JLabel destinationLabel = new JLabel(Messages.message("traderouteDialog.destinationLabel"));


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public TradeRouteInputDialog(final Canvas parent) {
        super(parent);

        tradeRoutePanel.setOpaque(false);

        ok.setActionCommand(String.valueOf(OK));
        cancel.setActionCommand(String.valueOf(CANCEL));

        ok.addActionListener(this);
        cancel.addActionListener(this);

        ok.setMnemonic('y');
        cancel.setMnemonic('n');

        FreeColPanel.enterPressesWhenFocused(cancel);
        FreeColPanel.enterPressesWhenFocused(ok);

        setCancelComponent(cancel);

        goodsPanel = new GoodsPanel();
        goodsPanel.setTransferHandler(cargoHandler);
        cargoPanel = new CargoPanel();
        cargoPanel.setTransferHandler(cargoHandler);

        stopList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateButtons();
            }
        });

        // button for adding new Stop
        addStopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int startIndex = -1;
                int endIndex = -1;
                if (destinationSelector.getSelectedIndex()==0 ) {   // All colonies + Europe
                    startIndex=1;
                    endIndex=destinationSelector.getItemCount()-1;  // will use inverted order
                } else {
                    startIndex=destinationSelector.getSelectedIndex();  // just 1 colony
                    endIndex=startIndex;
                }
                for(int i=startIndex;i<=endIndex;i++)
                {
                    Stop stop = originalRoute.new Stop((Location) destinationSelector.getItemAt(i) );
                    for (Component comp : cargoPanel.getComponents()) {
                        CargoLabel label = (CargoLabel) comp;
                        stop.addCargo(label.getType());
                    }
                    if (stopList.getSelectedIndex() == -1) {
                        listModel.addElement(stop);
                    } else {
                        listModel.add(stopList.getSelectedIndex() + 1, stop);
                    }
                }
            }
        });

        // button for deleting Stop
        removeStopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                listModel.removeElement(stopList.getSelectedValue());
            }
        });

        // TODO: allow reordering of stops
        stopList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stopList.setDragEnabled(true);
        stopList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                cargoPanel.initialize((Stop) stopList.getSelectedValue());
            }
        });

        int[] widths = { 0 };
        int[] heights = { 0, margin, 0, margin, 0 };
        setLayout(new HIGLayout(widths, heights));

        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        buttonPanel.setOpaque(false);
        add(getDefaultHeader(Messages.message("traderouteDialog.editRoute")), higConst.rc(1, 1));
        add(tradeRoutePanel, higConst.rc(3, 1));
        add(buttonPanel, higConst.rc(5, 1));

    }

    public void initialize(TradeRoute newRoute) {
        originalRoute = newRoute;
        TradeRoute tradeRoute = newRoute.clone();

        Player player = getCanvas().getClient().getMyPlayer();

        // remove all items from lists and panels
        destinationSelector.removeAllItems();
        tradeRoutePanel.removeAll();
        cargoPanel.removeAll();
        listModel.removeAllElements();

        // combo box for selecting destination
        
        destinationSelector.addItem(Messages.message("report.allColonies", "%number%", ""));
        if (player.getEurope() != null) {
            destinationSelector.addItem(player.getEurope());
        }
        for (Settlement settlement : player.getSettlements()) {
            destinationSelector.addItem(settlement);
        }

        // add stops if any
        for (Stop stop : tradeRoute.getStops()) {
            listModel.addElement(stop);
        }

        // update cargo panel if stop is selected
        if (listModel.getSize() > 0) {
            stopList.setSelectedIndex(0);
            Stop selectedStop = (Stop) listModel.firstElement();
            cargoPanel.initialize(selectedStop);
        }

        // update buttons according to selection
        updateButtons();

        // set name of trade route
        tradeRouteName.setText(tradeRoute.getName());

        int widths[] = { 240, 3 * margin, 0, margin, 0 };
        int heights[] = { 0, 3 * margin, 0, margin, 0, margin, 80, margin, 0 };
        int listColumn = 1;
        int labelColumn = 3;
        int valueColumn = 5;

        tradeRoutePanel.setLayout(new HIGLayout(widths, heights));

        int row = 1;
        tradeRoutePanel.add(tradeRouteView, higConst.rcwh(row, listColumn, 1, heights.length));
        tradeRoutePanel.add(nameLabel, higConst.rc(row, labelColumn));
        tradeRoutePanel.add(tradeRouteName, higConst.rc(row, valueColumn));
        row += 2;

        tradeRoutePanel.add(destinationLabel, higConst.rc(row, labelColumn));
        tradeRoutePanel.add(destinationSelector, higConst.rc(row, valueColumn));
        row += 2;

        tradeRoutePanel.add(goodsPanel, higConst.rcwh(row, labelColumn, 3, 1));
        row += 2;

        tradeRoutePanel.add(cargoPanel, higConst.rcwh(row, labelColumn, 3, 1));
        row += 2;

        tradeRoutePanel.add(addStopButton, higConst.rc(row, labelColumn));
        tradeRoutePanel.add(removeStopButton, higConst.rc(row, valueColumn));
        row += 2;

        setSize(getPreferredSize());

    }

    /**
     * Enables the remove stop button if a stop is selected and disables it
     * otherwise.
     */
    public void updateButtons() {
        if (stopList.getSelectedIndex() == -1) {
            removeStopButton.setEnabled(false);
        } else {
            removeStopButton.setEnabled(true);
        }
    }

    public void requestFocus() {
        ok.requestFocus();
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
            case OK:
                getCanvas().remove(this);
                originalRoute.setName(tradeRouteName.getText());
                ArrayList<Stop> stops = new ArrayList<Stop>();
                for (int index = 0; index < listModel.getSize(); index++) {
                    stops.add((Stop) listModel.getElementAt(index));
                }
                originalRoute.setStops(stops);
                // TODO: update trade routes only if they have been modified
                getCanvas().getClient().getInGameController().updateTradeRoute(originalRoute);
                setResponse(new Boolean(true));
                break;
            case CANCEL:
                getCanvas().remove(this);
                setResponse(new Boolean(false));
                break;
            default:
                logger.warning("Invalid ActionCommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }


    /**
     * Special label for goods type.
     * 
     * TODO: clean this up for 0.7.0 -- The GoodsLabel needs to be modified so
     * that it can act as a GoodsTypeLabel (like this label), an
     * AbstractGoodsLabel and a CargoLabel (its current function).
     */
    public class CargoLabel extends JLabel {
        private final GoodsType goodsType;


        public CargoLabel(GoodsType type) {
            super(getCanvas().getGUI().getImageLibrary().getGoodsImageIcon(type));
            setTransferHandler(cargoHandler);
            addMouseListener(dragListener);
            this.goodsType = type;
        }

        public GoodsType getType() {
            return this.goodsType;
        }

    }

    /**
     * Panel for all types of goods that can be loaded onto a carrier.
     */
    public class GoodsPanel extends JPanel {

        public GoodsPanel() {
            super(new GridLayout(4, 4, margin, margin));
            for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
                CargoLabel label = new CargoLabel(goodsType);
                add(label);
            }
            setOpaque(false);
            setBorder(BorderFactory.createTitledBorder(Messages.message("goods")));
            addMouseListener(dropListener);
        }

    }

    /**
     * Panel for the cargo the carrier is supposed to take on board at a certain
     * stop.
     * 
     * TODO: create a single cargo panel for this purpose and the use in the
     * ColonyPanel, the EuropePanel and the CaptureGoodsDialog.
     */
    public class CargoPanel extends JPanel {

        // private Stop stop;

        public CargoPanel() {
            super();
            setOpaque(false);
            setBorder(BorderFactory.createTitledBorder(Messages.message("cargoOnCarrier")));
            addMouseListener(dropListener);
        }

        public void initialize(Stop newStop) {
            removeAll();
            if (newStop != null) {
                // stop = newStop;
                for (GoodsType goodsType : newStop.getCargo()) {
                    add(new CargoLabel(goodsType));
                }
            }
            revalidate();
            repaint();
        }
    }

    /**
     * TransferHandler for CargoLabels.
     * 
     * TODO: check whether this could/should be folded into the
     * DefaultTransferHandler.
     */
    public class CargoHandler extends TransferHandler {

        protected Transferable createTransferable(JComponent c) {
            return new ImageSelection((CargoLabel) c);
        }

        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        public boolean importData(JComponent target, Transferable data) {
            if (canImport(target, data.getTransferDataFlavors())) {
                try {
                    CargoLabel label = (CargoLabel) data.getTransferData(DefaultTransferHandler.flavor);
                    if (target instanceof CargoPanel) {
                        CargoLabel newLabel = new CargoLabel(label.getType());
                        cargoPanel.add(newLabel);
                        cargoPanel.revalidate();
                        Stop stop = (Stop) stopList.getSelectedValue();
                        if (stop != null) {
                            stop.addCargo(label.getType());
                            stop.setModified(true);
                        }
                    }
                    return true;
                } catch (UnsupportedFlavorException ufe) {
                    logger.warning(ufe.toString());
                } catch (IOException ioe) {
                    logger.warning(ioe.toString());
                }
            }

            return false;
        }

        protected void exportDone(JComponent source, Transferable data, int action) {
            try {
                CargoLabel label = (CargoLabel) data.getTransferData(DefaultTransferHandler.flavor);
                if (source.getParent() instanceof CargoPanel) {
                    cargoPanel.remove(label);
                    Stop stop = (Stop) stopList.getSelectedValue();
                    if (stop != null) {
                        ArrayList<GoodsType> cargo = stop.getCargo();
                        for (int index = 0; index < cargo.size(); index++) {
                            if (cargo.get(index) == label.getType()) {
                                cargo.remove(index);
                                stop.setModified(true);
                                break;
                            }
                        }
                        stop.setCargo(cargo);
                    }
                    cargoPanel.revalidate();
                    cargoPanel.repaint();
                }
            } catch (UnsupportedFlavorException ufe) {
                logger.warning(ufe.toString());
            } catch (IOException ioe) {
                logger.warning(ioe.toString());
            }
        }

        public boolean canImport(JComponent c, DataFlavor[] flavors) {
            for (int i = 0; i < flavors.length; i++) {
                if (flavors[i].equals(DefaultTransferHandler.flavor)) {
                    return true;
                }
            }
            return false;
        }
    }

    /*
     * public class StopPanel extends JPanel {
     * 
     * private Location location; private ArrayList<Integer> goods = new
     * ArrayList<Integer>(); private final JComboBox destinationBox; private
     * final JPanel goodsPanel; private final
     * 
     * public void saveSettings() { if (export.isSelected() !=
     * colony.getExports(goodsType)) {
     * getCanvas().getClient().getInGameController().setExports(colony,
     * goodsType, export.isSelected()); colony.setExports(goodsType,
     * export.isSelected()); } colony.getLowLevel()[goodsType] =
     * ((SpinnerNumberModel) lowLevel.getModel()).getNumber().intValue();
     * colony.getHighLevel()[goodsType] = ((SpinnerNumberModel)
     * highLevel.getModel()).getNumber().intValue();
     * colony.getExportLevel()[goodsType] = ((SpinnerNumberModel)
     * exportLevel.getModel()).getNumber().intValue();
     *  } }
     */
}
