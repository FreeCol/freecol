
package net.sf.freecol.client.gui.panel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ViewMode;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.gui.action.BuildColonyAction;
// import net.sf.freecol.client.gui.action.BuildRoadAction;
import net.sf.freecol.client.gui.action.DisbandUnitAction;
import net.sf.freecol.client.gui.action.FortifyAction;
// Replaced BuildRoad and Plow Actions with ImprovementAction
import net.sf.freecol.client.gui.action.ImprovementActionType;
// import net.sf.freecol.client.gui.action.PlowAction;
import net.sf.freecol.client.gui.action.SentryAction;
import net.sf.freecol.client.gui.action.SkipUnitAction;
import net.sf.freecol.client.gui.action.WaitAction;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel.MapTransform;
import net.sf.freecol.common.model.Tile;


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
    
    private final FreeColClient freeColClient;

    private final InfoPanel        infoPanel;
    private final MiniMap          miniMap;
    private final UnitButton[]     unitButton;   
    private final JLabel compassRose;
    @SuppressWarnings("unused")
    private final GUI                    gui;




    /**
    * The basic constructor.
    * @param freeColClient The main controller object for the client
    * @param gui An object that contains useful GUI-related methods.
    */
    public MapControls(final FreeColClient freeColClient, final GUI gui) {
        this.freeColClient = freeColClient;
        this.gui = gui;

        //
        // Create GUI Objects
        //

        infoPanel = new InfoPanel(freeColClient, freeColClient.getGame(), freeColClient.getImageLibrary());
        miniMap = new MiniMap(freeColClient);
        compassRose = new JLabel(freeColClient.getGUI().getImageLibrary().getMiscImageIcon(9));
        
        final ActionManager am = freeColClient.getActionManager();
        
        List<UnitButton> ubList = new ArrayList<UnitButton>();
        ubList.add(new UnitButton(am.getFreeColAction(WaitAction.ID)));
        ubList.add(new UnitButton(am.getFreeColAction(SkipUnitAction.ID)));
        ubList.add(new UnitButton(am.getFreeColAction(SentryAction.ID)));
        ubList.add(new UnitButton(am.getFreeColAction(FortifyAction.ID)));
        for (ImprovementActionType iaType : FreeCol.getSpecification().getImprovementActionTypeList()) {
            ubList.add(new UnitButton(am.getFreeColAction(iaType.getID())));
        }
        ubList.add(new UnitButton(am.getFreeColAction(BuildColonyAction.ID)));
        ubList.add(new UnitButton(am.getFreeColAction(DisbandUnitAction.ID)));
        unitButton = (ubList.toArray(new UnitButton[0]));
        /*  Depreciated
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
        */
        
        //
        // Don't allow them to gain focus
        //

        infoPanel.setFocusable(false);
        miniMap.setFocusable(false);
        compassRose.setFocusable(false);

        for(int i=0; i<unitButton.length; i++) {
            unitButton[i].setFocusable(false);
        }

        compassRose.setSize(compassRose.getPreferredSize());
        compassRose.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    int x = e.getX() - compassRose.getWidth()/2;
                    int y = e.getY() - compassRose.getHeight()/2;
                    double theta = Math.atan2(y, x) + Math.PI/2 + Math.PI/8;
                    if (theta < 0) {
                        theta += 2*Math.PI;
                    }
                    int direction = (int) Math.floor(theta / (Math.PI/4));
                    freeColClient.getInGameController().moveActiveUnit(direction);
                }
            });

    }

    /**
     * Updates this <code>InfoPanel</code>.
     * 
     * @param mapTransform The current MapTransform.
     */
    public void update(MapTransform mapTransform) {
        if (infoPanel != null) {
            infoPanel.update(mapTransform);
        }
    }

    /**
    * Adds the map controls to the given component.
    * @param component The component to add the map controls to.
    */
    public void addToComponent(Canvas component) {
        if (freeColClient.getGame() == null
                || freeColClient.getGame().getMap() == null) {
            return;
        }
        
        //
        // Relocate GUI Objects
        //

        infoPanel.setLocation(component.getWidth() - infoPanel.getWidth(), component.getHeight() - infoPanel.getHeight());
        miniMap.setLocation(0, component.getHeight() - miniMap.getHeight());
        compassRose.setLocation(component.getWidth() - compassRose.getWidth() - 20,
                                component.getMenuBarHeight() + 20);
        
        final int SPACE = unitButton[0].getWidth() + 5;
        for(int i=0; i<unitButton.length; i++) {            
            unitButton[i].setLocation(miniMap.getWidth() +
                                      (infoPanel.getX() - miniMap.getWidth() - 
                                       unitButton.length*SPACE)/2 +
                                      i*SPACE,
                                      component.getHeight() - 40);
        }

        //
        // Add the GUI Objects to the container
        //
        component.add(infoPanel, JLayeredPane.DEFAULT_LAYER, false);
        component.add(miniMap, JLayeredPane.DEFAULT_LAYER, false);
        if (freeColClient.getClientOptions().getBoolean(ClientOptions.DISPLAY_COMPASS_ROSE)) {
            component.add(compassRose, JLayeredPane.DEFAULT_LAYER, false);
        }

        if (!freeColClient.isMapEditor()) {
            for(int i=0; i<unitButton.length; i++) {
                component.add(unitButton[i], JLayeredPane.DEFAULT_LAYER, false);
                Action a = unitButton[i].getAction();
                unitButton[i].setAction(null);
                unitButton[i].setAction(a);
            }
        }
    }


    /**
     * Removes the map controls from the parent component.
     */
    public void removeFromComponent(Canvas canvas) {
        canvas.remove(infoPanel, false);
        canvas.remove(miniMap, false);
        canvas.remove(compassRose, false);

        for(int i=0; i<unitButton.length; i++) {
            canvas.remove(unitButton[i], false);
        }
    }


    public boolean isShowing() {
        return infoPanel.getParent() != null;
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
        GUI gui = freeColClient.getGUI();
        int viewMode = gui.getViewMode().getView();
        switch (viewMode) {
            case ViewMode.MOVE_UNITS_MODE:
                infoPanel.update(gui.getActiveUnit());
                break;
            case ViewMode.VIEW_TERRAIN_MODE:
                if (gui.getSelectedTile() != null) {
                    Tile selectedTile = freeColClient.getGame().getMap().getTile(gui.getSelectedTile());
                    if (infoPanel.getTile() != selectedTile) {
                        infoPanel.update(selectedTile);
                    }
                }
                break;
        }
    }
}
