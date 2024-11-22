package fr.imta.naomod.atl.runners;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.m2m.atl.core.emf.EMFInjector;
import org.eclipse.m2m.atl.core.emf.EMFModelFactory;
import org.eclipse.m2m.atl.core.emf.EMFReferenceModel;
import org.eclipse.m2m.atl.emftvm.EmftvmFactory;
import org.eclipse.m2m.atl.emftvm.ExecEnv;
import org.eclipse.m2m.atl.emftvm.Metamodel;
import org.eclipse.m2m.atl.emftvm.Model;
import org.eclipse.m2m.atl.emftvm.compiler.AtlToEmftvmCompiler;
import org.eclipse.m2m.atl.emftvm.util.DefaultModuleResolver;

import fr.imta.naomod.atl.NamedFile;
import fr.imta.naomod.atl.Transformation;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EMFTVMRunner extends ATLRunner {

    @Override
    public String applyTransformation(Map<String, String> sources, Transformation transfo) throws IOException {
        ExecEnv execEnv = EmftvmFactory.eINSTANCE.createExecEnv();

        // Register input metamodels and load corresponding models

        // Load input model, we assume input model contains all sources
        for (NamedFile metamodel : transfo.inputMetamodels) {
            Model sourceModel = loadModel(sources.get(metamodel.name));
            registerMetamodel(execEnv, transfo.folderPath + "/" + metamodel.path);

            execEnv.registerInputModel(metamodel.name, sourceModel);
        }

        // Register output metamodels
        Map<String, Resource> targets = new HashMap<>();
        for (NamedFile metamodel : transfo.outputMetamodels) {
            // Create and register output model
            String targetPath = UUID.randomUUID() + ".xmi";
            Model targetModel = createModel(targetPath);
            targets.put(metamodel.name, targetModel.getResource());
            execEnv.registerOutputModel(metamodel.name, targetModel);
            registerMetamodel(execEnv,  transfo.folderPath + "/" + metamodel.path);
        }

        // Compile the ATL module
        compileATLModule(transfo.folderPath + "/" + transfo.atlFile);


        // Load and run the transformation
        Path transofPath = Path.of( transfo.folderPath + "/" + transfo.atlFile); //fixme: only one file for now
        DefaultModuleResolver moduleResolver = new DefaultModuleResolver(transofPath.getParent() + "/", resourceSet);
        execEnv.loadModule(moduleResolver, transofPath.getFileName().toString().replace(".atl", ""));
        execEnv.run(null);

        StringBuilder result = new StringBuilder();
        // Save and return the result
        for (var out : targets.entrySet()) {
            Resource r = out.getValue();
            String name = out.getKey();
            r.save(null);
            Path p = Path.of(r.getURI().path());
            String content = Files.readString(p);
            Files.delete(p);

            result.append("***************" + name + "*****************\n");
            result.append(content);
            result.append("**********************************\n");
        }
        return result.toString();
    }

    private void registerMetamodel(ExecEnv execEnv, String path) throws IOException {
        Metamodel metamodel = EmftvmFactory.eINSTANCE.createMetamodel();
        Resource metamodelResource = resourceSet.getResource(URI.createFileURI(path), true);

        // Inject primitive types for metamodel that have a PrimitiveType packages
        EMFModelFactory factory = new EMFModelFactory();
        EMFInjector injector = new EMFInjector();
        EMFReferenceModel metamodelRef = (EMFReferenceModel) factory.newReferenceModel();
        injector.inject(metamodelRef, metamodelResource);
        try {
            injector.inject(metamodelRef, (Resource)null);
        } catch (NullPointerException e) {
            // ignore
        }

        metamodel.setResource(metamodelResource);
        for (var p : metamodelResource.getContents()) {
            if (p instanceof EPackage pkg) {
                System.err.println("Registering metamodel: " + pkg.getName());
                resourceSet.getPackageRegistry().put(pkg.getNsURI(), p);
                execEnv.registerMetaModel(pkg.getName(), metamodel);
            }
        }
    }

    private Model loadModel(String path) throws IOException {
        System.err.println("Loading model: " + path);
        Resource inputResource = resourceSet.getResource(URI.createFileURI(path), true);
        Model model = EmftvmFactory.eINSTANCE.createModel();
        model.setResource(inputResource);
        return model;
    }

    private Model createModel(String path) {
        Resource outputResource = resourceSet.createResource(URI.createFileURI(path));
        Model model = EmftvmFactory.eINSTANCE.createModel();
        model.setResource(outputResource);
        return model;
    }

    private void compileATLModule(String atlPath) throws IOException {
        // TODO: skip compilation if file already exists
        AtlToEmftvmCompiler compiler = new AtlToEmftvmCompiler();
        String emftvmPath = atlPath.replace(".atl", ".emftvm");
        
        try (InputStream fin = new FileInputStream(atlPath)) {
            compiler.compile(fin, emftvmPath);
        }
    }
}
