package soundcheck.shared;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class ZoneProperties implements Serializable {
	private static final long serialVersionUID = 2291858576711436331L;

	private String zoneName;
	private final LinkedList<Song> playList = new LinkedList<Song>();
	private String password;

	public ZoneProperties(String zoneName) {
		this.zoneName = zoneName;
	}

	/**
	 * Appends the specified element to the end of the queue
	 * @param song Song to add to back of queue
	 */
	public void addLast(Song song) {
		playList.addLast(song);
	}

	/**
	 * Replaces the song in front of the queue with the given song
	 * @param song Song to replace the front of the queue
	 */
	public void addFirst(Song song) {
		if( playList.size() > 0) {
			
			if(playList.contains(song)){
				playList.remove(song);
			}
		}
		
		playList.addFirst(song);
	}

	/**
	 * Returns the song at the head of the queue
	 * @return
	 */
	public Song getCurrent() {
		
		Song currentSong = null;
		
		//Make sure there is actually an element to get.
		if(playList.size() > 0){
			currentSong = playList.getFirst();
		}
		
		return currentSong;
	}

	/**
	 * Removes the head of the queue and returns the
	 * next song in the list
	 * @return
	 */
	public Song nextSong() {
		
		if(playList.size() > 0){
			playList.removeFirst();
		}
		
		return getCurrent();
	}

	@Override
	public int hashCode() {
		int hashName = zoneName != null ? zoneName.hashCode() : 0;
		int hashPlayList = playList != null ? playList.hashCode() : 0;

		return (hashName + hashPlayList) * hashPlayList + hashName;
	}

	/**
	 * Checks if pairs are equal to eachother.
	 * The zoneProp must equal the second pairs zoneProp, and
	 * the zone must equal the second pairs zone. If fields are
	 * null in both pairs, they are also determined to be equivelent.
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof ZoneProperties) {
			ZoneProperties otherProp = (ZoneProperties) other;
			return 
					((  this.zoneName == otherProp.zoneName ||
					( this.zoneName != null && otherProp.zoneName != null &&
					this.zoneName.equals(otherProp.zoneName))) &&
					(      this.playList == otherProp.playList ||
					( this.playList != null && otherProp.playList != null &&
					this.playList.equals(otherProp.playList))) );
		}

		return false;
	}

	@Override
	public String toString()
	{ 
		return "(" + zoneName + ", " + playList + ")"; 
	}

	/**
	 * @return the zoneName
	 */
	public String getZoneName() {
		return zoneName;
	}

	/**
	 * @param zoneName the zoneName to set
	 */
	public void setZoneName(String zoneName) {
		this.zoneName = zoneName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * 
	 * @return - List<Song> This zone's playlist
	 */
	public List<Song> getPlayList() {
		if(playList != null) {
			return playList;
		} else {
			return new LinkedList<Song>();
		}
	}
	
	/**
	 * Replace the current playlist
	 * @param playList
	 */
	public void setPlayList(List<Song> newPlayList) {
		playList.clear();
		
		// Add the list in reverse order since the playlist
		// is displayed bottom to top (So, in effect, reverse
		// order is the correct order)
		for(int i = newPlayList.size()-1; i >= 0; --i) {
			playList.add(newPlayList.get(i));
		}
	}

	/**
	 * Removes a song from the playList
	 * @param songToRemove - The song to remove
	 */
	public void removeSong(Song songToRemove) {
		if(playList.contains(songToRemove)){
			playList.remove(songToRemove);
		}
	}
}
