package fr.imta.naomod.atl;

import java.io.IOException;

public interface ATLRunner {

    public String applyTransformation(String source, Transformation transfo) throws IOException;
}
