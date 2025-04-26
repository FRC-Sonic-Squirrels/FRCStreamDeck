package frcstreamdeck.networking;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;

public class StreamDeckNetwork {
    public static NetworkTableInstance inst;
    public static NetworkTable smartDashboardTable;
    public static void initNetwork(){
        inst = NetworkTableInstance.create();
    }
    public static void connect(){
        inst.disconnect();
        inst.startClient4("StreamDeckProgram");
        // this allows the program to connect easily during sim but requires the ds to be open
        inst.startDSClient();
        smartDashboardTable = inst.getTable("SmartDashboard");
    }
}