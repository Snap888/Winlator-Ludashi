package com.winlator.cmod.inputcontrols;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MultiBinding {
    private List<Binding> bindings;
    private boolean simultaneous; // true: все клавиши одновременно, false: последовательно
    
    public MultiBinding() {
        this.bindings = new ArrayList<>();
        this.simultaneous = true;
    }
    
    public MultiBinding(List<Binding> bindings, boolean simultaneous) {
        this.bindings = bindings;
        this.simultaneous = simultaneous;
    }
    
    public void addBinding(Binding binding) {
        if (!bindings.contains(binding)) {
            bindings.add(binding);
        }
    }
    
    public void removeBinding(Binding binding) {
        bindings.remove(binding);
    }
    
    public void clear() {
        bindings.clear();
    }
    
    public List<Binding> getBindings() {
        return bindings;
    }
    
    public boolean isSimultaneous() {
        return simultaneous;
    }
    
    public void setSimultaneous(boolean simultaneous) {
        this.simultaneous = simultaneous;
    }
    
    public boolean isEmpty() {
        return bindings.isEmpty();
    }
    
    public int size() {
        return bindings.size();
    }
    
    @Override
    public String toString() {
        if (bindings.isEmpty()) return "NONE";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bindings.size(); i++) {
            if (i > 0) {
                sb.append(simultaneous ? "+" : "→");
            }
            sb.append(bindings.get(i).toString());
        }
        return sb.toString();
    }
    
    public JSONObject toJSONObject() {
        try {
            JSONObject json = new JSONObject();
            JSONArray bindingsArray = new JSONArray();
            for (Binding binding : bindings) {
                bindingsArray.put(binding.name());
            }
            json.put("bindings", bindingsArray);
            json.put("simultaneous", simultaneous);
            return json;
        } catch (JSONException e) {
            return null;
        }
    }
    
    public static MultiBinding fromJSONObject(JSONObject json) {
        try {
            MultiBinding multiBinding = new MultiBinding();
            JSONArray bindingsArray = json.getJSONArray("bindings");
            for (int i = 0; i < bindingsArray.length(); i++) {
                multiBinding.addBinding(Binding.fromString(bindingsArray.getString(i)));
            }
            multiBinding.setSimultaneous(json.getBoolean("simultaneous"));
            return multiBinding;
        } catch (JSONException e) {
            return new MultiBinding();
        }
    }
    
    public static MultiBinding fromString(String str) {
        MultiBinding multiBinding = new MultiBinding();
        if (str == null || str.equals("NONE")) return multiBinding;
        
        // Определяем тип комбинации по разделителю
        boolean simultaneous = str.contains("+");
        String delimiter = simultaneous ? "\\+" : "→";
        
        String[] parts = str.split(delimiter);
        for (String part : parts) {
            part = part.trim();
            // Ищем соответствующее Binding
            for (Binding binding : Binding.values()) {
                if (binding.toString().equals(part)) {
                    multiBinding.addBinding(binding);
                    break;
                }
            }
        }
        multiBinding.setSimultaneous(simultaneous);
        return multiBinding;
    }
}