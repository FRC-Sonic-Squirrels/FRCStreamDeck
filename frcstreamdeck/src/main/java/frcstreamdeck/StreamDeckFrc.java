package frcstreamdeck;

import de.rcblum.stream.deck.event.KeyEvent;
import de.rcblum.stream.deck.event.StreamKeyListener;
import edu.wpi.first.networktables.NetworkTableValue;
import frcstreamdeck.networking.StreamDeckNetwork;
import frcstreamdeck.streamDeck.FRCStreamDeckDevices;
import frcstreamdeck.streamDeck.IStreamDeckFRC;

// serves as the main class responsible for doing things or something idk aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
public class StreamDeckFrc {
    static IStreamDeckFRC streamDeck;
    public static void main(String[] args) {
		StreamDeckNetwork.initNetwork();
        streamDeck = FRCStreamDeckDevices.getStreamDeck();
        streamDeck.addKeyListener(new StreamDeckListener());
		StreamDeckNetwork.smartDashboardTable.putValue("/streamdeck/rowOffset", NetworkTableValue.makeInteger(streamDeck.getRowOffset()));
		StreamDeckNetwork.smartDashboardTable.putValue("/streamdeck/rowWidth", NetworkTableValue.makeInteger(streamDeck.getRowWidth()));
		for (int i = 0; i < streamDeck.getKeySize(); i++) {
			StreamDeckNetwork.smartDashboardTable.putValue(keyToString(i), NetworkTableValue.makeBoolean(false));
		}
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				System.out.println("sleep interupted");
			}
		}
    }

	public static String keyToString(int key){
		return "/streamdeck/" + String.valueOf((key-streamDeck.getRowOffset())/5) + "_" + String.valueOf(key%streamDeck.getRowWidth()) + "_down";
	}

    public static class StreamDeckListener implements StreamKeyListener {
		public void onKeyEvent(KeyEvent event) {
			switch(event.getType()) {
			case OFF_DISPLAY :
				System.out.println(event.getKeyId() + ": taken off display");
				break;
			case ON_DISPLAY:
				System.out.println(event.getKeyId() + ": put on display");
				break;
			case PRESSED:
				System.out.println(event.getKeyId() + ": pressed");
				StreamDeckNetwork.smartDashboardTable.putValue(keyToString(event.getKeyId()), NetworkTableValue.makeBoolean(true));
				break;
			case RELEASED_CLICKED:
				System.out.println(event.getKeyId() + ": released/clicked");
				StreamDeckNetwork.smartDashboardTable.putValue(keyToString(event.getKeyId()), NetworkTableValue.makeBoolean(false));
				break;
			default:
				break;
			}
		}
	}
}
