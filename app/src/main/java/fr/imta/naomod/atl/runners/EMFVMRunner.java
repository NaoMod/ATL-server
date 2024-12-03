package fr.imta.naomod.atl.runners;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.m2m.atl.core.ATLCoreException;
import org.eclipse.m2m.atl.core.emf.EMFInjector;
import org.eclipse.m2m.atl.core.emf.EMFModel;
import org.eclipse.m2m.atl.core.emf.EMFModelFactory;
import org.eclipse.m2m.atl.core.emf.EMFReferenceModel;
import org.eclipse.m2m.atl.core.launch.ILauncher;
import org.eclipse.m2m.atl.engine.compiler.AtlCompiler;
import org.eclipse.m2m.atl.engine.compiler.AtlStandaloneCompiler;
import org.eclipse.m2m.atl.engine.compiler.CompileTimeError;
import org.eclipse.m2m.atl.engine.emfvm.launch.EMFVMLauncher;

import fr.imta.naomod.atl.NamedFile;
import fr.imta.naomod.atl.Transformation;

public class EMFVMRunner extends ATLRunner {

    @Override
    public String applyTransformation(Map<String, String> sources, Transformation transfo) throws ATLCoreException, IOException {
        // Create factory and injector
		EMFModelFactory factory = new EMFModelFactory();
		EMFInjector emfinjector = new EMFInjector();
        String pathPrefix = transfo.folderPath;

        EMFVMLauncher launcher = new EMFVMLauncher();
        launcher.initialize(Collections.emptyMap());


        // load source metamodel
        for (NamedFile inMM : transfo.inputMetamodels) {
            EMFReferenceModel inMetamodel = (EMFReferenceModel) factory.newReferenceModel();
            emfinjector.inject(inMetamodel, pathPrefix + "/" + inMM.path);

            // load source model
            EMFModel input = (EMFModel) factory.newModel(inMetamodel);
            emfinjector.inject(input, sources.get(inMM.name));

            launcher.addInModel(input, inMM.name, inMM.getFileName(pathPrefix));
        }

        // load target metamodel
        Map<String, EMFModel> outputs = new HashMap<>();
        for (NamedFile outMM : transfo.outputMetamodels) {
            EMFReferenceModel outMetamodel = (EMFReferenceModel) factory.newReferenceModel();
            emfinjector.inject(outMetamodel, pathPrefix + "/" +  outMM.path);

            // create target model
            EMFModel output = (EMFModel) factory.newModel(outMetamodel);
            
            launcher.addOutModel(output, outMM.name, outMM.getFileName(pathPrefix));

            outputs.put(outMM.name, output);
        }

        // we load (and compile if needed) required libraries
        for (var lib : transfo.libraries) {
            String asmPath = pathPrefix + "/" + lib.path;
            // lib does not end with .asm, so we compile it
            if (!lib.path.endsWith("asm")) {
                String compiledPath = asmPath.replaceAll(".atl", ".asm");
                compileASM(asmPath, compiledPath);
                asmPath = compiledPath;
            }

            launcher.addLibrary(lib.name, new FileInputStream(asmPath));

        }
        String atlPath = pathPrefix + "/" + transfo.atlFile;
        String asmPath =  atlPath.replace(".atl", ".asm");
		compileASM(atlPath, asmPath);
		InputStream asm = new FileInputStream(asmPath);
		
		launcher.launch(
				ILauncher.RUN_MODE, 
				new NullProgressMonitor(), 
				Collections.<String, Object> emptyMap(),
				new Object[] {asm} );

        StringBuilder results = new StringBuilder();
        for (var out : outputs.entrySet()) {
            String targetPath = UUID.randomUUID() + ".xmi";
            Resource r = out.getValue().getResource();
            String name = out.getKey();
            String result = "";

            // resource is null if transformation does not generate anything
            if (r != null) {
                r.setURI(URI.createURI(targetPath));
                r.save(Collections.emptyMap());

                result = Files.readString(Path.of(targetPath));
                Files.delete(Path.of(targetPath));
            }

            if (outputs.size() > 1) results.append("***************" + name + "*****************\n");
            results.append(result);
            if (outputs.size() > 1) results.append("**********************************\n");
        }
        return results.toString();
    }

	private void compileASM(String atlPath, String asmPath) throws FileNotFoundException, UnsupportedOperationException {
        AtlStandaloneCompiler compiler = AtlCompiler.getCompiler(AtlCompiler.DEFAULT_COMPILER_NAME);

        // do not compile if it already exists
        if (!Path.of(asmPath).toFile().exists()) {
            CompileTimeError[] errors =  compiler.compile(new FileReader(atlPath), asmPath);

            boolean hasError = false;
            for (CompileTimeError e : errors) {
                System.err.println(e.getSeverity() + " - " + e.getLocation() + " - " + e.getDescription());
                if (e.getSeverity().equals("error")) {
                    hasError = true;
                }
            }
            if (hasError)
                throw new UnsupportedOperationException();
        }
	}
}
