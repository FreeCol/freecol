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


package net.sf.freecol.client.control;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.CanvasMapEditorMouseListener;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.MapEditorMenuBar;
import net.sf.freecol.client.gui.action.MapControlsAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel.MapTransform;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.resources.ResourceMapping;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.generator.IMapGenerator;
import net.sf.freecol.server.generator.MapGenerator;
import net.sf.freecol.server.generator.MapGeneratorOptions;


/**
* The map editor controller.
*/
public final class MapEditorController {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapEditorController.class.getName());


    /**
     * The main controller.
     */
    private final FreeColClient freeColClient;
    
    /**
     * The transform that should be applied to a
     * <code>Tile</code> that is clicked on the map.
     */
    private MapTransform currentMapTransform = null;


    /**
     * Creates a new <code>MapEditorController</code>.
     * @param freeColClient The main controller.
     */
    public MapEditorController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
    }


    /**
     * Enters map editor modus.
     */
    public void startMapEditor() {
        try {
            freeColClient.setMapEditor(true);
            final FreeColServer freeColServer = new FreeColServer(false, false, 0, null);
            freeColClient.setFreeColServer(freeColServer);            
            freeColClient.setGame(freeColServer.getGame());
            freeColClient.setMyPlayer(null);

            final Canvas canvas = freeColClient.getCanvas();
            final GUI gui = freeColClient.getGUI();

            canvas.closeMainPanel();
            canvas.closeMenus();            
            gui.setInGame(true);
            
            // We may need to reset the zoom value to the default value
            gui.scaleMap(2f);
            
            freeColClient.getFrame().setJMenuBar(new MapEditorMenuBar(freeColClient));
            JInternalFrame f = freeColClient.getCanvas().addAsToolBox(new MapEditorTransformPanel(canvas));
            f.setLocation(f.getX(), 50);
            
            canvas.repaint();            
            CanvasMapEditorMouseListener listener = new CanvasMapEditorMouseListener(canvas, gui);
            canvas.addMouseListener(listener);
            canvas.addMouseMotionListener(listener);
        } catch (NoRouteToServerException e) {
            freeColClient.getCanvas().errorMessage("server.noRouteToServer");
            return;
        } catch (IOException e) {
            freeColClient.getCanvas().errorMessage("server.couldNotStart");
            return;
        }
    }
    
    /**
     * Sets the currently chosen <code>MapTransform</code>.
     * @param mt The transform that should be applied to a
     *      <code>Tile</code> that is clicked on the map.
     */
    public void setMapTransform(MapTransform mt) {
        currentMapTransform = mt;
        MapControlsAction mca = (MapControlsAction) freeColClient.getActionManager().getFreeColAction(MapControlsAction.id);
        if (mca.getMapControls() != null) {
            mca.getMapControls().update(mt);
        }
    }
    
    /**
     * Gets the current <code>MapTransform</code>.
     * @return The transform that should be applied to a
     *      <code>Tile</code> that is clicked on the map.
     */
    public MapTransform getMapTransform() {
        return currentMapTransform;
    }
    
    /**
     * Transforms the given <code>Tile</code> using the
     * {@link #getMapTransform() current <code>MapTransform</code>}.
     * 
     * @param t The <code>Tile</code> to be modified.
     */
    public void transform(Tile t) {
        if (currentMapTransform != null) {
            currentMapTransform.transform(t);
        }
    }
    
    /**
     * Creates a new map using a <code>MapGenerator</code>. A panel
     * with the <code>MapGeneratorOptions</code> is first displayed.
     * 
     * @see MapGenerator
     * @see MapGeneratorOptions
     */
    public void newMap() {
        final Canvas canvas = freeColClient.getCanvas();
        final Game game = freeColClient.getGame();
        final IMapGenerator mapGenerator = freeColClient.getFreeColServer().getMapGenerator();        
        
        boolean ok = canvas.showMapGeneratorOptionsDialog(true, mapGenerator.getMapGeneratorOptions());
        if (!ok) {
            return;
        }
        
        try {
            game.setMapGeneratorOptions(mapGenerator.getMapGeneratorOptions());
            mapGenerator.createMap(game);
            freeColClient.getGUI().setFocus(1, 1);
            freeColClient.getActionManager().update();
            canvas.refresh();
        } catch (FreeColException e) {
            canvas.closeMenus();
            canvas.errorMessage( e.getMessage() );
        }
    }
    
    /**
     * Opens a dialog where the user should specify the filename
     * and saves the game.
     */
    public void saveGame() {
        final Canvas canvas = freeColClient.getCanvas();
        String fileName = "my_map.fsg";
        final File file = canvas.showSaveDialog(FreeCol.getSaveDirectory(), fileName);
        if (file != null) {
            saveGame(file);
        }
    }
    
    /**
     * Saves the game to the given file.
     * @param file The <code>File</code>.
     */
    public void saveGame(final File file) {
        final Canvas canvas = freeColClient.getCanvas();

        canvas.showStatusPanel(Messages.message("status.savingGame"));
        Thread t = new Thread(FreeCol.CLIENT_THREAD+"Saving Map") {
            public void run() {
                try {
                    freeColClient.getFreeColServer().saveGame(file, "mapEditor");
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            canvas.closeStatusPanel();
                            canvas.requestFocusInWindow();
                        }
                    });
                } catch (IOException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            canvas.errorMessage("couldNotSaveGame");
                        }
                    });
                }
            }
        };
        t.start();
    }

    /**
     * Opens a dialog where the user should specify the filename and loads the
     * game.
     */
    public void loadGame() {
        Canvas canvas = freeColClient.getCanvas();

        File file = canvas.showLoadDialog(FreeCol.getSaveDirectory());

        if (file == null) {
            return;
        }

        if (!file.isFile()) {
            canvas.errorMessage("fileNotFound");
            return;
        }

        loadGame(file);
    }
    
    /**
     * Loads a game from the given file.
     * @param file The <code>File</code>.
     */
    public void loadGame(File file) {
        final Canvas canvas = freeColClient.getCanvas();
        final File theFile = file;

        freeColClient.setMapEditor(true);
        
        class ErrorJob implements Runnable {
            private final  String  message;
            ErrorJob( String message ) {
                this.message = message;
            }
            public void run() {
                canvas.closeMenus();
                canvas.errorMessage( message );
            }
        }

        canvas.showStatusPanel(Messages.message("status.loadingGame"));
        
        Runnable loadGameJob = new Runnable() {
            public void run() {
                FreeColServer freeColServer = null;
                try {                    
                    freeColServer = new FreeColServer(new FreeColSavegameFile(theFile), false, false, 0, "MapEditor");
                    freeColClient.setFreeColServer(freeColServer);
                    freeColClient.setGame(freeColServer.getGame());
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {               
                            canvas.closeStatusPanel();
                            freeColClient.getGUI().setFocus(1, 1);
                            freeColClient.getActionManager().update();
                            canvas.refresh();
                        }
                    } );                    
                } catch (NoRouteToServerException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            freeColClient.getCanvas().closeMainPanel();
                            freeColClient.getCanvas().showMainPanel();
                        }
                    });
                    SwingUtilities.invokeLater( new ErrorJob("server.noRouteToServer") );
                } catch (FileNotFoundException e) {                    
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            freeColClient.getCanvas().closeMainPanel();
                            freeColClient.getCanvas().showMainPanel();
                        }
                    });
                    SwingUtilities.invokeLater( new ErrorJob("fileNotFound") );
                } catch (IOException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            freeColClient.getCanvas().closeMainPanel();
                            freeColClient.getCanvas().showMainPanel();
                        }
                    });
                    SwingUtilities.invokeLater( new ErrorJob("server.couldNotStart") );
                } catch (FreeColException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            freeColClient.getCanvas().closeMainPanel();
                            freeColClient.getCanvas().showMainPanel();
                        }
                    });
                    SwingUtilities.invokeLater( new ErrorJob(e.getMessage()) );                    
                }
            }
        };
        freeColClient.worker.schedule( loadGameJob );        
    }
}
