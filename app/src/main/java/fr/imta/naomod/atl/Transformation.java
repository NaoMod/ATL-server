package fr.imta.naomod.atl;

import java.util.Map;
import java.util.HashMap;

public class Transformation {
    public int id;
    public String name;
    public String atlFile;
    public Map<String, String> inputs = new HashMap<>();
    public Map<String, String> outputs = new HashMap<>();

}
