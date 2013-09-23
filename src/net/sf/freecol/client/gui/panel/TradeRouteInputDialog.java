/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.PanelUI;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColSelectedPanelUI;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TradeRouteStop;


/**
 * Allows the user to edit trade routes.
 */
public final class TradeRouteInputDialog extends FreeColDialog<Boolean> implements ActionListener {

    private static final Logger logger = Logger.getLogger(TradeRouteInputDialog.class.getName());

    public static final DataFlavor STOP_FLAVOR
        = new DataFlavor(TradeRouteStop.class, "Stop");

    private TradeRoute originalRoute;

    private final JButton addStopButton
        = new JButton(Messages.message("traderouteDialog.addStop"));

    private final JButton removeStopButton
        = new JButton(Messages.message("traderouteDialog.removeStop"));

    private final CargoHandler cargoHandler = new CargoHandler();

    private final MouseListener dragListener
        = new DragListener(getFreeColClient(), this);

    private final MouseListener dropListener = new DropListener();

    private final GoodsPanel goodsPanel;

    private final CargoPanel cargoPanel;

    private final JComboBox destinationSelector = new JComboBox();

    private final JTextField tradeRouteName
        = new JTextField(Messages.message("traderouteDialog.newRoute"));

    private final DefaultListModel stopListModel = new DefaultListModel();

    @SuppressWarnings("unchecked") // FIXME in Java7
    private final JList stopList = new JList(stopListModel);

    private final JCheckBox messagesBox
        = new JCheckBox(Messages.message("traderouteDialog.silence"));

    private final JScrollPane tradeRouteView = new JScrollPane(stopList);

    private final JLabel nameLabel
        = new JLabel(Messages.message("traderouteDialog.nameLabel"));

    private final JLabel destinationLabel
        = new JLabel(Messages.message("traderouteDialog.destinationLabel"));


    /**
     * Create a dialog to define trade route cargos.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param newRoute The <code>TradeRoute</code> to operate on.
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public TradeRouteInputDialog(FreeColClient freeColClient,
                                 TradeRoute newRoute) {
        super(freeColClient, new MigLayout("wrap 4, fill", "[]20[fill]rel"));

        this.originalRoute = newRoute;

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
        stopList.addKeyListener(new KeyListener() {

                @Override
                public void keyTyped(KeyEvent e) {
                    if (e.getKeyChar() == KeyEvent.VK_DELETE) {
                        deleteCurrentlySelectedStops();
                    }
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    throw new UnsupportedOperationException("Not supported yet.");
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
                    Game game = originalRoute.getGame();
                    for (int i = startIndex; i <= endIndex; i++) {
                        TradeRouteStop stop = new TradeRouteStop(game,
                            (Location) destinationSelector.getItemAt(i));
                        stop.setCargo(cargo);
                        if (maxIndex < 0) {
                            stopListModel.addElement(stop);
                        } else {
                            maxIndex++;
                            stopListModel.add(maxIndex, stop);
                        }
                    }
                }
            });

        // button for deleting Stop
        removeStopButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    deleteCurrentlySelectedStops();
                }
            });

        stopList.setDragEnabled(true);
        stopList.setTransferHandler(new StopHandler());
        stopList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        int[] indices = stopList.getSelectedIndices();
                        if (indices.length > 0) {
                            cargoPanel.initialize((TradeRouteStop) stopListModel.get(indices[0]));
                        }
                    }
                }
            });

        add(GUI.getDefaultHeader(Messages.message("traderouteDialog.editRoute")),
            "span, align center");
        add(tradeRouteView, "span 1 5, grow");
        add(nameLabel);
        add(tradeRouteName, "span");
        add(destinationLabel);
        add(destinationSelector, "span");
        add(messagesBox);
        add(addStopButton);
        add(removeStopButton, "span");
        add(goodsPanel, "span");
        add(cargoPanel, "span, height 80:, growy");
        add(okButton, "newline 20, span, split 2, tag ok");
        add(cancelButton, "tag cancel");

        final Game game = freeColClient.getGame();
        final Player player = getMyPlayer();
        final TradeRoute tradeRoute = newRoute.copy(game, TradeRoute.class);

        // combo box for selecting destination
        destinationSelector.setRenderer(new DestinationCellRenderer());
        destinationSelector.addItem(Messages.message(StringTemplate.template("report.allColonies")
                                                     .addName("%number%", "")));
        if (player.getEurope() != null) {
            destinationSelector.addItem(player.getEurope());
        }
        for (Colony colony : freeColClient.getMySortedColonies()) {
            destinationSelector.addItem((Settlement) colony);
        }

        messagesBox.setSelected(tradeRoute.isSilent());
        messagesBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    tradeRoute.setSilent(messagesBox.isSelected());
                }
            });

        // add stops if any
        for (TradeRouteStop stop : tradeRoute.getStops()) {
            stopListModel.addElement(stop);
        }

        // update cargo panel if stop is selected
        if (stopListModel.getSize() > 0) {
            stopList.setSelectedIndex(0);
            TradeRouteStop selectedStop
                = (TradeRouteStop)stopListModel.firstElement();
            cargoPanel.initialize(selectedStop);
        }

        // update buttons according to selection
        updateButtons();

        // set name of trade route
        tradeRouteName.setText(tradeRoute.getName());

        restoreSavedSize(getPreferredSize());
    }

    private void deleteCurrentlySelectedStops() {
        int count = 0;
        int lastIndex = 0;
        for (int index : stopList.getSelectedIndices()) {
            stopListModel.remove(index - count);
            count++;
            lastIndex = index;
        }

        // If the remaining list is non-empty, make sure that the
        // element beneath the last of the previously selected is
        // selected (ie, delete one of many, the one -under- the
        // deleted is selected) the user can then click in the list
        // once, and continue deleting without having to click in the
        // list again.
        if (stopListModel.getSize() > 0) {
            stopList.setSelectedIndex(lastIndex - count + 1);
        }
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

    /**
     * Check that the trade route is valid.
     *
     * @return True if the trade route is valid.
     */
    private boolean verifyNewTradeRoute() {
        Player player = getFreeColClient().getMyPlayer();

        // Check that the name is unique
        String newName = tradeRouteName.getText();
        for (TradeRoute route : player.getTradeRoutes()) {
            if (route.getName().equals(originalRoute.getName())) continue;
            if (route.getName().equals(newName)) {
                StringTemplate template
                    = StringTemplate.template("traderouteDialog.duplicateName")
                        .addName("%name%", newName);
                getGUI().errorMessage(template);
                return false;
            }
        }

        // Verify that it has at least two stops
        if (stopListModel.getSize() < 2) {
            getGUI().errorMessage("traderouteDialog.notEnoughStops");
            return false;
        }

        // Check that all stops are valid
        for (int index = 0; index < stopListModel.getSize(); index++) {
            TradeRouteStop stop = (TradeRouteStop)stopListModel.get(index);
            if (!TradeRoute.isStopValid(player, stop)) {
                String badStop = Messages.message(stop.getLocation().getLocationName());
                StringTemplate template
                    = StringTemplate.template("traderouteDialog.invalidStop")
                        .addName("%name%", badStop);
                getGUI().errorMessage(template);
                return false;
            }
        }

        return true;
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent event) {
        final String command = event.getActionCommand();
        if (OK.equals(command)) {
            if (!verifyNewTradeRoute()) return;
            originalRoute.setName(tradeRouteName.getText());
            originalRoute.clearStops();
            for (int index = 0; index < stopListModel.getSize(); index++) {
                originalRoute.addStop((TradeRouteStop)stopListModel.get(index));
            }
            // TODO: update trade routes only if they have been modified
            originalRoute.setSilent(messagesBox.isSelected());
            getController().updateTradeRoute(originalRoute);
            getGUI().removeFromCanvas(this);
            setResponse(Boolean.TRUE);
        } else if (CANCEL.equals(command)) {
            getGUI().removeFromCanvas(this);
            setResponse(Boolean.FALSE);
        } else {
            super.actionPerformed(event);
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
            super(new GridLayout(0, 4, MARGIN, MARGIN));
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
     * Panel for the cargo the carrier is supposed to take on board at
     * a certain stop.
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

        public void initialize(TradeRouteStop newStop) {
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
                            TradeRouteStop stop = (TradeRouteStop)stopListModel.get(index);
                            stop.addCargo(label.getType());
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
                        TradeRouteStop stop = (TradeRouteStop)stopListModel.get(stopIndex);
                        ArrayList<GoodsType> cargo = new ArrayList<GoodsType>(stop.getCargo());
                        for (int index = 0; index < cargo.size(); index++) {
                            if (cargo.get(index) == label.getType()) {
                                cargo.remove(index);
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

        private List<TradeRouteStop> stops;

        public StopTransferable(List<TradeRouteStop> stops) {
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
            List<TradeRouteStop> stops = new ArrayList<TradeRouteStop>();
            for (int index : list.getSelectedIndices()) {
                stops.add((TradeRouteStop)model.get(index));
            }
            return new StopTransferable(stops);
        }

        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @SuppressWarnings("unchecked") // FIXME in Java7
        public boolean importData(JComponent target, Transferable data) {
            if (canImport(target, data.getTransferDataFlavors())) {
                try {
                    List stops = (List) data.getTransferData(STOP_FLAVOR);
                    if (target instanceof JList) {
                        JList list = (JList) target;
                        DefaultListModel model = (DefaultListModel) list.getModel();
                        int index = list.getMaxSelectionIndex();
                        for (Object o : stops) {
                            TradeRouteStop stop = new TradeRouteStop((TradeRouteStop)o);
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

        private final JPanel SELECTED_COMPONENT = new MigPanel();
        private final JPanel NORMAL_COMPONENT = new MigPanel();

        public StopRenderer() {
            NORMAL_COMPONENT.setLayout(new MigLayout("", "[80, center][]"));
            NORMAL_COMPONENT.setOpaque(false);
            SELECTED_COMPONENT.setLayout(new MigLayout("", "[80, center][]"));
            SELECTED_COMPONENT.setOpaque(false);
            SELECTED_COMPONENT.setUI((PanelUI) FreeColSelectedPanelUI.createUI(SELECTED_COMPONENT));
        }

        /**
         * Returns a <code>ListCellRenderer</code> for the given
         * <code>JList</code>.
         *
         * @param list The <code>JList</code>.
         * @param value The list cell.
         * @param index The index in the list.
         * @param isSelected <code>true</code> if the given list cell
         *     is selected.
         * @param hasFocus <code>false</code> if the given list cell
         *     has the focus.
         * @return The <code>ListCellRenderer</code>
         */
        public Component getListCellRendererComponent(JList list,
            Object value, int index, boolean isSelected, boolean hasFocus) {

            JPanel panel = (isSelected) ? SELECTED_COMPONENT : NORMAL_COMPONENT;
            panel.removeAll();
            panel.setForeground(list.getForeground());
            panel.setFont(list.getFont());
            TradeRouteStop stop = (TradeRouteStop)value;
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
    }

    private class DestinationCellRenderer extends JLabel
        implements ListCellRenderer {

        public DestinationCellRenderer() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            setText((value instanceof Location)
                ? Messages.message(((Location)value).getLocationName())
                : value.toString());
            setForeground((isSelected) ? list.getSelectionForeground()
                : list.getForeground());
            setBackground((isSelected) ? list.getSelectionBackground()
                : list.getBackground());
            return this;
        }
    }
}
