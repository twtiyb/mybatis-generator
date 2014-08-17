package com.xuchun.mybatis.generator.main;

import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.mybatis.generator.logging.LogFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mybatis.generator.internal.util.messages.Messages.getString;

public class Main {


    private static final String CONFIG_FILE = "-configfile"; //$NON-NLS-1$
    private static final String OVERWRITE = "-overwrite"; //$NON-NLS-1$
    private static final String CONTEXT_IDS = "-contextids"; //$NON-NLS-1$
    private static final String TABLES = "-tables"; //$NON-NLS-1$
    private static final String VERBOSE = "-verbose"; //$NON-NLS-1$
    private static final String FORCE_JAVA_LOGGING = "-forceJavaLogging"; //$NON-NLS-1$
    private static final String HELP_1 = "-?"; //$NON-NLS-1$
    private static final String HELP_2 = "-h"; //$NON-NLS-1$


    public static void main(String[] args) throws Exception {


        if (args.length == 0) {
            System.exit(0);
            return; // only to satisfy compiler, never returns
        }

        Map<String, String> arguments = parseCommandLine(args);
        if (arguments.get(CONFIG_FILE) == null) {
            return ;
        }



        List<String> warnings = new ArrayList<String>();
        boolean overwrite = true;
        File configFile = new File(Main.class.getResource(CONFIG_FILE).toURI());

        ConfigurationParser cp = new ConfigurationParser(warnings);
        Configuration config = cp.parseConfiguration(configFile);
        DefaultShellCallback callback = new DefaultShellCallback(overwrite);
        MyBatisGenerator generator = new MyBatisGenerator(config, callback, warnings);
        generator.generate(null);
        
        System.out.println(warnings);
    }


    private static Map<String, String> parseCommandLine(String[] args) {
        List<String> errors = new ArrayList<String>();
        Map<String, String> arguments = new HashMap<String, String>();

        for (int i = 0; i < args.length; i++) {
            if (CONFIG_FILE.equalsIgnoreCase(args[i])) {
                if ((i + 1) < args.length) {
                    arguments.put(CONFIG_FILE, args[i + 1]);
                } else {
                    errors.add(getString(
                            "RuntimeError.19", CONFIG_FILE)); //$NON-NLS-1$
                }
                i++;
            } else if (OVERWRITE.equalsIgnoreCase(args[i])) {
                arguments.put(OVERWRITE, "Y"); //$NON-NLS-1$
            } else if (VERBOSE.equalsIgnoreCase(args[i])) {
                arguments.put(VERBOSE, "Y"); //$NON-NLS-1$
            } else if (HELP_1.equalsIgnoreCase(args[i])) {
                arguments.put(HELP_1, "Y"); //$NON-NLS-1$
            } else if (HELP_2.equalsIgnoreCase(args[i])) {
                // put HELP_1 in the map here too - so we only
                // have to check for one entry in the mainline
                arguments.put(HELP_1, "Y"); //$NON-NLS-1$
            } else if (FORCE_JAVA_LOGGING.equalsIgnoreCase(args[i])) {
                LogFactory.forceJavaLogging();
            } else if (CONTEXT_IDS.equalsIgnoreCase(args[i])) {
                if ((i + 1) < args.length) {
                    arguments.put(CONTEXT_IDS, args[i + 1]);
                } else {
                    errors.add(getString(
                            "RuntimeError.19", CONTEXT_IDS)); //$NON-NLS-1$
                }
                i++;
            } else if (TABLES.equalsIgnoreCase(args[i])) {
                if ((i + 1) < args.length) {
                    arguments.put(TABLES, args[i + 1]);
                } else {
                    errors.add(getString("RuntimeError.19", TABLES)); //$NON-NLS-1$
                }
                i++;
            } else {
                errors.add(getString("RuntimeError.20", args[i])); //$NON-NLS-1$
            }
        }

        if (!errors.isEmpty()) {
            for (String error : errors) {
            }

            System.exit(-1);
        }

        return arguments;
    }
}
