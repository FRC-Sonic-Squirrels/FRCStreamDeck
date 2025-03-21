package frcstreamdeck.streamDeck;

import de.rcblum.stream.deck.device.IStreamDeck;
import de.rcblum.stream.deck.event.KeyEvent;
import de.rcblum.stream.deck.event.StreamKeyListener;
import edu.wpi.first.smartdashboard.SmartDashboard;

import java.io.IOException;

public class Example2_Receiving_key_events {
	public static void main(String[] args) throws IOException {
		SmartDashboard.main(args);
		// Get the first connected (or software) ESD:
		IStreamDeck streamDeck = FRCStreamDeckDevices.getStreamDeck();
		// Reset the ESD so we can display our icon on it:
		System.out.println(streamDeck.addKeyListener(new ExampleListener()));
		try {
			Thread.sleep(100_000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}
	
	public static class ExampleListener implements StreamKeyListener {
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
