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

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;


/**
 * This panel displays the Colony Report.
 */
public final class ReportColonyPanel extends ReportPanel
    implements ActionListener {

    private static final int COLONISTS_PER_ROW = 20;
    private static final int UNITS_PER_ROW = 14;
    private static final int GOODS_PER_ROW = 10;
    private static final int BUILDINGS_PER_ROW = 8;

    private static final Comparator<GoodsType> goodsComparator
        = new Comparator<GoodsType>() {
            private int rank(GoodsType g) {
                return (!g.isStorable() || g.isTradeGoods()) ? -1
                    : (g.isFoodType()) ? 1
                    : (g.isNewWorldGoodsType()) ? 2
                    : (g.isFarmed()) ? 3
                    : (g.isRawMaterial()) ? 4
                    : (g.isNewWorldLuxuryType()) ? 5
                    : (g.isRefined()) ? 6
                    : -1;
            }

            public int compare(GoodsType g1, GoodsType g2) {
                int r1 = rank(g1);
                int r2 = rank(g2);
                return (r1 != r2) ? r1 - r2
                : g1.getNameKey().compareTo(g2.getNameKey());
            }
        };

    private static final Comparator<AbstractGoods> abstractGoodsComparator
        = new Comparator<AbstractGoods>() {
            public int compare(AbstractGoods a1, AbstractGoods a2) {
                int cmp = a2.getAmount() - a1.getAmount();
                return (cmp != 0) ? cmp
                    : goodsComparator.compare(a2.getType(), a1.getType());
            }
        };

    private static final String BUILDQUEUE = "buildQueue.";
    private boolean useCompact = false;

    private List<Colony> colonies;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     */
    public ReportColonyPanel(Canvas parent) {
        super(parent, Messages.message("reportColonyAction.name"));
        ClientOptions options = getFreeColClient().getClientOptions();
        colonies = options.getSortedColonies(getMyPlayer());

        try {
            useCompact = options.getBoolean(ClientOptions.COMPACT_COLONY_REPORT);
        } catch (Exception e) {
            useCompact = false;
        }
        if (useCompact) {
            conciseColonyPanel(colonies);
        } else {
            prettyColonyPanel(colonies);
        }
    }

    private void prettyColonyPanel(List<Colony> colonies) {
        // Display Panel
        reportPanel.setLayout(new MigLayout("fill"));

        for (Colony colony : colonies) {

            // Name
            JButton button = getLinkButton(colony.getName(), null,
                colony.getId());
            button.addActionListener(this);
            reportPanel.add(button, "newline 20, split 2");
            reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

            // Currently building
            BuildableType currentType = colony.getCurrentlyBuilding();
            JLabel buildableLabel = null;
            if (currentType != null) {
                buildableLabel = new JLabel(new ImageIcon(ResourceManager.getImage(currentType.getId()
                                                          + ".image", 0.66)));
                buildableLabel.setToolTipText(Messages.message(StringTemplate.template("colonyPanel.currentlyBuilding")
                                                               .add("%buildable%", currentType.getNameKey())));
                buildableLabel.setIcon(buildableLabel.getDisabledIcon());
            }

            // Units
            JPanel colonistsPanel = new JPanel(new GridLayout(0, COLONISTS_PER_ROW));
            List<Unit> unitList = colony.getUnitList();
            Collections.sort(unitList, getUnitTypeComparator());
            for (Unit unit : unitList) {
                UnitLabel unitLabel = new UnitLabel(unit, getCanvas(), true, true);
                colonistsPanel.add(unitLabel);
            }
            JPanel unitsPanel = new JPanel(new GridLayout(0, UNITS_PER_ROW));
            unitList = colony.getTile().getUnitList();
            Collections.sort(unitList, getUnitTypeComparator());
            for (Unit unit : unitList) {
                UnitLabel unitLabel = new UnitLabel(unit, getCanvas(), true, true);
                unitsPanel.add(unitLabel);
            }
            if(buildableLabel != null && currentType.getSpecification().getUnitTypeList().contains(currentType)) {
                unitsPanel.add(buildableLabel);
            }
            reportPanel.add(colonistsPanel, "newline, growx");
            reportPanel.add(unitsPanel, "newline, growx");

            // Production
            GoodsType horses = getSpecification().getGoodsType("model.goods.horses");
            int count = 0;
            for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
                int newValue = colony.getNetProductionOf(goodsType);
                int stockValue = colony.getGoodsCount(goodsType);
                if (newValue != 0 || stockValue > 0) {
                    Building building = colony.getBuildingForProducing(goodsType);
                    ProductionLabel productionLabel = new ProductionLabel(goodsType, newValue, getCanvas());
                    if (building != null) {
                        productionLabel.setMaximumProduction(building.getMaximumProduction());
                    }
                    if (goodsType == horses) {
                        // horse images don't stack well
                        productionLabel.setMaxGoodsIcons(1);
                    }
                    // Show stored items in ReportColonyPanel
                    productionLabel.setStockNumber(stockValue);
                    if (count % GOODS_PER_ROW == 0) {
                        reportPanel.add(productionLabel, "newline, split " + GOODS_PER_ROW);
                    } else {
                        reportPanel.add(productionLabel);
                    }
                    count++;
                }
            }

            // Buildings
            JPanel buildingsPanel = new JPanel(new GridLayout(0, BUILDINGS_PER_ROW));
            List<Building> buildingList = colony.getBuildings();
            Collections.sort(buildingList);
            for (Building building : buildingList) {
                if(building.getType().isAutomaticBuild()) {
                    continue;
                }
                
                JLabel buildingLabel =
                    new JLabel(new ImageIcon(ResourceManager.getImage(building.getType().getId()
                                                                      + ".image", 0.66)));
                buildingLabel.setToolTipText(Messages.message(building.getNameKey()));
                buildingsPanel.add(buildingLabel);
            }
            if(buildableLabel != null && currentType.getSpecification().getBuildingTypeList().contains(currentType)) {
                buildingsPanel.add(buildableLabel);
            }
            reportPanel.add(buildingsPanel, "newline, growx");
        }
    }

    /**
     * Implement the action listener, hooking BUILDQUEUE events, but
     * otherwise delegating to the ReportPanel handler.
     *
     * @param event The incoming event.
     */
    public void actionPerformed(ActionEvent event) {
        if (useCompact) {
            String command = event.getActionCommand();
            if (command.startsWith(BUILDQUEUE)) {
                command = command.substring(BUILDQUEUE.length());
                FreeColGameObject fcgo
                    = getGame().getFreeColGameObject(command);
                if (fcgo instanceof Colony) {
                    Canvas canvas = getCanvas();
                    canvas.showSubPanel(new BuildQueuePanel((Colony) fcgo,
                            canvas));
                }
                return;
            }
        }
        super.actionPerformed(event);
    }

    private void conciseColonyPanel(List<Colony> colonies) {
        final Specification spec = getSpecification();
        final GoodsType foodType = spec.getPrimaryFoodType();
        final UnitType colonistType
            = spec.getUnitType("model.unit.freeColonist");
        final ImageLibrary lib = getCanvas().getImageLibrary();
        List<GoodsType> goodsTypes
            = new ArrayList<GoodsType>(spec.getGoodsTypeList());
        Collections.sort(goodsTypes, goodsComparator);
        while (!goodsTypes.get(0).isStorable()
            || goodsTypes.get(0).isTradeGoods()) {
            goodsTypes.remove(0);
        }

        // Define the layout, with a column for each goods type.
        String cols = "[l][c][c][c][c]";
        for (int i = 0; i < goodsTypes.size(); i++) cols += "[c, 25!]";
        cols += "[l][l][l]";
        reportPanel.setLayout(new MigLayout("", cols, ""));

        conciseHeaders(goodsTypes, true);

        // For all colonies...
        for (Colony colony : colonies) {
            int pop = colony.getUnitCount();
            int bonus = colony.getProductionBonus();
            List<Tile> exploreTiles = new ArrayList<Tile>();
            List<Tile> clearTiles = new ArrayList<Tile>();
            List<Tile> plowTiles = new ArrayList<Tile>();
            List<Tile> roadTiles = new ArrayList<Tile>();
            colony.getColonyTileTodo(exploreTiles, clearTiles, plowTiles,
                roadTiles);
            boolean plowMe = plowTiles.size() > 0
                && plowTiles.get(0) == colony.getTile();
            int newFood = colony.getAdjustedNetProductionOf(foodType);
            boolean famine = newFood < 0
                && colony.getGoodsCount(foodType) / -newFood <= 3;
            int newColonist = (newFood <= 0) ? -1
                : (Settlement.FOOD_PER_COLONIST + 1
                    - colony.getGoodsCount(foodType)) / newFood;
            JLabel l;

            // Field: A button for the colony.
            // Colour: The production bonus colour.
            // Font: Bold if famine is threatening.
            JButton b = colourButton(colony.getName(), colony.getId(),
                (bonus == 0) ? Color.BLACK
                : ResourceManager.getProductionColor(bonus));
            if (famine) {
                b.setFont(b.getFont().deriveFont(Font.BOLD));
            }
            reportPanel.add(b, "newline");

            // Field: The number of colonists that can be added to a
            // colony without damaging the production bonus, unless
            // the colony is inefficient in which case add the number
            // of colonists to remove to fix the inefficiency.
            // Colour: Blue if efficient/Red if inefficient.
            int i;
            if (bonus < 0) {
                for (i = 1; i < pop; i++) {
                    if (colony.governmentChange(pop - i) == 1) break;
                }
                l = newLabel(Integer.toString(i), null, Color.RED,
                    "report.colony.grow.description");
            } else {
                for (i = 1; i < pop; i++) {
                    if (colony.governmentChange(pop + i) == -1) break;
                }
                i--;
                l = (i > 0)
                    ? newLabel(Integer.toString(i), null, Color.BLUE,
                        "report.colony.grow.description")
                    : new JLabel("");
            }
            reportPanel.add(l);

            // Field: The number of potential colony tiles that need
            // exploring.
            // Colour: Always Red.
            reportPanel.add((exploreTiles.size() > 0)
                ? newLabel(Integer.toString(exploreTiles.size()), null,
                    Color.RED, "report.colony.explore.description")
                : new JLabel(""));

            // Field: The number of existing colony tiles that would
            // benefit from ploughing.
            // Colour: Blue, unless one of the tiles is the colony center.
            reportPanel.add((plowTiles.size() > 0)
                ? newLabel(Integer.toString(plowTiles.size()), null,
                    (plowMe) ? Color.RED : Color.BLUE,
                    "report.colony.plow.description")
                : new JLabel(""));

            // Field: The number of existing colony tiles that would
            // benefit from a road.
            // Colour: Blue.
            reportPanel.add((roadTiles.size() > 0)
                ? newLabel(Integer.toString(roadTiles.size()), null,
                    Color.BLUE, "report.colony.road.description")
                : new JLabel(""));

            // Fields: The net production of each storable+non-trade-goods
            // goods type.
            // Colour: Red if too low, Orange if negative, empty if no
            // production, otherwise must be positive, wherein Green
            // if exported, Red if too high, else Blue.
            final int adjustment = colony.getWarehouseCapacity()
                / GoodsContainer.CARGO_SIZE;
            for (GoodsType g : goodsTypes) {
                int p = colony.getAdjustedNetProductionOf(g);
                ExportData exportData = colony.getExportData(g);
                int low = exportData.getLowLevel() * adjustment;
                int high = exportData.getHighLevel() * adjustment;
                int amount = colony.getGoodsCount(g);
                Color c;
                String tip;
                if (p < 0) {
                    if (amount < low) {
                        int turns = amount / p + 1;
                        c = Color.RED;
                        tip = Messages.message(StringTemplate
                            .template("report.colony.production.low.description")
                                .add("%goods%", g.getNameKey())
                                .addAmount("%amount%", p)
                                .addAmount("%turns%", turns));
                    } else {
                        c = Color.ORANGE;
                        tip = Messages.message(StringTemplate
                            .template("report.colony.production.description")
                                .add("%goods%", g.getNameKey())
                                .addAmount("%amount%", p));
                    }
                } else if (p == 0) {
                    c = null;
                    tip = null;
                } else if (exportData.isExported()) {
                    c = Color.GREEN;
                    tip = Messages.message(StringTemplate
                        .template("report.colony.production.export.description")
                            .add("%goods%", g.getNameKey())
                            .addAmount("%amount%", p)
                            .addAmount("%export%", exportData.getExportLevel()));
                } else if (g != foodType && amount > high) {
                    int turns = 1 + (colony.getWarehouseCapacity() - amount)
                        / p;
                    c = Color.RED;
                    tip = Messages.message(StringTemplate
                        .template("report.colony.production.high.description")
                            .add("%goods%", g.getNameKey())
                            .addAmount("%amount%", p)
                            .addAmount("%turns%", turns));
                } else {
                    c = Color.BLUE;
                    tip = Messages.message(StringTemplate
                        .template("report.colony.production.description")
                            .add("%goods%", g.getNameKey())
                            .addAmount("%amount%", p));
                }
                l = (c == null) ? new JLabel("")
                    : newLabel(Integer.toString(p), null, c, tip);
                reportPanel.add(l);
            }

            // Collect the types of the units at work in the colony
            // (colony tiles and buildings) that are suboptimal (and
            // are not just temporarily there because they are being
            // taught), and the types for sites that really need a new
            // unit.
            List<AbstractUnit> improve = new ArrayList<AbstractUnit>();
            List<AbstractUnit> want = new ArrayList<AbstractUnit>();
            for (ColonyTile ct : colony.getColonyTiles()) {
                if (ct.isColonyCenterTile() || !ct.canBeWorked()) continue;
                Unit u = ct.getUnit();
                GoodsType work;
                if (u == null) {
                    work = bestProduction(ct.getWorkTile(), colonistType);
                    if (work == null) continue;
                    UnitType expert = spec.getExpertForProducing(work);
                    if (expert != null) {
                        addAbstractUnit(want, expert);
                    }
                } else {
                    work = u.getWorkType();
                    if (work == null) continue;
                    UnitType expert = spec.getExpertForProducing(work);
                    if (expert != null && expert != u.getType()
                        && u.getTeacher() == null) {
                        addAbstractUnit(improve, expert);
                    }
                }
            }
            Building school = null; // Also collect the school building.
            for (Building building : colony.getBuildings()) {
                if (building.canTeach()) school = building;
                GoodsType work = building.getGoodsOutputType();
                UnitType expert = building.getExpertUnitType();
                if (work == null || expert == null
                    || (work.isLibertyType() && colony.getSoL() >= 100)) {
                    continue; // Ignore liberty if SoL is maximum
                }
                if (building.getUnitList().isEmpty()) {
                    GoodsType inputType = building.getGoodsInputType();
                    // Liberty and hammers are always wanted.
                    // If a building is upgraded, assume we want to use it.
                    if (work.isLibertyType()
                        || "model.goods.hammers".equals(work.getId())
                        //|| (inputType != null
                        //    && colony.getProductionOf(inputType) > 5)
                        || building.getLevel() > 1) {
                        addAbstractUnit(want, expert);
                    }
                } else {
                    for (Unit u : building.getUnitList()) {
                        if (u.getType() != expert && u.getTeacher() == null) {
                            addAbstractUnit(improve, expert);
                        }
                    }
                }
            }
            // Collect the teachers and sort by completion proximity.
            List<Unit> teachers = new ArrayList<Unit>();
            if (school != null) {
                teachers.addAll(school.getUnitList());
                Collections.sort(teachers, new Comparator<Unit>() {
                        public int compare(Unit u1, Unit u2) {
                            int l1 = u1.getNeededTurnsOfTraining()
                                - u1.getTurnsOfTraining();
                            int l2 = u2.getNeededTurnsOfTraining()
                                - u2.getTurnsOfTraining();
                            return l1 - l2;
                        }
                    });
            }
            // Make a list of unit types that are not working at their
            // speciality, including the units just standing around.
            List<UnitType> types = new ArrayList<UnitType>();
            List<Unit> available = colony.getUnitList();
            available.addAll(colony.getTile().getUnitList());
            for (Unit u : available) {
                GoodsType t = u.getWorkType();
                GoodsType w;
                Location wl = u.getWorkLocation2();
                if (wl instanceof ColonyTile) {
                    w = bestProduction(((ColonyTile) wl).getWorkTile(),
                                       colonistType);
                } else if (wl instanceof Building) {
                    w = ((Building) wl).getGoodsOutputType();
                } else w = null;
                if (w != t) types.add(u.getType());
            }

            // Field: What is currently being built (clickable if on the
            // buildqueue) and the turns until it completes, including
            // units being taught and new colonists.
            // Colour: Red bold Nothing if nothing being built, Red
            // with no turns if no production, Blue with turns if
            // completing, Red with turns if will block, turns
            // indicates when blocking occurs.
            BuildableType build = colony.getCurrentlyBuilding();
            int fields = 1 + teachers.size()
                + ((newColonist < 0) ? 0 : 1);
            String layout = (fields > 1) ? "split " + fields : null;
            if (build == null) {
                l = newLabel("nothing", null, Color.RED,
                    "report.colony.making.noconstruction.description");
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                reportPanel.add(l, layout);
            } else {
                int turns = colony.getTurnsToComplete(build);
                String name = Messages.message(build.getNameKey());
                String action = BUILDQUEUE + colony.getId();
                if (turns == FreeColObject.UNDEFINED) {
                    b = colourButton(name, action, Color.RED);
                    b.setToolTipText(Messages.message("report.colony.making.noconstruction.description"));
                } else if (turns >= 0) {
                    turns++;
                    name += " " + Integer.toString(turns);
                    b = colourButton(name, action, Color.BLUE);
                    b.setToolTipText(Messages.message(StringTemplate
                        .template("report.colony.making.constructing.description")
                            .add("%buildable%", build.getNameKey())
                            .addAmount("%turns%", turns)));
                } else if (turns < 0) {
                    turns = -turns;
                    name += " " + Integer.toString(turns);
                    b = colourButton(name, action, Color.RED);
                    b.setToolTipText(Messages.message(StringTemplate
                        .template("report.colony.making.block.description")
                            .add("%buildable%", build.getNameKey())
                            .addAmount("%turns%", turns)));
                }
                reportPanel.add(b, layout);
            }
            for (Unit u : teachers) {
                int left = u.getNeededTurnsOfTraining()
                    - u.getTurnsOfTraining();
                JLabel teach;
                if (left <= 0) {
                    teach = newLabel(Integer.toString(0),
                        lib.getUnitImageIcon(u.getType(), Role.DEFAULT,
                            true, 0.333), Color.RED,
                        Messages.message(StringTemplate
                            .template("report.colony.making.noteach.description")
                            .addStringTemplate("%teacher%", u.getLabel())));
                } else {
                    teach = newLabel(Integer.toString(left),
                        lib.getUnitImageIcon(u.getType(), Role.DEFAULT,
                            true, 0.333), Color.BLACK,
                        Messages.message(StringTemplate
                            .template("report.colony.making.educating.description")
                            .addStringTemplate("%teacher%", u.getLabel())
                            .addAmount("%turns%", left)));
                }
                reportPanel.add(teach);
            }
            if (newColonist >= 0) {
                newColonist++;
                JLabel newC = newLabel(Integer.toString(newColonist),
                    lib.getUnitImageIcon(colonistType, Role.DEFAULT,
                        true, 0.333),
                    null, Messages.message(StringTemplate
                        .template("report.colony.making.birth.description")
                        .add("%unit%", colonistType.getNameKey())
                        .addAmount("%turns%", newColonist)));
                reportPanel.add(newC);
            }
            if (fields <= 0) {
                reportPanel.add(new JLabel(""));
            }

            // Field: The units that could be upgraded.
            if (!improve.isEmpty()) {
                addUnitTypes(improve, types);
            } else {
                reportPanel.add(new JLabel(""));
            }

            // Field: The units the colony could make good use of.
            if (!want.isEmpty()) {
                addUnitTypes(want, types);
            } else {
                reportPanel.add(new JLabel(""));
            }
        }

        conciseHeaders(goodsTypes, false);
    }

    private void addAbstractUnit(List<AbstractUnit> units, UnitType type) {
        for (AbstractUnit a : units) {
            if (type.getId().equals(a.getId())) {
                a.setNumber(a.getNumber() + 1);
                return;
            }
        }
        units.add(new AbstractUnit(type.getId(), Role.DEFAULT, 1));
    }

    private void conciseHeaders(List<GoodsType> goodsTypes, boolean top) {
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL),
            "newline, span, growx");

        reportPanel.add(newLabel("report.colony.name.header", null, null,
                                 "report.colony.name.description"), "newline");
        reportPanel.add(newLabel("report.colony.grow.header", null, null,
                                 "report.colony.grow.description"));
        reportPanel.add(newLabel("report.colony.explore.header", null, null,
                                 "report.colony.explore.description"));
        reportPanel.add(newLabel("report.colony.plow.header", null, null,
                                 "report.colony.plow.description"));
        reportPanel.add(newLabel("report.colony.road.header", null, null,
                                 "report.colony.road.description"));
        for (GoodsType g : goodsTypes) {
            ImageIcon ii = getCanvas().getImageLibrary()
                .getScaledGoodsImageIcon(g, 0.667);
            reportPanel.add(newLabel(null, ii, null,
                    Messages.message(StringTemplate
                        .template("report.colony.production.header")
                        .add("%goods%", g.getNameKey()))));
        }
        reportPanel.add(newLabel("report.colony.making.header", null, null,
                                 "report.colony.making.description"));
        reportPanel.add(newLabel("report.colony.improve.header", null, null,
                                 "report.colony.improve.description"));
        reportPanel.add(newLabel("report.colony.wanted.header", null, null,
                                 "report.colony.wanted.description"));

        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL),
            "newline, span, growx");
    }

    private JLabel newLabel(String h, ImageIcon i, Color c, String t) {
        if (h != null) h = Messages.message(h);
        JLabel l = new JLabel(h, i, SwingConstants.CENTER);
        l.setForeground((c == null) ? Color.BLACK : c);
        if (t != null) {
            if (Messages.containsKey(t)) t = Messages.message(t);
            l.setToolTipText(t);
        }
        return l;
    }

    private JButton colourButton(String name, String action, Color c) {
        JButton b = getLinkButton("<html><u>" + name + "</u></html>",
            null, action);
        b.setForeground(c);
        b.addActionListener(this);
        return b;
    }

    private void addUnitTypes(List<AbstractUnit> types, List<UnitType> have) {
        final ImageLibrary lib = getCanvas().getImageLibrary();
        final Specification spec = getSpecification();
        Collections.sort(types, new Comparator<AbstractUnit>() {
                public int compare(AbstractUnit a1, AbstractUnit a2) {
                    int cmp = a2.getNumber() - a1.getNumber();
                    return (cmp != 0) ? cmp : a2.getId().compareTo(a1.getId());
                }
            });

        String layout = (types.size() <= 1) ? null
            : "split " + Integer.toString(types.size());
        for (AbstractUnit a : types) {
            int n = a.getNumber();
            UnitType t = a.getUnitType(spec);
            boolean gray = false;
            if (have.contains(t)) {
                for (int j = 0; j < n; j++) {
                    if (have.contains(t)) {
                        have.remove(t);
                    } else break;
                }
                gray = true;
            }
            String label = (n > 1) ? "x" + Integer.toString(n) : null;
            ImageIcon ii = lib.getUnitImageIcon(t, Role.DEFAULT, gray, 0.333);
            String tip = Messages.message(a.getLabel(spec));
            if (layout != null) {
                reportPanel.add(newLabel(label, ii, null, tip), layout);
                layout = null;
            } else {
                reportPanel.add(newLabel(label, ii, null, tip));
            }
        }
    }

    private GoodsType bestProduction(Tile tile, UnitType type) {
        final Specification spec = getSpecification();
        List<AbstractGoods> prod = new ArrayList<AbstractGoods>();
        for (GoodsType g : spec.getGoodsTypeList()) {
            int amount = tile.getMaximumPotential(g, type);
            if (amount > 0) prod.add(new AbstractGoods(g, amount));
        }
        if (prod.isEmpty()) return null;
        Collections.sort(prod, abstractGoodsComparator);
        return prod.get(0).getType();
    }
}
