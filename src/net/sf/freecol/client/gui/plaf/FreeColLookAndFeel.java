package net.sf.freecol.client.gui.plaf;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;
import javax.swing.plaf.basic.*;
import java.io.File;

import net.sf.freecol.client.gui.plaf.FreeColCheckBoxUI;
import net.sf.freecol.client.gui.plaf.FreeColRadioButtonUI;
import net.sf.freecol.client.gui.plaf.FreeColButtonUI;
import net.sf.freecol.client.gui.plaf.FreeColPanelUI;
import net.sf.freecol.client.gui.plaf.FreeColMenuBarUI;
import net.sf.freecol.client.gui.plaf.FreeColPopupMenuUI;
import net.sf.freecol.client.gui.plaf.FreeColTextFieldUI;
import net.sf.freecol.client.gui.plaf.FreeColTextAreaUI;



/**
* Implements the "FreeCol Look and Feel".
*/
public class FreeColLookAndFeel extends MetalLookAndFeel {

    private File uiDirectory;



    /**
    * Initiates a new "FreeCol Look and Feel".
    *
    * @param uiDirectory The directory containing the UI-graphics.
    * @exception IllegalArgumentException if the <code>uiDirectory</code>
    *            does not exists.
    */
    public FreeColLookAndFeel(File uiDirectory) {
        super();

        if (!uiDirectory.exists()) {
            throw new IllegalArgumentException("The file \"" + uiDirectory.getName() + "\" does not exist.");
        }

        if (!uiDirectory.isDirectory()) {
            throw new IllegalArgumentException("The file \"" + uiDirectory.getName() + "\" is not a directory.");
        }

        this.uiDirectory = uiDirectory;

        setCurrentTheme(new DefaultMetalTheme() {
            protected ColorUIResource getSecondary3() {
                return new ColorUIResource(new Color(216, 194, 145));
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

            // Add other UI resources:
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
