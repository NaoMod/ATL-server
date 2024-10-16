package fr.imta.naomod.atl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransformationManager {
    private Map<Integer, Transformation> transformations;
    private ATLRunner runner;

    public TransformationManager() {
        transformations = new HashMap<>();
        loadTransformations();
    }

    private void loadTransformations() {
        File dir = new File("src/main/resources/transformations");
        File[] files = dir.listFiles((d, name) -> name.endsWith(".atl"));
        
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String fileName = files[i].getName();
                String transformationName = fileName.substring(0, fileName.lastIndexOf('.'));
                
                Transformation transformation = new Transformation();
                transformation.id = i + 1;
                transformation.name = transformationName;
                transformation.atlFile = files[i].getAbsolutePath();
                
                String[] parts = transformationName.split("2");
                if (parts.length == 2) {
                    transformation.inputs = findEcoreFile(dir, parts[0]);
                    transformation.outputs = findEcoreFile(dir, parts[1]);
                }
                
                transformations.put(transformation.id, transformation);
            }
        }
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

    public String applyTransformation(Transformation transformation, String inputFile) throws Exception {
        return runner.applyTransformation(inputFile, transformation.inputs, transformation.outputs, transformation.atlFile);
    }
}



















