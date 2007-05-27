
package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;
import cz.autel.dmi.HIGLayout;

/**
* A table cell editor that can be used to edit colors.
*/
public final class ColorCellEditor extends AbstractCellEditor implements TableCellEditor,
        ActionListener {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(EuropePanel.class.getName());

    private static final String EDIT = "EDIT",
                                OK = "OK",
                                CANCEL = "CANCEL";

    private final JButton              colorEditButton;
    private final JColorChooser        colorChooser;
    private final ColorChooserPanel    colorChooserPanel;
    private final JPanel               parent;
    private final Canvas               canvas;

    private Color currentColor;
    private Vector<Player> players;
    private int lastRow;
    private Player thisPlayer;


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
            lastRow = -1;

            JButton okButton = new JButton( Messages.message("ok") );
            JButton cancelButton = new JButton( Messages.message("cancel") );

            setLayout(new HIGLayout(new int[] {220, 10, 220}, new int[] {350, 10, 0}));

            add(colorChooser, higConst.rcwh(1, 1, 3, 1));
            add(okButton, higConst.rc(3, 1));
            add(cancelButton, higConst.rc(3, 3));

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
    * @param parent The parent JPanel of the table
    */
    public ColorCellEditor(Canvas canvas, JPanel parent) {
        this.canvas = canvas;
        this.parent = parent;

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
    * Gives this table model the data that is being used in the table.
    *
    * @param players The players that should be edited in the table.
    * @param owningPlayer The player running the client that is displaying the table.
    */
    public void setData(Vector<Player> players, Player owningPlayer) {
        this.players = players;
        thisPlayer = owningPlayer;
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
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals(EDIT)) {
            colorChooser.setColor(currentColor);

            // Add the colorChooserPanel.
            canvas.add(colorChooserPanel, JLayeredPane.PALETTE_LAYER);
            parent.setEnabled(false);
            // No repainting needed apparently.
        }
        else if (event.getActionCommand().equals(OK)) {
            currentColor = colorChooser.getColor();

            if ((lastRow >= 0) && (lastRow < players.size())) {
                Player player = getPlayer(lastRow);
                player.setColor(currentColor);
            }

            // Remove the colorChooserPanel.
            canvas.remove(colorChooserPanel);
            parent.setEnabled(true);
            // No repainting needed apparently.

            fireEditingStopped();
        }
        else if (event.getActionCommand().equals(CANCEL)) {
            // Remove the colorChooserPanel.
            canvas.remove(colorChooserPanel);
            parent.setEnabled(true);
            // No repainting needed apparently.
            
            fireEditingCanceled();
        }
        else {
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
        lastRow = row;
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
