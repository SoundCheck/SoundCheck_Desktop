package soundcheck.service;

import java.io.File;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jgroups.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.musicPlayer.ViewController;
import soundcheck.service.data.ConfigurationManager;
import soundcheck.service.data.PeerCollection;
import soundcheck.service.data.SongListManager;
import soundcheck.service.interprocess.MusicPlayerConnection;
import soundcheck.service.network.ExternalChannel;
import soundcheck.service.network.NetworkChannel;
import soundcheck.service.network.PeerManager;
import soundcheck.service.streaming.StreamController;
import soundcheck.shared.Const;
import soundcheck.shared.Const.Command;
import soundcheck.shared.Const.Service;
import soundcheck.shared.DataPacket;
import soundcheck.shared.PacketCreator;
import soundcheck.shared.Peer;
import soundcheck.shared.Song;
import soundcheck.shared.StreamInfo;
import soundcheck.shared.Util;
import soundcheck.shared.Zone;
import soundcheck.shared.ZonePair;
import soundcheck.shared.ZoneProperties;


public final class MasterController implements Runnable {
	final static Logger logger = LoggerFactory.getLogger(MasterController.class);


	private Peer localPeer;
	private NetworkChannel nc;
	HashMap<String,String> configs;

	/**
	 * Service entry point
	 * @param args
	 * @throws SocketException 
	 */
	public static void main(String[] args) throws SocketException {
		java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.OFF);



		Properties ip4 = System.getProperties();
		ip4.put("java.net.preferIPv4Stack", "true");

		System.setProperties(ip4);

		// Determine if we should start the service, view, or both

		// Check if service is already running, otherwise start service
		if( Util.isPortAvailable(Const.INTERFACE_PORT) == true) {
			new Thread(new MasterController()).start();
		}
		// If only service is specified, skip starting view
		if( args.length == 0 || !args[0].equalsIgnoreCase("-s") ) {
			try {
				ViewController.startView();
			} catch (InterruptedException e) {
				logger.error("",e);
			}
		}
	}

	/**
	 * Constructor. Starts network listeners and attempts to find music player.
	 */
	private MasterController() {
		// Load configuration data
		ConfigurationManager.loadSettings();
		String localName = ConfigurationManager.getConfigSetting("LocalName");
		String libName = ConfigurationManager.getConfigSetting("Library");

		// Put configuration data into hash map for sending to view
		configs = new HashMap<String,String>();
		configs.put("LocalName", localName);
		configs.put("Library", libName);
		localPeer = new Peer(localName);

		// Start network listeners
		nc = new NetworkChannel(this);
		new Thread(new ExternalChannel()).start();

		if( !libName.equals("") ) {	// no library in configuration settings
			// load library
			File libFile = new File(libName);
			new Thread(new SongListManager(libFile, this)).start();
		}

		PeerCollection.initPeerCollectionListener(this);


	}

	@Override
	public void run() {
		findMusicPlayer();
	}

	/**
	 * Search for instance of Music player on local computer.
	 */
	@SuppressWarnings("unchecked")
	private void findMusicPlayer() {
		boolean initialized = false;
		logger.info("Searching for SoundCheck Music Player...");
		MusicPlayerConnection.openSocket();

		// Keep this thread checking for data from the view until program exits.
		while( true ) {
			DataPacket packet = MusicPlayerConnection.receiveData();
			logger.trace("Received {} command.", packet.getCommand().toString());

			if (isStreamingCmd(packet.getCommand())) {
				if(packet.getZone() != null){
					StreamController.createStreamInfo(packet.getZone(), packet.getCommand());
				}
			} else {

				List<Address> mobileList = PeerManager.getMobileAddresses();

				switch (packet.getCommand()) {

				case REQUEST_UPDATE:
					// MusicPlayer has requested the current peerlist
					sendZoneMapToMusicPlayer();
					sendPeerListToMusicPlayer();
					sendSongListToMusicPlayer();

					if(initialized == false) {
						initialized = true;
						sendInitDataToMusicPlayer();
					}
					break;
				case SET_LOCAL_PEER:
					// Data on this peer
					localPeer = (Peer) packet.getData();
					break;
				case GET_LOCAL_PEER:
					sendLocalPeerToMusicPlayer();
					break;
				case PEER_ZONE_CHANGE:
					// Peers have changed zones
					NetworkChannel.send(null, PacketCreator.createPeerZonePacket(packet.getData()));

					for(Address mobAddress : mobileList){
						NetworkChannel.send(mobAddress, PacketCreator.createPeerZonePacket(packet.getData()));
					}

					break;
				case PEER_STATUS_CHANGE:
					// local peer changed privacy status
					boolean status = (Boolean) packet.getData();
					localPeer.setStatus(status);
					if( !status ) {
						localPeer.setZone(new Zone());
					}

					NetworkChannel.send(null, PacketCreator.createPeerStatusChangePacket(localPeer));
					break;
				case ZONE_MAP:
					// A new zone mapping has been sent
					NetworkChannel.send(null, PacketCreator.createNewZonePacket((ZonePair) packet.getData()));

					for(Address mobAddress : mobileList){
						NetworkChannel.send(mobAddress, PacketCreator.createNewZonePacket((ZonePair) packet.getData()));
					}

					break;
				case SET_SONGLIST:

					File libFile = (File) packet.getData();

					// load songs from file directory using SongListManager
					new Thread(new SongListManager(libFile, this)).start();
					break;
				case LIBRARY_UPDATE:

					// MusicPlayer has requested updated library in response to peer list change
					sendSongListToMusicPlayer();

					break;
				case QUEUE_FRONT:
				case QUEUE_BACK:
				case QUEUE_REMOVE:
				case NEW_QUEUE:

					NetworkChannel.send(null, PacketCreator.createNewQueuePacket(packet.getCommand(), packet.getData(), packet.getZone()));
					break;
				case CONFIG:
					HashMap<String,String> configs = (HashMap<String,String>)packet.getData();

					ConfigurationManager.setConfigSetting("LocalName",configs.get("LocalName"));
					ConfigurationManager.setConfigSetting("Library", configs.get("Library"));

					localPeer.setName(configs.get("LocalName"));
					NetworkChannel.send(null, PacketCreator.createPeerChangePacket(localPeer));

					// Save new settings to disk
					ConfigurationManager.saveSettings();
					break;
				case NEW_ZONE:
					ZonePair zp = new ZonePair(packet.getZone(), (ZoneProperties)packet.getData());
					NetworkChannel.send(null, PacketCreator.createNewZonePacket(zp));

					for(Address mobAddress : mobileList){
						NetworkChannel.send(mobAddress, PacketCreator.createNewZonePacket(zp));
					}

					break;
				case PASSWORD_SET:

					PeerCollection.getZonemap().get(packet.getZone()).setPassword((String) packet.getData());
					
					NetworkChannel.send(null, PacketCreator.createUpdatePacket());

				break;

			case STREAM_ACK:

				StreamInfo strInfo = (StreamInfo) packet.getData();

				NetworkChannel.send(strInfo.getAddr(), PacketCreator.createStreamAcknowledgePacket(Command.STREAM_ACK, strInfo, strInfo.getZone()));

				break;
			default:
				logger.error("Service received unrecognized command {}.", packet.getCommand().toString());
			}
		}
	}
}

/**
 * Determines if a Command is a Streaming related Command
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



/**
 * Sends the most recent zone map to the music player.
 */
public void sendPeerListToMusicPlayer() {
	if( MusicPlayerConnection.isConnected() ) {
		// Send peer list
		MusicPlayerConnection.sendData(
				PacketCreator.createInterprocessPacket(Command.SET_PEERLIST, PeerCollection.getPeers()) );
	} else {
		logger.warn("Could not find running instance of music player.");
	}
}

/**
 * Sends the most recent zone map to the music player.
 */
public void sendZoneMapToMusicPlayer() {
	if( MusicPlayerConnection.isConnected() ) {
		// Send Zone map
		MusicPlayerConnection.sendData(
				PacketCreator.createInterprocessPacket(Command.ZONE_MAP, PeerCollection.getZonemap()) );
	} else {
		logger.warn("Could not find running instance of music player.");
	}
}

/**
 * Sends the most recent song list to the music player.
 */
public synchronized void sendSongListToMusicPlayer() {
	if( MusicPlayerConnection.isConnected() ) {

		List<Song> songList = generateLibrary();

		// Send Song List
		MusicPlayerConnection.sendData(
				PacketCreator.createInterprocessPacket( Command.SET_SONGLIST, songList ));
	} else {
		logger.warn("Could not find running instance of music player.");
	}
}

public synchronized void sendInitDataToMusicPlayer() {
	if( MusicPlayerConnection.isConnected() ) {
		MusicPlayerConnection.sendData(
				PacketCreator.createInterprocessPacket( Command.CONFIG, configs ));
	} else {
		logger.warn("Could not find running instance of music player.");
	}
}

public synchronized void sendLocalPeerToMusicPlayer() {
	if( MusicPlayerConnection.isConnected() ) {
		MusicPlayerConnection.sendData( 
				PacketCreator.createInterprocessPacket( Command.GET_LOCAL_PEER, getLocalPeer() ));
	} else {
		logger.warn("Could not find running instance of music player.");
	}
}

/**
 * Generate the virtual library by combining the song list of all peers in the collection
 * 
 * @return virtual library
 */
public List<Song> generateLibrary() {

	List<Peer> peerList = PeerCollection.getPeers();
	List<Song> songList = new ArrayList<Song>();	// list to be returned

	for( Peer peer : peerList ) {
		// remove new songs to be added matching previously stored songs, preventing duplicates
		List<Song> newList = removeDuplicates( songList, peer.getSongList() );

		// combine lists
		songList.addAll( newList );
	}

	return songList;
}

/**
 * Remove songs that will result in the creation of duplicate entries
 * 
 * @param library current music library
 * @param local list of songs to be added to library
 * @return local list minus songs already in music library
 */
public List<Song> removeDuplicates( List<Song> library, List<Song> local ) {

	List<Song> listToMod = new ArrayList<Song>();
	listToMod.addAll(local);

	for( Song song : library ) { // iterate through library

		if( listToMod.contains( song )) { // check for matching song in new list
			listToMod.remove( song ); // remove from new list to prevent duplicate
		}	
	}

	return listToMod;
}

/**
 * Get the local peer
 * @return
 */
public Peer getLocalPeer() {
	return localPeer;
}

/**
 * Set the local peers songlist
 * @param songList
 */
public void setSongList(List<Song> songList) {
	// Local song data has been sent
	localPeer.setSongList(songList);

	// Update other peers with new songlist
	nc.sendNewSongList();
}
}
