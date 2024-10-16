package fr.imta.naomod.atl;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
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
        
        router.route().handler(BodyHandler.create());
        
         router.get("/transformations").handler(ctx -> {
            List<Transformation> allTransformations = transformationManager.getAllTransformations();
            System.out.println("Returning " + allTransformations.size() + " transformations");
            ctx.response()
               .putHeader("content-type", "application/json")
               .end(Json.encodePrettily(allTransformations));
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
            String id = ctx.pathParam("id");
            try {
                Transformation transformation = transformationManager.getTransformation(Integer.parseInt(id));
                if (transformation == null) {
                    ctx.response().setStatusCode(404).end("Transformation not found");
                    return;
                }

                File tmpFile = Files.createTempFile("", ".xmi").toFile();
                Files.write(tmpFile.toPath(), ctx.body().toString().getBytes());
                String result = transformationManager.applyTransformation(transformation, tmpFile.getAbsolutePath());
                ctx.response().setStatusCode(200).send(result);
                tmpFile.delete();
            } catch (IOException e) {
                ctx.response().setStatusCode(500).end("Error applying transformation");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        server.createHttpServer().requestHandler(router).listen(8080);
    }

    public static void main(String[] args) {
        new Main().start();
    }
}