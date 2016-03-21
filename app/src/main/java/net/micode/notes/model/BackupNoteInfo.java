package net.micode.notes.model;

/**
 * Created by zhangmeng on 15/12/6.
 */
public class BackupNoteInfo {
    private String createTime;
    private String fixTime;
    private String bgcolorId;
    private String folderId;
    private String content;


    public String getCreateTime() {
        return createTime;
    }

    public String getFixTime() {
        return fixTime;
    }

    public String getBgcolorId() {
        return bgcolorId;
    }

    public String getFolderId() {
        return folderId;
    }

    public String getContent() {
        return content;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public void setFixTime(String fixTime) {
        this.fixTime = fixTime;
    }

    public void setBgcolorId(String bgcolorId) {
        this.bgcolorId = bgcolorId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
