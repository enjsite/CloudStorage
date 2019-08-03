package net.enjy.cloudstorage.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import net.enjy.cloudstorage.common.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    TextField tfClientFileName;

    @FXML
    TextField tfServerFileName;

    @FXML
    TextField tfLogin;

    @FXML
    TextField tfPassword;

    @FXML
    Button btnAuthorize;

    @FXML
    ListView<String> filesList;

    @FXML
    ListView<String> serverFilesList;

    @FXML
    HBox hbNotAuth;

    @FXML
    HBox hbAuth;

    @FXML
    HBox hbCommandsClient;

    @FXML
    HBox hbCommandsServer;

    @FXML
    Text tLogin;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        refreshLocalFilesList();
    }

    public void connect() {
        if (Network.isConnected()) {
            return;
        }
            Network.start();
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        AbstractMessage am = Network.readObject();
                        if (am instanceof AuthOk) {
                            showAuthorizedWindow();
                            requestServerFilesList();
                        }
                        if (am instanceof FileMessage) {
                            FileMessage fm = (FileMessage) am;
                            Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                            refreshLocalFilesList();
                        }
                        if (am instanceof FileListMessage) {
                            FileListMessage fl = (FileListMessage) am;
                            refreshServerFilesList(fl);
                        }
                    }
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                } finally {
                    Network.stop();
                }
            });
            t.setDaemon(true);
            t.start();

    }

    public void pressOnAuthorizeBtn(ActionEvent actionEvent) {
        if ((tfLogin.getLength() > 0) && (tfPassword.getLength() > 0)) {
            connect();
            Network.sendMsg(new AuthMessage(tfLogin.getText(), tfPassword.getText()));
        }
    }

    public void pressOnDeleteClientFileBtn(ActionEvent actionEvent) throws IOException {
        if (tfClientFileName.getLength() > 0) {
            Files.deleteIfExists(Paths.get("client_storage/" + tfClientFileName.getText()));
            tfClientFileName.clear();
            refreshLocalFilesList();
        }
    }

    public void pressOnDeleteServerFileBtn(ActionEvent actionEvent) {
        if (tfServerFileName.getLength() > 0) {
            Network.sendMsg(new FileDeleteRequest(tfServerFileName.getText()));
            tfServerFileName.clear();
        }
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        if (tfServerFileName.getLength() > 0) {
            Network.sendMsg(new FileRequest(tfServerFileName.getText()));
            tfServerFileName.clear();
        }
    }

    public void pressOnUploadBtn (ActionEvent actionEvent) throws IOException{
        if (tfClientFileName.getLength() > 0) {
            if (Files.exists(Paths.get("client_storage/" + tfClientFileName.getText()))) {
                Network.sendMsg(new FileMessage(Paths.get("client_storage/" + tfClientFileName.getText())));
                tfClientFileName.clear();
            }
        }
    }

    public void saveFileName(MouseEvent event) {
        tfClientFileName.setText(filesList.getSelectionModel().getSelectedItem());
    }

    public void saveServerFileName(MouseEvent event) {
        tfServerFileName.setText(serverFilesList.getSelectionModel().getSelectedItem());
    }

    public void refreshLocalFilesList() {
        updateUI(() -> {
            try {
                filesList.getItems().clear();
                Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> filesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void showAuthorizedWindow() {
        hbNotAuth.setVisible(false);
        hbNotAuth.setManaged(false);
        hbAuth.setManaged(true);
        hbAuth.setVisible(true);
        hbCommandsClient.setManaged(true);
        hbCommandsClient.setVisible(true);
        hbCommandsServer.setManaged(true);
        hbCommandsServer.setVisible(true);
        tLogin.setText(tfLogin.getText());
    }

    public void refreshServerFilesList(FileListMessage fl) {
        updateUI(() -> {
            try {
                serverFilesList.getItems().clear();
                for (String fileName : fl.getFileList()) {
                    serverFilesList.getItems().add(fileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void requestServerFilesList() {
        Network.sendMsg(new FileListRequest());
    }


    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
