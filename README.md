# Fowles

Distributed collection system.


## Core Infrastructure

The core infrastructure works like this:

* You feed it URIs.
* It performs GETs concurrently, as fast as it can.
* It gives you back the response bodies of successful GETs, and it logs which ones failed.

It backs off, and it retries as necessary.

For search results, it grabs all subsequent pages (using
nextPageToken).


## Tools for Collection Jobs

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


## Input and Output

You currently have two choices for input and output.  You may use:

* Flat files, whose paths you specify in a configuration file -OR-
* A software client, which queues up inputs and receives outputs one at a time.





