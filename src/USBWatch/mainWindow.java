package USBWatch;

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class mainWindow extends Application{
	@Override
	public void start(Stage primaryStage) throws Exception {
		Stage dialog = new Stage(StageStyle.UTILITY);
		
		dialog.initModality(Modality.NONE);
		dialog.initOwner(primaryStage);
		dialog.setTitle("                                                                      "
				+ "                      USB감시보안프로그램");
		
		FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/root.fxml"));
		Parent root = (Parent)loader.load();
		
		ClientRootController rootController = (ClientRootController)loader.getController();
		rootController.setPrimaryStage(primaryStage);
		rootController.setDialog(dialog);
		
		Scene scene = new Scene(root);
		//scene.getStylesheets().add(getClass().getResource("css.css").toString());
		
		dialog.setScene(scene);
		//dialog.setAlwaysOnTop(true);
		
		Image image = new Image("/USBWatch/img/icon.png");//패키지명/이미지파일명
		dialog.getIcons().add(image);
		dialog.show();		
		dialog.requestFocus();
		dialog.setResizable(false);
		
		
		dialog.setOnCloseRequest(ev1 -> {
			String password = rootController.password;
			boolean closeState = rootController.closeState;
			Platform.runLater(()->{
				if(closeState){
					Runtime.getRuntime().exit(0);
					if(rootController.passInput.isShowing())rootController.passInput.close();
					return;
				}
				Stage closeDlg = new Stage(StageStyle.UTILITY);
				closeDlg.initModality(Modality.WINDOW_MODAL);
				closeDlg.initOwner(primaryStage);
				closeDlg.setTitle("패스워드 입력");
				closeDlg.setResizable(false);
			    FXMLLoader loaderDlg = new FXMLLoader(getClass().getResource("fxml/close.fxml"));
			    Parent rootDlg;
				try {
					rootDlg = (Parent)loaderDlg.load();
				    Button closeOK = (Button)rootDlg.lookup("#closeOK");
				    TextField closePwd = (TextField)rootDlg.lookup("#closePwd");
				    Text txt = (Text)rootDlg.lookup("#txt");
				    closeOK.setOnAction(ev2 -> {
				    	if(closePwd.getText().equals(password)){
					    	dialog.close();
					    	closeDlg.close();
					    	rootController.executorService.shutdownNow();
					    	Runtime.getRuntime().exit(0);
				    	}else if(closePwd.getText().equals("")){
				    		txt.setFill(Paint.valueOf("RED"));
			    			return;
				    	}else{
				    		txt.setFill(Paint.valueOf("RED"));
				    		txt.setText("올바른 비밀번호를 입력하세요");
				    		return;
				    	}
				    });
				    
				    closePwd.setOnKeyPressed(ev -> {
				    	if (ev.getCode() == KeyCode.ENTER){
				    		if(closePwd.getText().equals(password)){
						    	dialog.close();
						    	closeDlg.close();
						    	rootController.executorService.shutdownNow();
						    	Runtime.getRuntime().exit(0);
					    	}else if(closePwd.getText().equals("")){
					    		txt.setFill(Paint.valueOf("RED"));
				    			return;
					    	}else{
					    		txt.setFill(Paint.valueOf("RED"));
					    		txt.setText("올바른 비밀번호를 입력하세요");
					    		return;
					    	}
				    	}
				    });
				    
				    Scene sceneDlg = new Scene(rootDlg);
				    closeDlg.setScene(sceneDlg);
				} catch (IOException e) {}
				dialog.show();
				closeDlg.show();
				closeDlg.setAlwaysOnTop(true);
				closeDlg.toFront();
				closeDlg.requestFocus();
			});
		});
	}
	public static void main(String args[]){
		launch(args);
	}
}