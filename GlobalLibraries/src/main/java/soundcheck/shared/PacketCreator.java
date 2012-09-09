package soundcheck.shared;

import java.util.Map;

import soundcheck.shared.Const.Command;
import soundcheck.shared.Const.Service;

/**
 * A list of predetermined packet structures for various purposes such
 * as discovery and stream control.
 *
 */
public final class PacketCreator {
	
	public static final DataPacket createRequestZoneMapPacket() {
		return new DataPacket(Service.REQUEST_ZONE_MAP);
	}
	
	public static final DataPacket createRespondZoneMapPacket(Map<Zone, ZoneProperties> zoneMap) {
		return new DataPacket(Service.GET_ZONE_MAP, null, zoneMap);
	}

	/**
	 * Packet for peer discovery.
	 * @return
	 */
	public static final DataPacket createDiscoveryPacket() {
		return new DataPacket(Service.DISCOVERY);
	}
	
	/**
	 * Packet for peer data transfer
	 * @param data Data about this peer to be sent to other peers.
	 * @return
	 */
	public static final DataPacket createPeerDataPacket(Object data) {
		return new DataPacket(Service.PEER_DATA, null, data );
	}
	
	/**
	 * Packet for telling peers about this local peer
	 * @param peerList
	 * @return
	 */
	public static final DataPacket createPeerZonePacket(Object peerList) {
		return new DataPacket(Service.PEER_ZONE, null, peerList);
	}
	
	/**
	 * Packet for peer data transfer
	 * @param data Data about this peer to be sent to other peers.
	 * @return
	 */
	public static final DataPacket createNewZonePacket(ZonePair zonePair) {
		return new DataPacket(Service.ZONE_MAPPING, null, zonePair );
	}
	
	/**
	 * Packet for updating local peers songlist
	 * @param peer
	 * @return
	 */
	public static final DataPacket createNewSongListPacket(Peer peer) {
		return new DataPacket(Service.SONGLIST, null, peer);
	}
	
	public static final DataPacket createNewQueuePacket(Command cmd, Object song, Zone zone) {
		return new DataPacket(Service.QUEUE_CHANGE, cmd, song, zone );
	}
	
	/**
	 * Packet for stream control
	 * @param cmd How to interact with the stream
	 * @param streamInfo Data for stream initialization
	 * @return
	 */
	public static final DataPacket createStreamCommandPacket(Command cmd, Object streamInfo) {
		return new DataPacket(Service.STREAMING, cmd, streamInfo);
	}
	
	/**
	 * A Stream packet when it is necessary to know what zone it is for. 
	 * @param cmd
	 * @param streamInfo
	 * @param zone
	 * @return
	 */
	public static final DataPacket createStreamCommandPacket(Command cmd, Object streamInfo, Zone zone) {
		return new DataPacket(Service.STREAMING, cmd, streamInfo, zone);
	}
	/**
	 * Packet for stream acknowledgment
	 * @param cmd
	 * @param streamInfo
	 * @param zone
	 * @return
	 */
	public static final DataPacket createStreamAcknowledgePacket(Command cmd, Object streamInfo, Zone zone) {
		return new DataPacket(Service.STREAMING_ACK, cmd, streamInfo, zone);
	}
	/**
	 * Packet for communication between service and music player
	 * @param cmd What the recipient should do with this packet
	 * @param object Data to be dealt with according to the cmd
	 * @return
	 */
	public static final DataPacket createInterprocessPacket(Command cmd, Object object) {
		return new DataPacket(Service.INTERPROCESS, cmd, object);
	}
	
	/**
	 * Packet for communication between service and music player
	 * @param cmd What the recipient should do with this packet
	 * @param object Data to be dealt with according to the cmd
	 * @param zone Which zone will be affected by the action or change
	 * @return
	 */
	public static final DataPacket createInterprocessPacket(Command cmd, Object object, Zone zone) {
		return new DataPacket(Service.INTERPROCESS, cmd, object, zone);
	}
	
	/**
	 * Packet for changing something about a current peer, usually the name.
	 * @param object What it should be changed to.
	 * @return
	 */
	public static final DataPacket createPeerChangePacket(Object object) {
		return new DataPacket(Service.PEER_EDIT, Command.CONFIG, object);
	}
	
	public static final DataPacket createPeerStatusChangePacket( Object object ) {
		return new DataPacket(Service.PEER_STATUS, null, object);
	}
	
	public static final DataPacket createUpdatePacket() {
		return new DataPacket(Service.UPDATE, null, null);
	}
}
