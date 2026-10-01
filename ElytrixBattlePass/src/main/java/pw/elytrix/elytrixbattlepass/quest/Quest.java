package pw.elytrix.elytrixbattlepass.quest;

public class Quest {
    private Task task;
    private String taskId;

    public Quest(Task task, String taskId) {
        this.task = task;
        this.taskId = taskId;
    }

    public Task getTask() {
        return task;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}