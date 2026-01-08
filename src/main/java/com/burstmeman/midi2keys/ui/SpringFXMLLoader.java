package com.burstmeman.midi2keys.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.URL;

/**
 * Helper class for loading FXML files with Spring-managed controllers.
 */
@Slf4j
public class SpringFXMLLoader {

    private final ApplicationContext applicationContext;

    public SpringFXMLLoader(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Loads an FXML file and injects Spring dependencies into the controller.
     *
     * @param fxmlPath Path to the FXML file (relative to classpath)
     * @return The loaded root node
     * @throws IOException if the FXML file cannot be loaded
     */
    public Parent load(String fxmlPath) throws IOException {
        return load(fxmlPath, null);
    }

    /**
     * Loads an FXML file and injects Spring dependencies into the controller.
     *
     * @param fxmlPath Path to the FXML file (relative to classpath)
     * @param controllerHolder Optional array to hold the controller instance
     * @return The loaded root node
     * @throws IOException if the FXML file cannot be loaded
     */
    public Parent load(String fxmlPath, Object[] controllerHolder) throws IOException {
        URL resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            throw new IOException("FXML resource not found: " + fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(resource);
        
        // Set controller factory to get controllers from Spring context
        loader.setControllerFactory(clazz -> {
            try {
                // Try to get controller from Spring context first
                String[] beanNames = applicationContext.getBeanNamesForType(clazz);
                if (beanNames.length > 0) {
                    log.debug("Loading controller {} from Spring context", clazz.getSimpleName());
                    Object controller = applicationContext.getBean(beanNames[0], clazz);
                    if (controllerHolder != null && controllerHolder.length > 0) {
                        controllerHolder[0] = controller;
                    }
                    return controller;
                }
                
                // Fallback to default instantiation if not found in Spring
                log.debug("Controller {} not found in Spring context, using default instantiation", clazz.getSimpleName());
                Object controller = clazz.getDeclaredConstructor().newInstance();
                if (controllerHolder != null && controllerHolder.length > 0) {
                    controllerHolder[0] = controller;
                }
                return controller;
            } catch (Exception e) {
                log.error("Failed to create controller: " + clazz.getName(), e);
                throw new RuntimeException("Failed to create controller: " + clazz.getName(), e);
            }
        });

        return loader.load();
    }
}

