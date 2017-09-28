package com.example.flowstart;

import com.fnproject.fn.testing.*;
import org.junit.*;

public class FlowSimpleTest {

    @Rule
    public final FnTestingRule testing = FnTestingRule.createDefault();

    @Test
    public void shouldReturnGreeting() {
        testing.givenEvent().withBody("{\n" +
                "    \"filename\": \"hamlet\",\n" +
                "    \"keyword\" : \"love\"\n" +
                "}").enqueue();

        testing.givenFn("./post-slack");
        testing.givenFn("./gettext").withResult("some text".getBytes());
        testing.givenFn("./head").withResult("some text".getBytes());
        testing.givenFn("./grep").withResult("grep result".getBytes());
        testing.givenFn("./linecount").withResult("10".getBytes());


        testing.thenRun(FlowSimple.class,"handleRequest");
        Assert.assertEquals(200,testing.getOnlyResult().getStatus());
        
    }

}