import java.util.ArrayList;

import edu.wpi.first.wpilibj.networktables.*;
import edu.wpi.first.wpilibj.tables.*;
import edu.wpi.cscore.*;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import test2.GripPipeline;

public class Main {
  public static void main(String[] args) {
    // Loads our OpenCV library. This MUST be included
    System.loadLibrary("opencv_java310");

    NetworkTable.setClientMode();
    NetworkTable.setTeam(4587);

    NetworkTable.initialize();

    int streamPort = 1185;

    MjpegServer inputStream = new MjpegServer("MJPEG Server", streamPort);
    
    UsbCamera camera = setUsbCamera(0, inputStream);
    camera.setResolution(640,480);
    int streamCameraPort = 1187;
    MjpegServer cameraStream = new MjpegServer("MJPEG", streamCameraPort);
    UsbCamera camera2 = setUsbCamera2(1,cameraStream);
    camera2.setResolution(640,480);
    CvSink imageSink = new CvSink("CV Image Grabber");
    imageSink.setSource(camera);

    CvSource imageSource = new CvSource("CV Image Source", VideoMode.PixelFormat.kMJPEG, 640, 480, 30);
    MjpegServer cvStream = new MjpegServer("CV Image Stream", 1186);
    cvStream.setSource(imageSource);
    GripPipeline gp = new GripPipeline();
    Mat inputImage = new Mat();
    Mat hsv = new Mat();
	NetworkTable sdTable = NetworkTable.getTable("SmartDashboard");
	sdTable.putString("pi","hello");
    while (true) {
      long frameTime = imageSink.grabFrame(inputImage);
      if (frameTime == 0) continue;

      //Imgproc.cvtColor(inputImage, hsv, Imgproc.COLOR_BGR2HSV);
      gp.process(inputImage);
      //imageSource.putFrame(gp.hsvThresholdOutput());
	imageSource.putFrame(inputImage);
	sdTable.putNumber("pi.centerline", gp.centerline);
	sdTable.putNumber("pi.height", gp.height);
    }
  }

  private static UsbCamera setUsbCamera(int cameraId, MjpegServer server) {
    UsbCamera camera = new UsbCamera("CoprocessorCamera", cameraId);
    System.out.println("----------");
    for ( VideoProperty vp: camera.enumerateProperties() ) {
        System.out.println("Property \""+vp.getName()+"\"="+vp.get()+" ("+vp.getKind()+") "+vp.getMin()+"/"+vp.getMax());
    }
    VideoProperty wbm = camera.getProperty("white_balance_temperature");
    camera.setWhiteBalanceManual(wbm.getMin());
    camera.setExposureManual(0);
    System.out.println("----------");
    for ( VideoProperty vp: camera.enumerateProperties() ) {
        System.out.println("Property \""+vp.getName()+"\"="+vp.get()+" ("+vp.getKind()+") "+vp.getMin()+"/"+vp.getMax());
    }
    server.setSource(camera);
    return camera;
  }
  private static UsbCamera setUsbCamera2(int cameraId, MjpegServer server) {
    UsbCamera camera = new UsbCamera("HumanCamera", cameraId);
    server.setSource(camera);
    return camera;
  }
}
