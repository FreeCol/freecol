
package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Component;
import java.util.List;
//import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;

/**
* A table cell renderer that should be used to display the chosen nation in a table.
* It is being used in the players table (StartGamePanel).
*/
public final class NationCellRenderer implements TableCellRenderer {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /*
    private static Vector<Nation> classicNations = 
        new Vector<Nation>(FreeCol.getSpecification().getClassicNations());
    private static final JComboBox classicNationsComboBox = new JComboBox(classicNations);

    private static Vector<Nation> europeanNations = 
        new Vector<Nation>(FreeCol.getSpecification().getEuropeanNations());
    private static final JComboBox europeanNationsComboBox = new JComboBox(europeanNations);
    */

    private final Nation[] nations;
    private final JComboBox comboBox;
    private List<Player> players;
    private Player thisPlayer;
    //private boolean additionalNations;

    /**
    * The default constructor.
    */
    public NationCellRenderer(Nation[] nations) {
        this.nations = nations;
        this.comboBox = new JComboBox(nations);
    }


    /**
    * Gives this table model the data that is being used in the table.
    *
    * @param players The players that should be rendered in the table.
    * @param owningPlayer The player running the client that is displaying the table.
    */
    public void setData(List<Player> players, Player owningPlayer) {
        //, boolean additionalNations) {
        this.players = players;
        thisPlayer = owningPlayer;
        //this.additionalNations = additionalNations;
    }

    private Player getPlayer(int i) {
        if (i == 0) {
            return thisPlayer;
        } else if (players.get(i) == thisPlayer) {
            return players.get(0);
        } else {
            return players.get(i);
        }
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

        Player player = getPlayer(row);

        Component component;
        if (player == thisPlayer) {
            /*
            Vector<Nation> nations;
            JComboBox comboBox;
            if (additionalNations) {
                nations = europeanNations;
                comboBox = europeanNationsComboBox;
                System.out.println("size is " + comboBox.getItemCount());
                for (int index = 0; index < comboBox.getItemCount(); index++) {
                    System.out.println(comboBox.getItemAt(index));
                }
            } else {
                nations = classicNations;
                comboBox = classicNationsComboBox;
            }
            */
            for (int index = 0; index < nations.length; index++) {
                if (nations[index].getID().equals(player.getNationID())) {
                    comboBox.setSelectedIndex(index);
                    break;
                }
            }
            component = comboBox;
        } else {
            component = new JLabel(player.getNationAsString());
        }

        if (player.isReady()) {
            component.setForeground(Color.GRAY);
        } else {
            component.setForeground(table.getForeground());
        }
        component.setBackground(table.getBackground());

        return component;
    }
}
