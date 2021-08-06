package org.jrd.backend.data;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jrd.backend.data.Cli.*;
import static org.jrd.backend.decompiling.ExpandableUrl.isOsWindows;

public class Help {

    private static final String HELP_FORMAT = HELP + ", " + H;
    private static final String VERBOSE_FORMAT = VERBOSE;
    private static final String VERSION_FORMAT = VERSION;
    private static final String BASE64_FORMAT = BASE64 + " <PUC> <CLASS REGEX>...";
    private static final String BYTES_FORMAT = BYTES + " <PUC> <CLASS REGEX>...";
    private static final String LISTJVMS_FORMAT = LISTJVMS;
    private static final String LISTPLUGINS_FORMAT = LISTPLUGINS;
    private static final String LISTCLASSES_FORMAT = LISTCLASSES + " <PUC> [<CLASS REGEX>...]";
    private static final String COMPILE_FORMAT = COMPILE + " [-p <PLUGIN>] [-cp <PUC>] [-r] <PATH>...";
    private static final String DECOMPILE_FORMAT = DECOMPILE + " <PUC> <PLUGIN> <CLASS REGEX>...";
    private static final String OVERWRITE_FORMAT = OVERWRITE + " <PUC> <CLASS NAME> [<CLASS FILE>]";
    private static final String SAVEAS_FORMAT = SAVEAS + " <PATH>";
    private static final String SAVELIKE_FORMAT = SAVELIKE + " <SAVE METHOD>";

    private static final String HELP_TEXT = "Print this help text.";
    private static final String VERBOSE_TEXT = "All exceptions and some debugging strings will be printed to standard error.";
    private static final String VERSION_TEXT = "Print version project name, version and build timestamp.";
    private static final String BASE64_TEXT = "Print Base64 encoded binary form of requested classes of a process.";
    private static final String BYTES_TEXT = "Print binary form of requested classes of a process";
    private static final String LISTJVMS_TEXT = "List all local Java processes and their PIDs.";
    private static final String LISTPLUGINS_TEXT = "List all currently configured decompiler plugins and their statuses.";
    private static final String LISTCLASSES_TEXT = "List all loaded classes of a process, optionally filtering them.\n" +
            "Only '" + SAVELIKE + " " + Saving.EXACT + "' or '" + SAVELIKE + " " + Saving.DEFAULT + "' are allowed as saving modifiers.";
    private static final String COMPILE_TEXT = "Compile local files against runtime classpath, specified by -cp.\n" +
            "Use -p to utilize some plugins' (like jasm or jcoder) bundled compilers.\n" +
            "Use -r for recursive search if <PATH> is a directory.\n" +
            "If the argument of '" + SAVEAS + "' is a valid PID or URL, the compiled code will be attempted to be injected into that process.\n" +
            "If multiple PATHs were specified, but no '" + SAVEAS + "', the process fails.";
    private static final String DECOMPILE_TEXT = "Decompile and print classes of a process with the specified decompiler plugin.\n" +
            "Javap can be passed options by appending them without spaces: 'javap-v-public ...' executes as 'javap -v -public ...'";
    private static final String OVERWRITE_TEXT = "Overwrite class of a process with new bytecode. If <CLASS FILE> is not set, standard input is used.";
    private static final String SAVEAS_TEXT = "All outputs will be written to PATH instead of to standard output.";
    private static final String SAVELIKE_TEXT = "Specify how saving will behave.";

    private static final String NOTES_SLASH = "All options can be with either one or two leading slashes ('-').";
    private static final String NOTES_REGEX = "When using <CLASS REGEX>, don't forget to escape dollar signs '$' of inner classes to '\\$', as otherwise they are treated as end-of-line by REGEX.";
    private static final String NOTES_PUC = "<PUC>, short for PidUrlClasspath, can be one of:";
    private static final String NOTES_SAVE = "<SAVE METHOD> can be one of:";
    private static final String[] NOTES_PUC_ITEMS = new String[] {
            "local process PID",
            "remote process URL, in the format of 'hostname:port'",
            "classpath of JAR on the filesystem (classpath separator is '" + File.pathSeparator + "')"

    };
    private static final String[] NOTES_SAVE_ITEMS = new String[] {
            "'" + Saving.DIR + "' - Result will be saved as '<PATH>/fully/qualified/name.class'. Default for .class binaries.",
            "'" + Saving.FQN + "' - Result will be saved as '<PATH>/fully.qualified.name.java'. Default for .java sources.",
            "'" + Saving.EXACT + "' - Result will be saved exactly to '<PATH>'. Default for everything else.",
            "'" + Saving.DEFAULT + "' - Saving uses the defaults mentioned above."
    };

    private static final String LAUNCHER_LINUX = "./start.sh";
    private static final String LAUNCHER_WINDOWS = "start.bat";

    private static final Map<String, String> ALL_OPTIONS;
    private static final Map<String, String> SAVING_OPTIONS;
    private static final Map<String, String[]> NOTES;

    static {
        ALL_OPTIONS = new LinkedHashMap<>();
        ALL_OPTIONS.put(HELP_FORMAT, HELP_TEXT);
        ALL_OPTIONS.put(VERBOSE_FORMAT, VERBOSE_TEXT);
        ALL_OPTIONS.put(VERSION_FORMAT, VERSION_TEXT);
        ALL_OPTIONS.put(LISTJVMS_FORMAT, LISTJVMS_TEXT);
        ALL_OPTIONS.put(LISTPLUGINS_FORMAT, LISTPLUGINS_TEXT);
        ALL_OPTIONS.put(LISTCLASSES_FORMAT, LISTCLASSES_TEXT);
        ALL_OPTIONS.put(BASE64_FORMAT, BASE64_TEXT);
        ALL_OPTIONS.put(BYTES_FORMAT, BYTES_TEXT);
        ALL_OPTIONS.put(COMPILE_FORMAT, COMPILE_TEXT);
        ALL_OPTIONS.put(DECOMPILE_FORMAT, DECOMPILE_TEXT);
        ALL_OPTIONS.put(OVERWRITE_FORMAT, OVERWRITE_TEXT);

        SAVING_OPTIONS = new LinkedHashMap<>();
        SAVING_OPTIONS.put(SAVEAS_FORMAT, SAVEAS_TEXT);
        SAVING_OPTIONS.put(SAVELIKE_FORMAT, SAVELIKE_TEXT);

        NOTES = new LinkedHashMap<>();
        NOTES.put(NOTES_SLASH, new String[0]);
        NOTES.put(NOTES_REGEX, new String[0]);
        NOTES.put(NOTES_PUC, NOTES_PUC_ITEMS);
        NOTES.put(NOTES_SAVE, NOTES_SAVE_ITEMS);
    }

    private static final String[] UNSAVABLE_OPTIONS = {HELP, H, LISTJVMS, LISTPLUGINS, OVERWRITE};
    private static final String[] SAVABLE_OPTIONS = {LISTCLASSES, BYTES, BASE64, COMPILE, DECOMPILE};

    private static final int LONGEST_FORMAT_LENGTH =
        Stream.of(ALL_OPTIONS.keySet(), SAVING_OPTIONS.keySet())
                .flatMap(Collection::stream)
                .map(String::length)
                .max(Integer::compare)
                .orElse(30) + 1; // at least one space between format and text

    protected static void printHelpText() {
        printHelpText(new CliHelpFormatter());
    }

    private static void printHelpText(HelpFormatter formatter) {
        formatter.printTitle();
        formatter.printName();

        formatter.printUsageHeading();
        formatter.printUsage();

        formatter.printOptionsHeading();
        formatter.printOptions();

        formatter.printNotesHeading();
        formatter.printNotes();
    }

    public static void main(String[] args) {
        printHelpText(new ManPageFormatter());
    }

    private interface HelpFormatter {
        void printTitle();

        void printName();

        void printUsageHeading();

        default void printUsage() {
            for (String launchOption : launchOptions()) {
                System.out.println(indent(1) + launcher() + launchOption);
            }
        }

        void printOptionsHeading();

        void printMainOptionsSubheading();

        void printSavingOptionsSubheading();

        default void printOptions() {
            printMainOptionsSubheading();
            printOptions(ALL_OPTIONS);

            printSavingOptionsSubheading();
            printOptions(SAVING_OPTIONS);
        }

        void printOptions(Map<String, String> map);

        String optionize(String[] options);

        default String indent(int depth) {
            return "  ".repeat(depth);
        }

        default String[] launchOptions() {
            return new String[] {
                    "# launches GUI",
                    optionize(UNSAVABLE_OPTIONS),
                    optionize(SAVABLE_OPTIONS) + savingModifiers()
            };
        }

        String launcher();

        String savingModifiers();

        void printNotesHeading();

        void printNotes();
    }

    private static class CliHelpFormatter implements HelpFormatter {

        @Override
        public void printTitle() { }

        @Override
        public void printName() { }

        @Override
        public void printUsageHeading() {
            System.out.println("Usage:");
        }

        @Override
        public void printOptionsHeading() { }

        @Override
        public void printMainOptionsSubheading() {
            System.out.println("Available options:");
        }

        @Override
        public void printSavingOptionsSubheading() {
            System.out.println("Saving modifiers:");
        }

        @Override
        public void printOptions(Map<String, String> map) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String format = entry.getKey();
                String initialSpacing = " ".repeat(LONGEST_FORMAT_LENGTH - format.length());
                String interlineSpacing = "\n" + indent(1) + " ".repeat(LONGEST_FORMAT_LENGTH);

                System.out.println(indent(1) + format + initialSpacing +
                        entry.getValue().replaceAll("\n", interlineSpacing));
            }
        }

        @Override
        public String optionize(String[] options) {
            return "(" + String.join("|", options) + ")";
        }

        @Override
        public String savingModifiers() {
            return " [" + SAVEAS + " [" + SAVELIKE + "]]";
        }

        @Override
        public String launcher() {
            return (isOsWindows() ? LAUNCHER_WINDOWS : LAUNCHER_LINUX) + " [" + VERBOSE + "] ";
        }

        @Override
        public void printNotesHeading() {
            System.out.println("Additional information");
        }

        @Override
        public void printNotes() {
            for (Map.Entry<String, String[]> entry : NOTES.entrySet()) {
                System.out.println(indent(1) + entry.getKey());

                for (String item : entry.getValue()) {
                    System.out.println(indent(2) + "- " + item);
                }
            }
        }
    }

    private static class ManPageFormatter implements HelpFormatter {

        String formatWrap(char formatChar, String string) {
            return "\\f" + formatChar + string + "\\fR";
        }

        String manFormat(String line) {
            return line.replace("\\", "\\\\") // escape one more time for man parsing
                    .replaceAll("<(.*?)>", "<\\\\fI$1\\\\fR>"); // underline <PLACEHOLDERS>
        }

        @Override
        public void printTitle() {
            String buildTimestamp;

            try {
                buildTimestamp = getJrdAttributes().get().getValue("timestamp").split(" ")[0];
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (NoSuchElementException | IndexOutOfBoundsException e) {
                buildTimestamp = "?";
            }

            System.out.println(".TH JRD 1 \"" + buildTimestamp + "\"");
        }

        @Override
        public void printName() {
            System.out.println(".SH NAME");
            System.out.println("JRD - Java Runtime Decompiler");
        }

        @Override
        public void printUsageHeading() {
            System.out.println(".SH SYNOPSIS");
        }

        @Override
        public void printOptionsHeading() {
            System.out.println(".SH OPTIONS");
        }

        @Override
        public void printMainOptionsSubheading() {
            System.out.println(".SS Standard options");
        }

        @Override
        public void printSavingOptionsSubheading() {
            System.out.println(".SS Saving modifiers");
        }

        @Override
        public void printOptions(Map<String, String> map) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String manPageParagraphs = entry.getValue().replaceAll("\n", "\n\n ");

                System.out.println(
                        ".HP\n" +
                        manFormat(entry.getKey()) + "\n " +
                        manFormat(manPageParagraphs)
                );
            }
        }

        @Override
        public String optionize(String[] options) {
            return "(" +
                    Stream.of(options)
                        .map(s -> formatWrap('B', s))
                        .collect(Collectors.joining("|")) +
                    ")";
        }


        @Override
        public String launcher() {
            return "\n" +
                    // paragraph separation
                    optionize(new String[] {LAUNCHER_LINUX, LAUNCHER_WINDOWS}) +
                    " [" + formatWrap('I', VERBOSE) + "] "; // trailing space separates from rest of line
        }

        @Override
        public String savingModifiers() {
            return " [" + formatWrap('I', SAVEAS) + " [" + formatWrap('I', SAVELIKE) + "]]";
        }

        @Override
        public void printNotesHeading() {
            System.out.println(".SH NOTES");
        }

        @Override
        public void printNotes() {
            for (Map.Entry<String, String[]> entry : NOTES.entrySet()) {
                System.out.println(".HP\n" + manFormat(entry.getKey()) + "\n");

                for (String item : entry.getValue()) {
                    System.out.println("\\[bu] " + manFormat(item) + "\n"); // unordered list
                }
            }
        }
    }
}