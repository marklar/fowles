# Fowles

Distributed collection system.

## What it Does

### Core Infrastructure

The core infrastructure works like this:

* You feed it URIs.
* It performs GETs concurrently, as fast as it can.
* It gives you back the response bodies of successful GETs, and it logs which ones failed.

It backs off, and it retries as necessary.

For search results, it grabs all subsequent pages (using
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

You currently have two choices for input and output.  You may use:

* Flat files, whose paths you specify in a configuration file -OR-
* Software clients.  One to queue up inputs, and another to receive outputs.
  * e.g. producer: py/video_id_producer.py (which simply gets its IDs from a file)
  * e.g. consumer: py/video_consumer.py (which simply outputs the responses to stdout)


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

Fowles runs as a service.  Whether it takes input from flat files or
from a producer client, it doesn't have any notion of being done with
its work.  When you think it's done, Ctrl-C it.

At the moment, there are three different versions of Fowles, depending
on whether you want to fetch by videoIds, or search by query, or
search by topic.  Each has its own executable and configuration file.

The configuration file is required to tell it how to perform its job.
Currently, the names of these configuration files are hardcoded into
the different versions of the executables.  (Their names appear
below.)

### Configuration

The configuration file looks something like below.

This one is specifically for fetching videos by videoId, so it has a
pair of configuration settings specific to that (such as
`uris.num_ids_per_request`).  See the `config` directory for examples
of all the different types of configuration files.

    {
        "uris": {
            "api_key": "some-google-api-key-here",
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
        },

        "ports": {
            "input":  5557,
            "output": 5558
        },

        "files": {
            "input":  "io/video_and_channel_ids.txt",
            "output": "io/fetch/video_json.txt",
            "failed": "io/fetch/failed.txt"
        }
    }

The `ports` and `files` are for specifying input and output.  Ports
take precedence.  If you provide `ports.input`, Fowles will look there
for its input, not `files.input`.  Likewise for `ports.output` and
`files.output`.  For both input and output, you must supply at least
one.

Currently, Fowles accepts only a single API key.  Once it's exhausted,
Fowles simply "fails" on those requests, logging them to
`files.failed`.  (Eventually, it will take an array of API keys, using
each in turn.)

The `concurrency` settings are for controlling how "nice" Fowles plays
with the YouTube service.  The above settings seem like good defaults.


### Fetch by videoIds

To fetch by videoIds, run this command:

    > lein run -m fowles.fetch.core

It uses the configuration file: `fetch_cfg.json`.

If you're using files for providing input and collecting output,
that's all you need to run.  However, if you're using clients for
input and output, you need to start those as well.  For example, in
one terminal window:
   
    > python py/video_id_producer.py

, and in a different terminal window:

    > python py/video_consumer.py

You can start the processes in any order.  (It's like magic!)

### Search by Query

To search by queries, run this command:

    > lein run -m fowles.search.query.core

(At the time of this writing, it doesn't work with producers and
consumers, only files.)

It uses the configuration file: `search_query_cfg.json`.

### Search by Topic

To search by topic, run this command:

    > lein run -m fowles.search.topic.core

(At the time of this writing, it doesn't work with producers and
consumers, only files.)

It uses the configuration file: `search_topic_cfg.json`.
