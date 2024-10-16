package fr.imta.naomod.atl;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
        
        router.get("/transformation/:id").handler(ctx -> {
            String id = ctx.pathParam("id");
            Transformation transformation = transformationManager.getTransformation(Integer.parseInt(id));
            if (transformation != null) {
                ctx.json(transformation);
            } else {
                ctx.response().setStatusCode(404).end("Transformation not found");
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
                    Transformation transformation = transformationManager.getTransformation(Integer.parseInt(id));
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

        server.createHttpServer().requestHandler(router).listen(8080);
    }

    public static void main(String[] args) {
        new Main().start();
    }
}