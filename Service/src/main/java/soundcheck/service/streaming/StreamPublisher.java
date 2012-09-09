package soundcheck.service.streaming;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IContainer.Type;

import soundcheck.service.data.PeerCollection;
import soundcheck.service.network.NetworkChannel;
import soundcheck.shared.Const;
import soundcheck.shared.Const.Command;
import soundcheck.shared.Const.PlayBack_State;
import soundcheck.shared.Const.StreamType;
import soundcheck.shared.PacketCreator;
import soundcheck.shared.Peer;
import soundcheck.shared.Song;
import soundcheck.shared.StreamInfo;

public class StreamPublisher extends Stream implements Runnable{


	final static Logger logger = LoggerFactory.getLogger(StreamPublisher.class);

	private volatile PlayBack_State state = PlayBack_State.INIT; //The current state of the stream

	private volatile boolean alive = true;  //Whether to keep the thread the stream is running on alive

	private StreamInfo streamInfo;

	private volatile Object LOCK_OBJECT = new Object();

	private int numDesktopReceivers = 0;
	private int acksReceived = 0;


	public StreamPublisher(StreamInfo strInfo) {
		streamInfo = strInfo;
	}

	@Override
	public void run() {
		startStream();
		
		StreamController.removeStream(this);
	}

	/**
	 * Implementation of setup from the Stream class.
	 * Prepares a stream to be received by other peers.
	 * 
	 * @param setupInfo - Information regarding how the stream should be set up
	 * 
	 * @return boolean - Indicates whether setup completed successfully.
	 */
	@Override
	protected boolean setup(Object setupInfo) {
		boolean setupFinished = false;

		if(state == PlayBack_State.INIT){

			Peer localPeer = PeerCollection.getPeerByUID(streamInfo.getPeerUID());

			Song songToStream = localPeer.getSongByID(streamInfo.getSongID()); 

			if(songToStream.getFilePath() != null){  //If filePath is null, an incorrect song ID was passed

				String ipAddr = findOpenIPAddress();

				streamInfo.setIpAddress(ipAddr);

				streamInfo.setLocalFilePath(songToStream.getFilePath());

				StreamInfo responseInfo = new StreamInfo(ipAddr, streamInfo.getSongID(), null, songToStream.getDuration(), StreamType.RECEIVE);

				List<Peer> peerList = PeerCollection.getPeers();

				for(Peer p : peerList){
					if(p.getZone().getUid().equals(streamInfo.getZone().getUid())){

						if( !p.isExternal() ) {
							numDesktopReceivers++;
						}
						
						NetworkChannel.send(p.getAddress(), PacketCreator.createStreamCommandPacket(Command.PLAY, responseInfo, streamInfo.getZone()));
					}
				}

				setupFinished = true;

				state = PlayBack_State.SETUP;
			}
			else{
				logger.warn("Requested song was not found in this peer's list");
			}

		}
		return setupFinished;
	}


	/**
	 * Begins playing the stream.
	 * 
	 * @param playInfo - Additional information that is required to play the stream
	 */
	@Override
	protected boolean play(Object playInfo) {
		boolean streamPlaying = false;
		
		if(state == PlayBack_State.PAUSED){

			streamPlaying = true;

			StreamInfo responseInfo = new StreamInfo("", streamInfo.getSongID(), null, 0, StreamType.RECEIVE);

			List<Peer> peerList = PeerCollection.getPeers();

			for(Peer p : peerList){
				if(p.getZone().getUid().equals(streamInfo.getZone().getUid())){
					NetworkChannel.send(p.getAddress(), PacketCreator.createStreamCommandPacket(Command.PLAY, responseInfo, streamInfo.getZone()));
				}
			}

			synchronized(LOCK_OBJECT){
				LOCK_OBJECT.notifyAll();
			}
		}
		
		state = PlayBack_State.PLAYING;

		return streamPlaying;
	}

	/**
	 * Pauses the stream.
	 */
	@Override
	protected boolean pause() {
		boolean streamPaused = false;

		if(state == PlayBack_State.PLAYING){

			streamPaused = true;

			state = PlayBack_State.PAUSED;

		}

		return streamPaused;
	}

	/**
	 * Stops the stream and cleans up resources associated with the stream.
	 * Sends a teardown command to all listeners indicating that the stream is ending.
	 */
	@Override
	protected boolean teardown() {
		boolean streamToreDown = false;

		state = PlayBack_State.TEARDOWN;

		alive = false;

		synchronized(LOCK_OBJECT){
			LOCK_OBJECT.notifyAll();
		}
		
		List<Peer> peerList = PeerCollection.getPeers();
		
		Peer localPeer = PeerCollection.getPeerByUID(streamInfo.getPeerUID());

		Song songToStop = localPeer.getSongByID(streamInfo.getSongID()); 

		for(Peer p : peerList){
			if(p.getZone().getUid().equals(streamInfo.getZone().getUid())){
				NetworkChannel.send(p.getAddress(), PacketCreator.createNewQueuePacket(Command.QUEUE_REMOVE, songToStop, streamInfo.getZone()));
			}
		}

		return streamToreDown;
	}

	/**
	 * Returns the ID of the song this stream is setup to stream.
	 */
	@Override
	protected int getStreamSongID() {
		return streamInfo.getSongID();
	}

	/**
	 * Finds an open multicast address.
	 * Starts with Const.STARTING_MCAST_ADDRESS
	 * 
	 * @return - The first open multicast address
	 */
	private String findOpenIPAddress() {
		String ipAddr = Const.STARTING_MCAST_ADDRESS;

		boolean openAddressFound = false;
		
		Timer socketTimer = new Timer();

		try {
			
			while (!openAddressFound) {
				
				socketTimer = new Timer();
				
				MulticastSocket mCastSocket = new MulticastSocket(5005);

				mCastSocket.joinGroup(InetAddress.getByName(ipAddr));

				DatagramPacket inPacket = new DatagramPacket(new byte[627], 627);

				//Wait for 1/2 second and then interrupt the socket
				socketTimer.schedule(new SocketCheck(mCastSocket), 500);

				mCastSocket.receive(inPacket); //This call blocks, will be interrupted by the timer task if data is not received in 2 seconds.

				socketTimer.cancel();  //Cancel the timer

				//If we get here, it means that the current address is being used by someone else
				//mCastSocket.leaveGroup(InetAddress.getByName(ipAddr)); //Leave the group
				
				mCastSocket.close();

				ipAddr = determineNewAddress(ipAddr);  //Find a new address to try

			}


		} 
		catch(SocketException e){
			logger.trace("SocketException caught.  New address for publishing found! {}", ipAddr);

			socketTimer.cancel();  //Cancel the timer task so the thread dies 

			return ipAddr;  //Return the current value of ipAddr which is a multicast address that is not being used.

		}
		catch (IOException e) {
			logger.error("",e);
		}




		return ipAddr;
	}

	/**
	 * 
	 * Finds the next sequential address to the one passed in.
	 * 
	 * @param ipAddr
	 * @return
	 * 
	 * NOTE: This will only work for IPv4 address
	 * 
	 */
	private String determineNewAddress(String ipAddr) {

		// The last place in the address
		int first = Integer.parseInt(ipAddr.substring(ipAddr.lastIndexOf('.') + 1));

		StringBuilder strBld = new StringBuilder(ipAddr);

		//
		// if(first == 255){
		//
		// This will have to implemented if we run into problems.
		// Hopefully we never have to generate this many addresses.
		//
		// }

		first++; // Increment the value

		strBld.delete(strBld.lastIndexOf(".") + 1, strBld.length()); //Remove the current last place digits.

		strBld.append(first);  //Append on the new last place digits.

		return strBld.toString();
	}

	/**
	 * Inner class that is used to cause a timeout on a multicast socket.
	 * 
	 * The TimerTask is the action that is to be executed by a Timer.  The Timer runs on its own
	 * thread and calls the TimerTask's run() method after each time interval the TimerTask was
	 * scheduled for.  The action is defined in the run() method.  
	 * The run() method will fire every time period until it is canceled.
	 * 
	 * EXAMPLE
	 * 		socketTimer.schedule(new SocketCheck(mCastSocket), 10000);
	 * 
	 * The run method of the SocketCheck object will run every 10000ms (10s) until the Timer is canceled.
	 * 
	 */
	private class SocketCheck extends TimerTask{

		private MulticastSocket mSocket;  //The socket to be used.

		/**
		 * Constructor for SocketCheck.  Initializes the socket to be checked.
		 * @param mCastSocket
		 */
		public SocketCheck(MulticastSocket mCastSocket) {
			this.mSocket = mCastSocket;
		}

		/**
		 * The action to be performed when scheduled time is reached.
		 */
		@Override
		public void run() {
			mSocket.close();  //Close the socket.
		}

	}

	public void startStream() {
		
		// list for holding multiple containers to allow hardware integration
		List<IContainer> wContainers = new ArrayList<IContainer>();
		
		// ttl needs to be added to allow packets to make more hops or they will not reach destination
		// 12 was chosen based on online example, value may need to be adjusted in final product
		// TODO: test what ttl value should be
		String streamAddress = "rtp://" + streamInfo.getIpAddress() + ":5005?ttl=12";

		IContainerFormat format = IContainerFormat.make();
		
		// only mp3 audio can currently be streamed
		format.setOutputFormat("mp3", streamAddress, "audio/mpeg");

		// create container for streaming mulitcast to desktop
		IContainer multicastContainer = IContainer.make();
		
		if(multicastContainer.open(streamAddress, Type.WRITE, format, true, false) < 0){
			throw new RuntimeException("Opening write container failed");
		}
		
		// add standard desktop multicast streamer
		wContainers.add( multicastContainer );
			
		List<Peer> peerList = PeerCollection.getPeers();

		for(Peer p : peerList){
			if(p.getZone().getUid().equals(streamInfo.getZone().getUid())){
				
				if( p.isExternal() ) {
					IContainer externalContainer = IContainer.make();
					
					streamAddress = "rtp://" + p.getIp() + ":5005";
					
					if( externalContainer.open(streamAddress, Type.WRITE, format, true, false ) <0){
						throw new RuntimeException("Opening write container failed");
					}
					
					wContainers.add( externalContainer );
				}
			}
		}
		
		
		
		IContainer rContainer = IContainer.make();

		if(rContainer.open(streamInfo.getLocalFilePath(), Type.READ, null) < 0){
			throw new RuntimeException("Opening read container failed");
		}
		
		IStreamCoder rCoder = rContainer.getStream(0).getStreamCoder();
		
		rCoder.setTimeBase(rContainer.getStream(0).getTimeBase());
		
		
		// hold the stream coder for each write container
		List<IStreamCoder> wCoders = new ArrayList<IStreamCoder>();
		
		for( IContainer wContainer : wContainers ) {	// iterate through write containers creating/configuring stream coders
			
			IStream wStream = wContainer.addNewStream( rCoder.getCodec() );

			IStreamCoder wCoder = wStream.getStreamCoder();

			//The codec and sample rate are required to write audio
			//wCoder.setCodec(rCoder.getCodec());
			wCoder.setSampleRate(rCoder.getSampleRate());
			wCoder.setAutomaticallyStampPacketsForStream(true);
			wCoder.setBitRate(rCoder.getBitRate());
			wCoder.setTimeBase(rCoder.getTimeBase());
			
			wCoders.add(wCoder);
		}

		// open all coders
		for( IStreamCoder wCoder : wCoders ) {
			wCoder.open(null, null);
		}
		rCoder.open(null, null);

		IPacket rPacket = IPacket.make();

		// write the header to each address
		for( IContainer wContainer : wContainers ) {
			wContainer.writeHeader();
		}

		while(rContainer.readNextPacket(rPacket) >= 0 && alive){

			while(state != PlayBack_State.PLAYING){
				if(state == PlayBack_State.TEARDOWN){
					break;
				}
				else{
					try {
						synchronized(LOCK_OBJECT){
							LOCK_OBJECT.wait();
						}
					} catch (InterruptedException e) {
						logger.warn("",e);
					}
				}
			}
			
			// write the packet to each address
			for( IContainer wContainer : wContainers ) {
				wContainer.writePacket(rPacket);
			}

			long packetDuration = rPacket.getDuration();  //Packet duration in timeBase units

			IRational timeBase = rPacket.getTimeBase();  //Get the time base to calculate to duration

			double denomMS = timeBase.getDenominator() / 1000.0;  //Rational is 1/(packetLength in microSeconds)  Converting to milliseconds

			double time = packetDuration / denomMS;	// Packet duration in milliseconds

			long milli = (long) (time);	// number of full milliseconds

			int nano = (int) (( time - milli ) * 1E6);	// number of additional nanoseconds

			try{
				// sleep for duration of the packet to allow playback on receiving end
				Thread.sleep( milli, nano );
			}
			catch(InterruptedException e){
				e.printStackTrace();
			}

		}

		//Write a trailer to indicate the stream is complete
		for( IContainer wContainer : wContainers ) {
			wContainer.writeTrailer();
		}

		//Close containers and stream coders
		for( IStreamCoder wCoder : wCoders ) {
			wCoder.close();
		}
		rCoder.close();

		for( IContainer wContainer : wContainers ) {
			wContainer.close();
		}
		rContainer.close();

	}

	/*
	 * (non-Javadoc)
	 * @see soundcheck.service.streaming.Stream#receiverReady()
	 */
	@Override
	public void receiverReady() {

		acksReceived++;

		if( numDesktopReceivers == acksReceived ) {
			new Thread(this).start();
		}
	}
}
