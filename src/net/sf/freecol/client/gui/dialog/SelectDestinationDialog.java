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

package net.sf.freecol.client.gui.dialog;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitChangeType;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.pathfinding.GoalDeciders.MultipleAdjacentDecider;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Select a location as the destination for a given unit.
 */
public final class SelectDestinationDialog extends FreeColDialog<Location>
    implements ListSelectionListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SelectDestinationDialog.class.getName());


    /**
     * A container for a destination location, with associated
     * distance and extra characteristics.
     */
    private static class Destination {

        /** Basic destination comparator, that just uses the name field. */
        private static Comparator<Destination> nameComparator
            = new Comparator<Destination>() {
                    public int compare(Destination d1, Destination d2) {
                        if (!(d1.location instanceof Settlement)) return -1;
                        if (!(d2.location instanceof Settlement)) return 1;
                        return d1.name.compareTo(d2.name);
                    }
                };

        /** Distance comparator, uses turns field then falls back to names. */
        private static Comparator<Destination> distanceComparator
            = Comparator.comparingInt(Destination::getTurns)
                .thenComparing(Destination.nameComparator);

        /** Owner comparator, uses the owner field then falls back to names. */
        private static Comparator<Destination> ownerComparator
            = Comparator.comparingInt(Destination::getScore)
                .thenComparing(Destination.nameComparator);

        /** The unit that is travelling. */
        public final Unit unit;

        /** The location to travel to. */
        public final Location location;

        /** The displayed name of the location (needed in text and comparator)*/
        public final String name;

        /** The number of turns to reach the destination. */
        public final int turns;

        /** Extra special information about the destination */
        public final String extras;

        /** A magic score for the destination for the comparator/s. */
        public final int score;

        /** The full text to display. */
        public final String text;


        /**
         * Create a destination.
         *
         * @param location The {@code Location} to go to.
         * @param turns The number of turns it takes to get to the location.
         * @param unit The {@code Unit} that is moving.
         * @param goodsTypes A list of goods types the unit is carrying.
         */
        public Destination(Location location, int turns, Unit unit,
                           List<GoodsType> goodsTypes) {
            this.unit = unit;
            this.location = location;
            this.name = Messages.message(location.getLocationLabelFor(unit.getOwner()));
            this.turns = turns;
            this.extras = getExtras(location, unit, goodsTypes);
            this.score = calculateScore();
            this.text = Messages.message(StringTemplate
                .template("selectDestinationDialog.destinationTurns")
                .addName("%location%", this.name)
                .addAmount("%turns%", this.turns)
                .addName("%extras%", this.extras));
        }

        /**
         * Collected extra annotations of interest to a unit proposing to
         * visit a location.
         *
         * @param loc The {@code Location} to visit.
         * @param unit The {@code Unit} proposing to visit.
         * @param goodsTypes A list of goods types the unit is carrying.
         * @return A string containing interesting annotations about the visit
         *         or an empty string if nothing is of interest.
         */
        private String getExtras(Location loc, Unit unit,
                                 List<GoodsType> goodsTypes) {
            final String sep = ", ";
            final Player owner = unit.getOwner();
            LogBuilder lb = new LogBuilder(32);
            boolean dropSep = false;

            // Always show our missions, it may influence our choice of
            // units to bring, and/or goods.
            if (loc instanceof IndianSettlement
                && ((IndianSettlement)loc).hasMissionary(owner)) {
                lb.add(ResourceManager.getString("cross"));
            }

            if (loc instanceof Europe && !goodsTypes.isEmpty()) {
                Market market = owner.getMarket();
                for (GoodsType goodsType : goodsTypes) {
                    lb.add(Messages.getName(goodsType), " ",
                           market.getSalePrice(goodsType, 1), sep);
                    dropSep = true;
                }

            } else if (loc instanceof Settlement
                && owner.owns((Settlement)loc)) {
                ; // Do nothing

            } else if (loc instanceof Settlement
                && ((Settlement)loc).getOwner().atWarWith(owner)) {
                lb.add("[", Messages.getName(Stance.WAR), "]");

            } else if (loc instanceof Settlement) {
                if (loc instanceof IndianSettlement) {
                    // Show skill if relevant
                    IndianSettlement is = (IndianSettlement)loc;
                    UnitType sk = is.getLearnableSkill();
                    if (sk != null) {
                        final Predicate<Unit> upgradePred = u ->
                            u.getUnitChange(UnitChangeType.NATIVES) != null;
                        Unit up = (unit.isCarrier())
                            ? find(unit.getUnits(), upgradePred)
                            : (upgradePred.test(unit)) ? unit
                            : null;
                        if (up != null) {
                            lb.add("[", Messages.getName(sk), "]");
                        }
                    }
                }
                if (!goodsTypes.isEmpty()) {
                    // Show goods prices if relevant
                    for (GoodsType g : goodsTypes) {
                        String sale = owner.getLastSaleString(loc, g);
                        String more = null;
                        if (loc instanceof IndianSettlement) {
                            IndianSettlement is = (IndianSettlement)loc;
                            more = (g == is.getWantedGoods(0)) ? "***"
                                : (g == is.getWantedGoods(1)) ? "**"
                                : (g == is.getWantedGoods(2)) ? "*"
                                : null;
                        }
                        if (sale != null && more != null) {
                            lb.add(Messages.getName(g), " ", sale, more, sep);
                            dropSep = true;
                        }
                    }
                }
            } // else do nothing

            if (dropSep) lb.shrink(sep);
            return lb.toString();
        }

        private int calculateScore() {
            return (location instanceof Europe || location instanceof Map)
                ? 10
                : (location instanceof Colony)
                ? ((unit.getOwner().owns((Colony)location)) ? 20 : 30)
                : (location instanceof IndianSettlement)
                ? 40
                : 100;
        }

        public int getTurns() {
            return this.turns;
        }

        public int getScore() {
            return this.score;
        }


        public static int getDestinationComparatorIndex(Comparator<Destination> dc) {
            return (dc == nameComparator) ? 1
                : (dc == distanceComparator) ? 2
                : 0;
        }

        public static Comparator<Destination> getDestinationComparator(int index) {
            return (index == 1) ? nameComparator
                : (index == 2) ? distanceComparator
                : ownerComparator;
        }
    }

    private static class LocationRenderer
        extends FreeColComboBoxRenderer<Destination> {

        private final ImageLibrary lib;
        
        /**
         * Create a new location renderer.
         *
         * @param lib The {@code ImageLibrary} to use to make icons.
         */
        public LocationRenderer(ImageLibrary lib) {
            this.lib = lib;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setLabelValues(JLabel label, Destination value) {
            label.setText(value.text);
            label.setIcon(value.location.getLocationImage(CELL_HEIGHT, this.lib));
        }
    }

    /** The size of each destination cell. */
    private static final int CELL_HEIGHT = 48;

    /** Show only the player colonies.  FIXME: make a client option. */
    private static AtomicBoolean showOnlyMyColonies = new AtomicBoolean(true);

    /** How to order the destinations. */
    private static AtomicReference<Comparator<Destination>>
        destinationComparator = new AtomicReference<>(null);
    
    /** The available destinations. */
    private final List<Destination> destinations = new ArrayList<>();

    /** The list of destinations. */
    private final JList<Destination> destinationList;

    /** Restrict to only the player colonies? */
    private JCheckBox onlyMyColoniesBox;

    /** Choice of the comparator. */
    private JComboBox<String> comparatorBox;


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     * @param unit The {@code Unit} to plan for.
     */
    public SelectDestinationDialog(FreeColClient freeColClient, JFrame frame,
                                   Unit unit) {
        super(freeColClient, frame);

        final ImageLibrary lib = getImageLibrary();
        // Collect the goods the unit is carrying and set this.destinations.
        final List<GoodsType> goodsTypes
            = transform(unit.getCompactGoodsList(), alwaysTrue(),
                        Goods::getType);
        loadDestinations(unit, goodsTypes);

        DefaultListModel<Destination> model
            = new DefaultListModel<>();
        this.destinationList = new JList<>(model);
        this.destinationList.setCellRenderer(new LocationRenderer(lib));
        this.destinationList.setFixedCellHeight(CELL_HEIGHT);
        this.destinationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.destinationList.addListSelectionListener(this);
        this.destinationList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() != 2) return;
                    Destination d = destinationList.getSelectedValue();
                    if (d != null) setValue(first(options));
                }
            });
        updateDestinationList();

        JScrollPane listScroller = new JScrollPane(destinationList);
        listScroller.setPreferredSize(new Dimension(300, 300));

        String omcb = Messages.message("selectDestinationDialog.onlyMyColonies");
        this.onlyMyColoniesBox = new JCheckBox(omcb, showOnlyMyColonies.get());
        this.onlyMyColoniesBox.addChangeListener((ChangeEvent event) -> {
                showOnlyMyColonies.set(onlyMyColoniesBox.isSelected());
                updateDestinationList();
            });

        this.comparatorBox = new JComboBox<>(new String[] {
                Messages.message("selectDestinationDialog.sortByOwner"),
                Messages.message("selectDestinationDialog.sortByName"),
                Messages.message("selectDestinationDialog.sortByDistance")
            });
        this.comparatorBox.addItemListener((ItemEvent event) -> {
                updateDestinationComparator();
                Comparator<Destination> dc = getDestinationComparator();
                SelectDestinationDialog.this.destinations.sort(dc);
                updateDestinationList();
            });
        Comparator<Destination> dc = getDestinationComparator();
        this.comparatorBox.setSelectedIndex(Destination
            .getDestinationComparatorIndex(dc));

        JPanel panel = new MigPanel(new MigLayout("wrap 1, fill",
                                                  "[align center]", ""));
        panel.add(Utility.localizedHeader("selectDestinationDialog.text", true));

        panel.add(listScroller, "newline 30, growx, growy");
        panel.add(this.onlyMyColoniesBox, "left");
        panel.add(this.comparatorBox, "left");
        panel.setSize(panel.getPreferredSize());

        List<ChoiceItem<Location>> c = choices();
        c.add(new ChoiceItem<>(Messages.message("ok"),
                (Location)null).okOption());
        c.add(new ChoiceItem<>(Messages.message("selectDestinationDialog.cancel"),
                (Location)null).cancelOption().defaultOption());
        initializeDialog(frame, DialogType.QUESTION, true, panel,
                         new ImageIcon(lib.getSmallUnitImage(unit)), c);
    }


    /**
     * Get the current destination comparator.
     *
     * @return The destination comparator.
     */
    private Comparator<Destination> getDestinationComparator() {
        Comparator<Destination> ret = destinationComparator.get();
        if (ret == null) {
            ret = Destination.getDestinationComparator(0);
            destinationComparator.set(ret);
        }
        return ret;
    }

    /**
     * Set the current destination comparator.
     *
     * @param dc The new destination comparator.
     */
    private void setDestinationComparator(Comparator<Destination> dc) {
        destinationComparator.set(dc);
    }
    
    /**
     * Load destinations for a given unit and carried goods types.
     *
     * @param unit The {@code Unit} to select destinations for.
     * @param goodsTypes A list of {@code GoodsType}s carried.
     */
    private void loadDestinations(final Unit unit,
                                  List<GoodsType> goodsTypes) {
        if (unit.isInEurope() && !unit.getType().canMoveToHighSeas()) return;

        final Player player = unit.getOwner();
        final Settlement inSettlement = unit.getSettlement();
        final boolean canTrade
            = player.hasAbility(Ability.TRADE_WITH_FOREIGN_COLONIES);
        final Europe europe = player.getEurope();
        final Game game = getGame();
        final Map map = game.getMap();
        // Quick check for whether a settlement is reachable by the unit.
        // Used to knock out obviously impossible candidates before invoking
        // the expensive full path search.
        final Predicate<Settlement> canReach = s ->
            (unit.isNaval()) ? s.isConnectedPort()
                : Map.isSameContiguity(unit.getLocation(), s.getTile());
        // Add Europe or "New World" (the map) depending where the unit is
        List<Destination> td = new ArrayList<>();
        if (unit.isInEurope()) {
            td.add(new Destination(map, unit.getSailTurns(), unit, goodsTypes));
        } else if (europe != null
            && player.canMoveToEurope()
            && unit.getType().canMoveToHighSeas()) {
            int turns = unit.getTurnsToReach(europe);
            if (turns < Unit.MANY_TURNS) {
                td.add(new Destination(europe, turns, unit, goodsTypes));
            }
        }

        // Find all the player accessible settlements except the current one.
        td.addAll(transform(player.getSettlements(),
                            s -> s != inSettlement && canReach.test(s),
                            s -> new Destination(s, unit.getTurnsToReach(s),
                                                 unit, goodsTypes)));

        // Find all other player accessible settlements.  Build a list
        // of accessible settlement locations and do a bulk path search
        // to determine the travel times, and create Destinations from
        // the results.
        final Predicate<Player> tradePred = p ->
            p.hasContacted(player) && (canTrade || !p.isEuropean());
        final Function<Player, Stream<Location>> settlementTileMapper = p ->
            transform(p.getSettlements(),
                      s -> canReach.test(s) && s.hasContacted(p),
                      s -> (Location)s.getTile()).stream();
        List<Location> locs = toList(flatten(game.getLivePlayers(player),
                                             tradePred, settlementTileMapper));
        MultipleAdjacentDecider md = new MultipleAdjacentDecider(locs);
        unit.search(unit.getLocation(), md.getGoalDecider(), null,
                    INFINITY, null);
        final Function<Entry<Location, PathNode>, Destination> dmapper = e -> {
            Settlement s = e.getKey().getTile().getSettlement();
            PathNode p = e.getValue();
            if (s == null) {
                logger.warning("BR#3149 null settlement: "
                    + Messages.message(e.getKey().getLocationLabel())
                    + " path=" + p.fullPathToString());
                return null;
            }                    
            int turns = p.getTotalTurns();
            if (unit.isInEurope()) turns += unit.getSailTurns();
            if (p.getMovesLeft() < unit.getInitialMovesLeft()) turns++;
            return new Destination(s, turns, unit, goodsTypes);
        };
        td.addAll(transform(md.getResults().entrySet(), isNotNull(), dmapper));

        // Drop inaccessible destinations and sort as specified.
        this.destinations.addAll(transform(td, d -> d.turns < Unit.MANY_TURNS,
                                           Function.<Destination>identity(),
                                           getDestinationComparator()));
    }

    /**
     * Reset the destinations in the model.
     */
    private void updateDestinationList() {
        final Player player = getMyPlayer();
        Destination selected = this.destinationList.getSelectedValue();
        DefaultListModel<Destination> model
            = new DefaultListModel<>();
        for (Destination d : this.destinations) {
            if (showOnlyMyColonies.get()) {
                if (d.location instanceof Europe
                    || d.location instanceof Map
                    || (d.location instanceof Colony
                        && player.owns((Colony)d.location))) {
                    model.addElement(d);
                }
            } else {
                model.addElement(d);
            }
        }
        this.destinationList.setModel(model);
        this.destinationList.setSelectedValue(selected, true);
        if (this.destinationList.getSelectedIndex() < 0) {
            this.destinationList.setSelectedIndex(0);
        }
        recenter(this.destinationList.getSelectedValue());
    }

    /**
     * Show a destination on the map.
     *
     * @param destination The {@code Destination} to display.
     */
    private void recenter(Destination destination) {
        if (destination != null
            && destination.location.getTile() != null) {
            getGUI().setFocus(destination.location.getTile());
        }
    }

    /**
     * Set the selected destination comparator.
     */
    private void updateDestinationComparator() {
        setDestinationComparator(Destination
            .getDestinationComparator(this.comparatorBox.getSelectedIndex()));
    }


    // Interface ListSelectionListener
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        recenter(this.destinationList.getSelectedValue());
    }


    // Implement FreeColDialog

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getResponse() {
        Object value = getValue();
        if (options.get(0).equals(value)) {
            Destination d = this.destinationList.getSelectedValue();
            if (d != null) return d.location;
        }
        return null;
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        removeAll();
        this.destinations.clear();
        this.onlyMyColoniesBox = null;
        this.comparatorBox = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        this.destinationList.requestFocus();
    }
}
