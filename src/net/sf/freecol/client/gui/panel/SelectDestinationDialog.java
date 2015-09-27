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

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
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
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.pathfinding.GoalDeciders.MultipleAdjacentDecider;
import net.sf.freecol.common.resources.ResourceManager;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;


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
    private class Destination {

        public final Unit unit;
        public final Location location;
        public final int turns;
        public final String extras;
        public final String text;
        public final int score;
        public ImageIcon icon;


        /**
         * Create a destination.
         *
         * @param location The <code>Location</code> to go to.
         * @param turns The number of turns it takes to get to the location.
         * @param unit The <code>Unit</code> that is moving.
         * @param goodsTypes A list of goods types the unit is carrying.
         */
        public Destination(Location location, int turns, Unit unit,
                           List<GoodsType> goodsTypes) {
            this.unit = unit;
            this.location = location;
            this.turns = turns;
            this.extras = getExtras(location, unit, goodsTypes);
            this.score = calculateScore();
            this.icon = null;

            final Player player = getMyPlayer();
            final ImageLibrary lib = getImageLibrary();
            String name = "";
            if (location instanceof Europe) {
                Europe europe = (Europe)location;
                Nation nation = europe.getOwner().getNation();
                name = Messages.getName(europe);
                this.icon = new ImageIcon(ImageLibrary.getMiscIconImage(nation,
                    new Dimension(-1, CELL_HEIGHT)));
            } else if (location instanceof Map) {
                name = Messages.message(location.getLocationLabelFor(player));
                this.icon = new ImageIcon(lib.getMiscImage(
                    ImageLibrary.LOST_CITY_RUMOUR));
            } else if (location instanceof Settlement) {
                Settlement settlement = (Settlement) location;
                name = Messages.message(settlement.getLocationLabelFor(player));
                this.icon = new ImageIcon(ImageLibrary.getSettlementImage(
                    settlement,
                    new Dimension(64, -1)));
            }
            StringTemplate template = StringTemplate
                .template("selectDestinationDialog.destinationTurns")
                .addName("%location%", name)
                .addAmount("%turns%", this.turns)
                .addName("%extras%", this.extras);
            this.text = Messages.message(template);
        }

        /**
         * Collected extra annotations of interest to a unit proposing to
         * visit a location.
         *
         * @param loc The <code>Location</code> to visit.
         * @param unit The <code>Unit</code> proposing to visit.
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
                        Unit up = (unit.getType().canBeUpgraded(sk,
                                ChangeType.NATIVES)) ? unit : null;
                        if (unit.isCarrier()) {
                            up = find(unit.getUnitList(),
                                u -> u.getType().canBeUpgraded(sk, ChangeType.NATIVES));
                        }
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
                            GoodsType[] wanted
                                = ((IndianSettlement)loc).getWantedGoods();
                            if (wanted.length > 0 && g == wanted[0]) {
                                more = "***";
                            } else if (wanted.length > 1 && g == wanted[1]) {
                                more = "**";
                            } else if (wanted.length > 2 && g == wanted[2]) {
                                more = "*";
                            }
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
    }

    private class DestinationComparator implements Comparator<Destination> {

        protected final Player owner;

        public DestinationComparator(Player player) {
            this.owner = player;
        }

        @Override
        public int compare(Destination choice1, Destination choice2) {
            int score1 = choice1.score;
            int score2 = choice2.score;
            return (score1 != score2) ? score1 - score2
                : compareNames(choice1.location, choice2.location);
        }

        /**
         * Compare the names of two locations.
         *
         * @param loc1 The first <code>Location</code>.
         * @param loc2 The second <code>Location</code>.
         */
        protected int compareNames(Location loc1, Location loc2) {
            if (!(loc1 instanceof Settlement)) return -1;
            if (!(loc2 instanceof Settlement)) return 1;
            Settlement s1 = (Settlement)loc1;
            String name1 = Messages.message(s1.getLocationLabelFor(owner));
            Settlement s2 = (Settlement)loc2;
            String name2 = Messages.message(s2.getLocationLabelFor(owner));
            return name1.compareTo(name2);
        }
    }

    private class NameComparator extends DestinationComparator {

        public NameComparator(Player player) {
            super(player);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(Destination choice1, Destination choice2) {
            return compareNames(choice1.location, choice2.location);
        }
    }

    private class DistanceComparator extends DestinationComparator {

        public DistanceComparator(Player player) {
            super(player);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(Destination choice1, Destination choice2) {
            int result = choice1.turns - choice2.turns;
            return (result != 0) ? result
                : compareNames(choice1.location, choice2.location);
        }
    }

    private static class LocationRenderer
        extends FreeColComboBoxRenderer<Destination> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void setLabelValues(JLabel label, Destination value) {
            if (value.icon != null) label.setIcon(value.icon);
            label.setText(value.text);
        }
    }

    /** The size of each destination cell. */
    private static final int CELL_HEIGHT = 48;

    /** Show only the player colonies.  FIXME: make a client option. */
    private static boolean showOnlyMyColonies = true;

    /** How to order the destinations. */
    private static Comparator<Destination> destinationComparator = null;

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
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     */
    public SelectDestinationDialog(FreeColClient freeColClient, JFrame frame,
            Unit unit) {
        super(freeColClient, frame);

        // Collect the goods the unit is carrying and set this.destinations.
        final List<GoodsType> goodsTypes = new ArrayList<>();
        for (Goods goods : unit.getCompactGoodsList()) {
            goodsTypes.add(goods.getType());
        }
        loadDestinations(unit, goodsTypes);

        DefaultListModel<Destination> model
            = new DefaultListModel<>();
        this.destinationList = new JList<>(model);
        this.destinationList.setCellRenderer(new LocationRenderer());
        this.destinationList.setFixedCellHeight(CELL_HEIGHT);
        this.destinationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.destinationList.addListSelectionListener(this);
        this.destinationList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() != 2) return;
                    Destination d = destinationList.getSelectedValue();
                    if (d != null) setValue(options.get(0));
                }
            });
        updateDestinationList();

        JScrollPane listScroller = new JScrollPane(destinationList);
        listScroller.setPreferredSize(new Dimension(300, 300));

        String omcb = Messages.message("selectDestinationDialog.onlyMyColonies");
        this.onlyMyColoniesBox = new JCheckBox(omcb, showOnlyMyColonies);
        this.onlyMyColoniesBox.addChangeListener((ChangeEvent event) -> {
                showOnlyMyColonies = onlyMyColoniesBox.isSelected();
                updateDestinationList();
            });

        this.comparatorBox = new JComboBox<>(new String[] {
                Messages.message("selectDestinationDialog.sortByOwner"),
                Messages.message("selectDestinationDialog.sortByName"),
                Messages.message("selectDestinationDialog.sortByDistance")
            });
        this.comparatorBox.addItemListener((ItemEvent event) -> {
                updateDestinationComparator();
                Collections.sort(SelectDestinationDialog.this.destinations,
                    SelectDestinationDialog.this.destinationComparator);
                updateDestinationList();
            });
        this.comparatorBox.setSelectedIndex(
            (this.destinationComparator instanceof NameComparator) ? 1
            : (this.destinationComparator instanceof DistanceComparator) ? 2
            : 0);

        MigPanel panel = new MigPanel(new MigLayout("wrap 1, fill",
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
        initializeDialog(frame, DialogType.QUESTION, true, panel, new ImageIcon(
            getImageLibrary().getSmallUnitImage(unit)), c);
    }


    /**
     * Load destinations for a given unit and carried goods types.
     *
     * @param unit The <code>Unit</code> to select destinations for.
     * @param goodsTypes A list of <code>GoodsType</code>s carried.
     */
    private void loadDestinations(Unit unit, List<GoodsType> goodsTypes) {
        final Player player = unit.getOwner();
        final Settlement inSettlement = unit.getSettlement();
        final boolean canTrade
            = player.hasAbility(Ability.TRADE_WITH_FOREIGN_COLONIES);
        final Europe europe = player.getEurope();
        final Game game = getGame();
        final Map map = game.getMap();
        int turns;

        if (unit.isInEurope() && !unit.getType().canMoveToHighSeas()) return;

        if (unit.isInEurope()) {
            this.destinations.add(new Destination(map, unit.getSailTurns(),
                                                  unit, goodsTypes));
        } else if (europe != null
            && player.canMoveToEurope()
            && unit.getType().canMoveToHighSeas()
            && (turns = unit.getTurnsToReach(europe)) < Unit.MANY_TURNS) {
            this.destinations.add(new Destination(europe, turns, 
                                                  unit, goodsTypes));
        }

        for (Settlement s : player.getSettlements()) {
            if (s == inSettlement) continue;
            if (unit.isNaval()) {
                if (!s.isConnectedPort()) continue;
            } else {
                if (!Map.isSameContiguity(unit.getLocation(),
                        s.getTile())) continue;
            }
            if ((turns = unit.getTurnsToReach(s)) < Unit.MANY_TURNS) {
                this.destinations.add(new Destination(s, turns,
                                                      unit, goodsTypes));
            }
        }
        
        List<Location> locs = new ArrayList<>();
        for (Player p : game.getLivePlayers(player)) {
            if (!p.hasContacted(player)
                || (p.isEuropean() && !canTrade)) continue;

            for (Settlement s : p.getSettlements()) {
                if (unit.isNaval()) {
                    if (!s.isConnectedPort()) continue;
                } else {
                    if (!Map.isSameContiguity(unit.getLocation(),
                                              s.getTile())) continue;
                }
                if (s instanceof IndianSettlement
                    && !((IndianSettlement)s).hasContacted(player)) continue;
                locs.add(s.getTile());
            }
        }
        MultipleAdjacentDecider md = new MultipleAdjacentDecider(locs);
        unit.search(unit.getLocation(), md.getGoalDecider(), null,
                    FreeColObject.INFINITY, null);
        PathNode path;
        for (Entry<Location, PathNode> e : md.getResults().entrySet()) {
            Settlement s = e.getKey().getTile().getSettlement();
            PathNode p = e.getValue();
            turns = p.getTotalTurns();
            if (unit.isInEurope()) turns += unit.getSailTurns();
            if (p.getMovesLeft() < unit.getInitialMovesLeft()) turns++;
            this.destinations.add(new Destination(s, turns, unit, goodsTypes));
        }

        if (this.destinationComparator == null) {
            this.destinationComparator = new DestinationComparator(player);
        }
        Collections.sort(this.destinations, this.destinationComparator);
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
            if (showOnlyMyColonies) {
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
     * @param destination The <code>Destination</code> to display.
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
        final Player player = getMyPlayer();
        switch (this.comparatorBox.getSelectedIndex()) {
        case 1:
            this.destinationComparator = new NameComparator(player);
            break;
        case 2:
            this.destinationComparator = new DistanceComparator(player);
            break;
        case 0: default:
            this.destinationComparator = new DestinationComparator(player);
            break;
        }
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
