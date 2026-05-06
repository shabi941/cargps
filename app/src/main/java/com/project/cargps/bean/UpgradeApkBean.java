package com.project.cargps.bean;

public class UpgradeApkBean {

    private DataDTO data = null;
    private int code;
    private String message;

    public DataDTO getData() {
        return data;
    }

    public void setData(DataDTO data) {
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static class DataDTO {
        private String versions;
        private String vname;
        private String notes;
        private String apk_url;
        private String is_upgrade;
        private String md5_code;

        public String getVersions() {
            return versions;
        }

        public void setVersions(String versions) {
            this.versions = versions;
        }

        public String getVname() {
            return vname;
        }

        public void setVname(String vname) {
            this.vname = vname;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getApk_url() {
            return apk_url;
        }

        public void setApk_url(String apk_url) {
            this.apk_url = apk_url;
        }

        public String getIs_upgrade() {
            return is_upgrade;
        }

        public void setIs_upgrade(String is_upgrade) {
            this.is_upgrade = is_upgrade;
        }

        public String getMd5_code() {
            return md5_code;
        }

        public void setMd5_code(String md5_code) {
            this.md5_code = md5_code;
        }
    }
}
