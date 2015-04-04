/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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

package net.sf.freecol.common.i18n;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


public final class MergeTableCellRenderer extends DefaultTableCellRenderer
{

    @Override
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
