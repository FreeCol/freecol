/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * This panel displays the Colony Report.
 */
public final class ReportColonyPanel extends ReportPanel
    implements ActionListener {

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

    private static final Comparator<Unit> teacherComparator
        = new Comparator<Unit>() {
        public int compare(Unit u1, Unit u2) {
            int l1 = u1.getNeededTurnsOfTraining() - u1.getTurnsOfTraining();
            int l2 = u2.getNeededTurnsOfTraining() - u2.getTurnsOfTraining();
            int cmp = l1 - l2;
            return (cmp != 0) ? cmp
                : u2.getType().getId().compareTo(u1.getType().getId());
        }
    };

    private static final String BUILDQUEUE = "buildQueue.";
    private boolean useCompact = false;

    private List<Colony> colonies;
    private List<GoodsType> goodsTypes;

    // Customized colours.
    private Color cAlarm;
    private Color cWarn;
    private Color cPlain;
    private Color cExport;
    private Color cGood;


    /**
     * Creates a colony report.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param gui The <code>GUI</code> to display on.
     */
    public ReportColonyPanel(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, Messages.message("reportColonyAction.name"));
        colonies = getSortedColonies();

        try {
            useCompact = getClientOptions().getInteger(ClientOptions.COLONY_REPORT)
                == ClientOptions.COLONY_REPORT_COMPACT;
        } catch (Exception e) {
            useCompact = false;
        }
        if (useCompact) {
            initializeCompactColonyPanel();
            updateCompactColonyPanel();
        } else {
            classicColonyPanel(colonies);
        }
    }


    // Standard pretty version
    private void classicColonyPanel(List<Colony> colonies) {
        final int COLONISTS_PER_ROW = 20;
        final int UNITS_PER_ROW = 14;
        final int GOODS_PER_ROW = 10;
        final int BUILDINGS_PER_ROW = 8;

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
            colonistsPanel.setOpaque(false);
            List<Unit> unitList = colony.getUnitList();
            Collections.sort(unitList, getUnitTypeComparator());
            for (Unit unit : unitList) {
                UnitLabel unitLabel = new UnitLabel(getFreeColClient(), unit, getGUI(), true, true);
                colonistsPanel.add(unitLabel);
            }
            JPanel unitsPanel = new JPanel(new GridLayout(0, UNITS_PER_ROW));
            unitsPanel.setOpaque(false);
            unitList = colony.getTile().getUnitList();
            Collections.sort(unitList, getUnitTypeComparator());
            for (Unit unit : unitList) {
                UnitLabel unitLabel = new UnitLabel(getFreeColClient(), unit, getGUI(), true, true);
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
                    int maxProduction = 0;
                    for (Building building : colony.getBuildingsForProducing(goodsType)) {
                        maxProduction += building.getMaximumProductionOf(goodsType);
                    }
                    ProductionLabel productionLabel = new ProductionLabel(getFreeColClient(), getGUI(), goodsType, newValue);
                    if (maxProduction > 0) {
                        productionLabel.setMaximumProduction(maxProduction);
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
            buildingsPanel.setOpaque(false);
            List<Building> buildingList = colony.getBuildings();
            Collections.sort(buildingList);
            for (Building building : buildingList) {
                if(building.getType().isAutomaticBuild()) {
                    continue;
                }

                JLabel buildingLabel =
                    new JLabel(new ImageIcon(getGUI().getImageLibrary().
                                             getBuildingImage(building, 0.66)));
                buildingLabel.setToolTipText(Messages.message(building.getNameKey()));
                buildingsPanel.add(buildingLabel);
            }
            if(buildableLabel != null && currentType.getSpecification().getBuildingTypeList().contains(currentType)) {
                buildingsPanel.add(buildableLabel);
            }
            reportPanel.add(buildingsPanel, "newline, growx");
        }
    }


    // Compact version

    private void initializeCompactColonyPanel() {
        Specification spec = getSpecification();
        goodsTypes = new ArrayList<GoodsType>(spec.getGoodsTypeList());
        Collections.sort(goodsTypes, goodsComparator);
        while (!goodsTypes.get(0).isStorable()
            || goodsTypes.get(0).isTradeGoods()) {
            goodsTypes.remove(0);
        }

        // Define the layout, with a column for each goods type.
        String cols = "[l][c][c][c]";
        for (int i = 0; i < goodsTypes.size(); i++) cols += "[c]";
        cols += "[c][c][l][l][l]";
        reportPanel.setLayout(new MigLayout("fillx, insets 0, gap 0 0",
                cols, ""));

        // Load the customized colours, with simple fallbacks.
        cAlarm = ResourceManager.getColor("report.colony.alarmColor");
        cWarn = ResourceManager.getColor("report.colony.warningColor");
        cPlain = ResourceManager.getColor("report.colony.plainColor");
        cExport = ResourceManager.getColor("report.colony.exportColor");
        cGood = ResourceManager.getColor("report.colony.goodColor");
        if (cAlarm == null) cAlarm = Color.RED;
        if (cWarn == null) cWarn = Color.MAGENTA;
        if (cPlain == null) cPlain = Color.DARK_GRAY;
        if (cExport == null) cExport = Color.GREEN;
        if (cGood == null) cGood = Color.BLUE;
    }

    /**
     * Implement the action listener, checking for BUILDQUEUE events,
     * generally displaying the colony panel if given a colony id, but
     * otherwise delegating to the ReportPanel handler.
     *
     * @param event The incoming event.
     */
    public void actionPerformed(ActionEvent event) {
        if (useCompact) {
            String command = event.getActionCommand();
            if (command.startsWith(BUILDQUEUE)) {
                command = command.substring(BUILDQUEUE.length());
                Colony colony = getGame().getFreeColGameObject(command, Colony.class);
                if (colony != null) {
                    getGUI().showBuildQueuePanel(colony, new Runnable() {
                        public void run() {
                            updateCompactColonyPanel();
                        }
                    });
                    return;
                }
            } else {
                Colony colony = getGame().getFreeColGameObject(command, Colony.class);
                if (colony != null) {
                    getGUI().showColonyPanel(colony, new Runnable() {
                        public void run() {
                            updateCompactColonyPanel();
                        }
                    });
                    return;
                }
            }
        }
        super.actionPerformed(event);
    }

    // Work done by (optional) oldType would be better done by newType
    // because it could produce amount more goodsType.
    private class Suggestion {
        public UnitType oldType;
        public UnitType newType;
        public GoodsType goodsType;
        public int amount;

        public Suggestion(UnitType oldType, UnitType newType,
                          GoodsType goodsType, int amount) {
            this.oldType = oldType;
            this.newType = newType;
            this.goodsType = goodsType;
            this.amount = amount;
        }
    };

    private void updateCompactColonyPanel() {
        reportPanel.removeAll();

        Market market = getMyPlayer().getMarket();
        conciseHeaders(goodsTypes, true, market);

        for (Colony colony : colonies) {
            // Do not include colonies that have been abandoned but are
            // still on the colonies list.
            if (colony.getUnitCount() > 0) {
                updateColony(colony);
            }
        }

        conciseHeaders(goodsTypes, false, market);
    }

    private void updateColony(Colony colony) {
        final Specification spec = getSpecification();
        final GoodsType foodType = spec.getPrimaryFoodType();
        final UnitType colonistType = spec.getDefaultUnitType();
        final ImageLibrary lib = getGUI().getImageLibrary();

        // Assemble the fundamental facts about this colony
        final String cac = colony.getId();
        List<Tile> exploreTiles = new ArrayList<Tile>();
        List<Tile> clearTiles = new ArrayList<Tile>();
        List<Tile> plowTiles = new ArrayList<Tile>();
        List<Tile> roadTiles = new ArrayList<Tile>();
        colony.getColonyTileTodo(exploreTiles, clearTiles, plowTiles,
            roadTiles);
        boolean plowMe = plowTiles.size() > 0
            && plowTiles.get(0) == colony.getTile();
        int newColonist;
        boolean famine;
        if (colony.getGoodsCount(foodType) > Settlement.FOOD_PER_COLONIST) {
            famine = false;
            newColonist = 1;
        } else {
            int newFood = colony.getAdjustedNetProductionOf(foodType);
            famine = newFood < 0
                && (colony.getGoodsCount(foodType) / -newFood) <= 3;
            newColonist = (newFood == 0) ? 0
                : (newFood < 0) ? colony.getGoodsCount(foodType) / newFood - 1
                : (Settlement.FOOD_PER_COLONIST
                    - colony.getGoodsCount(foodType)) / newFood + 1;
        }
        int grow = colony.getPreferredSizeChange();
        int bonus = colony.getProductionBonus();

        // Field: A button for the colony.
        // Colour: bonus in {-2,2} => {alarm, warn, plain, export, good}
        // Font: Bold if famine is threatening.
        JButton b = colourButton(cac, colony.getName(), null,
            (bonus <= -2) ? cAlarm
            : (bonus == -1) ? cWarn
            : (bonus == 0) ? cPlain
            : (bonus == 1) ? cExport
            : cGood,
            null);
        if (famine) {
            b.setFont(b.getFont().deriveFont(Font.BOLD));
        }
        reportPanel.add(b, "newline");

        // Field: The number of colonists that can be added to a
        // colony without damaging the production bonus, unless
        // the colony is inefficient in which case add the number
        // of colonists to remove to fix the inefficiency.
        // Colour: Blue if efficient/Red if inefficient.
        if (grow < 0) {
            b = colourButton(cac, Integer.toString(-grow), null, cAlarm,
                stpl("report.colony.shrinking.description")
                    .addName("%colony%", colony.getName())
                    .addAmount("%amount%", -grow));
            reportPanel.add(b);
        } else if (grow > 0) {
            b = colourButton(cac, Integer.toString(grow), null, cGood,
                stpl("report.colony.growing.description")
                    .addName("%colony%", colony.getName())
                    .addAmount("%amount%", grow));
            reportPanel.add(b);
        } else {
            reportPanel.add(new JLabel(""));
        }

        // Field: The number of potential colony tiles that need
        // exploring.
        // Colour: Always cAlarm
        if (exploreTiles.size() > 0) {
            b = colourButton(cac, Integer.toString(exploreTiles.size()),
                null, cAlarm,
                stpl("report.colony.exploring.description")
                    .addName("%colony%", colony.getName())
                    .addAmount("%amount%", exploreTiles.size()));
            reportPanel.add(b);
        } else {
            reportPanel.add(new JLabel(""));
        }

        // Field: The number of existing colony tiles that would
        // benefit from ploughing.
        // Colour: Always cAlarm
        // Font: Bold if one of the tiles is the colony center.
        if (plowTiles.size() > 0) {
            b = colourButton(cac, Integer.toString(plowTiles.size()),
                null, cAlarm,
                stpl("report.colony.plowing.description")
                    .addName("%colony%", colony.getName())
                    .addAmount("%amount%", plowTiles.size()));
            if (plowMe) {
                b.setFont(b.getFont().deriveFont(Font.BOLD));
            }
            reportPanel.add(b);
        } else {
            reportPanel.add(new JLabel(""));
        }

        // Field: The number of existing colony tiles that would
        // benefit from a road.
        // Colour: cAlarm
        if (roadTiles.size() > 0) {
            b = colourButton(cac, Integer.toString(roadTiles.size()),
                null, cAlarm,
                stpl("report.colony.roadBuilding.description")
                    .addName("%colony%", colony.getName())
                    .addAmount("%amount%", roadTiles.size()));
            reportPanel.add(b);
        } else {
            reportPanel.add(new JLabel(""));
        }

        // Fields: The net production of each storable+non-trade-goods
        // goods type.
        // Colour: cAlarm if too low, cWarn if negative, empty if no
        // production, cPlain if production balanced at zero,
        // otherwise must be positive, wherein cExport
        // if exported, cAlarm if too high, else cGood.
        final int adjustment = colony.getWarehouseCapacity()
            / GoodsContainer.CARGO_SIZE;
        for (GoodsType g : goodsTypes) {
            int p = colony.getAdjustedNetProductionOf(g);
            ExportData exportData = colony.getExportData(g);
            int low = exportData.getLowLevel() * adjustment;
            int high = exportData.getHighLevel() * adjustment;
            int amount = colony.getGoodsCount(g);
            Color c;
            StringTemplate tip;
            if (p < 0) {
                if (amount < low) {
                    int turns = -amount / p + 1;
                    c = cAlarm;
                    tip = stpl("report.colony.production.low.description")
                        .addName("%colony%", colony.getName())
                        .add("%goods%", g.getNameKey())
                        .addAmount("%amount%", p)
                        .addAmount("%turns%", turns);
                } else {
                    c = cWarn;
                    tip = stpl("report.colony.production.description")
                            .addName("%colony%", colony.getName())
                            .add("%goods%", g.getNameKey())
                        .addAmount("%amount%", p);
                }
            } else if (p == 0) {
                if (colony.getTotalProductionOf(g) == 0) {
                    c = null;
                    tip = null;
                } else {
                    c = cPlain;
                    tip = stpl("report.colony.production.description")
                        .addName("%colony%", colony.getName())
                        .add("%goods%", g.getNameKey())
                        .addAmount("%amount%", p);
                }
            } else if (exportData.isExported()) {
                c = cExport;
                tip = stpl("report.colony.production.export.description")
                    .addName("%colony%", colony.getName())
                    .add("%goods%", g.getNameKey())
                    .addAmount("%amount%", p)
                    .addAmount("%export%", exportData.getExportLevel());
            } else if (g != foodType
                && amount + p > colony.getWarehouseCapacity()) {
                c = cAlarm;
                int waste = amount + p - colony.getWarehouseCapacity();
                tip = stpl("report.colony.production.waste.description")
                    .addName("%colony%", colony.getName())
                    .add("%goods%", g.getNameKey())
                    .addAmount("%amount%", p)
                    .addAmount("%waste%", waste);
            } else if (g != foodType && amount > high) {
                int turns = (colony.getWarehouseCapacity() - amount) / p;
                c = cWarn;
                tip = stpl("report.colony.production.high.description")
                    .addName("%colony%", colony.getName())
                    .add("%goods%", g.getNameKey())
                    .addAmount("%amount%", p)
                    .addAmount("%turns%", turns);
            } else {
                c = cGood;
                tip = stpl("report.colony.production.description")
                    .addName("%colony%", colony.getName())
                    .add("%goods%", g.getNameKey())
                    .addAmount("%amount%", p);
            }
            if (c == null) reportPanel.add(new JLabel(""));
            else {
                b = colourButton(cac, Integer.toString(p), null, c, tip);
                reportPanel.add(b);
            }
        }

        // Collect the types of the units at work in the colony
        // (colony tiles and buildings) that are suboptimal (and
        // are not just temporarily there because they are being
        // taught), the types for sites that really need a new
        // unit, the teachers, and the units that are not working.
        // TODO: this needs to be merged with the requirements
        // checking code, but that in turn should be opened up
        // so the AI can use it...
        HashMap<UnitType, Suggestion> improve
            = new HashMap<UnitType, Suggestion>();
        HashMap<UnitType, Suggestion> want
            = new HashMap<UnitType, Suggestion>();
        List<Unit> teachers = new ArrayList<Unit>();
        List<Unit> notWorking = new ArrayList<Unit>();
        for (Unit u : colony.getTile().getUnitList()) {
            if (u.getState() != Unit.UnitState.FORTIFIED
                && u.getState() != Unit.UnitState.SENTRY) {
                notWorking.add(u);
            }
        }

        for (WorkLocation wl : colony.getAvailableWorkLocations()) {
            if (!wl.canBeWorked()) {
                continue;
            } else if (wl.canTeach()) {
                teachers.addAll(wl.getUnitList());
                continue;
            }

            UnitType expert;
            GoodsType work;
            boolean needsWorker = !wl.isFull();
            int delta;

            // Check first if the units are working, and then add a
            // suggestion if there is a better type of unit for the
            // work being done.
            for (Unit u : wl.getUnitList()) {
                if (u.getTeacher() != null) {
                    continue; // Ignore students, they are temporary
                } else if ((work = u.getWorkType()) == null) {
                    notWorking.add(u);
                    needsWorker = true;
                } else if ((expert = spec.getExpertForProducing(work)) != null
                    && expert != u.getType()
                    && (delta = wl.getPotentialProduction(work, expert)
                        - wl.getPotentialProduction(work, u.getType())) > 0
                    && wantGoods(wl, work, u, expert)) {
                    addSuggestion(improve, u.getType(), expert,
                        work, delta);
                }
            }

            // Add a suggestion for an extra worker if there is
            // space, valid work to do, an expert type to do it,
            // and the goods are wanted.
            if (needsWorker
                && (work = bestProduction(wl, colonistType)) != null
                && (expert = spec.getExpertForProducing(work)) != null
                && (delta = wl.getPotentialProduction(work, expert)) > 0
                && wantGoods(wl, work, null, expert)) {
                addSuggestion(want, null, expert, work, delta);
            }
        }
        // Make a list of unit types that are not working at their
        // speciality, including the units just standing around.
        List<UnitType> couldWork = new ArrayList<UnitType>();
        for (Unit u : notWorking) {
            GoodsType t = u.getWorkType();
            WorkLocation wl = (u.getLocation() instanceof WorkLocation)
                ? (WorkLocation) u.getLocation()
                : null;
            GoodsType w = bestProduction(wl, colonistType);
            if (w == null || w != t) couldWork.add(u.getType());
        }

        // Field: New colonist arrival or famine warning.
        // Colour: cGood if arriving eventually, blank if not enough food
        // to grow, cWarn if negative, cAlarm if famine soon.
        if (newColonist > 0) {
            b = colourButton(cac, Integer.toString(newColonist),
                null, cGood,
                stpl("report.colony.arriving.description")
                    .addName("%colony%", colony.getName())
                    .add("%unit%", colonistType.getNameKey())
                    .addAmount("%turns%", newColonist));
            reportPanel.add(b);
        } else if (newColonist < 0) {
            b = colourButton(cac, Integer.toString(-newColonist),
                null, (newColonist >= -3) ? cAlarm : cWarn,
                stpl("report.colony.starving.description")
                    .addName("%colony%", colony.getName())
                    .addAmount("%turns%", -newColonist));
            reportPanel.add(b);
        } else {
            reportPanel.add(new JLabel(""));
        }

        // Field: What is currently being built (clickable if on the
        // buildqueue) and the turns until it completes, including
        // units being taught.
        // Colour: cAlarm bold "Nothing" if nothing being built, cAlarm
        // with no turns if no production, cGood with turns if
        // completing, cAlarm with turns if will block, turns
        // indicates when blocking occurs.
        BuildableType build = colony.getCurrentlyBuilding();
        int fields = 1 + teachers.size();
        String layout = (fields > 1) ? "split " + fields : null;
        String qac = BUILDQUEUE + colony.getId();
        if (build == null) {
            b = colourButton(qac, Messages.message("nothing"),
                null, cAlarm,
                stpl("report.colony.making.noconstruction.description")
                    .addName("%colony%", colony.getName()));
            b.setFont(b.getFont().deriveFont(Font.BOLD));
        } else {
            AbstractGoods needed = new AbstractGoods();
            int turns = colony.getTurnsToComplete(build, needed);
            String name = Messages.message(build.getNameKey());
            if (turns == FreeColObject.UNDEFINED) {
                b = colourButton(qac, name, null, cAlarm,
                    stpl("report.colony.making.noconstruction.description")
                        .addName("%colony%", colony.getName()));
            } else if (turns >= 0) {
                name += " " + Integer.toString(turns);
                b = colourButton(qac, name, null, cGood,
                    stpl("report.colony.making.constructing.description")
                        .addName("%colony%", colony.getName())
                        .add("%buildable%", build.getNameKey())
                        .addAmount("%turns%", turns));;
            } else if (turns < 0) {
                GoodsType goodsType = needed.getType();
                int goodsAmount = needed.getAmount()
                    - colony.getGoodsCount(goodsType);
                turns = -turns;
                name += " " + Integer.toString(turns);
                b = colourButton(qac, name, null, cAlarm,
                    stpl("report.colony.making.blocking.description")
                        .addName("%colony%", colony.getName())
                        .addAmount("%amount%", goodsAmount)
                        .add("%goods%", goodsType.getNameKey())
                        .add("%buildable%", build.getNameKey())
                        .addAmount("%turns%", turns));
            }
        }
        reportPanel.add(b, layout);
        layout = null;
        Collections.sort(teachers, teacherComparator);
        for (Unit u : teachers) {
            int left = u.getNeededTurnsOfTraining()
                - u.getTurnsOfTraining();
            if (left <= 0) {
                b = colourButton(cac, Integer.toString(0),
                    lib.getUnitImageIcon(u.getType(), Role.DEFAULT,
                        true, 0.333), cAlarm,
                    stpl("report.colony.making.noteach.description")
                        .addName("%colony%", colony.getName())
                        .addStringTemplate("%teacher%", u.getLabel()));
            } else {
                b = colourButton(cac, Integer.toString(left),
                    lib.getUnitImageIcon(u.getType(), Role.DEFAULT,
                        true, 0.333), Color.BLACK,
                    stpl("report.colony.making.educating.description")
                        .addName("%colony%", colony.getName())
                        .addStringTemplate("%teacher%", u.getLabel())
                        .addAmount("%turns%", left));
            }
            reportPanel.add(b);
        }
        if (fields <= 0) reportPanel.add(new JLabel(""));

        // Field: The units that could be upgraded.
        if (!improve.isEmpty()) {
            addUnits(improve, couldWork, colony, grow);
        } else {
            reportPanel.add(new JLabel(""));
        }

        // Field: The units the colony could make good use of.
        if (!want.isEmpty()) {
            // TODO: explain food limitations better
            grow = Math.min(grow, colony.getNetProductionOf(foodType)
                / Settlement.FOOD_PER_COLONIST);
            addUnits(want, couldWork, colony, grow);
        } else {
            reportPanel.add(new JLabel(""));
        }
    }

    private StringTemplate stpl(String messageId) {
        return StringTemplate.template(messageId);
    }

    private void conciseHeaders(List<GoodsType> goodsTypes, boolean top,
                                Market market) {
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL),
            "newline, span, growx");

        reportPanel.add(newLabel("report.colony.name.header", null, null,
                                 stpl("report.colony.name.description")),
            "newline");
        reportPanel.add(newLabel("report.colony.grow.header", null, null,
                                 stpl("report.colony.grow.description")));
        reportPanel.add(newLabel("report.colony.explore.header", null, null,
                                 stpl("report.colony.explore.description")));
        reportPanel.add(newLabel("report.colony.plow.header", null, null,
                                 stpl("report.colony.plow.description")));
        reportPanel.add(newLabel("report.colony.road.header", null, null,
                                 stpl("report.colony.road.description")));
        for (GoodsType g : goodsTypes) {
            ImageIcon ii = getGUI().getImageLibrary()
                .getScaledGoodsImageIcon(g, 0.667);
            JLabel l = newLabel(null, ii, null,
                stpl("report.colony.production.header")
                    .add("%goods%", g.getNameKey()));
            l.setEnabled(market == null || market.getArrears(g) <= 0);
            reportPanel.add(l);
        }
        final UnitType colonistType = getSpecification().getDefaultUnitType();
        ImageIcon colonistIcon
            = getGUI().getImageLibrary().getUnitImageIcon(colonistType,
                Role.DEFAULT, true, 0.333);
        reportPanel.add(newLabel(null, colonistIcon, null,
                                 stpl("report.colony.birth.description")));
        reportPanel.add(newLabel("report.colony.making.header", null, null,
                                 stpl("report.colony.making.description")));
        reportPanel.add(newLabel("report.colony.improve.header", null, null,
                                 stpl("report.colony.improve.description")));
        reportPanel.add(newLabel("report.colony.wanted.header", null, null,
                                 stpl("report.colony.wanted.description")));

        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL),
            "newline, span, growx");
    }

    private JLabel newLabel(String h, ImageIcon i, Color c, StringTemplate t) {
        if (h != null) h = Messages.message(h);
        JLabel l = new JLabel(h, i, SwingConstants.CENTER);
        l.setForeground((c == null) ? Color.BLACK : c);
        if (t != null) {
            l.setToolTipText(Messages.message(t));
        }
        return l;
    }

    private JButton colourButton(String action, String h,
                                 ImageIcon i, Color c, StringTemplate t) {
        if (h != null) {
            if (Messages.containsKey(h)) h = Messages.message(h);
        }
        JButton b = getLinkButton(h, i, action);
        b.setForeground((c == null) ? Color.BLACK : c);
        if (t != null) {
            b.setToolTipText(Messages.message(t));
        }
        b.addActionListener(this);
        return b;
    }

    private void addSuggestion(HashMap<UnitType, Suggestion> suggestions,
                               UnitType old, UnitType expert,
                               GoodsType work, int amount) {
        Suggestion suggestion = suggestions.get(expert);
        // Keep it simple for now.
        if (suggestion == null || suggestion.amount < amount) {
            suggestions.put(expert, new Suggestion(old, expert, work, amount));
        }
    }

    /**
     * Is it a good idea to produce goods at this work location using a
     * better unit type?
     *
     * Always true for colony tiles, but for buildings we need to be
     * more conservative or we will end up recommending packing each
     * building to capacity.
     *
     * FTM then:
     * - assume that if we have upgraded the building we really do
     *   want to use it
     * - we should produce hammers if we are not, or if we can upgrade
     *   and existing unit
     * - we should produce liberty until we max out the colony SoL
     *
     * @param wl The <code>WorkLocation</code> where production is to occur.
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param unit The <code>Unit</code> that is doing the job at present,
     *     which may be null if none is at work.
     * @param expert The expert <code>UnitType</code> to put to work.
     * @return True if it is a good idea to use the expert.
     */
    private boolean wantGoods(WorkLocation wl, GoodsType goodsType,
                              Unit unit, UnitType expert) {
        boolean ret = false;
        if (wl instanceof ColonyTile) {
            ret = true;
        } else if (wl instanceof Building) {
            Building bu = (Building) wl;
            Colony colony = wl.getColony();
            ret = bu.canAddType(expert)
                && (bu.getLevel() > 1
                    || ("model.goods.hammers".equals(goodsType.getId())
                        && (colony.getTotalProductionOf(goodsType) == 0
                            || (unit != null && unit.getType() != expert)))
                    || (goodsType.isLibertyType()
                        && colony.getSoL() < 100));
        }
        return ret;
    }

    private void addUnits(final HashMap<UnitType, Suggestion> suggestions,
                          List<UnitType> have, Colony colony, int grow) {
        final String action = colony.getId();
        final ImageLibrary lib = getGUI().getImageLibrary();

        String layout = (suggestions.size() <= 1) ? null
            : "split " + Integer.toString(suggestions.size());
        List<UnitType> types = new ArrayList<UnitType>();
        types.addAll(suggestions.keySet());
        Collections.sort(types, new Comparator<UnitType>() {
                public int compare(UnitType t1, UnitType t2) {
                    int cmp = suggestions.get(t2).amount
                        - suggestions.get(t1).amount;
                    return (cmp != 0) ? cmp
                        : t1.getId().compareTo(t2.getId());
                }
            });
        for (UnitType type : types) {
            boolean present = false;
            if (have.contains(type)) {
                have.remove(type);
                present = true;
            }
            Suggestion suggestion = suggestions.get(type);
            String label = Integer.toString(suggestion.amount);
            ImageIcon ii = lib.getUnitImageIcon(type, Role.DEFAULT,
                true, 0.333);
            StringTemplate tip = (suggestion.oldType == null)
                ? stpl("report.colony.wanting.description")
                    .addName("%colony%", colony.getName())
                    .add("%unit%", type.getNameKey())
                    .add("%goods%", suggestion.goodsType.getNameKey())
                    .addAmount("%amount%", suggestion.amount)
                : stpl("report.colony.improving.description")
                    .addName("%colony%", colony.getName())
                    .add("%oldUnit%", suggestion.oldType.getNameKey())
                    .add("%unit%", type.getNameKey())
                    .add("%goods%", suggestion.goodsType.getNameKey())
                    .addAmount("%amount%", suggestion.amount);
            JButton b = colourButton(action, label, ii,
                (present) ? cGood : cPlain, tip);
            reportPanel.add(b, layout);
            layout = null;
        }
    }

    private GoodsType bestProduction(WorkLocation wl, UnitType type) {
        if (wl == null) {
            return null;
        } else if (wl instanceof Building) {
            return ((Building) wl).getGoodsOutputType();
        } else {
            final Specification spec = getSpecification();
            List<AbstractGoods> prod = new ArrayList<AbstractGoods>();
            for (GoodsType g : spec.getGoodsTypeList()) {
                int amount = wl.getPotentialProduction(g, type);
                if (amount > 0) prod.add(new AbstractGoods(g, amount));
            }
            if (prod.isEmpty()) return null;
            Collections.sort(prod, abstractGoodsComparator);
            return prod.get(0).getType();
        }
    }
}
