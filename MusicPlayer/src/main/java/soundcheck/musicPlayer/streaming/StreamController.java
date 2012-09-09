package soundcheck.musicPlayer.streaming;

import soundcheck.musicPlayer.ServiceCallback;
import soundcheck.shared.Const.Command;
import soundcheck.shared.DataPacket;
import soundcheck.shared.StreamInfo;


/**
 * Controls the receiving and publishing of streams.
 * 
 *
 */
public class StreamController {

	
	private static StreamReceiver strReceiver;
	
	public static void receiveStream(Object streamData, ServiceCallback callback){
		
		if(strReceiver == null){
			strReceiver = new StreamReceiver(callback);
		}
		
		DataPacket packet = (DataPacket) streamData;
		
		if(packet.getCommand() == Command.NEXT){
			packet.setCommand(Command.TEARDOWN);
			
			callback.notify(Command.REQUEST_UPDATE, null);
		}
		
		StreamInfo streamInfo = (StreamInfo) packet.getData();
		
		strReceiver.processCommand(packet.getCommand(), streamInfo);
		
		if(packet.getCommand() == Command.TEARDOWN){
			strReceiver = null;
		}
		
		callback.notify( packet.getCommand(), streamInfo  );  //Send response to the source
	}
}
