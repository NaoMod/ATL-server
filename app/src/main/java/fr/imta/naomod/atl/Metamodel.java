package fr.imta.naomod.atl;

import java.nio.file.Path;

public class Metamodel {
    public String name;
    public String metamodel;

    public String getMetamodelName() {
        Path path = Path.of(metamodel);

        return path.getFileName().toString().split(".")[0];
    }
}
