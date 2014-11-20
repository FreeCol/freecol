/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
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
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.client.gui.plaf.FreeColSelectedPanelUI;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Limit;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.resources.ResourceManager;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * The panel used to display a colony build queue.
 */
public class BuildQueuePanel extends FreeColPanel implements ItemListener {

    private static Logger logger = Logger.getLogger(BuildQueuePanel.class.getName());

    private class DefaultBuildQueueCellRenderer
        implements ListCellRenderer<BuildableType> {

        private JPanel itemPanel = new JPanel();
        private JPanel selectedPanel = new JPanel();
        private JLabel imageLabel = new JLabel(new ImageIcon());
        private JLabel nameLabel = new JLabel();

        private JLabel lockLabel
            = new JLabel(new ImageIcon(ResourceManager.getImage("lock.image", 0.5)));

        private Dimension buildingDimension = new Dimension(-1, 48);


        public DefaultBuildQueueCellRenderer() {
            itemPanel.setOpaque(false);
            itemPanel.setLayout(new MigLayout());
            selectedPanel.setOpaque(false);
            selectedPanel.setLayout(new MigLayout());
            selectedPanel.setUI((PanelUI) FreeColSelectedPanelUI.createUI(selectedPanel));
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

            ((ImageIcon)imageLabel.getIcon()).setImage(ResourceManager
                .getImage(value.getId() + ".image", buildingDimension));

            nameLabel.setText(Messages.message(value.getNameKey()));
            panel.setToolTipText(lockReasons.get(value));
            panel.add(imageLabel, "span 1 2");
            if (lockReasons.get(value) == null) {
                panel.add(nameLabel, "wrap");
            } else {
                panel.add(nameLabel, "split 2");
                panel.add(lockLabel, "wrap");
            }

            List<AbstractGoods> required = value.getRequiredGoods();
            int size = required.size();
            for (int i = 0; i < size; i++) {
                AbstractGoods goods = required.get(i);
                ImageIcon icon = new ImageIcon(ResourceManager.getImage(goods.getType().getId() + ".image", 0.66));
                JLabel goodsLabel = new JLabel(Integer.toString(goods.getAmount()),
                    icon, SwingConstants.CENTER);
                if (i == 0 && size > 1) {
                    panel.add(goodsLabel, "split " + size);
                } else {
                    panel.add(goodsLabel);
                }
            }
            return panel;
        }
    }

    class BuildQueueMouseAdapter extends MouseAdapter {

        private boolean add = true;
        
        private boolean enabled = false;

        public BuildQueueMouseAdapter(boolean add) {
            this.add = add;
        }

        @Override
        @SuppressWarnings("unchecked") // FIXME in Java7
        public void mousePressed(MouseEvent e) {
            if (!enabled && e.getClickCount() == 1 && !e.isConsumed()) {
                enabled = true;
            }

            if (enabled) {
                JList<BuildableType> source
                    = (JList<BuildableType>)e.getSource();
                if (e.getButton() == MouseEvent.BUTTON3
                    || e.isPopupTrigger()) {
                    int index = source.locationToIndex(e.getPoint());
                    BuildableType type = source.getModel().getElementAt(index);
                    getGUI().showColopediaPanel(type.getId());
                } else if (e.getClickCount() > 1 && !e.isConsumed()) {
                    JList<BuildableType> bql = BuildQueuePanel.this.buildQueueList;
                    DefaultListModel<BuildableType> model
                        = (DefaultListModel<BuildableType>)bql.getModel();
                    if (source.getSelectedIndex() < 0) {
                        source.setSelectedIndex(source.locationToIndex(e.getPoint()));
                    }
                    for (BuildableType bt : source.getSelectedValuesList()) {
                        if (add) {
                            model.addElement(bt);
                        } else {
                            model.removeElement(bt);
                        }
                    }
                    updateAllLists();
                }
            }
        }
    }

    private static final String BUY = "buy";

    private static final int UNABLE_TO_BUILD = -1;

    private static boolean compact = false;
    private static boolean showAll = false;

    /** The enclosing colony. */
    private final Colony colony;

    /** A feature container for potential features from queued buildables. */
    private FeatureContainer featureContainer;

    private final BuildQueueTransferHandler buildQueueHandler
        = new BuildQueueTransferHandler();

    private ListCellRenderer cellRenderer;
    private JCheckBox compactBox = new JCheckBox();
    private JCheckBox showAllBox = new JCheckBox();
    private JList<BuildableType> buildQueueList;
    private JList unitList;
    private JList buildingList;
    private ConstructionPanel constructionPanel;
    private JButton buyBuilding;

    private Map<BuildableType, String> lockReasons
        = new HashMap<BuildableType, String>();
    private Set<BuildableType> unbuildableTypes
        = new HashSet<BuildableType>();


    @SuppressWarnings("unchecked") // FIXME in Java7
    public BuildQueuePanel(FreeColClient freeColClient, Colony colony) {
        super(freeColClient, new MigLayout("wrap 3", 
                "[260:][390:, fill][260:]", "[][][300:400:][]"));

        this.colony = colony;
        this.featureContainer = new FeatureContainer();

        DefaultListModel<BuildableType> current
            = new DefaultListModel<BuildableType>();
        for (BuildableType type : this.colony.getBuildQueue()) {
            current.addElement(type);
            this.featureContainer.addFeatures(type);
        }

        this.cellRenderer = getCellRenderer();

        // remove previous listeners
        for (ItemListener listener : compactBox.getItemListeners()) {
            compactBox.removeItemListener(listener);
        }
        compactBox.setText(Messages.message("colonyPanel.compactView"));
        compactBox.addItemListener(this);

        // remove previous listeners
        for (ItemListener listener : showAllBox.getItemListeners()) {
            showAllBox.removeItemListener(listener);
        }
        showAllBox.setText(Messages.message("colonyPanel.showAll"));
        showAllBox.addItemListener(this);

        this.buildQueueList = new JList<BuildableType>(current);
        this.buildQueueList.setTransferHandler(buildQueueHandler);
        this.buildQueueList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.buildQueueList.setDragEnabled(true);
        this.buildQueueList.setCellRenderer(this.cellRenderer);
        this.buildQueueList.addMouseListener(new BuildQueueMouseAdapter(false));
        Action deleteAction = new AbstractAction() {
                @SuppressWarnings("deprecation") // FIXME in Java7
                public void actionPerformed(ActionEvent e) {
                    JList<BuildableType> bql = BuildQueuePanel.this.buildQueueList;
                    for (BuildableType bt : bql.getSelectedValuesList()) {
                        removeBuildable(bt);
                    }
                    updateAllLists();
                }
            };
        this.buildQueueList.getInputMap()
            .put(KeyStroke.getKeyStroke("DELETE"), "delete");
        buildQueueList.getActionMap().put("delete", deleteAction);

        Action addAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    JList<BuildableType> bql = BuildQueuePanel.this.buildQueueList;
                    DefaultListModel<BuildableType> model
                        = (DefaultListModel<BuildableType>)bql.getModel();
                    JList<BuildableType> btl
                        = (JList<BuildableType>)e.getSource();
                    for (BuildableType bt : btl.getSelectedValuesList()) {
                        model.addElement(bt);
                    }
                    updateAllLists();
                }
            };

        BuildQueueMouseAdapter adapter = new BuildQueueMouseAdapter(true);
        DefaultListModel units = new DefaultListModel();
        unitList = new JList(units);
        unitList.setTransferHandler(buildQueueHandler);
        unitList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        unitList.setDragEnabled(true);
        unitList.setCellRenderer(this.cellRenderer);
        unitList.addMouseListener(adapter);

        unitList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "add");
        unitList.getActionMap().put("add", addAction);

        DefaultListModel buildings = new DefaultListModel();
        buildingList = new JList(buildings);
        buildingList.setTransferHandler(buildQueueHandler);
        buildingList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        buildingList.setDragEnabled(true);
        buildingList.setCellRenderer(this.cellRenderer);
        buildingList.addMouseListener(adapter);

        buildingList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "add");
        buildingList.getActionMap().put("add", addAction);

        JLabel headLine = new JLabel(Messages.message("colonyPanel.buildQueue"));
        headLine.setFont(GUI.BIG_HEADER_FONT);

        buyBuilding = new JButton(Messages.message("colonyPanel.buyBuilding"));
        buyBuilding.setActionCommand(BUY);
        buyBuilding.addActionListener(this);

        constructionPanel = new ConstructionPanel(freeColClient, this.colony, false);
        StringTemplate buildingNothing = StringTemplate.template("colonyPanel.currentlyBuilding")
            .add("%buildable%", "nothing");
        constructionPanel.setDefaultLabel(buildingNothing);

        updateAllLists();
        compactBox.setSelected(compact);
        showAllBox.setSelected(showAll);

        add(headLine, "span 3, align center, wrap 40");
        add(new JLabel(Messages.message("colonyPanel.units")), "align center");
        add(new JLabel(Messages.message("colonyPanel.buildQueue")), "align center");
        add(new JLabel(Messages.message("colonyPanel.buildings")), "align center");
        add(new JScrollPane(unitList), "grow");
        add(constructionPanel, "split 2, flowy");
        add(new JScrollPane(this.buildQueueList), "grow");
        add(new JScrollPane(buildingList), "grow, wrap 20");
        add(buyBuilding, "span, split 4");
        add(compactBox);
        add(showAllBox);
        add(okButton, "tag ok");
    }

    public Colony getColony() {
        return this.colony;
    }

    private void removeBuildable(Object type) {
        DefaultListModel model = (DefaultListModel)this.buildQueueList.getModel();
        model.removeElement(type);
    }

    @SuppressWarnings("unchecked") // FIXME in Java7
    private void updateUnitList() {
        final Specification spec = getSpecification();
        DefaultListModel units = (DefaultListModel) unitList.getModel();
        units.clear();
        loop: for (UnitType unitType : spec.getBuildableUnitTypes()) {
            // compare colony.getNoBuildReason()
            List<String> lockReason = new ArrayList<String>();
            if (unbuildableTypes.contains(unitType)) {
                continue;
            }

            if (unitType.getRequiredPopulation() > this.colony.getUnitCount()) {
                lockReason.add(Messages.message(StringTemplate.template("colonyPanel.populationTooSmall")
                                                .addAmount("%number%", unitType.getRequiredPopulation())));
            }

            if (unitType.getLimits() != null) {
                for (Limit limit : unitType.getLimits()) {
                    if (!limit.evaluate(this.colony)) {
                        lockReason.add(Messages.message(limit.getDescriptionKey()));
                    }
                }
            }

            if (!(this.colony.hasAbility(Ability.BUILD, unitType, getGame().getTurn())
                    || this.featureContainer.hasAbility(Ability.BUILD, unitType, null))) {
                boolean builderFound = false;
                for (Ability ability : spec.getAbilities(Ability.BUILD)) {
                    if (ability.appliesTo(unitType)
                        && ability.getValue()
                        && ability.getSource() != null
                        && !unbuildableTypes.contains(ability.getSource())) {
                        builderFound = true;
                        lockReason.add(Messages.getName(ability.getSource()));
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
                    && this.featureContainer.hasAbility(entry.getKey(), null, null)
                    != entry.getValue()) {
                    List<FreeColGameObjectType> sources
                        = spec.getTypesProviding(entry.getKey(),
                                                 entry.getValue());
                    if (sources.isEmpty()) {
                        // no type provides the required ability
                        unbuildableTypes.add(unitType);
                        continue loop;
                    } else {
                        lockReason.add(Messages.message(sources.get(0).getNameKey()));
                    }
                }
            }
            if (lockReason.isEmpty()) {
                lockReasons.put(unitType, null);
            } else {
                lockReasons.put(unitType, Messages.message(StringTemplate.template("colonyPanel.requires")
                                                           .addName("%string%", join("/", lockReason))));
            }
            if (lockReason.isEmpty() || showAllBox.isSelected()) {
                units.addElement(unitType);
            }
        }
    }

    @SuppressWarnings("unchecked") // FIXME in Java7
    private void updateBuildingList() {
        DefaultListModel buildings = (DefaultListModel) buildingList.getModel();
        DefaultListModel current = (DefaultListModel)this.buildQueueList.getModel();
        buildings.clear();
        loop: for (BuildingType buildingType : getSpecification().getBuildingTypeList()) {
            // compare colony.getNoBuildReason()
            List<String> lockReason = new ArrayList<String>();
            Building colonyBuilding = this.colony.getBuilding(buildingType);
            if (current.contains(buildingType) || hasBuildingType(buildingType)) {
                // only one building of any kind
                continue;
            } else if (unbuildableTypes.contains(buildingType)) {
                continue;
            } else if (!buildingType.needsGoodsToBuild()) {
                // pre-built
                continue;
            } else if (unbuildableTypes.contains(buildingType.getUpgradesFrom())) {
                // impossible upgrade path
                unbuildableTypes.add(buildingType);
                continue;
            }

            if (buildingType.hasAbility(Ability.COASTAL_ONLY)
                && !this.colony.getTile().isCoastland()) {
                lockReason.add(Messages.message(StringTemplate.template("colonyPanel.coastalOnly")));
            }
                                                
            if (buildingType.getRequiredPopulation() > this.colony.getUnitCount()) {
                lockReason.add(Messages.message(StringTemplate.template("colonyPanel.populationTooSmall")
                                                .addAmount("%number%", buildingType.getRequiredPopulation())));
            }

            for (Entry<String, Boolean> entry
                     : buildingType.getRequiredAbilities().entrySet()) {
                if (this.colony.hasAbility(entry.getKey()) != entry.getValue()
                    && this.featureContainer.hasAbility(entry.getKey(), null, null)
                    != entry.getValue()) {
                    List<FreeColGameObjectType> sources = getSpecification()
                        .getTypesProviding(entry.getKey(), entry.getValue());
                    if (sources.isEmpty()) {
                        // no type provides the required ability
                        unbuildableTypes.add(buildingType);
                        continue loop;
                    } else {
                        lockReason.add(Messages.message(sources.get(0).getNameKey()));
                    }
                }
            }

            if (buildingType.getLimits() != null) {
                for (Limit limit : buildingType.getLimits()) {
                    if (!limit.evaluate(this.colony)) {
                        lockReason.add(Messages.message(limit.getDescriptionKey()));
                    }
                }
            }

            if (buildingType.getUpgradesFrom() != null
                && !current.contains(buildingType.getUpgradesFrom())) {
                if (colonyBuilding == null
                    || colonyBuilding.getType() != buildingType.getUpgradesFrom()) {
                    lockReason.add(Messages.message(buildingType.getUpgradesFrom().getNameKey()));
                }
            }
            if (lockReason.isEmpty()) {
                lockReasons.put(buildingType, null);
            } else {
                lockReasons.put(buildingType, Messages.message(StringTemplate.template("colonyPanel.requires")
                                                               .addName("%string%", join("/", lockReason))));
            }
            if (lockReason.isEmpty() || showAllBox.isSelected()) {
                buildings.addElement(buildingType);
            }
        }
    }

    private void updateAllLists() {
        DefaultListModel<BuildableType> current = (DefaultListModel<BuildableType>)this.buildQueueList.getModel();
        this.featureContainer = new FeatureContainer();
        for (Object type : current.toArray()) {
            if (getMinimumIndex((BuildableType) type) >= 0) {
                featureContainer.addFeatures((BuildableType)type);
            } else {
                current.removeElement(type);
            }
        }
        // ATTENTION: buildings must be updated first, since units
        // might depend on the build ability of an unbuildable
        // building
        updateBuildingList();
        updateUnitList();
        updateBuyBuildingButton();
        // work-around to re-initialize construction panel
        PropertyChangeEvent event = new PropertyChangeEvent(this.colony, ConstructionPanel.EVENT, null,
                                                            getBuildableTypes(this.buildQueueList));
        constructionPanel.propertyChange(event);
    }

    private void updateBuyBuildingButton() {
        final boolean pay = getSpecification()
            .getBoolean(GameOptions.PAY_FOR_BUILDING);

        DefaultListModel current = (DefaultListModel<BuildableType>)this.buildQueueList.getModel();
        if (current.getSize() == 0 || !pay) {
            buyBuilding.setEnabled(false);
        } else {
            buyBuilding.setEnabled(this.colony.canPayToFinishBuilding((BuildableType) current.getElementAt(0)));
        }
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

    private int getMinimumIndex(BuildableType buildableType) {
        ListModel<BuildableType> buildQueue = this.buildQueueList.getModel();
        if (buildableType instanceof UnitType) {
            if (this.colony.canBuild(buildableType)) {
                return 0;
            } else {
                for (int index = 0; index < buildQueue.getSize(); index++) {
                    if (((BuildableType) buildQueue.getElementAt(index))
                        .hasAbility(Ability.BUILD, buildableType)) {
                        return index + 1;
                    }
                }
            }
        } else if (buildableType instanceof BuildingType) {
            BuildingType upgradesFrom = ((BuildingType) buildableType).getUpgradesFrom();
            if (upgradesFrom == null) {
                return 0;
            } else {
                Building building = this.colony.getBuilding((BuildingType) buildableType);
                BuildingType buildingType = (building == null) ? null : building.getType();
                if (buildingType == upgradesFrom) {
                    return 0;
                } else {
                    for (int index = 0; index < buildQueue.getSize(); index++) {
                        if (upgradesFrom.equals(buildQueue.getElementAt(index))) {
                            return index + 1;
                        }
                    }
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
            if(canBuild){
                return buildQueueLastPos;
            }
            // check for building in queue that allows builting this unit
            for (int index = 0; index < buildQueue.getSize(); index++) {
                BuildableType toBuild = (BuildableType) buildQueue.getElementAt(index);

                if(toBuild == buildableType){
                    continue;
                }

                if (toBuild.hasAbility(Ability.BUILD, buildableType)) {
                    return buildQueueLastPos;
                }
            }

            return UNABLE_TO_BUILD;
        }

        if (buildableType instanceof BuildingType) {
            BuildingType upgradesFrom = ((BuildingType) buildableType).getUpgradesFrom();
            BuildingType upgradesTo = ((BuildingType) buildableType).getUpgradesTo();

            // does not depend on nothing, but still cannot be built
            if (!canBuild && upgradesFrom == null) {
                return UNABLE_TO_BUILD;
            }

            // if can be built and does not have any upgrade,
            //then it can be built at any time
            if(canBuild && upgradesTo == null){
                return buildQueueLastPos;
            }

            // if can be built, does not depend on anything, mark upgradesfrom as found
            boolean foundUpgradesFrom = canBuild? true:false;
            for (int index = 0; index < buildQueue.getSize(); index++) {
                BuildableType toBuild = (BuildableType) buildQueue.getElementAt(index);

                if(toBuild == buildableType){
                    continue;
                }

                if (!canBuild && !foundUpgradesFrom && upgradesFrom.equals(toBuild)) {
                    foundUpgradesFrom = true;
                    // nothing else to upgrade this building to
                    if(upgradesTo == null){
                        return buildQueueLastPos;
                    }
                }
                // found a building it upgrades to, cannot go to or beyond this position
                if (foundUpgradesFrom && upgradesTo != null && upgradesTo.equals(toBuild)) {
                    return index;
                }

                // Don't go past a unit this building can build.
                if (buildableType.hasAbility(Ability.BUILD, toBuild)) {
                    return index;
                }
            }

            return buildQueueLastPos;
        }

        return UNABLE_TO_BUILD;
    }

    public void itemStateChanged(ItemEvent event) {
        if (event.getSource() == compactBox) {
            updateDetailView();
            compact = compactBox.isSelected();
        } else if (event.getSource() == showAllBox) {
            updateAllLists();
            showAll = showAllBox.isSelected();
        }
    }

    @SuppressWarnings("unchecked") // FIXME in Java7
    private void updateDetailView() {
        this.cellRenderer = getCellRenderer();
        this.buildQueueList.setCellRenderer(this.cellRenderer);
        buildingList.setCellRenderer(this.cellRenderer);
        unitList.setCellRenderer(this.cellRenderer);
    }

    private ListCellRenderer getCellRenderer() {
        if (compactBox.isSelected()) {
            if (this.cellRenderer == null
                || this.cellRenderer instanceof DefaultBuildQueueCellRenderer) {
                return new FreeColComboBoxRenderer<BuildableType>();
            }
        } else if (this.cellRenderer == null
            || this.cellRenderer instanceof FreeColComboBoxRenderer) {
            return new DefaultBuildQueueCellRenderer();
        }

        return this.cellRenderer; // return current one
    }

    /**
     * This class implements a transfer handler able to transfer
     * <code>BuildQueueItem</code>s between the build queue list, the
     * unit list and the building list.
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public class BuildQueueTransferHandler extends TransferHandler {

        private final DataFlavor buildQueueFlavor
            = new DataFlavor(List.class, "BuildingQueueFlavor");

        JList source = null;
        int[] indices = null;
        int numberOfItems = 0;  // number of items to be added

        /**
         * Imports a build queue into the build queue list, the
         * building list or the unit list, if possible.
         * @param comp The list which imports data.
         * @param data The build queue to import.
         * @return Whether the import was successful.
         */
        @Override
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

                if (!(transferData instanceof List<?>)) {
                    return false;
                }

                for (Object object : (List<?>) transferData) {
                    // first we need to make sure all items are compatible
                    //with the drop zone
                    if (!(object instanceof BuildableType)) {
                        return false;
                    }

                    // check if trying to drop units in the Building list
                    //or buildings in the Unit List
                    if ((object instanceof BuildingType && target == unitList)
                        || (object instanceof UnitType && target == buildingList)) {
                        return false;
                    }

                    // Object check complete
                    // Add object to the to-add list
                    buildQueue.add((BuildableType) object);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Build queue import fail", e);
                return false;
            }

            // check if every Buildable can be added to the queue
            for (BuildableType type : buildQueue) {
                if (getMinimumIndex(type) < 0) {
                    return false;
                }
            }

            // This is where the dragged target was dropped.
            int preferredIndex = target.getSelectedIndex();

            boolean isOrderingQueue = false;
            int prevPos = -1;
            if (source.equals(target)) {
                // only the build queue allows ordering
                if (target != BuildQueuePanel.this.buildQueueList
                    && target.getParent() != BuildQueuePanel.this.buildQueueList) {
                    return false;
                }

                // find previous position
                for(int i=0; i < targetModel.getSize(); i++){
                    if(targetModel.getElementAt(i) == buildQueue.get(0)){
                        prevPos = i;
                        break;
                    }
                }

                // don't drop selection on itself
                if(preferredIndex != -1 && prevPos == preferredIndex){
                    indices = null;
                    return false;
                }

                int maximumIndex = getMaximumIndex(buildQueue.get(0));
                if(preferredIndex > maximumIndex){
                    indices = null;
                    return false;
                }

                isOrderingQueue = true;
                numberOfItems = buildQueue.size();
            }
            else if (source == BuildQueuePanel.this.buildQueueList) {
                // Dragging out of build queue - just remove the element.
                // updateAllLists takes care of the rest.
                DefaultListModel sourceModel = (DefaultListModel) source.getModel();

                for(Object obj : buildQueue) {
                    sourceModel.removeElement(obj);
                }

                return true;
            }

            int maxIndex = targetModel.size();
            // Set index to add to the last position, if not set or out-of-bounds
            if (preferredIndex < 0 || preferredIndex > maxIndex) {
                preferredIndex = maxIndex;
            }

            if(isOrderingQueue){
                // Remove all elements in the build queue from the target.
                for(Object obj : buildQueue){
                    targetModel.removeElement(obj);
                }
            }

            for (int index = 0; index < buildQueue.size(); index++) {
                BuildableType toBuild = (BuildableType) buildQueue.get(index);

                int minimumIndex = getMinimumIndex(toBuild);

                // minimumIndex == targetModel.size() means it has to go at the
                // end.
                if (minimumIndex < targetModel.size())
                    {
                        int maximumIndex = getMaximumIndex(toBuild);

                        // minimumIndex == maximumIndex means there is only one
                        // place it can go.
                        if (minimumIndex != maximumIndex)
                            {
                                if (minimumIndex < preferredIndex + index) {
                                    minimumIndex = preferredIndex + index;
                                }

                                if (minimumIndex > targetModel.size())
                                    {
                                        minimumIndex = targetModel.size();
                                    }

                                if (minimumIndex > maximumIndex)
                                    {
                                        minimumIndex = maximumIndex;
                                    }
                            }
                    }

                targetModel.add(minimumIndex,buildQueue.get(index));
            }

            // update selected index to new position
            if (isOrderingQueue) {
                BuildQueuePanel.this.buildQueueList
                    .setSelectedIndex(preferredIndex);
            }
            return true;
        }

        /**
         * Cleans up after a successful import.
         * @param source The component that has exported data.
         * @param data The data exported.
         * @param action The transfer action, e.g. MOVE.
         */
        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            // clean up
            indices = null;
            numberOfItems = 0;
            updateAllLists();
        }

        /**
         * Returns <code>true</code> if the component can import this
         * data flavor.
         * @param comp The component to import data.
         * @param flavors An array of data flavors.
         */
        @Override
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
        @Override
        @SuppressWarnings("deprecation") // FIXME in Java7
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
        @Override
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
             *
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


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent event) {
        final String FAIL = "FAIL";
        if (this.colony.getOwner() == getMyPlayer()) {
            String command = event.getActionCommand();
            List<BuildableType> buildables = getBuildableTypes(this.buildQueueList);
            while (!buildables.isEmpty()
                && lockReasons.get(buildables.get(0)) != null) {
                getGUI().showInformationMessage(buildables.get(0),
                    StringTemplate.template("colonyPanel.unbuildable")
                        .addName("%colony%", this.colony.getName())
                        .add("%object%", buildables.get(0).getNameKey()));
                command = FAIL;
                removeBuildable(buildables.remove(0));
            }
            getController().setBuildQueue(this.colony, buildables);
            if (FAIL.equals(command)) { // Let the user reconsider.
                updateAllLists();
                return;
            } else if (OK.equals(command)) {
                // do nothing?
            } else if (BUY.equals(command)) {
                getController().payForBuilding(this.colony);
            } else {
                super.actionPerformed(event);
            }
        }
        getGUI().removeFromCanvas(this);
    }
}
