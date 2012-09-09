package soundcheck.shared;

import java.awt.Color;


/**
 * Constant variables that may be used throughout the program
 *
 */
public class Const {
	// Packet data
	public final static String PROJECT_NAME = "SoundCheck";
	
	// Services are for network use
	public enum Service {
		REQUEST_ZONE_MAP // Ask an existing peer for it's zone map
		,GET_ZONE_MAP // Initialize with another peer's zone map
		,DISCOVERY // Sent to ask for more data
		,PEER_DATA // Information on the local peer. Sent in response to discovery
		,PEER_EDIT // A peer has changed (name change, usually)
		,PEER_STATUS // A peer has changed privacy
		,PEER_ZONE // A peer's zone has changed
		,ZONE_MAPPING // A zone-name mapping
		,INTERPROCESS // Packet is for interprocess use. See commands
		,STREAMING // Packet is for streaming use
		,STREAMING_ACK	// Packet it for stream setup acknowledgments
		,SONGLIST // Songlist has been changed for a peer
		,QUEUE_CHANGE // Change is being made to zone's queue
		,UPDATE
	}
	
	// Commands are for interprocess use.
	public enum Command {
		// Zone management commands
		REQUEST_UPDATE // Request the most recent peer data
		,SET_PEERLIST // Send a list of known SoundCheck peers
		,SET_LOCAL_PEER // Send the local peer's data
		,GET_LOCAL_PEER	// Get the local peer's data
		,ZONE_MAP // Contains a new zone map
		,PEER_ZONE_CHANGE // A peer's zone has changed
		,PEER_STATUS_CHANGE // A peer's status has changed
		
		,SET_SONGLIST // Packet is for passing a peer's songlist
		,LIBRARY_UPDATE	// Request the most recent library
		
		,NEW_ZONE // A new zone has been created
		
		,CONFIG // Set configuration options
		
		,QUEUE_FRONT // add song to front of queue
		,QUEUE_BACK // add song to back of queue
		,QUEUE_REMOVE //Remove a song from the queue
		,NEW_QUEUE // Replace the contents of the current queue

		// Streaming commands
		,SETUP  //Create an audio stream
		,PLAY  //Play the audio stream
		,PAUSE //Pause the audio stream
		,NEXT //Skip to the next audio stream
		,TEARDOWN  //Stop the audio stream
		
		,STREAM_ACK		// acknowledgment that stream is setup

		,GET_ZONE_LIST  //Indicates that the list of zones should be returned
		,GET_ZONE_INFO  //Indicates that a zone's playlist should be returned

		,PASSWORD_SET	// Indicates that the user has requested a password be assigned to the selected zone
	}

	//Stream state related information
	public enum PlayBack_State {INIT, PLAYING, SETUP, PAUSED, TEARDOWN};

	//Type of Stream to create
	public enum StreamType {RECEIVE, PUBLISH};

	// Broadcast Discovery constants.
	public final static String DISCOVER_CLUSTER = "SoundCheckDiscovery";

	// Interprocess constants
	public final static int INTERFACE_PORT = 9998;
	
	// External connection constants
	public final static int EXTERNAL_PORT = 9997;
	
	//Address for StreamPublishers to begin trying to stream on
	public final static String STARTING_MCAST_ADDRESS = "226.0.0.0";
	
	// Colors
	public final static Color bgColor = new Color(61,58,57);
	public final static Color txtColor = Color.white;
	public final static Color highlightColor = new Color(214,214,214);
	public final static Color mnuColor1 = new Color(200, 200, 200);
	public final static Color mnuColor2 = new Color(70, 70, 70);
	public final static Color mnuTxtColor = Color.black;
	public final static Color listColor1 = new Color(83,83,83);
	public final static Color listColor2 = new Color(76,76,76);
}
