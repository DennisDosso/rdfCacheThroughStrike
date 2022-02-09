package test.printing;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class HelloWorldWithParameters {

    @Parameter(
            names = {"--name" , "-N"},
            description = "The name given to the user",
            required = true
    )
    private String name;

    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        HelloWorldWithParameters jArgs = new HelloWorldWithParameters();
        System.out.println("Hello " + jArgs.getName());
        // first, read the parameters
        JCommander helloCmd = JCommander.newBuilder()
                .addObject(jArgs)
                .build();
        // parse the commands and set them to our class
        helloCmd.parse(args);

        System.out.println("Hello " + jArgs.getName());
    }
}
