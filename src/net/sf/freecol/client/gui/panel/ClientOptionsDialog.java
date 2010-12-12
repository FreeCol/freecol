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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JMenuBar;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.FreeColMenuBar;
import net.sf.freecol.client.gui.action.MapControlsAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.OptionGroupUI;

import net.miginfocom.swing.MigLayout;

/**
 * Dialog for changing the {@link net.sf.freecol.client.ClientOptions}.
 */
public final class ClientOptionsDialog extends FreeColDialog<Boolean>  {

    private static final Logger logger = Logger.getLogger(ClientOptionsDialog.class.getName());

    private OptionGroupUI ui;
    private JButton reset = new JButton(Messages.message("reset"));

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ClientOptionsDialog(Canvas parent) {
        super(parent);
        setLayout(new MigLayout("wrap 1", "[center]"));

        reset.setActionCommand(RESET);
        reset.addActionListener(this);

        setCancelComponent(cancelButton);

        setSize(850, 600);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(850, 600);
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    public void initialize() {
        removeAll();

        // Header:
        add(getDefaultHeader(getClient().getClientOptions().getName()));

        // Options:
        ui = new OptionGroupUI(getClient().getClientOptions());
        add(ui);

        // Buttons:
        add(okButton, "newline 20, split 3, tag ok");
        add(cancelButton);
        add(reset);
    }

    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            ui.unregister();
            ui.updateOption();
            getCanvas().remove(this);
            getClient().saveClientOptions();
            getClient().getActionManager().update();
            JMenuBar menuBar = getClient().getFrame().getJMenuBar();
            if (menuBar != null) {
                ((FreeColMenuBar) menuBar).reset();
            }
            setResponse(Boolean.TRUE);

            // Immediately redraw the minimap if that was updated.
            MapControlsAction mca = (MapControlsAction) getClient()
                .getActionManager().getFreeColAction(MapControlsAction.id);
            if (mca.getMapControls() != null) {
                mca.getMapControls().update();
            }
        } else if (CANCEL.equals(command)) {
            ui.rollback();
            ui.unregister();
            getCanvas().remove(this);
            setResponse(Boolean.FALSE);
        } else if (RESET.equals(command)) {
            ui.reset();
        } else {
            logger.warning("Invalid ActionCommand: " + command);
        }
    }
}
