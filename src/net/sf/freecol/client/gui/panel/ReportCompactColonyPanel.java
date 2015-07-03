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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Occupation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.model.WorkLocation.Suggestion;
import net.sf.freecol.common.resources.ResourceManager;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This panel displays the compact colony report.
 */
public final class ReportCompactColonyPanel extends ReportPanel
    implements ActionListener {

    /** Container class for all the information about a colony. */
    private static class ColonySummary {

        /** Types of production for a given goods type. */
        public static enum ProductionStatus {
            FAIL,        // Negative production and below low alarm level
            BAD,         // Negative production
            NONE,        // No production at all
            ZERO,        // Production ==  consumption
            GOOD,        // Positive production
            EXPORT,      // Positive production and exporting
            EXCESS,      // Positive production and above high alarm level
            OVERFLOW,    // Positive production and above capacity
        };

        /** Container class for goods production. */
        public static class GoodsProduction {
            public int amount;
            public ProductionStatus status;
            public int extra;

            public GoodsProduction(int amount, ProductionStatus status,
                int extra) {
                this.amount = amount;
                this.status = status;
                this.extra = extra;
            }
        };
            
        /** The colony being summarized. */
        public final Colony colony;

        /**
         * Lists of tiles that could be explored or improved in
         * some way.
         */
        public final List<Tile> exploreTiles = new ArrayList<>();
        public final List<Tile> clearTiles = new ArrayList<>();
        public final List<Tile> plowTiles = new ArrayList<>();
        public final List<Tile> roadTiles = new ArrayList<>();

        /** Famine warning required? */
        public final boolean famine;

        /**
         * Turns to new colonist if non-negative, turns to starvation
         * if negative.
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
         * @param colony The <code>Colony</code> to summarize.
         * @param goodsTypes A list of <code>GoodsType</code>s to
         *     summarize.
         */
        public ColonySummary(Colony colony, List<GoodsType> goodsTypes) {
            this.colony = colony;
            
            final Specification spec = colony.getSpecification();
            final Player owner = colony.getOwner();
            final GoodsType foodType = spec.getPrimaryFoodType();

            colony.getColonyTileTodo(this.exploreTiles, this.clearTiles,
                this.plowTiles, this.roadTiles);
            if (colony.getGoodsCount(foodType) > Settlement.FOOD_PER_COLONIST) {
                this.famine = false;
                this.newColonist = 1;
            } else {
                int newFood = colony.getAdjustedNetProductionOf(foodType);
                this.famine = newFood < 0
                    && (colony.getGoodsCount(foodType) / -newFood)
                        <= Colony.FAMINE_TURNS;
                this.newColonist = (newFood == 0) ? 0
                    : (newFood < 0)
                    ? colony.getGoodsCount(foodType) / newFood - 1
                    : (Settlement.FOOD_PER_COLONIST
                        - colony.getGoodsCount(foodType)) / newFood + 1;
            }

            this.bonus = colony.getProductionBonus();
            
            this.sizeChange = colony.getPreferredSizeChange();

            for (GoodsType gt : goodsTypes) produce(gt);
                
            for (Unit u : colony.getTile().getUnitList()) {
                if (u.getState() != Unit.UnitState.FORTIFIED
                    && u.getState() != Unit.UnitState.SENTRY) {
                    this.notWorking.add(u);
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
            for (WorkLocation wl : colony.getAvailableWorkLocations()) {
                if (!wl.canBeWorked()) continue;
                if (wl.canTeach()) {
                    for (Unit u : wl.getUnitList()) {
                        teachers.put(u, u.getNeededTurnsOfTraining()
                            - u.getTurnsOfTraining());
                    }
                    continue;
                }

                // Check if the units are working.
                for (Unit u : wl.getUnitList()) {
                    if (u.getTeacher() == null && u.getWorkType() == null) {
                        this.notWorking.add(u);
                    }
                }

                // Add work location suggestions.
                for (Entry<Unit, Suggestion> e
                         : wl.getSuggestions().entrySet()) {
                    Unit u = e.getKey();
                    Suggestion s = e.getValue();
                    UnitType expert = spec.getExpertForProducing(s.goodsType);
                    if (u == null) {
                        addSuggestion(this.want, expert, s);
                    } else {
                        addSuggestion(this.improve, expert, s);
                    }
                }
            }
            
            // Make a list of unit types that are not working at their
            // speciality, including the units just standing around.
            for (Unit u : this.notWorking) {
                GoodsType t = u.getWorkType();
                WorkLocation wl = u.getWorkLocation();
                if (wl == null) continue;
                GoodsType w = wl.getWorkFor(u);
                if (w == null || w != t) this.couldWork.add(u.getType());
            }

            this.build = colony.getCurrentlyBuilding();
            AbstractGoods needed = new AbstractGoods();
            if (build == null) {
                this.completeTurns = -1;
            } else {
                int turns = colony.getTurnsToComplete(build, needed);
                if (turns == FreeColObject.UNDEFINED) {
                    needed.setAmount(needed.getAmount()
                        - colony.getGoodsCount(needed.getType()));
                    this.completeTurns = -1;
                } else if (turns >= 0) {
                    needed = null;
                    this.completeTurns = turns;
                } else {
                    needed.setAmount(needed.getAmount()
                        - colony.getGoodsCount(needed.getType()));
                    this.completeTurns = turns - 1;
                }
            }
            this.needed = needed;
        }

        /**
         * Set the production map values for the given goods type.
         *
         * @param goodsType The <code>GoodsType</code> to use.
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
            int extra = 0;
            if (p < 0) {
                status = (amount < low) ? ProductionStatus.FAIL
                    : ProductionStatus.BAD;
                extra = -amount / p + 1;
            } else if (p == 0) {
                status = (colony.getTotalProductionOf(goodsType) == 0)
                    ? ProductionStatus.NONE
                    : ProductionStatus.ZERO;
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

    private static final String BUILDQUEUE = "buildQueue.";
    private static final String cAlarmKey = "color.report.colony.alarm";
    private static final String cWarnKey = "color.report.colony.warning";
    private static final String cPlainKey = "color.report.colony.plain";
    private static final String cExportKey = "color.report.colony.export";
    private static final String cGoodKey = "color.report.colony.good";

    private final Specification spec;
    private final ImageLibrary lib;
    private final List<List<Colony>> colonies = new ArrayList<>();
    private final Market market;
    private final List<GoodsType> goodsTypes = new ArrayList<>();
    private final Color cAlarm;
    private final Color cWarn;
    private final Color cPlain;
    private final Color cExport;
    private final Color cGood;


    /**
     * Creates a colony report.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ReportCompactColonyPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportColonyAction");

        this.spec = getSpecification();
        this.lib = getImageLibrary();
        
        // Sort the colonies by continent.
        final Map<Integer, List<Colony>> continents = new HashMap<>();
        for (Colony c : freeColClient.getMySortedColonies()) {
            appendToMapList(continents, c.getTile().getContiguity(), c);
        }
        for (Entry<Integer, List<Colony>> e
                 : mapEntriesByKey(continents, descendingIntegerComparator)) {
            this.colonies.add(e.getValue());
        }

        this.market = getMyPlayer().getMarket();

        this.goodsTypes.addAll(spec.getGoodsTypeList());
        Iterator<GoodsType> gti = goodsTypes.iterator();
        while (gti.hasNext()) {
            GoodsType gt = gti.next();
            if (!gt.isStorable() || gt.isTradeGoods()) gti.remove();
        }
        Collections.sort(this.goodsTypes, GoodsType.goodsTypeComparator);

        // Load the customized colours, with simple fallbacks.
        this.cAlarm = (ResourceManager.hasColorResource(cAlarmKey))
            ? ResourceManager.getColor(cAlarmKey)
            : Color.RED;
        this.cWarn = (ResourceManager.hasColorResource(cWarnKey))
            ? ResourceManager.getColor(cWarnKey)
            : Color.MAGENTA;
        this.cPlain = (ResourceManager.hasColorResource(cPlainKey))
            ? ResourceManager.getColor(cPlainKey)
            : Color.DARK_GRAY;
        this.cExport = (ResourceManager.hasColorResource(cExportKey))
            ? ResourceManager.getColor(cExportKey)
            : Color.GREEN;
        this.cGood = (ResourceManager.hasColorResource(cGoodKey))
            ? ResourceManager.getColor(cGoodKey)
            : Color.BLUE;

        update();
    }


    private static StringTemplate stpl(String messageId) {
        return StringTemplate.template(messageId);
    }

    private JLabel newLabel(String h, ImageIcon i, Color c, StringTemplate t) {
        if (h != null) h = Messages.message(h);
        JLabel l = new JLabel(h, i, SwingConstants.CENTER);
        l.setForeground((c == null) ? Color.BLACK : c);
        if (t != null) Utility.localizeToolTip(l, t);
        return l;
    }

    private JButton colourButton(String action, String h,
        ImageIcon i, Color c, StringTemplate t) {
        if (h != null && Messages.containsKey(h)) h = Messages.message(h);
        JButton b = Utility.getLinkButton(h, i, action);
        b.setForeground((c == null) ? Color.BLACK : c);
        if (t != null) Utility.localizeToolTip(b, t);
        b.addActionListener(this);
        return b;
    }

    /**
     * Update a single colony.
     *
     * @param colony The <code>Colony</code> to update.
     */
    private void updateColony(Colony colony) {
        final String cac = colony.getId();
        final ColonySummary s = new ColonySummary(colony, this.goodsTypes);
        JButton b;
        Color c;
        StringTemplate t;

        // Field: A button for the colony.
        // Colour: bonus in {-2,2} => {alarm, warn, plain, export, good}
        // Font: Bold if famine is threatening.
        c = (s.bonus <= -2) ? cAlarm
            : (s.bonus == -1) ? cWarn
            : (s.bonus == 0) ? cPlain
            : (s.bonus == 1) ? cExport
            : cGood;
        b = colourButton(cac, colony.getName(), null, c, null);
        if (s.famine) b.setFont(b.getFont().deriveFont(Font.BOLD));
        reportPanel.add(b, "newline");

        // Field: The number of colonists that can be added to a
        // colony without damaging the production bonus, unless
        // the colony is inefficient in which case add the number
        // of colonists to remove to fix the inefficiency.
        // Colour: Blue if efficient/Red if inefficient.
        if (s.sizeChange < 0) {
            c = cAlarm;
            t = stpl("report.colony.shrinking.description")
                    .addName("%colony%", colony.getName())
                    .addAmount("%amount%", -s.sizeChange);
            b = colourButton(cac, Integer.toString(-s.sizeChange), null, c, t);
        } else if (s.sizeChange > 0) {
            c = cGood;
            t = stpl("report.colony.growing.description")
                    .addName("%colony%", colony.getName())
                    .addAmount("%amount%", s.sizeChange);
            b = colourButton(cac, Integer.toString(s.sizeChange), null, c, t);
        } else {
            b = null;
        }
        reportPanel.add((b == null) ? new JLabel() : b);

        // Field: The number of potential colony tiles that need
        // exploring.
        // Colour: Always cAlarm
        if (!s.exploreTiles.isEmpty()) {
            t = stpl("report.colony.exploring.description")
                    .addName("%colony%", colony.getName())
                    .addAmount("%amount%", s.exploreTiles.size());
            b = colourButton(cac, Integer.toString(s.exploreTiles.size()),
                             null, cAlarm, t);
        } else {
            b = null;
        }
        reportPanel.add((b == null) ? new JLabel() : b);

        // Field: The number of existing colony tiles that would
        // benefit from ploughing.
        // Colour: Always cAlarm
        // Font: Bold if one of the tiles is the colony center.
        if (!s.plowTiles.isEmpty()) {
            t = stpl("report.colony.plowing.description")
                    .addName("%colony%", colony.getName())
                    .addAmount("%amount%", s.plowTiles.size());
            b = colourButton(cac, Integer.toString(s.plowTiles.size()),
                             null, cAlarm, t);
            if (s.plowTiles.contains(colony.getTile())) {
                b.setFont(b.getFont().deriveFont(Font.BOLD));
            }
        } else {
            b = null;
        }
        reportPanel.add((b == null) ? new JLabel() : b);

        // Field: The number of existing colony tiles that would
        // benefit from a road.
        // Colour: cAlarm
        if (!s.roadTiles.isEmpty()) {
            t = stpl("report.colony.roadBuilding.description")
                    .addName("%colony%", colony.getName())
                    .addAmount("%amount%", s.roadTiles.size());
            b = colourButton(cac, Integer.toString(s.roadTiles.size()),
                             null, cAlarm, t);
        } else {
            b = null;
        }
        reportPanel.add((b == null) ? new JLabel() : b);

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
                t = stpl("report.colony.production.low.description")
                        .addName("%colony%", colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount)
                        .addAmount("%turns%", gp.extra);
                break;
            case BAD:
                c = cWarn;
                t = stpl("report.colony.production.description")
                        .addName("%colony%", colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount);
                break;
            case NONE:
                c = null;
                t = null;
                break;
            case ZERO:
                c = cPlain;
                t = stpl("report.colony.production.description")
                        .addName("%colony%", colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount);
                break;
            case GOOD:
                c = cGood;
                t = stpl("report.colony.production.description")
                        .addName("%colony%", colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount);
                break;
            case EXPORT:
                c = cExport;
                t = stpl("report.colony.production.export.description")
                        .addName("%colony%", colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount)
                        .addAmount("%export%", gp.extra);
                break;
            case EXCESS:
                c = cWarn;
                t = stpl("report.colony.production.high.description")
                        .addName("%colony%", colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount)
                        .addAmount("%turns%", gp.extra);
                break;
            case OVERFLOW:
                c = cAlarm;
                t = stpl("report.colony.production.waste.description")
                        .addName("%colony%", colony.getName())
                        .addNamed("%goods%", gt)
                        .addAmount("%amount%", gp.amount)
                        .addAmount("%waste%", gp.extra);
                break;
            default:
                throw new IllegalStateException("Bogus status: " + gp.status);
            }
            reportPanel.add((c == null) ? new JLabel()
                : colourButton(cac, Integer.toString(gp.amount), null, c, t));
        }

        // Field: New colonist arrival or famine warning.
        // Colour: cGood if arriving eventually, blank if not enough food
        // to grow, cWarn if negative, cAlarm if famine soon.
        if (s.newColonist > 0) {
            t = stpl("report.colony.arriving.description")
                    .addName("%colony%", colony.getName())
                    .addNamed("%unit%",
                        spec.getDefaultUnitType(colony.getOwner()))
                    .addAmount("%turns%", s.newColonist);
            b = colourButton(cac, Integer.toString(s.newColonist),
                             null, cGood, t);
        } else if (s.newColonist < 0) {
            c = (s.famine) ? cAlarm : cWarn;
            t = stpl("report.colony.starving.description")
                    .addName("%colony%", colony.getName())
                    .addAmount("%turns%", -s.newColonist);
            b = colourButton(cac, Integer.toString(-s.newColonist),
                             null, c, t);
            if (s.famine) b.setFont(b.getFont().deriveFont(Font.BOLD));
        } else {
            b = null;
        }
        reportPanel.add((b == null) ? new JLabel() : b);

        // Field: What is currently being built (clickable if on the
        // buildqueue) and the turns until it completes, including
        // units being taught.
        // Colour: cAlarm bold "Nothing" if nothing being built, cAlarm
        // with no turns if no production, cGood with turns if
        // completing, cAlarm with turns if will block, turns
        // indicates when blocking occurs.
        BuildableType build = s.build;
        int fields = 1 + s.teachers.size();
        String layout = (fields > 1) ? "split " + fields : null;
        final String qac = BUILDQUEUE + cac;
        if (build == null) {
            t = stpl("report.colony.making.noconstruction.description")
                    .addName("%colony%", colony.getName());
            b = colourButton(qac, Messages.message("nothing"),
                             null, cAlarm, t);
            b.setFont(b.getFont().deriveFont(Font.BOLD));
        } else {
            AbstractGoods needed = s.needed;
            int turns = s.completeTurns;
            String name = Messages.getName(build);
            if (turns == FreeColObject.UNDEFINED) {
                t = stpl("report.colony.making.noconstruction.description")
                        .addName("%colony%", colony.getName());
                b = colourButton(qac, name, null, cAlarm, t);
            } else if (turns >= 0) {
                t = stpl("report.colony.making.constructing.description")
                        .addName("%colony%", colony.getName())
                        .addNamed("%buildable%", build)
                        .addAmount("%turns%", turns);
                b = colourButton(qac, name + " " + Integer.toString(turns),
                    null, cGood, t);
            } else if (turns < 0) {
                t = stpl("report.colony.making.blocking.description")
                        .addName("%colony%", colony.getName())
                        .addAmount("%amount%", needed.getAmount())
                        .addNamed("%goods%", needed.getType())
                        .addNamed("%buildable%", build)
                        .addAmount("%turns%", -turns - 1);
                b = colourButton(qac, name + " " + Integer.toString(-turns-1),
                                 null, cAlarm, t);
                if (turns == -1) b.setFont(b.getFont().deriveFont(Font.BOLD));
            }
        }
        reportPanel.add(b, layout);

        // Field: What is being trained.
        // Colour: cAlarm if completion is blocked, otherwise cPlain.
        layout = null;
        for (Entry<Unit, Integer> e
                 : mapEntriesByValue(s.teachers, descendingIntegerComparator)) {
            Unit u = e.getKey();
            ImageIcon ii
                = new ImageIcon(this.lib.getTinyUnitImage(u.getType(), true));
            if (e.getValue() <= 0) {
                t = stpl("report.colony.making.noteach.description")
                        .addName("%colony%", colony.getName())
                        .addStringTemplate("%teacher%",
                            u.getLabel(Unit.UnitLabelType.NATIONAL));
                b = colourButton(cac, Integer.toString(0), ii, cAlarm, t);
            } else {
                t = stpl("report.colony.making.educating.description")
                        .addName("%colony%", colony.getName())
                        .addStringTemplate("%teacher%",
                            u.getLabel(Unit.UnitLabelType.NATIONAL))
                        .addAmount("%turns%", e.getValue());
                b = colourButton(cac, Integer.toString(e.getValue()),
                                 ii, cPlain, t);
            }
            reportPanel.add(b);
        }

        // Field: The units that could be upgraded, followed by the units
        // that could be added.
        if (s.improve.isEmpty() && s.want.isEmpty()) {
            reportPanel.add(new JLabel());
        } else {
            List<JComponent> buttons = new ArrayList<>();
            buttons.addAll(addUnits(s.improve, s.couldWork, colony));
            buttons.add(new JLabel("/"));
            buttons.addAll(addUnits(s.want, s.couldWork, colony));
            layout = "split " + (s.improve.size() + s.want.size() + 1);
            for (JComponent jc : buttons) {
                reportPanel.add(jc, layout);
                layout = null;
            }
        }

        // TODO: notWorking?
    }

    private List<JButton> addUnits(final Map<UnitType, Suggestion> suggestions,
                                   List<UnitType> have, Colony colony) {
        final String cac = colony.getId();

        List<JButton> result = new ArrayList<>();
        List<UnitType> types = new ArrayList<>();
        types.addAll(suggestions.keySet());
        Collections.sort(types, new Comparator<UnitType>() {
                @Override
                public int compare(UnitType t1, UnitType t2) {
                    int cmp = suggestions.get(t2).amount
                        - suggestions.get(t1).amount;
                    return (cmp != 0) ? cmp
                        : t1.getId().compareTo(t2.getId());
                }
            });
        for (UnitType type : types) {
            boolean present = have.contains(type);
            Suggestion suggestion = suggestions.get(type);
            String label = Integer.toString(suggestion.amount);
            ImageIcon icon
                = new ImageIcon(this.lib.getTinyUnitImage(type, false));
            StringTemplate tip = (suggestion.oldType == null)
                ? stpl("report.colony.wanting.description")
                    .addName("%colony%", colony.getName())
                    .addNamed("%unit%", type)
                    .addNamed("%goods%", suggestion.goodsType)
                    .addAmount("%amount%", suggestion.amount)
                : stpl("report.colony.improving.description")
                    .addName("%colony%", colony.getName())
                    .addNamed("%oldUnit%", suggestion.oldType)
                    .addNamed("%unit%", type)
                    .addNamed("%goods%", suggestion.goodsType)
                    .addAmount("%amount%", suggestion.amount);
            JButton b = colourButton(cac, label, icon,
                                     (present) ? cGood : cPlain, tip);
            if (present) b.setFont(b.getFont().deriveFont(Font.BOLD));
            result.add(b);
        }
        return result;
    }

    /**
     * Display the header area for the concise panel.
     *
     * @param market A <code>Market</code> to check goods arrears
     *     status with.
     */
    private void conciseHeaders(Market market) {
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
        for (GoodsType gt : this.goodsTypes) {
            ImageIcon icon = new ImageIcon(this.lib.getSmallIconImage(gt));
            JLabel l = newLabel(null, icon, null,
                                stpl("report.colony.production.header")
                                    .addNamed("%goods%", gt));
            l.setEnabled(market == null || market.getArrears(gt) <= 0);
            reportPanel.add(l);
        }

        final UnitType type = spec.getDefaultUnitType(market.getOwner());
        ImageIcon colonistIcon
            = new ImageIcon(this.lib.getTinyUnitImage(type, true));
        reportPanel.add(newLabel(null, colonistIcon, null,
                                 stpl("report.colony.birth.description")));
        reportPanel.add(newLabel("report.colony.making.header", null, null,
                                 stpl("report.colony.making.description")));
        reportPanel.add(newLabel("report.colony.improve.header", null, null,
                                 stpl("report.colony.improve.description")));

        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL),
                        "newline, span, growx");
    }

    /**
     * Update the panel.
     */
    private void update() {
        reportPanel.removeAll();

        // Define the layout, with a column for each goods type.
        String cols = "[l][c][c][c]";
        for (int i = 0; i < this.goodsTypes.size(); i++) cols += "[c]";
        cols += "[c][c][l][l][l]";
        reportPanel.setLayout(new MigLayout("fillx, insets 0, gap 0 0",
                                            cols, ""));

        conciseHeaders(market);
        for (List<Colony> cs : this.colonies) {
            for (Colony c : cs) {
                // Do not include colonies that have been
                // abandoned but are still on the colonies list.
                if (c.getUnitCount() > 0) updateColony(c);
            }
            conciseHeaders(market);
        }
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        final Game game = getGame();
        String command = event.getActionCommand();
        if (command.startsWith(BUILDQUEUE)) {
            command = command.substring(BUILDQUEUE.length());
            Colony colony = game.getFreeColGameObject(command, Colony.class);
            if (colony != null) {
                getGUI().showBuildQueuePanel(colony, new Runnable() {
                        @Override
                        public void run() {
                            update();
                        }
                    });
                return;
            }
        } else {
            Colony colony = game.getFreeColGameObject(command, Colony.class);
            if (colony != null) {
                getGUI().showColonyPanel(colony, null)
                    .addClosingCallback(new Runnable() {
                            @Override
                            public void run() {
                                update();
                            }
                        });
                return;
            }
        }
        super.actionPerformed(event);
    }
}
