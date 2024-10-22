package fr.imta.naomod.atl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;

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

    public Transformation getTransformationById(int id) {
        return transformations.get(id);
    }

    public Transformation getTransformationByName(String name) {
        return transformations.values().stream()
                .filter(t -> t.name.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public Transformation addTransformation(String name, String atlFilePath, String inputMetamodelPath, String outputMetamodelPath) throws IOException {


        Path sourcePath = Paths.get(inputMetamodelPath);
        Path targePath = Paths.get(outputMetamodelPath);

        //create the folder for the transformation
        File transformationDir = new File("src/main/resources/transformations/" + name);
        if (transformationDir.exists()) {
            throw new IOException("The folder of the transformation already exists : " + transformationDir.getAbsolutePath());
        }
        transformationDir.mkdirs();
        //add the files to the folder
        File atlFile = new File("src/main/resources/transformations/" + name + "/" + name + ".atl");
        File inputMetamodelFile = new File("src/main/resources/transformations/" + name + "/" + sourcePath.getFileName());
        File outputMetamodelFile = new File("src/main/resources/transformations/" + name + "/" + targePath.getFileName());

        try {
            atlFile.createNewFile();
            inputMetamodelFile.createNewFile();
            outputMetamodelFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        Files.copy(Paths.get(atlFilePath),atlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(inputMetamodelPath),inputMetamodelFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(outputMetamodelPath),outputMetamodelFile.toPath(), StandardCopyOption.REPLACE_EXISTING);


        // Generate new ID
        int newId = transformations.size() + 1;
    
        // Create new transformation 
        Transformation transformation = new Transformation();
        transformation.id = newId;
        transformation.name = name;
        transformation.atlFile = atlFilePath;
    
        // Add input and output metamodels
        transformation.inputs.put("input", inputMetamodelPath);
        transformation.outputs.put("output", outputMetamodelPath);
    
        // Save the transformation in the map
        transformations.put(newId, transformation);
    
        return transformation;
    }


    public String applyTransformation(Transformation transformation, String inputFile) throws IOException {
        return runner.applyTransformation(inputFile, transformation);
    }


    public void deleteTransformation(int int1) {
        
        String name = transformations.get(int1).name;
        //delete the transformation from the map 
        transformations.remove(int1);
        //delete the folder of the transformation
        File transformationDir = new File("src/main/resources/transformations/" + name);
        if (transformationDir.exists()) {
            deleteDirectoryRecursively(transformationDir);
        }
    }
    
    // Recursively delete all files and directories
    private void deleteDirectoryRecursively(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectoryRecursively(file);
            }
        }
        dir.delete(); 
    }
}



















