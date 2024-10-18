package fr.imta.naomod.atl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransformationManager {
    private Map<Integer, Transformation> transformations;
    private ATLRunner runner;

    public TransformationManager() {
        transformations = new HashMap<>();
        runner = new ATLRunner();
        loadTransformations();
    }

    public void loadTransformations() {
        File dir = new File("src/main/resources/transformations");
        File[] transformationDirs = dir.listFiles(File::isDirectory);
        
        if (transformationDirs != null) {
            for (int i = 0; i < transformationDirs.length; i++) {
                File transformationDir = transformationDirs[i];
                String transformationName = transformationDir.getName();
                
                File atlFile = findFileWithExtension(transformationDir, ".atl");
                if (atlFile == null) {
                    continue; // Skip if no ATL file found
                }

                Transformation transformation = new Transformation();
                transformation.id = i + 1;
                transformation.name = transformationName;
                transformation.atlFile = atlFile.getAbsolutePath();
                
                String[] parts = transformationName.split("2");
                if (parts.length == 2) {
                    transformation.inputs = findEcoreFile(transformationDir, parts[0]);
                    transformation.outputs = findEcoreFile(transformationDir, parts[1]);
                }
                
                transformations.put(transformation.id, transformation);
            }
        }
    }

    private File findFileWithExtension(File dir, String extension) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(extension));
        return (files != null && files.length > 0) ? files[0] : null;
    }

    private Map<String, String> findEcoreFile(File dir, String baseName) {
        Map<String, String> result = new HashMap<>();
        File[] ecoreFiles = dir.listFiles((d, name) -> name.toLowerCase().startsWith(baseName.toLowerCase()) && name.endsWith(".ecore"));
        if (ecoreFiles != null && ecoreFiles.length > 0) {
            String metamodelName = ecoreFiles[0].getName().replace(".ecore", "");
            result.put(metamodelName, ecoreFiles[0].getAbsolutePath());
        }
        return result;
    }

    public List<Transformation> getAllTransformations() {
        return new ArrayList<>(transformations.values());
    }

    public Transformation getTransformation(int id) {
        return transformations.get(id);
    }

    public String applyTransformation(Transformation transformation, String inputFile) throws IOException {
        return runner.applyTransformation(inputFile, transformation);
    }
}



















