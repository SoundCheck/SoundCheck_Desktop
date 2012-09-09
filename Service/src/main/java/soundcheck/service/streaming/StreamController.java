package soundcheck.service.streaming;

import java.util.ArrayList;
import java.util.List;
import org.jgroups.Address;
import org.jgroups.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.service.data.PeerCollection;
import soundcheck.service.interprocess.MusicPlayerConnection;
import soundcheck.service.network.NetworkChannel;
import soundcheck.shared.DataPacket;
import soundcheck.shared.PacketCreator;
import soundcheck.shared.Const.Command;
import soundcheck.shared.Const.StreamType;
import soundcheck.shared.Peer;
import soundcheck.shared.Song;
import soundcheck.shared.StreamInfo;
import soundcheck.shared.Zone;
import soundcheck.shared.ZoneProperties;

/**
 * 
 * Handles control of publishing and receiving streams.
 * 
 *
 */
public class StreamController {

	final static Logger logger = LoggerFactory.getLogger(StreamController.class);  //Logger for the class

	public final static int DEFAULT_NUMBER_OF_ALLOWED_STREAMS = 6;  //Number of streams that can be active at once by default

	private static List<Stream> streamPool = new ArrayList<Stream>(DEFAULT_NUMBER_OF_ALLOWED_STREAMS); //List of Streaming connections

	private static int NUMBER_OF_ALLOWED_STREAMS = 6; //Number of concurrent streams the user has set

	public final static int MAX_NUMBER_OF_STREAMS = 10;  //The maximum number of concurrent streams

	/**
	 * Get necessary data from packet and pass it on to be processed.
	 * @param msg
	 */
	public static void receive(Message msg) {
		logger.debug("Received SoundCheck stream control request from host {}.", msg.getSrc().toString());

		DataPacket packet = (DataPacket) msg.getObject();

		StreamInfo strInfo = (StreamInfo) packet.getData();

		strInfo.setZone(packet.getZone());
		strInfo.setAddr(msg.getSrc());

		if (strInfo.getStreamType() == StreamType.PUBLISH) {
			
			if(packet.getCommand() == Command.NEXT){
				nextSong(strInfo.getZone());
				packet.setCommand(Command.PLAY); //Changing to a play command so the next song is played by the service instead of skipping to the next song again when this packet is received.
			}

			if(packet.getCommand() == Command.PLAY){
				addStream(strInfo);
			}

			Stream stream = getStream(strInfo);

			if(stream != null){
				if(PeerCollection.isLocalZone(packet.getZone()) && packet.getCommand() != Command.PLAY){  //Play command will already have been sent by Publisher, do not need to send twice.
					MusicPlayerConnection.sendData(PacketCreator.createInterprocessPacket(packet.getCommand(), strInfo, packet.getZone()));
				}
				stream.processCommand(packet.getCommand(), strInfo);
			}
			//			else{
			//				createStreamInfo(packet.getZone(), packet.getCommand());
			//			}

		} 
		else if (strInfo.getStreamType() == StreamType.RECEIVE) {
			MusicPlayerConnection.sendData(PacketCreator.createInterprocessPacket(packet.getCommand(), strInfo, packet.getZone()));
		}
		else {
			logger.warn("Unrecognized streaming command received.");
		}

	}
	
	private static Song nextSong(Zone zone){
		ZoneProperties zoneProp = PeerCollection.getZoneProps(zone);
		
		Song currentSong = zoneProp.getCurrent();
		
		NetworkChannel.send(null, PacketCreator.createNewQueuePacket(Command.QUEUE_REMOVE, currentSong, zone));
		
		try {
			Thread.sleep(10);  //Pausing to make sure the playlist is updated before we get the current song
		} catch (InterruptedException e) {
			logger.error("",e);
		}
		
		currentSong = zoneProp.getCurrent();

		MusicPlayerConnection.sendData(PacketCreator.createInterprocessPacket(Command.TEARDOWN, null));
		
		return currentSong;
	}

	/**
	 * 
	 * Sends the streaming packet over the network channel to the cluster 
	 * 
	 * @param addr - The address to send the packet to
	 * @param streamInfo - Information related to the stream.  May be song info, IP address
	 * @param streamCmd - Command to execute on the stream
	 */
	public static void sendStreamPacket(Address addr, Object streamInfo, Command streamCmd, Zone zone){
		NetworkChannel.send(addr, PacketCreator.createStreamCommandPacket(streamCmd, streamInfo, zone));
	}


	/**
	 * Adds a new stream to the stream pool.
	 * @param type - The type of Stream to be added.
	 * @return - boolean that indicates whether the Stream was added.
	 */
	public synchronized static boolean addStream(StreamInfo strInfo){

		boolean connectionAdded = false;

		if (streamPool.size() < NUMBER_OF_ALLOWED_STREAMS) {

			Stream stream = null;

			stream = new StreamPublisher(strInfo);

			if (StreamController.getStream(strInfo) == null) {
				streamPool.add(stream);

				stream.processCommand(Command.SETUP, strInfo);

				connectionAdded = true;
				logger.trace("Stream was added to the pool");
			}
			else{
				logger.trace("Stream already exists, not adding a new one.");
			}



		} else {
			logger.warn("Stream was not added to the pool because max size has been reached.");
		}


		return connectionAdded;
	}

	/**
	 * Removes a stream from the stream pool.
	 * @param streamToBeRemoved - The Stream to be removed from the pool.
	 * @return - boolean that indicates whether the Stream was removed.
	 */
	public synchronized static boolean removeStream(Stream streamToBeRemoved){

		boolean connectionRemoved = false;

		connectionRemoved = streamPool.remove(streamToBeRemoved);

		return connectionRemoved;
	}

	/**
	 * Sets the number of allowed streams
	 * @param streamPoolSize - The new size of the stream pool.
	 * @return - boolean that indicates whether the new size was set.
	 */
	public synchronized static boolean setStreamPoolSize(int streamPoolSize){

		boolean newSizeSet = false;

		if(streamPoolSize > 0 && streamPoolSize <= MAX_NUMBER_OF_STREAMS){

			NUMBER_OF_ALLOWED_STREAMS = streamPoolSize;

			newSizeSet = true;

		}

		return newSizeSet;
	}


	/**
	 * Gets the specified Stream from the stream pool.
	 * @param stream
	 * @return - The Stream requested.
	 */
	public synchronized static Stream getStream(Object stream) {

		StreamInfo strInfo = (StreamInfo) stream;

		Stream retStream = null;

		for(Stream s : streamPool){
			if(s.getStreamSongID() == strInfo.getSongID()){
				retStream = s;
				break;
			}
		}

		if(retStream == null){
			logger.warn("Stream not found in the list.");
		}


		return retStream;

	}

	/**
	 * Creates the StreamInfo object and sends it to the Peer who is to publish
	 * the requested stream.
	 * 
	 * @param zone - The Zone the stream is to be played for
	 * @param cmd - The Command to be executed on that stream
	 */
	public static void createStreamInfo(Zone zone, Command cmd) {

		ZoneProperties zoneProp = PeerCollection.getZoneProps(zone);

		Song songToPlay = null;

		if (cmd.equals(Command.NEXT)) {
			songToPlay = nextSong(zone);

			if(songToPlay != null){

				cmd = Command.PLAY; //Changing to a play command so the next song is played by the service instead of skipping to the next song again when this packet is received.
			}
			else{
				logger.debug("Only 1 song in the playlist, ignoring NEXT command.");
				return;
			}

		} else {
			songToPlay = zoneProp.getCurrent();
		}

		if(( songToPlay != null ) && ( songToPlay.getFilePath() == null )) {	// song in queue has no location
			// find song in library
			songToPlay = locateSong( songToPlay );
		}

		if (songToPlay != null) {	// song in queue and library

			Peer peer = identifySongSource(songToPlay);

			// create stream info and send to peer with song
			StreamInfo strInfo = new StreamInfo(null, songToPlay.getId(), peer.getUid(), songToPlay.getDuration(), StreamType.PUBLISH);

			if ( cmd != Command.PLAY ) {

				for( Peer zonePeer : PeerCollection.getPeers() ) {
					if( zonePeer.getZone().getUid().equals(zone.getUid())) {
						if( peer.equals(zonePeer)) {
							strInfo.setStreamType(StreamType.PUBLISH);
							sendStreamPacket(zonePeer.getAddress(), strInfo, cmd, zone);
						}
						strInfo.setStreamType(StreamType.RECEIVE);
						sendStreamPacket(zonePeer.getAddress(), strInfo, cmd, zone);
					}
				}
			} else {
				sendStreamPacket(peer.getAddress(), strInfo, cmd, zone);
			}
		} else {
			// song not in library, try next song
			createStreamInfo(zone, Command.NEXT);
		}
	}

	/**
	 * Identifies the peer that possesses the selected song stored and can publish
	 * the stream.
	 * 
	 * @param song the song that has been requested to play
	 * @return the peer that possesses the song
	 */
	public static Peer identifySongSource( Song song) {

		int id = song.getId();

		for ( Peer peer : PeerCollection.getPeers() ) {

			// check if the peers song of that id equals the requested song
			if ( peer.getSongByID(id).equals(song) ) {
				return peer;
			}
		}

		Song s = locateSong( song );
		return identifySongSource(s);
	}

	/**
	 * 
	 * @param song
	 * @return
	 */
	public static Song locateSong( Song song ) {

		Song retSong = null;

		for ( Peer peer : PeerCollection.getPeers() ) {

			int index = peer.getSongList().indexOf( song );

			if( index >= 0 ) {
				retSong = peer.getSongList().get(index);
			}
		}

		return retSong;
	}

	/**
	 * 
	 * @param msg
	 */
	public static void ackStream( Message msg ) {
		logger.debug("Received SoundCheck stream acknowledgment from receiver {}.", msg.getSrc().toString());

		DataPacket packet = (DataPacket) msg.getObject();

		StreamInfo strInfo = (StreamInfo) packet.getData();

		Stream stream = getStream(strInfo);

		stream.receiverReady();
	}
}

