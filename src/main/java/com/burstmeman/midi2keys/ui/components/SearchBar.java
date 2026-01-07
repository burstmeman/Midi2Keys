package com.burstmeman.midi2keys.ui.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Reusable search bar component with debounced search functionality.
 */
public class SearchBar extends HBox {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchBar.class);
    private static final int DEFAULT_DEBOUNCE_MS = 300;
    
    private TextField searchField;
    private Button clearButton;
    private final StringProperty searchText = new SimpleStringProperty("");
    
    private Consumer<String> onSearch;
    private Timer debounceTimer;
    private int debounceDelayMs = DEFAULT_DEBOUNCE_MS;
    private boolean debounceEnabled = true;
    
    public SearchBar() {
        this("Search files...");
    }
    
    public SearchBar(String placeholder) {
        initializeComponents(placeholder);
        setupLayout();
        setupEventHandlers();
        applyStyles();
    }
    
    private void initializeComponents(String placeholder) {
        searchField = new TextField();
        searchField.setPromptText(placeholder);
        searchField.setPrefWidth(250);
        
        clearButton = new Button("×");
        clearButton.getStyleClass().add("clear-button");
        clearButton.setVisible(false);
    }
    
    private void setupLayout() {
        setSpacing(8);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(4));
        
        HBox.setHgrow(searchField, Priority.ALWAYS);
        getChildren().addAll(searchField, clearButton);
    }
    
    private void setupEventHandlers() {
        searchText.bind(searchField.textProperty());
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            clearButton.setVisible(newVal != null && !newVal.isEmpty());
            
            if (debounceEnabled && onSearch != null) {
                scheduleSearch(newVal);
            }
        });
        
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                cancelDebounce();
                triggerSearch(searchField.getText());
            } else if (event.getCode() == KeyCode.ESCAPE) {
                clear();
            }
        });
        
        clearButton.setOnAction(e -> clear());
    }
    
    private void applyStyles() {
        getStyleClass().add("search-bar");
    }
    
    private void scheduleSearch(String query) {
        cancelDebounce();
        
        debounceTimer = new Timer("search-debounce", true);
        debounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> triggerSearch(query));
            }
        }, debounceDelayMs);
    }
    
    private void cancelDebounce() {
        if (debounceTimer != null) {
            debounceTimer.cancel();
            debounceTimer = null;
        }
    }
    
    private void triggerSearch(String query) {
        if (onSearch != null) {
            logger.debug("Triggering search for: {}", query);
            try {
                onSearch.accept(query);
            } catch (Exception e) {
                logger.error("Error in search callback", e);
            }
        }
    }
    
    public void clear() {
        searchField.clear();
        cancelDebounce();
        triggerSearch("");
    }
    
    public void setOnSearch(Consumer<String> callback) {
        this.onSearch = callback;
    }
    
    public void setDebounceDelay(int delayMs) {
        this.debounceDelayMs = Math.max(0, delayMs);
    }
    
    public void setDebounceEnabled(boolean enabled) {
        this.debounceEnabled = enabled;
    }
    
    public String getSearchText() {
        return searchText.get();
    }
    
    public StringProperty searchTextProperty() {
        return searchText;
    }
    
    public void setSearchText(String text) {
        searchField.setText(text);
    }
    
    public void focusSearch() {
        searchField.requestFocus();
    }
    
    public void setPlaceholder(String placeholder) {
        searchField.setPromptText(placeholder);
    }
}
