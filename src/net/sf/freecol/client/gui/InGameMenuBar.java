package net.sf.freecol.client.gui;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.AboutAction;
import net.sf.freecol.client.gui.action.AssignTradeRouteAction;
import net.sf.freecol.client.gui.action.BuildColonyAction;
import net.sf.freecol.client.gui.action.BuildRoadAction;
import net.sf.freecol.client.gui.action.ChangeAction;
import net.sf.freecol.client.gui.action.ChatAction;
import net.sf.freecol.client.gui.action.ClearOrdersAction;
import net.sf.freecol.client.gui.action.ColopediaBuildingAction;
import net.sf.freecol.client.gui.action.ColopediaFatherAction;
import net.sf.freecol.client.gui.action.ColopediaGoodsAction;
import net.sf.freecol.client.gui.action.ColopediaSkillAction;
import net.sf.freecol.client.gui.action.ColopediaTerrainAction;
import net.sf.freecol.client.gui.action.ColopediaUnitAction;
import net.sf.freecol.client.gui.action.DeclareIndependenceAction;
import net.sf.freecol.client.gui.action.DisbandUnitAction;
import net.sf.freecol.client.gui.action.DisplayGridAction;
import net.sf.freecol.client.gui.action.DisplayTileNamesAction;
import net.sf.freecol.client.gui.action.DisplayTileOwnersAction;
import net.sf.freecol.client.gui.action.EndTurnAction;
import net.sf.freecol.client.gui.action.EuropeAction;
import net.sf.freecol.client.gui.action.ExecuteGotoOrdersAction;
import net.sf.freecol.client.gui.action.FortifyAction;
import net.sf.freecol.client.gui.action.GotoAction;
import net.sf.freecol.client.gui.action.MapControlsAction;
import net.sf.freecol.client.gui.action.NewAction;
import net.sf.freecol.client.gui.action.OpenAction;
import net.sf.freecol.client.gui.action.PlowAction;
import net.sf.freecol.client.gui.action.PreferencesAction;
import net.sf.freecol.client.gui.action.QuitAction;
import net.sf.freecol.client.gui.action.ReconnectAction;
import net.sf.freecol.client.gui.action.RenameAction;
import net.sf.freecol.client.gui.action.ReportCargoAction;
import net.sf.freecol.client.gui.action.ReportColonyAction;
import net.sf.freecol.client.gui.action.ReportContinentalCongressAction;
import net.sf.freecol.client.gui.action.ReportForeignAction;
import net.sf.freecol.client.gui.action.ReportIndianAction;
import net.sf.freecol.client.gui.action.ReportLabourAction;
import net.sf.freecol.client.gui.action.ReportMilitaryAction;
import net.sf.freecol.client.gui.action.ReportNavalAction;
import net.sf.freecol.client.gui.action.ReportReligionAction;
import net.sf.freecol.client.gui.action.ReportRequirementsAction;
import net.sf.freecol.client.gui.action.ReportTradeAction;
import net.sf.freecol.client.gui.action.ReportTurnAction;
import net.sf.freecol.client.gui.action.SaveAction;
import net.sf.freecol.client.gui.action.SentryAction;
import net.sf.freecol.client.gui.action.ShowMainAction;
import net.sf.freecol.client.gui.action.SkipUnitAction;
import net.sf.freecol.client.gui.action.ToggleViewModeAction;
import net.sf.freecol.client.gui.action.TradeRouteAction;
import net.sf.freecol.client.gui.action.UnloadAction;
import net.sf.freecol.client.gui.action.WaitAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.menu.DebugMenu;

/**
 * This is the menu bar used in-game.
 * 
 * <br><br>
 * 
 * The menu bar that is displayed on the top left corner of the
 * <code>Canvas</code>.
 * 
 * @see Canvas#setJMenuBar
 * @see MapEditorMenuBar
 */
public class InGameMenuBar extends FreeColMenuBar {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(InGameMenuBar.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final int UNIT_ORDER_WAIT = 0;

    public static final int UNIT_ORDER_FORTIFY = 1;

    public static final int UNIT_ORDER_SENTRY = 2;

    public static final int UNIT_ORDER_CLEAR_ORDERS = 3;

    public static final int UNIT_ORDER_BUILD_COL = 5;

    public static final int UNIT_ORDER_PLOW = 6;

    public static final int UNIT_ORDER_BUILD_ROAD = 7;

    public static final int UNIT_ORDER_SKIP = 9;

    public static final int UNIT_ORDER_DISBAND = 11;

    private JMenuItem reportsTradeMenuItem = null;


    /**
     * Creates a new <code>FreeColMenuBar</code>. This menu bar will include
     * all of the submenus and items.
     * 
     * @param f The main controller.
     */
    public InGameMenuBar(FreeColClient f) {

        // TODO: FreeColClient should not have to be passed in to this class.
        // This is only a menu bar, it doesn't need
        // a reference to the main controller. The only reason it has one now is
        // because DebugMenu needs it. And DebugMenu
        // needs it because it is using inner classes for ActionListeners and
        // those inner classes use the reference.
        // If those inner classes were in seperate classes, when they were
        // created, they could use the FreeColClient
        // reference of the ActionManger. So DebugMenu needs to be refactored to
        // remove inner classes so that this
        // MenuBar can lose its unnecessary reference to the main controller.
        // See FreeColMenuTest.
        //
        // Okay, I lied.. the update() and paintComponent() methods in this
        // MenuBar use freeColClient, too. But so what.
        // Move those to another class too. :)
        
        super(f);

        reset();
    }

    
    /**
     * Resets this menu bar.
     */
    public void reset() {
        removeAll();
        
        buildGameMenu();
        buildViewMenu();
        buildOrdersMenu();
        buildReportMenu();
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
        menu.add(getMenuItem(OpenAction.ID));
        menu.add(getMenuItem(SaveAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(PreferencesAction.ID));
        menu.add(getMenuItem(ReconnectAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(ChatAction.ID));
        menu.add(getMenuItem(DeclareIndependenceAction.ID));
        menu.add(getMenuItem(EndTurnAction.ID));

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
        menu.add(getMenuItem(ToggleViewModeAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(EuropeAction.ID));
        menu.add(getMenuItem(TradeRouteAction.ID));

        add(menu);
    }

    private void buildOrdersMenu() {
        // --> Orders
        JMenu menu = new JMenu(Messages.message("menuBar.orders"));
        menu.setOpaque(false);
        menu.setMnemonic(KeyEvent.VK_O);

        menu.add(getMenuItem(WaitAction.ID));
        menu.add(getMenuItem(SentryAction.ID));
        menu.add(getMenuItem(FortifyAction.ID));
        menu.add(getMenuItem(GotoAction.ID));
        menu.add(getMenuItem(AssignTradeRouteAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(BuildColonyAction.ID));
        menu.add(getMenuItem(PlowAction.ID));
        menu.add(getMenuItem(BuildRoadAction.ID));
        menu.add(getMenuItem(UnloadAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(ExecuteGotoOrdersAction.ID));
        menu.add(getMenuItem(SkipUnitAction.ID));
        menu.add(getMenuItem(ChangeAction.ID));
        menu.add(getMenuItem(ClearOrdersAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(RenameAction.ID));
        menu.add(getMenuItem(DisbandUnitAction.ID));

        add(menu);
    }

    private void buildReportMenu() {
        // --> Report

        JMenu menu = new JMenu(Messages.message("menuBar.report"));
        menu.setOpaque(false);
        menu.setMnemonic(KeyEvent.VK_R);

        menu.add(getMenuItem(ReportReligionAction.ID));
        menu.add(getMenuItem(ReportLabourAction.ID));
        menu.add(getMenuItem(ReportColonyAction.ID));
        menu.add(getMenuItem(ReportForeignAction.ID));
        menu.add(getMenuItem(ReportIndianAction.ID));
        menu.add(getMenuItem(ReportContinentalCongressAction.ID));
        menu.add(getMenuItem(ReportMilitaryAction.ID));
        menu.add(getMenuItem(ReportNavalAction.ID));
        menu.add(getMenuItem(ReportCargoAction.ID));
        menu.add(getMenuItem(ReportTradeAction.ID));
        menu.add(getMenuItem(ReportTurnAction.ID));
        menu.add(getMenuItem(ReportRequirementsAction.ID));

        add(menu);

    }

    public JMenuItem getReportsTradeMenuItem() {
        return reportsTradeMenuItem;
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
        menu.addSeparator();        
        menu.add(getMenuItem(AboutAction.ID));
        
        add(menu);
    }
    
    /**
     * Paints information about gold, tax and year.
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        String displayString = Messages.message("menuBar.statusLine", new String[][] {
                { "%gold%", Integer.toString(freeColClient.getMyPlayer().getGold()) },
                { "%tax%", Integer.toString(freeColClient.getMyPlayer().getTax()) },
                { "%score%", Integer.toString(freeColClient.getMyPlayer().getScore()) },
                { "%year%", freeColClient.getGame().getTurn().toString() }});
        Rectangle2D displayStringBounds = g.getFontMetrics().getStringBounds(displayString, g);
        int y = 15 + getInsets().top;
        g.drawString(displayString, getWidth() - 10 - (int) displayStringBounds.getWidth(), y);
    }

}
