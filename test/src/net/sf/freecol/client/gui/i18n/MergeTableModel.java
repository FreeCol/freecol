
package net.sf.freecol.client.gui.i18n;


import javax.swing.table.AbstractTableModel;


public final class MergeTableModel extends AbstractTableModel
{
    private static final String[]  COLUMN_NAME_ARRAY = { "file1", "file2" };

    Merge  merge;


    // ------------------------------------------------------------ API methods

    void mergeChanged()
    {
        fireTableDataChanged();
    }


    String leftLineAtRow( int rowIndex )
    {
        return rowIndex < merge.lineFromFile1.size()
            ? (String) merge.lineFromFile1.get(rowIndex)
            : null;
    }


    String rightLineAtRow( int rowIndex )
    {
        return rowIndex < merge.lineFromFile2.size()
            ? (String) merge.lineFromFile2.get(rowIndex)
            : null;
    }


    public void insertInRight( int from, int to )
    {
        for ( int i = from;  i <= to;  i ++ )
        {
            String  line = leftLineAtRow( i );
            // prefix newly inserted lines with an "@" to make it easy to find
            // them when translating the new messages
            line = "@" + line;
            merge.lineFromFile2.add( i, line );
        }

        mergeChanged();
    }


    public void deleteFromRight( int from, int to )
    {
        for ( int i = to;  from <= i;  i -- )
        {
            merge.lineFromFile2.remove( i  );
        }

        mergeChanged();
    }


    // ----------------------------------------------------- overridden methods

    public int getRowCount()
    {
        return Math.max( merge.lineFromFile1.size(), merge.lineFromFile2.size() );
    }


    public int getColumnCount()
    {
        return COLUMN_NAME_ARRAY.length;
    }


    public String getColumnName( int columnIndex )
    {
        return COLUMN_NAME_ARRAY[ columnIndex ];
    }


    public Object getValueAt( int rowIndex, int columnIndex )
    {
        if ( 0 == columnIndex )
        {
            return leftLineAtRow( rowIndex );
        }
        else if ( 1 == columnIndex )
        {
            return rightLineAtRow( rowIndex );
        }
        else { throw new RuntimeException( Integer.toString(columnIndex) ); }
    }

}
