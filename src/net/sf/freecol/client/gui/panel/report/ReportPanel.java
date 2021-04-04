/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays a report.
 */
public class ReportPanel extends FreeColPanel {

    protected static final Logger logger = Logger.getLogger(ReportPanel.class.getName());

    /**
     * The default layout contrains of the {@code JScrollPane}s in this class.
     */
    private static final String SCROLL_PANE_SIZE
        = "cell 0 1, height 100%, width 100%";

    protected final JPanel reportPanel;

    protected final JLabel header;

    protected JScrollPane scrollPane;


    /**
     * Creates the basic FreeCol report panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param key A key for the title.
     */
    protected ReportPanel(FreeColClient freeColClient, String key) {
        super(freeColClient, "ReportPanelUI",
              new MigLayout("wrap 1", "[fill]", "[]30[fill]30[]"));

        header = Utility.localizedHeader(Messages.nameKey(key),
                                         Utility.FONTSPEC_TITLE);
        add(header, "cell 0 0, align center");

        reportPanel = new MigPanel("ReportPanelUI");
        reportPanel.setOpaque(true);
        reportPanel.setBorder(createBorder());

        scrollPane = new JScrollPane(reportPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
        add(scrollPane, SCROLL_PANE_SIZE);
        add(okButton, "cell 0 2, tag ok");

        getGUI().restoreSavedSize(this, new Dimension(1050, 725));
    }


    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        reportPanel.removeAll();
        reportPanel.doLayout();
    }

    private Border createBorder() {
        return new EmptyBorder(20, 20, 20, 20);
    }

    protected JLabel createUnitTypeLabel(AbstractUnit au) {
        UnitType unitType = au.getType(getSpecification());
        String roleId = au.getRoleId();
        int count = au.getNumber();
        ImageIcon unitIcon = new ImageIcon(getImageLibrary()
            .getSmallUnitTypeImage(unitType, roleId, (count == 0)));
        JLabel unitLabel = new JLabel(unitIcon);
        unitLabel.setText(String.valueOf(count));
        if (count == 0) {
            unitLabel.setForeground(Color.GRAY);
        }
        unitLabel.setToolTipText(au.getDescription());
        return unitLabel;
    }

    protected String getLocationLabelFor(Unit unit) {
        if (unit.getDestination() instanceof Map) {
            return Messages.message("sailingToAmerica");
        } else if (unit.getDestination() instanceof Europe) {
            return Messages.message("sailingToEurope");
        } else {
            return Messages.message(unit.getLocation()
                .getLocationLabelFor(unit.getOwner()));
        }
    }

    protected void setMainComponent(Component main) {
        remove(scrollPane);
        add(main, SCROLL_PANE_SIZE);
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String command = ae.getActionCommand();
        if (OK.equals(command)) {
            getGUI().removeComponent(this);
        } else {
            FreeColGameObject fco = getGame().getFreeColGameObject(command);
            if (fco != null) {
                getGUI().displayObject(fco);
            } else {
                getGUI().showColopediaPanel(command);
            }
        }
    }

    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        removeAll();
        scrollPane = null;
    }
}
