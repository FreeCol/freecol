/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
        super(freeColClient);
    }


    /**
     * Enters map editor modus.
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
            fcc.setInGame(true);
            getGUI().changeViewMode(GUI.VIEW_TERRAIN_MODE);
            getGUI().startMapEditorGUI();
        } catch (IOException e) {
            getGUI().showErrorMessage("server.initialize");
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
        getGUI().updateMapControls();
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
     * {@link #getMapTransform()} current <code>MapTransform</code>.
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
        final Game game = getGame();
        final Specification spec = getSpecification();

        getGUI().removeInGameComponents();
        OptionGroup mgo = getGUI().showMapGeneratorOptionsDialog(true);
        if (mgo == null) return;
        game.setMapGeneratorOptions(mgo);
        Map map = getFreeColServer().getMapGenerator()
            .createMap(new LogBuilder(-1));
        requireNativeNations(game);
        getGUI().setFocus(game.getMap().getTile(1,1));
        getGUI().updateMenuBar();
        getGUI().refresh();
    }

    /**
     * Opens a dialog where the user should specify the filename
     * and saves the game.
     */
    public void saveGame() {
        File file = getGUI().showSaveDialog(FreeColDirectories.getSaveDirectory(),
                                            FreeColDirectories.MAP_FILE_NAME);
        if (file != null) saveGame(file);
    }

    /**
     * Saves the game to the given file.
     *
     * @param file The <code>File</code>.
     */
    public void saveGame(final File file) {
        final Game game = getGame();
        Map map = game.getMap();
        map.resetContiguity();
        map.resetHighSeasCount();
        map.resetLayers();

        getGUI().showStatusPanel(Messages.message("status.savingGame"));
        Thread t = new Thread(FreeCol.CLIENT_THREAD+"Saving Map") {
                @Override
                public void run() {
                    try {
                        BufferedImage thumb = getGUI().createMiniMapThumbNail();
                        getFreeColServer().saveMapEditorGame(file, thumb);
                        SwingUtilities.invokeLater(() -> {
                                getGUI().closeStatusPanel();
                                getGUI().requestFocusInWindow();
                            });
                    } catch (IOException e) {
                        SwingUtilities.invokeLater(() -> {
                                getGUI().showErrorMessage(FreeCol.badSave(file));
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
        File file = getGUI().showLoadSaveFileDialog();
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

        getFreeColClient().setMapEditor(true);

        class ErrorJob implements Runnable {
            private final StringTemplate template;

            ErrorJob(StringTemplate template) {
                this.template = template;
            }

            @Override
            public void run() {
                getGUI().closeMenus();
                getGUI().showErrorMessage(template);
            }
        }

        getGUI().showStatusPanel(Messages.message("status.loadingGame"));

        Runnable loadGameJob = () -> {
            FreeColServer freeColServer = getFreeColServer();
            try {
                Specification spec = getDefaultSpecification();
                Game game = FreeColServer.readGame(new FreeColSavegameFile(theFile),
                                                   spec, freeColServer);
                getFreeColClient().setGame(game);
                requireNativeNations(game);
                SwingUtilities.invokeLater(() -> {
                        getGUI().closeStatusPanel();
                        getGUI().setFocus(getGame().getMap().getTile(1,1));
                        getGUI().updateMenuBar();
                        getGUI().refresh();
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
        getFreeColClient().setWork(loadGameJob);
    }

    private void reloadMainPanel () {
        SwingUtilities.invokeLater(() -> {
                getGUI().closeMainPanel();
                getGUI().showMainPanel(null);
                getSoundController().playSound("sound.intro.general");
            });
    }
}
