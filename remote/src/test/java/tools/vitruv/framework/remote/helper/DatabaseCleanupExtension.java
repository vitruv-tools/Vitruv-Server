package tools.vitruv.framework.remote.helper;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Clears VSUM-related tables before each integration test so tests do not
 * interfere with each other when sharing the same Postgres database.
 */
public class DatabaseCleanupExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        var applicationContext = SpringExtension.getApplicationContext(context);
        applicationContext.getBean(TestDatabaseCleaner.class).cleanAll();
    }
}
