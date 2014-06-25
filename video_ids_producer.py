import time
import zmq
import pprint

#-- cfg --
input_file = "io/video_and_channel_ids.txt"
host = "127.0.0.1"
port = 5557

context = None
zmq_socket = None

def mk_addr(h, p):
    return "tcp://%s:%d" % (h, p)

def get_pusher():
    global context, zmq_socket
    if zmq_socket is None:
        context = zmq.Context()
        zmq_socket = context.socket(zmq.PUSH)
        zmq_socket.connect( mk_addr(host, port) )
    return zmq_socket

def submit(msg):
    # get_pusher().send_json(msg)
    get_pusher().send(msg)

def each_line(fname):
    with open(fname) as f:
        return (ln.strip('\n') for ln in f.readlines())

def send_ids_from_file(fname):
    for ln in each_line(fname):
        # get first word in line
        id = ln.split()[0]
        print "sending: %s" % (id)
        submit(id)

send_ids_from_file(input_file)
