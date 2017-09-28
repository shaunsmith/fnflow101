package com.example.flowstart;


import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.RuntimeContext;

import static com.example.flowstart.helpers.SlackRequest.slackMessage;
import static com.example.flowstart.helpers.SyncFn.invokeFunction;
import static com.example.flowstart.helpers.SyncFn.invokeJsonFunction;

/**
 * Created on 22/09/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class BadExample {

    public static class Input {
        public String filename;
        public String keyword;
    }

    private static String channel;

    @FnConfiguration
    public static void setup(RuntimeContext ctx) {
        channel = ctx.getConfigurationByKey("SLACK_CHANNEL").orElse("general");
    }

    public void handleRequest(Input input) {

        invokeJsonFunction("./post-slack", slackMessage(channel, "Counting lines of text matching " + input.keyword + " in " + input.filename));
        String contents;

        try {
            contents = invokeFunction("./gettext", input.filename);
        } catch (Exception e) {
            invokeJsonFunction("./post-slack", slackMessage(channel, String.format("Failed to get text %s does it exist? ", input.filename)));
            return;
        }
        String fileHead = invokeFunction("./head", contents);

        String greppedContents = invokeFunction("./grep", contents, Headers.emptyHeaders().withHeader("WORD", input.keyword));

        String lineCount = invokeFunction("./linecount", greppedContents);

        invokeJsonFunction("./post-slack", slackMessage(channel, "Found " + lineCount + " lines matching " + input.keyword + " in " + input.filename + " With summary " + fileHead));

    }


}
