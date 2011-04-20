/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.Feature;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Scope;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.common.util.Utils;


/**
 * This panel displays the Colopedia.
 */
public final class ColopediaPanel extends FreeColPanel implements TreeSelectionListener {

    private static final Logger logger = Logger.getLogger(ColopediaPanel.class.getName());

    public static enum PanelType { TERRAIN, RESOURCES, UNITS, GOODS,
            SKILLS, BUILDINGS, FATHERS, NATIONS, NATION_TYPES }

    private static final Font arrowFont = ResourceManager.getFont("SimpleFont", Font.BOLD, 24f);
    private static final DecimalFormat modifierFormat =
        new DecimalFormat("0.##");

    private final String none;

    // layout of production modifier panel
    private static final int MODIFIERS_PER_ROW = 5;

    private JLabel header;

    private JPanel listPanel;

    private JPanel detailPanel;

    private JTree tree;

    private Map<String, DefaultMutableTreeNode> nodeMap =
        new HashMap<String, DefaultMutableTreeNode>();

    /**
     * The saved size of this panel.
     */
    private static Dimension savedSize = new Dimension(850, 600);


    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     */
    public ColopediaPanel(Canvas parent, PanelType panelType, FreeColGameObjectType objectType) {
        super(parent);

        if (panelType == null) {
            panelType = getPanelType(objectType);
        }

        none = Messages.message("none");

        setLayout(new MigLayout("fill", "[200:]unrelated[550:, grow, fill]", "[][grow, fill][]"));

        header = getDefaultHeader(Messages.message("menuBar.colopedia"));
        add(header, "span, align center");

        listPanel = new JPanel() {
                @Override
                public String getUIClassID() {
                    return "ColopediaPanelUI";
                }
            };
        listPanel.setOpaque(true);
        JScrollPane sl = new JScrollPane(listPanel,
                                         JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sl.getVerticalScrollBar().setUnitIncrement(16);
        sl.getViewport().setOpaque(false);
        add(sl);

        detailPanel = new JPanel() {
                @Override
                public String getUIClassID() {
                    return "ColopediaPanelUI";
                }
            };
        detailPanel.setOpaque(true);
        JScrollPane detail = new JScrollPane(detailPanel,
                                             JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        detail.getVerticalScrollBar().setUnitIncrement(16);
        detail.getViewport().setOpaque(false);
        add(detail, "grow");

        add(okButton, "newline 20, span, tag ok");

        setPreferredSize(savedSize);
        tree = buildTree();
        initialize(panelType, objectType);
    }

    /**
     * Get the <code>SavedSize</code> value.
     *
     * @return a <code>Dimension</code> value
     */
    public final Dimension getSavedSize() {
        return savedSize;
    }

    /**
     * Set the <code>SavedSize</code> value.
     *
     * @param newSavedSize The new SavedSize value.
     */
    public final void setSavedSize(final Dimension newSavedSize) {
        ColopediaPanel.savedSize = newSavedSize;
    }

    /**
     * Prepares this panel to be displayed.
     *
     * @param panelType - the panel type
     * @param type - the FreeColGameObjectType of the item to be displayed
     */
    public void initialize(PanelType panelType, FreeColGameObjectType type) {
        detailPanel.removeAll();
        DefaultMutableTreeNode node = nodeMap.get(panelType.toString());
        tree.expandPath(new TreePath(node.getPath()));
        selectDetail(panelType, type);
        detailPanel.validate();
    }

    /**
     * Prepares this panel to be displayed.
     *
     * @param type - the FreeColGameObjectType of the item to be displayed
     */
    public void initialize(FreeColGameObjectType type) {
        initialize(getPanelType(type), type);
    }

    /**
     * Returns the <code>PanelType</code> to be used for the given
     * <code>FreeColGameObjectType</code>.
     *
     * @param type - the FreeColGameObjectType of the item to be displayed
     * @return the PanelType
     */
    public PanelType getPanelType(FreeColGameObjectType type) {
        if (type instanceof TileType) {
            return PanelType.TERRAIN;
        } else if (type instanceof ResourceType) {
            return PanelType.RESOURCES;
        } else if (type instanceof UnitType) {
            if (((UnitType) type).hasSkill()) {
                return PanelType.SKILLS;
            } else {
                return PanelType.UNITS;
            }
        } else if (type instanceof GoodsType) {
            return PanelType.GOODS;
        } else if (type instanceof BuildingType) {
            return PanelType.BUILDINGS;
        } else if (type instanceof FoundingFather) {
            return PanelType.FATHERS;
        } else if (type instanceof Nation) {
            return PanelType.NATIONS;
        } else if (type instanceof NationType) {
            return PanelType.NATION_TYPES;
        } else {
            return null;
        }
    }


    public void selectDetail(PanelType panelType, FreeColGameObjectType type) {

        detailPanel.removeAll();

        if (type != null) {

            switch (panelType) {
            case TERRAIN:
                buildTerrainDetail((TileType) type);
                break;
            case RESOURCES:
                buildResourceDetail((ResourceType) type);
                break;
            case UNITS:
            case SKILLS:
                buildUnitDetail((UnitType) type);
                break;
            case GOODS:
                buildGoodsDetail((GoodsType) type);
                break;
            case BUILDINGS:
                buildBuildingDetail((BuildingType) type);
                break;
            case FATHERS:
                buildFatherDetail((FoundingFather) type);
                break;
            case NATIONS:
                buildNationDetail((Nation) type);
                break;
            case NATION_TYPES:
                if (type instanceof EuropeanNationType) {
                    buildEuropeanNationTypeDetail((EuropeanNationType) type);
                } else if (type instanceof IndianNationType) {
                    buildIndianNationTypeDetail((IndianNationType) type);
                }
                break;
            default:
                break;
            }
        }
    }

    /**
     * Builds the JTree which represents the navigation menu and then returns it
     *
     * @return The navigation tree.
     */
    private JTree buildTree() {
        DefaultMutableTreeNode root;
        root = new DefaultMutableTreeNode(new ColopediaTreeItem(null, Messages.message("menuBar.colopedia")));

        DefaultMutableTreeNode terrain;
        terrain = new DefaultMutableTreeNode(new ColopediaTreeItem(PanelType.TERRAIN));
        buildTerrainSubtree(terrain);
        root.add(terrain);
        nodeMap.put(PanelType.TERRAIN.toString(), terrain);

        root.add(buildSubTree(PanelType.RESOURCES, getSpecification().getResourceTypeList(), 0.75));

        root.add(buildSubTree(PanelType.GOODS, getSpecification().getGoodsTypeList(), 0.75));

        List<UnitType> units = new ArrayList<UnitType>();
        List<UnitType> skills = new ArrayList<UnitType>();
        for (UnitType u : getSpecification().getUnitTypeList()) {
            if (u.getSkill() <= 0 || u.hasAbility("model.ability.expertSoldier")) {
                units.add(u);
            } else {
                skills.add(u);
            }
        }

        root.add(buildSubTree(PanelType.UNITS, units, 0.5));
        root.add(buildSubTree(PanelType.SKILLS, skills, 0.5));

        DefaultMutableTreeNode buildings =
            new DefaultMutableTreeNode(new ColopediaTreeItem(PanelType.BUILDINGS));
        buildBuildingSubtree(buildings);
        root.add(buildings);
        nodeMap.put(PanelType.BUILDINGS.toString(), buildings);

        DefaultMutableTreeNode fathers =
            new DefaultMutableTreeNode(new ColopediaTreeItem(PanelType.FATHERS));
        buildFathersSubtree(fathers);
        root.add(fathers);
        nodeMap.put(PanelType.FATHERS.toString(), fathers);

        List<Nation> nations = new ArrayList<Nation>();
        nations.addAll(getSpecification().getEuropeanNations());
        nations.addAll(getSpecification().getIndianNations());
        root.add(buildSubTree(PanelType.NATIONS, nations, 0.5));

        DefaultMutableTreeNode nationTypes =
            new DefaultMutableTreeNode(new ColopediaTreeItem(PanelType.NATION_TYPES));
        buildNationTypesSubtree(nationTypes);
        root.add(nationTypes);
        nodeMap.put(PanelType.NATION_TYPES.toString(), nationTypes);

        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel) {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(200, super.getPreferredSize().height);
                }
            };
        tree.setRootVisible(false);
        tree.setCellRenderer(new ColopediaTreeCellRenderer());
        tree.setOpaque(false);
        tree.addTreeSelectionListener(this);

        listPanel.add(tree);

        return tree;
    }


    private <T extends FreeColGameObjectType> DefaultMutableTreeNode
                       buildSubTree(PanelType panelType, List<T> types, double scale) {
        DefaultMutableTreeNode parent = new DefaultMutableTreeNode(new ColopediaTreeItem(panelType));
        for (FreeColGameObjectType type : types) {
            Image image = getLibrary().getImage(type, scale);
            parent.add(buildItem(type, new ImageIcon(image)));
        }
        nodeMap.put(panelType.toString(), parent);
        return parent;
    }


    /**
     * Builds the buttons for all the tiles.
     * @param parent
     */
    private void buildTerrainSubtree(DefaultMutableTreeNode parent) {
        for (TileType t : getSpecification().getTileTypeList()) {
            ImageIcon icon = new ImageIcon(getLibrary().getCompoundTerrainImage(t, 0.25));
            parent.add(buildItem(t, icon));
        }
    }

    /**
     * Builds the buttons for all the buildings.
     * @param parent
     */
    private void buildBuildingSubtree(DefaultMutableTreeNode parent) {
        Image buildingImage = ResourceManager.getImage("Colopedia.buildingSection.image");
        ImageIcon buildingIcon = new ImageIcon((buildingImage != null) ? buildingImage : null);

        List<BuildingType> buildingTypes = new ArrayList<BuildingType>();
        Map<BuildingType, DefaultMutableTreeNode> buildingHash =
            new HashMap<BuildingType, DefaultMutableTreeNode>();
        for (BuildingType buildingType : getSpecification().getBuildingTypeList()) {
            if (buildingType.getUpgradesFrom() == null) {
                String name = Messages.message(buildingType.getNameKey());
                DefaultMutableTreeNode item =
                    new DefaultMutableTreeNode(new ColopediaTreeItem(buildingType,
                                                                     name,
                                                                     buildingIcon));
                buildingHash.put(buildingType, item);
                parent.add(item);
                nodeMap.put(buildingType.getId(), item);
            } else {
                buildingTypes.add(buildingType);
            }
        }

        while (!buildingTypes.isEmpty()) {
            for (Iterator<BuildingType> iterator = buildingTypes.iterator(); iterator.hasNext();) {
                BuildingType buildingType = iterator.next();
                DefaultMutableTreeNode node = buildingHash.get(buildingType.getUpgradesFrom());
                if (node != null) {
                    String name = Messages.message(buildingType.getNameKey());
                    DefaultMutableTreeNode item =
                        new DefaultMutableTreeNode(new ColopediaTreeItem(buildingType,
                                                                         name,
                                                                         buildingIcon));
                    node.add(item);
                    nodeMap.put(buildingType.getId(), item);
                    buildingHash.put(buildingType, item);
                    iterator.remove();
                }
            }

        }
    }

    /**
     * Builds the buttons for all the founding fathers.
     * @param parent
     */
    private void buildFathersSubtree(DefaultMutableTreeNode parent) {
        EnumMap<FoundingFatherType, List<FoundingFather>> fathersByType =
            new EnumMap<FoundingFatherType, List<FoundingFather>>(FoundingFatherType.class);
        for (FoundingFatherType fatherType : FoundingFatherType.values()) {
            fathersByType.put(fatherType, new ArrayList<FoundingFather>());
        }
        for (FoundingFather foundingFather : getSpecification().getFoundingFathers()) {
            fathersByType.get(foundingFather.getType()).add(foundingFather);
        }
        for (FoundingFatherType fatherType : FoundingFatherType.values()) {
            String typeName = Messages.message(FoundingFather.getTypeKey(fatherType));
            DefaultMutableTreeNode node =
                new DefaultMutableTreeNode(new ColopediaTreeItem(PanelType.FATHERS, typeName));

            parent.add(node);
            nodeMap.put(fatherType.toString(), node);
            for (FoundingFather father : fathersByType.get(fatherType)) {
                ImageIcon icon = new ImageIcon(ResourceManager.getImage("model.goods.bells.image", 0.75f));
                node.add(buildItem(father, icon));
            }
        }
    }

    /**
     * Builds the buttons for all the nation types.
     * @param parent
     */
    private void buildNationTypesSubtree(DefaultMutableTreeNode parent) {
        List<NationType> nations = new ArrayList<NationType>();
        nations.addAll(getSpecification().getEuropeanNationTypes());
        nations.addAll(getSpecification().getREFNationTypes());
        nations.addAll(getSpecification().getIndianNationTypes());
        for (NationType type : nations) {
            ImageIcon icon = new ImageIcon(ResourceManager.getImage("model.goods.bells.image", 0.75f));
            parent.add(buildItem(type, icon));
        }
    }

    private DefaultMutableTreeNode buildItem(FreeColGameObjectType type, ImageIcon icon) {
        String name = Messages.getName(type);
        DefaultMutableTreeNode item =
            new DefaultMutableTreeNode(new ColopediaTreeItem(type, name, icon));
        nodeMap.put(type.getId(), item);
        return item;
    }

    private JButton getButton(FreeColGameObjectType type, String text, ImageIcon icon) {
        JButton button = getLinkButton(text == null ? Messages.getName(type) : text, icon, type.getId());
        button.addActionListener(this);
        return button;
    }

    private JButton getButton(FreeColGameObjectType type) {
        return getButton(type, null, null);
    }

    private JButton getResourceButton(final ResourceType resourceType) {
        return getButton(resourceType, null, getLibrary().getBonusImageIcon(resourceType));
    }

    private JButton getGoodsButton(final GoodsType goodsType) {
        return getButton(goodsType, null, getLibrary().getGoodsImageIcon(goodsType));
    }

    private JButton getGoodsButton(final GoodsType goodsType, String text) {
        return getButton(goodsType, text, getLibrary().getGoodsImageIcon(goodsType));
    }

    private JButton getGoodsButton(final GoodsType goodsType, int amount) {
        return getButton(goodsType, Integer.toString(amount), getLibrary().getGoodsImageIcon(goodsType));
    }

    private JButton getUnitButton(AbstractUnit unit) {
        return getUnitButton(unit.getUnitType(getSpecification()), unit.getRole());
    }

    private JButton getUnitButton(final UnitType unitType, Role role) {
        ImageIcon unitIcon = getLibrary().getUnitImageIcon(unitType, role, 0.66);
        JButton unitButton = getButton(unitType, null, unitIcon);
        unitButton.setHorizontalAlignment(SwingConstants.LEFT);
        return unitButton;
    }

    private JButton getUnitButton(final UnitType unitType) {
        return getUnitButton(unitType, Role.DEFAULT);
    }


    /**
     * Builds the details panel for the given tile.
     *
     * @param tileType The TileType
     */
    private void buildTerrainDetail(TileType tileType) {

        detailPanel.setLayout(new MigLayout("wrap 4, gap 20", "[][]push[][]", ""));

        String movementCost = String.valueOf(tileType.getBasicMoveCost() / 3);
        String defenseBonus = none;
        Set<Modifier> defenceModifiers = tileType.getDefenceBonus();
        if (!defenceModifiers.isEmpty()) {
            defenseBonus = getModifierAsString(defenceModifiers.iterator().next());
        }

        JLabel nameLabel = localizedLabel(tileType.getNameKey());
        nameLabel.setFont(smallHeaderFont);
        detailPanel.add(nameLabel, "span, align center");

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.terrainImage")));
        Image terrainImage = getLibrary().getCompoundTerrainImage(tileType, 1);
        detailPanel.add(new JLabel(new ImageIcon(terrainImage)));

        List<ResourceType> resourceList = tileType.getResourceTypeList();
        if (resourceList.size() > 0) {
            detailPanel.add(new JLabel(Messages.message("colopedia.terrain.resource")));
            if (resourceList.size() > 1) {
                detailPanel.add(getResourceButton(resourceList.get(0)), "split " + resourceList.size());
                for (int index = 1; index < resourceList.size(); index++) {
                    detailPanel.add(getResourceButton(resourceList.get(index)));
                }
            } else {
                detailPanel.add(getResourceButton(resourceList.get(0)));
            }
        } else {
            detailPanel.add(new JLabel(), "wrap");
        }

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.movementCost")));
        detailPanel.add(new JLabel(movementCost));

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.defenseBonus")));
        detailPanel.add(new JLabel(defenseBonus));

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.production")));

        List<AbstractGoods> production = tileType.getProduction();
        if (production.size() > 0) {
            AbstractGoods goods = production.get(0);
            if (production.size() > 1) {
                detailPanel.add(getGoodsButton(goods.getType(), goods.getAmount()),
                                "span, split " + production.size());
                for (int index = 1; index < production.size(); index++) {
                    goods = production.get(index);
                    detailPanel.add(getGoodsButton(goods.getType(), goods.getAmount()));
                }
            } else {
                detailPanel.add(getGoodsButton(goods.getType(), goods.getAmount()), "span");
            }
        } else {
            detailPanel.add(new JLabel(), "wrap");
        }

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.description")));
        detailPanel.add(getDefaultTextArea(Messages.message(tileType.getDescriptionKey()), 20),
                        "span, growx");

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    /**
     * Builds the details panel for the given resource.
     *
     * @param type The ResourceType
     */
    private void buildResourceDetail(ResourceType type) {

        detailPanel.setLayout(new MigLayout("wrap 2, fillx, gapx 20", "", ""));

        JLabel name = localizedLabel(type.getNameKey());
        name.setFont(smallHeaderFont);
        detailPanel.add(name, "span, align center, wrap 40");

        Set<Modifier> modifiers = type.getFeatureContainer().getModifiers();

        detailPanel.add(new JLabel(Messages.message("colopedia.resource.bonusProduction")));
        JPanel goodsPanel = new JPanel();
        goodsPanel.setOpaque(false);
        for (Modifier modifier : modifiers) {
            String text = getModifierAsString(modifier);
            if (modifier.hasScope()) {
                List<String> scopeStrings = new ArrayList<String>();
                for (Scope scope : modifier.getScopes()) {
                    if (scope.getType() != null) {
                        FreeColGameObjectType objectType = getSpecification()
                            .getType(scope.getType());
                        scopeStrings.add(Messages.message(objectType.getNameKey()));
                    }
                }
                if (!scopeStrings.isEmpty()) {
                    text += " (" + Utils.join(", ", scopeStrings) + ")";
                }
            }

            GoodsType goodsType = getSpecification().getGoodsType(modifier.getId());
            JButton goodsButton = getGoodsButton(goodsType, text);
            goodsPanel.add(goodsButton);
        }
        detailPanel.add(goodsPanel);

        detailPanel.add(new JLabel(Messages.message("colopedia.resource.description")), "newline 20");
        detailPanel.add(getDefaultTextArea(Messages.message(type.getDescriptionKey()), 20), "growx");

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    /**
     * Builds the details panel for the given unit.
     *
     * @param type - the UnitType
     */
    private void buildUnitDetail(UnitType type) {

        detailPanel.setLayout(new MigLayout("wrap 2, fillx, gapx 20", "", ""));

        JLabel name = localizedLabel(type.getNameKey());
        name.setFont(smallHeaderFont);
        detailPanel.add(name, "span, align center, wrap 40");

        detailPanel.add(new JLabel(Messages.message("colopedia.unit.offensivePower")));
        detailPanel.add(new JLabel(Integer.toString(type.getOffence())));

        detailPanel.add(new JLabel(Messages.message("colopedia.unit.defensivePower")));
        detailPanel.add(new JLabel(Integer.toString(type.getDefence())));

        detailPanel.add(new JLabel(Messages.message("colopedia.unit.movement")));
        detailPanel.add(new JLabel(String.valueOf(type.getMovement()/3)));

        if (type.canCarryGoods() || type.canCarryUnits()) {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.capacity")));
            detailPanel.add(new JLabel(Integer.toString(type.getSpace())));
        }

        if (type.hasSkill()) {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.skill")));
            detailPanel.add(new JLabel(Integer.toString(type.getSkill())));

            List<BuildingType> schools = new ArrayList<BuildingType>();
            for (final BuildingType buildingType : getSpecification().getBuildingTypeList()) {
                if (buildingType.hasAbility("model.ability.teach") &&
                    buildingType.canAdd(type)) {
                    schools.add(buildingType);
                }
            }

            if (!schools.isEmpty()) {
                detailPanel.add(new JLabel(Messages.message("colopedia.unit.school")));
                if (schools.size() > 1) {
                    detailPanel.add(getButton(schools.get(0)),
                                    "span, split " + schools.size());
                    for (int index = 1; index < schools.size(); index++) {
                        detailPanel.add(getButton(schools.get(index)));
                    }
                } else {
                    detailPanel.add(getButton(schools.get(0)));
                }
            }
        }

        Player player = getMyPlayer();
        // player can be null when using the map editor
        Europe europe = (player == null) ? null : player.getEurope();

        String price = null;
        if (europe != null && europe.getUnitPrice(type) > 0) {
            price = Integer.toString(europe.getUnitPrice(type));
        } else if (type.getPrice() > 0) {
            price = Integer.toString(type.getPrice());
        }
        if (price != null) {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.price")));
            detailPanel.add(new JLabel(price));
        }

        // Requires - prerequisites to build
        if (!type.getAbilitiesRequired().isEmpty()) {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.requirements")), "top");
            try {
                JTextPane textPane = getDefaultTextPane();
                StyledDocument doc = textPane.getStyledDocument();
                appendRequiredAbilities(doc, type);
                detailPanel.add(textPane);
            } catch(BadLocationException e) {
                logger.warning(e.toString());
            }
        }

        List<Modifier> bonusList = new ArrayList<Modifier>();
        for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
            bonusList.addAll(type.getModifierSet(goodsType.getId()));
        }
        int bonusNumber = bonusList.size();
        if (bonusNumber > 0) {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.productionBonus")),
                            "newline 20, top");
            JPanel productionPanel = new JPanel(new GridLayout(0, MODIFIERS_PER_ROW));
            productionPanel.setOpaque(false);
            for (Modifier productionBonus : bonusList) {
                GoodsType goodsType = getSpecification().getGoodsType(productionBonus.getId());
                String bonus = getModifierAsString(productionBonus);
                productionPanel.add(getGoodsButton(goodsType, bonus));
            }
            detailPanel.add(productionPanel, "growx");
        }

        if (!type.getGoodsRequired().isEmpty()) {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.goodsRequired")),
                            "newline 20");
            AbstractGoods goods = type.getGoodsRequired().get(0);
            if (type.getGoodsRequired().size() > 1) {
                detailPanel.add(getGoodsButton(goods.getType(), goods.getAmount()),
                                "span, split " + type.getGoodsRequired().size());
                for (int index = 1; index < type.getGoodsRequired().size(); index++) {
                    goods = type.getGoodsRequired().get(index);
                    detailPanel.add(getGoodsButton(goods.getType(), goods.getAmount()));
                }
            } else {
                detailPanel.add(getGoodsButton(goods.getType(), goods.getAmount()));
            }
        }

        detailPanel.add(new JLabel(Messages.message("colopedia.unit.description")),
                        "newline 20");
        detailPanel.add(getDefaultTextArea(Messages.message(type.getDescriptionKey()), 20), "growx");

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    /**
     * Builds the details panel for the given goods.
     *
     * @param type The GoodsType
     */
    private void buildGoodsDetail(GoodsType type) {

        detailPanel.setLayout(new MigLayout("wrap 2, fillx, gap 20", "", ""));

        JLabel name = localizedLabel(type.getNameKey());
        name.setFont(smallHeaderFont);
        detailPanel.add(name, "span, align center, wrap 40");

        /**
        detailPanel.add(new JLabel(Messages.message("colopedia.goods.isFarmed")));
        detailPanel.add(new JLabel(Messages.message(type.isFarmed() ? "yes" : "no")));
        **/

        if (type.isFarmed()) {
            List<TileImprovementType> improvements = new ArrayList<TileImprovementType>();
            List<Modifier> modifiers = new ArrayList<Modifier>();
            for (TileImprovementType improvementType :
                     getSpecification().getTileImprovementTypeList()) {
                Modifier productionModifier = improvementType.getProductionModifier(type);
                if (productionModifier != null) {
                    improvements.add(improvementType);
                    modifiers.add(productionModifier);
                }
            }

            detailPanel.add(new JLabel(Messages.message("colopedia.goods.improvedBy")), "top");
            if (improvements.size() == 0) {
                detailPanel.add(new JLabel(none));
            } else if (improvements.size() == 1) {
                detailPanel.add(new JLabel(Messages.message(improvements.get(0).getNameKey()) + " (" +
                                           getModifierAsString(modifiers.get(0)) + ")"));
            } else {
                detailPanel.add(new JLabel(Messages.message(improvements.get(0).getNameKey()) + " (" +
                                           getModifierAsString(modifiers.get(0)) + ")"),
                                "flowy, split " + improvements.size());
                for (int index = 1; index < improvements.size(); index++) {
                    detailPanel.add(new JLabel(Messages.message(improvements.get(index).getNameKey()) + " (" +
                                               getModifierAsString(modifiers.get(index)) + ")"));
                }
            }
        } else {
            detailPanel.add(new JLabel(Messages.message("colopedia.goods.madeFrom")));
            if (type.isRefined()) {
                detailPanel.add(getGoodsButton(type.getRawMaterial()));
            } else {
                detailPanel.add(new JLabel(none));
            }
        }

        detailPanel.add(new JLabel(Messages.message("colopedia.goods.makes")));
        if (type.isRawMaterial()) {
            detailPanel.add(getGoodsButton(type.getProducedMaterial()), "wrap 40");
        } else {
            detailPanel.add(new JLabel(none), "wrap 40");
        }

        detailPanel.add(new JLabel(Messages.message("colopedia.goods.description")));
        detailPanel.add(getDefaultTextArea(Messages.message(type.getDescriptionKey()), 20), "growx");

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    /**
     * Builds the details panel for the given building.
     *
     * @param buildingType The BuildingType
     */
    private void buildBuildingDetail(BuildingType buildingType) {

        detailPanel.setLayout(new MigLayout("wrap 7, fillx, gapx 20", "", ""));

        JLabel name = localizedLabel(buildingType.getNameKey());
        name.setFont(smallHeaderFont);
        detailPanel.add(name, "span, align center, wrap 40");

        // Requires - prerequisites to build
        JTextPane textPane = getDefaultTextPane();
        StyledDocument doc = textPane.getStyledDocument();

        try {
            if (buildingType.getUpgradesFrom() != null) {
                StyleConstants.setComponent(doc.getStyle("button"), getButton(buildingType.getUpgradesFrom()));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                doc.insertString(doc.getLength(), "\n", doc.getStyle("regular"));
            }
            if (buildingType.getPopulationRequired() > 0) {
                StringTemplate template = StringTemplate.template("colonist")
                    .addAmount("%number%", buildingType.getPopulationRequired());
                doc.insertString(doc.getLength(),
                                 Messages.message(template) + "\n",
                                 doc.getStyle("regular"));
            }
            appendRequiredAbilities(doc, buildingType);

            detailPanel.add(new JLabel(Messages.message("colopedia.buildings.requires")), "top");
            detailPanel.add(textPane, "span, growx");
        } catch(BadLocationException e) {
            logger.warning(e.toString());
        }

        // Costs to build - Hammers & Tools
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.cost")));
        if (buildingType.getGoodsRequired().isEmpty()) {
            detailPanel.add(new JLabel(Messages.message("colopedia.buildings.autoBuilt")), "span");
        } else {
            AbstractGoods goodsRequired = buildingType.getGoodsRequired().get(0);
            if (buildingType.getGoodsRequired().size() > 1) {
                detailPanel.add(getGoodsButton(goodsRequired.getType(), goodsRequired.getAmount()),
                                "span, split " + buildingType.getGoodsRequired().size());

                for (int index = 1; index < buildingType.getGoodsRequired().size(); index++) {
                    goodsRequired = buildingType.getGoodsRequired().get(index);
                    detailPanel.add(getGoodsButton(goodsRequired.getType(), goodsRequired.getAmount()));
                }
            } else {
                detailPanel.add(getGoodsButton(goodsRequired.getType(), goodsRequired.getAmount()), "span");
            }
        }

        // Production - Needs & Produces
        if (buildingType.hasAbility("model.ability.teach")) {
            detailPanel.add(new JLabel(Messages.message("colopedia.buildings.teaches")), "newline, top");
            int count = 0;
            for (UnitType unitType2 : getSpecification().getUnitTypeList()) {
                if (buildingType.canAdd(unitType2)) {
                    if (count > 0 && count % 3 == 0) {
                        detailPanel.add(getButton(unitType2), "skip, span 2");
                    } else {
                        detailPanel.add(getButton(unitType2), "span 2");
                    }
                    count++;
                }
            }
        } else {

            GoodsType inputType = buildingType.getConsumedGoodsType();
            GoodsType outputType = buildingType.getProducedGoodsType();

            if (outputType != null) {
                detailPanel.add(new JLabel(Messages.message("colopedia.buildings.production")), "newline");
                if (inputType != null) {
                    detailPanel.add(getGoodsButton(inputType), "span, split 3");
                    JLabel arrow = new JLabel("\u2192");
                    arrow.setFont(arrowFont);
                    detailPanel.add(arrow);
                }
                detailPanel.add(getGoodsButton(outputType));
            }
        }

        int workplaces = buildingType.getWorkPlaces();
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.workplaces")), "newline");
        detailPanel.add(new JLabel(Integer.toString(workplaces)), "span");

        // Specialist
        if (workplaces > 0) {
            detailPanel.add(new JLabel(Messages.message("colopedia.buildings.specialist")), "newline");
            final UnitType unitType = getSpecification()
                .getExpertForProducing(buildingType.getProducedGoodsType());
            if (unitType == null) {
                detailPanel.add(new JLabel(none), "span");
            } else {
                detailPanel.add(getUnitButton(unitType), "span");
            }
        }

        Set<Modifier> bonusList = buildingType.getFeatureContainer().getModifiers();
        List<JComponent> labels = new ArrayList<JComponent>();
        for (Modifier productionBonus : bonusList) {
            try {
                GoodsType goodsType = getSpecification()
                    .getGoodsType(productionBonus.getId());
                String bonus = getModifierAsString(productionBonus);
                labels.add(0, getGoodsButton(goodsType, bonus));
            } catch(Exception e) {
                // not a production bonus
                String bonus = getFeatureName(productionBonus)
                    + ": " + getModifierAsString(productionBonus);
                JLabel label = new JLabel(bonus);
                label.setToolTipText(Messages.message(productionBonus.getId() + ".shortDescription"));
                labels.add(label);
            }
        }

        Set<Ability> abilities = buildingType.getFeatureContainer().getAbilities();
        for (Ability ability : abilities) {
            if (ability.getValue()) {
                JLabel label = new JLabel(getAbilityAsString(ability));
                label.setToolTipText(Messages.message(ability.getId() + ".shortDescription"));
                labels.add(label);
            }
        }

        if (!labels.isEmpty()) {
            detailPanel.add(new JLabel(Messages.message(StringTemplate.template("colopedia.buildings.modifiers")
                                                        .addAmount("%number%", labels.size()))), "newline, top");
            int count = 0;
            for (JComponent component : labels) {
                if (count > 0 && count % 2 == 0) {
                    detailPanel.add(component, "skip, span 3");
                } else {
                    detailPanel.add(component, "span 3");
                }
                count++;
            }
        }

        // Notes
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.notes")), "newline 20, top");
        detailPanel.add(getDefaultTextArea(Messages.message(buildingType.getDescriptionKey()), 20),
                        "span, growx");

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    /**
     * Builds the details panel for the given founding father.
     *
     * @param father - the FoundingFather
     */
    private void buildFatherDetail(FoundingFather father) {

        detailPanel.setLayout(new MigLayout("wrap 2, fillx, gapx 20", "", ""));

        JLabel name = new JLabel(Messages.message(father.getNameKey())
                                 + " (" + Messages.message(father.getTypeKey()) + ")");
        name.setFont(smallHeaderFont);
        detailPanel.add(name, "span, align center, wrap 40");

        Image image = getLibrary().getFoundingFatherImage(father);

        JLabel imageLabel;
        if (image != null) {
            imageLabel = new JLabel(new ImageIcon(image));
        } else {
            imageLabel = new JLabel();
        }
        detailPanel.add(imageLabel, "top");

        String text = Messages.message(father.getDescriptionKey()) + "\n\n" + "["
            + Messages.message(father.getId() + ".birthAndDeath") + "] "
            + Messages.message(father.getId() + ".text");
        JTextArea description = getDefaultTextArea(text, 20);
        detailPanel.add(description, "top, growx");

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    /**
     * Builds the details panel for the given nation.
     *
     * @param nation - the Nation
     */
    private void buildNationDetail(Nation nation) {

        NationType currentNationType = nation.getType();;
        for (Player player : getGame().getPlayers()) {
            if (player.getNation() == nation) {
                currentNationType = player.getNationType();
                break;
            }
        }

        detailPanel.setLayout(new MigLayout("wrap 3, fillx, gapx 20", "", ""));

        JLabel name = localizedLabel(nation.getNameKey());
        name.setFont(smallHeaderFont);
        detailPanel.add(name, "span, align center, wrap 40");

        JLabel artLabel = new JLabel(getLibrary().getMonarchImageIcon(nation));
        detailPanel.add(artLabel, "spany, gap 40, top");

        detailPanel.add(localizedLabel("colopedia.nation.ruler"));
        detailPanel.add(localizedLabel(nation.getRulerNameKey()));

        detailPanel.add(new JLabel(Messages.message("colopedia.nation.defaultAdvantage")));
        detailPanel.add(getButton(nation.getType()));

        detailPanel.add(new JLabel(Messages.message("colopedia.nation.currentAdvantage")));
        detailPanel.add(getButton(currentNationType), "wrap push");

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    /**
     * Builds the details panel for the given nation type.
     *
     * @param nationType - the EuropeanNationType
     */
    private void buildEuropeanNationTypeDetail(EuropeanNationType nationType) {

        Set<Ability> abilities = nationType.getFeatureContainer().getAbilities();
        Set<Modifier> modifiers = nationType.getFeatureContainer().getModifiers();

        detailPanel.setLayout(new MigLayout("wrap 2, gapx 20", "", ""));

        JLabel name = localizedLabel(nationType.getNameKey());
        name.setFont(smallHeaderFont);
        detailPanel.add(name, "span, align center, wrap 40");

        detailPanel.add(new JLabel(Messages.message("colopedia.nationType.units")));
        List<AbstractUnit> startingUnits = nationType.getStartingUnits();
        if (startingUnits.isEmpty()) {
            detailPanel.add(new JLabel());
        } else {
            AbstractUnit startingUnit = startingUnits.get(0);
            if (startingUnits.size() > 1) {
                detailPanel.add(getUnitButton(startingUnit),
                                "span, split " + startingUnits.size());
                for (int index = 1; index < startingUnits.size(); index++) {
                    startingUnit = startingUnits.get(index);
                    detailPanel.add(getUnitButton(startingUnit));
                }
            } else {
                detailPanel.add(getUnitButton(startingUnit));
            }
        }

        if (!abilities.isEmpty()) {
            detailPanel.add(new JLabel(Messages.message("abilities")), "newline 20, span");
            String trueString = Messages.message("true");
            String falseString = Messages.message("false");
            for (Ability ability : abilities) {
                detailPanel.add(new JLabel("* " + getFeatureName(ability)));
                String value = ability.getValue() ? trueString : falseString;
                detailPanel.add(new JLabel(value));
            }
        }

        if (!modifiers.isEmpty()) {
            detailPanel.add(new JLabel(Messages.message("modifiers")), "newline 20, span");
            for (Modifier modifier : modifiers) {
                detailPanel.add(new JLabel("* " + getFeatureName(modifier)));
                detailPanel.add(new JLabel(getModifierAsString(modifier)));
            }
        }

        detailPanel.revalidate();
        detailPanel.repaint();
    }


    /**
     * Builds the details panel for the given nation type.
     *
     * @param nationType - the IndianNationType
     */
    private void buildIndianNationTypeDetail(IndianNationType nationType) {

        List<RandomChoice<UnitType>> skills = nationType.getSkills();

        detailPanel.setLayout(new MigLayout("wrap 2, gapx 20", "", ""));

        JLabel name = localizedLabel(nationType.getNameKey());
        name.setFont(smallHeaderFont);
        detailPanel.add(name, "span, align center, wrap 40");

        detailPanel.add(new JLabel(Messages.message("colopedia.nationType.aggression")));
        detailPanel.add(new JLabel(Messages.message("colopedia.nationType.aggression." +
                                                    nationType.getAggression().toString().toLowerCase())));

        detailPanel.add(new JLabel(Messages.message("colopedia.nationType.numberOfSettlements")));
        detailPanel.add(new JLabel(Messages.message("colopedia.nationType.numberOfSettlements." +
                                                    nationType.getNumberOfSettlements().toString()
                                                    .toLowerCase())));

        detailPanel.add(new JLabel(Messages.message("colopedia.nationType.typeOfSettlements")));
        detailPanel.add(new JLabel(Messages.message(nationType.getCapitalType().getId() + ".name"),
                                   new ImageIcon(getLibrary().getSettlementImage(nationType.getCapitalType())),
                                   SwingConstants.CENTER));

        List<String> regionNames = new ArrayList<String>();
        for (String regionName : nationType.getRegionNames()) {
            regionNames.add(Messages.message(regionName + ".name"));
        }
        detailPanel.add(new JLabel(Messages.message("colopedia.nationType.regions")));
        detailPanel.add(new JLabel(Utils.join(", ", regionNames)));

        detailPanel.add(new JLabel(Messages.message("colopedia.nationType.skills")), "top, newline 20");
        GridLayout gridLayout = new GridLayout(0, 2);
        gridLayout.setHgap(10);
        JPanel unitPanel = new JPanel(gridLayout);
        unitPanel.setOpaque(false);
        for (RandomChoice<UnitType> choice : skills) {
            unitPanel.add(getUnitButton(choice.getObject()));
        }
        detailPanel.add(unitPanel);

        detailPanel.revalidate();
        detailPanel.repaint();
    }


    /**
     * This function analyses a tree selection event and calls the right methods to take care
     * of building the requested unit's details.
     *
     * @param event The incoming TreeSelectionEvent.
     */
    public void valueChanged(TreeSelectionEvent event) {
        if (event.getSource() == tree) {

            DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
            DefaultMutableTreeNode parent = node;

            while (parent != null) {
                ColopediaTreeItem parentItem = (ColopediaTreeItem) parent.getUserObject();
                if (parentItem.getPanelType() == null) {
                    parent = (DefaultMutableTreeNode) parent.getParent();
                } else {
                    ColopediaTreeItem nodeItem = (ColopediaTreeItem) node.getUserObject();
                    selectDetail(parentItem.getPanelType(), nodeItem.getFreeColGameObjectType());
                    break;
                }
            }
        }
    }

    /**
     * Returns a text area with standard settings suitable for use in FreeCol
     * dialogs.
     *
     * @param text The text to display in the text area.
     * @return a text area with standard settings suitable for use in FreeCol
     *         dialogs.
     */
    public static JTextArea getDefaultTextArea(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setOpaque(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFocusable(false);
        textArea.setFont(defaultFont);
        return textArea;
    }

    private String getFeatureName(Feature feature) {
        return Messages.message(feature.getNameKey());
    }

    private String getAbilityAsString(Ability ability) {
        String label = Messages.message(getFeatureName(ability)) + ":";
        if (ability.hasScope()) {
            for (Scope scope : ability.getScopes()) {
                if (scope.getType() != null) {
                    label += (scope.isMatchNegated() ? " !" : " ")
                        + Messages.message(scope.getType() + ".name") + ",";
                } else if (scope.getAbilityID() != null) {
                    label += (scope.isMatchNegated() ? " !" : " ")
                        + Messages.message(scope.getAbilityID() + ".name") + ",";
                }
            }
        }
        return label.substring(0, label.length() - 1);
    }

    public String getModifierAsString(Modifier modifier) {
        String bonus = modifierFormat.format(modifier.getValue());
        switch(modifier.getType()) {
        case ADDITIVE:
            if (modifier.getValue() > 0) {
                bonus = "+" + bonus;
            }
            break;
        case PERCENTAGE:
            if (modifier.getValue() > 0) {
                bonus = "+" + bonus;
            }
            bonus = bonus + "%";
            break;
        case MULTIPLICATIVE:
            bonus = "\u00D7" + bonus;
            break;
        default:
        }
        return bonus;
    }

    public void appendRequiredAbilities(StyledDocument doc, BuildableType buildableType)
        throws BadLocationException {
        for (Entry<String, Boolean> entry : buildableType.getAbilitiesRequired().entrySet()) {
            doc.insertString(doc.getLength(),
                             Messages.message(entry.getKey() + ".name"),
                             doc.getStyle("regular"));
            List<JButton> requiredTypes = new ArrayList<JButton>();
            for (FreeColGameObjectType type : getSpecification()
                     .getTypesProviding(entry.getKey(), entry.getValue())) {
                JButton typeButton = getButton(type);
                typeButton.addActionListener(this);
                requiredTypes.add(typeButton);
            }
            if (!requiredTypes.isEmpty()) {
                doc.insertString(doc.getLength(), " (", doc.getStyle("regular"));
                StyleConstants.setComponent(doc.getStyle("button"), requiredTypes.get(0));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                for (int index = 1; index < requiredTypes.size(); index++) {
                    JButton button = requiredTypes.get(index);
                    doc.insertString(doc.getLength(), " / ", doc.getStyle("regular"));
                    StyleConstants.setComponent(doc.getStyle("button"), button);
                    doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                }
                doc.insertString(doc.getLength(), ")", doc.getStyle("regular"));
            }
            doc.insertString(doc.getLength(), "\n", doc.getStyle("regular"));
        }
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     *
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            getCanvas().remove(this);
        } else {
            DefaultMutableTreeNode node = nodeMap.get(command);
            tree.collapsePath(tree.getSelectionPath().getParentPath());
            tree.scrollPathToVisible(new TreePath(node.getPath()));
            initialize(getSpecification().getType(command));
        }
    }


}
