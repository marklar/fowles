import time
import zmq
import pprint

host = "127.0.0.1"
port = 5558
zmq_socket = None

def mk_addr(h, p):
    return("tcp://%s:%d" % (h, p))

def get_puller():
    global zmq_socket
    if zmq_socket is None:
        context = zmq.Context()
        zmq_socket = context.socket(zmq.PULL)
        zmq_socket.connect( mk_addr(host, port) )
    return zmq_socket

def receive():
    return get_puller().recv_json()

while True:
    msg = receive()
    print 'receiving val: %s' % (msg)
