/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Limit;
import net.sf.freecol.common.model.Named;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.UnitType;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * The panel used to display a colony build queue.
 */
public class BuildQueuePanel extends FreeColPanel implements ItemListener {

    private static final Logger logger = Logger.getLogger(BuildQueuePanel
            .class.getName());

    private static final DataFlavor BUILD_LIST_FLAVOR
        = new DataFlavor(List.class, "BuildListFlavor");

    /**
     * This class implements a transfer handler able to transfer
     * <code>BuildQueueItem</code>s between the build queue list, the
     * unit list and the building list.
     */
    private class BuildQueueTransferHandler extends TransferHandler {

        /**
         * This class implements the <code>Transferable</code> interface.
         */
        public class BuildablesTransferable implements Transferable {

            private final List<? extends BuildableType> buildables;

            private final DataFlavor[] supportedFlavors = {
                BUILD_LIST_FLAVOR
            };


            /**
             * Default constructor.
             *
             * @param buildables The build queue to transfer.
             */
            public BuildablesTransferable(List<? extends BuildableType> buildables) {
                this.buildables = buildables;
            }


            /**
             * Get the build queue from the <code>Transferable</code>.
             *
             * @return The build queue.
             */
            public List<? extends BuildableType> getBuildables() {
                return this.buildables;
            }


            // Interface Transferable

            /**
             * {@inheritDoc}
             */
            @Override
            public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException {
                if (isDataFlavorSupported(flavor)) return this.buildables;
                throw new UnsupportedFlavorException(flavor);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return supportedFlavors;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return any(supportedFlavors, f -> f.equals(flavor));
            }
        }

        private JList<? extends BuildableType> source = null;
        //private int[] indices = null;
        private int numberOfItems = 0; // number of items to be added

        
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

            // Collect the transferred buildables.
            final JList<BuildableType> bql
                = BuildQueuePanel.this.buildQueueList;
            List<BuildableType> queue = new ArrayList<>();
            for (Object object : (List<?>)transferData) {
                // Fail if:
                // - dropping a non-Buildable
                // - dropping a unit in the Building list
                // - dropping a building in the Unit list
                // - no minimum index for the buildable is found
                if (!(object instanceof BuildableType)
                    || (object instanceof UnitType
                        && target == BuildQueuePanel.this.buildingList)
                    || (object instanceof BuildingType
                        && target == BuildQueuePanel.this.unitList)
                    || getMinimumIndex((BuildableType)object) < 0)
                    return false;
                queue.add((BuildableType)object);
            }

            // Handle the cases where the BQL is not the target.
            boolean isOrderingQueue = false;
            if (this.source == bql) {
                if (target != bql) {
                    // Dragging out of build queue => just remove the element.
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
            int prevPos = -1;
            if (isOrderingQueue) {
                // Find previous position
                for (int i = 0; i < targetModel.getSize(); i++) {
                    if (targetModel.getElementAt(i) == queue.get(0)) {
                        prevPos = i;
                        break;
                    }
                }

                // Suppress dropping the selection onto itself.
                if (preferredIndex != -1 && prevPos == preferredIndex) {
                    //indices = null;
                    return false;
                }

                // Insist drop is within bounds.
                int maximumIndex = getMaximumIndex(queue.get(0));
                if (preferredIndex > maximumIndex) {
                    //indices = null;
                    return false;
                }

                this.numberOfItems = queue.size();

                // Remove all transferring elements from the build queue.
                for (BuildableType bt : queue) {
                    targetModel.removeElement(bt);
                }
            }

            // Set index to add to the last position, if not set or
            // out-of-bounds
            if (preferredIndex < 0 || preferredIndex > maxIndex) {
                preferredIndex = maxIndex;
            }

            for (int index = 0; index < queue.size(); index++) {
                BuildableType toBuild = queue.get(index);
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
                        if (minimumIndex > targetModel.size()) {
                            minimumIndex = targetModel.size();
                        }
                        if (minimumIndex > maximumIndex) {
                            minimumIndex = maximumIndex;
                        }
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
            this.numberOfItems = 0;
            updateAllLists();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean canImport(JComponent comp, DataFlavor[] flavors) {
            return flavors != null
                && any(flavors, f -> f.equals(BUILD_LIST_FLAVOR));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Transferable createTransferable(JComponent comp) {
            
            this.source = BuildQueuePanel.this.convertJComp(comp);
            if (this.source == null) return null;
            //this.indices = this.source.getSelectedIndices();
            return new BuildablesTransferable(this.source.getSelectedValuesList());
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
                for (BuildableType bt : jlist.getSelectedValuesList()) {
                    if (this.add) {
                        model.addElement(bt);
                    } else {
                        model.removeElement(bt);
                    }
                }
                updateAllLists();
            }
        }
    }

    /** The cell renderer to use for the build queue. */
    private class DefaultBuildQueueCellRenderer
        implements ListCellRenderer<BuildableType> {

        private final JPanel itemPanel = new JPanel();
        private final JPanel selectedPanel = new JPanel();
        private final JLabel imageLabel = new JLabel(new ImageIcon());
        private final JLabel nameLabel = new JLabel();

        private final JLabel lockLabel = new JLabel(new ImageIcon(
            ImageLibrary.getMiscImage(ImageLibrary.ICON_LOCK, 0.5f)));

        private final Dimension buildingDimension = new Dimension(-1, 48);


        public DefaultBuildQueueCellRenderer() {
            itemPanel.setOpaque(false);
            itemPanel.setLayout(new MigLayout());
            selectedPanel.setOpaque(false);
            selectedPanel.setLayout(new MigLayout());
            selectedPanel.setUI((PanelUI)FreeColSelectedPanelUI.createUI(selectedPanel));
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(JList<? extends BuildableType> list,
                        BuildableType value,
                        int index,
                        boolean isSelected,
                        boolean cellHasFocus) {
            JPanel panel = (isSelected) ? selectedPanel : itemPanel;
            panel.removeAll();

            ((ImageIcon)imageLabel.getIcon()).setImage(ImageLibrary.getBuildableImage(value, buildingDimension));

            nameLabel.setText(Messages.getName(value));
            panel.setToolTipText(lockReasons.get(value));
            panel.add(imageLabel, "span 1 2");
            if (lockReasons.get(value) == null) {
                panel.add(nameLabel, "wrap");
            } else {
                panel.add(nameLabel, "split 2");
                panel.add(lockLabel, "wrap");
            }

            ImageLibrary lib = getImageLibrary();
            List<AbstractGoods> required = value.getRequiredGoods();
            int size = required.size();
            for (int i = 0; i < size; i++) {
                AbstractGoods goods = required.get(i);
                ImageIcon icon = new ImageIcon(lib.getSmallIconImage(goods.getType()));
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
    private final Set<BuildableType> unbuildableTypes
        = new HashSet<>();


    /**
     * Create a new build queue panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param colony The enclosing <code>Colony</code>.
     */
    public BuildQueuePanel(FreeColClient freeColClient, Colony colony) {
        super(freeColClient, new MigLayout("wrap 3", 
                "[260:][390:, fill][260:]", "[][][300:400:][]"));

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
     * @return The <code>Colony</code>.
     */
    public Colony getColony() {
        return this.colony;
    }

    /**
     * Convert a component to an actual part of this panel,
     * recovering its list type.
     *
     * @param comp The <code>JComponent</code> to convert.
     * @return The actual panel component <code>JList</code>, or
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

    private void updateUnitList() {
        final Specification spec = getSpecification();
        final Turn turn = getGame().getTurn();
        StringTemplate tmpl;
        DefaultListModel<UnitType> units
            = (DefaultListModel<UnitType>)this.unitList.getModel();
        units.clear();
        loop: for (UnitType unitType : spec.getBuildableUnitTypes()) {
            // compare colony.getNoBuildReason()
            List<String> lockReason = new ArrayList<>();
            if (unbuildableTypes.contains(unitType)) {
                continue;
            }

            if (unitType.getRequiredPopulation() > this.colony.getUnitCount()) {
                tmpl = StringTemplate.template("buildQueuePanel.populationTooSmall")
                    .addAmount("%number%", unitType.getRequiredPopulation());
                lockReason.add(Messages.message(tmpl));
            }

            if (unitType.getLimits() != null) {
                for (Limit limit : unitType.getLimits()) {
                    if (!limit.evaluate(this.colony)) {
                        lockReason.add(Messages.getDescription(limit));
                    }
                }
            }

            if (!(this.colony.hasAbility(Ability.BUILD, unitType, turn)
                    || this.featureContainer.hasAbility(Ability.BUILD,
                        unitType, null))) {
                boolean builderFound = false;
                for (Ability ability : spec.getAbilities(Ability.BUILD)) {
                    FreeColObject source = ability.getSource();
                    if (ability.appliesTo(unitType)
                        && ability.getValue()
                        && source != null
                        && !unbuildableTypes.contains(source)) {
                        builderFound = true;
                        if (source instanceof Named) {
                            lockReason.add(Messages.getName((Named)source));
                        }
                        break;
                    }
                }
                if (!builderFound) {
                    unbuildableTypes.add(unitType);
                    continue;
                }
            }

            for (Entry<String, Boolean> entry
                     : unitType.getRequiredAbilities().entrySet()) {
                if (this.colony.hasAbility(entry.getKey()) != entry.getValue()
                    && this.featureContainer.hasAbility(entry.getKey(),
                            null, null)
                    != entry.getValue()) {
                    List<FreeColGameObjectType> sources
                        = spec.getTypesProviding(entry.getKey(),
                                                 entry.getValue());
                    if (sources.isEmpty()) {
                        // no type provides the required ability
                        unbuildableTypes.add(unitType);
                        continue loop;
                    } else {
                        lockReason.add(Messages.getName(sources.get(0)));
                    }
                }
            }
            if (lockReason.isEmpty()) {
                lockReasons.put(unitType, null);
            } else {
                tmpl = StringTemplate.template("buildQueuePanel.requires")
                    .addName("%string%", join("/", lockReason));
                lockReasons.put(unitType, Messages.message(tmpl));
            }
            if (lockReason.isEmpty() || showAllBox.isSelected()) {
                units.addElement(unitType);
            }
        }
    }

    private void updateBuildingList() {
        final Specification spec = getSpecification();
        final DefaultListModel<BuildingType> buildings
            = (DefaultListModel<BuildingType>)this.buildingList.getModel();
        final DefaultListModel<BuildableType> current
            = (DefaultListModel<BuildableType>)this.buildQueueList.getModel();
        StringTemplate tmpl;

        buildings.clear();
        loop: for (BuildingType buildingType : spec.getBuildingTypeList()) {
            // compare colony.getNoBuildReason()
            List<String> lockReason = new ArrayList<>();
            Building colonyBuilding = this.colony.getBuilding(buildingType);
            if (current.contains(buildingType)
                || hasBuildingType(buildingType)) {
                // only one building of any kind
                continue;
            } else if (unbuildableTypes.contains(buildingType)) {
                continue;
            } else if (!buildingType.needsGoodsToBuild()) {
                // pre-built
                continue;
            } else if (unbuildableTypes.contains(buildingType
                    .getUpgradesFrom())) {
                unbuildableTypes.add(buildingType); // impossible upgrade path
                continue;
            }

            if (buildingType.hasAbility(Ability.COASTAL_ONLY)
                && !this.colony.getTile().isCoastland()) {
                tmpl = StringTemplate.template("buildQueuePanel.coastalOnly");
                lockReason.add(Messages.message(tmpl));
            }
                                                
            if (buildingType.getRequiredPopulation() > this.colony
                    .getUnitCount()) {
                tmpl = StringTemplate.template("buildQueuePanel.populationTooSmall")
                    .addAmount("%number%", buildingType.getRequiredPopulation());
                lockReason.add(Messages.message(tmpl));
            }

            for (Entry<String, Boolean> entry
                     : buildingType.getRequiredAbilities().entrySet()) {
                if (this.colony.hasAbility(entry.getKey()) != entry.getValue()
                    && this.featureContainer.hasAbility(entry.getKey(),
                            null, null)
                    != entry.getValue()) {
                    List<FreeColGameObjectType> sources = getSpecification()
                        .getTypesProviding(entry.getKey(), entry.getValue());
                    if (sources.isEmpty()) {
                        // no type provides the required ability
                        unbuildableTypes.add(buildingType);
                        continue loop;
                    } else {
                        lockReason.add(Messages.getName(sources.get(0)));
                    }
                }
            }

            if (buildingType.getLimits() != null) {
                for (Limit limit : buildingType.getLimits()) {
                    if (!limit.evaluate(this.colony)) {
                        lockReason.add(Messages.getDescription(limit));
                    }
                }
            }

            if (buildingType.getUpgradesFrom() != null
                && !current.contains(buildingType.getUpgradesFrom())) {
                if (colonyBuilding == null
                    || colonyBuilding.getType() != buildingType
                            .getUpgradesFrom()) {
                    lockReason.add(Messages.getName(buildingType
                            .getUpgradesFrom()));
                }
            }
            if (lockReason.isEmpty()) {
                lockReasons.put(buildingType, null);
            } else {
                tmpl = StringTemplate.template("buildQueuePanel.requires")
                    .addName("%string%", join("/", lockReason));
                lockReasons.put(buildingType, Messages.message(tmpl));
            }
            if (lockReason.isEmpty() || showAllBox.isSelected()) {
                buildings.addElement(buildingType);
            }
        }
    }

    private void updateAllLists() {
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

    private boolean hasBuildingType(BuildingType buildingType) {
        if (this.colony.getBuilding(buildingType) == null) {
            return false;
        } else if (colony.getBuilding(buildingType).getType() == buildingType) {
            return true;
        } else if (buildingType.getUpgradesTo() != null) {
            return hasBuildingType(buildingType.getUpgradesTo());
        } else {
            return false;
        }
    }

    private List<BuildableType> getBuildableTypes(JList<? extends BuildableType> list) {
        List<BuildableType> result = new ArrayList<>();
        if (list == null) return result;
        ListModel<? extends BuildableType> model = list.getModel();
        for (int index = 0; index < model.getSize(); index++) {
            result.add(model.getElementAt(index));
        }
        return result;
    }

    private int getMinimumIndex(BuildableType buildableType) {
        ListModel<BuildableType> buildQueue = this.buildQueueList.getModel();
        if (buildableType instanceof UnitType) {
            if (this.colony.canBuild(buildableType)) return 0;
            for (int index = 0; index < buildQueue.getSize(); index++) {
                if (buildQueue.getElementAt(index).hasAbility(Ability.BUILD,
                        buildableType)) return index + 1;
            }
        } else if (buildableType instanceof BuildingType) {
            BuildingType upgradesFrom = ((BuildingType)buildableType)
                    .getUpgradesFrom();
            if (upgradesFrom == null) return 0;
            Building building = this.colony
                    .getBuilding((BuildingType)buildableType);
            BuildingType buildingType = (building == null) ? null
                : building.getType();
            if (buildingType == upgradesFrom) return 0;
            for (int index = 0; index < buildQueue.getSize(); index++) {
                if (upgradesFrom.equals(buildQueue.getElementAt(index))) {
                    return index + 1;
                }
            }
        }
        return UNABLE_TO_BUILD;
    }

    private int getMaximumIndex(BuildableType buildableType) {
        ListModel<BuildableType> buildQueue = this.buildQueueList.getModel();
        final int buildQueueLastPos = buildQueue.getSize();

        boolean canBuild = false;
        if (this.colony.canBuild(buildableType)) {
            canBuild = true;
        }

        if (buildableType instanceof UnitType) {
            // does not depend on anything, nothing depends on it
            // can be built at any time
            if (canBuild) return buildQueueLastPos;
            // check for building in queue that allows builting this unit
            for (int index = 0; index < buildQueue.getSize(); index++) {
                BuildableType toBuild = buildQueue.getElementAt(index);
                if (toBuild == buildableType) continue;
                if (toBuild.hasAbility(Ability.BUILD, buildableType)) {
                    return buildQueueLastPos;
                }
            }
            return UNABLE_TO_BUILD;
        }

        if (buildableType instanceof BuildingType) {
            BuildingType upgradesFrom = ((BuildingType)buildableType
                    ).getUpgradesFrom();
            BuildingType upgradesTo = ((BuildingType)buildableType)
                    .getUpgradesTo();
            // does not depend on nothing, but still cannot be built
            if (!canBuild && upgradesFrom == null) {
                return UNABLE_TO_BUILD;
            }

            // if can be built and does not have any upgrade,
            // then it can be built at any time
            if (canBuild && upgradesTo == null) {
                return buildQueueLastPos;
            }

            // if can be built, does not depend on anything, mark
            // upgrades from as found
            boolean foundUpgradesFrom = canBuild;
            for (int index = 0; index < buildQueue.getSize(); index++) {
                BuildableType toBuild = buildQueue.getElementAt(index);
                
                if (toBuild == buildableType) continue;

                if (!canBuild && !foundUpgradesFrom
                    && upgradesFrom.equals(toBuild)) {
                    foundUpgradesFrom = true;
                    // nothing else to upgrade this building to
                    if (upgradesTo == null) return buildQueueLastPos;
                }
                // found a building it upgrades to, cannot go to or
                // beyond this position
                if (foundUpgradesFrom && upgradesTo != null
                    && upgradesTo.equals(toBuild)) return index;

                // Don't go past a unit this building can build.
                if (buildableType.hasAbility(Ability.BUILD, toBuild)) {
                    return index;
                }
            }
            return buildQueueLastPos;
        }
        return UNABLE_TO_BUILD;
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


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String FAIL = "FAIL";
        if (this.colony.getOwner() == getMyPlayer()) {
            String command = ae.getActionCommand();
            List<BuildableType> buildables = getBuildableTypes(this
                    .buildQueueList);
            while (!buildables.isEmpty()
                && lockReasons.get(buildables.get(0)) != null) {
                getGUI().showInformationMessage(buildables.get(0),
                    this.colony.getUnbuildableMessage(buildables.get(0)));
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
        getGUI().removeFromCanvas(this);
    }


    // Interface ItemListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void itemStateChanged(ItemEvent event) {
        if (event.getSource() == this.compactBox) {
            updateDetailView();
            defaultCompact = this.compactBox.isSelected();
        } else if (event.getSource() == this.showAllBox) {
            updateAllLists();
            defaultShowAll = this.showAllBox.isSelected();
        }
    }
}
