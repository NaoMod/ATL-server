package fr.imta.naomod.atl.runners;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.CollationElementIterator;
import java.util.Collections;
import java.util.UUID;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.m2m.atl.core.ATLCoreException;
import org.eclipse.m2m.atl.core.emf.EMFInjector;
import org.eclipse.m2m.atl.core.emf.EMFModel;
import org.eclipse.m2m.atl.core.emf.EMFModelFactory;
import org.eclipse.m2m.atl.core.emf.EMFReferenceModel;
import org.eclipse.m2m.atl.core.launch.ILauncher;
import org.eclipse.m2m.atl.engine.emfvm.launch.EMFVMLauncher;

import fr.imta.naomod.atl.Metamodel;
import fr.imta.naomod.atl.Transformation;

public class EMFVMRunner extends ATLRunner {

    @Override
    public String applyTransformation(String source, Transformation transfo) throws ATLCoreException, IOException {
        // Create factory and injector
		EMFModelFactory factory = new EMFModelFactory();
		EMFInjector emfinjector = new EMFInjector();

        // load source metamodel
        EMFReferenceModel inMetamodel = (EMFReferenceModel) factory.newReferenceModel();
        for (Metamodel inMM : transfo.inputMetamodels) {
            emfinjector.inject(inMetamodel, inMM.metamodel);
        }

        // load target metamodel
        EMFReferenceModel outMetamodel = (EMFReferenceModel) factory.newReferenceModel();
        for (Metamodel outMM : transfo.outputMetamodels) {
            emfinjector.inject(outMetamodel, outMM.metamodel);
        }

        // load source model
        EMFModel input = (EMFModel) factory.newModel(inMetamodel);
        emfinjector.inject(input, source);

        // create target model
        EMFModel output = (EMFModel) factory.newModel(outMetamodel);

        EMFVMLauncher launcher = new EMFVMLauncher();
        launcher.initialize(Collections.emptyMap());

		// fixme: we assume only one input/output MM
        launcher.addInModel(input, "IN", transfo.inputMetamodels.get(0).getMetamodelName());
		launcher.addOutModel(output, "OUT", transfo.outputMetamodels.get(0).getMetamodelName());

		// if necessary
		// launcher.addLibrary("strings", new FileInputStream(
		// 	"../../../data/Class2Relational/ATLFile/strings.asm"
		// )); 

		// TODO
		InputStream asm = new FileInputStream(asmPath);
		
		launcher.launch(
				ILauncher.RUN_MODE, 
				new NullProgressMonitor(), 
				Collections.<String, Object> emptyMap(),
				new Object[] {asm} );

		String targetPath = UUID.randomUUID() + ".xmi";
		output.getResource().setURI(URI.createURI(targetPath));
		output.getResource().save(Collections.emptyMap());

		String result = Files.readString(Path.of(targetPath));
        Files.delete(Path.of(targetPath));
        return result;
    }

	private void compileASM(/* args */) {

	}
}
