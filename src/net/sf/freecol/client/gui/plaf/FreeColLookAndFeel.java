package net.sf.freecol.client.gui.plaf;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

import net.sf.freecol.common.FreeColException;

import java.io.File;
import java.net.URL;


/**
 * Implements the "FreeCol Look and Feel".
 */
public class FreeColLookAndFeel extends MetalLookAndFeel {

    private final static Class resourceLocator = net.sf.freecol.FreeCol.class;
    private File uiDirectory;
    
    private static final Color PRIMARY_1 = new Color(122, 109, 82),
                               BG_COLOR_SELECT = new Color(255, 244, 195),
                               PRIMARY_3 = new Color(203, 182, 136),
                               SECONDARY_1 = new Color(10, 10, 10),
                               DISABLED_COLOR = new Color(166, 144, 95),
                               BG_COLOR = new Color(216, 194, 145);
                               
                               


    /**
     * Initiates a new "FreeCol Look and Feel".
     *
     * @exception FreeColException If the ui directory could not be found.
     */
    public FreeColLookAndFeel() throws FreeColException {
        this("");
    }
    

   /**
    * Initiates a new "FreeCol Look and Feel".
    *
    * @param dataFolder The home of the FreeCol data files.
    * @exception FreeColException If the ui directory could not be found.
    */
    public FreeColLookAndFeel(String dataFolder) throws FreeColException {
        super();
        
        if(dataFolder.equals("")) { // lookup is necessary
            URL uiDir = resourceLocator.getResource("data/images/ui");
            
            if(uiDir == null) {
                uiDirectory = new File("data" + System.getProperty("file.separator") + "images" + System.getProperty("file.separator") + "ui");            
                
                if(!uiDirectory.exists() || !uiDirectory.isDirectory()) {
                    throw new FreeColException("UI directory not found!");
                }
            }
        } else {
            uiDirectory = new File(dataFolder + "images" + System.getProperty("file.separator") + "ui");

            if(!uiDirectory.exists() || !uiDirectory.isDirectory()) {
                throw new FreeColException("UI directory not found in: " + uiDirectory.getName());
            }
        }
        
        setCurrentTheme(new DefaultMetalTheme() {
            protected ColorUIResource getPrimary1() {
                return new ColorUIResource(PRIMARY_1);
            }

            protected ColorUIResource getPrimary2() {
                return new ColorUIResource(BG_COLOR_SELECT);
            }

            protected ColorUIResource getPrimary3() {
                return new ColorUIResource(PRIMARY_3);
            }

            protected ColorUIResource getSecondary1() {
                return new ColorUIResource(SECONDARY_1);
            }

            protected ColorUIResource getSecondary2() {
                return new ColorUIResource(DISABLED_COLOR);
            }

            protected ColorUIResource getSecondary3() {
                return new ColorUIResource(BG_COLOR);
            }
        });
    }


    
    
   /**
    * Creates the look and feel specific defaults table.
    * @return The defaults table.
    */
    public UIDefaults getDefaults() {
        UIDefaults u = super.getDefaults();

        try {
            // Add ComponentUI classes:
            u.put("CheckBoxUI", "net.sf.freecol.client.gui.plaf.FreeColCheckBoxUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColCheckBoxUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColCheckBoxUI"));
            u.put("ComboBoxUI", "net.sf.freecol.client.gui.plaf.FreeColComboBoxUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColComboBoxUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColComboBoxUI"));
            u.put("RadioButtonUI", "net.sf.freecol.client.gui.plaf.FreeColRadioButtonUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColRadioButtonUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColRadioButtonUI"));
            u.put("ButtonUI", "net.sf.freecol.client.gui.plaf.FreeColButtonUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColButtonUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColButtonUI"));
            u.put("TextFieldUI", "net.sf.freecol.client.gui.plaf.FreeColTextFieldUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColTextFieldUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColTextFieldUI"));
            u.put("TextAreaUI", "net.sf.freecol.client.gui.plaf.FreeColTextAreaUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColTextAreaUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColTextAreaUI"));
            u.put("PanelUI", "net.sf.freecol.client.gui.plaf.FreeColPanelUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColPanelUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColPanelUI"));
            u.put("MenuBarUI", "net.sf.freecol.client.gui.plaf.FreeColMenuBarUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColMenuBarUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColMenuBarUI"));
            u.put("PopupMenuUI", "net.sf.freecol.client.gui.plaf.FreeColPopupMenuUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColPopupMenuUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColPopupMenuUI"));
            u.put("LabelUI", "net.sf.freecol.client.gui.plaf.FreeColLabelUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColLabelUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColLabelUI"));
            u.put("MenuItemUI", "net.sf.freecol.client.gui.plaf.FreeColMenuItemUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColMenuItemUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColMenuItemUI"));
            u.put("ListUI", "net.sf.freecol.client.gui.plaf.FreeColListUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColListUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColListUI"));
            u.put("TableUI", "net.sf.freecol.client.gui.plaf.FreeColTableUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColTableUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColTableUI"));
            u.put("TableHeaderUI", "net.sf.freecol.client.gui.plaf.FreeColTableHeaderUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColTableHeaderUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColTableHeaderUI"));
            u.put("ScrollPaneUI", "net.sf.freecol.client.gui.plaf.FreeColScrollPaneUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColScrollPaneUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColScrollPaneUI"));

            // Add other UI resources:
            if(uiDirectory != null) { // no lookup
                File file;

                file = new File(uiDirectory, "bg.png");
                if (file.exists()) {
                    u.put("BackgroundImage", new ImageIcon(file.toString()));
                }

                file = new File(uiDirectory, "bg2.png");
                if (file.exists()) {
                    u.put("BackgroundImage2", new ImageIcon(file.toString()));
                } else {
                    u.put("BackgroundImage2", u.get("BackgroundImage"));
                }

                file = new File(uiDirectory, "bg_map1.jpg");
                if (file.exists()) {
                    u.put("CanvasBackgroundImage", new ImageIcon(file.toString()));
                }

                file = new File(uiDirectory, "freecol.png");
                if (file.exists()) {
                    u.put("TitleImage", new ImageIcon(file.toString()));
                }
            
                file = new File(uiDirectory, "victory.png");
                if (file.exists()) {
                    u.put("VictoryImage", new ImageIcon(file.toString()));
                }
            } else {
                URL url;
                String uiFolder = "data/images/ui/";
                
                url = resourceLocator.getResource(uiFolder + "bg.png");
                if (url != null) {
                    u.put("BackgroundImage", new ImageIcon(url));
                }

                url = resourceLocator.getResource(uiFolder + "bg2.png");
                if (url != null) {
                    u.put("BackgroundImage2", new ImageIcon(url));
                } else {
                    u.put("BackgroundImage2", u.get("BackgroundImage"));
                }

                url = resourceLocator.getResource(uiFolder + "bg_map1.jpg");
                if (url != null) {
                    u.put("CanvasBackgroundImage", new ImageIcon(url));
                }

                url = resourceLocator.getResource(uiFolder + "freecol.png");
                if (url != null) {
                    u.put("TitleImage", new ImageIcon(url));
                }
            
                url = resourceLocator.getResource(uiFolder + "victory.png");
                if (url != null) {
                    u.put("VictoryImage", new ImageIcon(url));
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println(e);
            System.exit(-1);
        }

        return u;
    }


    /**
    * Gets a one line description of this Look and Feel.
    * @return "The default Look and Feel for FreeCol"
    */
    public String getDescription() {
        return "The default Look and Feel for FreeCol";
    }


    /**
    * Gets the name of this Look and Feel.
    * @return "FreeCol Look and Feel"
    */
    public String getName() {
        return "FreeCol Look and Feel";
    }
}
