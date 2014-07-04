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
INPUT_FILE = "py/100_pairs_of_vid_and_chan_ids.txt"
PORT = 5557

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

def submit_json(msg):
    get_pusher().send_json(msg)

def get_lines(fname):
    with open(fname) as f:
        return (ln.strip('\n') for ln in f.readlines())

def send(msg):
    print "sending: %s" % (msg)
    submit_json(msg)

def videos(vid_id):
    send({'request': 'videos',
          'id': vid_id})

def channels(chan_id):
    send({'request': 'channels',
          'id': chan_id})

def activities(chan_id):
    send({'request': 'activities',
          'channelId': chan_id})

def playlistItems(chan_id):
    playlist_id = re.sub('^UC', 'UU', chan_id)
    send({'request': 'playlistItems',
          'playlistId': playlist_id})

def push_video_ids(fname):
    for ln in get_lines(fname):
        [vid_id, chan_id] = ln.split()
        videos(vid_id)
        channels(chan_id)
        activities(chan_id)
        playlistItems(chan_id)

push_video_ids(INPUT_FILE)
