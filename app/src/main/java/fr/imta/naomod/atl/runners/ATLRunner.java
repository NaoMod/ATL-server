package fr.imta.naomod.atl.runners;

import java.util.Map;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.m2m.atl.emftvm.impl.resource.EMFTVMResourceFactoryImpl;

import fr.imta.naomod.atl.Transformation;

public abstract class ATLRunner {
    protected ResourceSet resourceSet;

    public ATLRunner() {
        this.resourceSet = new ResourceSetImpl();

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

    public abstract String applyTransformation(Map<String, String> sources, Transformation transfo) throws Exception;
}
