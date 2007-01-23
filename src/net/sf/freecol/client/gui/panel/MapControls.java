
package net.sf.freecol.client.gui.panel;

import javax.swing.Action;
import javax.swing.JComponent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.gui.action.BuildColonyAction;
import net.sf.freecol.client.gui.action.BuildRoadAction;
import net.sf.freecol.client.gui.action.DisbandUnitAction;
import net.sf.freecol.client.gui.action.FortifyAction;
import net.sf.freecol.client.gui.action.PlowAction;
import net.sf.freecol.client.gui.action.SentryAction;
import net.sf.freecol.client.gui.action.SkipUnitAction;
import net.sf.freecol.client.gui.action.WaitAction;


/**
* A collection of panels and buttons that are used to provide
* the user with a more detailed view of certain elements on the
* map and also to provide a means of input in case the user
* can't use the keyboard.
*
* The MapControls are useless by themselves, this object needs to
* be placed on a JComponent in order to be useable.
*/
public final class MapControls {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    public static final int EUROPE = 2;
    public static final int UNITBUTTON = 3;
    
    private JComponent  container;
    private FreeColClient freeColClient;

    private final InfoPanel        infoPanel;
    private final MiniMap          miniMap;
    private final UnitButton[]     unitButton;   
    private GUI                    gui;




    /**
    * The basic constructor.
    * @param freeColClient The main controller object for the client
    * @param gui An object that contains useful GUI-related methods.
    */
    public MapControls(FreeColClient freeColClient, GUI gui) {
        this.freeColClient = freeColClient;
        this.gui = gui;
        container = null;

        //
        // Create GUI Objects
        //

        infoPanel = new InfoPanel(freeColClient, freeColClient.getGame(), freeColClient.getGUI().getImageLibrary());
        miniMap = new MiniMap(freeColClient, freeColClient.getGUI().getImageLibrary(), container);
        
        final ActionManager am = freeColClient.getActionManager();
        unitButton = new UnitButton[] {
            new UnitButton(am.getFreeColAction(WaitAction.ID)),
            new UnitButton(am.getFreeColAction(SkipUnitAction.ID)),
            new UnitButton(am.getFreeColAction(SentryAction.ID)),
            new UnitButton(am.getFreeColAction(FortifyAction.ID)),
            new UnitButton(am.getFreeColAction(PlowAction.ID)),
            new UnitButton(am.getFreeColAction(BuildRoadAction.ID)),
            new UnitButton(am.getFreeColAction(BuildColonyAction.ID)),
            new UnitButton(am.getFreeColAction(DisbandUnitAction.ID))
        };
        
        //
        // Don't allow them to gain focus
        //

        infoPanel.setFocusable(false);
        miniMap.setFocusable(false);
        for(int i=0; i<unitButton.length; i++) {
            unitButton[i].setFocusable(false);
        }
    }


    /**
    * Adds the map controls to the given component.
    * @param component The component to add the map controls to.
    */
    public void addToComponent(JComponent component) {
        container = component;
        
        miniMap.setContainer(container);
        
        //
        // Relocate GUI Objects
        //

        infoPanel.setLocation(container.getWidth() - infoPanel.getWidth(), container.getHeight() - infoPanel.getHeight());
        miniMap.setLocation(0, container.getHeight() - miniMap.getHeight());
        
        final int SPACE = unitButton[0].getWidth() + 5;
        for(int i=0; i<unitButton.length; i++) {            
            unitButton[i].setLocation(miniMap.getWidth() + (infoPanel.getX() - miniMap.getWidth() - unitButton.length*SPACE)/2 + i*SPACE, container.getHeight() - 40);
        }

        //
        // Add the GUI Objects to the container
        //

        container.add(infoPanel);
        container.add(miniMap);

        for(int i=0; i<unitButton.length; i++) {
            container.add(unitButton[i]);
            Action a = unitButton[i].getAction();
            unitButton[i].setAction(null);
            unitButton[i].setAction(a);
        }
    }


    /**
    * Removes the map controls from the given component.
    * @param component The component to remove the map controls from.
    */
    public void removeFromComponent(JComponent component) {
        //
        // Remove the GUI Objects from the container
        //

        if (container != null) {
            JComponent temp = container;
            container = null;

            temp.remove(infoPanel);
            temp.remove(miniMap);

            for(int i=0; i<unitButton.length; i++) {
                temp.remove(unitButton[i]);
            }
        }
    }


    public boolean isShowing() {
        return container != null;
    }


    /**
    * Zooms in the mini map.
    */
    public void zoomIn() {
        miniMap.zoomIn();
    }


    /**
    * Zooms out the mini map.
    */
    public void zoomOut() {
        miniMap.zoomOut();
    }
    
    public boolean canZoomIn() {
        return miniMap.canZoomIn();
    }

    public boolean canZoomOut() {
        return miniMap.canZoomOut();
    }

    /**
    * Updates this <code>MapControls</code>.
    */
    public void update() {
        if (infoPanel.getUnit() != freeColClient.getGUI().getActiveUnit()) {
            infoPanel.update(freeColClient.getGUI().getActiveUnit());
        }
    }
}
