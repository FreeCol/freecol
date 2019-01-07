/**
 * Copyright (C) 2002-2019   The FreeCol Team
 * <p>
 * This file is part of FreeCol.
 * <p>
 * FreeCol is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * FreeCol is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;


/**
 * A table cell editor that can be used to edit colors.
 */
public final class ColorCellEditor extends AbstractCellEditor
        implements TableCellEditor, ActionListener {

    private static final Logger logger = Logger.getLogger(ColorCellEditor.class.getName());

    private final FreeColClient freeColClient;
    private final JButton colorEditButton;
    private ColorChooserPanel colorChooserPanel = null;
    private Color currentColor;


    /**
     * The constructor to use.
     *
     * @param freeColClient The top level component that holds all
     *     other components.
     */
    public ColorCellEditor(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;

        this.colorEditButton = new JButton();
        this.colorEditButton.addActionListener(ae ->
            this.colorChooserPanel = freeColClient.getGUI()
                .showColorChooserPanel(this));
        this.colorEditButton.setBorderPainted(false);
    }


    // Implement TableCellEditor

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean hasFocus, int row, int column) {

        this.currentColor = (Color) value;
        return this.colorEditButton;
    }

    // Override CellEditor

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getCellEditorValue() {
        return this.currentColor;
    }

    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String command = ae.getActionCommand();
        GUI gui = freeColClient.getGUI();
        if (null != command) switch (command) {
            case FreeColPanel.OK:
                if (this.colorChooserPanel != null) {
                    this.currentColor = this.colorChooserPanel.getColor();
                    gui.removeComponent(this.colorChooserPanel);
                }
                fireEditingStopped();
                break;
            case FreeColPanel.CANCEL:
                if (this.colorChooserPanel != null) {
                    gui.removeComponent(this.colorChooserPanel);
                }
                fireEditingCanceled();
                break;
            default:
                logger.warning("Bad event: " + command);
                break;
        }
    }
}
