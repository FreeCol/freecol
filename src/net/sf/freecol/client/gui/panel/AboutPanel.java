/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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


package net.sf.freecol.client.gui.panel;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.resources.ResourceManager;

/**
 * This is the About panel
 */
public final class AboutPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(AboutPanel.class.getName());

    public static final String SITE_URL = "http://www.freecol.org";
    public static final String PROJECT_URL = "http://sourceforge.net/projects/freecol/";

    /**
    * The constructor that will add the items to this panel.
    *
    * @param parent The parent of this panel.
    */
    public AboutPanel(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, new MigLayout("wrap 2"));

        // Header with image
        Image tempImage = ResourceManager.getImage("TitleImage");
        if (tempImage != null) {
            JLabel logoLabel = new JLabel(new ImageIcon(tempImage));
            logoLabel.setBorder(new CompoundBorder(new EmptyBorder(2,2,2,2), new BevelBorder(BevelBorder.LOWERED)));
            add(logoLabel, "span, center");
        }

        // version and links
        add(localizedLabel("aboutPanel.version"), "newline 20");
        add(new JLabel(FreeCol.getRevision()));

        add(localizedLabel("aboutPanel.officialSite"));
        JButton site = getLinkButton(SITE_URL, null, SITE_URL);
        site.addActionListener(this);
        add(site);

        add(localizedLabel("aboutPanel.sfProject"));
        JButton project = getLinkButton(PROJECT_URL, null, PROJECT_URL);
        project.addActionListener(this);
        add(project);

        // license disclaimer
        add(getDefaultTextArea(Messages.message("aboutPanel.legalDisclaimer")),
            "newline 20, span, growx");

        // copyright
        add(localizedLabel("aboutPanel.copyright"), "span, center");

        add(okButton, "newline 20, span, tag ok");

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String url = event.getActionCommand();
        if (SITE_URL.equals(url) || PROJECT_URL.equals(url)) {
            String os = System.getProperty("os.name");
            String[] cmd = null;
            if (os == null) {
                // error, the operating system could not be determined
                return;
            } else if (os.toLowerCase().contains("mac")) {
                // Apple Macintosh, Safari is the main browser
                cmd = new String[] { "open" , "-a", "Safari", url };
            } else if (os.toLowerCase().contains("windows")) {
                // Microsoft Windows, use the default browser
                cmd = new String[] { "rundll32.exe", "url.dll,FileProtocolHandler", url};
            } else if (os.toLowerCase().contains("linux")) {
                // GNU Linux, use xdg-utils to launch the default browser (portland.freedesktop.org)
                cmd = new String[] {"xdg-open", url};
            } else {
                // Unix, ...
                // TODO: should we just call an arbitrary browser like this?
                //cmd = new String[] { "netscape",  "-remote", "openURL(" + url + ")"};
                cmd = new String[] { "firefox", url};
            }
            try {
                Runtime.getRuntime().exec(cmd);
            } catch(IOException x) {
                // couldn't start browser
            }
        } else {
            super.actionPerformed(event);
        }
    }
}
