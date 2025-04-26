package frcstreamdeck.visual;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import frcstreamdeck.Constants;
import frcstreamdeck.StreamDeckFrc;
import frcstreamdeck.networking.StreamDeckNetwork;
import frcstreamdeck.streamDeck.FRCSoftStreamDeck;
import frcstreamdeck.streamDeck.IStreamDeckFRC;

public class FRCStreamDeckUI extends JFrame implements ActionListener{
    JPanel panel;
    JButton connect;
    JButton deckReset;
    JLabel statusLabel;
    JLabel[] streamDeckList = new JLabel[0];
    JButton[] streamDeckShufflers = new JButton[0];
    JTextArea deckCount;
    public FRCStreamDeckUI(){
        setVisible(true);
        this.setSize(600,400);
        this.setTitle("FRCStreamDeck");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);

        panel = new JPanel();
        panel.setSize(600,400);
        panel.setLocation(0,0);
        panel.setLayout(null);
        this.add(panel);

        connect = new JButton("connect");
        connect.setBounds(20, 20, 100, 50);
        connect.setActionCommand("connect");
        panel.add(connect);
        connect.addActionListener(this);

        deckReset = new JButton("deckReset");
        deckReset.setBounds(20, 120, 100, 50);
        deckReset.setActionCommand("deckReset");
        panel.add(deckReset);
        deckReset.addActionListener(this);

        JLabel deckCountText = new JLabel("Deck Count:");
        deckCountText.setBounds(140, 120, 100, 50);
        panel.add(deckCountText);

        deckCount = new JTextArea("2");
        deckCount.setBounds(220, 137, 20, 20);
        panel.add(deckCount);

        statusLabel = new JLabel("Status: ");
        statusLabel.setBounds(140, 20, 150, 50);
        panel.add(statusLabel);

        this.revalidate();
        this.repaint();
    }
    public void periodic(){
        String text = "<html>";
        text += "Status: " + (StreamDeckNetwork.inst.isConnected()? "Connected":"Disconnected") + "<br>";
        text += "Real decks: " + StreamDeckFrc.getRealDeckCount();
        text += "</html>";
        statusLabel.setText(text);
        try {
            for (int i = 0; i < streamDeckList.length; i++) {
                    if(StreamDeckFrc.getKeyIds(i).size()>0){
                        streamDeckList[i].setBackground(Color.ORANGE);
                    }else{
                        streamDeckList[i].setBackground(Color.WHITE);
                    }
            }
        } catch (NullPointerException e) {
           // decks not initialised
        }
    }
    private void resetNames(){
        try{
            for (int i = 0; i < Constants.STREAMDECK_COUNT; i++) {
                streamDeckList[i].setText(StreamDeckFrc.getNameOfDeck(i));
            }
        } catch (NullPointerException e) {
            // decks not initialised
        }
        this.revalidate();
        this.repaint();
    }
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()){
            case "connect":
                StreamDeckFrc.connectToRobot();
                break;
            case "deckReset":
                try {
                    Constants.STREAMDECK_COUNT = Integer.valueOf(deckCount.getText());
                } catch (NumberFormatException ex) {}
                System.out.println(Constants.STREAMDECK_COUNT);
                StreamDeckFrc.resetDecks();
                for (JLabel jLabel : streamDeckList) {
                    if(jLabel==null) continue;
                    panel.remove(jLabel);
                }
                for (JButton jButton : streamDeckShufflers) {
                    if(jButton==null) continue;
                    panel.remove(jButton);
                }
                streamDeckList = new JLabel[Constants.STREAMDECK_COUNT];
                streamDeckShufflers = new JButton[Constants.STREAMDECK_COUNT*2-2];
                int startpos = 100;
                int ysize = 20;
                for (int i = 0; i < Constants.STREAMDECK_COUNT; i++) {
                    streamDeckList[i] = new JLabel(StreamDeckFrc.getNameOfDeck(i));
                    streamDeckList[i].setBounds(400, startpos+ysize*i, 100, ysize);
                    streamDeckList[i].setOpaque(true);
                    panel.add(streamDeckList[i]);
                    if(i!=Constants.STREAMDECK_COUNT-1){
                        streamDeckShufflers[i*2] = new JButton("\\/");
                        streamDeckShufflers[i*2].setBounds(320, startpos+ysize*i, ysize*2, ysize);
                        streamDeckShufflers[i*2].setActionCommand("moveUp");
                        panel.add(streamDeckShufflers[i*2]);
                        streamDeckShufflers[i*2].addActionListener(this);
                    }
                    if(i!=0){
                        streamDeckShufflers[i*2-1] = new JButton("/\\");
                        streamDeckShufflers[i*2-1].setBounds(360, startpos+ysize*i, ysize*2, ysize);
                        streamDeckShufflers[i*2-1].setActionCommand("moveDown");
                        panel.add(streamDeckShufflers[i*2-1]);
                        streamDeckShufflers[i*2-1].addActionListener(this);
                    }
                }
                resetNames();
                this.revalidate();
                this.repaint();
                break;
            case "moveUp":
                for (int i = 0; i < streamDeckShufflers.length; i++) {
                    if(e.getSource() == streamDeckShufflers[i]){
                        if(i==streamDeckShufflers.length-1)
                        return;
                        StreamDeckFrc.swapDecks(i/2, i/2+1);
                        resetNames();
                    }
                }
                break;
            case "moveDown":
                for (int i = 0; i < streamDeckShufflers.length; i++) {
                    if(e.getSource() == streamDeckShufflers[i]){
                        if(i==0)
                        return;
                        StreamDeckFrc.swapDecks(i/2+1, i/2);
                        resetNames();
                    }
                }
                break;
            default:
                System.out.println("Unassigned action: " + e.getActionCommand());
            break;
        }
    }
}
