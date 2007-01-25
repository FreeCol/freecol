package net.sf.freecol.client.gui;

import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Position;

/**
 * This class controls the type of view currently being used
 */
public class ViewMode {
    public static final int MOVE_UNITS_MODE = 0;
    public static final int VIEW_TERRAIN_MODE = 1;
    
    private static final Logger logger = Logger.getLogger(GUI.class.getName());
    
    private int currentMode;
    private Unit savedActiveUnit;
    private GUI gui;
    
    public ViewMode(GUI gui){
        this.gui = gui;
    }
    
    public void toggleViewMode(){
        logger.warning("Changing view");
        changeViewMode(1-currentMode);
    }
    
    public void changeViewMode(int newViewMode){
        
        if(newViewMode == currentMode){
            logger.warning("Trying to change to the same view mode");
            return;
        }
        
        currentMode = newViewMode;

        switch(currentMode){
            case ViewMode.MOVE_UNITS_MODE:
                if(gui.getActiveUnit() == null){
                    gui.setActiveUnit(savedActiveUnit);
                }
                savedActiveUnit = null;
                logger.warning("Change view to Move Units Mode");
                break;
            case ViewMode.VIEW_TERRAIN_MODE:
                savedActiveUnit = gui.getActiveUnit();
                gui.setActiveUnit(null);
                logger.warning("Change view to View Terrain Mode");
                break;
        }
    }
    
    public int getView(){
        return currentMode;
    }
    
    public boolean displayTileCursor(Tile tile, int canvasX, int canvasY){
        if(currentMode == ViewMode.VIEW_TERRAIN_MODE){
            
            Position selectedTilePos = gui.getSelectedTile();
            if (selectedTilePos == null) return false;
            
            if(selectedTilePos.getX() == tile.getX() && selectedTilePos.getY() == tile.getY()){
                Cursor cursor = gui.getCursor();
                cursor.setTile(tile);
                cursor.setCanvasPos(canvasX,canvasY);
                return true;
            }
        }
        
        return false;
    }
    
    public boolean displayUnitCursor(Unit unit, int canvasX, int canvasY){
      if( currentMode == ViewMode.MOVE_UNITS_MODE){
          
          Cursor cursor = gui.getCursor();
          
          if( (unit == gui.getActiveUnit()) && (cursor.isActive() || (unit.getMovesLeft() == 0)) ) {
              cursor.setTile(unit.getTile());
              cursor.setCanvasPos(canvasX,canvasY);
              return true;
          }
      }
      return false;
    }
}
