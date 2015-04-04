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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;


public final class MessageMerge {

    public static void main( String[] args )
    {
        if ( args.length != 2 )
        {
            System.err.println( "use: MessageMerge path-to-file-1 path-to-file-2" );
            System.exit( 1 );
        }

              String  pathToFile1 = args[0];
        final String  pathToFile2 = args[1];

        final MergeTableModel  mergeTableModel = new MergeTableModel();
        mergeTableModel.merge = new Merge();
        mergeTableModel.merge.lineFromFile1 = loadLinesFromFile( pathToFile1 );
        mergeTableModel.merge.lineFromFile2 = loadLinesFromFile( pathToFile2 );

        final JTable  mergeTable = new JTable( mergeTableModel );
        mergeTable.setSelectionMode( ListSelectionModel.SINGLE_INTERVAL_SELECTION );
        mergeTable.setDefaultRenderer( Object.class, new MergeTableCellRenderer() );

        Action  insertInRightAction = new AbstractAction( "insert in right" )
        {
            @Override
            public void actionPerformed( ActionEvent event )
            {
                int  from = mergeTable.getSelectionModel().getMinSelectionIndex();
                int  to = mergeTable.getSelectionModel().getMaxSelectionIndex();
                mergeTableModel.insertInRight( from, to );
            }
        };

        Action  deleteFromRightAction = new AbstractAction( "delete from right" )
        {
            @Override
            public void actionPerformed( ActionEvent event )
            {
                int  from = mergeTable.getSelectionModel().getMinSelectionIndex();
                int  to = mergeTable.getSelectionModel().getMaxSelectionIndex();
                mergeTableModel.deleteFromRight( from, to );
            }
        };

        Action  saveRightAction = new AbstractAction( "save right" )
        {
            @Override
            public void actionPerformed( ActionEvent event )
            {
                saveLinesToFile( mergeTableModel.merge.lineFromFile2, pathToFile2 );
            }
        };

        JPanel controlPanel = new JPanel(new GridLayout(1, 3));
        controlPanel.add(new JButton(insertInRightAction));
        controlPanel.add(new JButton(deleteFromRightAction));
        controlPanel.add(new JButton(saveRightAction));

        JPanel rootPane = new JPanel(new BorderLayout());
        rootPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        rootPane.add(new JScrollPane(mergeTable), BorderLayout.CENTER);
        rootPane.add(controlPanel, BorderLayout.SOUTH);

        JFrame frame = new JFrame("MessageMerge");
        frame.getContentPane().add(rootPane);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
                // for when the window is closed by the OS
                @Override
                public void windowClosing(WindowEvent event) {
                    System.exit(0);
                }

                // for when the window is closed by Java
                @Override
                public void windowClosed(WindowEvent event) {
                    windowClosing(event);
                }
            }
        );

        frame.pack();
        frame.setVisible(true);
    }


    private static List<String> loadLinesFromFile(String pathToFile) {
        try {
            List<String> lineList = new ArrayList<>();
            FileInputStream in = new FileInputStream( pathToFile );
            StringBuilder line = new StringBuilder();
            while ( true )
            {
                int  data = in.read();
                if ( -1 == data )
                {
                    if ( 0 < line.length() ) { lineList.add( line.toString() ); }
                    break;
                }
                char  c = (char) data;
                if ( '\r' == c )
                {
                    // do nothing
                }
                if ( '\n' == c )
                {
                    lineList.add( line.toString() );
                    line.setLength( 0 );
                }
                else {
                    line.append( c );
                }
            }
            in.close();
            return lineList;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    private static void saveLinesToFile( List<String> lineList, String pathToFile )
    {
        try {
            FileOutputStream  out = new FileOutputStream( pathToFile );
            for ( int lineNumber = 0, lines = lineList.size();  lineNumber < lines;  lineNumber ++ )
            {
                String  line = (String) lineList.get( lineNumber );
                for ( int i = 0;  i < line.length();  i ++ )
                {
                    out.write( line.charAt(i) );
                }
                out.write( '\n' );
            }
            out.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
