
package net.sf.freecol.client.gui.i18n;


import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


public final class MergeTableCellRenderer extends DefaultTableCellRenderer
{

    public Component getTableCellRendererComponent( JTable   table,
                                                    Object   value,
                                                    boolean  isSelected,
                                                    boolean  hasFocus,
                                                    int      rowIndex,
                                                    int      columnIndex )
    {
        MergeTableModel  model = (MergeTableModel) table.getModel();
        String  leftProperty = propertyOn( model.leftLineAtRow(rowIndex) );
        String  rightProperty = propertyOn( model.rightLineAtRow(rowIndex) );
        boolean  same = leftProperty.equals( rightProperty );
        Component  c = super.getTableCellRendererComponent( table,
                                                            value,
                                                            isSelected,
                                                            hasFocus,
                                                            rowIndex,
                                                            columnIndex );
        c.setForeground( same ? Color.BLACK : Color.RED );
        return c;
    }


    private static String propertyOn( String line )
    {
        if ( null == line ) { return ""; }
        int  indexOfEquals = line.indexOf( '=' );
        return ( indexOfEquals != -1 ) ? line.substring( 0, indexOfEquals ) : "";
    }

}
