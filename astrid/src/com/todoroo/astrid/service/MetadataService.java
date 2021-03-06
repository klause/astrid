package com.todoroo.astrid.service;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.data.sql.Criterion;
import com.todoroo.andlib.data.sql.Order;
import com.todoroo.andlib.data.sql.Query;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.model.Metadata;

/**
 * Service layer for {@link Metadata}-centered activities.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MetadataService {

    @Autowired
    private MetadataDao metadataDao;

    public MetadataService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- property list

    /**
     * @return property list containing just task id's
     */
    public static Property<?>[] idProperty() {
        return new Property<?>[] { Metadata.ID };
    }

    /**
     * @return property list containing just task id's
     */
    public static Property<?>[] valueProperty() {
        return new Property<?>[] { Metadata.VALUE };
    }

    // --- service layer

    /**
     * Clean up metadata. Typically called on startup
     */
    public void cleanup() {
        TodorooCursor<Metadata> cursor = metadataDao.fetchDangling(idProperty());
        try {
            if(cursor.getCount() == 0)
                return;

            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                metadataDao.delete(id);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Retrieve count of all metadata grouped by value
     * @param where SQL where clause
     * @param onlyCountsGreaterThanZero only include items where count > 0
     */
    public TodorooCursor<Metadata> fetchWithCount(CountProperty count,
            Criterion where, Order order, boolean onlyCountsGreaterThanZero) {
        Query query = Query.select(Metadata.VALUE, count).
            where(where).orderBy(order);
        if(onlyCountsGreaterThanZero)
            query.having(count.gt(0));
        TodorooCursor<Metadata> cursor = metadataDao.query(query);
        return cursor;
    }

    /**
     * Delete from metadata table where rows match a certain condition
     * @param where
     */
    public void deleteWhere(Criterion where) {
        metadataDao.deleteWhere(where);
    }
}
