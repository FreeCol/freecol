/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;

import net.miginfocom.swing.MigLayout;


/**
 * Centers the map on a known settlement or colony. Pressing ENTER
 * opens a panel if appropriate.
 */
public final class EndTurnDialog extends FreeColDialog<Boolean> implements ListSelectionListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FindSettlementDialog.class.getName());

    private JList unitList;


    /**
     * The constructor to use.
     */
    public EndTurnDialog(Canvas parent, List<Unit> units) {
        super(parent);

        setLayout(new MigLayout("wrap 1", "[align center]"));

        JLabel header = new JLabel(Messages.message("endTurnDialog.name"));
        header.setFont(smallHeaderFont);

        StringTemplate t = StringTemplate.template("endTurnDialog.areYouSure")
            .addAmount("%number%", units.size());

        unitList = new JList(units.toArray(new Unit[units.size()]));
        unitList.setCellRenderer(new UnitRenderer());
        unitList.setFixedCellHeight(48);
        JScrollPane listScroller = new JScrollPane(unitList);
        listScroller.setPreferredSize(new Dimension(250, 250));
        unitList.addListSelectionListener(this);

        Action selectAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    selectUnit();
                }
            };

        Action quitAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    EndTurnDialog.this.setResponse(Boolean.FALSE);
                }
            };

        unitList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "select");
        unitList.getActionMap().put("select", selectAction);
        unitList.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "quit");
        unitList.getActionMap().put("quit", quitAction);

        MouseListener mouseListener = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        selectUnit();
                    }
                }
            };
        unitList.addMouseListener(mouseListener);

        add(header);
        add(getDefaultTextArea(Messages.message(t)), "newline 30");
        add(listScroller, "width max(200, 100%), height max(300, 100%), newline 20");

        add(cancelButton, "newline 20, span, split 2, tag cancel");
        add(okButton, "tag ok");

        restoreSavedSize(getPreferredSize());
    }

    private void selectUnit() {
        Unit unit = (Unit) unitList.getSelectedValue();
        Canvas canvas = getCanvas();
        Location location = unit.getLocation();
        if (location.getColony() != null) {
            canvas.showColonyPanel(location.getColony());
        } else {
            canvas.getGUI().setFocus(location.getTile());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        unitList.requestFocus();
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     *
     * @param e a <code>ListSelectionEvent</code> value
     */
    public void valueChanged(ListSelectionEvent e) {
        Unit unit = (Unit) unitList.getSelectedValue();
        getCanvas().getGUI().setFocus(unit.getTile());
    }

    private class UnitRenderer extends FreeColComboBoxRenderer {

        @Override
        public void setLabelValues(JLabel label, Object value) {
            Unit unit = (Unit) value;
            label.setText(Messages.message(Messages.getLabel(unit)));
            label.setIcon(getLibrary().getUnitImageIcon(unit, 0.5));
        }
    }


    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            setResponse(Boolean.TRUE);
        } else if (CANCEL.equals(command)) {
            setResponse(Boolean.FALSE);
        } else {
            super.actionPerformed(event);
        }
    }

}

