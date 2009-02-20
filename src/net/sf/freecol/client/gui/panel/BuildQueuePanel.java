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

import java.io.IOException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.BuildQueue;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;


public class BuildQueuePanel extends FreeColPanel implements ActionListener {

    private static Logger logger = Logger.getLogger(BuildQueuePanel.class.getName());

    private final BuildQueueTransferHandler buildQueueHandler = new BuildQueueTransferHandler();

    private BuildQueue finished, current, units, buildings;
    private Colony colony; 

    public BuildQueuePanel(Canvas parent) {
        super(parent, new MigLayout("wrap 3", "", ""));
    }

    public void initialize(Colony colony) {

        this.colony = colony;

        removeAll();

        current = new BuildQueue(colony, colony.getBuildQueue());

        finished = new BuildQueue();
        for (Building building : colony.getBuildings()) {
            finished.addUnchecked(building.getType());
        }

        units = new BuildQueue(BuildQueue.Type.UNITS);
        for (UnitType unitType : FreeCol.getSpecification().getUnitTypeList()) {
            if (!unitType.getGoodsRequired().isEmpty()) {
                units.addUnchecked(unitType);
            }
        }

        buildings = new BuildQueue(BuildQueue.Type.BUILDINGS);
        for (BuildingType buildingType : FreeCol.getSpecification().getBuildingTypeList()) {
            if (!hasBuildingType(colony, buildingType)) {
                buildings.add(buildingType);
            }
        }

        BuildQueueCellRenderer cellRenderer = new BuildQueueCellRenderer();
	JList buildQueueList = new JList(current);
	buildQueueList.setTransferHandler(buildQueueHandler);
	buildQueueList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	buildQueueList.setDragEnabled(true);
        buildQueueList.setCellRenderer(cellRenderer);

	JList unitList = new JList(units);
	unitList.setTransferHandler(buildQueueHandler);
	unitList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	unitList.setDragEnabled(true);
        unitList.setCellRenderer(cellRenderer);

	JList buildingList = new JList(buildings);
	buildingList.setTransferHandler(buildQueueHandler);
	buildingList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	buildingList.setDragEnabled(true);
        buildingList.setCellRenderer(cellRenderer);

        JScrollPane buildQueueView = new JScrollPane(buildQueueList);
        buildQueueView.setPreferredSize(new Dimension(260, 400));
        JPanel buildQueuePanel = new JPanel();
        buildQueuePanel.setLayout(new BorderLayout());
        buildQueuePanel.add(buildQueueView, BorderLayout.CENTER);
        buildQueuePanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JScrollPane unitView = new JScrollPane(unitList);
        unitView.setPreferredSize(new Dimension(260, 400));
        JPanel unitPanel = new JPanel();
        unitPanel.setLayout(new BorderLayout());
        unitPanel.add(unitView, BorderLayout.CENTER);
        unitPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JScrollPane buildingView = new JScrollPane(buildingList);
        buildingView.setPreferredSize(new Dimension(260, 400));
        JPanel buildingPanel = new JPanel();
        buildingPanel.setLayout(new BorderLayout());
        buildingPanel.add(buildingView, BorderLayout.CENTER);
        buildingPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JLabel headLine = new JLabel(Messages.message("colonyPanel.buildQueue"));
        headLine.setFont(bigHeaderFont);

        JButton ok = new JButton(Messages.message("ok"));
        ok.setActionCommand("ok");
        ok.addActionListener(this);

        JButton buyBuilding = new JButton(Messages.message("colonyPanel.buyBuilding"));
        buyBuilding.setActionCommand("buy");
        buyBuilding.addActionListener(this);

        add(headLine, "span 3, align center, wrap 40");
        add(new JLabel(Messages.message("menuBar.colopedia.unit")), "align center");
        add(new JLabel(Messages.message("colonyPanel.buildQueue")), "align center");
        add(new JLabel(Messages.message("menuBar.colopedia.building")), "align center");
        add(unitPanel);
        add(buildQueuePanel);
        add(buildingPanel, "wrap 20");
        add(buyBuilding, "span, split 2, align center");
        add(ok);
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

    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if ("ok".equals(command)) {
            colony.setBuildQueue(current.getBuildableTypes());
            getCanvas().remove(this);
        } else if ("buy".equals(command)) {
            colony.setBuildQueue(current.getBuildableTypes());
            getCanvas().getClient().getInGameController().payForBuilding(colony);
            getCanvas().updateGoldLabel();
        } else {
            logger.warning("Invalid ActionCommand: " + command);
        }
    }

    /**
     * This class implements a transfer handler able to transfer
     * <code>BuildQueueItem</code>s between the build queue list, the
     * unit list and the building list.
     */
    public class BuildQueueTransferHandler extends TransferHandler {

	private final DataFlavor buildQueueFlavor = new DataFlavor(BuildQueue.class, "BuildingQueueFlavor");
	
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
	    JList target = null;
	    BuildQueue buildQueue = null;
	    BuildQueue targetQueue;

	    if (!canImport(comp, data.getTransferDataFlavors())) {
                System.out.println("Can't import data flavor");
		return false;
	    }

	    try {
		target = (JList) comp;
		targetQueue = (BuildQueue) target.getModel();
		buildQueue = (BuildQueue) data.getTransferData(buildQueueFlavor);
	    } catch (UnsupportedFlavorException e) {
		System.out.println("importData: unsupported data flavor " + e);
		return false;
	    } catch (IOException e) {
		System.out.println("importData: I/O exception " + e);
		return false;
	    }

            if (!buildQueue.dependenciesSatisfiedBy(targetQueue, finished)) {
                return false;
            }

	    int preferredIndex = target.getSelectedIndex();

	    if (source.equals(target)) {
		if (targetQueue.getType() == BuildQueue.Type.MIXED) {
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

	    int maxIndex = targetQueue.getSize();
	    if (preferredIndex < 0) {
		preferredIndex = maxIndex; 
	    } else {
		if (preferredIndex > maxIndex) {
		    preferredIndex = maxIndex;
		}
	    }
	    targetIndex = preferredIndex;

	    targetQueue.addAll(preferredIndex, buildQueue);
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
		BuildQueue model = (BuildQueue) ((JList) source).getModel();

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
	}

	/**
	 * Returns <code>true</code> if the component can import this
	 * data flavor.
	 * @param comp The component to import data.
	 * @param flavors An array of data flavors.
	 */
	public boolean canImport(JComponent comp, DataFlavor[] flavors) {
	    if (flavors == null) {
		System.out.println("flavors == null");
		return false;
	    } else {
		BuildQueue buildQueue = (BuildQueue) ((JList) comp).getModel();
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
		BuildQueue buildQueue = new BuildQueue(colony, source.getSelectedValues());
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
	    BuildQueue buildQueue = (BuildQueue) ((JList) comp).getModel();
	    if (buildQueue.isReadOnly()) {
		return COPY;
	    } else {
		return MOVE;
	    }
	}

	/**
	 * This class implements the <code>Transferable</code> interface.
	 */
	public class BuildQueueTransferable implements Transferable {
	    private BuildQueue buildQueue;
	    private final DataFlavor[] supportedFlavors = new DataFlavor[] {
		buildQueueFlavor
	    };

	    /**
	     * Default constructor.
	     * @param buildQueue The build queue to transfer.
	     */
	    public BuildQueueTransferable(BuildQueue buildQueue) {
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

    class BuildQueueCellRenderer implements ListCellRenderer {

        public BuildQueueCellRenderer() {
        }

        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            BuildableType item = (BuildableType) value;

            JPanel itemPanel = new JPanel(new MigLayout("", "", ""));
            JLabel imageLabel = new JLabel();
            ImageLibrary library = getCanvas().getGUI().getImageLibrary();
            Image buildableImage = ResourceManager.getImage(item.getId() + ".image");
            if (buildableImage != null) {
                buildableImage = buildableImage.getScaledInstance(-1, 48, Image.SCALE_SMOOTH);
                imageLabel = new JLabel(new ImageIcon(buildableImage));
            }
            itemPanel.add(imageLabel, "span 1 2");
            itemPanel.add(new JLabel(item.getName()), "wrap");

            List<AbstractGoods> goodsRequired = item.getGoodsRequired();
            int size = goodsRequired.size();
            if (size > 0) {
                AbstractGoods goods = goodsRequired.get(0);
                JLabel goodsLabel = new JLabel(Integer.toString(goods.getAmount()), 
                                               library.getScaledGoodsImageIcon(goods.getType(), 0.66f),
                                               SwingConstants.CENTER);
                if (size == 1) {
                    itemPanel.add(goodsLabel);
                } else {
                    itemPanel.add(goodsLabel, "split " + size);
                    for (int i = 1; i < size; i++) {
                        goods = goodsRequired.get(i);
                        goodsLabel = new JLabel(Integer.toString(goods.getAmount()),
                                                library.getScaledGoodsImageIcon(goods.getType(), 0.66f),
                                                SwingConstants.CENTER);
                        itemPanel.add(goodsLabel);
                    }
                }
            }
            if (isSelected) {
                itemPanel.setOpaque(false);
            }
            return itemPanel;
        }
    }
}

