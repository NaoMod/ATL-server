package fr.imta.naomod.atl;
import java.nio.file.Files;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.HashMap;
public class Main {
    private Vertx server;
    private TransformationManager transformationManager;

    public Main() {
        server = Vertx.vertx();
        transformationManager = new TransformationManager();
    }

    public void start() {
        var router = Router.router(server);

        router.route().handler(BodyHandler.create().setDeleteUploadedFilesOnEnd(true));

        router.get("/transformations").handler(ctx -> {
            List<Transformation> allTransformations = transformationManager.getAllTransformations();
            System.out.println("Returning " + allTransformations.size() + " transformations");
            ctx.json(allTransformations);
        });

        router.get("/transformations/enabled").handler(ctx -> {
            List<Transformation> allTransformations = transformationManager.getAllTransformations();
            List<Transformation> enabledTransformations = allTransformations.stream()
                .filter(t -> t.enabled != null && t.enabled)
                .collect(Collectors.toList());
            
            System.out.println("Returning " + enabledTransformations.size() + " enabled transformations");
            ctx.json(enabledTransformations);
        });

        // Get transformation by input & output metamodels
        router.get("/transformation/hasTransformation").handler(ctx -> {
            String inputMetamodel = ctx.request().getParam("inputMetamodel");
            String outputMetamodel = ctx.request().getParam("outputMetamodel");

            if (inputMetamodel == null || outputMetamodel == null) {
                ctx.response().setStatusCode(400).end("Both inputMetamodel and outputMetamodel are required");
                return;
            }

            // Convert input and output metamodel parameters to lowercase
            String normalizedInputMetamodel = inputMetamodel.toLowerCase();
            String normalizedOutputMetamodel = outputMetamodel.toLowerCase();

            List<Transformation> allTransformations = transformationManager.getAllTransformations();
            List<String> matchingTransformations = allTransformations.stream()
                .filter(transformation -> {
                    boolean matchesInput = transformation.inputMetamodels.stream()
                        .anyMatch(mm -> mm.path != null && mm.path.toLowerCase().contains(normalizedInputMetamodel));
                    boolean matchesOutput = transformation.outputMetamodels.stream()
                        .anyMatch(mm -> mm.path != null && mm.path.toLowerCase().contains(normalizedOutputMetamodel));
                    return matchesInput && matchesOutput;
                })
                .map(transformation -> transformation.name)
                .collect(Collectors.toList());

            if (matchingTransformations.isEmpty()) {
                ctx.response().setStatusCode(404).end("No transformations found for the given metamodels");
            } else {
                ctx.json(matchingTransformations);
            }
        });
                
        router.get("/transformation/:idOrName").handler(ctx -> {
            String idOrName = ctx.pathParam("idOrName");
            Transformation transformation = null;
            // Try to parse as integer for ID
            try {
                int id = Integer.parseInt(idOrName);
                transformation = transformationManager.getTransformationById(id);
            } catch (NumberFormatException e) {
                // If not an integer, treat as name
                transformation = transformationManager.getTransformationByName(idOrName);
            }

            if (transformation != null) {
                ctx.json(transformation);
            } else {
                ctx.response()
                        .setStatusCode(404)
                        .end("Transformation not found with ID or name: " + idOrName);
            }
        });

        router.post("/transformation/add").handler(ctx -> {
            // Get request parameters
            String name = ctx.request().getParam("name");
            String atlFilePath = ctx.request().getParam("atlFilePath");
            String description = ctx.request().getParam("description"); 
            
            // Get input metamodel paths as a list
            List<String> inputMetamodelPaths = new ArrayList<>();
            int inputIndex = 1;
            while (true) {
                String inputPath = ctx.request().getParam("inputMetamodelPath" + inputIndex);
                if (inputPath == null) break;
                inputMetamodelPaths.add(inputPath);
                inputIndex++;
            }
        
            // Get output metamodel paths as a list
            List<String> outputMetamodelPaths = new ArrayList<>();
            int outputIndex = 1;
            while (true) {
                String outputPath = ctx.request().getParam("outputMetamodelPath" + outputIndex);
                if (outputPath == null) break;
                outputMetamodelPaths.add(outputPath);
                outputIndex++;
            }
        
            // Check if parameters are missing
            if (name == null || atlFilePath == null || inputMetamodelPaths.isEmpty() || outputMetamodelPaths.isEmpty()) {
                ctx.response().setStatusCode(400).end("Missing parameters");
                return;
            }
        
            try {
                // Add transformation with multiple metamodels
                Transformation transformation = transformationManager.addTransformation(name, atlFilePath,
                        inputMetamodelPaths, outputMetamodelPaths,description);
                ctx.response().setStatusCode(201);
                ctx.json(transformation);
            } catch (IOException e) {
                ctx.response().setStatusCode(500).end("Error adding transformation: " + e.getMessage());
            }
        });


        router.get("/debug/transformations").handler(ctx -> {
            List<Transformation> allTransformations = transformationManager.getAllTransformations();
            System.out.println("Total transformations: " + allTransformations.size());
            for (Transformation t : allTransformations) {
                System.out.println("Name: " + t.name);
                System.out.println("ATL files: " + t.atlFile);
                System.out.println("Folder: " + t.folderPath);
            }
            ctx.json(allTransformations);
        });


        // Search for a term in all atl files transformations

        router.get("/transformations/search").handler(ctx -> {
            String searchTerm = ctx.request().getParam("query");
            
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                ctx.response().setStatusCode(400).end("Search query is required");
                return;
            }
        
            try {
                List<Transformation> allTransformations = transformationManager.getAllTransformations();
                List<SearchResult> results = new ArrayList<>();
                
                for (Transformation transformation : allTransformations) {
                    if (transformation.atlFile != null && !transformation.atlFile.isEmpty()) {
                        try {
                            File file = new File(transformation.folderPath, transformation.atlFile);
                            if (file.exists()) {
                                String atlContent = Files.readString(file.toPath());
                                if (atlContent.toLowerCase().contains(searchTerm.toLowerCase())) {
                                    results.add(new SearchResult(
                                        transformation.name,
                                        transformation.atlFile,
                                        highlightSearchTerm(atlContent, searchTerm)
                                    ));
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("Error reading file " + transformation.atlFile + ": " + e.getMessage());
                        }
                    }
                }     
                ctx.json(results);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.response()
                    .setStatusCode(500)
                    .end("Error searching transformations: " + e.getMessage());
            }
        });


        

        // Apply a transformation by ID or name
        router.post("/transformation/:idOrName/apply").handler(ctx -> {
            List<FileUpload> uploads = ctx.fileUploads();
            
            if (uploads.size() == 0) {
                ctx.fail(503);
            } else {
                String idOrName = ctx.pathParam("idOrName");
                try {
                    Transformation transformation = null;
                    
                    // Try to parse as integer for ID
                    try {
                        int id = Integer.parseInt(idOrName);
                        transformation = transformationManager.getTransformationById(id);
                    } catch (NumberFormatException e) {
                        // If not an integer, treat as name
                        transformation = transformationManager.getTransformationByName(idOrName);
                    }
                     
                    if (transformation == null) {
                        ctx.response()
                            .setStatusCode(404)
                            .end("Transformation not found with ID or name: " + idOrName);
                        return;
                    }
                    
                    Map<String, String> inputs = new HashMap<>();
                    
                    for (var upload : uploads) {
                        inputs.put(upload.name(), upload.uploadedFileName());
                    }

                    String result = transformationManager.applyTransformation(transformation, inputs);
                    ctx.response().setStatusCode(200).send(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    ctx.response()
                        .setStatusCode(500)
                        .end("Error applying transformation");
                }
            }
        });

        // Apply a chain of transformations 
        router.post("/transformation/chain").handler(ctx -> {
            try {
                // Get the transformation chain from form field
                String transformationChainStr = ctx.request().getFormAttribute("transformationChain");
                if (transformationChainStr == null || transformationChainStr.isEmpty()) {
                    ctx.response().setStatusCode(400).end("Missing or empty transformation chain");
                    return;
                }

                // Parse the JSON array string into List<String>
                JsonArray jsonArray = new JsonArray(transformationChainStr);
                List<String> chainedTransformations = jsonArray.stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());

                // Get the uploaded file
                List<FileUpload> uploads = ctx.fileUploads();
                if (uploads.size() != 1) {
                    ctx.response().setStatusCode(400).end("Exactly one input file required");
                    return;
                }

                // Apply the chain of transformations
                String result = transformationManager.applyTransformationChain(
                        chainedTransformations,
                        uploads.get(0).uploadedFileName());

                ctx.response().setStatusCode(200).send(result);
            } catch (Exception e) {
                ctx.response()
                        .setStatusCode(500)
                        .end("Error applying transformation chain: " + e.getMessage());
            }
        });

        // delete transformation by name or id

        // router.delete("/transformation/:idOrName").handler(ctx -> {
        //     String idOrName = ctx.pathParam("idOrName");

        //     // If it's not an integer, assume it's a name
        //     System.out.println("Deleting by name: " + idOrName);
        //     transformationManager.deleteTransformationByName(idOrName);
        //     ctx.response().setStatusCode(200).end("Transformation deleted by name" + idOrName);

        // });

        //Transformations grouped by their input metamodels
        router.get("/transformations/byInputMetamodel").handler(ctx -> {
            List<Transformation> allTransformations = transformationManager.getAllTransformations();
            Map<String, List<String>> categorizedTransformations = new HashMap<>();
            
            // First, categorize all transformations
            for (Transformation transformation : allTransformations) {
                if (transformation.inputMetamodels == null || transformation.inputMetamodels.isEmpty()) {
                    continue;
                }
                
                for (NamedFile inputMetamodel : transformation.inputMetamodels) {
                    // Skip if path is null or empty
                    if (inputMetamodel.path == null || inputMetamodel.path.isEmpty()) {
                        continue;
                    }
                    
                    // Get the filename from the path
                    String fileName = new File(inputMetamodel.path).getName();
                    // Get metamodel name (remove .ecore extension)
                    String metamodelName = fileName.replace(".ecore", "");
                    
                    if (!categorizedTransformations.containsKey(metamodelName)) {
                        categorizedTransformations.put(metamodelName, new ArrayList<>());
                    }
                    categorizedTransformations.get(metamodelName).add(transformation.name);
                }
            }
            
            // Filter to keep only metamodels with more than 2 transformations
            Map<String, List<String>> filteredTransformations = categorizedTransformations.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 2)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
            
            ctx.json(filteredTransformations);
        });

        server.createHttpServer().requestHandler(router).listen(8080);
    }

        

    // Highlight the search term in the content and return a context around it
    private static String highlightSearchTerm(String content, String searchTerm) {

        int index = content.toLowerCase().indexOf(searchTerm.toLowerCase());
        if (index == -1) return "";       
        // Extract a cleaner context (about 50 chars before and after)
        int contextStart = Math.max(0, index - 50);
        int contextEnd = Math.min(content.length(), index + searchTerm.length() + 50);
        
        // Find the start of the current line
        while (contextStart > 0 && content.charAt(contextStart) != '\n') {
            contextStart--;
        }
        
        // Find the end of the current line
        while (contextEnd < content.length() && content.charAt(contextEnd) != '\n') {
            contextEnd++;
        }
        
        String context = content.substring(contextStart, contextEnd).trim();
        return context.replaceAll("(?i)" + Pattern.quote(searchTerm), "**$0**");
    }

    public static void main(String[] args) {
        new Main().start();
    }
}