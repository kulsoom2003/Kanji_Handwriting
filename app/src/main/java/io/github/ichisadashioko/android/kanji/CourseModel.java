package io.github.ichisadashioko.android.kanji;

public class CourseModel {
    private String kanjiChar;
    private int imgid;
    private int grade;

    public CourseModel(String kanjiChar, int imgid, int grade) {
        this.kanjiChar = kanjiChar;
        this.imgid = imgid;
        this.grade = grade;
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
    public int getGrade() { return grade; }

    public void setImgid(int imgid) {
        this.imgid = imgid;
    }
}
