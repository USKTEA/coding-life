public class Cat extends Animal {
    public Cat(String name) {
        super(name);
    }

    @Override
    public String shout() {
        return "meow";
    }
}
