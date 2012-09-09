package soundcheck.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jgroups.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Peer implements Serializable {	
	private static final long serialVersionUID = 9171772658877688960L;
	final static Logger logger = LoggerFactory.getLogger(Peer.class);

	private String uid;
	private transient Address address; // Determined by receiver (not serializable)
	private String name;
	private Zone zone;
	private String ip; // Determined by receiver (no easy way to determine network interface being used to find own IP)
	private List<Song> songList = new ArrayList<Song>();
	private boolean status = true;
	
	private boolean isExternal = false;

	/**
	 * Raw constructor with no arguments
	 */
	public Peer(String name) {
		this(null, name);
	}
	
	/**
	 * Basic constructor. address and ip don't need to be known for sending
	 * Peers across a network, as that info can be retrieved by the
	 * receiver.
	 * @param uid Unique identifier. String version of address.
	 * @param name User chosen name for their peer
	 */
	public Peer(String uid, String name) {
		this.uid = uid;
		this.name = name;
		this.zone = new Zone(); // Zone names are handled by GUI
	}

	/**
	 * Check if peers are equal based on their uid.
	 * @param peer
	 * @return
	 */
	@Override
	public boolean equals(Object peer) {
		boolean retVal = false;

		if( peer instanceof Peer) {
			if( this.uid.equals(( (Peer)peer ).uid) ) {
				retVal = true;
			}
		}

		return retVal;
	}

	/**
	 * @return the uid
	 */
	public String getUid() {
		return uid;
	}

	/**
	 * @param uid the uid to set
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the ipAddress
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

	/**
	 * @return the JGroups address
	 */
	public Address getAddress() {
		if( address == null) {
			logger.warn("Address is null. Note that address is not serializable" +
					" so it is only known by the class receiving a peer through a JChannel.");
		}
		return address;
	}

	/**
	 * @param address the JGroups address to set
	 */
	public void setAddress(Address address) {
		this.address = address;
	}

	/**
	 * String representation of the peer
	 */
	@Override
	public String toString() {
		return getName();
	}

	/**
	 * Override hashCode because equals is being overridden.
	 */
	@Override
	public int hashCode() {
		return uid.hashCode();
	}
	

	/**
	 * Returns the song with the ID passed in
	 * @param songID
	 * @return
	 */
	public Song getSongByID(int songID) {
		
		Song song = null;
		
		for(Song s : songList){
			if(s.getId() == songID){
				song = s;
				break;
			}
		}
		
		//Should not happen
		if(song == null){
			song = new Song();
		}
		
		return song;
	}

	/**
	 * @return the zone
	 */
	public Zone getZone() {
		return zone;
	}

	/**
	 * @param zone the zone to set
	 */
	public void setZone(Zone zone) {
		this.zone = zone;
	}
	
	/**
	 * @return the song list
	 */
	public List<Song> getSongList() {
		return songList;
	}
	
	/**
	 * @param songList the songList to set
	 */
	public void setSongList( List<Song> songList ) {
		this.songList = songList;
	}
	
	/**
	 * @return the peer's status
	 */
	public boolean getStatus() {
		return status;
	}
	
	/**
	 * @param status the status to set
	 */
	public void setStatus( boolean status ) {
		this.status = status;
	}

	/**
	 * @return the isExternal
	 */
	public boolean isExternal() {
		return isExternal;
	}

	/**
	 * @param isExternal the isExternal to set
	 */
	public void setExternal(boolean isExternal) {
		this.isExternal = isExternal;
	}
}
