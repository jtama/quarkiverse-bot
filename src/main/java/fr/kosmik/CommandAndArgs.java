package fr.kosmik;

import java.util.Optional;

public class CommandAndArgs {
    Optional<String> command;
    Optional<String> nextVersion;

    static CommandAndArgs from(String[] commandAndArgs) {
        CommandAndArgs result = new CommandAndArgs();
        result.command = getFromIndex(commandAndArgs, 1);
        result.nextVersion = getFromIndex(commandAndArgs, 2);
        return result;
    }

    private static Optional<String> getFromIndex(String[] commandAndArgs, int index) {
        return commandAndArgs.length > index ? Optional.of(commandAndArgs[index].strip()) : Optional.empty();
    }
}
