package frcstreamdeck.streamDeck;

import de.rcblum.stream.deck.device.IStreamDeck;
import de.rcblum.stream.deck.event.KeyEvent;
import de.rcblum.stream.deck.event.StreamKeyListener;

// serves as the main class responsible for doing things or something idk aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
public class StreamDeckFrc {
    IStreamDeck streamDeck;
    public static void main(String[] args) {
        IStreamDeck streamDeck = FRCStreamDeckDevices.getStreamDeck();
        streamDeck.addKeyListener(new StreamDeckListener());
		try {
			Thread.sleep(100_000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
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
				break;
			case RELEASED_CLICKED:
				System.out.println(event.getKeyId() + ": released/clicked");
				break;
			default:
				break;
			}
		}
	}
}
