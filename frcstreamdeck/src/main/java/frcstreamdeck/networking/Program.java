package frcstreamdeck.networking;

import edu.wpi.first.networktables.DoubleSubscriber;
import edu.wpi.first.networktables.DoubleTopic;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import java.io.IOException;

import org.opencv.core.Core;

import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.math.jni.WPIMathJNI;
import edu.wpi.first.util.WPIUtilJNI;
public class Program {
    public static void main(String[] args) throws IOException {
        NetworkTablesJNI.Helper.setExtractOnStaticLoad(false);
        WPIUtilJNI.Helper.setExtractOnStaticLoad(false);
        WPIMathJNI.Helper.setExtractOnStaticLoad(false);
        CameraServerJNI.Helper.setExtractOnStaticLoad(false);
        CombinedRuntimeLoader.loadLibraries(Program.class, "wpiutiljni", "wpimathjni", "ntcorejni",
            Core.NATIVE_LIBRARY_NAME, "cscorejni");
        new Program().run();
    }
    public void run() {
        NetworkTableInstance inst = NetworkTableInstance.getDefault();
        NetworkTable table = inst.getTable("datatable");
        DoubleTopic dt = new DoubleTopic(inst, 1);
        DoubleSubscriber xSub = table.getDoubleTopic("x").subscribe(0.0);
        DoubleSubscriber ySub = table.getDoubleTopic("y").subscribe(0.0);
        inst.startClient("example client");
        inst.setServerTeam(2930); // where TEAM=190, 294, etc, or use inst.setServer("hostname") or similar
        inst.startDSClient(); // recommended if running on DS computer; this gets the robot IP from the DS
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                System.out.println("interrupted");
                return;
            }
            double x = xSub.get();
            double y = ySub.get();
            System.out.println("X: " + x + " Y: " + y);
        }
    }
}