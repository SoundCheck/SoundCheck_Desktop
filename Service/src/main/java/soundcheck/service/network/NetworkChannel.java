package soundcheck.service.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.PhysicalAddress;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.service.MasterController;
import soundcheck.service.data.PeerCollection;
import soundcheck.shared.Const;
import soundcheck.shared.DataPacket;
import soundcheck.shared.PacketCreator;
import soundcheck.shared.Peer;
import soundcheck.shared.Zone;
import soundcheck.shared.ZonePair;
import soundcheck.shared.ZoneProperties;

/**
 * Handles all communication over the JGroups channel to the SoundCheck cluster.
 * This cluster (a multicast address with JGroups wrappers) contains all available
 * SoundCheck peers on the network.
 *
 */
public final class NetworkChannel extends ReceiverAdapter {
	final static Logger logger = LoggerFactory.getLogger(NetworkChannel.class);

	private MasterController mc;
	private Peer thisPeer;
	private static JChannel channel;

	/**
	 * Creates new JGroups channel to the cluster defined in Const.
	 */
	public NetworkChannel(MasterController mc) {
		this(Const.DISCOVER_CLUSTER, mc);
	}

	/**
	 * Creates new JGroups channel instance to the given cluster name.
	 * @param cluster - The cluster to connect to.
	 * @param mc - The containing class
	 */
	public NetworkChannel(String cluster, MasterController mc) {
		this.mc = mc;
		initChannel(this, cluster);
	}

	/**
	 * Initialize the JChannel and it's attributes
	 * @param nc
	 * @param cluster
	 */
	private static final void initChannel(NetworkChannel nc, String cluster) {
		try {
			NetworkChannel.channel = new JChannel("udp_bping.xml");
			channel.setReceiver(nc);
			channel.setDiscardOwnMessages(false);
			// Wait set time for state transfer
			channel.connect(cluster, null, 5000);
			channel.send(null, PacketCreator.createRequestZoneMapPacket());
		} catch( Exception e) {
			logger.error("", e);
		}
	}

	/**
	 * This method is called by JGroups whenever a change
	 * is detected in the channel's cluster. Execution of this method should
	 * be as fast as possible. Refer to JGroups documentation for more info.
	 */
	@Override
	public synchronized void viewAccepted(View view) {
		logger.trace("Detected change in peers.");

		PeerManager peerManager = new PeerManager( PeerCollection.getPeers(), view.getMembers() );
		new Thread(peerManager).start();
	}

	/**
	 * Called by JGroups when a message is received over
	 * the cluster. Execution of this method should
	 * be as fast as possible. Refer to JGroups documentation for more info.
	 */
	@Override
	public void receive(Message msg) {
		MessageHandler messageHandler = new MessageHandler(this, msg);
		new Thread(messageHandler).start();
	}

	/**
	 * Given an output stream, place the state object within it so it
	 * can be sent to other peers.
	 */
	@Override
	public void getState(OutputStream output) throws Exception {
		logger.trace("Uploading new state to cluster.");

		Util.objectToStream( PeerCollection.getZonemap(), new DataOutputStream(output) );
	}

	/**
	 * Retrieve the state from the input stream and
	 * store in the local state variable.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setState(InputStream input) throws Exception {
		logger.trace("Receiving new state from cluster.");
		Map<Zone, ZoneProperties> map;

		map=(Map<Zone, ZoneProperties>)Util.objectFromStream(new DataInputStream(input));

		PeerCollection.setZoneMap(map);
	}

	/**
	 * Send a packet to the SoundCheck network
	 * @param dst Specific peer to send packet to. Null if all peers
	 * @param packet The DataPacket object to send
	 */
	public static void send(Address dst, DataPacket packet) {
		try {
			logger.trace("Sending {} packet to {}", packet.getService()
					.toString(), dst == null ? "Everyone" : dst.toString());
			
			List<Address> mobileAddresses = PeerManager.getMobileAddresses();
			

			if (channel.isConnected()) {
				channel.send(dst, packet);

				if(dst == null){
					for (Address addr : mobileAddresses) {
						send(addr, packet);
					}
				}
				
			}
			else {
				logger.warn("Channel is not connected - Retrying");
				while (!channel.isConnected()) {
				} // Keep trying until the channel is connected.
				logger.trace("Resending {} packet to {}", packet.getService()
						.toString(), dst == null ? "Everyone" : dst.toString());
				channel.send(dst, packet);

				if(dst == null){
					for (Address addr : mobileAddresses) {
						send(addr, packet);
					}
				}
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	/**
	 * Gets the physical IP address from a given JGroups address.
	 * @param addr
	 * @return IP address or null if it could not be determined.
	 */
	public String getIp(Address addr) {
		/* WARNING. This is dangerous code, as the JGroups API hides the 
		 * IP address and it could change between releases. However, this
		 * is the cleanest and easiest way for us to do this.
		 */
		PhysicalAddress physicalAddr = (PhysicalAddress)channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, addr));
		if(physicalAddr instanceof IpAddress) {
			IpAddress ipAddr = (IpAddress)physicalAddr;
			InetAddress inetAddr = ipAddr.getIpAddress();
			return inetAddr.getHostAddress();
		}

		logger.warn( "Could not determine {}'s IP address.", addr.toString() );
		return null;
	}

	/**
	 * Initialize all necessary information for this peer.
	 * @return Peer information from this computer.
	 */
	private void initializeThisPeer() {
		thisPeer = mc.getLocalPeer();

		thisPeer.setUid(channel.getAddress().toString());

		// If a name has not been determined by user, use UID
		if(thisPeer.getName() == null) {
			thisPeer.setName(thisPeer.getUid());
		}

		// Add local peer to zone map
		send(null, PacketCreator.createNewZonePacket( new ZonePair(thisPeer.getZone(),
				new ZoneProperties( thisPeer.getName() )) ));
		
		List<Address> mobileAddresses = PeerManager.getMobileAddresses();
		
		for(Address addr : mobileAddresses){
			send(addr, PacketCreator.createNewZonePacket( new ZonePair(thisPeer.getZone(),	new ZoneProperties( thisPeer.getName() )) ));
		}
	}

	/**
	 * Request info on this peer. If this peer has not been initialized, do so.
	 * @return
	 */
	public Peer getThisPeer() {
		if(thisPeer == null) {
			initializeThisPeer();
		}

		return thisPeer;
	}

	/**
	 * Update songlist for this peer amongst all other peers on network
	 */
	public final void sendNewSongList() {
		send( null, PacketCreator.createNewSongListPacket(mc.getLocalPeer()) );
		
		List<Address> mobileAddresses = PeerManager.getMobileAddresses();
		
		for(Address addr : mobileAddresses){
			send(addr, PacketCreator.createNewSongListPacket(mc.getLocalPeer()));
		}
	}

	/**
	 * Close this connection.
	 */
	public void destory() {
		channel.close();
	}
}
