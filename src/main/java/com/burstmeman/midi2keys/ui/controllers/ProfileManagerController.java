package com.burstmeman.midi2keys.ui.controllers;

import com.burstmeman.midi2keys.application.services.ProfileService;
import com.burstmeman.midi2keys.application.usecases.CreateProfileUseCase;
import com.burstmeman.midi2keys.domain.entities.Profile;
import com.burstmeman.midi2keys.infrastructure.error.ApplicationException;
import com.burstmeman.midi2keys.infrastructure.error.ErrorHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller for the Profile Manager view.
 * Lists, creates, edits, and deletes profiles.
 */
@Slf4j
@Component
@Scope("prototype")
public class ProfileManagerController implements Initializable {

    private final ObservableList<Profile> profiles = FXCollections.observableArrayList();
    @FXML
    private ListView<Profile> profileListView;
    @FXML
    private TextField newProfileNameField;
    @FXML
    private Button createButton;
    @FXML
    private Button editButton;
    @FXML
    private Button duplicateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button selectButton;
    @FXML
    private Label currentProfileLabel;
    @FXML
    private VBox detailsPane;
    @FXML
    private Label mappingCountLabel;
    @FXML
    private Label createdAtLabel;
    @FXML
    private Label updatedAtLabel;
    @Autowired
    private CreateProfileUseCase createProfileUseCase;
    @Autowired
    private ProfileService profileService;
    private Consumer<Profile> onProfileSelected;
    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing ProfileManagerController");

        if (profileListView != null) {
            profileListView.setItems(profiles);
            profileListView.setCellFactory(lv -> new ProfileListCell());

            profileListView.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldVal, newVal) -> onProfileSelectionChanged(newVal));
        }

        updateButtonStates(null);
    }

    /**
     * Sets dependencies for this controller.
     */
    public void setDependencies(CreateProfileUseCase createProfileUseCase, ProfileService profileService) {
        this.createProfileUseCase = createProfileUseCase;
        this.profileService = profileService;
        refreshProfileList();
        updateCurrentProfileLabel();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnProfileSelected(Consumer<Profile> callback) {
        this.onProfileSelected = callback;
    }

    @FXML
    private void onCreateProfile() {
        String name = newProfileNameField != null ? newProfileNameField.getText() : null;

        if (name == null || name.isBlank()) {
            ErrorHandler.showWarning("Invalid Name", "Please enter a profile name.");
            return;
        }

        try {
            Profile profile = createProfileUseCase.createProfile(name.trim());
            refreshProfileList();

            profileListView.getSelectionModel().select(profile);

            if (newProfileNameField != null) {
                newProfileNameField.clear();
            }

            openProfileEditor(profile);

        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
        }
    }

    @FXML
    private void onCreateDefaultProfile() {
        try {
            Profile profile = createProfileUseCase.createDefaultPianoProfile("Default Piano");
            refreshProfileList();
            profileListView.getSelectionModel().select(profile);

            ErrorHandler.showInfo("Profile Created",
                    "Created default piano profile with 24 key mappings (C4-B5).");

        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
        }
    }

    @FXML
    private void onEditProfile() {
        Profile selected = getSelectedProfile();
        if (selected != null) {
            openProfileEditor(selected);
        }
    }

    @FXML
    private void onDuplicateProfile() {
        Profile selected = getSelectedProfile();
        if (selected == null) return;

        String newName = selected.getName() + " (Copy)";

        try {
            Profile copy = createProfileUseCase.duplicateProfile(selected.getId(), newName);
            refreshProfileList();
            profileListView.getSelectionModel().select(copy);

        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
        }
    }

    @FXML
    private void onDeleteProfile() {
        Profile selected = getSelectedProfile();
        if (selected == null) return;

        boolean confirmed = ErrorHandler.showConfirmation("Delete Profile",
                String.format("Are you sure you want to delete the profile '%s'?\n\nThis action cannot be undone.",
                        selected.getName()));

        if (confirmed) {
            try {
                createProfileUseCase.deleteProfile(selected.getId());
                refreshProfileList();
                updateCurrentProfileLabel();

            } catch (ApplicationException e) {
                ErrorHandler.handle(e);
            }
        }
    }

    @FXML
    private void onSelectProfile() {
        Profile selected = getSelectedProfile();
        if (selected == null) return;

        try {
            createProfileUseCase.selectProfile(selected.getId());
            updateCurrentProfileLabel();

            if (onProfileSelected != null) {
                onProfileSelected.accept(selected);
            }

            ErrorHandler.showInfo("Profile Selected",
                    String.format("'%s' is now the active profile.", selected.getName()));

        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
        }
    }

    @FXML
    private void onSetDefault() {
        Profile selected = getSelectedProfile();
        if (selected == null) return;

        try {
            profileService.setDefaultProfile(selected.getId());
            refreshProfileList();

            ErrorHandler.showInfo("Default Set",
                    String.format("'%s' is now the default profile.", selected.getName()));

        } catch (ApplicationException e) {
            ErrorHandler.handle(e);
        }
    }

    @FXML
    private void onClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private void refreshProfileList() {
        if (createProfileUseCase == null) return;

        List<Profile> allProfiles = createProfileUseCase.getAllProfiles();
        profiles.setAll(allProfiles);
    }

    private void updateCurrentProfileLabel() {
        if (currentProfileLabel == null || profileService == null) return;

        Profile current = profileService.getCurrentProfile();
        if (current != null) {
            currentProfileLabel.setText("Current: " + current.getName());
        } else {
            currentProfileLabel.setText("No profile selected");
        }
    }

    private void onProfileSelectionChanged(Profile profile) {
        updateButtonStates(profile);
        updateDetailsPane(profile);
    }

    private void updateButtonStates(Profile profile) {
        boolean hasSelection = profile != null;

        if (editButton != null) editButton.setDisable(!hasSelection);
        if (duplicateButton != null) duplicateButton.setDisable(!hasSelection);
        if (deleteButton != null) deleteButton.setDisable(!hasSelection);
        if (selectButton != null) selectButton.setDisable(!hasSelection);
    }

    private void updateDetailsPane(Profile profile) {
        if (detailsPane == null) return;

        if (profile == null) {
            detailsPane.setVisible(false);
            return;
        }

        detailsPane.setVisible(true);

        if (mappingCountLabel != null) {
            mappingCountLabel.setText(profile.getMappingCount() + " mappings");
        }

        if (createdAtLabel != null) {
            createdAtLabel.setText("Created: " + profile.getCreatedAt().toLocalDate());
        }

        if (updatedAtLabel != null) {
            updatedAtLabel.setText("Updated: " + profile.getUpdatedAt().toLocalDate());
        }
    }

    private Profile getSelectedProfile() {
        if (profileListView == null) return null;
        return profileListView.getSelectionModel().getSelectedItem();
    }

    private void openProfileEditor(Profile profile) {
        log.info("Opening profile editor for: {}", profile.getName());
        ErrorHandler.showInfo("Coming Soon",
                "Profile editor will open here. For now, profiles can be edited via JSON files.");
    }

    /**
     * Custom list cell for displaying profiles.
     */
    private class ProfileListCell extends ListCell<Profile> {
        @Override
        protected void updateItem(Profile profile, boolean empty) {
            super.updateItem(profile, empty);

            if (empty || profile == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            HBox container = new HBox(8);
            container.getStyleClass().add("profile-list-item");

            VBox info = new VBox(2);
            Label nameLabel = new Label(profile.getName());
            nameLabel.getStyleClass().add("profile-name");

            Label detailLabel = new Label(profile.getMappingCount() + " mappings");
            detailLabel.getStyleClass().add("caption");

            info.getChildren().addAll(nameLabel, detailLabel);
            container.getChildren().add(info);

            if (profile.isDefault()) {
                Label defaultBadge = new Label("DEFAULT");
                defaultBadge.getStyleClass().addAll("caption", "status-info");
                container.getChildren().add(defaultBadge);
            }

            setGraphic(container);
            setText(null);
        }
    }
}
