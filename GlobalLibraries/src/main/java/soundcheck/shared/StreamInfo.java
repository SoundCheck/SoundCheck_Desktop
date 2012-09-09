package soundcheck.shared;

import java.io.Serializable;

import org.jgroups.Address;

import soundcheck.shared.Const.StreamType;

public class StreamInfo implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2458541938062862870L;

	private String ipAddress;
	
	private int songID;
	
	private String peerUID;
	
	private StreamType streamType;
	
	private String localFilePath;
	
	private Zone zone;
	
	private Address addr;
	
	private long songDuration;
	
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
	 * @return the streamType
	 */
	public StreamType getStreamType() {
		return streamType;
	}
	
	/**
	 * 
	 * @param streamType
	 */
	public void setStreamType( StreamType streamType ) {
		this.streamType = streamType;
	}


	public StreamInfo(String ipAddress, int songID, String peerUID, long duration, StreamType streamType){
		this.ipAddress = ipAddress;
		
		this.songID = songID;
		
		this.peerUID = peerUID;
		
		this.streamType = streamType;
		
		this.songDuration = duration;
	}

	/**
	 * @return the ipAddress
	 */
	public String getIpAddress() {
		return ipAddress;
	}


	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}


	/**
	 * @return the songID
	 */
	public int getSongID() {
		return songID;
	}


	/**
	 * @return the peerUID
	 */
	public String getPeerUID() {
		return peerUID;
	}


	/**
	 * @return the localFilePath
	 */
	public String getLocalFilePath() {
		return localFilePath;
	}


	/**
	 * @param localFilePath the localFilePath to set
	 */
	public void setLocalFilePath(String localFilePath) {
		this.localFilePath = localFilePath;
	}


	/**
	 * @return the addr
	 */
	public Address getAddr() {
		return addr;
	}


	/**
	 * @param addr the addr to set
	 */
	public void setAddr(Address addr) {
		this.addr = addr;
	}


	/**
	 * @return the songDuration
	 */
	public long getSongDuration() {
		return songDuration;
	}


	/**
	 * @param songDuration the songDuration to set
	 */
	public void setSongDuration(long songDuration) {
		this.songDuration = songDuration;
	}
	
	

}
