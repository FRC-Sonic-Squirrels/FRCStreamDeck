package frcstreamdeck;

import de.rcblum.stream.deck.event.KeyEvent;
import de.rcblum.stream.deck.event.StreamKeyListener;
import edu.wpi.first.networktables.NetworkTableValue;
import frcstreamdeck.networking.StreamDeckNetwork;
import frcstreamdeck.streamDeck.FRCStreamDeckDevices;
import frcstreamdeck.streamDeck.IStreamDeckFRC;

// serves as the main class responsible for doing things or something idk aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
public class StreamDeckFrc {
    static IStreamDeckFRC[] streamDecks = new IStreamDeckFRC[Constants.STREAMDECK_COUNT];
    public static void main(String[] args) {
		StreamDeckNetwork.initNetwork();
		for (int i = 0; i < Constants.STREAMDECK_COUNT; i++) {
			streamDecks[i] = FRCStreamDeckDevices.getStreamDeck(i);
			streamDecks[i].addKeyListener(new StreamDeckListener(i));
			StreamDeckNetwork.smartDashboardTable.putValue("/streamdecks/sd" + String.valueOf(i) + "/rowOffset", NetworkTableValue.makeInteger(streamDecks[i].getRowOffset()));
			StreamDeckNetwork.smartDashboardTable.putValue("/streamdeck/sd" + String.valueOf(i) + "/rowWidth", NetworkTableValue.makeInteger(streamDecks[i].getRowWidth()));
			for (int y = 0; y < streamDecks[i].getKeySize(); y++) {
				StreamDeckNetwork.smartDashboardTable.putValue(keyToString(y, i), NetworkTableValue.makeBoolean(false));
			}
		}
		
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				System.out.println("sleep interupted");
			}
		}
    }

	public static String keyToString(int key, int deck){
		return "/streamdeck/sd" + String.valueOf(deck) + "/" + String.valueOf((key-streamDecks[deck].getRowOffset())/5) + "_" + String.valueOf(key%streamDecks[deck].getRowWidth()) + "_down";
	}

    public static class StreamDeckListener implements StreamKeyListener {
		private int deck;
		public StreamDeckListener(int deck){
			this.deck = deck;
		}
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
				StreamDeckNetwork.smartDashboardTable.putValue(keyToString(event.getKeyId(),deck), NetworkTableValue.makeBoolean(true));
				break;
			case RELEASED_CLICKED:
				System.out.println(event.getKeyId() + ": released/clicked");
				StreamDeckNetwork.smartDashboardTable.putValue(keyToString(event.getKeyId(),deck), NetworkTableValue.makeBoolean(false));
				break;
			default:
				break;
			}
		}
	}
}
