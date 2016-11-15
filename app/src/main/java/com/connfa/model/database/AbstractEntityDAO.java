package com.connfa.model.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractEntityDAO<E extends AbstractEntity<I>, I> {

    private final Context context;

    public AbstractEntityDAO(Context context) {
        this.context = context;
    }

    protected abstract String getSearchCondition();

    /**
     * @param id the id of object to search
     * @return list of arguments, generated by object id.
     */
    protected abstract String[] getSearchConditionArguments(I id);

    protected abstract String getTableName();

    public abstract String getDatabaseName();

    protected abstract E newInstance();

    /**
     * This method should return list of columns, used to define unique object
     * in "contains" method
     */
    protected abstract String[] getKeyColumns();

    protected final Context getContext() {
        return context;
    }

    protected final ILAPIDBFacade getFacade() {
        return LAPIDBRegister.getInstance().lookup(getDatabaseName());
    }

    private boolean containsData(E entity) {
        ILAPIDBFacade facade = getFacade();
        return entity.getId() != null
                && facade.containsRecord(getTableName(), getSearchCondition(), getSearchConditionArguments(entity.getId()), getKeyColumns());

    }

    public final int deleteData(I entity) {
        ILAPIDBFacade facade = getFacade();
        return facade.delete(getTableName(), getSearchCondition(),
                             getSearchConditionArguments(entity)
        );
    }

    public final int deleteDataSafe(I entity) {
        ILAPIDBFacade facade = getFacade();

        try {
            facade.open();

            return facade.delete(getTableName(), getSearchCondition(),
                                 getSearchConditionArguments(entity)
            );
        } finally {
            facade.close();
        }
    }

    public int deleteAll() {
        ILAPIDBFacade facade = getFacade();
        return facade.delete(getTableName(), null, null);
    }

    public final int deleteAllSafe() {
        ILAPIDBFacade facade = getFacade();

        try {
            facade.open();

            return facade.delete(getTableName(), null, null);
        } finally {
            facade.close();
        }
    }

    private List<E> getDataSafe(String condition, String[] arguments) {
        ILAPIDBFacade facade = getFacade();

        try {
            facade.open();
            return getData(condition, arguments);

        } finally {
            facade.close();
        }

    }

    public final List<E> getDataSafe(I entity) {
        return getDataSafe(getSearchCondition(), getSearchConditionArguments(entity));
    }

    private List<E> getData(String condition, String[] arguments) {
        List<E> result = new LinkedList<>();

        ILAPIDBFacade facade = getFacade();
        Cursor cursor = facade.getAllRecords(getTableName(), null,
                                             condition, arguments
        );

        boolean moved = cursor.moveToFirst();
        while (moved) {
            E obj = newInstance();
            obj.initialize(cursor);

            result.add(obj);

            moved = cursor.moveToNext();
        }

        cursor.close();

        return result;
    }

    protected final List<E> querySafe(String query, String[] arguments) {
        ILAPIDBFacade facade = getFacade();

        try {
            facade.open();
            return query(query, arguments);

        } finally {
            facade.close();
        }
    }

    private List<E> query(String query, String[] arguments) {
        List<E> result = new LinkedList<>();

        ILAPIDBFacade facade = getFacade();
        Cursor cursor = facade.query(query, arguments);

        boolean moved = cursor.moveToFirst();
        while (moved) {
            E obj = newInstance();
            obj.initialize(cursor);

            result.add(obj);

            moved = cursor.moveToNext();
        }
        cursor.close();

        return result;
    }

    private List<E> getDataBySqlQuery(final String query, final String[] arguments) {
        List<E> result = new LinkedList<>();

        ILAPIDBFacade facade = getFacade();
        Cursor cursor = facade.query(query, arguments);

        boolean moved = cursor.moveToFirst();
        while (moved) {
            E obj = newInstance();
            obj.initialize(cursor);

            result.add(obj);

            moved = cursor.moveToNext();
        }

        cursor.close();

        return result;
    }

    protected final List<E> getDataBySqlQuerySafe(String query, String[] arguments) {
        ILAPIDBFacade facade = getFacade();
        try {
            facade.open();

            return getDataBySqlQuery(query, arguments);
        } finally {
            facade.close();
        }
    }

    public final List<E> getData(I entity) {
        return getData(getSearchCondition(), getSearchConditionArguments(entity));
    }

    public final List<E> getAllSafe() {
        ILAPIDBFacade facade = getFacade();
        try {
            facade.open();
            return getAll();

        } finally {
            facade.close();
        }
    }

    private List<E> getAll() {
        return getData(null, null);
    }

    public final void saveDataSafe(List<E> entityList) {
        ILAPIDBFacade facade = getFacade();
        try {
            facade.open();
            saveData(entityList);
        } finally {
            facade.close();
        }
    }

    public final void saveDataSafe(E entity) {
        ILAPIDBFacade facade = getFacade();
        try {
            facade.open();
            saveData(entity);
        } finally {
            facade.close();
        }
    }

    private void saveData(List<E> entityList) {
        for (E obj : entityList) {
            saveData(obj);
        }
    }

    private void saveOrUpdateData(List<E> entityList) {
        for (E obj : entityList) {
            saveOrUpdate(obj);
        }
    }

    public final void saveOrUpdateDataSafe(List<E> entityList) {
        ILAPIDBFacade facade = getFacade();
        try {
            facade.open();
            saveOrUpdateData(entityList);
        } finally {
            facade.close();
        }
    }

    public final void saveOrUpdateSafe(E entity) {
        ILAPIDBFacade facade = getFacade();

        try {
            facade.open();
            saveOrUpdate(entity);
        } finally {
            facade.close();
        }
    }

    private void saveOrUpdate(E entity) {
        if (entity != null) {
            if (!containsData(entity)) {
                saveData(entity);
            } else {
                updateData(entity);
            }
        }
    }

    private long saveData(E entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Object can't be null");
        }

        ContentValues values = entity.getContentValues();
        ILAPIDBFacade facade = getFacade();
        return facade.save(getTableName(), values);
    }

    private int updateData(E entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Object can't be null");
        }

        ContentValues values = entity.getContentValues();

        ILAPIDBFacade facade = getFacade();
        return facade.update(getTableName(), getSearchCondition(),
                             getSearchConditionArguments(entity.getId()), values
        );
    }

    public final void clearData() {
        ILAPIDBFacade facade = getFacade();
        facade.clearTable(getTableName());
    }

    public static int getIntFromBool(boolean value) {
        return value ? 1 : 0;
    }

}
