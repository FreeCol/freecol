package net.sf.freecol.client.gui;

import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenu;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.ColopediaBuildingAction;
import net.sf.freecol.client.gui.action.ColopediaFatherAction;
import net.sf.freecol.client.gui.action.ColopediaGoodsAction;
import net.sf.freecol.client.gui.action.ColopediaSkillAction;
import net.sf.freecol.client.gui.action.ColopediaTerrainAction;
import net.sf.freecol.client.gui.action.ColopediaUnitAction;
import net.sf.freecol.client.gui.action.DisplayGridAction;
import net.sf.freecol.client.gui.action.DisplayTileNamesAction;
import net.sf.freecol.client.gui.action.DisplayTileOwnersAction;
import net.sf.freecol.client.gui.action.MapControlsAction;
import net.sf.freecol.client.gui.action.NewAction;
import net.sf.freecol.client.gui.action.NewEmptyMapAction;
import net.sf.freecol.client.gui.action.OpenAction;
import net.sf.freecol.client.gui.action.PreferencesAction;
import net.sf.freecol.client.gui.action.QuitAction;
import net.sf.freecol.client.gui.action.SaveAction;
import net.sf.freecol.client.gui.action.ShowMainAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.menu.DebugMenu;

/**
 * The menu bar used when running in editor mode.
 * 
 * <br><br>
 * 
 * The menu bar that is displayed on the top left corner of the
 * <code>Canvas</code>.
 * 
 * @see Canvas#setJMenuBar
 * @see InGameMenuBar
 */
public class MapEditorMenuBar extends FreeColMenuBar {
    private static final Logger logger = Logger.getLogger(MapEditorMenuBar.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";


    /**
     * Creates a new <code>MapEditorMenuBar</code>. This menu bar will include
     * all of the submenus and items.
     * 
     * @param freeColClient The main controller.
     */
    public MapEditorMenuBar(final FreeColClient freeColClient) {
        super(freeColClient);

        buildGameMenu();
        buildViewMenu();
        buildColopediaMenu();

        // --> Debug
        if (FreeCol.isInDebugMode()) {
            add(new DebugMenu(freeColClient));
        }

        update();
    }

    private void buildGameMenu() {
        // --> Game
        JMenu menu = new JMenu(Messages.message("menuBar.game"));
        menu.setOpaque(false);
        menu.setMnemonic(KeyEvent.VK_G);

        menu.add(getMenuItem(NewAction.ID));
        menu.add(getMenuItem(NewEmptyMapAction.ID));
        
        menu.addSeparator();
        
        menu.add(getMenuItem(OpenAction.ID));
        menu.add(getMenuItem(SaveAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(PreferencesAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(ShowMainAction.ID));
        menu.add(getMenuItem(QuitAction.ID));

        add(menu);
    }
    
    private void buildViewMenu() {
        // --> View

        JMenu menu = new JMenu(Messages.message("menuBar.view"));
        menu.setOpaque(false);
        menu.setMnemonic(KeyEvent.VK_V);

        menu.add(getCheckBoxMenuItem(MapControlsAction.ID));
        menu.add(getCheckBoxMenuItem(DisplayTileNamesAction.ID));
        menu.add(getCheckBoxMenuItem(DisplayTileOwnersAction.ID));
        menu.add(getCheckBoxMenuItem(DisplayGridAction.ID));

        add(menu);
    }
    
    private void buildColopediaMenu() {
        // --> Colopedia

        JMenu menu = new JMenu(Messages.message("menuBar.colopedia"));
        menu.setOpaque(false);
        menu.setMnemonic(KeyEvent.VK_C);

        menu.add(getMenuItem(ColopediaTerrainAction.ID));
        menu.add(getMenuItem(ColopediaUnitAction.ID));
        menu.add(getMenuItem(ColopediaGoodsAction.ID));
        menu.add(getMenuItem(ColopediaSkillAction.ID));
        menu.add(getMenuItem(ColopediaBuildingAction.ID));
        menu.add(getMenuItem(ColopediaFatherAction.ID));

        add(menu);
    }
}
