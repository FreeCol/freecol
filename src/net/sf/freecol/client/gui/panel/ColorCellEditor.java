/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;

import net.miginfocom.swing.MigLayout;


/**
* A table cell editor that can be used to edit colors.
*/
public final class ColorCellEditor extends AbstractCellEditor implements TableCellEditor,
        ActionListener {


    private static final Logger logger = Logger.getLogger(ColorCellEditor.class.getName());

    private static final String EDIT = "EDIT",
                                OK = "OK",
                                CANCEL = "CANCEL";

    private final JButton              colorEditButton;
    private final JColorChooser        colorChooser;
    private final ColorChooserPanel    colorChooserPanel;
    private final Canvas               canvas;

    private Color currentColor;


    /**
    * This class represents a panel that holds a JColorChooser and OK and cancel buttons.
    * Once constructed this panel is comparable to the dialog that is returned from
    * JColorChooser::createDialog.
    */
    private final class ColorChooserPanel extends FreeColPanel {

        /**
        * The constructor to use.
        * @param l The ActionListener for the OK and cancel buttons.
        */
        public ColorChooserPanel(ActionListener l) {
            super(canvas);

            JButton okButton = new JButton( Messages.message("ok") );
            JButton cancelButton = new JButton( Messages.message("cancel") );

            setLayout(new MigLayout("", "", ""));

            add(colorChooser);
            add(okButton, "newline 20, split 2, tag ok");
            add(cancelButton, "tag cancel");

            okButton.setActionCommand(OK);
            cancelButton.setActionCommand(CANCEL);

            okButton.addActionListener(l);
            cancelButton.addActionListener(l);

            setOpaque(true);
            setSize(getPreferredSize());
        }
    }


    /**
    * The constructor to use.
    * @param canvas The top level component that holds all other components.
    */
    public ColorCellEditor(Canvas canvas) {
        this.canvas = canvas;

        colorEditButton = new JButton();
        colorEditButton.setActionCommand(EDIT);
        colorEditButton.addActionListener(this);
        colorEditButton.setBorderPainted(false);

        colorChooser = new JColorChooser();

        colorChooserPanel = new ColorChooserPanel(this);
        colorChooserPanel.setLocation(canvas.getWidth() / 2 - colorChooserPanel.getWidth()
                / 2, canvas.getHeight() / 2 - colorChooserPanel.getHeight() / 2);
    }


    /**
    * This function analyzes an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals(EDIT)) {
            if (!canvas.isAncestorOf(colorChooserPanel)) {
                colorChooser.setColor(currentColor);
    
                // Add the colorChooserPanel.
                canvas.addAsFrame(colorChooserPanel);
                colorChooserPanel.requestFocus();
            }
        } else if (event.getActionCommand().equals(OK)) {
            currentColor = colorChooser.getColor();
            // Remove the colorChooserPanel.
            canvas.remove(colorChooserPanel);
            fireEditingStopped();
        } else if (event.getActionCommand().equals(CANCEL)) {
            // Remove the colorChooserPanel.
            canvas.remove(colorChooserPanel);
            fireEditingCanceled();
        } else {
            logger.warning("Invalid action command");
        }
    }


    /**
    * Returns the component used to edit the cell's value.
    * @param table The table whose cell needs to be edited.
    * @param value The value of the cell being edited.
    * @param hasFocus Indicates whether or not the cell in question has focus.
    * @param row The row index of the cell that is being edited.
    * @param column The column index of the cell that is being edited.
    * @return The component used to edit the cell's value.
    */
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean hasFocus, int row, int column) {

        currentColor = (Color)value;
        return colorEditButton;
    }


    /**
    * Returns the value of the cell editor.
    * @return The value of the cell editor.
    */
    public Object getCellEditorValue() {
        return currentColor;
    }
}
