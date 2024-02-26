package io.github.ichisadashioko.android.kanji;

public class CourseModel {  //in tutorial this is CourseModel
    // string course_name for storing course_name
    // and imgid for storing image id.
    private String kanjiChar;
    private int imgid;

    public CourseModel(String kanjiChar, int imgid) {
        this.kanjiChar = kanjiChar;
        this.imgid = imgid;
    }

    public String getKanjiChar() {
        return kanjiChar;
    }

    public void setCourse_name(String kanjiChar) {
        this.kanjiChar = kanjiChar;
    }

    public int getImgid() {
        return imgid;
    }

    public void setImgid(int imgid) {
        this.imgid = imgid;
    }
}
