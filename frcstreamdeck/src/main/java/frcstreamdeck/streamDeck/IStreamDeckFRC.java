package frcstreamdeck.streamDeck;

import de.rcblum.stream.deck.device.IStreamDeck;

public interface IStreamDeckFRC extends IStreamDeck{
    abstract public int getRowWidth();
    abstract public int getRowOffset();
}