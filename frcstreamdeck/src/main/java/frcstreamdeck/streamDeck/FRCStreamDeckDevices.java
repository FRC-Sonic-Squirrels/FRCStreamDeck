package frcstreamdeck.streamDeck;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.rcblum.stream.deck.device.StreamDeck;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;


/**
 * 
 * <br><br>
 * 
 * MIT License
 * 
 * Copyright (c) 2017 Roland von Werden
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * @author Roland von Werden
 * @version 1.0.0
 *
 */
public class FRCStreamDeckDevices {
	
	/**
	 * Flag for enabling the software stream deck GUI. <code>true</code> Stream Deck
	 * devices will be wrapped in a software SD, <code>false</code> the StreamDeck
	 * will be returned directly.
	 */
	private static boolean enableSoftwareStreamDeck = true;
	
	public static final int VENDOR_ID = 4057;
	
	public static final int PRODUCT_ID = 109;

	private static List<HidDeviceInfo> deckInfos = null;

	private static List<HidDevice> deckDevices = null;

	private static List<IStreamDeckFRC> decks = null;

	private static List<IStreamDeckFRC> softDecks = null;
	
	public static void enableSoftwareStreamDeck() {
		FRCStreamDeckDevices.enableSoftwareStreamDeck = true;
	}
	
	public static void disableSoftwareStreamDeck() {
		FRCStreamDeckDevices.enableSoftwareStreamDeck = false;
	}
	
	public static boolean isSoftwareStreamDeckEnabled() {
		return FRCStreamDeckDevices.enableSoftwareStreamDeck;
	}
	
	
	public static HidDeviceInfo getStreamDeckInfo() {
		if (deckInfos == null) {
			deckInfos = new ArrayList<>(5);
			//System.out.println("Scanning for devices");
			List<HidDeviceInfo> devList = PureJavaHidApi.enumerateDevices();
			for (HidDeviceInfo info : devList) {
				//System.out.println("Vendor-ID: " + info.getVendorId() + ", Product-ID: " + info.getProductId());
				if (info.getVendorId() == VENDOR_ID && info.getProductId() == PRODUCT_ID) {
					deckInfos.add(info);
				}
			}
		}
		return !deckInfos.isEmpty() ? deckInfos.get(0) : null;
	}
	
	public static HidDevice getStreamDeckDevice() {
		if (deckDevices == null || deckDevices.isEmpty()) {
			HidDeviceInfo info = getStreamDeckInfo();
			deckDevices = new ArrayList<>(deckInfos.size());
			if (info != null) {
				try {
					for (HidDeviceInfo hidDeviceinfo : deckInfos) {
						deckDevices.add(PureJavaHidApi.openDevice(hidDeviceinfo));
					}
				} catch (IOException e) {
				}
			}
		}
		return !deckDevices.isEmpty() ? deckDevices.get(0) : null;
	}
	
	public static IStreamDeckFRC getStreamDeck() {
		if (decks == null || decks.isEmpty()) {
			HidDevice dev = getStreamDeckDevice();
			decks = new ArrayList<>(deckDevices.size());
			if (dev != null) {
				for (HidDevice hidDevice : deckDevices) {
					decks.add(new frcstreamdeck.streamDeck.StreamDeck(hidDevice, 99, StreamDeck.BUTTON_COUNT));
				}
			}
		}
		if(enableSoftwareStreamDeck && !GraphicsEnvironment.isHeadless() && softDecks == null) {
			softDecks = new ArrayList<>(deckDevices.size()); 
			for (int i=0; i<decks.size(); i++) {
				IStreamDeckFRC iStreamDeck = decks.get(i);
				softDecks.add(new frcstreamdeck.streamDeck.SoftStreamDeck("Stream Deck " + i, iStreamDeck));
			}
		}
		return !decks.isEmpty()
				? (enableSoftwareStreamDeck && !GraphicsEnvironment.isHeadless() ? softDecks.get(0) : decks.get(0)) 
				: (enableSoftwareStreamDeck && !GraphicsEnvironment.isHeadless() ? new frcstreamdeck.streamDeck.SoftStreamDeck("Soft Stream Deck", null) : null);
	}
	
	public static int getStreamDeckSize() {
		return decks != null ? decks.size() : 0;
	}
	
	public static IStreamDeckFRC getStreamDeck(int id) {
		if (decks == null || id < 0 || id >= getStreamDeckSize())
			return null;
		return decks.get(id);
	}
	
	public static String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    builder.append("{");
	    for(byte b : in) {
	        builder.append(String.format(" 0x%02x,", b));
	    }
	    builder.append("}");
	    return builder.toString();
	}
	
	private FRCStreamDeckDevices() {
		// Nothing here stanger
	}
}
 