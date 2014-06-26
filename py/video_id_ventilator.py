import time
import zmq

# cfg
INPUT_FILE = "io/video_and_channel_ids.txt"
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

def submit(msg):
    get_pusher().send(msg)

def get_lines(fname):
    with open(fname) as f:
        return (ln.strip('\n') for ln in f.readlines())

def push_video_ids(fname):
    for ln in get_lines(fname):
        vid_id = ln.split()[0]
        print "sending: %s" % (vid_id)
        submit(vid_id)

push_video_ids(INPUT_FILE)
