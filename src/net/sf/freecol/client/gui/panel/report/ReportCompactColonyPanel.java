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

package net.sf.freecol.client.gui.panel.report;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Colony.TileImprovementSuggestion;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ProductionInfo;
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
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This panel displays the compact colony report.
 */
public final class ReportCompactColonyPanel extends ReportPanel {

    /** Predicate to select units that are not working. */
    private static final Predicate<Unit> notWorkingPred = u ->
        u.getState() != Unit.UnitState.FORTIFIED && u.getState() != Unit.UnitState.SENTRY;
    
    private static final JLabel emptyLabel = new JLabel();

    /** Container class for all the information about a colony. */
    private static class ColonySummary {

        /** Types of production for a given goods type. */
        public static enum ProductionStatus {
            FAIL,        // Negative production and below low alarm level
            BAD,         // Negative production
            NONE,        // No production at all
            ZERO,        // Production == consumption
            GOOD,        // Positive production
            EXPORT,      // Positive production and exporting
            EXCESS,      // Positive production and above high alarm level
            OVERFLOW,    // Positive production and above capacity
            PRODUCTION,  // Positive production but could produce more
            CONSUMPTION, // Positive production but could consume more
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
                    : (this.amount < 0) ? ProductionStatus.BAD
                    : (this.amount > 0) ? ProductionStatus.GOOD
                    : ProductionStatus.ZERO;
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
        
        /** Preferred size change. */
        public final int sizeChange;

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
            
            this.sizeChange = colony.getPreferredSizeChange();

            for (GoodsType gt : goodsTypes) produce(gt);

            this.notWorking.addAll(transform(colony.getTile().getUnits(),
                                             notWorkingPred));

            // Collect the types of the units at work in the colony
            // (colony tiles and buildings) that are suboptimal (and
            // are not just temporarily there because they are being
            // taught), the types for sites that really need a new
            // unit, the teachers, and the units that are not working.
            //
            // FIXME: this needs to be merged with the requirements
            // checking code, but that in turn should be opened up
            // so the AI can use it...
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

        /**
         * Set the production map values for the given goods type.
         *
         * @param goodsType The {@code GoodsType} to use.
         */
        private void produce(GoodsType goodsType) {
            final ExportData exportData = colony.getExportData(goodsType);
            final int adjustment = colony.getWarehouseCapacity()
                / GoodsContainer.CARGO_SIZE;
            final int low = exportData.getLowLevel() * adjustment;
            final int high = exportData.getHighLevel() * adjustment;
            final int amount = colony.getGoodsCount(goodsType);
            int p = colony.getAdjustedNetProductionOf(goodsType);

            ProductionStatus status;
            AbstractGoods deficit;
            int extra = 0;
            if (p < 0) {
                status = (amount < low) ? ProductionStatus.FAIL
                    : ProductionStatus.BAD;
                extra = -amount / p + 1;
            } else if (p == 0 && !colony.isProducing(goodsType)) {
                status = ProductionStatus.NONE;
            } else if (p == 0) {
                status = ProductionStatus.ZERO;
                extra = 0;
                deficit = null;
                for (WorkLocation wl : colony.getWorkLocationsForProducing(goodsType)) {
                    ProductionInfo pi = colony.getProductionInfo(wl);
                    if (pi == null) continue;
                    deficit = find(pi.getConsumptionDeficit(),
                                   AbstractGoods.matches(goodsType));
                    if (deficit != null) {
                        status = ProductionStatus.CONSUMPTION;
                        extra = deficit.getAmount();
                        break;
                    }
                }
            } else if (exportData.getExported()) {
                status = ProductionStatus.EXPORT;
                extra = exportData.getExportLevel();
            } else if (goodsType.limitIgnored()) {
                status = ProductionStatus.GOOD;
            } else if (amount + p > colony.getWarehouseCapacity()) {
                status = ProductionStatus.OVERFLOW;
                extra = amount + p - colony.getWarehouseCapacity();
            } else if (amount >= high) {
                status = ProductionStatus.EXCESS;
                extra = (colony.getWarehouseCapacity() - amount) / p;
            } else {
                status = ProductionStatus.GOOD;
                extra = 0;
                deficit = null;
                for (WorkLocation wl : colony.getWorkLocationsForProducing(goodsType)) {
                    ProductionInfo pi = colony.getProductionInfo(wl);
                    if (pi == null) continue;
                    deficit = find(pi.getProductionDeficit(),
                                   AbstractGoods.matches(goodsType));
                    if (deficit != null) {
                        status = ProductionStatus.PRODUCTION;
                        extra = deficit.getAmount();
                        break;
                    }
                }
            }
            this.production.put(goodsType,
                new GoodsProduction(p, status, extra));
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

    /** Predicate to select the goods to report on. */
    private static final Predicate<GoodsType> reportGoodsPred = gt ->
        gt.isStorable() && !gt.isTradeGoods();
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
            reportPanel.add(emptyLabel);
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
        final String cac = s.colony.getId();
        final UnitType defaultUnitType
            = spec.getDefaultUnitType(s.colony.getOwner());
        List<JComponent> buttons = new ArrayList<>(16);
        JButton b;
        Color c;
        StringTemplate t;
        Building building;

        // Field: A button for the colony.
        // Colour: bonus in {-2,2} => {alarm, warn, plain, export, good}
        // Font: Bold if famine is threatening.
        c = (s.bonus <= -2) ? cAlarm
            : (s.bonus == -1) ? cWarn
            : (s.bonus == 0) ? cPlain
            : (s.bonus == 1) ? cExport
            : cGood;
        String annotations = "", key;
        t = StringTemplate.label(",");
        if ((building = s.colony.getStockade()) == null) {
            key = "annotation.unfortified";
            t.add(Messages.message("report.colony.annotation.unfortified"));
        } else {
            key = "annotation." + building.getType().getSuffix();
            t.add(Messages.message(building.getLabel()));
        }
        if (ResourceManager.hasStringResource(key))
            annotations += ResourceManager.getString(key);
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
        if (ResourceManager.hasStringResource(key))
            annotations += ResourceManager.getString(key);
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
        b = newButton(cac, s.colony.getName() + annotations, null, c,
            StringTemplate.label(": ").add(s.colony.getName())
                .add(Messages.message(t)));
        if (s.famine) b.setFont(b.getFont().deriveFont(Font.BOLD));
        reportPanel.add(b, "newline");

        // Field: The number of colonists that can be added to a
        // colony without damaging the production bonus, unless
        // the colony is inefficient in which case add the number
        // of colonists to remove to fix the inefficiency.
        // Colour: Blue if efficient/Red if inefficient.
        if (s.sizeChange < 0) {
            c = cAlarm;
            t = stpld("report.colony.shrinking")
                    .addName("%colony%", s.colony.getName())
                    .addAmount("%amount%", -s.sizeChange);
            b = newButton(cac, Integer.toString(-s.sizeChange), null, c, t);
        } else if (s.sizeChange > 0) {
            c = cGood;
            t = stpld("report.colony.growing")
                    .addName("%colony%", s.colony.getName())
                    .addAmount("%amount%", s.sizeChange);
            b = newButton(cac, Integer.toString(s.sizeChange), null, c, t);
        } else {
            b = null;
        }
        reportPanel.add((b == null) ? emptyLabel : b);

        // Field: The number of potential colony tiles that need
        // exploring.
        // Colour: Always cAlarm
        int n = count(s.tileSuggestions,
                      TileImprovementSuggestion::isExploration);
        if (n > 0) {
            t = stpld("report.colony.exploring")
                    .addName("%colony%", s.colony.getName())
                    .addAmount("%amount%", n);
            b = newButton(cac, Integer.toString(n), null, cAlarm, t);
        } else {
            b = null;
        }
        reportPanel.add((b == null) ? emptyLabel : b);

        // Fields: The number of existing colony tiles that would
        // benefit from improvements.
        // Colour: Always cAlarm
        // Font: Bold if one of the tiles is the colony center.
        for (TileImprovementType ti : spec.getTileImprovementTypeList()) {
            if (ti.isNatural()) continue;
            n = 0;
            boolean center = false; 
            for (TileImprovementSuggestion tis : s.tileSuggestions) {
                if (tis.tileImprovementType == ti) {
                    n++;
                    if (tis.tile == s.colony.getTile()) center = true;
                }
            }
            if (n > 0) {
                c = cAlarm;
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
                b = newButton(cac, Integer.toString(n), null, c, t);
                if (center) b.setFont(b.getFont().deriveFont(Font.BOLD));
            } else {
                b = null;
            }
            reportPanel.add((b == null) ? emptyLabel : b);
        }

        // Fields: The net production of each storable+non-trade-goods
        // goods type.
        // Colour: cAlarm if too low, cWarn if negative, empty if no
        // production, cPlain if production balanced at zero,
        // otherwise must be positive, wherein cExport
        // if exported, cAlarm if too high, else cGood.
        for (GoodsType gt : this.goodsTypes) {
            final ColonySummary.GoodsProduction gp = s.production.get(gt);
            switch (gp.status) {
            case FAIL:
                c = cAlarm;
                t = stpld("report.colony.production.low")
                        .addName("%colony%", s.colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", -gp.amount)
                        .addAmount("%turns%", gp.extra);
                break;
            case BAD:
                c = cWarn;
                t = stpld("report.colony.production")
                        .addName("%colony%", s.colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount);
                break;
            case NONE:
                c = null;
                t = null;
                break;
            case ZERO:
                c = cPlain;
                t = stpld("report.colony.production")
                        .addName("%colony%", s.colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount);
                break;
            case GOOD:
                c = cGood;
                t = stpld("report.colony.production")
                        .addName("%colony%", s.colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount);
                break;
            case EXPORT:
                c = cExport;
                t = stpld("report.colony.production.export")
                        .addName("%colony%", s.colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount)
                        .addAmount("%export%", gp.extra);
                break;
            case EXCESS:
                c = cWarn;
                t = stpld("report.colony.production.high")
                        .addName("%colony%", s.colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount)
                        .addAmount("%turns%", gp.extra);
                break;
            case OVERFLOW:
                c = cAlarm;
                t = stpld("report.colony.production.waste")
                        .addName("%colony%", s.colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount)
                        .addAmount("%waste%", gp.extra);
                break;
            case PRODUCTION:
                c = cWarn;
                t = stpld("report.colony.production.maxProduction")
                        .addName("%colony%", s.colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount)
                        .addAmount("%more%", gp.extra);
                break;
            case CONSUMPTION:
                c = cWarn;
                t = stpld("report.colony.production.maxConsumption")
                        .addName("%colony%", s.colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount)
                        .addAmount("%more%", gp.extra);
                break;
            default:
                throw new IllegalStateException("Bogus status: " + gp.status);
            }
            reportPanel.add((c == null) ? emptyLabel
                : newButton(cac, Integer.toString(gp.amount), null, c, t));
        }

        // Field: New colonist arrival or famine warning.
        // Colour: cGood if arriving eventually, blank if not enough food
        // to grow, cWarn if negative, cAlarm if famine soon.
        if (s.newColonist > 0) {
            t = stpld("report.colony.arriving")
                    .addName("%colony%", s.colony.getName())
                    .addNamed("%unit%", defaultUnitType)
                    .addAmount("%turns%", s.newColonist);
            b = newButton(cac, Integer.toString(s.newColonist), null,
                          cGood, t);
        } else if (s.newColonist < 0) {
            c = (s.famine) ? cAlarm : cWarn;
            t = stpld("report.colony.starving")
                    .addName("%colony%", s.colony.getName())
                    .addAmount("%turns%", -s.newColonist);
            b = newButton(cac, Integer.toString(-s.newColonist), null,
                          c, t);
            if (s.famine) b.setFont(b.getFont().deriveFont(Font.BOLD));
        } else {
            b = null;
        }
        reportPanel.add((b == null) ? emptyLabel : b);

        // Field: What is currently being built (clickable if on the
        // buildqueue) and the turns until it completes, including
        // units being taught, or blank if nothing queued.
        // Colour: cWarn if no construction is occurring, cGood with
        // turns if completing, cAlarm with turns if will block, turns
        // indicates when blocking occurs.
        // Font: Bold if blocked right now.
        final String qac = BUILDQUEUE + cac;
        if (s.build != null) {
            int turns = s.completeTurns;
            String bname = Messages.getName(s.build);
            if (turns == UNDEFINED) {
                t = stpld("report.colony.making.noconstruction")
                        .addName("%colony%", s.colony.getName());
                b = newButton(qac, bname, null, cWarn, t);
            } else if (turns >= 0) {
                t = stpld("report.colony.making.constructing")
                        .addName("%colony%", s.colony.getName())
                        .addNamed("%buildable%", s.build)
                        .addAmount("%turns%", turns);
                b = newButton(qac, bname + " " + Integer.toString(turns), null,
                              cGood, t);
            } else { // turns < 0
                turns = -(turns + 1);
                t = stpld("report.colony.making.blocking")
                        .addName("%colony%", s.colony.getName())
                        .addAmount("%amount%", s.needed.getAmount())
                        .addNamed("%goods%", s.needed.getType())
                        .addNamed("%buildable%", s.build)
                        .addAmount("%turns%", turns);
                b = newButton(qac, bname + " " + Integer.toString(turns),
                              null, cAlarm, t);
                if (turns == 0) b.setFont(b.getFont().deriveFont(Font.BOLD));
            }
            buttons.add(b);
        }

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
                b = newButton(cac, Integer.toString(0), ii, cAlarm, t);
            } else {
                t = stpld("report.colony.making.educating")
                        .addName("%colony%", s.colony.getName())
                        .addStringTemplate("%teacher%",
                            u.getLabel(Unit.UnitLabelType.NATIONAL))
                        .addAmount("%turns%", e.getValue());
                b = newButton(cac, Integer.toString(e.getValue()), ii,
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
                buttons.add(newButton(cac, "", emptyIcon, cPlain, t));
            }
        }
        addTogether(buttons);

        // Field: The units that could be upgraded, followed by the units
        // that could be added.
        if (s.improve.isEmpty() && s.want.isEmpty()) {
            reportPanel.add(emptyLabel);
        } else {
            buttons.clear();
            buttons.addAll(unitButtons(s.improve, s.couldWork, s.colony));
            buttons.add(new JLabel("/"));
            // Prefer to suggest an improvement over and addition.
            for (UnitType ut : s.improve.keySet()) s.want.remove(ut);
            buttons.addAll(unitButtons(s.want, s.couldWork, s.colony));
            addTogether(buttons);
        }

        // TODO: notWorking?
    }
    
    private List<JButton> unitButtons(final Map<UnitType, Suggestion> suggestions,
                                      List<UnitType> have, Colony colony) {
        final String cac = colony.getId();
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
            JButton b = newButton(cac, label, icon,
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
        JLabel l;
        Color c;
        StringTemplate t;

        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL),
                        "newline, span, growx");

        // Accumulate all the summaries
        Map<Region, Integer> rRegionMap = new HashMap<>();
        List<TileImprovementSuggestion> rTileSuggestions = new ArrayList<>();
        int rFamine = 0, rBonus = 0, rSizeChange = 0,
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
            if (s.famine) rFamine++;
            if (s.newColonist > 0) rNewColonist += s.newColonist;
            rBonus += s.bonus;
            rSizeChange += s.sizeChange;
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

        // Field: The total of the size change field.
        // Colour: cGood if efficient/cAlarm if inefficient.
        reportPanel.add(newLabel(Integer.toString(rSizeChange), null,
                                 (rSizeChange < 0) ? cAlarm : cGood,
                                 stpld("report.colony.growing.summary")));

        // Field: The number of potential colony tiles that need
        // exploring.
        // Colour: cAlarm
        Set<Tile> tiles = transform(rTileSuggestions,
                                    TileImprovementSuggestion::isExploration,
                                    ts -> ts.tile, Collectors.toSet());
        reportPanel.add((tiles.isEmpty()) ? emptyLabel
            : newLabel(Integer.toString(tiles.size()), null, cAlarm,
                       stpld("report.colony.exploring.summary")));

        // Fields: The number of existing colony tiles that would
        // benefit from improvements.
        // Colour: cAlarm
        for (TileImprovementType ti : spec.getTileImprovementTypeList()) {
            if (ti.isNatural()) continue;
            tiles.clear();
            tiles.addAll(transform(rTileSuggestions,
                                   matchKey(ti, ts -> ts.tileImprovementType),
                                   ts -> ts.tile, Collectors.toSet()));
            reportPanel.add((tiles.isEmpty()) ? emptyLabel
                : newLabel(Integer.toString(tiles.size()), null, cAlarm,
                           stpld("report.colony.tile." + ti.getSuffix()
                               + ".summary")));
        }

        // Fields: The net production of each storable+non-trade-goods
        // goods type.
        // Colour: cWarn if negative, empty if no production,
        // cPlain if production balanced at zero, otherwise cGood.
        for (GoodsType gt : this.goodsTypes) {
            final ColonySummary.GoodsProduction gp = rProduction.get(gt);
            switch (gp.status) {
            case BAD:
                c = cWarn;
                break;
            case NONE:
                c = null;
                break;
            case ZERO:
                c = cPlain;
                break;
            case GOOD:
                c = cGood;
                break;
            default:
                throw new IllegalStateException("Bogus status: " + gp.status);
            }
            reportPanel.add((c == null) ? emptyLabel
                : newLabel(Integer.toString(gp.amount), null, c,
                    stpld("report.colony.production.summary")
                        .addNamed("%goods%", gt)));
        }

        // Field: New colonist arrival or famine warning.
        // Colour: cWarn if negative, else cGood
        reportPanel.add(newLabel(Integer.toString((int)rNewColonist), null,
                                 (rNewColonist < 0) ? cWarn : cGood,
                                 stpld("report.colony.arriving.summary")));

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

        reportPanel.add(newLabel("report.colony.grow.header", null, null,
                                 stpld("report.colony.grow")));
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

        final UnitType type = spec.getDefaultUnitType(getMyPlayer());
        ImageIcon colonistIcon
            = new ImageIcon(this.lib.getTinyUnitTypeImage(type));
        reportPanel.add(newLabel(null, colonistIcon, null,
                                 stpld("report.colony.birth")));
        reportPanel.add(newLabel("report.colony.making.header", null, null,
                                 stpld("report.colony.making")));
        reportPanel.add(newLabel("report.colony.improve.header", null, null,
                                 stpld("report.colony.improve")));

        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL),
                        "newline, span, growx");
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
        sb.append("[c][c][l][l][l]");
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
