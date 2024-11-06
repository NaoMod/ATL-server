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
        // List to store directories to process
        List<File> dirsToProcess = new ArrayList<>();

        // Add original transformations directory
        File originalDir = new File("src/main/resources/transformations");
        if (originalDir.exists()) {
            dirsToProcess.add(originalDir);
        }

        // Add user transformations directory
        File userDir = new File("src/main/resources/userTransformations");
        if (userDir.exists()) {
            dirsToProcess.add(userDir);
        }

        int idCounter = 1; // Counter for generating unique IDs

        // Process each directory
        for (File dir : dirsToProcess) {
            File[] transformationDirs = dir.listFiles(File::isDirectory);

            if (transformationDirs != null) {
                for (File transformationDir : transformationDirs) {
                    String transformationName = transformationDir.getName();

                    File atlFile = findFileWithExtension(transformationDir, ".atl");
                    if (atlFile == null) {
                        continue; // Skip if no ATL file found
                    }

                    Transformation transformation = new Transformation();
                    transformation.id = idCounter++;
                    transformation.name = transformationName;
                    transformation.atlFile = atlFile.getAbsolutePath();
                    // Load the description from a description.txt file if it exists
                    File descFile = new File(transformationDir, "description.txt");
                    if (descFile.exists()) {
                        try {
                            transformation.description = Files.readString(descFile.toPath()).trim();
                        } catch (IOException e) {
                            transformation.description = "Error reading description: " + e.getMessage();
                        }
                    } else {
                        transformation.description = "No description available";
                    }

                    String[] parts = transformationName.split("2");
                    if (parts.length == 2) {
                        // Identify metamodels by looking at .ecore files in the directory
                        File[] ecoreFiles = transformationDir.listFiles((dir1, name) -> name.endsWith(".ecore"));

                        if (ecoreFiles != null) {
                            for (File ecoreFile : ecoreFiles) {
                                String metamodelName = ecoreFile.getName().replace(".ecore", "");

                                // If metamodel is part of input names (before 2)
                                if (parts[0].contains(metamodelName)) {
                                    Map<String, String> inputMap = findEcoreFile(transformationDir, metamodelName);
                                    if (inputMap != null) {
                                        transformation.SourceMetamodels.putAll(inputMap);
                                    }
                                }

                                // If metamodel is part of output names (after 2)
                                if (parts[1].contains(metamodelName)) {
                                    Map<String, String> outputMap = findEcoreFile(transformationDir, metamodelName);
                                    if (outputMap != null) {
                                        transformation.TargetMetamodels.putAll(outputMap);
                                    }
                                }
                            }
                        }
                    }

                    transformations.put(transformation.id, transformation);
                }
            }
        }
    }

    private File findFileWithExtension(File dir, String extension) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(extension));
        return (files != null && files.length > 0) ? files[0] : null;
    }

    private Map<String, String> findEcoreFile(File dir, String baseName) {
        Map<String, String> result = new HashMap<>();
        File[] ecoreFiles = dir.listFiles(
                (d, name) -> name.toLowerCase().startsWith(baseName.toLowerCase()) && name.endsWith(".ecore"));
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

    public Transformation addTransformation(String name, String atlFilePath,
            List<String> inputMetamodelPaths, List<String> outputMetamodelPaths, String description) throws IOException {

        // create the userTransformations directory first
        File userTransformationsDir = new File("src/main/resources/userTransformations");
        if (!userTransformationsDir.exists()) {
            userTransformationsDir.mkdirs();
        }

        // create the folder for the transformation inside userTransformations
        File transformationDir = new File("src/main/resources/userTransformations/" + name);
        if (transformationDir.exists()) {
            throw new IOException(
                    "The folder of the transformation already exists : " + transformationDir.getAbsolutePath());
        }
        transformationDir.mkdirs();

        File descFile = new File(transformationDir, "description.txt");
        Files.writeString(descFile.toPath(), description != null ? description : "No description provided");

        // add the ATL file to the folder
        File atlFile = new File("src/main/resources/userTransformations/" + name + "/" + name + ".atl");
        atlFile.createNewFile();
        Files.copy(Paths.get(atlFilePath), atlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Copy all input metamodel files
        for (int i = 0; i < inputMetamodelPaths.size(); i++) {
            String inputPath = inputMetamodelPaths.get(i);
            Path sourcePath = Paths.get(inputPath);
            File inputMetamodelFile = new File(
                    "src/main/resources/userTransformations/" + name + "/" + sourcePath.getFileName());
            inputMetamodelFile.createNewFile();
            Files.copy(Paths.get(inputPath), inputMetamodelFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Copy all output metamodel files
        for (int i = 0; i < outputMetamodelPaths.size(); i++) {
            String outputPath = outputMetamodelPaths.get(i);
            Path targetPath = Paths.get(outputPath);
            File outputMetamodelFile = new File(
                    "src/main/resources/userTransformations/" + name + "/" + targetPath.getFileName());
            outputMetamodelFile.createNewFile();
            Files.copy(Paths.get(outputPath), outputMetamodelFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Generate new ID
        int newId = transformations.size() + 1;

        // Create new transformation
        Transformation transformation = new Transformation();
        transformation.id = newId;
        transformation.name = name;
        transformation.atlFile = atlFilePath;

        // Add input metamodels
        for (int i = 0; i < inputMetamodelPaths.size(); i++) {
            transformation.SourceMetamodels.put("input" + (i + 1), inputMetamodelPaths.get(i));
        }

        // Add output metamodels
        for (int i = 0; i < outputMetamodelPaths.size(); i++) {
            transformation.TargetMetamodels.put("output" + (i + 1), outputMetamodelPaths.get(i));
        }

        // Save the transformation in the map
        transformations.put(newId, transformation);

        return transformation;
    }

    public String applyTransformation(Transformation transformation, String inputFile) throws IOException {
        return runner.applyTransformation(inputFile, transformation);
    }

    public void deleteTransformation(int int1) {

        String name = transformations.get(int1).name;
        // delete the transformation from the map
        transformations.remove(int1);
        // delete the folder of the transformation
        File transformationDir = new File("src/main/resources/transformations/" + name);
        if (transformationDir.exists()) {
            deleteDirectoryRecursively(transformationDir);
        }
    }

    public void deleteTransformationByName(String idOrName) {
        Transformation transformation = getTransformationByName(idOrName);
        System.out.println("Transformation to delete: " + idOrName);
        System.out.println(" the whole Transformation to delete: " + transformation);
        System.out.println(getAllTransformations());
        if (transformation != null) {
            deleteTransformation(transformation.id);
            System.out.println("Transformation deleted: " + idOrName);
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

    public String applyTransformationChain(List<String> transformationNames, String initialInputFile)
            throws IOException {
        if (transformationNames == null || transformationNames.isEmpty()) {
            throw new IllegalArgumentException("Transformation chain cannot be empty");
        }

        String currentInputFile = initialInputFile;
        String finalOutput = null;
        Path tempDir = Files.createTempDirectory("chain_transformation_");

        try {
            // Apply each transformation in sequence
            for (int i = 0; i < transformationNames.size(); i++) {
                // Get current transformation
                Transformation currentTransformation = getTransformationByName(transformationNames.get(i));
                if (currentTransformation == null) {
                    throw new IllegalArgumentException("Transformation not found: " + transformationNames.get(i));
                }

                // Apply transformation
                String output = runner.applyTransformation(currentInputFile, currentTransformation);

                if (i < transformationNames.size() - 1) {
                    // Save intermediate result to temp file
                    Path tempOutput = tempDir.resolve("intermediate_" + i + ".xmi");
                    Files.write(tempOutput, output.getBytes());
                    currentInputFile = tempOutput.toString();
                } else {
                    // Keep final output
                    finalOutput = output;
                }
            }

            return finalOutput;

        } finally {
            // Clean up temp files
            deleteDirectoryRecursively(tempDir.toFile());
        }
    }

}