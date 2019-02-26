/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Function;

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
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.label.FreeColLabel;
import net.sf.freecol.client.gui.plaf.FreeColSelectedPanelUI;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TradeLocation;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TradeRouteStop;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Allows the user to edit trade routes.
 */
public final class TradeRouteInputPanel extends FreeColPanel 
    implements ListSelectionListener {

    private static final Logger logger = Logger.getLogger(TradeRouteInputPanel.class.getName());

    public static final DataFlavor STOP_FLAVOR
        = new DataFlavor(TradeRouteStop.class, "Stop");

    /**
     * Special label for cargo-to-carry type.
     *
     * Public for DragListener.
     */
    public static class TradeRouteCargoLabel extends FreeColLabel {

        /** The type of goods to load at a stop. */
        private final GoodsType goodsType;


        /**
         * Build a new cargo label for a given goods type.
         *
         * @param icon The label icon.
         * @param type The {@code GoodsType}.
         */         
        public TradeRouteCargoLabel(ImageIcon icon, GoodsType type) {
            super(icon);

            this.goodsType = type;
        }

        /**
         * Get the goods type of this label.
         *
         * @return The {@code GoodsType}.
         */
        public GoodsType getType() {
            return this.goodsType;
        }
    }


    /**
     * Panel for the cargo the carrier is supposed to take on board at
     * a certain stop.
     *
     * FIXME: create a single cargo panel for this purpose and the use
     * in the ColonyPanel, the EuropePanel and the CaptureGoodsDialog?
     */
    private static class TradeRouteCargoPanel extends MigPanel {

        /**
         * Build a new cargo panel.
         */
        public TradeRouteCargoPanel() {
            super(new MigLayout());

            setOpaque(false);
            setBorder(Utility.localizedBorder("cargoOnCarrier"));
        }

        /**
         * Set the labels to display on this panel.
         *
         * @param labels The {@code TradeRouteCargoLabel}s to display.
         */
        public void setLabels(List<TradeRouteCargoLabel> labels) {
            removeAll();
            if (labels != null) {
                for (TradeRouteCargoLabel label : labels) add(label);
            }
            revalidate();
            repaint();
        }

        /**
         * Add a single label.
         *
         * Do not repaint, that will be done top down.
         *
         * @param label The {@code TradeRouteCargoLabel} to add.
         */
        public void addLabel(TradeRouteCargoLabel label) {
            if (label != null) {
                add(label);
                revalidate();
            }
        }

        /**
         * Remove labels that correspond to a given goods type.
         *
         * @param gt The {@code GoodsType} to remove.
         */
        public void removeType(GoodsType gt) {
            for (Component child : getComponents()) {
                if (child instanceof TradeRouteCargoLabel) {
                    if (((TradeRouteCargoLabel)child).getType() == gt) {
                        remove(child);
                    }
                }
                revalidate();
            }
        }
    }

    /**
     * TransferHandler for TradeRouteCargoLabels.
     *
     * FIXME: check whether this could/should be folded into the
     * DefaultTransferHandler.
     */
    private static class CargoHandler extends TransferHandler {

        /** The enclosing panel. */
        private TradeRouteInputPanel tradeRouteInputPanel;


        /**
         * Build a new cargo handler for the given panel.
         *
         * @param trip The {@code TradeRouteInputPanel} to notify of changes.
         */
        public CargoHandler(TradeRouteInputPanel trip) {
            this.tradeRouteInputPanel = trip;
        }


        /**
         * Convenience function to try to get a {@code TradeRouteCargoLabel}
         * from a {@code Transferable}.
         *
         * @param data The {@code Transferable} to query.
         * @return The label found if any.
         * @exception IOException if the data is not available.
         * @exception UnsupportedFlavorException on flavor mismatch.
         */
        private TradeRouteCargoLabel tryTransfer(Transferable data)
            throws IOException, UnsupportedFlavorException {
            return (TradeRouteCargoLabel)data.getTransferData(DefaultTransferHandler.flavor);
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        protected Transferable createTransferable(JComponent c) {
            return new ImageSelection((TradeRouteCargoLabel) c);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean importData(JComponent target, Transferable data) {
            if (!canImport(target, data.getTransferDataFlavors()))
                return false;
            TradeRouteCargoLabel label;
            try {
                label = tryTransfer(data);
            } catch (IOException|UnsupportedFlavorException ex) {
                logger.log(Level.WARNING, "CargoHandler import", ex);
                return false;
            }
            if (target instanceof TradeRouteCargoPanel) {
                this.tradeRouteInputPanel.enableImport(label.getType());
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void exportDone(JComponent source, Transferable data,
                                  int action) {
            TradeRouteCargoLabel label;
            try {
                label = tryTransfer(data);
            } catch (IOException|UnsupportedFlavorException ex) {
                logger.log(Level.WARNING, "CargoHandler export", ex);
                return;
            }
            if (source.getParent() instanceof TradeRouteCargoPanel) {
                this.tradeRouteInputPanel.cancelImport(label.getType());
            }
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean canImport(JComponent c, DataFlavor[] flavors) {
            return any(flavors, matchKeyEquals(DefaultTransferHandler.flavor));
        }
    }

    private class DestinationCellRenderer extends JLabel
        implements ListCellRenderer<String> {

        public DestinationCellRenderer() {
            setOpaque(true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list,
                                                      String value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            FreeColGameObject fcgo = getGame().getFreeColGameObject(value);
            if (fcgo instanceof Location) {
                setText(Messages.message(((Location)fcgo).getLocationLabel()));
            } else {
                setText(value);
            }
            setForeground((isSelected) ? list.getSelectionForeground()
                : list.getForeground());
            setBackground((isSelected) ? list.getSelectionBackground()
                : list.getBackground());
            return this;
        }
    }

    /**
     * Panel for all types of goods that can be loaded onto a carrier.
     */
    private static class TradeRouteGoodsPanel extends MigPanel {

        public TradeRouteGoodsPanel(List<TradeRouteCargoLabel> labels) {
            super(new GridLayout(0, 4, MARGIN, MARGIN));

            for (TradeRouteCargoLabel label : labels) add(label);
            setOpaque(false);
            setBorder(Utility.localizedBorder("goods"));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setEnabled(boolean enable) {
            super.setEnabled(enable);
            for (Component child : getComponents()) {
                if (child instanceof TradeRouteCargoLabel) child.setEnabled(enable);
            }
        }
    }

    private static class StopListTransferable implements Transferable {

        private final List<TradeRouteStop> stops;


        public StopListTransferable(List<TradeRouteStop> stops) {
            this.stops = stops;
        }

        public List<TradeRouteStop> getStops() {
            return stops;
        }

        // Interface Transferable

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getTransferData(DataFlavor flavor) {
            return (flavor == STOP_FLAVOR) ? stops : null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { STOP_FLAVOR };
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor == STOP_FLAVOR;
        }
    }

    /**
     * TransferHandler for Stops.
     */
    private class StopListHandler extends TransferHandler {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean canImport(JComponent c, DataFlavor[] flavors) {
            return any(flavors, matchKeyEquals(STOP_FLAVOR));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Transferable createTransferable(JComponent c) {
            JList list = (JList)c;
            final DefaultListModel model = (DefaultListModel)list.getModel();
            int[] indicies = list.getSelectedIndices();
            List<TradeRouteStop> stops = new ArrayList<>(indicies.length);
            for (int i : indicies) stops.add(i, (TradeRouteStop)model.get(i));
            return new StopListTransferable(stops);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean importData(JComponent target, Transferable data) {
            JList<TradeRouteStop> stl = TradeRouteInputPanel.this.stopList;
            if (target == stl
                && data instanceof StopListTransferable
                && canImport(target, data.getTransferDataFlavors())) {
                List<TradeRouteStop> stops
                    = ((StopListTransferable)data).getStops();
                DefaultListModel<TradeRouteStop> model
                    = new DefaultListModel<>();
                int index = stl.getMaxSelectionIndex();
                for (TradeRouteStop stop : stops) {
                    if (index < 0) {
                        model.addElement(stop);
                    } else {
                        index++;
                        model.add(index, stop);
                    }
                }
                stl.setModel(model);
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
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
    }

    private class StopRenderer implements ListCellRenderer<TradeRouteStop> {

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
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(JList<? extends TradeRouteStop> list,
                                                      TradeRouteStop value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean hasFocus) {
            JPanel panel = (isSelected) ? SELECTED_COMPONENT
                : NORMAL_COMPONENT;
            panel.removeAll();
            panel.setForeground(list.getForeground());
            panel.setFont(list.getFont());
            Location location = value.getLocation();
            ImageLibrary lib = getImageLibrary();
            JLabel icon, name;

            if (location instanceof TradeLocation) {
                TradeLocation tl = (TradeLocation) location;
                if (tl.canBeInput() == true) {
                    name = tl.getNameAsJlabel();
                } else {
                    throw new IllegalStateException("Bogus location: " + location);
                }
            } else {
                throw new IllegalStateException("Bogus location: " + location);
            }

            if (location instanceof Europe) {
                Europe europe = (Europe) location;
                Image image = lib.getSmallerNationImage(europe.getOwner().getNation());
                icon = new JLabel(new ImageIcon(image));
            } else if (location instanceof Colony) {
                Colony colony = (Colony) location;
                icon = new JLabel(new ImageIcon(lib.getSmallerSettlementImage(colony)));
            } else {
                throw new IllegalStateException("Bogus location: " + location);
            }
            panel.add(icon, "spany");
            panel.add(name, "span, wrap");
            for (GoodsType cargo : value.getCargo()) {
                panel.add(new JLabel(new ImageIcon(
                    lib.getSmallerGoodsTypeImage(cargo))));
            }
            return panel;
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
    private transient MouseListener dragListener, dropListener;

    /** Model to contain the current stops. */
    private DefaultListModel<TradeRouteStop> stopListModel;

    /** The list of stops to show. */
    private JList<TradeRouteStop> stopList;

    /** The user-editable name of the trade route. */
    private JTextField tradeRouteName;

    /** A box to select stops to add. */
    private JComboBox<String> destinationSelector;

    /** Toggle message display. */
    private JCheckBox messagesBox;

    /** A button to add stops with. */
    private JButton addStopButton;

    /** A button to remove stops with. */
    private JButton removeStopButton;

    /** The panel displaying the goods that could be transported. */
    private final TradeRouteGoodsPanel goodsPanel;

    /** The panel displaying the cargo at the selected stop. */
    private TradeRouteCargoPanel cargoPanel;


    /**
     * Create a panel to define trade route cargos.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param newRoute The {@code TradeRoute} to operate on.
     */
    public TradeRouteInputPanel(FreeColClient freeColClient,
                                TradeRoute newRoute) {
        super(freeColClient, null,
              new MigLayout("wrap 4, fill", "[]20[fill]rel"));

        final Game game = freeColClient.getGame();
        final Player player = getMyPlayer();
        final TradeRoute tradeRoute = newRoute.copy(game);

        this.newRoute = newRoute;
        this.cargoHandler = new CargoHandler(this);
        this.dragListener = new DragListener(freeColClient, this);
        this.dropListener = new DropListener();

        this.stopListModel = new DefaultListModel<>();
        for (TradeRouteStop stop : tradeRoute.getStopList()) {
            this.stopListModel.addElement(stop);
        }

        this.stopList = new JList<>(this.stopListModel);
        this.stopList.setCellRenderer(new StopRenderer());
        this.stopList.setFixedCellHeight(48);
        this.stopList.setDragEnabled(true);
        this.stopList.setTransferHandler(new StopListHandler());
        this.stopList.addKeyListener(new KeyListener() {

                @Override
                public void keyTyped(KeyEvent e) {
                    if (e.getKeyChar() == KeyEvent.VK_DELETE) {
                        deleteCurrentlySelectedStops();
                    }
                }

                @Override
                public void keyPressed(KeyEvent e) {} // Ignore

                @Override
                public void keyReleased(KeyEvent e) {} // Ignore
            });
        this.stopList.addListSelectionListener(this);
        JScrollPane tradeRouteView = new JScrollPane(stopList);

        JLabel nameLabel = Utility.localizedLabel("tradeRouteInputPanel.nameLabel");
        this.tradeRouteName = new JTextField(tradeRoute.getName());

        JLabel destinationLabel
            = Utility.localizedLabel("tradeRouteInputPanel.destinationLabel");
        this.destinationSelector = new JComboBox<>();
        this.destinationSelector.setRenderer(new DestinationCellRenderer());
        StringTemplate template = StringTemplate.template("tradeRouteInputPanel.allColonies");
        this.destinationSelector.addItem(Messages.message(template));
        if (player.getEurope() != null) {
            this.destinationSelector.addItem(player.getEurope().getId());
        }
        for (Colony colony : player.getColonyList()) {
            this.destinationSelector.addItem(colony.getId());
        }

        this.messagesBox
            = new JCheckBox(Messages.message("tradeRouteInputPanel.silence"));
        this.messagesBox.setSelected(tradeRoute.isSilent());
        this.messagesBox.addActionListener((ActionEvent ae) -> {
                tradeRoute.setSilent(messagesBox.isSelected());
            });

        this.addStopButton = Utility.localizedButton("tradeRouteInputPanel.addStop");
        this.addStopButton.addActionListener((ActionEvent ae) -> {
                addSelectedStops();
            });

        this.removeStopButton = Utility.localizedButton("tradeRouteInputPanel.removeStop");
        this.removeStopButton.addActionListener((ActionEvent ae) -> {
                deleteCurrentlySelectedStops();
            });

        final List<GoodsType> gtl = getSpecification().getGoodsTypeList();
        this.goodsPanel = new TradeRouteGoodsPanel(transform(gtl,
                GoodsType::isStorable, gt -> buildCargoLabel(gt)));
        this.goodsPanel.setTransferHandler(this.cargoHandler);
        this.goodsPanel.setEnabled(false);
        this.goodsPanel.addMouseListener(this.dropListener);

        this.cargoPanel = new TradeRouteCargoPanel();
        this.cargoPanel.setTransferHandler(this.cargoHandler);
        this.cargoPanel.addMouseListener(this.dropListener);

        JButton cancelButton = Utility.localizedButton("cancel");
        cancelButton.setActionCommand(CANCEL);
        cancelButton.addActionListener(this);
        setCancelComponent(cancelButton);

        add(Utility.localizedHeader("tradeRouteInputPanel.editRoute", false),
            "span, align center");
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
            updateCargoPanel(this.stopListModel.firstElement());
        }
        // update buttons according to selection
        updateButtons();

        getGUI().restoreSavedSize(this, getPreferredSize());
    }

    /**
     * Update the cargo panel to show a given stop.
     *
     * @param stop The {@code TradeRouteStop} to select.
     */
    private void updateCargoPanel(TradeRouteStop stop) {
        this.cargoPanel.setLabels((stop == null) ? null
            : transform(stop.getCargo(), GoodsType::isStorable,
                        gt -> buildCargoLabel(gt)));
    }

    /**
     * Import new goods at the selected stops.
     *
     * @param gt The {@code GoodsType} to import.
     */
    private void enableImport(GoodsType gt) {
        if (gt == null) return;
        this.cargoPanel.addLabel(buildCargoLabel(gt));
        for (int stopIndex : this.stopList.getSelectedIndices()) {
            TradeRouteStop stop = this.stopListModel.get(stopIndex);
            stop.addCargo(gt);
        }
        this.stopList.revalidate();
        this.stopList.repaint();
    }

    /**
     * Cancel import of goods at the selected stops.
     *
     * @param gt The {@code GoodsType} to stop importing.
     */
    private void cancelImport(GoodsType gt) {
        this.cargoPanel.removeType(gt);
        for (int stopIndex : this.stopList.getSelectedIndices()) {
            TradeRouteStop stop = this.stopListModel.get(stopIndex);
            List<GoodsType> cargo = new ArrayList<>(stop.getCargo());
            if (removeInPlace(cargo, matchKey(gt))) {
                stop.setCargo(cargo);
            }
        }
        this.stopList.revalidate();
        this.stopList.repaint();
        this.cargoPanel.revalidate();
        this.cargoPanel.repaint();
    }

    /**
     * Add any stops selected in the destination selector.
     */
    private void addSelectedStops() {
        int startIndex = -1;
        int endIndex = -1;
        int sel = this.destinationSelector.getSelectedIndex();
        if (sel == 0) { // All colonies + Europe
            startIndex = 1;
            endIndex = this.destinationSelector.getItemCount();
        } else { // just one place
            startIndex = sel;
            endIndex = startIndex+1;
        }
        List<GoodsType> cargo = transform(cargoPanel.getComponents(),
            c -> c instanceof TradeRouteCargoLabel,
            c -> ((TradeRouteCargoLabel)c).getType());
        int maxIndex = this.stopList.getMaxSelectionIndex();
        for (int i = startIndex; i < endIndex; i++) {
            String id = this.destinationSelector.getItemAt(i);
            FreeColGameObject fcgo = getGame().getFreeColGameObject(id);
            if (fcgo instanceof Location) {
                TradeRouteStop stop
                    = new TradeRouteStop(getGame(), (Location)fcgo);
                stop.setCargo(cargo);
                if (maxIndex < 0) {
                    this.stopListModel.addElement(stop);
                } else {
                    maxIndex++;
                    this.stopListModel.add(maxIndex, stop);
                }
            }
        }
    }

    /**
     * Convenience function to build a new {@code TradeRouteCargoLabel}.
     *
     * @param gt The {@code GoodsType} for the label.
     * @return A {@code TradeRouteCargoLabel} for the goods type.
     */
    private TradeRouteCargoLabel buildCargoLabel(GoodsType gt) {
        final ImageLibrary lib = getImageLibrary();
        ImageIcon icon = new ImageIcon(lib.getScaledGoodsTypeImage(gt));
        TradeRouteCargoLabel label = new TradeRouteCargoLabel(icon, gt);
        label.setTransferHandler(this.cargoHandler);
        label.addMouseListener(this.dragListener);
        return label;
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
     * Make sure the original route is invalid and remove this panel.
     *
     * Public so that this panel can be signalled to close if the parent
     * TradeRoutePanel is closed.
     */
    public void cancelTradeRoute() {
        this.newRoute.setName(null);
        getGUI().removeComponent(this);
    }

    /**
     * Enables the remove stop button if a stop is selected and disables it
     * otherwise.
     */
    private void updateButtons() {
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
        // Update the trade route with the current settings
        this.newRoute.setName(tradeRouteName.getText());
        this.newRoute.clearStops();
        for (int index = 0; index < this.stopListModel.getSize(); index++) {
            this.newRoute.addStop(this.stopListModel.get(index));
        }
        this.newRoute.setSilent(this.messagesBox.isSelected());

        StringTemplate err = this.newRoute.verify();
        if (err != null) {
            getGUI().showInformationMessage(err);
            this.newRoute.setName(null); // Mark as unacceptable
            return false;
        }
        return true;
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String command = ae.getActionCommand();
        if (command == null) return;
        switch (command) {
        case OK:
            if (!verifyNewTradeRoute()) return;
            // Return to TradeRoutePanel, which will add the route
            // if needed, and it is valid.
            super.actionPerformed(ae);
            break;
        case CANCEL:
            cancelTradeRoute();
            break;
        default:
            super.actionPerformed(ae);
            break;
        }
    }


    // Interface ListSelectionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int[] idx = this.stopList.getSelectedIndices();
        if (idx.length > 0) {
            updateCargoPanel(this.stopListModel.get(idx[0]));
            this.goodsPanel.setEnabled(true);
        } else {
            this.goodsPanel.setEnabled(false);
        }
        updateButtons();
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
