
package net.sf.freecol.client.gui.panel;


import javax.swing.JButton;
import javax.swing.JComponent;

import net.sf.freecol.common.model.Unit;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.FreeColClient;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
* A button with a set of images which is used to give commands
* to a unit with the mouse instead of the keyboard. The UnitButton
* has rollover highlighting, can be grayed out if it is unusable,
* and will use a separate image for being pressed.
* The UnitButton is useless by itself, this object needs to
* be placed on a JComponent in order to be useable.
*/
public final class UnitButton extends JButton {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    public static final int EUROPE = 2;
    
    private JComponent             container;
    //private UserInputHandler       userInputHandler;
    private int                    buttonType;
    private GUI                    gui;
    private FreeColClient freeColClient;
    
    public static final int UNIT_BUTTON_WAIT = 0,
                            UNIT_BUTTON_DONE = 1,
                            UNIT_BUTTON_FORTIFY = 2,
                            UNIT_BUTTON_SENTRY = 3,
                            UNIT_BUTTON_CLEAR = 4,
                            UNIT_BUTTON_PLOW = 5,
                            UNIT_BUTTON_ROAD = 6,
                            UNIT_BUTTON_BUILD = 7,
                            UNIT_BUTTON_DISBAND = 8,
                            UNIT_BUTTON_COUNT = 9;

    
    
    /**
    * The basic constructor
    */
    public UnitButton(FreeColClient freeColClient, GUI gui) {
      this.gui = gui;
      this.freeColClient = freeColClient;
    }
        
    
    /**
    * A constructor which initializes the container
    * @param container The JComponent that contains this button
    */
    public UnitButton(JComponent container, FreeColClient freeColClient, GUI gui) {
        this.container = container;
        this.gui = gui;
        this.freeColClient = freeColClient;
    }
    

    
    

    /**
    * Sets various attributes about the button, as well as the picture set
    * @param index The picture set to use (see definitions in ImageLibrary.java)
    * @param imageProvider The ImageProvider used to retrive the Images
    */
    public void initialize(int index, ImageProvider imageProvider) {
        buttonType = index;
        setSize(30, 30);
        setRolloverEnabled(true);
        setIcon(imageProvider.getUnitButtonImageIcon(index, 0));
        setRolloverIcon(imageProvider.getUnitButtonImageIcon(index, 1));
        setPressedIcon(imageProvider.getUnitButtonImageIcon(index, 2));
        setDisabledIcon(imageProvider.getUnitButtonImageIcon(index, 3));
        setToolTipText(Messages.message("unit.state." + index));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                push();
            }
        });
    }


    /**
    * Sets the container
    * @param container The JComponent to set as the container
    */
    public void setContainer(JComponent container) {
        this.container = container;
    }
    

    /**
    * A function that removes this button from its container
    */
    public void removeFromContainer() {
        container.remove(this);
    }
    

    /**
    * Gets called when this button becomes pressed
    * The button will determine what it is supposed to do
    */
    public void push() {
        switch(buttonType) {
            case UNIT_BUTTON_WAIT:
                if (gui.getActiveUnit() != null) {
                    freeColClient.getInGameController().nextActiveUnit();
                }
                break;
            
            case UNIT_BUTTON_DONE:
                if (gui.getActiveUnit() != null) {
                    freeColClient.getInGameController().skipActiveUnit();
                }
                break;
                
            case UNIT_BUTTON_FORTIFY:
                if (gui.getActiveUnit() != null) {
                    freeColClient.getInGameController().changeState(gui.getActiveUnit(), Unit.FORTIFY);
                }
                break;
                
            case UNIT_BUTTON_SENTRY:
                if (gui.getActiveUnit() != null) {
                    freeColClient.getInGameController().changeState(gui.getActiveUnit(), Unit.SENTRY);
                }
                break;
            
            case UNIT_BUTTON_CLEAR:
                if (gui.getActiveUnit() != null) {
                    freeColClient.getInGameController().changeState(gui.getActiveUnit(), Unit.PLOW);
                }
                break;
            
            case UNIT_BUTTON_PLOW:
                if (gui.getActiveUnit() != null) {
                    freeColClient.getInGameController().changeState(gui.getActiveUnit(), Unit.PLOW);
                }
                break;
            
            case UNIT_BUTTON_ROAD:
                if (gui.getActiveUnit() != null) {
                    freeColClient.getInGameController().changeState(gui.getActiveUnit(), Unit.BUILD_ROAD);
                }
                break;
            
            case UNIT_BUTTON_BUILD:
                if (gui.getActiveUnit() != null) {
                    freeColClient.getInGameController().buildColony();
                }
                break;
            
            case UNIT_BUTTON_DISBAND:
                break;
            
            default:
                break;
        }
    }
}
