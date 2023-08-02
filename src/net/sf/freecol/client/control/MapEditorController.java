/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.panel.MiniMap; // FIXME: should go away
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.generator.MapGenerator;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The map editor controller.
 */
public final class MapEditorController extends FreeColClientHolder {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapEditorController.class.getName());

    /** Map height in MapGeneratorOptionsDialog. */
    private static final int MINI_MAP_THUMBNAIL_FINAL_HEIGHT = 64;

    /**
     * The transform that should be applied to a {@code Tile}
     * that is clicked on the map.
     */
    private MapTransform currentMapTransform = null;


    /**
     * Creates a new {@code MapEditorController}.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public MapEditorController(FreeColClient freeColClient) {
        super(freeColClient);
    }


    /**
     * Create a thumbnail for the minimap.
     * 
     * FIXME: Delete all code inside this method and replace it with
     *        sensible code directly drawing in necessary size,
     *        without creating a throwaway GUI panel, drawing in wrong
     *        size and immediately resizing.
     *        Consider moving to ImageLibrary in due course, but not
     *        until the MiniMap dependency is gone.
     *
     * @return The created {@code BufferedImage}.
     */
    private BufferedImage createMiniMapThumbNail() {
        MiniMap miniMap = new MiniMap(getFreeColClient());
        miniMap.setTileSize(MiniMap.MAX_TILE_SIZE);
        final Map map = getMap();
        int width = map.getWidth() * MiniMap.MAX_TILE_SIZE
            + MiniMap.MAX_TILE_SIZE / 2;
        int height = map.getHeight() * MiniMap.MAX_TILE_SIZE / 4;
        miniMap.setSize(width, height);
        BufferedImage image = new BufferedImage(
            width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = image.createGraphics();
        miniMap.paintEntireMinimap(g1, MiniMap.MAX_TILE_SIZE, new Dimension(width, height));
        g1.dispose();

        int scaledWidth = Math.min((int)((64 * width) / (float)height), 128);
        BufferedImage scaledImage = new BufferedImage(scaledWidth,
            MINI_MAP_THUMBNAIL_FINAL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaledImage.createGraphics();
        g2.drawImage(image, 0, 0, scaledWidth, MINI_MAP_THUMBNAIL_FINAL_HEIGHT,
                     null);
        g2.dispose();
        return scaledImage;
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

    /**
     * Enters map editor mode.
     *
     * FIXME: The TC and difficulty level can now be set at the
     * command line, but we should do better.
     */
    public void startMapEditor() {
        final FreeColClient fcc = getFreeColClient();
        final GUI gui = getGUI();
        try {
            final Specification specification = getDefaultSpecification();
            
            final List<FreeColModFile> mods = getClientOptions().getActiveMods();
            final boolean specificationChanges  = mods.stream().anyMatch(m -> m.hasSpecification());
            specification.loadMods(mods);
            
            fcc.setMapEditor(true);
            final FreeColServer freeColServer
                = new FreeColServer(false, false, specification, null, 0, null);
            fcc.setFreeColServer(freeColServer);
            ServerGame serverGame = freeColServer.getGame();
            requireNativeNations(serverGame);
            fcc.setGame(serverGame);
            fcc.setMyPlayer(null);
            gui.playSound(null);
            gui.closeMainPanel();
            gui.closeMenus();
            //fcc.changeClientState(true);
            //gui.changeView((Tile)null);
            gui.startMapEditorGUI();
            
            if (specificationChanges) {
                gui.showInformationPanel("mapEditor.loadedWithMods");
            }
        } catch (IOException e) {
            gui.showErrorPanel(StringTemplate
                .template("server.initialize"));
            return;
        }
    }

    /**
     * Get the default specification from the default rules.
     *
     * @return A {@code Specification} to use in the map editor.
     */
    public Specification getDefaultSpecification() {
        return FreeCol.loadSpecification(FreeCol.getRulesFile(), 
            FreeCol.getAdvantages(), FreeCol.getDifficulty());
    }
        
    /**
     * Sets the currently chosen {@code MapTransform}.
     *
     * @param mt The transform that should be applied to a
     *     {@code Tile} that is clicked on the map.
     */
    public void setMapTransform(MapTransform mt) {
        currentMapTransform = mt;
        getGUI().changeView(mt);
    }

    /**
     * Gets the current {@code MapTransform}.
     *
     * @return The transform that should be applied to a
     *     {@code Tile} that is clicked on the map.
     */
    public MapTransform getMapTransform() {
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
        final FreeColServer freeColServer = getFreeColServer();
        final ServerGame serverGame = freeColServer.getGame();

        getGUI().removeInGameComponents();
        getGUI().showMapGeneratorOptionsDialog(true, mgo -> {
            if (mgo != null) {
                serverGame.setMapGeneratorOptions(mgo);
                freeColServer.generateMap(false);
                requireNativeNations(serverGame);
                
                getGUI().setFocus(serverGame.getMap().getTile(1,1));
                getGUI().updateMenuBar();
                getGUI().startMapEditorGUI();
                getGUI().refresh();
            }
        });
    }

    /**
     * Opens a dialog where the user should specify the filename
     * and saves the game.
     */
    public void saveMapEditorGame() {
        File dir = FreeColDirectories.getUserMapsDirectory();
        if (dir == null) dir = FreeColDirectories.getSaveDirectory();
        File file = getGUI()
            .showSaveDialog(dir, FreeColDirectories.MAP_EDITOR_FILE_NAME);
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
                    BufferedImage thumb = createMiniMapThumbNail();
                    getFreeColServer().saveMapEditorGame(file, thumb);
                    SwingUtilities.invokeLater(() -> {
                            gui.closeStatusPanel();
                        });
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                            gui.closeStatusPanel();
                            gui.showErrorPanel(FreeCol.badFile("error.couldNotSave", file),
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
        File file = getGUI()
            .showLoadSaveFileDialog(FreeColDirectories.getUserMapsDirectory(),
                                    FreeCol.FREECOL_MAP_EXTENSION, "*");
        if (file != null) loadGame(file);
    }

    /**
     * Loads a game from the given file.
     *
     * @param file The {@code File}.
     */
    private void loadGame(File file) {
        final FreeColClient fcc = getFreeColClient();
        final GUI gui = getGUI();

        fcc.setMapEditor(true);
        gui.showStatusPanel(Messages.message("status.loadingGame"));

        final File theFile = file;
        new Thread(FreeCol.CLIENT_THREAD + "Loading-Map") {
            @Override
            public void run() {
                final FreeColServer freeColServer = getFreeColServer();
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
                    gui.showErrorPanel(fnfe,
                        FreeCol.badFile("error.couldNotFind", theFile));
                } catch (IOException | FreeColException ioe) {
                    gui.showErrorPanel(ioe,
                        StringTemplate.key("server.initialize"));
                } catch (XMLStreamException xse) {
                    gui.showErrorPanel(xse,
                        FreeCol.badFile("error.couldNotLoad", theFile));
                }
            }
        }.start();
    }
}
