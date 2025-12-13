package com.winlator.cmod.contentdialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.winlator.cmod.R;
import com.winlator.cmod.renderer.effects.VRProfile;
import com.winlator.cmod.renderer.effects.VRProfileManager;
import com.winlator.cmod.renderer.effects.VREffect;

import java.util.ArrayList;
import java.util.List;

public class VRProfileDialog extends Dialog {
    private final VREffect vrEffect;
    private final OnProfileChangedListener onProfileChangedListener;
    private final VRProfileManager profileManager;
    
    private ListView profilesListView;
    private EditText profileNameEditText;
    private Button createButton, loadButton, saveButton, deleteButton, renameButton;

    public interface OnProfileChangedListener {
        void onProfileChanged(VREffect vrEffect);
    }

    public VRProfileDialog(Context context, VREffect vrEffect, OnProfileChangedListener onProfileChangedListener) {
        super(context);
        this.vrEffect = vrEffect;
        this.onProfileChangedListener = onProfileChangedListener;
        this.profileManager = new VRProfileManager(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.vr_profile_dialog);
        setTitle("VR Profiles");

        initViews();
        setupListeners();
        updateProfileList();
    }

    private void initViews() {
        profilesListView = findViewById(R.id.profiles_listview);
        profileNameEditText = findViewById(R.id.profile_name_edittext);
        createButton = findViewById(R.id.create_button);
        loadButton = findViewById(R.id.load_button);
        saveButton = findViewById(R.id.save_button);
        deleteButton = findViewById(R.id.delete_button);
        renameButton = findViewById(R.id.rename_button);
    }

    private void setupListeners() {
        createButton.setOnClickListener(v -> createProfile());
        loadButton.setOnClickListener(v -> loadProfile());
        saveButton.setOnClickListener(v -> saveCurrentProfile());
        deleteButton.setOnClickListener(v -> deleteProfile());
        renameButton.setOnClickListener(v -> renameProfile());
    }

    private void createProfile() {
        String name = profileNameEditText.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a profile name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (profileManager.hasProfile(name)) {
            Toast.makeText(getContext(), "Profile with this name already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        VRProfile profile = new VRProfile(name, vrEffect);
        profileManager.addProfile(profile);
        updateProfileList();
        Toast.makeText(getContext(), "Profile created", Toast.LENGTH_SHORT).show();
    }

    private void loadProfile() {
        int selectedIndex = profilesListView.getCheckedItemPosition();
        if (selectedIndex == ListView.INVALID_POSITION) {
            Toast.makeText(getContext(), "Please select a profile to load", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedProfileName = (String) profilesListView.getAdapter().getItem(selectedIndex);
        VRProfile profile = profileManager.getProfile(selectedProfileName);
        if (profile != null) {
            vrEffect.loadFromProfile(profile);
            if (onProfileChangedListener != null) {
                onProfileChangedListener.onProfileChanged(vrEffect);
            }
            Toast.makeText(getContext(), "Profile loaded: " + selectedProfileName, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCurrentProfile() {
        int selectedIndex = profilesListView.getCheckedItemPosition();
        if (selectedIndex == ListView.INVALID_POSITION) {
            Toast.makeText(getContext(), "Please select a profile to save", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedProfileName = (String) profilesListView.getAdapter().getItem(selectedIndex);
        VRProfile profile = new VRProfile(selectedProfileName, vrEffect);
        profileManager.updateProfile(selectedProfileName, profile);
        Toast.makeText(getContext(), "Profile saved: " + selectedProfileName, Toast.LENGTH_SHORT).show();
    }

    private void deleteProfile() {
        int selectedIndex = profilesListView.getCheckedItemPosition();
        if (selectedIndex == ListView.INVALID_POSITION) {
            Toast.makeText(getContext(), "Please select a profile to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedProfileName = (String) profilesListView.getAdapter().getItem(selectedIndex);
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete the profile '" + selectedProfileName + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    profileManager.removeProfile(selectedProfileName);
                    updateProfileList();
                    Toast.makeText(getContext(), "Profile deleted: " + selectedProfileName, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renameProfile() {
        int selectedIndex = profilesListView.getCheckedItemPosition();
        if (selectedIndex == ListView.INVALID_POSITION) {
            Toast.makeText(getContext(), "Please select a profile to rename", Toast.LENGTH_SHORT).show();
            return;
        }

        String oldName = (String) profilesListView.getAdapter().getItem(selectedIndex);
        String newName = profileNameEditText.getText().toString().trim();
        
        if (newName.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a new profile name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (profileManager.hasProfile(newName)) {
            Toast.makeText(getContext(), "Profile with this name already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        VRProfile profile = profileManager.getProfile(oldName);
        if (profile != null) {
            profile.setName(newName);
            profileManager.updateProfile(oldName, profile);
            updateProfileList();
            Toast.makeText(getContext(), "Profile renamed to: " + newName, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProfileList() {
        List<VRProfile> profiles = profileManager.getProfiles();
        List<String> profileNames = new ArrayList<>();
        for (VRProfile profile : profiles) {
            profileNames.add(profile.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            getContext(), 
            android.R.layout.simple_list_item_single_choice, 
            profileNames
        );
        profilesListView.setAdapter(adapter);
    }
}