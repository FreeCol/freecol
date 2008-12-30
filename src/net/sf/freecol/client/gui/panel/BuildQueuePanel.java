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

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
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

import cz.autel.dmi.HIGLayout;

public class BuildQueuePanel extends ReportPanel {

    private final BuildQueueTransferHandler buildQueueHandler = new BuildQueueTransferHandler();

    private BuildQueue finished, current, units, buildings;
    private Colony colony; 

    private GridLayout gridLayout = new GridLayout(0, 2);

    public BuildQueuePanel(Canvas parent) {
        super(parent, Messages.message("buildQueue"));
    }

    public void initialize(Colony colony) {
        this.colony = colony;

        reportPanel.removeAll();

        current = new BuildQueue(colony.getBuildQueue());

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
        buildQueueView.setPreferredSize(new Dimension(240, 400));
        JPanel buildQueuePanel = new JPanel();
        buildQueuePanel.setLayout(new BorderLayout());
        buildQueuePanel.add(buildQueueView, BorderLayout.CENTER);
        buildQueuePanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JScrollPane unitView = new JScrollPane(unitList);
        unitView.setPreferredSize(new Dimension(240, 400));
        JPanel unitPanel = new JPanel();
        unitPanel.setLayout(new BorderLayout());
        unitPanel.add(unitView, BorderLayout.CENTER);
        unitPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JScrollPane buildingView = new JScrollPane(buildingList);
        buildingView.setPreferredSize(new Dimension(240, 400));
        JPanel buildingPanel = new JPanel();
        buildingPanel.setLayout(new BorderLayout());
        buildingPanel.add(buildingView, BorderLayout.CENTER);
        buildingPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        reportPanel.add(buildQueuePanel);
        reportPanel.add(unitPanel);
        reportPanel.add(buildingPanel);
        //setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        gridLayout.setHgap(5);

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
        int action = Integer.valueOf(command).intValue();
        if (action == OK) {
            /*
            getCanvas().getClient().getInGameController().setBuildQueue(colony, colony.getBuildQueue());
            getCanvas().getColonyPanel().updateBuildingBox();
            getCanvas().getColonyPanel().updateProgressLabel();
            */
            getCanvas().remove(this);
        } else {
            logger.warning("Invalid ActionCommand: " + action);
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
		preferredIndex++;
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
		BuildQueue buildQueue = new BuildQueue(source.getSelectedValues());
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

        private final int[] widths = new int[] {0, 5, 0};
        private final int[] heights = new int[] {83};

        HIGLayout layout = new HIGLayout(widths, heights);

        public BuildQueueCellRenderer() {
        }

        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            BuildableType item = (BuildableType) value;

            JPanel itemPanel = new JPanel(layout);
            JLabel imageLabel = new JLabel();
            ImageLibrary library = getCanvas().getGUI().getImageLibrary();
            if (value instanceof UnitType) {
                imageLabel = new JLabel(library.scaleIcon(library.getUnitImageIcon((UnitType) value), 0.66f));
                itemPanel.add(imageLabel, higConst.rc(1, 1));
            } else if (value instanceof BuildingType) {
                BuildingType building = (BuildingType) value;
                GoodsType outputType = building.getProducedGoodsType();
                if (outputType != null) {
                    JPanel goodsPanel = new JPanel();
                    Image goodsImage = library.getGoodsImage(outputType);
                    ImageIcon imageIcon = new ImageIcon(library.scaleImage(goodsImage, 0.66f));
                    for (int gindex = 0; gindex < building.getLevel(); gindex++) {
                        goodsPanel.add(new JLabel(imageIcon));
                    }
                    if (isSelected) {
                        goodsPanel.setOpaque(false);
                    }
                    itemPanel.add(goodsPanel, higConst.rc(1, 1));
                }
            }
            JPanel costs = new JPanel(gridLayout);
            costs.setBorder(BorderFactory.createTitledBorder(item.getName()));
            costs.setOpaque(false);
            for (AbstractGoods goodsRequired : item.getGoodsRequired()) {
                Image goodsImage = library.getGoodsImage(goodsRequired.getType());
                ImageIcon imageIcon = new ImageIcon(library.scaleImage(goodsImage, 0.66f));
                costs.add(new JLabel(String.valueOf(goodsRequired.getAmount()),
                                     imageIcon, SwingConstants.CENTER));
            }
            itemPanel.add(costs, higConst.rc(1, 3));
            if (isSelected) {
                itemPanel.setOpaque(false);
            }
            return itemPanel;
        }
    }
}

