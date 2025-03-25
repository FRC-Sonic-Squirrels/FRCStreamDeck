package frcstreamdeck;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.EnumSet;

import org.opencv.objdetect.Board;

import de.rcblum.stream.deck.event.KeyEvent;
import de.rcblum.stream.deck.event.StreamKeyListener;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTableValue;
import frcstreamdeck.networking.StreamDeckNetwork;
import frcstreamdeck.streamDeck.FRCStreamDeckDevices;
import frcstreamdeck.streamDeck.IStreamDeckFRC;
import frcstreamdeck.visual.FRCStreamDeckUI;

// serves as the main class responsible for doing things or something idk aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
public class StreamDeckFrc {
    static IStreamDeckFRC[] streamDecks = new IStreamDeckFRC[Constants.STREAMDECK_COUNT];
	static StreamDeckListener[] deckListeners = new StreamDeckListener[Constants.STREAMDECK_COUNT];
	static int updateListenerHandle;
	static FRCStreamDeckUI ui = new FRCStreamDeckUI();
    public static void main(String[] args) {
		// start the network
		StreamDeckNetwork.initNetwork();
		FRCStreamDeckDevices.getStreamDeck();
		// add as many streamdecks as we want
		for (int i = 0; i < Constants.STREAMDECK_COUNT; i++) {
			streamDecks[i] = FRCStreamDeckDevices.getStreamDeck(i);
			// add a fake one if none are connected
			if(streamDecks[i] == null){
				System.out.println("nulldeck");
				streamDecks[i] = new frcstreamdeck.streamDeck.FRCSoftStreamDeck("Soft Stream Deck", null);
			}
			deckListeners[i] = new StreamDeckListener(i);
			streamDecks[i].addKeyListener(deckListeners[i]);
			// initialise smartdashboard with them
			StreamDeckNetwork.smartDashboardTable.putValue("/streamdeck/sd" + String.valueOf(i) + "/rowWidth", NetworkTableValue.makeInteger(streamDecks[i].getRowWidth()));
			for (int y = streamDecks[i].getRowOffset(); y < streamDecks[i].getKeySize(); y++) {
				StreamDeckNetwork.smartDashboardTable.putValue(keyToString(y, i), NetworkTableValue.makeBoolean(false));
			}
		}
		System.out.println(deckListeners.length);
		// when the StreamDeck subsystem requests an update do this
		updateListenerHandle = StreamDeckNetwork.inst.addListener(
			StreamDeckNetwork.smartDashboardTable.getTopic("/streamdeck/update"),
			EnumSet.of(NetworkTableEvent.Kind.kValueAll),
			event -> {
				// go through all of the streamdecks
				for (int i = 0; i < Constants.STREAMDECK_COUNT; i++) {
					StreamDeckListener dl = deckListeners[i];
					dl.update();
					try {
						ArrayList<Integer> kids = dl.getKeyIds();
						for (int y = streamDecks[i].getRowOffset(); y < streamDecks[i].getKeySize(); y++) {
							// check if a key is down or not
							if(kids.contains(Integer.valueOf(y))){
								StreamDeckNetwork.smartDashboardTable.putValue(keyToString(y,i), NetworkTableValue.makeBoolean(true));
							}else{
								StreamDeckNetwork.smartDashboardTable.putValue(keyToString(y,i), NetworkTableValue.makeBoolean(false));
							}
						}
					} catch (ConcurrentModificationException e) {
						// this is here because this is on a seperate thread than the decks
						System.out.println("ConcurrentModificationException");
					}
					
				}
			}
		);
    }

	public static String keyToString(int key, int deck){
		int k = key - streamDecks[deck].getRowOffset();
		return "/streamdeck/sd" + String.valueOf(deck) + "/" + String.valueOf((k)/5) + "_" + String.valueOf((k)%streamDecks[deck].getRowWidth()) + "_down";
	}

    public static class StreamDeckListener implements StreamKeyListener {
		private int deck;

		private ArrayList<Integer> keysDown = new ArrayList<>();

		private ArrayList<Integer> keysPressed = new ArrayList<>();
		// removing from keysPressed didnt work so this is here
		private ArrayList<Integer> keysReleased = new ArrayList<>();

		public StreamDeckListener(int deck){
			this.deck = deck;
		}
		// this is here so that values pressed and released between robot updates
		// get registered for at least one
		public void update(){
			keysDown.clear();
			keysDown.addAll(keysPressed);
			keysPressed.removeAll(keysReleased);
			keysReleased.clear();
		}

		public ArrayList<Integer> getKeyIds(){
			return keysDown;
		}

		public void onKeyEvent(KeyEvent event) {
			switch(event.getType()) {
			case OFF_DISPLAY :
				break;
			case ON_DISPLAY:
				break;
			case PRESSED:
			    System.out.println(event.getKeyId() + ": pressed on board: " + deck);
				keysPressed.add(event.getKeyId());
				break;
			case RELEASED_CLICKED:
				System.out.println(event.getKeyId() + ": released on board: " + deck);
				keysReleased.add(event.getKeyId());
				break;
			default:
				break;
			}
		}
	}
}
