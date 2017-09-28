package com.example.flowstart.helpers;


/**
 * Created on 19/09/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class SlackRequest {
    public String channel;
    public SlackMessage message;

    public static SlackRequest slackMessage(String channel , String message){
        SlackMessage msg = new SlackMessage(message);
        SlackRequest req = new SlackRequest();
        req.channel = channel;
        req.message = msg;

        return req;
    }

    /**
     * Created on 19/09/2017.
     * <p>
     * (c) 2017 Oracle Corporation
     */
    public static class SlackMessage {
        public final String text;

        public SlackMessage(String text) {
            this.text = text;
        }
    }
}
