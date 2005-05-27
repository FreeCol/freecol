
package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

/**
* A table cell renderer that should be used to display the chosen color in a table.
* It is being used in the players table (StartGamePanel).
*/
public class ColorCellRenderer extends JLabel implements TableCellRenderer {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private Border border = null;
    private boolean useBorder = true;

    /**
    * The constructor to use.
    * @param useBorder Indicated whether or not a border should be drawn.
    */
    public ColorCellRenderer(boolean useBorder) {
        this.useBorder = useBorder;

        // This must be done because the background displays the actual color:
        setOpaque(true);
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
    public Component getTableCellRendererComponent(JTable table, Object color,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Color myColor = (Color)color;

        setBackground(myColor);

        if (useBorder) {
            if (border == null) {
                border = BorderFactory.createMatteBorder(2,5,2,5, table.getBackground());
            }
            setBorder(border);
        }

        setToolTipText("RGB value: " + myColor.getRed() + ", " + myColor.getGreen() + ", "
                + myColor.getBlue());

        return this;
    }
}
