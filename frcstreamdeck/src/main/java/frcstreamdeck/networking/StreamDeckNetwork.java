package frcstreamdeck.networking;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import java.io.IOException;

import org.opencv.core.Core;
import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.math.jni.WPIMathJNI;
import edu.wpi.first.util.WPIUtilJNI;

public class StreamDeckNetwork {
    public static NetworkTableInstance inst;
    public static NetworkTable smartDashboardTable;
    public static void initNetwork(){
        System.out.println("initNetwork");
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
        inst = NetworkTableInstance.create();
        inst.startClient4("localhost");
        //inst.setServerTeam(2930, 1735);
        inst.setServerTeam(2930); // where TEAM=190, 294, etc, or use inst.setServer("hostname") or similar
        inst.startDSClient(); // recommended if running on DS computer; this gets the robot IP from the DS
        smartDashboardTable = inst.getTable("SmartDashboard");

        // remiders:
        // use driverstation even in sim
        // use OutlineViewer
        // use gradle tasks not vscode stuff
    }
}