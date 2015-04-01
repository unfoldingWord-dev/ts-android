Task Manager
---

The task manager provides a way for an application to perform certain tasks that need to be kept
track of without breaking the ui. When you queue a task you receive a task id that can be used
to check up on the task at a later time, in a different thread or in a different activity.

##Example Usage
Here is a popular way to use the task manager.
This example does not cover storing the task id for later reference.
You could use `onSavedInstanceState` or some other custom method for keeping track of the id.

```
public int connectToTask(String taskId) {
    MyManagedTask task = (MyManagedTask) TaskManager.getTask(taskId);
    if(task != null) {
        // connect to existing task
        task.setOnFinishedListener(this);
        task.setOnProgressListener(this);
    } else if(taskHasAlreadyRan == false) {
        // start new task
        task = new MyManagedTask();
        task.setOnFinishedListener(this);
        task.setOnProgressListener(this);
        taskId = TaskManager.addTask(task);
    }
    return taskId;
}
```

You may also specifiy your own String id rather than using the default integer provided.

##Creating Tasks
In order to use the task manager you'll first need to create some tasks.
A task must extend the abstract `ManagedTask` class.