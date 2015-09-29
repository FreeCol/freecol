/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.generator.MapGenerator;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The map editor controller.
 */
public final class MapEditorController {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapEditorController.class.getName());


    private final FreeColClient freeColClient;

    private final GUI gui;

    public interface IMapTransform {

        /**
         * Applies this transformation to the given tile.
         * @param t The <code>Tile</code> to be transformed,
         */
        public abstract void transform(Tile t);

    }

    /**
     * The transform that should be applied to a <code>Tile</code>
     * that is clicked on the map.
     */
    private IMapTransform currentMapTransform = null;


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
     * FIXME: The TC and difficulty level can now be set at the
     * command line, but we should do better.
     */
    public void startMapEditor() {
        try {
            Specification specification = getDefaultSpecification();
            freeColClient.setMapEditor(true);
            final FreeColServer freeColServer
                = new FreeColServer(false, false, specification, 0, null);
            freeColClient.setFreeColServer(freeColServer);
            Game game = freeColServer.getGame();
            requireNativeNations(game);
            freeColClient.setGame(game);
            freeColClient.setMyPlayer(null);
            freeColClient.getSoundController().playSound(null);

            gui.closeMainPanel();
            gui.closeMenus();
            freeColClient.setInGame(true);
            gui.changeViewMode(GUI.VIEW_TERRAIN_MODE);
            gui.startMapEditorGUI();
        } catch (IOException e) {
            gui.showErrorMessage("server.initialize");
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
        return FreeCol.loadSpecification(FreeCol.getTCFile(), 
            FreeCol.getAdvantages(), FreeCol.getDifficulty());
    }
        
    /**
     * Sets the currently chosen <code>MapTransform</code>.
     * @param mt The transform that should be applied to a
     *      <code>Tile</code> that is clicked on the map.
     */
    public void setMapTransform(IMapTransform mt) {
        currentMapTransform = mt;
        gui.updateMapControls();
    }

    /**
     * Gets the current <code>MapTransform</code>.
     * @return The transform that should be applied to a
     *      <code>Tile</code> that is clicked on the map.
     */
    public IMapTransform getMapTransform() {
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

        gui.removeInGameComponents();
        OptionGroup mgo = gui.showMapGeneratorOptionsDialog(true);
        if (mgo == null) return;
        game.setMapGeneratorOptions(mgo);
        Map map = freeColClient.getFreeColServer().getMapGenerator()
            .createMap(new LogBuilder(-1));
        requireNativeNations(game);
        gui.setFocus(game.getMap().getTile(1,1));
        gui.updateMenuBar();
        gui.refresh();
    }

    /**
     * Opens a dialog where the user should specify the filename
     * and saves the game.
     */
    public void saveGame() {
        File file = gui.showSaveDialog(FreeColDirectories.getSaveDirectory(),
                                       FreeColDirectories.MAP_FILE_NAME);
        if (file != null) saveGame(file);
    }

    /**
     * Saves the game to the given file.
     *
     * @param file The <code>File</code>.
     */
    public void saveGame(final File file) {
        final Game game = freeColClient.getGame();
        Map map = game.getMap();
        map.resetContiguity();
        map.resetHighSeasCount();
        map.resetLayers();

        gui.showStatusPanel(Messages.message("status.savingGame"));
        Thread t = new Thread(FreeCol.CLIENT_THREAD+"Saving Map") {
                @Override
                public void run() {
                    try {
                        BufferedImage thumb = gui.createMiniMapThumbNail();
                        freeColClient.getFreeColServer()
                            .saveMapEditorGame(file, thumb);
                        SwingUtilities.invokeLater(() -> {
                                gui.closeStatusPanel();
                                gui.requestFocusInWindow();
                            });
                    } catch (IOException e) {
                        SwingUtilities.invokeLater(() -> {
                                gui.showErrorMessage(FreeCol.badSave(file));
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
        File file = gui.showLoadSaveFileDialog();
        if (file != null) loadGame(file);
    }

    /**
     * Require all native nation players to be present in a game.
     *
     * @param game The <code>Game</code> to add native nations to.
     */
    public void requireNativeNations(Game game) {
        final Specification spec = game.getSpecification();
        for (Nation n : spec.getIndianNations()) {
            Player p = game.getPlayerByNation(n);
            if (p == null) {
                p = new ServerPlayer(game, false, n, null, null);
                game.addPlayer(p);
            }
        }
    }

    /**
     * Loads a game from the given file.
     *
     * @param file The <code>File</code>.
     */
    public void loadGame(File file) {
        final File theFile = file;

        freeColClient.setMapEditor(true);

        class ErrorJob implements Runnable {
            private final StringTemplate template;

            ErrorJob(StringTemplate template) {
                this.template = template;
            }

            @Override
            public void run() {
                gui.closeMenus();
                gui.showErrorMessage(template);
            }
        }

        gui.showStatusPanel(Messages.message("status.loadingGame"));

        Runnable loadGameJob = () -> {
            FreeColServer freeColServer = freeColClient.getFreeColServer();
            try {
                Specification spec = getDefaultSpecification();
                Game game = FreeColServer.readGame(new FreeColSavegameFile(theFile),
                                                   spec, freeColServer);
                freeColClient.setGame(game);
                requireNativeNations(game);
                SwingUtilities.invokeLater(() -> {
                        gui.closeStatusPanel();
                        gui.setFocus(freeColClient.getGame().getMap().getTile(1,1));
                        gui.updateMenuBar();
                        gui.refresh();
                    });
            } catch (FreeColException e) {
                reloadMainPanel();
                SwingUtilities.invokeLater(new ErrorJob(StringTemplate.name(e.getMessage())));
            } catch (FileNotFoundException e) {
                reloadMainPanel();
                SwingUtilities.invokeLater(new ErrorJob(StringTemplate.key("server.fileNotFound")));
            } catch (IOException e) {
                reloadMainPanel();
                SwingUtilities.invokeLater(new ErrorJob(StringTemplate.key("server.initialize")));
            } catch (XMLStreamException e) {
                reloadMainPanel();
                SwingUtilities.invokeLater(new ErrorJob(FreeCol.badLoad(theFile)));
            }
        };
        freeColClient.setWork(loadGameJob);
    }

    private void reloadMainPanel () {
        SwingUtilities.invokeLater(() -> {
                gui.closeMainPanel();
                gui.showMainPanel(null);
                freeColClient.getSoundController()
                    .playSound("sound.intro.general");
            });
    }
}
