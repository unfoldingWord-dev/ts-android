package com.door43.util.threads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class manages multiple threads from a static singleton so you can easily keep track of
 * threads accross activities.
 */
public class TaskManager {

    private static Map<String, Integer> mTaskKeys = new HashMap<>();
    private static Map<Integer, ManagedTask> mTaskMap = new HashMap<>();
    private static int mCurrentTaskIndex = 0;

    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();

    private final BlockingQueue<Runnable> mWorkQueue;
    private final ThreadPoolExecutor mThreadPool;
    private static TaskManager sInstance = null;

    static {
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        sInstance = new TaskManager();
    }

    private TaskManager() {
        mWorkQueue = new LinkedBlockingQueue<>();
        mThreadPool = new ThreadPoolExecutor(
                NUMBER_OF_CORES,       // Initial pool size
                NUMBER_OF_CORES * 2,       // Max pool size
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mWorkQueue);
    }

    /**
     * Returns the TaskManager object
     * @return
     */
    public static TaskManager getsInstance() {
        return sInstance;
    }

    /**
     * Adds a task to be executed
     * @param task the task to be executed
     * @return the id of the task process
     */
    static public int addTask(ManagedTask task) {
        mCurrentTaskIndex ++;
        mTaskMap.put(mCurrentTaskIndex, task);
        task.setTaskId(mCurrentTaskIndex);
        queueTask(task);
        return mCurrentTaskIndex;
    }

    /**
     * Adds a task to be executed.
     * If the key is already in use the task will not be added.
     * @param task the task to be executed
     * @param key a key to retrieve the task at a later time
     * @return true if the key was added
     */
    static public boolean addTask(ManagedTask task, String key) {
        if(!mTaskKeys.containsKey(key)) {
            int index = addTask(task);
            task.setTaskId(key);
            mTaskKeys.put(key, index);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if a task has finished
     * @param id
     * @return
     */
    static public boolean isTaskFinished(int id) {
        return mTaskMap.get(id).isFinished();
    }

    static public ManagedTask getTask(Object id) {
        if(id instanceof String) {
            return getTask((String)id);
        } else if(id instanceof Integer) {
            return getTask((int)id);
        } else {
            return null;
        }
    }

    /**
     * Returns the task by it's id
     * @param id
     * @return
     */
    static private ManagedTask getTask(int id) {
        if(mTaskMap.containsKey(id)) {
            return mTaskMap.get(id);
        } else {
            return null;
        }
    }

    /**
     * Returns the task by it's key
     * @param key
     * @return
     */
    static private ManagedTask getTask(String key) {
        if(mTaskKeys.containsKey(key)) {
            return getTask(mTaskKeys.get(key));
        } else {
            return null;
        }
    }

    /**
     * Removes a completed task from the manager.
     * If the task has not finished it will not be removed
     * @param id the id of the task to be removed. Can be either a string or an int
     */
    public static void clearTask(Object id) {
        if(id instanceof String) {
            clearTask((String)id);
        } else if(id instanceof Integer) {
            clearTask((int)id);
        }
    }

    /**
     * Removes a completed task from the manager.
     * If the task has not finished it will not be removed.
     * @param task the task to be removed
     */
    public static void clearTask(ManagedTask task) {
        if(task != null) {
            clearTask(task.getTaskId());
        }
    }

    /**
     * Removes a task from the manager
     * @param id
     */
    private static boolean clearTask(int id) {
        if(mTaskMap.containsKey(id) && (mTaskMap.get(id).isFinished())) {
            mTaskMap.remove(id);
            return true;
        }
        return false;
    }

    /**
     * Removes a task from the manager
     * @param key
     */
    private static void clearTask(String key) {
        if(mTaskKeys.containsKey(key)) {
            if(clearTask((int)mTaskKeys.get(key))) {
                mTaskKeys.remove(key);
            }
        }
    }

    /**
     * Cancels and removes all tasks
     */
    public static void cancelAll() {
        ManagedTask[] runnableArray = new ManagedTask[sInstance.mWorkQueue.size()];
        // Populates the array with the Runnables in the queue
        sInstance.mWorkQueue.toArray(runnableArray);
        // Stores the array length in order to iterate over the array
        int len = runnableArray.length;
        /*
         * Iterates over the array of Runnables and interrupts each one's Thread.
         */
        synchronized (sInstance) {
            for (int runnableIndex = 0; runnableIndex < len; runnableIndex++) {
                Thread thread = runnableArray[runnableIndex].getThread();
                if (thread != null) {
                    thread.interrupt();
                }
            }
        }
    }

    /**
     * Adds a task to the thread pool queue
     * @param task
     */
    private static void queueTask(ManagedTask task) {
        sInstance.mThreadPool.execute(task);
    }

    /**
     * Cancels and removes a task
     * @param task
     */
    public static void cancelTask(ManagedTask task) {
        if(task != null) {
            synchronized (sInstance) {
                Thread thread = task.getThread();
                if(thread != null) {
                    thread.interrupt();
                }
                task.stop();
            }
            sInstance.mThreadPool.remove(task);
        }
    }
}
