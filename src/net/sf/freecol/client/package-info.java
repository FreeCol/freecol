/**
 * <h1>FreeCol Client Package</h1>
 * 
 * <p>This is the main client package.</p>
 *
 * <h2>survey of Thread objects in the FreeCol client</h2>
 * 
 * <p>This is the way threads were used when this was written:</p>
 * 
 * <h3>Anonymous sub-classes of Thread</h3>
 *
 *  <UL>
 *      <li>Canvas (client shutdown hook)</li>
 *      <li>ConnectController (loading game)</li>
 *      <li>FreeCol (server shutdown hook)</li>
 *      <li>InGameController (save game, choose Founding Father)</li>
 *      <li>ReceivingThread (urgent messages)</li>
 *  </UL>
 *  
 * <p>(The shutdown hooks don't really count as they're not a normal use of a thread.)</p>
 * 
 * <h3>Named sub-classes of Thread</h3>
 * 
 *  <UL>
 *      <li>Canvas (ChatDisplayThread, TakeFocusThread)</li>
 *      <li>CanvasMouseMotionListener (ScrollThread)</li>
 *      <li>MetaServer</li>
 *      <li>ReceivingThread</li>
 *      <li>Server</li>
 *      <li>SoundPlayer (SoundPlayer)</li>
 *  </UL>
 *  
 * <p>Some code in FreeCol that does real work is run on the AWT thread. The AWT
 * thread is used to paint the user interface and to notify the application of user
 * interface events. When the AWT thread is busy, Java user interfaces look like
 * grey boxes. Users often report this as a "hang" or a "crash".</p>
 * 
 * <p>This can be avoided by only using the AWT thread for things that must be run
 * on it (such as to update the state of the user interface objects (JTable, etc.). 
 * Technically, all Swing methods should be invoked on the AWT thread).</p>
 * 
 * <p>What follows is not an invention, rather something that worked well on other
 * projects.</p>
 * 
 * <h2>The three-thread model of a GUI application</h2>
 *
 * <p>The three threads are:</p>
 *
 *  <OL>
 *      <li>the AWT thread</li>
 *      <li>the network thread</li>
 *      <li>the work thread</li>
 *  </OL>
 *
 * <h3>the AWT thread</h3>
 *
 * <p>The AWT thread is started by Java and runs all callbacks (such as MouseListener). When a callback is invoked, the
 * AWT thread does the work if it involves only manipulating Swing objects, otherwise it queues a job for the work
 * thread. All Swing objects should be manipulated on the AWT thread. This is done as normal with invokeLater(Runnable).
 * The behaviour ensures that the AWT thread is always ready to paint when the Operating System uncovers an application
 * window.</p>
 *
 * <h3>The network thread</h3>
 *
 * <p>The network thread is blocked from listening most of the time. When it wakes up, it may interact with the work
 * thread (typically by queueing a message that has been received) and then goes straight back to listening. This
 * behaviour improves the throughput of the link.</p>
 *
 * <h3>The work thread</h3>
 *
 * <p>The work thread is idle most of the time and does jobs for the other threads when they are queued.</p>
 *
 * <h3>Advantages</h3>
 *
 * <p>The model is very simple and because the only places in the code where synchronization is required are where the
 * AWT or network threads interact with the work thread, no synchronization is required over the rest of the code, which
 * saves typing, is easier to understand and faster.</p>
 *
 * <p style="text-align: center; font-weight: bold;">$Revision$</p>
 *
 * @since 0.2.0
 */
package net.sf.freecol.client;