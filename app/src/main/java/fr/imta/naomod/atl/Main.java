package fr.imta.naomod.atl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class Main {
    private Vertx server;
    private ATLRunner runner;

    public Main() {
        server = Vertx.vertx();
        runner = new ATLRunner();
    }

    public void start() {
        var router = Router.router(server);
        
        router.route().handler(BodyHandler.create());
        router.get("/transformations").handler(ctx -> {
            Transformation class2Relation = new Transformation();
            class2Relation.id = 1;
            class2Relation.name = "Class2Relational";
            class2Relation.inputs.add("Class");
            class2Relation.outputs.add("Relational");
            ctx.json(class2Relation);
        });
        router.get("/transformation/:id").handler(ctx -> {
            String id = ctx.pathParam("id");

            System.out.println("getting info on " + id);
            ctx.response().setStatusCode(404).end();
        });
        router.post("/transformation/:id/apply").handler(ctx -> {
            String id = ctx.pathParam("id");
            System.out.println("Executing " + id);
            try {
                Map<String, String> sourceMM = new HashMap<>();
                sourceMM.put("Class", "./src/main/resources/Class.ecore");
                
                Map<String, String> targetMM = new HashMap<>();
                targetMM.put("Relational", "./src/main/resources/Relational.ecore");

                File tmpFile = Files.createTempFile("", ".xmi").toFile();
                BufferedWriter writter = new BufferedWriter(new FileWriter(tmpFile));
                writter.append(ctx.body().asString());
                writter.flush();

                String result = runner.applyTransformation(tmpFile.getAbsolutePath(), sourceMM, targetMM);

                ctx.response().setStatusCode(200).send(result);
                tmpFile.delete();
            } catch (IOException e) {
                e.printStackTrace();
                ctx.response().setStatusCode(500).end();
            }

        });

        System.out.println("Starting server on port 8080");
        server.createHttpServer().requestHandler(router).listen(8080);
    }
    public static void main(String[] args) {
        new Main().start();
    }
}
