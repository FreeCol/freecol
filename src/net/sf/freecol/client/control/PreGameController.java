
package net.sf.freecol.client.control;


import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;
import java.util.zip.InflaterInputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.CanvasKeyListener;
import net.sf.freecol.client.gui.CanvasMouseListener;
import net.sf.freecol.client.gui.CanvasMouseMotionListener;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Message;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;



/**
* The controller that will be used before the game starts.
*/
public final class PreGameController {
    private static final Logger logger = Logger.getLogger(PreGameController.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private FreeColClient freeColClient;





    /**
    * The constructor to use.
    * @param freeColClient The main controller.
    */
    public PreGameController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
    }





    /**
    * Sets this client to be (or not be) ready to start the game.
    * @param ready Indicates wether or not this client is ready
    *              to start the game.
    */
    public void setReady(boolean ready) {
        // Make the change:
        freeColClient.getMyPlayer().setReady(ready);

        // Inform the server:
        Element readyElement = Message.createNewRootElement("ready");
        readyElement.setAttribute("value", Boolean.toString(ready));

        freeColClient.getClient().send(readyElement);
    }


    /**
    * Sets this client's player's nation.
    * @param nation Which nation this player wishes to set.
    */
    public void setNation(String nation) {
        // Make the change:
        try {
            freeColClient.getMyPlayer().setNation(nation);
        }
        catch (FreeColException e) {
            logger.warning(e.getMessage());
        }

        // Inform the server:
        Element nationElement = Message.createNewRootElement("setNation");
        nationElement.setAttribute("value", nation);

        freeColClient.getClient().send(nationElement);
    }


    /**
    * Sets this client's player's color.
    * @param color Which color this player wishes to set.
    */
    public void setColor(Color color) {
        // Make the change:
        freeColClient.getMyPlayer().setColor(color);

        // Inform the server:
        Element colorElement = Message.createNewRootElement("setColor");
        colorElement.setAttribute("value", Player.convertColorToString(color));

        freeColClient.getClient().send(colorElement);
    }


    /**
    * Requests the game to be started. This will only be successful
    * if all players are ready to start the game.
    */
    public void requestLaunch() {
        Canvas canvas = freeColClient.getCanvas();

        if (!freeColClient.getGame().isAllPlayersReadyToLaunch()) {
            canvas.errorMessage("server.notAllReady");
            return;
        }

        Element requestLaunchElement = Message.createNewRootElement("requestLaunch");
        freeColClient.getClient().send(requestLaunchElement);

        canvas.setEnabled(false);
        canvas.showStatusPanel( Messages.message("status.startingGame") );
    }


    /**
    * Sends a chat message.
    * @param message The message as plain text.
    */
    public void chat(String message) {
        Element chatElement = Message.createNewRootElement("chat");
        chatElement.setAttribute("senderName", freeColClient.getMyPlayer().getName());
        chatElement.setAttribute("message", message);
        chatElement.setAttribute("privateChat", "false");

        freeColClient.getClient().send(chatElement);
    }
    

    /**
    * Sends the {@link GameOptions} to the server.
    * This method should be called after updating that object.
    */
    public void sendGameOptions() {
        Element updateGameOptionsElement = Message.createNewRootElement("updateGameOptions");
        updateGameOptionsElement.appendChild(freeColClient.getGame().getGameOptions().toXMLElement(updateGameOptionsElement.getOwnerDocument()));

        freeColClient.getClient().send(updateGameOptionsElement);        
    }

    
    /**
    * Reads the {@link GameOptions} from the given file.
    * *
    * @param loadFile The <code>File</code> from which the
    *       <code>GameOptions</code> should be loaded.
    * @throws IOException If the exception is thrown
    *       while opening or reading the file. 
    */
    public void loadGameOptions(File loadFile) throws IOException {
        //BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(loadFile)));
        Element element;
        try {
            if (loadFile.getName().endsWith(".fsg")) {
                Message message = new Message(new InflaterInputStream(new FileInputStream(loadFile)));
                element = (Element) message.getDocument().getDocumentElement().getElementsByTagName(GameOptions.getXMLElementTagName()).item(0);
            } else {
                Message message = new Message(new FileInputStream(loadFile));
                element = message.getDocument().getDocumentElement();
            }
        } catch (SAXException sxe) {
            // Error generated during parsing
            Exception  x = sxe;
            if (sxe.getException() != null) {
                x = sxe.getException();
            }
            StringWriter sw = new StringWriter();
            x.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new IOException("SAXException while creating Message.");
        } catch (NullPointerException e) {
            throw new IOException("the given file does not contain game options.");
        }

        if (element != null) {
            freeColClient.getGame().getGameOptions().readFromXMLElement(element);
        } else {
            throw new IOException("the given file does not contain game options.");
        }
    }


    /**
    * Writes the {@link GameOptions} to the given file.
    * 
    * @param saveFile The <code>File</code> to which the
    *       <code>GameOptions</code> should be written.
    * @throws IOException If the exception is thrown
    *       while creating or writing to the file.
    */
    public void saveGameOptions(File saveFile) throws IOException {
        Element element = freeColClient.getGame().getGameOptions().toXMLElement(Message.createNewDocument());

        // Write the XML Element to the file:
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer xmlTransformer = factory.newTransformer();
            //xmlTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            xmlTransformer.setOutputProperty(OutputKeys.INDENT, "yes");

            PrintWriter out = new PrintWriter(new FileOutputStream(saveFile));
            xmlTransformer.transform(new DOMSource(element), new StreamResult(out));
            out.close();
        } catch (TransformerException e) {
            e.printStackTrace();
            return;
        }
    }


    /**
    * Starts the game.
    */
    public void startGame() {
        Canvas canvas = freeColClient.getCanvas();
        GUI gui = freeColClient.getGUI();

        canvas.closeMainPanel();
        canvas.closeMenus();

        InGameController inGameController = freeColClient.getInGameController();
        InGameInputHandler inGameInputHandler = freeColClient.getInGameInputHandler();

        freeColClient.getClient().setMessageHandler(inGameInputHandler);
        gui.setInGame(true);

        freeColClient.getCanvas().resetFreeColMenuBar();

        Unit activeUnit = freeColClient.getMyPlayer().getNextActiveUnit();
        freeColClient.getMyPlayer().updateCrossesRequired();
        gui.setActiveUnit(activeUnit);
        if (activeUnit != null) {
            gui.setFocus(activeUnit.getTile().getPosition());
        } else {
            gui.setFocus(((Tile) freeColClient.getMyPlayer().getEntryLocation()).getPosition());
        }

        canvas.addKeyListener(new CanvasKeyListener(canvas, inGameController));
        canvas.addMouseListener(new CanvasMouseListener(canvas, gui));
        canvas.addMouseMotionListener(new CanvasMouseMotionListener(canvas, gui,  freeColClient.getGame().getMap()));

        if (freeColClient.getMyPlayer().equals(freeColClient.getGame().getCurrentPlayer())) {
            canvas.requestFocus();
        } else {
            canvas.setEnabled(false);
            canvas.showStatusPanel(Messages.message("waitingForOtherPlayers"));
        }
    }
}
