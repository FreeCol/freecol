
package net.sf.freecol.client.gui.panel;


import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.FlowLayout;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
//import net.sf.freecol.client.control.UserInputHandler;


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
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    public static final int EUROPE = 2;
    public static final int UNITBUTTON = 3;
    public static final int MINIMAP_ZOOMOUT = 13;
    public static final int MINIMAP_ZOOMIN = 14;

    private JComponent  container;
    private FreeColClient freeColClient;
    
    private final JButton          miniMapZoomOutButton;
    private final JButton          miniMapZoomInButton;
    private final InfoPanel        infoPanel;
    private final MiniMap          miniMap;
    //private final Game             game;
    //private final ImageProvider    imageProvider;
    private final UnitButton[]     unitButton;
    private final int              NUMBER_OF_BUTTONS = 8;
    //private UserInputHandler       userInputHandler;
    private GUI                    gui;
    
    /**
    * The basic constructor.
    */
    public MapControls(FreeColClient freeColClient, GUI gui) {
        this.freeColClient = freeColClient;
        this.gui = gui;
        //this.game = game;
        //this.imageProvider = imageProvider;
        container = null;
        
        //
        // Create GUI Objects
        //
        
        miniMapZoomOutButton = new JButton("-");
        miniMapZoomInButton = new JButton("+");
        
        miniMapZoomOutButton.setOpaque(true);
        miniMapZoomInButton.setOpaque(true);

        infoPanel = new InfoPanel(freeColClient, freeColClient.getGame(), freeColClient.getGUI().getImageLibrary());
        miniMap = new MiniMap(freeColClient, freeColClient.getGame().getMap(), freeColClient.getGUI().getImageLibrary(), container);
        unitButton = new UnitButton[NUMBER_OF_BUTTONS];
        for(int i=0; i<NUMBER_OF_BUTTONS; i++) {
            unitButton[i] = new UnitButton(freeColClient, gui);
        }
        
        
        //
        // Don't allow them to gain focus
        //
        
        infoPanel.setFocusable(false);
        miniMap.setFocusable(false);
        miniMapZoomOutButton.setFocusable(false);
        miniMapZoomInButton.setFocusable(false);
        for(int i=0; i<NUMBER_OF_BUTTONS; i++) {
            unitButton[i].setFocusable(false);
        }
        
        
        //
        // Resize GUI Objects
        //
        
        miniMapZoomOutButton.setSize(50, 20);
        miniMapZoomInButton.setSize(50, 20);
        
        
        //
        // Set ActionCommands
        //
        
        miniMapZoomOutButton.setActionCommand(String.valueOf(MINIMAP_ZOOMOUT));
        miniMapZoomInButton.setActionCommand(String.valueOf(MINIMAP_ZOOMIN));
        for(int i=0; i<NUMBER_OF_BUTTONS; i++) {
            unitButton[i].setActionCommand(String.valueOf(UNITBUTTON + i));
        }        
    }
    
    /**
    * Adds the map controls to the given component.
    * @param component The component to add the map controls to.
    */
    public void addToComponent(JComponent component) {
        container = component;
        miniMap.setContainer(container);
        for(int i=0; i<NUMBER_OF_BUTTONS; i++) {
            unitButton[i].setContainer(container);
        }

        // Initialize Unit-Buttons
        ImageProvider imageProvider = freeColClient.getGUI().getImageLibrary();
        unitButton[0].initialize(UnitButton.UNIT_BUTTON_WAIT, imageProvider);
        unitButton[1].initialize(UnitButton.UNIT_BUTTON_DONE, imageProvider);
        unitButton[2].initialize(UnitButton.UNIT_BUTTON_FORTIFY, imageProvider);
        unitButton[3].initialize(UnitButton.UNIT_BUTTON_SENTRY, imageProvider);
        unitButton[4].initialize(UnitButton.UNIT_BUTTON_CLEAR, imageProvider);
        unitButton[5].initialize(UnitButton.UNIT_BUTTON_ROAD, imageProvider);
        unitButton[6].initialize(UnitButton.UNIT_BUTTON_BUILD, imageProvider);
        unitButton[7].initialize(UnitButton.UNIT_BUTTON_DISBAND, imageProvider);
        
        //
        // Relocate GUI Objects
        //
        
        infoPanel.setLocation(container.getWidth() - infoPanel.getWidth(), container.getHeight() - infoPanel.getHeight());
        miniMap.setLocation(0, container.getHeight() - miniMap.getHeight());
        for(int i=0; i<NUMBER_OF_BUTTONS; i++) {
            int SPACE = unitButton[0].getWidth() + 5;
            unitButton[i].setLocation(miniMap.getWidth() + (infoPanel.getX() - miniMap.getWidth() - NUMBER_OF_BUTTONS*SPACE)/2 + i*SPACE, container.getHeight() - 40);
        }
        
        JPanel mapZoomButtons = new JPanel(new FlowLayout());

        miniMapZoomOutButton.setLocation(0, container.getHeight() - miniMapZoomOutButton.getHeight());
        miniMapZoomInButton.setLocation(miniMapZoomOutButton.getWidth(), container.getHeight() - miniMapZoomInButton.getHeight());


        //
        // Add the GUI Objects to the container
        //

        container.add(miniMapZoomOutButton);
        container.add(miniMapZoomInButton);


        container.add(infoPanel);
        container.add(miniMap);
        for(int i=0; i<NUMBER_OF_BUTTONS; i++) {
            container.add(unitButton[i]);
        }

        updateButtons();
    }

    /**
    * Removes the map controls from the given component.
    * @param component The component to remove the map controls from.
    */
    public void removeFromComponent(JComponent component) {
        //
        // Remove the GUI Objects from the container
        //
        
        container.remove(infoPanel);
        container.remove(miniMap);
        container.remove(miniMapZoomOutButton);
        container.remove(miniMapZoomInButton);
        for(int i=0; i<NUMBER_OF_BUTTONS; i++) {
            container.remove(unitButton[i]);
        }
        
        container = null;
    }
    
    /**
    * Adds an ActionListener to the map controls.
    * @param l The ActionListener to add to the map controls.
    */
    public void addActionListener(ActionListener l) {
        //
        // Add ActionListener to GUI Objects
        //    
    
        miniMapZoomOutButton.addActionListener(l);
        miniMapZoomInButton.addActionListener(l);
        for(int i=0; i<NUMBER_OF_BUTTONS; i++) {
            unitButton[i].addActionListener(l);
        }
    }
    
    /**
    * Sets the userInputHandler field
    * @param userInputHandler the UserInputHandler to set
    */
    /*public void setUserInputHandler(UserInputHandler userInputHandler) {
        this.userInputHandler = userInputHandler;
        for (int i=0; i<NUMBER_OF_BUTTONS; i++) {
            unitButton[i].setUserInputHandler(userInputHandler);
        }
    }*/
    
    /**
    * Zooms in the mini map
    */
    public void zoomIn() {
        miniMap.zoomIn();
    }
    
    /**
    * Zooms out the mini map
    */
    public void zoomOut() {
        miniMap.zoomOut();
    }

    /**
    * Updates the moves of the displayed unit (or erases it if null provided).
    * @param unit The displayed unit (or null if none)
    */
    public void updateMoves(Unit unit) {
        infoPanel.updateMoves(unit);
    }
    
    /**
    * Pushes one of the unit buttons
    * @param buttonNumber The index of the button to be pushed
    */
    public void pushUnitButton(int buttonNumber) {
        unitButton[buttonNumber].push();
    }
    
    /**
    * Updates the buttons depending on the currently selected unit. Buttons may
    * disappear or become enabled or disabled.
    */
    public void updateButtons() {
        ImageProvider imageProvider = freeColClient.getGUI().getImageLibrary();    
        Unit selectedOne = freeColClient.getGUI().getActiveUnit();
        if(selectedOne == null) {
            for (int t=0; t<NUMBER_OF_BUTTONS; t++) {
                unitButton[t].setEnabled(false);
            }
            return;
        }

        int unitType = selectedOne.getType();
        
        /* Wait
        *  All units can wait
        */
        if (true) {
            unitButton[0].setEnabled(true);
        }
        
        /* Done
        *  All units can be done
        */
        if (true) {
            unitButton[1].setEnabled(true);
        }
        
        /* Fortify
        *  All units can fortify
        */
        if (true) {
            unitButton[2].setEnabled(true);
        }
        
        /* Sentry
        *  All units can sentry
        */
        if (true) {
            unitButton[3].setEnabled(true);
        }
        
        /* Clear Forest / Plow Fields
        *  Only colonists can do this, only if they have at least 20 tools, and only if they are
        *  in a square that can be improved
        */
        if (selectedOne.getTile() != null) {
            Tile tile = selectedOne.getTile();
            if(tile.isLand() && tile.isForested()) {
                unitButton[4].initialize(UnitButton.UNIT_BUTTON_CLEAR, imageProvider);
                unitButton[4].setEnabled(selectedOne.isPioneer());
            } else if (tile.isLand() && !tile.isForested() && !tile.isPlowed()) {
                unitButton[4].initialize(UnitButton.UNIT_BUTTON_PLOW, imageProvider);
                unitButton[4].setEnabled(selectedOne.isPioneer());
            } else if (tile.isLand() && !tile.isForested() && tile.isPlowed()) {
                unitButton[4].initialize(UnitButton.UNIT_BUTTON_PLOW, imageProvider);
                unitButton[4].setEnabled(false);
            } else {
                unitButton[4].initialize(UnitButton.UNIT_BUTTON_CLEAR, imageProvider);
                unitButton[4].setEnabled(false);
            }
        } else {
            unitButton[4].initialize(UnitButton.UNIT_BUTTON_CLEAR, imageProvider);
            unitButton[4].setEnabled(false);
        }
        
        /* Build roads
        *  Only colonists can do this, only if they have at least 20 tools, and only if they are
        *  in a land square that does not already have roads
        */
        if (unitType >= 0 && unitType <= 28 && selectedOne.getNumberOfTools() >= 20 && selectedOne.getTile() != null) {
            Tile tile = selectedOne.getTile();
            if(tile.isLand() && !tile.hasRoad()) {
                unitButton[5].setEnabled(true);
            } else {
                unitButton[5].setEnabled(false);
            }
        } else {
            unitButton[5].setEnabled(false);
        }
        
        /* Build a new colony
        *  Only colonists can do this, and only if they are on a 'colonizeable' tile
        */
        if (unitType >= 0 && unitType <= 28 && selectedOne.getTile() != null) {
            Tile tile = selectedOne.getTile();
            if(tile.isColonizeable()) {
                unitButton[6].setEnabled(true); 
            } else {
                unitButton[6].setEnabled(false);
            }
        } else {
            unitButton[6].setEnabled(false);
        }
        
        /* Disband
        *  Any unit can do this
        */
        if (true) {
            unitButton[7].setEnabled(true);
        }
    }
}
