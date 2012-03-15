package net.sf.freecol.client.gui.panel;

public class Parameters {

    public static final String COPYRIGHT = "Copyright (C) 2003-2012 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public int distToLandFromHighSeas;

    public int maxDistanceToEdge;


    Parameters(int distToLandFromHighSeas, int maxDistanceToEdge) {
        this.distToLandFromHighSeas = distToLandFromHighSeas;
        this.maxDistanceToEdge = maxDistanceToEdge;
    }
}
