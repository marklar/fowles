import time
import zmq
import pprint

context = None
zmq_socket = None

host = "127.0.0.1"
port = 5557

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
    get_pusher().send_json(msg)

# synchronous pushing
for n in xrange(20):
    msg = {"num": n}
    print "sending: %s" % (msg)
    submit(msg)
