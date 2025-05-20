package com.jianhui.project.harbor.client;

import java.util.HashSet;
import java.util.Objects;
import java.util.TreeSet;

class Student{
    public String name;
    public String sex;

    public Student(String name, String sex) {
        this.name = name;
        this.sex = sex;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return Objects.equals(name, student.name) && Objects.equals(sex, student.sex);
    }

//    @Override
//    public int hashCode() {
//        return Objects.hash(name, sex);
//    }
}

public class Test1 {
    public static void main(String[] args) {
        HashSet<Object> set1 = new HashSet<>();
        TreeSet<Object> set2 = new TreeSet<>();
    }
}
