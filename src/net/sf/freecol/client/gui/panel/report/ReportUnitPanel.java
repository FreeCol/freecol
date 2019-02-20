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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.label.GoodsLabel;
import net.sf.freecol.client.gui.label.UnitLabel;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This panel displays a unit Report.
 */
public abstract class ReportUnitPanel extends ReportPanel {

    /** Simple comparator to order miscellaneous locations. */
    private static final Comparator<Location> locationComparator
        = new Comparator<Location>() {
                public int compare(Location l1, Location l2) {
                    return l1.getId().compareTo(l2.getId());
                }
            };

    /**
     * Comparator to order abstract unit lists with most common
     * type+role first.
     */
    private static final Comparator<AbstractUnit> auComparator
        = Comparator.comparingInt(AbstractUnit::getNumber).reversed();

    /** Where are the players reportable units? */
    private final Map<Location, List<Unit>> where = new HashMap<>();
    
    /** Count the number of units of each type+role. */
    private final List<AbstractUnit> reportables = new ArrayList<>();

    /** Count the relevant REF units. */
    private final List<AbstractUnit> reportableREF = new ArrayList<>();
    
    /** Locations in the order to display them. */
    private final List<Location> orderedLocations = new ArrayList<>();
    
    /** Whether to show colonies even if no selected units are present. */
    private boolean showColonies = false;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param key The report name key.
     * @param showColonies Whether to show colonies with no selected units.
     */
    protected ReportUnitPanel(FreeColClient freeColClient, String key,
                              boolean showColonies) {
        super(freeColClient, key);

        // Override default layout
        this.reportPanel.setLayout(new MigLayout("fillx, wrap 12", "", ""));

        this.showColonies = showColonies;

        gatherData();
        display();
    }


    private final void gatherData() {
        // Add entries to where and reportables for the players units
        final Player player = getMyPlayer();
        for (Unit u : transform(player.getUnits(), u -> isReportable(u))) {
            appendToMapList(this.where, u.getLocation().up(), u);
            AbstractUnit au = find(this.reportables, AbstractUnit.matcher(u));
            if (au == null) {
                this.reportables.add(new AbstractUnit(u.getType(),
                        u.getRole().getId(), 1));
            } else {
                au.addToNumber(1);
            }
        }
        this.reportables.sort(auComparator);

        // Create the list of reportable REF abstract units
        List<AbstractUnit> refUnits = player.getREFUnits();
        if (refUnits != null) {
            for (AbstractUnit au : refUnits) {
                if (isReportableREF(au)) {
                    this.reportableREF.add(au);
                }
            }
        }
        this.reportableREF.sort(auComparator);

        // Build a list of locations, colonies first, then Europe, then other
        this.orderedLocations.addAll(transform(this.where.keySet(),
                l -> !(l instanceof Colony || l instanceof Europe),
                Function.<Location>identity(), locationComparator));
        Europe europe = player.getEurope();
        if (europe != null) this.orderedLocations.add(0, europe);
        List<Colony> colonies = player.getSortedColonies(getClientOptions()
            .getColonyComparator());
        this.orderedLocations.addAll(0, colonies);
    }

    private final void display() {
        final Player player = getMyPlayer();
        // Display REF
        if (!this.reportableREF.isEmpty()) {
            final Nation refNation = player.getNation().getREFNation();
            this.reportPanel.add(new JLabel(Messages.getName(refNation)),
                                SPAN_SPLIT_2);
            this.reportPanel.add(new JSeparator(JSeparator.HORIZONTAL),
                                 "growx");
            for (AbstractUnit au : this.reportableREF) {
                this.reportPanel.add(createUnitTypeLabel(au), "sg");
            }
        }

        // Display our units
        reportPanel.add(Utility.localizedLabel(player.getForcesLabel()),
                        NL_SPAN_SPLIT_2);
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");
        for (AbstractUnit au : this.reportables) {
            if (au.getNumber() <= 0) continue;
            reportPanel.add(createUnitTypeLabel(au), "sg");
        }

        // Display again by location
        for (Location loc : orderedLocations) {
            List<Unit> present = this.where.get(loc);
            if (present == null) {
                present = Collections.<Unit>emptyList();
            } else {
                present = sort(present, Unit.typeRoleComparator);
            }
            if (!this.showColonies && present.isEmpty()) continue;
            String locName = Messages.message(loc.getLocationLabelFor(player));
            JButton button = Utility.getLinkButton(locName, null, loc.getId());
            button.addActionListener(this);
            this.reportPanel.add(button, NL_SPAN_SPLIT_2);
            this.reportPanel.add(new JSeparator(JSeparator.HORIZONTAL),
                                 "growx");
            if (present.isEmpty()) {
                this.reportPanel.add(Utility.localizedLabel("none"), "sg");
            } else {
                for (Unit u : sort(present, Unit.typeRoleComparator)) {
                    JButton unitButton = getUnitButton(u);
                    if (u.isCarrier()) {
                        this.reportPanel.add(unitButton, "newline, sg");
                        for (Goods goods : u.getGoodsList()) {
                            GoodsLabel goodsLabel = new GoodsLabel(getGUI(), goods);
                            this.reportPanel.add(goodsLabel);
                        }
                        for (Unit unitLoaded : u.getUnitList()) {
                            UnitLabel unitLoadedLabel
                                = new UnitLabel(getFreeColClient(), unitLoaded, true);
                            this.reportPanel.add(unitLoadedLabel);
                        }
                    } else {
                        this.reportPanel.add(unitButton, "sg");
                    }
                }
            }
        }

        revalidate();
        repaint();
    }

    /**
     * Get a button for a unit.
     *
     * @param unit The {@code Unit} that needs a button.
     * @return A suitable {@code JButton}.
     */
    private JButton getUnitButton(Unit unit) {
        ImageIcon icon = new ImageIcon(getImageLibrary().getScaledUnitImage(unit));
        JButton button = Utility.getLinkButton("", icon,
                                               unit.getLocation().up().getId());
        button.addActionListener(this);
        StringTemplate tip = StringTemplate.label("\n")
                                           .addStringTemplate(unit.getLabel());
        if (unit.getDestination() != null) {
            tip.addStringTemplate(unit.getDestinationLabel());
        }
        Utility.localizeToolTip(button, tip);
        return button;
    }


    // To be implemented by specific unit panels.

    /**
     * Is this unit from the player's units reportable?
     *
     * @param unit The {@code Unit} to check.
     * @return True if the unit should appear in this panel.
     */
    protected abstract boolean isReportable(Unit unit);

    /**
     * Is a unit type and role reportable as a combination usable by
     * the player?
     *
     * @param unitType The {@code UnitType} to check.
     * @param role The {@code Role} to check.
     * @return True if the unit type and role should appear in this panel.
     */
    protected abstract boolean isReportable(UnitType unitType, Role role);
    
    /**
     * Is a REF unit reportable?
     *
     * @param au The {@code AbstractUnit} to check.
     * @return True if REF unit should appear in this panel.
     */
    protected abstract boolean isReportableREF(AbstractUnit au);
}
