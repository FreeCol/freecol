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

package net.sf.freecol.client;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;


public final class WorkerCheck {

    private final  JTextArea  textArea;
    private final  Action     workAction;


    public static void main( String[] args ) {

        new WorkerCheck();
    }


    private WorkerCheck() {

        final Worker  worker = new Worker();

        textArea = new JTextArea( 10, 40 );
        textArea.setEditable( false );

        final Runnable workerJob = () -> {
            try {
                SwingUtilities.invokeLater( new TestJob("starting the job\n", false) );
                SwingUtilities.invokeLater( new TestJob("working.  check that the AWT thread can repaint this window\n", false) );
                // this example sleeps, but time-consuming work could be done
                Thread.sleep( 7000 );
                SwingUtilities.invokeLater( new TestJob("the job is done\n", true) );
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        workAction = new AbstractAction("work") {
            @Override
            public void actionPerformed( ActionEvent event ) {
                setEnabled( false );
                worker.schedule( workerJob );
            }
        };

        JPanel buttonPane = new JPanel();
        buttonPane.add(new JButton(workAction));

        JFrame window = new JFrame("WorkerTest");
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.getContentPane().add(textArea, BorderLayout.CENTER);
        window.getContentPane().add(buttonPane, BorderLayout.SOUTH);
        window.pack();
        window.addWindowListener(new WindowAdapter() {
                // for when the window is closed by the OS
                @Override
                public void windowClosing(WindowEvent event) {
                    worker.askToStop();
                }

                // for when the window is closed by Java
                @Override
                public void windowClosed(WindowEvent event) {
                    windowClosing(event);
                }
            }
        );
        window.setVisible(true);

        // the worker is being run on the main thread in this example
        worker.run();
    }


    final class TestJob implements Runnable {

        private final  String   message;
        private final  boolean  enableAction;

        TestJob( String message, boolean enableAction ) {

            this.message = message;
            this.enableAction = enableAction;
        }

        /** This method is invoked on the AWT thread */
        @Override
        public void run() {

            textArea.setText( textArea.getText() + message);
            if ( enableAction ) { workAction.setEnabled(true); }
        }
    }

}
