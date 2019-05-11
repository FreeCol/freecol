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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.plaf.PanelUI;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.client.gui.plaf.FreeColSelectedPanelUI;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FreeColSpecObjectType;
import net.sf.freecol.common.model.Limit;
import net.sf.freecol.common.model.Named;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.GameOptions;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * The panel used to display a colony build queue.
 * <p>
 * Panel Layout:
 * <p style="display: block; font-family: monospace; white-space: pre; margin: 1em 0;">
 * | ----------------------------------------------------- |
 * |                         header                        |
 * | --------------- | ----------------- | --------------- |
 * |     bqpUnits    |   bpqBuildQueue   |  bqpBuildings   |
 * | --------------- | ----------------- | --------------- |
 * |     unitList    | constructionPanel |  buildingList   |
 * |                 | ----------------- |                 |
 * |                 |   buildQueueList  |                 |
 * |                 |                   |                 |
 * | ----------------------------------------------------- |
 * | [buyBuildable] []compactBox []showAllBox   [okButton] |
 * | ----------------------------------------------------- |
 */
public class BuildQueuePanel extends FreeColPanel implements ItemListener {

    private static final Logger logger = Logger.getLogger(BuildQueuePanel
            .class.getName());

    private static final DataFlavor BUILD_LIST_FLAVOR
        = new DataFlavor(List.class, "IndexedBuildableFlavor");

    // This private constant can be established here, rather than in {@code FreeColPanel}
    // with the other constants so that the resource is only initialized when this class
    // is called.
    /* Constant used by #actionPerformed() */
    private static final String FAIL = "FAIL";

    /**
     * This class represents a buildable, that is dragged/dropped
     * accompanied by its index in the source list where it is dragged from.
     */
    private static class IndexedBuildable {
        private final BuildableType buildable;
        private final int index;

        IndexedBuildable(BuildableType buildable, int index) {
            this.buildable = buildable;
            this.index = index;
        }

        BuildableType getBuildable() {
            return buildable;
        }

        int getIndex() {
            return index;
        }
    }

    /**
     * This class implements a transfer handler able to transfer
     * {@code BuildQueueItem}s between the build queue list, the
     * unit list and the building list.
     */
    private class BuildQueueTransferHandler extends TransferHandler {

        /**
         * This class implements the {@code Transferable} interface.
         */
        public class BuildablesTransferable implements Transferable {

            private final List<IndexedBuildable> indexedBuildables;

            private final DataFlavor[] supportedFlavors = {
                BUILD_LIST_FLAVOR
            };


            /**
             * Default constructor.
             *
             * @param indexedBuildables The build queue to transfer.
             */
            public BuildablesTransferable(List<IndexedBuildable> indexedBuildables) {
                this.indexedBuildables = indexedBuildables;
            }


            /**
             * Get the build queue from the {@code Transferable}.
             *
             * @return The build queue.
             */
            public List<IndexedBuildable> getBuildables() {
                return this.indexedBuildables;
            }


            // Interface Transferable

            /**
             * {@inheritDoc}
             */
            @Override
            public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException {
                if (isDataFlavorSupported(flavor)) return this.indexedBuildables;
                throw new UnsupportedFlavorException(flavor);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                // findbugs warns against returning supportedFlavors directly
                // as it exposes internal representation
                return Arrays.copyOf(supportedFlavors, supportedFlavors.length);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return any(supportedFlavors, matchKeyEquals(flavor));
            }
        }

        private JList<? extends BuildableType> source = null;

        
        // Override TransferHandler

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean importData(JComponent comp, Transferable data) {
            if (!canImport(comp, data.getTransferDataFlavors())) return false;

            // What actual panel component was chosen?
            JList<? extends BuildableType> target = BuildQueuePanel.this.convertJComp(comp);
            if (target == null) {
                logger.warning("Build queue import failed to: " + comp);
                return false;
            }

            // Grab the transfer object and insist it is a list of something.
            Object transferData;
            try {
                transferData = data.getTransferData(BUILD_LIST_FLAVOR);
            } catch (UnsupportedFlavorException | IOException e) {
                logger.log(Level.WARNING, "BuildQueue import", e);
                return false;
            }
            if (!(transferData instanceof List<?>)) return false;
            List<?> transferList = (List<?>)transferData;
            
            // Collect the transferred buildables.
            final JList<BuildableType> bql
                = BuildQueuePanel.this.buildQueueList;
            List<IndexedBuildable> queue = new ArrayList<>(transferList.size());
            for (Object object : transferList) {
                // Fail if:
                // - dropping a non-Buildable
                // - dropping a unit in the Building list
                // - dropping a building in the Unit list
                if (!(object instanceof IndexedBuildable)
                            || (((IndexedBuildable)object).getBuildable() instanceof UnitType
                            && target == BuildQueuePanel.this.buildingList)
                            || (((IndexedBuildable)object).getBuildable() instanceof BuildingType
                            && target == BuildQueuePanel.this.unitList))
                    return false;
                queue.add((IndexedBuildable)object);
            }

            // Handle the cases where the BQL is not the target.
            boolean isOrderingQueue = false;
            if (this.source == bql) {
                if (target != bql) {
                    // Dragging out of build queue => just remove the item.
                    // updateAllLists takes care of the rest.
                    DefaultListModel sourceModel
                        = (DefaultListModel)source.getModel();
                    for (Object obj : queue) {
                        sourceModel.removeElement(obj);
                    }
                    return true;
                }
                // OK, we are reordering the build queue.
                isOrderingQueue = true;
            } else {
                // Can only drag from the Building and Unit lists to
                // the build queue, so require that to be the target.
                if (target != bql && target.getParent() != bql) return false;
            }

            // Now that the BQL is the target, grab its model (fully
            // type qualified now).
            DefaultListModel<BuildableType> targetModel
                = (DefaultListModel<BuildableType>)bql.getModel();

            // This is where the dragged target was dropped.
            int preferredIndex = target.getSelectedIndex();
            int maxIndex = targetModel.size();
            final int prevPos = queue.get(0).getIndex();
            if (isOrderingQueue) {
                // Suppress dropping the selection onto itself.
                if (preferredIndex != -1 && prevPos == preferredIndex) {
                    //indices = null;
                    return false;
                }

                // Insist drop is within bounds.
                int maximumIndex = getMaximumIndex(queue.get(0).getBuildable());
                if (preferredIndex > maximumIndex) {
                    //indices = null;
                    return false;
                }

                // Remove all transferring elements from the build queue.
                for (IndexedBuildable ib : queue) {
                    targetModel.removeElementAt(ib.getIndex());
                }
            }

            // Set index to add to the last position, if not set or
            // out-of-bounds
            if (preferredIndex < 0 || preferredIndex > maxIndex) {
                preferredIndex = maxIndex;
            }

            for (int index = 0; index < queue.size(); index++) {
                BuildableType toBuild = queue.get(index).getBuildable();
                int minimumIndex = getMinimumIndex(toBuild);

                // minimumIndex == targetModel.size() means it has to
                // go at the end.
                if (minimumIndex < targetModel.size()) {
                    int maximumIndex = getMaximumIndex(toBuild);

                    // minimumIndex == maximumIndex means there is
                    // only one place it can go.
                    if (minimumIndex != maximumIndex) {
                        if (minimumIndex < preferredIndex + index) {
                            minimumIndex = preferredIndex + index;
                        }
                        minimumIndex = Math.min(Math.min(minimumIndex,
                                targetModel.size()), maximumIndex);
                    }
                }
                targetModel.add(minimumIndex, toBuild);
            }

            // update selected index to new position
            if (isOrderingQueue) {
                BuildQueuePanel.this.buildQueueList
                    .setSelectedIndex(preferredIndex);
            }
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void exportDone(JComponent source, Transferable data, 
                                  int action) {
            //this.indices = null;
            updateAllLists();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean canImport(JComponent comp, DataFlavor[] flavors) {
            return flavors != null
                && any(flavors, matchKeyEquals(BUILD_LIST_FLAVOR));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Transferable createTransferable(JComponent comp) {
            this.source = BuildQueuePanel.this.convertJComp(comp);
            if (this.source == null) return null;
            int[] indicesArray = this.source.getSelectedIndices();
            List<? extends BuildableType> buildableTypes = this.source.getSelectedValuesList();
            List<IndexedBuildable> indexedBuildables = new ArrayList<>(indicesArray.length);
            int i = 0;
            for (int index : indicesArray) {
                BuildableType bt = buildableTypes.get(i++);
                indexedBuildables.add(new IndexedBuildable(bt, index));
            }
            return new BuildablesTransferable(indexedBuildables);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getSourceActions(JComponent comp) {
            return (comp == BuildQueuePanel.this.unitList) ? COPY : MOVE;
        }
    }

    private class BuildQueueMouseAdapter extends MouseAdapter {

        private boolean add = true;
        
        private boolean enabled = false;


        public BuildQueueMouseAdapter(boolean add) {
            this.add = add;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void mousePressed(MouseEvent e) {
            if (!this.enabled && e.getClickCount() == 1 && !e.isConsumed()) {
                this.enabled = true;
            }
            if (!this.enabled) return;

            Object source = e.getSource();
            JList<? extends BuildableType> jlist
                = (source instanceof JComponent)
                ? BuildQueuePanel.this.convertJComp((JComponent)source)
                : null;
            if (jlist == null) return;

            if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
                int index = jlist.locationToIndex(e.getPoint());
                BuildableType bt = jlist.getModel().getElementAt(index);
                getGUI().showColopediaPanel(bt.getId());
            } else if (e.getClickCount() > 1 && !e.isConsumed()) {
                JList<BuildableType> bql = BuildQueuePanel.this.buildQueueList;
                DefaultListModel<BuildableType> model
                    = (DefaultListModel<BuildableType>)bql.getModel();
                if (jlist.getSelectedIndex() < 0) {
                    jlist.setSelectedIndex(jlist.locationToIndex(e.getPoint()));
                }
                for (int index : jlist.getSelectedIndices()) {
                    if (this.add) {
                        model.addElement(jlist.getModel().getElementAt(index));
                    } else {
                        model.removeElementAt(index);
                    }
                }
                updateAllLists();
            }
        }
    }

    /** The cell renderer to use for the build queue. */
    private class DefaultBuildQueueCellRenderer
        implements ListCellRenderer<BuildableType> {

        private final Dimension buildingDimension = new Dimension(-1, 48);


        public DefaultBuildQueueCellRenderer() {}


        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(JList<? extends BuildableType> list,
                                                      BuildableType value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            final ImageLibrary lib = getImageLibrary();
            JPanel panel = new MigPanel(new MigLayout());
            panel.setOpaque(false);
            if (isSelected) {
                panel.setUI((PanelUI)FreeColSelectedPanelUI.createUI(panel));
            }

            JLabel imageLabel = new JLabel(new ImageIcon(ImageLibrary
                    .getBuildableTypeImage(value, buildingDimension)));
            JLabel nameLabel = new JLabel(Messages.getName(value));
            String reason = lockReasons.get(value);
            panel.add(imageLabel, "span 1 2");
            if (reason == null) {
                panel.add(nameLabel, "wrap");
            } else {
                panel.add(nameLabel, "split 2");
                panel.add(lib.getLockLabel(), "wrap");
                panel.setToolTipText(reason);
            }

            List<AbstractGoods> required = value.getRequiredGoodsList();
            int size = required.size();
            for (int i = 0; i < size; i++) {
                AbstractGoods goods = required.get(i);
                ImageIcon icon = new ImageIcon(lib.getSmallGoodsTypeImage(goods.getType()));
                JLabel goodsLabel = new JLabel(Integer.toString(goods.getAmount()), icon, SwingConstants.CENTER);
                if (i == 0 && size > 1) {
                    panel.add(goodsLabel, "split " + size);
                } else {
                    panel.add(goodsLabel);
                }
            }
            return panel;
        }
    }


    private static final String BUY = "buy";
    private static final int UNABLE_TO_BUILD = -1;

    /** Default setting for the compact box. */
    private static boolean defaultCompact = false;

    /** Default setting for the showAll box. */
    private static boolean defaultShowAll = false;

    /** The enclosing colony. */
    private final Colony colony;

    /** A feature container for potential features from queued buildables. */
    private final FeatureContainer featureContainer;

    /** A transfer handler for the build queue lists. */
    private final BuildQueueTransferHandler buildQueueHandler
        = new BuildQueueTransferHandler();

    /** The list of buildable unit types. */
    private final JList<UnitType> unitList;

    /** The panel showing the current buildable. */
    private final ConstructionPanel constructionPanel;

    /** The build queue. */
    private final JList<BuildableType> buildQueueList;

    /** The list of buildable building types. */
    private final JList<BuildingType> buildingList;

    /** A button to buy the current buildable. */
    private final JButton buyBuildable;

    /** The check box to enable compact mode. */
    private final JCheckBox compactBox;

    /** The check box to enable showing all buildables. */
    private final JCheckBox showAllBox;

    private final Map<BuildableType, String> lockReasons = new HashMap<>();


    /**
     * Create a new build queue panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param colony The enclosing {@code Colony}.
     */
    public BuildQueuePanel(FreeColClient freeColClient, Colony colony) {
        super(freeColClient, null,
              new MigLayout("wrap 3", "[260:][390:, fill][260:]",
                            "[][][300:400:][]"));

        this.colony = colony;
        this.featureContainer = new FeatureContainer();

        DefaultListModel<BuildableType> current
            = new DefaultListModel<>();
        for (BuildableType type : this.colony.getBuildQueue()) {
            current.addElement(type);
            this.featureContainer.addFeatures(type);
        }

        BuildQueueMouseAdapter adapter = new BuildQueueMouseAdapter(true);
        Action addAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    JList<BuildableType> bql
                        = BuildQueuePanel.this.buildQueueList;
                    DefaultListModel<BuildableType> model
                        = (DefaultListModel<BuildableType>)bql.getModel();
                    JList<? extends BuildableType> btl
                        = (ae.getSource() == BuildQueuePanel.this.unitList)
                        ? BuildQueuePanel.this.unitList
                        : (ae.getSource() == BuildQueuePanel.this.buildingList)
                        ? BuildQueuePanel.this.buildingList
                        : null;
                    if (btl != null) {
                        for (BuildableType bt : btl.getSelectedValuesList()) {
                            model.addElement(bt);
                        }
                    }
                    updateAllLists();
                }
            };

        // Create Font choice
        Font fontSubHead = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
                FontLibrary.FontSize.SMALLER, Font.BOLD,
                getImageLibrary().getScaleFactor());
        
        // Create the components
        JLabel header = Utility.localizedHeaderLabel(
            "buildQueuePanel.buildQueue",
            SwingConstants.LEADING, FontLibrary.FontSize.BIG);
        
        // JLabel SubHeads
        JLabel bqpUnits = Utility.localizedLabel("buildQueuePanel.units");
        bqpUnits.setFont(fontSubHead);
        JLabel bpqBuildQueue = Utility.localizedLabel("buildQueuePanel.buildQueue");
        bpqBuildQueue.setFont(fontSubHead);
        bpqBuildQueue.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel bqpBuildings = Utility.localizedLabel("buildQueuePanel.buildings");
        bqpBuildings.setFont(fontSubHead);

        DefaultListModel<UnitType> units = new DefaultListModel<>();
        this.unitList = new JList<>(units);
        this.unitList.setTransferHandler(buildQueueHandler);
        this.unitList.setSelectionMode(ListSelectionModel
                .MULTIPLE_INTERVAL_SELECTION);
        this.unitList.setDragEnabled(true);
        this.unitList.addMouseListener(adapter);
        this.unitList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), 
                "add");
        this.unitList.getActionMap().put("add", addAction);

        this.constructionPanel
            = new ConstructionPanel(freeColClient, this.colony, false);
        this.constructionPanel.setDefaultLabel(StringTemplate
            .template("buildQueuePanel.currentlyBuilding")
            .add("%buildable%", "nothing"));

        this.buildQueueList = new JList<>(current);
        this.buildQueueList.setTransferHandler(buildQueueHandler);
        this.buildQueueList.setSelectionMode(ListSelectionModel
                .SINGLE_SELECTION);
        this.buildQueueList.setDragEnabled(true);
        this.buildQueueList.addMouseListener(new BuildQueueMouseAdapter(false));
        this.buildQueueList.getInputMap()
            .put(KeyStroke.getKeyStroke("DELETE"), "delete");
        this.buildQueueList.getActionMap().put("delete",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    JList<BuildableType> bql = BuildQueuePanel.
                            this.buildQueueList;
                    for (BuildableType bt : bql.getSelectedValuesList()) {
                        removeBuildable(bt);
                    }
                    updateAllLists();
                }
            });

        DefaultListModel<BuildingType> buildings
            = new DefaultListModel<>();
        this.buildingList = new JList<>(buildings);
        this.buildingList.setTransferHandler(buildQueueHandler);
        this.buildingList.setSelectionMode(ListSelectionModel.
                MULTIPLE_INTERVAL_SELECTION);
        this.buildingList.setDragEnabled(true);
        this.buildingList.addMouseListener(adapter);
        this.buildingList.getInputMap()
            .put(KeyStroke.getKeyStroke("ENTER"), "add");
        this.buildingList.getActionMap().put("add", addAction);

        this.buyBuildable = Utility.localizedButton("none"); // placeholder
        setBuyLabel(null);
        this.buyBuildable.setActionCommand(BUY);
        this.buyBuildable.addActionListener(this);

        this.compactBox
            = new JCheckBox(Messages.message("buildQueuePanel.compactView"));
        this.compactBox.addItemListener(this);
        this.compactBox.setSelected(defaultCompact);

        this.showAllBox
            = new JCheckBox(Messages.message("buildQueuePanel.showAll"));
        this.showAllBox.addItemListener(this);
        this.showAllBox.setSelected(defaultShowAll);

        updateDetailView(); // sets cell renderer
        updateAllLists();

        // Add all the components
        add(header, "span 3, align center, wrap 40");
        add(bqpUnits, "align center");
        add(bpqBuildQueue, "align center");
        add(bqpBuildings, "align center");
        add(new JScrollPane(this.unitList), "grow");
        add(this.constructionPanel, "split 2, flowy");
        add(new JScrollPane(this.buildQueueList), "grow");
        add(new JScrollPane(this.buildingList), "grow, wrap 20");
        add(this.buyBuildable, "span, split 4");
        add(this.compactBox);
        add(this.showAllBox);
        add(okButton, "tag ok");

        setSize(getPreferredSize());
    }


    /**
     * Get the colony for this build queue.
     *
     * @return The {@code Colony}.
     */
    public Colony getColony() {
        return this.colony;
    }

    /**
     * Convert a component to an actual part of this panel,
     * recovering its list type.
     *
     * @param comp The {@code JComponent} to convert.
     * @return The actual panel component {@code JList}, or
     *     null on error.
     */
    private JList<? extends BuildableType> convertJComp(JComponent comp) {
        return (comp == this.unitList) ? this.unitList
            : (comp == this.buildQueueList) ? this.buildQueueList
            : (comp == this.buildingList) ? this.buildingList
            : null;
    }

    private void removeBuildable(Object type) {
        DefaultListModel<BuildableType> model
            = (DefaultListModel<BuildableType>)this.buildQueueList.getModel();
        model.removeElement(type);
    }

    private boolean checkAbilities(BuildableType bt, List<String> lockReason) {
        final Specification spec = getSpecification();
        final int oldSize = lockReason.size();
        forEachMapEntry(bt.getRequiredAbilities(), e -> {
                final String id = e.getKey();
                final boolean value = e.getValue();
                if (this.featureContainer.hasAbility(id, null, null) != value
                    && this.colony.hasAbility(id) != value) {
                    FreeColSpecObjectType source
                        = first(spec.getTypesProviding(id, value));
                    lockReason.add((source != null)
                        ? Messages.getName(source)
                        : Messages.getName(bt));
                }
            });
        return lockReason.size() == oldSize;
    }

    /**
     * Update the list of available buildings to build
     *
     * This method will verify whether a building can be built by
     *      checking against the following criteria:
     *       * Does the Colony meet the population limit to build?
     *       * Does the new building require a special circumstance,
     *              such as a prerequisite unit or building?
     */
    private void updateBuildingList() {
        final Specification spec = getSpecification();
        final DefaultListModel<BuildableType> current
            = (DefaultListModel<BuildableType>)this.buildQueueList.getModel();
        final DefaultListModel<BuildingType> buildings
            = (DefaultListModel<BuildingType>)this.buildingList.getModel();
        buildings.clear();
        Set<BuildableType> unbuildableTypes = new HashSet<>();

        // For each building type, find out if it is buildable, and
        // reasons to not build it (and perhaps display a lock icon).
        for (BuildingType bt : spec.getBuildingTypeList()) {
            if (unbuildableTypes.contains(bt)) continue;

            // Impossible upgrade path
            if (bt.getUpgradesFrom() != null
                && unbuildableTypes.contains(bt.getUpgradesFrom())) {
                unbuildableTypes.add(bt);
                continue;
            }

            // Ignore pre-built buildings
            if (!bt.needsGoodsToBuild()) continue;
            
            // Only one building of any kind
            if (current.contains(bt) || hasBuildingType(bt)) continue;
            
            List<String> reasons = new ArrayList<>(8);

            // Coastal limit
            if (bt.hasAbility(Ability.COASTAL_ONLY)
                && !this.colony.getTile().isCoastland()) {
                reasons.add(Messages.message(StringTemplate
                        .template("buildQueuePanel.coastalOnly")));
            }

            // Population limit
            if (bt.getRequiredPopulation() > this.colony.getUnitCount()) {
                reasons.add(Messages.message(StringTemplate
                        .template("buildQueuePanel.populationTooSmall")
                        .addAmount("%number%", bt.getRequiredPopulation())));
            }

            // Spec limits
            for (Limit limit : transform(bt.getLimits(),
                                         l -> !l.evaluate(this.colony))) {
                reasons.add(Messages.getDescription(limit));
            }

            // Missing ability
            if (!checkAbilities(bt, reasons)) unbuildableTypes.add(bt);

            // Upgrade path is blocked
            Building colonyBuilding = this.colony.getBuilding(bt);
            BuildingType up = bt.getUpgradesFrom();
            if (up != null && !current.contains(up)
                && (colonyBuilding == null || colonyBuilding.getType() != up)) {
                reasons.add(Messages.getName(up));
            }

            lockReasons.put(bt, (reasons.isEmpty()) ? null
                : Messages.message(StringTemplate
                    .template("buildQueuePanel.requires")
                    .addName("%string%", join("/", reasons))));
            if (reasons.isEmpty()
                || showAllBox.isSelected()) buildings.addElement(bt);
        }
    }

    /**
     * Update the list of available units (ships, wagon, artillery) to build
     *
     * This method will verify whether a unit can be built by
     *      checking against the following criteria:
     *       * Does the Colony meet the population limit to build?
     *       * Does the new building require a special circumstance,
     *              such as a prerequisite unit or building?
     */
    private void updateUnitList() {
        final Specification spec = getSpecification();
        final Turn turn = getGame().getTurn();
        DefaultListModel<UnitType> units
            = (DefaultListModel<UnitType>)this.unitList.getModel();
        units.clear();
        Set<BuildableType> unbuildableTypes = new HashSet<>();
        
        // For each unit type, find out if it is buildable, and
        // reasons to not build it (and perhaps display a lock icon).
        for (UnitType ut : spec.getBuildableUnitTypes()) {
            if (unbuildableTypes.contains(ut)) continue;

            List<String> reasons = new ArrayList<>(8);

            // Population limit
            if (ut.getRequiredPopulation() > this.colony.getUnitCount()) {
                reasons.add(Messages.message(StringTemplate
                        .template("buildQueuePanel.populationTooSmall")
                        .addAmount("%number%", ut.getRequiredPopulation())));
            }

            // Spec limits
            for (Limit limit : transform(ut.getLimits(),
                                         l -> !l.evaluate(this.colony))) {
                reasons.add(Messages.getDescription(limit));
            }

            // Missing ability
            if (!checkAbilities(ut, reasons)) unbuildableTypes.add(ut);

            // Missing unit build ability?
            if (!this.featureContainer.hasAbility(Ability.BUILD, ut, null)
                && !this.colony.hasAbility(Ability.BUILD, ut, turn)) {
                Ability buildAbility = find(spec.getAbilities(Ability.BUILD),
                    a -> (a.appliesTo(ut)
                        && a.getValue()
                        && a.getSource() != null
                        && !unbuildableTypes.contains(a.getSource())));
                reasons.add((buildAbility != null)
                    ? ((buildAbility.getSource() instanceof Named)
                        ? Messages.getName((Named)buildAbility.getSource())
                        : Messages.getName(buildAbility))
                    : Messages.getName(ut));
            }

            lockReasons.put(ut, (reasons.isEmpty()) ? null
                : Messages.message(StringTemplate
                    .template("buildQueuePanel.requires")
                    .addName("%string%", join("/", reasons))));
            if (reasons.isEmpty()
                || showAllBox.isSelected()) units.addElement(ut);
        }
    }

    /**
     * Update all the lists and buttons, using
     *      {@link #updateBuildingList()} and
     *      {@link #updateUnitList()}
     */
    private final void updateAllLists() {
        final DefaultListModel<BuildableType> current
            = (DefaultListModel<BuildableType>)this.buildQueueList.getModel();

        this.featureContainer.clear();
        for (Enumeration<BuildableType> e = current.elements();
             e.hasMoreElements();) {
            BuildableType type = e.nextElement();
            if (getMinimumIndex(type) >= 0) {
                featureContainer.addFeatures(type);
            } else {
                current.removeElement(type);
            }
        }
        // ATTENTION: buildings must be updated first, since units
        // might depend on the build ability of an unbuildable
        // building
        updateBuildingList();
        updateUnitList();

        // Update the buy button
        final boolean pay = getSpecification()
            .getBoolean(GameOptions.PAY_FOR_BUILDING);
        BuildableType bt = (current.getSize() <= 0) ? null
            : current.getElementAt(0);
        this.buyBuildable.setEnabled(bt != null && pay
            && this.colony.canPayToFinishBuilding(bt));
        this.setBuyLabel(bt);

        // Update the construction panel
        if (current.getSize() > 0) {
            this.constructionPanel.update(current.getElementAt(0));
        } else if (current.getSize() == 0) {
            this.constructionPanel.update(); // generates Building: Nothing
        }
    }

    private void setBuyLabel(BuildableType buildable) {
        this.buyBuildable.setText(Messages.message((buildable == null)
                ? StringTemplate.template("buildQueuePanel.buyBuilding")
                    .addStringTemplate("%buildable%",
                        StringTemplate.key("nothing"))
                : StringTemplate.template("buildQueuePanel.buyBuilding")
                    .addNamed("%buildable%", buildable)));
    }

    /**
     * Checks whether a specified {@code BuildingType} exists
     * within a colony
     *
     * @param buildingType The {@code BuildingType} to check
     * @return boolean
     */
    private boolean hasBuildingType(BuildingType buildingType) {
        while (true) {
            if (this.colony.getBuilding(buildingType) == null) {
                return false;
            } else if (colony.getBuilding(buildingType).getType() == buildingType) {
                return true;
            } else if (buildingType.getUpgradesTo() != null) {
                buildingType = buildingType.getUpgradesTo();
            } else {
                return false;
            }
        }
    }

    private List<BuildableType> getBuildableTypes(JList<? extends BuildableType> list) {
        if (list == null) return Collections.<BuildableType>emptyList();
        ListModel<? extends BuildableType> model = list.getModel();
        List<BuildableType> result = new ArrayList<>(model.getSize());
        for (int index = 0; index < model.getSize(); index++) {
            result.add(model.getElementAt(index));
        }
        return result;
    }

    private int getMinimumIndex(BuildableType buildableType) {
        return buildableType.getMinimumIndex(this.getColony(), buildQueueList, UNABLE_TO_BUILD);
    }

    private int getMaximumIndex(BuildableType buildableType) {
        return buildableType.getMaximumIndex(this.getColony(),
                buildQueueList, UNABLE_TO_BUILD);
    }

    /**
     * Set the correct cell renderer in the buildables lists.
     */
    private void updateDetailView() {
        ListCellRenderer<BuildableType> cellRenderer
            = (this.compactBox.isSelected())
            ? new FreeColComboBoxRenderer<BuildableType>()
            : new DefaultBuildQueueCellRenderer();
        this.buildQueueList.setCellRenderer(cellRenderer);
        this.buildingList.setCellRenderer(cellRenderer);
        this.unitList.setCellRenderer(cellRenderer);
    }

    private static void updateCompact(boolean selected) {
        defaultCompact = selected;
    }

    private static void updateLists(boolean selected) {
        defaultShowAll = selected;
    }

    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        if (this.colony.getOwner() == getMyPlayer()) {
            String command = ae.getActionCommand();
            List<BuildableType> buildables = getBuildableTypes(this
                    .buildQueueList);
            BuildableType bt;
            while ((bt = first(buildables)) != null
                && lockReasons.get(bt) != null) {
                getGUI().showInformationMessage(bt,
                    this.colony.getUnbuildableMessage(bt));
                command = FAIL;
                removeBuildable(buildables.remove(0));
            }
            igc().setBuildQueue(this.colony, buildables);
            if (null != command) switch (command) {
                case FAIL:
                    // Let the user reconsider.
                    updateAllLists();
                    return;
                case OK:
                    break;
                case BUY:
                    igc().payForBuilding(this.colony);
                    break;
                default:
                    super.actionPerformed(ae);
                    break;
            }
        }
        getGUI().removeComponent(this);
    }


    /**
     * Override {@link ItemListener} for this panel's use.
     *      This function evaluates whether a the user has
     *      clicked the {@link #compactBox} or the
     *      {@link #showAllBox} has been checked.
     *
     * {@inheritDoc}
     */
    @Override
    public void itemStateChanged(ItemEvent event) {
        if (event.getSource() == this.compactBox) {
            updateDetailView();
            updateCompact(this.compactBox.isSelected());
        } else if (event.getSource() == this.showAllBox) {
            updateAllLists();
            updateLists(this.showAllBox.isSelected());
        }
    }
}
