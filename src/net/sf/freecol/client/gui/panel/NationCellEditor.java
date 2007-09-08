
package net.sf.freecol.client.gui.panel;

import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Player;

/**
* A table cell editor that can be used to select a nation.
*/
public final class NationCellEditor extends DefaultCellEditor {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    
    /**
    * A standard constructor.
    */
    public NationCellEditor() {
        super(new JComboBox(new Vector(FreeCol.getSpecification().getClassicNations())));
    }
    
    public Object getCellEditorValue() {
        return new Integer(((JComboBox) getComponent()).getSelectedIndex());
    }
}
