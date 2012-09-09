package soundcheck.musicPlayer;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.musicPlayer.interprocess.ServiceConnection;
import soundcheck.musicPlayer.streaming.StreamController;
import soundcheck.musicPlayer.view.PlayerGUI;
import soundcheck.shared.Const.Command;
import soundcheck.shared.DataPacket;
import soundcheck.shared.PacketCreator;
import soundcheck.shared.Peer;
import soundcheck.shared.Song;
import soundcheck.shared.Zone;
import soundcheck.shared.ZoneProperties;

/**
 * Initializes interface while simultaneously handling service connection.
 *
 */
public class ViewController implements ServiceCallback {
	final static Logger logger = LoggerFactory.getLogger(ViewController.class);

	private volatile PlayerGUI gui;
	private ServiceConnection serviceConnect;

	private boolean isGUIInitialized = false;
	private final Object GUI_INIT_LOCK = new Object();
	
	private Peer localPeer;

	public static void startView() throws InterruptedException {
		
		ViewController ic = new ViewController();

		ic.initGUI();

		// Wait for the GUI to initialize
		synchronized(ic.GUI_INIT_LOCK) {
			while(ic.isGUIInitialized == false) {
				ic.GUI_INIT_LOCK.wait();
			}
		}

		ic.serviceConnect = new ServiceConnection();

		// Request peer list from service
		ic.notify(Command.REQUEST_UPDATE, null, null);
		// Request local peer from service
		ic.notify(Command.GET_LOCAL_PEER, null, null);

		ic.receiveFromService();
	}

	/**
	 * This method will continually watch for messages from the service,
	 * and then act on them when they arrive.
	 */
	@SuppressWarnings("unchecked")
	public void receiveFromService() {

		while( true ) {
			DataPacket recData = serviceConnect.receiveData();

			if( recData == null) {
				logger.warn("Service sent bad data");
				continue;
			}

			if(isStreamingCmd(recData.getCommand())){
				StreamController.receiveStream(recData, this);
			}
			else{

				// Data is list of peers
				switch( recData.getCommand() ) {

				case SET_PEERLIST:
					logger.trace("Received new peerlist from service");

					List<Peer> peerList = (ArrayList<Peer>) recData.getData();

					logger.trace("Size of incoming peerlist is {}.", peerList.size());

					try {
						gui.updatePeerList(peerList);
					} catch (NullPointerException e) {
						// May be thrown if interface has not yet finished initializing.
						logger.warn("Interface is not ready to receive peerlist",e);
					}
					break;

				case ZONE_MAP:
					logger.trace("Received new zone map from service");

					Map<Zone,ZoneProperties> zoneMap = (ConcurrentHashMap<Zone,ZoneProperties>) recData.getData();

					try {
						gui.updateZoneMap(zoneMap);
					} catch (NullPointerException e) {
						// May be thrown if interface has not yet finished initializing.
						logger.warn("Interface is not ready to receive zonemap");
					}
					break;
					
				case GET_LOCAL_PEER:
					logger.trace("Received local peer data from service");
					
					this.localPeer = (Peer) recData.getData();
					break;
					
				case SET_SONGLIST:
					logger.trace("Received new songlist from service");

					List<Song> songList = (ArrayList<Song>) recData.getData();

					logger.trace("Size of incoming songlist is {} songs.", songList.size());

					try {
						// update song list with each peers list
						gui.updateSongList(songList);
					} catch (NullPointerException e) {
						// May be thrown if interface has not yet finished initializing.
						logger.warn("Interface is not ready to receive songlist",e);
					}
					break;
					
				case CONFIG:
					gui.setInitConfig((HashMap<String,String>) recData.getData());
					break;
				default:
					logger.error("View received unrecognized command.");
				}
			}
		}
	}

	/**
	 * Create the graphic user interface
	 */
	public void initGUI() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					gui = new PlayerGUI(ViewController.this);
					gui.setVisible(true);
					synchronized(GUI_INIT_LOCK) {
						isGUIInitialized = true;
						GUI_INIT_LOCK.notify(); // The GUI can now receive messages.
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Called by child objects to notify the service of an event.
	 */
	@Override
	public void notify(Command cmd, Object object) {
		notify(cmd, object, null);
	}

	/**
	 * Called by child objects to notify the service of an event.
	 */
	@Override
	public void notify(Command cmd, Object object, Zone zone) {
		logger.trace("Sending to service: " + cmd.toString() );
		serviceConnect.sendData(PacketCreator.createInterprocessPacket(cmd, object, zone));
	}

	/**
	 * Called by the streaming code to notify the GUI of the
	 * current time stamp
	 */
	@Override
	public void notifyGUISongProgress(long timeStamp) {
		gui.updateProgressBar(timeStamp);
	}
	
	@Override
	public void isStreaming(boolean isPlaying) {
		gui.isStreaming(isPlaying);
		
	}

	/** Determines if a Command is a Streaming related Command
	 * @param command
	 * @return - boolean
	 */
	private boolean isStreamingCmd(Command command) {

		boolean isStreamingCmd = false;

		switch(command){
		case SETUP:
			isStreamingCmd = true;
			break;
		case PLAY:
			isStreamingCmd = true;
			break;
		case PAUSE:
			isStreamingCmd = true;
			break;
		case NEXT:
			isStreamingCmd = true;
			break;
		case TEARDOWN:
			isStreamingCmd = true;
			break;
		default:
			isStreamingCmd = false;
		}


		return isStreamingCmd;
	}
	
	public Peer getLocalPeer() {
		return localPeer;
	}

}
