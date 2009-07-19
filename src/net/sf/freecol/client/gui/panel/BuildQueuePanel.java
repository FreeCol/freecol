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
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.DefaultListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.Canvas;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;


public class BuildQueuePanel extends FreeColPanel implements ActionListener, ItemListener {

    private static Logger logger = Logger.getLogger(BuildQueuePanel.class.getName());

    private static final String BUY = "buy";

    private final BuildQueueTransferHandler buildQueueHandler = new BuildQueueTransferHandler();

    private static ListCellRenderer cellRenderer;
    private JList buildQueueList;
    private JList unitList;
    private JList buildingList;
    private JButton buyBuilding;
    private JCheckBox compact;
    private Colony colony; 

    private FeatureContainer featureContainer = new FeatureContainer();

    public BuildQueuePanel(Colony colony, Canvas parent) {

        super(parent, new MigLayout("wrap 3", "[260:][260:][260:]", "[][][300:400:][]"));
        this.colony = colony;

        DefaultListModel current = new DefaultListModel();
        for (BuildableType type : colony.getBuildQueue()) {
            current.addElement(type);
            featureContainer.add(type.getFeatureContainer());
        }

        cellRenderer = new DefaultBuildQueueCellRenderer();

        buildQueueList = new JList(current);
        buildQueueList.setTransferHandler(buildQueueHandler);
        buildQueueList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        buildQueueList.setDragEnabled(true);
        buildQueueList.setCellRenderer(cellRenderer);
        buildQueueList.addMouseListener(new BuildQueueMouseAdapter());

        BuildableListMouseAdapter adapter = new BuildableListMouseAdapter();
        DefaultListModel units = new DefaultListModel();
        unitList = new JList(units);
        unitList.setTransferHandler(buildQueueHandler);
        unitList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        unitList.setDragEnabled(true);
        unitList.setCellRenderer(cellRenderer);
        unitList.addMouseListener(adapter);
        updateUnitList();

        DefaultListModel buildings = new DefaultListModel();
        buildingList = new JList(buildings);
        buildingList.setTransferHandler(buildQueueHandler);
        buildingList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        buildingList.setDragEnabled(true);
        buildingList.setCellRenderer(cellRenderer);
        buildingList.addMouseListener(adapter);
        updateBuildingList();

        JLabel headLine = new JLabel(Messages.message("colonyPanel.buildQueue"));
        headLine.setFont(bigHeaderFont);

        buyBuilding = new JButton(Messages.message("colonyPanel.buyBuilding"));
        buyBuilding.setActionCommand(BUY);
        buyBuilding.addActionListener(this);
        updateBuyBuildingButton();

        compact = new JCheckBox(Messages.message("colonyPanel.compactView"));
        compact.addItemListener(this);

        add(headLine, "span 3, align center, wrap 40");
        add(new JLabel(Messages.message("menuBar.colopedia.unit")), "align center");
        add(new JLabel(Messages.message("colonyPanel.buildQueue")), "align center");
        add(new JLabel(Messages.message("menuBar.colopedia.building")), "align center");
        add(new JScrollPane(unitList), "grow");
        add(new JScrollPane(buildQueueList), "grow");
        add(new JScrollPane(buildingList), "grow, wrap 20");
        add(buyBuilding, "span, split 3");
        add(compact);
        add(okButton, "tag ok");
    }

    private void updateUnitList() {
        DefaultListModel units = (DefaultListModel) unitList.getModel();
        units.clear();
        for (UnitType unitType : FreeCol.getSpecification().getUnitTypeList()) {
            if (!unitType.getGoodsRequired().isEmpty()
                && (colony.getFeatureContainer().hasAbility("model.ability.build", unitType)
                    || featureContainer.hasAbility("model.ability.build", unitType))) {
                units.addElement(unitType);
            }
        }
    }

    private void updateBuildingList() {
        DefaultListModel buildings = (DefaultListModel) buildingList.getModel();
        DefaultListModel current = (DefaultListModel) buildQueueList.getModel();
        buildings.clear();
        for (BuildingType buildingType : FreeCol.getSpecification().getBuildingTypeList()) {
            Building oldBuilding = colony.getBuilding(buildingType);
            BuildingType oldBuildingType = (oldBuilding == null) ? null : oldBuilding.getType();
            if ((oldBuildingType == buildingType.getUpgradesFrom()
                 || current.contains(buildingType.getUpgradesFrom()))
                && !current.contains(buildingType)) {
                buildings.addElement(buildingType);
            }
        }
    }

    private void updateAllLists() {
        DefaultListModel current = (DefaultListModel) buildQueueList.getModel();
        featureContainer = new FeatureContainer();
        for (Object type: current.toArray()) {
            featureContainer.add(((BuildableType) type).getFeatureContainer());
        }
        updateUnitList();
        updateBuildingList();
    }

    private void updateBuyBuildingButton(){
        boolean canBuy = true;
        if(!colony.canPayToFinishBuilding() 
                || colony.getPriceForBuilding() == 0){
            canBuy = false;
        }
        buyBuilding.setEnabled(canBuy);
        buyBuilding.repaint();
    }

    private boolean hasBuildingType(Colony colony, BuildingType buildingType) {
        if (colony.getBuilding(buildingType) == null) {
            return false;
        } else if (colony.getBuilding(buildingType).getType() == buildingType) {
            return true;
        } else if (buildingType.getUpgradesTo() != null) {
            return hasBuildingType(colony, buildingType.getUpgradesTo());
        } else {
            return false;
        }
    }

    private List<BuildableType> getBuildableTypes(JList list) {
        List<BuildableType> result = new ArrayList<BuildableType>();
        if (list != null) {
            ListModel model = list.getModel();
            for (int index = 0; index < model.getSize(); index++) {
                Object object = model.getElementAt(index);
                if (object instanceof BuildableType) {
                    result.add((BuildableType) object);
                }
            }
        }
        return result;
    }

    private List<BuildableType> getBuildableTypes(Object[] objects) {
        List<BuildableType> result = new ArrayList<BuildableType>();
        if (objects != null) {
            for (Object object : objects) {
                if (object instanceof BuildableType) {
                    result.add((BuildableType) object);
                }
            }
        }
        return result;
    }

    private int getMinimumIndex(BuildableType buildableType, JList target, List<BuildableType> buildables) {
        if (colony.canBuild(buildableType)) {
            return 0;
        } else if (buildableType instanceof UnitType) {
            List<BuildableType> buildQueue = getBuildableTypes(target);
            for (int index = 0; index < buildQueue.size(); index++) {
                if (buildQueue.get(index).hasAbility("model.ability.build", buildableType)) {
                    return index + 1;
                }
            }
        } else if (buildableType instanceof BuildingType) {
            BuildingType upgradesFrom = ((BuildingType) buildableType).getUpgradesFrom();
            if (upgradesFrom == null) {
                return 0;
            } else {
                List<BuildableType> buildQueue = getBuildableTypes(target);
                for (int index = 0; index < buildQueue.size(); index++) {
                    if (upgradesFrom.equals(buildQueue.get(index))) {
                        return index + 1;
                    }
                }
                if (buildables != null && buildables.contains(upgradesFrom)) {
                    return buildQueue.size();
                } else {
                    return -1;
                }
            }
        }
        return -1;
    }            


    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            getController().setBuildQueue(colony, getBuildableTypes(buildQueueList));
            getCanvas().remove(this);
        } else if (BUY.equals(command)) {
            getController().setBuildQueue(colony, getBuildableTypes(buildQueueList));
            getController().payForBuilding(colony);
            getCanvas().updateGoldLabel();
            updateBuyBuildingButton();
        } else {
            logger.warning("Invalid ActionCommand: " + command);
        }
    }

    public void itemStateChanged(ItemEvent event) {
        if (compact.isSelected()) {
            if (cellRenderer instanceof DefaultBuildQueueCellRenderer) {
                cellRenderer = new SimpleBuildQueueCellRenderer();
            }
        } else if (cellRenderer instanceof SimpleBuildQueueCellRenderer) {
            cellRenderer = new DefaultBuildQueueCellRenderer();
        }
        buildQueueList.setCellRenderer(cellRenderer);
        buildingList.setCellRenderer(cellRenderer);
        unitList.setCellRenderer(cellRenderer);
    }

    /**
     * This class implements a transfer handler able to transfer
     * <code>BuildQueueItem</code>s between the build queue list, the
     * unit list and the building list.
     */
    public class BuildQueueTransferHandler extends TransferHandler {

        private final DataFlavor buildQueueFlavor = new DataFlavor(List.class, "BuildingQueueFlavor");
        
        JList source = null;
        int[] indices = null;
        int targetIndex = -1;  // preferred index of target list
        int numberOfItems = 0;  // number of items to be added

        /**
         * Imports a build queue into the build queue list, the
         * building list or the unit list, if possible.
         * @param comp The list which imports data.
         * @param data The build queue to import.
         * @return Whether the import was successful.
         */
        public boolean importData(JComponent comp, Transferable data) {

            if (!canImport(comp, data.getTransferDataFlavors())) {
                return false;
            }

            JList target = null;
            List<BuildableType> buildQueue = new ArrayList<BuildableType>();
            DefaultListModel targetModel;

            try {
                target = (JList) comp;
                targetModel = (DefaultListModel) target.getModel();
                Object transferData = data.getTransferData(buildQueueFlavor);
                if (transferData instanceof List) {
                    for (Object object : (List) transferData) {
                        if (object instanceof BuildableType) {
                            if ((object instanceof BuildingType
                                 && target == unitList)
                                || (object instanceof UnitType
                                    && target == buildingList)) {
                                return false;
                            }
                            buildQueue.add((BuildableType) object);
                        }
                    }
                }                    
            } catch (Exception e) {
                logger.warning(e.toString());
                return false;
            }

            for (BuildableType type : buildQueue) {
                if (getMinimumIndex(type, target, buildQueue) < 0) {
                    return false;
                }
            }

            int preferredIndex = target.getSelectedIndex();

            if (source.equals(target)) {
                if (target == buildQueueList) {
                    // don't drop selection on itself
                    if (indices != null && 
                        preferredIndex >= indices[0] - 1 &&
                        preferredIndex <= indices[indices.length - 1]) {
                        indices = null;
                        return true;
                    }
                    numberOfItems = buildQueue.size();
                } else {
                    return false;
                }
            }

            int maxIndex = targetModel.size();
            if (preferredIndex < 0 || preferredIndex > maxIndex) {
                preferredIndex = maxIndex;
            }
            targetIndex = preferredIndex;

            for (int index = 0; index < buildQueue.size(); index++) {
                int minimumIndex = getMinimumIndex(buildQueue.get(index), target, null);
                if (minimumIndex < targetIndex + index) {
                    minimumIndex = targetIndex + index;
                }
                targetModel.insertElementAt(buildQueue.get(index), minimumIndex);
            }

            return true;
        }

        /**
         * Cleans up after a successful import.
         * @param source The component that has exported data.
         * @param data The data exported.
         * @param action The transfer action, e.g. MOVE.
         */
        protected void exportDone(JComponent source, Transferable data, int action) {

            if ((action == MOVE) && (indices != null)) {
                DefaultListModel model = (DefaultListModel) ((JList) source).getModel();

                // adjust indices if necessary
                if (numberOfItems > 0) {
                    for (int i = 0; i < indices.length; i++) {
                        if (indices[i] > targetIndex) {
                            indices[i] += numberOfItems;
                        }
                    }
                }
                // has to be done backwards
                for (int i = indices.length -1; i >= 0; i--) {
                    model.remove(indices[i]);
                }
            }
            // clean up
            indices = null;
            targetIndex = -1;
            numberOfItems = 0;
            updateAllLists();
        }

        /**
         * Returns <code>true</code> if the component can import this
         * data flavor.
         * @param comp The component to import data.
         * @param flavors An array of data flavors.
         */
        public boolean canImport(JComponent comp, DataFlavor[] flavors) {
            if (flavors == null) {
                return false;
            } else {
                for (DataFlavor flavor : flavors) {
                    if (flavor.equals(buildQueueFlavor)) {
                        return true;
                    }
                }
                return false;
            }
        }

        /**
         * Returns a <code>Transferable</code> suitable for wrapping
         * the build queue.
         * @param comp The source of the build queue.
         * @return A Transferable suitable for wrapping the build
         * queue. 
         */
        protected Transferable createTransferable(JComponent comp) {
            if (comp instanceof JList) {
                source = (JList) comp;
                indices = source.getSelectedIndices();
                List<BuildableType> buildQueue = getBuildableTypes(source.getSelectedValues());
                return new BuildQueueTransferable(buildQueue);
            } else {
                return null;
            }
        }

        /**
         * Returns the possible source actions of the component.
         * @param comp The source component.
         * @return The possible source actions of the component.
         */
        public int getSourceActions(JComponent comp) {
            if (comp == unitList) {
                return COPY;
            } else {
                return MOVE;
            }
        }

        /**
         * This class implements the <code>Transferable</code> interface.
         */
        public class BuildQueueTransferable implements Transferable {
            private List<BuildableType> buildQueue;
            private final DataFlavor[] supportedFlavors = new DataFlavor[] {
                buildQueueFlavor
            };

            /**
             * Default constructor.
             * @param buildQueue The build queue to transfer.
             */
            public BuildQueueTransferable(List<BuildableType> buildQueue) {
                this.buildQueue = buildQueue;
            }

            /**
             * Returns the build queue from the <code>Transferable</code>.
             * @param flavor The data flavor to use.
             * @return The build queue from the <code>Transferable</code>.
             */
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (isDataFlavorSupported(flavor)) {
                    return buildQueue;
                } else {
                    throw new UnsupportedFlavorException(flavor);
                }
            }

            /**
             * Returns the build queue from the <code>Transferable</code>.
             * @param flavor The data flavor to use.
             * @return The build queue from the <code>Transferable</code>.
             */
            public List<BuildableType> getBuildQueue() {
                return buildQueue;
            }

            /**
             * Returns an array of supported data flavors.
             * @return An array of supported data flavors.
             */
            public DataFlavor[] getTransferDataFlavors() {
                return supportedFlavors;
            }

            /**
             * Returns <code>true</code> if this data flavor is supported.
             * @param flavor The data flavor.
             * @return Whether this data flavor is supported.
             */
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                for (DataFlavor myFlavor : supportedFlavors) {
                    if (myFlavor.equals(flavor)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    class DefaultBuildQueueCellRenderer implements ListCellRenderer {

        JPanel itemPanel = new JPanel(new MigLayout("", "", ""));
        JLabel imageLabel = new JLabel();
        JLabel nameLabel = new JLabel();

        Map<FreeColGameObjectType, ImageIcon> imageCache
            = new HashMap<FreeColGameObjectType, ImageIcon>();

        public DefaultBuildQueueCellRenderer() {
            // do nothing
        }

        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            BuildableType item = (BuildableType) value;

            itemPanel.removeAll();

            ImageIcon buildableIcon = imageCache.get(value);
            if (buildableIcon == null) {
                Image buildableImage = ResourceManager.getImage(item.getId() + ".image");
                if (buildableImage != null) {
                    buildableImage = buildableImage.getScaledInstance(-1, 48, Image.SCALE_SMOOTH);
                    buildableIcon = new ImageIcon(buildableImage);
                    imageCache.put(item, buildableIcon);
                }
            }
            imageLabel.setIcon(buildableIcon);

            nameLabel.setText(item.getName());
            itemPanel.add(imageLabel, "span 1 2");
            itemPanel.add(nameLabel, "wrap");

            List<AbstractGoods> goodsRequired = item.getGoodsRequired();
            int size = goodsRequired.size();
            for (int i = 0; i < size; i++) {
                AbstractGoods goods = goodsRequired.get(i);
                ImageIcon goodsIcon = imageCache.get(goods.getType());
                if (goodsIcon == null) {
                    goodsIcon = getLibrary().getScaledGoodsImageIcon(goods.getType(), 0.66f);
                    imageCache.put(goods.getType(), goodsIcon);
                }
                JLabel goodsLabel = new JLabel(Integer.toString(goods.getAmount()), 
                                               goodsIcon,
                                               SwingConstants.CENTER);
                if (i == 0 && size > 1) {
                    itemPanel.add(goodsLabel, "split " + size);
                } else {
                    itemPanel.add(goodsLabel);
                }
            }
            if (isSelected) {
                itemPanel.setBorder(BorderFactory.createLineBorder(LINK_COLOR, 2));
            } else {
                itemPanel.setBorder(null);
            }
            return itemPanel;
        }
    }

    class SimpleBuildQueueCellRenderer implements ListCellRenderer {

        JLabel label = new JLabel();
        Border emptyBorder = BorderFactory.createEmptyBorder(2, 5, 2, 5);
        Border lineBorder =
            BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINK_COLOR, 2),
                                               BorderFactory.createEmptyBorder(2, 3, 3, 3));

        public SimpleBuildQueueCellRenderer() {
            label.setBorder(emptyBorder);
        }

        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            label.setText(((BuildableType) value).getName());
            if (isSelected) {
                label.setBorder(lineBorder);
            } else {
                label.setBorder(emptyBorder);
            }
            return label;
        }
    }

    class BuildQueueMouseAdapter extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            if ((e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger())) {
                JList source = (JList) e.getSource();
                for (Object type : source.getSelectedValues()) {
                    ((DefaultListModel) buildQueueList.getModel()).removeElement(type);
                }
                updateAllLists();
            }
        }
    }

    class BuildableListMouseAdapter extends MouseAdapter {

        public void mouseClicked(MouseEvent e) {
            if ((e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger())) {
                JList source = (JList) e.getSource();
                for (Object type : source.getSelectedValues()) {
                    ((DefaultListModel) buildQueueList.getModel()).addElement(type);
                }
                updateAllLists();
            }
        }
    }

}

