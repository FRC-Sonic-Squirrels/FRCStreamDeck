package frcstreamdeck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.EnumSet;

import org.opencv.core.Core;

import de.rcblum.stream.deck.event.KeyEvent;
import de.rcblum.stream.deck.event.StreamKeyListener;
import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.math.jni.WPIMathJNI;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTableValue;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;
import frcstreamdeck.networking.StreamDeckNetwork;
import frcstreamdeck.streamDeck.FRCSoftStreamDeck;
import frcstreamdeck.streamDeck.FRCStreamDeckDevices;
import frcstreamdeck.streamDeck.IStreamDeckFRC;
import frcstreamdeck.visual.FRCStreamDeckUI;

// serves as the main class responsible for doing things or something idk aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
public class StreamDeckFrc {
    private static IStreamDeckFRC[] streamDecks = new IStreamDeckFRC[Constants.STREAMDECK_COUNT];
	private static StreamDeckListener[] deckListeners = new StreamDeckListener[Constants.STREAMDECK_COUNT];
	private static int updateListenerHandle;
	private static FRCStreamDeckUI ui = new FRCStreamDeckUI();
	private static int realdeckCount = 0;
    public static void main(String[] args) {
		NetworkTablesJNI.Helper.setExtractOnStaticLoad(false);
        WPIUtilJNI.Helper.setExtractOnStaticLoad(false);
        WPIMathJNI.Helper.setExtractOnStaticLoad(false);
        CameraServerJNI.Helper.setExtractOnStaticLoad(false);
        try {
            CombinedRuntimeLoader.loadLibraries(StreamDeckNetwork.class, "wpiutiljni", "wpimathjni", "ntcorejni",
            Core.NATIVE_LIBRARY_NAME, "cscorejni");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
		StreamDeckNetwork.initNetwork();
		while (true) {
			try {Thread.sleep(100);} catch (Exception e) {}
			ui.periodic();
			if(!StreamDeckNetwork.inst.isConnected()){
				try {
					for (StreamDeckListener dl : deckListeners) {
						dl.update();
					}
				} catch (NullPointerException e) {
					// decks not initialised
				}
			}
		}
    }

	public static void swapDecks(int i1, int i2){
		IStreamDeckFRC temp = streamDecks[i1];
		streamDecks[i1] = streamDecks[i2];
		streamDecks[i2] = temp;

		StreamDeckListener tempListen = deckListeners[i1];
		deckListeners[i1] = deckListeners[i2];
		deckListeners[i2] = tempListen;

		streamDecks[i2].removeKeyListener(deckListeners[i1]);
		streamDecks[i1].removeKeyListener(deckListeners[i2]);
		streamDecks[i2].addKeyListener(deckListeners[i2]);
		streamDecks[i1].addKeyListener(deckListeners[i1]);
	}

	public static int getRealDeckCount(){
		return realdeckCount;
	}

	public static String getNameOfDeck(int i){
		if(i >= streamDecks.length || streamDecks[i]==null) return "";
		return streamDecks[i].getName();
	}

	public static ArrayList<Integer> getKeyIds(int i){
		if(i >= deckListeners.length || deckListeners[i]==null) return new ArrayList<>();
		return deckListeners[i].getKeyIds();
	}

	public static void resetDecks(){
		// resets the list of streamdecks connected
		FRCStreamDeckDevices.resetDecks();
		FRCStreamDeckDevices.getStreamDeck();
		// add as many streamdecks as we want
		for (IStreamDeckFRC deck : streamDecks) {
			if(deck instanceof FRCSoftStreamDeck){
				((FRCSoftStreamDeck)deck).stop();
			}
		}
		realdeckCount = Constants.STREAMDECK_COUNT;
		streamDecks = new IStreamDeckFRC[Constants.STREAMDECK_COUNT];
		deckListeners = new StreamDeckListener[Constants.STREAMDECK_COUNT];
		for (int i = 0; i < Constants.STREAMDECK_COUNT; i++) {
			streamDecks[i] = FRCStreamDeckDevices.getStreamDeck(i);
			// add a fake one if none are connected
			if(streamDecks[i] == null){
				streamDecks[i] = new frcstreamdeck.streamDeck.FRCSoftStreamDeck("Soft Stream Deck", null);
				realdeckCount--;
			}
			streamDecks[i].setName("Deck: " + i);
			deckListeners[i] = new StreamDeckListener(i);
			streamDecks[i].addKeyListener(deckListeners[i]);
		}
	}

	public static void connectToRobot(){
		// start the network
		StreamDeckNetwork.connect();
		for (int i = 0; i < Constants.STREAMDECK_COUNT; i++) {
			// initialise smartdashboard with them
			StreamDeckNetwork.smartDashboardTable.putValue("streamdeck/sd" + String.valueOf(i) + "/rowWidth", NetworkTableValue.makeInteger(streamDecks[i].getRowWidth()));
			for (int y = streamDecks[i].getRowOffset(); y < streamDecks[i].getKeySize(); y++) {
				StreamDeckNetwork.smartDashboardTable.putValue(keyToString(y, i), NetworkTableValue.makeBoolean(false));
			}
		}
		// when the StreamDeck subsystem requests an update do this
		StreamDeckNetwork.inst.removeListener(updateListenerHandle);
		updateListenerHandle = StreamDeckNetwork.inst.addListener(
			StreamDeckNetwork.smartDashboardTable.getTopic("streamdeck/update"),
			EnumSet.of(NetworkTableEvent.Kind.kValueAll),
			event -> {
				// go through all of the streamdecks
				for (int i = 0; i < deckListeners.length; i++) {
					if (deckListeners[i] == null || streamDecks[i] == null) {
						continue;
					}
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
		return "streamdeck/sd" + String.valueOf(deck) + "/" + String.valueOf((k)/5) + "_" + String.valueOf((k)%streamDecks[deck].getRowWidth()) + "_down";
	}

    public static class StreamDeckListener implements StreamKeyListener {
		public int deck;

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
				keysPressed.add(event.getKeyId());
				break;
			case RELEASED_CLICKED:
				keysReleased.add(event.getKeyId());
				break;
			default:
				break;
			}
		}
	}
}
