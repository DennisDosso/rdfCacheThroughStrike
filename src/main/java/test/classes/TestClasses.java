package test.classes;

public class TestClasses {

    public static void main(String[] args) {
        ClassB c = new ClassB("one", "two");
        System.out.println(((ClassA)c).test);
    }
}
