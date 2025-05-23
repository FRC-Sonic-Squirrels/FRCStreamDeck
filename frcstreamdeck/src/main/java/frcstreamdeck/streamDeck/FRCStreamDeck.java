package frcstreamdeck.streamDeck;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.rcblum.stream.deck.device.IStreamDeck;
import de.rcblum.stream.deck.event.KeyEvent;
import de.rcblum.stream.deck.event.KeyEvent.Type;
import de.rcblum.stream.deck.event.StreamKeyListener;
import de.rcblum.stream.deck.items.StreamItem;
import de.rcblum.stream.deck.util.IconHelper;
import de.rcblum.stream.deck.util.SDImage;
import purejavahidapi.HidDevice;
import purejavahidapi.InputReportListener;

/**
 * Provides low level access to a connected stream deck. Allows to do the
 * following without having to deal with the HID format:<br>
 * 1. Reset the device<br>
 * 2. Set the brightness of the device<br>
 * 3. Set an image for one of the key at a time.<br>
 * 4. Receive an {@link KeyEvent} whenever a key was
 * pressed/released/clicked.<br>
 * <br>
 * <br>
 * For the device to properly function, it has to be initialised properly. This
 * is done by 1.) resetting it, 2) setting the brightness of the device. <br>
 * <br>
 * All processing is don asynchronous, meaning anything sent to the deck is put
 * in a queue and processed in another thread. Processing is done at a tick rate
 * of 100 (100 updates per second max).
 * 
 * <br>
 * <br>
 * 
 * MIT License<br>
 * <br>
 * Copyright (c) 2017 Roland von Werden<br>
 * <br>
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy<br>
 * of this software and associated documentation files (the "Software"), to
 * deal<br>
 * in the Software without restriction, including without limitation the
 * rights<br>
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell<br>
 * copies of the Software, and to permit persons to whom the Software is<br>
 * furnished to do so, subject to the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be included
 * in<br>
 * all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR<br>
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,<br>
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE<br>
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER<br>
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM,<br>
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE<br>
 * SOFTWARE.<br>
 * 
 * @author Roland von Werden
 * @version 1.0.0
 *
 */
public class FRCStreamDeck implements InputReportListener, IStreamDeckFRC {

	/**
	 * Job that is submitted, when the Method {@link FRCStreamDeck#setBrightness(int)} is called.<br>
	 * When executed it will call the Method {@link FRCStreamDeck#internalUpdateBrightnes()}, which will send the brightness command to the stream deck.
	 * @author Roland von Werden
	 *
	 */
	private class BrightnessUpdater implements Runnable {
		public BrightnessUpdater() {
		}

		@Override
		public void run() {
			FRCStreamDeck.this.internalUpdateBrightnes();
		}
	}

	/**
	 * Dispatcher that asynchronously sends out all issued {@link KeyEvent}s.
	 * @author Roland von Werden
	 *
	 */
	private class EventDispatcher implements Runnable {

		@Override
		public void run() {
			while (FRCStreamDeck.this.running || !FRCStreamDeck.this.running && !recievePool.isEmpty()) {
				if (!FRCStreamDeck.this.recievePool.isEmpty()) {
					KeyEvent event = FRCStreamDeck.this.recievePool.poll();
					int i = event.getKeyId();
					if (i < FRCStreamDeck.this.keys.length && FRCStreamDeck.this.keys[i] != null) {
						FRCStreamDeck.this.keys[i].onKeyEvent(event);
					}
					FRCStreamDeck.this.listerners.stream().forEach(l -> 
						{
							try {
								l.onKeyEvent(event);
							} 
							catch (Exception e) {
							}
						}
					);
				}
				if (FRCStreamDeck.this.recievePool.isEmpty()) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}

	}

	/**
	 * Dispatches all commands asynchronously queued up in {@link FRCStreamDeck#sendPool} to the ESD.
	 * Send rate is limited to 500 commands per second.
	 * If the execution of one command is completed in less tha on ms the thread is put to sleep for 1 ms.
	 * As long as one loop takes up less then 2 ms the rest of the time is actively wated
	 * 
	 * @author Roland von Werden
	 *
	 */
	private class DeckWorker implements Runnable {
		
		@SuppressWarnings("unused")
		@Override
		public void run() {
			long actions = 0;
			long time = System.currentTimeMillis();
			long t = 0;
			while (running || !running && !sendPool.isEmpty()) {
				if(sendPool.size() > 100) {
					Runnable[] payloads = new Runnable[FRCStreamDeck.this.getKeySize()];
					Runnable resetTask = null;
					Runnable brightnessTask = null;
					Runnable task = null;
					while ((task = sendPool.poll()) != null) {
						if (task instanceof IconUpdater) {
							IconUpdater iu = (IconUpdater)task;
							payloads[iu.keyIndex] = iu;
						}
						else if(task instanceof Resetter)
							resetTask = task;
						else if (task instanceof BrightnessUpdater) 
							brightnessTask = task;
					}
					if(brightnessTask != null)
						sendPool.add(brightnessTask);
					if(resetTask != null)
						sendPool.add(resetTask);
					for (int i = 0; i < payloads.length; i++) {
						if(payloads[i] != null)
							sendPool.add(payloads[i]);
					}
				}
				t = System.nanoTime();
				Runnable task = sendPool.poll();
				if (task != null) {
					try {
						task.run();
					} catch (Exception e) {
					}
				}
				if (System.nanoTime()-t < 1_000)
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				while(System.nanoTime()-t < 2_000) {
					Math.subtractExact(2, 1);
				}
			}
		}

	}

	/**
	 * Sends an Icon to a given key on the ESD.
	 * @author Roland von Werden
	 *
	 */
	private class IconUpdater implements Runnable {

		int keyIndex;
		SDImage img;

		public IconUpdater(int keyIndex, SDImage img) {
			this.keyIndex = keyIndex;
			this.img = img;
		}

		@Override
		public void run() {
			FRCStreamDeck.this.internalDrawImage(keyIndex, img);
		}

	}

	/**
	 * Sends the reset command to the ESD.
	 * @author Roland von Werden
	 *
	 */
	private class Resetter implements Runnable {
		public Resetter() {}

		@Override
		public void run() {
			FRCStreamDeck.this.internalReset();
		}
	}

	/**
	 * Reset command
	 */
	private static final byte[] RESET_DATA = new byte[] { 0x0B, 0x63, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	/**
	 * Brightness command
	 */
	private static final byte[] BRIGHTNES_DATA = new byte[] { 0x05, 0x55, (byte) 0xAA, (byte) 0xD1, 0x01, 0x63, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

	/**
	 * Header for Page 1 of the image command
	 */
	private static final byte[] PAGE_1_HEADER = new byte[] { 0x01, 0x01, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x42, 0x4D, (byte) 0xF6, 0x3C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x36, 0x00, 0x00,
			0x00, 0x28, 0x00, 0x00, 0x00, 0x48, 0x00, 0x00, 0x00, 0x48, 0x00, 0x00, 0x00, 0x01, 0x00, 0x18, 0x00, 0x00,
			0x00, 0x00, 0x00, (byte) 0xC0, 0x3C, 0x00, 0x00, (byte) 0xC4, 0x0E, 0x00, 0x00, (byte) 0xC4, 0x0E, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

	/**
	 * Header for Page 2 of the image command
	 */
	private static final byte[] PAGE_2_HEADER = new byte[] { 0x01, 0x02, 0x00, 0x01, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

	/**
	 * Number of buttons on the ESD
	 */
	public static final int BUTTON_COUNT = 18;

	/**
	 * Icon size of one key
	 */
	public static final int ICON_SIZE = 72;

	/**
	 * Page size that can be sent to the ESD at once
	 */
	public static final int PAGE_PACKET_SIZE = 1023;

	/**
	 * Pixels(times 3 to get the amount of bytes) of an icon that can be sent with page 1 of the image command
	 */
	public static final int NUM_FIRST_PAGE_PIXELS = 2583;

	/**
	 * Pixels(times 3 to get the amount of bytes) of an icon that can be sent with page 2 of the image command
	 */
	public static final int NUM_SECOND_PAGE_PIXELS = 2601;

	/**
	 * Back image for not used keys
	 */
	public static final SDImage BLACK_ICON = createBlackIcon("temp://BLACK_ICON");

	private static SDImage createBlackIcon(String path) {
		BufferedImage img = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, ICON_SIZE, ICON_SIZE);
		g.dispose();
		return IconHelper.cacheImage(path, img);
	}

	/**
	 * HidDevice associated with the connected ESD
	 */
	private HidDevice hidDevice = null;

	/**
	 * Brightness command for this instance.
	 */
	private byte[] brightness = Arrays.copyOf(BRIGHTNES_DATA, BRIGHTNES_DATA.length);

	/**
	 * Keys set to be displayed on the StreamDeck
	 */
	private StreamItem[] keys = new StreamItem[FRCStreamDeck.this.getKeySize()];

	/**
	 * current values if a key on a certain index is pressed or not
	 */
	private boolean[] keysPressed = new boolean[FRCStreamDeck.this.getKeySize()];
	
	/**
	 * Amount of keys present in the stream deck.
	 */
	private int keyCount =  BUTTON_COUNT;

	/**
	 * Queue for commands to be sent to the ESD
	 */
	private Queue<Runnable> sendPool = new ConcurrentLinkedQueue<>();

	/**
	 * Queue for {@link KeyEvent}s that are triggered by the ESD
	 */
	private Queue<KeyEvent> recievePool = new ConcurrentLinkedQueue<>();

	/**
	 * Page 1 for the image command
	 */
	private byte[] p1 = new byte[PAGE_PACKET_SIZE];

	/**
	 * Page 2 for the image command
	 */
	private byte[] p2 = new byte[PAGE_PACKET_SIZE];

	/**
	 * Daemon that send commands to the ESD
	 */
	private Thread sendWorker = null;

	/**
	 * Daemon that sends received {@link KeyEvent}s  to the affected listeners.
	 */
	private Thread eventDispatcher = null;

	/**
	 * Intended state of the daemons
	 */
	private boolean running = true;

	/**
	 * Registered Listeners to the {@link KeyEvent}s created by the ESD
	 */
	private List<StreamKeyListener> listerners;

	/**
	 * Creates a wrapper for the Stream Deck HID
	 * 
	 * @param streamDeck
	 *            Stream Decks HID Devices
	 * @param brightness
	 *            Brightness from 0 .. 99
	 */
	public FRCStreamDeck(HidDevice streamDeck, int brightness, int keyCount) {
		super();
		this.keyCount = keyCount;
		this.keys = new StreamItem[this.getKeySize()];
		this.keysPressed = new boolean[this.getKeySize()];
		this.hidDevice = streamDeck;
		this.hidDevice.setInputReportListener(this);
		this.brightness[5] = (byte) brightness;
		listerners = new ArrayList<>(5);
		this.sendWorker = new Thread(new DeckWorker());
		this.sendWorker.setDaemon(true);
		this.sendWorker.start();
		this.eventDispatcher = new Thread(new EventDispatcher());
		this.eventDispatcher.setDaemon(true);
		this.eventDispatcher.start();
	}

	/**
	 * Sends reset-command to ESD
	 */
	private void internalReset() {
		hidDevice.setFeatureReport(RESET_DATA[0], Arrays.copyOfRange(RESET_DATA, 1, RESET_DATA.length), RESET_DATA.length-1);
	}


	/**
	 * Sends brightness-command to ESD
	 */
	private void internalUpdateBrightnes() {
		hidDevice.setFeatureReport(this.brightness[0], Arrays.copyOfRange(this.brightness, 1, this.brightness.length), this.brightness.length-1);
	}
	
	@Override
	public int getKeySize() {
		return this.keyCount;
	}

	/* (non-Javadoc)
	 * @see de.rcblum.stream.deck.IStreamDeck#addKey(int, de.rcblum.stream.deck.items.StreamItem)
	 */
	@Override
	public void addKey(int keyId, StreamItem item) {
		if (keyId < this.keys.length && keyId >= 0) {
			this.keys[keyId] = item;
			queue(new IconUpdater(keyId, item.getIcon()));
		}
	}

	/* (non-Javadoc)
	 * @see de.rcblum.stream.deck.IStreamDeck#addKeyListener(de.rcblum.stream.deck.event.StreamKeyListener)
	 */
	@Override
	public boolean addKeyListener(StreamKeyListener listener) {
		return this.listerners.add(listener);
	}


	/* (non-Javadoc)
	 * @see de.rcblum.stream.deck.IStreamDeck#remvoeKeyListener(de.rcblum.stream.deck.event.StreamKeyListener)
	 */
	@Override
	public boolean removeKeyListener(StreamKeyListener listener) {
		return this.listerners.remove(listener);
	}

	/* (non-Javadoc)
	 * @see de.rcblum.stream.deck.IStreamDeck#drawImage(int, de.rcblum.stream.deck.util.SDImage)
	 */
	@Override
	public void drawImage(int keyIndex, SDImage imgData) {
		queue(new IconUpdater(keyIndex, imgData));
	}

	public synchronized void internalDrawImage(int keyIndex, SDImage imgData) {
		byte[] page1 = generatePage1(keyIndex, imgData.sdImage);
		byte[] page2 = generatePage2(keyIndex, imgData.sdImage);
		this.hidDevice.setOutputReport((byte) 0x02, page1, page1.length);
		this.hidDevice.setOutputReport((byte) 0x02, page2, page2.length);
	}

	private void fireKeyChangedEvent(int i, boolean keyPressed) {
		KeyEvent event = new KeyEvent(this, i, keyPressed ? Type.PRESSED : Type.RELEASED_CLICKED);
		this.recievePool.add(event);
	}

	/**
	 * Generates HID-Report Page 1/2 to update an image of one stream deck key
	 * 
	 * @param keyId
	 *            Id of the key to be updated
	 * @param imgData
	 *            image data in the bgr-format
	 * @return HID-Report in byte format ready to be send to the stream deck
	 */
	private byte[] generatePage1(int keyId, byte[] imgData) {
		for (int i = 0; i < PAGE_1_HEADER.length; i++) {
			p1[i] = PAGE_1_HEADER[i];
		}
		if (imgData != null) {
			for (int i = 0; i < imgData.length && i < NUM_FIRST_PAGE_PIXELS * 3; i++) {
				p1[PAGE_1_HEADER.length + i] = imgData[i];
			}
		}
		p1[4] = (byte) (keyId + 1);
		return p1;
	}

	/**
	 * Generates HID-Report Page 2/2 to update an image of one stream deck key
	 * 
	 * @param keyId
	 *            Id of the key to be updated
	 * @param imgData
	 *            image data in the bgr-format
	 * @return HID-Report in byte format ready to be send to the stream deck
	 */
	private byte[] generatePage2(int keyId, byte[] imgData) {
		for (int i = 0; i < PAGE_2_HEADER.length; i++) {
			p2[i] = PAGE_2_HEADER[i];
		}
		if (imgData != null) {
			for (int i = 0; i < NUM_SECOND_PAGE_PIXELS * 3 && i < imgData.length; i++) {
				p2[PAGE_2_HEADER.length + i] = imgData[(NUM_FIRST_PAGE_PIXELS * 3) + i];
			}
		}
		p2[4] = (byte) (keyId + 1);
		return p2;
	}

	/* (non-Javadoc)
	 * @see de.rcblum.stream.deck.IStreamDeck#getHidDevice()
	 */
	@Override
	public HidDevice getHidDevice() {
		return hidDevice;
	}

	@Override
	public void onInputReport(HidDevice source, byte reportID, byte[] reportData, int reportLength) {
		if (reportID == 1) {
			for (int i = 0; i < FRCStreamDeck.this.getKeySize() && i < reportLength; i++) {
				if (keysPressed[i] != (reportData[i] == 0x01)) {
					fireKeyChangedEvent(i, reportData[i] == 0x01);
					keysPressed[i] = reportData[i] == 0x01;
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see de.rcblum.stream.deck.IStreamDeck#removeKey(int)
	 */
	@Override
	public void removeKey(int keyId) {
		if (keyId < this.keys.length && keyId >= 0 && this.keys[keyId] != null) {
			this.keys[keyId] = null;
			queue(new IconUpdater(keyId, BLACK_ICON));
		}
	}

	/* (non-Javadoc)
	 * @see de.rcblum.stream.deck.IStreamDeck#reset()
	 */
	@Override
	public void reset() {
		this.queue(new Resetter());
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != null)
				this.queue(new IconUpdater(i, keys[i].getIcon()));
			else
				this.queue(new IconUpdater(i, BLACK_ICON));
		}
	}

	/* (non-Javadoc)
	 * @see de.rcblum.stream.deck.IStreamDeck#setBrightness(int)
	 */
	@Override
	public void setBrightness(int brightness) {
		brightness = brightness > 99 ? 99 : brightness < 0 ? 0 : brightness;
		this.brightness[5] = (byte) brightness;
		this.queue(new BrightnessUpdater());
	}

	private void queue(Runnable payload) {
		if(this.running)
			this.sendPool.add(payload);
	}

	/* (non-Javadoc)
	 * @see de.rcblum.stream.deck.IStreamDeck#stop()
	 */
	@Override
	public void stop() {
		this.running = false;
	}

	/* (non-Javadoc)
	 * @see de.rcblum.stream.deck.IStreamDeck#waitForCompletion()
	 */
	@Override
	public void waitForCompletion() {
		long time = 0;
		while (this.sendPool.isEmpty()) {
			try {
				Thread.sleep(50);
				time += 50;
				if (time > 2_000)
					return;
			} catch (InterruptedException e) {

				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * @see IStreamDeck#clearButton(int)
	 */
	@Override
	public void clearButton(int i) {
		queue(new IconUpdater(i, BLACK_ICON));
	}

	public StreamItem[] getItems() {
		return this.keys;
	}
	
	@Override
	public boolean isHardware() {
		return true;
	}

	/**
	 * Manually Pushing a button at the given id.
	 * 
	 * @param no Number of the button to be pushed, 0 - 14, right top to left
	 *           bottom.
	 */
	public void pushButton(int no) {
		no = no > 14 ? 14 : no < 0 ? 0 : no;
		KeyEvent evnt = new KeyEvent(this, no, Type.RELEASED_CLICKED);
		recievePool.add(evnt);
	}

	/**
	 * Manually presses a button at the given id until {@link #releaseButton(int)}
	 * is called.
	 * 
	 * @param no Number of the button to be pushed, 0 - 14, right top to left
	 *           bottom.
	 */
	public void pressButton(int no) {
		no = no > 14 ? 14 : no < 0 ? 0 : no;
		KeyEvent evnt = new KeyEvent(this, no, Type.PRESSED);
		recievePool.add(evnt);
	}

	/**
	 * Manually releases a button at the given id. If the button is not pressed, it
	 * will be pushed instead.
	 * 
	 * @param no Number of the button to be pushed, 0 - 14, right top to left
	 *           bottom.
	 */
	public void releaseButton(int no) {;
		this.pushButton(no);
	}

	public int getRowWidth(){
		return 5;
	};

	public int getRowOffset(){
		return 3;
	};
	String name;
	public String getName(){
		return name;
	}
	public void setName(String name){
		this.name = name;
	}
}
