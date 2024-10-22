package fr.imta.naomod.atl;

import io.vertx.core.Vertx;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import java.io.IOException;
import java.util.List;


public class Main {
    private Vertx server;
    private TransformationManager transformationManager;

    public Main() {
        server = Vertx.vertx();
        transformationManager = new TransformationManager();
    }

    public void start() {
        var router = Router.router(server);
        
        router.route().handler(BodyHandler.create().setDeleteUploadedFilesOnEnd(true)
        );

        router.get("/transformations").handler(ctx -> {
            List<Transformation> allTransformations = transformationManager.getAllTransformations();
            System.out.println("Returning " + allTransformations.size() + " transformations");
            ctx.json(allTransformations);
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
            String inputMetamodelPath = ctx.request().getParam("inputMetamodelPath");
            String outputMetamodelPath = ctx.request().getParam("outputMetamodelPath");
        
            // Check if parameters are missing
            if (name == null || atlFilePath == null || inputMetamodelPath == null || outputMetamodelPath == null) {
                ctx.response().setStatusCode(400).end("Missing parameters");
                return;
            }
        
            try {
                // Add transformation
                Transformation transformation = transformationManager.addTransformation(name, atlFilePath, inputMetamodelPath, outputMetamodelPath);
                ctx.response().setStatusCode(201);
                ctx.json(transformation);
            } catch (IOException e) {
                ctx.response().setStatusCode(500).end("Error adding transformation: " + e.getMessage());
            }
        });
        
        router.post("/transformation/:id/apply").handler(ctx -> {
            List<FileUpload> uploads = ctx.fileUploads();

            if (uploads.size() != 1) { 
                ctx.fail(503);
            }
            else {
                String id = ctx.pathParam("id");
                try {
                    Transformation transformation = transformationManager.getTransformationById(Integer.parseInt(id));
                    if (transformation == null) {
                        ctx.response().setStatusCode(404).end("Transformation not found");
                        return;
                    }

                    String result = transformationManager.applyTransformation(transformation, uploads.get(0).uploadedFileName());
                    ctx.response().setStatusCode(200).send(result);
                } catch (IOException e) {
                    ctx.response().setStatusCode(500).end("Error applying transformation");
                }
            }
        });
        //delete transformation
        router.delete("/transformation/:id").handler(ctx -> {
            String id = ctx.pathParam("id");
            transformationManager.deleteTransformation(Integer.parseInt(id));
            ctx.response().setStatusCode(200).end("Transformation deleted");
        });


        server.createHttpServer().requestHandler(router).listen(8080);
    }

    public static void main(String[] args) {
        new Main().start();
    }
}