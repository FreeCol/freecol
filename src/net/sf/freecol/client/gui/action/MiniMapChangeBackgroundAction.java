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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;


/**
 * An action for changing the minimap's background color.
 * 
 * Required:
 * 		Updates to ClientOptions.java to display the option
 * 		Additions to FreeColMessages.properties to display the strings
 * 		MiniMap.java needed the attribute, setter, and changes to paintComponent to work.
 * 		MapControls.java needed a new method to pass-through the data to MiniMap
 * 		ActionManager.java got the new action in the huge list
 * BUT WHAT HOOKS THE DIALOG TO THE ACTION???
 */
public class MiniMapChangeBackgroundAction extends MapboardAction {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MiniMapChangeBackgroundAction.class.getName());

    public static final String id = "miniMapChangeBackgroundAction";


    /**
     * Creates a new <code>MiniMapChangeBackgroundAction</code>.
     * @param freeColClient The main controller object for the client.
     */
    MiniMapChangeBackgroundAction(FreeColClient freeColClient) {
        super(freeColClient, "black", null);
    }
    
    
    /**
    * Returns the id of this <code>Option</code>.
    * @return "miniMapZoomInAction"
    */
    public String getId() {
        return id;
    }
    

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if the minimap can be zoomed in.
     */
    protected boolean shouldBeEnabled() {
        MapControlsAction mca = (MapControlsAction) getFreeColClient().getActionManager().getFreeColAction(MapControlsAction.id);
        return super.shouldBeEnabled()
                && mca.getMapControls() != null;
    }  
    
    /**
     * Applies this action.
     * @param ae The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent ae) {
        MapControlsAction mca = (MapControlsAction) getFreeColClient().getActionManager().getFreeColAction(MapControlsAction.id);
        final String whichColor = ae.getActionCommand();
        Color toColor = Color.BLACK;
        if( whichColor != null ) {
        	if( whichColor.equalsIgnoreCase("gray.light")) {
        		toColor = new Color(220,220,220);
        	} else if( whichColor.equalsIgnoreCase("gray.dark")) {
            		toColor = new Color(100,100,100);
        	} else if( whichColor.equalsIgnoreCase("gray")) {
        		toColor = new Color(160,160,160);
        	} else if( whichColor.equalsIgnoreCase("blue.light")) {
        		toColor = new Color(255,255,200);
        	}
        }
        mca.getMapControls().changeBackgroundColor(toColor);
        update();
        getFreeColClient().getActionManager().getFreeColAction(MiniMapChangeBackgroundAction.id).update();
    }

    /**
     * Ouch, bad news if in here we have to interpret a select boxes' indexes...
     * @param index
     * @return the color
     */
    public static Color interpretIndex(int index) {
        Color toColor = Color.BLACK;
        switch( index ) {
        case 1:
        	return new Color(48,48,48);
        case 2:
        	return new Color(96,96,96);
        case 3:
        	return new Color(128,128,128);
        case 4:
        	return new Color(176,176,176);
        case 5:
        	return new Color(224,224,224);
        case 6:
        	return new Color(200,200,255);
        }
    	return Color.BLACK;
//        if( whichColor != null ) {
//        	if( whichColor.equalsIgnoreCase("gray.light")) {
//        		toColor = new Color(220,220,220);
//        	} else if( whichColor.equalsIgnoreCase("gray.dark")) {
//            		toColor = new Color(100,100,100);
//        	} else if( whichColor.equalsIgnoreCase("gray")) {
//        		toColor = new Color(160,160,160);
//        	} else if( whichColor.equalsIgnoreCase("blue.light")) {
//        		toColor = new Color(255,255,200);
//        	}
//        }
    }
}
