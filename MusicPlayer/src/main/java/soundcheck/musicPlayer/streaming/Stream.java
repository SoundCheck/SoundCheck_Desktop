package soundcheck.musicPlayer.streaming;

import soundcheck.shared.Const.Command;

/**
 * Superclass for Stream objects.
 * 
 *
 */
public abstract class Stream {	
	
	/**
	 * Process the command.  Extending classes will call this method and then call their own implementation of each command method.  Ex - play(), setup()
	 * 
	 * @param cmd - The command to process
	 * @param streamInfo - Info required by the Stream to process the command.
	 */
	public void processCommand(Command cmd, Object streamInfo) {

		switch(cmd){

		case SETUP:
			setup(streamInfo);
			break;

		case PLAY:
			boolean complete = play(streamInfo);
			if(complete){
				
			}
			break;

		case PAUSE:
			pause();
			break;

		case TEARDOWN:
			teardown();
			break;

		default:
			break;
		}
	}
	
	/**
	 * Setup the stream based on the info passed in.  Extending classes must implement this  method.
	 * 
	 * @param streamInfo - Info required to set up the stream
	 * @return - Whether the Stream was setup successfully
	 */
	protected abstract boolean setup(Object setupInfo);
	
	/**
	 * Begin playing the stream.  Extending classes must implement this method.
	 * 
	 * @return - Whether the stream was started
	 */
	protected abstract boolean play(Object playInfo);
	
	/**
	 * Pause the stream.   Extending classes must implement this method.
	 * 
	 * @return - Whether the stream was paused.
	 */
	protected abstract boolean pause();
	
	/**
	 * Teardown/close the stream.   Extending classes must implement this method.
	 * 
	 * @return - Whether the stream was closed.
	 */
	protected abstract boolean teardown();
	
	/**
	 * 
	 * @return - int The id of the song that the stream is streaming
	 */
	protected abstract int getStreamSongID();
	
	
}
