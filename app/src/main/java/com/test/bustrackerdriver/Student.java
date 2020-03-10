package com.test.bustrackerdriver;

public class Student {
    private int StudentNo;
    private double latitude,longitude;
    Student(){

    }
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }



    public Student(int StudentNo) {
        this.StudentNo = StudentNo;
    }

    public int getStudentNo() {
        return StudentNo;
    }

    public void setStudentNo(int StudentNo) {
        this.StudentNo = StudentNo;
    }
}
