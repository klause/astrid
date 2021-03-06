package com.todoroo.astrid.test;

import java.io.File;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.TestDependencyInjector;
import com.todoroo.andlib.test.TodorooTestCase;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * Test case that automatically sets up and tears down a test database
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DatabaseTestCase extends TodorooTestCase {

    private static final String SYNC_TEST = "synctest";
    private static final String ALERTS_TEST = "alertstest";
    private static final String TAG_TASK_TEST = "tagtasktest";
    private static final String TAGS_TEST = "tagstest";
    private static final String TASKS_TEST = "taskstest";

    @Autowired
	public Database database;

    static {
        AstridDependencyInjector.initialize();

        // initialize test dependency injector
        TestDependencyInjector injector = TestDependencyInjector.initialize("db");
        injector.addInjectable("tasksTable", TASKS_TEST);
        injector.addInjectable("tagsTable", TAGS_TEST);
        injector.addInjectable("tagTaskTable", TAG_TASK_TEST);
        injector.addInjectable("alertsTable", ALERTS_TEST);
        injector.addInjectable("syncTable", SYNC_TEST);
        injector.addInjectable("database", new TestDatabase());
    }

	@Override
	protected void setUp() throws Exception {
	    super.setUp();

	    DependencyInjectionService.getInstance().inject(this);

		// empty out test databases
        database.clear();
		deleteDatabase(TASKS_TEST);
		deleteDatabase(TAGS_TEST);
		deleteDatabase(TAG_TASK_TEST);
		deleteDatabase(ALERTS_TEST);
		deleteDatabase(SYNC_TEST);

		database.openForWriting();
	}

	private void deleteDatabase(String database) {
	    File db = getContext().getDatabasePath(database);
	    if(db.exists())
	        db.delete();
    }

    @Override
	protected void tearDown() throws Exception {
		database.close();
	}

	public static class TestDatabase extends Database {
	    private static final String NAME = "databasetest";

        @Override
	    protected String getName() {
	        return NAME;
	    }
	}
}
