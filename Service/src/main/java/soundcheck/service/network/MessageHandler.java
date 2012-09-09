package soundcheck.service.network;

import java.util.List;
import java.util.Map;

import org.jgroups.Address;
import org.jgroups.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.service.MasterController;
import soundcheck.service.data.PeerCollection;
import soundcheck.service.streaming.StreamController;
import soundcheck.shared.DataPacket;
import soundcheck.shared.PacketCreator;
import soundcheck.shared.Peer;
import soundcheck.shared.Song;
import soundcheck.shared.Zone;
import soundcheck.shared.ZonePair;
import soundcheck.shared.ZoneProperties;

/**
 * Determines which handler to pass the packet to
 * based on the packets contents.
 *
 */
public class MessageHandler implements Runnable {
	final static Logger logger = LoggerFactory.getLogger(MessageHandler.class);

	private final NetworkChannel channel;
	private final Message msg;

	public MessageHandler( NetworkChannel channel, Message msg ) {
		this.channel = channel;
		this.msg = msg;
	}

	@Override
	public void run() {
		handleMessage();
	}

	@SuppressWarnings("unchecked")
	private void handleMessage() {

		DataPacket packet = (DataPacket)msg.getObject();

		logger.trace("Received {} packet from {}.", packet.getService().toString(), msg.getSrc().toString());
		switch( packet.getService() ) {
		
		case REQUEST_ZONE_MAP:
			NetworkChannel.send(msg.getSrc(), PacketCreator.createRespondZoneMapPacket(PeerCollection.getZonemap()));
			break;
			
		case GET_ZONE_MAP:
			logger.trace("Receiving initial Zone Map from peer");
			PeerCollection.getZonemap().putAll((Map<Zone,ZoneProperties>)packet.getData());
			break;
			
		case DISCOVERY:
			// When receiving a discovery packet, send local data to requester
			
			//All Android devices have this in the name.  Add the address to the list
			if(msg.getSrc().toString().contains("localhost")){
				if(!PeerManager.getMobileAddresses().contains(msg.getSrc())){
					PeerManager.addMobileAddress(msg.getSrc());
				}
			}

			Peer thisPeer = channel.getThisPeer();
			// Get the peer from PeerCollection as this is the most up to date peer.
			Peer updatedPeer = PeerCollection.getPeerByUID(thisPeer.getUid());
			NetworkChannel.send( msg.getSrc(), PacketCreator.createPeerDataPacket(updatedPeer != null ? updatedPeer : thisPeer) );
			break;

		case PEER_DATA:
			// When receiving a data packet, add new peer to peerlist

			// This data on peer is set once packet has been received
			Peer newPeer = (Peer)packet.getData();
			Address srcAddr = msg.getSrc();
			String srcIp = channel.getIp( srcAddr );

			newPeer.setAddress(srcAddr);
			newPeer.setIp(srcIp);

			logger.trace("New Peer\n\tName: {}\n\tIP: {}", newPeer.getName(), newPeer.getIp());

			PeerCollection.addPeer(newPeer);
			break;
			
		case PEER_EDIT:
			// Edit an existing peer to this peer's name
			Peer changedPeer = (Peer)packet.getData();
			
			PeerCollection.editPeerName(changedPeer);
			break;
			
		case PEER_STATUS:
			// Edit an existing peer's status
			
			PeerCollection.editPeerStatus((Peer)packet.getData());
			break;
			
		case PEER_ZONE:
			// Modify the zones for received peers
			List<Peer> peerList = (List<Peer>)packet.getData();
			
			PeerCollection.updatePeerZones(peerList);
			break;

		case ZONE_MAPPING:
			// A new zone has been created or a zone has been renamed.

			ZonePair zonePair = (ZonePair)packet.getData();

			PeerCollection.putZone( zonePair.getZone(), zonePair.getProp() );

			break;
			
		case QUEUE_CHANGE:
			// Get current zone data
			Zone zone = packet.getZone();
			ZoneProperties zoneProp = PeerCollection.getZoneProps(zone);
			
			// Change queue in proper way
			switch (packet.getCommand()) {
			case QUEUE_FRONT:
				zoneProp.addFirst((Song) packet.getData());
				break;
			case QUEUE_BACK:
				zoneProp.addLast((Song) packet.getData());
				break;
			case QUEUE_REMOVE:
				zoneProp.removeSong((Song) packet.getData());
				break;
			case NEW_QUEUE:
				zoneProp.setPlayList( (List<Song>)packet.getData() );
				break;
			default:
				logger.warn("Unknown QUEUE_CHANGE command {}", packet.getCommand().toString());
			}
			
			// Write new queue to peer collection
			PeerCollection.putZone(zone, zoneProp);
			break;

		case STREAMING:
			// Receiving a stream control packet
			StreamController.receive(msg);
			break;
			
		case STREAMING_ACK:
			// Receiving a stream acknowledgment packet
			StreamController.ackStream( msg );
			break;

		case SONGLIST:
			
			// receiving data packet, update songlist for sending peer
			
			// peer object passed in data packet
			Peer songPeer = (Peer) packet.getData();
			
			// get peer with same uid from local collection and update songlist to newly passed peer
			PeerCollection.updateSongList(songPeer);
			
			break;
			
		case UPDATE:
			
			MasterController mc = PeerCollection.getMC();
			
			mc.sendPeerListToMusicPlayer();
			mc.sendSongListToMusicPlayer();
			mc.sendZoneMapToMusicPlayer();
			
		default:
			logger.warn("Unrecognized packet type \"{}\" received from {}.", packet.getService().toString(), msg.getSrc().toString() );
		}
	}
}
