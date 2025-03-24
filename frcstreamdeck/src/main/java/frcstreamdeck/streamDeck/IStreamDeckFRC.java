package frcstreamdeck.streamDeck;

import de.rcblum.stream.deck.device.IStreamDeck;

// this class just lets the keyToString method get some more values
public interface IStreamDeckFRC extends IStreamDeck{
    abstract public int getRowWidth();
    abstract public int getRowOffset();
}