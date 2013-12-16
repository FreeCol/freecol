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
import java.util.logging.Level;
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
public final class TradeRouteInputPanel extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(TradeRouteInputPanel.class.getName());

    public static final DataFlavor STOP_FLAVOR
        = new DataFlavor(TradeRouteStop.class, "Stop");

    /**
     * Special label for goods type.
     *
     * TODO: clean this up for 0.7.0 -- The GoodsLabel needs to be
     * modified so that it can act as a GoodsTypeLabel (like this
     * label), an AbstractGoodsLabel and a CargoLabel (its current
     * function).
     */
    public class CargoLabel extends JLabel {

        private final GoodsType goodsType;


        public CargoLabel(GoodsType type) {
            super(getLibrary().getGoodsImageIcon(type));

            this.goodsType = type;
            setDisabledIcon(getDisabledIcon());
            setTransferHandler(TradeRouteInputPanel.this.cargoHandler);
            addMouseListener(TradeRouteInputPanel.this.dragListener);
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
            setBorder(BorderFactory
                .createTitledBorder(Messages.message("goods")));
            addMouseListener(TradeRouteInputPanel.this.dropListener);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setEnabled(boolean enable) {
            super.setEnabled(enable);
            for (Component child : getComponents()) {
                if (child instanceof CargoLabel) {
                    ((CargoLabel)child).setEnabled(enable);
                }
            }
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
            setBorder(BorderFactory
                .createTitledBorder(Messages.message("cargoOnCarrier")));
            addMouseListener(TradeRouteInputPanel.this.dropListener);
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
            if (!canImport(target, data.getTransferDataFlavors()))
                return false;
            try {
                CargoLabel label = (CargoLabel)data.getTransferData(DefaultTransferHandler.flavor);
                if (target instanceof CargoPanel) {
                    CargoLabel newLabel = new CargoLabel(label.getType());
                    TradeRouteInputPanel.this.cargoPanel.add(newLabel);
                    TradeRouteInputPanel.this.cargoPanel.revalidate();
                    int[] indices = stopList.getSelectedIndices();
                    for (int index : indices) {
                        TradeRouteStop stop
                            = (TradeRouteStop)stopListModel.get(index);
                        stop.addCargo(label.getType());
                    }
                    stopList.revalidate();
                    stopList.repaint();
                }
                return true;
            } catch (UnsupportedFlavorException ufe) {
                logger.log(Level.WARNING, "CargoHandler import", ufe);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "CargoHandler import", ioe);
            }
            return false;
        }

        protected void exportDone(JComponent source, Transferable data,
                                  int action) {
            try {
                CargoLabel label = (CargoLabel)data.getTransferData(DefaultTransferHandler.flavor);
                if (source.getParent() instanceof CargoPanel) {
                    TradeRouteInputPanel.this.cargoPanel.remove(label);
                    int[] indices = stopList.getSelectedIndices();
                    for (int stopIndex : indices) {
                        TradeRouteStop stop
                            = (TradeRouteStop)stopListModel.get(stopIndex);
                        List<GoodsType> cargo
                            = new ArrayList<GoodsType>(stop.getCargo());
                        for (int index = 0; index < cargo.size(); index++) {
                            if (cargo.get(index) == label.getType()) {
                                cargo.remove(index);
                                break;
                            }
                        }
                        stop.setCargo(cargo);
                    }
                    TradeRouteInputPanel.this.stopList.revalidate();
                    TradeRouteInputPanel.this.stopList.repaint();
                    TradeRouteInputPanel.this.cargoPanel.revalidate();
                    TradeRouteInputPanel.this.cargoPanel.repaint();
                }
            } catch (UnsupportedFlavorException ufe) {
                logger.log(Level.WARNING, "CargoHandler export", ufe);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "CargoHandler export", ioe);
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

    /**
     * TransferHandler for Stops.
     */
    public class StopHandler extends TransferHandler {

        protected Transferable createTransferable(JComponent c) {
            JList list = (JList) c;
            DefaultListModel model = (DefaultListModel)list.getModel();
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
            if (!canImport(target, data.getTransferDataFlavors()))
                return false;
            try {
                List stops = (List) data.getTransferData(STOP_FLAVOR);
                if (target instanceof JList) {
                    JList list = (JList) target;
                    DefaultListModel model = (DefaultListModel)list.getModel();
                    int index = list.getMaxSelectionIndex();
                    for (Object o : stops) {
                        TradeRouteStop stop
                            = new TradeRouteStop((TradeRouteStop)o);
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
                logger.log(Level.WARNING, "StopHandler import", ufe);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "StopHandler import", ioe);
            }
            return false;
        }

        protected void exportDone(JComponent source, Transferable data,
                                  int action) {
            try {
                if (source instanceof JList && action == MOVE) {
                    JList stopList = (JList)source;
                    DefaultListModel listModel
                        = (DefaultListModel)stopList.getModel();
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

        private final JPanel SELECTED_COMPONENT = new JPanel();
        private final JPanel NORMAL_COMPONENT = new JPanel();

        public StopRenderer() {
            NORMAL_COMPONENT.setLayout(new MigLayout("", "[80, center][]"));
            NORMAL_COMPONENT.setOpaque(false);
            SELECTED_COMPONENT.setLayout(new MigLayout("", "[80, center][]"));
            SELECTED_COMPONENT.setOpaque(false);
            SELECTED_COMPONENT.setUI((PanelUI)FreeColSelectedPanelUI
                .createUI(SELECTED_COMPONENT));
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
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean hasFocus) {
            JPanel panel = (isSelected) ? SELECTED_COMPONENT
                : NORMAL_COMPONENT;

            panel.removeAll();
            panel.setForeground(list.getForeground());
            panel.setFont(list.getFont());
            TradeRouteStop stop = (TradeRouteStop)value;
            Location location = stop.getLocation();
            JLabel icon, name;
            if (location instanceof Europe) {
                Europe europe = (Europe) location;
                Image image = getLibrary()
                    .getCoatOfArmsImage(europe.getOwner().getNation(), 0.5);
                icon = new JLabel(new ImageIcon(image));
                name = localizedLabel(europe.getNameKey());
            } else if (location instanceof Colony) {
                Colony colony = (Colony) location;
                icon = new JLabel(new ImageIcon(getLibrary()
                        .getSettlementImage(colony, 0.5)));
                name = new JLabel(colony.getName());
            } else {
                throw new IllegalStateException("Bogus location: " + location);
            }
            panel.add(icon, "spany");
            panel.add(name, "span, wrap");
            for (GoodsType cargo : stop.getCargo()) {
                panel.add(new JLabel(new ImageIcon(getLibrary()
                            .getGoodsImage(cargo, 0.5))));
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

    /**
     * The original route passed to this panel.  We are careful not to
     * modify it until we are sure all is well.
     */
    private final TradeRoute newRoute;

    /** A TransferHandler for the cargo labels. */
    private CargoHandler cargoHandler;

    /** Mouse listeners to use throughout. */
    private MouseListener dragListener, dropListener;

    /** Model to contain the current stops. */
    private DefaultListModel stopListModel;

    /** The list of stops to show. */
    private JList stopList;

    /** The user-editable name of the trade route. */
    private JTextField tradeRouteName;

    /** A box to select stops to add. */
    private JComboBox destinationSelector;

    /** Toggle message display. */
    private JCheckBox messagesBox;

    /** A button to add stops with. */
    private JButton addStopButton;

    /** A button to remove stops with. */
    private JButton removeStopButton;

    /** The panel displaying the goods that could be transported. */
    private GoodsPanel goodsPanel;

    /** The panel displaying the cargo at the selected stop. */
    private CargoPanel cargoPanel;


    /**
     * Create a panel to define trade route cargos.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param newRoute The <code>TradeRoute</code> to operate on.
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public TradeRouteInputPanel(FreeColClient freeColClient,
                                TradeRoute newRoute) {
        super(freeColClient, new MigLayout("wrap 4, fill", "[]20[fill]rel"));

        final Game game = freeColClient.getGame();
        final Player player = getMyPlayer();
        final TradeRoute tradeRoute = newRoute.copy(game, TradeRoute.class);

        this.newRoute = newRoute;
        this.cargoHandler = new CargoHandler();
        this.dragListener = new DragListener(getFreeColClient(), this);
        this.dropListener = new DropListener();

        this.stopListModel = new DefaultListModel();
        for (TradeRouteStop stop : tradeRoute.getStops()) {
            this.stopListModel.addElement(stop);
        }

        this.stopList = new JList(this.stopListModel);
        this.stopList.setCellRenderer(new StopRenderer());
        this.stopList.setFixedCellHeight(48);
        this.stopList.setDragEnabled(true);
        this.stopList.setTransferHandler(new StopHandler());
        this.stopList.addKeyListener(new KeyListener() {

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
        this.stopList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) return;
                    int[] idx = stopList.getSelectedIndices();
                    if (idx.length > 0) {
                        TradeRouteStop stop = (TradeRouteStop)
                            TradeRouteInputPanel.this.stopListModel.get(idx[0]);
                        TradeRouteInputPanel.this.cargoPanel.initialize(stop);
                        TradeRouteInputPanel.this.goodsPanel.setEnabled(true);
                    } else {
                        TradeRouteInputPanel.this.goodsPanel.setEnabled(false);
                    }
                    updateButtons();
                }
            });
        JScrollPane tradeRouteView = new JScrollPane(stopList);

        JLabel nameLabel = localizedLabel("tradeRouteInputPanel.nameLabel");
        this.tradeRouteName = new JTextField(tradeRoute.getName());

        JLabel destinationLabel
            = localizedLabel("tradeRouteInputPanel.destinationLabel");
        this.destinationSelector = new JComboBox();
        this.destinationSelector.setRenderer(new DestinationCellRenderer());
        StringTemplate template = StringTemplate.template("report.allColonies")
            .addName("%number%", "");
        this.destinationSelector.addItem(Messages.message(template));
        if (player.getEurope() != null) {
            this.destinationSelector.addItem(player.getEurope());
        }
        for (Colony colony : freeColClient.getMySortedColonies()) {
            this.destinationSelector.addItem((Settlement)colony);
        }

        this.messagesBox
            = new JCheckBox(Messages.message("tradeRouteInputPanel.silence"));
        this.messagesBox.setSelected(tradeRoute.isSilent());
        this.messagesBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    tradeRoute.setSilent(messagesBox.isSelected());
                }
            });

        this.addStopButton
            = new JButton(Messages.message("tradeRouteInputPanel.addStop"));
        this.addStopButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addSelectedStops();
                }
            });

        this.removeStopButton
            = new JButton(Messages.message("tradeRouteInputPanel.removeStop"));
        this.removeStopButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    deleteCurrentlySelectedStops();
                }
            });

        this.goodsPanel = new GoodsPanel();
        this.goodsPanel.setTransferHandler(this.cargoHandler);
        this.goodsPanel.setEnabled(false);
        this.cargoPanel = new CargoPanel();
        this.cargoPanel.setTransferHandler(this.cargoHandler);

        JButton cancelButton = new JButton(Messages.message("cancel"));
        cancelButton.setActionCommand(CANCEL);
        cancelButton.addActionListener(this);
        setCancelComponent(cancelButton);

        String hdr = Messages.message("tradeRouteInputPanel.editRoute");
        add(GUI.getDefaultHeader(hdr), "span, align center");
        add(tradeRouteView, "span 1 5, grow");
        add(nameLabel);
        add(this.tradeRouteName, "span");
        add(destinationLabel);
        add(this.destinationSelector, "span");
        add(this.messagesBox);
        add(this.addStopButton);
        add(this.removeStopButton, "span");
        add(this.goodsPanel, "span");
        add(this.cargoPanel, "span, height 80:, growy");
        add(okButton, "newline 20, span, split 2, tag ok");
        add(cancelButton, "tag cancel");

        // update cargo panel if stop is selected
        if (this.stopListModel.getSize() > 0) {
            this.stopList.setSelectedIndex(0);
            TradeRouteStop selectedStop
                = (TradeRouteStop)this.stopListModel.firstElement();
            this.cargoPanel.initialize(selectedStop);
        }

        // update buttons according to selection
        updateButtons();

        getGUI().restoreSavedSize(this, getPreferredSize());
    }

    /**
     * Add any stops selected in the destination selector.
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    private void addSelectedStops() {
        int startIndex = -1;
        int endIndex = -1;
        int sel = this.destinationSelector.getSelectedIndex();
        if (sel == 0) { // All colonies + Europe
            startIndex = 1;
            endIndex = destinationSelector.getItemCount() - 1;
        } else {
            // just 1 colony
            startIndex = sel;
            endIndex = startIndex;
        }
        List<GoodsType> cargo = new ArrayList<GoodsType>();
        for (Component comp : cargoPanel.getComponents()) {
            CargoLabel label = (CargoLabel)comp;
            cargo.add(label.getType());
        }
        int maxIndex = this.stopList.getMaxSelectionIndex();
        for (int i = startIndex; i <= endIndex; i++) {
            Location loc = (Location)this.destinationSelector.getItemAt(i);
            TradeRouteStop stop = new TradeRouteStop(getGame(), loc);
            stop.setCargo(cargo);
            if (maxIndex < 0) {
                this.stopListModel.addElement(stop);
            } else {
                maxIndex++;
                this.stopListModel.add(maxIndex, stop);
            }
        }
    }

    /**
     * Delete any stops currently selected in the stop list.
     */
    private void deleteCurrentlySelectedStops() {
        int count = 0;
        int lastIndex = 0;
        for (int index : this.stopList.getSelectedIndices()) {
            this.stopListModel.remove(index - count);
            count++;
            lastIndex = index;
        }

        // If the remaining list is non-empty, make sure that the
        // element beneath the last of the previously selected is
        // selected (ie, delete one of many, the one -under- the
        // deleted is selected) the user can then click in the list
        // once, and continue deleting without having to click in the
        // list again.
        if (this.stopListModel.getSize() > 0) {
            this.stopList.setSelectedIndex(lastIndex - count + 1);
        }
    }

    /**
     * Enables the remove stop button if a stop is selected and disables it
     * otherwise.
     */
    public void updateButtons() {
        this.addStopButton.setEnabled(this.stopListModel.getSize() 
            < this.destinationSelector.getItemCount() - 1);
        this.removeStopButton.setEnabled(this.stopList.getSelectedIndices()
            .length > 0);
    }

    /**
     * Check that the trade route is valid.
     *
     * @return True if the trade route is valid.
     */
    private boolean verifyNewTradeRoute() {
        final Player player = getFreeColClient().getMyPlayer();

        // Check that the name is unique
        String newName = this.tradeRouteName.getText();
        for (TradeRoute route : player.getTradeRoutes()) {
            if (route.getName() == null) continue;
            if (route.getName().equals(this.newRoute.getName())) continue;
            if (route.getName().equals(newName)) {
                StringTemplate template
                    = StringTemplate.template("tradeRoute.duplicateName")
                        .addName("%name%", newName);
                getGUI().showErrorMessage(template);
                return false;
            }
        }

        // Verify that it has at least two stops
        if (this.stopListModel.getSize() < 2) {
            getGUI().showErrorMessage("tradeRoute.notEnoughStops");
            return false;
        }

        // Check that all stops are valid
        for (int index = 0; index < stopListModel.getSize(); index++) {
            TradeRouteStop stop = (TradeRouteStop)stopListModel.get(index);
            if (!TradeRoute.isStopValid(player, stop)) {
                String badStop = Messages.message(stop.getLocation()
                    .getLocationNameFor(player));
                StringTemplate template
                    = StringTemplate.template("tradeRoute.invalidStop")
                        .addName("%name%", badStop);
                getGUI().showErrorMessage(template);
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
            this.newRoute.setName(tradeRouteName.getText());
            this.newRoute.clearStops();
            for (int index = 0; index < this.stopListModel.getSize(); index++) {
                this.newRoute.addStop((TradeRouteStop)this.stopListModel
                    .get(index));
            }
            this.newRoute.setSilent(this.messagesBox.isSelected());
            // Return to TradeRoutePanel, which will add the route
            // if needed, and it is valid.
            super.actionPerformed(event);

        } else if (CANCEL.equals(command)) {
            // Make sure the original route is invalid.
            this.newRoute.setName(null);
            getGUI().removeFromCanvas(this);

        } else {
            super.actionPerformed(event);
        }
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        this.cargoHandler = null;
        this.dragListener = null;
        this.dropListener = null;
        this.stopListModel.clear();
        this.stopListModel = null;
        this.stopList = null;
        this.tradeRouteName = null;
        this.destinationSelector = null;
        this.messagesBox = null;
        this.addStopButton = null;
        this.removeStopButton = null;
        this.cargoPanel = null;

        super.removeNotify();
    }
}
