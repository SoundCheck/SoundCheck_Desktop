package soundcheck.musicPlayer;

import soundcheck.shared.Const.Command;
import soundcheck.shared.Peer;
import soundcheck.shared.Zone;

public interface ServiceCallback {
	
	/**
	 * Notify the service of an action or change not effecting a particular zone
	 * @param cmd The command to issue to the service
	 * @param object The data to be dealt with by the service
	 */
	void notify(Command cmd, Object object);
	
	/**
	 * Notify the service of an action or change that effects a particular zone
	 * @param cmd The command to issue to the service
	 * @param object The data to be dealt with by the service
	 * @param zone The zone being effected by the action or change
	 */
	void notify(Command cmd, Object object, Zone zone);
	
	/**
	 * Notify the GUI of the current packets time stamp
	 * @param timeStamp The time stamp of the current packet
	 */
	void notifyGUISongProgress(long timeStamp);
	
	/**
	 * True if the stream is currently playing, false otherwise.
	 * @param isPlaying
	 */
	void isStreaming(boolean isPlaying);
	
	/**
	 * 
	 * @return
	 */
	Peer getLocalPeer();
}
