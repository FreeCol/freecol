
package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;

import net.sf.freecol.client.FreeColClient;


/**
 * An action for initiating chatting.
 *
 * @see net.sf.freecol.client.gui.panel.MapControls
 */
public class ToDoAction extends FreeColAction {

    public static final String id = "ToDoAction";


    /**
     * Creates a new {@code ChatAction}.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ToDoAction(FreeColClient freeColClient) {
        super(freeColClient, id);
    }


    // Override FreeColAction

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled()
            && !freeColClient.getSinglePlayer()
            && (!getGUI().isPanelShowing() || getGame() != null
                && !freeColClient.currentPlayerIsMyPlayer());
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        getGUI().showToDoPanel();
    }
}
