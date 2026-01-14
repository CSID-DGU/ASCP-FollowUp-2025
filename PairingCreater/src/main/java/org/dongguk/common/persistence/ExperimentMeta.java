package org.dongguk.common.persistence;

public class ExperimentMeta {
    public final String modeTag;
    public final String runId;
    public final int exportSeq;
    public final String timestamp;

    public ExperimentMeta(
        String modeTag,
        String runId,
        int exportSeq,
        String timestamp
    ) {
        this.modeTag = modeTag;
        this.runId = runId;
        this.exportSeq = exportSeq;
        this.timestamp = timestamp;
    }
}
