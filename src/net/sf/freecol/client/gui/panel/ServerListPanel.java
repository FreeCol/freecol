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

package net.sf.freecol.client.gui.panel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import net.miginfocom.swing.MigLayout;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.common.metaserver.ServerInfo;
import static net.sf.freecol.common.util.CollectionUtils.*;



/**
 * This panel is used to display the information received from the meta-server.
 */
public final class ServerListPanel extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(ServerListPanel.class.getName());

    private static class ServerListTableCellRenderer
        extends DefaultTableCellRenderer {

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(JTable t, Object o,
            boolean isSelected, boolean hasFocus, int row, int column) {
            setOpaque(isSelected);
            return super.getTableCellRendererComponent(t, o, isSelected,
                                                       hasFocus, row, column);
        }
    };
        
    private final ConnectController connectController;

    private final JTable table;

    private final ServerListTableModel tableModel;

    private final JButton connect;

    
    /**
     * Creates a panel to display the meta-server.
     * 
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param connectController The controller responsible for creating new
     *     connections.
     */
    public ServerListPanel(FreeColClient freeColClient,
                           ConnectController connectController) {
        super(freeColClient, null, new MigLayout("", "", ""));

        this.connectController = connectController;

        JButton cancel = Utility.localizedButton("cancel");
        JScrollPane tableScroll;

        setCancelComponent(cancel);

        connect = Utility.localizedButton("connect");

        tableModel = new ServerListTableModel(new ArrayList<ServerInfo>());
        table = new JTable(tableModel);

        DefaultTableCellRenderer dtcr = new ServerListTableCellRenderer();
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(dtcr);
        }

        table.setRowHeight(22);

        table.setCellSelectionEnabled(false);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tableScroll = new JScrollPane(table);
        table.addNotify();
        tableScroll.getViewport().setOpaque(false);
        tableScroll.getColumnHeader().setOpaque(false);

        connect.addActionListener(ae -> {
                ServerInfo si = tableModel.getItem(table.getSelectedRow());
                connectController.joinMultiplayerGame(si.getAddress(),
                                                      si.getPort());
            });

        cancel.addActionListener(ae -> {
                getGUI().removeComponent(this);
                getGUI().showNewPanel();
            });

        add(tableScroll, "width 400:, height 350:");
        add(connect, "newline 20, split 2");
        add(cancel, "tag cancel");

        setSize(getPreferredSize());
    }


    @Override
    public void requestFocus() {
        connect.requestFocus();
    }

    /**
     * Initializes the data that is displayed in this panel.
     * 
     * @param servers A list of {@code ServerInfo}-objects to be
     *            displayed.
     */
    public void initialize(List<ServerInfo> servers) {
        // FIXME: This should be added as a filtering rule:
        // Remove servers with an incorrect version from the list:
        removeInPlace(servers,
                      si -> !si.getVersion().equals(FreeCol.getVersion()));

        tableModel.setItems(servers);
        setEnabled(true);
        if (servers.isEmpty()) {
            connect.setEnabled(false);
        } else {
            table.setRowSelectionInterval(0, 0);
        }
    }

    /**
     * Sets whether or not this component is enabled. It also does this for its
     * children.
     * 
     * @param enabled 'true' if this component and its children should be
     *            enabled, 'false' otherwise.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        Component[] components = getComponents();
        for (Component component : components) {
            component.setEnabled(enabled);
        }

        table.setEnabled(enabled);
    }

    /**
     * Refreshes the table.
     */
    public void refreshTable() {
        tableModel.fireTableDataChanged();
    }
}
