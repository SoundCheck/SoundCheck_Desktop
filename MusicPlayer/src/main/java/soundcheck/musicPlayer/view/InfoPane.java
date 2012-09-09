package soundcheck.musicPlayer.view;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import soundcheck.shared.Util;

class InfoPane {

	PlayerGUI gui;
	JProgressBar songProgress;
	JLabel currentTime;
	JLabel endTime;
	
	private Runnable setProgressPositionRunnable;
	private Runnable setProgressMaximum;
	private static long timeStamp;
	private static long songDuration;

	InfoPane(PlayerGUI gui) {
		this.gui = gui;
		
		// Here so these methods can be run on the event dispatch thread
		setProgressPositionRunnable = new Runnable() {
			public void run() {
				songProgress.setValue((int) timeStamp);
				currentTime.setText(Util.secToFormattedTime(timeStamp));
			}
		};
		
		setProgressMaximum = new Runnable() {
			public void run() {
				songProgress.setMaximum((int) songDuration);
				endTime.setText(Util.secToFormattedTime(songDuration));
				setProgressPosition(0); // New song, set progress to 0
			}
		};
	}

	/**
	 * Set the length of the progress bar
	 */
	void setProgressMaximum(long songDurationNew) {
		songDuration = songDurationNew;
		SwingUtilities.invokeLater(setProgressMaximum);
	}

	/**
	 * Set the progress bar to a specific position and update
	 * the time display.
	 * @param timeStamp
	 */
	void setProgressPosition(long timeStampNew) {
		timeStamp = timeStampNew;
		SwingUtilities.invokeLater(setProgressPositionRunnable);
	}

	/**
	 * Create the bottom panel that displays playing song info.
	 * @return
	 */
	JPanel createInfoPane() {
		JPanel bottomPanel = new JPanel();

		bottomPanel.setPreferredSize(new Dimension(0,50));
		currentTime = new JLabel("0:00");
		endTime = new JLabel("0:00");

		songProgress = new JProgressBar(JSlider.HORIZONTAL, 0, 1);
		songProgress.setEnabled(true);

		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));

		bottomPanel.add(Box.createRigidArea(new Dimension(5,0)));
		bottomPanel.add(currentTime);
		bottomPanel.add(Box.createRigidArea(new Dimension(5,0)));
		bottomPanel.add(songProgress);
		bottomPanel.add(Box.createRigidArea(new Dimension(5,0)));
		bottomPanel.add(endTime);
		bottomPanel.add(Box.createRigidArea(new Dimension(5,0)));

		return bottomPanel;
	}

}
