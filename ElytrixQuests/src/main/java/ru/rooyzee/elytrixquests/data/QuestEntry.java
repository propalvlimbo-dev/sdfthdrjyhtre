package ru.rooyzee.elytrixquests.data;

public class QuestEntry {

    private QuestStatus status;
    private int progress;

    public QuestEntry(QuestStatus status, int progress) {
        this.status = status;
        this.progress = progress;
    }

    public QuestStatus getStatus() { return status; }
    public void setStatus(QuestStatus status) { this.status = status; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
}