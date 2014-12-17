/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;


/**
 * Centers the map on a known settlement or colony.  Pressing ENTER
 * opens a panel if appropriate.
 */
public final class FindSettlementPanel extends FreeColPanel
    implements ListSelectionListener, ItemListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FindSettlementPanel.class.getName());

    private class SettlementRenderer
        extends FreeColComboBoxRenderer<Settlement> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void setLabelValues(JLabel label, Settlement value) {
            StringTemplate template = StringTemplate
                .template("findSettlementPanel.settlement")
                .addName("%name%", value.getName())
                .addName("%capital%", ((value.isCapital()) ? "*" : ""))
                .addStringTemplate("%nation%", 
                    value.getOwner().getNationName());
            label.setText(Messages.message(template));
            label.setIcon(new ImageIcon(getLibrary().getSettlementImage(value)
                    .getScaledInstance(64, -1, Image.SCALE_SMOOTH)));
        }
    }

    private static enum DisplayListOption {
        ALL,
        ONLY_NATIVES,
        ONLY_EUROPEAN
    }

    /** Box to choose the type of settlements to list. */
    private JComboBox<String> displayOptionBox;

    /** The list of settlements to display. */
    private final JList<Settlement> settlementList;



    /**
     * Create a new panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public FindSettlementPanel(FreeColClient freeColClient) {
        super(freeColClient, new MigLayout("wrap 1", "[align center]",
                                           "[]30[]30[]"));

        JLabel header = GUI.localizedLabel("findSettlementPanel.name");
        header.setFont(GUI.SMALL_HEADER_FONT);
        add(header);

        this.settlementList = new JList<Settlement>();
        this.settlementList.setCellRenderer(new SettlementRenderer());
        this.settlementList.setFixedCellHeight(48);
        this.settlementList.addListSelectionListener(this);
        this.settlementList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        Action selectAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    selectSettlement();
                }
            };
        this.settlementList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),
                                              "select");
        this.settlementList.getActionMap().put("select", selectAction);

        Action quitAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    getGUI().removeFromCanvas(FindSettlementPanel.this);
                }
            };
        this.settlementList.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"),
                                             "quit");
        this.settlementList.getActionMap().put("quit", quitAction);

        MouseListener mouseListener = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        selectSettlement();
                    }
                }
            };
        this.settlementList.addMouseListener(mouseListener);

        JScrollPane listScroller = new JScrollPane(this.settlementList);
        listScroller.setPreferredSize(new Dimension(250, 250));
        add(listScroller, "width max(300, 100%), height max(300, 100%)");

        this.displayOptionBox = new JComboBox<String>(new String[] {
                Messages.message("findSettlementPanel.displayAll"),
                Messages.message("findSettlementPanel.displayOnlyNatives"),
                Messages.message("findSettlementPanel.displayOnlyEuropean"),
            });
        this.displayOptionBox.addItemListener(this);
        add(this.displayOptionBox);

        add(okButton, "tag ok");

        getGUI().restoreSavedSize(this, getPreferredSize());

        updateSearch(DisplayListOption.valueOf("ALL"));
    }

    private void updateSearch(DisplayListOption displayListOption) {
        DefaultListModel<Settlement> model
            = new DefaultListModel<Settlement>();
        Object selected = this.settlementList.getSelectedValue();

        for (Player player : getGame().getLivePlayers(null)) {
            boolean ok;
            switch (displayListOption) {
            case ONLY_NATIVES:
                ok = player.isIndian();
                break;
            case ONLY_EUROPEAN:
                ok = player.isEuropean();
                break;
            case ALL:
                ok = true;
                break;
            default:
                ok = false;
                break;
            }
            if (ok) {
                for (Settlement s : player.getSettlements()) {
                    model.addElement(s);
                }
            }
        }

        this.settlementList.setModel(model);
        this.settlementList.setSelectedValue(selected, true);
        if (this.settlementList.getSelectedIndex() < 0) {
            this.settlementList.setSelectedIndex(0);
        }
    }

    private void selectSettlement() {
        Settlement settlement = this.settlementList.getSelectedValue();
        if (settlement instanceof Colony
            && settlement.getOwner() == getMyPlayer()) {
            getGUI().removeFromCanvas(FindSettlementPanel.this);
            getGUI().showColonyPanel((Colony)settlement, null);
        } else if (settlement instanceof IndianSettlement) {
            getGUI().removeFromCanvas(FindSettlementPanel.this);
            getGUI().showIndianSettlementPanel((IndianSettlement)settlement);
        }
    }


    // Interface ItemListener

    public void itemStateChanged(ItemEvent event) {
        switch (this.displayOptionBox.getSelectedIndex()) {
        case 0:
        default:
            updateSearch(DisplayListOption.valueOf("ALL"));
            break;
        case 1:
            updateSearch(DisplayListOption.valueOf("ONLY_NATIVES"));
            break;
        case 2:
            updateSearch(DisplayListOption.valueOf("ONLY_EUROPEAN"));
        }
    }


    // Interface ListSelectionListener

    /**
     * {@inheritDoc}
     */
    public void valueChanged(ListSelectionEvent e) {
        Settlement settlement = this.settlementList.getSelectedValue();
        if (settlement != null) {
            getGUI().setFocus(settlement.getTile());
        }
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        this.settlementList.requestFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        removeAll();
        this.displayOptionBox = null;
    }
}
