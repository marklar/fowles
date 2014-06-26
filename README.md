# Fowles

Worker process for collecting web data.

## What it Does

### Core Infrastructure

The core infrastructure works like this:

* You feed it URIs.
* It performs GETs concurrently, as fast as it can.
* It gives you back the response bodies of successful GETs, and it logs which ones failed.

It backs off, and it retries as necessary.

For YouTube search results, it grabs all subsequent pages (using
nextPageToken).


### Tools for Collection Jobs

On top of the core infrastructure, there are sets of tools which
handle some additional work of input and output.  They help you to:

* Fetch videos.
  * You give it video IDs.
  * It returns the videos' JSON representations.
* Search for videos by query.
  * You give it 'q' strings.
  * It returns pairs of (videoId, channelId).
* Search for videos by topic.
  * You give it topicIds.
  * It returns pairs of (videoId, channelId).

With these tools, it constructs the URIs for you, and it parses the
responses for you.


### Input and Output

To supply input to Fowles and receive its output, you must provide
software "servers".  You need three:

1. a "ventilator", to queue up inputs
  * e.g. py/video_id_producer.py (which simply gets its IDs from a file)
2. a results "sink", to accept outputs from successful requests
  * e.g. py/video_consumer.py (which simply outputs the responses to stdout)
3. a failures "sink": to receive notifications about failed requests
  * e.g. py/failure_consumer.py (which simply outputs the responses to stdout)


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

At the moment, there are three different versions of Fowles, depending
on whether you want to fetch by videoIds, or search by query, or
search by topic.  Each has its own executable and configuration file.

The configuration file is required to tell it how to perform its job.
Currently, the names of these configuration files are hardcoded into
the different versions of the executables.  (Their names appear
below.)

### Configuration

Fowles requires two different configuration files:

* `config/secret.json` - which currently contains only your API keys
* `config/fetch_cfg.json` - everything else Fowles needs to know

`secret.json` is meant to contain sensitve data.  You don't include
this file in source control.  It looks like this:

    {
        "api_keys": [
            "some-google-api-key-here",
            "and-another-api-key-here"
        ]
    }


`fetch_cfg.json` contains everything else.  A 'default' version can be
checked into source control.

This configuration file looks something like below.  This one is
specifically for fetching videos by videoId, so it has one setting
specific to that purpose (i.e. `requests.num_ids_per_request`).  See
the `config` directory for examples of all the different types of
configuration files.


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

To fetch by videoIds, run this command:

    > lein run -m fowles.fetch.core

It uses the configuration file: `config/fetch_cfg.json`.

If you're using files for providing input and collecting output,
that's all you need to run.  However, if you're using clients for
input and output, you need to start those as well.  For example, in
three different terminal windows, run the following:

The ventilator:

    > python py/video_id_ventilator.py

The results sink:

    > python py/video_sink.py

The failures sink:

    > python py/failure_sink.py

You can start the processes in any order.  (It's like magic!)


# IGNORE EVERYTHING AFTER THIS...


### Search by Query

To search by queries, run this command:

    > lein run -m fowles.search.query.core

(At the time of this writing, it doesn't work with producers and
consumers, only files.)

It uses the configuration file: `config/search_query_cfg.json`.

### Search by Topic

To search by topic, run this command:

    > lein run -m fowles.search.topic.core

(At the time of this writing, it doesn't work with producers and
consumers, only files.)

It uses the configuration file: `config/search_topic_cfg.json`.
