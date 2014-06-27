import time
import zmq

# The input file is expected to have on each line:
#    1. a video_id
#    2. a tab
#    3. a channel_id
#
# Like this:
#
# --T5RKc8RK8	UC7S2CiPev9N7r3AJ7BRRuVQ
# -6MTsQCMT3Q	UCJrDMFOdv1I2k8n9oK_V21w
# -77cUxba-aA	UC3d3_X0dUnt8YXVKyqr-XLA
# -Aa-ZfUIjIQ	UCe4fpq3_Hb8L1tt14SfoLog
# -Jf7bdHoQy0	UCbTf4RlbdSjTdaE9p2SpWyg
# ...

# cfg
INPUT_FILE = "io/video_and_channel_ids.txt"
# INPUT_FILE = "lots_of_ids.txt"
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

def send(key, val):
    msg = {key: val}
    print "sending: %s" % (msg)
    submit_json(msg)

def push_video_ids(fname):
    for ln in get_lines(fname):
        [vid_id, chan_id] = ln.split()
        send("video_id", vid_id)
        send("channel_id", chan_id)

push_video_ids(INPUT_FILE)
