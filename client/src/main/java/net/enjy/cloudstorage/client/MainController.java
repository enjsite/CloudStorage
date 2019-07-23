package net.enjy.cloudstorage.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import net.enjy.cloudstorage.common.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML
    TextField tfFileName;

    @FXML
    ListView<String> filesList;

    @FXML
    ListView<String> serverFilesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.start();
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();
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
        refreshLocalFilesList();
        requestServerFilesList();
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        if (tfFileName.getLength() > 0) {
            Network.sendMsg(new FileRequest(tfFileName.getText()));
            tfFileName.clear();
        }
    }

    public void pressOnUploadBtn (ActionEvent actionEvent) throws IOException{
        if (tfFileName.getLength() > 0) {
            if (Files.exists(Paths.get("client_storage/" + tfFileName.getText()))) {
                Network.sendMsg(new FileMessage(Paths.get("client_storage/" + tfFileName.getText())));
                tfFileName.clear();
            }
        }
    }

    public void saveFileName(MouseEvent event) {
        tfFileName.setText(filesList.getSelectionModel().getSelectedItem());
    }

    public void saveServerFileName(MouseEvent event) {
        tfFileName.setText(serverFilesList.getSelectionModel().getSelectedItem());
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
