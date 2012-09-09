package soundcheck.musicPlayer.view;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import soundcheck.shared.Util;

public class AboutFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;

	static final int UP = KeyEvent.VK_UP;
	static final int DOWN = KeyEvent.VK_DOWN;
	static final int LEFT = KeyEvent.VK_LEFT;
	static final int RIGHT = KeyEvent.VK_RIGHT;
	static final int B = KeyEvent.VK_B;

	static private int[] code = 
		{UP, UP, DOWN, DOWN, LEFT, RIGHT, LEFT, RIGHT, B};
	static private Map<Integer, Integer>[] graph;
	static private int currentNode = 0;

	/**
	 * Create the frame.
	 */
	public AboutFrame() {
		setTitle("About SoundCheck");
		setResizable(false);
		setBounds(100, 100, 600, 402);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JLabel logo = new JLabel("");
		logo.setBounds(103, 11, 346, 104);
		logo.setIcon(new Util().createImageIcon("/images/textLogo.png", "Logo"));
		contentPane.add(logo);

		String version = getClass().getPackage().getImplementationVersion();
		JLabel versionName = new JLabel("Version " + version);
		versionName.setBounds(29, 129, 366, 14);
		contentPane.add(versionName);

		JLabel copyright = new JLabel("Copyright (c) 2011-2012 The SoundCheck Team. All Rights Reserved.");
		copyright.setBounds(29, 154, 366, 14);
		contentPane.add(copyright);

		JLabel sponsors = new JLabel("SoundCheck is made possible through the following resources.");
		sponsors.setBounds(29, 179, 366, 14);
		contentPane.add(sponsors);

		JEditorPane sponsorScrollArea = new JEditorPane("text/html","");
		HTMLEditorKit kit = new HTMLEditorKit();
		StyleSheet styleSheet = kit.getStyleSheet();
		styleSheet.addRule("A {color:white}"); //change links to white
		sponsorScrollArea.setEditorKit(kit);

		sponsorScrollArea.setText("<a href='http://www.msoe.edu/'>Milwaukee School of Engineering</a><br/>" +
				"<a href='http://www.iconarchive.com/show/fugue-icons-by-yusuke-kamiyamane.html'>IconArchive</a><br/>" +
				"<a href='http://www.jgroups.org/'>JGroups</a><br/>" +
				"<a href='http://www.xuggle.com/'>Xuggler</a><br/>" +
				"<a href='http://logback.qos.ch/'>Logback</a><br/>" +
				"<a href='http://www.junit.org/'>JUnit</a><br/>" +
				"<a href='http://www.jthink.net/jaudiotagger/'>Jaudiotagger</a><br/>" +
				"<a href='http://insubstantial.posterous.com/'>Insubstantial</a><br/>" +
				"<a href='http://swingx.java.net/'>SwingX</a><br/>");
		sponsorScrollArea.setEditable(false);
		sponsorScrollArea.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent event) {  
				if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {  
					try {  
						java.awt.Desktop.getDesktop().browse(event.getURL().toURI());
					} catch (IOException e) {  
						System.out.println("IOException........");  
						e.printStackTrace();  
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				}        
			}  
		});
		JScrollPane scroll = new JScrollPane(sponsorScrollArea);
		scroll.setBounds(29, 204, 534, 144);
		contentPane.add(scroll);

		graph = generateSequenceMap(code);
		addKeyListener(new KeyEventHandler());
		setFocusable(true);
	}

	class KeyEventHandler implements KeyListener {

		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
		}

		@Override
		public void keyReleased(KeyEvent e) {	
			if( checkSequence(e.getKeyCode()) ) {
				try {
					Clip clip = AudioSystem.getClip();
					AudioInputStream inputStream = AudioSystem.getAudioInputStream(AboutFrame.class.getResourceAsStream("/sounds/soundCheck.wav"));
					clip.open(inputStream);
					clip.start(); 
				} catch (Exception e1) {
					System.err.println(e1.getMessage());
				}
			}
		}	
	}

	static public boolean checkSequence(int keyPressed) {
		Integer nextNode = graph[currentNode].get(keyPressed);

		//Set currentNode to nextNode or to 0 if no matching sub-sequence exists
		currentNode = (nextNode==null ? 0 : nextNode);

		return currentNode == code.length-1;
	}

	static private Map<Integer, Integer>[] generateSequenceMap(int[] sequence) {

		//Create map
		@SuppressWarnings("unchecked")
		Map<Integer, Integer>[] graph = new Map[sequence.length];
		for(int i=0 ; i<sequence.length ; i++) {
			graph[i] = new TreeMap<Integer,Integer>();
		}

		//i is delta
		for(int i=0 ; i<sequence.length ; i++) {
			loop: for(int j=i ; j<sequence.length-1 ; j++) {
				if(sequence[j-i] == sequence[j]) {
					//Ensure that the longest possible sub-sequence is recognized
					Integer value = graph[j].get(sequence[j-i+1]);
					if(value == null || value < j-i+1)
						graph[j].put(sequence[j-i+1], j-i+1);
				}
				else {
					break loop;
				}
			}
		}
		return graph;
	}


}