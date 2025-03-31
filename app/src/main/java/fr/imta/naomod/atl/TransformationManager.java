package fr.imta.naomod.atl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.imta.naomod.atl.runners.ATLRunner;
import fr.imta.naomod.atl.runners.EMFTVMRunner;
import fr.imta.naomod.atl.runners.EMFVMRunner;
import io.vertx.core.json.Json;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;

public class TransformationManager {
    private Map<String, Transformation> transformations;
    private Map<String, ATLRunner> runners = new HashMap<>();

    public TransformationManager() {
        transformations = new HashMap<>();
        runners.put("EMFTVM", new EMFTVMRunner());
        runners.put("EMFVM", new EMFVMRunner());
        loadTransformations();
    }

    public void loadTransformations() {
        // List to store directories to process
        List<File> dirsToProcess = new ArrayList<>();

        // Add original transformations directory
        File originalDir = new File("./path//to/youDirectory");//put your atl zoo project directory
        if (originalDir.exists()) {
            dirsToProcess.add(originalDir);
        }

        // Add user transformations directory
        File userDir = new File("src/main/resources/userTransformations");
        if (userDir.exists()) {
            dirsToProcess.add(userDir);
        }

        // Process each directory
        for (File dir : dirsToProcess) {
            File[] transformationDirs = dir.listFiles(File::isDirectory);

            if (transformationDirs != null) {
                for (File transformationDir : transformationDirs) {
                    System.out.println("Processing folder: " + transformationDir.getName());
                    File config = findFileWithExtension(transformationDir, "json");

                    if (config == null) continue;

                    String content;
                    try {
                        content = Files.readString(config.toPath());
                        Transformation[] transformationsJson = Json.decodeValue(content, Transformation[].class);
                        for (var t : transformationsJson) {
                            System.out.println("Transformation: " + t.name);
                            t.folderPath = transformationDir.getAbsolutePath();
                            transformations.put(t.name, t);
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private File findFileWithExtension(File dir, String extension) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(extension));
        return (files != null && files.length > 0) ? files[0] : null;
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

        // Create new transformation
        Transformation transformation = new Transformation();
        transformation.name = name;
        transformation.atlFile = atlFilePath;

        // Add input metamodels
        for (int i = 0; i < inputMetamodelPaths.size(); i++) {
            NamedFile m = new NamedFile("IN", inputMetamodelPaths.get(i)); // fixme: name of MM should be provided in request
            transformation.inputMetamodels.add(m);
        }

        // Add output metamodels
        for (int i = 0; i < outputMetamodelPaths.size(); i++) {
            NamedFile m = new NamedFile("OUT", outputMetamodelPaths.get(i)); // fixme: name of MM should be provided in request
            transformation.outputMetamodels.add(m);
        }

        // Save the transformation in the map
        transformations.put(name, transformation);

        return transformation;
    }

    public String applyTransformation(Transformation transformation, Map<String, String> inputFiles) throws Exception {
        return runners.get(transformation.compiler).applyTransformation(inputFiles, transformation);
    }

    public void deleteTransformation(String name) {
        // delete the transformation from the map
        transformations.remove(name);
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
            deleteTransformation(transformation.name);
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
            throws Exception {
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
                Map<String, String> input = new HashMap<>();
                input.put("IN", currentInputFile); //fixme: we assume that intermediate transformations only have 1 input & ouput
                String output = runners.get(currentTransformation.compiler).applyTransformation(input, currentTransformation);

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
