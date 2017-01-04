/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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
import net.sf.freecol.client.control.FreeColClientHolder;
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
public final class MapEditorController extends FreeColClientHolder {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapEditorController.class.getName());

    public interface IMapTransform {

        /**
         * Applies this transformation to the given tile.
         * @param t The {@code Tile} to be transformed,
         */
        public abstract void transform(Tile t);
    }

    /**
     * The transform that should be applied to a {@code Tile}
     * that is clicked on the map.
     */
    private IMapTransform currentMapTransform = null;


    /**
     * Creates a new {@code MapEditorController}.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public MapEditorController(FreeColClient freeColClient) {
        super(freeColClient);
    }


    /**
     * Require all native nation players to be present in a game.
     *
     * @param game The {@code Game} to add native nations to.
     */
    private void requireNativeNations(Game game) {
        final Specification spec = game.getSpecification();
        for (Nation n : spec.getIndianNations()) {
            Player p = game.getPlayerByNation(n);
            if (p == null) {
                p = new ServerPlayer(game, false, n);
                game.addPlayer(p);
            }
        }
    }

    private void reloadMainPanel () {
        SwingUtilities.invokeLater(() -> {
                getGUI().closeMainPanel();
                getGUI().showMainPanel(null);
                getSoundController().playSound("sound.intro.general");
            });
    }


    /**
     * Enters map editor mode.
     *
     * FIXME: The TC and difficulty level can now be set at the
     * command line, but we should do better.
     */
    public void startMapEditor() {
        final FreeColClient fcc = getFreeColClient();
        try {
            Specification specification = getDefaultSpecification();
            fcc.setMapEditor(true);
            final FreeColServer freeColServer
                = new FreeColServer(false, false, specification, 0, null);
            fcc.setFreeColServer(freeColServer);
            Game game = freeColServer.getGame();
            requireNativeNations(game);
            fcc.setGame(game);
            fcc.setMyPlayer(null);
            getSoundController().playSound(null);

            getGUI().closeMainPanel();
            getGUI().closeMenus();
            //fcc.changeClientState(true);
            getGUI().changeViewMode(GUI.VIEW_TERRAIN_MODE);
            getGUI().startMapEditorGUI();
        } catch (IOException e) {
            getGUI().showErrorMessage(StringTemplate
                .template("server.initialize"));
            return;
        }
    }

    /**
     * Get the default specification from the default TC.
     *
     * @return A {@code Specification} to use in the map editor.
     * @throws IOException on failure to find the spec.
     */
    public Specification getDefaultSpecification() throws IOException {
        return FreeCol.loadSpecification(FreeCol.getTCFile(), 
            FreeCol.getAdvantages(), FreeCol.getDifficulty());
    }
        
    /**
     * Sets the currently chosen {@code MapTransform}.
     *
     * @param mt The transform that should be applied to a
     *     {@code Tile} that is clicked on the map.
     */
    public void setMapTransform(IMapTransform mt) {
        currentMapTransform = mt;
        getGUI().updateMapControls();
    }

    /**
     * Gets the current {@code MapTransform}.
     *
     * @return The transform that should be applied to a
     *     {@code Tile} that is clicked on the map.
     */
    public IMapTransform getMapTransform() {
        return currentMapTransform;
    }

    /**
     * Transforms the given {@code Tile} using the
     * {@link #getMapTransform()} current {@code MapTransform}.
     *
     * @param t The {@code Tile} to be modified.
     */
    public void transform(Tile t) {
        if (currentMapTransform != null) {
            currentMapTransform.transform(t);
        }
    }

    /**
     * Creates a new map using a {@code MapGenerator}. A panel
     * with the {@code MapGeneratorOptions} is first displayed.
     *
     * @see MapGenerator
     * @see MapGeneratorOptions
     */
    public void newMap() {
        final Game game = getGame();

        getGUI().removeInGameComponents();
        OptionGroup mgo = getGUI().showMapGeneratorOptionsDialog(true);
        if (mgo == null) return;
        game.setMapGeneratorOptions(mgo);
        getFreeColServer().getMapGenerator().createMap(new LogBuilder(-1));
        requireNativeNations(game);
        getGUI().setFocus(game.getMap().getTile(1,1));
        getGUI().updateMenuBar();
        getGUI().refresh();
    }

    /**
     * Opens a dialog where the user should specify the filename
     * and saves the game.
     */
    public void saveMapEditorGame() {
        File file = getGUI().showSaveDialog(FreeColDirectories.getSaveDirectory(),
                                            FreeColDirectories.MAP_FILE_NAME);
        if (file != null) saveMapEditorGame(file);
    }

    /**
     * Saves the game to the given file.
     *
     * @param file The {@code File}.
     */
    public void saveMapEditorGame(final File file) {
        final GUI gui = getGUI();
        final Game game = getGame();
        Map map = game.getMap();
        map.resetContiguity();
        map.resetHighSeasCount();
        map.resetLayers();

        gui.showStatusPanel(Messages.message("status.savingGame"));
        new Thread(FreeCol.CLIENT_THREAD + "-Saving-Map") {
            @Override
            public void run() {
                try {
                    BufferedImage thumb = gui.createMiniMapThumbNail();
                    getFreeColServer().saveMapEditorGame(file, thumb);
                    SwingUtilities.invokeLater(() -> {
                            gui.closeStatusPanel();
                            gui.requestFocusInWindow();
                        });
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                            gui.closeStatusPanel();
                            gui.showErrorMessage(FreeCol.badFile("error.couldNotSave", file),
                                (e == null) ? null : e.getMessage());
                        });
                }
            }
        }.start();
    }

    /**
     * Opens a dialog where the user should specify the filename and loads the
     * game.
     */
    public void loadGame() {
        File file = getGUI().showLoadSaveFileDialog();
        if (file != null) loadGame(file);
    }

    /**
     * Loads a game from the given file.
     *
     * @param file The {@code File}.
     */
    public void loadGame(File file) {
        final FreeColClient fcc = getFreeColClient();
        final GUI gui = getGUI();

        fcc.setMapEditor(true);
        gui.showStatusPanel(Messages.message("status.loadingGame"));

        final File theFile = file;
        new Thread(FreeCol.CLIENT_THREAD + "Loading-Map") {
            @Override
            public void run() {
                final FreeColServer freeColServer = getFreeColServer();
                GUI.ErrorJob ej = null;
                try {
                    Specification spec = getDefaultSpecification();
                    Game game = FreeColServer.readGame(new FreeColSavegameFile(theFile),
                                                       spec, freeColServer);
                    fcc.setGame(game);
                    requireNativeNations(game);
                    SwingUtilities.invokeLater(() -> {
                            gui.closeStatusPanel();
                            gui.setFocus(game.getMap().getTile(1,1));
                            gui.updateMenuBar();
                            gui.refresh();
                        });
                } catch (FileNotFoundException fnfe) {
                    ej = gui.errorJob(fnfe,
                        FreeCol.badFile("error.couldNotFind", theFile));
                } catch (IOException ioe) {
                    ej = gui.errorJob(ioe, "server.initialize");
                } catch (XMLStreamException xse) {
                    ej = gui.errorJob(xse,
                        FreeCol.badFile("error.couldNotLoad", theFile));
                } catch (FreeColException ex) {
                    ej = gui.errorJob(ex, "server.initialize");
                }
                if (ej != null) {
                    ej.setRunnable(fcc.invokeMainPanel(null)).invokeLater();
                }
            }
        }.start();
    }
}
