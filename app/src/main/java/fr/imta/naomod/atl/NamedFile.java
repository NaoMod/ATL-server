package fr.imta.naomod.atl;

import java.nio.file.Path;

public class NamedFile {
    public String name;
    public String path;

    public NamedFile() {}

    public NamedFile(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getFileName(String prefix) {
        Path path = Path.of(prefix + "/" + this.path);
        return path.getFileName().toString().split("\\.")[0];
    }
}
