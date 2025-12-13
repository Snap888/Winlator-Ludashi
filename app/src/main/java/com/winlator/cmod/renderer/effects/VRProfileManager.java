package com.winlator.cmod.renderer.effects;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VRProfileManager {
    private static final String PREFS_NAME = "vr_profiles";
    private static final String PROFILES_KEY = "vr_profiles_list";
    
    private Context context;
    private List<VRProfile> profiles;

    public VRProfileManager(Context context) {
        this.context = context;
        this.profiles = new ArrayList<>();
        loadProfiles();
    }

    public void loadProfiles() {
        profiles.clear();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String profilesJson = prefs.getString(PROFILES_KEY, "");
        
        if (!profilesJson.isEmpty()) {
            try {
                JSONArray array = new JSONArray(profilesJson);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject profileJson = array.getJSONObject(i);
                    VRProfile profile = VRProfile.fromJSON(profileJson);
                    profiles.add(profile);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        // Если нет профилей, добавляем профиль по умолчанию
        if (profiles.isEmpty()) {
            VRProfile defaultProfile = new VRProfile("Default");
            profiles.add(defaultProfile);
        }
    }

    public void saveProfiles() {
        JSONArray array = new JSONArray();
        for (VRProfile profile : profiles) {
            try {
                array.put(profile.toJSON());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PROFILES_KEY, array.toString()).apply();
    }

    public void addProfile(VRProfile profile) {
        // Проверяем, существует ли профиль с таким именем
        for (VRProfile existingProfile : profiles) {
            if (existingProfile.getName().equals(profile.getName())) {
                // Обновляем существующий профиль
                int index = profiles.indexOf(existingProfile);
                profiles.set(index, profile);
                saveProfiles();
                return;
            }
        }
        profiles.add(profile);
        saveProfiles();
    }

    public void removeProfile(String name) {
        profiles.removeIf(profile -> profile.getName().equals(name));
        saveProfiles();
    }

    public void updateProfile(String oldName, VRProfile newProfile) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getName().equals(oldName)) {
                profiles.set(i, newProfile);
                saveProfiles();
                return;
            }
        }
    }

    public VRProfile getProfile(String name) {
        for (VRProfile profile : profiles) {
            if (profile.getName().equals(name)) {
                return profile;
            }
        }
        return null;
    }

    public List<VRProfile> getProfiles() {
        return new ArrayList<>(profiles);
    }

    public boolean hasProfile(String name) {
        for (VRProfile profile : profiles) {
            if (profile.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void clearAllProfiles() {
        profiles.clear();
        saveProfiles();
    }
}