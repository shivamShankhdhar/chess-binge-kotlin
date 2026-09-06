package org.shivam;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main() {
        Student s1 = new Student();
        s1.name = "Shivam";
        s1.age = 26;
        s1.rollNo=34;
        s1.college = "IITD";

        IO.println(s1.name);
        }
    }


    class Student{
       private String name;
        private int age;
        private int rollNo;
        private String college;

        Student(String name, int age, int rollNo,String college){
            this.name = name;
            this.rollNo = rollNo;
            this.college = college;
            this.age=age;
        }
    }
