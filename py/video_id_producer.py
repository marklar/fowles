import time
import zmq

input_file = "io/video_and_channel_ids.txt"
host = "127.0.0.1"
port = 5557

context = None
zmq_socket = None

def mk_addr(h, p):
    return "tcp://%s:%d" % (h, p)

def set_host_and_port(h, p):
    global host, port
    host = h
    port = p

def get_pusher():
    global context, zmq_socket
    if zmq_socket is None:
        context = zmq.Context()
        zmq_socket = context.socket(zmq.PUSH)
        zmq_socket.connect( mk_addr(host, port) )
    return zmq_socket

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

push_video_ids(input_file)
