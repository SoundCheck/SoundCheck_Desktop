package soundcheck.service.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.service.data.PeerCollection;
import soundcheck.service.streaming.StreamController;
import soundcheck.shared.Const;
import soundcheck.shared.Const.Command;
import soundcheck.shared.DataPacket;
import soundcheck.shared.PacketCreator;
import soundcheck.shared.Peer;
import soundcheck.shared.Zone;

public class ExternalChannel implements Runnable {
	final static Logger logger = LoggerFactory.getLogger(ExternalChannel.class);

	private static DatagramSocket socket;
	private static List<Peer> externalPeers = new LinkedList<Peer>();

	public ExternalChannel() {
		try {
			socket = new DatagramSocket(Const.EXTERNAL_PORT);
		} catch (SocketException e) {
			logger.warn("Could not open port to external device.",e);
		}
	}

	@Override
	public void run() {
		while(true) {
			receiveCommand();
		}

	}

	/**
	 * Send data to the external device
	 * @param msg
	 */
	public static final void sendList(List<Peer> peerList, InetAddress address) {
		try{
			logger.trace("Sending list to external device.");

			// List to csv
			StringBuilder listCSV = new StringBuilder("LIST" + peerList.size());
			for( Peer peer : peerList) {
				listCSV.append(peer.getIp() + ",");
			}
			// Remove last comma
			listCSV.deleteCharAt(listCSV.length()-1);

			socket.send(createDatagramPacket("LIST", (short)peerList.size(), listCSV.toString(), address));

		} catch(Exception e) {
			logger.error("", e);
		}
	}

	/**
	 * After receiving a discovery request from device, send a reply
	 * to indicate this peer exists.
	 */
	public static final void discoverReply(InetAddress address) {
		try {
			logger.trace("Responding to external discovery with SYNC");
			socket.send(createDatagramPacket("SYNC", address));
		} catch (IOException e) {
			logger.error("",e);
		}
	}

	/**
	 * Listen for data from an external device. Blocks
	 * @return
	 *TODO need to get receiving info from Erik
	 */
	public static final DataPacket receiveCommand() {
		DataPacket retPacket = null;
		String recData = null;
		logger.trace("Waiting to receive from external device.");

		try {
			byte buffer[] = new byte[65535];
			DatagramPacket packet = new DatagramPacket (buffer, buffer.length);

			socket.receive(packet);
			logger.trace("External packet received");
			// Convert the byte array read from network into a string
			recData = new String(packet.getData(), 0, 4);

			InetAddress currentAddr = packet.getAddress();

			// Ignore name since we don't care at this point
			if( recData.equalsIgnoreCase("DISC") ) {
				discoverReply(currentAddr);
			} else {
				// Get name and IP address to differentiate it from other peers on network.
				String externalName = new String(packet.getData(), 4, packet.getLength()).trim();
				String externalID = externalName + currentAddr;

				Zone externalZone = null;

				// Get zone if device already exists on network
				Peer externalPeer = PeerCollection.getPeerByUID(externalID);
				if( externalPeer != null ) {
					externalZone = externalPeer.getZone();
				}

				/* If a streaming command was issued, create data packet and send out as if
				from a normal Peer. */
				if( externalZone != null ) {
					if( recData.equalsIgnoreCase("PLAY") ) {
						StreamController.createStreamInfo(externalZone, Command.PLAY);
					} else if( recData.equalsIgnoreCase("STOP")) {
						StreamController.createStreamInfo(externalZone, Command.PAUSE);
					} else if( recData.equalsIgnoreCase("NEXT")) {
						StreamController.createStreamInfo(externalZone, Command.NEXT);
					}
				}
				if( recData.equalsIgnoreCase("ACKN")) {
					// New peer was discovered. Update the peer with current state of peers
					sendList(PeerCollection.getPeers(), currentAddr);

					/* Create Peer Data packet to register the external device 
					as a peer on all computers on the network */
					if( externalPeer == null ) {
						externalPeer = new Peer(externalID, externalName);
						externalPeer.setExternal(true);
					}

					externalPeers.add(externalPeer);

					// Don't add to list if it is already on the network
					if( PeerCollection.getPeerByUID(externalPeer.getUid()) == null ) {
						NetworkChannel.send( null, PacketCreator.createPeerDataPacket(externalPeer) );
					}
				}
			}

		} catch (IOException e) {
			logger.error("Connection to external device lost.");
			socket.close();
		}

		return retPacket;
	}

	public static DatagramPacket createDatagramPacket(String message, InetAddress destination) {
		return createDatagramPacket(message, (short)0, "", destination);
	}

	public static DatagramPacket createDatagramPacket(String message, short size, String message2, InetAddress destination) {
		// Create a byte array from a string
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream ();
		DataOutputStream dataOut = new DataOutputStream (byteOut);
		try {
			dataOut.write(170);
			dataOut.write(170);
			dataOut.writeBytes(message);
			dataOut.write(InetAddress.getLocalHost().getAddress());
			dataOut.writeShort(size);
			dataOut.writeBytes(message2);
		} catch (IOException e) {
			logger.error("",e);
		}
		byte[] data = byteOut.toByteArray();

		//Return the new packet with the byte array payload
		return new DatagramPacket (data, data.length, destination, Const.EXTERNAL_PORT);
	}
}
