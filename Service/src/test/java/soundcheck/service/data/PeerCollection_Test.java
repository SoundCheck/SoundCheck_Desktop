package soundcheck.service.data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import soundcheck.shared.Peer;

public class PeerCollection_Test {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		PeerCollection.clearPeers();
	}

	@Test
	public void testAddPeer() {
		System.out.println("Testing Add Peer");
		Peer peer1 = new Peer("Test1", "Test1");
		Peer peer2 = new Peer("Test2", "Test2");
		List<Peer> peerList;

		PeerCollection.addPeer(peer1);
		peerList = PeerCollection.getPeers();
		assertEquals("Peer was not added", 1, peerList.size());
		assertEquals("Peer not added correctly", "Test1", peerList.get(0).getUid());
		assertEquals("Peer not added correctly", "Test1", peerList.get(0).getName());

		PeerCollection.addPeer(peer2);
		peerList = PeerCollection.getPeers();
		assertEquals("Peer was not added", 2, peerList.size());
		assertEquals("Peer not added correctly", "Test2", peerList.get(1).getUid());
		assertEquals("Peer not added correctly", "Test2", peerList.get(1).getName());

		peer2.setName("Test3");
		PeerCollection.addPeer(peer2);
		peerList = PeerCollection.getPeers();
		assertEquals("Duplicate peer was added", 2, peerList.size());
		assertEquals("Duplicate peer was not replaced", "Test3", peerList.get(1).getName());
	}

	@Test
	public void testRemovePeers() {
		System.out.println("Testing remove Peers");
		Peer peer1 = new Peer("Test1", "Test1");
		Peer peer2 = new Peer("Test2", "Test2");
		Peer peer3 = new Peer("Test3", "Test3");
		List<Peer> peerList;

		PeerCollection.addPeer(peer1);
		PeerCollection.addPeer(peer2);
		PeerCollection.addPeer(peer3);
		peerList = PeerCollection.getPeers();
		
		List<Peer> removeList = new ArrayList<Peer>();
		removeList.add(peer1);
		removeList.add(peer2);
		PeerCollection.removePeers(removeList);
		assertEquals("Wrong number of peers removed", 1, peerList.size());
		assertEquals("Correct peers not removed", "Test3", peerList.get(0).getName());
	}

	@Test
	public void testGetPeerAtIndex() {
		System.out.println("Testing get peer at index");
		Peer peer1 = new Peer("Test1", "Test1");
		Peer peer2 = new Peer("Test2", "Test2");

		PeerCollection.addPeer(peer1);
		PeerCollection.addPeer(peer2);

		assertEquals("Wrong peer returned", "Test1", PeerCollection.getPeerAtIndex(0).getName());
		assertEquals("Wrong peer returned", "Test2", PeerCollection.getPeerAtIndex(1).getName());
	}

	@Test
	public void testIndexOfPeer() {
		System.out.println("Testing indexofPeer");
		Peer peer1 = new Peer("Test1", "Test1");
		Peer peer2 = new Peer("Test2", "Test2");
		PeerCollection.addPeer(peer1);
		PeerCollection.addPeer(peer2);
		
		assertEquals("Wrong index returned", 1, PeerCollection.getPeers().indexOf(peer2));
		assertEquals("Wrong index returned", 0, PeerCollection.getPeers().indexOf(peer1));
	}
}
