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



package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ColopediaPanel;
import net.sf.freecol.client.gui.panel.ColopediaPanel.PanelType;
import net.sf.freecol.common.model.FreeColGameObjectType;


/**
 * Displays a section of the Colopedia.
 */
public class ColopediaAction extends FreeColAction {

    public static final String id = "colopediaAction";

    public static final int[] mnemonics = new int[] {
        KeyEvent.VK_T,
        KeyEvent.VK_R,
        KeyEvent.VK_U,
        KeyEvent.VK_G,
        KeyEvent.VK_S,
        KeyEvent.VK_B,
        KeyEvent.VK_F,
        KeyEvent.VK_N,
        KeyEvent.VK_A
    };

    private PanelType panelType = null;
    private FreeColGameObjectType object = null;

    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client.
     * @param panelType a <code>PanelType</code> value
     */
    public ColopediaAction(FreeColClient freeColClient, PanelType panelType) {
        super(freeColClient, id + "." + panelType);
        this.panelType = panelType;
        setMnemonic(mnemonics[panelType.ordinal()]);
    }

    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client.
     */
    public ColopediaAction(FreeColClient freeColClient, FreeColGameObjectType object) {
        super(freeColClient, id);
        putValue(NAME, Messages.message(id + ".name", "%object%", Messages.getName(object)));
        this.object = object;
    }

    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        Canvas canvas = freeColClient.getCanvas();
        canvas.showPanel(new ColopediaPanel(canvas, panelType, object));
    }
}
