

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
    */
    public ActionManager(FreeColClient freeColClient) {
        super("actionManager.name", "actionManager.shortDescription");
        
        this.freeColClient = freeColClient;
        
        add(new BuildColonyAction(freeColClient));
        add(new BuildRoadAction(freeColClient));
        add(new PlowAction(freeColClient));
        add(new ClearOrdersAction(freeColClient));
        add(new DisbandUnitAction(freeColClient));
        add(new EuropeAction(freeColClient));
        add(new FortifyAction(freeColClient));
        add(new MapControlsAction(freeColClient));
        add(new WaitAction(freeColClient));
        add(new SkipUnitAction(freeColClient));
        add(new EndTurnAction(freeColClient));
        add(new ChatAction(freeColClient));
        add(new ChangeAction(freeColClient));
        
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
    * @param id The string identifying the action.
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
