/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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
import net.sf.freecol.client.gui.MapViewer;
import net.sf.freecol.client.gui.action.MapControlsAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel.MapTransform;
import net.sf.freecol.client.gui.panel.MapGeneratorOptionsDialog;
import net.sf.freecol.client.gui.panel.MiniMap;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.FreeColServer;
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


    private GUI gui;


    /**
     * Creates a new <code>MapEditorController</code>.
     * @param freeColClient The main controller.
     */
    public MapEditorController(FreeColClient freeColClient, GUI gui) {
        this.freeColClient = freeColClient;
        this.gui = gui;
    }


    /**
     * Enters map editor modus.
     */
    public void startMapEditor() {

        // TODO: fixme! Specification must be known in advance
        final String tc = "freecol";

        try {
            FreeColTcFile tcData = new FreeColTcFile(tc);
            Specification specification = tcData.getSpecification();
            freeColClient.setMapEditor(true);
            final FreeColServer freeColServer = new FreeColServer(specification, false, false, 0, null);
            freeColClient.setFreeColServer(freeColServer);
            freeColClient.setGame(freeColServer.getGame());
            freeColClient.setMyPlayer(null);
            gui.playSound(null);

            final Canvas canvas = gui.getCanvas();
            final MapViewer mapViewer = gui.getMapViewer();

            canvas.closeMainPanel();
            canvas.closeMenus();
            freeColClient.setInGame(true);

            // We may need to reset the zoom value to the default value
            mapViewer.scaleMap(2f);
            
            gui.setupMapEditorMenuBar();
            JInternalFrame f = gui.getCanvas().addAsToolBox(new MapEditorTransformPanel(canvas));
            f.setLocation(f.getX(), 50);

            canvas.repaint();
            CanvasMapEditorMouseListener listener = new CanvasMapEditorMouseListener(canvas, mapViewer);
            canvas.addMouseListener(listener);
            canvas.addMouseMotionListener(listener);
        } catch (NoRouteToServerException e) {
            gui.errorMessage("server.noRouteToServer");
            return;
        } catch (IOException e) {
            gui.errorMessage("server.couldNotStart");
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
        MapControlsAction mca = (MapControlsAction) freeColClient.getActionManager()
                                .getFreeColAction(MapControlsAction.id);
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
        final Canvas canvas = gui.getCanvas();
        final Game game = freeColClient.getGame();
        final MapGenerator mapGenerator = freeColClient.getFreeColServer().getMapGenerator();

        OptionGroup group = freeColClient.getGame().getMapGeneratorOptions();
        group = canvas.showFreeColDialog(new MapGeneratorOptionsDialog(gui, canvas, group, true, true));
        if (group == null) {
            return;
        }

        try {
            if (game.getSpecification().getDifficultyLevel() == null) {
                game.getSpecification().applyDifficultyLevel("model.difficulty.medium");
            }
            mapGenerator.createMap(game);
            gui.getMapViewer().setFocus(game.getMap().getTile(1,1));
            freeColClient.getActionManager().update();
            canvas.refresh();
        } catch (FreeColException e) {
            canvas.closeMenus();
            gui.errorMessage( e.getMessage() );
        }
    }

    /**
     * Opens a dialog where the user should specify the filename
     * and saves the game.
     */
    public void saveGame() {
        final Canvas canvas = gui.getCanvas();
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
        final Canvas canvas = gui.getCanvas();

        canvas.showStatusPanel(Messages.message("status.savingGame"));
        Thread t = new Thread(FreeCol.CLIENT_THREAD+"Saving Map") {
            @Override
            public void run() {
                try {
                    // create thumbnail
                    MiniMap miniMap = new MiniMap(freeColClient, gui);
                    miniMap.setTileSize(MiniMap.MAX_TILE_SIZE);
                    int width = freeColClient.getGame().getMap().getWidth()
                        * MiniMap.MAX_TILE_SIZE + MiniMap.MAX_TILE_SIZE/2;
                    int height = freeColClient.getGame().getMap().getHeight()
                        * MiniMap.MAX_TILE_SIZE / 4;
                    BufferedImage image = new BufferedImage(width, height,
                                                            BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = image.createGraphics();
                    miniMap.paintMap(g2d, width, height);

                    // TODO: this can probably done more efficiently
                    // by applying a suitable AffineTransform to the
                    // Graphics2D
                    double scaledWidth = Math.min((64 * width) / height, 128);
                    BufferedImage scaledImage = new BufferedImage((int) scaledWidth, 64,
                                                                  BufferedImage.TYPE_INT_ARGB);
                    scaledImage.createGraphics().drawImage(image, 0, 0, (int) scaledWidth, 64, null);
                    freeColClient.getFreeColServer().saveGame(file, "mapEditor", null, scaledImage);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            canvas.closeStatusPanel();
                            canvas.requestFocusInWindow();
                        }
                    });
                } catch (IOException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            gui.errorMessage("couldNotSaveGame");
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
        Canvas canvas = gui.getCanvas();

        File file = canvas.showLoadDialog(FreeCol.getSaveDirectory());

        if (file == null) {
            return;
        }

        if (!file.isFile()) {
            gui.errorMessage("fileNotFound");
            return;
        }

        loadGame(file);
    }

    /**
     * Loads a game from the given file.
     * @param file The <code>File</code>.
     */
    public void loadGame(File file) {
        final Canvas canvas = gui.getCanvas();
        final File theFile = file;

        freeColClient.setMapEditor(true);

        class ErrorJob implements Runnable {
            private final  String  message;
            ErrorJob( String message ) {
                this.message = message;
            }
            public void run() {
                canvas.closeMenus();
                gui.errorMessage( message );
            }
        }

        canvas.showStatusPanel(Messages.message("status.loadingGame"));

        Runnable loadGameJob = new Runnable() {
            public void run() {
                FreeColServer freeColServer = null;
                try {
                    freeColServer = new FreeColServer(new FreeColSavegameFile(theFile), 0, "MapEditor");
                    freeColClient.setFreeColServer(freeColServer);
                    freeColClient.setGame(freeColServer.getGame());
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            canvas.closeStatusPanel();
                            gui.getMapViewer().setFocus(freeColClient.getGame().getMap().getTile(1,1));
                            freeColClient.getActionManager().update();
                            canvas.refresh();
                        }
                    } );
                } catch (NoRouteToServerException e) {
                    reloadMainPanel();
                    SwingUtilities.invokeLater( new ErrorJob("server.noRouteToServer") );
                } catch (FileNotFoundException e) {
                    reloadMainPanel();
                    SwingUtilities.invokeLater( new ErrorJob("fileNotFound") );
                } catch (IOException e) {
                    reloadMainPanel();
                    SwingUtilities.invokeLater( new ErrorJob("server.couldNotStart") );
                } catch (FreeColException e) {
                    reloadMainPanel();
                    SwingUtilities.invokeLater( new ErrorJob(e.getMessage()) );
                }
            }
        };
        freeColClient.worker.schedule( loadGameJob );
    }

    private void reloadMainPanel ()
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.getCanvas().closeMainPanel();
                gui.getCanvas().showMainPanel();
                gui.playSound("sound.intro.general");
            }
        });
    }
}
