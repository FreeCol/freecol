/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import static net.sf.freecol.common.util.CollectionUtils.any;
import static net.sf.freecol.common.util.CollectionUtils.matchKeyEquals;
import static net.sf.freecol.common.util.CollectionUtils.transform;

import java.awt.Component;
import java.awt.GridLayout;
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

import javax.swing.AbstractAction;
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
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.PanelUI;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.label.GoodsTypeLabel;
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


/**
 * Allows the user to edit trade routes.
 */
public final class TradeRouteInputPanel extends FreeColPanel 
    implements ListSelectionListener {

    private static final Logger logger = Logger.getLogger(TradeRouteInputPanel.class.getName());

    public static final DataFlavor STOP_FLAVOR
        = new DataFlavor(TradeRouteStop.class, "Stop");

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
     * This should *not* change under drag/drop, but a drop indicates
     * that the dropped goods type is no longer needed to be loaded
     * at the currently selected stop.
     */
    private class AllGoodsTypesPanel extends GoodsTypePanel {

        /** The currently selected goods types. */
        private List<GoodsTypeLabel> labels;
        
        public AllGoodsTypesPanel(List<GoodsTypeLabel> labels) {
            super(new GridLayout(0, 4, MARGIN, MARGIN), true);

            this.labels = labels;
            setOpaque(false);
            setBorder(Utility.localizedBorder("goods"));
            reset();
        }

        public void reset() {
            setLabels(this.labels);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setEnabled(boolean enable) {
            super.setEnabled(enable);
            for (Component child : getComponents()) {
                if (child instanceof GoodsTypeLabel) child.setEnabled(enable);
            }
        }

        // Interface DropTarget

        /**
         * {@inheritDoc}
         */
        @Override
        public Component add(Component comp, boolean editState) {
            if (comp instanceof GoodsTypeLabel) {
                GoodsTypeLabel gtl = (GoodsTypeLabel)comp;
                cancelImport(gtl);
                
                // XXX: Do not add the component -- we already have the GoodsType here.
                
                return comp;
            } else {
                return super.add(comp);
            }
        }
    }

    /**
     * Panel for the types of goods that are to be loaded onto the carrier
     * at the current stop.  This changes under drag/drop, and a drop
     * adds the dropped goods type to be loaded at the currently selected
     * stop.
     */
    private class StopGoodsTypesPanel extends GoodsTypePanel {

        public StopGoodsTypesPanel() {
            super(new MigLayout("wrap 10"), false);
        }

        // Interface DropTarget

        /**
         * {@inheritDoc}
         */
        @Override
        public Component add(Component comp, boolean editState) {
            if (comp instanceof GoodsTypeLabel) {
                final GoodsTypeLabel dndComp = (GoodsTypeLabel) comp;
                final Component newComp = super.add(new GoodsTypeLabel(dndComp), editState);
                enableImport(dndComp.getType());
                revalidate();
                repaint();
                return newComp;
            } else {
                return super.add(comp);
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
            final JList list = (JList) c;
            final DefaultListModel model = (DefaultListModel)list.getModel();
            final int[] indicies = list.getSelectedIndices();
            final List<TradeRouteStop> stops = new ArrayList<>(indicies.length);
            for (int i : indicies) {
                stops.add((TradeRouteStop) model.get(i));
            }
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
            final JList<TradeRouteStop> stl = TradeRouteInputPanel.this.stopList;
            try {
                if (target == stl && canImport(target, data.getTransferDataFlavors())) {
                    @SuppressWarnings("unchecked")
                    final List<TradeRouteStop> stops = (List<TradeRouteStop>) data.getTransferData(STOP_FLAVOR);
                    
                    final DefaultListModel<TradeRouteStop> newModel = new DefaultListModel<>();
                    final int targetIndex = stl.getMaxSelectionIndex();
                    
                    final List<TradeRouteStop> oldList = getAllValues(stl);
                    if (targetIndex < 0) {
                        oldList.removeAll(stops);
                        newModel.addAll(oldList);
                        newModel.addAll(stops);
                    } else {
                        final List<TradeRouteStop> beforeList = new ArrayList<>(oldList.subList(0, targetIndex));
                        beforeList.removeAll(stops);
                        
                        final List<TradeRouteStop> afterList = new ArrayList<>(oldList.subList(targetIndex, oldList.size()));
                        afterList.removeAll(stops);
                        
                        newModel.addAll(beforeList);
                        newModel.addAll(stops);
                        newModel.addAll(afterList);
                    }
                    
                    stopListModel = newModel;
                    stl.setModel(newModel);
                    if (targetIndex < 0) {
                        stl.setSelectedIndex(newModel.getSize() - 1);
                    } else {
                        stl.setSelectedIndex(targetIndex);
                    }
                    stl.invalidate();
                    stl.repaint();
                    
                    return true;
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Drag&drop failed.", e);
                return false;
            } catch (UnsupportedFlavorException e) {
                logger.log(Level.WARNING, "Drag&drop failed.", e);
                return false;
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Drag&drop failed.", e);
                throw e;
            }
            return false;
        }

        private List<TradeRouteStop> getAllValues(final JList<TradeRouteStop> jList) {
            final List<TradeRouteStop> oldList = new ArrayList<>();
            for (int i=0; i<jList.getModel().getSize(); i++) {
                oldList.add(jList.getModel().getElementAt(i));
            }
            return oldList;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
        }
    }

    private class StopRenderer implements ListCellRenderer<TradeRouteStop> {

        private final JPanel SELECTED_COMPONENT = new JPanel();
        private final JPanel NORMAL_COMPONENT = new JPanel();

        public StopRenderer() {
            NORMAL_COMPONENT.setLayout(new MigLayout("", "[center][]"));
            NORMAL_COMPONENT.setOpaque(false);
            SELECTED_COMPONENT.setLayout(new MigLayout("", "[center][]"));
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
            ImageIcon ii;
            JLabel name;

            if (location instanceof TradeLocation) {
                TradeLocation tl = (TradeLocation) location;
                if (tl.canBeInput() == true) {
                    name = tl.getNameAsJlabel();
                } else   {
                    throw new IllegalStateException("Bogus location: " + location);
                }
            } else {
                throw new IllegalStateException("Bogus location: " + location);
            }

            if (location instanceof Europe) {
                Player owner = ((Europe)location).getOwner();
                ii = new ImageIcon(lib.getSmallerNationImage(owner.getNation()));
            } else if (location instanceof Colony) {
                Colony colony = (Colony) location;
                ii = new ImageIcon(lib.getSmallerSettlementImage(colony));
            } else {
                throw new IllegalStateException("Bogus location: " + location);
            }
            
            name.setVerticalTextPosition(SwingConstants.CENTER);
            
            final int width = (int) (80 * getImageLibrary().getScaleFactor());
            final int height = (int) (60 * getImageLibrary().getScaleFactor());
            panel.add(new JLabel(ii), "spany, width " + width + ", height " + height);
            panel.add(name, "span, wrap");
            for (GoodsType cargo : value.getCargo()) {
                ii = new ImageIcon(lib.getSmallerGoodsTypeImage(cargo));
                panel.add(new JLabel(ii));
            }
            return panel;
        }
    }


    /**
     * The original route passed to this panel.  We are careful not to
     * modify it until we are sure all is well.
     */
    private final TradeRoute newRoute;

    /** The TransferHandler for the cargo labels. */
    private TransferHandler transferHandler;

    /** Mouse listener to use throughout. */
    private transient MouseListener dragListener;

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

    /** The panel displaying all goods types that could be transported. */
    private AllGoodsTypesPanel allGoodsTypesPanel;

    /** The panel displaying the goods types to collect at the selected stop. */
    private StopGoodsTypesPanel stopGoodsTypesPanel;


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
        this.transferHandler = new DefaultTransferHandler(freeColClient, this);
        this.dragListener = new DragListener(freeColClient, this);

        this.stopListModel = new DefaultListModel<>();
        for (TradeRouteStop stop : tradeRoute.getStopList()) {
            this.stopListModel.addElement(stop);
        }

        this.stopList = new JList<>(this.stopListModel);
        this.stopList.setCellRenderer(new StopRenderer());
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

        JLabel nameLabel
            = Utility.localizedLabel("tradeRouteInputPanel.nameLabel");
        this.tradeRouteName = new JTextField(tradeRoute.getName());

        JLabel destinationLabel
            = Utility.localizedLabel("tradeRouteInputPanel.destinationLabel");
        this.destinationSelector = new JComboBox<>();
        this.destinationSelector.setRenderer(new DestinationCellRenderer());
        StringTemplate template
            = StringTemplate.template("tradeRouteInputPanel.allColonies");
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

        this.addStopButton
            = Utility.localizedButton("tradeRouteInputPanel.addStop");
        this.addStopButton.addActionListener((ActionEvent ae) -> {
                addSelectedStops();
            });

        this.removeStopButton
            = Utility.localizedButton("tradeRouteInputPanel.removeStop");
        this.removeStopButton.addActionListener((ActionEvent ae) -> {
                deleteCurrentlySelectedStops();
            });

        final List<GoodsType> gtl = getSpecification().getGoodsTypeList();
        this.allGoodsTypesPanel = new AllGoodsTypesPanel(transform(gtl,
                GoodsType::isStorable, gt -> buildCargoLabel(gt)));
        this.allGoodsTypesPanel.setTransferHandler(this.transferHandler);
        this.allGoodsTypesPanel.setEnabled(false);

        this.stopGoodsTypesPanel = new StopGoodsTypesPanel();
        this.stopGoodsTypesPanel.setTransferHandler(this.transferHandler);

        JButton cancelButton = Utility.localizedButton("cancel");
        cancelButton.setActionCommand(CANCEL);
        cancelButton.addActionListener(this);
        setEscapeAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    TradeRouteInputPanel.this.cancelTradeRoute();
                }
            });
                    
        add(Utility.localizedHeader("tradeRouteInputPanel.editRoute",
                                    Utility.FONTSPEC_TITLE),
            "span, align center");
        add(tradeRouteView, "span 1 5, grow");
        add(nameLabel);
        add(this.tradeRouteName, "span");
        add(destinationLabel);
        add(this.destinationSelector, "span");
        add(this.messagesBox);
        add(this.addStopButton);
        add(this.removeStopButton, "span");
        add(this.allGoodsTypesPanel, "span");
        
        final int iconHeight = (int) (80 * getImageLibrary().getScaleFactor());
        add(this.stopGoodsTypesPanel, "span, height " + iconHeight + "px:, growy");
        add(okButton, "newline 20, span, split 2, tag ok");
        add(cancelButton, "tag cancel");
        
        setEscapeAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                cancelButton.doClick();
            }
        });

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
        this.stopGoodsTypesPanel.setLabels((stop == null) ? null
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
        for (int stopIndex : this.stopList.getSelectedIndices()) {
            TradeRouteStop stop = this.stopListModel.get(stopIndex);
            stop.addCargo(gt);
            
            final int[] idx = this.stopList.getSelectedIndices();
            if (idx.length > 0) {
                updateCargoPanel(this.stopListModel.get(idx[0]));
            }
        }
        this.stopList.revalidate();
        this.stopList.repaint();
    }

    /**
     * Cancel import of goods at the selected stops.
     *
     * @param gt The {@code GoodsType} to stop importing.
     */
    private void cancelImport(GoodsTypeLabel gtl) {
        this.stopGoodsTypesPanel.remove(gtl);
        
        final GoodsType gt = gtl.getType();
        for (int stopIndex : this.stopList.getSelectedIndices()) {
            TradeRouteStop stop = this.stopListModel.get(stopIndex);
            List<GoodsType> cargo = new ArrayList<>(stop.getCargo());
            
            // Only remove the GoodsType once.
            final int cargoIndex = cargo.indexOf(gt);
            if (cargoIndex >= 0) {
                cargo.remove(cargoIndex);
                stop.setCargo(cargo);
            }
        }
        this.stopList.revalidate();
        this.stopList.repaint();
        this.stopGoodsTypesPanel.revalidate();
        this.stopGoodsTypesPanel.repaint();
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
        List<GoodsType> cargo
            = transform(this.stopGoodsTypesPanel.getComponents(),
                        c -> c instanceof GoodsTypeLabel,
                        c -> ((GoodsTypeLabel)c).getType());
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
     * Convenience function to build a new {@code GoodsTypeLabel}.
     *
     * @param gt The {@code GoodsType} for the label.
     * @return A {@code GoodsTypeLabel} for the goods type.
     */
    private GoodsTypeLabel buildCargoLabel(GoodsType gt) {
        GoodsTypeLabel label = new GoodsTypeLabel(getFreeColClient(), gt);
        label.setTransferHandler(this.transferHandler);
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
            getGUI().showInformationPanel(err);
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
            this.allGoodsTypesPanel.setEnabled(true);
        } else {
            this.allGoodsTypesPanel.setEnabled(false);
        }
        updateButtons();
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        this.transferHandler = null;
        this.dragListener = null;
        this.stopListModel.clear();
        this.stopListModel = null;
        this.stopList = null;
        this.tradeRouteName = null;
        this.destinationSelector = null;
        this.messagesBox = null;
        this.addStopButton = null;
        this.removeStopButton = null;
        this.stopGoodsTypesPanel = null;
        this.allGoodsTypesPanel = null;

        super.removeNotify();
    }
}
