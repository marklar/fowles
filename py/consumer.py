import time
import zmq
import pprint

host = "127.0.0.1"
port = 5557
zmq_socket = None

def mk_addr(p):
    return("tcp://*:%d" % (port))

def set_host_and_port(h, p):
    global host, port
    host = h
    port = p

def get_puller():
    global zmq_socket
    if zmq_socket is None:
        context = zmq.Context()
        zmq_socket = context.socket(zmq.PULL)
        zmq_socket.bind( mk_addr(port) )
    return zmq_socket

def receive():
    return get_puller().recv_json()

# synchronous pulling
while True:
# for x in xrange(1000):
    msg = receive()
    print 'receiving val: %d' % (msg['num'])
    
