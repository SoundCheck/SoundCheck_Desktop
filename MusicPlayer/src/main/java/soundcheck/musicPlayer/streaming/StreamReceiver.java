package soundcheck.musicPlayer.streaming;

import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.musicPlayer.ServiceCallback;
import soundcheck.shared.Const.Command;
import soundcheck.shared.Const.PlayBack_State;
import soundcheck.shared.StreamInfo;

import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainer.Type;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;

public class StreamReceiver extends Stream implements Runnable{
	
	final static Logger logger = LoggerFactory.getLogger(StreamReceiver.class);
	
	private volatile PlayBack_State state = PlayBack_State.INIT;  //The current state of the stream
	
	private StreamInfo streamInfo;
	
	private volatile boolean alive = true;  //Whether to keep the thread the stream is running on alive
	
	private static SourceDataLine mLine;
	
	private final Object LOCK_OBJECT = new Object();
	
	private Thread streamThread;
	
	private ServiceCallback callback;
	
	public StreamReceiver (ServiceCallback callback) {
		this.callback = callback;
	}

	@Override
	public void run() {

		// format the url for the stream source location (Need to modify for unicast vs multicast)
		// may move to passing streaminfo to music player and forming url there
		String streamURL = "rtp://" + streamInfo.getIpAddress() + ":5005?multicast";

		playSong(streamURL);
		
		try {
			Thread.sleep(375);  
			/*Pausing to ensure the publisher has had time to close in the case of playing the same song twice.
			  When playing the same song twice, if the publisher for the first time has not closed, it will not start a
			  new stream publisher because the song ID will match one that is still playing.  375 chosen by testing different values
			  and this was the shortest.
			*/
		} catch (InterruptedException e) {
			//logger.error("",e);
			logger.debug("StreamReceiver thread interrupted on TEARDOWN");
		}
		
		
		//Only send the next command if the stream finished playing on its own
		if(state != PlayBack_State.TEARDOWN){
			callback.notify(Command.NEXT, streamInfo, streamInfo.getZone());
		}
		
		callback.notifyGUISongProgress(0);
				
		callback.isStreaming(false);		

	}

	@Override
	protected boolean setup(Object setupInfo) {
		
		boolean streamSetup = false;

		if(state == PlayBack_State.INIT){

			streamInfo = (StreamInfo) setupInfo;
			
			//Peer remotePeer = PeerCollection.getPeerByUID(streamInfo.getPeerUID());
			
			//StreamController.sendStreamPacket(remotePeer.getAddress(), streamInfo, Command.SETUP);

			streamSetup = true;
			
			state = PlayBack_State.SETUP;
			
		}

		return streamSetup;
	}

	@Override
	protected boolean play(Object playInfo) {

		boolean connectedToStream = false;

		if(state == PlayBack_State.INIT){

			streamInfo = (StreamInfo) playInfo;
			
			// TODO: Move acknowledgment to after Xuggler is started
			callback.notify( Command.STREAM_ACK, streamInfo, streamInfo.getZone() );
			
			streamThread = new Thread(this);
			
			streamThread.start();

			connectedToStream = true;

			state = PlayBack_State.PLAYING;
		}
		else if(state == PlayBack_State.PAUSED){
			//Send play request to Peer

			connectedToStream = true;
			
			state = PlayBack_State.PLAYING;
			
			synchronized(LOCK_OBJECT){
				LOCK_OBJECT.notify();
			}

		}
		return connectedToStream;
	}

	@Override
	protected boolean pause() {
		
		boolean streamPaused = false;

		if(state == PlayBack_State.PLAYING){

			streamPaused = true;

			state = PlayBack_State.PAUSED;
			callback.isStreaming(false);
		}
		
		return streamPaused;
	}

	@Override
	protected boolean teardown() {
		
		boolean streamClosed = false;
		
		//Send teardown request to Peer
		
		state = PlayBack_State.TEARDOWN;
		
		alive = false;
		
		callback.isStreaming(false);

		synchronized(LOCK_OBJECT){
			LOCK_OBJECT.notifyAll();
		}
		
		if(streamThread != null && streamThread.isAlive()){
			streamThread.interrupt();
		}
		
		return streamClosed;
	}

	@Override
	protected int getStreamSongID() {
		return streamInfo.getSongID();
	}
	
	/**
	 * The playSong method is responsible for opening a
	 * Xuggler container to play song at provided location.
	 * 
	 * @param songURL The location of the song to play (local file path or url)
	 */
	public void playSong(String songURL) {

		IContainer container = IContainer.make();

		IContainerFormat format = IContainerFormat.make();

		// Stream format must currently be mp3
		format.setInputFormat("mp3");
		
//		int s = container.setInputBufferLength(6270);
//		
//		if(s < 0){
//			logger.warn("Input buffer was not set to desired length");
//		}
		
		//Probe size value must be >50 for some reason. Native libraries throw an exception if it's <50. Measured in bytes.
		if(container.setProperty("probesize", 50) < 0){
			logger.warn("Probe size not set for input container.");
		}
		
		if(container.setProperty("analyzeduration", 1) < 0){
			logger.warn("Analyze duration not changed for input container.");
		}

		
		container.setFlag(IContainer.Flags.FLAG_NONBLOCK, true);
		
		if (container.open(songURL, Type.READ, format, true, false) < 0) {
			throw new IllegalArgumentException("stream not found");
		}

		int numStreams = container.getNumStreams();
		
		//long streamRec = System.currentTimeMillis();
		
		

		logger.info("Number of Audio streams detected {}", numStreams);

		IPacket packet = IPacket.make();
		IStream stream = null;
		IStreamCoder audioCoder = null;

		Map<Integer, IStreamCoder> knownStreams = new HashMap<Integer, IStreamCoder>();
		
		long previousValue = 0;
		
		while (container.readNextPacket(packet) >= 0 && alive) {
			
			if (packet.isComplete()) {

				if (knownStreams.get(packet.getStreamIndex()) == null) {
					container.queryStreamMetaData();  //This method tends to take awhile when reading a stream
					stream = container.getStream(packet.getStreamIndex());
					knownStreams.put(packet.getStreamIndex(),stream.getStreamCoder());
					
					audioCoder = knownStreams.get(packet.getStreamIndex());
					
					audioCoder.setTimeBase(stream.getTimeBase());
				}				

				if (!audioCoder.isOpen()) {
					if (audioCoder.open(null, null) < 0) {
						throw new RuntimeException("could not open audio decoder for container");
					}

					openSound(audioCoder);
					
					//System.out.println("Opening sound  " + (System.currentTimeMillis() - streamRec));
				}
				
				//System.err.println(audioCoder.getNumDroppedFrames());

				int offset = 0;

				IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());

				while (offset < packet.getSize() && alive) {
					
					//Wait until the state is playing
					while(state != PlayBack_State.PLAYING){

						if(state == PlayBack_State.TEARDOWN){
							break;
						}
						else{
							try{
								synchronized(LOCK_OBJECT){
									//mLine.drain();
									mLine.flush();
									mLine.stop();
									
									LOCK_OBJECT.wait();
									
									mLine.start();
								}
							}
							catch(InterruptedException e){
								logger.error("",e);
							}
						}
					}
					
					int bytesDecoded = audioCoder.decodeAudio(samples, packet,offset);

					if (bytesDecoded < 0) {
						logger.warn("Error occurred decoding audio");
						break;
						//throw new RuntimeException("got error decoding audio");
					}

					offset += bytesDecoded;

					if (samples.isComplete() && alive) {
						playJavaSound(samples);
					}
					
					// Send the time stamp to the GUI for updating the progress bar
					long newValue = (long)(packet.getTimeStamp() * packet.getTimeBase().getValue());
					
					// Update GUI every second that the stream is playing
					if(newValue > previousValue){
						callback.notifyGUISongProgress(newValue);
						callback.isStreaming(true);
						previousValue = newValue;
						
						if(newValue == streamInfo.getSongDuration()){
							alive = false;
						}
					}
				}

			}
		}

		closeJavaSound();

		if (audioCoder != null) {
			audioCoder.close();
			audioCoder = null;
		}
		if (container != null) {
			container.close();
			container = null;
		}

	}

	private static void openSound(IStreamCoder aAudioCoder) {

		/* xuggler defaults to signed 16 bit samples */
		AudioFormat audioFormat = new AudioFormat(aAudioCoder.getSampleRate(),
				(int) IAudioSamples.findSampleBitDepth(aAudioCoder
						.getSampleFormat()), aAudioCoder.getChannels(), true,
						false);

		DataLine.Info info = new DataLine.Info(SourceDataLine.class,
				audioFormat);

		try {
			mLine = (SourceDataLine) AudioSystem.getLine(info);
			/**
			 * if that succeeded, try opening the line.
			 */
			mLine.open(audioFormat);
			/**
			 * And if that succeed, start the line.
			 */
			mLine.start();
		} catch (LineUnavailableException e) {
			throw new RuntimeException("could not open audio line");
		}

	}

	private static void playJavaSound(IAudioSamples aSamples) {
		/**
		 * We're just going to dump all the samples into the line.
		 */
		byte[] rawBytes = aSamples.getData()
				.getByteArray(0, aSamples.getSize());
		mLine.write(rawBytes, 0, aSamples.getSize());
	}

	private static void closeJavaSound() {
		if (mLine != null) {
			/*
			 * Wait for the line to finish playing
			 */
			mLine.drain();
			/*
			 * Close the line.
			 */
			mLine.close();
			mLine = null;
		}
	}
}
