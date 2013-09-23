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

import net.miginfocom.swing.MigLayout;
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
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.common.util.Utils;


/**
 * Centers the map on a known settlement or colony.
 */
public final class SelectDestinationDialog extends FreeColDialog<Location>
    implements ActionListener, ChangeListener, ItemListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SelectDestinationDialog.class.getName());

    private static final int INFINITY = FreeColObject.INFINITY;

    private static boolean showOnlyMyColonies = true;

    private static Comparator<Destination> destinationComparator = null;

    private final JCheckBox onlyMyColoniesBox;

    private final JComboBox comparatorBox;

    private final JList destinationList;

    private final List<Destination> destinations
        = new ArrayList<Destination>();


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public SelectDestinationDialog(FreeColClient freeColClient, Unit unit) {
        super(freeColClient, new MigLayout("wrap 1, fill",
                                           "[align center]", ""));

        // Collect the goods the unit is carrying.
        final List<GoodsType> goodsTypes = new ArrayList<GoodsType>();
        for (Goods goods : unit.getCompactGoodsList()) {
            if (!goodsTypes.contains(goods.getType())) {
                goodsTypes.add(goods.getType());
            }
        }

        destinations.clear();
        collectDestinations(unit, goodsTypes);

        JLabel header = new JLabel(Messages.message("selectDestination.text"));
        header.setFont(GUI.SMALL_HEADER_FONT);
        add(header);

        DefaultListModel model = new DefaultListModel();
        destinationList = new JList(model);
        filterDestinations();

        destinationList.setCellRenderer(new LocationRenderer());
        destinationList.setFixedCellHeight(48);

        Action selectAction = new AbstractAction(Messages.message("ok")) {
                public void actionPerformed(ActionEvent e) {
                    Destination d = (Destination) destinationList.getSelectedValue();
                    if (d != null) {
                        setResponse((Location) d.location);
                    }
                    getGUI().removeFromCanvas(SelectDestinationDialog.this);
                }
            };

        Action quitAction = new AbstractAction(Messages.message("selectDestination.cancel")) {
                public void actionPerformed(ActionEvent e) {
                    getGUI().removeFromCanvas(SelectDestinationDialog.this);
                    setResponse(null);
                }
            };

        destinationList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "select");
        destinationList.getActionMap().put("select", selectAction);
        destinationList.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "quit");
        destinationList.getActionMap().put("quit", quitAction);

        MouseListener mouseListener = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    Destination d;
                    switch (e.getClickCount()) {
                    case 2:
                        d = (Destination)destinationList.getSelectedValue();
                        if (d != null) setResponse((Location)d.location);
                        getGUI().removeFromCanvas(SelectDestinationDialog.this);
                        break;
                    case 1:
                        d = (Destination)destinationList.getSelectedValue();
                        Location loc = (d == null) ? null
                            : (Location)d.location;
                        Tile tile = (loc == null) ? null : loc.getTile();
                        if (tile != null) getGUI().setFocusImmediately(tile);
                        break;
                    }
                }
            };
        destinationList.addMouseListener(mouseListener);

        JScrollPane listScroller = new JScrollPane(destinationList);
        listScroller.setPreferredSize(new Dimension(250, 250));

        add(listScroller, "newline 30, growx, growy");

        onlyMyColoniesBox = new JCheckBox(Messages.message("selectDestination.onlyMyColonies"),
                                          showOnlyMyColonies);
        onlyMyColoniesBox.addChangeListener(this);
        add(onlyMyColoniesBox, "left");

        comparatorBox = new JComboBox(new String[] {
                Messages.message("selectDestination.sortByOwner"),
                Messages.message("selectDestination.sortByName"),
                Messages.message("selectDestination.sortByDistance")
            });
        comparatorBox.addItemListener(this);
        if (destinationComparator instanceof DestinationComparator) {
            comparatorBox.setSelectedIndex(0);
        } else if (destinationComparator instanceof NameComparator) {
            comparatorBox.setSelectedIndex(1);
        } else if (destinationComparator instanceof DistanceComparator) {
            comparatorBox.setSelectedIndex(2);
        }
        add(comparatorBox, "left");

        cancelButton.setAction(quitAction);
        okButton.setAction(selectAction);

        add(okButton, "newline 30, split 2, tag ok");
        add(cancelButton, "tag cancel");

        setSize(getPreferredSize());
    }

    private void collectDestinations(Unit unit, List<GoodsType> goodsTypes) {
        final Player player = unit.getOwner();
        final Settlement inSettlement = unit.getSettlement();
        final boolean canTrade = player.hasAbility(Ability.TRADE_WITH_FOREIGN_COLONIES);
        final Europe europe = player.getEurope();
        final Game game = getGame();
        final Map map = game.getMap();
        int turns;

        if (unit.isInEurope() && !unit.getType().canMoveToHighSeas()) return;
        for (Player p : game.getPlayers()) {
            if (p != player
                && (!p.hasContacted(player) 
                    || (p.isEuropean() && !canTrade))) continue;

            for (Settlement s : p.getSettlements()) {
                if (s == inSettlement
                    || (unit.isNaval() && !s.isConnectedPort())
                    || (s instanceof IndianSettlement
                        && !((IndianSettlement)s).hasContacted(player)))
                    continue;
                if (p == player) {
                    if ((turns = unit.getTurnsToReach(s)) == INFINITY) 
                        continue;
                } else {
                    PathNode path = unit.search(unit.getLocation(),
                        GoalDeciders.getAdjacentLocationGoalDecider(s),
                        null, INFINITY, null);
                    if (path == null) continue;
                    turns = path.getTotalTurns();
                    if (path.getLastNode().getMovesLeft() <= 0) turns++;
                }
                destinations.add(new Destination(s, turns,
                        ((s.getOwner() == unit.getOwner()) ? ""
                            : getExtras(unit, s, goodsTypes))));
            }
        }
        if (unit.isInEurope()) {
            destinations.add(new Destination(map, unit.getSailTurns(), ""));
        } else if (europe != null
            && player.canMoveToEurope()
            && unit.getType().canMoveToHighSeas()
            && (turns = unit.getTurnsToReach(europe)) != INFINITY) {
            destinations.add(new Destination(europe, turns,
                    getExtras(unit, europe, goodsTypes)));
        }
        Collections.sort(destinations,
            ((destinationComparator != null) ? destinationComparator
                : new DestinationComparator(player)));
    }

    @Override
    public void requestFocus() {
        destinationList.requestFocus();
    }

    public void stateChanged(ChangeEvent event) {
        showOnlyMyColonies = onlyMyColoniesBox.isSelected();
        filterDestinations();
    }

    public void itemStateChanged(ItemEvent event) {
        switch(comparatorBox.getSelectedIndex()) {
        case 0:
        default:
            destinationComparator = new DestinationComparator(getMyPlayer());
            break;
        case 1:
            destinationComparator = new NameComparator();
            break;
        case 2:
            destinationComparator = new DistanceComparator();
            break;
        }
        Collections.sort(destinations, destinationComparator);
        filterDestinations();
    }

    /**
     * Collected extra annotations of interest to a unit proposing to
     * visit a location.
     *
     * @param unit The <code>Unit</code> proposing to visit.
     * @param loc The <code>Location</code> to visit.
     * @param goodsTypes A list of goods types the unit is carrying.
     * @return A string containing interesting annotations about the visit
     *         or an empty string if nothing is of interest.
     */
    private String getExtras(Unit unit, Location loc,
                             List<GoodsType> goodsTypes) {
        Player owner = unit.getOwner();
        if (loc instanceof Europe && !goodsTypes.isEmpty()) {
            Market market = owner.getMarket();
            List<String> sales = new ArrayList<String>();
            for (GoodsType goodsType : goodsTypes) {
                sales.add(Messages.message(goodsType.getNameKey()) + " "
                    + Integer.toString(market.getSalePrice(goodsType, 1)));
            }
            if (!sales.isEmpty()) {
                return "[" + Utils.join(", ", sales) + "]";
            }
        } else if (loc instanceof Settlement
            && ((Settlement)loc).getOwner().atWarWith(owner)) {
            return "[" + Messages.message("model.stance.war") + "]";
        } else if (loc instanceof Settlement && !goodsTypes.isEmpty()) {
            List<String> sales = new ArrayList<String>();
            for (GoodsType goodsType : goodsTypes) {
                String sale = owner.getLastSaleString((Settlement)loc,
                                                      goodsType);
                if (sale != null) {
                    sales.add(Messages.message(goodsType.getNameKey())
                              + " " + sale);
                    continue;
                }
                if (loc instanceof IndianSettlement) {
                    IndianSettlement indianSettlement = (IndianSettlement) loc;
                    GoodsType[] wanted = indianSettlement.getWantedGoods();
                    if (wanted.length > 0 && goodsType == wanted[0]) {
                        sales.add(Messages.message(goodsType.getNameKey())
                            + "***");
                    } else if (wanted.length > 1 && goodsType == wanted[1]) {
                        sales.add(Messages.message(goodsType.getNameKey())
                            + "**");
                    } else if (wanted.length > 2 && goodsType == wanted[2]) {
                        sales.add(Messages.message(goodsType.getNameKey())
                            + "*");
                    }
                }
            }
            if (!sales.isEmpty()) {
                return "[" + Utils.join(", ", sales) + "]";
            }
        } else if (loc instanceof IndianSettlement) {
            IndianSettlement indianSettlement = (IndianSettlement) loc;
            UnitType skill = indianSettlement.getLearnableSkill();
            if (skill != null
                && unit.getType().canBeUpgraded(skill, ChangeType.NATIVES)) {
                return "[" + Messages.message(skill.getNameKey()) + "]";
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked") // FIXME in Java7
    private void filterDestinations() {
        DefaultListModel model = (DefaultListModel) destinationList.getModel();
        Object selected = destinationList.getSelectedValue();
        model.clear();
        for (Destination d : destinations) {
            if (showOnlyMyColonies) {
                if (d.location instanceof Europe
                    || d.location instanceof Map
                    || (d.location instanceof Colony
                        && ((Colony) d.location).getOwner() == getMyPlayer())) {
                    model.addElement(d);
                }
            } else {
                model.addElement(d);
            }
        }
        destinationList.setSelectedValue(selected, true);
        if (destinationList.getSelectedIndex() == -1) {
            destinationList.setSelectedIndex(0);
        }
    }

    public int compareNames(Location dest1, Location dest2) {
        Player player = getMyPlayer();
        String name1 = "";
        if (dest1 instanceof Settlement) {
            name1 = Messages.message(((Settlement) dest1).getLocationNameFor(player));
        } else if (dest1 instanceof Europe || dest1 instanceof Map) {
            return -1;
        }
        String name2 = "";
        if (dest2 instanceof Settlement) {
            name2 = Messages.message(((Settlement) dest2).getLocationNameFor(player));
        } else if (dest2 instanceof Europe || dest2 instanceof Map) {
            return 1;
        }
        return name1.compareTo(name2);
    }

    private class Destination {
        public Location location;
        public int turns;
        public String extras;

        public Destination(Location location, int turns, String extras) {
            this.location = location;
            this.turns = turns;
            this.extras = extras;
        }
    }

    private class LocationRenderer extends FreeColComboBoxRenderer {

        @Override
        public void setLabelValues(JLabel label, Object value) {

            Destination d = (Destination) value;
            Location location = d.location;
            Player player = getMyPlayer();
            String name = "";
            ImageLibrary lib = getLibrary();
            if (location instanceof Europe) {
                Europe europe = (Europe) location;
                name = Messages.message(europe.getNameKey());
                label.setIcon(new ImageIcon(lib.getCoatOfArmsImage(europe.getOwner().getNation())
                        .getScaledInstance(-1, 48, Image.SCALE_SMOOTH)));
            } else if (location instanceof Map) {
                name = Messages.message(location.getLocationNameFor(player));
                label.setIcon(lib.getMiscImageIcon(ImageLibrary.LOST_CITY_RUMOUR));
            } else if (location instanceof Settlement) {
                Settlement settlement = (Settlement) location;
                name = Messages.message(settlement.getLocationNameFor(player));
                label.setIcon(new ImageIcon(lib.getSettlementImage(settlement)
                        .getScaledInstance(64, -1, Image.SCALE_SMOOTH)));
            }
            label.setText(Messages.message(StringTemplate.template("selectDestination.destinationTurns")
                                           .addName("%location%", name)
                                           .addAmount("%turns%", d.turns)
                                           .addName("%extras%", d.extras)));
        }
    }

    private class DestinationComparator implements Comparator<Destination> {

        private Player owner;

        public DestinationComparator(Player player) {
            this.owner = player;
        }

        public int compare(Destination choice1, Destination choice2) {
            Location dest1 = choice1.location;
            Location dest2 = choice2.location;

            int score1 = 100;
            if (dest1 instanceof Europe || dest1 instanceof Map) {
                score1 = 10;
            } else if (dest1 instanceof Colony) {
                if (((Colony) dest1).getOwner() == owner) {
                    score1 = 20;
                } else {
                    score1 = 30;
                }
            } else if (dest1 instanceof IndianSettlement) {
                score1 = 40;
            }
            int score2 = 100;
            if (dest2 instanceof Europe || dest2 instanceof Map) {
                score2 = 10;
            } else if (dest2 instanceof Colony) {
                if (((Colony) dest2).getOwner() == owner) {
                    score2 = 20;
                } else {
                    score2 = 30;
                }
            } else if (dest2 instanceof IndianSettlement) {
                score2 = 40;
            }

            if (score1 == score2) {
                return compareNames(dest1, dest2);
            } else {
                return score1 - score2;
            }
        }
    }

    private class NameComparator implements Comparator<Destination> {
        public int compare(Destination choice1, Destination choice2) {
            return compareNames(choice1.location, choice2.location);
        }
    }

    private class DistanceComparator implements Comparator<Destination> {
        public int compare(Destination choice1, Destination choice2) {
            int result = choice1.turns - choice2.turns;
            if (result == 0) {
                return compareNames(choice1.location, choice2.location);
            } else {
                return result;
            }
        }
    }
}
