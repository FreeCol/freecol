
package net.sf.freecol.client.gui.panel;

import net.sf.freecol.common.model.Player;

import java.awt.Color;
import java.awt.Component;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
* A table cell renderer that should be used to display the chosen nation in a table.
* It is being used in the players table (StartGamePanel).
*/
public final class NationCellRenderer implements TableCellRenderer {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final JComboBox standardNationsComboBox = new JComboBox(Player.NATIONS);
    private static final JComboBox indianTribesComboBox = new JComboBox(Player.TRIBES);

    private Vector players;

    /**
    * The default constructor.
    */
    public NationCellRenderer() {
    }


    /**
    * Sets the players that should be rendered in the table.
    * @param players The players that should be rendered in the table.
    */
    public void setPlayers(Vector players) {
        this.players = players;
    }


    /**
    * Returns the component used to render the cell's value.
    * @param table The table whose cell needs to be rendered.
    * @param value The value of the cell being rendered.
    * @param hasFocus Indicates whether or not the cell in question has focus.
    * @param row The row index of the cell that is being rendered.
    * @param column The column index of the cell that is being rendered.
    * @return The component used to render the cell's value.
    */
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Player player = (Player)players.get(row);
        if (player.isAI()) {
            int nation = player.getNation();
            if ((nation != Player.DUTCH) && (nation != Player.ENGLISH)
                    && (nation != Player.FRENCH) && (nation != Player.SPANISH)) {
                // This is an indian AI player.

                indianTribesComboBox.setForeground(Color.LIGHT_GRAY);
                return indianTribesComboBox;
            }
            else {
                standardNationsComboBox.setForeground(Color.LIGHT_GRAY);
            }
        }
        else if (player.isReady()) {
            standardNationsComboBox.setForeground(Color.GRAY);
        }
        else {
            standardNationsComboBox.setForeground(table.getForeground());
        }
        standardNationsComboBox.setBackground(table.getBackground());

        standardNationsComboBox.setSelectedItem(value);
        return standardNationsComboBox;
    }
}
