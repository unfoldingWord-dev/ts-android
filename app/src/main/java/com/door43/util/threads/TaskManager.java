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

    private static List<ManagedTask> mTaskList = new ArrayList<>();
    private static Map<String, Integer> mTaskKeys = new HashMap<>();
    private static Map<Integer, ManagedTask> mTaskMap = new HashMap<>();
    private static int mCurrentTaskIndex = 0;
    private static final TaskManager sInstance;
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private static final BlockingQueue<Runnable> mWorkQueue = new LinkedBlockingQueue<>();
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private ThreadPoolExecutor mThreadPool = new ThreadPoolExecutor(
            NUMBER_OF_CORES,       // Initial pool size
            NUMBER_OF_CORES,       // Max pool size
            KEEP_ALIVE_TIME,
            KEEP_ALIVE_TIME_UNIT,
            mWorkQueue);

    static {
        sInstance = new TaskManager();
    }

    private TaskManager() {

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

    /**
     * Returns the task by it's id
     * @param id
     * @return
     */
    static public ManagedTask getTask(int id) {
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
    static public ManagedTask getTask(String key) {
        if(mTaskKeys.containsKey(key)) {
            return getTask(mTaskKeys.get(key));
        } else {
            return null;
        }
    }

    /**
     * Removes a task from the manager
     * @param id
     * @param forced
     */
    private static void clearTask(Object id, boolean forced) {
        if(id instanceof String) {
            clearTask((String)id, forced);
        } else if(id instanceof Integer) {
            clearTask((int)id, forced);
        }
    }

    /**
     * Removes a task from the manager
     * @param id
     */
    public static void clearTask(Object id) {
        clearTask(id, false);
    }

    /**
     * Removes a task from the manager
     * @param id
     * @param forced if true the task will be removed even if it is not finished
     */
    private static void clearTask(int id, boolean forced) {
        if(mTaskMap.containsKey(id) && (mTaskMap.get(id).isFinished() || forced)) {
            mTaskMap.remove(id);
        }
    }

    /**
     * Removes a task from the manager
     * @param key
     * @param forced if true the task will be removed even if it is not finished
     */
    private static void clearTask(String key, boolean forced) {
        if(mTaskKeys.containsKey(key)) {
            clearTask(mTaskKeys.get(key), forced);
            mTaskKeys.remove(key);
        }
    }

    public static void cancelAll() {
        ManagedTask[] runnableArray = new ManagedTask[mWorkQueue.size()];
        // Populates the array with the Runnables in the queue
        mWorkQueue.toArray(runnableArray);
        // Stores the array length in order to iterate over the array
        int len = runnableArray.length;
        /*
         * Iterates over the array of Runnables and interrupts each one's Thread.
         */
        synchronized (sInstance) {
            for (int runnableIndex = 0; runnableIndex < len; runnableIndex++) {
                Thread thread = runnableArray[runnableIndex].getThread();
                if (null != thread) {
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
     * Cancels and clears out a task
     * @param task
     */
    public static void cancelTask(ManagedTask task) {
        ManagedTask[] runnableArray = new ManagedTask[mWorkQueue.size()];
        // Populates the array with the Runnables in the queue
        mWorkQueue.toArray(runnableArray);
        // Stores the array length in order to iterate over the array
        int len = runnableArray.length;
        /*
         * Iterates over the array of Runnables and interrupts just the one thread
         */
        synchronized (sInstance) {
            for (int runnableIndex = 0; runnableIndex < len; runnableIndex++) {
                if(runnableArray[runnableIndex].getTaskId().equals(task.getTaskId())) {
                    // stop the thread
                    Thread thread = runnableArray[runnableIndex].getThread();
                    if (null != thread) {
                        thread.interrupt();
                    }
                    // clear the task
                    clearTask(runnableArray[runnableIndex].getTaskId(), true);
                    break;
                }
            }
        }
    }
}
