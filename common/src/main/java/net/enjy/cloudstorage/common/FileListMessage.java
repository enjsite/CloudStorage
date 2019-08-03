package net.enjy.cloudstorage.common;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FileListMessage extends AbstractMessage {
    private List<String> fileList = new ArrayList<>();

    public List<String> getFileList() {
        return fileList;
    }

    public FileListMessage (Path path) throws IOException {

        fileList.clear();

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                System.out.println(file.getFileName());
                fileList.add(file.getFileName().toString());
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
