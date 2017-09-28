package com.example.flowstart;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.HttpResponse;

import java.io.Serializable;

import static com.example.flowstart.helpers.SlackRequest.slackMessage;
import static com.fnproject.fn.api.Headers.emptyHeaders;
import static com.fnproject.fn.api.flow.Flows.currentFlow;
import static com.fnproject.fn.api.flow.HttpMethod.POST;

public class FlowSimple {

    private static String channel;

    @FnConfiguration
    public static void setup(RuntimeContext ctx) {
        channel = ctx.getConfigurationByKey("SLACK_CHANNEL").orElse("general");
    }

    public static class Input implements Serializable {
        public String filename;
        public String keyword;
    }

    public String handleRequest(Input input) {

        FlowFuture<?> slackSent = currentFlow().invokeFunction("./post-slack",
                slackMessage(channel, String.format("Counting lines containing /%s/ in /%s/", input.keyword, input.filename)));

        slackSent.thenRun(() -> {
            FlowFuture<byte[]> getTextResponse = currentFlow()
                    .invokeFunction("./gettext", POST, emptyHeaders(), input.filename.getBytes())
                    .thenApply((HttpResponse::getBodyAsBytes));


            // Get the head of the file
            FlowFuture<byte[]> headText = getTextResponse.thenCompose((r) -> currentFlow()
                    .invokeFunction("./head", POST, emptyHeaders().withHeader("LINES", "10"), r))
                    .thenApply(HttpResponse::getBodyAsBytes);


            // Calculate the word count of grepped file file
            FlowFuture<byte[]> wordCountResult = getTextResponse
                    .thenCompose((result) -> currentFlow()
                            .invokeFunction("./grep", POST, emptyHeaders().withHeader("WORD", input.keyword), result))
                    .thenApply(HttpResponse::getBodyAsBytes)
                    .thenCompose((grepResponse) -> currentFlow()
                            .invokeFunction("./linecount", POST, emptyHeaders(), grepResponse))
                    .thenApply(HttpResponse::getBodyAsBytes);

            headText.thenCombine(wordCountResult, (head, wordcount) -> currentFlow()
                    .invokeFunction("./post-slack",
                            slackMessage(channel, "Got Results For text '" + input.filename + "' staring with :\n "
                                        + new String(head) + "\n Found " + new String(wordcount).trim() + " lines matching the query '" + input.keyword + "' ")));


            // We can handle errors on futures - Exceptionally is a bit like a catch block
            getTextResponse.exceptionallyCompose((e) -> {
                currentFlow()
                        .invokeFunction("./post-slack", slackMessage(channel, String.format("Failed to get text %s does it exist? ", input.filename)));
                return currentFlow().failedFuture(e);
            });


        });

        return "Handling request";
    }

}
