/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.astrid.dao;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.data.GenericDao;
import com.todoroo.andlib.data.sql.Criterion;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.model.Task;

/**
 * Data Access layer for {@link Task}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskDao extends GenericDao<Task> {

    @Autowired
    MetadataDao metadataDao;

    @Autowired
    Database database;

	public TaskDao() {
        super(Task.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    // --- SQL clause generators

    /**
     * Generates SQL clauses
     */
    public static class TaskCriteria {

    	/** Returns tasks by id */
    	public static Criterion byId(long id) {
    	    return Task.ID.eq(id);
    	}

    	/** Return tasks that have not yet been completed */
    	public static Criterion isActive() {
    	    return Criterion.and(Task.COMPLETION_DATE.eq(0),
    	            Task.DELETION_DATE.eq(0));
    	}

    	/** Return tasks that are not hidden at given unixtime */
    	public static Criterion isVisible(int time) {
    	    return Task.HIDDEN_UNTIL.lt(time);
        }

    	/** Returns tasks that have a due date */
    	public static Criterion hasDeadlines() {
    	    return Task.DUE_DATE.neq(0);
    	}

        /** Returns tasks that are due before a certain unixtime */
        public static Criterion dueBefore(int time) {
            return Criterion.and(Task.DUE_DATE.gt(0), Task.DUE_DATE.lt(time));
        }

        /** Returns tasks that are due after a certain unixtime */
        public static Criterion dueAfter(int time) {
            return Task.DUE_DATE.gt(time);
        }

    	/** Returns tasks completed before a given unixtime */
    	public static Criterion completedBefore(int time) {
    	    return Criterion.and(Task.COMPLETION_DATE.gt(0), Task.COMPLETION_DATE.lt(time));
    	}

    	/** Returns tasks that have a blank or null title */
    	@SuppressWarnings("nls")
        public static Criterion hasNoTitle() {
    	    return Criterion.or(Task.TITLE.isNull(), Task.TITLE.eq(""));
    	}

    }

    // --- custom operations


    // --- delete

    /**
     * Delete the given item
     *
     * @param database
     * @param id
     * @return true if delete was successful
     */
    @Override
    public boolean delete(long id) {
        boolean result = super.delete(id);
        if(!result)
            return false;

        // delete all metadata
        metadataDao.deleteWhere(MetadataCriteria.byTask(id));

        return true;
    }

    // --- save

    /**
     * Saves the given task to the database.getDatabase(). Task must already
     * exist. Returns true on success.
     *
     * @param duringSync whether this save occurs as part of a sync
     */
    public boolean save(Task task, boolean duringSync) {
        boolean saveSuccessful;

        if (task.getId() == Task.NO_ID) {
            task.setValue(Task.CREATION_DATE, DateUtilities.now());
            task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
            saveSuccessful = createItem(task);
        } else {
            ContentValues values = task.getSetValues();
            if(values.size() == 0)
                return true;
            task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
            beforeSave(task, values, duringSync);
            saveSuccessful = saveItem(task);
            afterSave(task, values, duringSync);
        }

        return saveSuccessful;
    }

    /**
     * Called before the task is saved.
     * <ul>
     * <li>Update notifications based on task status
     * <li>Update associated calendar event
     *
     * @param database
     * @param task
     *            task that was just changed
     * @param values
     *            values that were changed
     * @param duringSync
     *            whether this save occurs as part of a sync
     */
    private void beforeSave(Task task, ContentValues values, boolean duringSync) {
        //
    }

    /**
     * Called after the task is saved.
     * <ul>
     * <li>Handle repeating tasks
     * <li>Save for synchronization
     *
     * @param database
     * @param task task that was just changed
     * @param values values to be persisted to the database
     * @param duringSync whether this save occurs as part of a sync
     */
    private void afterSave(Task task, ContentValues values, boolean duringSync) {
        if(duringSync)
            return;

        // if task was completed, fire task completed notification
        if(values.containsKey(Task.COMPLETION_DATE.name) &&
                values.getAsInteger(Task.COMPLETION_DATE.name) > 0 && !duringSync) {

            Context context = ContextManager.getContext();
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_COMPLETED);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
            context.sendOrderedBroadcast(broadcastIntent, null);

        }
    }

}

