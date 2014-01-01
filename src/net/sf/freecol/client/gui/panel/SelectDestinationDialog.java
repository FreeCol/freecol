/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
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
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDeciders.MultipleAdjacentDecider;
import net.sf.freecol.common.util.Utils;

import net.miginfocom.swing.MigLayout;


/**
 * Centers the map on a known settlement or colony.
 */
public final class SelectDestinationDialog extends FreeColDialog<Location> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SelectDestinationDialog.class.getName());


    /**
     * A container for a destination location, with associated
     * distance and extra characteristics.
     */
    private class Destination {

        public Location location;
        public int turns;
        public String extras;

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
            this.location = location;
            this.turns = turns;
            this.extras = getExtras(location, unit, goodsTypes);
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
            final Player owner = unit.getOwner();
            List<String> sales = new ArrayList<String>();

            if (loc instanceof Europe && !goodsTypes.isEmpty()) {
                Market market = owner.getMarket();
                for (GoodsType goodsType : goodsTypes) {
                    sales.add(Messages.message(goodsType.getNameKey()) + " "
                        + Integer.toString(market.getSalePrice(goodsType, 1)));
                }

            } else if (loc instanceof Settlement
                && owner.owns((Settlement)loc)) {
                ; // Do nothing

            } else if (loc instanceof Settlement
                && ((Settlement)loc).getOwner().atWarWith(owner)) {
                return "[" + Messages.message("model.stance.war") + "]";

            } else if (loc instanceof Settlement && !goodsTypes.isEmpty()) {
                for (GoodsType g : goodsTypes) {
                    String sale = owner.getLastSaleString((Settlement)loc, g);
                    if (sale != null) {
                        sales.add(Messages.message(g.getNameKey())
                            + " " + sale);
                        continue;
                    }
                    if (loc instanceof IndianSettlement) {
                        GoodsType[] wanted
                            = ((IndianSettlement)loc).getWantedGoods();
                        if (wanted.length > 0 && g == wanted[0]) {
                            sales.add(Messages.message(g.getNameKey()) + "***");
                        } else if (wanted.length > 1 && g == wanted[1]) {
                            sales.add(Messages.message(g.getNameKey()) + "**");
                        } else if (wanted.length > 2 && g == wanted[2]) {
                            sales.add(Messages.message(g.getNameKey()) + "*");
                        }
                    }
                }

            } else if (loc instanceof IndianSettlement) {
                IndianSettlement indianSettlement = (IndianSettlement) loc;
                UnitType sk = indianSettlement.getLearnableSkill();
                if (sk != null
                    && unit.getType().canBeUpgraded(sk, ChangeType.NATIVES)) {
                    return "[" + Messages.message(sk.getNameKey()) + "]";
                }
            }

            return (sales.isEmpty()) ? ""
                : "[" + Utils.join(", ", sales) + "]";
        }
    }

    private class DestinationComparator implements Comparator<Destination> {

        protected Player owner;

        public DestinationComparator(Player player) {
            this.owner = player;
        }

        public int compare(Destination choice1, Destination choice2) {
            Location loc1 = choice1.location;
            Location loc2 = choice2.location;

            int score1 = (loc1 instanceof Europe || loc1 instanceof Map)
                ? 10
                : (loc1 instanceof Colony)
                ? ((owner.owns((Colony)loc1)) ? 20 : 30)
                : (loc1 instanceof IndianSettlement)
                ? 40
                : 100;
            int score2 = (loc2 instanceof Europe || loc2 instanceof Map) ? 10
                : (loc2 instanceof Colony)
                ? ((owner.owns((Colony)loc2)) ? 20 : 30)
                : (loc2 instanceof IndianSettlement)
                ? 40
                : 100;
            return (score1 != score2) ? score1 - score2
                : compareNames(loc1, loc2);
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
            String name1 = Messages.message(s1.getLocationNameFor(owner));
            Settlement s2 = (Settlement)loc2;
            String name2 = Messages.message(s2.getLocationNameFor(owner));
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

    private class LocationRenderer extends FreeColComboBoxRenderer {

        @Override
        public void setLabelValues(JLabel label, Object value) {
            final Player player = getMyPlayer();
            final ImageLibrary lib = getImageLibrary();

            Destination d = (Destination)value;
            Location location = d.location;
            String name = "";
            ImageIcon icon = null;
            if (location instanceof Europe) {
                Europe europe = (Europe)location;
                Nation nation = europe.getOwner().getNation();
                name = Messages.message(europe.getNameKey());
                icon = new ImageIcon(lib.getCoatOfArmsImage(nation)
                    .getScaledInstance(-1, CELL_HEIGHT, Image.SCALE_SMOOTH));
            } else if (location instanceof Map) {
                name = Messages.message(location.getLocationNameFor(player));
                icon = lib.getMiscImageIcon(ImageLibrary.LOST_CITY_RUMOUR);
            } else if (location instanceof Settlement) {
                Settlement settlement = (Settlement) location;
                name = Messages.message(settlement.getLocationNameFor(player));
                icon = new ImageIcon(lib.getSettlementImage(settlement)
                    .getScaledInstance(64, -1, Image.SCALE_SMOOTH));
            }
            if (icon != null) label.setIcon(icon);
            StringTemplate template
                = StringTemplate.template("selectDestination.destinationTurns")
                    .addName("%location%", name)
                    .addAmount("%turns%", d.turns)
                    .addName("%extras%", d.extras);
            label.setText(Messages.message(template));
        }
    }

    /** The size of each destination cell. */
    private static final int CELL_HEIGHT = 48;

    /** Show only the player colonies.  TODO: make a client option. */
    private static boolean showOnlyMyColonies = true;

    /** How to order the destinations. */
    private static Comparator<Destination> destinationComparator = null;

    /** The available destinations. */
    private final List<Destination> destinations
        = new ArrayList<Destination>();

    /** The list of destinations. */
    private final JList destinationList;

    /** Restrict to only the player colonies? */
    private final JCheckBox onlyMyColoniesBox;

    /** Choice of the comparator. */
    private final JComboBox comparatorBox;


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public SelectDestinationDialog(FreeColClient freeColClient, Unit unit) {
        super(freeColClient);

        // Collect the goods the unit is carrying and set this.destinations.
        final List<GoodsType> goodsTypes = new ArrayList<GoodsType>();
        for (Goods goods : unit.getCompactGoodsList()) {
            goodsTypes.add(goods.getType());
        }
        loadDestinations(unit, goodsTypes);

        String sel = Messages.message("selectDestination.text");
        JLabel header = GUI.getDefaultHeader(sel);
        header.setFont(GUI.SMALL_HEADER_FONT);

        DefaultListModel model = new DefaultListModel();
        this.destinationList = new JList(model);
        this.destinationList.setCellRenderer(new LocationRenderer());
        this.destinationList.setFixedCellHeight(CELL_HEIGHT);
        this.destinationList
            .addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) return;
                    recenter((Destination)destinationList.getSelectedValue());
                }
            });
        this.destinationList.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() != 2) return;
                    Destination d
                        = (Destination)destinationList.getSelectedValue();
                    if (d != null) setValue(options.get(0));
                }
            });
        update();

        JScrollPane listScroller = new JScrollPane(destinationList);
        listScroller.setPreferredSize(new Dimension(300, 300));

        String omcb = Messages.message("selectDestination.onlyMyColonies");
        this.onlyMyColoniesBox = new JCheckBox(omcb, showOnlyMyColonies);
        this.onlyMyColoniesBox.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    showOnlyMyColonies = onlyMyColoniesBox.isSelected();
                    update();
                }
            });

        this.comparatorBox = new JComboBox(new String[] {
                Messages.message("selectDestination.sortByOwner"),
                Messages.message("selectDestination.sortByName"),
                Messages.message("selectDestination.sortByDistance")
            });
        this.comparatorBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    updateDestinationComparator();
                    Collections.sort(destinations, destinationComparator);
                    update();
                }
            });
        this.comparatorBox.setSelectedIndex(
            (this.destinationComparator instanceof NameComparator) ? 1
            : (this.destinationComparator instanceof DistanceComparator) ? 2
            : 0);

        MigPanel panel = new MigPanel(new MigLayout("wrap 1, fill",
                                                    "[align center]", ""));
        panel.add(header);
        panel.add(listScroller, "newline 30, growx, growy");
        panel.add(onlyMyColoniesBox, "left");
        panel.add(comparatorBox, "left");
        panel.setSize(panel.getPreferredSize());

        List<ChoiceItem<Location>> c = choices();
        c.add(new ChoiceItem<Location>(Messages.message("ok"),
                (Location)null).okOption());
        c.add(new ChoiceItem<Location>(Messages.message("cancel"),
                (Location)null).cancelOption().defaultOption());
        initialize(DialogType.QUESTION, true, panel,
                   getImageLibrary().getImageIcon(unit, true), c);
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
            && (turns = unit.getTurnsToReach(europe)) != FreeColObject.INFINITY) {
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
            if ((turns = unit.getTurnsToReach(s)) != FreeColObject.INFINITY) {
                this.destinations.add(new Destination(s, turns,
                                                      unit, goodsTypes));
            }
        }

        List<Location> locs = new ArrayList<Location>();
        for (Player p : game.getPlayers()) {
            if (p == player || !p.hasContacted(player)
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
    @SuppressWarnings("unchecked") // FIXME in Java7
    private void update() {
        final Player player = getMyPlayer();
        DefaultListModel model = (DefaultListModel)destinationList.getModel();
        Object selected = destinationList.getSelectedValue();
        model.clear();
        for (Destination d : destinations) {
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
        destinationList.setSelectedValue(selected, true);
        if (destinationList.getSelectedIndex() < 0) {
            destinationList.setSelectedIndex(0);
        }
        recenter((Destination)destinationList.getSelectedValue());
    }

    /**
     * Show a destination on the map.
     *
     * @param destination The <code>Destination</code> to display.
     */
    private void recenter(Destination destination) {
        if (destination == null
            || destination.location.getTile() == null) return;
        getGUI().setFocus(destination.location.getTile());
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

    /**
     * {@inheritDoc}
     */
    public Location getResponse() {
        Object value = getValue();
        if (options.get(0).equals(value)) {
            Destination d = (Destination)destinationList.getSelectedValue();
            if (d != null) return d.location;
        }
        return null;
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        destinationList.requestFocus();
    }
}
