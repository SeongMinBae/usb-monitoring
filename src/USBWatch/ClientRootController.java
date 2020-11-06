package USBWatch;

import java.awt.Desktop;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import com.github.sarxos.webcam.Webcam;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

import USBWatch.java.ToggleSwitch;
import USBWatch.java.WebCamInfo;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ClientRootController implements Initializable{
	@FXML TextArea textArea;
	@FXML ListView<String> dirList;
	@FXML ListView<String> fList;
	@FXML Button clear;
	@FXML Button capture;
	@FXML Button option;
	@FXML SplitPane allList;
	@FXML BorderPane allRoot;
	@FXML Slider slider;
	@FXML ImageView usbIcon;
	@FXML Button normalOption;
	@FXML Text info;
	@FXML Button detailLook;
	@FXML PieChart pieChart;
	@FXML ImageView defaultBackground;
	@FXML ToggleSwitch prohibitCopy;
	@FXML ToggleSwitch opacityShort;
	@FXML ToggleSwitch reserveClose;
	@FXML Button mailOption;
	@FXML TextField focus;
	
	/*
	 * 웹 캠관련 FXML주입변수
	 */
	@FXML Button camCapture;
	@FXML Button btnStartCamera;
	@FXML Button btnStopCamera;
	@FXML Button btnDisposeCamera;
	@FXML ComboBox<WebCamInfo> cbCameraOptions;
	@FXML BorderPane bpWebCamPaneHolder;
	@FXML FlowPane fpBottomPane;
	@FXML ImageView imgWebCamCapturedImage;
	//
	
	/*
	 * 웹 캠관련 변수
	 */
	private BufferedImage grabbedImage;
	private Webcam selWebCam = null;
	private boolean stopCamera = false;
	private ObjectProperty<Image> imageProperty = new SimpleObjectProperty<Image>();
	private String cameraListPromptText = "Choose Camera";
	//
	
	Stage captureDlg, informationDlg, optionDlg, dialog, primaryStage;
	public Stage passInput;
	String usb, usbRead, usbWrite, warning, mailWarning, shortcutKey;
	public String password;
	boolean unusualActionExitFlag, TaskManagerCheck1, TaskManagerCheck2, TaskManagerCheck3
			,optionOnOff, optionStop, mailOptionOnOff, mailOptionStop, mailManagerCheck1
			, mailManagerCheck2, normalOption1Flag, normalOption2Flag, normalOption3Flag
			, timeOff;
	public boolean closeState = true;
	Thread captureThread, usbConnectCheckThread, infoThread, camCaptureThread;
	int dirListCnt, fListCnt, optionCnt, mailOptionCnt, count = 0, usbIconCheck=0, reserveTime;
	Map<Integer,String> dirMap = new HashMap<>();
	Map<Integer,String> fMap = new HashMap<>();
	List<String> eng = Arrays.asList("d", "e", "f", "g", "h", "i", "j", "k", 
			"l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z");
	Date date = new Date();
	SimpleDateFormat sdate = new SimpleDateFormat("yyyy-MM-dd,hh:mm:ss a"); 
	NumberFormat nf = NumberFormat.getInstance();
	public ExecutorService executorService = Executors.newCachedThreadPool();
	WatchServiceThread wst;
	FileWriter fos;
	Text executeState;
	Image usb_green;
	Image usb_red;
	
	Clipboard clipboard = Clipboard.getSystemClipboard();
	
	
	
	@SuppressWarnings("deprecation")
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		usb_green = new Image("USBWatch/img/usb_green.png");
		usb_red = new Image("USBWatch/img/usb_red.png");
		
		/*
		 * 웹 캠관련 이벤트 처리
		 */
		ObservableList<WebCamInfo> options = FXCollections.observableArrayList();
		int webCamCounter = 0;
		for (Webcam webcam : Webcam.getWebcams()) {
			WebCamInfo webCamInfo = new WebCamInfo();
			webCamInfo.setWebCamIndex(webCamCounter);
			webCamInfo.setWebCamName(webcam.getName());
			options.add(webCamInfo);
			webCamCounter++;
		}
		
		cbCameraOptions.setItems(options);
		cbCameraOptions.setPromptText(cameraListPromptText);
		cbCameraOptions.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<WebCamInfo>() {
			@Override
			public void changed(ObservableValue<? extends WebCamInfo> arg0, WebCamInfo arg1, WebCamInfo arg2) {
				if (arg2 != null) {
					//System.out.println("WebCam Index: " + arg2.getWebCamIndex() + ": WebCam Name:" + arg2.getWebCamName());
					initializeWebCam(arg2.getWebCamIndex());
					camCapture.setVisible(true);
					camCapture.setDisable(false);
				}
			}
		});
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				setImageViewSize();
			}
		});
		camCapture.setOnAction(ev1 ->{
			try{
				if(closeState)return;
				if(camCapture.getText().equals("캠 캡처 중지")){
					Stage passCheck = new Stage(StageStyle.UTILITY);
					passCheck.initModality(Modality.WINDOW_MODAL);
					passCheck.initOwner(primaryStage);
					passCheck.setTitle("비밀번호 확인");
					//passCheck.setAlwaysOnTop(true);
					passCheck.toFront();
					passCheck.setResizable(false);
				    FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/password.fxml"));
				    Parent root;
					try{
						root = (Parent)loader.load();
						Button pwdOK = (Button)root.lookup("#btnOK");
						TextField pwd = (TextField)root.lookup("#password");
						Text text = (Text)root.lookup("#text");
						
						pwdOK.setOnAction(ev3 -> {
							if(pwd.getText().equals(""))text.setFill(Paint.valueOf("RED"));
					    	else if(!pwd.getText().equals(password))text.setFill(Paint.valueOf("RED"));
					    	else{
					    		camCapture.setText("캠 캡처");
					    		camCaptureThread.stop();
					    		passCheck.close();
					    		return;
					    	}
						});
						Scene scene = new Scene(root);
						passCheck.setScene(scene);
						passCheck.show();
						return;
					}catch(Exception e){}
		    	 }
			     captureDlg = new Stage(StageStyle.UTILITY);
			     captureDlg.initModality(Modality.WINDOW_MODAL);
			     captureDlg.initOwner(primaryStage);
			     captureDlg.setTitle("캡처설정");
			    // captureDlg.setAlwaysOnTop(true);
			     captureDlg.toFront();
			     captureDlg.setResizable(false);
			     FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/cap.fxml"));
			     Parent root = (Parent)loader.load();
			     Button btnOK = (Button)root.lookup("#btnOK");
			     Button looking = (Button)root.lookup("#looking");
			     TextField path = (TextField)root.lookup("#path");
			     TextField time = (TextField)root.lookup("#time");
			     Text warning = (Text)root.lookup("#warning");
			     
			     looking.setOnAction(ev -> {
			    	 if(closeState == false){
				    	 DirectoryChooser directoryChooser = new DirectoryChooser();
				    	 directoryChooser.setTitle("디렉터리 경로 찾기");
				    	 File selectedFile = directoryChooser.showDialog(primaryStage);
				    	 String selectedFilePath = selectedFile.getPath();
				    	 path.setText(selectedFilePath);
				     }
			     });
			     
			     btnOK.setOnAction(ev2 -> {
			     	if(closeState == true)captureDlg.close();
			    	if(path.getText().equals("") && time.getText().equals("")){
			    		warning.setText("경로와 시간을 입력해주세요");
			    		return;
			    	}else if(path.getText().equals("")){
			    		warning.setText("경로를 입력해주세요");
			    		return;
			    	}else if(time.getText().equals("")){
			    		warning.setText("시간을 입력해주세요");
			    		return;
			    	}
			    	 
		    		try {
						fos = new FileWriter(path.getText());
		    		} catch (Exception e1) {}
	    			camCaptureThread = new Thread(){
	    				public void run(){
			    			try {
								Robot robot = new Robot();
									while(true){
										if(closeState){
											return;
										}
										robot.delay(Integer.valueOf(time.getText()) * 1000);
									    try {
									    	count++;
									    	BufferedImage bf = selWebCam.getImage();
											ImageIO.write(bf, "jpg", new File(path.getText() + "/" + "camCapture" + count + ".jpg"));	
											textArea.appendText("Cam Capture" + count + "가 저장되었습니다\n");
									    } catch (Exception e) {}											
									}
								} catch (Exception e) {}
			    			};
			    		};
				    	camCaptureThread.start();
				 		Platform.runLater(()->camCapture.setText("캠 캡처 중지"));
				 		captureDlg.close();
			     });
			     	Scene scene = new Scene(root);
			     	captureDlg.setScene(scene);
			     	captureDlg.show();
			    }catch(Exception eu){}
		});
		//
		
		usbIcon.setOnMouseClicked(ev ->{
			if(closeState)return;
			informationDlg = new Stage(StageStyle.UTILITY);
			informationDlg.initModality(Modality.WINDOW_MODAL);
			informationDlg.initOwner(primaryStage);
			informationDlg.setTitle("장치정보");
			//informationDlg.setAlwaysOnTop(true);
			informationDlg.toFront();
			informationDlg.setResizable(false);
		    FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/info.fxml"));
		    Parent root;
			try {
				root = (Parent)loader.load();
				Text info = (Text)root.lookup("#info");
				Button detailLook = (Button)root.lookup("#detailLook");
				PieChart pieChart = (PieChart)root.lookup("#pieChart");
				executeState = (Text)root.lookup("#executeState");
				
				File file = new File(usb + ":\\");
				
				info.setText(usb + ":\\" + "드라이브");
				if(file.canExecute() && closeState == false)executeState.setText("연결중");
				else{
					executeState.setText("연결끊김");
				}
				
				long useSize, totalSize;
				useSize = file.getTotalSpace() - file.getUsableSpace();
				totalSize = file.getTotalSpace();
				
				pieChart.setData(FXCollections.observableArrayList(
						new PieChart.Data("사용 가능 공간", totalSize),		
						new PieChart.Data("사용 중인 공간", useSize)
				));
				detailLook.setOnAction(ev2 -> {
					try {
						Popup popup = new Popup();
						
						Parent parent = FXMLLoader.load(getClass().getResource("fxml/popup.fxml"));
						ImageView imageView = (ImageView) parent.lookup("#imgMessage");
						imageView.setImage(new Image(getClass().getResource("/USBWatch/img/dialog-info.png").toString()));
						imageView.setOnMouseClicked(event->popup.hide());
						Label lblMessage = (Label)parent.lookup("#lblMessage");
						
						if(file.canRead())usbRead = "가능";
						else usbRead = "불가능";
						
						if(file.canWrite())usbWrite = "가능";
						else usbWrite = "불가능";
				
						lblMessage.setText("읽기 가능 여부 : " + usbRead + "\n" + 
												"쓰기 가능 여부 : " + usbWrite + "\n" +
												"전체용량 : " + nf.format(file.getTotalSpace()/Math.pow(1024, 3)) + "GB" +
												" [ " + file.getTotalSpace() + "바이트 ]" + "\n" +
												"사용한 용량 : " + nf.format(file.getTotalSpace()/Math.pow(1024, 3) - file.getUsableSpace()/Math.pow(1024, 3)) + "GB" +
												" [ " + String.valueOf(file.getTotalSpace() - file.getUsableSpace()) + "바이트 ]");
							
						popup.centerOnScreen();
						
						popup.getContent().add(parent);
						popup.setAutoHide(true);	
						popup.show(informationDlg);
					} catch (Exception e) {}
				});
				
				detailLook.setOnMouseMoved(ev2 -> {
					try {
						Popup popup = new Popup();
						
						Parent parent = FXMLLoader.load(getClass().getResource("fxml/popup.fxml"));
						ImageView imageView = (ImageView) parent.lookup("#imgMessage");
						imageView.setImage(new Image(getClass().getResource("/USBWatch/img/dialog-info.png").toString()));
						imageView.setOnMouseClicked(event->popup.hide());
						Label lblMessage = (Label)parent.lookup("#lblMessage");
						
						if(file.canRead())usbRead = "가능";
						else usbRead = "불가능";
						
						if(file.canWrite())usbWrite = "가능";
						else usbWrite = "불가능";
				
						lblMessage.setText("읽기 가능 여부 : " + usbRead + "\n" + 
											"쓰기 가능 여부 : " + usbWrite + "\n" +
											"전체용량 : " + nf.format(file.getTotalSpace()/Math.pow(1024, 3)) + "GB" +
											" [ " + file.getTotalSpace() + "바이트 ]" + "\n" +
											"사용한 용량 : " + nf.format(file.getTotalSpace()/Math.pow(1024, 3) - file.getUsableSpace()/Math.pow(1024, 3)) + "GB" +
											" [ " + String.valueOf(file.getTotalSpace() - file.getUsableSpace()) + "바이트 ]");
						
						popup.centerOnScreen();
						
						popup.getContent().add(parent);
						popup.setAutoHide(true);	
						popup.show(informationDlg);
					} catch (Exception e) {}
				});					
					
				Scene scene = new Scene(root);
				informationDlg.setScene(scene);
			    informationDlg.show();    
			} catch (Exception e) {}
		});
		
		wst = new WatchServiceThread();
		wst.start();
		
		nf.setMaximumFractionDigits(2);
		
		slider.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				if(newValue.doubleValue() < 0.015)return;
				dialog.setOpacity(newValue.doubleValue());
			}
		});
		
		dirList.setOnMouseClicked(ev ->{
			if(ev.getClickCount() == 2 && !(ev.getButton().compareTo(MouseButton.SECONDARY) == 0)){
				try {
					File myfile = new File(dirMap.get(dirList.getSelectionModel().getSelectedIndex()));
					String path = myfile.getAbsolutePath();
					File dir = new File(path.substring(0, path.lastIndexOf(File.separator)));
					if (Desktop.isDesktopSupported())Desktop.getDesktop().open(dir);
				} catch (Exception e) {}
			}
			if(ev.getButton().compareTo(MouseButton.SECONDARY) == 0){
				dirList.setCellFactory(c -> {
					ListCell<String> cell = new ListCell<>();
				    ContextMenu contextMenu = new ContextMenu();
				    MenuItem editItem = new MenuItem();
				    File file = new File(dirMap.get(dirList.getSelectionModel().getSelectedIndex()));
				    String canR = null;
				    String canW = null;
				      
				    if(file.canRead())canR = "가능";
				    if(file.canWrite())canW = "가능";
				    editItem.textProperty().bind(Bindings.format("읽기 모드[ %s ]", canR));
				      
				    MenuItem deleteItem = new MenuItem();
				    deleteItem.textProperty().bind(Bindings.format("쓰기 모드[ %s ]", canW));
				    contextMenu.getItems().addAll(editItem, deleteItem);

				    cell.textProperty().bind(cell.itemProperty());

				    cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
				    	if (isNowEmpty) {
				            cell.setContextMenu(null);
				        } else {
				            cell.setContextMenu(contextMenu);
				        }
				    });
				    return cell ;
				});
			}
		});
		
		fList.setOnMouseClicked(ev ->{
			if(ev.getClickCount() == 2 && !(ev.getButton().compareTo(MouseButton.SECONDARY) == 0)){
				try {
					File myfile = new File(fMap.get(fList.getSelectionModel().getSelectedIndex()));
					String path = myfile.getAbsolutePath();
					File dir = new File(path.substring(0, path.lastIndexOf(File.separator)));
					if (Desktop.isDesktopSupported())Desktop.getDesktop().open(dir);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if(ev.getButton().compareTo(MouseButton.SECONDARY) == 0){
				fList.setCellFactory(c -> {
					ListCell<String> cell = new ListCell<>();

				    ContextMenu contextMenu = new ContextMenu();
				    MenuItem editItem = new MenuItem();
				      
				    File file = new File(fMap.get(fList.getSelectionModel().getSelectedIndex()));
				      
				    String canR = null;
				    String canW = null;
				      
				    if(file.canRead())canR = "가능";
				    if(file.canWrite())canW = "가능";
				    editItem.textProperty().bind(Bindings.format("읽기 모드[ %s ]", canR));
				      
				    MenuItem deleteItem = new MenuItem();
				    deleteItem.textProperty().bind(Bindings.format("쓰기 모드[ %s ]", canW));
				    contextMenu.getItems().addAll(editItem, deleteItem);

				    cell.textProperty().bind(cell.itemProperty());

				    cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
					    if (isNowEmpty) {
					    	cell.setContextMenu(null);
					    } else {
					    	cell.setContextMenu(contextMenu);
					    }
					});
				    return cell ;
				});
			}
		});
		
		textArea.setEditable(false);
	
		clear.setOnAction(ev -> {
			if(closeState)return;
			Stage passCheck = new Stage(StageStyle.UTILITY);
			passCheck.initModality(Modality.WINDOW_MODAL);
			passCheck.initOwner(primaryStage);
			passCheck.setTitle("비밀번호 확인");
			//passCheck.setAlwaysOnTop(true);
			passCheck.toFront();
			passCheck.setResizable(false);
		    FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/password.fxml"));
		    Parent root;
			try{
				root = (Parent)loader.load();
				Button pwdOK = (Button)root.lookup("#btnOK");
				TextField pwd = (TextField)root.lookup("#password");
				Text text = (Text)root.lookup("#text");
				
				pwdOK.setOnAction(ev3 -> {
					if(pwd.getText().equals(""))text.setFill(Paint.valueOf("RED"));
			    	else if(!pwd.getText().equals(password))text.setFill(Paint.valueOf("RED"));
			    	else{
			    		passCheck.close();
			    		textArea.clear();
			    	}
				});
				Scene scene = new Scene(root);
				passCheck.setScene(scene);
				passCheck.show();
			}catch(Exception e){}
		});
		
		capture.setOnAction(ev1 ->{
			try{
				if(closeState)return;
				if(capture.getText().equals("캡처중지")){
					Stage passCheck = new Stage(StageStyle.UTILITY);
					passCheck.initModality(Modality.WINDOW_MODAL);
					passCheck.initOwner(primaryStage);
					passCheck.setTitle("비밀번호 확인");
					//passCheck.setAlwaysOnTop(true);
					passCheck.toFront();
					passCheck.setResizable(false);
				    FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/password.fxml"));
				    Parent root;
					try{
						root = (Parent)loader.load();
						Button pwdOK = (Button)root.lookup("#btnOK");
						TextField pwd = (TextField)root.lookup("#password");
						Text text = (Text)root.lookup("#text");
						
						pwdOK.setOnAction(ev3 -> {
							if(pwd.getText().equals(""))text.setFill(Paint.valueOf("RED"));
					    	else if(!pwd.getText().equals(password))text.setFill(Paint.valueOf("RED"));
					    	else{
					    		capture.setText("캡처");
					    		captureThread.stop();
					    		passCheck.close();
					    		return;
					    	}
						});
						Scene scene = new Scene(root);
						passCheck.setScene(scene);
						passCheck.show();
						return;
					}catch(Exception e){}
		    	 }
			     captureDlg = new Stage(StageStyle.UTILITY);
			     captureDlg.initModality(Modality.WINDOW_MODAL);
			     captureDlg.initOwner(primaryStage);
			     captureDlg.setTitle("캡처설정");
			     //captureDlg.setAlwaysOnTop(true);
			     captureDlg.toFront();
			     captureDlg.setResizable(false);
			     FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/cap.fxml"));
			     Parent root = (Parent)loader.load();
			     Button btnOK = (Button)root.lookup("#btnOK");
			     Button looking = (Button)root.lookup("#looking");
			     TextField path = (TextField)root.lookup("#path");
			     TextField time = (TextField)root.lookup("#time");
			     Text warning = (Text)root.lookup("#warning");
			     
			     looking.setOnAction(ev -> {
			    	 if(closeState == false){
				    	 DirectoryChooser directoryChooser = new DirectoryChooser();
				    	 directoryChooser.setTitle("디렉터리 경로 찾기");
				    	 File selectedFile = directoryChooser.showDialog(primaryStage);
				    	 String selectedFilePath = selectedFile.getPath();
				    	 path.setText(selectedFilePath);
				     }
			     });
			     
			     btnOK.setOnAction(ev2 -> {
			     	if(closeState == true)captureDlg.close();
			    	if(path.getText().equals("") && time.getText().equals("")){
			    		warning.setText("경로와 시간을 입력해주세요");
			    		return;
			    	}else if(path.getText().equals("")){
			    		warning.setText("경로를 입력해주세요");
			    		return;
			    	}else if(time.getText().equals("")){
			    		warning.setText("시간을 입력해주세요");
			    		return;
			    	}
			    	 
		    		try {
						fos = new FileWriter(path.getText());
		    		} catch (Exception e1) {}
		    			captureThread = new Thread(){
		    				public void run(){
				    			try {
									Robot robot = new Robot();
										while(true){
											if(closeState){
												return;
											}
											robot.delay(Integer.valueOf(time.getText()) * 1000);
											
										    BufferedImage bi = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height));
										    try {
										    	count++;
												ImageIO.write(bi, "jpg", new File(path.getText() + "/" + "screenShot" + count + ".jpg"));
												textArea.appendText("screenShot" + count + "가 저장되었습니다\n");
										    } catch (Exception e) {}											
										}
									} catch (Exception e) {}
				    			};
				    		};
				    	captureThread.start();
				 		Platform.runLater(()->capture.setText("캡처중지"));
				 		captureDlg.close();
			     });
			     	Scene scene = new Scene(root);
			     	captureDlg.setScene(scene);
			     	captureDlg.show();
			    }catch(Exception eu){}
		});
		
		normalOption.setOnAction(ev->{
			if(closeState)return;
			Stage passCheck = new Stage(StageStyle.UTILITY);
			passCheck.initModality(Modality.WINDOW_MODAL);
			passCheck.initOwner(primaryStage);
			passCheck.setTitle("비밀번호 확인");
			//passCheck.setAlwaysOnTop(true);
			passCheck.toFront();
			passCheck.setResizable(false);
			FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/password.fxml"));
			Parent root;
			try{
				root = (Parent)loader.load();
				Button pwdOK = (Button)root.lookup("#btnOK");
				TextField pwd = (TextField)root.lookup("#password");
				Text text = (Text)root.lookup("#text");
				
				pwdOK.setOnAction(ev3 -> {
					if(pwd.getText().equals(""))text.setFill(Paint.valueOf("RED"));
			        else if(!pwd.getText().equals(password))text.setFill(Paint.valueOf("RED"));
			    	else{
			    		try{    
				    		optionDlg = new Stage(StageStyle.UTILITY);
				    		optionDlg.initModality(Modality.WINDOW_MODAL);
				    		optionDlg.initOwner(primaryStage);
				    		optionDlg.setTitle("일반 옵션");
				    		//optionDlg.setAlwaysOnTop(true);
				    		optionDlg.toFront();
				    		optionDlg.setResizable(false);
				    		FXMLLoader loader2 = new FXMLLoader(getClass().getResource("fxml/normalOption.fxml"));
							Parent root2 = (Parent)loader2.load();
							Text pathTxt = (Text)root2.lookup("#pathTxt");
							TextField path = (TextField)root2.lookup("#path");
							ToggleSwitch prohibitCopy = (ToggleSwitch)root2.lookup("#prohibitCopy");
							ToggleSwitch pathToggle = (ToggleSwitch)root2.lookup("#pathToggle");
							ToggleSwitch reserveClose = (ToggleSwitch)root2.lookup("#reserveClose");
							TextField time = (TextField)root2.lookup("#time");
							Text timeTxt = (Text)root2.lookup("#timeTxt");
							Button timeBtn = (Button)root2.lookup("#timeBtn");
							Text explainTxt = (Text)root2.lookup("#explainTxt");
							
							if(normalOption1Flag && !normalOption2Flag && !normalOption3Flag){
								prohibitCopy.switchOnProperty().set(true);
							}else if(normalOption2Flag && !normalOption1Flag && !normalOption3Flag){
								pathToggle.switchOnProperty().set(true);
								path.setVisible(true); pathTxt.setVisible(true);
							}else if(normalOption3Flag && !normalOption1Flag && !normalOption2Flag){
								reserveClose.switchOnProperty().set(true);
								time.setVisible(true); timeTxt.setVisible(true); timeBtn.setVisible(true);
							}else if(normalOption1Flag && normalOption2Flag && !normalOption3Flag){
								prohibitCopy.switchOnProperty().set(true); pathToggle.switchOnProperty().set(true);
								path.setVisible(true); pathTxt.setVisible(true);
							}else if(normalOption1Flag && normalOption3Flag && !normalOption2Flag){
								prohibitCopy.switchOnProperty().set(true); reserveClose.switchOnProperty().set(true);
								time.setVisible(true); timeTxt.setVisible(true); timeBtn.setVisible(true);
							}else if(normalOption2Flag && normalOption3Flag && !normalOption1Flag){
								pathToggle.switchOnProperty().set(true); reserveClose.switchOnProperty().set(true);
								path.setVisible(true); pathTxt.setVisible(true);
								time.setVisible(true); timeTxt.setVisible(true); timeBtn.setVisible(true);
							}else if(normalOption1Flag && normalOption2Flag && normalOption3Flag){
								prohibitCopy.switchOnProperty().set(true); pathToggle.switchOnProperty().set(true); reserveClose.switchOnProperty().set(true);
								path.setVisible(true); pathTxt.setVisible(true);
								time.setVisible(true); timeTxt.setVisible(true); timeBtn.setVisible(true);
							}
							
							prohibitCopy.setOnMouseClicked((ev2)->{
								if(prohibitCopy.switchOnProperty().get() == false){
									normalOption1Flag = false;
									if(copyThread.isAlive()){
										copyThread.stop();
									}
									explainTxt.setVisible(true); explainTxt.setText("복사 금지 설정 해제");
								}
								else{normalOption1Flag = true;}
								copyThread.start();
								explainTxt.setVisible(true); explainTxt.setText("복사 금지 설정 완료");
							});
							pathToggle.setOnMouseClicked((ev2)->{
								if(pathToggle.switchOnProperty().get() == false){
									normalOption2Flag = false;
									path.setVisible(false); pathTxt.setVisible(false);
								}
								else{
									normalOption2Flag = true;
									path.setVisible(true); pathTxt.setVisible(true);
									DirectoryChooser directoryChooser = new DirectoryChooser();
							    	directoryChooser.setTitle("디렉터리 경로 찾기");
							    	File selectedFile = directoryChooser.showDialog(primaryStage);
							    	String selectedFilePath = selectedFile.getPath();
							    	path.setText(selectedFilePath);
									try {
										OutputStream foss = new FileOutputStream(path.getText() + "/" + "result.txt");
										foss.write(textArea.getText().getBytes(Charset.forName("UTF-8")));
										explainTxt.setVisible(true); explainTxt.setText("결과 텍스트 저장 완료");
									} catch (Exception e1) {}
								}								
							});
							reserveClose.setOnMouseClicked((ev2)->{
								if(reserveClose.switchOnProperty().get() == false){
									normalOption3Flag = false;
									time.setVisible(false); timeTxt.setVisible(false); timeBtn.setVisible(false);
									if(timeThread.isAlive()){
										timeOff = true;
									}
									explainTxt.setVisible(true); explainTxt.setText("예약종료 설정 해제");
								}
								else{
									normalOption3Flag = true;
									time.setVisible(true); timeTxt.setVisible(true); timeBtn.setVisible(true);
								}
								timeBtn.setOnAction(ev4 ->{
									if(time.getText().isEmpty())return;
									reserveTime = Integer.valueOf(time.getText().toString());
									timeOff = false;
									timeThread.start();
									explainTxt.setVisible(true); explainTxt.setText("예약종료 설정 완료");
								});
							});
							
							Scene scene = new Scene(root2);
						    optionDlg.setScene(scene);
						    optionDlg.show();
						    passCheck.close();
					    }catch(Exception e){}
			    	}
				});
				Scene scene = new Scene(root);
				passCheck.setScene(scene);
				passCheck.show();
			}catch(Exception e){}
		});
		
		option.setOnAction(ev->{
			if(closeState)return;
			Stage passCheck = new Stage(StageStyle.UTILITY);
			passCheck.initModality(Modality.WINDOW_MODAL);
			passCheck.initOwner(primaryStage);
			passCheck.setTitle("비밀번호 확인");
			//passCheck.setAlwaysOnTop(true);
			passCheck.toFront();
			passCheck.setResizable(false);
		    FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/password.fxml"));
		    Parent root;
			try{
				root = (Parent)loader.load();
				Button pwdOK = (Button)root.lookup("#btnOK");
				TextField pwd = (TextField)root.lookup("#password");
				Text text = (Text)root.lookup("#text");
				
				pwdOK.setOnAction(ev3 -> {
					if(pwd.getText().equals(""))text.setFill(Paint.valueOf("RED"));
			        else if(!pwd.getText().equals(password))text.setFill(Paint.valueOf("RED"));
			    	else{
			    		optionDlg = new Stage(StageStyle.UTILITY);
			    		optionDlg.initModality(Modality.WINDOW_MODAL);
			    		optionDlg.initOwner(primaryStage);
			    		optionDlg.setTitle("프로세스 종료 시도 방지 옵션");
			    		//optionDlg.setAlwaysOnTop(true);
			    		optionDlg.toFront();
			    		optionDlg.setResizable(false);
			    		FXMLLoader loader2 = new FXMLLoader(getClass().getResource("fxml/option.fxml"));
				    try{
					    Parent root2 = (Parent)loader2.load();
					    ToggleButton OnOffBtn = (ToggleButton)root2.lookup("#OnOffBtn");
					    CheckBox show1 =(CheckBox)root2.lookup("#show1");
					    CheckBox show2 = (CheckBox)root2.lookup("#show2");
					    Text show3 =(Text)root2.lookup("#show3");
					    CheckBox show4 =(CheckBox)root2.lookup("#show4");
					    Button btn = (Button)root2.lookup("#btn");
					    TextField warningTxt = (TextField)root2.lookup("#warningTxt");
					    Text warningTxtCheck =(Text)root2.lookup("#show3");
					    
					    if(optionOnOff == true){
					    	OnOffBtn.setText("끄기");
				    		show1.setVisible(true);
				    		show2.setVisible(true);
				    		show3.setVisible(true);
				    		show4.setVisible(true);
				    		btn.setVisible(true);
				    		
					    	if(TaskManagerCheck1)show1.selectedProperty().set(true);
					    	if(TaskManagerCheck2)show2.selectedProperty().set(true);
					    	if(TaskManagerCheck3)show4.selectedProperty().set(true);
					    }
					    
					    OnOffBtn.setOnAction(ev2 -> {
					    	if(OnOffBtn.getText().equals("켜기")){
					    		optionOnOff = true;
					    		if(optionOnOff == true){
					    			OnOffBtn.setText("끄기");
						    		show1.setVisible(true);
						    		show2.setVisible(true);
						    		show3.setVisible(true);
						    		show4.setVisible(true);
						    		btn.setVisible(true);
					    		}
					    	}else{
					    		optionOnOff = false;
					    		if(TaskCheckThread.isAlive())
					    			optionStop = true;
					    		if(optionOnOff == false){
						    		OnOffBtn.setText("켜기");
						    		show1.setVisible(false);
						    		show2.setVisible(false);
						    		show3.setVisible(false);
						    		show4.setVisible(false);
						    		btn.setVisible(false);
						    		TaskManagerCheck1 = false;
						    		TaskManagerCheck2 = false;
						    		TaskManagerCheck3 = false;
						    		show1.setSelected(false);
							    	show2.setSelected(false);
							    	show4.setSelected(false);
						    	}
					    	}
					    });
					    
					    show1.setOnAction(ev2 -> {
					    	TaskManagerCheck1=show1.selectedProperty().get();
					    });
					    show2.setOnAction(ev2 -> {
					    	TaskManagerCheck2 = show2.selectedProperty().get();
					    });
					    show4.setOnAction(ev2 -> {
					    	TaskManagerCheck3 = show4.selectedProperty().get();
					    	if(TaskManagerCheck3){
					    		warningTxt.setVisible(true);
					    		return;
					    	}
					    	warningTxt.setVisible(false);
					    });
					    
					    btn.setOnAction(ev2 ->{
					    	if(!show1.selectedProperty().get() && !show2.selectedProperty().get() && !show4.selectedProperty().get()){
					    		optionDlg.close();
					    		optionStop = true;
					    		return;
					    	}
					    	if(show4.selectedProperty().get() && warningTxt.getText().equals("")){
					    		warningTxtCheck.setVisible(true);
					    		warningTxtCheck.setText("출력할 경고문을 입력하세요");
					    		warningTxtCheck.setFill(Paint.valueOf("RED"));
					    		optionStop = true;
					    		return;
					    	}else if(show4.selectedProperty().get() && !warningTxt.getText().equals("")){
					    		 warning = warningTxt.getText();
					    	}
					    	optionStop = false;
					    	optionDlg.close();
					    	optionCnt++;
					    	warningTxtCheck.setVisible(false);
					    	if(optionCnt == 1)TaskCheckThread.start();
					    });
					    
					    Scene scene = new Scene(root2);
					    optionDlg.setScene(scene);
					    optionDlg.show();
					    passCheck.close();
				    }catch(Exception e){}
			    	}
				});
				Scene scene = new Scene(root);
				passCheck.setScene(scene);
				passCheck.show();
			}catch(Exception e){}
		});
		
		mailOption.setOnAction(ev->{
			if(closeState)return;
			Stage passCheck = new Stage(StageStyle.UTILITY);
			passCheck.initModality(Modality.WINDOW_MODAL);
			passCheck.initOwner(primaryStage);
			passCheck.setTitle("비밀번호 확인");
			//passCheck.setAlwaysOnTop(true);
			passCheck.toFront();
			passCheck.setResizable(false);
		    FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/password.fxml"));
		    Parent root;
			try{
				root = (Parent)loader.load();
				Button pwdOK = (Button)root.lookup("#btnOK");
				TextField pwd = (TextField)root.lookup("#password");
				Text text = (Text)root.lookup("#text");
				
				pwdOK.setOnAction(ev3 -> {
					if(pwd.getText().equals(""))text.setFill(Paint.valueOf("RED"));
			        else if(!pwd.getText().equals(password))text.setFill(Paint.valueOf("RED"));
			    	else{
			    		optionDlg = new Stage(StageStyle.UTILITY);
			    		optionDlg.initModality(Modality.WINDOW_MODAL);
			    		optionDlg.initOwner(primaryStage);
			    		optionDlg.setTitle("메일/클라우드 전송 방지 옵션");
			    		//optionDlg.setAlwaysOnTop(true);
			    		optionDlg.toFront();
			    		optionDlg.setResizable(false);
			    		FXMLLoader loader2 = new FXMLLoader(getClass().getResource("fxml/mailOption.fxml"));
					    try{
						    Parent root2 = (Parent)loader2.load();
						    ToggleButton mailOnOffBtn = (ToggleButton)root2.lookup("#mailOnOffBtn");
						    CheckBox mailShow1 =(CheckBox)root2.lookup("#mailShow1");
						    CheckBox mailShow2 = (CheckBox)root2.lookup("#mailShow2");
						    Button mailBtn = (Button)root2.lookup("#mailBtn");
						    TextField mailWarningTxt = (TextField)root2.lookup("#mailWarningTxt");
						    Text mailWarningTxtCheck =(Text)root2.lookup("#mailWarningTxtCheck");
						    
						    if(mailOptionOnOff == true){
						    	mailOnOffBtn.setText("끄기");
					    		mailShow1.setVisible(true);
					    		mailShow2.setVisible(true);
					    		mailBtn.setVisible(true);
					    		
						    	if(mailManagerCheck1)mailShow1.selectedProperty().set(true);
						    	if(mailManagerCheck2)mailShow2.selectedProperty().set(true);
						    }
						    
						    mailOnOffBtn.setOnAction(ev2 -> {
						    	if(mailOnOffBtn.getText().equals("켜기")){
						    		mailOptionOnOff = true;
						    		if(mailOptionOnOff == true){
						    			mailOnOffBtn.setText("끄기");
						    			mailShow1.setVisible(true);
						    			mailShow2.setVisible(true);
						    			mailBtn.setVisible(true);
						    		}
						    	}else{
						    		mailOptionOnOff = false;
						    		if(mailThread.isAlive())
						    			mailOptionStop = true;
						    		if(mailOptionOnOff == false){
						    			mailOnOffBtn.setText("켜기");
						    			mailShow1.setVisible(false);
						    			mailShow2.setVisible(false);
						    			mailBtn.setVisible(false);
						    			mailManagerCheck1 = false;
						    			mailManagerCheck2 = false;
						    			mailShow1.setSelected(false);
						    			mailShow2.setSelected(false);
							    	}
						    	}
						    });
						    
						    mailShow1.setOnAction(ev2 -> {
						    	mailManagerCheck1 = mailShow1.selectedProperty().get();
						    });
						    mailShow2.setOnAction(ev2 -> {
						    	mailManagerCheck2 = mailShow2.selectedProperty().get();
						    	if(mailManagerCheck2){
						    		mailWarningTxt.setVisible(true);
						    		return;
						    	}
						    	mailWarningTxt.setVisible(false);
						    });
						    
						    mailBtn.setOnAction(ev2 ->{
						    	if(!mailShow1.selectedProperty().get() && !mailShow2.selectedProperty().get()){
						    		optionDlg.close();
						    		mailOptionOnOff = true;
						    		return;
						    	}
						    	if(mailShow2.selectedProperty().get() && mailWarningTxt.getText().equals("")){
						    		mailWarningTxtCheck.setVisible(true);
						    		mailWarningTxtCheck.setText("출력할 경고문을 입력하세요");
						    		mailWarningTxtCheck.setFill(Paint.valueOf("RED"));
						    		mailOptionStop = true;
						    		return;
						    	}else if(mailShow2.selectedProperty().get() && !mailWarningTxt.getText().equals("")){
						    		 mailWarning = mailWarningTxt.getText().toString();
						    	}
						    	mailOptionStop = false;
						    	optionDlg.close();
						    	mailOptionCnt++;
						    	mailWarningTxtCheck.setVisible(false);
						    	if(mailOptionCnt == 1)mailThread.start();
						    });
						    Scene scene = new Scene(root2);
						    optionDlg.setScene(scene);
						    optionDlg.show();
						    passCheck.close();
					    }catch(Exception e){}
			    	}
				});
				Scene scene = new Scene(root);
				passCheck.setScene(scene);
				passCheck.show();
			}catch(Exception e){}
			
		});
		
		option.setOnAction(ev->{
			if(closeState)return;
			Stage passCheck = new Stage(StageStyle.UTILITY);
			passCheck.initModality(Modality.WINDOW_MODAL);
			passCheck.initOwner(primaryStage);
			passCheck.setTitle("비밀번호 확인");
			//passCheck.setAlwaysOnTop(true);
			passCheck.toFront();
			passCheck.setResizable(false);
		    FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/password.fxml"));
		    Parent root;
			try{
				root = (Parent)loader.load();
				Button pwdOK = (Button)root.lookup("#btnOK");
				TextField pwd = (TextField)root.lookup("#password");
				Text text = (Text)root.lookup("#text");
				
				pwdOK.setOnAction(ev3 -> {
					if(pwd.getText().equals(""))text.setFill(Paint.valueOf("RED"));
			        else if(!pwd.getText().equals(password))text.setFill(Paint.valueOf("RED"));
			    	else{
			    		optionDlg = new Stage(StageStyle.UTILITY);
			    		optionDlg.initModality(Modality.WINDOW_MODAL);
			    		optionDlg.initOwner(primaryStage);
			    		optionDlg.setTitle("프로세스 종료 시도 방지 옵션");
			    		//optionDlg.setAlwaysOnTop(true);
			    		optionDlg.toFront();
			    		optionDlg.setResizable(false);
			    		FXMLLoader loader2 = new FXMLLoader(getClass().getResource("fxml/option.fxml"));
				    try{
					    Parent root2 = (Parent)loader2.load();
					    ToggleButton OnOffBtn = (ToggleButton)root2.lookup("#OnOffBtn");
					    CheckBox show1 =(CheckBox)root2.lookup("#show1");
					    CheckBox show2 = (CheckBox)root2.lookup("#show2");
					    Text show3 =(Text)root2.lookup("#show3");
					    CheckBox show4 =(CheckBox)root2.lookup("#show4");
					    Button btn = (Button)root2.lookup("#btn");
					    TextField warningTxt = (TextField)root2.lookup("#warningTxt");
					    Text warningTxtCheck =(Text)root2.lookup("#show3");
					    
					    if(optionOnOff == true){
					    	OnOffBtn.setText("끄기");
				    		show1.setVisible(true);
				    		show2.setVisible(true);
				    		show3.setVisible(true);
				    		show4.setVisible(true);
				    		btn.setVisible(true);
				    		
					    	if(TaskManagerCheck1)show1.selectedProperty().set(true);
					    	if(TaskManagerCheck2)show2.selectedProperty().set(true);
					    	if(TaskManagerCheck3)show4.selectedProperty().set(true);
					    }
					    
					    OnOffBtn.setOnAction(ev2 -> {
					    	if(OnOffBtn.getText().equals("켜기")){
					    		optionOnOff = true;
					    		if(optionOnOff == true){
					    			OnOffBtn.setText("끄기");
						    		show1.setVisible(true);
						    		show2.setVisible(true);
						    		show3.setVisible(true);
						    		show4.setVisible(true);
						    		btn.setVisible(true);
					    		}
					    	}else{
					    		optionOnOff = false;
					    		if(TaskCheckThread.isAlive())
					    			optionStop = true;
					    		if(optionOnOff == false){
						    		OnOffBtn.setText("켜기");
						    		show1.setVisible(false);
						    		show2.setVisible(false);
						    		show3.setVisible(false);
						    		show4.setVisible(false);
						    		btn.setVisible(false);
						    		TaskManagerCheck1 = false;
						    		TaskManagerCheck2 = false;
						    		TaskManagerCheck3 = false;
						    		show1.setSelected(false);
							    	show2.setSelected(false);
							    	show4.setSelected(false);
						    	}
					    	}
					    });
					    
					    show1.setOnAction(ev2 -> {
					    	TaskManagerCheck1=show1.selectedProperty().get();
					    });
					    show2.setOnAction(ev2 -> {
					    	TaskManagerCheck2 = show2.selectedProperty().get();
					    });
					    show4.setOnAction(ev2 -> {
					    	TaskManagerCheck3 = show4.selectedProperty().get();
					    	if(TaskManagerCheck3){
					    		warningTxt.setVisible(true);
					    		return;
					    	}
					    	warningTxt.setVisible(false);
					    });
					    
					    btn.setOnAction(ev2 ->{
					    	if(!show1.selectedProperty().get() && !show2.selectedProperty().get() && !show4.selectedProperty().get()){
					    		optionDlg.close();
					    		optionStop = true;
					    		return;
					    	}
					    	if(show4.selectedProperty().get() && warningTxt.getText().equals("")){
					    		warningTxtCheck.setVisible(true);
					    		warningTxtCheck.setText("출력할 경고문을 입력하세요");
					    		warningTxtCheck.setFill(Paint.valueOf("RED"));
					    		optionStop = true;
					    		return;
					    	}else if(show4.selectedProperty().get() && !warningTxt.getText().equals("")){
					    		 warning = warningTxt.getText();
					    	}
					    	optionStop = false;
					    	optionDlg.close();
					    	optionCnt++;
					    	warningTxtCheck.setVisible(false);
					    	if(optionCnt == 1)TaskCheckThread.start();
					    });
					    
					    Scene scene = new Scene(root2);
					    optionDlg.setScene(scene);
					    optionDlg.show();
					    passCheck.close();
				    }catch(Exception e){}
			    	}
				});
				Scene scene = new Scene(root);
				passCheck.setScene(scene);
				passCheck.show();
			}catch(Exception e){}
		});
	}
	
	/*
	 * main과 연결을 위한 메서드
	 */
	public void setPrimaryStage(Stage primaryStage){
		this.primaryStage = primaryStage;
	}
	public void setDialog(Stage dialog){
		this.dialog = dialog; 
	}
	//

	public static List<String> listRunningProcesses(String processName) {
		  List<String> processes = new ArrayList<String>();
		  try {
			  String line;
			  StringTokenizer temp;
			  Process p = Runtime.getRuntime().exec("tasklist.exe /FI \"IMAGENAME eq \""+processName+" /FO CSV /NH");

			  BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			  while ((line = input.readLine()) != null) {
				  if (!line.trim().equals("")) {
					  line = line.replace("\",\"", "^").replace("\"", "").replace(",", "");
				      temp = new StringTokenizer(line, "^");
				      while (temp.hasMoreTokens()) {
				    	  processes.add(temp.nextToken());
				      }
			      }
			  }
		  } catch (Exception e) {}
		  return processes;
	}
	
	/*
	 * 브라우저 감시
	 */
	Thread mailThread = new Thread(){
    	public void run(){
    		int cnt = 0;
    		while(true){
				while(mailOptionStop){
					cnt = 0;
				}
				cnt++;
				if(mailManagerCheck1 && mailManagerCheck2){
					if(cnt==1)textArea.appendText("메일/클라우드 서비스 이용 추정시 마우스를 비정상 동작, 메모장으로 통보합니다\n\n");
					try{	
			    		while(true){
							Thread.sleep(500);
				    		HWND fgWindow = User32.INSTANCE.GetForegroundWindow();
					        int titleLength = User32.INSTANCE.GetWindowTextLength(fgWindow) + 1;
					        char[] title = new char[titleLength];
					        User32.INSTANCE.GetWindowText(fgWindow, title, titleLength);
					        if(Native.toString(title).contains("메일") || Native.toString(title).contains("mail") 
					        		|| Native.toString(title).contains("클라우드") || Native.toString(title).contains("cloud") 
					        		|| Native.toString(title).contains("드라이브") || Native.toString(title).contains("drive"))
					        {
					        	unusualAction(usb + ":\\");
					        	textArea.appendText(mailWarning + "\n\n");
					        	mailOptionCnt = 0;
					        	try {this.finalize();} catch (Throwable e) {}
					        	return;
					        }
			    		}
			    	}catch(Exception e){}
				}else if(mailManagerCheck1){
					if(cnt==1)textArea.appendText("메일/클라우드 서비스 이용 추정시 마우스를 비정상 동작시킵니다\n\n");
					try{	
			    		while(true){
							Thread.sleep(500);
				    		HWND fgWindow = User32.INSTANCE.GetForegroundWindow();
					        int titleLength = User32.INSTANCE.GetWindowTextLength(fgWindow) + 1;
					        char[] title = new char[titleLength];
					        User32.INSTANCE.GetWindowText(fgWindow, title, titleLength);
					        if(Native.toString(title).contains("메일") || Native.toString(title).contains("mail") 
					        		|| Native.toString(title).contains("클라우드") || Native.toString(title).contains("cloud") 
					        		|| Native.toString(title).contains("드라이브") || Native.toString(title).contains("drive"))
					        {
					        	unusualAction(usb + ":\\");
					        	mailOptionCnt = 0;
					        	try {this.finalize();} catch (Throwable e) {}
					        	return;
					        }
			    		}
			    	}catch(Exception e){}
				}else if(mailManagerCheck2){
					if(cnt==1)textArea.appendText("메일/클라우드 서비스 이용 추정시 메모장으로 통보합니다\n\n");
					try{	
			    		while(true){
							Thread.sleep(500);
				    		HWND fgWindow = User32.INSTANCE.GetForegroundWindow();
					        int titleLength = User32.INSTANCE.GetWindowTextLength(fgWindow) + 1;
					        char[] title = new char[titleLength];
					        User32.INSTANCE.GetWindowText(fgWindow, title, titleLength);
					        if(Native.toString(title).contains("메일") || Native.toString(title).contains("mail") 
					        		|| Native.toString(title).contains("클라우드") || Native.toString(title).contains("cloud") 
					        		|| Native.toString(title).contains("드라이브") || Native.toString(title).contains("drive"))
					        {
					        	textArea.appendText(mailWarning + "\n\n");
					        	mailOptionCnt = 0;
					        	try {this.finalize();} catch (Throwable e) {}
					        	return;
					        }
			    		}
			    	}catch(Exception e){}
				}
			   }
    	}
    };
    //
	
	Thread TaskCheckThread = new Thread(){
		@SuppressWarnings("deprecation")
		public void run() {
			int cnt = 0;
			while(true){
				while(optionStop){
					cnt = 0;
				}
				List<String> processes = listRunningProcesses("Taskmgr.exe");
				String result = "";
			
				Iterator<String> it = processes.iterator();
	
				int i = 0;
				while (it.hasNext()) {
					result += it.next() + "\n";
					
					i++;
					if (i % 5 == 0)
					result += "\n";
				}
				cnt++;
				
				if(TaskManagerCheck1 && TaskManagerCheck2 && TaskManagerCheck3){
					if(cnt==1)textArea.appendText("프로세스 강제 종료 추정시 USB파일을 삭제, 마우스를 비정상 동작, 메모장으로 통보합니다\n\n");
					if(result.contains("Taskmgr.exe")){
						textArea.appendText(warning + "\n\n");
						
						unusualAction(usb + ":\\");
						
						executorService.shutdownNow();
						if(wst.isAlive())wst.stop();
						deleteFolder(usb + ":\\");
					}
				}else if(TaskManagerCheck1 && TaskManagerCheck3){
					if(cnt==1)textArea.appendText("프로세스 강제 종료 추정시 USB파일을 삭제, 메모장으로 통보합니다\n\n");
					if(result.contains("Taskmgr.exe")){
						textArea.appendText(warning + "\n\n");
						
						executorService.shutdownNow();
						if(wst.isAlive())wst.stop();
						deleteFolder(usb + ":\\");
					}
				}else if(TaskManagerCheck2 && TaskManagerCheck3){
					if(cnt==1)textArea.appendText("프로세스 강제 종료 추정시 마우스를 비정상 동작, 메모장으로 통보합니다\n\n");
					if(result.contains("Taskmgr.exe")){
						textArea.appendText(warning + "\n\n");
						unusualAction(usb + ":\\");
					}
				}else if(TaskManagerCheck1){
					if(cnt==1)textArea.appendText("프로세스 강제 종료 추정시 USB파일을 삭제합니다\n\n");
					if(result.contains("Taskmgr.exe")){
						executorService.shutdownNow();
						if(wst.isAlive())wst.stop();
						deleteFolder(usb + ":\\");
					}
				}else if(TaskManagerCheck2){
					if(cnt==1)textArea.appendText("프로세스 강제 종료 추정시 마우스를 비정상 동작시킵니다\n\n");
					if(result.contains("Taskmgr.exe")){
						executorService.shutdownNow();
						if(wst.isAlive())wst.stop();
						unusualAction(usb + ":\\");
					}
				}else if(TaskManagerCheck3){
					if(cnt==1)textArea.appendText("프로세스 강제 종료 추정시 메모장으로 통보합니다\n\n");
					if(result.contains("Taskmgr.exe")){
						textArea.appendText(warning + "\n\n");
					}
				}
			   }
			}
	};
	
	public void unusualAction(String parentPath){
		try {
			Robot robot = new Robot();
			unusualActionExitFlag = true;
			if(closeState)return;
			Platform.runLater(()->{
				Stage passCheck = new Stage(StageStyle.UTILITY);
				passCheck.initModality(Modality.WINDOW_MODAL);
				passCheck.initOwner(primaryStage);
				passCheck.setTitle("비밀번호 확인");
				//passCheck.setAlwaysOnTop(true);
				passCheck.toFront();
				passCheck.setResizable(false);
			    FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/password.fxml"));
			    Parent root;
				try{
					root = (Parent)loader.load();
					Button pwdOK = (Button)root.lookup("#btnOK");
					TextField pwd = (TextField)root.lookup("#password");
					Text text = (Text)root.lookup("#text");
					
					pwdOK.setOnAction(ev3 -> {
						if(pwd.getText().equals(""))text.setFill(Paint.valueOf("RED"));
				    	else if(!pwd.getText().equals(password))text.setFill(Paint.valueOf("RED"));
				    	else{
				    		robot.mouseRelease(InputEvent.BUTTON1_MASK);
				    		unusualActionExitFlag = false;
				    		passCheck.close();
				    	}
					});
					Scene scene = new Scene(root);
					passCheck.setScene(scene);
					passCheck.show();
				}catch(Exception e){}
			});
			
			while(unusualActionExitFlag){
			    	robot.mouseMove(0, 0);
			    	robot.delay(100);
			}
			optionStop = true;
			mailOptionStop = true;
		} catch (Exception e) {}
	}

	public void deleteFolder(String parentPath) {
	    File file = new File(parentPath);
	    String[] fnameList = file.list();
	    int fCnt = fnameList.length;
	    String childPath = "";
	    
	    for(int i = 0; i < fCnt; i++) {
	    	childPath = parentPath+"/"+fnameList[i];
	    	File f = new File(childPath);
	    	if( ! f.isDirectory()) {
	    		f.delete();
	    	}
	        else {
	        	deleteFolder(childPath);
	        }
	    }
	}
	
	Thread copyThread = new Thread(){
		public void run(){
			while(true){
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {}
				Platform.runLater(()->{
					clipboard.clear();
				});
			}	
		}
	};
	
	Thread timeThread = new Thread(){
		Timer timer = new java.util.Timer();
		public void run(){
			timer.schedule(new TimerTask() {
			    public void run() {
			    	if(timeOff){
			    		try {this.finalize();} catch (Throwable e) {}
			    		return;
			    	}
			         Platform.runLater(new Runnable() {
			            public void run() {
			            	Runtime.getRuntime().exit(0);
			            }
			        });
			    }
			}, reserveTime * 1000 * 60);
		}
	};
	
	
	/*
	 * USB인식확인을 위한 VBScript
	 */
	public static String getDriveType(String drive) {
        String result = "";
        try {
            File file = File.createTempFile("test", ".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);
 
            String vbs =
                    "Set objWMIService = GetObject(\"winmgmts:\")\n"
                    + "Set objDisk = objWMIService.Get(\"Win32_LogicalDisk.DeviceID=\'"
                    + drive
                    + ":\'\")\n" + "Wscript.Echo objDisk.DriveType";
 
            fw.write(vbs);
            fw.close();

            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = input.readLine()) != null) {
                result += line;
            }
            input.close();
        } catch (Exception e) {}
        return result;
	}
	//
	
	
	/*
	 * WatchService 메서드
	 */
	class WatchServiceThread extends Thread {
		@Override
		public void run() {
			try {
				boolean check = true;
				
				while(check){
					for(int i=0; i<23; i++){
						if(getDriveType(eng.get(i)).equals("2")){
							check = false;
							usb = eng.get(i).toUpperCase();
							break;
						}
						closeState = true;
					}
				}
				
				usbConnectCheckThread = new Thread(){
					@SuppressWarnings("deprecation")
					@Override
					public void run() {
						while(getDriveType(usb).equals("2")){
							usbIconCheck=1;
							if(usbIconCheck==1){
								usbIcon.setImage(usb_green);
								usbIconCheck+=1;
							}
						}
						Platform.runLater(()->{
							textArea.setText("연결이 종료되었습니다");
							closeState = true;
							dirList.getItems().clear();
							fList.getItems().clear();
							if(passInput.isShowing())passInput.close();
							executeState.setText("연결끊김");
						});
						usbIcon.setImage(usb_red);
						executorService.shutdown();
					}
				};
				usbConnectCheckThread.start();
				
				Platform.runLater(()->{
					passInput = new Stage(StageStyle.UTILITY);
					passInput.initModality(Modality.WINDOW_MODAL);
					passInput.initOwner(primaryStage);
					passInput.setTitle("비밀번호 설정");
					//passInput.setAlwaysOnTop(true);
					passInput.toFront();
					passInput.setResizable(false);
				    FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/password.fxml"));
				    Parent root;
					try {
						root = (Parent)loader.load();
					    Button pwdOK = (Button)root.lookup("#btnOK");
					    TextField pwd = (TextField)root.lookup("#password");
					    Text text = (Text)root.lookup("#text");
					    
					    pwdOK.setOnAction(ev -> {
					    	password = pwd.getText();
					    	if(password.equals("")){
					    		text.setFill(Paint.valueOf("RED"));
					    		closeState = true;
					    		return;
					    	}
					    	closeState = false;
					    	passInput.close();
							textArea.setText("USB 감시를 시작합니다\n" + sdate.format(date).toString() +"\n\n");
							
							Runnable runnable = new Runnable(){
								@Override
								public void run() {
									try{
										Platform.runLater(()->dirList.getItems().add(usb + ":\\"));
										Path directory = Paths.get(usb + ":\\");
										
										dirMap.put(dirListCnt++, String.valueOf(directory.toRealPath(LinkOption.NOFOLLOW_LINKS)));
										
										WatchService watchService = FileSystems.getDefault().newWatchService();
										directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
						                         StandardWatchEventKinds.ENTRY_DELETE,
						                         StandardWatchEventKinds.ENTRY_MODIFY);
										while(true) {
											WatchKey watchKey = watchService.take();
											List<WatchEvent<?>> list = watchKey.pollEvents();
											for(WatchEvent watchEvent : list) {
												Kind kind = watchEvent.kind();
												Path path = (Path)watchEvent.context();
												if(kind == StandardWatchEventKinds.ENTRY_CREATE) {
													Platform.runLater(()->{
														try{
														if(path.toString().contains(".txt")){
															textArea.appendText(usb + ":\\" + " 폴더에 파일 생성됨 -> " + path.getFileName() + " \n"+sdate.format(date).toString() + "\n\n");
															fList.getItems().add(String.valueOf(path.getFileName()));
														}else if(path.toString().contains(".jpg") || path.toString().contains(".png") || path.toString().contains(".gif") || path.toString().contains(".bmp")){
															textArea.appendText(usb + ":\\" + " 폴더에 이미지파일 생성됨 -> " + path.getFileName() + " \n"+sdate.format(date).toString() + "\n\n");
															fList.getItems().add(String.valueOf(path.getFileName()));
															fMap.put(fListCnt++, usb + ":\\" + path.toString());
														}else if(path.toString().contains(".avi") || path.toString().contains(".wmv") || path.toString().contains(".mp4") || path.toString().contains(".mpeg")){
															textArea.appendText(usb + ":\\" + " 폴더에 동영상파일 생성됨 -> " + path.getFileName() + " \n"+sdate.format(date).toString() + "\n\n");
															fList.getItems().add(String.valueOf(path.getFileName()));	
															fMap.put(fListCnt++, usb + ":\\" + path.toString());
														}else if(path.toString().contains(".zip")){
															textArea.appendText(usb + ":\\" + " 폴더에 압축파일 생성됨 -> " + path.getFileName() + " \n"+sdate.format(date).toString() + "\n\n");
															fList.getItems().add(String.valueOf(path.getFileName()));	
															fMap.put(fListCnt++, usb + ":\\" + path.toString());
														}else{
															textArea.appendText(usb + ":\\" + " 폴더에 디렉터리 생성됨 -> " + path.getFileName() + " \n" + sdate.format(date).toString() + "\n\n");
															dirList.getItems().add(String.valueOf(path.getFileName()));
															dirMap.put(dirListCnt++, usb + ":\\" + path.toString());
														}
														}catch(Exception e){}
													});
										
												} else if(kind == StandardWatchEventKinds.ENTRY_DELETE) {
													Platform.runLater(()->{
														if(path.toString().contains(".txt")){
															textArea.appendText(usb + ":\\" +" 폴더에 파일 삭제됨 -> " + path.getFileName() + " \n" +sdate.format(date).toString() + "\n\n");
															fList.getItems().remove(String.valueOf(path.getFileName()));
														}else if(path.toString().contains(".jpg") || path.toString().contains(".png") || path.toString().contains(".gif") || path.toString().contains(".bmp")){
															textArea.appendText(usb + ":\\" + " 폴더에 이미지파일 삭제됨 -> " + path.getFileName() + " \n"+sdate.format(date).toString() + "\n\n");
															fList.getItems().remove(String.valueOf(path.getFileName()));
														}else if(path.toString().contains(".avi") || path.toString().contains(".wmv") || path.toString().contains(".mp4") || path.toString().contains(".mpeg")){
															textArea.appendText(usb + ":\\" + " 폴더에 동영상파일 삭제됨 -> " + path.getFileName() + " \n"+sdate.format(date).toString() + "\n\n");
															fList.getItems().remove(String.valueOf(path.getFileName()));	
														}else if(path.toString().contains(".zip")){
															textArea.appendText(usb + ":\\" + " 폴더에 압축파일 삭제됨 -> " + path.getFileName() + " \n"+sdate.format(date).toString() + "\n\n");
															fList.getItems().remove(String.valueOf(path.getFileName()));	
														}else{
															textArea.appendText(usb + ":\\" + " 폴더에 디렉터리 삭제됨 -> " + path.getFileName()+ " \n" + sdate.format(date).toString() + "\n\n");
															dirList.getItems().remove(path.toString());
														}
													});
												} else if(kind == StandardWatchEventKinds.ENTRY_MODIFY) {
													Platform.runLater(()->{
														if(path.toString().contains(".txt")){
															textArea.appendText(usb + ":\\" + " 폴더에 파일 변경됨 -> "  + path.getFileName() + "\n" + sdate.format(date).toString() + "\n\n");
														}else if(path.toString().contains(".jpg") || path.toString().contains(".png") || path.toString().contains(".gif") || path.toString().contains(".bmp")){
															textArea.appendText(usb + ":\\" + " 폴더에 이미지파일 변경됨 -> " + path.getFileName() + "\n"+sdate.format(date).toString() + "\n\n");
														}else if(path.toString().contains(".avi") || path.toString().contains(".wmv") || path.toString().contains(".mp4") || path.toString().contains(".mpeg")){
															textArea.appendText(usb + ":\\" + " 폴더에 동영상파일 변경됨 -> " + path.getFileName() + "\n"+sdate.format(date).toString() + "\n\n");
														}else if(path.toString().contains(".zip")){
															textArea.appendText(usb + ":\\" + " 폴더에 압축파일 변경됨 -> " + path.getFileName() + "\n"+sdate.format(date).toString() + "\n\n");
														}else{
															textArea.appendText(usb + ":\\" + " 폴더에 디렉터리 변경됨 -> " +path.getFileName()+ "\n" + sdate.format(date).toString() + "\n\n");
														}
													});
												} else if(kind == StandardWatchEventKinds.OVERFLOW) {}
											}
											boolean valid = watchKey.reset();
											if(!valid) { break; }
										}}catch(Exception e){}
									}
							};
							executorService.submit(runnable);
							subDirList(usb + ":\\");
							
					    });
					    Scene scene = new Scene(root);
					    passInput.setScene(scene);
					} catch (IOException e) {}
				    passInput.show();
				    passInput.setOnCloseRequest(ev -> {
				    		passInput.showAndWait();
				    });
				});
			} catch (Exception e) {}
		}

		public void subDirList(String source){
			File dir = new File(source); 
			File[] fileList = dir.listFiles(); 
			try{
				for(int i = 0 ; i < fileList.length ; i++){
					File file = fileList[i]; 
					if(file.isFile()){
						Platform.runLater(()->{
							fList.getItems().add(file.getName());
							fMap.put(fListCnt++, String.valueOf(file.getAbsolutePath()));
						});
					}
					else if(file.isDirectory()){
						Path directory = Paths.get(file.getPath());
						
						dirMap.put(dirListCnt++, String.valueOf(directory.toRealPath(LinkOption.NOFOLLOW_LINKS)));
						
						Runnable runnable = new Runnable(){
							@SuppressWarnings("rawtypes")
							@Override
							public void run() {
								try{
									Platform.runLater(()->dirList.getItems().add(file.getName()));
									WatchService watchService = FileSystems.getDefault().newWatchService();
									directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
					                         StandardWatchEventKinds.ENTRY_DELETE,
					                         StandardWatchEventKinds.ENTRY_MODIFY);
									while(true) {
										WatchKey watchKey = watchService.take();
										List<WatchEvent<?>> list = watchKey.pollEvents();
										for(WatchEvent watchEvent : list) {
											Kind kind = watchEvent.kind();
											Path path = (Path)watchEvent.context();
											if(kind == StandardWatchEventKinds.ENTRY_CREATE) {
												Platform.runLater(()->{
													try {
													if(path.toString().contains(".txt")){
														textArea.appendText(directory.getFileName() + " 폴더에 파일 생성됨 -> " + path.getFileName() + " \n"+sdate.format(date).toString() + "\n\n");
														fList.getItems().add(String.valueOf(path.getFileName()));
														fMap.put(fListCnt++, directory.toRealPath(LinkOption.NOFOLLOW_LINKS) + "\\" + path.toString());
													}else if(path.toString().contains(".jpg") || path.toString().contains(".png") || path.toString().contains(".gif") || path.toString().contains(".bmp")){
														textArea.appendText(directory.getFileName() + " 폴더에 이미지파일 생성됨 -> " + path.getFileName() + " \n"+sdate.format(date).toString() + "\n\n");
														fList.getItems().add(String.valueOf(path.getFileName()));
														fMap.put(fListCnt++, directory.toRealPath(LinkOption.NOFOLLOW_LINKS) + "\\" + path.toString());					
													}else if(path.toString().contains(".avi") || path.toString().contains(".wmv") || path.toString().contains(".mp4") || path.toString().contains(".mpeg")){
														textArea.appendText(directory.getFileName() + " 폴더에 동영상파일 생성됨 -> " + path.getFileName() + " \n"+sdate.format(date).toString() + "\n\n");
														fList.getItems().add(String.valueOf(path.getFileName()));
														fMap.put(fListCnt++, directory.toRealPath(LinkOption.NOFOLLOW_LINKS) + "\\" + path.toString());
													}else if(path.toString().contains(".zip")){
														textArea.appendText(directory.getFileName() + " 폴더에 압축파일 생성됨 -> " + path.getFileName() + " \n"+sdate.format(date).toString() + "\n\n");
														fList.getItems().add(String.valueOf(path.getFileName()));	
														fMap.put(fListCnt++, directory.toRealPath(LinkOption.NOFOLLOW_LINKS) + "\\" + path.toString());
													}else{
														textArea.appendText(directory.getFileName() + " 폴더에 디렉터리 생성됨 -> " +path.getFileName() + " \n"+ sdate.format(date).toString() + "\n\n");
														dirList.getItems().add(String.valueOf(path.getFileName()));
														dirMap.put(dirListCnt++, directory.toRealPath(LinkOption.NOFOLLOW_LINKS) + "\\" + path.toString());
													}
													} catch (Exception e) {}
												});
											} else if(kind == StandardWatchEventKinds.ENTRY_DELETE) {
												Platform.runLater(()->{
													if(path.toString().contains(".txt")){
														textArea.appendText(directory.getFileName() +" 폴더에 파일 삭제됨 -> " + path.getFileName() + " \n" +sdate.format(date).toString() + "\n\n");
														fList.getItems().remove(String.valueOf(path.getFileName()));
													}else if(path.toString().contains(".jpg") || path.toString().contains(".png") || path.toString().contains(".gif") || path.toString().contains(".bmp")){
														textArea.appendText(directory.getFileName() + " 폴더에 이미지파일 삭제됨 -> " + path.getFileName() + " \n"+sdate.format(date).toString() + "\n\n");
														fList.getItems().remove(String.valueOf(path.getFileName()));
													}else if(path.toString().contains(".avi") || path.toString().contains(".wmv") || path.toString().contains(".mp4") || path.toString().contains(".mpeg")){
														textArea.appendText(directory.getFileName() + " 폴더에 동영상파일 삭제됨 -> " + path.getFileName() + " \n"+sdate.format(date).toString() + "\n\n");
														fList.getItems().remove(String.valueOf(path.getFileName()));	
													}else if(path.toString().contains(".zip")){
														textArea.appendText(directory.getFileName() + " 폴더에 압축파일 삭제됨 -> " + path.getFileName() + " \n"+sdate.format(date).toString() + "\n\n");
														fList.getItems().remove(String.valueOf(path.getFileName()));	
													}else{
														textArea.appendText(directory.getFileName() + " 폴더에 디렉터리 삭제됨 -> " +path.getFileName()+ " \n" + sdate.format(date).toString() + "\n\n");
														dirList.getItems().remove(String.valueOf(path.getFileName()));
													}
												});
											} else if(kind == StandardWatchEventKinds.ENTRY_MODIFY) {
												Platform.runLater(()->{
													if(path.toString().contains(".txt")){
														textArea.appendText(directory.getFileName() + " 폴더에 파일 변경됨 -> "  + path.getFileName() + "\n" + sdate.format(date).toString() + "\n\n");
													}else if(path.toString().contains(".jpg") || path.toString().contains(".png") || path.toString().contains(".gif") || path.toString().contains(".bmp")){
														textArea.appendText(directory.getFileName() + " 폴더에 이미지파일 변경됨 -> " + path.getFileName() + "\n"+sdate.format(date).toString() + "\n\n");
													}else if(path.toString().contains(".avi") || path.toString().contains(".wmv") || path.toString().contains(".mp4") || path.toString().contains(".mpeg")){
														textArea.appendText(directory.getFileName() + " 폴더에 동영상파일 변경됨 -> " + path.getFileName() + "\n"+sdate.format(date).toString() + "\n\n");
													}else if(path.toString().contains(".zip")){
														textArea.appendText(directory.getFileName() + " 폴더에 압축파일 변경됨 -> " + path.getFileName() + "\n"+sdate.format(date).toString() + "\n\n");
													}else{
														textArea.appendText(directory.getFileName() + " 폴더에 디렉터리 변경됨 -> " + path.getFileName()+ "\n"+ sdate.format(date).toString() + "\n\n");
													}
												});
											} else if(kind == StandardWatchEventKinds.OVERFLOW) {}
										}
										boolean valid = watchKey.reset();
										if(!valid) { break; }
									}}catch(Exception e){}
								}
						};
						executorService.submit(runnable);
						subDirList(file.getCanonicalPath().toString()); 
					}
				}
			}catch(Exception e){}
		}
	}
	//
		
	/*
	 * 웹 캠 관련 메서드
	 */
	protected void setImageViewSize() {
		double height = bpWebCamPaneHolder.getHeight();
		double width = bpWebCamPaneHolder.getWidth();
		imgWebCamCapturedImage.setFitHeight(height);
		imgWebCamCapturedImage.setFitWidth(width);
		imgWebCamCapturedImage.prefHeight(height);
		imgWebCamCapturedImage.prefWidth(width);
	}

	protected void initializeWebCam(final int webCamIndex) {
		Task<Void> webCamIntilizer = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				if (selWebCam == null) {
					selWebCam = Webcam.getWebcams().get(webCamIndex);
					selWebCam.open();
				} else {
					closeCamera();
					selWebCam = Webcam.getWebcams().get(webCamIndex);
					selWebCam.open();
				}
				startWebCamStream();
				return null;
			}
		};
		new Thread(webCamIntilizer).start();
		fpBottomPane.setDisable(false);
		btnStartCamera.setDisable(true);
	}

	protected void startWebCamStream() {
		stopCamera = false;
		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				while (!stopCamera) {
					try {
						if ((grabbedImage = selWebCam.getImage()) != null) {
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									final Image mainiamge = SwingFXUtils
										.toFXImage(grabbedImage, null);
									imageProperty.set(mainiamge);
								}
							});
							grabbedImage.flush();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				return null;
			}
		};
		Thread th = new Thread(task);
		th.setDaemon(true);
		th.start();
		imgWebCamCapturedImage.imageProperty().bind(imageProperty);
	}

	private void closeCamera() {
		if (selWebCam != null) {
			selWebCam.close();
		}
	}
	//
}
