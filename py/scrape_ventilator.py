import re
import time
import zmq

#
# The input file is expected to have on each line:
#    1. a video_id
#    2. a tab
#    3. a channel_id
# The provided file (below: INPUT_FILE) matches that format.
#

# cfg
INPUT_FILE = "py/10_pairs_of_vid_and_chan_ids.txt"
PORT = 1337

# globals
context = None
pusher = None

def mk_addr(p):
    return "tcp://*:%d" % (p)

def get_pusher():
    global context, pusher
    if pusher is None:
        context = zmq.Context()
        pusher = context.socket(zmq.PUSH)
        pusher.bind( mk_addr(PORT) )
    return pusher

def get_lines(fname):
    with open(fname) as f:
        return (ln.strip('\n') for ln in f.readlines())

def push_video_ids(fname):
    for ln in get_lines(fname):
        [vid_id, chan_id] = ln.split()
        print "sending: %s" % (vid_id)
        get_pusher().send(vid_id)

push_video_ids(INPUT_FILE)
