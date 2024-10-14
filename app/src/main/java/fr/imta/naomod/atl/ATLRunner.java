package fr.imta.naomod.atl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

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
import org.eclipse.m2m.atl.emftvm.util.ModuleResolver;


// Setup
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

		EPackage e = EcorePackage.eINSTANCE;
	}


	protected String applyTransformation(String source, Map<String, String> inputMetamodels, Map<String, String> targetMetamodels) throws IOException {
		// create an execution environment
		ExecEnv execEnv = EmftvmFactory.eINSTANCE.createExecEnv();

		// register source and target metamodels in ExecEnv
		for (var e : inputMetamodels.entrySet()) {
			Metamodel sourceMM = EmftvmFactory.eINSTANCE.createMetamodel();
			Resource sourceMMResource = resourceSet.getResource(URI.createFileURI(e.getValue()), true);
			resourceSet.getPackageRegistry().put(e.getKey(), sourceMMResource.getContents().get(0));
			sourceMM.setResource(sourceMMResource);
			execEnv.registerMetaModel(e.getKey(), sourceMM);
		}

		for (var e : targetMetamodels.entrySet()) {
			Metamodel targetMM = EmftvmFactory.eINSTANCE.createMetamodel();
			Resource targetMMResource = resourceSet.getResource(URI.createFileURI(e.getValue()), true);

			resourceSet.getPackageRegistry().put(e.getKey(), targetMMResource.getContents().get(0));
			targetMM.setResource(targetMMResource);
			execEnv.registerMetaModel(e.getKey(), targetMM);
		}
		
		System.out.println(resourceSet.getPackageRegistry());
		// compile the ATL transformation
		compileATLModule("./src/main/resources/Class2Relational");
		
		Resource inputModel = resourceSet.getResource(URI.createFileURI(source), true);
		Model sourceModel = EmftvmFactory.eINSTANCE.createModel();
		sourceModel.setResource(inputModel);
		System.out.println(sourceModel);
		// register source model as the IN model in ATL transformation
		execEnv.registerInputModel("IN", sourceModel);
		
		String targetPath = UUID.randomUUID() + ".xmi";
		Resource target = resourceSet.createResource(URI.createFileURI(targetPath));
		Model targetModel = EmftvmFactory.eINSTANCE.createModel();
		targetModel.setResource(target);
		// register target model as the OUT model in ATL transformation
		execEnv.registerOutputModel("OUT", targetModel);


		// create a new ClassModuleResolver
		// this is used resolve ATL modules
		final ModuleResolver mr = new DefaultModuleResolver("./src/main/resources/", resourceSet);
		execEnv.loadModule(mr, "Class2Relational");

		execEnv.run(null);

		target.save(null);

		String content = Files.readString(Path.of(targetPath));
		Files.delete(Path.of(targetPath));

		return content;
	}

	private static void compileATLModule(String path) {
		AtlToEmftvmCompiler compiler = new AtlToEmftvmCompiler();
		
		try {
			InputStream fin = new FileInputStream(path + ".atl");
			compiler.compile(fin, path + ".emftvm");
			fin.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
