

package net.sf.freecol.client.gui.action;

import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.option.OptionGroup;


/**
* Stores the actions.
*/
public class ActionManager extends OptionGroup {
    private static final Logger logger = Logger.getLogger(ActionManager.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private FreeColClient freeColClient;

    /**
    * Creates a new <code>ActionManager</code>.
    * @param freeColClient The main client controller.
    */
    public ActionManager(FreeColClient freeColClient) {
        super("actionManager.name", "actionManager.shortDescription");
        
        this.freeColClient = freeColClient;
        
        // keep this list alphabetized.
        
        add(new BuildColonyAction(freeColClient));
        add(new BuildRoadAction(freeColClient));
        add(new ChangeAction(freeColClient));
        add(new ChatAction(freeColClient));
        add(new ClearOrdersAction(freeColClient));
        add(new ColopediaBuildingAction(freeColClient));
        add(new ColopediaFatherAction(freeColClient));
        add(new ColopediaGoodsAction(freeColClient));
        add(new ColopediaSkillAction(freeColClient));
        add(new ColopediaTerrainAction(freeColClient));
        add(new ColopediaUnitAction(freeColClient));        
        add(new DeclareIndependenceAction(freeColClient));
        add(new DisbandUnitAction(freeColClient));
        add(new DisplayGridAction(freeColClient));
        add(new DisplayTileNamesAction(freeColClient));
        add(new EndTurnAction(freeColClient));
        add(new EuropeAction(freeColClient));
        add(new ExecuteGotoOrdersAction(freeColClient));
        add(new FortifyAction(freeColClient));
        add(new GotoAction(freeColClient));
        add(new MapControlsAction(freeColClient));
        add(new MiniMapZoomInAction(freeColClient));
        add(new MiniMapZoomOutAction(freeColClient));
        add(new NewAction(freeColClient));
        add(new OpenAction(freeColClient));
        add(new PreferencesAction(freeColClient));
        add(new PlowAction(freeColClient));
        add(new ReconnectAction(freeColClient));
        add(new ReportContinentalCongressAction(freeColClient));
        add(new ReportForeignAction(freeColClient));
        add(new ReportIndianAction(freeColClient));        
        add(new ReportLabourAction(freeColClient));
        add(new ReportMilitaryAction(freeColClient));
        add(new ReportNavalAction(freeColClient));
        add(new ReportReligionAction(freeColClient));
        add(new ReportTradeAction(freeColClient));        
        add(new SaveAction(freeColClient));
        add(new SkipUnitAction(freeColClient));
        add(new UnloadAction(freeColClient));
        add(new WaitAction(freeColClient));
        add(new QuitAction(freeColClient));

        freeColClient.getClientOptions().add(this);
        freeColClient.getClientOptions().addToMap(this);
    }

    
    /**
    * Adds the given <code>FreeColAction</code>.
    * @param freeColAction The <code>FreeColAction</code> that should be added to this
    *        <code>ActionManager</code>.
    */
    public void add(FreeColAction freeColAction) {
        super.add(freeColAction);
    }

    
    /**
    * Gets the <code>FreeColAction</code> specified by the given <code>id</code>.
    * 
    * @param id The string identifying the action.
    * @return The <code>FreeColAction</code>.
    */
    public FreeColAction getFreeColAction(String id) {
        Iterator it = iterator();
        while (it.hasNext()) {
            FreeColAction fa = (FreeColAction) it.next();
            if (fa.getId().equals(id)) {
                return fa;
            }
        }

        return null;
    }
    
    
    /**
    * Updates every <code>FreeColAction</code> this object keeps.
    * @see FreeColAction
    */
    public void update() {      
        Iterator it = iterator();
        while (it.hasNext()) {
            FreeColAction fa = (FreeColAction) it.next();
            fa.update();
        }
    }
}
