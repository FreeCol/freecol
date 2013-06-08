package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.option.OptionUI;
import net.sf.freecol.common.option.Option;


/**
 * Dialog to edit options with.
 */
public class EditOptionDialog extends FreeColDialog<Boolean> {

    public static final String COPYRIGHT = "Copyright (C) 2003-2012 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";
    
    private OptionUI ui;

    public EditOptionDialog(FreeColClient freeColClient, GUI gui, Option option) {
        super(freeColClient, gui);
        setLayout(new MigLayout()); 
        ui = OptionUI.getOptionUI(gui, option, editable);
        if (ui.getLabel() == null) {
            add(ui.getLabel(), "split 2");
        }
        add(ui.getComponent());

        add(okButton, "newline, split 2, tag ok");
        add(cancelButton, "tag cancel");
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            ui.updateOption();
            setResponse(true);
        } else {
            setResponse(false);
        }
    }

}
