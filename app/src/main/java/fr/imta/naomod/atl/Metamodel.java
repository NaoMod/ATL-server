package fr.imta.naomod.atl;

import java.nio.file.Path;

public class Metamodel {
    public String name;
    public String metamodel;

    public Metamodel() {}

    public Metamodel(String name, String metamodel) {
        this.name = name;
        this.metamodel = metamodel;
    }

    public String getMetamodelName(String prefix) {
        Path path = Path.of(prefix + "/" + metamodel);
        return path.getFileName().toString().split("\\.")[0];
    }

    public String getPath() {
        return metamodel;
    }
}
