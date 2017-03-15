import java.util.ArrayList;

import edu.wpi.first.wpilibj.networktables.*;
import edu.wpi.first.wpilibj.tables.*;
import edu.wpi.cscore.*;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.*;
import org.opencv.core.Core.*;
import org.opencv.core.Core;

import test2.GripPipeline;

public class Main {

	static NetworkTable sdTable;

	static GearCameraThread gct;
  public static void main(String[] args) {
    // Loads our OpenCV library. This MUST be included
    System.loadLibrary("opencv_java310");

    NetworkTable.setClientMode();
    NetworkTable.setTeam(4587);

    NetworkTable.initialize();

    int streamPort = 1185;

    MjpegServer inputStream = new MjpegServer("MJPEG Server", streamPort);
    
    UsbCamera camera = setUsbCamera(0, inputStream);
    camera.setResolution(320,240);
    CvSink imageSink = new CvSink("CV Image Grabber");
    imageSink.setSource(camera);

    CvSource imageSource = new CvSource("CV Image Source", VideoMode.PixelFormat.kMJPEG, 640, 480, 30);
    MjpegServer cvStream = new MjpegServer("CV Image Stream", 1186);
    cvStream.setSource(imageSource);
    GripPipeline gp = new GripPipeline();
    Mat inputImage = new Mat();
    Mat hsv = new Mat();
	sdTable = NetworkTable.getTable("SmartDashboard");
	sdTable.putString("pi","hello");
	gct = new GearCameraThread();
	gct.run();
	
    while (true) {
    	long start = System.nanoTime();
      long frameTime = imageSink.grabFrame(inputImage);
      if (frameTime == 0) continue;

      //Imgproc.cvtColor(inputImage, hsv, Imgproc.COLOR_BGR2HSV);
      //Core.inRange(hsv, new Scalar(70.0, 200.0, 70),
  		//	new Scalar(100.0, 255.0, 120), hsv);
      gp.process(inputImage);
      //imageSource.putFrame(gp.hsvThresholdOutput());
	imageSource.putFrame(inputImage);
	sdTable.putNumber("pi.centerline", gp.centerline);
	sdTable.putNumber("pi.height", gp.height);
	sdTable.putNumber("pi.time", (System.nanoTime() - start) / 1000000);
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
    UsbCamera camera = new UsbCamera("GearCamera", cameraId);
    server.setSource(camera);
    return camera;
  }
  
  private static class GearCameraThread extends Thread
  {
	  String oldMode = "startUp";
	  MjpegServer cameraStream;
	  UsbCamera camera2;
	  int streamCameraPort = 1187;
	  CvSink imageSink;
	  MjpegServer cvStream;
	  GripPipeline gp;
	  Mat inputImage;
	  Mat hsv;
	  CvSource imageSource;
	  public void run()
	  {
		  while(true)
		  {
			  String mode = sdTable.getString("GearCameraMode", "HumanVision");
			  if (oldMode.equals("startUp"))
			  {
				    cameraStream = new MjpegServer("MJPEG", streamCameraPort);
				    camera2 = setUsbCamera2(1,cameraStream);
				    camera2.setResolution(320,240);
				    imageSink = new CvSink("CV Image Grabber");
				    imageSink.setSource(camera2);
				    imageSource = new CvSource("CV Image Source", VideoMode.PixelFormat.kMJPEG, 640, 480, 30);
				    cvStream = new MjpegServer("CV Image Stream", 1188);
				    cvStream.setSource(imageSource);
				    gp = new GripPipeline();
				    inputImage = new Mat();
				    hsv = new Mat();
			  }
			  if(mode.equals("HumanVision"))
			  {
				  if(oldMode.equals("HumanVision") == false)
				  {
					    camera2.setWhiteBalanceAuto();
					    camera2.setExposureAuto();
				  }
				  try
				  {
					  currentThread().sleep(100);
				  }catch(Exception e)
				  {
					  
				  }
			  }
			  else
			  {
				  if(oldMode.equals("ComputerVision") == false)
				  {
					  VideoProperty wbm = camera2.getProperty("white_balance_temperature");
					  camera2.setWhiteBalanceManual(wbm.getMin());
					  camera2.setExposureManual(0);
				  }
					
				long start = System.nanoTime();
				long frameTime = imageSink.grabFrame(inputImage);
				if (frameTime != 0) 
				{
					//Imgproc.cvtColor(inputImage, hsv, Imgproc.COLOR_BGR2HSV);
					//Core.inRange(hsv, new Scalar(70.0, 200.0, 70),
					//	new Scalar(100.0, 255.0, 120), hsv);
					gp.processGear(inputImage);
					//imageSource.putFrame(gp.hsvThresholdOutput());
					imageSource.putFrame(inputImage);
					sdTable.putNumber("piGear.centerline", gp.centerline);
					sdTable.putNumber("piGear.height", gp.height);
					sdTable.putNumber("piGear.time", (System.nanoTime() - start) / 1000000);
				}
			  }
			  oldMode = mode;
		  }
	  }
  }
}
