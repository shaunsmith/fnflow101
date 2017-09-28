# FnFlow 101 demo 
This demo walks through the Fn Flow system from scratch with a simple example . 

## Pre-reqs:
Ensure that the [general demo setup instructions have been followed](../README.md) at least once on the machine you are using. 

## Notes to demoer  

* Open flow101 in intellij 
  * import flow1/functions/flow as a maven project (if not done automatically)
  * your project should have `functions` visible in the top-level directory  
*  Open the flow UI before starting the function : [http://localhost:3000](http://localhost:3000)
   * You can reset the UI by reloading in the browser.  
* open slack and get into the correct demo room (`fnflow101` by default)  for the demo booth you are on 
* copy conference-demo-scripts/flow101/ to /demo  (optional - but it makes the terminal a bit more snesible.)
* Have a terminal window open 

You can configure the slack channel in FLOW101_SLACK_CHANNEL in `setenv.sh`



# Script 

This is a demo that walks through a simple  example that shows how to build long-running processes that use parallelism, asynchronous chaining and error handling in Fn using Fn Flow . 

Functions as a service platforms like Fn  are great for deploying code in small, independent units  like event handlers, simple REST services, and worker processes that only use resources when they are active and scale organically based on demand.  

What if want to build  build larger applications like workflows,  that combine these units together to implement long-running event-driven processes? 

Fn Flow is a system for implementing sophisticated, event-driven applications on top of Fn that simplifies development and speeds up delivery by letting you write  processes in the same way as they would for individual functions, retaining all of the benefits of scalability and economy that you get from running on Fn.

---

This can be explained through a simple example: 

Suppose we want to build an app that 

Given a keyword and a file name,  
* fetches the file, 
* greps the file  for a given keyword
* counts how many lines match that keyword 
* prints the number of matching lines with a 10 line summary of the file 

On linux we might do something like: 

```
pushd functions/gettext/data

cat hamlet | grep -i love | wc -l | xargs -n1 echo result:
head -n10 hamlet 

popd 
```
---

Let's start by looking at how we'd implement the individual parts in fn . 

*open services directory in IDE* 


We've built some simple functions that implement the commands we want. 

One of the great things about Fn is tha functions can just be simple docker images, these just call the corresponding unix shell commands: 

* *gettext*: We have a `gettext`function that retrieves some text by from disk by  key  - there are a few texts there like the constitution & hamlet hamlet 
* *grep*: greps a file for lines matching a specific expression 
  *  "One of the cool thiings about Fn is that because it's based on docker underneath we can write functions in any language, including Bash!      
* *head*: takes the first n lines of a text stream 
* *linecount*: counts the number of lines in a stream 
* *post-slack*: posts a message to slack 

Let's deploy these to fn : 

```
fn deploy --app flow101 --local  --all
```

Thes can now be called as individual functions: 


```
cd gettext

echo "limerick" | fn call flow101 gettext 
```

---

So how do we chain these functions togeher? 

We could write a new function that calls each of the tasks one by one, waits for the result, and then calls the next function in the chain like this: 

*open BadFunction.java*

As you can see, we call each function in a sequence and combine the results into a new function call to post the results to slack. 

This looks pretty simple but it has one big problem: 

If we run this then we would need to run  the function container that implements the process alive for the whole duration of the request, as it has to wait for each function to finish before starting the next. 

*make sure func.yaml points to BadFunction* 

```
cmd: com.example.flowstart.BadFunction::handleRequest
```

```
cd functions 
fn deploy --app flow101 --all --local
```
Open flowUI , open slack 

We'll send a request looking for Love in hamlet 
```
cat functions/flow/payload.json


./run.sh 
```

As you can see from the UI  we had to run two function containers for the duration of the request. 

This both doubles the chance that our service will fail (as the outer function may fail while the inner function is runnning) and also doubles our costs as  we have to waste an entire container which is doing nothing while  other functions are doing the work.  Nobody likes paying  twice! 

A smaller problem you might notice is that some of this work can be parellised  - we could start getting the summary of the text while whe count the words.  

FnFlow solves both of these problems by providing a Java DSL that lets you build long-lived parallel processes (called Flows) that are coordinated reliably within the FaaS platform.  while never needing to  have active functions waiting on results and allowing the maximum amount of concurrency that your process needs to run.  

---

Let's look at how we would re-write this example in Flow: 

*Update functions/flow/func.yaml to use FlowSimple.java*
```
cmd: com.example.flowstart.FlowSimple::handleRequest
```
*(open FlowSimple.java)* 
* don't start reading code yet, do bit below*

This is the same proces written as a flow application - this does the same thing as before but with the advantage that when we are waiting for these child functions to finish, we aren't using any resources, and  we can run tasks in parallel very easily. 

Rather than exectuting calls sequentially in the main function, we can execute them asynchronously - FnFlow calls create *FlowFuture* objects that behave like java's CompletableFutures and are simlar to things like Promises in Javascript. 

These allow chaining asynchronous work and error handling onto the completion of other work.  Unlike completable futures or promises  where work is chained inside the same process, FnFlow stages are triggered as function calls within the FaaS allowing us to take advantage of the distributed computing capabilites of Fn. 

In many ways you can see Flows as analagous to current asynchronous programming patterns like futures and promises but with an effectively unbounded cloud thread pool 

Let's step through the flow version and explain what it does: 

*Highlight in IDE as you go along* 

we start by posting the first message to slack : 

```
        FlowFuture<?> slackSent = currentFlow().invokeFunction("./post-slack", SlackRequest.slackMessage(channel, String.format("Counting lines containing /%s/ in /%s/", input.keyword, input.filename)));
```

This returns a future which completes when the message has been sent. 

Once this is complete we we then Run a new continuation  to perform the work: 

*Highlight body of thenRun* 

It's worth noting that this block of code may or  may not execute on the same JVM as the original request (depending on timing and current utilisation).  The Java FDK  ensures that your function is initialzied and the correct data is passed into each stage. 

This includes class and object state from the function and also captured variables  - for instance we use `input` in both the original call and the inner closure. 

*Select from "headText" to WordConut Result*

As each of the stages is declaritively chained  onto its dependenceies and Fn will start stages as soon as the input is available;   parallism comes naturally - for instance we calculate the head of the file and teh word count result in parallel here- and then join on the two results (using *thenCombine* here:) 

*select the thenCombine here* 

We can also catch errors on stages and either handle them or catch them and re-propagate them. 


Let's run this and see what it does. 

Let's redeploy the flow version: 

```
./setup.sh 
```

*open completer UI, refresh*

*open slack channel* 

We'll send the same JSON request to the function and see what it does, let's search for "love" in hamlet again: 


*Run the function*: 

```
./run.sh 
```

*Watch the completer running*
*Show the results in slack* 

As you can see, the main function now completes as soon as it's started the task and each stage runs only for as long as it needs to 

---

The blue bars are the function invocations that implement the work, and the smaller green bars are the completion stages in our main function  glue these together.

FnFlow maintains the depenendencies between stages and the UI lets us see the history of stages that caused a given stage to run. 

if we click on a stage; 

*click on linecount* 

The UI highlights the stages that were involved in triggering that stage and if you scroll down: 

*scroll down* 

it shows the code locations and logs for each of those stages allowing us to quickly diagnose problems and see what happened. 

Error handling in FnFlow is similar to using try/catch in java - you can choose to handle errors incluing recovering by executing a new graph or propagate them as we do here. 

When a stage fails with an error then downstream stages are not run. 

We can see this if we try to call the function with an unknown text: 
```
cat functions/flow/payload-bad.json
cat functions/flow/payload-bad.json | fn call flow101 flow  
```

Show error in UI 



# Summary

In this demo we've show how you can build long-running processes on Fn using only Java code. 

These processes take advantage of the underlying compute capabilities of Fn by naturally allowing parallelism and by assisting developers with a simple Java DSL. 