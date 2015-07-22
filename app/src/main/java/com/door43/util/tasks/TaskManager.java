package com.door43.util.tasks;

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
    private static Map<String, List<Integer>> mGroupTasksMap = new HashMap<>();
    private static Map<Integer, List<String>> mTaskGroupsMap = new HashMap<>();
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
                NUMBER_OF_CORES * 4,       // Max pool size
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
     * Group a task with others.
     * The task must be added to the task manager before calling this method
     * @param task
     * @param group
     * @return
     */
    static public boolean groupTask(ManagedTask task, String group) {
        synchronized (sInstance) {
            if (task != null && task.getTaskId() != null) {
                if (task.getTaskId() instanceof String) {
                    if (mTaskKeys.containsKey(task.getTaskId())) {
                        setGroup(mTaskKeys.get(task.getTaskId()), group);
                    }
                } else if (task.getTaskId() instanceof Integer) {
                    if (mTaskMap.containsKey(task.getTaskId())) {
                        setGroup((int) task.getTaskId(), group);
                    }
                }
            }
        }
        return false;
    }

    static private void setGroup(int id, String group) {
        // group to task access
        if (mGroupTasksMap.containsKey(group)) {
            if (!mGroupTasksMap.get(group).contains(id)) {
                mGroupTasksMap.get(group).add(id);
            }
        } else {
            List<Integer> l = new ArrayList<>();
            l.add(id);
            mGroupTasksMap.put(group, l);
        }

        // task to group access
        if (mTaskGroupsMap.containsKey(id)) {
            if (!mTaskGroupsMap.get(id).contains(group)) {
                mTaskGroupsMap.get(id).add(group);
            }
        } else {
            List<String> l = new ArrayList<>();
            l.add(group);
            mTaskGroupsMap.put(id, l);
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
            return getTask((String) id);
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
        if(id != null) {
            synchronized (sInstance) {
                if (id instanceof String) {
                    clearTask((String) id);
                } else if (id instanceof Integer) {
                    clearTask((int) id);
                }
            }
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
     * Returns a group of tasks
     * @param group
     * @return
     */
    public static List<ManagedTask> getGroupedTasks(String group) {
        List<ManagedTask> tasks = new ArrayList<>();
        List<Integer> ids = mGroupTasksMap.get(group);
        if(ids != null) {
            for(Integer id:ids) {
                ManagedTask t = getTask(id);
                if(t != null) {
                    tasks.add(t);
                }
            }
        }
        return tasks;
    }

    /**
     * Removes a task from the manager
     * @param id
     */
    private static boolean clearTask(Integer id) {
        if(id != null) {
            if (mTaskMap.containsKey(id) && (mTaskMap.get(id).isFinished())) {
                mTaskMap.remove(id);
                // clear group mapping
                List<String> groups = mTaskGroupsMap.get(id);
                if (groups != null) {
                    for (String group : groups) {
                        mGroupTasksMap.get(group).remove(id);
                        if (mGroupTasksMap.get(group).size() == 0) {
                            mGroupTasksMap.remove(group);
                        }
                    }
                    mTaskGroupsMap.remove(id);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Removes a task from the manager
     * @param key
     */
    private static void clearTask(String key) {
        if(mTaskKeys.containsKey(key)) {
            if(clearTask(mTaskKeys.get(key))) {
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
     * Cancels a task and removes it from the queue.
     * The task will however, remain in the list until cleared
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

    /**
     * Cancels and clears all the tasks in a group
     * @param group
     */
    public static void killGroup(String group) {
        synchronized (sInstance) {
            List<ManagedTask> tasks = getGroupedTasks(group);
            for (ManagedTask t : tasks) {
                cancelTask(t);
                clearTask(t);
            }
        }
    }
}
