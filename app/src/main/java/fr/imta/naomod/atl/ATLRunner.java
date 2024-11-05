package fr.imta.naomod.atl;


import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.m2m.atl.emftvm.EmftvmFactory;
import org.eclipse.m2m.atl.emftvm.ExecEnv;
import org.eclipse.m2m.atl.emftvm.Metamodel;
import org.eclipse.m2m.atl.emftvm.Model;
import org.eclipse.m2m.atl.emftvm.compiler.AtlToEmftvmCompiler;
import org.eclipse.m2m.atl.emftvm.impl.resource.EMFTVMResourceFactoryImpl;
import org.eclipse.m2m.atl.emftvm.util.DefaultModuleResolver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class ATLRunner {
    private ResourceSet resourceSet;

    public ATLRunner() {
        resourceSet = new ResourceSetImpl();

        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
            "emftvm",
            new EMFTVMResourceFactoryImpl()
        );
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
            "ecore",
            new EcoreResourceFactoryImpl()
        );
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
            "xmi",
            new XMIResourceFactoryImpl()
        );
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
            "",
            new XMIResourceFactoryImpl()
        );

        EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
    }

    public String applyTransformation(String source, Transformation transfo) throws IOException {
        ExecEnv execEnv = EmftvmFactory.eINSTANCE.createExecEnv();

        // Register input metamodels
        for (Map.Entry<String, String> entry : transfo.SourceMetamodels.entrySet()) {
            registerMetamodel(execEnv, entry.getKey(), entry.getValue());
        }

        // Register output metamodels
        for (Map.Entry<String, String> entry : transfo.TargetMetamodels.entrySet()) {
            registerMetamodel(execEnv, entry.getKey(), entry.getValue());
        }

        // Compile the ATL module
		compileATLModule(transfo.atlFile);
        // Load input model
        Model sourceModel = loadModel(source);
        execEnv.registerInputModel("IN", sourceModel);

        // Create and register output model
        String targetPath = UUID.randomUUID() + ".xmi";
        Model targetModel = createModel(targetPath);
        execEnv.registerOutputModel("OUT", targetModel);

        // Load and run the transformation
        Path transofPath = Path.of(transfo.atlFile);
        DefaultModuleResolver moduleResolver = new DefaultModuleResolver(transofPath.getParent() + "/", resourceSet);
        execEnv.loadModule(moduleResolver, transofPath.getFileName().toString().replace(".atl", ""));
        execEnv.run(null);

        // Save and return the result
        targetModel.getResource().save(null);
        String result = Files.readString(Path.of(targetPath));
        Files.delete(Path.of(targetPath));
        return result;
    }

    private void registerMetamodel(ExecEnv execEnv, String name, String path) throws IOException {
        Metamodel metamodel = EmftvmFactory.eINSTANCE.createMetamodel();
        Resource metamodelResource = resourceSet.getResource(URI.createFileURI(path), true);
        for (var p : metamodelResource.getContents()) {
            if (p instanceof EPackage) {
                System.out.println(p);
                resourceSet.getPackageRegistry().put(((EPackage)p).getName(), p);
            }
        }
        metamodel.setResource(metamodelResource);
        execEnv.registerMetaModel(name, metamodel);
    }

    private Model loadModel(String path) throws IOException {
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
