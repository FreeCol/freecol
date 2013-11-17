/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel.MapTransform;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.generator.MapGenerator;


/**
 * The map editor controller.
 */
public final class MapEditorController {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapEditorController.class.getName());


    private final FreeColClient freeColClient;

    private final GUI gui;

    /**
     * The transform that should be applied to a
     * <code>Tile</code> that is clicked on the map.
     */
    private MapTransform currentMapTransform = null;



    /**
     * Creates a new <code>MapEditorController</code>.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public MapEditorController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
        this.gui = freeColClient.getGUI();
    }


    /**
     * Enters map editor modus.
     *
     * TODO: The TC and difficulty level can now be set at the command line,
     * but we should do better.
     */
    public void startMapEditor() {
        try {
            Specification specification = getDefaultSpecification();
            freeColClient.setMapEditor(true);
            final FreeColServer freeColServer
                = new FreeColServer(false, false, specification, 0, null);
            freeColClient.setFreeColServer(freeColServer);
            freeColClient.setGame(freeColServer.getGame());
            freeColClient.setMyPlayer(null);
            gui.playSound(null);

            gui.closeMainPanel();
            gui.closeMenus();
            freeColClient.setInGame(true);

            gui.startMapEditorGUI();
        } catch (NoRouteToServerException e) {
            gui.errorMessage("server.noRouteToServer");
            return;
        } catch (IOException e) {
            gui.errorMessage("server.couldNotStart");
            return;
        }
    }

    /**
     * Get the default specification from the default TC.
     *
     * @return A <code>Specification</code> to use in the map editor.
     * @throws IOException on failure to find the spec.
     */
    public Specification getDefaultSpecification() throws IOException {
        final String tc = FreeCol.getTC();
        FreeColTcFile tcData = new FreeColTcFile(tc);
        Specification spec = tcData.getSpecification();
        spec.applyDifficultyLevel(FreeCol.getDifficulty());
        return spec;
    }
        
    /**
     * Sets the currently chosen <code>MapTransform</code>.
     * @param mt The transform that should be applied to a
     *      <code>Tile</code> that is clicked on the map.
     */
    public void setMapTransform(MapTransform mt) {
        currentMapTransform = mt;
        gui.updateMapControls();
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
        final Game game = freeColClient.getGame();
        final Specification spec = game.getSpecification();
        final MapGenerator mapGenerator = freeColClient.getFreeColServer()
            .getMapGenerator();

        OptionGroup group = spec.getMapGeneratorOptions();
        group = gui.showMapGeneratorOptionsDialog(group, true, true);
        if (group == null) return;

        try {
            if (spec.getDifficultyLevel() == null) {
                spec.applyDifficultyLevel(FreeCol.getDifficulty());
            }
            mapGenerator.createMap(game);
            gui.setFocus(game.getMap().getTile(1,1));
            freeColClient.updateActions();
            gui.refresh();
        } catch (FreeColException e) {
            gui.closeMenus();
            gui.errorMessage(e.getMessage());
        }
    }

    /**
     * Opens a dialog where the user should specify the filename
     * and saves the game.
     */
    public void saveGame() {
        String fileName = "my_map.fsg";
        final File file = gui.showSaveDialog(FreeColDirectories.getSaveDirectory(), fileName);
        if (file != null) {
            saveGame(file);
        }
    }

    /**
     * Saves the game to the given file.
     * @param file The <code>File</code>.
     */
    public void saveGame(final File file) {
        final Game game = freeColClient.getGame();
        game.getMap().resetContiguity();
        game.getMap().resetHighSeasCount();

        gui.showStatusPanel(Messages.message("status.savingGame"));
        Thread t = new Thread(FreeCol.CLIENT_THREAD+"Saving Map") {
            @Override
            public void run() {
                try {
                    BufferedImage scaledImage = gui.createMiniMapThumbNail();
                    freeColClient.getFreeColServer().saveGame(file, null, scaledImage);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            gui.closeStatusPanel();
                            gui.requestFocusInWindow();
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
        File file = gui.showLoadDialog(FreeColDirectories.getSaveDirectory());
        if (file == null) return;
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
        final File theFile = file;

        freeColClient.setMapEditor(true);

        class ErrorJob implements Runnable {
            private final String message;

            ErrorJob( String message ) {
                this.message = message;
            }

            public void run() {
                gui.closeMenus();
                gui.errorMessage(message);
            }
        }

        gui.showStatusPanel(Messages.message("status.loadingGame"));

        Runnable loadGameJob = new Runnable() {
            public void run() {
                FreeColServer freeColServer = null;
                try {
                    Specification spec = getDefaultSpecification();
                    freeColServer = new FreeColServer(new FreeColSavegameFile(theFile),
                        spec, 0, "MapEditor");
                    freeColClient.setFreeColServer(freeColServer);
                    freeColClient.setGame(freeColServer.getGame());
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            gui.closeStatusPanel();
                            gui.setFocus(freeColClient.getGame().getMap().getTile(1,1));
                            freeColClient.updateActions();
                            gui.refresh();
                        }
                    } );
                } catch (FreeColException e) {
                    reloadMainPanel();
                    SwingUtilities.invokeLater(new ErrorJob(e.getMessage()));
                } catch (FileNotFoundException e) {
                    reloadMainPanel();
                    SwingUtilities.invokeLater(new ErrorJob("fileNotFound"));
                } catch (IOException e) {
                    reloadMainPanel();
                    SwingUtilities.invokeLater(new ErrorJob("server.couldNotStart"));
                } catch (NoRouteToServerException e) {
                    reloadMainPanel();
                    SwingUtilities.invokeLater(new ErrorJob("server.noRouteToServer"));
                } catch (XMLStreamException e) {
                    reloadMainPanel();
                    SwingUtilities.invokeLater(new ErrorJob("server.streamError"));
                }
            }
        };
        freeColClient.worker.schedule( loadGameJob );
    }

    private void reloadMainPanel ()
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.closeMainPanel();
                gui.showMainPanel(null);
                gui.playSound("sound.intro.general");
            }
        });
    }
}
