package net.sf.freecol.client.networking;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.networking.ServerAPI;

import org.w3c.dom.Element;

public class UserServerAPI extends ServerAPI {

    public static final String COPYRIGHT = "Copyright (C) 2003-2012 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";


    private GUI gui;

    public UserServerAPI(GUI gui) {
        super();
        this.gui = gui;
    }

    @Override
    protected void doRaiseErrorMessage(String complaint) {
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.COMMS)) {
            gui.errorMessage(null, complaint);
        }
    }

    @Override
    protected void doClientProcessingFor(Element reply) {
        String sound = reply.getAttribute("sound");
        if (sound != null && !sound.isEmpty()) {
            gui.playSound(sound);
        }
    }
}
