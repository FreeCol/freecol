/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.client.gui.panel.report;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Colony.TileImprovementSuggestion;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.ProductionType;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.model.WorkLocation.Suggestion;
import net.sf.freecol.common.resources.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.sf.freecol.common.model.Constants.UNDEFINED;
import static net.sf.freecol.common.util.CollectionUtils.accumulateMap;
import static net.sf.freecol.common.util.CollectionUtils.accumulateToMap;
import static net.sf.freecol.common.util.CollectionUtils.alwaysTrue;
import static net.sf.freecol.common.util.CollectionUtils.any;
import static net.sf.freecol.common.util.CollectionUtils.appendToMapList;
import static net.sf.freecol.common.util.CollectionUtils.count;
import static net.sf.freecol.common.util.CollectionUtils.descendingDoubleComparator;
import static net.sf.freecol.common.util.CollectionUtils.descendingIntegerComparator;
import static net.sf.freecol.common.util.CollectionUtils.doubleAccumulator;
import static net.sf.freecol.common.util.CollectionUtils.find;
import static net.sf.freecol.common.util.CollectionUtils.first;
import static net.sf.freecol.common.util.CollectionUtils.forEachMapEntry;
import static net.sf.freecol.common.util.CollectionUtils.integerAccumulator;
import static net.sf.freecol.common.util.CollectionUtils.mapEntriesByValue;
import static net.sf.freecol.common.util.CollectionUtils.matchKey;
import static net.sf.freecol.common.util.CollectionUtils.sort;
import static net.sf.freecol.common.util.CollectionUtils.transform;


/**
 * This panel displays the compact colony report.
 */
public final class ReportCompactColonyPanel extends ReportPanel {

    /** Predicate to select units that are not working. */
    private static final Predicate<Unit> notWorkingPred = u ->
        u.getState() != Unit.UnitState.FORTIFIED && u.getState() != Unit.UnitState.SENTRY;

    private static Map<String, Set<BuildingType>> bestProduction;

    /** Container class for all the information about a colony. */
    private static class ColonySummary {

        /** Types of production for a given goods type. */
        public static enum ProductionStatus {
            FAIL,        // Negative production and below low alarm level
            NONE,        // No production at all
            INSUFFICIENT_BUILDINGS,     // Positive or negative production
            GOOD,        // Production with best buildings
            EXPORT,      // Positive production and exporting
            EXCESS,      // Positive production and above high alarm level
            OVERFLOW,    // Positive production and above capacity
            INSUFFICIENT_PRODUCTION,  // Positive production but could produce more
        };


        /** Container class for goods production. */
        public static class GoodsProduction {

            /** Binary accumulation operator for goods production. */
            public static final BinaryOperator<GoodsProduction>
                goodsProductionAccumulator = (g1, g2) -> g1.accumulate(g2);

            public int amount;
            public ProductionStatus status;
            public int extra;

            /**
             * Build a new goods production container.
             *
             * @param amount The amount of goods.
             * @param status The production status.
             * @param extra Extra production.
             */
            public GoodsProduction(int amount, ProductionStatus status,
                int extra) {
                this.amount = amount;
                this.status = status;
                this.extra = extra;
            }

            /**
             * Accumulate other production into this one.
             *
             * @param other The other {@code GoodsProduction}.
             * @return This {@code GoodsProduction}.
             */
            public GoodsProduction accumulate(GoodsProduction other) {
                this.amount += other.amount;
                this.status = (this.status == ProductionStatus.NONE
                               && other.status == ProductionStatus.NONE)
                        ? ProductionStatus.NONE
                        : ProductionStatus.INSUFFICIENT_BUILDINGS;
                this.extra = 0;
                return this;
            }
        };


        /** The colony being summarized. */
        public final Colony colony;

        /** Possible tile improvements. */
        public final List<TileImprovementSuggestion> tileSuggestions
            = new ArrayList<>();

        /** Famine warning required? */
        public final boolean famine;

        /**
         * Turns to new colonist if positive, no colonist if zero,
         * -turns-1 to starvation if negative.
         */
        public final int newColonist;

        /** Current production bonus. */
        public final int bonus;

        public final int unitCount;

        public final int unitsToAdd;

        public final int unitsToRemove;

        /** Goods production. */
        public final Map<GoodsType, GoodsProduction> production
            = new HashMap<>();

        /** Teacher units mapped to turns to complete. */
        public final Map<Unit, Integer> teachers = new HashMap<>();
        /** Units present that are not working. */
        public final List<Unit> notWorking = new ArrayList<>();
        /** Units present that might be working. */
        public final List<UnitType> couldWork = new ArrayList<>();
        /** Suggested better unit use. */
        public final Map<UnitType, Suggestion> improve = new HashMap<>();
        /** Suggested new unit use. */
        public final Map<UnitType, Suggestion> want = new HashMap<>();

        /** Currently building. */
        public final BuildableType build;
        public final int completeTurns;
        public final AbstractGoods needed;


        /**
         * Create the colony summary.
         *
         * @param colony The {@code Colony} to summarize.
         * @param goodsTypes A list of {@code GoodsType}s to include
         *     in the summary.
         */
        public ColonySummary(Colony colony, List<GoodsType> goodsTypes) {
            this.colony = colony;

            final Specification spec = colony.getSpecification();

            this.tileSuggestions.clear();
            this.tileSuggestions.addAll(colony.getTileImprovementSuggestions());

            int starve = colony.getStarvationTurns();
            if (starve < 0) {
                this.famine = false;
                this.newColonist = colony.getNewColonistTurns();
            } else {
                this.famine = starve <= Colony.FAMINE_TURNS;
                this.newColonist = -starve - 1;
            }

            this.bonus = colony.getProductionBonus();

            this.unitCount = colony.getUnitCount();
            this.unitsToAdd = colony.getUnitsToAdd();
            this.unitsToRemove = colony.getUnitsToRemove();

            for (GoodsType gt : goodsTypes) {
                this.production.put(gt, getGoodsProduction(gt));
            }

            this.notWorking.addAll(transform(colony.getTile().getUnits(),
                    notWorkingPred));

            collectSuboptimalWorkLocations(colony, spec);

            // Make a list of unit types that are not working at their
            // speciality, including the units just standing around.
            final Predicate<Unit> couldWorkPred = u -> {
                WorkLocation wl = u.getWorkLocation();
                return wl != null && (wl.getWorkFor(u) == null
                    || wl.getWorkFor(u) != u.getWorkType());
            };
            this.couldWork.addAll(transform(this.notWorking, couldWorkPred,
                    Unit::getType));

            this.build = colony.getCurrentlyBuilding();
            if (this.build == null) {
                this.completeTurns = -1;
                this.needed = null;
            } else {
                AbstractGoods needed = new AbstractGoods();
                this.completeTurns = colony.getTurnsToComplete(build, needed);
                this.needed = (this.completeTurns < 0) ? needed : null;
            }
        }

        // Collect the types of the units at work in the colony
        // (colony tiles and buildings) that are suboptimal (and
        // are not just temporarily there because they are being
        // taught), the types for sites that really need a new
        // unit, the teachers, and the units that are not working.
        //
        // FIXME: this needs to be merged with the requirements
        // checking code, but that in turn should be opened up
        // so the AI can use it...
        private void collectSuboptimalWorkLocations(Colony colony, Specification spec) {
            for (WorkLocation wl :transform(colony.getAvailableWorkLocations(),
                                            WorkLocation::canBeWorked)) {
                if (wl.canTeach()) {
                    for (Unit u : wl.getUnitList()) {
                        teachers.put(u, u.getNeededTurnsOfTraining()
                            - u.getTurnsOfTraining());
                    }
                    continue;
                }

                // Check if the units are working.
                this.notWorking.addAll(transform(wl.getUnits(),
                        u -> (u.getTeacher() == null
                            && u.getWorkType() == null)));

                // Add work location suggestions.
                forEachMapEntry(wl.getSuggestions(),
                    e -> addSuggestion(((e.getKey() == null) ? this.want
                            : this.improve),
                                       spec.getExpertForProducing(e.getValue().goodsType),
                                       e.getValue()));
            }
        }

        private boolean hasBestBuildings(GoodsType goodsType) {
            if (bestProduction == null) {
                initBestProduction();
            }
            Set<BuildingType> buildingTypes = bestProduction.get(goodsType.getId());
            if (buildingTypes == null) {
                return true;
            }
            return buildingTypes.stream().allMatch(type -> {
                Building b = colony.getBuilding(type);
                return b != null && b.getType().getId().equals(type.getId());
            });
        }

        private void initBestProduction() {
            bestProduction = new HashMap<>();
            for (BuildingType buildingType : colony.getGame().getSpecification().getBuildingTypeList()) {
                if (buildingType.getUpgradesTo() != null) {
                    continue;
                }
                buildingType.getModifiers().forEach(m -> {
                    bestProduction.computeIfAbsent(m.getId(), s -> new HashSet<>()).add(buildingType);
                });
                for (ProductionType productionType : buildingType.getProductionTypes()) {
                    if (productionType.getUnattended()) {
                        continue;
                    }
                    productionType.getOutputs().forEach(o -> {
                        String goodsType = o.getId();
                        bestProduction.computeIfAbsent(goodsType, s -> new HashSet<>()).add(buildingType);
                    });
                }
            }
        }

        /**
         * Set the production map values for the given goods type.
         *
         * @param goodsType The {@code GoodsType} to use.
         */
        private GoodsProduction getGoodsProduction(GoodsType goodsType) {
            final ExportData exportData = colony.getExportData(goodsType);
            final int adjustment = colony.getWarehouseCapacity() / GoodsContainer.CARGO_SIZE;
            final int low = exportData.getLowLevel() * adjustment;
            final int high = exportData.getHighLevel() * adjustment;
            final int amount = colony.getGoodsCount(goodsType);
            int p = colony.getAdjustedNetProductionOf(goodsType);

            if (!goodsType.limitIgnored()) {
                if (amount + p > colony.getWarehouseCapacity()) {
                    return new GoodsProduction(p, ProductionStatus.OVERFLOW, amount + p - colony.getWarehouseCapacity());
                }
                if (p > 0 && amount >= high) {
                    return new GoodsProduction(p, ProductionStatus.EXCESS, (colony.getWarehouseCapacity() - amount) / p);
                }
            }
            if (p < 0 && amount < low) {
                return new GoodsProduction(p, ProductionStatus.FAIL, -amount / p + 1);
            }
            int productionDeficit = getProductionDeficit(goodsType);
            if (productionDeficit > 0) {
                return new GoodsProduction(p, ProductionStatus.INSUFFICIENT_PRODUCTION, productionDeficit);
            }
            if (exportData.getExported()) {
                return new GoodsProduction(p, ProductionStatus.EXPORT, exportData.getExportLevel());
            }
            if (colony.isProducing(goodsType) && !hasBestBuildings(goodsType)) {
                return new GoodsProduction(p, ProductionStatus.INSUFFICIENT_BUILDINGS, 0);
            }
            if (p == 0) {
                return new GoodsProduction(p, ProductionStatus.NONE, 0);
            }
            return new GoodsProduction(p, ProductionStatus.GOOD, 0);
        }

        private int getProductionDeficit(GoodsType goodsType) {
            for (WorkLocation wl : colony.getCurrentWorkLocationsList()) {
                if (wl.getUnitCount() == 0) {
                    continue;
                }
                List<AbstractGoods> goodsList = wl.getAvailableProductionTypes(false)
                                                  .stream()
                                                  .flatMap(t -> Optional.ofNullable(
                                                          t.getOutput(goodsType)).stream())
                                                  .collect(Collectors.toList());
                if (goodsList.isEmpty()) {
                    continue;
                }
                ProductionInfo pi = colony.getProductionInfo(wl);
                List<AbstractGoods> abstractGoods = pi == null || (pi.getMaximumProduction().size() == 0 && pi.getProduction().size() == 0)
                        ? goodsList : pi.getProductionDeficit();
                AbstractGoods deficit = find(abstractGoods, AbstractGoods.matches(goodsType));
                if (deficit != null) {
                    return deficit.getAmount();
                }
            }
            return 0;
        }

        private void addSuggestion(Map<UnitType, Suggestion> suggestions,
            UnitType expert, Suggestion suggestion) {
            if (suggestion == null || expert == null) return;
            Suggestion now = suggestions.get(expert);
            if (now == null || now.amount < suggestion.amount) {
                suggestions.put(expert, suggestion);
            }
        }
    };

    /**
     * Predicate to select the goods to report on.
     */
    private static final Predicate<GoodsType> reportGoodsPred = gt ->
            (gt.isStorable() && !gt.isTradeGoods()) || gt.limitIgnored();
    private static final String BUILDQUEUE = "buildQueue.";
    private static final String cAlarmKey = "color.report.colony.alarm";
    private static final String cWarnKey = "color.report.colony.warning";
    private static final String cPlainKey = "color.report.colony.plain";
    private static final String cExportKey = "color.report.colony.export";
    private static final String cGoodKey = "color.report.colony.good";
    private static Color cAlarm = null;
    private static Color cWarn;
    private static Color cPlain;
    private static Color cExport;
    private static Color cGood;

    private final Specification spec;
    private final ImageLibrary lib;
    private final List<List<Colony>> colonies = new ArrayList<>();
    private final Market market;
    private final List<GoodsType> goodsTypes = new ArrayList<>();


    /**
     * Creates a compact colony report.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ReportCompactColonyPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportColonyAction");

        this.spec = getSpecification();
        this.lib = getImageLibrary();
        final Player player = getMyPlayer();
        this.market = player.getMarket();

        // Sort the colonies by continent.
        final Map<Integer, List<Colony>> continents = new HashMap<>();
        for (Colony c : player.getColonyList()) {
            if (c.getUnitCount() > 0) {
                // Do not include colonies that have been abandoned
                // but are still on the colonies list.
                appendToMapList(continents, c.getTile().getContiguity(), c);
            }
        }
        final Comparator<Colony> colonyComparator
            = freeColClient.getClientOptions().getColonyComparator();
        final Comparator<List<Colony>> firstColonyComparator
            = Comparator.comparing(l -> first(l), colonyComparator);
        this.colonies.addAll(sort(continents.values(), firstColonyComparator));

        this.goodsTypes.addAll(transform(spec.getGoodsTypeList(),
                reportGoodsPred,
                Function.<GoodsType>identity(),
                GoodsType.goodsTypeComparator));

        loadResources();
        update();
    }


    private synchronized void loadResources() {
        if (cAlarm != null) return;

        cAlarm = ImageLibrary.getColor(cAlarmKey, Color.RED);
        cWarn = ImageLibrary.getColor(cWarnKey, Color.MAGENTA);
        cPlain = ImageLibrary.getColor(cPlainKey, Color.DARK_GRAY);
        cExport = ImageLibrary.getColor(cExportKey, Color.GREEN);
        cGood = ImageLibrary.getColor(cGoodKey, Color.BLUE);
    }

    private static StringTemplate stpl(String messageId) {
        return (Messages.containsKey(messageId))
            ? StringTemplate.template(messageId)
            : null;
    }

    private static StringTemplate stpld(String messageId) {
        messageId = Messages.descriptionKey(messageId);
        return stpl(messageId);
    }

    private JLabel newLabel(String h, ImageIcon i, Color c) {
        JLabel l = new JLabel(h, i, SwingConstants.CENTER);
        l.setForeground((c == null) ? Color.BLACK : c);
        return l;
    }

    private JLabel newLabel(String h, ImageIcon i, Color c, StringTemplate t) {
        if (h != null && Messages.containsKey(h)) h = Messages.message(h);
        JLabel l = newLabel(h, i, c);
        if (t != null) Utility.localizeToolTip(l, t);
        return l;
    }

    private JButton newButton(String action, String h, ImageIcon i,
        Color c, StringTemplate t) {
        if (h != null && Messages.containsKey(h)) h = Messages.message(h);
        JButton b = Utility.getLinkButton(h, i, action);
        b.setForeground((c == null) ? Color.BLACK : c);
        if (t != null) Utility.localizeToolTip(b, t);
        b.addActionListener(this);
        return b;
    }

    private void addTogether(List<? extends JComponent> components) {
        if (components.isEmpty()) {
            reportPanel.add(new JLabel());
            return;
        }
        String layout = (components.size() > 1) ? "split " + components.size()
            : null;
        for (JComponent jc : components) {
            reportPanel.add(jc, layout);
            layout = null;
        }
    }

    /**
     * Update a single colony.
     *
     * @param s The {@code ColonySummary} to update from.
     */
    private void updateColony(ColonySummary s) {
        final String colonyId = s.colony.getId();
        final UnitType defaultUnitType = spec.getDefaultUnitType(s.colony.getOwner());
        List<JComponent> buttons = new ArrayList<>(16);

        addName(s, colonyId);
        addSize(s, colonyId);
        addUnitsToAdd(s, colonyId);
        addUnitsToRemove(s, colonyId);
        addExploring(s, colonyId);
        addTileImprovements(s, colonyId);
        addGoods(s, colonyId);
        addGrowth(s, colonyId, defaultUnitType);
        addNotWorking(s, colonyId);
        addNotWorkingInProfession(s, colonyId);
        addBuildQueue(s, colonyId, buttons);
        addEducation(s, colonyId, defaultUnitType, buttons);
        addTogether(buttons);
        addUnitsToUpgrade(s, buttons);
    }

    private void addUnitsToUpgrade(ColonySummary s, List<JComponent> buttons) {
        // Field: The units that could be upgraded, followed by the units
        // that could be added.
        if (s.improve.isEmpty() && s.want.isEmpty()) {
            reportPanel.add(new JLabel());
        } else {
            buttons.clear();
            buttons.addAll(unitButtons(s.improve, s.couldWork, s.colony));
            buttons.add(new JLabel("/"));
            // Prefer to suggest an improvement over and addition.
            for (UnitType ut : s.improve.keySet()) s.want.remove(ut);
            buttons.addAll(unitButtons(s.want, s.couldWork, s.colony));
            addTogether(buttons);
        }
    }

    private void addExploring(ColonySummary s, String colonyId) {
        JButton b;
        StringTemplate t;
        // Field: The number of potential colony tiles that need
        // exploring.
        // Colour: Always cAlarm
        int n = count(s.tileSuggestions,
                      TileImprovementSuggestion::isExploration);
        if (n > 0) {
            t = stpld("report.colony.exploring")
                .addName("%colony%", s.colony.getName())
                .addAmount("%amount%", n);
            b = newButton(colonyId, Integer.toString(n), null, cAlarm, t);
        } else {
            b = null;
        }
        reportPanel.add((b == null) ? new JLabel() : b);
    }

    private void addSize(ColonySummary s, String colonyId) {
        Color c;
        StringTemplate t;
        // Field: Size
        c = cGood;
        t = stpld("report.colony.size");
        reportPanel.add(newButton(colonyId, Integer.toString(s.unitCount), null, c, t));
    }

    // Field: The number of colonists that can be added to a
    // colony without damaging the production bonus
    private void addUnitsToAdd(ColonySummary s, String colonyId) {
        Color c;
        JButton b;
        StringTemplate t;
        if (s.unitsToAdd > 0) {
            c = cGood;
            t = stpld("report.colony.growing")
                .addName("%colony%", s.colony.getName())
                .addAmount("%amount%", s.unitsToAdd);
            b = newButton(colonyId, Integer.toString(s.unitsToAdd), null, c, t);
        } else {
            b = null;
        }
        reportPanel.add((b == null) ? new JLabel() : b);
    }


    private void addUnitsToRemove(ColonySummary s, String colonyId) {
        JButton b;
        Color c;
        StringTemplate t;

        // Field: the number of colonists to remove to fix the inefficiency.
        // Colour: Blue if efficient/Red if inefficient.
        if (s.unitsToRemove > 0) {
            c = s.bonus < 0 ? cAlarm : cGood;
            t = stpld("report.colony.shrinking")
                .addName("%colony%", s.colony.getName())
                .addAmount("%amount%", s.unitsToRemove);
            b = newButton(colonyId, Integer.toString(s.unitsToRemove), null, c, t);
        } else {
            b = null;
        }
        reportPanel.add((b == null) ? new JLabel() : b);
    }

    // Field: A button for the colony.
    // Colour: bonus in {-2,2} => {alarm, warn, plain, export, good}
    // Font: Bold if famine is threatening.
    private void addName(ColonySummary s, String colonyId) {
        Color c = (s.bonus <= -2) ? cAlarm
                : (s.bonus == -1) ? cWarn
                        : (s.bonus == 0) ? cPlain
                                : (s.bonus == 1) ? cExport
                                        : cGood;
        StringTemplate t = StringTemplate.label(",");
        String annotations = getAnnotations(s, t);
        JButton b = newButton(colonyId, s.colony.getName() + annotations, null, c,
                              StringTemplate.label(": ").add(s.colony.getName())
                                            .add(Messages.message(t)));
        if (s.famine) b.setFont(b.getFont().deriveFont(Font.BOLD));
        reportPanel.add(b, "newline");
    }

    // Field: New colonist arrival or famine warning.
    // Colour: cGood if arriving eventually, blank if not enough food
    // to grow, cWarn if negative, cAlarm if famine soon.
    private void addGrowth(ColonySummary s, String colonyId, UnitType defaultUnitType) {
        JButton b;
        StringTemplate t;
        if (s.newColonist > 0) {
            t = stpld("report.colony.arriving")
                .addName("%colony%", s.colony.getName())
                .addNamed("%unit%", defaultUnitType)
                .addAmount("%turns%", s.newColonist);
            b = newButton(colonyId, Integer.toString(s.newColonist), null,
                          cGood, t);
        } else if (s.newColonist < 0) {
            Color c = (s.famine) ? cAlarm : cWarn;
            t = stpld("report.colony.starving")
                .addName("%colony%", s.colony.getName())
                .addAmount("%turns%", -s.newColonist);
            b = newButton(colonyId, Integer.toString(-s.newColonist), null,
                          c, t);
            if (s.famine) b.setFont(b.getFont().deriveFont(Font.BOLD));
        } else {
            b = null;
        }
        reportPanel.add((b == null) ? new JLabel() : b);
    }

    private boolean isWorking(Unit unit) {
        if (!unit.isPerson() || unit.getDestination() != null) {
            return true;
        }
        switch (unit.getState()) {
            case FORTIFIED:
            case FORTIFYING:
            case IMPROVING:
                return true;
            case IN_COLONY:
                return unit.getWorkType() != null || unit.getStudent() != null;
        }
        return unit.getRole().getRequiredGoodsList().stream().anyMatch(g -> g.getId().equals("model.goods.muskets"));
    }

    private void addNotWorking(ColonySummary s, String colonyId) {
        long n = getAllUnits(s).filter(u -> !isWorking(u)).count();
        reportPanel.add((n == 0) ? new JLabel() : newButton(colonyId, Long.toString(n), null, cAlarm, stpld("report.colony.notWorking")));
    }

    private void addNotWorkingInProfession(ColonySummary s, String colonyId) {
        long n = getAllUnits(s)
                .filter(u -> u.isPerson() && !u.isWorkingInProfession())
                .count();
        reportPanel.add((n == 0) ? new JLabel() : newButton(colonyId, Long.toString(n), null, cWarn, stpld("report.colony.notWorkingInProfession")));
    }

    private Stream<Unit> getAllUnits(ColonySummary s) {
        return Stream.concat(s.colony.getUnits(), s.colony.getTile().getUnits());
    }

    private String getAnnotations(ColonySummary s, StringTemplate t) {
        Building building;
        String annotations = "";
        String key;
        if ((building = s.colony.getStockade()) == null) {
            key = "annotation.unfortified";
            t.add(Messages.message("report.colony.annotation.unfortified"));
        } else {
            key = "annotation." + building.getType().getSuffix();
            t.add(Messages.message(building.getLabel()));
        }
        if (ResourceManager.getStringResource(key, false) != null) {
            annotations += ResourceManager.getString(key);
        }
        if (!s.colony.getTile().isCoastland()) {
            key = "annotation.inland";
            t.add(Messages.message("report.colony.annotation.inland"));
        } else if ((building = s.colony.getWorkLocationWithAbility(Ability.PRODUCE_IN_WATER, Building.class)) == null) {
            key = "annotation.coastal";
            t.add(Messages.message("report.colony.annotation.coastal"));
        } else {
            key = "annotation." + building.getType().getSuffix();
            t.add(Messages.message(building.getLabel()));
        }
        if (ResourceManager.getStringResource(key, false) != null) {
            annotations += ResourceManager.getString(key);
        }
        /* Omit for now, too much detail.
           for (GoodsType gt : spec.getLibertyGoodsTypeList()) {
           if ((building = s.colony.getWorkLocationWithModifier(gt.getId(), Building.class)) != null) {
           key = "annotation." + building.getType().getSuffix();
           t.add(Messages.message(building.getLabel()));
           if (ResourceManager.hasResource(key))
           annotations += ResourceManager.getString(key);
           }
           }*/
        /* Omit for now, too much detail.
           for (GoodsType gt : spec.getImmigrationGoodsTypeList()) {
           if ((building = s.colony.getWorkLocationWithModifier(gt.getId(), Building.class)) != null) {
           key = "annotation." + building.getType().getSuffix();
           t.add(Messages.message(building.getLabel()));
           if (ResourceManager.hasResource(key))
           annotations += ResourceManager.getString(key);
           }
           }*/
        /* Font update needed
           if ((building = s.colony.getWorkLocationWithAbility(Ability.TEACH, Building.class)) != null) {
           key = "annotation." + building.getType().getSuffix();
           t.add(Messages.message(building.getLabel()));
           if (ResourceManager.hasResource(key)) annotations += ResourceManager.getString(key);
           }*/
        if ((building = s.colony.getWorkLocationWithAbility(Ability.EXPORT, Building.class)) != null) {
            annotations += "*";
            t.add(Messages.message(building.getLabel()));
        }
        return annotations;
    }

    private void addEducation(ColonySummary s, String colonyId, UnitType defaultUnitType, List<JComponent> buttons) {
        StringTemplate t;
        JButton b;
        // Field: What is being trained, including shadow units for vacant
        // places.
        // Colour: cAlarm if completion is blocked, otherwise cPlain.
        int empty = 0;
        Building school = s.colony.getWorkLocationWithAbility(Ability.TEACH,
                                                              Building.class);
        if (school != null) empty = school.getType().getWorkPlaces();
        for (Entry<Unit, Integer> e
                 : mapEntriesByValue(s.teachers, descendingIntegerComparator)) {
            Unit u = e.getKey();
            ImageIcon ii = new ImageIcon(this.lib.getTinyUnitImage(u));
            if (e.getValue() <= 0) {
                t = stpld("report.colony.making.noteach")
                    .addName("%colony%", s.colony.getName())
                    .addStringTemplate("%teacher%",
                        u.getLabel(Unit.UnitLabelType.NATIONAL));
                b = newButton(colonyId, Integer.toString(0), ii, cAlarm, t);
            } else {
                t = stpld("report.colony.making.educating")
                    .addName("%colony%", s.colony.getName())
                    .addStringTemplate("%teacher%",
                        u.getLabel(Unit.UnitLabelType.NATIONAL))
                    .addAmount("%turns%", e.getValue());
                b = newButton(colonyId, Integer.toString(e.getValue()), ii,
                              cPlain, t);
            }
            buttons.add(b);
            empty--;
        }

        if (empty > 0) {
            final ImageIcon emptyIcon
                = new ImageIcon(this.lib.getTinyUnitTypeImage(defaultUnitType, true));
            t = stpld("report.colony.making.educationVacancy")
                .addName("%colony%", s.colony.getName())
                .addAmount("%number%", empty);
            for (; empty > 0; empty--) {
                buttons.add(newButton(colonyId, "", emptyIcon, cPlain, t));
            }
        }
    }

    // Field: What is currently being built (clickable if on the
    // buildqueue) and the turns until it completes, including
    // units being taught, or blank if nothing queued.
    // Colour: cWarn if no construction is occurring, cGood with
    // turns if completing, cAlarm with turns if will block, turns
    // indicates when blocking occurs.
    // Font: Bold if blocked right now.
    private void addBuildQueue(ColonySummary s, String colonyId, List<JComponent> buttons) {
        final String qac = BUILDQUEUE + colonyId;
        if (s.build != null) {
            int turns = s.completeTurns;
            String bname = Messages.getName(s.build);
            JButton b;
            StringTemplate t;
            if (turns == UNDEFINED) {
                t = stpld("report.colony.making.noconstruction")
                    .addName("%colony%", s.colony.getName());
                b = newButton(qac, bname, null, cWarn, t);
            } else if (turns >= 0) {
                t = stpld("report.colony.making.constructing")
                    .addName("%colony%", s.colony.getName())
                    .addNamed("%buildable%", s.build)
                    .addAmount("%turns%", turns);
                b = newButton(qac, bname + " " + turns, null,
                              cGood, t);
            } else { // turns < 0
                turns = -(turns + 1);
                t = stpld("report.colony.making.blocking")
                    .addName("%colony%", s.colony.getName())
                    .addAmount("%amount%", s.needed.getAmount())
                    .addNamed("%goods%", s.needed.getType())
                    .addNamed("%buildable%", s.build)
                    .addAmount("%turns%", turns);
                b = newButton(qac, bname + " " + turns,
                    null, cAlarm, t);
                if (turns == 0) b.setFont(b.getFont().deriveFont(Font.BOLD));
            }
            buttons.add(b);
        }
    }

    // Fields: The number of existing colony tiles that would
    // benefit from improvements.
    // Colour: Always cAlarm
    // Font: Bold if one of the tiles is the colony center.
    private void addTileImprovements(ColonySummary s, String colonyId) {
        for (TileImprovementType ti : spec.getTileImprovementTypeList()) {
            if (ti.isNatural()) continue;
            int n = 0;
            boolean center = false;
            for (TileImprovementSuggestion tis : s.tileSuggestions) {
                if (tis.tileImprovementType == ti) {
                    n++;
                    if (tis.tile == s.colony.getTile()) center = true;
                }
            }
            JButton b;
            if (n > 0) {
                Color c = cAlarm;
                StringTemplate t;
                if (n == 1) {
                    TileImprovementSuggestion tis = first(s.tileSuggestions);
                    if (any(tis.tile.getUnits(),
                            u -> (u.getState() == Unit.UnitState.IMPROVING
                                && u.getWorkImprovement() != null
                                && u.getWorkImprovement().getType()
                                == tis.tileImprovementType))) {
                        c = cWarn; // Work is underway
                    }
                    t = stpld("report.colony.tile." + ti.getSuffix()
                        + ".specific")
                        .addName("%colony%", s.colony.getName())
                        .addStringTemplate("%location%",
                            tis.tile.getColonyTileLocationLabel(s.colony));
                } else {
                    t = stpld("report.colony.tile." + ti.getSuffix())
                        .addName("%colony%", s.colony.getName())
                        .addAmount("%amount%", n);
                }
                b = newButton(colonyId, Integer.toString(n), null, c, t);
                if (center) b.setFont(b.getFont().deriveFont(Font.BOLD));
            } else {
                b = null;
            }
            reportPanel.add((b == null) ? new JLabel() : b);
        }
    }

    // Fields: The net production of each storable+non-trade-goods
    // goods type.
    // Colour: cAlarm if too low, cWarn if negative, empty if no
    // production, cPlain if production balanced at zero,
    // otherwise must be positive, wherein cExport
    // if exported, cAlarm if too high, else cGood.
    private void addGoods(ColonySummary s, String colonyId) {
        Colony colony = s.colony;
        for (GoodsType gt : this.goodsTypes) {
            final ColonySummary.GoodsProduction gp = s.production.get(gt);
            StringTemplate t;
            Color c;
            switch (gp.status) {
            case FAIL:
                c = cAlarm;
                t = stpld("report.colony.production.low")
                    .addName("%colony%", colony.getName())
                    .addNamed("%goods%", gt)
                    .addAmount("%amount%", -gp.amount)
                    .addAmount("%turns%", gp.extra);
                break;
            case NONE:
                c = null;
                t = null;
                break;
            case INSUFFICIENT_BUILDINGS:
                c = cPlain;
                t = stpld("report.colony.production")
                    .addName("%colony%", colony.getName())
                    .addNamed("%goods%", gt)
                    .addAmount("%amount%", gp.amount);
                break;
            case GOOD:
                c = cGood;
                t = stpld("report.colony.production")
                    .addName("%colony%", colony.getName())
                    .addNamed("%goods%", gt)
                    .addAmount("%amount%", gp.amount);
                break;
            case EXPORT:
                c = cExport;
                t = stpld("report.colony.production.export")
                    .addName("%colony%", colony.getName())
                    .addNamed("%goods%", gt)
                    .addAmount("%amount%", gp.amount)
                    .addAmount("%export%", gp.extra);
                break;
            case EXCESS:
                c = cWarn;
                t = stpld("report.colony.production.high")
                    .addName("%colony%", colony.getName())
                    .addNamed("%goods%", gt)
                    .addAmount("%amount%", gp.amount)
                    .addAmount("%turns%", gp.extra);
                break;
            case OVERFLOW:
                c = cAlarm;
                t = stpld("report.colony.production.waste")
                    .addName("%colony%", colony.getName())
                    .addNamed("%goods%", gt)
                    .addAmount("%amount%", gp.amount)
                    .addAmount("%waste%", gp.extra);
                break;
            case INSUFFICIENT_PRODUCTION:
                c = cAlarm;
                t = stpld("report.colony.production.maxProduction")
                    .addName("%colony%", colony.getName())
                    .addNamed("%goods%", gt)
                    .addAmount("%amount%", gp.amount)
                    .addAmount("%more%", gp.extra);
                break;
            default:
                throw new IllegalStateException("Bogus status: " + gp.status);
            }
            reportPanel.add((c == null) ? new JLabel()
                : newButton(colonyId, Integer.toString(gp.amount), null, c, t));
        }
    }

    private List<JButton> unitButtons(final Map<UnitType, Suggestion> suggestions,
        List<UnitType> have, Colony colony) {
        List<JButton> result = new ArrayList<>(suggestions.size());
        final Comparator<UnitType> buttonComparator
            = Comparator.comparing(ut -> suggestions.get(ut),
                Suggestion.descendingAmountComparator);
        for (UnitType type : sort(suggestions.keySet(), buttonComparator)) {
            boolean present = have.contains(type);
            Suggestion suggestion = suggestions.get(type);
            String label = Integer.toString(suggestion.amount);
            ImageIcon icon
                = new ImageIcon(this.lib.getTinyUnitTypeImage(type, false));
            StringTemplate tip = (suggestion.oldType == null)
                ? stpld("report.colony.wanting")
                .addName("%colony%", colony.getName())
                .addNamed("%unit%", type)
                .addStringTemplate("%location%",
                    suggestion.workLocation.getLabel())
                .addNamed("%goods%", suggestion.goodsType)
                .addAmount("%amount%", suggestion.amount)
                : stpld("report.colony.improving")
                .addName("%colony%", colony.getName())
                .addNamed("%oldUnit%", suggestion.oldType)
                .addNamed("%unit%", type)
                .addStringTemplate("%location%",
                    suggestion.workLocation.getLabel())
                .addNamed("%goods%", suggestion.goodsType)
                .addAmount("%amount%", suggestion.amount);
            JButton b = newButton(colony.getId(), label, icon,
                                  (present) ? cGood : cPlain, tip);
            if (present) b.setFont(b.getFont().deriveFont(Font.BOLD));
            result.add(b);
        }
        return result;
    }

    /**
     * Update several colonies.
     *
     * @param summaries A list of {@code ColonySummary}s to update from.
     */
    private void updateCombinedColonies(List<ColonySummary> summaries) {
        StringTemplate t;

        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL),
            "newline, span, growx");

        // Accumulate all the summaries
        Map<Region, Integer> rRegionMap = new HashMap<>();
        List<TileImprovementSuggestion> rTileSuggestions = new ArrayList<>();
        int rUnitCount = 0, rUnitsToAdd = 0, rUnitsToRemove = 0,
            teacherLen = 0, improveLen = 0;
        double rNewColonist = 0.0;
        Map<GoodsType, ColonySummary.GoodsProduction> rProduction
            = new HashMap<>();
        Map<UnitType, Integer> rTeachers = new HashMap<>();
        //List<Unit> rNotWorking = new ArrayList<>();
        //List<UnitType> rCouldWork = new ArrayList<>();
        Map<UnitType, Integer> rImprove = new HashMap<>();
        Map<GoodsType, Double> rNeeded = new HashMap<>();
        for (ColonySummary s : summaries) {
            accumulateToMap(rRegionMap, s.colony.getTile().getRegion(), 1,
                integerAccumulator);
            rTileSuggestions.addAll(s.tileSuggestions);
            if (s.newColonist > 0) rNewColonist += s.newColonist;
            rUnitCount += s.unitCount;
            rUnitsToAdd += s.unitsToAdd;
            if (s.bonus < 0) {
                rUnitsToRemove += s.unitsToRemove;
            }
            accumulateMap(rProduction, s.production,
                ColonySummary.GoodsProduction.goodsProductionAccumulator);
            teacherLen = Math.max(teacherLen, s.teachers.size());
            for (Unit u : s.teachers.keySet()) {
                accumulateToMap(rTeachers, u.getType(), 1, integerAccumulator);
            }
            //rNotWorking.addAll(s.notWorking);
            //rCouldWork.addAll(s.couldWork);
            improveLen = Math.max(improveLen, s.improve.size() + s.want.size());
            for (UnitType ut : s.improve.keySet()) {
                accumulateToMap(rImprove, ut, 1, integerAccumulator);
            }
            for (UnitType ut : s.want.keySet()) {
                accumulateToMap(rImprove, ut, 1, integerAccumulator);
            }
            if (s.needed != null && s.needed.getType().isStorable()) {
                accumulateToMap(rNeeded, s.needed.getType(),
                    (double)s.needed.getAmount() / s.completeTurns,
                    doubleAccumulator);
            }
        }
        rNewColonist = Math.round(rNewColonist / summaries.size());

        // Field: A label for the most settled region in the list.
        // Colour: Plain
        t = mapEntriesByValue(rRegionMap, descendingIntegerComparator)
            .get(0).getKey().getLabel();
        reportPanel.add(newLabel(Messages.message(t), null, cPlain,
                stpld("report.colony.name.summary")),
            "newline");

        // Field: The total units.
        reportPanel.add(newLabel(Integer.toString(rUnitCount), null, cGood, stpld("report.colony.size.summary")));

        // Field: The total units to add.
        reportPanel.add(newLabel(Integer.toString(rUnitsToAdd), null, cGood, stpld("report.colony.growing.summary")));

        // Field: The total units to remove.
        // Colour: cGood if efficient/cAlarm if inefficient.
        reportPanel.add(newLabel(Integer.toString(rUnitsToRemove), null,
                (rUnitsToRemove > 0) ? cAlarm : cGood,
                stpld("report.colony.shrinking.summary")));

        // Field: The number of potential colony tiles that need
        // exploring.
        // Colour: cAlarm
        Set<Tile> tiles = transform(rTileSuggestions,
            TileImprovementSuggestion::isExploration,
            ts -> ts.tile, Collectors.toSet());
        reportPanel.add((tiles.isEmpty()) ? new JLabel()
            : newLabel(Integer.toString(tiles.size()), null, cAlarm,
                stpld("report.colony.exploring.summary")));

        addTotalTimeImprovements(rTileSuggestions, tiles);
        addTotalGoods(rProduction);

        // Field: New colonist arrival or famine warning.
        // Colour: cWarn if negative, else cGood
        reportPanel.add(newLabel(Integer.toString((int)rNewColonist), null,
                (rNewColonist < 0) ? cWarn : cGood,
                stpld("report.colony.arriving.summary")));

        reportPanel.add(new JLabel()); // not working
        reportPanel.add(new JLabel()); // not working in profession

        // Field: The required goods rates.
        // Colour: cPlain
        List<JLabel> labels = transform(mapEntriesByValue(rNeeded, descendingDoubleComparator),
            alwaysTrue(),
            e -> newLabel(String.format("%4.1f %s", e.getValue(),
                    Messages.getName(e.getKey())),
                null, cPlain,
                stpld("report.colony.making.summary")
                .addNamed("%goods%", e.getKey())));

        // Field: What is being trained (attached to previous)
        // Colour: cPlain.
        teacherLen = Math.max(3, teacherLen); // Always some room here
        labels.addAll(unitTypeLabels(rTeachers, teacherLen,
                stpld("report.colony.making.educating.summary")));
        addTogether(labels);

        // Field: The units that could be upgraded, followed by the units
        // that could be added.
        addTogether(unitTypeLabels(rImprove, improveLen,
                stpld("report.colony.improving.summary")));
    }

    // Fields: The net production of each storable+non-trade-goods
    // goods type.
    // Colour: cWarn if negative, empty if no production,
    // cPlain if production balanced at zero, otherwise cGood.
    private void addTotalGoods(Map<GoodsType, ColonySummary.GoodsProduction> productionMap) {
        for (GoodsType gt : this.goodsTypes) {
            final ColonySummary.GoodsProduction gp = productionMap.get(gt);
            Color c;
            switch (gp.status) {
            case NONE:
                c = null;
                break;
            case INSUFFICIENT_BUILDINGS:
                c = cPlain;
                break;
            case GOOD:
                c = cGood;
                break;
            default:
                throw new IllegalStateException("Bogus status: " + gp.status);
            }
            reportPanel.add((c == null) ? new JLabel()
                : newLabel(Integer.toString(gp.amount), null, c,
                    stpld("report.colony.production.summary")
                    .addNamed("%goods%", gt)));
        }
    }

    // Fields: The number of existing colony tiles that would
    // benefit from improvements.
    // Colour: cAlarm
    private void addTotalTimeImprovements(List<TileImprovementSuggestion> rTileSuggestions, Set<Tile> tiles) {
        for (TileImprovementType ti : spec.getTileImprovementTypeList()) {
            if (ti.isNatural()) continue;
            tiles.clear();
            tiles.addAll(transform(rTileSuggestions,
                                   matchKey(ti, ts -> ts.tileImprovementType),
                    ts -> ts.tile, Collectors.toSet()));
            reportPanel.add((tiles.isEmpty()) ? new JLabel()
                : newLabel(Integer.toString(tiles.size()), null, cAlarm,
                           stpld("report.colony.tile." + ti.getSuffix()
                        + ".summary")));
        }
    }

    private List<JLabel> unitTypeLabels(Map<UnitType, Integer> unitTypeMap,
        int maxSize, StringTemplate t) {
        List<JLabel> result = new ArrayList<>(maxSize);
        int n = 0;
        for (Entry<UnitType, Integer> e
                 : mapEntriesByValue(unitTypeMap, descendingIntegerComparator)) {
            ImageIcon icon
                = new ImageIcon(this.lib.getTinyUnitTypeImage(e.getKey(), false));
            result.add(newLabel(Integer.toString(e.getValue()), icon,
                    cPlain, t));
            if (++n >= maxSize) break;
        }

        return result;
    }

    /**
     * Display the header area for the concise panel.
     *
     * @param market A {@code Market} to check goods arrears
     *     status with.
     */
    private void conciseHeaders(Market market) {
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL),
            "newline, span, growx");

        reportPanel.add(newLabel("report.colony.name.header", null, null,
                stpld("report.colony.name")),
            "newline");

        reportPanel.add(newLabel("report.colony.size.header", null, null,
                stpld("report.colony.size")));

        reportPanel.add(newLabel("report.colony.grow.header", null, null,
                stpld("report.colony.grow")));
        reportPanel.add(newLabel("report.colony.shrink.header", null, null,
                stpld("report.colony.shrink")));

        reportPanel.add(newLabel("report.colony.explore.header", null, null,
                stpld("report.colony.explore")));
        for (TileImprovementType ti : this.spec.getTileImprovementTypeList()) {
            if (ti.isNatural()) continue;
            String key = "report.colony.tile." + ti.getSuffix() + ".header";
            reportPanel.add(newLabel(key, null, null, stpld(key)));
        }
        for (GoodsType gt : this.goodsTypes) {
            ImageIcon icon = new ImageIcon(this.lib.getSmallGoodsTypeImage(gt));
            JLabel l = newLabel(null, icon, null,
                stpl("report.colony.production.header")
                .addNamed("%goods%", gt));
            l.setEnabled(market == null || market.getArrears(gt) <= 0);
            reportPanel.add(l);
        }

        ImageIcon colonist = new ImageIcon(this.lib.getTinyUnitTypeImage(spec.getDefaultUnitType(getMyPlayer())));
        reportPanel.add(newLabel(null, colonist, null, stpld("report.colony.birth")));
        ImageIcon criminal = new ImageIcon(this.lib.getTinyUnitTypeImage(spec.getUnitType("model.unit.pettyCriminal")));
        reportPanel.add(newLabel(null, criminal, null, stpld("report.colony.notWorking")));
        ImageIcon elderStatesman = new ImageIcon(this.lib.getTinyUnitTypeImage(spec.getUnitType("model.unit.elderStatesman")));
        reportPanel.add(newLabel(null, elderStatesman, null, stpld("report.colony.notWorkingInProfession")));

        reportPanel.add(newLabel("report.colony.making.header", null, null, stpld("report.colony.making")));
        reportPanel.add(newLabel("report.colony.improve.header", null, null, stpld("report.colony.improve")));

        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "newline, span, growx");
    }

    /**
     * Update the panel.
     */
    private void update() {
        reportPanel.removeAll();

        // Define the layout, with a column for each goods type.
        StringBuilder sb = new StringBuilder(64);
        sb.append("[l][c][c][c]");
        for (int i = 0; i < this.goodsTypes.size(); i++) sb.append("[c]");
        sb.append("[c][c][c][c][l][l][l]");
        reportPanel.setLayout(new MigLayout("fillx, insets 0, gap 0 0",
                sb.toString(), ""));

        conciseHeaders(this.market);
        List<ColonySummary> summaries = new ArrayList<>();
        for (List<Colony> cs : this.colonies) {
            summaries.clear();
            for (Colony c : cs) {
                ColonySummary s = new ColonySummary(c, this.goodsTypes);
                summaries.add(s);
                updateColony(s);
            }
            if (cs.size() > 1) {
                updateCombinedColonies(summaries);
            }
            conciseHeaders(this.market);
        }
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final Game game = getGame();
        String command = ae.getActionCommand();
        if (command.startsWith(BUILDQUEUE)) {
            command = command.substring(BUILDQUEUE.length());
            Colony colony = game.getFreeColGameObject(command, Colony.class);
            if (colony != null) {
                getGUI().showBuildQueuePanel(colony)
                    .addClosingCallback(() -> { update(); });
                return;
            }
        } else {
            Colony colony = game.getFreeColGameObject(command, Colony.class);
            if (colony != null) {
                getGUI().showColonyPanel(colony, null)
                    .addClosingCallback(() -> { update(); });
                return;
            }
        }
        super.actionPerformed(ae);
    }
}
