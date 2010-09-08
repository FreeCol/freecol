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
                if (destinationSelector.getSelectedIndex() == 0 ) {   // All colonies + Europe
                    startIndex = 1;
                    endIndex = destinationSelector.getItemCount() - 1;  // will use inverted order
                } else {
                    startIndex = destinationSelector.getSelectedIndex();  // just 1 colony
                    endIndex = startIndex;
                }
                for (int i = startIndex; i <= endIndex; i++) {
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

        setLayout(new MigLayout("wrap 3", "[fill][fill][fill]", ""));

        add(getDefaultHeader(Messages.message("traderouteDialog.editRoute")),
            "span, align center");
        add(tradeRouteView, "span 1 5, grow");
        add(nameLabel);
        add(tradeRouteName);        
        add(destinationLabel);
        add(destinationSelector);
        add(goodsPanel, "span");
        add(cargoPanel, "span, height 80:");
        add(addStopButton);
        add(removeStopButton);
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
        if (stopList.getSelectedIndex() == -1) {
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
    	if(listModel.getSize() < 2){
    		 getCanvas().errorMessage("traderouteDialog.notEnoughStops");
    		 return false;
    	}
    	
    	Player player = getCanvas().getClient().getMyPlayer();
        for (int index = 0; index < listModel.getSize(); index++) {
            Stop stop = (Stop) listModel.getElementAt(index);
            if(!TradeRoute.isStopValid(player, stop)){
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
            	
            	if(! verifyNewTradeRoute())
            		break;
            	
                getCanvas().remove(this);
                originalRoute.setName(tradeRouteName.getText());
                ArrayList<Stop> stops = new ArrayList<Stop>();
                for (int index = 0; index < listModel.getSize(); index++) {
                    stops.add((Stop) listModel.getElementAt(index));
                }
                originalRoute.setStops(stops);
                // TODO: update trade routes only if they have been modified
                getController().updateTradeRoute(originalRoute);
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
                        int selectedIndex = stopList.getSelectedIndex();
                        if (selectedIndex >= 0) {
                            for (int index = selectedIndex; index < listModel.size(); index++) {
                                Stop stop = (Stop) listModel.getElementAt(index);
                                stop.addCargo(label.getType());
                                stop.setModified(true);
                            }
                            stopList.revalidate();
                            stopList.repaint();
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
                    int selectedIndex = stopList.getSelectedIndex();
                    if (selectedIndex >= 0) {
                        for (int stopIndex = selectedIndex; stopIndex < listModel.size(); stopIndex++) {
                            Stop stop = (Stop) listModel.getElementAt(stopIndex);
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
     * getController().setExports(colony,
     * goodsType, export.isSelected()); colony.setExports(goodsType,
     * export.isSelected()); } colony.getLowLevel()[goodsType] =
     * ((SpinnerNumberModel) lowLevel.getModel()).getNumber().intValue();
     * colony.getHighLevel()[goodsType] = ((SpinnerNumberModel)
     * highLevel.getModel()).getNumber().intValue();
     * colony.getExportLevel()[goodsType] = ((SpinnerNumberModel)
     * exportLevel.getModel()).getNumber().intValue();
     *  } }
     */

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
            } else {
                Colony colony = (Colony) location;
                icon = new JLabel(new ImageIcon(getLibrary().getSettlementImage(colony, 0.5)));
                name = new JLabel(colony.getName());
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
