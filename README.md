# Fowles

Worker process for collecting web data.

## What it Does

### Core Infrastructure

Fowles's core infrastructure works like this:

* You feed it HTTP request info.
* It performs GETs concurrently, as fast as it can.
* It gives you back the response bodies of successful GETs, plus info about the ones which failed.

It backs off, and it retries as necessary.

For YouTube search results, it grabs all subsequent pages (using
nextPageToken).

We can build layers on top of the core infrastructure to make it easy to perform different kinds of API queries.


### Tools for Collection Jobs

One such tool that's been built is "Fetch".  It allows you to request the fetching of YouTube videos or channels.  You pass in a stream of IDs, specifying of which type (i.e. "video_id" or "channel_id"), and it returns that entity's JSON string.

We hope to have intelligent "Search", too, which would allow you to pass in a "q" (query) or a Freebase "topicId" and do iterative searches through a range of dates.


### Input and Output

To supply input to Fowles and receive its output, you must provide
software "servers".  You need three:

1. a "ventilator", to queue up inputs
  * e.g. py/id_ventilator.py
2. a results "sink", to accept outputs from successful requests
  * e.g. py/results_sink.py (which simply outputs the responses to stdout)
3. a failures "sink": to receive notifications about failed requests
  * e.g. py/failures_sink.py (which simply outputs the responses to stdout)


## Prerequisites

### Java

As Fowles is written in Clojure, it runs on the JVM, so you'll need to
have Java installed.  In fact, you'll need the developer version
(because of ZeroMQ).

### Leiningen

You will need [Leiningen][5] 1.7.0 or above installed.

[5]: https://github.com/technomancy/leiningen

What is Leiningen?  Leiningen is magic.  Leiningen is your best
friend.  It's like RVM and Bundler and Rake all in one.  It's a tool
for managing your project's dependencies (including Clojure itself),
running your code, producing binary deliverables, and much more.

(Actually, Leiningen is required for building Fowles, but not
necessarily for running it.  Leiningen can be used to generate a Java
bytecode JAR file and then run directly from `java`.  This is likely
how we'll deploy it to remote servers when the need arises.)


## Running Fowles

Fowles is a software "client".  You may run one instance of Fowles or
multiple.  Each must know where (i.e. which host and port) to find
your "server" processes mentioned above.

Fowles has no notion of being "done" with its queue of work.  It just
waits for more.  When you think it's done, Ctrl-C it.


### Configuration

Fowles requires two different configuration files:

* `config/secret.json` - which currently contains only your API keys
* `config/fetch.json` - everything else Fowles needs to know

`secret.json` is meant to contain sensitve data.  You don't include
this file in source control.  It looks like this:

    {
        "api_keys": [
            "some-google-api-key-here",
            "and-another-api-key-here"
        ]
    }


`fetch.json` contains everything else.  A 'default' version can be
checked into source control.

This configuration file looks something like below.

    {
        "servers": {
            "input": {
                "host": "127.0.0.1",
                "port": 5557
            },
            "output": {
                "host": "127.0.0.1",
                "port": 5558
            },
            "failed": {
                "host": "127.0.0.1",
                "port": 5559
            }
        },

        "requests": {
            "videos": {
                "num_ids_per_request": 50,
                "args": {
                    "part": [
                        "contentDetails",
                        "snippet",
                        "statistics",
                        "status",
                        "topicDetails"
                     ],
                     "fields": "items(id,status,statistics,topicDetails,contentDetails(duration,licensedContent),snippet(publishedAt,channelId,title,categoryId,liveBroadcastContent))"
                }
            },
            "channels": {
                "num_ids_per_request": 50,
                "args": {
                    "part": [
                        "snippet",
                        "contentDetails",
                        "statistics",
                        "topicDetails",
                        "status"
                    ],
                    "fields": "items(kind,id,status,statistics,topicDetails,contentDetails,snippet(title,publishedAt))"
                }
            }
        },

        "concurrency": {
            "batches": {
                "num_requests": 5,
                "frequency_ms": 250
            },
            "sleep_ms": 1000
        }
    }


The `servers` info describes where your "ventilator" and "sink"
programs can be found.

The `requests` settings determine what queries you make of the YouTube API.

The `concurrency` settings are for controlling how "nice" Fowles plays
with the YouTube service.  The above settings seem like good defaults.


### Fetch by videoIds

To start Fowles, run this command:

    > lein run

It uses the configuration file: `config/fetch.json`.

You'll also need to start the input/output servers.  For example, in
three different terminal windows, run the following:

The ventilator:

    > python py/ids_ventilator.py

The results sink:

    > python py/results_sink.py

The failures sink:

    > python py/failures_sink.py

You can start the processes in any order.  (It's like magic!)
