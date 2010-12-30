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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TradeRoute.Stop;

import net.miginfocom.swing.MigLayout;


/**
 * Allows the user to edit trade routes.
 */
public final class TradeRouteInputDialog extends FreeColDialog<Boolean> implements ActionListener {

    private static final Logger logger = Logger.getLogger(TradeRouteInputDialog.class.getName());

    private static final int OK = 0, CANCEL = 1;

    public static final DataFlavor STOP_FLAVOR = new DataFlavor(Stop.class, "Stop");

    private TradeRoute originalRoute;

    private final JButton ok = new JButton(Messages.message("ok"));

    private final JButton cancel = new JButton(Messages.message("cancel"));

    private final JButton addStopButton = new JButton(Messages.message("traderouteDialog.addStop"));

    private final JButton removeStopButton = new JButton(Messages.message("traderouteDialog.removeStop"));

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
    public TradeRouteInputDialog(final Canvas parent, TradeRoute newRoute) {
        super(parent);

        originalRoute = newRoute;

        ok.setActionCommand(String.valueOf(OK));
        ok.addActionListener(this);
        enterPressesWhenFocused(ok);

        cancel.setActionCommand(String.valueOf(CANCEL));
        cancel.addActionListener(this);
        enterPressesWhenFocused(cancel);
        setCancelComponent(cancel);

        goodsPanel = new GoodsPanel();
        goodsPanel.setTransferHandler(cargoHandler);
        cargoPanel = new CargoPanel();
        cargoPanel.setTransferHandler(cargoHandler);

        stopList.setCellRenderer(new StopRenderer());
        stopList.setFixedCellHeight(48);
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
                if (destinationSelector.getSelectedIndex() == 0 ) {
                    // All colonies + Europe
                    startIndex = 1;
                    endIndex = destinationSelector.getItemCount() - 1;
                } else {
                    // just 1 colony
                    startIndex = destinationSelector.getSelectedIndex();
                    endIndex = startIndex;
                }
                List<GoodsType> cargo = new ArrayList<GoodsType>();
                for (Component comp : cargoPanel.getComponents()) {
                    CargoLabel label = (CargoLabel) comp;
                    cargo.add(label.getType());
                }
                int maxIndex = stopList.getMaxSelectionIndex();
                for (int i = startIndex; i <= endIndex; i++) {
                    Stop stop = originalRoute.new Stop((Location) destinationSelector.getItemAt(i));
                    stop.setCargo(cargo);
                    if (maxIndex < 0) {
                        listModel.addElement(stop);
                    } else {
                        maxIndex++;
                        listModel.add(maxIndex, stop);
                    }
                }
            }
        });

        // button for deleting Stop
        removeStopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int count = 0;
                for (int index : stopList.getSelectedIndices()) {
                    listModel.remove(index - count);
                    count++;
                }
            }
        });

        stopList.setDragEnabled(true);
        stopList.setTransferHandler(new StopHandler());
        stopList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int[] indices = stopList.getSelectedIndices();
                    if (indices.length > 0) {
                        cargoPanel.initialize((Stop) listModel.get(indices[0]));
                    }
                }
            }
        });

        setLayout(new MigLayout("wrap 4", "[fill]", ""));

        add(getDefaultHeader(Messages.message("traderouteDialog.editRoute")),
            "span, align center");
        add(tradeRouteView, "span 1 5, grow");
        add(nameLabel);
        add(tradeRouteName, "span");
        add(destinationLabel);
        add(destinationSelector);
        add(addStopButton);
        add(removeStopButton, "skip 2");
        add(goodsPanel, "span");
        add(cargoPanel, "span, height 80:");
        add(ok, "newline 20, span, split 2, tag ok");
        add(cancel, "tag cancel");

        TradeRoute tradeRoute = newRoute.clone();

        Player player = getMyPlayer();

        // combo box for selecting destination
        destinationSelector.addItem(Messages.message("report.allColonies", "%number%", ""));
        if (player.getEurope() != null) {
            destinationSelector.addItem(player.getEurope());
        }
        List<Settlement> settlements = player.getSettlements();
        final Comparator<Colony> comparator = getClient().getClientOptions().getColonyComparator();
        Collections.sort(settlements, new Comparator<Settlement>() {
                public int compare(final Settlement s1, final Settlement s2) {
                    return comparator.compare((Colony) s1, (Colony) s2);
                }
            });
        for (Settlement settlement : settlements) {
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

        setSize(getPreferredSize());

    }

    /**
     * Enables the remove stop button if a stop is selected and disables it
     * otherwise.
     */
    public void updateButtons() {
        if (stopList.getSelectedIndices().length == 0) {
            removeStopButton.setEnabled(false);
        } else {
            removeStopButton.setEnabled(true);
        }
    }

    public void requestFocus() {
        ok.requestFocus();
    }

    private boolean verifyNewTradeRoute(){
    	// Verify that it has at least two stops
    	if (listModel.getSize() < 2) {
            getCanvas().errorMessage("traderouteDialog.notEnoughStops");
            return false;
    	}
    	
    	Player player = getCanvas().getClient().getMyPlayer();
        for (int index = 0; index < listModel.getSize(); index++) {
            Stop stop = (Stop) listModel.get(index);
            if (!TradeRoute.isStopValid(player, stop)) {
            	return false;
            }
        }

    	return true;
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
            	if (verifyNewTradeRoute()) {            	
                    getCanvas().remove(this);
                    originalRoute.setName(tradeRouteName.getText());
                    ArrayList<Stop> stops = new ArrayList<Stop>();
                    for (int index = 0; index < listModel.getSize(); index++) {
                        stops.add((Stop) listModel.get(index));
                    }
                    originalRoute.setStops(stops);
                    // TODO: update trade routes only if they have been modified
                    getController().updateTradeRoute(originalRoute);
                    setResponse(new Boolean(true));
                }
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
            super(getLibrary().getGoodsImageIcon(type));
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
            super(new GridLayout(0, 5, margin, margin));
            for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
                if (goodsType.isStorable()) {
                    CargoLabel label = new CargoLabel(goodsType);
                    add(label);
                }
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
                        int[] indices = stopList.getSelectedIndices();
                        for (int index : indices) {
                            Stop stop = (Stop) listModel.get(index);
                            stop.addCargo(label.getType());
                            stop.setModified(true);
                        }
                        stopList.revalidate();
                        stopList.repaint();
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
                    int[] indices = stopList.getSelectedIndices();
                    for (int stopIndex : indices) {
                        Stop stop = (Stop) listModel.get(stopIndex);
                        ArrayList<GoodsType> cargo = new ArrayList<GoodsType>(stop.getCargo());
                        for (int index = 0; index < cargo.size(); index++) {
                            if (cargo.get(index) == label.getType()) {
                                cargo.remove(index);
                                stop.setModified(true);
                                break;
                            }
                        }
                        stop.setCargo(cargo);
                    }
                    stopList.revalidate();
                    stopList.repaint();
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

    public class StopTransferable implements Transferable {

        private List<Stop> stops;

        public StopTransferable(List<Stop> stops) {
            this.stops = stops;
        }

        public Object getTransferData(DataFlavor flavor) {
            return stops;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { STOP_FLAVOR };
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor == STOP_FLAVOR;
        }
    }

    /*
     * TransferHandler for Stops.
     */
    public class StopHandler extends TransferHandler {

        protected Transferable createTransferable(JComponent c) {
            JList list = (JList) c;
            DefaultListModel model = (DefaultListModel) list.getModel();
            List<Stop> stops = new ArrayList<Stop>();
            for (int index : list.getSelectedIndices()) {
                stops.add((Stop) model.get(index));
            }
            return new StopTransferable(stops);
        }

        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        public boolean importData(JComponent target, Transferable data) {
            if (canImport(target, data.getTransferDataFlavors())) {
                try {
                    List stops = (List) data.getTransferData(STOP_FLAVOR);
                    if (target instanceof JList) {
                        JList list = (JList) target;
                        DefaultListModel model = (DefaultListModel) list.getModel();
                        int index = list.getMaxSelectionIndex();
                        for (Object o : stops) {
                            Stop stop = originalRoute.new Stop((Stop) o);
                            if (index < 0) {
                                model.addElement(stop);
                            } else {
                                index++;
                                model.add(index, stop);
                            }
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
                if (source instanceof JList && action == MOVE) {
                    JList stopList = (JList) source;
                    DefaultListModel listModel = (DefaultListModel) stopList.getModel();
                    for (Object o : (List) data.getTransferData(STOP_FLAVOR)) {
                        listModel.removeElement(o);
                    }
                }
            } catch (Exception e) {
                logger.warning(e.toString());
            }
        }

        public boolean canImport(JComponent c, DataFlavor[] flavors) {
            for (int i = 0; i < flavors.length; i++) {
                if (flavors[i].equals(STOP_FLAVOR)) {
                    return true;
                }
            }
            return false;
        }
    }

    private class StopRenderer implements ListCellRenderer {

        private final SelectedComponent SELECTED_COMPONENT = new SelectedComponent();
        private final NormalComponent NORMAL_COMPONENT = new NormalComponent();


        /**
         * Returns a <code>ListCellRenderer</code> for the given <code>JList</code>.
         *
         * @param list The <code>JList</code>.
         * @param value The list cell.
         * @param index The index in the list.
         * @param isSelected <code>true</code> if the given list cell is selected.
         * @param hasFocus <code>false</code> if the given list cell has the focus.
         * @return The <code>ListCellRenderer</code>
         */
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean hasFocus) {

            JPanel panel = (isSelected ? SELECTED_COMPONENT : NORMAL_COMPONENT);
            panel.removeAll();
            panel.setForeground(list.getForeground());
            panel.setFont(list.getFont());
            Stop stop = (Stop) value;
            Location location = stop.getLocation();
            JLabel icon, name;
            if (location instanceof Europe) {
                Europe europe = (Europe) location;
                Image image = getLibrary().getCoatOfArmsImage(europe.getOwner().getNation(), 0.5);
                icon = new JLabel(new ImageIcon(image));
                name = localizedLabel(europe.getNameKey());
            } else if (location instanceof Colony) {
                Colony colony = (Colony) location;
                icon = new JLabel(new ImageIcon(getLibrary().getSettlementImage(colony, 0.5)));
                name = new JLabel(colony.getName());
            } else {
                throw new IllegalStateException("Bogus location: " + location);
            }
            panel.add(icon, "spany");
            panel.add(name, "span, wrap");
            for (GoodsType cargo : stop.getCargo()) {
                panel.add(new JLabel(new ImageIcon(getLibrary().getGoodsImage(cargo, 0.5))));
            }
            return panel;
        }

        private class NormalComponent extends JPanel {

            public NormalComponent() {
                super(new MigLayout("", "[80][]"));
                setOpaque(false);
            }
        }

        private class SelectedComponent extends NormalComponent {

            public void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                Composite oldComposite = g2d.getComposite();
                Color oldColor = g2d.getColor();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setComposite(oldComposite);
                g2d.setColor(oldColor);

                super.paintComponent(g);
            }
        }


    }

}
